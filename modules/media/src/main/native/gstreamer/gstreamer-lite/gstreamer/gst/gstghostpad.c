/* GStreamer
 * Copyright (C) 1999,2000 Erik Walthinsen <omega@cse.ogi.edu>
 *                    2000 Wim Taymans <wtay@chello.be>
 *                    2005 Andy Wingo <wingo@pobox.com>
 *		      2006 Edward Hervey <bilboed@bilboed.com>
 *
 * gstghostpad.c: Proxy pads
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
 * SECTION:gstghostpad
 * @short_description: Pseudo link pads
 * @see_also: #GstPad
 *
 * GhostPads are useful when organizing pipelines with #GstBin like elements.
 * The idea here is to create hierarchical element graphs. The bin element
 * contains a sub-graph. Now one would like to treat the bin-element like any
 * other #GstElement. This is where GhostPads come into play. A GhostPad acts as
 * a proxy for another pad. Thus the bin can have sink and source ghost-pads
 * that are associated with sink and source pads of the child elements.
 *
 * If the target pad is known at creation time, gst_ghost_pad_new() is the
 * function to use to get a ghost-pad. Otherwise one can use gst_ghost_pad_new_no_target()
 * to create the ghost-pad and use gst_ghost_pad_set_target() to establish the
 * association later on.
 *
 * Note that GhostPads add overhead to the data processing of a pipeline.
 *
 * Last reviewed on 2005-11-18 (0.9.5)
 */

#include "gst_private.h"
#include "gstinfo.h"

#include "gstghostpad.h"
#include "gst.h"

#define GST_CAT_DEFAULT GST_CAT_PADS

#define GST_PROXY_PAD_CAST(obj)         ((GstProxyPad *)obj)
#define GST_PROXY_PAD_PRIVATE(obj)      (GST_PROXY_PAD_CAST (obj)->priv)
#define GST_PROXY_PAD_TARGET(pad)       (GST_PROXY_PAD_PRIVATE (pad)->target)
#define GST_PROXY_PAD_INTERNAL(pad)     (GST_PROXY_PAD_PRIVATE (pad)->internal)
#define GST_PROXY_PAD_RETARGET(pad)     (GST_PROXY_PAD_PRIVATE (pad)->retarget)
#define GST_PROXY_GET_LOCK(pad) (GST_PROXY_PAD_PRIVATE (pad)->proxy_lock)
#define GST_PROXY_LOCK(pad)     (g_mutex_lock (GST_PROXY_GET_LOCK (pad)))
#define GST_PROXY_UNLOCK(pad)   (g_mutex_unlock (GST_PROXY_GET_LOCK (pad)))

struct _GstProxyPadPrivate
{
  /* with PROXY_LOCK */
  GMutex *proxy_lock;
  GstPad *target;
  GstPad *internal;
  gboolean retarget;
};

G_DEFINE_TYPE (GstProxyPad, gst_proxy_pad, GST_TYPE_PAD);

static GstPad *gst_proxy_pad_get_target (GstPad * pad);
static GstPad *gst_proxy_pad_get_internal (GstPad * pad);

static void gst_proxy_pad_dispose (GObject * object);
static void gst_proxy_pad_finalize (GObject * object);

#if !defined(GST_DISABLE_LOADSAVE) && !defined(GST_REMOVE_DEPRECATED)
#ifdef GST_DISABLE_DEPRECATED
#include <libxml/parser.h>
#endif
static xmlNodePtr gst_proxy_pad_save_thyself (GstObject * object,
    xmlNodePtr parent);
#endif

static void on_src_target_notify (GstPad * target,
    GParamSpec * unused, gpointer user_data);

static GParamSpec *pspec_caps = NULL;

static const GstQueryType *
gst_proxy_pad_do_query_type (GstPad * pad)
{
  GstPad *target = gst_proxy_pad_get_target (pad);
  const GstQueryType *res = NULL;

  if (target) {
    res = gst_pad_get_query_types (target);
    gst_object_unref (target);
  }
  return res;
}

static gboolean
gst_proxy_pad_do_event (GstPad * pad, GstEvent * event)
{
  gboolean res = FALSE;
  GstPad *internal = gst_proxy_pad_get_internal (pad);

  if (internal) {
    res = gst_pad_push_event (internal, event);
    gst_object_unref (internal);
  }

  return res;
}

static gboolean
gst_proxy_pad_do_query (GstPad * pad, GstQuery * query)
{
  gboolean res = FALSE;
  GstPad *target = gst_proxy_pad_get_target (pad);

  if (target) {
    res = gst_pad_query (target, query);
    gst_object_unref (target);
  }

  return res;
}

static GstIterator *
gst_proxy_pad_do_iterate_internal_links (GstPad * pad)
{
  GstIterator *res = NULL;
  GstPad *internal = GST_PROXY_PAD_INTERNAL (pad);

  if (internal) {
    res =
        gst_iterator_new_single (GST_TYPE_PAD, internal,
        (GstCopyFunction) gst_object_ref, (GFreeFunc) gst_object_unref);
  }

  return res;
}

static GstFlowReturn
gst_proxy_pad_do_bufferalloc (GstPad * pad, guint64 offset, guint size,
    GstCaps * caps, GstBuffer ** buf)
{
  GstFlowReturn result = GST_FLOW_WRONG_STATE;
  GstPad *internal = gst_proxy_pad_get_internal (pad);

  if (internal) {
    result = gst_pad_alloc_buffer (internal, offset, size, caps, buf);
    gst_object_unref (internal);
  }

  return result;
}

static GstFlowReturn
gst_proxy_pad_do_chain (GstPad * pad, GstBuffer * buffer)
{
  GstFlowReturn res;
  GstPad *internal = GST_PROXY_PAD_INTERNAL (pad);

  res = gst_pad_push (internal, buffer);

  return res;
}

static GstFlowReturn
gst_proxy_pad_do_chain_list (GstPad * pad, GstBufferList * list)
{
  GstFlowReturn res;
  GstPad *internal = GST_PROXY_PAD_INTERNAL (pad);

  res = gst_pad_push_list (internal, list);

  return res;
}

static GstFlowReturn
gst_proxy_pad_do_getrange (GstPad * pad, guint64 offset, guint size,
    GstBuffer ** buffer)
{
  GstFlowReturn res;
  GstPad *internal = GST_PROXY_PAD_INTERNAL (pad);

  res = gst_pad_pull_range (internal, offset, size, buffer);

  return res;
}

static gboolean
gst_proxy_pad_do_checkgetrange (GstPad * pad)
{
  gboolean result;
  GstPad *internal = GST_PROXY_PAD_INTERNAL (pad);

  result = gst_pad_check_pull_range (internal);

  return result;
}

static GstCaps *
gst_proxy_pad_do_getcaps (GstPad * pad)
{
  GstPad *target = gst_proxy_pad_get_target (pad);
  GstCaps *res;
  GstPadTemplate *templ = GST_PAD_PAD_TEMPLATE (pad);

  if (target) {
    /* if we have a real target, proxy the call */
    res = gst_pad_get_caps_reffed (target);

    GST_DEBUG_OBJECT (pad, "get caps of target %s:%s : %" GST_PTR_FORMAT,
        GST_DEBUG_PAD_NAME (target), res);

    gst_object_unref (target);

    /* filter against the template */
    if (templ && res) {
      GstCaps *filt, *tmp;

      filt = GST_PAD_TEMPLATE_CAPS (templ);
      if (filt) {
        tmp = gst_caps_intersect (filt, res);
        gst_caps_unref (res);
        res = tmp;
        GST_DEBUG_OBJECT (pad,
            "filtered against template gives %" GST_PTR_FORMAT, res);
      }
    }
  } else {
    /* else, if we have a template, use its caps. */
    if (templ) {
      res = GST_PAD_TEMPLATE_CAPS (templ);
      GST_DEBUG_OBJECT (pad,
          "using pad template %p with caps %p %" GST_PTR_FORMAT, templ, res,
          res);
      res = gst_caps_ref (res);
      goto done;
    }

    /* last resort, any caps */
    GST_DEBUG_OBJECT (pad, "pad has no template, returning ANY");
    res = gst_caps_new_any ();
  }

done:
  return res;
}

static gboolean
gst_proxy_pad_do_acceptcaps (GstPad * pad, GstCaps * caps)
{
  GstPad *target = gst_proxy_pad_get_target (pad);
  gboolean res;

  if (target) {
    res = gst_pad_accept_caps (target, caps);
    gst_object_unref (target);
  } else {
    /* We don't have a target, we return TRUE and we assume that any future
     * target will be able to deal with any configured caps. */
    res = TRUE;
  }

  return res;
}

static void
gst_proxy_pad_do_fixatecaps (GstPad * pad, GstCaps * caps)
{
  GstPad *target = gst_proxy_pad_get_target (pad);

  if (target) {
    gst_pad_fixate_caps (target, caps);
    gst_object_unref (target);
  }
}

static gboolean
gst_proxy_pad_do_setcaps (GstPad * pad, GstCaps * caps)
{
  GstPad *target = gst_proxy_pad_get_target (pad);
  gboolean res;

  if (target) {
    res = gst_pad_set_caps (target, caps);
    gst_object_unref (target);
  } else {
    /* We don't have any target, but we shouldn't return FALSE since this
     * would stop the actual push of a buffer (which might trigger a pad block
     * or probe, or properly return GST_FLOW_NOT_LINKED.
     */
    res = TRUE;
  }
  return res;
}

static gboolean
gst_proxy_pad_set_target_unlocked (GstPad * pad, GstPad * target)
{
  GstPad *oldtarget;

  if (target) {
    GST_LOG_OBJECT (pad, "setting target %s:%s", GST_DEBUG_PAD_NAME (target));

    if (G_UNLIKELY (GST_PAD_DIRECTION (pad) != GST_PAD_DIRECTION (target)))
      goto wrong_direction;
  } else
    GST_LOG_OBJECT (pad, "clearing target");

  /* clear old target */
  if ((oldtarget = GST_PROXY_PAD_TARGET (pad)))
    gst_object_unref (oldtarget);

  /* set and ref new target if any */
  if (target)
    GST_PROXY_PAD_TARGET (pad) = gst_object_ref (target);
  else
    GST_PROXY_PAD_TARGET (pad) = NULL;

  return TRUE;

  /* ERRORS */
wrong_direction:
  {
    GST_ERROR_OBJECT (pad,
        "target pad doesn't have the same direction as ourself");
    return FALSE;
  }
}

static gboolean
gst_proxy_pad_set_target (GstPad * pad, GstPad * target)
{
  gboolean result;

  GST_PROXY_LOCK (pad);
  result = gst_proxy_pad_set_target_unlocked (pad, target);
  GST_PROXY_UNLOCK (pad);

  return result;
}

static GstPad *
gst_proxy_pad_get_target (GstPad * pad)
{
  GstPad *target;

  GST_PROXY_LOCK (pad);
  target = GST_PROXY_PAD_TARGET (pad);
  if (target)
    gst_object_ref (target);
  GST_PROXY_UNLOCK (pad);

  return target;
}

static GstPad *
gst_proxy_pad_get_internal (GstPad * pad)
{
  GstPad *internal;

  GST_PROXY_LOCK (pad);
  internal = GST_PROXY_PAD_INTERNAL (pad);
  if (internal)
    gst_object_ref (internal);
  GST_PROXY_UNLOCK (pad);

  return internal;
}

static void
gst_proxy_pad_do_unlink (GstPad * pad)
{
  GstPad *internal;

  /* don't do anything if this unlink resulted from retargeting the pad
   * controlled by the ghostpad. We only want to invalidate the target pad when
   * the element suddently unlinked with our internal pad. */
  if (GST_PROXY_PAD_RETARGET (pad))
    return;

  internal = GST_PROXY_PAD_INTERNAL (pad);

  GST_DEBUG_OBJECT (pad, "pad is unlinked");

  gst_proxy_pad_set_target (internal, NULL);
}

static void
gst_proxy_pad_class_init (GstProxyPadClass * klass)
{
  GObjectClass *gobject_class = (GObjectClass *) klass;

  g_type_class_add_private (klass, sizeof (GstProxyPadPrivate));

  gobject_class->dispose = gst_proxy_pad_dispose;
  gobject_class->finalize = gst_proxy_pad_finalize;

#if !defined(GST_DISABLE_LOADSAVE) && !defined(GST_REMOVE_DEPRECATED)
  {
    GstObjectClass *gstobject_class = (GstObjectClass *) klass;

    gstobject_class->save_thyself =
        ((gpointer (*)(GstObject * object,
                gpointer self)) *
        GST_DEBUG_FUNCPTR (gst_proxy_pad_save_thyself));
  }
#endif
  /* Register common function pointer descriptions */
  GST_DEBUG_REGISTER_FUNCPTR (gst_proxy_pad_do_query_type);
  GST_DEBUG_REGISTER_FUNCPTR (gst_proxy_pad_do_event);
  GST_DEBUG_REGISTER_FUNCPTR (gst_proxy_pad_do_query);
  GST_DEBUG_REGISTER_FUNCPTR (gst_proxy_pad_do_iterate_internal_links);
  GST_DEBUG_REGISTER_FUNCPTR (gst_proxy_pad_do_getcaps);
  GST_DEBUG_REGISTER_FUNCPTR (gst_proxy_pad_do_acceptcaps);
  GST_DEBUG_REGISTER_FUNCPTR (gst_proxy_pad_do_fixatecaps);
  GST_DEBUG_REGISTER_FUNCPTR (gst_proxy_pad_do_setcaps);
  GST_DEBUG_REGISTER_FUNCPTR (gst_proxy_pad_do_unlink);
  GST_DEBUG_REGISTER_FUNCPTR (gst_proxy_pad_do_bufferalloc);
  GST_DEBUG_REGISTER_FUNCPTR (gst_proxy_pad_do_chain);
  GST_DEBUG_REGISTER_FUNCPTR (gst_proxy_pad_do_chain_list);
  GST_DEBUG_REGISTER_FUNCPTR (gst_proxy_pad_do_getrange);
  GST_DEBUG_REGISTER_FUNCPTR (gst_proxy_pad_do_checkgetrange);
}

static void
gst_proxy_pad_dispose (GObject * object)
{
  GstPad *pad = GST_PAD (object);
  GstPad **target_p;

  GST_PROXY_LOCK (pad);
  /* remove and unref the target */
  target_p = &GST_PROXY_PAD_TARGET (pad);
  gst_object_replace ((GstObject **) target_p, NULL);
  /* The internal is only cleared by GstGhostPad::dispose, since it is the 
   * parent of non-ghost GstProxyPad and owns the refcount on the internal.
   */
  GST_PROXY_UNLOCK (pad);

  G_OBJECT_CLASS (gst_proxy_pad_parent_class)->dispose (object);
}

static void
gst_proxy_pad_finalize (GObject * object)
{
  GstProxyPad *pad = GST_PROXY_PAD (object);

  g_mutex_free (GST_PROXY_GET_LOCK (pad));
  GST_PROXY_GET_LOCK (pad) = NULL;

  G_OBJECT_CLASS (gst_proxy_pad_parent_class)->finalize (object);
}

static void
gst_proxy_pad_init (GstProxyPad * ppad)
{
  GstPad *pad = (GstPad *) ppad;

  GST_PROXY_PAD_PRIVATE (ppad) = G_TYPE_INSTANCE_GET_PRIVATE (ppad,
      GST_TYPE_PROXY_PAD, GstProxyPadPrivate);
  GST_PROXY_GET_LOCK (pad) = g_mutex_new ();

  gst_pad_set_query_type_function (pad, gst_proxy_pad_do_query_type);
  gst_pad_set_event_function (pad, gst_proxy_pad_do_event);
  gst_pad_set_query_function (pad, gst_proxy_pad_do_query);
  gst_pad_set_iterate_internal_links_function (pad,
      gst_proxy_pad_do_iterate_internal_links);

  gst_pad_set_getcaps_function (pad, gst_proxy_pad_do_getcaps);
  gst_pad_set_acceptcaps_function (pad, gst_proxy_pad_do_acceptcaps);
  gst_pad_set_fixatecaps_function (pad, gst_proxy_pad_do_fixatecaps);
  gst_pad_set_setcaps_function (pad, gst_proxy_pad_do_setcaps);
  gst_pad_set_unlink_function (pad, gst_proxy_pad_do_unlink);
}

#if !defined(GST_DISABLE_LOADSAVE) && !defined(GST_REMOVE_DEPRECATED)
/**
 * gst_proxy_pad_save_thyself:
 * @pad: a ghost #GstPad to save.
 * @parent: the parent #xmlNodePtr to save the description in.
 *
 * Saves the ghost pad into an xml representation.
 *
 * Returns: the #xmlNodePtr representation of the pad.
 */
static xmlNodePtr
gst_proxy_pad_save_thyself (GstObject * object, xmlNodePtr parent)
{
  xmlNodePtr self;
  GstProxyPad *proxypad;
  GstPad *pad;
  GstPad *peer;

  g_return_val_if_fail (GST_IS_PROXY_PAD (object), NULL);

  self = xmlNewChild (parent, NULL, (xmlChar *) "ghostpad", NULL);
  xmlNewChild (self, NULL, (xmlChar *) "name",
      (xmlChar *) GST_OBJECT_NAME (object));
  xmlNewChild (self, NULL, (xmlChar *) "parent",
      (xmlChar *) GST_OBJECT_NAME (GST_OBJECT_PARENT (object)));

  proxypad = GST_PROXY_PAD_CAST (object);
  pad = GST_PAD_CAST (proxypad);
  peer = GST_PAD_CAST (pad->peer);

  if (GST_IS_PAD (pad)) {
    if (GST_PAD_IS_SRC (pad))
      xmlNewChild (self, NULL, (xmlChar *) "direction", (xmlChar *) "source");
    else if (GST_PAD_IS_SINK (pad))
      xmlNewChild (self, NULL, (xmlChar *) "direction", (xmlChar *) "sink");
    else
      xmlNewChild (self, NULL, (xmlChar *) "direction", (xmlChar *) "unknown");
  } else {
    xmlNewChild (self, NULL, (xmlChar *) "direction", (xmlChar *) "unknown");
  }
  if (GST_IS_PAD (peer)) {
    gchar *content = g_strdup_printf ("%s.%s",
        GST_OBJECT_NAME (GST_PAD_PARENT (peer)), GST_PAD_NAME (peer));

    xmlNewChild (self, NULL, (xmlChar *) "peer", (xmlChar *) content);
    g_free (content);
  } else {
    xmlNewChild (self, NULL, (xmlChar *) "peer", NULL);
  }

  return self;
}
#endif /* GST_DISABLE_LOADSAVE */


/***********************************************************************
 * Ghost pads, implemented as a pair of proxy pads (sort of)
 */


#define GST_GHOST_PAD_PRIVATE(obj)	(GST_GHOST_PAD_CAST (obj)->priv)

struct _GstGhostPadPrivate
{
  /* with PROXY_LOCK */
  gulong notify_id;

  gboolean constructed;
};

G_DEFINE_TYPE (GstGhostPad, gst_ghost_pad, GST_TYPE_PROXY_PAD);

static void gst_ghost_pad_dispose (GObject * object);

/* see gstghostpad design docs */
static gboolean
gst_ghost_pad_internal_do_activate_push (GstPad * pad, gboolean active)
{
  gboolean ret;
  GstPad *other;

  GST_LOG_OBJECT (pad, "%sactivate push on %s:%s, we're ok",
      (active ? "" : "de"), GST_DEBUG_PAD_NAME (pad));

  /* in both cases (SRC and SINK) we activate just the internal pad. The targets
   * will be activated later (or already in case of a ghost sinkpad). */
  other = GST_PROXY_PAD_INTERNAL (pad);
  ret = gst_pad_activate_push (other, active);

  return ret;
}

static gboolean
gst_ghost_pad_internal_do_activate_pull (GstPad * pad, gboolean active)
{
  gboolean ret;
  GstPad *other;

  GST_LOG_OBJECT (pad, "%sactivate pull on %s:%s", (active ? "" : "de"),
      GST_DEBUG_PAD_NAME (pad));

  if (GST_PAD_DIRECTION (pad) == GST_PAD_SRC) {
    /* we are activated in pull mode by our peer element, which is a sinkpad
     * that wants to operate in pull mode. This activation has to propagate
     * upstream throught the pipeline. We call the internal activation function,
     * which will trigger gst_ghost_pad_do_activate_pull, which propagates even
     * further upstream */
    GST_LOG_OBJECT (pad, "pad is src, activate internal");
    other = GST_PROXY_PAD_INTERNAL (pad);
    ret = gst_pad_activate_pull (other, active);
  } else if (G_LIKELY ((other = gst_pad_get_peer (pad)))) {
    /* We are SINK, the ghostpad is SRC, we propagate the activation upstream
     * since we hold a pointer to the upstream peer. */
    GST_LOG_OBJECT (pad, "activating peer");
    ret = gst_pad_activate_pull (other, active);
    gst_object_unref (other);
  } else {
    /* this is failure, we can't activate pull if there is no peer */
    GST_LOG_OBJECT (pad, "not src and no peer, failing");
    ret = FALSE;
  }

  return ret;
}

static gboolean
gst_ghost_pad_do_activate_push (GstPad * pad, gboolean active)
{
  gboolean ret;
  GstPad *other;

  GST_LOG_OBJECT (pad, "%sactivate push on %s:%s, proxy internal",
      (active ? "" : "de"), GST_DEBUG_PAD_NAME (pad));

  /* just activate the internal pad */
  other = GST_PROXY_PAD_INTERNAL (pad);
  ret = gst_pad_activate_push (other, active);

  return ret;
}

static gboolean
gst_ghost_pad_do_activate_pull (GstPad * pad, gboolean active)
{
  gboolean ret;
  GstPad *other;

  GST_LOG_OBJECT (pad, "%sactivate pull on %s:%s", (active ? "" : "de"),
      GST_DEBUG_PAD_NAME (pad));

  if (GST_PAD_DIRECTION (pad) == GST_PAD_SRC) {
    /* the ghostpad is SRC and activated in pull mode by its peer, call the
     * activation function of the internal pad to propagate the activation
     * upstream */
    GST_LOG_OBJECT (pad, "pad is src, activate internal");
    other = GST_PROXY_PAD_INTERNAL (pad);
    ret = gst_pad_activate_pull (other, active);
  } else if (G_LIKELY ((other = gst_pad_get_peer (pad)))) {
    /* We are SINK and activated by the internal pad, propagate activation
     * upstream because we hold a ref to the upstream peer */
    GST_LOG_OBJECT (pad, "activating peer");
    ret = gst_pad_activate_pull (other, active);
    gst_object_unref (other);
  } else {
    /* no peer, we fail */
    GST_LOG_OBJECT (pad, "pad not src and no peer, failing");
    ret = FALSE;
  }

  return ret;
}

static GstPadLinkReturn
gst_ghost_pad_do_link (GstPad * pad, GstPad * peer)
{
  GstPadLinkReturn ret;
  GstPad *internal;

  GST_DEBUG_OBJECT (pad, "linking ghostpad");

  internal = GST_PROXY_PAD_INTERNAL (pad);
  if (!gst_proxy_pad_set_target (internal, peer))
    goto target_failed;

  ret = GST_PAD_LINK_OK;
  /* if we are a source pad, we should call the peer link function
   * if the peer has one, see design docs. */
  if (GST_PAD_IS_SRC (pad)) {
    if (GST_PAD_LINKFUNC (peer)) {
      ret = GST_PAD_LINKFUNC (peer) (peer, pad);
      if (ret != GST_PAD_LINK_OK)
        goto link_failed;
    }
  }
  return ret;

  /* ERRORS */
target_failed:
  {
    GST_DEBUG_OBJECT (pad, "setting target failed");
    return GST_PAD_LINK_REFUSED;
  }
link_failed:
  {
    GST_DEBUG_OBJECT (pad, "linking failed");
    /* clear target again */
    gst_proxy_pad_set_target (internal, NULL);
    return ret;
  }
}

static void
gst_ghost_pad_do_unlink (GstPad * pad)
{
  GstPad *internal;

  internal = GST_PROXY_PAD_INTERNAL (pad);

  GST_DEBUG_OBJECT (pad, "unlinking ghostpad");

  /* The target of the internal pad is no longer valid */
  gst_proxy_pad_set_target (internal, NULL);
}

static void
on_int_notify (GstPad * internal, GParamSpec * unused, GstGhostPad * pad)
{
  GstCaps *caps;
  gboolean changed;

  g_object_get (internal, "caps", &caps, NULL);

  GST_DEBUG_OBJECT (pad, "notified %p %" GST_PTR_FORMAT, caps, caps);

  GST_OBJECT_LOCK (pad);
  changed = (GST_PAD_CAPS (pad) != caps);
  if (changed)
    gst_caps_replace (&(GST_PAD_CAPS (pad)), caps);
  GST_OBJECT_UNLOCK (pad);

  if (changed) {
#if GLIB_CHECK_VERSION(2,26,0)
    g_object_notify_by_pspec ((GObject *) pad, pspec_caps);
#else
    g_object_notify ((GObject *) pad, "caps");
#endif
  }

  if (caps)
    gst_caps_unref (caps);
}

static void
on_src_target_notify (GstPad * target, GParamSpec * unused, gpointer user_data)
{
  GstProxyPad *proxypad;
  GstGhostPad *gpad;
  GstCaps *caps;
  gboolean changed;

  g_object_get (target, "caps", &caps, NULL);

  GST_OBJECT_LOCK (target);
  /* First check if the peer is still available and our proxy pad */
  if (!GST_PAD_PEER (target) || !GST_IS_PROXY_PAD (GST_PAD_PEER (target))) {
    GST_OBJECT_UNLOCK (target);
    goto done;
  }

  proxypad = GST_PROXY_PAD (GST_PAD_PEER (target));
  GST_PROXY_LOCK (proxypad);
  /* Now check if the proxypad's internal pad is still there and
   * a ghostpad */
  if (!GST_PROXY_PAD_INTERNAL (proxypad) ||
      !GST_IS_GHOST_PAD (GST_PROXY_PAD_INTERNAL (proxypad))) {
    GST_OBJECT_UNLOCK (target);
    GST_PROXY_UNLOCK (proxypad);
    goto done;
  }
  gpad = GST_GHOST_PAD (GST_PROXY_PAD_INTERNAL (proxypad));
  g_object_ref (gpad);
  GST_PROXY_UNLOCK (proxypad);
  GST_OBJECT_UNLOCK (target);

  GST_OBJECT_LOCK (gpad);

  GST_DEBUG_OBJECT (gpad, "notified %p %" GST_PTR_FORMAT, caps, caps);

  changed = (GST_PAD_CAPS (gpad) != caps);
  if (changed)
    gst_caps_replace (&(GST_PAD_CAPS (gpad)), caps);
  GST_OBJECT_UNLOCK (gpad);

  if (changed) {
#if GLIB_CHECK_VERSION(2,26,0)
    g_object_notify_by_pspec ((GObject *) gpad, pspec_caps);
#else
    g_object_notify ((GObject *) gpad, "caps");
#endif
  }

  g_object_unref (gpad);

done:
  if (caps)
    gst_caps_unref (caps);
}

static gboolean
gst_ghost_pad_do_setcaps (GstPad * pad, GstCaps * caps)
{
  if (GST_PAD_DIRECTION (pad) == GST_PAD_SRC)
    return TRUE;

  return gst_proxy_pad_do_setcaps (pad, caps);
}

static GstIterator *
gst_ghost_pad_do_iterate_internal_links (GstPad * pad)
{
  GstIterator *res = NULL;
  GstPad *internal = GST_PROXY_PAD_INTERNAL (GST_GHOST_PAD_CAST (pad));

  if (internal) {
    res =
        gst_iterator_new_single (GST_TYPE_PAD, internal,
        (GstCopyFunction) gst_object_ref, (GFreeFunc) gst_object_unref);
  }

  return res;
}

static void
gst_ghost_pad_class_init (GstGhostPadClass * klass)
{
  GObjectClass *gobject_class = (GObjectClass *) klass;

  g_type_class_add_private (klass, sizeof (GstGhostPadPrivate));

  pspec_caps = g_object_class_find_property (gobject_class, "caps");

  gobject_class->dispose = gst_ghost_pad_dispose;

  GST_DEBUG_REGISTER_FUNCPTR (gst_ghost_pad_do_setcaps);
  GST_DEBUG_REGISTER_FUNCPTR (gst_ghost_pad_do_activate_pull);
  GST_DEBUG_REGISTER_FUNCPTR (gst_ghost_pad_do_activate_push);
  GST_DEBUG_REGISTER_FUNCPTR (gst_ghost_pad_do_link);
}

static void
gst_ghost_pad_init (GstGhostPad * pad)
{
  GST_GHOST_PAD_PRIVATE (pad) = G_TYPE_INSTANCE_GET_PRIVATE (pad,
      GST_TYPE_GHOST_PAD, GstGhostPadPrivate);

  gst_pad_set_setcaps_function (GST_PAD_CAST (pad), gst_ghost_pad_do_setcaps);
  gst_pad_set_activatepull_function (GST_PAD_CAST (pad),
      gst_ghost_pad_do_activate_pull);
  gst_pad_set_activatepush_function (GST_PAD_CAST (pad),
      gst_ghost_pad_do_activate_push);
  gst_pad_set_iterate_internal_links_function (GST_PAD_CAST (pad),
      gst_ghost_pad_do_iterate_internal_links);
}

static void
gst_ghost_pad_dispose (GObject * object)
{
  GstPad *pad;
  GstPad *internal;
  GstPad *peer;

  pad = GST_PAD (object);

  GST_DEBUG_OBJECT (pad, "dispose");

  gst_ghost_pad_set_target (GST_GHOST_PAD (pad), NULL);

  /* Unlink here so that gst_pad_dispose doesn't. That would lead to a call to
   * gst_ghost_pad_do_unlink when the ghost pad is in an inconsistent state */
  peer = gst_pad_get_peer (pad);
  if (peer) {
    if (GST_PAD_IS_SRC (pad))
      gst_pad_unlink (pad, peer);
    else
      gst_pad_unlink (peer, pad);

    gst_object_unref (peer);
  }

  GST_PROXY_LOCK (pad);
  internal = GST_PROXY_PAD_INTERNAL (pad);

  gst_pad_set_activatepull_function (internal, NULL);
  gst_pad_set_activatepush_function (internal, NULL);

  g_signal_handler_disconnect (internal,
      GST_GHOST_PAD_PRIVATE (pad)->notify_id);

  /* disposes of the internal pad, since the ghostpad is the only possible object
   * that has a refcount on the internal pad. */
  gst_object_unparent (GST_OBJECT_CAST (internal));
  GST_PROXY_PAD_INTERNAL (pad) = NULL;

  GST_PROXY_UNLOCK (pad);

  G_OBJECT_CLASS (gst_ghost_pad_parent_class)->dispose (object);
}

/**
 * gst_ghost_pad_construct:
 * @gpad: the newly allocated ghost pad
 *
 * Finish initialization of a newly allocated ghost pad.
 *
 * This function is most useful in language bindings and when subclassing
 * #GstGhostPad; plugin and application developers normally will not call this
 * function. Call this function directly after a call to g_object_new
 * (GST_TYPE_GHOST_PAD, "direction", @dir, ..., NULL).
 *
 * Returns: %TRUE if the construction succeeds, %FALSE otherwise.
 *
 * Since: 0.10.22
 */
gboolean
gst_ghost_pad_construct (GstGhostPad * gpad)
{
  GstPadDirection dir, otherdir;
  GstPadTemplate *templ;
  GstPad *pad, *internal;

  g_return_val_if_fail (GST_IS_GHOST_PAD (gpad), FALSE);
  g_return_val_if_fail (GST_GHOST_PAD_PRIVATE (gpad)->constructed == FALSE,
      FALSE);

  g_object_get (gpad, "direction", &dir, "template", &templ, NULL);

  g_return_val_if_fail (dir != GST_PAD_UNKNOWN, FALSE);

  pad = GST_PAD (gpad);

  /* Set directional padfunctions for ghostpad */
  if (dir == GST_PAD_SINK) {
    gst_pad_set_bufferalloc_function (pad, gst_proxy_pad_do_bufferalloc);
    gst_pad_set_chain_function (pad, gst_proxy_pad_do_chain);
    gst_pad_set_chain_list_function (pad, gst_proxy_pad_do_chain_list);
  } else {
    gst_pad_set_getrange_function (pad, gst_proxy_pad_do_getrange);
    gst_pad_set_checkgetrange_function (pad, gst_proxy_pad_do_checkgetrange);
  }

  /* link/unlink functions */
  gst_pad_set_link_function (pad, gst_ghost_pad_do_link);
  gst_pad_set_unlink_function (pad, gst_ghost_pad_do_unlink);

  /* INTERNAL PAD, it always exists and is child of the ghostpad */
  otherdir = (dir == GST_PAD_SRC) ? GST_PAD_SINK : GST_PAD_SRC;
  if (templ) {
    internal =
        g_object_new (GST_TYPE_PROXY_PAD, "name", NULL,
        "direction", otherdir, "template", templ, NULL);
    /* release ref obtained via g_object_get */
    gst_object_unref (templ);
  } else {
    internal =
        g_object_new (GST_TYPE_PROXY_PAD, "name", NULL,
        "direction", otherdir, NULL);
  }
  GST_PAD_UNSET_FLUSHING (internal);

  /* Set directional padfunctions for internal pad */
  if (dir == GST_PAD_SRC) {
    gst_pad_set_bufferalloc_function (internal, gst_proxy_pad_do_bufferalloc);
    gst_pad_set_chain_function (internal, gst_proxy_pad_do_chain);
    gst_pad_set_chain_list_function (internal, gst_proxy_pad_do_chain_list);
  } else {
    gst_pad_set_getrange_function (internal, gst_proxy_pad_do_getrange);
    gst_pad_set_checkgetrange_function (internal,
        gst_proxy_pad_do_checkgetrange);
  }

  GST_PROXY_LOCK (pad);

  /* now make the ghostpad a parent of the internal pad */
  if (!gst_object_set_parent (GST_OBJECT_CAST (internal),
          GST_OBJECT_CAST (pad)))
    goto parent_failed;

  /* The ghostpad is the parent of the internal pad and is the only object that
   * can have a refcount on the internal pad.
   * At this point, the GstGhostPad has a refcount of 1, and the internal pad has
   * a refcount of 1.
   * When the refcount of the GstGhostPad drops to 0, the ghostpad will dispose
   * its refcount on the internal pad in the dispose method by un-parenting it.
   * This is why we don't take extra refcounts in the assignments below
   */
  GST_PROXY_PAD_INTERNAL (pad) = internal;
  GST_PROXY_PAD_INTERNAL (internal) = pad;

  /* could be more general here, iterating over all writable properties...
   * taking the short road for now tho */
  GST_GHOST_PAD_PRIVATE (pad)->notify_id =
      g_signal_connect (internal, "notify::caps", G_CALLBACK (on_int_notify),
      pad);

  /* call function to init values of the pad caps */
  on_int_notify (internal, NULL, GST_GHOST_PAD_CAST (pad));

  /* special activation functions for the internal pad */
  gst_pad_set_activatepull_function (internal,
      gst_ghost_pad_internal_do_activate_pull);
  gst_pad_set_activatepush_function (internal,
      gst_ghost_pad_internal_do_activate_push);

  GST_PROXY_UNLOCK (pad);

  GST_GHOST_PAD_PRIVATE (gpad)->constructed = TRUE;
  return TRUE;

  /* ERRORS */
parent_failed:
  {
    GST_WARNING_OBJECT (gpad, "Could not set internal pad %s:%s",
        GST_DEBUG_PAD_NAME (internal));
    g_critical ("Could not set internal pad %s:%s",
        GST_DEBUG_PAD_NAME (internal));
    GST_PROXY_UNLOCK (pad);
    gst_object_unref (internal);
    return FALSE;
  }
}

static GstPad *
gst_ghost_pad_new_full (const gchar * name, GstPadDirection dir,
    GstPadTemplate * templ)
{
  GstGhostPad *ret;

  g_return_val_if_fail (dir != GST_PAD_UNKNOWN, NULL);

  /* OBJECT CREATION */
  if (templ) {
    ret = g_object_new (GST_TYPE_GHOST_PAD, "name", name,
        "direction", dir, "template", templ, NULL);
  } else {
    ret = g_object_new (GST_TYPE_GHOST_PAD, "name", name,
        "direction", dir, NULL);
  }

  if (!gst_ghost_pad_construct (ret))
    goto construct_failed;

  return GST_PAD_CAST (ret);

construct_failed:
  /* already logged */
  gst_object_unref (ret);
  return NULL;
}

/**
 * gst_ghost_pad_new_no_target:
 * @name: (allow-none): the name of the new pad, or NULL to assign a default name.
 * @dir: the direction of the ghostpad
 *
 * Create a new ghostpad without a target with the given direction.
 * A target can be set on the ghostpad later with the
 * gst_ghost_pad_set_target() function.
 *
 * The created ghostpad will not have a padtemplate.
 *
 * Returns: (transfer full): a new #GstPad, or NULL in case of an error.
 */
GstPad *
gst_ghost_pad_new_no_target (const gchar * name, GstPadDirection dir)
{
  GstPad *ret;

  g_return_val_if_fail (dir != GST_PAD_UNKNOWN, NULL);

  GST_LOG ("name:%s, direction:%d", GST_STR_NULL (name), dir);

  ret = gst_ghost_pad_new_full (name, dir, NULL);

  return ret;
}

/**
 * gst_ghost_pad_new:
 * @name: (allow-none): the name of the new pad, or NULL to assign a default name
 * @target: (transfer none): the pad to ghost.
 *
 * Create a new ghostpad with @target as the target. The direction will be taken
 * from the target pad. @target must be unlinked.
 *
 * Will ref the target.
 *
 * Returns: (transfer full): a new #GstPad, or NULL in case of an error.
 */
GstPad *
gst_ghost_pad_new (const gchar * name, GstPad * target)
{
  GstPad *ret;

  g_return_val_if_fail (GST_IS_PAD (target), NULL);
  g_return_val_if_fail (!gst_pad_is_linked (target), NULL);

  GST_LOG ("name:%s, target:%s:%s", GST_STR_NULL (name),
      GST_DEBUG_PAD_NAME (target));

  if ((ret = gst_ghost_pad_new_no_target (name, GST_PAD_DIRECTION (target))))
    if (!gst_ghost_pad_set_target (GST_GHOST_PAD_CAST (ret), target))
      goto set_target_failed;

  return ret;

  /* ERRORS */
set_target_failed:
  {
    GST_WARNING_OBJECT (ret, "failed to set target %s:%s",
        GST_DEBUG_PAD_NAME (target));
    gst_object_unref (ret);
    return NULL;
  }
}

/**
 * gst_ghost_pad_new_from_template:
 * @name: (allow-none): the name of the new pad, or NULL to assign a default name.
 * @target: (transfer none): the pad to ghost.
 * @templ: (transfer none): the #GstPadTemplate to use on the ghostpad.
 *
 * Create a new ghostpad with @target as the target. The direction will be taken
 * from the target pad. The template used on the ghostpad will be @template.
 *
 * Will ref the target.
 *
 * Returns: (transfer full): a new #GstPad, or NULL in case of an error.
 *
 * Since: 0.10.10
 */

GstPad *
gst_ghost_pad_new_from_template (const gchar * name, GstPad * target,
    GstPadTemplate * templ)
{
  GstPad *ret;

  g_return_val_if_fail (GST_IS_PAD (target), NULL);
  g_return_val_if_fail (!gst_pad_is_linked (target), NULL);
  g_return_val_if_fail (templ != NULL, NULL);
  g_return_val_if_fail (GST_PAD_TEMPLATE_DIRECTION (templ) ==
      GST_PAD_DIRECTION (target), NULL);

  GST_LOG ("name:%s, target:%s:%s, templ:%p", GST_STR_NULL (name),
      GST_DEBUG_PAD_NAME (target), templ);

  if ((ret = gst_ghost_pad_new_full (name, GST_PAD_DIRECTION (target), templ)))
    if (!gst_ghost_pad_set_target (GST_GHOST_PAD_CAST (ret), target))
      goto set_target_failed;

  return ret;

  /* ERRORS */
set_target_failed:
  {
    GST_WARNING_OBJECT (ret, "failed to set target %s:%s",
        GST_DEBUG_PAD_NAME (target));
    gst_object_unref (ret);
    return NULL;
  }
}

/**
 * gst_ghost_pad_new_no_target_from_template:
 * @name: (allow-none): the name of the new pad, or NULL to assign a default name
 * @templ: (transfer none): the #GstPadTemplate to create the ghostpad from.
 *
 * Create a new ghostpad based on @templ, without setting a target. The
 * direction will be taken from the @templ.
 *
 * Returns: (transfer full): a new #GstPad, or NULL in case of an error.
 *
 * Since: 0.10.10
 */
GstPad *
gst_ghost_pad_new_no_target_from_template (const gchar * name,
    GstPadTemplate * templ)
{
  GstPad *ret;

  g_return_val_if_fail (templ != NULL, NULL);

  ret =
      gst_ghost_pad_new_full (name, GST_PAD_TEMPLATE_DIRECTION (templ), templ);

  return ret;
}

/**
 * gst_ghost_pad_get_target:
 * @gpad: the #GstGhostPad
 *
 * Get the target pad of @gpad. Unref target pad after usage.
 *
 * Returns: (transfer full): the target #GstPad, can be NULL if the ghostpad
 * has no target set. Unref target pad after usage.
 */
GstPad *
gst_ghost_pad_get_target (GstGhostPad * gpad)
{
  GstPad *ret;

  g_return_val_if_fail (GST_IS_GHOST_PAD (gpad), NULL);

  ret = gst_proxy_pad_get_target (GST_PAD_CAST (gpad));

  GST_DEBUG_OBJECT (gpad, "get target %s:%s", GST_DEBUG_PAD_NAME (ret));

  return ret;
}

/**
 * gst_ghost_pad_set_target:
 * @gpad: the #GstGhostPad
 * @newtarget: (transfer none) (allow-none): the new pad target
 *
 * Set the new target of the ghostpad @gpad. Any existing target
 * is unlinked and links to the new target are established. if @newtarget is
 * NULL the target will be cleared.
 *
 * Returns: (transfer full): TRUE if the new target could be set. This function
 *     can return FALSE when the internal pads could not be linked.
 */
gboolean
gst_ghost_pad_set_target (GstGhostPad * gpad, GstPad * newtarget)
{
  GstPad *internal;
  GstPad *oldtarget;
  gboolean result;
  GstPadLinkReturn lret;

  g_return_val_if_fail (GST_IS_GHOST_PAD (gpad), FALSE);
  g_return_val_if_fail (GST_PAD_CAST (gpad) != newtarget, FALSE);
  g_return_val_if_fail (newtarget != GST_PROXY_PAD_INTERNAL (gpad), FALSE);

  /* no need for locking, the internal pad's lifecycle is directly linked to the
   * ghostpad's */
  internal = GST_PROXY_PAD_INTERNAL (gpad);

  if (newtarget)
    GST_DEBUG_OBJECT (gpad, "set target %s:%s", GST_DEBUG_PAD_NAME (newtarget));
  else
    GST_DEBUG_OBJECT (gpad, "clearing target");

  /* clear old target */
  GST_PROXY_LOCK (gpad);
  if ((oldtarget = GST_PROXY_PAD_TARGET (gpad))) {
    if (GST_PAD_IS_SRC (oldtarget)) {
      g_signal_handlers_disconnect_by_func (oldtarget,
          (gpointer) on_src_target_notify, NULL);
    }

    GST_PROXY_PAD_RETARGET (internal) = TRUE;

    /* unlink internal pad */
    if (GST_PAD_IS_SRC (internal))
      gst_pad_unlink (internal, oldtarget);
    else
      gst_pad_unlink (oldtarget, internal);

    GST_PROXY_PAD_RETARGET (internal) = FALSE;
  }

  result = gst_proxy_pad_set_target_unlocked (GST_PAD_CAST (gpad), newtarget);
  GST_PROXY_UNLOCK (gpad);

  if (result && newtarget) {
    if (GST_PAD_IS_SRC (newtarget)) {
      g_signal_connect (newtarget, "notify::caps",
          G_CALLBACK (on_src_target_notify), NULL);
    }

    /* and link to internal pad without any checks */
    GST_DEBUG_OBJECT (gpad, "connecting internal pad to target");

    if (GST_PAD_IS_SRC (internal))
      lret =
          gst_pad_link_full (internal, newtarget, GST_PAD_LINK_CHECK_NOTHING);
    else
      lret =
          gst_pad_link_full (newtarget, internal, GST_PAD_LINK_CHECK_NOTHING);

    if (lret != GST_PAD_LINK_OK)
      goto link_failed;
  }

  return result;

  /* ERRORS */
link_failed:
  {
    GST_WARNING_OBJECT (gpad, "could not link internal and target, reason:%d",
        lret);
    /* and unset target again */
    GST_PROXY_LOCK (gpad);
    gst_proxy_pad_set_target_unlocked (GST_PAD_CAST (gpad), NULL);
    GST_PROXY_UNLOCK (gpad);
    return FALSE;
  }
}
