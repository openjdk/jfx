/* GStreamer
 * Copyright (C) 2015 Jan Schmidt <jan@centricular.com>
 *
 * gstdynamictypefactory.c: Implementation of GstDynamicTypeFactory
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
 * SECTION:gstdynamictypefactory
 * @title: GstDynamicTypeFactory
 * @short_description: Represents a registered dynamically loadable GType
 * @see_also: #GstPlugin, #GstPluginFeature.
 *
 * #GstDynamicTypeFactory is used to represent a type that can be
 * automatically loaded the first time it is used. For example,
 * a non-standard type for use in caps fields.
 *
 * In general, applications and plugins don't need to use the factory
 * beyond registering the type in a plugin init function. Once that is
 * done, the type is stored in the registry, and ready as soon as the
 * registry is loaded.
 *
 * ## Registering a type for dynamic loading
 *
 * |[<!-- language="C" -->
 *
 * static gboolean
 * plugin_init (GstPlugin * plugin)
 * {
 *   return gst_dynamic_type_register (plugin, GST_TYPE_CUSTOM_CAPS_FIELD);
 * }
 * ]|
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "gst_private.h"

#include <glib-object.h>

#include "gst.h"

#include "glib-compat-private.h"


GST_DEBUG_CATEGORY_STATIC (dynamic_type_factory_debug);
#define GST_CAT_DEFAULT dynamic_type_factory_debug

#define _do_init \
{ \
  GST_DEBUG_CATEGORY_INIT (dynamic_type_factory_debug, \
      "GST_DYNAMIC_TYPE_FACTORY", GST_DEBUG_BOLD, \
      "dynamic type factories allow automatically loading a type from a plugin"); \
}

G_DEFINE_TYPE_WITH_CODE (GstDynamicTypeFactory, gst_dynamic_type_factory,
    GST_TYPE_PLUGIN_FEATURE, _do_init);

static void
gst_dynamic_type_factory_class_init (GstDynamicTypeFactoryClass * klass)
{
}

static void
gst_dynamic_type_factory_init (GstDynamicTypeFactory * factory)
{
}

static GstDynamicTypeFactory *
gst_dynamic_type_factory_find (const gchar * name)
{
  GstPluginFeature *feature;

  g_return_val_if_fail (name != NULL, NULL);

  feature = gst_registry_find_feature (gst_registry_get (), name,
      GST_TYPE_DYNAMIC_TYPE_FACTORY);
  if (feature)
    return GST_DYNAMIC_TYPE_FACTORY (feature);

  return NULL;
}

GType
gst_dynamic_type_factory_load (const gchar * factoryname)
{
  GstDynamicTypeFactory *factory = gst_dynamic_type_factory_find (factoryname);

  /* Called with a non-dynamic or unregistered type? */
  if (factory == NULL)
    return FALSE;

  factory =
      GST_DYNAMIC_TYPE_FACTORY (gst_plugin_feature_load (GST_PLUGIN_FEATURE
          (factory)));
  if (factory == NULL)
    return 0;

  GST_DEBUG_OBJECT (factory, "Loaded type %s", factoryname);

  return factory->type;
}

static GstDynamicTypeFactory *
gst_dynamic_type_factory_create (GstRegistry * registry,
    GstPlugin * plugin, const gchar * name)
{
  GstDynamicTypeFactory *factory;

  factory = g_object_new (GST_TYPE_DYNAMIC_TYPE_FACTORY, NULL);
  gst_plugin_feature_set_name (GST_PLUGIN_FEATURE_CAST (factory), name);
  GST_LOG_OBJECT (factory, "Created new dynamictypefactory for type %s", name);

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

gboolean
gst_dynamic_type_register (GstPlugin * plugin, GType dyn_type)
{
  GstDynamicTypeFactory *factory;
  const gchar *name;
  GstPluginFeature *existing_feature;
  GstRegistry *registry;

  name = g_type_name (dyn_type);
  g_return_val_if_fail (name != NULL, FALSE);

  registry = gst_registry_get ();

  /* check if feature already exists, if it exists there is no need to
   * update it for this method of dynamic type */
  existing_feature = gst_registry_lookup_feature (registry, name);
  if (existing_feature) {
    GST_DEBUG_OBJECT (registry, "update existing feature %p (%s)",
        existing_feature, name);
    existing_feature->loaded = TRUE;
    GST_DYNAMIC_TYPE_FACTORY (existing_feature)->type = dyn_type;
    gst_object_unref (existing_feature);
    return TRUE;
  }

  factory = gst_dynamic_type_factory_create (registry, plugin, name);
  factory->type = dyn_type;

  gst_registry_add_feature (registry, GST_PLUGIN_FEATURE_CAST (factory));

  return TRUE;
}
