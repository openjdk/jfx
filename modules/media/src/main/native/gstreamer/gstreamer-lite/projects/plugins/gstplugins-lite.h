/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
