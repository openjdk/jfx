/* GLIB - Library of useful routines for C programming
 *
 * gthreadprivate.h - GLib internal thread system related declarations.
 *
 *  Copyright (C) 2003 Sebastian Wilhelmi
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

#ifndef __G_THREADPRIVATE_H__
#define __G_THREADPRIVATE_H__

#include "config.h"

#include "deprecated/gthread.h"

typedef struct _GRealThread GRealThread;
struct  _GRealThread
{
  GThread thread;

  gint ref_count;
  gboolean ours;
  char name[16];
  gpointer retval;
};

/* system thread implementation (gthread-posix.c, gthread-win32.c) */

#if defined(HAVE_FUTEX) || defined(HAVE_FUTEX_TIME64)
#include <errno.h>
#include <linux/futex.h>
#include <sys/syscall.h>
#include <unistd.h>

#ifndef FUTEX_WAIT_PRIVATE
#define FUTEX_WAIT_PRIVATE FUTEX_WAIT
#define FUTEX_WAKE_PRIVATE FUTEX_WAKE
#endif

/* Wrapper macro to call `futex_time64` and/or `futex` with simple
 * parameters and without returning the return value.
 *
 * We expect futex to sometimes return EAGAIN due to the race
 * between the caller checking the current value and deciding to
 * do the futex op. To avoid splattering errno on success, we
 * restore the original errno if EAGAIN is seen. See also:
 *   https://gitlab.gnome.org/GNOME/glib/-/issues/3034
 *
 * If the `futex_time64` syscall does not exist (`ENOSYS`), we retry again
 * with the normal `futex` syscall. This can happen if newer kernel headers
 * are used than the kernel that is actually running.
 *
 * The `futex_time64` syscall is also skipped in favour of `futex` if the
 * Android runtime’s API level is lower than 30, as it’s blocked by seccomp
 * there and using it will cause the app to be terminated:
 *   https://android-review.googlesource.com/c/platform/bionic/+/1094758
 *   https://github.com/aosp-mirror/platform_bionic/commit/ee7bc3002dc3127faac110167d28912eb0e86a20
 *
 * This must not be called with a timeout parameter as that differs
 * in size between the two syscall variants!
 */
#if defined(HAVE_FUTEX) && defined(HAVE_FUTEX_TIME64)
#if defined(__ANDROID__)
#define g_futex_simple(uaddr, futex_op, ...)                                     \
  G_STMT_START                                                                   \
  {                                                                              \
    int saved_errno = errno;                                                     \
    int res = 0;                                                                 \
    if (__builtin_available (android 30, *))                                     \
      {                                                                          \
        res = syscall (__NR_futex_time64, uaddr, (gsize) futex_op, __VA_ARGS__); \
        if (res < 0 && errno == ENOSYS)                                          \
          {                                                                      \
            errno = saved_errno;                                                 \
            res = syscall (__NR_futex, uaddr, (gsize) futex_op, __VA_ARGS__);    \
          }                                                                      \
      }                                                                          \
    else                                                                         \
      {                                                                          \
        res = syscall (__NR_futex, uaddr, (gsize) futex_op, __VA_ARGS__);        \
      }                                                                          \
    if (res < 0 && errno == EAGAIN)                                              \
      {                                                                          \
        errno = saved_errno;                                                     \
      }                                                                          \
  }                                                                              \
  G_STMT_END
#else
#define g_futex_simple(uaddr, futex_op, ...)                                     \
  G_STMT_START                                                                   \
  {                                                                              \
    int saved_errno = errno;                                                     \
    int res = syscall (__NR_futex_time64, uaddr, (gsize) futex_op, __VA_ARGS__); \
    if (res < 0 && errno == ENOSYS)                                              \
      {                                                                          \
        errno = saved_errno;                                                     \
        res = syscall (__NR_futex, uaddr, (gsize) futex_op, __VA_ARGS__);        \
      }                                                                          \
    if (res < 0 && errno == EAGAIN)                                              \
      {                                                                          \
        errno = saved_errno;                                                     \
      }                                                                          \
  }                                                                              \
  G_STMT_END
#endif /* defined(__ANDROID__) */
#elif defined(HAVE_FUTEX_TIME64)
#define g_futex_simple(uaddr, futex_op, ...)                                     \
  G_STMT_START                                                                   \
  {                                                                              \
    int saved_errno = errno;                                                     \
    int res = syscall (__NR_futex_time64, uaddr, (gsize) futex_op, __VA_ARGS__); \
    if (res < 0 && errno == EAGAIN)                                              \
      {                                                                          \
        errno = saved_errno;                                                     \
      }                                                                          \
  }                                                                              \
  G_STMT_END
#elif defined(HAVE_FUTEX)
#define g_futex_simple(uaddr, futex_op, ...)                              \
  G_STMT_START                                                            \
  {                                                                       \
    int saved_errno = errno;                                              \
    int res = syscall (__NR_futex, uaddr, (gsize) futex_op, __VA_ARGS__); \
    if (res < 0 && errno == EAGAIN)                                       \
      {                                                                   \
        errno = saved_errno;                                              \
      }                                                                   \
  }                                                                       \
  G_STMT_END
#else /* !defined(HAVE_FUTEX) && !defined(HAVE_FUTEX_TIME64) */
#error "Neither __NR_futex nor __NR_futex_time64 are available"
#endif /* defined(HAVE_FUTEX) && defined(HAVE_FUTEX_TIME64) */

#endif

void            g_system_thread_wait            (GRealThread  *thread);

GRealThread *g_system_thread_new (GThreadFunc proxy,
                                  gulong stack_size,
                                  const char *name,
                                  GThreadFunc func,
                                  gpointer data,
                                  GError **error);
void            g_system_thread_free            (GRealThread  *thread);

G_NORETURN void g_system_thread_exit            (void);
void            g_system_thread_set_name        (const gchar  *name);
void            g_system_thread_get_name        (char         *buffer,
                                                 gsize         length);

/* gthread.c */
GThread *g_thread_new_internal (const gchar *name,
                                GThreadFunc proxy,
                                GThreadFunc func,
                                gpointer data,
                                gsize stack_size,
                                GError **error);

gpointer        g_thread_proxy                  (gpointer      thread);

guint           g_thread_n_created              (void);

gpointer        g_private_set_alloc0            (GPrivate       *key,
                                                 gsize           size);

#endif /* __G_THREADPRIVATE_H__ */
