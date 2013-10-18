/*
 * GStreamer
 * Copyright (C) 2005,2006 Zaheer Abbas Merali <zaheerabbas at merali dot org>
 * Copyright (C) 2007,2008 Pioneers of the Inevitable <songbird@songbirdnest.com>
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
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
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
 * gst-launch filesrc location=sine.ogg ! oggdemux ! vorbisdec ! audioconvert ! audioresample ! osxaudiosink
 * ]| Play an Ogg/Vorbis file.
 * </refsect2>
 *
 * Last reviewed on 2006-03-01 (0.10.4)
 */

#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include <gst/gst.h>
#include <CoreAudio/CoreAudio.h>
#include <CoreAudio/AudioHardware.h>
#include "gstosxaudiosink.h"
#include "gstosxaudioelement.h"

GST_DEBUG_CATEGORY_STATIC (osx_audiosink_debug);
#define GST_CAT_DEFAULT osx_audiosink_debug

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

static GstStaticPadTemplate sink_factory = GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS ("audio/x-raw-float, "
        "endianness = (int) {" G_STRINGIFY (G_BYTE_ORDER) " }, "
        "signed = (boolean) { TRUE }, "
        "width = (int) 32, "
        "depth = (int) 32, "
        "rate = (int) [1, MAX], " "channels = (int) [1, MAX]")
    );

static void gst_osx_audio_sink_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec);
static void gst_osx_audio_sink_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec);

static GstRingBuffer *gst_osx_audio_sink_create_ringbuffer (GstBaseAudioSink *
    sink);
static void gst_osx_audio_sink_osxelement_init (gpointer g_iface,
    gpointer iface_data);
static void gst_osx_audio_sink_select_device (GstOsxAudioSink * osxsink);
static void gst_osx_audio_sink_set_volume (GstOsxAudioSink * sink);

static OSStatus gst_osx_audio_sink_io_proc (GstOsxRingBuffer * buf,
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
  GST_DEBUG ("Adding static interface");
  g_type_add_interface_static (type, GST_OSX_AUDIO_ELEMENT_TYPE,
      &osxelement_info);
}

GST_BOILERPLATE_FULL (GstOsxAudioSink, gst_osx_audio_sink, GstBaseAudioSink,
    GST_TYPE_BASE_AUDIO_SINK, gst_osx_audio_sink_do_init);

static void
gst_osx_audio_sink_base_init (gpointer g_class)
{
  GstElementClass *element_class = GST_ELEMENT_CLASS (g_class);

  gst_element_class_add_pad_template (element_class,
      gst_static_pad_template_get (&sink_factory));

  gst_element_class_set_details_simple (element_class, "Audio Sink (OSX)",
      "Sink/Audio",
      "Output to a sound card in OS X",
      "Zaheer Abbas Merali <zaheerabbas at merali dot org>");
}

static void
gst_osx_audio_sink_class_init (GstOsxAudioSinkClass * klass)
{
  GObjectClass *gobject_class;
  GstElementClass *gstelement_class;
  GstBaseSinkClass *gstbasesink_class;
  GstBaseAudioSinkClass *gstbaseaudiosink_class;

  gobject_class = (GObjectClass *) klass;
  gstelement_class = (GstElementClass *) klass;
  gstbasesink_class = (GstBaseSinkClass *) klass;
  gstbaseaudiosink_class = (GstBaseAudioSinkClass *) klass;

  parent_class = g_type_class_peek_parent (klass);

  gobject_class->set_property = gst_osx_audio_sink_set_property;
  gobject_class->get_property = gst_osx_audio_sink_get_property;

  g_object_class_install_property (gobject_class, ARG_DEVICE,
      g_param_spec_int ("device", "Device ID", "Device ID of output device",
          0, G_MAXINT, 0, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, ARG_VOLUME,
      g_param_spec_double ("volume", "Volume", "Volume of this stream",
          0, 1.0, 1.0, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  gstbaseaudiosink_class->create_ringbuffer =
      GST_DEBUG_FUNCPTR (gst_osx_audio_sink_create_ringbuffer);
}

static void
gst_osx_audio_sink_init (GstOsxAudioSink * sink, GstOsxAudioSinkClass * gclass)
{
  GST_DEBUG ("Initialising object");

  sink->device_id = kAudioDeviceUnknown;
  sink->volume = DEFAULT_VOLUME;
}

static void
gst_osx_audio_sink_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstOsxAudioSink *sink = GST_OSX_AUDIO_SINK (object);

  switch (prop_id) {
    case ARG_DEVICE:
      sink->device_id = g_value_get_int (value);
      break;
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
    case ARG_DEVICE:
      g_value_set_int (value, sink->device_id);
      break;
    case ARG_VOLUME:
      g_value_set_double (value, sink->volume);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static GstRingBuffer *
gst_osx_audio_sink_create_ringbuffer (GstBaseAudioSink * sink)
{
  GstOsxAudioSink *osxsink;
  GstOsxRingBuffer *ringbuffer;

  osxsink = GST_OSX_AUDIO_SINK (sink);

  gst_osx_audio_sink_select_device (osxsink);

  GST_DEBUG ("Creating ringbuffer");
  ringbuffer = g_object_new (GST_TYPE_OSX_RING_BUFFER, NULL);
  GST_DEBUG ("osx sink 0x%p element 0x%p  ioproc 0x%p", osxsink,
      GST_OSX_AUDIO_ELEMENT_GET_INTERFACE (osxsink),
      (void *) gst_osx_audio_sink_io_proc);

  gst_osx_audio_sink_set_volume (osxsink);

  ringbuffer->element = GST_OSX_AUDIO_ELEMENT_GET_INTERFACE (osxsink);
  ringbuffer->device_id = osxsink->device_id;

  return GST_RING_BUFFER (ringbuffer);
}

/* HALOutput AudioUnit will request fairly arbitrarily-sized chunks of data,
 * not of a fixed size. So, we keep track of where in the current ringbuffer
 * segment we are, and only advance the segment once we've read the whole
 * thing */
static OSStatus
gst_osx_audio_sink_io_proc (GstOsxRingBuffer * buf,
    AudioUnitRenderActionFlags * ioActionFlags,
    const AudioTimeStamp * inTimeStamp,
    UInt32 inBusNumber, UInt32 inNumberFrames, AudioBufferList * bufferList)
{
  guint8 *readptr;
  gint readseg;
  gint len;
  gint remaining = bufferList->mBuffers[0].mDataByteSize;
  gint offset = 0;

  while (remaining) {
    if (!gst_ring_buffer_prepare_read (GST_RING_BUFFER (buf),
            &readseg, &readptr, &len))
      return 0;

    len -= buf->segoffset;

    if (len > remaining)
      len = remaining;

    memcpy ((char *) bufferList->mBuffers[0].mData + offset,
        readptr + buf->segoffset, len);

    buf->segoffset += len;
    offset += len;
    remaining -= len;

    if ((gint) buf->segoffset == GST_RING_BUFFER (buf)->spec.segsize) {
      /* clear written samples */
      gst_ring_buffer_clear (GST_RING_BUFFER (buf), readseg);

      /* we wrote one segment */
      gst_ring_buffer_advance (GST_RING_BUFFER (buf), 1);

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
  if (!sink->audiounit)
    return;

  AudioUnitSetParameter (sink->audiounit, kHALOutputParam_Volume,
      kAudioUnitScope_Global, 0, (float) sink->volume, 0);
}

static void
gst_osx_audio_sink_select_device (GstOsxAudioSink * osxsink)
{
  OSStatus status;
  UInt32 propertySize;

  if (osxsink->device_id == kAudioDeviceUnknown) {
    /* If no specific device has been selected by the user, then pick the
     * default device */
    GST_DEBUG_OBJECT (osxsink, "Selecting device for OSXAudioSink");
    propertySize = sizeof (osxsink->device_id);
    status =
        AudioHardwareGetProperty (kAudioHardwarePropertyDefaultOutputDevice,
        &propertySize, &osxsink->device_id);

    if (status) {
      GST_WARNING_OBJECT (osxsink,
          "AudioHardwareGetProperty returned %d", (int) status);
    } else {
      GST_DEBUG_OBJECT (osxsink, "AudioHardwareGetProperty returned 0");
    }

    if (osxsink->device_id == kAudioDeviceUnknown) {
      GST_WARNING_OBJECT (osxsink,
          "AudioHardwareGetProperty: device_id is kAudioDeviceUnknown");
    }

    GST_DEBUG_OBJECT (osxsink, "AudioHardwareGetProperty: device_id is %lu",
        (long) osxsink->device_id);
  }
}
