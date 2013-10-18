/* GStreamer
 * Copyright (C) 1999,2000 Erik Walthinsen <omega@cse.ogi.edu>
 *                    2000 Wim Taymans <wtay@chello.be>
 *                    2003 Benjamin Otte <in7y118@public.uni-hamburg.de>
 * Copyright (C) 2008-2009 Tim-Philipp Müller <tim centricular net>
 *
 * gstinfo.c: debugging functions
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
 * SECTION:gstinfo
 * @short_description: Debugging and logging facilities
 * @see_also: #gstreamer-gstconfig, #gstreamer-Gst for command line parameters
 * and environment variables that affect the debugging output.
 *
 * GStreamer's debugging subsystem is an easy way to get information about what
 * the application is doing.  It is not meant for programming errors. Use GLib
 * methods (g_warning and friends) for that.
 *
 * The debugging subsystem works only after GStreamer has been initialized
 * - for example by calling gst_init().
 *
 * The debugging subsystem is used to log informational messages while the
 * application runs.  Each messages has some properties attached to it. Among
 * these properties are the debugging category, the severity (called "level"
 * here) and an optional #GObject it belongs to. Each of these messages is sent
 * to all registered debugging handlers, which then handle the messages.
 * GStreamer attaches a default handler on startup, which outputs requested
 * messages to stderr.
 *
 * Messages are output by using shortcut macros like #GST_DEBUG,
 * #GST_CAT_ERROR_OBJECT or similar. These all expand to calling gst_debug_log()
 * with the right parameters.
 * The only thing a developer will probably want to do is define his own
 * categories. This is easily done with 3 lines. At the top of your code,
 * declare
 * the variables and set the default category.
 * <informalexample>
 * <programlisting>
 * GST_DEBUG_CATEGORY_STATIC (my_category);     // define category (statically)
 * &hash;define GST_CAT_DEFAULT my_category     // set as default
 * </programlisting>
 * </informalexample>
 * After that you only need to initialize the category.
 * <informalexample>
 * <programlisting>
 * GST_DEBUG_CATEGORY_INIT (my_category, "my category",
 *                          0, "This is my very own");
 * </programlisting>
 * </informalexample>
 * Initialization must be done before the category is used first.
 * Plugins do this
 * in their plugin_init function, libraries and applications should do that
 * during their initialization.
 *
 * The whole debugging subsystem can be disabled at build time with passing the
 * --disable-gst-debug switch to configure. If this is done, every function,
 * macro and even structs described in this file evaluate to default values or
 * nothing at all.
 * So don't take addresses of these functions or use other tricks.
 * If you must do that for some reason, there is still an option.
 * If the debugging
 * subsystem was compiled out, #GST_DISABLE_GST_DEBUG is defined in
 * &lt;gst/gst.h&gt;,
 * so you can check that before doing your trick.
 * Disabling the debugging subsystem will give you a slight (read: unnoticeable)
 * speed increase and will reduce the size of your compiled code. The GStreamer
 * library itself becomes around 10% smaller.
 *
 * Please note that there are naming conventions for the names of debugging
 * categories. These are explained at GST_DEBUG_CATEGORY_INIT().
 */

#define GST_INFO_C
#include "gst_private.h"
#include "gstinfo.h"

#undef gst_debug_remove_log_function
#undef gst_debug_add_log_function

#ifndef GST_DISABLE_GST_DEBUG

#ifdef HAVE_DLFCN_H
#  include <dlfcn.h>
#endif
#ifdef HAVE_PRINTF_EXTENSION
#  include <printf.h>
#endif
#include <stdio.h>              /* fprintf */
#include <glib/gstdio.h>
#include <errno.h>
#ifdef HAVE_UNISTD_H
#  include <unistd.h>           /* getpid on UNIX */
#endif
#ifdef HAVE_PROCESS_H
#  include <process.h>          /* getpid on win32 */
#endif
#include <string.h>             /* G_VA_COPY */
#ifdef G_OS_WIN32
#  define WIN32_LEAN_AND_MEAN   /* prevents from including too many things */
#  include <windows.h>          /* GetStdHandle, windows console */
#endif

#include "gst_private.h"
#include "gstutils.h"
#include "gstquark.h"
#include "gstsegment.h"
#ifdef HAVE_VALGRIND_VALGRIND_H
#  include <valgrind/valgrind.h>
#endif
#include <glib/gprintf.h>       /* g_sprintf */

#endif /* !GST_DISABLE_GST_DEBUG */

/* we want these symbols exported even if debug is disabled, to maintain
 * ABI compatibility. Unless GST_REMOVE_DISABLED is defined. */
#if !defined(GST_DISABLE_GST_DEBUG) || !defined(GST_REMOVE_DISABLED)

/* disabled by default, as soon as some threshold is set > NONE,
 * it becomes enabled. */
gboolean __gst_debug_enabled = FALSE;
GstDebugLevel __gst_debug_min = GST_LEVEL_NONE;

GstDebugCategory *GST_CAT_DEFAULT = NULL;

GstDebugCategory *GST_CAT_GST_INIT = NULL;
GstDebugCategory *GST_CAT_AUTOPLUG = NULL;
GstDebugCategory *GST_CAT_AUTOPLUG_ATTEMPT = NULL;
GstDebugCategory *GST_CAT_PARENTAGE = NULL;
GstDebugCategory *GST_CAT_STATES = NULL;
GstDebugCategory *GST_CAT_SCHEDULING = NULL;

GstDebugCategory *GST_CAT_BUFFER = NULL;
GstDebugCategory *GST_CAT_BUFFER_LIST = NULL;
GstDebugCategory *GST_CAT_BUS = NULL;
GstDebugCategory *GST_CAT_CAPS = NULL;
GstDebugCategory *GST_CAT_CLOCK = NULL;
GstDebugCategory *GST_CAT_ELEMENT_PADS = NULL;
GstDebugCategory *GST_CAT_PADS = NULL;
GstDebugCategory *GST_CAT_PERFORMANCE = NULL;
GstDebugCategory *GST_CAT_PIPELINE = NULL;
GstDebugCategory *GST_CAT_PLUGIN_LOADING = NULL;
GstDebugCategory *GST_CAT_PLUGIN_INFO = NULL;
GstDebugCategory *GST_CAT_PROPERTIES = NULL;
GstDebugCategory *GST_CAT_TYPES = NULL;
GstDebugCategory *GST_CAT_XML = NULL;
GstDebugCategory *GST_CAT_NEGOTIATION = NULL;
GstDebugCategory *GST_CAT_REFCOUNTING = NULL;
GstDebugCategory *GST_CAT_ERROR_SYSTEM = NULL;
GstDebugCategory *GST_CAT_EVENT = NULL;
GstDebugCategory *GST_CAT_MESSAGE = NULL;
GstDebugCategory *GST_CAT_PARAMS = NULL;
GstDebugCategory *GST_CAT_CALL_TRACE = NULL;
GstDebugCategory *GST_CAT_SIGNAL = NULL;
GstDebugCategory *GST_CAT_PROBE = NULL;
GstDebugCategory *GST_CAT_REGISTRY = NULL;
GstDebugCategory *GST_CAT_QOS = NULL;
GstDebugCategory *_priv_GST_CAT_POLL = NULL;


#endif /* !defined(GST_DISABLE_GST_DEBUG) || !defined(GST_REMOVE_DISABLED) */

#ifndef GST_DISABLE_GST_DEBUG

/* underscore is to prevent conflict with GST_CAT_DEBUG define */
GST_DEBUG_CATEGORY_STATIC (_GST_CAT_DEBUG);

/* time of initialization, so we get useful debugging output times
 * FIXME: we use this in gstdebugutils.c, what about a function + macro to
 * get the running time: GST_DEBUG_RUNNING_TIME
 */
GstClockTime _priv_gst_info_start_time;

#if 0
#if defined __sgi__
#include <rld_interface.h>
typedef struct DL_INFO
{
  const char *dli_fname;
  void *dli_fbase;
  const char *dli_sname;
  void *dli_saddr;
  int dli_version;
  int dli_reserved1;
  long dli_reserved[4];
}
Dl_info;

#define _RLD_DLADDR             14
int dladdr (void *address, Dl_info * dl);

int
dladdr (void *address, Dl_info * dl)
{
  void *v;

  v = _rld_new_interface (_RLD_DLADDR, address, dl);
  return (int) v;
}
#endif /* __sgi__ */
#endif

static void gst_debug_reset_threshold (gpointer category, gpointer unused);
static void gst_debug_reset_all_thresholds (void);

#ifdef HAVE_PRINTF_EXTENSION
static int _gst_info_printf_extension_ptr (FILE * stream,
    const struct printf_info *info, const void *const *args);
static int _gst_info_printf_extension_segment (FILE * stream,
    const struct printf_info *info, const void *const *args);
#ifdef HAVE_REGISTER_PRINTF_SPECIFIER
static int _gst_info_printf_extension_arginfo (const struct printf_info *info,
    size_t n, int *argtypes, int *size);
#else
static int _gst_info_printf_extension_arginfo (const struct printf_info *info,
    size_t n, int *argtypes);
#endif
#endif

struct _GstDebugMessage
{
  gchar *message;
  const gchar *format;
  va_list arguments;
};

/* list of all name/level pairs from --gst-debug and GST_DEBUG */
static GStaticMutex __level_name_mutex = G_STATIC_MUTEX_INIT;
static GSList *__level_name = NULL;
typedef struct
{
  GPatternSpec *pat;
  GstDebugLevel level;
}
LevelNameEntry;

/* list of all categories */
static GStaticMutex __cat_mutex = G_STATIC_MUTEX_INIT;
static GSList *__categories = NULL;

/* all registered debug handlers */
typedef struct
{
  GstLogFunction func;
  gpointer user_data;
}
LogFuncEntry;
static GStaticMutex __log_func_mutex = G_STATIC_MUTEX_INIT;
static GSList *__log_functions = NULL;

#define PRETTY_TAGS_DEFAULT  TRUE
static gboolean pretty_tags = PRETTY_TAGS_DEFAULT;

static volatile gint G_GNUC_MAY_ALIAS __default_level = GST_LEVEL_DEFAULT;
static volatile gint G_GNUC_MAY_ALIAS __use_color = 1;

static FILE *log_file;

/* FIXME: export this? */
gboolean
_priv_gst_in_valgrind (void)
{
  static enum
  {
    GST_VG_UNCHECKED,
    GST_VG_NO_VALGRIND,
    GST_VG_INSIDE
  }
  in_valgrind = GST_VG_UNCHECKED;

  if (in_valgrind == GST_VG_UNCHECKED) {
#ifdef HAVE_VALGRIND_VALGRIND_H
    if (RUNNING_ON_VALGRIND) {
      GST_CAT_INFO (GST_CAT_GST_INIT, "we're running inside valgrind");
      printf ("GStreamer has detected that it is running inside valgrind.\n");
      printf ("It might now take different code paths to ease debugging.\n");
      printf ("Of course, this may also lead to different bugs.\n");
      in_valgrind = GST_VG_INSIDE;
    } else {
      GST_CAT_LOG (GST_CAT_GST_INIT, "not doing extra valgrind stuff");
      in_valgrind = GST_VG_NO_VALGRIND;
    }
#else
    in_valgrind = GST_VG_NO_VALGRIND;
#endif
    g_assert (in_valgrind == GST_VG_NO_VALGRIND ||
        in_valgrind == GST_VG_INSIDE);
  }
  return (in_valgrind == GST_VG_INSIDE);
}

/**
 * _gst_debug_init:
 *
 * Initializes the debugging system.
 * Normally you don't want to call this, because gst_init() does it for you.
 */
void
_gst_debug_init (void)
{
  const gchar *env;

  env = g_getenv ("GST_DEBUG_FILE");
  if (env != NULL && *env != '\0') {
    if (strcmp (env, "-") == 0) {
      log_file = stdout;
    } else {
      log_file = g_fopen (env, "w");
      if (log_file == NULL) {
        g_printerr ("Could not open log file '%s' for writing: %s\n", env,
            g_strerror (errno));
        log_file = stderr;
      }
    }
  } else {
    log_file = stderr;
  }

  /* get time we started for debugging messages */
  _priv_gst_info_start_time = gst_util_get_timestamp ();

#ifdef HAVE_PRINTF_EXTENSION
#ifdef HAVE_REGISTER_PRINTF_SPECIFIER
  register_printf_specifier (GST_PTR_FORMAT[0], _gst_info_printf_extension_ptr,
      _gst_info_printf_extension_arginfo);
  register_printf_specifier (GST_SEGMENT_FORMAT[0],
      _gst_info_printf_extension_segment, _gst_info_printf_extension_arginfo);
#else
  register_printf_function (GST_PTR_FORMAT[0], _gst_info_printf_extension_ptr,
      _gst_info_printf_extension_arginfo);
  register_printf_function (GST_SEGMENT_FORMAT[0],
      _gst_info_printf_extension_segment, _gst_info_printf_extension_arginfo);
#endif
#endif

  /* do NOT use a single debug function before this line has been run */
  GST_CAT_DEFAULT = _gst_debug_category_new ("default",
      GST_DEBUG_UNDERLINE, NULL);
  _GST_CAT_DEBUG = _gst_debug_category_new ("GST_DEBUG",
      GST_DEBUG_BOLD | GST_DEBUG_FG_YELLOW, "debugging subsystem");

  gst_debug_add_log_function (gst_debug_log_default, NULL);

  /* FIXME: add descriptions here */
  GST_CAT_GST_INIT = _gst_debug_category_new ("GST_INIT",
      GST_DEBUG_BOLD | GST_DEBUG_FG_RED, NULL);
  GST_CAT_AUTOPLUG = _gst_debug_category_new ("GST_AUTOPLUG",
      GST_DEBUG_BOLD | GST_DEBUG_FG_BLUE, NULL);
  GST_CAT_AUTOPLUG_ATTEMPT = _gst_debug_category_new ("GST_AUTOPLUG_ATTEMPT",
      GST_DEBUG_BOLD | GST_DEBUG_FG_CYAN | GST_DEBUG_BG_BLUE, NULL);
  GST_CAT_PARENTAGE = _gst_debug_category_new ("GST_PARENTAGE",
      GST_DEBUG_BOLD | GST_DEBUG_FG_WHITE | GST_DEBUG_BG_RED, NULL);
  GST_CAT_STATES = _gst_debug_category_new ("GST_STATES",
      GST_DEBUG_BOLD | GST_DEBUG_FG_RED, NULL);
  GST_CAT_SCHEDULING = _gst_debug_category_new ("GST_SCHEDULING",
      GST_DEBUG_BOLD | GST_DEBUG_FG_MAGENTA, NULL);
  GST_CAT_BUFFER = _gst_debug_category_new ("GST_BUFFER",
      GST_DEBUG_BOLD | GST_DEBUG_BG_GREEN, NULL);
  GST_CAT_BUFFER_LIST = _gst_debug_category_new ("GST_BUFFER_LIST",
      GST_DEBUG_BOLD | GST_DEBUG_BG_GREEN, NULL);
  GST_CAT_BUS = _gst_debug_category_new ("GST_BUS", GST_DEBUG_BG_YELLOW, NULL);
  GST_CAT_CAPS = _gst_debug_category_new ("GST_CAPS",
      GST_DEBUG_BOLD | GST_DEBUG_FG_BLUE, NULL);
  GST_CAT_CLOCK = _gst_debug_category_new ("GST_CLOCK",
      GST_DEBUG_BOLD | GST_DEBUG_FG_YELLOW, NULL);
  GST_CAT_ELEMENT_PADS = _gst_debug_category_new ("GST_ELEMENT_PADS",
      GST_DEBUG_BOLD | GST_DEBUG_FG_WHITE | GST_DEBUG_BG_RED, NULL);
  GST_CAT_PADS = _gst_debug_category_new ("GST_PADS",
      GST_DEBUG_BOLD | GST_DEBUG_FG_RED | GST_DEBUG_BG_RED, NULL);
  GST_CAT_PERFORMANCE = _gst_debug_category_new ("GST_PERFORMANCE",
      GST_DEBUG_BOLD | GST_DEBUG_FG_WHITE | GST_DEBUG_BG_RED, NULL);
  GST_CAT_PIPELINE = _gst_debug_category_new ("GST_PIPELINE",
      GST_DEBUG_BOLD | GST_DEBUG_FG_WHITE | GST_DEBUG_BG_RED, NULL);
  GST_CAT_PLUGIN_LOADING = _gst_debug_category_new ("GST_PLUGIN_LOADING",
      GST_DEBUG_BOLD | GST_DEBUG_FG_CYAN, NULL);
  GST_CAT_PLUGIN_INFO = _gst_debug_category_new ("GST_PLUGIN_INFO",
      GST_DEBUG_BOLD | GST_DEBUG_FG_CYAN, NULL);
  GST_CAT_PROPERTIES = _gst_debug_category_new ("GST_PROPERTIES",
      GST_DEBUG_BOLD | GST_DEBUG_FG_WHITE | GST_DEBUG_BG_BLUE, NULL);
  GST_CAT_TYPES = _gst_debug_category_new ("GST_TYPES",
      GST_DEBUG_BOLD | GST_DEBUG_FG_WHITE | GST_DEBUG_BG_RED, NULL);
  GST_CAT_XML = _gst_debug_category_new ("GST_XML",
      GST_DEBUG_BOLD | GST_DEBUG_FG_WHITE | GST_DEBUG_BG_RED, NULL);
  GST_CAT_NEGOTIATION = _gst_debug_category_new ("GST_NEGOTIATION",
      GST_DEBUG_BOLD | GST_DEBUG_FG_BLUE, NULL);
  GST_CAT_REFCOUNTING = _gst_debug_category_new ("GST_REFCOUNTING",
      GST_DEBUG_BOLD | GST_DEBUG_FG_RED | GST_DEBUG_BG_BLUE, NULL);
  GST_CAT_ERROR_SYSTEM = _gst_debug_category_new ("GST_ERROR_SYSTEM",
      GST_DEBUG_BOLD | GST_DEBUG_FG_RED | GST_DEBUG_BG_WHITE, NULL);

  GST_CAT_EVENT = _gst_debug_category_new ("GST_EVENT",
      GST_DEBUG_BOLD | GST_DEBUG_FG_BLUE, NULL);
  GST_CAT_MESSAGE = _gst_debug_category_new ("GST_MESSAGE",
      GST_DEBUG_BOLD | GST_DEBUG_FG_WHITE | GST_DEBUG_BG_RED, NULL);
  GST_CAT_PARAMS = _gst_debug_category_new ("GST_PARAMS",
      GST_DEBUG_BOLD | GST_DEBUG_FG_BLACK | GST_DEBUG_BG_YELLOW, NULL);
  GST_CAT_CALL_TRACE = _gst_debug_category_new ("GST_CALL_TRACE",
      GST_DEBUG_BOLD, NULL);
  GST_CAT_SIGNAL = _gst_debug_category_new ("GST_SIGNAL",
      GST_DEBUG_BOLD | GST_DEBUG_FG_WHITE | GST_DEBUG_BG_RED, NULL);
  GST_CAT_PROBE = _gst_debug_category_new ("GST_PROBE",
      GST_DEBUG_BOLD | GST_DEBUG_FG_GREEN, "pad probes");
  GST_CAT_REGISTRY = _gst_debug_category_new ("GST_REGISTRY", 0, "registry");
  GST_CAT_QOS = _gst_debug_category_new ("GST_QOS", 0, "QoS");
  _priv_GST_CAT_POLL = _gst_debug_category_new ("GST_POLL", 0, "poll");


  /* print out the valgrind message if we're in valgrind */
  _priv_gst_in_valgrind ();

  env = g_getenv ("GST_DEBUG_OPTIONS");
  if (env != NULL) {
    if (strstr (env, "full_tags") || strstr (env, "full-tags"))
      pretty_tags = FALSE;
    else if (strstr (env, "pretty_tags") || strstr (env, "pretty-tags"))
      pretty_tags = TRUE;
  }
}

/* we can't do this further above, because we initialize the GST_CAT_DEFAULT struct */
#define GST_CAT_DEFAULT _GST_CAT_DEBUG

/**
 * gst_debug_log:
 * @category: category to log
 * @level: level of the message is in
 * @file: the file that emitted the message, usually the __FILE__ identifier
 * @function: the function that emitted the message
 * @line: the line from that the message was emitted, usually __LINE__
 * @object: (transfer none) (allow-none): the object this message relates to,
 *     or NULL if none
 * @format: a printf style format string
 * @...: optional arguments for the format
 *
 * Logs the given message using the currently registered debugging handlers.
 */
void
gst_debug_log (GstDebugCategory * category, GstDebugLevel level,
    const gchar * file, const gchar * function, gint line,
    GObject * object, const gchar * format, ...)
{
  va_list var_args;

  va_start (var_args, format);
  gst_debug_log_valist (category, level, file, function, line, object, format,
      var_args);
  va_end (var_args);
}

#ifdef _MSC_VER
/* based on g_basename(), which we can't use because it was deprecated */
static inline const gchar *
gst_path_basename (const gchar * file_name)
{
  register const gchar *base;

  base = strrchr (file_name, G_DIR_SEPARATOR);

  {
    const gchar *q = strrchr (file_name, '/');
    if (base == NULL || (q != NULL && q > base))
      base = q;
  }

  if (base)
    return base + 1;

  if (g_ascii_isalpha (file_name[0]) && file_name[1] == ':')
    return file_name + 2;

  return file_name;
}
#endif

/**
 * gst_debug_log_valist:
 * @category: category to log
 * @level: level of the message is in
 * @file: the file that emitted the message, usually the __FILE__ identifier
 * @function: the function that emitted the message
 * @line: the line from that the message was emitted, usually __LINE__
 * @object: (transfer none) (allow-none): the object this message relates to,
 *     or NULL if none
 * @format: a printf style format string
 * @args: optional arguments for the format
 *
 * Logs the given message using the currently registered debugging handlers.
 */
void
gst_debug_log_valist (GstDebugCategory * category, GstDebugLevel level,
    const gchar * file, const gchar * function, gint line,
    GObject * object, const gchar * format, va_list args)
{
  GstDebugMessage message;
  LogFuncEntry *entry;
  GSList *handler;

  g_return_if_fail (category != NULL);
  g_return_if_fail (file != NULL);
  g_return_if_fail (function != NULL);
  g_return_if_fail (format != NULL);

  /* The predefined macro __FILE__ is always the exact path given to the
   * compiler with MSVC, which may or may not be the basename.  We work
   * around it at runtime to improve the readability. */
#ifdef _MSC_VER
  file = gst_path_basename (file);
#endif

  message.message = NULL;
  message.format = format;
  G_VA_COPY (message.arguments, args);

  handler = __log_functions;
  while (handler) {
    entry = handler->data;
    handler = g_slist_next (handler);
    entry->func (category, level, file, function, line, object, &message,
        entry->user_data);
  }
  g_free (message.message);
  va_end (message.arguments);
}

/**
 * gst_debug_message_get:
 * @message: a debug message
 *
 * Gets the string representation of a #GstDebugMessage. This function is used
 * in debug handlers to extract the message.
 *
 * Returns: the string representation of a #GstDebugMessage.
 */
const gchar *
gst_debug_message_get (GstDebugMessage * message)
{
  if (message->message == NULL) {
    message->message = g_strdup_vprintf (message->format, message->arguments);
  }
  return message->message;
}

#define MAX_BUFFER_DUMP_STRING_LEN  100

/* structure_to_pretty_string:
 * @structure: a #GstStructure
 *
 * Converts @structure to a human-readable string representation. Basically
 * the same as gst_structure_to_string(), but if the structure contains large
 * buffers such as images the hex representation of those buffers will be
 * shortened so that the string remains readable.
 *
 * Returns: a newly-allocated string. g_free() when no longer needed.
 */
static gchar *
structure_to_pretty_string (const GstStructure * s)
{
  gchar *str, *pos, *end;

  str = gst_structure_to_string (s);
  if (str == NULL)
    return NULL;

  pos = str;
  while ((pos = strstr (pos, "(buffer)"))) {
    guint count = 0;

    pos += strlen ("(buffer)");
    for (end = pos; *end != '\0' && *end != ';' && *end != ' '; ++end)
      ++count;
    if (count > MAX_BUFFER_DUMP_STRING_LEN) {
      memcpy (pos + MAX_BUFFER_DUMP_STRING_LEN - 6, "..", 2);
      memcpy (pos + MAX_BUFFER_DUMP_STRING_LEN - 4, pos + count - 4, 4);
      g_memmove (pos + MAX_BUFFER_DUMP_STRING_LEN, pos + count,
          strlen (pos + count) + 1);
      pos += MAX_BUFFER_DUMP_STRING_LEN;
    }
  }

  return str;
}

static inline gchar *
gst_info_structure_to_string (GstStructure * s)
{
  if (G_UNLIKELY (pretty_tags && s->name == GST_QUARK (TAGLIST)))
    return structure_to_pretty_string (s);
  else
    return gst_structure_to_string (s);
}

static gchar *
gst_debug_print_object (gpointer ptr)
{
  GObject *object = (GObject *) ptr;

#ifdef unused
  /* This is a cute trick to detect unmapped memory, but is unportable,
   * slow, screws around with madvise, and not actually that useful. */
  {
    int ret;

    ret = madvise ((void *) ((unsigned long) ptr & (~0xfff)), 4096, 0);
    if (ret == -1 && errno == ENOMEM) {
      buffer = g_strdup_printf ("%p (unmapped memory)", ptr);
    }
  }
#endif

  /* nicely printed object */
  if (object == NULL) {
    return g_strdup ("(NULL)");
  }
  if (*(GType *) ptr == GST_TYPE_CAPS) {
    return gst_caps_to_string ((GstCaps *) ptr);
  }
  if (*(GType *) ptr == GST_TYPE_STRUCTURE) {
    return gst_info_structure_to_string ((GstStructure *) ptr);
  }
#ifdef USE_POISONING
  if (*(guint32 *) ptr == 0xffffffff) {
    return g_strdup_printf ("<poisoned@%p>", ptr);
  }
#endif
  if (GST_IS_PAD (object) && GST_OBJECT_NAME (object)) {
    return g_strdup_printf ("<%s:%s>", GST_DEBUG_PAD_NAME (object));
  }
  if (GST_IS_OBJECT (object) && GST_OBJECT_NAME (object)) {
    return g_strdup_printf ("<%s>", GST_OBJECT_NAME (object));
  }
  if (G_IS_OBJECT (object)) {
    return g_strdup_printf ("<%s@%p>", G_OBJECT_TYPE_NAME (object), object);
  }
  if (GST_IS_MESSAGE (object)) {
    GstMessage *msg = GST_MESSAGE_CAST (object);
    gchar *s, *ret;

    if (msg->structure) {
      s = gst_info_structure_to_string (msg->structure);
    } else {
      s = g_strdup ("(NULL)");
    }

    ret = g_strdup_printf ("%s message from element '%s': %s",
        GST_MESSAGE_TYPE_NAME (msg), (msg->src != NULL) ?
        GST_ELEMENT_NAME (msg->src) : "(NULL)", s);
    g_free (s);
    return ret;
  }
  if (GST_IS_QUERY (object)) {
    GstQuery *query = GST_QUERY_CAST (object);

    if (query->structure) {
      return gst_info_structure_to_string (query->structure);
    } else {
      const gchar *query_type_name;

      query_type_name = gst_query_type_get_name (query->type);
      if (G_LIKELY (query_type_name != NULL)) {
        return g_strdup_printf ("%s query", query_type_name);
      } else {
        return g_strdup_printf ("query of unknown type %d", query->type);
      }
    }
  }
  if (GST_IS_EVENT (object)) {
    GstEvent *event = GST_EVENT_CAST (object);
    gchar *s, *ret;

    if (event->structure) {
      s = gst_info_structure_to_string (event->structure);
    } else {
      s = g_strdup ("(NULL)");
    }

    ret = g_strdup_printf ("%s event from '%s' at time %"
        GST_TIME_FORMAT ": %s",
        GST_EVENT_TYPE_NAME (event), (event->src != NULL) ?
        GST_OBJECT_NAME (event->src) : "(NULL)",
        GST_TIME_ARGS (event->timestamp), s);
    g_free (s);
    return ret;
  }

  return g_strdup_printf ("%p", ptr);
}

#ifdef HAVE_PRINTF_EXTENSION

static gchar *
gst_debug_print_segment (gpointer ptr)
{
  GstSegment *segment = (GstSegment *) ptr;

  /* nicely printed segment */
  if (segment == NULL) {
    return g_strdup ("(NULL)");
  }

  switch (segment->format) {
    case GST_FORMAT_UNDEFINED:{
      return g_strdup_printf ("UNDEFINED segment");
    }
    case GST_FORMAT_TIME:{
      return g_strdup_printf ("time segment start=%" GST_TIME_FORMAT
          ", stop=%" GST_TIME_FORMAT ", last_stop=%" GST_TIME_FORMAT
          ", duration=%" GST_TIME_FORMAT ", rate=%f, applied_rate=%f"
          ", flags=0x%02x, time=%" GST_TIME_FORMAT ", accum=%" GST_TIME_FORMAT,
          GST_TIME_ARGS (segment->start), GST_TIME_ARGS (segment->stop),
          GST_TIME_ARGS (segment->last_stop), GST_TIME_ARGS (segment->duration),
          segment->rate, segment->applied_rate, (guint) segment->flags,
          GST_TIME_ARGS (segment->time), GST_TIME_ARGS (segment->accum));
    }
    default:{
      const gchar *format_name;

      format_name = gst_format_get_name (segment->format);
      if (G_UNLIKELY (format_name == NULL))
        format_name = "(UNKNOWN FORMAT)";
      return g_strdup_printf ("%s segment start=%" G_GINT64_FORMAT
          ", stop=%" G_GINT64_FORMAT ", last_stop=%" G_GINT64_FORMAT
          ", duration=%" G_GINT64_FORMAT ", rate=%f, applied_rate=%f"
          ", flags=0x%02x, time=%" GST_TIME_FORMAT ", accum=%" GST_TIME_FORMAT,
          format_name, segment->start, segment->stop, segment->last_stop,
          segment->duration, segment->rate, segment->applied_rate,
          (guint) segment->flags, GST_TIME_ARGS (segment->time),
          GST_TIME_ARGS (segment->accum));
    }
  }
}

#endif /* HAVE_PRINTF_EXTENSION */

/**
 * gst_debug_construct_term_color:
 * @colorinfo: the color info
 *
 * Constructs a string that can be used for getting the desired color in color
 * terminals.
 * You need to free the string after use.
 *
 * Returns: (transfer full) (type gchar*): a string containing the color
 *     definition
 */
gchar *
gst_debug_construct_term_color (guint colorinfo)
{
  GString *color;

  color = g_string_new ("\033[00");

  if (colorinfo & GST_DEBUG_BOLD) {
    g_string_append_len (color, ";01", 3);
  }
  if (colorinfo & GST_DEBUG_UNDERLINE) {
    g_string_append_len (color, ";04", 3);
  }
  if (colorinfo & GST_DEBUG_FG_MASK) {
    g_string_append_printf (color, ";3%1d", colorinfo & GST_DEBUG_FG_MASK);
  }
  if (colorinfo & GST_DEBUG_BG_MASK) {
    g_string_append_printf (color, ";4%1d",
        (colorinfo & GST_DEBUG_BG_MASK) >> 4);
  }
  g_string_append_c (color, 'm');

  return g_string_free (color, FALSE);
}

/**
 * gst_debug_construct_win_color:
 * @colorinfo: the color info
 *
 * Constructs an integer that can be used for getting the desired color in
 * windows' terminals (cmd.exe). As there is no mean to underline, we simply
 * ignore this attribute.
 *
 * This function returns 0 on non-windows machines.
 *
 * Returns: an integer containing the color definition
 *
 * Since: 0.10.23
 */
gint
gst_debug_construct_win_color (guint colorinfo)
{
  gint color = 0;
#ifdef G_OS_WIN32
  static const guchar ansi_to_win_fg[8] = {
    0,                          /* black   */
    FOREGROUND_RED,             /* red     */
    FOREGROUND_GREEN,           /* green   */
    FOREGROUND_RED | FOREGROUND_GREEN,  /* yellow  */
    FOREGROUND_BLUE,            /* blue    */
    FOREGROUND_RED | FOREGROUND_BLUE,   /* magenta */
    FOREGROUND_GREEN | FOREGROUND_BLUE, /* cyan    */
    FOREGROUND_RED | FOREGROUND_GREEN | FOREGROUND_BLUE /* white   */
  };
  static const guchar ansi_to_win_bg[8] = {
    0,
    BACKGROUND_RED,
    BACKGROUND_GREEN,
    BACKGROUND_RED | BACKGROUND_GREEN,
    BACKGROUND_BLUE,
    BACKGROUND_RED | BACKGROUND_BLUE,
    BACKGROUND_GREEN | FOREGROUND_BLUE,
    BACKGROUND_RED | BACKGROUND_GREEN | BACKGROUND_BLUE
  };

  /* we draw black as white, as cmd.exe can only have black bg */
  if (colorinfo == 0) {
    return ansi_to_win_fg[7];
  }

  if (colorinfo & GST_DEBUG_BOLD) {
    color |= FOREGROUND_INTENSITY;
  }
  if (colorinfo & GST_DEBUG_FG_MASK) {
    color |= ansi_to_win_fg[colorinfo & GST_DEBUG_FG_MASK];
  }
  if (colorinfo & GST_DEBUG_BG_MASK) {
    color |= ansi_to_win_bg[(colorinfo & GST_DEBUG_BG_MASK) >> 4];
  }
#endif
  return color;
}

/* width of %p varies depending on actual value of pointer, which can make
 * output unevenly aligned if multiple threads are involved, hence the %14p
 * (should really be %18p, but %14p seems a good compromise between too many
 * white spaces and likely unalignment on my system) */
#if defined (GLIB_SIZEOF_VOID_P) && GLIB_SIZEOF_VOID_P == 8
#define PTR_FMT "%14p"
#else
#define PTR_FMT "%10p"
#endif
#define PID_FMT "%5d"
#define CAT_FMT "%20s %s:%d:%s:%s"

#ifdef G_OS_WIN32
static const guchar levelcolormap[GST_LEVEL_COUNT] = {
  /* GST_LEVEL_NONE */
  FOREGROUND_RED | FOREGROUND_GREEN | FOREGROUND_BLUE,
  /* GST_LEVEL_ERROR */
  FOREGROUND_RED | FOREGROUND_INTENSITY,
  /* GST_LEVEL_WARNING */
  FOREGROUND_RED | FOREGROUND_GREEN | FOREGROUND_INTENSITY,
  /* GST_LEVEL_INFO */
  FOREGROUND_GREEN | FOREGROUND_INTENSITY,
  /* GST_LEVEL_DEBUG */
  FOREGROUND_GREEN | FOREGROUND_BLUE,
  /* GST_LEVEL_LOG */
  FOREGROUND_RED | FOREGROUND_GREEN | FOREGROUND_BLUE,
  /* GST_LEVEL_FIXME */
  FOREGROUND_RED | FOREGROUND_GREEN,
  /* GST_LEVEL_TRACE */
  FOREGROUND_RED | FOREGROUND_GREEN | FOREGROUND_BLUE,
  /* placeholder for log level 8 */
  0,
  /* GST_LEVEL_MEMDUMP */
  FOREGROUND_RED | FOREGROUND_GREEN | FOREGROUND_BLUE
};

static const guchar available_colors[] = {
  FOREGROUND_RED, FOREGROUND_GREEN, FOREGROUND_RED | FOREGROUND_GREEN,
  FOREGROUND_BLUE, FOREGROUND_RED | FOREGROUND_BLUE,
  FOREGROUND_GREEN | FOREGROUND_BLUE,
};
#else
static const gchar *levelcolormap[GST_LEVEL_COUNT] = {
  "\033[37m",                   /* GST_LEVEL_NONE */
  "\033[31;01m",                /* GST_LEVEL_ERROR */
  "\033[33;01m",                /* GST_LEVEL_WARNING */
  "\033[32;01m",                /* GST_LEVEL_INFO */
  "\033[36m",                   /* GST_LEVEL_DEBUG */
  "\033[37m",                   /* GST_LEVEL_LOG */
  "\033[33;01m",                /* GST_LEVEL_FIXME */
  "\033[37m",                   /* GST_LEVEL_TRACE */
  "\033[37m",                   /* placeholder for log level 8 */
  "\033[37m"                    /* GST_LEVEL_MEMDUMP */
};
#endif

/**
 * gst_debug_log_default:
 * @category: category to log
 * @level: level of the message
 * @file: the file that emitted the message, usually the __FILE__ identifier
 * @function: the function that emitted the message
 * @line: the line from that the message was emitted, usually __LINE__
 * @message: the actual message
 * @object: (transfer none) (allow-none): the object this message relates to,
 *     or NULL if none
 * @unused: an unused variable, reserved for some user_data.
 *
 * The default logging handler used by GStreamer. Logging functions get called
 * whenever a macro like GST_DEBUG or similar is used. This function outputs the
 * message and additional info to stderr (or the log file specified via the
 * GST_DEBUG_FILE environment variable).
 *
 * You can add other handlers by using gst_debug_add_log_function().
 * And you can remove this handler by calling
 * gst_debug_remove_log_function(gst_debug_log_default);
 */
void
gst_debug_log_default (GstDebugCategory * category, GstDebugLevel level,
    const gchar * file, const gchar * function, gint line,
    GObject * object, GstDebugMessage * message, gpointer unused)
{
  gint pid;
  GstClockTime elapsed;
  gchar *obj = NULL;
  gboolean is_colored;

  if (level > gst_debug_category_get_threshold (category))
    return;

  pid = getpid ();
  is_colored = gst_debug_is_colored ();

  if (object) {
    obj = gst_debug_print_object (object);
  } else {
    obj = g_strdup ("");
  }

  elapsed = GST_CLOCK_DIFF (_priv_gst_info_start_time,
      gst_util_get_timestamp ());

  if (is_colored) {
#ifndef G_OS_WIN32
    /* colors, non-windows */
    gchar *color = NULL;
    const gchar *clear;
    gchar pidcolor[10];
    const gchar *levelcolor;

    color = gst_debug_construct_term_color (gst_debug_category_get_color
        (category));
    clear = "\033[00m";
    g_sprintf (pidcolor, "\033[3%1dm", pid % 6 + 31);
    levelcolor = levelcolormap[level];

#define PRINT_FMT " %s"PID_FMT"%s "PTR_FMT" %s%s%s %s"CAT_FMT"%s %s\n"
    fprintf (log_file, "%" GST_TIME_FORMAT PRINT_FMT, GST_TIME_ARGS (elapsed),
        pidcolor, pid, clear, g_thread_self (), levelcolor,
        gst_debug_level_get_name (level), clear, color,
        gst_debug_category_get_name (category), file, line, function, obj,
        clear, gst_debug_message_get (message));
    fflush (log_file);
#undef PRINT_FMT
    g_free (color);
#else
    /* colors, windows. We take a lock to keep colors and content together.
     * Maybe there is a better way but for now this will do the right
     * thing. */
    static GStaticMutex win_print_mutex = G_STATIC_MUTEX_INIT;
    const gint clear = FOREGROUND_RED | FOREGROUND_GREEN | FOREGROUND_BLUE;
#define SET_COLOR(c) G_STMT_START { \
  if (log_file == stderr) \
    SetConsoleTextAttribute (GetStdHandle (STD_ERROR_HANDLE), (c)); \
  } G_STMT_END
    g_static_mutex_lock (&win_print_mutex);
    /* timestamp */
    fprintf (log_file, "%" GST_TIME_FORMAT " ", GST_TIME_ARGS (elapsed));
    fflush (log_file);
    /* pid */
    SET_COLOR (available_colors[pid % G_N_ELEMENTS (available_colors)]);
    fprintf (log_file, PID_FMT, pid);
    fflush (log_file);
    /* thread */
    SET_COLOR (clear);
    fprintf (log_file, " " PTR_FMT " ", g_thread_self ());
    fflush (log_file);
    /* level */
    SET_COLOR (levelcolormap[level]);
    fprintf (log_file, "%s ", gst_debug_level_get_name (level));
    fflush (log_file);
    /* category */
    SET_COLOR (gst_debug_construct_win_color (gst_debug_category_get_color
            (category)));
    fprintf (log_file, CAT_FMT, gst_debug_category_get_name (category),
        file, line, function, obj);
    fflush (log_file);
    /* message */
    SET_COLOR (clear);
    fprintf (log_file, " %s\n", gst_debug_message_get (message));
    fflush (log_file);
    g_static_mutex_unlock (&win_print_mutex);
#endif
  } else {
    /* no color, all platforms */
#define PRINT_FMT " "PID_FMT" "PTR_FMT" %s "CAT_FMT" %s\n"
    fprintf (log_file, "%" GST_TIME_FORMAT PRINT_FMT, GST_TIME_ARGS (elapsed),
        pid, g_thread_self (), gst_debug_level_get_name (level),
        gst_debug_category_get_name (category), file, line, function, obj,
        gst_debug_message_get (message));
    fflush (log_file);
#undef PRINT_FMT
  }

  g_free (obj);
}

/**
 * gst_debug_level_get_name:
 * @level: the level to get the name for
 *
 * Get the string representation of a debugging level
 *
 * Returns: the name
 */
const gchar *
gst_debug_level_get_name (GstDebugLevel level)
{
  switch (level) {
    case GST_LEVEL_NONE:
      return "";
    case GST_LEVEL_ERROR:
      return "ERROR  ";
    case GST_LEVEL_WARNING:
      return "WARN   ";
    case GST_LEVEL_INFO:
      return "INFO   ";
    case GST_LEVEL_DEBUG:
      return "DEBUG  ";
    case GST_LEVEL_LOG:
      return "LOG    ";
    case GST_LEVEL_FIXME:
      return "FIXME  ";
    case GST_LEVEL_TRACE:
      return "TRACE  ";
    case GST_LEVEL_MEMDUMP:
      return "MEMDUMP";
    default:
      g_warning ("invalid level specified for gst_debug_level_get_name");
      return "";
  }
}

/**
 * gst_debug_add_log_function:
 * @func: the function to use
 * @data: (closure): user data
 *
 * Adds the logging function to the list of logging functions.
 * Be sure to use #G_GNUC_NO_INSTRUMENT on that function, it is needed.
 */
void
gst_debug_add_log_function (GstLogFunction func, gpointer data)
{
  LogFuncEntry *entry;
  GSList *list;

  if (func == NULL)
    func = gst_debug_log_default;

  entry = g_slice_new (LogFuncEntry);
  entry->func = func;
  entry->user_data = data;
  /* FIXME: we leak the old list here - other threads might access it right now
   * in gst_debug_logv. Another solution is to lock the mutex in gst_debug_logv,
   * but that is waaay costly.
   * It'd probably be clever to use some kind of RCU here, but I don't know
   * anything about that.
   */
  g_static_mutex_lock (&__log_func_mutex);
  list = g_slist_copy (__log_functions);
  __log_functions = g_slist_prepend (list, entry);
  g_static_mutex_unlock (&__log_func_mutex);

  GST_DEBUG ("prepended log function %p (user data %p) to log functions",
      func, data);
}

static gint
gst_debug_compare_log_function_by_func (gconstpointer entry, gconstpointer func)
{
  gpointer entryfunc = (gpointer) (((LogFuncEntry *) entry)->func);

  return (entryfunc < func) ? -1 : (entryfunc > func) ? 1 : 0;
}

static gint
gst_debug_compare_log_function_by_data (gconstpointer entry, gconstpointer data)
{
  gpointer entrydata = ((LogFuncEntry *) entry)->user_data;

  return (entrydata < data) ? -1 : (entrydata > data) ? 1 : 0;
}

static guint
gst_debug_remove_with_compare_func (GCompareFunc func, gpointer data)
{
  GSList *found;
  GSList *new;
  guint removals = 0;

  g_static_mutex_lock (&__log_func_mutex);
  new = __log_functions;
  while ((found = g_slist_find_custom (new, data, func))) {
    if (new == __log_functions) {
      /* make a copy when we have the first hit, so that we modify the copy and
       * make that the new list later */
      new = g_slist_copy (new);
      continue;
    }
    g_slice_free (LogFuncEntry, found->data);
    new = g_slist_delete_link (new, found);
    removals++;
  }
  /* FIXME: We leak the old list here. See _add_log_function for why. */
  __log_functions = new;
  g_static_mutex_unlock (&__log_func_mutex);

  return removals;
}

/**
 * gst_debug_remove_log_function:
 * @func: the log function to remove
 *
 * Removes all registered instances of the given logging functions.
 *
 * Returns: How many instances of the function were removed
 */
guint
gst_debug_remove_log_function (GstLogFunction func)
{
  guint removals;

  if (func == NULL)
    func = gst_debug_log_default;

  removals =
      gst_debug_remove_with_compare_func
      (gst_debug_compare_log_function_by_func, (gpointer) func);
  GST_DEBUG ("removed log function %p %d times from log function list", func,
      removals);

  return removals;
}

/**
 * gst_debug_remove_log_function_by_data:
 * @data: user data of the log function to remove
 *
 * Removes all registered instances of log functions with the given user data.
 *
 * Returns: How many instances of the function were removed
 */
guint
gst_debug_remove_log_function_by_data (gpointer data)
{
  guint removals;

  removals =
      gst_debug_remove_with_compare_func
      (gst_debug_compare_log_function_by_data, data);
  GST_DEBUG
      ("removed %d log functions with user data %p from log function list",
      removals, data);

  return removals;
}

/**
 * gst_debug_set_colored:
 * @colored: Whether to use colored output or not
 *
 * Sets or unsets the use of coloured debugging output.
 *
 * This function may be called before gst_init().
 */
void
gst_debug_set_colored (gboolean colored)
{
  g_atomic_int_set (&__use_color, (gint) colored);
}

/**
 * gst_debug_is_colored:
 *
 * Checks if the debugging output should be colored.
 *
 * Returns: TRUE, if the debug output should be colored.
 */
gboolean
gst_debug_is_colored (void)
{
  return (gboolean) g_atomic_int_get (&__use_color);
}

/**
 * gst_debug_set_active:
 * @active: Whether to use debugging output or not
 *
 * If activated, debugging messages are sent to the debugging
 * handlers.
 * It makes sense to deactivate it for speed issues.
 * <note><para>This function is not threadsafe. It makes sense to only call it
 * during initialization.</para></note>
 */
void
gst_debug_set_active (gboolean active)
{
  __gst_debug_enabled = active;
  if (active)
    __gst_debug_min = GST_LEVEL_COUNT;
  else
    __gst_debug_min = GST_LEVEL_NONE;
}

/**
 * gst_debug_is_active:
 *
 * Checks if debugging output is activated.
 *
 * Returns: TRUE, if debugging is activated
 */
gboolean
gst_debug_is_active (void)
{
  return __gst_debug_enabled;
}

/**
 * gst_debug_set_default_threshold:
 * @level: level to set
 *
 * Sets the default threshold to the given level and updates all categories to
 * use this threshold.
 *
 * This function may be called before gst_init().
 */
void
gst_debug_set_default_threshold (GstDebugLevel level)
{
  g_atomic_int_set (&__default_level, level);
  gst_debug_reset_all_thresholds ();
}

/**
 * gst_debug_get_default_threshold:
 *
 * Returns the default threshold that is used for new categories.
 *
 * Returns: the default threshold level
 */
GstDebugLevel
gst_debug_get_default_threshold (void)
{
  return (GstDebugLevel) g_atomic_int_get (&__default_level);
}

static void
gst_debug_reset_threshold (gpointer category, gpointer unused)
{
  GstDebugCategory *cat = (GstDebugCategory *) category;
  GSList *walk;

  g_static_mutex_lock (&__level_name_mutex);
  walk = __level_name;
  while (walk) {
    LevelNameEntry *entry = walk->data;

    walk = g_slist_next (walk);
    if (g_pattern_match_string (entry->pat, cat->name)) {
      GST_LOG ("category %s matches pattern %p - gets set to level %d",
          cat->name, entry->pat, entry->level);
      gst_debug_category_set_threshold (cat, entry->level);
      goto exit;
    }
  }
  gst_debug_category_set_threshold (cat, gst_debug_get_default_threshold ());

exit:
  g_static_mutex_unlock (&__level_name_mutex);
}

static void
gst_debug_reset_all_thresholds (void)
{
  g_static_mutex_lock (&__cat_mutex);
  g_slist_foreach (__categories, gst_debug_reset_threshold, NULL);
  g_static_mutex_unlock (&__cat_mutex);
}

static void
for_each_threshold_by_entry (gpointer data, gpointer user_data)
{
  GstDebugCategory *cat = (GstDebugCategory *) data;
  LevelNameEntry *entry = (LevelNameEntry *) user_data;

  if (g_pattern_match_string (entry->pat, cat->name)) {
    GST_LOG ("category %s matches pattern %p - gets set to level %d",
        cat->name, entry->pat, entry->level);
    gst_debug_category_set_threshold (cat, entry->level);
  }
}

/**
 * gst_debug_set_threshold_for_name:
 * @name: name of the categories to set
 * @level: level to set them to
 *
 * Sets all categories which match the given glob style pattern to the given
 * level.
 */
void
gst_debug_set_threshold_for_name (const gchar * name, GstDebugLevel level)
{
  GPatternSpec *pat;
  LevelNameEntry *entry;

  g_return_if_fail (name != NULL);

  pat = g_pattern_spec_new (name);
  entry = g_slice_new (LevelNameEntry);
  entry->pat = pat;
  entry->level = level;
  g_static_mutex_lock (&__level_name_mutex);
  __level_name = g_slist_prepend (__level_name, entry);
  g_static_mutex_unlock (&__level_name_mutex);
  g_static_mutex_lock (&__cat_mutex);
  g_slist_foreach (__categories, for_each_threshold_by_entry, entry);
  g_static_mutex_unlock (&__cat_mutex);
}

/**
 * gst_debug_unset_threshold_for_name:
 * @name: name of the categories to set
 *
 * Resets all categories with the given name back to the default level.
 */
void
gst_debug_unset_threshold_for_name (const gchar * name)
{
  GSList *walk;
  GPatternSpec *pat;

  g_return_if_fail (name != NULL);

  pat = g_pattern_spec_new (name);
  g_static_mutex_lock (&__level_name_mutex);
  walk = __level_name;
  /* improve this if you want, it's mighty slow */
  while (walk) {
    LevelNameEntry *entry = walk->data;

    if (g_pattern_spec_equal (entry->pat, pat)) {
      __level_name = g_slist_remove_link (__level_name, walk);
      g_pattern_spec_free (entry->pat);
      g_slice_free (LevelNameEntry, entry);
      g_slist_free_1 (walk);
      walk = __level_name;
    }
  }
  g_static_mutex_unlock (&__level_name_mutex);
  g_pattern_spec_free (pat);
  gst_debug_reset_all_thresholds ();
}

GstDebugCategory *
_gst_debug_category_new (const gchar * name, guint color,
    const gchar * description)
{
  GstDebugCategory *cat;

  g_return_val_if_fail (name != NULL, NULL);

  cat = g_slice_new (GstDebugCategory);
  cat->name = g_strdup (name);
  cat->color = color;
  if (description != NULL) {
    cat->description = g_strdup (description);
  } else {
    cat->description = g_strdup ("no description");
  }
  g_atomic_int_set (&cat->threshold, 0);
  gst_debug_reset_threshold (cat, NULL);

  /* add to category list */
  g_static_mutex_lock (&__cat_mutex);
  __categories = g_slist_prepend (__categories, cat);
  g_static_mutex_unlock (&__cat_mutex);

  return cat;
}

/**
 * gst_debug_category_free:
 * @category: #GstDebugCategory to free.
 *
 * Removes and frees the category and all associated resources.
 */
void
gst_debug_category_free (GstDebugCategory * category)
{
  if (category == NULL)
    return;

  /* remove from category list */
  g_static_mutex_lock (&__cat_mutex);
  __categories = g_slist_remove (__categories, category);
  g_static_mutex_unlock (&__cat_mutex);

  g_free ((gpointer) category->name);
  g_free ((gpointer) category->description);
  g_slice_free (GstDebugCategory, category);
}

/**
 * gst_debug_category_set_threshold:
 * @category: a #GstDebugCategory to set threshold of.
 * @level: the #GstDebugLevel threshold to set.
 *
 * Sets the threshold of the category to the given level. Debug information will
 * only be output if the threshold is lower or equal to the level of the
 * debugging message.
 * <note><para>
 * Do not use this function in production code, because other functions may
 * change the threshold of categories as side effect. It is however a nice
 * function to use when debugging (even from gdb).
 * </para></note>
 */
void
gst_debug_category_set_threshold (GstDebugCategory * category,
    GstDebugLevel level)
{
  g_return_if_fail (category != NULL);

  if (level > __gst_debug_min) {
    __gst_debug_enabled = TRUE;
    __gst_debug_min = level;
  }

  g_atomic_int_set (&category->threshold, level);
}

/**
 * gst_debug_category_reset_threshold:
 * @category: a #GstDebugCategory to reset threshold of.
 *
 * Resets the threshold of the category to the default level. Debug information
 * will only be output if the threshold is lower or equal to the level of the
 * debugging message.
 * Use this function to set the threshold back to where it was after using
 * gst_debug_category_set_threshold().
 */
void
gst_debug_category_reset_threshold (GstDebugCategory * category)
{
  gst_debug_reset_threshold (category, NULL);
}

/**
 * gst_debug_category_get_threshold:
 * @category: a #GstDebugCategory to get threshold of.
 *
 * Returns the threshold of a #GstDebugCategory.
 *
 * Returns: the #GstDebugLevel that is used as threshold.
 */
GstDebugLevel
gst_debug_category_get_threshold (GstDebugCategory * category)
{
  return g_atomic_int_get (&category->threshold);
}

/**
 * gst_debug_category_get_name:
 * @category: a #GstDebugCategory to get name of.
 *
 * Returns the name of a debug category.
 *
 * Returns: the name of the category.
 */
const gchar *
gst_debug_category_get_name (GstDebugCategory * category)
{
  return category->name;
}

/**
 * gst_debug_category_get_color:
 * @category: a #GstDebugCategory to get the color of.
 *
 * Returns the color of a debug category used when printing output in this
 * category.
 *
 * Returns: the color of the category.
 */
guint
gst_debug_category_get_color (GstDebugCategory * category)
{
  return category->color;
}

/**
 * gst_debug_category_get_description:
 * @category: a #GstDebugCategory to get the description of.
 *
 * Returns the description of a debug category.
 *
 * Returns: the description of the category.
 */
const gchar *
gst_debug_category_get_description (GstDebugCategory * category)
{
  return category->description;
}

/**
 * gst_debug_get_all_categories:
 *
 * Returns a snapshot of a all categories that are currently in use . This list
 * may change anytime.
 * The caller has to free the list after use.
 *
 * Returns: (transfer container) (element-type Gst.DebugCategory): the list of
 *     debug categories
 */
GSList *
gst_debug_get_all_categories (void)
{
  GSList *ret;

  g_static_mutex_lock (&__cat_mutex);
  ret = g_slist_copy (__categories);
  g_static_mutex_unlock (&__cat_mutex);

  return ret;
}

GstDebugCategory *
_gst_debug_get_category (const gchar * name)
{
  GstDebugCategory *ret = NULL;
  GSList *node;

  for (node = __categories; node; node = g_slist_next (node)) {
    ret = (GstDebugCategory *) node->data;
    if (!strcmp (name, ret->name)) {
      return ret;
    }
  }
  return NULL;
}

/*** FUNCTION POINTERS ********************************************************/

static GHashTable *__gst_function_pointers;     /* NULL */
static GStaticMutex __dbg_functions_mutex = G_STATIC_MUTEX_INIT;

/* This function MUST NOT return NULL */
const gchar *
_gst_debug_nameof_funcptr (GstDebugFuncPtr func)
{
  gchar *ptrname;

#ifdef HAVE_DLADDR
  Dl_info dl_info;
#endif

  if (G_UNLIKELY (func == NULL))
    return "(NULL)";

  g_static_mutex_lock (&__dbg_functions_mutex);
  if (G_LIKELY (__gst_function_pointers)) {
    ptrname = g_hash_table_lookup (__gst_function_pointers, (gpointer) func);
    g_static_mutex_unlock (&__dbg_functions_mutex);
    if (G_LIKELY (ptrname))
      return ptrname;
  } else {
    g_static_mutex_unlock (&__dbg_functions_mutex);
  }
  /* we need to create an entry in the hash table for this one so we don't leak
   * the name */
#ifdef HAVE_DLADDR
  if (dladdr ((gpointer) func, &dl_info) && dl_info.dli_sname) {
    gchar *name = g_strdup (dl_info.dli_sname);

    _gst_debug_register_funcptr (func, name);
    return name;
  } else
#endif
  {
    gchar *name = g_strdup_printf ("%p", (gpointer) func);

    _gst_debug_register_funcptr (func, name);
    return name;
  }
}

void
_gst_debug_register_funcptr (GstDebugFuncPtr func, const gchar * ptrname)
{
  gpointer ptr = (gpointer) func;

  g_static_mutex_lock (&__dbg_functions_mutex);

  if (!__gst_function_pointers)
    __gst_function_pointers = g_hash_table_new (g_direct_hash, g_direct_equal);
  if (!g_hash_table_lookup (__gst_function_pointers, ptr))
    g_hash_table_insert (__gst_function_pointers, ptr, (gpointer) ptrname);

  g_static_mutex_unlock (&__dbg_functions_mutex);
}

/*** PRINTF EXTENSIONS ********************************************************/

#ifdef HAVE_PRINTF_EXTENSION
static int
_gst_info_printf_extension_ptr (FILE * stream, const struct printf_info *info,
    const void *const *args)
{
  char *buffer;
  int len;
  void *ptr;

  buffer = NULL;
  ptr = *(void **) args[0];

  buffer = gst_debug_print_object (ptr);
  len = fprintf (stream, "%*s", (info->left ? -info->width : info->width),
      buffer);

  g_free (buffer);
  return len;
}

static int
_gst_info_printf_extension_segment (FILE * stream,
    const struct printf_info *info, const void *const *args)
{
  char *buffer;
  int len;
  void *ptr;

  buffer = NULL;
  ptr = *(void **) args[0];

  buffer = gst_debug_print_segment (ptr);
  len = fprintf (stream, "%*s", (info->left ? -info->width : info->width),
      buffer);

  g_free (buffer);
  return len;
}

#ifdef HAVE_REGISTER_PRINTF_SPECIFIER
static int
_gst_info_printf_extension_arginfo (const struct printf_info *info, size_t n,
    int *argtypes, int *size)
#else
static int
_gst_info_printf_extension_arginfo (const struct printf_info *info, size_t n,
    int *argtypes)
#endif
{
  if (n > 0) {
    argtypes[0] = PA_POINTER;
#ifdef HAVE_REGISTER_PRINTF_SPECIFIER
    *size = sizeof (gpointer);
#endif
  }
  return 1;
}
#endif /* HAVE_PRINTF_EXTENSION */

static void
gst_info_dump_mem_line (gchar * linebuf, gsize linebuf_size,
    const guint8 * mem, gsize mem_offset, gsize mem_size)
{
  gchar hexstr[50], ascstr[18], digitstr[4];

  if (mem_size > 16)
    mem_size = 16;

  hexstr[0] = '\0';
  ascstr[0] = '\0';

  if (mem != NULL) {
    guint i = 0;

    mem += mem_offset;
    while (i < mem_size) {
      ascstr[i] = (g_ascii_isprint (mem[i])) ? mem[i] : '.';
      g_snprintf (digitstr, sizeof (digitstr), "%02x ", mem[i]);
      g_strlcat (hexstr, digitstr, sizeof (hexstr));
      ++i;
    }
    ascstr[i] = '\0';
  }

  g_snprintf (linebuf, linebuf_size, "%08x: %-48.48s %-16.16s",
      (guint) mem_offset, hexstr, ascstr);
}

void
_gst_debug_dump_mem (GstDebugCategory * cat, const gchar * file,
    const gchar * func, gint line, GObject * obj, const gchar * msg,
    const guint8 * data, guint length)
{
  guint off = 0;

  gst_debug_log ((cat), GST_LEVEL_MEMDUMP, file, func, line, obj, "--------"
      "-------------------------------------------------------------------");

  if (msg != NULL && *msg != '\0') {
    gst_debug_log ((cat), GST_LEVEL_MEMDUMP, file, func, line, obj, "%s", msg);
  }

  while (off < length) {
    gchar buf[128];

    /* gst_info_dump_mem_line will process 16 bytes at most */
    gst_info_dump_mem_line (buf, sizeof (buf), data, off, length - off);
    gst_debug_log (cat, GST_LEVEL_MEMDUMP, file, func, line, obj, "%s", buf);
    off += 16;
  }

  gst_debug_log ((cat), GST_LEVEL_MEMDUMP, file, func, line, obj, "--------"
      "-------------------------------------------------------------------");
}

#else /* !GST_DISABLE_GST_DEBUG */
#ifndef GST_REMOVE_DISABLED

GstDebugCategory *
_gst_debug_category_new (const gchar * name, guint color,
    const gchar * description)
{
  return NULL;
}

void
_gst_debug_register_funcptr (GstDebugFuncPtr func, const gchar * ptrname)
{
}

/* This function MUST NOT return NULL */
const gchar *
_gst_debug_nameof_funcptr (GstDebugFuncPtr func)
{
  return "(NULL)";
}

void
gst_debug_log (GstDebugCategory * category, GstDebugLevel level,
    const gchar * file, const gchar * function, gint line,
    GObject * object, const gchar * format, ...)
{
}

void
gst_debug_log_valist (GstDebugCategory * category, GstDebugLevel level,
    const gchar * file, const gchar * function, gint line,
    GObject * object, const gchar * format, va_list args)
{
}

const gchar *
gst_debug_message_get (GstDebugMessage * message)
{
  return "";
}

void
gst_debug_log_default (GstDebugCategory * category, GstDebugLevel level,
    const gchar * file, const gchar * function, gint line,
    GObject * object, GstDebugMessage * message, gpointer unused)
{
}

const gchar *
gst_debug_level_get_name (GstDebugLevel level)
{
  return "NONE";
}

void
gst_debug_add_log_function (GstLogFunction func, gpointer data)
{
}

guint
gst_debug_remove_log_function (GstLogFunction func)
{
  return 0;
}

guint
gst_debug_remove_log_function_by_data (gpointer data)
{
  return 0;
}

void
gst_debug_set_active (gboolean active)
{
}

gboolean
gst_debug_is_active (void)
{
  return FALSE;
}

void
gst_debug_set_colored (gboolean colored)
{
}

gboolean
gst_debug_is_colored (void)
{
  return FALSE;
}

void
gst_debug_set_default_threshold (GstDebugLevel level)
{
}

GstDebugLevel
gst_debug_get_default_threshold (void)
{
  return GST_LEVEL_NONE;
}

void
gst_debug_set_threshold_for_name (const gchar * name, GstDebugLevel level)
{
}

void
gst_debug_unset_threshold_for_name (const gchar * name)
{
}

void
gst_debug_category_free (GstDebugCategory * category)
{
}

void
gst_debug_category_set_threshold (GstDebugCategory * category,
    GstDebugLevel level)
{
}

void
gst_debug_category_reset_threshold (GstDebugCategory * category)
{
}

GstDebugLevel
gst_debug_category_get_threshold (GstDebugCategory * category)
{
  return GST_LEVEL_NONE;
}

const gchar *
gst_debug_category_get_name (GstDebugCategory * category)
{
  return "";
}

guint
gst_debug_category_get_color (GstDebugCategory * category)
{
  return 0;
}

const gchar *
gst_debug_category_get_description (GstDebugCategory * category)
{
  return "";
}

GSList *
gst_debug_get_all_categories (void)
{
  return NULL;
}

GstDebugCategory *
_gst_debug_get_category (const gchar * name)
{
  return NULL;
}

gchar *
gst_debug_construct_term_color (guint colorinfo)
{
  return g_strdup ("00");
}

gint
gst_debug_construct_win_color (guint colorinfo)
{
  return 0;
}

gboolean
_priv_gst_in_valgrind (void)
{
  return FALSE;
}

void
_gst_debug_dump_mem (GstDebugCategory * cat, const gchar * file,
    const gchar * func, gint line, GObject * obj, const gchar * msg,
    const guint8 * data, guint length)
{
}
#endif /* GST_REMOVE_DISABLED */
#endif /* GST_DISABLE_GST_DEBUG */


#ifdef GST_ENABLE_FUNC_INSTRUMENTATION
/* FIXME make this thread specific */
static GSList *stack_trace = NULL;

void
__cyg_profile_func_enter (void *this_fn, void *call_site)
    G_GNUC_NO_INSTRUMENT;
     void __cyg_profile_func_enter (void *this_fn, void *call_site)
{
  gchar *name = _gst_debug_nameof_funcptr (this_fn);
  gchar *site = _gst_debug_nameof_funcptr (call_site);

  GST_CAT_DEBUG (GST_CAT_CALL_TRACE, "entering function %s from %s", name,
      site);
  stack_trace =
      g_slist_prepend (stack_trace, g_strdup_printf ("%8p in %s from %p (%s)",
          this_fn, name, call_site, site));

  g_free (name);
  g_free (site);
}

void
__cyg_profile_func_exit (void *this_fn, void *call_site)
    G_GNUC_NO_INSTRUMENT;
     void __cyg_profile_func_exit (void *this_fn, void *call_site)
{
  gchar *name = _gst_debug_nameof_funcptr (this_fn);

  GST_CAT_DEBUG (GST_CAT_CALL_TRACE, "leaving function %s", name);
  g_free (stack_trace->data);
  stack_trace = g_slist_delete_link (stack_trace, stack_trace);

  g_free (name);
}

/**
 * gst_debug_print_stack_trace:
 *
 * If GST_ENABLE_FUNC_INSTRUMENTATION is defined a stacktrace is available for
 * gstreamer code, which can be printed with this function.
 */
void
gst_debug_print_stack_trace (void)
{
  GSList *walk = stack_trace;
  gint count = 0;

  if (walk)
    walk = g_slist_next (walk);

  while (walk) {
    gchar *name = (gchar *) walk->data;

    g_print ("#%-2d %s\n", count++, name);

    walk = g_slist_next (walk);
  }
}
#else
void
gst_debug_print_stack_trace (void)
{
  /* nothing because it's compiled out */
}

#endif /* GST_ENABLE_FUNC_INSTRUMENTATION */
