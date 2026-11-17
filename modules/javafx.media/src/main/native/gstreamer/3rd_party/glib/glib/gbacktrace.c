/* GLIB - Library of useful routines for C programming
 * Copyright (C) 1995-1997  Peter Mattis, Spencer Kimball and Josh MacDonald
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

/*
 * Modified by the GLib Team and others 1997-2000.  See the AUTHORS
 * file for a list of people on the GLib Team.  See the ChangeLog
 * files for a list of changes.  These files are distributed with
 * GLib at ftp://ftp.gtk.org/pub/gtk/.
 */

/*
 * MT safe ; except for g_on_error_stack_trace, but who wants thread safety
 * then
 */

#include "config.h"
#include "glibconfig.h"

#include <signal.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>

#ifdef HAVE_SYS_TIME_H
#include <sys/time.h>
#endif
#include <sys/types.h>

#include <time.h>

#ifdef G_OS_UNIX
#include "glib-unixprivate.h"
#include <errno.h>
#include <unistd.h>
#include <sys/wait.h>
#ifdef HAVE_SYS_SELECT_H
#include <sys/select.h>
#endif /* HAVE_SYS_SELECT_H */
#endif

#include <string.h>

#ifdef G_OS_WIN32
#include <windows.h>
#else
#include <fcntl.h>
#endif

#include "gbacktrace.h"

#include "gtypes.h"
#include "gmain.h"
#include "gprintfint.h"
#include "gunicode.h"
#include "gutils.h"

#ifndef G_OS_WIN32
static void stack_trace (const char * const *args);
#endif

/* Default to using LLDB for backtraces on macOS. */
#ifdef __APPLE__
#define USE_LLDB
#endif

#ifdef USE_LLDB
#define DEBUGGER "lldb"
#else
#define DEBUGGER "gdb"
#endif

/* People want to hit this from their debugger... */
GLIB_AVAILABLE_IN_ALL volatile gboolean glib_on_error_halt;
volatile gboolean glib_on_error_halt = TRUE;

/**
 * g_on_error_query:
 * @prg_name: the program name, needed by gdb for the "[S]tack trace"
 *     option. If @prg_name is %NULL, g_get_prgname() is called to get
 *     the program name (which will work correctly if gdk_init() or
 *     gtk_init() has been called)
 *
 * Prompts the user with
 * `[E]xit, [H]alt, show [S]tack trace or [P]roceed`.
 * This function is intended to be used for debugging use only.
 * The following example shows how it can be used together with
 * the g_log() functions.
 *
 * |[<!-- language="C" -->
 * #include <glib.h>
 *
 * static void
 * log_handler (const gchar   *log_domain,
 *              GLogLevelFlags log_level,
 *              const gchar   *message,
 *              gpointer       user_data)
 * {
 *   g_log_default_handler (log_domain, log_level, message, user_data);
 *
 *   g_on_error_query (MY_PROGRAM_NAME);
 * }
 *
 * int
 * main (int argc, char *argv[])
 * {
 *   g_log_set_handler (MY_LOG_DOMAIN,
 *                      G_LOG_LEVEL_WARNING |
 *                      G_LOG_LEVEL_ERROR |
 *                      G_LOG_LEVEL_CRITICAL,
 *                      log_handler,
 *                      NULL);
 *   ...
 * ]|
 *
 * If "[E]xit" is selected, the application terminates with a call
 * to _exit(0).
 *
 * If "[S]tack" trace is selected, g_on_error_stack_trace() is called.
 * This invokes gdb, which attaches to the current process and shows
 * a stack trace. The prompt is then shown again.
 *
 * If "[P]roceed" is selected, the function returns.
 *
 * This function may cause different actions on non-UNIX platforms.
 *
 * On Windows consider using the `G_DEBUGGER` environment
 * variable (see [Running GLib Applications](glib-running.html)) and
 * calling g_on_error_stack_trace() instead.
 */
void
g_on_error_query (const gchar *prg_name)
{
#ifndef G_OS_WIN32
  static const gchar * const query1 = "[E]xit, [H]alt";
  static const gchar * const query2 = ", show [S]tack trace";
  static const gchar * const query3 = " or [P]roceed";
  gchar buf[16];

  if (!prg_name)
    prg_name = g_get_prgname ();

 retry:

  _g_fprintf (stdout,
              "(process:%u): %s%s%s: ",
              (guint) getpid (),
              query1,
              query2,
              query3);
  fflush (stdout);

  if (isatty(0) && isatty(1))
    {
      if (fgets (buf, 8, stdin) == NULL)
        _exit (0);
    }
  else
    {
      strcpy (buf, "E\n");
    }

  if ((buf[0] == 'E' || buf[0] == 'e')
      && buf[1] == '\n')
    _exit (0);
  else if ((buf[0] == 'P' || buf[0] == 'p')
           && buf[1] == '\n')
    return;
  else if ((buf[0] == 'S' || buf[0] == 's')
           && buf[1] == '\n')
    {
      g_on_error_stack_trace (prg_name);
      goto retry;
    }
  else if ((buf[0] == 'H' || buf[0] == 'h')
           && buf[1] == '\n')
    {
      while (glib_on_error_halt)
        ;
      glib_on_error_halt = TRUE;
      return;
    }
  else
    goto retry;
#else
  if (!prg_name)
    prg_name = g_get_prgname ();

  /* MessageBox is allowed on UWP apps only when building against
   * the debug CRT, which will set -D_DEBUG */
#if defined(_DEBUG) || !defined(G_WINAPI_ONLY_APP)
  {
    WCHAR *caption = NULL;

    if (prg_name && *prg_name)
      {
        caption = g_utf8_to_utf16 (prg_name, -1, NULL, NULL, NULL);
      }

    MessageBoxW (NULL, L"g_on_error_query called, program terminating",
                 caption,
                 MB_OK|MB_ICONERROR);

    g_free (caption);
  }
#else
  printf ("g_on_error_query called, program '%s' terminating\n",
      (prg_name && *prg_name) ? prg_name : "(null)");
#endif
  _exit(0);
#endif
}

/**
 * g_on_error_stack_trace:
 * @prg_name: (nullable): the program name, needed by gdb for the
 *   "[S]tack trace" option, or `NULL` to use a default string
 *
 * Invokes gdb, which attaches to the current process and shows a
 * stack trace. Called by g_on_error_query() when the "[S]tack trace"
 * option is selected. You can get the current process's program name
 * with g_get_prgname(), assuming that you have called gtk_init() or
 * gdk_init().
 *
 * This function may cause different actions on non-UNIX platforms.
 *
 * When running on Windows, this function is *not* called by
 * g_on_error_query(). If called directly, it will raise an
 * exception, which will crash the program. If the `G_DEBUGGER` environment
 * variable is set, a debugger will be invoked to attach and
 * handle that exception (see [Running GLib Applications](glib-running.html)).
 */
void
g_on_error_stack_trace (const gchar *prg_name)
{
#if defined(G_OS_UNIX)
  pid_t pid;
  gchar buf[16];
  gchar buf2[64];
  const gchar *args[5] = { DEBUGGER, NULL, NULL, NULL, NULL };
  int status;

  if (!prg_name)
    {
      _g_snprintf (buf2, sizeof (buf2), "/proc/%u/exe", (guint) getpid ());
      prg_name = buf2;
    }

  _g_snprintf (buf, sizeof (buf), "%u", (guint) getpid ());

#ifdef USE_LLDB
  args[1] = prg_name;
  args[2] = "-p";
  args[3] = buf;
#else
  args[1] = prg_name;
  args[2] = buf;
#endif

  pid = fork ();
  if (pid == 0)
    {
      stack_trace (args);
      _exit (0);
    }
  else if (pid == (pid_t) -1)
    {
      perror ("unable to fork " DEBUGGER);
      return;
    }

  /* Wait until the child really terminates. On Mac OS X waitpid ()
   * will also return when the child is being stopped due to tracing.
   */
  while (1)
    {
      pid_t retval = waitpid (pid, &status, 0);
      if (WIFEXITED (retval) || WIFSIGNALED (retval))
        break;
    }
#else
#ifdef GSTREAMER_LITE
  #ifdef G_ENABLE_DEBUG
    if (IsDebuggerPresent ())
      G_BREAKPOINT ();
    else
  #endif // G_ENABLE_DEBUG
    g_abort ();
#else // GSTREAMER_LITE
  if (IsDebuggerPresent ())
    G_BREAKPOINT ();
  else
    g_abort ();
#endif // GSTREAMER_LITE
#endif
}

#ifndef G_OS_WIN32

static gboolean stack_trace_done = FALSE;

static void
stack_trace_sigchld (int signum)
{
  stack_trace_done = TRUE;
}

#define BUFSIZE 1024

static inline const char *
get_strerror (char *buffer, gsize n)
{
#if defined(STRERROR_R_CHAR_P)
  return strerror_r (errno, buffer, n);
#elif defined(HAVE_STRERROR_R)
  int ret = strerror_r (errno, buffer, n);
  if (ret == 0 || ret == EINVAL)
    return buffer;
  return NULL;
#else
  const char *error_str = strerror (errno);
  if (!error_str)
    return NULL;

  strncpy (buffer, error_str, n);
  return buffer;
#endif
}

static gssize
checked_write (int fd, gconstpointer buf, gsize n)
{
  gssize written = write (fd, buf, n);

  if (written == -1)
    {
      char msg[BUFSIZE] = {0};
      char error_str[BUFSIZE / 2] = {0};

      get_strerror (error_str, sizeof (error_str) - 1);
      snprintf (msg, sizeof (msg) - 1, "Unable to write to fd %d: %s", fd, error_str);
      perror (msg);
      _exit (0);
    }

  return written;
}

static int
checked_dup (int fd)
{
  int new_fd = dup (fd);

  if (new_fd == -1)
    {
      char msg[BUFSIZE] = {0};
      char error_str[BUFSIZE / 2] = {0};

      get_strerror (error_str, sizeof (error_str) - 1);
      snprintf (msg, sizeof (msg) - 1, "Unable to duplicate fd %d: %s", fd, error_str);
      perror (msg);
      _exit (0);
    }

  return new_fd;
}

static void
stack_trace (const char * const *args)
{
  pid_t pid;
  int in_fd[2];
  int out_fd[2];
  fd_set fdset;
  fd_set readset;
  struct timeval tv;
  int sel, idx, state;
#ifdef USE_LLDB
  int line_idx;
#endif
  char buffer[BUFSIZE];
  char c;

  stack_trace_done = FALSE;
  signal (SIGCHLD, stack_trace_sigchld);

  if (!g_unix_open_pipe_internal (in_fd, TRUE, FALSE) ||
      !g_unix_open_pipe_internal (out_fd, TRUE, FALSE))
    {
      perror ("unable to open pipe");
      _exit (0);
    }

  pid = fork ();
  if (pid == 0)
    {
      /* Save stderr for printing failure below */
      int old_err = dup (2);
      if (old_err != -1)
        {
          int getfd = fcntl (old_err, F_GETFD);
          if (getfd != -1)
            (void) fcntl (old_err, F_SETFD, getfd | FD_CLOEXEC);
        }

      close (0);
      checked_dup (in_fd[0]);   /* set the stdin to the in pipe */
      close (1);
      checked_dup (out_fd[1]);  /* set the stdout to the out pipe */
      close (2);
      checked_dup (out_fd[1]);  /* set the stderr to the out pipe */

      execvp (args[0], (char **) args);      /* exec gdb */

      /* Print failure to original stderr */
      if (old_err != -1)
        {
          close (2);
          /* We can ignore the return value here as we're failing anyways */
          (void) !dup (old_err);
        }
      perror ("exec " DEBUGGER " failed");
      _exit (0);
    }
  else if (pid == (pid_t) -1)
    {
      perror ("unable to fork");
      _exit (0);
    }

  FD_ZERO (&fdset);
  FD_SET (out_fd[0], &fdset);

#ifdef USE_LLDB
  checked_write (in_fd[1], "bt\n", 3);
  checked_write (in_fd[1], "p x = 0\n", 8);
  checked_write (in_fd[1], "process detach\n", 15);
  checked_write (in_fd[1], "quit\n", 5);
#else
  /* Don't wrap so that lines are not truncated */
  checked_write (in_fd[1], "set width 0\n", 12);
  checked_write (in_fd[1], "set height 0\n", 13);
  checked_write (in_fd[1], "set pagination no\n", 18);
  checked_write (in_fd[1], "thread apply all backtrace\n", 27);
  checked_write (in_fd[1], "p x = 0\n", 8);
  checked_write (in_fd[1], "quit\n", 5);
#endif

  idx = 0;
#ifdef USE_LLDB
  line_idx = 0;
#endif
  state = 0;

  while (1)
    {
      readset = fdset;
      tv.tv_sec = 1;
      tv.tv_usec = 0;

      sel = select (FD_SETSIZE, &readset, NULL, NULL, &tv);
      if (sel == -1)
        break;

      if ((sel > 0) && (FD_ISSET (out_fd[0], &readset)))
        {
          if (read (out_fd[0], &c, 1))
            {
#ifdef USE_LLDB
              line_idx += 1;
#endif

              switch (state)
                {
                case 0:
#ifdef USE_LLDB
                  if (c == '*' || (c == ' ' && line_idx == 1))
#else
                  if (c == '#')
#endif
                    {
                      state = 1;
                      idx = 0;
                      buffer[idx++] = c;
                    }
                  break;
                case 1:
                  if (idx < BUFSIZE)
                    buffer[idx++] = c;
                  if ((c == '\n') || (c == '\r'))
                    {
                      buffer[idx] = 0;
                      _g_fprintf (stdout, "%s", buffer);
                      state = 0;
                      idx = 0;
#ifdef USE_LLDB
                      line_idx = 0;
#endif
                    }
                  break;
                default:
                  break;
                }
            }
        }
      else if (stack_trace_done)
        break;
    }

  close (in_fd[0]);
  close (in_fd[1]);
  close (out_fd[0]);
  close (out_fd[1]);
  _exit (0);
}

#endif /* !G_OS_WIN32 */
