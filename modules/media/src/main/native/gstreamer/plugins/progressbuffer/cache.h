/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#ifndef __CACHE_H__
#define __CACHE_H__

#include <gst/gst.h>

typedef struct _Cache Cache;

void      cache_static_init(void); // Must be called only once from the ProgressBuffer class initializer

Cache*    create_cache();
void      destroy_cache(Cache* instance);

// Writes a buffer.
void           cache_write_buffer(Cache* cache, GstBuffer* buffer);

/* Reads a buffer of the fixed size from the current read position.
 * Returns the read position after the operation has been made.
 * buffer parameter contains the target buffer with offset and size values set
 * This method is used in push mode.
 */
gint64         cache_read_buffer(Cache* cache, GstBuffer** buffer);

/* Reads a buffer of the specified size and start position.
 * Returns GST_FLOW_OK if the seek operation and subsequent read operation
 * were successfull. GST_FLOW_ERROR otherwise.
 */
GstFlowReturn  cache_read_buffer_from_position(Cache* cache, gint64 start_position, guint size, GstBuffer** buffer);

// Sets a new write position
gboolean       cache_set_write_position(Cache* cache, gint64 position);

// Sets a new read position
gboolean       cache_set_read_position(Cache* cache, gint64 position);

// Returns true if the cache has enough data for fluent reading, but we can't expect more than total.
gboolean       cache_has_enough_data(Cache* cache);

#endif // __CACHE_H__
