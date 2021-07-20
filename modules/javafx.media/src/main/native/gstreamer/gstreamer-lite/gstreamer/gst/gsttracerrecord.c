/* GStreamer
 * Copyright (C) 2016 Stefan Sauer <ensonic@users.sf.net>
 *
 * gsttracerrecord.c: tracer log record class
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
 * SECTION:gsttracerrecord
 * @title: GstTracerRecord
 * @short_description: Trace log entry class
 *
 * Tracing modules will create instances of this class to announce the data they
 * will log and create a log formatter.
 *
 * Since: 1.8
 */

#include "gst_private.h"
#include "gstenumtypes.h"
#include "gstinfo.h"
#include "gststructure.h"
#include "gsttracerrecord.h"
#include "gstvalue.h"
#include <gobject/gvaluecollector.h>

GST_DEBUG_CATEGORY_EXTERN (tracer_debug);
#define GST_CAT_DEFAULT tracer_debug

struct _GstTracerRecord
{
  GstObject parent;

  GstStructure *spec;
  gchar *format;
};

struct _GstTracerRecordClass
{
  GstObjectClass parent_class;
};

#define gst_tracer_record_parent_class parent_class
G_DEFINE_TYPE (GstTracerRecord, gst_tracer_record, GST_TYPE_OBJECT);

static gboolean
build_field_template (GQuark field_id, const GValue * value, gpointer user_data)
{
  GString *s = (GString *) user_data;
  const GstStructure *sub;
  GValue template_value = { 0, };
  GType type = G_TYPE_INVALID;
  GstTracerValueFlags flags = GST_TRACER_VALUE_FLAGS_NONE;
  gboolean res;

  if (G_VALUE_TYPE (value) != GST_TYPE_STRUCTURE) {
    GST_ERROR ("expected field of type GstStructure, but %s is %s",
        g_quark_to_string (field_id), G_VALUE_TYPE_NAME (value));
    return FALSE;
  }

  sub = gst_value_get_structure (value);
  gst_structure_get (sub, "type", G_TYPE_GTYPE, &type, "flags",
      GST_TYPE_TRACER_VALUE_FLAGS, &flags, NULL);

  if (flags & GST_TRACER_VALUE_FLAGS_OPTIONAL) {
    gchar *opt_name = g_strconcat ("have-", g_quark_to_string (field_id), NULL);

    /* add a boolean field, that indicates the presence of the next field */
    g_value_init (&template_value, G_TYPE_BOOLEAN);
    priv__gst_structure_append_template_to_gstring (g_quark_from_string
        (opt_name), &template_value, s);
    g_value_unset (&template_value);
    g_free (opt_name);
  }

  g_value_init (&template_value, type);
  res = priv__gst_structure_append_template_to_gstring (field_id,
      &template_value, s);
  g_value_unset (&template_value);
  return res;
}

static void
gst_tracer_record_build_format (GstTracerRecord * self)
{
  GstStructure *structure = self->spec;
  GString *s;
  gchar *name = (gchar *) g_quark_to_string (structure->name);
  gchar *p;

  g_return_if_fail (g_str_has_suffix (name, ".class"));

  /* announce the format */
  GST_TRACE ("%" GST_PTR_FORMAT, structure);

  /* cut off '.class' suffix */
  name = g_strdup (name);
  p = strrchr (name, '.');
  g_assert (p != NULL);
  *p = '\0';

  s = g_string_sized_new (STRUCTURE_ESTIMATED_STRING_LEN (structure));
  g_string_append (s, name);
  gst_structure_foreach (structure, build_field_template, s);
  g_string_append_c (s, ';');

  self->format = g_string_free (s, FALSE);
  GST_DEBUG ("new format string: %s", self->format);
  g_free (name);
}

static void
gst_tracer_record_dispose (GObject * object)
{
  GstTracerRecord *self = GST_TRACER_RECORD (object);

  if (self->spec) {
    gst_structure_free (self->spec);
    self->spec = NULL;
  }
  g_free (self->format);
  self->format = NULL;
}

static void
gst_tracer_record_class_init (GstTracerRecordClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gobject_class->dispose = gst_tracer_record_dispose;
}

static void
gst_tracer_record_init (GstTracerRecord * self)
{
}

/**
 * gst_tracer_record_new:
 * @name: name of new record, must end on ".class".
 * @firstfield: name of first field to set
 * @...: additional arguments

 *
 * Create a new tracer record. The record instance can be used to efficiently
 * log entries using gst_tracer_record_log().
 *
 * The @name without the ".class" suffix will be used for the log records.
 * There must be fields for each value that gets logged where the field name is
 * the value name. The field must be a #GstStructure describing the value. The
 * sub structure must contain a field called 'type' of %G_TYPE_GTYPE that
 * contains the GType of the value. The resulting #GstTracerRecord will take
 * ownership of the field structures.
 *
 * The way to deal with optional values is to log an additional boolean before
 * the optional field, that if %TRUE signals that the optional field is valid
 * and %FALSE signals that the optional field should be ignored. One must still
 * log a placeholder value for the optional field though. Please also note, that
 * pointer type values must not be NULL - the underlying serialisation can not
 * handle that right now.
 *
 * > Please note that this is still under discussion and subject to change.
 *
 * Returns: (transfer full): a new #GstTracerRecord
 *
 * Since: 1.8
 */
GstTracerRecord *
gst_tracer_record_new (const gchar * name, const gchar * firstfield, ...)
{
  GstTracerRecord *self;
  GstStructure *structure;
  va_list varargs;
  gchar *err = NULL;
  GType type;
  GQuark id;

  va_start (varargs, firstfield);
  structure = gst_structure_new_empty (name);

  while (firstfield) {
    GValue val = { 0, };

    id = g_quark_from_string (firstfield);
    type = va_arg (varargs, GType);

    /* all fields passed here must be GstStructures which we take over */
    if (type != GST_TYPE_STRUCTURE) {
      GST_ERROR ("expected field of type GstStructure, but %s is %s",
          firstfield, g_type_name (type));
      va_end (varargs);
      gst_structure_free (structure);
      return NULL;
    }

    G_VALUE_COLLECT_INIT (&val, type, varargs, G_VALUE_NOCOPY_CONTENTS, &err);
    if (G_UNLIKELY (err)) {
      g_critical ("%s", err);
      g_free (err);
      break;
    }
    /* give ownership of the GstStructure "value" collected from varargs
     * to this structure by unsetting the NOCOPY_CONTENTS collect-flag.
     * see boxed_proxy_collect_value in glib's gobject/gboxed.c */
    val.data[1].v_uint &= ~G_VALUE_NOCOPY_CONTENTS;
    gst_structure_id_take_value (structure, id, &val);

    firstfield = va_arg (varargs, gchar *);
  }
  va_end (varargs);

  self = g_object_new (GST_TYPE_TRACER_RECORD, NULL);

  /* Clear floating flag */
  gst_object_ref_sink (self);

  self->spec = structure;
  gst_tracer_record_build_format (self);

  return self;
}

#ifndef GST_DISABLE_GST_DEBUG
/**
 * gst_tracer_record_log:
 * @self: the tracer-record
 * @...: the args as described in the spec-
 *
 * Serialzes the trace event into the log.
 *
 * Right now this is using the gstreamer debug log with the level TRACE (7) and
 * the category "GST_TRACER".
 *
 * > Please note that this is still under discussion and subject to change.
 *
 * Since: 1.8
 */
void
gst_tracer_record_log (GstTracerRecord * self, ...)
{
  va_list var_args;

  /*
   * does it make sense to use the {file, line, func} from the tracer hook?
   * a)
   * - we'd need to pass them in the macros to gst_tracer_dispatch()
   * - and each tracer needs to grab them from the va_list and pass them here
   * b)
   * - we create a context in dispatch, pass that to the tracer
   * - and the tracer will pass that here
   * ideally we also use *our* ts instead of the one that
   * gst_debug_log_default() will pick
   */

  va_start (var_args, self);
  if (G_LIKELY (GST_LEVEL_TRACE <= _gst_debug_min)) {
    gst_debug_log_valist (GST_CAT_DEFAULT, GST_LEVEL_TRACE, "", "", 0, NULL,
        self->format, var_args);
  }
  va_end (var_args);
}
#endif
