/*
 * Copyright (c) 2012, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include <stdlib.h>
#include <assert.h>
#include <stdio.h>
#include <string.h>
#include <math.h>
#include <X11/Xutil.h>

#include "../PrismES2Defs.h"
#include "../prism_es2_api.h"

void setGLXAttrs(const Es2PixelFormatAttrs *attrs, int *glxAttrs) {
    int index = 0;

    /* Specify pbuffer as default */
    glxAttrs[index++] = GLX_DRAWABLE_TYPE;
    if (attrs->on_screen != 0) {
        glxAttrs[index++] = (GLX_PBUFFER_BIT | GLX_WINDOW_BIT);
    } else {
        glxAttrs[index++] = GLX_PBUFFER_BIT;
    }

    /* only interested in RGBA type */
    glxAttrs[index++] = GLX_RENDER_TYPE;
    glxAttrs[index++] = GLX_RGBA_BIT;

    /* only interested in FBConfig with associated X Visual type */
    glxAttrs[index++] = GLX_X_RENDERABLE;
    glxAttrs[index++] = True;

    glxAttrs[index++] = GLX_DOUBLEBUFFER;
    if (attrs->double_buffer != 0) {
        glxAttrs[index++] = True;
    } else {
        glxAttrs[index++] = False;
    }

    glxAttrs[index++] = GLX_RED_SIZE;
    glxAttrs[index++] = attrs->red_size;
    glxAttrs[index++] = GLX_GREEN_SIZE;
    glxAttrs[index++] = attrs->green_size;
    glxAttrs[index++] = GLX_BLUE_SIZE;
    glxAttrs[index++] = attrs->blue_size;
    glxAttrs[index++] = GLX_ALPHA_SIZE;
    glxAttrs[index++] = attrs->alpha_size;

    glxAttrs[index++] = GLX_DEPTH_SIZE;
    glxAttrs[index++] = attrs->depth_size;

    glxAttrs[index] = None;
}

void printAndReleaseResources(Display *display, GLXFBConfig *fbConfigList,
        XVisualInfo *visualInfo, Window win, GLXContext ctx, Colormap cmap,
        const char *message) {
    if (message != NULL) {
        fprintf(stderr, "%s\n", message);
    }
    if (display == NULL) {
        return;
    }
    glXMakeCurrent(display, None, NULL);
    if (fbConfigList != NULL) {
        XFree(fbConfigList);
    }
    if (visualInfo != NULL) {
        XFree(visualInfo);
    }
    if (ctx != NULL) {
        glXDestroyContext(display, ctx);
    }
    if (win != None) {
        XDestroyWindow(display, win);
    }
    if (cmap != None) {
        XFreeColormap(display, cmap);
    }
}

GLboolean queryGLX13(Display *display) {

    int major, minor;
    int errorBase, eventBase;

    if (!glXQueryExtension(display, &errorBase, &eventBase)) {
        fprintf(stderr, "ES2 Prism: Error - GLX extension is not supported\n");
        fprintf(stderr, "    GLX version 1.3 or higher is required\n");
        return GL_FALSE;
    }

    /* Query the GLX version number */
    if (!glXQueryVersion(display, &major, &minor)) {
        fprintf(stderr, "ES2 Prism: Error - Unable to query GLX version\n");
        fprintf(stderr, "    GLX version 1.3 or higher is required\n");
        return GL_FALSE;
    }

    /*
        fprintf(stderr, "Checking GLX version : %d.%d\n", major, minor);
     */

    /* Check for GLX 1.3 and higher */
    if (!(major == 1 && minor >= 3)) {
        fprintf(stderr, "ES2 Prism: Error - reported GLX version = %d.%d\n", major, minor);
        fprintf(stderr, "    GLX version 1.3 or higher is required\n");

        return GL_FALSE;
    }

    return GL_TRUE;
}

