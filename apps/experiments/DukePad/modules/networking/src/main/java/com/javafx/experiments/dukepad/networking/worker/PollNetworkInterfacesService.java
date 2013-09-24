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

package com.javafx.experiments.dukepad.networking.worker;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.javafx.experiments.dukepad.networking.NetworkInterface;

/**
 * A service which will populate an ObservableList with the NetworkInterfaces that are available.
 */
public class PollNetworkInterfacesService extends ScheduledService<List<NetworkInterface>> {
    private ObservableList<NetworkInterface> networkInterfaces = FXCollections.observableArrayList();
    private ObservableList<NetworkInterface> unmodifiable = FXCollections.unmodifiableObservableList(networkInterfaces);
    private FilteredList<NetworkInterface> available = unmodifiable.filtered(networkInterface -> networkInterface.isUp());

    /**
     * An unmodifiable observable list of NetworkInterfaces. Whenever this Service is successfully executed,
     * this list will be updated to include all network interfaces on the device. We attempt to keep the
     * same instances around where possible.
     */
    public final ObservableList<NetworkInterface> getNetworkInterfaces() { return unmodifiable; }

    /**
     * An unmodifiable observable list of available Network interfaces. That is, these are network interfaces
     * that are up -- they have an ip address and subnet.
     */
    public final ObservableList<NetworkInterface> getAvailableNetworkInterfaces() { return available; }

    @Override protected Task<List<NetworkInterface>> createTask() {
        return new GetNetworkInterfacesTask();
    }

    @Override protected void succeeded() {
        super.succeeded();
        List<NetworkInterface> results = getLastValue();
        if (results == null || results.isEmpty()) {
            networkInterfaces.clear();
        } else {
            // We want to make sure that we disturb the existing list as little as possible.
            // So we're going to only remove an interface from the existing list if it isn't
            // in the set of new interfaces, and we're only going to add an interface to
            // the list if it isn't already there.
            Map<String, NetworkInterface> existing = new HashMap<>();
            for (NetworkInterface nic : networkInterfaces) existing.put(nic.getName(), nic);

            Map<String, NetworkInterface> candidates = new HashMap<>();
            for (NetworkInterface nic : results) candidates.put(nic.getName(), nic);

            // Get the list of interface names to be removed
            Set<String> namesOfInterfacesToBeRemoved = new HashSet<>(existing.keySet());
            namesOfInterfacesToBeRemoved.removeAll(candidates.keySet());

            // Get the list of interface names to be added
            Set<String> namesOfInterfacesToBeAdded = new HashSet<>(candidates.keySet());
            namesOfInterfacesToBeAdded.removeAll(existing.keySet());

            // Get the list of interface names to be updated
            Set<String> namesOfInterfacesToBeUpdated = new HashSet<>(existing.keySet());
            namesOfInterfacesToBeUpdated.retainAll(candidates.keySet());

            // Now remove the ones that need to be removed
            for (String name : namesOfInterfacesToBeRemoved) {
                System.out.println("Removed " + existing.get(name));
                networkInterfaces.remove(existing.remove(name));
            }

            // Update the ones that need to be updated
            for (String name : namesOfInterfacesToBeUpdated) {
                NetworkInterface nic = existing.get(name);
                NetworkInterface candidate = candidates.get(name);
                String a1 = nic.getAddress();
                String a2 = candidate.getAddress();
                String s1 = nic.getSubnet();
                String s2 = candidate.getSubnet();
                boolean aChanged = (a1 != a2 && (a1 == null || !a1.equals(a2)));
                boolean bChanged = (s1 != s2 && (s1 == null || !s1.equals(s2)));
                if (aChanged || bChanged) {
                    System.out.println("Updating " + nic.getName() + " to " + candidate);
                    nic.addressProperty().set(candidate.getAddress());
                    nic.subnetProperty().set(candidate.getSubnet());
                }
            }

            // Now populate a list of existing + ones to be added, and then sort them,
            // and then update the real list by putting things in the right order as needed.
            List<NetworkInterface> list = new ArrayList<>(networkInterfaces);
            for (String name : namesOfInterfacesToBeAdded) {
                System.out.println("Added " + candidates.get(name));
                list.add(candidates.get(name));
            }

            // Now sort it
            list.sort((nic1, nic2) -> {
                NetworkInterface.Type t1 = nic1.getType();
                NetworkInterface.Type t2 = nic2.getType();
                return t1 == t2 ? nic1.getName().compareTo(nic2.getName()) :
                        t1.compareTo(t2);
            });

            // Now update the real list
            if (networkInterfaces.isEmpty()) {
                networkInterfaces.addAll(list);
            } else {
                for (int i=0; i<networkInterfaces.size(); i++) {
                    NetworkInterface nic = networkInterfaces.get(i);
                    while (!list.isEmpty()) {
                        NetworkInterface candidate = list.remove(0);
                        if (nic != candidate) {
                            networkInterfaces.add(i++, candidate);
                        } else {
                            break;
                        }
                    }
                }
                if (!list.isEmpty()) networkInterfaces.addAll(list);
            }
        }
    }

    @Override protected void failed() {
        super.failed();
        networkInterfaces.clear();
    }
}
