/* GStreamer
 * Copyright (C) 2003-2004 Ronald Bultje <rbultje@ronald.bitfreak.net>
 * Copyright (C) 2005-2006 Tim-Philipp Müller <tim centricular net>
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
 * SECTION:gstaudiomixerutils
 * @short_description:  utility functions to find available audio mixers
 *                      from the plugin registry
 *
 * <refsect2>
 * <para>
 * Provides some utility functions to detect available audio mixers
 * on the system.
 * </para>
 * </refsect2>
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "mixerutils.h"

#include <gst/interfaces/propertyprobe.h>

#include <string.h>

static void
gst_audio_mixer_filter_do_filter (GstAudioMixerFilterFunc filter_func,
    GstElementFactory * factory,
    GstElement ** p_element, GList ** p_collection, gpointer user_data)
{
  /* so, the element is a mixer, let's see if the caller wants it */
  if (filter_func != NULL) {
    if (filter_func (GST_MIXER (*p_element), user_data) == TRUE) {
      *p_collection = g_list_prepend (*p_collection, *p_element);
      /* do not set state back to NULL here on purpose, caller
       * might want to keep the mixer open */
      *p_element = NULL;
    }
  } else {
    gst_element_set_state (*p_element, GST_STATE_NULL);
    *p_collection = g_list_prepend (*p_collection, *p_element);
    *p_element = NULL;
  }

  /* create new element for further probing if the old one was cleared */
  if (*p_element == NULL) {
    *p_element = gst_element_factory_create (factory, NULL);
  }
}

static gboolean
gst_audio_mixer_filter_check_element (GstElement * element)
{
  GstStateChangeReturn ret;

  /* open device (only then we can know for sure whether it is a mixer) */
  gst_element_set_state (element, GST_STATE_READY);
  ret = gst_element_get_state (element, NULL, NULL, 1 * GST_SECOND);
  if (ret != GST_STATE_CHANGE_SUCCESS) {
    GST_DEBUG ("could not open device / set element to READY");
    gst_element_set_state (element, GST_STATE_NULL);
    return FALSE;
  }

  /* is this device a mixer? */
  if (!GST_IS_MIXER (element)) {
    GST_DEBUG ("element is not a mixer");
    gst_element_set_state (element, GST_STATE_NULL);
    return FALSE;
  }

  /* any tracks? */
  if (!gst_mixer_list_tracks (GST_MIXER (element))) {
    GST_DEBUG ("element is a mixer, but has no tracks");
    gst_element_set_state (element, GST_STATE_NULL);
    return FALSE;
  }

  GST_DEBUG ("element is a mixer with mixer tracks");
  return TRUE;
}

static void
gst_audio_mixer_filter_probe_feature (GstAudioMixerFilterFunc filter_func,
    GstElementFactory * factory,
    GList ** p_collection, gboolean first, gpointer user_data)
{
  GstElement *element;

  GST_DEBUG ("probing %s ...", gst_element_factory_get_longname (factory));

  /* create element */
  element = gst_element_factory_create (factory, NULL);

  if (element == NULL) {
    GST_DEBUG ("could not create element from factory");
    return;
  }

  GST_DEBUG ("created element %s (%p)", GST_ELEMENT_NAME (element), element);

  if (GST_IS_PROPERTY_PROBE (element)) {
    GstPropertyProbe *probe;
    const GParamSpec *devspec;

    probe = GST_PROPERTY_PROBE (element);

    GST_DEBUG ("probing available devices ...");
    if ((devspec = gst_property_probe_get_property (probe, "device"))) {
      GValueArray *array;

      if ((array = gst_property_probe_probe_and_get_values (probe, devspec))) {
        guint n;

        GST_DEBUG ("there are %d available devices", array->n_values);

        /* set all devices and test for mixer */
        for (n = 0; n < array->n_values; n++) {
          GValue *device;

          /* set this device */
          device = g_value_array_get_nth (array, n);
          g_object_set_property (G_OBJECT (element), "device", device);

          GST_DEBUG ("trying device %s ..", g_value_get_string (device));

          if (gst_audio_mixer_filter_check_element (element)) {
            gst_audio_mixer_filter_do_filter (filter_func, factory, &element,
                p_collection, user_data);

            if (first && *p_collection != NULL) {
              GST_DEBUG ("Stopping after first found mixer, as requested");
              break;
            }
          }
        }
        g_value_array_free (array);
      }
    }
  } else {
    GST_DEBUG ("element does not support the property probe interface");

    if (gst_audio_mixer_filter_check_element (element)) {
      gst_audio_mixer_filter_do_filter (filter_func, factory, &element,
          p_collection, user_data);
    }
  }

  if (element) {
    gst_element_set_state (element, GST_STATE_NULL);
    gst_object_unref (element);
  }
}

static gint
element_factory_rank_compare_func (gconstpointer a, gconstpointer b)
{
  gint rank_a = gst_plugin_feature_get_rank (GST_PLUGIN_FEATURE (a));
  gint rank_b = gst_plugin_feature_get_rank (GST_PLUGIN_FEATURE (b));

  /* make order chosen in the end more determinable */
  if (rank_a == rank_b) {
    const gchar *name_a = GST_PLUGIN_FEATURE_NAME (GST_PLUGIN_FEATURE (a));
    const gchar *name_b = GST_PLUGIN_FEATURE_NAME (GST_PLUGIN_FEATURE (b));

    return g_ascii_strcasecmp (name_a, name_b);
  }

  return rank_b - rank_a;
}

/**
 * gst_audio_default_registry_mixer_filter:
 * @filter_func: filter function, or #NULL
 * @first: set to #TRUE if you only want the first suitable mixer element
 * @user_data: user data to pass to the filter function
 *
 * Utility function to find audio mixer elements.
 *
 * Will traverse the default plugin registry in order of plugin rank and
 * find usable audio mixer elements. The caller may optionally fine-tune
 * the selection by specifying a filter function.
 *
 * Returns: a #GList of audio mixer #GstElement<!-- -->s. You must free each
 *          element in the list by setting it to NULL state and calling
 *          gst_object_unref(). After that the list itself should be freed
 *          using g_list_free().
 *
 * Since: 0.10.2
 */
GList *
gst_audio_default_registry_mixer_filter (GstAudioMixerFilterFunc filter_func,
    gboolean first, gpointer data)
{
  GList *mixer_list = NULL;
  GList *feature_list;
  GList *walk;

  /* go through all elements of a certain class and check whether
   * they implement a mixer. If so, add it to the list. */
  feature_list = gst_registry_get_feature_list (gst_registry_get_default (),
      GST_TYPE_ELEMENT_FACTORY);

  feature_list = g_list_sort (feature_list, element_factory_rank_compare_func);

  for (walk = feature_list; walk != NULL; walk = walk->next) {
    GstElementFactory *factory;
    const gchar *klass;

    factory = GST_ELEMENT_FACTORY (walk->data);

    /* check category */
    klass = gst_element_factory_get_klass (factory);
    if (strcmp (klass, "Generic/Audio") == 0) {
      gst_audio_mixer_filter_probe_feature (filter_func, factory,
          &mixer_list, first, data);
    }

    if (first && mixer_list != NULL) {
      GST_DEBUG ("Stopping after first found mixer, as requested");
      break;
    }
  }

  gst_plugin_feature_list_free (feature_list);

  return g_list_reverse (mixer_list);
}
