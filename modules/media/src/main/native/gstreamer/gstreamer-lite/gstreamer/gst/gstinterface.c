/* GStreamer
 *
 * Copyright (C) 1999,2000 Erik Walthinsen <omega@cse.ogi.edu>
 *                    2000 Wim Taymans <wtay@chello.be>
 *
 * gstinterface.c: Interface functions
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
 * SECTION:gstimplementsinterface
 * @short_description: Core interface implemented by #GstElement instances that
 * allows runtime querying of interface availabillity
 * @see_also: #GstElement
 *
 * Provides interface functionality on per instance basis and not per class
 * basis, which is the case for gobject.
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "gst_private.h"
#include "gstinterface.h"

static void
gst_implements_interface_class_init (GstImplementsInterfaceClass * ifklass);
static gboolean
gst_implements_interface_supported_default (GstImplementsInterface * iface,
    GType iface_type);

GType
gst_implements_interface_get_type (void)
{
  static volatile gsize gst_interface_type = 0;

  if (g_once_init_enter (&gst_interface_type)) {
    GType _type;
    static const GTypeInfo gst_interface_info = {
      sizeof (GstImplementsInterfaceClass),
      (GBaseInitFunc) gst_implements_interface_class_init,
      NULL,
      NULL,
      NULL,
      NULL,
      0,
      0,
      NULL,
      NULL
    };

    _type = g_type_register_static (G_TYPE_INTERFACE,
        "GstImplementsInterface", &gst_interface_info, 0);

    g_type_interface_add_prerequisite (_type, GST_TYPE_ELEMENT);
    g_once_init_leave (&gst_interface_type, _type);
  }

  return gst_interface_type;
}

static void
gst_implements_interface_class_init (GstImplementsInterfaceClass * klass)
{
  klass->supported = gst_implements_interface_supported_default;
}

static gboolean
gst_implements_interface_supported_default (GstImplementsInterface * interface,
    GType iface_type)
{
  /* Well, if someone didn't set the virtual function,
   * then something is clearly wrong. So big no-no here */

  return FALSE;
}

/**
 * gst_element_implements_interface:
 * @element: #GstElement to check for the implementation of the interface
 * @iface_type: (final) type of the interface which we want to be implemented
 *
 * Test whether the given element implements a certain interface of type
 * iface_type, and test whether it is supported for this specific instance.
 *
 * Returns: whether or not the element implements the interface.
 */

gboolean
gst_element_implements_interface (GstElement * element, GType iface_type)
{
  g_return_val_if_fail (GST_IS_ELEMENT (element), FALSE);

  if (G_TYPE_CHECK_INSTANCE_TYPE (G_OBJECT (element), iface_type)) {
    GstImplementsInterface *iface;
    GstImplementsInterfaceClass *ifclass;

    iface = G_TYPE_CHECK_INSTANCE_CAST (G_OBJECT (element),
        iface_type, GstImplementsInterface);
    ifclass = GST_IMPLEMENTS_INTERFACE_GET_CLASS (iface);

    /* element implements iface_type but not GstImplementsInterface, so
     * just assume the other interface is implemented unconditionally */
    if (ifclass == NULL)
      return TRUE;

    if (ifclass->supported != NULL &&
        ifclass->supported (iface, iface_type) == TRUE) {
      return TRUE;
    }
  }

  return FALSE;
}

/**
 * gst_implements_interface_cast:
 * @from: the object (any sort) from which to cast to the interface
 * @type: the interface type to cast to
 *
 * cast a given object to an interface type, and check whether this
 * interface is supported for this specific instance.
 *
 * Returns: (transfer none): a gpointer to the interface type
 */

gpointer
gst_implements_interface_cast (gpointer from, GType iface_type)
{
  GstImplementsInterface *iface;

  /* check cast, give warning+fail if it's invalid */
  if (!(iface = G_TYPE_CHECK_INSTANCE_CAST (from, iface_type,
              GstImplementsInterface))) {
    return NULL;
  }

  /* if we're an element, take care that this interface
   * is actually implemented */
  if (GST_IS_ELEMENT (from)) {
    g_return_val_if_fail (gst_element_implements_interface (GST_ELEMENT (from),
            iface_type), NULL);
  }

  return iface;
}

/**
 * gst_implements_interface_check:
 * @from: the object (any sort) from which to check from for the interface
 * @type: the interface type to check for
 *
 * check a given object for an interface implementation, and check
 * whether this interface is supported for this specific instance.
 *
 * Returns: whether or not the object implements the given interface
 */

gboolean
gst_implements_interface_check (gpointer from, GType type)
{
  /* check cast, return FALSE if it fails, don't give a warning... */
  if (!G_TYPE_CHECK_INSTANCE_TYPE (from, type)) {
    return FALSE;
  }

  /* now, if we're an element (or derivative), is this thing
   * actually implemented for real? */
  if (GST_IS_ELEMENT (from)) {
    if (!gst_element_implements_interface (GST_ELEMENT (from), type)) {
      return FALSE;
    }
  }

  return TRUE;
}
