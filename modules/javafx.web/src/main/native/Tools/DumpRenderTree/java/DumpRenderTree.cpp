/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include <cstring>
#include <memory>
#include <string>
#include <vector>

#include <drt_java_api.h>
#include "TestRunner.h"
#include "GCController.h"
#include "EventSender.h"
#include "WorkQueue.h"
#include "WebCore/testing/js/WebCoreTestSupport.h"

#include <wtf/RefPtr.h>
#include <JavaScriptCore/JavaScript.h>
#include <JavaScriptCore/JSCConfig.h>
#include <JavaScriptCore/JSStringRef.h>
#include <JavaScriptCore/TestRunnerUtils.h>

RefPtr<TestRunner> gTestRunner;
std::unique_ptr<GCController> gGCController;
JSGlobalContextRef gContext;

// The process-wide callback table, installed by drt_init. It replaces the JVM pointer,
// the global DumpRenderTree class reference and the eight cached static method ids that
// the deleted JavaEnv.cpp kept.
const WKJDrtHost* drt_host = nullptr;

// UTF-16 in, UTF-8 out. TestRunner stores the test path and the pixel hash as std::string,
// so the conversion has to happen somewhere; doing it through JSC keeps one well-tested
// UTF-16 decoder in the picture instead of a hand-written one. The JNI implementation
// received modified UTF-8 from GetStringUTFChars, which differs from this only for U+0000
// and for characters outside the BMP.
static std::string toUTF8(const uint16_t* s, int32_t length)
{
    if (!s || length <= 0)
        return std::string();

    JSStringRef string = JSStringCreateWithCharacters(reinterpret_cast<const JSChar*>(s),
            static_cast<size_t>(length));
    const size_t bufferSize = JSStringGetMaximumUTF8CStringSize(string);
    std::vector<char> buffer(bufferSize);
    JSStringGetUTF8CString(string, buffer.data(), bufferSize);
    JSStringRelease(string);
    return std::string(buffer.data());
}

// UTF-8 in, UTF-16 out, following the WKJ_STR_* protocol of drt_java_api.h.
static int32_t returnUTF8AsUTF16(const std::string& value, uint16_t* result_buf,
        int32_t result_cap, int32_t* result_length)
{
    JSStringRef string = JSStringCreateWithUTF8CString(value.c_str());
    const int32_t length = static_cast<int32_t>(JSStringGetLength(string));
    if (result_length)
        *result_length = length;

    if (!result_buf || result_cap < length) {
        JSStringRelease(string);
        return WKJ_STR_OVERFLOW;
    }

    const JSChar* characters = JSStringGetCharactersPtr(string);
    if (characters && length > 0)
        memcpy(result_buf, characters, static_cast<size_t>(length) * sizeof(uint16_t));
    JSStringRelease(string);
    return WKJ_STR_OK;
}

extern "C" {

WKJ_EXPORT uint32_t drt_abi_version(void)
{
    return DRT_ABI_VERSION;
}

WKJ_EXPORT int32_t drt_init(const WKJDrtHost* host, int32_t host_size, uint32_t abi_version)
{
    if (!host)
        return DRT_INIT_ERR_NULL_HOST;
    if (abi_version != DRT_ABI_VERSION)
        return DRT_INIT_ERR_ABI_VERSION;
    if (host_size != static_cast<int32_t>(sizeof(WKJDrtHost)) || host->size != host_size)
        return DRT_INIT_ERR_HOST_SIZE;
    if (drt_host)
        return DRT_INIT_ERR_ALREADY_INITED;

    drt_host = host;
    return DRT_INIT_OK;
}

WKJ_EXPORT void drt_init_drt(void)
{
    WTF::setPermissionsOfConfigPage();
    WTF::Config::disableFreezingForTesting();
    JSC::Config::enableRestrictedOptions();
}

WKJ_EXPORT void drt_init_test(const uint16_t* test_path, int32_t test_path_len,
        const uint16_t* pixels_hash, int32_t pixels_hash_len)
{
    const std::string testPath = toUTF8(test_path, test_path_len);
    const std::string pixelsHash = toUTF8(pixels_hash, pixels_hash_len);

    ASSERT(!gTestRunner);
    gTestRunner = TestRunner::create(testPath, pixelsHash);
    ASSERT(!gGCController);
    gGCController = std::make_unique<GCController>();

    DRT::WorkQueue::singleton().clear();
}

WKJ_EXPORT void drt_did_clear_window_object(int64_t js_context, int64_t js_window_object,
        wkj_ref event_sender)
{
    if (!gTestRunner || !gGCController)
        return;
    ASSERT(js_context);
    ASSERT(js_window_object);
    ASSERT(event_sender);

    gContext = static_cast<JSGlobalContextRef>(wkj_to_ptr(js_context));
    JSObjectRef windowObject =
            static_cast<JSObjectRef>(wkj_to_ptr(js_window_object));

    JSValueRef exception = 0;

    gTestRunner->makeWindowObject(gContext);

    makeEventSender(gContext, windowObject, event_sender, &exception);
    ASSERT(!exception);
    WebCoreTestSupport::injectInternalsObject(gContext);
    gGCController->makeWindowObject(gContext);
}

WKJ_EXPORT void drt_dispose(void)
{
    ASSERT(gTestRunner);
    gTestRunner->cleanup();
    gTestRunner = nullptr;
    ASSERT(gGCController);
    gGCController = nullptr;
    JSC::waitForVMDestruction();
}

WKJ_EXPORT int32_t drt_dump_as_text(void)
{
    ASSERT(gTestRunner);
    return gTestRunner->dumpAsText() ? 1 : 0;
}

WKJ_EXPORT int32_t drt_dump_child_frames_as_text(void)
{
    ASSERT(gTestRunner);
    return gTestRunner->dumpChildFramesAsText() ? 1 : 0;
}

WKJ_EXPORT int32_t drt_did_finish_load(void)
{
    ASSERT(gTestRunner);
    return DRT::WorkQueue::singleton().processWork() ? 1 : 0;
}

WKJ_EXPORT int32_t drt_dump_back_forward_list(void)
{
    ASSERT(gTestRunner);
    return gTestRunner->dumpBackForwardList() ? 1 : 0;
}

WKJ_EXPORT int32_t drt_should_stay_on_page_after_handling_before_unload(void)
{
    ASSERT(gTestRunner);
    return gTestRunner->shouldStayOnPageAfterHandlingBeforeUnload() ? 1 : 0;
}

WKJ_EXPORT int32_t drt_open_panel_file_count(void)
{
    ASSERT(gTestRunner);
    return static_cast<int32_t>(gTestRunner->openPanelFiles().size());
}

WKJ_EXPORT int32_t drt_open_panel_file(int32_t index, uint16_t* result_buf,
        int32_t result_cap, int32_t* result_length)
{
    ASSERT(gTestRunner);
    if (result_length)
        *result_length = 0;

    const auto& openFiles = gTestRunner->openPanelFiles();
    if (index < 0 || static_cast<size_t>(index) >= openFiles.size())
        return WKJ_STR_NULL;

    return returnUTF8AsUTF16(openFiles[index], result_buf, result_cap, result_length);
}

#if OS(WINDOWS)
#include <Windows.h>
#include <math.h>

// Moved here verbatim from the deleted JavaEnv.cpp, which is where this library kept its
// DllMain alongside JNI_OnLoad.
BOOL WINAPI DllMain(HINSTANCE hinstDLL, DWORD fdwReason, LPVOID lpvReserved)
{
    if (fdwReason == DLL_PROCESS_ATTACH) {
#if defined(_MSC_VER) && _MSC_VER >= 1800 && _MSC_VER < 1900 && defined(_M_X64) || defined(__x86_64__)
        // The VS2013 runtime has a bug where it mis-detects AVX-capable processors
        // if the feature has been disabled in firmware. This causes us to crash
        // in some of the math functions. For now, we disable those optimizations
        // because Microsoft is not going to fix the problem in VS2013.
        // FIXME: Remove this workaround when we switch to VS2015+.
        _set_FMA3_enable(0);
#endif
    }

    return TRUE;
}
#endif

} // extern "C"
