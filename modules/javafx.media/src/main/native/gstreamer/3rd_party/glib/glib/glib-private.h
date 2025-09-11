/* glib-private.h - GLib-internal private API, shared between glib, gobject, gio
 * Copyright (C) 2011 Red Hat, Inc.
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, see <http://www.gnu.org/licenses/>.
 */

#ifndef __GLIB_PRIVATE_H__
#define __GLIB_PRIVATE_H__

#include <glib.h>
#include "gwakeup.h"
#include "gstdioprivate.h"
#include "gdatasetprivate.h"

/*
 * G_SIGNEDNESS_OF:
 * @T: a numeric type such as `unsigned int`
 *
 * An integer constant expression indicating whether @T is a signed type.
 *
 * Returns: 1 if @T is signed, 0 if it is unsigned
 */
#define G_SIGNEDNESS_OF(T) (((T) -1) <= 0)

/* gcc defines __SANITIZE_ADDRESS__, clang sets the address_sanitizer
 * feature flag.
 *
 * MSVC defines __SANITIZE_ADDRESS__ as well when AddressSanitizer
 * is enabled but __lsan_ignore_object() equivalent method is not supported
 * See also
 * https://docs.microsoft.com/en-us/cpp/sanitizers/asan-building?view=msvc-160
 */
#if !defined(_MSC_VER) && (defined(__SANITIZE_ADDRESS__) || g_macro__has_feature(address_sanitizer))

/*
 * %_GLIB_ADDRESS_SANITIZER:
 *
 * Private macro defined if the AddressSanitizer is in use by GLib itself.
 */
#define _GLIB_ADDRESS_SANITIZER

#include <sanitizer/lsan_interface.h>

/* If GLib itself is not compiled with ASAN sanitizer we may still want to
 * control it in case it's linked by the loading application, so we need to
 * do this check dynamically.
 * However MinGW/Cygwin doesn't support weak attribute properly (even if it advertises
 * it), so we ignore it in such case since it's not convenient to go through
 * dlsym().
 * Under MSVC we could use alternatename, but it doesn't seem to be as reliable
 * as we'd like: https://stackoverflow.com/a/11529277/210151 and
 * https://devblogs.microsoft.com/oldnewthing/20200731-00/?p=104024
 */
#elif defined (G_OS_UNIX) && !defined (__APPLE__) && !defined(__CYGWIN__) && !defined(_AIX) && \
      g_macro__has_attribute (weak)

#define HAS_DYNAMIC_ASAN_LOADING

void __lsan_enable (void) __attribute__ ((weak));
void __lsan_disable (void) __attribute__ ((weak));
void __lsan_ignore_object (const void *p) __attribute__ ((weak));

#endif

/**
 * G_CONTAINER_OF:
 * @ptr: a pointer to a member @field of type @type.
 * @type: the type of the container in which @field is embedded.
 * @field: the name of the field in @type.
 *
 * Casts away constness of @ptr.
 *
 * Returns: a pointer to the container, so that "&(@container)->field == (@ptr)" holds.
 */
#define G_CONTAINER_OF(ptr, type, field) ((type *) G_STRUCT_MEMBER_P (ptr, -G_STRUCT_OFFSET (type, field)))

/*
 * g_leak_sanitizer_is_supported:
 *
 * Checks at runtime if LeakSanitizer is currently supported by the running
 * binary. This may imply that GLib itself is not compiled with sanitizer
 * but that the loading program is.
 */
static inline gboolean
g_leak_sanitizer_is_supported (void)
{
#if defined (_GLIB_ADDRESS_SANITIZER)
  return TRUE;
#elif defined (HAS_DYNAMIC_ASAN_LOADING)
  return G_UNLIKELY (__lsan_enable != NULL && __lsan_ignore_object != NULL);
#else
  return FALSE;
#endif
}

/*
 * g_ignore_leak:
 * @p: any pointer
 *
 * Tell AddressSanitizer and similar tools that if the object pointed to
 * by @p is leaked, it is not a problem. Use this to suppress memory leak
 * reports when a potentially unreachable pointer is deliberately not
 * going to be deallocated.
 */
static inline void
g_ignore_leak (gconstpointer p)
{
#if defined (_GLIB_ADDRESS_SANITIZER)
  if (p != NULL)
    __lsan_ignore_object (p);
#elif defined (HAS_DYNAMIC_ASAN_LOADING)
  if (G_LIKELY (p != NULL) && G_UNLIKELY (__lsan_ignore_object != NULL))
    __lsan_ignore_object (p);
#endif
}

/*
 * g_ignore_strv_leak:
 * @strv: (nullable) (array zero-terminated=1): an array of strings
 *
 * The same as g_ignore_leak(), but for the memory pointed to by @strv,
 * and for each element of @strv.
 */
static inline void
g_ignore_strv_leak (GStrv strv)
{
  gchar **item;

  if (!g_leak_sanitizer_is_supported ())
    return;

  if (strv)
    {
      g_ignore_leak (strv);

      for (item = strv; *item != NULL; item++)
        g_ignore_leak (*item);
    }
}

/*
 * g_begin_ignore_leaks:
 *
 * Tell AddressSanitizer and similar tools to ignore all leaks from this point
 * onwards, until g_end_ignore_leaks() is called.
 *
 * Try to use g_ignore_leak() where possible to target deliberate leaks more
 * specifically.
 */
static inline void
g_begin_ignore_leaks (void)
{
#if defined (_GLIB_ADDRESS_SANITIZER)
  __lsan_disable ();
#elif defined (HAS_DYNAMIC_ASAN_LOADING)
  if (G_UNLIKELY (__lsan_disable != NULL))
    __lsan_disable ();
#endif
}

/*
 * g_end_ignore_leaks:
 *
 * Start ignoring leaks again; this must be paired with a previous call to
 * g_begin_ignore_leaks().
 */
static inline void
g_end_ignore_leaks (void)
{
#if defined (_GLIB_ADDRESS_SANITIZER)
  __lsan_enable ();
#elif defined (HAS_DYNAMIC_ASAN_LOADING)
  if (G_UNLIKELY (__lsan_enable != NULL))
    __lsan_enable ();
#endif
}

#undef HAS_DYNAMIC_ASAN_LOADING

GMainContext *          g_get_worker_context            (void);
gboolean                g_check_setuid                  (void);
GMainContext *          g_main_context_new_with_next_id (guint next_id);

#if (defined (HAVE__SET_THREAD_LOCAL_INVALID_PARAMETER_HANDLER) || \
     defined (HAVE__SET_INVALID_PARAMETER_HANDLER)) && \
    defined (HAVE__CRT_SET_REPORT_MODE)
# define USE_INVALID_PARAMETER_HANDLER
#endif

#ifdef USE_INVALID_PARAMETER_HANDLER
struct _GWin32InvalidParameterHandler
{
  _invalid_parameter_handler old_handler;
  _invalid_parameter_handler pushed_handler;
  int prev_report_mode;
  int pushed_report_mode;
};
#else
struct _GWin32InvalidParameterHandler
{
  int unused_really;
};
#endif

#ifdef G_OS_WIN32
GLIB_AVAILABLE_IN_ALL
gchar *_glib_get_locale_dir    (void);
#endif

GDir * g_dir_open_with_errno (const gchar *path, guint flags);
GDir * g_dir_new_from_dirp (gpointer dirp);

typedef struct _GWin32InvalidParameterHandler GWin32InvalidParameterHandler;
void g_win32_push_empty_invalid_parameter_handler (GWin32InvalidParameterHandler *items);
void g_win32_pop_invalid_parameter_handler (GWin32InvalidParameterHandler *items);

char *g_find_program_for_path (const char *program,
                               const char *path,
                               const char *working_dir);

int g_uri_get_default_scheme_port (const char *scheme);

#define GLIB_PRIVATE_CALL(symbol) (glib__private__()->symbol)


typedef struct {
  /* See gwakeup.c */
  GWakeup *             (* g_wakeup_new)                (void);
  void                  (* g_wakeup_free)               (GWakeup *wakeup);
  void                  (* g_wakeup_get_pollfd)         (GWakeup *wakeup,
                                                        GPollFD *poll_fd);
  void                  (* g_wakeup_signal)             (GWakeup *wakeup);
  void                  (* g_wakeup_acknowledge)        (GWakeup *wakeup);

  /* See gmain.c */
  GMainContext *        (* g_get_worker_context)        (void);

  gboolean              (* g_check_setuid)              (void);
  GMainContext *        (* g_main_context_new_with_next_id) (guint next_id);

  GDir *                (* g_dir_open_with_errno)       (const gchar *path,
                                                         guint        flags);
  GDir *                (* g_dir_new_from_dirp)         (gpointer dirp);

  /* See glib-init.c */
  void                  (* glib_init)                   (void);

  /* See gstdio.c */
#ifdef G_OS_WIN32
  int                   (* g_win32_stat_utf8)           (const gchar        *filename,
                                                         GWin32PrivateStat  *buf);

  int                   (* g_win32_lstat_utf8)          (const gchar        *filename,
                                                         GWin32PrivateStat  *buf);

  int                   (* g_win32_readlink_utf8)       (const gchar        *filename,
                                                         gchar              *buf,
                                                         gsize               buf_size,
                                                         gchar             **alloc_buf,
                                                         gboolean            terminate);

  int                   (* g_win32_fstat)               (int                 fd,
                                                         GWin32PrivateStat  *buf);

  /* See gwin32.c */
  gchar *(*g_win32_find_helper_executable_path) (const gchar *process_name,
                                                 void *dll_handle);

  int                   (* g_win32_reopen_noninherited) (int      fd,
                                                         int      mode,
                                                         GError **err);

  gboolean              (* g_win32_handle_is_socket)    (void *handle);

#endif

  /* See glib-private.c */
  void (* g_win32_push_empty_invalid_parameter_handler) (GWin32InvalidParameterHandler *items);

  void (* g_win32_pop_invalid_parameter_handler)        (GWin32InvalidParameterHandler *items);

  /* See gutils.c */
  char *(* g_find_program_for_path) (const char *program,
                                     const char *path,
                                     const char *working_dir);

  /* See guri.c */
  int (* g_uri_get_default_scheme_port) (const char *scheme);

  /* See gutils.c */
  gboolean (* g_set_prgname_once) (const gchar *prgname);

  gpointer (*g_datalist_id_update_atomic) (GData **datalist,
                                           GQuark key_id,
                                           GDataListUpdateAtomicFunc callback,
                                           gpointer user_data);

  /* Add other private functions here, initialize them in glib-private.c */
} GLibPrivateVTable;

GLIB_AVAILABLE_IN_ALL
const GLibPrivateVTable *glib__private__ (void);

/* Please see following for the use of ".ACP" over ""
 * on Windows, although both are accepted at compile-time
 * but "" renders translated console messages unreadable if
 * built with Visual Studio 2012 and later (this is, unfortunately,
 * undocumented):
 *
 * https://docs.microsoft.com/en-us/cpp/c-runtime-library/reference/setlocale-wsetlocale
 * https://gitlab.gnome.org/GNOME/glib/merge_requests/895#note_525881
 * https://gitlab.gnome.org/GNOME/glib/merge_requests/895#note_525900
 *
 * Additional related items:
 * https://stackoverflow.com/questions/22604329/php-5-5-setlocale-not-working-in-cli-on-windows
 * https://bugs.php.net/bug.php?id=66265
 */

#ifdef G_OS_WIN32
# define GLIB_DEFAULT_LOCALE ".ACP"
#else
# define GLIB_DEFAULT_LOCALE ""
#endif

gboolean g_uint_equal (gconstpointer v1, gconstpointer v2);
guint g_uint_hash (gconstpointer v);

#if defined(__GNUC__)
#define G_THREAD_LOCAL __thread
#else
#undef G_THREAD_LOCAL
#endif

/* Convenience wrapper to call private g_datalist_id_update_atomic() function. */
#define _g_datalist_id_update_atomic(datalist, key_id, callback, user_data) \
  (GLIB_PRIVATE_CALL (g_datalist_id_update_atomic) ((datalist), (key_id), (callback), (user_data)))

#endif /* __GLIB_PRIVATE_H__ */
