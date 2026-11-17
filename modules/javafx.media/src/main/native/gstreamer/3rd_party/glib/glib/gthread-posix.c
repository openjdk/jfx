/* GLIB - Library of useful routines for C programming
 * Copyright (C) 1995-1997  Peter Mattis, Spencer Kimball and Josh MacDonald
 *
 * gthread.c: posix thread system implementation
 * Copyright 1998 Sebastian Wilhelmi; University of Karlsruhe
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

/* The GMutex, GCond and GPrivate implementations in this file are some
 * of the lowest-level code in GLib.  All other parts of GLib (messages,
 * memory, slices, etc) assume that they can freely use these facilities
 * without risking recursion.
 *
 * As such, these functions are NOT permitted to call any other part of
 * GLib.
 *
 * The thread manipulation functions (create, exit, join, etc.) have
 * more freedom -- they can do as they please.
 */

#include "config.h"

#include "gthread.h"

#include "gmain.h"
#include "gmessages.h"
#include "gslice.h"
#include "gstrfuncs.h"
#include "gtestutils.h"
#include "gthreadprivate.h"
#include "gutils.h"

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <pthread.h>

#include <sys/time.h>
#include <unistd.h>

#ifdef HAVE_PTHREAD_SET_NAME_NP
#include <pthread_np.h>
#endif
#ifdef HAVE_SCHED_H
#include <sched.h>
#endif
#ifdef G_OS_WIN32
#include <windows.h>
#endif

#if defined(HAVE_SYS_SCHED_GETATTR)
#include <sys/syscall.h>
#endif

#if (defined(HAVE_FUTEX) || defined(HAVE_FUTEX_TIME64)) && \
    (defined(HAVE_STDATOMIC_H) || defined(__ATOMIC_SEQ_CST))
#define USE_NATIVE_MUTEX
#endif

static void
g_thread_abort (gint         status,
                const gchar *function)
{
  fprintf (stderr, "GLib (gthread-posix.c): Unexpected error from C library during '%s': %s.  Aborting.\n",
           function, strerror (status));
  g_abort ();
}

/* {{{1 GMutex */

#if !defined(USE_NATIVE_MUTEX)

static pthread_mutex_t *
g_mutex_impl_new (void)
{
  pthread_mutexattr_t *pattr = NULL;
  pthread_mutex_t *mutex;
  gint status;
#ifdef PTHREAD_ADAPTIVE_MUTEX_INITIALIZER_NP
  pthread_mutexattr_t attr;
#endif

  mutex = malloc (sizeof (pthread_mutex_t));
  if G_UNLIKELY (mutex == NULL)
    g_thread_abort (errno, "malloc");

#ifdef PTHREAD_ADAPTIVE_MUTEX_INITIALIZER_NP
  pthread_mutexattr_init (&attr);
  pthread_mutexattr_settype (&attr, PTHREAD_MUTEX_ADAPTIVE_NP);
  pattr = &attr;
#endif

  if G_UNLIKELY ((status = pthread_mutex_init (mutex, pattr)) != 0)
    g_thread_abort (status, "pthread_mutex_init");

#ifdef PTHREAD_ADAPTIVE_MUTEX_INITIALIZER_NP
  pthread_mutexattr_destroy (&attr);
#endif

  return mutex;
}

static void
g_mutex_impl_free (pthread_mutex_t *mutex)
{
  pthread_mutex_destroy (mutex);
  free (mutex);
}

static inline pthread_mutex_t *
g_mutex_get_impl (GMutex *mutex)
{
  pthread_mutex_t *impl = g_atomic_pointer_get (&mutex->p);

  if G_UNLIKELY (impl == NULL)
    {
      impl = g_mutex_impl_new ();
      if (!g_atomic_pointer_compare_and_exchange (&mutex->p, NULL, impl))
        g_mutex_impl_free (impl);
      impl = mutex->p;
    }

  return impl;
}


G_ALWAYS_INLINE static inline void
g_mutex_init_impl (GMutex *mutex)
{
  mutex->p = g_mutex_impl_new ();
}

G_ALWAYS_INLINE static inline void
g_mutex_clear_impl (GMutex *mutex)
{
  g_mutex_impl_free (mutex->p);
}

G_ALWAYS_INLINE static inline void
g_mutex_lock_impl (GMutex *mutex)
{
  gint status;

  if G_UNLIKELY ((status = pthread_mutex_lock (g_mutex_get_impl (mutex))) != 0)
    g_thread_abort (status, "pthread_mutex_lock");
}

G_ALWAYS_INLINE static inline void
g_mutex_unlock_impl (GMutex *mutex)
{
  gint status;

  if G_UNLIKELY ((status = pthread_mutex_unlock (g_mutex_get_impl (mutex))) != 0)
    g_thread_abort (status, "pthread_mutex_unlock");
}

G_ALWAYS_INLINE static inline gboolean
g_mutex_trylock_impl (GMutex *mutex)
{
  gint status;

  if G_LIKELY ((status = pthread_mutex_trylock (g_mutex_get_impl (mutex))) == 0)
    return TRUE;

  if G_UNLIKELY (status != EBUSY)
    g_thread_abort (status, "pthread_mutex_trylock");

  return FALSE;
}

#endif /* !defined(USE_NATIVE_MUTEX) */

/* {{{1 GRecMutex */

static pthread_mutex_t *
g_rec_mutex_impl_new (void)
{
  pthread_mutexattr_t attr;
  pthread_mutex_t *mutex;

  mutex = malloc (sizeof (pthread_mutex_t));
  if G_UNLIKELY (mutex == NULL)
    g_thread_abort (errno, "malloc");

  pthread_mutexattr_init (&attr);
  pthread_mutexattr_settype (&attr, PTHREAD_MUTEX_RECURSIVE);
  pthread_mutex_init (mutex, &attr);
  pthread_mutexattr_destroy (&attr);

  return mutex;
}

static void
g_rec_mutex_impl_free (pthread_mutex_t *mutex)
{
  pthread_mutex_destroy (mutex);
  free (mutex);
}

static inline pthread_mutex_t *
g_rec_mutex_get_impl (GRecMutex *rec_mutex)
{
  pthread_mutex_t *impl = g_atomic_pointer_get (&rec_mutex->p);

  if G_UNLIKELY (impl == NULL)
    {
      impl = g_rec_mutex_impl_new ();
      if (!g_atomic_pointer_compare_and_exchange (&rec_mutex->p, NULL, impl))
        g_rec_mutex_impl_free (impl);
      impl = rec_mutex->p;
    }

  return impl;
}

G_ALWAYS_INLINE static inline void
g_rec_mutex_init_impl (GRecMutex *rec_mutex)
{
  rec_mutex->p = g_rec_mutex_impl_new ();
}

G_ALWAYS_INLINE static inline void
g_rec_mutex_clear_impl (GRecMutex *rec_mutex)
{
  g_rec_mutex_impl_free (rec_mutex->p);
}

G_ALWAYS_INLINE static inline void
g_rec_mutex_lock_impl (GRecMutex *mutex)
{
  pthread_mutex_lock (g_rec_mutex_get_impl (mutex));
}

G_ALWAYS_INLINE static inline void
g_rec_mutex_unlock_impl (GRecMutex *rec_mutex)
{
  pthread_mutex_unlock (rec_mutex->p);
}

G_ALWAYS_INLINE static inline gboolean
g_rec_mutex_trylock_impl (GRecMutex *rec_mutex)
{
  if (pthread_mutex_trylock (g_rec_mutex_get_impl (rec_mutex)) != 0)
    return FALSE;

  return TRUE;
}

/* {{{1 GRWLock */

static pthread_rwlock_t *
g_rw_lock_impl_new (void)
{
  pthread_rwlock_t *rwlock;
  gint status;

  rwlock = malloc (sizeof (pthread_rwlock_t));
  if G_UNLIKELY (rwlock == NULL)
    g_thread_abort (errno, "malloc");

  if G_UNLIKELY ((status = pthread_rwlock_init (rwlock, NULL)) != 0)
    g_thread_abort (status, "pthread_rwlock_init");

  return rwlock;
}

static void
g_rw_lock_impl_free (pthread_rwlock_t *rwlock)
{
  pthread_rwlock_destroy (rwlock);
  free (rwlock);
}

static inline pthread_rwlock_t *
g_rw_lock_get_impl (GRWLock *lock)
{
  pthread_rwlock_t *impl = g_atomic_pointer_get (&lock->p);

  if G_UNLIKELY (impl == NULL)
    {
      impl = g_rw_lock_impl_new ();
      if (!g_atomic_pointer_compare_and_exchange (&lock->p, NULL, impl))
        g_rw_lock_impl_free (impl);
      impl = lock->p;
    }

  return impl;
}

G_ALWAYS_INLINE static inline void
g_rw_lock_init_impl (GRWLock *rw_lock)
{
  rw_lock->p = g_rw_lock_impl_new ();
}

G_ALWAYS_INLINE static inline void
g_rw_lock_clear_impl (GRWLock *rw_lock)
{
  g_rw_lock_impl_free (rw_lock->p);
}

G_ALWAYS_INLINE static inline void
g_rw_lock_writer_lock_impl (GRWLock *rw_lock)
{
  int retval = pthread_rwlock_wrlock (g_rw_lock_get_impl (rw_lock));

  if (retval != 0)
    g_critical ("Failed to get RW lock %p: %s", rw_lock, g_strerror (retval));
}

G_ALWAYS_INLINE static inline gboolean
g_rw_lock_writer_trylock_impl (GRWLock *rw_lock)
{
  if (pthread_rwlock_trywrlock (g_rw_lock_get_impl (rw_lock)) != 0)
    return FALSE;

  return TRUE;
}

G_ALWAYS_INLINE static inline void
g_rw_lock_writer_unlock_impl (GRWLock *rw_lock)
{
  pthread_rwlock_unlock (g_rw_lock_get_impl (rw_lock));
}

G_ALWAYS_INLINE static inline void
g_rw_lock_reader_lock_impl (GRWLock *rw_lock)
{
  int retval = pthread_rwlock_rdlock (g_rw_lock_get_impl (rw_lock));

  if (retval != 0)
    g_critical ("Failed to get RW lock %p: %s", rw_lock, g_strerror (retval));
}

G_ALWAYS_INLINE static inline gboolean
g_rw_lock_reader_trylock_impl (GRWLock *rw_lock)
{
  if (pthread_rwlock_tryrdlock (g_rw_lock_get_impl (rw_lock)) != 0)
    return FALSE;

  return TRUE;
}

G_ALWAYS_INLINE static inline void
g_rw_lock_reader_unlock_impl (GRWLock *rw_lock)
{
  pthread_rwlock_unlock (g_rw_lock_get_impl (rw_lock));
}

/* {{{1 GCond */

#if !defined(USE_NATIVE_MUTEX)

static pthread_cond_t *
g_cond_impl_new (void)
{
  pthread_condattr_t attr;
  pthread_cond_t *cond;
  gint status;

  pthread_condattr_init (&attr);

#ifdef HAVE_PTHREAD_COND_TIMEDWAIT_RELATIVE_NP
#elif defined (HAVE_PTHREAD_CONDATTR_SETCLOCK) && defined (CLOCK_MONOTONIC)
  if G_UNLIKELY ((status = pthread_condattr_setclock (&attr, CLOCK_MONOTONIC)) != 0)
    g_thread_abort (status, "pthread_condattr_setclock");
#else
#error Cannot support GCond on your platform.
#endif

  cond = malloc (sizeof (pthread_cond_t));
  if G_UNLIKELY (cond == NULL)
    g_thread_abort (errno, "malloc");

  if G_UNLIKELY ((status = pthread_cond_init (cond, &attr)) != 0)
    g_thread_abort (status, "pthread_cond_init");

  pthread_condattr_destroy (&attr);

  return cond;
}

static void
g_cond_impl_free (pthread_cond_t *cond)
{
  pthread_cond_destroy (cond);
  free (cond);
}

static inline pthread_cond_t *
g_cond_get_impl (GCond *cond)
{
  pthread_cond_t *impl = g_atomic_pointer_get (&cond->p);

  if G_UNLIKELY (impl == NULL)
    {
      impl = g_cond_impl_new ();
      if (!g_atomic_pointer_compare_and_exchange (&cond->p, NULL, impl))
        g_cond_impl_free (impl);
      impl = cond->p;
    }

  return impl;
}

G_ALWAYS_INLINE static inline void
g_cond_init_impl (GCond *cond)
{
  cond->p = g_cond_impl_new ();
}

G_ALWAYS_INLINE static inline void
g_cond_clear_impl (GCond *cond)
{
  g_cond_impl_free (cond->p);
}

G_ALWAYS_INLINE static inline void
g_cond_wait_impl (GCond  *cond,
                  GMutex *mutex)
{
  gint status;

  if G_UNLIKELY ((status = pthread_cond_wait (g_cond_get_impl (cond), g_mutex_get_impl (mutex))) != 0)
    g_thread_abort (status, "pthread_cond_wait");
}

G_ALWAYS_INLINE static inline void
g_cond_signal_impl (GCond *cond)
{
  gint status;

  if G_UNLIKELY ((status = pthread_cond_signal (g_cond_get_impl (cond))) != 0)
    g_thread_abort (status, "pthread_cond_signal");
}

G_ALWAYS_INLINE static inline void
g_cond_broadcast_impl (GCond *cond)
{
  gint status;

  if G_UNLIKELY ((status = pthread_cond_broadcast (g_cond_get_impl (cond))) != 0)
    g_thread_abort (status, "pthread_cond_broadcast");
}

G_ALWAYS_INLINE static inline gboolean
g_cond_wait_until_impl (GCond  *cond,
                        GMutex *mutex,
                        gint64  end_time)
{
  struct timespec ts;
  gint status;

#ifdef HAVE_PTHREAD_COND_TIMEDWAIT_RELATIVE_NP
  /* end_time is given relative to the monotonic clock as returned by
   * g_get_monotonic_time().
   *
   * Since this pthreads wants the relative time, convert it back again.
   */
  {
    gint64 now = g_get_monotonic_time ();
    gint64 relative;

    if (end_time <= now)
      return FALSE;

    relative = end_time - now;

    ts.tv_sec = relative / 1000000;
    ts.tv_nsec = (relative % 1000000) * 1000;

    if ((status = pthread_cond_timedwait_relative_np (g_cond_get_impl (cond), g_mutex_get_impl (mutex), &ts)) == 0)
      return TRUE;
  }
#elif defined (HAVE_PTHREAD_CONDATTR_SETCLOCK) && defined (CLOCK_MONOTONIC)
  /* This is the exact check we used during init to set the clock to
   * monotonic, so if we're in this branch, timedwait() will already be
   * expecting a monotonic clock.
   */
  {
    ts.tv_sec = end_time / 1000000;
    ts.tv_nsec = (end_time % 1000000) * 1000;

    if ((status = pthread_cond_timedwait (g_cond_get_impl (cond), g_mutex_get_impl (mutex), &ts)) == 0)
      return TRUE;
  }
#else
#error Cannot support GCond on your platform.
#endif

  if G_UNLIKELY (status != ETIMEDOUT)
    g_thread_abort (status, "pthread_cond_timedwait");

  return FALSE;
}

#endif /* defined(USE_NATIVE_MUTEX) */

/* {{{1 GPrivate */

static pthread_key_t *
g_private_impl_new (GDestroyNotify notify)
{
  pthread_key_t *key;
  gint status;

  key = malloc (sizeof (pthread_key_t));
  if G_UNLIKELY (key == NULL)
    g_thread_abort (errno, "malloc");
  status = pthread_key_create (key, notify);
  if G_UNLIKELY (status != 0)
    g_thread_abort (status, "pthread_key_create");

  return key;
}

static void
g_private_impl_free (pthread_key_t *key)
{
  gint status;

  status = pthread_key_delete (*key);
  if G_UNLIKELY (status != 0)
    g_thread_abort (status, "pthread_key_delete");
  free (key);
}

static gpointer
g_private_impl_new_direct (GDestroyNotify notify)
{
  gpointer impl = (void *) (gssize) -1;
  pthread_key_t key;
  gint status;

  status = pthread_key_create (&key, notify);
  if G_UNLIKELY (status != 0)
    g_thread_abort (status, "pthread_key_create");

  memcpy (&impl, &key, sizeof (pthread_key_t));

  /* pthread_key_create could theoretically put a NULL value into key.
   * If that happens, waste the result and create a new one, since we
   * use NULL to mean "not yet allocated".
   *
   * This will only happen once per program run.
   *
   * We completely avoid this problem for the case where pthread_key_t
   * is smaller than void* (for example, on 64 bit Linux) by putting
   * some high bits in the value of 'impl' to start with.  Since we only
   * overwrite part of the pointer, we will never end up with NULL.
   */
  if (sizeof (pthread_key_t) == sizeof (gpointer))
    {
      if G_UNLIKELY (impl == NULL)
        {
          status = pthread_key_create (&key, notify);
          if G_UNLIKELY (status != 0)
            g_thread_abort (status, "pthread_key_create");

          memcpy (&impl, &key, sizeof (pthread_key_t));

          if G_UNLIKELY (impl == NULL)
            g_thread_abort (status, "pthread_key_create (gave NULL result twice)");
        }
    }

  return impl;
}

static void
g_private_impl_free_direct (gpointer impl)
{
  pthread_key_t tmp;
  gint status;

  memcpy (&tmp, &impl, sizeof (pthread_key_t));

  status = pthread_key_delete (tmp);
  if G_UNLIKELY (status != 0)
    g_thread_abort (status, "pthread_key_delete");
}

static inline pthread_key_t
_g_private_get_impl (GPrivate *key)
{
  if (sizeof (pthread_key_t) > sizeof (gpointer))
    {
      pthread_key_t *impl = g_atomic_pointer_get (&key->p);

      if G_UNLIKELY (impl == NULL)
        {
          impl = g_private_impl_new (key->notify);
          if (!g_atomic_pointer_compare_and_exchange (&key->p, NULL, impl))
            {
              g_private_impl_free (impl);
              impl = key->p;
            }
        }

      return *impl;
    }
  else
    {
      gpointer impl = g_atomic_pointer_get (&key->p);
      pthread_key_t tmp;

      if G_UNLIKELY (impl == NULL)
        {
          impl = g_private_impl_new_direct (key->notify);
          if (!g_atomic_pointer_compare_and_exchange (&key->p, NULL, impl))
            {
              g_private_impl_free_direct (impl);
              impl = key->p;
            }
        }

      memcpy (&tmp, &impl, sizeof (pthread_key_t));

      return tmp;
    }
}

G_ALWAYS_INLINE static inline gpointer
g_private_get_impl (GPrivate *key)
{
  /* quote POSIX: No errors are returned from pthread_getspecific(). */
  return pthread_getspecific (_g_private_get_impl (key));
}

G_ALWAYS_INLINE static inline void
g_private_set_impl (GPrivate *key,
                    gpointer  value)
{
  gint status;

  if G_UNLIKELY ((status = pthread_setspecific (_g_private_get_impl (key), value)) != 0)
    g_thread_abort (status, "pthread_setspecific");
}

G_ALWAYS_INLINE static inline void
g_private_replace_impl (GPrivate *key,
                        gpointer  value)
{
  pthread_key_t impl = _g_private_get_impl (key);
  gpointer old;
  gint status;

  old = pthread_getspecific (impl);

  if G_UNLIKELY ((status = pthread_setspecific (impl, value)) != 0)
    g_thread_abort (status, "pthread_setspecific");

  if (old && key->notify)
    key->notify (old);
}

/* {{{1 GThread */

#define posix_check_err(err, name) G_STMT_START{      \
  int error = (err);              \
  if (error)                \
    g_error ("file %s: line %d (%s): error '%s' during '%s'",   \
           __FILE__, __LINE__, G_STRFUNC,       \
           g_strerror (error), name);         \
  }G_STMT_END

#define posix_check_cmd(cmd) posix_check_err (cmd, #cmd)

typedef struct
{
  GRealThread thread;

  pthread_t system_thread;
  gboolean  joined;
  GMutex    lock;

  void *(*proxy) (void *);
} GThreadPosix;

void
g_system_thread_free (GRealThread *thread)
{
  GThreadPosix *pt = (GThreadPosix *) thread;

  if (!pt->joined)
    pthread_detach (pt->system_thread);

  g_mutex_clear (&pt->lock);

  g_slice_free (GThreadPosix, pt);
}

GRealThread *
g_system_thread_new (GThreadFunc proxy,
                     gulong stack_size,
                     const char *name,
                     GThreadFunc func,
                     gpointer data,
                     GError **error)
{
  GThreadPosix *thread;
  GRealThread *base_thread;
  pthread_attr_t attr;
  gint ret;

  thread = g_slice_new0 (GThreadPosix);
  base_thread = (GRealThread*)thread;
  base_thread->ref_count = 2;
  base_thread->ours = TRUE;
  base_thread->thread.joinable = TRUE;
  base_thread->thread.func = func;
  base_thread->thread.data = data;
  if (name)
    g_strlcpy (base_thread->name, name, sizeof (base_thread->name));
  thread->proxy = proxy;

  posix_check_cmd (pthread_attr_init (&attr));

#ifdef HAVE_PTHREAD_ATTR_SETSTACKSIZE
  if (stack_size)
    {
#ifdef _SC_THREAD_STACK_MIN
      long min_stack_size = sysconf (_SC_THREAD_STACK_MIN);
      if (min_stack_size >= 0)
        stack_size = MAX ((gulong) min_stack_size, stack_size);
#endif /* _SC_THREAD_STACK_MIN */
      /* No error check here, because some systems can't do it and
       * we simply don't want threads to fail because of that. */
      pthread_attr_setstacksize (&attr, stack_size);
    }
#endif /* HAVE_PTHREAD_ATTR_SETSTACKSIZE */

#ifdef HAVE_PTHREAD_ATTR_SETINHERITSCHED
    {
      /* While this is the default, better be explicit about it */
      pthread_attr_setinheritsched (&attr, PTHREAD_INHERIT_SCHED);
    }
#endif /* HAVE_PTHREAD_ATTR_SETINHERITSCHED */

  ret = pthread_create (&thread->system_thread, &attr, (void* (*)(void*))proxy, thread);

  posix_check_cmd (pthread_attr_destroy (&attr));

  if (ret == EAGAIN)
    {
      g_set_error (error, G_THREAD_ERROR, G_THREAD_ERROR_AGAIN,
                   "Error creating thread: %s", g_strerror (ret));
      g_slice_free (GThreadPosix, thread);
      return NULL;
    }

  posix_check_err (ret, "pthread_create");

  g_mutex_init (&thread->lock);

  return (GRealThread *) thread;
}

G_ALWAYS_INLINE static inline void
g_thread_yield_impl (void)
{
  sched_yield ();
}

void
g_system_thread_wait (GRealThread *thread)
{
  GThreadPosix *pt = (GThreadPosix *) thread;

  g_mutex_lock (&pt->lock);

  if (!pt->joined)
    {
      posix_check_cmd (pthread_join (pt->system_thread, NULL));
      pt->joined = TRUE;
    }

  g_mutex_unlock (&pt->lock);
}

void
g_system_thread_exit (void)
{
  pthread_exit (NULL);
}

void
g_system_thread_set_name (const gchar *name)
{
#if defined(HAVE_PTHREAD_SETNAME_NP_WITHOUT_TID)
  pthread_setname_np (name); /* on OS X and iOS */
#elif defined(HAVE_PTHREAD_SETNAME_NP_WITH_TID)
#ifdef __LINUX__
#define MAX_THREADNAME_LEN 16
#else
#define MAX_THREADNAME_LEN 32
#endif
  char name_[MAX_THREADNAME_LEN];
  g_strlcpy (name_, name, MAX_THREADNAME_LEN);
  pthread_setname_np (pthread_self (), name_); /* on Linux and Solaris */
#elif defined(HAVE_PTHREAD_SETNAME_NP_WITH_TID_AND_ARG)
  pthread_setname_np (pthread_self (), "%s", (gchar *) name); /* on NetBSD */
#elif defined(HAVE_PTHREAD_SET_NAME_NP)
  pthread_set_name_np (pthread_self (), name); /* on FreeBSD, DragonFlyBSD, OpenBSD */
#endif
}

void
g_system_thread_get_name (char  *buffer,
                          gsize  length)
{
#ifdef HAVE_PTHREAD_GETNAME_NP
  pthread_getname_np (pthread_self (), buffer, length);
#else
  g_assert (length >= 1);
  buffer[0] = '\0';
#endif
}

/* {{{1 GMutex and GCond futex implementation */

#if defined(USE_NATIVE_MUTEX)
/* We should expand the set of operations available in gatomic once we
 * have better C11 support in GCC in common distributions (ie: 4.9).
 *
 * Before then, let's define a couple of useful things for our own
 * purposes...
 */

#ifdef HAVE_STDATOMIC_H

#include <stdatomic.h>

#define exchange_acquire(ptr, new) \
  atomic_exchange_explicit((atomic_uint *) (ptr), (new), __ATOMIC_ACQUIRE)
#define compare_exchange_acquire(ptr, old, new) \
  atomic_compare_exchange_strong_explicit((atomic_uint *) (ptr), (old), (new), \
                                          __ATOMIC_ACQUIRE, __ATOMIC_RELAXED)

#define exchange_release(ptr, new) \
  atomic_exchange_explicit((atomic_uint *) (ptr), (new), __ATOMIC_RELEASE)
#define store_release(ptr, new) \
  atomic_store_explicit((atomic_uint *) (ptr), (new), __ATOMIC_RELEASE)

#else

#define exchange_acquire(ptr, new) \
  __atomic_exchange_4((ptr), (new), __ATOMIC_ACQUIRE)
#define compare_exchange_acquire(ptr, old, new) \
  __atomic_compare_exchange_4((ptr), (old), (new), 0, __ATOMIC_ACQUIRE, __ATOMIC_RELAXED)

#define exchange_release(ptr, new) \
  __atomic_exchange_4((ptr), (new), __ATOMIC_RELEASE)
#define store_release(ptr, new) \
  __atomic_store_4((ptr), (new), __ATOMIC_RELEASE)

#endif

/* Our strategy for the mutex is pretty simple:
 *
 *  0: not in use
 *
 *  1: acquired by one thread only, no contention
 *
 *  2: contended
 */

typedef enum {
  G_MUTEX_STATE_EMPTY = 0,
  G_MUTEX_STATE_OWNED,
  G_MUTEX_STATE_CONTENDED,
} GMutexState;

 /*
 * As such, attempting to acquire the lock should involve an increment.
 * If we find that the previous value was 0 then we can return
 * immediately.
 *
 * On unlock, we always store 0 to indicate that the lock is available.
 * If the value there was 1 before then we didn't have contention and
 * can return immediately.  If the value was something other than 1 then
 * we have the contended case and need to wake a waiter.
 *
 * If it was not 0 then there is another thread holding it and we must
 * wait.  We must always ensure that we mark a value >1 while we are
 * waiting in order to instruct the holder to do a wake operation on
 * unlock.
 */

void
g_mutex_init_impl (GMutex *mutex)
{
  mutex->i[0] = G_MUTEX_STATE_EMPTY;
}

void
g_mutex_clear_impl (GMutex *mutex)
{
  if G_UNLIKELY (mutex->i[0] != G_MUTEX_STATE_EMPTY)
    {
      fprintf (stderr, "g_mutex_clear() called on uninitialised or locked mutex\n");
      g_abort ();
    }
}

G_GNUC_NO_INLINE
static void
g_mutex_lock_slowpath (GMutex *mutex)
{
  /* Set to contended.  If it was empty before then we
   * just acquired the lock.
   *
   * Otherwise, sleep for as long as the contended state remains...
   */
  while (exchange_acquire (&mutex->i[0], G_MUTEX_STATE_CONTENDED) != G_MUTEX_STATE_EMPTY)
    {
      g_futex_simple (&mutex->i[0], (gsize) FUTEX_WAIT_PRIVATE,
                      G_MUTEX_STATE_CONTENDED, NULL);
    }
}

G_GNUC_NO_INLINE
static void
g_mutex_unlock_slowpath (GMutex *mutex,
                         guint   prev)
{
  /* We seem to get better code for the uncontended case by splitting
   * this out...
   */
  if G_UNLIKELY (prev == G_MUTEX_STATE_EMPTY)
    {
      fprintf (stderr, "Attempt to unlock mutex that was not locked\n");
      g_abort ();
    }

  g_futex_simple (&mutex->i[0], (gsize) FUTEX_WAKE_PRIVATE, (gsize) 1, NULL);
}

inline void
g_mutex_lock_impl (GMutex *mutex)
{
  /* empty -> owned and we're done.  Anything else, and we need to wait... */
  if G_UNLIKELY (!g_atomic_int_compare_and_exchange (&mutex->i[0],
                                                     G_MUTEX_STATE_EMPTY,
                                                     G_MUTEX_STATE_OWNED))
    g_mutex_lock_slowpath (mutex);
}

void
g_mutex_unlock_impl (GMutex *mutex)
{
  guint prev;

  prev = exchange_release (&mutex->i[0], G_MUTEX_STATE_EMPTY);

  /* 1-> 0 and we're done.  Anything else and we need to signal... */
  if G_UNLIKELY (prev != G_MUTEX_STATE_OWNED)
    g_mutex_unlock_slowpath (mutex, prev);
}

gboolean
g_mutex_trylock_impl (GMutex *mutex)
{
  GMutexState empty = G_MUTEX_STATE_EMPTY;

  /* We don't want to touch the value at all unless we can move it from
   * exactly empty to owned.
   */
  return compare_exchange_acquire (&mutex->i[0], &empty, G_MUTEX_STATE_OWNED);
}

/* Condition variables are implemented in a rather simple way as well.
 * In many ways, futex() as an abstraction is even more ideally suited
 * to condition variables than it is to mutexes.
 *
 * We store a generation counter.  We sample it with the lock held and
 * unlock before sleeping on the futex.
 *
 * Signalling simply involves increasing the counter and making the
 * appropriate futex call.
 *
 * The only thing that is the slightest bit complicated is timed waits
 * because we must convert our absolute time to relative.
 */

void
g_cond_init_impl (GCond *cond)
{
  cond->i[0] = 0;
}

void
g_cond_clear_impl (GCond *cond)
{
}

void
g_cond_wait_impl (GCond  *cond,
                  GMutex *mutex)
{
  guint sampled = (guint) g_atomic_int_get (&cond->i[0]);

  g_mutex_unlock (mutex);
  g_futex_simple (&cond->i[0], (gsize) FUTEX_WAIT_PRIVATE, (gsize) sampled, NULL);
  g_mutex_lock (mutex);
}

void
g_cond_signal_impl (GCond *cond)
{
  g_atomic_int_inc (&cond->i[0]);

  g_futex_simple (&cond->i[0], (gsize) FUTEX_WAKE_PRIVATE, (gsize) 1, NULL);
}

void
g_cond_broadcast_impl (GCond *cond)
{
  g_atomic_int_inc (&cond->i[0]);

  g_futex_simple (&cond->i[0], (gsize) FUTEX_WAKE_PRIVATE, (gsize) INT_MAX, NULL);
}

gboolean
g_cond_wait_until_impl (GCond  *cond,
                        GMutex *mutex,
                        gint64  end_time)
{
  struct timespec now;
  struct timespec span;

  guint sampled;
  int res;
  gboolean success;

  if (end_time < 0)
    return FALSE;

  clock_gettime (CLOCK_MONOTONIC, &now);
  span.tv_sec = (end_time / 1000000) - now.tv_sec;
  span.tv_nsec = ((end_time % 1000000) * 1000) - now.tv_nsec;
  if (span.tv_nsec < 0)
    {
      span.tv_nsec += 1000000000;
      span.tv_sec--;
    }

  if (span.tv_sec < 0)
    return FALSE;

  /* `struct timespec` as defined by the libc headers does not necessarily
   * have any relation to the one used by the kernel for the `futex` syscall.
   *
   * Specifically, the libc headers might use 64-bit `time_t` while the kernel
   * headers use 32-bit types on certain systems.
   *
   * To get around this problem we
   *   a) check if `futex_time64` is available, which only exists on 32-bit
   *      platforms and always uses 64-bit `time_t`.
   *   b) if `futex_time64` is available, but the Android runtime's API level
   *      is < 30, `futex_time64` is blocked by seccomp and using it will cause
   *      the app to be terminated. Skip to c).
   *         https://android-review.googlesource.com/c/platform/bionic/+/1094758
   *   c) otherwise (or if that returns `ENOSYS`), we call the normal `futex`
   *      syscall with the `struct timespec` used by the kernel. By default, we
   *      use `__kernel_long_t` for both its fields, which is equivalent to
   *      `__kernel_old_time_t` and is available in the kernel headers for a
   *      longer time.
   *   d) With very old headers (~2.6.x), `__kernel_long_t` is not available, and
   *      we use an older definition that uses `__kernel_time_t` and `long`.
   *
   * Also some 32-bit systems do not define `__NR_futex` at all and only
   * define `__NR_futex_time64`.
   */

  sampled = cond->i[0];
  g_mutex_unlock (mutex);

#if defined(HAVE_FUTEX_TIME64)
#if defined(__ANDROID__)
  if (__builtin_available (android 30, *)) {
#else
  {
#endif
    struct
    {
      gint64 tv_sec;
      gint64 tv_nsec;
    } span_arg;

    span_arg.tv_sec = span.tv_sec;
    span_arg.tv_nsec = span.tv_nsec;

    res = syscall (__NR_futex_time64, &cond->i[0], (gsize) FUTEX_WAIT_PRIVATE, (gsize) sampled, &span_arg);

    /* If the syscall does not exist (`ENOSYS`), we retry again below with the
     * normal `futex` syscall. This can happen if newer kernel headers are
     * used than the kernel that is actually running.
     */
#  if defined(HAVE_FUTEX)
    if (res >= 0 || errno != ENOSYS)
#  endif /* defined(HAVE_FUTEX) */
      {
        success = (res < 0 && errno == ETIMEDOUT) ? FALSE : TRUE;
        g_mutex_lock (mutex);

        return success;
      }
  }
#endif

#if defined(HAVE_FUTEX)
  {
#  ifdef __kernel_long_t
#    define KERNEL_SPAN_SEC_TYPE __kernel_long_t
    struct
    {
      __kernel_long_t tv_sec;
      __kernel_long_t tv_nsec;
    } span_arg;
#  else
    /* Very old kernel headers: version 2.6.32 and thereabouts */
#    define KERNEL_SPAN_SEC_TYPE __kernel_time_t
    struct
    {
      __kernel_time_t tv_sec;
      long            tv_nsec;
    } span_arg;
#  endif
    /* Make sure to only ever call this if the end time actually fits into the target type */
    if (G_UNLIKELY (sizeof (KERNEL_SPAN_SEC_TYPE) < 8 && span.tv_sec > G_MAXINT32))
      g_error ("%s: Can’t wait for more than %us", G_STRFUNC, G_MAXINT32);

    span_arg.tv_sec = span.tv_sec;
    span_arg.tv_nsec = span.tv_nsec;

    res = syscall (__NR_futex, &cond->i[0], (gsize) FUTEX_WAIT_PRIVATE, (gsize) sampled, &span_arg);
    success = (res < 0 && errno == ETIMEDOUT) ? FALSE : TRUE;
    g_mutex_lock (mutex);

    return success;
  }
#  undef KERNEL_SPAN_SEC_TYPE
#endif /* defined(HAVE_FUTEX) */

  /* We can't end up here because of the checks above */
  g_assert_not_reached ();
}

#endif

  /* {{{1 Epilogue */
/* vim:set foldmethod=marker: */
