/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "hlsprogressbuffer.h"
#include "cache.h"

/***********************************************************************************
 * Debug category init
 ***********************************************************************************/
GST_DEBUG_CATEGORY (hls_progress_buffer_debug);
#define GST_CAT_DEFAULT hls_progress_buffer_debug

#define ELEMENT_DESCRIPTION "JFX HLS Progress buffer element"

/***********************************************************************************
 * Element structures are hidden from outside
 ***********************************************************************************/
#define NUM_OF_CACHED_SEGMENTS 3

struct _HLSProgressBuffer
{
    GstElement    parent;

    GstPad*       sinkpad;
    GstPad*       srcpad;

    GMutex       lock;
    GCond        add_cond;
    GCond        del_cond;

    Cache*        cache[NUM_OF_CACHED_SEGMENTS];
    guint         cache_size[NUM_OF_CACHED_SEGMENTS];
    gboolean      cache_write_ready[NUM_OF_CACHED_SEGMENTS];
    gint          cache_write_index;
    gint          cache_read_index;

    gboolean      send_new_segment;
    gboolean      set_src_caps;

    gboolean      is_flushing;
    gboolean      is_eos;

    GstFlowReturn srcresult;

    GstClockTime buffer_pts;
};

struct _HLSProgressBufferClass
{
    GstElementClass parent;
};

/***********************************************************************************
 * Substitution for
 * G_DEFINE_TYPE(HLSProgressBuffer, hls_progress_buffer, GstElement, GST_TYPE_ELEMENT);
 ***********************************************************************************/
#define hls_progress_buffer_parent_class parent_class
static void hls_progress_buffer_init          (HLSProgressBuffer      *self);
static void hls_progress_buffer_class_init    (HLSProgressBufferClass *klass);
static gpointer hls_progress_buffer_parent_class = NULL;
static void     hls_progress_buffer_class_intern_init (gpointer klass)
{
    hls_progress_buffer_parent_class = g_type_class_peek_parent (klass);
    hls_progress_buffer_class_init ((HLSProgressBufferClass*) klass);
}

GType hls_progress_buffer_get_type (void)
{
    static volatile gsize gonce_data = 0;
// INLINE - g_once_init_enter()
    if (g_once_init_enter (&gonce_data))
    {
        GType _type;
        _type = g_type_register_static_simple (GST_TYPE_ELEMENT,
               g_intern_static_string ("HLSProgressBuffer"),
               sizeof (HLSProgressBufferClass),
               (GClassInitFunc) hls_progress_buffer_class_intern_init,
               sizeof(HLSProgressBuffer),
               (GInstanceInitFunc) hls_progress_buffer_init,
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
    GST_PAD_SRC, GST_PAD_ALWAYS, GST_STATIC_CAPS_ANY);

/***********************************************************************************
 * Instance init and forward declarations
 ***********************************************************************************/
static void                 hls_progress_buffer_finalize (GObject *object);
static GstStateChangeReturn hls_progress_buffer_change_state (GstElement *element, GstStateChange transition);
static GstFlowReturn        hls_progress_buffer_chain(GstPad *pad, GstObject *parent, GstBuffer *data);
static gboolean             hls_progress_buffer_activatemode(GstPad *pad, GstObject *parent, GstPadMode mode, gboolean active);
static gboolean             hls_progress_buffer_activatepush_src(GstPad *pad, GstObject *parent, gboolean active);
static gboolean             hls_progress_buffer_sink_event(GstPad *pad, GstObject *parent, GstEvent *event);
static void                 hls_progress_buffer_loop(void *data);
static void                 hls_progress_buffer_flush_data(HLSProgressBuffer *buffer);

/**
 * hls_progress_buffer_class_init()
 *
 * Sets up the GLib object oriented C class structure for ProgressBuffer.
 */
static void hls_progress_buffer_class_init (HLSProgressBufferClass *klass)
{
    GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
    GstElementClass *element_class = GST_ELEMENT_CLASS (klass);

    gst_element_class_set_metadata (element_class,
        "HLS Progressive download plugin",
        "Element",
        "Progressively stores incoming data in memory or file",
        "Oracle Corporation");

    gst_element_class_add_pad_template (element_class,
        gst_static_pad_template_get (&sink_template));
    gst_element_class_add_pad_template (element_class,
        gst_static_pad_template_get (&source_template));

    gobject_class->finalize = hls_progress_buffer_finalize;
    GST_ELEMENT_CLASS (klass)->change_state = hls_progress_buffer_change_state;

    cache_static_init();
}

/**
 * hls_progress_buffer_init()
 *
 * Initializer.  Automatically declared in the G_DEFINE_TYPE macro above.  Should be
 * only called by GStreamer.
 */
static void hls_progress_buffer_init(HLSProgressBuffer *element)
{
    int i = 0;

    element->sinkpad = gst_pad_new_from_template (gst_element_class_get_pad_template (GST_ELEMENT_GET_CLASS(element), "sink"), "sink");
    gst_pad_set_chain_function(element->sinkpad, hls_progress_buffer_chain);
    gst_pad_set_event_function(element->sinkpad, hls_progress_buffer_sink_event);
    gst_element_add_pad (GST_ELEMENT (element), element->sinkpad);

    element->srcpad = gst_pad_new_from_template (gst_element_class_get_pad_template (GST_ELEMENT_GET_CLASS(element), "src"), "src");
    gst_pad_set_activatemode_function (element->srcpad, hls_progress_buffer_activatemode);
    gst_element_add_pad (GST_ELEMENT (element), element->srcpad);

    g_mutex_init(&element->lock);
    g_cond_init(&element->add_cond);
    g_cond_init(&element->del_cond);

    for (i = 0; i < NUM_OF_CACHED_SEGMENTS; i++)
    {
        element->cache[i] = create_cache();
        element->cache_size[i] = 0;
        element->cache_write_ready[i] = TRUE;
    }

    element->cache_write_index = -1;
    element->cache_read_index = 0;

    element->send_new_segment = TRUE;
    element->set_src_caps = TRUE;

    element->is_flushing = FALSE;
    element->is_eos = FALSE;

    element->srcresult = GST_FLOW_OK;

    element->buffer_pts = GST_CLOCK_TIME_NONE;
}

/**
 * hls_progress_buffer_finalize()
 *
 * Equivalent of destructor.
 */
static void hls_progress_buffer_finalize (GObject *object)
{
    HLSProgressBuffer *element = HLS_PROGRESS_BUFFER(object);
    int i = 0;

    for (i = 0; i < NUM_OF_CACHED_SEGMENTS; i++)
    {
        if (element->cache[i])
            destroy_cache(element->cache[i]);
    }

    g_mutex_clear(&element->lock);
    g_cond_clear(&element->add_cond);
    g_cond_clear(&element->del_cond);

    G_OBJECT_CLASS (parent_class)->finalize (object);
}

/**
 * hls_progress_buffer_activatepush_src()
 *
 * Set the source pad's push mode.
 */
static gboolean hls_progress_buffer_activatemode(GstPad *pad, GstObject *parent, GstPadMode mode, gboolean active)
{
    gboolean res = FALSE;

    switch (mode) {
        case GST_PAD_MODE_PUSH:
            res = hls_progress_buffer_activatepush_src(pad, parent, active);
            break;
        default:
            /* unknown scheduling mode */
            res = FALSE;
            break;
    }

    return res;
}

static gboolean hls_progress_buffer_activatepush_src(GstPad *pad, GstObject *parent, gboolean active)
{
    HLSProgressBuffer *element = HLS_PROGRESS_BUFFER(parent);

    if (active)
    {
        g_mutex_lock(&element->lock);
        element->srcresult = GST_FLOW_OK;
        g_mutex_unlock(&element->lock);

        if (gst_pad_is_linked(pad))
            return gst_pad_start_task(pad, hls_progress_buffer_loop, element, NULL);
        else
            return TRUE;
    }
    else
    {
        g_mutex_lock(&element->lock);
        element->srcresult = GST_FLOW_FLUSHING;
        g_cond_signal(&element->add_cond);
        g_cond_signal(&element->del_cond);
        g_mutex_unlock(&element->lock);

        return gst_pad_stop_task(pad);
    }
}

/***********************************************************************************
 * Internal functions
 ***********************************************************************************/
static void hls_progress_buffer_flush_data(HLSProgressBuffer *element)
{
    guint i = 0;

    g_mutex_lock(&element->lock);

    element->srcresult = GST_FLOW_FLUSHING;

    g_cond_signal(&element->add_cond);
    g_cond_signal(&element->del_cond);

    element->cache_write_index = -1;
    element->cache_read_index = 0;
    for (i = 0; i < NUM_OF_CACHED_SEGMENTS; i++)
    {
        if (element->cache[i])
        {
            cache_set_write_position(element->cache[i], 0);
            cache_set_read_position(element->cache[i], 0);
            element->cache_size[i] = 0;
            element->cache_write_ready[i] = TRUE;
        }
    }

    g_mutex_unlock(&element->lock);
}

/***********************************************************************************
 * chain, loop, sink_event and src_event, buffer_alloc
 ***********************************************************************************/
/**
 * hls_progress_buffer_chain()
 *
 * Primary function for push-mode.  Receives data from hls progressbuffer's sink pad.
 */
static GstFlowReturn hls_progress_buffer_chain(GstPad *pad, GstObject *parent, GstBuffer *data)
{
    HLSProgressBuffer *element = HLS_PROGRESS_BUFFER(parent);
    GstFlowReturn  result = GST_FLOW_OK;

    if (element->is_flushing || element->is_eos)
    {
        // INLINE - gst_buffer_unref()
        gst_buffer_unref(data);
        return GST_FLOW_FLUSHING;
    }

    g_mutex_lock(&element->lock);
    if (element->srcresult != GST_FLOW_FLUSHING)
    {
        cache_write_buffer(element->cache[element->cache_write_index], data);
        g_cond_signal(&element->add_cond);
    }
    g_mutex_unlock(&element->lock);

    // INLINE - gst_buffer_unref()
    gst_buffer_unref(data);

    return result;
}

/**
 * send_hls_resume_message
 *
 * Sends HLS RESUME message to the bus.
 */
static void send_hls_resume_message(HLSProgressBuffer* element)
{
    GstStructure *s = gst_structure_new_empty(HLS_PB_MESSAGE_RESUME);
    GstMessage *msg = gst_message_new_application(GST_OBJECT(element), s);
    gst_element_post_message(GST_ELEMENT(element), msg);
}

/**
 * send_hls_eos_message
 *
 * Sends HLS EOS message to the bus.
 */
static void send_hls_eos_message(HLSProgressBuffer* element)
{
    GstStructure *s = gst_structure_new_empty(HLS_PB_MESSAGE_HLS_EOS);
    GstMessage *msg = gst_message_new_application(GST_OBJECT(element), s);
    gst_element_post_message(GST_ELEMENT(element), msg);
}

/**
 * send_hls_full_message
 *
 * Sends HLS FULL message to the bus.
 */
static void send_hls_full_message(HLSProgressBuffer* element)
{
    GstStructure *s = gst_structure_new_empty(HLS_PB_MESSAGE_FULL);
    GstMessage *msg = gst_message_new_application(GST_OBJECT(element), s);
    gst_element_post_message(GST_ELEMENT(element), msg);
}

/**
 * send_hls_not_full_message
 *
 * Sends HLS NOT FULL message to the bus.
 */
static void send_hls_not_full_message(HLSProgressBuffer* element)
{
    GstStructure *s = gst_structure_new_empty(HLS_PB_MESSAGE_NOT_FULL);
    GstMessage *msg = gst_message_new_application(GST_OBJECT(element), s);
    gst_element_post_message(GST_ELEMENT(element), msg);
}

/**
 * hls_progress_buffer_loop()
 *
 * Primary function for push-mode.  Pulls data from progressbuffer's cache queue.
 */
static void hls_progress_buffer_loop(void *data)
{
    HLSProgressBuffer* element = HLS_PROGRESS_BUFFER(data);
    GstFlowReturn      result = GST_FLOW_OK;

    g_mutex_lock(&element->lock);

    while (element->srcresult == GST_FLOW_OK && !cache_has_enough_data(element->cache[element->cache_read_index]))
    {
        if (element->is_eos)
        {
            gst_pad_push_event(element->srcpad, gst_event_new_eos());
            element->srcresult = GST_FLOW_FLUSHING;
            break;
        }

        if (!element->is_eos)
        {
            g_cond_wait(&element->add_cond, &element->lock);
        }
    }

    result = element->srcresult;

    if (result == GST_FLOW_OK)
    {
        GstBuffer *buffer = NULL;
        guint64 read_position = cache_read_buffer(element->cache[element->cache_read_index], &buffer);

        if (read_position == element->cache_size[element->cache_read_index])
        {
            element->cache_write_ready[element->cache_read_index] = TRUE;
            element->cache_read_index = (element->cache_read_index + 1) % NUM_OF_CACHED_SEGMENTS;
            send_hls_not_full_message(element);
            g_cond_signal(&element->del_cond);
        }

        if (element->buffer_pts != GST_CLOCK_TIME_NONE)
        {
            GST_BUFFER_TIMESTAMP(buffer) = element->buffer_pts;
            GST_BUFFER_DTS(buffer) = element->buffer_pts;
            element->buffer_pts = GST_CLOCK_TIME_NONE;
        }

        g_mutex_unlock(&element->lock);

        // Send the data to the hls progressbuffer source pad
        result = gst_pad_push(element->srcpad, buffer);

        g_mutex_lock(&element->lock);
        if (GST_FLOW_OK == element->srcresult || GST_FLOW_OK != result)
            element->srcresult = result;
        else
            result = element->srcresult;
        g_mutex_unlock(&element->lock);
    }
    else
    {
        g_mutex_unlock(&element->lock);
    }

    if (result != GST_FLOW_OK && !element->is_flushing)
        gst_pad_pause_task(element->srcpad);
}

/**
 * hls_progress_buffer_sink_event()
 *
 * Receives event from the sink pad (currently, data from javasource).  When an event comes in,
 * we get the data from the pad by getting at the ProgressBuffer* object associated with the pad.
 */
static gboolean hls_progress_buffer_sink_event(GstPad *pad, GstObject *parent, GstEvent *event)
{
    HLSProgressBuffer *element = HLS_PROGRESS_BUFFER(parent);
    gboolean ret = FALSE;

    switch (GST_EVENT_TYPE (event))
    {
    case GST_EVENT_SEGMENT:
        {
            GstSegment segment;

            g_mutex_lock(&element->lock);
            if (element->srcresult != GST_FLOW_OK)
            {
                // INLINE - gst_event_unref()
                gst_event_unref(event);
                g_mutex_unlock(&element->lock);
                return TRUE;
            }
            g_mutex_unlock(&element->lock);

            if (element->is_eos)
            {
                element->is_eos = FALSE;
                element->srcresult = GST_FLOW_OK;
                if (gst_pad_is_linked(element->srcpad))
                    gst_pad_start_task(element->srcpad, hls_progress_buffer_loop, element, NULL);
            }

            // In HLS mode javasource will set time to correct position in time unit, even if segment in byte units.
            // Maybe not perfect, but works.
            gst_event_copy_segment (event, &segment);
            // INLINE - gst_event_unref()
            gst_event_unref(event);
            ret = TRUE;

            if (segment.stop - segment.start <= 0)
            {
                gst_element_message_full(GST_ELEMENT(element), GST_MESSAGE_ERROR, GST_STREAM_ERROR, GST_STREAM_ERROR_WRONG_TYPE, g_strdup("Only limited content is supported by hlsprogressbuffer."), NULL, ("hlsprogressbuffer.c"), ("hls_progress_buffer_sink_event"), 0);
                return TRUE;
            }

            if (element->send_new_segment)
            {
                GstSegment new_segment;
                gst_segment_init (&new_segment, GST_FORMAT_TIME);
                new_segment.flags = segment.flags;
                new_segment.rate = segment.rate;
                new_segment.start = segment.position;
                new_segment.stop = -1;
                new_segment.position = segment.position;
                new_segment.time = segment.position;

                element->buffer_pts = segment.position;

                event = gst_event_new_segment (&new_segment);
                element->send_new_segment = FALSE;
                ret = gst_pad_push_event(element->srcpad, event);
            }

            // Get and prepare next write segment
            g_mutex_lock(&element->lock);
            element->cache_write_index = (element->cache_write_index + 1) % NUM_OF_CACHED_SEGMENTS;

            while (element->srcresult == GST_FLOW_OK && !element->cache_write_ready[element->cache_write_index])
            {
                g_mutex_unlock(&element->lock);
                send_hls_full_message(element);
                g_mutex_lock(&element->lock);
                g_cond_wait(&element->del_cond, &element->lock);
                if (element->srcresult != GST_FLOW_OK)
                {
                    g_mutex_unlock(&element->lock);
                    return TRUE;
                }
            }
            element->cache_size[element->cache_write_index] = segment.stop;
            element->cache_write_ready[element->cache_write_index] = FALSE;
            cache_set_write_position(element->cache[element->cache_write_index], 0);
            cache_set_read_position(element->cache[element->cache_write_index], 0);

            g_mutex_unlock(&element->lock);

            send_hls_resume_message(element); // Send resume message for each segment
        }
        break;
    case GST_EVENT_FLUSH_START:
        g_mutex_lock(&element->lock);
        element->is_flushing = TRUE;
        g_mutex_unlock(&element->lock);

        ret = gst_pad_push_event(element->srcpad, event);
        hls_progress_buffer_flush_data(element);

        if (gst_pad_is_linked(element->srcpad))
            gst_pad_pause_task(element->srcpad);

        break;
    case GST_EVENT_FLUSH_STOP:
        ret = gst_pad_push_event(element->srcpad, event);

        g_mutex_lock(&element->lock);

        element->send_new_segment = TRUE;
        element->is_flushing = FALSE;
        element->srcresult = GST_FLOW_OK;

        if (!element->is_eos && gst_pad_is_linked(element->srcpad))
            gst_pad_start_task(element->srcpad, hls_progress_buffer_loop, element, NULL);

        g_mutex_unlock(&element->lock);

        break;
    case GST_EVENT_EOS:
        send_hls_eos_message(element); // Just in case we stall

        g_mutex_lock(&element->lock);
        element->is_eos = TRUE;
        g_cond_signal(&element->add_cond);
        g_mutex_unlock(&element->lock);
        // INLINE - gst_event_unref()
        gst_event_unref(event);
        ret = TRUE;

        break;
    default:
        ret = gst_pad_push_event(element->srcpad, event);
        break;
    }

    return ret;
}

/***********************************************************************************
 * State change handler
 ***********************************************************************************/
static GstStateChangeReturn hls_progress_buffer_change_state (GstElement *e, GstStateChange transition)
{
    HLSProgressBuffer *element = HLS_PROGRESS_BUFFER(e);
    GstStateChangeReturn ret = GST_STATE_CHANGE_FAILURE;

    switch (transition)
    {
    case GST_STATE_CHANGE_PAUSED_TO_READY:
        hls_progress_buffer_flush_data(element);
        break;

    default:
        break;
    }

    ret = GST_ELEMENT_CLASS (parent_class)->change_state (e, transition);
    if (ret == GST_STATE_CHANGE_FAILURE)
        return ret;

    return ret;
}

/***********************************************************************************
 * Plugin registration infrastructure
 ***********************************************************************************/

gboolean hls_progress_buffer_plugin_init (GstPlugin *plugin)
{
    GST_DEBUG_CATEGORY_INIT (progress_buffer_debug, PROGRESS_BUFFER_PLUGIN_NAME, 0, ELEMENT_DESCRIPTION);

    return gst_element_register (plugin, HLS_PROGRESS_BUFFER_PLUGIN_NAME, GST_RANK_NONE, HLS_PROGRESS_BUFFER_TYPE);
}
