/* GStreamer
 * Copyright (C) 2009 Wim Taymans <wim.taymans@gmail.com>
 *
 * gsttaskpool.c: Pool for streaming threads
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 */

/**
 * SECTION:gsttaskpool
 * @title: GstTaskPool
 * @short_description: Pool of GStreamer streaming threads
 * @see_also: #GstTask, #GstPad
 *
 * This object provides an abstraction for creating threads. The default
 * implementation uses a regular GThreadPool to start tasks.
 *
 * Subclasses can be made to create custom threads.
 */

#include "gst_private.h"

#include "gstinfo.h"
#include "gsttaskpool.h"
#include "gsterror.h"

GST_DEBUG_CATEGORY_STATIC (taskpool_debug);
#define GST_CAT_DEFAULT (taskpool_debug)

#ifndef GST_DISABLE_GST_DEBUG
static void gst_task_pool_finalize (GObject * object);
#endif

#define _do_init \
{ \
  GST_DEBUG_CATEGORY_INIT (taskpool_debug, "taskpool", 0, "Thread pool"); \
}

G_DEFINE_TYPE_WITH_CODE (GstTaskPool, gst_task_pool, GST_TYPE_OBJECT, _do_init);

typedef struct
{
  GstTaskPoolFunction func;
  gpointer user_data;
} TaskData;

static void
default_func (TaskData * tdata, GstTaskPool * pool)
{
  GstTaskPoolFunction func;
  gpointer user_data;

  func = tdata->func;
  user_data = tdata->user_data;
  g_free (tdata);

  func (user_data);
}

static void
default_prepare (GstTaskPool * pool, GError ** error)
{
  GST_OBJECT_LOCK (pool);
  pool->pool = g_thread_pool_new ((GFunc) default_func, pool, -1, FALSE, error);
  GST_OBJECT_UNLOCK (pool);
}

static void
default_cleanup (GstTaskPool * pool)
{
  GThreadPool *pool_;

  GST_OBJECT_LOCK (pool);
  pool_ = pool->pool;
  pool->pool = NULL;
  GST_OBJECT_UNLOCK (pool);

  if (pool_) {
    /* Shut down all the threads, we still process the ones scheduled
     * because the unref happens in the thread function.
     * Also wait for currently running ones to finish. */
    g_thread_pool_free (pool_, FALSE, TRUE);
  }
}

static gpointer
default_push (GstTaskPool * pool, GstTaskPoolFunction func,
    gpointer user_data, GError ** error)
{
  TaskData *tdata;

  tdata = g_new (TaskData, 1);
  tdata->func = func;
  tdata->user_data = user_data;

  GST_OBJECT_LOCK (pool);
  if (pool->pool)
    g_thread_pool_push (pool->pool, tdata, error);
  else {
    g_free (tdata);
    g_set_error_literal (error, GST_CORE_ERROR, GST_CORE_ERROR_FAILED,
        "No thread pool");
  }
  GST_OBJECT_UNLOCK (pool);

  return NULL;
}

static void
default_join (GstTaskPool * pool, gpointer id)
{
  /* we do nothing here, we can't join from the pools */
}

static void
default_dispose_handle (GstTaskPool * pool, gpointer id)
{
  /* we do nothing here, the default handle is NULL */
}

static void
gst_task_pool_class_init (GstTaskPoolClass * klass)
{
  GObjectClass *gobject_class;
  GstTaskPoolClass *gsttaskpool_class;

  gobject_class = (GObjectClass *) klass;
  gsttaskpool_class = (GstTaskPoolClass *) klass;

#ifndef GST_DISABLE_GST_DEBUG
  gobject_class->finalize = gst_task_pool_finalize;
#endif

  gsttaskpool_class->prepare = default_prepare;
  gsttaskpool_class->cleanup = default_cleanup;
  gsttaskpool_class->push = default_push;
  gsttaskpool_class->join = default_join;
  gsttaskpool_class->dispose_handle = default_dispose_handle;
}

static void
gst_task_pool_init (GstTaskPool * pool)
{
}

#ifndef GST_DISABLE_GST_DEBUG
static void
gst_task_pool_finalize (GObject * object)
{
  GST_DEBUG ("taskpool %p finalize", object);

  G_OBJECT_CLASS (gst_task_pool_parent_class)->finalize (object);
}
#endif
/**
 * gst_task_pool_new:
 *
 * Create a new default task pool. The default task pool will use a regular
 * GThreadPool for threads.
 *
 * Returns: (transfer full): a new #GstTaskPool. gst_object_unref() after usage.
 */
GstTaskPool *
gst_task_pool_new (void)
{
  GstTaskPool *pool;

  pool = g_object_new (GST_TYPE_TASK_POOL, NULL);

  /* clear floating flag */
  gst_object_ref_sink (pool);

  return pool;
}

/**
 * gst_task_pool_prepare:
 * @pool: a #GstTaskPool
 * @error: an error return location
 *
 * Prepare the taskpool for accepting gst_task_pool_push() operations.
 *
 * MT safe.
 */
void
gst_task_pool_prepare (GstTaskPool * pool, GError ** error)
{
  GstTaskPoolClass *klass;

  g_return_if_fail (GST_IS_TASK_POOL (pool));

  klass = GST_TASK_POOL_GET_CLASS (pool);

  if (klass->prepare)
    klass->prepare (pool, error);
}

/**
 * gst_task_pool_cleanup:
 * @pool: a #GstTaskPool
 *
 * Wait for all tasks to be stopped. This is mainly used internally
 * to ensure proper cleanup of internal data structures in test suites.
 *
 * MT safe.
 */
void
gst_task_pool_cleanup (GstTaskPool * pool)
{
  GstTaskPoolClass *klass;

  g_return_if_fail (GST_IS_TASK_POOL (pool));

  klass = GST_TASK_POOL_GET_CLASS (pool);

  if (klass->cleanup)
    klass->cleanup (pool);
}

/**
 * gst_task_pool_push:
 * @pool: a #GstTaskPool
 * @func: (scope async): the function to call
 * @user_data: (closure): data to pass to @func
 * @error: return location for an error
 *
 * Start the execution of a new thread from @pool.
 *
 * Returns: (transfer full) (nullable): a pointer that should be used
 * for the gst_task_pool_join function. This pointer can be %NULL, you
 * must check @error to detect errors. If the pointer is not %NULL and
 * gst_task_pool_join() is not used, call gst_task_pool_dispose_handle()
 * instead.
 */
gpointer
gst_task_pool_push (GstTaskPool * pool, GstTaskPoolFunction func,
    gpointer user_data, GError ** error)
{
  GstTaskPoolClass *klass;

  g_return_val_if_fail (GST_IS_TASK_POOL (pool), NULL);

  klass = GST_TASK_POOL_GET_CLASS (pool);

  if (klass->push == NULL)
    goto not_supported;

  return klass->push (pool, func, user_data, error);

  /* ERRORS */
not_supported:
  {
    g_warning ("pushing tasks on pool %p is not supported", pool);
    return NULL;
  }
}

/**
 * gst_task_pool_join:
 * @pool: a #GstTaskPool
 * @id: (transfer full) (nullable): the id
 *
 * Join a task and/or return it to the pool. @id is the id obtained from
 * gst_task_pool_push(). The default implementation does nothing, as the
 * default #GstTaskPoolClass::push implementation always returns %NULL.
 *
 * This method should only be called with the same @pool instance that provided
 * @id.
 */
void
gst_task_pool_join (GstTaskPool * pool, gpointer id)
{
  GstTaskPoolClass *klass;

  g_return_if_fail (GST_IS_TASK_POOL (pool));

  klass = GST_TASK_POOL_GET_CLASS (pool);

  if (klass->join)
    klass->join (pool, id);
}

/**
 * gst_task_pool_dispose_handle:
 * @pool: a #GstTaskPool
 * @id: (transfer full) (nullable): the id
 *
 * Dispose of the handle returned by gst_task_pool_push(). This does
 * not need to be called with the default implementation as the default
 * #GstTaskPoolClass::push implementation always returns %NULL. This does not need to be
 * called either when calling gst_task_pool_join(), but should be called
 * when joining is not necessary, but gst_task_pool_push() returned a
 * non-%NULL value.
 *
 * This method should only be called with the same @pool instance that provided
 * @id.
 *
 * Since: 1.20
 */
void
gst_task_pool_dispose_handle (GstTaskPool * pool, gpointer id)
{
  GstTaskPoolClass *klass;

  g_return_if_fail (GST_IS_TASK_POOL (pool));

  klass = GST_TASK_POOL_GET_CLASS (pool);

  if (klass->dispose_handle)
    klass->dispose_handle (pool, id);
}

typedef struct
{
  gboolean done;
  guint64 id;
  GstTaskPoolFunction func;
  gpointer user_data;
  GMutex done_lock;
  GCond done_cond;
  gint refcount;
} SharedTaskData;

static SharedTaskData *
shared_task_data_ref (SharedTaskData * tdata)
{
  g_atomic_int_add (&tdata->refcount, 1);

  return tdata;
}

static void
shared_task_data_unref (SharedTaskData * tdata)
{
  if (g_atomic_int_dec_and_test (&tdata->refcount)) {
    g_mutex_clear (&tdata->done_lock);
    g_cond_clear (&tdata->done_cond);
    g_free (tdata);
  }
}

struct _GstSharedTaskPoolPrivate
{
  guint max_threads;
};

#define GST_SHARED_TASK_POOL_CAST(pool)       ((GstSharedTaskPool*)(pool))

G_DEFINE_TYPE_WITH_PRIVATE (GstSharedTaskPool, gst_shared_task_pool,
    GST_TYPE_TASK_POOL);

static void
shared_func (SharedTaskData * tdata, GstTaskPool * pool)
{
  tdata->func (tdata->user_data);

  g_mutex_lock (&tdata->done_lock);
  tdata->done = TRUE;
  g_cond_signal (&tdata->done_cond);
  g_mutex_unlock (&tdata->done_lock);

  shared_task_data_unref (tdata);
}

static gpointer
shared_push (GstTaskPool * pool, GstTaskPoolFunction func,
    gpointer user_data, GError ** error)
{
  SharedTaskData *ret = NULL;

  GST_OBJECT_LOCK (pool);

  if (!pool->pool) {
    GST_OBJECT_UNLOCK (pool);
    goto done;
  }

  ret = g_new (SharedTaskData, 1);

  ret->done = FALSE;
  ret->func = func;
  ret->user_data = user_data;
  g_atomic_int_set (&ret->refcount, 1);
  g_cond_init (&ret->done_cond);
  g_mutex_init (&ret->done_lock);

  g_thread_pool_push (pool->pool, shared_task_data_ref (ret), error);

  GST_OBJECT_UNLOCK (pool);

done:
  return ret;
}

static void
shared_join (GstTaskPool * pool, gpointer id)
{
  SharedTaskData *tdata;

  if (!id)
    return;

  tdata = (SharedTaskData *) id;

  g_mutex_lock (&tdata->done_lock);
  while (!tdata->done) {
    g_cond_wait (&tdata->done_cond, &tdata->done_lock);
  }
  g_mutex_unlock (&tdata->done_lock);

  shared_task_data_unref (tdata);
}

static void
shared_dispose_handle (GstTaskPool * pool, gpointer id)
{
  SharedTaskData *tdata;

  if (!id)
    return;

  tdata = (SharedTaskData *) id;


  shared_task_data_unref (tdata);
}

static void
shared_prepare (GstTaskPool * pool, GError ** error)
{
  GstSharedTaskPool *shared_pool = GST_SHARED_TASK_POOL_CAST (pool);

  GST_OBJECT_LOCK (pool);
  pool->pool =
      g_thread_pool_new ((GFunc) shared_func, pool,
      shared_pool->priv->max_threads, FALSE, error);
  GST_OBJECT_UNLOCK (pool);
}

static void
gst_shared_task_pool_class_init (GstSharedTaskPoolClass * klass)
{
  GstTaskPoolClass *taskpoolclass = GST_TASK_POOL_CLASS (klass);

  taskpoolclass->prepare = shared_prepare;
  taskpoolclass->push = shared_push;
  taskpoolclass->join = shared_join;
  taskpoolclass->dispose_handle = shared_dispose_handle;
}

static void
gst_shared_task_pool_init (GstSharedTaskPool * pool)
{
  GstSharedTaskPoolPrivate *priv;

  priv = pool->priv = gst_shared_task_pool_get_instance_private (pool);
  priv->max_threads = 1;
}

/**
 * gst_shared_task_pool_set_max_threads:
 * @pool: a #GstSharedTaskPool
 * @max_threads: Maximum number of threads to spawn.
 *
 * Update the maximal number of threads the @pool may spawn. When
 * the maximal number of threads is reduced, existing threads are not
 * immediately shut down, see g_thread_pool_set_max_threads().
 *
 * Setting @max_threads to 0 effectively freezes the pool.
 *
 * Since: 1.20
 */
void
gst_shared_task_pool_set_max_threads (GstSharedTaskPool * pool,
    guint max_threads)
{
  GstTaskPool *taskpool;

  g_return_if_fail (GST_IS_SHARED_TASK_POOL (pool));

  taskpool = GST_TASK_POOL (pool);

  GST_OBJECT_LOCK (pool);
  if (taskpool->pool)
    g_thread_pool_set_max_threads (taskpool->pool, max_threads, NULL);
  pool->priv->max_threads = max_threads;
  GST_OBJECT_UNLOCK (pool);
}

/**
 * gst_shared_task_pool_get_max_threads:
 * @pool: a #GstSharedTaskPool
 *
 * Returns: the maximum number of threads @pool is configured to spawn
 * Since: 1.20
 */
guint
gst_shared_task_pool_get_max_threads (GstSharedTaskPool * pool)
{
  guint ret;

  g_return_val_if_fail (GST_IS_SHARED_TASK_POOL (pool), 0);

  GST_OBJECT_LOCK (pool);
  ret = pool->priv->max_threads;
  GST_OBJECT_UNLOCK (pool);

  return ret;
}

/**
 * gst_shared_task_pool_new:
 *
 * Create a new shared task pool. The shared task pool will queue tasks on
 * a maximum number of threads, 1 by default.
 *
 * Do not use a #GstSharedTaskPool to manage potentially inter-dependent tasks such
 * as pad tasks, as having one task waiting on another to return before returning
 * would cause obvious deadlocks if they happen to share the same thread.
 *
 * Returns: (transfer full): a new #GstSharedTaskPool. gst_object_unref() after usage.
 * Since: 1.20
 */
GstTaskPool *
gst_shared_task_pool_new (void)
{
  GstTaskPool *pool;

  pool = g_object_new (GST_TYPE_SHARED_TASK_POOL, NULL);

  /* clear floating flag */
  gst_object_ref_sink (pool);

  return pool;
}
