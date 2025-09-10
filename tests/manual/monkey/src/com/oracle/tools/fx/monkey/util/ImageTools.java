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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.imageio.ImageIO;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

/**
 * Image Tools.
 */
public class ImageTools {
    public static ImageView createImageView(int w, int h) {
        Image im = createImage(w, h);
        return new ImageView(im);
    }

    public static Image createImage(int w, int h) {
        Canvas c = new Canvas(w, h);
        GraphicsContext g = c.getGraphicsContext2D();
        g.setFill(Color.gray(0.97));
        g.fillRect(0, 0, w, h);

        g.setStroke(Color.gray(0.9));
        g.setLineWidth(1.0);
        for (double y = 0.5; y < h; y += 10) {
            g.strokeLine(0, y, w, y);
        }
        for (double x = 0.5; x < w; x += 10) {
            g.strokeLine(x, 0, x, h);
        }

        g.setStroke(Color.gray(0.7));
        for (double y = 0.5; y < h; y += 100) {
            g.strokeLine(0, y, w, y);
        }
        for (double x = 0.5; x < w; x += 100) {
            g.strokeLine(x, 0, x, h);
        }

        g.setStroke(Color.RED);
        g.strokeRect(0, 0, w, h);

        return c.snapshot(null, null);
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

    /**
     * Writes an Image to a byte array in PNG format.
     *
     * @param im source image
     * @return byte array containing PNG image
     * @throws IOException if an I/O error occurs
     */
    public static byte[] writePNG(Image im) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(65536);
        // this might conflict with user-set value
        ImageIO.setUseCache(false);
        ImageIO.write(ImgUtil.fromFXImage(im, null), "PNG", out);
        return out.toByteArray();
    }
}
