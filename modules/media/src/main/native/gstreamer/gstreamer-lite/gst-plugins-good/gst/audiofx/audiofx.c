/*
 * GStreamer
 * Copyright (C) 2006 Stefan Kost <ensonic@users.sf.net>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>
#include <gst/controller/gstcontroller.h>

#include "audiopanorama.h"
#include "audioinvert.h"
#include "audiokaraoke.h"
#include "audioamplify.h"
#include "audiodynamic.h"
#include "audiocheblimit.h"
#include "audiochebband.h"
#include "audioiirfilter.h"
#include "audiowsincband.h"
#include "audiowsinclimit.h"
#include "audiofirfilter.h"
#include "audioecho.h"

/* entry point to initialize the plug-in
 * initialize the plug-in itself
 * register the element factories and pad templates
 * register the features
 */
#ifdef GSTREAMER_LITE
gboolean
plugin_init_audiofx (GstPlugin * plugin)
#else // GSTREAMER_LITE
static gboolean
plugin_init (GstPlugin * plugin)
#endif // GSTREAMER_LITE
{
  /* initialize gst controller library */
  gst_controller_init (NULL, NULL);

#ifdef GSTREAMER_LITE
  return (gst_element_register (plugin, "audiopanorama", GST_RANK_NONE,
          GST_TYPE_AUDIO_PANORAMA));
#else // GSTREAMER_LITE
  return (gst_element_register (plugin, "audiopanorama", GST_RANK_NONE,
          GST_TYPE_AUDIO_PANORAMA) &&
      gst_element_register (plugin, "audioinvert", GST_RANK_NONE,
          GST_TYPE_AUDIO_INVERT) &&
      gst_element_register (plugin, "audiokaraoke", GST_RANK_NONE,
          GST_TYPE_AUDIO_KARAOKE) &&
      gst_element_register (plugin, "audioamplify", GST_RANK_NONE,
          GST_TYPE_AUDIO_AMPLIFY) &&
      gst_element_register (plugin, "audiodynamic", GST_RANK_NONE,
          GST_TYPE_AUDIO_DYNAMIC) &&
      gst_element_register (plugin, "audiocheblimit", GST_RANK_NONE,
          GST_TYPE_AUDIO_CHEB_LIMIT) &&
      gst_element_register (plugin, "audiochebband", GST_RANK_NONE,
          GST_TYPE_AUDIO_CHEB_BAND) &&
      gst_element_register (plugin, "audioiirfilter", GST_RANK_NONE,
          GST_TYPE_AUDIO_IIR_FILTER) &&
      gst_element_register (plugin, "audiowsinclimit", GST_RANK_NONE,
          GST_TYPE_AUDIO_WSINC_LIMIT) &&
      gst_element_register (plugin, "audiowsincband", GST_RANK_NONE,
          GST_TYPE_AUDIO_WSINC_BAND) &&
      gst_element_register (plugin, "audiofirfilter", GST_RANK_NONE,
          GST_TYPE_AUDIO_FIR_FILTER) &&
      gst_element_register (plugin, "audioecho", GST_RANK_NONE,
          GST_TYPE_AUDIO_ECHO));
#endif // GSTREAMER_LITE
}

#ifndef GSTREAMER_LITE
GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    "audiofx",
    "Audio effects plugin",
    plugin_init, VERSION, GST_LICENSE, GST_PACKAGE_NAME, GST_PACKAGE_ORIGIN)
#endif // GSTREAMER_LITE