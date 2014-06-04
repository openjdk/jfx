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
#ifndef EVENTLOOP_H
#define EVENTLOOP_H

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>
#include <sys/types.h>
#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include "Events.h"    
        
#define TRUE    1
#define FALSE   0

typedef struct {
    JavaVM          *jvm;
    size_t          size;
    int             running;
    pthread_t       thread;
    pthread_mutex_t mtx;
    pthread_cond_t  cv;
    Event           head;
    Event           tail;
    int     (*start)(JNIEnv *);
    void    (*stop)();
    void    *(*loop)(void*);
    int     (*push)(Event);
    Event   (*pop)();
    void    (*process)(JNIEnv *, Event);
} _EventQ;    
typedef _EventQ* EventQ;

EventQ eventq_getInstance();

#ifdef __cplusplus
}
#endif

#endif /* EVENTLOOP_H */

