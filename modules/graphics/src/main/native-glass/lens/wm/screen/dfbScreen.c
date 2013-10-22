/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 
#include "LensCommon.h"
#include "input/LensInput.h"
#include "wm/LensWindowManager.h"
#include "com_sun_glass_ui_Window.h"

#include <link.h>
#include <limits.h>
#include <dlfcn.h>
#include <fcntl.h>

#include "directfb.h"
#include "directfb_util.h"
#include "directfb_version.h"



int _lastError = DFB_OK; //used to save execution status in DFBBREAK() macro

//DirectFB library files, in order of preference
static const char *dfb_library_files[] = {
    "libdirectfb-1.4.so.15",
    "libdirectfb-1.4.so.6",
    "libdirectfb-1.4.so.5",
    "libdirectfb.so",
    NULL
};

unsigned int dfb_major_version;
unsigned int dfb_minor_version;
unsigned int dfb_micro_version;

typedef const char *(*func_dfb_window_event_type_name_t)(DFBWindowEventType type);
func_dfb_window_event_type_name_t func_dfb_window_event_type_name;

typedef const char *(*func_dfb_pixelformat_name_t)(DFBSurfacePixelFormat format);
func_dfb_pixelformat_name_t func_dfb_pixelformat_name;

#define DFBBREAK(x...)                                                        \
    {                                                                         \
        _lastError = x;                                                       \
        if (_lastError != DFB_OK) {                                           \
            fprintf( stderr, "DFB error, code %d, at %s <%d>:\n\t",           \
                     _lastError, __FILE__, __LINE__ );                        \
            break;                                                            \
        }                                                                     \
    }

struct _PlatformWindowData {
    IDirectFBWindow *dfbWindow;
    
    u8 opacity;

    jboolean isUnderGrab;

    //access point for the window event buffer, used for sending custom
    //events and also used by the robot implementation
    IDirectFBEventBuffer *eventBuffer;

};

struct _PlatformViewData {
    IDirectFBSurface *surface;
    /**
     * When fullscreen is asked to keep ratio of window the surface
     * will contain the actual drawing surface and mainSurface will
     * hold the entire screen
     */
    IDirectFBSurface *mainSurface;

    int index;
    int pitch;
    unsigned char *frameBuffer;
};


static IDirectFB *dfb = NULL; //DFB main interface
static IDirectFBDisplayLayer *primaryLayer = NULL; // the primary layer interface
static DFBDisplayLayerConfig primConfig; //configuration of the primary layer

static int initialized = JNI_FALSE;
static struct _NativeScreen screen;

IDirectFBDisplayLayer *dfbGetPrimaryLayer() {
    return primaryLayer;
}

IDirectFB *dfb_get_main_interface() {
    return dfb;
}

DFBDisplayLayerConfig *dfbGetPrimaryConfig() {
    return &primConfig;
}

/**
 * service function to release any DFB related resources
 *
 * TODO: at this point there is no shutdown procedure and no
 * resources cleaning implementation like close window. need to
 * implement
 */
static void releaseReasources() {
    DEBUG_FUNC_ENTRY();

    if (primaryLayer != NULL) {
        GLASS_LOG_FINE("Releasing primaryLayer");
        GLASS_LOG_FINER("IDirectFBDisplayLayer->Release(primaryLayer =%p)",
                        primaryLayer);
        primaryLayer->Release(primaryLayer);
    }
    if (dfb != NULL) {
        GLASS_LOG_FINE("Releasing DFB interface");
        GLASS_LOG_FINER("IDirectFB->Release(dfb=%p)", dfb);
        dfb->Release(dfb);
    }

    DEBUG_FUNC_EXIT();
}

/**
 * Service function to enable/disable the console cursor blink
 * 
 * @param mode JNI_TRUE enabled, JNI_FALSE disabled
 * 
 * @return jboolean JNI_TRUE on success
 */
static jboolean dfb_setCursorBlink(jboolean mode) {
     
     int fd = open ("/sys/class/graphics/fbcon/cursor_blink", O_WRONLY);
     jboolean result = JNI_FALSE;

     GLASS_LOG_FINE("Trying to %s the console cursor bling",
                    (mode)?"enable" : "disable");

     if (fd < 0) {
         GLASS_LOG_WARNING("Failed to open /sys/class/graphics/fbcon/cursor_blink"
                           ", errno %i -%s", errno, strerror(errno));
     } else {
         char c = (mode)? 1 : 0;
         int res = write(fd, &c, sizeof(char));
         if (res == sizeof(char)) { // we tried to write 1 byte
             GLASS_LOG_FINE("command prompt cursor %s",
                             (mode)?"enabled" : "disabled");
             result = JNI_TRUE;
         } else {
             GLASS_LOG_WARNING("Failed to %s command prompt, errno %i - %s",
                                (mode)?"enable" : "disable",
                               errno, strerror(errno));
         }
         close(fd);
     }

     return result;
}


/**
 * Function will load DFB library dynamically, cash function
 * pointers of DFB library and create the main DFB interface
*/
jboolean glass_application_initialize(JNIEnv *env) {

    DEBUG_FUNC_ENTRY();

    if (initialized == JNI_TRUE) {

        DEBUG_FUNC_EXIT();
        return JNI_TRUE;
    }

    do {
        DFBRectangle visibleRect;
        void *libdirectfb = NULL;
        const char *libdirectfbName;
        int i;
        typedef DFBResult(*func_DirectFBInit_t)(int *, char * (*argv[]));
        typedef DFBResult(*func_DirectFBCreate_t)(IDirectFB **);
        func_DirectFBInit_t func_DirectFBInit;
        func_DirectFBCreate_t func_DirectFBCreate;
        //Load DirectFB
        for (i = 0; libdirectfb == NULL && dfb_library_files[i] != NULL; i++) {
            libdirectfbName = dfb_library_files[i];
            GLASS_LOG_FINE("dlopen(%s, RTLD_NOW)", libdirectfbName);
            libdirectfb = dlopen(libdirectfbName, RTLD_NOW);
        }
        if (libdirectfb == NULL) {
            GLASS_LOG_SEVERE("Failed to load DirectFB shared object");
            break;
        } else {
            char path[PATH_MAX];
            const unsigned int *p_directfb_major_version;
            const unsigned int *p_directfb_minor_version;
            const unsigned int *p_directfb_micro_version;
            p_directfb_major_version = (const unsigned int *) dlsym(
                                           libdirectfb, "directfb_major_version");
            p_directfb_minor_version = (const unsigned int *) dlsym(
                                           libdirectfb, "directfb_minor_version");
            p_directfb_micro_version = (const unsigned int *) dlsym(
                                           libdirectfb, "directfb_micro_version");
            if (p_directfb_major_version == NULL
                    || p_directfb_minor_version == NULL
                    || p_directfb_micro_version == NULL) {
                GLASS_LOG_SEVERE("Could not locate DirectFB version information");
                break;
            }
            dfb_major_version = *p_directfb_major_version;
            dfb_minor_version = *p_directfb_minor_version;
            dfb_micro_version = *p_directfb_micro_version;
            path[0] = '\0';
#ifdef __USE_GNU
            dlinfo(libdirectfb, RTLD_DI_ORIGIN, path);
            strcat(path, "/");
#endif
            strcat(path, libdirectfbName);
            GLASS_LOG_CONFIG("Loaded DirectFB shared object from %s", path);
            GLASS_LOG_CONFIG("DirectFB version is %u.%u.%u",
                             dfb_major_version,
                             dfb_minor_version,
                             dfb_micro_version);
#if DIRECTFB_MAJOR_VERSION == 1 && DIRECTFB_MINOR_VERSION < 4
            if (dfb_major_version == 1 && dfb_minor_version >= 4) {
                GLASS_LOG_WARNING(
                    "Compiled against an earlier version of DirectFB. "
                    "Some functionality may be missing.");
            }
#endif
        }
        //Find function pointers in the DirectFB library
        func_DirectFBInit = (func_DirectFBInit_t)
                            dlsym(libdirectfb, "DirectFBInit");
        if (func_DirectFBInit == NULL) {
            GLASS_LOG_SEVERE("Cannot locate function DirectFBInit");
            break;
        }
        func_DirectFBCreate = (func_DirectFBCreate_t)
                              dlsym(libdirectfb, "DirectFBCreate");
        if (func_DirectFBCreate == NULL) {
            GLASS_LOG_SEVERE("Cannot locate function DirectFBCreate");
            break;
        }
        func_dfb_window_event_type_name = (func_dfb_window_event_type_name_t)
                                          dlsym(libdirectfb, "dfb_window_event_type_name");
        if (func_dfb_window_event_type_name == NULL) {
            GLASS_LOG_SEVERE("Cannot locate function dfb_window_event_type_name");
            break;
        }
        func_dfb_pixelformat_name = (func_dfb_pixelformat_name_t)dlsym(libdirectfb, "dfb_pixelformat_name");
        if (func_dfb_pixelformat_name == NULL) {
            GLASS_LOG_SEVERE("Cannot locate function dfb_pixelformat_name");
            break;
        }
        //Disable signal handler and don't long input driver
        const char *args[] = {
            "java",
            "--dfb:no-deinit-check,no-sighandler,disable-module=linux_input" 
        };
        int         argc   = sizeof(args) / sizeof(args[0]);
        char      **argp   = (char **) args;

        GLASS_LOG_INFO("DirectFBInit %s", args[1]);
        DFBBREAK(func_DirectFBInit(&argc, &argp));  //Init DFB, must be called first
        DFBBREAK(func_DirectFBCreate(&dfb));   //create the main DFB interface
        GLASS_LOG_INFO("DirectFBCreate returned, dfb := %p", dfb);
        GLASS_LOG_FINER("IDirectFB->SetCooperativeLevel(dfb=%p, DFSCL_NORMAL)", dfb);
        DFBBREAK(dfb->SetCooperativeLevel(dfb, DFSCL_NORMAL));

        //init primary layer
        GLASS_LOG_FINER("IDirectFB->GetDisplayLayer(dfb=%p, DLID_PRIMARY, "
                        "&primaryLayer)", dfb);
        DFBBREAK(dfb->GetDisplayLayer(dfb, DLID_PRIMARY, &primaryLayer)); //get the primary layer
        GLASS_LOG_INFO(
            "IDirectFB->GetDisplayLayer(dfb=%p, DLID_PRIMARY) returned %p",
            dfb, primaryLayer);
        GLASS_LOG_FINER("IDirectFBDisplayLayer->SetCooperativeLevel(layer=%p, "
                        "DLSCL_ADMINISTRATIVE", primaryLayer);
        DFBBREAK(primaryLayer->SetCooperativeLevel(primaryLayer,
                                                   DLSCL_ADMINISTRATIVE));

        GLASS_LOG_FINE("Disable mouse");
        DFBBREAK(primaryLayer->EnableCursor(primaryLayer, 0));

        GLASS_LOG_FINER("Setting background repaint to used solid color");
        DFBBREAK(primaryLayer->SetBackgroundMode(primaryLayer, DLBM_COLOR));

        GLASS_LOG_FINE("Setting background color to black");
        DFBBREAK(primaryLayer->SetBackgroundColor(primaryLayer,
                                                  0 /* r */,
                                                  0 /* g */,
                                                  0 /* b */,
                                                  255 /* a */));

        //get layer configuration
        GLASS_LOG_FINE("IDirectFBDisplayLayer->GetConfiguration(layer=%p)",
                       primaryLayer);
        DFBBREAK(primaryLayer->GetConfiguration(primaryLayer, &primConfig));
        GLASS_LOG_INFO("layer %p size=%ix%i pixelformat=%s (code 0x%x)",
                       primaryLayer,
                       primConfig.width, primConfig.height,
                       func_dfb_pixelformat_name(primConfig.pixelformat),
                       primConfig.pixelformat);

        initialized = JNI_TRUE;

        //try to disable the command prompt cursor
        dfb_setCursorBlink(JNI_FALSE);

    } while (0);

    if (initialized != JNI_TRUE) {
        GLASS_LOG_SEVERE("Failed to initialize DirectFB");
        releaseReasources();
    }

    DEBUG_FUNC_EXIT();
    return initialized;
}

void lens_platform_shutdown(JNIEnv *env) {

    //all windows should got close request by now
    GLASS_LOG_FINE("native shutdown");

    //try to enable the command prompt cursor
    dfb_setCursorBlink(JNI_TRUE);

    GLASS_LOG_FINE("Release DFB resources");
    releaseReasources();
    GLASS_LOG_FINE("DFB shutdown complete");
}

jboolean glass_window_setAlpha(JNIEnv *env, NativeWindow window, float alpha) {
    DEBUG_FUNC_ENTRY();
    jboolean result = JNI_FALSE;

    do {
        IDirectFBWindow *dfbWindow = window->data->dfbWindow;
        GLASS_LOG_FINE("IDirectFBWindow->SetOpacity(window=%p, %i)",
                       dfbWindow, (int)(alpha * 255));
        DFBBREAK(dfbWindow->SetOpacity(dfbWindow, (alpha * 255)/*convert to u8 */));

        //save current alpha level as the actual level set may be different
        DFBBREAK(dfbWindow->GetOpacity(dfbWindow, &(window->data->opacity)));
        GLASS_LOG_FINE("IDirectFBWindow->GetOpacity(window=%p) returned %i",
                       dfbWindow, window->data->opacity);
        result = JNI_TRUE;
    } while (0);

    DEBUG_FUNC_EXIT();
    return result;
}

void glass_pixel_attachIntBuffer(JNIEnv *env, jint *src,
                                 NativeWindow window,
                                 jint width, jint height, int offset) {

    NativeView view = window->view;
    IDirectFBWindow* dfbWindow = window->data->dfbWindow;
    IDirectFBSurface *surface = view->data->surface;
    NativeScreen primaryScreen = glass_screen_getMainScreen();

    GLASS_LOG_FINER("Repaint %ix%i", width, height);
    

    do {
        unsigned char *fb;
        int pitch;
        jboolean dimensionsUpdated = JNI_FALSE;
        int _x, _y, _w, _h;

        DFBBREAK(dfbWindow->GetPosition(dfbWindow, &_x, &_y));
        DFBBREAK(dfbWindow->GetSize(dfbWindow, &_w, &_h));

        //check for window update
        if (_x != window->currentBounds.x ||
            _y != window->currentBounds.y ||
            _w != window->currentBounds.width ||
            _h != window->currentBounds.height) {

            GLASS_LOG_FINER("Window dimentions have been changed, updating");

            //hide the window so no "noise" pixels will be displayed where the
            //surface wasn't yet updated
            DFBBREAK(dfbWindow->SetOpacity(window->data->dfbWindow, 0));

            //update window dimensions
            DFBBREAK(dfbWindow->SetBounds(window->data->dfbWindow,
                                                        window->currentBounds.x,
                                                        window->currentBounds.y,
                                                        window->currentBounds.width,
                                                        window->currentBounds.height));
            dimensionsUpdated = JNI_TRUE;
        }
        
        GLASS_LOG_FINER("Getting window's %i surface", window->id);
        DFBBREAK(dfbWindow->GetSurface(window->data->dfbWindow ,
                                                     &view->data->surface));
        surface = view->data->surface;
        if (!surface) {
            GLASS_LOG_WARNING(
                "window structure %p surface is NULL - window may be closing",
            window);
            return;
        }

        //update surface dimensions
        DFBBREAK(view->data->surface->GetPosition(view->data->surface,
                                                  &(view->bounds.x),
                                                  &(view->bounds.y)));
        DFBBREAK(view->data->surface->GetSize(view->data->surface,
                                                  &(view->bounds.width),
                                                  &(view->bounds.height)));

        GLASS_LOG_FINER("surface = %p x=%i, y=%i, w=%i h=%i",
                       view->data->surface,
                       view->bounds.x,
                       view->bounds.y,
                       view->bounds.width,
                       view->bounds.height);
        
        

        DFBBREAK(surface->Lock(surface, DSLF_WRITE,
                               (void **) &fb, &pitch));
        
        if (view->bounds.width > pitch || height > view->bounds.height) {
            GLASS_LOG_FINER("attachIntBuffer was called with width = %d height = %d "
                           "offset = %d",
                           width, height, offset);
            GLASS_LOG_WARNING("Window %d[%p] surface dimensions (%iX%i) are smaller than requested "
                              "width and height. Window size may have been resized "
                              "before the Java window have been notified. ignoring",
                              window->id, window,
                              view->bounds.width, view->bounds.height);

            GLASS_LOG_FINEST("IDirectFBSurface->Unlock(surface=%p)", surface);
            DFBBREAK(surface->Unlock(surface));
            //unhide window
            if (dimensionsUpdated) {
                DFBBREAK(dfbWindow->SetOpacity(window->data->dfbWindow, window->alpha*255));
            }
            return;
        }
        
        GLASS_LOG_FINEST(
            "IDirectFBSurface->Lock(surface=%p, DSLF_WRITE) returned data=%p pitch=%i",
            surface, fb, pitch);

        switch (primaryScreen->depth) {
            case 32:
                /**
                 * check if we are using the full surface width if yes, just
                 * copy the src buffer as is.
                 */
                 GLASS_LOG_FINEST("Rendering in 32bit, pitch = %i, widht = %i, height = %i",
                                pitch, width, height);
                if (pitch / 4  == width) {
                     GLASS_LOG_FINEST("Repainting all window");
                    memcpy(fb, src, width * height * 4);
                } else {
                    /**
                     * We are using only a sub surface.
                     * copy pixels from src to the begging of the framebuffer row
                     * according to the src array width.
                     * After we are done we need to advance the src index to the
                     * next row (+width * 4), advance the framebuffer index to the
                     * next row (+pitch)
                     */
                     GLASS_LOG_FINEST("repainting sub-surface");

                    unsigned char *__src = (unsigned char *) src;
                    unsigned char *__fb = fb; //why we need this? fb is also unsigned char
                    int i;
                    for (i = 0; i < height; i++) {
                        memcpy(__fb, __src, width * 4); //copy partial row
                        __src += width * 4; //go to the src next row
                        __fb += pitch; //go to the fb next row
                    }
                }
                break;
            case 16: {
                jint *__src = src; //why we need this? src is also jint*
                int i;

                /**
                 * We need to convert the pixels to 16 bits
                 */
                for (i = 0; i < height; i++) { //traverse the rows
                    //get the row indexed by i
                    jchar *__fb = (jchar *)(fb + pitch * i);  //jchar is used for 16 bits type cast
                    int j;
                    for (j = 0; j < width; j += 1) { //traverse the column
                        jint pixel = *__src++; //read the pixel, and advance index
                        //convert the pixel to 565
                        *__fb++ = ((pixel >> 8) & 0xf800)
                                  | ((pixel >> 5) & 0x7e0)
                                  | ((pixel >> 3) & 0x1f);
                    }
                }

            }
            break;
        }
        GLASS_LOG_FINEST("IDirectFBSurface->Unlock(surface=%p)", surface);
        DFBBREAK(surface->Unlock(surface));
        GLASS_LOG_FINEST("IDirectFBSurface->Flip(surface=%p, DSFLIP_WAIT)",
                         surface);
        DFBBREAK(surface->Flip(surface, NULL, DSFLIP_WAIT));
        //we can unhide window as its content is up-to-date 
        if (dimensionsUpdated) {
            DFBBREAK(dfbWindow->SetOpacity(window->data->dfbWindow, window->alpha*255));
        }
    } while (0);
}


LensResult glass_window_PlatformWindowData_create(JNIEnv *env,
                                                  NativeWindow window) {


    DFBWindowDescription wdc;

    int result = LENS_FAILED;

    do {

        //for ease of use;
        int mask_transparent =  com_sun_glass_ui_Window_TRANSPARENT;


        IDirectFBDisplayLayer *primaryLayer = dfbGetPrimaryLayer();

        //alocate the nativeData structure
        PlatformWindowData data = (PlatformWindowData)calloc(1,
                                      sizeof(struct _PlatformWindowData));
        if (data == NULL) {
            GLASS_LOG_SEVERE("Failed to allocate native window structure");
            break;
        }
        GLASS_LOG_FINE("Allocated PlatformWindowData structure %p", data);

        data->isUnderGrab = JNI_FALSE;

        //set default values
        wdc.flags  = DWDESC_POSX | DWDESC_POSY |
                     DWDESC_WIDTH | DWDESC_HEIGHT;

        //use default values
        wdc.posx   = window->currentBounds.x;
        wdc.posy   = window->currentBounds.y;
        wdc.width  = window->currentBounds.width;
        wdc.height = window->currentBounds.height;

        if ((window->creationMask & mask_transparent) ==  mask_transparent) {
            //check if alpha is supported
            if (window->screen->depth == 32) {
                //window will support alpha chanel
                wdc.flags |= DWDESC_CAPS;
                wdc.caps  = DWCAPS_ALPHACHANNEL ;
            }
        }

        GLASS_LOG_FINE(
            "IDirectFBDisplayLayer->CreateWindow(layer=%p)",
            primaryLayer);
        DFBBREAK(primaryLayer->CreateWindow(primaryLayer, &wdc,
                                            &data->dfbWindow));
        GLASS_LOG_FINE(
            "IDirectFBDisplayLayer->CreateWindow returned %p",
            data->dfbWindow);

        DFBBREAK(data->dfbWindow->GetID(data->dfbWindow,
                                       (DFBWindowID *)&window->id));
        GLASS_LOG_FINE(
            "IDirectFBWindow->GetID(window=%p) returned %p",
            data->dfbWindow, window->id);

        //window need to be invisible until setvisible(true) is called
        //GLASS_LOG_FINE("IDirectFBWindow->SetOpacity(window=%p, 0)",
        //               data->dfbWindow);
        //DFBBREAK(data->dfbWindow->SetOpacity(data->dfbWindow,
        //                                     0));
        DFBBREAK(data->dfbWindow->GetOpacity(data->dfbWindow,
                                             &(data->opacity)));
        GLASS_LOG_FINE("IDirectFBWindow->GetOpacity(window=%p) returned %i",
                       data->dfbWindow, data->opacity);

        window->state = NWS_NORMAL;
        //update window list

        window->data = data;

        GLASS_LOG_FINE("updating window");
        //dfbUpdateWindowAndView(window);

        result = LENS_OK;
    } while (0);

    //in case of failure caller will release resources

    return result;
}

LensResult glass_window_PlatformWindowRelease(JNIEnv *env, NativeWindow window) {

    int result = LENS_FAILED;

    PlatformWindowData data = window->data;

    do {

        if (data) {
            if (data->dfbWindow) {
                DFBBREAK(data->dfbWindow->Destroy(data->dfbWindow));
                GLASS_LOG_FINE("IDirectFBWindow->Release(window=%p)",
                               data->dfbWindow);
                DFBBREAK(data->dfbWindow->Release(data->dfbWindow));
                data->dfbWindow = NULL;
            }

            GLASS_LOG_INFO("Freeing data %p", data);
            free(data);
        }

        result = LENS_OK;
    } while (0);

    return result;
}

LensResult glass_view_PlatformViewData_create(NativeView view) {

    GLASS_LOG_FINE("Allocating PlatformViewData");
    int result = LENS_FAILED;
    PlatformViewData platformView  = (PlatformViewData)malloc(
                                         sizeof(struct _PlatformViewData));
    if (platformView != NULL) {
        GLASS_LOG_FINE("Alloc returned %p", platformView);
        memset(platformView, 0, sizeof(struct _PlatformViewData));
        GLASS_LOG_FINE("view(%p)->data = %p", view, platformView);
        view->data = platformView;
        result = LENS_OK;
    } else {
        GLASS_LOG_SEVERE("Failed to allocate PlatformViewData");
    }

    return result;


}

LensResult glass_view_PlatformViewRelease(JNIEnv *env, NativeView view) {

    PlatformViewData data = view->data;

    GLASS_LOG_FINE("Releasing PlatformViewData %p", data);
    int result = LENS_FAILED;
    do {
        if (data->surface) {
            GLASS_LOG_FINE("IDirectFBSurface->Release(surface=%p)",
                           data->surface);
            DFBBREAK(data->surface->Release(data->surface));
        }
        if (data->mainSurface) {
            GLASS_LOG_FINE("IDirectFBSurface->Release(mainSurface=%p)",
                           data->mainSurface);
            DFBBREAK(data->mainSurface->Release(data->mainSurface));
        }
        GLASS_LOG_FINE("free(%p)", data);
        free(data);
        result = LENS_OK;
    } while (0);

    return result;
}

NativeScreen lens_screen_initialize(JNIEnv *env) {

    DFBDisplayLayerConfig *primConfig = dfbGetPrimaryConfig();
    GLASS_LOG_FINE("Using primConfig (%p) to create NativeScreenHandle");

    screen.x = 0;
    screen.y = 0;
    screen.width = primConfig->width;
    screen.height = primConfig->height;
    switch (primConfig->pixelformat) {
        case DSPF_RGB16:
            screen.depth = 16;
            break;
        case DSPF_RGB24:
        case DSPF_RGB32:
        case DSPF_ARGB:
            screen.depth = 32;
            break;
        default:
            GLASS_LOG_SEVERE("Unknown pixel format 0x%x",
                             primConfig->pixelformat);
            return NULL;
    }
    GLASS_LOG_FINE("Layer depth is %i\n", screen.depth);

    screen.resolutionX = 72; //Not sure how to determine this..
    screen.resolutionY = 72; //Not sure how to determine this..
    screen.visibleX = 0;
    screen.visibleY = 0;
    screen.visibleWidth = primConfig->width;
    screen.visibleHeight = primConfig->height;

    return &screen;
}

void *glass_window_getPlatformWindow(JNIEnv *env, NativeWindow window) {
    return window;
}

char *lens_screen_getFrameBuffer() {
    return NULL;
}

void glass_screen_clear() {
    // NOOP
}

jboolean glass_screen_capture(jint x,
                              jint y,
                              jint width,
                              jint height,
                              jint *pixels) {

    IDirectFBDisplayLayer *primaryLayer = dfbGetPrimaryLayer();
    jboolean result = JNI_FALSE;
    jboolean surfaceLock = JNI_FALSE;

    if (primaryLayer != NULL) {

        do {
            DFBSurfacePixelFormat pixelFormat;
            IDirectFBSurface *primarySurface;
            unsigned char *fb;

            /**
             * pitch is the number of bytes in a row. This value will change
             * according to the pixel format.
             * So for example a surface with a width of 600 pixels with 32
             * bit pixel format (4 bytes) will have a pitch of 2400 (600 X
             * 4)
             */
            int pitch;


            /**
             * used for traversing the pixels in a row. The offset represent
             * the pixel number and not the actuall offset of the buffer.
             * The actual offset will bebased on the pixel size according to
             * the pixel format.
             */
            int row;
            int surfaceWidth, surfaceHeight;

            //get the surface from the primary layer
            DFBBREAK(primaryLayer->GetSurface(primaryLayer, &primarySurface));

            DFBBREAK(primarySurface->GetSize(primarySurface, &surfaceWidth,
                                             &surfaceHeight));

            GLASS_LOG_FINE("primary surface size w=%d h=%d",
                           surfaceWidth, surfaceHeight);


            if ((x + width) > surfaceWidth || y + height > surfaceHeight) {

                //TODO:expected behavior is not clear, should we fail or clip to size?
                GLASS_LOG_WARNING("[Error] Pixel(s) requested is out of"
                                  " surface bounds");
                result = JNI_FALSE;
                break;                
            }

            DFBBREAK(primarySurface->Lock(primarySurface,  DSLF_READ,
                                          (void **)(&fb), &pitch));

            surfaceLock = JNI_TRUE;

            // get the surface pixel format
            DFBBREAK(primarySurface->GetPixelFormat(primarySurface, &pixelFormat));
            //lock the surface and get access to the frame buffer


            GLASS_LOG_FINE("getPixel x=%d, y=%d, width=%d, height=%d, pitch=%d",
                           x, y, width, height, pitch);

            switch (pixelFormat) {
                
                case DSPF_RGB24:
                case DSPF_RGB32:
                case DSPF_ARGB: {
                        unsigned char *pixelsAsChar = (unsigned char *)pixels;

                        //set the pointer to the first pixel
                        fb += (y * pitch) + (x * 4);

                        GLASS_LOG_FINEST("fb moved  0%x | 0%x | 0%x | 0%x | ",
                                         fb[0], fb[1],
                                         fb[2], fb[3]);

                        for (row = 0; row < height; row++) {
                            memcpy(pixelsAsChar, fb, width * 4);
                            fb += pitch; //go to next row with same X
                            pixelsAsChar += width * 4; //go to the last copied pixel, i.e
                            //next row
                        }

                    }
                    //all done
                    result = JNI_TRUE;
                    break;

                case DSPF_RGB16: {
                        int column;
                        int pixelNumber = 0;

                        //set the pointer to the first pixel (16 bits)
                        fb += (y * pitch) + (x * 2);

                        for (row = 0; row < height; row++) {
                            for (column = 0; column < width; column++) {

                                //convert the pixel for a 16 primitive for ease of use
                                jshort pixel = (jshort)(fb[1] << 8 | fb[0]);

                                /**
                                 * We are dealing with 5-6-5 pixel format, so we need 
                                 * to mask out the relevant bits of each color, 
                                 * turn it to 8-8-8 pixel format and shift them to the proper 
                                 * location as the pixel is represented in 32 bits. 
                                 */

                                //Masking out the relevant bits of each color
                                int r = ( pixel & 0xF800 ) >> 11;
                                int g = ( pixel & 0x07E0 ) >> 5;
                                int b = ( pixel & 0x001F );

                                //Conversion of each color to 8-8-8 pixel format
                                 r = (r << 3) + (r >> 2);  // 5 bits to 8 bits
                                 g = (g << 2) + (g >> 4);  // 6 bits to 8 bits
                                 b = (b << 3) + (b >> 2);  // 5 bits to 8 bits


                                pixels[pixelNumber++] =
                                (jint) ((0xFF << 24) | (r << 16) | (g << 8) | b);

                                //next pixel
                                fb += 2;

                            } //end for column

                            //next row
                            fb += pitch;

                        }//end for row

                    }
                    result = JNI_TRUE;
                    //all done
                    break;
                default:
                    GLASS_LOG_WARNING("ERROR: unknown pixel format %i\n",
                                      pixelFormat);
                    result = JNI_FALSE;
            }

            if (surfaceLock) {
                DFBBREAK(primarySurface->Unlock(primarySurface));
            }
        } while (0);

    } else {
        GLASS_LOG_SEVERE("Failed to get dfb primary layer");
    }

    return result;

}

LensResult lens_platform_windowMinimize(JNIEnv *env,
                                        NativeWindow window,
                                        jboolean toMinimize) {
    GLASS_LOG_FINE("Calling lens_platform_windowSetVisible(window %i[%p], %s)",
                   window?window->id : -1,
                   window,
                   (!toMinimize)?"true" : "false");

    return lens_platform_windowSetVisible(env, window, !toMinimize);
}


LensResult lens_platform_windowSetVisible(JNIEnv *env,
                                        NativeWindow window,
                                        jboolean visible) {

    IDirectFBWindow *dfbWindow = window->data->dfbWindow;
    jboolean result = JNI_FALSE;

    GLASS_LOG_FINE("Setting window %i[%p] to %s",
                   window->id, window,
                   (visible)?"visible":"invisible");

    do {        
        if (visible == JNI_TRUE) {
            GLASS_LOG_FINE("IDirectFBWindow->SetOpacity(window=%p, %i)",
                           dfbWindow, window->data->opacity);
            DFBBREAK(dfbWindow->SetOpacity(dfbWindow, window->data->opacity));
        } else {
            GLASS_LOG_FINE("IDirectFBWindow->SetOpacity(window=%p, 0)",
                           dfbWindow);
            DFBBREAK(dfbWindow->SetOpacity(dfbWindow, 0));
        }
        result = JNI_TRUE;
    } while (0);

    return result;
}
