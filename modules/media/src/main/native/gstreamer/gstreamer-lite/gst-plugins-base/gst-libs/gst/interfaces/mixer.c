/* GStreamer Mixer
 * Copyright (C) 2003 Ronald Bultje <rbultje@ronald.bitfreak.net>
 *
 * mixer.c: mixer design virtual class function wrappers
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

#include "mixer.h"
#include "interfaces-marshal.h"

#define GST_MIXER_MESSAGE_NAME "gst-mixer-message"

/**
 * SECTION:gstmixer
 * @short_description: Interface for elements that provide mixer operations
 * @see_also: alsamixer, oss4mixer, sunaudiomixer
 *
 * Basic interface for hardware mixer controls.
 *
 * Applications rarely need to use this interface, it is provided mainly
 * for system-level mixer applets and the like. Volume control in playback
 * applications should be done using a <classname>volume</classname>
 * element or, if available, using the <quote>volume</quote> property of
 * the audio sink element used (as provided by <classname>pulsesink</classname>
 * for example), or even better: just use the <classname>playbin2</classname>
 * element's <quote>volume</quote> property.
 *
 * Usage: In order to use the <classname>GstMixer</classname> interface, the
 * element needs to be at least in READY state (so that the element has opened
 * the mixer device). Once the element has been set to READY state or higher,
 * it can be cast to a <classname>GstMixer</classname> using the GST_MIXER
 * macro (in C) and the mixer API can be used.
 */

#ifndef GST_DISABLE_DEPRECATED
enum
{
  SIGNAL_MUTE_TOGGLED,
  SIGNAL_RECORD_TOGGLED,
  SIGNAL_VOLUME_CHANGED,
  SIGNAL_OPTION_CHANGED,
  LAST_SIGNAL
};

static guint gst_mixer_signals[LAST_SIGNAL] = { 0 };

#endif

static void gst_mixer_class_init (GstMixerClass * klass);

GType
gst_mixer_get_type (void)
{
  static GType gst_mixer_type = 0;

  if (!gst_mixer_type) {
    static const GTypeInfo gst_mixer_info = {
      sizeof (GstMixerClass),
      (GBaseInitFunc) gst_mixer_class_init,
      NULL,
      NULL,
      NULL,
      NULL,
      0,
      0,
      NULL,
    };

    gst_mixer_type = g_type_register_static (G_TYPE_INTERFACE,
        "GstMixer", &gst_mixer_info, 0);
    g_type_interface_add_prerequisite (gst_mixer_type,
        GST_TYPE_IMPLEMENTS_INTERFACE);
  }

  return gst_mixer_type;
}

static void
gst_mixer_class_init (GstMixerClass * klass)
{
#ifndef GST_DISABLE_DEPRECATED
  static gboolean initialized = FALSE;

  /* signals (deprecated) */
  if (!initialized) {
    gst_mixer_signals[SIGNAL_RECORD_TOGGLED] =
        g_signal_new ("record-toggled",
        GST_TYPE_MIXER, G_SIGNAL_RUN_LAST,
        G_STRUCT_OFFSET (GstMixerClass, record_toggled),
        NULL, NULL,
        gst_interfaces_marshal_VOID__OBJECT_BOOLEAN, G_TYPE_NONE, 2,
        GST_TYPE_MIXER_TRACK, G_TYPE_BOOLEAN);
    gst_mixer_signals[SIGNAL_MUTE_TOGGLED] =
        g_signal_new ("mute-toggled",
        GST_TYPE_MIXER, G_SIGNAL_RUN_LAST,
        G_STRUCT_OFFSET (GstMixerClass, mute_toggled),
        NULL, NULL,
        gst_interfaces_marshal_VOID__OBJECT_BOOLEAN, G_TYPE_NONE, 2,
        GST_TYPE_MIXER_TRACK, G_TYPE_BOOLEAN);
    gst_mixer_signals[SIGNAL_VOLUME_CHANGED] =
        g_signal_new ("volume-changed",
        GST_TYPE_MIXER, G_SIGNAL_RUN_LAST,
        G_STRUCT_OFFSET (GstMixerClass, volume_changed),
        NULL, NULL,
        gst_interfaces_marshal_VOID__OBJECT_POINTER, G_TYPE_NONE, 2,
        GST_TYPE_MIXER_TRACK, G_TYPE_POINTER);
    gst_mixer_signals[SIGNAL_OPTION_CHANGED] =
        g_signal_new ("option-changed",
        GST_TYPE_MIXER, G_SIGNAL_RUN_LAST,
        G_STRUCT_OFFSET (GstMixerClass, option_changed),
        NULL, NULL,
        gst_interfaces_marshal_VOID__OBJECT_STRING, G_TYPE_NONE, 2,
        GST_TYPE_MIXER_OPTIONS, G_TYPE_STRING);

    initialized = TRUE;
  }
#endif

  klass->mixer_type = GST_MIXER_SOFTWARE;

  /* default virtual functions */
  klass->list_tracks = NULL;
  klass->set_volume = NULL;
  klass->get_volume = NULL;
  klass->set_mute = NULL;
  klass->set_record = NULL;
  klass->set_option = NULL;
  klass->get_option = NULL;
}

/**
 * gst_mixer_list_tracks:
 * @mixer: the #GstMixer (a #GstElement) to get the tracks from.
 *
 * Returns a list of available tracks for this mixer/element. Note
 * that it is allowed for sink (output) elements to only provide
 * the output tracks in this list. Likewise, for sources (inputs),
 * it is allowed to only provide input elements in this list.
 *
 * Returns: A #GList consisting of zero or more #GstMixerTracks.
 *          The list is owned by the #GstMixer instance and must not be freed
 *          or modified.
 */

const GList *
gst_mixer_list_tracks (GstMixer * mixer)
{
  GstMixerClass *klass;

  g_return_val_if_fail (mixer != NULL, NULL);

  klass = GST_MIXER_GET_CLASS (mixer);

  if (klass->list_tracks) {
    return klass->list_tracks (mixer);
  }

  return NULL;
}

/**
 * gst_mixer_set_volume:
 * @mixer: The #GstMixer (a #GstElement) that owns the track.
 * @track: The #GstMixerTrack to set the volume on.
 * @volumes: an array of integers (of size track->num_channels)
 *           that gives the wanted volume for each channel in
 *           this track.
 *
 * Sets the volume on each channel in a track. Short note about
 * naming: a track is defined as one separate stream owned by
 * the mixer/element, such as 'Line-in' or 'Microphone'. A
 * channel is said to be a mono-stream inside this track. A
 * stereo track thus contains two channels.
 */

void
gst_mixer_set_volume (GstMixer * mixer, GstMixerTrack * track, gint * volumes)
{
  GstMixerClass *klass;

  g_return_if_fail (mixer != NULL);
  g_return_if_fail (track != NULL);
  g_return_if_fail (volumes != NULL);

  klass = GST_MIXER_GET_CLASS (mixer);

  if (klass->set_volume) {
    klass->set_volume (mixer, track, volumes);
  }
}

/**
 * gst_mixer_get_volume:
 * @mixer: the #GstMixer (a #GstElement) that owns the track
 * @track: the GstMixerTrack to get the volume from.
 * @volumes: a pre-allocated array of integers (of size
 *           track->num_channels) to store the current volume
 *           of each channel in the given track in.
 *
 * Get the current volume(s) on the given track.
 */

void
gst_mixer_get_volume (GstMixer * mixer, GstMixerTrack * track, gint * volumes)
{
  GstMixerClass *klass;

  g_return_if_fail (mixer != NULL);
  g_return_if_fail (track != NULL);
  g_return_if_fail (volumes != NULL);

  klass = GST_MIXER_GET_CLASS (mixer);

  if (klass->get_volume) {
    klass->get_volume (mixer, track, volumes);
  } else {
    gint i;

    for (i = 0; i < track->num_channels; i++) {
      volumes[i] = 0;
    }
  }
}

/**
 * gst_mixer_set_mute:
 * @mixer: the #GstMixer (a #GstElement) that owns the track.
 * @track: the #GstMixerTrack to operate on.
 * @mute: a boolean value indicating whether to turn on or off
 *        muting.
 *
 * Mutes or unmutes the given channel. To find out whether a
 * track is currently muted, use GST_MIXER_TRACK_HAS_FLAG ().
 */

void
gst_mixer_set_mute (GstMixer * mixer, GstMixerTrack * track, gboolean mute)
{
  GstMixerClass *klass;

  g_return_if_fail (mixer != NULL);
  g_return_if_fail (track != NULL);

  klass = GST_MIXER_GET_CLASS (mixer);

  if (klass->set_mute) {
    klass->set_mute (mixer, track, mute);
  }
}

/**
 * gst_mixer_set_record:
 * @mixer: The #GstMixer (a #GstElement) that owns the track.
 * @track: the #GstMixerTrack to operate on.
 * @record: a boolean value that indicates whether to turn on
 *          or off recording.
 *
 * Enables or disables recording on the given track. Note that
 * this is only possible on input tracks, not on output tracks
 * (see GST_MIXER_TRACK_HAS_FLAG () and the GST_MIXER_TRACK_INPUT
 * flag).
 */

void
gst_mixer_set_record (GstMixer * mixer, GstMixerTrack * track, gboolean record)
{
  GstMixerClass *klass = GST_MIXER_GET_CLASS (mixer);

  if (klass->set_record) {
    klass->set_record (mixer, track, record);
  }
}

/**
 * gst_mixer_set_option:
 * @mixer: The #GstMixer (a #GstElement) that owns the optionlist.
 * @opts: The #GstMixerOptions that we operate on.
 * @value: The requested new option value.
 *
 * Sets a name/value option in the mixer to the requested value.
 */

void
gst_mixer_set_option (GstMixer * mixer, GstMixerOptions * opts, gchar * value)
{
  GstMixerClass *klass;

  g_return_if_fail (mixer != NULL);
  g_return_if_fail (opts != NULL);

  klass = GST_MIXER_GET_CLASS (mixer);

  if (klass->set_option) {
    klass->set_option (mixer, opts, value);
  }
}

/**
 * gst_mixer_get_option:
 * @mixer: The #GstMixer (a #GstElement) that owns the optionlist.
 * @opts: The #GstMixerOptions that we operate on.
 *
 * Get the current value of a name/value option in the mixer.
 *
 * Returns: current value of the name/value option.
 */

const gchar *
gst_mixer_get_option (GstMixer * mixer, GstMixerOptions * opts)
{
  GstMixerClass *klass;

  g_return_val_if_fail (mixer != NULL, NULL);
  g_return_val_if_fail (opts != NULL, NULL);

  klass = GST_MIXER_GET_CLASS (mixer);

  if (klass->get_option) {
    return klass->get_option (mixer, opts);
  }

  return NULL;
}

/**
 * gst_mixer_get_mixer_type:
 * @mixer: The #GstMixer implementation
 *
 * Get the #GstMixerType of this mixer implementation.
 *
 * Returns: A the #GstMixerType.
 *
 * Since: 0.10.24
 */
GstMixerType
gst_mixer_get_mixer_type (GstMixer * mixer)
{
  GstMixerClass *klass = GST_MIXER_GET_CLASS (mixer);

  return klass->mixer_type;
}

/**
 * gst_mixer_get_mixer_flags:
 * @mixer: The #GstMixer implementation
 *
 * Get the set of supported flags for this mixer implementation.
 *
 * Returns: A set of or-ed GstMixerFlags for supported features.
 */
GstMixerFlags
gst_mixer_get_mixer_flags (GstMixer * mixer)
{
  GstMixerClass *klass;

  g_return_val_if_fail (mixer != NULL, FALSE);
  klass = GST_MIXER_GET_CLASS (mixer);

  if (klass->get_mixer_flags) {
    return klass->get_mixer_flags (mixer);
  }
  return GST_MIXER_FLAG_NONE;
}

/**
 * gst_mixer_mute_toggled:
 * @mixer: the #GstMixer (a #GstElement) that owns the track
 * @track: the GstMixerTrack that has change mute state.
 * @mute: the new state of the mute flag on the track
 *
 * This function is called by the mixer implementation to produce
 * a notification message on the bus indicating that the given track
 * has changed mute state.
 *
 * This function only works for GstElements that are implementing the
 * GstMixer interface, and the element needs to have been provided a bus.
 */
void
gst_mixer_mute_toggled (GstMixer * mixer, GstMixerTrack * track, gboolean mute)
{
  GstStructure *s;
  GstMessage *m;

  g_return_if_fail (mixer != NULL);
  g_return_if_fail (GST_IS_ELEMENT (mixer));
  g_return_if_fail (track != NULL);

  s = gst_structure_new (GST_MIXER_MESSAGE_NAME,
      "type", G_TYPE_STRING, "mute-toggled",
      "track", GST_TYPE_MIXER_TRACK, track, "mute", G_TYPE_BOOLEAN, mute, NULL);

  m = gst_message_new_element (GST_OBJECT (mixer), s);
  if (gst_element_post_message (GST_ELEMENT (mixer), m) == FALSE) {
    GST_WARNING ("This element has no bus, therefore no message sent!");
  }
}

/**
 * gst_mixer_record_toggled:
 * @mixer: the #GstMixer (a #GstElement) that owns the track
 * @track: the GstMixerTrack that has changed recording state.
 * @record: the new state of the record flag on the track
 *
 * This function is called by the mixer implementation to produce
 * a notification message on the bus indicating that the given track
 * has changed recording state.
 *
 * This function only works for GstElements that are implementing the
 * GstMixer interface, and the element needs to have been provided a bus.
 */
void
gst_mixer_record_toggled (GstMixer * mixer,
    GstMixerTrack * track, gboolean record)
{
  GstStructure *s;
  GstMessage *m;

  g_return_if_fail (mixer != NULL);
  g_return_if_fail (GST_IS_ELEMENT (mixer));
  g_return_if_fail (track != NULL);

  s = gst_structure_new (GST_MIXER_MESSAGE_NAME,
      "type", G_TYPE_STRING, "record-toggled",
      "track", GST_TYPE_MIXER_TRACK, track,
      "record", G_TYPE_BOOLEAN, record, NULL);

  m = gst_message_new_element (GST_OBJECT (mixer), s);
  if (gst_element_post_message (GST_ELEMENT (mixer), m) == FALSE) {
    GST_WARNING ("This element has no bus, therefore no message sent!");
  }
}

/**
 * gst_mixer_volume_changed:
 * @mixer: the #GstMixer (a #GstElement) that owns the track
 * @track: the GstMixerTrack that has changed.
 * @volumes: Array of volume values, one per channel on the mixer track.
 *
 * This function is called by the mixer implementation to produce
 * a notification message on the bus indicating that the volume(s) for the
 * given track have changed.
 *
 * This function only works for GstElements that are implementing the
 * GstMixer interface, and the element needs to have been provided a bus.
 */
void
gst_mixer_volume_changed (GstMixer * mixer,
    GstMixerTrack * track, gint * volumes)
{
  GstStructure *s;
  GstMessage *m;
  GValue l = { 0, };
  GValue v = { 0, };
  gint i;

  g_return_if_fail (mixer != NULL);
  g_return_if_fail (GST_IS_ELEMENT (mixer));
  g_return_if_fail (track != NULL);

  s = gst_structure_new (GST_MIXER_MESSAGE_NAME,
      "type", G_TYPE_STRING, "volume-changed",
      "track", GST_TYPE_MIXER_TRACK, track, NULL);

  g_value_init (&l, GST_TYPE_ARRAY);

  g_value_init (&v, G_TYPE_INT);

  /* FIXME 0.11: pass track->num_channels to the function */
  for (i = 0; i < track->num_channels; ++i) {
    g_value_set_int (&v, volumes[i]);
    gst_value_array_append_value (&l, &v);
  }
  g_value_unset (&v);

  gst_structure_set_value (s, "volumes", &l);
  g_value_unset (&l);

  m = gst_message_new_element (GST_OBJECT (mixer), s);
  if (gst_element_post_message (GST_ELEMENT (mixer), m) == FALSE) {
    GST_WARNING ("This element has no bus, therefore no message sent!");
  }
}

/**
 * gst_mixer_option_changed:
 * @mixer: the #GstMixer (a #GstElement) that owns the options 
 * @opts: the GstMixerOptions that has changed value.
 * @value: the new value of the GstMixerOptions.
 *
 * This function is called by the mixer implementation to produce
 * a notification message on the bus indicating that the given options
 * object has changed state. 
 *
 * This function only works for GstElements that are implementing the
 * GstMixer interface, and the element needs to have been provided a bus.
 */
void
gst_mixer_option_changed (GstMixer * mixer,
    GstMixerOptions * opts, const gchar * value)
{
  GstStructure *s;
  GstMessage *m;

  g_return_if_fail (mixer != NULL);
  g_return_if_fail (GST_IS_ELEMENT (mixer));
  g_return_if_fail (opts != NULL);

  s = gst_structure_new (GST_MIXER_MESSAGE_NAME,
      "type", G_TYPE_STRING, "option-changed",
      "options", GST_TYPE_MIXER_OPTIONS, opts,
      "value", G_TYPE_STRING, value, NULL);

  m = gst_message_new_element (GST_OBJECT (mixer), s);
  if (gst_element_post_message (GST_ELEMENT (mixer), m) == FALSE) {
    GST_WARNING ("This element has no bus, therefore no message sent!");
  }
}

/**
 * gst_mixer_options_list_changed:
 * @mixer: the #GstMixer (a #GstElement) that owns the options 
 * @opts: the GstMixerOptions whose list of values has changed
 *
 * This function is called by the mixer implementation to produce
 * a notification message on the bus indicating that the list of possible
 * options of a given options object has changed.
 *
 * The new options are not contained in the message on purpose. Applications
 * should call gst_mixer_option_get_values() on @opts to make @opts update
 * its internal state and obtain the new list of values.
 *
 * This function only works for GstElements that are implementing the
 * GstMixer interface, and the element needs to have been provided a bus
 * for this to work.
 *
 * Since: 0.10.18
 */
void
gst_mixer_options_list_changed (GstMixer * mixer, GstMixerOptions * opts)
{
  GstStructure *s;
  GstMessage *m;

  g_return_if_fail (mixer != NULL);
  g_return_if_fail (GST_IS_ELEMENT (mixer));
  g_return_if_fail (opts != NULL);
  g_return_if_fail (GST_IS_MIXER_OPTIONS (opts));

  /* we do not include the new list here on purpose, so that the application
   * has to use gst_mixer_options_get_values() to get the new list, which then
   * allows the mixer options object to update the internal GList in a somewhat
   * thread-safe way at least */
  s = gst_structure_new (GST_MIXER_MESSAGE_NAME,
      "type", G_TYPE_STRING, "options-list-changed",
      "options", GST_TYPE_MIXER_OPTIONS, opts, NULL);

  m = gst_message_new_element (GST_OBJECT (mixer), s);
  if (gst_element_post_message (GST_ELEMENT (mixer), m) == FALSE) {
    GST_WARNING ("This element has no bus, therefore no message sent!");
  }
}

/**
 * gst_mixer_mixer_changed:
 * @mixer: the #GstMixer (a #GstElement) which has changed
 *
 * This function is called by the mixer implementation to produce
 * a notification message on the bus indicating that the list of available
 * mixer tracks for a given mixer object has changed. Applications should
 * rebuild their interface when they receive this message.
 *
 * This function only works for GstElements that are implementing the
 * GstMixer interface, and the element needs to have been provided a bus.
 *
 * Since: 0.10.18
 */
void
gst_mixer_mixer_changed (GstMixer * mixer)
{
  GstStructure *s;
  GstMessage *m;

  g_return_if_fail (mixer != NULL);
  g_return_if_fail (GST_IS_ELEMENT (mixer));

  s = gst_structure_new (GST_MIXER_MESSAGE_NAME,
      "type", G_TYPE_STRING, "mixer-changed", NULL);

  m = gst_message_new_element (GST_OBJECT (mixer), s);
  if (gst_element_post_message (GST_ELEMENT (mixer), m) == FALSE) {
    GST_WARNING ("This element has no bus, therefore no message sent!");
  }
}

static gboolean
gst_mixer_message_is_mixer_message (GstMessage * message)
{
  const GstStructure *s;

  if (message == NULL)
    return FALSE;
  if (GST_MESSAGE_TYPE (message) != GST_MESSAGE_ELEMENT)
    return FALSE;

  s = gst_message_get_structure (message);
  return gst_structure_has_name (s, GST_MIXER_MESSAGE_NAME);
}

/**
 * gst_mixer_message_get_type:
 * @message: A GstMessage to inspect.
 *
 * Check a bus message to see if it is a GstMixer notification
 * message and return the GstMixerMessageType identifying which
 * type of notification it is.
 *
 * Returns: The type of the GstMixerMessage, or GST_MIXER_MESSAGE_INVALID
 * if the message is not a GstMixer notification.
 *
 * Since: 0.10.14
 */
GstMixerMessageType
gst_mixer_message_get_type (GstMessage * message)
{
  const GstStructure *s;
  const gchar *m_type;

  if (!gst_mixer_message_is_mixer_message (message))
    return GST_MIXER_MESSAGE_INVALID;

  s = gst_message_get_structure (message);
  m_type = gst_structure_get_string (s, "type");
  g_return_val_if_fail (m_type != NULL, GST_MIXER_MESSAGE_INVALID);

  if (g_str_equal (m_type, "mute-toggled"))
    return GST_MIXER_MESSAGE_MUTE_TOGGLED;
  else if (g_str_equal (m_type, "record-toggled"))
    return GST_MIXER_MESSAGE_RECORD_TOGGLED;
  else if (g_str_equal (m_type, "volume-changed"))
    return GST_MIXER_MESSAGE_VOLUME_CHANGED;
  else if (g_str_equal (m_type, "option-changed"))
    return GST_MIXER_MESSAGE_OPTION_CHANGED;
  else if (g_str_equal (m_type, "options-list-changed"))
    return GST_MIXER_MESSAGE_OPTIONS_LIST_CHANGED;
  else if (g_str_equal (m_type, "mixer-changed"))
    return GST_MIXER_MESSAGE_MIXER_CHANGED;

  return GST_MIXER_MESSAGE_INVALID;
}

#define GST_MIXER_MESSAGE_HAS_TYPE(msg,msg_type) \
(gst_mixer_message_get_type (msg) == GST_MIXER_MESSAGE_ ## msg_type)

/**
 * gst_mixer_message_parse_mute_toggled:
 * @message: A mute-toggled change notification message.
 * @track: Pointer to hold a GstMixerTrack object, or NULL.
 * @mute: A pointer to a gboolean variable, or NULL.
 *
 * Extracts the contents of a mute-toggled bus message. Reads
 * the GstMixerTrack that has changed, and the new value of the mute
 * flag.
 *
 * The GstMixerTrack remains valid until the message is freed.
 *
 * Since: 0.10.14
 */
void
gst_mixer_message_parse_mute_toggled (GstMessage * message,
    GstMixerTrack ** track, gboolean * mute)
{
  const GstStructure *s;

  g_return_if_fail (gst_mixer_message_is_mixer_message (message));
  g_return_if_fail (GST_MIXER_MESSAGE_HAS_TYPE (message, MUTE_TOGGLED));

  s = gst_message_get_structure (message);

  if (track) {
    const GValue *v = gst_structure_get_value (s, "track");

    g_return_if_fail (v != NULL);
    *track = (GstMixerTrack *) g_value_get_object (v);
    g_return_if_fail (GST_IS_MIXER_TRACK (*track));
  }

  if (mute)
    g_return_if_fail (gst_structure_get_boolean (s, "mute", mute));
}

/**
 * gst_mixer_message_parse_record_toggled:
 * @message: A record-toggled change notification message.
 * @track: Pointer to hold a GstMixerTrack object, or NULL.
 * @record: A pointer to a gboolean variable, or NULL.
 *
 * Extracts the contents of a record-toggled bus message. Reads
 * the GstMixerTrack that has changed, and the new value of the 
 * recording flag.
 *
 * The GstMixerTrack remains valid until the message is freed.
 *
 * Since: 0.10.14
 */
void
gst_mixer_message_parse_record_toggled (GstMessage * message,
    GstMixerTrack ** track, gboolean * record)
{
  const GstStructure *s;

  g_return_if_fail (gst_mixer_message_is_mixer_message (message));
  g_return_if_fail (GST_MIXER_MESSAGE_HAS_TYPE (message, RECORD_TOGGLED));

  s = gst_message_get_structure (message);

  if (track) {
    const GValue *v = gst_structure_get_value (s, "track");

    g_return_if_fail (v != NULL);
    *track = (GstMixerTrack *) g_value_get_object (v);
    g_return_if_fail (GST_IS_MIXER_TRACK (*track));
  }

  if (record)
    g_return_if_fail (gst_structure_get_boolean (s, "record", record));
}

/**
 * gst_mixer_message_parse_volume_changed:
 * @message: A volume-changed change notification message.
 * @track: Pointer to hold a GstMixerTrack object, or NULL.
 * @volumes: A pointer to receive an array of gint values, or NULL.
 * @num_channels: Result location to receive the number of channels, or NULL.
 *
 * Parses a volume-changed notification message and extracts the track object
 * it refers to, as well as an array of volumes and the size of the volumes array.
 *
 * The track object remains valid until the message is freed.
 *
 * The caller must free the array returned in the volumes parameter using g_free
 * when they are done with it.
 *
 * Since: 0.10.14
 */
void
gst_mixer_message_parse_volume_changed (GstMessage * message,
    GstMixerTrack ** track, gint ** volumes, gint * num_channels)
{
  const GstStructure *s;

  g_return_if_fail (gst_mixer_message_is_mixer_message (message));
  g_return_if_fail (GST_MIXER_MESSAGE_HAS_TYPE (message, VOLUME_CHANGED));

  s = gst_message_get_structure (message);

  if (track) {
    const GValue *v = gst_structure_get_value (s, "track");

    g_return_if_fail (v != NULL);
    *track = (GstMixerTrack *) g_value_get_object (v);
    g_return_if_fail (GST_IS_MIXER_TRACK (*track));
  }

  if (volumes || num_channels) {
    gint n_chans, i;
    const GValue *v = gst_structure_get_value (s, "volumes");

    g_return_if_fail (v != NULL);
    g_return_if_fail (GST_VALUE_HOLDS_ARRAY (v));

    n_chans = gst_value_array_get_size (v);
    if (num_channels)
      *num_channels = n_chans;

    if (volumes) {
      *volumes = g_new (gint, n_chans);
      for (i = 0; i < n_chans; i++) {
        const GValue *e = gst_value_array_get_value (v, i);

        g_return_if_fail (e != NULL && G_VALUE_HOLDS_INT (e));
        (*volumes)[i] = g_value_get_int (e);
      }
    }
  }
}

/**
 * gst_mixer_message_parse_option_changed:
 * @message: A volume-changed change notification message.
 * @options: Pointer to hold a GstMixerOptions object, or NULL.
 * @value: Result location to receive the new options value, or NULL.
 *
 * Extracts the GstMixerOptions and new value from a option-changed bus notification
 * message.
 *
 * The options and value returned remain valid until the message is freed.
 *
 * Since: 0.10.14
 */
void
gst_mixer_message_parse_option_changed (GstMessage * message,
    GstMixerOptions ** options, const gchar ** value)
{
  const GstStructure *s;

  g_return_if_fail (gst_mixer_message_is_mixer_message (message));
  g_return_if_fail (GST_MIXER_MESSAGE_HAS_TYPE (message, OPTION_CHANGED));

  s = gst_message_get_structure (message);

  if (options) {
    const GValue *v = gst_structure_get_value (s, "options");

    g_return_if_fail (v != NULL);
    *options = (GstMixerOptions *) g_value_get_object (v);
    g_return_if_fail (GST_IS_MIXER_OPTIONS (*options));
  }

  if (value)
    *value = gst_structure_get_string (s, "value");
}

/**
 * gst_mixer_message_parse_options_list_changed:
 * @message: A volume-changed change notification message.
 * @options: Pointer to hold a GstMixerOptions object, or NULL.
 *
 * Extracts the GstMixerOptions whose value list has changed from an
 * options-list-changed bus notification message.
 *
 * The options object returned remains valid until the message is freed. You
 * do not need to unref it.
 *
 * Since: 0.10.18
 */
void
gst_mixer_message_parse_options_list_changed (GstMessage * message,
    GstMixerOptions ** options)
{
  const GstStructure *s;

  g_return_if_fail (gst_mixer_message_is_mixer_message (message));
  g_return_if_fail (GST_MIXER_MESSAGE_HAS_TYPE (message, OPTIONS_LIST_CHANGED));

  s = gst_message_get_structure (message);

  if (options) {
    const GValue *v = gst_structure_get_value (s, "options");

    g_return_if_fail (v != NULL);
    *options = (GstMixerOptions *) g_value_get_object (v);
    g_return_if_fail (GST_IS_MIXER_OPTIONS (*options));
  }
}
