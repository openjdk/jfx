/* GStreamer
 * Copyright (C) 2013 Stefan Sauer <ensonic@users.sf.net>
 *
 * gsttracerutils.h: tracing subsystem
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


#ifndef __GST_TRACER_UTILS_H__
#define __GST_TRACER_UTILS_H__

#include <glib.h>
#include <glib-object.h>
#include <gst/gstconfig.h>
#include <gst/gstbin.h>
#include <gst/gstutils.h>

G_BEGIN_DECLS

#ifndef GST_DISABLE_GST_TRACER_HOOKS

/* tracing hooks */

void _priv_gst_tracing_init (void);
void _priv_gst_tracing_deinit (void);

/* tracer quarks */

/* These enums need to match the number and order
 * of strings declared in _quark_table, in gsttracerutils.c */
typedef enum /*< skip >*/
{
  GST_TRACER_QUARK_HOOK_PAD_PUSH_PRE = 0,
  GST_TRACER_QUARK_HOOK_PAD_PUSH_POST,
  GST_TRACER_QUARK_HOOK_PAD_PUSH_LIST_PRE,
  GST_TRACER_QUARK_HOOK_PAD_PUSH_LIST_POST,
  GST_TRACER_QUARK_HOOK_PAD_PULL_RANGE_PRE,
  GST_TRACER_QUARK_HOOK_PAD_PULL_RANGE_POST,
  GST_TRACER_QUARK_HOOK_PAD_PUSH_EVENT_PRE ,
  GST_TRACER_QUARK_HOOK_PAD_PUSH_EVENT_POST,
  GST_TRACER_QUARK_HOOK_PAD_QUERY_PRE ,
  GST_TRACER_QUARK_HOOK_PAD_QUERY_POST,
  GST_TRACER_QUARK_HOOK_ELEMENT_POST_MESSAGE_PRE,
  GST_TRACER_QUARK_HOOK_ELEMENT_POST_MESSAGE_POST,
  GST_TRACER_QUARK_HOOK_ELEMENT_QUERY_PRE,
  GST_TRACER_QUARK_HOOK_ELEMENT_QUERY_POST,
  GST_TRACER_QUARK_HOOK_ELEMENT_NEW,
  GST_TRACER_QUARK_HOOK_ELEMENT_ADD_PAD,
  GST_TRACER_QUARK_HOOK_ELEMENT_REMOVE_PAD,
  GST_TRACER_QUARK_HOOK_BIN_ADD_PRE,
  GST_TRACER_QUARK_HOOK_BIN_ADD_POST,
  GST_TRACER_QUARK_HOOK_BIN_REMOVE_PRE,
  GST_TRACER_QUARK_HOOK_BIN_REMOVE_POST,
  GST_TRACER_QUARK_HOOK_PAD_LINK_PRE,
  GST_TRACER_QUARK_HOOK_PAD_LINK_POST,
  GST_TRACER_QUARK_HOOK_PAD_UNLINK_PRE,
  GST_TRACER_QUARK_HOOK_PAD_UNLINK_POST,
  GST_TRACER_QUARK_HOOK_ELEMENT_CHANGE_STATE_PRE,
  GST_TRACER_QUARK_HOOK_ELEMENT_CHANGE_STATE_POST,
  GST_TRACER_QUARK_HOOK_MINI_OBJECT_CREATED,
  GST_TRACER_QUARK_HOOK_MINI_OBJECT_DESTROYED,
  GST_TRACER_QUARK_HOOK_OBJECT_CREATED,
  GST_TRACER_QUARK_HOOK_OBJECT_DESTROYED,
  GST_TRACER_QUARK_HOOK_MINI_OBJECT_REFFED,
  GST_TRACER_QUARK_HOOK_MINI_OBJECT_UNREFFED,
  GST_TRACER_QUARK_HOOK_OBJECT_REFFED,
  GST_TRACER_QUARK_HOOK_OBJECT_UNREFFED,
  GST_TRACER_QUARK_MAX
} GstTracerQuarkId;

extern GQuark _priv_gst_tracer_quark_table[GST_TRACER_QUARK_MAX];

#define GST_TRACER_QUARK(q) _priv_gst_tracer_quark_table[GST_TRACER_QUARK_##q]

/* tracing module helpers */

typedef struct {
  GObject *tracer;
  GCallback func;
} GstTracerHook;

extern gboolean _priv_tracer_enabled;
/* key are hook-id quarks, values are GstTracerHook */
extern GHashTable *_priv_tracers;

#define GST_TRACER_IS_ENABLED (_priv_tracer_enabled)

#define GST_TRACER_TS \
  GST_CLOCK_DIFF (_priv_gst_start_time, gst_util_get_timestamp ())

/* tracing hooks */

#define GST_TRACER_ARGS h->tracer, ts
#define GST_TRACER_DISPATCH(key,type,args) G_STMT_START{ \
  if (GST_TRACER_IS_ENABLED) {                                         \
    GstClockTime ts = GST_TRACER_TS;                                   \
    GList *__l, *__n;                                                  \
    GstTracerHook *h;                                                  \
    __l = g_hash_table_lookup (_priv_tracers, GINT_TO_POINTER (key));  \
    for (__n = __l; __n; __n = g_list_next (__n)) {                    \
      h = (GstTracerHook *) __n->data;                                 \
      ((type)(h->func)) args;                                          \
    }                                                                  \
    __l = g_hash_table_lookup (_priv_tracers, NULL);                   \
    for (__n = __l; __n; __n = g_list_next (__n)) {                    \
      h = (GstTracerHook *) __n->data;                                 \
      ((type)(h->func)) args;                                          \
    }                                                                  \
  }                                                                    \
}G_STMT_END

/**
 * GstTracerHookPadPushPre:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @pad: the pad
 * @buffer: the buffer
 *
 * Pre-hook for gst_pad_push() named "pad-push-pre".
 */
typedef void (*GstTracerHookPadPushPre) (GObject *self, GstClockTime ts,
    GstPad *pad, GstBuffer *buffer);
#define GST_TRACER_PAD_PUSH_PRE(pad, buffer) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_PAD_PUSH_PRE), \
    GstTracerHookPadPushPre, (GST_TRACER_ARGS, pad, buffer)); \
}G_STMT_END

/**
 * GstTracerHookPadPushPost:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @pad: the pad
 * @res: the result of gst_pad_push()
 *
 * Post-hook for gst_pad_push() named "pad-push-post".
 */
typedef void (*GstTracerHookPadPushPost) (GObject * self, GstClockTime ts,
    GstPad *pad, GstFlowReturn res);
#define GST_TRACER_PAD_PUSH_POST(pad, res) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_PAD_PUSH_POST), \
    GstTracerHookPadPushPost, (GST_TRACER_ARGS, pad, res)); \
}G_STMT_END

/**
 * GstTracerHookPadPushListPre:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @pad: the pad
 * @list: the buffer-list
 *
 * Pre-hook for gst_pad_push_list() named "pad-push-list-pre".
 */
typedef void (*GstTracerHookPadPushListPre) (GObject *self, GstClockTime ts,
    GstPad *pad, GstBufferList *list);
#define GST_TRACER_PAD_PUSH_LIST_PRE(pad, list) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_PAD_PUSH_LIST_PRE), \
    GstTracerHookPadPushListPre, (GST_TRACER_ARGS, pad, list)); \
}G_STMT_END

/**
 * GstTracerHookPadPushListPost:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @pad: the pad
 * @res: the result of gst_pad_push_list()
 *
 * Post-hook for gst_pad_push_list() named "pad-push-list-post".
 */
typedef void (*GstTracerHookPadPushListPost) (GObject *self, GstClockTime ts,
    GstPad *pad,
    GstFlowReturn res);
#define GST_TRACER_PAD_PUSH_LIST_POST(pad, res) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_PAD_PUSH_LIST_POST), \
    GstTracerHookPadPushListPost, (GST_TRACER_ARGS, pad, res)); \
}G_STMT_END

/**
 * GstTracerHookPadPullRangePre:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @pad: the pad
 * @offset: the stream offset
 * @size: the requested size
 *
 * Pre-hook for gst_pad_pull_range() named "pad-pull-range-pre".
 */
typedef void (*GstTracerHookPadPullRangePre) (GObject *self, GstClockTime ts,
    GstPad *pad, guint64 offset, guint size);
#define GST_TRACER_PAD_PULL_RANGE_PRE(pad, offset, size) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_PAD_PULL_RANGE_PRE), \
    GstTracerHookPadPullRangePre, (GST_TRACER_ARGS, pad, offset, size)); \
}G_STMT_END

/**
 * GstTracerHookPadPullRangePost:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @pad: the pad
 * @buffer: the buffer
 * @res: the result of gst_pad_pull_range()
 *
 * Post-hook for gst_pad_pull_range() named "pad-pull-range-post".
 */
typedef void (*GstTracerHookPadPullRangePost) (GObject *self, GstClockTime ts,
    GstPad *pad, GstBuffer *buffer, GstFlowReturn res);
#define GST_TRACER_PAD_PULL_RANGE_POST(pad, buffer, res) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_PAD_PULL_RANGE_POST), \
    GstTracerHookPadPullRangePost, (GST_TRACER_ARGS, pad, buffer, res)); \
}G_STMT_END

/**
 * GstTracerHookPadPushEventPre:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @pad: the pad
 * @event: the event
 *
 * Pre-hook for gst_pad_push_event() named "pad-push-event-pre".
 */
typedef void (*GstTracerHookPadPushEventPre) (GObject *self, GstClockTime ts,
    GstPad *pad, GstEvent *event);
#define GST_TRACER_PAD_PUSH_EVENT_PRE(pad, event) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_PAD_PUSH_EVENT_PRE), \
    GstTracerHookPadPushEventPre, (GST_TRACER_ARGS, pad, event)); \
}G_STMT_END

/**
 * GstTracerHookPadPushEventPost:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @pad: the pad
 * @res: the result of gst_pad_push_event()
 *
 * Post-hook for gst_pad_push_event() named "pad-push-event-post".
 */
typedef void (*GstTracerHookPadPushEventPost) (GObject *self, GstClockTime ts,
    GstPad *pad, gboolean res);
#define GST_TRACER_PAD_PUSH_EVENT_POST(pad, res) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_PAD_PUSH_EVENT_POST), \
    GstTracerHookPadPushEventPost, (GST_TRACER_ARGS, pad, res)); \
}G_STMT_END

/**
 * GstTracerHookPadQueryPre:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @pad: the pad
 * @query: the query
 *
 * Pre-hook for gst_pad_query() named "pad-query-pre".
 */
typedef void (*GstTracerHookPadQueryPre) (GObject *self, GstClockTime ts,
    GstPad *pad, GstQuery *query);
#define GST_TRACER_PAD_QUERY_PRE(pad, query) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_PAD_QUERY_PRE), \
    GstTracerHookPadQueryPre, (GST_TRACER_ARGS, pad, query)); \
}G_STMT_END

/**
 * GstTracerHookPadQueryPost:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @pad: the pad
 * @query: the query
 * @res: the result of gst_pad_query()
 *
 * Post-hook for gst_pad_query() named "pad-query-post".
 */
typedef void (*GstTracerHookPadQueryPost) (GObject *self, GstClockTime ts,
    GstPad *pad, GstQuery *query, gboolean res);
#define GST_TRACER_PAD_QUERY_POST(pad, query, res) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_PAD_QUERY_POST), \
    GstTracerHookPadQueryPost, (GST_TRACER_ARGS, pad, query, res)); \
}G_STMT_END

/**
 * GstTracerHookElementPostMessagePre:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @element: the element
 * @message: the message
 *
 * Pre-hook for gst_element_post_message() named "element-post-message-pre".
 */
typedef void (*GstTracerHookElementPostMessagePre) (GObject *self,
    GstClockTime ts, GstElement *element, GstMessage *message);
#define GST_TRACER_ELEMENT_POST_MESSAGE_PRE(element, message) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_ELEMENT_POST_MESSAGE_PRE), \
    GstTracerHookElementPostMessagePre, (GST_TRACER_ARGS, element, message)); \
}G_STMT_END

/**
 * GstTracerHookElementPostMessagePost:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @element: the element
 * @res: the result of gst_element_post_message()
 *
 * Pre-hook for gst_element_post_message() named "element-post-message-post".
 */
typedef void (*GstTracerHookElementPostMessagePost) (GObject *self,
    GstClockTime ts, GstElement *element, gboolean res);
#define GST_TRACER_ELEMENT_POST_MESSAGE_POST(element, res) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_ELEMENT_POST_MESSAGE_POST), \
    GstTracerHookElementPostMessagePost, (GST_TRACER_ARGS, element, res)); \
}G_STMT_END

/**
 * GstTracerHookElementQueryPre:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @element: the element
 * @query: the query
 *
 * Pre-hook for gst_element_query() named "element-query-pre".
 */
typedef void (*GstTracerHookElementQueryPre) (GObject *self, GstClockTime ts,
    GstElement *element, GstQuery *query);
#define GST_TRACER_ELEMENT_QUERY_PRE(element, query) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_ELEMENT_QUERY_PRE), \
    GstTracerHookElementQueryPre, (GST_TRACER_ARGS, element, query)); \
}G_STMT_END

/**
 * GstTracerHookElementQueryPost:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @element: the element
 * @query: the query
 * @res: the result of gst_element_query()
 *
 * Post-hook for gst_element_query() named "element-query-post".
 */
typedef void (*GstTracerHookElementQueryPost) (GObject *self, GstClockTime ts,
    GstElement *element, GstQuery *query, gboolean res);
#define GST_TRACER_ELEMENT_QUERY_POST(element, query, res) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_ELEMENT_QUERY_POST), \
    GstTracerHookElementQueryPost, (GST_TRACER_ARGS, element, query, res)); \
}G_STMT_END

/**
 * GstTracerHookElementNew:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @element: the element
 *
 * Hook for whenever a new element is created, named "element-new".
 */
typedef void (*GstTracerHookElementNew) (GObject *self, GstClockTime ts,
    GstElement *element);
#define GST_TRACER_ELEMENT_NEW(element) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_ELEMENT_NEW), \
    GstTracerHookElementNew, (GST_TRACER_ARGS, element)); \
}G_STMT_END

/**
 * GstTracerHookElementAddPad:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @element: the element
 * @pad: the pad
 *
 * Hook for gst_element_add_pad() named "element-add-pad".
 */
typedef void (*GstTracerHookElementAddPad) (GObject *self, GstClockTime ts,
    GstElement *element, GstPad *pad);
#define GST_TRACER_ELEMENT_ADD_PAD(element, pad) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_ELEMENT_ADD_PAD), \
    GstTracerHookElementAddPad, (GST_TRACER_ARGS, element, pad)); \
}G_STMT_END

/**
 * GstTracerHookElementRemovePad:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @element: the element
 * @pad: the pad
 *
 * Hook for gst_element_remove_pad() named "element-remove-pad".
 */
typedef void (*GstTracerHookElementRemovePad) (GObject *self, GstClockTime ts,
    GstElement *element, GstPad *pad);
#define GST_TRACER_ELEMENT_REMOVE_PAD(element, pad) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_ELEMENT_REMOVE_PAD), \
    GstTracerHookElementRemovePad, (GST_TRACER_ARGS, element, pad)); \
}G_STMT_END

/**
 * GstTracerHookElementChangeStatePre:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @element: the element
 * @transition: the transition
 *
 * Pre-hook for gst_element_change_state() named "element-change-state-pre".
 */
typedef void (*GstTracerHookElementChangeStatePre) (GObject *self,
    GstClockTime ts, GstElement *element, GstStateChange transition);
#define GST_TRACER_ELEMENT_CHANGE_STATE_PRE(element, transition) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_ELEMENT_CHANGE_STATE_PRE), \
    GstTracerHookElementChangeStatePre, (GST_TRACER_ARGS, element, transition)); \
}G_STMT_END

/**
 * GstTracerHookElementChangeStatePost:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @element: the element
 * @transition: the transition
 * @result: the result of gst_pad_push()
 *
 * Post-hook for gst_element_change_state() named "element-change-state-post".
 */
typedef void (*GstTracerHookElementChangeStatePost) (GObject *self,
    GstClockTime ts, GstElement *element, GstStateChange transition,
    GstStateChangeReturn result);
#define GST_TRACER_ELEMENT_CHANGE_STATE_POST(element, transition, result) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_ELEMENT_CHANGE_STATE_POST), \
    GstTracerHookElementChangeStatePost, (GST_TRACER_ARGS, element, transition, result)); \
}G_STMT_END

/**
 * GstTracerHookBinAddPre:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @bin: the bin
 * @element: the element
 *
 * Pre-hook for gst_bin_add() named "bin-add-pre".
 */
typedef void (*GstTracerHookBinAddPre) (GObject *self, GstClockTime ts,
    GstBin *bin, GstElement *element);
#define GST_TRACER_BIN_ADD_PRE(bin, element) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_BIN_ADD_PRE), \
    GstTracerHookBinAddPre, (GST_TRACER_ARGS, bin, element)); \
}G_STMT_END

/**
 * GstTracerHookBinAddPost:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @bin: the bin
 * @element: the element
 * @result: the result of gst_bin_add()
 *
 * Post-hook for gst_bin_add() named "bin-add-post".
 */
typedef void (*GstTracerHookBinAddPost) (GObject *self, GstClockTime ts,
    GstBin *bin, GstElement *element, gboolean result);
#define GST_TRACER_BIN_ADD_POST(bin, element, result) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_BIN_ADD_POST), \
    GstTracerHookBinAddPost, (GST_TRACER_ARGS, bin, element, result)); \
}G_STMT_END

/**
 * GstTracerHookBinRemovePre:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @bin: the bin
 * @element: the element
 *
 * Pre-hook for gst_bin_remove() named "bin-remove-pre".
 */
typedef void (*GstTracerHookBinRemovePre) (GObject *self, GstClockTime ts,
    GstBin *bin, GstElement *element);
#define GST_TRACER_BIN_REMOVE_PRE(bin, element) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_BIN_REMOVE_PRE), \
    GstTracerHookBinRemovePre, (GST_TRACER_ARGS, bin, element)); \
}G_STMT_END

/**
 * GstTracerHookBinRemovePost:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @bin: the bin
 * @result: the result of gst_bin_remove()
 *
 * Post-hook for gst_bin_remove() named "bin-remove-post".
 */
typedef void (*GstTracerHookBinRemovePost) (GObject *self, GstClockTime ts,
    GstBin *bin, gboolean result);
#define GST_TRACER_BIN_REMOVE_POST(bin, result) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_BIN_REMOVE_POST), \
    GstTracerHookBinRemovePost, (GST_TRACER_ARGS, bin, result)); \
}G_STMT_END

/**
 * GstTracerHookPadLinkPre:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @srcpad: the srcpad
 * @sinkpad: the sinkpad
 *
 * Pre-hook for gst_pad_link() named "pad-link-pre".
 */
typedef void (*GstTracerHookPadLinkPre) (GObject *self, GstClockTime ts,
    GstPad *srcpad, GstPad *sinkpad);
#define GST_TRACER_PAD_LINK_PRE(srcpad, sinkpad) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_PAD_LINK_PRE), \
    GstTracerHookPadLinkPre, (GST_TRACER_ARGS, srcpad, sinkpad)); \
}G_STMT_END

/**
 * GstTracerHookPadLinkPost:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @srcpad: the srcpad
 * @sinkpad: the sinkpad
 * @result: the result of gst_pad_link()
 *
 * Post-hook for gst_pad_link() named "pad-link-post".
 */
typedef void (*GstTracerHookPadLinkPost) (GObject *self, GstClockTime ts,
    GstPad *srcpad, GstPad *sinkpad, GstPadLinkReturn result);
#define GST_TRACER_PAD_LINK_POST(srcpad, sinkpad, result) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_PAD_LINK_POST), \
    GstTracerHookPadLinkPost, (GST_TRACER_ARGS, srcpad, sinkpad, result)); \
}G_STMT_END

/**
 * GstTracerHookPadUnlinkPre:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @srcpad: the srcpad
 * @sinkpad: the sinkpad
 *
 * Pre-hook for gst_pad_unlink() named "pad-unlink-pre".
 */
typedef void (*GstTracerHookPadUnlinkPre) (GObject *self, GstClockTime ts,
    GstPad *srcpad, GstPad *sinkpad);
#define GST_TRACER_PAD_UNLINK_PRE(srcpad, sinkpad) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_PAD_UNLINK_PRE), \
    GstTracerHookPadUnlinkPre, (GST_TRACER_ARGS, srcpad, sinkpad)); \
}G_STMT_END

/**
 * GstTracerHookPadUnlinkPost:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @srcpad: the srcpad
 * @sinkpad: the sinkpad
 * @result: the result of gst_pad_push()
 *
 * Post-hook for gst_pad_unlink() named "pad-unlink-post".
 */
typedef void (*GstTracerHookPadUnlinkPost) (GObject *self, GstClockTime ts,
    GstPad *srcpad, GstPad *sinkpad, gboolean result);
#define GST_TRACER_PAD_UNLINK_POST(srcpad, sinkpad, result) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_PAD_UNLINK_POST), \
    GstTracerHookPadUnlinkPost, (GST_TRACER_ARGS, srcpad, sinkpad, result)); \
}G_STMT_END

/**
 * GstTracerHookMiniObjectCreated:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @object: the mini object being created
 *
 * Hook called when a #GstMiniObject is created named "mini-object-created".
 */
typedef void (*GstTracerHookMiniObjectCreated) (GObject *self, GstClockTime ts,
    GstMiniObject *object);
#define GST_TRACER_MINI_OBJECT_CREATED(object) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_MINI_OBJECT_CREATED), \
    GstTracerHookMiniObjectCreated, (GST_TRACER_ARGS, object)); \
}G_STMT_END

/**
 * GstTracerHookMiniObjectDestroyed:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @object: the mini object being destroyed
 *
 * Hook called when a #GstMiniObject is being destroyed named
 * "mini-object-destroyed".
 */
typedef void (*GstTracerHookMiniObjectDestroyed) (GObject *self, GstClockTime ts,
    GstMiniObject *object);
#define GST_TRACER_MINI_OBJECT_DESTROYED(object) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_MINI_OBJECT_DESTROYED), \
    GstTracerHookMiniObjectDestroyed, (GST_TRACER_ARGS, object)); \
}G_STMT_END

/**
 * GstTracerHookObjectUnreffed:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @object: the object being unreffed
 * @refcount: the new refcount after unrefing @object
 *
 * Hook called when a #GstObject is being unreffed named
 * "object-unreffed"
 */
typedef void (*GstTracerHookObjectUnreffed) (GObject *self, GstClockTime ts,
    GstObject *object, gint new_refcount);
#define GST_TRACER_OBJECT_UNREFFED(object, new_refcount) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_OBJECT_UNREFFED), \
    GstTracerHookObjectUnreffed, (GST_TRACER_ARGS, object, new_refcount)); \
}G_STMT_END

/**
 * GstTracerHookObjectReffed:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @object: the object being reffed
 * @refcount: the new refcount after refing @object
 *
 * Hook called when a #GstObject is being reffed named
 * "object-reffed".
 */
typedef void (*GstTracerHookObjectReffed) (GObject *self, GstClockTime ts,
    GstObject *object, gint new_refcount);
#define GST_TRACER_OBJECT_REFFED(object, new_refcount) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_OBJECT_REFFED), \
    GstTracerHookObjectReffed, (GST_TRACER_ARGS, object, new_refcount)); \
}G_STMT_END

/**
 * GstTracerHookMiniObjectUnreffed:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @object: the mini object being unreffed
 * @refcount: the new refcount after unrefing @object
 *
 * Hook called when a #GstMiniObject is being unreffed named
 * "mini-object-unreffed".
 */
typedef void (*GstTracerHookMiniObjectUnreffed) (GObject *self, GstClockTime ts,
    GstMiniObject *object, gint new_refcount);
#define GST_TRACER_MINI_OBJECT_UNREFFED(object, new_refcount) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_MINI_OBJECT_UNREFFED), \
    GstTracerHookMiniObjectUnreffed, (GST_TRACER_ARGS, object, new_refcount)); \
}G_STMT_END

/**
 * GstTracerHookMiniObjectReffed:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @object: the mini object being reffed
 * @refcount: the new refcount after refing @object
 *
 * Hook called when a #GstMiniObject is being reffed named
 * "mini-object-reffed".
 */
typedef void (*GstTracerHookMiniObjectReffed) (GObject *self, GstClockTime ts,
    GstMiniObject *object, gint new_refcount);
#define GST_TRACER_MINI_OBJECT_REFFED(object, new_refcount) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_MINI_OBJECT_REFFED), \
    GstTracerHookMiniObjectReffed, (GST_TRACER_ARGS, object, new_refcount)); \
}G_STMT_END

/**
 * GstTracerHookObjectCreated:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @object: the object being created
 *
 * Hook called when a #GstObject is created named "object-created".
 */
typedef void (*GstTracerHookObjectCreated) (GObject *self, GstClockTime ts,
    GstObject *object);
#define GST_TRACER_OBJECT_CREATED(object) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_OBJECT_CREATED), \
    GstTracerHookObjectCreated, (GST_TRACER_ARGS, object)); \
}G_STMT_END

/**
 * GstTracerHookObjectDestroyed:
 * @self: the tracer instance
 * @ts: the current timestamp
 * @object: the object being destroyed
 *
 * Hook called when a #GstObject is being destroyed named
 * "object-destroyed".
 */
typedef void (*GstTracerHookObjectDestroyed) (GObject *self, GstClockTime ts,
    GstObject *object);
#define GST_TRACER_OBJECT_DESTROYED(object) G_STMT_START{ \
  GST_TRACER_DISPATCH(GST_TRACER_QUARK(HOOK_OBJECT_DESTROYED), \
    GstTracerHookObjectDestroyed, (GST_TRACER_ARGS, object)); \
}G_STMT_END


#else /* !GST_DISABLE_GST_TRACER_HOOKS */

static inline void
_priv_gst_tracing_init (void)
{
  GST_DEBUG ("Tracing hooks are disabled");
}

static inline void
_priv_gst_tracing_deinit (void)
{
}

#define GST_TRACER_PAD_PUSH_PRE(pad, buffer)
#define GST_TRACER_PAD_PUSH_POST(pad, res)
#define GST_TRACER_PAD_PUSH_LIST_PRE(pad, list)
#define GST_TRACER_PAD_PUSH_LIST_POST(pad, res)
#define GST_TRACER_PAD_PULL_RANGE_PRE(pad, offset, size)
#define GST_TRACER_PAD_PULL_RANGE_POST(pad, buffer, res)
#define GST_TRACER_PAD_PUSH_EVENT_PRE(pad, event)
#define GST_TRACER_PAD_PUSH_EVENT_POST(pad, res)
#define GST_TRACER_PAD_QUERY_PRE(pad, query)
#define GST_TRACER_PAD_QUERY_POST(pad, query, res)
#define GST_TRACER_ELEMENT_POST_MESSAGE_PRE(element, message)
#define GST_TRACER_ELEMENT_POST_MESSAGE_POST(element, res)
#define GST_TRACER_ELEMENT_QUERY_PRE(element, query)
#define GST_TRACER_ELEMENT_QUERY_POST(element, query, res)
#define GST_TRACER_ELEMENT_NEW(element)
#define GST_TRACER_ELEMENT_ADD_PAD(element, pad)
#define GST_TRACER_ELEMENT_REMOVE_PAD(element, pad)
#define GST_TRACER_ELEMENT_CHANGE_STATE_PRE(element, transition)
#define GST_TRACER_ELEMENT_CHANGE_STATE_POST(element, transition, res)
#define GST_TRACER_BIN_ADD_PRE(bin, element)
#define GST_TRACER_BIN_ADD_POST(bin, element, res)
#define GST_TRACER_BIN_REMOVE_PRE(bin, element)
#define GST_TRACER_BIN_REMOVE_POST(bin, res)
#define GST_TRACER_PAD_LINK_PRE(srcpad, sinkpad)
#define GST_TRACER_PAD_LINK_POST(srcpad, sinkpad, res)
#define GST_TRACER_PAD_UNLINK_PRE(srcpad, sinkpad)
#define GST_TRACER_PAD_UNLINK_POST(srcpad, sinkpad, res)
#define GST_TRACER_MINI_OBJECT_CREATED(object)
#define GST_TRACER_MINI_OBJECT_DESTROYED(object)
#define GST_TRACER_MINI_OBJECT_REFFED(object, new_refcount)
#define GST_TRACER_MINI_OBJECT_UNREFFED(object, new_refcount)
#define GST_TRACER_OBJECT_CREATED(object)
#define GST_TRACER_OBJECT_DESTROYED(object)
#define GST_TRACER_OBJECT_REFFED(object, new_refcount)
#define GST_TRACER_OBJECT_UNREFFED(object, new_refcount)

#endif /* GST_DISABLE_GST_TRACER_HOOKS */

G_END_DECLS

#endif /* __GST_TRACER_UTILS_H__ */

