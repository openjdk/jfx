/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include "progressbuffer.h"
#include "cache.h"
#include <fxplugins_common.h>

#if ENABLE_PULL_MODE
#define NO_RANGE_REQUEST -1
#endif

/***********************************************************************************
 * Debug category init
 ***********************************************************************************/
GST_DEBUG_CATEGORY (progress_buffer_debug);
#define GST_CAT_DEFAULT progress_buffer_debug

#define ELEMENT_DESCRIPTION "JFX Progress buffer element"

/***********************************************************************************
 * Properties
 ***********************************************************************************/
enum
{
    PROP_0,
    PROP_THRESHOLD,
    PROP_BANDWIDTH,
    PROP_PREBUFFER_TIME,
    PROP_WAIT_TOLERANCE
};

/***********************************************************************************
 * Element structures are hidden from outside
 ***********************************************************************************/
#define EOS_SIGNAL_LIMIT 1 // Send EOS notification only this amount of times

struct EosStatus
{
    gboolean      eos;
    gint          signal_limit;
};

struct _ProgressBuffer
{
    GstElement    parent;

    GstPad        *sinkpad;
    GstPad        *srcpad;

    GMutex        *lock;
    GCond         *add_cond;

    // Cache infrastructure
    Cache         *cache;
    GstEvent      *pending_src_event;
    guint8        *incoming_buffer;
    int            incoming_buffer_size;
    gint64         cache_read_offset;

    GstSegment    sink_segment;
    gdouble       last_update;
    gdouble       threshold; // property controlled.

    guint64       subtotal;  // bandwidth accumulator.
    gdouble       bandwidth; // property accessible.
    gdouble       prebuffer_time; // property controlled.
    gdouble       wait_tolerance; // property controlled.
    GTimer        *bandwidth_timer;

    gboolean      unexpected;
    GstFlowReturn srcresult;

    struct EosStatus  eos_status;

    gboolean      instant_seek;

#if ENABLE_PULL_MODE
    gint64       range_start;
    gint64       range_stop;
    GThread     *monitor_thread;
#endif
};

struct _ProgressBufferClass
{
    GstElementClass parent;
};

/***********************************************************************************
 * Substitution for
 * GST_BOILERPLATE(ProgressBuffer, progress_buffer, GstElement, GST_TYPE_ELEMENT);
 ***********************************************************************************/
static void progress_buffer_base_init     (gpointer             g_class);
static void progress_buffer_class_init    (ProgressBufferClass *g_class);
static void progress_buffer_init          (ProgressBuffer      *object,
                                           ProgressBufferClass *g_class);
static GstElementClass *parent_class = NULL;

static void progress_buffer_class_init_trampoline (gpointer g_class, gpointer data)
{
    parent_class = (GstElementClass *)g_type_class_peek_parent (g_class);
    progress_buffer_class_init ((ProgressBufferClass *)g_class);
}

GType progress_buffer_get_type (void)
{
    static volatile gsize gonce_data = 0;
// INLINE - g_once_init_enter()
    if (g_once_init_enter (&gonce_data))
    {
        GType _type;
        _type = gst_type_register_static_full (GST_TYPE_ELEMENT,
            g_intern_static_string ("ProgressBuffer"),
            sizeof (ProgressBufferClass),
            progress_buffer_base_init,
            NULL,
            progress_buffer_class_init_trampoline,
            NULL,
            NULL,
            sizeof (ProgressBuffer),
            0,
            (GInstanceInitFunc) progress_buffer_init,
            NULL,
            (GTypeFlags) 0);
        g_once_init_leave (&gonce_data, (gsize) _type);
    }
    return (GType) gonce_data;
}

/***********************************************************************************
 * Init stuff
 ***********************************************************************************/
static GstStaticPadTemplate sink_template = GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK, GST_PAD_ALWAYS, GST_STATIC_CAPS_ANY);

static GstStaticPadTemplate source_template = GST_STATIC_PAD_TEMPLATE ("src",
    GST_PAD_SRC, GST_PAD_SOMETIMES, GST_STATIC_CAPS_ANY);

static void progress_buffer_base_init (gpointer g_class)
{
    GstElementClass *element_class = GST_ELEMENT_CLASS (g_class);

    gst_element_class_set_details_simple (element_class,
        "Progressive download plugin",
        "Element",
        "Progressively stores incoming data in memory or file",
        "Oracle Corporation");

    gst_element_class_add_pad_template (element_class,
                                        gst_static_pad_template_get (&sink_template));
    gst_element_class_add_pad_template (element_class,
                                        gst_static_pad_template_get (&source_template));
}

/***********************************************************************************
 * Instance init and forward declarations
 ***********************************************************************************/
static void             progress_buffer_set_property (GObject *object, guint property_id,
                                                      const GValue *value, GParamSpec *pspec);
static void             progress_buffer_get_property (GObject *object, guint property_id,
                                                      GValue *value, GParamSpec *pspec);
static void             progress_buffer_finalize (GObject *object);
static GstStateChangeReturn progress_buffer_change_state (GstElement *element,
                                                          GstStateChange transition);
static GstFlowReturn    progress_buffer_chain(GstPad *pad, GstBuffer *data);
static gboolean         progress_buffer_activatepush_src(GstPad *pad, gboolean active);
static gboolean         progress_buffer_activatepull_src(GstPad *pad, gboolean active);
static gboolean         progress_buffer_sink_event(GstPad *pad, GstEvent *event);
static gboolean         progress_buffer_src_event(GstPad *pad, GstEvent *event);
static GstFlowReturn    progress_buffer_bufferalloc (GstPad * pad, guint64 offset,
                                                     guint size, GstCaps * caps, GstBuffer ** buf);
static void             progress_buffer_loop(void *data);
static void             progress_buffer_flush_data(ProgressBuffer *buffer);

static gboolean         progress_buffer_checkgetrange(GstPad *pad);
static GstFlowReturn    progress_buffer_getrange(GstPad *pad, guint64 start_position,
                                                 guint length, GstBuffer **data);
#if ENABLE_PULL_MODE
static gpointer         progress_buffer_range_monitor(ProgressBuffer *element);
#endif

static void             progress_buffer_set_pending_event(ProgressBuffer *element, GstEvent* new_event);

/**
 * progress_buffer_class_init()
 *
 * Sets up the GLib object oriented C class structure for ProgressBuffer.
 */
static void progress_buffer_class_init (ProgressBufferClass *klass)
{
    GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

    gobject_class->set_property = GST_DEBUG_FUNCPTR(progress_buffer_set_property);
    gobject_class->get_property = GST_DEBUG_FUNCPTR(progress_buffer_get_property);
    gobject_class->finalize = GST_DEBUG_FUNCPTR(progress_buffer_finalize);
    GST_ELEMENT_CLASS (klass)->change_state = GST_DEBUG_FUNCPTR(progress_buffer_change_state);

    g_object_class_install_property (gobject_class, PROP_THRESHOLD,
                                     g_param_spec_double ("threshold",
                                                          "Message threshold",
                                                          "Message emission threshold in percents.",
                                                          0.0  /* minimum value */,
                                                          100.0 /* maximum value */,
                                                          1.0  /* default value */,
                                                          G_PARAM_READWRITE | G_PARAM_CONSTRUCT));

    g_object_class_install_property (gobject_class, PROP_BANDWIDTH,
                                     g_param_spec_double ("bandwidth",
                                                          "Network bandwidth",
                                                          "Network bandwidth in bytes/second",
                                                          0.0  /* minimum value */,
                                                          G_MAXDOUBLE /* maximum value */,
                                                          0.0  /* default value */,
                                                          G_PARAM_READABLE));

    g_object_class_install_property (gobject_class, PROP_PREBUFFER_TIME,
                                     g_param_spec_double ("prebuffer-time",
                                                          "Prebuffer time",
                                                          "Controls prebuffer for prebuffer-time*bandwidth before emitting RANGE_READY event.",
                                                          0.0  /* minimum value */,
                                                          20.0 /* maximum value */,
                                                          2.0  /* default value */,
                                                          G_PARAM_READWRITE | G_PARAM_CONSTRUCT));

    g_object_class_install_property (gobject_class, PROP_WAIT_TOLERANCE,
                                     g_param_spec_double ("wait-tolerance",
                                                          "Wait tolerance timeout",
                                                          "Threshold timeout before emitting seek request to the specified range position.",
                                                          0.0  /* minimum value */,
                                                          20.0 /* maximum value */,
                                                          2.0  /* default value */,
                                                          G_PARAM_READWRITE | G_PARAM_CONSTRUCT));

    cache_static_init();
}

/**
 * progress_buffer_init()
 *
 * Initializer.  Automatically declared in the GST_BOILERPLATE macro above.  Should be
 * only called by GStreamer.
 */
static void progress_buffer_init(ProgressBuffer *element, ProgressBufferClass *element_klass)
{
    GstElementClass *klass = GST_ELEMENT_CLASS (element_klass);

    element->sinkpad = gst_pad_new_from_template (gst_element_class_get_pad_template (klass, "sink"), "sink");
    gst_pad_set_chain_function       (element->sinkpad, GST_DEBUG_FUNCPTR(progress_buffer_chain));
    gst_pad_set_event_function       (element->sinkpad, GST_DEBUG_FUNCPTR(progress_buffer_sink_event));
    gst_pad_set_bufferalloc_function (element->sinkpad, GST_DEBUG_FUNCPTR(progress_buffer_bufferalloc));
    gst_element_add_pad (GST_ELEMENT (element), element->sinkpad);

    element->srcpad = NULL;
    element->cache = NULL;
    element->cache_read_offset = 0;
    element->lock = g_mutex_new();
    element->add_cond = g_cond_new();
    element->bandwidth_timer = g_timer_new();

    element->incoming_buffer = NULL;
    element->incoming_buffer_size = 0;

#if ENABLE_PULL_MODE
    element->monitor_thread = NULL;
#endif

    progress_buffer_flush_data(element);
}

/**
 * progress_buffer_set_property()
 *
 * Function to set properties on the element.  This is where we can add custom properties.
 */
static void progress_buffer_set_property (GObject *object, guint property_id,
                                          const GValue *value, GParamSpec *pspec)
{
    ProgressBuffer *element = PROGRESS_BUFFER(object);
    switch (property_id)
    {
        case PROP_THRESHOLD:
            element->threshold = g_value_get_double(value);
            break;
        case PROP_PREBUFFER_TIME:
            element->prebuffer_time = g_value_get_double(value);
            break;
        case PROP_WAIT_TOLERANCE:
            element->wait_tolerance = g_value_get_double(value);
            break;

        default:
            break;
    }
}

/**
 * progress_buffer_get_property()
 *
 * Function to get properties from the element.  This is where we can add custom properties.
 */
static void progress_buffer_get_property (GObject *object, guint property_id,
                                          GValue *value, GParamSpec *pspec)
{
    ProgressBuffer *element = PROGRESS_BUFFER(object);
    switch (property_id)
    {
        case PROP_THRESHOLD:
            g_value_set_double(value, element->threshold);
            break;

        case PROP_BANDWIDTH:
            g_value_set_double(value, element->bandwidth);
            break;

        case PROP_PREBUFFER_TIME:
            g_value_set_double(value, element->prebuffer_time);
            break;

        case PROP_WAIT_TOLERANCE:
            g_value_set_double(value, element->wait_tolerance);
            break;

        default:
            break;
    }
}

/**
 * progress_buffer_finalize()
 *
 * Equivalent of destructor.
 */
static void progress_buffer_finalize (GObject *object)
{
    ProgressBuffer *element = PROGRESS_BUFFER(object);

    if (element->pending_src_event)
        gst_event_unref(element->pending_src_event); // INLINE - gst_event_unref()

    if (element->cache)
        destroy_cache(element->cache);

    if (element->incoming_buffer)
        g_free(element->incoming_buffer);

    g_mutex_free(element->lock);
    g_cond_free(element->add_cond);
    g_timer_destroy(element->bandwidth_timer);

    G_OBJECT_CLASS (parent_class)->finalize (object);
}

/***********************************************************************************/
static inline void reset_eos(ProgressBuffer *element)
{
    element->eos_status.eos = FALSE;
    element->eos_status.signal_limit = EOS_SIGNAL_LIMIT;

    progress_buffer_set_pending_event(element, NULL);
}

static inline gboolean pending_eos(ProgressBuffer *element)
{
    gboolean result = (element->eos_status.eos && element->eos_status.signal_limit > 0);

    if (result)
        element->eos_status.signal_limit--;

    return result;
}

/***********************************************************************************
 * Pad functions
 ***********************************************************************************/
/**
 * progress_buffer_activatepull_src()
 *
 * Set the source pad's pull mode.
 */
static gboolean progress_buffer_activatepull_src(GstPad *pad, gboolean active)
{
#if ENABLE_PULL_MODE
    ProgressBuffer *element = PROGRESS_BUFFER(GST_PAD_PARENT(pad));

    if (active) // Start a custom task in pull mode for monitoring pull_range requests
    {
        g_mutex_lock(element->lock);
        element->srcresult = GST_FLOW_OK;
        reset_eos(element);
        element->unexpected = FALSE;
        g_mutex_unlock(element->lock);

        if (element->monitor_thread == NULL)
            element->monitor_thread = g_thread_create((GThreadFunc)progress_buffer_range_monitor,
                                                        element, TRUE, NULL);
        return (element->monitor_thread != NULL);
    }
    else if (!active && element->monitor_thread != NULL) // Stop the custom task if it's been created
    {
        g_mutex_lock(element->lock);
        element->srcresult = GST_FLOW_WRONG_STATE;
        g_cond_signal(element->add_cond);
        g_mutex_unlock(element->lock);

        g_thread_join(element->monitor_thread);
        element->monitor_thread = NULL;
    }

    return TRUE;
#else
    return FALSE;
#endif
}

/**
 * progress_buffer_activatepush_src()
 *
 * Set the source pad's push mode.
 */
static gboolean progress_buffer_activatepush_src(GstPad *pad, gboolean active)
{
    ProgressBuffer *element = PROGRESS_BUFFER(GST_PAD_PARENT(pad));

    if (active)
    {
        g_mutex_lock(element->lock);
        element->srcresult = GST_FLOW_OK;
        reset_eos(element);
        element->unexpected = FALSE;
        g_mutex_unlock(element->lock);

        if (gst_pad_is_linked(pad))
            return gst_pad_start_task(pad, progress_buffer_loop, element);
        else
            return TRUE;
    }
    else
    {
        g_mutex_lock(element->lock);
        element->srcresult = GST_FLOW_WRONG_STATE;
        g_cond_signal(element->add_cond);
        g_mutex_unlock(element->lock);

        return gst_pad_stop_task(pad);
    }
}

/**
 * progress_buffer_create_sourcepad()
 *
 */
static void progress_buffer_create_sourcepad(ProgressBuffer *element)
{
    element->srcpad = gst_pad_new_from_template (gst_element_class_get_pad_template (GST_ELEMENT_GET_CLASS(element), "src"), "src");

    gst_pad_set_activatepush_function  (element->srcpad, GST_DEBUG_FUNCPTR(progress_buffer_activatepush_src));
    gst_pad_set_activatepull_function  (element->srcpad, GST_DEBUG_FUNCPTR(progress_buffer_activatepull_src));
    gst_pad_set_checkgetrange_function (element->srcpad, GST_DEBUG_FUNCPTR(progress_buffer_checkgetrange));
    gst_pad_set_event_function         (element->srcpad, GST_DEBUG_FUNCPTR(progress_buffer_src_event));
    gst_pad_set_getrange_function      (element->srcpad, GST_DEBUG_FUNCPTR(progress_buffer_getrange));
    GST_PAD_UNSET_FLUSHING(element->srcpad);

    // Add pad
    gst_element_add_pad (GST_ELEMENT (element), element->srcpad);

    // Activate pad
    gst_pad_set_active (element->srcpad, TRUE);

    // Send "no-more-pads"
    gst_element_no_more_pads(GST_ELEMENT (element));
}

/***********************************************************************************
 * Internal functions
 ***********************************************************************************/
static void progress_buffer_flush_data(ProgressBuffer *element)
{
    element->last_update = 0.0;
    element->bandwidth = 0.0;
    element->subtotal = 0;
    element->pending_src_event = NULL;
    gst_segment_init (&element->sink_segment, GST_FORMAT_BYTES);

#if ENABLE_PULL_MODE
    element->range_start = NO_RANGE_REQUEST;
    element->range_stop = NO_RANGE_REQUEST;
#endif
}

static void progress_buffer_set_pending_event(ProgressBuffer *element, GstEvent* new_event)
{
    if (element->pending_src_event)
        gst_event_unref(element->pending_src_event); // INLINE - gst_event_unref()
    element->pending_src_event = new_event;
}

/**
 * send_position_message
 * Sends application message on the BUS with the following parameters:
 *  - structure name is the constant defined as PB_MESSAGE_BUFFERING
 *  - "start" as gint64 is the start position of the current buffer
 *  - "position" as gint64 is current position up to which data has been read from the source
 *  - "stop" as gint64 is the duration of the current segment, usually equals to the whole duration
 *
 * gboolean "mandatory" flag desribes whether the message must be sent anyways.
 * If it's TRUE message is aways sent, otherwise if it's FALSE the function tries to
 * avoid sending messages every time - it sends messages every percent of the whole size.
 */
static gboolean send_position_message(ProgressBuffer *element, gboolean mandatory)
{
    gdouble percent = (double)element->sink_segment.last_stop/element->sink_segment.stop * 100;
    mandatory |= (percent - element->last_update) > element->threshold; // Prevent sending update messages to often

    if (mandatory)
    {
        GstStructure *s = gst_structure_new(PB_MESSAGE_BUFFERING,
                                            "start", G_TYPE_INT64, element->sink_segment.start,
                                            "position", G_TYPE_INT64, element->sink_segment.last_stop,
                                            "stop", G_TYPE_INT64, element->sink_segment.stop,
                                            "eos", G_TYPE_BOOLEAN, element->eos_status.eos,
                                            NULL);
        GstMessage *msg = gst_message_new_application(GST_OBJECT(element), s);

        gst_element_post_message(GST_ELEMENT(element), msg);
        element->last_update = percent;
    }
    return mandatory;
}

/**
 * progress_buffer_enqueue_item()
 *
 * Add an item in the queue. Must be called in the locked context.  Item may be event or data.
 */
static GstFlowReturn progress_buffer_enqueue_item(ProgressBuffer *element, GstMiniObject *item)
{
    gboolean signal = FALSE;

    if (GST_IS_BUFFER (item))
    {
        gdouble elapsed;
        // update sink segment position
        gst_segment_set_last_stop (&element->sink_segment, GST_FORMAT_BYTES,
                                   GST_BUFFER_OFFSET(item) + GST_BUFFER_SIZE(item));

        if(element->sink_segment.stop < element->sink_segment.last_stop) // This must never happen.
            return  GST_FLOW_ERROR;

        cache_write_buffer(element->cache, GST_BUFFER(item));

        elapsed = g_timer_elapsed(element->bandwidth_timer, NULL);
        element->subtotal += GST_BUFFER_SIZE(item);

        if (elapsed > 1.0)
        {
            element->bandwidth = element->subtotal/elapsed;
            element->subtotal = 0;
            g_timer_start(element->bandwidth_timer);
        }

        // send buffer progress position up (used to track buffer fill, etc.)
        signal = send_position_message(element, signal);
    }
    else if (GST_IS_EVENT (item))
    {
        GstEvent *event = GST_EVENT_CAST (item);

        switch (GST_EVENT_TYPE (event))
        {
            case GST_EVENT_EOS:
                element->eos_status.eos = TRUE;
                if (element->sink_segment.last_stop < element->sink_segment.stop)
                    element->sink_segment.stop = element->sink_segment.last_stop;

                signal = send_position_message(element, TRUE);
                gst_event_unref(event); // INLINE - gst_event_unref()
                break;

            case GST_EVENT_NEWSEGMENT:
            {
                gboolean update;
                GstFormat format;
                gdouble rate, arate;
                gint64 start, stop, time;

                element->unexpected = FALSE;

                gst_event_parse_new_segment_full (event, &update, &rate, &arate,
                                                  &format, &start, &stop, &time);
                if (format != GST_FORMAT_BYTES)
                {
                    gst_element_message_full(GST_ELEMENT(element), GST_MESSAGE_ERROR, GST_STREAM_ERROR, GST_STREAM_ERROR_FORMAT,
                                             g_strdup("GST_FORMAT_BYTES buffers expected."), NULL,
                                             ("progressbuffer.c"), ("progress_buffer_enqueue_item"), 0);
                    gst_event_unref(event); // INLINE - gst_event_unref()
                    return GST_FLOW_ERROR;
                 }

                if (stop - start <= 0)
                {
                    gst_element_message_full(GST_ELEMENT(element), GST_MESSAGE_ERROR, GST_STREAM_ERROR, GST_STREAM_ERROR_WRONG_TYPE,
                                             g_strdup("Only limited content is supported by progressbuffer."), NULL,
                                             ("progressbuffer.c"), ("progress_buffer_enqueue_item"), 0);
                    gst_event_unref(event); // INLINE - gst_event_unref()
                    return GST_FLOW_ERROR;
                }

                if (update) // Updating segments create new cache.
                {
                    if (element->cache)
                        destroy_cache(element->cache);

                    element->cache = create_cache();
                    if (!element->cache)
                    {
                        gst_element_message_full(GST_ELEMENT(element), GST_MESSAGE_ERROR, GST_RESOURCE_ERROR, GST_RESOURCE_ERROR_OPEN_READ_WRITE,
                                                 g_strdup("Couldn't create backing cache"), NULL,
                                                 ("progressbuffer.c"), ("progress_buffer_enqueue_item"), 0);
                        gst_event_unref(event); // INLINE - gst_event_unref()
                        return GST_FLOW_ERROR;
                    }
                }
                else
                {
                    cache_set_write_position(element->cache, 0);
                    cache_set_read_position(element->cache, 0);
                    element->cache_read_offset = start;
                }

                gst_segment_set_newsegment_full (&element->sink_segment, update, rate, arate,
                                                 GST_FORMAT_BYTES, start, stop, time);
                progress_buffer_set_pending_event(element, event);
                element->instant_seek = TRUE;

                signal = send_position_message(element, TRUE);
                break;
            }

            default:
                gst_event_unref(event); // INLINE - gst_event_unref()
                break;
        }
    }

    if (signal)
        g_cond_signal(element->add_cond);

    return GST_FLOW_OK;
}

/***********************************************************************************
 * Seek implementation
 ***********************************************************************************/
static gboolean progress_buffer_perform_push_seek(ProgressBuffer *element, GstPad *pad, GstEvent *event)
{
    GstFormat    format;
    gdouble      rate;
    GstSeekFlags flags;
    GstSeekType  start_type, stop_type;
    gint64       position;

    gst_event_parse_seek(event, &rate, &format, &flags, &start_type, &position, &stop_type, NULL);

    if (format != GST_FORMAT_BYTES || start_type != GST_SEEK_TYPE_SET)
        return FALSE;

    if (stop_type != GST_SEEK_TYPE_NONE)
    {
        gst_element_message_full(GST_ELEMENT(element),
            GST_MESSAGE_WARNING,
            GST_CORE_ERROR,
            GST_CORE_ERROR_SEEK, g_strdup("stop_type != GST_SEEK_TYPE_NONE. Seeking to stop is not supported."), NULL,
            ("progressbuffer.c"), ("progress_buffer_perform_push_seek"), 0);
        return FALSE;
    }

    if (flags & GST_SEEK_FLAG_FLUSH)
        gst_pad_push_event(pad, gst_event_new_flush_start());

    // Signal the task to stop if it's waiting.
    g_mutex_lock(element->lock);
    element->srcresult = GST_FLOW_WRONG_STATE;
    g_cond_signal(element->add_cond);
    g_mutex_unlock(element->lock);

    GST_PAD_STREAM_LOCK(pad); // Wait for task to stop

    g_mutex_lock(element->lock);
    element->srcresult = GST_FLOW_OK;

#ifdef ENABLE_SOURCE_SEEKING
    element->instant_seek = (position >= element->sink_segment.start &&
                             position - element->sink_segment.last_stop <= element->bandwidth * element->wait_tolerance);

    if (element->instant_seek)
    {
        cache_set_read_position(element->cache, position - element->cache_read_offset);
        progress_buffer_set_pending_event(element, gst_event_new_new_segment(FALSE, rate, GST_FORMAT_BYTES, position, element->sink_segment.stop, position));
    }
    else
        reset_eos(element);
#else
    cache_set_read_position(element->cache, position - element->cache_read_offset);
    progress_buffer_set_pending_event(element, gst_event_new_new_segment(FALSE, rate, GST_FORMAT_BYTES, position, element->sink_segment.stop, position));
#endif

    g_mutex_unlock(element->lock);

    if (flags & GST_SEEK_FLAG_FLUSH)
        gst_pad_push_event(pad, gst_event_new_flush_stop());

#ifdef ENABLE_SOURCE_SEEKING
    if (!element->instant_seek)
    {
        if (!gst_pad_push_event(element->sinkpad, gst_event_new_seek(rate, GST_FORMAT_BYTES, GST_SEEK_FLAG_NONE, GST_SEEK_TYPE_SET, position, GST_SEEK_TYPE_NONE, 0)))
        {
            element->instant_seek = TRUE;
            cache_set_read_position(element->cache, position - element->cache_read_offset);
            progress_buffer_set_pending_event(element, gst_event_new_new_segment(FALSE, rate, GST_FORMAT_BYTES, position, element->sink_segment.stop, position));
        }
    }
#endif

    gst_pad_start_task(element->srcpad, progress_buffer_loop, element);
    GST_PAD_STREAM_UNLOCK(pad);

// INLINE - gst_event_unref()
    gst_event_unref(event);
    return TRUE;
}

/***********************************************************************************
 * chain, loop, sink_event and src_event, buffer_alloc
 ***********************************************************************************/
/**
 * progress_buffer_chain()
 *
 * Primary function for push-mode.  Receives data from progressbuffer's sink pad.
 */
static GstFlowReturn progress_buffer_chain(GstPad *pad, GstBuffer *data)
{
    ProgressBuffer *element = PROGRESS_BUFFER(GST_PAD_PARENT(pad));
    GstFlowReturn  result = GST_FLOW_OK;

    //Try to enqueue the data
    g_mutex_lock(element->lock);

    if (element->eos_status.eos || element->unexpected)
        result = GST_FLOW_UNEXPECTED;
    else
        result = progress_buffer_enqueue_item(element, GST_MINI_OBJECT_CAST(data));

    g_mutex_unlock(element->lock);

// INLINE - gst_buffer_unref()
    gst_buffer_unref(data);

    // Here we can maintain some prebuffering strategy.
    if (!element->srcpad)
        progress_buffer_create_sourcepad(element);

    return result;
}

/**
 * send_underrun_message
 *
 * Sends UNDERRUN message to the bus.
 */
static void send_underrun_message(ProgressBuffer* element)
{
    GstStructure *s = gst_structure_empty_new(PB_MESSAGE_UNDERRUN);
    GstMessage *msg = gst_message_new_application(GST_OBJECT(element), s);

    gst_element_post_message(GST_ELEMENT(element), msg);
}

/**
 * progress_buffer_loop()
 *
 * Primary function for push-mode.  Pulls data from progressbuffer's cache queue.
 */
static void progress_buffer_loop(void *data)
{
    ProgressBuffer *element = PROGRESS_BUFFER(data);
    GstFlowReturn  result;
    gboolean       skip = FALSE;

    g_mutex_lock(element->lock);

next_item:
    while (element->srcresult == GST_FLOW_OK &&
           element->pending_src_event == NULL &&
           (!cache_has_enough_data(element->cache) || !element->instant_seek))
    {
        if (element->instant_seek)
            send_underrun_message(element);
        g_cond_wait(element->add_cond, element->lock);
    }

    result = element->srcresult;

    if (result == GST_FLOW_OK)
    {
        if (element->pending_src_event)
        {
            GstEvent *event = gst_event_ref(element->pending_src_event);
            progress_buffer_set_pending_event(element, NULL);

            switch(GST_EVENT_TYPE (event))
            {
                case GST_EVENT_EOS:
                    result = GST_FLOW_UNEXPECTED;
                    break;
                case GST_EVENT_NEWSEGMENT:
                    skip = FALSE;
                    break;
                default:
                    if (skip)
                    {
                        gst_event_unref (event); // INLINE - gst_event_unref()
                        goto next_item;
                    }
                    break;
            }
            element->srcresult = result;
            g_mutex_unlock(element->lock);
            gst_pad_push_event (element->srcpad, event);
        }
        else // create a buffer
        {
            GstBuffer *buffer = NULL;
            guint64 read_position = cache_read_buffer(element->cache, &buffer);
            read_position += element->cache_read_offset;
            GST_BUFFER_OFFSET(buffer) = read_position - GST_BUFFER_SIZE(buffer);

            if (read_position == element->sink_segment.stop)
                progress_buffer_set_pending_event(element, gst_event_new_eos());

            if (skip)
            {
                gst_buffer_unref(buffer); // INLINE - gst_buffer_unref()
                goto next_item;
            }
            else
            {
                GstCaps *caps;

                g_mutex_unlock(element->lock);

                caps = GST_BUFFER_CAPS(buffer);
                if (caps && caps != GST_PAD_CAPS(element->srcpad))
                    gst_pad_set_caps (element->srcpad, caps);

                // Send the data to the progressbuffer source pad
                result = gst_pad_push(element->srcpad, buffer);

                // Switch to skip mode. No we can only pass EOS and NEWSEGMENT events.
                if (result == GST_FLOW_UNEXPECTED)
                {
                    g_mutex_lock(element->lock);
                    skip = TRUE;
                    goto next_item;
                }

                g_mutex_lock(element->lock);
                element->srcresult = result;
                g_mutex_unlock(element->lock);
            }
        }
    }
    else
    {
        if (skip) // Run out of items in skip mode. Expecting only EOS or NEWSEGMENT in _chain()
        {
            element->unexpected = TRUE;
            result = GST_FLOW_OK;
        }
        g_mutex_unlock(element->lock);
    }

    if (result != GST_FLOW_OK)
        gst_pad_pause_task(element->srcpad);
}

/**
 * progress_buffer_sink_event()
 *
 * Receives event from the sink pad (currently, data from javasource).  When an event comes in,
 * we get the data from the pad by getting at the ProgressBuffer* object associated with the pad.
 */
static gboolean progress_buffer_sink_event(GstPad *pad, GstEvent *event)
{
    ProgressBuffer *element = PROGRESS_BUFFER(GST_PAD_PARENT(pad));
    gboolean       result = TRUE;

    if (GST_EVENT_IS_SERIALIZED (event) && GST_EVENT_TYPE(event) != GST_EVENT_FLUSH_STOP)
    {
        g_mutex_lock(element->lock);

        if (element->eos_status.eos)
        {
// INLINE - gst_event_unref()
            gst_event_unref(event);
            result = FALSE;
        }
        else
            progress_buffer_enqueue_item(element, GST_MINI_OBJECT_CAST(event));

        g_mutex_unlock(element->lock);
    }
    else
        result = gst_pad_push_event(element->srcpad, event);

    return result;

}

static gboolean progress_buffer_src_event(GstPad *pad, GstEvent *event)
{
    ProgressBuffer *element = PROGRESS_BUFFER(GST_PAD_PARENT(pad));
    if (pad->mode == GST_ACTIVATE_PUSH)
    {
        switch (GST_EVENT_TYPE (event))
        {
            case GST_EVENT_SEEK:
                return progress_buffer_perform_push_seek(element, pad, event);
            default:
                break;
        }
    }
    else if (pad->mode == GST_ACTIVATE_PULL) // Isolate the source element from all upcoming events
    {
// INLINE - gst_event_unref()
        gst_event_unref(event);
        return TRUE;
    }

    return gst_pad_event_default(pad, event);
}

static GstFlowReturn
progress_buffer_bufferalloc (GstPad *pad, guint64 offset, guint size,
                             GstCaps *caps, GstBuffer **buffer)
{
    ProgressBuffer *element = PROGRESS_BUFFER(GST_PAD_PARENT(pad));

    *buffer = gst_buffer_new ();
    GST_BUFFER_SIZE(*buffer) = size;
    GST_BUFFER_OFFSET(*buffer) = offset;

    if (size > element->incoming_buffer_size)
    {
        element->incoming_buffer = (guint8*)g_realloc(element->incoming_buffer, size);
        element->incoming_buffer_size = size;
    }
    GST_BUFFER_DATA(*buffer) = element->incoming_buffer;
    GST_BUFFER_CAPS(*buffer) = caps;
    return GST_FLOW_OK;
}

/***********************************************************************************
 * Pull-range function
 ***********************************************************************************/
#if ENABLE_PULL_MODE
#define VALID_RANGE(value)  (value != NO_RANGE_REQUEST)

static inline gboolean pending_range_start(ProgressBuffer *element)
{
    return (VALID_RANGE(element->range_start) &&
            element->sink_segment.start > element->range_start);
}

static inline gboolean pending_range_stop(ProgressBuffer *element)
{
    return (VALID_RANGE(element->range_stop) &&
            element->sink_segment.last_stop < element->range_stop);
}

static gpointer progress_buffer_range_monitor(ProgressBuffer *element)
{
    g_mutex_lock(element->lock);

check_loop:
    while (element->srcresult == GST_FLOW_OK && !pending_eos(element) &&
           (pending_range_start(element) || pending_range_stop(element) ||
           !VALID_RANGE(element->range_start) && !VALID_RANGE(element->range_stop)))
    {
        g_cond_wait(element->add_cond, element->lock);
    }

    if (element->srcresult == GST_FLOW_OK && (VALID_RANGE(element->range_start) || VALID_RANGE(element->range_stop)))
    {
        element->range_stop = element->range_start = NO_RANGE_REQUEST;
        g_mutex_unlock(element->lock);
        gst_pad_push_event(element->srcpad, gst_event_new_custom(FX_EVENT_RANGE_READY, NULL));
        g_mutex_lock(element->lock);
        goto check_loop;
    }
    else
        g_mutex_unlock(element->lock);

    return NULL;
}
#endif

static GstFlowReturn progress_buffer_getrange(GstPad *pad, guint64 start_position,
                                              guint size, GstBuffer **buffer)
{
#if ENABLE_PULL_MODE
    ProgressBuffer *element = PROGRESS_BUFFER(GST_PAD_PARENT(pad));
    GstFlowReturn  result = GST_FLOW_OK;
    guint64        end_position = start_position + size;
    gboolean       needs_seeking = FALSE;

    g_mutex_lock(element->lock); // Use one lock for push and pull modes

    if (element->sink_segment.stop < (gint64)end_position)
        result = GST_FLOW_UNEXPECTED;
    else if (element->sink_segment.start <= (gint64)start_position &&
             element->sink_segment.last_stop >= (gint64)end_position)
        result = cache_read_buffer_from_position(element->cache, start_position, size, buffer);
    else
    {
#if ENABLE_SOURCE_SEEKING
        needs_seeking = element->sink_segment.start > (gint64)start_position;
        if (needs_seeking)
        {
            element->range_start = start_position;
            reset_eos(element);
        }
#endif
        if (element->sink_segment.last_stop < (gint64)end_position)
        {
            element->range_stop = end_position + (gint64)(element->bandwidth * element->prebuffer_time);

            if (element->sink_segment.stop < element->range_stop)
                element->range_stop = element->sink_segment.stop;

#if ENABLE_SOURCE_SEEKING
            needs_seeking = element->bandwidth > 0 &&
                end_position - element->sink_segment.last_stop > element->bandwidth * element->wait_tolerance;
#endif
        }

        send_underrun_message(element);
        result = GST_FLOW_WRONG_STATE;
    }

    g_mutex_unlock(element->lock);

    if (needs_seeking)
        gst_pad_push_event(element->sinkpad, gst_event_new_seek(element->sink_segment.rate, GST_FORMAT_BYTES, GST_SEEK_FLAG_NONE,
            GST_SEEK_TYPE_SET, start_position, GST_SEEK_TYPE_NONE, 0));

    return result;
#else
    ProgressBuffer *element = PROGRESS_BUFFER(GST_PAD_PARENT(pad));
    return gst_pad_pull_range(element->sinkpad, start_position, size, buffer);
#endif
}

static gboolean progress_buffer_checkgetrange(GstPad *pad)
{
    ProgressBuffer *element = PROGRESS_BUFFER(GST_PAD_PARENT(pad));
#if ENABLE_PULL_MODE
    gboolean    result = FALSE;
    GstStructure *s = gst_structure_new(GETRANGE_QUERY_NAME, NULL);
    GstQuery *query = gst_query_new_application(GST_QUERY_CUSTOM, s);
    if (gst_pad_peer_query(pad, query))
        result = gst_structure_get_boolean(s, GETRANGE_QUERY_SUPPORTS_FIELDNANE, &result) && result;
// INLINE - gst_query_unref()
    gst_query_unref(query);
    return result;
#else
    return gst_pad_check_pull_range(element->sinkpad);
#endif
}

/***********************************************************************************
 * State change handler
 ***********************************************************************************/
static GstStateChangeReturn progress_buffer_change_state (GstElement *e,
                                                          GstStateChange transition)
{
    ProgressBuffer *element = PROGRESS_BUFFER(e);
    GstStateChangeReturn ret = GST_ELEMENT_CLASS (parent_class)->change_state (e, transition);

    if (ret == GST_STATE_CHANGE_FAILURE)
        return ret;

    switch (transition)
    {
        case GST_STATE_CHANGE_PAUSED_TO_READY:
            g_mutex_lock(element->lock);
            element->srcresult = GST_FLOW_WRONG_STATE;
            progress_buffer_flush_data(element);
            g_cond_signal(element->add_cond); // Signal the task to stop if it's waiting.
            g_mutex_unlock(element->lock);
            break;

        default:
            break;
    }
    return ret;
}

/***********************************************************************************
 * Plugin registration infrastructure
 ***********************************************************************************/

gboolean progress_buffer_plugin_init (GstPlugin *plugin)
{
    GST_DEBUG_CATEGORY_INIT (progress_buffer_debug, PROGRESS_BUFFER_PLUGIN_NAME,
            0, ELEMENT_DESCRIPTION);

    return gst_element_register (plugin, PROGRESS_BUFFER_PLUGIN_NAME,
                                 GST_RANK_NONE,
                                 PROGRESS_BUFFER_TYPE);
}
