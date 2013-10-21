/* ALSA mixer track implementation.
 * Copyright (C) 2003 Leif Johnson <leif@ambient.2y.net>
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

#include <gst/gst-i18n-plugin.h>

#include "gstalsamixertrack.h"

static void gst_alsa_mixer_track_init (GstAlsaMixerTrack * alsa_track);
static void gst_alsa_mixer_track_class_init (gpointer g_class,
    gpointer class_data);

static GstMixerTrackClass *parent_class = NULL;

GType
gst_alsa_mixer_track_get_type (void)
{
  static GType track_type = 0;

  if (!track_type) {
    static const GTypeInfo track_info = {
      sizeof (GstAlsaMixerTrackClass),
      NULL,
      NULL,
      gst_alsa_mixer_track_class_init,
      NULL,
      NULL,
      sizeof (GstAlsaMixerTrack),
      0,
      (GInstanceInitFunc) gst_alsa_mixer_track_init,
      NULL
    };

    track_type =
        g_type_register_static (GST_TYPE_MIXER_TRACK, "GstAlsaMixerTrack",
        &track_info, 0);
  }

  return track_type;
}

static void
gst_alsa_mixer_track_class_init (gpointer g_class, gpointer class_data)
{
  parent_class = g_type_class_peek_parent (g_class);
}

static void
gst_alsa_mixer_track_init (GstAlsaMixerTrack * alsa_track)
{
}

static void
gst_alsa_mixer_track_update_alsa_capabilities (GstAlsaMixerTrack * alsa_track)
{
  alsa_track->alsa_flags = 0;
  alsa_track->capture_group = -1;

  /* common flags */
  if (snd_mixer_selem_has_common_volume (alsa_track->element))
    alsa_track->alsa_flags |= GST_ALSA_MIXER_TRACK_VOLUME;

  if (snd_mixer_selem_has_common_switch (alsa_track->element))
    alsa_track->alsa_flags |= GST_ALSA_MIXER_TRACK_SWITCH;

  /* Since we create two separate mixer track objects for alsa elements that
   * support both playback and capture, we're going to 'hide' the alsa flags
   * that don't pertain to this mixer track from alsa_flags, otherwise
   * gst_alsa_mixer_track_update() is going to do things we don't want */

  /* playback flags */
  if ((GST_MIXER_TRACK (alsa_track)->flags & GST_MIXER_TRACK_OUTPUT)) {
    if (snd_mixer_selem_has_playback_volume (alsa_track->element))
      alsa_track->alsa_flags |= GST_ALSA_MIXER_TRACK_PVOLUME;

    if (snd_mixer_selem_has_playback_switch (alsa_track->element))
      alsa_track->alsa_flags |= GST_ALSA_MIXER_TRACK_PSWITCH;
  }

  /* capture flags */
  if ((GST_MIXER_TRACK (alsa_track)->flags & GST_MIXER_TRACK_INPUT)) {
    if (snd_mixer_selem_has_capture_volume (alsa_track->element))
      alsa_track->alsa_flags |= GST_ALSA_MIXER_TRACK_CVOLUME;

    if (snd_mixer_selem_has_capture_switch (alsa_track->element)) {
      alsa_track->alsa_flags |= GST_ALSA_MIXER_TRACK_CSWITCH;

      if (snd_mixer_selem_has_capture_switch_exclusive (alsa_track->element)) {
        alsa_track->alsa_flags |= GST_ALSA_MIXER_TRACK_CSWITCH_EXCL;
        alsa_track->capture_group =
            snd_mixer_selem_get_capture_group (alsa_track->element);
      }
    }
  }

  GST_LOG ("[%s] alsa_flags=0x%08x, capture_group=%d",
      snd_mixer_selem_get_name (alsa_track->element),
      alsa_track->alsa_flags, alsa_track->capture_group);
}

inline static gboolean
alsa_track_has_cap (GstAlsaMixerTrack * alsa_track, guint32 flag)
{
  return ((alsa_track->alsa_flags & flag) != 0);
}

GstMixerTrack *
gst_alsa_mixer_track_new (snd_mixer_elem_t * element,
    gint num, gint track_num, gint flags, gboolean sw,
    GstAlsaMixerTrack * shared_mute_track, gboolean append_capture)
{
  GstAlsaMixerTrack *alsa_track;
  GstMixerTrack *track;
  const gchar *name;
  guint index;
  const gchar *label;
  gint i;
  long min = 0, max = 0;
  const struct
  {
    const gchar orig[12];
    const gchar trans[12];
  } alsa_track_labels[] = {
    {
    "Master", N_("Master")}, {
    "Bass", N_("Bass")}, {
    "Treble", N_("Treble")}, {
    "PCM", N_("PCM")}, {
    "Synth", N_("Synth")}, {
    "Line", N_("Line-in")}, {
    "CD", N_("CD")}, {
    "Mic", N_("Microphone")}, {
    "PC Speaker", N_("PC Speaker")}, {
    "Playback", N_("Playback")}, {
    "Capture", N_("Capture")}
  };

  name = snd_mixer_selem_get_name (element);
  index = snd_mixer_selem_get_index (element);

  GST_LOG
      ("[%s,%u] num=%d,track_num=%d,flags=0x%08x,sw=%s,shared_mute_track=%p",
      name, index, num, track_num, flags, (sw) ? "true" : "false",
      shared_mute_track);

  track = (GstMixerTrack *) g_object_new (GST_ALSA_MIXER_TRACK_TYPE,
      "untranslated-label", name, "index", index, NULL);

  alsa_track = (GstAlsaMixerTrack *) track;

  GST_LOG ("[%s] created new mixer track %p", name, track);

  /* This reflects the assumptions used for GstAlsaMixerTrack */
  if (!(!!(flags & GST_MIXER_TRACK_OUTPUT) ^ !!(flags & GST_MIXER_TRACK_INPUT))) {
    GST_ERROR ("Mixer track must be either output or input!");
    g_return_val_if_reached (NULL);
  }

  track->flags = flags;
  alsa_track->element = element;
  alsa_track->shared_mute = shared_mute_track;
  alsa_track->track_num = track_num;
  alsa_track->alsa_channels = 0;

  gst_alsa_mixer_track_update_alsa_capabilities (alsa_track);

  if (flags & GST_MIXER_TRACK_OUTPUT) {
    while (alsa_track->alsa_channels < GST_ALSA_MAX_CHANNELS &&
        snd_mixer_selem_has_playback_channel (element,
            alsa_track->alsa_channels)) {
      alsa_track->alsa_channels++;
    }
    GST_LOG ("[%s] %d output channels", name, alsa_track->alsa_channels);
  } else if (flags & GST_MIXER_TRACK_INPUT) {
    while (alsa_track->alsa_channels < GST_ALSA_MAX_CHANNELS &&
        snd_mixer_selem_has_capture_channel (element,
            alsa_track->alsa_channels)) {
      alsa_track->alsa_channels++;
    }
    GST_LOG ("[%s] %d input channels", name, alsa_track->alsa_channels);
  } else {
    g_assert_not_reached ();
  }

  if (sw)
    track->num_channels = 0;
  else
    track->num_channels = alsa_track->alsa_channels;

  /* translate the name if we can */
  label = name;
  for (i = 0; i < G_N_ELEMENTS (alsa_track_labels); ++i) {
    if (g_utf8_collate (label, alsa_track_labels[i].orig) == 0) {
      label = _(alsa_track_labels[i].trans);
      break;
    }
  }

  if (num == 0) {
    track->label = g_strdup_printf ("%s%s%s", label,
        append_capture ? " " : "", append_capture ? _("Capture") : "");
  } else {
    track->label = g_strdup_printf ("%s%s%s %d", label,
        append_capture ? " " : "", append_capture ? _("Capture") : "", num);
  }

  /* set volume information */
  if (track->num_channels > 0) {
    if ((flags & GST_MIXER_TRACK_OUTPUT))
      snd_mixer_selem_get_playback_volume_range (element, &min, &max);
    else
      snd_mixer_selem_get_capture_volume_range (element, &min, &max);
  }
  track->min_volume = (gint) min;
  track->max_volume = (gint) max;

  for (i = 0; i < track->num_channels; i++) {
    long tmp = 0;

    if (flags & GST_MIXER_TRACK_OUTPUT)
      snd_mixer_selem_get_playback_volume (element, i, &tmp);
    else
      snd_mixer_selem_get_capture_volume (element, i, &tmp);

    alsa_track->volumes[i] = (gint) tmp;
  }

  gst_alsa_mixer_track_update (alsa_track);

  return track;
}

void
gst_alsa_mixer_track_update (GstAlsaMixerTrack * alsa_track)
{
  GstMixerTrack *track = (GstMixerTrack *) alsa_track;
  gint i;
  gint audible = !(track->flags & GST_MIXER_TRACK_MUTE);

  if (alsa_track_has_cap (alsa_track, GST_ALSA_MIXER_TRACK_PVOLUME)) {
    /* update playback volume */
    for (i = 0; i < track->num_channels; i++) {
      long vol = 0;

      snd_mixer_selem_get_playback_volume (alsa_track->element, i, &vol);
      alsa_track->volumes[i] = (gint) vol;
    }
  }

  if (alsa_track_has_cap (alsa_track, GST_ALSA_MIXER_TRACK_CVOLUME)) {
    /* update capture volume */
    for (i = 0; i < track->num_channels; i++) {
      long vol = 0;

      snd_mixer_selem_get_capture_volume (alsa_track->element, i, &vol);
      alsa_track->volumes[i] = (gint) vol;
    }
  }

  /* Any updates in flags? */
  if (alsa_track_has_cap (alsa_track, GST_ALSA_MIXER_TRACK_PSWITCH)) {
    int v = 0;

    audible = 0;
    for (i = 0; i < alsa_track->alsa_channels; ++i) {
      snd_mixer_selem_get_playback_switch (alsa_track->element, i, &v);
      audible += v;
    }

  } else if (alsa_track_has_cap (alsa_track, GST_ALSA_MIXER_TRACK_PVOLUME) &&
      track->flags & GST_MIXER_TRACK_MUTE) {
    /* check if user has raised volume with a parallel running application */

    for (i = 0; i < track->num_channels; i++) {
      long vol = 0;

      snd_mixer_selem_get_playback_volume (alsa_track->element, i, &vol);

      if (vol > track->min_volume) {
        audible = 1;
        break;
      }
    }
  }

  if (!!(audible) != !(track->flags & GST_MIXER_TRACK_MUTE)) {
    if (audible) {
      track->flags &= ~GST_MIXER_TRACK_MUTE;

      if (alsa_track->shared_mute)
        ((GstMixerTrack *) (alsa_track->shared_mute))->flags &=
            ~GST_MIXER_TRACK_MUTE;
    } else {
      track->flags |= GST_MIXER_TRACK_MUTE;

      if (alsa_track->shared_mute)
        ((GstMixerTrack *) (alsa_track->shared_mute))->flags |=
            GST_MIXER_TRACK_MUTE;
    }
  }

  if (track->flags & GST_MIXER_TRACK_INPUT) {
    gint recording = track->flags & GST_MIXER_TRACK_RECORD;

    if (alsa_track_has_cap (alsa_track, GST_ALSA_MIXER_TRACK_CSWITCH)) {
      int v = 0;

      recording = 0;
      for (i = 0; i < alsa_track->alsa_channels; ++i) {
        snd_mixer_selem_get_capture_switch (alsa_track->element, i, &v);
        recording += v;
      }

    } else if (alsa_track_has_cap (alsa_track, GST_ALSA_MIXER_TRACK_CVOLUME) &&
        !(track->flags & GST_MIXER_TRACK_RECORD)) {
      /* check if user has raised volume with a parallel running application */

      for (i = 0; i < track->num_channels; i++) {
        long vol = 0;

        snd_mixer_selem_get_capture_volume (alsa_track->element, i, &vol);

        if (vol > track->min_volume) {
          recording = 1;
          break;
        }
      }
    }

    if (recording)
      track->flags |= GST_MIXER_TRACK_RECORD;
    else
      track->flags &= ~GST_MIXER_TRACK_RECORD;
  }

}
