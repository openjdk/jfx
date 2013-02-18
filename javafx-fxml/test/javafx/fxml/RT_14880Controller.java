/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class RT_14880Controller {
    private StringProperty nameProperty = new SimpleStringProperty(this, "name");
    private DoubleProperty percentageProperty = new SimpleDoubleProperty(this, "percentage");

    public RT_14880Controller() {
        setPercentage(0.5);
    }

    public String getName() {
        return nameProperty.get();
    }

    public void setName(String value) {
        nameProperty.set(value);
    }

    public StringProperty nameProperty() {
        return nameProperty;
    }

    public Double getPercentage() {
        return percentageProperty.get();
    }

    public void setPercentage(Double value) {
        percentageProperty.set(value);
    }

    public DoubleProperty percentageProperty() {
        return percentageProperty;
    }
}
