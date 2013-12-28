/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#ifdef HAVE_CONFIG_H
#  include "config.h"
#endif

#include <gst/gst.h>

#include "gstplugins-lite.h"

gboolean lite_plugins_init (GstPlugin * plugin)
{
  if (!plugin_init_elements(plugin) ||
      !plugin_init_typefind(plugin) ||
      !plugin_init_audioconvert(plugin) ||
      !plugin_init_equalizer(plugin) ||
      !plugin_init_spectrum(plugin) ||
      !plugin_init_wavparse(plugin) ||
      !plugin_init_aiff(plugin) ||
      !plugin_init_app(plugin) ||
      !plugin_init_audioparsers(plugin) ||
      !plugin_init_qtdemux(plugin))
    return FALSE;

#ifdef WIN32
  if (!plugin_init_directsound(plugin) ||
      !plugin_init_indexers(plugin))
    return FALSE;  
#endif

#ifdef OSX
  if (!plugin_init_audiofx(plugin) ||
      !plugin_init_osxaudio(plugin))
    return FALSE;
#endif

#ifdef LINUX
  if (!plugin_init_audiofx(plugin) ||
      !plugin_init_alsa(plugin) ||
      !plugin_init_volume(plugin))
    return FALSE;
#endif
  
  return TRUE;
}
