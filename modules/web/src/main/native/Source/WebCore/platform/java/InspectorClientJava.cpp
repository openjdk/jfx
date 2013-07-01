/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"
#include "InspectorClientJava.h"

#include "Frame.h"
#include "NotImplemented.h"
#include "Page.h"
#include "RenderObject.h"
#include "WebPage.h"

static JGClass webPageClass;
static jmethodID repaintAllMethod;
static jmethodID sendInspectorMessageToFrontendMethod;

static void initRefs(JNIEnv* env)
{
    if (!webPageClass) {
        webPageClass = JLClass(env->FindClass(
                "com/sun/webkit/WebPage"));
        ASSERT(webPageClass);

        repaintAllMethod = env->GetMethodID(
                webPageClass,
                "fwkRepaintAll",
                "()V");
        ASSERT(repaintAllMethod);

        sendInspectorMessageToFrontendMethod = env->GetMethodID(
                webPageClass,
                "fwkSendInspectorMessageToFrontend",
                "(Ljava/lang/String;)Z");
        ASSERT(sendInspectorMessageToFrontendMethod);
    }
}

namespace WebCore {

InspectorClientJava::InspectorClientJava(const JLObject &webPage)
    : m_webPage(webPage)
{
}

void InspectorClientJava::inspectorDestroyed()
{
    delete this;
}

InspectorFrontendChannel* InspectorClientJava::openInspectorFrontend(InspectorController*)
{
    //FIXME: need to be realized!
    notImplemented();
    return this;
}

void InspectorClientJava::closeInspectorFrontend()
{
    notImplemented();
}

void InspectorClientJava::bringFrontendToFront()
{
    notImplemented();
}

void InspectorClientJava::highlight()
{
    // InspectorController::drawHighlight() may want to draw outside any
    // node boundary so our only option here is invalidate the entire page.
    // See also WebPage_twkDrawHighlight.

    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

    env->CallVoidMethod(m_webPage, repaintAllMethod);
    CheckAndClearException(env);
}

void InspectorClientJava::hideHighlight()
{
    highlight();
}

bool InspectorClientJava::sendMessageToFrontend(const String& message)
{
    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

    jboolean result = env->CallBooleanMethod(
            m_webPage,
            sendInspectorMessageToFrontendMethod,
            (jstring) message.toJavaString(env));
    CheckAndClearException(env);

    return jbool_to_bool(result);
}

} // namespace WebCore
