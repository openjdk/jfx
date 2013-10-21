/* ALSA mixer implementation.
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

/**
 * SECTION:element-alsamixer
 * @see_also: alsasink, alsasrc
 *
 * This element controls various aspects such as the volume and balance
 * of an audio device using the ALSA api.
 *
 * The application should query and use the interfaces provided by this 
 * element to control the device.
 *
 * Last reviewed on 2006-03-01 (0.10.4)
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "gstalsamixer.h"
#include <errno.h>

static void gst_alsa_mixer_update_option (GstAlsaMixer * mixer,
    GstAlsaMixerOptions * alsa_opts);
static void gst_alsa_mixer_update_track (GstAlsaMixer * mixer,
    GstAlsaMixerTrack * alsa_track);
static int gst_alsa_mixer_handle_callback (snd_mixer_t * handle,
    unsigned int mask, snd_mixer_elem_t * elem);

/* First some utils, then the mixer implementation */
static gboolean
gst_alsa_mixer_open (GstAlsaMixer * mixer)
{
  gint err;
  snd_ctl_t *ctl;
  snd_ctl_card_info_t *card_info;

  g_return_val_if_fail (mixer->handle == NULL, FALSE);

  /* open and initialize the mixer device */
  err = snd_mixer_open (&mixer->handle, 0);
  if (err < 0 || mixer->handle == NULL)
    goto open_failed;

  if ((err = snd_mixer_attach (mixer->handle, mixer->device)) < 0) {
    GST_WARNING ("Cannot open mixer for sound device '%s': %s", mixer->device,
        snd_strerror (err));
    goto error;
  }

  if ((err = snd_mixer_selem_register (mixer->handle, NULL, NULL)) < 0) {
    GST_WARNING ("Cannot register mixer elements: %s", snd_strerror (err));
    goto error;
  }

  if ((err = snd_mixer_load (mixer->handle)) < 0) {
    GST_WARNING ("Cannot load mixer settings: %s", snd_strerror (err));
    goto error;
  }

  snd_mixer_set_callback_private (mixer->handle, mixer);
  snd_mixer_set_callback (mixer->handle, gst_alsa_mixer_handle_callback);

  /* now get the device name, any of this is not fatal */
  g_free (mixer->cardname);
  if ((err = snd_ctl_open (&ctl, mixer->device, 0)) < 0) {
    GST_WARNING ("Cannot open CTL: %s", snd_strerror (err));
    goto no_card_name;
  }

  snd_ctl_card_info_malloc (&card_info);
  if ((err = snd_ctl_card_info (ctl, card_info)) < 0) {
    GST_WARNING ("Cannot get card info: %s", snd_strerror (err));
    snd_ctl_close (ctl);
    goto no_card_name;
  }

  mixer->cardname = g_strdup (snd_ctl_card_info_get_name (card_info));
  GST_DEBUG ("Card name = %s", GST_STR_NULL (mixer->cardname));
  snd_ctl_card_info_free (card_info);
  snd_ctl_close (ctl);

no_card_name:
  if (mixer->cardname == NULL) {
    mixer->cardname = g_strdup ("Unknown");
    GST_DEBUG ("Cannot find card name");
  }

  GST_INFO ("Successfully opened mixer for device '%s'.", mixer->device);

  return TRUE;

  /* ERROR */
open_failed:
  {
    GST_WARNING ("Cannot open mixer: %s", snd_strerror (err));
    mixer->handle = NULL;
    return FALSE;
  }
error:
  {
    snd_mixer_close (mixer->handle);
    mixer->handle = NULL;
    return FALSE;
  }
}

static snd_mixer_elem_t *
gst_alsa_mixer_find_master_mixer (GstAlsaMixer * mixer, snd_mixer_t * handle)
{
  snd_mixer_elem_t *element;
  gint i, count;

  count = snd_mixer_get_count (handle);

  g_static_rec_mutex_lock (mixer->rec_mutex);

  /* Check if we have a playback mixer labelled as 'Master' */
  element = snd_mixer_first_elem (handle);
  for (i = 0; i < count; i++) {
    if (snd_mixer_selem_has_playback_volume (element) &&
        strcmp (snd_mixer_selem_get_name (element), "Master") == 0) {
      g_static_rec_mutex_unlock (mixer->rec_mutex);
      return element;
    }
    element = snd_mixer_elem_next (element);
  }

  /* If not, check if we have a playback mixer labelled as 'Front' */
  element = snd_mixer_first_elem (handle);
  for (i = 0; i < count; i++) {
    if (snd_mixer_selem_has_playback_volume (element) &&
        strcmp (snd_mixer_selem_get_name (element), "Front") == 0) {
      g_static_rec_mutex_unlock (mixer->rec_mutex);
      return element;
    }
    element = snd_mixer_elem_next (element);
  }

  /* If not, check if we have a playback mixer labelled as 'PCM' */
  element = snd_mixer_first_elem (handle);
  for (i = 0; i < count; i++) {
    if (snd_mixer_selem_has_playback_volume (element) &&
        strcmp (snd_mixer_selem_get_name (element), "PCM") == 0) {
      g_static_rec_mutex_unlock (mixer->rec_mutex);
      return element;
    }
    element = snd_mixer_elem_next (element);
  }

  /* If not, check if we have a playback mixer labelled as 'Speaker' */
  element = snd_mixer_first_elem (handle);
  for (i = 0; i < count; i++) {
    if (snd_mixer_selem_has_playback_volume (element) &&
        strcmp (snd_mixer_selem_get_name (element), "Speaker") == 0) {
      g_static_rec_mutex_unlock (mixer->rec_mutex);
      return element;
    }
    element = snd_mixer_elem_next (element);
  }

  /* If not, check if we have a playback mixer with both volume and switch that
   * is not mono */
  element = snd_mixer_first_elem (handle);
  for (i = 0; i < count; i++) {
    if (snd_mixer_selem_has_playback_volume (element) &&
        snd_mixer_selem_has_playback_switch (element) &&
        !snd_mixer_selem_is_playback_mono (element)) {
      g_static_rec_mutex_unlock (mixer->rec_mutex);
      return element;
    }
    element = snd_mixer_elem_next (element);
  }

  /* If not, check if we have any playback mixer with both volume and switch */
  element = snd_mixer_first_elem (handle);
  for (i = 0; i < count; i++) {
    if (snd_mixer_selem_has_playback_volume (element) &&
        snd_mixer_selem_has_playback_switch (element)) {
      g_static_rec_mutex_unlock (mixer->rec_mutex);
      return element;
    }
    element = snd_mixer_elem_next (element);
  }

  /* If not, take any playback mixer with a volume control */
  element = snd_mixer_first_elem (handle);
  for (i = 0; i < count; i++) {
    if (snd_mixer_selem_has_playback_volume (element)) {
      g_static_rec_mutex_unlock (mixer->rec_mutex);
      return element;
    }
    element = snd_mixer_elem_next (element);
  }

  g_static_rec_mutex_unlock (mixer->rec_mutex);
  /* Looks like we're out of luck ... */
  return NULL;
}

static void
gst_alsa_mixer_update (GstAlsaMixer * mixer, snd_mixer_elem_t * elem)
{
  GList *item;

  g_return_if_fail (mixer != NULL);

  g_static_rec_mutex_lock (mixer->rec_mutex);

  for (item = mixer->tracklist; item != NULL; item = item->next) {
    if (GST_IS_ALSA_MIXER_TRACK (item->data)) {
      if (elem && (GST_ALSA_MIXER_TRACK (item->data)->element != elem))
        continue;

      gst_alsa_mixer_update_track (mixer, GST_ALSA_MIXER_TRACK (item->data));
    } else if (GST_IS_ALSA_MIXER_OPTIONS (item->data)) {
      if (elem && (GST_ALSA_MIXER_OPTIONS (item->data)->element != elem))
        continue;

      gst_alsa_mixer_update_option (mixer, GST_ALSA_MIXER_OPTIONS (item->data));
    }
  }

  g_static_rec_mutex_unlock (mixer->rec_mutex);
}

static int
gst_alsa_mixer_elem_handle_callback (snd_mixer_elem_t * elem, unsigned int mask)
{
  GstAlsaMixer *mixer =
      (GstAlsaMixer *) snd_mixer_elem_get_callback_private (elem);

  GST_LOG ("ALSA elem cb");

  g_return_val_if_fail (mixer != NULL, 1);

  gst_alsa_mixer_update (mixer, elem);

  return 0;
}

static int
gst_alsa_mixer_handle_callback (snd_mixer_t * handle, unsigned int mask,
    snd_mixer_elem_t * elem)
{
  GstAlsaMixer *mixer =
      (GstAlsaMixer *) snd_mixer_get_callback_private (handle);

  GST_LOG ("ALSA cb");

  g_return_val_if_fail (mixer != NULL, 1);

  /* Hopefully won't be call recursively and will handle pending elem events */
  snd_mixer_handle_events (mixer->handle);

  gst_alsa_mixer_update (mixer, elem);

  return 0;
}

static void
gst_alsa_mixer_ensure_track_list (GstAlsaMixer * mixer)
{
  gint i, count;
  snd_mixer_elem_t *element, *master;
  GList *item;

  g_return_if_fail (mixer->handle != NULL);

  if (mixer->tracklist)
    return;

  g_static_rec_mutex_lock (mixer->rec_mutex);

  master = gst_alsa_mixer_find_master_mixer (mixer, mixer->handle);

  count = snd_mixer_get_count (mixer->handle);
  element = snd_mixer_first_elem (mixer->handle);

  /* build track list
   *
   * Some ALSA tracks may have playback and capture capabilities.
   * Here we model them as two separate GStreamer tracks.
   */

  for (i = 0; i < count; i++) {
    GstMixerTrack *play_track = NULL;
    GstMixerTrack *cap_track = NULL;
    const gchar *name;
    GList *item;
    gint samename = 0;

    name = snd_mixer_selem_get_name (element);

    /* prevent dup names */
    for (item = mixer->tracklist; item != NULL; item = item->next) {
      snd_mixer_elem_t *temp;

      if (GST_IS_ALSA_MIXER_OPTIONS (item->data))
        temp = GST_ALSA_MIXER_OPTIONS (item->data)->element;
      else
        temp = GST_ALSA_MIXER_TRACK (item->data)->element;

      if (strcmp (name, snd_mixer_selem_get_name (temp)) == 0)
        samename++;
    }

    GST_LOG ("[%s] probing element #%u, mixer->dir=%u", name, i, mixer->dir);

    if (mixer->dir & GST_ALSA_MIXER_PLAYBACK) {
      gboolean has_playback_switch, has_playback_volume;

      has_playback_switch = snd_mixer_selem_has_playback_switch (element);
      has_playback_volume = snd_mixer_selem_has_playback_volume (element);

      GST_LOG ("[%s] PLAYBACK: has_playback_volume=%d, has_playback_switch=%d"
          "%s", name, has_playback_volume, has_playback_switch,
          (element == master) ? " MASTER" : "");

      if (has_playback_volume) {
        gint flags = GST_MIXER_TRACK_OUTPUT;

        if (element == master)
          flags |= GST_MIXER_TRACK_MASTER;

        play_track = gst_alsa_mixer_track_new (element, samename, i,
            flags, FALSE, NULL, FALSE);

      } else if (has_playback_switch) {
        /* simple mute switch */
        play_track = gst_alsa_mixer_track_new (element, samename, i,
            GST_MIXER_TRACK_OUTPUT, TRUE, NULL, FALSE);
      }

      if (snd_mixer_selem_is_enumerated (element)) {
        GstMixerOptions *opts = gst_alsa_mixer_options_new (element, i);

        GST_LOG ("[%s] is enumerated (%d)", name, i);
        mixer->tracklist = g_list_append (mixer->tracklist, opts);
      }
    }

    if (mixer->dir & GST_ALSA_MIXER_CAPTURE) {
      gboolean has_capture_switch, has_common_switch;
      gboolean has_capture_volume, has_common_volume;

      has_capture_switch = snd_mixer_selem_has_capture_switch (element);
      has_common_switch = snd_mixer_selem_has_common_switch (element);
      has_capture_volume = snd_mixer_selem_has_capture_volume (element);
      has_common_volume = snd_mixer_selem_has_common_volume (element);

      GST_LOG ("[%s] CAPTURE: has_capture_volume=%d, has_common_volume=%d, "
          "has_capture_switch=%d, has_common_switch=%d, play_track=%p", name,
          has_capture_volume, has_common_volume, has_capture_switch,
          has_common_switch, play_track);

      if (has_capture_volume && !(play_track && has_common_volume)) {
        cap_track = gst_alsa_mixer_track_new (element, samename, i,
            GST_MIXER_TRACK_INPUT, FALSE, NULL, play_track != NULL);
      } else if (has_capture_switch && !(play_track && has_common_switch)) {
        cap_track = gst_alsa_mixer_track_new (element, samename, i,
            GST_MIXER_TRACK_INPUT, TRUE, NULL, play_track != NULL);
      }
    }


    if (play_track && cap_track) {
      GST_ALSA_MIXER_TRACK (play_track)->shared_mute =
          GST_ALSA_MIXER_TRACK (cap_track);
      GST_ALSA_MIXER_TRACK (cap_track)->shared_mute =
          GST_ALSA_MIXER_TRACK (play_track);
    }

    if (play_track)
      mixer->tracklist = g_list_append (mixer->tracklist, play_track);

    if (cap_track)
      mixer->tracklist = g_list_append (mixer->tracklist, cap_track);

    element = snd_mixer_elem_next (element);
  }

  for (item = mixer->tracklist; item != NULL; item = item->next) {
    snd_mixer_elem_t *temp;

    if (GST_IS_ALSA_MIXER_OPTIONS (item->data))
      temp = GST_ALSA_MIXER_OPTIONS (item->data)->element;
    else
      temp = GST_ALSA_MIXER_TRACK (item->data)->element;

    snd_mixer_elem_set_callback (temp, gst_alsa_mixer_elem_handle_callback);
    snd_mixer_elem_set_callback_private (temp, mixer);
  }

  g_static_rec_mutex_unlock (mixer->rec_mutex);
}

static void
task_monitor_alsa (gpointer data)
{
  struct pollfd *pfds;
  int nfds, rnfds;
  unsigned short revents;
  GstAlsaMixer *mixer = (GstAlsaMixer *) data;
  gint ret;

  g_static_rec_mutex_lock (mixer->rec_mutex);

  nfds = snd_mixer_poll_descriptors_count (mixer->handle);
  if (nfds <= 0) {
    GST_ERROR ("snd_mixer_poll_descriptors_count <= 0: %d", nfds);
    /* FIXME: sleep ? stop monitoring ? */
    g_static_rec_mutex_unlock (mixer->rec_mutex);
    return;
  }

  pfds = g_newa (struct pollfd, nfds + 1);
  rnfds = snd_mixer_poll_descriptors (mixer->handle, pfds, nfds);
  g_assert (rnfds <= nfds);

  if (rnfds < 0) {
    GST_ELEMENT_ERROR (mixer, RESOURCE, READ, (NULL), ("alsa error: %s",
            snd_strerror (rnfds)));
    gst_task_pause (mixer->task);
    g_static_rec_mutex_unlock (mixer->rec_mutex);
    return;
  }

  pfds[rnfds].fd = mixer->pfd[0];
  pfds[rnfds].events = POLLIN | POLLPRI | POLLHUP | POLLERR;
  pfds[rnfds].revents = 0;

  g_static_rec_mutex_unlock (mixer->rec_mutex);

  GST_LOG ("task loop");
  ret = poll (pfds, rnfds + 1, -1);

  if (ret < 0) {
    GST_ELEMENT_ERROR (mixer, RESOURCE, READ, (NULL), GST_ERROR_SYSTEM);
    gst_task_pause (mixer->task);
    return;
  }

  g_static_rec_mutex_lock (mixer->rec_mutex);

  ret =
      snd_mixer_poll_descriptors_revents (mixer->handle, pfds, nfds, &revents);
  if (ret < 0) {
    GST_ELEMENT_ERROR (mixer, RESOURCE, READ, (NULL), ("alsa error: %s",
            snd_strerror (ret)));
    gst_task_pause (mixer->task);
  } else if (revents & (POLLIN | POLLPRI)) {
    GST_DEBUG ("Handling events");
    snd_mixer_handle_events (mixer->handle);
  } else if (revents & (POLLERR | POLLNVAL | POLLHUP)) {
    GST_ELEMENT_ERROR (mixer, RESOURCE, READ, (NULL), (NULL));
    gst_task_pause (mixer->task);
  }

  g_static_rec_mutex_unlock (mixer->rec_mutex);
}

/* API */

GstAlsaMixer *
gst_alsa_mixer_new (const char *device, GstAlsaMixerDirection dir)
{
  GstAlsaMixer *ret = NULL;

  g_return_val_if_fail (device != NULL, NULL);

  ret = g_new0 (GstAlsaMixer, 1);

  if (pipe (ret->pfd) == -1)
    goto error;

  ret->rec_mutex = g_new (GStaticRecMutex, 1);
  g_static_rec_mutex_init (ret->rec_mutex);

  ret->task_mutex = g_new (GStaticRecMutex, 1);
  g_static_rec_mutex_init (ret->task_mutex);

  ret->task = gst_task_create (task_monitor_alsa, ret);
  gst_task_set_lock (ret->task, ret->task_mutex);

  ret->device = g_strdup (device);
  ret->dir = dir;

  if (!gst_alsa_mixer_open (ret))
    goto error;

  if (gst_task_start (ret->task) == FALSE) {
    GST_WARNING ("Could not start alsamixer task");
  }

  return ret;

  /* ERRORS */
error:
  {
    gst_alsa_mixer_free (ret);
    return NULL;
  }
}

void
gst_alsa_mixer_free (GstAlsaMixer * mixer)
{
  g_return_if_fail (mixer != NULL);

  if (mixer->task) {
    if (write (mixer->pfd[1], "stop", 5) <= 0) {
      GST_ERROR ("Cannot send " "stop" " to alsamixer task");
      close (mixer->pfd[1]);
      mixer->pfd[1] = -1;
    }

    if (gst_task_join (mixer->task) == FALSE) {
      GST_ERROR ("Cannot join alsamixer task");
    }

    gst_object_unref (mixer->task);
    mixer->task = NULL;
  }

  g_static_rec_mutex_free (mixer->task_mutex);
  g_free (mixer->task_mutex);
  mixer->task_mutex = NULL;

  if (mixer->pfd[0] > 0) {
    close (mixer->pfd[0]);
    mixer->pfd[0] = -1;
  }

  if (mixer->pfd[1] > 0) {
    close (mixer->pfd[1]);
    mixer->pfd[1] = -1;
  }

  if (mixer->interface) {
    g_object_unref (G_OBJECT (mixer->interface));
    mixer->interface = NULL;
  }

  if (mixer->device) {
    g_free (mixer->device);
    mixer->device = NULL;
  }

  if (mixer->cardname) {
    g_free (mixer->cardname);
    mixer->cardname = NULL;
  }

  if (mixer->tracklist) {
    g_list_foreach (mixer->tracklist, (GFunc) g_object_unref, NULL);
    g_list_free (mixer->tracklist);
    mixer->tracklist = NULL;
  }

  if (mixer->handle) {
    snd_mixer_close (mixer->handle);
    mixer->handle = NULL;
  }

  g_static_rec_mutex_free (mixer->rec_mutex);
  g_free (mixer->rec_mutex);
  mixer->rec_mutex = NULL;

  g_free (mixer);
}

const GList *
gst_alsa_mixer_list_tracks (GstAlsaMixer * mixer)
{
  g_return_val_if_fail (mixer->handle != NULL, NULL);

  gst_alsa_mixer_ensure_track_list (mixer);

  return (const GList *) mixer->tracklist;
}

void
gst_alsa_mixer_get_volume (GstAlsaMixer * mixer, GstMixerTrack * track,
    gint * volumes)
{
  gint i;
  GstAlsaMixerTrack *alsa_track = GST_ALSA_MIXER_TRACK (track);

  g_return_if_fail (mixer->handle != NULL);

  g_static_rec_mutex_lock (mixer->rec_mutex);

  gst_alsa_mixer_track_update (alsa_track);

  if (track->flags & GST_MIXER_TRACK_OUTPUT) {  /* return playback volume */

    /* Is emulated mute flag activated? */
    if (track->flags & GST_MIXER_TRACK_MUTE &&
        !(alsa_track->alsa_flags & GST_ALSA_MIXER_TRACK_PSWITCH)) {
      for (i = 0; i < track->num_channels; i++)
        volumes[i] = alsa_track->volumes[i];
    } else {
      for (i = 0; i < track->num_channels; i++) {
        long tmp = 0;

        snd_mixer_selem_get_playback_volume (alsa_track->element, i, &tmp);
        alsa_track->volumes[i] = volumes[i] = (gint) tmp;
      }
    }

  } else if (track->flags & GST_MIXER_TRACK_INPUT) {    /* return capture volume */

    /* Is emulated record flag activated? */
    if (alsa_track->alsa_flags & GST_ALSA_MIXER_TRACK_CSWITCH ||
        track->flags & GST_MIXER_TRACK_RECORD) {
      for (i = 0; i < track->num_channels; i++) {
        long tmp = 0;

        snd_mixer_selem_get_capture_volume (alsa_track->element, i, &tmp);
        alsa_track->volumes[i] = volumes[i] = (gint) tmp;
      }
    } else {
      for (i = 0; i < track->num_channels; i++)
        volumes[i] = alsa_track->volumes[i];
    }
  }
  g_static_rec_mutex_unlock (mixer->rec_mutex);
}

static gboolean
check_if_volumes_are_the_same (guint num_channels, gint * volumes)
{
  guint i;

  if (num_channels <= 1)
    return TRUE;

  for (i = 1; i < num_channels; i++) {
    if (volumes[i] != volumes[0])
      return FALSE;
  }

  return TRUE;
}

void
gst_alsa_mixer_set_volume (GstAlsaMixer * mixer, GstMixerTrack * track,
    gint * volumes)
{
  GstAlsaMixerTrack *alsa_track = GST_ALSA_MIXER_TRACK (track);
  gint i;

  g_return_if_fail (mixer->handle != NULL);

  g_static_rec_mutex_lock (mixer->rec_mutex);

  gst_alsa_mixer_track_update (alsa_track);

  if (track->flags & GST_MIXER_TRACK_OUTPUT) {

    /* Is emulated mute flag activated? */
    if (track->flags & GST_MIXER_TRACK_MUTE &&
        !(alsa_track->alsa_flags & GST_ALSA_MIXER_TRACK_PSWITCH)) {
      for (i = 0; i < track->num_channels; i++)
        alsa_track->volumes[i] = volumes[i];
    } else {
      if (check_if_volumes_are_the_same (track->num_channels, volumes)) {
        snd_mixer_selem_set_playback_volume_all (alsa_track->element,
            volumes[0]);
        for (i = 0; i < track->num_channels; i++)
          alsa_track->volumes[i] = volumes[0];
      } else {
        for (i = 0; i < track->num_channels; i++) {
          alsa_track->volumes[i] = volumes[i];
          snd_mixer_selem_set_playback_volume (alsa_track->element, i,
              volumes[i]);
        }
      }
    }

  } else if (track->flags & GST_MIXER_TRACK_INPUT) {

    /* Is emulated record flag activated? */
    if (track->flags & GST_MIXER_TRACK_RECORD ||
        alsa_track->alsa_flags & GST_ALSA_MIXER_TRACK_CSWITCH) {
      if (check_if_volumes_are_the_same (track->num_channels, volumes)) {
        snd_mixer_selem_set_capture_volume_all (alsa_track->element,
            volumes[0]);
        for (i = 0; i < track->num_channels; i++)
          alsa_track->volumes[i] = volumes[0];
      } else {
        for (i = 0; i < track->num_channels; i++) {
          alsa_track->volumes[i] = volumes[i];
          snd_mixer_selem_set_capture_volume (alsa_track->element, i,
              volumes[i]);
        }
      }
    } else {
      for (i = 0; i < track->num_channels; i++)
        alsa_track->volumes[i] = volumes[i];
    }
  }
  g_static_rec_mutex_unlock (mixer->rec_mutex);
}

void
gst_alsa_mixer_set_mute (GstAlsaMixer * mixer, GstMixerTrack * track,
    gboolean mute)
{
  GstAlsaMixerTrack *alsa_track = GST_ALSA_MIXER_TRACK (track);

  g_return_if_fail (mixer->handle != NULL);

  g_static_rec_mutex_lock (mixer->rec_mutex);

  gst_alsa_mixer_track_update (alsa_track);

  if (! !(mute) == ! !(track->flags & GST_MIXER_TRACK_MUTE)) {
    g_static_rec_mutex_unlock (mixer->rec_mutex);
    return;
  }
  if (mute) {
    track->flags |= GST_MIXER_TRACK_MUTE;

    if (alsa_track->shared_mute)
      ((GstMixerTrack *) (alsa_track->shared_mute))->flags |=
          GST_MIXER_TRACK_MUTE;
  } else {
    track->flags &= ~GST_MIXER_TRACK_MUTE;

    if (alsa_track->shared_mute)
      ((GstMixerTrack *) (alsa_track->shared_mute))->flags &=
          ~GST_MIXER_TRACK_MUTE;
  }

  if (alsa_track->alsa_flags & GST_ALSA_MIXER_TRACK_PSWITCH) {
    snd_mixer_selem_set_playback_switch_all (alsa_track->element, mute ? 0 : 1);
  } else {
    gint i;
    GstAlsaMixerTrack *ctrl_track;

    if ((track->flags & GST_MIXER_TRACK_INPUT)
        && alsa_track->shared_mute != NULL)
      ctrl_track = alsa_track->shared_mute;
    else
      ctrl_track = alsa_track;

    for (i = 0; i < ((GstMixerTrack *) ctrl_track)->num_channels; i++) {
      long vol =
          mute ? ((GstMixerTrack *) ctrl_track)->
          min_volume : ctrl_track->volumes[i];
      snd_mixer_selem_set_playback_volume (ctrl_track->element, i, vol);
    }
  }
  g_static_rec_mutex_unlock (mixer->rec_mutex);
}

void
gst_alsa_mixer_set_record (GstAlsaMixer * mixer,
    GstMixerTrack * track, gboolean record)
{
  GstAlsaMixerTrack *alsa_track = GST_ALSA_MIXER_TRACK (track);

  g_return_if_fail (mixer->handle != NULL);

  g_static_rec_mutex_lock (mixer->rec_mutex);

  gst_alsa_mixer_track_update (alsa_track);

  if (! !(record) == ! !(track->flags & GST_MIXER_TRACK_RECORD)) {
    g_static_rec_mutex_unlock (mixer->rec_mutex);
    return;
  }

  if (record) {
    track->flags |= GST_MIXER_TRACK_RECORD;
  } else {
    track->flags &= ~GST_MIXER_TRACK_RECORD;
  }

  if (alsa_track->alsa_flags & GST_ALSA_MIXER_TRACK_CSWITCH) {
    snd_mixer_selem_set_capture_switch_all (alsa_track->element,
        record ? 1 : 0);

    /* update all tracks in same exlusive cswitch group */
    if (alsa_track->alsa_flags & GST_ALSA_MIXER_TRACK_CSWITCH_EXCL) {
      GList *item;

      for (item = mixer->tracklist; item != NULL; item = item->next) {

        if (GST_IS_ALSA_MIXER_TRACK (item->data)) {
          GstAlsaMixerTrack *item_alsa_track =
              GST_ALSA_MIXER_TRACK (item->data);

          if (item_alsa_track->alsa_flags & GST_ALSA_MIXER_TRACK_CSWITCH_EXCL &&
              item_alsa_track->capture_group == alsa_track->capture_group) {
            gst_alsa_mixer_track_update (item_alsa_track);
          }
        }
      }
    }
  } else {
    gint i;

    for (i = 0; i < track->num_channels; i++) {
      long vol = record ? alsa_track->volumes[i] : track->min_volume;

      snd_mixer_selem_set_capture_volume (alsa_track->element, i, vol);
    }
  }
  g_static_rec_mutex_unlock (mixer->rec_mutex);
}

void
gst_alsa_mixer_set_option (GstAlsaMixer * mixer,
    GstMixerOptions * opts, gchar * value)
{
  gint idx = -1, n = 0;
  GList *item;
  GstAlsaMixerOptions *alsa_opts = GST_ALSA_MIXER_OPTIONS (opts);

  g_return_if_fail (mixer->handle != NULL);

  for (item = opts->values; item != NULL; item = item->next, n++) {
    if (!strcmp (item->data, value)) {
      idx = n;
      break;
    }
  }
  if (idx == -1)
    return;

  g_static_rec_mutex_lock (mixer->rec_mutex);
  snd_mixer_selem_set_enum_item (alsa_opts->element, 0, idx);
  g_static_rec_mutex_unlock (mixer->rec_mutex);
}

const gchar *
gst_alsa_mixer_get_option (GstAlsaMixer * mixer, GstMixerOptions * opts)
{
  gint ret;
  guint idx;
  GstAlsaMixerOptions *alsa_opts = GST_ALSA_MIXER_OPTIONS (opts);

  g_return_val_if_fail (mixer->handle != NULL, NULL);
  g_static_rec_mutex_lock (mixer->rec_mutex);
  ret = snd_mixer_selem_get_enum_item (alsa_opts->element, 0, &idx);
  g_static_rec_mutex_unlock (mixer->rec_mutex);
  if (ret == 0)
    return g_list_nth_data (opts->values, idx);
  else
    return snd_strerror (ret);  /* feeble attempt at error handling */
}

GstMixerFlags
gst_alsa_mixer_get_mixer_flags (GstAlsaMixer * mixer)
{
  g_return_val_if_fail (mixer != NULL, GST_MIXER_FLAG_NONE);

  return GST_MIXER_FLAG_AUTO_NOTIFICATIONS;
}

static void
gst_alsa_mixer_update_option (GstAlsaMixer * mixer,
    GstAlsaMixerOptions * alsa_opts)
{
  gint ret;
  guint idx;
  /* const */ gchar *option;

  if (mixer->interface == NULL) {
    GST_WARNING ("Cannot send update notifications, no GstMixer * given");
    return;
  }
  g_static_rec_mutex_lock (mixer->rec_mutex);
  ret = snd_mixer_selem_get_enum_item (alsa_opts->element, 0, &idx);
  g_static_rec_mutex_unlock (mixer->rec_mutex);
  if (ret == 0) {
    option = g_list_nth_data (GST_MIXER_OPTIONS (alsa_opts)->values, idx);
    gst_mixer_option_changed (mixer->interface, GST_MIXER_OPTIONS (alsa_opts),
        option);
  }
}

static void
gst_alsa_mixer_update_track (GstAlsaMixer * mixer,
    GstAlsaMixerTrack * alsa_track)
{
  GstMixerTrack *track = (GstMixerTrack *) alsa_track;
  gboolean old_mute;
  gboolean old_record;
  gint i, n_channels;
  gint *old_volumes;

  GST_DEBUG ("Updating track %" GST_PTR_FORMAT, alsa_track);

  if (mixer->interface == NULL) {
    GST_WARNING ("Cannot send update notifications, no GstMixer * given");
    return;
  }

  old_mute = ! !(GST_MIXER_TRACK_HAS_FLAG (track, GST_MIXER_TRACK_MUTE));
  old_record = ! !(GST_MIXER_TRACK_HAS_FLAG (track, GST_MIXER_TRACK_RECORD));
  old_volumes = g_new (gint, track->num_channels);
  n_channels = track->num_channels;
  memcpy (old_volumes, alsa_track->volumes,
      sizeof (gint) * track->num_channels);

  gst_alsa_mixer_track_update (alsa_track);

  if (old_record !=
      ! !(GST_MIXER_TRACK_HAS_FLAG (track, GST_MIXER_TRACK_RECORD))) {
    gst_mixer_record_toggled (mixer->interface, track,
        ! !GST_MIXER_TRACK_HAS_FLAG (track, GST_MIXER_TRACK_RECORD));
  }
  if (old_mute != ! !(GST_MIXER_TRACK_HAS_FLAG (track, GST_MIXER_TRACK_MUTE))) {
    gst_mixer_mute_toggled (mixer->interface, track,
        ! !GST_MIXER_TRACK_HAS_FLAG (track, GST_MIXER_TRACK_MUTE));
  }

  n_channels = MIN (n_channels, track->num_channels);
  for (i = 0; i < n_channels; i++) {
    if (old_volumes[i] != alsa_track->volumes[i]) {
      gst_mixer_volume_changed (mixer->interface, track, alsa_track->volumes);
      break;
    }
  }
  g_free (old_volumes);
}

/* utility function for gstalsamixerelement to set the interface */
void
_gst_alsa_mixer_set_interface (GstAlsaMixer * mixer, GstMixer * interface)
{
  g_return_if_fail (mixer != NULL && mixer->interface == NULL);
  g_return_if_fail (interface != NULL);

  mixer->interface = g_object_ref (G_OBJECT (interface));
}
