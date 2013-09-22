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
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.javafx.experiments.dukepad.networking.command.IfconfigCommand;
import com.javafx.experiments.dukepad.networking.worker.CommandService;
import com.javafx.experiments.dukepad.networking.worker.JobModel;

/** Not Presently Used, still mining for useful code */
public class EthernetManager {

   // private static final String IP_ADDR_LABEL = "inet\\s" //mac;
    private static final String EXACT_IP_ADDR_LABEL = "inet addr:";
    private static final String EXPR_IP_ADDR = new StringBuilder().append("\\b").append("inet\\saddr\\:").append("([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)").toString(); //pi
    private static final String SUBNET_MASK_LABEL = "Mask:";  //pi
    private static final String EXPR_SUBNET_MASK = new StringBuilder().append("\\b").append(SUBNET_MASK_LABEL).append("([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)").toString(); //pi
    public StringProperty ipAddress = new SimpleStringProperty();
    private StringProperty subnetMask = new SimpleStringProperty();

    public final void setIpAddress(String ipAddress) {
        this.ipAddress.setValue(ipAddress);
    }

    public final String getIpAddress() {
        return ipAddress.getValue();
    }

    public final StringProperty ipAddressProperty() {
        return ipAddress;
    }

    public final void setSubnetMask(String mask) {
        this.subnetMask.setValue(mask);
    }

    public final String getSubnetMask() {
        return subnetMask.getValue();
    }

    public final StringProperty subnetMaskProperty() {
        return subnetMask;
    }

    public void collectEthernetInfo() {
        IfconfigCommand ifconfigCommand = new IfconfigCommand();

        CommandService commandService = new CommandService(ifconfigCommand);
        commandService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                JobModel jm = (JobModel) t.getSource().getValue();
                if (jm.getResult() != -1) {
                    setIpAddress(parseForEthernetInfo(jm.getStdOutText(), EXACT_IP_ADDR_LABEL, EXPR_IP_ADDR));
                    setSubnetMask(parseForEthernetInfo(jm.getStdOutText(), SUBNET_MASK_LABEL, EXPR_SUBNET_MASK));
                }
            }
        });
        commandService.setOnFailed(event -> {
            Throwable e = event.getSource().getException();
            if (e != null) e.printStackTrace();
            else {
                System.out.println("FAILED for some reason?");
            }
        });
        commandService.start();
    }

    private String parseForEthernetInfo(String inString, String exactInfoLabel, String searchExpression) {
        Pattern findString = Pattern.compile(searchExpression);
        //System.out.println("info from ifconfig" + inString);
        Matcher matcher = findString.matcher(inString);
        String foundInfo = null;

        while (matcher.find())  {
            foundInfo = matcher.group() ;
            //System.out.println("matching IP is: " + ipAddr) ;//should be just one
        }
        if (foundInfo != null) {
            System.out.println("parseForsubnet mask (generic): substring=" + foundInfo.substring(exactInfoLabel.length()) + " and length is " + exactInfoLabel.length() )  ;
            return foundInfo.substring(exactInfoLabel.length()) ;
        } else return null;
    }
}
