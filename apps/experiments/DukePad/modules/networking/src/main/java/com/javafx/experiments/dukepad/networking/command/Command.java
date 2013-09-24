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

import java.util.ArrayList;
import java.util.List;

public abstract class Command {
    /**
     * Get the name of the command to be run (no options)
     *
     * @return The name of the command to be run
     */
    public abstract String getCommandName();

    /**
     * Command-specific validation
     *
     * @return true = good. false = bad.
     */
    public boolean commandSyntaxValid() {
        return true;
    }

    /**
     * Build/generate the command to be run
     *
     * @return The command to be run
     */
    public List<String> buildCommand() {
        List<String> commandAndOptionsList = new ArrayList<String>();
        commandAndOptionsList.add(getCommandName());
        return commandAndOptionsList;
    }
}
