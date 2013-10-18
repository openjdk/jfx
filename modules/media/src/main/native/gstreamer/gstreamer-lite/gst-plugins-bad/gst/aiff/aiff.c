/* -*- Mode: C; tab-width: 2; indent-tabs-mode: t; c-basic-offset: 2 -*- */
/* GStreamer AIFF plugin initialisation
 * Copyright (C) <2008> Pioneers of the Inevitable <songbird@songbirdnest.com>
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

#include <gst/gst-i18n-plugin.h>

#include "aiffparse.h"
#ifndef GSTREAMER_LITE
#include "aiffmux.h"
#endif

GST_DEBUG_CATEGORY_STATIC (aiff_debug);
#define GST_CAT_DEFAULT (aiff_debug)

GST_DEBUG_CATEGORY_EXTERN (aiffparse_debug);
GST_DEBUG_CATEGORY_EXTERN (aiffmux_debug);

#ifdef GSTREAMER_LITE
gboolean
plugin_init_aiff (GstPlugin * plugin)
#else // GSTREAMER_LITE
static gboolean
plugin_init (GstPlugin * plugin)
#endif // GSTREAMER_LITE
{
  gboolean ret;

#ifdef GSTREAMER_LITE
  GST_DEBUG_CATEGORY_INIT (aiffparse_debug, "aiffparse", 0, "AIFF parser");
#else
  GST_DEBUG_CATEGORY_INIT (aiff_debug, "aiff", 0, "AIFF plugin");
  GST_DEBUG_CATEGORY_INIT (aiffparse_debug, "aiffparse", 0, "AIFF parser");
  GST_DEBUG_CATEGORY_INIT (aiffmux_debug, "aiffmux", 0, "AIFF muxer");
#endif

#ifdef ENABLE_NLS
  GST_DEBUG ("binding text domain %s to locale dir %s", GETTEXT_PACKAGE,
      LOCALEDIR);
  bindtextdomain (GETTEXT_PACKAGE, LOCALEDIR);
  bind_textdomain_codeset (GETTEXT_PACKAGE, "UTF-8");
#endif

  ret = gst_element_register (plugin, "aiffparse", GST_RANK_PRIMARY,
      GST_TYPE_AIFF_PARSE);
#ifndef GSTREAMER_LITE
  ret &= gst_element_register (plugin, "aiffmux", GST_RANK_PRIMARY,
      GST_TYPE_AIFF_MUX);
#endif

  return ret;
}

#ifndef GSTREAMER_LITE
GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    "aiff",
    "Create and parse Audio Interchange File Format (AIFF) files",
    plugin_init, VERSION, GST_LICENSE, GST_PACKAGE_NAME, GST_PACKAGE_ORIGIN)
#endif
