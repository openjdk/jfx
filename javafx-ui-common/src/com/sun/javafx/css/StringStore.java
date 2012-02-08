/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package com.sun.javafx.css;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * StringStore
 *
 */
public class StringStore {
    private Map<String,Integer> stringMap = new HashMap<String,Integer>();
    public List<String> strings = new ArrayList<String>();

    public int addString(String s) {

        Integer index = stringMap.get(s);
        if (index == null) {
            index = strings.size();
            strings.add(s);
            stringMap.put(s,index);
        }
        return index;
    }

    public void writeBinary(DataOutputStream os) throws IOException {
        os.writeShort(strings.size());
        if (stringMap.containsKey(null)) {
            Integer index = stringMap.get(null);
            os.writeShort(index);
        } else {
            os.writeShort(-1);
        };
        for (int n=0; n<strings.size(); n++) {
            String s = strings.get(n);
            if (s == null) continue;
            os.writeUTF(s);
        }
    }

    // TODO: this isn't parallel with writeBinary
    static String[] readBinary(DataInputStream is) throws IOException {
        int nStrings = is.readShort();
        int nullIndex = is.readShort();
        String[] strings = new String[nStrings];
        java.util.Arrays.fill(strings, null);
        for (int n=0; n<nStrings; n++) {
            if (n == nullIndex) continue;
            strings[n] = is.readUTF();
        }
        return strings;
    }
}
