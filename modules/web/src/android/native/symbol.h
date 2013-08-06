/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
 */
#ifndef SYMBOL_H
#define	SYMBOL_H

#include "android_log.h"
#include <dlfcn.h>

#ifdef	__cplusplus
extern "C" {
#endif

#ifdef DEBUG
    // This method is good for early debug, but is unneeded for general use

    static void *get_check_symbol(JNIEnv *env, void *handle, const char *name) {
        void *ret = dlsym(handle, name);
        if (!ret) {
            char error_msg[256];
            sprintf(error_msg, "Failed to load symbol %s", name);
            (*env)->ThrowNew(env,
                    (*env)->FindClass(env, "java/lang/RuntimeException"), error_msg);
        }
        return ret;
    }
#define GET_SYMBOL(env, handle,name) get_check_symbol(env, handle,name)

#else // #ifdef DEBUG

#define GET_SYMBOL(env, handle,name) dlsym(handle,name)

#endif

#ifdef	__cplusplus
}
#endif

#endif	/* SYMBOL_H */

