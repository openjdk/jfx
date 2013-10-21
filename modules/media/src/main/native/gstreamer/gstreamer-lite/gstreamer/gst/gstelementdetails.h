/* GStreamer
 * Copyright (C) 1999,2000 Erik Walthinsen <omega@cse.ogi.edu>
 *               2000,2004 Wim Taymans <wim@fluendo.com>
 *
 * gstelement.h: Header for GstElement
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

#ifndef __GST_ELEMENT_DETAILS_H__
#define __GST_ELEMENT_DETAILS_H__

G_BEGIN_DECLS

static inline void
__gst_element_details_clear (GstElementDetails * dp)
{
  g_free (dp->longname);
  g_free (dp->klass);
  g_free (dp->description);
  g_free (dp->author);
  memset (dp, 0, sizeof (GstElementDetails));
}

#define VALIDATE_SET(__dest, __src, __entry)                            \
G_STMT_START {                                                          \
  if (g_utf8_validate (__src->__entry, -1, NULL)) {                     \
    __dest->__entry = g_strdup (__src->__entry);                        \
  } else {                                                              \
    g_warning ("Invalid UTF-8 in " G_STRINGIFY (__entry) ": %s",        \
        __src->__entry);                                                \
    __dest->__entry = g_strdup ("[ERROR: invalid UTF-8]");              \
  }                                                                     \
} G_STMT_END

static inline void
__gst_element_details_set (GstElementDetails * dest,
    const GstElementDetails * src)
{
  VALIDATE_SET (dest, src, longname);
  VALIDATE_SET (dest, src, klass);
  VALIDATE_SET (dest, src, description);
  VALIDATE_SET (dest, src, author);
}

static inline void
__gst_element_details_copy (GstElementDetails * dest,
    const GstElementDetails * src)
{
  __gst_element_details_clear (dest);
  __gst_element_details_set (dest, src);
}

G_END_DECLS

#endif /* __GST_ELEMENT_DETAILS_H__ */
