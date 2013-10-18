/* GStreamer
 * Copyright (C) 1999,2000 Erik Walthinsen <omega@cse.ogi.edu>
 *                    2000 Wim Taymans <wtay@chello.be>
 *
 * gstelements.c:
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
#  include "config.h"
#endif

#include <gst/gst.h>

#ifdef GSTREAMER_LITE
#include "gstplugins-lite.h"
#include "gstqueue.h"
#include "gsttypefindelement.h"
#else // GSTREAMER_LITE
#include "gstcapsfilter.h"
#include "gstfakesink.h"
#include "gstfakesrc.h"
#include "gstfdsrc.h"
#include "gstfdsink.h"
#include "gstfilesink.h"
#include "gstfilesrc.h"
#include "gstfunnel.h"
#include "gstidentity.h"
#include "gstinputselector.h"
#include "gstoutputselector.h"
#include "gstmultiqueue.h"
#include "gstqueue.h"
#include "gstqueue2.h"
#include "gsttee.h"
#include "gsttypefindelement.h"
#include "gstvalve.h"
#endif // GSTREAMER_LITE

struct _elements_entry
{
  const gchar *name;
  guint rank;
    GType (*type) (void);
};


static struct _elements_entry _elements[] = {
#ifdef GSTREAMER_LITE
  {"queue", GST_RANK_NONE, gst_queue_get_type},
  {"typefind", GST_RANK_NONE, gst_type_find_element_get_type},
#else // GSTREAMER_LITE
  {"capsfilter", GST_RANK_NONE, gst_capsfilter_get_type},
  {"fakesrc", GST_RANK_NONE, gst_fake_src_get_type},
  {"fakesink", GST_RANK_NONE, gst_fake_sink_get_type},
#if defined(HAVE_SYS_SOCKET_H) || defined(_MSC_VER)
  {"fdsrc", GST_RANK_NONE, gst_fd_src_get_type},
  {"fdsink", GST_RANK_NONE, gst_fd_sink_get_type},
#endif 
  {"filesrc", GST_RANK_PRIMARY, gst_file_src_get_type},
  {"funnel", GST_RANK_NONE, gst_funnel_get_type},
  {"identity", GST_RANK_NONE, gst_identity_get_type},
  {"input-selector", GST_RANK_NONE, gst_input_selector_get_type},
  {"output-selector", GST_RANK_NONE, gst_output_selector_get_type},
  {"queue", GST_RANK_NONE, gst_queue_get_type},
  {"queue2", GST_RANK_NONE, gst_queue2_get_type},
  {"filesink", GST_RANK_PRIMARY, gst_file_sink_get_type},
  {"tee", GST_RANK_NONE, gst_tee_get_type},
  {"typefind", GST_RANK_NONE, gst_type_find_element_get_type},
  {"multiqueue", GST_RANK_NONE, gst_multi_queue_get_type},
  {"valve", GST_RANK_NONE, gst_valve_get_type},
#endif // GSTREAMER_LITE
  {NULL, 0},
};

#ifdef GSTREAMER_LITE
gboolean
plugin_init_elements (GstPlugin * plugin)
#else // GSTREAMER_LITE
static gboolean
plugin_init (GstPlugin * plugin)
#endif // GSTREAMER_LITE
{
  struct _elements_entry *my_elements = _elements;

  while ((*my_elements).name) {
    if (!gst_element_register (plugin, (*my_elements).name, (*my_elements).rank,
            ((*my_elements).type) ()))
      return FALSE;
    my_elements++;
  }

  return TRUE;
}

#ifndef GSTREAMER_LITE
GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    "coreelements",
    "standard GStreamer elements",
    plugin_init, VERSION, GST_LICENSE, GST_PACKAGE_NAME, GST_PACKAGE_ORIGIN);
#endif // GSTREAMER_LITE
