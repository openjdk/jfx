/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

#ifndef __GST_PLUGINS_H__
#define __GST_PLUGINS_H__

#include <gst/gst.h>

G_BEGIN_DECLS

gboolean lite_plugins_init (GstPlugin * plugin);
gboolean plugin_init_elements (GstPlugin * plugin);
gboolean plugin_init_typefind (GstPlugin * plugin);
gboolean plugin_init_audioconvert (GstPlugin * plugin);
gboolean plugin_init_equalizer (GstPlugin * plugin);
gboolean plugin_init_spectrum (GstPlugin * plugin);
gboolean plugin_init_wavparse (GstPlugin * plugin);
gboolean plugin_init_aiff (GstPlugin * plugin);
gboolean plugin_init_app (GstPlugin * plugin);
gboolean plugin_init_audioparsers (GstPlugin * plugin);
gboolean plugin_init_qtdemux (GstPlugin * plugin);

#ifdef WIN32
gboolean plugin_init_directsound (GstPlugin * plugin);
gboolean plugin_init_indexers (GstPlugin * plugin);
#endif

#ifdef OSX
gboolean plugin_init_audiofx (GstPlugin * plugin);
gboolean plugin_init_osxaudio (GstPlugin * plugin);
#endif

#ifdef LINUX
gboolean plugin_init_audiofx (GstPlugin * plugin);
gboolean plugin_init_alsa (GstPlugin * plugin);
gboolean plugin_init_volume (GstPlugin * plugin);
#endif

G_END_DECLS

#endif /* __GST_FAKE_SRC_H__ */
