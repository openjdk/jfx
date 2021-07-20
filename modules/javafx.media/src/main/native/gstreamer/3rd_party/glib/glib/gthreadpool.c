/* GLIB - Library of useful routines for C programming
 * Copyright (C) 1995-1997  Peter Mattis, Spencer Kimball and Josh MacDonald
 *
 * GThreadPool: thread pool implementation.
 * Copyright (C) 2000 Sebastian Wilhelmi; University of Karlsruhe
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
 * MT safe
 */

#include "config.h"

#include "gthreadpool.h"

#include "gasyncqueue.h"
#include "gasyncqueueprivate.h"
#include "gmain.h"
#include "gtestutils.h"
#include "gthreadprivate.h"
#include "gtimer.h"
#include "gutils.h"

/**
 * SECTION:thread_pools
 * @title: Thread Pools
 * @short_description: pools of threads to execute work concurrently
 * @see_also: #GThread
 *
 * Sometimes you wish to asynchronously fork out the execution of work
 * and continue working in your own thread. If that will happen often,
 * the overhead of starting and destroying a thread each time might be
 * too high. In such cases reusing already started threads seems like a
 * good idea. And it indeed is, but implementing this can be tedious
 * and error-prone.
 *
 * Therefore GLib provides thread pools for your convenience. An added
 * advantage is, that the threads can be shared between the different
 * subsystems of your program, when they are using GLib.
 *
 * To create a new thread pool, you use g_thread_pool_new().
 * It is destroyed by g_thread_pool_free().
 *
 * If you want to execute a certain task within a thread pool,
 * you call g_thread_pool_push().
 *
 * To get the current number of running threads you call
 * g_thread_pool_get_num_threads(). To get the number of still
 * unprocessed tasks you call g_thread_pool_unprocessed(). To control
 * the maximal number of threads for a thread pool, you use
 * g_thread_pool_get_max_threads() and g_thread_pool_set_max_threads().
 *
 * Finally you can control the number of unused threads, that are kept
 * alive by GLib for future use. The current number can be fetched with
 * g_thread_pool_get_num_unused_threads(). The maximal number can be
 * controlled by g_thread_pool_get_max_unused_threads() and
 * g_thread_pool_set_max_unused_threads(). All currently unused threads
 * can be stopped by calling g_thread_pool_stop_unused_threads().
 */

#define DEBUG_MSG(x)
/* #define DEBUG_MSG(args) g_printerr args ; g_printerr ("\n");    */

typedef struct _GRealThreadPool GRealThreadPool;

/**
 * GThreadPool:
 * @func: the function to execute in the threads of this pool
 * @user_data: the user data for the threads of this pool
 * @exclusive: are all threads exclusive to this pool
 *
 * The #GThreadPool struct represents a thread pool. It has three
 * public read-only members, but the underlying struct is bigger,
 * so you must not copy this struct.
 */
struct _GRealThreadPool
{
  GThreadPool pool;
  GAsyncQueue *queue;
  GCond cond;
  gint max_threads;
  guint num_threads;
  gboolean running;
  gboolean immediate;
  gboolean waiting;
  GCompareDataFunc sort_func;
  gpointer sort_user_data;
};

/* The following is just an address to mark the wakeup order for a
 * thread, it could be any address (as long, as it isn't a valid
 * GThreadPool address)
 */
static const gpointer wakeup_thread_marker = (gpointer) &g_thread_pool_new;
static gint wakeup_thread_serial = 0;

/* Here all unused threads are waiting  */
static GAsyncQueue *unused_thread_queue = NULL;
static gint unused_threads = 0;
static gint max_unused_threads = 2;
static gint kill_unused_threads = 0;
static guint max_idle_time = 15 * 1000;

static GThreadSchedulerSettings shared_thread_scheduler_settings;
static gboolean have_shared_thread_scheduler_settings = FALSE;

typedef struct
{
  /* Either thread or error are set in the end. Both transfer-full. */
  GThreadPool *pool;
  GThread *thread;
  GError *error;
} SpawnThreadData;

static GCond spawn_thread_cond;
static GAsyncQueue *spawn_thread_queue;

static void             g_thread_pool_queue_push_unlocked (GRealThreadPool  *pool,
                                                           gpointer          data);
static void             g_thread_pool_free_internal       (GRealThreadPool  *pool);
static gpointer         g_thread_pool_thread_proxy        (gpointer          data);
static gboolean         g_thread_pool_start_thread        (GRealThreadPool  *pool,
                                                           GError          **error);
static void             g_thread_pool_wakeup_and_stop_all (GRealThreadPool  *pool);
static GRealThreadPool* g_thread_pool_wait_for_new_pool   (void);
static gpointer         g_thread_pool_wait_for_new_task   (GRealThreadPool  *pool);

static void
g_thread_pool_queue_push_unlocked (GRealThreadPool *pool,
                                   gpointer         data)
{
  if (pool->sort_func)
    g_async_queue_push_sorted_unlocked (pool->queue,
                                        data,
                                        pool->sort_func,
                                        pool->sort_user_data);
  else
    g_async_queue_push_unlocked (pool->queue, data);
}

static GRealThreadPool*
g_thread_pool_wait_for_new_pool (void)
{
  GRealThreadPool *pool;
  gint local_wakeup_thread_serial;
  guint local_max_unused_threads;
  gint local_max_idle_time;
  gint last_wakeup_thread_serial;
  gboolean have_relayed_thread_marker = FALSE;

  local_max_unused_threads = (guint) g_atomic_int_get (&max_unused_threads);
  local_max_idle_time = g_atomic_int_get (&max_idle_time);
  last_wakeup_thread_serial = g_atomic_int_get (&wakeup_thread_serial);

  g_atomic_int_inc (&unused_threads);

  do
    {
      if ((guint) g_atomic_int_get (&unused_threads) >= local_max_unused_threads)
        {
          /* If this is a superfluous thread, stop it. */
          pool = NULL;
        }
      else if (local_max_idle_time > 0)
        {
          /* If a maximal idle time is given, wait for the given time. */
          DEBUG_MSG (("thread %p waiting in global pool for %f seconds.",
                      g_thread_self (), local_max_idle_time / 1000.0));

          pool = g_async_queue_timeout_pop (unused_thread_queue,
              local_max_idle_time * 1000);
        }
      else
        {
          /* If no maximal idle time is given, wait indefinitely. */
          DEBUG_MSG (("thread %p waiting in global pool.", g_thread_self ()));
          pool = g_async_queue_pop (unused_thread_queue);
        }

      if (pool == wakeup_thread_marker)
        {
          local_wakeup_thread_serial = g_atomic_int_get (&wakeup_thread_serial);
          if (last_wakeup_thread_serial == local_wakeup_thread_serial)
            {
              if (!have_relayed_thread_marker)
              {
                /* If this wakeup marker has been received for
                 * the second time, relay it.
                 */
                DEBUG_MSG (("thread %p relaying wakeup message to "
                            "waiting thread with lower serial.",
                            g_thread_self ()));

                g_async_queue_push (unused_thread_queue, wakeup_thread_marker);
                have_relayed_thread_marker = TRUE;

                /* If a wakeup marker has been relayed, this thread
                 * will get out of the way for 100 microseconds to
                 * avoid receiving this marker again.
                 */
                g_usleep (100);
              }
            }
          else
            {
              if (g_atomic_int_add (&kill_unused_threads, -1) > 0)
                {
                  pool = NULL;
                  break;
                }

              DEBUG_MSG (("thread %p updating to new limits.",
                          g_thread_self ()));

              local_max_unused_threads = (guint) g_atomic_int_get (&max_unused_threads);
              local_max_idle_time = g_atomic_int_get (&max_idle_time);
              last_wakeup_thread_serial = local_wakeup_thread_serial;

              have_relayed_thread_marker = FALSE;
            }
        }
    }
  while (pool == wakeup_thread_marker);

  g_atomic_int_add (&unused_threads, -1);

  return pool;
}

static gpointer
g_thread_pool_wait_for_new_task (GRealThreadPool *pool)
{
  gpointer task = NULL;

  if (pool->running || (!pool->immediate &&
                        g_async_queue_length_unlocked (pool->queue) > 0))
    {
      /* This thread pool is still active. */
      if (pool->max_threads != -1 && pool->num_threads > (guint) pool->max_threads)
        {
          /* This is a superfluous thread, so it goes to the global pool. */
          DEBUG_MSG (("superfluous thread %p in pool %p.",
                      g_thread_self (), pool));
        }
      else if (pool->pool.exclusive)
        {
          /* Exclusive threads stay attached to the pool. */
          task = g_async_queue_pop_unlocked (pool->queue);

          DEBUG_MSG (("thread %p in exclusive pool %p waits for task "
                      "(%d running, %d unprocessed).",
                      g_thread_self (), pool, pool->num_threads,
                      g_async_queue_length_unlocked (pool->queue)));
        }
      else
        {
          /* A thread will wait for new tasks for at most 1/2
           * second before going to the global pool.
           */
          DEBUG_MSG (("thread %p in pool %p waits for up to a 1/2 second for task "
                      "(%d running, %d unprocessed).",
                      g_thread_self (), pool, pool->num_threads,
                      g_async_queue_length_unlocked (pool->queue)));

          task = g_async_queue_timeout_pop_unlocked (pool->queue,
                 G_USEC_PER_SEC / 2);
        }
    }
  else
    {
      /* This thread pool is inactive, it will no longer process tasks. */
      DEBUG_MSG (("pool %p not active, thread %p will go to global pool "
                  "(running: %s, immediate: %s, len: %d).",
                  pool, g_thread_self (),
                  pool->running ? "true" : "false",
                  pool->immediate ? "true" : "false",
                  g_async_queue_length_unlocked (pool->queue)));
    }

  return task;
}

static gpointer
g_thread_pool_spawn_thread (gpointer data)
{
  while (TRUE)
    {
      SpawnThreadData *spawn_thread_data;
      GThread *thread = NULL;
      GError *error = NULL;
      const gchar *prgname = g_get_prgname ();
      gchar name[16] = "pool";

      if (prgname)
        g_snprintf (name, sizeof (name), "pool-%s", prgname);

      g_async_queue_lock (spawn_thread_queue);
      /* Spawn a new thread for the given pool and wake the requesting thread
       * up again with the result. This new thread will have the scheduler
       * settings inherited from this thread and in extension of the thread
       * that created the first non-exclusive thread-pool. */
      spawn_thread_data = g_async_queue_pop_unlocked (spawn_thread_queue);
      thread = g_thread_try_new (name, g_thread_pool_thread_proxy, spawn_thread_data->pool, &error);

      spawn_thread_data->thread = g_steal_pointer (&thread);
      spawn_thread_data->error = g_steal_pointer (&error);

      g_cond_broadcast (&spawn_thread_cond);
      g_async_queue_unlock (spawn_thread_queue);
    }

  return NULL;
}

static gpointer
g_thread_pool_thread_proxy (gpointer data)
{
  GRealThreadPool *pool;

  pool = data;

  DEBUG_MSG (("thread %p started for pool %p.", g_thread_self (), pool));

  g_async_queue_lock (pool->queue);

  while (TRUE)
    {
      gpointer task;

      task = g_thread_pool_wait_for_new_task (pool);
      if (task)
        {
          if (pool->running || !pool->immediate)
            {
              /* A task was received and the thread pool is active,
               * so execute the function.
               */
              g_async_queue_unlock (pool->queue);
              DEBUG_MSG (("thread %p in pool %p calling func.",
                          g_thread_self (), pool));
              pool->pool.func (task, pool->pool.user_data);
              g_async_queue_lock (pool->queue);
            }
        }
      else
        {
          /* No task was received, so this thread goes to the global pool. */
          gboolean free_pool = FALSE;

          DEBUG_MSG (("thread %p leaving pool %p for global pool.",
                      g_thread_self (), pool));
          pool->num_threads--;

          if (!pool->running)
            {
              if (!pool->waiting)
                {
                  if (pool->num_threads == 0)
                    {
                      /* If the pool is not running and no other
                       * thread is waiting for this thread pool to
                       * finish and this is the last thread of this
                       * pool, free the pool.
                       */
                      free_pool = TRUE;
                    }
                  else
                    {
                      /* If the pool is not running and no other
                       * thread is waiting for this thread pool to
                       * finish and this is not the last thread of
                       * this pool and there are no tasks left in the
                       * queue, wakeup the remaining threads.
                       */
                      if (g_async_queue_length_unlocked (pool->queue) ==
                          (gint) -pool->num_threads)
                        g_thread_pool_wakeup_and_stop_all (pool);
                    }
                }
              else if (pool->immediate ||
                       g_async_queue_length_unlocked (pool->queue) <= 0)
                {
                  /* If the pool is not running and another thread is
                   * waiting for this thread pool to finish and there
                   * are either no tasks left or the pool shall stop
                   * immediately, inform the waiting thread of a change
                   * of the thread pool state.
                   */
                  g_cond_broadcast (&pool->cond);
                }
            }

          g_async_queue_unlock (pool->queue);

          if (free_pool)
            g_thread_pool_free_internal (pool);

          if ((pool = g_thread_pool_wait_for_new_pool ()) == NULL)
            break;

          g_async_queue_lock (pool->queue);

          DEBUG_MSG (("thread %p entering pool %p from global pool.",
                      g_thread_self (), pool));

          /* pool->num_threads++ is not done here, but in
           * g_thread_pool_start_thread to make the new started
           * thread known to the pool before itself can do it.
           */
        }
    }

  return NULL;
}

static gboolean
g_thread_pool_start_thread (GRealThreadPool  *pool,
                            GError          **error)
{
  gboolean success = FALSE;

  if (pool->max_threads != -1 && pool->num_threads >= (guint) pool->max_threads)
    /* Enough threads are already running */
    return TRUE;

  g_async_queue_lock (unused_thread_queue);

  if (g_async_queue_length_unlocked (unused_thread_queue) < 0)
    {
      g_async_queue_push_unlocked (unused_thread_queue, pool);
      success = TRUE;
    }

  g_async_queue_unlock (unused_thread_queue);

  if (!success)
    {
      const gchar *prgname = g_get_prgname ();
      gchar name[16] = "pool";
      GThread *thread;

      if (prgname)
        g_snprintf (name, sizeof (name), "pool-%s", prgname);

      /* No thread was found, we have to start a new one */
      if (pool->pool.exclusive)
        {
          /* For exclusive thread-pools this is directly called from new() and
           * we simply start new threads that inherit the scheduler settings
           * from the current thread.
           */
          thread = g_thread_try_new (name, g_thread_pool_thread_proxy, pool, error);
        }
      else
        {
          /* For non-exclusive thread-pools this can be called at any time
           * when a new thread is needed. We make sure to create a new thread
           * here with the correct scheduler settings: either by directly
           * providing them if supported by the GThread implementation or by
           * going via our helper thread.
           */
          if (have_shared_thread_scheduler_settings)
            {
              thread = g_thread_new_internal (name, g_thread_proxy, g_thread_pool_thread_proxy, pool, 0, &shared_thread_scheduler_settings, error);
            }
          else
            {
              SpawnThreadData spawn_thread_data = { (GThreadPool *) pool, NULL, NULL };

              g_async_queue_lock (spawn_thread_queue);

              g_async_queue_push_unlocked (spawn_thread_queue, &spawn_thread_data);

              while (!spawn_thread_data.thread && !spawn_thread_data.error)
                g_cond_wait (&spawn_thread_cond, _g_async_queue_get_mutex (spawn_thread_queue));

              thread = spawn_thread_data.thread;
              if (!thread)
                g_propagate_error (error, g_steal_pointer (&spawn_thread_data.error));
              g_async_queue_unlock (spawn_thread_queue);
            }
        }

      if (thread == NULL)
        return FALSE;

      g_thread_unref (thread);
    }

  /* See comment in g_thread_pool_thread_proxy as to why this is done
   * here and not there
   */
  pool->num_threads++;

  return TRUE;
}

/**
 * g_thread_pool_new:
 * @func: a function to execute in the threads of the new thread pool
 * @user_data: user data that is handed over to @func every time it
 *     is called
 * @max_threads: the maximal number of threads to execute concurrently
 *     in  the new thread pool, -1 means no limit
 * @exclusive: should this thread pool be exclusive?
 * @error: return location for error, or %NULL
 *
 * This function creates a new thread pool.
 *
 * Whenever you call g_thread_pool_push(), either a new thread is
 * created or an unused one is reused. At most @max_threads threads
 * are running concurrently for this thread pool. @max_threads = -1
 * allows unlimited threads to be created for this thread pool. The
 * newly created or reused thread now executes the function @func
 * with the two arguments. The first one is the parameter to
 * g_thread_pool_push() and the second one is @user_data.
 *
 * Pass g_get_num_processors() to @max_threads to create as many threads as
 * there are logical processors on the system. This will not pin each thread to
 * a specific processor.
 *
 * The parameter @exclusive determines whether the thread pool owns
 * all threads exclusive or shares them with other thread pools.
 * If @exclusive is %TRUE, @max_threads threads are started
 * immediately and they will run exclusively for this thread pool
 * until it is destroyed by g_thread_pool_free(). If @exclusive is
 * %FALSE, threads are created when needed and shared between all
 * non-exclusive thread pools. This implies that @max_threads may
 * not be -1 for exclusive thread pools. Besides, exclusive thread
 * pools are not affected by g_thread_pool_set_max_idle_time()
 * since their threads are never considered idle and returned to the
 * global pool.
 *
 * @error can be %NULL to ignore errors, or non-%NULL to report
 * errors. An error can only occur when @exclusive is set to %TRUE
 * and not all @max_threads threads could be created.
 * See #GThreadError for possible errors that may occur.
 * Note, even in case of error a valid #GThreadPool is returned.
 *
 * Returns: the new #GThreadPool
 */
GThreadPool *
g_thread_pool_new (GFunc      func,
                   gpointer   user_data,
                   gint       max_threads,
                   gboolean   exclusive,
                   GError   **error)
{
  GRealThreadPool *retval;
  G_LOCK_DEFINE_STATIC (init);

  g_return_val_if_fail (func, NULL);
  g_return_val_if_fail (!exclusive || max_threads != -1, NULL);
  g_return_val_if_fail (max_threads >= -1, NULL);

  retval = g_new (GRealThreadPool, 1);

  retval->pool.func = func;
  retval->pool.user_data = user_data;
  retval->pool.exclusive = exclusive;
  retval->queue = g_async_queue_new ();
  g_cond_init (&retval->cond);
  retval->max_threads = max_threads;
  retval->num_threads = 0;
  retval->running = TRUE;
  retval->immediate = FALSE;
  retval->waiting = FALSE;
  retval->sort_func = NULL;
  retval->sort_user_data = NULL;

  G_LOCK (init);
  if (!unused_thread_queue)
      unused_thread_queue = g_async_queue_new ();

  /* For the very first non-exclusive thread-pool we remember the thread
   * scheduler settings of the thread creating the pool, if supported by
   * the GThread implementation. This is then used for making sure that
   * all threads created on the non-exclusive thread-pool have the same
   * scheduler settings, and more importantly don't just inherit them
   * from the thread that just happened to push a new task and caused
   * a new thread to be created.
   *
   * Not doing so could cause real-time priority threads or otherwise
   * threads with problematic scheduler settings to be part of the
   * non-exclusive thread-pools.
   *
   * If this is not supported by the GThread implementation then we here
   * start a thread that will inherit the scheduler settings from this
   * very thread and whose only purpose is to spawn new threads with the
   * same settings for use by the non-exclusive thread-pools.
   *
   *
   * For non-exclusive thread-pools this is not required as all threads
   * are created immediately below and are running forever, so they will
   * automatically inherit the scheduler settings from this very thread.
   */
  if (!exclusive && !have_shared_thread_scheduler_settings && !spawn_thread_queue)
    {
      if (g_thread_get_scheduler_settings (&shared_thread_scheduler_settings))
        {
          have_shared_thread_scheduler_settings = TRUE;
        }
      else
        {
          spawn_thread_queue = g_async_queue_new ();
          g_cond_init (&spawn_thread_cond);
          g_thread_new ("pool-spawner", g_thread_pool_spawn_thread, NULL);
        }
    }
  G_UNLOCK (init);

  if (retval->pool.exclusive)
    {
      g_async_queue_lock (retval->queue);

      while (retval->num_threads < (guint) retval->max_threads)
        {
          GError *local_error = NULL;

          if (!g_thread_pool_start_thread (retval, &local_error))
            {
              g_propagate_error (error, local_error);
              break;
            }
        }

      g_async_queue_unlock (retval->queue);
    }

  return (GThreadPool*) retval;
}

/**
 * g_thread_pool_push:
 * @pool: a #GThreadPool
 * @data: a new task for @pool
 * @error: return location for error, or %NULL
 *
 * Inserts @data into the list of tasks to be executed by @pool.
 *
 * When the number of currently running threads is lower than the
 * maximal allowed number of threads, a new thread is started (or
 * reused) with the properties given to g_thread_pool_new().
 * Otherwise, @data stays in the queue until a thread in this pool
 * finishes its previous task and processes @data.
 *
 * @error can be %NULL to ignore errors, or non-%NULL to report
 * errors. An error can only occur when a new thread couldn't be
 * created. In that case @data is simply appended to the queue of
 * work to do.
 *
 * Before version 2.32, this function did not return a success status.
 *
 * Returns: %TRUE on success, %FALSE if an error occurred
 */
gboolean
g_thread_pool_push (GThreadPool  *pool,
                    gpointer      data,
                    GError      **error)
{
  GRealThreadPool *real;
  gboolean result;

  real = (GRealThreadPool*) pool;

  g_return_val_if_fail (real, FALSE);
  g_return_val_if_fail (real->running, FALSE);

  result = TRUE;

  g_async_queue_lock (real->queue);

  if (g_async_queue_length_unlocked (real->queue) >= 0)
    {
      /* No thread is waiting in the queue */
      GError *local_error = NULL;

      if (!g_thread_pool_start_thread (real, &local_error))
        {
          g_propagate_error (error, local_error);
          result = FALSE;
        }
    }

  g_thread_pool_queue_push_unlocked (real, data);
  g_async_queue_unlock (real->queue);

  return result;
}

/**
 * g_thread_pool_set_max_threads:
 * @pool: a #GThreadPool
 * @max_threads: a new maximal number of threads for @pool,
 *     or -1 for unlimited
 * @error: return location for error, or %NULL
 *
 * Sets the maximal allowed number of threads for @pool.
 * A value of -1 means that the maximal number of threads
 * is unlimited. If @pool is an exclusive thread pool, setting
 * the maximal number of threads to -1 is not allowed.
 *
 * Setting @max_threads to 0 means stopping all work for @pool.
 * It is effectively frozen until @max_threads is set to a non-zero
 * value again.
 *
 * A thread is never terminated while calling @func, as supplied by
 * g_thread_pool_new(). Instead the maximal number of threads only
 * has effect for the allocation of new threads in g_thread_pool_push().
 * A new thread is allocated, whenever the number of currently
 * running threads in @pool is smaller than the maximal number.
 *
 * @error can be %NULL to ignore errors, or non-%NULL to report
 * errors. An error can only occur when a new thread couldn't be
 * created.
 *
 * Before version 2.32, this function did not return a success status.
 *
 * Returns: %TRUE on success, %FALSE if an error occurred
 */
gboolean
g_thread_pool_set_max_threads (GThreadPool  *pool,
                               gint          max_threads,
                               GError      **error)
{
  GRealThreadPool *real;
  gint to_start;
  gboolean result;

  real = (GRealThreadPool*) pool;

  g_return_val_if_fail (real, FALSE);
  g_return_val_if_fail (real->running, FALSE);
  g_return_val_if_fail (!real->pool.exclusive || max_threads != -1, FALSE);
  g_return_val_if_fail (max_threads >= -1, FALSE);

  result = TRUE;

  g_async_queue_lock (real->queue);

  real->max_threads = max_threads;

  if (pool->exclusive)
    to_start = real->max_threads - real->num_threads;
  else
    to_start = g_async_queue_length_unlocked (real->queue);

  for ( ; to_start > 0; to_start--)
    {
      GError *local_error = NULL;

      if (!g_thread_pool_start_thread (real, &local_error))
        {
          g_propagate_error (error, local_error);
          result = FALSE;
          break;
        }
    }

  g_async_queue_unlock (real->queue);

  return result;
}

/**
 * g_thread_pool_get_max_threads:
 * @pool: a #GThreadPool
 *
 * Returns the maximal number of threads for @pool.
 *
 * Returns: the maximal number of threads
 */
gint
g_thread_pool_get_max_threads (GThreadPool *pool)
{
  GRealThreadPool *real;
  gint retval;

  real = (GRealThreadPool*) pool;

  g_return_val_if_fail (real, 0);
  g_return_val_if_fail (real->running, 0);

  g_async_queue_lock (real->queue);
  retval = real->max_threads;
  g_async_queue_unlock (real->queue);

  return retval;
}

/**
 * g_thread_pool_get_num_threads:
 * @pool: a #GThreadPool
 *
 * Returns the number of threads currently running in @pool.
 *
 * Returns: the number of threads currently running
 */
guint
g_thread_pool_get_num_threads (GThreadPool *pool)
{
  GRealThreadPool *real;
  guint retval;

  real = (GRealThreadPool*) pool;

  g_return_val_if_fail (real, 0);
  g_return_val_if_fail (real->running, 0);

  g_async_queue_lock (real->queue);
  retval = real->num_threads;
  g_async_queue_unlock (real->queue);

  return retval;
}

/**
 * g_thread_pool_unprocessed:
 * @pool: a #GThreadPool
 *
 * Returns the number of tasks still unprocessed in @pool.
 *
 * Returns: the number of unprocessed tasks
 */
guint
g_thread_pool_unprocessed (GThreadPool *pool)
{
  GRealThreadPool *real;
  gint unprocessed;

  real = (GRealThreadPool*) pool;

  g_return_val_if_fail (real, 0);
  g_return_val_if_fail (real->running, 0);

  unprocessed = g_async_queue_length (real->queue);

  return MAX (unprocessed, 0);
}

/**
 * g_thread_pool_free:
 * @pool: a #GThreadPool
 * @immediate: should @pool shut down immediately?
 * @wait_: should the function wait for all tasks to be finished?
 *
 * Frees all resources allocated for @pool.
 *
 * If @immediate is %TRUE, no new task is processed for @pool.
 * Otherwise @pool is not freed before the last task is processed.
 * Note however, that no thread of this pool is interrupted while
 * processing a task. Instead at least all still running threads
 * can finish their tasks before the @pool is freed.
 *
 * If @wait_ is %TRUE, this function does not return before all
 * tasks to be processed (dependent on @immediate, whether all
 * or only the currently running) are ready.
 * Otherwise this function returns immediately.
 *
 * After calling this function @pool must not be used anymore.
 */
void
g_thread_pool_free (GThreadPool *pool,
                    gboolean     immediate,
                    gboolean     wait_)
{
  GRealThreadPool *real;

  real = (GRealThreadPool*) pool;

  g_return_if_fail (real);
  g_return_if_fail (real->running);

  /* If there's no thread allowed here, there is not much sense in
   * not stopping this pool immediately, when it's not empty
   */
  g_return_if_fail (immediate ||
                    real->max_threads != 0 ||
                    g_async_queue_length (real->queue) == 0);

  g_async_queue_lock (real->queue);

  real->running = FALSE;
  real->immediate = immediate;
  real->waiting = wait_;

  if (wait_)
    {
      while (g_async_queue_length_unlocked (real->queue) != (gint) -real->num_threads &&
             !(immediate && real->num_threads == 0))
        g_cond_wait (&real->cond, _g_async_queue_get_mutex (real->queue));
    }

  if (immediate || g_async_queue_length_unlocked (real->queue) == (gint) -real->num_threads)
    {
      /* No thread is currently doing something (and nothing is left
       * to process in the queue)
       */
      if (real->num_threads == 0)
        {
          /* No threads left, we clean up */
          g_async_queue_unlock (real->queue);
          g_thread_pool_free_internal (real);
          return;
        }

      g_thread_pool_wakeup_and_stop_all (real);
    }

  /* The last thread should cleanup the pool */
  real->waiting = FALSE;
  g_async_queue_unlock (real->queue);
}

static void
g_thread_pool_free_internal (GRealThreadPool* pool)
{
  g_return_if_fail (pool);
  g_return_if_fail (pool->running == FALSE);
  g_return_if_fail (pool->num_threads == 0);

  g_async_queue_unref (pool->queue);
  g_cond_clear (&pool->cond);

  g_free (pool);
}

static void
g_thread_pool_wakeup_and_stop_all (GRealThreadPool *pool)
{
  guint i;

  g_return_if_fail (pool);
  g_return_if_fail (pool->running == FALSE);
  g_return_if_fail (pool->num_threads != 0);

  pool->immediate = TRUE;

  /*
   * So here we're sending bogus data to the pool threads, which
   * should cause them each to wake up, and check the above
   * pool->immediate condition. However we don't want that
   * data to be sorted (since it'll crash the sorter).
   */
  for (i = 0; i < pool->num_threads; i++)
    g_async_queue_push_unlocked (pool->queue, GUINT_TO_POINTER (1));
}

/**
 * g_thread_pool_set_max_unused_threads:
 * @max_threads: maximal number of unused threads
 *
 * Sets the maximal number of unused threads to @max_threads.
 * If @max_threads is -1, no limit is imposed on the number
 * of unused threads.
 *
 * The default value is 2.
 */
void
g_thread_pool_set_max_unused_threads (gint max_threads)
{
  g_return_if_fail (max_threads >= -1);

  g_atomic_int_set (&max_unused_threads, max_threads);

  if (max_threads != -1)
    {
      max_threads -= g_atomic_int_get (&unused_threads);
      if (max_threads < 0)
        {
          g_atomic_int_set (&kill_unused_threads, -max_threads);
          g_atomic_int_inc (&wakeup_thread_serial);

          g_async_queue_lock (unused_thread_queue);

          do
            {
              g_async_queue_push_unlocked (unused_thread_queue,
                                           wakeup_thread_marker);
            }
          while (++max_threads);

          g_async_queue_unlock (unused_thread_queue);
        }
    }
}

/**
 * g_thread_pool_get_max_unused_threads:
 *
 * Returns the maximal allowed number of unused threads.
 *
 * Returns: the maximal number of unused threads
 */
gint
g_thread_pool_get_max_unused_threads (void)
{
  return g_atomic_int_get (&max_unused_threads);
}

/**
 * g_thread_pool_get_num_unused_threads:
 *
 * Returns the number of currently unused threads.
 *
 * Returns: the number of currently unused threads
 */
guint
g_thread_pool_get_num_unused_threads (void)
{
  return (guint) g_atomic_int_get (&unused_threads);
}

/**
 * g_thread_pool_stop_unused_threads:
 *
 * Stops all currently unused threads. This does not change the
 * maximal number of unused threads. This function can be used to
 * regularly stop all unused threads e.g. from g_timeout_add().
 */
void
g_thread_pool_stop_unused_threads (void)
{
  guint oldval;

  oldval = g_thread_pool_get_max_unused_threads ();

  g_thread_pool_set_max_unused_threads (0);
  g_thread_pool_set_max_unused_threads (oldval);
}

/**
 * g_thread_pool_set_sort_function:
 * @pool: a #GThreadPool
 * @func: the #GCompareDataFunc used to sort the list of tasks.
 *     This function is passed two tasks. It should return
 *     0 if the order in which they are handled does not matter,
 *     a negative value if the first task should be processed before
 *     the second or a positive value if the second task should be
 *     processed first.
 * @user_data: user data passed to @func
 *
 * Sets the function used to sort the list of tasks. This allows the
 * tasks to be processed by a priority determined by @func, and not
 * just in the order in which they were added to the pool.
 *
 * Note, if the maximum number of threads is more than 1, the order
 * that threads are executed cannot be guaranteed 100%. Threads are
 * scheduled by the operating system and are executed at random. It
 * cannot be assumed that threads are executed in the order they are
 * created.
 *
 * Since: 2.10
 */
void
g_thread_pool_set_sort_function (GThreadPool      *pool,
                                 GCompareDataFunc  func,
                                 gpointer          user_data)
{
  GRealThreadPool *real;

  real = (GRealThreadPool*) pool;

  g_return_if_fail (real);
  g_return_if_fail (real->running);

  g_async_queue_lock (real->queue);

  real->sort_func = func;
  real->sort_user_data = user_data;

  if (func)
    g_async_queue_sort_unlocked (real->queue,
                                 real->sort_func,
                                 real->sort_user_data);

  g_async_queue_unlock (real->queue);
}

/**
 * g_thread_pool_move_to_front:
 * @pool: a #GThreadPool
 * @data: an unprocessed item in the pool
 *
 * Moves the item to the front of the queue of unprocessed
 * items, so that it will be processed next.
 *
 * Returns: %TRUE if the item was found and moved
 *
 * Since: 2.46
 */
gboolean
g_thread_pool_move_to_front (GThreadPool *pool,
                             gpointer     data)
{
  GRealThreadPool *real = (GRealThreadPool*) pool;
  gboolean found;

  g_async_queue_lock (real->queue);

  found = g_async_queue_remove_unlocked (real->queue, data);
  if (found)
    g_async_queue_push_front_unlocked (real->queue, data);

  g_async_queue_unlock (real->queue);

  return found;
}

/**
 * g_thread_pool_set_max_idle_time:
 * @interval: the maximum @interval (in milliseconds)
 *     a thread can be idle
 *
 * This function will set the maximum @interval that a thread
 * waiting in the pool for new tasks can be idle for before
 * being stopped. This function is similar to calling
 * g_thread_pool_stop_unused_threads() on a regular timeout,
 * except this is done on a per thread basis.
 *
 * By setting @interval to 0, idle threads will not be stopped.
 *
 * The default value is 15000 (15 seconds).
 *
 * Since: 2.10
 */
void
g_thread_pool_set_max_idle_time (guint interval)
{
  guint i;

  g_atomic_int_set (&max_idle_time, interval);

  i = (guint) g_atomic_int_get (&unused_threads);
  if (i > 0)
    {
      g_atomic_int_inc (&wakeup_thread_serial);
      g_async_queue_lock (unused_thread_queue);

      do
        {
          g_async_queue_push_unlocked (unused_thread_queue,
                                       wakeup_thread_marker);
        }
      while (--i);

      g_async_queue_unlock (unused_thread_queue);
    }
}

/**
 * g_thread_pool_get_max_idle_time:
 *
 * This function will return the maximum @interval that a
 * thread will wait in the thread pool for new tasks before
 * being stopped.
 *
 * If this function returns 0, threads waiting in the thread
 * pool for new work are not stopped.
 *
 * Returns: the maximum @interval (milliseconds) to wait
 *     for new tasks in the thread pool before stopping the
 *     thread
 *
 * Since: 2.10
 */
guint
g_thread_pool_get_max_idle_time (void)
{
  return (guint) g_atomic_int_get (&max_idle_time);
}
