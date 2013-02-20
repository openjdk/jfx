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

package javafx.scene;


import com.sun.javafx.test.BuilderTestBase;

import java.util.Arrays;
import java.util.Collection;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventDispatcher;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.BoxBlur;
import javafx.scene.input.InputMethodRequests;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public final class Node_builder_Test extends BuilderTestBase {
    @Parameters
    public static Collection data() {
        BuilderTestBase.Configuration cfg = new BuilderTestBase.Configuration(Rectangle.class);

        cfg.addProperty("blendMode", BlendMode.ADD);
        cfg.addProperty("cache", true);
        cfg.addProperty("cacheHint", CacheHint.QUALITY);
        cfg.addProperty("clip", new Rectangle());
        cfg.addProperty("cursor", Cursor.HAND);
        cfg.addProperty("depthTest", DepthTest.ENABLE);
        cfg.addProperty("disable", true);
        cfg.addProperty("effect", new BoxBlur());
        cfg.addProperty("eventDispatcher", new EventDispatcher() {
            @Override
            public Event dispatchEvent(Event event, EventDispatchChain tail) {
                return null;
            }
        });
        cfg.addProperty("focusTraversable", true);
        cfg.addProperty("id", "ID");
        cfg.addProperty("inputMethodRequests", new InputMethodRequests() {
            @Override
            public Point2D getTextLocation(int i) {
                return null;
            }
            @Override
            public int getLocationOffset(int i, int i1) {
                return 0;
            }
            @Override
            public void cancelLatestCommittedText() {
            }
            @Override
            public String getSelectedText() {
                return "";
            }
        });
        cfg.addProperty("layoutX", 1.0);
        cfg.addProperty("layoutY", 2.0);
        cfg.addProperty("managed", true);
        cfg.addProperty("mouseTransparent", true);
        cfg.addProperty("onContextMenuRequested", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onDragDetected", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onDragDone", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onDragDropped", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onDragEntered", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onDragExited", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onDragOver", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onInputMethodTextChanged", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onKeyPressed", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onKeyReleased", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onKeyTyped", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onMouseClicked", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onMouseDragEntered", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onMouseDragExited", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onMouseDragged", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onMouseDragOver", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onMouseDragReleased", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onMouseEntered", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onMouseExited", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onMouseMoved", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onMousePressed", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onMouseReleased", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onRotate", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onRotationFinished", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onRotationStarted", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onScroll", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onScrollFinished", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onScrollStarted", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onSwipeDown", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onSwipeLeft", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onSwipeRight", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onSwipeUp", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onTouchMoved", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onTouchPressed", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onTouchReleased", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onTouchStationary", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onZoom", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onZoomFinished", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onZoomStarted", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("opacity", 0.7);
        cfg.addProperty("pickOnBounds", true);
        cfg.addProperty("rotate", 0.2);
        cfg.addProperty("scaleX", 1.0);
        cfg.addProperty("scaleY", 2.0);
        cfg.addProperty("scaleZ", 3.0);
        cfg.addProperty("style", "");
        cfg.addProperty("translateX", 1.0);
        cfg.addProperty("translateY", 1.0);
        cfg.addProperty("translateZ", 1.0);
        cfg.addProperty("userData", null);
        cfg.addProperty("visible", false);
        cfg.addProperty("styleClass", String.class, Arrays.asList(""));
        cfg.addProperty("transforms",  Arrays.asList(new Rotate()));

        return Arrays.asList(new Object[] {
            config(cfg)
        });
    }

    public Node_builder_Test(final Configuration configuration) {
        super(configuration);
    }
}
