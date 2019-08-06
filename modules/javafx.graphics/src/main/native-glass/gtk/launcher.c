/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

#include <X11/Xlib.h>

#include <assert.h>

#include <jni.h>

#include <com_sun_glass_ui_gtk_GtkApplication.h>

static jboolean gtk_versionDebug = JNI_FALSE;

static const char * gtk2_chain[] = {
   "libglassgtk2.so", "libglassgtk3.so", 0
};

static const char * gtk3_chain[] = {
   "libglassgtk3.so", "libglassgtk2.so", 0
};

static JavaVM* javaVM;

JNIEXPORT jint JNICALL
#ifdef STATIC_BUILD
JNI_OnLoad_glass(JavaVM *jvm, void *reserved)
#else
JNI_OnLoad(JavaVM *jvm, void *reserved)
#endif
{
    (void) reserved;

    javaVM = jvm;

    return JNI_VERSION_1_6;
}

// our library combinations defined
// "version" "libgtk", "libdgdk", "libpixbuf"
// note that currently only the first char of the version is used
static char * gtk2_versioned[] = {
   "2", "libgtk-x11-2.0.so.0"
};

static char * gtk2_not_versioned[] = {
   "2", "libgtk-x11-2.0.so"
};

static char * gtk3_versioned[] = {
   "3", "libgtk-3.so.0"
};

static char * gtk3_not_versioned[] = {
   "3", "libgtk-3.so"
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

static int try_opening_libraries(char *names[3])
{
    void * gtk;

    gtk = dlopen (names[1], RTLD_LAZY | RTLD_GLOBAL);
    if (!gtk) {
        return 0;
    }

    return 1;
}

static int try_libraries_noload(char *names[3])
{
#ifdef RTLD_NOLOAD
    void *gtk;
    gtk = dlopen(names[1], RTLD_LAZY | RTLD_NOLOAD);
    return gtk ? 1 : 0;
#else
    return 0;
#endif
}

static int sniffLibs(int wantVersion) {

     if (gtk_versionDebug) {
         printf("checking GTK version %d\n",wantVersion);
     }

     int success = 1;
     char *** use_chain = three_to_two;
     int i, found = 0;

     //at first try to detect already loaded GTK version
     for (i = 0; use_chain[i] && !found; i++) {
        found = try_libraries_noload(use_chain[i]);
        if (found && gtk_versionDebug) {
            printf("found already loaded GTK library %s\n", use_chain[i][1]);
        }
     }

     if (!found) {
         if (wantVersion == 0 || wantVersion == 3) {
             use_chain = three_to_two;
         } else if (wantVersion == 2) {
             use_chain = two_to_three;
         } else {
             // Note, this should never happen, java should be protecting us
             if (gtk_versionDebug) {
                 printf("bad GTK version specified, assuming 3\n");
             }
             wantVersion = 3;
             use_chain = three_to_two;
         }

         for (i = 0; use_chain[i] && !found; i++) {
             if (gtk_versionDebug) {
                 printf("trying GTK library %s\n", use_chain[i][1]);
             }
             found = try_opening_libraries(use_chain[i]);
         }
     }

    if (found) {
        if (gtk_versionDebug) {
            i--;
            printf("using GTK library version %s set %s\n",
                 use_chain[i][0],
                 use_chain[i][1]);
            fflush(stdout);
        }
        return use_chain[i][0][0];
    }
    if (gtk_versionDebug) {
        fflush(stdout);
    }
    return -1;
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkApplication
 * Method:    _queryLibrary
 * Signature: Signature: (IZ)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_gtk_GtkApplication__1queryLibrary
  (JNIEnv *env, jclass clazz, jint suggestedVersion, jboolean verbose)
{
    (void) env;
    (void) clazz;

    gtk_versionDebug = verbose;

    //Set the gtk backend to x11 on all the systems
    putenv("GDK_BACKEND=x11");

    // Before doing anything with GTK we validate that the DISPLAY can be opened
    Display *display = XOpenDisplay(NULL);
    if (display == NULL) {
        return com_sun_glass_ui_gtk_GtkApplication_QUERY_NO_DISPLAY;
    }
    XCloseDisplay(display);

    // now check the the presence of the libraries

    char version = sniffLibs(suggestedVersion);

    if (version == '2') {
        return com_sun_glass_ui_gtk_GtkApplication_QUERY_LOAD_GTK2;
    } else if (version == '3') {
        return com_sun_glass_ui_gtk_GtkApplication_QUERY_LOAD_GTK3;
    }

    return com_sun_glass_ui_gtk_GtkApplication_QUERY_ERROR;
}

