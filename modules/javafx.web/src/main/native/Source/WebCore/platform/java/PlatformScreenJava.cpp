/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include "config.h"

#include "FloatRect.h"
#include "Frame.h"
#include "FrameView.h"
#include "HostWindow.h"
#include "PlatformJavaClasses.h"
#include "PlatformScreen.h"
#include "ScrollView.h"
#include "Widget.h"

#include "NotImplemented.h"

namespace PlatformScreenJavaInternal {

static JGClass rectangleCls;
static JGClass widgetClass;

static jfieldID rectxFID;
static jfieldID rectyFID;
static jfieldID rectwFID;
static jfieldID recthFID;
static jmethodID getScreenDepthMID;
static jmethodID getScreenRectMID;

static void initRefs(JNIEnv* env)
{
    if (!widgetClass) {
        widgetClass = JLClass(env->FindClass("com/sun/webkit/WCWidget"));
        ASSERT(widgetClass);

        getScreenDepthMID = env->GetMethodID(
                widgetClass,
                "fwkGetScreenDepth",
                "()I");
        ASSERT(getScreenDepthMID);

        getScreenRectMID = env->GetMethodID(
                widgetClass,
                "fwkGetScreenRect",
                "(Z)Lcom/sun/webkit/graphics/WCRectangle;");
        ASSERT(getScreenRectMID);

        rectangleCls = JLClass(env->FindClass("com/sun/webkit/graphics/WCRectangle"));
        ASSERT(rectangleCls);

        rectxFID = env->GetFieldID(rectangleCls, "x", "F");
        ASSERT(rectxFID);
        rectyFID = env->GetFieldID(rectangleCls, "y", "F");
        ASSERT(rectyFID);
        rectwFID = env->GetFieldID(rectangleCls, "w", "F");
        ASSERT(rectwFID);
        recthFID = env->GetFieldID(rectangleCls, "h", "F");
        ASSERT(recthFID);
    }
}
}

namespace WebCore
{

int screenHorizontalDPI(Widget*)
{
    notImplemented();
    return 0;
}

int screenVerticalDPI(Widget*)
{
    notImplemented();
    return 0;
}

int screenDepth(Widget* w)
{
    using namespace PlatformScreenJavaInternal;
    if (!w)
        return 24;

    ASSERT(w->root());
    ASSERT(w->root()->hostWindow());
    PlatformWidget j(w->root()->hostWindow()->platformPageClient());
    if (!j)
        return 24;

    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    jint depth(env->CallIntMethod(
            (jobject) j,
            getScreenDepthMID));
    WTF::CheckAndClearException(env);

    return depth;
}

int screenDepthPerComponent(Widget* w)
{
    return screenDepth(w) / 3;
}

bool screenIsMonochrome(Widget*)
{
    notImplemented();
    return false;
}

FloatRect getScreenRect(Widget* w, bool available)
{
    using namespace PlatformScreenJavaInternal;
    if (!w)
        return IntRect(0, 0, 0, 0);

    ASSERT(w->root());
    ASSERT(w->root()->hostWindow());
    PlatformWidget j(w->root()->hostWindow()->platformPageClient());
    if (!j)
        return IntRect(0, 0, 0, 0);

    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    JLObject rect(env->CallObjectMethod(
            (jobject) j,
            getScreenRectMID,
            bool_to_jbool(available)));
    WTF::CheckAndClearException(env);

    if (!rect) {
        return IntRect(0, 0, 0, 0);
    }

    float x = env->GetFloatField(rect, rectxFID);
    float y = env->GetFloatField(rect, rectyFID);
    float width = env->GetFloatField(rect, rectwFID);
    float height = env->GetFloatField(rect, recthFID);

    return FloatRect(x, y, width, height);
}

FloatRect screenRect(Widget* w)
{
    return getScreenRect(w, false);
}

FloatRect screenAvailableRect(Widget* w)
{
    return getScreenRect(w, true);
}

bool screenHasInvertedColors() //XXX: recheck
{
    return false;
}

bool screenSupportsExtendedColor(Widget*)
{
    return false;
}

} // namespace WebCore
