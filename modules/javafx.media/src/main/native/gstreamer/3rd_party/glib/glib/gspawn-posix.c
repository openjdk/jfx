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

#include <sys/time.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>
#include <errno.h>
#include <fcntl.h>
#include <signal.h>
#include <string.h>
#include <stdlib.h>   /* for fdwalk */
#include <dirent.h>
#include <unistd.h>

#ifdef HAVE_SPAWN_H
#include <spawn.h>
#endif /* HAVE_SPAWN_H */

#ifdef HAVE_CRT_EXTERNS_H
#include <crt_externs.h> /* for _NSGetEnviron */
#endif

#ifdef HAVE_SYS_SELECT_H
#include <sys/select.h>
#endif /* HAVE_SYS_SELECT_H */

#ifdef HAVE_SYS_RESOURCE_H
#include <sys/resource.h>
#endif /* HAVE_SYS_RESOURCE_H */

#if defined(__linux__) || defined(__DragonFly__)
#include <sys/syscall.h>  /* for syscall and SYS_getdents64 */
#endif

#include "gspawn.h"
#include "gspawn-private.h"
#include "gthread.h"
#include "gtrace-private.h"
#include "glib/gstdio.h"

#include "genviron.h"
#include "gmem.h"
#include "gshell.h"
#include "gstring.h"
#include "gstrfuncs.h"
#include "gtestutils.h"
#include "gutils.h"
#include "glibintl.h"
#include "glib-unix.h"

#if defined(__APPLE__) && defined(HAVE_LIBPROC_H)
#include <libproc.h>
#include <sys/proc_info.h>
#endif

#define INHERITS_OR_NULL_STDIN  (G_SPAWN_STDIN_FROM_DEV_NULL | G_SPAWN_CHILD_INHERITS_STDIN)
#define INHERITS_OR_NULL_STDOUT (G_SPAWN_STDOUT_TO_DEV_NULL | G_SPAWN_CHILD_INHERITS_STDOUT)
#define INHERITS_OR_NULL_STDERR (G_SPAWN_STDERR_TO_DEV_NULL | G_SPAWN_CHILD_INHERITS_STDERR)

#define IS_STD_FILENO(_fd) ((_fd >= STDIN_FILENO) && (_fd <= STDERR_FILENO))
#define IS_VALID_FILENO(_fd) (_fd >= 0)

/* posix_spawn() is assumed the fastest way to spawn, but glibc's
 * implementation was buggy before glibc 2.24, so avoid it on old versions.
 */
#ifdef HAVE_POSIX_SPAWN
#ifdef __GLIBC__

#if __GLIBC_PREREQ(2,24)
#define POSIX_SPAWN_AVAILABLE
#endif

#else /* !__GLIBC__ */
/* Assume that all non-glibc posix_spawn implementations are fine. */
#define POSIX_SPAWN_AVAILABLE
#endif /* __GLIBC__ */
#endif /* HAVE_POSIX_SPAWN */

#ifdef HAVE__NSGETENVIRON
#define environ (*_NSGetEnviron())
#else
extern char **environ;
#endif

#ifndef O_CLOEXEC
#define O_CLOEXEC 0
#else
#define HAVE_O_CLOEXEC 1
#endif

static gint g_execute (const gchar  *file,
                       gchar       **argv,
                       gchar       **argv_buffer,
                       gsize         argv_buffer_len,
                       gchar       **envp,
                       const gchar  *search_path,
                       gchar        *search_path_buffer,
                       gsize         search_path_buffer_len);

static gboolean fork_exec (gboolean              intermediate_child,
                           const gchar          *working_directory,
                           const gchar * const  *argv,
                           const gchar * const  *envp,
                           gboolean              close_descriptors,
                           gboolean              search_path,
                           gboolean              search_path_from_envp,
                           gboolean              stdout_to_null,
                           gboolean              stderr_to_null,
                           gboolean              child_inherits_stdin,
                           gboolean              file_and_argv_zero,
                           gboolean              cloexec_pipes,
                           GSpawnChildSetupFunc  child_setup,
                           gpointer              user_data,
                           GPid                 *child_pid,
                           gint                 *stdin_pipe_out,
                           gint                 *stdout_pipe_out,
                           gint                 *stderr_pipe_out,
                           gint                  stdin_fd,
                           gint                  stdout_fd,
                           gint                  stderr_fd,
                           const gint           *source_fds,
                           const gint           *target_fds,
                           gsize                 n_fds,
                           GError              **error);

G_DEFINE_QUARK (g-exec-error-quark, g_spawn_error)
G_DEFINE_QUARK (g-spawn-exit-error-quark, g_spawn_exit_error)

/* Some versions of OS X define READ_OK in public headers */
#undef READ_OK

typedef enum
{
  READ_FAILED = 0, /* FALSE */
  READ_OK,
  READ_EOF
} ReadResult;

static ReadResult
read_data (GString *str,
           gint     fd,
           GError **error)
{
  gssize bytes;
  gchar buf[4096];

 again:
  bytes = read (fd, buf, 4096);

  if (bytes == 0)
    return READ_EOF;
  else if (bytes > 0)
    {
      g_string_append_len (str, buf, bytes);
      return READ_OK;
    }
  else if (errno == EINTR)
    goto again;
  else
    {
      int errsv = errno;

      g_set_error (error,
                   G_SPAWN_ERROR,
                   G_SPAWN_ERROR_READ,
                   _("Failed to read data from child process (%s)"),
                   g_strerror (errsv));

      return READ_FAILED;
    }
}

gboolean
g_spawn_sync_impl (const gchar           *working_directory,
                   gchar                **argv,
                   gchar                **envp,
                   GSpawnFlags            flags,
                   GSpawnChildSetupFunc   child_setup,
                   gpointer               user_data,
                   gchar                **standard_output,
                   gchar                **standard_error,
                   gint                  *wait_status,
                   GError               **error)
{
  gint outpipe = -1;
  gint errpipe = -1;
  GPid pid;
  gint ret;
  GString *outstr = NULL;
  GString *errstr = NULL;
  gboolean failed;
  gint status;

  g_return_val_if_fail (argv != NULL, FALSE);
  g_return_val_if_fail (argv[0] != NULL, FALSE);
  g_return_val_if_fail (!(flags & G_SPAWN_DO_NOT_REAP_CHILD), FALSE);
  g_return_val_if_fail (standard_output == NULL ||
                        !(flags & G_SPAWN_STDOUT_TO_DEV_NULL), FALSE);
  g_return_val_if_fail (standard_error == NULL ||
                        !(flags & G_SPAWN_STDERR_TO_DEV_NULL), FALSE);

  /* Just to ensure segfaults if callers try to use
   * these when an error is reported.
   */
  if (standard_output)
    *standard_output = NULL;

  if (standard_error)
    *standard_error = NULL;

  if (!fork_exec (FALSE,
                  working_directory,
                  (const gchar * const *) argv,
                  (const gchar * const *) envp,
                  !(flags & G_SPAWN_LEAVE_DESCRIPTORS_OPEN),
                  (flags & G_SPAWN_SEARCH_PATH) != 0,
                  (flags & G_SPAWN_SEARCH_PATH_FROM_ENVP) != 0,
                  (flags & G_SPAWN_STDOUT_TO_DEV_NULL) != 0,
                  (flags & G_SPAWN_STDERR_TO_DEV_NULL) != 0,
                  (flags & G_SPAWN_CHILD_INHERITS_STDIN) != 0,
                  (flags & G_SPAWN_FILE_AND_ARGV_ZERO) != 0,
                  (flags & G_SPAWN_CLOEXEC_PIPES) != 0,
                  child_setup,
                  user_data,
                  &pid,
                  NULL,
                  standard_output ? &outpipe : NULL,
                  standard_error ? &errpipe : NULL,
                  -1, -1, -1,
                  NULL, NULL, 0,
                  error))
    return FALSE;

  /* Read data from child. */

  failed = FALSE;

  if (outpipe >= 0)
    {
      outstr = g_string_new (NULL);
    }

  if (errpipe >= 0)
    {
      errstr = g_string_new (NULL);
    }

  /* Read data until we get EOF on both pipes. */
  while (!failed &&
         (outpipe >= 0 ||
          errpipe >= 0))
    {
      /* Any negative FD in the array is ignored, so we can use a fixed length.
       * We can use UNIX FDs here without worrying about Windows HANDLEs because
       * the Windows implementation is entirely in gspawn-win32.c. */
      GPollFD fds[] =
        {
          { outpipe, G_IO_IN | G_IO_HUP | G_IO_ERR, 0 },
          { errpipe, G_IO_IN | G_IO_HUP | G_IO_ERR, 0 },
        };

      ret = g_poll (fds, G_N_ELEMENTS (fds), -1  /* no timeout */);

      if (ret < 0)
        {
          int errsv = errno;

    if (errno == EINTR)
      continue;

          failed = TRUE;

          g_set_error (error,
                       G_SPAWN_ERROR,
                       G_SPAWN_ERROR_READ,
                       _("Unexpected error in reading data from a child process (%s)"),
                       g_strerror (errsv));

          break;
        }

      if (outpipe >= 0 && fds[0].revents != 0)
        {
          switch (read_data (outstr, outpipe, error))
            {
            case READ_FAILED:
              failed = TRUE;
              break;
            case READ_EOF:
              g_clear_fd (&outpipe, NULL);
              break;
            default:
              break;
            }

          if (failed)
            break;
        }

      if (errpipe >= 0 && fds[1].revents != 0)
        {
          switch (read_data (errstr, errpipe, error))
            {
            case READ_FAILED:
              failed = TRUE;
              break;
            case READ_EOF:
              g_clear_fd (&errpipe, NULL);
              break;
            default:
              break;
            }

          if (failed)
            break;
        }
    }

  /* These should only be open still if we had an error.  */
  g_clear_fd (&outpipe, NULL);
  g_clear_fd (&errpipe, NULL);

  /* Wait for child to exit, even if we have
   * an error pending.
   */
 again:

  ret = waitpid (pid, &status, 0);

  if (ret < 0)
    {
      if (errno == EINTR)
        goto again;
      else if (errno == ECHILD)
        {
          if (wait_status)
            {
              g_warning ("In call to g_spawn_sync(), wait status of a child process was requested but ECHILD was received by waitpid(). See the documentation of g_child_watch_source_new() for possible causes.");
            }
          else
            {
              /* We don't need the wait status. */
            }
        }
      else
        {
          if (!failed) /* avoid error pileups */
            {
              int errsv = errno;

              failed = TRUE;

              g_set_error (error,
                           G_SPAWN_ERROR,
                           G_SPAWN_ERROR_READ,
                           _("Unexpected error in waitpid() (%s)"),
                           g_strerror (errsv));
            }
        }
    }

  if (failed)
    {
      if (outstr)
        g_string_free (outstr, TRUE);
      if (errstr)
        g_string_free (errstr, TRUE);

      return FALSE;
    }
  else
    {
      if (wait_status)
        *wait_status = status;

      if (standard_output)
        *standard_output = g_string_free (outstr, FALSE);

      if (standard_error)
        *standard_error = g_string_free (errstr, FALSE);

      return TRUE;
    }
}

gboolean
g_spawn_async_with_pipes_and_fds_impl (const gchar           *working_directory,
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
                                       GError               **error)
{
  g_return_val_if_fail (argv != NULL, FALSE);
  g_return_val_if_fail (argv[0] != NULL, FALSE);
  /* can’t both inherit and set pipes to /dev/null */
  g_return_val_if_fail ((flags & INHERITS_OR_NULL_STDIN) != INHERITS_OR_NULL_STDIN, FALSE);
  g_return_val_if_fail ((flags & INHERITS_OR_NULL_STDOUT) != INHERITS_OR_NULL_STDOUT, FALSE);
  g_return_val_if_fail ((flags & INHERITS_OR_NULL_STDERR) != INHERITS_OR_NULL_STDERR, FALSE);
  /* can’t use pipes and stdin/stdout/stderr FDs */
  g_return_val_if_fail (stdin_pipe_out == NULL || stdin_fd < 0, FALSE);
  g_return_val_if_fail (stdout_pipe_out == NULL || stdout_fd < 0, FALSE);
  g_return_val_if_fail (stderr_pipe_out == NULL || stderr_fd < 0, FALSE);

  if ((flags & INHERITS_OR_NULL_STDIN) != 0)
    stdin_pipe_out = NULL;
  if ((flags & INHERITS_OR_NULL_STDOUT) != 0)
    stdout_pipe_out = NULL;
  if ((flags & INHERITS_OR_NULL_STDERR) != 0)
    stderr_pipe_out = NULL;

  return fork_exec (!(flags & G_SPAWN_DO_NOT_REAP_CHILD),
                    working_directory,
                    (const gchar * const *) argv,
                    (const gchar * const *) envp,
                    !(flags & G_SPAWN_LEAVE_DESCRIPTORS_OPEN),
                    (flags & G_SPAWN_SEARCH_PATH) != 0,
                    (flags & G_SPAWN_SEARCH_PATH_FROM_ENVP) != 0,
                    (flags & G_SPAWN_STDOUT_TO_DEV_NULL) != 0,
                    (flags & G_SPAWN_STDERR_TO_DEV_NULL) != 0,
                    (flags & G_SPAWN_CHILD_INHERITS_STDIN) != 0,
                    (flags & G_SPAWN_FILE_AND_ARGV_ZERO) != 0,
                    (flags & G_SPAWN_CLOEXEC_PIPES) != 0,
                    child_setup,
                    user_data,
                    child_pid_out,
                    stdin_pipe_out,
                    stdout_pipe_out,
                    stderr_pipe_out,
                    stdin_fd,
                    stdout_fd,
                    stderr_fd,
                    source_fds,
                    target_fds,
                    n_fds,
                    error);
}

gboolean
g_spawn_check_wait_status_impl (gint     wait_status,
                                GError **error)
{
  gboolean ret = FALSE;

  if (WIFEXITED (wait_status))
    {
      if (WEXITSTATUS (wait_status) != 0)
  {
    g_set_error (error, G_SPAWN_EXIT_ERROR, WEXITSTATUS (wait_status),
           _("Child process exited with code %ld"),
           (long) WEXITSTATUS (wait_status));
    goto out;
  }
    }
  else if (WIFSIGNALED (wait_status))
    {
      g_set_error (error, G_SPAWN_ERROR, G_SPAWN_ERROR_FAILED,
       _("Child process killed by signal %ld"),
       (long) WTERMSIG (wait_status));
      goto out;
    }
  else if (WIFSTOPPED (wait_status))
    {
      g_set_error (error, G_SPAWN_ERROR, G_SPAWN_ERROR_FAILED,
       _("Child process stopped by signal %ld"),
       (long) WSTOPSIG (wait_status));
      goto out;
    }
  else
    {
      g_set_error (error, G_SPAWN_ERROR, G_SPAWN_ERROR_FAILED,
       _("Child process exited abnormally"));
      goto out;
    }

  ret = TRUE;
 out:
  return ret;
}

/* This function is called between fork() and exec() and hence must be
 * async-signal-safe (see signal-safety(7)). */
static gssize
write_all (gint fd, gconstpointer vbuf, gsize to_write)
{
  gchar *buf = (gchar *) vbuf;

  while (to_write > 0)
    {
      gssize count = write (fd, buf, to_write);
      if (count < 0)
        {
          if (errno != EINTR)
            return FALSE;
        }
      else
        {
          to_write -= count;
          buf += count;
        }
    }

  return TRUE;
}

/* This function is called between fork() and exec() and hence must be
 * async-signal-safe (see signal-safety(7)). */
G_NORETURN
static void
write_err_and_exit (gint fd, gint msg)
{
  gint en = errno;

  write_all (fd, &msg, sizeof(msg));
  write_all (fd, &en, sizeof(en));

  close (fd);

  _exit (1);
}

/* This function is called between fork() and exec() and hence must be
 * async-signal-safe (see signal-safety(7)). */
static void
set_cloexec (int fd)
{
  fcntl (fd, F_SETFD, FD_CLOEXEC);
}

/* This function is called between fork() and exec() and hence must be
 * async-signal-safe (see signal-safety(7)). */
static void
unset_cloexec (int fd)
{
  int flags;
  int result;

  flags = fcntl (fd, F_GETFD, 0);

  if (flags != -1)
    {
      int errsv;
      flags &= (~FD_CLOEXEC);
      do
        {
          result = fcntl (fd, F_SETFD, flags);
          errsv = errno;
        }
      while (result == -1 && errsv == EINTR);
    }
}

/* This function is called between fork() and exec() and hence must be
 * async-signal-safe (see signal-safety(7)). */
static int
dupfd_cloexec (int old_fd, int new_fd_min)
{
  int fd, errsv;
#ifdef F_DUPFD_CLOEXEC
  do
    {
      fd = fcntl (old_fd, F_DUPFD_CLOEXEC, new_fd_min);
      errsv = errno;
    }
  while (fd == -1 && errsv == EINTR);
#else
  /* OS X Snow Lion and earlier don't have F_DUPFD_CLOEXEC:
   * https://bugzilla.gnome.org/show_bug.cgi?id=710962
   */
  int result, flags;
  do
    {
      fd = fcntl (old_fd, F_DUPFD, new_fd_min);
      errsv = errno;
    }
  while (fd == -1 && errsv == EINTR);
  flags = fcntl (fd, F_GETFD, 0);
  if (flags != -1)
    {
      flags |= FD_CLOEXEC;
      do
        {
          result = fcntl (fd, F_SETFD, flags);
          errsv = errno;
        }
      while (result == -1 && errsv == EINTR);
    }
#endif
  return fd;
}

/* This function is called between fork() and exec() and hence must be
 * async-signal-safe (see signal-safety(7)). */
static gint
safe_dup2 (gint fd1, gint fd2)
{
  gint ret;

  do
    ret = dup2 (fd1, fd2);
  while (ret < 0 && (errno == EINTR || errno == EBUSY));

  return ret;
}

/* This function is called between fork() and exec() and hence must be
 * async-signal-safe (see signal-safety(7)). */
static gboolean
relocate_fd_out_of_standard_range (gint *fd)
{
  gint ret = -1;
  const int min_fileno = STDERR_FILENO + 1;

  do
    ret = fcntl (*fd, F_DUPFD, min_fileno);
  while (ret < 0 && errno == EINTR);

  /* Note we don't need to close the old fd, because the caller is expected
   * to close fds in the standard range itself.
   */
  if (ret >= min_fileno)
    {
      *fd = ret;
      return TRUE;
    }

  return FALSE;
}

/* This function is called between fork() and exec() and hence must be
 * async-signal-safe (see signal-safety(7)). */
static gint
safe_open (const char *path, gint mode)
{
  gint ret;

  do
    ret = open (path, mode);
  while (ret < 0 && errno == EINTR);

  return ret;
}

enum
{
  CHILD_CHDIR_FAILED,
  CHILD_EXEC_FAILED,
  CHILD_OPEN_FAILED,
  CHILD_DUPFD_FAILED,
  CHILD_FORK_FAILED,
  CHILD_CLOSE_FAILED,
};

/* This function is called between fork() and exec() and hence must be
 * async-signal-safe (see signal-safety(7)) until it calls exec().
 *
 * All callers must guarantee that @argv and @argv[0] are non-NULL. */
static void
do_exec (gint                  child_err_report_fd,
         gint                  stdin_fd,
         gint                  stdout_fd,
         gint                  stderr_fd,
         gint                 *source_fds,
         const gint           *target_fds,
         gsize                 n_fds,
         const gchar          *working_directory,
         const gchar * const  *argv,
         gchar               **argv_buffer,
         gsize                 argv_buffer_len,
         const gchar * const  *envp,
         gboolean              close_descriptors,
         const gchar          *search_path,
         gchar                *search_path_buffer,
         gsize                 search_path_buffer_len,
         gboolean              stdout_to_null,
         gboolean              stderr_to_null,
         gboolean              child_inherits_stdin,
         gboolean              file_and_argv_zero,
         GSpawnChildSetupFunc  child_setup,
         gpointer              user_data)
{
  gsize i;
  gint max_target_fd = 0;

  if (working_directory && chdir (working_directory) < 0)
    write_err_and_exit (child_err_report_fd,
                        CHILD_CHDIR_FAILED);

  /* It's possible the caller assigned stdin to an fd with a
   * file number that is supposed to be reserved for
   * stdout or stderr.
   *
   * If so, move it up out of the standard range, so it doesn't
   * cause a conflict.
   */
  if (IS_STD_FILENO (stdin_fd) && stdin_fd != STDIN_FILENO)
    {
      int old_fd = stdin_fd;

      if (!relocate_fd_out_of_standard_range (&stdin_fd))
        write_err_and_exit (child_err_report_fd, CHILD_DUPFD_FAILED);

      if (stdout_fd == old_fd)
        stdout_fd = stdin_fd;

      if (stderr_fd == old_fd)
        stderr_fd = stdin_fd;
    }

  /* Redirect pipes as required
   *
   * There are two cases where we don't need to do the redirection
   * 1. Where the associated file descriptor is cleared/invalid
   * 2. When the associated file descriptor is already given the
   * correct file number.
   */
  if (IS_VALID_FILENO (stdin_fd) && stdin_fd != STDIN_FILENO)
    {
      if (safe_dup2 (stdin_fd, 0) < 0)
        write_err_and_exit (child_err_report_fd,
                            CHILD_DUPFD_FAILED);

      set_cloexec (stdin_fd);
    }
  else if (!child_inherits_stdin)
    {
      /* Keep process from blocking on a read of stdin */
      gint read_null = safe_open ("/dev/null", O_RDONLY);
      if (read_null < 0)
        write_err_and_exit (child_err_report_fd,
                            CHILD_OPEN_FAILED);
      if (safe_dup2 (read_null, 0) < 0)
        write_err_and_exit (child_err_report_fd,
                            CHILD_DUPFD_FAILED);
      g_clear_fd (&read_null, NULL);
    }

  /* Like with stdin above, it's possible the caller assigned
   * stdout to an fd with a file number that's intruding on the
   * standard range.
   *
   * If so, move it out of the way, too.
   */
  if (IS_STD_FILENO (stdout_fd) && stdout_fd != STDOUT_FILENO)
    {
      int old_fd = stdout_fd;

      if (!relocate_fd_out_of_standard_range (&stdout_fd))
        write_err_and_exit (child_err_report_fd, CHILD_DUPFD_FAILED);

      if (stderr_fd == old_fd)
        stderr_fd = stdout_fd;
    }

  if (IS_VALID_FILENO (stdout_fd) && stdout_fd != STDOUT_FILENO)
    {
      if (safe_dup2 (stdout_fd, 1) < 0)
        write_err_and_exit (child_err_report_fd,
                            CHILD_DUPFD_FAILED);

      set_cloexec (stdout_fd);
    }
  else if (stdout_to_null)
    {
      gint write_null = safe_open ("/dev/null", O_WRONLY);
      if (write_null < 0)
        write_err_and_exit (child_err_report_fd,
                            CHILD_OPEN_FAILED);
      if (safe_dup2 (write_null, 1) < 0)
        write_err_and_exit (child_err_report_fd,
                            CHILD_DUPFD_FAILED);
      g_clear_fd (&write_null, NULL);
    }

  if (IS_STD_FILENO (stderr_fd) && stderr_fd != STDERR_FILENO)
    {
      if (!relocate_fd_out_of_standard_range (&stderr_fd))
        write_err_and_exit (child_err_report_fd, CHILD_DUPFD_FAILED);
    }

  /* Like with stdin/stdout above, it's possible the caller assigned
   * stderr to an fd with a file number that's intruding on the
   * standard range.
   *
   * Make sure it's out of the way, also.
   */
  if (IS_VALID_FILENO (stderr_fd) && stderr_fd != STDERR_FILENO)
    {
      if (safe_dup2 (stderr_fd, 2) < 0)
        write_err_and_exit (child_err_report_fd,
                            CHILD_DUPFD_FAILED);

      set_cloexec (stderr_fd);
    }
  else if (stderr_to_null)
    {
      gint write_null = safe_open ("/dev/null", O_WRONLY);
      if (write_null < 0)
        write_err_and_exit (child_err_report_fd,
                            CHILD_OPEN_FAILED);
      if (safe_dup2 (write_null, 2) < 0)
        write_err_and_exit (child_err_report_fd,
                            CHILD_DUPFD_FAILED);
      g_clear_fd (&write_null, NULL);
    }

  /* Close all file descriptors but stdin, stdout and stderr, and any of source_fds,
   * before we exec. Note that this includes
   * child_err_report_fd, which keeps the parent from blocking
   * forever on the other end of that pipe.
   */
  if (close_descriptors)
    {
      if (child_setup == NULL && n_fds == 0)
        {
          if (safe_dup2 (child_err_report_fd, 3) < 0)
            write_err_and_exit (child_err_report_fd, CHILD_DUPFD_FAILED);
          set_cloexec (3);
          if (g_closefrom (4) < 0)
            write_err_and_exit (child_err_report_fd, CHILD_CLOSE_FAILED);
          child_err_report_fd = 3;
        }
      else
        {
          if (g_fdwalk_set_cloexec (3) < 0)
            write_err_and_exit (child_err_report_fd, CHILD_CLOSE_FAILED);
        }
    }
  else
    {
      /* We need to do child_err_report_fd anyway */
      set_cloexec (child_err_report_fd);
    }

  /*
   * Work through the @source_fds and @target_fds mapping.
   *
   * Based on code originally derived from
   * gnome-terminal:src/terminal-screen.c:terminal_screen_child_setup(),
   * used under the LGPLv2+ with permission from author. (The code has
   * since migrated to vte:src/spawn.cc:SpawnContext::exec and is no longer
   * terribly similar to what we have here.)
   */

  if (n_fds > 0)
    {
      for (i = 0; i < n_fds; i++)
        max_target_fd = MAX (max_target_fd, target_fds[i]);

      if (max_target_fd == G_MAXINT)
        {
          errno = EINVAL;
          write_err_and_exit (child_err_report_fd, CHILD_DUPFD_FAILED);
        }

      /* If we're doing remapping fd assignments, we need to handle
       * the case where the user has specified e.g. 5 -> 4, 4 -> 6.
       * We do this by duping all source fds, taking care to ensure the new
       * fds are larger than any target fd to avoid introducing new conflicts.
       */
      for (i = 0; i < n_fds; i++)
        {
          if (source_fds[i] != target_fds[i])
            {
              source_fds[i] = dupfd_cloexec (source_fds[i], max_target_fd + 1);
              if (source_fds[i] < 0)
                write_err_and_exit (child_err_report_fd, CHILD_DUPFD_FAILED);
            }
        }

      for (i = 0; i < n_fds; i++)
        {
          /* For basic fd assignments (where source == target), we can just
           * unset FD_CLOEXEC.
           */
          if (source_fds[i] == target_fds[i])
            {
              unset_cloexec (source_fds[i]);
            }
          else
            {
              /* If any of the @target_fds conflict with @child_err_report_fd,
               * dup it so it doesn’t get conflated.
               */
              if (target_fds[i] == child_err_report_fd)
                {
                  child_err_report_fd = dupfd_cloexec (child_err_report_fd, max_target_fd + 1);
                  if (child_err_report_fd < 0)
                    write_err_and_exit (child_err_report_fd, CHILD_DUPFD_FAILED);
                }

              if (safe_dup2 (source_fds[i], target_fds[i]) < 0)
                write_err_and_exit (child_err_report_fd, CHILD_DUPFD_FAILED);

              g_clear_fd (&source_fds[i], NULL);
            }
        }
    }

  /* Call user function just before we exec */
  if (child_setup)
    {
      (* child_setup) (user_data);
    }

  g_execute (argv[0],
             (gchar **) (file_and_argv_zero ? argv + 1 : argv),
             argv_buffer, argv_buffer_len,
             (gchar **) envp, search_path, search_path_buffer, search_path_buffer_len);

  /* Exec failed */
  write_err_and_exit (child_err_report_fd,
                      CHILD_EXEC_FAILED);
}

static gboolean
read_ints (int      fd,
           gint*    buf,
           gint     n_ints_in_buf,
           gint    *n_ints_read,
           GError **error)
{
  gsize bytes = 0;

  while (TRUE)
    {
      gssize chunk;

      if (bytes >= sizeof(gint)*2)
        break; /* give up, who knows what happened, should not be
                * possible.
                */

    again:
      chunk = read (fd,
                    ((gchar*)buf) + bytes,
                    sizeof(gint) * n_ints_in_buf - bytes);
      if (chunk < 0 && errno == EINTR)
        goto again;

      if (chunk < 0)
        {
          int errsv = errno;

          /* Some weird shit happened, bail out */
          g_set_error (error,
                       G_SPAWN_ERROR,
                       G_SPAWN_ERROR_FAILED,
                       _("Failed to read from child pipe (%s)"),
                       g_strerror (errsv));

          return FALSE;
        }
      else if (chunk == 0)
        break; /* EOF */
      else /* chunk > 0 */
  bytes += chunk;
    }

  *n_ints_read = (gint)(bytes / sizeof(gint));

  return TRUE;
}

#ifdef POSIX_SPAWN_AVAILABLE
static gboolean
do_posix_spawn (const gchar * const *argv,
                const gchar * const *envp,
                gboolean    search_path,
                gboolean    stdout_to_null,
                gboolean    stderr_to_null,
                gboolean    child_inherits_stdin,
                gboolean    file_and_argv_zero,
                GPid       *child_pid,
                gint       *child_close_fds,
                gint        stdin_fd,
                gint        stdout_fd,
                gint        stderr_fd,
                const gint *source_fds,
                const gint *target_fds,
                gsize       n_fds)
{
  pid_t pid;
  gint *duped_source_fds = NULL;
  gint max_target_fd = 0;
  const gchar * const *argv_pass;
  posix_spawnattr_t attr;
  posix_spawn_file_actions_t file_actions;
  gint parent_close_fds[3];
  gsize num_parent_close_fds = 0;
  GSList *child_close = NULL;
  GSList *elem;
  sigset_t mask;
  gsize i;
  int r;

  g_assert (argv != NULL && argv[0] != NULL);

  if (*argv[0] == '\0')
    {
      /* We check the simple case first. */
      return ENOENT;
    }

  r = posix_spawnattr_init (&attr);
  if (r != 0)
    return r;

  if (child_close_fds)
    {
      int i = -1;
      while (child_close_fds[++i] != -1)
        child_close = g_slist_prepend (child_close,
                                       GINT_TO_POINTER (child_close_fds[i]));
    }

  r = posix_spawnattr_setflags (&attr, POSIX_SPAWN_SETSIGDEF);
  if (r != 0)
    goto out_free_spawnattr;

  /* Reset some signal handlers that we may use */
  sigemptyset (&mask);
  sigaddset (&mask, SIGCHLD);
  sigaddset (&mask, SIGINT);
  sigaddset (&mask, SIGTERM);
  sigaddset (&mask, SIGHUP);

  r = posix_spawnattr_setsigdefault (&attr, &mask);
  if (r != 0)
    goto out_free_spawnattr;

  r = posix_spawn_file_actions_init (&file_actions);
  if (r != 0)
    goto out_free_spawnattr;

  /* Redirect pipes as required */

  if (stdin_fd >= 0)
    {
      r = posix_spawn_file_actions_adddup2 (&file_actions, stdin_fd, 0);
      if (r != 0)
        goto out_close_fds;

      if (!g_slist_find (child_close, GINT_TO_POINTER (stdin_fd)))
        child_close = g_slist_prepend (child_close, GINT_TO_POINTER (stdin_fd));
    }
  else if (!child_inherits_stdin)
    {
      /* Keep process from blocking on a read of stdin */
      gint read_null = safe_open ("/dev/null", O_RDONLY | O_CLOEXEC);
      g_assert (read_null != -1);
      parent_close_fds[num_parent_close_fds++] = read_null;

#ifndef HAVE_O_CLOEXEC
      fcntl (read_null, F_SETFD, FD_CLOEXEC);
#endif

      r = posix_spawn_file_actions_adddup2 (&file_actions, read_null, 0);
      if (r != 0)
        goto out_close_fds;
    }

  if (stdout_fd >= 0)
    {
      r = posix_spawn_file_actions_adddup2 (&file_actions, stdout_fd, 1);
      if (r != 0)
        goto out_close_fds;

      if (!g_slist_find (child_close, GINT_TO_POINTER (stdout_fd)))
        child_close = g_slist_prepend (child_close, GINT_TO_POINTER (stdout_fd));
    }
  else if (stdout_to_null)
    {
      gint write_null = safe_open ("/dev/null", O_WRONLY | O_CLOEXEC);
      g_assert (write_null != -1);
      parent_close_fds[num_parent_close_fds++] = write_null;

#ifndef HAVE_O_CLOEXEC
      fcntl (write_null, F_SETFD, FD_CLOEXEC);
#endif

      r = posix_spawn_file_actions_adddup2 (&file_actions, write_null, 1);
      if (r != 0)
        goto out_close_fds;
    }

  if (stderr_fd >= 0)
    {
      r = posix_spawn_file_actions_adddup2 (&file_actions, stderr_fd, 2);
      if (r != 0)
        goto out_close_fds;

      if (!g_slist_find (child_close, GINT_TO_POINTER (stderr_fd)))
        child_close = g_slist_prepend (child_close, GINT_TO_POINTER (stderr_fd));
    }
  else if (stderr_to_null)
    {
      gint write_null = safe_open ("/dev/null", O_WRONLY | O_CLOEXEC);
      g_assert (write_null != -1);
      parent_close_fds[num_parent_close_fds++] = write_null;

#ifndef HAVE_O_CLOEXEC
      fcntl (write_null, F_SETFD, FD_CLOEXEC);
#endif

      r = posix_spawn_file_actions_adddup2 (&file_actions, write_null, 2);
      if (r != 0)
        goto out_close_fds;
    }

  /* If source_fds[i] != target_fds[i], we need to handle the case
   * where the user has specified, e.g., 5 -> 4, 4 -> 6. We do this
   * by duping the source fds, taking care to ensure the new fds are
   * larger than any target fd to avoid introducing new conflicts.
   *
   * If source_fds[i] == target_fds[i], then we just need to leak
   * the fd into the child process, which we *could* do by temporarily
   * unsetting CLOEXEC and then setting it again after we spawn if
   * it was originally set. POSIX requires that the addup2 action unset
   * CLOEXEC if source and target are identical, so you'd think doing it
   * manually wouldn't be needed, but unfortunately as of 2021 many
   * libcs still don't do so. Example nonconforming libcs:
   *  Bionic: https://android.googlesource.com/platform/bionic/+/f6e5b582604715729b09db3e36a7aeb8c24b36a4/libc/bionic/spawn.cpp#71
   *  uclibc-ng: https://cgit.uclibc-ng.org/cgi/cgit/uclibc-ng.git/tree/librt/spawn.c?id=7c36bcae09d66bbaa35cbb02253ae0556f42677e#n88
   *
   * Anyway, unsetting CLOEXEC ourselves would open a small race window
   * where the fd could be inherited into a child process if another
   * thread spawns something at the same time, because we have not
   * called fork() and are multithreaded here. This race is avoidable by
   * using dupfd_cloexec, which we already have to do to handle the
   * source_fds[i] != target_fds[i] case. So let's always do it!
   */

  for (i = 0; i < n_fds; i++)
    max_target_fd = MAX (max_target_fd, target_fds[i]);

  if (max_target_fd == G_MAXINT)
    goto out_close_fds;

  duped_source_fds = g_new (gint, n_fds);
  for (i = 0; i < n_fds; i++)
    duped_source_fds[i] = -1;  /* initialise in case dupfd_cloexec() fails below */
  for (i = 0; i < n_fds; i++)
    {
      duped_source_fds[i] = dupfd_cloexec (source_fds[i], max_target_fd + 1);
      if (duped_source_fds[i] < 0)
        goto out_close_fds;
    }

  for (i = 0; i < n_fds; i++)
    {
      r = posix_spawn_file_actions_adddup2 (&file_actions, duped_source_fds[i], target_fds[i]);
      if (r != 0)
        goto out_close_fds;
    }

  /* Intentionally close the fds in the child as the last file action,
   * having been careful not to add the same fd to this list twice.
   *
   * This is important to allow (e.g.) for the same fd to be passed as stdout
   * and stderr (we must not close it before we have dupped it in both places,
   * and we must not attempt to close it twice).
   */
  for (elem = child_close; elem != NULL; elem = elem->next)
    {
      r = posix_spawn_file_actions_addclose (&file_actions,
                                             GPOINTER_TO_INT (elem->data));
      if (r != 0)
        goto out_close_fds;
    }

  argv_pass = file_and_argv_zero ? argv + 1 : argv;
  if (envp == NULL)
    envp = (const gchar * const *) environ;

  /* Don't search when it contains a slash. */
  if (!search_path || strchr (argv[0], '/') != NULL)
    r = posix_spawn (&pid, argv[0], &file_actions, &attr, (char * const *) argv_pass, (char * const *) envp);
  else
    r = posix_spawnp (&pid, argv[0], &file_actions, &attr, (char * const *) argv_pass, (char * const *) envp);

  if (r == 0 && child_pid != NULL)
    *child_pid = pid;

out_close_fds:
  for (i = 0; i < num_parent_close_fds; i++)
    g_clear_fd (&parent_close_fds[i], NULL);

  if (duped_source_fds != NULL)
    {
      for (i = 0; i < n_fds; i++)
        g_clear_fd (&duped_source_fds[i], NULL);
      g_free (duped_source_fds);
    }

  posix_spawn_file_actions_destroy (&file_actions);
out_free_spawnattr:
  posix_spawnattr_destroy (&attr);
  g_slist_free (child_close);

  return r;
}
#endif /* POSIX_SPAWN_AVAILABLE */

static gboolean
source_fds_collide_with_pipe (const GUnixPipe  *pipefd,
                              const int        *source_fds,
                              gsize             n_fds,
                              GError          **error)
{
  return (_g_spawn_invalid_source_fd (pipefd->fds[G_UNIX_PIPE_END_READ], source_fds, n_fds, error) ||
          _g_spawn_invalid_source_fd (pipefd->fds[G_UNIX_PIPE_END_WRITE], source_fds, n_fds, error));
}

static gboolean
fork_exec (gboolean              intermediate_child,
           const gchar          *working_directory,
           const gchar * const  *argv,
           const gchar * const  *envp,
           gboolean              close_descriptors,
           gboolean              search_path,
           gboolean              search_path_from_envp,
           gboolean              stdout_to_null,
           gboolean              stderr_to_null,
           gboolean              child_inherits_stdin,
           gboolean              file_and_argv_zero,
           gboolean              cloexec_pipes,
           GSpawnChildSetupFunc  child_setup,
           gpointer              user_data,
           GPid                 *child_pid,
           gint                 *stdin_pipe_out,
           gint                 *stdout_pipe_out,
           gint                 *stderr_pipe_out,
           gint                  stdin_fd,
           gint                  stdout_fd,
           gint                  stderr_fd,
           const gint           *source_fds,
           const gint           *target_fds,
           gsize                 n_fds,
           GError              **error)
{
  GPid pid = -1;
  GUnixPipe child_err_report_pipe = G_UNIX_PIPE_INIT;
  GUnixPipe child_pid_report_pipe = G_UNIX_PIPE_INIT;
  guint pipe_flags = cloexec_pipes ? O_CLOEXEC : 0;
  gint status;
  const gchar *chosen_search_path;
  gchar *search_path_buffer = NULL;
  gchar *search_path_buffer_heap = NULL;
  gsize search_path_buffer_len = 0;
  gchar **argv_buffer = NULL;
  gchar **argv_buffer_heap = NULL;
  gsize argv_buffer_len = 0;
  GUnixPipe stdin_pipe = G_UNIX_PIPE_INIT;
  GUnixPipe stdout_pipe = G_UNIX_PIPE_INIT;
  GUnixPipe stderr_pipe = G_UNIX_PIPE_INIT;
  gint child_close_fds[4] = { -1, -1, -1, -1 };
  gint n_child_close_fds = 0;
  gint *source_fds_copy = NULL;

  g_assert (argv != NULL && argv[0] != NULL);
  g_assert (stdin_pipe_out == NULL || stdin_fd < 0);
  g_assert (stdout_pipe_out == NULL || stdout_fd < 0);
  g_assert (stderr_pipe_out == NULL || stderr_fd < 0);

  /* If pipes have been requested, open them */
  if (stdin_pipe_out != NULL)
    {
      if (!g_unix_pipe_open (&stdin_pipe, pipe_flags, error))
        goto cleanup_and_fail;
      if (source_fds_collide_with_pipe (&stdin_pipe, source_fds, n_fds, error))
        goto cleanup_and_fail;
      child_close_fds[n_child_close_fds++] = g_unix_pipe_get (&stdin_pipe, G_UNIX_PIPE_END_WRITE);
      stdin_fd = g_unix_pipe_get (&stdin_pipe, G_UNIX_PIPE_END_READ);
    }

  if (stdout_pipe_out != NULL)
    {
      if (!g_unix_pipe_open (&stdout_pipe, pipe_flags, error))
        goto cleanup_and_fail;
      if (source_fds_collide_with_pipe (&stdout_pipe, source_fds, n_fds, error))
        goto cleanup_and_fail;
      child_close_fds[n_child_close_fds++] = g_unix_pipe_get (&stdout_pipe, G_UNIX_PIPE_END_READ);
      stdout_fd = g_unix_pipe_get (&stdout_pipe, G_UNIX_PIPE_END_WRITE);
    }

  if (stderr_pipe_out != NULL)
    {
      if (!g_unix_pipe_open (&stderr_pipe, pipe_flags, error))
        goto cleanup_and_fail;
      if (source_fds_collide_with_pipe (&stderr_pipe, source_fds, n_fds, error))
        goto cleanup_and_fail;
      child_close_fds[n_child_close_fds++] = g_unix_pipe_get (&stderr_pipe, G_UNIX_PIPE_END_READ);
      stderr_fd = g_unix_pipe_get (&stderr_pipe, G_UNIX_PIPE_END_WRITE);
    }

  child_close_fds[n_child_close_fds++] = -1;

#ifdef POSIX_SPAWN_AVAILABLE
  if (!intermediate_child && working_directory == NULL && !close_descriptors &&
      !search_path_from_envp && child_setup == NULL)
    {
      g_trace_mark (G_TRACE_CURRENT_TIME, 0,
                    "GLib", "posix_spawn",
                    "%s", argv[0]);

      status = do_posix_spawn (argv,
                               envp,
                               search_path,
                               stdout_to_null,
                               stderr_to_null,
                               child_inherits_stdin,
                               file_and_argv_zero,
                               child_pid,
                               child_close_fds,
                               stdin_fd,
                               stdout_fd,
                               stderr_fd,
                               source_fds,
                               target_fds,
                               n_fds);
      if (status == 0)
        goto success;

      if (status != ENOEXEC)
        {
          g_set_error (error,
                       G_SPAWN_ERROR,
                       G_SPAWN_ERROR_FAILED,
                       _("Failed to spawn child process “%s” (%s)"),
                       argv[0],
                       g_strerror (status));
          goto cleanup_and_fail;
       }

      /* posix_spawn is not intended to support script execution. It does in
       * some situations on some glibc versions, but that will be fixed.
       * So if it fails with ENOEXEC, we fall through to the regular
       * gspawn codepath so that script execution can be attempted,
       * per standard gspawn behaviour. */
      g_debug ("posix_spawn failed (ENOEXEC), fall back to regular gspawn");
    }
  else
    {
      g_trace_mark (G_TRACE_CURRENT_TIME, 0,
                    "GLib", "fork",
                    "posix_spawn avoided %s%s%s%s%s",
                    !intermediate_child ? "" : "(automatic reaping requested) ",
                    working_directory == NULL ? "" : "(workdir specified) ",
                    !close_descriptors ? "" : "(fd close requested) ",
                    !search_path_from_envp ? "" : "(using envp for search path) ",
                    child_setup == NULL ? "" : "(child_setup specified) ");
    }
#endif /* POSIX_SPAWN_AVAILABLE */

  /* Choose a search path. This has to be done before calling fork()
   * as getenv() isn’t async-signal-safe (see `man 7 signal-safety`). */
  chosen_search_path = NULL;
  if (search_path_from_envp)
    chosen_search_path = g_environ_getenv ((gchar **) envp, "PATH");
  if (search_path && chosen_search_path == NULL)
    chosen_search_path = g_getenv ("PATH");

  if ((search_path || search_path_from_envp) && chosen_search_path == NULL)
    {
      /* There is no 'PATH' in the environment.  The default
       * * search path in libc is the current directory followed by
       * * the path 'confstr' returns for '_CS_PATH'.
       * */

      /* In GLib we put . last, for security, and don't use the
       * * unportable confstr(); UNIX98 does not actually specify
       * * what to search if PATH is unset. POSIX may, dunno.
       * */

      chosen_search_path = "/bin:/usr/bin:.";
    }

  if (search_path || search_path_from_envp)
    g_assert (chosen_search_path != NULL);
  else
    g_assert (chosen_search_path == NULL);

  /* Allocate a buffer which the fork()ed child can use to assemble potential
   * paths for the binary to exec(), combining the argv[0] and elements from
   * the chosen_search_path. This can’t be done in the child because malloc()
   * (or alloca()) are not async-signal-safe (see `man 7 signal-safety`).
   *
   * Add 2 for the nul terminator and a leading `/`. */
  if (chosen_search_path != NULL)
    {
      search_path_buffer_len = strlen (chosen_search_path) + strlen (argv[0]) + 2;
      if (search_path_buffer_len < 4000)
        {
          /* Prefer small stack allocations to avoid valgrind leak warnings
           * in forked child. The 4000B cutoff is arbitrary. */
          search_path_buffer = g_alloca (search_path_buffer_len);
        }
      else
        {
          search_path_buffer_heap = g_malloc (search_path_buffer_len);
          search_path_buffer = search_path_buffer_heap;
        }
    }

  if (search_path || search_path_from_envp)
    g_assert (search_path_buffer != NULL);
  else
    g_assert (search_path_buffer == NULL);

  /* And allocate a buffer which is 2 elements longer than @argv, so that if
   * script_execute() has to be called later on, it can build a wrapper argv
   * array in this buffer. */
  argv_buffer_len = g_strv_length ((gchar **) argv) + 2;
  if (argv_buffer_len < 4000 / sizeof (gchar *))
    {
      /* Prefer small stack allocations to avoid valgrind leak warnings
       * in forked child. The 4000B cutoff is arbitrary. */
      argv_buffer = g_newa (gchar *, argv_buffer_len);
    }
  else
    {
      argv_buffer_heap = g_new (gchar *, argv_buffer_len);
      argv_buffer = argv_buffer_heap;
    }

  /* And one to hold a copy of @source_fds for later manipulation in do_exec(). */
  source_fds_copy = g_new (int, n_fds);
  if (n_fds > 0)
    memcpy (source_fds_copy, source_fds, sizeof (*source_fds) * n_fds);

  if (!g_unix_pipe_open (&child_err_report_pipe, pipe_flags, error))
    goto cleanup_and_fail;
  if (source_fds_collide_with_pipe (&child_err_report_pipe, source_fds, n_fds, error))
    goto cleanup_and_fail;

  if (intermediate_child)
    {
      if (!g_unix_pipe_open (&child_pid_report_pipe, pipe_flags, error))
        goto cleanup_and_fail;
      if (source_fds_collide_with_pipe (&child_pid_report_pipe, source_fds, n_fds, error))
        goto cleanup_and_fail;
    }

  pid = fork ();

  if (pid < 0)
    {
      int errsv = errno;

      g_set_error (error,
                   G_SPAWN_ERROR,
                   G_SPAWN_ERROR_FORK,
                   _("Failed to fork (%s)"),
                   g_strerror (errsv));

      goto cleanup_and_fail;
    }
  else if (pid == 0)
    {
      /* Immediate child. This may or may not be the child that
       * actually execs the new process.
       */

      /* Reset some signal handlers that we may use */
      signal (SIGCHLD, SIG_DFL);
      signal (SIGINT, SIG_DFL);
      signal (SIGTERM, SIG_DFL);
      signal (SIGHUP, SIG_DFL);

      /* Be sure we crash if the parent exits
       * and we write to the err_report_pipe
       */
      signal (SIGPIPE, SIG_DFL);

      /* Close the parent's end of the pipes;
       * not needed in the close_descriptors case,
       * though
       */
      g_unix_pipe_close (&child_err_report_pipe, G_UNIX_PIPE_END_READ, NULL);
      g_unix_pipe_close (&child_pid_report_pipe, G_UNIX_PIPE_END_READ, NULL);
      if (child_close_fds[0] != -1)
        {
           int i = -1;
           while (child_close_fds[++i] != -1)
             g_clear_fd (&child_close_fds[i], NULL);
        }

      if (intermediate_child)
        {
          /* We need to fork an intermediate child that launches the
           * final child. The purpose of the intermediate child
           * is to exit, so we can waitpid() it immediately.
           * Then the grandchild will not become a zombie.
           */
          GPid grandchild_pid;

          grandchild_pid = fork ();

          if (grandchild_pid < 0)
            {
              /* report -1 as child PID */
              write_all (g_unix_pipe_get (&child_pid_report_pipe, G_UNIX_PIPE_END_WRITE),
                         &grandchild_pid, sizeof(grandchild_pid));

              write_err_and_exit (g_unix_pipe_get (&child_err_report_pipe, G_UNIX_PIPE_END_WRITE),
                                  CHILD_FORK_FAILED);
            }
          else if (grandchild_pid == 0)
            {
              g_unix_pipe_close (&child_pid_report_pipe, G_UNIX_PIPE_END_WRITE, NULL);
              do_exec (g_unix_pipe_get (&child_err_report_pipe, G_UNIX_PIPE_END_WRITE),
                       stdin_fd,
                       stdout_fd,
                       stderr_fd,
                       source_fds_copy,
                       target_fds,
                       n_fds,
                       working_directory,
                       argv,
                       argv_buffer,
                       argv_buffer_len,
                       envp,
                       close_descriptors,
                       chosen_search_path,
                       search_path_buffer,
                       search_path_buffer_len,
                       stdout_to_null,
                       stderr_to_null,
                       child_inherits_stdin,
                       file_and_argv_zero,
                       child_setup,
                       user_data);
            }
          else
            {
              write_all (g_unix_pipe_get (&child_pid_report_pipe, G_UNIX_PIPE_END_WRITE),
                         &grandchild_pid, sizeof(grandchild_pid));
              g_unix_pipe_close (&child_pid_report_pipe, G_UNIX_PIPE_END_WRITE, NULL);

              _exit (0);
            }
        }
      else
        {
          /* Just run the child.
           */

          do_exec (g_unix_pipe_get (&child_err_report_pipe, G_UNIX_PIPE_END_WRITE),
                   stdin_fd,
                   stdout_fd,
                   stderr_fd,
                   source_fds_copy,
                   target_fds,
                   n_fds,
                   working_directory,
                   argv,
                   argv_buffer,
                   argv_buffer_len,
                   envp,
                   close_descriptors,
                   chosen_search_path,
                   search_path_buffer,
                   search_path_buffer_len,
                   stdout_to_null,
                   stderr_to_null,
                   child_inherits_stdin,
                   file_and_argv_zero,
                   child_setup,
                   user_data);
        }
    }
  else
    {
      /* Parent */

      gint buf[2];
      gint n_ints = 0;

      /* Close the uncared-about ends of the pipes */
      g_unix_pipe_close (&child_err_report_pipe, G_UNIX_PIPE_END_WRITE, NULL);
      g_unix_pipe_close (&child_pid_report_pipe, G_UNIX_PIPE_END_WRITE, NULL);

      /* If we had an intermediate child, reap it */
      if (intermediate_child)
        {
        wait_again:
          if (waitpid (pid, &status, 0) < 0)
            {
              if (errno == EINTR)
                goto wait_again;
              else if (errno == ECHILD)
                ; /* do nothing, child already reaped */
              else
                g_warning ("waitpid() should not fail in 'fork_exec'");
            }
        }


      if (!read_ints (g_unix_pipe_get (&child_err_report_pipe, G_UNIX_PIPE_END_READ),
                      buf, 2, &n_ints,
                      error))
        goto cleanup_and_fail;

      if (n_ints >= 2)
        {
          /* Error from the child. */

          switch (buf[0])
            {
            case CHILD_CHDIR_FAILED:
              g_set_error (error,
                           G_SPAWN_ERROR,
                           G_SPAWN_ERROR_CHDIR,
                           _("Failed to change to directory “%s” (%s)"),
                           working_directory,
                           g_strerror (buf[1]));

              break;

            case CHILD_EXEC_FAILED:
              g_set_error (error,
                           G_SPAWN_ERROR,
                           _g_spawn_exec_err_to_g_error (buf[1]),
                           _("Failed to execute child process “%s” (%s)"),
                           argv[0],
                           g_strerror (buf[1]));

              break;

            case CHILD_OPEN_FAILED:
              g_set_error (error,
                           G_SPAWN_ERROR,
                           G_SPAWN_ERROR_FAILED,
                           _("Failed to open file to remap file descriptor (%s)"),
                           g_strerror (buf[1]));
              break;

            case CHILD_DUPFD_FAILED:
              g_set_error (error,
                           G_SPAWN_ERROR,
                           G_SPAWN_ERROR_FAILED,
                           _("Failed to duplicate file descriptor for child process (%s)"),
                           g_strerror (buf[1]));

              break;

            case CHILD_FORK_FAILED:
              g_set_error (error,
                           G_SPAWN_ERROR,
                           G_SPAWN_ERROR_FORK,
                           _("Failed to fork child process (%s)"),
                           g_strerror (buf[1]));
              break;

            case CHILD_CLOSE_FAILED:
              g_set_error (error,
                           G_SPAWN_ERROR,
                           G_SPAWN_ERROR_FAILED,
                           _("Failed to close file descriptor for child process (%s)"),
                           g_strerror (buf[1]));
              break;

            default:
              g_set_error (error,
                           G_SPAWN_ERROR,
                           G_SPAWN_ERROR_FAILED,
                           _("Unknown error executing child process “%s”"),
                           argv[0]);
              break;
            }

          goto cleanup_and_fail;
        }

      /* Get child pid from intermediate child pipe. */
      if (intermediate_child)
        {
          n_ints = 0;

          if (!read_ints (g_unix_pipe_get (&child_pid_report_pipe, G_UNIX_PIPE_END_READ),
                          buf, 1, &n_ints, error))
            goto cleanup_and_fail;

          if (n_ints < 1)
            {
              int errsv = errno;

              g_set_error (error,
                           G_SPAWN_ERROR,
                           G_SPAWN_ERROR_FAILED,
                           _("Failed to read enough data from child pid pipe (%s)"),
                           g_strerror (errsv));
              goto cleanup_and_fail;
            }
          else
            {
              /* we have the child pid */
              pid = buf[0];
            }
        }

      /* Success against all odds! return the information */
      g_unix_pipe_close (&child_err_report_pipe, G_UNIX_PIPE_END_READ, NULL);
      g_unix_pipe_close (&child_pid_report_pipe, G_UNIX_PIPE_END_READ, NULL);

      g_free (search_path_buffer_heap);
      g_free (argv_buffer_heap);
      g_free (source_fds_copy);

      if (child_pid)
        *child_pid = pid;

      goto success;
    }

success:
  /* Close the uncared-about ends of the pipes */
  g_unix_pipe_close (&stdin_pipe, G_UNIX_PIPE_END_READ, NULL);
  g_unix_pipe_close (&stdout_pipe, G_UNIX_PIPE_END_WRITE, NULL);
  g_unix_pipe_close (&stderr_pipe, G_UNIX_PIPE_END_WRITE, NULL);

  if (stdin_pipe_out != NULL)
    *stdin_pipe_out = g_unix_pipe_steal (&stdin_pipe, G_UNIX_PIPE_END_WRITE);

  if (stdout_pipe_out != NULL)
    *stdout_pipe_out = g_unix_pipe_steal (&stdout_pipe, G_UNIX_PIPE_END_READ);

  if (stderr_pipe_out != NULL)
    *stderr_pipe_out = g_unix_pipe_steal (&stderr_pipe, G_UNIX_PIPE_END_READ);

  return TRUE;

 cleanup_and_fail:

  /* There was an error from the Child, reap the child to avoid it being
     a zombie.
   */

  if (pid > 0)
  {
    wait_failed:
     if (waitpid (pid, NULL, 0) < 0)
       {
          if (errno == EINTR)
            goto wait_failed;
          else if (errno == ECHILD)
            ; /* do nothing, child already reaped */
          else
            g_warning ("waitpid() should not fail in 'fork_exec'");
       }
   }

  g_unix_pipe_clear (&stdin_pipe);
  g_unix_pipe_clear (&stdout_pipe);
  g_unix_pipe_clear (&stderr_pipe);
  g_unix_pipe_clear (&child_err_report_pipe);
  g_unix_pipe_clear (&child_pid_report_pipe);

  g_clear_pointer (&search_path_buffer_heap, g_free);
  g_clear_pointer (&argv_buffer_heap, g_free);
  g_clear_pointer (&source_fds_copy, g_free);

  return FALSE;
}

/* Based on execvp from GNU C Library */

/* This function is called between fork() and exec() and hence must be
 * async-signal-safe (see signal-safety(7)) until it calls exec(). */
static gboolean
script_execute (const gchar *file,
                gchar      **argv,
                gchar      **argv_buffer,
                gsize        argv_buffer_len,
                gchar      **envp)
{
  /* Count the arguments.  */
  gsize argc = 0;
  while (argv[argc])
    ++argc;

  /* Construct an argument list for the shell. */
  if (argc + 2 > argv_buffer_len)
    return FALSE;

  argv_buffer[0] = (char *) "/bin/sh";
  argv_buffer[1] = (char *) file;
  while (argc > 0)
    {
      argv_buffer[argc + 1] = argv[argc];
      --argc;
    }

  /* Execute the shell. */
  if (envp)
    execve (argv_buffer[0], argv_buffer, envp);
  else
    execv (argv_buffer[0], argv_buffer);

  return TRUE;
}

/* This function is called between fork() and exec() and hence must be
 * async-signal-safe (see signal-safety(7)). */
static gchar*
my_strchrnul (const gchar *str, gchar c)
{
  gchar *p = (gchar*) str;
  while (*p && (*p != c))
    ++p;

  return p;
}

/* This function is called between fork() and exec() and hence must be
 * async-signal-safe (see signal-safety(7)) until it calls exec(). */
static gint
g_execute (const gchar  *file,
           gchar       **argv,
           gchar       **argv_buffer,
           gsize         argv_buffer_len,
           gchar       **envp,
           const gchar  *search_path,
           gchar        *search_path_buffer,
           gsize         search_path_buffer_len)
{
  if (file == NULL || *file == '\0')
    {
      /* We check the simple case first. */
      errno = ENOENT;
      return -1;
    }

  if (search_path == NULL || strchr (file, '/') != NULL)
    {
      /* Don't search when it contains a slash. */
      if (envp)
        execve (file, argv, envp);
      else
        execv (file, argv);

      if (errno == ENOEXEC &&
          !script_execute (file, argv, argv_buffer, argv_buffer_len, envp))
        {
          errno = ENOMEM;
          return -1;
        }
    }
  else
    {
      gboolean got_eacces = 0;
      const gchar *path, *p;
      gchar *name;
      gsize len;
      gsize pathlen;

      path = search_path;
      len = strlen (file) + 1;
      pathlen = strlen (path);
      name = search_path_buffer;

      if (search_path_buffer_len < pathlen + len + 1)
        {
          errno = ENOMEM;
          return -1;
        }

      /* Copy the file name at the top, including '\0'  */
      memcpy (name + pathlen + 1, file, len);
      name = name + pathlen;
      /* And add the slash before the filename  */
      *name = '/';

      p = path;
      do
  {
    char *startp;

    path = p;
    p = my_strchrnul (path, ':');

    if (p == path)
      /* Two adjacent colons, or a colon at the beginning or the end
             * of 'PATH' means to search the current directory.
             */
      startp = name + 1;
    else
      startp = memcpy (name - (p - path), path, p - path);

    /* Try to execute this name.  If it works, execv will not return.  */
          if (envp)
            execve (startp, argv, envp);
          else
            execv (startp, argv);

          if (errno == ENOEXEC &&
              !script_execute (startp, argv, argv_buffer, argv_buffer_len, envp))
            {
              errno = ENOMEM;
              return -1;
            }

    switch (errno)
      {
      case EACCES:
        /* Record the we got a 'Permission denied' error.  If we end
               * up finding no executable we can use, we want to diagnose
               * that we did find one but were denied access.
               */
        got_eacces = TRUE;

              G_GNUC_FALLTHROUGH;
      case ENOENT:
#ifdef ESTALE
      case ESTALE:
#endif
#ifdef ENOTDIR
      case ENOTDIR:
#endif
        /* Those errors indicate the file is missing or not executable
               * by us, in which case we want to just try the next path
               * directory.
               */
        break;

      case ENODEV:
      case ETIMEDOUT:
        /* Some strange filesystems like AFS return even
         * stranger error numbers.  They cannot reasonably mean anything
         * else so ignore those, too.
         */
        break;

      default:
        /* Some other error means we found an executable file, but
               * something went wrong executing it; return the error to our
               * caller.
               */
        return -1;
      }
  }
      while (*p++ != '\0');

      /* We tried every element and none of them worked.  */
      if (got_eacces)
        /* At least one failure was due to permissions, so report that
         * error.
         */
        errno = EACCES;
    }

  /* Return the error from the last attempt (probably ENOENT).  */
  return -1;
}

void
g_spawn_close_pid_impl (GPid pid)
{
  /* no-op */
}
