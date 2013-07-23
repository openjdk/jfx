/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "FloatRect.h"
#include "Frame.h"
#include "FrameView.h"
#include "HostWindow.h"
#include "JavaEnv.h"
#include "PlatformScreen.h"
#include "ScrollView.h"
#include "WebPage.h"
#include "Widget.h"

#include "NotImplemented.h"

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

namespace WebCore
{

int screenHorizontalDPI(Widget* widget)
{
    notImplemented();
    return 0;
}

int screenVerticalDPI(Widget* widget)
{
    notImplemented();
    return 0;
}

int screenDepth(Widget* w)
{
    if (!w)
        return 24;

    ASSERT(w->root());
    ASSERT(w->root()->hostWindow());
    PlatformWidget j(w->root()->hostWindow()->platformPageClient());
    if (!j)
        return 24;

    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

    jint depth(env->CallIntMethod(
            (jobject) j,
            getScreenDepthMID));
    CheckAndClearException(env);

    return depth;
}

int screenDepthPerComponent(Widget* w)
{
    return screenDepth(w) / 3;
}

bool screenIsMonochrome(Widget* w)
{
    notImplemented();
    return false;
}

FloatRect getScreenRect(Widget* w, bool available)
{
    if (!w)
        return IntRect(0, 0, 0, 0);

    ASSERT(w->root());
    ASSERT(w->root()->hostWindow());
    PlatformWidget j(w->root()->hostWindow()->platformPageClient());
    if (!j)
        return IntRect(0, 0, 0, 0);

    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

    JLObject rect(env->CallObjectMethod(
            (jobject) j,
            getScreenRectMID,
            bool_to_jbool(available)));
    CheckAndClearException(env);

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

} // namespace WebCore
