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

#ifndef __AV_ELEMENT_H__
#define __AV_ELEMENT_H__

#include <gst/gst.h>
#include <libavcodec/avcodec.h>

G_BEGIN_DECLS

// According to ffmpeg Git they introduced
// _decode_video2 and _decode_audio3  in version 52.25.0
#define LIBAVCODEC_NEW (LIBAVCODEC_VERSION_INT >= AV_VERSION_INT(52,25,1))

// Maximum size of the buffer for string representation of errors
#define ERROR_STRING_SIZE 256

#define TYPE_AVELEMENT            (avelement_get_type())
#define AVELEMENT(obj)            (G_TYPE_CHECK_INSTANCE_CAST((obj), TYPE_AVELEMENT, AVElement))
#define AVELEMENT_CLASS(klass)    (G_TYPE_CHECK_CLASS_CAST((klass), TYPE_AVELEMENT, AVElementClass))
#define AVELEMENT_GET_CLASS(obj)  (G_TYPE_INSTANCE_GET_CLASS ((obj), TYPE_AVELEMENT, AVElementClass))
#define IS_AVELEMENT(obj)         (G_TYPE_CHECK_INSTANCE_TYPE((obj), TYPE_AVELEMENT))
#define IS_AVELEMENT_CLASS(klass) (G_TYPE_CHECK_CLASS_TYPE((klass), TYPE_AVELEMENT))

typedef struct _AVElement       AVElement;
typedef struct _AVElementClass  AVElementClass;

struct _AVElement
{
    GstElement element;

#if LIBAVCODEC_NEW
    char error_string[ERROR_STRING_SIZE];
#endif // LIBAVCODEC_NEW
};

struct _AVElementClass
{
    GstElementClass parent_class;
};

GType     avelement_get_type (void);

const char* avelement_error_to_string(AVElement *avelement, int ret);

G_END_DECLS

#endif // __AV_ELEMENT_H__
