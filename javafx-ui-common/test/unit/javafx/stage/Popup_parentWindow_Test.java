/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.stage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sun.javafx.test.PropertiesTestBase;
import com.sun.javafx.test.objects.TestGroup;
import com.sun.javafx.test.objects.TestNode;
import com.sun.javafx.test.objects.TestScene;
import com.sun.javafx.test.objects.TestStage;

@Ignore ("This test is basically invalidated with the new design and needs to be rewritten")
@RunWith(Parameterized.class)
public final class Popup_parentWindow_Test extends PropertiesTestBase {

    @Parameters
    public static Collection<Object> data() {
        final List<Object> configurations = new ArrayList<Object>();

        TestObjects to;

        to = new TestObjects();
        configurations.add(
                config(to.testPopup,
                       "owner", to.testStage1, to.testStage2));

        to = new TestObjects();
        configurations.add(
                config(to.testPopup,
                       "owner", to.testScene1, to.testScene2));

        to = new TestObjects();
        configurations.add(
                config(to.testPopup,
                       "owner", to.testRoot1, to.testRoot2));

        to = new TestObjects();
        configurations.add(
                config(to.testPopup,
                       "owner", to.testNode1, to.testNode2));

//        to = new TestObjects();
//        to.testPopup.setParent(to.testScene1);
//        configurations.add(
//                config(to.testScene1,
//                       "_window", to.testStage1, to.testStage2,
//                       to.testPopup,
//                       "parentWindow", to.testStage1, to.testStage2));
//
//        to = new TestObjects();
//        to.testPopup.setParent(to.testNode1);
//        configurations.add(
//                config(to.testScene1,
//                       "_window", to.testStage1, to.testStage2,
//                       to.testPopup,
//                       "parentWindow", to.testStage1, to.testStage2));

//        Configuration extcfg;
//
//        to = new TestObjects();
//        to.testPopup.setParent(to.testNode1);
//        extcfg = new Configuration(to.testRoot1,
//                                   "_scene", to.testScene1, to.testScene2,
//                                   to.testPopup,
//                                   "parentWindow", to.testStage1,
//                                                   to.testStage2);
//        extcfg.setAllowMultipleNotifications(true);
//        configurations.add(new Object[] { extcfg });

//        to = new TestObjects();
//        to.testPopup.setParent(to.testNode1);
//        extcfg = new Configuration(to.testNode1,
//                                   "_parent", to.testRoot1, to.testRoot2,
//                                   to.testPopup,
//                                   "parentWindow", to.testStage1,
//                                                   to.testStage2);
//        extcfg.setAllowMultipleNotifications(true);
//        configurations.add(new Object[] { extcfg });

        return configurations;
    }

    public Popup_parentWindow_Test(final Configuration configuration) {
        super(configuration);

    }

    private static final class TestObjects {
        public final Popup testPopup;
        public final TestNode testNode1;
        public final TestNode testNode2;
        public final TestGroup testRoot1;
        public final TestGroup testRoot2;
        public final TestScene testScene1;
        public final TestScene testScene2;
        public final TestStage testStage1;
        public final TestStage testStage2;

        public TestObjects() {
            testRoot1 = new TestGroup("ROOT_1");
            testRoot2 = new TestGroup("ROOT_2");

            testNode1 = new TestNode("NODE_1");
            testNode2 = new TestNode("NODE_2");

            testRoot1.getChildren().add(testNode1);
            testRoot2.getChildren().add(testNode2);

            testScene1 = new TestScene("SCENE_1", testRoot1);
            testScene2 = new TestScene("SCENE_2", testRoot2);

            testStage1 = new TestStage("STAGE_1");
            testStage2 = new TestStage("STAGE_2");

            testStage1.setScene(testScene1);
            testStage2.setScene(testScene2);

            testPopup = new Popup();
        }

    }
}
