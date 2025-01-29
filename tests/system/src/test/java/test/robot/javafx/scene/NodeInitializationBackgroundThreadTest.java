/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
package test.robot.javafx.scene;

import static org.junit.jupiter.api.Assertions.fail;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.skin.AccordionSkin;
import javafx.scene.control.skin.DatePickerSkin;
import javafx.scene.control.skin.LabelSkin;
import javafx.scene.control.skin.TextAreaSkin;
import javafx.scene.control.skin.TextFieldSkin;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import test.robot.testharness.RobotTestBase;

/**
 * Tests Node initialization from a background thread, per
 *
 * https://openjfx.io/javadoc/23/javafx.graphics/javafx/scene/Node.html
 *
 * "Node objects may be constructed and modified on any thread as long they are not yet attached to a Scene in a Window
 * that is showing. An application must attach nodes to such a Scene or modify them on the JavaFX Application Thread."
 */
public class NodeInitializationBackgroundThreadTest extends RobotTestBase {
    private static final int THREAD_COUNT = 100;
    private static final int DURATION = 2000;
    private static final AtomicLong seq = new AtomicLong();
    private static final AtomicBoolean failed = new AtomicBoolean();

    @Test
    public void accordion() {
        test(() -> {
            Accordion c = new Accordion();
            c.setSkin(new AccordionSkin(c));
            c.getPanes().add(new TitledPane("Accordion", new BorderPane()));
            return c;
        }, (c) -> {
            c.prefHeight(-1);
            c.prefWidth(-1);
        });
    }

    @Disabled("JDK-8349004") // FIX
    @Test
    public void datePicker() {
        test(() -> {
            DatePicker c = new DatePicker();
            c.setSkin(new DatePickerSkin(c));
            return c;
        }, (c) -> {
            c.show(); // fails here
            c.setValue(LocalDate.now());
            c.prefHeight(-1);
            c.setValue(LocalDate.EPOCH);
            c.prefWidth(-1);
        });
    }

    @Test
    public void passwordField() {
        test(() -> {
            PasswordField c = new PasswordField();
            c.setSkin(new TextFieldSkin(c));
            return c;
        }, (c) -> {
            // could not get it to fail
            access(c);
            c.setAlignment(Pos.CENTER);
            c.getCharacters();
        });
    }

    @Disabled("JDK-8347392") // FIX
    @Test
    public void textArea() {
        test(() -> {
            TextArea c = new TextArea();
            c.setSkin(new TextAreaSkin(c));
            return c;
        }, (c) -> {
            access(c);
        });
    }

    @Test
    public void textField() {
        test(() -> {
            TextField c = new TextField();
            c.setSkin(new TextFieldSkin(c));
            return c;
        }, (c) -> {
            // could not get it to fail
            access(c);
            c.setAlignment(Pos.CENTER);
            c.getCharacters();
        });
    }

    @Disabled("JDK-8348100") // FIX
    @Test
    public void tooltip() {
        test(() -> {
            Tooltip t = new Tooltip("this is a tooltip");
            t.setShowDelay(Duration.ZERO);
            t.setHideDelay(Duration.ZERO);
            Label c = new Label("testing tooltip");
            c.setSkin(new LabelSkin(c));
            c.setTooltip(t);
            return c;
        }, (c) -> {
            Tooltip t = c.getTooltip();
            t.isShowing();
            t.setGraphic(new Label("yo!"));
            if (Platform.isFxApplicationThread()) {
                Point2D p = c.localToScreen(c.getWidth() / 2.0, c.getHeight() / 2.0);
                robot.mouseMove(p);
            }
        });
    }

    private void access(TextInputControl c) {
        c.setPrefWidth(20);
        c.setPromptText("yo");
        c.setText(nextString());
        c.prefHeight(-1);
        c.getControlCssMetaData();
    }

    private <T extends Node> void test(Supplier<T> generator, Consumer<T> operation) {
        T visibleNode = generator.get();
        String title = visibleNode.getClass().getSimpleName();
        
        setContent(visibleNode);
        setTitle(title);

        AtomicBoolean running = new AtomicBoolean(true);
        CountDownLatch counter = new CountDownLatch(THREAD_COUNT);

        try {
            for (int i = 0; i < THREAD_COUNT; i++) {
                new Thread(title) {
                    @Override
                    public void run() {
                        try {
                            T n = generator.get();
                            int count = 0;
                            while (running.get()) {
                                operation.accept(n);
    
                                count++;
                                if ((count % 100) == 0) {
                                    inFx(() -> {
                                        operation.accept(visibleNode);
                                    });
                                }
                            }
                        } finally {
                            counter.countDown();
                        }
                    }
                }.start();
            }

            sleep(DURATION);
        } finally {
            running.set(false);
        }

        // let them finish
        try {
            counter.await(500, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e);
        }
    }

    private static String nextString() {
        long ix = seq.incrementAndGet();
        if ((ix % 10) == 0) {
            return null;
        }
        return "_a" + ix + "\nyo!";
    }

    @BeforeAll
    public static void beforeAll() {
        // this might be made a part of the base class (RobotTestBase)
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
            failed.set(true);
            // we could also accumulate stack trace(s) and send them to fail() in afterEach()
        });
    }

    @BeforeEach
    public void beforeEach() {
        failed.set(false);
    }

    @AfterEach
    public void afterEach() {
        if (failed.get()) {
            fail();
        }
    }
}
