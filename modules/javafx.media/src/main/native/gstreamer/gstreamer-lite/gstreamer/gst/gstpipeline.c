/* GStreamer
 * Copyright (C) 1999,2000 Erik Walthinsen <omega@cse.ogi.edu>
 *                    2004,2005 Wim Taymans <wim@fluendo.com>
 *
 * gstpipeline.c: Overall pipeline management element
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
 * SECTION:gstpipeline
 * @title: GstPipeline
 * @short_description: Top-level bin with clocking and bus management
                       functionality.
 * @see_also: #GstElement, #GstBin, #GstClock, #GstBus
 *
 * A #GstPipeline is a special #GstBin used as the toplevel container for
 * the filter graph. The #GstPipeline will manage the selection and
 * distribution of a global #GstClock as well as provide a #GstBus to the
 * application.
 *
 * gst_pipeline_new() is used to create a pipeline. when you are done with
 * the pipeline, use gst_object_unref() to free its resources including all
 * added #GstElement objects (if not otherwise referenced).
 *
 * Elements are added and removed from the pipeline using the #GstBin
 * methods like gst_bin_add() and gst_bin_remove() (see #GstBin).
 *
 * Before changing the state of the #GstPipeline (see #GstElement) a #GstBus
 * can be retrieved with gst_pipeline_get_bus(). This bus can then be
 * used to receive #GstMessage from the elements in the pipeline.
 *
 * By default, a #GstPipeline will automatically flush the pending #GstBus
 * messages when going to the NULL state to ensure that no circular
 * references exist when no messages are read from the #GstBus. This
 * behaviour can be changed with gst_pipeline_set_auto_flush_bus().
 *
 * When the #GstPipeline performs the PAUSED to PLAYING state change it will
 * select a clock for the elements. The clock selection algorithm will by
 * default select a clock provided by an element that is most upstream
 * (closest to the source). For live pipelines (ones that return
 * #GST_STATE_CHANGE_NO_PREROLL from the gst_element_set_state() call) this
 * will select the clock provided by the live source. For normal pipelines
 * this will select a clock provided by the sinks (most likely the audio
 * sink). If no element provides a clock, a default #GstSystemClock is used.
 *
 * The clock selection can be controlled with the gst_pipeline_use_clock()
 * method, which will enforce a given clock on the pipeline. With
 * gst_pipeline_auto_clock() the default clock selection algorithm can be
 * restored.
 *
 * A #GstPipeline maintains a running time for the elements. The running
 * time is defined as the difference between the current clock time and
 * the base time. When the pipeline goes to READY or a flushing seek is
 * performed on it, the running time is reset to 0. When the pipeline is
 * set from PLAYING to PAUSED, the current clock time is sampled and used to
 * configure the base time for the elements when the pipeline is set
 * to PLAYING again. The effect is that the running time (as the difference
 * between the clock time and the base time) will count how much time was spent
 * in the PLAYING state. This default behaviour can be changed with the
 * gst_element_set_start_time() method.
 */

#include "gst_private.h"
#include "gsterror.h"
#include "gst-i18n-lib.h"

#include "gstpipeline.h"
#include "gstinfo.h"
#include "gstsystemclock.h"
#include "gstutils.h"

GST_DEBUG_CATEGORY_STATIC (pipeline_debug);
#define GST_CAT_DEFAULT pipeline_debug

/* Pipeline signals and args */
enum
{
  /* FILL ME */
  LAST_SIGNAL
};

#define DEFAULT_DELAY           0
#define DEFAULT_AUTO_FLUSH_BUS  TRUE
#define DEFAULT_LATENCY         GST_CLOCK_TIME_NONE

enum
{
  PROP_0,
  PROP_DELAY,
  PROP_AUTO_FLUSH_BUS,
  PROP_LATENCY
};

struct _GstPipelinePrivate
{
  /* with LOCK */
  gboolean auto_flush_bus;
  gboolean is_live;

  /* when we need to update stream_time or clock when going back to
   * PLAYING*/
  GstClockTime last_start_time;
  gboolean update_clock;

  GstClockTime latency;

  /* seqnum of the most recent instant-rate-request, %GST_SEQNUM_INVALID if none */
  guint32 instant_rate_seqnum;
  gdouble active_instant_rate;
  GstClockTime instant_rate_upstream_anchor;
  GstClockTime instant_rate_clock_anchor;
};


static void gst_pipeline_dispose (GObject * object);
static void gst_pipeline_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec);
static void gst_pipeline_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec);

static GstClock *gst_pipeline_provide_clock_func (GstElement * element);
static GstStateChangeReturn gst_pipeline_change_state (GstElement * element,
    GstStateChange transition);

static void gst_pipeline_handle_message (GstBin * bin, GstMessage * message);
static gboolean gst_pipeline_do_latency (GstBin * bin);
static gboolean gst_pipeline_handle_instant_rate (GstPipeline * pipeline,
    gdouble rate, guint32 seqnum);

/* static guint gst_pipeline_signals[LAST_SIGNAL] = { 0 }; */

#define _do_init \
{ \
  GST_DEBUG_CATEGORY_INIT (pipeline_debug, "pipeline", GST_DEBUG_BOLD, \
      "debugging info for the 'pipeline' container element"); \
}

#define gst_pipeline_parent_class parent_class
G_DEFINE_TYPE_WITH_CODE (GstPipeline, gst_pipeline, GST_TYPE_BIN,
    G_ADD_PRIVATE (GstPipeline) _do_init);

static void
gst_pipeline_class_init (GstPipelineClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);
  GstBinClass *gstbin_class = GST_BIN_CLASS (klass);

  gobject_class->set_property = gst_pipeline_set_property;
  gobject_class->get_property = gst_pipeline_get_property;

  /**
   * GstPipeline:delay:
   *
   * The expected delay needed for elements to spin up to the
   * PLAYING state expressed in nanoseconds.
   * see gst_pipeline_set_delay() for more information on this option.
   **/
  g_object_class_install_property (gobject_class, PROP_DELAY,
      g_param_spec_uint64 ("delay", "Delay",
          "Expected delay needed for elements "
          "to spin up to PLAYING in nanoseconds", 0, G_MAXUINT64, DEFAULT_DELAY,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  /**
   * GstPipeline:auto-flush-bus:
   *
   * Whether or not to automatically flush all messages on the
   * pipeline's bus when going from READY to NULL state. Please see
   * gst_pipeline_set_auto_flush_bus() for more information on this option.
   **/
  g_object_class_install_property (gobject_class, PROP_AUTO_FLUSH_BUS,
      g_param_spec_boolean ("auto-flush-bus", "Auto Flush Bus",
          "Whether to automatically flush the pipeline's bus when going "
          "from READY into NULL state", DEFAULT_AUTO_FLUSH_BUS,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  /**
   * GstPipeline:latency:
   *
   * Latency to configure on the pipeline. See gst_pipeline_set_latency().
   *
   * Since: 1.6
   **/
  g_object_class_install_property (gobject_class, PROP_LATENCY,
      g_param_spec_uint64 ("latency", "Latency",
          "Latency to configure on the pipeline", 0, G_MAXUINT64,
          DEFAULT_LATENCY, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  gobject_class->dispose = gst_pipeline_dispose;

  gst_element_class_set_static_metadata (gstelement_class, "Pipeline object",
      "Generic/Bin",
      "Complete pipeline object",
      "Erik Walthinsen <omega@cse.ogi.edu>, Wim Taymans <wim@fluendo.com>");

  gstelement_class->change_state =
      GST_DEBUG_FUNCPTR (gst_pipeline_change_state);
  gstelement_class->provide_clock =
      GST_DEBUG_FUNCPTR (gst_pipeline_provide_clock_func);
  gstbin_class->handle_message =
      GST_DEBUG_FUNCPTR (gst_pipeline_handle_message);
  gstbin_class->do_latency = GST_DEBUG_FUNCPTR (gst_pipeline_do_latency);
}

static void
gst_pipeline_init (GstPipeline * pipeline)
{
  GstBus *bus;

  pipeline->priv = gst_pipeline_get_instance_private (pipeline);

  /* set default property values */
  pipeline->priv->auto_flush_bus = DEFAULT_AUTO_FLUSH_BUS;
  pipeline->delay = DEFAULT_DELAY;
  pipeline->priv->latency = DEFAULT_LATENCY;

  pipeline->priv->is_live = FALSE;

  /* create and set a default bus */
  bus = gst_bus_new ();
#if 0
  /* FIXME, disabled for 0.10.5 release as it caused to many regressions */
  /* Start our bus in flushing if appropriate */
  if (pipeline->priv->auto_flush_bus)
    gst_bus_set_flushing (bus, TRUE);
#endif

  gst_element_set_bus (GST_ELEMENT_CAST (pipeline), bus);
  GST_DEBUG_OBJECT (pipeline, "set bus %" GST_PTR_FORMAT " on pipeline", bus);
  gst_object_unref (bus);
}

static void
gst_pipeline_dispose (GObject * object)
{
  GstPipeline *pipeline = GST_PIPELINE (object);
  GstClock **clock_p = &pipeline->fixed_clock;

  GST_CAT_DEBUG_OBJECT (GST_CAT_REFCOUNTING, pipeline, "%p dispose", pipeline);

  /* clear and unref any fixed clock */
  gst_object_replace ((GstObject **) clock_p, NULL);

  G_OBJECT_CLASS (parent_class)->dispose (object);
}

static void
gst_pipeline_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstPipeline *pipeline = GST_PIPELINE (object);

  switch (prop_id) {
    case PROP_DELAY:
      gst_pipeline_set_delay (pipeline, g_value_get_uint64 (value));
      break;
    case PROP_AUTO_FLUSH_BUS:
      gst_pipeline_set_auto_flush_bus (pipeline, g_value_get_boolean (value));
      break;
    case PROP_LATENCY:
      gst_pipeline_set_latency (pipeline, g_value_get_uint64 (value));
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_pipeline_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  GstPipeline *pipeline = GST_PIPELINE (object);

  switch (prop_id) {
    case PROP_DELAY:
      g_value_set_uint64 (value, gst_pipeline_get_delay (pipeline));
      break;
    case PROP_AUTO_FLUSH_BUS:
      g_value_set_boolean (value, gst_pipeline_get_auto_flush_bus (pipeline));
      break;
    case PROP_LATENCY:
      g_value_set_uint64 (value, gst_pipeline_get_latency (pipeline));
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

/* set the start_time to 0, this will cause us to select a new base_time and
 * make the running_time start from 0 again. */
static void
reset_start_time (GstPipeline * pipeline, GstClockTime start_time)
{
  GST_OBJECT_LOCK (pipeline);
  if (GST_ELEMENT_START_TIME (pipeline) != GST_CLOCK_TIME_NONE) {
    GST_DEBUG_OBJECT (pipeline, "reset start_time to 0");
    GST_ELEMENT_START_TIME (pipeline) = start_time;
    pipeline->priv->last_start_time = -1;

    /* Reset instant rate multiplier because we flushed / reset time.
     * Old anchor's don't make sense */
    pipeline->priv->instant_rate_seqnum = GST_SEQNUM_INVALID;
    pipeline->priv->instant_rate_upstream_anchor =
        pipeline->priv->instant_rate_clock_anchor = GST_CLOCK_TIME_NONE;
    pipeline->priv->active_instant_rate = 1.0;
    GST_DEBUG_OBJECT (pipeline, "Reset start time to %" GST_TIME_FORMAT,
        GST_TIME_ARGS (start_time));
  } else {
    GST_DEBUG_OBJECT (pipeline, "application asked to not reset stream_time");
  }
  GST_OBJECT_UNLOCK (pipeline);
}

/**
 * gst_pipeline_new:
 * @name: (allow-none): name of new pipeline
 *
 * Create a new pipeline with the given name.
 *
 * Returns: (transfer floating): newly created GstPipeline
 *
 * MT safe.
 */
GstElement *
gst_pipeline_new (const gchar * name)
{
  return gst_element_factory_make ("pipeline", name);
}

/* takes a snapshot of the running_time of the pipeline and store this as the
 * element start_time. This is the time we will set as the running_time of the
 * pipeline when we go to PLAYING next. */
static void
pipeline_update_start_time (GstElement * element)
{
  GstPipeline *pipeline = GST_PIPELINE_CAST (element);
  GstClock *clock;

  GST_OBJECT_LOCK (element);
  if ((clock = element->clock)) {
    GstClockTime now;

    gst_object_ref (clock);
    GST_OBJECT_UNLOCK (element);

    /* calculate the time when we stopped */
    now = gst_clock_get_time (clock);
    gst_object_unref (clock);

    GST_OBJECT_LOCK (element);
    /* store the current running time */
    if (GST_ELEMENT_START_TIME (pipeline) != GST_CLOCK_TIME_NONE) {
      if (now != GST_CLOCK_TIME_NONE)
        GST_ELEMENT_START_TIME (pipeline) = now - element->base_time;
      else
        GST_WARNING_OBJECT (element,
            "Clock %s returned invalid time, can't calculate "
            "running_time when going to the PAUSED state",
            GST_OBJECT_NAME (clock));

      /* we went to PAUSED, when going to PLAYING select clock and new
       * base_time */
      pipeline->priv->update_clock = TRUE;
    }
    GST_DEBUG_OBJECT (element,
        "start_time=%" GST_TIME_FORMAT ", now=%" GST_TIME_FORMAT
        ", base_time %" GST_TIME_FORMAT,
        GST_TIME_ARGS (GST_ELEMENT_START_TIME (pipeline)),
        GST_TIME_ARGS (now), GST_TIME_ARGS (element->base_time));
  }
  GST_OBJECT_UNLOCK (element);
}

/* MT safe */
static GstStateChangeReturn
gst_pipeline_change_state (GstElement * element, GstStateChange transition)
{
  GstStateChangeReturn result = GST_STATE_CHANGE_SUCCESS;
  GstPipeline *pipeline = GST_PIPELINE_CAST (element);
  GstClock *clock;

  switch (transition) {
    case GST_STATE_CHANGE_NULL_TO_NULL:
      break;
    case GST_STATE_CHANGE_READY_TO_READY:
      break;
    case GST_STATE_CHANGE_PAUSED_TO_PAUSED:
      break;
    case GST_STATE_CHANGE_PLAYING_TO_PLAYING:
      break;
    case GST_STATE_CHANGE_NULL_TO_READY:
      GST_OBJECT_LOCK (element);
      if (element->bus)
        gst_bus_set_flushing (element->bus, FALSE);
      GST_OBJECT_UNLOCK (element);
      break;
    case GST_STATE_CHANGE_READY_TO_PAUSED:
      GST_OBJECT_LOCK (element);
      pipeline->priv->update_clock = TRUE;
      GST_OBJECT_UNLOCK (element);

      /* READY to PAUSED starts running_time from 0 */
      reset_start_time (pipeline, 0);
      break;
    case GST_STATE_CHANGE_PAUSED_TO_PLAYING:
    {
      GstClockTime now, start_time, last_start_time, delay;
      gboolean update_clock;
      GstClock *cur_clock;

      GST_DEBUG_OBJECT (element, "selecting clock and base_time");

      GST_OBJECT_LOCK (element);
      cur_clock = element->clock;
      if (cur_clock)
        gst_object_ref (cur_clock);
      /* get the desired running_time of the first buffer aka the start_time */
      start_time = GST_ELEMENT_START_TIME (pipeline);
      last_start_time = pipeline->priv->last_start_time;
      pipeline->priv->last_start_time = start_time;
      /* see if we need to update the clock */
      update_clock = pipeline->priv->update_clock;
      pipeline->priv->update_clock = FALSE;
      delay = pipeline->delay;
      GST_OBJECT_UNLOCK (element);

      /* running time changed, either with a PAUSED or a flush, we need to check
       * if there is a new clock & update the base time */
      /* only do this for top-level, however */
      if (GST_OBJECT_PARENT (element) == NULL &&
          (update_clock || last_start_time != start_time)) {
        GST_DEBUG_OBJECT (pipeline, "Need to update start_time");

        /* when going to PLAYING, select a clock when needed. If we just got
         * flushed, we don't reselect the clock. */
        if (update_clock) {
          GST_DEBUG_OBJECT (pipeline, "Need to update clock.");
          clock = gst_element_provide_clock (element);
        } else {
          GST_DEBUG_OBJECT (pipeline,
              "Don't need to update clock, using old clock.");
          /* only try to ref if cur_clock is not NULL */
          if (cur_clock)
            gst_object_ref (cur_clock);
          clock = cur_clock;
        }

        if (clock) {
          now = gst_clock_get_time (clock);
        } else {
          GST_DEBUG_OBJECT (pipeline, "no clock, using base time of NONE");
          now = GST_CLOCK_TIME_NONE;
        }

        if (clock != cur_clock) {
          /* now distribute the clock (which could be NULL). If some
           * element refuses the clock, this will return FALSE and
           * we effectively fail the state change. */
          if (!gst_element_set_clock (element, clock))
            goto invalid_clock;

          /* if we selected and distributed a new clock, let the app
           * know about it */
          gst_element_post_message (element,
              gst_message_new_new_clock (GST_OBJECT_CAST (element), clock));
        }

        if (clock)
          gst_object_unref (clock);

        if (start_time != GST_CLOCK_TIME_NONE && now != GST_CLOCK_TIME_NONE) {
          GstClockTime new_base_time = now - start_time + delay;
          GST_DEBUG_OBJECT (element,
              "start_time=%" GST_TIME_FORMAT ", now=%" GST_TIME_FORMAT
              ", base_time %" GST_TIME_FORMAT,
              GST_TIME_ARGS (start_time), GST_TIME_ARGS (now),
              GST_TIME_ARGS (new_base_time));

          gst_element_set_base_time (element, new_base_time);
        } else {
          GST_DEBUG_OBJECT (pipeline,
              "NOT adjusting base_time because start_time is NONE");
        }
      } else {
        GST_DEBUG_OBJECT (pipeline,
            "NOT adjusting base_time because we selected one before");
      }

      if (cur_clock)
        gst_object_unref (cur_clock);
      break;
    }
    case GST_STATE_CHANGE_PLAYING_TO_PAUSED:
    {
      /* we take a start_time snapshot before calling the children state changes
       * so that they know about when the pipeline PAUSED. */
      pipeline_update_start_time (element);
      break;
    }
    case GST_STATE_CHANGE_PAUSED_TO_READY:
      pipeline->priv->is_live = FALSE;
      reset_start_time (pipeline, 0);
      break;
    case GST_STATE_CHANGE_READY_TO_NULL:
      break;
  }

  result = GST_ELEMENT_CLASS (parent_class)->change_state (element, transition);

  if (GST_STATE_TRANSITION_NEXT (transition) == GST_STATE_PAUSED) {
    pipeline->priv->is_live = result == GST_STATE_CHANGE_NO_PREROLL;
    GST_INFO_OBJECT (pipeline, "pipeline is%slive",
        pipeline->priv->is_live ? " " : " not ");
  }

  switch (transition) {
    case GST_STATE_CHANGE_NULL_TO_NULL:
      break;
    case GST_STATE_CHANGE_READY_TO_READY:
      break;
    case GST_STATE_CHANGE_PAUSED_TO_PAUSED:
      break;
    case GST_STATE_CHANGE_PLAYING_TO_PLAYING:
      break;
    case GST_STATE_CHANGE_NULL_TO_READY:
      break;
    case GST_STATE_CHANGE_READY_TO_PAUSED:
      break;
    case GST_STATE_CHANGE_PAUSED_TO_PLAYING:
      break;
    case GST_STATE_CHANGE_PLAYING_TO_PAUSED:
    {
      /* Take a new snapshot of the start_time after calling the state change on
       * all children. This will be the running_time of the pipeline when we go
       * back to PLAYING */
      pipeline_update_start_time (element);
      break;
    }
    case GST_STATE_CHANGE_PAUSED_TO_READY:
      break;
    case GST_STATE_CHANGE_READY_TO_NULL:
    {
      GstBus *bus;
      gboolean auto_flush;

      /* grab some stuff before we release the lock to flush out the bus */
      GST_OBJECT_LOCK (element);
      if ((bus = element->bus))
        gst_object_ref (bus);
      auto_flush = pipeline->priv->auto_flush_bus;
      GST_OBJECT_UNLOCK (element);

      if (bus) {
        if (auto_flush) {
          gst_bus_set_flushing (bus, TRUE);
        } else {
          GST_INFO_OBJECT (element, "not flushing bus, auto-flushing disabled");
        }
        gst_object_unref (bus);
      }
      break;
    }
  }
  return result;

  /* ERRORS */
invalid_clock:
  {
    /* we generate this error when the selected clock was not
     * accepted by some element */
    GST_ELEMENT_ERROR (pipeline, CORE, CLOCK,
        (_("Selected clock cannot be used in pipeline.")),
        ("Pipeline cannot operate with selected clock"));
    GST_DEBUG_OBJECT (pipeline,
        "Pipeline cannot operate with selected clock %p", clock);
    if (clock)
      gst_object_unref (clock);
    return GST_STATE_CHANGE_FAILURE;
  }
}

/* intercept the bus messages from our children. We watch for the ASYNC_START
 * message with is posted by the elements (sinks) that require a reset of the
 * running_time after a flush. ASYNC_START also brings the pipeline back into
 * the PAUSED, pending PAUSED state. When the ASYNC_DONE message is received the
 * pipeline will redistribute the new base_time and will bring the elements back
 * to the desired state of the pipeline. */
/* GST_MESSAGE_INSTANT_RATE_REQUEST: This message is only posted by sinks and
 *     bins containing sinks (which are also considered sinks). Once all sinks
 *     have posted this message it is posted to the parent bin, or if this is
 *     a top-level bin (e.g. pipeline), a instant-rate-sync-time event with
 *     the current running time is sent to the whole pipeline.
 */
static void
gst_pipeline_handle_message (GstBin * bin, GstMessage * message)
{
  GstPipeline *pipeline = GST_PIPELINE_CAST (bin);

  switch (GST_MESSAGE_TYPE (message)) {
    case GST_MESSAGE_RESET_TIME:
    {
      GstClockTime running_time;

      gst_message_parse_reset_time (message, &running_time);

      /* reset our running time if we need to distribute a new base_time to the
       * children. */
      reset_start_time (pipeline, running_time);

      /* If we are live, sample a new base_time immediately */
      if (pipeline->priv->is_live
          && GST_STATE_TARGET (pipeline) == GST_STATE_PLAYING) {
        gst_pipeline_change_state (GST_ELEMENT (pipeline),
            GST_STATE_CHANGE_PAUSED_TO_PLAYING);
      }

      break;
    }
    case GST_MESSAGE_CLOCK_LOST:
    {
      GstClock *clock;

      gst_message_parse_clock_lost (message, &clock);

      GST_OBJECT_LOCK (bin);
      if (clock == GST_ELEMENT_CAST (bin)->clock) {
        GST_DEBUG_OBJECT (bin, "Used clock '%s' got lost",
            GST_OBJECT_NAME (clock));
        pipeline->priv->update_clock = TRUE;
      }
      GST_OBJECT_UNLOCK (bin);
    }
      break;

    case GST_MESSAGE_INSTANT_RATE_REQUEST:{
      guint32 seqnum = gst_message_get_seqnum (message);
      gdouble rate_multiplier;

      gst_message_parse_instant_rate_request (message, &rate_multiplier);

      gst_pipeline_handle_instant_rate (pipeline, rate_multiplier, seqnum);

      break;
    }
    default:
      break;
  }
  GST_BIN_CLASS (parent_class)->handle_message (bin, message);
}

static gboolean
gst_pipeline_do_latency (GstBin * bin)
{
  GstPipeline *pipeline = GST_PIPELINE (bin);
  GstQuery *query;
  GstClockTime latency;
  GstClockTime min_latency, max_latency;
  gboolean res;

  GST_OBJECT_LOCK (pipeline);
  latency = pipeline->priv->latency;
  GST_OBJECT_UNLOCK (pipeline);

  if (latency == GST_CLOCK_TIME_NONE)
    return GST_BIN_CLASS (parent_class)->do_latency (bin);

  GST_DEBUG_OBJECT (pipeline, "querying latency");

  query = gst_query_new_latency ();
  if ((res = gst_element_query (GST_ELEMENT_CAST (pipeline), query))) {
    gboolean live;

    gst_query_parse_latency (query, &live, &min_latency, &max_latency);

    GST_DEBUG_OBJECT (pipeline,
        "got min latency %" GST_TIME_FORMAT ", max latency %"
        GST_TIME_FORMAT ", live %d", GST_TIME_ARGS (min_latency),
        GST_TIME_ARGS (max_latency), live);

    if (max_latency < min_latency) {
      /* this is an impossible situation, some parts of the pipeline might not
       * work correctly. We post a warning for now. */
      GST_ELEMENT_WARNING (pipeline, CORE, CLOCK, (NULL),
          ("Impossible to configure latency: max %" GST_TIME_FORMAT " < min %"
              GST_TIME_FORMAT ". Add queues or other buffering elements.",
              GST_TIME_ARGS (max_latency), GST_TIME_ARGS (min_latency)));
    }

    if (latency < min_latency) {
      /* This is a problematic situation as we will most likely drop lots of
       * data if we configure a too low latency */
      GST_ELEMENT_WARNING (pipeline, CORE, CLOCK, (NULL),
          ("Configured latency is lower than detected minimum latency: configured %"
              GST_TIME_FORMAT " < min %" GST_TIME_FORMAT,
              GST_TIME_ARGS (latency), GST_TIME_ARGS (min_latency)));
    }
  } else {
    /* this is not a real problem, we just don't configure any latency. */
    GST_WARNING_OBJECT (pipeline, "failed to query latency");
  }
  gst_query_unref (query);


  /* configure latency on elements */
  res =
      gst_element_send_event (GST_ELEMENT_CAST (pipeline),
      gst_event_new_latency (latency));
  if (res) {
    GST_INFO_OBJECT (pipeline, "configured latency of %" GST_TIME_FORMAT,
        GST_TIME_ARGS (latency));
  } else {
    GST_WARNING_OBJECT (pipeline,
        "did not really configure latency of %" GST_TIME_FORMAT,
        GST_TIME_ARGS (latency));
  }

  return res;
}

/**
 * gst_pipeline_get_bus:
 * @pipeline: a #GstPipeline
 *
 * Gets the #GstBus of @pipeline. The bus allows applications to receive
 * #GstMessage packets.
 *
 * Returns: (transfer full): a #GstBus, unref after usage.
 *
 * MT safe.
 */
GstBus *
gst_pipeline_get_bus (GstPipeline * pipeline)
{
  return gst_element_get_bus (GST_ELEMENT_CAST (pipeline));
}

static GstClock *
gst_pipeline_provide_clock_func (GstElement * element)
{
  GstClock *clock = NULL;
  GstPipeline *pipeline = GST_PIPELINE (element);

  /* if we have a fixed clock, use that one */
  GST_OBJECT_LOCK (pipeline);
  if (GST_OBJECT_FLAG_IS_SET (pipeline, GST_PIPELINE_FLAG_FIXED_CLOCK)) {
    clock = pipeline->fixed_clock;
    if (clock)
      gst_object_ref (clock);
    GST_OBJECT_UNLOCK (pipeline);

    GST_CAT_DEBUG (GST_CAT_CLOCK, "pipeline using fixed clock %p (%s)",
        clock, clock ? GST_STR_NULL (GST_OBJECT_NAME (clock)) : "-");
  } else {
    GST_OBJECT_UNLOCK (pipeline);
    /* let the parent bin select a clock */
    clock =
        GST_ELEMENT_CLASS (parent_class)->provide_clock (GST_ELEMENT
        (pipeline));
    /* no clock, use a system clock */
    if (!clock) {
      clock = gst_system_clock_obtain ();

      GST_CAT_DEBUG (GST_CAT_CLOCK, "pipeline obtained system clock: %p (%s)",
          clock, clock ? GST_STR_NULL (GST_OBJECT_NAME (clock)) : "-");
    } else {
      GST_CAT_DEBUG (GST_CAT_CLOCK, "pipeline obtained clock: %p (%s)",
          clock, clock ? GST_STR_NULL (GST_OBJECT_NAME (clock)) : "-");
    }
  }
  return clock;
}

/**
 * gst_pipeline_get_clock: (skip)
 * @pipeline: a #GstPipeline
 *
 * Gets the current clock used by @pipeline. Users of object
 * oriented languages should use gst_pipeline_get_pipeline_clock()
 * to avoid confusion with gst_element_get_clock() which has a different behavior.
 *
 * Unlike gst_element_get_clock(), this function will always return a
 * clock, even if the pipeline is not in the PLAYING state.
 *
 * Returns: (transfer full): a #GstClock, unref after usage.
 */
GstClock *
gst_pipeline_get_clock (GstPipeline * pipeline)
{
  return gst_pipeline_get_pipeline_clock (pipeline);
}

/**
 * gst_pipeline_get_pipeline_clock:
 * @pipeline: a #GstPipeline
 *
 * Gets the current clock used by @pipeline.
 *
 * Unlike gst_element_get_clock(), this function will always return a
 * clock, even if the pipeline is not in the PLAYING state.
 *
 * Returns: (transfer full): a #GstClock, unref after usage.
 *
 * Since: 1.6
 */
GstClock *
gst_pipeline_get_pipeline_clock (GstPipeline * pipeline)
{
  g_return_val_if_fail (GST_IS_PIPELINE (pipeline), NULL);

  return gst_pipeline_provide_clock_func (GST_ELEMENT_CAST (pipeline));
}


/**
 * gst_pipeline_use_clock:
 * @pipeline: a #GstPipeline
 * @clock: (transfer none) (allow-none): the clock to use
 *
 * Force @pipeline to use the given @clock. The pipeline will
 * always use the given clock even if new clock providers are added
 * to this pipeline.
 *
 * If @clock is %NULL all clocking will be disabled which will make
 * the pipeline run as fast as possible.
 *
 * MT safe.
 */
void
gst_pipeline_use_clock (GstPipeline * pipeline, GstClock * clock)
{
  GstClock **clock_p;

  g_return_if_fail (GST_IS_PIPELINE (pipeline));

  GST_OBJECT_LOCK (pipeline);
  GST_OBJECT_FLAG_SET (pipeline, GST_PIPELINE_FLAG_FIXED_CLOCK);

  clock_p = &pipeline->fixed_clock;
  gst_object_replace ((GstObject **) clock_p, (GstObject *) clock);
  GST_OBJECT_UNLOCK (pipeline);

  GST_CAT_DEBUG (GST_CAT_CLOCK, "pipeline using fixed clock %p (%s)", clock,
      (clock ? GST_OBJECT_NAME (clock) : "nil"));
}

/**
 * gst_pipeline_set_clock: (skip)
 * @pipeline: a #GstPipeline
 * @clock: (transfer none): the clock to set
 *
 * Set the clock for @pipeline. The clock will be distributed
 * to all the elements managed by the pipeline.
 *
 * Returns: %TRUE if the clock could be set on the pipeline. %FALSE if
 *   some element did not accept the clock.
 *
 * MT safe.
 */
gboolean
gst_pipeline_set_clock (GstPipeline * pipeline, GstClock * clock)
{
  g_return_val_if_fail (pipeline != NULL, FALSE);
  g_return_val_if_fail (GST_IS_PIPELINE (pipeline), FALSE);

  return
      GST_ELEMENT_CLASS (parent_class)->set_clock (GST_ELEMENT_CAST (pipeline),
      clock);
}

/**
 * gst_pipeline_auto_clock:
 * @pipeline: a #GstPipeline
 *
 * Let @pipeline select a clock automatically. This is the default
 * behaviour.
 *
 * Use this function if you previous forced a fixed clock with
 * gst_pipeline_use_clock() and want to restore the default
 * pipeline clock selection algorithm.
 *
 * MT safe.
 */
void
gst_pipeline_auto_clock (GstPipeline * pipeline)
{
  GstClock **clock_p;

  g_return_if_fail (pipeline != NULL);
  g_return_if_fail (GST_IS_PIPELINE (pipeline));

  GST_OBJECT_LOCK (pipeline);
  GST_OBJECT_FLAG_UNSET (pipeline, GST_PIPELINE_FLAG_FIXED_CLOCK);

  clock_p = &pipeline->fixed_clock;
  gst_object_replace ((GstObject **) clock_p, NULL);
  GST_OBJECT_UNLOCK (pipeline);

  GST_CAT_DEBUG (GST_CAT_CLOCK, "pipeline using automatic clock");
}

/**
 * gst_pipeline_set_delay:
 * @pipeline: a #GstPipeline
 * @delay: the delay
 *
 * Set the expected delay needed for all elements to perform the
 * PAUSED to PLAYING state change. @delay will be added to the
 * base time of the elements so that they wait an additional @delay
 * amount of time before starting to process buffers and cannot be
 * #GST_CLOCK_TIME_NONE.
 *
 * This option is used for tuning purposes and should normally not be
 * used.
 *
 * MT safe.
 */
void
gst_pipeline_set_delay (GstPipeline * pipeline, GstClockTime delay)
{
  g_return_if_fail (GST_IS_PIPELINE (pipeline));
  g_return_if_fail (delay != GST_CLOCK_TIME_NONE);

  GST_OBJECT_LOCK (pipeline);
  pipeline->delay = delay;
  GST_OBJECT_UNLOCK (pipeline);
}

/**
 * gst_pipeline_get_delay:
 * @pipeline: a #GstPipeline
 *
 * Get the configured delay (see gst_pipeline_set_delay()).
 *
 * Returns: The configured delay.
 *
 * MT safe.
 */
GstClockTime
gst_pipeline_get_delay (GstPipeline * pipeline)
{
  GstClockTime res;

  g_return_val_if_fail (GST_IS_PIPELINE (pipeline), GST_CLOCK_TIME_NONE);

  GST_OBJECT_LOCK (pipeline);
  res = pipeline->delay;
  GST_OBJECT_UNLOCK (pipeline);

  return res;
}

/**
 * gst_pipeline_set_auto_flush_bus:
 * @pipeline: a #GstPipeline
 * @auto_flush: whether or not to automatically flush the bus when
 * the pipeline goes from READY to NULL state
 *
 * Usually, when a pipeline goes from READY to NULL state, it automatically
 * flushes all pending messages on the bus, which is done for refcounting
 * purposes, to break circular references.
 *
 * This means that applications that update state using (async) bus messages
 * (e.g. do certain things when a pipeline goes from PAUSED to READY) might
 * not get to see messages when the pipeline is shut down, because they might
 * be flushed before they can be dispatched in the main thread. This behaviour
 * can be disabled using this function.
 *
 * It is important that all messages on the bus are handled when the
 * automatic flushing is disabled else memory leaks will be introduced.
 *
 * MT safe.
 */
void
gst_pipeline_set_auto_flush_bus (GstPipeline * pipeline, gboolean auto_flush)
{
  g_return_if_fail (GST_IS_PIPELINE (pipeline));

  GST_OBJECT_LOCK (pipeline);
  pipeline->priv->auto_flush_bus = auto_flush;
  GST_OBJECT_UNLOCK (pipeline);
}

/**
 * gst_pipeline_get_auto_flush_bus:
 * @pipeline: a #GstPipeline
 *
 * Check if @pipeline will automatically flush messages when going to
 * the NULL state.
 *
 * Returns: whether the pipeline will automatically flush its bus when
 * going from READY to NULL state or not.
 *
 * MT safe.
 */
gboolean
gst_pipeline_get_auto_flush_bus (GstPipeline * pipeline)
{
  gboolean res;

  g_return_val_if_fail (GST_IS_PIPELINE (pipeline), FALSE);

  GST_OBJECT_LOCK (pipeline);
  res = pipeline->priv->auto_flush_bus;
  GST_OBJECT_UNLOCK (pipeline);

  return res;
}

/**
 * gst_pipeline_set_latency:
 * @pipeline: a #GstPipeline
 * @latency: latency to configure
 *
 * Sets the latency that should be configured on the pipeline. Setting
 * GST_CLOCK_TIME_NONE will restore the default behaviour of using the minimum
 * latency from the LATENCY query. Setting this is usually not required and
 * the pipeline will figure out an appropriate latency automatically.
 *
 * Setting a too low latency, especially lower than the minimum latency from
 * the LATENCY query, will most likely cause the pipeline to fail.
 *
 * Since: 1.6
 */
void
gst_pipeline_set_latency (GstPipeline * pipeline, GstClockTime latency)
{
  gboolean changed;

  g_return_if_fail (GST_IS_PIPELINE (pipeline));

  GST_OBJECT_LOCK (pipeline);
  changed = (pipeline->priv->latency != latency);
  pipeline->priv->latency = latency;
  GST_OBJECT_UNLOCK (pipeline);

  if (changed)
    gst_bin_recalculate_latency (GST_BIN_CAST (pipeline));
}

/**
 * gst_pipeline_get_latency:
 * @pipeline: a #GstPipeline
 *
 * Gets the latency that should be configured on the pipeline. See
 * gst_pipeline_set_latency().
 *
 * Returns: Latency to configure on the pipeline or GST_CLOCK_TIME_NONE
 *
 * Since: 1.6
 */

GstClockTime
gst_pipeline_get_latency (GstPipeline * pipeline)
{
  GstClockTime latency;

  g_return_val_if_fail (GST_IS_PIPELINE (pipeline), GST_CLOCK_TIME_NONE);

  GST_OBJECT_LOCK (pipeline);
  latency = pipeline->priv->latency;
  GST_OBJECT_UNLOCK (pipeline);

  return latency;
}

static gboolean
gst_pipeline_handle_instant_rate (GstPipeline * pipeline, gdouble rate,
    guint32 seqnum)
{
  GstClockTime running_time = GST_CLOCK_TIME_NONE;
  GstClockTime upstream_running_time = GST_CLOCK_TIME_NONE;
  gboolean is_playing;
  GstEvent *event;

  GST_OBJECT_LOCK (pipeline);

  if (pipeline->priv->instant_rate_seqnum != GST_SEQNUM_INVALID &&
      pipeline->priv->instant_rate_seqnum == seqnum) {
    GST_DEBUG_OBJECT (pipeline,
        "Handling duplicate instant-rate-request message with seqnum %u",
        seqnum);
    upstream_running_time = pipeline->priv->instant_rate_upstream_anchor;
    running_time = pipeline->priv->instant_rate_clock_anchor;

    if (G_UNLIKELY (rate != pipeline->priv->active_instant_rate)) {
      GST_WARNING_OBJECT (pipeline,
          "Repeated instant-rate-request has a different rate to before! %f != %f",
          rate, pipeline->priv->active_instant_rate);
      rate = pipeline->priv->active_instant_rate;
    }
  } else {
    /* Get the current running time of the pipeline */
    is_playing = GST_STATE (pipeline) == GST_STATE_PLAYING
        && (GST_STATE_PENDING (pipeline) == GST_STATE_VOID_PENDING ||
        GST_STATE_PENDING (pipeline) == GST_STATE_PLAYING);

    if (is_playing) {
      GstClockTime base_time, clock_time;
      GstClock *clock;

      base_time = GST_ELEMENT_CAST (pipeline)->base_time;
      clock = GST_ELEMENT_CLOCK (pipeline);

      if (clock) {
        clock_time = gst_clock_get_time (clock);
        running_time = clock_time - base_time;
      }
    } else {
      running_time = GST_ELEMENT_START_TIME (pipeline);
    }

    if (!GST_CLOCK_TIME_IS_VALID (running_time)) {
      GST_OBJECT_UNLOCK (pipeline);
      return FALSE;
    }

    if (GST_CLOCK_TIME_IS_VALID (pipeline->priv->instant_rate_upstream_anchor)) {
      /* Already had an override, calculate the adjustment due to that
       * elapsed duration */
      GstClockTime elapsed =
          running_time - pipeline->priv->instant_rate_clock_anchor;
      pipeline->priv->instant_rate_upstream_anchor +=
          elapsed * pipeline->priv->active_instant_rate;
      pipeline->priv->instant_rate_clock_anchor = running_time;
    } else {
      /* Else this is the first override event */
      pipeline->priv->instant_rate_upstream_anchor =
          pipeline->priv->instant_rate_clock_anchor = running_time;
    }
    upstream_running_time = pipeline->priv->instant_rate_upstream_anchor;

    pipeline->priv->instant_rate_seqnum = seqnum;
    pipeline->priv->active_instant_rate = rate;
  }

  GST_OBJECT_UNLOCK (pipeline);

  GST_DEBUG_OBJECT (pipeline,
      "Instant rate multiplier to %f rt %" GST_TIME_FORMAT " upstream %"
      GST_TIME_FORMAT, rate, GST_TIME_ARGS (running_time),
      GST_TIME_ARGS (upstream_running_time));

  event =
      gst_event_new_instant_rate_sync_time (rate, running_time,
      upstream_running_time);
  gst_event_set_seqnum (event, seqnum);

  return gst_element_send_event (GST_ELEMENT_CAST (pipeline), event);
}
