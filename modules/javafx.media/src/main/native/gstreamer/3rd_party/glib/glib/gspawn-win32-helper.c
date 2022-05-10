/* gspawn-win32-helper.c - Helper program for process launching on Win32.
 *
 *  Copyright 2000 Red Hat, Inc.
 *  Copyright 2000 Tor Lillqvist
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

#include <fcntl.h>

/* For _CrtSetReportMode, we don't want Windows CRT (2005 and later)
 * to terminate the process if a bad file descriptor is passed into
 * _get_osfhandle().  This is necessary because we use _get_osfhandle()
 * to check the validity of the fd before we try to call close() on
 * it as attempting to close an invalid fd will cause the Windows CRT
 * to abort() this program internally.
 *
 * Please see http://msdn.microsoft.com/zh-tw/library/ks2530z6%28v=vs.80%29.aspx
 * for an explanation on this.
 */
#if (defined (_MSC_VER) && _MSC_VER >= 1400)
#include <crtdbg.h>
#endif

#undef G_LOG_DOMAIN
#include "glib.h"
#define GSPAWN_HELPER
#include "gspawn-win32.c"       /* For shared definitions */


static void
write_err_and_exit (gint    fd,
        gintptr msg)
{
  gintptr en = errno;

  write (fd, &msg, sizeof(gintptr));
  write (fd, &en, sizeof(gintptr));

  _exit (1);
}

#ifdef __GNUC__
#  ifndef _stdcall
#    define _stdcall  __attribute__((stdcall))
#  endif
#endif

/* We build gspawn-win32-helper.exe as a Windows GUI application
 * to avoid any temporarily flashing console windows in case
 * the gspawn function is invoked by a GUI program. Thus, no main()
 * but a WinMain().
 */

/* Copy of protect_argv that handles wchar_t strings */

static gint
protect_wargv (gint       argc,
         wchar_t  **wargv,
         wchar_t ***new_wargv)
{
  gint i;

  *new_wargv = g_new (wchar_t *, argc+1);

  /* Quote each argv element if necessary, so that it will get
   * reconstructed correctly in the C runtime startup code.  Note that
   * the unquoting algorithm in the C runtime is really weird, and
   * rather different than what Unix shells do. See stdargv.c in the C
   * runtime sources (in the Platform SDK, in src/crt).
   *
   * Note that a new_wargv[0] constructed by this function should
   * *not* be passed as the filename argument to a _wspawn* or _wexec*
   * family function. That argument should be the real file name
   * without any quoting.
   */
  for (i = 0; i < argc; i++)
    {
      wchar_t *p = wargv[i];
      wchar_t *q;
      gint len = 0;
      gint pre_bslash = 0;
      gboolean need_dblquotes = FALSE;
      while (*p)
  {
    if (*p == ' ' || *p == '\t')
      need_dblquotes = TRUE;
      /* estimate max len, assuming that all escapable characters will be escaped */
    if (*p == '"' || *p == '\\')
      len += 2;
    else
      len += 1;
    p++;
  }

      q = (*new_wargv)[i] = g_new (wchar_t, len + need_dblquotes*2 + 1);
      p = wargv[i];

      if (need_dblquotes)
  *q++ = '"';

      /* Only quotes and backslashes preceding quotes are escaped:
       * see "Parsing C Command-Line Arguments" at
       * https://docs.microsoft.com/en-us/cpp/c-language/parsing-c-command-line-arguments
       */
      while (*p)
  {
    if (*p == '"')
      {
        /* Add backslash for escaping quote itself */
        *q++ = '\\';
          /* Add backslash for every preceding backslash for escaping it */
        for (;pre_bslash > 0; --pre_bslash)
    *q++ = '\\';
      }

      /* Count length of continuous sequence of preceding backslashes. */
    if (*p == '\\')
      ++pre_bslash;
    else
      pre_bslash = 0;

    *q++ = *p;
    p++;
  }

      if (need_dblquotes)
  {
      /* Add backslash for every preceding backslash for escaping it,
           * do NOT escape quote itself.
           */
    for (;pre_bslash > 0; --pre_bslash)
      *q++ = '\\';
    *q++ = '"';
  }
      *q++ = '\0';
    }
  (*new_wargv)[argc] = NULL;

  return argc;
}

static int
checked_dup2 (int oldfd, int newfd, int report_fd)
{
  if (oldfd == newfd)
    return newfd;

  if (dup2 (oldfd, newfd) == -1)
    write_err_and_exit (report_fd, CHILD_DUP_FAILED);

  return newfd;
}

#if (defined (_MSC_VER) && _MSC_VER >= 1400)
/*
 * This is the (empty) invalid parameter handler
 * that is used for Visual C++ 2005 (and later) builds
 * so that we can use this instead of the system automatically
 * aborting the process.
 *
 * This is necessary as we use _get_oshandle() to check the validity
 * of the file descriptors as we close them, so when an invalid file
 * descriptor is passed into that function as we check on it, we get
 * -1 as the result, instead of the gspawn helper program aborting.
 *
 * Please see http://msdn.microsoft.com/zh-tw/library/ks2530z6%28v=vs.80%29.aspx
 * for an explanation on this.
 */
extern void
myInvalidParameterHandler(const wchar_t *expression,
                          const wchar_t *function,
                          const wchar_t *file,
                          unsigned int   line,
                          uintptr_t      pReserved);
#endif

#ifndef GSTREAMER_LITE
#ifndef HELPER_CONSOLE
int _stdcall
WinMain (struct HINSTANCE__ *hInstance,
   struct HINSTANCE__ *hPrevInstance,
   char               *lpszCmdLine,
   int                 nCmdShow)
#else
int
main (int ignored_argc, char **ignored_argv)
#endif
{
  GHashTable *fds;  /* (element-type int int) */
  int child_err_report_fd = -1;
  int helper_sync_fd = -1;
  int saved_stderr_fd = -1;
  int i;
  int fd;
  int mode;
  int maxfd = 2;
  gintptr handle;
  int saved_errno;
  gintptr no_error = CHILD_NO_ERROR;
  gint argv_zero_offset = ARG_PROGRAM;
  wchar_t **new_wargv;
  int argc;
  char **argv;
  wchar_t **wargv;
  char c;

#if (defined (_MSC_VER) && _MSC_VER >= 1400)
  /* set up our empty invalid parameter handler */
  _invalid_parameter_handler oldHandler, newHandler;
  newHandler = myInvalidParameterHandler;
  oldHandler = _set_invalid_parameter_handler(newHandler);

  /* Disable the message box for assertions. */
  _CrtSetReportMode(_CRT_ASSERT, 0);
#endif

  /* Fetch the wide-char argument vector */
  wargv = CommandLineToArgvW (GetCommandLineW(), &argc);

  g_assert (argc >= ARG_COUNT);

  /* Convert unicode wargs to utf8 */
  argv = g_new(char *, argc + 1);
  for (i = 0; i < argc; i++)
    argv[i] = g_utf16_to_utf8(wargv[i], -1, NULL, NULL, NULL);
  argv[i] = NULL;

  /* argv[ARG_CHILD_ERR_REPORT] is the file descriptor number onto
   * which write error messages.
   */
  child_err_report_fd = atoi (argv[ARG_CHILD_ERR_REPORT]);
  maxfd = MAX (child_err_report_fd, maxfd);

  /* Hack to implement G_SPAWN_FILE_AND_ARGV_ZERO. If
   * argv[ARG_CHILD_ERR_REPORT] is suffixed with a '#' it means we get
   * the program to run and its argv[0] separately.
   */
  if (argv[ARG_CHILD_ERR_REPORT][strlen (argv[ARG_CHILD_ERR_REPORT]) - 1] == '#')
    argv_zero_offset++;

  /* argv[ARG_HELPER_SYNC] is the file descriptor number we read a
   * byte that tells us it is OK to exit. We have to wait until the
   * parent allows us to exit, so that the parent has had time to
   * duplicate the process handle we sent it. Duplicating a handle
   * from another process works only if that other process exists.
   */
  helper_sync_fd = atoi (argv[ARG_HELPER_SYNC]);
  maxfd = MAX (helper_sync_fd, maxfd);

  /* argv[ARG_STDIN..ARG_STDERR] are the file descriptor numbers that
   * should be dup2'd to 0, 1 and 2. '-' if the corresponding fd
   * should be left alone, and 'z' if it should be connected to the
   * bit bucket NUL:.
   */
  if (argv[ARG_STDIN][0] == '-')
    ; /* Nothing */
  else if (argv[ARG_STDIN][0] == 'z')
    {
      fd = open ("NUL:", O_RDONLY);
      checked_dup2 (fd, 0, child_err_report_fd);
  }
  else
    {
      fd = atoi (argv[ARG_STDIN]);
      checked_dup2 (fd, 0, child_err_report_fd);
  }

  if (argv[ARG_STDOUT][0] == '-')
    ; /* Nothing */
  else if (argv[ARG_STDOUT][0] == 'z')
    {
      fd = open ("NUL:", O_WRONLY);
      checked_dup2 (fd, 1, child_err_report_fd);
  }
  else
    {
      fd = atoi (argv[ARG_STDOUT]);
      checked_dup2 (fd, 1, child_err_report_fd);
  }

  saved_stderr_fd = reopen_noninherited (dup (2), _O_WRONLY);
  if (saved_stderr_fd == -1)
    write_err_and_exit (child_err_report_fd, CHILD_DUP_FAILED);

  maxfd = MAX (saved_stderr_fd, maxfd);
  if (argv[ARG_STDERR][0] == '-')
    ; /* Nothing */
  else if (argv[ARG_STDERR][0] == 'z')
    {
      fd = open ("NUL:", O_WRONLY);
      checked_dup2 (fd, 2, child_err_report_fd);
    }
  else
    {
      fd = atoi (argv[ARG_STDERR]);
      checked_dup2 (fd, 2, child_err_report_fd);
    }

  /* argv[ARG_WORKING_DIRECTORY] is the directory in which to run the
   * process.  If "-", don't change directory.
   */
  if (argv[ARG_WORKING_DIRECTORY][0] == '-' &&
      argv[ARG_WORKING_DIRECTORY][1] == 0)
    ; /* Nothing */
  else if (_wchdir (wargv[ARG_WORKING_DIRECTORY]) < 0)
    write_err_and_exit (child_err_report_fd, CHILD_CHDIR_FAILED);

  fds = g_hash_table_new (NULL, NULL);
  if (argv[ARG_FDS][0] != '-')
    {
      gchar **fdsv = g_strsplit (argv[ARG_FDS], ",", -1);
      gsize i;

      for (i = 0; fdsv[i]; i++)
        {
          char *endptr = NULL;
          int sourcefd, targetfd;
          gint64 val;

          val = g_ascii_strtoll (fdsv[i], &endptr, 10);
          g_assert (val <= G_MAXINT32);
          sourcefd = val;
          g_assert (endptr != fdsv[i]);
          g_assert (*endptr == ':');
          val = g_ascii_strtoll (endptr + 1, &endptr, 10);
          targetfd = val;
          g_assert (val <= G_MAXINT32);
          g_assert (*endptr == '\0');

          maxfd = MAX (maxfd, sourcefd);
          maxfd = MAX (maxfd, targetfd);

          g_hash_table_insert (fds, GINT_TO_POINTER (targetfd), GINT_TO_POINTER (sourcefd));
        }

      g_strfreev (fdsv);
    }

  maxfd++;
  child_err_report_fd = checked_dup2 (child_err_report_fd, maxfd, child_err_report_fd);
  maxfd++;
  helper_sync_fd = checked_dup2 (helper_sync_fd, maxfd, child_err_report_fd);
  maxfd++;
  saved_stderr_fd = checked_dup2 (saved_stderr_fd, maxfd, child_err_report_fd);

  {
    GHashTableIter iter;
    gpointer sourcefd, targetfd;

    g_hash_table_iter_init (&iter, fds);
    while (g_hash_table_iter_next (&iter, &targetfd, &sourcefd))
      {
        /* If we're doing remapping fd assignments, we need to handle
         * the case where the user has specified e.g. 5 -> 4, 4 -> 6.
         * We do this by duping all source fds, taking care to ensure the new
         * fds are larger than any target fd to avoid introducing new conflicts.
         */
        maxfd++;
        checked_dup2 (GPOINTER_TO_INT (sourcefd), maxfd, child_err_report_fd);
        g_hash_table_iter_replace (&iter, GINT_TO_POINTER (maxfd));
      }

    g_hash_table_iter_init (&iter, fds);
    while (g_hash_table_iter_next (&iter, &targetfd, &sourcefd))
      checked_dup2 (GPOINTER_TO_INT (sourcefd), GPOINTER_TO_INT (targetfd), child_err_report_fd);
  }

  g_hash_table_add (fds, GINT_TO_POINTER (child_err_report_fd));
  g_hash_table_add (fds, GINT_TO_POINTER (helper_sync_fd));
  g_hash_table_add (fds, GINT_TO_POINTER (saved_stderr_fd));

  /* argv[ARG_CLOSE_DESCRIPTORS] is "y" if file descriptors from 3
   *  upwards should be closed
   */
  if (argv[ARG_CLOSE_DESCRIPTORS][0] == 'y')
    for (i = 3; i < 1000; i++)  /* FIXME real limit? */
      if (!g_hash_table_contains (fds, GINT_TO_POINTER (i)))
        if (_get_osfhandle (i) != -1)
          close (i);

  /* We don't want our child to inherit the error report and
   * helper sync fds.
   */
  child_err_report_fd = reopen_noninherited (child_err_report_fd, _O_WRONLY);
  helper_sync_fd = reopen_noninherited (helper_sync_fd, _O_RDONLY);
  if (helper_sync_fd == -1)
    write_err_and_exit (child_err_report_fd, CHILD_DUP_FAILED);

  /* argv[ARG_WAIT] is "w" to wait for the program to exit */
  if (argv[ARG_WAIT][0] == 'w')
    mode = P_WAIT;
  else
    mode = P_NOWAIT;

  /* argv[ARG_USE_PATH] is "y" to use PATH, otherwise not */

  /* argv[ARG_PROGRAM] is executable file to run,
   * argv[argv_zero_offset]... is its argv. argv_zero_offset equals
   * ARG_PROGRAM unless G_SPAWN_FILE_AND_ARGV_ZERO was used, in which
   * case we have a separate executable name and argv[0].
   */

  /* For the program name passed to spawnv(), don't use the quoted
   * version.
   */
  protect_wargv (argc - argv_zero_offset, wargv + argv_zero_offset, &new_wargv);

  if (argv[ARG_USE_PATH][0] == 'y')
    handle = _wspawnvp (mode, wargv[ARG_PROGRAM], (const wchar_t **) new_wargv);
  else
    handle = _wspawnv (mode, wargv[ARG_PROGRAM], (const wchar_t **) new_wargv);

  saved_errno = errno;

  /* Some coverage warnings may be printed on stderr during this process exit.
   * Remove redirection so that they would go to original stderr
   * instead of being treated as part of stderr of child process.
   */
  dup2 (saved_stderr_fd, 2);
  if (handle == -1 && saved_errno != 0)
    {
      int ec = (saved_errno == ENOENT)
          ? CHILD_SPAWN_NOENT
          : CHILD_SPAWN_FAILED;
      write_err_and_exit (child_err_report_fd, ec);
    }

  write (child_err_report_fd, &no_error, sizeof (no_error));
  write (child_err_report_fd, &handle, sizeof (handle));

  read (helper_sync_fd, &c, 1);

  LocalFree (wargv);
  g_strfreev (argv);
  g_hash_table_unref (fds);

  return 0;
}
#endif // GSTREAMER_LITE
