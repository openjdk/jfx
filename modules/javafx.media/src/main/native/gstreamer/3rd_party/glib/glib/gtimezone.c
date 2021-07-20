/*
 * Copyright (C) 2010 Codethink Limited
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
 *
 * Author: Ryan Lortie <desrt@desrt.ca>
 */

/* Prologue {{{1 */

#include "config.h"

#include "gtimezone.h"

#include <string.h>
#include <stdlib.h>
#include <signal.h>

#include "gmappedfile.h"
#include "gtestutils.h"
#include "gfileutils.h"
#include "gstrfuncs.h"
#include "ghash.h"
#include "gthread.h"
#include "gbytes.h"
#include "gslice.h"
#include "gdatetime.h"
#include "gdate.h"
#include "genviron.h"

#ifdef G_OS_WIN32

#define STRICT
#include <windows.h>
#include <wchar.h>
#endif

/**
 * SECTION:timezone
 * @title: GTimeZone
 * @short_description: a structure representing a time zone
 * @see_also: #GDateTime
 *
 * #GTimeZone is a structure that represents a time zone, at no
 * particular point in time.  It is refcounted and immutable.
 *
 * Each time zone has an identifier (for example, 'Europe/London') which is
 * platform dependent. See g_time_zone_new() for information on the identifier
 * formats. The identifier of a time zone can be retrieved using
 * g_time_zone_get_identifier().
 *
 * A time zone contains a number of intervals.  Each interval has
 * an abbreviation to describe it (for example, 'PDT'), an offset to UTC and a
 * flag indicating if the daylight savings time is in effect during that
 * interval.  A time zone always has at least one interval - interval 0. Note
 * that interval abbreviations are not the same as time zone identifiers
 * (apart from 'UTC'), and cannot be passed to g_time_zone_new().
 *
 * Every UTC time is contained within exactly one interval, but a given
 * local time may be contained within zero, one or two intervals (due to
 * incontinuities associated with daylight savings time).
 *
 * An interval may refer to a specific period of time (eg: the duration
 * of daylight savings time during 2010) or it may refer to many periods
 * of time that share the same properties (eg: all periods of daylight
 * savings time).  It is also possible (usually for political reasons)
 * that some properties (like the abbreviation) change between intervals
 * without other properties changing.
 *
 * #GTimeZone is available since GLib 2.26.
 */

/**
 * GTimeZone:
 *
 * #GTimeZone is an opaque structure whose members cannot be accessed
 * directly.
 *
 * Since: 2.26
 **/

/* IANA zoneinfo file format {{{1 */

/* unaligned */
typedef struct { gchar bytes[8]; } gint64_be;
typedef struct { gchar bytes[4]; } gint32_be;
typedef struct { gchar bytes[4]; } guint32_be;

static inline gint64 gint64_from_be (const gint64_be be) {
  gint64 tmp; memcpy (&tmp, &be, sizeof tmp); return GINT64_FROM_BE (tmp);
}

static inline gint32 gint32_from_be (const gint32_be be) {
  gint32 tmp; memcpy (&tmp, &be, sizeof tmp); return GINT32_FROM_BE (tmp);
}

static inline guint32 guint32_from_be (const guint32_be be) {
  guint32 tmp; memcpy (&tmp, &be, sizeof tmp); return GUINT32_FROM_BE (tmp);
}

/* The layout of an IANA timezone file header */
struct tzhead
{
  gchar      tzh_magic[4];
  gchar      tzh_version;
  guchar     tzh_reserved[15];

  guint32_be tzh_ttisgmtcnt;
  guint32_be tzh_ttisstdcnt;
  guint32_be tzh_leapcnt;
  guint32_be tzh_timecnt;
  guint32_be tzh_typecnt;
  guint32_be tzh_charcnt;
};

struct ttinfo
{
  gint32_be tt_gmtoff;
  guint8    tt_isdst;
  guint8    tt_abbrind;
};

/* A Transition Date structure for TZ Rules, an intermediate structure
   for parsing MSWindows and Environment-variable time zones. It
   Generalizes MSWindows's SYSTEMTIME struct.
 */
typedef struct
{
  gint     year;
  gint     mon;
  gint     mday;
  gint     wday;
  gint     week;
  gint32   offset;  /* hour*3600 + min*60 + sec; can be negative.  */
} TimeZoneDate;

/* POSIX Timezone abbreviations are typically 3 or 4 characters, but
   Microsoft uses 32-character names. We'll use one larger to ensure
   we have room for the terminating \0.
 */
#define NAME_SIZE 33

/* A MSWindows-style time zone transition rule. Generalizes the
   MSWindows TIME_ZONE_INFORMATION struct. Also used to compose time
   zones from tzset-style identifiers.
 */
typedef struct
{
  gint         start_year;
  gint32       std_offset;
  gint32       dlt_offset;
  TimeZoneDate dlt_start;
  TimeZoneDate dlt_end;
  gchar std_name[NAME_SIZE];
  gchar dlt_name[NAME_SIZE];
} TimeZoneRule;

/* GTimeZone's internal representation of a Daylight Savings (Summer)
   time interval.
 */
typedef struct
{
  gint32     gmt_offset;
  gboolean   is_dst;
  gchar     *abbrev;
} TransitionInfo;

/* GTimeZone's representation of a transition time to or from Daylight
   Savings (Summer) time and Standard time for the zone. */
typedef struct
{
  gint64 time;
  gint   info_index;
} Transition;

/* GTimeZone structure */
struct _GTimeZone
{
  gchar   *name;
  GArray  *t_info;         /* Array of TransitionInfo */
  GArray  *transitions;    /* Array of Transition */
  gint     ref_count;
};

G_LOCK_DEFINE_STATIC (time_zones);
static GHashTable/*<string?, GTimeZone>*/ *time_zones;
G_LOCK_DEFINE_STATIC (tz_default);
static GTimeZone *tz_default = NULL;
G_LOCK_DEFINE_STATIC (tz_local);
static GTimeZone *tz_local = NULL;

#define MIN_TZYEAR 1916 /* Daylight Savings started in WWI */
#define MAX_TZYEAR 2999 /* And it's not likely ever to go away, but
                           there's no point in getting carried
                           away. */

#ifdef G_OS_UNIX
static GTimeZone *parse_footertz (const gchar *, size_t);
#endif

/**
 * g_time_zone_unref:
 * @tz: a #GTimeZone
 *
 * Decreases the reference count on @tz.
 *
 * Since: 2.26
 **/
void
g_time_zone_unref (GTimeZone *tz)
{
  int ref_count;

again:
  ref_count = g_atomic_int_get (&tz->ref_count);

  g_assert (ref_count > 0);

  if (ref_count == 1)
    {
      if (tz->name != NULL)
        {
          G_LOCK(time_zones);

          /* someone else might have grabbed a ref in the meantime */
          if G_UNLIKELY (g_atomic_int_get (&tz->ref_count) != 1)
            {
              G_UNLOCK(time_zones);
              goto again;
            }

          if (time_zones != NULL)
            g_hash_table_remove (time_zones, tz->name);
          G_UNLOCK(time_zones);
        }

      if (tz->t_info != NULL)
        {
          guint idx;
          for (idx = 0; idx < tz->t_info->len; idx++)
            {
              TransitionInfo *info = &g_array_index (tz->t_info, TransitionInfo, idx);
              g_free (info->abbrev);
            }
          g_array_free (tz->t_info, TRUE);
        }
      if (tz->transitions != NULL)
        g_array_free (tz->transitions, TRUE);
      g_free (tz->name);

      g_slice_free (GTimeZone, tz);
    }

  else if G_UNLIKELY (!g_atomic_int_compare_and_exchange (&tz->ref_count,
                                                          ref_count,
                                                          ref_count - 1))
    goto again;
}

/**
 * g_time_zone_ref:
 * @tz: a #GTimeZone
 *
 * Increases the reference count on @tz.
 *
 * Returns: a new reference to @tz.
 *
 * Since: 2.26
 **/
GTimeZone *
g_time_zone_ref (GTimeZone *tz)
{
  g_assert (tz->ref_count > 0);

  g_atomic_int_inc (&tz->ref_count);

  return tz;
}

/* fake zoneinfo creation (for RFC3339/ISO 8601 timezones) {{{1 */
/*
 * parses strings of the form h or hh[[:]mm[[[:]ss]]] where:
 *  - h[h] is 0 to 24
 *  - mm is 00 to 59
 *  - ss is 00 to 59
 * If RFC8536, TIME_ is a transition time sans sign,
 * so colons are required before mm and ss, and hh can be up to 167.
 * See Internet RFC 8536 section 3.3.1:
 * https://tools.ietf.org/html/rfc8536#section-3.3.1
 * and POSIX Base Definitions 8.3 TZ rule time:
 * https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap08.html#tag_08_03
 */
static gboolean
parse_time (const gchar *time_,
            gint32      *offset,
            gboolean    rfc8536)
{
  if (*time_ < '0' || '9' < *time_)
    return FALSE;

  *offset = 60 * 60 * (*time_++ - '0');

  if (*time_ == '\0')
    return TRUE;

  if (*time_ != ':')
    {
      if (*time_ < '0' || '9' < *time_)
        return FALSE;

      *offset *= 10;
      *offset += 60 * 60 * (*time_++ - '0');

      if (rfc8536)
        {
          /* Internet RFC 8536 section 3.3.1 and POSIX 8.3 TZ together say
             that a transition time must be of the form [+-]hh[:mm[:ss]] where
             the hours part can range from -167 to 167.  */
          if ('0' <= *time_ && *time_ <= '9')
            {
              *offset *= 10;
              *offset += 60 * 60 * (*time_++ - '0');
            }
          if (*offset > 167 * 60 * 60)
            return FALSE;
        }
      else if (*offset > 24 * 60 * 60)
        return FALSE;

      if (*time_ == '\0')
        return TRUE;
    }

  if (*time_ == ':')
    time_++;
  else if (rfc8536)
    return FALSE;

  if (*time_ < '0' || '5' < *time_)
    return FALSE;

  *offset += 10 * 60 * (*time_++ - '0');

  if (*time_ < '0' || '9' < *time_)
    return FALSE;

  *offset += 60 * (*time_++ - '0');

  if (*time_ == '\0')
    return TRUE;

  if (*time_ == ':')
    time_++;
  else if (rfc8536)
    return FALSE;

  if (*time_ < '0' || '5' < *time_)
    return FALSE;

  *offset += 10 * (*time_++ - '0');

  if (*time_ < '0' || '9' < *time_)
    return FALSE;

  *offset += *time_++ - '0';

  return *time_ == '\0';
}

static gboolean
parse_constant_offset (const gchar *name,
                       gint32      *offset,
                       gboolean    rfc8536)
{
  /* Internet RFC 8536 section 3.3.1 and POSIX 8.3 TZ together say
     that a transition time must be numeric.  */
  if (!rfc8536 && g_strcmp0 (name, "UTC") == 0)
    {
      *offset = 0;
      return TRUE;
    }

  if (*name >= '0' && '9' >= *name)
    return parse_time (name, offset, rfc8536);

  switch (*name++)
    {
    case 'Z':
      *offset = 0;
      /* Internet RFC 8536 section 3.3.1 requires a numeric zone.  */
      return !rfc8536 && !*name;

    case '+':
      return parse_time (name, offset, rfc8536);

    case '-':
      if (parse_time (name, offset, rfc8536))
        {
          *offset = -*offset;
          return TRUE;
        }
      else
        return FALSE;

    default:
      return FALSE;
    }
}

static void
zone_for_constant_offset (GTimeZone *gtz, const gchar *name)
{
  gint32 offset;
  TransitionInfo info;

  if (name == NULL || !parse_constant_offset (name, &offset, FALSE))
    return;

  info.gmt_offset = offset;
  info.is_dst = FALSE;
  info.abbrev =  g_strdup (name);

  gtz->name = g_strdup (name);
  gtz->t_info = g_array_sized_new (FALSE, TRUE, sizeof (TransitionInfo), 1);
  g_array_append_val (gtz->t_info, info);

  /* Constant offset, no transitions */
  gtz->transitions = NULL;
}

#ifdef G_OS_UNIX
static gchar *
zone_identifier_unix (void)
{
  gchar *resolved_identifier = NULL;
  gsize prefix_len = 0;
  gchar *canonical_path = NULL;
  GError *read_link_err = NULL;
  const gchar *tzdir;

  /* Resolve the actual timezone pointed to by /etc/localtime. */
  resolved_identifier = g_file_read_link ("/etc/localtime", &read_link_err);
  if (resolved_identifier == NULL)
    {
      gboolean not_a_symlink = g_error_matches (read_link_err,
                                                G_FILE_ERROR,
                                                G_FILE_ERROR_INVAL);
      g_clear_error (&read_link_err);

      /* Fallback to the content of /var/db/zoneinfo or /etc/timezone
       * if /etc/localtime is not a symlink. /var/db/zoneinfo is
       * where 'tzsetup' program on FreeBSD and DragonflyBSD stores
       * the timezone chosen by the user. /etc/timezone is where user
       * choice is expressed on Gentoo OpenRC and others. */
      if (not_a_symlink && (g_file_get_contents ("/var/db/zoneinfo",
                                                 &resolved_identifier,
                                                 NULL, NULL) ||
                            g_file_get_contents ("/etc/timezone",
                                                 &resolved_identifier,
                                                 NULL, NULL)))
        g_strchomp (resolved_identifier);
      else
        {
          /* Error */
          g_assert (resolved_identifier == NULL);
          goto out;
        }
    }
  else
    {
      /* Resolve relative path */
      canonical_path = g_canonicalize_filename (resolved_identifier, "/etc");
      g_free (resolved_identifier);
      resolved_identifier = g_steal_pointer (&canonical_path);
    }

  tzdir = g_getenv ("TZDIR");
  if (tzdir == NULL)
    tzdir = "/usr/share/zoneinfo";

  /* Strip the prefix and slashes if possible. */
  if (g_str_has_prefix (resolved_identifier, tzdir))
    {
      prefix_len = strlen (tzdir);
      while (*(resolved_identifier + prefix_len) == '/')
        prefix_len++;
    }

  if (prefix_len > 0)
    memmove (resolved_identifier, resolved_identifier + prefix_len,
             strlen (resolved_identifier) - prefix_len + 1  /* nul terminator */);

  g_assert (resolved_identifier != NULL);

out:
  g_free (canonical_path);

  return resolved_identifier;
}

static GBytes*
zone_info_unix (const gchar *identifier,
                const gchar *resolved_identifier)
{
  gchar *filename = NULL;
  GMappedFile *file = NULL;
  GBytes *zoneinfo = NULL;
  const gchar *tzdir;

  tzdir = g_getenv ("TZDIR");
  if (tzdir == NULL)
    tzdir = "/usr/share/zoneinfo";

  /* identifier can be a relative or absolute path name;
     if relative, it is interpreted starting from /usr/share/zoneinfo
     while the POSIX standard says it should start with :,
     glibc allows both syntaxes, so we should too */
  if (identifier != NULL)
    {
      if (*identifier == ':')
        identifier ++;

      if (g_path_is_absolute (identifier))
        filename = g_strdup (identifier);
      else
        filename = g_build_filename (tzdir, identifier, NULL);
    }
  else
    {
      if (resolved_identifier == NULL)
        goto out;

      filename = g_strdup ("/etc/localtime");
    }

  file = g_mapped_file_new (filename, FALSE, NULL);
  if (file != NULL)
    {
      zoneinfo = g_bytes_new_with_free_func (g_mapped_file_get_contents (file),
                                             g_mapped_file_get_length (file),
                                             (GDestroyNotify)g_mapped_file_unref,
                                             g_mapped_file_ref (file));
      g_mapped_file_unref (file);
    }

  g_assert (resolved_identifier != NULL);

out:
  g_free (filename);

  return zoneinfo;
}

static void
init_zone_from_iana_info (GTimeZone *gtz,
                          GBytes    *zoneinfo,
                          gchar     *identifier  /* (transfer full) */)
{
  gsize size;
  guint index;
  guint32 time_count, type_count;
  guint8 *tz_transitions, *tz_type_index, *tz_ttinfo;
  guint8 *tz_abbrs;
  gsize timesize = sizeof (gint32);
  gconstpointer header_data = g_bytes_get_data (zoneinfo, &size);
  const gchar *data = header_data;
  const struct tzhead *header = header_data;
  GTimeZone *footertz = NULL;
  guint extra_time_count = 0, extra_type_count = 0;
  gint64 last_explicit_transition_time;

  g_return_if_fail (size >= sizeof (struct tzhead) &&
                    memcmp (header, "TZif", 4) == 0);

  /* FIXME: Handle invalid TZif files better (Issue#1088).  */

  if (header->tzh_version >= '2')
      {
        /* Skip ahead to the newer 64-bit data if it's available. */
        header = (const struct tzhead *)
          (((const gchar *) (header + 1)) +
           guint32_from_be(header->tzh_ttisgmtcnt) +
           guint32_from_be(header->tzh_ttisstdcnt) +
           8 * guint32_from_be(header->tzh_leapcnt) +
           5 * guint32_from_be(header->tzh_timecnt) +
           6 * guint32_from_be(header->tzh_typecnt) +
           guint32_from_be(header->tzh_charcnt));
        timesize = sizeof (gint64);
      }
  time_count = guint32_from_be(header->tzh_timecnt);
  type_count = guint32_from_be(header->tzh_typecnt);

  if (header->tzh_version >= '2')
    {
      const gchar *footer = (((const gchar *) (header + 1))
                             + guint32_from_be(header->tzh_ttisgmtcnt)
                             + guint32_from_be(header->tzh_ttisstdcnt)
                             + 12 * guint32_from_be(header->tzh_leapcnt)
                             + 9 * time_count
                             + 6 * type_count
                             + guint32_from_be(header->tzh_charcnt));
      const gchar *footerlast;
      size_t footerlen;
      g_return_if_fail (footer <= data + size - 2 && footer[0] == '\n');
      footerlast = memchr (footer + 1, '\n', data + size - (footer + 1));
      g_return_if_fail (footerlast);
      footerlen = footerlast + 1 - footer;
      if (footerlen != 2)
        {
          footertz = parse_footertz (footer, footerlen);
          g_return_if_fail (footertz);
          extra_type_count = footertz->t_info->len;
          extra_time_count = footertz->transitions->len;
        }
    }

  tz_transitions = ((guint8 *) (header) + sizeof (*header));
  tz_type_index = tz_transitions + timesize * time_count;
  tz_ttinfo = tz_type_index + time_count;
  tz_abbrs = tz_ttinfo + sizeof (struct ttinfo) * type_count;

  gtz->name = g_steal_pointer (&identifier);
  gtz->t_info = g_array_sized_new (FALSE, TRUE, sizeof (TransitionInfo),
                                   type_count + extra_type_count);
  gtz->transitions = g_array_sized_new (FALSE, TRUE, sizeof (Transition),
                                        time_count + extra_time_count);

  for (index = 0; index < type_count; index++)
    {
      TransitionInfo t_info;
      struct ttinfo info = ((struct ttinfo*)tz_ttinfo)[index];
      t_info.gmt_offset = gint32_from_be (info.tt_gmtoff);
      t_info.is_dst = info.tt_isdst ? TRUE : FALSE;
      t_info.abbrev = g_strdup ((gchar *) &tz_abbrs[info.tt_abbrind]);
      g_array_append_val (gtz->t_info, t_info);
    }

  for (index = 0; index < time_count; index++)
    {
      Transition trans;
      if (header->tzh_version >= '2')
        trans.time = gint64_from_be (((gint64_be*)tz_transitions)[index]);
      else
        trans.time = gint32_from_be (((gint32_be*)tz_transitions)[index]);
      last_explicit_transition_time = trans.time;
      trans.info_index = tz_type_index[index];
      g_assert (trans.info_index >= 0);
      g_assert ((guint) trans.info_index < gtz->t_info->len);
      g_array_append_val (gtz->transitions, trans);
    }

  if (footertz)
    {
      /* Append footer time types.  Don't bother to coalesce
         duplicates with existing time types.  */
      for (index = 0; index < extra_type_count; index++)
        {
          TransitionInfo t_info;
          TransitionInfo *footer_t_info
            = &g_array_index (footertz->t_info, TransitionInfo, index);
          t_info.gmt_offset = footer_t_info->gmt_offset;
          t_info.is_dst = footer_t_info->is_dst;
          t_info.abbrev = g_steal_pointer (&footer_t_info->abbrev);
          g_array_append_val (gtz->t_info, t_info);
        }

      /* Append footer transitions that follow the last explicit
         transition.  */
      for (index = 0; index < extra_time_count; index++)
        {
          Transition *footer_transition
            = &g_array_index (footertz->transitions, Transition, index);
          if (time_count <= 0
              || last_explicit_transition_time < footer_transition->time)
            {
              Transition trans;
              trans.time = footer_transition->time;
              trans.info_index = type_count + footer_transition->info_index;
              g_array_append_val (gtz->transitions, trans);
            }
        }

      g_time_zone_unref (footertz);
    }
}

#elif defined (G_OS_WIN32)

static void
copy_windows_systemtime (SYSTEMTIME *s_time, TimeZoneDate *tzdate)
{
  tzdate->offset
    = s_time->wHour * 3600 + s_time->wMinute * 60 + s_time->wSecond;
  tzdate->mon = s_time->wMonth;
  tzdate->year = s_time->wYear;
  tzdate->wday = s_time->wDayOfWeek ? s_time->wDayOfWeek : 7;

  if (s_time->wYear)
    {
      tzdate->mday = s_time->wDay;
      tzdate->wday = 0;
    }
  else
    tzdate->week = s_time->wDay;
}

/* UTC = local time + bias while local time = UTC + offset */
static gboolean
rule_from_windows_time_zone_info (TimeZoneRule *rule,
                                  TIME_ZONE_INFORMATION *tzi)
{
  gchar *std_name, *dlt_name;

  std_name = g_utf16_to_utf8 ((gunichar2 *)tzi->StandardName, -1, NULL, NULL, NULL);
  if (std_name == NULL)
    return FALSE;

  dlt_name = g_utf16_to_utf8 ((gunichar2 *)tzi->DaylightName, -1, NULL, NULL, NULL);
  if (dlt_name == NULL)
    {
      g_free (std_name);
      return FALSE;
    }

  /* Set offset */
  if (tzi->StandardDate.wMonth)
    {
      rule->std_offset = -(tzi->Bias + tzi->StandardBias) * 60;
      rule->dlt_offset = -(tzi->Bias + tzi->DaylightBias) * 60;
      copy_windows_systemtime (&(tzi->DaylightDate), &(rule->dlt_start));

      copy_windows_systemtime (&(tzi->StandardDate), &(rule->dlt_end));
    }

  else
    {
      rule->std_offset = -tzi->Bias * 60;
      rule->dlt_start.mon = 0;
    }
  strncpy (rule->std_name, std_name, NAME_SIZE - 1);
  strncpy (rule->dlt_name, dlt_name, NAME_SIZE - 1);

  g_free (std_name);
  g_free (dlt_name);

  return TRUE;
}

static gchar*
windows_default_tzname (void)
{
  const gunichar2 *subkey =
    L"SYSTEM\\CurrentControlSet\\Control\\TimeZoneInformation";
  HKEY key;
  gchar *key_name = NULL;
  gunichar2 *key_name_w = NULL;
  if (RegOpenKeyExW (HKEY_LOCAL_MACHINE, subkey, 0,
                     KEY_QUERY_VALUE, &key) == ERROR_SUCCESS)
    {
      DWORD size = 0;
      if (RegQueryValueExW (key, L"TimeZoneKeyName", NULL, NULL,
                            NULL, &size) == ERROR_SUCCESS)
        {
          key_name_w = g_malloc ((gint)size);

          if (key_name_w == NULL ||
              RegQueryValueExW (key, L"TimeZoneKeyName", NULL, NULL,
                                (LPBYTE)key_name_w, &size) != ERROR_SUCCESS)
            {
              g_free (key_name_w);
              key_name = NULL;
            }
          else
            key_name = g_utf16_to_utf8 (key_name_w, -1, NULL, NULL, NULL);
        }
      RegCloseKey (key);
    }
  return key_name;
}

typedef   struct
{
  LONG Bias;
  LONG StandardBias;
  LONG DaylightBias;
  SYSTEMTIME StandardDate;
  SYSTEMTIME DaylightDate;
} RegTZI;

static void
system_time_copy (SYSTEMTIME *orig, SYSTEMTIME *target)
{
  g_return_if_fail (orig != NULL);
  g_return_if_fail (target != NULL);

  target->wYear = orig->wYear;
  target->wMonth = orig->wMonth;
  target->wDayOfWeek = orig->wDayOfWeek;
  target->wDay = orig->wDay;
  target->wHour = orig->wHour;
  target->wMinute = orig->wMinute;
  target->wSecond = orig->wSecond;
  target->wMilliseconds = orig->wMilliseconds;
}

static void
register_tzi_to_tzi (RegTZI *reg, TIME_ZONE_INFORMATION *tzi)
{
  g_return_if_fail (reg != NULL);
  g_return_if_fail (tzi != NULL);
  tzi->Bias = reg->Bias;
  system_time_copy (&(reg->StandardDate), &(tzi->StandardDate));
  tzi->StandardBias = reg->StandardBias;
  system_time_copy (&(reg->DaylightDate), &(tzi->DaylightDate));
  tzi->DaylightBias = reg->DaylightBias;
}

static guint
rules_from_windows_time_zone (const gchar   *identifier,
                              const gchar   *resolved_identifier,
                              TimeZoneRule **rules)
{
  HKEY key;
  gchar *subkey = NULL;
  gchar *subkey_dynamic = NULL;
  const gchar *key_name;
  const gchar *reg_key =
    "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Time Zones\\";
  TIME_ZONE_INFORMATION tzi;
  DWORD size;
  guint rules_num = 0;
  RegTZI regtzi, regtzi_prev;
  WCHAR winsyspath[MAX_PATH];
  gunichar2 *subkey_w, *subkey_dynamic_w;

  subkey_dynamic_w = NULL;

  if (GetSystemDirectoryW (winsyspath, MAX_PATH) == 0)
    return 0;

  g_assert (rules != NULL);

  *rules = NULL;
  key_name = NULL;

  if (!identifier)
    key_name = resolved_identifier;
  else
    key_name = identifier;

  if (!key_name)
    return 0;

  subkey = g_strconcat (reg_key, key_name, NULL);
  subkey_w = g_utf8_to_utf16 (subkey, -1, NULL, NULL, NULL);
  if (subkey_w == NULL)
    goto utf16_conv_failed;

  subkey_dynamic = g_strconcat (subkey, "\\Dynamic DST", NULL);
  subkey_dynamic_w = g_utf8_to_utf16 (subkey_dynamic, -1, NULL, NULL, NULL);
  if (subkey_dynamic_w == NULL)
    goto utf16_conv_failed;

  if (RegOpenKeyExW (HKEY_LOCAL_MACHINE, subkey_w, 0,
                     KEY_QUERY_VALUE, &key) != ERROR_SUCCESS)
      goto utf16_conv_failed;

  size = sizeof tzi.StandardName;

  /* use RegLoadMUIStringW() to query MUI_Std from the registry if possible, otherwise
     fallback to querying Std */
  if (RegLoadMUIStringW (key, L"MUI_Std", tzi.StandardName,
                         size, &size, 0, winsyspath) != ERROR_SUCCESS)
    {
      size = sizeof tzi.StandardName;
      if (RegQueryValueExW (key, L"Std", NULL, NULL,
                            (LPBYTE)&(tzi.StandardName), &size) != ERROR_SUCCESS)
        goto registry_failed;
    }

  size = sizeof tzi.DaylightName;

  /* use RegLoadMUIStringW() to query MUI_Dlt from the registry if possible, otherwise
     fallback to querying Dlt */
  if (RegLoadMUIStringW (key, L"MUI_Dlt", tzi.DaylightName,
                         size, &size, 0, winsyspath) != ERROR_SUCCESS)
    {
      size = sizeof tzi.DaylightName;
      if (RegQueryValueExW (key, L"Dlt", NULL, NULL,
                            (LPBYTE)&(tzi.DaylightName), &size) != ERROR_SUCCESS)
        goto registry_failed;
    }

  RegCloseKey (key);
  if (RegOpenKeyExW (HKEY_LOCAL_MACHINE, subkey_dynamic_w, 0,
                     KEY_QUERY_VALUE, &key) == ERROR_SUCCESS)
    {
      DWORD first, last;
      int year, i;
      wchar_t s[12];

      size = sizeof first;
      if (RegQueryValueExW (key, L"FirstEntry", NULL, NULL,
                            (LPBYTE) &first, &size) != ERROR_SUCCESS)
        goto registry_failed;

      size = sizeof last;
      if (RegQueryValueExW (key, L"LastEntry", NULL, NULL,
                            (LPBYTE) &last, &size) != ERROR_SUCCESS)
        goto registry_failed;

      rules_num = last - first + 2;
      *rules = g_new0 (TimeZoneRule, rules_num);

      for (year = first, i = 0; *rules != NULL && year <= last; year++)
        {
          gboolean failed = FALSE;
          swprintf_s (s, 11, L"%d", year);

          if (!failed)
            {
              size = sizeof regtzi;
              if (RegQueryValueExW (key, s, NULL, NULL,
                                    (LPBYTE) &regtzi, &size) != ERROR_SUCCESS)
                failed = TRUE;
            }

          if (failed)
            {
              g_free (*rules);
              *rules = NULL;
              break;
            }

          if (year > first && memcmp (&regtzi_prev, &regtzi, sizeof regtzi) == 0)
              continue;
          else
            memcpy (&regtzi_prev, &regtzi, sizeof regtzi);

          register_tzi_to_tzi (&regtzi, &tzi);

          if (!rule_from_windows_time_zone_info (&(*rules)[i], &tzi))
            {
              g_free (*rules);
              *rules = NULL;
              break;
            }

          (*rules)[i++].start_year = year;
        }

      rules_num = i + 1;

registry_failed:
      RegCloseKey (key);
    }
  else if (RegOpenKeyExW (HKEY_LOCAL_MACHINE, subkey_w, 0,
                          KEY_QUERY_VALUE, &key) == ERROR_SUCCESS)
    {
      size = sizeof regtzi;
      if (RegQueryValueExW (key, L"TZI", NULL, NULL,
                            (LPBYTE) &regtzi, &size) == ERROR_SUCCESS)
        {
          rules_num = 2;
          *rules = g_new0 (TimeZoneRule, 2);
          register_tzi_to_tzi (&regtzi, &tzi);

          if (!rule_from_windows_time_zone_info (&(*rules)[0], &tzi))
            {
              g_free (*rules);
              *rules = NULL;
            }
        }

      RegCloseKey (key);
    }

utf16_conv_failed:
  g_free (subkey_dynamic_w);
  g_free (subkey_dynamic);
  g_free (subkey_w);
  g_free (subkey);

  if (*rules)
    {
      (*rules)[0].start_year = MIN_TZYEAR;
      if ((*rules)[rules_num - 2].start_year < MAX_TZYEAR)
        (*rules)[rules_num - 1].start_year = MAX_TZYEAR;
      else
        (*rules)[rules_num - 1].start_year = (*rules)[rules_num - 2].start_year + 1;

      return rules_num;
    }

  return 0;
}

#endif

static void
find_relative_date (TimeZoneDate *buffer)
{
  guint wday;
  GDate date;
  g_date_clear (&date, 1);
  wday = buffer->wday;

  /* Get last day if last is needed, first day otherwise */
  if (buffer->mon == 13 || buffer->mon == 14) /* Julian Date */
    {
      g_date_set_dmy (&date, 1, 1, buffer->year);
      if (wday >= 59 && buffer->mon == 13 && g_date_is_leap_year (buffer->year))
        g_date_add_days (&date, wday);
      else
        g_date_add_days (&date, wday - 1);
      buffer->mon = (int) g_date_get_month (&date);
      buffer->mday = (int) g_date_get_day (&date);
      buffer->wday = 0;
    }
  else /* M.W.D */
    {
      guint days;
#ifdef GSTREAMER_LITE
      guint days_in_month = g_date_get_days_in_month (buffer->mon, buffer->year);
#else // GSTREAMER_LITE
      guint days_in_month = g_date_days_in_month (buffer->mon, buffer->year);
#endif // GSTREAMER_LITE
      GDateWeekday first_wday;

      g_date_set_dmy (&date, 1, buffer->mon, buffer->year);
      first_wday = g_date_get_weekday (&date);

      if (first_wday > wday)
        ++(buffer->week);
      /* week is 1 <= w <= 5, we need 0-based */
      days = 7 * (buffer->week - 1) + wday - first_wday;

      /* "days" is a 0-based offset from the 1st of the month.
       * Adding days == days_in_month would bring us into the next month,
       * hence the ">=" instead of just ">".
       */
      while (days >= days_in_month)
        days -= 7;

      g_date_add_days (&date, days);

      buffer->mday = g_date_get_day (&date);
    }
}

/* Offset is previous offset of local time. Returns 0 if month is 0 */
static gint64
boundary_for_year (TimeZoneDate *boundary,
                   gint          year,
                   gint32        offset)
{
  TimeZoneDate buffer;
  GDate date;
  const guint64 unix_epoch_start = 719163L;
  const guint64 seconds_per_day = 86400L;

  if (!boundary->mon)
    return 0;
  buffer = *boundary;

  if (boundary->year == 0)
    {
      buffer.year = year;

      if (buffer.wday)
        find_relative_date (&buffer);
    }

  g_assert (buffer.year == year);
  g_date_clear (&date, 1);
  g_date_set_dmy (&date, buffer.mday, buffer.mon, buffer.year);
  return ((g_date_get_julian (&date) - unix_epoch_start) * seconds_per_day +
          buffer.offset - offset);
}

static void
fill_transition_info_from_rule (TransitionInfo *info,
                                TimeZoneRule   *rule,
                                gboolean        is_dst)
{
  gint offset = is_dst ? rule->dlt_offset : rule->std_offset;
  gchar *name = is_dst ? rule->dlt_name : rule->std_name;

  info->gmt_offset = offset;
  info->is_dst = is_dst;

  if (name)
    info->abbrev = g_strdup (name);

  else
    info->abbrev = g_strdup_printf ("%+03d%02d",
                                      (int) offset / 3600,
                                      (int) abs (offset / 60) % 60);
}

static void
init_zone_from_rules (GTimeZone    *gtz,
                      TimeZoneRule *rules,
                      guint         rules_num,
                      gchar        *identifier  /* (transfer full) */)
{
  guint type_count = 0, trans_count = 0, info_index = 0;
  guint ri; /* rule index */
  gboolean skip_first_std_trans = TRUE;
  gint32 last_offset;

  type_count = 0;
  trans_count = 0;

  /* Last rule only contains max year */
  for (ri = 0; ri < rules_num - 1; ri++)
    {
      if (rules[ri].dlt_start.mon || rules[ri].dlt_end.mon)
        {
          guint rulespan = (rules[ri + 1].start_year - rules[ri].start_year);
          guint transitions = rules[ri].dlt_start.mon > 0 ? 1 : 0;
          transitions += rules[ri].dlt_end.mon > 0 ? 1 : 0;
          type_count += rules[ri].dlt_start.mon > 0 ? 2 : 1;
          trans_count += transitions * rulespan;
        }
      else
        type_count++;
    }

  gtz->name = g_steal_pointer (&identifier);
  gtz->t_info = g_array_sized_new (FALSE, TRUE, sizeof (TransitionInfo), type_count);
  gtz->transitions = g_array_sized_new (FALSE, TRUE, sizeof (Transition), trans_count);

  last_offset = rules[0].std_offset;

  for (ri = 0; ri < rules_num - 1; ri++)
    {
      if ((rules[ri].std_offset || rules[ri].dlt_offset) &&
          rules[ri].dlt_start.mon == 0 && rules[ri].dlt_end.mon == 0)
        {
          TransitionInfo std_info;
          /* Standard */
          fill_transition_info_from_rule (&std_info, &(rules[ri]), FALSE);
          g_array_append_val (gtz->t_info, std_info);

          if (ri > 0 &&
              ((rules[ri - 1].dlt_start.mon > 12 &&
                rules[ri - 1].dlt_start.wday > rules[ri - 1].dlt_end.wday) ||
                rules[ri - 1].dlt_start.mon > rules[ri - 1].dlt_end.mon))
            {
              /* The previous rule was a southern hemisphere rule that
                 starts the year with DST, so we need to add a
                 transition to return to standard time */
              guint year = rules[ri].start_year;
              gint64 std_time =  boundary_for_year (&rules[ri].dlt_end,
                                                    year, last_offset);
              Transition std_trans = {std_time, info_index};
              g_array_append_val (gtz->transitions, std_trans);

            }
          last_offset = rules[ri].std_offset;
          ++info_index;
          skip_first_std_trans = TRUE;
         }
      else
        {
          const guint start_year = rules[ri].start_year;
          const guint end_year = rules[ri + 1].start_year;
          gboolean dlt_first;
          guint year;
          TransitionInfo std_info, dlt_info;
          if (rules[ri].dlt_start.mon > 12)
            dlt_first = rules[ri].dlt_start.wday > rules[ri].dlt_end.wday;
          else
            dlt_first = rules[ri].dlt_start.mon > rules[ri].dlt_end.mon;
          /* Standard rules are always even, because before the first
             transition is always standard time, and 0 is even. */
          fill_transition_info_from_rule (&std_info, &(rules[ri]), FALSE);
          fill_transition_info_from_rule (&dlt_info, &(rules[ri]), TRUE);

          g_array_append_val (gtz->t_info, std_info);
          g_array_append_val (gtz->t_info, dlt_info);

          /* Transition dates. We hope that a year which ends daylight
             time in a southern-hemisphere country (i.e., one that
             begins the year in daylight time) will include a rule
             which has only a dlt_end. */
          for (year = start_year; year < end_year; year++)
            {
              gint32 dlt_offset = (dlt_first ? last_offset :
                                   rules[ri].dlt_offset);
              gint32 std_offset = (dlt_first ? rules[ri].std_offset :
                                   last_offset);
              /* NB: boundary_for_year returns 0 if mon == 0 */
              gint64 std_time =  boundary_for_year (&rules[ri].dlt_end,
                                                    year, dlt_offset);
              gint64 dlt_time = boundary_for_year (&rules[ri].dlt_start,
                                                   year, std_offset);
              Transition std_trans = {std_time, info_index};
              Transition dlt_trans = {dlt_time, info_index + 1};
              last_offset = (dlt_first ? rules[ri].dlt_offset :
                             rules[ri].std_offset);
              if (dlt_first)
                {
                  if (skip_first_std_trans)
                    skip_first_std_trans = FALSE;
                  else if (std_time)
                    g_array_append_val (gtz->transitions, std_trans);
                  if (dlt_time)
                    g_array_append_val (gtz->transitions, dlt_trans);
                }
              else
                {
                  if (dlt_time)
                    g_array_append_val (gtz->transitions, dlt_trans);
                  if (std_time)
                    g_array_append_val (gtz->transitions, std_trans);
                }
            }

          info_index += 2;
        }
    }
  if (ri > 0 &&
      ((rules[ri - 1].dlt_start.mon > 12 &&
        rules[ri - 1].dlt_start.wday > rules[ri - 1].dlt_end.wday) ||
       rules[ri - 1].dlt_start.mon > rules[ri - 1].dlt_end.mon))
    {
      /* The previous rule was a southern hemisphere rule that
         starts the year with DST, so we need to add a
         transition to return to standard time */
      TransitionInfo info;
      guint year = rules[ri].start_year;
      Transition trans;
      fill_transition_info_from_rule (&info, &(rules[ri - 1]), FALSE);
      g_array_append_val (gtz->t_info, info);
      trans.time = boundary_for_year (&rules[ri - 1].dlt_end,
                                      year, last_offset);
      trans.info_index = info_index;
      g_array_append_val (gtz->transitions, trans);
     }
}

/*
 * parses date[/time] for parsing TZ environment variable
 *
 * date is either Mm.w.d, Jn or N
 * - m is 1 to 12
 * - w is 1 to 5
 * - d is 0 to 6
 * - n is 1 to 365
 * - N is 0 to 365
 *
 * time is either h or hh[[:]mm[[[:]ss]]]
 *  - h[h] is 0 to 24
 *  - mm is 00 to 59
 *  - ss is 00 to 59
 */
static gboolean
parse_mwd_boundary (gchar **pos, TimeZoneDate *boundary)
{
  gint month, week, day;

  if (**pos == '\0' || **pos < '0' || '9' < **pos)
    return FALSE;

  month = *(*pos)++ - '0';

  if ((month == 1 && **pos >= '0' && '2' >= **pos) ||
      (month == 0 && **pos >= '0' && '9' >= **pos))
    {
      month *= 10;
      month += *(*pos)++ - '0';
    }

  if (*(*pos)++ != '.' || month == 0)
    return FALSE;

  if (**pos == '\0' || **pos < '1' || '5' < **pos)
    return FALSE;

  week = *(*pos)++ - '0';

  if (*(*pos)++ != '.')
    return FALSE;

  if (**pos == '\0' || **pos < '0' || '6' < **pos)
    return FALSE;

  day = *(*pos)++ - '0';

  if (!day)
    day += 7;

  boundary->year = 0;
  boundary->mon = month;
  boundary->week = week;
  boundary->wday = day;
  return TRUE;
}

/*
 * This parses two slightly different ways of specifying
 * the Julian day:
 *
 * - ignore_leap == TRUE
 *
 *   Jn   This specifies the Julian day with n between 1 and 365. Leap days
 *        are not counted. In this format, February 29 can't be represented;
 *        February 28 is day 59, and March 1 is always day 60.
 *
 * - ignore_leap == FALSE
 *
 *   n   This specifies the zero-based Julian day with n between 0 and 365.
 *       February 29 is counted in leap years.
 */
static gboolean
parse_julian_boundary (gchar** pos, TimeZoneDate *boundary,
                       gboolean ignore_leap)
{
  gint day = 0;
  GDate date;

  while (**pos >= '0' && '9' >= **pos)
    {
      day *= 10;
      day += *(*pos)++ - '0';
    }

  if (ignore_leap)
    {
      if (day < 1 || 365 < day)
        return FALSE;
      if (day >= 59)
        day++;
    }
  else
    {
      if (day < 0 || 365 < day)
        return FALSE;
      /* GDate wants day in range 1->366 */
      day++;
    }

  g_date_clear (&date, 1);
  g_date_set_julian (&date, day);
  boundary->year = 0;
  boundary->mon = (int) g_date_get_month (&date);
  boundary->mday = (int) g_date_get_day (&date);
  boundary->wday = 0;

  return TRUE;
}

static gboolean
parse_tz_boundary (const gchar  *identifier,
                   TimeZoneDate *boundary)
{
  gchar *pos;

  pos = (gchar*)identifier;
  /* Month-week-weekday */
  if (*pos == 'M')
    {
      ++pos;
      if (!parse_mwd_boundary (&pos, boundary))
        return FALSE;
    }
  /* Julian date which ignores Feb 29 in leap years */
  else if (*pos == 'J')
    {
      ++pos;
      if (!parse_julian_boundary (&pos, boundary, TRUE))
        return FALSE ;
    }
  /* Julian date which counts Feb 29 in leap years */
  else if (*pos >= '0' && '9' >= *pos)
    {
      if (!parse_julian_boundary (&pos, boundary, FALSE))
        return FALSE;
    }
  else
    return FALSE;

  /* Time */

  if (*pos == '/')
    return parse_constant_offset (pos + 1, &boundary->offset, TRUE);
  else
    {
      boundary->offset = 2 * 60 * 60;
      return *pos == '\0';
    }
}

static guint
create_ruleset_from_rule (TimeZoneRule **rules, TimeZoneRule *rule)
{
  *rules = g_new0 (TimeZoneRule, 2);

  (*rules)[0].start_year = MIN_TZYEAR;
  (*rules)[1].start_year = MAX_TZYEAR;

  (*rules)[0].std_offset = -rule->std_offset;
  (*rules)[0].dlt_offset = -rule->dlt_offset;
  (*rules)[0].dlt_start  = rule->dlt_start;
  (*rules)[0].dlt_end = rule->dlt_end;
  strcpy ((*rules)[0].std_name, rule->std_name);
  strcpy ((*rules)[0].dlt_name, rule->dlt_name);
  return 2;
}

static gboolean
parse_offset (gchar **pos, gint32 *target)
{
  gchar *buffer;
  gchar *target_pos = *pos;
  gboolean ret;

  while (**pos == '+' || **pos == '-' || **pos == ':' ||
         (**pos >= '0' && '9' >= **pos))
    ++(*pos);

  buffer = g_strndup (target_pos, *pos - target_pos);
  ret = parse_constant_offset (buffer, target, FALSE);
  g_free (buffer);

  return ret;
}

static gboolean
parse_identifier_boundary (gchar **pos, TimeZoneDate *target)
{
  gchar *buffer;
  gchar *target_pos = *pos;
  gboolean ret;

  while (**pos != ',' && **pos != '\0')
    ++(*pos);
  buffer = g_strndup (target_pos, *pos - target_pos);
  ret = parse_tz_boundary (buffer, target);
  g_free (buffer);

  return ret;
}

static gboolean
set_tz_name (gchar **pos, gchar *buffer, guint size)
{
  gboolean quoted = **pos == '<';
  gchar *name_pos = *pos;
  guint len;

  if (quoted)
    {
      name_pos++;
      do
        ++(*pos);
      while (g_ascii_isalnum (**pos) || **pos == '-' || **pos == '+');
      if (**pos != '>')
        return FALSE;
    }
  else
    while (g_ascii_isalpha (**pos))
      ++(*pos);

  /* Name should be three or more characters */
  /* FIXME: Should return FALSE if the name is too long.
     This should simplify code later in this function.  */
  if (*pos - name_pos < 3)
    return FALSE;

  memset (buffer, 0, size);
  /* name_pos isn't 0-terminated, so we have to limit the length expressly */
  len = *pos - name_pos > size - 1 ? size - 1 : *pos - name_pos;
  strncpy (buffer, name_pos, len);
  *pos += quoted;
  return TRUE;
}

static gboolean
parse_identifier_boundaries (gchar **pos, TimeZoneRule *tzr)
{
  if (*(*pos)++ != ',')
    return FALSE;

  /* Start date */
  if (!parse_identifier_boundary (pos, &(tzr->dlt_start)) || *(*pos)++ != ',')
    return FALSE;

  /* End date */
  if (!parse_identifier_boundary (pos, &(tzr->dlt_end)))
    return FALSE;
  return TRUE;
}

/*
 * Creates an array of TimeZoneRule from a TZ environment variable
 * type of identifier.  Should free rules afterwards
 */
static guint
rules_from_identifier (const gchar   *identifier,
                       TimeZoneRule **rules)
{
  gchar *pos;
  TimeZoneRule tzr;

  g_assert (rules != NULL);

  *rules = NULL;

  if (!identifier)
    return 0;

  pos = (gchar*)identifier;
  memset (&tzr, 0, sizeof (tzr));
  /* Standard offset */
  if (!(set_tz_name (&pos, tzr.std_name, NAME_SIZE)) ||
      !parse_offset (&pos, &(tzr.std_offset)))
    return 0;

  if (*pos == 0)
    {
      return create_ruleset_from_rule (rules, &tzr);
    }

  /* Format 2 */
  if (!(set_tz_name (&pos, tzr.dlt_name, NAME_SIZE)))
    return 0;
  parse_offset (&pos, &(tzr.dlt_offset));
  if (tzr.dlt_offset == 0) /* No daylight offset given, assume it's 1
                              hour earlier that standard */
    tzr.dlt_offset = tzr.std_offset - 3600;
  if (*pos == '\0')
#ifdef G_OS_WIN32
    /* Windows allows us to use the US DST boundaries if they're not given */
    {
      int i;
      guint rules_num = 0;

      /* Use US rules, Windows' default is Pacific Standard Time */
      if ((rules_num = rules_from_windows_time_zone ("Pacific Standard Time",
                                                     NULL,
                                                     rules)))
        {
          for (i = 0; i < rules_num - 1; i++)
            {
              (*rules)[i].std_offset = - tzr.std_offset;
              (*rules)[i].dlt_offset = - tzr.dlt_offset;
              strcpy ((*rules)[i].std_name, tzr.std_name);
              strcpy ((*rules)[i].dlt_name, tzr.dlt_name);
            }

          return rules_num;
        }
      else
        return 0;
    }
#else
  return 0;
#endif
  /* Start and end required (format 2) */
  if (!parse_identifier_boundaries (&pos, &tzr))
    return 0;

  return create_ruleset_from_rule (rules, &tzr);
}

#ifdef G_OS_UNIX
static GTimeZone *
parse_footertz (const gchar *footer, size_t footerlen)
{
  gchar *tzstring = g_strndup (footer + 1, footerlen - 2);
  GTimeZone *footertz = NULL;

  /* FIXME: The allocation for tzstring could be avoided by
     passing a gsize identifier_len argument to rules_from_identifier
     and changing the code in that function to stop assuming that
     identifier is nul-terminated.  */
  TimeZoneRule *rules;
  guint rules_num = rules_from_identifier (tzstring, &rules);

  g_free (tzstring);
  if (rules_num > 1)
    {
      footertz = g_slice_new0 (GTimeZone);
      init_zone_from_rules (footertz, rules, rules_num, NULL);
      footertz->ref_count++;
    }
  g_free (rules);
  return footertz;
}
#endif

/* Construction {{{1 */
/**
 * g_time_zone_new:
 * @identifier: (nullable): a timezone identifier
 *
 * Creates a #GTimeZone corresponding to @identifier.
 *
 * @identifier can either be an RFC3339/ISO 8601 time offset or
 * something that would pass as a valid value for the `TZ` environment
 * variable (including %NULL).
 *
 * In Windows, @identifier can also be the unlocalized name of a time
 * zone for standard time, for example "Pacific Standard Time".
 *
 * Valid RFC3339 time offsets are '"Z"' (for UTC) or
 * '"+|-hh:mm"'.  ISO 8601 additionally specifies
 * '"+|-hhmm"' and '"+|-hh"'.  Offsets are
 * time values to be added to Coordinated Universal Time (UTC) to get
 * the local time.
 *
 * In UNIX, the 'TZ' environment variable typically corresponds
 * to the name of a file in the zoneinfo database, or string in
 * "std offset [dst [offset],start[/time],end[/time]]" (POSIX) format.
 * There  are  no spaces in the specification. The name of standard
 * and daylight savings time zone must be three or more alphabetic
 * characters. Offsets are time values to be added to local time to
 * get Coordinated Universal Time (UTC) and should be
 * '"[+|-]hh[[:]mm[:ss]]"'.  Dates are either
 * '"Jn"' (Julian day with n between 1 and 365, leap
 * years not counted), '"n"' (zero-based Julian day
 * with n between 0 and 365) or '"Mm.w.d"' (day d
 * (0 <= d <= 6) of week w (1 <= w <= 5) of month m (1 <= m <= 12), day
 * 0 is a Sunday).  Times are in local wall clock time, the default is
 * 02:00:00.
 *
 * In Windows, the "tzn[+|-]hh[:mm[:ss]][dzn]" format is used, but also
 * accepts POSIX format.  The Windows format uses US rules for all time
 * zones; daylight savings time is 60 minutes behind the standard time
 * with date and time of change taken from Pacific Standard Time.
 * Offsets are time values to be added to the local time to get
 * Coordinated Universal Time (UTC).
 *
 * g_time_zone_new_local() calls this function with the value of the
 * 'TZ' environment variable. This function itself is independent of
 * the value of 'TZ', but if @identifier is %NULL then '/etc/localtime'
 * will be consulted to discover the correct time zone on UNIX and the
 * registry will be consulted or GetTimeZoneInformation() will be used
 * to get the local time zone on Windows.
 *
 * If intervals are not available, only time zone rules from `TZ`
 * environment variable or other means, then they will be computed
 * from year 1900 to 2037.  If the maximum year for the rules is
 * available and it is greater than 2037, then it will followed
 * instead.
 *
 * See
 * [RFC3339 5.6](http://tools.ietf.org/html/rfc3339#section-5.6)
 * for a precise definition of valid RFC3339 time offsets
 * (the `time-offset` expansion) and ISO 8601 for the
 * full list of valid time offsets.  See
 * [The GNU C Library manual](http://www.gnu.org/s/libc/manual/html_node/TZ-Variable.html)
 * for an explanation of the possible
 * values of the `TZ` environment variable. See
 * [Microsoft Time Zone Index Values](http://msdn.microsoft.com/en-us/library/ms912391%28v=winembedded.11%29.aspx)
 * for the list of time zones on Windows.
 *
 * You should release the return value by calling g_time_zone_unref()
 * when you are done with it.
 *
 * Returns: the requested timezone
 *
 * Since: 2.26
 **/
GTimeZone *
g_time_zone_new (const gchar *identifier)
{
  GTimeZone *tz = NULL;
  TimeZoneRule *rules;
  gint rules_num;
  gchar *resolved_identifier = NULL;

  if (identifier)
    {
      G_LOCK (time_zones);
      if (time_zones == NULL)
        time_zones = g_hash_table_new (g_str_hash, g_str_equal);

      tz = g_hash_table_lookup (time_zones, identifier);
      if (tz)
        {
          g_atomic_int_inc (&tz->ref_count);
          G_UNLOCK (time_zones);
          return tz;
        }
      else
        resolved_identifier = g_strdup (identifier);
    }
  else
    {
      G_LOCK (tz_default);
#ifdef G_OS_UNIX
      resolved_identifier = zone_identifier_unix ();
#elif defined (G_OS_WIN32)
      resolved_identifier = windows_default_tzname ();
#endif
      if (tz_default)
        {
          /* Flush default if changed. If the identifier couldn’t be resolved,
           * we’re going to fall back to UTC eventually, so don’t clear out the
           * cache if it’s already UTC. */
          if (!(resolved_identifier == NULL && g_str_equal (tz_default->name, "UTC")) &&
              g_strcmp0 (tz_default->name, resolved_identifier) != 0)
            {
              g_clear_pointer (&tz_default, g_time_zone_unref);
            }
          else
            {
              tz = g_time_zone_ref (tz_default);
              G_UNLOCK (tz_default);

              g_free (resolved_identifier);
              return tz;
            }
        }
    }

  tz = g_slice_new0 (GTimeZone);
  tz->ref_count = 0;

  zone_for_constant_offset (tz, identifier);

  if (tz->t_info == NULL &&
      (rules_num = rules_from_identifier (identifier, &rules)))
    {
      init_zone_from_rules (tz, rules, rules_num, g_steal_pointer (&resolved_identifier));
      g_free (rules);
    }

  if (tz->t_info == NULL)
    {
#ifdef G_OS_UNIX
      GBytes *zoneinfo = zone_info_unix (identifier, resolved_identifier);
      if (zoneinfo != NULL)
        {
          init_zone_from_iana_info (tz, zoneinfo, g_steal_pointer (&resolved_identifier));
          g_bytes_unref (zoneinfo);
        }
#elif defined (G_OS_WIN32)
      if ((rules_num = rules_from_windows_time_zone (identifier,
                                                     resolved_identifier,
                                                     &rules)))
        {
          init_zone_from_rules (tz, rules, rules_num, g_steal_pointer (&resolved_identifier));
          g_free (rules);
        }
#endif
    }

#if defined (G_OS_WIN32)
  if (tz->t_info == NULL)
    {
      if (identifier == NULL)
        {
          TIME_ZONE_INFORMATION tzi;

          if (GetTimeZoneInformation (&tzi) != TIME_ZONE_ID_INVALID)
            {
              rules = g_new0 (TimeZoneRule, 2);

              if (rule_from_windows_time_zone_info (&rules[0], &tzi))
                {
                  memset (rules[0].std_name, 0, NAME_SIZE);
                  memset (rules[0].dlt_name, 0, NAME_SIZE);

                  rules[0].start_year = MIN_TZYEAR;
                  rules[1].start_year = MAX_TZYEAR;

                  init_zone_from_rules (tz, rules, 2, g_steal_pointer (&resolved_identifier));
                }

              g_free (rules);
            }
        }
    }
#endif

  g_free (resolved_identifier);

  /* Always fall back to UTC. */
  if (tz->t_info == NULL)
    zone_for_constant_offset (tz, "UTC");

  g_assert (tz->name != NULL);
  g_assert (tz->t_info != NULL);

  if (tz->t_info != NULL)
    {
      if (identifier)
        g_hash_table_insert (time_zones, tz->name, tz);
      else if (tz->name)
        {
          /* Caching reference */
          g_atomic_int_inc (&tz->ref_count);
          tz_default = tz;
        }
    }
  g_atomic_int_inc (&tz->ref_count);

  if (identifier)
    G_UNLOCK (time_zones);
  else
    G_UNLOCK (tz_default);

  return tz;
}

/**
 * g_time_zone_new_utc:
 *
 * Creates a #GTimeZone corresponding to UTC.
 *
 * This is equivalent to calling g_time_zone_new() with a value like
 * "Z", "UTC", "+00", etc.
 *
 * You should release the return value by calling g_time_zone_unref()
 * when you are done with it.
 *
 * Returns: the universal timezone
 *
 * Since: 2.26
 **/
GTimeZone *
g_time_zone_new_utc (void)
{
  static GTimeZone *utc = NULL;
  static gsize initialised;

  if (g_once_init_enter (&initialised))
    {
      utc = g_time_zone_new ("UTC");
      g_once_init_leave (&initialised, TRUE);
    }

  return g_time_zone_ref (utc);
}

/**
 * g_time_zone_new_local:
 *
 * Creates a #GTimeZone corresponding to local time.  The local time
 * zone may change between invocations to this function; for example,
 * if the system administrator changes it.
 *
 * This is equivalent to calling g_time_zone_new() with the value of
 * the `TZ` environment variable (including the possibility of %NULL).
 *
 * You should release the return value by calling g_time_zone_unref()
 * when you are done with it.
 *
 * Returns: the local timezone
 *
 * Since: 2.26
 **/
GTimeZone *
g_time_zone_new_local (void)
{
  const gchar *tzenv = g_getenv ("TZ");
  GTimeZone *tz;

  G_LOCK (tz_local);

  /* Is time zone changed and must be flushed? */
  if (tz_local && g_strcmp0 (g_time_zone_get_identifier (tz_local), tzenv))
    g_clear_pointer (&tz_local, g_time_zone_unref);

  if (tz_local == NULL)
    tz_local = g_time_zone_new (tzenv);

  tz = g_time_zone_ref (tz_local);

  G_UNLOCK (tz_local);

  return tz;
}

/**
 * g_time_zone_new_offset:
 * @seconds: offset to UTC, in seconds
 *
 * Creates a #GTimeZone corresponding to the given constant offset from UTC,
 * in seconds.
 *
 * This is equivalent to calling g_time_zone_new() with a string in the form
 * `[+|-]hh[:mm[:ss]]`.
 *
 * Returns: (transfer full): a timezone at the given offset from UTC
 * Since: 2.58
 */
GTimeZone *
g_time_zone_new_offset (gint32 seconds)
{
  GTimeZone *tz = NULL;
  gchar *identifier = NULL;

  /* Seemingly, we should be using @seconds directly to set the
   * #TransitionInfo.gmt_offset to avoid all this string building and parsing.
   * However, we always need to set the #GTimeZone.name to a constructed
   * string anyway, so we might as well reuse its code. */
  identifier = g_strdup_printf ("%c%02u:%02u:%02u",
                                (seconds >= 0) ? '+' : '-',
                                (ABS (seconds) / 60) / 60,
                                (ABS (seconds) / 60) % 60,
                                ABS (seconds) % 60);
  tz = g_time_zone_new (identifier);
  g_free (identifier);

  g_assert (g_time_zone_get_offset (tz, 0) == seconds);

  return tz;
}

#define TRANSITION(n)         g_array_index (tz->transitions, Transition, n)
#define TRANSITION_INFO(n)    g_array_index (tz->t_info, TransitionInfo, n)

/* Internal helpers {{{1 */
/* NB: Interval 0 is before the first transition, so there's no
 * transition structure to point to which TransitionInfo to
 * use. Rule-based zones are set up so that TI 0 is always standard
 * time (which is what's in effect before Daylight time got started
 * in the early 20th century), but IANA tzfiles don't follow that
 * convention. The tzfile documentation says to use the first
 * standard-time (i.e., non-DST) tinfo, so that's what we do.
 */
inline static const TransitionInfo*
interval_info (GTimeZone *tz,
               guint      interval)
{
  guint index;
  g_return_val_if_fail (tz->t_info != NULL, NULL);
  if (interval && tz->transitions && interval <= tz->transitions->len)
    index = (TRANSITION(interval - 1)).info_index;
  else
    {
      for (index = 0; index < tz->t_info->len; index++)
        {
          TransitionInfo *tzinfo = &(TRANSITION_INFO(index));
          if (!tzinfo->is_dst)
            return tzinfo;
        }
      index = 0;
    }

  return &(TRANSITION_INFO(index));
}

inline static gint64
interval_start (GTimeZone *tz,
                guint      interval)
{
  if (!interval || tz->transitions == NULL || tz->transitions->len == 0)
    return G_MININT64;
  if (interval > tz->transitions->len)
    interval = tz->transitions->len;
  return (TRANSITION(interval - 1)).time;
}

inline static gint64
interval_end (GTimeZone *tz,
              guint      interval)
{
  if (tz->transitions && interval < tz->transitions->len)
    {
      gint64 lim = (TRANSITION(interval)).time;
      return lim - (lim != G_MININT64);
    }
  return G_MAXINT64;
}

inline static gint32
interval_offset (GTimeZone *tz,
                 guint      interval)
{
  g_return_val_if_fail (tz->t_info != NULL, 0);
  return interval_info (tz, interval)->gmt_offset;
}

inline static gboolean
interval_isdst (GTimeZone *tz,
                guint      interval)
{
  g_return_val_if_fail (tz->t_info != NULL, 0);
  return interval_info (tz, interval)->is_dst;
}


inline static gchar*
interval_abbrev (GTimeZone *tz,
                  guint      interval)
{
  g_return_val_if_fail (tz->t_info != NULL, 0);
  return interval_info (tz, interval)->abbrev;
}

inline static gint64
interval_local_start (GTimeZone *tz,
                      guint      interval)
{
  if (interval)
    return interval_start (tz, interval) + interval_offset (tz, interval);

  return G_MININT64;
}

inline static gint64
interval_local_end (GTimeZone *tz,
                    guint      interval)
{
  if (tz->transitions && interval < tz->transitions->len)
    return interval_end (tz, interval) + interval_offset (tz, interval);

  return G_MAXINT64;
}

static gboolean
interval_valid (GTimeZone *tz,
                guint      interval)
{
  if ( tz->transitions == NULL)
    return interval == 0;
  return interval <= tz->transitions->len;
}

/* g_time_zone_find_interval() {{{1 */

/**
 * g_time_zone_adjust_time:
 * @tz: a #GTimeZone
 * @type: the #GTimeType of @time_
 * @time_: a pointer to a number of seconds since January 1, 1970
 *
 * Finds an interval within @tz that corresponds to the given @time_,
 * possibly adjusting @time_ if required to fit into an interval.
 * The meaning of @time_ depends on @type.
 *
 * This function is similar to g_time_zone_find_interval(), with the
 * difference that it always succeeds (by making the adjustments
 * described below).
 *
 * In any of the cases where g_time_zone_find_interval() succeeds then
 * this function returns the same value, without modifying @time_.
 *
 * This function may, however, modify @time_ in order to deal with
 * non-existent times.  If the non-existent local @time_ of 02:30 were
 * requested on March 14th 2010 in Toronto then this function would
 * adjust @time_ to be 03:00 and return the interval containing the
 * adjusted time.
 *
 * Returns: the interval containing @time_, never -1
 *
 * Since: 2.26
 **/
gint
g_time_zone_adjust_time (GTimeZone *tz,
                         GTimeType  type,
                         gint64    *time_)
{
  guint i, intervals;
  gboolean interval_is_dst;

  if (tz->transitions == NULL)
    return 0;

  intervals = tz->transitions->len;

  /* find the interval containing *time UTC
   * TODO: this could be binary searched (or better) */
  for (i = 0; i <= intervals; i++)
    if (*time_ <= interval_end (tz, i))
      break;

  g_assert (interval_start (tz, i) <= *time_ && *time_ <= interval_end (tz, i));

  if (type != G_TIME_TYPE_UNIVERSAL)
    {
      if (*time_ < interval_local_start (tz, i))
        /* if time came before the start of this interval... */
        {
          i--;

          /* if it's not in the previous interval... */
          if (*time_ > interval_local_end (tz, i))
            {
              /* it doesn't exist.  fast-forward it. */
              i++;
              *time_ = interval_local_start (tz, i);
            }
        }

      else if (*time_ > interval_local_end (tz, i))
        /* if time came after the end of this interval... */
        {
          i++;

          /* if it's not in the next interval... */
          if (*time_ < interval_local_start (tz, i))
            /* it doesn't exist.  fast-forward it. */
            *time_ = interval_local_start (tz, i);
        }

      else
        {
          interval_is_dst = interval_isdst (tz, i);
          if ((interval_is_dst && type != G_TIME_TYPE_DAYLIGHT) ||
              (!interval_is_dst && type == G_TIME_TYPE_DAYLIGHT))
            {
              /* it's in this interval, but dst flag doesn't match.
               * check neighbours for a better fit. */
              if (i && *time_ <= interval_local_end (tz, i - 1))
                i--;

              else if (i < intervals &&
                       *time_ >= interval_local_start (tz, i + 1))
                i++;
            }
        }
    }

  return i;
}

/**
 * g_time_zone_find_interval:
 * @tz: a #GTimeZone
 * @type: the #GTimeType of @time_
 * @time_: a number of seconds since January 1, 1970
 *
 * Finds an interval within @tz that corresponds to the given @time_.
 * The meaning of @time_ depends on @type.
 *
 * If @type is %G_TIME_TYPE_UNIVERSAL then this function will always
 * succeed (since universal time is monotonic and continuous).
 *
 * Otherwise @time_ is treated as local time.  The distinction between
 * %G_TIME_TYPE_STANDARD and %G_TIME_TYPE_DAYLIGHT is ignored except in
 * the case that the given @time_ is ambiguous.  In Toronto, for example,
 * 01:30 on November 7th 2010 occurred twice (once inside of daylight
 * savings time and the next, an hour later, outside of daylight savings
 * time).  In this case, the different value of @type would result in a
 * different interval being returned.
 *
 * It is still possible for this function to fail.  In Toronto, for
 * example, 02:00 on March 14th 2010 does not exist (due to the leap
 * forward to begin daylight savings time).  -1 is returned in that
 * case.
 *
 * Returns: the interval containing @time_, or -1 in case of failure
 *
 * Since: 2.26
 */
gint
g_time_zone_find_interval (GTimeZone *tz,
                           GTimeType  type,
                           gint64     time_)
{
  guint i, intervals;
  gboolean interval_is_dst;

  if (tz->transitions == NULL)
    return 0;
  intervals = tz->transitions->len;
  for (i = 0; i <= intervals; i++)
    if (time_ <= interval_end (tz, i))
      break;

  if (type == G_TIME_TYPE_UNIVERSAL)
    return i;

  if (time_ < interval_local_start (tz, i))
    {
      if (time_ > interval_local_end (tz, --i))
        return -1;
    }

  else if (time_ > interval_local_end (tz, i))
    {
      if (time_ < interval_local_start (tz, ++i))
        return -1;
    }

  else
    {
      interval_is_dst = interval_isdst (tz, i);
      if  ((interval_is_dst && type != G_TIME_TYPE_DAYLIGHT) ||
           (!interval_is_dst && type == G_TIME_TYPE_DAYLIGHT))
        {
          if (i && time_ <= interval_local_end (tz, i - 1))
            i--;

          else if (i < intervals && time_ >= interval_local_start (tz, i + 1))
            i++;
        }
    }

  return i;
}

/* Public API accessors {{{1 */

/**
 * g_time_zone_get_abbreviation:
 * @tz: a #GTimeZone
 * @interval: an interval within the timezone
 *
 * Determines the time zone abbreviation to be used during a particular
 * @interval of time in the time zone @tz.
 *
 * For example, in Toronto this is currently "EST" during the winter
 * months and "EDT" during the summer months when daylight savings time
 * is in effect.
 *
 * Returns: the time zone abbreviation, which belongs to @tz
 *
 * Since: 2.26
 **/
const gchar *
g_time_zone_get_abbreviation (GTimeZone *tz,
                              gint       interval)
{
  g_return_val_if_fail (interval_valid (tz, (guint)interval), NULL);

  return interval_abbrev (tz, (guint)interval);
}

/**
 * g_time_zone_get_offset:
 * @tz: a #GTimeZone
 * @interval: an interval within the timezone
 *
 * Determines the offset to UTC in effect during a particular @interval
 * of time in the time zone @tz.
 *
 * The offset is the number of seconds that you add to UTC time to
 * arrive at local time for @tz (ie: negative numbers for time zones
 * west of GMT, positive numbers for east).
 *
 * Returns: the number of seconds that should be added to UTC to get the
 *          local time in @tz
 *
 * Since: 2.26
 **/
gint32
g_time_zone_get_offset (GTimeZone *tz,
                        gint       interval)
{
  g_return_val_if_fail (interval_valid (tz, (guint)interval), 0);

  return interval_offset (tz, (guint)interval);
}

/**
 * g_time_zone_is_dst:
 * @tz: a #GTimeZone
 * @interval: an interval within the timezone
 *
 * Determines if daylight savings time is in effect during a particular
 * @interval of time in the time zone @tz.
 *
 * Returns: %TRUE if daylight savings time is in effect
 *
 * Since: 2.26
 **/
gboolean
g_time_zone_is_dst (GTimeZone *tz,
                    gint       interval)
{
  g_return_val_if_fail (interval_valid (tz, interval), FALSE);

  if (tz->transitions == NULL)
    return FALSE;

  return interval_isdst (tz, (guint)interval);
}

/**
 * g_time_zone_get_identifier:
 * @tz: a #GTimeZone
 *
 * Get the identifier of this #GTimeZone, as passed to g_time_zone_new().
 * If the identifier passed at construction time was not recognised, `UTC` will
 * be returned. If it was %NULL, the identifier of the local timezone at
 * construction time will be returned.
 *
 * The identifier will be returned in the same format as provided at
 * construction time: if provided as a time offset, that will be returned by
 * this function.
 *
 * Returns: identifier for this timezone
 * Since: 2.58
 */
const gchar *
g_time_zone_get_identifier (GTimeZone *tz)
{
  g_return_val_if_fail (tz != NULL, NULL);

  return tz->name;
}

/* Epilogue {{{1 */
/* vim:set foldmethod=marker: */
