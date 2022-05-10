/* GStreamer
 *
 * Copyright (C) 2014 Samsung Electronics. All rights reserved.
 *   Author: Thiago Santos <ts.santos@sisa.samsung.com>
 *
 * gstflowcombiner.c: utility to combine multiple flow returns into a single one
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
 * SECTION:gstflowcombiner
 * @title: GstFlowCombiner
 * @short_description: Utility to combine multiple flow returns into one
 *
 * Utility struct to help handling #GstFlowReturn combination. Useful for
 * #GstElement<!-- -->s that have multiple source pads and need to combine
 * the different #GstFlowReturn for those pads.
 *
 * #GstFlowCombiner works by using the last #GstFlowReturn for all #GstPad
 * it has in its list and computes the combined return value and provides
 * it to the caller.
 *
 * To add a new pad to the #GstFlowCombiner use gst_flow_combiner_add_pad().
 * The new #GstPad is stored with a default value of %GST_FLOW_OK.
 *
 * In case you want a #GstPad to be removed, use gst_flow_combiner_remove_pad().
 *
 * Please be aware that this struct isn't thread safe as its designed to be
 *  used by demuxers, those usually will have a single thread operating it.
 *
 * These functions will take refs on the passed #GstPad<!-- -->s.
 *
 * Aside from reducing the user's code size, the main advantage of using this
 * helper struct is to follow the standard rules for #GstFlowReturn combination.
 * These rules are:
 *
 * * %GST_FLOW_EOS: only if all returns are EOS too
 * * %GST_FLOW_NOT_LINKED: only if all returns are NOT_LINKED too
 * * %GST_FLOW_ERROR or below: if at least one returns an error return
 * * %GST_FLOW_NOT_NEGOTIATED: if at least one returns a not-negotiated return
 * * %GST_FLOW_FLUSHING: if at least one returns flushing
 * * %GST_FLOW_OK: otherwise
 *
 * %GST_FLOW_ERROR or below, GST_FLOW_NOT_NEGOTIATED and GST_FLOW_FLUSHING are
 * returned immediately from the gst_flow_combiner_update_flow() function.
 *
 * Since: 1.4
 */
#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>
#include "gstflowcombiner.h"

struct _GstFlowCombiner
{
  GQueue pads;

  GstFlowReturn last_ret;
  gint ref_count;
};

GST_DEBUG_CATEGORY_STATIC (flowcombiner_dbg);
#define GST_CAT_DEFAULT flowcombiner_dbg

G_DEFINE_BOXED_TYPE_WITH_CODE (GstFlowCombiner, gst_flow_combiner,
    (GBoxedCopyFunc) gst_flow_combiner_ref,
    (GBoxedFreeFunc) gst_flow_combiner_unref,
    GST_DEBUG_CATEGORY_INIT (flowcombiner_dbg, "flowcombiner", 0,
        "Flow Combiner"));

/**
 * gst_flow_combiner_new:
 *
 * Creates a new #GstFlowCombiner, use gst_flow_combiner_free() to free it.
 *
 * Returns: A new #GstFlowCombiner
 * Since: 1.4
 */
GstFlowCombiner *
gst_flow_combiner_new (void)
{
  GstFlowCombiner *combiner = g_slice_new (GstFlowCombiner);

  g_queue_init (&combiner->pads);
  combiner->last_ret = GST_FLOW_OK;
  g_atomic_int_set (&combiner->ref_count, 1);

  /* Make sure debug category is initialised */
  gst_flow_combiner_get_type ();

  return combiner;
}

/**
 * gst_flow_combiner_free:
 * @combiner: the #GstFlowCombiner to free
 *
 * Frees a #GstFlowCombiner struct and all its internal data.
 *
 * Since: 1.4
 */
void
gst_flow_combiner_free (GstFlowCombiner * combiner)
{
  gst_flow_combiner_unref (combiner);
}

/**
 * gst_flow_combiner_ref:
 * @combiner: the #GstFlowCombiner to add a reference to.
 *
 * Increments the reference count on the #GstFlowCombiner.
 *
 * Returns: the #GstFlowCombiner.
 *
 * Since: 1.12.1
 */
GstFlowCombiner *
gst_flow_combiner_ref (GstFlowCombiner * combiner)
{
  g_return_val_if_fail (combiner != NULL, NULL);

  g_atomic_int_inc (&combiner->ref_count);

  return combiner;
}

/**
 * gst_flow_combiner_unref:
 * @combiner: the #GstFlowCombiner to unreference.
 *
 * Decrements the reference count on the #GstFlowCombiner.
 *
 * Since: 1.12.1
 */
void
gst_flow_combiner_unref (GstFlowCombiner * combiner)
{
  g_return_if_fail (combiner != NULL);
  g_return_if_fail (combiner->ref_count > 0);

  if (g_atomic_int_dec_and_test (&combiner->ref_count)) {
    GstPad *pad;

    while ((pad = g_queue_pop_head (&combiner->pads)))
      gst_object_unref (pad);

    g_slice_free (GstFlowCombiner, combiner);
  }
}

/**
 * gst_flow_combiner_clear:
 * @combiner: the #GstFlowCombiner to clear
 *
 * Removes all pads from a #GstFlowCombiner and resets it to its initial state.
 *
 * Since: 1.6
 */
void
gst_flow_combiner_clear (GstFlowCombiner * combiner)
{
  GstPad *pad;

  g_return_if_fail (combiner != NULL);

  GST_DEBUG ("%p clearing", combiner);

  while ((pad = g_queue_pop_head (&combiner->pads)))
    gst_object_unref (pad);
  combiner->last_ret = GST_FLOW_OK;
}

/**
 * gst_flow_combiner_reset:
 * @combiner: the #GstFlowCombiner to clear
 *
 * Reset flow combiner and all pads to their initial state without removing pads.
 *
 * Since: 1.6
 */
void
gst_flow_combiner_reset (GstFlowCombiner * combiner)
{
  GList *iter;

  g_return_if_fail (combiner != NULL);

  GST_DEBUG ("%p reset flow returns", combiner);

  for (iter = combiner->pads.head; iter; iter = iter->next) {
    GST_PAD_LAST_FLOW_RETURN (iter->data) = GST_FLOW_OK;
  }

  combiner->last_ret = GST_FLOW_OK;
}

static GstFlowReturn
gst_flow_combiner_get_flow (GstFlowCombiner * combiner)
{
  GstFlowReturn cret = GST_FLOW_OK;
  gboolean all_eos = TRUE;
  gboolean all_notlinked = TRUE;
  GList *iter;

  GST_DEBUG ("%p Combining flow returns", combiner);

  for (iter = combiner->pads.head; iter; iter = iter->next) {
    GstFlowReturn fret = GST_PAD_LAST_FLOW_RETURN (iter->data);

    GST_TRACE ("%p pad %" GST_PTR_FORMAT " has flow return of %s (%d)",
        combiner, iter->data, gst_flow_get_name (fret), fret);

    if (fret <= GST_FLOW_NOT_NEGOTIATED || fret == GST_FLOW_FLUSHING) {
      GST_DEBUG ("%p Error flow return found, returning", combiner);
      cret = fret;
      goto done;
    }

    if (fret != GST_FLOW_NOT_LINKED) {
      all_notlinked = FALSE;
      if (fret != GST_FLOW_EOS)
        all_eos = FALSE;
    }
  }
  if (all_notlinked)
    cret = GST_FLOW_NOT_LINKED;
  else if (all_eos)
    cret = GST_FLOW_EOS;

done:
  GST_DEBUG ("%p Combined flow return: %s (%d)", combiner,
      gst_flow_get_name (cret), cret);
  return cret;
}

/**
 * gst_flow_combiner_update_flow:
 * @combiner: the #GstFlowCombiner
 * @fret: the latest #GstFlowReturn received for a pad in this #GstFlowCombiner
 *
 * Computes the combined flow return for the pads in it.
 *
 * The #GstFlowReturn parameter should be the last flow return update for a pad
 * in this #GstFlowCombiner. It will use this value to be able to shortcut some
 * combinations and avoid looking over all pads again. e.g. The last combined
 * return is the same as the latest obtained #GstFlowReturn.
 *
 * Returns: The combined #GstFlowReturn
 * Since: 1.4
 */
GstFlowReturn
gst_flow_combiner_update_flow (GstFlowCombiner * combiner, GstFlowReturn fret)
{
  GstFlowReturn ret;

  g_return_val_if_fail (combiner != NULL, GST_FLOW_ERROR);

  GST_DEBUG ("%p updating combiner with flow %s (%d)", combiner,
      gst_flow_get_name (fret), fret);

  if (combiner->last_ret == fret) {
    return fret;
  }

  if (fret <= GST_FLOW_NOT_NEGOTIATED || fret == GST_FLOW_FLUSHING
      || !combiner->pads.head) {
    ret = fret;
  } else {
    ret = gst_flow_combiner_get_flow (combiner);
  }
  combiner->last_ret = ret;
  return ret;
}

/**
 * gst_flow_combiner_update_pad_flow:
 * @combiner: the #GstFlowCombiner
 * @pad: the #GstPad whose #GstFlowReturn to update
 * @fret: the latest #GstFlowReturn received for a pad in this #GstFlowCombiner
 *
 * Sets the provided pad's last flow return to provided value and computes
 * the combined flow return for the pads in it.
 *
 * The #GstFlowReturn parameter should be the last flow return update for a pad
 * in this #GstFlowCombiner. It will use this value to be able to shortcut some
 * combinations and avoid looking over all pads again. e.g. The last combined
 * return is the same as the latest obtained #GstFlowReturn.
 *
 * Returns: The combined #GstFlowReturn
 * Since: 1.6
 */
GstFlowReturn
gst_flow_combiner_update_pad_flow (GstFlowCombiner * combiner, GstPad * pad,
    GstFlowReturn fret)
{
  g_return_val_if_fail (pad != NULL, GST_FLOW_ERROR);

  GST_PAD_LAST_FLOW_RETURN (pad) = fret;

  return gst_flow_combiner_update_flow (combiner, fret);
}

/**
 * gst_flow_combiner_add_pad:
 * @combiner: the #GstFlowCombiner
 * @pad: (transfer none): the #GstPad that is being added
 *
 * Adds a new #GstPad to the #GstFlowCombiner.
 *
 * Since: 1.4
 */
void
gst_flow_combiner_add_pad (GstFlowCombiner * combiner, GstPad * pad)
{
  g_return_if_fail (combiner != NULL);
  g_return_if_fail (pad != NULL);

  g_queue_push_head (&combiner->pads, gst_object_ref (pad));
}

/**
 * gst_flow_combiner_remove_pad:
 * @combiner: the #GstFlowCombiner
 * @pad: (transfer none): the #GstPad to remove
 *
 * Removes a #GstPad from the #GstFlowCombiner.
 *
 * Since: 1.4
 */
void
gst_flow_combiner_remove_pad (GstFlowCombiner * combiner, GstPad * pad)
{
  g_return_if_fail (combiner != NULL);
  g_return_if_fail (pad != NULL);

  if (g_queue_remove (&combiner->pads, pad))
    gst_object_unref (pad);
}
