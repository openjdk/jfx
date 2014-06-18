/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.javafx.experiments.importers.maya;

import java.util.Comparator;

public class MConnection {
    private MPath sourcePath;
    private MPath targetPath;

    public MConnection(MPath sourcePath, MPath targetPath) {
        this.sourcePath = sourcePath;
        this.targetPath = targetPath;
    }

    public MPath getSourcePath() {
        return sourcePath;
    }

    public MPath getTargetPath() {
        return targetPath;
    }

    public boolean equals(Object arg) {
        if (!(arg instanceof MConnection)) {
            return false;
        }
        MConnection other = (MConnection) arg;
        return (sourcePath.equals(other.sourcePath) &&
                targetPath.equals(other.targetPath));
    }

    public int hashCode() {
        return sourcePath.hashCode() ^ targetPath.hashCode();
    }

    public static final Comparator SOURCE_PATH_COMPARATOR = (o1, o2) -> {
        MConnection c1 = (MConnection) o1;
        MConnection c2 = (MConnection) o2;
        return c1.getSourcePath().compareTo(c2.getSourcePath());
    };

    public static final Comparator TARGET_PATH_COMPARATOR = (o1, o2) -> {
        MConnection c1 = (MConnection) o1;
        MConnection c2 = (MConnection) o2;
        return c1.getTargetPath().compareTo(c2.getTargetPath());
    };
}
