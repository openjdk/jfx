/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

import javafx.embed.swt.FXCanvas;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class FXCanvasGestureEventsTest {

    static final String instructions =
            "This tests that SWT gesture events are properly transferred to JavaFX. " +
                    "It passes if proper event sequences for ZOOM (Mac, Windows), ROTATE (Mac, Windows), SCROLL (Mac, Windows)" +
                    "are printed to the console (SWT does not support any gestures on Linux yet, and SWIPE does not seem to be supported any more):\n\n " +
                    " 1) Perform a simple ZOOM gesture and observe that a 'ZoomStarted (Zoom)+ ZoomFinished' event sequence is printed out. The finish event should provide a zoom of '1.0' and a totalZoom that corresponds to the product of the zoom values of all preceding events.\n\n" +
                    " 2) Perform a simple ROTATE gesture and observe that a 'RotationStarted (Rotate)+ RotationFinished' event sequence is printed out. The finish event should provide an angle of '0.0' and a totalAngle that corresponds to the sum of the angle values of all preceding events. The rotation angle values should be positive when rotating clockwise and negative when rotating counter-clockwise. Note that with SWT < 3.8, rotate values will all be zero on MacOS 64-bit due to https://bugs.eclipse.org/bugs/show_bug.cgi?id=349812.\n\n" +
                    " 3) Perform a complex ROTATE-ZOOM gesture (start with ROTATE then continue with ZOOM) and observe that a 'RotationStarted (Rotate)+ ZoomStarted (Zoom | Rotate)+ ZoomFinished (Rotate)* RotationFinished' event sequence is printed out.\n\n" +
                    " 4) Perform a complex ZOOM-ROTATE gesture (start with ZOOM then continue with ROTATE) and observe that a 'ZoomStarted (Zoom)+ RotationStarted (Zoom | Rotate)+ RotationFinished (Zoom)* ZoomFinished' event sequence is printed out.\n\n" +
                    " 5) Perform a simple vertical SCROLL gesture and observe that a 'ScrollStarted (Scroll)+ ScrollFinished (Scroll)*' event sequence is printed out. The finish event should provide a scrollY value of 0 and a totalScrollY value that corresponds to the sum of the scrollY events of all preceding events. The scroll events that occur after the finish event should have inertia set to true, while all others should have set inertia to false.\n\n" +
                    " 6) Perform a simple horizontal SCROLL gesture and observe that a 'ScrollStarted (Scroll)+ ScrollFinished (Scroll)*' event sequence is printed out. The finish event should provide a scrollX value of 0 and a totalScrollX value that corresponds to the sum of the scrollX events of all preceding events. The scroll events that occur after the finish event should have inertia set to true, while all others should have set inertia to false.\n\n";

    private static TextArea createInfo(String msg) {
        TextArea t = new TextArea(msg);
        t.setWrapText(true);
        t.setEditable(false);
        return t;
    }

    public static void main(String[] args) {
        final Display display = new Display();
        final Shell shell = new Shell(display);
        shell.setText("FXCanvasGestureEventsTest");
        shell.setSize(1000, 500);
        shell.setLayout(new FillLayout());
        final FXCanvas canvas = new FXCanvas(shell, SWT.NONE);
        shell.open();

        // create and hook scene
        TextArea info = createInfo(instructions);
        AnchorPane root = new AnchorPane();
        root.getChildren().add(info);
        AnchorPane.setBottomAnchor(info, 0d);
        AnchorPane.setTopAnchor(info, 0d);
        AnchorPane.setLeftAnchor(info, 0d);
        AnchorPane.setRightAnchor(info, 0d);
        final Scene scene = new Scene(root, 600, 400);
        canvas.setScene(scene);

        final int[] zoomEventCount = {0, 0, 0};
        root.setOnZoomStarted(zoomEvent -> {
            System.out.println("ZoomStarted event #" + zoomEventCount[0]++ + ": zoom: " + zoomEvent.getZoomFactor() + ", totalZoom: " + zoomEvent.getTotalZoomFactor());
        });
        root.setOnZoom(zoomEvent -> {
            System.out.println("Zoom event #" + zoomEventCount[1]++ + ": zoom: " + zoomEvent.getZoomFactor() + ", totalZoom: " + zoomEvent.getTotalZoomFactor());
        });
        root.setOnZoomFinished(zoomEvent -> {
            System.out.println("ZoomFinished event #" + zoomEventCount[2]++ + ": zoom: " + zoomEvent.getZoomFactor() + ", totalZoom: " + zoomEvent.getTotalZoomFactor());
        });

        final int[] scrollEventCount = {0, 0, 0};
        root.setOnScrollStarted(scrollEvent -> {
            System.out.println("ScrollStarted event #" + scrollEventCount[0]++ + ": scrollX: " + scrollEvent.getDeltaX() + ", scrollY: " + scrollEvent.getDeltaY() + ", totalScrollX: " + scrollEvent.getTotalDeltaX() + ", totalScrollY: " + scrollEvent.getTotalDeltaY());
        });
        root.setOnScroll(scrollEvent -> {
            System.out.println("Scroll event #" + scrollEventCount[1]++ + ": scrollX: " + scrollEvent.getDeltaX() + ", scrollY: " + scrollEvent.getDeltaY() + ", totalScrollX: " + scrollEvent.getTotalDeltaX() + ", totalScrollY: " + scrollEvent.getTotalDeltaY() + ", inertia: " + scrollEvent.isInertia());
        });
        root.setOnScrollFinished(scrollEvent -> {
            System.out.println("ScrollFinished event #" + scrollEventCount[2]++ + ": scrollX: " + scrollEvent.getDeltaX() + ", scrollY: " + scrollEvent.getDeltaY() + ", totalScrollX: " + scrollEvent.getTotalDeltaX() + ", totalScrollY: " + scrollEvent.getTotalDeltaY());
        });

        final int[] rotateEventCount = {0, 0, 0};
        root.setOnRotationStarted(rotateEvent -> {
            System.out.println("RotationStarted event #" + rotateEventCount[0]++ + ": angle: " + rotateEvent.getAngle() + ", totalAngle: " + rotateEvent.getTotalAngle());
        });
        root.setOnRotate(rotateEvent -> {
            System.out.println("Rotate event #" + rotateEventCount[0]++ + ": angle: " + rotateEvent.getAngle() + ", totalAngle: " + rotateEvent.getTotalAngle());
        });
        root.setOnRotationFinished(rotateEvent -> {
            System.out.println("RotationFinished event #" + rotateEventCount[0]++ + ": angle: " + rotateEvent.getAngle() + ", totalAngle: " + rotateEvent.getTotalAngle());
        });

        final int[] swipeEventCount = {0};
        root.setOnSwipeDown(swipeEvent -> {
            System.out.println("Swipe DOWN event #" + swipeEventCount[0]++);
        });
        root.setOnSwipeUp(swipeEvent -> {
            System.out.println("Swipe UP event #" + swipeEventCount[0]++);
        });
        root.setOnSwipeLeft(swipeEvent -> {
            System.out.println("Swipe LEFT event #" + swipeEventCount[0]++);
        });
        root.setOnSwipeRight(swipeEvent -> {
            System.out.println("Swipe RIGHT event #" + swipeEventCount[0]++);
        });

        while (!shell.isDisposed()) {
            // run SWT event loop
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();
    }
}
