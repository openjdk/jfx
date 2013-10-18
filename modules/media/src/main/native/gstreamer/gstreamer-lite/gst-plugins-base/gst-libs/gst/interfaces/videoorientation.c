/* GStreamer
 * Copyright (C) 2006 Nokia <stefan.kost@nokia.com>
 *
 * videoorientation.c: video flipping and centering interface
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

#include "videoorientation.h"
#include "interfaces-marshal.h"

#include <string.h>

/**
 * SECTION:gstvideoorientation
 * @short_description: Interface for elements providing video orientation
 * controls
 *
 * The interface allows unified access to control flipping and autocenter
 * operation of video-sources or operators.
 *
 * Since: 0.10.11
 */

static void gst_video_orientation_iface_init (GstVideoOrientationInterface *
    iface);

GType
gst_video_orientation_get_type (void)
{
  static GType gst_video_orientation_type = 0;

  if (!gst_video_orientation_type) {
    static const GTypeInfo gst_video_orientation_info = {
      sizeof (GstVideoOrientationInterface),
      (GBaseInitFunc) gst_video_orientation_iface_init,
      NULL,
      NULL,
      NULL,
      NULL,
      0,
      0,
      NULL,
    };

    gst_video_orientation_type = g_type_register_static (G_TYPE_INTERFACE,
        "GstVideoOrientation", &gst_video_orientation_info, 0);
    g_type_interface_add_prerequisite (gst_video_orientation_type,
        GST_TYPE_IMPLEMENTS_INTERFACE);
  }

  return gst_video_orientation_type;
}

static void
gst_video_orientation_iface_init (GstVideoOrientationInterface * iface)
{
  /* default virtual functions */

  iface->get_hflip = NULL;
  iface->get_vflip = NULL;
  iface->get_hcenter = NULL;
  iface->get_vcenter = NULL;

  iface->set_hflip = NULL;
  iface->set_vflip = NULL;
  iface->set_hcenter = NULL;
  iface->set_vcenter = NULL;
}

/**
 * gst_video_orientation_get_hflip:
 * @video_orientation: #GstVideoOrientation interface of a #GstElement
 * @flip: return location for the result
 *
 * Get the horizontal flipping state (%TRUE for flipped) from the given object.
 *
 * Since: 0.10.11
 * Returns: %TRUE in case the element supports flipping
 */
gboolean
gst_video_orientation_get_hflip (GstVideoOrientation * video_orientation,
    gboolean * flip)
{
  GstVideoOrientationInterface *iface =
      GST_VIDEO_ORIENTATION_GET_IFACE (video_orientation);

  if (iface->get_hflip) {
    return iface->get_hflip (video_orientation, flip);
  }
  return FALSE;
}

/**
 * gst_video_orientation_get_vflip:
 * @video_orientation: #GstVideoOrientation interface of a #GstElement
 * @flip: return location for the result
 *
 * Get the vertical flipping state (%TRUE for flipped) from the given object.
 *
 * Since: 0.10.11
 * Returns: %TRUE in case the element supports flipping
 */
gboolean
gst_video_orientation_get_vflip (GstVideoOrientation * video_orientation,
    gboolean * flip)
{
  GstVideoOrientationInterface *iface =
      GST_VIDEO_ORIENTATION_GET_IFACE (video_orientation);

  if (iface->get_vflip) {
    return iface->get_vflip (video_orientation, flip);
  }
  return FALSE;
}

/**
 * gst_video_orientation_get_hcenter:
 * @video_orientation: #GstVideoOrientation interface of a #GstElement
 * @center: return location for the result
 *
 * Get the horizontal centering offset from the given object.
 *
 * Since: 0.10.11
 * Returns: %TRUE in case the element supports centering
 */
gboolean
gst_video_orientation_get_hcenter (GstVideoOrientation * video_orientation,
    gint * center)
{
  GstVideoOrientationInterface *iface =
      GST_VIDEO_ORIENTATION_GET_IFACE (video_orientation);

  if (iface->get_hcenter) {
    return iface->get_hcenter (video_orientation, center);
  }
  return FALSE;
}

/**
 * gst_video_orientation_get_vcenter:
 * @video_orientation: #GstVideoOrientation interface of a #GstElement
 * @center: return location for the result
 *
 * Get the vertical centering offset from the given object.
 *
 * Since: 0.10.11
 * Returns: %TRUE in case the element supports centering
 */
gboolean
gst_video_orientation_get_vcenter (GstVideoOrientation * video_orientation,
    gint * center)
{
  GstVideoOrientationInterface *iface =
      GST_VIDEO_ORIENTATION_GET_IFACE (video_orientation);

  if (iface->get_vcenter) {
    return iface->get_vcenter (video_orientation, center);
  }
  return FALSE;
}

/**
 * gst_video_orientation_set_hflip:
 * @video_orientation: #GstVideoOrientation interface of a #GstElement
 * @flip: use flipping
 *
 * Set the horizontal flipping state (%TRUE for flipped) for the given object.
 *
 * Since: 0.10.11
 * Returns: %TRUE in case the element supports flipping
 */
gboolean
gst_video_orientation_set_hflip (GstVideoOrientation * video_orientation,
    gboolean flip)
{
  GstVideoOrientationInterface *iface =
      GST_VIDEO_ORIENTATION_GET_IFACE (video_orientation);

  if (iface->set_hflip) {
    return iface->set_hflip (video_orientation, flip);
  }
  return FALSE;
}

/**
 * gst_video_orientation_set_vflip:
 * @video_orientation: #GstVideoOrientation interface of a #GstElement
 * @flip: use flipping
 *
 * Set the vertical flipping state (%TRUE for flipped) for the given object.
 *
 * Since: 0.10.11
 * Returns: %TRUE in case the element supports flipping
 */
gboolean
gst_video_orientation_set_vflip (GstVideoOrientation * video_orientation,
    gboolean flip)
{
  GstVideoOrientationInterface *iface =
      GST_VIDEO_ORIENTATION_GET_IFACE (video_orientation);

  if (iface->set_vflip) {
    return iface->set_vflip (video_orientation, flip);
  }
  return FALSE;
}

/**
 * gst_video_orientation_set_hcenter:
 * @video_orientation: #GstVideoOrientation interface of a #GstElement
 * @center: centering offset
 *
 * Set the horizontal centering offset for the given object.
 *
 * Since: 0.10.11
 * Returns: %TRUE in case the element supports centering
 */
gboolean
gst_video_orientation_set_hcenter (GstVideoOrientation * video_orientation,
    gint center)
{
  GstVideoOrientationInterface *iface =
      GST_VIDEO_ORIENTATION_GET_IFACE (video_orientation);

  if (iface->set_hcenter) {
    return iface->set_hcenter (video_orientation, center);
  }
  return FALSE;
}

/**
 * gst_video_orientation_set_vcenter:
 * @video_orientation: #GstVideoOrientation interface of a #GstElement
 * @center: centering offset
 *
 * Set the vertical centering offset for the given object.
 *
 * Since: 0.10.11
 * Returns: %TRUE in case the element supports centering
 */
gboolean
gst_video_orientation_set_vcenter (GstVideoOrientation * video_orientation,
    gint center)
{
  GstVideoOrientationInterface *iface =
      GST_VIDEO_ORIENTATION_GET_IFACE (video_orientation);

  if (iface->set_vcenter) {
    return iface->set_vcenter (video_orientation, center);
  }
  return FALSE;
}
