/* ALSA mixer track object.
 * Copyright (C) 2003 Leif Johnson <leif@ambient.2y.net>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */


#ifndef __GST_ALSA_MIXER_TRACK_H__
#define __GST_ALSA_MIXER_TRACK_H__


#include "gstalsa.h"
#include <gst/interfaces/mixertrack.h>


G_BEGIN_DECLS


#define GST_ALSA_MIXER_TRACK_TYPE               (gst_alsa_mixer_track_get_type ())
#define GST_ALSA_MIXER_TRACK(obj)               (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_ALSA_MIXER_TRACK,GstAlsaMixerTrack))
#define GST_ALSA_MIXER_TRACK_CLASS(klass)       (G_TYPE_CHECK_CLASS_CAST((klass),GST_TYPE_ALSA_MIXER_TRACK,GstAlsaMixerTrackClass))
#define GST_IS_ALSA_MIXER_TRACK(obj)            (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_ALSA_MIXER_TRACK))
#define GST_IS_ALSA_MIXER_TRACK_CLASS(klass)    (G_TYPE_CHECK_CLASS_TYPE((klass),GST_TYPE_ALSA_MIXER_TRACK))
#define GST_TYPE_ALSA_MIXER_TRACK               (gst_alsa_mixer_track_get_type())

typedef struct _GstAlsaMixerTrack GstAlsaMixerTrack;
typedef struct _GstAlsaMixerTrackClass GstAlsaMixerTrackClass;

#define GST_ALSA_MIXER_TRACK_VOLUME         (1<<0)           /* common volume */
#define GST_ALSA_MIXER_TRACK_PVOLUME        (1<<1)
#define GST_ALSA_MIXER_TRACK_CVOLUME        (1<<2)
#define GST_ALSA_MIXER_TRACK_SWITCH         (1<<3)           /* common switch */
#define GST_ALSA_MIXER_TRACK_PSWITCH        (1<<4)
#define GST_ALSA_MIXER_TRACK_CSWITCH        (1<<5)
#define GST_ALSA_MIXER_TRACK_CSWITCH_EXCL   (1<<6)

#define GST_ALSA_MAX_CHANNELS   (SND_MIXER_SCHN_LAST+1)

struct _GstAlsaMixerTrack {
  GstMixerTrack          parent;
  snd_mixer_elem_t      *element;    /* the ALSA mixer element for this track */
  GstAlsaMixerTrack     *shared_mute;  
  gint                  track_num;
  guint32               alsa_flags;                /* alsa track capabilities */
  gint                  alsa_channels;  
  gint                  capture_group;  
  gint                  volumes[GST_ALSA_MAX_CHANNELS];
};

struct _GstAlsaMixerTrackClass {
  GstMixerTrackClass parent;
};

GType           gst_alsa_mixer_track_get_type   (void);
GstMixerTrack * gst_alsa_mixer_track_new        (snd_mixer_elem_t *     element,
                                                 gint                   num,
                                                 gint                   track_num,
                                                 gint                   flags,
                                                 gboolean               sw,  /* is simple switch? */
                                                 GstAlsaMixerTrack *    shared_mute_track,
                                                 gboolean               label_append_capture);
void            gst_alsa_mixer_track_update      (GstAlsaMixerTrack * alsa_track);

G_END_DECLS


#endif /* __GST_ALSA_MIXER_TRACK_H__ */
