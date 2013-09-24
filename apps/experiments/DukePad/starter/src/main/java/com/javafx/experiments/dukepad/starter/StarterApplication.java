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

package com.javafx.experiments.dukepad.starter;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import java.io.File;
import java.net.MalformedURLException;
import java.util.*;

/**
 * Starter for DukePad platform
 */
public class StarterApplication extends Application {
    private Stage primaryStage;
    private Scene scene;
    private final LinkedList<Runnable> loadingTasks = new LinkedList<>();
    private final List<Bundle> bundles = new ArrayList<>();

    public void start(Stage primaryStage) {
        // create main window and show splash screen
        this.primaryStage = primaryStage;
        ImageView bootScreen = new ImageView(new Image(StarterApplication.class.getResource("/Duke-Startup.jpg").toExternalForm(),false));
        Text loadingText = new Text("Loading...");
        loadingText.setFont(Font.font("System",24));
        loadingText.setFill(Color.rgb(255, 255, 255, 0.8));
        loadingText.setX(30);
        loadingText.setY(50);
        scene = new Scene(new Group(bootScreen,loadingText), 1280, 800);
//        scene.setFill(Color.BLACK);
//        scene.setFill(Color.TRANSPARENT);
        scene.setFill(Color.RED);
//        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setScene(scene);
        primaryStage.show();

        // boot OSGi in own thread
        Platform.runLater(() -> startOSGi(loadingText));
    }

    private void startOSGi(final Text loadingText) {
        try {
            // CONFIGURE OSGI
            String[] equinoxArgs = {"-clean","-consoleLog","-console","-dev","bin"};
            Map<String,String> initialProperties = new HashMap<>();
            initialProperties.put("osgi.parentClassloader","ext");
            initialProperties.put("org.osgi.framework.system.packages","sun.misc,java,javax,javax.net.ssl,javax.security.cert,javax.naming,javax.naming.directory,javax.security.auth,javax.security.auth.x500");
            EclipseStarter.setInitialProperties(initialProperties);
            // START OSGI
            final BundleContext context = EclipseStarter.startup(equinoxArgs, null);
            // REGISTER SCENE AND STAGE AS SERVICES
            context.registerService(Scene.class,scene, null);
            context.registerService(Stage.class,primaryStage, null);

            // Create tasks to install all jar bundles in bundles directory
            File bundleDir = new File("bundles");
            if (bundleDir.exists()) {
                File[] bundleJars = bundleDir.listFiles();
                if (bundleJars != null) {
                    // load app modules
                    for (final File file: bundleJars) {
                        if (file.getName().endsWith(".jar")) {
                            loadingTasks.add(() -> {
                                final String name = file.getName().substring(0,file.getName().lastIndexOf('.'));
                                loadingText.setText("Loading " + name);
                                System.out.println("Loading " + name);
                                try {
                                    bundles.add(context.installBundle(file.toURI().toURL().toExternalForm()));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                    }
                }
            }
            // Create task to start all bundles
            loadingTasks.add(() -> {
                for (Bundle b: bundles) {
                    loadingTasks.push(() -> {
                        loadingText.setText("Starting " + b.getSymbolicName());
                        System.out.println("Starting " + b.getSymbolicName());
                        try {
                            b.start();
                        } catch (BundleException e) {
                            e.printStackTrace();
                        }
                    });
                }
            });
            // Create task to install and start all bundles in apps directory
            File appDir = new File("apps");
            if (appDir.exists()) {
                File[] appJars = appDir.listFiles();
                if (appJars != null) {
                    // sort alphabetical
                    Arrays.sort(appJars,new Comparator<File>() {
                        @Override public int compare(File o1, File o2) {
                        return o1.getName().compareTo(o2.getName());
                        }
                    });
                    // load app modules
                    for (File file: appJars) {
                        if (file.getName().endsWith(".jar")) {
                            loadingTasks.add(() -> {
                                final String name = file.getName().substring(0,file.getName().lastIndexOf('.'));
                                loadingText.setText("Loading " + name);
                                System.out.println("Loading " + name);
                                try {
                                    Bundle bundle = context.installBundle(file.toURI().toURL().toExternalForm());
                                    bundle.start();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        // start executing all the tasks we have queued up
        runNextStartupTask();
    }

    /**
     * Execute each task from list in order in separate platform.runLater(). This allows FX to draw the screen
     * between tasks.
     */
    private void runNextStartupTask() {
        if (loadingTasks.isEmpty()) return;
        final Runnable task = loadingTasks.pop();
        Platform.runLater(new Runnable() {
            @Override public void run() {
                task.run();
                runNextStartupTask();
            }
        });
    }

    @Override public void stop() throws Exception {
        System.out.println("StarterApplication.stop");
        EclipseStarter.shutdown();
    }

    public static void main(String[] args) {
//        System.setProperty("org.osgi.framework.system.packages.extra","javafx.application");
        launch(args);
    }
}
