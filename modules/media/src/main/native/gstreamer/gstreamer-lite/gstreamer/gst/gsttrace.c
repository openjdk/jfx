/* GStreamer
 * Copyright (C) 1999,2000 Erik Walthinsen <omega@cse.ogi.edu>
 *                    2000 Wim Taymans <wtay@chello.be>
 *
 * gsttrace.c: Tracing functions (deprecated)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

/**
 * SECTION:gsttrace
 * @short_description: Tracing functionality
 *
 * Traces allows to track object allocation. They provide a instance counter per
 * #GType. The counter is incremented for each object allocated and decremented
 * it when it's freed.
 *
 * <example>
 * <title>Tracing object instances</title>
 *   <programlisting>
 *     // trace un-freed object instances
 *     gst_alloc_trace_set_flags_all (GST_ALLOC_TRACE_LIVE);
 *     if (!gst_alloc_trace_available ()) {
 *       g_warning ("Trace not available (recompile with trace enabled).");
 *     }
 *     gst_alloc_trace_print_live ();
 *     // do something here
 *     gst_alloc_trace_print_live ();
 *   </programlisting>
 * </example>
 *
 * Last reviewed on 2005-11-21 (0.9.5)
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif
#include <stdio.h>
#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif
#include <sys/stat.h>
#include <fcntl.h>
#include <string.h>
#include <errno.h>

#if defined (_MSC_VER) && _MSC_VER >= 1400
# include <io.h>
#endif

#include "gst_private.h"
#include "gstinfo.h"

#include "gsttrace.h"

GStaticMutex _gst_trace_mutex = G_STATIC_MUTEX_INIT;

static
#ifdef __inline__
  __inline__
#endif
    void
read_tsc (gint64 * dst)
{
#if defined(HAVE_RDTSC) && defined(__GNUC__)
  guint64 tsc;
  __asm__ __volatile__ ("rdtsc":"=A" (tsc));

  *dst = tsc;
#else
  *dst = 0;
#endif
}

/**
 * gst_trace_read_tsc:
 * @dst: (out) pointer to hold the result.
 *
 * Read a platform independent timer value that can be used in
 * benchmarks.
 */
void
gst_trace_read_tsc (gint64 * dst)
{
  read_tsc (dst);
}

static GstTrace *_gst_trace_default = NULL;
gint _gst_trace_on = 1;

/**
 * gst_trace_new:
 * @filename: a filename
 * @size: the max size of the file
 *
 * Create a ringbuffer of @size in the file with @filename to
 * store trace results in.
 *
 * Free-function: gst_trace_destroy
 *
 * Returns: (transfer full): a new #GstTrace.
 */
GstTrace *
gst_trace_new (const gchar * filename, gint size)
{
  GstTrace *trace = g_slice_new (GstTrace);

  g_return_val_if_fail (trace != NULL, NULL);
  trace->filename = g_strdup (filename);
  GST_DEBUG ("opening '%s'", trace->filename);
#ifndef S_IWUSR
#define S_IWUSR S_IWRITE
#endif
#ifndef S_IRUSR
#define S_IRUSR S_IREAD
#endif
  trace->fd =
      open (trace->filename, O_RDWR | O_CREAT | O_TRUNC, S_IRUSR | S_IWUSR);
  perror ("opening trace file");
  g_return_val_if_fail (trace->fd > 0, NULL);
  trace->buf = g_malloc (size * sizeof (GstTraceEntry));
  g_return_val_if_fail (trace->buf != NULL, NULL);
  trace->bufsize = size;
  trace->bufoffset = 0;

  return trace;
}

/**
 * gst_trace_destroy:
 * @trace: (in) (transfer full): the #GstTrace to destroy
 *
 * Flush an close the previously allocated @trace.
 */
void
gst_trace_destroy (GstTrace * trace)
{
  g_return_if_fail (trace != NULL);
  g_return_if_fail (trace->buf != NULL);

  if (gst_trace_get_remaining (trace) > 0)
    gst_trace_flush (trace);
  close (trace->fd);
  g_free (trace->buf);
  g_slice_free (GstTrace, trace);
}

/**
 * gst_trace_flush:
 * @trace: the #GstTrace to flush.
 *
 * Flush any pending trace entries in @trace to the trace file.
 * @trace can be NULL in which case the default #GstTrace will be
 * flushed.
 */
void
gst_trace_flush (GstTrace * trace)
{
  int res, buf_len;

  if (!trace) {
    trace = _gst_trace_default;
    if (!trace)
      return;
  }

  buf_len = trace->bufoffset * sizeof (GstTraceEntry);
  res = write (trace->fd, trace->buf, buf_len);
  if (res < 0) {
    g_warning ("Failed to write trace: %s", g_strerror (errno));
    return;
  } else if (res < buf_len) {
    g_warning ("Failed to write trace: only wrote %d/%d bytes", res, buf_len);
    return;
  }
  trace->bufoffset = 0;
}

/**
 * gst_trace_text_flush:
 * @trace: the #GstTrace to flush.
 *
 * Flush any pending trace entries in @trace to the trace file,
 * formatted as a text line with timestamp and sequence numbers.
 * @trace can be NULL in which case the default #GstTrace will be
 * flushed.
 */
void
gst_trace_text_flush (GstTrace * trace)
{
  int i;

#define STRSIZE (20 + 1 + 10 + 1 + 10 + 1 + 112 + 1 + 1)
  char str[STRSIZE];

  if (!trace) {
    trace = _gst_trace_default;
    if (!trace)
      return;
  }

  for (i = 0; i < trace->bufoffset; i++) {
    g_snprintf (str, STRSIZE, "%20" G_GINT64_FORMAT " %10d %10d %s\n",
        trace->buf[i].timestamp,
        trace->buf[i].sequence, trace->buf[i].data, trace->buf[i].message);
    if (write (trace->fd, str, strlen (str)) < 0) {
      g_warning ("Failed to write trace %d: %s", i, g_strerror (errno));
      return;
    }
  }
  trace->bufoffset = 0;
#undef STRSIZE
}

/**
 * gst_trace_set_default:
 * @trace: the #GstTrace to set as the default.
 *
 * Set the default #GstTrace to @trace.
 */
void
gst_trace_set_default (GstTrace * trace)
{
  g_return_if_fail (trace != NULL);
  _gst_trace_default = trace;
}

void
_gst_trace_add_entry (GstTrace * trace, guint32 seq, guint32 data, gchar * msg)
{
  GstTraceEntry *entry;

  if (!trace) {
    trace = _gst_trace_default;
    if (!trace)
      return;
  }

  entry = trace->buf + trace->bufoffset;
  read_tsc (&(entry->timestamp));
  entry->sequence = seq;
  entry->data = data;
  strncpy (entry->message, msg, 112);
  entry->message[111] = '\0';
  trace->bufoffset++;

  gst_trace_flush (trace);
}


/* global flags */
static GstAllocTraceFlags _gst_trace_flags = 0;

/* list of registered tracers */
static GList *_gst_alloc_tracers = NULL;

/**
 * gst_alloc_trace_available:
 *
 * Check if alloc tracing was compiled into the core
 *
 * Returns: TRUE if the core was compiled with alloc
 * tracing enabled.
 */
gboolean
gst_alloc_trace_available (void)
{
#ifdef GST_DISABLE_ALLOC_TRACE
  return FALSE;
#else
  return TRUE;
#endif
}

/**
 * _gst_alloc_trace_register:
 * @name: the name of the new alloc trace object.
 *
 * Register an get a handle to a GstAllocTrace object that
 * can be used to trace memory allocations.
 *
 * Returns: A handle to a GstAllocTrace.
 */
GstAllocTrace *
_gst_alloc_trace_register (const gchar * name)
{
  GstAllocTrace *trace;

  g_return_val_if_fail (name, NULL);

  trace = g_slice_new (GstAllocTrace);
  trace->name = g_strdup (name);
  trace->live = 0;
  trace->mem_live = NULL;
  trace->flags = _gst_trace_flags;

  _gst_alloc_tracers = g_list_prepend (_gst_alloc_tracers, trace);

  return trace;
}

/**
 * gst_alloc_trace_list:
 *
 * Get a list of all registered alloc trace objects.
 *
 * Returns: a GList of GstAllocTrace objects.
 */
const GList *
gst_alloc_trace_list (void)
{
  return _gst_alloc_tracers;
}

/**
 * gst_alloc_trace_live_all:
 *
 * Get the total number of live registered alloc trace objects.
 *
 * Returns: the total number of live registered alloc trace objects.
 */
int
gst_alloc_trace_live_all (void)
{
  GList *walk = _gst_alloc_tracers;
  int num = 0;

  while (walk) {
    GstAllocTrace *trace = (GstAllocTrace *) walk->data;

    num += trace->live;

    walk = g_list_next (walk);
  }

  return num;
}

static gint
compare_func (GstAllocTrace * a, GstAllocTrace * b)
{
  return strcmp (a->name, b->name);
}

static GList *
gst_alloc_trace_list_sorted (void)
{
  GList *ret;

  ret = g_list_sort (g_list_copy (_gst_alloc_tracers),
      (GCompareFunc) compare_func);

  return ret;
}

/**
 * gst_alloc_trace_print_all:
 *
 * Print the status of all registered alloc trace objects.
 */
void
gst_alloc_trace_print_all (void)
{
  GList *orig, *walk;

  orig = walk = gst_alloc_trace_list_sorted ();

  while (walk) {
    GstAllocTrace *trace = (GstAllocTrace *) walk->data;

    gst_alloc_trace_print (trace);

    walk = g_list_next (walk);
  }

  g_list_free (orig);
}

/**
 * gst_alloc_trace_print_live:
 *
 * Print the status of all registered alloc trace objects, ignoring those
 * without live objects.
 */
void
gst_alloc_trace_print_live (void)
{
  GList *orig, *walk;

  orig = walk = gst_alloc_trace_list_sorted ();

  while (walk) {
    GstAllocTrace *trace = (GstAllocTrace *) walk->data;

    if (trace->live)
      gst_alloc_trace_print (trace);

    walk = g_list_next (walk);
  }

  g_list_free (orig);
}

/**
 * gst_alloc_trace_set_flags_all:
 * @flags: the options to enable
 *
 * Enable the specified options on all registered alloc trace
 * objects.
 */
void
gst_alloc_trace_set_flags_all (GstAllocTraceFlags flags)
{
  GList *walk = _gst_alloc_tracers;

  while (walk) {
    GstAllocTrace *trace = (GstAllocTrace *) walk->data;

    GST_DEBUG ("setting flags %d on %p", (gint) flags, trace);
    gst_alloc_trace_set_flags (trace, flags);

    walk = g_list_next (walk);
  }
  _gst_trace_flags = flags;
}

/**
 * gst_alloc_trace_get:
 * @name: the name of the alloc trace object
 *
 * Get the named alloc trace object.
 *
 * Returns: a GstAllocTrace with the given name or NULL when
 * no alloc tracer was registered with that name.
 */
GstAllocTrace *
gst_alloc_trace_get (const gchar * name)
{
  GList *walk = _gst_alloc_tracers;

  g_return_val_if_fail (name, NULL);

  while (walk) {
    GstAllocTrace *trace = (GstAllocTrace *) walk->data;

    if (!strcmp (trace->name, name))
      return trace;

    walk = g_list_next (walk);
  }
  return NULL;
}

/**
 * gst_alloc_trace_print:
 * @trace: the GstAllocTrace to print
 *
 * Print the status of the given GstAllocTrace.
 */
void
gst_alloc_trace_print (const GstAllocTrace * trace)
{
  GSList *mem_live;

  g_return_if_fail (trace != NULL);

  if (trace->flags & GST_ALLOC_TRACE_LIVE) {
    g_print ("%-22.22s : %d\n", trace->name, trace->live);
  } else {
    g_print ("%-22.22s : (no live count)\n", trace->name);
  }

  if (trace->flags & GST_ALLOC_TRACE_MEM_LIVE) {
    mem_live = trace->mem_live;

    while (mem_live) {
      gpointer data = mem_live->data;

      if (G_IS_OBJECT (data)) {
        g_print ("%-22.22s : %p\n", g_type_name (G_OBJECT_TYPE (data)), data);
      } else {
        g_print ("%-22.22s : %p\n", "", data);
      }
      mem_live = mem_live->next;
    }
  }
}

/**
 * gst_alloc_trace_set_flags:
 * @trace: the GstAllocTrace
 * @flags: flags to set
 *
 * Enable the given features on the given GstAllocTrace object.
 */
void
gst_alloc_trace_set_flags (GstAllocTrace * trace, GstAllocTraceFlags flags)
{
  g_return_if_fail (trace != NULL);

  trace->flags = flags;
}
