/* GStreamer
 *
 * Copyright (C) 2011 Stefan Sauer <ensonic@users.sf.net>
 *
 * gstcontrolbinding.h: Attachment for control sources
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

#ifndef __GST_CONTROL_BINDING_H__
#define __GST_CONTROL_BINDING_H__

#include <gst/gstconfig.h>

#include <glib-object.h>

G_BEGIN_DECLS

#define GST_TYPE_CONTROL_BINDING \
  (gst_control_binding_get_type())
#define GST_CONTROL_BINDING(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_CONTROL_BINDING,GstControlBinding))
#define GST_CONTROL_BINDING_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),GST_TYPE_CONTROL_BINDING,GstControlBindingClass))
#define GST_IS_CONTROL_BINDING(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_CONTROL_BINDING))
#define GST_IS_CONTROL_BINDING_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),GST_TYPE_CONTROL_BINDING))
#define GST_CONTROL_BINDING_GET_CLASS(obj) \
  (G_TYPE_INSTANCE_GET_CLASS ((obj), GST_TYPE_CONTOL_SOURCE, GstControlBindingClass))

typedef struct _GstControlBinding GstControlBinding;
typedef struct _GstControlBindingClass GstControlBindingClass;
typedef struct _GstControlBindingPrivate GstControlBindingPrivate;

#include <gst/gstcontrolsource.h>

/**
 * GstControlBindingConvert: (attributes doc.skip=true)
 * FIXME(2.0): remove, this is unused
 */
typedef void (* GstControlBindingConvert) (GstControlBinding *binding, gdouble src_value, GValue *dest_value);

/**
 * GstControlBinding:
 * @name: name of the property of this binding
 * @pspec: #GParamSpec for this property
 *
 * The instance structure of #GstControlBinding.
 */
struct _GstControlBinding {
  GstObject parent;

  /*< public >*/
  gchar *name;
  GParamSpec *pspec;

  /*< private >*/
#ifndef GSTREAMER_LITE
#ifndef GST_DISABLE_DEPRECATED
  GstObject *object;            /* GstObject owning the property
                                 * (== parent when bound) */
#else
  gpointer __object;
#endif
#else // GSTREAMER_LITE
  // Looks like a bug in GStreamer. Code expects __object even if
  // GST_DISABLE_DEPRECATED is not defined.
  gpointer __object;
#endif // GSTREAMER_LITE
  gboolean disabled;

  union {
    struct {
      GstControlBindingPrivate *priv;
    } abi;
    gpointer _gst_reserved[GST_PADDING];
  } ABI;
};

/**
 * GstControlBindingClass:
 * @parent_class: Parent class
 * @sync_values: implementation for updating the target values
 * @get_value: implementation to fetch a single control-value
 * @get_value_array: implementation to fetch a series of control-values
 * @get_g_value_array: implementation to fetch a series of control-values
 *                     as g_values
 *
 * The class structure of #GstControlBinding.
 */

struct _GstControlBindingClass
{
  GstObjectClass parent_class;

  /*< public >*/
  gboolean (* sync_values) (GstControlBinding *binding, GstObject *object, GstClockTime timestamp, GstClockTime last_sync);
  GValue * (* get_value) (GstControlBinding *binding, GstClockTime timestamp);
  gboolean (* get_value_array) (GstControlBinding *binding, GstClockTime timestamp,GstClockTime interval, guint n_values, gpointer values);
  gboolean (* get_g_value_array) (GstControlBinding *binding, GstClockTime timestamp,GstClockTime interval, guint n_values, GValue *values);

  /*< private >*/
  gpointer _gst_reserved[GST_PADDING];
};

#define GST_CONTROL_BINDING_PSPEC(cb) (((GstControlBinding *) cb)->pspec)

GST_API
GType               gst_control_binding_get_type (void);

/* Functions */

GST_API
gboolean            gst_control_binding_sync_values        (GstControlBinding * binding, GstObject *object,
                                                            GstClockTime timestamp, GstClockTime last_sync);
GST_API
GValue *            gst_control_binding_get_value          (GstControlBinding *binding,
                                                            GstClockTime timestamp);
GST_API
gboolean            gst_control_binding_get_value_array    (GstControlBinding *binding, GstClockTime timestamp,
                                                            GstClockTime interval, guint n_values, gpointer values);
GST_API
gboolean            gst_control_binding_get_g_value_array  (GstControlBinding *binding, GstClockTime timestamp,
                                                            GstClockTime interval, guint n_values, GValue *values);
GST_API
void                gst_control_binding_set_disabled       (GstControlBinding * binding, gboolean disabled);

GST_API
gboolean            gst_control_binding_is_disabled        (GstControlBinding * binding);

G_DEFINE_AUTOPTR_CLEANUP_FUNC(GstControlBinding, gst_object_unref)

G_END_DECLS

#endif /* __GST_CONTROL_BINDING_H__ */
