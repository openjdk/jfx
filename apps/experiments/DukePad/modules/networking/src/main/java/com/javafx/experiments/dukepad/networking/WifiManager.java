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

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.javafx.experiments.dukepad.networking.command.ConfigureWifiCommands;
import com.javafx.experiments.dukepad.networking.command.IwconfigCommand;
import com.javafx.experiments.dukepad.networking.command.IwlistCommand;
import com.javafx.experiments.dukepad.networking.worker.CommandService;
import com.javafx.experiments.dukepad.networking.worker.JobModel;

/** Not Presently Used, still mining for useful code */
public class WifiManager {

    private static final String ESSID_LABEL = "ESSID:";
    private static final String EXPR_ESSID_NETWORK_NAME = "ESSID\\:\\\"([^\\\"]*)\\\"";
    private StringProperty essid = new SimpleStringProperty();
    private String networkName;
    private String passcode;

    public ObservableList networkNamesList = FXCollections.observableArrayList();

    public final void setEssid(String essid) {
        this.essid.setValue(essid);
    }

    public final String getEssid() {
        return essid.getValue();
    }

    public final StringProperty essidProperty() {
        return essid;
    }

    public void collectEssidInfo() {
        IwconfigCommand iwconfigCommand = new IwconfigCommand();

        CommandService commandService = new CommandService(iwconfigCommand);
        commandService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                JobModel jm = (JobModel) t.getSource().getValue();
                if (jm.getResult() != -1) {
                    setEssid(parseForWifiInfo(jm.getStdOutText(), ESSID_LABEL, EXPR_ESSID_NETWORK_NAME));
                }
            }
        });
        commandService.start();
    }

    public final void setSelectedNetwork(String network) {
        this.networkName = network;
    }

    public final void setPw(String pw) {
        this.passcode = pw;
    }

    public void collectNetworkNames() {
        IwlistCommand iwlistCommand = new IwlistCommand();
        //System.out.println("WifiManager: collectNetworkNames()");
        CommandService commandService = new CommandService(iwlistCommand);
        commandService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                JobModel jm = (JobModel) t.getSource().getValue();
                if (jm.getResult() != -1) {
                    parseForNetworkNames(jm.getStdOutText(), ESSID_LABEL, EXPR_ESSID_NETWORK_NAME);
                    System.out.println("WifiManager.collectNetworkNames: nameList = " + networkNamesList.toString()) ;
                }
            }
        });
        commandService.start();
    }

    public void connectWifi() {
        // System.out.println("WifiManager: networkName = " + networkName)  ;
        ConfigureWifiCommands configureWifiCommand = new ConfigureWifiCommands(networkName, passcode);

        CommandService commandService = new CommandService(configureWifiCommand);
        commandService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                JobModel jm = (JobModel) t.getSource().getValue();
                if (jm.getResult() != -1) {
                    setEssid(networkName);  //Update the current network name. TODO what if connect fails
                    //System.out.println("WifiManager return from configWifiCommand success");
                    //System.out.println("stdout=" + jm.getStdOutText());
                }
            }
        });
        commandService.start();
    }

    private String parseForWifiInfo(String inString, String exactInfoLabel, String searchExpression) {
        Pattern findString = Pattern.compile(searchExpression);
        System.out.println("parseForWifiInfo inString=" + inString + " searchExpr=" + searchExpression);
        Matcher matcher = findString.matcher(inString);
        String foundInfo = null;

        while (matcher.find())  {
            foundInfo = matcher.group() ;
            //System.out.println("matching IP is: " + ipAddr) ;//should be just one
        }
        if (foundInfo != null) {
            // System.out.println("parseForwifi: substring=" + foundInfo.substring(exactInfoLabel.length()) + " and length is " + exactInfoLabel.length() )  ;
            return foundInfo.substring(exactInfoLabel.length()) ;
        } else return null;
    }

    private void parseForNetworkNames(String inString, String exactInfoLabel, String searchExpression) {
        Pattern findString = Pattern.compile(searchExpression);
        //System.out.println("parseForNetworkNames: info from iwconfig" + inString);
        Matcher matcher = findString.matcher(inString);
        String foundInfo = null;

        while (matcher.find())  {
            foundInfo = matcher.group() ;
            //System.out.println("foundInfo is: " + foundInfo) ;//should be just one
            //System.out.println("parseForNetworkNames: substring=" + foundInfo.substring(exactInfoLabel.length()) + " and length is " + exactInfoLabel.length() );
            String name = foundInfo.substring(exactInfoLabel.length()+1, foundInfo.length()-1);
            if (foundInfo != null)  {
                if ((!networkNamesList.contains(name)) && (name != null) && (name.length() > 0)) {
                    networkNamesList.add(name); //remove ESSID: and the quotes
                }
            }
        }
    }
}
