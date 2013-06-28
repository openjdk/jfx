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
#include "com_sun_glass_ui_lens_LensCursor.h"
#include "LensCursorImages.h"

/*
 * Class:     com_sun_glass_ui_lens_LensCursor
 * Method:    _setNativeCursor
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_lens_LensCursor__1setNativeCursor
(JNIEnv *env, jobject jCursor, jlong nativeCursorPointer) {
    glass_cursor_setNativeCursor(nativeCursorPointer);
}

/*
 * Class:     com_sun_glass_ui_lens_LensCursor
 * Method:    _releaseNativeCursor
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_lens_LensCursor__1releaseNativeCursor
(JNIEnv *env, jobject jCursor, jlong nativeCursorPointer) {
    glass_cursor_releaseNativeCursor(nativeCursorPointer);
}

/*
 * Class:     com_sun_glass_ui_lens_LensCursor
 * Method:    _createNativeCursorByType
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_lens_LensCursor__1createNativeCursorByType
(JNIEnv *env, jobject jCursor, jint type) {

    int width, height;
    jbyte *img = lensCursorsGetCursor(type, &width, &height,
                                      glass_cursor_supportsTranslucency());

    return glass_cursor_createNativeCursor(env, 0, 0, img, width, height);
}


/*
 * Class:     com_sun_glass_ui_lens_LensCursor
 * Method:    _createNativeCursorInts
 * Signature: (II[III)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_lens_LensCursor__1createNativeCursorInts
(JNIEnv *env, jobject jCursor, jint x, jint y,  jintArray srcArray, jint width, jint height) {

    jbyte *src = (*env)->GetPrimitiveArrayCritical(env, srcArray, 0);


    jlong res =  glass_cursor_createNativeCursor(env, x, y, src, width, height);


    (*env)->ReleasePrimitiveArrayCritical(env, srcArray, src, JNI_ABORT);

    return res;
}


/*
 * Class:     com_sun_glass_ui_lens_LensCursor
 * Method:    _createNativeCursorBytes
 * Signature: (II[BII)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_lens_LensCursor__1createNativeCursorBytes
(JNIEnv *env, jobject jCursor, jint x, jint y,  jbyteArray srcArray, jint width, jint height) {
    glass_throw_exception_by_name(env, glass_RuntimeException, "Unimplemented");
    return 0;
}


/*
 * Class:     com_sun_glass_ui_lens_LensCursor
 * Method:    _createNativeCursorDirect
 * Signature: (IILjava/nio/Buffer;III)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_lens_LensCursor__1createNativeCursorDirect
(JNIEnv *env, jobject jCursor, jint x, jint y,  jobject srcArray, jint capacity, jint width, jint height) {
    glass_throw_exception_by_name(env, glass_RuntimeException, "Unimplemented");
    return 0;
}


/*
 * Class:     com_sun_glass_ui_lens_LensCursor
 * Method:    _setVisible
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_lens_LensCursor__1setVisible
(JNIEnv *env, jclass cursurClass, jboolean isVisible) {
    glass_cursor_setVisible(isVisible);
}




/*
* Destructor function to clean the cursor resources.
* The execution time of this function must be short since it is called
* when the application is exiting.
*/
int lensCursorDestructor(void) __attribute__((destructor));

int lensCursorDestructor(void) {
    glass_cursor_terminate();

    return 0;
}
