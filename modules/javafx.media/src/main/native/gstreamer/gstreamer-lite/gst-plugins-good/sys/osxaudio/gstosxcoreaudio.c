/*
 * GStreamer
 * Copyright (C) 2012-2013 Fluendo S.A. <support@fluendo.com>
 *   Authors: Josep Torra Vallès <josep@fluendo.com>
 *            Andoni Morales Alastruey <amorales@fluendo.com>
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
 */

#include "gstosxcoreaudio.h"
#include "gstosxcoreaudiocommon.h"
#include "gstosxaudiosrc.h"

GST_DEBUG_CATEGORY_STATIC (osx_audio_debug);
#define GST_CAT_DEFAULT osx_audio_debug

G_DEFINE_TYPE (GstCoreAudio, gst_core_audio, G_TYPE_OBJECT);

#ifdef HAVE_IOS
#include "gstosxcoreaudioremoteio.c"
#else
#include "gstosxcoreaudiohal.c"
#endif


static void
gst_core_audio_class_init (GstCoreAudioClass * klass)
{
}

static void
gst_core_audio_init (GstCoreAudio * core_audio)
{
  core_audio->is_passthrough = FALSE;
  core_audio->device_id = kAudioDeviceUnknown;
  core_audio->is_src = FALSE;
  core_audio->audiounit = NULL;
#ifndef HAVE_IOS
  core_audio->hog_pid = -1;
  core_audio->disabled_mixing = FALSE;
#endif
}

/**************************
 *       Public API       *
 *************************/

GstCoreAudio *
gst_core_audio_new (GstObject * osxbuf)
{
  GstCoreAudio *core_audio;

  core_audio = g_object_new (GST_TYPE_CORE_AUDIO, NULL);
  core_audio->osxbuf = osxbuf;
  return core_audio;
}

gboolean
gst_core_audio_close (GstCoreAudio * core_audio)
{
  AudioComponentInstanceDispose (core_audio->audiounit);
  core_audio->audiounit = NULL;
  return TRUE;
}

gboolean
gst_core_audio_open (GstCoreAudio * core_audio)
{

  if (!gst_core_audio_open_impl (core_audio))
    return FALSE;

  if (core_audio->is_src) {
    AudioStreamBasicDescription asbd_in;
    UInt32 propertySize;
    OSStatus status;

    GstOsxAudioSrc *src =
        GST_OSX_AUDIO_SRC (GST_OBJECT_PARENT (core_audio->osxbuf));

    propertySize = sizeof (asbd_in);
    status = AudioUnitGetProperty (core_audio->audiounit,
        kAudioUnitProperty_StreamFormat,
        kAudioUnitScope_Input, 1, &asbd_in, &propertySize);

    if (status) {
      AudioComponentInstanceDispose (core_audio->audiounit);
      core_audio->audiounit = NULL;
      GST_WARNING_OBJECT (core_audio,
          "Unable to obtain device properties: %d", (int) status);
      return FALSE;
    } else {
      src->deviceChannels = asbd_in.mChannelsPerFrame;
    }
  }

  return TRUE;
}

gboolean
gst_core_audio_start_processing (GstCoreAudio * core_audio)
{
  return gst_core_audio_start_processing_impl (core_audio);
}

gboolean
gst_core_audio_pause_processing (GstCoreAudio * core_audio)
{
  return gst_core_audio_pause_processing_impl (core_audio);
}

gboolean
gst_core_audio_stop_processing (GstCoreAudio * core_audio)
{
  return gst_core_audio_stop_processing_impl (core_audio);
}

gboolean
gst_core_audio_get_samples_and_latency (GstCoreAudio * core_audio,
    gdouble rate, guint * samples, gdouble * latency)
{
  return gst_core_audio_get_samples_and_latency_impl (core_audio, rate,
      samples, latency);
}

gboolean
gst_core_audio_initialize (GstCoreAudio * core_audio,
    AudioStreamBasicDescription format, GstCaps * caps, gboolean is_passthrough)
{
  guint32 frame_size;
  OSStatus status;

  GST_DEBUG_OBJECT (core_audio,
      "Initializing: passthrough:%d caps:%" GST_PTR_FORMAT, is_passthrough,
      caps);

  if (!gst_core_audio_initialize_impl (core_audio, format, caps,
          is_passthrough, &frame_size)) {
    goto error;
  }

  if (core_audio->is_src) {
    /* create AudioBufferList needed for recording */
    core_audio->recBufferList =
        buffer_list_alloc (format.mChannelsPerFrame,
        frame_size * format.mBytesPerFrame);
  }

  /* Initialize the AudioUnit */
  status = AudioUnitInitialize (core_audio->audiounit);
  if (status) {
    GST_ERROR_OBJECT (core_audio, "Failed to initialise AudioUnit: %d",
        (int) status);
    goto error;
  }
  return TRUE;

error:
  if (core_audio->is_src && core_audio->recBufferList) {
    buffer_list_free (core_audio->recBufferList);
    core_audio->recBufferList = NULL;
  }
  return FALSE;
}

void
gst_core_audio_unitialize (GstCoreAudio * core_audio)
{
  AudioUnitUninitialize (core_audio->audiounit);

  if (core_audio->recBufferList) {
    buffer_list_free (core_audio->recBufferList);
    core_audio->recBufferList = NULL;
  }
}

void
gst_core_audio_set_volume (GstCoreAudio * core_audio, gfloat volume)
{
  AudioUnitSetParameter (core_audio->audiounit, kHALOutputParam_Volume,
      kAudioUnitScope_Global, 0, (float) volume, 0);
}

gboolean
gst_core_audio_select_device (AudioDeviceID * device_id)
{
  return gst_core_audio_select_device_impl (device_id);
}

gboolean
gst_core_audio_select_source_device (AudioDeviceID * device_id)
{
  return gst_core_audio_select_source_device_impl (device_id);
}

void
gst_core_audio_init_debug (void)
{
  GST_DEBUG_CATEGORY_INIT (osx_audio_debug, "osxaudio", 0,
      "OSX Audio Elements");
}

gboolean
gst_core_audio_audio_device_is_spdif_avail (AudioDeviceID device_id)
{
  return gst_core_audio_audio_device_is_spdif_avail_impl (device_id);
}
