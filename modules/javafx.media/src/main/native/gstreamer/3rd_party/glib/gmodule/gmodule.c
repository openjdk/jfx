/* GMODULE - GLIB wrapper code for dynamic module loading
 * Copyright (C) 1998 Tim Janik
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

/*
 * MT safe
 */

#include "config.h"

#include "glib.h"
#include "gmodule.h"

#include <errno.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#ifdef G_OS_UNIX
#include <unistd.h>
#endif
#ifdef G_OS_WIN32
#include <io.h>    /* For open() and close() prototypes. */
#endif

#ifndef O_CLOEXEC
#define O_CLOEXEC 0
#endif

#include "gmoduleconf.h"
#include "gstdio.h"


/**
 * GModule:
 *
 * The #GModule struct is an opaque data structure to represent a
 * [dynamically-loaded module][glib-Dynamic-Loading-of-Modules].
 * It should only be accessed via the following functions.
 */

/**
 * GModuleCheckInit:
 * @module: the #GModule corresponding to the module which has just been loaded
 *
 * Specifies the type of the module initialization function.
 * If a module contains a function named g_module_check_init() it is called
 * automatically when the module is loaded. It is passed the #GModule structure
 * and should return %NULL on success or a string describing the initialization
 * error.
 *
 * Returns: %NULL on success, or a string describing the initialization error
 */

/**
 * GModuleUnload:
 * @module: the #GModule about to be unloaded
 *
 * Specifies the type of the module function called when it is unloaded.
 * If a module contains a function named g_module_unload() it is called
 * automatically when the module is unloaded.
 * It is passed the #GModule structure.
 */

/**
 * G_MODULE_SUFFIX:
 *
 * Expands to a shared library suffix for the current platform without the
 * leading dot. On Unixes this is "so", and on Windows this is "dll".
 *
 * Deprecated: 2.76: Use g_module_open() instead with @module_name as the
 * basename of the file_name argument. You will get the wrong results using
 * this macro most of the time:
 *
 * 1. The suffix on macOS is usually 'dylib', but it's 'so' when using
 *    Autotools, so there's no way to get the suffix correct using
 *    a pre-processor macro.
 * 2. Prefixes also vary in a platform-specific way. You may or may not have
 *    a 'lib' prefix for the name on Windows and on Cygwin the prefix is
 *    'cyg'.
 * 3. The library name itself can vary per platform. For instance, you may
 *    want to load foo-1.dll on Windows and libfoo.1.dylib on macOS.
 *
 * g_module_open() takes care of all this by searching the filesystem for
 * combinations of possible suffixes and prefixes.
 */

/**
 * G_MODULE_EXPORT:
 *
 * Used to declare functions exported by libraries or modules.
 *
 * When compiling for Windows, it marks the symbol as `dllexport`.
 *
 * When compiling for Linux and Unices, it marks the symbol as having `default`
 * visibility. This is no-op unless the code is being compiled with a
 * non-default
 * [visibility flag](https://gcc.gnu.org/onlinedocs/gcc/Code-Gen-Options.html#index-fvisibility-1260)
 * such as `hidden`.
*
 * This macro must only be used when compiling a shared module. Modules that
 * support both shared and static linking should define their own macro that
 * expands to %G_MODULE_EXPORT when compiling the shared module, but is empty
 * when compiling the static module on Windows.
 */

/**
 * G_MODULE_IMPORT:
 *
 * Used to declare functions imported from modules.
 */

/* We maintain a list of modules, so we can reference count them.
 * That's needed because some platforms don't support references counts on
 * modules. Also, the module for the program itself is kept separately for
 * faster access and because it has special semantics.
 */


/* --- structures --- */
struct _GModule
{
  gchar *file_name;
  gpointer handle;
  guint ref_count : 31;
  guint is_resident : 1;
  GModuleUnload unload;
  GModule *next;
};


/* --- prototypes --- */
static gpointer _g_module_open (const gchar  *file_name,
                                gboolean      bind_lazy,
                                gboolean      bind_local,
                                GError      **error);
static void             _g_module_close         (gpointer        handle);
static gpointer         _g_module_self          (void);
static gpointer         _g_module_symbol        (gpointer        handle,
                                                 const gchar    *symbol_name);
#if (G_MODULE_IMPL != G_MODULE_IMPL_DL) && (G_MODULE_IMPL != G_MODULE_IMPL_AR)
static gchar*           _g_module_build_path    (const gchar    *directory,
                                                 const gchar    *module_name);
#else
/* Implementation is in gmodule-deprecated.c */
gchar*                  _g_module_build_path    (const gchar    *directory,
                                                 const gchar    *module_name);

#endif
static inline void      g_module_set_error      (const gchar    *error);
static inline GModule*  g_module_find_by_handle (gpointer        handle);
static inline GModule*  g_module_find_by_name   (const gchar    *name);


/* --- variables --- */
static GModule       *modules = NULL;
static GModule       *main_module = NULL;
static GPrivate       module_error_private = G_PRIVATE_INIT (g_free);
static gboolean       module_debug_initialized = FALSE;
static guint          module_debug_flags = 0;


/* --- inline functions --- */
static inline GModule*
g_module_find_by_handle (gpointer handle)
{
  GModule *module;
  GModule *retval = NULL;

  if (main_module && main_module->handle == handle)
    retval = main_module;
  else
    for (module = modules; module; module = module->next)
      if (handle == module->handle)
  {
    retval = module;
    break;
  }

  return retval;
}

static inline GModule*
g_module_find_by_name (const gchar *name)
{
  GModule *module;
  GModule *retval = NULL;

  for (module = modules; module; module = module->next)
    if (strcmp (name, module->file_name) == 0)
  {
    retval = module;
    break;
  }

  return retval;
}

static inline void
g_module_set_error_unduped (gchar *error)
{
  g_private_replace (&module_error_private, error);
  errno = 0;
}

static inline void
g_module_set_error (const gchar *error)
{
  g_module_set_error_unduped (g_strdup (error));
}


/* --- include platform specific code --- */
#define SUPPORT_OR_RETURN(rv)   { g_module_set_error (NULL); }
#if     (G_MODULE_IMPL == G_MODULE_IMPL_DL)
#include "gmodule-dl.c"
#elif   (G_MODULE_IMPL == G_MODULE_IMPL_WIN32)
#include "gmodule-win32.c"
#elif   (G_MODULE_IMPL == G_MODULE_IMPL_AR)
#include "gmodule-ar.c"
#else
#undef  SUPPORT_OR_RETURN
#define SUPPORT_OR_RETURN(rv)   { g_module_set_error ("dynamic modules are " \
                                              "not supported by this system"); return rv; }
static gpointer
_g_module_open (const gchar  *file_name,
                gboolean      bind_lazy,
                gboolean      bind_local,
                GError      **error)
{
  g_module_set_error (NULL);
  return NULL;
}
static void
_g_module_close (gpointer handle)
{
}
static gpointer
_g_module_self (void)
{
  return NULL;
}
static gpointer
_g_module_symbol (gpointer       handle,
                  const gchar   *symbol_name)
{
  return NULL;
}
static gchar*
_g_module_build_path (const gchar *directory,
          const gchar *module_name)
{
  return NULL;
}
#endif  /* no implementation */

/**
 * G_MODULE_ERROR:
 *
 * The error domain of the #GModule API.
 *
 * Since: 2.70
 */
G_DEFINE_QUARK (g-module-error-quark, g_module_error)

/* --- functions --- */

/**
 * g_module_supported:
 *
 * Checks if modules are supported on the current platform.
 *
 * Returns: %TRUE if modules are supported
 */
gboolean
g_module_supported (void)
{
  SUPPORT_OR_RETURN (FALSE);

  return TRUE;
}

#ifndef GSTREAMER_LITE
static gchar*
parse_libtool_archive (const gchar* libtool_name)
{
  const guint TOKEN_DLNAME = G_TOKEN_LAST + 1;
  const guint TOKEN_INSTALLED = G_TOKEN_LAST + 2;
  const guint TOKEN_LIBDIR = G_TOKEN_LAST + 3;
  gchar *lt_dlname = NULL;
  gboolean lt_installed = TRUE;
  gchar *lt_libdir = NULL;
  gchar *name;
  GTokenType token;
  GScanner *scanner;

  int fd = g_open (libtool_name, O_RDONLY | O_CLOEXEC, 0);
  if (fd < 0)
    {
      gchar *display_libtool_name = g_filename_display_name (libtool_name);
      g_module_set_error_unduped (g_strdup_printf ("failed to open libtool archive \"%s\"", display_libtool_name));
      g_free (display_libtool_name);
      return NULL;
    }
  /* search libtool's dlname specification  */
  scanner = g_scanner_new (NULL);
  g_scanner_input_file (scanner, fd);
  scanner->config->symbol_2_token = TRUE;
  g_scanner_scope_add_symbol (scanner, 0, "dlname",
            GUINT_TO_POINTER (TOKEN_DLNAME));
  g_scanner_scope_add_symbol (scanner, 0, "installed",
            GUINT_TO_POINTER (TOKEN_INSTALLED));
  g_scanner_scope_add_symbol (scanner, 0, "libdir",
            GUINT_TO_POINTER (TOKEN_LIBDIR));
  while (!g_scanner_eof (scanner))
    {
      token = g_scanner_get_next_token (scanner);
      if (token == TOKEN_DLNAME || token == TOKEN_INSTALLED ||
          token == TOKEN_LIBDIR)
        {
          if (g_scanner_get_next_token (scanner) != '=' ||
              g_scanner_get_next_token (scanner) !=
              (token == TOKEN_INSTALLED ?
               G_TOKEN_IDENTIFIER : G_TOKEN_STRING))
            {
              gchar *display_libtool_name = g_filename_display_name (libtool_name);
              g_module_set_error_unduped (g_strdup_printf ("unable to parse libtool archive \"%s\"", display_libtool_name));
              g_free (display_libtool_name);

              g_free (lt_dlname);
              g_free (lt_libdir);
              g_scanner_destroy (scanner);
              close (fd);

              return NULL;
            }
          else
            {
              if (token == TOKEN_DLNAME)
                {
                  g_free (lt_dlname);
                  lt_dlname = g_strdup (scanner->value.v_string);
                }
              else if (token == TOKEN_INSTALLED)
                lt_installed =
                  strcmp (scanner->value.v_identifier, "yes") == 0;
              else /* token == TOKEN_LIBDIR */
                {
                  g_free (lt_libdir);
                  lt_libdir = g_strdup (scanner->value.v_string);
                }
            }
        }
    }

  if (!lt_installed)
    {
      gchar *dir = g_path_get_dirname (libtool_name);
      g_free (lt_libdir);
      lt_libdir = g_strconcat (dir, G_DIR_SEPARATOR_S ".libs", NULL);
      g_free (dir);
    }

g_clear_pointer (&scanner, g_scanner_destroy);
  close (g_steal_fd (&fd));

  if (lt_libdir == NULL || lt_dlname == NULL)
    {
      gchar *display_libtool_name = g_filename_display_name (libtool_name);
      g_module_set_error_unduped (g_strdup_printf ("unable to parse libtool archive \"%s\"", display_libtool_name));
      g_free (display_libtool_name);

      return NULL;
    }

  name = g_strconcat (lt_libdir, G_DIR_SEPARATOR_S, lt_dlname, NULL);

  g_free (lt_dlname);
  g_free (lt_libdir);

  return name;
}
#endif // GSTREAMER_LITE

enum
{
  G_MODULE_DEBUG_RESIDENT_MODULES = 1 << 0,
  G_MODULE_DEBUG_BIND_NOW_MODULES = 1 << 1
};

static void
_g_module_debug_init (void)
{
  const GDebugKey keys[] = {
    { "resident-modules", G_MODULE_DEBUG_RESIDENT_MODULES },
    { "bind-now-modules", G_MODULE_DEBUG_BIND_NOW_MODULES }
  };
  const gchar *env;

  env = g_getenv ("G_DEBUG");

  module_debug_flags =
    !env ? 0 : g_parse_debug_string (env, keys, G_N_ELEMENTS (keys));

  module_debug_initialized = TRUE;
}

static GRecMutex g_module_global_lock;

/**
 * g_module_open_full:
 * @file_name: (nullable): the name or path to the file containing the module,
 *     or %NULL to obtain a #GModule representing the main program itself
 * @flags: the flags used for opening the module. This can be the
 *     logical OR of any of the #GModuleFlags
 * @error: #GError.
 *
 * Opens a module. If the module has already been opened, its reference count
 * is incremented. If not, the module is searched using @file_name.
 *
 * Since 2.76, the search order/behavior is as follows:
 *
 * 1. If @file_name exists as a regular file, it is used as-is; else
 * 2. If @file_name doesn't have the correct suffix and/or prefix for the
 *    platform, then possible suffixes and prefixes will be added to the
 *    basename till a file is found and whatever is found will be used; else
 * 3. If @file_name doesn't have the ".la"-suffix, ".la" is appended. Either
 *    way, if a matching .la file exists (and is a libtool archive) the
 *    libtool archive is parsed to find the actual file name, and that is
 *    used.
 *
 * If, at the end of all this, we have a file path that we can access on disk,
 * it is opened as a module. If not, @file_name is attempted to be opened as a
 * module verbatim in the hopes that the system implementation will somehow be
 * able to access it. If that is not possible, %NULL is returned.
 *
 * Note that this behaviour was different prior to 2.76, but there is some
 * overlap in functionality. If backwards compatibility is an issue, kindly
 * consult earlier #GModule documentation for the prior search order/behavior
 * of @file_name.
 *
 * Returns: a #GModule on success, or %NULL on failure
 *
 * Since: 2.70
 */
GModule*
g_module_open_full (const gchar   *file_name,
                    GModuleFlags   flags,
                    GError       **error)
{
  GModule *module;
  gpointer handle = NULL;
  gchar *name = NULL;

  SUPPORT_OR_RETURN (NULL);

  g_return_val_if_fail (error == NULL || *error == NULL, NULL);

  g_rec_mutex_lock (&g_module_global_lock);

  if (G_UNLIKELY (!module_debug_initialized))
    _g_module_debug_init ();

  if (module_debug_flags & G_MODULE_DEBUG_BIND_NOW_MODULES)
    flags &= ~G_MODULE_BIND_LAZY;

  if (!file_name)
    {
      if (!main_module)
  {
    handle = _g_module_self ();
/* On Android 64 bit, RTLD_DEFAULT is (void *)0x0
 * so it always fails to create main_module if file_name is NULL */
#if !defined(__BIONIC__) || !defined(__LP64__)
    if (handle)
#endif
      {
        main_module = g_new (GModule, 1);
        main_module->file_name = NULL;
        main_module->handle = handle;
        main_module->ref_count = 1;
        main_module->is_resident = TRUE;
        main_module->unload = NULL;
        main_module->next = NULL;
      }
  }
      else
  main_module->ref_count++;

      g_rec_mutex_unlock (&g_module_global_lock);
      return main_module;
    }

  /* we first search the module list by name */
  module = g_module_find_by_name (file_name);
  if (module)
    {
      module->ref_count++;

      g_rec_mutex_unlock (&g_module_global_lock);
      return module;
    }

  /* check whether we have a readable file right away */
  if (g_file_test (file_name, G_FILE_TEST_IS_REGULAR))
    name = g_strdup (file_name);
  /* try completing file name with standard library suffix */
  if (!name)
    {
      char *basename, *dirname;
      size_t prefix_idx = 0, suffix_idx = 0;
      const char *prefixes[2] = {0}, *suffixes[2] = {0};

      basename = g_path_get_basename (file_name);
      dirname = g_path_get_dirname (file_name);
#ifdef G_OS_WIN32
      if (!g_str_has_prefix (basename, "lib"))
        prefixes[prefix_idx++] = "lib";
      prefixes[prefix_idx++] = "";
      if (!g_str_has_suffix (basename, ".dll"))
        suffixes[suffix_idx++] = ".dll";
#else
  #ifdef __CYGWIN__
      if (!g_str_has_prefix (basename, "cyg"))
        prefixes[prefix_idx++] = "cyg";
  #else
      if (!g_str_has_prefix (basename, "lib"))
        prefixes[prefix_idx++] = "lib";
      else
        /* People commonly pass `libfoo` as the file_name and want us to
         * auto-detect the suffix as .la or .so, etc. We need to also find
         * .dylib and .dll in those cases. */
        prefixes[prefix_idx++] = "";
  #endif
  #ifdef __APPLE__
      if (!g_str_has_suffix (basename, ".dylib") &&
          !g_str_has_suffix (basename, ".so"))
        {
          suffixes[suffix_idx++] = ".dylib";
          suffixes[suffix_idx++] = ".so";
        }
  #else
      if (!g_str_has_suffix (basename, ".so"))
        suffixes[suffix_idx++] = ".so";
  #endif
#endif
      for (guint i = 0; i < prefix_idx; i++)
        {
          for (guint j = 0; j < suffix_idx; j++)
            {
              name = g_strconcat (dirname, G_DIR_SEPARATOR_S, prefixes[i],
                                  basename, suffixes[j], NULL);
              if (g_file_test (name, G_FILE_TEST_IS_REGULAR))
                goto name_found;
              g_free (name);
              name = NULL;
            }
        }
    name_found:
      g_free (basename);
      g_free (dirname);
    }
  /* try completing by appending libtool suffix */
  if (!name)
    {
      name = g_strconcat (file_name, ".la", NULL);
      if (!g_file_test (name, G_FILE_TEST_IS_REGULAR))
        {
          g_free (name);
          name = NULL;
        }
    }
  /* we can't access() the file, lets hope the platform backends finds
   * it via library paths
   */
  if (!name)
    {
      gchar *dot = strrchr (file_name, '.');
      gchar *slash = strrchr (file_name, G_DIR_SEPARATOR);

      /* we make sure the name has a suffix using the deprecated
       * G_MODULE_SUFFIX for backward-compat */
      if (!dot || dot < slash)
        name = g_strconcat (file_name, "." G_MODULE_SUFFIX, NULL);
      else
        name = g_strdup (file_name);
    }

  /* ok, try loading the module */
  g_assert (name != NULL);

#ifndef GSTREAMER_LITE
  /* if it's a libtool archive, figure library file to load */
  if (g_str_has_suffix (name, ".la")) /* libtool archive? */
  {
    gchar *real_name = parse_libtool_archive (name);

    /* real_name might be NULL, but then module error is already set */
    if (real_name)
      {
        g_free (name);
        name = real_name;
      }
  }
#endif // GSTREAMER_LITE

  handle = _g_module_open (name, (flags & G_MODULE_BIND_LAZY) != 0,
                           (flags & G_MODULE_BIND_LOCAL) != 0, error);
  g_free (name);

  if (handle)
    {
      gchar *saved_error;
      GModuleCheckInit check_init;
      const gchar *check_failed = NULL;

      /* search the module list by handle, since file names are not unique */
      module = g_module_find_by_handle (handle);
      if (module)
        {
      _g_module_close (module->handle);
          module->ref_count++;
          g_module_set_error (NULL);

          g_rec_mutex_unlock (&g_module_global_lock);
          return module;
        }

      saved_error = g_strdup (g_module_error ());
      g_module_set_error (NULL);

      module = g_new (GModule, 1);
      module->file_name = g_strdup (file_name);
      module->handle = handle;
      module->ref_count = 1;
      module->is_resident = FALSE;
      module->unload = NULL;
      module->next = modules;
      modules = module;

      /* check initialization */
      if (g_module_symbol (module, "g_module_check_init", (gpointer) &check_init) && check_init != NULL)
  check_failed = check_init (module);

      /* we don't call unload() if the initialization check failed. */
      if (!check_failed)
  g_module_symbol (module, "g_module_unload", (gpointer) &module->unload);

      if (check_failed)
        {
          gchar *temp_error;

          temp_error = g_strconcat ("GModule (", file_name, ") ",
                               "initialization check failed: ",
                               check_failed, NULL);
          g_module_close (module);
          module = NULL;
          g_module_set_error (temp_error);
          g_set_error_literal (error, G_MODULE_ERROR, G_MODULE_ERROR_CHECK_FAILED, temp_error);
          g_free (temp_error);
        }
      else
        g_module_set_error (saved_error);

      g_free (saved_error);
    }

  if (module != NULL &&
      (module_debug_flags & G_MODULE_DEBUG_RESIDENT_MODULES))
    g_module_make_resident (module);

  g_rec_mutex_unlock (&g_module_global_lock);
  return module;
}

/**
 * g_module_open:
 * @file_name: (nullable): the name or path to the file containing the module,
 *     or %NULL to obtain a #GModule representing the main program itself
 * @flags: the flags used for opening the module. This can be the
 *     logical OR of any of the #GModuleFlags.
 *
 * A thin wrapper function around g_module_open_full()
 *
 * Returns: a #GModule on success, or %NULL on failure
 */
GModule *
g_module_open (const gchar  *file_name,
               GModuleFlags  flags)
{
  return g_module_open_full (file_name, flags, NULL);
}

/**
 * g_module_close:
 * @module: a #GModule to close
 *
 * Closes a module.
 *
 * Returns: %TRUE on success
 */
gboolean
g_module_close (GModule *module)
{
  SUPPORT_OR_RETURN (FALSE);

  g_return_val_if_fail (module != NULL, FALSE);
  g_return_val_if_fail (module->ref_count > 0, FALSE);

  g_rec_mutex_lock (&g_module_global_lock);

  module->ref_count--;

  if (!module->ref_count && !module->is_resident && module->unload)
    {
      GModuleUnload unload;

      unload = module->unload;
      module->unload = NULL;
      unload (module);
    }

  if (!module->ref_count && !module->is_resident)
    {
      GModule *last;
      GModule *node;

      last = NULL;

      node = modules;
      while (node)
  {
    if (node == module)
      {
        if (last)
    last->next = node->next;
        else
    modules = node->next;
        break;
      }
    last = node;
    node = last->next;
  }
      module->next = NULL;

      _g_module_close (module->handle);
      g_free (module->file_name);
      g_free (module);
    }

  g_rec_mutex_unlock (&g_module_global_lock);
  return g_module_error() == NULL;
}

/**
 * g_module_make_resident:
 * @module: a #GModule to make permanently resident
 *
 * Ensures that a module will never be unloaded.
 * Any future g_module_close() calls on the module will be ignored.
 */
void
g_module_make_resident (GModule *module)
{
  g_return_if_fail (module != NULL);

  module->is_resident = TRUE;
}

/**
 * g_module_error:
 *
 * Gets a string describing the last module error.
 *
 * Returns: a string describing the last module error
 */
const gchar *
g_module_error (void)
{
  return g_private_get (&module_error_private);
}

/**
 * g_module_symbol:
 * @module: a #GModule
 * @symbol_name: the name of the symbol to find
 * @symbol: (out): returns the pointer to the symbol value
 *
 * Gets a symbol pointer from a module, such as one exported
 * by %G_MODULE_EXPORT. Note that a valid symbol can be %NULL.
 *
 * Returns: %TRUE on success
 */
gboolean
g_module_symbol (GModule     *module,
                 const gchar *symbol_name,
                 gpointer    *symbol)
{
  const gchar *module_error;

  if (symbol)
    *symbol = NULL;
  SUPPORT_OR_RETURN (FALSE);

  g_return_val_if_fail (module != NULL, FALSE);
  g_return_val_if_fail (symbol_name != NULL, FALSE);
  g_return_val_if_fail (symbol != NULL, FALSE);

  g_rec_mutex_lock (&g_module_global_lock);

#ifdef  G_MODULE_NEED_USCORE
  {
    gchar *name;

    name = g_strconcat ("_", symbol_name, NULL);
    *symbol = _g_module_symbol (module->handle, name);
    g_free (name);
  }
#else  /* !G_MODULE_NEED_USCORE */
  *symbol = _g_module_symbol (module->handle, symbol_name);
#endif  /* !G_MODULE_NEED_USCORE */

  module_error = g_module_error ();
  if (module_error)
    {
      gchar *error;

      error = g_strconcat ("'", symbol_name, "': ", module_error, NULL);
      g_module_set_error (error);
      g_free (error);
      *symbol = NULL;
    }

  g_rec_mutex_unlock (&g_module_global_lock);
  return !module_error;
}

/**
 * g_module_name:
 * @module: a #GModule
 *
 * Returns the filename that the module was opened with.
 *
 * If @module refers to the application itself, "main" is returned.
 *
 * Returns: (transfer none): the filename of the module
 */
const gchar *
g_module_name (GModule *module)
{
  g_return_val_if_fail (module != NULL, NULL);

  if (module == main_module)
    return "main";

  return module->file_name;
}

/**
 * g_module_build_path:
 * @directory: (nullable): the directory where the module is. This can be
 *     %NULL or the empty string to indicate that the standard platform-specific
 *     directories will be used, though that is not recommended
 * @module_name: the name of the module
 *
 * A portable way to build the filename of a module. The platform-specific
 * prefix and suffix are added to the filename, if needed, and the result
 * is added to the directory, using the correct separator character.
 *
 * The directory should specify the directory where the module can be found.
 * It can be %NULL or an empty string to indicate that the module is in a
 * standard platform-specific directory, though this is not recommended
 * since the wrong module may be found.
 *
 * For example, calling g_module_build_path() on a Linux system with a
 * @directory of `/lib` and a @module_name of "mylibrary" will return
 * `/lib/libmylibrary.so`. On a Windows system, using `\Windows` as the
 * directory it will return `\Windows\mylibrary.dll`.
 *
 * Returns: the complete path of the module, including the standard library
 *     prefix and suffix. This should be freed when no longer needed
 *
 * Deprecated: 2.76: Use g_module_open() instead with @module_name as the
 * basename of the file_name argument. See %G_MODULE_SUFFIX for why.
 */
gchar *
g_module_build_path (const gchar *directory,
                     const gchar *module_name)
{
  g_return_val_if_fail (module_name != NULL, NULL);

  return _g_module_build_path (directory, module_name);
}


#ifdef G_OS_WIN32

/* Binary compatibility versions. Not for newly compiled code. */

_GMODULE_EXTERN GModule *    g_module_open_utf8 (const gchar  *file_name,
                                              GModuleFlags  flags);

_GMODULE_EXTERN const gchar *g_module_name_utf8 (GModule      *module);

GModule*
g_module_open_utf8 (const gchar    *file_name,
                    GModuleFlags    flags)
{
  return g_module_open (file_name, flags);
}

const gchar *
g_module_name_utf8 (GModule *module)
{
  return g_module_name (module);
}

#endif
