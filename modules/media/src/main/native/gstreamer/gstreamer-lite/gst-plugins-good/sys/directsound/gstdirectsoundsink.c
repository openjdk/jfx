/* GStreamer
* Copyright (C) 2005 Sebastien Moutte <sebastien@moutte.net>
* Copyright (C) 2007 Pioneers of the Inevitable <songbird@songbirdnest.com>
* Copyright (C) 2010 Fluendo S.A. <support@fluendo.com>
*
* gstdirectsoundsink.c:
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
*
* The development of this code was made possible due to the involvement
* of Pioneers of the Inevitable, the creators of the Songbird Music player
*
*/

/**
 * SECTION:element-directsoundsink
 *
 * This element lets you output sound using the DirectSound API.
 *
 * Note that you should almost always use generic audio conversion elements
 * like audioconvert and audioresample in front of an audiosink to make sure
 * your pipeline works under all circumstances (those conversion elements will
 * act in passthrough-mode if no conversion is necessary).
 *
 * <refsect2>
 * <title>Example pipelines</title>
 * |[
 * gst-launch -v audiotestsrc ! audioconvert ! volume volume=0.1 ! directsoundsink
 * ]| will output a sine wave (continuous beep sound) to your sound card (with
 * a very low volume as precaution).
 * |[
 * gst-launch -v filesrc location=music.ogg ! decodebin ! audioconvert ! audioresample ! directsoundsink
 * ]| will play an Ogg/Vorbis audio file and output it.
 * </refsect2>
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "gstdirectsoundsink.h"

#include <math.h>

#ifdef __CYGWIN__
#include <unistd.h>
#ifndef _swab
#define _swab swab
#endif
#endif

GST_DEBUG_CATEGORY_STATIC (directsoundsink_debug);
#define GST_CAT_DEFAULT directsoundsink_debug

static void gst_directsound_sink_finalise (GObject * object);

static void gst_directsound_sink_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec);
static void gst_directsound_sink_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec);

static GstCaps *gst_directsound_sink_getcaps (GstBaseSink * bsink);
static gboolean gst_directsound_sink_prepare (GstAudioSink * asink,
    GstRingBufferSpec * spec);
static gboolean gst_directsound_sink_unprepare (GstAudioSink * asink);

static gboolean gst_directsound_sink_open (GstAudioSink * asink);
static gboolean gst_directsound_sink_close (GstAudioSink * asink);
static guint gst_directsound_sink_write (GstAudioSink * asink, gpointer data,
    guint length);
static guint gst_directsound_sink_delay (GstAudioSink * asink);
static void gst_directsound_sink_reset (GstAudioSink * asink);
static GstCaps *gst_directsound_probe_supported_formats (GstDirectSoundSink *
    dsoundsink, const GstCaps * template_caps);

/* interfaces */
static void gst_directsound_sink_interfaces_init (GType type);
static void
gst_directsound_sink_implements_interface_init (GstImplementsInterfaceClass *
    iface);
static void gst_directsound_sink_mixer_interface_init (GstMixerClass * iface);

#ifndef GSTREAMER_LITE
static GstStaticPadTemplate directsoundsink_sink_factory =
    GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS ("audio/x-raw-int, "
        "signed = (boolean) { TRUE, FALSE }, "
        "width = (int) 16, "
        "depth = (int) 16, "
        "rate = (int) [ 1, MAX ], " "channels = (int) [ 1, 2 ]; "
        "audio/x-raw-int, "
        "signed = (boolean) { TRUE, FALSE }, "
        "width = (int) 8, "
        "depth = (int) 8, "
        "rate = (int) [ 1, MAX ], " "channels = (int) [ 1, 2 ];"
        "audio/x-iec958"));
#else // GSTREAMER_LITE
static GstStaticPadTemplate directsoundsink_sink_factory =
    GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS ("audio/x-raw-int, "
        "signed = (boolean) TRUE, "
        "width = (int) 16, "
        "depth = (int) 16, "
        "rate = (int) [ 1, MAX ], "
				"endianness = (int) LITTLE_ENDIAN, "
				"channels = (int) [ 1, 2 ]; "
        "audio/x-raw-int, "
        "signed = (boolean) FALSE, "
        "width = (int) 8, "
        "depth = (int) 8, "
        "rate = (int) [ 1, MAX ], "
				"endianness = (int) LITTLE_ENDIAN, "
				"channels = (int) [ 1, 2 ]"));
#endif // GSTREAMER_LITE

enum
{
  PROP_0,
  PROP_VOLUME
#ifdef GSTREAMER_LITE
  ,PROP_PANORAMA
#endif // GSTREAMER_LITE
};

GST_BOILERPLATE_FULL (GstDirectSoundSink, gst_directsound_sink, GstAudioSink,
    GST_TYPE_AUDIO_SINK, gst_directsound_sink_interfaces_init);

/* interfaces stuff */
static void
gst_directsound_sink_interfaces_init (GType type)
{
  static const GInterfaceInfo implements_interface_info = {
    (GInterfaceInitFunc) gst_directsound_sink_implements_interface_init,
    NULL,
    NULL,
  };

  static const GInterfaceInfo mixer_interface_info = {
    (GInterfaceInitFunc) gst_directsound_sink_mixer_interface_init,
    NULL,
    NULL,
  };

  g_type_add_interface_static (type,
      GST_TYPE_IMPLEMENTS_INTERFACE, &implements_interface_info);
  g_type_add_interface_static (type, GST_TYPE_MIXER, &mixer_interface_info);
}

static gboolean
gst_directsound_sink_interface_supported (GstImplementsInterface * iface,
    GType iface_type)
{
  g_return_val_if_fail (iface_type == GST_TYPE_MIXER, FALSE);

  /* for the sake of this example, we'll always support it. However, normally,
   * you would check whether the device you've opened supports mixers. */
  return TRUE;
}

static void
gst_directsound_sink_implements_interface_init (GstImplementsInterfaceClass *
    iface)
{
  iface->supported = gst_directsound_sink_interface_supported;
}

/*
 * This function returns the list of support tracks (inputs, outputs)
 * on this element instance. Elements usually build this list during
 * _init () or when going from NULL to READY.
 */

static const GList *
gst_directsound_sink_mixer_list_tracks (GstMixer * mixer)
{
  GstDirectSoundSink *dsoundsink = GST_DIRECTSOUND_SINK (mixer);

  return dsoundsink->tracks;
}

static void
gst_directsound_sink_set_volume (GstDirectSoundSink * dsoundsink)
{
  if (dsoundsink->pDSBSecondary) {
    /* DirectSound controls volume using units of 100th of a decibel,
     * ranging from -10000 to 0. We use a linear scale of 0 - 100
     * here, so remap.
     */
    long dsVolume;
    if (dsoundsink->volume == 0)
      dsVolume = -10000;
    else
      dsVolume = 100 * (long) (20 * log10 ((double) dsoundsink->volume / 100.));
    dsVolume = CLAMP (dsVolume, -10000, 0);

    GST_DEBUG_OBJECT (dsoundsink,
        "Setting volume on secondary buffer to %d from %d", (int) dsVolume,
        (int) dsoundsink->volume);
    IDirectSoundBuffer_SetVolume (dsoundsink->pDSBSecondary, dsVolume);
  }
}

#ifdef GSTREAMER_LITE
static void
gst_directsound_sink_set_pan (GstDirectSoundSink * dsoundsink)
{
  if (dsoundsink->pDSBSecondary)
  {
    LONG lPan = DSBPAN_CENTER;
    double panorama = 0.0;
    BOOL bLeftChannel = FALSE;

    panorama = dsoundsink->panorama;
    
    /* DirectSound controls pan using units of 100th of a decibel,
     * ranging from -10000 (DSBPAN_LEFT) to 10000 (DSBPAN_RIGHT). We use a linear scale of -1.00 - 1.00
     * here, so remap.
     */
    if (panorama < 0.0)
    {
      bLeftChannel = TRUE;
      panorama = panorama * (-1.0);
    }
      
    if (dsoundsink->panorama == 0.0)
      lPan = DSBPAN_CENTER;
    else if (dsoundsink->panorama == 1.0)
      lPan = DSBPAN_RIGHT;
    else if (dsoundsink->panorama == -1.0)
      lPan = DSBPAN_LEFT;
    else
    {
      lPan = 100 * (long) (20.0 * log10 ((double) 1.0 - panorama));

      if (!bLeftChannel)
        lPan = lPan * (-1);
    
      lPan = CLAMP(lPan, DSBPAN_LEFT, DSBPAN_RIGHT);
    }

    IDirectSoundBuffer_SetPan(dsoundsink->pDSBSecondary, lPan); 
  }
}
#endif // GSTREAMER_LITE

/*
 * Set volume. volumes is an array of size track->num_channels, and
 * each value in the array gives the wanted volume for one channel
 * on the track.
 */

static void
gst_directsound_sink_mixer_set_volume (GstMixer * mixer,
    GstMixerTrack * track, gint * volumes)
{
  GstDirectSoundSink *dsoundsink = GST_DIRECTSOUND_SINK (mixer);

  if (volumes[0] != dsoundsink->volume) {
    dsoundsink->volume = volumes[0];

    gst_directsound_sink_set_volume (dsoundsink);
  }
}

static void
gst_directsound_sink_mixer_get_volume (GstMixer * mixer,
    GstMixerTrack * track, gint * volumes)
{
  GstDirectSoundSink *dsoundsink = GST_DIRECTSOUND_SINK (mixer);

  volumes[0] = dsoundsink->volume;
}

static void
gst_directsound_sink_mixer_interface_init (GstMixerClass * iface)
{
  /* the mixer interface requires a definition of the mixer type:
   * hardware or software? */
  GST_MIXER_TYPE (iface) = GST_MIXER_SOFTWARE;

  /* virtual function pointers */
  iface->list_tracks = gst_directsound_sink_mixer_list_tracks;
  iface->set_volume = gst_directsound_sink_mixer_set_volume;
  iface->get_volume = gst_directsound_sink_mixer_get_volume;
}

static void
gst_directsound_sink_finalise (GObject * object)
{
  GstDirectSoundSink *dsoundsink = GST_DIRECTSOUND_SINK (object);

  g_mutex_free (dsoundsink->dsound_lock);

  if (dsoundsink->tracks) {
    g_list_foreach (dsoundsink->tracks, (GFunc) g_object_unref, NULL);
    g_list_free (dsoundsink->tracks);
    dsoundsink->tracks = NULL;
  }

  G_OBJECT_CLASS (parent_class)->finalize (object);
}

static void
gst_directsound_sink_base_init (gpointer g_class)
{
  GstElementClass *element_class = GST_ELEMENT_CLASS (g_class);

  gst_element_class_set_details_simple (element_class,
      "Direct Sound Audio Sink", "Sink/Audio",
      "Output to a sound card via Direct Sound",
      "Sebastien Moutte <sebastien@moutte.net>");
  gst_element_class_add_pad_template (element_class,
      gst_static_pad_template_get (&directsoundsink_sink_factory));
}

static void
gst_directsound_sink_class_init (GstDirectSoundSinkClass * klass)
{
  GObjectClass *gobject_class;
  GstElementClass *gstelement_class;
  GstBaseSinkClass *gstbasesink_class;
  GstBaseAudioSinkClass *gstbaseaudiosink_class;
  GstAudioSinkClass *gstaudiosink_class;

  gobject_class = (GObjectClass *) klass;
  gstelement_class = (GstElementClass *) klass;
  gstbasesink_class = (GstBaseSinkClass *) klass;
  gstbaseaudiosink_class = (GstBaseAudioSinkClass *) klass;
  gstaudiosink_class = (GstAudioSinkClass *) klass;

  GST_DEBUG_CATEGORY_INIT (directsoundsink_debug, "directsoundsink", 0,
      "DirectSound sink");

  parent_class = g_type_class_peek_parent (klass);

  gobject_class->finalize = gst_directsound_sink_finalise;
  gobject_class->set_property = gst_directsound_sink_set_property;
  gobject_class->get_property = gst_directsound_sink_get_property;

  gstbasesink_class->get_caps =
      GST_DEBUG_FUNCPTR (gst_directsound_sink_getcaps);

  gstaudiosink_class->prepare =
      GST_DEBUG_FUNCPTR (gst_directsound_sink_prepare);
  gstaudiosink_class->unprepare =
      GST_DEBUG_FUNCPTR (gst_directsound_sink_unprepare);
  gstaudiosink_class->open = GST_DEBUG_FUNCPTR (gst_directsound_sink_open);
  gstaudiosink_class->close = GST_DEBUG_FUNCPTR (gst_directsound_sink_close);
  gstaudiosink_class->write = GST_DEBUG_FUNCPTR (gst_directsound_sink_write);
  gstaudiosink_class->delay = GST_DEBUG_FUNCPTR (gst_directsound_sink_delay);
  gstaudiosink_class->reset = GST_DEBUG_FUNCPTR (gst_directsound_sink_reset);

  g_object_class_install_property (gobject_class,
      PROP_VOLUME,
      g_param_spec_double ("volume", "Volume",
          "Volume of this stream", 0.0, 1.0, 1.0,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

#ifdef GSTREAMER_LITE
  g_object_class_install_property (gobject_class,
      PROP_PANORAMA,
      g_param_spec_float ("panorama", "Panorama",
          "Position in stereo panorama (-1.00 left -> 1.00 right)", -1.0, 1.0,
          0.0, G_PARAM_READWRITE | GST_PARAM_CONTROLLABLE));
#endif // GSTREAMER_LITE
}

static void
gst_directsound_sink_init (GstDirectSoundSink * dsoundsink,
    GstDirectSoundSinkClass * g_class)
{
  GstMixerTrack *track = NULL;

  dsoundsink->tracks = NULL;
  track = g_object_new (GST_TYPE_MIXER_TRACK, NULL);
  track->label = g_strdup ("DSoundTrack");
  track->num_channels = 2;
  track->min_volume = 0;
  track->max_volume = 100;
  track->flags = GST_MIXER_TRACK_OUTPUT;
  dsoundsink->tracks = g_list_append (dsoundsink->tracks, track);

  dsoundsink->pDS = NULL;
  dsoundsink->cached_caps = NULL;
  dsoundsink->pDSBSecondary = NULL;
  dsoundsink->current_circular_offset = 0;
  dsoundsink->buffer_size = DSBSIZE_MIN;
  dsoundsink->volume = 100;
  dsoundsink->dsound_lock = g_mutex_new ();
  dsoundsink->first_buffer_after_reset = FALSE;
#ifdef GSTREAMER_LITE
  dsoundsink->panorama = 0.0;
#endif // GSTREAMER_LITE
}

static void
gst_directsound_sink_set_property (GObject * object,
    guint prop_id, const GValue * value, GParamSpec * pspec)
{
  GstDirectSoundSink *sink = GST_DIRECTSOUND_SINK (object);

  switch (prop_id) {
    case PROP_VOLUME:
      sink->volume = (int) (g_value_get_double (value) * 100);
      gst_directsound_sink_set_volume (sink);
      break;
#ifdef GSTREAMER_LITE
    case PROP_PANORAMA:
      sink->panorama = g_value_get_float (value);
      gst_directsound_sink_set_pan (sink);
      break;
#endif // GSTREAMER_LITE
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_directsound_sink_get_property (GObject * object,
    guint prop_id, GValue * value, GParamSpec * pspec)
{
  GstDirectSoundSink *sink = GST_DIRECTSOUND_SINK (object);

  switch (prop_id) {
    case PROP_VOLUME:
      g_value_set_double (value, (double) sink->volume / 100.);
      break;
#ifdef GSTREAMER_LITE
    case PROP_PANORAMA:
      g_value_set_float (value, sink->panorama);
      break;
#endif // GSTREAMER_LITE
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static GstCaps *
gst_directsound_sink_getcaps (GstBaseSink * bsink)
{
  GstElementClass *element_class;
  GstPadTemplate *pad_template;
  GstDirectSoundSink *dsoundsink = GST_DIRECTSOUND_SINK (bsink);
  GstCaps *caps;
  gchar *caps_string = NULL;

  if (dsoundsink->pDS == NULL) {
    GST_DEBUG_OBJECT (dsoundsink, "device not open, using template caps");
    return NULL;                /* base class will get template caps for us */
  }

  if (dsoundsink->cached_caps) {
    caps_string = gst_caps_to_string (dsoundsink->cached_caps);
    GST_DEBUG_OBJECT (dsoundsink, "Returning cached caps: %s", caps_string);
    g_free (caps_string);
    return gst_caps_ref (dsoundsink->cached_caps);
  }

  element_class = GST_ELEMENT_GET_CLASS (dsoundsink);
  pad_template = gst_element_class_get_pad_template (element_class, "sink");
  g_return_val_if_fail (pad_template != NULL, NULL);

  caps = gst_directsound_probe_supported_formats (dsoundsink,
      gst_pad_template_get_caps (pad_template));
  if (caps) {
    dsoundsink->cached_caps = gst_caps_ref (caps);
  }

  if (caps) {
    gchar *caps_string = gst_caps_to_string (caps);
    GST_DEBUG_OBJECT (dsoundsink, "returning caps %s", caps_string);
    g_free (caps_string);
  }

  return caps;
}

static gboolean
gst_directsound_sink_open (GstAudioSink * asink)
{
  GstDirectSoundSink *dsoundsink = GST_DIRECTSOUND_SINK (asink);
  HRESULT hRes;

  /* create and initialize a DirecSound object */
  if (FAILED (hRes = DirectSoundCreate (NULL, &dsoundsink->pDS, NULL))) {
#ifndef GSTREAMER_LITE
    GST_ELEMENT_ERROR (dsoundsink, RESOURCE, OPEN_READ,
        ("gst_directsound_sink_open: DirectSoundCreate: %s",
            DXGetErrorString9 (hRes)), (NULL));
    return FALSE;
#else // GSTREAMER_LITE
    dsoundsink->pDS = NULL;
    return TRUE;
#endif // GSTREAMER_LITE
  }

  if (FAILED (hRes = IDirectSound_SetCooperativeLevel (dsoundsink->pDS,
              GetDesktopWindow (), DSSCL_PRIORITY))) {
#ifndef GSTREAMER_LITE
    GST_ELEMENT_ERROR (dsoundsink, RESOURCE, OPEN_READ,
        ("gst_directsound_sink_open: IDirectSound_SetCooperativeLevel: %s",
            DXGetErrorString9 (hRes)), (NULL));
    return FALSE;
#else // GSTREAMER_LITE
    if (dsoundsink->pDS)
    {
      IDirectSound_Release(dsoundsink->pDS);
      dsoundsink->pDS = NULL;
    }
    return TRUE;
#endif // GSTREAMER_LITE    
  }

  return TRUE;
}

static gboolean
gst_directsound_sink_prepare (GstAudioSink * asink, GstRingBufferSpec * spec)
{
  GstDirectSoundSink *dsoundsink = GST_DIRECTSOUND_SINK (asink);
  HRESULT hRes;
  DSBUFFERDESC descSecondary;
  WAVEFORMATEX wfx;

  /*save number of bytes per sample and buffer format */
  dsoundsink->bytes_per_sample = spec->bytes_per_sample;
  dsoundsink->buffer_format = spec->format;

#ifdef GSTREAMER_LITE
  dsoundsink->rate = spec->rate;
  if (dsoundsink->bytes_per_sample == 0 || dsoundsink->rate == 0)
      return FALSE;
  if (dsoundsink->pDS == NULL)
    return TRUE;
#endif // GSTREAMER_LITE

  /* fill the WAVEFORMATEX structure with spec params */
  memset (&wfx, 0, sizeof (wfx));
  if (spec->format != GST_IEC958) {
    wfx.cbSize = sizeof (wfx);
    wfx.wFormatTag = WAVE_FORMAT_PCM;
    wfx.nChannels = spec->channels;
    wfx.nSamplesPerSec = spec->rate;
    wfx.wBitsPerSample = (spec->bytes_per_sample * 8) / wfx.nChannels;
    wfx.nBlockAlign = spec->bytes_per_sample;
    wfx.nAvgBytesPerSec = wfx.nSamplesPerSec * wfx.nBlockAlign;

    /* Create directsound buffer with size based on our configured  
     * buffer_size (which is 200 ms by default) */
    dsoundsink->buffer_size =
        gst_util_uint64_scale_int (wfx.nAvgBytesPerSec, spec->buffer_time,
        GST_MSECOND);
    /* Make sure we make those numbers multiple of our sample size in bytes */
    dsoundsink->buffer_size += dsoundsink->buffer_size % spec->bytes_per_sample;

    spec->segsize =
        gst_util_uint64_scale_int (wfx.nAvgBytesPerSec, spec->latency_time,
        GST_MSECOND);
    spec->segsize += spec->segsize % spec->bytes_per_sample;
    spec->segtotal = dsoundsink->buffer_size / spec->segsize;
  } else {
#ifdef WAVE_FORMAT_DOLBY_AC3_SPDIF
    wfx.cbSize = 0;
    wfx.wFormatTag = WAVE_FORMAT_DOLBY_AC3_SPDIF;
    wfx.nChannels = 2;
    wfx.nSamplesPerSec = spec->rate;
    wfx.wBitsPerSample = 16;
    wfx.nBlockAlign = wfx.wBitsPerSample / 8 * wfx.nChannels;
    wfx.nAvgBytesPerSec = wfx.nSamplesPerSec * wfx.nBlockAlign;

    spec->segsize = 6144;
    spec->segtotal = 10;
#else
    g_assert_not_reached ();
#endif
  }

  // Make the final buffer size be an integer number of segments
  dsoundsink->buffer_size = spec->segsize * spec->segtotal;

  GST_INFO_OBJECT (dsoundsink,
      "GstRingBufferSpec->channels: %d, GstRingBufferSpec->rate: %d, GstRingBufferSpec->bytes_per_sample: %d\n"
      "WAVEFORMATEX.nSamplesPerSec: %ld, WAVEFORMATEX.wBitsPerSample: %d, WAVEFORMATEX.nBlockAlign: %d, WAVEFORMATEX.nAvgBytesPerSec: %ld\n"
      "Size of dsound circular buffer=>%d\n", spec->channels, spec->rate,
      spec->bytes_per_sample, wfx.nSamplesPerSec, wfx.wBitsPerSample,
      wfx.nBlockAlign, wfx.nAvgBytesPerSec, dsoundsink->buffer_size);

  /* create a secondary directsound buffer */
  memset (&descSecondary, 0, sizeof (DSBUFFERDESC));
  descSecondary.dwSize = sizeof (DSBUFFERDESC);
#ifndef GSTREAMER_LITE
  descSecondary.dwFlags = DSBCAPS_GETCURRENTPOSITION2 | DSBCAPS_GLOBALFOCUS;
#else // GSTREAMER_LITE
  descSecondary.dwFlags = DSBCAPS_GETCURRENTPOSITION2 | DSBCAPS_GLOBALFOCUS | DSBCAPS_CTRLPAN;
#endif // GSTREAMER_LITE
  if (spec->format != GST_IEC958)
    descSecondary.dwFlags |= DSBCAPS_CTRLVOLUME;

  descSecondary.dwBufferBytes = dsoundsink->buffer_size;
  descSecondary.lpwfxFormat = (WAVEFORMATEX *) & wfx;

  hRes = IDirectSound_CreateSoundBuffer (dsoundsink->pDS, &descSecondary,
      &dsoundsink->pDSBSecondary, NULL);
  if (FAILED (hRes)) {
#ifndef GSTREAMER_LITE
    GST_ELEMENT_ERROR (dsoundsink, RESOURCE, OPEN_READ,
        ("gst_directsound_sink_prepare: IDirectSound_CreateSoundBuffer: %s",
            DXGetErrorString9 (hRes)), (NULL));
    return FALSE;
#else // GSTREAMER_LITE
    if (dsoundsink->pDS)
    {
      IDirectSound_Release(dsoundsink->pDS);
      dsoundsink->pDS = NULL;
    }
    dsoundsink->pDSBSecondary = NULL;
    return TRUE;
#endif // GSTREAMER_LITE    
  }

  gst_directsound_sink_set_volume (dsoundsink);
#ifdef GSTREAMER_LITE
  gst_directsound_sink_set_pan (dsoundsink);
#endif // GSTREAMER_LITE

  return TRUE;
}

static gboolean
gst_directsound_sink_unprepare (GstAudioSink * asink)
{
  GstDirectSoundSink *dsoundsink;

  dsoundsink = GST_DIRECTSOUND_SINK (asink);

  /* release secondary DirectSound buffer */
  if (dsoundsink->pDSBSecondary) {
    IDirectSoundBuffer_Release (dsoundsink->pDSBSecondary);
    dsoundsink->pDSBSecondary = NULL;
  }

  return TRUE;
}

static gboolean
gst_directsound_sink_close (GstAudioSink * asink)
{
  GstDirectSoundSink *dsoundsink = NULL;

  dsoundsink = GST_DIRECTSOUND_SINK (asink);

  /* release DirectSound object */
#ifndef GSTREAMER_LITE
  g_return_val_if_fail (dsoundsink->pDS != NULL, FALSE);
#else // GSTREAMER_LITE
  if (dsoundsink->pDS)
#endif // GSTREAMER_LITE
  IDirectSound_Release (dsoundsink->pDS);
  dsoundsink->pDS = NULL;

  gst_caps_replace (&dsoundsink->cached_caps, NULL);

  return TRUE;
}

static guint
gst_directsound_sink_write (GstAudioSink * asink, gpointer data, guint length)
{
  GstDirectSoundSink *dsoundsink;
  DWORD dwStatus;
  HRESULT hRes;
  LPVOID pLockedBuffer1 = NULL, pLockedBuffer2 = NULL;
  DWORD dwSizeBuffer1, dwSizeBuffer2;
  DWORD dwCurrentPlayCursor;
#ifdef GSTREAMER_LITE
  DWORD samples = 0;
  DWORD duration = 0;
#endif // GSTREAMER_LITE

  dsoundsink = GST_DIRECTSOUND_SINK (asink);

#ifdef GSTREAMER_LITE
  if (dsoundsink->pDS == NULL)
  {
    GST_DSOUND_LOCK (dsoundsink);
    samples = length/dsoundsink->bytes_per_sample;
    duration = (1000*samples)/dsoundsink->rate;
    Sleep(duration);
    GST_DSOUND_UNLOCK (dsoundsink);
    return length;
  }
#endif // GSTREAMER_LITE

  /* Fix endianness */
  if (dsoundsink->buffer_format == GST_IEC958)
    _swab (data, data, length);

  GST_DSOUND_LOCK (dsoundsink);

  /* get current buffer status */
  hRes = IDirectSoundBuffer_GetStatus (dsoundsink->pDSBSecondary, &dwStatus);

  /* get current play cursor position */
  hRes = IDirectSoundBuffer_GetCurrentPosition (dsoundsink->pDSBSecondary,
      &dwCurrentPlayCursor, NULL);

  if (SUCCEEDED (hRes) && (dwStatus & DSBSTATUS_PLAYING)) {
    DWORD dwFreeBufferSize;

  calculate_freesize:
    /* calculate the free size of the circular buffer */
    if (dwCurrentPlayCursor < dsoundsink->current_circular_offset)
      dwFreeBufferSize =
          dsoundsink->buffer_size - (dsoundsink->current_circular_offset -
          dwCurrentPlayCursor);
    else
      dwFreeBufferSize =
          dwCurrentPlayCursor - dsoundsink->current_circular_offset;

    if (length >= dwFreeBufferSize) {
      Sleep (100);
      hRes = IDirectSoundBuffer_GetCurrentPosition (dsoundsink->pDSBSecondary,
          &dwCurrentPlayCursor, NULL);

      hRes =
          IDirectSoundBuffer_GetStatus (dsoundsink->pDSBSecondary, &dwStatus);
      if (SUCCEEDED (hRes) && (dwStatus & DSBSTATUS_PLAYING))
        goto calculate_freesize;
      else {
        dsoundsink->first_buffer_after_reset = FALSE;
        GST_DSOUND_UNLOCK (dsoundsink);
        return 0;
      }
    }
  }

  if (dwStatus & DSBSTATUS_BUFFERLOST) {
    hRes = IDirectSoundBuffer_Restore (dsoundsink->pDSBSecondary);      /*need a loop waiting the buffer is restored?? */

    dsoundsink->current_circular_offset = 0;
  }

  hRes = IDirectSoundBuffer_Lock (dsoundsink->pDSBSecondary,
      dsoundsink->current_circular_offset, length, &pLockedBuffer1,
      &dwSizeBuffer1, &pLockedBuffer2, &dwSizeBuffer2, 0L);

  if (SUCCEEDED (hRes)) {
    // Write to pointers without reordering.
    memcpy (pLockedBuffer1, data, dwSizeBuffer1);
    if (pLockedBuffer2 != NULL)
      memcpy (pLockedBuffer2, (LPBYTE) data + dwSizeBuffer1, dwSizeBuffer2);

    // Update where the buffer will lock (for next time)
    dsoundsink->current_circular_offset += dwSizeBuffer1 + dwSizeBuffer2;
    dsoundsink->current_circular_offset %= dsoundsink->buffer_size;     /* Circular buffer */

    hRes = IDirectSoundBuffer_Unlock (dsoundsink->pDSBSecondary, pLockedBuffer1,
        dwSizeBuffer1, pLockedBuffer2, dwSizeBuffer2);
  }

  /* if the buffer was not in playing state yet, call play on the buffer 
     except if this buffer is the fist after a reset (base class call reset and write a buffer when setting the sink to pause) */
  if (!(dwStatus & DSBSTATUS_PLAYING) &&
      dsoundsink->first_buffer_after_reset == FALSE) {
    hRes = IDirectSoundBuffer_Play (dsoundsink->pDSBSecondary, 0, 0,
        DSBPLAY_LOOPING);
  }

  dsoundsink->first_buffer_after_reset = FALSE;

  GST_DSOUND_UNLOCK (dsoundsink);

  return length;
}

static guint
gst_directsound_sink_delay (GstAudioSink * asink)
{
  GstDirectSoundSink *dsoundsink;
  HRESULT hRes;
  DWORD dwCurrentPlayCursor;
  DWORD dwBytesInQueue = 0;
  gint nNbSamplesInQueue = 0;
  DWORD dwStatus;

  dsoundsink = GST_DIRECTSOUND_SINK (asink);

#ifdef GSTREAMER_LITE
  if (dsoundsink->pDS == NULL)
    return nNbSamplesInQueue;
#endif // GSTREAMER_LITE

  /* get current buffer status */
  hRes = IDirectSoundBuffer_GetStatus (dsoundsink->pDSBSecondary, &dwStatus);

  if (dwStatus & DSBSTATUS_PLAYING) {
    /*evaluate the number of samples in queue in the circular buffer */
    hRes = IDirectSoundBuffer_GetCurrentPosition (dsoundsink->pDSBSecondary,
        &dwCurrentPlayCursor, NULL);

    if (hRes == S_OK) {
      if (dwCurrentPlayCursor < dsoundsink->current_circular_offset)
        dwBytesInQueue =
            dsoundsink->current_circular_offset - dwCurrentPlayCursor;
      else
        dwBytesInQueue =
            dsoundsink->current_circular_offset + (dsoundsink->buffer_size -
            dwCurrentPlayCursor);

      nNbSamplesInQueue = dwBytesInQueue / dsoundsink->bytes_per_sample;
    }
  }

  return nNbSamplesInQueue;
}

#ifndef GSTREAMER_LITE
static void
gst_directsound_sink_reset (GstAudioSink * asink)
{
  GstDirectSoundSink *dsoundsink;
  LPVOID pLockedBuffer = NULL;
  DWORD dwSizeBuffer = 0;

  dsoundsink = GST_DIRECTSOUND_SINK (asink);

  GST_DSOUND_LOCK (dsoundsink);

  if (dsoundsink->pDSBSecondary) {
    /*stop playing */
    HRESULT hRes = IDirectSoundBuffer_Stop (dsoundsink->pDSBSecondary);

    /*reset position */
    hRes = IDirectSoundBuffer_SetCurrentPosition (dsoundsink->pDSBSecondary, 0);
    dsoundsink->current_circular_offset = 0;

    /*reset the buffer */
    hRes = IDirectSoundBuffer_Lock (dsoundsink->pDSBSecondary,
        dsoundsink->current_circular_offset, dsoundsink->buffer_size,
        &pLockedBuffer, &dwSizeBuffer, NULL, NULL, 0L);

    if (SUCCEEDED (hRes)) {
      memset (pLockedBuffer, 0, dwSizeBuffer);

      hRes =
          IDirectSoundBuffer_Unlock (dsoundsink->pDSBSecondary, pLockedBuffer,
          dwSizeBuffer, NULL, 0);
    }
  }

  dsoundsink->first_buffer_after_reset = TRUE;

  GST_DSOUND_UNLOCK (dsoundsink);
}
#else // GSTREAMER_LITE
static void
gst_directsound_sink_reset (GstAudioSink * asink)
{
  GstDirectSoundSink *dsoundsink;
  LPVOID pLockedBuffer1 = NULL, pLockedBuffer2 = NULL;
  DWORD dwSizeBuffer1 = 0, dwSizeBuffer2 = 0;
  DWORD dwInitialPlayCursor = 0, dwCurrentPlayCursor = 0, dwInitialWriteCursor = 0, dwCurrentWriteCursor = 0;
  DWORD buff_size = 0;
  HRESULT hRes = S_OK;

  dsoundsink = GST_DIRECTSOUND_SINK (asink);

  GST_DSOUND_LOCK (dsoundsink);

  if (dsoundsink->pDSBSecondary) {
    IDirectSoundBuffer_GetCurrentPosition (dsoundsink->pDSBSecondary, &dwCurrentPlayCursor, &dwCurrentWriteCursor);
    dwInitialPlayCursor = dwCurrentPlayCursor;
    dwInitialWriteCursor = dwCurrentWriteCursor;

    /*reset the buffer */
    if (dwCurrentPlayCursor <= dwCurrentWriteCursor)
      buff_size = dsoundsink->buffer_size - (dwCurrentWriteCursor-dwCurrentPlayCursor);
    else
      buff_size = dwCurrentPlayCursor - dwCurrentWriteCursor;

    hRes = IDirectSoundBuffer_Lock (dsoundsink->pDSBSecondary, 0, buff_size, &pLockedBuffer1, &dwSizeBuffer1, &pLockedBuffer2, &dwSizeBuffer2, DSBLOCK_FROMWRITECURSOR);

    if (SUCCEEDED (hRes))
    {
      if (pLockedBuffer1)
        memset (pLockedBuffer1, 0, dwSizeBuffer1);
      if (pLockedBuffer2)
        memset (pLockedBuffer2, 0, dwSizeBuffer2);

      IDirectSoundBuffer_Unlock (dsoundsink->pDSBSecondary, pLockedBuffer1, dwSizeBuffer1, pLockedBuffer2, dwSizeBuffer2);
    }

    do
    {
      IDirectSoundBuffer_GetCurrentPosition (dsoundsink->pDSBSecondary, &dwCurrentPlayCursor, &dwCurrentWriteCursor);    
      if (dwInitialPlayCursor <= dwInitialWriteCursor)
      {
        if (dwCurrentPlayCursor >= dwInitialWriteCursor || dwCurrentPlayCursor <= dwInitialPlayCursor)
          break;
      }
      else
      {
        if (dwCurrentPlayCursor < dwInitialPlayCursor)
        {
          if (dwCurrentPlayCursor > dwInitialWriteCursor)
            break;
        }
      }
      Sleep(5);
    } while (TRUE);

    /*stop playing */
    hRes = IDirectSoundBuffer_Stop (dsoundsink->pDSBSecondary);

    /*reset position */
    hRes = IDirectSoundBuffer_SetCurrentPosition (dsoundsink->pDSBSecondary, 0);
    dsoundsink->current_circular_offset = 0;

    // Now reset entire buffer
    hRes = IDirectSoundBuffer_Lock (dsoundsink->pDSBSecondary,
      dsoundsink->current_circular_offset, dsoundsink->buffer_size,
      &pLockedBuffer1, &dwSizeBuffer1, NULL, NULL, 0L);

    if (SUCCEEDED (hRes)) {
      memset (pLockedBuffer1, 0, dwSizeBuffer1);

      hRes =
        IDirectSoundBuffer_Unlock (dsoundsink->pDSBSecondary, pLockedBuffer1,
        dwSizeBuffer1, NULL, 0);
    }
  }

  dsoundsink->first_buffer_after_reset = TRUE;

  GST_DSOUND_UNLOCK (dsoundsink);
}
#endif // GSTREAMER_LITE

/* 
 * gst_directsound_probe_supported_formats: 
 * 
 * Takes the template caps and returns the subset which is actually 
 * supported by this device. 
 * 
 */

static GstCaps *
gst_directsound_probe_supported_formats (GstDirectSoundSink * dsoundsink,
    const GstCaps * template_caps)
{
  HRESULT hRes;
  DSBUFFERDESC descSecondary;
  WAVEFORMATEX wfx;
  GstCaps *caps;

  caps = gst_caps_copy (template_caps);

  /* 
   * Check availability of digital output by trying to create an SPDIF buffer 
   */

#ifdef WAVE_FORMAT_DOLBY_AC3_SPDIF
  /* fill the WAVEFORMATEX structure with some standard AC3 over SPDIF params */
  memset (&wfx, 0, sizeof (wfx));
  wfx.cbSize = 0;
  wfx.wFormatTag = WAVE_FORMAT_DOLBY_AC3_SPDIF;
  wfx.nChannels = 2;
  wfx.nSamplesPerSec = 48000;
  wfx.wBitsPerSample = 16;
  wfx.nBlockAlign = 4;
  wfx.nAvgBytesPerSec = wfx.nSamplesPerSec * wfx.nBlockAlign;

  // create a secondary directsound buffer 
  memset (&descSecondary, 0, sizeof (DSBUFFERDESC));
  descSecondary.dwSize = sizeof (DSBUFFERDESC);
  descSecondary.dwFlags = DSBCAPS_GETCURRENTPOSITION2 | DSBCAPS_GLOBALFOCUS;
  descSecondary.dwBufferBytes = 6144;
  descSecondary.lpwfxFormat = &wfx;

  hRes = IDirectSound_CreateSoundBuffer (dsoundsink->pDS, &descSecondary,
      &dsoundsink->pDSBSecondary, NULL);
  if (FAILED (hRes)) {
    GST_INFO_OBJECT (dsoundsink, "AC3 passthrough not supported "
        "(IDirectSound_CreateSoundBuffer returned: %s)\n",
        DXGetErrorString9 (hRes));
#ifndef GSTREAMER_LITE
    caps =
        gst_caps_subtract (caps, gst_caps_new_simple ("audio/x-iec958", NULL));
#else // GSTREAMER_LITE
    {
        GstCaps *caps1 = caps;
        GstCaps *caps2 = gst_caps_new_simple ("audio/x-iec958", NULL);
        caps = gst_caps_subtract(caps1, caps2);
        gst_caps_unref(caps1);
        gst_caps_unref(caps2);
    }
#endif // GSTREAMER_LITE
  } else {
    GST_INFO_OBJECT (dsoundsink, "AC3 passthrough supported");
    hRes = IDirectSoundBuffer_Release (dsoundsink->pDSBSecondary);
    if (FAILED (hRes)) {
      GST_DEBUG_OBJECT (dsoundsink,
          "(IDirectSoundBuffer_Release returned: %s)\n",
          DXGetErrorString9 (hRes));
    }
  }
#else
  caps = gst_caps_subtract (caps, gst_caps_new_simple ("audio/x-iec958", NULL));
#endif

  return caps;
}
