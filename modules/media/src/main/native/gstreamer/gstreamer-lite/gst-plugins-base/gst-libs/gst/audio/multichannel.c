/* GStreamer Multichannel-Audio helper functions
 * (c) 2004 Ronald Bultje <rbultje@ronald.bitfreak.net>
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
 * SECTION:gstmultichannel
 * @short_description: Support for multichannel audio elements
 *
 * This module contains some helper functions and a enum to work with
 * multichannel audio.
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "multichannel.h"

#define GST_AUDIO_CHANNEL_POSITIONS_FIELD_NAME "channel-positions"

/**
 * gst_audio_check_channel_positions:
 * @pos: An array of #GstAudioChannelPosition.
 * @channels: The number of elements in @pos.
 *
 * This functions checks if the given channel positions are valid. Channel
 * positions are valid if:
 * <itemizedlist>
 *   <listitem><para>No channel positions appears twice or all positions are %GST_AUDIO_CHANNEL_POSITION_NONE.
 *   </para></listitem>
 *   <listitem><para>Either all or none of the channel positions are %GST_AUDIO_CHANNEL_POSITION_NONE.
 *   </para></listitem>
 *   <listitem><para>%GST_AUDIO_CHANNEL_POSITION_FRONT_MONO and %GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT or %GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT don't appear together in the given positions.
 *   </para></listitem>
 * </itemizedlist>
 *
 * Since: 0.10.20
 *
 * Returns: %TRUE if the given channel positions are valid
 * and %FALSE otherwise.
 */
gboolean
gst_audio_check_channel_positions (const GstAudioChannelPosition * pos,
    guint channels)
{
  gint i, n;

  const struct
  {
    const GstAudioChannelPosition pos1[2];
    const GstAudioChannelPosition pos2[1];
  } conf[] = {
    /* front: mono <-> stereo */
    { {
    GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,
            GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT}, {
    GST_AUDIO_CHANNEL_POSITION_FRONT_MONO}}, { {
    GST_AUDIO_CHANNEL_POSITION_INVALID}}
  };

  g_return_val_if_fail (pos != NULL, FALSE);
  g_return_val_if_fail (channels > 0, FALSE);

  /* check for invalid channel positions */
  for (n = 0; n < channels; n++) {
    if (pos[n] <= GST_AUDIO_CHANNEL_POSITION_INVALID ||
        pos[n] >= GST_AUDIO_CHANNEL_POSITION_NUM) {
      GST_WARNING ("Channel position %d for channel %d is invalid", pos[n], n);
      return FALSE;
    }
  }

  /* either all channel positions are NONE or all are defined,
   * but having only some channel positions NONE and others not
   * is not allowed */
  if (pos[0] == GST_AUDIO_CHANNEL_POSITION_NONE) {
    for (n = 1; n < channels; ++n) {
      if (pos[n] != GST_AUDIO_CHANNEL_POSITION_NONE) {
        GST_WARNING ("Either all channel positions must be defined, or all "
            "be set to NONE, having only some defined is not allowed");
        return FALSE;
      }
    }
    /* all positions are NONE, we are done here */
    return TRUE;
  }

  /* check for multiple position occurrences */
  for (i = GST_AUDIO_CHANNEL_POSITION_INVALID + 1;
      i < GST_AUDIO_CHANNEL_POSITION_NUM; i++) {
    gint count = 0;

    for (n = 0; n < channels; n++) {
      if (pos[n] == i)
        count++;
    }

    /* NONE may not occur mixed with other channel positions */
    if (i == GST_AUDIO_CHANNEL_POSITION_NONE && count > 0) {
      GST_WARNING ("Either all channel positions must be defined, or all "
          "be set to NONE, having only some defined is not allowed");
      return FALSE;
    }

    if (count > 1) {
      GST_WARNING ("Channel position %d occurred %d times, not allowed",
          i, count);
      return FALSE;
    }
  }

  /* check for position conflicts */
  for (i = 0; conf[i].pos1[0] != GST_AUDIO_CHANNEL_POSITION_INVALID; i++) {
    gboolean found1 = FALSE, found2 = FALSE;

    for (n = 0; n < channels; n++) {
      if (pos[n] == conf[i].pos1[0] || pos[n] == conf[i].pos1[1])
        found1 = TRUE;
      else if (pos[n] == conf[i].pos2[0])
        found2 = TRUE;
    }

    if (found1 && found2) {
      GST_WARNING ("Found conflicting channel positions %d/%d and %d",
          conf[i].pos1[0], conf[i].pos1[1], conf[i].pos2[0]);
      return FALSE;
    }
  }

  return TRUE;
}

/* FIXME: these default positions may or may not be correct. In any
 * case, they are mostly just a fallback for buggy plugins, so it
 * should not really matter too much */
#define NUM_DEF_CHANS  8
static const GstAudioChannelPosition
    default_positions[NUM_DEF_CHANS][NUM_DEF_CHANS] = {
  /* 1 channel */
  {
        GST_AUDIO_CHANNEL_POSITION_FRONT_MONO,
      },
  /* 2 channels */
  {
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT,
      },
  /* 3 channels (2.1) */
  {
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT,
        GST_AUDIO_CHANNEL_POSITION_LFE, /* or FRONT_CENTER for 3.0? */
      },
  /* 4 channels (4.0 or 3.1?) */
  {
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT,
        GST_AUDIO_CHANNEL_POSITION_REAR_LEFT,
        GST_AUDIO_CHANNEL_POSITION_REAR_RIGHT,
      },
  /* 5 channels */
  {
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT,
        GST_AUDIO_CHANNEL_POSITION_REAR_LEFT,
        GST_AUDIO_CHANNEL_POSITION_REAR_RIGHT,
        GST_AUDIO_CHANNEL_POSITION_FRONT_CENTER,
      },
  /* 6 channels */
  {
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT,
        GST_AUDIO_CHANNEL_POSITION_REAR_LEFT,
        GST_AUDIO_CHANNEL_POSITION_REAR_RIGHT,
        GST_AUDIO_CHANNEL_POSITION_FRONT_CENTER,
        GST_AUDIO_CHANNEL_POSITION_LFE,
      },
  /* 7 channels */
  {
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT,
        GST_AUDIO_CHANNEL_POSITION_REAR_LEFT,
        GST_AUDIO_CHANNEL_POSITION_REAR_RIGHT,
        GST_AUDIO_CHANNEL_POSITION_FRONT_CENTER,
        GST_AUDIO_CHANNEL_POSITION_LFE,
        GST_AUDIO_CHANNEL_POSITION_REAR_CENTER,
      },
  /* 8 channels */
  {
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT,
        GST_AUDIO_CHANNEL_POSITION_REAR_LEFT,
        GST_AUDIO_CHANNEL_POSITION_REAR_RIGHT,
        GST_AUDIO_CHANNEL_POSITION_FRONT_CENTER,
        GST_AUDIO_CHANNEL_POSITION_LFE,
        GST_AUDIO_CHANNEL_POSITION_SIDE_LEFT,
        GST_AUDIO_CHANNEL_POSITION_SIDE_RIGHT,
      }
};

/**
 * gst_audio_get_channel_positions:
 * @str: A #GstStructure to retrieve channel positions from.
 *
 * Retrieves a number of (fixed!) audio channel positions from
 * the provided #GstStructure and returns it as a newly allocated
 * array. The caller should g_free () this array. The caller
 * should also check that the members in this #GstStructure are
 * indeed "fixed" before calling this function.
 *
 * Returns: a newly allocated array containing the channel
 * positions as provided in the given #GstStructure. Returns
 * NULL on error.
 */

GstAudioChannelPosition *
gst_audio_get_channel_positions (GstStructure * str)
{
  GstAudioChannelPosition *pos;

  gint channels, n;

  const GValue *pos_val_arr, *pos_val_entry;

  gboolean res;

  GType t;

  /* get number of channels, general type checkups */
  g_return_val_if_fail (str != NULL, NULL);
  res = gst_structure_get_int (str, "channels", &channels);
  g_return_val_if_fail (res, NULL);
  g_return_val_if_fail (channels > 0, NULL);
  pos_val_arr = gst_structure_get_value (str,
      GST_AUDIO_CHANNEL_POSITIONS_FIELD_NAME);

  /* The following checks are here to retain compatibility for plugins not
   * implementing this field. They expect that channels=1 implies mono
   * and channels=2 implies stereo, so we follow that. */
  if (pos_val_arr == NULL) {
    /* channel layouts for 1 and 2 channels are implicit, don't warn */
    if (channels > 2) {
      g_warning ("Failed to retrieve channel layout from caps. This usually "
          "means there is a GStreamer element that does not implement "
          "multichannel audio correctly. Please file a bug.");
    }

    /* just return some default channel layout if we have one */
    if (channels >= 1 && channels <= NUM_DEF_CHANS) {
      const GstAudioChannelPosition *p;

      p = default_positions[channels - 1];
      return g_memdup (p, channels * sizeof (GstAudioChannelPosition));
    }

    return NULL;
  }

  g_return_val_if_fail (gst_value_array_get_size (pos_val_arr) == channels,
      NULL);
  for (n = 0; n < channels; n++) {
    t = G_VALUE_TYPE (gst_value_array_get_value (pos_val_arr, n));
    g_return_val_if_fail (t == GST_TYPE_AUDIO_CHANNEL_POSITION, NULL);
  }

  /* ... and fill array */
  pos = g_new (GstAudioChannelPosition, channels);
  for (n = 0; n < channels; n++) {
    pos_val_entry = gst_value_array_get_value (pos_val_arr, n);
    pos[n] = g_value_get_enum (pos_val_entry);
  }

  if (!gst_audio_check_channel_positions (pos, channels)) {
    g_free (pos);
    return NULL;
  }

  return pos;
}

/**
 * gst_audio_set_channel_positions:
 * @str: A #GstStructure to set channel positions on.
 * @pos: an array of channel positions. The number of members
 *       in this array should be equal to the (fixed!) number
 *       of the "channels" field in the given #GstStructure.
 *
 * Adds a "channel-positions" field to the given #GstStructure,
 * which will represent the channel positions as given in the
 * provided #GstAudioChannelPosition array.
 */

void
gst_audio_set_channel_positions (GstStructure * str,
    const GstAudioChannelPosition * pos)
{
  GValue pos_val_arr = { 0 }, pos_val_entry = {
  0};
  gint channels, n;

  gboolean res;

  /* get number of channels, checkups */
  g_return_if_fail (str != NULL);
  g_return_if_fail (pos != NULL);
  res = gst_structure_get_int (str, "channels", &channels);
  g_return_if_fail (res);
  g_return_if_fail (channels > 0);
  if (!gst_audio_check_channel_positions (pos, channels))
    return;

  /* build gvaluearray from positions */
  g_value_init (&pos_val_entry, GST_TYPE_AUDIO_CHANNEL_POSITION);
  g_value_init (&pos_val_arr, GST_TYPE_ARRAY);
  for (n = 0; n < channels; n++) {
    g_value_set_enum (&pos_val_entry, pos[n]);
    gst_value_array_append_value (&pos_val_arr, &pos_val_entry);
  }
  g_value_unset (&pos_val_entry);

  /* add to structure */
  gst_structure_set_value (str,
      GST_AUDIO_CHANNEL_POSITIONS_FIELD_NAME, &pos_val_arr);
  g_value_unset (&pos_val_arr);
}

/**
 * gst_audio_set_structure_channel_positions_list:
 * @str: #GstStructure to set the list of channel positions
 *       on.
 * @pos: the array containing one or more possible audio
 *       channel positions that we should add in each value
 *       of the array in the given structure.
 * @num_positions: the number of values in pos.
 *
 * Sets a (possibly non-fixed) list of possible audio channel
 * positions (given in pos) on the given structure. The
 * structure, after this function has been called, will contain
 * a "channel-positions" field with an array of the size of
 * the "channels" field value in the given structure (note
 * that this means that the channels field in the provided
 * structure should be fixed!). Each value in the array will
 * contain each of the values given in the pos array.
 */

void
gst_audio_set_structure_channel_positions_list (GstStructure * str,
    const GstAudioChannelPosition * pos, gint num_positions)
{
  gint channels, n, c;
  GValue pos_val_arr = { 0 }, pos_val_list = {
  0}, pos_val_entry = {
  0};
  gboolean res;

  /* get number of channels, general type checkups */
  g_return_if_fail (str != NULL);
  g_return_if_fail (num_positions > 0);
  g_return_if_fail (pos != NULL);
  res = gst_structure_get_int (str, "channels", &channels);
  g_return_if_fail (res);
  g_return_if_fail (channels > 0);

  /* create the array of lists */
  g_value_init (&pos_val_arr, GST_TYPE_ARRAY);
  g_value_init (&pos_val_entry, GST_TYPE_AUDIO_CHANNEL_POSITION);
  for (n = 0; n < channels; n++) {
    g_value_init (&pos_val_list, GST_TYPE_LIST);
    for (c = 0; c < num_positions; c++) {
      g_value_set_enum (&pos_val_entry, pos[c]);
      gst_value_list_append_value (&pos_val_list, &pos_val_entry);
    }
    gst_value_array_append_value (&pos_val_arr, &pos_val_list);
    g_value_unset (&pos_val_list);
  }
  g_value_unset (&pos_val_entry);
  gst_structure_set_value (str, GST_AUDIO_CHANNEL_POSITIONS_FIELD_NAME,
      &pos_val_arr);
  g_value_unset (&pos_val_arr);
}

/*
 * Helper function for below. The structure will be conserved,
 * but might be cut down. Any additional structures that were
 * created will be stored in the returned caps.
 */

static GstCaps *
add_list_to_struct (GstStructure * str,
    const GstAudioChannelPosition * pos, gint num_positions)
{
  GstCaps *caps = gst_caps_new_empty ();

  const GValue *chan_val;

  chan_val = gst_structure_get_value (str, "channels");
  if (G_VALUE_TYPE (chan_val) == G_TYPE_INT) {
    gst_audio_set_structure_channel_positions_list (str, pos, num_positions);
  } else if (G_VALUE_TYPE (chan_val) == GST_TYPE_LIST) {
    gint size;

    const GValue *sub_val;

    size = gst_value_list_get_size (chan_val);
    sub_val = gst_value_list_get_value (chan_val, 0);
    gst_structure_set_value (str, "channels", sub_val);
    gst_caps_append (caps, add_list_to_struct (str, pos, num_positions));
    while (--size > 0) {
      str = gst_structure_copy (str);
      sub_val = gst_value_list_get_value (chan_val, size);
      gst_structure_set_value (str, "channels", sub_val);
      gst_caps_append (caps, add_list_to_struct (str, pos, num_positions));
      gst_caps_append_structure (caps, str);
    }
  } else if (G_VALUE_TYPE (chan_val) == GST_TYPE_INT_RANGE) {
    gint min, max;

    min = gst_value_get_int_range_min (chan_val);
    max = gst_value_get_int_range_max (chan_val);

    gst_structure_set (str, "channels", G_TYPE_INT, min, NULL);
    gst_audio_set_structure_channel_positions_list (str, pos, num_positions);
    for (++min; min < max; min++) {
      str = gst_structure_copy (str);
      gst_structure_set (str, "channels", G_TYPE_INT, min, NULL);
      gst_audio_set_structure_channel_positions_list (str, pos, num_positions);
      gst_caps_append_structure (caps, str);
    }
  } else {
    g_warning ("Unexpected value type '%s' for channels field",
        GST_STR_NULL (g_type_name (G_VALUE_TYPE (chan_val))));
  }

  return caps;
}

/**
 * gst_audio_set_caps_channel_positions_list:
 * @caps: #GstCaps to set the list of channel positions on.
 * @pos: the array containing one or more possible audio
 *       channel positions that we should add in each value
 *       of the array in the given structure.
 * @num_positions: the number of values in pos.
 *
 * Sets a (possibly non-fixed) list of possible audio channel
 * positions (given in pos) on the given caps. Each of the
 * structures of the caps, after this function has been called,
 * will contain a "channel-positions" field with an array.
 * Each value in the array will contain each of the values given
 * in the pos array. Note that the size of the caps might be
 * increased by this, since each structure with a "channel-
 * positions" field needs to have a fixed "channels" field.
 * The input caps is not required to have this.
 */

void
gst_audio_set_caps_channel_positions_list (GstCaps * caps,
    const GstAudioChannelPosition * pos, gint num_positions)
{
  gint size, n;

  /* get number of channels, general type checkups */
  g_return_if_fail (caps != NULL);
  g_return_if_fail (num_positions > 0);
  g_return_if_fail (pos != NULL);

  size = gst_caps_get_size (caps);
  for (n = 0; n < size; n++) {
    gst_caps_append (caps, add_list_to_struct (gst_caps_get_structure (caps,
                n), pos, num_positions));
  }
}

/**
 * gst_audio_fixate_channel_positions:
 * @str: a #GstStructure containing a (possibly unfixed)
 *       "channel-positions" field.
 *
 * Custom fixate function. Elements that implement some sort of
 * channel conversion algorithm should use this function for
 * fixating on GstAudioChannelPosition properties. It will take
 * care of equal channel positioning (left/right). Caller g_free()s
 * the return value. The input properties may be (and are supposed
 * to be) unfixed.
 * Note that this function is mostly a hack because we currently
 * have no way to add default fixation functions for new GTypes.
 *
 * Returns: fixed values that the caller could use as a fixed
 * set of #GstAudioChannelPosition values.
 */

GstAudioChannelPosition *
gst_audio_fixate_channel_positions (GstStructure * str)
{
  GstAudioChannelPosition *pos;

  gint channels, n, num_unfixed = 0, i, c;

  const GValue *pos_val_arr, *pos_val_entry, *pos_val;

  gboolean res, is_stereo = TRUE;

  GType t;

  /*
   * We're going to do this cluelessly. We'll make an array of values that
   * conflict with each other and, for each iteration in this array, pick
   * either one until all unknown values are filled. This might not work in
   * corner cases but should work OK for the general case.
   */
  const struct
  {
    const GstAudioChannelPosition pos1[2];
    const GstAudioChannelPosition pos2[1];
  } conf[] = {
    /* front: mono <-> stereo */
    {
      {
      GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,
            GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT}, {
    GST_AUDIO_CHANNEL_POSITION_FRONT_MONO}}, { {
    GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT_OF_CENTER,
            GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT_OF_CENTER}, {
    GST_AUDIO_CHANNEL_POSITION_INVALID}}, { {
    GST_AUDIO_CHANNEL_POSITION_INVALID, GST_AUDIO_CHANNEL_POSITION_INVALID}, {
    GST_AUDIO_CHANNEL_POSITION_FRONT_CENTER}}, { {
    GST_AUDIO_CHANNEL_POSITION_REAR_LEFT,
            GST_AUDIO_CHANNEL_POSITION_REAR_RIGHT}, {
    GST_AUDIO_CHANNEL_POSITION_INVALID}}, { {
    GST_AUDIO_CHANNEL_POSITION_INVALID, GST_AUDIO_CHANNEL_POSITION_INVALID}, {
    GST_AUDIO_CHANNEL_POSITION_REAR_CENTER}}, { {
    GST_AUDIO_CHANNEL_POSITION_INVALID, GST_AUDIO_CHANNEL_POSITION_INVALID}, {
    GST_AUDIO_CHANNEL_POSITION_LFE}}, { {
    GST_AUDIO_CHANNEL_POSITION_SIDE_LEFT,
            GST_AUDIO_CHANNEL_POSITION_SIDE_RIGHT}, {
    GST_AUDIO_CHANNEL_POSITION_INVALID}}, { {
    GST_AUDIO_CHANNEL_POSITION_INVALID, GST_AUDIO_CHANNEL_POSITION_INVALID}, {
    GST_AUDIO_CHANNEL_POSITION_INVALID}}
  };
  struct
  {
    gint num_opt[3];
    guint num_opts[3];
    gboolean is_fixed[3];
    gint choice;                /* -1 is none, 0 is the two, 1 is the one */
  } opt;

  /* get number of channels, general type checkups */
  g_return_val_if_fail (str != NULL, NULL);
  res = gst_structure_get_int (str, "channels", &channels);
  g_return_val_if_fail (res, NULL);
  g_return_val_if_fail (channels > 0, NULL);

  /* 0.8.x mono/stereo checks */
  pos_val_arr = gst_structure_get_value (str,
      GST_AUDIO_CHANNEL_POSITIONS_FIELD_NAME);
  if (!pos_val_arr && (channels == 1 || channels == 2)) {
    pos = g_new (GstAudioChannelPosition, channels);
    if (channels == 1) {
      pos[0] = GST_AUDIO_CHANNEL_POSITION_FRONT_MONO;
    } else {
      pos[0] = GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT;
      pos[1] = GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT;
    }
    return pos;
  }
  g_return_val_if_fail (pos_val_arr != NULL, NULL);
  g_return_val_if_fail (gst_value_array_get_size (pos_val_arr) == channels,
      NULL);
  for (n = 0; n < channels; n++) {
    t = G_VALUE_TYPE (gst_value_array_get_value (pos_val_arr, n));
    g_return_val_if_fail (t == GST_TYPE_LIST ||
        t == GST_TYPE_AUDIO_CHANNEL_POSITION, NULL);
  }

  /* all unknown, to start with */
  pos = g_new (GstAudioChannelPosition, channels);
  for (n = 0; n < channels; n++)
    pos[n] = GST_AUDIO_CHANNEL_POSITION_INVALID;
  num_unfixed = channels;

  /* Iterate the array of conflicting values */
  for (i = 0; conf[i].pos1[0] != GST_AUDIO_CHANNEL_POSITION_INVALID ||
      conf[i].pos2[0] != GST_AUDIO_CHANNEL_POSITION_INVALID; i++) {
    /* front/center only important if not mono (obviously) */
    if (conf[i].pos1[0] == GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT_OF_CENTER &&
        !is_stereo)
      continue;

    /* init values */
    for (n = 0; n < 3; n++) {
      opt.num_opt[n] = -1;
      opt.num_opts[n] = -1;
      opt.is_fixed[n] = FALSE;
    }

    /* Now, we'll see for each channel if it allows for any of the values in
     * the set of conflicting audio channel positions and keep scores. */
    for (n = 0; n < channels; n++) {
      /* if the channel is already taken, don't bother */
      if (pos[n] != GST_AUDIO_CHANNEL_POSITION_INVALID)
        continue;

      pos_val_entry = gst_value_array_get_value (pos_val_arr, n);
      t = G_VALUE_TYPE (pos_val_entry);
      if (t == GST_TYPE_LIST) {
        /* This algorhythm is suboptimal. */
        for (c = 0; c < gst_value_list_get_size (pos_val_entry); c++) {
          pos_val = gst_value_list_get_value (pos_val_entry, c);
          if (g_value_get_enum (pos_val) == conf[i].pos1[0] &&
              opt.num_opts[0] > gst_value_list_get_size (pos_val_entry) &&
              !opt.is_fixed[0]) {
            /* Now test if the old position of num_opt[0] also allows for
             * the other channel (which was skipped previously). If so,
             * keep score. */
            if (opt.num_opt[0] != -1) {
              gint c1;

              pos_val_entry = gst_value_array_get_value (pos_val_arr,
                  opt.num_opt[0]);
              if (G_VALUE_TYPE (pos_val_entry) == GST_TYPE_LIST) {
                for (c1 = 0; c1 < gst_value_list_get_size (pos_val_entry); c1++) {
                  pos_val = gst_value_list_get_value (pos_val_entry, c1);
                  if (g_value_get_enum (pos_val) == conf[i].pos1[1] &&
                      opt.num_opts[1] > opt.num_opts[0] && !opt.is_fixed[1]) {
                    opt.num_opts[1] = opt.num_opts[0];
                    opt.num_opt[1] = opt.num_opt[0];
                  }
                }
                pos_val = gst_value_list_get_value (pos_val_entry, c);
              }
              pos_val_entry = gst_value_array_get_value (pos_val_arr, n);
            }

            /* and save values */
            opt.num_opts[0] = gst_value_list_get_size (pos_val_entry);
            opt.num_opt[0] = n;
          } else if (g_value_get_enum (pos_val) == conf[i].pos1[1] &&
              opt.num_opts[1] > gst_value_list_get_size (pos_val_entry) &&
              !opt.is_fixed[1] && n != opt.num_opt[0]) {
            opt.num_opts[1] = gst_value_list_get_size (pos_val_entry);
            opt.num_opt[1] = n;
          }

          /* 2 goes separately, because 0/1 vs. 2 are separate */
          if (g_value_get_enum (pos_val) == conf[i].pos2[0] &&
              opt.num_opts[2] > gst_value_list_get_size (pos_val_entry) &&
              !opt.is_fixed[2]) {
            opt.num_opts[2] = gst_value_list_get_size (pos_val_entry);
            opt.num_opt[2] = n;
          }
        }
      } else {
        if (g_value_get_enum (pos_val_entry) == conf[i].pos1[0]) {
          opt.num_opt[0] = n;
          opt.is_fixed[0] = TRUE;
        } else if (g_value_get_enum (pos_val_entry) == conf[i].pos1[1]) {
          opt.num_opt[1] = n;
          opt.is_fixed[1] = TRUE;
        } else if (g_value_get_enum (pos_val_entry) == conf[i].pos2[0]) {
          opt.num_opt[2] = n;
          opt.is_fixed[2] = TRUE;
        }
      }
    }

    /* check our results and choose either one */
    if ((opt.is_fixed[0] || opt.is_fixed[1]) && opt.is_fixed[2]) {
      g_warning ("Pre-fixated on both %d/%d and %d - conflict!",
          conf[i].pos1[0], conf[i].pos1[1], conf[i].pos2[0]);
      g_free (pos);
      return NULL;
    } else if ((opt.is_fixed[0] && opt.num_opt[1] == -1) ||
        (opt.is_fixed[1] && opt.num_opt[0] == -1)) {
      g_warning ("Pre-fixated one side, but other side n/a of %d/%d",
          conf[i].pos1[0], conf[i].pos1[1]);
      g_free (pos);
      return NULL;
    } else if (opt.is_fixed[0] || opt.is_fixed[1]) {
      opt.choice = 0;
    } else if (opt.is_fixed[2]) {
      opt.choice = 1;
    } else if (opt.num_opt[0] != -1 && opt.num_opt[1] != -1) {
      opt.choice = 0;
    } else if (opt.num_opt[2] != -1) {
      opt.choice = 1;
    } else {
      opt.choice = -1;
    }

    /* stereo? Note that we keep is_stereo to TRUE if we didn't decide on
     * any arrangement. The mono/stereo channels might be handled elsewhere
     * which is clearly outside the scope of this element, so we cannot
     * know and expect the application to handle that then. */
    if (conf[i].pos2[0] == GST_AUDIO_CHANNEL_POSITION_FRONT_MONO &&
        opt.choice == 1) {
      is_stereo = FALSE;
    }

    /* now actually decide what we'll do and fixate on that */
    if (opt.choice == 0) {
      g_assert (conf[i].pos1[0] != GST_AUDIO_CHANNEL_POSITION_INVALID &&
          conf[i].pos1[1] != GST_AUDIO_CHANNEL_POSITION_INVALID);
      pos[opt.num_opt[0]] = conf[i].pos1[0];
      pos[opt.num_opt[1]] = conf[i].pos1[1];
      num_unfixed -= 2;
    } else if (opt.choice == 1) {
      g_assert (conf[i].pos2[0] != GST_AUDIO_CHANNEL_POSITION_INVALID);
      pos[opt.num_opt[2]] = conf[i].pos2[0];
      num_unfixed--;
    }
  }

  /* safety check */
  if (num_unfixed > 0) {
    g_warning ("%d unfixed channel positions left after fixation!",
        num_unfixed);
    g_free (pos);
    return NULL;
  }

  if (!gst_audio_check_channel_positions (pos, channels)) {
    g_free (pos);
    return NULL;
  }

  return pos;
}
