/* GStreamer
 * Copyright (C) 2010 Thiago Santos <thiago.sousa.santos@collabora.co.uk>
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

#include "glib-compat-private.h"
#include "gst_private.h"
#include "gstdatetime.h"
#include <glib.h>
#include <math.h>

/**
 * SECTION:gstdatetime
 * @title: GstDateTime
 * @short_description: A date, time and timezone structure
 *
 * Struct to store date, time and timezone information altogether.
 * #GstDateTime is refcounted and immutable.
 *
 * Date information is handled using the proleptic Gregorian calendar.
 *
 * Provides basic creation functions and accessor functions to its fields.
 *
 * Since: 0.10.31
 */

/**
 * gst_date_time_get_year:
 * @datetime: a #GstDateTime
 *
 * Returns the year of this #GstDateTime
 *
 * Return value: The year of this #GstDateTime
 * Since: 0.10.31
 */

/**
 * gst_date_time_get_month:
 * @datetime: a #GstDateTime
 *
 * Returns the month of this #GstDateTime. January is 1, February is 2, etc..
 *
 * Return value: The month of this #GstDateTime
 * Since: 0.10.31
 */

/**
 * gst_date_time_get_day:
 * @datetime: a #GstDateTime
 *
 * Returns the day of this #GstDateTime.
 *
 * Return value: The day of this #GstDateTime
 * Since: 0.10.31
 */

/**
 * gst_date_time_get_hour:
 * @datetime: a #GstDateTime
 *
 * Retrieves the hour of the day represented by @datetime in the gregorian
 * calendar. The return is in the range of 0 to 23.
 *
 * Return value: the hour of the day
 *
 * Since: 0.10.31
 */

/**
 * gst_date_time_get_microsecond:
 * @datetime: a #GstDateTime
 *
 * Retrieves the fractional part of the seconds in microseconds represented by
 * @datetime in the gregorian calendar.
 *
 * Return value: the microsecond of the second
 *
 * Since: 0.10.31
 */

/**
 * gst_date_time_get_minute:
 * @datetime: a #GstDateTime
 *
 * Retrieves the minute of the hour represented by @datetime in the gregorian
 * calendar.
 *
 * Return value: the minute of the hour
 *
 * Since: 0.10.31
 */

/**
 * gst_date_time_get_second:
 * @datetime: a #GstDateTime
 *
 * Retrieves the second of the minute represented by @datetime in the gregorian
 * calendar.
 *
 * Return value: the second represented by @datetime
 *
 * Since: 0.10.31
 */

/**
 * gst_date_time_get_second:
 * @datetime: a #GstDateTime
 *
 * Retrieves the second of the minute represented by @datetime in the gregorian
 * calendar.
 *
 * Return value: the second represented by @datetime
 *
 * Since: 0.10.31
 */

/**
 * gst_date_time_get_time_zone_offset:
 * @datetime: a #GstDateTime
 *
 * Retrieves the offset from UTC in hours that the timezone specified
 * by @datetime represents. Timezones ahead (to the east) of UTC have positive
 * values, timezones before (to the west) of UTC have negative values.
 * If @datetime represents UTC time, then the offset is zero.
 *
 * Return value: the offset from UTC in hours
 * Since: 0.10.31
 */

/**
 * gst_date_time_new_from_unix_epoch_local_time:
 * @secs: seconds from the Unix epoch
 *
 * Creates a new #GstDateTime using the time since Jan 1, 1970 specified by
 * @secs. The #GstDateTime is in the local timezone.
 *
 * Free-function: gst_date_time_unref
 *
 * Return value: (transfer full): the newly created #GstDateTime
 *
 * Since: 0.10.31
 */

/**
 * gst_date_time_new_from_unix_epoch_utc:
 * @secs: seconds from the Unix epoch
 *
 * Creates a new #GstDateTime using the time since Jan 1, 1970 specified by
 * @secs. The #GstDateTime is in the UTC timezone.
 *
 * Free-function: gst_date_time_unref
 *
 * Return value: (transfer full): the newly created #GstDateTime
 *
 * Since: 0.10.31
 */

/**
 * gst_date_time_new_local_time:
 * @year: the gregorian year
 * @month: the gregorian month
 * @day: the day of the gregorian month
 * @hour: the hour of the day
 * @minute: the minute of the hour
 * @seconds: the second of the minute
 *
 * Creates a new #GstDateTime using the date and times in the gregorian calendar
 * in the local timezone.
 *
 * @year should be from 1 to 9999, @month should be from 1 to 12, @day from
 * 1 to 31, @hour from 0 to 23, @minutes and @seconds from 0 to 59.
 *
 * Free-function: gst_date_time_unref
 *
 * Return value: (transfer full): the newly created #GstDateTime
 *
 * Since: 0.10.31
 */

/**
 * gst_date_time_new:
 * @tzoffset: Offset from UTC in hours.
 * @year: the gregorian year
 * @month: the gregorian month
 * @day: the day of the gregorian month
 * @hour: the hour of the day
 * @minute: the minute of the hour
 * @seconds: the second of the minute
 *
 * Creates a new #GstDateTime using the date and times in the gregorian calendar
 * in the supplied timezone.
 *
 * @year should be from 1 to 9999, @month should be from 1 to 12, @day from
 * 1 to 31, @hour from 0 to 23, @minutes and @seconds from 0 to 59.
 *
 * Note that @tzoffset is a float and was chosen so for being able to handle
 * some fractional timezones, while it still keeps the readability of
 * represeting it in hours for most timezones.
 *
 * Free-function: gst_date_time_unref
 *
 * Return value: (transfer full): the newly created #GstDateTime
 *
 * Since: 0.10.31
 */

/**
 * gst_date_time_new_now_local_time:
 *
 * Creates a new #GstDateTime representing the current date and time.
 *
 * Free-function: gst_date_time_unref
 *
 * Return value: (transfer full): the newly created #GstDateTime which should
 *     be freed with gst_date_time_unref().
 *
 * Since: 0.10.31
 */

/**
 * gst_date_time_new_now_utc:
 *
 * Creates a new #GstDateTime that represents the current instant at Universal
 * coordinated time.
 *
 * Free-function: gst_date_time_unref
 *
 * Return value: (transfer full): the newly created #GstDateTime which should
 *   be freed with gst_date_time_unref().
 *
 * Since: 0.10.31
 */


#define GST_DATE_TIME_SEC_PER_DAY          (G_GINT64_CONSTANT (86400))
#define GST_DATE_TIME_USEC_PER_DAY         (G_GINT64_CONSTANT (86400000000))
#define GST_DATE_TIME_USEC_PER_HOUR        (G_GINT64_CONSTANT (3600000000))
#define GST_DATE_TIME_USEC_PER_MINUTE      (G_GINT64_CONSTANT (60000000))
#define GST_DATE_TIME_USEC_PER_SECOND      (G_GINT64_CONSTANT (1000000))
#define GST_DATE_TIME_USEC_PER_MILLISECOND (G_GINT64_CONSTANT (1000))

/* Jan 5th 2011 (Edward) : GLib's GDateTime is broken in regards to gmt offset
 * on macosx. Re-enable it once the following bug is fixed:
 * https://bugzilla.gnome.org/show_bug.cgi?id=638666 */
#ifdef HAVE_OSX
#undef GLIB_HAS_GDATETIME
#endif


#ifndef GLIB_HAS_GDATETIME

#define MAX_SUPPORTED_YEAR 9999
#define GREGORIAN_LEAP(y)  (((y%4)==0)&&(!(((y%100)==0)&&((y%400)!=0))))

static const guint16 days_in_months[2][13] = {
  {0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31},
  {0, 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31}
};

struct _GstDateTime
{
  /*
   * As we don't have a datetime math API, we can have fields split here.
   * (There is still some math done internally, but nothing really relevant).
   *
   * If we ever add one, we should go for a days since some epoch counter.
   * (Proleptic Gregorian with 0001-01-01 as day 1)
   */
  gint16 year;
  gint8 month;
  gint8 day;
  guint64 usec;                 /* Microsecond timekeeping within Day */

  gint tzoffset;

  volatile gint ref_count;
};

/*
 * Returns the utc offset in seconds for this time structure
 */
static gint
gmt_offset (struct tm *tm, time_t t)
{
#if defined (HAVE_TM_GMTOFF)
  return tm->tm_gmtoff;
#else
  struct tm g;
  time_t t2;
#ifdef HAVE_GMTIME_R
  gmtime_r (&t, &g);
#else
  g = *gmtime (&t);
#endif
  t2 = mktime (&g);
  return (int) difftime (t, t2);
#endif
}

static void
gst_date_time_set_local_timezone (GstDateTime * dt)
{
  struct tm tt;
  time_t t;

  g_return_if_fail (dt != NULL);

  memset (&tt, 0, sizeof (tt));

  tt.tm_mday = gst_date_time_get_day (dt);
  tt.tm_mon = gst_date_time_get_month (dt) - 1;
  tt.tm_year = gst_date_time_get_year (dt) - 1900;
  tt.tm_hour = gst_date_time_get_hour (dt);
  tt.tm_min = gst_date_time_get_minute (dt);
  tt.tm_sec = gst_date_time_get_second (dt);

  t = mktime (&tt);

  dt->tzoffset = gmt_offset (&tt, t) / 60;
}

static GstDateTime *
gst_date_time_alloc (void)
{
  GstDateTime *datetime;

  datetime = g_slice_new0 (GstDateTime);
  datetime->ref_count = 1;

  return datetime;
}

static void
gst_date_time_free (GstDateTime * datetime)
{
  g_slice_free (GstDateTime, datetime);
}

static GstDateTime *
gst_date_time_new_from_date (gint year, gint month, gint day)
{
  GstDateTime *dt;

  g_return_val_if_fail (year > 0 && year <= 9999, NULL);
  g_return_val_if_fail ((month > 0 && month <= 12), NULL);
  g_return_val_if_fail ((day > 0 && day <= 31), NULL);

  dt = gst_date_time_alloc ();

  dt->year = year;
  dt->month = month;
  dt->day = day;
  gst_date_time_set_local_timezone (dt);

  return dt;
}

gint
gst_date_time_get_year (const GstDateTime * datetime)
{
  g_return_val_if_fail (datetime != NULL, 0);

  return datetime->year;
}

gint
gst_date_time_get_month (const GstDateTime * datetime)
{
  g_return_val_if_fail (datetime != NULL, 0);

  return datetime->month;
}

gint
gst_date_time_get_day (const GstDateTime * datetime)
{
  g_return_val_if_fail (datetime != NULL, 0);

  return datetime->day;
}

gint
gst_date_time_get_hour (const GstDateTime * datetime)
{
  g_return_val_if_fail (datetime != NULL, 0);
  return (datetime->usec / GST_DATE_TIME_USEC_PER_HOUR);
}

gint
gst_date_time_get_microsecond (const GstDateTime * datetime)
{
  g_return_val_if_fail (datetime != NULL, 0);
  return (datetime->usec % GST_DATE_TIME_USEC_PER_SECOND);
}

gint
gst_date_time_get_minute (const GstDateTime * datetime)
{
  g_return_val_if_fail (datetime != NULL, 0);
  return (datetime->usec % GST_DATE_TIME_USEC_PER_HOUR) /
      GST_DATE_TIME_USEC_PER_MINUTE;
}

gint
gst_date_time_get_second (const GstDateTime * datetime)
{
  g_return_val_if_fail (datetime != NULL, 0);
  return (datetime->usec % GST_DATE_TIME_USEC_PER_MINUTE) /
      GST_DATE_TIME_USEC_PER_SECOND;
}

gfloat
gst_date_time_get_time_zone_offset (const GstDateTime * datetime)
{
  g_return_val_if_fail (datetime != NULL, 0);

  return datetime->tzoffset / 60.0f;
}

GstDateTime *
gst_date_time_new_from_unix_epoch_local_time (gint64 secs)
{
  GstDateTime *dt;
  struct tm tm;
  time_t tt;

  memset (&tm, 0, sizeof (tm));
  tt = (time_t) secs;

#ifdef HAVE_LOCALTIME_R
  localtime_r (&tt, &tm);
#else
  memcpy (&tm, localtime (&tt), sizeof (struct tm));
#endif

  dt = gst_date_time_new (0, tm.tm_year + 1900,
      tm.tm_mon + 1, tm.tm_mday, tm.tm_hour, tm.tm_min, tm.tm_sec);
  gst_date_time_set_local_timezone (dt);
  return dt;
}

GstDateTime *
gst_date_time_new_from_unix_epoch_utc (gint64 secs)
{
  GstDateTime *dt;
  struct tm tm;
  time_t tt;

  memset (&tm, 0, sizeof (tm));
  tt = (time_t) secs;

#ifdef HAVE_GMTIME_R
  gmtime_r (&tt, &tm);
#else
  memcpy (&tm, gmtime (&tt), sizeof (struct tm));
#endif

  dt = gst_date_time_new (0, tm.tm_year + 1900,
      tm.tm_mon + 1, tm.tm_mday, tm.tm_hour, tm.tm_min, tm.tm_sec);
  return dt;
}

GstDateTime *
gst_date_time_new_local_time (gint year, gint month, gint day, gint hour,
    gint minute, gdouble seconds)
{
  GstDateTime *dt;

  dt = gst_date_time_new (0, year, month, day, hour, minute, seconds);

  gst_date_time_set_local_timezone (dt);

  return dt;
}

GstDateTime *
gst_date_time_new (gfloat tzoffset, gint year, gint month, gint day, gint hour,
    gint minute, gdouble seconds)
{
  GstDateTime *dt;

  g_return_val_if_fail (hour >= 0 && hour < 24, NULL);
  g_return_val_if_fail (minute >= 0 && minute < 60, NULL);
  g_return_val_if_fail (seconds >= 0 && seconds < 60, NULL);
  g_return_val_if_fail (tzoffset >= -12.0 && tzoffset <= 12.0, NULL);

  if (!(dt = gst_date_time_new_from_date (year, month, day)))
    return NULL;

  dt->usec = (hour * GST_DATE_TIME_USEC_PER_HOUR)
      + (minute * GST_DATE_TIME_USEC_PER_MINUTE)
      + (guint64) (floor (seconds * GST_DATE_TIME_USEC_PER_SECOND + 0.5));

  /* we store in minutes */
  dt->tzoffset = (gint) (tzoffset * 60.0);

  return dt;
}

GstDateTime *
gst_date_time_new_now_local_time (void)
{
  GstDateTime *datetime;
  GTimeVal tv;
  g_get_current_time (&tv);

  datetime = gst_date_time_new_from_unix_epoch_local_time (tv.tv_sec);
  datetime->usec += tv.tv_usec;
  gst_date_time_set_local_timezone (datetime);
  return datetime;
}

static GstDateTime *
gst_date_time_copy (const GstDateTime * dt)
{
  GstDateTime *copy = gst_date_time_alloc ();

  memcpy (copy, dt, sizeof (GstDateTime));
  copy->ref_count = 1;

  return copy;
}

static GstDateTime *
gst_date_time_to_utc (const GstDateTime * dt)
{
  GstDateTime *utc;
  gint64 usec;
  gint days;
  gint leap;

  g_return_val_if_fail (dt != NULL, NULL);

  utc = gst_date_time_copy (dt);

  usec = dt->usec - dt->tzoffset * GST_DATE_TIME_USEC_PER_MINUTE;
  days = usec / GST_DATE_TIME_USEC_PER_DAY;
  if (usec < 0)
    days--;
  utc->day += days;

  leap = GREGORIAN_LEAP (utc->year) ? 1 : 0;

  /* check if we should update month/year */
  if (utc->day < 1) {
    if (utc->month == 1) {
      utc->year--;
      utc->month = 12;
    } else {
      utc->month--;
    }
    if (GREGORIAN_LEAP (utc->year))
      utc->day = days_in_months[1][utc->month];
    else
      utc->day = days_in_months[0][utc->month];
  } else if (utc->day > days_in_months[leap][utc->month]) {
    if (utc->month == 12) {
      utc->year++;
      utc->month = 1;
    } else {
      utc->month++;
    }
    utc->day = 1;
  }

  if (usec < 0)
    utc->usec =
        GST_DATE_TIME_USEC_PER_DAY + (usec % GST_DATE_TIME_USEC_PER_DAY);
  else
    utc->usec = usec % GST_DATE_TIME_USEC_PER_DAY;

  return utc;
}

GstDateTime *
gst_date_time_new_now_utc (void)
{
  GstDateTime *now, *utc;

  now = gst_date_time_new_now_local_time ();
  utc = gst_date_time_to_utc (now);
  gst_date_time_unref (now);
  return utc;
}

gint
priv_gst_date_time_compare (gconstpointer dt1, gconstpointer dt2)
{
  GstDateTime *a, *b;
  gint res = 0;

  a = gst_date_time_to_utc (dt1);
  b = gst_date_time_to_utc (dt2);

#define GST_DATE_TIME_COMPARE_VALUE(a,b,v)   \
  if ((a)->v > (b)->v) {                     \
    res = 1;                                 \
    goto done;                               \
  } else if ((a)->v < (b)->v) {              \
    res = -1;                                \
    goto done;                               \
  }

  GST_DATE_TIME_COMPARE_VALUE (a, b, year);
  GST_DATE_TIME_COMPARE_VALUE (a, b, month);
  GST_DATE_TIME_COMPARE_VALUE (a, b, day);
  GST_DATE_TIME_COMPARE_VALUE (a, b, usec);

#undef GST_DATE_TIME_COMPARE_VALUE

done:
  gst_date_time_unref (a);
  gst_date_time_unref (b);
  return res;
}

#else

struct _GstDateTime
{
  GDateTime *datetime;

  volatile gint ref_count;
};

static GstDateTime *
gst_date_time_new_from_gdatetime (GDateTime * dt)
{
  GstDateTime *gst_dt;

  if (!dt)
    return NULL;

  gst_dt = g_slice_new (GstDateTime);
  gst_dt->datetime = dt;
  gst_dt->ref_count = 1;
  return gst_dt;
}

gint
gst_date_time_get_year (const GstDateTime * datetime)
{
  return g_date_time_get_year (datetime->datetime);
}

gint
gst_date_time_get_month (const GstDateTime * datetime)
{
  return g_date_time_get_month (datetime->datetime);
}

gint
gst_date_time_get_day (const GstDateTime * datetime)
{
  return g_date_time_get_day_of_month (datetime->datetime);
}

gint
gst_date_time_get_hour (const GstDateTime * datetime)
{
  return g_date_time_get_hour (datetime->datetime);
}

gint
gst_date_time_get_minute (const GstDateTime * datetime)
{
  return g_date_time_get_minute (datetime->datetime);
}

gint
gst_date_time_get_second (const GstDateTime * datetime)
{
  return g_date_time_get_second (datetime->datetime);
}

gint
gst_date_time_get_microsecond (const GstDateTime * datetime)
{
  return g_date_time_get_microsecond (datetime->datetime);
}

gfloat
gst_date_time_get_time_zone_offset (const GstDateTime * datetime)
{
  return (g_date_time_get_utc_offset (datetime->datetime) /
      G_USEC_PER_SEC) / 3600.0;
}

GstDateTime *
gst_date_time_new_from_unix_epoch_local_time (gint64 secs)
{
  return
      gst_date_time_new_from_gdatetime (g_date_time_new_from_unix_local (secs));
}

GstDateTime *
gst_date_time_new_from_unix_epoch_utc (gint64 secs)
{
  return
      gst_date_time_new_from_gdatetime (g_date_time_new_from_unix_utc (secs));
}

GstDateTime *
gst_date_time_new_local_time (gint year, gint month, gint day, gint hour,
    gint minute, gdouble seconds)
{
  return gst_date_time_new_from_gdatetime (g_date_time_new_local (year, month,
          day, hour, minute, seconds));
}

GstDateTime *
gst_date_time_new_now_local_time (void)
{
  return gst_date_time_new_from_gdatetime (g_date_time_new_now_local ());
}

GstDateTime *
gst_date_time_new_now_utc (void)
{
  return gst_date_time_new_from_gdatetime (g_date_time_new_now_utc ());
}

gint
priv_gst_date_time_compare (gconstpointer dt1, gconstpointer dt2)
{
  const GstDateTime *datetime1 = dt1;
  const GstDateTime *datetime2 = dt2;
  return g_date_time_compare (datetime1->datetime, datetime2->datetime);
}

GstDateTime *
gst_date_time_new (gfloat tzoffset, gint year, gint month, gint day, gint hour,
    gint minute, gdouble seconds)
{
  gchar buf[6];
  GTimeZone *tz;
  GDateTime *dt;
  gint tzhour, tzminute;

  tzhour = (gint) ABS (tzoffset);
  tzminute = (gint) ((ABS (tzoffset) - tzhour) * 60);

  g_snprintf (buf, 6, "%c%02d%02d", tzoffset >= 0 ? '+' : '-', tzhour,
      tzminute);

  tz = g_time_zone_new (buf);

  dt = g_date_time_new (tz, year, month, day, hour, minute, seconds);
  g_time_zone_unref (tz);
  return gst_date_time_new_from_gdatetime (dt);
}

static void
gst_date_time_free (GstDateTime * datetime)
{
  g_date_time_unref (datetime->datetime);
  g_slice_free (GstDateTime, datetime);
}

#endif

/**
 * gst_date_time_ref:
 * @datetime: a #GstDateTime
 *
 * Atomically increments the reference count of @datetime by one.
 *
 * Return value: (transfer full): the reference @datetime
 *
 * Since: 0.10.31
 */
GstDateTime *
gst_date_time_ref (GstDateTime * datetime)
{
  g_return_val_if_fail (datetime != NULL, NULL);
  g_return_val_if_fail (datetime->ref_count > 0, NULL);
  g_atomic_int_inc (&datetime->ref_count);
  return datetime;
}

/**
 * gst_date_time_unref:
 * @datetime: (transfer full): a #GstDateTime
 *
 * Atomically decrements the reference count of @datetime by one.  When the
 * reference count reaches zero, the structure is freed.
 *
 * Since: 0.10.31
 */
void
gst_date_time_unref (GstDateTime * datetime)
{
  g_return_if_fail (datetime != NULL);
  g_return_if_fail (datetime->ref_count > 0);

  if (g_atomic_int_dec_and_test (&datetime->ref_count))
    gst_date_time_free (datetime);
}
