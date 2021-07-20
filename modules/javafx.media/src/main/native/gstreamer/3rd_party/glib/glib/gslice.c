/* GLIB sliced memory - fast concurrent memory chunk allocator
 * Copyright (C) 2005 Tim Janik
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see <http://www.gnu.org/licenses/>.
 */
/* MT safe */

#include "config.h"
#include "glibconfig.h"

#if defined(HAVE_POSIX_MEMALIGN) && !defined(_XOPEN_SOURCE)
#define _XOPEN_SOURCE 600       /* posix_memalign() */
#endif
#include <stdlib.h>             /* posix_memalign() */
#include <string.h>
#include <errno.h>

#ifdef G_OS_UNIX
#include <unistd.h>             /* sysconf() */
#endif
#ifdef G_OS_WIN32
#include <windows.h>
#include <process.h>
#endif

#include <stdio.h>              /* fputs */

#include "gslice.h"

#include "gmain.h"
#include "gmem.h"               /* gslice.h */
#include "gstrfuncs.h"
#include "gstrfuncsprivate.h"
#include "gutils.h"
#include "gtrashstack.h"
#include "gtestutils.h"
#include "gthread.h"
#include "gthreadprivate.h"
#include "glib_trace.h"
#include "gprintf.h"

#include "gvalgrind.h"

/**
 * SECTION:memory_slices
 * @title: Memory Slices
 * @short_description: efficient way to allocate groups of equal-sized
 *     chunks of memory
 *
 * Memory slices provide a space-efficient and multi-processing scalable
 * way to allocate equal-sized pieces of memory, just like the original
 * #GMemChunks (from GLib 2.8), while avoiding their excessive
 * memory-waste, scalability and performance problems.
 *
 * To achieve these goals, the slice allocator uses a sophisticated,
 * layered design that has been inspired by Bonwick's slab allocator
 * ([Bonwick94](http://citeseer.ist.psu.edu/bonwick94slab.html)
 * Jeff Bonwick, The slab allocator: An object-caching kernel
 * memory allocator. USENIX 1994, and
 * [Bonwick01](http://citeseer.ist.psu.edu/bonwick01magazines.html)
 * Bonwick and Jonathan Adams, Magazines and vmem: Extending the
 * slab allocator to many cpu's and arbitrary resources. USENIX 2001)
 *
 * It uses posix_memalign() to optimize allocations of many equally-sized
 * chunks, and has per-thread free lists (the so-called magazine layer)
 * to quickly satisfy allocation requests of already known structure sizes.
 * This is accompanied by extra caching logic to keep freed memory around
 * for some time before returning it to the system. Memory that is unused
 * due to alignment constraints is used for cache colorization (random
 * distribution of chunk addresses) to improve CPU cache utilization. The
 * caching layer of the slice allocator adapts itself to high lock contention
 * to improve scalability.
 *
 * The slice allocator can allocate blocks as small as two pointers, and
 * unlike malloc(), it does not reserve extra space per block. For large block
 * sizes, g_slice_new() and g_slice_alloc() will automatically delegate to the
 * system malloc() implementation. For newly written code it is recommended
 * to use the new `g_slice` API instead of g_malloc() and
 * friends, as long as objects are not resized during their lifetime and the
 * object size used at allocation time is still available when freeing.
 *
 * Here is an example for using the slice allocator:
 * |[<!-- language="C" -->
 * gchar *mem[10000];
 * gint i;
 *
 * // Allocate 10000 blocks.
 * for (i = 0; i < 10000; i++)
 *   {
 *     mem[i] = g_slice_alloc (50);
 *
 *     // Fill in the memory with some junk.
 *     for (j = 0; j < 50; j++)
 *       mem[i][j] = i * j;
 *   }
 *
 * // Now free all of the blocks.
 * for (i = 0; i < 10000; i++)
 *   g_slice_free1 (50, mem[i]);
 * ]|
 *
 * And here is an example for using the using the slice allocator
 * with data structures:
 * |[<!-- language="C" -->
 * GRealArray *array;
 *
 * // Allocate one block, using the g_slice_new() macro.
 * array = g_slice_new (GRealArray);
 *
 * // We can now use array just like a normal pointer to a structure.
 * array->data            = NULL;
 * array->len             = 0;
 * array->alloc           = 0;
 * array->zero_terminated = (zero_terminated ? 1 : 0);
 * array->clear           = (clear ? 1 : 0);
 * array->elt_size        = elt_size;
 *
 * // We can free the block, so it can be reused.
 * g_slice_free (GRealArray, array);
 * ]|
 */

/* the GSlice allocator is split up into 4 layers, roughly modelled after the slab
 * allocator and magazine extensions as outlined in:
 * + [Bonwick94] Jeff Bonwick, The slab allocator: An object-caching kernel
 *   memory allocator. USENIX 1994, http://citeseer.ist.psu.edu/bonwick94slab.html
 * + [Bonwick01] Bonwick and Jonathan Adams, Magazines and vmem: Extending the
 *   slab allocator to many cpu's and arbitrary resources.
 *   USENIX 2001, http://citeseer.ist.psu.edu/bonwick01magazines.html
 * the layers are:
 * - the thread magazines. for each (aligned) chunk size, a magazine (a list)
 *   of recently freed and soon to be allocated chunks is maintained per thread.
 *   this way, most alloc/free requests can be quickly satisfied from per-thread
 *   free lists which only require one g_private_get() call to retrieve the
 *   thread handle.
 * - the magazine cache. allocating and freeing chunks to/from threads only
 *   occurs at magazine sizes from a global depot of magazines. the depot
 *   maintaines a 15 second working set of allocated magazines, so full
 *   magazines are not allocated and released too often.
 *   the chunk size dependent magazine sizes automatically adapt (within limits,
 *   see [3]) to lock contention to properly scale performance across a variety
 *   of SMP systems.
 * - the slab allocator. this allocator allocates slabs (blocks of memory) close
 *   to the system page size or multiples thereof which have to be page aligned.
 *   the blocks are divided into smaller chunks which are used to satisfy
 *   allocations from the upper layers. the space provided by the reminder of
 *   the chunk size division is used for cache colorization (random distribution
 *   of chunk addresses) to improve processor cache utilization. multiple slabs
 *   with the same chunk size are kept in a partially sorted ring to allow O(1)
 *   freeing and allocation of chunks (as long as the allocation of an entirely
 *   new slab can be avoided).
 * - the page allocator. on most modern systems, posix_memalign(3) or
 *   memalign(3) should be available, so this is used to allocate blocks with
 *   system page size based alignments and sizes or multiples thereof.
 *   if no memalign variant is provided, valloc() is used instead and
 *   block sizes are limited to the system page size (no multiples thereof).
 *   as a fallback, on system without even valloc(), a malloc(3)-based page
 *   allocator with alloc-only behaviour is used.
 *
 * NOTES:
 * [1] some systems memalign(3) implementations may rely on boundary tagging for
 *     the handed out memory chunks. to avoid excessive page-wise fragmentation,
 *     we reserve 2 * sizeof (void*) per block size for the systems memalign(3),
 *     specified in NATIVE_MALLOC_PADDING.
 * [2] using the slab allocator alone already provides for a fast and efficient
 *     allocator, it doesn't properly scale beyond single-threaded uses though.
 *     also, the slab allocator implements eager free(3)-ing, i.e. does not
 *     provide any form of caching or working set maintenance. so if used alone,
 *     it's vulnerable to trashing for sequences of balanced (alloc, free) pairs
 *     at certain thresholds.
 * [3] magazine sizes are bound by an implementation specific minimum size and
 *     a chunk size specific maximum to limit magazine storage sizes to roughly
 *     16KB.
 * [4] allocating ca. 8 chunks per block/page keeps a good balance between
 *     external and internal fragmentation (<= 12.5%). [Bonwick94]
 */

/* --- macros and constants --- */
#define LARGEALIGNMENT          (256)
#define P2ALIGNMENT             (2 * sizeof (gsize))                            /* fits 2 pointers (assumed to be 2 * GLIB_SIZEOF_SIZE_T below) */
#define ALIGN(size, base)       ((base) * (gsize) (((size) + (base) - 1) / (base)))
#define NATIVE_MALLOC_PADDING   P2ALIGNMENT                                     /* per-page padding left for native malloc(3) see [1] */
#define SLAB_INFO_SIZE          P2ALIGN (sizeof (SlabInfo) + NATIVE_MALLOC_PADDING)
#define MAX_MAGAZINE_SIZE       (256)                                           /* see [3] and allocator_get_magazine_threshold() for this */
#define MIN_MAGAZINE_SIZE       (4)
#define MAX_STAMP_COUNTER       (7)                                             /* distributes the load of gettimeofday() */
#define MAX_SLAB_CHUNK_SIZE(al) (((al)->max_page_size - SLAB_INFO_SIZE) / 8)    /* we want at last 8 chunks per page, see [4] */
#define MAX_SLAB_INDEX(al)      (SLAB_INDEX (al, MAX_SLAB_CHUNK_SIZE (al)) + 1)
#define SLAB_INDEX(al, asize)   ((asize) / P2ALIGNMENT - 1)                     /* asize must be P2ALIGNMENT aligned */
#define SLAB_CHUNK_SIZE(al, ix) (((ix) + 1) * P2ALIGNMENT)
#define SLAB_BPAGE_SIZE(al,csz) (8 * (csz) + SLAB_INFO_SIZE)

/* optimized version of ALIGN (size, P2ALIGNMENT) */
#if     GLIB_SIZEOF_SIZE_T * 2 == 8  /* P2ALIGNMENT */
#define P2ALIGN(size)   (((size) + 0x7) & ~(gsize) 0x7)
#elif   GLIB_SIZEOF_SIZE_T * 2 == 16 /* P2ALIGNMENT */
#define P2ALIGN(size)   (((size) + 0xf) & ~(gsize) 0xf)
#else
#define P2ALIGN(size)   ALIGN (size, P2ALIGNMENT)
#endif

/* special helpers to avoid gmessage.c dependency */
static void mem_error (const char *format, ...) G_GNUC_PRINTF (1,2);
#define mem_assert(cond)    do { if (G_LIKELY (cond)) ; else mem_error ("assertion failed: %s", #cond); } while (0)

/* --- structures --- */
typedef struct _ChunkLink      ChunkLink;
typedef struct _SlabInfo       SlabInfo;
typedef struct _CachedMagazine CachedMagazine;
struct _ChunkLink {
  ChunkLink *next;
  ChunkLink *data;
};
struct _SlabInfo {
  ChunkLink *chunks;
  guint n_allocated;
  SlabInfo *next, *prev;
};
typedef struct {
  ChunkLink *chunks;
  gsize      count;                     /* approximative chunks list length */
} Magazine;
typedef struct {
  Magazine   *magazine1;                /* array of MAX_SLAB_INDEX (allocator) */
  Magazine   *magazine2;                /* array of MAX_SLAB_INDEX (allocator) */
} ThreadMemory;
typedef struct {
  gboolean always_malloc;
  gboolean bypass_magazines;
  gboolean debug_blocks;
  gsize    working_set_msecs;
  guint    color_increment;
} SliceConfig;
typedef struct {
  /* const after initialization */
  gsize         min_page_size, max_page_size;
  SliceConfig   config;
  gsize         max_slab_chunk_size_for_magazine_cache;
  /* magazine cache */
  GMutex        magazine_mutex;
  ChunkLink   **magazines;                /* array of MAX_SLAB_INDEX (allocator) */
  guint        *contention_counters;      /* array of MAX_SLAB_INDEX (allocator) */
  gint          mutex_counter;
  guint         stamp_counter;
  guint         last_stamp;
  /* slab allocator */
  GMutex        slab_mutex;
  SlabInfo    **slab_stack;                /* array of MAX_SLAB_INDEX (allocator) */
  guint        color_accu;
} Allocator;

/* --- g-slice prototypes --- */
static gpointer     slab_allocator_alloc_chunk       (gsize      chunk_size);
static void         slab_allocator_free_chunk        (gsize      chunk_size,
                                                      gpointer   mem);
static void         private_thread_memory_cleanup    (gpointer   data);
static gpointer     allocator_memalign               (gsize      alignment,
                                                      gsize      memsize);
static void         allocator_memfree                (gsize      memsize,
                                                      gpointer   mem);
static inline void  magazine_cache_update_stamp      (void);
static inline gsize allocator_get_magazine_threshold (Allocator *allocator,
                                                      guint      ix);

/* --- g-slice memory checker --- */
static void     smc_notify_alloc  (void   *pointer,
                                   size_t  size);
static int      smc_notify_free   (void   *pointer,
                                   size_t  size);

/* --- variables --- */
static GPrivate    private_thread_memory = G_PRIVATE_INIT (private_thread_memory_cleanup);
static gsize       sys_page_size = 0;
static Allocator   allocator[1] = { { 0, }, };
static SliceConfig slice_config = {
  FALSE,        /* always_malloc */
  FALSE,        /* bypass_magazines */
  FALSE,        /* debug_blocks */
  15 * 1000,    /* working_set_msecs */
  1,            /* color increment, alt: 0x7fffffff */
};
static GMutex      smc_tree_mutex; /* mutex for G_SLICE=debug-blocks */

/* --- auxiliary functions --- */
void
g_slice_set_config (GSliceConfig ckey,
                    gint64       value)
{
  g_return_if_fail (sys_page_size == 0);
  switch (ckey)
    {
    case G_SLICE_CONFIG_ALWAYS_MALLOC:
      slice_config.always_malloc = value != 0;
      break;
    case G_SLICE_CONFIG_BYPASS_MAGAZINES:
      slice_config.bypass_magazines = value != 0;
      break;
    case G_SLICE_CONFIG_WORKING_SET_MSECS:
      slice_config.working_set_msecs = value;
      break;
    case G_SLICE_CONFIG_COLOR_INCREMENT:
      slice_config.color_increment = value;
      break;
    default: ;
    }
}

gint64
g_slice_get_config (GSliceConfig ckey)
{
  switch (ckey)
    {
    case G_SLICE_CONFIG_ALWAYS_MALLOC:
      return slice_config.always_malloc;
    case G_SLICE_CONFIG_BYPASS_MAGAZINES:
      return slice_config.bypass_magazines;
    case G_SLICE_CONFIG_WORKING_SET_MSECS:
      return slice_config.working_set_msecs;
    case G_SLICE_CONFIG_CHUNK_SIZES:
      return MAX_SLAB_INDEX (allocator);
    case G_SLICE_CONFIG_COLOR_INCREMENT:
      return slice_config.color_increment;
    default:
      return 0;
    }
}

gint64*
g_slice_get_config_state (GSliceConfig ckey,
                          gint64       address,
                          guint       *n_values)
{
  guint i = 0;
  g_return_val_if_fail (n_values != NULL, NULL);
  *n_values = 0;
  switch (ckey)
    {
      gint64 array[64];
    case G_SLICE_CONFIG_CONTENTION_COUNTER:
      array[i++] = SLAB_CHUNK_SIZE (allocator, address);
      array[i++] = allocator->contention_counters[address];
      array[i++] = allocator_get_magazine_threshold (allocator, address);
      *n_values = i;
      return g_memdup2 (array, sizeof (array[0]) * *n_values);
    default:
      return NULL;
    }
}

static void
slice_config_init (SliceConfig *config)
{
#ifndef GSTREAMER_LITE
  const gchar *val;
  gchar *val_allocated = NULL;

  *config = slice_config;

  /* Note that the empty string (`G_SLICE=""`) is treated differently from the
   * envvar being unset. In the latter case, we also check whether running under
   * valgrind. */
#ifndef G_OS_WIN32
  val = g_getenv ("G_SLICE");
#else
  /* The win32 implementation of g_getenv() has to do UTF-8 ↔ UTF-16 conversions
   * which use the slice allocator, leading to deadlock. Use a simple in-place
   * implementation here instead.
   *
   * Ignore references to other environment variables: only support values which
   * are a combination of always-malloc and debug-blocks. */
  {

  wchar_t wvalue[128];  /* at least big enough for `always-malloc,debug-blocks` */
  int len;

  len = GetEnvironmentVariableW (L"G_SLICE", wvalue, G_N_ELEMENTS (wvalue));

  if (len == 0)
    {
      if (GetLastError () == ERROR_ENVVAR_NOT_FOUND)
        val = NULL;
      else
        val = "";
    }
  else if (len >= G_N_ELEMENTS (wvalue))
    {
      /* @wvalue isn’t big enough. Give up. */
      g_warning ("Unsupported G_SLICE value");
      val = NULL;
    }
  else
    {
      /* it’s safe to use g_utf16_to_utf8() here as it only allocates using
       * malloc() rather than GSlice */
      val = val_allocated = g_utf16_to_utf8 (wvalue, -1, NULL, NULL, NULL);
    }

  }
#endif  /* G_OS_WIN32 */

  if (val != NULL)
    {
      gint flags;
      const GDebugKey keys[] = {
        { "always-malloc", 1 << 0 },
        { "debug-blocks",  1 << 1 },
      };

      flags = g_parse_debug_string (val, keys, G_N_ELEMENTS (keys));
      if (flags & (1 << 0))
        config->always_malloc = TRUE;
      if (flags & (1 << 1))
        config->debug_blocks = TRUE;
    }
  else
    {
      /* G_SLICE was not specified, so check if valgrind is running and
       * disable ourselves if it is.
       *
       * This way it's possible to force gslice to be enabled under
       * valgrind just by setting G_SLICE to the empty string.
       */
#ifdef ENABLE_VALGRIND
      if (RUNNING_ON_VALGRIND)
        config->always_malloc = TRUE;
#endif
    }

  g_free (val_allocated);
#else // GSTREAMER_LITE
  *config = slice_config;
  config->always_malloc = TRUE;
#endif // GSTREAMER_LITE
}

static void
g_slice_init_nomessage (void)
{
  /* we may not use g_error() or friends here */
  mem_assert (sys_page_size == 0);
  mem_assert (MIN_MAGAZINE_SIZE >= 4);

#ifdef G_OS_WIN32
  {
    SYSTEM_INFO system_info;
    GetSystemInfo (&system_info);
    sys_page_size = system_info.dwPageSize;
  }
#else
  sys_page_size = sysconf (_SC_PAGESIZE); /* = sysconf (_SC_PAGE_SIZE); = getpagesize(); */
#endif
  mem_assert (sys_page_size >= 2 * LARGEALIGNMENT);
  mem_assert ((sys_page_size & (sys_page_size - 1)) == 0);
  slice_config_init (&allocator->config);
  allocator->min_page_size = sys_page_size;
#if HAVE_POSIX_MEMALIGN || HAVE_MEMALIGN
  /* allow allocation of pages up to 8KB (with 8KB alignment).
   * this is useful because many medium to large sized structures
   * fit less than 8 times (see [4]) into 4KB pages.
   * we allow very small page sizes here, to reduce wastage in
   * threads if only small allocations are required (this does
   * bear the risk of increasing allocation times and fragmentation
   * though).
   */
  allocator->min_page_size = MAX (allocator->min_page_size, 4096);
  allocator->max_page_size = MAX (allocator->min_page_size, 8192);
  allocator->min_page_size = MIN (allocator->min_page_size, 128);
#else
  /* we can only align to system page size */
  allocator->max_page_size = sys_page_size;
#endif
  if (allocator->config.always_malloc)
    {
      allocator->contention_counters = NULL;
      allocator->magazines = NULL;
      allocator->slab_stack = NULL;
    }
  else
    {
      allocator->contention_counters = g_new0 (guint, MAX_SLAB_INDEX (allocator));
      allocator->magazines = g_new0 (ChunkLink*, MAX_SLAB_INDEX (allocator));
      allocator->slab_stack = g_new0 (SlabInfo*, MAX_SLAB_INDEX (allocator));
    }

  allocator->mutex_counter = 0;
  allocator->stamp_counter = MAX_STAMP_COUNTER; /* force initial update */
  allocator->last_stamp = 0;
  allocator->color_accu = 0;
  magazine_cache_update_stamp();
  /* values cached for performance reasons */
  allocator->max_slab_chunk_size_for_magazine_cache = MAX_SLAB_CHUNK_SIZE (allocator);
  if (allocator->config.always_malloc || allocator->config.bypass_magazines)
    allocator->max_slab_chunk_size_for_magazine_cache = 0;      /* non-optimized cases */
}

static inline guint
allocator_categorize (gsize aligned_chunk_size)
{
  /* speed up the likely path */
  if (G_LIKELY (aligned_chunk_size && aligned_chunk_size <= allocator->max_slab_chunk_size_for_magazine_cache))
    return 1;           /* use magazine cache */

  if (!allocator->config.always_malloc &&
      aligned_chunk_size &&
      aligned_chunk_size <= MAX_SLAB_CHUNK_SIZE (allocator))
    {
      if (allocator->config.bypass_magazines)
        return 2;       /* use slab allocator, see [2] */
      return 1;         /* use magazine cache */
    }
  return 0;             /* use malloc() */
}

static inline void
g_mutex_lock_a (GMutex *mutex,
                guint  *contention_counter)
{
  gboolean contention = FALSE;
  if (!g_mutex_trylock (mutex))
    {
      g_mutex_lock (mutex);
      contention = TRUE;
    }
  if (contention)
    {
      allocator->mutex_counter++;
      if (allocator->mutex_counter >= 1)        /* quickly adapt to contention */
        {
          allocator->mutex_counter = 0;
          *contention_counter = MIN (*contention_counter + 1, MAX_MAGAZINE_SIZE);
        }
    }
  else /* !contention */
    {
      allocator->mutex_counter--;
      if (allocator->mutex_counter < -11)       /* moderately recover magazine sizes */
        {
          allocator->mutex_counter = 0;
          *contention_counter = MAX (*contention_counter, 1) - 1;
        }
    }
}

static inline ThreadMemory*
thread_memory_from_self (void)
{
  ThreadMemory *tmem = g_private_get (&private_thread_memory);
  if (G_UNLIKELY (!tmem))
    {
      static GMutex init_mutex;
      guint n_magazines;

      g_mutex_lock (&init_mutex);
      if G_UNLIKELY (sys_page_size == 0)
        g_slice_init_nomessage ();
      g_mutex_unlock (&init_mutex);

      n_magazines = MAX_SLAB_INDEX (allocator);
      tmem = g_private_set_alloc0 (&private_thread_memory, sizeof (ThreadMemory) + sizeof (Magazine) * 2 * n_magazines);
#ifdef GSTREAMER_LITE
      if (tmem == NULL)
        return NULL;
#endif // GSTREAMER_LITE
      tmem->magazine1 = (Magazine*) (tmem + 1);
      tmem->magazine2 = &tmem->magazine1[n_magazines];
    }
  return tmem;
}

static inline ChunkLink*
magazine_chain_pop_head (ChunkLink **magazine_chunks)
{
  /* magazine chains are linked via ChunkLink->next.
   * each ChunkLink->data of the toplevel chain may point to a subchain,
   * linked via ChunkLink->next. ChunkLink->data of the subchains just
   * contains uninitialized junk.
   */
  ChunkLink *chunk = (*magazine_chunks)->data;
  if (G_UNLIKELY (chunk))
    {
      /* allocating from freed list */
      (*magazine_chunks)->data = chunk->next;
    }
  else
    {
      chunk = *magazine_chunks;
      *magazine_chunks = chunk->next;
    }
  return chunk;
}

#if 0 /* useful for debugging */
static guint
magazine_count (ChunkLink *head)
{
  guint count = 0;
  if (!head)
    return 0;
  while (head)
    {
      ChunkLink *child = head->data;
      count += 1;
      for (child = head->data; child; child = child->next)
        count += 1;
      head = head->next;
    }
  return count;
}
#endif

static inline gsize
allocator_get_magazine_threshold (Allocator *allocator,
                                  guint      ix)
{
  /* the magazine size calculated here has a lower bound of MIN_MAGAZINE_SIZE,
   * which is required by the implementation. also, for moderately sized chunks
   * (say >= 64 bytes), magazine sizes shouldn't be much smaller then the number
   * of chunks available per page/2 to avoid excessive traffic in the magazine
   * cache for small to medium sized structures.
   * the upper bound of the magazine size is effectively provided by
   * MAX_MAGAZINE_SIZE. for larger chunks, this number is scaled down so that
   * the content of a single magazine doesn't exceed ca. 16KB.
   */
  gsize chunk_size = SLAB_CHUNK_SIZE (allocator, ix);
  guint threshold = MAX (MIN_MAGAZINE_SIZE, allocator->max_page_size / MAX (5 * chunk_size, 5 * 32));
  guint contention_counter = allocator->contention_counters[ix];
  if (G_UNLIKELY (contention_counter))  /* single CPU bias */
    {
      /* adapt contention counter thresholds to chunk sizes */
      contention_counter = contention_counter * 64 / chunk_size;
      threshold = MAX (threshold, contention_counter);
    }
  return threshold;
}

/* --- magazine cache --- */
static inline void
magazine_cache_update_stamp (void)
{
  if (allocator->stamp_counter >= MAX_STAMP_COUNTER)
    {
      gint64 now_us = g_get_real_time ();
      allocator->last_stamp = now_us / 1000; /* milli seconds */
      allocator->stamp_counter = 0;
    }
  else
    allocator->stamp_counter++;
}

static inline ChunkLink*
magazine_chain_prepare_fields (ChunkLink *magazine_chunks)
{
  ChunkLink *chunk1;
  ChunkLink *chunk2;
  ChunkLink *chunk3;
  ChunkLink *chunk4;
  /* checked upon initialization: mem_assert (MIN_MAGAZINE_SIZE >= 4); */
  /* ensure a magazine with at least 4 unused data pointers */
  chunk1 = magazine_chain_pop_head (&magazine_chunks);
  chunk2 = magazine_chain_pop_head (&magazine_chunks);
  chunk3 = magazine_chain_pop_head (&magazine_chunks);
  chunk4 = magazine_chain_pop_head (&magazine_chunks);
  chunk4->next = magazine_chunks;
  chunk3->next = chunk4;
  chunk2->next = chunk3;
  chunk1->next = chunk2;
  return chunk1;
}

/* access the first 3 fields of a specially prepared magazine chain */
#define magazine_chain_prev(mc)         ((mc)->data)
#define magazine_chain_stamp(mc)        ((mc)->next->data)
#define magazine_chain_uint_stamp(mc)   GPOINTER_TO_UINT ((mc)->next->data)
#define magazine_chain_next(mc)         ((mc)->next->next->data)
#define magazine_chain_count(mc)        ((mc)->next->next->next->data)

static void
magazine_cache_trim (Allocator *allocator,
                     guint      ix,
                     guint      stamp)
{
  /* g_mutex_lock (allocator->mutex); done by caller */
  /* trim magazine cache from tail */
  ChunkLink *current = magazine_chain_prev (allocator->magazines[ix]);
  ChunkLink *trash = NULL;
  while (!G_APPROX_VALUE(stamp, magazine_chain_uint_stamp (current),
                         allocator->config.working_set_msecs))
    {
      /* unlink */
      ChunkLink *prev = magazine_chain_prev (current);
      ChunkLink *next = magazine_chain_next (current);
      magazine_chain_next (prev) = next;
      magazine_chain_prev (next) = prev;
      /* clear special fields, put on trash stack */
      magazine_chain_next (current) = NULL;
      magazine_chain_count (current) = NULL;
      magazine_chain_stamp (current) = NULL;
      magazine_chain_prev (current) = trash;
      trash = current;
      /* fixup list head if required */
      if (current == allocator->magazines[ix])
        {
          allocator->magazines[ix] = NULL;
          break;
        }
      current = prev;
    }
  g_mutex_unlock (&allocator->magazine_mutex);
  /* free trash */
  if (trash)
    {
      const gsize chunk_size = SLAB_CHUNK_SIZE (allocator, ix);
      g_mutex_lock (&allocator->slab_mutex);
      while (trash)
        {
          current = trash;
          trash = magazine_chain_prev (current);
          magazine_chain_prev (current) = NULL; /* clear special field */
          while (current)
            {
              ChunkLink *chunk = magazine_chain_pop_head (&current);
              slab_allocator_free_chunk (chunk_size, chunk);
            }
        }
      g_mutex_unlock (&allocator->slab_mutex);
    }
}

static void
magazine_cache_push_magazine (guint      ix,
                              ChunkLink *magazine_chunks,
                              gsize      count) /* must be >= MIN_MAGAZINE_SIZE */
{
  ChunkLink *current = magazine_chain_prepare_fields (magazine_chunks);
  ChunkLink *next, *prev;
  g_mutex_lock (&allocator->magazine_mutex);
  /* add magazine at head */
  next = allocator->magazines[ix];
  if (next)
    prev = magazine_chain_prev (next);
  else
    next = prev = current;
  magazine_chain_next (prev) = current;
  magazine_chain_prev (next) = current;
  magazine_chain_prev (current) = prev;
  magazine_chain_next (current) = next;
  magazine_chain_count (current) = (gpointer) count;
  /* stamp magazine */
  magazine_cache_update_stamp();
  magazine_chain_stamp (current) = GUINT_TO_POINTER (allocator->last_stamp);
  allocator->magazines[ix] = current;
  /* free old magazines beyond a certain threshold */
  magazine_cache_trim (allocator, ix, allocator->last_stamp);
  /* g_mutex_unlock (allocator->mutex); was done by magazine_cache_trim() */
}

static ChunkLink*
magazine_cache_pop_magazine (guint  ix,
                             gsize *countp)
{
  g_mutex_lock_a (&allocator->magazine_mutex, &allocator->contention_counters[ix]);
  if (!allocator->magazines[ix])
    {
      guint magazine_threshold = allocator_get_magazine_threshold (allocator, ix);
      gsize i, chunk_size = SLAB_CHUNK_SIZE (allocator, ix);
      ChunkLink *chunk, *head;
      g_mutex_unlock (&allocator->magazine_mutex);
      g_mutex_lock (&allocator->slab_mutex);
      head = slab_allocator_alloc_chunk (chunk_size);
      head->data = NULL;
      chunk = head;
      for (i = 1; i < magazine_threshold; i++)
        {
          chunk->next = slab_allocator_alloc_chunk (chunk_size);
          chunk = chunk->next;
          chunk->data = NULL;
        }
      chunk->next = NULL;
      g_mutex_unlock (&allocator->slab_mutex);
      *countp = i;
      return head;
    }
  else
    {
      ChunkLink *current = allocator->magazines[ix];
      ChunkLink *prev = magazine_chain_prev (current);
      ChunkLink *next = magazine_chain_next (current);
      /* unlink */
      magazine_chain_next (prev) = next;
      magazine_chain_prev (next) = prev;
      allocator->magazines[ix] = next == current ? NULL : next;
      g_mutex_unlock (&allocator->magazine_mutex);
      /* clear special fields and hand out */
      *countp = (gsize) magazine_chain_count (current);
      magazine_chain_prev (current) = NULL;
      magazine_chain_next (current) = NULL;
      magazine_chain_count (current) = NULL;
      magazine_chain_stamp (current) = NULL;
      return current;
    }
}

/* --- thread magazines --- */
static void
private_thread_memory_cleanup (gpointer data)
{
  ThreadMemory *tmem = data;
  const guint n_magazines = MAX_SLAB_INDEX (allocator);
  guint ix;
  for (ix = 0; ix < n_magazines; ix++)
    {
      Magazine *mags[2];
      guint j;
      mags[0] = &tmem->magazine1[ix];
      mags[1] = &tmem->magazine2[ix];
      for (j = 0; j < 2; j++)
        {
          Magazine *mag = mags[j];
          if (mag->count >= MIN_MAGAZINE_SIZE)
            magazine_cache_push_magazine (ix, mag->chunks, mag->count);
          else
            {
              const gsize chunk_size = SLAB_CHUNK_SIZE (allocator, ix);
              g_mutex_lock (&allocator->slab_mutex);
              while (mag->chunks)
                {
                  ChunkLink *chunk = magazine_chain_pop_head (&mag->chunks);
                  slab_allocator_free_chunk (chunk_size, chunk);
                }
              g_mutex_unlock (&allocator->slab_mutex);
            }
        }
    }
  g_free (tmem);
}

static void
thread_memory_magazine1_reload (ThreadMemory *tmem,
                                guint         ix)
{
  Magazine *mag = &tmem->magazine1[ix];
  mem_assert (mag->chunks == NULL); /* ensure that we may reset mag->count */
  mag->count = 0;
  mag->chunks = magazine_cache_pop_magazine (ix, &mag->count);
}

static void
thread_memory_magazine2_unload (ThreadMemory *tmem,
                                guint         ix)
{
  Magazine *mag = &tmem->magazine2[ix];
  magazine_cache_push_magazine (ix, mag->chunks, mag->count);
  mag->chunks = NULL;
  mag->count = 0;
}

static inline void
thread_memory_swap_magazines (ThreadMemory *tmem,
                              guint         ix)
{
  Magazine xmag = tmem->magazine1[ix];
  tmem->magazine1[ix] = tmem->magazine2[ix];
  tmem->magazine2[ix] = xmag;
}

static inline gboolean
thread_memory_magazine1_is_empty (ThreadMemory *tmem,
                                  guint         ix)
{
  return tmem->magazine1[ix].chunks == NULL;
}

static inline gboolean
thread_memory_magazine2_is_full (ThreadMemory *tmem,
                                 guint         ix)
{
  return tmem->magazine2[ix].count >= allocator_get_magazine_threshold (allocator, ix);
}

static inline gpointer
thread_memory_magazine1_alloc (ThreadMemory *tmem,
                               guint         ix)
{
  Magazine *mag = &tmem->magazine1[ix];
  ChunkLink *chunk = magazine_chain_pop_head (&mag->chunks);
  if (G_LIKELY (mag->count > 0))
    mag->count--;
  return chunk;
}

static inline void
thread_memory_magazine2_free (ThreadMemory *tmem,
                              guint         ix,
                              gpointer      mem)
{
  Magazine *mag = &tmem->magazine2[ix];
  ChunkLink *chunk = mem;
  chunk->data = NULL;
  chunk->next = mag->chunks;
  mag->chunks = chunk;
  mag->count++;
}

/* --- API functions --- */

/**
 * g_slice_new:
 * @type: the type to allocate, typically a structure name
 *
 * A convenience macro to allocate a block of memory from the
 * slice allocator.
 *
 * It calls g_slice_alloc() with `sizeof (@type)` and casts the
 * returned pointer to a pointer of the given type, avoiding a type
 * cast in the source code. Note that the underlying slice allocation
 * mechanism can be changed with the [`G_SLICE=always-malloc`][G_SLICE]
 * environment variable.
 *
 * This can never return %NULL as the minimum allocation size from
 * `sizeof (@type)` is 1 byte.
 *
 * Returns: (not nullable): a pointer to the allocated block, cast to a pointer
 *    to @type
 *
 * Since: 2.10
 */

/**
 * g_slice_new0:
 * @type: the type to allocate, typically a structure name
 *
 * A convenience macro to allocate a block of memory from the
 * slice allocator and set the memory to 0.
 *
 * It calls g_slice_alloc0() with `sizeof (@type)`
 * and casts the returned pointer to a pointer of the given type,
 * avoiding a type cast in the source code.
 * Note that the underlying slice allocation mechanism can
 * be changed with the [`G_SLICE=always-malloc`][G_SLICE]
 * environment variable.
 *
 * This can never return %NULL as the minimum allocation size from
 * `sizeof (@type)` is 1 byte.
 *
 * Returns: (not nullable): a pointer to the allocated block, cast to a pointer
 *    to @type
 *
 * Since: 2.10
 */

/**
 * g_slice_dup:
 * @type: the type to duplicate, typically a structure name
 * @mem: (not nullable): the memory to copy into the allocated block
 *
 * A convenience macro to duplicate a block of memory using
 * the slice allocator.
 *
 * It calls g_slice_copy() with `sizeof (@type)`
 * and casts the returned pointer to a pointer of the given type,
 * avoiding a type cast in the source code.
 * Note that the underlying slice allocation mechanism can
 * be changed with the [`G_SLICE=always-malloc`][G_SLICE]
 * environment variable.
 *
 * This can never return %NULL.
 *
 * Returns: (not nullable): a pointer to the allocated block, cast to a pointer
 *    to @type
 *
 * Since: 2.14
 */

/**
 * g_slice_free:
 * @type: the type of the block to free, typically a structure name
 * @mem: a pointer to the block to free
 *
 * A convenience macro to free a block of memory that has
 * been allocated from the slice allocator.
 *
 * It calls g_slice_free1() using `sizeof (type)`
 * as the block size.
 * Note that the exact release behaviour can be changed with the
 * [`G_DEBUG=gc-friendly`][G_DEBUG] environment variable, also see
 * [`G_SLICE`][G_SLICE] for related debugging options.
 *
 * If @mem is %NULL, this macro does nothing.
 *
 * Since: 2.10
 */

/**
 * g_slice_free_chain:
 * @type: the type of the @mem_chain blocks
 * @mem_chain: a pointer to the first block of the chain
 * @next: the field name of the next pointer in @type
 *
 * Frees a linked list of memory blocks of structure type @type.
 * The memory blocks must be equal-sized, allocated via
 * g_slice_alloc() or g_slice_alloc0() and linked together by
 * a @next pointer (similar to #GSList). The name of the
 * @next field in @type is passed as third argument.
 * Note that the exact release behaviour can be changed with the
 * [`G_DEBUG=gc-friendly`][G_DEBUG] environment variable, also see
 * [`G_SLICE`][G_SLICE] for related debugging options.
 *
 * If @mem_chain is %NULL, this function does nothing.
 *
 * Since: 2.10
 */

/**
 * g_slice_alloc:
 * @block_size: the number of bytes to allocate
 *
 * Allocates a block of memory from the slice allocator.
 * The block address handed out can be expected to be aligned
 * to at least 1 * sizeof (void*),
 * though in general slices are 2 * sizeof (void*) bytes aligned,
 * if a malloc() fallback implementation is used instead,
 * the alignment may be reduced in a libc dependent fashion.
 * Note that the underlying slice allocation mechanism can
 * be changed with the [`G_SLICE=always-malloc`][G_SLICE]
 * environment variable.
 *
 * Returns: a pointer to the allocated memory block, which will be %NULL if and
 *    only if @mem_size is 0
 *
 * Since: 2.10
 */
gpointer
g_slice_alloc (gsize mem_size)
{
  ThreadMemory *tmem;
  gsize chunk_size;
  gpointer mem;
  guint acat;

  /* This gets the private structure for this thread.  If the private
   * structure does not yet exist, it is created.
   *
   * This has a side effect of causing GSlice to be initialised, so it
   * must come first.
   */
  tmem = thread_memory_from_self ();
#ifdef GSTREAMER_LITE
  if (tmem == NULL)
    return NULL;
#endif // GSTREAMER_LITE

  chunk_size = P2ALIGN (mem_size);
  acat = allocator_categorize (chunk_size);
  if (G_LIKELY (acat == 1))     /* allocate through magazine layer */
    {
      guint ix = SLAB_INDEX (allocator, chunk_size);
      if (G_UNLIKELY (thread_memory_magazine1_is_empty (tmem, ix)))
        {
          thread_memory_swap_magazines (tmem, ix);
          if (G_UNLIKELY (thread_memory_magazine1_is_empty (tmem, ix)))
            thread_memory_magazine1_reload (tmem, ix);
        }
      mem = thread_memory_magazine1_alloc (tmem, ix);
    }
  else if (acat == 2)           /* allocate through slab allocator */
    {
      g_mutex_lock (&allocator->slab_mutex);
      mem = slab_allocator_alloc_chunk (chunk_size);
      g_mutex_unlock (&allocator->slab_mutex);
    }
  else                          /* delegate to system malloc */
    mem = g_malloc (mem_size);
  if (G_UNLIKELY (allocator->config.debug_blocks))
    smc_notify_alloc (mem, mem_size);

  TRACE (GLIB_SLICE_ALLOC((void*)mem, mem_size));

  return mem;
}

/**
 * g_slice_alloc0:
 * @block_size: the number of bytes to allocate
 *
 * Allocates a block of memory via g_slice_alloc() and initializes
 * the returned memory to 0. Note that the underlying slice allocation
 * mechanism can be changed with the [`G_SLICE=always-malloc`][G_SLICE]
 * environment variable.
 *
 * Returns: a pointer to the allocated block, which will be %NULL if and only
 *    if @mem_size is 0
 *
 * Since: 2.10
 */
gpointer
g_slice_alloc0 (gsize mem_size)
{
  gpointer mem = g_slice_alloc (mem_size);
  if (mem)
    memset (mem, 0, mem_size);
  return mem;
}

/**
 * g_slice_copy:
 * @block_size: the number of bytes to allocate
 * @mem_block: the memory to copy
 *
 * Allocates a block of memory from the slice allocator
 * and copies @block_size bytes into it from @mem_block.
 *
 * @mem_block must be non-%NULL if @block_size is non-zero.
 *
 * Returns: a pointer to the allocated memory block, which will be %NULL if and
 *    only if @mem_size is 0
 *
 * Since: 2.14
 */
gpointer
g_slice_copy (gsize         mem_size,
              gconstpointer mem_block)
{
  gpointer mem = g_slice_alloc (mem_size);
  if (mem)
    memcpy (mem, mem_block, mem_size);
  return mem;
}

/**
 * g_slice_free1:
 * @block_size: the size of the block
 * @mem_block: a pointer to the block to free
 *
 * Frees a block of memory.
 *
 * The memory must have been allocated via g_slice_alloc() or
 * g_slice_alloc0() and the @block_size has to match the size
 * specified upon allocation. Note that the exact release behaviour
 * can be changed with the [`G_DEBUG=gc-friendly`][G_DEBUG] environment
 * variable, also see [`G_SLICE`][G_SLICE] for related debugging options.
 *
 * If @mem_block is %NULL, this function does nothing.
 *
 * Since: 2.10
 */
void
g_slice_free1 (gsize    mem_size,
               gpointer mem_block)
{
  gsize chunk_size = P2ALIGN (mem_size);
  guint acat = allocator_categorize (chunk_size);
  if (G_UNLIKELY (!mem_block))
    return;
  if (G_UNLIKELY (allocator->config.debug_blocks) &&
      !smc_notify_free (mem_block, mem_size))
    abort();
  if (G_LIKELY (acat == 1))             /* allocate through magazine layer */
    {
      ThreadMemory *tmem = thread_memory_from_self();
      guint ix = SLAB_INDEX (allocator, chunk_size);
#ifdef GSTREAMER_LITE
      if (tmem == NULL)
        return; // Nothing to free
#endif // GSTREAMER_LITE
      if (G_UNLIKELY (thread_memory_magazine2_is_full (tmem, ix)))
        {
          thread_memory_swap_magazines (tmem, ix);
          if (G_UNLIKELY (thread_memory_magazine2_is_full (tmem, ix)))
            thread_memory_magazine2_unload (tmem, ix);
        }
      if (G_UNLIKELY (g_mem_gc_friendly))
        memset (mem_block, 0, chunk_size);
      thread_memory_magazine2_free (tmem, ix, mem_block);
    }
  else if (acat == 2)                   /* allocate through slab allocator */
    {
      if (G_UNLIKELY (g_mem_gc_friendly))
        memset (mem_block, 0, chunk_size);
      g_mutex_lock (&allocator->slab_mutex);
      slab_allocator_free_chunk (chunk_size, mem_block);
      g_mutex_unlock (&allocator->slab_mutex);
    }
  else                                  /* delegate to system malloc */
    {
      if (G_UNLIKELY (g_mem_gc_friendly))
        memset (mem_block, 0, mem_size);
      g_free (mem_block);
    }
  TRACE (GLIB_SLICE_FREE((void*)mem_block, mem_size));
}

/**
 * g_slice_free_chain_with_offset:
 * @block_size: the size of the blocks
 * @mem_chain:  a pointer to the first block of the chain
 * @next_offset: the offset of the @next field in the blocks
 *
 * Frees a linked list of memory blocks of structure type @type.
 *
 * The memory blocks must be equal-sized, allocated via
 * g_slice_alloc() or g_slice_alloc0() and linked together by a
 * @next pointer (similar to #GSList). The offset of the @next
 * field in each block is passed as third argument.
 * Note that the exact release behaviour can be changed with the
 * [`G_DEBUG=gc-friendly`][G_DEBUG] environment variable, also see
 * [`G_SLICE`][G_SLICE] for related debugging options.
 *
 * If @mem_chain is %NULL, this function does nothing.
 *
 * Since: 2.10
 */
void
g_slice_free_chain_with_offset (gsize    mem_size,
                                gpointer mem_chain,
                                gsize    next_offset)
{
  gpointer slice = mem_chain;
  /* while the thread magazines and the magazine cache are implemented so that
   * they can easily be extended to allow for free lists containing more free
   * lists for the first level nodes, which would allow O(1) freeing in this
   * function, the benefit of such an extension is questionable, because:
   * - the magazine size counts will become mere lower bounds which confuses
   *   the code adapting to lock contention;
   * - freeing a single node to the thread magazines is very fast, so this
   *   O(list_length) operation is multiplied by a fairly small factor;
   * - memory usage histograms on larger applications seem to indicate that
   *   the amount of released multi node lists is negligible in comparison
   *   to single node releases.
   * - the major performance bottle neck, namely g_private_get() or
   *   g_mutex_lock()/g_mutex_unlock() has already been moved out of the
   *   inner loop for freeing chained slices.
   */
  gsize chunk_size = P2ALIGN (mem_size);
  guint acat = allocator_categorize (chunk_size);
  if (G_LIKELY (acat == 1))             /* allocate through magazine layer */
    {
      ThreadMemory *tmem = thread_memory_from_self();
      guint ix = SLAB_INDEX (allocator, chunk_size);
#ifdef GSTREAMER_LITE
      if (tmem == NULL)
        return; // Nothing to free
#endif // GSTREAMER_LITE
      while (slice)
        {
          guint8 *current = slice;
          slice = *(gpointer*) (current + next_offset);
          if (G_UNLIKELY (allocator->config.debug_blocks) &&
              !smc_notify_free (current, mem_size))
            abort();
          if (G_UNLIKELY (thread_memory_magazine2_is_full (tmem, ix)))
            {
              thread_memory_swap_magazines (tmem, ix);
              if (G_UNLIKELY (thread_memory_magazine2_is_full (tmem, ix)))
                thread_memory_magazine2_unload (tmem, ix);
            }
          if (G_UNLIKELY (g_mem_gc_friendly))
            memset (current, 0, chunk_size);
          thread_memory_magazine2_free (tmem, ix, current);
        }
    }
  else if (acat == 2)                   /* allocate through slab allocator */
    {
      g_mutex_lock (&allocator->slab_mutex);
      while (slice)
        {
          guint8 *current = slice;
          slice = *(gpointer*) (current + next_offset);
          if (G_UNLIKELY (allocator->config.debug_blocks) &&
              !smc_notify_free (current, mem_size))
            abort();
          if (G_UNLIKELY (g_mem_gc_friendly))
            memset (current, 0, chunk_size);
          slab_allocator_free_chunk (chunk_size, current);
        }
      g_mutex_unlock (&allocator->slab_mutex);
    }
  else                                  /* delegate to system malloc */
    while (slice)
      {
        guint8 *current = slice;
        slice = *(gpointer*) (current + next_offset);
        if (G_UNLIKELY (allocator->config.debug_blocks) &&
            !smc_notify_free (current, mem_size))
          abort();
        if (G_UNLIKELY (g_mem_gc_friendly))
          memset (current, 0, mem_size);
        g_free (current);
      }
}

/* --- single page allocator --- */
static void
allocator_slab_stack_push (Allocator *allocator,
                           guint      ix,
                           SlabInfo  *sinfo)
{
  /* insert slab at slab ring head */
  if (!allocator->slab_stack[ix])
    {
      sinfo->next = sinfo;
      sinfo->prev = sinfo;
    }
  else
    {
      SlabInfo *next = allocator->slab_stack[ix], *prev = next->prev;
      next->prev = sinfo;
      prev->next = sinfo;
      sinfo->next = next;
      sinfo->prev = prev;
    }
  allocator->slab_stack[ix] = sinfo;
}

static gsize
allocator_aligned_page_size (Allocator *allocator,
                             gsize      n_bytes)
{
  gsize val = 1 << g_bit_storage (n_bytes - 1);
  val = MAX (val, allocator->min_page_size);
  return val;
}

static void
allocator_add_slab (Allocator *allocator,
                    guint      ix,
                    gsize      chunk_size)
{
  ChunkLink *chunk;
  SlabInfo *sinfo;
  gsize addr, padding, n_chunks, color = 0;
  gsize page_size;
  int errsv;
  gpointer aligned_memory;
  guint8 *mem;
  guint i;

  page_size = allocator_aligned_page_size (allocator, SLAB_BPAGE_SIZE (allocator, chunk_size));
  /* allocate 1 page for the chunks and the slab */
  aligned_memory = allocator_memalign (page_size, page_size - NATIVE_MALLOC_PADDING);
  errsv = errno;
  mem = aligned_memory;

  if (!mem)
    {
      const gchar *syserr = strerror (errsv);
      mem_error ("failed to allocate %u bytes (alignment: %u): %s\n",
                 (guint) (page_size - NATIVE_MALLOC_PADDING), (guint) page_size, syserr);
    }
  /* mask page address */
  addr = ((gsize) mem / page_size) * page_size;
  /* assert alignment */
  mem_assert (aligned_memory == (gpointer) addr);
  /* basic slab info setup */
  sinfo = (SlabInfo*) (mem + page_size - SLAB_INFO_SIZE);
  sinfo->n_allocated = 0;
  sinfo->chunks = NULL;
  /* figure cache colorization */
  n_chunks = ((guint8*) sinfo - mem) / chunk_size;
  padding = ((guint8*) sinfo - mem) - n_chunks * chunk_size;
  if (padding)
    {
      color = (allocator->color_accu * P2ALIGNMENT) % padding;
      allocator->color_accu += allocator->config.color_increment;
    }
  /* add chunks to free list */
  chunk = (ChunkLink*) (mem + color);
  sinfo->chunks = chunk;
  for (i = 0; i < n_chunks - 1; i++)
    {
      chunk->next = (ChunkLink*) ((guint8*) chunk + chunk_size);
      chunk = chunk->next;
    }
  chunk->next = NULL;   /* last chunk */
  /* add slab to slab ring */
  allocator_slab_stack_push (allocator, ix, sinfo);
}

static gpointer
slab_allocator_alloc_chunk (gsize chunk_size)
{
  ChunkLink *chunk;
  guint ix = SLAB_INDEX (allocator, chunk_size);
  /* ensure non-empty slab */
  if (!allocator->slab_stack[ix] || !allocator->slab_stack[ix]->chunks)
    allocator_add_slab (allocator, ix, chunk_size);
  /* allocate chunk */
  chunk = allocator->slab_stack[ix]->chunks;
  allocator->slab_stack[ix]->chunks = chunk->next;
  allocator->slab_stack[ix]->n_allocated++;
  /* rotate empty slabs */
  if (!allocator->slab_stack[ix]->chunks)
    allocator->slab_stack[ix] = allocator->slab_stack[ix]->next;
  return chunk;
}

static void
slab_allocator_free_chunk (gsize    chunk_size,
                           gpointer mem)
{
  ChunkLink *chunk;
  gboolean was_empty;
  guint ix = SLAB_INDEX (allocator, chunk_size);
  gsize page_size = allocator_aligned_page_size (allocator, SLAB_BPAGE_SIZE (allocator, chunk_size));
  gsize addr = ((gsize) mem / page_size) * page_size;
  /* mask page address */
  guint8 *page = (guint8*) addr;
  SlabInfo *sinfo = (SlabInfo*) (page + page_size - SLAB_INFO_SIZE);
  /* assert valid chunk count */
  mem_assert (sinfo->n_allocated > 0);
  /* add chunk to free list */
  was_empty = sinfo->chunks == NULL;
  chunk = (ChunkLink*) mem;
  chunk->next = sinfo->chunks;
  sinfo->chunks = chunk;
  sinfo->n_allocated--;
  /* keep slab ring partially sorted, empty slabs at end */
  if (was_empty)
    {
      /* unlink slab */
      SlabInfo *next = sinfo->next, *prev = sinfo->prev;
      next->prev = prev;
      prev->next = next;
      if (allocator->slab_stack[ix] == sinfo)
        allocator->slab_stack[ix] = next == sinfo ? NULL : next;
      /* insert slab at head */
      allocator_slab_stack_push (allocator, ix, sinfo);
    }
  /* eagerly free complete unused slabs */
  if (!sinfo->n_allocated)
    {
      /* unlink slab */
      SlabInfo *next = sinfo->next, *prev = sinfo->prev;
      next->prev = prev;
      prev->next = next;
      if (allocator->slab_stack[ix] == sinfo)
        allocator->slab_stack[ix] = next == sinfo ? NULL : next;
      /* free slab */
      allocator_memfree (page_size, page);
    }
}

/* --- memalign implementation --- */
#ifdef HAVE_MALLOC_H
#include <malloc.h>             /* memalign() */
#endif

/* from config.h:
 * define HAVE_POSIX_MEMALIGN           1 // if free(posix_memalign(3)) works, <stdlib.h>
 * define HAVE_MEMALIGN                 1 // if free(memalign(3)) works, <malloc.h>
 * define HAVE_VALLOC                   1 // if free(valloc(3)) works, <stdlib.h> or <malloc.h>
 * if none is provided, we implement malloc(3)-based alloc-only page alignment
 */

#if !(HAVE_POSIX_MEMALIGN || HAVE_MEMALIGN || HAVE_VALLOC)
G_GNUC_BEGIN_IGNORE_DEPRECATIONS
static GTrashStack *compat_valloc_trash = NULL;
G_GNUC_END_IGNORE_DEPRECATIONS
#endif

static gpointer
allocator_memalign (gsize alignment,
                    gsize memsize)
{
  gpointer aligned_memory = NULL;
  gint err = ENOMEM;
#if     HAVE_POSIX_MEMALIGN
  err = posix_memalign (&aligned_memory, alignment, memsize);
#elif   HAVE_MEMALIGN
  errno = 0;
  aligned_memory = memalign (alignment, memsize);
  err = errno;
#elif   HAVE_VALLOC
  errno = 0;
  aligned_memory = valloc (memsize);
  err = errno;
#else
  /* simplistic non-freeing page allocator */
  mem_assert (alignment == sys_page_size);
  mem_assert (memsize <= sys_page_size);
  if (!compat_valloc_trash)
    {
      const guint n_pages = 16;
      guint8 *mem = malloc (n_pages * sys_page_size);
      err = errno;
      if (mem)
        {
          gint i = n_pages;
          guint8 *amem = (guint8*) ALIGN ((gsize) mem, sys_page_size);
          if (amem != mem)
            i--;        /* mem wasn't page aligned */
          G_GNUC_BEGIN_IGNORE_DEPRECATIONS
          while (--i >= 0)
            g_trash_stack_push (&compat_valloc_trash, amem + i * sys_page_size);
          G_GNUC_END_IGNORE_DEPRECATIONS
        }
    }
  G_GNUC_BEGIN_IGNORE_DEPRECATIONS
  aligned_memory = g_trash_stack_pop (&compat_valloc_trash);
  G_GNUC_END_IGNORE_DEPRECATIONS
#endif
  if (!aligned_memory)
    errno = err;
  return aligned_memory;
}

static void
allocator_memfree (gsize    memsize,
                   gpointer mem)
{
#if     HAVE_POSIX_MEMALIGN || HAVE_MEMALIGN || HAVE_VALLOC
  free (mem);
#else
  mem_assert (memsize <= sys_page_size);
  G_GNUC_BEGIN_IGNORE_DEPRECATIONS
  g_trash_stack_push (&compat_valloc_trash, mem);
  G_GNUC_END_IGNORE_DEPRECATIONS
#endif
}

static void
mem_error (const char *format,
           ...)
{
  const char *pname;
  va_list args;
  /* at least, put out "MEMORY-ERROR", in case we segfault during the rest of the function */
  fputs ("\n***MEMORY-ERROR***: ", stderr);
  pname = g_get_prgname();
  g_fprintf (stderr, "%s[%ld]: GSlice: ", pname ? pname : "", (long)getpid());
  va_start (args, format);
  g_vfprintf (stderr, format, args);
  va_end (args);
  fputs ("\n", stderr);
  abort();
  _exit (1);
}

/* --- g-slice memory checker tree --- */
typedef size_t SmcKType;                /* key type */
typedef size_t SmcVType;                /* value type */
typedef struct {
  SmcKType key;
  SmcVType value;
} SmcEntry;
static void             smc_tree_insert      (SmcKType  key,
                                              SmcVType  value);
static gboolean         smc_tree_lookup      (SmcKType  key,
                                              SmcVType *value_p);
static gboolean         smc_tree_remove      (SmcKType  key);


/* --- g-slice memory checker implementation --- */
static void
smc_notify_alloc (void   *pointer,
                  size_t  size)
{
  size_t address = (size_t) pointer;
  if (pointer)
    smc_tree_insert (address, size);
}

#if 0
static void
smc_notify_ignore (void *pointer)
{
  size_t address = (size_t) pointer;
  if (pointer)
    smc_tree_remove (address);
}
#endif

static int
smc_notify_free (void   *pointer,
                 size_t  size)
{
  size_t address = (size_t) pointer;
  SmcVType real_size;
  gboolean found_one;

  if (!pointer)
    return 1; /* ignore */
  found_one = smc_tree_lookup (address, &real_size);
  if (!found_one)
    {
      g_fprintf (stderr, "GSlice: MemChecker: attempt to release non-allocated block: %p size=%" G_GSIZE_FORMAT "\n", pointer, size);
      return 0;
    }
  if (real_size != size && (real_size || size))
    {
      g_fprintf (stderr, "GSlice: MemChecker: attempt to release block with invalid size: %p size=%" G_GSIZE_FORMAT " invalid-size=%" G_GSIZE_FORMAT "\n", pointer, real_size, size);
      return 0;
    }
  if (!smc_tree_remove (address))
    {
      g_fprintf (stderr, "GSlice: MemChecker: attempt to release non-allocated block: %p size=%" G_GSIZE_FORMAT "\n", pointer, size);
      return 0;
    }
  return 1; /* all fine */
}

/* --- g-slice memory checker tree implementation --- */
#define SMC_TRUNK_COUNT     (4093 /* 16381 */)          /* prime, to distribute trunk collisions (big, allocated just once) */
#define SMC_BRANCH_COUNT    (511)                       /* prime, to distribute branch collisions */
#define SMC_TRUNK_EXTENT    (SMC_BRANCH_COUNT * 2039)   /* key address space per trunk, should distribute uniformly across BRANCH_COUNT */
#define SMC_TRUNK_HASH(k)   ((k / SMC_TRUNK_EXTENT) % SMC_TRUNK_COUNT)  /* generate new trunk hash per megabyte (roughly) */
#define SMC_BRANCH_HASH(k)  (k % SMC_BRANCH_COUNT)

typedef struct {
  SmcEntry    *entries;
  unsigned int n_entries;
} SmcBranch;

static SmcBranch     **smc_tree_root = NULL;

static void
smc_tree_abort (int errval)
{
  const char *syserr = strerror (errval);
  mem_error ("MemChecker: failure in debugging tree: %s", syserr);
}

static inline SmcEntry*
smc_tree_branch_grow_L (SmcBranch   *branch,
                        unsigned int index)
{
  unsigned int old_size = branch->n_entries * sizeof (branch->entries[0]);
  unsigned int new_size = old_size + sizeof (branch->entries[0]);
  SmcEntry *entry;
  mem_assert (index <= branch->n_entries);
  branch->entries = (SmcEntry*) realloc (branch->entries, new_size);
  if (!branch->entries)
    smc_tree_abort (errno);
  entry = branch->entries + index;
  memmove (entry + 1, entry, (branch->n_entries - index) * sizeof (entry[0]));
  branch->n_entries += 1;
  return entry;
}

static inline SmcEntry*
smc_tree_branch_lookup_nearest_L (SmcBranch *branch,
                                  SmcKType   key)
{
  unsigned int n_nodes = branch->n_entries, offs = 0;
  SmcEntry *check = branch->entries;
  int cmp = 0;
  while (offs < n_nodes)
    {
      unsigned int i = (offs + n_nodes) >> 1;
      check = branch->entries + i;
      cmp = key < check->key ? -1 : key != check->key;
      if (cmp == 0)
        return check;                   /* return exact match */
      else if (cmp < 0)
        n_nodes = i;
      else /* (cmp > 0) */
        offs = i + 1;
    }
  /* check points at last mismatch, cmp > 0 indicates greater key */
  return cmp > 0 ? check + 1 : check;   /* return insertion position for inexact match */
}

static void
smc_tree_insert (SmcKType key,
                 SmcVType value)
{
  unsigned int ix0, ix1;
  SmcEntry *entry;

  g_mutex_lock (&smc_tree_mutex);
  ix0 = SMC_TRUNK_HASH (key);
  ix1 = SMC_BRANCH_HASH (key);
  if (!smc_tree_root)
    {
      smc_tree_root = calloc (SMC_TRUNK_COUNT, sizeof (smc_tree_root[0]));
      if (!smc_tree_root)
        smc_tree_abort (errno);
    }
  if (!smc_tree_root[ix0])
    {
      smc_tree_root[ix0] = calloc (SMC_BRANCH_COUNT, sizeof (smc_tree_root[0][0]));
      if (!smc_tree_root[ix0])
        smc_tree_abort (errno);
    }
  entry = smc_tree_branch_lookup_nearest_L (&smc_tree_root[ix0][ix1], key);
  if (!entry ||                                                                         /* need create */
      entry >= smc_tree_root[ix0][ix1].entries + smc_tree_root[ix0][ix1].n_entries ||   /* need append */
      entry->key != key)                                                                /* need insert */
    entry = smc_tree_branch_grow_L (&smc_tree_root[ix0][ix1], entry - smc_tree_root[ix0][ix1].entries);
  entry->key = key;
  entry->value = value;
  g_mutex_unlock (&smc_tree_mutex);
}

static gboolean
smc_tree_lookup (SmcKType  key,
                 SmcVType *value_p)
{
  SmcEntry *entry = NULL;
  unsigned int ix0 = SMC_TRUNK_HASH (key), ix1 = SMC_BRANCH_HASH (key);
  gboolean found_one = FALSE;
  *value_p = 0;
  g_mutex_lock (&smc_tree_mutex);
  if (smc_tree_root && smc_tree_root[ix0])
    {
      entry = smc_tree_branch_lookup_nearest_L (&smc_tree_root[ix0][ix1], key);
      if (entry &&
          entry < smc_tree_root[ix0][ix1].entries + smc_tree_root[ix0][ix1].n_entries &&
          entry->key == key)
        {
          found_one = TRUE;
          *value_p = entry->value;
        }
    }
  g_mutex_unlock (&smc_tree_mutex);
  return found_one;
}

static gboolean
smc_tree_remove (SmcKType key)
{
  unsigned int ix0 = SMC_TRUNK_HASH (key), ix1 = SMC_BRANCH_HASH (key);
  gboolean found_one = FALSE;
  g_mutex_lock (&smc_tree_mutex);
  if (smc_tree_root && smc_tree_root[ix0])
    {
      SmcEntry *entry = smc_tree_branch_lookup_nearest_L (&smc_tree_root[ix0][ix1], key);
      if (entry &&
          entry < smc_tree_root[ix0][ix1].entries + smc_tree_root[ix0][ix1].n_entries &&
          entry->key == key)
        {
          unsigned int i = entry - smc_tree_root[ix0][ix1].entries;
          smc_tree_root[ix0][ix1].n_entries -= 1;
          memmove (entry, entry + 1, (smc_tree_root[ix0][ix1].n_entries - i) * sizeof (entry[0]));
          if (!smc_tree_root[ix0][ix1].n_entries)
            {
              /* avoid useless pressure on the memory system */
              free (smc_tree_root[ix0][ix1].entries);
              smc_tree_root[ix0][ix1].entries = NULL;
            }
          found_one = TRUE;
        }
    }
  g_mutex_unlock (&smc_tree_mutex);
  return found_one;
}

#ifdef G_ENABLE_DEBUG
void
g_slice_debug_tree_statistics (void)
{
  g_mutex_lock (&smc_tree_mutex);
  if (smc_tree_root)
    {
      unsigned int i, j, t = 0, o = 0, b = 0, su = 0, ex = 0, en = 4294967295u;
      double tf, bf;
      for (i = 0; i < SMC_TRUNK_COUNT; i++)
        if (smc_tree_root[i])
          {
            t++;
            for (j = 0; j < SMC_BRANCH_COUNT; j++)
              if (smc_tree_root[i][j].n_entries)
                {
                  b++;
                  su += smc_tree_root[i][j].n_entries;
                  en = MIN (en, smc_tree_root[i][j].n_entries);
                  ex = MAX (ex, smc_tree_root[i][j].n_entries);
                }
              else if (smc_tree_root[i][j].entries)
                o++; /* formerly used, now empty */
          }
      en = b ? en : 0;
      tf = MAX (t, 1.0); /* max(1) to be a valid divisor */
      bf = MAX (b, 1.0); /* max(1) to be a valid divisor */
      g_fprintf (stderr, "GSlice: MemChecker: %u trunks, %u branches, %u old branches\n", t, b, o);
      g_fprintf (stderr, "GSlice: MemChecker: %f branches per trunk, %.2f%% utilization\n",
               b / tf,
               100.0 - (SMC_BRANCH_COUNT - b / tf) / (0.01 * SMC_BRANCH_COUNT));
      g_fprintf (stderr, "GSlice: MemChecker: %f entries per branch, %u minimum, %u maximum\n",
               su / bf, en, ex);
    }
  else
    g_fprintf (stderr, "GSlice: MemChecker: root=NULL\n");
  g_mutex_unlock (&smc_tree_mutex);

  /* sample statistics (beast + GSLice + 24h scripted core & GUI activity):
   *  PID %CPU %MEM   VSZ  RSS      COMMAND
   * 8887 30.3 45.8 456068 414856   beast-0.7.1 empty.bse
   * $ cat /proc/8887/statm # total-program-size resident-set-size shared-pages text/code data/stack library dirty-pages
   * 114017 103714 2354 344 0 108676 0
   * $ cat /proc/8887/status
   * Name:   beast-0.7.1
   * VmSize:   456068 kB
   * VmLck:         0 kB
   * VmRSS:    414856 kB
   * VmData:   434620 kB
   * VmStk:        84 kB
   * VmExe:      1376 kB
   * VmLib:     13036 kB
   * VmPTE:       456 kB
   * Threads:        3
   * (gdb) print g_slice_debug_tree_statistics ()
   * GSlice: MemChecker: 422 trunks, 213068 branches, 0 old branches
   * GSlice: MemChecker: 504.900474 branches per trunk, 98.81% utilization
   * GSlice: MemChecker: 4.965039 entries per branch, 1 minimum, 37 maximum
   */
}
#endif /* G_ENABLE_DEBUG */
