/* GStreamer
 * Copyright (C) 1999,2000 Erik Walthinsen <omega@cse.ogi.edu>
 *                    2000 Wim Taymans <wtay@chello.be>
 *
 * gst_private.h: Private header for within libgst
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

#ifndef __GST_PRIVATE_H__
#define __GST_PRIVATE_H__

#ifdef HAVE_CONFIG_H
# ifndef GST_LICENSE   /* don't include config.h twice, it has no guards */
#  include "config.h"
# endif
#endif

#include <glib.h>

#include <stdlib.h>
#include <string.h>

#ifdef GSTREAMER_LITE
#include <glib/gprintf.h>       /* g_vasprintf */
#endif // GSTREAMER_LITE

/* Needed for GST_API */
#include "gst/gstconfig.h"

/* Needed for GstRegistry * */
#include "gstregistry.h"
#include "gststructure.h"

/* we need this in pretty much all files */
#include "gstinfo.h"

/* for the flags in the GstPluginDep structure below */
#include "gstplugin.h"

/* for the pad cache */
#include "gstpad.h"

/* for GstElement */
#include "gstelement.h"

#ifndef GSTREAMER_LITE
/* for GstDeviceProvider */
#include "gstdeviceprovider.h"
#endif // GSTREAMER_LITE

/* for GstToc */
#include "gsttoc.h"

#include "gstdatetime.h"

#include "gsttracerutils.h"

G_BEGIN_DECLS

/* used by gstparse.c and grammar.y */
struct _GstParseContext {
  GList * missing_elements;
};

/* used by gstplugin.c and gstregistrybinary.c */
typedef struct {
  /* details registered via gst_plugin_add_dependency() */
  GstPluginDependencyFlags  flags;
  gchar **env_vars;
  gchar **paths;
  gchar **names;

  /* information saved from the last time the plugin was loaded (-1 = unset) */
  guint   env_hash;  /* hash of content of environment variables in env_vars */
  guint   stat_hash; /* hash of stat() on all relevant files and directories */
} GstPluginDep;

struct _GstPluginPrivate {
  GList *deps;                 /* list of GstPluginDep structures */
  GstStructure *status_info;
  GstStructure *cache_data;
};

/* Private function for getting plugin features directly */
GList *
_priv_plugin_get_features(GstRegistry *registry, GstPlugin *plugin);

/* Needed by GstMeta (to access meta seq) and GstBuffer (create/free/iterate) */
typedef struct _GstMetaItem GstMetaItem;
struct _GstMetaItem {
  GstMetaItem *next;
  guint64 seq_num;
  GstMeta meta;
};

/* FIXME: could rename all priv_gst_* functions to __gst_* now */
G_GNUC_INTERNAL  gboolean priv_gst_plugin_loading_have_whitelist (void);

G_GNUC_INTERNAL  guint32  priv_gst_plugin_loading_get_whitelist_hash (void);

G_GNUC_INTERNAL  gboolean priv_gst_plugin_desc_is_whitelisted (const GstPluginDesc * desc,
                                                               const gchar   * filename);

G_GNUC_INTERNAL  gboolean _priv_plugin_deps_env_vars_changed (GstPlugin * plugin);

G_GNUC_INTERNAL  gboolean _priv_plugin_deps_files_changed (GstPlugin * plugin);

/* init functions called from gst_init(). */
G_GNUC_INTERNAL  void  _priv_gst_quarks_initialize (void);
G_GNUC_INTERNAL  void  _priv_gst_mini_object_initialize (void);
G_GNUC_INTERNAL  void  _priv_gst_memory_initialize (void);
G_GNUC_INTERNAL  void  _priv_gst_allocator_initialize (void);
G_GNUC_INTERNAL  void  _priv_gst_buffer_initialize (void);
G_GNUC_INTERNAL  void  _priv_gst_buffer_list_initialize (void);
G_GNUC_INTERNAL  void  _priv_gst_structure_initialize (void);
G_GNUC_INTERNAL  void  _priv_gst_caps_initialize (void);
G_GNUC_INTERNAL  void  _priv_gst_caps_features_initialize (void);
G_GNUC_INTERNAL  void  _priv_gst_event_initialize (void);
G_GNUC_INTERNAL  void  _priv_gst_format_initialize (void);
G_GNUC_INTERNAL  void  _priv_gst_message_initialize (void);
G_GNUC_INTERNAL  void  _priv_gst_meta_initialize (void);
G_GNUC_INTERNAL  void  _priv_gst_plugin_initialize (void);
G_GNUC_INTERNAL  void  _priv_gst_query_initialize (void);
G_GNUC_INTERNAL  void  _priv_gst_sample_initialize (void);
G_GNUC_INTERNAL  void  _priv_gst_tag_initialize (void);
G_GNUC_INTERNAL  void  _priv_gst_value_initialize (void);
G_GNUC_INTERNAL  void  _priv_gst_debug_init (void);
G_GNUC_INTERNAL  void  _priv_gst_context_initialize (void);
G_GNUC_INTERNAL  void  _priv_gst_toc_initialize (void);
G_GNUC_INTERNAL  void  _priv_gst_date_time_initialize (void);
G_GNUC_INTERNAL  void  _priv_gst_plugin_feature_rank_initialize (void);

/* cleanup functions called from gst_deinit(). */
G_GNUC_INTERNAL  void  _priv_gst_allocator_cleanup (void);
G_GNUC_INTERNAL  void  _priv_gst_caps_features_cleanup (void);
G_GNUC_INTERNAL  void  _priv_gst_caps_cleanup (void);
G_GNUC_INTERNAL  void  _priv_gst_debug_cleanup (void);
G_GNUC_INTERNAL  void  _priv_gst_meta_cleanup (void);

/* called from gst_task_cleanup_all(). */
G_GNUC_INTERNAL  void  _priv_gst_element_cleanup (void);

/* Private registry functions */
G_GNUC_INTERNAL
gboolean _priv_gst_registry_remove_cache_plugins (GstRegistry *registry);

G_GNUC_INTERNAL  void _priv_gst_registry_cleanup (void);

GST_API
gboolean _gst_plugin_loader_client_run (const gchar * pipe_name);

G_GNUC_INTERNAL  GstPlugin * _priv_gst_plugin_load_file_for_registry (const gchar *filename,
                                                                      GstRegistry * registry,
                                                                      GError** error);

/* GValue serialization/deserialization */

G_GNUC_INTERNAL const char * _priv_gst_value_gtype_to_abbr (GType type);

G_GNUC_INTERNAL gboolean _priv_gst_value_parse_string (gchar * s, gchar ** end, gchar ** next, gboolean unescape);
G_GNUC_INTERNAL gboolean _priv_gst_value_parse_simple_string (gchar * str, gchar ** end);
G_GNUC_INTERNAL gboolean _priv_gst_value_parse_value (gchar * str, gchar ** after, GValue * value, GType default_type, GParamSpec *pspec);
G_GNUC_INTERNAL gchar * _priv_gst_value_serialize_any_list (const GValue * value, const gchar * begin, const gchar * end, gboolean print_type, GstSerializeFlags flags);

/* Used in GstBin for manual state handling */
G_GNUC_INTERNAL  void _priv_gst_element_state_changed (GstElement *element,
                      GstState oldstate, GstState newstate, GstState pending);

/* used in both gststructure.c and gstcaps.c; numbers are completely made up */
#define STRUCTURE_ESTIMATED_STRING_LEN(s) (16 + gst_structure_n_fields(s) * 22)
#define FEATURES_ESTIMATED_STRING_LEN(s) (16 + gst_caps_features_get_size(s) * 14)

G_GNUC_INTERNAL
gboolean  priv_gst_structure_append_to_gstring (const GstStructure * structure,
                                                GString            * s,
                                                GstSerializeFlags flags);
G_GNUC_INTERNAL
gboolean priv__gst_structure_append_template_to_gstring (GQuark field_id,
                                                        const GValue *value,
                                                        gpointer user_data);

G_GNUC_INTERNAL
void priv_gst_caps_features_append_to_gstring (const GstCapsFeatures * features, GString *s);

G_GNUC_INTERNAL
gboolean priv_gst_structure_parse_name (gchar * str, gchar **start, gchar ** end, gchar ** next, gboolean check_valid);
G_GNUC_INTERNAL
gboolean priv_gst_structure_parse_fields (gchar *str, gchar ** end, GstStructure *structure);

/* used in gstvalue.c and gststructure.c */

#define GST_WRAPPED_PTR_FORMAT     "p\aa"

G_GNUC_INTERNAL
gchar *priv_gst_string_take_and_wrap (gchar * s);

/* registry cache backends */
G_GNUC_INTERNAL
gboolean    priv_gst_registry_binary_read_cache (GstRegistry * registry, const char *location);

G_GNUC_INTERNAL
gboolean    priv_gst_registry_binary_write_cache  (GstRegistry * registry, GList * plugins, const char *location);


G_GNUC_INTERNAL
void      __gst_element_factory_add_static_pad_template (GstElementFactory    * elementfactory,
                                                         GstStaticPadTemplate * templ);

G_GNUC_INTERNAL
void      __gst_element_factory_add_interface           (GstElementFactory    * elementfactory,
                                                         const gchar          * interfacename);

/* used in gstvalue.c and gststructure.c */
#define GST_ASCII_IS_STRING(c) (g_ascii_isalnum((c)) || ((c) == '_') || \
    ((c) == '-') || ((c) == '+') || ((c) == '/') || ((c) == ':') || \
    ((c) == '.'))

/* This is only meant for internal uses */
G_GNUC_INTERNAL
gint __gst_date_time_compare (const GstDateTime * dt1, const GstDateTime * dt2);

G_GNUC_INTERNAL
gchar * __gst_date_time_serialize (GstDateTime * datetime, gboolean with_usecs);

/* For use in gstdebugutils */
G_GNUC_INTERNAL
GstCapsFeatures * __gst_caps_get_features_unchecked (const GstCaps * caps, guint idx);

#ifndef GST_DISABLE_REGISTRY
/* Secret variable to initialise gst without registry cache */

GST_API gboolean _gst_disable_registry_cache;
#endif

/* Secret variable to let the plugin scanner use the same base path
 * as the main application in order to determine dependencies */
GST_API gchar *_gst_executable_path;

/* provide inline gst_g_value_get_foo_unchecked(), used in gststructure.c */
#define DEFINE_INLINE_G_VALUE_GET_UNCHECKED(ret_type,name_type,v_field) \
static inline ret_type                                                  \
gst_g_value_get_##name_type##_unchecked (const GValue *value)           \
{                                                                       \
  return value->data[0].v_field;                                        \
}

DEFINE_INLINE_G_VALUE_GET_UNCHECKED(gboolean,boolean,v_int)
DEFINE_INLINE_G_VALUE_GET_UNCHECKED(gint,int,v_int)
DEFINE_INLINE_G_VALUE_GET_UNCHECKED(guint,uint,v_uint)
DEFINE_INLINE_G_VALUE_GET_UNCHECKED(gint64,int64,v_int64)
DEFINE_INLINE_G_VALUE_GET_UNCHECKED(guint64,uint64,v_uint64)
DEFINE_INLINE_G_VALUE_GET_UNCHECKED(gfloat,float,v_float)
DEFINE_INLINE_G_VALUE_GET_UNCHECKED(gdouble,double,v_double)
DEFINE_INLINE_G_VALUE_GET_UNCHECKED(const gchar *,string,v_pointer)


/*** debugging categories *****************************************************/

#ifndef GST_REMOVE_GST_DEBUG

GST_API GstDebugCategory *GST_CAT_GST_INIT;
GST_API GstDebugCategory *GST_CAT_MEMORY;
GST_API GstDebugCategory *GST_CAT_PARENTAGE;
GST_API GstDebugCategory *GST_CAT_STATES;
GST_API GstDebugCategory *GST_CAT_SCHEDULING;
GST_API GstDebugCategory *GST_CAT_BUFFER;
GST_API GstDebugCategory *GST_CAT_BUFFER_LIST;
GST_API GstDebugCategory *GST_CAT_BUS;
GST_API GstDebugCategory *GST_CAT_CAPS;
GST_API GstDebugCategory *GST_CAT_CLOCK;
GST_API GstDebugCategory *GST_CAT_ELEMENT_PADS;
GST_API GstDebugCategory *GST_CAT_PADS;
GST_API GstDebugCategory *GST_CAT_PERFORMANCE;
GST_API GstDebugCategory *GST_CAT_PIPELINE;
GST_API GstDebugCategory *GST_CAT_PLUGIN_LOADING;
GST_API GstDebugCategory *GST_CAT_PLUGIN_INFO;
GST_API GstDebugCategory *GST_CAT_PROPERTIES;
GST_API GstDebugCategory *GST_CAT_NEGOTIATION;
GST_API GstDebugCategory *GST_CAT_REFCOUNTING;
GST_API GstDebugCategory *GST_CAT_ERROR_SYSTEM;
GST_API GstDebugCategory *GST_CAT_EVENT;
GST_API GstDebugCategory *GST_CAT_MESSAGE;
GST_API GstDebugCategory *GST_CAT_PARAMS;
GST_API GstDebugCategory *GST_CAT_CALL_TRACE;
GST_API GstDebugCategory *GST_CAT_SIGNAL;
GST_API GstDebugCategory *GST_CAT_PROBE;
GST_API GstDebugCategory *GST_CAT_REGISTRY;
GST_API GstDebugCategory *GST_CAT_QOS;
GST_API GstDebugCategory *GST_CAT_META;
GST_API GstDebugCategory *GST_CAT_LOCKING;
GST_API GstDebugCategory *GST_CAT_CONTEXT;

/* Categories that should be completely private to
 * libgstreamer should be done like this: */
#define GST_CAT_POLL _priv_GST_CAT_POLL
extern GstDebugCategory *_priv_GST_CAT_POLL;

#define GST_CAT_PROTECTION _priv_GST_CAT_PROTECTION
extern GstDebugCategory *_priv_GST_CAT_PROTECTION;

extern GstClockTime _priv_gst_start_time;

#else

#define GST_CAT_GST_INIT         NULL
#define GST_CAT_AUTOPLUG         NULL
#define GST_CAT_AUTOPLUG_ATTEMPT NULL
#define GST_CAT_PARENTAGE        NULL
#define GST_CAT_STATES           NULL
#define GST_CAT_SCHEDULING       NULL
#define GST_CAT_DATAFLOW         NULL
#define GST_CAT_BUFFER           NULL
#define GST_CAT_BUFFER_LIST      NULL
#define GST_CAT_BUS              NULL
#define GST_CAT_CAPS             NULL
#define GST_CAT_CLOCK            NULL
#define GST_CAT_ELEMENT_PADS     NULL
#define GST_CAT_PADS             NULL
#define GST_CAT_PERFORMANCE      NULL
#define GST_CAT_PIPELINE         NULL
#define GST_CAT_PLUGIN_LOADING   NULL
#define GST_CAT_PLUGIN_INFO      NULL
#define GST_CAT_PROPERTIES       NULL
#define GST_CAT_NEGOTIATION      NULL
#define GST_CAT_REFCOUNTING      NULL
#define GST_CAT_ERROR_SYSTEM     NULL
#define GST_CAT_EVENT            NULL
#define GST_CAT_MESSAGE          NULL
#define GST_CAT_PARAMS           NULL
#define GST_CAT_CALL_TRACE       NULL
#define GST_CAT_SIGNAL           NULL
#define GST_CAT_PROBE            NULL
#define GST_CAT_REGISTRY         NULL
#define GST_CAT_QOS              NULL
#define GST_CAT_TYPES            NULL
#define GST_CAT_POLL             NULL
#define GST_CAT_META             NULL
#define GST_CAT_LOCKING          NULL
#define GST_CAT_CONTEXT          NULL
#define GST_CAT_PROTECTION       NULL

#endif

#ifdef GSTREAMER_LITE
// In pre 1.22.6 __gst_vasprintf was defined as __gst_info_fallback_vasprintf
// when debug subsystem is diabled and starting from 1.26.0 upgrade we need to
// use __gst_vasprintf from gst/printf. As of now I do not see need to pull
// gst/printf code, since it is needed to handle old pointer extension formats
// such as %P and %Q and then call g_vasprintf. See __gst_info_fallback_vasprintf
// implementation in pre 1.22.6. Since we do not use old extension formats, we
// will use g_vasprintf direcly.
#define __gst_vasprintf g_vasprintf
#endif // GSTREAMER_LITE

/**** objects made opaque until the private bits have been made private ****/

#include <gmodule.h>
#include <time.h> /* time_t */
#include <sys/types.h> /* off_t */
#include <sys/stat.h> /* off_t */

typedef struct _GstPluginPrivate GstPluginPrivate;

struct _GstPlugin {
  GstObject       object;

  /*< private >*/
  GstPluginDesc desc;

  gchar * filename;
  gchar * basename;       /* base name (non-dir part) of plugin path */

  GModule * module;   /* contains the module if plugin is loaded */

  off_t         file_size;
  time_t        file_mtime;
  gboolean      registered;     /* TRUE when the registry has seen a filename
                                 * that matches the plugin's basename */

  GstPluginPrivate *priv;

  gpointer _gst_reserved[GST_PADDING];
};

struct _GstPluginClass {
  GstObjectClass  object_class;

  /*< private >*/
  gpointer _gst_reserved[GST_PADDING];
};

struct _GstPluginFeature {
  GstObject      object;

  /*< private >*/
  gboolean       loaded;
  guint          rank;

  const gchar   *plugin_name;
  GstPlugin     *plugin;      /* weak ref */

  /*< private >*/
  gpointer _gst_reserved[GST_PADDING];
};

struct _GstPluginFeatureClass {
  GstObjectClass        parent_class;

  /*< private >*/
  gpointer _gst_reserved[GST_PADDING];
};

#include "gsttypefind.h"

struct _GstTypeFindFactory {
  GstPluginFeature              feature;
  /* <private> */

  GstTypeFindFunction           function;
  gchar **                      extensions;
  GstCaps *                     caps;

  gpointer                      user_data;
  GDestroyNotify                user_data_notify;

  gpointer _gst_reserved[GST_PADDING];
};

struct _GstTypeFindFactoryClass {
  GstPluginFeatureClass         parent;
  /* <private> */

  gpointer _gst_reserved[GST_PADDING];
};

struct _GstTracerFactory {
  GstPluginFeature              feature;
  /* <private> */

  GType                         type;

  /*
  gpointer                      user_data;
  GDestroyNotify                user_data_notify;
  */

  gpointer _gst_reserved[GST_PADDING];
};

struct _GstTracerFactoryClass {
  GstPluginFeatureClass         parent;
  /* <private> */

  gpointer _gst_reserved[GST_PADDING];
};

struct _GstElementFactory {
  GstPluginFeature      parent;

  GType                 type;                   /* unique GType of element or 0 if not loaded */

  gpointer              metadata;

  GList *               staticpadtemplates;     /* GstStaticPadTemplate list */
  guint                 numpadtemplates;

  /* URI interface stuff */
  GstURIType            uri_type;
  gchar **              uri_protocols;

  GList *               interfaces;             /* interface type names this element implements */

  /*< private >*/
  gpointer _gst_reserved[GST_PADDING];
};

struct _GstElementFactoryClass {
  GstPluginFeatureClass parent_class;

  gpointer _gst_reserved[GST_PADDING];
};

#ifndef GSTREAMER_LITE
struct _GstDeviceProviderFactory {
  GstPluginFeature           feature;
  /* <private> */

  GType                      type;              /* unique GType the device factory or 0 if not loaded */

  GstDeviceProvider         *provider;
  gpointer                   metadata;

  gpointer _gst_reserved[GST_PADDING];
};

struct _GstDeviceProviderFactoryClass {
  GstPluginFeatureClass         parent;
  /* <private> */

  gpointer _gst_reserved[GST_PADDING];
};
#endif // GSTREAMER_LITE

struct _GstDynamicTypeFactory {
  GstPluginFeature           feature;

  GType                      type; /* GType of the type, when loaded. 0 if not */
};

struct _GstDynamicTypeFactoryClass {
  GstPluginFeatureClass      parent;
};

/* privat flag used by GstBus / GstMessage */
#define GST_MESSAGE_FLAG_ASYNC_DELIVERY (GST_MINI_OBJECT_FLAG_LAST << 0)

/* private struct used by GstClock and GstSystemClock */
struct _GstClockEntryImpl
{
  GstClockEntry entry;
#if defined (GSTREAMER_LITE) && defined(LINUX)
  GWeakRef clock;
#else // GSTREAMER_LITE
  GWeakRef *clock;
#endif // GSTREAMER_LITE
  GDestroyNotify destroy_entry;
  gpointer padding[21];                 /* padding for allowing e.g. systemclock
                                         * to add data in lieu of overridable
                                         * virtual functions on the clock */
};

char * priv_gst_get_relocated_libgstreamer (void);
gint   priv_gst_count_directories (const char *filepath);

void priv_gst_clock_init (void);
GstClockTime priv_gst_get_monotonic_time (void);
GstClockTime priv_gst_get_real_time (void);

G_END_DECLS
#endif /* __GST_PRIVATE_H__ */
