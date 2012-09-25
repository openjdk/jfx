/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

package javafx.fxml;

import com.sun.javafx.fxml.ObservableListChangeEvent;
import com.sun.javafx.fxml.ObservableMapChangeEvent;
import java.net.URL;
import java.util.*;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

public class RT_17714Controller implements Initializable {
    @FXML private Widget root;

    private ArrayList<Widget> children = new ArrayList<Widget>();
    private HashMap<String, Object> properties = new HashMap<String, Object>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        children.addAll(root.getChildren());
        properties.putAll(root.getProperties());
    }

    public List<Widget> getChildren() {
        return children;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    @FXML
    @SuppressWarnings("unchecked")
    protected void handleChildListChange(ObservableListChangeEvent<Widget> event) {
        ObservableList<Widget> list = (ObservableList<Widget>)event.getSource();
        int from = event.getFrom();
        int to = event.getTo();
        List<Widget> removed = event.getRemoved();

        if (event.getEventType() == ObservableListChangeEvent.ADD) {
            children.addAll(list.subList(from, to));
        } else if (event.getEventType() == ObservableListChangeEvent.UPDATE) {
            if (removed != null) {
                for (int i = from, n = from + removed.size(); i < n; i++) {
                    children.set(i, list.get(i));
                }
            }
        } else if (event.getEventType() == ObservableListChangeEvent.REMOVE) {
            children.subList(from, from + removed.size()).clear();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @FXML
    @SuppressWarnings("unchecked")
    protected void handlePropertiesChange(ObservableMapChangeEvent<String, Object> event) {
        ObservableMap<String, Object> map = (ObservableMap<String, Object>)event.getSource();
        String key = event.getKey();

        if (event.getEventType() == ObservableMapChangeEvent.ADD
            || event.getEventType() == ObservableMapChangeEvent.UPDATE) {
            properties.put(key, map.get(key));
        } else if (event.getEventType() == ObservableMapChangeEvent.REMOVE) {
            properties.remove(key);
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
