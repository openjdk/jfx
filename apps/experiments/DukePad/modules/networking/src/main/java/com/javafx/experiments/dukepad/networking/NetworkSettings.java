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

package com.javafx.experiments.dukepad.networking;

import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import javafx.util.StringConverter;
import com.javafx.experiments.dukepad.core.BaseSettings;
import com.javafx.experiments.dukepad.core.EasyGrid;
import com.javafx.experiments.dukepad.core.Settings;
import com.javafx.experiments.dukepad.networking.worker.PollNetworkInterfacesService;
import com.javafx.experiments.dukepad.networking.worker.PollWifiSignalStrengthService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Network Settings Section
 */
public class NetworkSettings extends BaseSettings implements BundleActivator {
    private PollNetworkInterfacesService networkInterfacesService = new PollNetworkInterfacesService();
    private PollWifiSignalStrengthService wifiStrengthService = new PollWifiSignalStrengthService();
    private ComboBox<NetworkInterface> interfaceChoiceBox;
    private EasyGrid extraProperties;

    public NetworkSettings() {
        super("Network");
    }

    private void nicChangeListener(ListChangeListener.Change change) {
        NetworkInterface nic = interfaceChoiceBox.getValue();
        if (nic == null && !change.getList().isEmpty()) {
            interfaceChoiceBox.setValue((NetworkInterface) change.getList().get(0));
        }
    }

    @Override
    protected void buildUI() {
        networkInterfacesService.setPeriod(Duration.seconds(5));
        networkInterfacesService.restart();

        FilteredList<NetworkInterface> items = networkInterfacesService.getAvailableNetworkInterfaces ()
            .filtered(iface -> iface.getType() != NetworkInterface.Type.LOOPBACK);

        // Filter out the loop back interface.
        interfaceChoiceBox = new ComboBox<>(items);
        interfaceChoiceBox.setPrefWidth(200);
        interfaceChoiceBox.setConverter(new StringConverter<NetworkInterface>() {
            @Override public String toString(NetworkInterface object) {
                switch (object.getType()) {
                    case WIFI: return "Wifi";
                    case LOOPBACK: return "Loopback";
                    case WIRED: return "Wired";
                    default: return object.getType() + "(" + object.getName() + ")";
                }
            }
            @Override public NetworkInterface fromString(String string) {
                return null;
            }
        });
        addRow("Interface", interfaceChoiceBox);

        // Listen to changes and update the selected value of the choice box if necessary
        items.addListener(this::nicChangeListener);

        Label ipAddress = new Label();
        addRow("IP Address", ipAddress);

        Label subnet = new Label();
        addRow("Subnet", subnet);

        extraProperties = new EasyGrid();
        addRow(extraProperties);

        // When the value of the choice box changes, we have to rebuild UI.
        interfaceChoiceBox.valueProperty().addListener((o, old, value) -> {
            if (value != null) {
                ipAddress.textProperty().bind(
                        Bindings.when(value.addressProperty().isNotNull())
                        .then(value.addressProperty()).otherwise("Disconnected"));
                subnet.textProperty().bind(
                        Bindings.when(value.subnetProperty().isNotNull())
                        .then(value.subnetProperty()).otherwise("Disconnected"));
            } else {
                ipAddress.textProperty().unbind();
                subnet.textProperty().unbind();
            }

            // Turning off this functionality for now because the text entry boxes are broken on
            // embedded, and performance needs investigation.
            final boolean enable = false;
            if (enable && (old != value && (old == null || !old.equals(value)))) {
                // The value has changed. Just rebuild it all.
                extraProperties.clear();
                wifiStrengthService.cancel();
                if (value != null) {
                    NetworkInterface.Type type = value.getType();
                    if (type == NetworkInterface.Type.WIFI) {
                        wifiStrengthService.setInterfaceName(value.getName());
                        wifiStrengthService.setPeriod(Duration.seconds(10));
                        wifiStrengthService.restart();

                        ProgressBar signalBar = new ProgressBar();
                        wifiStrengthService.lastValueProperty().addListener((valueProperty, oldSignal, newSignal) -> {
                            if (newSignal == null) {
                                signalBar.setProgress(0);
                            } else {
                                double link = ((WifiSignalStrength) newSignal).getLink();
                                signalBar.setProgress(link / 100.0);
                            }
                        });

                        extraProperties.addRow("Signal", signalBar);

                        ComboBox<String> ssidComboBox = new ComboBox<>();
                        ssidComboBox.getItems().addAll("BairDen", "SomethingElse");
                        extraProperties.addRow("SSID", ssidComboBox);

                        Pane passwordPane = new StackPane();
                        PasswordField passwordField = new PasswordField();
                        TextField clearField = new TextField();
                        passwordPane.getChildren().add(passwordField);
                        passwordPane.getChildren().add(clearField);

                        CheckBox showPassword = new CheckBox();
                        passwordField.visibleProperty().bind(showPassword.selectedProperty().not());
                        clearField.visibleProperty().bind(showPassword.selectedProperty());
                        passwordField.textProperty().bindBidirectional(clearField.textProperty());

                        extraProperties.addRow("Password", passwordPane);
                        extraProperties.addRow("Show Password", showPassword);
                    }
                }
            }
        });
    }

    @Override
    public void disposeUI() {
        networkInterfacesService.cancel();
        wifiStrengthService.cancel();
        interfaceChoiceBox = null;
        extraProperties = null;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        // Register as a Settings service
        context.registerService(Settings.class, this, null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {

    }

    @Override public Type getType() {
        return Type.SYSTEM;
    }

    @Override public int getSortOrder() {
        return Integer.MIN_VALUE;
    }
}
