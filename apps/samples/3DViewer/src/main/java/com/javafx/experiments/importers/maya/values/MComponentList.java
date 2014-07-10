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

package com.javafx.experiments.importers.maya.values;

import java.util.List;

public interface MComponentList extends MData {
    public static class Component {
        // Ideally we would have an enum of these, but we don't know all of the mappings yet.
        // The possible values are listed in MFn::Type (MFn.h), but not the names.
        // Here are some, derived by using the Maya selection tool and
        // watching the script editor output:
        //   "f[i]"          -> faces
        //   "vtx[i]"        -> vertices
        //   "e[i]"          -> edges
        //   "map[i]"        -> uvs
        //   "vtxFace[i][j]" -> vertices within faces
        private String name;
        private int startIndex; // Or -1 if "all"
        private int endIndex;   // Inclusive

        public String name() { return name; }

        public int startIndex() { return startIndex; }

        public int endIndex() { return endIndex; }

        public Component(String name, int startIndex, int endIndex) {
            this.name = name;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        public static Component parse(String str) {
            String name = null;
            int startIndex = 0;
            int endIndex = 0;

            int bracket = str.indexOf("[");
            int endBracket = str.indexOf("]");
            if (bracket < 0) {
                name = str;
                startIndex = -1;
            } else {
                name = str.substring(0, bracket);
                if (str.charAt(bracket + 1) == '*') {
                    startIndex = -1;
                    endIndex = -1;
                } else {
                    int i = bracket + 1;
                    for (; i < endBracket; i++) {
                        if (str.charAt(i) == ':')
                            break;
                        startIndex *= 10;
                        startIndex += str.charAt(i) - '0';
                    }
                    if (str.charAt(i) == ':') {
                        i++;
                        for (; i < endBracket; i++) {
                            endIndex *= 10;
                            endIndex += str.charAt(i) - '0';
                        }
                    } else {
                        endIndex = startIndex;
                    }
                }
            }

            return new Component(name, startIndex, endIndex);
        }

        public String toString() {
            StringBuffer buf = new StringBuffer();
            buf.append(name);
            buf.append("[");
            if (startIndex < 0) {
                buf.append("*");
            } else {
                buf.append(startIndex);
                if (endIndex > startIndex) {
                    buf.append(":");
                    buf.append(endIndex);
                }
            }
            buf.append("]");
            return buf.toString();
        }
    }

    public void set(List<Component> value);

    public List<Component> get();
}
