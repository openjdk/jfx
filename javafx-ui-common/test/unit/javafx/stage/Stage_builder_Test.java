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

package javafx.stage;

import com.sun.javafx.test.BuilderTestBase;
import java.util.Arrays;
import java.util.Collection;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventDispatcher;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.TestImages;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public final class Stage_builder_Test extends BuilderTestBase {
    @Parameters
    public static Collection data() {
        BuilderTestBase.Configuration cfg = new BuilderTestBase.Configuration(Stage.class);

        cfg.addProperty("y", 2.0);
        cfg.addProperty("width", 3.0);
        cfg.addProperty("height", 4.0);
        cfg.addProperty("scene", new Scene(new Group()));
        cfg.addProperty("opacity", 0.4);
        cfg.addProperty("focused", false); // deprecated

        cfg.addProperty("eventDispatcher", new EventDispatcher() {
            @Override
            public Event dispatchEvent(Event event, EventDispatchChain tail) {
                return null;
            }
        });
        cfg.addProperty("onHidden", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onHiding", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onShowing", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onShown", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("onCloseRequest", new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
            }
        });
        cfg.addProperty("x", 1.0);

        cfg.addProperty("fullScreen", false);
        cfg.addProperty("iconified", false);
        cfg.addProperty("maximized", false);
        cfg.addProperty("minWidth", 1.0);
        cfg.addProperty("minHeight", 2.0);
        cfg.addProperty("maxWidth", 3.0);
        cfg.addProperty("maxHeight", 4.0);
        cfg.addProperty("resizable", false);
        cfg.addProperty("title", "Title");
        cfg.addProperty("icons", Image.class, Arrays.asList(TestImages.TEST_IMAGE_100x200));

        return Arrays.asList(new Object[] {
            config(cfg)
        });
    }

    public Stage_builder_Test(final Configuration configuration) {
        super(configuration);
    }
}
