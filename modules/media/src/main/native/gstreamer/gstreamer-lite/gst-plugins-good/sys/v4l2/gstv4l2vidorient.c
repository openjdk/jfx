/* GStreamer
 *
 * Copyright (C) 2006 Edgard Lima <edgard.lima@indt.org.br>
 *
 * gstv4l2vidorient.c: video orientation interface implementation for V4L2
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

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>

#include "gstv4l2vidorient.h"
#include "gstv4l2object.h"
#include "v4l2_calls.h"
#include "v4l2src_calls.h"

GST_DEBUG_CATEGORY_STATIC (v4l2vo_debug);
#define GST_CAT_DEFAULT v4l2vo_debug

/* Those are deprecated calls that have been replaced */
#ifndef V4L2_CID_HCENTER
#define V4L2_CID_HCENTER V4L2_CID_PAN_RESET
#endif
#ifndef V4L2_CID_VCENTER
#define V4L2_CID_VCENTER V4L2_CID_TILT_RESET
#endif

void
gst_v4l2_video_orientation_interface_init (GstVideoOrientationInterface * klass)
{
  GST_DEBUG_CATEGORY_INIT (v4l2vo_debug, "v4l2vo", 0,
      "V4L2 VideoOrientation interface debugging");
}


gboolean
gst_v4l2_video_orientation_get_hflip (GstV4l2Object * v4l2object,
    gboolean * flip)
{

  return gst_v4l2_get_attribute (v4l2object, V4L2_CID_HFLIP, flip);
}

gboolean
gst_v4l2_video_orientation_get_vflip (GstV4l2Object * v4l2object,
    gboolean * flip)
{
  return gst_v4l2_get_attribute (v4l2object, V4L2_CID_VFLIP, flip);
}

gboolean
gst_v4l2_video_orientation_get_hcenter (GstV4l2Object * v4l2object,
    gint * center)
{
  return gst_v4l2_get_attribute (v4l2object, V4L2_CID_HCENTER, center);
}

gboolean
gst_v4l2_video_orientation_get_vcenter (GstV4l2Object * v4l2object,
    gint * center)
{
  return gst_v4l2_get_attribute (v4l2object, V4L2_CID_VCENTER, center);
}

gboolean
gst_v4l2_video_orientation_set_hflip (GstV4l2Object * v4l2object, gboolean flip)
{
  return gst_v4l2_set_attribute (v4l2object, V4L2_CID_HFLIP, flip);
}

gboolean
gst_v4l2_video_orientation_set_vflip (GstV4l2Object * v4l2object, gboolean flip)
{
  return gst_v4l2_set_attribute (v4l2object, V4L2_CID_VFLIP, flip);
}

gboolean
gst_v4l2_video_orientation_set_hcenter (GstV4l2Object * v4l2object, gint center)
{
  return gst_v4l2_set_attribute (v4l2object, V4L2_CID_HCENTER, center);
}

gboolean
gst_v4l2_video_orientation_set_vcenter (GstV4l2Object * v4l2object, gint center)
{
  return gst_v4l2_set_attribute (v4l2object, V4L2_CID_VCENTER, center);
}
