/* GStreamer
 * Copyright (C) 2003 Benjamin Otte <in7y118@public.uni-hamburg.de>
 *
 * gsttypefind.h: typefinding subsystem
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


#ifndef __GST_TYPE_FIND_H__
#define __GST_TYPE_FIND_H__

#include <gst/gstcaps.h>
#include <gst/gstplugin.h>
#include <gst/gstpluginfeature.h>

G_BEGIN_DECLS

#ifndef GSTREAMER_LITE
#define GST_TYPE_TYPE_FIND  (gst_type_find_get_type())
#endif // GSTREAMER_LITE

typedef struct _GstTypeFind GstTypeFind;

/**
 * GstTypeFindFunction:
 * @find: A #GstTypeFind structure
 * @user_data: optional data to pass to the function
 *
 * A function that will be called by typefinding.
 */
typedef void (* GstTypeFindFunction) (GstTypeFind *find, gpointer user_data);

/**
 * GstTypeFindProbability:
 * @GST_TYPE_FIND_NONE: type undetected.
 * @GST_TYPE_FIND_MINIMUM: unlikely typefind.
 * @GST_TYPE_FIND_POSSIBLE: possible type detected.
 * @GST_TYPE_FIND_LIKELY: likely a type was detected.
 * @GST_TYPE_FIND_NEARLY_CERTAIN: nearly certain that a type was detected.
 * @GST_TYPE_FIND_MAXIMUM: very certain a type was detected.
 *
 * The probability of the typefind function. Higher values have more certainty
 * in doing a reliable typefind.
 */
typedef enum {
  GST_TYPE_FIND_NONE = 0,
  GST_TYPE_FIND_MINIMUM = 1,
  GST_TYPE_FIND_POSSIBLE = 50,
  GST_TYPE_FIND_LIKELY = 80,
  GST_TYPE_FIND_NEARLY_CERTAIN = 99,
  GST_TYPE_FIND_MAXIMUM = 100
} GstTypeFindProbability;

/**
 * GstTypeFind:
 * @peek: Method to peek data.
 * @suggest: Method to suggest #GstCaps with a given probability.
 * @data: The data used by the caller of the typefinding function.
 * @get_length: Returns the length of current data.
 *
 * Object that stores typefind callbacks. To use with #GstTypeFindFactory.
 */
struct _GstTypeFind {
  /* private to the caller of the typefind function */
  const guint8 *  (* peek)       (gpointer         data,
                                  gint64           offset,
                                  guint            size);

  void            (* suggest)    (gpointer         data,
                                  guint            probability,
                                  GstCaps         *caps);

  gpointer         data;

  /* optional */
  guint64         (* get_length) (gpointer data);

  /* <private> */
  gpointer _gst_reserved[GST_PADDING];
};

#ifndef GSTREAMER_LITE
GST_API
GType     gst_type_find_get_type   (void);
#endif // GSTREAMER_LITE

/* typefind function interface */

GST_API
const guint8 *  gst_type_find_peek       (GstTypeFind   * find,
                                          gint64          offset,
                                          guint           size);
GST_API
void            gst_type_find_suggest    (GstTypeFind   * find,
                                          guint           probability,
                                          GstCaps       * caps);
GST_API
void            gst_type_find_suggest_simple (GstTypeFind * find,
                                              guint         probability,
                                              const char  * media_type,
                                              const char  * fieldname, ...);
GST_API
guint64   gst_type_find_get_length (GstTypeFind   * find);

/* registration interface */

GST_API
gboolean  gst_type_find_register   (GstPlugin            * plugin,
                                    const gchar          * name,
                                    guint                  rank,
                                    GstTypeFindFunction    func,
                                    const gchar          * extensions,
                                    GstCaps              * possible_caps,
                                    gpointer               data,
                                    GDestroyNotify         data_notify);

G_END_DECLS

#endif /* __GST_TYPE_FIND_H__ */
