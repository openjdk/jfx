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

#include "GlassView.h"
#include "GlassViewGL.h"


static inline UIView<GlassView>* getGlassView(JNIEnv *env, jlong jPtr)
{
    if (jPtr != 0L)
    {
        return (UIView<GlassView>*)jlong_to_ptr(jPtr);
    }
    else
    {
        return nil;
    }
}

// to be called on main thread
jlong Do_com_sun_glass_ui_ios_IosView__1create(JNIEnv *env, jobject jView, jobject jCapabilities)
{
    UIView<GlassView> *view;
    
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    
    {
        view = [[GlassViewGL alloc] initWithFrame:CGRectMake(0,0,0,0) withJview:jView withJproperties:jCapabilities];
        (*env)->SetLongField(env, jView, (*env)->GetFieldID(env, mat_jViewClass, "nativePtr", "J"), ptr_to_jlong(view));
    }
    [pool drain];
    
    GLASS_CHECK_EXCEPTION(env);
    
    return ptr_to_jlong(view);
}

// to be called on main thread
void Do_com_sun_glass_ui_ios_IosView__1close(JNIEnv *env, jlong jPtr)
{
    UIView<GlassView> *view = getGlassView(env, jPtr);
    if (view != nil) {
        [view removeFromSuperview];
        [view release];
    }
}


@interface GlassViewDispatcher : NSObject
{
@public
    jobject jView;
    jobject jCapabilities;
    jlong   jPtr;
    jlong   jlongReturn;
}
@end



@implementation GlassViewDispatcher

- (void) _1create
{
    self->jlongReturn = Do_com_sun_glass_ui_ios_IosView__1create(jEnv, self->jView, self->jCapabilities);
}


- (void)Do_com_sun_glass_ui_ios_IosView__1close
{
    GET_MAIN_JENV;
    Do_com_sun_glass_ui_ios_IosView__1close(env, self->jPtr);
}

@end



/*
 * Class:     com_sun_glass_ui_View
 * Method:    _create
 * Signature: (Ljava/util/Map;)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_ios_IosView__1create
(JNIEnv *env, jobject jview, jobject jCapabilities) {
    
    jlong value;
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GLASS_LOG("Java_com_sun_glass_ui_ios_IosView__1create");
        jobject jViewRef = (*env)->NewGlobalRef(env, jview);
        jobject jCapabilitiesRef = (*env)->NewGlobalRef(env, jCapabilities);
        {
            if ([[NSThread currentThread] isMainThread] == YES)
            {
                // Create GlassViewGL
                return Do_com_sun_glass_ui_ios_IosView__1create(env, jview, jCapabilities);
            }
            else
            {
                GlassViewDispatcher *dispatcher = [[GlassViewDispatcher alloc] autorelease];
                dispatcher->jView = jViewRef;
                dispatcher->jCapabilities = jCapabilitiesRef;
                // Create GlassViewGL on main thread
                [dispatcher performSelectorOnMainThread:@selector(_1create) withObject:dispatcher waitUntilDone:YES]; // block and wait for the return value
                value = dispatcher->jlongReturn;
            }
        }
        (*env)->DeleteGlobalRef(env, jCapabilitiesRef);
        (*env)->DeleteGlobalRef(env, jViewRef);
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
    return value;
}


/*
 * Class:     com_sun_glass_ui_ios_IosView
 * Method:    _getNativeView
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_ios_IosView__1getNativeView
(JNIEnv *env, jobject jview, jlong ptr) {
    // On iOS the native jPtr is the UIView* instance itself
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosView__1getNativeView - returns ptr");
    return ptr;
}


/*
 * Class:     com_sun_glass_ui_ios_IosView
 * Method:    _getX
 * Signature: (J)I
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_ios_IosView_getFrameBufferImpl
(JNIEnv *env, jobject jview, jlong ptr) {
    
    jlong framebuffer = 0;
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        // get FBO associated with given GlassViewGL
        UIView<GlassView> *view = getGlassView(env, ptr);
        if (view) {
            framebuffer = ((GlassViewGL*)view)->frameBuffer;
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
    
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosView_getFrameBufferImpl() returns %lld",framebuffer);
    
    return framebuffer;
}


/*
 * Class:     com_sun_glass_ui_ios_IosView
 * Method:    _getX
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_ios_IosView__1getX
(JNIEnv *env, jobject jview, jlong ptr) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosView__1getX - returns 0");
        
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_CHECK_EXCEPTION(env);
    
    return 0; //on iOS Windows content rect equals it's frame, thus View's origin equals Window's origin
}


/*
 * Class:     com_sun_glass_ui_ios_IosView
 * Method:    _getY
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_ios_IosView__1getY
(JNIEnv *env, jobject jview, jlong ptr) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosView__1getY - returning 0");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_CHECK_EXCEPTION(env);
    
    return 0; //on iOS Windows content rect equals it's frame, thus View's origin equals Window's origin
}


/*
 * Class:     com_sun_glass_ui_ios_IosView
 * Method:    _close
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_ios_IosView__1close
(JNIEnv *env, jclass jview, jlong ptr) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosView__1close called.");
   
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        if ([[NSThread currentThread] isMainThread] == YES)
        {
            Do_com_sun_glass_ui_ios_IosView__1close(env, ptr);
        }
        else
        {
            GlassViewDispatcher *dispatcher = [[GlassViewDispatcher alloc] autorelease];
            dispatcher->jPtr = ptr;
            [dispatcher performSelectorOnMainThread:@selector(Do_com_sun_glass_ui_ios_IosView__1close) withObject:dispatcher waitUntilDone:YES];
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
    
    return JNI_TRUE;
}


/*
 * Class:     com_sun_glass_ui_ios_IosView
 * Method:    _begin
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosView__1begin
(JNIEnv *env, jobject jview, jlong ptr) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosView__1begin");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    UIView<GlassView> *view = getGlassView(env, ptr);
    GLASS_POOL_PUSH; // it will be popped by "_end"
    {
        // Prepare GlassView for redraw
        [view retain];
        [view begin];
    }
}


/*
 * Class:     com_sun_glass_ui_ios_IosView
 * Method:    _end
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosView__1end
(JNIEnv *env, jobject jview, jlong ptr) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosView__1end");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    UIView<GlassView> *view = getGlassView(env, ptr);
    {
        [view end];
        [view release];
        // GlassView was redrawn
    }
    GLASS_POOL_POP;
}


/*
 * Class:     com_sun_glass_ui_ios_IosView
 * Method:    _scheduleRepaint
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosView__1scheduleRepaint
(JNIEnv *env, jobject jview, jlong ptr) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosView__1scheduleRepaint");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        UIView<GlassView> *view = getGlassView(env, ptr);
        [view setNeedsDisplay];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}


/*
 * Class:     com_sun_glass_ui_ios_IosView
 * Method:    _setParent
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosView__1setParent
(JNIEnv *env, jobject jView, jlong jPtr, jlong parentPtr)
{
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosView__1setParent. Code template");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
}


/*
 * Class:     com_sun_glass_ui_ios_IosView
 * Method:    _enterFullscreen
 * Signature: (ZZZ)V
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_ios_IosView__1enterFullscreen
(JNIEnv *env, jobject jView, jlong jPtr, jboolean jAnimate, jboolean jKeepRatio, jboolean jHideCursor)
{
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosView__1enterFullscreen. Code template.");
    
    return JNI_FALSE;
}


/*
 * Class:     com_sun_glass_ui_ios_IosView
 * Method:    _exitFullscreen
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosView__1exitFullscreen
(JNIEnv *env, jobject jView, jlong jPtr, jboolean jAnimate)
{
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosView__1exitFullscreen. Code template.");
}
