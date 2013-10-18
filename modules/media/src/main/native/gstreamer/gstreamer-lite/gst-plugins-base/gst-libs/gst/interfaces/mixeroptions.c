/* GStreamer Mixer
 * Copyright (C) 2003 Ronald Bultje <rbultje@ronald.bitfreak.net>
 *
 * mixeroptions.c: mixer track options object design
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
 * SECTION:gstmixeroptions
 * @short_description: Multi-option mixer control
 * @see_also: GstMixer, GstMixerTrack
 *
 * Mixer control object that allows switching between multiple options.
 * Note that <classname>GstMixerOptions</classname> is a subclass of
 * <classname>GstMixerTrack</classname>.
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "mixeroptions.h"

#if 0
enum
{
  /* FILL ME */
  SIGNAL_OPTION_CHANGED,
  LAST_SIGNAL
};
static guint signals[LAST_SIGNAL] = { 0 };
#endif

static void gst_mixer_options_class_init (GstMixerOptionsClass * klass);
static void gst_mixer_options_init (GstMixerOptions * mixer);
static void gst_mixer_options_dispose (GObject * object);

static GObjectClass *parent_class = NULL;

GType
gst_mixer_options_get_type (void)
{
  static GType gst_mixer_options_type = 0;

  if (!gst_mixer_options_type) {
    static const GTypeInfo mixer_options_info = {
      sizeof (GstMixerOptionsClass),
      NULL,
      NULL,
      (GClassInitFunc) gst_mixer_options_class_init,
      NULL,
      NULL,
      sizeof (GstMixerOptions),
      0,
      (GInstanceInitFunc) gst_mixer_options_init,
      NULL
    };

    gst_mixer_options_type =
        g_type_register_static (GST_TYPE_MIXER_TRACK,
        "GstMixerOptions", &mixer_options_info, 0);
  }

  return gst_mixer_options_type;
}

static void
gst_mixer_options_class_init (GstMixerOptionsClass * klass)
{
  GObjectClass *object_klass = (GObjectClass *) klass;

  parent_class = g_type_class_peek_parent (klass);
#if 0
  signals[SIGNAL_OPTION_CHANGED] =
      g_signal_new ("option_changed", G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (GstMixerOptionsClass, option_changed),
      NULL, NULL, g_cclosure_marshal_VOID__STRING,
      G_TYPE_NONE, 1, G_TYPE_STRING);
#endif

  object_klass->dispose = gst_mixer_options_dispose;
}

static void
gst_mixer_options_init (GstMixerOptions * mixer_options)
{
  mixer_options->values = NULL;
}

/**
 * gst_mixer_options_get_values:
 * @mixer_options: The #GstMixerOptions item that owns the values.
 *
 * Get the values for the mixer option.
 *
 * Returns: A list of strings with all the possible values for the mixer
 *     option. You must not free or modify the list or its contents, it belongs
 *     to the @mixer_options object.
 */
GList *
gst_mixer_options_get_values (GstMixerOptions * mixer_options)
{
  GstMixerOptionsClass *klass;
  GList *ret = NULL;

  g_return_val_if_fail (GST_IS_MIXER_OPTIONS (mixer_options), NULL);

  klass = GST_MIXER_OPTIONS_GET_CLASS (mixer_options);

  if (klass->get_values != NULL) {
    ret = klass->get_values (mixer_options);
  } else {
    ret = mixer_options->values;
  }

  return ret;
}


static void
gst_mixer_options_dispose (GObject * object)
{
  GstMixerOptions *opts = GST_MIXER_OPTIONS (object);

  g_list_foreach (opts->values, (GFunc) g_free, NULL);
  g_list_free (opts->values);
  opts->values = NULL;

  if (parent_class->dispose)
    parent_class->dispose (object);
}
