/*
 * GStreamer
 * Copyright (C) 2005,2006 Zaheer Abbas Merali <zaheerabbas at merali dot org>
 * Copyright (C) 2007,2008 Pioneers of the Inevitable <songbird@songbirdnest.com>
 * Copyright (C) 2012 Fluendo S.A. <support@fluendo.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 * Alternatively, the contents of this file may be used under the
 * GNU Lesser General Public License Version 2.1 (the "LGPL"), in
 * which case the following provisions apply instead of the ones
 * mentioned above:
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
 * The development of this code was made possible due to the involvement of
 * Pioneers of the Inevitable, the creators of the Songbird Music player
 *
 */

/**
 * SECTION:element-osxaudiosink
 *
 * This element renders raw audio samples using the CoreAudio api.
 *
 * <refsect2>
 * <title>Example pipelines</title>
 * |[
 * gst-launch-1.0 filesrc location=sine.ogg ! oggdemux ! vorbisdec ! audioconvert ! audioresample ! osxaudiosink
 * ]| Play an Ogg/Vorbis file.
 * </refsect2>
 */

#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include <gst/gst.h>
#include <gst/audio/audio.h>
#include <gst/audio/audio-channels.h>
#include <gst/audio/gstaudioiec61937.h>

#include "gstosxaudiosink.h"
#include "gstosxaudioelement.h"

GST_DEBUG_CATEGORY_STATIC (osx_audiosink_debug);
#define GST_CAT_DEFAULT osx_audiosink_debug

#include "gstosxcoreaudio.h"

/* Filter signals and args */
enum
{
  /* FILL ME */
  LAST_SIGNAL
};

enum
{
  ARG_0,
  ARG_DEVICE,
  ARG_VOLUME
};

#define DEFAULT_VOLUME 1.0

#if (G_BYTE_ORDER == G_LITTLE_ENDIAN)
# define FORMATS "{ S32LE, S24LE, S16LE, U8 }"
#else
# define FORMATS "{ S32BE, S24BE, S16BE, U8 }"
#endif

static GstStaticPadTemplate sink_factory = GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS ("audio/x-raw, "
        "format = (string) " FORMATS ", "
        "layout = (string) interleaved, "
        "rate = (int) [1, MAX], "
        "channels = (int) [1, 9];"
        "audio/x-ac3, framed = (boolean) true;"
        "audio/x-dts, framed = (boolean) true")
    );

static void gst_osx_audio_sink_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec);
static void gst_osx_audio_sink_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec);

static gboolean gst_osx_audio_sink_query (GstBaseSink * base, GstQuery * query);

static gboolean gst_osx_audio_sink_stop (GstBaseSink * base);
#ifdef GSTREAMER_LITE
static gboolean gst_osx_audio_sink_start (GstBaseSink * base);
#endif
static GstCaps *gst_osx_audio_sink_getcaps (GstBaseSink * base,
    GstCaps * filter);
static gboolean gst_osx_audio_sink_acceptcaps (GstOsxAudioSink * sink,
    GstCaps * caps);

static GstBuffer *gst_osx_audio_sink_sink_payload (GstAudioBaseSink * sink,
    GstBuffer * buf);
static GstAudioRingBuffer
    * gst_osx_audio_sink_create_ringbuffer (GstAudioBaseSink * sink);
static void gst_osx_audio_sink_osxelement_init (gpointer g_iface,
    gpointer iface_data);
static gboolean gst_osx_audio_sink_select_device (GstOsxAudioSink * osxsink);
static void gst_osx_audio_sink_set_volume (GstOsxAudioSink * sink);

static OSStatus gst_osx_audio_sink_io_proc (GstOsxAudioRingBuffer * buf,
    AudioUnitRenderActionFlags * ioActionFlags,
    const AudioTimeStamp * inTimeStamp,
    UInt32 inBusNumber, UInt32 inNumberFrames, AudioBufferList * bufferList);

static void
gst_osx_audio_sink_do_init (GType type)
{
  static const GInterfaceInfo osxelement_info = {
    gst_osx_audio_sink_osxelement_init,
    NULL,
    NULL
  };

  GST_DEBUG_CATEGORY_INIT (osx_audiosink_debug, "osxaudiosink", 0,
      "OSX Audio Sink");
  gst_core_audio_init_debug ();
  GST_DEBUG ("Adding static interface");
  g_type_add_interface_static (type, GST_OSX_AUDIO_ELEMENT_TYPE,
      &osxelement_info);
}

#define gst_osx_audio_sink_parent_class parent_class
G_DEFINE_TYPE_WITH_CODE (GstOsxAudioSink, gst_osx_audio_sink,
    GST_TYPE_AUDIO_BASE_SINK, gst_osx_audio_sink_do_init (g_define_type_id));

static void
gst_osx_audio_sink_class_init (GstOsxAudioSinkClass * klass)
{
  GObjectClass *gobject_class;
  GstElementClass *gstelement_class;
  GstBaseSinkClass *gstbasesink_class;
  GstAudioBaseSinkClass *gstaudiobasesink_class;

  gobject_class = (GObjectClass *) klass;
  gstelement_class = (GstElementClass *) klass;
  gstbasesink_class = (GstBaseSinkClass *) klass;
  gstaudiobasesink_class = (GstAudioBaseSinkClass *) klass;

  parent_class = g_type_class_peek_parent (klass);

  gobject_class->set_property = gst_osx_audio_sink_set_property;
  gobject_class->get_property = gst_osx_audio_sink_get_property;

#ifndef HAVE_IOS
  g_object_class_install_property (gobject_class, ARG_DEVICE,
      g_param_spec_int ("device", "Device ID", "Device ID of output device",
          0, G_MAXINT, 0, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
#endif

  gstbasesink_class->query = GST_DEBUG_FUNCPTR (gst_osx_audio_sink_query);

  g_object_class_install_property (gobject_class, ARG_VOLUME,
      g_param_spec_double ("volume", "Volume", "Volume of this stream",
          0, 1.0, 1.0, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  gstbasesink_class->get_caps = GST_DEBUG_FUNCPTR (gst_osx_audio_sink_getcaps);
  gstbasesink_class->stop = GST_DEBUG_FUNCPTR (gst_osx_audio_sink_stop);
#ifdef GSTREAMER_LITE
  gstbasesink_class->start = GST_DEBUG_FUNCPTR (gst_osx_audio_sink_start);
#endif

  gstaudiobasesink_class->create_ringbuffer =
      GST_DEBUG_FUNCPTR (gst_osx_audio_sink_create_ringbuffer);
  gstaudiobasesink_class->payload =
      GST_DEBUG_FUNCPTR (gst_osx_audio_sink_sink_payload);

  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&sink_factory));

  gst_element_class_set_static_metadata (gstelement_class, "Audio Sink (OSX)",
      "Sink/Audio",
      "Output to a sound card in OS X",
      "Zaheer Abbas Merali <zaheerabbas at merali dot org>");
}

static void
gst_osx_audio_sink_init (GstOsxAudioSink * sink)
{
  gint i;

  GST_DEBUG ("Initialising object");

  sink->device_id = kAudioDeviceUnknown;
  sink->cached_caps = NULL;

  sink->volume = DEFAULT_VOLUME;

  sink->channels = 0;
  for (i = 0; i < GST_OSX_AUDIO_MAX_CHANNEL; i++) {
    sink->channel_positions[i] = GST_AUDIO_CHANNEL_POSITION_INVALID;
  }
}

static void
gst_osx_audio_sink_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstOsxAudioSink *sink = GST_OSX_AUDIO_SINK (object);

  switch (prop_id) {
#ifndef HAVE_IOS
    case ARG_DEVICE:
      sink->device_id = g_value_get_int (value);
      break;
#endif
    case ARG_VOLUME:
      sink->volume = g_value_get_double (value);
      gst_osx_audio_sink_set_volume (sink);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_osx_audio_sink_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  GstOsxAudioSink *sink = GST_OSX_AUDIO_SINK (object);
  switch (prop_id) {
#ifndef HAVE_IOS
    case ARG_DEVICE:
      g_value_set_int (value, sink->device_id);
      break;
#endif
    case ARG_VOLUME:
      g_value_set_double (value, sink->volume);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static gboolean
gst_osx_audio_sink_query (GstBaseSink * base, GstQuery * query)
{
  GstOsxAudioSink *sink = GST_OSX_AUDIO_SINK (base);
  gboolean ret = FALSE;

  switch (GST_QUERY_TYPE (query)) {
    case GST_QUERY_ACCEPT_CAPS:
    {
      GstCaps *caps = NULL;

      gst_query_parse_accept_caps (query, &caps);
      ret = gst_osx_audio_sink_acceptcaps (sink, caps);
      gst_query_set_accept_caps_result (query, ret);
      ret = TRUE;
      break;
    }
    default:
      ret = GST_BASE_SINK_CLASS (parent_class)->query (base, query);
      break;
  }
  return ret;
}

static gboolean
gst_osx_audio_sink_stop (GstBaseSink * base)
{
  GstOsxAudioSink *sink = GST_OSX_AUDIO_SINK (base);

  if (sink->cached_caps) {
    gst_caps_unref (sink->cached_caps);
    sink->cached_caps = NULL;
  }

  return GST_CALL_PARENT_WITH_DEFAULT (GST_BASE_SINK_CLASS, stop, (base), TRUE);
}

#ifdef GSTREAMER_LITE
static gboolean
gst_osx_audio_sink_start (GstBaseSink * base)
{
  GstOsxAudioSink *sink = GST_OSX_AUDIO_SINK (base);

  gst_osx_audio_sink_set_volume(sink);

  return GST_CALL_PARENT_WITH_DEFAULT (GST_BASE_SINK_CLASS, start, (base), TRUE);
}
#endif

static GstCaps *
gst_osx_audio_sink_getcaps (GstBaseSink * base, GstCaps * filter)
{
  GstOsxAudioSink *sink = GST_OSX_AUDIO_SINK (base);
  gchar *caps_string = NULL;

  if (sink->cached_caps) {
    caps_string = gst_caps_to_string (sink->cached_caps);
    GST_DEBUG_OBJECT (sink, "using cached caps: %s", caps_string);
    g_free (caps_string);
    if (filter)
      return gst_caps_intersect_full (sink->cached_caps, filter,
          GST_CAPS_INTERSECT_FIRST);
    return gst_caps_ref (sink->cached_caps);
  }

  GST_DEBUG_OBJECT (sink, "using template caps");
  return NULL;
}

static gboolean
gst_osx_audio_sink_acceptcaps (GstOsxAudioSink * sink, GstCaps * caps)
{
  GstCaps *pad_caps;
  GstStructure *st;
  gboolean ret = FALSE;
  GstAudioRingBufferSpec spec = { 0 };
  gchar *caps_string = NULL;

  caps_string = gst_caps_to_string (caps);
  GST_DEBUG_OBJECT (sink, "acceptcaps called with %s", caps_string);
  g_free (caps_string);

  pad_caps = gst_pad_query_caps (GST_BASE_SINK_PAD (sink), caps);
  if (pad_caps) {
    gboolean cret = gst_caps_can_intersect (pad_caps, caps);
    gst_caps_unref (pad_caps);
    if (!cret)
      goto done;
  }

  /* If we've not got fixed caps, creating a stream might fail,
   * so let's just return from here with default acceptcaps
   * behaviour */
  if (!gst_caps_is_fixed (caps))
    goto done;

  /* parse helper expects this set, so avoid nasty warning
   * will be set properly later on anyway  */
  spec.latency_time = GST_SECOND;
  if (!gst_audio_ring_buffer_parse_caps (&spec, caps))
    goto done;

  /* Make sure input is framed and can be payloaded */
  switch (spec.type) {
    case GST_AUDIO_RING_BUFFER_FORMAT_TYPE_AC3:
    {
      gboolean framed = FALSE;

      st = gst_caps_get_structure (caps, 0);

      gst_structure_get_boolean (st, "framed", &framed);
      if (!framed || gst_audio_iec61937_frame_size (&spec) <= 0)
        goto done;
      break;
    }
    case GST_AUDIO_RING_BUFFER_FORMAT_TYPE_DTS:
    {
      gboolean parsed = FALSE;

      st = gst_caps_get_structure (caps, 0);

      gst_structure_get_boolean (st, "parsed", &parsed);
      if (!parsed || gst_audio_iec61937_frame_size (&spec) <= 0)
        goto done;
      break;
    }
    default:
      break;
  }
  ret = TRUE;

done:
  return ret;
}

static GstBuffer *
gst_osx_audio_sink_sink_payload (GstAudioBaseSink * sink, GstBuffer * buf)
{
  if (RINGBUFFER_IS_SPDIF (sink->ringbuffer->spec.type)) {
    gint framesize = gst_audio_iec61937_frame_size (&sink->ringbuffer->spec);
    GstBuffer *out;
    GstMapInfo inmap, outmap;
    gboolean res;

    if (framesize <= 0)
      return NULL;

    out = gst_buffer_new_and_alloc (framesize);

    gst_buffer_map (buf, &inmap, GST_MAP_READ);
    gst_buffer_map (out, &outmap, GST_MAP_WRITE);

    /* FIXME: the endianness needs to be queried and then set */
    res = gst_audio_iec61937_payload (inmap.data, inmap.size,
        outmap.data, outmap.size, &sink->ringbuffer->spec, G_BIG_ENDIAN);

    gst_buffer_unmap (buf, &inmap);
    gst_buffer_unmap (out, &outmap);

    if (!res) {
      gst_buffer_unref (out);
      return NULL;
    }

    gst_buffer_copy_into (out, buf, GST_BUFFER_COPY_METADATA, 0, -1);
    return out;

  } else {
    return gst_buffer_ref (buf);
  }
}

static GstAudioRingBuffer *
gst_osx_audio_sink_create_ringbuffer (GstAudioBaseSink * sink)
{
  GstOsxAudioSink *osxsink;
  GstOsxAudioRingBuffer *ringbuffer;

  osxsink = GST_OSX_AUDIO_SINK (sink);

  if (!gst_osx_audio_sink_select_device (osxsink)) {
    GST_ERROR_OBJECT (sink, "Could not select device");
    return NULL;
  }

  GST_DEBUG_OBJECT (sink, "Creating ringbuffer");
  ringbuffer = g_object_new (GST_TYPE_OSX_AUDIO_RING_BUFFER, NULL);
  GST_DEBUG_OBJECT (sink, "osx sink %p element %p  ioproc %p", osxsink,
      GST_OSX_AUDIO_ELEMENT_GET_INTERFACE (osxsink),
      (void *) gst_osx_audio_sink_io_proc);

  gst_osx_audio_sink_set_volume (osxsink);

  ringbuffer->core_audio->element =
      GST_OSX_AUDIO_ELEMENT_GET_INTERFACE (osxsink);
  ringbuffer->core_audio->device_id = osxsink->device_id;
  ringbuffer->core_audio->is_src = FALSE;

  return GST_AUDIO_RING_BUFFER (ringbuffer);
}

/* HALOutput AudioUnit will request fairly arbitrarily-sized chunks
 * of data, not of a fixed size. So, we keep track of where in
 * the current ringbuffer segment we are, and only advance the segment
 * once we've read the whole thing */
static OSStatus
gst_osx_audio_sink_io_proc (GstOsxAudioRingBuffer * buf,
    AudioUnitRenderActionFlags * ioActionFlags,
    const AudioTimeStamp * inTimeStamp,
    UInt32 inBusNumber, UInt32 inNumberFrames, AudioBufferList * bufferList)
{
  guint8 *readptr;
  gint readseg;
  gint len;
  gint stream_idx = buf->core_audio->stream_idx;
  gint remaining = bufferList->mBuffers[stream_idx].mDataByteSize;
  gint offset = 0;

  while (remaining) {
    if (!gst_audio_ring_buffer_prepare_read (GST_AUDIO_RING_BUFFER (buf),
            &readseg, &readptr, &len))
      return 0;

    len -= buf->segoffset;

    if (len > remaining)
      len = remaining;

    memcpy ((char *) bufferList->mBuffers[stream_idx].mData + offset,
        readptr + buf->segoffset, len);

    buf->segoffset += len;
    offset += len;
    remaining -= len;

    if ((gint) buf->segoffset == GST_AUDIO_RING_BUFFER (buf)->spec.segsize) {
      /* clear written samples */
      gst_audio_ring_buffer_clear (GST_AUDIO_RING_BUFFER (buf), readseg);

      /* we wrote one segment */
      gst_audio_ring_buffer_advance (GST_AUDIO_RING_BUFFER (buf), 1);

      buf->segoffset = 0;
    }
  }
  return 0;
}

static void
gst_osx_audio_sink_osxelement_init (gpointer g_iface, gpointer iface_data)
{
  GstOsxAudioElementInterface *iface = (GstOsxAudioElementInterface *) g_iface;

  iface->io_proc = (AURenderCallback) gst_osx_audio_sink_io_proc;
}

static void
gst_osx_audio_sink_set_volume (GstOsxAudioSink * sink)
{
  GstOsxAudioRingBuffer *osxbuf;

  osxbuf = GST_OSX_AUDIO_RING_BUFFER (GST_AUDIO_BASE_SINK (sink)->ringbuffer);
  if (!osxbuf)
    return;

  gst_core_audio_set_volume (osxbuf->core_audio, sink->volume);
}

static gboolean
gst_osx_audio_sink_allowed_caps (GstOsxAudioSink * osxsink)
{
  gint i, channels;
  gboolean spdif_allowed;
  AudioChannelLayout *layout;
  GstElementClass *element_class;
  GstPadTemplate *pad_template;
  GstCaps *caps, *in_caps;
  guint64 channel_mask = 0;
  GstAudioChannelPosition *pos = osxsink->channel_positions;

  /* First collect info about the HW capabilites and preferences */
  spdif_allowed =
      gst_core_audio_audio_device_is_spdif_avail (osxsink->device_id);
  layout = gst_core_audio_audio_device_get_channel_layout (osxsink->device_id);

  GST_DEBUG_OBJECT (osxsink, "Selected device ID: %u SPDIF allowed: %d",
      (unsigned) osxsink->device_id, spdif_allowed);

  if (layout) {
    channels = MIN (layout->mNumberChannelDescriptions,
        GST_OSX_AUDIO_MAX_CHANNEL);
  } else {
    GST_WARNING_OBJECT (osxsink, "This driver does not support "
        "kAudioDevicePropertyPreferredChannelLayout.");
    channels = 2;
  }

  switch (channels) {
    case 0:
      pos[0] = GST_AUDIO_CHANNEL_POSITION_NONE;
      break;
    case 1:
      pos[0] = GST_AUDIO_CHANNEL_POSITION_MONO;
      break;
    case 2:
      pos[0] = GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT;
      pos[1] = GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT;
      channel_mask |= GST_AUDIO_CHANNEL_POSITION_MASK (FRONT_LEFT);
      channel_mask |= GST_AUDIO_CHANNEL_POSITION_MASK (FRONT_RIGHT);
      break;
    default:
      channels = MIN (layout->mNumberChannelDescriptions,
          GST_OSX_AUDIO_MAX_CHANNEL);
      for (i = 0; i < channels; i++) {
        switch (layout->mChannelDescriptions[i].mChannelLabel) {
          case kAudioChannelLabel_Left:
            pos[i] = GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT;
            break;
          case kAudioChannelLabel_Right:
            pos[i] = GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT;
            break;
          case kAudioChannelLabel_Center:
            pos[i] = GST_AUDIO_CHANNEL_POSITION_FRONT_CENTER;
            break;
          case kAudioChannelLabel_LFEScreen:
            pos[i] = GST_AUDIO_CHANNEL_POSITION_LFE1;
            break;
          case kAudioChannelLabel_LeftSurround:
            pos[i] = GST_AUDIO_CHANNEL_POSITION_REAR_LEFT;
            break;
          case kAudioChannelLabel_RightSurround:
            pos[i] = GST_AUDIO_CHANNEL_POSITION_REAR_RIGHT;
            break;
          case kAudioChannelLabel_RearSurroundLeft:
            pos[i] = GST_AUDIO_CHANNEL_POSITION_SIDE_LEFT;
            break;
          case kAudioChannelLabel_RearSurroundRight:
            pos[i] = GST_AUDIO_CHANNEL_POSITION_SIDE_RIGHT;
            break;
          case kAudioChannelLabel_CenterSurround:
            pos[i] = GST_AUDIO_CHANNEL_POSITION_REAR_CENTER;
            break;
          default:
            GST_WARNING_OBJECT (osxsink, "unrecognized channel: %d",
                (int) layout->mChannelDescriptions[i].mChannelLabel);
            channel_mask = 0;
            channels = 2;
            break;
        }
      }
  }
  g_free (layout);

  /* Recover the template caps */
  element_class = GST_ELEMENT_GET_CLASS (osxsink);
  pad_template = gst_element_class_get_pad_template (element_class, "sink");
  in_caps = gst_pad_template_get_caps (pad_template);

  /* Create the allowed subset  */
  caps = gst_caps_new_empty ();
  for (i = 0; i < gst_caps_get_size (in_caps); i++) {
    GstStructure *in_s, *out_s;

    in_s = gst_caps_get_structure (in_caps, i);

    if (gst_structure_has_name (in_s, "audio/x-ac3") ||
        gst_structure_has_name (in_s, "audio/x-dts")) {
      if (spdif_allowed) {
        gst_caps_append_structure (caps, gst_structure_copy (in_s));
      }
    }
    gst_audio_channel_positions_to_mask (pos, channels, false, &channel_mask);
    out_s = gst_structure_copy (in_s);
    gst_structure_remove_fields (out_s, "channels", "channel-mask", NULL);
#ifdef GSTREAMER_LITE
    gst_structure_set (out_s, "channels", G_TYPE_INT, channels,
        NULL);
#else // GSTREAMER_LITE
    gst_structure_set (out_s, "channels", G_TYPE_INT, channels,
        "channel-mask", GST_TYPE_BITMASK, channel_mask, NULL);
#endif // GSTREAMER_LITE
    gst_caps_append_structure (caps, out_s);
  }

  if (osxsink->cached_caps) {
    gst_caps_unref (osxsink->cached_caps);
  }

  osxsink->cached_caps = caps;
  osxsink->channels = channels;

  return TRUE;
}

static gboolean
gst_osx_audio_sink_select_device (GstOsxAudioSink * osxsink)
{
  gboolean res = FALSE;

  if (!gst_core_audio_select_device (&osxsink->device_id))
    return FALSE;
  res = gst_osx_audio_sink_allowed_caps (osxsink);

  return res;
}
