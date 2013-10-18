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

    GMutex*       lock;
    GCond*        add_cond;
    GCond*        del_cond;

    Cache*        cache[NUM_OF_CACHED_SEGMENTS];
    guint         cache_size[NUM_OF_CACHED_SEGMENTS];
    gboolean      cache_write_ready[NUM_OF_CACHED_SEGMENTS];
    gint          cache_write_index;
    gint          cache_read_index;

    gboolean      send_new_segment;

    gboolean      is_flushing;
    gboolean      is_eos;

    GstFlowReturn srcresult;
};

struct _HLSProgressBufferClass
{
    GstElementClass parent;
};

/***********************************************************************************
 * Substitution for
 * GST_BOILERPLATE(HLSProgressBuffer, hls_progress_buffer, GstElement, GST_TYPE_ELEMENT);
 ***********************************************************************************/
static void hls_progress_buffer_base_init     (gpointer             g_class);
static void hls_progress_buffer_class_init    (HLSProgressBufferClass *g_class);
static void hls_progress_buffer_init          (HLSProgressBuffer      *object,  HLSProgressBufferClass *g_class);
static GstElementClass *parent_class = NULL;

static void hls_progress_buffer_class_init_trampoline (gpointer g_class, gpointer data)
{
    parent_class = (GstElementClass *)g_type_class_peek_parent (g_class);
    hls_progress_buffer_class_init ((HLSProgressBufferClass *)g_class);
}

GType hls_progress_buffer_get_type (void)
{
    static volatile gsize gonce_data = 0;
// INLINE - g_once_init_enter()
    if (g_once_init_enter (&gonce_data))
    {
        GType _type;
        _type = gst_type_register_static_full (GST_TYPE_ELEMENT,
            g_intern_static_string ("HLSProgressBuffer"),
            sizeof (HLSProgressBufferClass),
            hls_progress_buffer_base_init,
            NULL,
            hls_progress_buffer_class_init_trampoline,
            NULL,
            NULL,
            sizeof (HLSProgressBuffer),
            0,
            (GInstanceInitFunc) hls_progress_buffer_init,
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
    GST_PAD_SRC, GST_PAD_ALWAYS, GST_STATIC_CAPS_ANY);

static void hls_progress_buffer_base_init (gpointer g_class)
{
    GstElementClass *element_class = GST_ELEMENT_CLASS (g_class);

    gst_element_class_set_details_simple (element_class,
        "HLS Progressive download plugin",
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
static void                 hls_progress_buffer_finalize (GObject *object);
static GstStateChangeReturn hls_progress_buffer_change_state (GstElement *element, GstStateChange transition);
static GstFlowReturn        hls_progress_buffer_chain(GstPad *pad, GstBuffer *data);
static gboolean             hls_progress_buffer_activatepush_src(GstPad *pad, gboolean active);
static gboolean             hls_progress_buffer_sink_event(GstPad *pad, GstEvent *event);
static gboolean             hls_progress_buffer_src_event(GstPad *pad, GstEvent *event);
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

    gobject_class->finalize = hls_progress_buffer_finalize;
    GST_ELEMENT_CLASS (klass)->change_state = hls_progress_buffer_change_state;

    cache_static_init();
}

/**
 * hls_progress_buffer_init()
 *
 * Initializer.  Automatically declared in the GST_BOILERPLATE macro above.  Should be
 * only called by GStreamer.
 */
static void hls_progress_buffer_init(HLSProgressBuffer *element, HLSProgressBufferClass *element_klass)
{
    GstElementClass *klass = GST_ELEMENT_CLASS (element_klass);
    int i = 0;

    element->sinkpad = gst_pad_new_from_template (gst_element_class_get_pad_template (klass, "sink"), "sink");
    gst_pad_set_chain_function(element->sinkpad, hls_progress_buffer_chain);
    gst_pad_set_event_function(element->sinkpad, hls_progress_buffer_sink_event);
    gst_element_add_pad (GST_ELEMENT (element), element->sinkpad);

    element->srcpad = gst_pad_new_from_template (gst_element_class_get_pad_template (GST_ELEMENT_GET_CLASS(element), "src"), "src");
    gst_pad_set_activatepush_function(element->srcpad, hls_progress_buffer_activatepush_src);
    gst_pad_set_event_function(element->srcpad, hls_progress_buffer_src_event);
    gst_element_add_pad (GST_ELEMENT (element), element->srcpad);

    element->lock = g_mutex_new();
    element->add_cond = g_cond_new();
    element->del_cond = g_cond_new();

    for (i = 0; i < NUM_OF_CACHED_SEGMENTS; i++)
    {
        element->cache[i] = create_cache();
        element->cache_size[i] = 0;
        element->cache_write_ready[i] = TRUE;
    }

    element->cache_write_index = -1;
    element->cache_read_index = 0;

    element->send_new_segment = TRUE;

    element->is_flushing = FALSE;
    element->is_eos = FALSE;

    element->srcresult = GST_FLOW_OK;
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

    g_mutex_free(element->lock);
    g_cond_free(element->add_cond);
    g_cond_free(element->del_cond);

    G_OBJECT_CLASS (parent_class)->finalize (object);
}

/**
 * hls_progress_buffer_activatepush_src()
 *
 * Set the source pad's push mode.
 */
static gboolean hls_progress_buffer_activatepush_src(GstPad *pad, gboolean active)
{
    HLSProgressBuffer *element = HLS_PROGRESS_BUFFER(GST_PAD_PARENT(pad));

    if (active)
    {
        g_mutex_lock(element->lock);
        element->srcresult = GST_FLOW_OK;
        g_mutex_unlock(element->lock);

        if (gst_pad_is_linked(pad))
            return gst_pad_start_task(pad, hls_progress_buffer_loop, element);
        else
            return TRUE;
    }
    else
    {
        g_mutex_lock(element->lock);
        element->srcresult = GST_FLOW_WRONG_STATE;
        g_cond_signal(element->add_cond);
        g_cond_signal(element->del_cond);
        g_mutex_unlock(element->lock);

        return gst_pad_stop_task(pad);
    }
}

/***********************************************************************************
 * Internal functions
 ***********************************************************************************/
static void hls_progress_buffer_flush_data(HLSProgressBuffer *element)
{
    guint i = 0;

    g_mutex_lock(element->lock);

    element->srcresult = GST_FLOW_WRONG_STATE;

    g_cond_signal(element->add_cond);
    g_cond_signal(element->del_cond);

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

    g_mutex_unlock(element->lock);
}

/***********************************************************************************
 * chain, loop, sink_event and src_event, buffer_alloc
 ***********************************************************************************/
/**
 * hls_progress_buffer_chain()
 *
 * Primary function for push-mode.  Receives data from hls progressbuffer's sink pad.
 */
static GstFlowReturn hls_progress_buffer_chain(GstPad *pad, GstBuffer *data)
{
    HLSProgressBuffer *element = HLS_PROGRESS_BUFFER(GST_PAD_PARENT(pad));
    GstFlowReturn  result = GST_FLOW_OK;

    if (element->is_flushing || element->is_eos)
    {
        // INLINE - gst_buffer_unref()
        gst_buffer_unref(data);
        return GST_FLOW_WRONG_STATE;
    }

    g_mutex_lock(element->lock);
    cache_write_buffer(element->cache[element->cache_write_index], data);
    g_cond_signal(element->add_cond);
    g_mutex_unlock(element->lock);

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
    GstStructure *s = gst_structure_empty_new(HLS_PB_MESSAGE_RESUME);
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
    GstStructure *s = gst_structure_empty_new(HLS_PB_MESSAGE_HLS_EOS);
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
    GstStructure *s = gst_structure_empty_new(HLS_PB_MESSAGE_FULL);
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
    GstStructure *s = gst_structure_empty_new(HLS_PB_MESSAGE_NOT_FULL);
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

    g_mutex_lock(element->lock);

    while (element->srcresult == GST_FLOW_OK && !cache_has_enough_data(element->cache[element->cache_read_index]))
    {
        if (element->is_eos)
        {
            gst_pad_push_event(element->srcpad, gst_event_new_eos());
            element->srcresult = GST_FLOW_WRONG_STATE;
            break;
        }

        if (!element->is_eos)
        {
            g_cond_wait(element->add_cond, element->lock);
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
            g_cond_signal(element->del_cond);
        }

        g_mutex_unlock(element->lock);

        gst_buffer_set_caps(buffer, GST_PAD_CAPS(element->sinkpad));

        // Send the data to the hls progressbuffer source pad
        result = gst_pad_push(element->srcpad, buffer);

        g_mutex_lock(element->lock);
        if (GST_FLOW_OK == element->srcresult || GST_FLOW_OK != result)
            element->srcresult = result;
        else
            result = element->srcresult;
        g_mutex_unlock(element->lock);
    }
    else
    {
        g_mutex_unlock(element->lock);
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
static gboolean hls_progress_buffer_sink_event(GstPad *pad, GstEvent *event)
{
    HLSProgressBuffer *element = HLS_PROGRESS_BUFFER(GST_PAD_PARENT(pad));
    gboolean ret = FALSE;

    switch (GST_EVENT_TYPE (event))
    {
    case GST_EVENT_NEWSEGMENT:
        {
            gboolean update;
            GstFormat format;
            gdouble rate, arate;
            gint64 start, stop, time;

            g_mutex_lock(element->lock);
            if (element->srcresult != GST_FLOW_OK)
            {
                // INLINE - gst_event_unref()
                gst_event_unref(event);
                g_mutex_unlock(element->lock);
                return TRUE;
            }
            g_mutex_unlock(element->lock);

            if (element->is_eos)
            {
                element->is_eos = FALSE;
                element->srcresult = GST_FLOW_OK;
                if (gst_pad_is_linked(element->srcpad))
                    gst_pad_start_task(element->srcpad, hls_progress_buffer_loop, element);
            }

            // In HLS mode javasource will set time to correct position in time unit, even if segment in byte units.
            // Maybe not perfect, but works.
            gst_event_parse_new_segment_full(event, &update, &rate, &arate, &format, &start, &stop, &time);
            // INLINE - gst_event_unref()
            gst_event_unref(event);
            ret = TRUE;

            if (stop - start <= 0)
            {
                gst_element_message_full(GST_ELEMENT(element), GST_MESSAGE_ERROR, GST_STREAM_ERROR, GST_STREAM_ERROR_WRONG_TYPE, g_strdup("Only limited content is supported by hlsprogressbuffer."), NULL, ("hlsprogressbuffer.c"), ("hls_progress_buffer_src_event"), 0);
                return TRUE;
            }

            if (element->send_new_segment)
            {
                event = gst_event_new_new_segment(update, rate, GST_FORMAT_TIME, 0, -1, time);
                element->send_new_segment = FALSE;
                ret = gst_pad_push_event(element->srcpad, event);
            }

            // Get and prepare next write segment
            g_mutex_lock(element->lock);
            element->cache_write_index = (element->cache_write_index + 1) % NUM_OF_CACHED_SEGMENTS;

            while (element->srcresult == GST_FLOW_OK && !element->cache_write_ready[element->cache_write_index])
            {
                g_mutex_unlock(element->lock);
                send_hls_full_message(element);
                g_mutex_lock(element->lock);
                g_cond_wait(element->del_cond, element->lock);
                if (element->srcresult != GST_FLOW_OK)
                {
                    g_mutex_unlock(element->lock);
                    return TRUE;
                }
            }
            element->cache_size[element->cache_write_index] = stop;
            element->cache_write_ready[element->cache_write_index] = FALSE;
            cache_set_write_position(element->cache[element->cache_write_index], 0);
            cache_set_read_position(element->cache[element->cache_write_index], 0);

            g_mutex_unlock(element->lock);

            send_hls_resume_message(element); // Send resume message for each segment
        }
        break;
    case GST_EVENT_FLUSH_START:
        g_mutex_lock(element->lock);
        element->is_flushing = TRUE;
        g_mutex_unlock(element->lock);

        ret = gst_pad_push_event(element->srcpad, event);
        hls_progress_buffer_flush_data(element);

        if (gst_pad_is_linked(element->srcpad))
            gst_pad_pause_task(element->srcpad);

        break;
    case GST_EVENT_FLUSH_STOP:
        ret = gst_pad_push_event(element->srcpad, event);

        g_mutex_lock(element->lock);

        element->send_new_segment = TRUE;
        element->is_flushing = FALSE;
        element->srcresult = GST_FLOW_OK;
        
        if (!element->is_eos && gst_pad_is_linked(element->srcpad))
            gst_pad_start_task(element->srcpad, hls_progress_buffer_loop, element);

        g_mutex_unlock(element->lock);

        break;
    case GST_EVENT_EOS:
        send_hls_eos_message(element); // Just in case we stall

        g_mutex_lock(element->lock);
        element->is_eos = TRUE;
        g_cond_signal(element->add_cond);
        g_mutex_unlock(element->lock);
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

static gboolean hls_progress_buffer_src_event(GstPad *pad, GstEvent *event)
{
    return gst_pad_event_default(pad, event);
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
