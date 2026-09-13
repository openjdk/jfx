/* gstdio.h - GFilename wrappers for C library functions
 *
 * Copyright 2004 Tor Lillqvist
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

#ifndef __G_STDIO_H__
#define __G_STDIO_H__

#include <glib/gprintf.h>

#include <errno.h>
#include <sys/stat.h>

G_BEGIN_DECLS

#if (defined (__MINGW64_VERSION_MAJOR) || defined (_MSC_VER)) && !defined(_WIN64)

/* Make it clear that we mean the struct with 32-bit st_size and
 * 32-bit st_*time fields as that is how the 32-bit GLib DLL normally
 * has been compiled. If you get a compiler warning when calling
 * g_stat(), do take it seriously and make sure that the type of
 * struct stat the code in GLib fills in matches the struct the type
 * of struct stat you pass to g_stat(). To avoid hassle, to get file
 * attributes just use the GIO API instead which doesn't use struct
 * stat.
 *
 * Sure, it would be nicer to use a struct with 64-bit st_size and
 * 64-bit st_*time fields, but changing that now would break ABI. And
 * in MinGW, a plain "struct stat" is the one with 32-bit st_size and
 * st_*time fields.
 */

typedef struct _stat32 GStatBuf;

#elif defined(__MINGW64_VERSION_MAJOR) && defined(_WIN64)

typedef struct _stat64 GStatBuf;

#else

typedef struct stat GStatBuf;

#endif

#if defined(G_OS_UNIX) && !defined(G_STDIO_WRAP_ON_UNIX) && !defined(__GI_SCANNER__)

/* Just pass on to the system functions, so there's no potential for data
 * format mismatches, especially with large file interfaces.
 * A few functions can't be handled in this way, since they are not defined
 * in a portable system header that we could include here.
 *
 * G_STDIO_WRAP_ON_UNIX is not public API and its behaviour is not guaranteed
 * in future.
 */

#ifndef __GTK_DOC_IGNORE__
#define g_chmod   chmod
#define g_open    open
#define g_creat   creat
#define g_rename  rename
#define g_mkdir   mkdir
#define g_stat    stat
#define g_lstat   lstat
#define g_remove  remove
#define g_fopen   fopen
#define g_freopen freopen
#define g_fsync   fsync
#define g_utime   utime
#endif

GLIB_AVAILABLE_IN_ALL
int g_access (const gchar *filename,
        int          mode);

GLIB_AVAILABLE_IN_ALL
int g_chdir  (const gchar *path);

GLIB_AVAILABLE_IN_ALL
int g_unlink (const gchar *filename);

GLIB_AVAILABLE_IN_ALL
int g_rmdir  (const gchar *filename);

#else /* ! G_OS_UNIX */

/* Wrappers for C library functions that take pathname arguments. On
 * Unix, the pathname is a file name as it literally is in the file
 * system. On well-maintained systems with consistent users who know
 * what they are doing and no exchange of files with others this would
 * be a well-defined encoding, preferably UTF-8. On Windows, the
 * pathname is always in UTF-8, even if that is not the on-disk
 * encoding, and not the encoding accepted by the C library or Win32
 * API.
 */

GLIB_AVAILABLE_IN_ALL
int g_access    (const gchar *filename,
     int          mode);

GLIB_AVAILABLE_IN_ALL
int g_chmod     (const gchar *filename,
     int          mode);

GLIB_AVAILABLE_IN_ALL
int g_open      (const gchar *filename,
                 int          flags,
                 int          mode);

GLIB_AVAILABLE_IN_ALL
int g_creat     (const gchar *filename,
                 int          mode);

GLIB_AVAILABLE_IN_ALL
int g_rename    (const gchar *oldfilename,
                 const gchar *newfilename);

GLIB_AVAILABLE_IN_ALL
int g_mkdir     (const gchar *filename,
                 int          mode);

GLIB_AVAILABLE_IN_ALL
int g_chdir     (const gchar *path);

GLIB_AVAILABLE_IN_ALL
int g_stat      (const gchar *filename,
                 GStatBuf    *buf);

GLIB_AVAILABLE_IN_ALL
int g_lstat     (const gchar *filename,
                 GStatBuf    *buf);

GLIB_AVAILABLE_IN_ALL
int g_unlink    (const gchar *filename);

GLIB_AVAILABLE_IN_ALL
int g_remove    (const gchar *filename);

GLIB_AVAILABLE_IN_ALL
int g_rmdir     (const gchar *filename);

GLIB_AVAILABLE_IN_ALL
FILE *g_fopen   (const gchar *filename,
                 const gchar *mode);

GLIB_AVAILABLE_IN_ALL
FILE *g_freopen (const gchar *filename,
                 const gchar *mode,
                 FILE        *stream);

GLIB_AVAILABLE_IN_2_64
gint g_fsync    (gint fd);

struct utimbuf;         /* Don't need the real definition of struct utimbuf when just
                                 * including this header.
                                 */

GLIB_AVAILABLE_IN_ALL
int g_utime     (const gchar    *filename,
     struct utimbuf *utb);

#endif /* G_OS_UNIX */

GLIB_AVAILABLE_IN_2_36
gboolean g_close (gint       fd,
                  GError   **error);

/**
 * g_clear_fd: (skip)
 * @fd_ptr: (not optional) (inout) (transfer full): a pointer to a file descriptor
 * @error: Used to return an error on failure
 *
 * If @fd_ptr points to a file descriptor, close it and return
 * whether closing it was successful, like g_close().
 * If @fd_ptr points to a negative number, return %TRUE without closing
 * anything.
 * In both cases, set @fd_ptr to `-1` before returning.
 *
 * Like g_close(), if closing the file descriptor fails, the error is
 * stored in both %errno and @error. If this function succeeds,
 * %errno is undefined.
 *
 * On POSIX platforms, this function is async-signal safe
 * if @error is %NULL and @fd_ptr points to either a negative number or a
 * valid open file descriptor.
 * This makes it safe to call from a signal handler or a #GSpawnChildSetupFunc
 * under those conditions.
 * See [`signal(7)`](man:signal(7)) and
 * [`signal-safety(7)`](man:signal-safety(7)) for more details.
 *
 * It is a programming error for @fd_ptr to point to a non-negative
 * number that is not a valid file descriptor.
 *
 * A typical use of this function is to clean up a file descriptor at
 * the end of its scope, whether it has been set successfully or not:
 *
 * |[
 * gboolean
 * operate_on_fd (GError **error)
 * {
 *   gboolean ret = FALSE;
 *   int fd = -1;
 *
 *   fd = open_a_fd (error);
 *
 *   if (fd < 0)
 *     goto out;
 *
 *   if (!do_something (fd, error))
 *     goto out;
 *
 *   if (!g_clear_fd (&fd, error))
 *     goto out;
 *
 *   ret = TRUE;
 *
 * out:
 *   // OK to call even if fd was never opened or was already closed
 *   g_clear_fd (&fd, NULL);
 *   return ret;
 * }
 * ]|
 *
 * This function is also useful in conjunction with #g_autofd.
 *
 * Returns: %TRUE on success
 * Since: 2.76
 */
GLIB_AVAILABLE_STATIC_INLINE_IN_2_76
static inline gboolean g_clear_fd (int     *fd_ptr,
                                   GError **error);

GLIB_AVAILABLE_STATIC_INLINE_IN_2_76
static inline gboolean
g_clear_fd (int     *fd_ptr,
            GError **error)
{
  int fd = *fd_ptr;

  *fd_ptr = -1;

  if (fd < 0)
    return TRUE;

  /* Suppress "Not available before" warning */
  G_GNUC_BEGIN_IGNORE_DEPRECATIONS
  return g_close (fd, error);
  G_GNUC_END_IGNORE_DEPRECATIONS
}

/* g_autofd should be defined on the same compilers where g_autofree is
 * This avoids duplicating the feature-detection here. */
#ifdef g_autofree
#ifndef __GTK_DOC_IGNORE__
/* Not public API */
static inline void
_g_clear_fd_ignore_error (int *fd_ptr)
{
  /* Don't overwrite thread-local errno if closing the fd fails */
  int errsv = errno;

  /* Suppress "Not available before" warning */
  G_GNUC_BEGIN_IGNORE_DEPRECATIONS

  if (!g_clear_fd (fd_ptr, NULL))
    {
      /* Do nothing: we ignore all errors, except for EBADF which
       * is a programming error, checked for by g_close(). */
    }

  G_GNUC_END_IGNORE_DEPRECATIONS

  errno = errsv;
}
#endif

#define g_autofd _GLIB_CLEANUP(_g_clear_fd_ignore_error) GLIB_AVAILABLE_MACRO_IN_2_76
#endif

G_END_DECLS

#endif /* __G_STDIO_H__ */
