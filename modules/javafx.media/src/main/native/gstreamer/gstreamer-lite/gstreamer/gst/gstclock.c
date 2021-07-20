/* GStreamer
 * Copyright (C) 1999,2000 Erik Walthinsen <omega@cse.ogi.edu>
 *                    2000 Wim Taymans <wtay@chello.be>
 *                    2004 Wim Taymans <wim@fluendo.com>
 *
 * gstclock.c: Clock subsystem for maintaining time sync
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
 * SECTION:gstclock
 * @title: GstClock
 * @short_description: Abstract class for global clocks
 * @see_also: #GstSystemClock, #GstPipeline
 *
 * GStreamer uses a global clock to synchronize the plugins in a pipeline.
 * Different clock implementations are possible by implementing this abstract
 * base class or, more conveniently, by subclassing #GstSystemClock.
 *
 * The #GstClock returns a monotonically increasing time with the method
 * gst_clock_get_time(). Its accuracy and base time depend on the specific
 * clock implementation but time is always expressed in nanoseconds. Since the
 * baseline of the clock is undefined, the clock time returned is not
 * meaningful in itself, what matters are the deltas between two clock times.
 * The time returned by a clock is called the absolute time.
 *
 * The pipeline uses the clock to calculate the running time. Usually all
 * renderers synchronize to the global clock using the buffer timestamps, the
 * newsegment events and the element's base time, see #GstPipeline.
 *
 * A clock implementation can support periodic and single shot clock
 * notifications both synchronous and asynchronous.
 *
 * One first needs to create a #GstClockID for the periodic or single shot
 * notification using gst_clock_new_single_shot_id() or
 * gst_clock_new_periodic_id().
 *
 * To perform a blocking wait for the specific time of the #GstClockID use the
 * gst_clock_id_wait(). To receive a callback when the specific time is reached
 * in the clock use gst_clock_id_wait_async(). Both these calls can be
 * interrupted with the gst_clock_id_unschedule() call. If the blocking wait is
 * unscheduled a return value of #GST_CLOCK_UNSCHEDULED is returned.
 *
 * Periodic callbacks scheduled async will be repeatedly called automatically
 * until it is unscheduled. To schedule a sync periodic callback,
 * gst_clock_id_wait() should be called repeatedly.
 *
 * The async callbacks can happen from any thread, either provided by the core
 * or from a streaming thread. The application should be prepared for this.
 *
 * A #GstClockID that has been unscheduled cannot be used again for any wait
 * operation, a new #GstClockID should be created and the old unscheduled one
 * should be destroyed with gst_clock_id_unref().
 *
 * It is possible to perform a blocking wait on the same #GstClockID from
 * multiple threads. However, registering the same #GstClockID for multiple
 * async notifications is not possible, the callback will only be called for
 * the thread registering the entry last.
 *
 * None of the wait operations unref the #GstClockID, the owner is responsible
 * for unreffing the ids itself. This holds for both periodic and single shot
 * notifications. The reason being that the owner of the #GstClockID has to
 * keep a handle to the #GstClockID to unblock the wait on FLUSHING events or
 * state changes and if the entry would be unreffed automatically, the handle
 * might become invalid without any notification.
 *
 * These clock operations do not operate on the running time, so the callbacks
 * will also occur when not in PLAYING state as if the clock just keeps on
 * running. Some clocks however do not progress when the element that provided
 * the clock is not PLAYING.
 *
 * When a clock has the #GST_CLOCK_FLAG_CAN_SET_MASTER flag set, it can be
 * slaved to another #GstClock with the gst_clock_set_master(). The clock will
 * then automatically be synchronized to this master clock by repeatedly
 * sampling the master clock and the slave clock and recalibrating the slave
 * clock with gst_clock_set_calibration(). This feature is mostly useful for
 * plugins that have an internal clock but must operate with another clock
 * selected by the #GstPipeline.  They can track the offset and rate difference
 * of their internal clock relative to the master clock by using the
 * gst_clock_get_calibration() function.
 *
 * The master/slave synchronisation can be tuned with the #GstClock:timeout,
 * #GstClock:window-size and #GstClock:window-threshold properties.
 * The #GstClock:timeout property defines the interval to sample the master
 * clock and run the calibration functions. #GstClock:window-size defines the
 * number of samples to use when calibrating and #GstClock:window-threshold
 * defines the minimum number of samples before the calibration is performed.
 */

#include "gst_private.h"
#include <time.h>

#include "gstclock.h"
#include "gstinfo.h"
#include "gstutils.h"
#include "glib-compat-private.h"

/* #define DEBUGGING_ENABLED */

#define DEFAULT_WINDOW_SIZE             32
#define DEFAULT_WINDOW_THRESHOLD        4
#define DEFAULT_TIMEOUT                 GST_SECOND / 10

enum
{
  PROP_0,
  PROP_WINDOW_SIZE,
  PROP_WINDOW_THRESHOLD,
  PROP_TIMEOUT
};

enum
{
  SIGNAL_SYNCED,
  SIGNAL_LAST
};

#define GST_CLOCK_SLAVE_LOCK(clock)     g_mutex_lock (&GST_CLOCK_CAST (clock)->priv->slave_lock)
#define GST_CLOCK_SLAVE_UNLOCK(clock)   g_mutex_unlock (&GST_CLOCK_CAST (clock)->priv->slave_lock)

struct _GstClockPrivate
{
  GMutex slave_lock;            /* order: SLAVE_LOCK, OBJECT_LOCK */

  GCond sync_cond;

  /* with LOCK */
  GstClockTime internal_calibration;
  GstClockTime external_calibration;
  GstClockTime rate_numerator;
  GstClockTime rate_denominator;
  GstClockTime last_time;

  /* with LOCK */
  GstClockTime resolution;

  /* for master/slave clocks */
  GstClock *master;

  /* with SLAVE_LOCK */
  gboolean filling;
  gint window_size;
  gint window_threshold;
  gint time_index;
  GstClockTime timeout;
  GstClockTime *times;
  GstClockTime *times_temp;
  GstClockID clockid;

  gint pre_count;
  gint post_count;

  gboolean synced;
};

typedef struct _GstClockEntryImpl GstClockEntryImpl;

#define GST_CLOCK_ENTRY_CLOCK_WEAK_REF(entry) (&((GstClockEntryImpl *)(entry))->clock)

/* seqlocks */
#define read_seqbegin(clock)                                   \
  g_atomic_int_get (&clock->priv->post_count);

static inline gboolean
read_seqretry (GstClock * clock, gint seq)
{
  /* no retry if the seqnum did not change */
  if (G_LIKELY (seq == g_atomic_int_get (&clock->priv->pre_count)))
    return FALSE;

  /* wait for the writer to finish and retry */
  GST_OBJECT_LOCK (clock);
  GST_OBJECT_UNLOCK (clock);
  return TRUE;
}

#define write_seqlock(clock)                      \
G_STMT_START {                                    \
  GST_OBJECT_LOCK (clock);                        \
  g_atomic_int_inc (&clock->priv->pre_count);     \
} G_STMT_END;

#define write_sequnlock(clock)                    \
G_STMT_START {                                    \
  g_atomic_int_inc (&clock->priv->post_count);    \
  GST_OBJECT_UNLOCK (clock);                      \
} G_STMT_END;

#ifndef GST_DISABLE_GST_DEBUG
static const gchar *
gst_clock_return_get_name (GstClockReturn ret)
{
  switch (ret) {
    case GST_CLOCK_OK:
      return "ok";
    case GST_CLOCK_EARLY:
      return "early";
    case GST_CLOCK_UNSCHEDULED:
      return "unscheduled";
    case GST_CLOCK_BUSY:
      return "busy";
    case GST_CLOCK_BADTIME:
      return "bad-time";
    case GST_CLOCK_ERROR:
      return "error";
    case GST_CLOCK_UNSUPPORTED:
      return "unsupported";
    case GST_CLOCK_DONE:
      return "done";
    default:
      break;
  }

  return "unknown";
}
#endif /* GST_DISABLE_GST_DEBUG */

static void gst_clock_dispose (GObject * object);
static void gst_clock_finalize (GObject * object);

static void gst_clock_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec);
static void gst_clock_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec);

static guint gst_clock_signals[SIGNAL_LAST] = { 0 };

static GstClockID
gst_clock_entry_new (GstClock * clock, GstClockTime time,
    GstClockTime interval, GstClockEntryType type)
{
  GstClockEntry *entry;

  entry = (GstClockEntry *) g_slice_new0 (GstClockEntryImpl);

  /* FIXME: add tracer hook for struct allocations such as clock entries */

  GST_CAT_DEBUG_OBJECT (GST_CAT_CLOCK, clock,
      "created entry %p, time %" GST_TIME_FORMAT, entry, GST_TIME_ARGS (time));

  entry->refcount = 1;
#ifndef GST_REMOVE_DEPRECATED
#ifndef GST_DISABLE_DEPRECATED
  entry->clock = clock;
#else
  entry->_clock = clock;
#endif
#endif
  g_weak_ref_init (GST_CLOCK_ENTRY_CLOCK_WEAK_REF (entry), clock);
  entry->type = type;
  entry->time = time;
  entry->interval = interval;
  entry->status = GST_CLOCK_OK;
  entry->func = NULL;
  entry->user_data = NULL;
  entry->destroy_data = NULL;
  entry->unscheduled = FALSE;
  entry->woken_up = FALSE;

  return (GstClockID) entry;
}

/* WARNING : Does not modify the refcount
 * WARNING : Do not use if a pending clock operation is happening on that entry */
static gboolean
gst_clock_entry_reinit (GstClock * clock, GstClockEntry * entry,
    GstClockTime time, GstClockTime interval, GstClockEntryType type)
{
  g_return_val_if_fail (entry->status != GST_CLOCK_BUSY, FALSE);
  g_return_val_if_fail (gst_clock_id_uses_clock ((GstClockID) entry, clock),
      FALSE);

  entry->type = type;
  entry->time = time;
  entry->interval = interval;
  entry->status = GST_CLOCK_OK;
  entry->unscheduled = FALSE;
  entry->woken_up = FALSE;

  return TRUE;
}

/**
 * gst_clock_single_shot_id_reinit:
 * @clock: a #GstClock
 * @id: a #GstClockID
 * @time: The requested time.
 *
 * Reinitializes the provided single shot @id to the provided time. Does not
 * modify the reference count.
 *
 * Returns: %TRUE if the GstClockID could be reinitialized to the provided
 * @time, else %FALSE.
 */
gboolean
gst_clock_single_shot_id_reinit (GstClock * clock, GstClockID id,
    GstClockTime time)
{
  return gst_clock_entry_reinit (clock, (GstClockEntry *) id, time,
      GST_CLOCK_TIME_NONE, GST_CLOCK_ENTRY_SINGLE);
}

/**
 * gst_clock_periodic_id_reinit:
 * @clock: a #GstClock
 * @id: a #GstClockID
 * @start_time: the requested start time
 * @interval: the requested interval
 *
 * Reinitializes the provided periodic @id to the provided start time and
 * interval. Does not modify the reference count.
 *
 * Returns: %TRUE if the GstClockID could be reinitialized to the provided
 * @time, else %FALSE.
 */
gboolean
gst_clock_periodic_id_reinit (GstClock * clock, GstClockID id,
    GstClockTime start_time, GstClockTime interval)
{
  return gst_clock_entry_reinit (clock, (GstClockEntry *) id, start_time,
      interval, GST_CLOCK_ENTRY_PERIODIC);
}

/**
 * gst_clock_id_ref:
 * @id: The #GstClockID to ref
 *
 * Increase the refcount of given @id.
 *
 * Returns: (transfer full): The same #GstClockID with increased refcount.
 *
 * MT safe.
 */
GstClockID
gst_clock_id_ref (GstClockID id)
{
  g_return_val_if_fail (id != NULL, NULL);

  g_atomic_int_inc (&((GstClockEntry *) id)->refcount);

  return id;
}

static void
_gst_clock_id_free (GstClockID id)
{
  GstClockEntry *entry;
  GstClockEntryImpl *entry_impl;
  g_return_if_fail (id != NULL);

  GST_CAT_DEBUG (GST_CAT_CLOCK, "freed entry %p", id);
  entry = (GstClockEntry *) id;
  if (entry->destroy_data)
    entry->destroy_data (entry->user_data);

  entry_impl = (GstClockEntryImpl *) id;
  if (entry_impl->destroy_entry)
    entry_impl->destroy_entry (entry_impl);

  g_weak_ref_clear (GST_CLOCK_ENTRY_CLOCK_WEAK_REF (entry));

  /* FIXME: add tracer hook for struct allocations such as clock entries */

  g_slice_free (GstClockEntryImpl, (GstClockEntryImpl *) id);
}

/**
 * gst_clock_id_unref:
 * @id: (transfer full): The #GstClockID to unref
 *
 * Unref given @id. When the refcount reaches 0 the
 * #GstClockID will be freed.
 *
 * MT safe.
 */
void
gst_clock_id_unref (GstClockID id)
{
  gint zero;

  g_return_if_fail (id != NULL);

  zero = g_atomic_int_dec_and_test (&((GstClockEntry *) id)->refcount);
  /* if we ended up with the refcount at zero, free the id */
  if (zero) {
    _gst_clock_id_free (id);
  }
}

/**
 * gst_clock_new_single_shot_id:
 * @clock: The #GstClockID to get a single shot notification from
 * @time: the requested time
 *
 * Get a #GstClockID from @clock to trigger a single shot
 * notification at the requested time. The single shot id should be
 * unreffed after usage.
 *
 * Free-function: gst_clock_id_unref
 *
 * Returns: (transfer full): a #GstClockID that can be used to request the
 *     time notification.
 *
 * MT safe.
 */
GstClockID
gst_clock_new_single_shot_id (GstClock * clock, GstClockTime time)
{
  g_return_val_if_fail (GST_IS_CLOCK (clock), NULL);

  return gst_clock_entry_new (clock,
      time, GST_CLOCK_TIME_NONE, GST_CLOCK_ENTRY_SINGLE);
}

/**
 * gst_clock_new_periodic_id:
 * @clock: The #GstClockID to get a periodic notification id from
 * @start_time: the requested start time
 * @interval: the requested interval
 *
 * Get an ID from @clock to trigger a periodic notification.
 * The periodic notifications will start at time @start_time and
 * will then be fired with the given @interval. @id should be unreffed
 * after usage.
 *
 * Free-function: gst_clock_id_unref
 *
 * Returns: (transfer full): a #GstClockID that can be used to request the
 *     time notification.
 *
 * MT safe.
 */
GstClockID
gst_clock_new_periodic_id (GstClock * clock, GstClockTime start_time,
    GstClockTime interval)
{
  g_return_val_if_fail (GST_IS_CLOCK (clock), NULL);
  g_return_val_if_fail (GST_CLOCK_TIME_IS_VALID (start_time), NULL);
  g_return_val_if_fail (interval != 0, NULL);
  g_return_val_if_fail (GST_CLOCK_TIME_IS_VALID (interval), NULL);

  return gst_clock_entry_new (clock,
      start_time, interval, GST_CLOCK_ENTRY_PERIODIC);
}

/**
 * gst_clock_id_compare_func:
 * @id1: A #GstClockID
 * @id2: A #GstClockID to compare with
 *
 * Compares the two #GstClockID instances. This function can be used
 * as a GCompareFunc when sorting ids.
 *
 * Returns: negative value if a < b; zero if a = b; positive value if a > b
 *
 * MT safe.
 */
gint
gst_clock_id_compare_func (gconstpointer id1, gconstpointer id2)
{
  GstClockEntry *entry1, *entry2;

  entry1 = (GstClockEntry *) id1;
  entry2 = (GstClockEntry *) id2;

  if (GST_CLOCK_ENTRY_TIME (entry1) > GST_CLOCK_ENTRY_TIME (entry2)) {
    return 1;
  }
  if (GST_CLOCK_ENTRY_TIME (entry1) < GST_CLOCK_ENTRY_TIME (entry2)) {
    return -1;
  }
  return 0;
}

/**
 * gst_clock_id_get_time:
 * @id: The #GstClockID to query
 *
 * Get the time of the clock ID
 *
 * Returns: the time of the given clock id.
 *
 * MT safe.
 */
GstClockTime
gst_clock_id_get_time (GstClockID id)
{
  g_return_val_if_fail (id != NULL, GST_CLOCK_TIME_NONE);

  return GST_CLOCK_ENTRY_TIME ((GstClockEntry *) id);
}

/**
 * gst_clock_id_wait:
 * @id: The #GstClockID to wait on
 * @jitter: (out) (allow-none): a pointer that will contain the jitter,
 *     can be %NULL.
 *
 * Perform a blocking wait on @id.
 * @id should have been created with gst_clock_new_single_shot_id()
 * or gst_clock_new_periodic_id() and should not have been unscheduled
 * with a call to gst_clock_id_unschedule().
 *
 * If the @jitter argument is not %NULL and this function returns #GST_CLOCK_OK
 * or #GST_CLOCK_EARLY, it will contain the difference
 * against the clock and the time of @id when this method was
 * called.
 * Positive values indicate how late @id was relative to the clock
 * (in which case this function will return #GST_CLOCK_EARLY).
 * Negative values indicate how much time was spent waiting on the clock
 * before this function returned.
 *
 * Returns: the result of the blocking wait. #GST_CLOCK_EARLY will be returned
 * if the current clock time is past the time of @id, #GST_CLOCK_OK if
 * @id was scheduled in time. #GST_CLOCK_UNSCHEDULED if @id was
 * unscheduled with gst_clock_id_unschedule().
 *
 * MT safe.
 */
GstClockReturn
gst_clock_id_wait (GstClockID id, GstClockTimeDiff * jitter)
{
  GstClockEntry *entry;
  GstClock *clock;
  GstClockReturn res;
  GstClockTime requested;
  GstClockClass *cclass;

  g_return_val_if_fail (id != NULL, GST_CLOCK_ERROR);

  entry = (GstClockEntry *) id;
  requested = GST_CLOCK_ENTRY_TIME (entry);

  clock = g_weak_ref_get (GST_CLOCK_ENTRY_CLOCK_WEAK_REF (entry));
  if (G_UNLIKELY (clock == NULL))
    goto invalid_entry;

  /* can't sync on invalid times */
  if (G_UNLIKELY (!GST_CLOCK_TIME_IS_VALID (requested)))
    goto invalid_time;

  cclass = GST_CLOCK_GET_CLASS (clock);

  GST_CAT_DEBUG_OBJECT (GST_CAT_CLOCK, clock, "waiting on clock entry %p", id);

  /* if we have a wait_jitter function, use that */
  if (G_UNLIKELY (cclass->wait == NULL))
    goto not_supported;

  res = cclass->wait (clock, entry, jitter);

  GST_CAT_DEBUG_OBJECT (GST_CAT_CLOCK, clock,
      "done waiting entry %p, res: %d (%s)", id, res,
      gst_clock_return_get_name (res));

  if (entry->type == GST_CLOCK_ENTRY_PERIODIC)
    entry->time = requested + entry->interval;

  gst_object_unref (clock);
  return res;

  /* ERRORS */
invalid_time:
  {
    GST_CAT_DEBUG_OBJECT (GST_CAT_CLOCK, clock,
        "invalid time requested, returning _BADTIME");
    gst_object_unref (clock);
    return GST_CLOCK_BADTIME;
  }
not_supported:
  {
    GST_CAT_DEBUG_OBJECT (GST_CAT_CLOCK, clock, "clock wait is not supported");
    gst_object_unref (clock);
    return GST_CLOCK_UNSUPPORTED;
  }
invalid_entry:
  {
    GST_CAT_DEBUG (GST_CAT_CLOCK, "clock entry %p lost its clock", id);
    return GST_CLOCK_ERROR;
  }
}

/**
 * gst_clock_id_wait_async:
 * @id: a #GstClockID to wait on
 * @func: The callback function
 * @user_data: User data passed in the callback
 * @destroy_data: #GDestroyNotify for user_data
 *
 * Register a callback on the given #GstClockID @id with the given
 * function and user_data. When passing a #GstClockID with an invalid
 * time to this function, the callback will be called immediately
 * with  a time set to GST_CLOCK_TIME_NONE. The callback will
 * be called when the time of @id has been reached.
 *
 * The callback @func can be invoked from any thread, either provided by the
 * core or from a streaming thread. The application should be prepared for this.
 *
 * Returns: the result of the non blocking wait.
 *
 * MT safe.
 */
GstClockReturn
gst_clock_id_wait_async (GstClockID id,
    GstClockCallback func, gpointer user_data, GDestroyNotify destroy_data)
{
  GstClockEntry *entry;
  GstClock *clock;
  GstClockReturn res;
  GstClockClass *cclass;
  GstClockTime requested;

  g_return_val_if_fail (id != NULL, GST_CLOCK_ERROR);
  g_return_val_if_fail (func != NULL, GST_CLOCK_ERROR);

  entry = (GstClockEntry *) id;
  requested = GST_CLOCK_ENTRY_TIME (entry);
  clock = g_weak_ref_get (GST_CLOCK_ENTRY_CLOCK_WEAK_REF (entry));
  if (G_UNLIKELY (clock == NULL))
    goto invalid_entry;

  /* can't sync on invalid times */
  if (G_UNLIKELY (!GST_CLOCK_TIME_IS_VALID (requested)))
    goto invalid_time;

  cclass = GST_CLOCK_GET_CLASS (clock);

  if (G_UNLIKELY (cclass->wait_async == NULL))
    goto not_supported;

  entry->func = func;
  entry->user_data = user_data;
  entry->destroy_data = destroy_data;

  res = cclass->wait_async (clock, entry);

  gst_object_unref (clock);
  return res;

  /* ERRORS */
invalid_time:
  {
    (func) (clock, GST_CLOCK_TIME_NONE, id, user_data);
    GST_CAT_DEBUG_OBJECT (GST_CAT_CLOCK, clock,
        "invalid time requested, returning _BADTIME");
    gst_object_unref (clock);
    return GST_CLOCK_BADTIME;
  }
not_supported:
  {
    GST_CAT_DEBUG_OBJECT (GST_CAT_CLOCK, clock, "clock wait is not supported");
    gst_object_unref (clock);
    return GST_CLOCK_UNSUPPORTED;
  }
invalid_entry:
  {
    GST_CAT_DEBUG (GST_CAT_CLOCK, "clock entry %p lost its clock", id);
    return GST_CLOCK_ERROR;
  }
}

/**
 * gst_clock_id_unschedule:
 * @id: The id to unschedule
 *
 * Cancel an outstanding request with @id. This can either
 * be an outstanding async notification or a pending sync notification.
 * After this call, @id cannot be used anymore to receive sync or
 * async notifications, you need to create a new #GstClockID.
 *
 * MT safe.
 */
void
gst_clock_id_unschedule (GstClockID id)
{
  GstClockEntry *entry;
  GstClock *clock;
  GstClockClass *cclass;

  g_return_if_fail (id != NULL);

  entry = (GstClockEntry *) id;
  clock = g_weak_ref_get (GST_CLOCK_ENTRY_CLOCK_WEAK_REF (entry));
  if (G_UNLIKELY (clock == NULL))
    goto invalid_entry;

  cclass = GST_CLOCK_GET_CLASS (clock);

  if (G_LIKELY (cclass->unschedule))
    cclass->unschedule (clock, entry);

  gst_object_unref (clock);
  return;

invalid_entry:
  {
    GST_CAT_DEBUG (GST_CAT_CLOCK, "clock entry %p lost its clock", id);
    return;
  }
}


/*
 * GstClock abstract base class implementation
 */
#define gst_clock_parent_class parent_class
G_DEFINE_ABSTRACT_TYPE_WITH_PRIVATE (GstClock, gst_clock, GST_TYPE_OBJECT);

static void
gst_clock_class_init (GstClockClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gobject_class->dispose = gst_clock_dispose;
  gobject_class->finalize = gst_clock_finalize;
  gobject_class->set_property = gst_clock_set_property;
  gobject_class->get_property = gst_clock_get_property;

  g_object_class_install_property (gobject_class, PROP_WINDOW_SIZE,
      g_param_spec_int ("window-size", "Window size",
          "The size of the window used to calculate rate and offset", 2, 1024,
          DEFAULT_WINDOW_SIZE, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_WINDOW_THRESHOLD,
      g_param_spec_int ("window-threshold", "Window threshold",
          "The threshold to start calculating rate and offset", 2, 1024,
          DEFAULT_WINDOW_THRESHOLD,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_TIMEOUT,
      g_param_spec_uint64 ("timeout", "Timeout",
          "The amount of time, in nanoseconds, to sample master and slave clocks",
          0, G_MAXUINT64, DEFAULT_TIMEOUT,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  /**
   * GstClock::synced:
   * @clock: the clock
   * @synced: if the clock is synced now
   *
   * Signaled on clocks with GST_CLOCK_FLAG_NEEDS_STARTUP_SYNC set once
   * the clock is synchronized, or when it completely lost synchronization.
   * This signal will not be emitted on clocks without the flag.
   *
   * This signal will be emitted from an arbitrary thread, most likely not
   * the application's main thread.
   *
   * Since: 1.6
   */
  gst_clock_signals[SIGNAL_SYNCED] =
      g_signal_new ("synced", G_TYPE_FROM_CLASS (klass), G_SIGNAL_RUN_LAST,
      0, NULL, NULL, NULL, G_TYPE_NONE, 1, G_TYPE_BOOLEAN);
}

static void
gst_clock_init (GstClock * clock)
{
  GstClockPrivate *priv;

  clock->priv = priv = gst_clock_get_instance_private (clock);

  priv->last_time = 0;

  priv->internal_calibration = 0;
  priv->external_calibration = 0;
  priv->rate_numerator = 1;
  priv->rate_denominator = 1;

  g_mutex_init (&priv->slave_lock);
  g_cond_init (&priv->sync_cond);
  priv->window_size = DEFAULT_WINDOW_SIZE;
  priv->window_threshold = DEFAULT_WINDOW_THRESHOLD;
  priv->filling = TRUE;
  priv->time_index = 0;
  priv->timeout = DEFAULT_TIMEOUT;
  priv->times = g_new0 (GstClockTime, 4 * priv->window_size);
  priv->times_temp = priv->times + 2 * priv->window_size;
}

static void
gst_clock_dispose (GObject * object)
{
  GstClock *clock = GST_CLOCK (object);
  GstClock **master_p;

  GST_OBJECT_LOCK (clock);
  master_p = &clock->priv->master;
  gst_object_replace ((GstObject **) master_p, NULL);
  GST_OBJECT_UNLOCK (clock);

  G_OBJECT_CLASS (parent_class)->dispose (object);
}

static void
gst_clock_finalize (GObject * object)
{
  GstClock *clock = GST_CLOCK (object);

  GST_CLOCK_SLAVE_LOCK (clock);
  if (clock->priv->clockid) {
    gst_clock_id_unschedule (clock->priv->clockid);
    gst_clock_id_unref (clock->priv->clockid);
    clock->priv->clockid = NULL;
  }
  g_free (clock->priv->times);
  clock->priv->times = NULL;
  clock->priv->times_temp = NULL;
  GST_CLOCK_SLAVE_UNLOCK (clock);

  g_mutex_clear (&clock->priv->slave_lock);
  g_cond_clear (&clock->priv->sync_cond);

  G_OBJECT_CLASS (parent_class)->finalize (object);
}

/**
 * gst_clock_set_resolution:
 * @clock: a #GstClock
 * @resolution: The resolution to set
 *
 * Set the accuracy of the clock. Some clocks have the possibility to operate
 * with different accuracy at the expense of more resource usage. There is
 * normally no need to change the default resolution of a clock. The resolution
 * of a clock can only be changed if the clock has the
 * #GST_CLOCK_FLAG_CAN_SET_RESOLUTION flag set.
 *
 * Returns: the new resolution of the clock.
 */
GstClockTime
gst_clock_set_resolution (GstClock * clock, GstClockTime resolution)
{
  GstClockPrivate *priv;
  GstClockClass *cclass;

  g_return_val_if_fail (GST_IS_CLOCK (clock), 0);
  g_return_val_if_fail (resolution != 0, 0);

  cclass = GST_CLOCK_GET_CLASS (clock);
  priv = clock->priv;

  if (cclass->change_resolution)
    priv->resolution =
        cclass->change_resolution (clock, priv->resolution, resolution);

  return priv->resolution;
}

/**
 * gst_clock_get_resolution:
 * @clock: a #GstClock
 *
 * Get the accuracy of the clock. The accuracy of the clock is the granularity
 * of the values returned by gst_clock_get_time().
 *
 * Returns: the resolution of the clock in units of #GstClockTime.
 *
 * MT safe.
 */
GstClockTime
gst_clock_get_resolution (GstClock * clock)
{
  GstClockClass *cclass;

  g_return_val_if_fail (GST_IS_CLOCK (clock), 0);

  cclass = GST_CLOCK_GET_CLASS (clock);

  if (cclass->get_resolution)
    return cclass->get_resolution (clock);

  return 1;
}

/* FIXME 2.0: Remove clock parameter below */
/**
 * gst_clock_adjust_with_calibration:
 * @clock: (allow-none): a #GstClock to use
 * @internal_target: a clock time
 * @cinternal: a reference internal time
 * @cexternal: a reference external time
 * @cnum: the numerator of the rate of the clock relative to its
 *        internal time
 * @cdenom: the denominator of the rate of the clock
 *
 * Converts the given @internal_target clock time to the external time,
 * using the passed calibration parameters. This function performs the
 * same calculation as gst_clock_adjust_unlocked() when called using the
 * current calibration parameters, but doesn't ensure a monotonically
 * increasing result as gst_clock_adjust_unlocked() does.
 *
 * Note: The @clock parameter is unused and can be NULL
 *
 * Returns: the converted time of the clock.
 *
 * Since: 1.6
 */
GstClockTime
gst_clock_adjust_with_calibration (GstClock * clock,
    GstClockTime internal_target, GstClockTime cinternal,
    GstClockTime cexternal, GstClockTime cnum, GstClockTime cdenom)
{
  GstClockTime ret;

  /* avoid divide by 0 */
  if (G_UNLIKELY (cdenom == 0))
    cnum = cdenom = 1;

  /* The formula is (internal - cinternal) * cnum / cdenom + cexternal
   *
   * Since we do math on unsigned 64-bit ints we have to special case for
   * internal < cinternal to get the sign right. this case is not very common,
   * though.
   */
  if (G_LIKELY (internal_target >= cinternal)) {
    ret = internal_target - cinternal;
    ret = gst_util_uint64_scale (ret, cnum, cdenom);
    ret += cexternal;
  } else {
    ret = cinternal - internal_target;
    ret = gst_util_uint64_scale (ret, cnum, cdenom);
    /* clamp to 0 */
    if (G_LIKELY (cexternal > ret))
      ret = cexternal - ret;
    else
      ret = 0;
  }

  return ret;
}

/**
 * gst_clock_adjust_unlocked:
 * @clock: a #GstClock to use
 * @internal: a clock time
 *
 * Converts the given @internal clock time to the external time, adjusting for the
 * rate and reference time set with gst_clock_set_calibration() and making sure
 * that the returned time is increasing. This function should be called with the
 * clock's OBJECT_LOCK held and is mainly used by clock subclasses.
 *
 * This function is the reverse of gst_clock_unadjust_unlocked().
 *
 * Returns: the converted time of the clock.
 */
GstClockTime
gst_clock_adjust_unlocked (GstClock * clock, GstClockTime internal)
{
  GstClockTime ret, cinternal, cexternal, cnum, cdenom;
  GstClockPrivate *priv = clock->priv;

  /* get calibration values for readability */
  cinternal = priv->internal_calibration;
  cexternal = priv->external_calibration;
  cnum = priv->rate_numerator;
  cdenom = priv->rate_denominator;

  ret =
      gst_clock_adjust_with_calibration (clock, internal, cinternal, cexternal,
      cnum, cdenom);

  /* make sure the time is increasing */
  priv->last_time = MAX (ret, priv->last_time);

  return priv->last_time;
}

/* FIXME 2.0: Remove clock parameter below */
/**
 * gst_clock_unadjust_with_calibration:
 * @clock: (allow-none): a #GstClock to use
 * @external_target: a clock time
 * @cinternal: a reference internal time
 * @cexternal: a reference external time
 * @cnum: the numerator of the rate of the clock relative to its
 *        internal time
 * @cdenom: the denominator of the rate of the clock
 *
 * Converts the given @external_target clock time to the internal time,
 * using the passed calibration parameters. This function performs the
 * same calculation as gst_clock_unadjust_unlocked() when called using the
 * current calibration parameters.
 *
 * Note: The @clock parameter is unused and can be NULL
 *
 * Returns: the converted time of the clock.
 *
 * Since: 1.8
 */
GstClockTime
gst_clock_unadjust_with_calibration (GstClock * clock,
    GstClockTime external_target, GstClockTime cinternal,
    GstClockTime cexternal, GstClockTime cnum, GstClockTime cdenom)
{
  GstClockTime ret;

  /* avoid divide by 0 */
  if (G_UNLIKELY (cnum == 0))
    cnum = cdenom = 1;

  /* The formula is (external - cexternal) * cdenom / cnum + cinternal */
  if (G_LIKELY (external_target >= cexternal)) {
    ret = external_target - cexternal;
    ret = gst_util_uint64_scale (ret, cdenom, cnum);
    ret += cinternal;
  } else {
    ret = cexternal - external_target;
    ret = gst_util_uint64_scale (ret, cdenom, cnum);
    if (G_LIKELY (cinternal > ret))
      ret = cinternal - ret;
    else
      ret = 0;
  }

  return ret;
}

/**
 * gst_clock_unadjust_unlocked:
 * @clock: a #GstClock to use
 * @external: an external clock time
 *
 * Converts the given @external clock time to the internal time of @clock,
 * using the rate and reference time set with gst_clock_set_calibration().
 * This function should be called with the clock's OBJECT_LOCK held and
 * is mainly used by clock subclasses.
 *
 * This function is the reverse of gst_clock_adjust_unlocked().
 *
 * Returns: the internal time of the clock corresponding to @external.
 */
GstClockTime
gst_clock_unadjust_unlocked (GstClock * clock, GstClockTime external)
{
  GstClockTime cinternal, cexternal, cnum, cdenom;
  GstClockPrivate *priv = clock->priv;

  /* get calibration values for readability */
  cinternal = priv->internal_calibration;
  cexternal = priv->external_calibration;
  cnum = priv->rate_numerator;
  cdenom = priv->rate_denominator;

  return gst_clock_unadjust_with_calibration (clock, external, cinternal,
      cexternal, cnum, cdenom);
}

/**
 * gst_clock_get_internal_time:
 * @clock: a #GstClock to query
 *
 * Gets the current internal time of the given clock. The time is returned
 * unadjusted for the offset and the rate.
 *
 * Returns: the internal time of the clock. Or GST_CLOCK_TIME_NONE when
 * given invalid input.
 *
 * MT safe.
 */
GstClockTime
gst_clock_get_internal_time (GstClock * clock)
{
  GstClockTime ret;
  GstClockClass *cclass;

  g_return_val_if_fail (GST_IS_CLOCK (clock), GST_CLOCK_TIME_NONE);

  if (G_UNLIKELY (GST_OBJECT_FLAG_IS_SET (clock,
              GST_CLOCK_FLAG_NEEDS_STARTUP_SYNC) && !clock->priv->synced))
    GST_CAT_WARNING_OBJECT (GST_CAT_CLOCK, clock,
        "clock is not synchronized yet");

  cclass = GST_CLOCK_GET_CLASS (clock);

  if (G_UNLIKELY (cclass->get_internal_time == NULL))
    goto not_supported;

  ret = cclass->get_internal_time (clock);

  GST_CAT_DEBUG_OBJECT (GST_CAT_CLOCK, clock, "internal time %" GST_TIME_FORMAT,
      GST_TIME_ARGS (ret));

  return ret;

  /* ERRORS */
not_supported:
  {
    GST_CAT_DEBUG_OBJECT (GST_CAT_CLOCK, clock,
        "internal time not supported, return 0");
    return G_GINT64_CONSTANT (0);
  }
}

/**
 * gst_clock_get_time:
 * @clock: a #GstClock to query
 *
 * Gets the current time of the given clock. The time is always
 * monotonically increasing and adjusted according to the current
 * offset and rate.
 *
 * Returns: the time of the clock. Or GST_CLOCK_TIME_NONE when
 * given invalid input.
 *
 * MT safe.
 */
GstClockTime
gst_clock_get_time (GstClock * clock)
{
  GstClockTime ret;
  gint seq;

  g_return_val_if_fail (GST_IS_CLOCK (clock), GST_CLOCK_TIME_NONE);

  do {
    /* reget the internal time when we retry to get the most current
     * timevalue */
    ret = gst_clock_get_internal_time (clock);

    seq = read_seqbegin (clock);
    /* this will scale for rate and offset */
    ret = gst_clock_adjust_unlocked (clock, ret);
  } while (read_seqretry (clock, seq));

  GST_CAT_DEBUG_OBJECT (GST_CAT_CLOCK, clock, "adjusted time %" GST_TIME_FORMAT,
      GST_TIME_ARGS (ret));

  return ret;
}

/**
 * gst_clock_set_calibration:
 * @clock: a #GstClock to calibrate
 * @internal: a reference internal time
 * @external: a reference external time
 * @rate_num: the numerator of the rate of the clock relative to its
 *            internal time
 * @rate_denom: the denominator of the rate of the clock
 *
 * Adjusts the rate and time of @clock. A rate of 1/1 is the normal speed of
 * the clock. Values bigger than 1/1 make the clock go faster.
 *
 * @internal and @external are calibration parameters that arrange that
 * gst_clock_get_time() should have been @external at internal time @internal.
 * This internal time should not be in the future; that is, it should be less
 * than the value of gst_clock_get_internal_time() when this function is called.
 *
 * Subsequent calls to gst_clock_get_time() will return clock times computed as
 * follows:
 *
 * |[
 *   time = (internal_time - internal) * rate_num / rate_denom + external
 * ]|
 *
 * This formula is implemented in gst_clock_adjust_unlocked(). Of course, it
 * tries to do the integer arithmetic as precisely as possible.
 *
 * Note that gst_clock_get_time() always returns increasing values so when you
 * move the clock backwards, gst_clock_get_time() will report the previous value
 * until the clock catches up.
 *
 * MT safe.
 */
void
gst_clock_set_calibration (GstClock * clock, GstClockTime internal, GstClockTime
    external, GstClockTime rate_num, GstClockTime rate_denom)
{
  GstClockPrivate *priv;

  g_return_if_fail (GST_IS_CLOCK (clock));
  g_return_if_fail (rate_num != GST_CLOCK_TIME_NONE);
  g_return_if_fail (rate_denom > 0 && rate_denom != GST_CLOCK_TIME_NONE);

  priv = clock->priv;

  write_seqlock (clock);
  GST_CAT_DEBUG_OBJECT (GST_CAT_CLOCK, clock,
      "internal %" GST_TIME_FORMAT " external %" GST_TIME_FORMAT " %"
      G_GUINT64_FORMAT "/%" G_GUINT64_FORMAT " = %f", GST_TIME_ARGS (internal),
      GST_TIME_ARGS (external), rate_num, rate_denom,
      gst_guint64_to_gdouble (rate_num) / gst_guint64_to_gdouble (rate_denom));

  priv->internal_calibration = internal;
  priv->external_calibration = external;
  priv->rate_numerator = rate_num;
  priv->rate_denominator = rate_denom;
  write_sequnlock (clock);
}

/**
 * gst_clock_get_calibration:
 * @clock: a #GstClock
 * @internal: (out) (allow-none): a location to store the internal time
 * @external: (out) (allow-none): a location to store the external time
 * @rate_num: (out) (allow-none): a location to store the rate numerator
 * @rate_denom: (out) (allow-none): a location to store the rate denominator
 *
 * Gets the internal rate and reference time of @clock. See
 * gst_clock_set_calibration() for more information.
 *
 * @internal, @external, @rate_num, and @rate_denom can be left %NULL if the
 * caller is not interested in the values.
 *
 * MT safe.
 */
void
gst_clock_get_calibration (GstClock * clock, GstClockTime * internal,
    GstClockTime * external, GstClockTime * rate_num, GstClockTime * rate_denom)
{
  gint seq;
  GstClockPrivate *priv;

  g_return_if_fail (GST_IS_CLOCK (clock));

  priv = clock->priv;

  do {
    seq = read_seqbegin (clock);
    if (rate_num)
      *rate_num = priv->rate_numerator;
    if (rate_denom)
      *rate_denom = priv->rate_denominator;
    if (external)
      *external = priv->external_calibration;
    if (internal)
      *internal = priv->internal_calibration;
  } while (read_seqretry (clock, seq));
}

/* will be called repeatedly to sample the master and slave clock
 * to recalibrate the clock  */
static gboolean
gst_clock_slave_callback (GstClock * master, GstClockTime time,
    GstClockID id, GstClock * clock)
{
  GstClockTime stime, mtime;
  gdouble r_squared;

  if (!gst_clock_is_synced (clock)) {
    GST_CAT_DEBUG_OBJECT (GST_CAT_CLOCK, clock,
        "Slave clock is not synced yet");
    return TRUE;
  }

  stime = gst_clock_get_internal_time (clock);
  mtime = gst_clock_get_time (master);

  GST_CAT_DEBUG_OBJECT (GST_CAT_CLOCK, clock,
      "master %" GST_TIME_FORMAT ", slave %" GST_TIME_FORMAT,
      GST_TIME_ARGS (mtime), GST_TIME_ARGS (stime));

  gst_clock_add_observation (clock, stime, mtime, &r_squared);

  /* FIXME, we can use the r_squared value to adjust the timeout
   * value of the clockid */

  return TRUE;
}

/**
 * gst_clock_set_master:
 * @clock: a #GstClock
 * @master: (allow-none): a master #GstClock
 *
 * Set @master as the master clock for @clock. @clock will be automatically
 * calibrated so that gst_clock_get_time() reports the same time as the
 * master clock.
 *
 * A clock provider that slaves its clock to a master can get the current
 * calibration values with gst_clock_get_calibration().
 *
 * @master can be %NULL in which case @clock will not be slaved anymore. It will
 * however keep reporting its time adjusted with the last configured rate
 * and time offsets.
 *
 * Returns: %TRUE if the clock is capable of being slaved to a master clock.
 * Trying to set a master on a clock without the
 * #GST_CLOCK_FLAG_CAN_SET_MASTER flag will make this function return %FALSE.
 *
 * MT safe.
 */
gboolean
gst_clock_set_master (GstClock * clock, GstClock * master)
{
  GstClock **master_p;
  GstClockPrivate *priv;

  g_return_val_if_fail (GST_IS_CLOCK (clock), FALSE);
  g_return_val_if_fail (master != clock, FALSE);

  GST_OBJECT_LOCK (clock);
  /* we always allow setting the master to NULL */
  if (master && !GST_OBJECT_FLAG_IS_SET (clock, GST_CLOCK_FLAG_CAN_SET_MASTER))
    goto not_supported;
  if (master && !gst_clock_is_synced (master))
    goto master_not_synced;

  GST_CAT_DEBUG_OBJECT (GST_CAT_CLOCK, clock,
      "slaving %p to master clock %p", clock, master);
  GST_OBJECT_UNLOCK (clock);

  priv = clock->priv;

  GST_CLOCK_SLAVE_LOCK (clock);
  if (priv->clockid) {
    gst_clock_id_unschedule (priv->clockid);
    gst_clock_id_unref (priv->clockid);
    priv->clockid = NULL;
  }
  if (master) {
    priv->filling = TRUE;
    priv->time_index = 0;
    /* use the master periodic id to schedule sampling and
     * clock calibration. */
    priv->clockid = gst_clock_new_periodic_id (master,
        gst_clock_get_time (master), priv->timeout);
    gst_clock_id_wait_async (priv->clockid,
        (GstClockCallback) gst_clock_slave_callback,
        gst_object_ref (clock), (GDestroyNotify) gst_object_unref);
  }
  GST_CLOCK_SLAVE_UNLOCK (clock);

  GST_OBJECT_LOCK (clock);
  master_p = &priv->master;
  gst_object_replace ((GstObject **) master_p, (GstObject *) master);
  GST_OBJECT_UNLOCK (clock);

  return TRUE;

  /* ERRORS */
not_supported:
  {
    GST_CAT_DEBUG_OBJECT (GST_CAT_CLOCK, clock,
        "cannot be slaved to a master clock");
    GST_OBJECT_UNLOCK (clock);
    return FALSE;
  }

master_not_synced:
  {
    GST_CAT_DEBUG_OBJECT (GST_CAT_CLOCK, master,
        "master clock is not synced yet");
    GST_OBJECT_UNLOCK (clock);
    return FALSE;
  }
}

/**
 * gst_clock_get_master:
 * @clock: a #GstClock
 *
 * Get the master clock that @clock is slaved to or %NULL when the clock is
 * not slaved to any master clock.
 *
 * Returns: (transfer full) (nullable): a master #GstClock or %NULL
 *     when this clock is not slaved to a master clock. Unref after
 *     usage.
 *
 * MT safe.
 */
GstClock *
gst_clock_get_master (GstClock * clock)
{
  GstClock *result = NULL;
  GstClockPrivate *priv;

  g_return_val_if_fail (GST_IS_CLOCK (clock), NULL);

  priv = clock->priv;

  GST_OBJECT_LOCK (clock);
  if (priv->master)
    result = gst_object_ref (priv->master);
  GST_OBJECT_UNLOCK (clock);

  return result;
}

/**
 * gst_clock_id_get_clock:
 * @id: a #GstClockID
 *
 * This function returns the underlying clock.
 *
 * Returns: (transfer full) (nullable): a #GstClock or %NULL when the
 *     underlying clock has been freed.  Unref after usage.
 *
 * MT safe.
 *
 * Since: 1.16
 */
GstClock *
gst_clock_id_get_clock (GstClockID id)
{
  GstClockEntry *entry;

  g_return_val_if_fail (id != NULL, NULL);

  entry = (GstClockEntry *) id;
  return g_weak_ref_get (GST_CLOCK_ENTRY_CLOCK_WEAK_REF (entry));
}

/**
 * gst_clock_id_uses_clock:
 * @id: a #GstClockID to check
 * @clock: a #GstClock to compare against
 *
 * This function returns whether @id uses @clock as the underlying clock.
 * @clock can be NULL, in which case the return value indicates whether
 * the underlying clock has been freed.  If this is the case, the @id is
 * no longer usable and should be freed.
 *
 * Returns: whether the clock @id uses the same underlying #GstClock @clock.
 *
 * MT safe.
 *
 * Since: 1.16
 */
gboolean
gst_clock_id_uses_clock (GstClockID id, GstClock * clock)
{
  GstClockEntry *entry;
  GstClock *entry_clock;
  gboolean ret = FALSE;

  g_return_val_if_fail (id != NULL, FALSE);
  g_return_val_if_fail (clock != NULL, FALSE);

  entry = (GstClockEntry *) id;
  entry_clock = g_weak_ref_get (GST_CLOCK_ENTRY_CLOCK_WEAK_REF (entry));
  if (entry_clock == clock)
    ret = TRUE;

  if (G_LIKELY (entry_clock != NULL))
    gst_object_unref (entry_clock);

  return ret;
}


/**
 * gst_clock_add_observation:
 * @clock: a #GstClock
 * @slave: a time on the slave
 * @master: a time on the master
 * @r_squared: (out): a pointer to hold the result
 *
 * The time @master of the master clock and the time @slave of the slave
 * clock are added to the list of observations. If enough observations
 * are available, a linear regression algorithm is run on the
 * observations and @clock is recalibrated.
 *
 * If this functions returns %TRUE, @r_squared will contain the
 * correlation coefficient of the interpolation. A value of 1.0
 * means a perfect regression was performed. This value can
 * be used to control the sampling frequency of the master and slave
 * clocks.
 *
 * Returns: %TRUE if enough observations were added to run the
 * regression algorithm.
 *
 * MT safe.
 */
gboolean
gst_clock_add_observation (GstClock * clock, GstClockTime slave,
    GstClockTime master, gdouble * r_squared)
{
  GstClockTime m_num, m_denom, b, xbase;

  if (!gst_clock_add_observation_unapplied (clock, slave, master, r_squared,
          &xbase, &b, &m_num, &m_denom))
    return FALSE;

  /* if we have a valid regression, adjust the clock */
  gst_clock_set_calibration (clock, xbase, b, m_num, m_denom);

  return TRUE;
}

/**
 * gst_clock_add_observation_unapplied:
 * @clock: a #GstClock
 * @slave: a time on the slave
 * @master: a time on the master
 * @r_squared: (out): a pointer to hold the result
 * @internal: (out) (allow-none): a location to store the internal time
 * @external: (out) (allow-none): a location to store the external time
 * @rate_num: (out) (allow-none): a location to store the rate numerator
 * @rate_denom: (out) (allow-none): a location to store the rate denominator
 *
 * Add a clock observation to the internal slaving algorithm the same as
 * gst_clock_add_observation(), and return the result of the master clock
 * estimation, without updating the internal calibration.
 *
 * The caller can then take the results and call gst_clock_set_calibration()
 * with the values, or some modified version of them.
 *
 * Since: 1.6
 */
gboolean
gst_clock_add_observation_unapplied (GstClock * clock, GstClockTime slave,
    GstClockTime master, gdouble * r_squared,
    GstClockTime * internal, GstClockTime * external,
    GstClockTime * rate_num, GstClockTime * rate_denom)
{
  GstClockTime m_num, m_denom, b, xbase;
  GstClockPrivate *priv;
  guint n;

  g_return_val_if_fail (GST_IS_CLOCK (clock), FALSE);
  g_return_val_if_fail (r_squared != NULL, FALSE);

  priv = clock->priv;

  GST_CLOCK_SLAVE_LOCK (clock);

  GST_CAT_LOG_OBJECT (GST_CAT_CLOCK, clock,
      "adding observation slave %" GST_TIME_FORMAT ", master %" GST_TIME_FORMAT,
      GST_TIME_ARGS (slave), GST_TIME_ARGS (master));

  priv->times[(2 * priv->time_index)] = slave;
  priv->times[(2 * priv->time_index) + 1] = master;

  priv->time_index++;
  if (G_UNLIKELY (priv->time_index == priv->window_size)) {
    priv->filling = FALSE;
    priv->time_index = 0;
  }

  if (G_UNLIKELY (priv->filling && priv->time_index < priv->window_threshold))
    goto filling;

  n = priv->filling ? priv->time_index : priv->window_size;
  if (!gst_calculate_linear_regression (priv->times, priv->times_temp, n,
          &m_num, &m_denom, &b, &xbase, r_squared))
    goto invalid;

  GST_CLOCK_SLAVE_UNLOCK (clock);

  GST_CAT_LOG_OBJECT (GST_CAT_CLOCK, clock,
      "adjusting clock to m=%" G_GUINT64_FORMAT "/%" G_GUINT64_FORMAT ", b=%"
      G_GUINT64_FORMAT " (rsquared=%g)", m_num, m_denom, b, *r_squared);

  if (internal)
    *internal = xbase;
  if (external)
    *external = b;
  if (rate_num)
    *rate_num = m_num;
  if (rate_denom)
    *rate_denom = m_denom;

  return TRUE;

filling:
  {
    GST_CLOCK_SLAVE_UNLOCK (clock);
    return FALSE;
  }
invalid:
  {
    /* no valid regression has been done, ignore the result then */
    GST_CLOCK_SLAVE_UNLOCK (clock);
    return FALSE;
  }
}

/**
 * gst_clock_set_timeout:
 * @clock: a #GstClock
 * @timeout: a timeout
 *
 * Set the amount of time, in nanoseconds, to sample master and slave
 * clocks
 */
void
gst_clock_set_timeout (GstClock * clock, GstClockTime timeout)
{
  g_return_if_fail (GST_IS_CLOCK (clock));

  GST_CLOCK_SLAVE_LOCK (clock);
  clock->priv->timeout = timeout;
  GST_CLOCK_SLAVE_UNLOCK (clock);
}

/**
 * gst_clock_get_timeout:
 * @clock: a #GstClock
 *
 * Get the amount of time that master and slave clocks are sampled.
 *
 * Returns: the interval between samples.
 */
GstClockTime
gst_clock_get_timeout (GstClock * clock)
{
  GstClockTime result;

  g_return_val_if_fail (GST_IS_CLOCK (clock), GST_CLOCK_TIME_NONE);

  GST_CLOCK_SLAVE_LOCK (clock);
  result = clock->priv->timeout;
  GST_CLOCK_SLAVE_UNLOCK (clock);

  return result;
}

static void
gst_clock_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstClock *clock;
  GstClockPrivate *priv;

  clock = GST_CLOCK (object);
  priv = clock->priv;

  switch (prop_id) {
    case PROP_WINDOW_SIZE:
      GST_CLOCK_SLAVE_LOCK (clock);
      priv->window_size = g_value_get_int (value);
      priv->window_threshold = MIN (priv->window_threshold, priv->window_size);
      priv->times = g_renew (GstClockTime, priv->times, 4 * priv->window_size);
      priv->times_temp = priv->times + 2 * priv->window_size;
      /* restart calibration */
      priv->filling = TRUE;
      priv->time_index = 0;
      GST_CLOCK_SLAVE_UNLOCK (clock);
      break;
    case PROP_WINDOW_THRESHOLD:
      GST_CLOCK_SLAVE_LOCK (clock);
      priv->window_threshold = MIN (g_value_get_int (value), priv->window_size);
      GST_CLOCK_SLAVE_UNLOCK (clock);
      break;
    case PROP_TIMEOUT:
      gst_clock_set_timeout (clock, g_value_get_uint64 (value));
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_clock_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  GstClock *clock;
  GstClockPrivate *priv;

  clock = GST_CLOCK (object);
  priv = clock->priv;

  switch (prop_id) {
    case PROP_WINDOW_SIZE:
      GST_CLOCK_SLAVE_LOCK (clock);
      g_value_set_int (value, priv->window_size);
      GST_CLOCK_SLAVE_UNLOCK (clock);
      break;
    case PROP_WINDOW_THRESHOLD:
      GST_CLOCK_SLAVE_LOCK (clock);
      g_value_set_int (value, priv->window_threshold);
      GST_CLOCK_SLAVE_UNLOCK (clock);
      break;
    case PROP_TIMEOUT:
      g_value_set_uint64 (value, gst_clock_get_timeout (clock));
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}


/**
 * gst_clock_wait_for_sync:
 * @clock: a GstClock
 * @timeout: timeout for waiting or %GST_CLOCK_TIME_NONE
 *
 * Waits until @clock is synced for reporting the current time. If @timeout
 * is %GST_CLOCK_TIME_NONE it will wait forever, otherwise it will time out
 * after @timeout nanoseconds.
 *
 * For asynchronous waiting, the GstClock::synced signal can be used.
 *
 * This returns immediately with TRUE if GST_CLOCK_FLAG_NEEDS_STARTUP_SYNC
 * is not set on the clock, or if the clock is already synced.
 *
 * Returns: %TRUE if waiting was successful, or %FALSE on timeout
 *
 * Since: 1.6
 */
gboolean
gst_clock_wait_for_sync (GstClock * clock, GstClockTime timeout)
{
  gboolean timed_out = FALSE;

  g_return_val_if_fail (GST_IS_CLOCK (clock), FALSE);

  GST_OBJECT_LOCK (clock);
  if (!GST_OBJECT_FLAG_IS_SET (clock, GST_CLOCK_FLAG_NEEDS_STARTUP_SYNC)
      || clock->priv->synced) {
    GST_OBJECT_UNLOCK (clock);
    return TRUE;
  }

  if (timeout != GST_CLOCK_TIME_NONE) {
    gint64 end_time = g_get_monotonic_time () + gst_util_uint64_scale (timeout,
        G_TIME_SPAN_SECOND, GST_SECOND);

    while (!clock->priv->synced && !timed_out) {
      timed_out =
          !g_cond_wait_until (&clock->priv->sync_cond,
          GST_OBJECT_GET_LOCK (clock), end_time);
    }
  } else {
    timed_out = FALSE;
    while (!clock->priv->synced) {
      g_cond_wait (&clock->priv->sync_cond, GST_OBJECT_GET_LOCK (clock));
    }
  }
  GST_OBJECT_UNLOCK (clock);

  return !timed_out;
}

/**
 * gst_clock_is_synced:
 * @clock: a GstClock
 *
 * Checks if the clock is currently synced.
 *
 * This returns if GST_CLOCK_FLAG_NEEDS_STARTUP_SYNC is not set on the clock.
 *
 * Returns: %TRUE if the clock is currently synced
 *
 * Since: 1.6
 */
gboolean
gst_clock_is_synced (GstClock * clock)
{
  g_return_val_if_fail (GST_IS_CLOCK (clock), TRUE);

  return !GST_OBJECT_FLAG_IS_SET (clock, GST_CLOCK_FLAG_NEEDS_STARTUP_SYNC)
      || clock->priv->synced;
}

/**
 * gst_clock_set_synced:
 * @clock: a GstClock
 * @synced: if the clock is synced
 *
 * Sets @clock to synced and emits the GstClock::synced signal, and wakes up any
 * thread waiting in gst_clock_wait_for_sync().
 *
 * This function must only be called if GST_CLOCK_FLAG_NEEDS_STARTUP_SYNC
 * is set on the clock, and is intended to be called by subclasses only.
 *
 * Since: 1.6
 */
void
gst_clock_set_synced (GstClock * clock, gboolean synced)
{
  g_return_if_fail (GST_IS_CLOCK (clock));
  g_return_if_fail (GST_OBJECT_FLAG_IS_SET (clock,
          GST_CLOCK_FLAG_NEEDS_STARTUP_SYNC));

  GST_OBJECT_LOCK (clock);
  if (clock->priv->synced != ! !synced) {
    clock->priv->synced = ! !synced;
    g_cond_signal (&clock->priv->sync_cond);
    GST_OBJECT_UNLOCK (clock);
    g_signal_emit (clock, gst_clock_signals[SIGNAL_SYNCED], 0, ! !synced);
  } else {
    GST_OBJECT_UNLOCK (clock);
  }
}
