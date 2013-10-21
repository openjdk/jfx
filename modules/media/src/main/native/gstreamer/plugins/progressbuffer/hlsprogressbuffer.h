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

#ifndef __HLS_PROGRESS_BUFFER_H__
#define __HLS_PROGRESS_BUFFER_H__

#include <gst/gst.h>

G_BEGIN_DECLS

#define HLS_PROGRESS_BUFFER_PLUGIN_NAME "hlsprogressbuffer"
#define HLS_PB_MESSAGE_HLS_EOS          "hls_pb_eos"
#define HLS_PB_MESSAGE_RESUME           "hls_pb_resume"
#define HLS_PB_MESSAGE_FULL             "hls_pb_full"
#define HLS_PB_MESSAGE_NOT_FULL         "hls_pb_not_full"

#define HLS_PROGRESS_BUFFER_TYPE            (hls_progress_buffer_get_type())
#define HLS_PROGRESS_BUFFER(obj)            (G_TYPE_CHECK_INSTANCE_CAST((obj), HLS_PROGRESS_BUFFER_TYPE, HLSProgressBuffer))
#define HLS_PROGRESS_BUFFER_CLASS(klass)    (G_TYPE_CHECK_CLASS_CAST((klass), HLS_PROGRESS_BUFFER_TYPE, HLSProgressBufferClass))
#define IS_HLS_PROGRESS_BUFFER(obj)         (G_TYPE_CHECK_INSTANCE_TYPE((obj), HLS_PROGRESS_BUFFER_TYPE))
#define IS_HLS_PROGRESS_BUFFER_CLASS(klass) (G_TYPE_CHECK_CLASS_TYPE((klass), HLS_PROGRESS_BUFFER_TYPE))
#define HLS_PROGRESS_BUFFER_GET_CLASS(obj)  (G_TYPE_INSTANCE_GET_CLASS ((obj), HLS_PROGRESS_BUFFER_TYPE, HLSProgressBufferClass))

typedef struct _HLSProgressBuffer      HLSProgressBuffer;
typedef struct _HLSProgressBufferClass HLSProgressBufferClass;

GType hls_progress_buffer_get_type (void);
gboolean hls_progress_buffer_plugin_init (GstPlugin *plugin);

G_END_DECLS

#endif // __HLS_PROGRESS_BUFFER_H__
