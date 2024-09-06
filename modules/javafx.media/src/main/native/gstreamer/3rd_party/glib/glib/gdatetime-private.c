/*
 * Copyright 2023 GNOME Foundation Inc.
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
 * Authors:
 *  - Philip Withnall <pwithnall@gnome.org>
 */

#include "glib.h"
#include "gdatetime-private.h"

/**
 * _g_era_date_compare:
 * @date1: first date
 * @date2: second date
 *
 * Compare two #GEraDates for ordering, taking into account negative and
 * positive infinity.
 *
 * Returns: strcmp()-style integer, `<0` indicates `date1 < date2`, `0`
 *   indicates `date1 == date2`, `>0` indicates `date1 > date2`
 * Since: 2.80
 */
int
_g_era_date_compare (const GEraDate *date1,
                     const GEraDate *date2)
{
  if (date1->type == G_ERA_DATE_SET &&
      date2->type == G_ERA_DATE_SET)
    {
      if (date1->year != date2->year)
        return date1->year - date2->year;
      if (date1->month != date2->month)
        return date1->month - date2->month;
      return date1->day - date2->day;
    }

  if (date1->type == date2->type)
    return 0;

  if (date1->type == G_ERA_DATE_MINUS_INFINITY || date2->type == G_ERA_DATE_PLUS_INFINITY)
    return -1;
  if (date1->type == G_ERA_DATE_PLUS_INFINITY || date2->type == G_ERA_DATE_MINUS_INFINITY)
    return 1;

  g_assert_not_reached ();
}

static gboolean
parse_era_date (const char *str,
                const char *endptr,
                GEraDate   *out_date)
{
  const char *str_endptr = NULL;
  int year_multiplier;
  guint64 year, month, day;

  year_multiplier = (str[0] == '-') ? -1 : 1;
  if (str[0] == '-' || str[0] == '+')
    str++;

  year = g_ascii_strtoull (str, (gchar **) &str_endptr, 10);
  g_assert (str_endptr <= endptr);
  if (str_endptr == endptr || *str_endptr != '/' || year > G_MAXINT)
    return FALSE;
  str = str_endptr + 1;

  month = g_ascii_strtoull (str, (gchar **) &str_endptr, 10);
  g_assert (str_endptr <= endptr);
  if (str_endptr == endptr || *str_endptr != '/' || month < 1 || month > 12)
    return FALSE;
  str = str_endptr + 1;

  day = g_ascii_strtoull (str, (gchar **) &str_endptr, 10);
  g_assert (str_endptr <= endptr);
  if (str_endptr != endptr || day < 1 || day > 31)
    return FALSE;

  /* Success */
  out_date->type = G_ERA_DATE_SET;
  out_date->year = year_multiplier * year;
  out_date->month = month;
  out_date->day = day;

  return TRUE;
}

/**
 * _g_era_description_segment_ref:
 * @segment: a #GEraDescriptionSegment
 *
 * Increase the ref count of @segment.
 *
 * Returns: (transfer full): @segment
 * Since: 2.80
 */
GEraDescriptionSegment *
_g_era_description_segment_ref (GEraDescriptionSegment *segment)
{
  g_atomic_ref_count_inc (&segment->ref_count);
  return segment;
}

/**
 * _g_era_description_segment_unref:
 * @segment: (transfer full): a #GEraDescriptionSegment to unref
 *
 * Decreases the ref count of @segment.
 *
 * Since: 2.80
 */
void
_g_era_description_segment_unref (GEraDescriptionSegment *segment)
{
  if (g_atomic_ref_count_dec (&segment->ref_count))
    {
      g_free (segment->era_format);
      g_free (segment->era_name);
      g_free (segment);
    }
}

/**
 * _g_era_description_parse:
 * @desc: an `ERA` description string from `nl_langinfo()`
 *
 * Parse an ERA description string. See [`nl_langinfo(3)`](man:nl_langinfo(3)).
 *
 * Example description string for th_TH.UTF-8:
 * ```
 * +:1:-543/01/01:+*:พ.ศ.:%EC %Ey
 * ```
 *
 * @desc must be in UTF-8, so all conversion from the locale encoding must
 * happen before this function is called. The resulting `era_name` and
 * `era_format` in the returned segments will be in UTF-8.
 *
 * Returns: (transfer full) (nullable) (element-type GEraDescriptionSegment):
 *   array of one or more parsed era segments, or %NULL if parsing failed
 * Since: 2.80
 */
GPtrArray *
_g_era_description_parse (const char *desc)
{
  GPtrArray *segments = g_ptr_array_new_with_free_func ((GDestroyNotify) _g_era_description_segment_unref);

  for (const char *p = desc; *p != '\0';)
    {
      const char *next_colon, *endptr = NULL;
      GEraDescriptionSegment *segment = NULL;
      char direction;
      guint64 offset;
      GEraDate start_date, end_date;
      char *era_name = NULL, *era_format = NULL;

      /* direction */
      direction = *p++;
      if (direction != '+' && direction != '-')
        goto error;

      if (*p++ != ':')
        goto error;

      /* offset */
      next_colon = strchr (p, ':');
      if (next_colon == NULL)
        goto error;

      offset = g_ascii_strtoull (p, (gchar **) &endptr, 10);
      if (endptr != next_colon)
        goto error;
      p = next_colon + 1;

      /* start_date */
      next_colon = strchr (p, ':');
      if (next_colon == NULL)
        goto error;

      if (!parse_era_date (p, next_colon, &start_date))
        goto error;
      p = next_colon + 1;

      /* end_date */
      next_colon = strchr (p, ':');
      if (next_colon == NULL)
        goto error;

      if (strncmp (p, "-*", 2) == 0)
        end_date.type = G_ERA_DATE_MINUS_INFINITY;
      else if (strncmp (p, "+*", 2) == 0)
        end_date.type = G_ERA_DATE_PLUS_INFINITY;
      else if (!parse_era_date (p, next_colon, &end_date))
        goto error;
      p = next_colon + 1;

      /* era_name */
      next_colon = strchr (p, ':');
      if (next_colon == NULL)
        goto error;

      if (next_colon - p == 0)
        goto error;
      era_name = g_strndup (p, next_colon - p);
      p = next_colon + 1;

      /* era_format; either the final field in the segment (followed by a
       * semicolon) or the description (followed by nul) */
      next_colon = strchr (p, ';');
      if (next_colon == NULL)
        next_colon = p + strlen (p);

      if (next_colon - p == 0)
        {
          g_free (era_name);
          goto error;
        }
      era_format = g_strndup (p, next_colon - p);
      if (*next_colon == ';')
        p = next_colon + 1;
      else
        p = next_colon;

      /* Successfully parsed that segment. */
      segment = g_new0 (GEraDescriptionSegment, 1);
      g_atomic_ref_count_init (&segment->ref_count);
      segment->offset = offset;
      segment->start_date = start_date;
      segment->end_date = end_date;
      segment->direction_multiplier =
          ((_g_era_date_compare (&segment->start_date, &segment->end_date) <= 0) ? 1 : -1) *
          ((direction == '-') ? -1 : 1);
      segment->era_name = g_steal_pointer (&era_name);
      segment->era_format = g_steal_pointer (&era_format);

      g_ptr_array_add (segments, g_steal_pointer (&segment));
    }

  return g_steal_pointer (&segments);

error:
  g_ptr_array_unref (segments);
  return NULL;
}
