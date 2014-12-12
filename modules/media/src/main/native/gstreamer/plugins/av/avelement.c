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

#include "avelement.h"

/***********************************************************************************
 * Substitution for
 * GST_BOILERPLATE (AVElement, avelement, GstElement, GST_TYPE_ELEMENT);
 ***********************************************************************************/
static void avelement_class_init(AVElementClass *g_class);
static void avcodec_logger(void* ptr, int level, const char* fmt, va_list vl);

static GstElementClass *parent_class = NULL;

static void avelement_class_init_trampoline(gpointer g_class, gpointer data) {
    parent_class = (GstElementClass *) g_type_class_peek_parent(g_class);
    avelement_class_init(AVELEMENT_CLASS(g_class));
}

GType avelement_get_type(void) {
    static volatile gsize gonce_data = 0;
    // INLINE - g_once_init_enter()
    if (g_once_init_enter(&gonce_data)) {
        GType _type = gst_type_register_static_full(GST_TYPE_ELEMENT,
                g_intern_static_string("AVElement"),
                sizeof (AVElementClass),
                NULL, //avelement_base_init,
                NULL,
                avelement_class_init_trampoline,
                NULL,
                NULL,
                sizeof (AVElement),
                0,
                NULL, //(GInstanceInitFunc) avelement_init,
                NULL,
                (GTypeFlags) 0);
        g_once_init_leave(&gonce_data, (gsize) _type);
    }
    return (GType) gonce_data;
}

// Init avcodec library and set the logger callback.
static void avelement_class_init(AVElementClass * klass)
{
    av_log_set_callback(avcodec_logger);
    av_log_set_level(AV_LOG_WARNING);
}
// libavcodec log callback.
static void avcodec_logger(void* ptr, int level, const char* fmt, va_list vl)
{
    if (AV_LOG_QUIET == level)
        return;

    GLogLevelFlags log_level;
    if(level < AV_LOG_WARNING)
        log_level = G_LOG_LEVEL_CRITICAL;
    else if(level == AV_LOG_WARNING)
        log_level = G_LOG_LEVEL_WARNING;
    else
        log_level = G_LOG_LEVEL_DEBUG;

    g_logv("Java FX avdecoder", log_level, fmt, vl);
}

/***********************************************************************************
 * Error and Warning
 ***********************************************************************************/
const char* avelement_error_to_string(AVElement *element, int ret)
{
    if (av_strerror(ret, element->error_string, ERROR_STRING_SIZE) < 0)
        g_strlcpy(element->error_string, "Unknown", ERROR_STRING_SIZE);

    return element->error_string;
}
