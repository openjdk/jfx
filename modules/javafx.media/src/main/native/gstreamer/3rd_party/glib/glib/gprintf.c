/* GLIB - Library of useful routines for C programming
 * Copyright (C) 1995-1997, 2002  Peter Mattis, Red Hat, Inc.
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

#include "config.h"

#include <stdarg.h>
#include <stdlib.h>
#include <stdio.h>
#include <errno.h>

#include "gprintf.h"
#include "gprintfint.h"


/**
 * g_printf:
 * @format: a standard printf() format string, but notice
 *          [string precision pitfalls][string-precision]
 * @...: the arguments to insert in the output.
 *
 * An implementation of the standard printf() function which supports
 * positional parameters, as specified in the Single Unix Specification.
 *
 * As with the standard printf(), this does not automatically append a trailing
 * new-line character to the message, so typically @format should end with its
 * own new-line character.
 *
 * `glib/gprintf.h` must be explicitly included in order to use this function.
 *
 * Returns: the number of bytes printed.
 *
 * Since: 2.2
 **/
gint
g_printf (gchar const *format,
    ...)
{
  va_list args;
  gint retval;

  va_start (args, format);
  retval = g_vprintf (format, args);
  va_end (args);

  return retval;
}

/**
 * g_fprintf:
 * @file: (not nullable): the stream to write to.
 * @format: a standard printf() format string, but notice
 *          [string precision pitfalls][string-precision]
 * @...: the arguments to insert in the output.
 *
 * An implementation of the standard fprintf() function which supports
 * positional parameters, as specified in the Single Unix Specification.
 *
 * `glib/gprintf.h` must be explicitly included in order to use this function.
 *
 * Returns: the number of bytes printed.
 *
 * Since: 2.2
 **/
gint
g_fprintf (FILE        *file,
           gchar const *format,
     ...)
{
  va_list args;
  gint retval;

  va_start (args, format);
  retval = g_vfprintf (file, format, args);
  va_end (args);

  return retval;
}

/**
 * g_sprintf:
 * @string: A pointer to a memory buffer to contain the resulting string. It
 *          is up to the caller to ensure that the allocated buffer is large
 *          enough to hold the formatted result
 * @format: a standard printf() format string, but notice
 *          [string precision pitfalls][string-precision]
 * @...: the arguments to insert in the output.
 *
 * An implementation of the standard sprintf() function which supports
 * positional parameters, as specified in the Single Unix Specification.
 *
 * Note that it is usually better to use g_snprintf(), to avoid the
 * risk of buffer overflow.
 *
 * `glib/gprintf.h` must be explicitly included in order to use this function.
 *
 * See also g_strdup_printf().
 *
 * Returns: the number of bytes printed.
 *
 * Since: 2.2
 **/
gint
g_sprintf (gchar       *string,
     gchar const *format,
     ...)
{
  va_list args;
  gint retval;

  va_start (args, format);
  retval = g_vsprintf (string, format, args);
  va_end (args);

  return retval;
}

/**
 * g_snprintf:
 * @string: the buffer to hold the output.
 * @n: the maximum number of bytes to produce (including the
 *     terminating nul character).
 * @format: a standard printf() format string, but notice
 *          [string precision pitfalls][string-precision]
 * @...: the arguments to insert in the output.
 *
 * A safer form of the standard sprintf() function. The output is guaranteed
 * to not exceed @n characters (including the terminating nul character), so
 * it is easy to ensure that a buffer overflow cannot occur.
 *
 * See also g_strdup_printf().
 *
 * In versions of GLib prior to 1.2.3, this function may return -1 if the
 * output was truncated, and the truncated string may not be nul-terminated.
 * In versions prior to 1.3.12, this function returns the length of the output
 * string.
 *
 * The return value of g_snprintf() conforms to the snprintf()
 * function as standardized in ISO C99. Note that this is different from
 * traditional snprintf(), which returns the length of the output string.
 *
 * The format string may contain positional parameters, as specified in
 * the Single Unix Specification.
 *
 * Returns: the number of bytes which would be produced if the buffer
 *     was large enough.
 **/
gint
g_snprintf (gchar *string,
      gulong   n,
      gchar const *format,
      ...)
{
  va_list args;
  gint retval;

  va_start (args, format);
  retval = g_vsnprintf (string, n, format, args);
  va_end (args);

  return retval;
}

/**
 * g_vprintf:
 * @format: a standard printf() format string, but notice
 *          [string precision pitfalls][string-precision]
 * @args: the list of arguments to insert in the output.
 *
 * An implementation of the standard vprintf() function which supports
 * positional parameters, as specified in the Single Unix Specification.
 *
 * `glib/gprintf.h` must be explicitly included in order to use this function.
 *
 * Returns: the number of bytes printed.
 *
 * Since: 2.2
 **/
gint
g_vprintf (gchar const *format,
     va_list      args)
{
  g_return_val_if_fail (format != NULL, -1);

  return _g_vprintf (format, args);
}

/**
 * g_vfprintf:
 * @file: (not nullable): the stream to write to.
 * @format: a standard printf() format string, but notice
 *          [string precision pitfalls][string-precision]
 * @args: the list of arguments to insert in the output.
 *
 * An implementation of the standard fprintf() function which supports
 * positional parameters, as specified in the Single Unix Specification.
 *
 * `glib/gprintf.h` must be explicitly included in order to use this function.
 *
 * Returns: the number of bytes printed.
 *
 * Since: 2.2
 **/
gint
g_vfprintf (FILE        *file,
            gchar const *format,
      va_list      args)
{
  g_return_val_if_fail (format != NULL, -1);

  return _g_vfprintf (file, format, args);
}

/**
 * g_vsprintf:
 * @string: the buffer to hold the output.
 * @format: a standard printf() format string, but notice
 *          [string precision pitfalls][string-precision]
 * @args: the list of arguments to insert in the output.
 *
 * An implementation of the standard vsprintf() function which supports
 * positional parameters, as specified in the Single Unix Specification.
 *
 * `glib/gprintf.h` must be explicitly included in order to use this function.
 *
 * Returns: the number of bytes printed.
 *
 * Since: 2.2
 **/
gint
g_vsprintf (gchar  *string,
      gchar const *format,
      va_list      args)
{
  g_return_val_if_fail (string != NULL, -1);
  g_return_val_if_fail (format != NULL, -1);

  return _g_vsprintf (string, format, args);
}

/**
 * g_vsnprintf:
 * @string: the buffer to hold the output.
 * @n: the maximum number of bytes to produce (including the
 *     terminating nul character).
 * @format: a standard printf() format string, but notice
 *          string precision pitfalls][string-precision]
 * @args: the list of arguments to insert in the output.
 *
 * A safer form of the standard vsprintf() function. The output is guaranteed
 * to not exceed @n characters (including the terminating nul character), so
 * it is easy to ensure that a buffer overflow cannot occur.
 *
 * See also g_strdup_vprintf().
 *
 * In versions of GLib prior to 1.2.3, this function may return -1 if the
 * output was truncated, and the truncated string may not be nul-terminated.
 * In versions prior to 1.3.12, this function returns the length of the output
 * string.
 *
 * The return value of g_vsnprintf() conforms to the vsnprintf() function
 * as standardized in ISO C99. Note that this is different from traditional
 * vsnprintf(), which returns the length of the output string.
 *
 * The format string may contain positional parameters, as specified in
 * the Single Unix Specification.
 *
 * Returns: the number of bytes which would be produced if the buffer
 *  was large enough.
 */
gint
g_vsnprintf (gchar   *string,
       gulong   n,
       gchar const *format,
       va_list      args)
{
  g_return_val_if_fail (n == 0 || string != NULL, -1);
  g_return_val_if_fail (format != NULL, -1);

  return _g_vsnprintf (string, n, format, args);
}

/**
 * g_vasprintf:
 * @string: (not optional) (nullable): the return location for the newly-allocated string.
 * @format: (not nullable): a standard printf() format string, but notice
 *          [string precision pitfalls][string-precision]
 * @args: the list of arguments to insert in the output.
 *
 * An implementation of the GNU vasprintf() function which supports
 * positional parameters, as specified in the Single Unix Specification.
 * This function is similar to g_vsprintf(), except that it allocates a
 * string to hold the output, instead of putting the output in a buffer
 * you allocate in advance.
 *
 * The returned value in @string is guaranteed to be non-NULL, unless
 * @format contains `%lc` or `%ls` conversions, which can fail if no
 * multibyte representation is available for the given character.
 *
 * `glib/gprintf.h` must be explicitly included in order to use this function.
 *
 * Returns: the number of bytes printed.
 *
 * Since: 2.4
 **/
gint
g_vasprintf (gchar      **string,
       gchar const *format,
       va_list      args)
{
  gint len;
  g_return_val_if_fail (string != NULL, -1);

#if !defined(USE_SYSTEM_PRINTF)

  len = _g_gnulib_vasprintf (string, format, args);
  if (len < 0)
    *string = NULL;

#elif defined (HAVE_VASPRINTF)

  {
    int saved_errno;
    len = vasprintf (string, format, args);
    saved_errno = errno;
    if (len < 0)
      {
        if (saved_errno == ENOMEM)
          g_error ("%s: failed to allocate memory", G_STRLOC);
        else
          *string = NULL;
      }
  }

#else

  {
    va_list args2;

    G_VA_COPY (args2, args);

    *string = g_new (gchar, g_printf_string_upper_bound (format, args));

    len = _g_vsprintf (*string, format, args2);
    va_end (args2);
  }
#endif

  return len;
}
