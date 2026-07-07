/* GStreamer App Library
 * Copyright (C) 2018 GStreamer developers
 *
 * app-prelude.h: prelude include header for gst-app library
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

#ifndef __GST_APP_PRELUDE_H__
#define __GST_APP_PRELUDE_H__

#include <gst/gst.h>

#ifdef GSTREAMER_LITE
#ifndef GST_APP_API
#define GST_APP_API
#endif
#else // GSTREAMER_LITE
#ifdef BUILDING_GST_APP
#define GST_APP_API GST_API_EXPORT         /* from config.h */
#else
#define GST_APP_API GST_API_IMPORT
#endif
#endif // GSTREAMER_LITE

#ifndef GST_DISABLE_DEPRECATED
#define GST_APP_DEPRECATED GST_APP_API
#define GST_APP_DEPRECATED_FOR(f) GST_APP_API
#define GST_APP_DEPRECATED_TYPE
#define GST_APP_DEPRECATED_TYPE_FOR(f)
#else
#define GST_APP_DEPRECATED G_DEPRECATED GST_APP_API
#define GST_APP_DEPRECATED_FOR(f) G_DEPRECATED_FOR(f) GST_APP_API
#define GST_APP_DEPRECATED_TYPE G_DEPRECATED
#define GST_APP_DEPRECATED_TYPE_FOR(f) G_DEPRECATED_FOR(f)
#endif

#endif /* __GST_APP_PRELUDE_H__ */
