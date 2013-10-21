/* GStreamer
 * Copyright (C) 2005 Wim Taymans <wim@fluendo.com>
 *
 * gstcollectpads.c:
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
 * SECTION:gstcollectpads
 * @short_description: manages a set of pads that operate in collect mode
 * @see_also:
 *
 * Manages a set of pads that operate in collect mode. This means that control
 * is given to the manager of this object when all pads have data.
 * <itemizedlist>
 *   <listitem><para>
 *     Collectpads are created with gst_collect_pads_new(). A callback should then
 *     be installed with gst_collect_pads_set_function ().
 *   </para></listitem>
 *   <listitem><para>
 *     Pads are added to the collection with gst_collect_pads_add_pad()/
 *     gst_collect_pads_remove_pad(). The pad
 *     has to be a sinkpad. The chain and event functions of the pad are
 *     overridden. The element_private of the pad is used to store
 *     private information for the collectpads.
 *   </para></listitem>
 *   <listitem><para>
 *     For each pad, data is queued in the _chain function or by
 *     performing a pull_range.
 *   </para></listitem>
 *   <listitem><para>
 *     When data is queued on all pads, the callback function is called.
 *   </para></listitem>
 *   <listitem><para>
 *     Data can be dequeued from the pad with the gst_collect_pads_pop() method.
 *     One can peek at the data with the gst_collect_pads_peek() function.
 *     These functions will return NULL if the pad received an EOS event. When all
 *     pads return NULL from a gst_collect_pads_peek(), the element can emit an EOS
 *     event itself.
 *   </para></listitem>
 *   <listitem><para>
 *     Data can also be dequeued in byte units using the gst_collect_pads_available(),
 *     gst_collect_pads_read() and gst_collect_pads_flush() calls.
 *   </para></listitem>
 *   <listitem><para>
 *     Elements should call gst_collect_pads_start() and gst_collect_pads_stop() in
 *     their state change functions to start and stop the processing of the collecpads.
 *     The gst_collect_pads_stop() call should be called before calling the parent
 *     element state change function in the PAUSED_TO_READY state change to ensure
 *     no pad is blocked and the element can finish streaming.
 *   </para></listitem>
 *   <listitem><para>
 *     gst_collect_pads_collect() and gst_collect_pads_collect_range() can be used by
 *     elements that start a #GstTask to drive the collect_pads. This feature is however
 *     not yet implemented.
 *   </para></listitem>
 * </itemizedlist>
 *
 * Last reviewed on 2006-05-10 (0.10.6)
 */

#include "gstcollectpads.h"

GST_DEBUG_CATEGORY_STATIC (collect_pads_debug);
#define GST_CAT_DEFAULT collect_pads_debug

#define GST_COLLECT_PADS_GET_PRIVATE(obj)  \
  (G_TYPE_INSTANCE_GET_PRIVATE ((obj), GST_TYPE_COLLECT_PADS, GstCollectPadsPrivate))

struct _GstCollectPadsPrivate
{
  GstCollectPadsClipFunction clipfunc;
  gpointer clipfunc_user_data;
};

GST_BOILERPLATE (GstCollectPads, gst_collect_pads, GstObject, GST_TYPE_OBJECT);

static void gst_collect_pads_clear (GstCollectPads * pads,
    GstCollectData * data);
static GstFlowReturn gst_collect_pads_chain (GstPad * pad, GstBuffer * buffer);
static gboolean gst_collect_pads_event (GstPad * pad, GstEvent * event);
static void gst_collect_pads_finalize (GObject * object);
static void ref_data (GstCollectData * data);
static void unref_data (GstCollectData * data);
static void gst_collect_pads_check_pads_unlocked (GstCollectPads * pads);

static void
gst_collect_pads_base_init (gpointer g_class)
{
  /* Do nothing here */
}

static void
gst_collect_pads_class_init (GstCollectPadsClass * klass)
{
  GObjectClass *gobject_class = (GObjectClass *) klass;

  g_type_class_add_private (klass, sizeof (GstCollectPadsPrivate));

  GST_DEBUG_CATEGORY_INIT (collect_pads_debug, "collectpads", 0,
      "GstCollectPads");

  gobject_class->finalize = gst_collect_pads_finalize;
}

static void
gst_collect_pads_init (GstCollectPads * pads, GstCollectPadsClass * g_class)
{
  pads->abidata.ABI.priv = GST_COLLECT_PADS_GET_PRIVATE (pads);

  pads->cond = g_cond_new ();
  pads->data = NULL;
  pads->cookie = 0;
  pads->numpads = 0;
  pads->queuedpads = 0;
  pads->eospads = 0;
  pads->started = FALSE;

  /* members to manage the pad list */
  pads->abidata.ABI.pad_lock = g_mutex_new ();
  pads->abidata.ABI.pad_cookie = 0;
  pads->abidata.ABI.pad_list = NULL;
}

static void
gst_collect_pads_finalize (GObject * object)
{
  GSList *collected;
  GstCollectPads *pads = GST_COLLECT_PADS (object);

  GST_DEBUG ("finalize");

  g_cond_free (pads->cond);
  g_mutex_free (pads->abidata.ABI.pad_lock);

  /* Remove pads */
  collected = pads->abidata.ABI.pad_list;
  for (; collected; collected = g_slist_next (collected)) {
    GstCollectData *pdata = (GstCollectData *) collected->data;

    unref_data (pdata);
  }
  /* Free pads list */
  g_slist_foreach (pads->data, (GFunc) unref_data, NULL);
  g_slist_free (pads->data);
  g_slist_free (pads->abidata.ABI.pad_list);

  G_OBJECT_CLASS (parent_class)->finalize (object);
}

/**
 * gst_collect_pads_new:
 *
 * Create a new instance of #GstCollectPads.
 *
 * MT safe.
 *
 * Returns: (transfer full): a new #GstCollectPads, or NULL in case of an error.
 */
GstCollectPads *
gst_collect_pads_new (void)
{
  GstCollectPads *newcoll;

  newcoll = g_object_newv (GST_TYPE_COLLECT_PADS, 0, NULL);

  return newcoll;
}

/**
 * gst_collect_pads_set_function:
 * @pads: the collectspads to use
 * @func: the function to set
 * @user_data: (closure): user data passed to the function
 *
 * Set the callback function and user data that will be called when
 * all the pads added to the collection have buffers queued.
 *
 * MT safe.
 */
void
gst_collect_pads_set_function (GstCollectPads * pads,
    GstCollectPadsFunction func, gpointer user_data)
{
  g_return_if_fail (pads != NULL);
  g_return_if_fail (GST_IS_COLLECT_PADS (pads));

  GST_OBJECT_LOCK (pads);
  pads->func = func;
  pads->user_data = user_data;
  GST_OBJECT_UNLOCK (pads);
}

static void
ref_data (GstCollectData * data)
{
  g_assert (data != NULL);

  g_atomic_int_inc (&(data->abidata.ABI.refcount));
}

static void
unref_data (GstCollectData * data)
{
  GstCollectDataDestroyNotify destroy_notify;

  g_assert (data != NULL);
  g_assert (data->abidata.ABI.refcount > 0);

  if (!g_atomic_int_dec_and_test (&(data->abidata.ABI.refcount)))
    return;

  /* FIXME: Ugly hack as we can't add more fields to GstCollectData */
  destroy_notify = (GstCollectDataDestroyNotify)
      g_object_get_data (G_OBJECT (data->pad),
      "gst-collect-data-destroy-notify");

  if (destroy_notify)
    destroy_notify (data);

  g_object_unref (data->pad);
  if (data->buffer) {
    gst_buffer_unref (data->buffer);
  }
  g_free (data);
}

/**
 * gst_collect_pads_add_pad:
 * @pads: the collectspads to use
 * @pad: (transfer none): the pad to add
 * @size: the size of the returned #GstCollectData structure
 *
 * Add a pad to the collection of collect pads. The pad has to be
 * a sinkpad. The refcount of the pad is incremented. Use
 * gst_collect_pads_remove_pad() to remove the pad from the collection
 * again.
 *
 * This function will override the chain and event functions of the pad
 * along with the element_private data, which is used to store private
 * information for the collectpads.
 *
 * You specify a size for the returned #GstCollectData structure
 * so that you can use it to store additional information.
 *
 * The pad will be automatically activated in push mode when @pads is
 * started.
 *
 * This function calls gst_collect_pads_add_pad_full() passing a value of NULL
 * for destroy_notify.
 *
 * MT safe.
 *
 * Returns: a new #GstCollectData to identify the new pad. Or NULL
 *   if wrong parameters are supplied.
 */
GstCollectData *
gst_collect_pads_add_pad (GstCollectPads * pads, GstPad * pad, guint size)
{
  return gst_collect_pads_add_pad_full (pads, pad, size, NULL);
}

/**
 * gst_collect_pads_add_pad_full:
 * @pads: the collectspads to use
 * @pad: (transfer none): the pad to add
 * @size: the size of the returned #GstCollectData structure
 * @destroy_notify: function to be called before the returned #GstCollectData
 * structure is freed
 *
 * Add a pad to the collection of collect pads. The pad has to be
 * a sinkpad. The refcount of the pad is incremented. Use
 * gst_collect_pads_remove_pad() to remove the pad from the collection
 * again.
 *
 * You specify a size for the returned #GstCollectData structure
 * so that you can use it to store additional information.
 *
 * You can also specify a #GstCollectDataDestroyNotify that will be called
 * just before the #GstCollectData structure is freed. It is passed the
 * pointer to the structure and should free any custom memory and resources
 * allocated for it.
 *
 * The pad will be automatically activated in push mode when @pads is
 * started.
 *
 * MT safe.
 *
 * Since: 0.10.12
 *
 * Returns: a new #GstCollectData to identify the new pad. Or NULL
 *   if wrong parameters are supplied.
 */
GstCollectData *
gst_collect_pads_add_pad_full (GstCollectPads * pads, GstPad * pad, guint size,
    GstCollectDataDestroyNotify destroy_notify)
{
  GstCollectData *data;

  g_return_val_if_fail (pads != NULL, NULL);
  g_return_val_if_fail (GST_IS_COLLECT_PADS (pads), NULL);
  g_return_val_if_fail (pad != NULL, NULL);
  g_return_val_if_fail (GST_PAD_IS_SINK (pad), NULL);
  g_return_val_if_fail (size >= sizeof (GstCollectData), NULL);

  GST_DEBUG ("adding pad %s:%s", GST_DEBUG_PAD_NAME (pad));

  data = g_malloc0 (size);
  data->collect = pads;
  data->pad = gst_object_ref (pad);
  data->buffer = NULL;
  data->pos = 0;
  gst_segment_init (&data->segment, GST_FORMAT_UNDEFINED);
  data->abidata.ABI.flushing = FALSE;
  data->abidata.ABI.new_segment = FALSE;
  data->abidata.ABI.eos = FALSE;
  data->abidata.ABI.refcount = 1;

  /* FIXME: Ugly hack as we can't add more fields to GstCollectData */
  g_object_set_data (G_OBJECT (pad), "gst-collect-data-destroy-notify",
      (void *) destroy_notify);

  GST_COLLECT_PADS_PAD_LOCK (pads);
  GST_OBJECT_LOCK (pad);
  gst_pad_set_element_private (pad, data);
  GST_OBJECT_UNLOCK (pad);
  pads->abidata.ABI.pad_list =
      g_slist_append (pads->abidata.ABI.pad_list, data);
  gst_pad_set_chain_function (pad, GST_DEBUG_FUNCPTR (gst_collect_pads_chain));
  gst_pad_set_event_function (pad, GST_DEBUG_FUNCPTR (gst_collect_pads_event));
  /* activate the pad when needed */
  if (pads->started)
    gst_pad_set_active (pad, TRUE);
  pads->abidata.ABI.pad_cookie++;
  GST_COLLECT_PADS_PAD_UNLOCK (pads);

  return data;
}

static gint
find_pad (GstCollectData * data, GstPad * pad)
{
  if (data->pad == pad)
    return 0;
  return 1;
}

/**
 * gst_collect_pads_set_clip_function:
 * @pads: the collectspads to use
 * @clipfunc: clip function to install
 * @user_data: (closure): user data to pass to @clip_func
 *
 * Install a clipping function that is called right after a buffer is received
 * on a pad managed by @pads. See #GstCollectPadsClipFunction for more info.
 *
 * Since: 0.10.26
 */
void
gst_collect_pads_set_clip_function (GstCollectPads * pads,
    GstCollectPadsClipFunction clipfunc, gpointer user_data)
{
  GstCollectPadsPrivate *priv;

  g_return_if_fail (pads != NULL);
  g_return_if_fail (GST_IS_COLLECT_PADS (pads));

  priv = pads->abidata.ABI.priv;

  priv->clipfunc = clipfunc;
  priv->clipfunc_user_data = user_data;
}

/**
 * gst_collect_pads_remove_pad:
 * @pads: the collectspads to use
 * @pad: (transfer none): the pad to remove
 *
 * Remove a pad from the collection of collect pads. This function will also
 * free the #GstCollectData and all the resources that were allocated with
 * gst_collect_pads_add_pad().
 *
 * The pad will be deactivated automatically when @pads is stopped.
 *
 * MT safe.
 *
 * Returns: %TRUE if the pad could be removed.
 */
gboolean
gst_collect_pads_remove_pad (GstCollectPads * pads, GstPad * pad)
{
  GstCollectData *data;
  GSList *list;

  g_return_val_if_fail (pads != NULL, FALSE);
  g_return_val_if_fail (GST_IS_COLLECT_PADS (pads), FALSE);
  g_return_val_if_fail (pad != NULL, FALSE);
  g_return_val_if_fail (GST_IS_PAD (pad), FALSE);

  GST_DEBUG ("removing pad %s:%s", GST_DEBUG_PAD_NAME (pad));

  GST_COLLECT_PADS_PAD_LOCK (pads);
  list =
      g_slist_find_custom (pads->abidata.ABI.pad_list, pad,
      (GCompareFunc) find_pad);
  if (!list)
    goto unknown_pad;

  data = (GstCollectData *) list->data;

  GST_DEBUG ("found pad %s:%s at %p", GST_DEBUG_PAD_NAME (pad), data);

  /* clear the stuff we configured */
  gst_pad_set_chain_function (pad, NULL);
  gst_pad_set_event_function (pad, NULL);
  GST_OBJECT_LOCK (pad);
  gst_pad_set_element_private (pad, NULL);
  GST_OBJECT_UNLOCK (pad);

  /* backward compat, also remove from data if stopped, note that this function
   * can only be called when we are stopped because we don't take the LOCK to
   * protect the pads->data list. */
  if (!pads->started) {
    GSList *dlist;

    dlist = g_slist_find_custom (pads->data, pad, (GCompareFunc) find_pad);
    if (dlist) {
      GstCollectData *pdata = dlist->data;

      pads->data = g_slist_delete_link (pads->data, dlist);
      unref_data (pdata);
    }
  }
  /* remove from the pad list */
  pads->abidata.ABI.pad_list =
      g_slist_delete_link (pads->abidata.ABI.pad_list, list);
  pads->abidata.ABI.pad_cookie++;

  /* signal waiters because something changed */
  GST_COLLECT_PADS_BROADCAST (pads);

  /* deactivate the pad when needed */
  if (!pads->started)
    gst_pad_set_active (pad, FALSE);

  /* clean and free the collect data */
  unref_data (data);

  GST_COLLECT_PADS_PAD_UNLOCK (pads);

  return TRUE;

unknown_pad:
  {
    GST_WARNING ("cannot remove unknown pad %s:%s", GST_DEBUG_PAD_NAME (pad));
    GST_COLLECT_PADS_PAD_UNLOCK (pads);
    return FALSE;
  }
}

/**
 * gst_collect_pads_is_active:
 * @pads: (transfer none): the collectspads to use
 * @pad: the pad to check
 *
 * Check if a pad is active.
 *
 * This function is currently not implemented.
 *
 * MT safe.
 *
 * Returns: %TRUE if the pad is active.
 */
gboolean
gst_collect_pads_is_active (GstCollectPads * pads, GstPad * pad)
{
  g_return_val_if_fail (pads != NULL, FALSE);
  g_return_val_if_fail (GST_IS_COLLECT_PADS (pads), FALSE);
  g_return_val_if_fail (pad != NULL, FALSE);
  g_return_val_if_fail (GST_IS_PAD (pad), FALSE);

  g_warning ("gst_collect_pads_is_active() is not implemented");

  return FALSE;
}

/**
 * gst_collect_pads_collect:
 * @pads: the collectspads to use
 *
 * Collect data on all pads. This function is usually called
 * from a #GstTask function in an element.
 *
 * This function is currently not implemented.
 *
 * MT safe.
 *
 * Returns: #GstFlowReturn of the operation.
 */
GstFlowReturn
gst_collect_pads_collect (GstCollectPads * pads)
{
  g_return_val_if_fail (pads != NULL, GST_FLOW_ERROR);
  g_return_val_if_fail (GST_IS_COLLECT_PADS (pads), GST_FLOW_ERROR);

  g_warning ("gst_collect_pads_collect() is not implemented");

  return GST_FLOW_NOT_SUPPORTED;
}

/**
 * gst_collect_pads_collect_range:
 * @pads: the collectspads to use
 * @offset: the offset to collect
 * @length: the length to collect
 *
 * Collect data with @offset and @length on all pads. This function
 * is typically called in the getrange function of an element.
 *
 * This function is currently not implemented.
 *
 * MT safe.
 *
 * Returns: #GstFlowReturn of the operation.
 */
GstFlowReturn
gst_collect_pads_collect_range (GstCollectPads * pads, guint64 offset,
    guint length)
{
  g_return_val_if_fail (pads != NULL, GST_FLOW_ERROR);
  g_return_val_if_fail (GST_IS_COLLECT_PADS (pads), GST_FLOW_ERROR);

  g_warning ("gst_collect_pads_collect_range() is not implemented");

  return GST_FLOW_NOT_SUPPORTED;
}

static gboolean
gst_collect_pads_is_flushing (GstCollectPads * pads)
{
  GSList *walk = NULL;
  gboolean res = TRUE;

  GST_COLLECT_PADS_PAD_LOCK (pads);

  /* Ensure pads->data state */
  gst_collect_pads_check_pads_unlocked (pads);

  GST_DEBUG ("Getting flushing state (pads:%p, pads->data:%p)",
      pads, pads->data);

  for (walk = pads->data; walk; walk = g_slist_next (walk)) {
    GstCollectData *cdata = walk->data;

    GST_DEBUG_OBJECT (cdata->pad, "flushing:%d", cdata->abidata.ABI.flushing);

    if (cdata->abidata.ABI.flushing) {
      goto done;
    }
  }

  res = FALSE;
done:
  GST_COLLECT_PADS_PAD_UNLOCK (pads);
  return res;
}

/* FIXME, I think this function is used to work around bad behaviour
 * of elements that add pads to themselves without activating them.
 *
 * Must be called with PAD_LOCK.
 */
static void
gst_collect_pads_set_flushing_unlocked (GstCollectPads * pads,
    gboolean flushing)
{
  GSList *walk = NULL;

  GST_DEBUG ("Setting flushing (%d)", flushing);

  /* Update the pads flushing flag */
  for (walk = pads->data; walk; walk = g_slist_next (walk)) {
    GstCollectData *cdata = walk->data;

    if (GST_IS_PAD (cdata->pad)) {
      GST_OBJECT_LOCK (cdata->pad);
      if (flushing)
        GST_PAD_SET_FLUSHING (cdata->pad);
      else
        GST_PAD_UNSET_FLUSHING (cdata->pad);
      cdata->abidata.ABI.flushing = flushing;
      gst_collect_pads_clear (pads, cdata);
      GST_OBJECT_UNLOCK (cdata->pad);
    }
  }
  /* Setting the pads to flushing means that we changed the values which
   * are 'protected' by the cookie. We therefore update it to force a
   * recalculation of the current pad status. */
  pads->abidata.ABI.pad_cookie++;
}

/**
 * gst_collect_pads_set_flushing:
 * @pads: the collectspads to use
 * @flushing: desired state of the pads
 *
 * Change the flushing state of all the pads in the collection. No pad
 * is able to accept anymore data when @flushing is %TRUE. Calling this
 * function with @flushing %FALSE makes @pads accept data again.
 *
 * MT safe.
 *
 * Since: 0.10.7.
 */
void
gst_collect_pads_set_flushing (GstCollectPads * pads, gboolean flushing)
{
  g_return_if_fail (pads != NULL);
  g_return_if_fail (GST_IS_COLLECT_PADS (pads));

  GST_COLLECT_PADS_PAD_LOCK (pads);
  /* Ensure pads->data state */
  gst_collect_pads_check_pads_unlocked (pads);
  gst_collect_pads_set_flushing_unlocked (pads, flushing);
  GST_COLLECT_PADS_PAD_UNLOCK (pads);
}

/**
 * gst_collect_pads_start:
 * @pads: the collectspads to use
 *
 * Starts the processing of data in the collect_pads.
 *
 * MT safe.
 */
void
gst_collect_pads_start (GstCollectPads * pads)
{
  GSList *collected;

  g_return_if_fail (pads != NULL);
  g_return_if_fail (GST_IS_COLLECT_PADS (pads));

  GST_DEBUG_OBJECT (pads, "starting collect pads");

  /* make sure stop and collect cannot be called anymore */
  GST_OBJECT_LOCK (pads);

  /* make pads streamable */
  GST_COLLECT_PADS_PAD_LOCK (pads);

  /* loop over the master pad list and reset the segment */
  collected = pads->abidata.ABI.pad_list;
  for (; collected; collected = g_slist_next (collected)) {
    GstCollectData *data;

    data = collected->data;
    gst_segment_init (&data->segment, GST_FORMAT_UNDEFINED);
  }

  gst_collect_pads_set_flushing_unlocked (pads, FALSE);

  /* Start collect pads */
  pads->started = TRUE;
  GST_COLLECT_PADS_PAD_UNLOCK (pads);
  GST_OBJECT_UNLOCK (pads);
}

/**
 * gst_collect_pads_stop:
 * @pads: the collectspads to use
 *
 * Stops the processing of data in the collect_pads. this function
 * will also unblock any blocking operations.
 *
 * MT safe.
 */
void
gst_collect_pads_stop (GstCollectPads * pads)
{
  GSList *collected;

  g_return_if_fail (pads != NULL);
  g_return_if_fail (GST_IS_COLLECT_PADS (pads));

  GST_DEBUG_OBJECT (pads, "stopping collect pads");

  /* make sure collect and start cannot be called anymore */
  GST_OBJECT_LOCK (pads);

  /* make pads not accept data anymore */
  GST_COLLECT_PADS_PAD_LOCK (pads);
  gst_collect_pads_set_flushing_unlocked (pads, TRUE);

  /* Stop collect pads */
  pads->started = FALSE;
  pads->eospads = 0;
  pads->queuedpads = 0;

  /* loop over the master pad list and flush buffers */
  collected = pads->abidata.ABI.pad_list;
  for (; collected; collected = g_slist_next (collected)) {
    GstCollectData *data;
    GstBuffer **buffer_p;

    data = collected->data;
    if (data->buffer) {
      buffer_p = &data->buffer;
      gst_buffer_replace (buffer_p, NULL);
      data->pos = 0;
    }
    data->abidata.ABI.eos = FALSE;
  }

  GST_COLLECT_PADS_PAD_UNLOCK (pads);
  /* Wake them up so they can end the chain functions. */
  GST_COLLECT_PADS_BROADCAST (pads);

  GST_OBJECT_UNLOCK (pads);
}

/**
 * gst_collect_pads_peek:
 * @pads: the collectspads to peek
 * @data: the data to use
 *
 * Peek at the buffer currently queued in @data. This function
 * should be called with the @pads LOCK held, such as in the callback
 * handler.
 *
 * MT safe.
 *
 * Returns: (transfer full): The buffer in @data or NULL if no buffer is queued.
 *  should unref the buffer after usage.
 */
GstBuffer *
gst_collect_pads_peek (GstCollectPads * pads, GstCollectData * data)
{
  GstBuffer *result;

  g_return_val_if_fail (pads != NULL, NULL);
  g_return_val_if_fail (GST_IS_COLLECT_PADS (pads), NULL);
  g_return_val_if_fail (data != NULL, NULL);

  if ((result = data->buffer))
    gst_buffer_ref (result);

  GST_DEBUG ("Peeking at pad %s:%s: buffer=%p",
      GST_DEBUG_PAD_NAME (data->pad), result);

  return result;
}

/**
 * gst_collect_pads_pop:
 * @pads: the collectspads to pop
 * @data: the data to use
 *
 * Pop the buffer currently queued in @data. This function
 * should be called with the @pads LOCK held, such as in the callback
 * handler.
 *
 * Free-function: gst_buffer_unref
 *
 * MT safe.
 *
 * Returns: (transfer full): The buffer in @data or NULL if no buffer was
 *   queued. You should unref the buffer after usage.
 */
GstBuffer *
gst_collect_pads_pop (GstCollectPads * pads, GstCollectData * data)
{
  GstBuffer *result;

  g_return_val_if_fail (pads != NULL, NULL);
  g_return_val_if_fail (GST_IS_COLLECT_PADS (pads), NULL);
  g_return_val_if_fail (data != NULL, NULL);

  if ((result = data->buffer)) {
    data->buffer = NULL;
    data->pos = 0;
    /* one less pad with queued data now */
    pads->queuedpads--;
  }

  GST_COLLECT_PADS_BROADCAST (pads);

  GST_DEBUG ("Pop buffer on pad %s:%s: buffer=%p",
      GST_DEBUG_PAD_NAME (data->pad), result);

  return result;
}

/* pop and unref the currently queued buffer, should e called with the LOCK
 * helt. */
static void
gst_collect_pads_clear (GstCollectPads * pads, GstCollectData * data)
{
  GstBuffer *buf;

  if ((buf = gst_collect_pads_pop (pads, data)))
    gst_buffer_unref (buf);
}

/**
 * gst_collect_pads_available:
 * @pads: the collectspads to query
 *
 * Query how much bytes can be read from each queued buffer. This means
 * that the result of this call is the maximum number of bytes that can
 * be read from each of the pads.
 *
 * This function should be called with @pads LOCK held, such as
 * in the callback.
 *
 * MT safe.
 *
 * Returns: The maximum number of bytes queued on all pads. This function
 * returns 0 if a pad has no queued buffer.
 */
/* FIXME, we can do this in the _chain functions */
guint
gst_collect_pads_available (GstCollectPads * pads)
{
  GSList *collected;
  guint result = G_MAXUINT;

  g_return_val_if_fail (pads != NULL, 0);
  g_return_val_if_fail (GST_IS_COLLECT_PADS (pads), 0);

  for (collected = pads->data; collected; collected = g_slist_next (collected)) {
    GstCollectData *pdata;
    GstBuffer *buffer;
    gint size;

    pdata = (GstCollectData *) collected->data;

    /* ignore pad with EOS */
    if (G_UNLIKELY (pdata->abidata.ABI.eos)) {
      GST_DEBUG ("pad %s:%s is EOS", GST_DEBUG_PAD_NAME (pdata->pad));
      continue;
    }

    /* an empty buffer without EOS is weird when we get here.. */
    if (G_UNLIKELY ((buffer = pdata->buffer) == NULL)) {
      GST_WARNING ("pad %s:%s has no buffer", GST_DEBUG_PAD_NAME (pdata->pad));
      goto not_filled;
    }

    /* this is the size left of the buffer */
    size = GST_BUFFER_SIZE (buffer) - pdata->pos;
    GST_DEBUG ("pad %s:%s has %d bytes left",
        GST_DEBUG_PAD_NAME (pdata->pad), size);

    /* need to return the min of all available data */
    if (size < result)
      result = size;
  }
  /* nothing changed, all must be EOS then, return 0 */
  if (G_UNLIKELY (result == G_MAXUINT))
    result = 0;

  return result;

not_filled:
  {
    return 0;
  }
}

/**
 * gst_collect_pads_read:
 * @pads: the collectspads to query
 * @data: the data to use
 * @bytes: (out) (transfer none) (array length=size): a pointer to a byte array
 * @size: the number of bytes to read
 *
 * Get a pointer in @bytes where @size bytes can be read from the
 * given pad @data.
 *
 * This function should be called with @pads LOCK held, such as
 * in the callback.
 *
 * MT safe.
 *
 * Returns: The number of bytes available for consumption in the
 * memory pointed to by @bytes. This can be less than @size and
 * is 0 if the pad is end-of-stream.
 */
guint
gst_collect_pads_read (GstCollectPads * pads, GstCollectData * data,
    guint8 ** bytes, guint size)
{
  guint readsize;
  GstBuffer *buffer;

  g_return_val_if_fail (pads != NULL, 0);
  g_return_val_if_fail (GST_IS_COLLECT_PADS (pads), 0);
  g_return_val_if_fail (data != NULL, 0);
  g_return_val_if_fail (bytes != NULL, 0);

  /* no buffer, must be EOS */
  if ((buffer = data->buffer) == NULL)
    return 0;

  readsize = MIN (size, GST_BUFFER_SIZE (buffer) - data->pos);

  *bytes = GST_BUFFER_DATA (buffer) + data->pos;

  return readsize;
}

/**
 * gst_collect_pads_read_buffer:
 * @pads: the collectspads to query
 * @data: the data to use
 * @size: the number of bytes to read
 *
 * Get a buffer of @size bytes from the given pad @data.
 *
 * This function should be called with @pads LOCK held, such as in the callback.
 *
 * Free-function: gst_buffer_unref
 *
 * Returns: (transfer full): a #GstBuffer. The size of the buffer can be less
 *     that requested. A return of NULL signals that the pad is end-of-stream.
 *     Unref the buffer with gst_buffer_unref() after use.
 *
 * MT safe.
 *
 * Since: 0.10.18
 */
GstBuffer *
gst_collect_pads_read_buffer (GstCollectPads * pads, GstCollectData * data,
    guint size)
{
  guint readsize, bufsize;
  GstBuffer *buffer;

  g_return_val_if_fail (pads != NULL, NULL);
  g_return_val_if_fail (GST_IS_COLLECT_PADS (pads), NULL);
  g_return_val_if_fail (data != NULL, NULL);

  /* no buffer, must be EOS */
  if ((buffer = data->buffer) == NULL)
    return NULL;

  bufsize = GST_BUFFER_SIZE (buffer);

  readsize = MIN (size, bufsize - data->pos);

  if (data->pos == 0 && readsize == bufsize)
    return gst_buffer_ref (buffer);
  else
    return gst_buffer_create_sub (buffer, data->pos, readsize);
}

/**
 * gst_collect_pads_take_buffer:
 * @pads: the collectspads to query
 * @data: the data to use
 * @size: the number of bytes to read
 *
 * Get a buffer of @size bytes from the given pad @data. Flushes the amount
 * of read bytes.
 *
 * This function should be called with @pads LOCK held, such as in the callback.
 *
 * Free-function: gst_buffer_unref
 *
 * MT safe.
 *
 * Returns: (transfer full): a #GstBuffer. The size of the buffer can be less
 *     that requested. A return of NULL signals that the pad is end-of-stream.
 *     Unref the buffer after use.
 *
 * Since: 0.10.18
 */
GstBuffer *
gst_collect_pads_take_buffer (GstCollectPads * pads, GstCollectData * data,
    guint size)
{
  GstBuffer *buffer = gst_collect_pads_read_buffer (pads, data, size);

  if (buffer) {
    gst_collect_pads_flush (pads, data, GST_BUFFER_SIZE (buffer));
  }
  return buffer;
}

/**
 * gst_collect_pads_flush:
 * @pads: the collectspads to query
 * @data: the data to use
 * @size: the number of bytes to flush
 *
 * Flush @size bytes from the pad @data.
 *
 * This function should be called with @pads LOCK held, such as
 * in the callback.
 *
 * MT safe.
 *
 * Returns: The number of bytes flushed. This can be less than @size and
 * is 0 if the pad was end-of-stream.
 */
guint
gst_collect_pads_flush (GstCollectPads * pads, GstCollectData * data,
    guint size)
{
  guint flushsize;
  GstBuffer *buffer;

  g_return_val_if_fail (pads != NULL, 0);
  g_return_val_if_fail (GST_IS_COLLECT_PADS (pads), 0);
  g_return_val_if_fail (data != NULL, 0);

  /* no buffer, must be EOS */
  if ((buffer = data->buffer) == NULL)
    return 0;

  /* this is what we can flush at max */
  flushsize = MIN (size, GST_BUFFER_SIZE (buffer) - data->pos);

  data->pos += size;

  GST_LOG_OBJECT (pads, "Flushing %d bytes, requested %u", flushsize, size);

  if (data->pos >= GST_BUFFER_SIZE (buffer))
    /* _clear will also reset data->pos to 0 */
    gst_collect_pads_clear (pads, data);

  return flushsize;
}

/* see if pads were added or removed and update our stats. Any pad
 * added after releasing the PAD_LOCK will get collected in the next
 * round.
 *
 * We can do a quick check by checking the cookies, that get changed
 * whenever the pad list is updated.
 *
 * Must be called with LOCK.
 */
static void
gst_collect_pads_check_pads_unlocked (GstCollectPads * pads)
{
  GST_DEBUG ("stored cookie : %d, used_cookie:%d",
      pads->abidata.ABI.pad_cookie, pads->cookie);
  if (G_UNLIKELY (pads->abidata.ABI.pad_cookie != pads->cookie)) {
    GSList *collected;

    /* clear list and stats */
    g_slist_foreach (pads->data, (GFunc) unref_data, NULL);
    g_slist_free (pads->data);
    pads->data = NULL;
    pads->numpads = 0;
    pads->queuedpads = 0;
    pads->eospads = 0;

    /* loop over the master pad list */
    collected = pads->abidata.ABI.pad_list;
    for (; collected; collected = g_slist_next (collected)) {
      GstCollectData *data;

      /* update the stats */
      pads->numpads++;
      data = collected->data;

      if (G_LIKELY (!data->abidata.ABI.flushing)) {
        if (data->buffer)
          pads->queuedpads++;
        if (data->abidata.ABI.eos)
          pads->eospads++;
      }

      /* add to the list of pads to collect */
      ref_data (data);
      pads->data = g_slist_prepend (pads->data, data);
    }
    /* and update the cookie */
    pads->cookie = pads->abidata.ABI.pad_cookie;
  }
}

static inline void
gst_collect_pads_check_pads (GstCollectPads * pads)
{
  /* the master list and cookie are protected with the PAD_LOCK */
  GST_COLLECT_PADS_PAD_LOCK (pads);
  gst_collect_pads_check_pads_unlocked (pads);
  GST_COLLECT_PADS_PAD_UNLOCK (pads);
}

/* checks if all the pads are collected and call the collectfunction
 *
 * Should be called with LOCK.
 *
 * Returns: The #GstFlowReturn of collection.
 */
static GstFlowReturn
gst_collect_pads_check_collected (GstCollectPads * pads)
{
  GstFlowReturn flow_ret = GST_FLOW_OK;

  g_return_val_if_fail (GST_IS_COLLECT_PADS (pads), GST_FLOW_ERROR);
  g_return_val_if_fail (pads->func != NULL, GST_FLOW_NOT_SUPPORTED);

  /* check for new pads, update stats etc.. */
  gst_collect_pads_check_pads (pads);

  if (G_UNLIKELY (pads->eospads == pads->numpads)) {
    /* If all our pads are EOS just collect once to let the element
     * do its final EOS handling. */
    GST_DEBUG ("All active pads (%d) are EOS, calling %s",
        pads->numpads, GST_DEBUG_FUNCPTR_NAME (pads->func));
    flow_ret = pads->func (pads, pads->user_data);
  } else {
    gboolean collected = FALSE;

    /* We call the collected function as long as our condition matches.
     * FIXME: should we error out if the collect function did not pop anything ?
     * we can get a busy loop here if the element does not pop from the collect
     * function
     */
    while (((pads->queuedpads + pads->eospads) >= pads->numpads)) {
      GST_DEBUG ("All active pads (%d + %d >= %d) have data, calling %s",
          pads->queuedpads, pads->eospads, pads->numpads,
          GST_DEBUG_FUNCPTR_NAME (pads->func));
      flow_ret = pads->func (pads, pads->user_data);
      collected = TRUE;

      /* break on error */
      if (flow_ret != GST_FLOW_OK)
        break;
      /* Don't keep looping after telling the element EOS or flushing */
      if (pads->queuedpads == 0)
        break;
    }
    if (!collected)
      GST_DEBUG ("Not all active pads (%d) have data, continuing",
          pads->numpads);
  }
  return flow_ret;
}

static gboolean
gst_collect_pads_event (GstPad * pad, GstEvent * event)
{
  gboolean res;
  GstCollectData *data;
  GstCollectPads *pads;

  /* some magic to get the managing collect_pads */
  GST_OBJECT_LOCK (pad);
  data = (GstCollectData *) gst_pad_get_element_private (pad);
  if (G_UNLIKELY (data == NULL))
    goto pad_removed;
  ref_data (data);
  GST_OBJECT_UNLOCK (pad);

  res = TRUE;

  pads = data->collect;

  GST_DEBUG ("Got %s event on pad %s:%s", GST_EVENT_TYPE_NAME (event),
      GST_DEBUG_PAD_NAME (data->pad));

  switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_FLUSH_START:
    {
      /* forward event to unblock check_collected */
      gst_pad_event_default (pad, event);

      /* now unblock the chain function.
       * no cond per pad, so they all unblock,
       * non-flushing block again */
      GST_OBJECT_LOCK (pads);
      data->abidata.ABI.flushing = TRUE;
      gst_collect_pads_clear (pads, data);
      GST_OBJECT_UNLOCK (pads);

      /* event already cleaned up by forwarding */
      goto done;
    }
    case GST_EVENT_FLUSH_STOP:
    {
      /* flush the 1 buffer queue */
      GST_OBJECT_LOCK (pads);
      data->abidata.ABI.flushing = FALSE;
      gst_collect_pads_clear (pads, data);
      /* we need new segment info after the flush */
      gst_segment_init (&data->segment, GST_FORMAT_UNDEFINED);
      data->abidata.ABI.new_segment = FALSE;
      /* if the pad was EOS, remove the EOS flag and
       * decrement the number of eospads */
      if (G_UNLIKELY (data->abidata.ABI.eos == TRUE)) {
        pads->eospads--;
        data->abidata.ABI.eos = FALSE;
      }

      if (!gst_collect_pads_is_flushing (pads)) {
        /* forward event if all pads are no longer flushing */
        GST_DEBUG ("No more pads are flushing, forwarding FLUSH_STOP");
        GST_OBJECT_UNLOCK (pads);
        goto forward;
      }
      gst_event_unref (event);
      GST_OBJECT_UNLOCK (pads);
      goto done;
    }
    case GST_EVENT_EOS:
    {
      GST_OBJECT_LOCK (pads);
      /* if the pad was not EOS, make it EOS and so we
       * have one more eospad */
      if (G_LIKELY (data->abidata.ABI.eos == FALSE)) {
        data->abidata.ABI.eos = TRUE;
        pads->eospads++;
      }
      /* check if we need collecting anything, we ignore the
       * result. */
      gst_collect_pads_check_collected (pads);
      GST_OBJECT_UNLOCK (pads);

      /* We eat this event, element should do something
       * in the collected callback. */
      gst_event_unref (event);
      goto done;
    }
    case GST_EVENT_NEWSEGMENT:
    {
      gint64 start, stop, time;
      gdouble rate, arate;
      GstFormat format;
      gboolean update;

      gst_event_parse_new_segment_full (event, &update, &rate, &arate, &format,
          &start, &stop, &time);

      GST_DEBUG_OBJECT (data->pad, "got newsegment, start %" GST_TIME_FORMAT
          ", stop %" GST_TIME_FORMAT, GST_TIME_ARGS (start),
          GST_TIME_ARGS (stop));

      gst_segment_set_newsegment_full (&data->segment, update, rate, arate,
          format, start, stop, time);

      data->abidata.ABI.new_segment = TRUE;

      /* we must not forward this event since multiple segments will be
       * accumulated and this is certainly not what we want. */
      gst_event_unref (event);
      /* FIXME: collect-pads based elements need to create their own newsegment
       * event (and only one really)
       * (a) make the segment part of the GstCollectData structure of each pad,
       * so you can just check that once you have a buffer queued on that pad,
       * (b) you can override a pad's event function with your own,
       * catch the newsegment event and then pass it on to the original
       * gstcollectpads event function
       * (that's what avimux does for something IIRC)
       * see #340060
       */
      goto done;
    }
    default:
      /* forward other events */
      goto forward;
  }

forward:
  GST_DEBUG_OBJECT (pads, "forward unhandled event: %s",
      GST_EVENT_TYPE_NAME (event));
  res = gst_pad_event_default (pad, event);

done:
  unref_data (data);
  return res;

  /* ERRORS */
pad_removed:
  {
    GST_DEBUG ("%s got removed from collectpads", GST_OBJECT_NAME (pad));
    GST_OBJECT_UNLOCK (pad);
    return FALSE;
  }
}

/* For each buffer we receive we check if our collected condition is reached
 * and if so we call the collected function. When this is done we check if
 * data has been unqueued. If data is still queued we wait holding the stream
 * lock to make sure no EOS event can happen while we are ready to be
 * collected
 */
static GstFlowReturn
gst_collect_pads_chain (GstPad * pad, GstBuffer * buffer)
{
  GstCollectData *data;
  GstCollectPads *pads;
  GstCollectPadsPrivate *priv;
  GstFlowReturn ret;

  GST_DEBUG ("Got buffer for pad %s:%s", GST_DEBUG_PAD_NAME (pad));

  /* some magic to get the managing collect_pads */
  GST_OBJECT_LOCK (pad);
  data = (GstCollectData *) gst_pad_get_element_private (pad);
  if (G_UNLIKELY (data == NULL))
    goto no_data;
  ref_data (data);
  GST_OBJECT_UNLOCK (pad);

  pads = data->collect;
  priv = pads->abidata.ABI.priv;

  GST_OBJECT_LOCK (pads);
  /* if not started, bail out */
  if (G_UNLIKELY (!pads->started))
    goto not_started;
  /* check if this pad is flushing */
  if (G_UNLIKELY (data->abidata.ABI.flushing))
    goto flushing;
  /* pad was EOS, we can refuse this data */
  if (G_UNLIKELY (data->abidata.ABI.eos))
    goto unexpected;

  /* see if we need to clip */
  if (priv->clipfunc) {
    buffer = priv->clipfunc (pads, data, buffer, priv->clipfunc_user_data);

    if (G_UNLIKELY (buffer == NULL))
      goto clipped;
  }

  GST_DEBUG ("Queuing buffer %p for pad %s:%s", buffer,
      GST_DEBUG_PAD_NAME (pad));

  /* One more pad has data queued */
  pads->queuedpads++;
  /* take ownership of the buffer */
  if (data->buffer)
    gst_buffer_unref (data->buffer);
  data->buffer = buffer;
  buffer = NULL;

  /* update segment last position if in TIME */
  if (G_LIKELY (data->segment.format == GST_FORMAT_TIME)) {
    GstClockTime timestamp = GST_BUFFER_TIMESTAMP (data->buffer);

    if (GST_CLOCK_TIME_IS_VALID (timestamp))
      gst_segment_set_last_stop (&data->segment, GST_FORMAT_TIME, timestamp);
  }

  /* While we have data queued on this pad try to collect stuff */
  do {
    GST_DEBUG ("Pad %s:%s checking", GST_DEBUG_PAD_NAME (pad));
    /* Check if our collected condition is matched and call the collected function
     * if it is */
    ret = gst_collect_pads_check_collected (pads);
    /* when an error occurs, we want to report this back to the caller ASAP
     * without having to block if the buffer was not popped */
    if (G_UNLIKELY (ret != GST_FLOW_OK))
      goto error;

    /* data was consumed, we can exit and accept new data */
    if (data->buffer == NULL)
      break;

    /* Check if we got removed in the mean time, FIXME, this is racy.
     * Between this check and the _WAIT, the pad could be removed which will
     * makes us hang in the _WAIT. */
    GST_OBJECT_LOCK (pad);
    if (G_UNLIKELY (gst_pad_get_element_private (pad) == NULL))
      goto pad_removed;
    GST_OBJECT_UNLOCK (pad);

    GST_DEBUG ("Pad %s:%s has a buffer queued, waiting",
        GST_DEBUG_PAD_NAME (pad));

    /* wait to be collected, this must happen from another thread triggered
     * by the _chain function of another pad. We release the lock so we
     * can get stopped or flushed as well. We can however not get EOS
     * because we still hold the STREAM_LOCK.
     */
    GST_COLLECT_PADS_WAIT (pads);

    GST_DEBUG ("Pad %s:%s resuming", GST_DEBUG_PAD_NAME (pad));

    /* after a signal, we could be stopped */
    if (G_UNLIKELY (!pads->started))
      goto not_started;
    /* check if this pad is flushing */
    if (G_UNLIKELY (data->abidata.ABI.flushing))
      goto flushing;
  }
  while (data->buffer != NULL);

unlock_done:
  GST_DEBUG ("Pad %s:%s done", GST_DEBUG_PAD_NAME (pad));
  GST_OBJECT_UNLOCK (pads);
  unref_data (data);
  if (buffer)
    gst_buffer_unref (buffer);
  return ret;

pad_removed:
  {
    GST_WARNING ("%s got removed from collectpads", GST_OBJECT_NAME (pad));
    GST_OBJECT_UNLOCK (pad);
    ret = GST_FLOW_NOT_LINKED;
    goto unlock_done;
  }
  /* ERRORS */
no_data:
  {
    GST_DEBUG ("%s got removed from collectpads", GST_OBJECT_NAME (pad));
    GST_OBJECT_UNLOCK (pad);
    gst_buffer_unref (buffer);
    return GST_FLOW_NOT_LINKED;
  }
not_started:
  {
    GST_DEBUG ("not started");
    gst_collect_pads_clear (pads, data);
    ret = GST_FLOW_WRONG_STATE;
    goto unlock_done;
  }
flushing:
  {
    GST_DEBUG ("pad %s:%s is flushing", GST_DEBUG_PAD_NAME (pad));
    gst_collect_pads_clear (pads, data);
    ret = GST_FLOW_WRONG_STATE;
    goto unlock_done;
  }
unexpected:
  {
    /* we should not post an error for this, just inform upstream that
     * we don't expect anything anymore */
    GST_DEBUG ("pad %s:%s is eos", GST_DEBUG_PAD_NAME (pad));
    ret = GST_FLOW_UNEXPECTED;
    goto unlock_done;
  }
clipped:
  {
    GST_DEBUG ("clipped buffer on pad %s:%s", GST_DEBUG_PAD_NAME (pad));
    ret = GST_FLOW_OK;
    goto unlock_done;
  }
error:
  {
    /* we print the error, the element should post a reasonable error
     * message for fatal errors */
    GST_DEBUG ("collect failed, reason %d (%s)", ret, gst_flow_get_name (ret));
    gst_collect_pads_clear (pads, data);
    goto unlock_done;
  }
}
