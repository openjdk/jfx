/* GStreamer PropertyProbe
 * Copyright (C) 2003 David Schleef <ds@schleef.org>
 *
 * property_probe.c: property_probe design virtual class function wrappers
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
 * SECTION:gstpropertyprobe
 * @short_description: Interface for probing possible property values
 *
 * The property probe is a way to autodetect allowed values for a GObject
 * property. It's primary use is to autodetect device-names in several elements.
 *
 * The interface is implemented by many hardware sources and sinks.
 */
#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <string.h>

#include "propertyprobe.h"

enum
{
  SIGNAL_PROBE_NEEDED,
  LAST_SIGNAL
};

static void gst_property_probe_iface_init (GstPropertyProbeInterface * iface);

static guint gst_property_probe_signals[LAST_SIGNAL] = { 0 };

GType
gst_property_probe_get_type (void)
{
  static GType gst_property_probe_type = 0;

  if (!gst_property_probe_type) {
    static const GTypeInfo gst_property_probe_info = {
      sizeof (GstPropertyProbeInterface),
      (GBaseInitFunc) gst_property_probe_iface_init,
      NULL,
      NULL,
      NULL,
      NULL,
      0,
      0,
      NULL,
    };

    gst_property_probe_type =
        g_type_register_static (G_TYPE_INTERFACE,
        "GstPropertyProbe", &gst_property_probe_info, 0);
  }

  return gst_property_probe_type;
}

static void
gst_property_probe_iface_init (GstPropertyProbeInterface * iface)
{
  static gboolean initialized = FALSE;

  if (!initialized) {
    /**
     * GstPropertyProbe::probe-needed
     * @pspec: #GParamSpec that needs a probe
     *
     */
    /* FIXME:
     * what is the purpose of this signal, I can't find any usage of it
     * according to proto n *.h, it should be g_cclosure_marshal_VOID__PARAM
     */
    gst_property_probe_signals[SIGNAL_PROBE_NEEDED] =
        g_signal_new ("probe-needed", G_TYPE_FROM_CLASS (iface),
        G_SIGNAL_RUN_LAST, G_STRUCT_OFFSET (GstPropertyProbeInterface,
            probe_needed), NULL, NULL, g_cclosure_marshal_VOID__POINTER,
        G_TYPE_NONE, 1, G_TYPE_POINTER);
    initialized = TRUE;
  }

  /* default virtual functions */
  iface->get_properties = NULL;
  iface->get_values = NULL;
}

/**
 * gst_property_probe_get_properties:
 * @probe: the #GstPropertyProbe to get the properties for.
 *
 * Get a list of properties for which probing is supported.
 *
 * Returns: the list of properties for which probing is supported
 * by this element.
 */
const GList *
gst_property_probe_get_properties (GstPropertyProbe * probe)
{
  GstPropertyProbeInterface *iface;

  g_return_val_if_fail (probe != NULL, NULL);
  g_return_val_if_fail (GST_IS_PROPERTY_PROBE (probe), NULL);

  iface = GST_PROPERTY_PROBE_GET_IFACE (probe);

  if (iface->get_properties)
    return iface->get_properties (probe);

  return NULL;
}

/**
 * gst_property_probe_get_property:
 * @probe: the #GstPropertyProbe to get the properties for.
 * @name: name of the property.
 *
 * Get #GParamSpec for a property for which probing is supported.
 *
 * Returns: the #GParamSpec of %NULL.
 */
const GParamSpec *
gst_property_probe_get_property (GstPropertyProbe * probe, const gchar * name)
{
  const GList *pspecs;

  g_return_val_if_fail (probe != NULL, NULL);
  g_return_val_if_fail (GST_IS_PROPERTY_PROBE (probe), NULL);
  g_return_val_if_fail (name != NULL, NULL);

  pspecs = gst_property_probe_get_properties (probe);

  while (pspecs) {
    const GParamSpec *pspec = pspecs->data;

    if (pspec) {
      if (!strcmp (pspec->name, name))
        return pspec;
    } else {
      GST_WARNING_OBJECT (probe, "NULL paramspec in property probe list");
    }

    pspecs = pspecs->next;
  }

  return NULL;
}

/**
 * gst_property_probe_probe_property:
 * @probe: the #GstPropertyProbe to check.
 * @pspec: #GParamSpec of the property.
 *
 * Runs a probe on the property specified by @pspec
 */
void
gst_property_probe_probe_property (GstPropertyProbe * probe,
    const GParamSpec * pspec)
{
  GstPropertyProbeInterface *iface;

  g_return_if_fail (probe != NULL);
  g_return_if_fail (GST_IS_PROPERTY_PROBE (probe));
  g_return_if_fail (pspec != NULL);

  iface = GST_PROPERTY_PROBE_GET_IFACE (probe);

  if (iface->probe_property)
    iface->probe_property (probe, pspec->param_id, pspec);
}

/**
 * gst_property_probe_probe_property_name:
 * @probe: the #GstPropertyProbe to check.
 * @name: name of the property.
 *
 * Runs a probe on the property specified by @name.
 */
void
gst_property_probe_probe_property_name (GstPropertyProbe * probe,
    const gchar * name)
{
  const GParamSpec *pspec;

  g_return_if_fail (probe != NULL);
  g_return_if_fail (GST_IS_PROPERTY_PROBE (probe));
  g_return_if_fail (name != NULL);

  pspec = g_object_class_find_property (G_OBJECT_GET_CLASS (probe), name);
  if (!pspec) {
    g_warning ("No such property %s", name);
    return;
  }

  gst_property_probe_probe_property (probe, pspec);
}

/**
 * gst_property_probe_needs_probe:
 * @probe: the #GstPropertyProbe object to which the given property belongs.
 * @pspec: a #GParamSpec that identifies the property to check.
 *
 * Checks whether a property needs a probe. This might be because
 * the property wasn't initialized before, or because host setup
 * changed. This might be, for example, because a new device was
 * added, and thus device probing needs to be refreshed to display
 * the new device.
 *
 * Returns: TRUE if the property needs a new probe, FALSE if not.
 */
gboolean
gst_property_probe_needs_probe (GstPropertyProbe * probe,
    const GParamSpec * pspec)
{
  GstPropertyProbeInterface *iface;

  g_return_val_if_fail (probe != NULL, FALSE);
  g_return_val_if_fail (GST_IS_PROPERTY_PROBE (probe), FALSE);
  g_return_val_if_fail (pspec != NULL, FALSE);

  iface = GST_PROPERTY_PROBE_GET_IFACE (probe);

  if (iface->needs_probe)
    return iface->needs_probe (probe, pspec->param_id, pspec);

  return FALSE;
}

/**
 * gst_property_probe_needs_probe_name:
 * @probe: the #GstPropertyProbe object to which the given property belongs.
 * @name: the name of the property to check.
 *
 * Same as gst_property_probe_needs_probe ().
 *
 * Returns: TRUE if the property needs a new probe, FALSE if not.
 */
gboolean
gst_property_probe_needs_probe_name (GstPropertyProbe * probe,
    const gchar * name)
{
  const GParamSpec *pspec;

  g_return_val_if_fail (probe != NULL, FALSE);
  g_return_val_if_fail (GST_IS_PROPERTY_PROBE (probe), FALSE);
  g_return_val_if_fail (name != NULL, FALSE);

  pspec = g_object_class_find_property (G_OBJECT_GET_CLASS (probe), name);
  if (!pspec) {
    g_warning ("No such property %s", name);
    return FALSE;
  }

  return gst_property_probe_needs_probe (probe, pspec);
}

/**
 * gst_property_probe_get_values:
 * @probe: the #GstPropertyProbe object.
 * @pspec: the #GParamSpec property identifier.
 *
 * Gets the possible (probed) values for the given property,
 * requires the property to have been probed before.
 *
 * Returns: A list of valid values for the given property.
 */
GValueArray *
gst_property_probe_get_values (GstPropertyProbe * probe,
    const GParamSpec * pspec)
{
  GstPropertyProbeInterface *iface;

  g_return_val_if_fail (probe != NULL, NULL);
  g_return_val_if_fail (GST_IS_PROPERTY_PROBE (probe), NULL);
  g_return_val_if_fail (pspec != NULL, NULL);

  iface = GST_PROPERTY_PROBE_GET_IFACE (probe);

  if (iface->get_values)
    return iface->get_values (probe, pspec->param_id, pspec);

  return NULL;
}

/**
 * gst_property_probe_get_values_name:
 * @probe: the #GstPropertyProbe object.
 * @name: the name of the property to get values for.
 *
 * Same as gst_property_probe_get_values ().
 *
 * Returns: A list of valid values for the given property.
 */
GValueArray *
gst_property_probe_get_values_name (GstPropertyProbe * probe,
    const gchar * name)
{
  const GParamSpec *pspec;

  g_return_val_if_fail (probe != NULL, NULL);
  g_return_val_if_fail (GST_IS_PROPERTY_PROBE (probe), NULL);
  g_return_val_if_fail (name != NULL, NULL);

  pspec = g_object_class_find_property (G_OBJECT_GET_CLASS (probe), name);
  if (!pspec) {
    g_warning ("No such property %s", name);
    return NULL;
  }

  return gst_property_probe_get_values (probe, pspec);
}

/**
 * gst_property_probe_probe_and_get_values:
 * @probe: the #GstPropertyProbe object.
 * @pspec: The #GParamSpec property identifier.
 *
 * Check whether the given property requires a new probe. If so,
 * fo the probe. After that, retrieve a value list. Meant as a
 * utility function that wraps the above functions.
 *
 * Returns: the list of valid values for this property.
 */
GValueArray *
gst_property_probe_probe_and_get_values (GstPropertyProbe * probe,
    const GParamSpec * pspec)
{
  g_return_val_if_fail (probe != NULL, NULL);
  g_return_val_if_fail (GST_IS_PROPERTY_PROBE (probe), NULL);
  g_return_val_if_fail (pspec != NULL, NULL);

  if (gst_property_probe_needs_probe (probe, pspec))
    gst_property_probe_probe_property (probe, pspec);

  return gst_property_probe_get_values (probe, pspec);
}

/**
 * gst_property_probe_probe_and_get_values_name:
 * @probe: the #GstPropertyProbe object.
 * @name: the name of the property to get values for.
 *
 * Same as gst_property_probe_probe_and_get_values ().
 *
 * Returns: the list of valid values for this property.
 */
GValueArray *
gst_property_probe_probe_and_get_values_name (GstPropertyProbe * probe,
    const gchar * name)
{
  const GParamSpec *pspec;

  g_return_val_if_fail (probe != NULL, NULL);
  g_return_val_if_fail (GST_IS_PROPERTY_PROBE (probe), NULL);
  g_return_val_if_fail (name != NULL, NULL);

  pspec = g_object_class_find_property (G_OBJECT_GET_CLASS (probe), name);
  if (!pspec) {
    g_warning ("No such property %s", name);
    return NULL;
  }

  return gst_property_probe_probe_and_get_values (probe, pspec);
}
