/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

/*
 * monocle_egl_ext.h - the contract between Monocle's EGL platform (-Dmonocle.platform=EGL) and the
 * vendor library named by -Dmonocle.egl.lib: the 23 functions that library must export. This is
 * the JNI-free republication of egl_ext.h: jlong -> int64_t, jint -> int32_t, jboolean -> uint8_t,
 * jfloat -> float, int -> int32_t, jbyte* -> const uint8_t*. Every width and the calling
 * convention are the same on LP64 (aarch64, x86-64), so a vendor library built against the old
 * egl_ext.h keeps working unchanged - binary compatibility is preserved; only a vendor that
 * #includes the header sees the spelling change, and egl_ext.h stays for one release as an alias
 * of this file.
 *
 * Java binds the 23 symbols directly (com.sun.glass.ui.monocle.EglVendorNative, which replaced the
 * JNI forwarders of eglBridge.c at commit 21d5a654f6) from the library -Dmonocle.egl.lib names,
 * which EGLPlatform first loads with dlopen(RTLD_LAZY | RTLD_GLOBAL) as before, so the vendor
 * library's own dependencies (libEGL, libGLESv2, libgbm, libdrm, ...) stay globally visible.
 * Booleans: any non-zero uint8_t is true. Handles are passed through unchanged; the vendor
 * library decides what a handle is.
 *
 * Call order the Java side relies on:
 *
 *   EGLAcceleratedScreen(int[] attributes) - once, when the accelerated screen is first requested:
 *     getNativeWindowHandle(cardId)            cardId = -Degl.displayid, default "/dev/dri/card1"
 *     getEglDisplayHandle()
 *     doEglInitialize(display)                 display = the handle just returned
 *     doEglBindApi(0x30A0)                     EGL_OPENGL_ES_API
 *     doEglChooseConfig(display, attributes)   attributes = Prism's GLPixelFormat.Attributes array,
 *                                              EXACTLY SEVEN int32_t (RED_SIZE, GREEN_SIZE, BLUE_SIZE,
 *                                              ALPHA_SIZE, DEPTH_SIZE, DOUBLEBUFFER, ONSCREEN), 28 bytes,
 *                                              NO terminator - it is not an EGL attribute list and must
 *                                              never be scanned for EGL_NONE; the vendor translates it
 *                                              (reference: EGL.setEGLAttrs of the Java EGL facade,
 *                                              formerly setEGLAttrs of EGL.c); -1 -> IllegalArgumentException
 *     doEglCreateWindowSurface(display, config, window)
 *     doEglCreateContext(display, config)
 *   then, driven by rendering: doEglMakeCurrent(display, surface, surface, context) from
 *     enableRendering(true), doEglMakeCurrent(display, 0, 0, context) from enableRendering(false),
 *     doEglSwapBuffers(display, surface) once per frame under NativeScreen.framebufferSwapLock.
 *
 *   EGLPlatform.createScreens: doGetNumberOfScreens() once, then for each idx a new EGLScreen(idx)
 *     calls doGetHandle, doGetDepth, doGetNativeFormat, doGetWidth, doGetHeight, doGetOffsetX,
 *     doGetOffsetY, doGetDpi, doGetScale, in that order, once, at construction; createScreen()
 *     constructs EGLScreen(0) the same way.
 *
 *   EGLCursor (the hardware cursor, unless -Dmonocle.egl.swcursor=true): doInitCursor(16, 16) at
 *     construction, then doSetCursorVisibility, doSetLocation and doSetCursorImage(img, length) as
 *     Glass drives the cursor. img is valid only for the duration of the call: copy it.
 */

#ifndef MONOCLE_EGL_EXT_H
#define MONOCLE_EGL_EXT_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/* get a handle to the native window (without specifying what window is) */
extern int64_t getNativeWindowHandle(const char *cardId);

/* get a handle to the EGL display */
extern int64_t getEglDisplayHandle(void);

/* initialize the EGL system with the specified handle */
extern uint8_t doEglInitialize(void *eglDisplay);

/* bind a specific API to the EGL system */
extern uint8_t doEglBindApi(int32_t api);

/* choose an EGL configuration for Prism's GLPixelFormat.Attributes array: attribs points at EXACTLY
 * seven int32_t - RED_SIZE, GREEN_SIZE, BLUE_SIZE, ALPHA_SIZE, DEPTH_SIZE, DOUBLEBUFFER (0/1),
 * ONSCREEN (0/1) - with no terminator (28 bytes; reading further is out of bounds). It is NOT an EGL
 * attribute list: translate it, as EGL.setEGLAttrs of the Java EGL facade does (the reference
 * translation, formerly setEGLAttrs of EGL.c). Return the chosen config handle, or -1 for none. */
extern int64_t doEglChooseConfig(int64_t eglDisplay, int32_t *attribs);

/* create an EGL Surface for the given display, configuration and window */
extern int64_t doEglCreateWindowSurface(int64_t eglDisplay, int64_t config, int64_t nativeWindow);

/* create an EGL Context for the given display and configuration */
extern int64_t doEglCreateContext(int64_t eglDisplay, int64_t config);

/* enable the specified EGL system */
extern uint8_t doEglMakeCurrent(int64_t eglDisplay, int64_t drawSurface, int64_t readSurface,
                                int64_t eglContext);

/* swap buffers (and render frontbuffer) */
extern uint8_t doEglSwapBuffers(int64_t eglDisplay, int64_t eglSurface);

/* get the number of native screens in the current configuration */
extern int32_t doGetNumberOfScreens(void);

/* get specific information about each screen; idx says which screen is queried */
extern int64_t doGetHandle(int32_t idx);
extern int32_t doGetDepth(int32_t idx);
extern int32_t doGetWidth(int32_t idx);
extern int32_t doGetHeight(int32_t idx);
extern int32_t doGetOffsetX(int32_t idx);
extern int32_t doGetOffsetY(int32_t idx);
extern int32_t doGetDpi(int32_t idx);
extern int32_t doGetNativeFormat(int32_t idx);
extern float doGetScale(int32_t idx);

/* initialize a hardware cursor with specified dimensions */
extern void doInitCursor(int32_t width, int32_t height);

/* show/hide the hardware cursor */
extern void doSetCursorVisibility(uint8_t val);

/* point the hardware cursor to the provided location */
extern void doSetLocation(int32_t x, int32_t y);

/* use the specified image as cursor image (length bytes) */
extern void doSetCursorImage(const uint8_t *img, int32_t length);

#ifdef __cplusplus
}
#endif

#endif /* MONOCLE_EGL_EXT_H */
