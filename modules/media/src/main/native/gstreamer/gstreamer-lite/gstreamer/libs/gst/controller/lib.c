/* GStreamer
 *
 * Copyright (C) <2005> Stefan Kost <ensonic at users dot sf dot net>
 *
 * lib.c: controller subsystem init
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
#  include "config.h"
#endif
#include <gst/gst.h>
#include <gst/controller/gstcontroller.h>

/* library initialisation */

#define GST_CAT_DEFAULT controller_debug
GST_DEBUG_CATEGORY (GST_CAT_DEFAULT);

/**
 * gst_controller_init:
 * @argc: pointer to the commandline argument count
 * @argv: pointer to the commandline argument values
 *
 * Initializes the use of the controller library. Suggested to be called right
 * after gst_init().
 *
 * Returns: the %TRUE for success.
 */
gboolean
gst_controller_init (int *argc, char ***argv)
{
  static gboolean _gst_controller_initialized = FALSE;

  if (_gst_controller_initialized)
    return TRUE;

  _gst_controller_initialized = TRUE;

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, "gstcontroller", 0,
      "dynamic parameter control for gstreamer elements");

  return TRUE;
}
