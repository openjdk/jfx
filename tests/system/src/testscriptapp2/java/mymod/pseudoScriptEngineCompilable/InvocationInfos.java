/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package pseudoScriptEngineCompilable;

import javax.script.Bindings;
import javax.script.ScriptContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;


/** Stores PseudoScriptEngine related invocation information for asserting and debugging. */
public class InvocationInfos {
    public String script;
    public TreeMap<Integer,TreeMap<String,Object>> bindings;
    public Instant dateTime;

    InvocationInfos(String script, ScriptContext context) {
        this.dateTime = Instant.now();
        this.script = script;
        this.bindings = new TreeMap();
        // get and save each Bindings
        for (Integer scope : context.getScopes()) {
            Bindings binding = context.getBindings(scope);
            bindings.put(scope, binding == null ? new TreeMap<String,Object>() : new TreeMap<String,Object>(binding));
        }
    }

    /**
     * Creates and returns a string having all information formatted to ease debugging.
     * @return string formatted to ease debugging
     */
    public String toDebugFormat(String indentation) {
        StringBuilder sb = new StringBuilder();
        String indent = (indentation == null ? "\t\t" : indentation);
        sb.append(indent).append("at:     [").append(dateTime.toString()).append("]\n");
        sb.append(indent).append("script: [").append(script)             .append("]\n");

        for (Integer scope : (Set<Integer>) bindings.keySet()) {
            sb.append(indent).append("Bindings for scope # ").append(scope);
            if (scope == 100) sb.append(" (ENGINE_SCOPE):");
            else if (scope == 200) sb.append(" (GLOBAL_SCOPE):");
            else                 sb.append(':');
            sb.append('\n');

            TreeMap<String,Object> treeMap = bindings.get(scope);
            for (String k : (Set<String>) treeMap.keySet()) {
                sb.append(indent).append("\t[").append(k).append("]:\t[").append(treeMap.get(k)).append("]\n");
            }
        }
        return sb.toString();
    }
}
