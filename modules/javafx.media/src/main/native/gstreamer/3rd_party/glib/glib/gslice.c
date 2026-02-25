/* GLIB sliced memory - fast concurrent memory chunk allocator
 * Copyright (C) 2005 Tim Janik
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
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

#include <stdio.h>
#include <string.h>

#include "gslice.h"

#include "gmem.h"               /* gslice.h */
#include "glib_trace.h"
#include "gprintf.h"


/* --- auxiliary functions --- */
void
g_slice_set_config (GSliceConfig ckey,
                    gint64       value)
{
  /* deprecated, no implementation */
}

gint64
g_slice_get_config (GSliceConfig ckey)
{
  /* deprecated, no implementation */
  return 0;
}

gint64*
g_slice_get_config_state (GSliceConfig ckey,
                          gint64       address,
                          guint       *n_values)
{
  /* deprecated, no implementation */
  return NULL;
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
 * cast in the source code.
 *
 * This can never return %NULL as the minimum allocation size from
 * `sizeof (@type)` is 1 byte.
 *
 * Since GLib 2.76 this always uses the system malloc() implementation
 * internally.
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
 *
 * This can never return %NULL as the minimum allocation size from
 * `sizeof (@type)` is 1 byte.
 *
 * Since GLib 2.76 this always uses the system malloc() implementation
 * internally.
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
 *
 * This can never return %NULL.
 *
 * Since GLib 2.76 this always uses the system malloc() implementation
 * internally.
 *
 * Returns: (not nullable): a pointer to the allocated block, cast to a pointer
 *    to @type
 *
 * Since: 2.14
 */

/**
 * g_slice_free:
 * @type: the type of the block to free, typically a structure name
 * @mem: (nullable): a pointer to the block to free
 *
 * A convenience macro to free a block of memory that has
 * been allocated from the slice allocator.
 *
 * It calls g_slice_free1() using `sizeof (type)`
 * as the block size.
 * Note that the exact release behaviour can be changed with the
 * [`G_DEBUG=gc-friendly`](running.html#environment-variables) environment variable.
 *
 * If @mem is %NULL, this macro does nothing.
 *
 * Since GLib 2.76 this always uses the system free() implementation internally.
 *
 * Since: 2.10
 */

/**
 * g_slice_free_chain:
 * @type: the type of the @mem_chain blocks
 * @mem_chain: (nullable): a pointer to the first block of the chain
 * @next: the field name of the next pointer in @type
 *
 * Frees a linked list of memory blocks of structure type @type.
 *
 * The memory blocks must be equal-sized, allocated via
 * g_slice_alloc() or g_slice_alloc0() and linked together by
 * a @next pointer (similar to #GSList). The name of the
 * @next field in @type is passed as third argument.
 * Note that the exact release behaviour can be changed with the
 * [`G_DEBUG=gc-friendly`](running.html#environment-variables) environment variable.
 *
 * If @mem_chain is %NULL, this function does nothing.
 *
 * Since GLib 2.76 this always uses the system free() implementation internally.
 *
 * Since: 2.10
 */

/**
 * g_slice_alloc:
 * @block_size: the number of bytes to allocate
 *
 * Allocates a block of memory from the libc allocator.
 *
 * The block address handed out can be expected to be aligned
 * to at least `1 * sizeof (void*)`.
 *
 * Since GLib 2.76 this always uses the system malloc() implementation
 * internally.
 *
 * Returns: (nullable): a pointer to the allocated memory block, which will
 *   be %NULL if and only if @mem_size is 0
 *
 * Since: 2.10
 */
gpointer
g_slice_alloc (gsize mem_size)
{
  gpointer mem;

  mem = g_malloc (mem_size);
  TRACE (GLIB_SLICE_ALLOC((void*)mem, mem_size));

  return mem;
}

/**
 * g_slice_alloc0:
 * @block_size: the number of bytes to allocate
 *
 * Allocates a block of memory via g_slice_alloc() and initializes
 * the returned memory to 0.
 *
 * Since GLib 2.76 this always uses the system malloc() implementation
 * internally.
 *
 * Returns: (nullable): a pointer to the allocated block, which will be %NULL
 *    if and only if @mem_size is 0
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
 * Since GLib 2.76 this always uses the system malloc() implementation
 * internally.
 *
 * Returns: (nullable): a pointer to the allocated memory block,
 *    which will be %NULL if and only if @mem_size is 0
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
 * @mem_block: (nullable): a pointer to the block to free
 *
 * Frees a block of memory.
 *
 * The memory must have been allocated via g_slice_alloc() or
 * g_slice_alloc0() and the @block_size has to match the size
 * specified upon allocation. Note that the exact release behaviour
 * can be changed with the [`G_DEBUG=gc-friendly`](running.html#environment-variables) environment
 * variable.
 *
 * If @mem_block is %NULL, this function does nothing.
 *
 * Since GLib 2.76 this always uses the system free_sized() implementation
 * internally.
 *
 * Since: 2.10
 */
void
g_slice_free1 (gsize    mem_size,
               gpointer mem_block)
{
  if (G_UNLIKELY (g_mem_gc_friendly && mem_block))
    memset (mem_block, 0, mem_size);
  g_free_sized (mem_block, mem_size);
  TRACE (GLIB_SLICE_FREE((void*)mem_block, mem_size));
}

/**
 * g_slice_free_chain_with_offset:
 * @block_size: the size of the blocks
 * @mem_chain: (nullable):  a pointer to the first block of the chain
 * @next_offset: the offset of the @next field in the blocks
 *
 * Frees a linked list of memory blocks of structure type @type.
 *
 * The memory blocks must be equal-sized, allocated via
 * g_slice_alloc() or g_slice_alloc0() and linked together by a
 * @next pointer (similar to #GSList). The offset of the @next
 * field in each block is passed as third argument.
 * Note that the exact release behaviour can be changed with the
 * [`G_DEBUG=gc-friendly`](running.html#environment-variables) environment variable.
 *
 * If @mem_chain is %NULL, this function does nothing.
 *
 * Since GLib 2.76 this always uses the system free_sized() implementation
 * internally.
 *
 * Since: 2.10
 */
void
g_slice_free_chain_with_offset (gsize    mem_size,
                                gpointer mem_chain,
                                gsize    next_offset)
{
  gpointer slice = mem_chain;
  while (slice)
    {
      guint8 *current = slice;
      slice = *(gpointer *) (current + next_offset);
      if (G_UNLIKELY (g_mem_gc_friendly))
        memset (current, 0, mem_size);
      g_free_sized (current, mem_size);
    }
}

#ifdef G_ENABLE_DEBUG
void
g_slice_debug_tree_statistics (void)
{
  g_fprintf (stderr, "GSlice: Implementation dropped in GLib 2.76\n");
}
#endif /* G_ENABLE_DEBUG */
