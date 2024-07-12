/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

#import "common.h"
#import "com_sun_glass_ui_View.h"
#import "com_sun_glass_ui_mac_MacView.h"
#import "com_sun_glass_ui_View_Capability.h"
#import "com_sun_glass_ui_Clipboard.h"
#import "com_sun_glass_events_ViewEvent.h"

#import "GlassMacros.h"
#import "GlassWindow.h"
#import "GlassView3D.h"
#import "GlassHelper.h"
#import "GlassLayer3D.h"

//#define VERBOSE
#ifndef VERBOSE
    #define LOG(MSG, ...)
#else
    #define LOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

static inline NSView<GlassView>* getGlassView(JNIEnv *env, jlong jPtr)
{
    assert(jPtr != 0L);

    return (NSView<GlassView>*)jlong_to_ptr(jPtr);
}

#pragma mark --- JNI

/*
 * Class:     com_sun_glass_ui_mac_MacView
 * Method:    _initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacView__1initIDs
(JNIEnv *env, jclass jClass)
{
    LOG("Java_com_sun_glass_ui_mac_MacView__1initIDs");

    if (jViewClass == NULL)
    {
        jViewClass = (*env)->NewGlobalRef(env, jClass);
    }

    if (jIntegerClass == NULL)
    {
        jclass jcls = (*env)->FindClass(env, "java/lang/Integer");
        if ((*env)->ExceptionCheck(env)) return;
        jIntegerClass = (*env)->NewGlobalRef(env, jcls);
    }

    if (jMapClass == NULL)
    {
        jclass jcls = (*env)->FindClass(env, "java/util/Map");
        if ((*env)->ExceptionCheck(env)) return;
        jMapClass = (*env)->NewGlobalRef(env, jcls);
    }

    if (jBooleanClass == NULL)
    {
        jclass jcls = (*env)->FindClass(env, "java/lang/Boolean");
        if ((*env)->ExceptionCheck(env)) return;
        jBooleanClass = (*env)->NewGlobalRef(env, jcls);
    }

    if (jViewNotifyEvent == NULL)
    {
        jViewNotifyEvent = (*env)->GetMethodID(env, jViewClass, "notifyView", "(I)V");
        if ((*env)->ExceptionCheck(env)) return;
    }

    if (jViewNotifyRepaint == NULL)
    {
        jViewNotifyRepaint = (*env)->GetMethodID(env, jViewClass, "notifyRepaint", "(IIII)V");
        if ((*env)->ExceptionCheck(env)) return;
    }

    if (jViewNotifyResize == NULL)
    {
        jViewNotifyResize = (*env)->GetMethodID(env, jViewClass, "notifyResize", "(II)V");
        if ((*env)->ExceptionCheck(env)) return;
    }

    if (jViewNotifyKey == NULL)
    {
        jViewNotifyKey = (*env)->GetMethodID(env, jViewClass, "notifyKey", "(II[CI)V");
        if ((*env)->ExceptionCheck(env)) return;
    }

    if (jViewNotifyMenu == NULL)
    {
        jViewNotifyMenu = (*env)->GetMethodID(env, jViewClass, "notifyMenu", "(IIIIZ)V");
        if ((*env)->ExceptionCheck(env)) return;
    }

    if (jViewNotifyMouse == NULL)
    {
        jViewNotifyMouse = (*env)->GetMethodID(env, jViewClass, "notifyMouse", "(IIIIIIIZZ)V");
        if ((*env)->ExceptionCheck(env)) return;
    }

    if (jViewNotifyInputMethod == NULL)
    {
        jViewNotifyInputMethod = (*env)->GetMethodID(env, jViewClass, "notifyInputMethod", "(Ljava/lang/String;[I[I[BIII)V");
        if ((*env)->ExceptionCheck(env)) return;
    }

    if (jViewNotifyInputMethodMac == NULL)
    {
        jclass jMacViewClass = [GlassHelper ClassForName:"com.sun.glass.ui.mac.MacView" withEnv:env];
        if (!jMacViewClass) return;
        jViewNotifyInputMethodMac = (*env)->GetMethodID(env, jMacViewClass, "notifyInputMethodMac", "(Ljava/lang/String;IIIII)V");
        if ((*env)->ExceptionCheck(env)) return;
    }

    if(jViewNotifyInputMethodCandidatePosRequest == NULL)
    {
        jViewNotifyInputMethodCandidatePosRequest = (*env)->GetMethodID(env, jViewClass, "notifyInputMethodCandidatePosRequest", "(I)[D");
        if ((*env)->ExceptionCheck(env)) return;
    }

    if (jViewNotifyDragEnter == NULL)
    {
        jViewNotifyDragEnter = (*env)->GetMethodID(env, jViewClass, "notifyDragEnter", "(IIIII)I");
        if ((*env)->ExceptionCheck(env)) return;
    }

    if (jViewNotifyDragOver == NULL)
    {
        jViewNotifyDragOver = (*env)->GetMethodID(env, jViewClass, "notifyDragOver", "(IIIII)I");
        if ((*env)->ExceptionCheck(env)) return;
    }

    if (jViewNotifyDragLeave == NULL)
    {
        jViewNotifyDragLeave = (*env)->GetMethodID(env, jViewClass, "notifyDragLeave", "()V");
        if ((*env)->ExceptionCheck(env)) return;
    }

    if (jViewNotifyDragDrop == NULL)
    {
        jViewNotifyDragDrop = (*env)->GetMethodID(env, jViewClass, "notifyDragDrop", "(IIIII)I");
        if ((*env)->ExceptionCheck(env)) return;
    }

    if (jViewNotifyDragEnd == NULL)
    {
        jViewNotifyDragEnd = (*env)->GetMethodID(env, jViewClass, "notifyDragEnd", "(I)V");
        if ((*env)->ExceptionCheck(env)) return;
    }

    if (jViewGetAccessible == NULL)
    {
        jViewGetAccessible = (*env)->GetMethodID(env, jViewClass, "getAccessible", "()J");
        if ((*env)->ExceptionCheck(env)) return;
    }

    if (jMapGetMethod == NULL)
    {
        jMapGetMethod = (*env)->GetMethodID(env, jMapClass, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
        if ((*env)->ExceptionCheck(env)) return;
    }

    if (jBooleanValueMethod == NULL)
    {
        jBooleanValueMethod = (*env)->GetMethodID(env, jBooleanClass, "booleanValue", "()Z");
        if ((*env)->ExceptionCheck(env)) return;
    }

    if (jIntegerInitMethod == NULL)
    {
        jIntegerInitMethod = (*env)->GetMethodID(env, jIntegerClass, "<init>", "(I)V");
        if ((*env)->ExceptionCheck(env)) return;
    }

    if (jIntegerValueMethod == NULL)
    {
        jIntegerValueMethod = (*env)->GetMethodID(env, jIntegerClass, "intValue", "()I");
        if ((*env)->ExceptionCheck(env)) return;
    }

    if (jLongClass == NULL)
    {
        jclass jcls = (*env)->FindClass(env, "java/lang/Long");
        if ((*env)->ExceptionCheck(env)) return;
        jLongClass = (*env)->NewGlobalRef(env, jcls);
    }

    if (jLongValueMethod == NULL)
    {
        jLongValueMethod = (*env)->GetMethodID(env, jLongClass, "longValue", "()J");
    }
}

/*
 * Class:     com_sun_glass_ui_mac_MacView
 * Method:    _getMultiClickTime_impl
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_mac_MacView__1getMultiClickTime_1impl
(JNIEnv *env, jclass cls)
{
    LOG("Java_com_sun_glass_ui_mac_MacView__1getMultiClickTime_1impl");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);

    // 10.6 API
    return (jlong)([NSEvent doubleClickInterval]*1000.0f);
}

/*
 * Class:     com_sun_glass_ui_mac_MacView
 * Method:    _getMultiClickMaxX_impl
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_mac_MacView__1getMultiClickMaxX_1impl
(JNIEnv *env, jclass cls)
{
    LOG("Java_com_sun_glass_ui_mac_MacView__1getMultiClickMaxX_1impl");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);

    // gznote: there is no way to get this value out of the system
    // Most of the Mac machines use the value 3, so we hardcode this value
    return (jint)3;
}

/*
 * Class:     com_sun_glass_ui_mac_MacView
 * Method:    _getMultiClickMaxY_impl
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_mac_MacView__1getMultiClickMaxY_1impl
(JNIEnv *env, jclass cls)
{
    LOG("Java_com_sun_glass_ui_mac_MacView__1getMultiClickMaxY_1impl");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);

    // gznote: there is no way to get this value out of the system
    // Most of the Mac machines use the value 3, so we hardcode this value
    return (jint)3;
}

/*
 * Class:     com_sun_glass_ui_mac_MacView
 * Method:    _create
 * Signature: (Ljava/util/Map;)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_mac_MacView__1create
(JNIEnv *env, jobject jView, jobject jCapabilities)
{
    LOG("Java_com_sun_glass_ui_mac_MacView__1create");

    jlong value = 0L;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        jobject jViewRef = (*env)->NewGlobalRef(env, jView);
        jobject jCapabilitiesRef = NULL;
        if (jCapabilities != NULL)
        {
            jCapabilitiesRef = (*env)->NewGlobalRef(env, jCapabilities);
        }

        // embed ourselves into GlassHostView, so we can later swap our view between windows (ex. fullscreen mode)
        NSView *hostView = [[GlassHostView alloc] initWithFrame:NSMakeRect(0, 0, 0, 0)]; // alloc creates ref count of 1
        [hostView setAutoresizingMask:(NSViewWidthSizable|NSViewHeightSizable)];
        [hostView setAutoresizesSubviews:YES];

        NSView* view = [[GlassView3D alloc] initWithFrame:[hostView bounds] withJview:jView withJproperties:jCapabilities];
        [view setAutoresizingMask:(NSViewWidthSizable|NSViewHeightSizable)];

        [hostView addSubview:view];
        jfieldID jfID = (*env)->GetFieldID(env, jViewClass, "ptr", "J");
        GLASS_CHECK_EXCEPTION(env);
        (*env)->SetLongField(env, jView, jfID, ptr_to_jlong(view));

        value = ptr_to_jlong(view);

        if (jCapabilities != NULL)
        {
            (*env)->DeleteGlobalRef(env, jCapabilitiesRef);
        }
        (*env)->DeleteGlobalRef(env, jViewRef);
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    LOG("   view: %p", value);
    return value;
}

/*
 * Class:     com_sun_glass_ui_mac_MacView
 * Method:    _getNativeFrameBuffer
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_mac_MacView__1getNativeFrameBuffer
(JNIEnv *env, jobject jView, jlong jPtr)
{
    LOG("Java_com_sun_glass_ui_mac_MacView__1_getNativeFrameBuffer");
    LOG("   view: %p", jPtr);
    if (!jPtr) return 0L;

    jint fb = 0;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        NSView<GlassView> *view = getGlassView(env, jPtr);
        GlassLayer3D *layer = (GlassLayer3D*)[view layer];
        fb = (jint) [[layer getPainterOffscreen] fbo];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return fb;
}

/*
 * Class:     com_sun_glass_ui_mac_MacView
 * Method:    _getNativeLayer
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_mac_MacView__1getNativeLayer
(JNIEnv *env, jobject jView, jlong jPtr)
{
    LOG("Java_com_sun_glass_ui_mac_MacView__1_getNativeLayer");
    LOG("   view: %p", jPtr);
    if (!jPtr) return 0L;

    jlong ptr = 0L;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        NSView<GlassView> *view = getGlassView(env, jPtr);
        ptr = ptr_to_jlong([view layer]);
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return ptr;
}

/*
 * Class:     com_sun_glass_ui_mac_MacView
 * Method:    _getX
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_mac_MacView__1getX
(JNIEnv *env, jobject jView, jlong jPtr)
{
    LOG("Java_com_sun_glass_ui_mac_MacView__1getX");
    if (!jPtr) return 0;

    jint x = 0;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        NSView<GlassView> *view = getGlassView(env, jPtr);
        NSWindow *window = [view window];
        if (window != nil)
        {
            NSRect frame = [window frame];
            NSRect contentRect = [window contentRectForFrameRect:frame];
            x = (jint)(contentRect.origin.x - frame.origin.x);
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return x;
}

/*
 * Class:     com_sun_glass_ui_mac_MacView
 * Method:    _getY
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_mac_MacView__1getY
(JNIEnv *env, jobject jView, jlong jPtr)
{
    LOG("Java_com_sun_glass_ui_mac_MacView__1getY");
    if (!jPtr) return 0;

    jint y = 0;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        NSView<GlassView> *view = getGlassView(env, jPtr);
        NSWindow * window = [view window];
        if (window != nil)
        {
            NSRect frame = [window frame];
            NSRect contentRect = [window contentRectForFrameRect:frame];

            // Assume that the border in the bottom is zero-sized
            y = (jint)(frame.size.height - contentRect.size.height);
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return y;
}

/*
 * Class:     com_sun_glass_ui_mac_MacView
 * Method:    _setParent
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacView__1setParent
(JNIEnv *env, jobject jView, jlong jPtr, jlong parentPtr)
{
    LOG("Java_com_sun_glass_ui_mac_MacView__1setParent");
    LOG("   view: %p", jPtr);
    LOG("   parent: %p", parentPtr);
    if (!jPtr) return;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    // TODO: Java_com_sun_glass_ui_mac_MacView__1setParent
}

/*
 * Class:     com_sun_glass_ui_mac_MacView
 * Method:    _close
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_mac_MacView__1close
(JNIEnv *env, jobject jView, jlong jPtr)
{
    LOG("Java_com_sun_glass_ui_mac_MacView__1close");
    if (!jPtr) return JNI_FALSE;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        NSView<GlassView> *view = getGlassView(env, jPtr);
        NSView * host = [view superview];
        if (host != nil) {
            [view removeFromSuperview];
            [host release];
        }
        [view release];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return JNI_TRUE;
}

/*
 * Class:     com_sun_glass_ui_mac_MacView
 * Method:    _begin
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacView__1begin
(JNIEnv *env, jobject jView, jlong jPtr)
{
    LOG("Java_com_sun_glass_ui_mac_MacView__1begin");
    if (!jPtr) return;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    NSView<GlassView> *view = getGlassView(env, jPtr);
    GLASS_POOL_PUSH; // it will be popped by "_end"
    {
        [view retain];
//        [view lockFocus];
        [view begin];
    }
}

/*
 * Class:     com_sun_glass_ui_mac_MacView
 * Method:    _end
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacView__1end
(JNIEnv *env, jobject jView, jlong jPtr)
{
    LOG("Java_com_sun_glass_ui_mac_MacView__1end");
    if (!jPtr) return;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    NSView<GlassView> *view = getGlassView(env, jPtr);
    {
        [view end];
//        [view unlockFocus];
        [view release];
    }
    GLASS_POOL_POP; // it was pushed by "_begin"
}

/*
 * Class:     com_sun_glass_ui_mac_MacView
 * Method:    _scheduleRepaint
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacView__1scheduleRepaint
(JNIEnv *env, jobject jView, jlong jPtr)
{
    LOG("Java_com_sun_glass_ui_mac_MacView__1scheduleRepaint");
    if (!jPtr) return;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        NSView<GlassView> *view = getGlassView(env, jPtr);
        [view setNeedsDisplay:YES];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_mac_MacView
 * Method:    _enterFullscreen
 * Signature: (ZZZ)V
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_mac_MacView__1enterFullscreen
(JNIEnv *env, jobject jView, jlong jPtr, jboolean jAnimate, jboolean jKeepRatio, jboolean jHideCursor)
{
    LOG("Java_com_sun_glass_ui_mac_MacView__1enterFullscreen");
    if (!jPtr) return JNI_FALSE;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        NSView<GlassView> *view = getGlassView(env, jPtr);
        [view enterFullscreenWithAnimate:(jAnimate==JNI_TRUE) withKeepRatio:(jKeepRatio==JNI_TRUE) withHideCursor:(jHideCursor==JNI_TRUE)];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return JNI_TRUE; // gznote: remove this return value
}

/*
 * Class:     com_sun_glass_ui_mac_MacView
 * Method:    _exitFullscreen
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacView__1exitFullscreen
(JNIEnv *env, jobject jView, jlong jPtr, jboolean jAnimate)
{
    LOG("Java_com_sun_glass_ui_mac_MacView__1exitFullscreen");
    if (!jPtr) return;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        NSView<GlassView> *view = getGlassView(env, jPtr);
        [view exitFullscreenWithAnimate:(jAnimate==JNI_TRUE)];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_mac_MacView
 * Method:    _uploadPixelsDirect
 * Signature: (JLjava/nio/Buffer;II)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacView__1uploadPixelsDirect
(JNIEnv *env, jobject jView, jlong jPtr, jobject jBuffer, jint jWidth, jint jHeight, jfloat jScaleX, jfloat jScaleY)
{
    LOG("Java_com_sun_glass_ui_mac_MacView__1uploadPixelsDirect");
    if (!jPtr) return;
    if (!jBuffer) return;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    NSView<GlassView> *view = getGlassView(env, jPtr);

    void *pixels = (*env)->GetDirectBufferAddress(env, jBuffer);

    // must be in the middle of begin/end
    if ((jWidth > 0) && (jHeight > 0))
    {
        [view pushPixels:pixels withWidth:(GLuint)jWidth withHeight:(GLuint)jHeight withScaleX:(GLfloat)jScaleX withScaleY:(GLfloat)jScaleY withEnv:env];
    }
}

/*
 * Class:     com_sun_glass_ui_mac_MacView
 * Method:    _uploadPixelsByteArray
 * Signature: (J[BIII)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacView__1uploadPixelsByteArray
(JNIEnv *env, jobject jView, jlong jPtr, jbyteArray jArray, jint jOffset, jint jWidth, jint jHeight, jfloat jScaleX, jfloat jScaleY)
{
    LOG("Java_com_sun_glass_ui_mac_MacView__1uploadPixelsByteArray");
    if (!jPtr) return;
    if (!jArray) return;
    if (jOffset < 0) return;
    if (jWidth <= 0 || jHeight <= 0) return;

    if (jWidth > (((INT_MAX - jOffset) / 4) / jHeight))
    {
        return;
    }

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);

    if ((4 * jWidth * jHeight + jOffset) > (*env)->GetArrayLength(env, jArray))
    {
        return;
    }

    jboolean isCopy = JNI_FALSE;
    u_int8_t *data = (*env)->GetPrimitiveArrayCritical(env, jArray, &isCopy);
    {
        NSView<GlassView> *view = getGlassView(env, jPtr);

        void *pixels = (data+jOffset);

        // must be in the middle of begin/end
        [view pushPixels:pixels withWidth:(GLuint)jWidth withHeight:(GLuint)jHeight withScaleX:(GLfloat)jScaleX withScaleY:(GLfloat)jScaleY withEnv:env];
    }
    (*env)->ReleasePrimitiveArrayCritical(env, jArray, data, JNI_ABORT);
}

/*
 * Class:     com_sun_glass_ui_mac_MacView
 * Method:    _uploadPixelsIntArray
 * Signature: (J[IIII)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacView__1uploadPixelsIntArray
(JNIEnv *env, jobject jView, jlong jPtr, jintArray jArray, jint jOffset, jint jWidth, jint jHeight, jfloat jScaleX, jfloat jScaleY)
{
    LOG("Java_com_sun_glass_ui_mac_MacView__1uploadPixelsIntArray");
    if (!jPtr) return;
    if (!jArray) return;
    if (jOffset < 0) return;
    if (jWidth <= 0 || jHeight <= 0) return;

    if (jWidth > ((INT_MAX - jOffset) / jHeight))
    {
        return;
    }

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);

    if ((jWidth * jHeight + jOffset) > (*env)->GetArrayLength(env, jArray))
    {
        return;
    }

    jboolean isCopy = JNI_FALSE;
    u_int32_t *data = (*env)->GetPrimitiveArrayCritical(env, jArray, &isCopy);
    {
        NSView<GlassView> *view = getGlassView(env, jPtr);

        void *pixels = (data+jOffset);

        // must be in the middle of begin/end
        [view pushPixels:pixels withWidth:(GLuint)jWidth withHeight:(GLuint)jHeight withScaleX:(GLfloat)jScaleX withScaleY:(GLfloat)jScaleY withEnv:env];
    }
    (*env)->ReleasePrimitiveArrayCritical(env, jArray, data, JNI_ABORT);
}

/*
 * Input methods callback
 */

/*
 * Class:     com_sun_glass_ui_mac_MacView
 * Method:    _enableInputMethodEvents
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacView__1enableInputMethodEvents
(JNIEnv *env, jobject jView, jlong ptr, jboolean enable)
{
    LOG("Java_com_sun_glass_ui_mac_MacView__1enableInputMethodEvents");
    if (!ptr) return;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        NSView<GlassView> *view = getGlassView(env, ptr);
        [view setInputMethodEnabled:(enable==JNI_TRUE)];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_mac_MacView
 * Method:    _finishInputMethodComposition
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacView__1finishInputMethodComposition
(JNIEnv *env, jobject jView, jlong ptr)
{
    LOG("Java_com_sun_glass_ui_mac_MacView__1finishInputMethodComposition");
    if (!ptr) return;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        NSView<GlassView> *view = getGlassView(env, ptr);
        [view finishInputMethodComposition];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}
