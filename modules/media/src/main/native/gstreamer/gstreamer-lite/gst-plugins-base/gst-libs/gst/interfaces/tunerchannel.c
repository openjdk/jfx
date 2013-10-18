/* GStreamer Tuner
 * Copyright (C) 2003 Ronald Bultje <rbultje@ronald.bitfreak.net>
 *
 * tunerchannel.c: tuner channel object design
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

#include "tunerchannel.h"

/**
 * SECTION:gsttunerchannel
 * @short_description: A channel from an element implementing the #GstTuner
 * interface.
 *
 * <refsect2>
 * <para>The #GstTunerChannel object is provided by an element implementing
 * the #GstTuner interface.
 * </para>
 * <para>
 * GstTunerChannel provides a name and flags to determine the type and
 * capabilities of the channel. If the GST_TUNER_CHANNEL_FREQUENCY flag is
 * set, then the channel also information about the minimum and maximum
 * frequency, and range of the reported signal strength.
 * </para>
 * </refsect2>
 */

enum
{
  /* FILL ME */
  SIGNAL_FREQUENCY_CHANGED,
  SIGNAL_SIGNAL_CHANGED,
  LAST_SIGNAL
};

static void gst_tuner_channel_class_init (GstTunerChannelClass * klass);
static void gst_tuner_channel_init (GstTunerChannel * channel);
static void gst_tuner_channel_dispose (GObject * object);

static GObjectClass *parent_class = NULL;
static guint signals[LAST_SIGNAL] = { 0 };

GType
gst_tuner_channel_get_type (void)
{
  static GType gst_tuner_channel_type = 0;

  if (!gst_tuner_channel_type) {
    static const GTypeInfo tuner_channel_info = {
      sizeof (GstTunerChannelClass),
      NULL,
      NULL,
      (GClassInitFunc) gst_tuner_channel_class_init,
      NULL,
      NULL,
      sizeof (GstTunerChannel),
      0,
      (GInstanceInitFunc) gst_tuner_channel_init,
      NULL
    };

    gst_tuner_channel_type =
        g_type_register_static (G_TYPE_OBJECT,
        "GstTunerChannel", &tuner_channel_info, 0);
  }

  return gst_tuner_channel_type;
}

static void
gst_tuner_channel_class_init (GstTunerChannelClass * klass)
{
  GObjectClass *object_klass = (GObjectClass *) klass;

  parent_class = g_type_class_peek_parent (klass);

  /**
   * GstTunerChannel::frequency-changed:
   * @tunerchannel: The #GstTunerChannel
   * @frequency: The new frequency (an unsigned long)
   *
   * Reports that the current frequency has changed.
   */
  signals[SIGNAL_FREQUENCY_CHANGED] =
      g_signal_new ("frequency-changed", G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (GstTunerChannelClass,
          frequency_changed),
      NULL, NULL, g_cclosure_marshal_VOID__ULONG, G_TYPE_NONE, 1, G_TYPE_ULONG);
  /**
   * GstTunerChannel::signal-changed:
   * @tunerchannel: The #GstTunerChannel
   * @signal: The new signal strength (an integer)
   *
   * Reports that the signal strength has changed.
   *
   * See Also: gst_tuner_signal_strength()
   */
  signals[SIGNAL_SIGNAL_CHANGED] =
      g_signal_new ("signal-changed", G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (GstTunerChannelClass,
          signal_changed),
      NULL, NULL, g_cclosure_marshal_VOID__INT, G_TYPE_NONE, 1, G_TYPE_INT);

  object_klass->dispose = gst_tuner_channel_dispose;
}

static void
gst_tuner_channel_init (GstTunerChannel * channel)
{
  channel->label = NULL;
  channel->flags = 0;
  channel->min_frequency = channel->max_frequency = 0;
  channel->min_signal = channel->max_signal = 0;
}

static void
gst_tuner_channel_dispose (GObject * object)
{
  GstTunerChannel *channel = GST_TUNER_CHANNEL (object);

  if (channel->label) {
    g_free (channel->label);
    channel->label = NULL;
  }

  if (parent_class->dispose)
    parent_class->dispose (object);
}
