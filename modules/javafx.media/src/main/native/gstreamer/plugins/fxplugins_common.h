/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef __FX_PLUGINS_COMMON_H__
#define __FX_PLUGINS_COMMON_H__

#include <gst/gst.h>

G_BEGIN_DECLS

// Custom events
enum
{
    FX_EVENT_RANGE_READY = GST_EVENT_MAKE_TYPE (64, GST_EVENT_TYPE_DOWNSTREAM | GST_EVENT_TYPE_SERIALIZED)
};

// Query to find out if a sinkpad supports progressive getrange
#define GETRANGE_QUERY_NAME               "progressive-getrange"
#define GETRANGE_QUERY_SUPPORTS_FIELDNANE "supports"
#define GETRANGE_QUERY_SUPPORTS_FIELDTYPE G_TYPE_BOOLEAN

// Do not use CODEC_ID_*, since it will conflict with libavcodec
enum JFX_CODEC_ID
{
    JFX_CODEC_ID_UNKNOWN = 0,
    JFX_CODEC_ID_AAC,
    JFX_CODEC_ID_H264, // HLS
    JFX_CODEC_ID_AVC1, // MP4
    JFX_CODEC_ID_H265, // MP4
};

// Custom error codes used by our plugins

// GStreamer enums with error codes does not contains more then 25 error codes,
// so we will start with 0xFF, so we do not conflict with GStreamer
enum JFX_GST_ERROR_CODES
{
    JFX_GST_ERROR = 0xFF,
    JFX_GST_MISSING_LIBSWSCALE,
    JFX_GST_INVALID_LIBSWSCALE,
};

G_END_DECLS

#endif /* __FX_PLUGINS_COMMON_H__ */
