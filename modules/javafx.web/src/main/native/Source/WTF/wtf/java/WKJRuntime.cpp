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

/*
 * WKJRuntime.cpp - the process-wide state of the library, and the load hook that installs it.
 *
 * This file is what wtf/java/JavaEnv.cpp becomes. That file held two globals and three JNI
 * hooks; the mapping is one for one:
 *
 *   JavaVM* jvm                       ->  const WKJHost* wkj_host
 *   volatile bool g_ShuttingDown      ->  unchanged, still here, still that name
 *   JNI_OnLoad / JNI_OnLoad_jfxwebkit ->  wkj_init
 *   JNI_OnUnload                      ->  nothing; see BELOW
 *   DllMain                           ->  unchanged, still here (it was never JNI)
 *
 * WHAT JNI_OnLoad DID THAT wkj_init DOES NOT HAVE TO
 *
 * JNI_OnLoad resolved com.sun.webkit.FileSystem eagerly, with a comment (JavaEnv.cpp:139-143)
 * explaining that the class loader which loaded jfxwebkit is reachable only from that hook:
 * from a WebKit-spawned thread, FindClass would have used the system loader and failed. That
 * whole constraint is a property of JNI class lookup, and this ABI has no class lookup - the
 * file-system upcalls are function pointers in WKJHostFileSystem, bound by Java where the
 * loader is not in question. So the eager resolution, the JGClass that held it and the
 * load-time ordering requirement all disappear together.
 *
 * WHAT HAPPENS TO JNI_OnUnload
 *
 * It did two things: zero the JavaVM pointer and, in a Windows debug build, call
 * _CrtDumpMemoryLeaks(). There is no unload hook in this ABI, and there is no need for the
 * first: Java owns the host table and simply stops calling. The leak dump is not lost either,
 * because the _CRTDBG_LEAK_CHECK_DF flag set below makes the CRT dump leaks at process exit;
 * what changes is the moment it prints, from library unload to process exit. In practice
 * JNI_OnUnload almost never ran - a library loaded with System.load is unloaded only when its
 * class loader is collected - so this is a debug-diagnostics timing difference and nothing
 * more.
 */

#include "config.h"

#include <wtf/java/WKJRuntime.h>

#if PLATFORM(JAVA_WIN) && !defined(NDEBUG)
#include <crtdbg.h>
#endif

/*
 * The installed callback table, NULL until wkj_init succeeds. Read on the hot path of every
 * upcall and of WKJHandle retain/release, exactly the way `extern JavaVM* jvm` was read.
 */
const WKJHost* wkj_host = nullptr;

/*
 * Set by wkj_set_shutdown (com.sun.webkit.MainThread.twkSetShutdown) and read by
 * WTF::wkjIsShuttingDown() and WebCore/platform/ThreadTimers.cpp. Declared in WKJRuntime.h.
 */
volatile bool g_ShuttingDown = false;

extern "C" {

uint32_t wkj_abi_version(void)
{
    return WKJ_ABI_VERSION;
}

int32_t wkj_init(const WKJHost* host, int32_t host_size, uint32_t abi_version)
{
    /*
     * Validate before storing anything, so that a mismatched pair of library and Java code is
     * rejected with a code the Java side can turn into one readable sentence, rather than
     * reading past the end of a table whose shape it disagrees about.
     */
    if (!host)
        return WKJ_INIT_ERR_NULL_HOST;

    if (abi_version != WKJ_ABI_VERSION)
        return WKJ_INIT_ERR_ABI_VERSION;

    if (host_size != static_cast<int32_t>(sizeof(WKJHost)) || host->size != host_size)
        return WKJ_INIT_ERR_HOST_SIZE;

    if (wkj_host)
        return WKJ_INIT_ERR_ALREADY_INITED;

#if PLATFORM(JAVA_WIN) && !defined(NDEBUG)
    /*
     * The debug-CRT leak checking JNI_OnLoad used to turn on. Kept verbatim, moved to the hook
     * that replaced it; _CRTDBG_LEAK_CHECK_DF is what dumps the leaks at process exit.
     */
    _CrtSetReportMode(_CRT_ERROR, _CRTDBG_MODE_FILE);
    _CrtSetReportFile(_CRT_ERROR, _CRTDBG_FILE_STDERR);

    int tmpFlag = _CrtSetDbgFlag(_CRTDBG_REPORT_FLAG);
    tmpFlag |= _CRTDBG_CHECK_CRT_DF | _CRTDBG_LEAK_CHECK_DF; //| _CRTDBG_CHECK_EVERY_1024_DF;
    _CrtSetDbgFlag(tmpFlag);
#endif

    wkj_host = host;
    return WKJ_INIT_OK;
}

} // extern "C"

#if OS(WINDOWS)
#include <Windows.h>
#include <math.h>

/*
 * Not JNI, and not part of the ABI - it moved here with the rest of the file-scope state when
 * JavaEnv.cpp was retired. Unchanged.
 */
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
