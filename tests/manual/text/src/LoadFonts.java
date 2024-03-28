/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import javafx.application.*;
import javafx.stage.*;
import javafx.scene.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.text.*;

import java.io.*;

public class LoadFonts extends Application {

    static String filename = null;
    public static void main(String[] args) {
       if (args.length > 0) {
           filename = args[0];
       } else {
          System.err.println("Needs a font file.");
          System.err.println("usage : java LoadFonts FOO.ttc");
       }
       launch(args);
    }

    public void start(Stage stage) {
        stage.setWidth(600);
        stage.setHeight(600);
        Group g = new Group();
        final Scene scene = new Scene(new Group());
        scene.setFill(Color.WHITE);
        VBox box = new VBox(10);
        ((Group)scene.getRoot()).getChildren().add(box);
        stage.setScene(scene);

        String url = "file:" + filename;

        // Load a single font from the TTC file
        Font font = Font.loadFont(url, 24.0);
        System.out.println(font);
        if (font != null) {
            addText(box, font);
        }

        // Load all fonts from the TTC file
        Font[] fonts = Font.loadFonts(url, 24.0);
        if (fonts != null) {
            for (int i=0; i<fonts.length; i++) {
                System.out.println(fonts[i]);
                addText(box, fonts[i]);
            }
       }

        // Load a single font from a stream open on the TTC file.
        Font sfont = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filename);
            sfont = Font.loadFont(fis, 24.0);
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
           if (fis != null) try {
               fis.close();
            } catch (IOException e) {
            }
        }
        System.out.println(sfont);
        if (font != null) {
            addText(box, sfont);
         }

        // Load all fonts from a stream open on the TTC file.
        Font[] sfonts = null;
        fis = null;
        try {
            fis = new FileInputStream(filename);
            sfonts = Font.loadFonts(fis, 24.0);
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
           if (fis != null) try {
               fis.close();
            } catch (IOException e) {
            }
        }
        System.out.println("Loaded from stream " + sfonts);
        if (sfonts != null) {
            for (int i=0; i<sfonts.length; i++) {
                System.out.println("Stream " + sfonts[i]);
                addText(box, sfonts[i]);
            }
       }

       stage.show();
    }

    private void addText(VBox box, Font f) {
        String str = "abcdefghihjklmnopqrstuvwxyz " + f.getName();
        Text txt1 = new Text(str);
        txt1.setFont(f);
        txt1.setFill(Color.BLACK);
        txt1.setFontSmoothingType(FontSmoothingType.GRAY);
        box.getChildren().add(txt1);
    }
}

