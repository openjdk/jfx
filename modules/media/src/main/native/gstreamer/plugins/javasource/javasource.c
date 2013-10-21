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

#include "javasource.h"
#include <string.h>
#include "marshal.h"

GST_DEBUG_CATEGORY (java_source_debug);
#define GST_CAT_DEFAULT java_source_debug

#define _BS(val) (val ? "TRUE" : "FALSE")
#define BUFFER_SIZE 4096

/***********************************************************************************
* HLS Properties and Values
***********************************************************************************/
// From HLSConnectionHolder.java
#define HLS_PROP_GET_DURATION                1
#define HLS_VALUE_FLOAT_MULTIPLIER           1000

/***********************************************************************************
* Signals
***********************************************************************************/
enum
{
    SIGNAL_SEEK_DATA,
    SIGNAL_READ_NEXT_BLOCK,
    SIGNAL_READ_BLOCK,
    SIGNAL_COPY_BLOCK,
    SIGNAL_CLOSE_CONNECTION,
    SIGNAL_PROPERTY,
    SIGNAL_GET_STREAM_SIZE,
    LAST_SIGNAL
};

enum
{
    PROP_0,
    PROP_SIZE,
    PROP_IS_SEEKABLE,
    PROP_IS_RANDOM_ACCESS,
    PROP_STOP_ON_PAUSE,
    PROP_LOCATION,
    PROP_MIMETYPE,
    PROP_HLS_MODE
};

/***********************************************************************************
* Modes
***********************************************************************************/
enum
{
    MODE_DEFAULT = 0x01,
    MODE_HLS = 0x02,
    MODE_HLS_LIVE = 0x04
};

/***********************************************************************************
* Element structures are hidden from outside
***********************************************************************************/
struct _JavaSource
{
    GstElement    parent;

    GMutex        *lock;
    GstFlowReturn srcresult;
    GstPad        *srcpad;

    GstEventType  pending_event;
    gint64        position;
    gint64        position_time;
    gint64        size;        // property controlled

    // Seek helper fields
    gboolean      is_seekable; // property controlled
    gboolean      is_random_access; // property controlled
    gboolean      update;
    gboolean      discont;

    guint         mode; // property controlled and/or internally
    gboolean      stop_on_pause; // property controlled
    gchar*        location; // property controlled
    gchar*        mimetype; // property controlled
    gdouble       rate;
};

struct _JavaSourceClass
{
    GstElementClass parent;

    guint           signals[LAST_SIGNAL];
};

/***********************************************************************************
 * Substitution for
 * GST_BOILERPLATE(JavaSource, java_source, GstElement, GST_TYPE_ELEMENT);
 ***********************************************************************************/
static void java_source_base_init     (gpointer         g_class);
static void java_source_class_init    (JavaSourceClass *g_class);
static void java_source_init          (JavaSource      *object,
                                       JavaSourceClass *g_class);
static GstElementClass *parent_class = NULL;

static void java_source_class_init_trampoline (gpointer g_class, gpointer data)
{
    parent_class = (GstElementClass*)g_type_class_peek_parent (g_class);
    java_source_class_init ((JavaSourceClass*)g_class);
}

GType java_source_get_type (void)
{
    static volatile gsize gonce_data = 0;
// INLINE - g_once_init_enter()
    if (g_once_init_enter (&gonce_data))
    {
        GType _type;
        _type = gst_type_register_static_full (GST_TYPE_ELEMENT,
               g_intern_static_string ("JavaSource"),
               sizeof (JavaSourceClass),
               java_source_base_init,
               NULL,
               java_source_class_init_trampoline,
               NULL,
               NULL,
               sizeof (JavaSource),
               0,
               (GInstanceInitFunc) java_source_init,
               NULL,
               (GTypeFlags) 0);
        g_once_init_leave (&gonce_data, (gsize) _type);
    }
    return (GType) gonce_data;
}

/***********************************************************************************
* Init stuff
***********************************************************************************/
static GstStaticPadTemplate source_template = GST_STATIC_PAD_TEMPLATE ("src",
    GST_PAD_SRC, GST_PAD_ALWAYS, GST_STATIC_CAPS_ANY);

static void java_source_base_init (gpointer g_class)
{
    GstElementClass *element_class = GST_ELEMENT_CLASS (g_class);

    gst_element_class_set_details_simple (element_class,
        "Java Source",
        "Source",
        "Java based source element",
        "Oracle Corporation");

    gst_element_class_add_pad_template (element_class,
        gst_static_pad_template_get (&source_template));
}

/***********************************************************************************
* Instance init and forward declarations
***********************************************************************************/
static void java_source_set_property (GObject *object, guint prop_id,
                                      const GValue *value, GParamSpec *spec);
static void java_source_get_property (GObject *object, guint prop_id,
                                      GValue *value, GParamSpec *spec);
static void                 java_source_finalize (GObject *object);
static GstStateChangeReturn java_source_change_state (GstElement *element,
    GstStateChange transition);

static gboolean         java_source_activatepush(GstPad *pad, gboolean active);
static gboolean         java_source_event(GstPad *pad, GstEvent *event);
static gboolean         java_source_checkgetrange(GstPad *pad);
static GstFlowReturn    java_source_getrange(GstPad *pad, guint64 offset,
    guint length, GstBuffer **data);
static void             java_source_loop(void *data);

static const GstQueryType* java_source_query_type(GstPad * pad);
static gboolean            java_source_query (GstPad *pad, GstQuery *query);

static void java_source_class_init (JavaSourceClass *klass)
{
    GObjectClass *gobject_klass = G_OBJECT_CLASS (klass);
    gobject_klass->finalize = GST_DEBUG_FUNCPTR(java_source_finalize);
    gobject_klass->set_property = java_source_set_property;
    gobject_klass->get_property = java_source_get_property;

    GST_ELEMENT_CLASS (klass)->change_state = GST_DEBUG_FUNCPTR(java_source_change_state);

    g_object_class_install_property (gobject_klass, PROP_SIZE,
        g_param_spec_int64 ("size", "Stream size", "stream size", -1, G_MAXINT64, -1,
        G_PARAM_WRITABLE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));

    g_object_class_install_property (gobject_klass, PROP_IS_SEEKABLE,
        g_param_spec_boolean ("is-seekable", "Is seekable", "Is the source seekable", FALSE,
        G_PARAM_WRITABLE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));

    g_object_class_install_property (gobject_klass, PROP_IS_RANDOM_ACCESS,
        g_param_spec_boolean ("is-random-access", "Is random access", "Random access source", FALSE,
        G_PARAM_WRITABLE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));

    g_object_class_install_property (gobject_klass, PROP_STOP_ON_PAUSE,
        g_param_spec_boolean ("stop-on-pause", "Stop on pause", "Stop pushing buffers after switching PLAYING to PAUSED", TRUE,
        G_PARAM_WRITABLE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));

    g_object_class_install_property (gobject_klass, PROP_HLS_MODE,
        g_param_spec_boolean ("hls-mode", "HLS Mode", "HTTP Live Streaming Mode", FALSE,
        G_PARAM_WRITABLE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));

    g_object_class_install_property (gobject_klass, PROP_LOCATION,
        g_param_spec_string ("location", "Source Location", "Location of the source to read", NULL,
        G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS | GST_PARAM_MUTABLE_READY));

    g_object_class_install_property (gobject_klass, PROP_MIMETYPE,
        g_param_spec_string ("mimetype", "Source Mimetype", "Mimetype of the source", NULL,
        G_PARAM_WRITABLE | G_PARAM_STATIC_STRINGS | GST_PARAM_MUTABLE_READY));

    klass->signals[SIGNAL_SEEK_DATA] = g_signal_new ("seek-data",
        G_TYPE_FROM_CLASS (klass),
        G_SIGNAL_RUN_LAST | G_SIGNAL_NO_RECURSE | G_SIGNAL_NO_HOOKS,
        0,
        NULL /* accumulator */,
        NULL /* accu_data */,
        source_marshal_INT64__INT64,
        G_TYPE_INT64 /* return_type */,
        1,     /* n_params */
        G_TYPE_INT64);

    klass->signals[SIGNAL_READ_NEXT_BLOCK] = g_signal_new ("read-next-block",
        G_TYPE_FROM_CLASS (klass),
        G_SIGNAL_RUN_LAST | G_SIGNAL_NO_RECURSE | G_SIGNAL_NO_HOOKS,
        0,
        NULL, /* accumulator */
        NULL, /* accu_data */
        source_marshal_INT__VOID,
        G_TYPE_INT, /* return_type */
        0     /* n_params */ );

    klass->signals[SIGNAL_READ_BLOCK] = g_signal_new ("read-block",
        G_TYPE_FROM_CLASS (klass),
        G_SIGNAL_RUN_LAST | G_SIGNAL_NO_RECURSE | G_SIGNAL_NO_HOOKS,
        0,
        NULL, /* accumulator */
        NULL, /* accu_data */
        source_marshal_INT__UINT64_UINT,
        G_TYPE_INT, /* return_type */
        2     /* n_params */,
        G_TYPE_UINT64, G_TYPE_UINT);

    klass->signals[SIGNAL_COPY_BLOCK] = g_signal_new ("copy-block",
        G_TYPE_FROM_CLASS (klass),
        G_SIGNAL_RUN_LAST | G_SIGNAL_NO_RECURSE | G_SIGNAL_NO_HOOKS,
        0,
        NULL, /* accumulator */
        NULL, /* accu_data */
        source_marshal_VOID__POINTER_INT,
        G_TYPE_NONE, /* return_type */
        2,     /* n_params */
        G_TYPE_POINTER, G_TYPE_INT);

    klass->signals[SIGNAL_CLOSE_CONNECTION] = g_signal_new ("close-connection",
        G_TYPE_FROM_CLASS (klass),
        G_SIGNAL_RUN_LAST | G_SIGNAL_NO_RECURSE | G_SIGNAL_NO_HOOKS,
        0,
        NULL, /* accumulator */
        NULL, /* accu_data */
        g_cclosure_marshal_VOID__VOID,
        G_TYPE_NONE, /* return_type */
        0     /* n_params */ );

    klass->signals[SIGNAL_PROPERTY] = g_signal_new ("property",
        G_TYPE_FROM_CLASS (klass),
        G_SIGNAL_RUN_LAST | G_SIGNAL_NO_RECURSE | G_SIGNAL_NO_HOOKS,
        0,
        NULL, /* accumulator */
        NULL, /* accu_data */
        source_marshal_INT__INT_INT,
        G_TYPE_INT, /* return_type */
        2,    /* n_params */
        G_TYPE_INT, G_TYPE_INT);

    klass->signals[SIGNAL_GET_STREAM_SIZE] = g_signal_new ("get-stream-size",
        G_TYPE_FROM_CLASS (klass),
        G_SIGNAL_RUN_LAST | G_SIGNAL_NO_RECURSE | G_SIGNAL_NO_HOOKS,
        0,
        NULL, /* accumulator */
        NULL, /* accu_data */
        source_marshal_INT__VOID,
        G_TYPE_INT, /* return_type */
        0    /* n_params */ );
}

static void java_source_init(JavaSource *element, JavaSourceClass *element_klass)
{
    GstElementClass *klass = GST_ELEMENT_CLASS (element_klass);

    element->srcpad = gst_pad_new_from_template (gst_element_class_get_pad_template (klass, "src"), "src");
    gst_pad_set_activatepush_function  (element->srcpad,
        GST_DEBUG_FUNCPTR(java_source_activatepush));
    gst_pad_set_checkgetrange_function (element->srcpad,
        GST_DEBUG_FUNCPTR(java_source_checkgetrange));
    gst_pad_set_event_function         (element->srcpad,
        GST_DEBUG_FUNCPTR(java_source_event));
    gst_pad_set_getrange_function      (element->srcpad,
        GST_DEBUG_FUNCPTR(java_source_getrange));
    gst_pad_set_query_type_function    (element->srcpad,
        GST_DEBUG_FUNCPTR(java_source_query_type));
    gst_pad_set_query_function         (element->srcpad,
        GST_DEBUG_FUNCPTR(java_source_query));
    gst_element_add_pad (GST_ELEMENT (element), element->srcpad);

    element->lock = g_mutex_new();

    element->mode = MODE_DEFAULT;

    element->rate = 1.0; // Default to 1.0

    element->mimetype = NULL;
}

/***********************************************************************************
* GObject overrides
***********************************************************************************/
static void java_source_set_property (GObject *object, guint prop_id,
    const GValue *value, GParamSpec *spec)
{
    JavaSource *element = JAVA_SOURCE(object);
    switch (prop_id)
    {
    case PROP_SIZE:
        element->size = g_value_get_int64 (value);
        break;
    case PROP_IS_SEEKABLE:
        element->is_seekable = g_value_get_boolean (value);
        break;
    case PROP_IS_RANDOM_ACCESS:
        element->is_random_access = g_value_get_boolean (value);
        break;
    case PROP_STOP_ON_PAUSE:
        element->stop_on_pause = g_value_get_boolean (value);
        break;
    case PROP_LOCATION:
        element->location = g_strdup(g_value_get_string (value));
        break;
    case PROP_HLS_MODE:
        if (g_value_get_boolean (value))
            element->mode = MODE_HLS;
        else
            element->mode = MODE_DEFAULT;
        break;
    case PROP_MIMETYPE:
        element->mimetype = g_strdup(g_value_get_string (value));
        // Set caps to mimetype if provided
        if (element->mimetype)
        {
            GstCaps *caps = NULL;
            caps = gst_caps_new_simple (element->mimetype, NULL);
            gst_pad_set_caps(element->srcpad, caps);
            gst_caps_unref(caps);
        }
        break;
    default:
        break;
    }
}

static void java_source_get_property (GObject *object, guint prop_id,
                                      GValue *value, GParamSpec *spec)
{
    JavaSource *element = JAVA_SOURCE(object);
    switch (prop_id) {
        case PROP_LOCATION:
            g_value_set_string (value, element->location);
            break;
        default:
            break;
    }
}

static void java_source_finalize (GObject *object)
{
    JavaSource *element = JAVA_SOURCE(object);
    g_mutex_free(element->lock);
    g_free(element->location);
    if (element->mimetype)
        g_free(element->mimetype);
    G_OBJECT_CLASS (parent_class)->finalize (object);
}

/***********************************************************************************
* activate_push handler. Called when the pipeline switches to or from push mode,
* depending on the 'active' flag.
* If we activate the element in the push mode we should start a task on its source
* pad and begin pushing buffers down the pipeline.
***********************************************************************************/
static gboolean java_source_activatepush(GstPad *pad, gboolean active)
{
    JavaSource *element = JAVA_SOURCE(GST_PAD_PARENT(pad));

    if (active)
    {
        g_mutex_lock(element->lock);
        element->srcresult = GST_FLOW_OK;
        g_mutex_unlock(element->lock);

        if (gst_pad_is_linked(pad))
            return gst_pad_start_task(pad, java_source_loop, element);
        else
            return TRUE;
    }
    else
    {
        g_mutex_lock(element->lock);
        element->srcresult = GST_FLOW_WRONG_STATE;
        g_mutex_unlock(element->lock);

        return gst_pad_stop_task(pad);
    }
}

/***********************************************************************************
* Seek implementation
***********************************************************************************/
static gboolean java_source_perform_seek(JavaSource *element, GstPad *pad, GstEvent *event)
{
    gboolean     result = FALSE;
    gboolean     hls_eos = FALSE;
    gdouble      rate;
    GstFormat    seek_format;
    GstSeekFlags flags;
    GstSeekType  start_type, stop_type;
    gint64       start, stop, position, new_position;

    gst_event_parse_seek(event, &rate, &seek_format, &flags,
        &start_type, &start, &stop_type, &stop);

    if (GST_FORMAT_BYTES != seek_format && (element->mode & MODE_DEFAULT) == MODE_DEFAULT)
    {
        gst_element_message_full(GST_ELEMENT(element), GST_MESSAGE_WARNING, GST_CORE_ERROR, GST_CORE_ERROR_SEEK, g_strdup("GST_FORMAT_BYTES seek request is expected."), NULL, ("javasource.c"), ("java_source_perform_seek"), 0);
        return FALSE;
    }
    else if (GST_FORMAT_TIME != seek_format && (element->mode & MODE_HLS) == MODE_HLS)
    {
        return FALSE;
    }

    if (flags & GST_SEEK_FLAG_FLUSH)
        gst_pad_push_event(pad, gst_event_new_flush_start());

    g_mutex_lock(element->lock);
    element->srcresult = GST_FLOW_WRONG_STATE;
    g_mutex_unlock(element->lock);

    if ((element->mode & MODE_HLS_LIVE) != MODE_HLS_LIVE)
        GST_PAD_STREAM_LOCK(pad);

    if ((element->mode & MODE_HLS) == MODE_HLS)
        position = start/GST_SECOND;
    else
        position = start;

    g_signal_emit(element, JAVA_SOURCE_GET_CLASS(element)->signals[SIGNAL_SEEK_DATA], 0, position, &new_position);

    if ((element->mode & MODE_HLS_LIVE) == MODE_HLS_LIVE)
        GST_PAD_STREAM_LOCK(pad);

    if (new_position >= 0)
    {
        if (hls_eos)
        {
            element->pending_event = GST_EVENT_EOS;
        }
        else
        {
            element->rate = rate;
            element->pending_event = GST_EVENT_NEWSEGMENT;
        }
        if ((element->mode & MODE_HLS) == MODE_HLS)
        {
            element->position = 0;
            element->position_time = (new_position * GST_SECOND) / HLS_VALUE_FLOAT_MULTIPLIER;
        }
        else
        {
            element->position = position;
            element->position_time = 0;
        }
        element->discont = TRUE;
        element->update = FALSE;
        result = TRUE;
    }

    g_mutex_lock(element->lock);
    element->srcresult = GST_FLOW_OK;
    g_mutex_unlock(element->lock);

    if (flags & GST_SEEK_FLAG_FLUSH)
        gst_pad_push_event(pad, gst_event_new_flush_stop());

    gst_pad_start_task(pad, java_source_loop, element);

    GST_PAD_STREAM_UNLOCK(pad);

// INLINE - gst_event_unref()
    gst_event_unref(event);
    return result;
}

static gboolean java_source_event(GstPad *pad, GstEvent *event)
{
    JavaSource *element = JAVA_SOURCE(GST_PAD_PARENT(pad));
    switch (GST_EVENT_TYPE (event))
    {
    case GST_EVENT_SEEK:
        if (element->is_seekable)
            return java_source_perform_seek(element, pad, event);
        break;

    default:
        //                g_log("\033[01;36msrc-event\033[00m", G_LOG_LEVEL_MESSAGE, "type=%s", GST_EVENT_TYPE_NAME(event));
        break;
    }

    return gst_pad_event_default(pad, event);
}

/***********************************************************************************
* source pad loop
***********************************************************************************/
static void java_source_loop(void *user_data)
{
    JavaSource   *element = JAVA_SOURCE(user_data);
    GstFlowReturn result;

    g_mutex_lock(element->lock);
    result = element->srcresult;
    g_mutex_unlock(element->lock);

    if (result == GST_FLOW_OK)
    {
next_event:
        switch (element->pending_event)
        {
        case GST_EVENT_NEWSEGMENT:
            {
                GstEvent *new_segment = NULL;
                if ((element->mode & MODE_HLS) == MODE_HLS)
                {
                    gint result = 0;
                    gboolean wrong_state = FALSE;
                    g_signal_emit(element, JAVA_SOURCE_GET_CLASS(element)->signals[SIGNAL_GET_STREAM_SIZE], 0, &result);

                    g_mutex_lock(element->lock);
                    wrong_state = (element->srcresult == GST_FLOW_WRONG_STATE);
                    g_mutex_unlock(element->lock);

                    if (wrong_state)
                        break; 

                    if (result == -1)
                    {
                        element->pending_event = GST_EVENT_EOS;
                        goto next_event;
                    }
                    else if (result < 0)
                    {
                        result = -1 * result;
                        element->discont = TRUE;
                    }
                    new_segment = gst_event_new_new_segment(element->update, element->rate, GST_FORMAT_BYTES, 0, result, element->position_time);
                }
                else
                {
                    new_segment = gst_event_new_new_segment(element->update, element->rate, GST_FORMAT_BYTES, element->position, element->size, element->position);
                }
                result = gst_pad_push_event (element->srcpad, new_segment) ? GST_FLOW_OK : GST_FLOW_WRONG_STATE;
                element->pending_event = GST_EVENT_UNKNOWN;
                break;
            }

        case GST_EVENT_EOS:
            gst_pad_push_event (element->srcpad, gst_event_new_eos());
            result = GST_FLOW_UNEXPECTED;
            break;

        case GST_EVENT_UNKNOWN: // Pushing buffers
            {
                gint     size;
                g_signal_emit(element, JAVA_SOURCE_GET_CLASS(element)->signals[SIGNAL_READ_NEXT_BLOCK], 0, &size);
                if (size > 0)
                {
                    GstBuffer *buffer = NULL;
                    if (gst_pad_alloc_buffer(element->srcpad, element->position, size, GST_PAD_CAPS(element->srcpad), &buffer) == GST_FLOW_OK)
                    {
                        g_signal_emit(element, JAVA_SOURCE_GET_CLASS(element)->signals[SIGNAL_COPY_BLOCK], 0, GST_BUFFER_DATA(buffer), size);

                        if (element->discont)
                        {
                            buffer = gst_buffer_make_metadata_writable (buffer);
                            GST_BUFFER_FLAG_SET (buffer, GST_BUFFER_FLAG_DISCONT);
                            element->discont = FALSE;
                        }

                        result = gst_pad_push(element->srcpad, buffer);

                        if (element->pending_event != GST_EVENT_NEWSEGMENT)
                            element->position += size;
                    }
                }
                else if ((element->mode & MODE_DEFAULT) == MODE_DEFAULT && size == EOS_CODE) // EOS
                {
                    element->pending_event = GST_EVENT_EOS;
                    goto next_event;
                }
                else if ((element->mode & MODE_HLS) == MODE_HLS && size == EOS_CODE) // Request more data
                {
                    element->pending_event = GST_EVENT_NEWSEGMENT;
                    goto next_event;
                }
                else if (size == OTHER_ERROR_CODE) // Other error
                    result = GST_FLOW_WRONG_STATE;
                break;
            }

        default:
            break;
        }
    }

    g_mutex_lock(element->lock);

    if (GST_FLOW_OK == element->srcresult || GST_FLOW_OK != result)
        element->srcresult = result;
    else
        result = element->srcresult;
    g_mutex_unlock(element->lock);

    if (result != GST_FLOW_OK)
        gst_pad_pause_task(element->srcpad);
}

/***********************************************************************************
* query stuff
***********************************************************************************/
static const GstQueryType* java_source_query_type(GstPad * pad)
{
    static const GstQueryType javasource_query_types[] = {
        GST_QUERY_DURATION,
        GST_QUERY_NONE
    };

    return javasource_query_types;
}

static gboolean java_source_query (GstPad *pad, GstQuery *query)
{
    gboolean result = TRUE;
    JavaSource *element = JAVA_SOURCE (GST_OBJECT_PARENT (pad));

    switch (GST_QUERY_TYPE(query))
    {
    case GST_QUERY_DURATION:
        {
            GstFormat format;

            gst_query_parse_duration(query, &format, NULL);

            if ((element->mode & MODE_HLS) == MODE_HLS)
            {
                gint duration = 0;

                // duration in time only
                if (format != GST_FORMAT_TIME)
                {
                    result = FALSE;
                    break;
                }

                g_signal_emit(element, JAVA_SOURCE_GET_CLASS(element)->signals[SIGNAL_PROPERTY], 0, HLS_PROP_GET_DURATION, 0, &duration);
                if (duration < 0)
                    element->mode |= MODE_HLS_LIVE;
                gst_query_set_duration(query, GST_FORMAT_TIME, ((gint64)duration*GST_SECOND)/HLS_VALUE_FLOAT_MULTIPLIER);
            }
            else
            {
                // duration in bytes only
                if (format != GST_FORMAT_BYTES)
                {
                    result = FALSE;
                    break;
                }
                gst_query_set_duration(query, GST_FORMAT_BYTES, element->size);
            }
            break;
        }

    default:
        result = gst_pad_query_default(pad, query);
        break;
    }
    return result;
}

/***********************************************************************************
* get_range stuff
***********************************************************************************/
static gboolean java_source_checkgetrange(GstPad *pad)
{
    JavaSource *element = JAVA_SOURCE (GST_OBJECT_PARENT (pad));
    return element->is_random_access;
}

static GstFlowReturn java_source_getrange(GstPad *pad, guint64 offset,
    guint length, GstBuffer **buffer)
{
    JavaSource *element = JAVA_SOURCE (GST_OBJECT_PARENT (pad));
    gint     size = 0;
    GstFlowReturn ret = GST_FLOW_ERROR;

    g_signal_emit(element, JAVA_SOURCE_GET_CLASS(element)->signals[SIGNAL_READ_BLOCK], 0, offset, length, &size);
    if (size > 0 || size <= length)
    {
        ret = gst_pad_alloc_buffer(element->srcpad, offset, size, GST_PAD_CAPS(element->srcpad), buffer);
        if (ret == GST_FLOW_OK)
        {
            g_signal_emit(element, JAVA_SOURCE_GET_CLASS(element)->signals[SIGNAL_COPY_BLOCK], 0, GST_BUFFER_DATA(*buffer), GST_BUFFER_SIZE(*buffer));
        }
    }
    else if (size == EOS_CODE)
    {
        ret = GST_FLOW_UNEXPECTED; // EOS
    }

    return ret;
}
/***********************************************************************************
* State change handler
***********************************************************************************/
static GstStateChangeReturn java_source_change_state (GstElement *e,
    GstStateChange transition)
{
    JavaSource          *element = JAVA_SOURCE(e);
    GstStateChangeReturn ret;

    switch (transition)
    {
    case GST_STATE_CHANGE_READY_TO_PAUSED:
        {
            GST_PAD_STREAM_LOCK(element->srcpad);
            element->pending_event = GST_EVENT_NEWSEGMENT;
            element->position = 0;
            element->position_time = 0;
            element->discont = FALSE;
            if ((element->mode & MODE_HLS) == MODE_HLS)
                element->update = FALSE;
            else
                element->update = TRUE;
            GST_PAD_STREAM_UNLOCK(element->srcpad);

            g_mutex_lock(element->lock);
            element->srcresult = GST_FLOW_OK;
            g_mutex_unlock(element->lock);
        }
        break;

    case GST_STATE_CHANGE_PAUSED_TO_PLAYING:
        g_mutex_lock(element->lock);
        if (element->stop_on_pause)
            element->srcresult = GST_FLOW_OK;
        g_mutex_unlock(element->lock);
        break;

    default:
        break;
    }

    ret = GST_ELEMENT_CLASS (parent_class)->change_state (e, transition);

    if (ret == GST_STATE_CHANGE_FAILURE)
        return ret;

    switch (transition)
    {
    case GST_STATE_CHANGE_PLAYING_TO_PAUSED:
        g_mutex_lock(element->lock);
        if (element->stop_on_pause)
            element->srcresult = GST_FLOW_WRONG_STATE;
        g_mutex_unlock(element->lock);
        break;

    case GST_STATE_CHANGE_READY_TO_NULL:
        g_mutex_lock(element->lock);
        if (!element->stop_on_pause)
            element->srcresult = GST_FLOW_WRONG_STATE;
        element->size = -1;
        g_signal_emit(element, JAVA_SOURCE_GET_CLASS(element)->signals[SIGNAL_CLOSE_CONNECTION], 0);
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

gboolean java_source_plugin_init (GstPlugin *plugin)
{
    GST_DEBUG_CATEGORY_INIT (java_source_debug, JAVA_SOURCE_PLUGIN_NAME,
        0, "JFX Java Source Plugin");

    return gst_element_register (plugin, JAVA_SOURCE_PLUGIN_NAME,
        GST_RANK_NONE,
        JAVA_SOURCE_TYPE);
}
