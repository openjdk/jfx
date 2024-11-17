/* gstdioprivate.h - Private GLib stdio functions
 *
 * Copyright 2017 Руслан Ижбулатов
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

#ifndef __G_STDIOPRIVATE_H__
#define __G_STDIOPRIVATE_H__

G_BEGIN_DECLS

#if defined (G_OS_WIN32)

typedef struct _gtimespec {
  guint64 tv_sec;
  guint32 tv_nsec;
} gtimespec;

struct _GWin32PrivateStat
{
  guint32 volume_serial;
  guint64 file_index;
  guint64 attributes;
  guint64 allocated_size;
  guint32 reparse_tag;

  guint32 st_dev;
  guint32 st_ino;
  guint16 st_mode;
  guint16 st_uid;
  guint16 st_gid;
  guint32 st_nlink;
  guint64 st_size;
  gtimespec st_ctim;
  gtimespec st_atim;
  gtimespec st_mtim;
};

typedef struct _GWin32PrivateStat GWin32PrivateStat;

int g_win32_stat_utf8     (const gchar       *filename,
                           GWin32PrivateStat *buf);

int g_win32_lstat_utf8    (const gchar       *filename,
                           GWin32PrivateStat *buf);

int g_win32_readlink_utf8 (const gchar       *filename,
                           gchar             *buf,
                           gsize              buf_size,
                           gchar            **alloc_buf,
                           gboolean           terminate);

int g_win32_fstat         (int                fd,
                           GWin32PrivateStat *buf);

#endif
/* The POSIX standard specifies that if close() fails with EINTR the
 * file descriptor may or may not be in fact closed. Since another
 * thread might have already reused the FD if it was in fact closed
 * either a test of FD to ensure that it's closed nor a second
 * call to close() may indicate the wrong FD, so the error must be
 * ignored.
 *
 * However, since Mac OS X 10.5 (Leopard) Apple provdes a hidden
 * implementation of close that doesn't allow another thread
 * to cancel the close so it never fails with EINTR.
 *
 * The official way to enable this is to set __DARWIN_NON_CANCELABLE
 * in the build, but that applies to all system calls, not just
 * close(). Following Chromium's example (see
 * https://chromium.googlesource.com/chromium/src/base/+/refs/heads/main/mac/close_nocancel.cc )
 * we choose to expose and use the hidden close variant only.
 */
#ifdef __APPLE__
#include <sys/cdefs.h>
#include <unistd.h>
# if !__DARWIN_NON_CANCELABLE
#  if !__DARWIN_ONLY_UNIX_CONFORMANCE
#   define close close$NOCANCEL$UNIX2003
int close$NOCANCEL$UNIX2003 (int fd);
#  else
#   define close close$NOCANCEL
int close$NOCANCEL (int fd);
#  endif
# endif
#endif
G_END_DECLS

#endif /* __G_STDIOPRIVATE_H__ */
