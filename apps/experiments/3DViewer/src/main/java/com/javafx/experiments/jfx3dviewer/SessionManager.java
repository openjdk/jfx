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
package com.javafx.experiments.jfx3dviewer;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.paint.Color;

public class SessionManager {

    public final String SESSION_PROPERTIES_FILENAME;
    private static final boolean ENABLE_SAVE_SESSION = true;
    private String name;
    private Properties props = new Properties();

    private SessionManager(String name) {
        this.name = name;
        SESSION_PROPERTIES_FILENAME = name + "_session.properties";
    }

    private static SessionManager sessionManager;

    public static SessionManager createSessionManager(String name) {
        return sessionManager = new SessionManager(name);
    }
    
    public static SessionManager getSessionManager() {
        return sessionManager;
    }

    public Properties getProperties() {
        return props;
    }

    public void loadSession() {
        Reader reader = null;
        try {
            reader = new FileReader(SESSION_PROPERTIES_FILENAME);
            props.load(reader);
        } catch (FileNotFoundException ignored) {
        } catch (IOException ex) {
            Logger.getLogger(OldTestViewer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(OldTestViewer.class.getName()).
                        log(Level.SEVERE, null, ex);
            }
        }
    }

    public void saveSession() {
        if (ENABLE_SAVE_SESSION) {
            try {
                props.store(new FileWriter(SESSION_PROPERTIES_FILENAME), name + " session properties");
            } catch (IOException ex) {
                Logger.getLogger(OldTestViewer.class.getName()).
                        log(Level.SEVERE, null, ex);
            }
        }
    }

    public void bind(final BooleanProperty property, final String propertyName) {
        String value = props.getProperty(propertyName);
        if (value != null) property.set(Boolean.valueOf(value));
        property.addListener(new InvalidationListener() {

            @Override
            public void invalidated(Observable o) {
                props.setProperty(propertyName, property.getValue().toString());
            }
        });
    }

    public void bind(final ObjectProperty<Color> property, final String propertyName) {
        String value = props.getProperty(propertyName);
        if (value != null) property.set(Color.valueOf(value));
        property.addListener(new InvalidationListener() {

            @Override
            public void invalidated(Observable o) {
                props.setProperty(propertyName, property.getValue().toString());
            }
        });
    }

    public void bind(final DoubleProperty property, final String propertyName) {
        String value = props.getProperty(propertyName);
        if (value != null) property.set(Double.valueOf(value));
        property.addListener(new InvalidationListener() {

            @Override
            public void invalidated(Observable o) {
                props.setProperty(propertyName, property.getValue().toString());
            }
        });
    }

    public void bind(final ToggleGroup toggleGroup, final String propertyName) {
        try {
            String value = props.getProperty(propertyName);
            if (value != null) {
                int selectedToggleIndex = Integer.parseInt(value);
                toggleGroup.selectToggle(toggleGroup.getToggles().get(selectedToggleIndex));
            }
        } catch (Exception ignored) {
        }
        toggleGroup.selectedToggleProperty().addListener(new InvalidationListener() {

            @Override
            public void invalidated(Observable o) {
                if (toggleGroup.getSelectedToggle() == null) {
                    props.remove(propertyName);
                } else {
                    props.setProperty(propertyName, Integer.toString(toggleGroup.getToggles().indexOf(toggleGroup.getSelectedToggle())));
                }
            }
        });
    }

    public void bind(final Accordion accordion, final String propertyName) {
        Object selectedPane = props.getProperty(propertyName);
        for (TitledPane tp : accordion.getPanes()) {
            if (tp.getText() != null && tp.getText().equals(selectedPane)) {
                accordion.setExpandedPane(tp);
                break;
            }
        }
        accordion.expandedPaneProperty().addListener(new ChangeListener<TitledPane>() {

            @Override
            public void changed(ObservableValue<? extends TitledPane> ov, TitledPane t, TitledPane expandedPane) {
                if (expandedPane != null) {
                    props.setProperty(propertyName, expandedPane.getText());
                }
            }
        });
    }

}
