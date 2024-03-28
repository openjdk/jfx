/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * Image Tools.
 */
public class ImageTools {
    public static ImageView createImageView(Color c, int w, int h) {
        Image im = createImage(c, w, h);
        return new ImageView(im);
    }

    public static Image createImage(Color c, int w, int h) {
        WritableImage im = new WritableImage(w, h);
        PixelWriter wr = im.getPixelWriter();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                wr.setColor(x, y, c);
            }
        }

        return im;
    }

    public static Image createImage(String s, int w, int h) {
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("sha-256").digest(s.getBytes());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            hash = new byte[3];
        }
        Color color = Color.rgb(hash[0] & 0xff, hash[1] & 0xff, hash[2] & 0xff);
        Canvas c = new Canvas(w, h);
        GraphicsContext g = c.getGraphicsContext2D();
        g.setFill(color);
        g.fillRect(0, 0, w, h);
        return c.snapshot(null, null);
    }
}
