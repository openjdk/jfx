/* ALSA mixer options object.
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


#ifndef __GST_ALSA_MIXER_OPTIONS_H__
#define __GST_ALSA_MIXER_OPTIONS_H__


#include "gstalsa.h"
#include <gst/interfaces/mixeroptions.h>


G_BEGIN_DECLS


#define GST_ALSA_MIXER_OPTIONS_TYPE             (gst_alsa_mixer_options_get_type ())
#define GST_ALSA_MIXER_OPTIONS(obj)             (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_ALSA_MIXER_OPTIONS,GstAlsaMixerOptions))
#define GST_ALSA_MIXER_OPTIONS_CLASS(klass)     (G_TYPE_CHECK_CLASS_CAST((klass),GST_TYPE_ALSA_MIXER_OPTIONS,GstAlsaMixerOptionsClass))
#define GST_IS_ALSA_MIXER_OPTIONS(obj)          (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_ALSA_MIXER_OPTIONS))
#define GST_IS_ALSA_MIXER_OPTIONS_CLASS(klass)  (G_TYPE_CHECK_CLASS_TYPE((klass),GST_TYPE_ALSA_MIXER_OPTIONS))
#define GST_TYPE_ALSA_MIXER_OPTIONS             (gst_alsa_mixer_options_get_type())


typedef struct _GstAlsaMixerOptions GstAlsaMixerOptions;
typedef struct _GstAlsaMixerOptionsClass GstAlsaMixerOptionsClass;


struct _GstAlsaMixerOptions {
  GstMixerOptions        parent;
  snd_mixer_elem_t      *element; /* the ALSA mixer element for this track */
  gint                  track_num;
};

struct _GstAlsaMixerOptionsClass {
  GstMixerOptionsClass parent;
};


GType           gst_alsa_mixer_options_get_type (void);
GstMixerOptions *gst_alsa_mixer_options_new     (snd_mixer_elem_t *     element,
                                                 gint                   track_num);


G_END_DECLS


#endif /* __GST_ALSA_MIXER_OPTIONS_H__ */
