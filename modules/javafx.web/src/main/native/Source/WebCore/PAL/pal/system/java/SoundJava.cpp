/*
 * Copyright (c) 2013, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include "Sound.h"

#include <webkit_java_api.h>

#include <wtf/java/WKJRuntime.h>

namespace PAL {

void systemBeep()
{
    /*
     * The JNI body opened with WC_GETJAVAENV_CHKRET(env), which returned early when
     * WTF::GetJavaEnv() gave back null - i.e. when the calling thread was not attached to the
     * JVM. An FFM upcall stub attaches on its own, so that early return has no counterpart
     * and the call would now go through where it used to be dropped. The shutdown test below
     * is the part of that gate worth keeping: it is what stopped the library calling into a
     * Java side that is tearing down. See THE SHUTDOWN GATE in wtf/java/WKJRuntime.h.
     */
    WKJ_RETURN_IF_SHUTTING_DOWN();

    const WKJHostPAL* cb = wkj_host ? &wkj_host->pal : nullptr;
    if (!cb || !cb->system_beep)
        return;

    /*
     * This used to be FindClass("java/awt/Toolkit"), getDefaultToolkit(), beep() - three JNI
     * calls to reach one method on a class from java.desktop, a module javafx.web does not
     * require. When the class was absent FindClass returned null, the ASSERT was compiled out
     * of a release build and the two calls that followed did nothing. The slot keeps that
     * shape: whether the Java side can beep at all is now a question the Java side answers.
     */
    cb->system_beep();
    WTF::wkjCheckAndClearException();
}

} // namespace PAL
