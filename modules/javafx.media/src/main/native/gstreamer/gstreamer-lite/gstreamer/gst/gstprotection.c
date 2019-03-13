/* GStreamer
 * Copyright (C) <2013> YouView TV Ltd.
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
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 */

/**
 * SECTION:gstprotection
 * @title: GstProtection
 * @short_description: Functions and classes to support encrypted streams.
 *
 * The GstProtectionMeta class enables the information needed to decrypt a
 * #GstBuffer to be attached to that buffer.
 *
 * Typically, a demuxer element would attach GstProtectionMeta objects
 * to the buffers that it pushes downstream. The demuxer would parse the
 * protection information for a video/audio frame from its input data and use
 * this information to populate the #GstStructure @info field,
 * which is then encapsulated in a GstProtectionMeta object and attached to
 * the corresponding output buffer using the gst_buffer_add_protection_meta()
 * function. The information in this attached GstProtectionMeta would be
 * used by a downstream decrypter element to recover the original unencrypted
 * frame.
 *
 * Since: 1.6
 */

#include "gst_private.h"
#include "glib-compat-private.h"

#include "gstprotection.h"

#define GST_CAT_DEFAULT GST_CAT_PROTECTION

static gboolean gst_protection_meta_init (GstMeta * meta, gpointer params,
    GstBuffer * buffer);

static void gst_protection_meta_free (GstMeta * meta, GstBuffer * buffer);

static const gchar *gst_protection_factory_check (GstElementFactory * fact,
    const gchar ** system_identifiers);

GType
gst_protection_meta_api_get_type (void)
{
  static volatile GType type;
  static const gchar *tags[] = { NULL };

  if (g_once_init_enter (&type)) {
    GType _type = gst_meta_api_type_register ("GstProtectionMetaAPI", tags);
    g_once_init_leave (&type, _type);
  }
  return type;
}

static gboolean
gst_protection_meta_init (GstMeta * meta, gpointer params, GstBuffer * buffer)
{
  GstProtectionMeta *protection_meta = (GstProtectionMeta *) meta;

  protection_meta->info = NULL;

  return TRUE;
}

static void
gst_protection_meta_free (GstMeta * meta, GstBuffer * buffer)
{
  GstProtectionMeta *protection_meta = (GstProtectionMeta *) meta;

  if (protection_meta->info)
    gst_structure_free (protection_meta->info);
}

static gboolean
gst_protection_meta_transform (GstBuffer * transbuf, GstMeta * meta,
    GstBuffer * buffer, GQuark type, gpointer data)
{
  GstProtectionMeta *protection_meta = (GstProtectionMeta *) meta;

  if (GST_META_TRANSFORM_IS_COPY (type)) {
    GstMetaTransformCopy *copy = data;
    if (!copy->region) {
      /* only copy if the complete data is copied as well */
      gst_buffer_add_protection_meta (transbuf,
          gst_structure_copy (protection_meta->info));
    } else {
      return FALSE;
    }
  } else {
    /* transform type not supported */
    return FALSE;
  }
  return TRUE;
}

const GstMetaInfo *
gst_protection_meta_get_info (void)
{
  static const GstMetaInfo *protection_meta_info = NULL;

  if (g_once_init_enter ((GstMetaInfo **) & protection_meta_info)) {
    const GstMetaInfo *meta =
        gst_meta_register (GST_PROTECTION_META_API_TYPE, "GstProtectionMeta",
        sizeof (GstProtectionMeta), gst_protection_meta_init,
        gst_protection_meta_free, gst_protection_meta_transform);

    g_once_init_leave ((GstMetaInfo **) & protection_meta_info,
        (GstMetaInfo *) meta);
  }
  return protection_meta_info;
}

/**
 * gst_buffer_add_protection_meta:
 * @buffer: #GstBuffer holding an encrypted sample, to which protection
 *     metadata should be added.
 * @info: (transfer full): a #GstStructure holding cryptographic
 *     information relating to the sample contained in @buffer. This
 *     function takes ownership of @info.
 *
 * Attaches protection metadata to a #GstBuffer.
 *
 * Returns: (transfer none): a pointer to the added #GstProtectionMeta if successful; %NULL if
 * unsuccessful.
 *
 * Since: 1.6
 */
GstProtectionMeta *
gst_buffer_add_protection_meta (GstBuffer * buffer, GstStructure * info)
{
  GstProtectionMeta *meta;

  g_return_val_if_fail (GST_IS_BUFFER (buffer), NULL);
  g_return_val_if_fail (info != NULL, NULL);

  meta =
      (GstProtectionMeta *) gst_buffer_add_meta (buffer,
      GST_PROTECTION_META_INFO, NULL);

  meta->info = info;

  return meta;
}

/**
 * gst_protection_select_system:
 * @system_identifiers: (transfer none) (array zero-terminated=1): A null terminated array of strings
 * that contains the UUID values of each protection system that is to be
 * checked.
 *
 * Iterates the supplied list of UUIDs and checks the GstRegistry for
 * an element that supports one of the supplied UUIDs. If more than one
 * element matches, the system ID of the highest ranked element is selected.
 *
 * Returns: (transfer none) (nullable): One of the strings from
 * @system_identifiers that indicates the highest ranked element that
 * implements the protection system indicated by that system ID, or %NULL if no
 * element has been found.
 *
 * Since: 1.6
 */
const gchar *
gst_protection_select_system (const gchar ** system_identifiers)
{
  GList *decryptors, *walk;
  const gchar *retval = NULL;

  decryptors =
      gst_element_factory_list_get_elements (GST_ELEMENT_FACTORY_TYPE_DECRYPTOR,
      GST_RANK_MARGINAL);

  for (walk = decryptors; !retval && walk; walk = g_list_next (walk)) {
    GstElementFactory *fact = (GstElementFactory *) walk->data;

    retval = gst_protection_factory_check (fact, system_identifiers);
  }

  gst_plugin_feature_list_free (decryptors);

  return retval;
}

/**
 * gst_protection_filter_systems_by_available_decryptors:
 * @system_identifiers: (transfer none) (array zero-terminated=1):
 * A null terminated array of strings that contains the UUID values of each
 * protection system that is to be checked.
 *
 * Iterates the supplied list of UUIDs and checks the GstRegistry for
 * all the decryptors supporting one of the supplied UUIDs.
 *
 * Returns: (transfer full) (array zero-terminated=1) (nullable):
 * A null terminated array containing all
 * the @system_identifiers supported by the set of available decryptors, or
 * %NULL if no matches were found.
 *
 * Since: 1.14
 */
gchar **
gst_protection_filter_systems_by_available_decryptors (const gchar **
    system_identifiers)
{
  GList *decryptors, *walk;
  gchar **retval;
  guint i = 0, decryptors_number;

  decryptors =
      gst_element_factory_list_get_elements (GST_ELEMENT_FACTORY_TYPE_DECRYPTOR,
      GST_RANK_MARGINAL);

  decryptors_number = g_list_length (decryptors);

  GST_TRACE ("found %u decrytors", decryptors_number);

  if (decryptors_number == 0)
    return NULL;

  retval = g_new (gchar *, decryptors_number + 1);

  for (walk = decryptors; walk; walk = g_list_next (walk)) {
    GstElementFactory *fact = (GstElementFactory *) walk->data;
    const char *found_sys_id =
        gst_protection_factory_check (fact, system_identifiers);

    GST_DEBUG ("factory %s is valid for %s", GST_OBJECT_NAME (fact),
        found_sys_id);

    if (found_sys_id) {
      retval[i++] = g_strdup (found_sys_id);
    }
  }
  retval[i] = NULL;

  if (retval[0] == NULL) {
    g_free (retval);
    retval = NULL;
  }

  gst_plugin_feature_list_free (decryptors);

  return retval;
}

static const gchar *
gst_protection_factory_check (GstElementFactory * fact,
    const gchar ** system_identifiers)
{
  const GList *template, *walk;
  const gchar *retval = NULL;

  template = gst_element_factory_get_static_pad_templates (fact);
  for (walk = template; walk && !retval; walk = g_list_next (walk)) {
    GstStaticPadTemplate *templ = walk->data;
    GstCaps *caps = gst_static_pad_template_get_caps (templ);
    guint leng = gst_caps_get_size (caps);
    guint i, j;

    for (i = 0; !retval && i < leng; ++i) {
      GstStructure *st;

      st = gst_caps_get_structure (caps, i);
      if (gst_structure_has_field_typed (st,
              GST_PROTECTION_SYSTEM_ID_CAPS_FIELD, G_TYPE_STRING)) {
        const gchar *sys_id =
            gst_structure_get_string (st, GST_PROTECTION_SYSTEM_ID_CAPS_FIELD);
        GST_DEBUG ("Found decryptor that supports protection system %s",
            sys_id);
        for (j = 0; !retval && system_identifiers[j]; ++j) {
          GST_TRACE ("  compare with %s", system_identifiers[j]);
          if (g_ascii_strcasecmp (system_identifiers[j], sys_id) == 0) {
            GST_DEBUG ("  Selecting %s", system_identifiers[j]);
            retval = system_identifiers[j];
          }
        }
      }
    }
    gst_caps_unref (caps);
  }

  return retval;
}
