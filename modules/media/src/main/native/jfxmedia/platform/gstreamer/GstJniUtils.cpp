/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

#include "GstJniUtils.h"

#include <glib.h>

extern "C" JavaVM* g_pJVM;
static GStaticPrivate g_Private = G_STATIC_PRIVATE_INIT;

void DetachThread(gpointer data)
{
    void *env = NULL;
    if (g_pJVM && g_pJVM->GetEnv(&env, JNI_VERSION_1_2) != JNI_EDETACHED)
        g_pJVM->DetachCurrentThread();
}

jboolean GstGetEnv(JNIEnv **env)
{
    if (g_pJVM->GetEnv((void**)env, JNI_VERSION_1_2) == JNI_OK)
        return true;
    else
    {
        JNIEnv *private_env = (JNIEnv*)g_static_private_get(&g_Private);
        if (!private_env)
        {
            if (g_pJVM->AttachCurrentThreadAsDaemon((void**)&private_env, NULL) == 0)
                g_static_private_set(&g_Private, private_env, DetachThread);
            else
                return false;
        }
        *env = private_env;
    }

    return true;
}
