/* GStreamer
 * Copyright (C) 2005 Stefan Kost <ensonic@users.sf.net>
 *
 * gstchildproxy.c: interface for multi child elements
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
 * SECTION:gstchildproxy
 * @short_description: Interface for multi child elements.
 * @see_also: #GstBin
 *
 * This interface abstracts handling of property sets for elements with
 * children. Imagine elements such as mixers or polyphonic generators. They all
 * have multiple #GstPad or some kind of voice objects. Another use case are
 * container elements like #GstBin.
 * The element implementing the interface acts as a parent for those child
 * objects.
 *
 * By implementing this interface the child properties can be accessed from the
 * parent element by using gst_child_proxy_get() and gst_child_proxy_set().
 *
 * Property names are written as "child-name::property-name". The whole naming
 * scheme is recursive. Thus "child1::child2::property" is valid too, if
 * "child1" and "child2" implement the #GstChildProxy interface.
 */
/* FIXME-0.11:
 * it would be nice to make gst_child_proxy_get_child_by_name virtual too and
 * use GObject instead of GstObject. We could eventually provide the current
 * implementation as a default if children are GstObjects.
 * This change would allow to propose the interface for inclusion with
 * glib/gobject. IMHO this is useful for GtkContainer and compound widgets too.
 */

#include "gst_private.h"

#include "gstchildproxy.h"
#include "gstmarshal.h"
#include <gobject/gvaluecollector.h>

/* signals */
enum
{
  CHILD_ADDED,
  CHILD_REMOVED,
  LAST_SIGNAL
};

static guint signals[LAST_SIGNAL] = { 0 };

/**
 * gst_child_proxy_get_child_by_name:
 * @parent: the parent object to get the child from
 * @name: the childs name
 *
 * Looks up a child element by the given name.
 *
 * Implementors can use #GstObject together with gst_object_get_name()
 *
 * Returns: (transfer full): the child object or %NULL if not found. Unref
 *     after usage.
 *
 * MT safe.
 */
GstObject *
gst_child_proxy_get_child_by_name (GstChildProxy * parent, const gchar * name)
{
  guint count, i;
  GstObject *object, *result;
  gchar *object_name;

  g_return_val_if_fail (GST_IS_CHILD_PROXY (parent), NULL);
  g_return_val_if_fail (name != NULL, NULL);

  result = NULL;

  count = gst_child_proxy_get_children_count (parent);
  for (i = 0; i < count; i++) {
    gboolean eq;

    if (!(object = gst_child_proxy_get_child_by_index (parent, i)))
      continue;

    object_name = gst_object_get_name (object);
    if (object_name == NULL) {
      g_warning ("child %u of parent %s has no name", i,
          GST_OBJECT_NAME (parent));
      goto next;
    }
    eq = g_str_equal (object_name, name);
    g_free (object_name);

    if (eq) {
      result = object;
      break;
    }
  next:
    gst_object_unref (object);
  }
  return result;
}

/**
 * gst_child_proxy_get_child_by_index:
 * @parent: the parent object to get the child from
 * @index: the childs position in the child list
 *
 * Fetches a child by its number.
 *
 * Returns: (transfer full): the child object or %NULL if not found (index
 *     too high). Unref after usage.
 *
 * MT safe.
 */
GstObject *
gst_child_proxy_get_child_by_index (GstChildProxy * parent, guint index)
{
  g_return_val_if_fail (GST_IS_CHILD_PROXY (parent), NULL);

  return (GST_CHILD_PROXY_GET_INTERFACE (parent)->get_child_by_index (parent,
          index));
}

/**
 * gst_child_proxy_get_children_count:
 * @parent: the parent object
 *
 * Gets the number of child objects this parent contains.
 *
 * Returns: the number of child objects
 *
 * MT safe.
 */
guint
gst_child_proxy_get_children_count (GstChildProxy * parent)
{
  g_return_val_if_fail (GST_IS_CHILD_PROXY (parent), 0);

  return (GST_CHILD_PROXY_GET_INTERFACE (parent)->get_children_count (parent));
}

/**
 * gst_child_proxy_lookup:
 * @object: object to lookup the property in
 * @name: name of the property to look up
 * @target: (out) (allow-none) (transfer full): pointer to a #GstObject that
 *     takes the real object to set property on
 * @pspec: (out) (allow-none) (transfer full): pointer to take the #GParamSpec
 *     describing the property
 *
 * Looks up which object and #GParamSpec would be effected by the given @name.
 *
 * Returns: TRUE if @target and @pspec could be found. FALSE otherwise. In that
 * case the values for @pspec and @target are not modified. Unref @target after
 * usage.
 *
 * MT safe.
 */
gboolean
gst_child_proxy_lookup (GstObject * object, const gchar * name,
    GstObject ** target, GParamSpec ** pspec)
{
  gboolean res = FALSE;
  gchar **names, **current;

  g_return_val_if_fail (GST_IS_OBJECT (object), FALSE);
  g_return_val_if_fail (name != NULL, FALSE);

  gst_object_ref (object);

  current = names = g_strsplit (name, "::", -1);
  while (current[1]) {
    GstObject *next;

    if (!GST_IS_CHILD_PROXY (object)) {
      GST_INFO
          ("object %s is not a parent, so you cannot request a child by name %s",
          GST_OBJECT_NAME (object), current[0]);
      break;
    }
    next = gst_child_proxy_get_child_by_name (GST_CHILD_PROXY (object),
        current[0]);
    if (!next) {
      GST_INFO ("no such object %s", current[0]);
      break;
    }
    gst_object_unref (object);
    object = next;
    current++;
  }
  if (current[1] == NULL) {
    GParamSpec *spec =
        g_object_class_find_property (G_OBJECT_GET_CLASS (object), current[0]);
    if (spec == NULL) {
      GST_INFO ("no param spec named %s", current[0]);
    } else {
      if (pspec)
        *pspec = spec;
      if (target) {
        gst_object_ref (object);
        *target = object;
      }
      res = TRUE;
    }
  }
  gst_object_unref (object);
  g_strfreev (names);
  return res;
}

/**
 * gst_child_proxy_get_property:
 * @object: object to query
 * @name: name of the property
 * @value: (out caller-allocates): a #GValue that should take the result.
 *
 * Gets a single property using the GstChildProxy mechanism.
 * You are responsible for for freeing it by calling g_value_unset()
 */
void
gst_child_proxy_get_property (GstObject * object, const gchar * name,
    GValue * value)
{
  GParamSpec *pspec;
  GstObject *target;

  g_return_if_fail (GST_IS_OBJECT (object));
  g_return_if_fail (name != NULL);
  g_return_if_fail (G_IS_VALUE (value));

  if (!gst_child_proxy_lookup (object, name, &target, &pspec))
    goto not_found;

  g_object_get_property (G_OBJECT (target), pspec->name, value);
  gst_object_unref (target);

  return;

not_found:
  {
    g_warning ("no property %s in object %s", name, GST_OBJECT_NAME (object));
    return;
  }
}

/**
 * gst_child_proxy_get_valist:
 * @object: the object to query
 * @first_property_name: name of the first property to get
 * @var_args: return location for the first property, followed optionally by more name/return location pairs, followed by NULL
 *
 * Gets properties of the parent object and its children.
 */
void
gst_child_proxy_get_valist (GstObject * object,
    const gchar * first_property_name, va_list var_args)
{
  const gchar *name;
  gchar *error = NULL;
  GValue value = { 0, };
  GParamSpec *pspec;
  GstObject *target;

  g_return_if_fail (G_IS_OBJECT (object));

  name = first_property_name;

  /* iterate over pairs */
  while (name) {
    if (!gst_child_proxy_lookup (object, name, &target, &pspec))
      goto not_found;

    g_value_init (&value, pspec->value_type);
    g_object_get_property (G_OBJECT (target), pspec->name, &value);
    gst_object_unref (target);

    G_VALUE_LCOPY (&value, var_args, 0, &error);
    if (error)
      goto cant_copy;
    g_value_unset (&value);
    name = va_arg (var_args, gchar *);
  }
  return;

not_found:
  {
    g_warning ("no property %s in object %s", name, GST_OBJECT_NAME (object));
    return;
  }
cant_copy:
  {
    g_warning ("error copying value %s in object %s: %s", pspec->name,
        GST_OBJECT_NAME (object), error);
    g_value_unset (&value);
    return;
  }
}

/**
 * gst_child_proxy_get:
 * @object: the parent object
 * @first_property_name: name of the first property to get
 * @...: return location for the first property, followed optionally by more name/return location pairs, followed by NULL
 *
 * Gets properties of the parent object and its children.
 */
void
gst_child_proxy_get (GstObject * object, const gchar * first_property_name, ...)
{
  va_list var_args;

  g_return_if_fail (GST_IS_OBJECT (object));

  va_start (var_args, first_property_name);
  gst_child_proxy_get_valist (object, first_property_name, var_args);
  va_end (var_args);
}

/**
 * gst_child_proxy_set_property:
 * @object: the parent object
 * @name: name of the property to set
 * @value: new #GValue for the property
 *
 * Sets a single property using the GstChildProxy mechanism.
 */
void
gst_child_proxy_set_property (GstObject * object, const gchar * name,
    const GValue * value)
{
  GParamSpec *pspec;
  GstObject *target;

  g_return_if_fail (GST_IS_OBJECT (object));
  g_return_if_fail (name != NULL);
  g_return_if_fail (G_IS_VALUE (value));

  if (!gst_child_proxy_lookup (object, name, &target, &pspec))
    goto not_found;

  g_object_set_property (G_OBJECT (target), pspec->name, value);
  gst_object_unref (target);
  return;

not_found:
  {
    g_warning ("cannot set property %s on object %s", name,
        GST_OBJECT_NAME (object));
    return;
  }
}

/**
 * gst_child_proxy_set_valist:
 * @object: the parent object
 * @first_property_name: name of the first property to set
 * @var_args: value for the first property, followed optionally by more name/value pairs, followed by NULL
 *
 * Sets properties of the parent object and its children.
 */
void
gst_child_proxy_set_valist (GstObject * object,
    const gchar * first_property_name, va_list var_args)
{
  const gchar *name;
  gchar *error = NULL;
  GValue value = { 0, };
  GParamSpec *pspec;
  GstObject *target;

  g_return_if_fail (G_IS_OBJECT (object));

  name = first_property_name;

  /* iterate over pairs */
  while (name) {
    if (!gst_child_proxy_lookup (object, name, &target, &pspec))
      goto not_found;

#if GLIB_CHECK_VERSION(2,23,3)
    G_VALUE_COLLECT_INIT (&value, pspec->value_type, var_args,
        G_VALUE_NOCOPY_CONTENTS, &error);
#else
    g_value_init (&value, G_PARAM_SPEC_VALUE_TYPE (pspec));
    G_VALUE_COLLECT (&value, var_args, G_VALUE_NOCOPY_CONTENTS, &error);
#endif
    if (error)
      goto cant_copy;

    g_object_set_property (G_OBJECT (target), pspec->name, &value);
    gst_object_unref (target);

    g_value_unset (&value);
    name = va_arg (var_args, gchar *);
  }
  return;

not_found:
  {
    g_warning ("no property %s in object %s", name, GST_OBJECT_NAME (object));
    return;
  }
cant_copy:
  {
    g_warning ("error copying value %s in object %s: %s", pspec->name,
        GST_OBJECT_NAME (object), error);
    g_value_unset (&value);
    gst_object_unref (target);
    return;
  }
}

/**
 * gst_child_proxy_set:
 * @object: the parent object
 * @first_property_name: name of the first property to set
 * @...: value for the first property, followed optionally by more name/value pairs, followed by NULL
 *
 * Sets properties of the parent object and its children.
 */
void
gst_child_proxy_set (GstObject * object, const gchar * first_property_name, ...)
{
  va_list var_args;

  g_return_if_fail (GST_IS_OBJECT (object));

  va_start (var_args, first_property_name);
  gst_child_proxy_set_valist (object, first_property_name, var_args);
  va_end (var_args);
}

/**
 * gst_child_proxy_child_added:
 * @object: the parent object
 * @child: the newly added child
 *
 * Emits the "child-added" signal.
 */
void
gst_child_proxy_child_added (GstObject * object, GstObject * child)
{
  g_signal_emit (G_OBJECT (object), signals[CHILD_ADDED], 0, child);
}

/**
 * gst_child_proxy_child_removed:
 * @object: the parent object
 * @child: the removed child
 *
 * Emits the "child-removed" signal.
 */
void
gst_child_proxy_child_removed (GstObject * object, GstObject * child)
{
  g_signal_emit (G_OBJECT (object), signals[CHILD_REMOVED], 0, child);
}

/* gobject methods */

static void
gst_child_proxy_base_init (gpointer g_class)
{
  static gboolean initialized = FALSE;

  if (!initialized) {
    /* create interface signals and properties here. */
        /**
	 * GstChildProxy::child-added:
	 * @child_proxy: the #GstChildProxy
	 * @object: the #GObject that was added
	 *
	 * Will be emitted after the @object was added to the @child_proxy.
	 */
    /* FIXME 0.11: use GST_TYPE_OBJECT as GstChildProxy only
     * supports GstObjects */
    signals[CHILD_ADDED] =
        g_signal_new ("child-added", G_TYPE_FROM_CLASS (g_class),
        G_SIGNAL_RUN_FIRST, G_STRUCT_OFFSET (GstChildProxyInterface,
            child_added), NULL, NULL, gst_marshal_VOID__OBJECT, G_TYPE_NONE, 1,
        G_TYPE_OBJECT);

        /**
	 * GstChildProxy::child-removed:
	 * @child_proxy: the #GstChildProxy
	 * @object: the #GObject that was removed
	 *
	 * Will be emitted after the @object was removed from the @child_proxy.
	 */
    /* FIXME 0.11: use GST_TYPE_OBJECT as GstChildProxy only
     * supports GstObjects */
    signals[CHILD_REMOVED] =
        g_signal_new ("child-removed", G_TYPE_FROM_CLASS (g_class),
        G_SIGNAL_RUN_FIRST, G_STRUCT_OFFSET (GstChildProxyInterface,
            child_removed), NULL, NULL, gst_marshal_VOID__OBJECT, G_TYPE_NONE,
        1, G_TYPE_OBJECT);

    initialized = TRUE;
  }
}

GType
gst_child_proxy_get_type (void)
{
  static volatile gsize type = 0;

  if (g_once_init_enter (&type)) {
    GType _type;
    static const GTypeInfo info = {
      sizeof (GstChildProxyInterface),
      gst_child_proxy_base_init,        /* base_init */
      NULL,                     /* base_finalize */
      NULL,                     /* class_init */
      NULL,                     /* class_finalize */
      NULL,                     /* class_data */
      0,
      0,                        /* n_preallocs */
      NULL                      /* instance_init */
    };

    _type =
        g_type_register_static (G_TYPE_INTERFACE, "GstChildProxy", &info, 0);

    g_type_interface_add_prerequisite (_type, GST_TYPE_OBJECT);
    g_once_init_leave (&type, (gsize) _type);
  }
  return type;
}
