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
#include "fxmplugin.h"
#include "vp6decoder.h"
#include "flvdemux.h"

GST_DEBUG_CATEGORY (fxm_plugin_debug);
#define GST_CAT_DEFAULT fxm_plugin_debug

/* entry point to initialize the plug-in
 * initialize the plug-in itself
 * register the element factories and other features
 */

gboolean
fxm_plugin_init (GstPlugin * plugin)
{
    gboolean result = TRUE;
    //fprintf(stderr, "===fxm_plugin_init()\n");
    // debug category for fltering log messages
    GST_DEBUG_CATEGORY_INIT (fxm_plugin_debug, "fxmplugin",
            0, "JMC FXM Plugin");

    result &= gst_element_register (plugin, "vp6decoder", 250, TYPE_VP6_DECODER);
    result &= gst_element_register (plugin, "flvdemux", 70, TYPE_FLV_DEMUX);
    return result;
}


gboolean
fxm_plugin_register_static()
{
    //fprintf(stderr, "===fxm_plugin_register_static()\n");
    gboolean r = gst_plugin_register_static(
            GST_VERSION_MAJOR,
            GST_VERSION_MINOR,
            "fxmplugin",
            "FXM plugin",
            fxm_plugin_init,
            "1.0", //VERSION,
            "Proprietary",
            "JMC",
            "JMC",
            "http://javafx.com/"
            );
    //fprintf(stderr, "gst_plugin_register_static returned : %d\n", r);
    return r;
}

#ifdef FXM_STANDALONE

#define PACKAGE "JMC"

static gboolean
fxm_init (GstPlugin * plugin)
{
    gboolean result = TRUE;
    //fprintf(stderr, "===fxm_plugin_init()\n");
    // debug category for fltering log messages
    GST_DEBUG_CATEGORY_INIT (fxm_plugin_debug, "fxmplugin",
            0, "JMC FXM Plugin");

    result &= gst_element_register (plugin, "vp6decoder", 250, TYPE_VP6_DECODER);
    result &= gst_element_register (plugin, "flvdemux", 70, TYPE_FLV_DEMUX);
    return result;
}

GST_PLUGIN_DEFINE (
           GST_VERSION_MAJOR,
           GST_VERSION_MINOR,
           "fxmplugin",
           "FXM plugin",
           fxm_plugin_init,
           "1.0",
           "Proprietary",
       "JMC",
           "http://javafx.com/");

#endif
