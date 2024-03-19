/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

public class BigGlyphIDTest extends Application {

    private static String OS = System.getProperty("os.name").toLowerCase();

    public void start(Stage stage) {
        if (OS.indexOf("win") < 0) {
            System.err.println("# You need to run on Windows");
            System.exit(0);
        }

        final String family = "Unifont";
        // download GNU Unifont and install
        // http://www.unifoundry.com/unifont.html
        // http://unifoundry.com/pub/unifont-11.0.01/font-builds/unifont-11.0.01.ttf

        Font font = Font.font(family, 48.0);
        if (font == null || !family.equals(font.getFamily())) {
            System.err.println("# You need to install font "+family);
            System.exit(0);
        }

        stage.setWidth(110);
        stage.setHeight(180);
        Group g = new Group();
        final Scene scene = new Scene(new Group());
        VBox box = new VBox();
        ((Group)scene.getRoot()).getChildren().add(box);
        stage.setScene(scene);

                                             // Unicode(GlyphID)
        Text txt = new Text("\u8002\u0362"); // U+8002(32773) + U+0362(869)
        txt.setFont(font);
        box.getChildren().add(txt);

        Image img = new Image("BigGlyphIDTest_Expected.png");
        ImageView iv = new ImageView();
        iv.setImage(img);
        box.getChildren().add(iv);

        stage.show();
    }
}
