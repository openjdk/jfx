/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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
#include <com_sun_glass_ui_gtk_GtkTimer.h>
#include "glass_general.h"

#include <glib.h>
#include <gdk/gdk.h>
#include <stdlib.h>

static gboolean call_runnable_in_timer
  (gpointer);

extern "C" {

/*
 * Class:     com_sun_glass_ui_gtk_GtkTimer
 * Method:    _start
 * Signature: (Ljava/lang/Runnable;I)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_gtk_GtkTimer__1start
  (JNIEnv * env, jobject obj, jobject runnable, jint period)
{
    (void)obj;

    RunnableContext* context = (RunnableContext*) malloc(sizeof(RunnableContext));
    context->runnable = env->NewGlobalRef(runnable);
    context->flag = 0;
    gdk_threads_add_timeout_full(G_PRIORITY_HIGH_IDLE, period, call_runnable_in_timer, context, NULL);
    return PTR_TO_JLONG(context);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkTimer
 * Method:    _stop
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkTimer__1stop
  (JNIEnv * env, jobject obj, jlong ptr)
{
    (void)obj;

    RunnableContext* context = (RunnableContext*) JLONG_TO_PTR(ptr);
    context->flag = 1;
    env->DeleteGlobalRef(context->runnable);
    context->runnable = NULL;
}

} // extern "C"


static gboolean call_runnable_in_timer
  (gpointer data)
{
    RunnableContext* context = (RunnableContext*) data;
    if (context->flag) {
        free(context);
        return FALSE;
    }
    else if (context->runnable) {
        JNIEnv *env;
        int envStatus = javaVM->GetEnv((void **)&env, JNI_VERSION_1_6);
        if (envStatus == JNI_EDETACHED) {
            javaVM->AttachCurrentThread((void **)&env, NULL);
        }

        env->CallVoidMethod(context->runnable, jRunnableRun, NULL);
        LOG_EXCEPTION(env);

        if (envStatus == JNI_EDETACHED) {
            javaVM->DetachCurrentThread();
        }
    }
    return TRUE;
}

