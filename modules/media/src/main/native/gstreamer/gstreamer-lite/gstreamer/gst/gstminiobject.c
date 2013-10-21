/* GStreamer
 * Copyright (C) 2005 David Schleef <ds@schleef.org>
 *
 * gstminiobject.h: Header for GstMiniObject
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
 * SECTION:gstminiobject
 * @short_description: Lightweight base class for the GStreamer object hierarchy
 *
 * #GstMiniObject is a baseclass like #GObject, but has been stripped down of
 * features to be fast and small.
 * It offers sub-classing and ref-counting in the same way as #GObject does.
 * It has no properties and no signal-support though.
 *
 * Last reviewed on 2005-11-23 (0.9.5)
 */
#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "gst/gst_private.h"
#include "gst/gstminiobject.h"
#include "gst/gstinfo.h"
#include <gobject/gvaluecollector.h>

#ifndef GST_DISABLE_TRACE
#include "gsttrace.h"
static GstAllocTrace *_gst_mini_object_trace;
#endif

#define GST_MINI_OBJECT_GET_CLASS_UNCHECKED(obj) \
    ((GstMiniObjectClass *) (((GTypeInstance*)(obj))->g_class))

#if 0
static void gst_mini_object_base_init (gpointer g_class);
static void gst_mini_object_base_finalize (gpointer g_class);
#endif
static void gst_mini_object_class_init (gpointer g_class, gpointer class_data);
static void gst_mini_object_init (GTypeInstance * instance, gpointer klass);

static void gst_value_mini_object_init (GValue * value);
static void gst_value_mini_object_free (GValue * value);
static void gst_value_mini_object_copy (const GValue * src_value,
    GValue * dest_value);
static gpointer gst_value_mini_object_peek_pointer (const GValue * value);
static gchar *gst_value_mini_object_collect (GValue * value,
    guint n_collect_values, GTypeCValue * collect_values, guint collect_flags);
static gchar *gst_value_mini_object_lcopy (const GValue * value,
    guint n_collect_values, GTypeCValue * collect_values, guint collect_flags);

static GstMiniObject *gst_mini_object_copy_default (const GstMiniObject * obj);
static void gst_mini_object_finalize (GstMiniObject * obj);

GType
gst_mini_object_get_type (void)
{
  static volatile GType _gst_mini_object_type = 0;

  if (g_once_init_enter (&_gst_mini_object_type)) {
    GType _type;
    static const GTypeValueTable value_table = {
      gst_value_mini_object_init,
      gst_value_mini_object_free,
      gst_value_mini_object_copy,
      gst_value_mini_object_peek_pointer,
      (char *) "p",
      gst_value_mini_object_collect,
      (char *) "p",
      gst_value_mini_object_lcopy
    };
    static const GTypeInfo mini_object_info = {
      sizeof (GstMiniObjectClass),
#if 0
      gst_mini_object_base_init,
      gst_mini_object_base_finalize,
#else
      NULL, NULL,
#endif
      gst_mini_object_class_init,
      NULL,
      NULL,
      sizeof (GstMiniObject),
      0,
      (GInstanceInitFunc) gst_mini_object_init,
      &value_table
    };
    static const GTypeFundamentalInfo mini_object_fundamental_info = {
      (G_TYPE_FLAG_CLASSED | G_TYPE_FLAG_INSTANTIATABLE |
          G_TYPE_FLAG_DERIVABLE | G_TYPE_FLAG_DEEP_DERIVABLE)
    };

    _type = g_type_fundamental_next ();
    g_type_register_fundamental (_type, "GstMiniObject",
        &mini_object_info, &mini_object_fundamental_info, G_TYPE_FLAG_ABSTRACT);

#ifndef GST_DISABLE_TRACE
    _gst_mini_object_trace = gst_alloc_trace_register (g_type_name (_type));
#endif
    g_once_init_leave (&_gst_mini_object_type, _type);
  }

  return _gst_mini_object_type;
}

#if 0
static void
gst_mini_object_base_init (gpointer g_class)
{
  /* do nothing */
}

static void
gst_mini_object_base_finalize (gpointer g_class)
{
  /* do nothing */
}
#endif

static void
gst_mini_object_class_init (gpointer g_class, gpointer class_data)
{
  GstMiniObjectClass *mo_class = GST_MINI_OBJECT_CLASS (g_class);

  mo_class->copy = gst_mini_object_copy_default;
  mo_class->finalize = gst_mini_object_finalize;
}

static void
gst_mini_object_init (GTypeInstance * instance, gpointer klass)
{
  GstMiniObject *mini_object = GST_MINI_OBJECT_CAST (instance);

  mini_object->refcount = 1;
}

static GstMiniObject *
gst_mini_object_copy_default (const GstMiniObject * obj)
{
  g_warning ("GstMiniObject classes must implement GstMiniObject::copy");
  return NULL;
}

static void
gst_mini_object_finalize (GstMiniObject * obj)
{
  /* do nothing */

  /* WARNING: if anything is ever put in this method, make sure that the
   * following sub-classes' finalize method chains up to this one:
   * gstbuffer
   * gstevent
   * gstmessage
   * gstquery
   */
}

/**
 * gst_mini_object_new:
 * @type: the #GType of the mini-object to create
 *
 * Creates a new mini-object of the desired type.
 *
 * MT safe
 *
 * Returns: (transfer full): the new mini-object.
 */
GstMiniObject *
gst_mini_object_new (GType type)
{
  GstMiniObject *mini_object;

  /* we don't support dynamic types because they really aren't useful,
   * and could cause refcount problems */
  mini_object = (GstMiniObject *) g_type_create_instance (type);

#ifndef GST_DISABLE_TRACE
  gst_alloc_trace_new (_gst_mini_object_trace, mini_object);
#endif

  return mini_object;
}

/* FIXME 0.11: Current way of doing the copy makes it impossible
 * to currectly chain to the parent classes and do a copy in a
 * subclass without knowing all internals of the parent classes.
 *
 * For 0.11 we should do something like the following:
 *  - The GstMiniObjectClass::copy() implementation of GstMiniObject
 *    should call g_type_create_instance() with the type of the source
 *    object.
 *  - All GstMiniObjectClass::copy() implementations should as first
 *    thing chain up to the parent class and then do whatever they need
 *    to do to copy their type specific data. Note that this way the
 *    instance_init() functions are called!
 */

/**
 * gst_mini_object_copy:
 * @mini_object: the mini-object to copy
 *
 * Creates a copy of the mini-object.
 *
 * MT safe
 *
 * Returns: (transfer full): the new mini-object.
 */
GstMiniObject *
gst_mini_object_copy (const GstMiniObject * mini_object)
{
  GstMiniObjectClass *mo_class;

  g_return_val_if_fail (mini_object != NULL, NULL);

  mo_class = GST_MINI_OBJECT_GET_CLASS (mini_object);

  return mo_class->copy (mini_object);
}

/**
 * gst_mini_object_is_writable:
 * @mini_object: the mini-object to check
 *
 * Checks if a mini-object is writable.  A mini-object is writable
 * if the reference count is one and the #GST_MINI_OBJECT_FLAG_READONLY
 * flag is not set.  Modification of a mini-object should only be
 * done after verifying that it is writable.
 *
 * MT safe
 *
 * Returns: TRUE if the object is writable.
 */
gboolean
gst_mini_object_is_writable (const GstMiniObject * mini_object)
{
  g_return_val_if_fail (mini_object != NULL, FALSE);

  return (GST_MINI_OBJECT_REFCOUNT_VALUE (mini_object) == 1) &&
      ((mini_object->flags & GST_MINI_OBJECT_FLAG_READONLY) == 0);
}

/**
 * gst_mini_object_make_writable:
 * @mini_object: (transfer full): the mini-object to make writable
 *
 * Checks if a mini-object is writable.  If not, a writable copy is made and
 * returned.  This gives away the reference to the original mini object,
 * and returns a reference to the new object.
 *
 * MT safe
 *
 * Returns: (transfer full): a mini-object (possibly the same pointer) that
 *     is writable.
 */
GstMiniObject *
gst_mini_object_make_writable (GstMiniObject * mini_object)
{
  GstMiniObject *ret;

  g_return_val_if_fail (mini_object != NULL, NULL);

  if (gst_mini_object_is_writable (mini_object)) {
    ret = mini_object;
  } else {
    GST_CAT_DEBUG (GST_CAT_PERFORMANCE, "copy %s miniobject",
        g_type_name (G_TYPE_FROM_INSTANCE (mini_object)));
    ret = gst_mini_object_copy (mini_object);
    gst_mini_object_unref (mini_object);
  }

  return ret;
}

/**
 * gst_mini_object_ref:
 * @mini_object: the mini-object
 *
 * Increase the reference count of the mini-object.
 *
 * Note that the refcount affects the writeability
 * of @mini-object, see gst_mini_object_is_writable(). It is
 * important to note that keeping additional references to
 * GstMiniObject instances can potentially increase the number
 * of memcpy operations in a pipeline, especially if the miniobject
 * is a #GstBuffer.
 *
 * Returns: (transfer full): the mini-object.
 */
GstMiniObject *
gst_mini_object_ref (GstMiniObject * mini_object)
{
  g_return_val_if_fail (mini_object != NULL, NULL);
  /* we can't assert that the refcount > 0 since the _free functions
   * increments the refcount from 0 to 1 again to allow resurecting
   * the object
   g_return_val_if_fail (mini_object->refcount > 0, NULL);
   */
  g_return_val_if_fail (GST_IS_MINI_OBJECT (mini_object), NULL);

  GST_CAT_TRACE (GST_CAT_REFCOUNTING, "%p ref %d->%d", mini_object,
      GST_MINI_OBJECT_REFCOUNT_VALUE (mini_object),
      GST_MINI_OBJECT_REFCOUNT_VALUE (mini_object) + 1);

  g_atomic_int_inc (&mini_object->refcount);

  return mini_object;
}

static void
gst_mini_object_free (GstMiniObject * mini_object)
{
  GstMiniObjectClass *mo_class;

  /* At this point, the refcount of the object is 0. We increase the refcount
   * here because if a subclass recycles the object and gives out a new
   * reference we don't want to free the instance anymore. */
  GST_CAT_TRACE (GST_CAT_REFCOUNTING, "%p ref %d->%d", mini_object,
      GST_MINI_OBJECT_REFCOUNT_VALUE (mini_object),
      GST_MINI_OBJECT_REFCOUNT_VALUE (mini_object) + 1);

  g_atomic_int_inc (&mini_object->refcount);

  mo_class = GST_MINI_OBJECT_GET_CLASS_UNCHECKED (mini_object);
  mo_class->finalize (mini_object);

  /* decrement the refcount again, if the subclass recycled the object we don't
   * want to free the instance anymore */
  if (G_LIKELY (g_atomic_int_dec_and_test (&mini_object->refcount))) {
#ifndef GST_DISABLE_TRACE
    gst_alloc_trace_free (_gst_mini_object_trace, mini_object);
#endif
    g_type_free_instance ((GTypeInstance *) mini_object);
  }
}

/**
 * gst_mini_object_unref:
 * @mini_object: the mini-object
 *
 * Decreases the reference count of the mini-object, possibly freeing
 * the mini-object.
 */
void
gst_mini_object_unref (GstMiniObject * mini_object)
{
  g_return_if_fail (GST_IS_MINI_OBJECT (mini_object));
  g_return_if_fail (mini_object->refcount > 0);

  GST_CAT_TRACE (GST_CAT_REFCOUNTING, "%p unref %d->%d",
      mini_object,
      GST_MINI_OBJECT_REFCOUNT_VALUE (mini_object),
      GST_MINI_OBJECT_REFCOUNT_VALUE (mini_object) - 1);

  if (G_UNLIKELY (g_atomic_int_dec_and_test (&mini_object->refcount))) {
    gst_mini_object_free (mini_object);
  }
}

/**
 * gst_mini_object_replace:
 * @olddata: (inout) (transfer full): pointer to a pointer to a mini-object to
 *     be replaced
 * @newdata: pointer to new mini-object
 *
 * Modifies a pointer to point to a new mini-object.  The modification
 * is done atomically, and the reference counts are updated correctly.
 * Either @newdata and the value pointed to by @olddata may be NULL.
 */
void
gst_mini_object_replace (GstMiniObject ** olddata, GstMiniObject * newdata)
{
  GstMiniObject *olddata_val;

  g_return_if_fail (olddata != NULL);

  GST_CAT_TRACE (GST_CAT_REFCOUNTING, "replace %p (%d) with %p (%d)",
      *olddata, *olddata ? (*olddata)->refcount : 0,
      newdata, newdata ? newdata->refcount : 0);

  olddata_val = g_atomic_pointer_get ((gpointer *) olddata);

  if (olddata_val == newdata)
    return;

  if (newdata)
    gst_mini_object_ref (newdata);

  while (!g_atomic_pointer_compare_and_exchange ((gpointer *) olddata,
          olddata_val, newdata)) {
    olddata_val = g_atomic_pointer_get ((gpointer *) olddata);
  }

  if (olddata_val)
    gst_mini_object_unref (olddata_val);
}

static void
gst_value_mini_object_init (GValue * value)
{
  value->data[0].v_pointer = NULL;
}

static void
gst_value_mini_object_free (GValue * value)
{
  if (value->data[0].v_pointer) {
    gst_mini_object_unref (GST_MINI_OBJECT_CAST (value->data[0].v_pointer));
  }
}

static void
gst_value_mini_object_copy (const GValue * src_value, GValue * dest_value)
{
  if (src_value->data[0].v_pointer) {
    dest_value->data[0].v_pointer =
        gst_mini_object_ref (GST_MINI_OBJECT_CAST (src_value->
            data[0].v_pointer));
  } else {
    dest_value->data[0].v_pointer = NULL;
  }
}

static gpointer
gst_value_mini_object_peek_pointer (const GValue * value)
{
  return value->data[0].v_pointer;
}

static gchar *
gst_value_mini_object_collect (GValue * value, guint n_collect_values,
    GTypeCValue * collect_values, guint collect_flags)
{
  if (collect_values[0].v_pointer) {
    value->data[0].v_pointer =
        gst_mini_object_ref (collect_values[0].v_pointer);
  } else {
    value->data[0].v_pointer = NULL;
  }

  return NULL;
}

static gchar *
gst_value_mini_object_lcopy (const GValue * value, guint n_collect_values,
    GTypeCValue * collect_values, guint collect_flags)
{
  gpointer *mini_object_p = collect_values[0].v_pointer;

  if (!mini_object_p) {
    return g_strdup_printf ("value location for '%s' passed as NULL",
        G_VALUE_TYPE_NAME (value));
  }

  if (!value->data[0].v_pointer)
    *mini_object_p = NULL;
  else if (collect_flags & G_VALUE_NOCOPY_CONTENTS)
    *mini_object_p = value->data[0].v_pointer;
  else
    *mini_object_p = gst_mini_object_ref (value->data[0].v_pointer);

  return NULL;
}

/**
 * gst_value_set_mini_object:
 * @value: a valid #GValue of %GST_TYPE_MINI_OBJECT derived type
 * @mini_object: (transfer none): mini object value to set
 *
 * Set the contents of a %GST_TYPE_MINI_OBJECT derived #GValue to
 * @mini_object.
 * The caller retains ownership of the reference.
 */
void
gst_value_set_mini_object (GValue * value, GstMiniObject * mini_object)
{
  gpointer *pointer_p;

  g_return_if_fail (GST_VALUE_HOLDS_MINI_OBJECT (value));
  g_return_if_fail (mini_object == NULL || GST_IS_MINI_OBJECT (mini_object));

  pointer_p = &value->data[0].v_pointer;
  gst_mini_object_replace ((GstMiniObject **) pointer_p, mini_object);
}

/**
 * gst_value_take_mini_object:
 * @value: a valid #GValue of %GST_TYPE_MINI_OBJECT derived type
 * @mini_object: (transfer full): mini object value to take
 *
 * Set the contents of a %GST_TYPE_MINI_OBJECT derived #GValue to
 * @mini_object.
 * Takes over the ownership of the caller's reference to @mini_object;
 * the caller doesn't have to unref it any more.
 */
void
gst_value_take_mini_object (GValue * value, GstMiniObject * mini_object)
{
  gpointer *pointer_p;

  g_return_if_fail (GST_VALUE_HOLDS_MINI_OBJECT (value));
  g_return_if_fail (mini_object == NULL || GST_IS_MINI_OBJECT (mini_object));

  pointer_p = &value->data[0].v_pointer;
  /* takes additional refcount */
  gst_mini_object_replace ((GstMiniObject **) pointer_p, mini_object);
  /* remove additional refcount */
  if (mini_object)
    gst_mini_object_unref (mini_object);
}

/**
 * gst_value_get_mini_object:
 * @value:   a valid #GValue of %GST_TYPE_MINI_OBJECT derived type
 *
 * Get the contents of a %GST_TYPE_MINI_OBJECT derived #GValue.
 * Does not increase the refcount of the returned object.
 *
 * Returns: (transfer none): mini object contents of @value
 */
GstMiniObject *
gst_value_get_mini_object (const GValue * value)
{
  g_return_val_if_fail (GST_VALUE_HOLDS_MINI_OBJECT (value), NULL);

  return value->data[0].v_pointer;
}

/**
 * gst_value_dup_mini_object:
 * @value:   a valid #GValue of %GST_TYPE_MINI_OBJECT derived type
 *
 * Get the contents of a %GST_TYPE_MINI_OBJECT derived #GValue,
 * increasing its reference count. If the contents of the #GValue
 * are %NULL, %NULL will be returned.
 *
 * Returns: (transfer full): mini object contents of @value
 *
 * Since: 0.10.20
 */
GstMiniObject *
gst_value_dup_mini_object (const GValue * value)
{
  g_return_val_if_fail (GST_VALUE_HOLDS_MINI_OBJECT (value), NULL);

  return value->data[0].v_pointer ? gst_mini_object_ref (value->data[0].
      v_pointer) : NULL;
}


/* param spec */

static void
param_mini_object_init (GParamSpec * pspec)
{
  /* GParamSpecMiniObject *ospec = G_PARAM_SPEC_MINI_OBJECT (pspec); */
}

static void
param_mini_object_set_default (GParamSpec * pspec, GValue * value)
{
  value->data[0].v_pointer = NULL;
}

static gboolean
param_mini_object_validate (GParamSpec * pspec, GValue * value)
{
  GstMiniObject *mini_object = value->data[0].v_pointer;
  gboolean changed = FALSE;

  if (mini_object
      && !g_value_type_compatible (G_OBJECT_TYPE (mini_object),
          pspec->value_type)) {
    gst_mini_object_unref (mini_object);
    value->data[0].v_pointer = NULL;
    changed = TRUE;
  }

  return changed;
}

static gint
param_mini_object_values_cmp (GParamSpec * pspec,
    const GValue * value1, const GValue * value2)
{
  guint8 *p1 = value1->data[0].v_pointer;
  guint8 *p2 = value2->data[0].v_pointer;

  /* not much to compare here, try to at least provide stable lesser/greater result */

  return p1 < p2 ? -1 : p1 > p2;
}

GType
gst_param_spec_mini_object_get_type (void)
{
  static GType type;

  if (G_UNLIKELY (type) == 0) {
    static const GParamSpecTypeInfo pspec_info = {
      sizeof (GstParamSpecMiniObject),  /* instance_size */
      16,                       /* n_preallocs */
      param_mini_object_init,   /* instance_init */
      G_TYPE_OBJECT,            /* value_type */
      NULL,                     /* finalize */
      param_mini_object_set_default,    /* value_set_default */
      param_mini_object_validate,       /* value_validate */
      param_mini_object_values_cmp,     /* values_cmp */
    };
    /* FIXME 0.11: Should really be GstParamSpecMiniObject */
    type = g_param_type_register_static ("GParamSpecMiniObject", &pspec_info);
  }

  return type;
}

/**
 * gst_param_spec_mini_object:
 * @name: the canonical name of the property
 * @nick: the nickname of the property
 * @blurb: a short description of the property
 * @object_type: the #GstMiniObject #GType for the property
 * @flags: a combination of #GParamFlags
 *
 * Creates a new #GParamSpec instance that hold #GstMiniObject references.
 *
 * Returns: (transfer full): a newly allocated #GParamSpec instance
 */
GParamSpec *
gst_param_spec_mini_object (const char *name, const char *nick,
    const char *blurb, GType object_type, GParamFlags flags)
{
  GstParamSpecMiniObject *ospec;

  g_return_val_if_fail (g_type_is_a (object_type, GST_TYPE_MINI_OBJECT), NULL);

  ospec = g_param_spec_internal (GST_TYPE_PARAM_MINI_OBJECT,
      name, nick, blurb, flags);
  G_PARAM_SPEC (ospec)->value_type = object_type;

  return G_PARAM_SPEC (ospec);
}
