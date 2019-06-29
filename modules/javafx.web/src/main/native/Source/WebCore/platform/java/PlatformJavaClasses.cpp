/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

#pragma once

#include "config.h"

#include "PlatformJavaClasses.h"
#include <wtf/java/JavaRef.h>

namespace WebCore {

jclass PG_GetGraphicsManagerClass(JNIEnv* env)
{
    static JGClass graphicsManagerCls(
        env->FindClass("com/sun/webkit/graphics/WCGraphicsManager"));
    ASSERT(graphicsManagerCls);
    return graphicsManagerCls;
}

jclass PG_GetGraphicsContextClass(JNIEnv* env)
{
    static JGClass graphicsContextCls(
        env->FindClass("com/sun/webkit/graphics/WCGraphicsContext"));
    ASSERT(graphicsContextCls);
    return graphicsContextCls;
}

jclass PG_GetPathClass(JNIEnv* env)
{
    static JGClass pathCls(
        env->FindClass("com/sun/webkit/graphics/WCPath"));
    ASSERT(pathCls);
    return pathCls;
}

jclass PG_GetPathIteratorClass(JNIEnv* env)
{
    static JGClass pathIteratorCls(
        env->FindClass("com/sun/webkit/graphics/WCPathIterator"));
    ASSERT(pathIteratorCls);
    return pathIteratorCls;
}


jclass PG_GetImageClass(JNIEnv* env)
{
    static JGClass imageCls(
        env->FindClass("com/sun/webkit/graphics/WCImage"));
    ASSERT(imageCls);
    return imageCls;
}

jclass PG_GetImageFrameClass(JNIEnv* env)
{
    static JGClass imageFrameCls(
        env->FindClass("com/sun/webkit/graphics/WCImageFrame"));
    ASSERT(imageFrameCls);
    return imageFrameCls;
}

jclass PG_GetRectangleClass(JNIEnv* env)
{
    static JGClass rectangleCls(
        env->FindClass("com/sun/webkit/graphics/WCRectangle"));
    ASSERT(rectangleCls);
    return rectangleCls;
}

jclass PG_GetFontClass(JNIEnv* env)
{
    static JGClass fontCls(
        env->FindClass("com/sun/webkit/graphics/WCFont"));
    ASSERT(fontCls);
    return fontCls;
}

jclass PG_GetFontCustomPlatformDataClass(JNIEnv* env)
{
    static JGClass fontCustomPlatformDataCls(env->FindClass(
            "com/sun/webkit/graphics/WCFontCustomPlatformData"));
    ASSERT(fontCustomPlatformDataCls);
    return fontCustomPlatformDataCls;
}

JLObject PL_GetGraphicsManager(JNIEnv* env)
{
    static jmethodID getGraphicsManagerMID = env->GetStaticMethodID(PG_GetGraphicsManagerClass(env),
            "getGraphicsManager",
            "()Lcom/sun/webkit/graphics/WCGraphicsManager;");
    ASSERT(getGraphicsManagerMID);

    JLObject mgr(env->CallStaticObjectMethod(
        PG_GetGraphicsManagerClass(env), getGraphicsManagerMID));
    ASSERT(mgr);
    WTF::CheckAndClearException(env);

    return mgr;
}

jclass PG_GetGraphicsImageDecoderClass(JNIEnv* env)
{
    static JGClass graphicsImageDecoderCls(
        env->FindClass("com/sun/webkit/graphics/WCImageDecoder"));
    ASSERT(graphicsImageDecoderCls);
    return graphicsImageDecoderCls;
}

jclass PG_GetRefClass(JNIEnv* env)
{
    static JGClass refCls(
        env->FindClass("com/sun/webkit/graphics/Ref"));
    ASSERT(refCls);
    return refCls;
}

jclass PG_GetRenderQueueClass(JNIEnv* env)
{
    static JGClass rqCls(
        env->FindClass("com/sun/webkit/graphics/WCRenderQueue"));
    ASSERT(rqCls);
    return rqCls;
}

jclass PG_GetMediaPlayerClass(JNIEnv* env)
{
    static JGClass mediaPlayerCls(
        env->FindClass("com/sun/webkit/graphics/WCMediaPlayer"));
    ASSERT(mediaPlayerCls);
    return mediaPlayerCls;
}

jclass PG_GetTransformClass(JNIEnv* env)
{
    static JGClass cls(
        env->FindClass("com/sun/webkit/graphics/WCTransform"));
    ASSERT(cls);
    return cls;
}

jclass PG_GetWebPageClass(JNIEnv* env)
{
    static JGClass cls(
        env->FindClass("com/sun/webkit/WebPage"));
    ASSERT(cls);
    return cls;
}

jclass PG_GetColorChooserClass(JNIEnv* env)
{
    static JGClass cls(
        env->FindClass("com/sun/webkit/ColorChooser"));
    return cls;
}

jclass getTimerClass(JNIEnv* env)
{
    static JGClass timerCls(
        env->FindClass("com/sun/webkit/Timer"));
    return timerCls;
}

jclass PG_GetRenderThemeClass(JNIEnv* env)
{
    static JGClass jRenderThemeCls(
        env->FindClass("com/sun/webkit/graphics/RenderTheme"));
    ASSERT(jRenderThemeCls);

    return jRenderThemeCls;
}

JLObject PG_GetRenderThemeObjectFromPage(JNIEnv* env, JLObject page)
{
    if (!page) {
        static jmethodID mid  = env->GetStaticMethodID(
            PG_GetWebPageClass(env),
            "fwkGetDefaultRenderTheme",
            "()Lcom/sun/webkit/graphics/RenderTheme;");
        ASSERT(mid);

        JLObject jRenderTheme(env->CallStaticObjectMethod(PG_GetWebPageClass(env), mid));
        WTF::CheckAndClearException(env);

        return jRenderTheme;
    }

    static jmethodID mid  = env->GetMethodID(
        PG_GetWebPageClass(env),
        "getRenderTheme",
        "()Lcom/sun/webkit/graphics/RenderTheme;");
    ASSERT(mid);

    JLObject jRenderTheme(env->CallObjectMethod(
        page,
        mid));
    WTF::CheckAndClearException(env);

    return jRenderTheme;
}

} // namespace
