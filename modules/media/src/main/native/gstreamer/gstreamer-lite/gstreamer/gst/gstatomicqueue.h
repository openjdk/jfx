/* GStreamer
 * Copyright (C) 2009-2010 Edward Hervey <bilboed@bilboed.com>
 *           (C) 2011 Wim Taymans <wim.taymans@gmail.com>
 *
 * gstatomicqueue.h:
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

#include <glib.h>

#ifndef __GST_ATOMIC_QUEUE_H__
#define __GST_ATOMIC_QUEUE_H__

G_BEGIN_DECLS

/**
 * GstAtomicQueue:
 *
 * Opaque atomic data queue.
 *
 * Use the acessor functions to get the stored values.
 *
 * Since: 0.10.33
 */
typedef struct _GstAtomicQueue GstAtomicQueue;


GstAtomicQueue *   gst_atomic_queue_new         (guint initial_size);

void               gst_atomic_queue_ref         (GstAtomicQueue * queue);
void               gst_atomic_queue_unref       (GstAtomicQueue * queue);

void               gst_atomic_queue_push        (GstAtomicQueue* queue, gpointer data);
gpointer           gst_atomic_queue_pop         (GstAtomicQueue* queue);
gpointer           gst_atomic_queue_peek        (GstAtomicQueue* queue);

guint              gst_atomic_queue_length      (GstAtomicQueue * queue);

G_END_DECLS

#endif /* __GST_ATOMIC_QUEUE_H__ */
