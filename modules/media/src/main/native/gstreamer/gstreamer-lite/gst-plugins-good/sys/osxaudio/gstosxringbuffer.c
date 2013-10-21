/*
 * GStreamer
 * Copyright (C) 2006 Zaheer Abbas Merali <zaheerabbas at merali dot org>
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

#include <CoreAudio/CoreAudio.h>
#include <CoreServices/CoreServices.h>
#include <gst/gst.h>
#include <gst/audio/multichannel.h>
#include "gstosxringbuffer.h"
#include "gstosxaudiosink.h"
#include "gstosxaudiosrc.h"

GST_DEBUG_CATEGORY_STATIC (osx_audio_debug);
#define GST_CAT_DEFAULT osx_audio_debug

static void gst_osx_ring_buffer_dispose (GObject * object);
static void gst_osx_ring_buffer_finalize (GObject * object);
static gboolean gst_osx_ring_buffer_open_device (GstRingBuffer * buf);
static gboolean gst_osx_ring_buffer_close_device (GstRingBuffer * buf);

static gboolean gst_osx_ring_buffer_acquire (GstRingBuffer * buf,
    GstRingBufferSpec * spec);
static gboolean gst_osx_ring_buffer_release (GstRingBuffer * buf);

static gboolean gst_osx_ring_buffer_start (GstRingBuffer * buf);
static gboolean gst_osx_ring_buffer_pause (GstRingBuffer * buf);
static gboolean gst_osx_ring_buffer_stop (GstRingBuffer * buf);
static guint gst_osx_ring_buffer_delay (GstRingBuffer * buf);
static GstRingBufferClass *ring_parent_class = NULL;

static OSStatus gst_osx_ring_buffer_render_notify (GstOsxRingBuffer * osxbuf,
    AudioUnitRenderActionFlags * ioActionFlags,
    const AudioTimeStamp * inTimeStamp, unsigned int inBusNumber,
    unsigned int inNumberFrames, AudioBufferList * ioData);

static AudioBufferList *buffer_list_alloc (int channels, int size);
static void buffer_list_free (AudioBufferList * list);

static void
gst_osx_ring_buffer_do_init (GType type)
{
  GST_DEBUG_CATEGORY_INIT (osx_audio_debug, "osxaudio", 0,
      "OSX Audio Elements");
}

GST_BOILERPLATE_FULL (GstOsxRingBuffer, gst_osx_ring_buffer, GstRingBuffer,
    GST_TYPE_RING_BUFFER, gst_osx_ring_buffer_do_init);

static void
gst_osx_ring_buffer_base_init (gpointer g_class)
{
  /* Nothing to do right now */
}

static void
gst_osx_ring_buffer_class_init (GstOsxRingBufferClass * klass)
{
  GObjectClass *gobject_class;
  GstObjectClass *gstobject_class;
  GstRingBufferClass *gstringbuffer_class;

  gobject_class = (GObjectClass *) klass;
  gstobject_class = (GstObjectClass *) klass;
  gstringbuffer_class = (GstRingBufferClass *) klass;

  ring_parent_class = g_type_class_peek_parent (klass);

  gobject_class->dispose = gst_osx_ring_buffer_dispose;
  gobject_class->finalize = gst_osx_ring_buffer_finalize;

  gstringbuffer_class->open_device =
      GST_DEBUG_FUNCPTR (gst_osx_ring_buffer_open_device);
  gstringbuffer_class->close_device =
      GST_DEBUG_FUNCPTR (gst_osx_ring_buffer_close_device);
  gstringbuffer_class->acquire =
      GST_DEBUG_FUNCPTR (gst_osx_ring_buffer_acquire);
  gstringbuffer_class->release =
      GST_DEBUG_FUNCPTR (gst_osx_ring_buffer_release);
  gstringbuffer_class->start = GST_DEBUG_FUNCPTR (gst_osx_ring_buffer_start);
  gstringbuffer_class->pause = GST_DEBUG_FUNCPTR (gst_osx_ring_buffer_pause);
  gstringbuffer_class->resume = GST_DEBUG_FUNCPTR (gst_osx_ring_buffer_start);
  gstringbuffer_class->stop = GST_DEBUG_FUNCPTR (gst_osx_ring_buffer_stop);

  gstringbuffer_class->delay = GST_DEBUG_FUNCPTR (gst_osx_ring_buffer_delay);

  GST_DEBUG ("osx ring buffer class init");
}

static void
gst_osx_ring_buffer_init (GstOsxRingBuffer * ringbuffer,
    GstOsxRingBufferClass * g_class)
{
  /* Nothing to do right now */
}

static void
gst_osx_ring_buffer_dispose (GObject * object)
{
  G_OBJECT_CLASS (ring_parent_class)->dispose (object);
}

static void
gst_osx_ring_buffer_finalize (GObject * object)
{
  G_OBJECT_CLASS (ring_parent_class)->finalize (object);
}

static AudioUnit
gst_osx_ring_buffer_create_audio_unit (GstOsxRingBuffer * osxbuf,
    gboolean input, AudioDeviceID device_id)
{
  ComponentDescription desc;
  Component comp;
  OSStatus status;
  AudioUnit unit;
  UInt32 enableIO;

  /* Create a HALOutput AudioUnit.
   * This is the lowest-level output API that is actually sensibly usable
   * (the lower level ones require that you do channel-remapping yourself,
   * and the CoreAudio channel mapping is sufficiently complex that doing
   * so would be very difficult)
   *
   * Note that for input we request an output unit even though we will do
   * input with it: http://developer.apple.com/technotes/tn2002/tn2091.html
   */
  desc.componentType = kAudioUnitType_Output;
  desc.componentSubType = kAudioUnitSubType_HALOutput;
  desc.componentManufacturer = kAudioUnitManufacturer_Apple;
  desc.componentFlags = 0;
  desc.componentFlagsMask = 0;

  comp = FindNextComponent (NULL, &desc);
  if (comp == NULL) {
    GST_WARNING_OBJECT (osxbuf, "Couldn't find HALOutput component");
    return NULL;
  }

  status = OpenAComponent (comp, &unit);

  if (status) {
    GST_WARNING_OBJECT (osxbuf, "Couldn't open HALOutput component");
    return NULL;
  }

  if (input) {
    /* enable input */
    enableIO = 1;
    status = AudioUnitSetProperty (unit, kAudioOutputUnitProperty_EnableIO, kAudioUnitScope_Input, 1,   /* 1 = input element */
        &enableIO, sizeof (enableIO));

    if (status) {
      CloseComponent (unit);
      GST_WARNING_OBJECT (osxbuf, "Failed to enable input: %lx",
          (gulong) status);
      return NULL;
    }

    /* disable output */
    enableIO = 0;
    status = AudioUnitSetProperty (unit, kAudioOutputUnitProperty_EnableIO, kAudioUnitScope_Output, 0,  /* 0 = output element */
        &enableIO, sizeof (enableIO));

    if (status) {
      CloseComponent (unit);
      GST_WARNING_OBJECT (osxbuf, "Failed to disable output: %lx",
          (gulong) status);
      return NULL;
    }
  }

  /* Specify which device we're using. */
  GST_DEBUG_OBJECT (osxbuf, "Setting device to %d", (int) device_id);
  status = AudioUnitSetProperty (unit, kAudioOutputUnitProperty_CurrentDevice, kAudioUnitScope_Global, 0,       /* N/A for global */
      &device_id, sizeof (AudioDeviceID));

  if (status) {
    CloseComponent (unit);
    GST_WARNING_OBJECT (osxbuf, "Failed to set device: %lx", (gulong) status);
    return NULL;
  }

  GST_DEBUG_OBJECT (osxbuf, "Create HALOutput AudioUnit: %p", unit);

  return unit;
}

static gboolean
gst_osx_ring_buffer_open_device (GstRingBuffer * buf)
{
  GstOsxRingBuffer *osxbuf;
  GstOsxAudioSink *sink;
  GstOsxAudioSrc *src;
  AudioStreamBasicDescription asbd_in;
  OSStatus status;
  UInt32 propertySize;

  osxbuf = GST_OSX_RING_BUFFER (buf);
  sink = NULL;
  src = NULL;

  osxbuf->audiounit = gst_osx_ring_buffer_create_audio_unit (osxbuf,
      osxbuf->is_src, osxbuf->device_id);

  if (osxbuf->is_src) {
    src = GST_OSX_AUDIO_SRC (GST_OBJECT_PARENT (buf));

    propertySize = sizeof (asbd_in);
    status = AudioUnitGetProperty (osxbuf->audiounit,
        kAudioUnitProperty_StreamFormat,
        kAudioUnitScope_Input, 1, &asbd_in, &propertySize);

    if (status) {
      CloseComponent (osxbuf->audiounit);
      osxbuf->audiounit = NULL;
      GST_WARNING_OBJECT (osxbuf, "Unable to obtain device properties: %lx",
          (gulong) status);
      return FALSE;
    }

    src->deviceChannels = asbd_in.mChannelsPerFrame;
  } else {
    sink = GST_OSX_AUDIO_SINK (GST_OBJECT_PARENT (buf));

    /* needed for the sink's volume control */
    sink->audiounit = osxbuf->audiounit;
#ifdef GSTREAMER_LITE
    // Set current volume after we open device
    if (sink->audiounit)
    {
        AudioUnitSetParameter (sink->audiounit, kHALOutputParam_Volume, kAudioUnitScope_Global, 0, (float) sink->volume, 0);
    }
#endif // GSTREAMER_LITE
  }

  return TRUE;
}

static gboolean
gst_osx_ring_buffer_close_device (GstRingBuffer * buf)
{
  GstOsxRingBuffer *osxbuf;
  osxbuf = GST_OSX_RING_BUFFER (buf);

  CloseComponent (osxbuf->audiounit);
  osxbuf->audiounit = NULL;

  return TRUE;
}

static AudioChannelLabel
gst_audio_channel_position_to_coreaudio_channel_label (GstAudioChannelPosition
    position, int channel)
{
  switch (position) {
    case GST_AUDIO_CHANNEL_POSITION_NONE:
      return kAudioChannelLabel_Discrete_0 | channel;
    case GST_AUDIO_CHANNEL_POSITION_FRONT_MONO:
      return kAudioChannelLabel_Mono;
    case GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT:
      return kAudioChannelLabel_Left;
    case GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT:
      return kAudioChannelLabel_Right;
    case GST_AUDIO_CHANNEL_POSITION_REAR_CENTER:
      return kAudioChannelLabel_CenterSurround;
    case GST_AUDIO_CHANNEL_POSITION_REAR_LEFT:
      return kAudioChannelLabel_LeftSurround;
    case GST_AUDIO_CHANNEL_POSITION_REAR_RIGHT:
      return kAudioChannelLabel_RightSurround;
    case GST_AUDIO_CHANNEL_POSITION_LFE:
      return kAudioChannelLabel_LFEScreen;
    case GST_AUDIO_CHANNEL_POSITION_FRONT_CENTER:
      return kAudioChannelLabel_Center;
    case GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT_OF_CENTER:
      return kAudioChannelLabel_Center; // ???
    case GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT_OF_CENTER:
      return kAudioChannelLabel_Center; // ???
    case GST_AUDIO_CHANNEL_POSITION_SIDE_LEFT:
      return kAudioChannelLabel_LeftSurroundDirect;
    case GST_AUDIO_CHANNEL_POSITION_SIDE_RIGHT:
      return kAudioChannelLabel_RightSurroundDirect;
    default:
      return kAudioChannelLabel_Unknown;
  }
}

static gboolean
gst_osx_ring_buffer_acquire (GstRingBuffer * buf, GstRingBufferSpec * spec)
{
  /* Configure the output stream and allocate ringbuffer memory */
  GstOsxRingBuffer *osxbuf;
  AudioStreamBasicDescription format;
  AudioChannelLayout *layout = NULL;
  OSStatus status;
  UInt32 propertySize;
  int layoutSize;
  int element;
  int i;
  AudioUnitScope scope;
  gboolean ret = FALSE;
  GstStructure *structure;
  GstAudioChannelPosition *positions;
  UInt32 frameSize;

  osxbuf = GST_OSX_RING_BUFFER (buf);

  /* Fill out the audio description we're going to be using */
  format.mFormatID = kAudioFormatLinearPCM;
  format.mSampleRate = (double) spec->rate;
  format.mChannelsPerFrame = spec->channels;
  format.mFormatFlags = kAudioFormatFlagsNativeFloatPacked;
  format.mBytesPerFrame = spec->channels * sizeof (float);
  format.mBitsPerChannel = sizeof (float) * 8;
  format.mBytesPerPacket = spec->channels * sizeof (float);
  format.mFramesPerPacket = 1;
  format.mReserved = 0;

  /* Describe channels */
  layoutSize = sizeof (AudioChannelLayout) +
      spec->channels * sizeof (AudioChannelDescription);
  layout = g_malloc (layoutSize);

  structure = gst_caps_get_structure (spec->caps, 0);
  positions = gst_audio_get_channel_positions (structure);

  layout->mChannelLayoutTag = kAudioChannelLayoutTag_UseChannelDescriptions;
  layout->mChannelBitmap = 0;   /* Not used */
  layout->mNumberChannelDescriptions = spec->channels;
  for (i = 0; i < spec->channels; i++) {
    if (positions) {
      layout->mChannelDescriptions[i].mChannelLabel =
          gst_audio_channel_position_to_coreaudio_channel_label (positions[i],
          i);
    } else {
      /* Discrete channel numbers are ORed into this */
      layout->mChannelDescriptions[i].mChannelLabel =
          kAudioChannelLabel_Discrete_0 | i;
    }

    /* Others unused */
    layout->mChannelDescriptions[i].mChannelFlags = 0;
    layout->mChannelDescriptions[i].mCoordinates[0] = 0.f;
    layout->mChannelDescriptions[i].mCoordinates[1] = 0.f;
    layout->mChannelDescriptions[i].mCoordinates[2] = 0.f;
  }

  if (positions) {
    g_free (positions);
    positions = NULL;
  }

  GST_LOG_OBJECT (osxbuf, "Format: %x, %f, %u, %x, %d, %d, %d, %d, %d",
      (unsigned int) format.mFormatID,
      format.mSampleRate,
      (unsigned int) format.mChannelsPerFrame,
      (unsigned int) format.mFormatFlags,
      (unsigned int) format.mBytesPerFrame,
      (unsigned int) format.mBitsPerChannel,
      (unsigned int) format.mBytesPerPacket,
      (unsigned int) format.mFramesPerPacket, (unsigned int) format.mReserved);

  GST_DEBUG_OBJECT (osxbuf, "Setting format for AudioUnit");

  scope = osxbuf->is_src ? kAudioUnitScope_Output : kAudioUnitScope_Input;
  element = osxbuf->is_src ? 1 : 0;

  propertySize = sizeof (format);
  status = AudioUnitSetProperty (osxbuf->audiounit,
      kAudioUnitProperty_StreamFormat, scope, element, &format, propertySize);

  if (status) {
    GST_WARNING_OBJECT (osxbuf, "Failed to set audio description: %lx",
        (gulong) status);
    goto done;
  }

  status = AudioUnitSetProperty (osxbuf->audiounit,
      kAudioUnitProperty_AudioChannelLayout,
      scope, element, layout, layoutSize);
  if (status) {
    GST_WARNING_OBJECT (osxbuf, "Failed to set output channel layout: %lx",
        (gulong) status);
    goto done;
  }

  spec->segsize =
      (spec->latency_time * spec->rate / G_USEC_PER_SEC) *
      spec->bytes_per_sample;
  spec->segtotal = spec->buffer_time / spec->latency_time;

  /* create AudioBufferList needed for recording */
  if (osxbuf->is_src) {
    propertySize = sizeof (frameSize);
    status = AudioUnitGetProperty (osxbuf->audiounit, kAudioDevicePropertyBufferFrameSize, kAudioUnitScope_Global, 0,   /* N/A for global */
        &frameSize, &propertySize);

    if (status) {
      GST_WARNING_OBJECT (osxbuf, "Failed to get frame size: %lx",
          (gulong) status);
      goto done;
    }

    osxbuf->recBufferList = buffer_list_alloc (format.mChannelsPerFrame,
        frameSize * format.mBytesPerFrame);
  }

  buf->data = gst_buffer_new_and_alloc (spec->segtotal * spec->segsize);
  memset (GST_BUFFER_DATA (buf->data), 0, GST_BUFFER_SIZE (buf->data));

  osxbuf->segoffset = 0;

  status = AudioUnitInitialize (osxbuf->audiounit);
  if (status) {
    gst_buffer_unref (buf->data);
    buf->data = NULL;

    if (osxbuf->recBufferList) {
      buffer_list_free (osxbuf->recBufferList);
      osxbuf->recBufferList = NULL;
    }

    GST_WARNING_OBJECT (osxbuf,
        "Failed to initialise AudioUnit: %d", (int) status);
    goto done;
  }

  GST_DEBUG_OBJECT (osxbuf, "osx ring buffer acquired");

  ret = TRUE;

done:
  g_free (layout);
  return ret;
}

static gboolean
gst_osx_ring_buffer_release (GstRingBuffer * buf)
{
  GstOsxRingBuffer *osxbuf;

  osxbuf = GST_OSX_RING_BUFFER (buf);

  AudioUnitUninitialize (osxbuf->audiounit);

  gst_buffer_unref (buf->data);
  buf->data = NULL;

  if (osxbuf->recBufferList) {
    buffer_list_free (osxbuf->recBufferList);
    osxbuf->recBufferList = NULL;
  }

  return TRUE;
}

static void
gst_osx_ring_buffer_remove_render_callback (GstOsxRingBuffer * osxbuf)
{
  AURenderCallbackStruct input;
  OSStatus status;

  /* Deactivate the render callback by calling SetRenderCallback with a NULL
   * inputProc.
   */
  input.inputProc = NULL;
  input.inputProcRefCon = NULL;

  status = AudioUnitSetProperty (osxbuf->audiounit, kAudioUnitProperty_SetRenderCallback, kAudioUnitScope_Global, 0,    /* N/A for global */
      &input, sizeof (input));

  if (status) {
    GST_WARNING_OBJECT (osxbuf, "Failed to remove render callback");
  }

  /* Remove the RenderNotify too */
  status = AudioUnitRemoveRenderNotify (osxbuf->audiounit,
      (AURenderCallback) gst_osx_ring_buffer_render_notify, osxbuf);

  if (status) {
    GST_WARNING_OBJECT (osxbuf, "Failed to remove render notify callback");
  }

  /* We're deactivated.. */
  osxbuf->io_proc_needs_deactivation = FALSE;
  osxbuf->io_proc_active = FALSE;
}

static OSStatus
gst_osx_ring_buffer_render_notify (GstOsxRingBuffer * osxbuf,
    AudioUnitRenderActionFlags * ioActionFlags,
    const AudioTimeStamp * inTimeStamp,
    unsigned int inBusNumber,
    unsigned int inNumberFrames, AudioBufferList * ioData)
{
  /* Before rendering a frame, we get the PreRender notification.
   * Here, we detach the RenderCallback if we've been paused.
   *
   * This is necessary (rather than just directly detaching it) to work
   * around some thread-safety issues in CoreAudio
   */
  if ((*ioActionFlags) & kAudioUnitRenderAction_PreRender) {
    if (osxbuf->io_proc_needs_deactivation) {
      gst_osx_ring_buffer_remove_render_callback (osxbuf);
    }
  }

  return noErr;
}

static gboolean
gst_osx_ring_buffer_start (GstRingBuffer * buf)
{
  OSStatus status;
  GstOsxRingBuffer *osxbuf;
  AURenderCallbackStruct input;
  AudioUnitPropertyID callback_type;

  osxbuf = GST_OSX_RING_BUFFER (buf);

  GST_DEBUG ("osx ring buffer start ioproc: 0x%p device_id %lu",
      osxbuf->element->io_proc, (gulong) osxbuf->device_id);
  if (!osxbuf->io_proc_active) {
    callback_type = osxbuf->is_src ?
        kAudioOutputUnitProperty_SetInputCallback :
        kAudioUnitProperty_SetRenderCallback;

    input.inputProc = (AURenderCallback) osxbuf->element->io_proc;
    input.inputProcRefCon = osxbuf;

    status = AudioUnitSetProperty (osxbuf->audiounit, callback_type, kAudioUnitScope_Global, 0, /* N/A for global */
        &input, sizeof (input));

    if (status) {
      GST_WARNING ("AudioUnitSetProperty returned %d", (int) status);
      return FALSE;
    }
    // ### does it make sense to do this notify stuff for input mode?
    status = AudioUnitAddRenderNotify (osxbuf->audiounit,
        (AURenderCallback) gst_osx_ring_buffer_render_notify, osxbuf);

    if (status) {
      GST_WARNING ("AudioUnitAddRenderNotify returned %d", (int) status);
      return FALSE;
    }

    osxbuf->io_proc_active = TRUE;
  }

  osxbuf->io_proc_needs_deactivation = FALSE;

  status = AudioOutputUnitStart (osxbuf->audiounit);
  if (status) {
    GST_WARNING ("AudioOutputUnitStart returned %d", (int) status);
    return FALSE;
  }
  return TRUE;
}

// ###
static gboolean
gst_osx_ring_buffer_pause (GstRingBuffer * buf)
{
  GstOsxRingBuffer *osxbuf = GST_OSX_RING_BUFFER (buf);

  GST_DEBUG ("osx ring buffer pause ioproc: 0x%p device_id %lu",
      osxbuf->element->io_proc, (gulong) osxbuf->device_id);
  if (osxbuf->io_proc_active) {
    /* CoreAudio isn't threadsafe enough to do this here; we must deactivate
     * the render callback elsewhere. See:
     *   http://lists.apple.com/archives/Coreaudio-api/2006/Mar/msg00010.html
     */
    osxbuf->io_proc_needs_deactivation = TRUE;
  }
  return TRUE;
}

// ###
static gboolean
gst_osx_ring_buffer_stop (GstRingBuffer * buf)
{
  OSErr status;
  GstOsxRingBuffer *osxbuf;

  osxbuf = GST_OSX_RING_BUFFER (buf);

  GST_DEBUG ("osx ring buffer stop ioproc: 0x%p device_id %lu",
      osxbuf->element->io_proc, (gulong) osxbuf->device_id);

  status = AudioOutputUnitStop (osxbuf->audiounit);
  if (status)
    GST_WARNING ("AudioOutputUnitStop returned %d", (int) status);

  // ###: why is it okay to directly remove from here but not from pause() ?
  if (osxbuf->io_proc_active) {
    gst_osx_ring_buffer_remove_render_callback (osxbuf);
  }
  return TRUE;
}

static guint
gst_osx_ring_buffer_delay (GstRingBuffer * buf)
{
  double latency;
  UInt32 size = sizeof (double);
  GstOsxRingBuffer *osxbuf;
  OSStatus status;
  guint samples;

  osxbuf = GST_OSX_RING_BUFFER (buf);

  status = AudioUnitGetProperty (osxbuf->audiounit, kAudioUnitProperty_Latency, kAudioUnitScope_Global, 0,      /* N/A for global */
      &latency, &size);

  if (status) {
    GST_WARNING_OBJECT (buf, "Failed to get latency: %d", (int) status);
    return 0;
  }

  samples = latency * GST_RING_BUFFER (buf)->spec.rate;
  GST_DEBUG_OBJECT (buf, "Got latency: %f seconds -> %d samples", latency,
      samples);
  return samples;
}

static AudioBufferList *
buffer_list_alloc (int channels, int size)
{
  AudioBufferList *list;
  int total_size;
  int n;

  total_size = sizeof (AudioBufferList) + 1 * sizeof (AudioBuffer);
  list = (AudioBufferList *) g_malloc (total_size);

  list->mNumberBuffers = 1;
  for (n = 0; n < (int) list->mNumberBuffers; ++n) {
    list->mBuffers[n].mNumberChannels = channels;
    list->mBuffers[n].mDataByteSize = size;
    list->mBuffers[n].mData = g_malloc (size);
  }

  return list;
}

static void
buffer_list_free (AudioBufferList * list)
{
  int n;

  for (n = 0; n < (int) list->mNumberBuffers; ++n) {
    if (list->mBuffers[n].mData)
      g_free (list->mBuffers[n].mData);
  }

  g_free (list);
}
