/* GStreamer
 * Copyright (C) <1999> Erik Walthinsen <omega@cse.ogi.edu>
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

#ifndef __QTDEMUX_TAGS_H__
#define __QTDEMUX_TAGS_H__

#include <gst/gst.h>
#include "qtdemux.h"

void qtdemux_parse_udta (GstQTDemux * qtdemux, GstTagList * taglist, GNode * udta);
void qtdemux_handle_xmp_taglist (GstQTDemux * qtdemux, GstTagList * taglist,
    GstTagList * xmptaglist);

#endif
