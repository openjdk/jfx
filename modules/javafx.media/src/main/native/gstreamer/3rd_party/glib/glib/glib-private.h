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
 * Private macro defined if the AddressSanitizer is in use.
 */
#define _GLIB_ADDRESS_SANITIZER

#include <sanitizer/lsan_interface.h>

#endif

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
#ifdef _GLIB_ADDRESS_SANITIZER
  if (p != NULL)
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
#ifdef _GLIB_ADDRESS_SANITIZER
  gchar **item;

  if (strv)
    {
      g_ignore_leak (strv);

      for (item = strv; *item != NULL; item++)
        g_ignore_leak (*item);
    }
#endif
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
#ifdef _GLIB_ADDRESS_SANITIZER
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
#ifdef _GLIB_ADDRESS_SANITIZER
  __lsan_enable ();
#endif
}

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

  /* Add other private functions here, initialize them in glib-private.c */
} GLibPrivateVTable;

GLIB_AVAILABLE_IN_ALL
GLibPrivateVTable *glib__private__ (void);

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

#endif /* __GLIB_PRIVATE_H__ */
