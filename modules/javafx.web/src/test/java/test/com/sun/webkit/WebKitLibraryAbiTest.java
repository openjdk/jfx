/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.webkit;

import com.sun.webkit.WebKitNativeShim;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The sentinel. It has one job: when the {@code javafx.web} test suite is run for real with
 * {@code -Djfx.web.skipTests=false}, say in one sentence whether the {@code jfxwebkit} on the
 * library path implements the {@code wkj_*} ABI this module is now written against.
 * <p>
 * It is deliberately not tagged {@code ffm}, so it runs in the ordinary module execution against the
 * ordinary library rather than against {@code wkjstub}. Until {@code jfxwebkit} is rebuilt from the
 * migrated sources this test <b>fails</b>, once, with the reason and the fix - which is the point.
 * The alternative, a green build that verified nothing, is the outcome the fork's testing rules
 * forbid.
 * <p>
 * <b>This class is deleted in the same commit that lands a rebuilt {@code jfxwebkit}</b>, and that
 * commit must show the module's own tests passing again. See {@code FFM-TEST-PLAN.md} section 5 and
 * {@code FFM-ABI-CONTRACT.md} section 8.
 */
public class WebKitLibraryAbiTest {

    /**
     * The probe is a symbol lookup, not a broad {@code try new WebPage() catch}: a broad catch would
     * also swallow a real initialisation bug and report it as this one.
     */
    @Test
    public void theLoadedLibraryImplementsTheWkjAbi() {
        assertTrue(WebKitNativeShim.abiAvailable(),
                () -> "The " + WebKitNativeShim.libraryName() + " library on java.library.path does"
                        + " not implement the wkj_* ABI that javafx.web now binds."
                        + System.lineSeparator() + "  " + WebKitNativeShim.abiUnavailableReason()
                        + System.lineSeparator()
                        + "  The prebuilt library exports Java_com_sun_* JNI entry points instead."
                        + " Rebuild jfxwebkit from the migrated sources in"
                        + " modules/javafx.web/src/main/native with the WebKit CMake and ninja"
                        + " toolchain; see WEBKIT-MEDIA-STUBS.md and FFM-ABI-CONTRACT.md section 8."
                        + System.lineSeparator()
                        + "  Until then the module's own tests cannot be evaluated, and this is the"
                        + " only test that says so.");
    }
}
