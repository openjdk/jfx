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
#ifndef ANDROIDINPUT_H
#define	ANDROIDINPUT_H

#include <dlfcn.h>
#include <sys/types.h>
#include <android/native_window.h>
#include "input/LensInput.h"
#include <android/keycodes.h>
#include <linux/input.h>

#ifdef	__cplusplus
extern "C" {
#endif

#define TRUE  1
#define FALSE 0

#define THROW_RUNTIME_EXCEPTION(ENV, MESSAGE, ...)                          \
    char error_msg[256];                                                    \
    sprintf(error_msg, MESSAGE, __VA_ARGS__);                               \
    (*ENV)->ThrowNew(ENV,                                                   \
        (*ENV)->FindClass(ENV, "java/lang/RuntimeException"), error_msg);

#ifdef DEBUG
    // This method is good for early debug, but is unneeded for general use

    static void *get_check_symbol(JNIEnv *env, void *handle, const char *name) {
        void *ret = dlsym(handle, name);
        if (!ret) {
            THROW_RUNTIME_EXCEPTION(env, "Failed to load symbol %s", name);
        }
        return ret;
    }
#define GET_SYMBOL(env, handle,name) get_check_symbol(env, handle,name)

#else // #ifdef DEBUG

#define GET_SYMBOL(env, handle,name) dlsym(handle,name)

#endif

ANativeWindow *getAndroidNativeWindow();

    
#ifdef	__cplusplus
}
#endif

#endif	/* ANDROIDINPUT_H */