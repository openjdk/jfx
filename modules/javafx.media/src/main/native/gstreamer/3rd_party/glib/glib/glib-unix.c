/* GLIB - Library of useful routines for C programming
 * Copyright 2000-2022 Red Hat, Inc.
 * Copyright 2006-2007 Matthias Clasen
 * Copyright 2006 Padraig O'Briain
 * Copyright 2007 Lennart Poettering
 * Copyright 2018-2022 Endless OS Foundation, LLC
 * Copyright 2018 Peter Wu
 * Copyright 2019 Ting-Wei Lan
 * Copyright 2019 Sebastian Schwarz
 * Copyright 2020 Matt Rose
 * Copyright 2021 Casper Dik
 * Copyright 2022 Alexander Richardson
 * Copyright 2022 Ray Strode
 * Copyright 2022 Thomas Haller
 * Copyright 2023-2024 Collabora Ltd.
 * Copyright 2023 Sebastian Wilhelmi
 * Copyright 2023 CaiJingLong
 *
 * glib-unix.c: UNIX specific API wrappers and convenience functions
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
 *
 * Authors: Colin Walters <walters@verbum.org>
 */

#include "config.h"

#include "glib-private.h"
#include "glib-unix.h"
#include "glib-unixprivate.h"
#include "gmain-internal.h"

#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <stdlib.h>   /* for fdwalk */
#include <string.h>
#include <sys/types.h>
#include <pwd.h>
#include <unistd.h>

#if defined(__linux__) || defined(__DragonFly__)
#include <sys/syscall.h>  /* for syscall and SYS_getdents64 */
#endif

#ifdef HAVE_SYS_RESOURCE_H
#include <sys/resource.h>
#endif /* HAVE_SYS_RESOURCE_H */

#if defined(__APPLE__) && defined(HAVE_LIBPROC_H)
#include <libproc.h>
#include <sys/proc_info.h>
#endif

G_STATIC_ASSERT (sizeof (ssize_t) == GLIB_SIZEOF_SSIZE_T);
G_STATIC_ASSERT (G_ALIGNOF (gssize) == G_ALIGNOF (ssize_t));
G_STATIC_ASSERT (G_SIGNEDNESS_OF (ssize_t) == 1);

G_STATIC_ASSERT (sizeof (GPid) == sizeof (pid_t));
G_STATIC_ASSERT (G_ALIGNOF (GPid) == G_ALIGNOF (pid_t));
/* It's platform-dependent whether pid_t is signed, so no assertion */

/* If this assertion fails, then the ABI of g_unix_open_pipe() would be
 * ambiguous on this platform.
 * On Linux, usually O_NONBLOCK == 04000 and FD_CLOEXEC == 1, but the same
 * might not be true everywhere. */
G_STATIC_ASSERT (O_NONBLOCK != FD_CLOEXEC);

G_DEFINE_QUARK (g-unix-error-quark, g_unix_error)

static gboolean
g_unix_set_error_from_errno (GError **error,
                             gint     saved_errno)
{
  g_set_error_literal (error,
                       G_UNIX_ERROR,
                       0,
                       g_strerror (saved_errno));
  errno = saved_errno;
  return FALSE;
}

/**
 * g_unix_open_pipe:
 * @fds: (array fixed-size=2): Array of two integers
 * @flags: Bitfield of file descriptor flags, as for fcntl()
 * @error: a #GError
 *
 * Similar to the UNIX pipe() call, but on modern systems like Linux
 * uses the pipe2() system call, which atomically creates a pipe with
 * the configured flags.
 *
 * As of GLib 2.78, the supported flags are `O_CLOEXEC`/`FD_CLOEXEC` (see below)
 * and `O_NONBLOCK`. Prior to GLib 2.78, only `FD_CLOEXEC` was supported — if
 * you wanted to configure `O_NONBLOCK` then that had to be done separately with
 * `fcntl()`.
 *
 * Since GLib 2.80, the constants %G_UNIX_PIPE_END_READ and
 * %G_UNIX_PIPE_END_WRITE can be used as mnemonic indexes in @fds.
 *
 * It is a programmer error to call this function with unsupported flags, and a
 * critical warning will be raised.
 *
 * As of GLib 2.78, it is preferred to pass `O_CLOEXEC` in, rather than
 * `FD_CLOEXEC`, as that matches the underlying `pipe()` API more closely. Prior
 * to 2.78, only `FD_CLOEXEC` was supported. Support for `FD_CLOEXEC` may be
 * deprecated and removed in future.
 *
 * Returns: %TRUE on success, %FALSE if not (and errno will be set).
 *
 * Since: 2.30
 */
gboolean
g_unix_open_pipe (int     *fds,
                  int      flags,
                  GError **error)
{
  /* We only support O_CLOEXEC/FD_CLOEXEC and O_NONBLOCK */
  g_return_val_if_fail ((flags & (O_CLOEXEC | FD_CLOEXEC | O_NONBLOCK)) == flags, FALSE);

#if O_CLOEXEC != FD_CLOEXEC && !defined(G_DISABLE_CHECKS)
  if (flags & FD_CLOEXEC)
    g_debug ("g_unix_open_pipe() called with FD_CLOEXEC; please migrate to using O_CLOEXEC instead");
#endif

  if (!g_unix_open_pipe_internal (fds,
                                  (flags & (O_CLOEXEC | FD_CLOEXEC)) != 0,
                                  (flags & O_NONBLOCK) != 0))
    return g_unix_set_error_from_errno (error, errno);

  return TRUE;
}

/**
 * g_unix_set_fd_nonblocking:
 * @fd: A file descriptor
 * @nonblock: If %TRUE, set the descriptor to be non-blocking
 * @error: a #GError
 *
 * Control the non-blocking state of the given file descriptor,
 * according to @nonblock. On most systems this uses %O_NONBLOCK, but
 * on some older ones may use %O_NDELAY.
 *
 * Returns: %TRUE if successful
 *
 * Since: 2.30
 */
gboolean
g_unix_set_fd_nonblocking (gint       fd,
                           gboolean   nonblock,
                           GError   **error)
{
#ifdef F_GETFL
  glong fcntl_flags;
  fcntl_flags = fcntl (fd, F_GETFL);

  if (fcntl_flags == -1)
    return g_unix_set_error_from_errno (error, errno);

  if (nonblock)
    fcntl_flags |= O_NONBLOCK;
  else
    fcntl_flags &= ~O_NONBLOCK;

  if (fcntl (fd, F_SETFL, fcntl_flags) == -1)
    return g_unix_set_error_from_errno (error, errno);
  return TRUE;
#else
  return g_unix_set_error_from_errno (error, EINVAL);
#endif
}

/**
 * g_unix_signal_source_new:
 * @signum: A signal number
 *
 * Create a #GSource that will be dispatched upon delivery of the UNIX
 * signal @signum.  In GLib versions before 2.36, only `SIGHUP`, `SIGINT`,
 * `SIGTERM` can be monitored.  In GLib 2.36, `SIGUSR1` and `SIGUSR2`
 * were added. In GLib 2.54, `SIGWINCH` was added.
 *
 * Note that unlike the UNIX default, all sources which have created a
 * watch will be dispatched, regardless of which underlying thread
 * invoked g_unix_signal_source_new().
 *
 * For example, an effective use of this function is to handle `SIGTERM`
 * cleanly; flushing any outstanding files, and then calling
 * g_main_loop_quit().  It is not safe to do any of this from a regular
 * UNIX signal handler; such a handler may be invoked while malloc() or
 * another library function is running, causing reentrancy issues if the
 * handler attempts to use those functions.  None of the GLib/GObject
 * API is safe against this kind of reentrancy.
 *
 * The interaction of this source when combined with native UNIX
 * functions like sigprocmask() is not defined.
 *
 * The source will not initially be associated with any #GMainContext
 * and must be added to one with g_source_attach() before it will be
 * executed.
 *
 * Returns: A newly created #GSource
 *
 * Since: 2.30
 */
GSource *
g_unix_signal_source_new (int signum)
{
  g_return_val_if_fail (signum == SIGHUP || signum == SIGINT || signum == SIGTERM ||
                        signum == SIGUSR1 || signum == SIGUSR2 || signum == SIGWINCH,
                        NULL);

  return _g_main_create_unix_signal_watch (signum);
}

/**
 * g_unix_signal_add_full: (rename-to g_unix_signal_add)
 * @priority: the priority of the signal source. Typically this will be in
 *            the range between %G_PRIORITY_DEFAULT and %G_PRIORITY_HIGH.
 * @signum: Signal number
 * @handler: Callback
 * @user_data: Data for @handler
 * @notify: #GDestroyNotify for @handler
 *
 * A convenience function for g_unix_signal_source_new(), which
 * attaches to the default #GMainContext.  You can remove the watch
 * using g_source_remove().
 *
 * Returns: An ID (greater than 0) for the event source
 *
 * Since: 2.30
 */
guint
g_unix_signal_add_full (int            priority,
                        int            signum,
                        GSourceFunc    handler,
                        gpointer       user_data,
                        GDestroyNotify notify)
{
  guint id;
  GSource *source;

  source = g_unix_signal_source_new (signum);

  if (priority != G_PRIORITY_DEFAULT)
    g_source_set_priority (source, priority);

  g_source_set_callback (source, handler, user_data, notify);
  id = g_source_attach (source, NULL);
  g_source_unref (source);

  return id;
}

/**
 * g_unix_signal_add:
 * @signum: Signal number
 * @handler: Callback
 * @user_data: Data for @handler
 *
 * A convenience function for g_unix_signal_source_new(), which
 * attaches to the default #GMainContext.  You can remove the watch
 * using g_source_remove().
 *
 * Returns: An ID (greater than 0) for the event source
 *
 * Since: 2.30
 */
guint
g_unix_signal_add (int         signum,
                   GSourceFunc handler,
                   gpointer    user_data)
{
  return g_unix_signal_add_full (G_PRIORITY_DEFAULT, signum, handler, user_data, NULL);
}

typedef struct
{
  GSource source;

  gint     fd;
  gpointer tag;
} GUnixFDSource;

static gboolean
g_unix_fd_source_dispatch (GSource     *source,
                           GSourceFunc  callback,
                           gpointer     user_data)
{
  GUnixFDSource *fd_source = (GUnixFDSource *) source;
  GUnixFDSourceFunc func = (GUnixFDSourceFunc) callback;

  if (!callback)
    {
      g_warning ("GUnixFDSource dispatched without callback. "
                 "You must call g_source_set_callback().");
      return FALSE;
    }

  return (* func) (fd_source->fd, g_source_query_unix_fd (source, fd_source->tag), user_data);
}

GSourceFuncs g_unix_fd_source_funcs = {
  NULL, NULL, g_unix_fd_source_dispatch, NULL, NULL, NULL
};

/**
 * g_unix_fd_source_new:
 * @fd: a file descriptor
 * @condition: I/O conditions to watch for on @fd
 *
 * Creates a #GSource to watch for a particular I/O condition on a file
 * descriptor.
 *
 * The source will never close the @fd — you must do it yourself.
 *
 * Any callback attached to the returned #GSource must have type
 * #GUnixFDSourceFunc.
 *
 * Returns: the newly created #GSource
 *
 * Since: 2.36
 **/
GSource *
g_unix_fd_source_new (gint         fd,
                      GIOCondition condition)
{
  GUnixFDSource *fd_source;
  GSource *source;

  source = g_source_new (&g_unix_fd_source_funcs, sizeof (GUnixFDSource));
  fd_source = (GUnixFDSource *) source;

  fd_source->fd = fd;
  fd_source->tag = g_source_add_unix_fd (source, fd, condition);

  return source;
}

/**
 * g_unix_fd_add_full:
 * @priority: the priority of the source
 * @fd: a file descriptor
 * @condition: IO conditions to watch for on @fd
 * @function: a #GUnixFDSourceFunc
 * @user_data: data to pass to @function
 * @notify: function to call when the idle is removed, or %NULL
 *
 * Sets a function to be called when the IO condition, as specified by
 * @condition becomes true for @fd.
 *
 * This is the same as g_unix_fd_add(), except that it allows you to
 * specify a non-default priority and a provide a #GDestroyNotify for
 * @user_data.
 *
 * Returns: the ID (greater than 0) of the event source
 *
 * Since: 2.36
 **/
guint
g_unix_fd_add_full (gint              priority,
                    gint              fd,
                    GIOCondition      condition,
                    GUnixFDSourceFunc function,
                    gpointer          user_data,
                    GDestroyNotify    notify)
{
  GSource *source;
  guint id;

  g_return_val_if_fail (function != NULL, 0);

  source = g_unix_fd_source_new (fd, condition);

  if (priority != G_PRIORITY_DEFAULT)
    g_source_set_priority (source, priority);

  g_source_set_callback (source, (GSourceFunc) function, user_data, notify);
  id = g_source_attach (source, NULL);
  g_source_unref (source);

  return id;
}

/**
 * g_unix_fd_add:
 * @fd: a file descriptor
 * @condition: IO conditions to watch for on @fd
 * @function: a #GUnixFDSourceFunc
 * @user_data: data to pass to @function
 *
 * Sets a function to be called when the IO condition, as specified by
 * @condition becomes true for @fd.
 *
 * @function will be called when the specified IO condition becomes
 * %TRUE.  The function is expected to clear whatever event caused the
 * IO condition to become true and return %TRUE in order to be notified
 * when it happens again.  If @function returns %FALSE then the watch
 * will be cancelled.
 *
 * The return value of this function can be passed to g_source_remove()
 * to cancel the watch at any time that it exists.
 *
 * The source will never close the fd -- you must do it yourself.
 *
 * Returns: the ID (greater than 0) of the event source
 *
 * Since: 2.36
 **/
guint
g_unix_fd_add (gint              fd,
               GIOCondition      condition,
               GUnixFDSourceFunc function,
               gpointer          user_data)
{
  return g_unix_fd_add_full (G_PRIORITY_DEFAULT, fd, condition, function, user_data, NULL);
}

/**
 * g_unix_get_passwd_entry:
 * @user_name: the username to get the passwd file entry for
 * @error: return location for a #GError, or %NULL
 *
 * Get the `passwd` file entry for the given @user_name using `getpwnam_r()`.
 * This can fail if the given @user_name doesn’t exist.
 *
 * The returned `struct passwd` has been allocated using g_malloc() and should
 * be freed using g_free(). The strings referenced by the returned struct are
 * included in the same allocation, so are valid until the `struct passwd` is
 * freed.
 *
 * This function is safe to call from multiple threads concurrently.
 *
 * You will need to include `pwd.h` to get the definition of `struct passwd`.
 *
 * Returns: (transfer full): passwd entry, or %NULL on error; free the returned
 *    value with g_free()
 * Since: 2.64
 */
struct passwd *
g_unix_get_passwd_entry (const gchar  *user_name,
                         GError      **error)
{
  struct passwd *passwd_file_entry;
  struct
    {
      struct passwd pwd;
      char string_buffer[];
    } *buffer = NULL;
  gsize string_buffer_size = 0;
  GError *local_error = NULL;

  g_return_val_if_fail (user_name != NULL, NULL);
  g_return_val_if_fail (error == NULL || *error == NULL, NULL);

#ifdef _SC_GETPW_R_SIZE_MAX
    {
      /* Get the recommended buffer size */
      glong string_buffer_size_long = sysconf (_SC_GETPW_R_SIZE_MAX);
      if (string_buffer_size_long > 0)
        string_buffer_size = string_buffer_size_long;
    }
#endif /* _SC_GETPW_R_SIZE_MAX */

  /* Default starting size. */
  if (string_buffer_size == 0)
    string_buffer_size = 64;

  do
    {
      int retval;

      g_free (buffer);
      /* Allocate space for the `struct passwd`, and then a buffer for all its
       * strings (whose size is @string_buffer_size, which increases in this
       * loop until it’s big enough). Add 6 extra bytes to work around a bug in
       * macOS < 10.3. See #156446.
       */
      buffer = g_malloc0 (sizeof (*buffer) + string_buffer_size + 6);

      retval = getpwnam_r (user_name, &buffer->pwd, buffer->string_buffer,
                           string_buffer_size, &passwd_file_entry);

      /* Bail out if: the lookup was successful, or if the user id can't be
       * found (should be pretty rare case actually), or if the buffer should be
       * big enough and yet lookups are still not successful.
       */
      if (passwd_file_entry != NULL)
        {
          /* Success. */
          break;
        }
      else if (retval == 0 ||
          retval == ENOENT || retval == ESRCH ||
          retval == EBADF || retval == EPERM)
        {
          /* Username not found. */
          g_unix_set_error_from_errno (&local_error, retval);
          break;
        }
      else if (retval == ERANGE)
        {
          /* Can’t allocate enough string buffer space. */
          if (string_buffer_size > 32 * 1024)
            {
              g_unix_set_error_from_errno (&local_error, retval);
              break;
            }

          string_buffer_size *= 2;
          continue;
        }
      else
        {
          g_unix_set_error_from_errno (&local_error, retval);
          break;
        }
    }
  while (passwd_file_entry == NULL);

  g_assert (passwd_file_entry == NULL ||
            (gpointer) passwd_file_entry == (gpointer) buffer);

  /* Success or error. */
  if (local_error != NULL)
    {
      g_clear_pointer (&buffer, g_free);
      g_propagate_error (error, g_steal_pointer (&local_error));
    }

  return (struct passwd *) g_steal_pointer (&buffer);
}

/* This function is called between fork() and exec() and hence must be
 * async-signal-safe (see signal-safety(7)). */
static int
set_cloexec (void *data, gint fd)
{
  if (fd >= GPOINTER_TO_INT (data))
    fcntl (fd, F_SETFD, FD_CLOEXEC);

  return 0;
}

/* fdwalk()-compatible callback to close a fd for non-compliant
 * implementations of fdwalk() that potentially pass already
 * closed fds.
 *
 * It is not an error to pass an invalid fd to this function.
 *
 * This function is called between fork() and exec() and hence must be
 * async-signal-safe (see signal-safety(7)).
 */
G_GNUC_UNUSED static int
close_func_with_invalid_fds (void *data, int fd)
{
  /* We use close and not g_close here because on some platforms, we
   * don't know how to close only valid, open file descriptors, so we
   * have to pass bad fds to close too. g_close warns if given a bad
   * fd.
   *
   * This function returns no error, because there is nothing that the caller
   * could do with that information. That is even the case for EINTR. See
   * g_close() about the specialty of EINTR and why that is correct.
   * If g_close() ever gets extended to handle EINTR specially, then this place
   * should get updated to do the same handling.
   */
  if (fd >= GPOINTER_TO_INT (data))
    close (fd);

  return 0;
}

#ifdef __linux__
struct linux_dirent64
{
  guint64        d_ino;    /* 64-bit inode number */
  guint64        d_off;    /* 64-bit offset to next structure */
  unsigned short d_reclen; /* Size of this dirent */
  unsigned char  d_type;   /* File type */
  char           d_name[]; /* Filename (null-terminated) */
};

/* This function is called between fork() and exec() and hence must be
 * async-signal-safe (see signal-safety(7)). */
static gint
filename_to_fd (const char *p)
{
  char c;
  int fd = 0;
  const int cutoff = G_MAXINT / 10;
  const int cutlim = G_MAXINT % 10;

  if (*p == '\0')
    return -1;

  while ((c = *p++) != '\0')
    {
      if (c < '0' || c > '9')
        return -1;
      c -= '0';

      /* Check for overflow. */
      if (fd > cutoff || (fd == cutoff && c > cutlim))
        return -1;

      fd = fd * 10 + c;
    }

  return fd;
}
#endif

static int safe_fdwalk_with_invalid_fds (int (*cb)(void *data, int fd), void *data);

/* This function is called between fork() and exec() and hence must be
 * async-signal-safe (see signal-safety(7)). */
static int
safe_fdwalk (int (*cb)(void *data, int fd), void *data)
{
#if 0
  /* Use fdwalk function provided by the system if it is known to be
   * async-signal safe.
   *
   * Currently there are no operating systems known to provide a safe
   * implementation, so this section is not used for now.
   */
  return fdwalk (cb, data);
#else
  /* Fallback implementation of fdwalk. It should be async-signal safe, but it
   * may fail on non-Linux operating systems. See safe_fdwalk_with_invalid_fds
   * for a slower alternative.
   */

#ifdef __linux__
  gint fd;
  gint res = 0;

  /* Avoid use of opendir/closedir since these are not async-signal-safe. */
  int dir_fd = open ("/proc/self/fd", O_RDONLY | O_DIRECTORY);
  if (dir_fd >= 0)
    {
      /* buf needs to be aligned correctly to receive linux_dirent64.
       * C11 has _Alignof for this purpose, but for now a
       * union serves the same purpose. */
      union
      {
        char buf[4096];
        struct linux_dirent64 alignment;
      } u;
      int pos, nread;
      struct linux_dirent64 *de;

      while ((nread = syscall (SYS_getdents64, dir_fd, u.buf, sizeof (u.buf))) > 0)
        {
          for (pos = 0; pos < nread; pos += de->d_reclen)
            {
              de = (struct linux_dirent64 *) (u.buf + pos);

              fd = filename_to_fd (de->d_name);
              if (fd < 0 || fd == dir_fd)
                  continue;

              if ((res = cb (data, fd)) != 0)
                  break;
            }
        }

      g_close (dir_fd, NULL);
      return res;
    }

  /* If /proc is not mounted or not accessible we fail here and rely on
   * safe_fdwalk_with_invalid_fds to fall back to the old
   * rlimit trick. */

#endif

#if defined(__sun__) && defined(F_PREVFD) && defined(F_NEXTFD)
/*
 * Solaris 11.4 has a signal-safe way which allows
 * us to find all file descriptors in a process.
 *
 * fcntl(fd, F_NEXTFD, maxfd)
 * - returns the first allocated file descriptor <= maxfd  > fd.
 *
 * fcntl(fd, F_PREVFD)
 * - return highest allocated file descriptor < fd.
 */
  gint open_max;
  gint fd;
  gint res = 0;

  open_max = fcntl (INT_MAX, F_PREVFD); /* find the maximum fd */
  if (open_max < 0) /* No open files */
    return 0;

  for (fd = -1; (fd = fcntl (fd, F_NEXTFD, open_max)) != -1; )
    if ((res = cb (data, fd)) != 0 || fd == open_max)
      break;

  return res;
#endif

  return safe_fdwalk_with_invalid_fds (cb, data);
#endif
}

/* This function is called between fork() and exec() and hence must be
 * async-signal-safe (see signal-safety(7)). */
static int
safe_fdwalk_with_invalid_fds (int (*cb)(void *data, int fd), void *data)
{
  /* Fallback implementation of fdwalk. It should be async-signal safe, but it
   * may be slow, especially on systems allowing very high number of open file
   * descriptors.
   */
  gint open_max = -1;
  gint fd;
  gint res = 0;

#if 0 && defined(HAVE_SYS_RESOURCE_H)
  struct rlimit rl;

  /* Use getrlimit() function provided by the system if it is known to be
   * async-signal safe.
   *
   * Currently there are no operating systems known to provide a safe
   * implementation, so this section is not used for now.
   */
  if (getrlimit (RLIMIT_NOFILE, &rl) == 0 && rl.rlim_max != RLIM_INFINITY)
    open_max = rl.rlim_max;
#endif
#if defined(__FreeBSD__) || defined(__OpenBSD__) || defined(__APPLE__)
  /* Use sysconf() function provided by the system if it is known to be
   * async-signal safe.
   *
   * FreeBSD: sysconf() is included in the list of async-signal safe functions
   * found in https://man.freebsd.org/sigaction(2).
   *
   * OpenBSD: sysconf() is included in the list of async-signal safe functions
   * found in https://man.openbsd.org/sigaction.2.
   *
   * Apple: sysconf() is included in the list of async-signal safe functions
   * found in https://opensource.apple.com/source/xnu/xnu-517.12.7/bsd/man/man2/sigaction.2
   */
  if (open_max < 0)
    open_max = sysconf (_SC_OPEN_MAX);
#endif
  /* Hardcoded fallback: the default process hard limit in Linux as of 2020 */
  if (open_max < 0)
    open_max = 4096;

#if defined(__APPLE__) && defined(HAVE_LIBPROC_H)
  /* proc_pidinfo isn't documented as async-signal-safe but looking at the implementation
   * in the darwin tree here:
   *
   * https://opensource.apple.com/source/Libc/Libc-498/darwin/libproc.c.auto.html
   *
   * It's just a thin wrapper around a syscall, so it's probably okay.
   */
  {
    char buffer[4096 * PROC_PIDLISTFD_SIZE];
    ssize_t buffer_size;

    buffer_size = proc_pidinfo (getpid (), PROC_PIDLISTFDS, 0, buffer, sizeof (buffer));

    if (buffer_size > 0 &&
        sizeof (buffer) >= (size_t) buffer_size &&
        (buffer_size % PROC_PIDLISTFD_SIZE) == 0)
      {
        const struct proc_fdinfo *fd_info = (const struct proc_fdinfo *) buffer;
        size_t number_of_fds = (size_t) buffer_size / PROC_PIDLISTFD_SIZE;

        for (size_t i = 0; i < number_of_fds; i++)
          if ((res = cb (data, fd_info[i].proc_fd)) != 0)
            break;

        return res;
      }
  }
#endif

  for (fd = 0; fd < open_max; fd++)
      if ((res = cb (data, fd)) != 0)
          break;

  return res;
}

/**
 * g_fdwalk_set_cloexec:
 * @lowfd: Minimum fd to act on, which must be non-negative
 *
 * Mark every file descriptor equal to or greater than @lowfd to be closed
 * at the next `execve()` or similar, as if via the `FD_CLOEXEC` flag.
 *
 * Typically @lowfd will be 3, to leave standard input, standard output
 * and standard error open after exec.
 *
 * This is the same as Linux `close_range (lowfd, ~0U, CLOSE_RANGE_CLOEXEC)`,
 * but portable to other OSs and to older versions of Linux.
 *
 * This function is async-signal safe, making it safe to call from a
 * signal handler or a [callback@GLib.SpawnChildSetupFunc], as long as @lowfd is
 * non-negative.
 * See [`signal(7)`](man:signal(7)) and
 * [`signal-safety(7)`](man:signal-safety(7)) for more details.
 *
 * Returns: 0 on success, -1 with errno set on error
 * Since: 2.80
 */
int
g_fdwalk_set_cloexec (int lowfd)
{
  int ret;

  g_return_val_if_fail (lowfd >= 0, (errno = EINVAL, -1));

#if defined(HAVE_CLOSE_RANGE) && defined(CLOSE_RANGE_CLOEXEC)
  /* close_range() is available in Linux since kernel 5.9, and on FreeBSD at
   * around the same time. It was designed for use in async-signal-safe
   * situations: https://bugs.python.org/issue38061
   *
   * The `CLOSE_RANGE_CLOEXEC` flag was added in Linux 5.11, and is not yet
   * present in FreeBSD.
   *
   * Handle ENOSYS in case it’s supported in libc but not the kernel; if so,
   * fall back to safe_fdwalk(). Handle EINVAL in case `CLOSE_RANGE_CLOEXEC`
   * is not supported. */
  ret = close_range (lowfd, G_MAXUINT, CLOSE_RANGE_CLOEXEC);
  if (ret == 0 || !(errno == ENOSYS || errno == EINVAL))
    return ret;
#endif  /* HAVE_CLOSE_RANGE */

  ret = safe_fdwalk (set_cloexec, GINT_TO_POINTER (lowfd));

  return ret;
}

/**
 * g_closefrom:
 * @lowfd: Minimum fd to close, which must be non-negative
 *
 * Close every file descriptor equal to or greater than @lowfd.
 *
 * Typically @lowfd will be 3, to leave standard input, standard output
 * and standard error open.
 *
 * This is the same as Linux `close_range (lowfd, ~0U, 0)`,
 * but portable to other OSs and to older versions of Linux.
 * Equivalently, it is the same as BSD `closefrom (lowfd)`, but portable,
 * and async-signal-safe on all OSs.
 *
 * This function is async-signal safe, making it safe to call from a
 * signal handler or a [callback@GLib.SpawnChildSetupFunc], as long as @lowfd is
 * non-negative.
 * See [`signal(7)`](man:signal(7)) and
 * [`signal-safety(7)`](man:signal-safety(7)) for more details.
 *
 * Returns: 0 on success, -1 with errno set on error
 * Since: 2.80
 */
int
g_closefrom (int lowfd)
{
  int ret;

  g_return_val_if_fail (lowfd >= 0, (errno = EINVAL, -1));

#if defined(HAVE_CLOSE_RANGE)
  /* close_range() is available in Linux since kernel 5.9, and on FreeBSD at
   * around the same time. It was designed for use in async-signal-safe
   * situations: https://bugs.python.org/issue38061
   *
   * Handle ENOSYS in case it’s supported in libc but not the kernel; if so,
   * fall back to safe_fdwalk(). */
  ret = close_range (lowfd, G_MAXUINT, 0);
  if (ret == 0 || errno != ENOSYS)
    return ret;
#endif  /* HAVE_CLOSE_RANGE */

#if defined(__FreeBSD__) || defined(__OpenBSD__) || \
  (defined(__sun__) && defined(F_CLOSEFROM))
  /* Use closefrom function provided by the system if it is known to be
   * async-signal safe.
   *
   * FreeBSD: closefrom is included in the list of async-signal safe functions
   * found in https://man.freebsd.org/sigaction(2).
   *
   * OpenBSD: closefrom is not included in the list, but a direct system call
   * should be safe to use.
   *
   * In Solaris as of 11.3 SRU 31, closefrom() is also a direct system call.
   * On such systems, F_CLOSEFROM is defined.
   */
  (void) closefrom (lowfd);
  return 0;
#elif defined(__DragonFly__)
  /* It is unclear whether closefrom function included in DragonFlyBSD libc_r
   * is safe to use because it calls a lot of library functions. It is also
   * unclear whether libc_r itself is still being used. Therefore, we do a
   * direct system call here ourselves to avoid possible issues.
   */
  (void) syscall (SYS_closefrom, lowfd);
  return 0;
#elif defined(F_CLOSEM)
  /* NetBSD and AIX have a special fcntl command which does the same thing as
   * closefrom. NetBSD also includes closefrom function, which seems to be a
   * simple wrapper of the fcntl command.
   */
  return fcntl (lowfd, F_CLOSEM);
#else
  ret = safe_fdwalk (close_func_with_invalid_fds, GINT_TO_POINTER (lowfd));

  return ret;
#endif
}
