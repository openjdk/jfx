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
#include <stdlib.h>

#include <assert.h>

#include <gtk/gtk.h>

#include "glass_wrapper.h"

int wrapper_debug = 0; // enable for development only
int wrapper_loaded = 0;
int wrapper_gtk_version = 0;
int wrapper_gtk_versionDebug = 0;

// our library combinations defined
// "version" "libgtk", "libdgdk", "libpixbuf"
// note that currently only the first char of the version is used
static char * gtk2_versioned[] = {
   "2", "libgtk-x11-2.0.so.0", "libgdk-x11-2.0.so.0", "libgdk_pixbuf-2.0.so"
};

static char * gtk2_not_versioned[] = {
   "2", "libgtk-x11-2.0.so", "libgdk-x11-2.0.so", "libgdk_pixbuf-2.0.so"
};

static char * gtk3_versioned[] = {
   "3", "libgtk-3.so.0", "libgdk-3.so.0", "libgdk_pixbuf-2.0.so.0"
};

static char * gtk3_not_versioned[] = {
   "3", "libgtk-3.so", "libgdk-3.so", "libgdk_pixbuf-2.0.so"
};

// our library set orders defined, null terminated
static char ** two_to_three[] = {
    gtk2_versioned, gtk2_not_versioned,
    gtk3_versioned, gtk3_not_versioned,
    0
};

static char ** three_to_two[] = {
    gtk3_versioned, gtk3_not_versioned,
    gtk2_versioned, gtk2_not_versioned,
    0
};

static int try_opening_libraries(char *names[3], void** gtk, void** gdk, void ** pix)
{
    *gtk = dlopen (names[1], RTLD_LAZY | RTLD_GLOBAL);
    if (!*gtk) {
        if (wrapper_gtk_versionDebug) {
            fprintf(stderr, "failed to load %s\n", names[1]);
        }
        return 0;
    }

    *gdk = dlopen (names[2], RTLD_LAZY | RTLD_GLOBAL);
    if (!*gdk) {
        if (wrapper_gtk_versionDebug) {
            fprintf(stderr, "failed to load %s\n", names[2]);
        }
        dlclose(*gtk);
        *gtk = 0;
        return 0;
    }

    *pix = dlopen (names[3], RTLD_LAZY | RTLD_GLOBAL);
    if (!*pix) {
        if (wrapper_gtk_versionDebug) {
            fprintf(stderr, "failed to load %s\n", names[3]);
        }
        dlclose(*gtk);
        dlclose(*gdk);
        *gtk = *gdk = 0;
        return 0;
    }

    return 1;
}

int wrapper_load_symbols(int version, int verbose) {
    if (wrapper_loaded) {
        return wrapper_gtk_version;
    }

    wrapper_gtk_versionDebug = verbose;

    void *libgtk = 0, *libgdk = 0, *libpix = 0;

    int success = 1;
    char *** use_chain;

    if (version == 3) {
        use_chain = three_to_two;
        wrapper_gtk_version = 3;
    } else if (version == 0 || version == 2) {
        use_chain = two_to_three;
        wrapper_gtk_version = 2;
    } else {
        // should never happen, java should pass validated values
        fprintf(stderr, "Unrecognized GTK version requested, falling back to v 2.0\n");
        fflush(stderr);
        use_chain = two_to_three;
        wrapper_gtk_version = 2;
    }

    if (wrapper_gtk_versionDebug) {
        fprintf(stderr, "Loading GTK libraries version %d\n", version);
    }

    int i, found = 0;
    for(i = 0; use_chain[i] && !found; i++) {
        if (wrapper_gtk_versionDebug) {
            printf("trying GTK library set %s, %s, %s\n",
                 use_chain[i][1],
                 use_chain[i][2],
                 use_chain[i][3]);
        }
        found = try_opening_libraries(use_chain[i], &libgtk, &libgdk, &libpix);

        if (found) {
            if (use_chain[i][0][0] == '2') {
                wrapper_gtk_version = 2;
            } else { // (use_chain[i][1][0] == '3') {
                wrapper_gtk_version = 3;
            }

            if (wrapper_load_symbols_gtk(wrapper_gtk_version, libgtk) != 0) {
                found = 0;
            } else if (wrapper_load_symbols_gdk(wrapper_gtk_version, libgdk) != 0) {
                found = 0;
            } else if (wrapper_load_symbols_pix(wrapper_gtk_version, libpix) != 0) {
                found = 0;
            }
        }

        if (!found) {
            if (libgtk) dlclose(libgtk);
            if (libgdk) dlclose(libgdk);
            if (libpix) dlclose(libpix);
        }
    }

    if (found) {
        if (wrapper_gtk_versionDebug) {
            i--;
            printf("using GTK library set %s, %s, %s\n",
                 use_chain[i][1],
                 use_chain[i][2],
                 use_chain[i][3]);
        }
    } else {
        return -1;
    }

    void *libgio = dlopen ("libgio-2.0.so", RTLD_LAZY | RTLD_GLOBAL);
    wrapper_load_symbols_gio(libgio);

    wrapper_loaded = 1;

    return wrapper_gtk_version;
}
