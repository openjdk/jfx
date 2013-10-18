/* GStreamer
 *
 * Copyright (C) 2001-2002 Ronald Bultje <rbultje@ronald.bitfreak.net>
 *               2006 Edgard Lima <edgard.lima@indt.org.br>
 *
 * gstv4l2.c: plugin for v4l2 elements
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

#include "gst/gst-i18n-plugin.h"

#include <gst/gst.h>
#include <gst/controller/gstcontroller.h>

#include "gstv4l2object.h"
#include "gstv4l2src.h"
#ifdef HAVE_EXPERIMENTAL
#include "gstv4l2sink.h"
#endif
#include "gstv4l2radio.h"
/* #include "gstv4l2jpegsrc.h" */
/* #include "gstv4l2mjpegsrc.h" */
/* #include "gstv4l2mjpegsink.h" */

/* used in v4l2_calls.c and v4l2src_calls.c */
GST_DEBUG_CATEGORY (v4l2_debug);
GST_DEBUG_CATEGORY (GST_CAT_PERFORMANCE);

static gboolean
plugin_init (GstPlugin * plugin)
{
  GST_DEBUG_CATEGORY_INIT (v4l2_debug, "v4l2", 0, "V4L2 API calls");
  GST_DEBUG_CATEGORY_GET (GST_CAT_PERFORMANCE, "GST_PERFORMANCE");

  /* initialize gst controller library */
  gst_controller_init (NULL, NULL);

  if (!gst_element_register (plugin, "v4l2src", GST_RANK_PRIMARY,
          GST_TYPE_V4L2SRC) ||
#ifdef HAVE_EXPERIMENTAL
      !gst_element_register (plugin, "v4l2sink", GST_RANK_NONE,
          GST_TYPE_V4L2SINK) ||
#endif
      !gst_element_register (plugin, "v4l2radio", GST_RANK_NONE,
          GST_TYPE_V4L2RADIO) ||
      /*       !gst_element_register (plugin, "v4l2jpegsrc", */
      /*           GST_RANK_NONE, GST_TYPE_V4L2JPEGSRC) || */
      /*       !gst_element_register (plugin, "v4l2mjpegsrc", */
      /*           GST_RANK_NONE, GST_TYPE_V4L2MJPEGSRC) || */
      /*       !gst_element_register (plugin, "v4l2mjpegsink", */
      /*           GST_RANK_NONE, GST_TYPE_V4L2MJPEGSINK)) */
      FALSE)
    return FALSE;

#ifdef ENABLE_NLS
  setlocale (LC_ALL, "");
  bindtextdomain (GETTEXT_PACKAGE, LOCALEDIR);
  bind_textdomain_codeset (GETTEXT_PACKAGE, "UTF-8");
#endif /* ENABLE_NLS */

  return TRUE;
}

GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    "video4linux2",
    "elements for Video 4 Linux",
    plugin_init, VERSION, GST_LICENSE, GST_PACKAGE_NAME, GST_PACKAGE_ORIGIN)
