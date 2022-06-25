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
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>

#include "audiopanorama.h"
#ifndef GSTREAMER_LITE
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
#include "gstscaletempo.h"
#include "gststereo.h"
#endif // GSTREAMER_LITE

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
  gboolean ret = FALSE;

  ret |= GST_ELEMENT_REGISTER (audiopanorama, plugin);
#ifndef GSTREAMER_LITE
  ret |= GST_ELEMENT_REGISTER (audioinvert, plugin);
  ret |= GST_ELEMENT_REGISTER (audiokaraoke, plugin);
  ret |= GST_ELEMENT_REGISTER (audioamplify, plugin);
  ret |= GST_ELEMENT_REGISTER (audiodynamic, plugin);
  ret |= GST_ELEMENT_REGISTER (audiocheblimit, plugin);
  ret |= GST_ELEMENT_REGISTER (audiochebband, plugin);
  ret |= GST_ELEMENT_REGISTER (audioiirfilter, plugin);
  ret |= GST_ELEMENT_REGISTER (audiowsinclimit, plugin);
  ret |= GST_ELEMENT_REGISTER (audiowsincband, plugin);
  ret |= GST_ELEMENT_REGISTER (audiofirfilter, plugin);
  ret |= GST_ELEMENT_REGISTER (audioecho, plugin);
  ret |= GST_ELEMENT_REGISTER (scaletempo, plugin);
  ret |= GST_ELEMENT_REGISTER (stereo, plugin);
#endif // GSTREAMER_LITE

  return ret;
}

#ifndef GSTREAMER_LITE
GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    audiofx,
    "Audio effects plugin",
    plugin_init, VERSION, GST_LICENSE, GST_PACKAGE_NAME, GST_PACKAGE_ORIGIN)
#endif // GSTREAMER_LITE
