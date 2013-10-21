/* GStreamer
 * Copyright (C) <1999> Erik Walthinsen <omega@cse.ogi.edu>
 * Library       <2002> Ronald Bultje <rbultje@ronald.bitfreak.net>
 * Copyright (C) 2007 David A. Schleef <ds@schleef.org>
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

#include "video.h"

#ifdef GSTREAMER_LITE
#ifndef WIN32
#undef GSTREAMER_LITE
#endif // WIN32
#endif // GSTREAMER_LITE

#ifndef GSTREAMER_LITE
/**
 * SECTION:gstvideo
 * @short_description: Support library for video operations
 *
 * <refsect2>
 * <para>
 * This library contains some helper functions and includes the 
 * videosink and videofilter base classes.
 * </para>
 * </refsect2>
 */

static GstVideoFormat gst_video_format_from_rgb32_masks (int red_mask,
    int green_mask, int blue_mask);
static GstVideoFormat gst_video_format_from_rgba32_masks (int red_mask,
    int green_mask, int blue_mask, int alpha_mask);
static GstVideoFormat gst_video_format_from_rgb24_masks (int red_mask,
    int green_mask, int blue_mask);
static GstVideoFormat gst_video_format_from_rgb16_masks (int red_mask,
    int green_mask, int blue_mask);


/**
 * gst_video_frame_rate:
 * @pad: pointer to a #GstPad
 *
 * A convenience function to retrieve a GValue holding the framerate
 * from the caps on a pad.
 * 
 * The pad needs to have negotiated caps containing a framerate property.
 *
 * Returns: NULL if the pad has no configured caps or the configured caps
 * do not contain a framerate.
 *
 */
const GValue *
gst_video_frame_rate (GstPad * pad)
{
  const GValue *fps;
  gchar *fps_string;

  const GstCaps *caps = NULL;
  GstStructure *structure;

  /* get pad caps */
  caps = GST_PAD_CAPS (pad);
  if (caps == NULL) {
    g_warning ("gstvideo: failed to get caps of pad %s:%s",
        GST_DEBUG_PAD_NAME (pad));
    return NULL;
  }

  structure = gst_caps_get_structure (caps, 0);
  if ((fps = gst_structure_get_value (structure, "framerate")) == NULL) {
    g_warning ("gstvideo: failed to get framerate property of pad %s:%s",
        GST_DEBUG_PAD_NAME (pad));
    return NULL;
  }
  if (!GST_VALUE_HOLDS_FRACTION (fps)) {
    g_warning
        ("gstvideo: framerate property of pad %s:%s is not of type Fraction",
        GST_DEBUG_PAD_NAME (pad));
    return NULL;
  }

  fps_string = gst_value_serialize (fps);
  GST_DEBUG ("Framerate request on pad %s:%s: %s",
      GST_DEBUG_PAD_NAME (pad), fps_string);
  g_free (fps_string);

  return fps;
}

/**
 * gst_video_get_size:
 * @pad: pointer to a #GstPad
 * @width: pointer to integer to hold pixel width of the video frames (output)
 * @height: pointer to integer to hold pixel height of the video frames (output)
 *
 * Inspect the caps of the provided pad and retrieve the width and height of
 * the video frames it is configured for.
 * 
 * The pad needs to have negotiated caps containing width and height properties.
 *
 * Returns: TRUE if the width and height could be retrieved.
 *
 */
gboolean
gst_video_get_size (GstPad * pad, gint * width, gint * height)
{
  const GstCaps *caps = NULL;
  GstStructure *structure;
  gboolean ret;

  g_return_val_if_fail (pad != NULL, FALSE);
  g_return_val_if_fail (width != NULL, FALSE);
  g_return_val_if_fail (height != NULL, FALSE);

  caps = GST_PAD_CAPS (pad);

  if (caps == NULL) {
    g_warning ("gstvideo: failed to get caps of pad %s:%s",
        GST_DEBUG_PAD_NAME (pad));
    return FALSE;
  }

  structure = gst_caps_get_structure (caps, 0);
  ret = gst_structure_get_int (structure, "width", width);
  ret &= gst_structure_get_int (structure, "height", height);

  if (!ret) {
    g_warning ("gstvideo: failed to get size properties on pad %s:%s",
        GST_DEBUG_PAD_NAME (pad));
    return FALSE;
  }

  GST_DEBUG ("size request on pad %s:%s: %dx%d",
      GST_DEBUG_PAD_NAME (pad), width ? *width : -1, height ? *height : -1);

  return TRUE;
}

/**
 * gst_video_calculate_display_ratio:
 * @dar_n: Numerator of the calculated display_ratio
 * @dar_d: Denominator of the calculated display_ratio
 * @video_width: Width of the video frame in pixels
 * @video_height: Height of the video frame in pixels
 * @video_par_n: Numerator of the pixel aspect ratio of the input video.
 * @video_par_d: Denominator of the pixel aspect ratio of the input video.
 * @display_par_n: Numerator of the pixel aspect ratio of the display device
 * @display_par_d: Denominator of the pixel aspect ratio of the display device
 *
 * Given the Pixel Aspect Ratio and size of an input video frame, and the 
 * pixel aspect ratio of the intended display device, calculates the actual 
 * display ratio the video will be rendered with.
 *
 * Returns: A boolean indicating success and a calculated Display Ratio in the 
 * dar_n and dar_d parameters. 
 * The return value is FALSE in the case of integer overflow or other error. 
 *
 * Since: 0.10.7
 */
gboolean
gst_video_calculate_display_ratio (guint * dar_n, guint * dar_d,
    guint video_width, guint video_height,
    guint video_par_n, guint video_par_d,
    guint display_par_n, guint display_par_d)
{
  gint num, den;
  gint tmp_n, tmp_d;

  g_return_val_if_fail (dar_n != NULL, FALSE);
  g_return_val_if_fail (dar_d != NULL, FALSE);

  /* Calculate (video_width * video_par_n * display_par_d) /
   * (video_height * video_par_d * display_par_n) */
  if (!gst_util_fraction_multiply (video_width, video_height, video_par_n,
          video_par_d, &tmp_n, &tmp_d))
    goto error_overflow;

  if (!gst_util_fraction_multiply (tmp_n, tmp_d, display_par_d, display_par_n,
          &num, &den))
    goto error_overflow;

  g_return_val_if_fail (num > 0, FALSE);
  g_return_val_if_fail (den > 0, FALSE);

  *dar_n = num;
  *dar_d = den;

  return TRUE;
error_overflow:
  return FALSE;
}

/**
 * gst_video_format_parse_caps_interlaced:
 * @caps: the fixed #GstCaps to parse
 * @interlaced: whether @caps represents interlaced video or not, may be NULL (output)
 *
 * Extracts whether the caps represents interlaced content or not and places it
 * in @interlaced.
 *
 * Since: 0.10.23
 *
 * Returns: TRUE if @caps was parsed correctly.
 */
gboolean
gst_video_format_parse_caps_interlaced (GstCaps * caps, gboolean * interlaced)
{
  GstStructure *structure;

  if (!gst_caps_is_fixed (caps))
    return FALSE;

  structure = gst_caps_get_structure (caps, 0);

  if (interlaced) {
    if (!gst_structure_get_boolean (structure, "interlaced", interlaced))
      *interlaced = FALSE;
  }

  return TRUE;
}

/**
 * gst_video_parse_caps_color_matrix:
 * @caps: the fixed #GstCaps to parse
 *
 * Extracts the color matrix used by the caps.  Possible values are
 * "sdtv" for the standard definition color matrix (as specified in
 * Rec. ITU-R BT.470-6) or "hdtv" for the high definition color
 * matrix (as specified in Rec. ITU-R BT.709)
 *
 * Since: 0.10.29
 *
 * Returns: a color matrix string, or NULL if no color matrix could be
 *     determined.
 */
const char *
gst_video_parse_caps_color_matrix (GstCaps * caps)
{
  GstStructure *structure;
  const char *s;

  if (!gst_caps_is_fixed (caps))
    return NULL;

  structure = gst_caps_get_structure (caps, 0);

  s = gst_structure_get_string (structure, "color-matrix");
  if (s)
    return s;

  if (gst_structure_has_name (structure, "video/x-raw-yuv")) {
    return "sdtv";
  }

  return NULL;
}

/**
 * gst_video_parse_caps_chroma_site:
 * @caps: the fixed #GstCaps to parse
 *
 * Extracts the chroma site used by the caps.  Possible values are
 * "mpeg2" for MPEG-2 style chroma siting (co-sited horizontally,
 * halfway-sited vertically), "jpeg" for JPEG and Theora style
 * chroma siting (halfway-sited both horizontally and vertically).
 * Other chroma site values are possible, but uncommon.
 * 
 * When no chroma site is specified in the caps, it should be assumed
 * to be "mpeg2".
 *
 * Since: 0.10.29
 *
 * Returns: a chroma site string, or NULL if no chroma site could be
 *     determined.
 */
const char *
gst_video_parse_caps_chroma_site (GstCaps * caps)
{
  GstStructure *structure;
  const char *s;

  if (!gst_caps_is_fixed (caps))
    return NULL;

  structure = gst_caps_get_structure (caps, 0);

  s = gst_structure_get_string (structure, "chroma-site");
  if (s)
    return s;

  if (gst_structure_has_name (structure, "video/x-raw-yuv")) {
    return "mpeg2";
  }

  return NULL;
}

/**
 * gst_video_format_parse_caps:
 * @caps: the #GstCaps to parse
 * @format: the #GstVideoFormat of the video represented by @caps (output)
 * @width: the width of the video represented by @caps, may be NULL (output)
 * @height: the height of the video represented by @caps, may be NULL (output)
 *
 * Determines the #GstVideoFormat of @caps and places it in the location
 * pointed to by @format.  Extracts the size of the video and places it
 * in the location pointed to by @width and @height.  If @caps does not
 * represent one of the raw video formats listed in #GstVideoFormat, the
 * function will fail and return FALSE.
 *
 * Since: 0.10.16
 *
 * Returns: TRUE if @caps was parsed correctly.
 */
gboolean
gst_video_format_parse_caps (GstCaps * caps, GstVideoFormat * format,
    int *width, int *height)
{
  GstStructure *structure;
  gboolean ok = TRUE;

  if (!gst_caps_is_fixed (caps))
    return FALSE;

  structure = gst_caps_get_structure (caps, 0);

  if (format) {
    if (gst_structure_has_name (structure, "video/x-raw-yuv")) {
      guint32 fourcc;

      ok &= gst_structure_get_fourcc (structure, "format", &fourcc);

      *format = gst_video_format_from_fourcc (fourcc);
      if (*format == GST_VIDEO_FORMAT_UNKNOWN) {
        ok = FALSE;
      }
    } else if (gst_structure_has_name (structure, "video/x-raw-rgb")) {
      int depth;
      int bpp;
      int endianness = 0;
      int red_mask = 0;
      int green_mask = 0;
      int blue_mask = 0;
      int alpha_mask = 0;
      gboolean have_alpha;

      ok &= gst_structure_get_int (structure, "depth", &depth);
      ok &= gst_structure_get_int (structure, "bpp", &bpp);

      if (bpp != 8) {
        ok &= gst_structure_get_int (structure, "endianness", &endianness);
        ok &= gst_structure_get_int (structure, "red_mask", &red_mask);
        ok &= gst_structure_get_int (structure, "green_mask", &green_mask);
        ok &= gst_structure_get_int (structure, "blue_mask", &blue_mask);
      }
      have_alpha = gst_structure_get_int (structure, "alpha_mask", &alpha_mask);

      if (depth == 30 && bpp == 32 && endianness == G_BIG_ENDIAN) {
        *format = GST_VIDEO_FORMAT_r210;
      } else if (depth == 24 && bpp == 32 && endianness == G_BIG_ENDIAN) {
        *format = gst_video_format_from_rgb32_masks (red_mask, green_mask,
            blue_mask);
        if (*format == GST_VIDEO_FORMAT_UNKNOWN) {
          ok = FALSE;
        }
      } else if (depth == 32 && bpp == 32 && endianness == G_BIG_ENDIAN &&
          have_alpha) {
        *format = gst_video_format_from_rgba32_masks (red_mask, green_mask,
            blue_mask, alpha_mask);
        if (*format == GST_VIDEO_FORMAT_UNKNOWN) {
          ok = FALSE;
        }
      } else if (depth == 24 && bpp == 24 && endianness == G_BIG_ENDIAN) {
        *format = gst_video_format_from_rgb24_masks (red_mask, green_mask,
            blue_mask);
        if (*format == GST_VIDEO_FORMAT_UNKNOWN) {
          ok = FALSE;
        }
      } else if ((depth == 15 || depth == 16) && bpp == 16 &&
          endianness == G_BYTE_ORDER) {
        *format = gst_video_format_from_rgb16_masks (red_mask, green_mask,
            blue_mask);
        if (*format == GST_VIDEO_FORMAT_UNKNOWN) {
          ok = FALSE;
        }
      } else if (depth == 8 && bpp == 8) {
        *format = GST_VIDEO_FORMAT_RGB8_PALETTED;
      } else if (depth == 64 && bpp == 64) {
        *format = gst_video_format_from_rgba32_masks (red_mask, green_mask,
            blue_mask, alpha_mask);
        if (*format == GST_VIDEO_FORMAT_ARGB) {
          *format = GST_VIDEO_FORMAT_ARGB64;
        } else {
          *format = GST_VIDEO_FORMAT_UNKNOWN;
          ok = FALSE;
        }
      } else {
        ok = FALSE;
      }
    } else if (gst_structure_has_name (structure, "video/x-raw-gray")) {
      int depth;
      int bpp;
      int endianness;

      ok &= gst_structure_get_int (structure, "depth", &depth);
      ok &= gst_structure_get_int (structure, "bpp", &bpp);

      if (bpp > 8)
        ok &= gst_structure_get_int (structure, "endianness", &endianness);

      if (depth == 8 && bpp == 8) {
        *format = GST_VIDEO_FORMAT_GRAY8;
      } else if (depth == 16 && bpp == 16 && endianness == G_BIG_ENDIAN) {
        *format = GST_VIDEO_FORMAT_GRAY16_BE;
      } else if (depth == 16 && bpp == 16 && endianness == G_LITTLE_ENDIAN) {
        *format = GST_VIDEO_FORMAT_GRAY16_LE;
      } else {
        ok = FALSE;
      }
    } else {
      ok = FALSE;
    }
  }

  if (width) {
    ok &= gst_structure_get_int (structure, "width", width);
  }

  if (height) {
    ok &= gst_structure_get_int (structure, "height", height);
  }

  return ok;
}
#endif // GSTREAMER_LITE


/**
 * gst_video_parse_caps_framerate:
 * @caps: pointer to a #GstCaps instance
 * @fps_n: pointer to integer to hold numerator of frame rate (output)
 * @fps_d: pointer to integer to hold denominator of frame rate (output)
 *
 * Extracts the frame rate from @caps and places the values in the locations
 * pointed to by @fps_n and @fps_d.  Returns TRUE if the values could be
 * parsed correctly, FALSE if not.
 *
 * This function can be used with #GstCaps that have any media type; it
 * is not limited to formats handled by #GstVideoFormat.
 *
 * Since: 0.10.16
 *
 * Returns: TRUE if @caps was parsed correctly.
 */
gboolean
gst_video_parse_caps_framerate (GstCaps * caps, int *fps_n, int *fps_d)
{
  GstStructure *structure;

  if (!gst_caps_is_fixed (caps))
    return FALSE;

  structure = gst_caps_get_structure (caps, 0);

  return gst_structure_get_fraction (structure, "framerate", fps_n, fps_d);
}

/**
 * gst_video_parse_caps_pixel_aspect_ratio:
 * @caps: pointer to a #GstCaps instance
 * @par_n: pointer to numerator of pixel aspect ratio (output)
 * @par_d: pointer to denominator of pixel aspect ratio (output)
 *
 * Extracts the pixel aspect ratio from @caps and places the values in
 * the locations pointed to by @par_n and @par_d.  Returns TRUE if the
 * values could be parsed correctly, FALSE if not.
 *
 * This function can be used with #GstCaps that have any media type; it
 * is not limited to formats handled by #GstVideoFormat.
 *
 * Since: 0.10.16
 *
 * Returns: TRUE if @caps was parsed correctly.
 */
gboolean
gst_video_parse_caps_pixel_aspect_ratio (GstCaps * caps, int *par_n, int *par_d)
{
  GstStructure *structure;

  if (!gst_caps_is_fixed (caps))
    return FALSE;

  structure = gst_caps_get_structure (caps, 0);

  if (!gst_structure_get_fraction (structure, "pixel-aspect-ratio",
          par_n, par_d)) {
    *par_n = 1;
    *par_d = 1;
  }
  return TRUE;
}

#ifndef GSTREAMER_LITE
/**
 * gst_video_format_new_caps_interlaced:
 * @format: the #GstVideoFormat describing the raw video format
 * @width: width of video
 * @height: height of video
 * @framerate_n: numerator of frame rate
 * @framerate_d: denominator of frame rate
 * @par_n: numerator of pixel aspect ratio
 * @par_d: denominator of pixel aspect ratio
 * @interlaced: #TRUE if the format is interlaced
 *
 * Creates a new #GstCaps object based on the parameters provided.
 *
 * Since: 0.10.23
 *
 * Returns: a new #GstCaps object, or NULL if there was an error
 */
GstCaps *
gst_video_format_new_caps_interlaced (GstVideoFormat format,
    int width, int height, int framerate_n, int framerate_d, int par_n,
    int par_d, gboolean interlaced)
{
  GstCaps *res;

  res =
      gst_video_format_new_caps (format, width, height, framerate_n,
      framerate_d, par_n, par_d);
  if (interlaced && (res != NULL))
    gst_caps_set_simple (res, "interlaced", G_TYPE_BOOLEAN, TRUE, NULL);

  return res;
}

static GstCaps *
gst_video_format_new_caps_raw (GstVideoFormat format)
{
  g_return_val_if_fail (format != GST_VIDEO_FORMAT_UNKNOWN, NULL);

  if (gst_video_format_is_yuv (format)) {
    return gst_caps_new_simple ("video/x-raw-yuv",
        "format", GST_TYPE_FOURCC, gst_video_format_to_fourcc (format), NULL);
  }
  if (gst_video_format_is_rgb (format)) {
    GstCaps *caps;
    int red_mask = 0;
    int blue_mask = 0;
    int green_mask = 0;
    int alpha_mask;
    int depth;
    int bpp;
    gboolean have_alpha;
    unsigned int mask = 0;

    switch (format) {
      case GST_VIDEO_FORMAT_RGBx:
      case GST_VIDEO_FORMAT_BGRx:
      case GST_VIDEO_FORMAT_xRGB:
      case GST_VIDEO_FORMAT_xBGR:
        bpp = 32;
        depth = 24;
        have_alpha = FALSE;
        break;
      case GST_VIDEO_FORMAT_RGBA:
      case GST_VIDEO_FORMAT_BGRA:
      case GST_VIDEO_FORMAT_ARGB:
      case GST_VIDEO_FORMAT_ABGR:
        bpp = 32;
        depth = 32;
        have_alpha = TRUE;
        break;
      case GST_VIDEO_FORMAT_RGB:
      case GST_VIDEO_FORMAT_BGR:
        bpp = 24;
        depth = 24;
        have_alpha = FALSE;
        break;
      case GST_VIDEO_FORMAT_RGB16:
      case GST_VIDEO_FORMAT_BGR16:
        bpp = 16;
        depth = 16;
        have_alpha = FALSE;
        break;
      case GST_VIDEO_FORMAT_RGB15:
      case GST_VIDEO_FORMAT_BGR15:
        bpp = 16;
        depth = 15;
        have_alpha = FALSE;
        break;
      case GST_VIDEO_FORMAT_RGB8_PALETTED:
        bpp = 8;
        depth = 8;
        have_alpha = FALSE;
        break;
      case GST_VIDEO_FORMAT_ARGB64:
        bpp = 64;
        depth = 64;
        have_alpha = TRUE;
        break;
      case GST_VIDEO_FORMAT_r210:
        bpp = 32;
        depth = 30;
        have_alpha = FALSE;
        break;
      default:
        return NULL;
    }
    if (bpp == 32 && depth == 30) {
      red_mask = 0x3ff00000;
      green_mask = 0x000ffc00;
      blue_mask = 0x000003ff;
      have_alpha = FALSE;
    } else if (bpp == 32 || bpp == 24 || bpp == 64) {
      if (bpp == 32) {
        mask = 0xff000000;
      } else {
        mask = 0xff0000;
      }
      red_mask =
          mask >> (8 * gst_video_format_get_component_offset (format, 0, 0, 0));
      green_mask =
          mask >> (8 * gst_video_format_get_component_offset (format, 1, 0, 0));
      blue_mask =
          mask >> (8 * gst_video_format_get_component_offset (format, 2, 0, 0));
    } else if (bpp == 16) {
      switch (format) {
        case GST_VIDEO_FORMAT_RGB16:
          red_mask = GST_VIDEO_COMP1_MASK_16_INT;
          green_mask = GST_VIDEO_COMP2_MASK_16_INT;
          blue_mask = GST_VIDEO_COMP3_MASK_16_INT;
          break;
        case GST_VIDEO_FORMAT_BGR16:
          red_mask = GST_VIDEO_COMP3_MASK_16_INT;
          green_mask = GST_VIDEO_COMP2_MASK_16_INT;
          blue_mask = GST_VIDEO_COMP1_MASK_16_INT;
          break;
          break;
        case GST_VIDEO_FORMAT_RGB15:
          red_mask = GST_VIDEO_COMP1_MASK_15_INT;
          green_mask = GST_VIDEO_COMP2_MASK_15_INT;
          blue_mask = GST_VIDEO_COMP3_MASK_15_INT;
          break;
        case GST_VIDEO_FORMAT_BGR15:
          red_mask = GST_VIDEO_COMP3_MASK_15_INT;
          green_mask = GST_VIDEO_COMP2_MASK_15_INT;
          blue_mask = GST_VIDEO_COMP1_MASK_15_INT;
          break;
        default:
          return NULL;
      }
    } else if (bpp != 8) {
      return NULL;
    }

    caps = gst_caps_new_simple ("video/x-raw-rgb",
        "bpp", G_TYPE_INT, bpp, "depth", G_TYPE_INT, depth, NULL);

    if (bpp != 8) {
      gst_caps_set_simple (caps,
          "endianness", G_TYPE_INT, G_BIG_ENDIAN,
          "red_mask", G_TYPE_INT, red_mask,
          "green_mask", G_TYPE_INT, green_mask,
          "blue_mask", G_TYPE_INT, blue_mask, NULL);
    }

    if (have_alpha) {
      alpha_mask =
          mask >> (8 * gst_video_format_get_component_offset (format, 3, 0, 0));
      gst_caps_set_simple (caps, "alpha_mask", G_TYPE_INT, alpha_mask, NULL);
    }
    return caps;
  }

  if (gst_video_format_is_gray (format)) {
    GstCaps *caps;
    int bpp;
    int depth;
    int endianness;

    switch (format) {
      case GST_VIDEO_FORMAT_GRAY8:
        bpp = depth = 8;
        endianness = G_BIG_ENDIAN;
        break;
      case GST_VIDEO_FORMAT_GRAY16_BE:
        bpp = depth = 16;
        endianness = G_BIG_ENDIAN;
        break;
      case GST_VIDEO_FORMAT_GRAY16_LE:
        bpp = depth = 16;
        endianness = G_LITTLE_ENDIAN;
        break;
      default:
        return NULL;
        break;
    }

    if (bpp <= 8) {
      caps = gst_caps_new_simple ("video/x-raw-gray",
          "bpp", G_TYPE_INT, bpp, "depth", G_TYPE_INT, depth, NULL);
    } else {
      caps = gst_caps_new_simple ("video/x-raw-gray",
          "bpp", G_TYPE_INT, bpp,
          "depth", G_TYPE_INT, depth,
          "endianness", G_TYPE_INT, endianness, NULL);
    }

    return caps;
  }

  return NULL;
}

/**
 * gst_video_format_new_template_caps:
 * @format: the #GstVideoFormat describing the raw video format
 *
 * Creates a new #GstCaps object based on the parameters provided.
 * Size, frame rate, and pixel aspect ratio are set to the full
 * range.
 *
 * Since: 0.10.33
 *
 * Returns: a new #GstCaps object, or NULL if there was an error
 */
GstCaps *
gst_video_format_new_template_caps (GstVideoFormat format)
{
  GstCaps *caps;
  GstStructure *structure;

  g_return_val_if_fail (format != GST_VIDEO_FORMAT_UNKNOWN, NULL);

  caps = gst_video_format_new_caps_raw (format);
  if (caps) {
    GValue value = { 0 };
    GValue v = { 0 };

    structure = gst_caps_get_structure (caps, 0);

    gst_structure_set (structure,
        "width", GST_TYPE_INT_RANGE, 1, G_MAXINT,
        "height", GST_TYPE_INT_RANGE, 1, G_MAXINT,
        "framerate", GST_TYPE_FRACTION_RANGE, 0, 1, G_MAXINT, 1,
        "pixel-aspect-ratio", GST_TYPE_FRACTION_RANGE, 0, 1, G_MAXINT, 1, NULL);

    g_value_init (&value, GST_TYPE_LIST);
    g_value_init (&v, G_TYPE_BOOLEAN);
    g_value_set_boolean (&v, TRUE);
    gst_value_list_append_value (&value, &v);
    g_value_set_boolean (&v, FALSE);
    gst_value_list_append_value (&value, &v);

    gst_structure_set_value (structure, "interlaced", &value);

    g_value_reset (&value);
    g_value_reset (&v);
  }

  return caps;
}

/**
 * gst_video_format_new_caps:
 * @format: the #GstVideoFormat describing the raw video format
 * @width: width of video
 * @height: height of video
 * @framerate_n: numerator of frame rate
 * @framerate_d: denominator of frame rate
 * @par_n: numerator of pixel aspect ratio
 * @par_d: denominator of pixel aspect ratio
 *
 * Creates a new #GstCaps object based on the parameters provided.
 *
 * Since: 0.10.16
 *
 * Returns: a new #GstCaps object, or NULL if there was an error
 */
GstCaps *
gst_video_format_new_caps (GstVideoFormat format, int width,
    int height, int framerate_n, int framerate_d, int par_n, int par_d)
{
  GstCaps *caps;
  GstStructure *structure;

  g_return_val_if_fail (format != GST_VIDEO_FORMAT_UNKNOWN, NULL);
  g_return_val_if_fail (width > 0 && height > 0, NULL);

  caps = gst_video_format_new_caps_raw (format);
  if (caps) {
    structure = gst_caps_get_structure (caps, 0);

    gst_structure_set (structure,
        "width", G_TYPE_INT, width,
        "height", G_TYPE_INT, height,
        "framerate", GST_TYPE_FRACTION, framerate_n, framerate_d,
        "pixel-aspect-ratio", GST_TYPE_FRACTION, par_n, par_d, NULL);
  }

  return caps;
}


/**
 * gst_video_format_from_fourcc:
 * @fourcc: a FOURCC value representing raw YUV video
 *
 * Converts a FOURCC value into the corresponding #GstVideoFormat.
 * If the FOURCC cannot be represented by #GstVideoFormat,
 * #GST_VIDEO_FORMAT_UNKNOWN is returned.
 *
 * Since: 0.10.16
 *
 * Returns: the #GstVideoFormat describing the FOURCC value
 */
GstVideoFormat
gst_video_format_from_fourcc (guint32 fourcc)
{
  switch (fourcc) {
    case GST_MAKE_FOURCC ('I', '4', '2', '0'):
      return GST_VIDEO_FORMAT_I420;
    case GST_MAKE_FOURCC ('Y', 'V', '1', '2'):
      return GST_VIDEO_FORMAT_YV12;
    case GST_MAKE_FOURCC ('Y', 'U', 'Y', '2'):
      return GST_VIDEO_FORMAT_YUY2;
    case GST_MAKE_FOURCC ('Y', 'V', 'Y', 'U'):
      return GST_VIDEO_FORMAT_YVYU;
    case GST_MAKE_FOURCC ('U', 'Y', 'V', 'Y'):
      return GST_VIDEO_FORMAT_UYVY;
    case GST_MAKE_FOURCC ('A', 'Y', 'U', 'V'):
      return GST_VIDEO_FORMAT_AYUV;
    case GST_MAKE_FOURCC ('Y', '4', '1', 'B'):
      return GST_VIDEO_FORMAT_Y41B;
    case GST_MAKE_FOURCC ('Y', '4', '2', 'B'):
      return GST_VIDEO_FORMAT_Y42B;
    case GST_MAKE_FOURCC ('Y', '4', '4', '4'):
      return GST_VIDEO_FORMAT_Y444;
    case GST_MAKE_FOURCC ('v', '2', '1', '0'):
      return GST_VIDEO_FORMAT_v210;
    case GST_MAKE_FOURCC ('v', '2', '1', '6'):
      return GST_VIDEO_FORMAT_v216;
    case GST_MAKE_FOURCC ('N', 'V', '1', '2'):
      return GST_VIDEO_FORMAT_NV12;
    case GST_MAKE_FOURCC ('N', 'V', '2', '1'):
      return GST_VIDEO_FORMAT_NV21;
    case GST_MAKE_FOURCC ('v', '3', '0', '8'):
      return GST_VIDEO_FORMAT_v308;
    case GST_MAKE_FOURCC ('Y', '8', '0', '0'):
    case GST_MAKE_FOURCC ('Y', '8', ' ', ' '):
    case GST_MAKE_FOURCC ('G', 'R', 'E', 'Y'):
      return GST_VIDEO_FORMAT_Y800;
    case GST_MAKE_FOURCC ('Y', '1', '6', ' '):
      return GST_VIDEO_FORMAT_Y16;
    case GST_MAKE_FOURCC ('U', 'Y', 'V', 'P'):
      return GST_VIDEO_FORMAT_UYVP;
    case GST_MAKE_FOURCC ('A', '4', '2', '0'):
      return GST_VIDEO_FORMAT_A420;
    case GST_MAKE_FOURCC ('Y', 'U', 'V', '9'):
      return GST_VIDEO_FORMAT_YUV9;
    case GST_MAKE_FOURCC ('Y', 'V', 'U', '9'):
      return GST_VIDEO_FORMAT_YVU9;
    case GST_MAKE_FOURCC ('I', 'Y', 'U', '1'):
      return GST_VIDEO_FORMAT_IYU1;
    case GST_MAKE_FOURCC ('A', 'Y', '6', '4'):
      return GST_VIDEO_FORMAT_AYUV64;
    default:
      return GST_VIDEO_FORMAT_UNKNOWN;
  }
}
#endif // GSTREAMER_LITE

/**
 * gst_video_format_to_fourcc:
 * @format: a #GstVideoFormat video format
 *
 * Converts a #GstVideoFormat value into the corresponding FOURCC.  Only
 * a few YUV formats have corresponding FOURCC values.  If @format has
 * no corresponding FOURCC value, 0 is returned.
 *
 * Since: 0.10.16
 *
 * Returns: the FOURCC corresponding to @format
 */
guint32
gst_video_format_to_fourcc (GstVideoFormat format)
{
  g_return_val_if_fail (format != GST_VIDEO_FORMAT_UNKNOWN, 0);

  switch (format) {
    case GST_VIDEO_FORMAT_I420:
      return GST_MAKE_FOURCC ('I', '4', '2', '0');
    case GST_VIDEO_FORMAT_YV12:
      return GST_MAKE_FOURCC ('Y', 'V', '1', '2');
    case GST_VIDEO_FORMAT_YUY2:
      return GST_MAKE_FOURCC ('Y', 'U', 'Y', '2');
    case GST_VIDEO_FORMAT_YVYU:
      return GST_MAKE_FOURCC ('Y', 'V', 'Y', 'U');
    case GST_VIDEO_FORMAT_UYVY:
      return GST_MAKE_FOURCC ('U', 'Y', 'V', 'Y');
    case GST_VIDEO_FORMAT_AYUV:
      return GST_MAKE_FOURCC ('A', 'Y', 'U', 'V');
    case GST_VIDEO_FORMAT_Y41B:
      return GST_MAKE_FOURCC ('Y', '4', '1', 'B');
    case GST_VIDEO_FORMAT_Y42B:
      return GST_MAKE_FOURCC ('Y', '4', '2', 'B');
    case GST_VIDEO_FORMAT_Y444:
      return GST_MAKE_FOURCC ('Y', '4', '4', '4');
    case GST_VIDEO_FORMAT_v210:
      return GST_MAKE_FOURCC ('v', '2', '1', '0');
    case GST_VIDEO_FORMAT_v216:
      return GST_MAKE_FOURCC ('v', '2', '1', '6');
    case GST_VIDEO_FORMAT_NV12:
      return GST_MAKE_FOURCC ('N', 'V', '1', '2');
    case GST_VIDEO_FORMAT_NV21:
      return GST_MAKE_FOURCC ('N', 'V', '2', '1');
    case GST_VIDEO_FORMAT_v308:
      return GST_MAKE_FOURCC ('v', '3', '0', '8');
    case GST_VIDEO_FORMAT_Y800:
      return GST_MAKE_FOURCC ('Y', '8', '0', '0');
    case GST_VIDEO_FORMAT_Y16:
      return GST_MAKE_FOURCC ('Y', '1', '6', ' ');
    case GST_VIDEO_FORMAT_UYVP:
      return GST_MAKE_FOURCC ('U', 'Y', 'V', 'P');
    case GST_VIDEO_FORMAT_A420:
      return GST_MAKE_FOURCC ('A', '4', '2', '0');
    case GST_VIDEO_FORMAT_YUV9:
      return GST_MAKE_FOURCC ('Y', 'U', 'V', '9');
    case GST_VIDEO_FORMAT_YVU9:
      return GST_MAKE_FOURCC ('Y', 'V', 'U', '9');
    case GST_VIDEO_FORMAT_IYU1:
      return GST_MAKE_FOURCC ('I', 'Y', 'U', '1');
    case GST_VIDEO_FORMAT_AYUV64:
      return GST_MAKE_FOURCC ('A', 'Y', '6', '4');
    default:
      return 0;
  }
}

#ifndef GSTREAMER_LITE
/*
 * gst_video_format_from_rgb32_masks:
 * @red_mask: red bit mask
 * @green_mask: green bit mask
 * @blue_mask: blue bit mask
 *
 * Converts red, green, blue bit masks into the corresponding
 * #GstVideoFormat.  
 *
 * Since: 0.10.16
 *
 * Returns: the #GstVideoFormat corresponding to the bit masks
 */
static GstVideoFormat
gst_video_format_from_rgb32_masks (int red_mask, int green_mask, int blue_mask)
{
  if (red_mask == 0xff000000 && green_mask == 0x00ff0000 &&
      blue_mask == 0x0000ff00) {
    return GST_VIDEO_FORMAT_RGBx;
  }
  if (red_mask == 0x0000ff00 && green_mask == 0x00ff0000 &&
      blue_mask == 0xff000000) {
    return GST_VIDEO_FORMAT_BGRx;
  }
  if (red_mask == 0x00ff0000 && green_mask == 0x0000ff00 &&
      blue_mask == 0x000000ff) {
    return GST_VIDEO_FORMAT_xRGB;
  }
  if (red_mask == 0x000000ff && green_mask == 0x0000ff00 &&
      blue_mask == 0x00ff0000) {
    return GST_VIDEO_FORMAT_xBGR;
  }

  return GST_VIDEO_FORMAT_UNKNOWN;
}

static GstVideoFormat
gst_video_format_from_rgba32_masks (int red_mask, int green_mask,
    int blue_mask, int alpha_mask)
{
  if (red_mask == 0xff000000 && green_mask == 0x00ff0000 &&
      blue_mask == 0x0000ff00 && alpha_mask == 0x000000ff) {
    return GST_VIDEO_FORMAT_RGBA;
  }
  if (red_mask == 0x0000ff00 && green_mask == 0x00ff0000 &&
      blue_mask == 0xff000000 && alpha_mask == 0x000000ff) {
    return GST_VIDEO_FORMAT_BGRA;
  }
  if (red_mask == 0x00ff0000 && green_mask == 0x0000ff00 &&
      blue_mask == 0x000000ff && alpha_mask == 0xff000000) {
    return GST_VIDEO_FORMAT_ARGB;
  }
  if (red_mask == 0x000000ff && green_mask == 0x0000ff00 &&
      blue_mask == 0x00ff0000 && alpha_mask == 0xff000000) {
    return GST_VIDEO_FORMAT_ABGR;
  }

  return GST_VIDEO_FORMAT_UNKNOWN;
}

static GstVideoFormat
gst_video_format_from_rgb24_masks (int red_mask, int green_mask, int blue_mask)
{
  if (red_mask == 0xff0000 && green_mask == 0x00ff00 && blue_mask == 0x0000ff) {
    return GST_VIDEO_FORMAT_RGB;
  }
  if (red_mask == 0x0000ff && green_mask == 0x00ff00 && blue_mask == 0xff0000) {
    return GST_VIDEO_FORMAT_BGR;
  }

  return GST_VIDEO_FORMAT_UNKNOWN;
}

static GstVideoFormat
gst_video_format_from_rgb16_masks (int red_mask, int green_mask, int blue_mask)
{
  if (red_mask == GST_VIDEO_COMP1_MASK_16_INT
      && green_mask == GST_VIDEO_COMP2_MASK_16_INT
      && blue_mask == GST_VIDEO_COMP3_MASK_16_INT) {
    return GST_VIDEO_FORMAT_RGB16;
  }
  if (red_mask == GST_VIDEO_COMP3_MASK_16_INT
      && green_mask == GST_VIDEO_COMP2_MASK_16_INT
      && blue_mask == GST_VIDEO_COMP1_MASK_16_INT) {
    return GST_VIDEO_FORMAT_BGR16;
  }
  if (red_mask == GST_VIDEO_COMP1_MASK_15_INT
      && green_mask == GST_VIDEO_COMP2_MASK_15_INT
      && blue_mask == GST_VIDEO_COMP3_MASK_15_INT) {
    return GST_VIDEO_FORMAT_RGB15;
  }
  if (red_mask == GST_VIDEO_COMP3_MASK_15_INT
      && green_mask == GST_VIDEO_COMP2_MASK_15_INT
      && blue_mask == GST_VIDEO_COMP1_MASK_15_INT) {
    return GST_VIDEO_FORMAT_BGR15;
  }

  return GST_VIDEO_FORMAT_UNKNOWN;
}
#endif // GSTREAMER_LITE

/**
 * gst_video_format_is_rgb:
 * @format: a #GstVideoFormat
 *
 * Determine whether the video format is an RGB format.
 *
 * Since: 0.10.16
 *
 * Returns: TRUE if @format represents RGB video
 */
gboolean
gst_video_format_is_rgb (GstVideoFormat format)
{
  switch (format) {
    case GST_VIDEO_FORMAT_I420:
    case GST_VIDEO_FORMAT_YV12:
    case GST_VIDEO_FORMAT_YUY2:
    case GST_VIDEO_FORMAT_YVYU:
    case GST_VIDEO_FORMAT_UYVY:
    case GST_VIDEO_FORMAT_AYUV:
    case GST_VIDEO_FORMAT_Y41B:
    case GST_VIDEO_FORMAT_Y42B:
    case GST_VIDEO_FORMAT_Y444:
    case GST_VIDEO_FORMAT_v210:
    case GST_VIDEO_FORMAT_v216:
    case GST_VIDEO_FORMAT_NV12:
    case GST_VIDEO_FORMAT_NV21:
    case GST_VIDEO_FORMAT_v308:
    case GST_VIDEO_FORMAT_UYVP:
    case GST_VIDEO_FORMAT_A420:
    case GST_VIDEO_FORMAT_YUV9:
    case GST_VIDEO_FORMAT_YVU9:
    case GST_VIDEO_FORMAT_IYU1:
    case GST_VIDEO_FORMAT_AYUV64:
      return FALSE;
    case GST_VIDEO_FORMAT_RGBx:
    case GST_VIDEO_FORMAT_BGRx:
    case GST_VIDEO_FORMAT_xRGB:
    case GST_VIDEO_FORMAT_xBGR:
    case GST_VIDEO_FORMAT_RGBA:
    case GST_VIDEO_FORMAT_BGRA:
    case GST_VIDEO_FORMAT_ARGB:
    case GST_VIDEO_FORMAT_ABGR:
    case GST_VIDEO_FORMAT_RGB:
    case GST_VIDEO_FORMAT_BGR:
    case GST_VIDEO_FORMAT_RGB16:
    case GST_VIDEO_FORMAT_BGR16:
    case GST_VIDEO_FORMAT_RGB15:
    case GST_VIDEO_FORMAT_BGR15:
    case GST_VIDEO_FORMAT_RGB8_PALETTED:
    case GST_VIDEO_FORMAT_ARGB64:
    case GST_VIDEO_FORMAT_r210:
      return TRUE;
    default:
      return FALSE;
  }
}

/**
 * gst_video_format_is_yuv:
 * @format: a #GstVideoFormat
 *
 * Determine whether the video format is a YUV format.
 *
 * Since: 0.10.16
 *
 * Returns: TRUE if @format represents YUV video
 */
gboolean
gst_video_format_is_yuv (GstVideoFormat format)
{
  switch (format) {
    case GST_VIDEO_FORMAT_I420:
    case GST_VIDEO_FORMAT_YV12:
    case GST_VIDEO_FORMAT_YUY2:
    case GST_VIDEO_FORMAT_YVYU:
    case GST_VIDEO_FORMAT_UYVY:
    case GST_VIDEO_FORMAT_AYUV:
    case GST_VIDEO_FORMAT_Y41B:
    case GST_VIDEO_FORMAT_Y42B:
    case GST_VIDEO_FORMAT_Y444:
    case GST_VIDEO_FORMAT_v210:
    case GST_VIDEO_FORMAT_v216:
    case GST_VIDEO_FORMAT_NV12:
    case GST_VIDEO_FORMAT_NV21:
    case GST_VIDEO_FORMAT_v308:
    case GST_VIDEO_FORMAT_Y800:
    case GST_VIDEO_FORMAT_Y16:
    case GST_VIDEO_FORMAT_UYVP:
    case GST_VIDEO_FORMAT_A420:
    case GST_VIDEO_FORMAT_YUV9:
    case GST_VIDEO_FORMAT_YVU9:
    case GST_VIDEO_FORMAT_IYU1:
    case GST_VIDEO_FORMAT_AYUV64:
      return TRUE;
    case GST_VIDEO_FORMAT_RGBx:
    case GST_VIDEO_FORMAT_BGRx:
    case GST_VIDEO_FORMAT_xRGB:
    case GST_VIDEO_FORMAT_xBGR:
    case GST_VIDEO_FORMAT_RGBA:
    case GST_VIDEO_FORMAT_BGRA:
    case GST_VIDEO_FORMAT_ARGB:
    case GST_VIDEO_FORMAT_ABGR:
    case GST_VIDEO_FORMAT_RGB:
    case GST_VIDEO_FORMAT_BGR:
    case GST_VIDEO_FORMAT_RGB16:
    case GST_VIDEO_FORMAT_BGR16:
    case GST_VIDEO_FORMAT_RGB15:
    case GST_VIDEO_FORMAT_BGR15:
    case GST_VIDEO_FORMAT_RGB8_PALETTED:
    case GST_VIDEO_FORMAT_ARGB64:
    case GST_VIDEO_FORMAT_r210:
      return FALSE;
    default:
      return FALSE;
  }
}

/**
 * gst_video_format_is_gray:
 * @format: a #GstVideoFormat
 *
 * Determine whether the video format is a grayscale format.
 *
 * Since: 0.10.29
 *
 * Returns: TRUE if @format represents grayscale video
 */
gboolean
gst_video_format_is_gray (GstVideoFormat format)
{
  switch (format) {
    case GST_VIDEO_FORMAT_GRAY8:
    case GST_VIDEO_FORMAT_GRAY16_BE:
    case GST_VIDEO_FORMAT_GRAY16_LE:
    case GST_VIDEO_FORMAT_Y800:
    case GST_VIDEO_FORMAT_Y16:
      return TRUE;
    default:
      return FALSE;
  }
}

#ifndef GSTREAMER_LITE
/**
 * gst_video_format_has_alpha:
 * @format: a #GstVideoFormat
 * 
 * Returns TRUE or FALSE depending on if the video format provides an
 * alpha channel.
 *
 * Since: 0.10.16
 *
 * Returns: TRUE if @format has an alpha channel
 */
gboolean
gst_video_format_has_alpha (GstVideoFormat format)
{
  switch (format) {
    case GST_VIDEO_FORMAT_I420:
    case GST_VIDEO_FORMAT_YV12:
    case GST_VIDEO_FORMAT_YUY2:
    case GST_VIDEO_FORMAT_YVYU:
    case GST_VIDEO_FORMAT_UYVY:
    case GST_VIDEO_FORMAT_Y41B:
    case GST_VIDEO_FORMAT_Y42B:
    case GST_VIDEO_FORMAT_Y444:
    case GST_VIDEO_FORMAT_v210:
    case GST_VIDEO_FORMAT_v216:
    case GST_VIDEO_FORMAT_NV12:
    case GST_VIDEO_FORMAT_NV21:
    case GST_VIDEO_FORMAT_v308:
    case GST_VIDEO_FORMAT_Y800:
    case GST_VIDEO_FORMAT_Y16:
    case GST_VIDEO_FORMAT_UYVP:
    case GST_VIDEO_FORMAT_YUV9:
    case GST_VIDEO_FORMAT_YVU9:
    case GST_VIDEO_FORMAT_IYU1:
      return FALSE;
    case GST_VIDEO_FORMAT_AYUV:
    case GST_VIDEO_FORMAT_RGBA:
    case GST_VIDEO_FORMAT_BGRA:
    case GST_VIDEO_FORMAT_ARGB:
    case GST_VIDEO_FORMAT_ABGR:
    case GST_VIDEO_FORMAT_A420:
    case GST_VIDEO_FORMAT_RGB8_PALETTED:
    case GST_VIDEO_FORMAT_ARGB64:
    case GST_VIDEO_FORMAT_AYUV64:
      return TRUE;
    case GST_VIDEO_FORMAT_RGBx:
    case GST_VIDEO_FORMAT_BGRx:
    case GST_VIDEO_FORMAT_xRGB:
    case GST_VIDEO_FORMAT_xBGR:
    case GST_VIDEO_FORMAT_RGB:
    case GST_VIDEO_FORMAT_BGR:
    case GST_VIDEO_FORMAT_RGB16:
    case GST_VIDEO_FORMAT_BGR16:
    case GST_VIDEO_FORMAT_RGB15:
    case GST_VIDEO_FORMAT_BGR15:
    case GST_VIDEO_FORMAT_r210:
      return FALSE;
    default:
      return FALSE;
  }
}

/**
 * gst_video_format_get_component_depth:
 * @format: a #GstVideoFormat
 * 
 * Returns the number of bits used to encode an individual pixel of
 * a given component.  Typically this is 8, although higher and lower
 * values are possible for some formats.
 *
 * Since: 0.10.33
 *
 * Returns: depth of component
 */
int
gst_video_format_get_component_depth (GstVideoFormat format, int component)
{
  if (component == 3 && !gst_video_format_has_alpha (format))
    return 0;

  switch (format) {
    case GST_VIDEO_FORMAT_RGB16:
    case GST_VIDEO_FORMAT_BGR16:
      if (component == 1)
        return 6;
      return 5;
    case GST_VIDEO_FORMAT_RGB15:
    case GST_VIDEO_FORMAT_BGR15:
      return 5;
    case GST_VIDEO_FORMAT_I420:
    case GST_VIDEO_FORMAT_YV12:
    case GST_VIDEO_FORMAT_YUY2:
    case GST_VIDEO_FORMAT_YVYU:
    case GST_VIDEO_FORMAT_UYVY:
    case GST_VIDEO_FORMAT_Y41B:
    case GST_VIDEO_FORMAT_Y42B:
    case GST_VIDEO_FORMAT_Y444:
    case GST_VIDEO_FORMAT_NV12:
    case GST_VIDEO_FORMAT_NV21:
    case GST_VIDEO_FORMAT_v308:
    case GST_VIDEO_FORMAT_Y800:
    case GST_VIDEO_FORMAT_YUV9:
    case GST_VIDEO_FORMAT_YVU9:
    case GST_VIDEO_FORMAT_IYU1:
    case GST_VIDEO_FORMAT_AYUV:
    case GST_VIDEO_FORMAT_RGBA:
    case GST_VIDEO_FORMAT_BGRA:
    case GST_VIDEO_FORMAT_ARGB:
    case GST_VIDEO_FORMAT_ABGR:
    case GST_VIDEO_FORMAT_A420:
    case GST_VIDEO_FORMAT_RGB8_PALETTED:
    case GST_VIDEO_FORMAT_RGBx:
    case GST_VIDEO_FORMAT_BGRx:
    case GST_VIDEO_FORMAT_xRGB:
    case GST_VIDEO_FORMAT_xBGR:
    case GST_VIDEO_FORMAT_RGB:
    case GST_VIDEO_FORMAT_BGR:
    default:
      return 8;
    case GST_VIDEO_FORMAT_v210:
    case GST_VIDEO_FORMAT_UYVP:
    case GST_VIDEO_FORMAT_r210:
      return 10;
    case GST_VIDEO_FORMAT_Y16:
    case GST_VIDEO_FORMAT_v216:
    case GST_VIDEO_FORMAT_ARGB64:
    case GST_VIDEO_FORMAT_AYUV64:
      return 16;
  }

}

/**
 * gst_video_format_get_row_stride:
 * @format: a #GstVideoFormat
 * @component: the component index
 * @width: the width of video
 *
 * Calculates the row stride (number of bytes from one row of pixels to
 * the next) for the video component with an index of @component.  For
 * YUV video, Y, U, and V have component indices of 0, 1, and 2,
 * respectively.  For RGB video, R, G, and B have component indicies of
 * 0, 1, and 2, respectively.  Alpha channels, if present, have a component
 * index of 3.  The @width parameter always represents the width of the
 * video, not the component.
 *
 * Since: 0.10.16
 *
 * Returns: row stride of component @component
 */
int
gst_video_format_get_row_stride (GstVideoFormat format, int component,
    int width)
{
  g_return_val_if_fail (format != GST_VIDEO_FORMAT_UNKNOWN, 0);
  g_return_val_if_fail (component >= 0 && component <= 3, 0);
  g_return_val_if_fail (width > 0, 0);

  switch (format) {
    case GST_VIDEO_FORMAT_I420:
    case GST_VIDEO_FORMAT_YV12:
      if (component == 0) {
        return GST_ROUND_UP_4 (width);
      } else {
        return GST_ROUND_UP_4 (GST_ROUND_UP_2 (width) / 2);
      }
    case GST_VIDEO_FORMAT_YUY2:
    case GST_VIDEO_FORMAT_YVYU:
    case GST_VIDEO_FORMAT_UYVY:
      return GST_ROUND_UP_4 (width * 2);
    case GST_VIDEO_FORMAT_AYUV:
    case GST_VIDEO_FORMAT_RGBx:
    case GST_VIDEO_FORMAT_BGRx:
    case GST_VIDEO_FORMAT_xRGB:
    case GST_VIDEO_FORMAT_xBGR:
    case GST_VIDEO_FORMAT_RGBA:
    case GST_VIDEO_FORMAT_BGRA:
    case GST_VIDEO_FORMAT_ARGB:
    case GST_VIDEO_FORMAT_ABGR:
    case GST_VIDEO_FORMAT_r210:
      return width * 4;
    case GST_VIDEO_FORMAT_RGB16:
    case GST_VIDEO_FORMAT_BGR16:
    case GST_VIDEO_FORMAT_RGB15:
    case GST_VIDEO_FORMAT_BGR15:
      return GST_ROUND_UP_4 (width * 2);
    case GST_VIDEO_FORMAT_RGB:
    case GST_VIDEO_FORMAT_BGR:
    case GST_VIDEO_FORMAT_v308:
      return GST_ROUND_UP_4 (width * 3);
    case GST_VIDEO_FORMAT_Y41B:
      if (component == 0) {
        return GST_ROUND_UP_4 (width);
      } else {
        return GST_ROUND_UP_16 (width) / 4;
      }
    case GST_VIDEO_FORMAT_Y42B:
      if (component == 0) {
        return GST_ROUND_UP_4 (width);
      } else {
        return GST_ROUND_UP_8 (width) / 2;
      }
    case GST_VIDEO_FORMAT_Y444:
      return GST_ROUND_UP_4 (width);
    case GST_VIDEO_FORMAT_v210:
      return ((width + 47) / 48) * 128;
    case GST_VIDEO_FORMAT_v216:
      return GST_ROUND_UP_8 (width * 4);
    case GST_VIDEO_FORMAT_NV12:
    case GST_VIDEO_FORMAT_NV21:
      return GST_ROUND_UP_4 (width);
    case GST_VIDEO_FORMAT_GRAY8:
    case GST_VIDEO_FORMAT_Y800:
      return GST_ROUND_UP_4 (width);
    case GST_VIDEO_FORMAT_GRAY16_BE:
    case GST_VIDEO_FORMAT_GRAY16_LE:
    case GST_VIDEO_FORMAT_Y16:
      return GST_ROUND_UP_4 (width * 2);
    case GST_VIDEO_FORMAT_UYVP:
      return GST_ROUND_UP_4 ((width * 2 * 5 + 3) / 4);
    case GST_VIDEO_FORMAT_A420:
      if (component == 0 || component == 3) {
        return GST_ROUND_UP_4 (width);
      } else {
        return GST_ROUND_UP_4 (GST_ROUND_UP_2 (width) / 2);
      }
    case GST_VIDEO_FORMAT_RGB8_PALETTED:
      return GST_ROUND_UP_4 (width);
    case GST_VIDEO_FORMAT_YUV9:
    case GST_VIDEO_FORMAT_YVU9:
      if (component == 0) {
        return GST_ROUND_UP_4 (width);
      } else {
        return GST_ROUND_UP_4 (GST_ROUND_UP_4 (width) / 4);
      }
    case GST_VIDEO_FORMAT_IYU1:
      return GST_ROUND_UP_4 (GST_ROUND_UP_4 (width) +
          GST_ROUND_UP_4 (width) / 2);
    case GST_VIDEO_FORMAT_ARGB64:
    case GST_VIDEO_FORMAT_AYUV64:
      return width * 8;
    default:
      return 0;
  }
}

/**
 * gst_video_format_get_pixel_stride:
 * @format: a #GstVideoFormat
 * @component: the component index
 *
 * Calculates the pixel stride (number of bytes from one pixel to the
 * pixel to its immediate left) for the video component with an index
 * of @component.  See @gst_video_format_get_row_stride for a description
 * of the component index.
 *
 * Since: 0.10.16
 *
 * Returns: pixel stride of component @component
 */
int
gst_video_format_get_pixel_stride (GstVideoFormat format, int component)
{
  g_return_val_if_fail (format != GST_VIDEO_FORMAT_UNKNOWN, 0);
  g_return_val_if_fail (component >= 0 && component <= 3, 0);

  switch (format) {
    case GST_VIDEO_FORMAT_I420:
    case GST_VIDEO_FORMAT_YV12:
    case GST_VIDEO_FORMAT_Y41B:
    case GST_VIDEO_FORMAT_Y42B:
    case GST_VIDEO_FORMAT_Y444:
    case GST_VIDEO_FORMAT_A420:
    case GST_VIDEO_FORMAT_YUV9:
    case GST_VIDEO_FORMAT_YVU9:
      return 1;
    case GST_VIDEO_FORMAT_YUY2:
    case GST_VIDEO_FORMAT_YVYU:
    case GST_VIDEO_FORMAT_UYVY:
      if (component == 0) {
        return 2;
      } else {
        return 4;
      }
    case GST_VIDEO_FORMAT_IYU1:
      /* doesn't make much sense for IYU1 because it's 1 or 3
       * for luma depending on position */
      return 0;
    case GST_VIDEO_FORMAT_AYUV:
    case GST_VIDEO_FORMAT_RGBx:
    case GST_VIDEO_FORMAT_BGRx:
    case GST_VIDEO_FORMAT_xRGB:
    case GST_VIDEO_FORMAT_xBGR:
    case GST_VIDEO_FORMAT_RGBA:
    case GST_VIDEO_FORMAT_BGRA:
    case GST_VIDEO_FORMAT_ARGB:
    case GST_VIDEO_FORMAT_ABGR:
    case GST_VIDEO_FORMAT_r210:
      return 4;
    case GST_VIDEO_FORMAT_RGB16:
    case GST_VIDEO_FORMAT_BGR16:
    case GST_VIDEO_FORMAT_RGB15:
    case GST_VIDEO_FORMAT_BGR15:
      return 2;
    case GST_VIDEO_FORMAT_RGB:
    case GST_VIDEO_FORMAT_BGR:
    case GST_VIDEO_FORMAT_v308:
      return 3;
    case GST_VIDEO_FORMAT_v210:
      /* v210 is packed at the bit level, so pixel stride doesn't make sense */
      return 0;
    case GST_VIDEO_FORMAT_v216:
      if (component == 0) {
        return 4;
      } else {
        return 8;
      }
    case GST_VIDEO_FORMAT_NV12:
    case GST_VIDEO_FORMAT_NV21:
      if (component == 0) {
        return 1;
      } else {
        return 2;
      }
    case GST_VIDEO_FORMAT_GRAY8:
    case GST_VIDEO_FORMAT_Y800:
      return 1;
    case GST_VIDEO_FORMAT_GRAY16_BE:
    case GST_VIDEO_FORMAT_GRAY16_LE:
    case GST_VIDEO_FORMAT_Y16:
      return 2;
    case GST_VIDEO_FORMAT_UYVP:
      /* UYVP is packed at the bit level, so pixel stride doesn't make sense */
      return 0;
    case GST_VIDEO_FORMAT_RGB8_PALETTED:
      return 1;
    case GST_VIDEO_FORMAT_ARGB64:
    case GST_VIDEO_FORMAT_AYUV64:
      return 8;
    default:
      return 0;
  }
}

/**
 * gst_video_format_get_component_width:
 * @format: a #GstVideoFormat
 * @component: the component index
 * @width: the width of video
 *
 * Calculates the width of the component.  See
 * @gst_video_format_get_row_stride for a description
 * of the component index.
 *
 * Since: 0.10.16
 *
 * Returns: width of component @component
 */
int
gst_video_format_get_component_width (GstVideoFormat format,
    int component, int width)
{
  g_return_val_if_fail (format != GST_VIDEO_FORMAT_UNKNOWN, 0);
  g_return_val_if_fail (component >= 0 && component <= 3, 0);
  g_return_val_if_fail (width > 0, 0);

  switch (format) {
    case GST_VIDEO_FORMAT_I420:
    case GST_VIDEO_FORMAT_YV12:
    case GST_VIDEO_FORMAT_YUY2:
    case GST_VIDEO_FORMAT_YVYU:
    case GST_VIDEO_FORMAT_UYVY:
    case GST_VIDEO_FORMAT_Y42B:
    case GST_VIDEO_FORMAT_v210:
    case GST_VIDEO_FORMAT_v216:
    case GST_VIDEO_FORMAT_NV12:
    case GST_VIDEO_FORMAT_NV21:
    case GST_VIDEO_FORMAT_UYVP:
      if (component == 0) {
        return width;
      } else {
        return GST_ROUND_UP_2 (width) / 2;
      }
    case GST_VIDEO_FORMAT_Y41B:
    case GST_VIDEO_FORMAT_YUV9:
    case GST_VIDEO_FORMAT_YVU9:
    case GST_VIDEO_FORMAT_IYU1:
      if (component == 0) {
        return width;
      } else {
        return GST_ROUND_UP_4 (width) / 4;
      }
    case GST_VIDEO_FORMAT_AYUV:
    case GST_VIDEO_FORMAT_RGBx:
    case GST_VIDEO_FORMAT_BGRx:
    case GST_VIDEO_FORMAT_xRGB:
    case GST_VIDEO_FORMAT_xBGR:
    case GST_VIDEO_FORMAT_RGBA:
    case GST_VIDEO_FORMAT_BGRA:
    case GST_VIDEO_FORMAT_ARGB:
    case GST_VIDEO_FORMAT_ABGR:
    case GST_VIDEO_FORMAT_RGB:
    case GST_VIDEO_FORMAT_BGR:
    case GST_VIDEO_FORMAT_RGB16:
    case GST_VIDEO_FORMAT_BGR16:
    case GST_VIDEO_FORMAT_RGB15:
    case GST_VIDEO_FORMAT_BGR15:
    case GST_VIDEO_FORMAT_Y444:
    case GST_VIDEO_FORMAT_v308:
    case GST_VIDEO_FORMAT_GRAY8:
    case GST_VIDEO_FORMAT_GRAY16_BE:
    case GST_VIDEO_FORMAT_GRAY16_LE:
    case GST_VIDEO_FORMAT_Y800:
    case GST_VIDEO_FORMAT_Y16:
    case GST_VIDEO_FORMAT_RGB8_PALETTED:
    case GST_VIDEO_FORMAT_ARGB64:
    case GST_VIDEO_FORMAT_AYUV64:
    case GST_VIDEO_FORMAT_r210:
      return width;
    case GST_VIDEO_FORMAT_A420:
      if (component == 0 || component == 3) {
        return width;
      } else {
        return GST_ROUND_UP_2 (width) / 2;
      }
    default:
      return 0;
  }
}

/**
 * gst_video_format_get_component_height:
 * @format: a #GstVideoFormat
 * @component: the component index
 * @height: the height of video
 *
 * Calculates the height of the component.  See
 * @gst_video_format_get_row_stride for a description
 * of the component index.
 *
 * Since: 0.10.16
 *
 * Returns: height of component @component
 */
int
gst_video_format_get_component_height (GstVideoFormat format,
    int component, int height)
{
  g_return_val_if_fail (format != GST_VIDEO_FORMAT_UNKNOWN, 0);
  g_return_val_if_fail (component >= 0 && component <= 3, 0);
  g_return_val_if_fail (height > 0, 0);

  switch (format) {
    case GST_VIDEO_FORMAT_I420:
    case GST_VIDEO_FORMAT_YV12:
    case GST_VIDEO_FORMAT_NV12:
    case GST_VIDEO_FORMAT_NV21:
      if (component == 0) {
        return height;
      } else {
        return GST_ROUND_UP_2 (height) / 2;
      }
    case GST_VIDEO_FORMAT_Y41B:
    case GST_VIDEO_FORMAT_Y42B:
    case GST_VIDEO_FORMAT_YUY2:
    case GST_VIDEO_FORMAT_YVYU:
    case GST_VIDEO_FORMAT_UYVY:
    case GST_VIDEO_FORMAT_AYUV:
    case GST_VIDEO_FORMAT_RGBx:
    case GST_VIDEO_FORMAT_BGRx:
    case GST_VIDEO_FORMAT_xRGB:
    case GST_VIDEO_FORMAT_xBGR:
    case GST_VIDEO_FORMAT_RGBA:
    case GST_VIDEO_FORMAT_BGRA:
    case GST_VIDEO_FORMAT_ARGB:
    case GST_VIDEO_FORMAT_ABGR:
    case GST_VIDEO_FORMAT_RGB:
    case GST_VIDEO_FORMAT_BGR:
    case GST_VIDEO_FORMAT_RGB16:
    case GST_VIDEO_FORMAT_BGR16:
    case GST_VIDEO_FORMAT_RGB15:
    case GST_VIDEO_FORMAT_BGR15:
    case GST_VIDEO_FORMAT_Y444:
    case GST_VIDEO_FORMAT_v210:
    case GST_VIDEO_FORMAT_v216:
    case GST_VIDEO_FORMAT_v308:
    case GST_VIDEO_FORMAT_GRAY8:
    case GST_VIDEO_FORMAT_GRAY16_BE:
    case GST_VIDEO_FORMAT_GRAY16_LE:
    case GST_VIDEO_FORMAT_Y800:
    case GST_VIDEO_FORMAT_Y16:
    case GST_VIDEO_FORMAT_UYVP:
    case GST_VIDEO_FORMAT_RGB8_PALETTED:
    case GST_VIDEO_FORMAT_IYU1:
    case GST_VIDEO_FORMAT_ARGB64:
    case GST_VIDEO_FORMAT_AYUV64:
    case GST_VIDEO_FORMAT_r210:
      return height;
    case GST_VIDEO_FORMAT_A420:
      if (component == 0 || component == 3) {
        return height;
      } else {
        return GST_ROUND_UP_2 (height) / 2;
      }
    case GST_VIDEO_FORMAT_YUV9:
    case GST_VIDEO_FORMAT_YVU9:
      if (component == 0) {
        return height;
      } else {
        return GST_ROUND_UP_4 (height) / 4;
      }
    default:
      return 0;
  }
}
#endif // GSTREAMER_LITE

/**
 * gst_video_format_get_component_offset:
 * @format: a #GstVideoFormat
 * @component: the component index
 * @width: the width of video
 * @height: the height of video
 *
 * Calculates the offset (in bytes) of the first pixel of the component
 * with index @component.  For packed formats, this will typically be a
 * small integer (0, 1, 2, 3).  For planar formats, this will be a
 * (relatively) large offset to the beginning of the second or third
 * component planes.  See @gst_video_format_get_row_stride for a description
 * of the component index.
 *
 * Since: 0.10.16
 *
 * Returns: offset of component @component
 */
int
gst_video_format_get_component_offset (GstVideoFormat format,
    int component, int width, int height)
{
  g_return_val_if_fail (format != GST_VIDEO_FORMAT_UNKNOWN, 0);
  g_return_val_if_fail (component >= 0 && component <= 3, 0);
  g_return_val_if_fail ((!gst_video_format_is_yuv (format)) || (width > 0
          && height > 0), 0);

  switch (format) {
    case GST_VIDEO_FORMAT_I420:
      if (component == 0)
        return 0;
      if (component == 1)
        return GST_ROUND_UP_4 (width) * GST_ROUND_UP_2 (height);
      if (component == 2) {
        return GST_ROUND_UP_4 (width) * GST_ROUND_UP_2 (height) +
            GST_ROUND_UP_4 (GST_ROUND_UP_2 (width) / 2) *
            (GST_ROUND_UP_2 (height) / 2);
      }
      return 0;
    case GST_VIDEO_FORMAT_YV12:        /* same as I420, but components 1+2 swapped */
      if (component == 0)
        return 0;
      if (component == 2)
        return GST_ROUND_UP_4 (width) * GST_ROUND_UP_2 (height);
      if (component == 1) {
        return GST_ROUND_UP_4 (width) * GST_ROUND_UP_2 (height) +
            GST_ROUND_UP_4 (GST_ROUND_UP_2 (width) / 2) *
            (GST_ROUND_UP_2 (height) / 2);
      }
      return 0;
    case GST_VIDEO_FORMAT_YUY2:
      if (component == 0)
        return 0;
      if (component == 1)
        return 1;
      if (component == 2)
        return 3;
      return 0;
    case GST_VIDEO_FORMAT_YVYU:
      if (component == 0)
        return 0;
      if (component == 1)
        return 3;
      if (component == 2)
        return 1;
      return 0;
    case GST_VIDEO_FORMAT_UYVY:
      if (component == 0)
        return 1;
      if (component == 1)
        return 0;
      if (component == 2)
        return 2;
      return 0;
    case GST_VIDEO_FORMAT_AYUV:
      if (component == 0)
        return 1;
      if (component == 1)
        return 2;
      if (component == 2)
        return 3;
      if (component == 3)
        return 0;
      return 0;
    case GST_VIDEO_FORMAT_RGBx:
    case GST_VIDEO_FORMAT_RGBA:
      if (component == 0)
        return 0;
      if (component == 1)
        return 1;
      if (component == 2)
        return 2;
      if (component == 3)
        return 3;
      return 0;
    case GST_VIDEO_FORMAT_BGRx:
    case GST_VIDEO_FORMAT_BGRA:
      if (component == 0)
        return 2;
      if (component == 1)
        return 1;
      if (component == 2)
        return 0;
      if (component == 3)
        return 3;
      return 0;
    case GST_VIDEO_FORMAT_xRGB:
    case GST_VIDEO_FORMAT_ARGB:
      if (component == 0)
        return 1;
      if (component == 1)
        return 2;
      if (component == 2)
        return 3;
      if (component == 3)
        return 0;
      return 0;
    case GST_VIDEO_FORMAT_xBGR:
    case GST_VIDEO_FORMAT_ABGR:
      if (component == 0)
        return 3;
      if (component == 1)
        return 2;
      if (component == 2)
        return 1;
      if (component == 3)
        return 0;
      return 0;
    case GST_VIDEO_FORMAT_RGB:
    case GST_VIDEO_FORMAT_v308:
      if (component == 0)
        return 0;
      if (component == 1)
        return 1;
      if (component == 2)
        return 2;
      return 0;
    case GST_VIDEO_FORMAT_BGR:
      if (component == 0)
        return 2;
      if (component == 1)
        return 1;
      if (component == 2)
        return 0;
      return 0;
    case GST_VIDEO_FORMAT_Y41B:
      if (component == 0)
        return 0;
      if (component == 1)
        return GST_ROUND_UP_4 (width) * height;
      if (component == 2)
        return (GST_ROUND_UP_4 (width) +
            (GST_ROUND_UP_16 (width) / 4)) * height;
      return 0;
    case GST_VIDEO_FORMAT_Y42B:
      if (component == 0)
        return 0;
      if (component == 1)
        return GST_ROUND_UP_4 (width) * height;
      if (component == 2)
        return (GST_ROUND_UP_4 (width) + (GST_ROUND_UP_8 (width) / 2)) * height;
      return 0;
    case GST_VIDEO_FORMAT_Y444:
      return GST_ROUND_UP_4 (width) * height * component;
    case GST_VIDEO_FORMAT_v210:
    case GST_VIDEO_FORMAT_r210:
      /* v210 is bit-packed, so this doesn't make sense */
      return 0;
    case GST_VIDEO_FORMAT_v216:
      if (component == 0)
        return 0;
      if (component == 1)
        return 2;
      if (component == 2)
        return 6;
      return 0;
    case GST_VIDEO_FORMAT_NV12:
      if (component == 0)
        return 0;
      if (component == 1)
        return GST_ROUND_UP_4 (width) * GST_ROUND_UP_2 (height);
      if (component == 2)
        return GST_ROUND_UP_4 (width) * GST_ROUND_UP_2 (height) + 1;
    case GST_VIDEO_FORMAT_NV21:
      if (component == 0)
        return 0;
      if (component == 1)
        return GST_ROUND_UP_4 (width) * GST_ROUND_UP_2 (height) + 1;
      if (component == 2)
        return GST_ROUND_UP_4 (width) * GST_ROUND_UP_2 (height);
    case GST_VIDEO_FORMAT_GRAY8:
    case GST_VIDEO_FORMAT_GRAY16_BE:
    case GST_VIDEO_FORMAT_GRAY16_LE:
    case GST_VIDEO_FORMAT_Y800:
    case GST_VIDEO_FORMAT_Y16:
      return 0;
    case GST_VIDEO_FORMAT_UYVP:
      /* UYVP is bit-packed, so this doesn't make sense */
      return 0;
    case GST_VIDEO_FORMAT_A420:
      if (component == 0)
        return 0;
      if (component == 1)
        return GST_ROUND_UP_4 (width) * GST_ROUND_UP_2 (height);
      if (component == 2) {
        return GST_ROUND_UP_4 (width) * GST_ROUND_UP_2 (height) +
            GST_ROUND_UP_4 (GST_ROUND_UP_2 (width) / 2) *
            (GST_ROUND_UP_2 (height) / 2);
      }
      if (component == 3) {
        return GST_ROUND_UP_4 (width) * GST_ROUND_UP_2 (height) +
            2 * GST_ROUND_UP_4 (GST_ROUND_UP_2 (width) / 2) *
            (GST_ROUND_UP_2 (height) / 2);
      }
    case GST_VIDEO_FORMAT_RGB8_PALETTED:
      return 0;
    case GST_VIDEO_FORMAT_YUV9:
      if (component == 0)
        return 0;
      if (component == 1)
        return GST_ROUND_UP_4 (width) * height;
      if (component == 2) {
        return GST_ROUND_UP_4 (width) * height +
            GST_ROUND_UP_4 (GST_ROUND_UP_4 (width) / 4) *
            (GST_ROUND_UP_4 (height) / 4);
      }
      return 0;
    case GST_VIDEO_FORMAT_YVU9:
      if (component == 0)
        return 0;
      if (component == 1) {
        return GST_ROUND_UP_4 (width) * height +
            GST_ROUND_UP_4 (GST_ROUND_UP_4 (width) / 4) *
            (GST_ROUND_UP_4 (height) / 4);
      }
      if (component == 2)
        return GST_ROUND_UP_4 (width) * height;
      return 0;
    case GST_VIDEO_FORMAT_IYU1:
      if (component == 0)
        return 1;
      if (component == 1)
        return 0;
      if (component == 2)
        return 4;
    case GST_VIDEO_FORMAT_ARGB64:
    case GST_VIDEO_FORMAT_AYUV64:
      if (component == 0)
        return 2;
      if (component == 1)
        return 4;
      if (component == 2)
        return 6;
      if (component == 3)
        return 0;
      return 0;
    default:
      return 0;
  }
}

#ifndef GSTREAMER_LITE
/**
 * gst_video_format_get_size:
 * @format: a #GstVideoFormat
 * @width: the width of video
 * @height: the height of video
 *
 * Calculates the total number of bytes in the raw video format.  This
 * number should be used when allocating a buffer for raw video.
 *
 * Since: 0.10.16
 *
 * Returns: size (in bytes) of raw video format
 */
int
gst_video_format_get_size (GstVideoFormat format, int width, int height)
{
  int size;

  g_return_val_if_fail (format != GST_VIDEO_FORMAT_UNKNOWN, 0);
  g_return_val_if_fail (width > 0 && height > 0, 0);

  switch (format) {
    case GST_VIDEO_FORMAT_I420:
    case GST_VIDEO_FORMAT_YV12:
      size = GST_ROUND_UP_4 (width) * GST_ROUND_UP_2 (height);
      size += GST_ROUND_UP_4 (GST_ROUND_UP_2 (width) / 2) *
          (GST_ROUND_UP_2 (height) / 2) * 2;
      return size;
    case GST_VIDEO_FORMAT_IYU1:
      return GST_ROUND_UP_4 (GST_ROUND_UP_4 (width) +
          GST_ROUND_UP_4 (width) / 2) * height;
    case GST_VIDEO_FORMAT_YUY2:
    case GST_VIDEO_FORMAT_YVYU:
    case GST_VIDEO_FORMAT_UYVY:
      return GST_ROUND_UP_4 (width * 2) * height;
    case GST_VIDEO_FORMAT_AYUV:
    case GST_VIDEO_FORMAT_RGBx:
    case GST_VIDEO_FORMAT_BGRx:
    case GST_VIDEO_FORMAT_xRGB:
    case GST_VIDEO_FORMAT_xBGR:
    case GST_VIDEO_FORMAT_RGBA:
    case GST_VIDEO_FORMAT_BGRA:
    case GST_VIDEO_FORMAT_ARGB:
    case GST_VIDEO_FORMAT_ABGR:
    case GST_VIDEO_FORMAT_r210:
      return width * 4 * height;
    case GST_VIDEO_FORMAT_RGB16:
    case GST_VIDEO_FORMAT_BGR16:
    case GST_VIDEO_FORMAT_RGB15:
    case GST_VIDEO_FORMAT_BGR15:
      return GST_ROUND_UP_4 (width * 2) * height;
    case GST_VIDEO_FORMAT_RGB:
    case GST_VIDEO_FORMAT_BGR:
    case GST_VIDEO_FORMAT_v308:
      return GST_ROUND_UP_4 (width * 3) * height;
    case GST_VIDEO_FORMAT_Y41B:
      /* simplification of ROUNDUP4(w)*h + 2*((ROUNDUP16(w)/4)*h */
      return (GST_ROUND_UP_4 (width) + (GST_ROUND_UP_16 (width) / 2)) * height;
    case GST_VIDEO_FORMAT_Y42B:
      /* simplification of ROUNDUP4(w)*h + 2*(ROUNDUP8(w)/2)*h */
      return (GST_ROUND_UP_4 (width) + GST_ROUND_UP_8 (width)) * height;
    case GST_VIDEO_FORMAT_Y444:
      return GST_ROUND_UP_4 (width) * height * 3;
    case GST_VIDEO_FORMAT_v210:
      return ((width + 47) / 48) * 128 * height;
    case GST_VIDEO_FORMAT_v216:
      return GST_ROUND_UP_8 (width * 4) * height;
    case GST_VIDEO_FORMAT_NV12:
    case GST_VIDEO_FORMAT_NV21:
      return GST_ROUND_UP_4 (width) * GST_ROUND_UP_2 (height) * 3 / 2;
    case GST_VIDEO_FORMAT_GRAY8:
    case GST_VIDEO_FORMAT_Y800:
    case GST_VIDEO_FORMAT_RGB8_PALETTED:
      return GST_ROUND_UP_4 (width) * height;
    case GST_VIDEO_FORMAT_GRAY16_BE:
    case GST_VIDEO_FORMAT_GRAY16_LE:
    case GST_VIDEO_FORMAT_Y16:
      return GST_ROUND_UP_4 (width * 2) * height;
    case GST_VIDEO_FORMAT_UYVP:
      return GST_ROUND_UP_4 ((width * 2 * 5 + 3) / 4) * height;
    case GST_VIDEO_FORMAT_A420:
      size = 2 * GST_ROUND_UP_4 (width) * GST_ROUND_UP_2 (height);
      size += GST_ROUND_UP_4 (GST_ROUND_UP_2 (width) / 2) *
          (GST_ROUND_UP_2 (height) / 2) * 2;
      return size;
    case GST_VIDEO_FORMAT_YUV9:
    case GST_VIDEO_FORMAT_YVU9:
      size = GST_ROUND_UP_4 (width) * height;
      size += GST_ROUND_UP_4 (GST_ROUND_UP_4 (width) / 4) *
          (GST_ROUND_UP_4 (height) / 4) * 2;
      return size;
    case GST_VIDEO_FORMAT_ARGB64:
    case GST_VIDEO_FORMAT_AYUV64:
      return width * 8 * height;
    default:
      return 0;
  }
}

/**
 * gst_video_format_convert:
 * @format: a #GstVideoFormat
 * @width: the width of video
 * @height: the height of video
 * @fps_n: frame rate numerator
 * @fps_d: frame rate denominator
 * @src_format: #GstFormat of the @src_value
 * @src_value: value to convert
 * @dest_format: #GstFormat of the @dest_value
 * @dest_value: pointer to destination value
 *
 * Converts among various #GstFormat types.  This function handles
 * GST_FORMAT_BYTES, GST_FORMAT_TIME, and GST_FORMAT_DEFAULT.  For
 * raw video, GST_FORMAT_DEFAULT corresponds to video frames.  This
 * function can be to handle pad queries of the type GST_QUERY_CONVERT.
 *
 * Since: 0.10.16
 *
 * Returns: TRUE if the conversion was successful.
 */
gboolean
gst_video_format_convert (GstVideoFormat format, int width, int height,
    int fps_n, int fps_d,
    GstFormat src_format, gint64 src_value,
    GstFormat dest_format, gint64 * dest_value)
{
  gboolean ret = FALSE;
  int size;

  g_return_val_if_fail (format != GST_VIDEO_FORMAT_UNKNOWN, 0);
  g_return_val_if_fail (width > 0 && height > 0, 0);

  size = gst_video_format_get_size (format, width, height);

  GST_DEBUG ("converting value %" G_GINT64_FORMAT " from %s to %s",
      src_value, gst_format_get_name (src_format),
      gst_format_get_name (dest_format));

  if (src_format == dest_format) {
    *dest_value = src_value;
    ret = TRUE;
    goto done;
  }

  if (src_value == -1) {
    *dest_value = -1;
    ret = TRUE;
    goto done;
  }

  /* bytes to frames */
  if (src_format == GST_FORMAT_BYTES && dest_format == GST_FORMAT_DEFAULT) {
    if (size != 0) {
      *dest_value = gst_util_uint64_scale_int (src_value, 1, size);
    } else {
      GST_ERROR ("blocksize is 0");
      *dest_value = 0;
    }
    ret = TRUE;
    goto done;
  }

  /* frames to bytes */
  if (src_format == GST_FORMAT_DEFAULT && dest_format == GST_FORMAT_BYTES) {
    *dest_value = gst_util_uint64_scale_int (src_value, size, 1);
    ret = TRUE;
    goto done;
  }

  /* time to frames */
  if (src_format == GST_FORMAT_TIME && dest_format == GST_FORMAT_DEFAULT) {
    if (fps_d != 0) {
      *dest_value = gst_util_uint64_scale (src_value,
          fps_n, GST_SECOND * fps_d);
    } else {
      GST_ERROR ("framerate denominator is 0");
      *dest_value = 0;
    }
    ret = TRUE;
    goto done;
  }

  /* frames to time */
  if (src_format == GST_FORMAT_DEFAULT && dest_format == GST_FORMAT_TIME) {
    if (fps_n != 0) {
      *dest_value = gst_util_uint64_scale (src_value,
          GST_SECOND * fps_d, fps_n);
    } else {
      GST_ERROR ("framerate numerator is 0");
      *dest_value = 0;
    }
    ret = TRUE;
    goto done;
  }

  /* time to bytes */
  if (src_format == GST_FORMAT_TIME && dest_format == GST_FORMAT_BYTES) {
    if (fps_d != 0) {
      *dest_value = gst_util_uint64_scale (src_value,
          fps_n * size, GST_SECOND * fps_d);
    } else {
      GST_ERROR ("framerate denominator is 0");
      *dest_value = 0;
    }
    ret = TRUE;
    goto done;
  }

  /* bytes to time */
  if (src_format == GST_FORMAT_BYTES && dest_format == GST_FORMAT_TIME) {
    if (fps_n != 0 && size != 0) {
      *dest_value = gst_util_uint64_scale (src_value,
          GST_SECOND * fps_d, fps_n * size);
    } else {
      GST_ERROR ("framerate denominator and/or blocksize is 0");
      *dest_value = 0;
    }
    ret = TRUE;
  }

done:

  GST_DEBUG ("ret=%d result %" G_GINT64_FORMAT, ret, *dest_value);

  return ret;
}

#define GST_VIDEO_EVENT_STILL_STATE_NAME "GstEventStillFrame"

/**
 * gst_video_event_new_still_frame:
 * @in_still: boolean value for the still-frame state of the event.
 *
 * Creates a new Still Frame event. If @in_still is %TRUE, then the event
 * represents the start of a still frame sequence. If it is %FALSE, then
 * the event ends a still frame sequence.
 *
 * To parse an event created by gst_video_event_new_still_frame() use
 * gst_video_event_parse_still_frame().
 *
 * Returns: The new GstEvent
 * Since: 0.10.26
 */
GstEvent *
gst_video_event_new_still_frame (gboolean in_still)
{
  GstEvent *still_event;
  GstStructure *s;

  s = gst_structure_new (GST_VIDEO_EVENT_STILL_STATE_NAME,
      "still-state", G_TYPE_BOOLEAN, in_still, NULL);
  still_event = gst_event_new_custom (GST_EVENT_CUSTOM_DOWNSTREAM, s);

  return still_event;
}

/**
 * gst_video_event_parse_still_frame:
 * @event: A #GstEvent to parse
 * @in_still: A boolean to receive the still-frame status from the event, or NULL
 *
 * Parse a #GstEvent, identify if it is a Still Frame event, and
 * return the still-frame state from the event if it is.
 * If the event represents the start of a still frame, the in_still
 * variable will be set to TRUE, otherwise FALSE. It is OK to pass NULL for the
 * in_still variable order to just check whether the event is a valid still-frame
 * event.
 *
 * Create a still frame event using gst_video_event_new_still_frame()
 *
 * Returns: %TRUE if the event is a valid still-frame event. %FALSE if not
 * Since: 0.10.26
 */
gboolean
gst_video_event_parse_still_frame (GstEvent * event, gboolean * in_still)
{
  const GstStructure *s;
  gboolean ev_still_state;

  g_return_val_if_fail (event != NULL, FALSE);

  if (GST_EVENT_TYPE (event) != GST_EVENT_CUSTOM_DOWNSTREAM)
    return FALSE;               /* Not a still frame event */

  s = gst_event_get_structure (event);
  if (s == NULL
      || !gst_structure_has_name (s, GST_VIDEO_EVENT_STILL_STATE_NAME))
    return FALSE;               /* Not a still frame event */
  if (!gst_structure_get_boolean (s, "still-state", &ev_still_state))
    return FALSE;               /* Not a still frame event */
  if (in_still)
    *in_still = ev_still_state;
  return TRUE;
}

/**
 * gst_video_parse_caps_palette:
 * @caps: #GstCaps to parse
 *
 * Returns the palette data from the caps as a #GstBuffer. For
 * #GST_VIDEO_FORMAT_RGB8_PALETTED this is containing 256 #guint32
 * values, each containing ARGB colors in native endianness.
 *
 * Returns: a #GstBuffer containing the palette data. Unref after usage.
 * Since: 0.10.32
 */
GstBuffer *
gst_video_parse_caps_palette (GstCaps * caps)
{
  GstStructure *s;
  const GValue *p_v;
  GstBuffer *p;

  if (!gst_caps_is_fixed (caps))
    return NULL;

  s = gst_caps_get_structure (caps, 0);

  p_v = gst_structure_get_value (s, "palette_data");
  if (!p_v || !GST_VALUE_HOLDS_BUFFER (p_v))
    return NULL;

  p = gst_buffer_ref (gst_value_get_buffer (p_v));

  return p;
}
#endif // GSTREAMER_LITE