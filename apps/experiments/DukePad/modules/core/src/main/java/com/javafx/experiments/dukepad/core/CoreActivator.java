/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.javafx.experiments.dukepad.core;

import javafx.animation.AnimationTimer;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.TouchPoint;
import com.sun.javafx.perf.PerformanceTracker;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;

/**
 * CoreActivator
 */
public class CoreActivator implements BundleActivator {
    private static final boolean PRINT_FPS = Boolean.getBoolean("printFPS");
    private Root root;

    // =================================================================================================================
    // BundleActivator Methods

    // Not sure what happens if start is called twice. Is this even possible?
    @Override public void start(final BundleContext bundleContext) throws Exception {
        // Get Scene Service
        Scene scene = bundleContext.getService(bundleContext.getServiceReference(Scene.class));
        // Load Theme CSS
        scene.getStylesheets().add(CoreActivator.class.getResource("/DukePadTheme.css").toExternalForm());
        root = new Root(scene);
        bundleContext.addServiceListener(serviceEvent -> {
            // Application has come or gone
            final DukeApplication app = (DukeApplication)bundleContext.getService(serviceEvent.getServiceReference());
            switch (serviceEvent.getType()) {
                case ServiceEvent.REGISTERED: root.add(app); break;
                case ServiceEvent.UNREGISTERING: root.remove(app); break;
            }
        }, "("+ Constants.OBJECTCLASS+"=com.javafx.experiments.dukepad.core.DukeApplication)");

        // Register the core settings services
        bundleContext.registerService(Settings.class, new AppearanceSettings(root.getHomeScreen()), null);

        if (PRINT_FPS) {
            PerformanceTracker p = PerformanceTracker.getSceneTracker(scene);
            AnimationTimer tx = new AnimationTimer() {
                private long last = 0;
                @Override
                public void handle(long now) {
                    if ((now - last) > 5_000_000_000L) {
                        last = now;
                        System.out.println(p.getAverageFPS() + "fps");
                        p.resetAverageFPS();
                    }
                }
            };
            tx.start();
        }
    }

    @Override public void stop(BundleContext bundleContext) throws Exception {
        // stop current app
        final DukeApplication app = root.getAppContainer().getApplication();
        if(app != null) app.stopApp();
    }

    // =================================================================================================================

    // This isn't used but Jasper wanted to keep it around. Seems like it should maybe go in the LockScreen?
    public class FlickGestureHelper implements EventHandler {
        private final Node node;
        private final FlickGestureResultListener gestureListener;
        private final Point2D successVector;
        private final boolean acceptsFlick;
        private double startX, startY;

        public FlickGestureHelper(Node node,FlickGestureResultListener gestureListener, Point2D successVector, boolean acceptsFlick) {
            this.node = node;
            this.gestureListener = gestureListener;
            this.successVector = successVector;
            this.acceptsFlick = acceptsFlick;
            node.addEventHandler(Event.ANY,this);
        }

        /*
        drag less than threshhold(vector) = fail
        drag more than threshhold(vector) = success
        flick(fast drag) in direction of vector = success
         */

        public void dispose(){
            node.removeEventHandler(Event.ANY,this);
        }

        @Override public void handle(Event event) {
            double sceneX = 0, sceneY = 0;
            if (event.getEventType().getSuperType() == MouseEvent.ANY) {
                MouseEvent me = (MouseEvent)event;
                sceneX = me.getSceneX();
                sceneY = me.getSceneY();
            } else if (event.getEventType().getSuperType() == TouchEvent.ANY) {
                TouchPoint tp = ((TouchEvent)event).getTouchPoint();
                sceneX = tp.getSceneX();
                sceneY = tp.getSceneY();
            }
            double dragX = sceneX - startX;
            double dragY = sceneY - startY;
            if (event.getEventType() == MouseEvent.MOUSE_PRESSED || event.getEventType() == TouchEvent.TOUCH_PRESSED) {
                startX = sceneX;
                startY = sceneY;
            } else if (event.getEventType() == MouseEvent.MOUSE_DRAGGED || event.getEventType() == TouchEvent.TOUCH_MOVED) {
                gestureListener.dragged(dragX,dragY);
            } else if (event.getEventType() == MouseEvent.MOUSE_RELEASED || event.getEventType() == TouchEvent.TOUCH_RELEASED) {
                // detect if drag distance is more than vector
                if ((successVector.getX() == 0 || (dragX/successVector.getX()) >= 1) && ( successVector.getY() == 0 || (dragY/successVector.getY()) >= 1)) {
                    gestureListener.gestureComplete(true);
                } else {
                    gestureListener.gestureComplete(false);
                }
            }
        }
    }


    public interface FlickGestureResultListener {
        public void gestureComplete(boolean success);
        public void dragged(double distanceX, double distanceY);
    }
}
