/* glib-unixprivate.h - Unix specific integration private functions
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

#ifndef __G_UNIXPRIVATE_H__
#define __G_UNIXPRIVATE_H__

#include "config.h"

#ifndef G_OS_UNIX
#error "This header may only be used on UNIX"
#endif

/* To make bionic export pipe2() */
#ifndef _GNU_SOURCE
#define _GNU_SOURCE 1
#endif

#include "gmacros.h"
#include "gtypes.h"

#include <errno.h>
#include <fcntl.h>
#include <unistd.h>

G_BEGIN_DECLS

static inline gboolean
g_unix_open_pipe_internal (int *fds,
                           gboolean close_on_exec,
                           gboolean nonblock)
  {
#ifdef HAVE_PIPE2
  do
    {
      int ecode;
      int flags = 0;

      if (close_on_exec)
        flags |= O_CLOEXEC;
      if (nonblock)
        flags |= O_NONBLOCK;

      /* Atomic */
      ecode = pipe2 (fds, flags);
      if (ecode == -1 && errno != ENOSYS)
        return FALSE;
      else if (ecode == 0)
        return TRUE;
      /* Fall through on -ENOSYS, we must be running on an old kernel */
    }
  while (FALSE);
#endif

  if (pipe (fds) == -1)
    return FALSE;

  if (close_on_exec)
    {
      if (fcntl (fds[0], F_SETFD, FD_CLOEXEC) == -1 ||
          fcntl (fds[1], F_SETFD, FD_CLOEXEC) == -1)
        {
          int saved_errno = errno;

          close (fds[0]);
          close (fds[1]);
          fds[0] = -1;
          fds[1] = -1;

          errno = saved_errno;
          return FALSE;
        }
    }

  if (nonblock)
    {
#ifdef O_NONBLOCK
      int flags = O_NONBLOCK;
#else
      int flags = O_NDELAY;
#endif

      if (fcntl (fds[0], F_SETFL, flags) == -1 ||
          fcntl (fds[1], F_SETFL, flags) == -1)
        {
          int saved_errno = errno;

          close (fds[0]);
          close (fds[1]);
          fds[0] = -1;
          fds[1] = -1;

          errno = saved_errno;
          return FALSE;
        }
    }

  return TRUE;
}

G_END_DECLS

#endif  /* __G_UNIXPRIVATE_H__ */
