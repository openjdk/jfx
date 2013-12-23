/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "NotImplemented.h"

#include "JavaEnv.h"
#include "StringJava.h"
#include "PluginPackage.h"
#include "PluginView.h"

#include "com_sun_webkit_WCPluginWidget.h"

namespace WebCore {

/*
PluginWidgetJava::PluginWidgetJava(jobject wfh, const IntSize& size,
                                   const String& url, const String& mimeType,
                                   const Vector<String>& paramNames,
                                   const Vector<String>& paramValues)
{
    m_size = size;
    m_url = url;
    m_mimeType = mimeType;
    m_paramNames = paramNames;
    m_paramValues = paramValues;

    JNIEnv* env = WebCore_GetJavaEnv();
    jstring urlJavaString = url.toJavaString(env);
    jstring mimeTypeJavaString = mimeType.toJavaString(env);

    jclass cls = env->FindClass("com/sun/webkit/WCPluginWidget");
    ASSERT(cls);

    jobjectArray pNames = strVect2JArray(env, paramNames);

    jobjectArray pValues = strVect2JArray(env, paramValues);

    jobject obj = env->CallStaticObjectMethod(cls, pluginWidgetCreateMID, wfh,
        url.toJavaString(env), mimeTypeJavaString, pNames, pValues);
    CheckAndClearException(env);
    ASSERT(obj);
    if (obj) {
        setJavaObject(obj);
        env->CallVoidMethod(obj, pluginWidgetFWKInitMID, size.width(),
                        size.height(), url.toJavaString(env));
        CheckAndClearException(env);
        env->DeleteLocalRef(obj);
    }
    env->DeleteLocalRef(cls);
    env->DeleteLocalRef(pNames);
    env->DeleteLocalRef(pValues);
}
*/

/*
PluginWidgetJava::~PluginWidgetJava()
{
    JNIEnv* env = WebCore_GetJavaEnv();

    env->CallVoidMethod(javaObject(), pluginWidgetFWKDestroyMID);
    CheckAndClearException(env);
}
*/

PluginView::~PluginView()
{
    notImplemented();
}

void PluginView::paint(GraphicsContext* gc, const IntRect& r)
{
    notImplemented();
}

void PluginView::invalidateRect(const IntRect& r)
{
    notImplemented();
}

void PluginView::setFocus()
{
    notImplemented();
}

void PluginView::show()
{
    notImplemented();
}

void PluginView::hide()
{
    notImplemented();
}

void PluginView::setParent(ScrollView* parent)
{
    notImplemented();
}

void PluginView::setParentVisible(bool visible)
{
    notImplemented();
}

} // namespace WebCore
