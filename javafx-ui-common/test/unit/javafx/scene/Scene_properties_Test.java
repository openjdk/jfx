/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene;

import com.sun.javafx.cursor.CursorFrame;
import com.sun.javafx.event.EventHandlerManager;
import java.util.Arrays;
import java.util.Collection;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.NodeOrientation;
import javafx.scene.paint.Color;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sun.javafx.test.PropertiesTestBase;
import com.sun.javafx.test.objects.TestScene;
import com.sun.javafx.test.objects.TestStage;
import javafx.scene.layout.Pane;

@RunWith(Parameterized.class)
public final class Scene_properties_Test extends PropertiesTestBase {
    @Parameters
    public static Collection data() {
        final TestScene testScene = new TestScene(new Group());

        final TestStage testStage1 = new TestStage("STAGE_1");
        final TestStage testStage2 = new TestStage("STAGE_2");
        
        final EventHandler testEventHandler =
                new EventHandler<Event>() {
                    @Override
                    public void handle(Event event) {
                    }
                };
        
        return Arrays.asList(new Object[] {
            config(testScene,
                   "_window", testStage1, testStage2,
                   "window", testStage1, testStage2),
//            config(testScene, "x", , ),
//            config(testScene, "y", , ),
//            config(testScene, "width", , ),
//            config(testScene, "height", , ),
            config(testScene, "camera", null, new ParallelCamera()),
            config(testScene, "fill", Color.WHITE, Color.BLACK),
            config(testScene, "fill", null, Color.TRANSPARENT),
            config(testScene, "root", new Group(), new Pane()),
            config(testScene, "cursor", null, new Cursor("TestCursor") {
                @Override public CursorFrame getCurrentFrame() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            }),
            config(testScene, "cursor", Cursor.DEFAULT, Cursor.CROSSHAIR),
            config(testScene, "eventDispatcher",
                   null,
                   new EventHandlerManager(null)),
            config(testScene, "camera", null, new PerspectiveCamera()),
            config(testScene, "onMouseClicked", null, testEventHandler),
            config(testScene, "onMouseDragged", null, testEventHandler),
            config(testScene, "onMouseEntered", null, testEventHandler),
            config(testScene, "onMouseExited", null, testEventHandler),
            config(testScene, "onMouseMoved", null, testEventHandler),
            config(testScene, "onMousePressed", null, testEventHandler),
            config(testScene, "onMouseReleased", null, testEventHandler),
            config(testScene, "onDragDetected", null, testEventHandler),
            config(testScene, "onDragEntered", null, testEventHandler),
            config(testScene, "onDragExited", null, testEventHandler),
            config(testScene, "onDragOver", null, testEventHandler),
            config(testScene, "onDragDropped", null, testEventHandler),
            config(testScene, "onDragDone", null, testEventHandler),
            config(testScene, "onKeyPressed", null, testEventHandler),
            config(testScene, "onKeyReleased", null, testEventHandler),
            config(testScene, "onKeyTyped", null, testEventHandler),
            config(testScene, "onContextMenuRequested", null, testEventHandler),
            config(testScene, "onDragDetected", null, testEventHandler),
            config(testScene, "onDragDone", null, testEventHandler),
            config(testScene, "onDragDropped", null, testEventHandler),
            config(testScene, "onDragEntered", null, testEventHandler),
            config(testScene, "onDragExited", null, testEventHandler),
            config(testScene, "onDragOver", null, testEventHandler),
            config(testScene, "onMouseDragEntered", null, testEventHandler),
            config(testScene, "onMouseDragExited", null, testEventHandler),
            config(testScene, "onMouseDragOver", null, testEventHandler),
            config(testScene, "onMouseDragReleased", null, testEventHandler),
            config(testScene, "onRotate", null, testEventHandler),
            config(testScene, "onRotationStarted", null, testEventHandler),
            config(testScene, "onRotationFinished", null, testEventHandler),
            config(testScene, "onZoom", null, testEventHandler),
            config(testScene, "onZoomStarted", null, testEventHandler),
            config(testScene, "onZoomFinished", null, testEventHandler),
            config(testScene, "onScroll", null, testEventHandler),
            config(testScene, "onScrollStarted", null, testEventHandler),
            config(testScene, "onScrollFinished", null, testEventHandler),
            config(testScene, "onSwipeLeft", null, testEventHandler),
            config(testScene, "onSwipeRight", null, testEventHandler),
            config(testScene, "onSwipeUp", null, testEventHandler),
            config(testScene, "onSwipeDown", null, testEventHandler),
            config(testScene, "onTouchPressed", null, testEventHandler),
            config(testScene, "onTouchReleased", null, testEventHandler),
            config(testScene, "onTouchMoved", null, testEventHandler),
            config(testScene, "onTouchStationary", null, testEventHandler),
            config(testScene, "onInputMethodTextChanged",
                   null, testEventHandler),
            config(testScene,
                   "nodeOrientation", NodeOrientation.INHERIT,
                                      NodeOrientation.RIGHT_TO_LEFT,
                   "effectiveNodeOrientation", NodeOrientation.LEFT_TO_RIGHT,
                                               NodeOrientation.RIGHT_TO_LEFT)
        });
    }

    public Scene_properties_Test(final Configuration configuration) {
        super(configuration);
    }
}
