/* GStreamer
 *
 * Copyright (C) <2005> Stefan Kost <ensonic at users dot sf dot net>
 *
 * gstcontrollerprivate.h: dynamic parameter control subsystem
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

#ifndef __GST_CONTROLLER_PRIVATE_H__
#define __GST_CONTROLLER_PRIVATE_H__

#include <glib.h>
#include <glib-object.h>
#include <gst/gst.h>

#include <gst/controller/gstcontroller.h>
#include <gst/controller/gstcontrolsource.h>

G_BEGIN_DECLS

/**
 * GstControlledProperty:
 */
typedef struct _GstControlledProperty
{
  GParamSpec *pspec;            /* GParamSpec for this property */
  gchar *name;                  /* name of the property */
  GstControlSource *csource;    /* GstControlSource for this property */
  gboolean disabled;
  GValue last_value;
} GstControlledProperty;

#define GST_CONTROLLED_PROPERTY(obj)    ((GstControlledProperty *)(obj))

extern GQuark priv_gst_controller_key;

G_END_DECLS

#endif /* __GST_CONTROLLER_PRIVATE_H__ */
