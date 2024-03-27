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

package attenuation;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;

/**
 * Utility class for creating screenshots.
 */
final class CaptureUtils {

    enum Format {
        BMP,
        GIF,
        JPG,
        PNG,
        TIF
    }

    private static final Path DIRECTORY = Path.of("screenshots");

    private static int imageNum = 1;

    static void capture(Image fxImage, Format extension) {
        BufferedImage image = SwingFXUtils.fromFXImage(fxImage, null);
        BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        rgbImage.getGraphics().drawImage(image, 0, 0, null);

        String formatName = extension.name().toLowerCase();
        var file = DIRECTORY.resolve(Path.of("screenshot" + imageNum + "." + formatName)).toAbsolutePath().toFile();
        try {
            Files.createDirectories(DIRECTORY);
            if (!ImageIO.write(rgbImage, formatName, file)) {
                throw new IOException("No writer found for " + formatName);
            }
            Desktop.getDesktop().open(DIRECTORY.toAbsolutePath().toFile());
        } catch (IOException e) {
            e.printStackTrace();
            var alert = new Alert(AlertType.ERROR);
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
        imageNum++;
    }
}
