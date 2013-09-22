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

package com.javafx.experiments.dukepad.networking.worker;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class JobModel {

    private final IntegerProperty result = new SimpleIntegerProperty();
    private final StringProperty stdOutText = new SimpleStringProperty();
    private final StringProperty stdErrText = new SimpleStringProperty();

    /**
     * Set the exit code of the job
     *
     * @param result The exit code
     */
    public final void setResult(int result) {
        this.result.set(result);
    }

    /**
     * Get the exit code of the job
     *
     * @return the exit code
     */
    public final int getResult() {
        return result.get();
    }

    /**
     * Property for job exit code
     */
    public final IntegerProperty resultProperty() {
        return result;
    }

    /**
     * Set the standard error text from the job
     *
     * @param stdErrText the standard error text
     */
    public final void setStdErrText(String stdErrText) {
        this.stdErrText.setValue(stdErrText);
    }

    /**
     * Get the standard error text from the job
     *
     * @return the standard error text
     */
    public final String getStdErrText() {
        return stdErrText.getValue();
    }

    /**
     * Property for standard error
     *
     * @return the standard error property
     */
    public final StringProperty stdErrProperty() {
        return stdErrText;
    }

    /**
     * Set the standard output text from the job
     *
     * @param stdOutText the standard error text
     */
    public final void setStdOutText(String stdOutText) {
        this.stdOutText.setValue(stdOutText);
    }

    /**
     * Set the standard output text from the job
     *
     */
    public final String getStdOutText() {
        return stdOutText.getValue();
    }

    /**
     * Property for standard out
     *
     * @return the standard out property
     */
    public final StringProperty stdOutProperty() {
        return stdOutText;
    }
}
