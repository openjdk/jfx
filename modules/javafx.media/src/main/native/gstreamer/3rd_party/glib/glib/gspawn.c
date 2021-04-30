/* gspawn.c - Process launching
 *
 *  Copyright 2000 Red Hat, Inc.
 *  g_execvpe implementation based on GNU libc execvp:
 *   Copyright 1991, 92, 95, 96, 97, 98, 99 Free Software Foundation, Inc.
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

/**
 * SECTION:spawn
 * @Short_description: process launching
 * @Title: Spawning Processes
 *
 * GLib supports spawning of processes with an API that is more
 * convenient than the bare UNIX fork() and exec().
 *
 * The g_spawn family of functions has synchronous (g_spawn_sync())
 * and asynchronous variants (g_spawn_async(), g_spawn_async_with_pipes()),
 * as well as convenience variants that take a complete shell-like
 * commandline (g_spawn_command_line_sync(), g_spawn_command_line_async()).
 *
 * See #GSubprocess in GIO for a higher-level API that provides
 * stream interfaces for communication with child processes.
 *
 * An example of using g_spawn_async_with_pipes():
 * |[<!-- language="C" -->
 * const gchar * const argv[] = { "my-favourite-program", "--args", NULL };
 * gint child_stdout, child_stderr;
 * GPid child_pid;
 * g_autoptr(GError) error = NULL;
 *
 * // Spawn child process.
 * g_spawn_async_with_pipes (NULL, argv, NULL, G_SPAWN_DO_NOT_REAP_CHILD, NULL,
 *                           NULL, &child_pid, NULL, &child_stdout,
 *                           &child_stderr, &error);
 * if (error != NULL)
 *   {
 *     g_error ("Spawning child failed: %s", error->message);
 *     return;
 *   }
 *
 * // Add a child watch function which will be called when the child process
 * // exits.
 * g_child_watch_add (child_pid, child_watch_cb, NULL);
 *
 * // You could watch for output on @child_stdout and @child_stderr using
 * // #GUnixInputStream or #GIOChannel here.
 *
 * static void
 * child_watch_cb (GPid     pid,
 *                 gint     status,
 *                 gpointer user_data)
 * {
 *   g_message ("Child %" G_PID_FORMAT " exited %s", pid,
 *              g_spawn_check_exit_status (status, NULL) ? "normally" : "abnormally");
 *
 *   // Free any resources associated with the child here, such as I/O channels
 *   // on its stdout and stderr FDs. If you have no code to put in the
 *   // child_watch_cb() callback, you can remove it and the g_child_watch_add()
 *   // call, but you must also remove the G_SPAWN_DO_NOT_REAP_CHILD flag,
 *   // otherwise the child process will stay around as a zombie until this
 *   // process exits.
 *
 *   g_spawn_close_pid (pid);
 * }
 * ]|
 */


static gint safe_close (gint fd);

static gint g_execute (const gchar  *file,
                       gchar       **argv,
                       gchar       **argv_buffer,
                       gsize         argv_buffer_len,
                       gchar       **envp,
                       const gchar  *search_path,
                       gchar        *search_path_buffer,
                       gsize         search_path_buffer_len);

static gboolean fork_exec_with_pipes (gboolean              intermediate_child,
                                      const gchar          *working_directory,
                                      gchar               **argv,
                                      gchar               **envp,
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
                                      gint                 *standard_input,
                                      gint                 *standard_output,
                                      gint                 *standard_error,
                                      GError              **error);

static gboolean fork_exec_with_fds (gboolean              intermediate_child,
                                    const gchar          *working_directory,
                                    gchar               **argv,
                                    gchar               **envp,
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
                                    gint                 *child_close_fds,
                                    gint                  stdin_fd,
                                    gint                  stdout_fd,
                                    gint                  stderr_fd,
                                    GError              **error);

G_DEFINE_QUARK (g-exec-error-quark, g_spawn_error)
G_DEFINE_QUARK (g-spawn-exit-error-quark, g_spawn_exit_error)

/**
 * g_spawn_async:
 * @working_directory: (type filename) (nullable): child's current working
 *     directory, or %NULL to inherit parent's
 * @argv: (array zero-terminated=1) (element-type filename):
 *     child's argument vector
 * @envp: (array zero-terminated=1) (element-type filename) (nullable):
 *     child's environment, or %NULL to inherit parent's
 * @flags: flags from #GSpawnFlags
 * @child_setup: (scope async) (nullable): function to run in the child just before exec()
 * @user_data: (closure): user data for @child_setup
 * @child_pid: (out) (optional): return location for child process reference, or %NULL
 * @error: return location for error
 *
 * See g_spawn_async_with_pipes() for a full description; this function
 * simply calls the g_spawn_async_with_pipes() without any pipes.
 *
 * You should call g_spawn_close_pid() on the returned child process
 * reference when you don't need it any more.
 *
 * If you are writing a GTK+ application, and the program you are spawning is a
 * graphical application too, then to ensure that the spawned program opens its
 * windows on the right screen, you may want to use #GdkAppLaunchContext,
 * #GAppLaunchContext, or set the %DISPLAY environment variable.
 *
 * Note that the returned @child_pid on Windows is a handle to the child
 * process and not its identifier. Process handles and process identifiers
 * are different concepts on Windows.
 *
 * Returns: %TRUE on success, %FALSE if error is set
 **/
gboolean
g_spawn_async (const gchar          *working_directory,
               gchar               **argv,
               gchar               **envp,
               GSpawnFlags           flags,
               GSpawnChildSetupFunc  child_setup,
               gpointer              user_data,
               GPid                 *child_pid,
               GError              **error)
{
  g_return_val_if_fail (argv != NULL, FALSE);

  return g_spawn_async_with_pipes (working_directory,
                                   argv, envp,
                                   flags,
                                   child_setup,
                                   user_data,
                                   child_pid,
                                   NULL, NULL, NULL,
                                   error);
}

/* Avoids a danger in threaded situations (calling close()
 * on a file descriptor twice, and another thread has
 * re-opened it since the first close)
 *
 * This function is called between fork() and exec() and hence must be
 * async-signal-safe (see signal-safety(7)).
 */
static void
close_and_invalidate (gint *fd)
{
  if (*fd < 0)
    return;
  else
    {
      safe_close (*fd);
      *fd = -1;
    }
}

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

/**
 * g_spawn_sync:
 * @working_directory: (type filename) (nullable): child's current working
 *     directory, or %NULL to inherit parent's
 * @argv: (array zero-terminated=1) (element-type filename):
 *     child's argument vector
 * @envp: (array zero-terminated=1) (element-type filename) (nullable):
 *     child's environment, or %NULL to inherit parent's
 * @flags: flags from #GSpawnFlags
 * @child_setup: (scope async) (nullable): function to run in the child just before exec()
 * @user_data: (closure): user data for @child_setup
 * @standard_output: (out) (array zero-terminated=1) (element-type guint8) (optional): return location for child output, or %NULL
 * @standard_error: (out) (array zero-terminated=1) (element-type guint8) (optional): return location for child error messages, or %NULL
 * @exit_status: (out) (optional): return location for child exit status, as returned by waitpid(), or %NULL
 * @error: return location for error, or %NULL
 *
 * Executes a child synchronously (waits for the child to exit before returning).
 * All output from the child is stored in @standard_output and @standard_error,
 * if those parameters are non-%NULL. Note that you must set the
 * %G_SPAWN_STDOUT_TO_DEV_NULL and %G_SPAWN_STDERR_TO_DEV_NULL flags when
 * passing %NULL for @standard_output and @standard_error.
 *
 * If @exit_status is non-%NULL, the platform-specific exit status of
 * the child is stored there; see the documentation of
 * g_spawn_check_exit_status() for how to use and interpret this.
 * Note that it is invalid to pass %G_SPAWN_DO_NOT_REAP_CHILD in
 * @flags, and on POSIX platforms, the same restrictions as for
 * g_child_watch_source_new() apply.
 *
 * If an error occurs, no data is returned in @standard_output,
 * @standard_error, or @exit_status.
 *
 * This function calls g_spawn_async_with_pipes() internally; see that
 * function for full details on the other parameters and details on
 * how these functions work on Windows.
 *
 * Returns: %TRUE on success, %FALSE if an error was set
 */
gboolean
g_spawn_sync (const gchar          *working_directory,
              gchar               **argv,
              gchar               **envp,
              GSpawnFlags           flags,
              GSpawnChildSetupFunc  child_setup,
              gpointer              user_data,
              gchar               **standard_output,
              gchar               **standard_error,
              gint                 *exit_status,
              GError              **error)
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

  if (!fork_exec_with_pipes (FALSE,
                             working_directory,
                             argv,
                             envp,
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
              close_and_invalidate (&outpipe);
              outpipe = -1;
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
              close_and_invalidate (&errpipe);
              errpipe = -1;
              break;
            default:
              break;
            }

          if (failed)
            break;
        }
    }

  /* These should only be open still if we had an error.  */

  if (outpipe >= 0)
    close_and_invalidate (&outpipe);
  if (errpipe >= 0)
    close_and_invalidate (&errpipe);

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
          if (exit_status)
            {
              g_warning ("In call to g_spawn_sync(), exit status of a child process was requested but ECHILD was received by waitpid(). See the documentation of g_child_watch_source_new() for possible causes.");
            }
          else
            {
              /* We don't need the exit status. */
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
      if (exit_status)
        *exit_status = status;

      if (standard_output)
        *standard_output = g_string_free (outstr, FALSE);

      if (standard_error)
        *standard_error = g_string_free (errstr, FALSE);

      return TRUE;
    }
}

/**
 * g_spawn_async_with_pipes:
 * @working_directory: (type filename) (nullable): child's current working
 *     directory, or %NULL to inherit parent's, in the GLib file name encoding
 * @argv: (array zero-terminated=1) (element-type filename): child's argument
 *     vector, in the GLib file name encoding
 * @envp: (array zero-terminated=1) (element-type filename) (nullable):
 *     child's environment, or %NULL to inherit parent's, in the GLib file
 *     name encoding
 * @flags: flags from #GSpawnFlags
 * @child_setup: (scope async) (nullable): function to run in the child just before exec()
 * @user_data: (closure): user data for @child_setup
 * @child_pid: (out) (optional): return location for child process ID, or %NULL
 * @standard_input: (out) (optional): return location for file descriptor to write to child's stdin, or %NULL
 * @standard_output: (out) (optional): return location for file descriptor to read child's stdout, or %NULL
 * @standard_error: (out) (optional): return location for file descriptor to read child's stderr, or %NULL
 * @error: return location for error
 *
 * Executes a child program asynchronously (your program will not
 * block waiting for the child to exit). The child program is
 * specified by the only argument that must be provided, @argv.
 * @argv should be a %NULL-terminated array of strings, to be passed
 * as the argument vector for the child. The first string in @argv
 * is of course the name of the program to execute. By default, the
 * name of the program must be a full path. If @flags contains the
 * %G_SPAWN_SEARCH_PATH flag, the `PATH` environment variable is
 * used to search for the executable. If @flags contains the
 * %G_SPAWN_SEARCH_PATH_FROM_ENVP flag, the `PATH` variable from
 * @envp is used to search for the executable. If both the
 * %G_SPAWN_SEARCH_PATH and %G_SPAWN_SEARCH_PATH_FROM_ENVP flags
 * are set, the `PATH` variable from @envp takes precedence over
 * the environment variable.
 *
 * If the program name is not a full path and %G_SPAWN_SEARCH_PATH flag is not
 * used, then the program will be run from the current directory (or
 * @working_directory, if specified); this might be unexpected or even
 * dangerous in some cases when the current directory is world-writable.
 *
 * On Windows, note that all the string or string vector arguments to
 * this function and the other g_spawn*() functions are in UTF-8, the
 * GLib file name encoding. Unicode characters that are not part of
 * the system codepage passed in these arguments will be correctly
 * available in the spawned program only if it uses wide character API
 * to retrieve its command line. For C programs built with Microsoft's
 * tools it is enough to make the program have a wmain() instead of
 * main(). wmain() has a wide character argument vector as parameter.
 *
 * At least currently, mingw doesn't support wmain(), so if you use
 * mingw to develop the spawned program, it should call
 * g_win32_get_command_line() to get arguments in UTF-8.
 *
 * On Windows the low-level child process creation API CreateProcess()
 * doesn't use argument vectors, but a command line. The C runtime
 * library's spawn*() family of functions (which g_spawn_async_with_pipes()
 * eventually calls) paste the argument vector elements together into
 * a command line, and the C runtime startup code does a corresponding
 * reconstruction of an argument vector from the command line, to be
 * passed to main(). Complications arise when you have argument vector
 * elements that contain spaces or double quotes. The `spawn*()` functions
 * don't do any quoting or escaping, but on the other hand the startup
 * code does do unquoting and unescaping in order to enable receiving
 * arguments with embedded spaces or double quotes. To work around this
 * asymmetry, g_spawn_async_with_pipes() will do quoting and escaping on
 * argument vector elements that need it before calling the C runtime
 * spawn() function.
 *
 * The returned @child_pid on Windows is a handle to the child
 * process, not its identifier. Process handles and process
 * identifiers are different concepts on Windows.
 *
 * @envp is a %NULL-terminated array of strings, where each string
 * has the form `KEY=VALUE`. This will become the child's environment.
 * If @envp is %NULL, the child inherits its parent's environment.
 *
 * @flags should be the bitwise OR of any flags you want to affect the
 * function's behaviour. The %G_SPAWN_DO_NOT_REAP_CHILD means that the
 * child will not automatically be reaped; you must use a child watch
 * (g_child_watch_add()) to be notified about the death of the child process,
 * otherwise it will stay around as a zombie process until this process exits.
 * Eventually you must call g_spawn_close_pid() on the @child_pid, in order to
 * free resources which may be associated with the child process. (On Unix,
 * using a child watch is equivalent to calling waitpid() or handling
 * the %SIGCHLD signal manually. On Windows, calling g_spawn_close_pid()
 * is equivalent to calling CloseHandle() on the process handle returned
 * in @child_pid). See g_child_watch_add().
 *
 * Open UNIX file descriptors marked as `FD_CLOEXEC` will be automatically
 * closed in the child process. %G_SPAWN_LEAVE_DESCRIPTORS_OPEN means that
 * other open file descriptors will be inherited by the child; otherwise all
 * descriptors except stdin/stdout/stderr will be closed before calling exec()
 * in the child. %G_SPAWN_SEARCH_PATH means that @argv[0] need not be an
 * absolute path, it will be looked for in the `PATH` environment
 * variable. %G_SPAWN_SEARCH_PATH_FROM_ENVP means need not be an
 * absolute path, it will be looked for in the `PATH` variable from
 * @envp. If both %G_SPAWN_SEARCH_PATH and %G_SPAWN_SEARCH_PATH_FROM_ENVP
 * are used, the value from @envp takes precedence over the environment.
 * %G_SPAWN_STDOUT_TO_DEV_NULL means that the child's standard output
 * will be discarded, instead of going to the same location as the parent's
 * standard output. If you use this flag, @standard_output must be %NULL.
 * %G_SPAWN_STDERR_TO_DEV_NULL means that the child's standard error
 * will be discarded, instead of going to the same location as the parent's
 * standard error. If you use this flag, @standard_error must be %NULL.
 * %G_SPAWN_CHILD_INHERITS_STDIN means that the child will inherit the parent's
 * standard input (by default, the child's standard input is attached to
 * `/dev/null`). If you use this flag, @standard_input must be %NULL.
 * %G_SPAWN_FILE_AND_ARGV_ZERO means that the first element of @argv is
 * the file to execute, while the remaining elements are the actual
 * argument vector to pass to the file. Normally g_spawn_async_with_pipes()
 * uses @argv[0] as the file to execute, and passes all of @argv to the child.
 *
 * @child_setup and @user_data are a function and user data. On POSIX
 * platforms, the function is called in the child after GLib has
 * performed all the setup it plans to perform (including creating
 * pipes, closing file descriptors, etc.) but before calling exec().
 * That is, @child_setup is called just before calling exec() in the
 * child. Obviously actions taken in this function will only affect
 * the child, not the parent.
 *
 * On Windows, there is no separate fork() and exec() functionality.
 * Child processes are created and run with a single API call,
 * CreateProcess(). There is no sensible thing @child_setup
 * could be used for on Windows so it is ignored and not called.
 *
 * If non-%NULL, @child_pid will on Unix be filled with the child's
 * process ID. You can use the process ID to send signals to the child,
 * or to use g_child_watch_add() (or waitpid()) if you specified the
 * %G_SPAWN_DO_NOT_REAP_CHILD flag. On Windows, @child_pid will be
 * filled with a handle to the child process only if you specified the
 * %G_SPAWN_DO_NOT_REAP_CHILD flag. You can then access the child
 * process using the Win32 API, for example wait for its termination
 * with the WaitFor*() functions, or examine its exit code with
 * GetExitCodeProcess(). You should close the handle with CloseHandle()
 * or g_spawn_close_pid() when you no longer need it.
 *
 * If non-%NULL, the @standard_input, @standard_output, @standard_error
 * locations will be filled with file descriptors for writing to the child's
 * standard input or reading from its standard output or standard error.
 * The caller of g_spawn_async_with_pipes() must close these file descriptors
 * when they are no longer in use. If these parameters are %NULL, the
 * corresponding pipe won't be created.
 *
 * If @standard_input is %NULL, the child's standard input is attached to
 * `/dev/null` unless %G_SPAWN_CHILD_INHERITS_STDIN is set.
 *
 * If @standard_error is NULL, the child's standard error goes to the same
 * location as the parent's standard error unless %G_SPAWN_STDERR_TO_DEV_NULL
 * is set.
 *
 * If @standard_output is NULL, the child's standard output goes to the same
 * location as the parent's standard output unless %G_SPAWN_STDOUT_TO_DEV_NULL
 * is set.
 *
 * @error can be %NULL to ignore errors, or non-%NULL to report errors.
 * If an error is set, the function returns %FALSE. Errors are reported
 * even if they occur in the child (for example if the executable in
 * @argv[0] is not found). Typically the `message` field of returned
 * errors should be displayed to users. Possible errors are those from
 * the #G_SPAWN_ERROR domain.
 *
 * If an error occurs, @child_pid, @standard_input, @standard_output,
 * and @standard_error will not be filled with valid values.
 *
 * If @child_pid is not %NULL and an error does not occur then the returned
 * process reference must be closed using g_spawn_close_pid().
 *
 * On modern UNIX platforms, GLib can use an efficient process launching
 * codepath driven internally by posix_spawn(). This has the advantage of
 * avoiding the fork-time performance costs of cloning the parent process
 * address space, and avoiding associated memory overcommit checks that are
 * not relevant in the context of immediately executing a distinct process.
 * This optimized codepath will be used provided that the following conditions
 * are met:
 *
 * 1. %G_SPAWN_DO_NOT_REAP_CHILD is set
 * 2. %G_SPAWN_LEAVE_DESCRIPTORS_OPEN is set
 * 3. %G_SPAWN_SEARCH_PATH_FROM_ENVP is not set
 * 4. @working_directory is %NULL
 * 5. @child_setup is %NULL
 * 6. The program is of a recognised binary format, or has a shebang. Otherwise, GLib will have to execute the program through the shell, which is not done using the optimized codepath.
 *
 * If you are writing a GTK+ application, and the program you are spawning is a
 * graphical application too, then to ensure that the spawned program opens its
 * windows on the right screen, you may want to use #GdkAppLaunchContext,
 * #GAppLaunchContext, or set the %DISPLAY environment variable.
 *
 * Returns: %TRUE on success, %FALSE if an error was set
 */
gboolean
g_spawn_async_with_pipes (const gchar          *working_directory,
                          gchar               **argv,
                          gchar               **envp,
                          GSpawnFlags           flags,
                          GSpawnChildSetupFunc  child_setup,
                          gpointer              user_data,
                          GPid                 *child_pid,
                          gint                 *standard_input,
                          gint                 *standard_output,
                          gint                 *standard_error,
                          GError              **error)
{
  g_return_val_if_fail (argv != NULL, FALSE);
  g_return_val_if_fail (standard_output == NULL ||
                        !(flags & G_SPAWN_STDOUT_TO_DEV_NULL), FALSE);
  g_return_val_if_fail (standard_error == NULL ||
                        !(flags & G_SPAWN_STDERR_TO_DEV_NULL), FALSE);
  /* can't inherit stdin if we have an input pipe. */
  g_return_val_if_fail (standard_input == NULL ||
                        !(flags & G_SPAWN_CHILD_INHERITS_STDIN), FALSE);

  return fork_exec_with_pipes (!(flags & G_SPAWN_DO_NOT_REAP_CHILD),
                               working_directory,
                               argv,
                               envp,
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
                               child_pid,
                               standard_input,
                               standard_output,
                               standard_error,
                               error);
}

/**
 * g_spawn_async_with_fds:
 * @working_directory: (type filename) (nullable): child's current working directory, or %NULL to inherit parent's, in the GLib file name encoding
 * @argv: (array zero-terminated=1): child's argument vector, in the GLib file name encoding
 * @envp: (array zero-terminated=1) (nullable): child's environment, or %NULL to inherit parent's, in the GLib file name encoding
 * @flags: flags from #GSpawnFlags
 * @child_setup: (scope async) (nullable): function to run in the child just before exec()
 * @user_data: (closure): user data for @child_setup
 * @child_pid: (out) (optional): return location for child process ID, or %NULL
 * @stdin_fd: file descriptor to use for child's stdin, or -1
 * @stdout_fd: file descriptor to use for child's stdout, or -1
 * @stderr_fd: file descriptor to use for child's stderr, or -1
 * @error: return location for error
 *
 * Identical to g_spawn_async_with_pipes() but instead of
 * creating pipes for the stdin/stdout/stderr, you can pass existing
 * file descriptors into this function through the @stdin_fd,
 * @stdout_fd and @stderr_fd parameters. The following @flags
 * also have their behaviour slightly tweaked as a result:
 *
 * %G_SPAWN_STDOUT_TO_DEV_NULL means that the child's standard output
 * will be discarded, instead of going to the same location as the parent's
 * standard output. If you use this flag, @standard_output must be -1.
 * %G_SPAWN_STDERR_TO_DEV_NULL means that the child's standard error
 * will be discarded, instead of going to the same location as the parent's
 * standard error. If you use this flag, @standard_error must be -1.
 * %G_SPAWN_CHILD_INHERITS_STDIN means that the child will inherit the parent's
 * standard input (by default, the child's standard input is attached to
 * /dev/null). If you use this flag, @standard_input must be -1.
 *
 * It is valid to pass the same fd in multiple parameters (e.g. you can pass
 * a single fd for both stdout and stderr).
 *
 * Returns: %TRUE on success, %FALSE if an error was set
 *
 * Since: 2.58
 */
gboolean
g_spawn_async_with_fds (const gchar          *working_directory,
                        gchar               **argv,
                        gchar               **envp,
                        GSpawnFlags           flags,
                        GSpawnChildSetupFunc  child_setup,
                        gpointer              user_data,
                        GPid                 *child_pid,
                        gint                  stdin_fd,
                        gint                  stdout_fd,
                        gint                  stderr_fd,
                        GError              **error)
{
  g_return_val_if_fail (argv != NULL, FALSE);
  g_return_val_if_fail (stdout_fd < 0 ||
                        !(flags & G_SPAWN_STDOUT_TO_DEV_NULL), FALSE);
  g_return_val_if_fail (stderr_fd < 0 ||
                        !(flags & G_SPAWN_STDERR_TO_DEV_NULL), FALSE);
  /* can't inherit stdin if we have an input pipe. */
  g_return_val_if_fail (stdin_fd < 0 ||
                        !(flags & G_SPAWN_CHILD_INHERITS_STDIN), FALSE);

  return fork_exec_with_fds (!(flags & G_SPAWN_DO_NOT_REAP_CHILD),
                               working_directory,
                               argv,
                               envp,
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
                               child_pid,
                               NULL,
                               stdin_fd,
                               stdout_fd,
                               stderr_fd,
                               error);
}

/**
 * g_spawn_command_line_sync:
 * @command_line: (type filename): a command line
 * @standard_output: (out) (array zero-terminated=1) (element-type guint8) (optional): return location for child output
 * @standard_error: (out) (array zero-terminated=1) (element-type guint8) (optional): return location for child errors
 * @exit_status: (out) (optional): return location for child exit status, as returned by waitpid()
 * @error: return location for errors
 *
 * A simple version of g_spawn_sync() with little-used parameters
 * removed, taking a command line instead of an argument vector.  See
 * g_spawn_sync() for full details. @command_line will be parsed by
 * g_shell_parse_argv(). Unlike g_spawn_sync(), the %G_SPAWN_SEARCH_PATH flag
 * is enabled. Note that %G_SPAWN_SEARCH_PATH can have security
 * implications, so consider using g_spawn_sync() directly if
 * appropriate. Possible errors are those from g_spawn_sync() and those
 * from g_shell_parse_argv().
 *
 * If @exit_status is non-%NULL, the platform-specific exit status of
 * the child is stored there; see the documentation of
 * g_spawn_check_exit_status() for how to use and interpret this.
 *
 * On Windows, please note the implications of g_shell_parse_argv()
 * parsing @command_line. Parsing is done according to Unix shell rules, not
 * Windows command interpreter rules.
 * Space is a separator, and backslashes are
 * special. Thus you cannot simply pass a @command_line containing
 * canonical Windows paths, like "c:\\program files\\app\\app.exe", as
 * the backslashes will be eaten, and the space will act as a
 * separator. You need to enclose such paths with single quotes, like
 * "'c:\\program files\\app\\app.exe' 'e:\\folder\\argument.txt'".
 *
 * Returns: %TRUE on success, %FALSE if an error was set
 **/
gboolean
g_spawn_command_line_sync (const gchar  *command_line,
                           gchar       **standard_output,
                           gchar       **standard_error,
                           gint         *exit_status,
                           GError      **error)
{
  gboolean retval;
  gchar **argv = NULL;

  g_return_val_if_fail (command_line != NULL, FALSE);

  if (!g_shell_parse_argv (command_line,
                           NULL, &argv,
                           error))
    return FALSE;

  retval = g_spawn_sync (NULL,
                         argv,
                         NULL,
                         G_SPAWN_SEARCH_PATH,
                         NULL,
                         NULL,
                         standard_output,
                         standard_error,
                         exit_status,
                         error);
  g_strfreev (argv);

  return retval;
}

/**
 * g_spawn_command_line_async:
 * @command_line: (type filename): a command line
 * @error: return location for errors
 *
 * A simple version of g_spawn_async() that parses a command line with
 * g_shell_parse_argv() and passes it to g_spawn_async(). Runs a
 * command line in the background. Unlike g_spawn_async(), the
 * %G_SPAWN_SEARCH_PATH flag is enabled, other flags are not. Note
 * that %G_SPAWN_SEARCH_PATH can have security implications, so
 * consider using g_spawn_async() directly if appropriate. Possible
 * errors are those from g_shell_parse_argv() and g_spawn_async().
 *
 * The same concerns on Windows apply as for g_spawn_command_line_sync().
 *
 * Returns: %TRUE on success, %FALSE if error is set
 **/
gboolean
g_spawn_command_line_async (const gchar *command_line,
                            GError     **error)
{
  gboolean retval;
  gchar **argv = NULL;

  g_return_val_if_fail (command_line != NULL, FALSE);

  if (!g_shell_parse_argv (command_line,
                           NULL, &argv,
                           error))
    return FALSE;

  retval = g_spawn_async (NULL,
                          argv,
                          NULL,
                          G_SPAWN_SEARCH_PATH,
                          NULL,
                          NULL,
                          NULL,
                          error);
  g_strfreev (argv);

  return retval;
}

/**
 * g_spawn_check_exit_status:
 * @exit_status: An exit code as returned from g_spawn_sync()
 * @error: a #GError
 *
 * Set @error if @exit_status indicates the child exited abnormally
 * (e.g. with a nonzero exit code, or via a fatal signal).
 *
 * The g_spawn_sync() and g_child_watch_add() family of APIs return an
 * exit status for subprocesses encoded in a platform-specific way.
 * On Unix, this is guaranteed to be in the same format waitpid() returns,
 * and on Windows it is guaranteed to be the result of GetExitCodeProcess().
 *
 * Prior to the introduction of this function in GLib 2.34, interpreting
 * @exit_status required use of platform-specific APIs, which is problematic
 * for software using GLib as a cross-platform layer.
 *
 * Additionally, many programs simply want to determine whether or not
 * the child exited successfully, and either propagate a #GError or
 * print a message to standard error. In that common case, this function
 * can be used. Note that the error message in @error will contain
 * human-readable information about the exit status.
 *
 * The @domain and @code of @error have special semantics in the case
 * where the process has an "exit code", as opposed to being killed by
 * a signal. On Unix, this happens if WIFEXITED() would be true of
 * @exit_status. On Windows, it is always the case.
 *
 * The special semantics are that the actual exit code will be the
 * code set in @error, and the domain will be %G_SPAWN_EXIT_ERROR.
 * This allows you to differentiate between different exit codes.
 *
 * If the process was terminated by some means other than an exit
 * status, the domain will be %G_SPAWN_ERROR, and the code will be
 * %G_SPAWN_ERROR_FAILED.
 *
 * This function just offers convenience; you can of course also check
 * the available platform via a macro such as %G_OS_UNIX, and use
 * WIFEXITED() and WEXITSTATUS() on @exit_status directly. Do not attempt
 * to scan or parse the error message string; it may be translated and/or
 * change in future versions of GLib.
 *
 * Returns: %TRUE if child exited successfully, %FALSE otherwise (and
 *     @error will be set)
 *
 * Since: 2.34
 */
gboolean
g_spawn_check_exit_status (gint      exit_status,
         GError  **error)
{
  gboolean ret = FALSE;

  if (WIFEXITED (exit_status))
    {
      if (WEXITSTATUS (exit_status) != 0)
  {
    g_set_error (error, G_SPAWN_EXIT_ERROR, WEXITSTATUS (exit_status),
           _("Child process exited with code %ld"),
           (long) WEXITSTATUS (exit_status));
    goto out;
  }
    }
  else if (WIFSIGNALED (exit_status))
    {
      g_set_error (error, G_SPAWN_ERROR, G_SPAWN_ERROR_FAILED,
       _("Child process killed by signal %ld"),
       (long) WTERMSIG (exit_status));
      goto out;
    }
  else if (WIFSTOPPED (exit_status))
    {
      g_set_error (error, G_SPAWN_ERROR, G_SPAWN_ERROR_FAILED,
       _("Child process stopped by signal %ld"),
       (long) WSTOPSIG (exit_status));
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
G_GNUC_NORETURN
static void
write_err_and_exit (gint fd, gint msg)
{
  gint en = errno;

  write_all (fd, &msg, sizeof(msg));
  write_all (fd, &en, sizeof(en));

  _exit (1);
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

/* This function is called between fork() and exec() and hence must be
 * async-signal-safe (see signal-safety(7)). */
static gint
safe_close (gint fd)
{
  gint ret;

  do
    ret = close (fd);
  while (ret < 0 && errno == EINTR);

  return ret;
}

/* This function is called between fork() and exec() and hence must be
 * async-signal-safe (see signal-safety(7)). */
G_GNUC_UNUSED static int
close_func (void *data, int fd)
{
  if (fd >= GPOINTER_TO_INT (data))
    (void) safe_close (fd);

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
   * may be slow on non-Linux operating systems, especially on systems allowing
   * very high number of open file descriptors.
   */
  gint open_max = -1;
  gint fd;
  gint res = 0;

#if 0 && defined(HAVE_SYS_RESOURCE_H)
  struct rlimit rl;
#endif

#ifdef __linux__
  /* Avoid use of opendir/closedir since these are not async-signal-safe. */
  int dir_fd = open ("/proc/self/fd", O_RDONLY | O_DIRECTORY);
  if (dir_fd >= 0)
    {
      char buf[4096];
      int pos, nread;
      struct linux_dirent64 *de;

      while ((nread = syscall (SYS_getdents64, dir_fd, buf, sizeof(buf))) > 0)
        {
          for (pos = 0; pos < nread; pos += de->d_reclen)
            {
              de = (struct linux_dirent64 *)(buf + pos);

              fd = filename_to_fd (de->d_name);
              if (fd < 0 || fd == dir_fd)
                  continue;

              if ((res = cb (data, fd)) != 0)
                  break;
            }
        }

      safe_close (dir_fd);
      return res;
    }

  /* If /proc is not mounted or not accessible we fall back to the old
   * rlimit trick. */

#endif

#if 0 && defined(HAVE_SYS_RESOURCE_H)
  /* Use getrlimit() function provided by the system if it is known to be
   * async-signal safe.
   *
   * Currently there are no operating systems known to provide a safe
   * implementation, so this section is not used for now.
   */
  if (getrlimit (RLIMIT_NOFILE, &rl) == 0 && rl.rlim_max != RLIM_INFINITY)
    open_max = rl.rlim_max;
#endif
#if defined(__FreeBSD__) || defined(__OpenBSD__)
  /* Use sysconf() function provided by the system if it is known to be
   * async-signal safe.
   *
   * FreeBSD: sysconf() is included in the list of async-signal safe functions
   * found in https://man.freebsd.org/sigaction(2).
   *
   * OpenBSD: sysconf() is included in the list of async-signal safe functions
   * found in https://man.openbsd.org/sigaction.2.
   */
  if (open_max < 0)
    open_max = sysconf (_SC_OPEN_MAX);
#endif
  /* Hardcoded fallback: the default process hard limit in Linux as of 2020 */
  if (open_max < 0)
    open_max = 4096;

  for (fd = 0; fd < open_max; fd++)
      if ((res = cb (data, fd)) != 0)
          break;

  return res;
#endif
}

/* This function is called between fork() and exec() and hence must be
 * async-signal-safe (see signal-safety(7)). */
static void
safe_closefrom (int lowfd)
{
#if defined(__FreeBSD__) || defined(__OpenBSD__)
  /* Use closefrom function provided by the system if it is known to be
   * async-signal safe.
   *
   * FreeBSD: closefrom is included in the list of async-signal safe functions
   * found in https://man.freebsd.org/sigaction(2).
   *
   * OpenBSD: closefrom is not included in the list, but a direct system call
   * should be safe to use.
   */
  (void) closefrom (lowfd);
#elif defined(__DragonFly__)
  /* It is unclear whether closefrom function included in DragonFlyBSD libc_r
   * is safe to use because it calls a lot of library functions. It is also
   * unclear whether libc_r itself is still being used. Therefore, we do a
   * direct system call here ourselves to avoid possible issues.
   */
  (void) syscall (SYS_closefrom, lowfd);
#elif defined(F_CLOSEM)
  /* NetBSD and AIX have a special fcntl command which does the same thing as
   * closefrom. NetBSD also includes closefrom function, which seems to be a
   * simple wrapper of the fcntl command.
   */
  (void) fcntl (lowfd, F_CLOSEM);
#else
  (void) safe_fdwalk (close_func, GINT_TO_POINTER (lowfd));
#endif
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
  CHILD_DUP2_FAILED,
  CHILD_FORK_FAILED
};

/* This function is called between fork() and exec() and hence must be
 * async-signal-safe (see signal-safety(7)) until it calls exec(). */
static void
do_exec (gint                  child_err_report_fd,
         gint                  stdin_fd,
         gint                  stdout_fd,
         gint                  stderr_fd,
         const gchar          *working_directory,
         gchar               **argv,
         gchar               **argv_buffer,
         gsize                 argv_buffer_len,
         gchar               **envp,
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
  if (working_directory && chdir (working_directory) < 0)
    write_err_and_exit (child_err_report_fd,
                        CHILD_CHDIR_FAILED);

  /* Redirect pipes as required */

  if (stdin_fd >= 0)
    {
      /* dup2 can't actually fail here I don't think */

      if (safe_dup2 (stdin_fd, 0) < 0)
        write_err_and_exit (child_err_report_fd,
                            CHILD_DUP2_FAILED);

      set_cloexec (GINT_TO_POINTER(0), stdin_fd);
    }
  else if (!child_inherits_stdin)
    {
      /* Keep process from blocking on a read of stdin */
      gint read_null = safe_open ("/dev/null", O_RDONLY);
      if (read_null < 0)
        write_err_and_exit (child_err_report_fd,
                            CHILD_DUP2_FAILED);
      safe_dup2 (read_null, 0);
      close_and_invalidate (&read_null);
    }

  if (stdout_fd >= 0)
    {
      /* dup2 can't actually fail here I don't think */

      if (safe_dup2 (stdout_fd, 1) < 0)
        write_err_and_exit (child_err_report_fd,
                            CHILD_DUP2_FAILED);

      set_cloexec (GINT_TO_POINTER(0), stdout_fd);
    }
  else if (stdout_to_null)
    {
      gint write_null = safe_open ("/dev/null", O_WRONLY);
      if (write_null < 0)
        write_err_and_exit (child_err_report_fd,
                            CHILD_DUP2_FAILED);
      safe_dup2 (write_null, 1);
      close_and_invalidate (&write_null);
    }

  if (stderr_fd >= 0)
    {
      /* dup2 can't actually fail here I don't think */

      if (safe_dup2 (stderr_fd, 2) < 0)
        write_err_and_exit (child_err_report_fd,
                            CHILD_DUP2_FAILED);

      set_cloexec (GINT_TO_POINTER(0), stderr_fd);
    }
  else if (stderr_to_null)
    {
      gint write_null = safe_open ("/dev/null", O_WRONLY);
      safe_dup2 (write_null, 2);
      close_and_invalidate (&write_null);
    }

  /* Close all file descriptors but stdin, stdout and stderr
   * before we exec. Note that this includes
   * child_err_report_fd, which keeps the parent from blocking
   * forever on the other end of that pipe.
   */
  if (close_descriptors)
    {
      if (child_setup == NULL)
        {
          safe_dup2 (child_err_report_fd, 3);
          set_cloexec (GINT_TO_POINTER (0), 3);
          safe_closefrom (4);
          child_err_report_fd = 3;
        }
      else
        {
          safe_fdwalk (set_cloexec, GINT_TO_POINTER (3));
        }
    }
  else
    {
      /* We need to do child_err_report_fd anyway */
      set_cloexec (GINT_TO_POINTER (0), child_err_report_fd);
    }

  /* Call user function just before we exec */
  if (child_setup)
    {
      (* child_setup) (user_data);
    }

  g_execute (argv[0],
             file_and_argv_zero ? argv + 1 : argv,
             argv_buffer, argv_buffer_len,
             envp, search_path, search_path_buffer, search_path_buffer_len);

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
do_posix_spawn (gchar     **argv,
                gchar     **envp,
                gboolean    search_path,
                gboolean    stdout_to_null,
                gboolean    stderr_to_null,
                gboolean    child_inherits_stdin,
                gboolean    file_and_argv_zero,
                GPid       *child_pid,
                gint       *child_close_fds,
                gint        stdin_fd,
                gint        stdout_fd,
                gint        stderr_fd)
{
  pid_t pid;
  gchar **argv_pass;
  posix_spawnattr_t attr;
  posix_spawn_file_actions_t file_actions;
  gint parent_close_fds[3];
  gint num_parent_close_fds = 0;
  GSList *child_close = NULL;
  GSList *elem;
  sigset_t mask;
  int i, r;

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
    envp = environ;

  /* Don't search when it contains a slash. */
  if (!search_path || strchr (argv[0], '/') != NULL)
    r = posix_spawn (&pid, argv[0], &file_actions, &attr, argv_pass, envp);
  else
    r = posix_spawnp (&pid, argv[0], &file_actions, &attr, argv_pass, envp);

  if (r == 0 && child_pid != NULL)
    *child_pid = pid;

out_close_fds:
  for (i = 0; i < num_parent_close_fds; i++)
    close_and_invalidate (&parent_close_fds [i]);

  posix_spawn_file_actions_destroy (&file_actions);
out_free_spawnattr:
  posix_spawnattr_destroy (&attr);
  g_slist_free (child_close);

  return r;
}
#endif /* POSIX_SPAWN_AVAILABLE */

static gboolean
fork_exec_with_fds (gboolean              intermediate_child,
                    const gchar          *working_directory,
                    gchar               **argv,
                    gchar               **envp,
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
                    gint                 *child_close_fds,
                    gint                  stdin_fd,
                    gint                  stdout_fd,
                    gint                  stderr_fd,
                    GError              **error)
{
  GPid pid = -1;
  gint child_err_report_pipe[2] = { -1, -1 };
  gint child_pid_report_pipe[2] = { -1, -1 };
  guint pipe_flags = cloexec_pipes ? FD_CLOEXEC : 0;
  gint status;
  const gchar *chosen_search_path;
  gchar *search_path_buffer = NULL;
  gchar *search_path_buffer_heap = NULL;
  gsize search_path_buffer_len = 0;
  gchar **argv_buffer = NULL;
  gchar **argv_buffer_heap = NULL;
  gsize argv_buffer_len = 0;

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
                               stderr_fd);
      if (status == 0)
        return TRUE;

      if (status != ENOEXEC)
        {
          g_set_error (error,
                       G_SPAWN_ERROR,
                       G_SPAWN_ERROR_FAILED,
                       _("Failed to spawn child process '%s' (%s)"),
                       argv[0],
                       g_strerror (status));
          return FALSE;
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
    chosen_search_path = g_environ_getenv (envp, "PATH");
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
  argv_buffer_len = g_strv_length (argv) + 2;
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

  if (!g_unix_open_pipe (child_err_report_pipe, pipe_flags, error))
    {
      g_free (search_path_buffer_heap);
      g_free (argv_buffer_heap);
      return FALSE;
    }

  if (intermediate_child && !g_unix_open_pipe (child_pid_report_pipe, pipe_flags, error))
    goto cleanup_and_fail;

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
      close_and_invalidate (&child_err_report_pipe[0]);
      close_and_invalidate (&child_pid_report_pipe[0]);
      if (child_close_fds != NULL)
        {
           int i = -1;
           while (child_close_fds[++i] != -1)
             close_and_invalidate (&child_close_fds[i]);
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
              write_all (child_pid_report_pipe[1], &grandchild_pid,
                         sizeof(grandchild_pid));

              write_err_and_exit (child_err_report_pipe[1],
                                  CHILD_FORK_FAILED);
            }
          else if (grandchild_pid == 0)
            {
              close_and_invalidate (&child_pid_report_pipe[1]);
              do_exec (child_err_report_pipe[1],
                       stdin_fd,
                       stdout_fd,
                       stderr_fd,
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
              write_all (child_pid_report_pipe[1], &grandchild_pid, sizeof(grandchild_pid));
              close_and_invalidate (&child_pid_report_pipe[1]);

              _exit (0);
            }
        }
      else
        {
          /* Just run the child.
           */

          do_exec (child_err_report_pipe[1],
                   stdin_fd,
                   stdout_fd,
                   stderr_fd,
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
      close_and_invalidate (&child_err_report_pipe[1]);
      close_and_invalidate (&child_pid_report_pipe[1]);

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
                g_warning ("waitpid() should not fail in "
         "'fork_exec_with_pipes'");
            }
        }


      if (!read_ints (child_err_report_pipe[0],
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
                           _("Failed to change to directory '%s' (%s)"),
                           working_directory,
                           g_strerror (buf[1]));

              break;

            case CHILD_EXEC_FAILED:
              g_set_error (error,
                           G_SPAWN_ERROR,
                           _g_spawn_exec_err_to_g_error (buf[1]),
                           _("Failed to execute child process '%s' (%s)"),
                           argv[0],
                           g_strerror (buf[1]));

              break;

            case CHILD_DUP2_FAILED:
              g_set_error (error,
                           G_SPAWN_ERROR,
                           G_SPAWN_ERROR_FAILED,
                           _("Failed to redirect output or input of child process (%s)"),
                           g_strerror (buf[1]));

              break;

            case CHILD_FORK_FAILED:
              g_set_error (error,
                           G_SPAWN_ERROR,
                           G_SPAWN_ERROR_FORK,
                           _("Failed to fork child process (%s)"),
                           g_strerror (buf[1]));
              break;

            default:
              g_set_error (error,
                           G_SPAWN_ERROR,
                           G_SPAWN_ERROR_FAILED,
                           _("Unknown error executing child process '%s'"),
                           argv[0]);
              break;
            }

          goto cleanup_and_fail;
        }

      /* Get child pid from intermediate child pipe. */
      if (intermediate_child)
        {
          n_ints = 0;

          if (!read_ints (child_pid_report_pipe[0],
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
      close_and_invalidate (&child_err_report_pipe[0]);
      close_and_invalidate (&child_pid_report_pipe[0]);

      g_free (search_path_buffer_heap);
      g_free (argv_buffer_heap);

      if (child_pid)
        *child_pid = pid;

      return TRUE;
    }

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
            g_warning ("waitpid() should not fail in "
                       "'fork_exec_with_pipes'");
       }
   }

  close_and_invalidate (&child_err_report_pipe[0]);
  close_and_invalidate (&child_err_report_pipe[1]);
  close_and_invalidate (&child_pid_report_pipe[0]);
  close_and_invalidate (&child_pid_report_pipe[1]);

  g_free (search_path_buffer_heap);
  g_free (argv_buffer_heap);

  return FALSE;
}

static gboolean
fork_exec_with_pipes (gboolean              intermediate_child,
                      const gchar          *working_directory,
                      gchar               **argv,
                      gchar               **envp,
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
                      gint                 *standard_input,
                      gint                 *standard_output,
                      gint                 *standard_error,
                      GError              **error)
{
  guint pipe_flags = cloexec_pipes ? FD_CLOEXEC : 0;
  gint stdin_pipe[2] = { -1, -1 };
  gint stdout_pipe[2] = { -1, -1 };
  gint stderr_pipe[2] = { -1, -1 };
  gint child_close_fds[4];
  gboolean ret;

  if (standard_input && !g_unix_open_pipe (stdin_pipe, pipe_flags, error))
    goto cleanup_and_fail;

  if (standard_output && !g_unix_open_pipe (stdout_pipe, pipe_flags, error))
    goto cleanup_and_fail;

  if (standard_error && !g_unix_open_pipe (stderr_pipe, FD_CLOEXEC, error))
    goto cleanup_and_fail;

  child_close_fds[0] = stdin_pipe[1];
  child_close_fds[1] = stdout_pipe[0];
  child_close_fds[2] = stderr_pipe[0];
  child_close_fds[3] = -1;

  ret = fork_exec_with_fds (intermediate_child,
                            working_directory,
                            argv,
                            envp,
                            close_descriptors,
                            search_path,
                            search_path_from_envp,
                            stdout_to_null,
                            stderr_to_null,
                            child_inherits_stdin,
                            file_and_argv_zero,
                            pipe_flags,
                            child_setup,
                            user_data,
                            child_pid,
                            child_close_fds,
                            stdin_pipe[0],
                            stdout_pipe[1],
                            stderr_pipe[1],
                            error);
  if (!ret)
    goto cleanup_and_fail;

  /* Close the uncared-about ends of the pipes */
  close_and_invalidate (&stdin_pipe[0]);
  close_and_invalidate (&stdout_pipe[1]);
  close_and_invalidate (&stderr_pipe[1]);

  if (standard_input)
    *standard_input = stdin_pipe[1];

  if (standard_output)
    *standard_output = stdout_pipe[0];

  if (standard_error)
    *standard_error = stderr_pipe[0];

  return TRUE;

cleanup_and_fail:
  close_and_invalidate (&stdin_pipe[0]);
  close_and_invalidate (&stdin_pipe[1]);
  close_and_invalidate (&stdout_pipe[0]);
  close_and_invalidate (&stdout_pipe[1]);
  close_and_invalidate (&stderr_pipe[0]);
  close_and_invalidate (&stderr_pipe[1]);

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
  if (*file == '\0')
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

/**
 * g_spawn_close_pid:
 * @pid: The process reference to close
 *
 * On some platforms, notably Windows, the #GPid type represents a resource
 * which must be closed to prevent resource leaking. g_spawn_close_pid()
 * is provided for this purpose. It should be used on all platforms, even
 * though it doesn't do anything under UNIX.
 **/
void
g_spawn_close_pid (GPid pid)
{
}
