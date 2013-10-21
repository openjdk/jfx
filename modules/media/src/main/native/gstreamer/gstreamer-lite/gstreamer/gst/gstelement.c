/* GStreamer
 * Copyright (C) 1999,2000 Erik Walthinsen <omega@cse.ogi.edu>
 *                    2004 Wim Taymans <wim@fluendo.com>
 *
 * gstelement.c: The base element, all elements derive from this
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
 * SECTION:gstelement
 * @short_description: Abstract base class for all pipeline elements
 * @see_also: #GstElementFactory, #GstPad
 *
 * GstElement is the abstract base class needed to construct an element that
 * can be used in a GStreamer pipeline. Please refer to the plugin writers
 * guide for more information on creating #GstElement subclasses.
 *
 * The name of a #GstElement can be get with gst_element_get_name() and set with
 * gst_element_set_name().  For speed, GST_ELEMENT_NAME() can be used in the
 * core when using the appropriate locking. Do not use this in plug-ins or
 * applications in order to retain ABI compatibility.
 *
 * All elements have pads (of the type #GstPad).  These pads link to pads on
 * other elements.  #GstBuffer flow between these linked pads.
 * A #GstElement has a #GList of #GstPad structures for all their input (or sink)
 * and output (or source) pads.
 * Core and plug-in writers can add and remove pads with gst_element_add_pad()
 * and gst_element_remove_pad().
 *
 * A pad of an element can be retrieved by name with gst_element_get_pad().
 * An iterator of all pads can be retrieved with gst_element_iterate_pads().
 *
 * Elements can be linked through their pads.
 * If the link is straightforward, use the gst_element_link()
 * convenience function to link two elements, or gst_element_link_many()
 * for more elements in a row.
 * Use gst_element_link_filtered() to link two elements constrained by
 * a specified set of #GstCaps.
 * For finer control, use gst_element_link_pads() and
 * gst_element_link_pads_filtered() to specify the pads to link on
 * each element by name.
 *
 * Each element has a state (see #GstState).  You can get and set the state
 * of an element with gst_element_get_state() and gst_element_set_state().
 * Setting a state triggers a #GstStateChange. To get a string representation
 * of a #GstState, use gst_element_state_get_name().
 *
 * You can get and set a #GstClock on an element using gst_element_get_clock()
 * and gst_element_set_clock().
 * Some elements can provide a clock for the pipeline if
 * gst_element_provides_clock() returns %TRUE. With the
 * gst_element_provide_clock() method one can retrieve the clock provided by
 * such an element.
 * Not all elements require a clock to operate correctly. If
 * gst_element_requires_clock() returns %TRUE, a clock should be set on the
 * element with gst_element_set_clock().
 *
 * Note that clock slection and distribution is normally handled by the
 * toplevel #GstPipeline so the clock functions are only to be used in very
 * specific situations.
 *
 * Last reviewed on 2009-05-29 (0.10.24)
 */

#include "gst_private.h"
#include <glib.h>
#include <stdarg.h>
#include <gobject/gvaluecollector.h>

#include "gstelement.h"
#include "gstelementdetails.h"
#include "gstenumtypes.h"
#include "gstbus.h"
#include "gstmarshal.h"
#include "gsterror.h"
#include "gstevent.h"
#include "gstutils.h"
#include "gstinfo.h"
#include "gstvalue.h"
#include "gst-i18n-lib.h"

/* Element signals and args */
enum
{
  PAD_ADDED,
  PAD_REMOVED,
  NO_MORE_PADS,
  /* add more above */
  LAST_SIGNAL
};

enum
{
  ARG_0
      /* FILL ME */
};

#ifdef GST_DISABLE_DEPRECATED
#if !defined(GST_DISABLE_LOADSAVE) && !defined(GST_REMOVE_DEPRECATED)
#include <libxml/parser.h>
xmlNodePtr gst_object_save_thyself (const GstObject * object,
    xmlNodePtr parent);
GstObject *gst_object_load_thyself (xmlNodePtr parent);
void gst_pad_load_and_link (xmlNodePtr self, GstObject * parent);
#endif
#endif

static void gst_element_class_init (GstElementClass * klass);
static void gst_element_init (GstElement * element);
static void gst_element_base_class_init (gpointer g_class);
static void gst_element_base_class_finalize (gpointer g_class);

static void gst_element_dispose (GObject * object);
static void gst_element_finalize (GObject * object);

static GstStateChangeReturn gst_element_change_state_func (GstElement * element,
    GstStateChange transition);
static GstStateChangeReturn gst_element_get_state_func (GstElement * element,
    GstState * state, GstState * pending, GstClockTime timeout);
static GstStateChangeReturn gst_element_set_state_func (GstElement * element,
    GstState state);
static void gst_element_set_bus_func (GstElement * element, GstBus * bus);

static gboolean gst_element_default_send_event (GstElement * element,
    GstEvent * event);
static gboolean gst_element_default_query (GstElement * element,
    GstQuery * query);

static GstPadTemplate
    * gst_element_class_get_request_pad_template (GstElementClass *
    element_class, const gchar * name);

#if !defined(GST_DISABLE_LOADSAVE) && !defined(GST_REMOVE_DEPRECATED)
static xmlNodePtr gst_element_save_thyself (GstObject * object,
    xmlNodePtr parent);
static void gst_element_restore_thyself (GstObject * parent, xmlNodePtr self);
#endif

static GstObjectClass *parent_class = NULL;
static guint gst_element_signals[LAST_SIGNAL] = { 0 };

/* this is used in gstelementfactory.c:gst_element_register() */
GQuark _gst_elementclass_factory = 0;

GType
gst_element_get_type (void)
{
  static volatile gsize gst_element_type = 0;

  if (g_once_init_enter (&gst_element_type)) {
    GType _type;
    static const GTypeInfo element_info = {
      sizeof (GstElementClass),
      gst_element_base_class_init,
      gst_element_base_class_finalize,
      (GClassInitFunc) gst_element_class_init,
      NULL,
      NULL,
      sizeof (GstElement),
      0,
      (GInstanceInitFunc) gst_element_init,
      NULL
    };

    _type = g_type_register_static (GST_TYPE_OBJECT, "GstElement",
        &element_info, G_TYPE_FLAG_ABSTRACT);

    _gst_elementclass_factory =
        g_quark_from_static_string ("GST_ELEMENTCLASS_FACTORY");
    g_once_init_leave (&gst_element_type, _type);
  }
  return gst_element_type;
}

static void
gst_element_class_init (GstElementClass * klass)
{
  GObjectClass *gobject_class;
  GstObjectClass *gstobject_class;

  gobject_class = (GObjectClass *) klass;
  gstobject_class = (GstObjectClass *) klass;

  parent_class = g_type_class_peek_parent (klass);

  /**
   * GstElement::pad-added:
   * @gstelement: the object which received the signal
   * @new_pad: the pad that has been added
   *
   * a new #GstPad has been added to the element. Note that this signal will
   * usually be emitted from the context of the streaming thread. Also keep in
   * mind that if you add new elements to the pipeline in the signal handler
   * you will need to set them to the desired target state with
   * gst_element_set_state() or gst_element_sync_state_with_parent().
   */
  gst_element_signals[PAD_ADDED] =
      g_signal_new ("pad-added", G_TYPE_FROM_CLASS (klass), G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (GstElementClass, pad_added), NULL, NULL,
      gst_marshal_VOID__OBJECT, G_TYPE_NONE, 1, GST_TYPE_PAD);
  /**
   * GstElement::pad-removed:
   * @gstelement: the object which received the signal
   * @old_pad: the pad that has been removed
   *
   * a #GstPad has been removed from the element
   */
  gst_element_signals[PAD_REMOVED] =
      g_signal_new ("pad-removed", G_TYPE_FROM_CLASS (klass), G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (GstElementClass, pad_removed), NULL, NULL,
      gst_marshal_VOID__OBJECT, G_TYPE_NONE, 1, GST_TYPE_PAD);
  /**
   * GstElement::no-more-pads:
   * @gstelement: the object which received the signal
   *
   * This signals that the element will not generate more dynamic pads.
   * Note that this signal will usually be emitted from the context of
   * the streaming thread.
   */
  gst_element_signals[NO_MORE_PADS] =
      g_signal_new ("no-more-pads", G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST, G_STRUCT_OFFSET (GstElementClass, no_more_pads), NULL,
      NULL, gst_marshal_VOID__VOID, G_TYPE_NONE, 0);

  gobject_class->dispose = gst_element_dispose;
  gobject_class->finalize = gst_element_finalize;

#if !defined(GST_DISABLE_LOADSAVE) && !defined(GST_REMOVE_DEPRECATED)
  gstobject_class->save_thyself =
      ((gpointer (*)(GstObject * object,
              gpointer self)) * GST_DEBUG_FUNCPTR (gst_element_save_thyself));
  gstobject_class->restore_thyself =
      ((void (*)(GstObject * object,
              gpointer self)) *GST_DEBUG_FUNCPTR (gst_element_restore_thyself));
#endif

  klass->change_state = GST_DEBUG_FUNCPTR (gst_element_change_state_func);
  klass->set_state = GST_DEBUG_FUNCPTR (gst_element_set_state_func);
  klass->get_state = GST_DEBUG_FUNCPTR (gst_element_get_state_func);
  klass->set_bus = GST_DEBUG_FUNCPTR (gst_element_set_bus_func);
  klass->query = GST_DEBUG_FUNCPTR (gst_element_default_query);
  klass->send_event = GST_DEBUG_FUNCPTR (gst_element_default_send_event);
  klass->numpadtemplates = 0;

  klass->elementfactory = NULL;
}

static void
gst_element_base_class_init (gpointer g_class)
{
  GstElementClass *element_class = GST_ELEMENT_CLASS (g_class);

  /* FIXME 0.11: Copy the element details and instead of clearing the
   * pad template list copy the list and increase the refcount of
   * the pad templates by one.
   *
   * This will make it possible to add pad templates and set element
   * details in the class_init functions and is the real GObject way
   * of doing things.
   * See http://bugzilla.gnome.org/show_bug.cgi?id=491501
   */
  memset (&element_class->details, 0, sizeof (GstElementDetails));
  element_class->meta_data = NULL;
  element_class->padtemplates = NULL;

  /* set the factory, see gst_element_register() */
  element_class->elementfactory =
      g_type_get_qdata (G_TYPE_FROM_CLASS (element_class),
      _gst_elementclass_factory);
  GST_DEBUG ("type %s : factory %p", G_OBJECT_CLASS_NAME (element_class),
      element_class->elementfactory);
}

static void
gst_element_base_class_finalize (gpointer g_class)
{
  GstElementClass *klass = GST_ELEMENT_CLASS (g_class);

  g_list_foreach (klass->padtemplates, (GFunc) gst_object_unref, NULL);
  g_list_free (klass->padtemplates);
  __gst_element_details_clear (&klass->details);
  if (klass->meta_data) {
    gst_structure_free (klass->meta_data);
    klass->meta_data = NULL;
  }
}

static void
gst_element_init (GstElement * element)
{
  GST_STATE (element) = GST_STATE_NULL;
  GST_STATE_TARGET (element) = GST_STATE_NULL;
  GST_STATE_NEXT (element) = GST_STATE_VOID_PENDING;
  GST_STATE_PENDING (element) = GST_STATE_VOID_PENDING;
  GST_STATE_RETURN (element) = GST_STATE_CHANGE_SUCCESS;

  /* FIXME 0.11: Store this directly in the instance struct */
  element->state_lock = g_slice_new (GStaticRecMutex);
  g_static_rec_mutex_init (element->state_lock);
  element->state_cond = g_cond_new ();
}

/**
 * gst_element_release_request_pad:
 * @element: a #GstElement to release the request pad of.
 * @pad: the #GstPad to release.
 *
 * Makes the element free the previously requested pad as obtained
 * with gst_element_get_request_pad().
 *
 * This does not unref the pad. If the pad was created by using
 * gst_element_get_request_pad(), gst_element_release_request_pad() needs to be
 * followed by gst_object_unref() to free the @pad.
 *
 * MT safe.
 */
void
gst_element_release_request_pad (GstElement * element, GstPad * pad)
{
  GstElementClass *oclass;

  g_return_if_fail (GST_IS_ELEMENT (element));
  g_return_if_fail (GST_IS_PAD (pad));

  oclass = GST_ELEMENT_GET_CLASS (element);

  /* if the element implements a custom release function we call that, else we
   * simply remove the pad from the element */
  if (oclass->release_pad)
    (oclass->release_pad) (element, pad);
  else
    gst_element_remove_pad (element, pad);
}

/**
 * gst_element_requires_clock:
 * @element: a #GstElement to query
 *
 * Query if the element requires a clock.
 *
 * Returns: %TRUE if the element requires a clock
 *
 * MT safe.
 */
gboolean
gst_element_requires_clock (GstElement * element)
{
  gboolean result;

  g_return_val_if_fail (GST_IS_ELEMENT (element), FALSE);

  result = (GST_ELEMENT_GET_CLASS (element)->set_clock != NULL);

  return result;
}

/**
 * gst_element_provides_clock:
 * @element: a #GstElement to query
 *
 * Query if the element provides a clock. A #GstClock provided by an
 * element can be used as the global #GstClock for the pipeline.
 * An element that can provide a clock is only required to do so in the PAUSED
 * state, this means when it is fully negotiated and has allocated the resources
 * to operate the clock.
 *
 * Returns: %TRUE if the element provides a clock
 *
 * MT safe.
 */
gboolean
gst_element_provides_clock (GstElement * element)
{
  gboolean result;

  g_return_val_if_fail (GST_IS_ELEMENT (element), FALSE);

  result = (GST_ELEMENT_GET_CLASS (element)->provide_clock != NULL);

  return result;
}

/**
 * gst_element_provide_clock:
 * @element: a #GstElement to query
 *
 * Get the clock provided by the given element.
 * <note>An element is only required to provide a clock in the PAUSED
 * state. Some elements can provide a clock in other states.</note>
 *
 * Returns: (transfer full): the GstClock provided by the element or %NULL
 * if no clock could be provided.  Unref after usage.
 *
 * MT safe.
 */
GstClock *
gst_element_provide_clock (GstElement * element)
{
  GstClock *result = NULL;
  GstElementClass *oclass;

  g_return_val_if_fail (GST_IS_ELEMENT (element), NULL);

  oclass = GST_ELEMENT_GET_CLASS (element);

  if (oclass->provide_clock)
    result = oclass->provide_clock (element);

  return result;
}

/**
 * gst_element_set_clock:
 * @element: a #GstElement to set the clock for.
 * @clock: the #GstClock to set for the element.
 *
 * Sets the clock for the element. This function increases the
 * refcount on the clock. Any previously set clock on the object
 * is unreffed.
 *
 * Returns: %TRUE if the element accepted the clock. An element can refuse a
 * clock when it, for example, is not able to slave its internal clock to the
 * @clock or when it requires a specific clock to operate.
 *
 * MT safe.
 */
gboolean
gst_element_set_clock (GstElement * element, GstClock * clock)
{
  GstElementClass *oclass;
  gboolean res = TRUE;
  GstClock **clock_p;

  g_return_val_if_fail (GST_IS_ELEMENT (element), FALSE);
  g_return_val_if_fail (clock == NULL || GST_IS_CLOCK (clock), FALSE);

  oclass = GST_ELEMENT_GET_CLASS (element);

  GST_CAT_DEBUG_OBJECT (GST_CAT_CLOCK, element, "setting clock %p", clock);

  if (oclass->set_clock)
    res = oclass->set_clock (element, clock);

  if (res) {
    /* only update the clock pointer if the element accepted the clock */
    GST_OBJECT_LOCK (element);
    clock_p = &element->clock;
    gst_object_replace ((GstObject **) clock_p, (GstObject *) clock);
    GST_OBJECT_UNLOCK (element);
  }
  return res;
}

/**
 * gst_element_get_clock:
 * @element: a #GstElement to get the clock of.
 *
 * Gets the currently configured clock of the element. This is the clock as was
 * last set with gst_element_set_clock().
 *
 * Returns: (transfer full): the #GstClock of the element. unref after usage.
 *
 * MT safe.
 */
GstClock *
gst_element_get_clock (GstElement * element)
{
  GstClock *result;

  g_return_val_if_fail (GST_IS_ELEMENT (element), NULL);

  GST_OBJECT_LOCK (element);
  if ((result = element->clock))
    gst_object_ref (result);
  GST_OBJECT_UNLOCK (element);

  return result;
}

/**
 * gst_element_set_base_time:
 * @element: a #GstElement.
 * @time: the base time to set.
 *
 * Set the base time of an element. See gst_element_get_base_time().
 *
 * MT safe.
 */
void
gst_element_set_base_time (GstElement * element, GstClockTime time)
{
  GstClockTime old;

  g_return_if_fail (GST_IS_ELEMENT (element));

  GST_OBJECT_LOCK (element);
  old = element->base_time;
  element->base_time = time;
  GST_OBJECT_UNLOCK (element);

  GST_CAT_DEBUG_OBJECT (GST_CAT_CLOCK, element,
      "set base_time=%" GST_TIME_FORMAT ", old %" GST_TIME_FORMAT,
      GST_TIME_ARGS (time), GST_TIME_ARGS (old));
}

/**
 * gst_element_get_base_time:
 * @element: a #GstElement.
 *
 * Returns the base time of the element. The base time is the
 * absolute time of the clock when this element was last put to
 * PLAYING. Subtracting the base time from the clock time gives
 * the running time of the element.
 *
 * Returns: the base time of the element.
 *
 * MT safe.
 */
GstClockTime
gst_element_get_base_time (GstElement * element)
{
  GstClockTime result;

  g_return_val_if_fail (GST_IS_ELEMENT (element), GST_CLOCK_TIME_NONE);

  GST_OBJECT_LOCK (element);
  result = element->base_time;
  GST_OBJECT_UNLOCK (element);

  return result;
}

/**
 * gst_element_set_start_time:
 * @element: a #GstElement.
 * @time: the base time to set.
 *
 * Set the start time of an element. The start time of the element is the
 * running time of the element when it last went to the PAUSED state. In READY
 * or after a flushing seek, it is set to 0.
 *
 * Toplevel elements like #GstPipeline will manage the start_time and
 * base_time on its children. Setting the start_time to #GST_CLOCK_TIME_NONE
 * on such a toplevel element will disable the distribution of the base_time to
 * the children and can be useful if the application manages the base_time
 * itself, for example if you want to synchronize capture from multiple
 * pipelines, and you can also ensure that the pipelines have the same clock.
 *
 * MT safe.
 *
 * Since: 0.10.24
 */
void
gst_element_set_start_time (GstElement * element, GstClockTime time)
{
  GstClockTime old;

  g_return_if_fail (GST_IS_ELEMENT (element));

  GST_OBJECT_LOCK (element);
  old = GST_ELEMENT_START_TIME (element);
  GST_ELEMENT_START_TIME (element) = time;
  GST_OBJECT_UNLOCK (element);

  GST_CAT_DEBUG_OBJECT (GST_CAT_CLOCK, element,
      "set start_time=%" GST_TIME_FORMAT ", old %" GST_TIME_FORMAT,
      GST_TIME_ARGS (time), GST_TIME_ARGS (old));
}

/**
 * gst_element_get_start_time:
 * @element: a #GstElement.
 *
 * Returns the start time of the element. The start time is the
 * running time of the clock when this element was last put to PAUSED.
 *
 * Usually the start_time is managed by a toplevel element such as
 * #GstPipeline.
 *
 * MT safe.
 *
 * Returns: the start time of the element.
 *
 * Since: 0.10.24
 */
GstClockTime
gst_element_get_start_time (GstElement * element)
{
  GstClockTime result;

  g_return_val_if_fail (GST_IS_ELEMENT (element), GST_CLOCK_TIME_NONE);

  GST_OBJECT_LOCK (element);
  result = GST_ELEMENT_START_TIME (element);
  GST_OBJECT_UNLOCK (element);

  return result;
}

/**
 * gst_element_is_indexable:
 * @element: a #GstElement.
 *
 * Queries if the element can be indexed.
 *
 * Returns: TRUE if the element can be indexed.
 *
 * MT safe.
 */
gboolean
gst_element_is_indexable (GstElement * element)
{
  gboolean result;

  g_return_val_if_fail (GST_IS_ELEMENT (element), FALSE);

  result = (GST_ELEMENT_GET_CLASS (element)->set_index != NULL);

  return result;
}

/**
 * gst_element_set_index:
 * @element: a #GstElement.
 * @index: (transfer none): a #GstIndex.
 *
 * Set @index on the element. The refcount of the index
 * will be increased, any previously set index is unreffed.
 *
 * MT safe.
 */
void
gst_element_set_index (GstElement * element, GstIndex * index)
{
  GstElementClass *oclass;

  g_return_if_fail (GST_IS_ELEMENT (element));
  g_return_if_fail (index == NULL || GST_IS_INDEX (index));

  oclass = GST_ELEMENT_GET_CLASS (element);

  if (oclass->set_index)
    oclass->set_index (element, index);
}

/**
 * gst_element_get_index:
 * @element: a #GstElement.
 *
 * Gets the index from the element.
 *
 * Returns: (transfer full): a #GstIndex or %NULL when no index was set on the
 * element. unref after usage.
 *
 * MT safe.
 */
GstIndex *
gst_element_get_index (GstElement * element)
{
  GstElementClass *oclass;
  GstIndex *result = NULL;

  g_return_val_if_fail (GST_IS_ELEMENT (element), NULL);

  oclass = GST_ELEMENT_GET_CLASS (element);

  if (oclass->get_index)
    result = oclass->get_index (element);

  return result;
}

/**
 * gst_element_add_pad:
 * @element: a #GstElement to add the pad to.
 * @pad: (transfer full): the #GstPad to add to the element.
 *
 * Adds a pad (link point) to @element. @pad's parent will be set to @element;
 * see gst_object_set_parent() for refcounting information.
 *
 * Pads are not automatically activated so elements should perform the needed
 * steps to activate the pad in case this pad is added in the PAUSED or PLAYING
 * state. See gst_pad_set_active() for more information about activating pads.
 *
 * The pad and the element should be unlocked when calling this function.
 *
 * This function will emit the #GstElement::pad-added signal on the element.
 *
 * Returns: %TRUE if the pad could be added. This function can fail when
 * a pad with the same name already existed or the pad already had another
 * parent.
 *
 * MT safe.
 */
gboolean
gst_element_add_pad (GstElement * element, GstPad * pad)
{
  gchar *pad_name;
  gboolean flushing;

  g_return_val_if_fail (GST_IS_ELEMENT (element), FALSE);
  g_return_val_if_fail (GST_IS_PAD (pad), FALSE);

  /* locking pad to look at the name */
  GST_OBJECT_LOCK (pad);
  pad_name = g_strdup (GST_PAD_NAME (pad));
  GST_CAT_INFO_OBJECT (GST_CAT_ELEMENT_PADS, element, "adding pad '%s'",
      GST_STR_NULL (pad_name));
  flushing = GST_PAD_IS_FLUSHING (pad);
  GST_OBJECT_UNLOCK (pad);

  /* then check to see if there's already a pad by that name here */
  GST_OBJECT_LOCK (element);
  if (G_UNLIKELY (!gst_object_check_uniqueness (element->pads, pad_name)))
    goto name_exists;

  /* try to set the pad's parent */
  if (G_UNLIKELY (!gst_object_set_parent (GST_OBJECT_CAST (pad),
              GST_OBJECT_CAST (element))))
    goto had_parent;

  /* check for flushing pads */
  if (flushing && (GST_STATE (element) > GST_STATE_READY ||
          GST_STATE_NEXT (element) == GST_STATE_PAUSED)) {
    g_warning ("adding flushing pad '%s' to running element '%s', you need to "
        "use gst_pad_set_active(pad,TRUE) before adding it.",
        GST_STR_NULL (pad_name), GST_ELEMENT_NAME (element));
    /* unset flushing */
    GST_OBJECT_LOCK (pad);
    GST_PAD_UNSET_FLUSHING (pad);
    GST_OBJECT_UNLOCK (pad);
  }

  g_free (pad_name);

  /* add it to the list */
  switch (gst_pad_get_direction (pad)) {
    case GST_PAD_SRC:
      element->srcpads = g_list_prepend (element->srcpads, pad);
      element->numsrcpads++;
      break;
    case GST_PAD_SINK:
      element->sinkpads = g_list_prepend (element->sinkpads, pad);
      element->numsinkpads++;
      break;
    default:
      goto no_direction;
  }
  element->pads = g_list_prepend (element->pads, pad);
  element->numpads++;
  element->pads_cookie++;
  GST_OBJECT_UNLOCK (element);

  /* emit the PAD_ADDED signal */
  g_signal_emit (element, gst_element_signals[PAD_ADDED], 0, pad);

  return TRUE;

  /* ERROR cases */
name_exists:
  {
    g_critical ("Padname %s is not unique in element %s, not adding",
        pad_name, GST_ELEMENT_NAME (element));
    GST_OBJECT_UNLOCK (element);
    g_free (pad_name);
    return FALSE;
  }
had_parent:
  {
    g_critical
        ("Pad %s already has parent when trying to add to element %s",
        pad_name, GST_ELEMENT_NAME (element));
    GST_OBJECT_UNLOCK (element);
    g_free (pad_name);
    return FALSE;
  }
no_direction:
  {
    GST_OBJECT_LOCK (pad);
    g_critical
        ("Trying to add pad %s to element %s, but it has no direction",
        GST_OBJECT_NAME (pad), GST_ELEMENT_NAME (element));
    GST_OBJECT_UNLOCK (pad);
    GST_OBJECT_UNLOCK (element);
    return FALSE;
  }
}

/**
 * gst_element_remove_pad:
 * @element: a #GstElement to remove pad from.
 * @pad: (transfer none): the #GstPad to remove from the element.
 *
 * Removes @pad from @element. @pad will be destroyed if it has not been
 * referenced elsewhere using gst_object_unparent().
 *
 * This function is used by plugin developers and should not be used
 * by applications. Pads that were dynamically requested from elements
 * with gst_element_get_request_pad() should be released with the
 * gst_element_release_request_pad() function instead.
 *
 * Pads are not automatically deactivated so elements should perform the needed
 * steps to deactivate the pad in case this pad is removed in the PAUSED or
 * PLAYING state. See gst_pad_set_active() for more information about
 * deactivating pads.
 *
 * The pad and the element should be unlocked when calling this function.
 *
 * This function will emit the #GstElement::pad-removed signal on the element.
 *
 * Returns: %TRUE if the pad could be removed. Can return %FALSE if the
 * pad does not belong to the provided element.
 *
 * MT safe.
 */
gboolean
gst_element_remove_pad (GstElement * element, GstPad * pad)
{
  GstPad *peer;

  g_return_val_if_fail (GST_IS_ELEMENT (element), FALSE);
  g_return_val_if_fail (GST_IS_PAD (pad), FALSE);

  /* locking pad to look at the name and parent */
  GST_OBJECT_LOCK (pad);
  GST_CAT_INFO_OBJECT (GST_CAT_ELEMENT_PADS, element, "removing pad '%s'",
      GST_STR_NULL (GST_PAD_NAME (pad)));

  if (G_UNLIKELY (GST_PAD_PARENT (pad) != element))
    goto not_our_pad;
  GST_OBJECT_UNLOCK (pad);

  /* unlink */
  if ((peer = gst_pad_get_peer (pad))) {
    /* window for MT unsafeness, someone else could unlink here
     * and then we call unlink with wrong pads. The unlink
     * function would catch this and safely return failed. */
    if (GST_PAD_IS_SRC (pad))
      gst_pad_unlink (pad, peer);
    else
      gst_pad_unlink (peer, pad);

    gst_object_unref (peer);
  }

  GST_OBJECT_LOCK (element);
  /* remove it from the list */
  switch (gst_pad_get_direction (pad)) {
    case GST_PAD_SRC:
      element->srcpads = g_list_remove (element->srcpads, pad);
      element->numsrcpads--;
      break;
    case GST_PAD_SINK:
      element->sinkpads = g_list_remove (element->sinkpads, pad);
      element->numsinkpads--;
      break;
    default:
      g_critical ("Removing pad without direction???");
      break;
  }
  element->pads = g_list_remove (element->pads, pad);
  element->numpads--;
  element->pads_cookie++;
  GST_OBJECT_UNLOCK (element);

  /* emit the PAD_REMOVED signal before unparenting and losing the last ref. */
  g_signal_emit (element, gst_element_signals[PAD_REMOVED], 0, pad);

  gst_object_unparent (GST_OBJECT_CAST (pad));

  return TRUE;

  /* ERRORS */
not_our_pad:
  {
    /* FIXME, locking order? */
    GST_OBJECT_LOCK (element);
    g_critical ("Padname %s:%s does not belong to element %s when removing",
        GST_DEBUG_PAD_NAME (pad), GST_ELEMENT_NAME (element));
    GST_OBJECT_UNLOCK (element);
    GST_OBJECT_UNLOCK (pad);
    return FALSE;
  }
}

/**
 * gst_element_no_more_pads:
 * @element: a #GstElement
 *
 * Use this function to signal that the element does not expect any more pads
 * to show up in the current pipeline. This function should be called whenever
 * pads have been added by the element itself. Elements with #GST_PAD_SOMETIMES
 * pad templates use this in combination with autopluggers to figure out that
 * the element is done initializing its pads.
 *
 * This function emits the #GstElement::no-more-pads signal.
 *
 * MT safe.
 */
void
gst_element_no_more_pads (GstElement * element)
{
  g_return_if_fail (GST_IS_ELEMENT (element));

  g_signal_emit (element, gst_element_signals[NO_MORE_PADS], 0);
}

static gint
pad_compare_name (GstPad * pad1, const gchar * name)
{
  gint result;

  GST_OBJECT_LOCK (pad1);
  result = strcmp (GST_PAD_NAME (pad1), name);
  GST_OBJECT_UNLOCK (pad1);

  return result;
}

/**
 * gst_element_get_static_pad:
 * @element: a #GstElement to find a static pad of.
 * @name: the name of the static #GstPad to retrieve.
 *
 * Retrieves a pad from @element by name. This version only retrieves
 * already-existing (i.e. 'static') pads.
 *
 * Returns: (transfer full): the requested #GstPad if found, otherwise %NULL.
 *     unref after usage.
 *
 * MT safe.
 */
GstPad *
gst_element_get_static_pad (GstElement * element, const gchar * name)
{
  GList *find;
  GstPad *result = NULL;

  g_return_val_if_fail (GST_IS_ELEMENT (element), NULL);
  g_return_val_if_fail (name != NULL, NULL);

  GST_OBJECT_LOCK (element);
  find =
      g_list_find_custom (element->pads, name, (GCompareFunc) pad_compare_name);
  if (find) {
    result = GST_PAD_CAST (find->data);
    gst_object_ref (result);
  }

  if (result == NULL) {
    GST_CAT_INFO (GST_CAT_ELEMENT_PADS, "no such pad '%s' in element \"%s\"",
        name, GST_ELEMENT_NAME (element));
  } else {
    GST_CAT_INFO (GST_CAT_ELEMENT_PADS, "found pad %s:%s",
        GST_ELEMENT_NAME (element), name);
  }
  GST_OBJECT_UNLOCK (element);

  return result;
}

static GstPad *
_gst_element_request_pad (GstElement * element, GstPadTemplate * templ,
    const gchar * name, const GstCaps * caps)
{
  GstPad *newpad = NULL;
  GstElementClass *oclass;

  oclass = GST_ELEMENT_GET_CLASS (element);

#ifndef G_DISABLE_CHECKS
  /* Some sanity checking here */
  if (name) {
    GstPad *pad;

    /* Is this the template name? */
    if (strstr (name, "%") || !strchr (templ->name_template, '%')) {
      g_return_val_if_fail (strcmp (name, templ->name_template) == 0, NULL);
    } else {
      const gchar *str, *data;
      gchar *endptr;

      /* Otherwise check if it's a valid name for the name template */
      str = strchr (templ->name_template, '%');
      g_return_val_if_fail (str != NULL, NULL);
      g_return_val_if_fail (strncmp (templ->name_template, name,
              str - templ->name_template) == 0, NULL);
      g_return_val_if_fail (strlen (name) > str - templ->name_template, NULL);

      data = name + (str - templ->name_template);

      /* Can either be %s or %d or %u, do sanity checking for %d */
      if (*(str + 1) == 'd') {
        gint64 tmp;

        /* it's an int */
        tmp = g_ascii_strtoll (data, &endptr, 10);
        g_return_val_if_fail (tmp >= G_MININT && tmp <= G_MAXINT
            && *endptr == '\0', NULL);
      } else if (*(str + 1) == 'u') {
        guint64 tmp;

        /* it's an int */
        tmp = g_ascii_strtoull (data, &endptr, 10);
        g_return_val_if_fail (tmp <= G_MAXUINT && *endptr == '\0', NULL);
      }
    }

    pad = gst_element_get_static_pad (element, name);
    if (pad) {
      gst_object_unref (pad);
      /* FIXME 0.11: Change this to g_return_val_if_fail() */
      g_critical ("Element %s already has a pad named %s, the behaviour of "
          " gst_element_get_request_pad() for existing pads is undefined!",
          GST_ELEMENT_NAME (element), name);
    }
  }
#endif

  if (oclass->request_new_pad_full)
    newpad = (oclass->request_new_pad_full) (element, templ, name, caps);
  else if (oclass->request_new_pad)
    newpad = (oclass->request_new_pad) (element, templ, name);

  if (newpad)
    gst_object_ref (newpad);

  return newpad;
}

/**
 * gst_element_get_request_pad:
 * @element: a #GstElement to find a request pad of.
 * @name: the name of the request #GstPad to retrieve.
 *
 * Retrieves a pad from the element by name. This version only retrieves
 * request pads. The pad should be released with
 * gst_element_release_request_pad().
 *
 * This method is slow and will be deprecated in the future. New code should
 * use gst_element_request_pad() with the requested template.
 *
 * Returns: (transfer full): requested #GstPad if found, otherwise %NULL.
 *     Release after usage.
 */
GstPad *
gst_element_get_request_pad (GstElement * element, const gchar * name)
{
  GstPadTemplate *templ = NULL;
  GstPad *pad;
  const gchar *req_name = NULL;
  gboolean templ_found = FALSE;
  GList *list;
  const gchar *data;
  gchar *str, *endptr = NULL;
  GstElementClass *class;

  g_return_val_if_fail (GST_IS_ELEMENT (element), NULL);
  g_return_val_if_fail (name != NULL, NULL);

  class = GST_ELEMENT_GET_CLASS (element);

  /* if the name contains a %, we assume it's the complete template name. Get
   * the template and try to get a pad */
  if (strstr (name, "%")) {
    templ = gst_element_class_get_request_pad_template (class, name);
    req_name = NULL;
    if (templ)
      templ_found = TRUE;
  } else {
    /* there is no % in the name, try to find a matching template */
    list = class->padtemplates;
    while (!templ_found && list) {
      templ = (GstPadTemplate *) list->data;
      if (templ->presence == GST_PAD_REQUEST) {
        GST_CAT_DEBUG (GST_CAT_PADS, "comparing %s to %s", name,
            templ->name_template);
        /* see if we find an exact match */
        if (strcmp (name, templ->name_template) == 0) {
          templ_found = TRUE;
          req_name = name;
          break;
        }
        /* Because of sanity checks in gst_pad_template_new(), we know that %s
           and %d and %u, occurring at the end of the name_template, are the only
           possibilities. */
        else if ((str = strchr (templ->name_template, '%'))
            && strncmp (templ->name_template, name,
                str - templ->name_template) == 0
            && strlen (name) > str - templ->name_template) {
          data = name + (str - templ->name_template);
          if (*(str + 1) == 'd') {
            glong tmp;

            /* it's an int */
            tmp = strtol (data, &endptr, 10);
            if (tmp != G_MINLONG && tmp != G_MAXLONG && endptr &&
                *endptr == '\0') {
              templ_found = TRUE;
              req_name = name;
              break;
            }
          } else if (*(str + 1) == 'u') {
            gulong tmp;

            /* it's an int */
            tmp = strtoul (data, &endptr, 10);
            if (tmp != G_MAXULONG && endptr && *endptr == '\0') {
              templ_found = TRUE;
              req_name = name;
              break;
            }
          } else {
            /* it's a string */
            templ_found = TRUE;
            req_name = name;
            break;
          }
        }
      }
      list = list->next;
    }
  }

  if (!templ_found)
    return NULL;

  pad = _gst_element_request_pad (element, templ, req_name, NULL);

  return pad;
}

/**
 * gst_element_request_pad:
 * @element: a #GstElement to find a request pad of.
 * @templ: a #GstPadTemplate of which we want a pad of.
 * @name: (transfer none) (allow-none): the name of the request #GstPad
 * to retrieve. Can be %NULL.
 * @caps: (transfer none) (allow-none): the caps of the pad we want to
 * request. Can be %NULL.
 *
 * Retrieves a request pad from the element according to the provided template.
 *
 * If the @caps are specified and the element implements thew new
 * request_new_pad_full virtual method, the element will use them to select
 * which pad to create.
 *
 * The pad should be released with gst_element_release_request_pad().
 *
 * Returns: (transfer full): requested #GstPad if found, otherwise %NULL.
 *     Release after usage.
 *
 * Since: 0.10.32
 */
GstPad *
gst_element_request_pad (GstElement * element,
    GstPadTemplate * templ, const gchar * name, const GstCaps * caps)
{
  g_return_val_if_fail (GST_IS_ELEMENT (element), NULL);
  g_return_val_if_fail (templ != NULL, NULL);

  return _gst_element_request_pad (element, templ, name, caps);
}

/**
 * gst_element_get_pad:
 * @element: a #GstElement.
 * @name: the name of the pad to retrieve.
 *
 * Retrieves a pad from @element by name. Tries gst_element_get_static_pad()
 * first, then gst_element_get_request_pad().
 *
 * Deprecated: This function is deprecated as it's unclear if the reference
 * to the result pad should be released with gst_object_unref() in case of a static pad
 * or gst_element_release_request_pad() in case of a request pad.
 * Use gst_element_get_static_pad() or gst_element_get_request_pad() instead.
 *
 * Returns: (transfer full): the #GstPad if found, otherwise %NULL. Unref or Release after usage,
 * depending on the type of the pad.
 */
#ifndef GST_REMOVE_DEPRECATED
#ifdef GST_DISABLE_DEPRECATED
GstPad *gst_element_get_pad (GstElement * element, const gchar * name);
#endif
GstPad *
gst_element_get_pad (GstElement * element, const gchar * name)
{
  GstPad *pad;

  g_return_val_if_fail (GST_IS_ELEMENT (element), NULL);
  g_return_val_if_fail (name != NULL, NULL);

  pad = gst_element_get_static_pad (element, name);
  if (!pad)
    pad = gst_element_get_request_pad (element, name);

  return pad;
}
#endif /* GST_REMOVE_DEPRECATED */

static GstIteratorItem
iterate_pad (GstIterator * it, GstPad * pad)
{
  gst_object_ref (pad);
  return GST_ITERATOR_ITEM_PASS;
}

static GstIterator *
gst_element_iterate_pad_list (GstElement * element, GList ** padlist)
{
  GstIterator *result;

  GST_OBJECT_LOCK (element);
  gst_object_ref (element);
  result = gst_iterator_new_list (GST_TYPE_PAD,
      GST_OBJECT_GET_LOCK (element),
      &element->pads_cookie,
      padlist,
      element,
      (GstIteratorItemFunction) iterate_pad,
      (GstIteratorDisposeFunction) gst_object_unref);
  GST_OBJECT_UNLOCK (element);

  return result;
}

/**
 * gst_element_iterate_pads:
 * @element: a #GstElement to iterate pads of.
 *
 * Retrieves an iterattor of @element's pads. The iterator should
 * be freed after usage.
 *
 * Returns: (transfer full): the #GstIterator of #GstPad. Unref each pad
 *     after use.
 *
 * MT safe.
 */
GstIterator *
gst_element_iterate_pads (GstElement * element)
{
  g_return_val_if_fail (GST_IS_ELEMENT (element), NULL);

  return gst_element_iterate_pad_list (element, &element->pads);
}

/**
 * gst_element_iterate_src_pads:
 * @element: a #GstElement.
 *
 * Retrieves an iterator of @element's source pads.
 *
 * Returns: (transfer full): the #GstIterator of #GstPad. Unref each pad
 *     after use.
 *
 * MT safe.
 */
GstIterator *
gst_element_iterate_src_pads (GstElement * element)
{
  g_return_val_if_fail (GST_IS_ELEMENT (element), NULL);

  return gst_element_iterate_pad_list (element, &element->srcpads);
}

/**
 * gst_element_iterate_sink_pads:
 * @element: a #GstElement.
 *
 * Retrieves an iterator of @element's sink pads.
 *
 * Returns: (transfer full): the #GstIterator of #GstPad. Unref each pad
 *     after use.
 *
 * MT safe.
 */
GstIterator *
gst_element_iterate_sink_pads (GstElement * element)
{
  g_return_val_if_fail (GST_IS_ELEMENT (element), NULL);

  return gst_element_iterate_pad_list (element, &element->sinkpads);
}

/**
 * gst_element_class_add_pad_template:
 * @klass: the #GstElementClass to add the pad template to.
 * @templ: (transfer none): a #GstPadTemplate to add to the element class.
 *
 * Adds a padtemplate to an element class. This is mainly used in the _base_init
 * functions of classes.
 */
void
gst_element_class_add_pad_template (GstElementClass * klass,
    GstPadTemplate * templ)
{
  g_return_if_fail (GST_IS_ELEMENT_CLASS (klass));
  g_return_if_fail (GST_IS_PAD_TEMPLATE (templ));

  /* FIXME 0.11: allow replacing the pad templates by
   * calling this with the same name as an already existing pad
   * template. For this we _must_ _not_ ref the added pad template
   * a second time and _must_ document that this function takes
   * ownership of the pad template. Otherwise we will leak pad templates
   * or the caller unref's the pad template and it disappears */
  /* avoid registering pad templates with the same name */
  g_return_if_fail (gst_element_class_get_pad_template (klass,
          templ->name_template) == NULL);

  klass->padtemplates = g_list_append (klass->padtemplates,
      gst_object_ref (templ));
  klass->numpadtemplates++;
}

static void
gst_element_class_add_meta_data (GstElementClass * klass,
    const gchar * key, const gchar * value)
{
  if (!klass->meta_data) {
    /* FIXME: use a quark for "metadata" */
    klass->meta_data = gst_structure_empty_new ("metadata");
  }

  gst_structure_set ((GstStructure *) klass->meta_data,
      key, G_TYPE_STRING, value, NULL);
}

/**
 * gst_element_class_set_documentation_uri:
 * @klass: class to set details for
 * @uri: uri of element documentation
 *
 * Set uri pointing to user documentation. Applications can use this to show
 * help for e.g. effects to users.
 *
 * Since: 0.10.31
 */
void
gst_element_class_set_documentation_uri (GstElementClass * klass,
    const gchar * uri)
{
  g_return_if_fail (GST_IS_ELEMENT_CLASS (klass));

  gst_element_class_add_meta_data (klass, "doc-uri", uri);
}

/**
 * gst_element_class_set_icon_name:
 * @klass: class to set details for
 * @name: name of an icon
 *
 * Elements that bridge to certain other products can include an icon of that
 * used product. Application can show the icon in menus/selectors to help
 * identifying specific elements.
 *
 * Since: 0.10.31
 */
void
gst_element_class_set_icon_name (GstElementClass * klass, const gchar * name)
{
  g_return_if_fail (GST_IS_ELEMENT_CLASS (klass));

  gst_element_class_add_meta_data (klass, "icon-name", name);
}

/* FIXME-0.11: deprecate and remove gst_element_class_set_details*() */
/**
 * gst_element_class_set_details:
 * @klass: class to set details for
 * @details: details to set
 *
 * Sets the detailed information for a #GstElementClass.
 * <note>This function is for use in _base_init functions only.</note>
 *
 * The @details are copied.
 *
 * Deprecated: Use gst_element_class_set_details_simple() instead.
 */
#ifndef GST_REMOVE_DEPRECATED
#ifdef GST_DISABLE_DEPRECATED
void gst_element_class_set_details (GstElementClass * klass,
    const GstElementDetails * details);
#endif
void
gst_element_class_set_details (GstElementClass * klass,
    const GstElementDetails * details)
{
  g_return_if_fail (GST_IS_ELEMENT_CLASS (klass));
  g_return_if_fail (GST_IS_ELEMENT_DETAILS (details));

  __gst_element_details_copy (&klass->details, details);
}
#endif

/**
 * gst_element_class_set_details_simple:
 * @klass: class to set details for
 * @longname: The long English name of the element. E.g. "File Sink"
 * @classification: String describing the type of element, as an unordered list
 * separated with slashes ('/'). See draft-klass.txt of the design docs
 * for more details and common types. E.g: "Sink/File"
 * @description: Sentence describing the purpose of the element.
 * E.g: "Write stream to a file"
 * @author: Name and contact details of the author(s). Use \n to separate
 * multiple author details. E.g: "Joe Bloggs &lt;joe.blogs at foo.com&gt;"
 *
 * Sets the detailed information for a #GstElementClass. Simpler version of
 * gst_element_class_set_details() that generates less linker overhead.
 * <note>This function is for use in _base_init functions only.</note>
 *
 * The detail parameter strings are copied into the #GstElementDetails for
 * the element class.
 *
 * Since: 0.10.14
 */
void
gst_element_class_set_details_simple (GstElementClass * klass,
    const gchar * longname, const gchar * classification,
    const gchar * description, const gchar * author)
{
  const GstElementDetails details =
      GST_ELEMENT_DETAILS ((gchar *) longname, (gchar *) classification,
      (gchar *) description, (gchar *) author);

  g_return_if_fail (GST_IS_ELEMENT_CLASS (klass));

  __gst_element_details_copy (&klass->details, &details);
}

/**
 * gst_element_class_get_pad_template_list:
 * @element_class: a #GstElementClass to get pad templates of.
 *
 * Retrieves a list of the pad templates associated with @element_class. The
 * list must not be modified by the calling code.
 * <note>If you use this function in the #GInstanceInitFunc of an object class
 * that has subclasses, make sure to pass the g_class parameter of the
 * #GInstanceInitFunc here.</note>
 *
 * Returns: (transfer none) (element-type Gst.PadTemplate): the #GList of
 *     pad templates.
 */
GList *
gst_element_class_get_pad_template_list (GstElementClass * element_class)
{
  g_return_val_if_fail (GST_IS_ELEMENT_CLASS (element_class), NULL);

  return element_class->padtemplates;
}

/**
 * gst_element_class_get_pad_template:
 * @element_class: a #GstElementClass to get the pad template of.
 * @name: the name of the #GstPadTemplate to get.
 *
 * Retrieves a padtemplate from @element_class with the given name.
 * <note>If you use this function in the #GInstanceInitFunc of an object class
 * that has subclasses, make sure to pass the g_class parameter of the
 * #GInstanceInitFunc here.</note>
 *
 * Returns: (transfer none): the #GstPadTemplate with the given name, or %NULL
 *     if none was found. No unreferencing is necessary.
 */
GstPadTemplate *
gst_element_class_get_pad_template (GstElementClass *
    element_class, const gchar * name)
{
  GList *padlist;

  g_return_val_if_fail (GST_IS_ELEMENT_CLASS (element_class), NULL);
  g_return_val_if_fail (name != NULL, NULL);

  padlist = element_class->padtemplates;

  while (padlist) {
    GstPadTemplate *padtempl = (GstPadTemplate *) padlist->data;

    if (strcmp (padtempl->name_template, name) == 0)
      return padtempl;

    padlist = g_list_next (padlist);
  }

  return NULL;
}

static GstPadTemplate *
gst_element_class_get_request_pad_template (GstElementClass *
    element_class, const gchar * name)
{
  GstPadTemplate *tmpl;

  tmpl = gst_element_class_get_pad_template (element_class, name);
  if (tmpl != NULL && tmpl->presence == GST_PAD_REQUEST)
    return tmpl;

  return NULL;
}

/* get a random pad on element of the given direction.
 * The pad is random in a sense that it is the first pad that is (optionaly) linked.
 */
static GstPad *
gst_element_get_random_pad (GstElement * element,
    gboolean need_linked, GstPadDirection dir)
{
  GstPad *result = NULL;
  GList *pads;

  GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS, "getting a random pad");

  switch (dir) {
    case GST_PAD_SRC:
      GST_OBJECT_LOCK (element);
      pads = element->srcpads;
      break;
    case GST_PAD_SINK:
      GST_OBJECT_LOCK (element);
      pads = element->sinkpads;
      break;
    default:
      goto wrong_direction;
  }
  for (; pads; pads = g_list_next (pads)) {
    GstPad *pad = GST_PAD_CAST (pads->data);

    GST_OBJECT_LOCK (pad);
    GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS, "checking pad %s:%s",
        GST_DEBUG_PAD_NAME (pad));

    if (need_linked && !GST_PAD_IS_LINKED (pad)) {
      /* if we require a linked pad, and it is not linked, continue the
       * search */
      GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS, "pad %s:%s is not linked",
          GST_DEBUG_PAD_NAME (pad));
      GST_OBJECT_UNLOCK (pad);
      continue;
    } else {
      /* found a pad, stop search */
      GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS, "found pad %s:%s",
          GST_DEBUG_PAD_NAME (pad));
      GST_OBJECT_UNLOCK (pad);
      result = pad;
      break;
    }
  }
  if (result)
    gst_object_ref (result);

  GST_OBJECT_UNLOCK (element);

  return result;

  /* ERROR handling */
wrong_direction:
  {
    g_warning ("unknown pad direction %d", dir);
    return NULL;
  }
}

static gboolean
gst_element_default_send_event (GstElement * element, GstEvent * event)
{
  gboolean result = FALSE;
  GstPad *pad;

  pad = GST_EVENT_IS_DOWNSTREAM (event) ?
      gst_element_get_random_pad (element, TRUE, GST_PAD_SRC) :
      gst_element_get_random_pad (element, TRUE, GST_PAD_SINK);

  if (pad) {
    GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS,
        "pushing %s event to random %s pad %s:%s",
        GST_EVENT_TYPE_NAME (event),
        (GST_PAD_DIRECTION (pad) == GST_PAD_SRC ? "src" : "sink"),
        GST_DEBUG_PAD_NAME (pad));

    result = gst_pad_push_event (pad, event);
    gst_object_unref (pad);
  } else {
    GST_CAT_INFO (GST_CAT_ELEMENT_PADS, "can't send %s event on element %s",
        GST_EVENT_TYPE_NAME (event), GST_ELEMENT_NAME (element));
    gst_event_unref (event);
  }
  return result;
}

/**
 * gst_element_send_event:
 * @element: a #GstElement to send the event to.
 * @event: (transfer full): the #GstEvent to send to the element.
 *
 * Sends an event to an element. If the element doesn't implement an
 * event handler, the event will be pushed on a random linked sink pad for
 * upstream events or a random linked source pad for downstream events.
 *
 * This function takes owership of the provided event so you should
 * gst_event_ref() it if you want to reuse the event after this call.
 *
 * Returns: %TRUE if the event was handled.
 *
 * MT safe.
 */
gboolean
gst_element_send_event (GstElement * element, GstEvent * event)
{
  GstElementClass *oclass;
  gboolean result = FALSE;

  g_return_val_if_fail (GST_IS_ELEMENT (element), FALSE);
  g_return_val_if_fail (event != NULL, FALSE);

  oclass = GST_ELEMENT_GET_CLASS (element);

  GST_STATE_LOCK (element);
  if (oclass->send_event) {
    GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS, "send %s event on element %s",
        GST_EVENT_TYPE_NAME (event), GST_ELEMENT_NAME (element));
    result = oclass->send_event (element, event);
  } else {
    result = gst_element_default_send_event (element, event);
  }
  GST_STATE_UNLOCK (element);

  return result;
}

/**
 * gst_element_seek:
 * @element: a #GstElement to send the event to.
 * @rate: The new playback rate
 * @format: The format of the seek values
 * @flags: The optional seek flags.
 * @cur_type: The type and flags for the new current position
 * @cur: The value of the new current position
 * @stop_type: The type and flags for the new stop position
 * @stop: The value of the new stop position
 *
 * Sends a seek event to an element. See gst_event_new_seek() for the details of
 * the parameters. The seek event is sent to the element using
 * gst_element_send_event().
 *
 * Returns: %TRUE if the event was handled.
 *
 * MT safe.
 */
gboolean
gst_element_seek (GstElement * element, gdouble rate, GstFormat format,
    GstSeekFlags flags, GstSeekType cur_type, gint64 cur,
    GstSeekType stop_type, gint64 stop)
{
  GstEvent *event;
  gboolean result;

  g_return_val_if_fail (GST_IS_ELEMENT (element), FALSE);

  event =
      gst_event_new_seek (rate, format, flags, cur_type, cur, stop_type, stop);
  result = gst_element_send_event (element, event);

  return result;
}

/**
 * gst_element_get_query_types:
 * @element: a #GstElement to query
 *
 * Get an array of query types from the element.
 * If the element doesn't implement a query types function,
 * the query will be forwarded to the peer of a random linked sink pad.
 *
 * Returns: An array of #GstQueryType elements that should not
 * be freed or modified.
 *
 * MT safe.
 */
const GstQueryType *
gst_element_get_query_types (GstElement * element)
{
  GstElementClass *oclass;
  const GstQueryType *result = NULL;

  g_return_val_if_fail (GST_IS_ELEMENT (element), NULL);

  oclass = GST_ELEMENT_GET_CLASS (element);

  if (oclass->get_query_types) {
    result = oclass->get_query_types (element);
  } else {
    GstPad *pad = gst_element_get_random_pad (element, TRUE, GST_PAD_SINK);

    if (pad) {
      GstPad *peer = gst_pad_get_peer (pad);

      if (peer) {
        result = gst_pad_get_query_types (peer);

        gst_object_unref (peer);
      }
      gst_object_unref (pad);
    }
  }
  return result;
}

static gboolean
gst_element_default_query (GstElement * element, GstQuery * query)
{
  gboolean result = FALSE;
  GstPad *pad;

  pad = gst_element_get_random_pad (element, FALSE, GST_PAD_SRC);
  if (pad) {
    result = gst_pad_query (pad, query);

    gst_object_unref (pad);
  } else {
    pad = gst_element_get_random_pad (element, TRUE, GST_PAD_SINK);
    if (pad) {
      GstPad *peer = gst_pad_get_peer (pad);

      if (peer) {
        result = gst_pad_query (peer, query);

        gst_object_unref (peer);
      }
      gst_object_unref (pad);
    }
  }
  return result;
}

/**
 * gst_element_query:
 * @element: a #GstElement to perform the query on.
 * @query: (transfer none): the #GstQuery.
 *
 * Performs a query on the given element.
 *
 * For elements that don't implement a query handler, this function
 * forwards the query to a random srcpad or to the peer of a
 * random linked sinkpad of this element.
 *
 * Please note that some queries might need a running pipeline to work.
 *
 * Returns: TRUE if the query could be performed.
 *
 * MT safe.
 */
gboolean
gst_element_query (GstElement * element, GstQuery * query)
{
  GstElementClass *oclass;
  gboolean result = FALSE;

  g_return_val_if_fail (GST_IS_ELEMENT (element), FALSE);
  g_return_val_if_fail (query != NULL, FALSE);

  oclass = GST_ELEMENT_GET_CLASS (element);

  if (oclass->query) {
    GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS, "send query on element %s",
        GST_ELEMENT_NAME (element));
    result = oclass->query (element, query);
  } else {
    result = gst_element_default_query (element, query);
  }
  return result;
}

/**
 * gst_element_post_message:
 * @element: a #GstElement posting the message
 * @message: (transfer full): a #GstMessage to post
 *
 * Post a message on the element's #GstBus. This function takes ownership of the
 * message; if you want to access the message after this call, you should add an
 * additional reference before calling.
 *
 * Returns: %TRUE if the message was successfully posted. The function returns
 * %FALSE if the element did not have a bus.
 *
 * MT safe.
 */
gboolean
gst_element_post_message (GstElement * element, GstMessage * message)
{
  GstBus *bus;
  gboolean result = FALSE;

  g_return_val_if_fail (GST_IS_ELEMENT (element), FALSE);
  g_return_val_if_fail (message != NULL, FALSE);

  GST_OBJECT_LOCK (element);
  bus = element->bus;

  if (G_UNLIKELY (bus == NULL))
    goto no_bus;

  gst_object_ref (bus);
  GST_OBJECT_UNLOCK (element);

  /* we release the element lock when posting the message so that any
   * (synchronous) message handlers can operate on the element */
  result = gst_bus_post (bus, message);
  gst_object_unref (bus);

  return result;

  /* ERRORS */
no_bus:
  {
    GST_CAT_DEBUG_OBJECT (GST_CAT_MESSAGE, element,
        "not posting message %p: no bus", message);
    GST_OBJECT_UNLOCK (element);
    gst_message_unref (message);
    return FALSE;
  }
}

/**
 * _gst_element_error_printf:
 * @format: the printf-like format to use, or %NULL
 *
 * This function is only used internally by the gst_element_error() macro.
 *
 * Returns: (transfer full): a newly allocated string, or %NULL if the format
 *     was %NULL or ""
 *
 * MT safe.
 */
gchar *
_gst_element_error_printf (const gchar * format, ...)
{
  va_list args;
  gchar *buffer;

  if (format == NULL)
    return NULL;
  if (format[0] == 0)
    return NULL;

  va_start (args, format);
  buffer = g_strdup_vprintf (format, args);
  va_end (args);
  return buffer;
}

/**
 * gst_element_message_full:
 * @element:  a #GstElement to send message from
 * @type:     the #GstMessageType
 * @domain:   the GStreamer GError domain this message belongs to
 * @code:     the GError code belonging to the domain
 * @text:     (allow-none) (transfer full): an allocated text string to be used
 *            as a replacement for the default message connected to code,
 *            or %NULL
 * @debug:    (allow-none) (transfer full): an allocated debug message to be
 *            used as a replacement for the default debugging information,
 *            or %NULL
 * @file:     the source code file where the error was generated
 * @function: the source code function where the error was generated
 * @line:     the source code line where the error was generated
 *
 * Post an error, warning or info message on the bus from inside an element.
 *
 * @type must be of #GST_MESSAGE_ERROR, #GST_MESSAGE_WARNING or
 * #GST_MESSAGE_INFO.
 *
 * MT safe.
 */
void gst_element_message_full
    (GstElement * element, GstMessageType type,
    GQuark domain, gint code, gchar * text,
    gchar * debug, const gchar * file, const gchar * function, gint line)
{
  GError *gerror = NULL;
  gchar *name;
  gchar *sent_text;
  gchar *sent_debug;
  gboolean has_debug = TRUE;
  GstMessage *message = NULL;

  /* checks */
  GST_CAT_DEBUG_OBJECT (GST_CAT_MESSAGE, element, "start");
  g_return_if_fail (GST_IS_ELEMENT (element));
  g_return_if_fail ((type == GST_MESSAGE_ERROR) ||
      (type == GST_MESSAGE_WARNING) || (type == GST_MESSAGE_INFO));

  /* check if we send the given text or the default error text */
  if ((text == NULL) || (text[0] == 0)) {
    /* text could have come from g_strdup_printf (""); */
    g_free (text);
    sent_text = gst_error_get_message (domain, code);
  } else
    sent_text = text;

  /* construct a sent_debug with extra information from source */
  if ((debug == NULL) || (debug[0] == 0)) {
    /* debug could have come from g_strdup_printf (""); */
    has_debug = FALSE;
  }

  name = gst_object_get_path_string (GST_OBJECT_CAST (element));
  if (has_debug)
    sent_debug = g_strdup_printf ("%s(%d): %s (): %s:\n%s",
        file, line, function, name, debug);
  else
    sent_debug = g_strdup_printf ("%s(%d): %s (): %s",
        file, line, function, name);
  g_free (name);
  g_free (debug);

  /* create gerror and post message */
  GST_CAT_INFO_OBJECT (GST_CAT_ERROR_SYSTEM, element, "posting message: %s",
      sent_text);
  gerror = g_error_new_literal (domain, code, sent_text);

  switch (type) {
    case GST_MESSAGE_ERROR:
      message =
          gst_message_new_error (GST_OBJECT_CAST (element), gerror, sent_debug);
      break;
    case GST_MESSAGE_WARNING:
      message = gst_message_new_warning (GST_OBJECT_CAST (element), gerror,
          sent_debug);
      break;
    case GST_MESSAGE_INFO:
      message = gst_message_new_info (GST_OBJECT_CAST (element), gerror,
          sent_debug);
      break;
    default:
      g_assert_not_reached ();
      break;
  }
  gst_element_post_message (element, message);

  GST_CAT_INFO_OBJECT (GST_CAT_ERROR_SYSTEM, element, "posted %s message: %s",
      (type == GST_MESSAGE_ERROR ? "error" : "warning"), sent_text);

  /* cleanup */
  g_error_free (gerror);
  g_free (sent_debug);
  g_free (sent_text);
}

/**
 * gst_element_is_locked_state:
 * @element: a #GstElement.
 *
 * Checks if the state of an element is locked.
 * If the state of an element is locked, state changes of the parent don't
 * affect the element.
 * This way you can leave currently unused elements inside bins. Just lock their
 * state before changing the state from #GST_STATE_NULL.
 *
 * MT safe.
 *
 * Returns: TRUE, if the element's state is locked.
 */
gboolean
gst_element_is_locked_state (GstElement * element)
{
  gboolean result;

  g_return_val_if_fail (GST_IS_ELEMENT (element), FALSE);

  GST_OBJECT_LOCK (element);
  result = GST_OBJECT_FLAG_IS_SET (element, GST_ELEMENT_LOCKED_STATE);
  GST_OBJECT_UNLOCK (element);

  return result;
}

/**
 * gst_element_set_locked_state:
 * @element: a #GstElement
 * @locked_state: TRUE to lock the element's state
 *
 * Locks the state of an element, so state changes of the parent don't affect
 * this element anymore.
 *
 * MT safe.
 *
 * Returns: TRUE if the state was changed, FALSE if bad parameters were given
 * or the elements state-locking needed no change.
 */
gboolean
gst_element_set_locked_state (GstElement * element, gboolean locked_state)
{
  gboolean old;

  g_return_val_if_fail (GST_IS_ELEMENT (element), FALSE);

  GST_OBJECT_LOCK (element);
  old = GST_OBJECT_FLAG_IS_SET (element, GST_ELEMENT_LOCKED_STATE);

  if (G_UNLIKELY (old == locked_state))
    goto was_ok;

  if (locked_state) {
    GST_CAT_DEBUG (GST_CAT_STATES, "locking state of element %s",
        GST_ELEMENT_NAME (element));
    GST_OBJECT_FLAG_SET (element, GST_ELEMENT_LOCKED_STATE);
  } else {
    GST_CAT_DEBUG (GST_CAT_STATES, "unlocking state of element %s",
        GST_ELEMENT_NAME (element));
    GST_OBJECT_FLAG_UNSET (element, GST_ELEMENT_LOCKED_STATE);
  }
  GST_OBJECT_UNLOCK (element);

  return TRUE;

was_ok:
  {
    GST_CAT_DEBUG (GST_CAT_STATES,
        "elements %s was already in locked state %d",
        GST_ELEMENT_NAME (element), old);
    GST_OBJECT_UNLOCK (element);

    return FALSE;
  }
}

/**
 * gst_element_sync_state_with_parent:
 * @element: a #GstElement.
 *
 * Tries to change the state of the element to the same as its parent.
 * If this function returns FALSE, the state of element is undefined.
 *
 * Returns: TRUE, if the element's state could be synced to the parent's state.
 *
 * MT safe.
 */
gboolean
gst_element_sync_state_with_parent (GstElement * element)
{
  GstElement *parent;
  GstState target;
  GstStateChangeReturn ret;

  g_return_val_if_fail (GST_IS_ELEMENT (element), FALSE);

  if ((parent = GST_ELEMENT_CAST (gst_element_get_parent (element)))) {
    GstState parent_current, parent_pending;

    GST_OBJECT_LOCK (parent);
    parent_current = GST_STATE (parent);
    parent_pending = GST_STATE_PENDING (parent);
    GST_OBJECT_UNLOCK (parent);

    /* set to pending if there is one, else we set it to the current state of
     * the parent */
    if (parent_pending != GST_STATE_VOID_PENDING)
      target = parent_pending;
    else
      target = parent_current;

    GST_CAT_DEBUG_OBJECT (GST_CAT_STATES, element,
        "syncing state (%s) to parent %s %s (%s, %s)",
        gst_element_state_get_name (GST_STATE (element)),
        GST_ELEMENT_NAME (parent), gst_element_state_get_name (target),
        gst_element_state_get_name (parent_current),
        gst_element_state_get_name (parent_pending));

    ret = gst_element_set_state (element, target);
    if (ret == GST_STATE_CHANGE_FAILURE)
      goto failed;

    gst_object_unref (parent);

    return TRUE;
  } else {
    GST_CAT_DEBUG_OBJECT (GST_CAT_STATES, element, "element has no parent");
  }
  return FALSE;

  /* ERROR */
failed:
  {
    GST_CAT_DEBUG_OBJECT (GST_CAT_STATES, element,
        "syncing state failed (%s)",
        gst_element_state_change_return_get_name (ret));
    gst_object_unref (parent);
    return FALSE;
  }
}

/* MT safe */
static GstStateChangeReturn
gst_element_get_state_func (GstElement * element,
    GstState * state, GstState * pending, GstClockTime timeout)
{
  GstStateChangeReturn ret = GST_STATE_CHANGE_FAILURE;
  GstState old_pending;

  GST_CAT_DEBUG_OBJECT (GST_CAT_STATES, element, "getting state, timeout %"
      GST_TIME_FORMAT, GST_TIME_ARGS (timeout));

  GST_OBJECT_LOCK (element);
  ret = GST_STATE_RETURN (element);
  GST_CAT_DEBUG_OBJECT (GST_CAT_STATES, element, "RETURN is %s",
      gst_element_state_change_return_get_name (ret));

  /* we got an error, report immediately */
  if (ret == GST_STATE_CHANGE_FAILURE)
    goto done;

  /* we got no_preroll, report immediatly */
  if (ret == GST_STATE_CHANGE_NO_PREROLL)
    goto done;

  /* no need to wait async if we are not async */
  if (ret != GST_STATE_CHANGE_ASYNC)
    goto done;

  old_pending = GST_STATE_PENDING (element);
  if (old_pending != GST_STATE_VOID_PENDING) {
    GTimeVal *timeval, abstimeout;
    guint32 cookie;

    if (timeout != GST_CLOCK_TIME_NONE) {
      glong add = timeout / 1000;

      if (add == 0)
        goto done;

      /* make timeout absolute */
      g_get_current_time (&abstimeout);
      g_time_val_add (&abstimeout, add);
      timeval = &abstimeout;
    } else {
      timeval = NULL;
    }
    /* get cookie to detect state changes during waiting */
    cookie = element->state_cookie;

    GST_CAT_INFO_OBJECT (GST_CAT_STATES, element,
        "waiting for element to commit state");

    /* we have a pending state change, wait for it to complete */
    if (!GST_STATE_TIMED_WAIT (element, timeval)) {
      GST_CAT_INFO_OBJECT (GST_CAT_STATES, element, "timed out");
      /* timeout triggered */
      ret = GST_STATE_CHANGE_ASYNC;
    } else {
      if (cookie != element->state_cookie)
        goto interrupted;

      /* could be success or failure */
      if (old_pending == GST_STATE (element)) {
        GST_CAT_DEBUG_OBJECT (GST_CAT_STATES, element, "got success");
        ret = GST_STATE_CHANGE_SUCCESS;
      } else {
        GST_CAT_DEBUG_OBJECT (GST_CAT_STATES, element, "got failure");
        ret = GST_STATE_CHANGE_FAILURE;
      }
    }
    /* if nothing is pending anymore we can return SUCCESS */
    if (GST_STATE_PENDING (element) == GST_STATE_VOID_PENDING) {
      GST_CAT_LOG_OBJECT (GST_CAT_STATES, element, "nothing pending");
      ret = GST_STATE_CHANGE_SUCCESS;
    }
  }

done:
  if (state)
    *state = GST_STATE (element);
  if (pending)
    *pending = GST_STATE_PENDING (element);

  GST_CAT_DEBUG_OBJECT (GST_CAT_STATES, element,
      "state current: %s, pending: %s, result: %s",
      gst_element_state_get_name (GST_STATE (element)),
      gst_element_state_get_name (GST_STATE_PENDING (element)),
      gst_element_state_change_return_get_name (ret));
  GST_OBJECT_UNLOCK (element);

  return ret;

interrupted:
  {
    if (state)
      *state = GST_STATE_VOID_PENDING;
    if (pending)
      *pending = GST_STATE_VOID_PENDING;

    GST_CAT_INFO_OBJECT (GST_CAT_STATES, element, "interruped");

    GST_OBJECT_UNLOCK (element);

    return GST_STATE_CHANGE_FAILURE;
  }
}

/**
 * gst_element_get_state:
 * @element: a #GstElement to get the state of.
 * @state: (out) (allow-none): a pointer to #GstState to hold the state.
 *     Can be %NULL.
 * @pending: (out) (allow-none): a pointer to #GstState to hold the pending
 *     state. Can be %NULL.
 * @timeout: a #GstClockTime to specify the timeout for an async
 *           state change or %GST_CLOCK_TIME_NONE for infinite timeout.
 *
 * Gets the state of the element.
 *
 * For elements that performed an ASYNC state change, as reported by
 * gst_element_set_state(), this function will block up to the
 * specified timeout value for the state change to complete.
 * If the element completes the state change or goes into
 * an error, this function returns immediately with a return value of
 * %GST_STATE_CHANGE_SUCCESS or %GST_STATE_CHANGE_FAILURE respectively.
 *
 * For elements that did not return %GST_STATE_CHANGE_ASYNC, this function
 * returns the current and pending state immediately.
 *
 * This function returns %GST_STATE_CHANGE_NO_PREROLL if the element
 * successfully changed its state but is not able to provide data yet.
 * This mostly happens for live sources that only produce data in
 * %GST_STATE_PLAYING. While the state change return is equivalent to
 * %GST_STATE_CHANGE_SUCCESS, it is returned to the application to signal that
 * some sink elements might not be able to complete their state change because
 * an element is not producing data to complete the preroll. When setting the
 * element to playing, the preroll will complete and playback will start.
 *
 * Returns: %GST_STATE_CHANGE_SUCCESS if the element has no more pending state
 *          and the last state change succeeded, %GST_STATE_CHANGE_ASYNC if the
 *          element is still performing a state change or
 *          %GST_STATE_CHANGE_FAILURE if the last state change failed.
 *
 * MT safe.
 */
GstStateChangeReturn
gst_element_get_state (GstElement * element,
    GstState * state, GstState * pending, GstClockTime timeout)
{
  GstElementClass *oclass;
  GstStateChangeReturn result = GST_STATE_CHANGE_FAILURE;

  g_return_val_if_fail (GST_IS_ELEMENT (element), GST_STATE_CHANGE_FAILURE);

  oclass = GST_ELEMENT_GET_CLASS (element);

  if (oclass->get_state)
    result = (oclass->get_state) (element, state, pending, timeout);

  return result;
}

/**
 * gst_element_abort_state:
 * @element: a #GstElement to abort the state of.
 *
 * Abort the state change of the element. This function is used
 * by elements that do asynchronous state changes and find out
 * something is wrong.
 *
 * This function should be called with the STATE_LOCK held.
 *
 * MT safe.
 */
void
gst_element_abort_state (GstElement * element)
{
  GstState pending;

#ifndef GST_DISABLE_GST_DEBUG
  GstState old_state;
#endif

  g_return_if_fail (GST_IS_ELEMENT (element));

  GST_OBJECT_LOCK (element);
  pending = GST_STATE_PENDING (element);

  if (pending == GST_STATE_VOID_PENDING ||
      GST_STATE_RETURN (element) == GST_STATE_CHANGE_FAILURE)
    goto nothing_aborted;

#ifndef GST_DISABLE_GST_DEBUG
  old_state = GST_STATE (element);

  GST_CAT_INFO_OBJECT (GST_CAT_STATES, element,
      "aborting state from %s to %s", gst_element_state_get_name (old_state),
      gst_element_state_get_name (pending));
#endif

  /* flag error */
  GST_STATE_RETURN (element) = GST_STATE_CHANGE_FAILURE;

  GST_STATE_BROADCAST (element);
  GST_OBJECT_UNLOCK (element);

  return;

nothing_aborted:
  {
    GST_OBJECT_UNLOCK (element);
    return;
  }
}

/**
 * gst_element_continue_state:
 * @element: a #GstElement to continue the state change of.
 * @ret: The previous state return value
 *
 * Commit the state change of the element and proceed to the next
 * pending state if any. This function is used
 * by elements that do asynchronous state changes.
 * The core will normally call this method automatically when an
 * element returned %GST_STATE_CHANGE_SUCCESS from the state change function.
 *
 * If after calling this method the element still has not reached
 * the pending state, the next state change is performed.
 *
 * This method is used internally and should normally not be called by plugins
 * or applications.
 *
 * Returns: The result of the commit state change.
 *
 * MT safe.
 */
GstStateChangeReturn
gst_element_continue_state (GstElement * element, GstStateChangeReturn ret)
{
  GstStateChangeReturn old_ret;
  GstState old_state, old_next;
  GstState current, next, pending;
  GstMessage *message;
  GstStateChange transition;

  GST_OBJECT_LOCK (element);
  old_ret = GST_STATE_RETURN (element);
  GST_STATE_RETURN (element) = ret;
  pending = GST_STATE_PENDING (element);

  /* check if there is something to commit */
  if (pending == GST_STATE_VOID_PENDING)
    goto nothing_pending;

  old_state = GST_STATE (element);
  /* this is the state we should go to next */
  old_next = GST_STATE_NEXT (element);
  /* update current state */
  current = GST_STATE (element) = old_next;

  /* see if we reached the final state */
  if (pending == current)
    goto complete;

  next = GST_STATE_GET_NEXT (current, pending);
  transition = (GstStateChange) GST_STATE_TRANSITION (current, next);

  GST_STATE_NEXT (element) = next;
  /* mark busy */
  GST_STATE_RETURN (element) = GST_STATE_CHANGE_ASYNC;
  GST_OBJECT_UNLOCK (element);

  GST_CAT_INFO_OBJECT (GST_CAT_STATES, element,
      "committing state from %s to %s, pending %s, next %s",
      gst_element_state_get_name (old_state),
      gst_element_state_get_name (old_next),
      gst_element_state_get_name (pending), gst_element_state_get_name (next));

  message = gst_message_new_state_changed (GST_OBJECT_CAST (element),
      old_state, old_next, pending);
  gst_element_post_message (element, message);

  GST_CAT_INFO_OBJECT (GST_CAT_STATES, element,
      "continue state change %s to %s, final %s",
      gst_element_state_get_name (current),
      gst_element_state_get_name (next), gst_element_state_get_name (pending));

  ret = gst_element_change_state (element, transition);

  return ret;

nothing_pending:
  {
    GST_CAT_INFO_OBJECT (GST_CAT_STATES, element, "nothing pending");
    GST_OBJECT_UNLOCK (element);
    return ret;
  }
complete:
  {
    GST_STATE_PENDING (element) = GST_STATE_VOID_PENDING;
    GST_STATE_NEXT (element) = GST_STATE_VOID_PENDING;

    GST_CAT_INFO_OBJECT (GST_CAT_STATES, element,
        "completed state change to %s", gst_element_state_get_name (pending));
    GST_OBJECT_UNLOCK (element);

    /* don't post silly messages with the same state. This can happen
     * when an element state is changed to what it already was. For bins
     * this can be the result of a lost state, which we check with the
     * previous return value.
     * We do signal the cond though as a _get_state() might be blocking
     * on it. */
    if (old_state != old_next || old_ret == GST_STATE_CHANGE_ASYNC) {
      GST_CAT_INFO_OBJECT (GST_CAT_STATES, element,
          "posting state-changed %s to %s",
          gst_element_state_get_name (old_state),
          gst_element_state_get_name (old_next));
      message =
          gst_message_new_state_changed (GST_OBJECT_CAST (element), old_state,
          old_next, GST_STATE_VOID_PENDING);
      gst_element_post_message (element, message);
    }

    GST_STATE_BROADCAST (element);

    return ret;
  }
}

/**
 * gst_element_lost_state_full:
 * @element: a #GstElement the state is lost of
 * @new_base_time: if a new base time should be distributed
 *
 * Brings the element to the lost state. The current state of the
 * element is copied to the pending state so that any call to
 * gst_element_get_state() will return %GST_STATE_CHANGE_ASYNC.
 *
 * An ASYNC_START message is posted with indication to distribute a new
 * base_time to the element when @new_base_time is %TRUE.
 * If the element was PLAYING, it will go to PAUSED. The element
 * will be restored to its PLAYING state by the parent pipeline when it
 * prerolls again.
 *
 * This is mostly used for elements that lost their preroll buffer
 * in the %GST_STATE_PAUSED or %GST_STATE_PLAYING state after a flush,
 * they will go to their pending state again when a new preroll buffer is
 * queued. This function can only be called when the element is currently
 * not in error or an async state change.
 *
 * This function is used internally and should normally not be called from
 * plugins or applications.
 *
 * MT safe.
 *
 * Since: 0.10.24
 */
void
gst_element_lost_state_full (GstElement * element, gboolean new_base_time)
{
  GstState old_state, new_state;
  GstMessage *message;

  g_return_if_fail (GST_IS_ELEMENT (element));

  GST_OBJECT_LOCK (element);
  if (GST_STATE_RETURN (element) == GST_STATE_CHANGE_FAILURE)
    goto nothing_lost;

  if (GST_STATE_PENDING (element) != GST_STATE_VOID_PENDING)
    goto only_async_start;

  old_state = GST_STATE (element);

  /* when we were PLAYING, the new state is PAUSED. We will also not
   * automatically go to PLAYING but let the parent bin(s) set us to PLAYING
   * when we preroll. */
  if (old_state > GST_STATE_PAUSED)
    new_state = GST_STATE_PAUSED;
  else
    new_state = old_state;

  GST_CAT_DEBUG_OBJECT (GST_CAT_STATES, element,
      "lost state of %s to %s", gst_element_state_get_name (old_state),
      gst_element_state_get_name (new_state));

  GST_STATE (element) = new_state;
  GST_STATE_NEXT (element) = new_state;
  GST_STATE_PENDING (element) = new_state;
  GST_STATE_RETURN (element) = GST_STATE_CHANGE_ASYNC;
  if (new_base_time)
    GST_ELEMENT_START_TIME (element) = 0;
  GST_OBJECT_UNLOCK (element);

  message = gst_message_new_state_changed (GST_OBJECT_CAST (element),
      new_state, new_state, new_state);
  gst_element_post_message (element, message);

  message =
      gst_message_new_async_start (GST_OBJECT_CAST (element), new_base_time);
  gst_element_post_message (element, message);

  return;

nothing_lost:
  {
    GST_OBJECT_UNLOCK (element);
    return;
  }
only_async_start:
  {
    GST_OBJECT_UNLOCK (element);

    message = gst_message_new_async_start (GST_OBJECT_CAST (element), TRUE);
    gst_element_post_message (element, message);
    return;
  }
}

/**
 * gst_element_lost_state:
 * @element: a #GstElement the state is lost of
 *
 * Brings the element to the lost state. This function calls
 * gst_element_lost_state_full() with the new_base_time set to %TRUE.
 *
 * This function is used internally and should normally not be called from
 * plugins or applications.
 *
 * MT safe.
 */
void
gst_element_lost_state (GstElement * element)
{
  gst_element_lost_state_full (element, TRUE);
}

/**
 * gst_element_set_state:
 * @element: a #GstElement to change state of.
 * @state: the element's new #GstState.
 *
 * Sets the state of the element. This function will try to set the
 * requested state by going through all the intermediary states and calling
 * the class's state change function for each.
 *
 * This function can return #GST_STATE_CHANGE_ASYNC, in which case the
 * element will perform the remainder of the state change asynchronously in
 * another thread.
 * An application can use gst_element_get_state() to wait for the completion
 * of the state change or it can wait for a state change message on the bus.
 *
 * State changes to %GST_STATE_READY or %GST_STATE_NULL never return
 * #GST_STATE_CHANGE_ASYNC.
 *
 * Returns: Result of the state change using #GstStateChangeReturn.
 *
 * MT safe.
 */
GstStateChangeReturn
gst_element_set_state (GstElement * element, GstState state)
{
  GstElementClass *oclass;
  GstStateChangeReturn result = GST_STATE_CHANGE_FAILURE;

  g_return_val_if_fail (GST_IS_ELEMENT (element), GST_STATE_CHANGE_FAILURE);

  oclass = GST_ELEMENT_GET_CLASS (element);

  if (oclass->set_state)
    result = (oclass->set_state) (element, state);

  return result;
}

/*
 * default set state function, calculates the next state based
 * on current state and calls the change_state function
 */
static GstStateChangeReturn
gst_element_set_state_func (GstElement * element, GstState state)
{
  GstState current, next, old_pending;
  GstStateChangeReturn ret;
  GstStateChange transition;
  GstStateChangeReturn old_ret;

  g_return_val_if_fail (GST_IS_ELEMENT (element), GST_STATE_CHANGE_FAILURE);

  GST_CAT_DEBUG_OBJECT (GST_CAT_STATES, element, "set_state to %s",
      gst_element_state_get_name (state));

  /* state lock is taken to protect the set_state() and get_state()
   * procedures, it does not lock any variables. */
  GST_STATE_LOCK (element);

  /* now calculate how to get to the new state */
  GST_OBJECT_LOCK (element);
  old_ret = GST_STATE_RETURN (element);
  /* previous state change returned an error, remove all pending
   * and next states */
  if (old_ret == GST_STATE_CHANGE_FAILURE) {
    GST_STATE_NEXT (element) = GST_STATE_VOID_PENDING;
    GST_STATE_PENDING (element) = GST_STATE_VOID_PENDING;
    GST_STATE_RETURN (element) = GST_STATE_CHANGE_SUCCESS;
  }

  current = GST_STATE (element);
  next = GST_STATE_NEXT (element);
  old_pending = GST_STATE_PENDING (element);

  /* this is the (new) state we should go to. TARGET is the last state we set on
   * the element. */
  if (state != GST_STATE_TARGET (element)) {
    GST_CAT_DEBUG_OBJECT (GST_CAT_STATES, element,
        "setting target state to %s", gst_element_state_get_name (state));
    GST_STATE_TARGET (element) = state;
    /* increment state cookie so that we can track each state change. We only do
     * this if this is actually a new state change. */
    element->state_cookie++;
  }
  GST_STATE_PENDING (element) = state;

  GST_CAT_DEBUG_OBJECT (GST_CAT_STATES, element,
      "current %s, old_pending %s, next %s, old return %s",
      gst_element_state_get_name (current),
      gst_element_state_get_name (old_pending),
      gst_element_state_get_name (next),
      gst_element_state_change_return_get_name (old_ret));

  /* if the element was busy doing a state change, we just update the
   * target state, it'll get to it async then. */
  if (old_pending != GST_STATE_VOID_PENDING) {
    /* upwards state change will happen ASYNC */
    if (old_pending <= state)
      goto was_busy;
    /* element is going to this state already */
    else if (next == state)
      goto was_busy;
    /* element was performing an ASYNC upward state change and
     * we request to go downward again. Start from the next pending
     * state then. */
    else if (next > state
        && GST_STATE_RETURN (element) == GST_STATE_CHANGE_ASYNC) {
      current = next;
    }
  }
  next = GST_STATE_GET_NEXT (current, state);
  /* now we store the next state */
  GST_STATE_NEXT (element) = next;
  /* mark busy, we need to check that there is actually a state change
   * to be done else we could accidentally override SUCCESS/NO_PREROLL and
   * the default element change_state function has no way to know what the
   * old value was... could consider this a FIXME...*/
  if (current != next)
    GST_STATE_RETURN (element) = GST_STATE_CHANGE_ASYNC;

  transition = (GstStateChange) GST_STATE_TRANSITION (current, next);

  GST_CAT_DEBUG_OBJECT (GST_CAT_STATES, element,
      "%s: setting state from %s to %s",
      (next != state ? "intermediate" : "final"),
      gst_element_state_get_name (current), gst_element_state_get_name (next));

  /* now signal any waiters, they will error since the cookie was incremented */
  GST_STATE_BROADCAST (element);

  GST_OBJECT_UNLOCK (element);

  ret = gst_element_change_state (element, transition);

  GST_STATE_UNLOCK (element);

  GST_CAT_DEBUG_OBJECT (GST_CAT_STATES, element, "returned %s",
      gst_element_state_change_return_get_name (ret));

  return ret;

was_busy:
  {
    GST_STATE_RETURN (element) = GST_STATE_CHANGE_ASYNC;
    GST_CAT_DEBUG_OBJECT (GST_CAT_STATES, element,
        "element was busy with async state change");
    GST_OBJECT_UNLOCK (element);

    GST_STATE_UNLOCK (element);

    return GST_STATE_CHANGE_ASYNC;
  }
}

/**
 * gst_element_change_state:
 * @element: a #GstElement
 * @transition: the requested transition
 *
 * Perform @transition on @element.
 *
 * This function must be called with STATE_LOCK held and is mainly used
 * internally.
 *
 * Returns: the #GstStateChangeReturn of the state transition.
 */
GstStateChangeReturn
gst_element_change_state (GstElement * element, GstStateChange transition)
{
  GstElementClass *oclass;
  GstStateChangeReturn ret = GST_STATE_CHANGE_SUCCESS;

  oclass = GST_ELEMENT_GET_CLASS (element);

  /* call the state change function so it can set the state */
  if (oclass->change_state)
    ret = (oclass->change_state) (element, transition);
  else
    ret = GST_STATE_CHANGE_FAILURE;

  switch (ret) {
    case GST_STATE_CHANGE_FAILURE:
      GST_CAT_INFO_OBJECT (GST_CAT_STATES, element,
          "have FAILURE change_state return");
      /* state change failure */
      gst_element_abort_state (element);
      break;
    case GST_STATE_CHANGE_ASYNC:
    {
      GstState target;

      GST_CAT_DEBUG_OBJECT (GST_CAT_STATES, element,
          "element will change state ASYNC");

      target = GST_STATE_TARGET (element);

      if (target > GST_STATE_READY)
        goto async;

      /* else we just continue the state change downwards */
      GST_CAT_INFO_OBJECT (GST_CAT_STATES, element,
          "forcing commit state %s <= %s",
          gst_element_state_get_name (target),
          gst_element_state_get_name (GST_STATE_READY));

      ret = gst_element_continue_state (element, GST_STATE_CHANGE_SUCCESS);
      break;
    }
    case GST_STATE_CHANGE_SUCCESS:
      GST_CAT_DEBUG_OBJECT (GST_CAT_STATES, element,
          "element changed state SUCCESS");
      /* we can commit the state now which will proceeed to
       * the next state */
      ret = gst_element_continue_state (element, ret);
      break;
    case GST_STATE_CHANGE_NO_PREROLL:
      GST_CAT_DEBUG_OBJECT (GST_CAT_STATES, element,
          "element changed state NO_PREROLL");
      /* we can commit the state now which will proceeed to
       * the next state */
      ret = gst_element_continue_state (element, ret);
      break;
    default:
      goto invalid_return;
  }

  GST_CAT_LOG_OBJECT (GST_CAT_STATES, element, "exit state change %d", ret);

  return ret;

async:
  GST_CAT_LOG_OBJECT (GST_CAT_STATES, element, "exit async state change %d",
      ret);

  return ret;

  /* ERROR */
invalid_return:
  {
    GST_OBJECT_LOCK (element);
    /* somebody added a GST_STATE_ and forgot to do stuff here ! */
    g_critical ("%s: unknown return value %d from a state change function",
        GST_ELEMENT_NAME (element), ret);

    /* we are in error now */
    ret = GST_STATE_CHANGE_FAILURE;
    GST_STATE_RETURN (element) = ret;
    GST_OBJECT_UNLOCK (element);

    return ret;
  }
}

/* gst_iterator_fold functions for pads_activate
 * Note how we don't stop the iterator when we fail an activation. This is
 * probably a FIXME since when one pad activation fails, we don't want to
 * continue our state change. */
static gboolean
activate_pads (GstPad * pad, GValue * ret, gboolean * active)
{
  if (!gst_pad_set_active (pad, *active))
    g_value_set_boolean (ret, FALSE);

  /* unref the object that was reffed for us by _fold */
  gst_object_unref (pad);
  return TRUE;
}

/* set the caps on the pad to NULL */
static gboolean
clear_caps (GstPad * pad, GValue * ret, gboolean * active)
{
  gst_pad_set_caps (pad, NULL);
  gst_object_unref (pad);
  return TRUE;
}

/* returns false on error or early cutout (will never happen because the fold
 * function always returns TRUE, see FIXME above) of the fold, true if all
 * pads in @iter were (de)activated successfully. */
static gboolean
iterator_activate_fold_with_resync (GstIterator * iter,
    GstIteratorFoldFunction func, gpointer user_data)
{
  GstIteratorResult ires;
  GValue ret = { 0 };

  /* no need to unset this later, it's just a boolean */
  g_value_init (&ret, G_TYPE_BOOLEAN);
  g_value_set_boolean (&ret, TRUE);

  while (1) {
    ires = gst_iterator_fold (iter, func, &ret, user_data);
    switch (ires) {
      case GST_ITERATOR_RESYNC:
        /* need to reset the result again */
        g_value_set_boolean (&ret, TRUE);
        gst_iterator_resync (iter);
        break;
      case GST_ITERATOR_DONE:
        /* all pads iterated, return collected value */
        goto done;
      default:
        /* iterator returned _ERROR or premature end with _OK,
         * mark an error and exit */
        g_value_set_boolean (&ret, FALSE);
        goto done;
    }
  }
done:
  /* return collected value */
  return g_value_get_boolean (&ret);
}

/* is called with STATE_LOCK
 *
 * Pads are activated from source pads to sinkpads.
 */
static gboolean
gst_element_pads_activate (GstElement * element, gboolean active)
{
  GstIterator *iter;
  gboolean res;

  GST_CAT_DEBUG_OBJECT (GST_CAT_ELEMENT_PADS, element,
      "pads_activate with active %d", active);

  iter = gst_element_iterate_src_pads (element);
  res =
      iterator_activate_fold_with_resync (iter,
      (GstIteratorFoldFunction) activate_pads, &active);
  gst_iterator_free (iter);
  if (G_UNLIKELY (!res))
    goto src_failed;

  iter = gst_element_iterate_sink_pads (element);
  res =
      iterator_activate_fold_with_resync (iter,
      (GstIteratorFoldFunction) activate_pads, &active);
  gst_iterator_free (iter);
  if (G_UNLIKELY (!res))
    goto sink_failed;

  if (!active) {
    /* clear the caps on all pads, this should never fail */
    iter = gst_element_iterate_pads (element);
    res =
        iterator_activate_fold_with_resync (iter,
        (GstIteratorFoldFunction) clear_caps, &active);
    gst_iterator_free (iter);
    if (G_UNLIKELY (!res))
      goto caps_failed;
  }

  GST_CAT_DEBUG_OBJECT (GST_CAT_ELEMENT_PADS, element,
      "pads_activate successful");

  return TRUE;

  /* ERRORS */
src_failed:
  {
    GST_CAT_DEBUG_OBJECT (GST_CAT_ELEMENT_PADS, element,
        "source pads_activate failed");
    return FALSE;
  }
sink_failed:
  {
    GST_CAT_DEBUG_OBJECT (GST_CAT_ELEMENT_PADS, element,
        "sink pads_activate failed");
    return FALSE;
  }
caps_failed:
  {
    GST_CAT_DEBUG_OBJECT (GST_CAT_ELEMENT_PADS, element,
        "failed to clear caps on pads");
    return FALSE;
  }
}

/* is called with STATE_LOCK */
static GstStateChangeReturn
gst_element_change_state_func (GstElement * element, GstStateChange transition)
{
  GstState state, next;
  GstStateChangeReturn result = GST_STATE_CHANGE_SUCCESS;
  GstClock **clock_p;

  g_return_val_if_fail (GST_IS_ELEMENT (element), GST_STATE_CHANGE_FAILURE);

  state = (GstState) GST_STATE_TRANSITION_CURRENT (transition);
  next = GST_STATE_TRANSITION_NEXT (transition);

  /* if the element already is in the given state, we just return success */
  if (next == GST_STATE_VOID_PENDING || state == next)
    goto was_ok;

  GST_CAT_LOG_OBJECT (GST_CAT_STATES, element,
      "default handler tries setting state from %s to %s (%04x)",
      gst_element_state_get_name (state),
      gst_element_state_get_name (next), transition);

  switch (transition) {
    case GST_STATE_CHANGE_NULL_TO_READY:
      break;
    case GST_STATE_CHANGE_READY_TO_PAUSED:
      if (!gst_element_pads_activate (element, TRUE)) {
        result = GST_STATE_CHANGE_FAILURE;
      }
      break;
    case GST_STATE_CHANGE_PAUSED_TO_PLAYING:
      break;
    case GST_STATE_CHANGE_PLAYING_TO_PAUSED:
      break;
    case GST_STATE_CHANGE_PAUSED_TO_READY:
    case GST_STATE_CHANGE_READY_TO_NULL:
      /* deactivate pads in both cases, since they are activated on
         ready->paused but the element might not have made it to paused */
      if (!gst_element_pads_activate (element, FALSE)) {
        result = GST_STATE_CHANGE_FAILURE;
      } else {
        gst_element_set_base_time (element, 0);
      }

      /* In null state release the reference to the clock */
      GST_OBJECT_LOCK (element);
      clock_p = &element->clock;
      gst_object_replace ((GstObject **) clock_p, NULL);
      GST_OBJECT_UNLOCK (element);
      break;
    default:
      /* this will catch real but unhandled state changes;
       * can only be caused by:
       * - a new state was added
       * - somehow the element was asked to jump across an intermediate state
       */
      g_warning ("Unhandled state change from %s to %s",
          gst_element_state_get_name (state),
          gst_element_state_get_name (next));
      break;
  }
  return result;

was_ok:
  {
    GST_OBJECT_LOCK (element);
    result = GST_STATE_RETURN (element);
    GST_CAT_DEBUG_OBJECT (GST_CAT_STATES, element,
        "element is already in the %s state",
        gst_element_state_get_name (state));
    GST_OBJECT_UNLOCK (element);

    return result;
  }
}

/**
 * gst_element_get_factory:
 * @element: a #GstElement to request the element factory of.
 *
 * Retrieves the factory that was used to create this element.
 *
 * Returns: (transfer none): the #GstElementFactory used for creating this
 *     element. no refcounting is needed.
 */
GstElementFactory *
gst_element_get_factory (GstElement * element)
{
  g_return_val_if_fail (GST_IS_ELEMENT (element), NULL);

  return GST_ELEMENT_GET_CLASS (element)->elementfactory;
}

static void
gst_element_dispose (GObject * object)
{
  GstElement *element = GST_ELEMENT_CAST (object);
  GstClock **clock_p;
  GstBus **bus_p;

  GST_CAT_INFO_OBJECT (GST_CAT_REFCOUNTING, element, "dispose");

  if (GST_STATE (element) != GST_STATE_NULL)
    goto not_null;

  GST_CAT_DEBUG_OBJECT (GST_CAT_ELEMENT_PADS, element,
      "removing %d pads", g_list_length (element->pads));
  /* first we break all our links with the outside */
  while (element->pads && element->pads->data) {
    /* don't call _remove_pad with NULL */
    gst_element_remove_pad (element, GST_PAD_CAST (element->pads->data));
  }
  if (G_UNLIKELY (element->pads != NULL)) {
    g_critical ("could not remove pads from element %s",
        GST_STR_NULL (GST_OBJECT_NAME (object)));
  }

  GST_OBJECT_LOCK (element);
  clock_p = &element->clock;
  bus_p = &element->bus;
  gst_object_replace ((GstObject **) clock_p, NULL);
  gst_object_replace ((GstObject **) bus_p, NULL);
  GST_OBJECT_UNLOCK (element);

  GST_CAT_INFO_OBJECT (GST_CAT_REFCOUNTING, element, "parent class dispose");

  G_OBJECT_CLASS (parent_class)->dispose (object);

  return;

  /* ERRORS */
not_null:
  {
    gboolean is_locked;

    is_locked = GST_OBJECT_FLAG_IS_SET (element, GST_ELEMENT_LOCKED_STATE);
    g_critical
        ("\nTrying to dispose element %s, but it is in %s%s instead of the NULL"
        " state.\n"
        "You need to explicitly set elements to the NULL state before\n"
        "dropping the final reference, to allow them to clean up.\n"
        "This problem may also be caused by a refcounting bug in the\n"
        "application or some element.\n",
        GST_OBJECT_NAME (element),
        gst_element_state_get_name (GST_STATE (element)),
        is_locked ? " (locked)" : "");
    return;
  }
}

static void
gst_element_finalize (GObject * object)
{
  GstElement *element = GST_ELEMENT_CAST (object);

  GST_CAT_INFO_OBJECT (GST_CAT_REFCOUNTING, element, "finalize");

  GST_STATE_LOCK (element);
  if (element->state_cond)
    g_cond_free (element->state_cond);
  element->state_cond = NULL;
  GST_STATE_UNLOCK (element);
  g_static_rec_mutex_free (element->state_lock);
  g_slice_free (GStaticRecMutex, element->state_lock);
  element->state_lock = NULL;

  GST_CAT_INFO_OBJECT (GST_CAT_REFCOUNTING, element, "finalize parent");

  G_OBJECT_CLASS (parent_class)->finalize (object);
}

#if !defined(GST_DISABLE_LOADSAVE) && !defined(GST_REMOVE_DEPRECATED)
/**
 * gst_element_save_thyself:
 * @element: a #GstElement to save.
 * @parent: the xml parent node.
 *
 * Saves the element as part of the given XML structure.
 *
 * Returns: the new #xmlNodePtr.
 */
static xmlNodePtr
gst_element_save_thyself (GstObject * object, xmlNodePtr parent)
{
  GList *pads;
  GstElementClass *oclass;
  GParamSpec **specs, *spec;
  guint nspecs;
  guint i;
  GValue value = { 0, };
  GstElement *element;

  g_return_val_if_fail (GST_IS_ELEMENT (object), parent);

  element = GST_ELEMENT_CAST (object);

  oclass = GST_ELEMENT_GET_CLASS (element);

  xmlNewChild (parent, NULL, (xmlChar *) "name",
      (xmlChar *) GST_ELEMENT_NAME (element));

  if (oclass->elementfactory != NULL) {
    GstElementFactory *factory = (GstElementFactory *) oclass->elementfactory;

    xmlNewChild (parent, NULL, (xmlChar *) "type",
        (xmlChar *) GST_PLUGIN_FEATURE (factory)->name);
  }

  /* params */
  specs = g_object_class_list_properties (G_OBJECT_GET_CLASS (object), &nspecs);

  for (i = 0; i < nspecs; i++) {
    spec = specs[i];
    if (spec->flags & G_PARAM_READABLE) {
      xmlNodePtr param;
      char *contents;

      g_value_init (&value, spec->value_type);

      g_object_get_property (G_OBJECT (element), spec->name, &value);
      param = xmlNewChild (parent, NULL, (xmlChar *) "param", NULL);
      xmlNewChild (param, NULL, (xmlChar *) "name", (xmlChar *) spec->name);

      if (G_IS_PARAM_SPEC_STRING (spec))
        contents = g_value_dup_string (&value);
      else if (G_IS_PARAM_SPEC_ENUM (spec))
        contents = g_strdup_printf ("%d", g_value_get_enum (&value));
      else if (G_IS_PARAM_SPEC_INT64 (spec))
        contents = g_strdup_printf ("%" G_GINT64_FORMAT,
            g_value_get_int64 (&value));
      else if (GST_VALUE_HOLDS_STRUCTURE (&value)) {
        if (g_value_get_boxed (&value) != NULL) {
          contents = g_strdup_value_contents (&value);
        } else {
          contents = g_strdup ("NULL");
        }
      } else
        contents = g_strdup_value_contents (&value);

      xmlNewChild (param, NULL, (xmlChar *) "value", (xmlChar *) contents);
      g_free (contents);

      g_value_unset (&value);
    }
  }

  g_free (specs);

  pads = g_list_last (GST_ELEMENT_PADS (element));

  while (pads) {
    GstPad *pad = GST_PAD_CAST (pads->data);

    /* figure out if it's a direct pad or a ghostpad */
    if (GST_ELEMENT_CAST (GST_OBJECT_PARENT (pad)) == element) {
      xmlNodePtr padtag = xmlNewChild (parent, NULL, (xmlChar *) "pad", NULL);

      gst_object_save_thyself (GST_OBJECT_CAST (pad), padtag);
    }
    pads = g_list_previous (pads);
  }

  return parent;
}

static void
gst_element_restore_thyself (GstObject * object, xmlNodePtr self)
{
  xmlNodePtr children;
  GstElement *element;
  gchar *name = NULL;
  gchar *value = NULL;

  element = GST_ELEMENT_CAST (object);
  g_return_if_fail (element != NULL);

  /* parameters */
  children = self->xmlChildrenNode;
  while (children) {
    if (!strcmp ((char *) children->name, "param")) {
      xmlNodePtr child = children->xmlChildrenNode;

      while (child) {
        if (!strcmp ((char *) child->name, "name")) {
          name = (gchar *) xmlNodeGetContent (child);
        } else if (!strcmp ((char *) child->name, "value")) {
          value = (gchar *) xmlNodeGetContent (child);
        }
        child = child->next;
      }
      /* FIXME: can this just be g_object_set ? */
      gst_util_set_object_arg (G_OBJECT (element), name, value);
      /* g_object_set (G_OBJECT (element), name, value, NULL); */
      g_free (name);
      g_free (value);
    }
    children = children->next;
  }

  /* pads */
  children = self->xmlChildrenNode;
  while (children) {
    if (!strcmp ((char *) children->name, "pad")) {
      gst_pad_load_and_link (children, GST_OBJECT_CAST (element));
    }
    children = children->next;
  }

  if (GST_OBJECT_CLASS (parent_class)->restore_thyself)
    (GST_OBJECT_CLASS (parent_class)->restore_thyself) (object, self);
}
#endif /* GST_DISABLE_LOADSAVE */

static void
gst_element_set_bus_func (GstElement * element, GstBus * bus)
{
  GstBus **bus_p;

  g_return_if_fail (GST_IS_ELEMENT (element));

  GST_CAT_DEBUG_OBJECT (GST_CAT_PARENTAGE, element, "setting bus to %p", bus);

  GST_OBJECT_LOCK (element);
  bus_p = &GST_ELEMENT_BUS (element);
  gst_object_replace ((GstObject **) bus_p, GST_OBJECT_CAST (bus));
  GST_OBJECT_UNLOCK (element);
}

/**
 * gst_element_set_bus:
 * @element: a #GstElement to set the bus of.
 * @bus: (transfer none): the #GstBus to set.
 *
 * Sets the bus of the element. Increases the refcount on the bus.
 * For internal use only, unless you're testing elements.
 *
 * MT safe.
 */
void
gst_element_set_bus (GstElement * element, GstBus * bus)
{
  GstElementClass *oclass;

  g_return_if_fail (GST_IS_ELEMENT (element));

  oclass = GST_ELEMENT_GET_CLASS (element);

  if (oclass->set_bus)
    oclass->set_bus (element, bus);
}

/**
 * gst_element_get_bus:
 * @element: a #GstElement to get the bus of.
 *
 * Returns the bus of the element. Note that only a #GstPipeline will provide a
 * bus for the application.
 *
 * Returns: (transfer full): the element's #GstBus. unref after usage.
 *
 * MT safe.
 */
GstBus *
gst_element_get_bus (GstElement * element)
{
  GstBus *result = NULL;

  g_return_val_if_fail (GST_IS_ELEMENT (element), result);

  GST_OBJECT_LOCK (element);
  if ((result = GST_ELEMENT_BUS (element)))
    gst_object_ref (result);
  GST_OBJECT_UNLOCK (element);

  GST_CAT_DEBUG_OBJECT (GST_CAT_BUS, element, "got bus %" GST_PTR_FORMAT,
      result);

  return result;
}
