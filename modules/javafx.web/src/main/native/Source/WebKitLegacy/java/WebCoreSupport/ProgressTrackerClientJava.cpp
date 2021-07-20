/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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


#include "ProgressTrackerClientJava.h"
#include "WebPage.h"

#include <WebCore/DocumentLoader.h>
#include <WebCore/FrameLoader.h>
#include <WebCore/Page.h>
#include <WebCore/ProgressTracker.h>

#include "com_sun_webkit_LoadListenerClient.h"

namespace ProgressTrackerClientJavaInternal {
static JGClass webPageClass;
static jmethodID fireLoadEventMID;

static void initRefs(JNIEnv* env)
{
    if (!webPageClass) {
        webPageClass = JLClass(env->FindClass(
            "com/sun/webkit/WebPage"));
        ASSERT(webPageClass);

        fireLoadEventMID = env->GetMethodID(webPageClass,
                                    "fwkFireLoadEvent",
                                    "(JILjava/lang/String;Ljava/lang/String;DI)V");
        ASSERT(fireLoadEventMID);
    }
}
}

namespace WebCore {

ProgressTrackerClientJava::ProgressTrackerClientJava(const JLObject& webPage)
    : m_webPage(webPage)
{
}

void ProgressTrackerClientJava::progressStarted(Frame&)
{
}

void ProgressTrackerClientJava::progressEstimateChanged(Frame& originatingProgressFrame)
{
    using namespace ProgressTrackerClientJavaInternal;
    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    double progress = originatingProgressFrame.page()->progress().estimatedProgress();
    // We have a redundant notification from webkit (with progress == 1)
    // after PAGE_FINISHED has already been posted.
    DocumentLoader* documentLoader = originatingProgressFrame.loader().activeDocumentLoader();
    if (documentLoader && progress < 1) {
        JLString urlJavaString(documentLoader->url().string().toJavaString(env));
        JLString contentTypeJavaString(documentLoader->responseMIMEType().toJavaString(env));

        if (documentLoader->mainResourceData()) {
            documentLoader->mainResourceData()->size(); // TODO-java: recheck
        }
        // Second, send a load event
        env->CallVoidMethod(m_webPage, fireLoadEventMID,
                            ptr_to_jlong(&originatingProgressFrame),
                            com_sun_webkit_LoadListenerClient_PROGRESS_CHANGED,
                            (jstring)urlJavaString,
                            (jstring)contentTypeJavaString,
                            progress,
                            0);
        WTF::CheckAndClearException(env);
    }
}

void ProgressTrackerClientJava::progressFinished(Frame&)
{
    // shouldn't post PROGRESS_CHANGED after PAGE_FINISHED
}

} // namespace WebCore
