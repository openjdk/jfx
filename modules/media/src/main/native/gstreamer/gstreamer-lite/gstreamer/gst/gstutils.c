/* GStreamer
 * Copyright (C) 1999,2000 Erik Walthinsen <omega@cse.ogi.edu>
 *                    2000 Wim Taymans <wtay@chello.be>
 *                    2002 Thomas Vander Stichele <thomas@apestaart.org>
 *
 * gstutils.c: Utility functions
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

/**
 * SECTION:gstutils
 * @short_description: Various utility functions
 *
 * When defining own plugins, use the GST_BOILERPLATE ease gobject creation.
 */

#include "gst_private.h"
#include <stdio.h>
#include <string.h>

#include "gstghostpad.h"
#include "gstutils.h"
#include "gsterror.h"
#include "gstinfo.h"
#include "gstparse.h"
#include "gstvalue.h"
#include "gst-i18n-lib.h"
#include "glib-compat-private.h"
#include <math.h>

/**
 * gst_util_dump_mem:
 * @mem: a pointer to the memory to dump
 * @size: the size of the memory block to dump
 *
 * Dumps the memory block into a hex representation. Useful for debugging.
 */
void
gst_util_dump_mem (const guchar * mem, guint size)
{
  guint i, j;
  GString *string = g_string_sized_new (50);
  GString *chars = g_string_sized_new (18);

  i = j = 0;
  while (i < size) {
    if (g_ascii_isprint (mem[i]))
      g_string_append_c (chars, mem[i]);
    else
      g_string_append_c (chars, '.');

    g_string_append_printf (string, "%02x ", mem[i]);

    j++;
    i++;

    if (j == 16 || i == size) {
      g_print ("%08x (%p): %-48.48s %-16.16s\n", i - j, mem + i - j,
          string->str, chars->str);
      g_string_set_size (string, 0);
      g_string_set_size (chars, 0);
      j = 0;
    }
  }
  g_string_free (string, TRUE);
  g_string_free (chars, TRUE);
}


/**
 * gst_util_set_value_from_string:
 * @value: (out caller-allocates): the value to set
 * @value_str: the string to get the value from
 *
 * Converts the string to the type of the value and
 * sets the value with it.
 *
 * Note that this function is dangerous as it does not return any indication
 * if the conversion worked or not.
 */
void
gst_util_set_value_from_string (GValue * value, const gchar * value_str)
{
  gboolean res;

  g_return_if_fail (value != NULL);
  g_return_if_fail (value_str != NULL);

  GST_CAT_DEBUG (GST_CAT_PARAMS, "parsing '%s' to type %s", value_str,
      g_type_name (G_VALUE_TYPE (value)));

  res = gst_value_deserialize (value, value_str);
  if (!res && G_VALUE_TYPE (value) == G_TYPE_BOOLEAN) {
    /* backwards compat, all booleans that fail to parse are false */
    g_value_set_boolean (value, FALSE);
    res = TRUE;
  }
  g_return_if_fail (res);
}

/**
 * gst_util_set_object_arg:
 * @object: the object to set the argument of
 * @name: the name of the argument to set
 * @value: the string value to set
 *
 * Convertes the string value to the type of the objects argument and
 * sets the argument with it.
 *
 * Note that this function silently returns if @object has no property named
 * @name or when @value cannot be converted to the type of the property.
 */
void
gst_util_set_object_arg (GObject * object, const gchar * name,
    const gchar * value)
{
  GParamSpec *pspec;
  GType value_type;
  GValue v = { 0, };

  g_return_if_fail (G_IS_OBJECT (object));
  g_return_if_fail (name != NULL);
  g_return_if_fail (value != NULL);

  pspec = g_object_class_find_property (G_OBJECT_GET_CLASS (object), name);
  if (!pspec)
    return;

  value_type = pspec->value_type;

  GST_DEBUG ("pspec->flags is %d, pspec->value_type is %s",
      pspec->flags, g_type_name (value_type));

  if (!(pspec->flags & G_PARAM_WRITABLE))
    return;

  g_value_init (&v, value_type);

  /* special case for element <-> xml (de)serialisation */
  if (GST_VALUE_HOLDS_STRUCTURE (&v) && strcmp (value, "NULL") == 0) {
    g_value_set_boxed (&v, NULL);
    goto done;
  }

  if (!gst_value_deserialize (&v, value))
    return;

done:

  g_object_set_property (object, pspec->name, &v);
  g_value_unset (&v);
}

/* work around error C2520: conversion from unsigned __int64 to double
 * not implemented, use signed __int64
 *
 * These are implemented as functions because on some platforms a 64bit int to
 * double conversion is not defined/implemented.
 */

gdouble
gst_util_guint64_to_gdouble (guint64 value)
{
  if (value & G_GINT64_CONSTANT (0x8000000000000000))
    return (gdouble) ((gint64) value) + (gdouble) 18446744073709551616.;
  else
    return (gdouble) ((gint64) value);
}

guint64
gst_util_gdouble_to_guint64 (gdouble value)
{
  if (value < (gdouble) 9223372036854775808.)   /* 1 << 63 */
    return ((guint64) ((gint64) value));

  value -= (gdouble) 18446744073709551616.;
  return ((guint64) ((gint64) value));
}

#ifndef HAVE_UINT128_T
/* convenience struct for getting high and low uint32 parts of
 * a guint64 */
typedef union
{
  guint64 ll;
  struct
  {
#if G_BYTE_ORDER == G_BIG_ENDIAN
    guint32 high, low;
#else
    guint32 low, high;
#endif
  } l;
} GstUInt64;

#if defined (__x86_64__) && defined (__GNUC__)
static inline void
gst_util_uint64_mul_uint64 (GstUInt64 * c1, GstUInt64 * c0, guint64 arg1,
    guint64 arg2)
{
  __asm__ __volatile__ ("mulq %3":"=a" (c0->ll), "=d" (c1->ll)
      :"a" (arg1), "g" (arg2)
      );
}
#else /* defined (__x86_64__) */
/* multiply two 64-bit unsigned ints into a 128-bit unsigned int.  the high
 * and low 64 bits of the product are placed in c1 and c0 respectively.
 * this operation cannot overflow. */
static inline void
gst_util_uint64_mul_uint64 (GstUInt64 * c1, GstUInt64 * c0, guint64 arg1,
    guint64 arg2)
{
  GstUInt64 a1, b0;
  GstUInt64 v, n;

  /* prepare input */
  v.ll = arg1;
  n.ll = arg2;

  /* do 128 bits multiply
   *                   nh   nl
   *                *  vh   vl
   *                ----------
   * a0 =              vl * nl
   * a1 =         vl * nh
   * b0 =         vh * nl
   * b1 =  + vh * nh
   *       -------------------
   *        c1h  c1l  c0h  c0l
   *
   * "a0" is optimized away, result is stored directly in c0.  "b1" is
   * optimized away, result is stored directly in c1.
   */
  c0->ll = (guint64) v.l.low * n.l.low;
  a1.ll = (guint64) v.l.low * n.l.high;
  b0.ll = (guint64) v.l.high * n.l.low;

  /* add the high word of a0 to the low words of a1 and b0 using c1 as
   * scrach space to capture the carry.  the low word of the result becomes
   * the final high word of c0 */
  c1->ll = (guint64) c0->l.high + a1.l.low + b0.l.low;
  c0->l.high = c1->l.low;

  /* add the carry from the result above (found in the high word of c1) and
   * the high words of a1 and b0 to b1, the result is c1. */
  c1->ll = (guint64) v.l.high * n.l.high + c1->l.high + a1.l.high + b0.l.high;
}
#endif /* defined (__x86_64__) */

#if defined (__x86_64__) && defined (__GNUC__)
static inline guint64
gst_util_div128_64 (GstUInt64 c1, GstUInt64 c0, guint64 denom)
{
  guint64 res;

  __asm__ __volatile__ ("divq %3":"=a" (res)
      :"d" (c1.ll), "a" (c0.ll), "g" (denom)
      );

  return res;
}
#else
/* count leading zeros */
static inline guint
gst_util_clz (guint32 val)
{
  guint s;

  s = val | (val >> 1);
  s |= (s >> 2);
  s |= (s >> 4);
  s |= (s >> 8);
  s = ~(s | (s >> 16));
  s = s - ((s >> 1) & 0x55555555);
  s = (s & 0x33333333) + ((s >> 2) & 0x33333333);
  s = (s + (s >> 4)) & 0x0f0f0f0f;
  s += (s >> 8);
  s = (s + (s >> 16)) & 0x3f;

  return s;
}

/* based on Hacker's Delight p152 */
static inline guint64
gst_util_div128_64 (GstUInt64 c1, GstUInt64 c0, guint64 denom)
{
  GstUInt64 q1, q0, rhat;
  GstUInt64 v, cmp1, cmp2;
  guint s;

  v.ll = denom;

  /* count number of leading zeroes, we know they must be in the high
   * part of denom since denom > G_MAXUINT32. */
  s = gst_util_clz (v.l.high);

  if (s > 0) {
    /* normalize divisor and dividend */
    v.ll <<= s;
    c1.ll = (c1.ll << s) | (c0.l.high >> (32 - s));
    c0.ll <<= s;
  }

  q1.ll = c1.ll / v.l.high;
  rhat.ll = c1.ll - q1.ll * v.l.high;

  cmp1.l.high = rhat.l.low;
  cmp1.l.low = c0.l.high;
  cmp2.ll = q1.ll * v.l.low;

  while (q1.l.high || cmp2.ll > cmp1.ll) {
    q1.ll--;
    rhat.ll += v.l.high;
    if (rhat.l.high)
      break;
    cmp1.l.high = rhat.l.low;
    cmp2.ll -= v.l.low;
  }
  c1.l.high = c1.l.low;
  c1.l.low = c0.l.high;
  c1.ll -= q1.ll * v.ll;
  q0.ll = c1.ll / v.l.high;
  rhat.ll = c1.ll - q0.ll * v.l.high;

  cmp1.l.high = rhat.l.low;
  cmp1.l.low = c0.l.low;
  cmp2.ll = q0.ll * v.l.low;

  while (q0.l.high || cmp2.ll > cmp1.ll) {
    q0.ll--;
    rhat.ll += v.l.high;
    if (rhat.l.high)
      break;
    cmp1.l.high = rhat.l.low;
    cmp2.ll -= v.l.low;
  }
  q0.l.high += q1.l.low;

  return q0.ll;
}
#endif /* defined (__GNUC__) */

/* This always gives the correct result because:
 * a) val <= G_MAXUINT64-1
 * b) (c0,c1) <= G_MAXUINT64 * (G_MAXUINT64-1)
 *    or
 *    (c0,c1) == G_MAXUINT64 * G_MAXUINT64 and denom < G_MAXUINT64
 *    (note: num==denom case is handled by short path)
 * This means that (c0,c1) either has enough space for val
 * or that the overall result will overflow anyway.
 */

/* add correction with carry */
#define CORRECT(c0,c1,val)                    \
  if (val) {                                  \
    if (G_MAXUINT64 - c0.ll < val) {          \
      if (G_UNLIKELY (c1.ll == G_MAXUINT64))  \
        /* overflow */                        \
        return G_MAXUINT64;                   \
      c1.ll++;                                \
    }                                         \
    c0.ll += val;                             \
  }

static guint64
gst_util_uint64_scale_uint64_unchecked (guint64 val, guint64 num,
    guint64 denom, guint64 correct)
{
  GstUInt64 c1, c0;

  /* compute 128-bit numerator product */
  gst_util_uint64_mul_uint64 (&c1, &c0, val, num);

  /* perform rounding correction */
  CORRECT (c0, c1, correct);

  /* high word as big as or bigger than denom --> overflow */
  if (G_UNLIKELY (c1.ll >= denom))
    return G_MAXUINT64;

  /* compute quotient, fits in 64 bits */
  return gst_util_div128_64 (c1, c0, denom);
}
#else

#define GST_MAXUINT128 ((__uint128_t) -1)
static guint64
gst_util_uint64_scale_uint64_unchecked (guint64 val, guint64 num,
    guint64 denom, guint64 correct)
{
  __uint128_t tmp;

  /* Calculate val * num */
  tmp = ((__uint128_t) val) * ((__uint128_t) num);

  /* overflow checks */
  if (G_UNLIKELY (GST_MAXUINT128 - correct < tmp))
    return G_MAXUINT64;

  /* perform rounding correction */
  tmp += correct;

  /* Divide by denom */
  tmp /= denom;

  /* if larger than G_MAXUINT64 --> overflow */
  if (G_UNLIKELY (tmp > G_MAXUINT64))
    return G_MAXUINT64;

  /* compute quotient, fits in 64 bits */
  return (guint64) tmp;
}

#endif

#if !defined (__x86_64__) && !defined (HAVE_UINT128_T)
static inline void
gst_util_uint64_mul_uint32 (GstUInt64 * c1, GstUInt64 * c0, guint64 arg1,
    guint32 arg2)
{
  GstUInt64 a;

  a.ll = arg1;

  c0->ll = (guint64) a.l.low * arg2;
  c1->ll = (guint64) a.l.high * arg2 + c0->l.high;
  c0->l.high = 0;
}

/* divide a 96-bit unsigned int by a 32-bit unsigned int when we know the
 * quotient fits into 64 bits.  the high 64 bits and low 32 bits of the
 * numerator are expected in c1 and c0 respectively. */
static inline guint64
gst_util_div96_32 (guint64 c1, guint64 c0, guint32 denom)
{
  c0 += (c1 % denom) << 32;
  return ((c1 / denom) << 32) + (c0 / denom);
}

static inline guint64
gst_util_uint64_scale_uint32_unchecked (guint64 val, guint32 num,
    guint32 denom, guint32 correct)
{
  GstUInt64 c1, c0;

  /* compute 96-bit numerator product */
  gst_util_uint64_mul_uint32 (&c1, &c0, val, num);

  /* condition numerator based on rounding mode */
  CORRECT (c0, c1, correct);

  /* high 32 bits as big as or bigger than denom --> overflow */
  if (G_UNLIKELY (c1.l.high >= denom))
    return G_MAXUINT64;

  /* compute quotient, fits in 64 bits */
  return gst_util_div96_32 (c1.ll, c0.ll, denom);
}
#endif

/* the guts of the gst_util_uint64_scale() variants */
static guint64
_gst_util_uint64_scale (guint64 val, guint64 num, guint64 denom,
    guint64 correct)
{
  g_return_val_if_fail (denom != 0, G_MAXUINT64);

  if (G_UNLIKELY (num == 0))
    return 0;

  if (G_UNLIKELY (num == denom))
    return val;

  /* on 64bits we always use a full 128bits multipy/division */
#if !defined (__x86_64__) && !defined (HAVE_UINT128_T)
  /* denom is low --> try to use 96 bit muldiv */
  if (G_LIKELY (denom <= G_MAXUINT32)) {
    /* num is low --> use 96 bit muldiv */
    if (G_LIKELY (num <= G_MAXUINT32))
      return gst_util_uint64_scale_uint32_unchecked (val, (guint32) num,
          (guint32) denom, correct);

    /* num is high but val is low --> swap and use 96-bit muldiv */
    if (G_LIKELY (val <= G_MAXUINT32))
      return gst_util_uint64_scale_uint32_unchecked (num, (guint32) val,
          (guint32) denom, correct);
  }
#endif /* !defined (__x86_64__) && !defined (HAVE_UINT128_T) */

  /* val is high and num is high --> use 128-bit muldiv */
  return gst_util_uint64_scale_uint64_unchecked (val, num, denom, correct);
}

/**
 * gst_util_uint64_scale:
 * @val: the number to scale
 * @num: the numerator of the scale ratio
 * @denom: the denominator of the scale ratio
 *
 * Scale @val by the rational number @num / @denom, avoiding overflows and
 * underflows and without loss of precision.
 *
 * This function can potentially be very slow if val and num are both
 * greater than G_MAXUINT32.
 *
 * Returns: @val * @num / @denom.  In the case of an overflow, this
 * function returns G_MAXUINT64.  If the result is not exactly
 * representable as an integer it is truncated.  See also
 * gst_util_uint64_scale_round(), gst_util_uint64_scale_ceil(),
 * gst_util_uint64_scale_int(), gst_util_uint64_scale_int_round(),
 * gst_util_uint64_scale_int_ceil().
 */
guint64
gst_util_uint64_scale (guint64 val, guint64 num, guint64 denom)
{
  return _gst_util_uint64_scale (val, num, denom, 0);
}

/**
 * gst_util_uint64_scale_round:
 * @val: the number to scale
 * @num: the numerator of the scale ratio
 * @denom: the denominator of the scale ratio
 *
 * Scale @val by the rational number @num / @denom, avoiding overflows and
 * underflows and without loss of precision.
 *
 * This function can potentially be very slow if val and num are both
 * greater than G_MAXUINT32.
 *
 * Returns: @val * @num / @denom.  In the case of an overflow, this
 * function returns G_MAXUINT64.  If the result is not exactly
 * representable as an integer, it is rounded to the nearest integer
 * (half-way cases are rounded up).  See also gst_util_uint64_scale(),
 * gst_util_uint64_scale_ceil(), gst_util_uint64_scale_int(),
 * gst_util_uint64_scale_int_round(), gst_util_uint64_scale_int_ceil().
 */
guint64
gst_util_uint64_scale_round (guint64 val, guint64 num, guint64 denom)
{
  return _gst_util_uint64_scale (val, num, denom, denom >> 1);
}

/**
 * gst_util_uint64_scale_ceil:
 * @val: the number to scale
 * @num: the numerator of the scale ratio
 * @denom: the denominator of the scale ratio
 *
 * Scale @val by the rational number @num / @denom, avoiding overflows and
 * underflows and without loss of precision.
 *
 * This function can potentially be very slow if val and num are both
 * greater than G_MAXUINT32.
 *
 * Returns: @val * @num / @denom.  In the case of an overflow, this
 * function returns G_MAXUINT64.  If the result is not exactly
 * representable as an integer, it is rounded up.  See also
 * gst_util_uint64_scale(), gst_util_uint64_scale_round(),
 * gst_util_uint64_scale_int(), gst_util_uint64_scale_int_round(),
 * gst_util_uint64_scale_int_ceil().
 */
guint64
gst_util_uint64_scale_ceil (guint64 val, guint64 num, guint64 denom)
{
  return _gst_util_uint64_scale (val, num, denom, denom - 1);
}

/* the guts of the gst_util_uint64_scale_int() variants */
static guint64
_gst_util_uint64_scale_int (guint64 val, gint num, gint denom, gint correct)
{
  g_return_val_if_fail (denom > 0, G_MAXUINT64);
  g_return_val_if_fail (num >= 0, G_MAXUINT64);

  if (G_UNLIKELY (num == 0))
    return 0;

  if (G_UNLIKELY (num == denom))
    return val;

  if (val <= G_MAXUINT32) {
    /* simple case.  num and denom are not negative so casts are OK.  when
     * not truncating, the additions to the numerator cannot overflow
     * because val*num <= G_MAXUINT32 * G_MAXINT32 < G_MAXUINT64 -
     * G_MAXINT32, so there's room to add another gint32. */
    val *= (guint64) num;
    /* add rounding correction */
    val += correct;

    return val / (guint64) denom;
  }
#if !defined (__x86_64__) && !defined (HAVE_UINT128_T)
  /* num and denom are not negative so casts are OK */
  return gst_util_uint64_scale_uint32_unchecked (val, (guint32) num,
      (guint32) denom, (guint32) correct);
#else
  /* always use full 128bits scale */
  return gst_util_uint64_scale_uint64_unchecked (val, num, denom, correct);
#endif
}

/**
 * gst_util_uint64_scale_int:
 * @val: guint64 (such as a #GstClockTime) to scale.
 * @num: numerator of the scale factor.
 * @denom: denominator of the scale factor.
 *
 * Scale @val by the rational number @num / @denom, avoiding overflows and
 * underflows and without loss of precision.  @num must be non-negative and
 * @denom must be positive.
 *
 * Returns: @val * @num / @denom.  In the case of an overflow, this
 * function returns G_MAXUINT64.  If the result is not exactly
 * representable as an integer, it is truncated.  See also
 * gst_util_uint64_scale_int_round(), gst_util_uint64_scale_int_ceil(),
 * gst_util_uint64_scale(), gst_util_uint64_scale_round(),
 * gst_util_uint64_scale_ceil().
 */
guint64
gst_util_uint64_scale_int (guint64 val, gint num, gint denom)
{
  return _gst_util_uint64_scale_int (val, num, denom, 0);
}

/**
 * gst_util_uint64_scale_int_round:
 * @val: guint64 (such as a #GstClockTime) to scale.
 * @num: numerator of the scale factor.
 * @denom: denominator of the scale factor.
 *
 * Scale @val by the rational number @num / @denom, avoiding overflows and
 * underflows and without loss of precision.  @num must be non-negative and
 * @denom must be positive.
 *
 * Returns: @val * @num / @denom.  In the case of an overflow, this
 * function returns G_MAXUINT64.  If the result is not exactly
 * representable as an integer, it is rounded to the nearest integer
 * (half-way cases are rounded up).  See also gst_util_uint64_scale_int(),
 * gst_util_uint64_scale_int_ceil(), gst_util_uint64_scale(),
 * gst_util_uint64_scale_round(), gst_util_uint64_scale_ceil().
 */
guint64
gst_util_uint64_scale_int_round (guint64 val, gint num, gint denom)
{
  /* we can use a shift to divide by 2 because denom is required to be
   * positive. */
  return _gst_util_uint64_scale_int (val, num, denom, denom >> 1);
}

/**
 * gst_util_uint64_scale_int_ceil:
 * @val: guint64 (such as a #GstClockTime) to scale.
 * @num: numerator of the scale factor.
 * @denom: denominator of the scale factor.
 *
 * Scale @val by the rational number @num / @denom, avoiding overflows and
 * underflows and without loss of precision.  @num must be non-negative and
 * @denom must be positive.
 *
 * Returns: @val * @num / @denom.  In the case of an overflow, this
 * function returns G_MAXUINT64.  If the result is not exactly
 * representable as an integer, it is rounded up.  See also
 * gst_util_uint64_scale_int(), gst_util_uint64_scale_int_round(),
 * gst_util_uint64_scale(), gst_util_uint64_scale_round(),
 * gst_util_uint64_scale_ceil().
 */
guint64
gst_util_uint64_scale_int_ceil (guint64 val, gint num, gint denom)
{
  return _gst_util_uint64_scale_int (val, num, denom, denom - 1);
}

/**
 * gst_util_seqnum_next:
 *
 * Return a constantly incrementing sequence number.
 *
 * This function is used internally to GStreamer to be able to determine which
 * events and messages are "the same". For example, elements may set the seqnum
 * on a segment-done message to be the same as that of the last seek event, to
 * indicate that event and the message correspond to the same segment.
 *
 * Returns: A constantly incrementing 32-bit unsigned integer, which might
 * overflow back to 0 at some point. Use gst_util_seqnum_compare() to make sure
 * you handle wraparound correctly.
 *
 * Since: 0.10.22
 */
guint32
gst_util_seqnum_next (void)
{
  static gint counter = 0;
  return G_ATOMIC_INT_ADD (&counter, 1);
}

/**
 * gst_util_seqnum_compare:
 * @s1: A sequence number.
 * @s2: Another sequence number.
 *
 * Compare two sequence numbers, handling wraparound.
 *
 * The current implementation just returns (gint32)(@s1 - @s2).
 *
 * Returns: A negative number if @s1 is before @s2, 0 if they are equal, or a
 * positive number if @s1 is after @s2.
 *
 * Since: 0.10.22
 */
gint32
gst_util_seqnum_compare (guint32 s1, guint32 s2)
{
  return (gint32) (s1 - s2);
}

/* -----------------------------------------------------
 *
 *  The following code will be moved out of the main
 * gstreamer library someday.
 */

#include "gstpad.h"

static void
string_append_indent (GString * str, gint count)
{
  gint xx;

  for (xx = 0; xx < count; xx++)
    g_string_append_c (str, ' ');
}

#ifndef GSTREAMER_LITE
/**
 * gst_print_pad_caps:
 * @buf: the buffer to print the caps in
 * @indent: initial indentation
 * @pad: (transfer none): the pad to print the caps from
 *
 * Write the pad capabilities in a human readable format into
 * the given GString.
 */
void
gst_print_pad_caps (GString * buf, gint indent, GstPad * pad)
{
  GstCaps *caps;

  caps = pad->caps;

  if (!caps) {
    string_append_indent (buf, indent);
    g_string_printf (buf, "%s:%s has no capabilities",
        GST_DEBUG_PAD_NAME (pad));
  } else {
    char *s;

    s = gst_caps_to_string (caps);
    g_string_append (buf, s);
    g_free (s);
  }
}
#endif // GSTREAMER_LITE

/**
 * gst_print_element_args:
 * @buf: the buffer to print the args in
 * @indent: initial indentation
 * @element: (transfer none): the element to print the args of
 *
 * Print the element argument in a human readable format in the given
 * GString.
 */
void
gst_print_element_args (GString * buf, gint indent, GstElement * element)
{
  guint width;
  GValue value = { 0, };        /* the important thing is that value.type = 0 */
  gchar *str = NULL;
  GParamSpec *spec, **specs, **walk;

  specs = g_object_class_list_properties (G_OBJECT_GET_CLASS (element), NULL);

  width = 0;
  for (walk = specs; *walk; walk++) {
    spec = *walk;
    if (width < strlen (spec->name))
      width = strlen (spec->name);
  }

  for (walk = specs; *walk; walk++) {
    spec = *walk;

    if (spec->flags & G_PARAM_READABLE) {
      g_value_init (&value, spec->value_type);
      g_object_get_property (G_OBJECT (element), spec->name, &value);
      str = g_strdup_value_contents (&value);
      g_value_unset (&value);
    } else {
      str = g_strdup ("Parameter not readable.");
    }

    string_append_indent (buf, indent);
    g_string_append (buf, spec->name);
    string_append_indent (buf, 2 + width - strlen (spec->name));
    g_string_append (buf, str);
    g_string_append_c (buf, '\n');

    g_free (str);
  }

  g_free (specs);
}

/**
 * gst_element_create_all_pads:
 * @element: (transfer none): a #GstElement to create pads for
 *
 * Creates a pad for each pad template that is always available.
 * This function is only useful during object intialization of
 * subclasses of #GstElement.
 */
void
gst_element_create_all_pads (GstElement * element)
{
  GList *padlist;

  /* FIXME: lock element */

  padlist =
      gst_element_class_get_pad_template_list (GST_ELEMENT_CLASS
      (G_OBJECT_GET_CLASS (element)));

  while (padlist) {
    GstPadTemplate *padtempl = (GstPadTemplate *) padlist->data;

    if (padtempl->presence == GST_PAD_ALWAYS) {
      GstPad *pad;

      pad = gst_pad_new_from_template (padtempl, padtempl->name_template);

      gst_element_add_pad (element, pad);
    }
    padlist = padlist->next;
  }
}

/**
 * gst_element_get_compatible_pad_template:
 * @element: (transfer none): a #GstElement to get a compatible pad template for
 * @compattempl: (transfer none): the #GstPadTemplate to find a compatible
 *     template for
 *
 * Retrieves a pad template from @element that is compatible with @compattempl.
 * Pads from compatible templates can be linked together.
 *
 * Returns: (transfer none): a compatible #GstPadTemplate, or NULL if none
 *     was found. No unreferencing is necessary.
 */
GstPadTemplate *
gst_element_get_compatible_pad_template (GstElement * element,
    GstPadTemplate * compattempl)
{
  GstPadTemplate *newtempl = NULL;
  GList *padlist;
  GstElementClass *class;
  gboolean compatible;

  g_return_val_if_fail (element != NULL, NULL);
  g_return_val_if_fail (GST_IS_ELEMENT (element), NULL);
  g_return_val_if_fail (compattempl != NULL, NULL);

  class = GST_ELEMENT_GET_CLASS (element);

  padlist = gst_element_class_get_pad_template_list (class);

  GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS,
      "Looking for a suitable pad template in %s out of %d templates...",
      GST_ELEMENT_NAME (element), g_list_length (padlist));

  while (padlist) {
    GstPadTemplate *padtempl = (GstPadTemplate *) padlist->data;

    /* Ignore name
     * Ignore presence
     * Check direction (must be opposite)
     * Check caps
     */
    GST_CAT_LOG (GST_CAT_CAPS,
        "checking pad template %s", padtempl->name_template);
    if (padtempl->direction != compattempl->direction) {
      GST_CAT_DEBUG (GST_CAT_CAPS,
          "compatible direction: found %s pad template \"%s\"",
          padtempl->direction == GST_PAD_SRC ? "src" : "sink",
          padtempl->name_template);

      GST_CAT_DEBUG (GST_CAT_CAPS,
          "intersecting %" GST_PTR_FORMAT, GST_PAD_TEMPLATE_CAPS (compattempl));
      GST_CAT_DEBUG (GST_CAT_CAPS,
          "..and %" GST_PTR_FORMAT, GST_PAD_TEMPLATE_CAPS (padtempl));

      compatible = gst_caps_can_intersect (GST_PAD_TEMPLATE_CAPS (compattempl),
          GST_PAD_TEMPLATE_CAPS (padtempl));

      GST_CAT_DEBUG (GST_CAT_CAPS, "caps are %scompatible",
          (compatible ? "" : "not "));

      if (compatible) {
        newtempl = padtempl;
        break;
      }
    }

    padlist = g_list_next (padlist);
  }
  if (newtempl)
    GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS,
        "Returning new pad template %p", newtempl);
  else
    GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS, "No compatible pad template found");

  return newtempl;
}

/**
 * gst_element_get_pad_from_template:
 * @element: (transfer none): a #GstElement.
 * @templ: (transfer none): a #GstPadTemplate belonging to @element.
 *
 * Gets a pad from @element described by @templ. If the presence of @templ is
 * #GST_PAD_REQUEST, requests a new pad. Can return %NULL for #GST_PAD_SOMETIMES
 * templates.
 *
 * Returns: (transfer full): the #GstPad, or NULL if one could not be found
 *     or created.
 */
static GstPad *
gst_element_get_pad_from_template (GstElement * element, GstPadTemplate * templ)
{
  GstPad *ret = NULL;
  GstPadPresence presence;

  /* If this function is ever exported, we need check the validity of `element'
   * and `templ', and to make sure the template actually belongs to the
   * element. */

  presence = GST_PAD_TEMPLATE_PRESENCE (templ);

  switch (presence) {
    case GST_PAD_ALWAYS:
    case GST_PAD_SOMETIMES:
      ret = gst_element_get_static_pad (element, templ->name_template);
      if (!ret && presence == GST_PAD_ALWAYS)
        g_warning
            ("Element %s has an ALWAYS template %s, but no pad of the same name",
            GST_OBJECT_NAME (element), templ->name_template);
      break;

    case GST_PAD_REQUEST:
      ret = gst_element_request_pad (element, templ, NULL, NULL);
      break;
  }

  return ret;
}

/*
 * gst_element_request_compatible_pad:
 * @element: a #GstElement.
 * @templ: the #GstPadTemplate to which the new pad should be able to link.
 *
 * Requests a pad from @element. The returned pad should be unlinked and
 * compatible with @templ. Might return an existing pad, or request a new one.
 *
 * Returns: a #GstPad, or %NULL if one could not be found or created.
 */
static GstPad *
gst_element_request_compatible_pad (GstElement * element,
    GstPadTemplate * templ)
{
  GstPadTemplate *templ_new;
  GstPad *pad = NULL;

  g_return_val_if_fail (GST_IS_ELEMENT (element), NULL);
  g_return_val_if_fail (GST_IS_PAD_TEMPLATE (templ), NULL);

  /* FIXME: should really loop through the templates, testing each for
   *      compatibility and pad availability. */
  templ_new = gst_element_get_compatible_pad_template (element, templ);
  if (templ_new)
    pad = gst_element_get_pad_from_template (element, templ_new);

  /* This can happen for non-request pads. No need to unref. */
  if (pad && GST_PAD_PEER (pad))
    pad = NULL;

  return pad;
}

/*
 * Checks if the source pad and the sink pad can be linked.
 * Both @srcpad and @sinkpad must be unlinked and have a parent.
 */
static gboolean
gst_pad_check_link (GstPad * srcpad, GstPad * sinkpad)
{
  /* FIXME This function is gross.  It's almost a direct copy of
   * gst_pad_link_filtered().  Any decent programmer would attempt
   * to merge the two functions, which I will do some day. --ds
   */

  /* generic checks */
  g_return_val_if_fail (GST_IS_PAD (srcpad), FALSE);
  g_return_val_if_fail (GST_IS_PAD (sinkpad), FALSE);

  GST_CAT_INFO (GST_CAT_PADS, "trying to link %s:%s and %s:%s",
      GST_DEBUG_PAD_NAME (srcpad), GST_DEBUG_PAD_NAME (sinkpad));

  /* FIXME: shouldn't we convert this to g_return_val_if_fail? */
  if (GST_PAD_PEER (srcpad) != NULL) {
    GST_CAT_INFO (GST_CAT_PADS, "Source pad %s:%s has a peer, failed",
        GST_DEBUG_PAD_NAME (srcpad));
    return FALSE;
  }
  if (GST_PAD_PEER (sinkpad) != NULL) {
    GST_CAT_INFO (GST_CAT_PADS, "Sink pad %s:%s has a peer, failed",
        GST_DEBUG_PAD_NAME (sinkpad));
    return FALSE;
  }
  if (!GST_PAD_IS_SRC (srcpad)) {
    GST_CAT_INFO (GST_CAT_PADS, "Src pad %s:%s is not source pad, failed",
        GST_DEBUG_PAD_NAME (srcpad));
    return FALSE;
  }
  if (!GST_PAD_IS_SINK (sinkpad)) {
    GST_CAT_INFO (GST_CAT_PADS, "Sink pad %s:%s is not sink pad, failed",
        GST_DEBUG_PAD_NAME (sinkpad));
    return FALSE;
  }
  if (GST_PAD_PARENT (srcpad) == NULL) {
    GST_CAT_INFO (GST_CAT_PADS, "Src pad %s:%s has no parent, failed",
        GST_DEBUG_PAD_NAME (srcpad));
    return FALSE;
  }
  if (GST_PAD_PARENT (sinkpad) == NULL) {
    GST_CAT_INFO (GST_CAT_PADS, "Sink pad %s:%s has no parent, failed",
        GST_DEBUG_PAD_NAME (srcpad));
    return FALSE;
  }

  return TRUE;
}

/**
 * gst_element_get_compatible_pad:
 * @element: (transfer none): a #GstElement in which the pad should be found.
 * @pad: (transfer none): the #GstPad to find a compatible one for.
 * @caps: the #GstCaps to use as a filter.
 *
 * Looks for an unlinked pad to which the given pad can link. It is not
 * guaranteed that linking the pads will work, though it should work in most
 * cases.
 *
 * This function will first attempt to find a compatible unlinked ALWAYS pad,
 * and if none can be found, it will request a compatible REQUEST pad by looking
 * at the templates of @element.
 *
 * Returns: (transfer full): the #GstPad to which a link can be made, or %NULL
 *     if one cannot be found. gst_object_unref() after usage.
 */
GstPad *
gst_element_get_compatible_pad (GstElement * element, GstPad * pad,
    const GstCaps * caps)
{
  GstIterator *pads;
  GstPadTemplate *templ;
  GstCaps *templcaps;
  GstPad *foundpad = NULL;
  gboolean done;

  g_return_val_if_fail (GST_IS_ELEMENT (element), NULL);
  g_return_val_if_fail (GST_IS_PAD (pad), NULL);

  GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS,
      "finding pad in %s compatible with %s:%s",
      GST_ELEMENT_NAME (element), GST_DEBUG_PAD_NAME (pad));

  g_return_val_if_fail (GST_PAD_PEER (pad) == NULL, NULL);

  done = FALSE;

  /* try to get an existing unlinked pad */
  if (GST_PAD_IS_SRC (pad)) {
    pads = gst_element_iterate_sink_pads (element);
  } else if (GST_PAD_IS_SINK (pad)) {
    pads = gst_element_iterate_src_pads (element);
  } else {
    pads = gst_element_iterate_pads (element);
  }

  while (!done) {
    gpointer padptr;

    switch (gst_iterator_next (pads, &padptr)) {
      case GST_ITERATOR_OK:
      {
        GstPad *peer;
        GstPad *current;
        GstPad *srcpad;
        GstPad *sinkpad;

        current = GST_PAD (padptr);

        GST_CAT_LOG (GST_CAT_ELEMENT_PADS, "examining pad %s:%s",
            GST_DEBUG_PAD_NAME (current));

        if (GST_PAD_IS_SRC (current)) {
          srcpad = current;
          sinkpad = pad;
        } else {
          srcpad = pad;
          sinkpad = current;
        }
        peer = gst_pad_get_peer (current);

        if (peer == NULL && gst_pad_check_link (srcpad, sinkpad)) {
          GstCaps *temp, *intersection;
          gboolean compatible;

          /* Now check if the two pads' caps are compatible */
          temp = gst_pad_get_caps_reffed (pad);
          if (caps) {
            intersection = gst_caps_intersect (temp, caps);
            gst_caps_unref (temp);
          } else {
            intersection = temp;
          }

          temp = gst_pad_get_caps_reffed (current);
          compatible = gst_caps_can_intersect (temp, intersection);
          gst_caps_unref (temp);
          gst_caps_unref (intersection);

          if (compatible) {
            GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS,
                "found existing unlinked compatible pad %s:%s",
                GST_DEBUG_PAD_NAME (current));
            gst_iterator_free (pads);

            return current;
          } else {
            GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS, "incompatible pads");
          }
        } else {
          GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS,
              "already linked or cannot be linked (peer = %p)", peer);
        }
        GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS, "unreffing pads");

        gst_object_unref (current);
        if (peer)
          gst_object_unref (peer);
        break;
      }
      case GST_ITERATOR_DONE:
        done = TRUE;
        break;
      case GST_ITERATOR_RESYNC:
        gst_iterator_resync (pads);
        break;
      case GST_ITERATOR_ERROR:
        g_assert_not_reached ();
        break;
    }
  }
  gst_iterator_free (pads);

  GST_CAT_DEBUG_OBJECT (GST_CAT_ELEMENT_PADS, element,
      "Could not find a compatible unlinked always pad to link to %s:%s, now checking request pads",
      GST_DEBUG_PAD_NAME (pad));

  /* try to create a new one */
  /* requesting is a little crazy, we need a template. Let's create one */
  /* FIXME: why not gst_pad_get_pad_template (pad); */
  templcaps = gst_pad_get_caps_reffed (pad);

  templ = gst_pad_template_new ((gchar *) GST_PAD_NAME (pad),
      GST_PAD_DIRECTION (pad), GST_PAD_ALWAYS, templcaps);

  foundpad = gst_element_request_compatible_pad (element, templ);
  gst_object_unref (templ);

  if (foundpad) {
    GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS,
        "found existing request pad %s:%s", GST_DEBUG_PAD_NAME (foundpad));
    return foundpad;
  }

  GST_CAT_INFO_OBJECT (GST_CAT_ELEMENT_PADS, element,
      "Could not find a compatible pad to link to %s:%s",
      GST_DEBUG_PAD_NAME (pad));
  return NULL;
}

/**
 * gst_element_state_get_name:
 * @state: a #GstState to get the name of.
 *
 * Gets a string representing the given state.
 *
 * Returns: (transfer none): a string with the name of the state.
 */
const gchar *
gst_element_state_get_name (GstState state)
{
  switch (state) {
    case GST_STATE_VOID_PENDING:
      return "VOID_PENDING";
    case GST_STATE_NULL:
      return "NULL";
    case GST_STATE_READY:
      return "READY";
    case GST_STATE_PLAYING:
      return "PLAYING";
    case GST_STATE_PAUSED:
      return "PAUSED";
    default:
      /* This is a memory leak */
      return g_strdup_printf ("UNKNOWN!(%d)", state);
  }
}

/**
 * gst_element_state_change_return_get_name:
 * @state_ret: a #GstStateChangeReturn to get the name of.
 *
 * Gets a string representing the given state change result.
 *
 * Returns: (transfer none): a string with the name of the state
 *    result.
 *
 * Since: 0.10.11
 */
const gchar *
gst_element_state_change_return_get_name (GstStateChangeReturn state_ret)
{
  switch (state_ret) {
    case GST_STATE_CHANGE_FAILURE:
      return "FAILURE";
    case GST_STATE_CHANGE_SUCCESS:
      return "SUCCESS";
    case GST_STATE_CHANGE_ASYNC:
      return "ASYNC";
    case GST_STATE_CHANGE_NO_PREROLL:
      return "NO PREROLL";
    default:
      /* This is a memory leak */
      return g_strdup_printf ("UNKNOWN!(%d)", state_ret);
  }
}


static gboolean
gst_element_factory_can_accept_all_caps_in_direction (GstElementFactory *
    factory, const GstCaps * caps, GstPadDirection direction)
{
  GList *templates;

  g_return_val_if_fail (factory != NULL, FALSE);
  g_return_val_if_fail (caps != NULL, FALSE);

  templates = factory->staticpadtemplates;

  while (templates) {
    GstStaticPadTemplate *template = (GstStaticPadTemplate *) templates->data;

    if (template->direction == direction) {
      GstCaps *templcaps = gst_static_caps_get (&template->static_caps);

      if (gst_caps_is_always_compatible (caps, templcaps)) {
        gst_caps_unref (templcaps);
        return TRUE;
      }
      gst_caps_unref (templcaps);
    }
    templates = g_list_next (templates);
  }

  return FALSE;
}

static gboolean
gst_element_factory_can_accept_any_caps_in_direction (GstElementFactory *
    factory, const GstCaps * caps, GstPadDirection direction)
{
  GList *templates;

  g_return_val_if_fail (factory != NULL, FALSE);
  g_return_val_if_fail (caps != NULL, FALSE);

  templates = factory->staticpadtemplates;

  while (templates) {
    GstStaticPadTemplate *template = (GstStaticPadTemplate *) templates->data;

    if (template->direction == direction) {
      GstCaps *templcaps = gst_static_caps_get (&template->static_caps);

      if (gst_caps_can_intersect (caps, templcaps)) {
        gst_caps_unref (templcaps);
        return TRUE;
      }
      gst_caps_unref (templcaps);
    }
    templates = g_list_next (templates);
  }

  return FALSE;
}

/**
 * gst_element_factory_can_src_caps:
 * @factory: factory to query
 * @caps: the caps to check
 *
 * Checks if the factory can source the given capability.
 *
 * Returns: %TRUE if it can src the capabilities
 *
 * Deprecated: use gst_element_factory_can_src_all_caps() instead.
 */
#ifndef GST_REMOVE_DEPRECATED
#ifdef GST_DISABLE_DEPRECATED
gboolean gst_element_factory_can_src_caps (GstElementFactory * factory,
    const GstCaps * caps);
#endif
gboolean
gst_element_factory_can_src_caps (GstElementFactory * factory,
    const GstCaps * caps)
{
  return gst_element_factory_can_accept_all_caps_in_direction (factory, caps,
      GST_PAD_SRC);
}
#endif /* GST_REMOVE_DEPRECATED */

/**
 * gst_element_factory_can_sink_caps:
 * @factory: factory to query
 * @caps: the caps to check
 *
 * Checks if the factory can sink the given capability.
 *
 * Returns: %TRUE if it can sink the capabilities
 *
 * Deprecated: use gst_element_factory_can_sink_all_caps() instead.
 */
#ifndef GST_REMOVE_DEPRECATED
#ifdef GST_DISABLE_DEPRECATED
gboolean gst_element_factory_can_sink_caps (GstElementFactory * factory,
    const GstCaps * caps);
#endif
gboolean
gst_element_factory_can_sink_caps (GstElementFactory * factory,
    const GstCaps * caps)
{
  return gst_element_factory_can_accept_all_caps_in_direction (factory, caps,
      GST_PAD_SINK);
}
#endif /* GST_REMOVE_DEPRECATED */

/**
 * gst_element_factory_can_sink_all_caps:
 * @factory: factory to query
 * @caps: the caps to check
 *
 * Checks if the factory can sink all possible capabilities.
 *
 * Returns: %TRUE if the caps are fully compatible.
 *
 * Since: 0.10.33
 */
gboolean
gst_element_factory_can_sink_all_caps (GstElementFactory * factory,
    const GstCaps * caps)
{
  return gst_element_factory_can_accept_all_caps_in_direction (factory, caps,
      GST_PAD_SINK);
}

/**
 * gst_element_factory_can_src_all_caps:
 * @factory: factory to query
 * @caps: the caps to check
 *
 * Checks if the factory can src all possible capabilities.
 *
 * Returns: %TRUE if the caps are fully compatible.
 *
 * Since: 0.10.33
 */
gboolean
gst_element_factory_can_src_all_caps (GstElementFactory * factory,
    const GstCaps * caps)
{
  return gst_element_factory_can_accept_all_caps_in_direction (factory, caps,
      GST_PAD_SRC);
}

/**
 * gst_element_factory_can_sink_any_caps:
 * @factory: factory to query
 * @caps: the caps to check
 *
 * Checks if the factory can sink any possible capability.
 *
 * Returns: %TRUE if the caps have a common subset.
 *
 * Since: 0.10.33
 */
gboolean
gst_element_factory_can_sink_any_caps (GstElementFactory * factory,
    const GstCaps * caps)
{
  return gst_element_factory_can_accept_any_caps_in_direction (factory, caps,
      GST_PAD_SINK);
}

/**
 * gst_element_factory_can_src_any_caps:
 * @factory: factory to query
 * @caps: the caps to check
 *
 * Checks if the factory can src any possible capability.
 *
 * Returns: %TRUE if the caps have a common subset.
 *
 * Since: 0.10.33
 */
gboolean
gst_element_factory_can_src_any_caps (GstElementFactory * factory,
    const GstCaps * caps)
{
  return gst_element_factory_can_accept_any_caps_in_direction (factory, caps,
      GST_PAD_SRC);
}

/* if return val is true, *direct_child is a caller-owned ref on the direct
 * child of ancestor that is part of object's ancestry */
static gboolean
object_has_ancestor (GstObject * object, GstObject * ancestor,
    GstObject ** direct_child)
{
  GstObject *child, *parent;

  if (direct_child)
    *direct_child = NULL;

  child = gst_object_ref (object);
  parent = gst_object_get_parent (object);

  while (parent) {
    if (ancestor == parent) {
      if (direct_child)
        *direct_child = child;
      else
        gst_object_unref (child);
      gst_object_unref (parent);
      return TRUE;
    }

    gst_object_unref (child);
    child = parent;
    parent = gst_object_get_parent (parent);
  }

  gst_object_unref (child);

  return FALSE;
}

/* caller owns return */
static GstObject *
find_common_root (GstObject * o1, GstObject * o2)
{
  GstObject *top = o1;
  GstObject *kid1, *kid2;
  GstObject *root = NULL;

  while (GST_OBJECT_PARENT (top))
    top = GST_OBJECT_PARENT (top);

  /* the itsy-bitsy spider... */

  if (!object_has_ancestor (o2, top, &kid2))
    return NULL;

  root = gst_object_ref (top);
  while (TRUE) {
    if (!object_has_ancestor (o1, kid2, &kid1)) {
      gst_object_unref (kid2);
      return root;
    }
    root = kid2;
    if (!object_has_ancestor (o2, kid1, &kid2)) {
      gst_object_unref (kid1);
      return root;
    }
    root = kid1;
  }
}

/* caller does not own return */
static GstPad *
ghost_up (GstElement * e, GstPad * pad)
{
  static gint ghost_pad_index = 0;
  GstPad *gpad;
  gchar *name;
  GstState current;
  GstState next;
  GstObject *parent = GST_OBJECT_PARENT (e);

  name = g_strdup_printf ("ghost%d", ghost_pad_index++);
  gpad = gst_ghost_pad_new (name, pad);
  g_free (name);

  GST_STATE_LOCK (e);
  gst_element_get_state (e, &current, &next, 0);

  if (current > GST_STATE_READY || next == GST_STATE_PAUSED)
    gst_pad_set_active (gpad, TRUE);

  if (!gst_element_add_pad ((GstElement *) parent, gpad)) {
    g_warning ("Pad named %s already exists in element %s\n",
        GST_OBJECT_NAME (gpad), GST_OBJECT_NAME (parent));
    gst_object_unref ((GstObject *) gpad);
    GST_STATE_UNLOCK (e);
    return NULL;
  }
  GST_STATE_UNLOCK (e);

  return gpad;
}

static void
remove_pad (gpointer ppad, gpointer unused)
{
  GstPad *pad = ppad;

  if (!gst_element_remove_pad ((GstElement *) GST_OBJECT_PARENT (pad), pad))
    g_warning ("Couldn't remove pad %s from element %s",
        GST_OBJECT_NAME (pad), GST_OBJECT_NAME (GST_OBJECT_PARENT (pad)));
}

static gboolean
prepare_link_maybe_ghosting (GstPad ** src, GstPad ** sink,
    GSList ** pads_created)
{
  GstObject *root;
  GstObject *e1, *e2;
  GSList *pads_created_local = NULL;

  g_assert (pads_created);

  e1 = GST_OBJECT_PARENT (*src);
  e2 = GST_OBJECT_PARENT (*sink);

  if (G_UNLIKELY (e1 == NULL)) {
    GST_WARNING ("Trying to ghost a pad that doesn't have a parent: %"
        GST_PTR_FORMAT, *src);
    return FALSE;
  }
  if (G_UNLIKELY (e2 == NULL)) {
    GST_WARNING ("Trying to ghost a pad that doesn't have a parent: %"
        GST_PTR_FORMAT, *sink);
    return FALSE;
  }

  if (GST_OBJECT_PARENT (e1) == GST_OBJECT_PARENT (e2)) {
    GST_CAT_INFO (GST_CAT_PADS, "%s and %s in same bin, no need for ghost pads",
        GST_OBJECT_NAME (e1), GST_OBJECT_NAME (e2));
    return TRUE;
  }

  GST_CAT_INFO (GST_CAT_PADS, "%s and %s not in same bin, making ghost pads",
      GST_OBJECT_NAME (e1), GST_OBJECT_NAME (e2));

  /* we need to setup some ghost pads */
  root = find_common_root (e1, e2);
  if (!root) {
    g_warning ("Trying to connect elements that don't share a common "
        "ancestor: %s and %s", GST_ELEMENT_NAME (e1), GST_ELEMENT_NAME (e2));
    return FALSE;
  }

  while (GST_OBJECT_PARENT (e1) != root) {
    *src = ghost_up ((GstElement *) e1, *src);
    if (!*src)
      goto cleanup_fail;
    e1 = GST_OBJECT_PARENT (*src);
    pads_created_local = g_slist_prepend (pads_created_local, *src);
  }
  while (GST_OBJECT_PARENT (e2) != root) {
    *sink = ghost_up ((GstElement *) e2, *sink);
    if (!*sink)
      goto cleanup_fail;
    e2 = GST_OBJECT_PARENT (*sink);
    pads_created_local = g_slist_prepend (pads_created_local, *sink);
  }

  gst_object_unref (root);
  *pads_created = g_slist_concat (*pads_created, pads_created_local);
  return TRUE;

cleanup_fail:
  gst_object_unref (root);
  g_slist_foreach (pads_created_local, remove_pad, NULL);
  g_slist_free (pads_created_local);
  return FALSE;
}

static gboolean
pad_link_maybe_ghosting (GstPad * src, GstPad * sink, GstPadLinkCheck flags)
{
  GSList *pads_created = NULL;
  gboolean ret;

  if (!prepare_link_maybe_ghosting (&src, &sink, &pads_created)) {
    ret = FALSE;
  } else {
    ret = (gst_pad_link_full (src, sink, flags) == GST_PAD_LINK_OK);
  }

  if (!ret) {
    g_slist_foreach (pads_created, remove_pad, NULL);
  }
  g_slist_free (pads_created);

  return ret;
}

/**
 * gst_element_link_pads_full:
 * @src: a #GstElement containing the source pad.
 * @srcpadname: (allow-none): the name of the #GstPad in source element
 *     or NULL for any pad.
 * @dest: (transfer none): the #GstElement containing the destination pad.
 * @destpadname: (allow-none): the name of the #GstPad in destination element,
 * or NULL for any pad.
 * @flags: the #GstPadLinkCheck to be performed when linking pads.
 *
 * Links the two named pads of the source and destination elements.
 * Side effect is that if one of the pads has no parent, it becomes a
 * child of the parent of the other element.  If they have different
 * parents, the link fails.
 *
 * Calling gst_element_link_pads_full() with @flags == %GST_PAD_LINK_CHECK_DEFAULT
 * is the same as calling gst_element_link_pads() and the recommended way of
 * linking pads with safety checks applied.
 *
 * This is a convenience function for gst_pad_link_full().
 *
 * Returns: TRUE if the pads could be linked, FALSE otherwise.
 *
 * Since: 0.10.30
 */
gboolean
gst_element_link_pads_full (GstElement * src, const gchar * srcpadname,
    GstElement * dest, const gchar * destpadname, GstPadLinkCheck flags)
{
  const GList *srcpads, *destpads, *srctempls, *desttempls, *l;
  GstPad *srcpad, *destpad;
  GstPadTemplate *srctempl, *desttempl;
  GstElementClass *srcclass, *destclass;

  /* checks */
  g_return_val_if_fail (GST_IS_ELEMENT (src), FALSE);
  g_return_val_if_fail (GST_IS_ELEMENT (dest), FALSE);

  GST_CAT_INFO (GST_CAT_ELEMENT_PADS,
      "trying to link element %s:%s to element %s:%s", GST_ELEMENT_NAME (src),
      srcpadname ? srcpadname : "(any)", GST_ELEMENT_NAME (dest),
      destpadname ? destpadname : "(any)");

  /* get a src pad */
  if (srcpadname) {
    /* name specified, look it up */
    if (!(srcpad = gst_element_get_static_pad (src, srcpadname)))
      srcpad = gst_element_get_request_pad (src, srcpadname);
    if (!srcpad) {
      GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS, "no pad %s:%s",
          GST_ELEMENT_NAME (src), srcpadname);
      return FALSE;
    } else {
      if (!(GST_PAD_DIRECTION (srcpad) == GST_PAD_SRC)) {
        GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS, "pad %s:%s is no src pad",
            GST_DEBUG_PAD_NAME (srcpad));
        gst_object_unref (srcpad);
        return FALSE;
      }
      if (GST_PAD_PEER (srcpad) != NULL) {
        GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS,
            "pad %s:%s is already linked to %s:%s", GST_DEBUG_PAD_NAME (srcpad),
            GST_DEBUG_PAD_NAME (GST_PAD_PEER (srcpad)));
        gst_object_unref (srcpad);
        return FALSE;
      }
    }
    srcpads = NULL;
  } else {
    /* no name given, get the first available pad */
    GST_OBJECT_LOCK (src);
    srcpads = GST_ELEMENT_PADS (src);
    srcpad = srcpads ? GST_PAD_CAST (srcpads->data) : NULL;
    if (srcpad)
      gst_object_ref (srcpad);
    GST_OBJECT_UNLOCK (src);
  }

  /* get a destination pad */
  if (destpadname) {
    /* name specified, look it up */
    if (!(destpad = gst_element_get_static_pad (dest, destpadname)))
      destpad = gst_element_get_request_pad (dest, destpadname);
    if (!destpad) {
      GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS, "no pad %s:%s",
          GST_ELEMENT_NAME (dest), destpadname);
      return FALSE;
    } else {
      if (!(GST_PAD_DIRECTION (destpad) == GST_PAD_SINK)) {
        GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS, "pad %s:%s is no sink pad",
            GST_DEBUG_PAD_NAME (destpad));
        gst_object_unref (destpad);
        return FALSE;
      }
      if (GST_PAD_PEER (destpad) != NULL) {
        GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS,
            "pad %s:%s is already linked to %s:%s",
            GST_DEBUG_PAD_NAME (destpad),
            GST_DEBUG_PAD_NAME (GST_PAD_PEER (destpad)));
        gst_object_unref (destpad);
        return FALSE;
      }
    }
    destpads = NULL;
  } else {
    /* no name given, get the first available pad */
    GST_OBJECT_LOCK (dest);
    destpads = GST_ELEMENT_PADS (dest);
    destpad = destpads ? GST_PAD_CAST (destpads->data) : NULL;
    if (destpad)
      gst_object_ref (destpad);
    GST_OBJECT_UNLOCK (dest);
  }

  if (srcpadname && destpadname) {
    gboolean result;

    /* two explicitly specified pads */
    result = pad_link_maybe_ghosting (srcpad, destpad, flags);

    gst_object_unref (srcpad);
    gst_object_unref (destpad);

    return result;
  }

  if (srcpad) {
    /* loop through the allowed pads in the source, trying to find a
     * compatible destination pad */
    GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS,
        "looping through allowed src and dest pads");
    do {
      GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS, "trying src pad %s:%s",
          GST_DEBUG_PAD_NAME (srcpad));
      if ((GST_PAD_DIRECTION (srcpad) == GST_PAD_SRC) &&
          (GST_PAD_PEER (srcpad) == NULL)) {
        GstPad *temp;

        if (destpadname) {
          temp = destpad;
          gst_object_ref (temp);
        } else {
          temp = gst_element_get_compatible_pad (dest, srcpad, NULL);
        }

        if (temp && pad_link_maybe_ghosting (srcpad, temp, flags)) {
          GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS, "linked pad %s:%s to pad %s:%s",
              GST_DEBUG_PAD_NAME (srcpad), GST_DEBUG_PAD_NAME (temp));
          if (destpad)
            gst_object_unref (destpad);
          gst_object_unref (srcpad);
          gst_object_unref (temp);
          return TRUE;
        }

        if (temp) {
          gst_object_unref (temp);
        }
      }
      /* find a better way for this mess */
      if (srcpads) {
        srcpads = g_list_next (srcpads);
        if (srcpads) {
          gst_object_unref (srcpad);
          srcpad = GST_PAD_CAST (srcpads->data);
          gst_object_ref (srcpad);
        }
      }
    } while (srcpads);
  }
  if (srcpadname) {
    GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS, "no link possible from %s:%s to %s",
        GST_DEBUG_PAD_NAME (srcpad), GST_ELEMENT_NAME (dest));
    if (destpad)
      gst_object_unref (destpad);
    destpad = NULL;
  }
  if (srcpad)
    gst_object_unref (srcpad);
  srcpad = NULL;

  if (destpad) {
    /* loop through the existing pads in the destination */
    do {
      GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS, "trying dest pad %s:%s",
          GST_DEBUG_PAD_NAME (destpad));
      if ((GST_PAD_DIRECTION (destpad) == GST_PAD_SINK) &&
          (GST_PAD_PEER (destpad) == NULL)) {
        GstPad *temp = gst_element_get_compatible_pad (src, destpad, NULL);

        if (temp && pad_link_maybe_ghosting (temp, destpad, flags)) {
          GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS, "linked pad %s:%s to pad %s:%s",
              GST_DEBUG_PAD_NAME (temp), GST_DEBUG_PAD_NAME (destpad));
          gst_object_unref (temp);
          gst_object_unref (destpad);
          return TRUE;
        }
        if (temp) {
          gst_object_unref (temp);
        }
      }
      if (destpads) {
        destpads = g_list_next (destpads);
        if (destpads) {
          gst_object_unref (destpad);
          destpad = GST_PAD_CAST (destpads->data);
          gst_object_ref (destpad);
        }
      }
    } while (destpads);
  }

  if (destpadname) {
    GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS, "no link possible from %s to %s:%s",
        GST_ELEMENT_NAME (src), GST_DEBUG_PAD_NAME (destpad));
    gst_object_unref (destpad);
    return FALSE;
  } else {
    if (destpad)
      gst_object_unref (destpad);
    destpad = NULL;
  }

  srcclass = GST_ELEMENT_GET_CLASS (src);
  destclass = GST_ELEMENT_GET_CLASS (dest);

  GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS,
      "we might have request pads on both sides, checking...");
  srctempls = gst_element_class_get_pad_template_list (srcclass);
  desttempls = gst_element_class_get_pad_template_list (destclass);

  if (srctempls && desttempls) {
    while (srctempls) {
      srctempl = (GstPadTemplate *) srctempls->data;
      if (srctempl->presence == GST_PAD_REQUEST) {
        for (l = desttempls; l; l = l->next) {
          desttempl = (GstPadTemplate *) l->data;
          if (desttempl->presence == GST_PAD_REQUEST &&
              desttempl->direction != srctempl->direction) {
            if (gst_caps_is_always_compatible (gst_pad_template_get_caps
                    (srctempl), gst_pad_template_get_caps (desttempl))) {
              srcpad =
                  gst_element_request_pad (src, srctempl,
                  srctempl->name_template, NULL);
              destpad =
                  gst_element_request_pad (dest, desttempl,
                  desttempl->name_template, NULL);
              if (srcpad && destpad
                  && pad_link_maybe_ghosting (srcpad, destpad, flags)) {
                GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS,
                    "linked pad %s:%s to pad %s:%s",
                    GST_DEBUG_PAD_NAME (srcpad), GST_DEBUG_PAD_NAME (destpad));
                gst_object_unref (srcpad);
                gst_object_unref (destpad);
                return TRUE;
              }
              /* it failed, so we release the request pads */
              if (srcpad)
                gst_element_release_request_pad (src, srcpad);
              if (destpad)
                gst_element_release_request_pad (dest, destpad);
            }
          }
        }
      }
      srctempls = srctempls->next;
    }
  }

  GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS, "no link possible from %s to %s",
      GST_ELEMENT_NAME (src), GST_ELEMENT_NAME (dest));
  return FALSE;
}

/**
 * gst_element_link_pads:
 * @src: a #GstElement containing the source pad.
 * @srcpadname: (allow-none): the name of the #GstPad in source element
 *     or NULL for any pad.
 * @dest: (transfer none): the #GstElement containing the destination pad.
 * @destpadname: (allow-none): the name of the #GstPad in destination element,
 * or NULL for any pad.
 *
 * Links the two named pads of the source and destination elements.
 * Side effect is that if one of the pads has no parent, it becomes a
 * child of the parent of the other element.  If they have different
 * parents, the link fails.
 *
 * Returns: TRUE if the pads could be linked, FALSE otherwise.
 */
gboolean
gst_element_link_pads (GstElement * src, const gchar * srcpadname,
    GstElement * dest, const gchar * destpadname)
{
  return gst_element_link_pads_full (src, srcpadname, dest, destpadname,
      GST_PAD_LINK_CHECK_DEFAULT);
}

/**
 * gst_element_link_pads_filtered:
 * @src: a #GstElement containing the source pad.
 * @srcpadname: (allow-none): the name of the #GstPad in source element
 *     or NULL for any pad.
 * @dest: (transfer none): the #GstElement containing the destination pad.
 * @destpadname: (allow-none): the name of the #GstPad in destination element
 *     or NULL for any pad.
 * @filter: (transfer none) (allow-none): the #GstCaps to filter the link,
 *     or #NULL for no filter.
 *
 * Links the two named pads of the source and destination elements. Side effect
 * is that if one of the pads has no parent, it becomes a child of the parent of
 * the other element. If they have different parents, the link fails. If @caps
 * is not #NULL, makes sure that the caps of the link is a subset of @caps.
 *
 * Returns: TRUE if the pads could be linked, FALSE otherwise.
 */
gboolean
gst_element_link_pads_filtered (GstElement * src, const gchar * srcpadname,
    GstElement * dest, const gchar * destpadname, GstCaps * filter)
{
  /* checks */
  g_return_val_if_fail (GST_IS_ELEMENT (src), FALSE);
  g_return_val_if_fail (GST_IS_ELEMENT (dest), FALSE);
  g_return_val_if_fail (filter == NULL || GST_IS_CAPS (filter), FALSE);

  if (filter) {
    GstElement *capsfilter;
    GstObject *parent;
    GstState state, pending;
    gboolean lr1, lr2;

    capsfilter = gst_element_factory_make ("capsfilter", NULL);
    if (!capsfilter) {
      GST_ERROR ("Could not make a capsfilter");
      return FALSE;
    }

    parent = gst_object_get_parent (GST_OBJECT (src));
    g_return_val_if_fail (GST_IS_BIN (parent), FALSE);

    gst_element_get_state (GST_ELEMENT_CAST (parent), &state, &pending, 0);

    if (!gst_bin_add (GST_BIN (parent), capsfilter)) {
      GST_ERROR ("Could not add capsfilter");
      gst_object_unref (capsfilter);
      gst_object_unref (parent);
      return FALSE;
    }

    if (pending != GST_STATE_VOID_PENDING)
      state = pending;

    gst_element_set_state (capsfilter, state);

    gst_object_unref (parent);

    g_object_set (capsfilter, "caps", filter, NULL);

    lr1 = gst_element_link_pads (src, srcpadname, capsfilter, "sink");
    lr2 = gst_element_link_pads (capsfilter, "src", dest, destpadname);
    if (lr1 && lr2) {
      return TRUE;
    } else {
      if (!lr1) {
        GST_INFO ("Could not link pads: %s:%s - capsfilter:sink",
            GST_ELEMENT_NAME (src), srcpadname);
      } else {
        GST_INFO ("Could not link pads: capsfilter:src - %s:%s",
            GST_ELEMENT_NAME (dest), destpadname);
      }
      gst_element_set_state (capsfilter, GST_STATE_NULL);
      /* this will unlink and unref as appropriate */
      gst_bin_remove (GST_BIN (GST_OBJECT_PARENT (capsfilter)), capsfilter);
      return FALSE;
    }
  } else {
    if (gst_element_link_pads (src, srcpadname, dest, destpadname)) {
      return TRUE;
    } else {
      GST_INFO ("Could not link pads: %s:%s - %s:%s", GST_ELEMENT_NAME (src),
          srcpadname, GST_ELEMENT_NAME (dest), destpadname);
      return FALSE;
    }
  }
}

/**
 * gst_element_link:
 * @src: (transfer none): a #GstElement containing the source pad.
 * @dest: (transfer none): the #GstElement containing the destination pad.
 *
 * Links @src to @dest. The link must be from source to
 * destination; the other direction will not be tried. The function looks for
 * existing pads that aren't linked yet. It will request new pads if necessary.
 * Such pads need to be released manualy when unlinking.
 * If multiple links are possible, only one is established.
 *
 * Make sure you have added your elements to a bin or pipeline with
 * gst_bin_add() before trying to link them.
 *
 * Returns: TRUE if the elements could be linked, FALSE otherwise.
 */
gboolean
gst_element_link (GstElement * src, GstElement * dest)
{
  return gst_element_link_pads (src, NULL, dest, NULL);
}

/**
 * gst_element_link_many:
 * @element_1: (transfer none): the first #GstElement in the link chain.
 * @element_2: (transfer none): the second #GstElement in the link chain.
 * @...: the NULL-terminated list of elements to link in order.
 *
 * Chain together a series of elements. Uses gst_element_link().
 * Make sure you have added your elements to a bin or pipeline with
 * gst_bin_add() before trying to link them.
 *
 * Returns: TRUE on success, FALSE otherwise.
 */
gboolean
gst_element_link_many (GstElement * element_1, GstElement * element_2, ...)
{
  gboolean res = TRUE;
  va_list args;

  g_return_val_if_fail (GST_IS_ELEMENT (element_1), FALSE);
  g_return_val_if_fail (GST_IS_ELEMENT (element_2), FALSE);

  va_start (args, element_2);

  while (element_2) {
    if (!gst_element_link (element_1, element_2)) {
      res = FALSE;
      break;
    }

    element_1 = element_2;
    element_2 = va_arg (args, GstElement *);
  }

  va_end (args);

  return res;
}

/**
 * gst_element_link_filtered:
 * @src: a #GstElement containing the source pad.
 * @dest: (transfer none): the #GstElement containing the destination pad.
 * @filter: (transfer none) (allow-none): the #GstCaps to filter the link,
 *     or #NULL for no filter.
 *
 * Links @src to @dest using the given caps as filtercaps.
 * The link must be from source to
 * destination; the other direction will not be tried. The function looks for
 * existing pads that aren't linked yet. It will request new pads if necessary.
 * If multiple links are possible, only one is established.
 *
 * Make sure you have added your elements to a bin or pipeline with
 * gst_bin_add() before trying to link them.
 *
 * Returns: TRUE if the pads could be linked, FALSE otherwise.
 */
gboolean
gst_element_link_filtered (GstElement * src, GstElement * dest,
    GstCaps * filter)
{
  return gst_element_link_pads_filtered (src, NULL, dest, NULL, filter);
}

/**
 * gst_element_unlink_pads:
 * @src: a (transfer none): #GstElement containing the source pad.
 * @srcpadname: the name of the #GstPad in source element.
 * @dest: (transfer none): a #GstElement containing the destination pad.
 * @destpadname: the name of the #GstPad in destination element.
 *
 * Unlinks the two named pads of the source and destination elements.
 *
 * This is a convenience function for gst_pad_unlink().
 */
void
gst_element_unlink_pads (GstElement * src, const gchar * srcpadname,
    GstElement * dest, const gchar * destpadname)
{
  GstPad *srcpad, *destpad;
  gboolean srcrequest, destrequest;

  srcrequest = destrequest = FALSE;

  g_return_if_fail (src != NULL);
  g_return_if_fail (GST_IS_ELEMENT (src));
  g_return_if_fail (srcpadname != NULL);
  g_return_if_fail (dest != NULL);
  g_return_if_fail (GST_IS_ELEMENT (dest));
  g_return_if_fail (destpadname != NULL);

  /* obtain the pads requested */
  if (!(srcpad = gst_element_get_static_pad (src, srcpadname)))
    if ((srcpad = gst_element_get_request_pad (src, srcpadname)))
      srcrequest = TRUE;
  if (srcpad == NULL) {
    GST_WARNING_OBJECT (src, "source element has no pad \"%s\"", srcpadname);
    return;
  }
  if (!(destpad = gst_element_get_static_pad (dest, destpadname)))
    if ((destpad = gst_element_get_request_pad (dest, destpadname)))
      destrequest = TRUE;
  if (destpad == NULL) {
    GST_WARNING_OBJECT (dest, "destination element has no pad \"%s\"",
        destpadname);
    goto free_src;
  }

  /* we're satisified they can be unlinked, let's do it */
  gst_pad_unlink (srcpad, destpad);

  if (destrequest)
    gst_element_release_request_pad (dest, destpad);
  gst_object_unref (destpad);

free_src:
  if (srcrequest)
    gst_element_release_request_pad (src, srcpad);
  gst_object_unref (srcpad);
}

/**
 * gst_element_unlink_many:
 * @element_1: (transfer none): the first #GstElement in the link chain.
 * @element_2: (transfer none): the second #GstElement in the link chain.
 * @...: the NULL-terminated list of elements to unlink in order.
 *
 * Unlinks a series of elements. Uses gst_element_unlink().
 */
void
gst_element_unlink_many (GstElement * element_1, GstElement * element_2, ...)
{
  va_list args;

  g_return_if_fail (element_1 != NULL && element_2 != NULL);
  g_return_if_fail (GST_IS_ELEMENT (element_1) && GST_IS_ELEMENT (element_2));

  va_start (args, element_2);

  while (element_2) {
    gst_element_unlink (element_1, element_2);

    element_1 = element_2;
    element_2 = va_arg (args, GstElement *);
  }

  va_end (args);
}

/**
 * gst_element_unlink:
 * @src: (transfer none): the source #GstElement to unlink.
 * @dest: (transfer none): the sink #GstElement to unlink.
 *
 * Unlinks all source pads of the source element with all sink pads
 * of the sink element to which they are linked.
 *
 * If the link has been made using gst_element_link(), it could have created an
 * requestpad, which has to be released using gst_element_release_request_pad().
 */
void
gst_element_unlink (GstElement * src, GstElement * dest)
{
  GstIterator *pads;
  gboolean done = FALSE;

  g_return_if_fail (GST_IS_ELEMENT (src));
  g_return_if_fail (GST_IS_ELEMENT (dest));

  GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS, "unlinking \"%s\" and \"%s\"",
      GST_ELEMENT_NAME (src), GST_ELEMENT_NAME (dest));

  pads = gst_element_iterate_pads (src);
  while (!done) {
    gpointer data;

    switch (gst_iterator_next (pads, &data)) {
      case GST_ITERATOR_OK:
      {
        GstPad *pad = GST_PAD_CAST (data);

        if (GST_PAD_IS_SRC (pad)) {
          GstPad *peerpad = gst_pad_get_peer (pad);

          /* see if the pad is linked and is really a pad of dest */
          if (peerpad) {
            GstElement *peerelem;

            peerelem = gst_pad_get_parent_element (peerpad);

            if (peerelem == dest) {
              gst_pad_unlink (pad, peerpad);
            }
            if (peerelem)
              gst_object_unref (peerelem);

            gst_object_unref (peerpad);
          }
        }
        gst_object_unref (pad);
        break;
      }
      case GST_ITERATOR_RESYNC:
        gst_iterator_resync (pads);
        break;
      case GST_ITERATOR_DONE:
        done = TRUE;
        break;
      default:
        g_assert_not_reached ();
        break;
    }
  }
  gst_iterator_free (pads);
}

/**
 * gst_element_query_position:
 * @element: a #GstElement to invoke the position query on.
 * @format: (inout): a pointer to the #GstFormat asked for.
 *          On return contains the #GstFormat used.
 * @cur: (out) (allow-none): a location in which to store the current
 *     position, or NULL.
 *
 * Queries an element for the stream position. If one repeatedly calls this
 * function one can also create and reuse it in gst_element_query().
 *
 * Returns: TRUE if the query could be performed.
 */
gboolean
gst_element_query_position (GstElement * element, GstFormat * format,
    gint64 * cur)
{
  GstQuery *query;
  gboolean ret;

  g_return_val_if_fail (GST_IS_ELEMENT (element), FALSE);
  g_return_val_if_fail (format != NULL, FALSE);

  query = gst_query_new_position (*format);
  ret = gst_element_query (element, query);

  if (ret)
    gst_query_parse_position (query, format, cur);

  gst_query_unref (query);

  return ret;
}

/**
 * gst_element_query_duration:
 * @element: a #GstElement to invoke the duration query on.
 * @format: (inout): a pointer to the #GstFormat asked for.
 *          On return contains the #GstFormat used.
 * @duration: (out): A location in which to store the total duration, or NULL.
 *
 * Queries an element for the total stream duration.
 *
 * Returns: TRUE if the query could be performed.
 */
gboolean
gst_element_query_duration (GstElement * element, GstFormat * format,
    gint64 * duration)
{
  GstQuery *query;
  gboolean ret;

  g_return_val_if_fail (GST_IS_ELEMENT (element), FALSE);
  g_return_val_if_fail (format != NULL, FALSE);

  query = gst_query_new_duration (*format);
  ret = gst_element_query (element, query);

  if (ret)
    gst_query_parse_duration (query, format, duration);

  gst_query_unref (query);

  return ret;
}

/**
 * gst_element_query_convert:
 * @element: a #GstElement to invoke the convert query on.
 * @src_format: (inout): a #GstFormat to convert from.
 * @src_val: a value to convert.
 * @dest_format: (inout): a pointer to the #GstFormat to convert to.
 * @dest_val: (out): a pointer to the result.
 *
 * Queries an element to convert @src_val in @src_format to @dest_format.
 *
 * Returns: TRUE if the query could be performed.
 */
gboolean
gst_element_query_convert (GstElement * element, GstFormat src_format,
    gint64 src_val, GstFormat * dest_format, gint64 * dest_val)
{
  GstQuery *query;
  gboolean ret;

  g_return_val_if_fail (GST_IS_ELEMENT (element), FALSE);
  g_return_val_if_fail (dest_format != NULL, FALSE);
  g_return_val_if_fail (dest_val != NULL, FALSE);

  if (*dest_format == src_format || src_val == -1) {
    *dest_val = src_val;
    return TRUE;
  }

  query = gst_query_new_convert (src_format, src_val, *dest_format);
  ret = gst_element_query (element, query);

  if (ret)
    gst_query_parse_convert (query, NULL, NULL, dest_format, dest_val);

  gst_query_unref (query);

  return ret;
}

/**
 * gst_element_seek_simple
 * @element: a #GstElement to seek on
 * @format: a #GstFormat to execute the seek in, such as #GST_FORMAT_TIME
 * @seek_flags: seek options; playback applications will usually want to use
 *            GST_SEEK_FLAG_FLUSH | GST_SEEK_FLAG_KEY_UNIT here
 * @seek_pos: position to seek to (relative to the start); if you are doing
 *            a seek in #GST_FORMAT_TIME this value is in nanoseconds -
 *            multiply with #GST_SECOND to convert seconds to nanoseconds or
 *            with #GST_MSECOND to convert milliseconds to nanoseconds.
 *
 * Simple API to perform a seek on the given element, meaning it just seeks
 * to the given position relative to the start of the stream. For more complex
 * operations like segment seeks (e.g. for looping) or changing the playback
 * rate or seeking relative to the last configured playback segment you should
 * use gst_element_seek().
 *
 * In a completely prerolled PAUSED or PLAYING pipeline, seeking is always
 * guaranteed to return %TRUE on a seekable media type or %FALSE when the media
 * type is certainly not seekable (such as a live stream).
 *
 * Some elements allow for seeking in the READY state, in this
 * case they will store the seek event and execute it when they are put to
 * PAUSED. If the element supports seek in READY, it will always return %TRUE when
 * it receives the event in the READY state.
 *
 * Returns: %TRUE if the seek operation succeeded (the seek might not always be
 * executed instantly though)
 *
 * Since: 0.10.7
 */
gboolean
gst_element_seek_simple (GstElement * element, GstFormat format,
    GstSeekFlags seek_flags, gint64 seek_pos)
{
  g_return_val_if_fail (GST_IS_ELEMENT (element), FALSE);
  g_return_val_if_fail (seek_pos >= 0, FALSE);

  return gst_element_seek (element, 1.0, format, seek_flags,
      GST_SEEK_TYPE_SET, seek_pos, GST_SEEK_TYPE_NONE, 0);
}

/**
 * gst_pad_use_fixed_caps:
 * @pad: the pad to use
 *
 * A helper function you can use that sets the
 * @gst_pad_get_fixed_caps_func as the getcaps function for the
 * pad. This way the function will always return the negotiated caps
 * or in case the pad is not negotiated, the padtemplate caps.
 *
 * Use this function on a pad that, once gst_pad_set_caps() has been called
 * on it, cannot be renegotiated to something else.
 */
void
gst_pad_use_fixed_caps (GstPad * pad)
{
  gst_pad_set_getcaps_function (pad, gst_pad_get_fixed_caps_func);
}

/**
 * gst_pad_get_fixed_caps_func:
 * @pad: the pad to use
 *
 * A helper function you can use as a GetCaps function that
 * will return the currently negotiated caps or the padtemplate
 * when NULL.
 *
 * Free-function: gst_caps_unref
 *
 * Returns: (transfer full): the currently negotiated caps or the padtemplate.
 */
GstCaps *
gst_pad_get_fixed_caps_func (GstPad * pad)
{
  GstCaps *result;

  g_return_val_if_fail (GST_IS_PAD (pad), NULL);

  GST_OBJECT_LOCK (pad);
  if (GST_PAD_CAPS (pad)) {
    result = GST_PAD_CAPS (pad);

    GST_CAT_DEBUG (GST_CAT_CAPS,
        "using pad caps %p %" GST_PTR_FORMAT, result, result);

    result = gst_caps_ref (result);
  } else if (GST_PAD_PAD_TEMPLATE (pad)) {
    GstPadTemplate *templ = GST_PAD_PAD_TEMPLATE (pad);

    result = GST_PAD_TEMPLATE_CAPS (templ);
    GST_CAT_DEBUG (GST_CAT_CAPS,
        "using pad template %p with caps %p %" GST_PTR_FORMAT, templ, result,
        result);

    result = gst_caps_ref (result);
  } else {
    GST_CAT_DEBUG (GST_CAT_CAPS, "pad has no caps");
    result = gst_caps_new_empty ();
  }
  GST_OBJECT_UNLOCK (pad);

  return result;
}

/**
 * gst_pad_get_parent_element:
 * @pad: a pad
 *
 * Gets the parent of @pad, cast to a #GstElement. If a @pad has no parent or
 * its parent is not an element, return NULL.
 *
 * Returns: (transfer full): the parent of the pad. The caller has a
 * reference on the parent, so unref when you're finished with it.
 *
 * MT safe.
 */
GstElement *
gst_pad_get_parent_element (GstPad * pad)
{
  GstObject *p;

  g_return_val_if_fail (GST_IS_PAD (pad), NULL);

  p = gst_object_get_parent (GST_OBJECT_CAST (pad));

  if (p && !GST_IS_ELEMENT (p)) {
    gst_object_unref (p);
    p = NULL;
  }
  return GST_ELEMENT_CAST (p);
}

/**
 * gst_object_default_error:
 * @source: the #GstObject that initiated the error.
 * @error: (in): the GError.
 * @debug: (in) (allow-none): an additional debug information string, or NULL
 *
 * A default error function.
 *
 * The default handler will simply print the error string using g_print.
 */
void
gst_object_default_error (GstObject * source, const GError * error,
    const gchar * debug)
{
  gchar *name = gst_object_get_path_string (source);

  /* FIXME 0.11: should change this to g_printerr() */
  g_print (_("ERROR: from element %s: %s\n"), name, error->message);
  if (debug)
    g_print (_("Additional debug info:\n%s\n"), debug);

  g_free (name);
}

/**
 * gst_bin_add_many:
 * @bin: a #GstBin
 * @element_1: (transfer full): the #GstElement element to add to the bin
 * @...: (transfer full): additional elements to add to the bin
 *
 * Adds a NULL-terminated list of elements to a bin.  This function is
 * equivalent to calling gst_bin_add() for each member of the list. The return
 * value of each gst_bin_add() is ignored.
 */
void
gst_bin_add_many (GstBin * bin, GstElement * element_1, ...)
{
  va_list args;

  g_return_if_fail (GST_IS_BIN (bin));
  g_return_if_fail (GST_IS_ELEMENT (element_1));

  va_start (args, element_1);

  while (element_1) {
    gst_bin_add (bin, element_1);

    element_1 = va_arg (args, GstElement *);
  }

  va_end (args);
}

/**
 * gst_bin_remove_many:
 * @bin: a #GstBin
 * @element_1: (transfer none): the first #GstElement to remove from the bin
 * @...: (transfer none): NULL-terminated list of elements to remove from the bin
 *
 * Remove a list of elements from a bin. This function is equivalent
 * to calling gst_bin_remove() with each member of the list.
 */
void
gst_bin_remove_many (GstBin * bin, GstElement * element_1, ...)
{
  va_list args;

  g_return_if_fail (GST_IS_BIN (bin));
  g_return_if_fail (GST_IS_ELEMENT (element_1));

  va_start (args, element_1);

  while (element_1) {
    gst_bin_remove (bin, element_1);

    element_1 = va_arg (args, GstElement *);
  }

  va_end (args);
}

static void
gst_element_populate_std_props (GObjectClass * klass, const gchar * prop_name,
    guint arg_id, GParamFlags flags)
{
  GQuark prop_id = g_quark_from_string (prop_name);
  GParamSpec *pspec;

  static GQuark fd_id = 0;
  static GQuark blocksize_id;
  static GQuark bytesperread_id;
  static GQuark dump_id;
  static GQuark filesize_id;
  static GQuark mmapsize_id;
  static GQuark location_id;
  static GQuark offset_id;
  static GQuark silent_id;
  static GQuark touch_id;

  flags |= G_PARAM_STATIC_STRINGS;

  if (!fd_id) {
    fd_id = g_quark_from_static_string ("fd");
    blocksize_id = g_quark_from_static_string ("blocksize");
    bytesperread_id = g_quark_from_static_string ("bytesperread");
    dump_id = g_quark_from_static_string ("dump");
    filesize_id = g_quark_from_static_string ("filesize");
    mmapsize_id = g_quark_from_static_string ("mmapsize");
    location_id = g_quark_from_static_string ("location");
    offset_id = g_quark_from_static_string ("offset");
    silent_id = g_quark_from_static_string ("silent");
    touch_id = g_quark_from_static_string ("touch");
  }

  if (prop_id == fd_id) {
    pspec = g_param_spec_int ("fd", "File-descriptor",
        "File-descriptor for the file being read", 0, G_MAXINT, 0, flags);
  } else if (prop_id == blocksize_id) {
    pspec = g_param_spec_ulong ("blocksize", "Block Size",
        "Block size to read per buffer", 0, G_MAXULONG, 4096, flags);

  } else if (prop_id == bytesperread_id) {
    pspec = g_param_spec_int ("bytesperread", "Bytes per read",
        "Number of bytes to read per buffer", G_MININT, G_MAXINT, 0, flags);

  } else if (prop_id == dump_id) {
    pspec = g_param_spec_boolean ("dump", "Dump",
        "Dump bytes to stdout", FALSE, flags);

  } else if (prop_id == filesize_id) {
    pspec = g_param_spec_int64 ("filesize", "File Size",
        "Size of the file being read", 0, G_MAXINT64, 0, flags);

  } else if (prop_id == mmapsize_id) {
    pspec = g_param_spec_ulong ("mmapsize", "mmap() Block Size",
        "Size in bytes of mmap()d regions", 0, G_MAXULONG, 4 * 1048576, flags);

  } else if (prop_id == location_id) {
    pspec = g_param_spec_string ("location", "File Location",
        "Location of the file to read", NULL, flags);

  } else if (prop_id == offset_id) {
    pspec = g_param_spec_int64 ("offset", "File Offset",
        "Byte offset of current read pointer", 0, G_MAXINT64, 0, flags);

  } else if (prop_id == silent_id) {
    pspec = g_param_spec_boolean ("silent", "Silent", "Don't produce events",
        FALSE, flags);

  } else if (prop_id == touch_id) {
    pspec = g_param_spec_boolean ("touch", "Touch read data",
        "Touch data to force disk read before " "push ()", TRUE, flags);
  } else {
    g_warning ("Unknown - 'standard' property '%s' id %d from klass %s",
        prop_name, arg_id, g_type_name (G_OBJECT_CLASS_TYPE (klass)));
    pspec = NULL;
  }

  if (pspec) {
    g_object_class_install_property (klass, arg_id, pspec);
  }
}

/**
 * gst_element_class_install_std_props:
 * @klass: the #GstElementClass to add the properties to.
 * @first_name: the name of the first property.
 * in a NULL terminated
 * @...: the id and flags of the first property, followed by
 * further 'name', 'id', 'flags' triplets and terminated by NULL.
 *
 * Adds a list of standardized properties with types to the @klass.
 * the id is for the property switch in your get_prop method, and
 * the flags determine readability / writeability.
 **/
void
gst_element_class_install_std_props (GstElementClass * klass,
    const gchar * first_name, ...)
{
  const char *name;

  va_list args;

  g_return_if_fail (GST_IS_ELEMENT_CLASS (klass));

  va_start (args, first_name);

  name = first_name;

  while (name) {
    int arg_id = va_arg (args, int);
    int flags = va_arg (args, int);

    gst_element_populate_std_props ((GObjectClass *) klass, name, arg_id,
        flags);

    name = va_arg (args, char *);
  }

  va_end (args);
}


/**
 * gst_buffer_merge:
 * @buf1: (transfer none): the first source #GstBuffer to merge.
 * @buf2: (transfer none): the second source #GstBuffer to merge.
 *
 * Create a new buffer that is the concatenation of the two source
 * buffers.  The original source buffers will not be modified or
 * unref'd.  Make sure you unref the source buffers if they are not used
 * anymore afterwards.
 *
 * If the buffers point to contiguous areas of memory, the buffer
 * is created without copying the data.
 *
 * Free-function: gst_buffer_unref
 *
 * Returns: (transfer full): the new #GstBuffer which is the concatenation
 *     of the source buffers.
 */
GstBuffer *
gst_buffer_merge (GstBuffer * buf1, GstBuffer * buf2)
{
  GstBuffer *result;

  /* we're just a specific case of the more general gst_buffer_span() */
  result = gst_buffer_span (buf1, 0, buf2, buf1->size + buf2->size);

  return result;
}

/**
 * gst_buffer_join:
 * @buf1: the first source #GstBuffer.
 * @buf2: the second source #GstBuffer.
 *
 * Create a new buffer that is the concatenation of the two source
 * buffers, and unrefs the original source buffers.
 *
 * If the buffers point to contiguous areas of memory, the buffer
 * is created without copying the data.
 *
 * This is a convenience function for C programmers. See also
 * gst_buffer_merge(), which does the same thing without
 * unreffing the input parameters. Language bindings without
 * explicit reference counting should not wrap this function.
 *
 * Returns: (transfer full): the new #GstBuffer which is the concatenation of
 * the source buffers.
 */
GstBuffer *
gst_buffer_join (GstBuffer * buf1, GstBuffer * buf2)
{
  GstBuffer *result;

  result = gst_buffer_span (buf1, 0, buf2, buf1->size + buf2->size);
  gst_buffer_unref (buf1);
  gst_buffer_unref (buf2);

  return result;
}


/**
 * gst_buffer_stamp:
 * @dest: (transfer none): buffer to stamp
 * @src: buffer to stamp from
 *
 * Copies additional information (the timestamp, duration, and offset start
 * and end) from one buffer to the other.
 *
 * This function does not copy any buffer flags or caps and is equivalent to
 * gst_buffer_copy_metadata(@dest, @src, GST_BUFFER_COPY_TIMESTAMPS).
 *
 * Deprecated: use gst_buffer_copy_metadata() instead, it provides more
 * control.
 */
#ifndef GST_REMOVE_DEPRECATED
#ifdef GST_DISABLE_DEPRECATED
void gst_buffer_stamp (GstBuffer * dest, const GstBuffer * src);
#endif
void
gst_buffer_stamp (GstBuffer * dest, const GstBuffer * src)
{
  gst_buffer_copy_metadata (dest, src, GST_BUFFER_COPY_TIMESTAMPS);
}
#endif /* GST_REMOVE_DEPRECATED */

static gboolean
getcaps_fold_func (GstPad * pad, GValue * ret, GstPad * orig)
{
  gboolean empty = FALSE;
  GstCaps *peercaps, *existing;

  existing = g_value_get_pointer (ret);
  peercaps = gst_pad_peer_get_caps_reffed (pad);
  if (G_LIKELY (peercaps)) {
    GstCaps *intersection = gst_caps_intersect (existing, peercaps);

    empty = gst_caps_is_empty (intersection);

    g_value_set_pointer (ret, intersection);
    gst_caps_unref (existing);
    gst_caps_unref (peercaps);
  }
  gst_object_unref (pad);
  return !empty;
}

/**
 * gst_pad_proxy_getcaps:
 * @pad: a #GstPad to proxy.
 *
 * Calls gst_pad_get_allowed_caps() for every other pad belonging to the
 * same element as @pad, and returns the intersection of the results.
 *
 * This function is useful as a default getcaps function for an element
 * that can handle any stream format, but requires all its pads to have
 * the same caps.  Two such elements are tee and adder.
 *
 * Free-function: gst_caps_unref
 *
 * Returns: (transfer full): the intersection of the other pads' allowed caps.
 */
GstCaps *
gst_pad_proxy_getcaps (GstPad * pad)
{
  GstElement *element;
  GstCaps *caps, *intersected;
  GstIterator *iter;
  GstIteratorResult res;
  GValue ret = { 0, };

  g_return_val_if_fail (GST_IS_PAD (pad), NULL);

  GST_CAT_DEBUG (GST_CAT_PADS, "proxying getcaps for %s:%s",
      GST_DEBUG_PAD_NAME (pad));

  element = gst_pad_get_parent_element (pad);
  if (element == NULL)
    goto no_parent;

  /* value to hold the return, by default it holds ANY, the ref is taken by
   * the GValue. */
  g_value_init (&ret, G_TYPE_POINTER);
  g_value_set_pointer (&ret, gst_caps_new_any ());

  /* only iterate the pads in the oposite direction */
  if (GST_PAD_IS_SRC (pad))
    iter = gst_element_iterate_sink_pads (element);
  else
    iter = gst_element_iterate_src_pads (element);

  while (1) {
    res =
        gst_iterator_fold (iter, (GstIteratorFoldFunction) getcaps_fold_func,
        &ret, pad);
    switch (res) {
      case GST_ITERATOR_RESYNC:
        /* unref any value stored */
        if ((caps = g_value_get_pointer (&ret)))
          gst_caps_unref (caps);
        /* need to reset the result again to ANY */
        g_value_set_pointer (&ret, gst_caps_new_any ());
        gst_iterator_resync (iter);
        break;
      case GST_ITERATOR_DONE:
        /* all pads iterated, return collected value */
        goto done;
      case GST_ITERATOR_OK:
        /* premature exit (happens if caps intersection is empty) */
        goto done;
      default:
        /* iterator returned _ERROR, mark an error and exit */
        if ((caps = g_value_get_pointer (&ret)))
          gst_caps_unref (caps);
        g_value_set_pointer (&ret, NULL);
        goto error;
    }
  }
done:
  gst_iterator_free (iter);

  gst_object_unref (element);

  caps = g_value_get_pointer (&ret);
  g_value_unset (&ret);

  if (caps) {
    intersected =
        gst_caps_intersect (caps, gst_pad_get_pad_template_caps (pad));
    gst_caps_unref (caps);
  } else {
    intersected = gst_caps_copy (gst_pad_get_pad_template_caps (pad));
  }

  return intersected;

  /* ERRORS */
no_parent:
  {
    GST_DEBUG_OBJECT (pad, "no parent");
    return gst_caps_copy (gst_pad_get_pad_template_caps (pad));
  }
error:
  {
    g_warning ("Pad list returned error on element %s",
        GST_ELEMENT_NAME (element));
    gst_iterator_free (iter);
    gst_object_unref (element);
    return gst_caps_copy (gst_pad_get_pad_template_caps (pad));
  }
}

typedef struct
{
  GstPad *orig;
  GstCaps *caps;
} SetCapsFoldData;

static gboolean
setcaps_fold_func (GstPad * pad, GValue * ret, SetCapsFoldData * data)
{
  gboolean success = TRUE;

  if (pad != data->orig) {
    success = gst_pad_set_caps (pad, data->caps);
    g_value_set_boolean (ret, success);
  }
  gst_object_unref (pad);

  return success;
}

/**
 * gst_pad_proxy_setcaps
 * @pad: a #GstPad to proxy from
 * @caps: (transfer none): the #GstCaps to link with
 *
 * Calls gst_pad_set_caps() for every other pad belonging to the
 * same element as @pad.  If gst_pad_set_caps() fails on any pad,
 * the proxy setcaps fails. May be used only during negotiation.
 *
 * Returns: TRUE if sucessful
 */
gboolean
gst_pad_proxy_setcaps (GstPad * pad, GstCaps * caps)
{
  GstElement *element;
  GstIterator *iter;
  GstIteratorResult res;
  GValue ret = { 0, };
  SetCapsFoldData data;

  g_return_val_if_fail (GST_IS_PAD (pad), FALSE);
  g_return_val_if_fail (caps != NULL, FALSE);

  GST_CAT_DEBUG (GST_CAT_PADS, "proxying pad link for %s:%s",
      GST_DEBUG_PAD_NAME (pad));

  element = gst_pad_get_parent_element (pad);
  if (element == NULL)
    return FALSE;

  /* only iterate the pads in the oposite direction */
  if (GST_PAD_IS_SRC (pad))
    iter = gst_element_iterate_sink_pads (element);
  else
    iter = gst_element_iterate_src_pads (element);

  g_value_init (&ret, G_TYPE_BOOLEAN);
  g_value_set_boolean (&ret, TRUE);
  data.orig = pad;
  data.caps = caps;

  while (1) {
    res = gst_iterator_fold (iter, (GstIteratorFoldFunction) setcaps_fold_func,
        &ret, &data);

    switch (res) {
      case GST_ITERATOR_RESYNC:
        /* reset return value */
        g_value_set_boolean (&ret, TRUE);
        gst_iterator_resync (iter);
        break;
      case GST_ITERATOR_DONE:
        /* all pads iterated, return collected value */
        goto done;
      default:
        /* iterator returned _ERROR or premature end with _OK,
         * mark an error and exit */
        goto error;
    }
  }
done:
  gst_iterator_free (iter);

  gst_object_unref (element);

  /* ok not to unset the gvalue */
  return g_value_get_boolean (&ret);

  /* ERRORS */
error:
  {
    g_warning ("Pad list return error on element %s",
        GST_ELEMENT_NAME (element));
    gst_iterator_free (iter);
    gst_object_unref (element);
    return FALSE;
  }
}

/**
 * gst_pad_query_position:
 * @pad: a #GstPad to invoke the position query on.
 * @format: (inout): a pointer to the #GstFormat asked for.
 *          On return contains the #GstFormat used.
 * @cur: (out): A location in which to store the current position, or NULL.
 *
 * Queries a pad for the stream position.
 *
 * Returns: TRUE if the query could be performed.
 */
gboolean
gst_pad_query_position (GstPad * pad, GstFormat * format, gint64 * cur)
{
  GstQuery *query;
  gboolean ret;

  g_return_val_if_fail (GST_IS_PAD (pad), FALSE);
  g_return_val_if_fail (format != NULL, FALSE);

  query = gst_query_new_position (*format);
  ret = gst_pad_query (pad, query);

  if (ret)
    gst_query_parse_position (query, format, cur);

  gst_query_unref (query);

  return ret;
}

/**
 * gst_pad_query_peer_position:
 * @pad: a #GstPad on whose peer to invoke the position query on.
 *       Must be a sink pad.
 * @format: (inout): a pointer to the #GstFormat asked for.
 *          On return contains the #GstFormat used.
 * @cur: (out) (allow-none): a location in which to store the current
 *     position, or NULL.
 *
 * Queries the peer of a given sink pad for the stream position.
 *
 * Returns: TRUE if the query could be performed.
 */
gboolean
gst_pad_query_peer_position (GstPad * pad, GstFormat * format, gint64 * cur)
{
  gboolean ret = FALSE;
  GstPad *peer;

  g_return_val_if_fail (GST_IS_PAD (pad), FALSE);
  g_return_val_if_fail (GST_PAD_IS_SINK (pad), FALSE);
  g_return_val_if_fail (format != NULL, FALSE);

  peer = gst_pad_get_peer (pad);
  if (peer) {
    ret = gst_pad_query_position (peer, format, cur);
    gst_object_unref (peer);
  }

  return ret;
}

/**
 * gst_pad_query_duration:
 * @pad: a #GstPad to invoke the duration query on.
 * @format: (inout): a pointer to the #GstFormat asked for.
 *          On return contains the #GstFormat used.
 * @duration: (out) (allow-none): a location in which to store the total
 *     duration, or NULL.
 *
 * Queries a pad for the total stream duration.
 *
 * Returns: TRUE if the query could be performed.
 */
gboolean
gst_pad_query_duration (GstPad * pad, GstFormat * format, gint64 * duration)
{
  GstQuery *query;
  gboolean ret;

  g_return_val_if_fail (GST_IS_PAD (pad), FALSE);
  g_return_val_if_fail (format != NULL, FALSE);

  query = gst_query_new_duration (*format);
  ret = gst_pad_query (pad, query);

  if (ret)
    gst_query_parse_duration (query, format, duration);

  gst_query_unref (query);

  return ret;
}

/**
 * gst_pad_query_peer_duration:
 * @pad: a #GstPad on whose peer pad to invoke the duration query on.
 *       Must be a sink pad.
 * @format: (inout) :a pointer to the #GstFormat asked for.
 *          On return contains the #GstFormat used.
 * @duration: (out) (allow-none): a location in which to store the total
 *     duration, or NULL.
 *
 * Queries the peer pad of a given sink pad for the total stream duration.
 *
 * Returns: TRUE if the query could be performed.
 */
gboolean
gst_pad_query_peer_duration (GstPad * pad, GstFormat * format,
    gint64 * duration)
{
  gboolean ret = FALSE;
  GstPad *peer;

  g_return_val_if_fail (GST_IS_PAD (pad), FALSE);
  g_return_val_if_fail (GST_PAD_IS_SINK (pad), FALSE);
  g_return_val_if_fail (format != NULL, FALSE);

  peer = gst_pad_get_peer (pad);
  if (peer) {
    ret = gst_pad_query_duration (peer, format, duration);
    gst_object_unref (peer);
  }

  return ret;
}

/**
 * gst_pad_query_convert:
 * @pad: a #GstPad to invoke the convert query on.
 * @src_format: a #GstFormat to convert from.
 * @src_val: a value to convert.
 * @dest_format: (inout): a pointer to the #GstFormat to convert to.
 * @dest_val: (out): a pointer to the result.
 *
 * Queries a pad to convert @src_val in @src_format to @dest_format.
 *
 * Returns: TRUE if the query could be performed.
 */
gboolean
gst_pad_query_convert (GstPad * pad, GstFormat src_format, gint64 src_val,
    GstFormat * dest_format, gint64 * dest_val)
{
  GstQuery *query;
  gboolean ret;

  g_return_val_if_fail (GST_IS_PAD (pad), FALSE);
  g_return_val_if_fail (dest_format != NULL, FALSE);
  g_return_val_if_fail (dest_val != NULL, FALSE);

  if (*dest_format == src_format || src_val == -1) {
    *dest_val = src_val;
    return TRUE;
  }

  query = gst_query_new_convert (src_format, src_val, *dest_format);
  ret = gst_pad_query (pad, query);

  if (ret)
    gst_query_parse_convert (query, NULL, NULL, dest_format, dest_val);

  gst_query_unref (query);

  return ret;
}

/**
 * gst_pad_query_peer_convert:
 * @pad: a #GstPad, on whose peer pad to invoke the convert query on.
 *       Must be a sink pad.
 * @src_format: a #GstFormat to convert from.
 * @src_val: a value to convert.
 * @dest_format: (inout): a pointer to the #GstFormat to convert to.
 * @dest_val: (out): a pointer to the result.
 *
 * Queries the peer pad of a given sink pad to convert @src_val in @src_format
 * to @dest_format.
 *
 * Returns: TRUE if the query could be performed.
 */
gboolean
gst_pad_query_peer_convert (GstPad * pad, GstFormat src_format, gint64 src_val,
    GstFormat * dest_format, gint64 * dest_val)
{
  gboolean ret = FALSE;
  GstPad *peer;

  g_return_val_if_fail (GST_IS_PAD (pad), FALSE);
  g_return_val_if_fail (GST_PAD_IS_SINK (pad), FALSE);
  g_return_val_if_fail (dest_format != NULL, FALSE);
  g_return_val_if_fail (dest_val != NULL, FALSE);

  peer = gst_pad_get_peer (pad);
  if (peer) {
    ret = gst_pad_query_convert (peer, src_format, src_val, dest_format,
        dest_val);
    gst_object_unref (peer);
  }

  return ret;
}

/**
 * gst_atomic_int_set:
 * @atomic_int: (inout): pointer to an atomic integer
 * @value: value to set
 *
 * Unconditionally sets the atomic integer to @value.
 *
 * Deprecated: Use g_atomic_int_set().
 *
 */
#ifndef GST_REMOVE_DEPRECATED
#ifdef GST_DISABLE_DEPRECATED
void gst_atomic_int_set (gint * atomic_int, gint value);
#endif
void
gst_atomic_int_set (gint * atomic_int, gint value)
{
  g_atomic_int_set (atomic_int, value);
}
#endif

/**
 * gst_pad_add_data_probe:
 * @pad: pad to add the data probe handler to
 * @handler: function to call when data is passed over pad
 * @data: (closure): data to pass along with the handler
 *
 * Adds a "data probe" to a pad. This function will be called whenever data
 * passes through a pad. In this case data means both events and buffers. The
 * probe will be called with the data as an argument, meaning @handler should
 * have the same callback signature as the #GstPad::have-data signal.
 * Note that the data will have a reference count greater than 1, so it will
 * be immutable -- you must not change it.
 *
 * For source pads, the probe will be called after the blocking function, if any
 * (see gst_pad_set_blocked_async()), but before looking up the peer to chain
 * to. For sink pads, the probe function will be called before configuring the
 * sink with new caps, if any, and before calling the pad's chain function.
 *
 * Your data probe should return TRUE to let the data continue to flow, or FALSE
 * to drop it. Dropping data is rarely useful, but occasionally comes in handy
 * with events.
 *
 * Although probes are implemented internally by connecting @handler to the
 * have-data signal on the pad, if you want to remove a probe it is insufficient
 * to only call g_signal_handler_disconnect on the returned handler id. To
 * remove a probe, use the appropriate function, such as
 * gst_pad_remove_data_probe().
 *
 * Returns: The handler id.
 */
gulong
gst_pad_add_data_probe (GstPad * pad, GCallback handler, gpointer data)
{
  return gst_pad_add_data_probe_full (pad, handler, data, NULL);
}

/**
 * gst_pad_add_data_probe_full:
 * @pad: pad to add the data probe handler to
 * @handler: function to call when data is passed over pad
 * @data: (closure): data to pass along with the handler
 * @notify: (allow-none): function to call when the probe is disconnected,
 *     or NULL
 *
 * Adds a "data probe" to a pad. This function will be called whenever data
 * passes through a pad. In this case data means both events and buffers. The
 * probe will be called with the data as an argument, meaning @handler should
 * have the same callback signature as the #GstPad::have-data signal.
 * Note that the data will have a reference count greater than 1, so it will
 * be immutable -- you must not change it.
 *
 * For source pads, the probe will be called after the blocking function, if any
 * (see gst_pad_set_blocked_async()), but before looking up the peer to chain
 * to. For sink pads, the probe function will be called before configuring the
 * sink with new caps, if any, and before calling the pad's chain function.
 *
 * Your data probe should return TRUE to let the data continue to flow, or FALSE
 * to drop it. Dropping data is rarely useful, but occasionally comes in handy
 * with events.
 *
 * Although probes are implemented internally by connecting @handler to the
 * have-data signal on the pad, if you want to remove a probe it is insufficient
 * to only call g_signal_handler_disconnect on the returned handler id. To
 * remove a probe, use the appropriate function, such as
 * gst_pad_remove_data_probe().
 *
 * The @notify function is called when the probe is disconnected and usually
 * used to free @data.
 *
 * Returns: The handler id.
 *
 * Since: 0.10.20
 */
gulong
gst_pad_add_data_probe_full (GstPad * pad, GCallback handler,
    gpointer data, GDestroyNotify notify)
{
  gulong sigid;

  g_return_val_if_fail (GST_IS_PAD (pad), 0);
  g_return_val_if_fail (handler != NULL, 0);

  GST_OBJECT_LOCK (pad);

  /* we only expose a GDestroyNotify in our API because that's less confusing */
  sigid = g_signal_connect_data (pad, "have-data", handler, data,
      (GClosureNotify) notify, 0);

  GST_PAD_DO_EVENT_SIGNALS (pad)++;
  GST_PAD_DO_BUFFER_SIGNALS (pad)++;
  GST_CAT_DEBUG_OBJECT (GST_CAT_PADS, pad,
      "adding data probe, now %d data, %d event probes",
      GST_PAD_DO_BUFFER_SIGNALS (pad), GST_PAD_DO_EVENT_SIGNALS (pad));
  _priv_gst_pad_invalidate_cache (pad);
  GST_OBJECT_UNLOCK (pad);

  return sigid;
}

/**
 * gst_pad_add_event_probe:
 * @pad: pad to add the event probe handler to
 * @handler: function to call when events are passed over pad
 * @data: (closure): data to pass along with the handler
 *
 * Adds a probe that will be called for all events passing through a pad. See
 * gst_pad_add_data_probe() for more information.
 *
 * Returns: The handler id
 */
gulong
gst_pad_add_event_probe (GstPad * pad, GCallback handler, gpointer data)
{
  return gst_pad_add_event_probe_full (pad, handler, data, NULL);
}

/**
 * gst_pad_add_event_probe_full:
 * @pad: pad to add the event probe handler to
 * @handler: function to call when events are passed over pad
 * @data: (closure): data to pass along with the handler, or NULL
 * @notify: (allow-none): function to call when probe is disconnected, or NULL
 *
 * Adds a probe that will be called for all events passing through a pad. See
 * gst_pad_add_data_probe() for more information.
 *
 * The @notify function is called when the probe is disconnected and usually
 * used to free @data.
 *
 * Returns: The handler id
 *
 * Since: 0.10.20
 */
gulong
gst_pad_add_event_probe_full (GstPad * pad, GCallback handler,
    gpointer data, GDestroyNotify notify)
{
  gulong sigid;

  g_return_val_if_fail (GST_IS_PAD (pad), 0);
  g_return_val_if_fail (handler != NULL, 0);

  GST_OBJECT_LOCK (pad);

  /* we only expose a GDestroyNotify in our API because that's less confusing */
  sigid = g_signal_connect_data (pad, "have-data::event", handler, data,
      (GClosureNotify) notify, 0);

  GST_PAD_DO_EVENT_SIGNALS (pad)++;
  GST_CAT_DEBUG_OBJECT (GST_CAT_PADS, pad, "adding event probe, now %d probes",
      GST_PAD_DO_EVENT_SIGNALS (pad));
  _priv_gst_pad_invalidate_cache (pad);
  GST_OBJECT_UNLOCK (pad);

  return sigid;
}

/**
 * gst_pad_add_buffer_probe:
 * @pad: pad to add the buffer probe handler to
 * @handler: function to call when buffers are passed over pad
 * @data: (closure): data to pass along with the handler
 *
 * Adds a probe that will be called for all buffers passing through a pad. See
 * gst_pad_add_data_probe() for more information.
 *
 * Returns: The handler id
 */
gulong
gst_pad_add_buffer_probe (GstPad * pad, GCallback handler, gpointer data)
{
  return gst_pad_add_buffer_probe_full (pad, handler, data, NULL);
}

/**
 * gst_pad_add_buffer_probe_full:
 * @pad: pad to add the buffer probe handler to
 * @handler: function to call when buffer are passed over pad
 * @data: (closure): data to pass along with the handler
 * @notify: (allow-none): function to call when the probe is disconnected,
 *     or NULL
 *
 * Adds a probe that will be called for all buffers passing through a pad. See
 * gst_pad_add_data_probe() for more information.
 *
 * The @notify function is called when the probe is disconnected and usually
 * used to free @data.
 *
 * Returns: The handler id
 *
 * Since: 0.10.20
 */
gulong
gst_pad_add_buffer_probe_full (GstPad * pad, GCallback handler,
    gpointer data, GDestroyNotify notify)
{
  gulong sigid;

  g_return_val_if_fail (GST_IS_PAD (pad), 0);
  g_return_val_if_fail (handler != NULL, 0);

  GST_OBJECT_LOCK (pad);

  /* we only expose a GDestroyNotify in our API because that's less confusing */
  sigid = g_signal_connect_data (pad, "have-data::buffer", handler, data,
      (GClosureNotify) notify, 0);

  GST_PAD_DO_BUFFER_SIGNALS (pad)++;
  GST_CAT_DEBUG_OBJECT (GST_CAT_PADS, pad, "adding buffer probe, now %d probes",
      GST_PAD_DO_BUFFER_SIGNALS (pad));
  _priv_gst_pad_invalidate_cache (pad);
  GST_OBJECT_UNLOCK (pad);

  return sigid;
}

/**
 * gst_pad_remove_data_probe:
 * @pad: pad to remove the data probe handler from
 * @handler_id: handler id returned from gst_pad_add_data_probe
 *
 * Removes a data probe from @pad.
 */
void
gst_pad_remove_data_probe (GstPad * pad, guint handler_id)
{
  g_return_if_fail (GST_IS_PAD (pad));
  g_return_if_fail (handler_id > 0);

  GST_OBJECT_LOCK (pad);
  g_signal_handler_disconnect (pad, handler_id);
  GST_PAD_DO_BUFFER_SIGNALS (pad)--;
  GST_PAD_DO_EVENT_SIGNALS (pad)--;
  GST_CAT_DEBUG_OBJECT (GST_CAT_PADS, pad,
      "removed data probe, now %d event, %d buffer probes",
      GST_PAD_DO_EVENT_SIGNALS (pad), GST_PAD_DO_BUFFER_SIGNALS (pad));
  GST_OBJECT_UNLOCK (pad);

}

/**
 * gst_pad_remove_event_probe:
 * @pad: pad to remove the event probe handler from
 * @handler_id: handler id returned from gst_pad_add_event_probe
 *
 * Removes an event probe from @pad.
 */
void
gst_pad_remove_event_probe (GstPad * pad, guint handler_id)
{
  g_return_if_fail (GST_IS_PAD (pad));
  g_return_if_fail (handler_id > 0);

  GST_OBJECT_LOCK (pad);
  g_signal_handler_disconnect (pad, handler_id);
  GST_PAD_DO_EVENT_SIGNALS (pad)--;
  GST_CAT_DEBUG_OBJECT (GST_CAT_PADS, pad,
      "removed event probe, now %d event probes",
      GST_PAD_DO_EVENT_SIGNALS (pad));
  GST_OBJECT_UNLOCK (pad);
}

/**
 * gst_pad_remove_buffer_probe:
 * @pad: pad to remove the buffer probe handler from
 * @handler_id: handler id returned from gst_pad_add_buffer_probe
 *
 * Removes a buffer probe from @pad.
 */
void
gst_pad_remove_buffer_probe (GstPad * pad, guint handler_id)
{
  g_return_if_fail (GST_IS_PAD (pad));
  g_return_if_fail (handler_id > 0);

  GST_OBJECT_LOCK (pad);
  g_signal_handler_disconnect (pad, handler_id);
  GST_PAD_DO_BUFFER_SIGNALS (pad)--;
  GST_CAT_DEBUG_OBJECT (GST_CAT_PADS, pad,
      "removed buffer probe, now %d buffer probes",
      GST_PAD_DO_BUFFER_SIGNALS (pad));
  GST_OBJECT_UNLOCK (pad);

}

/**
 * gst_element_found_tags_for_pad:
 * @element: element for which to post taglist to bus.
 * @pad: (transfer none): pad on which to push tag-event
 * @list: (transfer full): the taglist to post on the bus and create event from
 *
 * Posts a message to the bus that new tags were found and pushes the
 * tags as event. Takes ownership of the @list.
 *
 * This is a utility method for elements. Applications should use the
 * #GstTagSetter interface.
 */
void
gst_element_found_tags_for_pad (GstElement * element,
    GstPad * pad, GstTagList * list)
{
  g_return_if_fail (element != NULL);
  g_return_if_fail (pad != NULL);
  g_return_if_fail (list != NULL);

  gst_pad_push_event (pad, gst_event_new_tag (gst_tag_list_copy (list)));
  /* FIXME 0.11: Set the pad as source. */
  gst_element_post_message (element,
      gst_message_new_tag_full (GST_OBJECT (element), pad, list));
}

static void
push_and_ref (GstPad * pad, GstEvent * event)
{
  gst_pad_push_event (pad, gst_event_ref (event));
  /* iterator refs pad, we unref when we are done with it */
  gst_object_unref (pad);
}

/**
 * gst_element_found_tags:
 * @element: element for which we found the tags.
 * @list: (transfer full): list of tags.
 *
 * Posts a message to the bus that new tags were found, and pushes an event
 * to all sourcepads. Takes ownership of the @list.
 *
 * This is a utility method for elements. Applications should use the
 * #GstTagSetter interface.
 */
void
gst_element_found_tags (GstElement * element, GstTagList * list)
{
  GstIterator *iter;
  GstEvent *event;

  g_return_if_fail (element != NULL);
  g_return_if_fail (list != NULL);

  iter = gst_element_iterate_src_pads (element);
  event = gst_event_new_tag (gst_tag_list_copy (list));
  gst_iterator_foreach (iter, (GFunc) push_and_ref, event);
  gst_iterator_free (iter);
  gst_event_unref (event);

  gst_element_post_message (element,
      gst_message_new_tag (GST_OBJECT (element), list));
}

static GstPad *
element_find_unlinked_pad (GstElement * element, GstPadDirection direction)
{
  GstIterator *iter;
  GstPad *unlinked_pad = NULL;
  gboolean done;

  switch (direction) {
    case GST_PAD_SRC:
      iter = gst_element_iterate_src_pads (element);
      break;
    case GST_PAD_SINK:
      iter = gst_element_iterate_sink_pads (element);
      break;
    default:
      g_return_val_if_reached (NULL);
  }

  done = FALSE;
  while (!done) {
    gpointer pad;

    switch (gst_iterator_next (iter, &pad)) {
      case GST_ITERATOR_OK:{
        GstPad *peer;

        GST_CAT_LOG (GST_CAT_ELEMENT_PADS, "examining pad %s:%s",
            GST_DEBUG_PAD_NAME (pad));

        peer = gst_pad_get_peer (GST_PAD (pad));
        if (peer == NULL) {
          unlinked_pad = pad;
          done = TRUE;
          GST_CAT_DEBUG (GST_CAT_ELEMENT_PADS,
              "found existing unlinked pad %s:%s",
              GST_DEBUG_PAD_NAME (unlinked_pad));
        } else {
          gst_object_unref (pad);
          gst_object_unref (peer);
        }
        break;
      }
      case GST_ITERATOR_DONE:
        done = TRUE;
        break;
      case GST_ITERATOR_RESYNC:
        gst_iterator_resync (iter);
        break;
      case GST_ITERATOR_ERROR:
        g_return_val_if_reached (NULL);
        break;
    }
  }

  gst_iterator_free (iter);

  return unlinked_pad;
}

/**
 * gst_bin_find_unlinked_pad:
 * @bin: bin in which to look for elements with unlinked pads
 * @direction: whether to look for an unlinked source or sink pad
 *
 * Recursively looks for elements with an unlinked pad of the given
 * direction within the specified bin and returns an unlinked pad
 * if one is found, or NULL otherwise. If a pad is found, the caller
 * owns a reference to it and should use gst_object_unref() on the
 * pad when it is not needed any longer.
 *
 * Returns: (transfer full): unlinked pad of the given direction, or NULL.
 *
 * Since: 0.10.20
 */
GstPad *
gst_bin_find_unlinked_pad (GstBin * bin, GstPadDirection direction)
{
  GstIterator *iter;
  gboolean done;
  GstPad *pad = NULL;

  g_return_val_if_fail (GST_IS_BIN (bin), NULL);
  g_return_val_if_fail (direction != GST_PAD_UNKNOWN, NULL);

  done = FALSE;
  iter = gst_bin_iterate_recurse (bin);
  while (!done) {
    gpointer element;

    switch (gst_iterator_next (iter, &element)) {
      case GST_ITERATOR_OK:
        pad = element_find_unlinked_pad (GST_ELEMENT (element), direction);
        gst_object_unref (element);
        if (pad != NULL)
          done = TRUE;
        break;
      case GST_ITERATOR_DONE:
        done = TRUE;
        break;
      case GST_ITERATOR_RESYNC:
        gst_iterator_resync (iter);
        break;
      case GST_ITERATOR_ERROR:
        g_return_val_if_reached (NULL);
        break;
    }
  }

  gst_iterator_free (iter);

  return pad;
}

/**
 * gst_bin_find_unconnected_pad:
 * @bin: bin in which to look for elements with unlinked pads
 * @direction: whether to look for an unlinked source or sink pad
 *
 * Recursively looks for elements with an unlinked pad of the given
 * direction within the specified bin and returns an unlinked pad
 * if one is found, or NULL otherwise. If a pad is found, the caller
 * owns a reference to it and should use gst_object_unref() on the
 * pad when it is not needed any longer.
 *
 * Returns: (transfer full): unlinked pad of the given direction, or NULL.
 *
 * Since: 0.10.3
 *
 * Deprecated: use gst_bin_find_unlinked_pad() instead.
 */
#ifndef GST_REMOVE_DEPRECATED
#ifdef GST_DISABLE_DEPRECATED
GstPad *gst_bin_find_unconnected_pad (GstBin * bin, GstPadDirection direction);
#endif
GstPad *
gst_bin_find_unconnected_pad (GstBin * bin, GstPadDirection direction)
{
  return gst_bin_find_unlinked_pad (bin, direction);
}
#endif

/**
 * gst_parse_bin_from_description:
 * @bin_description: command line describing the bin
 * @ghost_unlinked_pads: whether to automatically create ghost pads
 *     for unlinked source or sink pads within the bin
 * @err: where to store the error message in case of an error, or NULL
 *
 * This is a convenience wrapper around gst_parse_launch() to create a
 * #GstBin from a gst-launch-style pipeline description. See
 * gst_parse_launch() and the gst-launch man page for details about the
 * syntax. Ghost pads on the bin for unlinked source or sink pads
 * within the bin can automatically be created (but only a maximum of
 * one ghost pad for each direction will be created; if you expect
 * multiple unlinked source pads or multiple unlinked sink pads
 * and want them all ghosted, you will have to create the ghost pads
 * yourself).
 *
 * Returns: (transfer full): a newly-created bin, or NULL if an error occurred.
 *
 * Since: 0.10.3
 */
GstElement *
gst_parse_bin_from_description (const gchar * bin_description,
    gboolean ghost_unlinked_pads, GError ** err)
{
  return gst_parse_bin_from_description_full (bin_description,
      ghost_unlinked_pads, NULL, 0, err);
}

/**
 * gst_parse_bin_from_description_full:
 * @bin_description: command line describing the bin
 * @ghost_unlinked_pads: whether to automatically create ghost pads
 *     for unlinked source or sink pads within the bin
 * @context: (transfer none) (allow-none): a parse context allocated with
 *     gst_parse_context_new(), or %NULL
 * @flags: parsing options, or #GST_PARSE_FLAG_NONE
 * @err: where to store the error message in case of an error, or NULL
 *
 * This is a convenience wrapper around gst_parse_launch() to create a
 * #GstBin from a gst-launch-style pipeline description. See
 * gst_parse_launch() and the gst-launch man page for details about the
 * syntax. Ghost pads on the bin for unlinked source or sink pads
 * within the bin can automatically be created (but only a maximum of
 * one ghost pad for each direction will be created; if you expect
 * multiple unlinked source pads or multiple unlinked sink pads
 * and want them all ghosted, you will have to create the ghost pads
 * yourself).
 *
 * Returns: (transfer full): a newly-created bin, or NULL if an error occurred.
 *
 * Since: 0.10.20
 */
#ifndef GSTREAMER_LITE
GstElement *
gst_parse_bin_from_description_full (const gchar * bin_description,
    gboolean ghost_unlinked_pads, GstParseContext * context,
    GstParseFlags flags, GError ** err)
{
#ifndef GST_DISABLE_PARSE
  GstPad *pad = NULL;
  GstBin *bin;
  gchar *desc;

  g_return_val_if_fail (bin_description != NULL, NULL);
  g_return_val_if_fail (err == NULL || *err == NULL, NULL);

  GST_DEBUG ("Making bin from description '%s'", bin_description);

  /* parse the pipeline to a bin */
  desc = g_strdup_printf ("bin.( %s )", bin_description);
  bin = (GstBin *) gst_parse_launch_full (desc, context, flags, err);
  g_free (desc);

  if (bin == NULL || (err && *err != NULL)) {
    if (bin)
      gst_object_unref (bin);
    return NULL;
  }

  /* find pads and ghost them if necessary */
  if (ghost_unlinked_pads) {
    if ((pad = gst_bin_find_unlinked_pad (bin, GST_PAD_SRC))) {
      gst_element_add_pad (GST_ELEMENT (bin), gst_ghost_pad_new ("src", pad));
      gst_object_unref (pad);
    }
    if ((pad = gst_bin_find_unlinked_pad (bin, GST_PAD_SINK))) {
      gst_element_add_pad (GST_ELEMENT (bin), gst_ghost_pad_new ("sink", pad));
      gst_object_unref (pad);
    }
  }

  return GST_ELEMENT (bin);
#else
  gchar *msg;

  GST_WARNING ("Disabled API called");

  msg = gst_error_get_message (GST_CORE_ERROR, GST_CORE_ERROR_DISABLED);
  g_set_error (err, GST_CORE_ERROR, GST_CORE_ERROR_DISABLED, "%s", msg);
  g_free (msg);

  return NULL;
#endif
}
#else // GSTREAMER_LITE
GstElement *
gst_parse_bin_from_description_full (const gchar * bin_description,
                                     gboolean ghost_unlinked_pads, GstParseContext * context,
                                     GstParseFlags flags, GError ** err)
{
	gchar *msg;
	
	GST_WARNING ("Disabled API called");
	
	msg = gst_error_get_message (GST_CORE_ERROR, GST_CORE_ERROR_DISABLED);
	g_set_error (err, GST_CORE_ERROR, GST_CORE_ERROR_DISABLED, "%s", msg);
	g_free (msg);
	
	return NULL;
}
#endif // GSTREAMER_LITE

/**
 * gst_type_register_static_full:
 * @parent_type: The GType of the parent type the newly registered type will
 *   derive from
 * @type_name: NULL-terminated string used as the name of the new type
 * @class_size: Size of the class structure.
 * @base_init: Location of the base initialization function (optional).
 * @base_finalize: Location of the base finalization function (optional).
 * @class_init: Location of the class initialization function for class types
 *   Location of the default vtable inititalization function for interface
 *   types. (optional)
 * @class_finalize: Location of the class finalization function for class types.
 *   Location of the default vtable finalization function for interface types.
 *   (optional)
 * @class_data: User-supplied data passed to the class init/finalize functions.
 * @instance_size: Size of the instance (object) structure (required for
 *   instantiatable types only).
 * @n_preallocs: The number of pre-allocated (cached) instances to reserve
 *   memory for (0 indicates no caching). Ignored on recent GLib's.
 * @instance_init: Location of the instance initialization function (optional,
 *   for instantiatable types only).
 * @value_table: A GTypeValueTable function table for generic handling of
 *   GValues of this type (usually only useful for fundamental types).
 * @flags: #GTypeFlags for this GType. E.g: G_TYPE_FLAG_ABSTRACT
 *
 * Helper function which constructs a #GTypeInfo structure and registers a
 * GType, but which generates less linker overhead than a static const
 * #GTypeInfo structure. For further details of the parameters, please see
 * #GTypeInfo in the GLib documentation.
 *
 * Registers type_name as the name of a new static type derived from
 * parent_type. The value of flags determines the nature (e.g. abstract or
 * not) of the type. It works by filling a GTypeInfo struct and calling
 * g_type_register_static().
 *
 * Returns: A #GType for the newly-registered type.
 *
 * Since: 0.10.14
 */
GType
gst_type_register_static_full (GType parent_type,
    const gchar * type_name,
    guint class_size,
    GBaseInitFunc base_init,
    GBaseFinalizeFunc base_finalize,
    GClassInitFunc class_init,
    GClassFinalizeFunc class_finalize,
    gconstpointer class_data,
    guint instance_size,
    guint16 n_preallocs,
    GInstanceInitFunc instance_init,
    const GTypeValueTable * value_table, GTypeFlags flags)
{
  GTypeInfo info;

  info.class_size = class_size;
  info.base_init = base_init;
  info.base_finalize = base_finalize;
  info.class_init = class_init;
  info.class_finalize = class_finalize;
  info.class_data = class_data;
  info.instance_size = instance_size;
  info.n_preallocs = n_preallocs;
  info.instance_init = instance_init;
  info.value_table = value_table;

  return g_type_register_static (parent_type, type_name, &info, flags);
}


/**
 * gst_util_get_timestamp:
 *
 * Get a timestamp as GstClockTime to be used for interval meassurements.
 * The timestamp should not be interpreted in any other way.
 *
 * Returns: the timestamp
 *
 * Since: 0.10.16
 */
GstClockTime
gst_util_get_timestamp (void)
{
#if defined (HAVE_POSIX_TIMERS) && defined(HAVE_MONOTONIC_CLOCK)
  struct timespec now;

  clock_gettime (CLOCK_MONOTONIC, &now);
  return GST_TIMESPEC_TO_TIME (now);
#else
  GTimeVal now;

  g_get_current_time (&now);
  return GST_TIMEVAL_TO_TIME (now);
#endif
}

/**
 * gst_util_array_binary_search:
 * @array: the sorted input array
 * @num_elements: number of elements in the array
 * @element_size: size of every element in bytes
 * @search_func: (scope call): function to compare two elements, @search_data will always be passed as second argument
 * @mode: search mode that should be used
 * @search_data: element that should be found
 * @user_data: (closure): data to pass to @search_func
 *
 * Searches inside @array for @search_data by using the comparison function
 * @search_func. @array must be sorted ascending.
 *
 * As @search_data is always passed as second argument to @search_func it's
 * not required that @search_data has the same type as the array elements.
 *
 * The complexity of this search function is O(log (num_elements)).
 *
 * Returns: (transfer none): The address of the found element or %NULL if nothing was found
 *
 * Since: 0.10.23
 */
gpointer
gst_util_array_binary_search (gpointer array, guint num_elements,
    gsize element_size, GCompareDataFunc search_func, GstSearchMode mode,
    gconstpointer search_data, gpointer user_data)
{
  glong left = 0, right = num_elements - 1, m;
  gint ret;
  guint8 *data = (guint8 *) array;

  g_return_val_if_fail (array != NULL, NULL);
  g_return_val_if_fail (element_size > 0, NULL);
  g_return_val_if_fail (search_func != NULL, NULL);

  /* 0. No elements => return NULL */
  if (num_elements == 0)
    return NULL;

  /* 1. If search_data is before the 0th element return the 0th element */
  ret = search_func (data, search_data, user_data);
  if ((ret >= 0 && mode == GST_SEARCH_MODE_AFTER) || ret == 0)
    return data;
  else if (ret > 0)
    return NULL;

  /* 2. If search_data is after the last element return the last element */
  ret =
      search_func (data + (num_elements - 1) * element_size, search_data,
      user_data);
  if ((ret <= 0 && mode == GST_SEARCH_MODE_BEFORE) || ret == 0)
    return data + (num_elements - 1) * element_size;
  else if (ret < 0)
    return NULL;

  /* 3. else binary search */
  while (TRUE) {
    m = left + (right - left) / 2;

    ret = search_func (data + m * element_size, search_data, user_data);

    if (ret == 0) {
      return data + m * element_size;
    } else if (ret < 0) {
      left = m + 1;
    } else {
      right = m - 1;
    }

    /* No exact match found */
    if (right < left) {
      if (mode == GST_SEARCH_MODE_EXACT) {
        return NULL;
      } else if (mode == GST_SEARCH_MODE_AFTER) {
        if (ret < 0)
          return (m < num_elements) ? data + (m + 1) * element_size : NULL;
        else
          return data + m * element_size;
      } else {
        if (ret < 0)
          return data + m * element_size;
        else
          return (m > 0) ? data + (m - 1) * element_size : NULL;
      }
    }
  }
}

/* Finds the greatest common divisor.
 * Returns 1 if none other found.
 * This is Euclid's algorithm. */

/**
 * gst_util_greatest_common_divisor:
 * @a: First value as #gint
 * @b: Second value as #gint
 *
 * Calculates the greatest common divisor of @a
 * and @b.
 *
 * Returns: Greatest common divisor of @a and @b
 *
 * Since: 0.10.26
 */
gint
gst_util_greatest_common_divisor (gint a, gint b)
{
  while (b != 0) {
    int temp = a;

    a = b;
    b = temp % b;
  }

  return ABS (a);
}

/**
 * gst_util_fraction_to_double:
 * @src_n: Fraction numerator as #gint
 * @src_d: Fraction denominator #gint
 * @dest: (out): pointer to a #gdouble for the result
 *
 * Transforms a fraction to a #gdouble.
 *
 * Since: 0.10.26
 */
void
gst_util_fraction_to_double (gint src_n, gint src_d, gdouble * dest)
{
  g_return_if_fail (dest != NULL);
  g_return_if_fail (src_d != 0);

  *dest = ((gdouble) src_n) / ((gdouble) src_d);
}

#define MAX_TERMS       30
#define MIN_DIVISOR     1.0e-10
#define MAX_ERROR       1.0e-20

/* use continued fractions to transform a double into a fraction,
 * see http://mathforum.org/dr.math/faq/faq.fractions.html#decfrac.
 * This algorithm takes care of overflows.
 */

/**
 * gst_util_double_to_fraction:
 * @src: #gdouble to transform
 * @dest_n: (out): pointer to a #gint to hold the result numerator
 * @dest_d: (out): pointer to a #gint to hold the result denominator
 *
 * Transforms a #gdouble to a fraction and simplifies
 * the result.
 *
 * Since: 0.10.26
 */
void
gst_util_double_to_fraction (gdouble src, gint * dest_n, gint * dest_d)
{

  gdouble V, F;                 /* double being converted */
  gint N, D;                    /* will contain the result */
  gint A;                       /* current term in continued fraction */
  gint64 N1, D1;                /* numerator, denominator of last approx */
  gint64 N2, D2;                /* numerator, denominator of previous approx */
  gint i;
  gint gcd;
  gboolean negative = FALSE;

  g_return_if_fail (dest_n != NULL);
  g_return_if_fail (dest_d != NULL);

  /* initialize fraction being converted */
  F = src;
  if (F < 0.0) {
    F = -F;
    negative = TRUE;
  }

  V = F;
  /* initialize fractions with 1/0, 0/1 */
  N1 = 1;
  D1 = 0;
  N2 = 0;
  D2 = 1;
  N = 1;
  D = 1;

  for (i = 0; i < MAX_TERMS; i++) {
    /* get next term */
    A = (gint) F;               /* no floor() needed, F is always >= 0 */
    /* get new divisor */
    F = F - A;

    /* calculate new fraction in temp */
    N2 = N1 * A + N2;
    D2 = D1 * A + D2;

    /* guard against overflow */
    if (N2 > G_MAXINT || D2 > G_MAXINT) {
      break;
    }

    N = N2;
    D = D2;

    /* save last two fractions */
    N2 = N1;
    D2 = D1;
    N1 = N;
    D1 = D;

    /* quit if dividing by zero or close enough to target */
    if (F < MIN_DIVISOR || fabs (V - ((gdouble) N) / D) < MAX_ERROR) {
      break;
    }

    /* Take reciprocal */
    F = 1 / F;
  }
  /* fix for overflow */
  if (D == 0) {
    N = G_MAXINT;
    D = 1;
  }
  /* fix for negative */
  if (negative)
    N = -N;

  /* simplify */
  gcd = gst_util_greatest_common_divisor (N, D);
  if (gcd) {
    N /= gcd;
    D /= gcd;
  }

  /* set results */
  *dest_n = N;
  *dest_d = D;
}

/**
 * gst_util_fraction_multiply:
 * @a_n: Numerator of first value
 * @a_d: Denominator of first value
 * @b_n: Numerator of second value
 * @b_d: Denominator of second value
 * @res_n: (out): Pointer to #gint to hold the result numerator
 * @res_d: (out): Pointer to #gint to hold the result denominator
 *
 * Multiplies the fractions @a_n/@a_d and @b_n/@b_d and stores
 * the result in @res_n and @res_d.
 *
 * Returns: %FALSE on overflow, %TRUE otherwise.
 *
 * Since: 0.10.26
 */
gboolean
gst_util_fraction_multiply (gint a_n, gint a_d, gint b_n, gint b_d,
    gint * res_n, gint * res_d)
{
  gint gcd;

  g_return_val_if_fail (res_n != NULL, FALSE);
  g_return_val_if_fail (res_d != NULL, FALSE);
  g_return_val_if_fail (a_d != 0, FALSE);
  g_return_val_if_fail (b_d != 0, FALSE);

  gcd = gst_util_greatest_common_divisor (a_n, a_d);
  a_n /= gcd;
  a_d /= gcd;

  gcd = gst_util_greatest_common_divisor (b_n, b_d);
  b_n /= gcd;
  b_d /= gcd;

  gcd = gst_util_greatest_common_divisor (a_n, b_d);
  a_n /= gcd;
  b_d /= gcd;

  gcd = gst_util_greatest_common_divisor (a_d, b_n);
  a_d /= gcd;
  b_n /= gcd;

  /* This would result in overflow */
  if (a_n != 0 && G_MAXINT / ABS (a_n) < ABS (b_n))
    return FALSE;
  if (G_MAXINT / ABS (a_d) < ABS (b_d))
    return FALSE;

  *res_n = a_n * b_n;
  *res_d = a_d * b_d;

  gcd = gst_util_greatest_common_divisor (*res_n, *res_d);
  *res_n /= gcd;
  *res_d /= gcd;

  return TRUE;
}

/**
 * gst_util_fraction_add:
 * @a_n: Numerator of first value
 * @a_d: Denominator of first value
 * @b_n: Numerator of second value
 * @b_d: Denominator of second value
 * @res_n: (out): Pointer to #gint to hold the result numerator
 * @res_d: (out): Pointer to #gint to hold the result denominator
 *
 * Adds the fractions @a_n/@a_d and @b_n/@b_d and stores
 * the result in @res_n and @res_d.
 *
 * Returns: %FALSE on overflow, %TRUE otherwise.
 *
 * Since: 0.10.26
 */
gboolean
gst_util_fraction_add (gint a_n, gint a_d, gint b_n, gint b_d, gint * res_n,
    gint * res_d)
{
  gint gcd;

  g_return_val_if_fail (res_n != NULL, FALSE);
  g_return_val_if_fail (res_d != NULL, FALSE);
  g_return_val_if_fail (a_d != 0, FALSE);
  g_return_val_if_fail (b_d != 0, FALSE);

  gcd = gst_util_greatest_common_divisor (a_n, a_d);
  a_n /= gcd;
  a_d /= gcd;

  gcd = gst_util_greatest_common_divisor (b_n, b_d);
  b_n /= gcd;
  b_d /= gcd;

  if (a_n == 0) {
    *res_n = b_n;
    *res_d = b_d;
    return TRUE;
  }
  if (b_n == 0) {
    *res_n = a_n;
    *res_d = a_d;
    return TRUE;
  }

  /* This would result in overflow */
  if (G_MAXINT / ABS (a_n) < ABS (b_n))
    return FALSE;
  if (G_MAXINT / ABS (a_d) < ABS (b_d))
    return FALSE;
  if (G_MAXINT / ABS (a_d) < ABS (b_d))
    return FALSE;

  *res_n = (a_n * b_d) + (a_d * b_n);
  *res_d = a_d * b_d;

  gcd = gst_util_greatest_common_divisor (*res_n, *res_d);
  if (gcd) {
    *res_n /= gcd;
    *res_d /= gcd;
  } else {
    /* res_n == 0 */
    *res_d = 1;
  }

  return TRUE;
}

/**
 * gst_util_fraction_compare:
 * @a_n: Numerator of first value
 * @a_d: Denominator of first value
 * @b_n: Numerator of second value
 * @b_d: Denominator of second value
 *
 * Compares the fractions @a_n/@a_d and @b_n/@b_d and returns
 * -1 if a < b, 0 if a = b and 1 if a > b.
 *
 * Returns: -1 if a < b; 0 if a = b; 1 if a > b.
 *
 * Since: 0.10.31
 */
gint
gst_util_fraction_compare (gint a_n, gint a_d, gint b_n, gint b_d)
{
  gint64 new_num_1;
  gint64 new_num_2;
  gint gcd;

  g_return_val_if_fail (a_d != 0 && b_d != 0, 0);

  /* Simplify */
  gcd = gst_util_greatest_common_divisor (a_n, a_d);
  a_n /= gcd;
  a_d /= gcd;

  gcd = gst_util_greatest_common_divisor (b_n, b_d);
  b_n /= gcd;
  b_d /= gcd;

  /* fractions are reduced when set, so we can quickly see if they're equal */
  if (a_n == b_n && a_d == b_d)
    return 0;

  /* extend to 64 bits */
  new_num_1 = ((gint64) a_n) * b_d;
  new_num_2 = ((gint64) b_n) * a_d;
  if (new_num_1 < new_num_2)
    return -1;
  if (new_num_1 > new_num_2)
    return 1;

  /* Should not happen because a_d and b_d are not 0 */
  g_return_val_if_reached (0);
}
