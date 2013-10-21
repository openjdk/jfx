/* GStreamer
 * Copyright (C) 2001 RidgeRun (http://www.ridgerun.com/)
 * Written by Erik Walthinsen <omega@ridgerun.com>
 *
 * gstindexfactory.c: Index for mappings and other data
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
 * SECTION:gstindexfactory
 * @short_description: Create GstIndexes from a factory
 * @see_also: #GstIndex
 *
 * GstIndexFactory is used to dynamically create GstIndex implementations.
 */


#include "gst_private.h"

#include "gstinfo.h"
#include "gstindex.h"
#include "gstindexfactory.h"
#include "gstmarshal.h"
#include "gstregistry.h"

static void gst_index_factory_finalize (GObject * object);

static GstPluginFeatureClass *factory_parent_class = NULL;

/* static guint gst_index_factory_signals[LAST_SIGNAL] = { 0 }; */
G_DEFINE_TYPE (GstIndexFactory, gst_index_factory, GST_TYPE_PLUGIN_FEATURE);

static void
gst_index_factory_class_init (GstIndexFactoryClass * klass)
{
  GObjectClass *gobject_class = (GObjectClass *) klass;

  factory_parent_class = g_type_class_peek_parent (klass);

  gobject_class->finalize = gst_index_factory_finalize;
}

static void
gst_index_factory_init (GstIndexFactory * factory)
{
}

static void
gst_index_factory_finalize (GObject * object)
{
  GstIndexFactory *factory = GST_INDEX_FACTORY (object);

  g_free (factory->longdesc);

  G_OBJECT_CLASS (factory_parent_class)->finalize (object);

}

/**
 * gst_index_factory_new:
 * @name: name of indexfactory to create
 * @longdesc: long description of indexfactory to create
 * @type: the GType of the GstIndex element of this factory
 *
 * Create a new indexfactory with the given parameters
 *
 * Returns: (transfer full): a new #GstIndexFactory.
 */
GstIndexFactory *
gst_index_factory_new (const gchar * name, const gchar * longdesc, GType type)
{
  GstIndexFactory *factory;

  g_return_val_if_fail (name != NULL, NULL);
  factory = GST_INDEX_FACTORY (g_object_newv (GST_TYPE_INDEX_FACTORY, 0, NULL));

  GST_PLUGIN_FEATURE_NAME (factory) = g_strdup (name);
  if (factory->longdesc)
    g_free (factory->longdesc);
  factory->longdesc = g_strdup (longdesc);
  factory->type = type;

  return factory;
}

/**
 * gst_index_factory_destroy:
 * @factory: factory to destroy
 *
 * Removes the index from the global list.
 */
void
gst_index_factory_destroy (GstIndexFactory * factory)
{
  g_return_if_fail (factory != NULL);

  /* we don't free the struct bacause someone might  have a handle to it.. */
  /* FIXME: gst_index_factory_destroy */
}

/**
 * gst_index_factory_find:
 * @name: name of indexfactory to find
 *
 * Search for an indexfactory of the given name.
 *
 * Returns: (transfer full): #GstIndexFactory if found, NULL otherwise
 */
GstIndexFactory *
gst_index_factory_find (const gchar * name)
{
  GstPluginFeature *feature;

  g_return_val_if_fail (name != NULL, NULL);

  GST_DEBUG ("gstindex: find \"%s\"", name);

  feature = gst_registry_find_feature (gst_registry_get_default (), name,
      GST_TYPE_INDEX_FACTORY);
  if (feature)
    return GST_INDEX_FACTORY (feature);

  return NULL;
}

/**
 * gst_index_factory_create:
 * @factory: the factory used to create the instance
 *
 * Create a new #GstIndex instance from the
 * given indexfactory.
 *
 * Returns: (transfer full): a new #GstIndex instance.
 */
GstIndex *
gst_index_factory_create (GstIndexFactory * factory)
{
  GstIndexFactory *newfactory;
  GstIndex *new = NULL;

  g_return_val_if_fail (factory != NULL, NULL);

  newfactory =
      GST_INDEX_FACTORY (gst_plugin_feature_load (GST_PLUGIN_FEATURE
          (factory)));
  if (newfactory == NULL)
    return NULL;

  new = GST_INDEX (g_object_newv (newfactory->type, 0, NULL));

  gst_object_unref (newfactory);

  return new;
}

/**
 * gst_index_factory_make:
 * @name: the name of the factory used to create the instance
 *
 * Create a new #GstIndex instance from the
 * indexfactory with the given name.
 *
 * Returns: (transfer full): a new #GstIndex instance.
 */
GstIndex *
gst_index_factory_make (const gchar * name)
{
  GstIndexFactory *factory;
  GstIndex *index;

  g_return_val_if_fail (name != NULL, NULL);

  factory = gst_index_factory_find (name);

  if (factory == NULL)
    goto no_factory;

  index = gst_index_factory_create (factory);

  if (index == NULL)
    goto create_failed;

  gst_object_unref (factory);
  return index;

  /* ERRORS */
no_factory:
  {
    GST_INFO ("no such index factory \"%s\"!", name);
    return NULL;
  }
create_failed:
  {
    GST_INFO_OBJECT (factory, "couldn't create instance!");
    gst_object_unref (factory);
    return NULL;
  }
}
