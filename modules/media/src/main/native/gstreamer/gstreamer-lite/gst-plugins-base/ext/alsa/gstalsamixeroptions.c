/* ALSA mixer object implementation.
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

#include "gstalsamixeroptions.h"

static void gst_alsa_mixer_options_init (GstAlsaMixerOptions * alsa_opts);
static void gst_alsa_mixer_options_class_init (gpointer g_class,
    gpointer class_data);

static GstMixerOptionsClass *parent_class = NULL;

GType
gst_alsa_mixer_options_get_type (void)
{
  static GType opts_type = 0;

  if (!opts_type) {
    static const GTypeInfo opts_info = {
      sizeof (GstAlsaMixerOptionsClass),
      NULL,
      NULL,
      gst_alsa_mixer_options_class_init,
      NULL,
      NULL,
      sizeof (GstAlsaMixerOptions),
      0,
      (GInstanceInitFunc) gst_alsa_mixer_options_init,
    };

    opts_type =
        g_type_register_static (GST_TYPE_MIXER_OPTIONS, "GstAlsaMixerOptions",
        &opts_info, 0);
  }

  return opts_type;
}

static void
gst_alsa_mixer_options_class_init (gpointer g_class, gpointer class_data)
{
  parent_class = g_type_class_peek_parent (g_class);
}

static void
gst_alsa_mixer_options_init (GstAlsaMixerOptions * alsa_opts)
{
}

GstMixerOptions *
gst_alsa_mixer_options_new (snd_mixer_elem_t * element, gint track_num)
{
  GstMixerOptions *opts;
  GstAlsaMixerOptions *alsa_opts;
  GstMixerTrack *track;
  const gchar *label;
  guint index;
  gint num, i;
  gchar str[256];

  label = snd_mixer_selem_get_name (element);
  index = snd_mixer_selem_get_index (element);

  GST_LOG ("[%s,%u]", label, index);

  opts = g_object_new (GST_ALSA_MIXER_OPTIONS_TYPE,
      "untranslated-label", label, "index", index, NULL);
  alsa_opts = (GstAlsaMixerOptions *) opts;
  track = (GstMixerTrack *) opts;

  /* set basic information */
  track->label = g_strdup (label);      /* FIXME: translate this? */
  track->num_channels = 0;
  track->flags = 0;
  alsa_opts->element = element;
  alsa_opts->track_num = track_num;

  /* get enumerations for switch/options object */
  num = snd_mixer_selem_get_enum_items (element);
  for (i = 0; i < num; i++) {
    if (snd_mixer_selem_get_enum_item_name (element, i, 255, str) < 0) {
      g_object_unref (G_OBJECT (alsa_opts));
      return NULL;
    }

    opts->values = g_list_append (opts->values, g_strdup (str));
  }

  return opts;
}
