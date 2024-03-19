/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class INVISIBLE_GLYPH_IDTest extends Application {

    private static String OS = System.getProperty("os.name").toLowerCase();

    public void start(Stage stage) {
        if (OS.indexOf("win") < 0) {
            System.err.println("# You need to run on Windows");
            System.exit(0);
        }

        final String fontName = "ipaexm.ttf";
        // download from https://ipafont.ipa.go.jp/node26#en
        // and place in {user.home}/fonts/

        String userHome = System.getProperty("user.home");
        userHome = userHome.replace("\\", "/");
        final String base = userHome+"/fonts/";
        Font font = Font.loadFont("file:"+base+fontName, 48);
        if (font == null || !"IPAexMincho".equals(font.getName())) {
            System.err.println("# You need to place "+fontName+" in "+base);
            System.exit(0);
        }

        stage.setWidth(140);
        stage.setHeight(260);
        Group g = new Group();
        final Scene scene = new Scene(new Group());
        VBox box = new VBox();
        ((Group)scene.getRoot()).getChildren().add(box);
        stage.setScene(scene);

        Text txt = new Text("\ud869\ude1a"); // U+2A61A (surrogate pair)
        txt.setFont(font);
        box.getChildren().add(txt);

        Image img = new Image("INVISIBLE_GLYPH_IDTest_Expected.png");
        ImageView iv = new ImageView();
        iv.setImage(img);
        box.getChildren().add(iv);

        stage.show();
    }
}
