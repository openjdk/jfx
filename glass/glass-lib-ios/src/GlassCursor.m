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

#include "GlassMacros.h"

/* Inaccessible static: type2CursorMap */
/*
 * Class:     com_sun_glass_ui_ios_IosCursor
 * Method:    _createCursor
 * Signature: (IILjava/nio/ByteBuffer;)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_ios_IosCursor__1createCursor
(JNIEnv *env, jclass jcursor, jint width, jint height, jobject pixels) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosCursor__1createCursor() called.");
    // There is no cursor on iOS
    return 0;
}

/*
 * Class:     com_sun_glass_ui_ios_IosCursor
 * Method:    _set
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosCursor__1set
(JNIEnv *env, jclass jcursor, jint type) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosCursor__1set() called.");
    // There is no cursor on iOS
}

/*
 * Class:     com_sun_glass_ui_ios_IosCursor
 * Method:    _setCustom
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosCursor__1setCustom
(JNIEnv *env, jclass jcursor, jlong ptr) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosCursor__1setCustom() called.");
    // There is no cursor on iOS
}
