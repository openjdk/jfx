/* GStreamer encoding profiles library
 * Copyright (C) 2009-2010 Edward Hervey <edward.hervey@collabora.co.uk>
 *           (C) 2009-2010 Nokia Corporation
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
 * SECTION:encoding-profile
 * @short_description: Encoding profile library
 *
 * <refsect2>
 * <para>
 * Functions to create and handle encoding profiles.
 * </para>
 * <para>
 * Encoding profiles describe the media types and settings one wishes to use for
 * an encoding process. The top-level profiles are commonly
 * #GstEncodingContainerProfile(s) (which contains a user-readable name and
 * description along with which container format to use). These, in turn,
 * reference one or more #GstEncodingProfile(s) which indicate which encoding
 * format should be used on each individual streams.
 * </para>
 * <para>
 * #GstEncodingProfile(s) can be provided to the 'encodebin' element, which will take
 * care of selecting and setting up the required elements to produce an output stream
 * conforming to the specifications of the profile.
 * </para>
 * <para>
 * Unlike other systems, the encoding profiles do not specify which #GstElement to use
 * for the various encoding and muxing steps, but instead relies on specifying the format
 * one wishes to use.
 * </para>
 * <para>
 * Encoding profiles can be created at runtime by the application or loaded from (and saved
 * to) file using the #GstEncodingTarget API.
 * </para>
 * </refsect2>
 * <refsect2>
 * <title>Example: Creating a profile</title>
 * <para>
 * |[
 * #include <gst/pbutils/encoding-profile.h>
 * ...
 * GstEncodingProfile *
 * create_ogg_theora_profile(void)
 *{
 *  GstEncodingContainerProfile *prof;
 *  GstCaps *caps;
 *
 *  caps = gst_caps_from_string("application/ogg");
 *  prof = gst_encoding_container_profile_new("Ogg audio/video",
 *     "Standard OGG/THEORA/VORBIS",
 *     caps, NULL);
 *  gst_caps_unref (caps);
 *
 *  caps = gst_caps_from_string("video/x-theora");
 *  gst_encoding_container_profile_add_profile(prof,
 *       (GstEncodingProfile*) gst_encoding_video_profile_new(caps, NULL, NULL, 0));
 *  gst_caps_unref (caps);
 *
 *  caps = gst_caps_from_string("audio/x-vorbis");
 *  gst_encoding_container_profile_add_profile(prof,
 *       (GstEncodingProfile*) gst_encoding_audio_profile_new(caps, NULL, NULL, 0));
 *  gst_caps_unref (caps);
 *
 *  return (GstEncodingProfile*) prof;
 *}
 *
 *
 * ]|
 * </para>
 * </refsect2>
 * <refsect2>
 * <title>Example: Listing categories, targets and profiles</title>
 * <para>
 * |[
 * #include <gst/pbutils/encoding-profile.h>
 * ...
 * GstEncodingProfile *prof;
 * GList *categories, *tmpc;
 * GList *targets, *tmpt;
 * ...
 * categories = gst_encoding_target_list_available_categories();
 *
 * ... Show available categories to user ...
 *
 * for (tmpc = categories; tmpc; tmpc = tmpc->next) {
 *   gchar *category = (gchar *) tmpc->data;
 *
 *   ... and we can list all targets within that category ...
 *   
 *   targets = gst_encoding_target_list_all (category);
 *
 *   ... and show a list to our users ...
 *
 *   g_list_foreach (targets, (GFunc) gst_encoding_target_unref, NULL);
 *   g_list_free (targets);
 * }
 *
 * g_list_foreach (categories, (GFunc) g_free, NULL);
 * g_list_free (categories);
 *
 * ...
 * ]|
 * </para>
 * </refsect2>
 *
 * Since: 0.10.32
 */

#ifdef HAVE_CONFIG_H
#  include "config.h"
#endif

#include "encoding-profile.h"
#include "encoding-target.h"

/* GstEncodingProfile API */

struct _GstEncodingProfile
{
  GstMiniObject parent;

  /*< public > */
  gchar *name;
  gchar *description;
  GstCaps *format;
  gchar *preset;
  guint presence;
  GstCaps *restriction;
};

static void string_to_profile_transform (const GValue * src_value,
    GValue * dest_value);
static gboolean gst_encoding_profile_deserialize_valfunc (GValue * value,
    const gchar * s);

static void gst_encoding_profile_class_init (GstEncodingProfileClass * klass);
static gpointer gst_encoding_profile_parent_class = NULL;

static void
gst_encoding_profile_class_intern_init (gpointer klass)
{
  gst_encoding_profile_parent_class = g_type_class_peek_parent (klass);
  gst_encoding_profile_class_init ((GstEncodingProfileClass *) klass);
}

GType
gst_encoding_profile_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;

  if (g_once_init_enter (&g_define_type_id__volatile)) {
    GType g_define_type_id =
        g_type_register_static_simple (GST_TYPE_MINI_OBJECT,
        g_intern_static_string ("GstEncodingProfile"),
        sizeof (GstEncodingProfileClass),
        (GClassInitFunc) gst_encoding_profile_class_intern_init,
        sizeof (GstEncodingProfile),
        NULL,
        (GTypeFlags) 0);
    static GstValueTable gstvtable = {
      G_TYPE_NONE,
      (GstValueCompareFunc) NULL,
      (GstValueSerializeFunc) NULL,
      (GstValueDeserializeFunc) gst_encoding_profile_deserialize_valfunc
    };

    gstvtable.type = g_define_type_id;

    /* Register a STRING=>PROFILE GValueTransformFunc */
    g_value_register_transform_func (G_TYPE_STRING, g_define_type_id,
        string_to_profile_transform);
    /* Register gst-specific GValue functions */
    gst_value_register (&gstvtable);

    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

static void
gst_encoding_profile_finalize (GstEncodingProfile * prof)
{
  if (prof->name)
    g_free (prof->name);
  if (prof->format)
    gst_caps_unref (prof->format);
  if (prof->preset)
    g_free (prof->preset);
  if (prof->description)
    g_free (prof->description);
  if (prof->restriction)
    gst_caps_unref (prof->restriction);
}

static void
gst_encoding_profile_class_init (GstMiniObjectClass * klass)
{
  klass->finalize =
      (GstMiniObjectFinalizeFunction) gst_encoding_profile_finalize;
}

/**
 * gst_encoding_profile_get_name:
 * @profile: a #GstEncodingProfile
 *
 * Since: 0.10.32
 *
 * Returns: the name of the profile, can be %NULL.
 */
const gchar *
gst_encoding_profile_get_name (GstEncodingProfile * profile)
{
  return profile->name;
}

/**
 * gst_encoding_profile_get_description:
 * @profile: a #GstEncodingProfile
 *
 * Since: 0.10.32
 *
 * Returns: the description of the profile, can be %NULL.
 */
const gchar *
gst_encoding_profile_get_description (GstEncodingProfile * profile)
{
  return profile->description;
}

/**
 * gst_encoding_profile_get_format:
 * @profile: a #GstEncodingProfile
 *
 * Since: 0.10.32
 *
 * Returns: the #GstCaps corresponding to the media format used in the profile.
 */
const GstCaps *
gst_encoding_profile_get_format (GstEncodingProfile * profile)
{
  return profile->format;
}

/**
 * gst_encoding_profile_get_preset:
 * @profile: a #GstEncodingProfile
 *
 * Since: 0.10.32
 *
 * Returns: the name of the #GstPreset to be used in the profile.
 */
const gchar *
gst_encoding_profile_get_preset (GstEncodingProfile * profile)
{
  return profile->preset;
}

/**
 * gst_encoding_profile_get_presence:
 * @profile: a #GstEncodingProfile
 *
 * Since: 0.10.32
 *
 * Returns: The number of times the profile is used in its parent
 * container profile. If 0, it is not a mandatory stream.
 */
guint
gst_encoding_profile_get_presence (GstEncodingProfile * profile)
{
  return profile->presence;
}

/**
 * gst_encoding_profile_get_restriction:
 * @profile: a #GstEncodingProfile
 *
 * Since: 0.10.32
 *
 * Returns: The restriction #GstCaps to apply before the encoder
 * that will be used in the profile. The fields present in restriction caps are
 * properties of the raw stream (that is before encoding), such as height and
 * width for video and depth and sampling rate for audio. Does not apply to
 * #GstEncodingContainerProfile (since there is no corresponding raw stream).
 * Can be %NULL.
 */
const GstCaps *
gst_encoding_profile_get_restriction (GstEncodingProfile * profile)
{
  return profile->restriction;
}

/**
 * gst_encoding_profile_set_name:
 * @profile: a #GstEncodingProfile
 * @name: the name to set on the profile
 *
 * Set @name as the given name for the @profile. A copy of @name will be made
 * internally.
 *
 * Since: 0.10.32
 */
void
gst_encoding_profile_set_name (GstEncodingProfile * profile, const gchar * name)
{
  if (profile->name)
    g_free (profile->name);
  profile->name = g_strdup (name);
}

/**
 * gst_encoding_profile_set_description:
 * @profile: a #GstEncodingProfile
 * @description: the description to set on the profile
 *
 * Set @description as the given description for the @profile. A copy of @description will be made
 * internally.
 *
 * Since: 0.10.32
 */
void
gst_encoding_profile_set_description (GstEncodingProfile * profile,
    const gchar * description)
{
  if (profile->description)
    g_free (profile->description);
  profile->description = g_strdup (description);
}

/**
 * gst_encoding_profile_set_format:
 * @profile: a #GstEncodingProfile
 * @format: the media format to use in the profile.
 *
 * Sets the media format used in the profile.
 *
 * Since: 0.10.32
 */
void
gst_encoding_profile_set_format (GstEncodingProfile * profile, GstCaps * format)
{
  if (profile->format)
    gst_caps_unref (profile->format);
  profile->format = gst_caps_ref (format);
}

/**
 * gst_encoding_profile_set_preset:
 * @profile: a #GstEncodingProfile
 * @preset: the element preset to use
 *
 * Sets the preset to use for the profile.
 *
 * Since: 0.10.32
 */
void
gst_encoding_profile_set_preset (GstEncodingProfile * profile,
    const gchar * preset)
{
  if (profile->preset)
    g_free (profile->preset);
  profile->preset = g_strdup (preset);
}

/**
 * gst_encoding_profile_set_presence:
 * @profile: a #GstEncodingProfile
 * @presence: the number of time the profile can be used
 *
 * Set the number of time the profile is used in its parent
 * container profile. If 0, it is not a mandatory stream
 *
 * Since: 0.10.32
 */
void
gst_encoding_profile_set_presence (GstEncodingProfile * profile, guint presence)
{
  profile->presence = presence;
}

/**
 * gst_encoding_profile_set_restriction:
 * @profile: a #GstEncodingProfile
 * @restriction: the restriction to apply
 *
 * Set the restriction #GstCaps to apply before the encoder
 * that will be used in the profile. See gst_encoding_profile_set_restriction()
 * for more about restrictions. Does not apply to #GstEncodingContainerProfile.
 *
 * Since: 0.10.32
 */
void
gst_encoding_profile_set_restriction (GstEncodingProfile * profile,
    GstCaps * restriction)
{
  if (profile->restriction)
    gst_caps_unref (profile->restriction);
  profile->restriction = restriction;
}

/* Container profiles */

struct _GstEncodingContainerProfile
{
  GstEncodingProfile parent;

  GList *encodingprofiles;
};

G_DEFINE_TYPE (GstEncodingContainerProfile, gst_encoding_container_profile,
    GST_TYPE_ENCODING_PROFILE);

static void
gst_encoding_container_profile_init (GstEncodingContainerProfile * prof)
{
  /* Nothing to initialize */
}

static void
gst_encoding_container_profile_finalize (GstEncodingContainerProfile * prof)
{
  g_list_foreach (prof->encodingprofiles, (GFunc) gst_mini_object_unref, NULL);
  g_list_free (prof->encodingprofiles);

  GST_MINI_OBJECT_CLASS (gst_encoding_container_profile_parent_class)->finalize
      ((GstMiniObject *) prof);
}

static void
gst_encoding_container_profile_class_init (GstMiniObjectClass * klass)
{
  klass->finalize =
      (GstMiniObjectFinalizeFunction) gst_encoding_container_profile_finalize;
}

const GList *
gst_encoding_container_profile_get_profiles (GstEncodingContainerProfile *
    profile)
{
  return profile->encodingprofiles;
}

/* Video profiles */

struct _GstEncodingVideoProfile
{
  GstEncodingProfile parent;

  guint pass;
  gboolean variableframerate;
};

G_DEFINE_TYPE (GstEncodingVideoProfile, gst_encoding_video_profile,
    GST_TYPE_ENCODING_PROFILE);

static void
gst_encoding_video_profile_init (GstEncodingVideoProfile * prof)
{
  /* Nothing to initialize */
}

static void
gst_encoding_video_profile_class_init (GstMiniObjectClass * klass)
{
}

/**
 * gst_encoding_video_profile_get_pass:
 * @prof: a #GstEncodingVideoProfile
 *
 * Since: 0.10.32
 *
 * Returns: The pass number if this is part of a multi-pass profile. Starts at
 * 1 for multi-pass. 0 if this is not a multi-pass profile
 **/
guint
gst_encoding_video_profile_get_pass (GstEncodingVideoProfile * prof)
{
  return prof->pass;
}

/**
 * gst_encoding_video_profile_get_variableframerate:
 * @prof: a #GstEncodingVideoProfile
 *
 * Since: 0.10.32
 *
 * Returns: Whether non-constant video framerate is allowed for encoding.
 */
gboolean
gst_encoding_video_profile_get_variableframerate (GstEncodingVideoProfile *
    prof)
{
  return prof->variableframerate;
}

/**
 * gst_encoding_video_profile_set_pass:
 * @prof: a #GstEncodingVideoProfile
 * @pass: the pass number for this profile
 *
 * Sets the pass number of this video profile. The first pass profile should have
 * this value set to 1. If this video profile isn't part of a multi-pass profile,
 * you may set it to 0 (the default value).
 *
 * Since: 0.10.32
 */
void
gst_encoding_video_profile_set_pass (GstEncodingVideoProfile * prof, guint pass)
{
  prof->pass = pass;
}

/**
 * gst_encoding_video_profile_set_variableframerate:
 * @prof: a #GstEncodingVideoProfile
 * @variableframerate: a boolean
 *
 * If set to %TRUE, then the incoming streamm will be allowed to have non-constant
 * framerate. If set to %FALSE (default value), then the incoming stream will
 * be normalized by dropping/duplicating frames in order to produce a
 * constance framerate.
 *
 * Since: 0.10.32
 */
void
gst_encoding_video_profile_set_variableframerate (GstEncodingVideoProfile *
    prof, gboolean variableframerate)
{
  prof->variableframerate = variableframerate;
}

/* Audio profiles */

struct _GstEncodingAudioProfile
{
  GstEncodingProfile parent;
};

G_DEFINE_TYPE (GstEncodingAudioProfile, gst_encoding_audio_profile,
    GST_TYPE_ENCODING_PROFILE);

static void
gst_encoding_audio_profile_init (GstEncodingAudioProfile * prof)
{
  /* Nothing to initialize */
}

static void
gst_encoding_audio_profile_class_init (GstMiniObjectClass * klass)
{
}

static inline gboolean
_gst_caps_is_equal_safe (GstCaps * a, GstCaps * b)
{
  if (a == b)
    return TRUE;
  if ((a == NULL) || (b == NULL))
    return FALSE;
  return gst_caps_is_equal (a, b);
}

static gint
_compare_container_encoding_profiles (GstEncodingContainerProfile * ca,
    GstEncodingContainerProfile * cb)
{
  GList *tmp;

  if (g_list_length (ca->encodingprofiles) !=
      g_list_length (cb->encodingprofiles))
    return -1;

  for (tmp = ca->encodingprofiles; tmp; tmp = tmp->next) {
    GstEncodingProfile *prof = (GstEncodingProfile *) tmp->data;
    if (!gst_encoding_container_profile_contains_profile (ca, prof))
      return -1;
  }

  return 0;
}

static gint
_compare_encoding_profiles (const GstEncodingProfile * a,
    const GstEncodingProfile * b)
{
  if ((G_TYPE_FROM_INSTANCE (a) != G_TYPE_FROM_INSTANCE (b)) ||
      !_gst_caps_is_equal_safe (a->format, b->format) ||
      (g_strcmp0 (a->preset, b->preset) != 0) ||
      (g_strcmp0 (a->name, b->name) != 0) ||
      (g_strcmp0 (a->description, b->description) != 0))
    return -1;

  if (GST_IS_ENCODING_CONTAINER_PROFILE (a))
    return
        _compare_container_encoding_profiles (GST_ENCODING_CONTAINER_PROFILE
        (a), GST_ENCODING_CONTAINER_PROFILE (b));

  if (GST_IS_ENCODING_VIDEO_PROFILE (a)) {
    GstEncodingVideoProfile *va = (GstEncodingVideoProfile *) a;
    GstEncodingVideoProfile *vb = (GstEncodingVideoProfile *) b;

    if ((va->pass != vb->pass)
        || (va->variableframerate != vb->variableframerate))
      return -1;
  }

  return 0;
}

/**
 * gst_encoding_container_profile_contains_profile:
 * @container: a #GstEncodingContainerProfile
 * @profile: a #GstEncodingProfile
 *
 * Checks if @container contains a #GstEncodingProfile identical to
 * @profile.
 *
 * Since: 0.10.32
 *
 * Returns: %TRUE if @container contains a #GstEncodingProfile identical
 * to @profile, else %FALSE.
 */
gboolean
gst_encoding_container_profile_contains_profile (GstEncodingContainerProfile *
    container, GstEncodingProfile * profile)
{
  g_return_val_if_fail (GST_IS_ENCODING_CONTAINER_PROFILE (container), FALSE);
  g_return_val_if_fail (GST_IS_ENCODING_PROFILE (profile), FALSE);

  return (g_list_find_custom (container->encodingprofiles, profile,
          (GCompareFunc) _compare_encoding_profiles) != NULL);
}

/**
 * gst_encoding_container_profile_add_profile:
 * @container: the #GstEncodingContainerProfile to use
 * @profile: the #GstEncodingProfile to add.
 *
 * Add a #GstEncodingProfile to the list of profiles handled by @container.
 * 
 * No copy of @profile will be made, if you wish to use it elsewhere after this
 * method you should increment its reference count.
 *
 * Since: 0.10.32
 *
 * Returns: %TRUE if the @stream was properly added, else %FALSE.
 */
gboolean
gst_encoding_container_profile_add_profile (GstEncodingContainerProfile *
    container, GstEncodingProfile * profile)
{
  g_return_val_if_fail (GST_IS_ENCODING_CONTAINER_PROFILE (container), FALSE);
  g_return_val_if_fail (GST_IS_ENCODING_PROFILE (profile), FALSE);

  if (g_list_find_custom (container->encodingprofiles, profile,
          (GCompareFunc) _compare_encoding_profiles)) {
    GST_ERROR
        ("Encoding profile already contains an identical GstEncodingProfile");
    return FALSE;
  }

  container->encodingprofiles =
      g_list_append (container->encodingprofiles, profile);

  return TRUE;
}

static GstEncodingProfile *
common_creation (GType objtype, GstCaps * format, const gchar * preset,
    const gchar * name, const gchar * description, GstCaps * restriction,
    guint presence)
{
  GstEncodingProfile *prof;

  prof = (GstEncodingProfile *) gst_mini_object_new (objtype);

  if (name)
    prof->name = g_strdup (name);
  if (description)
    prof->description = g_strdup (description);
  if (preset)
    prof->preset = g_strdup (preset);
  if (format)
    prof->format = gst_caps_ref (format);
  if (restriction)
    prof->restriction = gst_caps_ref (restriction);
  prof->presence = presence;

  return prof;
}

/**
 * gst_encoding_container_profile_new:
 * @name: The name of the container profile, can be %NULL
 * @description: The description of the container profile, can be %NULL
 * @format: The format to use for this profile
 * @preset: The preset to use for this profile
 *
 * Creates a new #GstEncodingContainerProfile.
 *
 * Since: 0.10.32
 *
 * Returns: The newly created #GstEncodingContainerProfile.
 */
GstEncodingContainerProfile *
gst_encoding_container_profile_new (const gchar * name,
    const gchar * description, GstCaps * format, const gchar * preset)
{
  g_return_val_if_fail (GST_IS_CAPS (format), NULL);

  return (GstEncodingContainerProfile *)
      common_creation (GST_TYPE_ENCODING_CONTAINER_PROFILE, format, preset,
      name, description, NULL, 0);
}

/**
 * gst_encoding_video_profile_new:
 * @format: the #GstCaps
 * @preset: the preset(s) to use on the encoder, can be #NULL
 * @restriction: the #GstCaps used to restrict the input to the encoder, can be
 * NULL. See gst_encoding_profile_get_restriction() for more details.
 * @presence: the number of time this stream must be used. 0 means any number of
 *  times (including never)
 *
 * Creates a new #GstEncodingVideoProfile
 *
 * All provided allocatable arguments will be internally copied, so can be
 * safely freed/unreferenced after calling this method.
 *
 * If you wish to control the pass number (in case of multi-pass scenarios),
 * please refer to the gst_encoding_video_profile_set_pass() documentation.
 *
 * If you wish to use/force a constant framerate please refer to the
 * gst_encoding_video_profile_set_variableframerate() documentation.
 *
 * Since: 0.10.32
 *
 * Returns: the newly created #GstEncodingVideoProfile.
 */
GstEncodingVideoProfile *
gst_encoding_video_profile_new (GstCaps * format, const gchar * preset,
    GstCaps * restriction, guint presence)
{
  return (GstEncodingVideoProfile *)
      common_creation (GST_TYPE_ENCODING_VIDEO_PROFILE, format, preset, NULL,
      NULL, restriction, presence);
}

/**
 * gst_encoding_audio_profile_new:
 * @format: the #GstCaps
 * @preset: the preset(s) to use on the encoder, can be #NULL
 * @restriction: the #GstCaps used to restrict the input to the encoder, can be
 * NULL. See gst_encoding_profile_get_restriction() for more details.
 * @presence: the number of time this stream must be used. 0 means any number of
 *  times (including never)
 *
 * Creates a new #GstEncodingAudioProfile
 *
 * All provided allocatable arguments will be internally copied, so can be
 * safely freed/unreferenced after calling this method.
 *
 * Since: 0.10.32
 *
 * Returns: the newly created #GstEncodingAudioProfile.
 */
GstEncodingAudioProfile *
gst_encoding_audio_profile_new (GstCaps * format, const gchar * preset,
    GstCaps * restriction, guint presence)
{
  return (GstEncodingAudioProfile *)
      common_creation (GST_TYPE_ENCODING_AUDIO_PROFILE, format, preset, NULL,
      NULL, restriction, presence);
}


/**
 * gst_encoding_profile_is_equal:
 * @a: a #GstEncodingProfile
 * @b: a #GstEncodingProfile
 *
 * Checks whether the two #GstEncodingProfile are equal
 *
 * Since: 0.10.32
 *
 * Returns: %TRUE if @a and @b are equal, else %FALSE.
 */
gboolean
gst_encoding_profile_is_equal (GstEncodingProfile * a, GstEncodingProfile * b)
{
  return (_compare_encoding_profiles (a, b) == 0);
}


/**
 * gst_encoding_profile_get_input_caps:
 * @profile: a #GstEncodingProfile
 *
 * Computes the full output caps that this @profile will be able to consume.
 *
 * Since: 0.10.32
 *
 * Returns: The full caps the given @profile can consume. Call gst_caps_unref()
 * when you are done with the caps.
 */
GstCaps *
gst_encoding_profile_get_input_caps (GstEncodingProfile * profile)
{
  GstCaps *out, *tmp;
  GList *ltmp;
  GstStructure *st, *outst;
  GQuark out_name;
  guint i, len;
  const GstCaps *fcaps;

  if (GST_IS_ENCODING_CONTAINER_PROFILE (profile)) {
    GstCaps *res = gst_caps_new_empty ();

    for (ltmp = GST_ENCODING_CONTAINER_PROFILE (profile)->encodingprofiles;
        ltmp; ltmp = ltmp->next) {
      GstEncodingProfile *sprof = (GstEncodingProfile *) ltmp->data;
      gst_caps_merge (res, gst_encoding_profile_get_input_caps (sprof));
    }
    return res;
  }

  fcaps = profile->format;

  /* fast-path */
  if ((profile->restriction == NULL) || gst_caps_is_any (profile->restriction))
    return gst_caps_copy (fcaps);

  /* Combine the format with the restriction caps */
  outst = gst_caps_get_structure (fcaps, 0);
  out_name = gst_structure_get_name_id (outst);
  tmp = gst_caps_new_empty ();
  len = gst_caps_get_size (profile->restriction);

  for (i = 0; i < len; i++) {
    st = gst_structure_copy (gst_caps_get_structure (profile->restriction, i));
    st->name = out_name;
    gst_caps_append_structure (tmp, st);
  }

  out = gst_caps_intersect (tmp, fcaps);
  gst_caps_unref (tmp);

  return out;
}

/**
 * gst_encoding_profile_get_type_nick:
 * @profile: a #GstEncodingProfile
 *
 * Since: 0.10.32
 *
 * Returns: the human-readable name of the type of @profile.
 */
const gchar *
gst_encoding_profile_get_type_nick (GstEncodingProfile * profile)
{
  if (GST_IS_ENCODING_CONTAINER_PROFILE (profile))
    return "container";
  if (GST_IS_ENCODING_VIDEO_PROFILE (profile))
    return "video";
  if (GST_IS_ENCODING_AUDIO_PROFILE (profile))
    return "audio";
  return NULL;
}

/**
 * gst_encoding_profile_find:
 * @targetname: (transfer none): The name of the target
 * @profilename: (transfer none): The name of the profile
 * @category: (transfer none) (allow-none): The target category. Can be %NULL
 *
 * Find the #GstEncodingProfile with the specified name and category.
 *
 * Returns: (transfer full): The matching #GstEncodingProfile or %NULL.
 *
 * Since: 0.10.32
 */
GstEncodingProfile *
gst_encoding_profile_find (const gchar * targetname, const gchar * profilename,
    const gchar * category)
{
  GstEncodingProfile *res = NULL;
  GstEncodingTarget *target;

  g_return_val_if_fail (targetname != NULL, NULL);
  g_return_val_if_fail (profilename != NULL, NULL);

  /* FIXME : how do we handle profiles named the same in several
   * categories but of which only one has the required profile ? */
  target = gst_encoding_target_load (targetname, category, NULL);
  if (target) {
    res = gst_encoding_target_get_profile (target, profilename);
    gst_encoding_target_unref (target);
  }

  return res;
}

static GstEncodingProfile *
combo_search (const gchar * pname)
{
  GstEncodingProfile *res;
  gchar **split;

  /* Splitup */
  split = g_strsplit (pname, "/", 2);
  if (g_strv_length (split) != 2)
    return NULL;

  res = gst_encoding_profile_find (split[0], split[1], NULL);

  g_strfreev (split);

  return res;
}

/* GValue transform function */
static void
string_to_profile_transform (const GValue * src_value, GValue * dest_value)
{
  const gchar *profilename;
  GstEncodingProfile *profile;

  profilename = g_value_get_string (src_value);

  profile = combo_search (profilename);

  if (profile)
    gst_value_take_mini_object (dest_value, (GstMiniObject *) profile);
}

static gboolean
gst_encoding_profile_deserialize_valfunc (GValue * value, const gchar * s)
{
  GstEncodingProfile *profile;

  profile = combo_search (s);

  if (profile) {
    gst_value_take_mini_object (value, (GstMiniObject *) profile);
    return TRUE;
  }

  return FALSE;
}
