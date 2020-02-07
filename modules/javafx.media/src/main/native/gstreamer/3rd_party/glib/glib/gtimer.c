/* GLIB - Library of useful routines for C programming
 * Copyright (C) 1995-1997  Peter Mattis, Spencer Kimball and Josh MacDonald
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Modified by the GLib Team and others 1997-2000.  See the AUTHORS
 * file for a list of people on the GLib Team.  See the ChangeLog
 * files for a list of changes.  These files are distributed with
 * GLib at ftp://ftp.gtk.org/pub/gtk/.
 */

/*
 * MT safe
 */

#include "config.h"
#include "glibconfig.h"

#include <stdlib.h>

#ifdef G_OS_UNIX
#include <unistd.h>
#endif /* G_OS_UNIX */

#ifdef HAVE_SYS_TIME_H
#include <sys/time.h>
#endif
#include <time.h>
#ifndef G_OS_WIN32
#include <errno.h>
#endif /* G_OS_WIN32 */

#ifdef G_OS_WIN32
#include <windows.h>
#endif /* G_OS_WIN32 */

#include "gtimer.h"

#include "gmem.h"
#include "gstrfuncs.h"
#include "gtestutils.h"
#include "gmain.h"

/**
 * SECTION:timers
 * @title: Timers
 * @short_description: keep track of elapsed time
 *
 * #GTimer records a start time, and counts microseconds elapsed since
 * that time. This is done somewhat differently on different platforms,
 * and can be tricky to get exactly right, so #GTimer provides a
 * portable/convenient interface.
 **/

/**
 * GTimer:
 *
 * Opaque datatype that records a start time.
 **/
struct _GTimer
{
  guint64 start;
  guint64 end;

  guint active : 1;
};

/**
 * g_timer_new:
 *
 * Creates a new timer, and starts timing (i.e. g_timer_start() is
 * implicitly called for you).
 *
 * Returns: a new #GTimer.
 **/
GTimer*
g_timer_new (void)
{
  GTimer *timer;

  timer = g_new (GTimer, 1);
  timer->active = TRUE;

  timer->start = g_get_monotonic_time ();

  return timer;
}

/**
 * g_timer_destroy:
 * @timer: a #GTimer to destroy.
 *
 * Destroys a timer, freeing associated resources.
 **/
void
g_timer_destroy (GTimer *timer)
{
  g_return_if_fail (timer != NULL);

  g_free (timer);
}

/**
 * g_timer_start:
 * @timer: a #GTimer.
 *
 * Marks a start time, so that future calls to g_timer_elapsed() will
 * report the time since g_timer_start() was called. g_timer_new()
 * automatically marks the start time, so no need to call
 * g_timer_start() immediately after creating the timer.
 **/
void
g_timer_start (GTimer *timer)
{
  g_return_if_fail (timer != NULL);

  timer->active = TRUE;

  timer->start = g_get_monotonic_time ();
}

/**
 * g_timer_stop:
 * @timer: a #GTimer.
 *
 * Marks an end time, so calls to g_timer_elapsed() will return the
 * difference between this end time and the start time.
 **/
void
g_timer_stop (GTimer *timer)
{
  g_return_if_fail (timer != NULL);

  timer->active = FALSE;

  timer->end = g_get_monotonic_time ();
}

/**
 * g_timer_reset:
 * @timer: a #GTimer.
 *
 * This function is useless; it's fine to call g_timer_start() on an
 * already-started timer to reset the start time, so g_timer_reset()
 * serves no purpose.
 **/
void
g_timer_reset (GTimer *timer)
{
  g_return_if_fail (timer != NULL);

  timer->start = g_get_monotonic_time ();
}

/**
 * g_timer_continue:
 * @timer: a #GTimer.
 *
 * Resumes a timer that has previously been stopped with
 * g_timer_stop(). g_timer_stop() must be called before using this
 * function.
 *
 * Since: 2.4
 **/
void
g_timer_continue (GTimer *timer)
{
  guint64 elapsed;

  g_return_if_fail (timer != NULL);
  g_return_if_fail (timer->active == FALSE);

  /* Get elapsed time and reset timer start time
   *  to the current time minus the previously
   *  elapsed interval.
   */

  elapsed = timer->end - timer->start;

  timer->start = g_get_monotonic_time ();

  timer->start -= elapsed;

  timer->active = TRUE;
}

/**
 * g_timer_elapsed:
 * @timer: a #GTimer.
 * @microseconds: return location for the fractional part of seconds
 *                elapsed, in microseconds (that is, the total number
 *                of microseconds elapsed, modulo 1000000), or %NULL
 *
 * If @timer has been started but not stopped, obtains the time since
 * the timer was started. If @timer has been stopped, obtains the
 * elapsed time between the time it was started and the time it was
 * stopped. The return value is the number of seconds elapsed,
 * including any fractional part. The @microseconds out parameter is
 * essentially useless.
 *
 * Returns: seconds elapsed as a floating point value, including any
 *          fractional part.
 **/
gdouble
g_timer_elapsed (GTimer *timer,
     gulong *microseconds)
{
  gdouble total;
  gint64 elapsed;

  g_return_val_if_fail (timer != NULL, 0);

  if (timer->active)
    timer->end = g_get_monotonic_time ();

  elapsed = timer->end - timer->start;

  total = elapsed / 1e6;

  if (microseconds)
    *microseconds = elapsed % 1000000;

  return total;
}

/**
 * g_timer_is_active:
 * @timer: a #GTimer.
 *
 * Exposes whether the timer is currently active.
 *
 * Returns: %TRUE if the timer is running, %FALSE otherwise
 * Since: 2.62
 **/
gboolean
g_timer_is_active (GTimer *timer)
{
  g_return_val_if_fail (timer != NULL, FALSE);

  return timer->active;
}

/**
 * g_usleep:
 * @microseconds: number of microseconds to pause
 *
 * Pauses the current thread for the given number of microseconds.
 *
 * There are 1 million microseconds per second (represented by the
 * #G_USEC_PER_SEC macro). g_usleep() may have limited precision,
 * depending on hardware and operating system; don't rely on the exact
 * length of the sleep.
 */
void
g_usleep (gulong microseconds)
{
#ifdef G_OS_WIN32
  /* Round up to the next millisecond */
  Sleep (microseconds ? (1 + (microseconds - 1) / 1000) : 0);
#else
  struct timespec request, remaining;
  request.tv_sec = microseconds / G_USEC_PER_SEC;
  request.tv_nsec = 1000 * (microseconds % G_USEC_PER_SEC);
  while (nanosleep (&request, &remaining) == -1 && errno == EINTR)
    request = remaining;
#endif
}

/**
 * g_time_val_add:
 * @time_: a #GTimeVal
 * @microseconds: number of microseconds to add to @time
 *
 * Adds the given number of microseconds to @time_. @microseconds can
 * also be negative to decrease the value of @time_.
 *
 * Deprecated: 2.62: #GTimeVal is not year-2038-safe. Use `guint64` for
 *    representing microseconds since the epoch, or use #GDateTime.
 **/
G_GNUC_BEGIN_IGNORE_DEPRECATIONS
void
g_time_val_add (GTimeVal *time_, glong microseconds)
{
  g_return_if_fail (time_->tv_usec >= 0 && time_->tv_usec < G_USEC_PER_SEC);

  if (microseconds >= 0)
    {
      time_->tv_usec += microseconds % G_USEC_PER_SEC;
      time_->tv_sec += microseconds / G_USEC_PER_SEC;
      if (time_->tv_usec >= G_USEC_PER_SEC)
       {
         time_->tv_usec -= G_USEC_PER_SEC;
         time_->tv_sec++;
       }
    }
  else
    {
      microseconds *= -1;
      time_->tv_usec -= microseconds % G_USEC_PER_SEC;
      time_->tv_sec -= microseconds / G_USEC_PER_SEC;
      if (time_->tv_usec < 0)
       {
         time_->tv_usec += G_USEC_PER_SEC;
         time_->tv_sec--;
       }
    }
}
G_GNUC_END_IGNORE_DEPRECATIONS

/* converts a broken down date representation, relative to UTC,
 * to a timestamp; it uses timegm() if it's available.
 */
static time_t
mktime_utc (struct tm *tm)
{
  time_t retval;

#ifndef HAVE_TIMEGM
  static const gint days_before[] =
  {
    0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334
  };
#endif

#ifndef HAVE_TIMEGM
  if (tm->tm_mon < 0 || tm->tm_mon > 11)
    return (time_t) -1;

  retval = (tm->tm_year - 70) * 365;
  retval += (tm->tm_year - 68) / 4;
  retval += days_before[tm->tm_mon] + tm->tm_mday - 1;

  if (tm->tm_year % 4 == 0 && tm->tm_mon < 2)
    retval -= 1;

  retval = ((((retval * 24) + tm->tm_hour) * 60) + tm->tm_min) * 60 + tm->tm_sec;
#else
  retval = timegm (tm);
#endif /* !HAVE_TIMEGM */

  return retval;
}

/**
 * g_time_val_from_iso8601:
 * @iso_date: an ISO 8601 encoded date string
 * @time_: (out): a #GTimeVal
 *
 * Converts a string containing an ISO 8601 encoded date and time
 * to a #GTimeVal and puts it into @time_.
 *
 * @iso_date must include year, month, day, hours, minutes, and
 * seconds. It can optionally include fractions of a second and a time
 * zone indicator. (In the absence of any time zone indication, the
 * timestamp is assumed to be in local time.)
 *
 * Any leading or trailing space in @iso_date is ignored.
 *
 * This function was deprecated, along with #GTimeVal itself, in GLib 2.62.
 * Equivalent functionality is available using code like:
 * |[
 * GDateTime *dt = g_date_time_new_from_iso8601 (iso8601_string, NULL);
 * gint64 time_val = g_date_time_to_unix (dt);
 * g_date_time_unref (dt);
 * ]|
 *
 * Returns: %TRUE if the conversion was successful.
 *
 * Since: 2.12
 * Deprecated: 2.62: #GTimeVal is not year-2038-safe. Use
 *    g_date_time_new_from_iso8601() instead.
 */
G_GNUC_BEGIN_IGNORE_DEPRECATIONS
gboolean
g_time_val_from_iso8601 (const gchar *iso_date,
       GTimeVal    *time_)
{
  struct tm tm = {0};
  long val;
  long mday, mon, year;
  long hour, min, sec;

  g_return_val_if_fail (iso_date != NULL, FALSE);
  g_return_val_if_fail (time_ != NULL, FALSE);

  /* Ensure that the first character is a digit, the first digit
   * of the date, otherwise we don't have an ISO 8601 date
   */
  while (g_ascii_isspace (*iso_date))
    iso_date++;

  if (*iso_date == '\0')
    return FALSE;

  if (!g_ascii_isdigit (*iso_date) && *iso_date != '+')
    return FALSE;

  val = strtoul (iso_date, (char **)&iso_date, 10);
  if (*iso_date == '-')
    {
      /* YYYY-MM-DD */
      year = val;
      iso_date++;

      mon = strtoul (iso_date, (char **)&iso_date, 10);
      if (*iso_date++ != '-')
        return FALSE;

      mday = strtoul (iso_date, (char **)&iso_date, 10);
    }
  else
    {
      /* YYYYMMDD */
      mday = val % 100;
      mon = (val % 10000) / 100;
      year = val / 10000;
    }

  /* Validation. */
  if (year < 1900 || year > G_MAXINT)
    return FALSE;
  if (mon < 1 || mon > 12)
    return FALSE;
  if (mday < 1 || mday > 31)
    return FALSE;

  tm.tm_mday = mday;
  tm.tm_mon = mon - 1;
  tm.tm_year = year - 1900;

  if (*iso_date != 'T')
    return FALSE;

  iso_date++;

  /* If there is a 'T' then there has to be a time */
  if (!g_ascii_isdigit (*iso_date))
    return FALSE;

  val = strtoul (iso_date, (char **)&iso_date, 10);
  if (*iso_date == ':')
    {
      /* hh:mm:ss */
      hour = val;
      iso_date++;
      min = strtoul (iso_date, (char **)&iso_date, 10);

      if (*iso_date++ != ':')
        return FALSE;

      sec = strtoul (iso_date, (char **)&iso_date, 10);
    }
  else
    {
      /* hhmmss */
      sec = val % 100;
      min = (val % 10000) / 100;
      hour = val / 10000;
    }

  /* Validation. Allow up to 2 leap seconds when validating @sec. */
  if (hour > 23)
    return FALSE;
  if (min > 59)
    return FALSE;
  if (sec > 61)
    return FALSE;

  tm.tm_hour = hour;
  tm.tm_min = min;
  tm.tm_sec = sec;

  time_->tv_usec = 0;

  if (*iso_date == ',' || *iso_date == '.')
    {
      glong mul = 100000;

      while (mul >= 1 && g_ascii_isdigit (*++iso_date))
        {
          time_->tv_usec += (*iso_date - '0') * mul;
          mul /= 10;
        }

      /* Skip any remaining digits after we've reached our limit of precision. */
      while (g_ascii_isdigit (*iso_date))
        iso_date++;
    }

  /* Now parse the offset and convert tm to a time_t */
  if (*iso_date == 'Z')
    {
      iso_date++;
      time_->tv_sec = mktime_utc (&tm);
    }
  else if (*iso_date == '+' || *iso_date == '-')
    {
      gint sign = (*iso_date == '+') ? -1 : 1;

      val = strtoul (iso_date + 1, (char **)&iso_date, 10);

      if (*iso_date == ':')
        {
          /* hh:mm */
          hour = val;
          min = strtoul (iso_date + 1, (char **)&iso_date, 10);
        }
      else
        {
          /* hhmm */
          hour = val / 100;
          min = val % 100;
        }

      if (hour > 99)
        return FALSE;
      if (min > 59)
        return FALSE;

      time_->tv_sec = mktime_utc (&tm) + (time_t) (60 * (gint64) (60 * hour + min) * sign);
    }
  else
    {
      /* No "Z" or offset, so local time */
      tm.tm_isdst = -1; /* locale selects DST */
      time_->tv_sec = mktime (&tm);
    }

  while (g_ascii_isspace (*iso_date))
    iso_date++;

  return *iso_date == '\0';
}
G_GNUC_END_IGNORE_DEPRECATIONS

/**
 * g_time_val_to_iso8601:
 * @time_: a #GTimeVal
 *
 * Converts @time_ into an RFC 3339 encoded string, relative to the
 * Coordinated Universal Time (UTC). This is one of the many formats
 * allowed by ISO 8601.
 *
 * ISO 8601 allows a large number of date/time formats, with or without
 * punctuation and optional elements. The format returned by this function
 * is a complete date and time, with optional punctuation included, the
 * UTC time zone represented as "Z", and the @tv_usec part included if
 * and only if it is nonzero, i.e. either
 * "YYYY-MM-DDTHH:MM:SSZ" or "YYYY-MM-DDTHH:MM:SS.fffffZ".
 *
 * This corresponds to the Internet date/time format defined by
 * [RFC 3339](https://www.ietf.org/rfc/rfc3339.txt),
 * and to either of the two most-precise formats defined by
 * the W3C Note
 * [Date and Time Formats](http://www.w3.org/TR/NOTE-datetime-19980827).
 * Both of these documents are profiles of ISO 8601.
 *
 * Use g_date_time_format() or g_strdup_printf() if a different
 * variation of ISO 8601 format is required.
 *
 * If @time_ represents a date which is too large to fit into a `struct tm`,
 * %NULL will be returned. This is platform dependent. Note also that since
 * `GTimeVal` stores the number of seconds as a `glong`, on 32-bit systems it
 * is subject to the year 2038 problem. Accordingly, since GLib 2.62, this
 * function has been deprecated. Equivalent functionality is available using:
 * |[
 * GDateTime *dt = g_date_time_new_from_unix_utc (time_val);
 * iso8601_string = g_date_time_format_iso8601 (dt);
 * g_date_time_unref (dt);
 * ]|
 *
 * The return value of g_time_val_to_iso8601() has been nullable since GLib
 * 2.54; before then, GLib would crash under the same conditions.
 *
 * Returns: (nullable): a newly allocated string containing an ISO 8601 date,
 *    or %NULL if @time_ was too large
 *
 * Since: 2.12
 * Deprecated: 2.62: #GTimeVal is not year-2038-safe. Use
 *    g_date_time_format_iso8601(dt) instead.
 */
G_GNUC_BEGIN_IGNORE_DEPRECATIONS
gchar *
g_time_val_to_iso8601 (GTimeVal *time_)
{
  gchar *retval;
  struct tm *tm;
#ifdef HAVE_GMTIME_R
  struct tm tm_;
#endif
  time_t secs;

  g_return_val_if_fail (time_->tv_usec >= 0 && time_->tv_usec < G_USEC_PER_SEC, NULL);

  secs = time_->tv_sec;
#ifdef _WIN32
  tm = gmtime (&secs);
#else
#ifdef HAVE_GMTIME_R
  tm = gmtime_r (&secs, &tm_);
#else
  tm = gmtime (&secs);
#endif
#endif

  /* If the gmtime() call has failed, time_->tv_sec is too big. */
  if (tm == NULL)
    return NULL;

  if (time_->tv_usec != 0)
    {
      /* ISO 8601 date and time format, with fractionary seconds:
       *   YYYY-MM-DDTHH:MM:SS.MMMMMMZ
       */
      retval = g_strdup_printf ("%4d-%02d-%02dT%02d:%02d:%02d.%06ldZ",
                                tm->tm_year + 1900,
                                tm->tm_mon + 1,
                                tm->tm_mday,
                                tm->tm_hour,
                                tm->tm_min,
                                tm->tm_sec,
                                time_->tv_usec);
    }
  else
    {
      /* ISO 8601 date and time format:
       *   YYYY-MM-DDTHH:MM:SSZ
       */
      retval = g_strdup_printf ("%4d-%02d-%02dT%02d:%02d:%02dZ",
                                tm->tm_year + 1900,
                                tm->tm_mon + 1,
                                tm->tm_mday,
                                tm->tm_hour,
                                tm->tm_min,
                                tm->tm_sec);
    }

  return retval;
}
G_GNUC_END_IGNORE_DEPRECATIONS
