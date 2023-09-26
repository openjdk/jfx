/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.font;

import java.util.ArrayList;

public class FontFallbackInfo {

   private ArrayList<String> linkedFontFiles;
   private ArrayList<String> linkedFontNames;
   private ArrayList<FontResource> linkedFonts;

   public FontFallbackInfo() {
      linkedFontFiles = new ArrayList<String>();
      linkedFontNames = new ArrayList<String>();
      linkedFonts = new ArrayList<FontResource>();
   }

   public void add(String name, String file, FontResource font) {
       linkedFontNames.add(name);
       linkedFontFiles.add(file);
       linkedFonts.add(font);
   }

   public boolean containsName(String name) {
       return (name != null) && linkedFontNames.contains(name);
   }

   public boolean containsFile(String file) {
       return (file != null) && linkedFontFiles.contains(file);
   }

   public String[] getFontNames() {
      return linkedFontNames.toArray(new String[0]);
   }

   public String[] getFontFiles() {
      return linkedFontFiles.toArray(new String[0]);
   }

   public FontResource[] getFonts() {
      return linkedFonts.toArray(new FontResource[0]);
   }
}
