/*
 * Copyright (C) <1999> Erik Walthinsen <omega@cse.ogi.edu>
 * Copyright (C) <2003> David A. Schleef <ds@schleef.org>
 * Copyright (C) <2006> Wim Taymans <wim@fluendo.com>
 * Copyright (C) 2020 Huawei Technologies Co., Ltd.
 *   @Author: Julian Bouzas <julian.bouzas@collabora.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA.
 */

#ifndef __GST_ISOMP4_ELEMENTS_H__
#define __GST_ISOMP4_ELEMENTS_H__

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>

G_BEGIN_DECLS

void isomp4_element_init (GstPlugin * plugin);

GST_ELEMENT_REGISTER_DECLARE (qtdemux);
#ifndef GSTREAMER_LITE
GST_ELEMENT_REGISTER_DECLARE (rtpxqtdepay);
GST_ELEMENT_REGISTER_DECLARE (qtmux);
GST_ELEMENT_REGISTER_DECLARE (qtmoovrecover);
#endif // GSTREAMER_LITE

G_END_DECLS

#endif /* __GST_ISOMP4_ELEMENTS_H__ */
