/*
 * GStreamer
 * Copyright (C) 2005,2006 Zaheer Abbas Merali <zaheerabbas at merali dot org>
 * Copyright (C) 2008 Pioneers of the Inevitable <songbird@songbirdnest.com>
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
 */

/**
 * SECTION:element-osxaudiosrc
 *
 * This element captures raw audio samples using the CoreAudio api.
 *
 * <refsect2>
 * <title>Example launch line</title>
 * |[
 * gst-launch osxaudiosrc ! wavenc ! filesink location=audio.wav
 * ]|
 * </refsect2>
 */

#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include <gst/gst.h>
#include <CoreAudio/CoreAudio.h>
#include <CoreAudio/AudioHardware.h>
#include "gstosxaudiosrc.h"
#include "gstosxaudioelement.h"

GST_DEBUG_CATEGORY_STATIC (osx_audiosrc_debug);
#define GST_CAT_DEFAULT osx_audiosrc_debug

/* Filter signals and args */
enum
{
  /* FILL ME */
  LAST_SIGNAL
};

enum
{
  ARG_0,
  ARG_DEVICE
};

static GstStaticPadTemplate src_factory = GST_STATIC_PAD_TEMPLATE ("src",
    GST_PAD_SRC,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS ("audio/x-raw-float, "
        "endianness = (int) {" G_STRINGIFY (G_BYTE_ORDER) " }, "
        "signed = (boolean) { TRUE }, "
        "width = (int) 32, "
        "depth = (int) 32, "
        "rate = (int) [1, MAX], " "channels = (int) [1, MAX]")
    );

static void gst_osx_audio_src_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec);
static void gst_osx_audio_src_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec);

static GstCaps *gst_osx_audio_src_get_caps (GstBaseSrc * src);

static GstRingBuffer *gst_osx_audio_src_create_ringbuffer (GstBaseAudioSrc *
    src);
static void gst_osx_audio_src_osxelement_init (gpointer g_iface,
    gpointer iface_data);
static OSStatus gst_osx_audio_src_io_proc (GstOsxRingBuffer * buf,
    AudioUnitRenderActionFlags * ioActionFlags,
    const AudioTimeStamp * inTimeStamp, UInt32 inBusNumber,
    UInt32 inNumberFrames, AudioBufferList * bufferList);
static void gst_osx_audio_src_select_device (GstOsxAudioSrc * osxsrc);

static void
gst_osx_audio_src_do_init (GType type)
{
  static const GInterfaceInfo osxelement_info = {
    gst_osx_audio_src_osxelement_init,
    NULL,
    NULL
  };

  GST_DEBUG_CATEGORY_INIT (osx_audiosrc_debug, "osxaudiosrc", 0,
      "OSX Audio Src");
  GST_DEBUG ("Adding static interface");
  g_type_add_interface_static (type, GST_OSX_AUDIO_ELEMENT_TYPE,
      &osxelement_info);
}

GST_BOILERPLATE_FULL (GstOsxAudioSrc, gst_osx_audio_src, GstBaseAudioSrc,
    GST_TYPE_BASE_AUDIO_SRC, gst_osx_audio_src_do_init);

static void
gst_osx_audio_src_base_init (gpointer g_class)
{
  GstElementClass *element_class = GST_ELEMENT_CLASS (g_class);

  gst_element_class_add_pad_template (element_class,
      gst_static_pad_template_get (&src_factory));

  gst_element_class_set_details_simple (element_class, "Audio Source (OSX)",
      "Source/Audio",
      "Input from a sound card in OS X",
      "Zaheer Abbas Merali <zaheerabbas at merali dot org>");
}

static void
gst_osx_audio_src_class_init (GstOsxAudioSrcClass * klass)
{
  GObjectClass *gobject_class;
  GstElementClass *gstelement_class;
  GstBaseSrcClass *gstbasesrc_class;
  GstBaseAudioSrcClass *gstbaseaudiosrc_class;

  gobject_class = (GObjectClass *) klass;
  gstelement_class = (GstElementClass *) klass;
  gstbasesrc_class = (GstBaseSrcClass *) klass;
  gstbaseaudiosrc_class = (GstBaseAudioSrcClass *) klass;

  parent_class = g_type_class_peek_parent (klass);

  gobject_class->set_property = gst_osx_audio_src_set_property;
  gobject_class->get_property = gst_osx_audio_src_get_property;

  gstbasesrc_class->get_caps = GST_DEBUG_FUNCPTR (gst_osx_audio_src_get_caps);

  g_object_class_install_property (gobject_class, ARG_DEVICE,
      g_param_spec_int ("device", "Device ID", "Device ID of input device",
          0, G_MAXINT, 0, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  gstbaseaudiosrc_class->create_ringbuffer =
      GST_DEBUG_FUNCPTR (gst_osx_audio_src_create_ringbuffer);
}

static void
gst_osx_audio_src_init (GstOsxAudioSrc * src, GstOsxAudioSrcClass * gclass)
{
  gst_base_src_set_live (GST_BASE_SRC (src), TRUE);

  src->device_id = kAudioDeviceUnknown;
  src->deviceChannels = -1;
}

static void
gst_osx_audio_src_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstOsxAudioSrc *src = GST_OSX_AUDIO_SRC (object);

  switch (prop_id) {
    case ARG_DEVICE:
      src->device_id = g_value_get_int (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_osx_audio_src_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  GstOsxAudioSrc *src = GST_OSX_AUDIO_SRC (object);

  switch (prop_id) {
    case ARG_DEVICE:
      g_value_set_int (value, src->device_id);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static GstCaps *
gst_osx_audio_src_get_caps (GstBaseSrc * src)
{
  GstElementClass *gstelement_class;
  GstOsxAudioSrc *osxsrc;
  GstPadTemplate *pad_template;
  GstCaps *caps;
  gint min, max;

  gstelement_class = GST_ELEMENT_GET_CLASS (src);
  osxsrc = GST_OSX_AUDIO_SRC (src);

  if (osxsrc->deviceChannels == -1) {
    /* -1 means we don't know the number of channels yet.  for now, return
     * template caps.
     */
    return NULL;
  }

  max = osxsrc->deviceChannels;
  if (max < 1)
    max = 1;                    /* 0 channels means 1 channel? */

  min = MIN (1, max);

  pad_template = gst_element_class_get_pad_template (gstelement_class, "src");
  g_return_val_if_fail (pad_template != NULL, NULL);

  caps = gst_caps_copy (gst_pad_template_get_caps (pad_template));

  if (min == max) {
    gst_caps_set_simple (caps, "channels", G_TYPE_INT, max, NULL);
  } else {
    gst_caps_set_simple (caps, "channels", GST_TYPE_INT_RANGE, min, max, NULL);
  }

  return caps;
}

static GstRingBuffer *
gst_osx_audio_src_create_ringbuffer (GstBaseAudioSrc * src)
{
  GstOsxAudioSrc *osxsrc;
  GstOsxRingBuffer *ringbuffer;

  osxsrc = GST_OSX_AUDIO_SRC (src);

  gst_osx_audio_src_select_device (osxsrc);

  GST_DEBUG ("Creating ringbuffer");
  ringbuffer = g_object_new (GST_TYPE_OSX_RING_BUFFER, NULL);
  GST_DEBUG ("osx src 0x%p element 0x%p  ioproc 0x%p", osxsrc,
      GST_OSX_AUDIO_ELEMENT_GET_INTERFACE (osxsrc),
      (void *) gst_osx_audio_src_io_proc);

  ringbuffer->element = GST_OSX_AUDIO_ELEMENT_GET_INTERFACE (osxsrc);
  ringbuffer->is_src = TRUE;
  ringbuffer->device_id = osxsrc->device_id;

  return GST_RING_BUFFER (ringbuffer);
}

static OSStatus
gst_osx_audio_src_io_proc (GstOsxRingBuffer * buf,
    AudioUnitRenderActionFlags * ioActionFlags,
    const AudioTimeStamp * inTimeStamp,
    UInt32 inBusNumber, UInt32 inNumberFrames, AudioBufferList * bufferList)
{
  OSStatus status;
  guint8 *writeptr;
  gint writeseg;
  gint len;
  gint remaining;
  gint offset = 0;

  status = AudioUnitRender (buf->audiounit, ioActionFlags, inTimeStamp,
      inBusNumber, inNumberFrames, buf->recBufferList);

  if (status) {
    GST_WARNING_OBJECT (buf, "AudioUnitRender returned %d", (int) status);
    return status;
  }

  remaining = buf->recBufferList->mBuffers[0].mDataByteSize;

  while (remaining) {
    if (!gst_ring_buffer_prepare_read (GST_RING_BUFFER (buf),
            &writeseg, &writeptr, &len))
      return 0;

    len -= buf->segoffset;

    if (len > remaining)
      len = remaining;

    memcpy (writeptr + buf->segoffset,
        (char *) buf->recBufferList->mBuffers[0].mData + offset, len);

    buf->segoffset += len;
    offset += len;
    remaining -= len;

    if ((gint) buf->segoffset == GST_RING_BUFFER (buf)->spec.segsize) {
      /* we wrote one segment */
      gst_ring_buffer_advance (GST_RING_BUFFER (buf), 1);

      buf->segoffset = 0;
    }
  }
  return 0;
}

static void
gst_osx_audio_src_osxelement_init (gpointer g_iface, gpointer iface_data)
{
  GstOsxAudioElementInterface *iface = (GstOsxAudioElementInterface *) g_iface;

  iface->io_proc = (AURenderCallback) gst_osx_audio_src_io_proc;
}

static void
gst_osx_audio_src_select_device (GstOsxAudioSrc * osxsrc)
{
  OSStatus status;
  UInt32 propertySize;

  if (osxsrc->device_id == kAudioDeviceUnknown) {
    /* If no specific device has been selected by the user, then pick the
     * default device */
    GST_DEBUG_OBJECT (osxsrc, "Selecting device for OSXAudioSrc");
    propertySize = sizeof (osxsrc->device_id);
    status = AudioHardwareGetProperty (kAudioHardwarePropertyDefaultInputDevice,
        &propertySize, &osxsrc->device_id);

    if (status) {
      GST_WARNING_OBJECT (osxsrc,
          "AudioHardwareGetProperty returned %d", (int) status);
    } else {
      GST_DEBUG_OBJECT (osxsrc, "AudioHardwareGetProperty returned 0");
    }

    if (osxsrc->device_id == kAudioDeviceUnknown) {
      GST_WARNING_OBJECT (osxsrc,
          "AudioHardwareGetProperty: device_id is kAudioDeviceUnknown");
    }

    GST_DEBUG_OBJECT (osxsrc, "AudioHardwareGetProperty: device_id is %lu",
        (long) osxsrc->device_id);
  }
}
