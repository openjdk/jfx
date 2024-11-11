/*
 * Copyright (c) 2014, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collection;
import javafx.scene.Node;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.InputEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.sun.glass.ui.monocle.TestLogShim;
import test.com.sun.glass.ui.monocle.TestRunnable;
import test.robot.com.sun.glass.ui.monocle.input.devices.TestTouchDevice;
import test.robot.com.sun.glass.ui.monocle.input.devices.TestTouchDevices;

public final class DragAndDropTest extends ParameterizedTestBase {

    private static Collection<TestTouchDevice> parameters() {
        return TestTouchDevices.getTouchDeviceParameters(1);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testDragOneNodeToAnother(TestTouchDevice device) throws Exception {
        createDevice(device, null);
        TestRunnable.invokeAndWait(() -> {
            Node n1 = new Rectangle(10, 10, 10, 10);
            Node n2 = new Rectangle(210, 10, 10, 10);
            TestApplication.getRootGroup().getChildren().add(n1);
            TestApplication.getRootGroup().getChildren().add(n2);
            n1.setOnDragDetected((event) -> {
                TestLogShim.log("Drag detected on n1");
                Dragboard db = n1.startDragAndDrop(TransferMode.ANY);
                ClipboardContent content = new ClipboardContent();
                content.putString("");
                db.setContent(content);
            });
            n2.setOnDragEntered((e) -> TestLogShim.log("Drag entered on n2"));
            n2.setOnDragOver((event) -> {
                TestLogShim.log("Drag over on n2");
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            });
            n2.setOnDragDropped((e) -> TestLogShim.log("Drag dropped on n2"));
            n1.setOnDragDone((e) -> TestLogShim.log("Drag done on n1"));
            n1.addEventHandler(InputEvent.ANY, (e) -> TestLogShim.log(e.toString()));
            n2.addEventHandler(InputEvent.ANY, (e) -> TestLogShim.log(e.toString()));
        });
        try {
            int p = device.addPoint(15, 15);
            device.sync();
            device.setPoint(p, 110, 15);
            device.sync();
            TestLogShim.waitForLogContaining("Drag detected on n1");
            TestLogShim.clear();
            device.setPoint(p, 215, 15);
            device.sync();
            TestLogShim.waitForLogContaining("Drag entered on n2");
            TestLogShim.waitForLogContaining("Drag over on n2");
            device.removePoint(p);
            device.sync();
            TestLogShim.waitForLogContaining("Drag dropped on n2");
            TestLogShim.waitForLogContaining("Drag done on n1");
        } finally {
            TestRunnable.invokeAndWait(() -> TestApplication.getRootGroup().getChildren().clear());
        }
    }
}
