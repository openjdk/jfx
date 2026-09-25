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

package test.com.sun.webkit.ffm;

import com.sun.webkit.WebKitNativeShim;
import com.sun.webkit.WebKitNativeShim.InstalledSlot;
import com.sun.webkit.WkjStubShim;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Every slot of every callback table javafx.web builds, read back out of the table production
 * allocated and compared with the C prototype of the slot it sits in. The C side comes from
 * {@code gen-wkjstub.pl}, which flattens every callback struct of the ABI headers - {@code WKJHost}
 * and each table installed on its own: the seven page sub-tables, the network, popup, colour
 * chooser, back-forward and event listener tables, and {@code WKJLiveConnectHost} - into the
 * member, its {@code offsetof} and the kinds of its prototype.
 * <p>
 * {@code WebKitLayoutTest} already compares the {@code WKJHost} descriptors by slot name. This
 * test reads the tables themselves, which is what makes it reach the tables filled by position:
 * {@code PopupMenuNative}, {@code ColorChooserNative}, {@code BackForwardListNative} and
 * {@code EventListenerNative} write their stubs in a fixed order, so if the header's members are
 * reordered and {@code WKJLayouts} is updated to match, the offsets all still agree while each
 * callback lands in its neighbour's slot. Reading the stub at each member's C offset catches that:
 * by its shape when the two prototypes differ, and by the name of its target when they do not,
 * because every target is named after its slot ({@code hide} for {@code WKJPopupCallbacks.hide},
 * {@code chromeSetFocus} for {@code WKJChromeCallbacks.set_focus}).
 * <p>
 * Production records the stubs and tables only with {@code -Dcom.sun.webkit.recordBindings=true},
 * which the {@code ffm-binding-test} surefire execution sets.
 */
@Tag("ffm")
public class WebKitCallbackTableTest {

    /**
     * The {@code WKJHost} slots production leaves {@code NULL} on purpose: the seven placeholder
     * groups and {@code theme.plugin_widget_paint}, named and justified in
     * {@code WebKitHostInstallTest}. Every other slot of every table holds a stub.
     */
    private static final int HOST_SLOTS_LEFT_NULL = 8;

    /** One slot as the C headers declare it. */
    private record CSlot(String table, String member, long offset, String signature) {
        @Override
        public String toString() {
            return table + "." + member;
        }
    }

    private static List<CSlot> slots;

    @BeforeAll
    static void readTheHeadersAndBuildTheTables() {
        assumeTrue(WkjStubShim.load(), WkjStubShim.loadFailure());
        WebKitNativeShim.buildCallbackTables();
        List<CSlot> fromC = new ArrayList<>();
        for (int i = 0, n = WkjStubShim.callbackSlotCount(); i < n; i++) {
            fromC.add(new CSlot(WkjStubShim.callbackSlotTable(i), WkjStubShim.callbackSlotName(i),
                    WkjStubShim.callbackSlotOffset(i), WkjStubShim.callbackSlotSignature(i)));
        }
        slots = List.copyOf(fromC);
    }

    @Test
    public void javafxWebBuildsEveryCallbackTableTheHeadersDeclare() {
        Set<String> missing = new TreeSet<>();
        for (CSlot slot : slots) {
            missing.add(slot.table());
        }
        assertTrue(missing.size() > 1, "the stub reports callback slots for " + missing
                + " only, so gen-wkjstub.pl no longer flattens the tables outside WKJHost");
        missing.removeAll(WebKitNativeShim.callbackTableNames());
        assertTrue(missing.isEmpty(), "the C headers declare these callback tables and javafx.web"
                + " allocated none of them, so nothing here reads them: " + missing);
    }

    /**
     * The check the offsets alone do not make: a stub whose descriptor has another shape than the
     * prototype of the slot it sits in is called with a mismatched calling convention, which
     * corrupts the stack rather than failing.
     */
    @Test
    public void everyFilledSlotHoldsAStubOfTheShapeItsCPrototypeDeclares() {
        List<String> mismatched = new ArrayList<>();
        int compared = 0;
        for (CSlot slot : slots) {
            InstalledSlot installed = WebKitNativeShim.installedSlot(slot.table(), slot.offset());
            if (installed == null) {
                continue;
            }
            compared++;
            if (!slot.signature().equals(installed.signature())) {
                mismatched.add(slot + ": C says " + slot.signature() + ", " + installed.target()
                        + " was bound as " + installed.signature());
            }
        }
        assertTrue(mismatched.isEmpty(), "of " + compared + " filled callback slots, these hold a"
                + " stub whose shape disagrees with the C prototype: " + mismatched);
        assertEquals(slots.size() - HOST_SLOTS_LEFT_NULL, compared,
                "the number of filled callback slots changed; WebKitHostInstallTest names the ones"
                        + " that are left NULL on purpose");
    }

    /**
     * The check the shapes do not make: two slots with the same prototype, such as
     * {@code WKJPopupCallbacks.hide} and {@code destroy}, can trade stubs without any descriptor
     * changing. Every target is named after the member it serves, with a prefix where one class
     * serves several tables, so the target's name must end in the member's.
     */
    @Test
    public void everyFilledSlotHoldsTheTargetNamedAfterIt() {
        List<String> misplaced = new ArrayList<>();
        for (CSlot slot : slots) {
            InstalledSlot installed = WebKitNativeShim.installedSlot(slot.table(), slot.offset());
            if (installed == null) {
                continue;
            }
            String method = installed.target().substring(installed.target().lastIndexOf('.') + 1);
            String member = slot.member().substring(slot.member().lastIndexOf('.') + 1);
            if (!normalized(method).endsWith(normalized(member))) {
                misplaced.add(slot + " holds " + installed.target());
            }
        }
        assertTrue(misplaced.isEmpty(), "these callback slots hold a stub for another slot's"
                + " target: " + misplaced);
    }

    /**
     * Only {@code WKJHost} may leave a slot {@code NULL}: its seven placeholder groups and one
     * unported slot, which {@code WebKitHostInstallTest} names and justifies. Every other table is
     * a client's complete set of callbacks, and a {@code NULL} there silently turns one of them
     * into the library's fallback.
     */
    @Test
    public void everyTableOutsideTheHostIsFilledCompletely() {
        List<String> empty = new ArrayList<>();
        for (CSlot slot : slots) {
            if (!"WKJHost".equals(slot.table())
                    && WebKitNativeShim.installedSlot(slot.table(), slot.offset()) == null) {
                empty.add(slot.toString());
            }
        }
        assertTrue(empty.isEmpty(), "these callback slots were left NULL: " + empty);
    }

    private static String normalized(String name) {
        return name.replace("_", "").toLowerCase(Locale.ROOT);
    }
}
