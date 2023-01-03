/*
 * Copyright (C) 2001 CodeFactory AB
 * Copyright (C) 2001 Thomas Nyberg <thomas@codefactory.se>
 * Copyright (C) 2001-2002 Andy Wingo <apwingo@eos.ncsu.edu>
 * Copyright (C) 2003 Benjamin Otte <in7y118@public.uni-hamburg.de>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA.
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "gstalsaelements.h"
#ifndef GSTREAMER_LITE
#include "gstalsadeviceprovider.h"
#endif // GSTREAMER_LITE

#include <gst/gst-i18n-plugin.h>

#ifdef GSTREAMER_LITE
gboolean
plugin_init_alsa (GstPlugin * plugin)
#else // GSTREAMER_LITE
static gboolean
plugin_init (GstPlugin * plugin)
#endif // GSTREAMER_LITE
{
  gboolean ret = FALSE;

#ifndef GSTREAMER_LITE
  ret |= GST_DEVICE_PROVIDER_REGISTER (alsadeviceprovider, plugin);

  ret |= GST_ELEMENT_REGISTER (alsasrc, plugin);
#endif // GSTREAMER_LITE
  ret |= GST_ELEMENT_REGISTER (alsasink, plugin);
#ifndef GSTREAMER_LITE
  ret |= GST_ELEMENT_REGISTER (alsamidisrc, plugin);
#endif // GSTREAMER_LITE

  return TRUE;
}

#ifndef GSTREAMER_LITE
GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    alsa,
    "ALSA plugin library",
    plugin_init, VERSION, "LGPL", GST_PACKAGE_NAME, GST_PACKAGE_ORIGIN)
#endif // GSTREAMER_LITE
