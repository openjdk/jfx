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
 * library path implements the {@code wkj_*} ABI this module is written against.
 * <p>
 * It is deliberately not tagged {@code ffm}, so it runs in the ordinary module execution against the
 * ordinary library rather than against {@code wkjstub}. When that library exports no
 * {@code wkj_abi_version}, as a JNI-era {@code jfxwebkit} from an OpenJFX SDK does not, or reports
 * a version other than {@code WKJ_ABI_VERSION}, this test <b>fails</b>, once, with the reason and
 * the fix. The alternative, a green build that verified nothing, is the outcome the fork's testing
 * rules forbid.
 * <p>
 * <b>This guard is permanent.</b> {@code jfxwebkit} is built out of tree, by
 * {@code .github/workflows/build-webkit.yml}, and reaches the library path as a prebuilt binary, so
 * no commit to this module can vouch for the library a given checkout runs against; this test is
 * what checks it, on every run. See {@code FFM-TEST-PLAN.md} section 5 and
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
                        + " not implement the wkj_* ABI that javafx.web binds."
                        + System.lineSeparator() + "  " + WebKitNativeShim.abiUnavailableReason()
                        + System.lineSeparator()
                        + "  Extract the jfxwebkit Release zip that .github/workflows/build-webkit.yml"
                        + " published for this platform into caches/sdk next to the repository, or run"
                        + " that workflow on this revision to build one; see WEBKIT-MEDIA-STUBS.md."
                        + " A jfxwebkit from an OpenJFX SDK or Maven Central is a JNI build that"
                        + " exports Java_* entry points and no wkj_* symbols, so it never passes."
                        + System.lineSeparator()
                        + "  Until then the module's own tests cannot be evaluated, and this is the"
                        + " only test that says so.");
    }
}
