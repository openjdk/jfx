/* GStreamer
 * Copyright (C) 2025 Netflix Inc.
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
 * SECTION:gstmetafactory
 * @title: GstMetaFactory
 * @short_description: Register dynamically loadable #GstMetaInfo
 * @see_also: #GstPlugin, #GstPluginFeature.
 *
 * Register a #GstMetaInfo that can be automatically loaded the first time it is
 * used.
 *
 * In general, applications and plugins don't need to use the factory
 * beyond registering the meta in a plugin init function. Once that is
 * done, the meta is stored in the registry, and ready as soon as the
 * registry is loaded.
 *
 * ## Registering a meta for dynamic loading
 *
 * |[<!-- language="C" -->
 *
 * static gboolean
 * plugin_init (GstPlugin * plugin)
 * {
 *   return gst_meta_factory_register (plugin, my_meta_get_info());
 * }
 * ]|
 * Since: 1.28
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "gst_private.h"
#include "gstmetafactory.h"

GST_DEBUG_CATEGORY_STATIC (meta_factory_debug);
#define GST_CAT_DEFAULT meta_factory_debug

struct _GstMetaFactory
{
  GstPluginFeature parent;
  const GstMetaInfo *meta_info;
};

struct _GstMetaFactoryClass
{
  GstPluginFeatureClass parent_class;
};

G_DEFINE_TYPE (GstMetaFactory, gst_meta_factory, GST_TYPE_PLUGIN_FEATURE);

static void
gst_meta_factory_class_init (GstMetaFactoryClass * klass)
{
  GST_DEBUG_CATEGORY_INIT (meta_factory_debug,
      "GST_META_FACTORY", GST_DEBUG_BOLD,
      "Meta factories allow automatically loading a GstMetaInfo from a plugin");
}

static void
gst_meta_factory_init (GstMetaFactory * factory)
{
}

static GstMetaFactory *
gst_meta_factory_find (const gchar * name)
{
  GstPluginFeature *feature;

  g_return_val_if_fail (name != NULL, NULL);

  feature = gst_registry_find_feature (gst_registry_get (), name,
      GST_TYPE_META_FACTORY);
  if (feature)
    return GST_META_FACTORY (feature);

  return NULL;
}

/**
 * gst_meta_factory_load:
 * @factoryname: The name of the #GstMetaInfo to load
 *
 * Loads a previously registered #GstMetaInfo from the registry.

 * Returns: A #GstMetaInfo or NULL if not found
 * Since: 1.28
 */
const GstMetaInfo *
gst_meta_factory_load (const gchar * factoryname)
{
  GstMetaFactory *factory = gst_meta_factory_find (factoryname);

  /* Called with a non-dynamic or unregistered type? */
  if (factory == NULL)
    return NULL;

  factory =
      GST_META_FACTORY (gst_plugin_feature_load (GST_PLUGIN_FEATURE (factory)));
  if (factory == NULL)
    return NULL;

  GST_DEBUG_OBJECT (factory, "Loaded type %s", factoryname);

  return factory->meta_info;
}

static GstMetaFactory *
gst_meta_factory_create (GstRegistry * registry,
    GstPlugin * plugin, const gchar * name)
{
  GstMetaFactory *factory;

  factory = g_object_new (GST_TYPE_META_FACTORY, NULL);
  gst_plugin_feature_set_name (GST_PLUGIN_FEATURE_CAST (factory), name);
  GST_LOG_OBJECT (factory, "Created new metafactory for type %s", name);

  if (plugin && plugin->desc.name) {
    GST_PLUGIN_FEATURE_CAST (factory)->plugin_name = plugin->desc.name;
    GST_PLUGIN_FEATURE_CAST (factory)->plugin = plugin;
    g_object_add_weak_pointer ((GObject *) plugin,
        (gpointer *) & GST_PLUGIN_FEATURE_CAST (factory)->plugin);
  } else {
    GST_PLUGIN_FEATURE_CAST (factory)->plugin_name = "NULL";
    GST_PLUGIN_FEATURE_CAST (factory)->plugin = NULL;
  }
  GST_PLUGIN_FEATURE_CAST (factory)->loaded = TRUE;

  return factory;
}

/**
 * gst_meta_factory_register:
 * @plugin: The #GstPlugin to register @meta_info for
 * @meta_info: The #GstMetaInfo to register
 *
 * Registers a new #GstMetaInfo in the registry
 *
 * Since: 1.28
 */
gboolean
gst_meta_factory_register (GstPlugin * plugin, const GstMetaInfo * meta_info)
{
  GstMetaFactory *factory;
  const gchar *name;
  GstPluginFeature *existing_feature;
  GstRegistry *registry;

  /* This is the name used by gst_meta_serialize() */
  name = g_type_name (meta_info->type);
  g_assert (name != NULL);

  registry = gst_registry_get ();

  /* check if feature already exists, if it exists there is no need to
   * update it for this method of dynamic type */
  existing_feature = gst_registry_lookup_feature (registry, name);
  if (existing_feature) {
    GST_DEBUG_OBJECT (registry, "update existing feature %p (%s)",
        existing_feature, name);
    existing_feature->loaded = TRUE;
    GST_META_FACTORY (existing_feature)->meta_info = meta_info;
    gst_object_unref (existing_feature);
    return TRUE;
  }

  factory = gst_meta_factory_create (registry, plugin, name);
  factory->meta_info = meta_info;

  gst_registry_add_feature (registry, GST_PLUGIN_FEATURE_CAST (factory));

  return TRUE;
}
