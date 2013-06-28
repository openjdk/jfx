/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.stage;

import javafx.collections.ObservableList;
import javafx.stage.Stage;

/**
 * Utility class class used for accessing certain implementation-specific
 * runtime functionality.
 */
public class StageHelper {

    private static StageAccessor stageAccessor;

    public static interface StageAccessor {
          public ObservableList<Stage> getStages();
    }

    /**
     * Returns a ObservableList containing {@code Stage}s created at this point.
     *
     * Note that application must use/reference javafx.stage.Stage class prior to
     * using this method (for example, by creating a Stage).
     *
     * @return ObservableList containing existing stages
     */
    public static ObservableList<Stage> getStages() {
        if (stageAccessor == null) {
            try {
                // Force stage static initialization, see http://java.sun.com/j2se/1.5.0/compatibility.html
                Class.forName(Stage.class.getName(), true, Stage.class.getClassLoader());
            } catch (ClassNotFoundException ex) {
                // Cannot happen
            }
        }
        return stageAccessor.getStages();
    }

    public static void setStageAccessor(StageAccessor a) {
        if (stageAccessor != null) {
            System.out.println("Warning: Stage accessor already set: " + stageAccessor);
            Thread.dumpStack();
        }
        stageAccessor = a;
    }
}
