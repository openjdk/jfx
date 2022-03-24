/*
 * Copyright (C) 2020 Huawei Technologies Co., Ltd.
 *   @Author: Stéphane Cerveau <scerveau@collabora.com>
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

#ifndef __GST_CORE_ELEMENTS_ELEMENTS_H__
#define __GST_CORE_ELEMENTS_ELEMENTS_H__

#include <gst/gst.h>

G_BEGIN_DECLS

#ifndef GSTREAMER_LITE
GST_ELEMENT_REGISTER_DECLARE (capsfilter);
GST_ELEMENT_REGISTER_DECLARE (clocksync);
GST_ELEMENT_REGISTER_DECLARE (concat);
GST_ELEMENT_REGISTER_DECLARE (dataurisrc);
GST_ELEMENT_REGISTER_DECLARE (downloadbuffer);
GST_ELEMENT_REGISTER_DECLARE (fakesink);
GST_ELEMENT_REGISTER_DECLARE (fakesrc);
#if defined(HAVE_SYS_SOCKET_H) || defined(_MSC_VER)
GST_ELEMENT_REGISTER_DECLARE (fdsrc);
GST_ELEMENT_REGISTER_DECLARE (fdsink);
#endif
GST_ELEMENT_REGISTER_DECLARE (filesink);
GST_ELEMENT_REGISTER_DECLARE (filesrc);
GST_ELEMENT_REGISTER_DECLARE (funnel);
GST_ELEMENT_REGISTER_DECLARE (identity);
GST_ELEMENT_REGISTER_DECLARE (input_selector);
GST_ELEMENT_REGISTER_DECLARE (multiqueue);
GST_ELEMENT_REGISTER_DECLARE (output_selector);
#endif // GSTREAMER_LITE
GST_ELEMENT_REGISTER_DECLARE (queue);
#ifndef GSTREAMER_LITE
GST_ELEMENT_REGISTER_DECLARE (queue2);
GST_ELEMENT_REGISTER_DECLARE (streamiddemux);
GST_ELEMENT_REGISTER_DECLARE (tee);
#endif // GSTREAMER_LITE
GST_ELEMENT_REGISTER_DECLARE (typefind);
#ifndef GSTREAMER_LITE
GST_ELEMENT_REGISTER_DECLARE (valve);
#endif // GSTREAMER_LITE

G_END_DECLS

#endif /* __GST_CORE_ELEMENTS_ELEMENTS_H__ */
