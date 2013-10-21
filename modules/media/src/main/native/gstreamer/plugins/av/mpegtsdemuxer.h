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

#ifndef __MPEGTS_DEMUXER_H__
#define __MPEGTS_DEMUXER_H__

#include "avelement.h"
#include <libavformat/avformat.h>
#include <gst/base/gstadapter.h>

G_BEGIN_DECLS

#define TYPE_MPEGTS_DEMUXER            (mpegts_demuxer_get_type())
#define MPEGTS_DEMUXER(obj)            (G_TYPE_CHECK_INSTANCE_CAST((obj), TYPE_MPEGTS_DEMUXER, MpegTSDemuxer))
#define MPEGTS_DEMUXER_CLASS(klass)    (G_TYPE_CHECK_CLASS_CAST((klass), TYPE_MPEGTS_DEMUXER, MpegTSDemuxerClass))
#define MPEGTS_DEMUXER_GET_CLASS(obj)  (G_TYPE_INSTANCE_GET_CLASS ((obj), TYPE_MPEGTS_DEMUXER, MpegTSDemuxerClass))
#define IS_MPEGTS_DEMUXER(obj)         (G_TYPE_CHECK_INSTANCE_TYPE((obj), TYPE_MPEGTS_DEMUXER))
#define IS_MPEGTS_DEMUXER_CLASS(klass) (G_TYPE_CHECK_CLASS_TYPE((klass), TYPE_MPEGTS_DEMUXER))

#define MPEGTS_DEMUXER_PLUGIN_NAME "avmpegtsdemuxer"

typedef struct _MpegTSDemuxer      MpegTSDemuxer;
typedef struct _MpegTSDemuxerClass MpegTSDemuxerClass;

GType mpegts_demuxer_get_type (void);

gboolean mpegts_demuxer_plugin_init (GstPlugin * mpegts_demuxer);

G_END_DECLS

#endif // __MPEGTS_DEMUXER_H__
