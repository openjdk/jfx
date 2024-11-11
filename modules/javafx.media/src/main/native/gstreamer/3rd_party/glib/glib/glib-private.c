/* GLIB - Library of useful routines for C programming
 * Copyright (C) 2011 Red Hat, Inc.
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
 * Author: Colin Walters <walters@verbum.org>
 */

#include "config.h"

#include "glib-private.h"
#include "glib-init.h"
#include "gutilsprivate.h"
#include "gdatasetprivate.h"

#ifdef USE_INVALID_PARAMETER_HANDLER
#include <crtdbg.h>
#endif

/**
 * glib__private__:
 * @arg: Do not use this argument
 *
 * Do not call this function; it is used to share private
 * API between glib, gobject, and gio.
 */
const GLibPrivateVTable *
glib__private__ (void)
{
  static const GLibPrivateVTable table = {
    g_wakeup_new,
    g_wakeup_free,
    g_wakeup_get_pollfd,
    g_wakeup_signal,
    g_wakeup_acknowledge,

    g_get_worker_context,

    g_check_setuid,
    g_main_context_new_with_next_id,

    g_dir_open_with_errno,
    g_dir_new_from_dirp,

    glib_init,

#ifdef G_OS_WIN32
    g_win32_stat_utf8,
    g_win32_lstat_utf8,
    g_win32_readlink_utf8,
    g_win32_fstat,
    g_win32_find_helper_executable_path,
    g_win32_reopen_noninherited,
    g_win32_handle_is_socket,
#endif

    g_win32_push_empty_invalid_parameter_handler,
    g_win32_pop_invalid_parameter_handler,

    g_find_program_for_path,

    g_uri_get_default_scheme_port,

    g_set_prgname_once,

    g_datalist_id_update_atomic,
  };

  return &table;
}

#ifdef USE_INVALID_PARAMETER_HANDLER
/*
 * This is the (empty) invalid parameter handler
 * that is used for Visual C++ 2005 (and later) builds
 * so that we can use this instead of the system automatically
 * aborting the process, when calling _get_osfhandle(), isatty()
 * and _commit() (via g_fsync()) and so on with an invalid file
 * descriptor.
 *
 * This is necessary so that the gspawn helper and the test programs
 * will continue to run as expected, since we are purposely or
 * forced to use invalid FDs.
 *
 * Please see https://learn.microsoft.com/en-us/cpp/c-runtime-library/parameter-validation?view=msvc-170
 * for an explanation on this.
 */
static void
empty_invalid_parameter_handler (const wchar_t *expression,
                                 const wchar_t *function,
                                 const wchar_t *file,
                                 unsigned int   line,
                                 uintptr_t      pReserved)
{
}

/* fallback to _set_invalid_parameter_handler() if we don't have _set_thread_local_invalid_parameter_handler() */
#ifndef HAVE__SET_THREAD_LOCAL_INVALID_PARAMETER_HANDLER
# define _set_thread_local_invalid_parameter_handler _set_invalid_parameter_handler
#endif

#endif
/*
 * g_win32_push_empty_invalid_parameter_handler:
 * @handler: a possibly uninitialized GWin32InvalidParameterHandler
 */
void
g_win32_push_empty_invalid_parameter_handler (GWin32InvalidParameterHandler *handler)
{
#ifdef USE_INVALID_PARAMETER_HANDLER
  /* use the empty invalid parameter handler to override the default invalid parameter_handler */
  handler->pushed_handler = empty_invalid_parameter_handler;
  handler->old_handler = _set_thread_local_invalid_parameter_handler (handler->pushed_handler);

  /* Disable the message box for assertions. */
  handler->pushed_report_mode = 0;
  handler->prev_report_mode = _CrtSetReportMode(_CRT_ASSERT, handler->pushed_report_mode);
#endif
}

/*
 * g_win32_pop_invalid_parameter_handler:
 * @handler: a GWin32InvalidParameterHandler processed with
 * g_win32_push_empty_invalid_parameter_handler()
 */
void
g_win32_pop_invalid_parameter_handler (GWin32InvalidParameterHandler *handler)
{
#ifdef USE_INVALID_PARAMETER_HANDLER
  G_GNUC_UNUSED _invalid_parameter_handler popped_handler;
  G_GNUC_UNUSED int popped_report_mode;

  /* Restore previous/default invalid parameter handler, check the value returned matches the one we previously pushed */
  popped_handler = _set_thread_local_invalid_parameter_handler (handler->old_handler);
  g_return_if_fail (handler->pushed_handler == popped_handler);

  /* Restore the message box for assertions, check the value returned matches the one we previously pushed */
  popped_report_mode = _CrtSetReportMode(_CRT_ASSERT, handler->prev_report_mode);
  g_return_if_fail (handler->pushed_report_mode == popped_report_mode);
#endif
}
