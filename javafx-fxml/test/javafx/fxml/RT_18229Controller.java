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

import com.sun.javafx.fxml.PropertyChangeEvent;
import java.net.URL;
import java.util.ResourceBundle;

public class RT_18229Controller implements Initializable {
    @FXML private Widget root;
    @FXML private Widget child;

    private String rootName = null;
    private String childName = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateRootName();
        updateChildName();
    }

    public Widget getRoot() {
        return root;
    }

    public String getRootName() {
        return rootName;
    }

    public Widget getChild() {
        return child;
    }

    public String getChildName() {
        return childName;
    }

    @FXML
    protected void handleRootNameChange() {
        updateRootName();
    }

    @FXML
    protected void handleChildNameChange() {
        throw new RuntimeException();
    }

    @FXML
    protected void handleChildNameChange(PropertyChangeEvent<String> event) {
        updateChildName();
    }

    private void updateRootName() {
        rootName = root.getName();
    }

    private void updateChildName() {
        childName = child.getName();
    }
}
