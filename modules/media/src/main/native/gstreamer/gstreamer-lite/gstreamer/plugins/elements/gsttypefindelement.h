/* GStreamer
 * Copyright (C) 2003 Benjamin Otte <in7y118@public.uni-hamburg.de>
 *
 * gsttypefindelement.h: element that detects type of stream
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


#ifndef __GST_TYPE_FIND_ELEMENT_H__
#define __GST_TYPE_FIND_ELEMENT_H__

#include <gst/gstinfo.h>
#include <gst/gstelement.h>

G_BEGIN_DECLS



#define GST_TYPE_TYPE_FIND_ELEMENT		(gst_type_find_element_get_type ())
#define GST_TYPE_FIND_ELEMENT(obj) 		(G_TYPE_CHECK_INSTANCE_CAST ((obj), GST_TYPE_TYPE_FIND_ELEMENT, GstTypeFindElement))
#define GST_IS_TYPE_FIND_ELEMENT(obj) 		(G_TYPE_CHECK_INSTANCE_TYPE ((obj), GST_TYPE_TYPE_FIND_ELEMENT))
#define GST_TYPE_FIND_ELEMENT_CLASS(klass) 	(G_TYPE_CHECK_CLASS_CAST ((klass), GST_TYPE_TYPE_FIND_ELEMENT, GstTypeFindElementClass))
#define GST_IS_TYPE_FIND_ELEMENT_CLASS(klass) 	(G_TYPE_CHECK_CLASS_TYPE ((klass), GST_TYPE_TYPE_FIND_ELEMENT))
#define GST_TYPE_FIND_ELEMENT_GET_CLASS(obj) 	(G_TYPE_INSTANCE_GET_CLASS ((obj), GST_TYPE_TYPE_FIND_ELEMENT, GstTypeFindElementClass))

typedef struct _GstTypeFindElement 		GstTypeFindElement;
typedef struct _GstTypeFindElementClass 	GstTypeFindElementClass;

/**
 * GstTypeFindElement:
 *
 * Opaque #GstTypeFindElement data structure
 */
struct _GstTypeFindElement {
  GstElement		element;

  GstPad *		sink;
  GstPad *		src;

  guint			min_probability;
  guint			max_probability;
  GstCaps *		caps;

  guint			mode;
  GstBuffer *		store;

  GList *               cached_events;
  GstCaps *             force_caps;
};

struct _GstTypeFindElementClass {
  GstElementClass 	parent_class;

  /* signals */
  void 			(*have_type) 	(GstTypeFindElement *element,
					 guint		probability,
					 const GstCaps *	caps);
};

GType gst_type_find_element_get_type (void);

G_END_DECLS

#endif /* __GST_TYPE_FIND_ELEMENT_H__ */
