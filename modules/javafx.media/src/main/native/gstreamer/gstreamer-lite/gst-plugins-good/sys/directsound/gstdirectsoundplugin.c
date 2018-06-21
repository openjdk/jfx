/* GStreamer
* Copyright (C) 2005 Sebastien Moutte <sebastien@moutte.net>
* Copyright (C) 2007 Pioneers of the Inevitable <songbird@songbirdnest.com>
*
* gstdirectsoundplugin.c:
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
*
*
* The development of this code was made possible due to the involvement
* of Pioneers of the Inevitable, the creators of the Songbird Music player
*
*/

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "gstdirectsoundsink.h"
#ifndef GSTREAMER_LITE
#include "gstdirectsounddevice.h"
#endif // GSTREAMER_LITE

#ifdef GSTREAMER_LITE
gboolean
plugin_init_directsound (GstPlugin * plugin)
#else // GSTREAMER_LITE
static gboolean
plugin_init (GstPlugin * plugin)
#endif // GSTREAMER_LITE
{
#ifdef GSTREAMER_LITE
  if (!gst_element_register (plugin, "directsoundsink", GST_RANK_PRIMARY,
          GST_TYPE_DIRECTSOUND_SINK))
    return FALSE;
#else // GSTREAMER_LITE
if (!gst_element_register (plugin, "directsoundsink", GST_RANK_SECONDARY,
          GST_TYPE_DIRECTSOUND_SINK))
    return FALSE;

  if (!gst_device_provider_register (plugin, "directsoundsinkdeviceprovider",
          GST_RANK_PRIMARY, GST_TYPE_DIRECTSOUND_DEVICE_PROVIDER))
    return FALSE;
#endif // GSTREAMER_LITE

  return TRUE;
}

#ifndef GSTREAMER_LITE
GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    directsound,
    "Direct Sound plugin library",
    plugin_init, VERSION, "LGPL", GST_PACKAGE_NAME, GST_PACKAGE_ORIGIN)
#endif // GSTREAMER_LITE