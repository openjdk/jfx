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

#include <gst/gst.h>

#include <fxplugins_common.h>
#include <javasource.h>
#include <progressbuffer.h>
#include <hlsprogressbuffer.h>

#ifdef ENABLE_ON2_DECODER
#include <vp6decoder.h>
#include <flvdemux.h>
#endif

#ifdef OSX
#include <audioconverter.h>
#include <avcdecoder.h>
#endif

#if defined(WIN32)
gboolean dshowwrapper_init(GstPlugin* aacdecoder);
#endif

static gboolean fxplugins_init (GstPlugin * plugin)
{
    return java_source_plugin_init(plugin) &&
           hls_progress_buffer_plugin_init(plugin) &&

#ifdef ENABLE_ON2_DECODER
           gst_element_register (plugin, "vp6decoder", 250, TYPE_VP6_DECODER) &&
           gst_element_register (plugin, "flvdemux", 70, TYPE_FLV_DEMUX) &&
#endif

#if defined(WIN32)
           dshowwrapper_init(plugin) &&
#elif defined(OSX)
           audioconverter_plugin_init(plugin) &&
           avcdecoder_plugin_init(plugin) &&
#endif // WIN32
           progress_buffer_plugin_init(plugin);
}

#if defined(WIN32)
extern __declspec(dllexport) GstPluginDesc gst_plugin_desc =
#else // WIN32
GstPluginDesc gst_plugin_desc =
#endif // WIN32
{
    GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    "fxplugins",
    "FX Plugins",
    fxplugins_init,
    "1.0",
    "Proprietary",
    "JFXMedia",
    "JFXMedia",
    "http://javafx.com/",
    NULL
};
