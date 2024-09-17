/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
package test.robot.com.sun.glass.ui.monocle;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Researching various aspects of JUnit4 -> JUnit5 migration.
 */
@ExtendWith(TestJUnit5Migration.Watcher.class)
public class TestJUnit5Migration {
    private String testName;
    
    private static List<String> parameters() {
        return List.of("1", "2", "3");
    }
    
    // gets test name from the junit5 system
    @BeforeEach
    void getTestName(TestInfo t) {
        testName = t.getDisplayName();
        System.out.println("@@@@ getTestName " + testName); // FIX
    }

    // @BeforeEach
    // junit5 does not support parameterized class-level tests yet
    protected void init(String arg) throws Exception {
        System.out.println("@@@@ createDevice " + testName); // FIX
    }

//    @ParameterizedTest
//    @MethodSource("parameters")
//    void test(String args) {
//        System.out.println("@@@@ test " + testName); // FIX
//    }
    
    @ParameterizedTest
    @MethodSource("parameters")
    void testAssumeTrue(String args) {
        System.out.println("@@@@ test " + testName); // FIX
        Assumptions.assumeTrue(true);
    }
    
    @ParameterizedTest
    @MethodSource("parameters")
    void testAssumeFalse(String args) {
        System.out.println("@@@@ test " + testName); // FIX
        Assumptions.assumeTrue(false);
    }

    public static class Watcher implements TestWatcher {
        @Override
        public void testFailed(ExtensionContext cx, Throwable err) {
            System.out.println("@@@@ testFailed cx=" + cx + " err=" + err); // FIX
        }
        
        @Override
        public void testSuccessful(ExtensionContext cx) {
            System.out.println("@@@@ testSuccessful cx=" + cx); // FIX
        }
        
        @Override
        public void testDisabled(ExtensionContext cx, Optional<String> err) {
            System.out.println("@@@@ testFailed cx=" + cx + " err=" + err); // FIX
        }
    }
}
