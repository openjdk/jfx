/* GStreamer
 * Copyright (C) <2011> Wim Taymans <wim.taymans@gmail.com>
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

#ifndef __GST_VIDEO_COLOR_H__
#define __GST_VIDEO_COLOR_H__

#include <gst/gst.h>

#include <gst/video/video-format.h>

G_BEGIN_DECLS

/**
 * GstVideoColorRange:
 * @GST_VIDEO_COLOR_RANGE_UNKNOWN: unknown range
 * @GST_VIDEO_COLOR_RANGE_0_255: [0..255] for 8 bit components
 * @GST_VIDEO_COLOR_RANGE_16_235: [16..235] for 8 bit components. Chroma has
 *                 [16..240] range.
 *
 * Possible color range values. These constants are defined for 8 bit color
 * values and can be scaled for other bit depths.
 */
typedef enum {
  GST_VIDEO_COLOR_RANGE_UNKNOWN = 0,
  GST_VIDEO_COLOR_RANGE_0_255,
  GST_VIDEO_COLOR_RANGE_16_235
} GstVideoColorRange;

/**
 * GstVideoColorMatrix:
 * @GST_VIDEO_COLOR_MATRIX_UNKNOWN: unknown matrix
 * @GST_VIDEO_COLOR_MATRIX_RGB: identity matrix
 * @GST_VIDEO_COLOR_MATRIX_FCC: FCC color matrix
 * @GST_VIDEO_COLOR_MATRIX_BT709: ITU-R BT.709 color matrix
 * @GST_VIDEO_COLOR_MATRIX_BT601: ITU-R BT.601 color matrix
 * @GST_VIDEO_COLOR_MATRIX_SMPTE240M: SMPTE 240M color matrix
 * @GST_VIDEO_COLOR_MATRIX_BT2020: ITU-R BT.2020 color matrix. Since: 1.6
 *
 * The color matrix is used to convert between Y'PbPr and
 * non-linear RGB (R'G'B')
 */
typedef enum {
  GST_VIDEO_COLOR_MATRIX_UNKNOWN = 0,
  GST_VIDEO_COLOR_MATRIX_RGB,
  GST_VIDEO_COLOR_MATRIX_FCC,
  GST_VIDEO_COLOR_MATRIX_BT709,
  GST_VIDEO_COLOR_MATRIX_BT601,
  GST_VIDEO_COLOR_MATRIX_SMPTE240M,
  GST_VIDEO_COLOR_MATRIX_BT2020
} GstVideoColorMatrix;

GST_VIDEO_API
gboolean gst_video_color_matrix_get_Kr_Kb (GstVideoColorMatrix matrix, gdouble * Kr, gdouble * Kb);

/**
 * GstVideoTransferFunction:
 * @GST_VIDEO_TRANSFER_UNKNOWN: unknown transfer function
 * @GST_VIDEO_TRANSFER_GAMMA10: linear RGB, gamma 1.0 curve
 * @GST_VIDEO_TRANSFER_GAMMA18: Gamma 1.8 curve
 * @GST_VIDEO_TRANSFER_GAMMA20: Gamma 2.0 curve
 * @GST_VIDEO_TRANSFER_GAMMA22: Gamma 2.2 curve
 * @GST_VIDEO_TRANSFER_BT709: Gamma 2.2 curve with a linear segment in the lower
 *                           range
 * @GST_VIDEO_TRANSFER_SMPTE240M: Gamma 2.2 curve with a linear segment in the
 *                               lower range
 * @GST_VIDEO_TRANSFER_SRGB: Gamma 2.4 curve with a linear segment in the lower
 *                          range
 * @GST_VIDEO_TRANSFER_GAMMA28: Gamma 2.8 curve
 * @GST_VIDEO_TRANSFER_LOG100: Logarithmic transfer characteristic
 *                             100:1 range
 * @GST_VIDEO_TRANSFER_LOG316: Logarithmic transfer characteristic
 *                             316.22777:1 range
 * @GST_VIDEO_TRANSFER_BT2020_12: Gamma 2.2 curve with a linear segment in the lower
 *                                range. Used for BT.2020 with 12 bits per
 *                                component. Since: 1.6
 * @GST_VIDEO_TRANSFER_ADOBERGB: Gamma 2.19921875. Since: 1.8
 *
 * The video transfer function defines the formula for converting between
 * non-linear RGB (R'G'B') and linear RGB
 */
typedef enum {
  GST_VIDEO_TRANSFER_UNKNOWN = 0,
  GST_VIDEO_TRANSFER_GAMMA10,
  GST_VIDEO_TRANSFER_GAMMA18,
  GST_VIDEO_TRANSFER_GAMMA20,
  GST_VIDEO_TRANSFER_GAMMA22,
  GST_VIDEO_TRANSFER_BT709,
  GST_VIDEO_TRANSFER_SMPTE240M,
  GST_VIDEO_TRANSFER_SRGB,
  GST_VIDEO_TRANSFER_GAMMA28,
  GST_VIDEO_TRANSFER_LOG100,
  GST_VIDEO_TRANSFER_LOG316,
  GST_VIDEO_TRANSFER_BT2020_12,
  GST_VIDEO_TRANSFER_ADOBERGB
} GstVideoTransferFunction;

GST_VIDEO_API
gdouble      gst_video_color_transfer_encode (GstVideoTransferFunction func, gdouble val);

GST_VIDEO_API
gdouble      gst_video_color_transfer_decode (GstVideoTransferFunction func, gdouble val);

/**
 * GstVideoColorPrimaries:
 * @GST_VIDEO_COLOR_PRIMARIES_UNKNOWN: unknown color primaries
 * @GST_VIDEO_COLOR_PRIMARIES_BT709: BT709 primaries
 * @GST_VIDEO_COLOR_PRIMARIES_BT470M: BT470M primaries
 * @GST_VIDEO_COLOR_PRIMARIES_BT470BG: BT470BG primaries
 * @GST_VIDEO_COLOR_PRIMARIES_SMPTE170M: SMPTE170M primaries
 * @GST_VIDEO_COLOR_PRIMARIES_SMPTE240M: SMPTE240M primaries
 * @GST_VIDEO_COLOR_PRIMARIES_FILM: Generic film
 * @GST_VIDEO_COLOR_PRIMARIES_BT2020: BT2020 primaries. Since: 1.6
 * @GST_VIDEO_COLOR_PRIMARIES_ADOBERGB: Adobe RGB primaries. Since: 1.8
 * @GST_VIDEO_COLOR_PRIMARIES_SMPTEST428: SMPTE ST 428 primaries. Since: 1.16
 * @GST_VIDEO_COLOR_PRIMARIES_SMPTERP431: SMPTE RP 431 primaries. Since: 1.16
 * @GST_VIDEO_COLOR_PRIMARIES_SMPTEEG432: SMPTE EG 432 primaries. Since: 1.16
 * @GST_VIDEO_COLOR_PRIMARIES_EBU3213: EBU 3213 primaries. Since: 1.16
 *
 * The color primaries define the how to transform linear RGB values to and from
 * the CIE XYZ colorspace.
 */
typedef enum {
  GST_VIDEO_COLOR_PRIMARIES_UNKNOWN = 0,
  GST_VIDEO_COLOR_PRIMARIES_BT709,
  GST_VIDEO_COLOR_PRIMARIES_BT470M,
  GST_VIDEO_COLOR_PRIMARIES_BT470BG,
  GST_VIDEO_COLOR_PRIMARIES_SMPTE170M,
  GST_VIDEO_COLOR_PRIMARIES_SMPTE240M,
  GST_VIDEO_COLOR_PRIMARIES_FILM,
  GST_VIDEO_COLOR_PRIMARIES_BT2020,
  GST_VIDEO_COLOR_PRIMARIES_ADOBERGB,
  GST_VIDEO_COLOR_PRIMARIES_SMPTEST428,
  GST_VIDEO_COLOR_PRIMARIES_SMPTERP431,
  GST_VIDEO_COLOR_PRIMARIES_SMPTEEG432,
  GST_VIDEO_COLOR_PRIMARIES_EBU3213,
} GstVideoColorPrimaries;

/**
 * GstVideoColorPrimariesInfo:
 * @primaries: a #GstVideoColorPrimaries
 * @Wx: reference white x coordinate
 * @Wy: reference white y coordinate
 * @Rx: red x coordinate
 * @Ry: red y coordinate
 * @Gx: green x coordinate
 * @Gy: green y coordinate
 * @Bx: blue x coordinate
 * @By: blue y coordinate
 *
 * Structure describing the chromaticity coordinates of an RGB system. These
 * values can be used to construct a matrix to transform RGB to and from the
 * XYZ colorspace.
 *
 * Since: 1.6
 */
typedef struct {
  GstVideoColorPrimaries primaries;
  gdouble Wx, Wy;
  gdouble Rx, Ry;
  gdouble Gx, Gy;
  gdouble Bx, By;
} GstVideoColorPrimariesInfo;

GST_VIDEO_API
const GstVideoColorPrimariesInfo *
                gst_video_color_primaries_get_info     (GstVideoColorPrimaries primaries);

/**
 * GstVideoColorimetry:
 * @range: the color range. This is the valid range for the samples.
 *         It is used to convert the samples to Y'PbPr values.
 * @matrix: the color matrix. Used to convert between Y'PbPr and
 *          non-linear RGB (R'G'B')
 * @transfer: the transfer function. used to convert between R'G'B' and RGB
 * @primaries: color primaries. used to convert between R'G'B' and CIE XYZ
 *
 * Structure describing the color info.
 */
typedef struct {
  GstVideoColorRange        range;
  GstVideoColorMatrix       matrix;
  GstVideoTransferFunction  transfer;
  GstVideoColorPrimaries    primaries;
} GstVideoColorimetry;

/* predefined colorimetry */
#define GST_VIDEO_COLORIMETRY_BT601       "bt601"
#define GST_VIDEO_COLORIMETRY_BT709       "bt709"
#define GST_VIDEO_COLORIMETRY_SMPTE240M   "smpte240m"
#define GST_VIDEO_COLORIMETRY_SRGB        "sRGB"
#define GST_VIDEO_COLORIMETRY_BT2020      "bt2020"

GST_VIDEO_API
gboolean     gst_video_colorimetry_matches     (const GstVideoColorimetry *cinfo, const gchar *color);

GST_VIDEO_API
gboolean     gst_video_colorimetry_from_string (GstVideoColorimetry *cinfo, const gchar *color);

GST_VIDEO_API
gchar *      gst_video_colorimetry_to_string   (const GstVideoColorimetry *cinfo);

GST_VIDEO_API
gboolean     gst_video_colorimetry_is_equal    (const GstVideoColorimetry *cinfo, const GstVideoColorimetry *other);

/* compute offset and scale */

GST_VIDEO_API
void         gst_video_color_range_offsets     (GstVideoColorRange range,
                                                const GstVideoFormatInfo *info,
                                                gint offset[GST_VIDEO_MAX_COMPONENTS],
                                                gint scale[GST_VIDEO_MAX_COMPONENTS]);


G_END_DECLS

#endif /* __GST_VIDEO_COLOR_H__ */
