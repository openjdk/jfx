/* GLIB - Library of useful routines for C programming
 * Copyright (C) 1995-1997  Peter Mattis, Spencer Kimball and Josh MacDonald
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
 */

/*
 * Modified by the GLib Team and others 1997-2000.  See the AUTHORS
 * file for a list of people on the GLib Team.  See the ChangeLog
 * files for a list of changes.  These files are distributed with
 * GLib at ftp://ftp.gtk.org/pub/gtk/.
 */

#include "config.h"

#include "gvalgrind.h"
#include <string.h>

#include "gerror.h"

#include "ghash.h"
#include "glib-init.h"
#include "gslice.h"
#include "gstrfuncs.h"
#include "gtestutils.h"
#include "gthread.h"

static GRWLock error_domain_global;
/* error_domain_ht must be accessed with error_domain_global
 * locked.
 */
static GHashTable *error_domain_ht = NULL;

void
g_error_init (void)
{
  error_domain_ht = g_hash_table_new (NULL, NULL);
}

typedef struct
{
  /* private_size is already aligned. */
  gsize private_size;
  GErrorInitFunc init;
  GErrorCopyFunc copy;
  GErrorClearFunc clear;
} ErrorDomainInfo;

/* Must be called with error_domain_global locked.
 */
static inline ErrorDomainInfo *
error_domain_lookup (GQuark domain)
{
  return g_hash_table_lookup (error_domain_ht,
                              GUINT_TO_POINTER (domain));
}

/* Copied from gtype.c. */
#define STRUCT_ALIGNMENT (2 * sizeof (gsize))
#define ALIGN_STRUCT(offset) \
      ((offset + (STRUCT_ALIGNMENT - 1)) & -STRUCT_ALIGNMENT)

static void
error_domain_register (GQuark            error_quark,
                       gsize             error_type_private_size,
                       GErrorInitFunc    error_type_init,
                       GErrorCopyFunc    error_type_copy,
                       GErrorClearFunc   error_type_clear)
{
  g_rw_lock_writer_lock (&error_domain_global);
  if (error_domain_lookup (error_quark) == NULL)
    {
      ErrorDomainInfo *info = g_new (ErrorDomainInfo, 1);
      info->private_size = ALIGN_STRUCT (error_type_private_size);
      info->init = error_type_init;
      info->copy = error_type_copy;
      info->clear = error_type_clear;

      g_hash_table_insert (error_domain_ht,
                           GUINT_TO_POINTER (error_quark),
                           info);
    }
  else
    {
      const char *name = g_quark_to_string (error_quark);

      g_critical ("Attempted to register an extended error domain for %s more than once", name);
    }
  g_rw_lock_writer_unlock (&error_domain_global);
}

/**
 * g_error_domain_register_static:
 * @error_type_name: static string to create a #GQuark from
 * @error_type_private_size: size of the private error data in bytes
 * @error_type_init: (scope forever): function initializing fields of the private error data
 * @error_type_copy: (scope forever): function copying fields of the private error data
 * @error_type_clear: (scope forever): function freeing fields of the private error data
 *
 * This function registers an extended #GError domain.
 *
 * @error_type_name should not be freed. @error_type_private_size must
 * be greater than 0.
 *
 * @error_type_init receives an initialized #GError and should then initialize
 * the private data.
 *
 * @error_type_copy is a function that receives both original and a copy
 * #GError and should copy the fields of the private error data. The standard
 * #GError fields are already handled.
 *
 * @error_type_clear receives the pointer to the error, and it should free the
 * fields of the private error data. It should not free the struct itself though.
 *
 * Normally, it is better to use G_DEFINE_EXTENDED_ERROR(), as it
 * already takes care of passing valid information to this function.
 *
 * Returns: #GQuark representing the error domain
 * Since: 2.68
 */
GQuark
g_error_domain_register_static (const char        *error_type_name,
                                gsize              error_type_private_size,
                                GErrorInitFunc     error_type_init,
                                GErrorCopyFunc     error_type_copy,
                                GErrorClearFunc    error_type_clear)
{
  GQuark error_quark;

  g_return_val_if_fail (error_type_name != NULL, 0);
  g_return_val_if_fail (error_type_private_size > 0, 0);
  g_return_val_if_fail (error_type_init != NULL, 0);
  g_return_val_if_fail (error_type_copy != NULL, 0);
  g_return_val_if_fail (error_type_clear != NULL, 0);

  error_quark = g_quark_from_static_string (error_type_name);
  error_domain_register (error_quark,
                         error_type_private_size,
                         error_type_init,
                         error_type_copy,
                         error_type_clear);
  return error_quark;
}

/**
 * g_error_domain_register:
 * @error_type_name: string to create a #GQuark from
 * @error_type_private_size: size of the private error data in bytes
 * @error_type_init: (scope forever): function initializing fields of the private error data
 * @error_type_copy: (scope forever): function copying fields of the private error data
 * @error_type_clear: (scope forever): function freeing fields of the private error data
 *
 * This function registers an extended #GError domain.
 * @error_type_name will be duplicated. Otherwise does the same as
 * g_error_domain_register_static().
 *
 * Returns: #GQuark representing the error domain
 * Since: 2.68
 */
GQuark
g_error_domain_register (const char        *error_type_name,
                         gsize              error_type_private_size,
                         GErrorInitFunc     error_type_init,
                         GErrorCopyFunc     error_type_copy,
                         GErrorClearFunc    error_type_clear)
{
  GQuark error_quark;

  g_return_val_if_fail (error_type_name != NULL, 0);
  g_return_val_if_fail (error_type_private_size > 0, 0);
  g_return_val_if_fail (error_type_init != NULL, 0);
  g_return_val_if_fail (error_type_copy != NULL, 0);
  g_return_val_if_fail (error_type_clear != NULL, 0);

  error_quark = g_quark_from_string (error_type_name);
  error_domain_register (error_quark,
                         error_type_private_size,
                         error_type_init,
                         error_type_copy,
                         error_type_clear);
  return error_quark;
}

static GError *
g_error_allocate (GQuark domain, ErrorDomainInfo *out_info)
{
  guint8 *allocated;
  GError *error;
  ErrorDomainInfo *info;
  gsize private_size;

  g_rw_lock_reader_lock (&error_domain_global);
  info = error_domain_lookup (domain);
  if (info != NULL)
    {
      if (out_info != NULL)
        *out_info = *info;
      private_size = info->private_size;
      g_rw_lock_reader_unlock (&error_domain_global);
    }
  else
    {
      g_rw_lock_reader_unlock (&error_domain_global);
      if (out_info != NULL)
        memset (out_info, 0, sizeof (*out_info));
      private_size = 0;
    }
  /* See comments in g_type_create_instance in gtype.c to see what
   * this magic is about.
   */
#ifdef ENABLE_VALGRIND
  if (private_size > 0 && RUNNING_ON_VALGRIND)
    {
      private_size += ALIGN_STRUCT (1);
      allocated = g_slice_alloc0 (private_size + sizeof (GError) + sizeof (gpointer));
      *(gpointer *) (allocated + private_size + sizeof (GError)) = allocated + ALIGN_STRUCT (1);
      VALGRIND_MALLOCLIKE_BLOCK (allocated + private_size, sizeof (GError) + sizeof (gpointer), 0, TRUE);
      VALGRIND_MALLOCLIKE_BLOCK (allocated + ALIGN_STRUCT (1), private_size - ALIGN_STRUCT (1), 0, TRUE);
    }
  else
#endif
    allocated = g_slice_alloc0 (private_size + sizeof (GError));

  error = (GError *) (allocated + private_size);
  return error;
}

/* This function takes ownership of @message. */
static GError *
g_error_new_steal (GQuark           domain,
                   gint             code,
                   gchar           *message,
                   ErrorDomainInfo *out_info)
{
  ErrorDomainInfo info;
  GError *error = g_error_allocate (domain, &info);

  error->domain = domain;
  error->code = code;
  error->message = message;

  if (info.init != NULL)
    info.init (error);
  if (out_info != NULL)
    *out_info = info;

  return error;
}

/**
 * g_error_new_valist:
 * @domain: error domain
 * @code: error code
 * @format: printf()-style format for error message
 * @args: #va_list of parameters for the message format
 *
 * Creates a new #GError with the given @domain and @code,
 * and a message formatted with @format.
 *
 * Returns: a new #GError
 *
 * Since: 2.22
 */
GError*
g_error_new_valist (GQuark       domain,
                    gint         code,
                    const gchar *format,
                    va_list      args)
{
  g_return_val_if_fail (format != NULL, NULL);

  /* Historically, GError allowed this (although it was never meant to work),
   * and it has significant use in the wild, which g_return_val_if_fail
   * would break. It should maybe g_return_val_if_fail in GLib 4.
   * (GNOME#660371, GNOME#560482)
   */
  g_warn_if_fail (domain != 0);

  return g_error_new_steal (domain, code, g_strdup_vprintf (format, args), NULL);
}

/**
 * g_error_new:
 * @domain: error domain
 * @code: error code
 * @format: printf()-style format for error message
 * @...: parameters for message format
 *
 * Creates a new #GError with the given @domain and @code,
 * and a message formatted with @format.
 *
 * Returns: a new #GError
 */
GError*
g_error_new (GQuark       domain,
             gint         code,
             const gchar *format,
             ...)
{
  GError* error;
  va_list args;

  g_return_val_if_fail (format != NULL, NULL);
  g_return_val_if_fail (domain != 0, NULL);

  va_start (args, format);
  error = g_error_new_valist (domain, code, format, args);
  va_end (args);

  return error;
}

/**
 * g_error_new_literal:
 * @domain: error domain
 * @code: error code
 * @message: error message
 *
 * Creates a new #GError; unlike g_error_new(), @message is
 * not a printf()-style format string. Use this function if
 * @message contains text you don't have control over,
 * that could include printf() escape sequences.
 *
 * Returns: a new #GError
 **/
GError*
g_error_new_literal (GQuark         domain,
                     gint           code,
                     const gchar   *message)
{
  g_return_val_if_fail (message != NULL, NULL);
  g_return_val_if_fail (domain != 0, NULL);

  return g_error_new_steal (domain, code, g_strdup (message), NULL);
}

/**
 * g_error_free:
 * @error: a #GError
 *
 * Frees a #GError and associated resources.
 */
void
g_error_free (GError *error)
{
  gsize private_size;
  ErrorDomainInfo *info;
  guint8 *allocated;

  g_return_if_fail (error != NULL);

  g_rw_lock_reader_lock (&error_domain_global);
  info = error_domain_lookup (error->domain);
  if (info != NULL)
    {
      GErrorClearFunc clear = info->clear;

      private_size = info->private_size;
      g_rw_lock_reader_unlock (&error_domain_global);
      clear (error);
    }
  else
    {
      g_rw_lock_reader_unlock (&error_domain_global);
      private_size = 0;
    }

  g_free (error->message);
  allocated = ((guint8 *) error) - private_size;
  /* See comments in g_type_free_instance in gtype.c to see what this
   * magic is about.
   */
#ifdef ENABLE_VALGRIND
  if (private_size > 0 && RUNNING_ON_VALGRIND)
    {
      private_size += ALIGN_STRUCT (1);
      allocated -= ALIGN_STRUCT (1);
      *(gpointer *) (allocated + private_size + sizeof (GError)) = NULL;
      g_slice_free1 (private_size + sizeof (GError) + sizeof (gpointer), allocated);
      VALGRIND_FREELIKE_BLOCK (allocated + ALIGN_STRUCT (1), 0);
      VALGRIND_FREELIKE_BLOCK (error, 0);
    }
  else
#endif
  g_slice_free1 (private_size + sizeof (GError), allocated);
}

/**
 * g_error_copy:
 * @error: a #GError
 *
 * Makes a copy of @error.
 *
 * Returns: a new #GError
 */
GError*
g_error_copy (const GError *error)
{
  GError *copy;
  ErrorDomainInfo info;

  g_return_val_if_fail (error != NULL, NULL);
  g_return_val_if_fail (error->message != NULL, NULL);

  /* See g_error_new_valist for why this doesn’t return */
  g_warn_if_fail (error->domain != 0);

  copy = g_error_new_steal (error->domain,
                            error->code,
                            g_strdup (error->message),
                            &info);
#ifdef GSTREAMER_LITE
  if (copy == NULL) {
    return NULL;
  }
#endif // GSTREAMER_LITE

  if (info.copy != NULL)
    info.copy (error, copy);

  return copy;
}

/**
 * g_error_matches:
 * @error: (nullable): a #GError
 * @domain: an error domain
 * @code: an error code
 *
 * Returns %TRUE if @error matches @domain and @code, %FALSE
 * otherwise. In particular, when @error is %NULL, %FALSE will
 * be returned.
 *
 * If @domain contains a `FAILED` (or otherwise generic) error code,
 * you should generally not check for it explicitly, but should
 * instead treat any not-explicitly-recognized error code as being
 * equivalent to the `FAILED` code. This way, if the domain is
 * extended in the future to provide a more specific error code for
 * a certain case, your code will still work.
 *
 * Returns: whether @error has @domain and @code
 */
gboolean
g_error_matches (const GError *error,
                 GQuark        domain,
                 gint          code)
{
  return error &&
    error->domain == domain &&
    error->code == code;
}

#define ERROR_OVERWRITTEN_WARNING "GError set over the top of a previous GError or uninitialized memory.\n" \
               "This indicates a bug in someone's code. You must ensure an error is NULL before it's set.\n" \
               "The overwriting error message was: %s"

/**
 * g_set_error:
 * @err: (out callee-allocates) (optional): a return location for a #GError
 * @domain: error domain
 * @code: error code
 * @format: printf()-style format
 * @...: args for @format
 *
 * Does nothing if @err is %NULL; if @err is non-%NULL, then *@err
 * must be %NULL. A new #GError is created and assigned to *@err.
 */
void
g_set_error (GError      **err,
             GQuark        domain,
             gint          code,
             const gchar  *format,
             ...)
{
  GError *new;

  va_list args;

  if (err == NULL)
    return;

  va_start (args, format);
  new = g_error_new_valist (domain, code, format, args);
  va_end (args);

  if (*err == NULL)
    *err = new;
  else
    {
      g_warning (ERROR_OVERWRITTEN_WARNING, new->message);
      g_error_free (new);
    }
}

/**
 * g_set_error_literal:
 * @err: (out callee-allocates) (optional): a return location for a #GError
 * @domain: error domain
 * @code: error code
 * @message: error message
 *
 * Does nothing if @err is %NULL; if @err is non-%NULL, then *@err
 * must be %NULL. A new #GError is created and assigned to *@err.
 * Unlike g_set_error(), @message is not a printf()-style format string.
 * Use this function if @message contains text you don't have control over,
 * that could include printf() escape sequences.
 *
 * Since: 2.18
 */
void
g_set_error_literal (GError      **err,
                     GQuark        domain,
                     gint          code,
                     const gchar  *message)
{
  if (err == NULL)
    return;

  if (*err == NULL)
    *err = g_error_new_literal (domain, code, message);
  else
    g_warning (ERROR_OVERWRITTEN_WARNING, message);
}

/**
 * g_propagate_error:
 * @dest: (out callee-allocates) (optional) (nullable): error return location
 * @src: (transfer full): error to move into the return location
 *
 * If @dest is %NULL, free @src; otherwise, moves @src into *@dest.
 * The error variable @dest points to must be %NULL.
 *
 * @src must be non-%NULL.
 *
 * Note that @src is no longer valid after this call. If you want
 * to keep using the same GError*, you need to set it to %NULL
 * after calling this function on it.
 */
void
g_propagate_error (GError **dest,
       GError  *src)
{
  g_return_if_fail (src != NULL);

  if (dest == NULL)
    {
      g_error_free (src);
      return;
    }
  else
    {
      if (*dest != NULL)
        {
          g_warning (ERROR_OVERWRITTEN_WARNING, src->message);
          g_error_free (src);
        }
      else
        *dest = src;
    }
}

/**
 * g_clear_error:
 * @err: a #GError return location
 *
 * If @err or *@err is %NULL, does nothing. Otherwise,
 * calls g_error_free() on *@err and sets *@err to %NULL.
 */
void
g_clear_error (GError **err)
{
  if (err && *err)
    {
      g_error_free (*err);
      *err = NULL;
    }
}

G_GNUC_PRINTF(2, 0)
static void
g_error_add_prefix (gchar       **string,
                    const gchar  *format,
                    va_list       ap)
{
  gchar *oldstring;
  gchar *prefix;

  prefix = g_strdup_vprintf (format, ap);
  oldstring = *string;
  *string = g_strconcat (prefix, oldstring, NULL);
  g_free (oldstring);
  g_free (prefix);
}

/**
 * g_prefix_error:
 * @err: (inout) (optional) (nullable): a return location for a #GError
 * @format: printf()-style format string
 * @...: arguments to @format
 *
 * Formats a string according to @format and prefix it to an existing
 * error message. If @err is %NULL (ie: no error variable) then do
 * nothing.
 *
 * If *@err is %NULL (ie: an error variable is present but there is no
 * error condition) then also do nothing.
 *
 * Since: 2.16
 */
void
g_prefix_error (GError      **err,
                const gchar  *format,
                ...)
{
  if (err && *err)
    {
      va_list ap;

      va_start (ap, format);
      g_error_add_prefix (&(*err)->message, format, ap);
      va_end (ap);
    }
}

/**
 * g_prefix_error_literal:
 * @err: (inout) (nullable) (optional): a return location for a #GError, or %NULL
 * @prefix: string to prefix @err with
 *
 * Prefixes @prefix to an existing error message. If @err or *@err is
 * %NULL (i.e.: no error variable) then do nothing.
 *
 * Since: 2.70
 */
void
g_prefix_error_literal (GError      **err,
                        const gchar  *prefix)
{
  if (err && *err)
    {
      gchar *oldstring;

      oldstring = (*err)->message;
      (*err)->message = g_strconcat (prefix, oldstring, NULL);
      g_free (oldstring);
    }
}

/**
 * g_propagate_prefixed_error:
 * @dest: error return location
 * @src: error to move into the return location
 * @format: printf()-style format string
 * @...: arguments to @format
 *
 * If @dest is %NULL, free @src; otherwise, moves @src into *@dest.
 * *@dest must be %NULL. After the move, add a prefix as with
 * g_prefix_error().
 *
 * Since: 2.16
 **/
void
g_propagate_prefixed_error (GError      **dest,
                            GError       *src,
                            const gchar  *format,
                            ...)
{
  g_propagate_error (dest, src);

  if (dest)
    {
      va_list ap;

      g_assert (*dest != NULL);
      va_start (ap, format);
      g_error_add_prefix (&(*dest)->message, format, ap);
      va_end (ap);
    }
}
