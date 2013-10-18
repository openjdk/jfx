/* GStreamer
 * Copyright (C) 2006 Jan Schmidt <thaytan@noraisin.net>
 *
 * gstquark.c: Registered quarks for the _priv_gst_quark_table, private to 
 *   GStreamer
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

#include "gst_private.h"
#include "gstquark.h"

/* These strings must match order and number declared in the GstQuarkId
 * enum in gstquark.h! */
static const gchar *_quark_strings[] = {
  "format", "current", "duration", "rate",
  "seekable", "segment-start", "segment-end",
  "src_format", "src_value", "dest_format", "dest_value",
  "start_format", "start_value", "stop_format", "stop_value",
  "gerror", "debug", "buffer-percent", "buffering-mode",
  "avg-in-rate", "avg-out-rate", "buffering-left",
  "estimated-total", "old-state", "new-state", "pending-state",
  "clock", "ready", "position", "new-base-time", "live", "min-latency",
  "max-latency", "busy", "type", "owner", "update", "applied-rate",
  "start", "stop", "minsize", "maxsize", "async", "proportion",
  "diff", "timestamp", "flags", "cur-type", "cur", "stop-type",
  "latency", "uri", "object", "taglist", "GstEventNewsegment",
  "GstEventBufferSize", "GstEventQOS", "GstEventSeek", "GstEventLatency",
  "GstMessageError", "GstMessageWarning", "GstMessageInfo",
  "GstMessageBuffering", "GstMessageState", "GstMessageClockProvide",
  "GstMessageClockLost", "GstMessageNewClock", "GstMessageStructureChange",
  "GstMessageSegmentStart", "GstMessageSegmentDone", "GstMessageDuration",
  "GstMessageAsyncStart", "GstMessageRequestState", "GstMessageStreamStatus",
  "GstQueryPosition", "GstQueryDuration", "GstQueryLatency", "GstQueryConvert",
  "GstQuerySegment", "GstQuerySeeking", "GstQueryFormats", "GstQueryBuffering",
  "GstQueryURI", "GstEventStep", "GstMessageStepDone", "amount", "flush",
  "intermediate", "GstMessageStepStart", "active", "eos", "sink-message",
  "message", "GstMessageQOS", "running-time", "stream-time", "jitter",
  "quality", "processed", "dropped", "buffering-ranges", "GstMessageProgress",
  "code", "text", "percent", "timeout"
};

GQuark _priv_gst_quark_table[GST_QUARK_MAX];

void
_priv_gst_quarks_initialize (void)
{
  gint i;

  if (G_N_ELEMENTS (_quark_strings) != GST_QUARK_MAX)
    g_warning ("the quark table is not consistent! %d != %d",
        (int) G_N_ELEMENTS (_quark_strings), GST_QUARK_MAX);

  for (i = 0; i < GST_QUARK_MAX; i++) {
    _priv_gst_quark_table[i] = g_quark_from_static_string (_quark_strings[i]);
  }
}
