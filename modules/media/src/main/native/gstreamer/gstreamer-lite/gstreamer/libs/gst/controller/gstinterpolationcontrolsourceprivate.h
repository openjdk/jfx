/* GStreamer
 *
 * Copyright (C) 2007 Sebastian Dröge <slomo@circular-chaos.org>
 *
 * gstinterpolationcontrolsourceprivate.h: Private declarations for the
 *                                         GstInterpolationControlSource
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

#ifndef __GST_INTERPOLATION_CONTROL_SOURCE_PRIVATE_H__
#define __GST_INTERPOLATION_CONTROL_SOURCE_PRIVATE_H__

/**
 * GstInterpolateMethod:
 *
 * Function pointer structure to do user-defined interpolation methods
 */
typedef struct _GstInterpolateMethod
{
  GstControlSourceGetValue get_int;
  GstControlSourceGetValueArray get_int_value_array;
  GstControlSourceGetValue get_uint;
  GstControlSourceGetValueArray get_uint_value_array;
  GstControlSourceGetValue get_long;
  GstControlSourceGetValueArray get_long_value_array;
  GstControlSourceGetValue get_ulong;
  GstControlSourceGetValueArray get_ulong_value_array;
  GstControlSourceGetValue get_int64;
  GstControlSourceGetValueArray get_int64_value_array;
  GstControlSourceGetValue get_uint64;
  GstControlSourceGetValueArray get_uint64_value_array;
  GstControlSourceGetValue get_float;
  GstControlSourceGetValueArray get_float_value_array;
  GstControlSourceGetValue get_double;
  GstControlSourceGetValueArray get_double_value_array;
  GstControlSourceGetValue get_boolean;
  GstControlSourceGetValueArray get_boolean_value_array;
  GstControlSourceGetValue get_enum;
  GstControlSourceGetValueArray get_enum_value_array;
  GstControlSourceGetValue get_string;
  GstControlSourceGetValueArray get_string_value_array;
} GstInterpolateMethod;

/**
 * GstControlPoint:
 *
 * a internal structure for value+time and various temporary
 * values used for interpolation. This "inherits" from
 * GstTimedValue.
 */
typedef struct _GstControlPoint
{
  /* fields from GstTimedValue. DO NOT CHANGE! */
  GstClockTime timestamp;       /* timestamp of the value change */
  GValue value;                 /* the new value */

  /* internal fields */

  /* Caches for the interpolators */
  union {
    struct {
      gdouble h;
      gdouble z;
    } cubic;
  } cache;

} GstControlPoint;

struct _GstInterpolationControlSourcePrivate
{
  GType type;                   /* type of the handled property */
  GType base;                   /* base-type of the handled property */

  GValue default_value;         /* default value for the handled property */
  GValue minimum_value;         /* min value for the handled property */
  GValue maximum_value;         /* max value for the handled property */
  GstInterpolateMode interpolation_mode;
  
  GSequence *values;            /* List of GstControlPoint */
  gint nvalues;                 /* Number of control points */
  gboolean valid_cache;
};

extern GstInterpolateMethod *priv_gst_interpolation_methods[];
extern guint priv_gst_num_interpolation_methods;

#endif /* __GST_INTERPOLATION_CONTROL_SOURCE_PRIVATE_H__ */

