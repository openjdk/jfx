/* GStreamer
 * Copyright (C) 2003 Benjamin Otte <in7y118@public.uni-hamburg.de>
 *
 * gsttypefindelement.c: element that detects type of stream
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
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */
/**
 * SECTION:element-typefind
 *
 * Determines the media-type of a stream. It applies typefind functions in the
 * order of their rank. One the type has been deteted it sets its src pad caps
 * to the found media type.
 *
 * Whenever a type is found the #GstTypeFindElement::have-type signal is
 * emitted, either from the streaming thread or the application thread
 * (the latter may happen when typefinding is done pull-based from the
 * state change function).
 *
 * Plugins can register custom typefinders by using #GstTypeFindFactory.
 */

/* FIXME: need a better solution for non-seekable streams */

/* way of operation:
 * 1) get a list of all typefind functions sorted best to worst
 * 2) if all elements have been called with all requested data goto 8
 * 3) call all functions once with all available data
 * 4) if a function returns a value >= PROP_MAXIMUM goto 8
 * 5) all functions with a result > PROP_MINIMUM or functions that did not get
 *    all requested data (where peek returned NULL) stay in list
 * 6) seek to requested offset of best function that still has open data
 *    requests
 * 7) goto 2
 * 8) take best available result and use its caps
 *
 * The element has two scheduling modes:
 *
 * 1) chain based, it will collect buffers and run the typefind function on
 *    the buffer until something is found.
 * 2) getrange based, it will proxy the getrange function to the sinkpad. It
 *    is assumed that the peer element is happy with whatever format we
 *    eventually read.
 *
 * By default it tries to do pull based typefinding (this avoids joining
 * received buffers and holding them back in store.)
 *
 * When the element has no connected srcpad, and the sinkpad can operate in
 * getrange based mode, the element starts its own task to figure out the
 * type of the stream.
 *
 * Most of the actual implementation is in libs/gst/base/gsttypefindhelper.c.
 */

#ifdef HAVE_CONFIG_H
#  include "config.h"
#endif

#include "gst/gst_private.h"

#include "gsttypefindelement.h"
#include "gst/gst-i18n-lib.h"
#include "gst/base/gsttypefindhelper.h"

#include <gst/gsttypefind.h>
#include <gst/gstutils.h>
#include <gst/gsterror.h>

GST_DEBUG_CATEGORY_STATIC (gst_type_find_element_debug);
#define GST_CAT_DEFAULT gst_type_find_element_debug

/* generic templates */
static GstStaticPadTemplate type_find_element_sink_template =
GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS_ANY);

static GstStaticPadTemplate type_find_element_src_template =
GST_STATIC_PAD_TEMPLATE ("src",
    GST_PAD_SRC,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS_ANY);

/* Require at least 2kB of data before we attempt typefinding in chain-mode.
 * 128kB is massive overkill for the maximum, but doesn't do any harm */
#define TYPE_FIND_MIN_SIZE   (2*1024)
#define TYPE_FIND_MAX_SIZE (128*1024)

/* TypeFind signals and args */
enum
{
  HAVE_TYPE,
  LAST_SIGNAL
};
enum
{
  PROP_0,
  PROP_CAPS,
  PROP_MINIMUM,
  PROP_MAXIMUM,
  PROP_FORCE_CAPS,
  PROP_LAST
};
enum
{
  MODE_NORMAL,                  /* act as identity */
  MODE_TYPEFIND,                /* do typefinding  */
  MODE_ERROR                    /* had fatal error */
};


#define _do_init(bla) \
    GST_DEBUG_CATEGORY_INIT (gst_type_find_element_debug, "typefind",           \
        GST_DEBUG_BG_YELLOW | GST_DEBUG_FG_GREEN, "type finding element");

GST_BOILERPLATE_FULL (GstTypeFindElement, gst_type_find_element, GstElement,
    GST_TYPE_ELEMENT, _do_init);

static void gst_type_find_element_dispose (GObject * object);
static void gst_type_find_element_set_property (GObject * object,
    guint prop_id, const GValue * value, GParamSpec * pspec);
static void gst_type_find_element_get_property (GObject * object,
    guint prop_id, GValue * value, GParamSpec * pspec);

#if 0
static const GstEventMask *gst_type_find_element_src_event_mask (GstPad * pad);
#endif

static gboolean gst_type_find_element_src_event (GstPad * pad,
    GstEvent * event);
static gboolean gst_type_find_handle_src_query (GstPad * pad, GstQuery * query);

static gboolean gst_type_find_element_handle_event (GstPad * pad,
    GstEvent * event);
static gboolean gst_type_find_element_setcaps (GstPad * pad, GstCaps * caps);
static GstFlowReturn gst_type_find_element_chain (GstPad * sinkpad,
    GstBuffer * buffer);
static GstFlowReturn gst_type_find_element_getrange (GstPad * srcpad,
    guint64 offset, guint length, GstBuffer ** buffer);
static gboolean gst_type_find_element_checkgetrange (GstPad * srcpad);

static GstStateChangeReturn
gst_type_find_element_change_state (GstElement * element,
    GstStateChange transition);
static gboolean gst_type_find_element_activate (GstPad * pad);
static gboolean
gst_type_find_element_activate_src_pull (GstPad * pad, gboolean active);
static GstFlowReturn
gst_type_find_element_chain_do_typefinding (GstTypeFindElement * typefind);
static void
gst_type_find_element_send_cached_events (GstTypeFindElement * typefind);

static guint gst_type_find_element_signals[LAST_SIGNAL] = { 0 };

static void
gst_type_find_element_have_type (GstTypeFindElement * typefind,
    guint probability, const GstCaps * caps)
{
  GstCaps *copy;

  g_assert (caps != NULL);

  GST_INFO_OBJECT (typefind, "found caps %" GST_PTR_FORMAT ", probability=%u",
      caps, probability);

  GST_OBJECT_LOCK (typefind);
  if (typefind->caps)
    gst_caps_unref (typefind->caps);
  typefind->caps = gst_caps_copy (caps);
  copy = gst_caps_ref (typefind->caps);
  GST_OBJECT_UNLOCK (typefind);

  gst_pad_set_caps (typefind->src, copy);
  gst_caps_unref (copy);
}

static void
gst_type_find_element_base_init (gpointer g_class)
{
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (g_class);

  gst_element_class_set_details_simple (gstelement_class,
      "TypeFind",
      "Generic",
      "Finds the media type of a stream",
      "Benjamin Otte <in7y118@public.uni-hamburg.de>");
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&type_find_element_src_template));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&type_find_element_sink_template));
}

static void
gst_type_find_element_class_init (GstTypeFindElementClass * typefind_class)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (typefind_class);
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (typefind_class);

  gobject_class->set_property = gst_type_find_element_set_property;
  gobject_class->get_property = gst_type_find_element_get_property;
  gobject_class->dispose = gst_type_find_element_dispose;

  g_object_class_install_property (gobject_class, PROP_CAPS,
      g_param_spec_boxed ("caps", _("caps"),
          _("detected capabilities in stream"), gst_caps_get_type (),
          G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_MINIMUM,
      g_param_spec_uint ("minimum", _("minimum"),
          "minimum probability required to accept caps", GST_TYPE_FIND_MINIMUM,
          GST_TYPE_FIND_MAXIMUM, GST_TYPE_FIND_MINIMUM,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_MAXIMUM,
      g_param_spec_uint ("maximum", _("maximum"),
          "probability to stop typefinding (deprecated; non-functional)",
          GST_TYPE_FIND_MINIMUM, GST_TYPE_FIND_MAXIMUM, GST_TYPE_FIND_MAXIMUM,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_FORCE_CAPS,
      g_param_spec_boxed ("force-caps", _("force caps"),
          _("force caps without doing a typefind"), gst_caps_get_type (),
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  /**
   * GstTypeFindElement::have-type:
   * @typefind: the typefind instance
   * @probability: the probability of the type found
   * @caps: the caps of the type found
   *
   * This signal gets emitted when the type and its probability has
   * been found.
   */
  gst_type_find_element_signals[HAVE_TYPE] = g_signal_new ("have-type",
      G_TYPE_FROM_CLASS (typefind_class), G_SIGNAL_RUN_FIRST,
      G_STRUCT_OFFSET (GstTypeFindElementClass, have_type), NULL, NULL,
      gst_marshal_VOID__UINT_BOXED, G_TYPE_NONE, 2,
      G_TYPE_UINT, GST_TYPE_CAPS | G_SIGNAL_TYPE_STATIC_SCOPE);

  typefind_class->have_type =
      GST_DEBUG_FUNCPTR (gst_type_find_element_have_type);

  gstelement_class->change_state =
      GST_DEBUG_FUNCPTR (gst_type_find_element_change_state);
}

static void
gst_type_find_element_init (GstTypeFindElement * typefind,
    GstTypeFindElementClass * g_class)
{
  /* sinkpad */
  typefind->sink =
      gst_pad_new_from_static_template (&type_find_element_sink_template,
      "sink");

  gst_pad_set_activate_function (typefind->sink,
      GST_DEBUG_FUNCPTR (gst_type_find_element_activate));
  gst_pad_set_setcaps_function (typefind->sink,
      GST_DEBUG_FUNCPTR (gst_type_find_element_setcaps));
  gst_pad_set_chain_function (typefind->sink,
      GST_DEBUG_FUNCPTR (gst_type_find_element_chain));
  gst_pad_set_event_function (typefind->sink,
      GST_DEBUG_FUNCPTR (gst_type_find_element_handle_event));
  gst_element_add_pad (GST_ELEMENT (typefind), typefind->sink);

  /* srcpad */
  typefind->src =
      gst_pad_new_from_static_template (&type_find_element_src_template, "src");

  gst_pad_set_activatepull_function (typefind->src,
      GST_DEBUG_FUNCPTR (gst_type_find_element_activate_src_pull));
  gst_pad_set_checkgetrange_function (typefind->src,
      GST_DEBUG_FUNCPTR (gst_type_find_element_checkgetrange));
  gst_pad_set_getrange_function (typefind->src,
      GST_DEBUG_FUNCPTR (gst_type_find_element_getrange));
  gst_pad_set_event_function (typefind->src,
      GST_DEBUG_FUNCPTR (gst_type_find_element_src_event));
  gst_pad_set_query_function (typefind->src,
      GST_DEBUG_FUNCPTR (gst_type_find_handle_src_query));
  gst_pad_use_fixed_caps (typefind->src);
  gst_element_add_pad (GST_ELEMENT (typefind), typefind->src);

  typefind->mode = MODE_TYPEFIND;
  typefind->caps = NULL;
  typefind->min_probability = 1;
  typefind->max_probability = GST_TYPE_FIND_MAXIMUM;

  typefind->store = NULL;
}

static void
gst_type_find_element_dispose (GObject * object)
{
  GstTypeFindElement *typefind = GST_TYPE_FIND_ELEMENT (object);

  if (typefind->store) {
    gst_buffer_unref (typefind->store);
    typefind->store = NULL;
  }

  if (typefind->force_caps) {
    gst_caps_unref (typefind->force_caps);
    typefind->force_caps = NULL;
  }

  G_OBJECT_CLASS (parent_class)->dispose (object);
}

static void
gst_type_find_element_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstTypeFindElement *typefind;

  typefind = GST_TYPE_FIND_ELEMENT (object);

  switch (prop_id) {
    case PROP_MINIMUM:
      typefind->min_probability = g_value_get_uint (value);
      break;
    case PROP_MAXIMUM:
      typefind->max_probability = g_value_get_uint (value);
      break;
    case PROP_FORCE_CAPS:
      GST_OBJECT_LOCK (typefind);
      if (typefind->force_caps)
        gst_caps_unref (typefind->force_caps);
      typefind->force_caps = g_value_dup_boxed (value);
      GST_OBJECT_UNLOCK (typefind);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_type_find_element_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  GstTypeFindElement *typefind;

  typefind = GST_TYPE_FIND_ELEMENT (object);

  switch (prop_id) {
    case PROP_CAPS:
      GST_OBJECT_LOCK (typefind);
      g_value_set_boxed (value, typefind->caps);
      GST_OBJECT_UNLOCK (typefind);
      break;
    case PROP_MINIMUM:
      g_value_set_uint (value, typefind->min_probability);
      break;
    case PROP_MAXIMUM:
      g_value_set_uint (value, typefind->max_probability);
      break;
    case PROP_FORCE_CAPS:
      GST_OBJECT_LOCK (typefind);
      g_value_set_boxed (value, typefind->force_caps);
      GST_OBJECT_UNLOCK (typefind);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static gboolean
gst_type_find_handle_src_query (GstPad * pad, GstQuery * query)
{
  GstTypeFindElement *typefind;
  gboolean res = FALSE;
  GstPad *peer;

  typefind = GST_TYPE_FIND_ELEMENT (GST_PAD_PARENT (pad));

  peer = gst_pad_get_peer (typefind->sink);
  if (peer == NULL)
    return FALSE;

  res = gst_pad_query (peer, query);
  if (!res)
    goto out;

  switch (GST_QUERY_TYPE (query)) {
    case GST_QUERY_POSITION:
    {
      gint64 peer_pos;
      GstFormat format;

      GST_OBJECT_LOCK (typefind);
      if (typefind->store == NULL) {
        GST_OBJECT_UNLOCK (typefind);
        goto out;
      }

      gst_query_parse_position (query, &format, &peer_pos);

      /* FIXME: this code assumes that there's no discont in the queue */
      switch (format) {
        case GST_FORMAT_BYTES:
          peer_pos -= GST_BUFFER_SIZE (typefind->store);
          break;
        default:
          /* FIXME */
          break;
      }
      GST_OBJECT_UNLOCK (typefind);
      gst_query_set_position (query, format, peer_pos);
      break;
    }
    default:
      break;
  }

out:
  gst_object_unref (peer);
  return res;
}

#if 0
static const GstEventMask *
gst_type_find_element_src_event_mask (GstPad * pad)
{
  static const GstEventMask mask[] = {
    {GST_EVENT_SEEK,
        GST_SEEK_METHOD_SET | GST_SEEK_METHOD_CUR | GST_SEEK_METHOD_END |
          GST_SEEK_FLAG_FLUSH},
    /* add more if you want, event masks suck and need to die anyway */
    {0,}
  };

  return mask;
}
#endif

static gboolean
gst_type_find_element_src_event (GstPad * pad, GstEvent * event)
{
  GstTypeFindElement *typefind = GST_TYPE_FIND_ELEMENT (GST_PAD_PARENT (pad));

  if (typefind->mode != MODE_NORMAL) {
    /* need to do more? */
    gst_mini_object_unref (GST_MINI_OBJECT (event));
    return FALSE;
  }
  return gst_pad_push_event (typefind->sink, event);
}

static void
start_typefinding (GstTypeFindElement * typefind)
{
  GST_DEBUG_OBJECT (typefind, "starting typefinding");
  gst_pad_set_caps (typefind->src, NULL);

  GST_OBJECT_LOCK (typefind);
  if (typefind->caps)
    gst_caps_replace (&typefind->caps, NULL);
  GST_OBJECT_UNLOCK (typefind);

  typefind->mode = MODE_TYPEFIND;
}

static void
stop_typefinding (GstTypeFindElement * typefind)
{
  GstState state;
  gboolean push_cached_buffers;

  gst_element_get_state (GST_ELEMENT (typefind), &state, NULL, 0);

  push_cached_buffers = (state >= GST_STATE_PAUSED);

  GST_DEBUG_OBJECT (typefind, "stopping typefinding%s",
      push_cached_buffers ? " and pushing cached buffers" : "");

  GST_OBJECT_LOCK (typefind);
  if (typefind->store) {
    GstBuffer *store;

    store = gst_buffer_make_metadata_writable (typefind->store);
    typefind->store = NULL;
    gst_buffer_set_caps (store, typefind->caps);
    GST_OBJECT_UNLOCK (typefind);

    if (!push_cached_buffers) {
      gst_buffer_unref (store);
    } else {
      GstPad *peer = gst_pad_get_peer (typefind->src);

      typefind->mode = MODE_NORMAL;

      /* make sure the user gets a meaningful error message in this case,
       * which is not a core bug or bug of any kind (as the default error
       * message emitted by gstpad.c otherwise would make you think) */
      if (peer && GST_PAD_CHAINFUNC (peer) == NULL) {
        GST_DEBUG_OBJECT (typefind, "upstream only supports push mode, while "
            "downstream element only works in pull mode, erroring out");
        GST_ELEMENT_ERROR (typefind, STREAM, FAILED,
            ("%s cannot work in push mode. The operation is not supported "
                "with this source element or protocol.",
                G_OBJECT_TYPE_NAME (GST_PAD_PARENT (peer))),
            ("Downstream pad %s:%s has no chainfunction, and the upstream "
                "element does not support pull mode",
                GST_DEBUG_PAD_NAME (peer)));
        typefind->mode = MODE_ERROR;    /* make the chain function error out */
        gst_buffer_unref (store);
      } else {
        gst_type_find_element_send_cached_events (typefind);
        gst_pad_push (typefind->src, store);
      }

      if (peer)
        gst_object_unref (peer);
    }
  } else {
    GST_OBJECT_UNLOCK (typefind);
  }
}

static gboolean
gst_type_find_element_handle_event (GstPad * pad, GstEvent * event)
{
  gboolean res = FALSE;
  GstTypeFindElement *typefind = GST_TYPE_FIND_ELEMENT (GST_PAD_PARENT (pad));

  GST_DEBUG_OBJECT (typefind, "got %s event in mode %d",
      GST_EVENT_TYPE_NAME (event), typefind->mode);

  switch (typefind->mode) {
    case MODE_TYPEFIND:
      switch (GST_EVENT_TYPE (event)) {
        case GST_EVENT_EOS:{
          GstTypeFindProbability prob = 0;
          GstCaps *caps = NULL;

          GST_INFO_OBJECT (typefind, "Got EOS and no type found yet");

          /* we might not have started typefinding yet because there was not
           * enough data so far; just give it a shot now and see what we get */
          GST_OBJECT_LOCK (typefind);
          if (typefind->store) {
            caps = gst_type_find_helper_for_buffer (GST_OBJECT (typefind),
                typefind->store, &prob);
            GST_OBJECT_UNLOCK (typefind);

            if (caps && prob >= typefind->min_probability) {
              g_signal_emit (typefind, gst_type_find_element_signals[HAVE_TYPE],
                  0, prob, caps);
            } else {
              GST_ELEMENT_ERROR (typefind, STREAM, TYPE_NOT_FOUND,
                  (NULL), (NULL));
            }
            gst_caps_replace (&caps, NULL);
          } else {
            GST_OBJECT_UNLOCK (typefind);
            /* keep message in sync with the one in the pad activate function */
            GST_ELEMENT_ERROR (typefind, STREAM, TYPE_NOT_FOUND,
                (_("Stream contains no data.")),
                ("Can't typefind empty stream"));
          }

          stop_typefinding (typefind);
          res = gst_pad_push_event (typefind->src, event);
          break;
        }
        case GST_EVENT_FLUSH_STOP:
          GST_OBJECT_LOCK (typefind);
          g_list_foreach (typefind->cached_events,
              (GFunc) gst_mini_object_unref, NULL);
          g_list_free (typefind->cached_events);
          typefind->cached_events = NULL;
          gst_buffer_replace (&typefind->store, NULL);
          GST_OBJECT_UNLOCK (typefind);
          /* fall through */
        case GST_EVENT_FLUSH_START:
          res = gst_pad_push_event (typefind->src, event);
          break;
        default:
          GST_DEBUG_OBJECT (typefind, "Saving %s event to send later",
              GST_EVENT_TYPE_NAME (event));
          GST_OBJECT_LOCK (typefind);
          typefind->cached_events =
              g_list_append (typefind->cached_events, event);
          GST_OBJECT_UNLOCK (typefind);
          res = TRUE;
          break;
      }
      break;
    case MODE_NORMAL:
      res = gst_pad_push_event (typefind->src, event);
      break;
    case MODE_ERROR:
      break;
    default:
      g_assert_not_reached ();
  }
  return res;
}

static void
gst_type_find_element_send_cached_events (GstTypeFindElement * typefind)
{
  GList *l, *cached_events;

  GST_OBJECT_LOCK (typefind);
  cached_events = typefind->cached_events;
  typefind->cached_events = NULL;
  GST_OBJECT_UNLOCK (typefind);

  for (l = cached_events; l != NULL; l = l->next) {
    GstEvent *event = GST_EVENT (l->data);

    GST_DEBUG_OBJECT (typefind, "sending cached %s event",
        GST_EVENT_TYPE_NAME (event));
    gst_pad_push_event (typefind->src, event);
  }
  g_list_free (cached_events);
}

static gboolean
gst_type_find_element_setcaps (GstPad * pad, GstCaps * caps)
{
  GstTypeFindElement *typefind;

  typefind = GST_TYPE_FIND_ELEMENT (GST_PAD_PARENT (pad));

  /* don't operate on ANY caps */
  if (gst_caps_is_any (caps))
    return TRUE;

  g_signal_emit (typefind, gst_type_find_element_signals[HAVE_TYPE], 0,
      GST_TYPE_FIND_MAXIMUM, caps);

  /* Shortcircuit typefinding if we get caps */
  if (typefind->mode == MODE_TYPEFIND) {
    GST_DEBUG_OBJECT (typefind, "Skipping typefinding, using caps from "
        "upstream buffer: %" GST_PTR_FORMAT, caps);
    typefind->mode = MODE_NORMAL;

    gst_type_find_element_send_cached_events (typefind);
    GST_OBJECT_LOCK (typefind);
    if (typefind->store) {
      GstBuffer *store;

      store = gst_buffer_make_metadata_writable (typefind->store);
      typefind->store = NULL;
      gst_buffer_set_caps (store, typefind->caps);
      GST_OBJECT_UNLOCK (typefind);

      GST_DEBUG_OBJECT (typefind, "Pushing store: %d", GST_BUFFER_SIZE (store));
      gst_pad_push (typefind->src, store);
    } else {
      GST_OBJECT_UNLOCK (typefind);
    }
  }

  return TRUE;
}

static gchar *
gst_type_find_get_extension (GstTypeFindElement * typefind, GstPad * pad)
{
  GstQuery *query;
  gchar *uri, *result;
  size_t len;
  gint find;

  query = gst_query_new_uri ();

  /* try getting the caps with an uri query and from the extension */
  if (!gst_pad_peer_query (pad, query))
    goto peer_query_failed;

  gst_query_parse_uri (query, &uri);
  if (uri == NULL)
    goto no_uri;

  GST_DEBUG_OBJECT (typefind, "finding extension of %s", uri);

  /* find the extension on the uri, this is everything after a '.' */
  len = strlen (uri);
  find = len - 1;

  while (find >= 0) {
    if (uri[find] == '.')
      break;
    find--;
  }
  if (find < 0)
    goto no_extension;

  result = g_strdup (&uri[find + 1]);

  GST_DEBUG_OBJECT (typefind, "found extension %s", result);
  gst_query_unref (query);
  g_free (uri);

  return result;

  /* ERRORS */
peer_query_failed:
  {
    GST_WARNING_OBJECT (typefind, "failed to query peer uri");
    gst_query_unref (query);
    return NULL;
  }
no_uri:
  {
    GST_WARNING_OBJECT (typefind, "could not parse the peer uri");
    gst_query_unref (query);
    return NULL;
  }
no_extension:
  {
    GST_WARNING_OBJECT (typefind, "could not find uri extension in %s", uri);
    gst_query_unref (query);
    g_free (uri);
    return NULL;
  }
}

static GstCaps *
gst_type_find_guess_by_extension (GstTypeFindElement * typefind, GstPad * pad,
    GstTypeFindProbability * probability)
{
  gchar *ext;
  GstCaps *caps;

  ext = gst_type_find_get_extension (typefind, pad);
  if (!ext)
    return NULL;

  caps = gst_type_find_helper_for_extension (GST_OBJECT_CAST (typefind), ext);
  if (caps)
    *probability = GST_TYPE_FIND_MAXIMUM;

  g_free (ext);

  return caps;
}

static GstFlowReturn
gst_type_find_element_chain (GstPad * pad, GstBuffer * buffer)
{
  GstTypeFindElement *typefind;
  GstFlowReturn res = GST_FLOW_OK;

  typefind = GST_TYPE_FIND_ELEMENT (GST_PAD_PARENT (pad));

  GST_LOG_OBJECT (typefind, "handling buffer in mode %d", typefind->mode);

  switch (typefind->mode) {
    case MODE_ERROR:
      /* we should already have called GST_ELEMENT_ERROR */
      return GST_FLOW_ERROR;
    case MODE_NORMAL:
      /* don't take object lock as typefind->caps should not change anymore */
      buffer = gst_buffer_make_metadata_writable (buffer);
      gst_buffer_set_caps (buffer, typefind->caps);
      return gst_pad_push (typefind->src, buffer);
    case MODE_TYPEFIND:{
      GST_OBJECT_LOCK (typefind);
      if (typefind->store)
        typefind->store = gst_buffer_join (typefind->store, buffer);
      else
        typefind->store = buffer;
      GST_OBJECT_UNLOCK (typefind);

      res = gst_type_find_element_chain_do_typefinding (typefind);

      if (typefind->mode == MODE_ERROR)
        res = GST_FLOW_ERROR;

      break;
    }
    default:
      g_assert_not_reached ();
      return GST_FLOW_ERROR;
  }

  return res;
}

static GstFlowReturn
gst_type_find_element_chain_do_typefinding (GstTypeFindElement * typefind)
{
  GstTypeFindProbability probability;
  GstCaps *caps;

  GST_OBJECT_LOCK (typefind);
  if (GST_BUFFER_SIZE (typefind->store) < TYPE_FIND_MIN_SIZE) {
    GST_DEBUG_OBJECT (typefind, "not enough data for typefinding yet "
        "(%u bytes)", GST_BUFFER_SIZE (typefind->store));
    GST_OBJECT_UNLOCK (typefind);
    return GST_FLOW_OK;
  }

  caps = gst_type_find_helper_for_buffer (GST_OBJECT (typefind),
      typefind->store, &probability);
  if (caps == NULL && GST_BUFFER_SIZE (typefind->store) > TYPE_FIND_MAX_SIZE) {
    GST_OBJECT_UNLOCK (typefind);
    GST_ELEMENT_ERROR (typefind, STREAM, TYPE_NOT_FOUND, (NULL), (NULL));
    stop_typefinding (typefind);
    return GST_FLOW_ERROR;
  } else if (caps == NULL) {
    GST_OBJECT_UNLOCK (typefind);
    GST_DEBUG_OBJECT (typefind, "no caps found with %u bytes of data, "
        "waiting for more data", GST_BUFFER_SIZE (typefind->store));
    return GST_FLOW_OK;
  }

  /* found a type */
  if (probability < typefind->min_probability) {
    GST_DEBUG_OBJECT (typefind, "found caps %" GST_PTR_FORMAT ", but "
        "probability is %u which is lower than the required minimum of %u",
        caps, probability, typefind->min_probability);

    gst_caps_replace (&caps, NULL);

    if (GST_BUFFER_SIZE (typefind->store) >= TYPE_FIND_MAX_SIZE) {
      GST_OBJECT_UNLOCK (typefind);
      GST_ELEMENT_ERROR (typefind, STREAM, TYPE_NOT_FOUND, (NULL), (NULL));
      stop_typefinding (typefind);
      return GST_FLOW_ERROR;
    }

    GST_OBJECT_UNLOCK (typefind);
    GST_DEBUG_OBJECT (typefind, "waiting for more data to try again");
    return GST_FLOW_OK;
  }
  GST_OBJECT_UNLOCK (typefind);

  /* probability is good enough too, so let's make it known ... */
  g_signal_emit (typefind, gst_type_find_element_signals[HAVE_TYPE], 0,
      probability, caps);

  /* .. and send out the accumulated data */
  stop_typefinding (typefind);
  gst_caps_unref (caps);
  return GST_FLOW_OK;
}

static gboolean
gst_type_find_element_checkgetrange (GstPad * srcpad)
{
  GstTypeFindElement *typefind;

  typefind = GST_TYPE_FIND_ELEMENT (GST_PAD_PARENT (srcpad));

  return gst_pad_check_pull_range (typefind->sink);
}

static GstFlowReturn
gst_type_find_element_getrange (GstPad * srcpad,
    guint64 offset, guint length, GstBuffer ** buffer)
{
  GstTypeFindElement *typefind;
  GstFlowReturn ret;

  typefind = GST_TYPE_FIND_ELEMENT (GST_PAD_PARENT (srcpad));

  ret = gst_pad_pull_range (typefind->sink, offset, length, buffer);

  if (ret == GST_FLOW_OK && buffer && *buffer) {
    /* don't take object lock as typefind->caps should not change anymore */
    /* we assume that pulled buffers are meta-data writable */
    gst_buffer_set_caps (*buffer, typefind->caps);
  }

  return ret;
}

static gboolean
gst_type_find_element_activate_src_pull (GstPad * pad, gboolean active)
{
  GstTypeFindElement *typefind;

  typefind = GST_TYPE_FIND_ELEMENT (GST_OBJECT_PARENT (pad));

  return gst_pad_activate_pull (typefind->sink, active);
}

static gboolean
gst_type_find_element_activate (GstPad * pad)
{
  GstTypeFindProbability probability = 0;
  GstCaps *found_caps = NULL;
  GstTypeFindElement *typefind;

  typefind = GST_TYPE_FIND_ELEMENT (GST_OBJECT_PARENT (pad));

  /* if we have force caps, use those */
  GST_OBJECT_LOCK (typefind);
  if (typefind->force_caps) {
    found_caps = gst_caps_ref (typefind->force_caps);
    probability = GST_TYPE_FIND_MAXIMUM;
    GST_OBJECT_UNLOCK (typefind);
    goto done;
  }
  GST_OBJECT_UNLOCK (typefind);

  /* 1. try to activate in pull mode. if not, switch to push and succeed.
     2. try to pull type find.
     3. deactivate pull mode.
     4. src pad might have been activated push by the state change. deactivate.
     5. if we didn't find any caps, try getting the uri extension by doing an uri
     query.
     6. if we didn't find any caps, fail.
     7. emit have-type; maybe the app connected the source pad to something.
     8. if the sink pad is activated, we are in pull mode. succeed.
     otherwise activate both pads in push mode and succeed.
   */

  /* 1 */
  if (!gst_pad_check_pull_range (pad) || !gst_pad_activate_pull (pad, TRUE)) {
    start_typefinding (typefind);
    return gst_pad_activate_push (pad, TRUE);
  }

  GST_DEBUG_OBJECT (typefind, "find type in pull mode");

  /* 2 */
  {
    GstPad *peer;

    peer = gst_pad_get_peer (pad);
    if (peer) {
      gint64 size;
      GstFormat format = GST_FORMAT_BYTES;
      gchar *ext;

      if (!gst_pad_query_duration (peer, &format, &size)) {
        GST_WARNING_OBJECT (typefind, "Could not query upstream length!");
        gst_object_unref (peer);
        gst_pad_activate_pull (pad, FALSE);
        return FALSE;
      }

      /* the size if 0, we cannot continue */
      if (size == 0) {
        /* keep message in sync with message in sink event handler */
        GST_ELEMENT_ERROR (typefind, STREAM, TYPE_NOT_FOUND,
            (_("Stream contains no data.")), ("Can't typefind empty stream"));
        gst_object_unref (peer);
        gst_pad_activate_pull (pad, FALSE);
        return FALSE;
      }
      ext = gst_type_find_get_extension (typefind, pad);

      found_caps = gst_type_find_helper_get_range_ext (GST_OBJECT_CAST (peer),
          (GstTypeFindHelperGetRangeFunction) (GST_PAD_GETRANGEFUNC (peer)),
          (guint64) size, ext, &probability);
      g_free (ext);

      gst_object_unref (peer);
    }
  }

  /* the type find helpers might have triggered setcaps here (due to upstream)
   * setting caps on buffers, which emits typefound signal and an element
   * could have been linked and have its pads activated
   *
   * If we deactivate the pads in the following steps we might mess up
   * downstream element. We should prevent that.
   */
  if (typefind->mode == MODE_NORMAL) {
    /* this means we already emitted typefound */
    GST_DEBUG ("Already managed to typefind !");
    goto really_done;
  }

  /* 3 */
  gst_pad_activate_pull (pad, FALSE);

  /* 4 */
  gst_pad_activate_push (typefind->src, FALSE);

  /* 5 */
  if (!found_caps || probability < typefind->min_probability) {
    found_caps = gst_type_find_guess_by_extension (typefind, pad, &probability);
  }

  /* 6 */
  if (!found_caps || probability < typefind->min_probability) {
    GST_ELEMENT_ERROR (typefind, STREAM, TYPE_NOT_FOUND, (NULL), (NULL));
    gst_caps_replace (&found_caps, NULL);
    return FALSE;
  }

done:
  /* 7 */
  g_signal_emit (typefind, gst_type_find_element_signals[HAVE_TYPE],
      0, probability, found_caps);
  typefind->mode = MODE_NORMAL;
really_done:
  gst_caps_unref (found_caps);

  /* 8 */
  if (gst_pad_is_active (pad))
    return TRUE;
  else {
    gboolean ret;

    ret = gst_pad_activate_push (typefind->src, TRUE);
    ret &= gst_pad_activate_push (pad, TRUE);
    return ret;
  }
}

static GstStateChangeReturn
gst_type_find_element_change_state (GstElement * element,
    GstStateChange transition)
{
  GstStateChangeReturn ret;
  GstTypeFindElement *typefind;

  typefind = GST_TYPE_FIND_ELEMENT (element);


  ret = GST_ELEMENT_CLASS (parent_class)->change_state (element, transition);

  switch (transition) {
    case GST_STATE_CHANGE_PAUSED_TO_READY:
    case GST_STATE_CHANGE_READY_TO_NULL:
      GST_OBJECT_LOCK (typefind);
      gst_caps_replace (&typefind->caps, NULL);

      g_list_foreach (typefind->cached_events,
          (GFunc) gst_mini_object_unref, NULL);
      g_list_free (typefind->cached_events);
      typefind->cached_events = NULL;
      typefind->mode = MODE_TYPEFIND;
      GST_OBJECT_UNLOCK (typefind);
      break;
    default:
      break;
  }

  return ret;
}
