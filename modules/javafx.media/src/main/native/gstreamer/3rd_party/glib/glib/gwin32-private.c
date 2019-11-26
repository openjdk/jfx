/* gwin32-private.c - private glib functions for gwin32.c
 *
 * Copyright 2019 Руслан Ижбулатов
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

/* Copy @cmdline into @debugger, and substitute @pid for `%p`
 * and @event for `%e`.
 * If @debugger_size (in bytes) is overflowed, return %FALSE.
 * Also returns %FALSE when `%` is followed by anything other
 * than `e` or `p`.
 */
static gboolean
_g_win32_subst_pid_and_event (char       *debugger,
                              gsize       debugger_size,
                              const char *cmdline,
                              DWORD       pid,
                              guintptr    event)
{
  gsize i = 0, dbg_i = 0;
/* These are integers, and they can't be longer than 20 characters
 * even when they are 64-bit and in decimal notation.
 * Use 30 just to be sure.
 */
#define STR_BUFFER_SIZE 30
  char pid_str[STR_BUFFER_SIZE] = {0};
  gsize pid_str_len;
  char event_str[STR_BUFFER_SIZE] = {0};
  gsize event_str_len;

  _snprintf_s (pid_str, STR_BUFFER_SIZE, G_N_ELEMENTS (pid_str), "%lu", pid);
  pid_str[G_N_ELEMENTS (pid_str) - 1] = 0;
  pid_str_len = strlen (pid_str);
  _snprintf_s (event_str, STR_BUFFER_SIZE, G_N_ELEMENTS (pid_str), "%Iu", event);
  event_str[G_N_ELEMENTS (pid_str) - 1] = 0;
  event_str_len = strlen (event_str);
#undef STR_BUFFER_SIZE

  while (cmdline[i] != 0 && dbg_i < debugger_size)
    {
      if (cmdline[i] != '%')
        debugger[dbg_i++] = cmdline[i++];
      else if (cmdline[i + 1] == 'p')
        {
          gsize j = 0;
          while (j < pid_str_len && dbg_i < debugger_size)
            debugger[dbg_i++] = pid_str[j++];
          i += 2;
        }
      else if (cmdline[i + 1] == 'e')
        {
          gsize j = 0;
          while (j < event_str_len && dbg_i < debugger_size)
            debugger[dbg_i++] = event_str[j++];
          i += 2;
        }
      else
        return FALSE;
    }
  if (dbg_i < debugger_size)
    debugger[dbg_i] = 0;
  else
    return FALSE;

  return TRUE;
}
