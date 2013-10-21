/* GStreamer
 *
 * Copyright (C) 2007 Sebastian Dröge <slomo@circular-chaos.org>
 *
 * gstlfocontrolsourceprivate.h: Private declarations for the
 *                                         GstLFOControlSource
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

#ifndef __GST_LFO_CONTROL_SOURCE_PRIVATE_H__
#define __GST_LFO_CONTROL_SOURCE_PRIVATE_H__

typedef struct _GstWaveformImplementation
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
} GstWaveformImplementation;

struct _GstLFOControlSourcePrivate
{
  GType type;                   /* type of the handled property */
  GType base;                   /* base-type of the handled property */

  GValue minimum_value;         /* min value for the handled property */
  GValue maximum_value;         /* max value for the handled property */

  GstLFOWaveform waveform;
  gdouble frequency;
  GstClockTime period;
  GstClockTime timeshift;
  GValue amplitude;
  GValue offset; 
};

#endif /* __GST_LFO_CONTROL_SOURCE_PRIVATE_H__ */

