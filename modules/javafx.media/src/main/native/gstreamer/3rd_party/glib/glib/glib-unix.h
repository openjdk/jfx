/* glib-unix.h - Unix specific integration
 * Copyright (C) 2011 Red Hat, Inc.
 * Copyright 2023 Collabora Ltd.
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

#ifndef __G_UNIX_H__
#define __G_UNIX_H__

/* We need to include the UNIX headers needed to use the APIs below,
 * but we also take this opportunity to include a wide selection of
 * other UNIX headers.  If one of the headers below is broken on some
 * system, work around it here (or better, fix the system or tell
 * people to use a better one).
 */
#include <unistd.h>
#include <errno.h>
#include <sys/wait.h>
#include <stdlib.h>
#include <fcntl.h>

#include <glib.h>
#include <glib/gstdio.h>

#ifndef G_OS_UNIX
#error "This header may only be used on UNIX"
#endif

G_BEGIN_DECLS

/**
 * G_UNIX_ERROR:
 *
 * Error domain for API in the g_unix_ namespace. Note that there is no
 * exported enumeration mapping %errno. Instead, all functions ensure that
 * %errno is relevant. The code for all %G_UNIX_ERROR is always 0, and the
 * error message is always generated via g_strerror().
 *
 * It is expected that most code will not look at %errno from these APIs.
 * Important cases where one would want to differentiate between errors are
 * already covered by existing cross-platform GLib API, such as e.g. #GFile
 * wrapping `ENOENT`. However, it is provided for completeness, at least.
 */
#define G_UNIX_ERROR (g_unix_error_quark())

GLIB_AVAILABLE_IN_2_30
GQuark g_unix_error_quark (void);

GLIB_AVAILABLE_IN_2_30
gboolean g_unix_open_pipe (gint    *fds,
                           gint     flags,
                           GError **error);

GLIB_AVAILABLE_IN_2_30
gboolean g_unix_set_fd_nonblocking (gint       fd,
                                    gboolean   nonblock,
                                    GError   **error);

GLIB_AVAILABLE_IN_2_30
GSource *g_unix_signal_source_new  (gint signum);

GLIB_AVAILABLE_IN_2_30
guint    g_unix_signal_add_full    (gint           priority,
                                    gint           signum,
                                    GSourceFunc    handler,
                                    gpointer       user_data,
                                    GDestroyNotify notify);

GLIB_AVAILABLE_IN_2_30
guint    g_unix_signal_add         (gint        signum,
                                    GSourceFunc handler,
                                    gpointer    user_data);

/**
 * GUnixFDSourceFunc:
 * @fd: the fd that triggered the event
 * @condition: the IO conditions reported on @fd
 * @user_data: user data passed to g_unix_fd_add()
 *
 * The type of functions to be called when a UNIX fd watch source
 * triggers.
 *
 * Returns: %FALSE if the source should be removed
 **/
typedef gboolean (*GUnixFDSourceFunc) (gint         fd,
                                       GIOCondition condition,
                                       gpointer     user_data);

GLIB_AVAILABLE_IN_2_36
GSource *g_unix_fd_source_new      (gint         fd,
                                    GIOCondition condition);

GLIB_AVAILABLE_IN_2_36
guint    g_unix_fd_add_full        (gint              priority,
                                    gint              fd,
                                    GIOCondition      condition,
                                    GUnixFDSourceFunc function,
                                    gpointer          user_data,
                                    GDestroyNotify    notify);

GLIB_AVAILABLE_IN_2_36
guint    g_unix_fd_add             (gint              fd,
                                    GIOCondition      condition,
                                    GUnixFDSourceFunc function,
                                    gpointer          user_data);

GLIB_AVAILABLE_IN_2_64
struct passwd *g_unix_get_passwd_entry (const gchar  *user_name,
                                        GError      **error);

/**
 * GUnixPipe:
 * @fds: A pair of file descriptors, each negative if closed or not yet opened.
 *  The file descriptor with index %G_UNIX_PIPE_END_READ is readable.
 *  The file descriptor with index %G_UNIX_PIPE_END_WRITE is writable.
 *
 * A Unix pipe. The advantage of this type over `int[2]` is that it can
 * be closed automatically when it goes out of scope, using `g_auto(GUnixPipe)`,
 * on compilers that support that feature.
 *
 * Since: 2.80
 */
GLIB_AVAILABLE_TYPE_IN_2_80
typedef struct {
  int fds[2];
} GUnixPipe;

/**
 * GUnixPipeEnd:
 * @G_UNIX_PIPE_END_READ: The readable file descriptor 0
 * @G_UNIX_PIPE_END_WRITE: The writable file descriptor 1
 *
 * Mnemonic constants for the ends of a Unix pipe.
 *
 * Since: 2.80
 */
GLIB_AVAILABLE_TYPE_IN_2_80
typedef enum
{
  G_UNIX_PIPE_END_READ = 0,
  G_UNIX_PIPE_END_WRITE = 1
} GUnixPipeEnd;

/**
 * G_UNIX_PIPE_INIT:
 *
 * Initializer for a #GUnixPipe that has not yet been opened.
 * Both of its file descriptors are initialized to `-1` (invalid),
 * the same as if they had been closed.
 *
 * Since: 2.80
 */
#define G_UNIX_PIPE_INIT { { -1, -1 } } GLIB_AVAILABLE_MACRO_IN_2_80

/* Suppress "Not available before" warnings when declaring the
 * implementations */
G_GNUC_BEGIN_IGNORE_DEPRECATIONS

/**
 * g_unix_pipe_open:
 * @self: A pair of file descriptors
 * @flags: Flags to pass to g_unix_open_pipe(), typically `O_CLOEXEC`
 * @error: Used to report an error on failure
 *
 * Open a pipe. This is the same as g_unix_open_pipe(), but uses the
 * #GUnixPipe data structure.
 *
 * Returns: %TRUE on success
 *
 * Since: 2.80
 */
GLIB_AVAILABLE_STATIC_INLINE_IN_2_80
static inline gboolean g_unix_pipe_open (GUnixPipe  *self,
                                         int         flags,
                                         GError    **error);

GLIB_AVAILABLE_STATIC_INLINE_IN_2_80
static inline gboolean
g_unix_pipe_open (GUnixPipe *self,
                  int flags,
                  GError **error)
{
  return g_unix_open_pipe (self->fds, flags, error);
}

/**
 * g_unix_pipe_get:
 * @self: A pair of file descriptors
 * @end: One of the ends of the pipe
 *
 * Return one of the ends of the pipe. It remains owned by @self.
 *
 * This function is async-signal safe (see [`signal(7)`](man:signal(7)) and
 * [`signal-safety(7)`](man:signal-safety(7))), making it safe to call from a
 * signal handler or a #GSpawnChildSetupFunc.
 *
 * This function preserves the value of `errno`.
 *
 * Returns: a non-negative file descriptor owned by @self, which must not
 *  be closed by the caller, or a negative number if the corresponding
 *  end of the pipe was already closed or stolen
 *
 * Since: 2.80
 */
GLIB_AVAILABLE_STATIC_INLINE_IN_2_80
static inline int g_unix_pipe_get (GUnixPipe    *self,
                                   GUnixPipeEnd  end);

GLIB_AVAILABLE_STATIC_INLINE_IN_2_80
static inline int
g_unix_pipe_get (GUnixPipe *self,
                 GUnixPipeEnd end)
{
  return self->fds[end];
}

/**
 * g_unix_pipe_steal:
 * @self: A pair of file descriptors
 * @end: One of the ends of the pipe
 *
 * Return one of the ends of the pipe. It becomes owned by the caller,
 * and the file descriptor in the data structure is set to `-1`,
 * similar to g_steal_fd().
 *
 * This function is async-signal safe (see [`signal(7)`](man:signal(7)) and
 * [`signal-safety(7)`](man:signal-safety(7))), making it safe to call from a
 * signal handler or a #GSpawnChildSetupFunc.
 *
 * This function preserves the value of `errno`.
 *
 * Returns: a non-negative file descriptor, which becomes owned by the
 *  caller and must be closed by the caller if required, or a negative
 *  number if the corresponding end of the pipe was already closed or stolen
 *
 * Since: 2.80
 */
GLIB_AVAILABLE_STATIC_INLINE_IN_2_80
static inline int g_unix_pipe_steal (GUnixPipe    *self,
                                     GUnixPipeEnd  end);

GLIB_AVAILABLE_STATIC_INLINE_IN_2_80
static inline int
g_unix_pipe_steal (GUnixPipe *self,
                   GUnixPipeEnd end)
{
  return g_steal_fd (&self->fds[end]);
}

/**
 * g_unix_pipe_close:
 * @self: A pair of file descriptors
 * @end: One of the ends of the pipe
 * @error: Optionally used to report an error on failure
 *
 * Close one of the ends of the pipe and set the relevant member of @fds
 * to `-1` before returning, equivalent to g_clear_fd().
 *
 * Like g_close(), if closing the file descriptor fails, the error is
 * stored in both %errno and @error. If this function succeeds,
 * %errno is undefined.
 *
 * This function is async-signal safe if @error is %NULL and the relevant
 * member of @fds is either negative or a valid open file descriptor.
 * This makes it safe to call from a signal handler or a #GSpawnChildSetupFunc
 * under those conditions.
 * See [`signal(7)`](man:signal(7)) and
 * [`signal-safety(7)`](man:signal-safety(7)) for more details.
 *
 * To close both file descriptors and ignore any errors, use
 * g_unix_pipe_clear() instead.
 *
 * Returns: %TRUE on success
 *
 * Since: 2.80
 */
GLIB_AVAILABLE_STATIC_INLINE_IN_2_80
static inline gboolean g_unix_pipe_close (GUnixPipe     *self,
                                          GUnixPipeEnd   end,
                                          GError       **error);

GLIB_AVAILABLE_STATIC_INLINE_IN_2_80
static inline gboolean
g_unix_pipe_close (GUnixPipe *self,
                   GUnixPipeEnd end,
                   GError **error)
{
  return g_clear_fd (&self->fds[end], error);
}

/**
 * g_unix_pipe_clear:
 * @self: a #GUnixPipe
 *
 * Close both ends of the pipe, unless they have already been closed or
 * stolen. Any errors are ignored: use g_unix_pipe_close() or g_clear_fd()
 * if error-handling is required.
 *
 * This function is async-signal safe if @error is %NULL and each member
 * of @fds are either negative or a valid open file descriptor.
 * As a result, it is safe to call this function or use `g_auto(GUnixPipe)`
 * (on compilers that support it) in a signal handler or a
 * #GSpawnChildSetupFunc, as long as those conditions are ensured to be true.
 * See [`signal(7)`](man:signal(7)) and
 * [`signal-safety(7)`](man:signal-safety(7)) for more details.
 *
 * This function preserves the value of `errno`.
 *
 * Since: 2.80
 */
GLIB_AVAILABLE_STATIC_INLINE_IN_2_80
static inline void g_unix_pipe_clear (GUnixPipe *self);

GLIB_AVAILABLE_STATIC_INLINE_IN_2_80
static inline void
g_unix_pipe_clear (GUnixPipe *self)
{
  /* Don't overwrite thread-local errno if closing the fd fails */
  int errsv = errno;

  if (!g_unix_pipe_close (self, G_UNIX_PIPE_END_READ, NULL))
    {
      /* ignore */
    }

  if (!g_unix_pipe_close (self, G_UNIX_PIPE_END_WRITE, NULL))
    {
      /* ignore */
    }

  errno = errsv;
}

G_DEFINE_AUTO_CLEANUP_CLEAR_FUNC (GUnixPipe, g_unix_pipe_clear)

GLIB_AVAILABLE_IN_2_80
int g_closefrom (int lowfd);

GLIB_AVAILABLE_IN_2_80
int g_fdwalk_set_cloexec (int lowfd);

G_GNUC_END_IGNORE_DEPRECATIONS

G_END_DECLS

#endif  /* __G_UNIX_H__ */
