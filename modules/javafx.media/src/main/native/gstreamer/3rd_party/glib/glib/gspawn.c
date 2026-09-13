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

#include "gspawn.h"
#include "gspawn-private.h"

#include "gmessages.h"
#include "gshell.h"

#define INHERITS_OR_NULL_STDIN  (G_SPAWN_STDIN_FROM_DEV_NULL | G_SPAWN_CHILD_INHERITS_STDIN)
#define INHERITS_OR_NULL_STDOUT (G_SPAWN_STDOUT_TO_DEV_NULL | G_SPAWN_CHILD_INHERITS_STDOUT)
#define INHERITS_OR_NULL_STDERR (G_SPAWN_STDERR_TO_DEV_NULL | G_SPAWN_CHILD_INHERITS_STDERR)

/**
 * g_spawn_async:
 * @working_directory: (type filename) (nullable): child's current working
 *     directory, or %NULL to inherit parent's
 * @argv: (array zero-terminated=1) (element-type filename):
 *     child's argument vector
 * @envp: (array zero-terminated=1) (element-type filename) (nullable):
 *     child's environment, or %NULL to inherit parent's
 * @flags: flags from #GSpawnFlags
 * @child_setup: (scope async) (closure user_data) (nullable): function to run
 *     in the child just before `exec()`
 * @user_data: user data for @child_setup
 * @child_pid: (out) (optional): return location for child process reference, or %NULL
 * @error: return location for error
 *
 * Executes a child program asynchronously.
 *
 * See g_spawn_async_with_pipes() for a full description; this function
 * simply calls the g_spawn_async_with_pipes() without any pipes.
 *
 * You should call g_spawn_close_pid() on the returned child process
 * reference when you don't need it any more.
 *
 * If you are writing a GTK application, and the program you are spawning is a
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
  return g_spawn_async_with_pipes (working_directory,
                                   argv, envp,
                                   flags,
                                   child_setup,
                                   user_data,
                                   child_pid,
                                   NULL, NULL, NULL,
                                   error);
}

/**
 * g_spawn_sync:
 * @working_directory: (type filename) (nullable): child's current working
 *     directory, or %NULL to inherit parent's
 * @argv: (array zero-terminated=1) (element-type filename):
 *     child's argument vector, which must be non-empty and %NULL-terminated
 * @envp: (array zero-terminated=1) (element-type filename) (nullable):
 *     child's environment, or %NULL to inherit parent's
 * @flags: flags from #GSpawnFlags
 * @child_setup: (scope call) (closure user_data) (nullable): function to run
 *     in the child just before `exec()`
 * @user_data: user data for @child_setup
 * @standard_output: (out) (array zero-terminated=1) (element-type guint8) (optional): return location for child output, or %NULL
 * @standard_error: (out) (array zero-terminated=1) (element-type guint8) (optional): return location for child error messages, or %NULL
 * @wait_status: (out) (optional): return location for child wait status, as returned by waitpid(), or %NULL
 * @error: return location for error, or %NULL
 *
 * Executes a child synchronously (waits for the child to exit before returning).
 *
 * All output from the child is stored in @standard_output and @standard_error,
 * if those parameters are non-%NULL. Note that you must set the
 * %G_SPAWN_STDOUT_TO_DEV_NULL and %G_SPAWN_STDERR_TO_DEV_NULL flags when
 * passing %NULL for @standard_output and @standard_error.
 *
 * If @wait_status is non-%NULL, the platform-specific status of
 * the child is stored there; see the documentation of
 * g_spawn_check_wait_status() for how to use and interpret this.
 * On Unix platforms, note that it is usually not equal
 * to the integer passed to `exit()` or returned from `main()`.
 *
 * Note that it is invalid to pass %G_SPAWN_DO_NOT_REAP_CHILD in
 * @flags, and on POSIX platforms, the same restrictions as for
 * g_child_watch_source_new() apply.
 *
 * If an error occurs, no data is returned in @standard_output,
 * @standard_error, or @wait_status.
 *
 * This function calls g_spawn_async_with_pipes() internally; see that
 * function for full details on the other parameters and details on
 * how these functions work on Windows.
 *
 * Returns: %TRUE on success, %FALSE if an error was set
 */
gboolean
g_spawn_sync (const gchar           *working_directory,
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
  g_return_val_if_fail (argv != NULL, FALSE);
  g_return_val_if_fail (argv[0] != NULL, FALSE);
  g_return_val_if_fail (!(flags & G_SPAWN_DO_NOT_REAP_CHILD), FALSE);
  g_return_val_if_fail (standard_output == NULL ||
                        !(flags & G_SPAWN_STDOUT_TO_DEV_NULL), FALSE);
  g_return_val_if_fail (standard_error == NULL ||
                        !(flags & G_SPAWN_STDERR_TO_DEV_NULL), FALSE);

  return g_spawn_sync_impl (working_directory, argv, envp, flags, child_setup,
                            user_data, standard_output, standard_error,
                            wait_status, error);
}

/**
 * g_spawn_async_with_pipes:
 * @working_directory: (type filename) (nullable): child's current working
 *     directory, or %NULL to inherit parent's, in the GLib file name encoding
 * @argv: (array zero-terminated=1) (element-type filename): child's argument
 *     vector, in the GLib file name encoding; it must be non-empty and %NULL-terminated
 * @envp: (array zero-terminated=1) (element-type filename) (nullable):
 *     child's environment, or %NULL to inherit parent's, in the GLib file
 *     name encoding
 * @flags: flags from #GSpawnFlags
 * @child_setup: (scope async) (closure user_data) (nullable): function to run
 *     in the child just before `exec()`
 * @user_data: user data for @child_setup
 * @child_pid: (out) (optional): return location for child process ID, or %NULL
 * @standard_input: (out) (optional): return location for file descriptor to write to child's stdin, or %NULL
 * @standard_output: (out) (optional): return location for file descriptor to read child's stdout, or %NULL
 * @standard_error: (out) (optional): return location for file descriptor to read child's stderr, or %NULL
 * @error: return location for error
 *
 * Identical to g_spawn_async_with_pipes_and_fds() but with `n_fds` set to zero,
 * so no FD assignments are used.
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
  return g_spawn_async_with_pipes_and_fds (working_directory,
                                           (const gchar * const *) argv,
                                           (const gchar * const *) envp,
                                           flags,
                                           child_setup, user_data,
                                           -1, -1, -1,
                                           NULL, NULL, 0,
                                           child_pid,
                                           standard_input,
                                           standard_output,
                                           standard_error,
                                           error);
}

/**
 * g_spawn_async_with_pipes_and_fds:
 * @working_directory: (type filename) (nullable): child's current working
 *     directory, or %NULL to inherit parent's, in the GLib file name encoding
 * @argv: (array zero-terminated=1) (element-type filename): child's argument
 *     vector, in the GLib file name encoding; it must be non-empty and %NULL-terminated
 * @envp: (array zero-terminated=1) (element-type filename) (nullable):
 *     child's environment, or %NULL to inherit parent's, in the GLib file
 *     name encoding
 * @flags: flags from #GSpawnFlags
 * @child_setup: (scope async) (closure user_data) (nullable): function to run
 *     in the child just before `exec()`
 * @user_data: user data for @child_setup
 * @stdin_fd: file descriptor to use for child's stdin, or `-1`
 * @stdout_fd: file descriptor to use for child's stdout, or `-1`
 * @stderr_fd: file descriptor to use for child's stderr, or `-1`
 * @source_fds: (array length=n_fds) (nullable): array of FDs from the parent
 *    process to make available in the child process
 * @target_fds: (array length=n_fds) (nullable): array of FDs to remap
 *    @source_fds to in the child process
 * @n_fds: number of FDs in @source_fds and @target_fds
 * @child_pid_out: (out) (optional): return location for child process ID, or %NULL
 * @stdin_pipe_out: (out) (optional): return location for file descriptor to write to child's stdin, or %NULL
 * @stdout_pipe_out: (out) (optional): return location for file descriptor to read child's stdout, or %NULL
 * @stderr_pipe_out: (out) (optional): return location for file descriptor to read child's stderr, or %NULL
 * @error: return location for error
 *
 * Executes a child program asynchronously (your program will not
 * block waiting for the child to exit).
 *
 * The child program is specified by the only argument that must be
 * provided, @argv. @argv should be a %NULL-terminated array of strings,
 * to be passed as the argument vector for the child. The first string
 * in @argv is of course the name of the program to execute. By default,
 * the name of the program must be a full path. If @flags contains the
 * %G_SPAWN_SEARCH_PATH flag, the `PATH` environment variable is used to
 * search for the executable. If @flags contains the
 * %G_SPAWN_SEARCH_PATH_FROM_ENVP flag, the `PATH` variable from @envp
 * is used to search for the executable. If both the
 * %G_SPAWN_SEARCH_PATH and %G_SPAWN_SEARCH_PATH_FROM_ENVP flags are
 * set, the `PATH` variable from @envp takes precedence over the
 * environment variable.
 *
 * If the program name is not a full path and %G_SPAWN_SEARCH_PATH flag
 * is not used, then the program will be run from the current directory
 * (or @working_directory, if specified); this might be unexpected or even
 * dangerous in some cases when the current directory is world-writable.
 *
 * On Windows, note that all the string or string vector arguments to
 * this function and the other `g_spawn*()` functions are in UTF-8, the
 * GLib file name encoding. Unicode characters that are not part of
 * the system codepage passed in these arguments will be correctly
 * available in the spawned program only if it uses wide character API
 * to retrieve its command line. For C programs built with Microsoft's
 * tools it is enough to make the program have a `wmain()` instead of
 * `main()`. `wmain()` has a wide character argument vector as parameter.
 *
 * At least currently, mingw doesn't support `wmain()`, so if you use
 * mingw to develop the spawned program, it should call
 * g_win32_get_command_line() to get arguments in UTF-8.
 *
 * On Windows the low-level child process creation API `CreateProcess()`
 * doesn't use argument vectors, but a command line. The C runtime
 * library's `spawn*()` family of functions (which g_spawn_async_with_pipes()
 * eventually calls) paste the argument vector elements together into
 * a command line, and the C runtime startup code does a corresponding
 * reconstruction of an argument vector from the command line, to be
 * passed to `main()`. Complications arise when you have argument vector
 * elements that contain spaces or double quotes. The `spawn*()` functions
 * don't do any quoting or escaping, but on the other hand the startup
 * code does do unquoting and unescaping in order to enable receiving
 * arguments with embedded spaces or double quotes. To work around this
 * asymmetry, g_spawn_async_with_pipes() will do quoting and escaping on
 * argument vector elements that need it before calling the C runtime
 * `spawn()` function.
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
 * the `SIGCHLD` signal manually. On Windows, calling g_spawn_close_pid()
 * is equivalent to calling `CloseHandle()` on the process handle returned
 * in @child_pid). See g_child_watch_add().
 *
 * Open UNIX file descriptors marked as `FD_CLOEXEC` will be automatically
 * closed in the child process. %G_SPAWN_LEAVE_DESCRIPTORS_OPEN means that
 * other open file descriptors will be inherited by the child; otherwise all
 * descriptors except stdin/stdout/stderr will be closed before calling `exec()`
 * in the child. %G_SPAWN_SEARCH_PATH means that @argv[0] need not be an
 * absolute path, it will be looked for in the `PATH` environment
 * variable. %G_SPAWN_SEARCH_PATH_FROM_ENVP means need not be an
 * absolute path, it will be looked for in the `PATH` variable from
 * @envp. If both %G_SPAWN_SEARCH_PATH and %G_SPAWN_SEARCH_PATH_FROM_ENVP
 * are used, the value from @envp takes precedence over the environment.
 *
 * %G_SPAWN_CHILD_INHERITS_STDIN means that the child will inherit the parent's
 * standard input (by default, the child's standard input is attached to
 * `/dev/null`). %G_SPAWN_STDIN_FROM_DEV_NULL explicitly imposes the default
 * behavior. Both flags cannot be enabled at the same time and, in both cases,
 * the @stdin_pipe_out argument is ignored.
 *
 * %G_SPAWN_STDOUT_TO_DEV_NULL means that the child's standard output
 * will be discarded (by default, it goes to the same location as the parent's
 * standard output). %G_SPAWN_CHILD_INHERITS_STDOUT explicitly imposes the
 * default behavior. Both flags cannot be enabled at the same time and, in
 * both cases, the @stdout_pipe_out argument is ignored.
 *
 * %G_SPAWN_STDERR_TO_DEV_NULL means that the child's standard error
 * will be discarded (by default, it goes to the same location as the parent's
 * standard error). %G_SPAWN_CHILD_INHERITS_STDERR explicitly imposes the
 * default behavior. Both flags cannot be enabled at the same time and, in
 * both cases, the @stderr_pipe_out argument is ignored.
 *
 * It is valid to pass the same FD in multiple parameters (e.g. you can pass
 * a single FD for both @stdout_fd and @stderr_fd, and include it in
 * @source_fds too).
 *
 * @source_fds and @target_fds allow zero or more FDs from this process to be
 * remapped to different FDs in the spawned process. If @n_fds is greater than
 * zero, @source_fds and @target_fds must both be non-%NULL and the same length.
 * Each FD in @source_fds is remapped to the FD number at the same index in
 * @target_fds. The source and target FD may be equal to simply propagate an FD
 * to the spawned process. FD remappings are processed after standard FDs, so
 * any target FDs which equal @stdin_fd, @stdout_fd or @stderr_fd will overwrite
 * them in the spawned process.
 *
 * @source_fds is supported on Windows since 2.72.
 *
 * %G_SPAWN_FILE_AND_ARGV_ZERO means that the first element of @argv is
 * the file to execute, while the remaining elements are the actual
 * argument vector to pass to the file. Normally g_spawn_async_with_pipes()
 * uses @argv[0] as the file to execute, and passes all of @argv to the child.
 *
 * @child_setup and @user_data are a function and user data. On POSIX
 * platforms, the function is called in the child after GLib has
 * performed all the setup it plans to perform (including creating
 * pipes, closing file descriptors, etc.) but before calling `exec()`.
 * That is, @child_setup is called just before calling `exec()` in the
 * child. Obviously actions taken in this function will only affect
 * the child, not the parent.
 *
 * On Windows, there is no separate `fork()` and `exec()` functionality.
 * Child processes are created and run with a single API call,
 * `CreateProcess()`. There is no sensible thing @child_setup
 * could be used for on Windows so it is ignored and not called.
 *
 * If non-%NULL, @child_pid will on Unix be filled with the child's
 * process ID. You can use the process ID to send signals to the child,
 * or to use g_child_watch_add() (or `waitpid()`) if you specified the
 * %G_SPAWN_DO_NOT_REAP_CHILD flag. On Windows, @child_pid will be
 * filled with a handle to the child process only if you specified the
 * %G_SPAWN_DO_NOT_REAP_CHILD flag. You can then access the child
 * process using the Win32 API, for example wait for its termination
 * with the `WaitFor*()` functions, or examine its exit code with
 * `GetExitCodeProcess()`. You should close the handle with `CloseHandle()`
 * or g_spawn_close_pid() when you no longer need it.
 *
 * If non-%NULL, the @stdin_pipe_out, @stdout_pipe_out, @stderr_pipe_out
 * locations will be filled with file descriptors for writing to the child's
 * standard input or reading from its standard output or standard error.
 * The caller of g_spawn_async_with_pipes() must close these file descriptors
 * when they are no longer in use. If these parameters are %NULL, the
 * corresponding pipe won't be created.
 *
 * If @stdin_pipe_out is %NULL, the child's standard input is attached to
 * `/dev/null` unless %G_SPAWN_CHILD_INHERITS_STDIN is set.
 *
 * If @stderr_pipe_out is NULL, the child's standard error goes to the same
 * location as the parent's standard error unless %G_SPAWN_STDERR_TO_DEV_NULL
 * is set.
 *
 * If @stdout_pipe_out is NULL, the child's standard output goes to the same
 * location as the parent's standard output unless %G_SPAWN_STDOUT_TO_DEV_NULL
 * is set.
 *
 * @error can be %NULL to ignore errors, or non-%NULL to report errors.
 * If an error is set, the function returns %FALSE. Errors are reported
 * even if they occur in the child (for example if the executable in
 * `@argv[0]` is not found). Typically the `message` field of returned
 * errors should be displayed to users. Possible errors are those from
 * the %G_SPAWN_ERROR domain.
 *
 * If an error occurs, @child_pid, @stdin_pipe_out, @stdout_pipe_out,
 * and @stderr_pipe_out will not be filled with valid values.
 *
 * If @child_pid is not %NULL and an error does not occur then the returned
 * process reference must be closed using g_spawn_close_pid().
 *
 * On modern UNIX platforms, GLib can use an efficient process launching
 * codepath driven internally by `posix_spawn()`. This has the advantage of
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
 * 6. The program is of a recognised binary format, or has a shebang.
 *    Otherwise, GLib will have to execute the program through the
 *    shell, which is not done using the optimized codepath.
 *
 * If you are writing a GTK application, and the program you are spawning is a
 * graphical application too, then to ensure that the spawned program opens its
 * windows on the right screen, you may want to use #GdkAppLaunchContext,
 * #GAppLaunchContext, or set the `DISPLAY` environment variable.
 *
 * Returns: %TRUE on success, %FALSE if an error was set
 *
 * Since: 2.68
 */
gboolean
g_spawn_async_with_pipes_and_fds (const gchar           *working_directory,
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

  return g_spawn_async_with_pipes_and_fds_impl (working_directory, argv,
                                                envp, flags, child_setup,
                                                user_data, stdin_fd, stdout_fd,
                                                stderr_fd,
                                                source_fds, target_fds, n_fds,
                                                child_pid_out, stdin_pipe_out,
                                                stdout_pipe_out,
                                                stderr_pipe_out, error);
}

/**
 * g_spawn_async_with_fds:
 * @working_directory: (type filename) (nullable): child's current working directory, or %NULL to inherit parent's, in the GLib file name encoding
 * @argv: (array zero-terminated=1): child's argument vector, in the GLib file name encoding;
 *   it must be non-empty and %NULL-terminated
 * @envp: (array zero-terminated=1) (nullable): child's environment, or %NULL to inherit parent's, in the GLib file name encoding
 * @flags: flags from #GSpawnFlags
 * @child_setup: (scope async) (closure user_data) (nullable): function to run
 *   in the child just before `exec()`
 * @user_data: user data for @child_setup
 * @child_pid: (out) (optional): return location for child process ID, or %NULL
 * @stdin_fd: file descriptor to use for child's stdin, or `-1`
 * @stdout_fd: file descriptor to use for child's stdout, or `-1`
 * @stderr_fd: file descriptor to use for child's stderr, or `-1`
 * @error: return location for error
 *
 * Executes a child program asynchronously.
 *
 * Identical to g_spawn_async_with_pipes_and_fds() but with `n_fds` set to zero,
 * so no FD assignments are used.
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
  g_return_val_if_fail (stdout_fd < 0 ||
                        !(flags & G_SPAWN_STDOUT_TO_DEV_NULL), FALSE);
  g_return_val_if_fail (stderr_fd < 0 ||
                        !(flags & G_SPAWN_STDERR_TO_DEV_NULL), FALSE);
  /* can't inherit stdin if we have an input pipe. */
  g_return_val_if_fail (stdin_fd < 0 ||
                        !(flags & G_SPAWN_CHILD_INHERITS_STDIN), FALSE);

  return g_spawn_async_with_pipes_and_fds (working_directory,
                                           (const gchar * const *) argv,
                                           (const gchar * const *) envp,
                                           flags, child_setup, user_data,
                                           stdin_fd, stdout_fd, stderr_fd,
                                           NULL, NULL, 0,
                                           child_pid,
                                           NULL, NULL, NULL,
                                           error);
}

/**
 * g_spawn_command_line_sync:
 * @command_line: (type filename): a command line
 * @standard_output: (out) (array zero-terminated=1) (element-type guint8) (optional): return location for child output
 * @standard_error: (out) (array zero-terminated=1) (element-type guint8) (optional): return location for child errors
 * @wait_status: (out) (optional): return location for child wait status, as returned by waitpid()
 * @error: return location for errors
 *
 * A simple version of g_spawn_sync() with little-used parameters
 * removed, taking a command line instead of an argument vector.
 *
 * See g_spawn_sync() for full details.
 *
 * The @command_line argument will be parsed by g_shell_parse_argv().
 *
 * Unlike g_spawn_sync(), the %G_SPAWN_SEARCH_PATH flag is enabled.
 * Note that %G_SPAWN_SEARCH_PATH can have security implications, so
 * consider using g_spawn_sync() directly if appropriate.
 *
 * Possible errors are those from g_spawn_sync() and those
 * from g_shell_parse_argv().
 *
 * If @wait_status is non-%NULL, the platform-specific status of
 * the child is stored there; see the documentation of
 * g_spawn_check_wait_status() for how to use and interpret this.
 * On Unix platforms, note that it is usually not equal
 * to the integer passed to `exit()` or returned from `main()`.
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
                           gint         *wait_status,
                           GError      **error)
{
  gboolean retval;
  gchar **argv = NULL;

  g_return_val_if_fail (command_line != NULL, FALSE);

  /* This will return a runtime error if @command_line is the empty string. */
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
                         wait_status,
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
 * g_shell_parse_argv() and passes it to g_spawn_async().
 *
 * Runs a command line in the background. Unlike g_spawn_async(), the
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

  /* This will return a runtime error if @command_line is the empty string. */
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
 * g_spawn_check_wait_status:
 * @wait_status: A platform-specific wait status as returned from g_spawn_sync()
 * @error: a #GError
 *
 * Set @error if @wait_status indicates the child exited abnormally
 * (e.g. with a nonzero exit code, or via a fatal signal).
 *
 * The g_spawn_sync() and g_child_watch_add() family of APIs return the
 * status of subprocesses encoded in a platform-specific way.
 * On Unix, this is guaranteed to be in the same format waitpid() returns,
 * and on Windows it is guaranteed to be the result of GetExitCodeProcess().
 *
 * Prior to the introduction of this function in GLib 2.34, interpreting
 * @wait_status required use of platform-specific APIs, which is problematic
 * for software using GLib as a cross-platform layer.
 *
 * Additionally, many programs simply want to determine whether or not
 * the child exited successfully, and either propagate a #GError or
 * print a message to standard error. In that common case, this function
 * can be used. Note that the error message in @error will contain
 * human-readable information about the wait status.
 *
 * The @domain and @code of @error have special semantics in the case
 * where the process has an "exit code", as opposed to being killed by
 * a signal. On Unix, this happens if WIFEXITED() would be true of
 * @wait_status. On Windows, it is always the case.
 *
 * The special semantics are that the actual exit code will be the
 * code set in @error, and the domain will be %G_SPAWN_EXIT_ERROR.
 * This allows you to differentiate between different exit codes.
 *
 * If the process was terminated by some means other than an exit
 * status (for example if it was killed by a signal), the domain will be
 * %G_SPAWN_ERROR and the code will be %G_SPAWN_ERROR_FAILED.
 *
 * This function just offers convenience; you can of course also check
 * the available platform via a macro such as %G_OS_UNIX, and use
 * WIFEXITED() and WEXITSTATUS() on @wait_status directly. Do not attempt
 * to scan or parse the error message string; it may be translated and/or
 * change in future versions of GLib.
 *
 * Prior to version 2.70, g_spawn_check_exit_status() provides the same
 * functionality, although under a misleading name.
 *
 * Returns: %TRUE if child exited successfully, %FALSE otherwise (and
 *   @error will be set)
 *
 * Since: 2.70
 */
gboolean
g_spawn_check_wait_status (gint      wait_status,
                           GError  **error)
{
  return g_spawn_check_wait_status_impl (wait_status, error);
}

/**
 * g_spawn_check_exit_status:
 * @wait_status: A status as returned from g_spawn_sync()
 * @error: a #GError
 *
 * An old name for g_spawn_check_wait_status(), deprecated because its
 * name is misleading.
 *
 * Despite the name of the function, @wait_status must be the wait status
 * as returned by g_spawn_sync(), g_subprocess_get_status(), `waitpid()`,
 * etc. On Unix platforms, it is incorrect for it to be the exit status
 * as passed to `exit()` or returned by g_subprocess_get_exit_status() or
 * `WEXITSTATUS()`.
 *
 * Returns: %TRUE if child exited successfully, %FALSE otherwise (and
 *     @error will be set)
 *
 * Since: 2.34
 *
 * Deprecated: 2.70: Use g_spawn_check_wait_status() instead, and check whether your code is conflating wait and exit statuses.
 */
gboolean
g_spawn_check_exit_status (gint      wait_status,
                           GError  **error)
{
  return g_spawn_check_wait_status (wait_status, error);
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
  g_spawn_close_pid_impl (pid);
}
