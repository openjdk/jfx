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
package com.sun.scenario.animation.shared;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TimelineHelper;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class TimelineClipCoreTest {
    private Timeline timeline;

    private KeyFrame start;
    private KeyFrame middle;
    private KeyFrame end;
    private IntegerProperty target;

    private TimelineClipCore core;

    private boolean tmpBool;

    @Before
    public void setUp() {
        target = new SimpleIntegerProperty();

        start = new KeyFrame(Duration.ZERO, new KeyValue(target, 10));
        middle = new KeyFrame(new Duration(500));
        end = new KeyFrame(new Duration(1000), new KeyValue(target, 20));

        timeline = new Timeline();
        timeline.getKeyFrames().setAll(start, middle, end);
        timeline.setRate(1.0);
        timeline.setCycleCount(1);
        timeline.setAutoReverse(false);
        core = TimelineHelper.getClipCore(timeline);
    }

    @Test
    public void testPlayTo() {
        //forward
        timeline.play();
        timeline.pause();
        core.playTo(6 * 500);
        assertEquals(15, target.get());

        //to the end
        core.playTo(6 * 1000);
        assertEquals(20, target.get());

        //backwards
        core.playTo(6 * 200);
        assertEquals(12, target.get());

        //back to start
        core.playTo(0);
        assertEquals(10, target.get());

        //catching up
        tmpBool = false;
        final KeyFrame newMiddle = new KeyFrame(
                Duration.millis(500),
                new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        tmpBool = true;
                    }
                });
        timeline.getKeyFrames().set(1, newMiddle);

        core.playTo(6 * 1000);
        assertEquals(20, target.get());
        assertTrue(tmpBool);

//        //visit last
//        core.start();
//        tmpBool = false;
//        end.setCanSkip(true);
//        end.setAction(new Runnable() {
//
//            @Override
//            public void run() {
//                tmpBool = true;
//            }
//        });
//
//        core.playTo(1000, true, true);
//        assertTrue(tmpBool);
    }

    @Test
    public void testPlayTo_ThrowsException() {
        final PrintStream defaultErrorStream = System.err;
        final PrintStream nirvana = new PrintStream(new OutputStream() {
            @Override
            public void write(int i) throws IOException {
            }
        });
        final OnFinishedExceptionListener eventHandler = new OnFinishedExceptionListener() ;
        start = new KeyFrame(Duration.ZERO, eventHandler);
        middle = new KeyFrame(new Duration(500), eventHandler);
        end = new KeyFrame(new Duration(1000), eventHandler);
        timeline.getKeyFrames().setAll(start, middle, end);

        try {
            System.setErr(nirvana);
        } catch (SecurityException ex) {
            // ignore
        }
        timeline.play();
        timeline.pause();
        core.playTo(6 * 100);
        try {
            System.setErr(defaultErrorStream);
        } catch (SecurityException ex) {
            // ignore
        }
        assertEquals(1, eventHandler.callCount);

        try {
            System.setErr(nirvana);
        } catch (SecurityException ex) {
            // ignore
        }
        core.playTo(6 * 600);
        try {
            System.setErr(defaultErrorStream);
        } catch (SecurityException ex) {
            // ignore
        }
        assertEquals(2, eventHandler.callCount);

        try {
            System.setErr(nirvana);
        } catch (SecurityException ex) {
            // ignore
        }
        core.playTo(6 * 1000);
        try {
            System.setErr(defaultErrorStream);
        } catch (SecurityException ex) {
            // ignore
        }
        assertEquals(3, eventHandler.callCount);
    }

    @Ignore
    @Test
    public void testJumpTo() {
    	// jumpTo on stopped timeline
        tmpBool = false;
        final KeyFrame newMiddle = new KeyFrame(
                Duration.millis(500),
                new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                tmpBool = true;
            }
        });
        timeline.getKeyFrames().set(1, newMiddle);

        core.jumpTo(6 * 600, false);
        assertEquals(0, target.get());
        assertFalse(tmpBool);
        
        // jumpTo on paused timeline
        tmpBool = false;
        timeline.play();
        timeline.pause();
        core.jumpTo(6 * 400, false);
        assertEquals(14, target.get());
        assertFalse(tmpBool);
    }

    private static class OnFinishedExceptionListener implements EventHandler<ActionEvent> {

        private int callCount = 0;

        @Override
        public void handle(ActionEvent event) {
            callCount++;
            throw new RuntimeException("Test Exception");
        }

    }
    
}
