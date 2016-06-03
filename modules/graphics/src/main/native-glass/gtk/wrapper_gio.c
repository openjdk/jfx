/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include <stdio.h>
#include <stdlib.h>
#include <linux/fb.h>
#include <fcntl.h>
#ifndef __USE_GNU       // required for dladdr() & Dl_info
#define __USE_GNU
#endif
#include <dlfcn.h>
#include <sys/ioctl.h>

#include <string.h>
#include <strings.h>

#include <assert.h>

#include <gio/gio.h>

#include "glass_wrapper.h"

static GSettingsSchemaSource *(*_g_settings_schema_source_get_default) ();
static GSettingsSchema *(*_g_settings_schema_source_lookup) (
                        GSettingsSchemaSource *source,
                        const gchar *schema_id,
                        gboolean recursive);
static gboolean (*_g_settings_schema_has_key) (
                        GSettingsSchema *schema,
                        const gchar *name);
static GSettings *(*_g_settings_new) (const gchar *schema_id);
static guint (*_g_settings_get_uint) (GSettings *settings,
                        const gchar *key);

/***************************************************************************/

#define PRELOAD_SYMBOL_GIO_OPT(x) \
    _##x = dlsym(libgio, #x); \
    if (!_##x && wrapper_debug) { \
        symbol_load_missing++; \
        fprintf(stderr, "did not find %s\n", #x); \
    } else if (wrapper_debug) { \
        fprintf(stderr, "found %s = 0x%08lx\n", #x, (long)_##x); \
    }

int wrapper_load_symbols_gio (void *libgio)
{
    int symbol_load_missing = 0;
    int symbol_load_errors = 0;

    PRELOAD_SYMBOL_GIO_OPT (g_settings_schema_source_get_default);
    PRELOAD_SYMBOL_GIO_OPT (g_settings_schema_source_lookup);
    PRELOAD_SYMBOL_GIO_OPT (g_settings_schema_has_key);
    PRELOAD_SYMBOL_GIO_OPT (g_settings_new);
    PRELOAD_SYMBOL_GIO_OPT (g_settings_get_uint);

    if (symbol_load_errors && wrapper_debug)
    {
      fprintf (stderr, "failed to load %d required gio symbols\n",
           symbol_load_errors);
    }
    if (symbol_load_missing && wrapper_debug)
    {
      fprintf (stderr, "missing %d optional gio symbols\n",
           symbol_load_missing);
    }

    return symbol_load_errors;
}

#define CHECK_LOAD_SYMBOL_GIO(x) \
    { \
        if (!_##x) { \
            if (wrapper_debug) fprintf(stderr, "missing %s\n", #x); \
            assert(_##x); \
        } else { \
            if (wrapper_debug) { \
               fprintf(stderr, "using %s\n", #x); \
               fflush(stderr); \
            } \
        } \
    }

#define CHECK_LOAD_SYMBOL_GIO_OPT(x, retval) \
    { \
        if (!_##x) { \
            if (wrapper_debug) fprintf(stderr, "missing %s\n", #x); \
            return retval; \
        } else { \
            if (wrapper_debug) { \
               fprintf(stderr, "using %s\n", #x); \
               fflush(stderr); \
            } \
        } \
    }

GSettingsSchemaSource *g_settings_schema_source_get_default ()
{
    CHECK_LOAD_SYMBOL_GIO_OPT(g_settings_schema_source_get_default, NULL)
    return (*_g_settings_schema_source_get_default) ();
}

GSettingsSchema *g_settings_schema_source_lookup (
                    GSettingsSchemaSource *source,
                    const gchar *schema_id,
                    gboolean recursive)
{
    CHECK_LOAD_SYMBOL_GIO_OPT(g_settings_schema_source_lookup, NULL)
    if (source == NULL) {
        return NULL;
    }
    return (*_g_settings_schema_source_lookup) (source, schema_id, recursive);
}

gboolean g_settings_schema_has_key (GSettingsSchema *schema,
                    const gchar *name)
{
    CHECK_LOAD_SYMBOL_GIO_OPT(g_settings_schema_has_key, FALSE)
    return (*_g_settings_schema_has_key) (schema, name);
}

GSettings *g_settings_new (const gchar *schema_id)
{
    CHECK_LOAD_SYMBOL_GIO (g_settings_new)
    return (*_g_settings_new) (schema_id);
}

guint g_settings_get_uint (GSettings *settings,
                        const gchar *key)
{
    CHECK_LOAD_SYMBOL_GIO (g_settings_get_uint)
    return (*_g_settings_get_uint) (settings, key);
}

//--------------------------------------------------------------------------------------

guint glass_settings_get_guint_opt (const gchar *schema_name,
                    const gchar *key_name,
                    int defval)
{
    GSettingsSchemaSource *default_schema_source =
            g_settings_schema_source_get_default();
    if (default_schema_source == NULL) {
        if (wrapper_debug) {
            fprintf(stderr, "No schema source dir found!\n");
        }
        return defval;
    }
    GSettingsSchema *the_schema =
            g_settings_schema_source_lookup(default_schema_source, schema_name, TRUE);
    if (the_schema == NULL) {
        if (wrapper_debug) {
            fprintf(stderr, "schema '%s' not found!\n", schema_name);
        }
        return defval;
    }
    if (!g_settings_schema_has_key(the_schema, key_name)) {
        if (wrapper_debug) {
            fprintf(stderr, "key '%s' not found in schema '%s'!\n", key_name, schema_name);
        }
        return defval;
    }
    if (wrapper_debug) {
        fprintf(stderr, "found schema '%s' and key '%s'\n", schema_name, key_name);
    }
    GSettings *gset = g_settings_new(schema_name);
    return g_settings_get_uint(gset, key_name);
}
