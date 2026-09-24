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

package test.com.sun.glass.ui.monocle;

import com.sun.glass.ui.monocle.LinuxSystemShim;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The path a platform LinuxSystem refuses takes, driven on Windows, whose canonical C {@code long} is 4 bytes
 * (LLP64): the first {@code loadLibrary()} sees the refusal itself, an {@code UnsatisfiedLinkError} thrown out
 * of the holder's initialisation, and every later use sees the {@code NoClassDefFoundError} of a class whose
 * initialisation failed, turned back into an {@code UnsatisfiedLinkError} that names the state. This is the path
 * a broken Linux box takes too. Whichever test touched the holder first, the reason is in the message or the
 * cause chain of what this class sees, so the assertions do not depend on the order surefire runs classes in.
 */
@EnabledOnOs(OS.WINDOWS)
public class LinuxSystemWindowsRefusalTest {

    @Test
    public void anIlp32LongIsRefusedOnceAndRememberedAfter() {
        UnsatisfiedLinkError first = assertThrows(UnsatisfiedLinkError.class, LinuxSystemShim::loadLibrary);
        assertTrue(mentions(first, "C long is 4 bytes"), () -> "the reason, in the message or a cause: " + first);

        UnsatisfiedLinkError second = assertThrows(UnsatisfiedLinkError.class, LinuxSystemShim::loadLibrary);
        assertTrue(second.getMessage().contains("libc is not bound"), second.getMessage());
        assertNotNull(second.getCause(), "the NoClassDefFoundError is the cause");

        assertFalse(LinuxSystemShim.isLibraryLoaded());

        UnsatisfiedLinkError errno = assertThrows(UnsatisfiedLinkError.class, LinuxSystemShim::errno);
        assertTrue(errno.getMessage().contains("libc is not bound"), errno.getMessage());

        UnsatisfiedLinkError call = assertThrows(UnsatisfiedLinkError.class, () -> LinuxSystemShim.close(-1));
        assertTrue(call.getMessage().contains("libc is not bound"), call.getMessage());
    }

    /**
     * Whether {@code text} is in the message of {@code t} or of a cause: the NoClassDefFoundError of a repeat
     * attempt names the original error in its cause chain.
     */
    private static boolean mentions(Throwable t, String text) {
        for (Throwable cause = t; cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null && cause.getMessage().contains(text)) {
                return true;
            }
        }
        return false;
    }
}
