/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.tools.packager;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class JreUtils {

    public static class Rule {
        String regex;
        boolean includeRule;
        Type type;
        enum Type {SUFFIX, PREFIX, SUBSTR, REGEX}

        private Rule(String regex, boolean includeRule, Type type) {
            this.regex = regex;
            this.type = type;
            this.includeRule = includeRule;
        }

        boolean match(String str) {
            if (type == Type.SUFFIX) {
                return str.endsWith(regex);
            }
            if (type == Type.PREFIX) {
                return str.startsWith(regex);
            }
            if (type == Type.SUBSTR) {
                return str.contains(regex);
            }
            return str.matches(regex);
        }

        boolean treatAsAccept() {return includeRule;}

        public static Rule suffix(String s) {
            return new Rule(s, true, Type.SUFFIX);
        }
        public static Rule suffixNeg(String s) {
            return new Rule(s, false, Type.SUFFIX);
        }
        static Rule prefix(String s) {
            return new Rule(s, true, Type.PREFIX);
        }
        public static Rule prefixNeg(String s) {
            return new Rule(s, false, Type.PREFIX);
        }
        static Rule substr(String s) {
            return new Rule(s, true, Type.SUBSTR);
        }
        public static Rule substrNeg(String s) {
            return new Rule(s, false, Type.SUBSTR);
        }
    }

    public static boolean shouldExclude(File baseDir, File f, Rule ruleset[]) {
        if (ruleset == null) {
            return false;
        }

        String fname = f.getAbsolutePath().toLowerCase().substring(
                baseDir.getAbsolutePath().length());
        //first rule match defines the answer
        for (Rule r: ruleset) {
            if (r.match(fname)) {
                return !r.treatAsAccept();
            }
        }
        //default is include
        return false;
    }

    public static void walk(File base, File root, Rule ruleset[], Set<File> files) {
        walk(base, root, ruleset, files, false);
    }

    public static void walk(File base, File root, Rule ruleset[], Set<File> files, boolean acceptSymlinks) {
        if (!root.isDirectory()) {
            if (root.isFile()) {
                files.add(root);
            }
            return;
        }

        File[] lst = root.listFiles();
        if (lst != null) {
            for (File f : lst) {
                if ((acceptSymlinks || IOUtils.isNotSymbolicLink(f)) && !shouldExclude(base, f, ruleset)) {
                    if (f.isDirectory()) {
                        walk(base, f, ruleset, files, acceptSymlinks);
                    } else if (f.isFile()) {
                        //add to list
                        files.add(f);
                    }
                }
            }
        }
    }

    public static RelativeFileSet extractJreAsRelativeFileSet(String root, JreUtils.Rule[] ruleset) {
        return extractJreAsRelativeFileSet(root, ruleset, false);
    }
    
    public static RelativeFileSet extractJreAsRelativeFileSet(String root, JreUtils.Rule[] ruleset, boolean acceptSymlinks) {
        if (root.isEmpty()) return null;

        File baseDir = new File(root);

        Set<File> lst = new HashSet<>();

        walk(baseDir, baseDir, ruleset, lst, acceptSymlinks);

        return new RelativeFileSet(baseDir, lst);
    }

}
