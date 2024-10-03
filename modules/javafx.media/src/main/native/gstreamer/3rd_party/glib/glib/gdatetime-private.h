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

#pragma once

#include "glib.h"

G_BEGIN_DECLS

/**
 * GEraDate:
 * @type: the type of date
 * @year: year of the date, in the Gregorian calendar
 * @month: month of the date, in the Gregorian calendar
 * @day: day of the date, in the Gregorian calendar
 *
 * A date from a #GEraDescriptionSegment.
 *
 * If @type is %G_ERA_DATE_SET, @year, @month and @day are valid. Otherwise,
 * they are undefined.
 *
 * Since: 2.80
 */
typedef struct {
  enum {
    G_ERA_DATE_SET,
    G_ERA_DATE_PLUS_INFINITY,
    G_ERA_DATE_MINUS_INFINITY,
  } type;
  int year;
  int month;
  int day;
} GEraDate;

int _g_era_date_compare (const GEraDate *date1,
                         const GEraDate *date2);

/**
 * GEraDescriptionSegment:
 * @ref_count: reference count
 * @direction_multiplier: `-1` or `1` depending on the order of @start_date and
 *   @end_date
 * @offset: offset of the first year in the era
 * @start_date: start date (in the Gregorian calendar) of the era
 * @end_date: end date (in the Gregorian calendar) of the era
 * @era_name: (not nullable): name of the era
 * @era_format: (not nullable): format string to use for `%EY`
 *
 * A segment of an `ERA` description string, describing a single era. See
 * [`nl_langinfo(3)`](man:nl_langinfo(3)).
 *
 * Since: 2.80
 */
typedef struct {
  gatomicrefcount ref_count;
  int direction_multiplier;
  guint64 offset;
  GEraDate start_date;  /* inclusive */
  GEraDate end_date;  /* inclusive */
  char *era_name;  /* UTF-8 encoded */
  char *era_format;  /* UTF-8 encoded */
} GEraDescriptionSegment;

GPtrArray *_g_era_description_parse (const char *desc);

GEraDescriptionSegment *_g_era_description_segment_ref (GEraDescriptionSegment *segment);
void _g_era_description_segment_unref (GEraDescriptionSegment *segment);

G_END_DECLS
