/* GStreamer
 * Copyright (C) 2013 Stefan Sauer <ensonic@users.sf.net>
 *
 * gsttracerutils.c: tracing subsystem
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

/* Tracing subsystem:
 *
 * The tracing subsystem provides hooks in the core library and API for modules
 * to attach to them.
 *
 * The user can activate tracers by setting the environment variable GST_TRACE
 * to a ';' separated list of tracers.
 *
 * Note that instantiating tracers at runtime is possible but is not thread safe
 * and needs to be done before any pipeline state is set to PAUSED.
 */

#include "gst_private.h"
#include "gsttracer.h"
#include "gsttracerfactory.h"
#include "gstvalue.h"
#include "gsttracerutils.h"

#ifndef GST_DISABLE_GST_TRACER_HOOKS

/* tracer quarks */

/* These strings must match order and number declared in the GstTracerQuarkId
 * enum in gsttracerutils.h! */
static const gchar *_quark_strings[] = {
  "pad-push-pre", "pad-push-post", "pad-push-list-pre", "pad-push-list-post",
  "pad-pull-range-pre", "pad-pull-range-post", "pad-push-event-pre",
  "pad-push-event-post", "pad-query-pre", "pad-query-post",
  "element-post-message-pre",
  "element-post-message-post", "element-query-pre", "element-query-post",
  "element-new", "element-add-pad", "element-remove-pad",
  "bin-add-pre", "bin-add-post", "bin-remove-pre", "bin-remove-post",
  "pad-link-pre", "pad-link-post", "pad-unlink-pre", "pad-unlink-post",
  "element-change-state-pre", "element-change-state-post",
  "mini-object-created", "mini-object-destroyed", "object-created",
  "object-destroyed", "mini-object-reffed", "mini-object-unreffed",
  "object-reffed", "object-unreffed", "plugin-feature-loaded",
  "pad-chain-pre", "pad-chain-post", "pad-chain-list-pre",
  "pad-chain-list-post", "pad-send-event-pre", "pad-send-event-post",
  "memory-init", "memory-free-pre", "memory-free-post",
};

GQuark _priv_gst_tracer_quark_table[GST_TRACER_QUARK_MAX];

/* tracing helpers */

gboolean _priv_tracer_enabled = FALSE;
GHashTable *_priv_tracers = NULL;

static gchar *
list_available_tracer_properties (GObjectClass * class)
{
  GParamSpec **properties;
  guint n_properties;
  GString *props_str;
  guint i;

  props_str = g_string_new (NULL);
  properties = g_object_class_list_properties (class, &n_properties);

  if (n_properties == 0) {
    g_string_append (props_str, "No properties available");
    g_free (properties);
    return g_string_free (props_str, FALSE);
  }

  g_string_append (props_str, "Available properties:");

  for (i = 0; i < n_properties; i++) {
    GParamSpec *prop = properties[i];

    if (!((prop->flags & G_PARAM_CONSTRUCT)
            || (prop->flags & G_PARAM_CONSTRUCT_ONLY))
        || !(prop->flags & G_PARAM_WRITABLE))
      continue;

    if (!g_strcmp0 (g_param_spec_get_name (prop), "parent"))
      continue;
    if (!g_strcmp0 (g_param_spec_get_name (prop), "params"))
      continue;

    const gchar *type_name = G_PARAM_SPEC_TYPE_NAME (prop);
    GValue default_value = G_VALUE_INIT;

    /* Get default value if possible */
    g_value_init (&default_value, prop->value_type);
    g_param_value_set_default (prop, &default_value);
    gchar *default_str = g_strdup_value_contents (&default_value);

    g_string_append_printf (props_str,
        "\n  '%s' (%s) (Default: %s): %s",
        g_param_spec_get_name (prop),
        type_name,
        default_str,
        g_param_spec_get_blurb (prop) ? g_param_spec_get_blurb (prop) :
        "(no description available)");

    g_free (default_str);
    g_value_unset (&default_value);
  }

  g_free (properties);
  return g_string_free (props_str, FALSE);
}

static void
gst_tracer_utils_create_tracer (GstTracerFactory * factory, const gchar * name,
    const gchar * params)
{
  gchar *available_props = NULL;
  GObjectClass *gobject_class = g_type_class_ref (factory->type);
  GstTracer *tracer = NULL;
  const gchar **names = NULL;
  GValue *values = NULL;
  gint n_properties = 1;
  GstStructure *structure = NULL;

  if (gst_tracer_class_uses_structure_params (GST_TRACER_CLASS (gobject_class))) {
    GST_DEBUG ("Use structure parameters for %s", params);

    if (!params) {
      n_properties = 0;
      goto create;
    }

    gchar *struct_str = g_strdup_printf ("%s,%s", name, params);
    structure = gst_structure_from_string (struct_str, NULL);
    g_free (struct_str);

    if (!structure) {
      available_props = list_available_tracer_properties (gobject_class);
      g_warning
          ("Can't instantiate `%s` tracer: invalid parameters '%s'\n  %s\n",
          name, params, available_props);
      goto done;
    }
    n_properties = gst_structure_n_fields (structure);

    names = g_new0 (const gchar *, n_properties);
    values = g_new0 (GValue, n_properties);
    for (gint i = 0; i < n_properties; i++) {
      const gchar *field_name = gst_structure_nth_field_name (structure, i);
      const GValue *field_value =
          gst_structure_get_value (structure, field_name);
      GParamSpec *pspec =
          g_object_class_find_property (gobject_class, field_name);

      if (!pspec) {
        available_props = list_available_tracer_properties (gobject_class);
        g_warning
            ("Can't instantiate `%s` tracer: property '%s' not found\n  %s\n",
            name, field_name, available_props);
        goto done;
      }

      if (G_VALUE_TYPE (field_value) == pspec->value_type) {
        names[i] = field_name;
        g_value_init (&values[i], G_VALUE_TYPE (field_value));
        g_value_copy (field_value, &values[i]);
      } else if (G_VALUE_TYPE (field_value) == G_TYPE_STRING) {
        names[i] = field_name;
        g_value_init (&values[i], G_PARAM_SPEC_VALUE_TYPE (pspec));
        if (!gst_value_deserialize_with_pspec (&values[i],
                g_value_get_string (field_value), pspec)) {
          available_props = list_available_tracer_properties (gobject_class);
          g_warning
              ("Can't instantiate `%s` tracer: invalid property '%s' value: '%s'\n  %s\n",
              name, field_name, g_value_get_string (field_value),
              available_props);
          goto done;
        }
      } else {
        available_props = list_available_tracer_properties (gobject_class);
        g_warning
            ("Can't instantiate `%s` tracer: property '%s' type mismatch, expected %s, got %s\n  %s\n",
            name, field_name, g_type_name (pspec->value_type),
            g_type_name (G_VALUE_TYPE (field_value)), available_props);
        goto done;
      }
    }
  } else {
    names = g_new0 (const gchar *, n_properties);
    names[0] = (const gchar *) "params";
    values = g_new0 (GValue, 1);
    g_value_init (&values[0], G_TYPE_STRING);
    g_value_set_string (&values[0], params);
  }
  GST_INFO_OBJECT (factory, "creating tracer: type-id=%u",
      (guint) factory->type);

create:
  tracer =
      GST_TRACER (g_object_new_with_properties (factory->type,
          n_properties, names, values));

done:
  g_free (available_props);

  if (structure)
    gst_structure_free (structure);

  if (values) {
    for (gint j = 0; j < n_properties; j++) {
      if (G_VALUE_TYPE (&values[j]) != G_TYPE_INVALID)
        g_value_unset (&values[j]);
    }
  }

  g_free (names);
  g_free (values);

  if (tracer) {
    /* Clear floating flag */
    gst_object_ref_sink (tracer);

    /* tracers register them self to the hooks */
    gst_object_unref (tracer);

  }

  g_type_class_unref (gobject_class);
}

/* Initialize the tracing system */
void
_priv_gst_tracing_init (void)
{
  gint i = 0;
  const gchar *env = g_getenv ("GST_TRACERS");

  /* We initialize the tracer sub system even if the end
   * user did not activate it through the env variable
   * so that external tools can use it anyway */
  GST_DEBUG ("Initializing GstTracer");
  _priv_tracers = g_hash_table_new (NULL, NULL);

  if (G_N_ELEMENTS (_quark_strings) != GST_TRACER_QUARK_MAX)
    g_warning ("the quark table is not consistent! %d != %d",
        (gint) G_N_ELEMENTS (_quark_strings), GST_TRACER_QUARK_MAX);

  for (i = 0; i < GST_TRACER_QUARK_MAX; i++) {
    _priv_gst_tracer_quark_table[i] =
        g_quark_from_static_string (_quark_strings[i]);
  }

  if (env != NULL && *env != '\0') {
    GstRegistry *registry = gst_registry_get ();
    GstPluginFeature *feature;
    GstTracerFactory *factory;
    gchar **t = g_strsplit_set (env, ";", 0);
    gchar *params;

    GST_INFO ("enabling tracers: '%s'", env);
    i = 0;
    while (t[i]) {
      // check t[i] for params
      if ((params = strchr (t[i], '('))) {
        // params can contain multiple '(' when using this kind of parameter: 'max-buffer-size=(uint)5'
        guint n_par = 1, j;
        gchar *end = NULL;

        for (j = 1; params[j] != '\0'; j++) {
          if (params[j] == '(')
            n_par++;
          else if (params[j] == ')') {
            n_par--;
            if (n_par == 0) {
              end = &params[j];
              break;
            }
          }
        }
        *params = '\0';
        params++;
        if (end)
          *end = '\0';
      } else {
        params = NULL;
      }

      GST_INFO ("checking tracer: '%s'", t[i]);

      if ((feature = gst_registry_lookup_feature (registry, t[i]))) {
        factory = GST_TRACER_FACTORY (gst_plugin_feature_load (feature));
        if (factory) {
          gst_tracer_utils_create_tracer (factory, t[i], params);
          gst_object_unref (factory);
        } else {
          g_warning ("loading plugin containing feature %s failed!", t[i]);
        }
        gst_object_unref (feature);
      } else if (t[i][0] != '\0') {
        g_warning ("no tracer named '%s'", t[i]);
      }
      i++;
    }
    g_strfreev (t);
  }
}

void
_priv_gst_tracing_deinit (void)
{
  GList *h_list, *h_node, *t_node;
  GstTracerHook *hook;

  _priv_tracer_enabled = FALSE;
  if (!_priv_tracers)
    return;

  /* shutdown tracers for final reports */
  h_list = g_hash_table_get_values (_priv_tracers);
  for (h_node = h_list; h_node; h_node = g_list_next (h_node)) {
    for (t_node = h_node->data; t_node; t_node = g_list_next (t_node)) {
      hook = (GstTracerHook *) t_node->data;
      gst_object_unref (hook->tracer);
      g_free (hook);
    }
    g_list_free (h_node->data);
  }
  g_list_free (h_list);
  g_hash_table_destroy (_priv_tracers);
  _priv_tracers = NULL;
}

static void
gst_tracing_register_hook_id (GstTracer * tracer, GQuark detail, GCallback func)
{
  gpointer key = GINT_TO_POINTER (detail);
  GList *list = g_hash_table_lookup (_priv_tracers, key);
  GstTracerHook *hook = g_new0 (GstTracerHook, 1);
  hook->tracer = gst_object_ref (tracer);
  hook->func = func;

  list = g_list_prepend (list, hook);
  g_hash_table_replace (_priv_tracers, key, list);
  GST_DEBUG ("registering tracer for '%s', list.len=%d",
      (detail ? g_quark_to_string (detail) : "*"), g_list_length (list));
  _priv_tracer_enabled = TRUE;
}

/**
 * gst_tracing_register_hook:
 * @tracer: the tracer
 * @detail: the detailed hook
 * @func: (scope async): the callback
 *
 * Register @func to be called when the trace hook @detail is getting invoked.
 * Use %NULL for @detail to register to all hooks.
 *
 * Since: 1.8
 */
void
gst_tracing_register_hook (GstTracer * tracer, const gchar * detail,
    GCallback func)
{
  gst_tracing_register_hook_id (tracer, g_quark_try_string (detail), func);
}

/**
 * gst_tracing_get_active_tracers:
 *
 * Get a list of all active tracer objects owned by the tracing framework for
 * the entirety of the run-time of the process or till gst_deinit() is called.
 *
 * Returns: (transfer full) (element-type Gst.Tracer): A #GList of
 * #GstTracer objects
 *
 * Since: 1.18
 */
GList *
gst_tracing_get_active_tracers (void)
{
  GList *tracers, *h_list, *h_node, *t_node;
  GstTracerHook *hook;

  if (!_priv_tracer_enabled || !_priv_tracers)
    return NULL;

  tracers = NULL;
  h_list = g_hash_table_get_values (_priv_tracers);
  for (h_node = h_list; h_node; h_node = g_list_next (h_node)) {
    for (t_node = h_node->data; t_node; t_node = g_list_next (t_node)) {
      hook = (GstTracerHook *) t_node->data;
      /* Skip duplicate tracers from different hooks. This function is O(n), but
       * that should be fine since the number of tracers enabled on a process
       * should be small. */
      if (g_list_index (tracers, hook->tracer) >= 0)
        continue;
      tracers = g_list_prepend (tracers, gst_object_ref (hook->tracer));
    }
  }
  g_list_free (h_list);

  return tracers;
}

#else /* !GST_DISABLE_GST_TRACER_HOOKS */

void
gst_tracing_register_hook (GstTracer * tracer, const gchar * detail,
    GCallback func)
{
}

GList *
gst_tracing_get_active_tracers (void)
{
  return NULL;
}
#endif /* GST_DISABLE_GST_TRACER_HOOKS */
