/* GStreamer
 * Copyright (C) 2013 Stefan Sauer <ensonic@users.sf.net>
 *
 * gsttracer.c: tracer base class
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
 * SECTION:gsttracer
 * @title: GstTracer
 * @short_description: Tracing base class
 *
 * Tracing modules will subclass #GstTracer and register through
 * gst_tracer_register(). Modules can attach to various hook-types - see
 * gst_tracing_register_hook(). When invoked they receive hook specific
 * contextual data, which they must not modify.
 *
 * Since: 1.8
 */

#include "gst_private.h"
#include "gstenumtypes.h"
#include "gsttracer.h"
#include "gsttracerfactory.h"
#include "gsttracerutils.h"

GST_DEBUG_CATEGORY_EXTERN (tracer_debug);
#define GST_CAT_DEFAULT tracer_debug

/* tracing plugins base class */

enum
{
  PROP_0,
  PROP_PARAMS,
  PROP_LAST
};

static GParamSpec *properties[PROP_LAST];

static void gst_tracer_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec);
static void gst_tracer_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec);

struct _GstTracerPrivate
{
  gchar *params;
};

#define gst_tracer_parent_class parent_class
G_DEFINE_ABSTRACT_TYPE_WITH_PRIVATE (GstTracer, gst_tracer, GST_TYPE_OBJECT);

static void
gst_tracer_dispose (GObject * object)
{
  GstTracer *tracer = GST_TRACER (object);
  g_free (tracer->priv->params);
}

static void
gst_tracer_class_init (GstTracerClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gobject_class->set_property = gst_tracer_set_property;
  gobject_class->get_property = gst_tracer_get_property;
  gobject_class->dispose = gst_tracer_dispose;

  properties[PROP_PARAMS] =
      g_param_spec_string ("params", "Params", "Extra configuration parameters",
      NULL, G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS);

  g_object_class_install_properties (gobject_class, PROP_LAST, properties);
}

static void
gst_tracer_init (GstTracer * tracer)
{
  tracer->priv = gst_tracer_get_instance_private (tracer);
}

static void
gst_tracer_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstTracer *self = GST_TRACER_CAST (object);

  switch (prop_id) {
    case PROP_PARAMS:
      self->priv->params = g_value_dup_string (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_tracer_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  GstTracer *self = GST_TRACER_CAST (object);

  switch (prop_id) {
    case PROP_PARAMS:
      g_value_set_string (value, self->priv->params);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

/* tracing modules */

/**
 * gst_tracer_register:
 * @plugin: (allow-none): A #GstPlugin, or %NULL for a static typefind function
 * @name: The name for registering
 * @type: GType of tracer to register
 *
 * Create a new tracer-factory  capable of instantiating objects of the
 * @type and add the factory to @plugin.
 *
 * Returns: %TRUE, if the registering succeeded, %FALSE on error
 */
gboolean
gst_tracer_register (GstPlugin * plugin, const gchar * name, GType type)
{
  GstPluginFeature *existing_feature;
  GstRegistry *registry;
  GstTracerFactory *factory;

  g_return_val_if_fail (name != NULL, FALSE);
  g_return_val_if_fail (g_type_is_a (type, GST_TYPE_TRACER), FALSE);

  registry = gst_registry_get ();
  /* check if feature already exists, if it exists there is no need to update it
   * when the registry is getting updated, outdated plugins and all their
   * features are removed and readded.
   */
  existing_feature = gst_registry_lookup_feature (registry, name);
  if (existing_feature) {
    GST_DEBUG_OBJECT (registry, "update existing feature %p (%s)",
        existing_feature, name);
    factory = GST_TRACER_FACTORY_CAST (existing_feature);
    factory->type = type;
    existing_feature->loaded = TRUE;
    gst_object_unref (existing_feature);
    return TRUE;
  }

  factory = g_object_new (GST_TYPE_TRACER_FACTORY, NULL);
  GST_DEBUG_OBJECT (factory, "new tracer factory for %s", name);

  gst_plugin_feature_set_name (GST_PLUGIN_FEATURE_CAST (factory), name);
  gst_plugin_feature_set_rank (GST_PLUGIN_FEATURE_CAST (factory),
      GST_RANK_NONE);

  factory->type = type;
  GST_DEBUG_OBJECT (factory, "tracer factory for %u:%s",
      (guint) type, g_type_name (type));

  if (plugin && plugin->desc.name) {
    GST_PLUGIN_FEATURE_CAST (factory)->plugin_name = plugin->desc.name; /* interned string */
    GST_PLUGIN_FEATURE_CAST (factory)->plugin = plugin;
    g_object_add_weak_pointer ((GObject *) plugin,
        (gpointer *) & GST_PLUGIN_FEATURE_CAST (factory)->plugin);
  } else {
    GST_PLUGIN_FEATURE_CAST (factory)->plugin_name = "NULL";
    GST_PLUGIN_FEATURE_CAST (factory)->plugin = NULL;
  }
  GST_PLUGIN_FEATURE_CAST (factory)->loaded = TRUE;

  gst_registry_add_feature (gst_registry_get (),
      GST_PLUGIN_FEATURE_CAST (factory));

  return TRUE;
}
