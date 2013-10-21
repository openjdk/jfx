/* GStreamer
 * Copyright (C) 2004-6 Zaheer Abbas Merali <zaheerabbas at merali dot org>
 * Copyright (C) 2007 Pioneers of the Inevitable <songbird@songbirdnest.com>
 *
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
 *
 *
 * The development of this code was made possible due to the involvement of Pioneers 
 * of the Inevitable, the creators of the Songbird Music player
 * 
 */
 
#ifndef __GST_OSX_VIDEO_SINK_H__
#define __GST_OSX_VIDEO_SINK_H__

#include <gst/video/gstvideosink.h>

#include <string.h>
#include <math.h>
#include <Cocoa/Cocoa.h>

#include <QuickTime/QuickTime.h>
#import "cocoawindow.h"

GST_DEBUG_CATEGORY_EXTERN (gst_debug_osx_video_sink);
#define GST_CAT_DEFAULT gst_debug_osx_video_sink

G_BEGIN_DECLS

#define GST_TYPE_OSX_VIDEO_SINK \
  (gst_osx_video_sink_get_type())
#define GST_OSX_VIDEO_SINK(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj), GST_TYPE_OSX_VIDEO_SINK, GstOSXVideoSink))
#define GST_OSX_VIDEO_SINK_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass), GST_TYPE_OSX_VIDEO_SINK, GstOSXVideoSinkClass))
#define GST_IS_OSX_VIDEO_SINK(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj), GST_TYPE_OSX_VIDEO_SINK))
#define GST_IS_OSX_VIDEO_SINK_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass), GST_TYPE_OSX_VIDEO_SINK))

typedef struct _GstOSXWindow GstOSXWindow;

typedef struct _GstOSXVideoSink GstOSXVideoSink;
typedef struct _GstOSXVideoSinkClass GstOSXVideoSinkClass;

#define GST_TYPE_OSXVIDEOBUFFER (gst_osxvideobuffer_get_type())

/* OSXWindow stuff */
struct _GstOSXWindow {
  gint width, height;
  gboolean internal;
  GstGLView* gstview;
};

struct _GstOSXVideoSink {
  /* Our element stuff */
  GstVideoSink videosink;
  GstOSXWindow *osxwindow;
  NSView *superview;
};

struct _GstOSXVideoSinkClass {
  GstVideoSinkClass parent_class;
};

GType gst_osx_video_sink_get_type(void);

G_END_DECLS

#endif /* __GST_OSX_VIDEO_SINK_H__ */

