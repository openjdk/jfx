/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

#include <jni.h>

/*
 * Class com_sun_javafx_embed_swing_newimpl_SwingNodeInteropN
 * Method: overrideNativeWindowHandle
 * Signature (Ljava/lang/Class;JLjava/lang/Runnable)Ljdk.swing.interop.LightweightFrameWrapper;
 */
JNIEXPORT void JNICALL
Java_com_sun_javafx_embed_swing_newimpl_SwingNodeInteropN_overrideNativeWindowHandle(
 JNIEnv *env, jclass cls, jclass lwFrameClass, jobject lwFrame, jlong id, jobject runnable) {

    jmethodID cons;
    if (lwFrameClass == NULL) {
        return;
    }
    cons = (*env)->GetMethodID(env, lwFrameClass, "overrideNativeWindowHandle",
                                         "(JLjava/lang/Runnable;)V");
    if (cons == NULL || (*env)->ExceptionCheck(env)) {
        return;
    }
    (*env)->CallVoidMethod(env, lwFrame, cons, id, runnable);
}


