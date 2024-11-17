/* GLIB - Library of useful routines for C programming
 * Copyright (C) 1995-1997  Peter Mattis, Spencer Kimball and Josh MacDonald
 *
 * gdir.c: Simplified wrapper around the DIRENT functions.
 *
 * Copyright 2001 Hans Breuer
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see <http://www.gnu.org/licenses/>.
 */

#include "config.h"

#include <errno.h>
#include <string.h>
#include <stdio.h>
#include <sys/stat.h>

#ifdef HAVE_DIRENT_H
#include <sys/types.h>
#include <dirent.h>
#endif

#include "gdir.h"

#include "gconvert.h"
#include "gfileutils.h"
#include "gstrfuncs.h"
#include "gtestutils.h"
#include "glibintl.h"

#if defined (_MSC_VER) && !defined (HAVE_DIRENT_H)
#include "dirent/dirent.h"
#endif

#ifdef GSTREAMER_LITE
#if defined (_MSC_VER) && !defined (HAVE_DIRENT_H)
#include "dirent/wdirent.c"
#endif
#endif // GSTREAMER_LITE

#include "glib-private.h" /* g_dir_open_with_errno, g_dir_new_from_dirp */

/**
 * GDir:
 *
 * An opaque structure representing an opened directory.
 */

struct _GDir
{
  gatomicrefcount ref_count;
#ifdef G_OS_WIN32
  _WDIR *wdirp;
#else
  DIR *dirp;
#endif
#ifdef G_OS_WIN32
  /* maximum encoding of FILENAME_MAX UTF-8 characters, plus a nul terminator
   * (FILENAME_MAX is not guaranteed to include one) */
  gchar utf8_buf[FILENAME_MAX*4 + 1];
#endif
};

/*< private >
 * g_dir_open_with_errno:
 * @path: the path to the directory you are interested in.
 * @flags: Currently must be set to 0. Reserved for future use.
 *
 * Opens a directory for reading.
 *
 * This function is equivalent to g_dir_open() except in the error case,
 * errno will be set accordingly.
 *
 * This is useful if you want to construct your own error message.
 *
 * Returns: a newly allocated #GDir on success, or %NULL on failure,
 *   with errno set accordingly.
 *
 * Since: 2.38
 */
GDir *
g_dir_open_with_errno (const gchar *path,
                       guint        flags)
{
#ifdef G_OS_WIN32
  GDir *dir;
  _WDIR *wdirp;
  gint saved_errno;
  wchar_t *wpath;
#else
  DIR *dirp;
#endif

  g_return_val_if_fail (path != NULL, NULL);

#ifdef G_OS_WIN32
  wpath = g_utf8_to_utf16 (path, -1, NULL, NULL, NULL);

  g_return_val_if_fail (wpath != NULL, NULL);

  wdirp = _wopendir (wpath);
  saved_errno = errno;
  g_free (wpath);
  errno = saved_errno;

  if (wdirp == NULL)
    return NULL;

  dir = g_new0 (GDir, 1);
  g_atomic_ref_count_init (&dir->ref_count);
  dir->wdirp = wdirp;

  return g_steal_pointer (&dir);
#else
  dirp = opendir (path);

  if (dirp == NULL)
    return NULL;

  return g_dir_new_from_dirp (dirp);
#endif
}

/**
 * g_dir_open: (constructor)
 * @path: the path to the directory you are interested in. On Unix
 *         in the on-disk encoding. On Windows in UTF-8
 * @flags: Currently must be set to 0. Reserved for future use.
 * @error: return location for a #GError, or %NULL.
 *         If non-%NULL, an error will be set if and only if
 *         g_dir_open() fails.
 *
 * Opens a directory for reading. The names of the files in the
 * directory can then be retrieved using g_dir_read_name().  Note
 * that the ordering is not defined.
 *
 * Returns: (transfer full): a newly allocated #GDir on success, %NULL on failure.
 *   If non-%NULL, you must free the result with g_dir_close()
 *   when you are finished with it.
 **/
GDir *
g_dir_open (const gchar  *path,
            guint         flags,
            GError      **error)
{
  gint saved_errno;
  GDir *dir;

  dir = g_dir_open_with_errno (path, flags);

  if (dir == NULL)
    {
      gchar *utf8_path;

      saved_errno = errno;

      utf8_path = g_filename_to_utf8 (path, -1, NULL, NULL, NULL);

      g_set_error (error, G_FILE_ERROR, g_file_error_from_errno (saved_errno),
                   _("Error opening directory '%s': %s"), utf8_path, g_strerror (saved_errno));
      g_free (utf8_path);
    }

  return dir;
}

/*< private >
 * g_dir_new_from_dirp:
 * @dirp: a #DIR* created by opendir() or fdopendir()
 *
 * Creates a #GDir object from the DIR object that is created using
 * opendir() or fdopendir().  The created #GDir assumes ownership of the
 * passed-in #DIR pointer.
 *
 * @dirp must not be %NULL.
 *
 * This function never fails.
 *
 * Returns: a newly allocated #GDir, which should be closed using
 *     g_dir_close().
 *
 * Since: 2.38
 **/
GDir *
g_dir_new_from_dirp (gpointer dirp)
{
#ifdef G_OS_UNIX
  GDir *dir;

  g_return_val_if_fail (dirp != NULL, NULL);

  dir = g_new0 (GDir, 1);
  g_atomic_ref_count_init (&dir->ref_count);
  dir->dirp = dirp;

  return dir;
#else
  g_assert_not_reached ();

  return NULL;
#endif
}

/**
 * g_dir_read_name:
 * @dir: a #GDir* created by g_dir_open()
 *
 * Retrieves the name of another entry in the directory, or %NULL.
 * The order of entries returned from this function is not defined,
 * and may vary by file system or other operating-system dependent
 * factors.
 *
 * %NULL may also be returned in case of errors. On Unix, you can
 * check `errno` to find out if %NULL was returned because of an error.
 *
 * On Unix, the '.' and '..' entries are omitted, and the returned
 * name is in the on-disk encoding.
 *
 * On Windows, as is true of all GLib functions which operate on
 * filenames, the returned name is in UTF-8.
 *
 * Returns: (type filename): The entry's name or %NULL if there are no
 *   more entries. The return value is owned by GLib and
 *   must not be modified or freed.
 **/
const gchar *
g_dir_read_name (GDir *dir)
{
#ifdef G_OS_WIN32
  gchar *utf8_name;
  struct _wdirent *wentry;
#else
  struct dirent *entry;
#endif

  g_return_val_if_fail (dir != NULL, NULL);

#ifdef G_OS_WIN32
  while (1)
    {
      wentry = _wreaddir (dir->wdirp);
      while (wentry
       && (0 == wcscmp (wentry->d_name, L".") ||
     0 == wcscmp (wentry->d_name, L"..")))
  wentry = _wreaddir (dir->wdirp);

      if (wentry == NULL)
  return NULL;

      utf8_name = g_utf16_to_utf8 (wentry->d_name, -1, NULL, NULL, NULL);

      if (utf8_name == NULL)
    continue;       /* Huh, impossible? Skip it anyway */

      strcpy (dir->utf8_buf, utf8_name);
      g_free (utf8_name);

      return dir->utf8_buf;
    }
#else
  entry = readdir (dir->dirp);
  while (entry
         && (0 == strcmp (entry->d_name, ".") ||
             0 == strcmp (entry->d_name, "..")))
    entry = readdir (dir->dirp);

  if (entry)
    return entry->d_name;
  else
    return NULL;
#endif
}

/**
 * g_dir_rewind:
 * @dir: a #GDir* created by g_dir_open()
 *
 * Resets the given directory. The next call to g_dir_read_name()
 * will return the first entry again.
 **/
void
g_dir_rewind (GDir *dir)
{
  g_return_if_fail (dir != NULL);

#ifdef G_OS_WIN32
  _wrewinddir (dir->wdirp);
#else
  rewinddir (dir->dirp);
#endif
}

static void
g_dir_actually_close (GDir *dir)
{
#ifdef G_OS_WIN32
  g_clear_pointer (&dir->wdirp, _wclosedir);
#else
  g_clear_pointer (&dir->dirp, closedir);
#endif
}

/**
 * g_dir_close:
 * @dir: (transfer full): a #GDir* created by g_dir_open()
 *
 * Closes the directory immediately and decrements the reference count.
 *
 * Once the reference count reaches zero, the `GDir` structure itself will be
 * freed. Prior to GLib 2.80, `GDir` was not reference counted.
 *
 * It is an error to call any of the `GDir` methods other than
 * [method@GLib.Dir.ref] and [method@GLib.Dir.unref] on a `GDir` after calling
 * [method@GLib.Dir.close] on it.
 **/
void
g_dir_close (GDir *dir)
{
  g_return_if_fail (dir != NULL);

  g_dir_actually_close (dir);
  g_dir_unref (dir);
}

/**
 * g_dir_ref:
 * @dir: (transfer none): a `GDir`
 *
 * Increment the reference count of `dir`.
 *
 * Returns: (transfer full): the same pointer as `dir`
 * Since: 2.80
 */
GDir *
g_dir_ref (GDir *dir)
{
  g_return_val_if_fail (dir != NULL, NULL);

  g_atomic_ref_count_inc (&dir->ref_count);
  return dir;
}

/**
 * g_dir_unref:
 * @dir: (transfer full): a `GDir`
 *
 * Decrements the reference count of `dir`.
 *
 * Once the reference count reaches zero, the directory will be closed and all
 * resources associated with it will be freed. If [method@GLib.Dir.close] is
 * called when the reference count is greater than zero, the directory is closed
 * but the `GDir` structure will not be freed until its reference count reaches
 * zero.
 *
 * It is an error to call any of the `GDir` methods other than
 * [method@GLib.Dir.ref] and [method@GLib.Dir.unref] on a `GDir` after calling
 * [method@GLib.Dir.close] on it.
 *
 * Since: 2.80
 */
void
g_dir_unref (GDir *dir)
{
  g_return_if_fail (dir != NULL);

  if (g_atomic_ref_count_dec (&dir->ref_count))
    {
      g_dir_actually_close (dir);
      g_free (dir);
    }
}

#ifdef G_OS_WIN32

/* Binary compatibility versions. Not for newly compiled code. */

_GLIB_EXTERN GDir        *g_dir_open_utf8      (const gchar  *path,
                                                guint         flags,
                                                GError      **error);
_GLIB_EXTERN const gchar *g_dir_read_name_utf8 (GDir         *dir);

GDir *
g_dir_open_utf8 (const gchar  *path,
                 guint         flags,
                 GError      **error)
{
  return g_dir_open (path, flags, error);
}

const gchar *
g_dir_read_name_utf8 (GDir *dir)
{
  return g_dir_read_name (dir);
}

#endif
