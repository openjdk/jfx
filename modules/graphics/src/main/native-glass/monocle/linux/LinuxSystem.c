/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

#include "com_sun_glass_ui_monocle_linux_LinuxSystem.h"
#include "com_sun_glass_ui_monocle_linux_LinuxSystem_FbVarScreenInfo.h"
#include "com_sun_glass_ui_monocle_linux_LinuxSystem_InputAbsInfo.h"
#include "Monocle.h"

#include <fcntl.h>
#include <linux/fb.h>
#include <linux/input.h>
#include <sys/ioctl.h>
#include <errno.h>
#include <string.h>
#include <dlfcn.h>
#include <unistd.h>

JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_open
  (JNIEnv *env, jobject UNUSED(obj), jstring filenameS, jint flag) {
    const char *filename = (*env)->GetStringUTFChars(env, filenameS, NULL);
    int fd = open(filename, (int) flag);
    (*env)->ReleaseStringUTFChars(env, filenameS, filename);
    return (jlong) fd;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_close
  (JNIEnv *UNUSED(env), jobject UNUSED(obj), jlong fdL) {
    return (jint) close((int) fdL);
}

JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_lseek
  (JNIEnv *UNUSED(env), jobject UNUSED(obj), jlong fdL, jlong offset, jint whence) {
    return (jlong) lseek((int) fdL, (off_t) offset, (int) whence);
}

JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_write
  (JNIEnv *env, jobject UNUSED(obj), jlong fdL, jobject buf) {
    void *data = (*env)->GetDirectBufferAddress(env, buf);
    size_t capacity = (*env)->GetDirectBufferCapacity(env, buf);
    return (jlong) write((int) fdL, data, capacity);
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_EVIOCGABS
  (JNIEnv *UNUSED(env), jobject UNUSED(obj), jint type) {
    return (jint) EVIOCGABS((int) type);
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_ioctl
  (JNIEnv *UNUSED(env), jobject UNUSED(obj), jlong fdL, jint request, jlong dataL) {
    return ioctl((int) fdL, (int) request, asPtr(dataL));
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_IOR
  (JNIEnv *UNUSED(env), jobject UNUSED(obj), jint type, jint number, jint size) {
    return _IOC(_IOC_READ, type, number, size);
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_IOW
  (JNIEnv *UNUSED(env), jobject UNUSED(obj), jint type, jint number, jint size) {
    return _IOC(_IOC_WRITE, type, number, size);
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_IOWR
  (JNIEnv *UNUSED(env), jobject UNUSED(obj), jint type, jint number, jint size) {
    return _IOC(_IOC_READ|_IOC_WRITE, type, number, size);
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_errno
  (JNIEnv *UNUSED(env), jobject UNUSED(obj)) {
    return (jint) errno;
}

JNIEXPORT jstring JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_strerror
  (JNIEnv *env, jobject UNUSED(obj), jint errnum) {
    char *errChars = strerror(errnum);
    return (*env)->NewStringUTF(env, errChars);
}

JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_dlopen
  (JNIEnv *env, jobject UNUSED(obj), jstring filenameS, jint flag) {
    const char *filename = (*env)->GetStringUTFChars(env, filenameS, NULL);
    void *handle = dlopen(filename, (int) flag);
    (*env)->ReleaseStringUTFChars(env, filenameS, filename);
    return asJLong(handle);
}

JNIEXPORT jstring JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_dlerror
  (JNIEnv *env, jobject UNUSED(obj)) {
    char *errChars = dlerror();
    return (*env)->NewStringUTF(env, errChars);
}

JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_dlsym
  (JNIEnv *UNUSED(env), jobject UNUSED(obj), jlong handleL, jstring symbolS) {
    const char *symbol = (*env)->GetStringUTFChars(env, symbolS, NULL);
    void *handle = dlsym(asPtr(handleL), symbol);
    (*env)->ReleaseStringUTFChars(env, symbolS, symbol);
    return asJLong(handle);
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_dlclose
  (JNIEnv *UNUSED(env), jobject UNUSED(obj), jlong handleL) {
    return (jint) dlclose(asPtr(handleL));
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_00024FbVarScreenInfo_sizeof
  (JNIEnv *UNUSED(env), jobject UNUSED(obj)) {
    return (jint) sizeof(struct fb_var_screeninfo);
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_00024FbVarScreenInfo_getXRes
  (JNIEnv *UNUSED(env), jobject UNUSED(obj), jlong p) {
    return (jint) ((struct fb_var_screeninfo *) asPtr(p))->xres;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_00024FbVarScreenInfo_getYRes
  (JNIEnv *UNUSED(env), jobject UNUSED(obj), jlong p) {
    return (jint) ((struct fb_var_screeninfo *) asPtr(p))->yres;
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_00024FbVarScreenInfo_setRes
  (JNIEnv *UNUSED(env), jobject UNUSED(obj), jlong p, int x, int y) {
    struct fb_var_screeninfo *screen = (struct fb_var_screeninfo *) asPtr(p);
    screen->xres = x;
    screen->yres = y;
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_00024FbVarScreenInfo_setVirtualRes
  (JNIEnv *UNUSED(env), jobject UNUSED(obj), jlong p, int x, int y) {
    struct fb_var_screeninfo *screen = (struct fb_var_screeninfo *) asPtr(p);
    screen->xres_virtual = x;
    screen->yres_virtual = y;
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_00024FbVarScreenInfo_setOffset
  (JNIEnv *UNUSED(env), jobject UNUSED(obj), jlong p, int x, int y) {
    struct fb_var_screeninfo *screen = (struct fb_var_screeninfo *) asPtr(p);
    screen->xoffset = x;
    screen->yoffset = y;
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_00024FbVarScreenInfo_setActivate
  (JNIEnv *UNUSED(env), jobject UNUSED(obj), jlong p, int activate) {
    struct fb_var_screeninfo *screen = (struct fb_var_screeninfo *) asPtr(p);
    screen->activate = activate;
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_00024FbVarScreenInfo_setBitsPerPixel
  (JNIEnv *UNUSED(env), jobject UNUSED(obj), jlong p, int bpp) {
    struct fb_var_screeninfo *screen = (struct fb_var_screeninfo *) asPtr(p);
    screen->bits_per_pixel = bpp;
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_00024FbVarScreenInfo_setRed
  (JNIEnv *UNUSED(env), jobject UNUSED(obj), jlong p, int length, int offset) {
    struct fb_var_screeninfo *screen = (struct fb_var_screeninfo *) asPtr(p);
    screen->red.length = length;
    screen->red.offset = offset;
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_00024FbVarScreenInfo_setGreen
  (JNIEnv *UNUSED(env), jobject UNUSED(obj), jlong p, int length, int offset) {
    struct fb_var_screeninfo *screen = (struct fb_var_screeninfo *) asPtr(p);
    screen->green.length = length;
    screen->green.offset = offset;
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_00024FbVarScreenInfo_setBlue
  (JNIEnv *UNUSED(env), jobject UNUSED(obj), jlong p, int length, int offset) {
    struct fb_var_screeninfo *screen = (struct fb_var_screeninfo *) asPtr(p);
    screen->blue.length = length;
    screen->blue.offset = offset;
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_00024FbVarScreenInfo_setTransp
  (JNIEnv *UNUSED(env), jobject UNUSED(obj), jlong p, int length, int offset) {
    struct fb_var_screeninfo *screen = (struct fb_var_screeninfo *) asPtr(p);
    screen->transp.length = length;
    screen->transp.offset = offset;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_00024InputAbsInfo_sizeof
  (JNIEnv *UNUSED(env), jobject UNUSED(obj)) {
    return (jint) sizeof(struct input_absinfo);
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_00024InputAbsInfo_getValue
  (JNIEnv *UNUSED(env), jclass UNUSED(cls), jlong p) {
    return (jint) ((struct input_absinfo *) asPtr(p))->value;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_00024InputAbsInfo_getMinimum
  (JNIEnv *UNUSED(env), jclass UNUSED(cls), jlong p) {
    return (jint) ((struct input_absinfo *) asPtr(p))->minimum;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_00024InputAbsInfo_getMaximum
  (JNIEnv *UNUSED(env), jclass UNUSED(cls), jlong p) {
    return (jint) ((struct input_absinfo *) asPtr(p))->maximum;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_00024InputAbsInfo_getFuzz
  (JNIEnv *UNUSED(env), jclass UNUSED(cls), jlong p) {
    return (jint) ((struct input_absinfo *) asPtr(p))->fuzz;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_00024InputAbsInfo_getFlat
  (JNIEnv *UNUSED(env), jclass UNUSED(cls), jlong p) {
    return (jint) ((struct input_absinfo *) asPtr(p))->flat;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_linux_LinuxSystem_00024InputAbsInfo_getResolution
  (JNIEnv *UNUSED(env), jclass UNUSED(cls), jlong p) {
    return (jint) ((struct input_absinfo *) asPtr(p))->resolution;
}
