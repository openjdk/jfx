/*
 * Copyright (c) 2012, 2024, Oracle and/or its affiliates. All rights reserved.
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


#include "InspectorClientJava.h"
#include "WebPage.h"

#include <WebCore/Frame.h>
#include <WebCore/NotImplemented.h>
#include <WebCore/Page.h>
#include <WebCore/RenderObject.h>

namespace InspectorClientJavaInternal {

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
}

namespace WebCore {

InspectorClientJava::InspectorClientJava(const JLObject &webPage)
    : m_webPage(webPage)
{
}

void InspectorClientJava::inspectedPageDestroyed()
{
}

Inspector::FrontendChannel* InspectorClientJava::openLocalFrontend(InspectorController*)
{
    //FIXME: need to be realized!
    notImplemented();
    return this;
}

void InspectorClientJava::bringFrontendToFront()
{
    notImplemented();
}

void InspectorClientJava::highlight()
{
    using namespace InspectorClientJavaInternal;
    // InspectorController::drawHighlight() may want to draw outside any
    // node boundary so our only option here is invalidate the entire page.
    // See also WebPage_twkDrawHighlight.

    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    env->CallVoidMethod(m_webPage, repaintAllMethod);
    WTF::CheckAndClearException(env);
}

void InspectorClientJava::hideHighlight()
{
    highlight();
}

void InspectorClientJava::sendMessageToFrontend(const String& message)
{
    using namespace InspectorClientJavaInternal;
    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    env->CallBooleanMethod(m_webPage,
                           sendInspectorMessageToFrontendMethod,
                           (jstring)message.toJavaString(env));
    WTF::CheckAndClearException(env);
}

} // namespace WebCore
