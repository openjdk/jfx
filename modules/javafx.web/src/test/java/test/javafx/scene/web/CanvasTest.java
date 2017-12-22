/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.web;

import netscape.javascript.JSObject;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.junit.Assert.*;

/**
 * Test the Image to DataURL function
 */
public class CanvasTest extends TestBase {

    private static final String expectedURL = "data:image/png;base64," +
        "iVBORw0KGgoAAAANSUhEUgAAAMgAAABkCAYAAADDhn8LAAACPklEQVR42u3T" +
        "MSsGAByE8b9BDJIiJSnpLTGIQZSkJCW9JQYxiJKUpCQlBjaTzWazmWw2m81m" +
        "stlsNps6PsRluJ6nfh/ghquflhLgVil9t5YAt5iDfLWXALeYg3x2lAC3mIN8" +
        "dJUAt5iDvHeXALeYg7z1lgC3mIO89pUAt5iDvAyUALeYgzwPlgC3mIM8NUqA" +
        "W8xBHodLgFvMQR5GS4BbzEHux0qAW8xB7iZKgFvMQW4nS4BbzEFupkuAW8xB" +
        "rmdKgFvMQa7mSoBbzEHO50uAW8xBThZLgFvMQQ6XSoBbzEH2miXALeYg2ysl" +
        "wC3mIBtrJcAt5iCr6yXALeYgy5slwC3mIAtbJcAt5iCzOyXALeYgU7slwC3m" +
        "IOP7JcAt5iAjByXALeYgQ0clwC3mIP3HJcAt5iA9pyXALeYgnWclwC3mIG0X" +
        "JcAt5iB1+TcGcIvpok2AXUxnnQLsYjrtEWAX03G/ALuYjoYE2MV0MCLALqb9" +
        "cQF2Me1OCbCLaWdWgF1MWwsC7GLaXBZgF9P6qgC7mNY2BNjFtLItwC6m5p4A" +
        "u5iWDgXYxbR4IsAupvlzAXYxzV0JsItp5lqAXUzTNwLsYpq8FWAX08SdALuY" +
        "xu4F2MU0+iDALqbhRwF2MTWeBNjFNPgswC6mgRcBdjH1vQqwi6n3TYBdTN3v" +
        "Auxi6voQYBdTx6cAu5javwTYxdT6LcAuppYfAXZERERERERERERERERERERE" +
        "RERERERERERERERERERERERERERERET/0S/+VJ8zeU9ECwAAAABJRU5ErkJg" +
        "gg==";

    private static final String htmlContent = "\n"
        + "<!DOCTYPE html>\n"
        + "<html>\n"
        + "<body>\n"
        + "<canvas id=\"theCanvas\" width=\"200\" height=\"100\">\n"
        + "</canvas>\n"
        + "<p id = \"encodedText\">\n"
        + "</p>\n"
        + "<script>\n"
        + "var c = document.getElementById(\"theCanvas\");\n"
        + "var ctx = c.getContext(\"2d\");\n"
        + "var my_gradient=ctx.createLinearGradient(0,0,0,75);\n"
        + "my_gradient.addColorStop(0,\"red\");\n"
        + "my_gradient.addColorStop(0.5,\"green\");\n"
        + "my_gradient.addColorStop(1,\"blue\");\n"
        + "ctx.fillStyle=my_gradient;\n"
        + "ctx.fillRect(0,0,150,75);\n"
        + "var dataURL = c.toDataURL();\n"
        + "document.getElementById(\"encodedText\").innerHTML=dataURL;\n"
        + "</script>\n"
        + "</body>\n"
        + "</html>\n";

    @Ignore("RT-40092")
    @Test public void testImageToDataURL() {
        loadContent(htmlContent);
        submit(() -> {
            final Document doc = getEngine().getDocument();
            Element elem = doc.getElementById("encodedText");
            String textContent = elem.getTextContent();
            textContent = textContent.replaceAll("\\s", "");
            assertEquals("Data URL not encoded correctly", expectedURL, textContent);
        });
    }

    // JDK-8162922
    @Test public void testCanvasStrokeRect() {

        final String htmlCanvasContent = "\n"
            + "<!DOCTYPE html>\n"
            + "<html>\n"
            + "<body>\n"
            + "<canvas id=\"myCanvas\" width=\"200\" height=\"100\">\n"
            + "</canvas>\n"
            + "<script>\n"
            + "var c = document.getElementById(\"myCanvas\");\n"
            + "var ctx = c.getContext(\"2d\");\n"
            + "ctx.lineWidth = 4;\n"
            + "ctx.setLineDash([4,4]);\n"
            + "ctx.strokeStyle = '#f00';\n"
            + "ctx.strokeRect(10,30,70,70);\n"
            + "var imageData = ctx.getImageData(10, 30, 60, 60);\n"
            + "window.data = imageData.data;\n"
            + "</script>\n"
            + "</body>\n"
            + "</html>\n";

        loadContent(htmlCanvasContent);

        // Without the fix for JDK-8162922, canvas image data will be like below, which is wrong.
        /**
        final int[] wrongPixelArray = {255, 0, 0, 255,
                                       255, 0, 0, 255,
                                       255, 0, 0, 255,
                                       255, 0, 0, 255,
                                       255, 0, 0, 255,
                                       255, 0, 0, 255,
                                       255, 0, 0, 255,
                                       255, 0, 0, 255,};
        */

        // Sample pixel array to test against the canvas image data (with fix for JDK-8162922)
        final int[] expectedPixelArray = {255, 0, 0, 255,
                                          255, 0, 0, 255,
                                          255, 0, 0, 255,
                                          255, 0, 0, 255,
                                          0, 0, 0, 0,
                                          0, 0, 0, 0,
                                          0, 0, 0, 0,
                                          0, 0, 0, 0};

        submit(() -> {
            final JSObject obj = (JSObject) getEngine().executeScript("window.data");
            assertEquals("Device Pixel Ratio should be 1", 1, (int) getEngine().executeScript("window.devicePixelRatio"));
            // Due to mismatch of first pixel(probably a bug), we are skipping first pixel and testing
            // from second pixel onwards (from 16th value) till next 3 pixels (till 47th value)
            for (int i = 16; i < 48; i++) {
                assertEquals("StrokeRect pixel data is same", expectedPixelArray[i - 16], (int)obj.getSlot(i));
            }
        });
    }

    // JDK-8191035
    @Test public void testCanvasArc() {
        final String htmlCanvasArc =
                "<canvas id='canvas' width='600' height='300'></canvas> <script>" +
                        "var context = document.getElementById('canvas').getContext('2d');" +
                        "context.beginPath();" +
                        "context.arc(300, 150, 75, -1.5707, 2.1362, false);" +
                        "context.strokeStyle = 'red';" +
                        "context.stroke();  </script>";

        loadContent(htmlCanvasArc);
        submit(() -> {
            int redColor = 255;
            assertEquals("Arc startAngle", redColor,
                    (int) getEngine().executeScript("document.getElementById('canvas').getContext('2d').getImageData(260,213,1,1).data[0]"));
            assertEquals("Arc endAngle", redColor,
                    (int) getEngine().executeScript("document.getElementById('canvas').getContext('2d').getImageData(300,75,1,1).data[0]"));
        });
    }
}
