/* GStreamer
 *
 * Copyright (C) 2003 Ronald Bultje <rbultje@ronald.bitfreak.net>
 *               2006 Edgard Lima <edgard.lima@indt.org.br>
 *
 * gstv4l2tuner.c: tuner interface implementation for V4L2
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

#include <gst/gst.h>

#include "gstv4l2tuner.h"
#include "gstv4l2object.h"
#include "v4l2_calls.h"
#include "v4l2src_calls.h"

static void gst_v4l2_tuner_channel_class_init (GstV4l2TunerChannelClass *
    klass);
static void gst_v4l2_tuner_channel_init (GstV4l2TunerChannel * channel);

static void gst_v4l2_tuner_norm_class_init (GstV4l2TunerNormClass * klass);
static void gst_v4l2_tuner_norm_init (GstV4l2TunerNorm * norm);

static GstTunerNormClass *norm_parent_class = NULL;
static GstTunerChannelClass *channel_parent_class = NULL;

GType
gst_v4l2_tuner_channel_get_type (void)
{
  static GType gst_v4l2_tuner_channel_type = 0;

  if (!gst_v4l2_tuner_channel_type) {
    static const GTypeInfo v4l2_tuner_channel_info = {
      sizeof (GstV4l2TunerChannelClass),
      NULL,
      NULL,
      (GClassInitFunc) gst_v4l2_tuner_channel_class_init,
      NULL,
      NULL,
      sizeof (GstV4l2TunerChannel),
      0,
      (GInstanceInitFunc) gst_v4l2_tuner_channel_init,
      NULL
    };

    gst_v4l2_tuner_channel_type =
        g_type_register_static (GST_TYPE_TUNER_CHANNEL,
        "GstV4l2TunerChannel", &v4l2_tuner_channel_info, 0);
  }

  return gst_v4l2_tuner_channel_type;
}

static void
gst_v4l2_tuner_channel_class_init (GstV4l2TunerChannelClass * klass)
{
  channel_parent_class = g_type_class_peek_parent (klass);
}

static void
gst_v4l2_tuner_channel_init (GstV4l2TunerChannel * channel)
{
  channel->index = (guint32) - 1;
  channel->tuner = (guint32) - 1;
  channel->audio = (guint32) - 1;
}

GType
gst_v4l2_tuner_norm_get_type (void)
{
  static GType gst_v4l2_tuner_norm_type = 0;

  if (!gst_v4l2_tuner_norm_type) {
    static const GTypeInfo v4l2_tuner_norm_info = {
      sizeof (GstV4l2TunerNormClass),
      NULL,
      NULL,
      (GClassInitFunc) gst_v4l2_tuner_norm_class_init,
      NULL,
      NULL,
      sizeof (GstV4l2TunerNorm),
      0,
      (GInstanceInitFunc) gst_v4l2_tuner_norm_init,
      NULL
    };

    gst_v4l2_tuner_norm_type =
        g_type_register_static (GST_TYPE_TUNER_NORM,
        "GstV4l2TunerNorm", &v4l2_tuner_norm_info, 0);
  }

  return gst_v4l2_tuner_norm_type;
}

static void
gst_v4l2_tuner_norm_class_init (GstV4l2TunerNormClass * klass)
{
  norm_parent_class = g_type_class_peek_parent (klass);
}

static void
gst_v4l2_tuner_norm_init (GstV4l2TunerNorm * norm)
{
  norm->index = 0;
}

static G_GNUC_UNUSED gboolean
gst_v4l2_tuner_contains_channel (GstV4l2Object * v4l2object,
    GstV4l2TunerChannel * v4l2channel)
{
  const GList *item;

  for (item = v4l2object->channels; item != NULL; item = item->next)
    if (item->data == v4l2channel)
      return TRUE;

  return FALSE;
}

const GList *
gst_v4l2_tuner_list_channels (GstV4l2Object * v4l2object)
{
  return v4l2object->channels;
}

gboolean
gst_v4l2_tuner_set_channel (GstV4l2Object * v4l2object,
    GstTunerChannel * channel)
{
  GstV4l2TunerChannel *v4l2channel = GST_V4L2_TUNER_CHANNEL (channel);

  /* assert that we're opened and that we're using a known item */
  g_return_val_if_fail (GST_V4L2_IS_OPEN (v4l2object), FALSE);
  g_return_val_if_fail (gst_v4l2_tuner_contains_channel (v4l2object,
          v4l2channel), FALSE);

  if (v4l2object->set_in_out_func (v4l2object, v4l2channel->index)) {
    gst_tuner_channel_changed (GST_TUNER (v4l2object->element), channel);
    /* can FPS change here? */
    return TRUE;
  }

  return FALSE;

}

GstTunerChannel *
gst_v4l2_tuner_get_channel (GstV4l2Object * v4l2object)
{
  GList *item;
  gint channel;

  /* assert that we're opened and that we're using a known item */
  g_return_val_if_fail (GST_V4L2_IS_OPEN (v4l2object), NULL);

  if (v4l2object->get_in_out_func (v4l2object, &channel)) {

    for (item = v4l2object->channels; item != NULL; item = item->next) {
      if (channel == GST_V4L2_TUNER_CHANNEL (item->data)->index)
        return (GstTunerChannel *) item->data;
    }

  }

  return NULL;
}

static G_GNUC_UNUSED gboolean
gst_v4l2_tuner_contains_norm (GstV4l2Object * v4l2object,
    GstV4l2TunerNorm * v4l2norm)
{
  const GList *item;

  for (item = v4l2object->norms; item != NULL; item = item->next)
    if (item->data == v4l2norm)
      return TRUE;

  return FALSE;
}

const GList *
gst_v4l2_tuner_list_norms (GstV4l2Object * v4l2object)
{
  return v4l2object->norms;
}

void
gst_v4l2_tuner_set_norm_and_notify (GstV4l2Object * v4l2object,
    GstTunerNorm * norm)
{
  if (gst_v4l2_tuner_set_norm (v4l2object, norm)) {
#if 0
    g_object_notify (G_OBJECT (v4l2object->element), "norm");
#endif
  }
}

gboolean
gst_v4l2_tuner_set_norm (GstV4l2Object * v4l2object, GstTunerNorm * norm)
{
  GstV4l2TunerNorm *v4l2norm = GST_V4L2_TUNER_NORM (norm);

  /* assert that we're opened and that we're using a known item */
  g_return_val_if_fail (GST_V4L2_IS_OPEN (v4l2object), FALSE);
  g_return_val_if_fail (gst_v4l2_tuner_contains_norm (v4l2object, v4l2norm),
      FALSE);

  if (gst_v4l2_set_norm (v4l2object, v4l2norm->index)) {
    gst_tuner_norm_changed (GST_TUNER (v4l2object->element), norm);
    if (v4l2object->update_fps_func)
      v4l2object->update_fps_func (v4l2object);
    return TRUE;
  }

  return FALSE;

}

GstTunerNorm *
gst_v4l2_tuner_get_norm (GstV4l2Object * v4l2object)
{
  GList *item;
  v4l2_std_id norm;

  /* assert that we're opened and that we're using a known item */
  g_return_val_if_fail (GST_V4L2_IS_OPEN (v4l2object), NULL);

  gst_v4l2_get_norm (v4l2object, &norm);

  for (item = v4l2object->norms; item != NULL; item = item->next) {
    if (norm & GST_V4L2_TUNER_NORM (item->data)->index)
      return (GstTunerNorm *) item->data;
  }

  return NULL;
}

void
gst_v4l2_tuner_set_frequency_and_notify (GstV4l2Object * v4l2object,
    GstTunerChannel * channel, gulong frequency)
{
  if (gst_v4l2_tuner_set_frequency (v4l2object, channel, frequency)) {
#if 0
    g_object_notify (G_OBJECT (v4l2object->element), "frequency");
#endif
  }
}

gboolean
gst_v4l2_tuner_set_frequency (GstV4l2Object * v4l2object,
    GstTunerChannel * channel, gulong frequency)
{
  GstV4l2TunerChannel *v4l2channel = GST_V4L2_TUNER_CHANNEL (channel);
  gint chan;

  /* assert that we're opened and that we're using a known item */
  g_return_val_if_fail (GST_V4L2_IS_OPEN (v4l2object), FALSE);
  g_return_val_if_fail (GST_TUNER_CHANNEL_HAS_FLAG (channel,
          GST_TUNER_CHANNEL_FREQUENCY), FALSE);
  g_return_val_if_fail (gst_v4l2_tuner_contains_channel (v4l2object,
          v4l2channel), FALSE);

  if (v4l2object->get_in_out_func (v4l2object, &chan)) {
    if (chan == GST_V4L2_TUNER_CHANNEL (channel)->index &&
        GST_TUNER_CHANNEL_HAS_FLAG (channel, GST_TUNER_CHANNEL_FREQUENCY)) {
      if (gst_v4l2_set_frequency (v4l2object, v4l2channel->tuner, frequency)) {
        gst_tuner_frequency_changed (GST_TUNER (v4l2object->element), channel,
            frequency);
        return TRUE;
      }
    }
  }

  return FALSE;
}

gulong
gst_v4l2_tuner_get_frequency (GstV4l2Object * v4l2object,
    GstTunerChannel * channel)
{
  GstV4l2TunerChannel *v4l2channel = GST_V4L2_TUNER_CHANNEL (channel);
  gint chan;
  gulong frequency = 0;

  /* assert that we're opened and that we're using a known item */
  g_return_val_if_fail (GST_V4L2_IS_OPEN (v4l2object), 0);
  g_return_val_if_fail (GST_TUNER_CHANNEL_HAS_FLAG (channel,
          GST_TUNER_CHANNEL_FREQUENCY), 0);
  g_return_val_if_fail (gst_v4l2_tuner_contains_channel (v4l2object,
          v4l2channel), 0);

  if (v4l2object->get_in_out_func (v4l2object, &chan)) {
    if (chan == GST_V4L2_TUNER_CHANNEL (channel)->index &&
        GST_TUNER_CHANNEL_HAS_FLAG (channel, GST_TUNER_CHANNEL_FREQUENCY)) {
      gst_v4l2_get_frequency (v4l2object, v4l2channel->tuner, &frequency);
    }
  }

  return frequency;
}

gint
gst_v4l2_tuner_signal_strength (GstV4l2Object * v4l2object,
    GstTunerChannel * channel)
{
  GstV4l2TunerChannel *v4l2channel = GST_V4L2_TUNER_CHANNEL (channel);
  gint chan;
  gulong signal = 0;

  /* assert that we're opened and that we're using a known item */
  g_return_val_if_fail (GST_V4L2_IS_OPEN (v4l2object), 0);
  g_return_val_if_fail (GST_TUNER_CHANNEL_HAS_FLAG (channel,
          GST_TUNER_CHANNEL_FREQUENCY), 0);
  g_return_val_if_fail (gst_v4l2_tuner_contains_channel (v4l2object,
          v4l2channel), 0);

  if (v4l2object->get_in_out_func (v4l2object, &chan)) {
    if (chan == GST_V4L2_TUNER_CHANNEL (channel)->index &&
        GST_TUNER_CHANNEL_HAS_FLAG (channel, GST_TUNER_CHANNEL_FREQUENCY)) {
      gst_v4l2_signal_strength (v4l2object, v4l2channel->tuner, &signal);
    }
  }

  return signal;
}
