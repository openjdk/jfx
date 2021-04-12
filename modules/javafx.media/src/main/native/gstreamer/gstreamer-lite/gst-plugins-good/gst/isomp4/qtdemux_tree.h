/* GStreamer
 * Copyright (C) <1999> Erik Walthinsen <omega@cse.ogi.edu>
 * Copyright (C) <2003> David A. Schleef <ds@schleef.org>
 * Copyright (C) <2006> Wim Taymans <wim@fluendo.com>
 * Copyright (C) <2007> Julien Moutte <julien@fluendo.com>
 * Copyright (C) <2009> Tim-Philipp Müller <tim centricular net>
 * Copyright (C) <2009> STEricsson <benjamin.gaignard@stericsson.com>
 * Copyright (C) <2013> Sreerenj Balachandran <sreerenj.balachandran@intel.com>
 * Copyright (C) <2013> Intel Corporation
 * Copyright (C) <2014> Centricular Ltd
 * Copyright (C) <2015> YouView TV Ltd.
 * Copyright (C) <2016> British Broadcasting Corporation
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
#include <gst/gst.h>
#include <gst/base/gstbytereader.h>

#ifndef __QTDEMUX_TREE_H__
#define __QTDEMUX_TREE_H__

G_BEGIN_DECLS

GNode *qtdemux_tree_get_child_by_type (GNode * node, guint32 fourcc);
GNode *qtdemux_tree_get_child_by_type_full (GNode * node,
    guint32 fourcc, GstByteReader * parser);
GNode *qtdemux_tree_get_sibling_by_type (GNode * node, guint32 fourcc);
GNode *qtdemux_tree_get_sibling_by_type_full (GNode * node,
    guint32 fourcc, GstByteReader * parser);
GNode *qtdemux_tree_get_child_by_index (GNode * node, guint index);

G_END_DECLS

#endif
