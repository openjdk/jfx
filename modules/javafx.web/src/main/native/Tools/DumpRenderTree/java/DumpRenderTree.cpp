/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include <memory>
#include "JavaEnv.h"
#include "TestRunner.h"
#include "GCController.h"
#include "EventSender.h"
#include "WorkQueue.h"
#include "WebCore/testing/js/WebCoreTestSupport.h"

#include <wtf/RefPtr.h>
#include <JavaScriptCore/JavaScript.h>
#include <JavaScriptCore/JSCConfig.h>
#include <JavaScriptCore/TestRunnerUtils.h>

RefPtr<TestRunner> gTestRunner;
std::unique_ptr<GCController> gGCController;
JSGlobalContextRef gContext;

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_com_sun_javafx_webkit_drt_DumpRenderTree_initDRT
    (JNIEnv* env, jclass cls)
{
    JSC::Config::configureForTesting();
}

JNIEXPORT void JNICALL Java_com_sun_javafx_webkit_drt_DumpRenderTree_initTest
    (JNIEnv* env, jclass cls, jstring testPath, jstring pixelsHash)
{
    const char* testPathChars = env->GetStringUTFChars(testPath, NULL);
    const char* pixelsHashChars = env->GetStringUTFChars(pixelsHash, NULL);

    ASSERT(!gTestRunner);
    gTestRunner = TestRunner::create(testPathChars, pixelsHashChars);
    ASSERT(!gGCController);
    gGCController = std::make_unique<GCController>();

    DRT::WorkQueue::singleton().clear();

    env->ReleaseStringUTFChars(testPath, testPathChars);
    env->ReleaseStringUTFChars(pixelsHash, pixelsHashChars);
}

JNIEXPORT void JNICALL Java_com_sun_javafx_webkit_drt_DumpRenderTree_didClearWindowObject
    (JNIEnv* env, jclass cls, jlong pContext, jlong pWindowObject,
    jobject eventSender)
{
    if(!gTestRunner || !gGCController)
        return;
    ASSERT(pContext);
    ASSERT(pWindowObject);
    ASSERT(eventSender);

    gContext = static_cast<JSGlobalContextRef>(jlong_to_ptr(pContext));
    JSObjectRef windowObject =
            static_cast<JSObjectRef>(jlong_to_ptr(pWindowObject));

    JSValueRef exception = 0;

    gTestRunner->makeWindowObject(gContext, windowObject, &exception);
    ASSERT(!exception);

    JLObject jlEventSender(eventSender, true);
    makeEventSender(gContext, windowObject, jlEventSender, &exception);
    ASSERT(!exception);
    WebCoreTestSupport::injectInternalsObject(gContext);
    gGCController->makeWindowObject(gContext, windowObject, &exception);
    ASSERT(!exception);
}

JNIEXPORT void JNICALL Java_com_sun_javafx_webkit_drt_DumpRenderTree_dispose
    (JNIEnv* env, jclass cls)
{
    ASSERT(gTestRunner);
    gTestRunner->cleanup();
    gTestRunner = nullptr;
    ASSERT(gGCController);
    gGCController = nullptr;
    JSC::waitForVMDestruction();
}

JNIEXPORT jboolean JNICALL Java_com_sun_javafx_webkit_drt_DumpRenderTree_dumpAsText
    (JNIEnv* env, jclass cls)
{
    ASSERT(gTestRunner);
    return bool_to_jbool(gTestRunner->dumpAsText());
}

JNIEXPORT jboolean JNICALL Java_com_sun_javafx_webkit_drt_DumpRenderTree_dumpChildFramesAsText
    (JNIEnv* env, jclass cls)
{
    ASSERT(gTestRunner);
    return bool_to_jbool(gTestRunner->dumpChildFramesAsText());
}

JNIEXPORT jboolean JNICALL Java_com_sun_javafx_webkit_drt_DumpRenderTree_didFinishLoad
    (JNIEnv* env, jclass cls)
{
    ASSERT(gTestRunner);
    return bool_to_jbool(DRT::WorkQueue::singleton().processWork());
}

JNIEXPORT jboolean JNICALL Java_com_sun_javafx_webkit_drt_DumpRenderTree_dumpBackForwardList
    (JNIEnv* env, jclass cls)
{
    ASSERT(gTestRunner);
    return bool_to_jbool(gTestRunner->dumpBackForwardList());
}

JNIEXPORT jboolean JNICALL Java_com_sun_javafx_webkit_drt_DumpRenderTree_shouldStayOnPageAfterHandlingBeforeUnload
    (JNIEnv*, jclass)
{
    ASSERT(gTestRunner);
    return bool_to_jbool(gTestRunner->shouldStayOnPageAfterHandlingBeforeUnload());
}

JNIEXPORT jobjectArray JNICALL Java_com_sun_javafx_webkit_drt_DumpRenderTree_openPanelFiles
    (JNIEnv* env, jclass)
{
    ASSERT(gTestRunner);
    const auto& openFiles = gTestRunner->openPanelFiles();
    static JGClass stringCls = env->FindClass("java/lang/String");
    ASSERT(stringCls);
    jobjectArray files = env->NewObjectArray(openFiles.size(), stringCls, env->NewStringUTF(""));
    for (auto i = 0; i < openFiles.size(); i++) {
        env->SetObjectArrayElement(files, i, env->NewStringUTF(openFiles[i].c_str()));
    }
    return files;
}

#ifdef __cplusplus
}
#endif
