/* gspawn.c - Process launching
 *
 *  Copyright 2000 Red Hat, Inc.
 *  g_execvpe implementation based on GNU libc execvp:
 *   Copyright 1991, 92, 95, 96, 97, 98, 99 Free Software Foundation, Inc.
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

#include "config.h"

#include <errno.h>

#include "glibintl.h"
#include "gspawn.h"

/* Platform-specific implementation functions. */
gboolean g_spawn_sync_impl (const gchar           *working_directory,
                            gchar                **argv,
                            gchar                **envp,
                            GSpawnFlags            flags,
                            GSpawnChildSetupFunc   child_setup,
                            gpointer               user_data,
                            gchar                **standard_output,
                            gchar                **standard_error,
                            gint                  *wait_status,
                            GError               **error);
gboolean g_spawn_async_with_pipes_and_fds_impl (const gchar           *working_directory,
                                                const gchar * const   *argv,
                                                const gchar * const   *envp,
                                                GSpawnFlags            flags,
                                                GSpawnChildSetupFunc   child_setup,
                                                gpointer               user_data,
                                                gint                   stdin_fd,
                                                gint                   stdout_fd,
                                                gint                   stderr_fd,
                                                const gint            *source_fds,
                                                const gint            *target_fds,
                                                gsize                  n_fds,
                                                GPid                  *child_pid_out,
                                                gint                  *stdin_pipe_out,
                                                gint                  *stdout_pipe_out,
                                                gint                  *stderr_pipe_out,
                                                GError               **error);
gboolean g_spawn_check_wait_status_impl (gint     wait_status,
                                         GError **error);
void g_spawn_close_pid_impl (GPid pid);

static inline gint
_g_spawn_exec_err_to_g_error (gint en)
{
  switch (en)
    {
#ifdef EACCES
    case EACCES:
      return G_SPAWN_ERROR_ACCES;
#endif

#ifdef EPERM
    case EPERM:
      return G_SPAWN_ERROR_PERM;
#endif

#ifdef E2BIG
    case E2BIG:
      return G_SPAWN_ERROR_TOO_BIG;
#endif

#ifdef ENOEXEC
    case ENOEXEC:
      return G_SPAWN_ERROR_NOEXEC;
#endif

#ifdef ENAMETOOLONG
    case ENAMETOOLONG:
      return G_SPAWN_ERROR_NAMETOOLONG;
#endif

#ifdef ENOENT
    case ENOENT:
      return G_SPAWN_ERROR_NOENT;
#endif

#ifdef ENOMEM
    case ENOMEM:
      return G_SPAWN_ERROR_NOMEM;
#endif

#ifdef ENOTDIR
    case ENOTDIR:
      return G_SPAWN_ERROR_NOTDIR;
#endif

#ifdef ELOOP
    case ELOOP:
      return G_SPAWN_ERROR_LOOP;
#endif

#ifdef ETXTBUSY
    case ETXTBUSY:
      return G_SPAWN_ERROR_TXTBUSY;
#endif

#ifdef EIO
    case EIO:
      return G_SPAWN_ERROR_IO;
#endif

#ifdef ENFILE
    case ENFILE:
      return G_SPAWN_ERROR_NFILE;
#endif

#ifdef EMFILE
    case EMFILE:
      return G_SPAWN_ERROR_MFILE;
#endif

#ifdef EINVAL
    case EINVAL:
      return G_SPAWN_ERROR_INVAL;
#endif

#ifdef EISDIR
    case EISDIR:
      return G_SPAWN_ERROR_ISDIR;
#endif

#ifdef ELIBBAD
    case ELIBBAD:
      return G_SPAWN_ERROR_LIBBAD;
#endif

    default:
      return G_SPAWN_ERROR_FAILED;
    }
}

static inline gboolean
_g_spawn_invalid_source_fd (gint         fd,
                            const gint  *source_fds,
                            gsize        n_fds,
                            GError     **error)
{
  gsize i;

  for (i = 0; i < n_fds; i++)
    if (fd == source_fds[i])
      {
        g_set_error (error,
                     G_SPAWN_ERROR,
                     G_SPAWN_ERROR_INVAL,
                     _("Invalid source FDs argument"));
        return TRUE;
      }

  return FALSE;
}
