/* gvarianttype-private.h
 *
 * Copyright © 2007, 2008 Ryan Lortie
 * Copyright © 2009, 2010 Codethink Limited
 * Copyright © 2024 Christian Hergert
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see <http://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */

#pragma once

#include "gvarianttype.h"

G_BEGIN_DECLS

static inline gboolean
_g_variant_type_equal (const GVariantType *type1,
                       const GVariantType *type2)
{
  const char *str1 = (const char *)type1;
  const char *str2 = (const char *)type2;
  gsize index = 0;
  int brackets = 0;

  if (str1 == str2)
    return TRUE;

  do
    {
      if (str1[index] != str2[index])
        return FALSE;

      while (str1[index] == 'a' || str1[index] == 'm')
        {
          index++;

          if (str1[index] != str2[index])
            return FALSE;
        }

      if (str1[index] == '(' || str1[index] == '{')
        brackets++;

      else if (str1[index] == ')' || str1[index] == '}')
        brackets--;

      index++;
    }
  while (brackets);

  return TRUE;
}

static inline guint
_g_variant_type_hash (gconstpointer type)
{
  const gchar *type_string = type;
  guint value = 0;
  gsize index = 0;
  int brackets = 0;

  do
    {
      value = (value << 5) - value + type_string[index];

      while (type_string[index] == 'a' || type_string[index] == 'm')
        {
          index++;

          value = (value << 5) - value + type_string[index];
        }

      if (type_string[index] == '(' || type_string[index] == '{')
        brackets++;

      else if (type_string[index] == ')' || type_string[index] == '}')
        brackets--;

      index++;
    }
  while (brackets);

  return value;
}

G_END_DECLS
