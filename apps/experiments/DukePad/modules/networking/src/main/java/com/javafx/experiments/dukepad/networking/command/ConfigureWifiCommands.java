/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.javafx.experiments.dukepad.networking.command;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.List;

public final class ConfigureWifiCommands extends Command {

    private static final String NETWORKING_COMMAND = "/etc/init.d/networking";
    private static final String BASH_COMMAND = "bash";
    private static final String COMMANDS = "scanning";
    private static final String NETWORK_INTERFACE = "wlan0"; //pi
    private StringProperty netInterface = new SimpleStringProperty(NETWORK_INTERFACE);
    private StringProperty networkName = new SimpleStringProperty();
    private StringProperty passcode = new SimpleStringProperty();

    public ConfigureWifiCommands(String networkName, String passcode) {
        this.networkName.setValue(networkName);
        this.passcode.setValue(passcode);
    }
    /**
     * Get the base command to be run (no options)
     *
     * @return The name of the command to be run
     */
    @Override
    public String getCommandName() {
        return BASH_COMMAND;
    }

    /**
     * Set interface
     *
     * @param netInterface The interface whose info we want. Default is "wlan0"
     */
    public void setNetInterface(String netInterface) {
        this.netInterface.setValue(netInterface);
    }

    /**
     * Get the interface
     *
     * @return The network interface to be queried/processed
     */
    public String getNetInterface() {
        return netInterface.getValue();
    }

    /**
     * Get the property that holds the interface
     *
     * @return The property that holds the network interface to be queried/processed
     */
    public StringProperty netInterfaceProperty() {
        return netInterface;
    }

    /**
     * Set network name
     *
     * @param networkName The network name we want to connect to
     */
    public void setNetworkName(String networkName) {
        this.networkName.setValue(networkName);
    }

    /**
     * Get the network name
     *
     * @return The network name
     */
    public String getNetworkName() {
        return networkName.getValue();
    }

    /**
     * Get the property that holds the network name
     *
     * @return The property that holds the network name
     */
    public StringProperty networkName() {
        return networkName;
    }

    /**
     * Set passcode
     *
     * @param parameter The iwlist command parameter (e.g. scanning, frequency,...)
     */
    public void setPasscode(String parameter) {
        this.passcode.setValue(parameter);
    }

    /**
     * Get the passcode
     *
     * @return The passcode
     */
    public String getPasscode() {
        return passcode.getValue();
    }

    /**
     * Get the property that holds the passcode
     *
     * @return The property that holds the passcode
     */
    public StringProperty passcodeProperty() {
        return passcode;
    }

    /**
     * Builds/generates the iwlist command to be executed.
     *
     * @return a List containing the platform-specific iwlist command
     */
    @Override
    public List<String> buildCommand() {
        List<String> commandParts = super.buildCommand();
        commandParts.add("-c");
        commandParts.add("cp /etc/network/interfaces /etc/network/interfaces.backup; " +
                "cp /tmp/interfaces /etc/network/interfaces; " +
                "/etc/init.d/networking restart");
        return commandParts;
    }
}
