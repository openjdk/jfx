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

package com.javafx.experiments.dukepad.settings;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import com.javafx.experiments.dukepad.core.BaseDukeApplication;
import com.javafx.experiments.dukepad.core.DukeApplication;
import com.javafx.experiments.dukepad.core.Fonts;
import com.javafx.experiments.dukepad.core.Settings;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;

/**
 * Settings App
 */
public class SettingsApp extends BaseDukeApplication implements BundleActivator {
    private static final Image appIcon = new Image(SettingsApp.class.getResource("/images/ico-settings.png").toExternalForm());
    private List<Settings> allSettings = new LinkedList<>();

    /** Get name of application */
    @Override public String getName() {
        return "Settings";
    }

    /** Create icon instance */
    @Override public Node createHomeIcon() {
        return new ImageView(appIcon);
    }

    /** Create the UI, new UI is created each time and not held on to */
    @Override protected Node createUI() {
        Accordion a = new Accordion();
        allSettings.sort((first, second) -> {
            int p1 = first.getSortOrder();
            int p2 = second.getSortOrder();
            if (p1 != p2) return p1 - p2;
            return first.getName().compareToIgnoreCase(second.getName());
        });
        allSettings.forEach(s -> {
            TitledPane pane = new TitledPane();
//            pane.setExpanded(false);
            pane.setText(s.getName());
//            pane.expandedProperty().addListener((o, old, value) -> {
//                System.out.println("expanded: " + pane.getText() + " old=" + old + ", value=" + value);
//                if (value && pane.getContent() == null) {
//                    pane.setContent(s.createUI());
//                }
//            });
            pane.setContent(s.createUI());
            a.getPanes().add(pane);
        });

        if (!allSettings.isEmpty()) {
            a.setExpandedPane(a.getPanes().get(0));
        }


        Button powerButton = new Button("Shutdown");
        powerButton.setGraphic(new ImageView(new Image(SettingsApp.class.getResource("/images/power.png").toExternalForm())));
        powerButton.setFont(Fonts.dosisExtraLight(30));
        powerButton.getStyleClass().clear();
        powerButton.setGraphicTextGap(8);
        powerButton.setOnAction(event -> {
            try {
                new ProcessBuilder()
                        .command("halt")
                        .inheritIO()
                        .start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Platform.exit();
        });
        HBox bottomControls = new HBox(10,powerButton);
        bottomControls.setBorder(new Border(new BorderStroke(Color.web("#dbdbdb"),null,null,null,
                BorderStrokeStyle.SOLID,BorderStrokeStyle.NONE,BorderStrokeStyle.NONE,BorderStrokeStyle.NONE,
                CornerRadii.EMPTY,BorderWidths.DEFAULT,Insets.EMPTY)));
        bottomControls.setPadding(new Insets(10,0,0,0));
        bottomControls.setAlignment(Pos.CENTER_RIGHT);


        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(a);
        borderPane.setBottom(bottomControls);
        borderPane.getStyleClass().add("app");
        borderPane.setPadding(new Insets(15));
        return borderPane;
    }

    /** Called when app is loaded at platform startup */
    @Override public void start(BundleContext bundleContext) throws Exception {
        // Register application service
        bundleContext.registerService(DukeApplication.class, this, null);
        // Load up any services that have already been registered
        ServiceReference[] services =
                bundleContext.getAllServiceReferences("com.javafx.experiments.dukepad.core.Settings", null);
        for (ServiceReference s : services) {
            allSettings.add((Settings)bundleContext.getService(s));
        }
        // Listen for Settings services that have been registered, so we can put them in
        // the user interface.
        bundleContext.addServiceListener(serviceEvent -> {
            final Settings settings = (Settings) bundleContext.getService(serviceEvent.getServiceReference());
            switch (serviceEvent.getType()) {
                case ServiceEvent.REGISTERED:
                    allSettings.add(settings);
                    break;
                case ServiceEvent.UNREGISTERING:
                    allSettings.remove(settings);
                    break;
            }
        }, "(" + Constants.OBJECTCLASS + "=com.javafx.experiments.dukepad.core.Settings)");
    }

    /** Called when app is unloaded at platform shutdown */
    @Override public void stop(BundleContext bundleContext) throws Exception {}

    @Override public void startApp() {
        super.startApp();
        setFullScreen(false);
    }

    @Override
    public void stopApp() {
        super.stopApp();
        allSettings.forEach(setting -> setting.disposeUI());
    }

    @Override public boolean supportsHalfScreenMode() {
        return true;
    }
}
