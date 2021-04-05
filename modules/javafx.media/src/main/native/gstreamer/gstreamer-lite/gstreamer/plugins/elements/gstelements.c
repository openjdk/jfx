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
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 */
/**
 * plugin-coreelements:
 *
 * GStreamer core elements
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
#include "gstclocksync.h"
#include "gstconcat.h"
#include "gstdataurisrc.h"
#include "gstdownloadbuffer.h"
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
#include "gststreamiddemux.h"
#endif // GSTREAMER_LITE


#ifdef GSTREAMER_LITE
gboolean
plugin_init_elements (GstPlugin * plugin)
#else // GSTREAMER_LITE
static gboolean
plugin_init (GstPlugin * plugin)
#endif // GSTREAMER_LITE
{
#ifndef GSTREAMER_LITE
  if (!gst_element_register (plugin, "capsfilter", GST_RANK_NONE,
          gst_capsfilter_get_type ()))
    return FALSE;
  if (!gst_element_register (plugin, "clocksync", GST_RANK_NONE,
          gst_clock_sync_get_type ()))
    return FALSE;
  if (!gst_element_register (plugin, "concat", GST_RANK_NONE,
          gst_concat_get_type ()))
    return FALSE;
  if (!gst_element_register (plugin, "dataurisrc", GST_RANK_PRIMARY,
          gst_data_uri_src_get_type ()))
    return FALSE;
  if (!gst_element_register (plugin, "downloadbuffer", GST_RANK_NONE,
          gst_download_buffer_get_type ()))
    return FALSE;
  if (!gst_element_register (plugin, "fakesrc", GST_RANK_NONE,
          gst_fake_src_get_type ()))
    return FALSE;
  if (!gst_element_register (plugin, "fakesink", GST_RANK_NONE,
          gst_fake_sink_get_type ()))
    return FALSE;
#if defined(HAVE_SYS_SOCKET_H) || defined(_MSC_VER)
  if (!gst_element_register (plugin, "fdsrc", GST_RANK_NONE,
          gst_fd_src_get_type ()))
    return FALSE;
  if (!gst_element_register (plugin, "fdsink", GST_RANK_NONE,
          gst_fd_sink_get_type ()))
    return FALSE;
#endif
  if (!gst_element_register (plugin, "filesrc", GST_RANK_PRIMARY,
          gst_file_src_get_type ()))
    return FALSE;
  if (!gst_element_register (plugin, "funnel", GST_RANK_NONE,
          gst_funnel_get_type ()))
    return FALSE;
  if (!gst_element_register (plugin, "identity", GST_RANK_NONE,
          gst_identity_get_type ()))
    return FALSE;
  if (!gst_element_register (plugin, "input-selector", GST_RANK_NONE,
          gst_input_selector_get_type ()))
    return FALSE;
  if (!gst_element_register (plugin, "output-selector", GST_RANK_NONE,
          gst_output_selector_get_type ()))
    return FALSE;
#endif // GSTREAMER_LITE
  if (!gst_element_register (plugin, "queue", GST_RANK_NONE,
          gst_queue_get_type ()))
    return FALSE;
#ifndef GSTREAMER_LITE
  if (!gst_element_register (plugin, "queue2", GST_RANK_NONE,
          gst_queue2_get_type ()))
    return FALSE;
  if (!gst_element_register (plugin, "filesink", GST_RANK_PRIMARY,
          gst_file_sink_get_type ()))
    return FALSE;
  if (!gst_element_register (plugin, "tee", GST_RANK_NONE, gst_tee_get_type ()))
    return FALSE;
#endif // GSTREAMER_LITE
  if (!gst_element_register (plugin, "typefind", GST_RANK_NONE,
          gst_type_find_element_get_type ()))
    return FALSE;
#ifndef GSTREAMER_LITE
  if (!gst_element_register (plugin, "multiqueue", GST_RANK_NONE,
          gst_multi_queue_get_type ()))
    return FALSE;
  if (!gst_element_register (plugin, "valve", GST_RANK_NONE,
          gst_valve_get_type ()))
    return FALSE;

  if (!gst_element_register (plugin, "streamiddemux", GST_RANK_PRIMARY,
          gst_streamid_demux_get_type ()))
    return FALSE;
#endif // GSTREAMER_LITE

  return TRUE;
}

#ifndef GSTREAMER_LITE
GST_PLUGIN_DEFINE (GST_VERSION_MAJOR, GST_VERSION_MINOR, coreelements,
    "GStreamer core elements", plugin_init, VERSION, GST_LICENSE,
    GST_PACKAGE_NAME, GST_PACKAGE_ORIGIN);
#endif // GSTREAMER_LITE
