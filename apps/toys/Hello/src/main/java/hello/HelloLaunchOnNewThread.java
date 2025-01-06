/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

package hello;

import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class HelloLaunchOnNewThread extends Application {

    static long startTime;
    static AtomicBoolean mainCalled = new AtomicBoolean(false);

    public HelloLaunchOnNewThread() {
        long endTime = System.nanoTime();
        long elapsedMsec = (endTime - startTime + 500000) / 1000000;
        System.err.println("DONE: elapsed time = " + elapsedMsec + " msec");
        System.err.println("Constructor: currentThread="
                + Thread.currentThread().getName());
        if (!mainCalled.get()) {
            System.err.println("***************************************");
            System.err.println("*** ERROR: main() method not called ***");
            System.err.println("***************************************");
        }
    }

    @Override public void init() {
        System.err.println("init: currentThread="
                + Thread.currentThread().getName());
    }

    @Override public void start(Stage stage) {
        System.err.println("start: currentThread="
                + Thread.currentThread().getName());

        stage.setTitle("Launch from New Thread");

        Group root = new Group();
        Scene scene = new Scene(root, 600, 450);
        scene.setFill(Color.LIGHTGREEN);

        Rectangle rect = new Rectangle();
        rect.setX(25);
        rect.setY(40);
        rect.setWidth(100);
        rect.setHeight(50);
        rect.setFill(Color.RED);

        root.getChildren().add(rect);
        stage.setScene(scene);
        stage.show();
        System.err.println("You should now see the 'HelloWorld' rectangle in the window");
    }

    @Override public void stop() {
        System.err.println("stop: currentThread="
                + Thread.currentThread().getName());
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        mainCalled.set(true);
        System.err.println("main: currentThread="
                + Thread.currentThread().getName());
        new Thread(() -> {
            // Sleep for a very short time to ensure main thread exits,
            // since that will provoke JDK-8109685
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {}
            System.err.println("Calling Application.launch from currentThread="
                    + Thread.currentThread().getName());
            System.err.print("LAUNCHING...");
            System.err.flush();
            startTime = System.nanoTime();
            Application.launch(HelloLaunchOnNewThread.class, args);
            System.err.println("Application.launch returns");
        }).start();
        System.err.println("Main thread exiting: currentThread="
                + Thread.currentThread().getName());
    }
}
