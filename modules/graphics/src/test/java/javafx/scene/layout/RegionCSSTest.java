/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.layout;

import com.sun.javafx.pgstub.StubImageLoaderFactory;
import com.sun.javafx.pgstub.StubPlatformImageInfo;
import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static javafx.scene.layout.BackgroundSize.*;
import static org.junit.Assert.*;

/**
 * All of these tests are based on information contained in the CSS 3 borders
 * and backgrounds specification: http://www.w3.org/TR/css3-background/
 *
 * The "Example" tests are pulled directly from one of the examples in the
 * spec, and is numbered such as example1_3, meaning "Example 1" in the spec
 * which is a box with multiple declarations, so declaration 3 (or whatnot).
 */
public class RegionCSSTest {
    // The color is drawn behind any background images.
    // The background color is clipped according to the ???background-clip??? value associated with the bottom-most background image.
    // Background images are drawn such that the first specified is on "top", closer to the user. But our BackgroundFill's are completely backwards from that!

    // Test "none" in terms of supplying an insets to the first and third but not specifying the second?
    // Failures to load images should result in "none" equivalence, not in exceptions
    //
    private Region region;
    private Scene scene;
    private static final String NL = System.getProperty("line.separator");

    private void processCSS() {
        scene.getRoot().impl_processCSS(true);
    }

    private static void installImage(final String str) {

        if (str == null || str.trim().isEmpty()) return;

        URL imageUrl = null;

        try {

            URI uri =  new URI(str.trim());

            // if url doesn't have a scheme
            if (uri.isAbsolute() == false) {

                final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                final String path = uri.getPath();

                URL resource = null;

                if (path.startsWith("/")) {
                    resource = contextClassLoader.getResource(path.substring(1));
                } else {
                    resource = contextClassLoader.getResource(path);
                }

                imageUrl = resource;

            } else {
                // else, url does have a scheme
                imageUrl = uri.toURL();
            }

        } catch (MalformedURLException malf) {

        } catch (URISyntaxException urise) {
        }

        if (imageUrl != null) {

            StubImageLoaderFactory imageLoaderFactory =
                    ((StubToolkit) Toolkit.getToolkit()).getImageLoaderFactory();

            imageLoaderFactory.registerImage(
                    imageUrl.toExternalForm(), new StubPlatformImageInfo(100, 100));
        }

    }
    
    @Before public void setUp() {
        region = new Region();
        scene = new Scene(region);

        installImage("javafx/scene/layout/red.png");
        installImage("javafx/scene/layout/green.png");
        installImage("javafx/scene/layout/blue.png");
        installImage("javafx/scene/layout/center-btn.png");
    }

    /**************************************************************************
     *                                                                        *
     * Basic tests                                                            *
     *                                                                        *
     *************************************************************************/

    @Test public void nothingSpecified() {
        region.setStyle("-fx-padding: 1;");
        processCSS();

        assertEquals(new Insets(1), region.getPadding());
        assertNull(region.getBackground());
        assertNull(region.getBorder());
    }

    /**************************************************************************
     *                                                                        *
     * Background Color tests                                                 *
     *                                                                        *
     *  These include -fx-background-color, -fx-background-radius, and        *
     *  -fx-background-insets.                                                *
     *                                                                        *
     *************************************************************************/

    @Test public void fillIsNull() {
        region.setStyle("-fx-background-color: null;");
        processCSS();

        assertEquals(new Insets(0), region.getPadding());
        assertNull(region.getBackground());
        assertNull(region.getBorder());
    }

    @Test public void fillIsTransparent() {
        region.setStyle("-fx-background-color: transparent;");
        processCSS();

        assertNull(region.getBorder());
        assertEquals(1, region.getBackground().getFills().size(), 0);

        BackgroundFill fill = region.getBackground().getFills().get(0);
        BackgroundFill expected = new BackgroundFill(Color.TRANSPARENT, null, null);
        assertEquals(expected, fill);
    }

    @Test public void fillIsSpecifiedOnly() {
        region.setStyle("-fx-background-color: purple;");
        processCSS();

        assertNull(region.getBorder());
        assertEquals(1, region.getBackground().getFills().size(), 0);

        BackgroundFill fill = region.getBackground().getFills().get(0);
        BackgroundFill expected = new BackgroundFill(Color.PURPLE, null, null);
        assertEquals(expected, fill);
    }

    @Test public void insetsIsSpecifiedOnly() {
        region.setStyle("-fx-background-insets: 1;");
        processCSS();

        assertNull(region.getBackground());
        assertNull(region.getBorder());
    }

    @Test public void radiusIsSpecifiedOnly() {
        region.setStyle("-fx-background-radius: 1;");
        processCSS();

        assertNull(region.getBackground());
        assertNull(region.getBorder());
    }

    @Test public void testWithExcessInsets() {
        region.setStyle(
                "-fx-background-color: red;" +
                "-fx-background-insets: 0 0 -1 0, 0, 1, 2;");
        processCSS();

        assertEquals(1, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundFill fill = region.getBackground().getFills().get(0);
        BackgroundFill expected = new BackgroundFill(Color.RED, null, new Insets(0, 0, -1, 0));
        assertEquals(expected, fill);
    }

    @Test public void testWithExcessRadius() {
        region.setStyle(
                "-fx-background-color: red;" +
                "-fx-background-radius: 0 0 0 0, 0, 1, 2;");
        processCSS();

        assertEquals(1, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundFill fill = region.getBackground().getFills().get(0);
        BackgroundFill expected = new BackgroundFill(Color.RED, new CornerRadii(0), null);
        assertEquals(expected, fill);
    }

    @Test public void backgroundScenario1() {
        region.setStyle(
                "-fx-background-color: red;" +
                "-fx-background-insets: 0 0 -1 0, 0, 1, 2;" +
                "-fx-background-radius: 1 2 3 4;" +
                "-fx-padding: 10 20 30 40;");
        processCSS();

        assertEquals(new Insets(10, 20, 30, 40), region.getPadding());
        assertEquals(1, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundFill fill = region.getBackground().getFills().get(0);
        BackgroundFill expected = new BackgroundFill(Color.RED, new CornerRadii(1, 2, 3, 4, false), new Insets(0, 0, -1, 0));
        assertEquals(expected, fill);
    }

    @Test public void backgroundScenario2() {
        region.setStyle(
                "-fx-background-color: red, green, blue, yellow;" +
                "-fx-background-insets: 0 0 -1 0, 1;" +
                "-fx-background-radius: 1 2 3 4, 5, 6 7 8 9;" +
                "-fx-padding: 10 20 30 40;");
        processCSS();

        assertEquals(new Insets(10, 20, 30, 40), region.getPadding());
        assertEquals(4, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundFill fill = region.getBackground().getFills().get(0);
        BackgroundFill expected = new BackgroundFill(Color.RED, new CornerRadii(1, 2, 3, 4, false), new Insets(0, 0, -1, 0));
        assertEquals(expected, fill);

        fill = region.getBackground().getFills().get(1);
        expected = new BackgroundFill(Color.GREEN, new CornerRadii(5), new Insets(1));
        assertEquals(expected, fill);

        fill = region.getBackground().getFills().get(2);
        expected = new BackgroundFill(Color.BLUE, new CornerRadii(6, 7, 8, 9, false), new Insets(1));
        assertEquals(expected, fill);

        fill = region.getBackground().getFills().get(3);
        expected = new BackgroundFill(Color.YELLOW, new CornerRadii(6, 7, 8, 9, false), new Insets(1));
        assertEquals(expected, fill);
    }

    @Test public void backgroundScenario3() {
        region.setStyle(
                "-fx-background-color: red, green, blue, yellow;" +
                "-fx-background-insets: 0 0 -1 0, 0, 1, 2;" +
                "-fx-background-radius: 1 2 3 4;" +
                "-fx-padding: 10 20 30 40;");
        processCSS();

        assertEquals(new Insets(10, 20, 30, 40), region.getPadding());
        assertEquals(4, region.getBackground().getFills().size(), 0);

        BackgroundFill fill = region.getBackground().getFills().get(0);
        BackgroundFill expected = new BackgroundFill(Color.RED,  new CornerRadii(1, 2, 3, 4, false), new Insets(0, 0, -1, 0));
        assertEquals(expected, fill);

        fill = region.getBackground().getFills().get(1);
        expected = new BackgroundFill(Color.GREEN,  new CornerRadii(1, 2, 3, 4, false), new Insets(0));
        assertEquals(expected, fill);

        fill = region.getBackground().getFills().get(2);
        expected = new BackgroundFill(Color.BLUE,  new CornerRadii(1, 2, 3, 4, false), new Insets(1));
        assertEquals(expected, fill);

        fill = region.getBackground().getFills().get(3);
        expected = new BackgroundFill(Color.YELLOW,  new CornerRadii(1, 2, 3, 4, false), new Insets(2));
        assertEquals(expected, fill);
    }

    /**
     * From the specification: http://www.w3.org/TR/css3-background/
     * This is "Example 1", except modified for multiple background fills
     */
    @Test public void specExample1_ModifiedForBackgroundFill() {
        region.setStyle(
                "-fx-background-color: red, green, blue;" +
                "-fx-background-insets: 1, 2, 3, 4;" + // An extra value here, which should be ignored
                "-fx-background-radius: 1;");
        processCSS();

        assertEquals(3, region.getBackground().getFills().size());

        BackgroundFill fill = region.getBackground().getFills().get(0);
        BackgroundFill expected = new BackgroundFill(Color.RED,  new CornerRadii(1), new Insets(1));
        assertEquals(expected, fill);

        fill = region.getBackground().getFills().get(1);
        expected = new BackgroundFill(Color.GREEN,  new CornerRadii(1), new Insets(2));
        assertEquals(expected, fill);

        fill = region.getBackground().getFills().get(2);
        expected = new BackgroundFill(Color.BLUE,  new CornerRadii(1), new Insets(3));
        assertEquals(expected, fill);
    }

    // See example 23 in http://www.w3.org/TR/css3-background/#the-border-radius
    @Test public void testBackgroundRadiusWithHorizontalAndVerticalRadii() {

        region.setStyle("-fx-background-color: black; -fx-background-radius: 2px 1px 4px / 0.5px 3px;");
        processCSS();

        assertNull(region.getBorder());
        assertEquals(1, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);

        BackgroundFill fill = region.getBackground().getFills().get(0);
        BackgroundFill expected = new BackgroundFill(Color.BLACK,
                new CornerRadii(2, .5, 3, 1, 4, .5, 3, 1, false, false, false, false, false, false, false, false),
                Insets.EMPTY);

        assertEquals(expected, fill);

    }


    /**************************************************************************
     *                                                                        *
     * Background Image tests                                                 *
     *                                                                        *
     *  These include -fx-background-image, -fx-background-repeat,            *
     *  -fx-background-position, -fx-background-clip, -fx-background-origin,  *
     *  and -fx-background-size. -fx-background-attachment does not yet exist *
     *  and is not supported.                                                 *
     *                                                                        *
     *************************************************************************/


    /**
     * From the specification: http://www.w3.org/TR/css3-background/.
     *
     * This is something of a comprehensive test, whereas most of the unit tests
     * in this file are checking one specific aspect of the functionality tested
     * herein.
     */
    @Test public void specExample1() {
        region.setStyle(
                "-fx-background-image: url(javafx/scene/layout/red.png), url(javafx/scene/layout/green.png), url(javafx/scene/layout/blue.png);" +
                "-fx-background-position: center center, 20% 80%, top left, bottom right;" +
                // TODO re-enable once I know how to test for background-origin
                //"-fx-background-origin: border-box, content-box;" +
                "-fx-background-repeat: no-repeat;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(3, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                new BackgroundPosition(Side.LEFT, .5, true, Side.TOP, .5, true),
                BackgroundSize.DEFAULT);
        assertEquals(expected, image);

        image = region.getBackground().getImages().get(1);
        expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                new BackgroundPosition(Side.LEFT, .2, true, Side.TOP, .8, true),
                BackgroundSize.DEFAULT);
        assertEquals(expected, image);

        image = region.getBackground().getImages().get(2);
        expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.DEFAULT,
                BackgroundSize.DEFAULT);
        assertEquals(expected, image);
    }

    @Test public void backgroundImageRepeat_repeatX() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-repeat: repeat-x;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.DEFAULT,
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundImageRepeat_repeatY() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-repeat: repeat-y;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.DEFAULT,
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundImageRepeat_repeat() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-repeat: repeat;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.DEFAULT,
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundImageRepeat_space() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-repeat: space;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.SPACE, BackgroundRepeat.SPACE,
                BackgroundPosition.DEFAULT,
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundImageRepeat_round() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-repeat: round;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.ROUND, BackgroundRepeat.ROUND,
                BackgroundPosition.DEFAULT,
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundImageRepeat_noRepeat() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-repeat: no-repeat;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.DEFAULT,
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundImageRepeat_repeat_space() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-repeat: repeat space;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.SPACE,
                BackgroundPosition.DEFAULT,
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundImageRepeat_round_noRepeat() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-repeat: round no-repeat;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.ROUND, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.DEFAULT,
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    // try "center left"
    // try "50% left" -- shouldn't work
    // try 3 values... remaining one should be 0
    // If only one value is specified, the second value is assumed to be ???center??? -- whatever this means...
    @Test public void backgroundImagePosition_right_bottom() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-position: right 20px bottom 10px;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                new BackgroundPosition(Side.RIGHT, 20, false, Side.BOTTOM, 10, false),
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    private static String dump(BackgroundImage image) {
        StringBuilder b = new StringBuilder("BackgroundImage[");
        b.append(NL + "  image: " + image.getImage());
        b.append(NL + "  position: " + dump(image.getPosition()));
        b.append(NL + "  repeat-x: " + image.getRepeatX());
        b.append(NL + "  repeat-y: " + image.getRepeatY());
        b.append(NL + "  size: " + dump(image.getSize()));
        b.append(NL + "]");
        return b.toString();
    }
    
    private static String dump(BackgroundSize size) {
        StringBuilder b = new StringBuilder("BackgroundSize[");
        b.append(NL + "    width: " + size.getWidth());
        b.append(NL + "    height: " + size.getHeight());
        b.append(NL + "    widthAsPercentage: " + size.isHeightAsPercentage());
        b.append(NL + "    heightAsPercentage: " + size.isWidthAsPercentage());
        b.append(NL + "    contain: " + size.isContain());
        b.append(NL + "    cover: " + size.isCover());
        b.append(NL + "  ]");
        return b.toString();
    }
    
    private static String dump(BackgroundPosition position) {
        StringBuilder b = new StringBuilder("BackgroundPosition[");
        b.append(NL + "    hSide: " + position.getHorizontalSide());
        b.append(NL + "    hPosition: " + position.getHorizontalPosition());
        b.append(NL + "    hAsPercentage: " + position.isHorizontalAsPercentage());
        b.append(NL + "    vSide: " + position.getVerticalSide());
        b.append(NL + "    vPosition: " + position.getVerticalPosition());
        b.append(NL + "    vAsPercentage: " + position.isVerticalAsPercentage());
        b.append(NL + "  ]");
        return b.toString();
    }
    
    @Test public void backgroundImagePosition_bottom_right() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-position: bottom 10px right 20px;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                new BackgroundPosition(Side.RIGHT, 20, false, Side.BOTTOM, 10, false),
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundImagePosition_top() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-position: top;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                new BackgroundPosition(Side.LEFT, .5, true, Side.TOP, 0, true),
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test  public void backgroundImagePosition_left() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-position: left;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                new BackgroundPosition(Side.LEFT, 0, true, Side.TOP, .5, true),
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundImagePosition_center() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-position: center;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                new BackgroundPosition(Side.LEFT, .5, true, Side.TOP, .5, true),
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundImagePosition_right() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-position: right;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                new BackgroundPosition(Side.LEFT, 1, true, Side.TOP, .5, true),
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundImagePosition_bottom() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-position: bottom;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                new BackgroundPosition(Side.LEFT, .5, true, Side.TOP, 1, true),
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundImagePosition_center_top() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-position: center top;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                new BackgroundPosition(Side.LEFT, .5, true, Side.TOP, 0, true),
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundImagePosition_Example8_1() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-position: left 10px top 15px;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                new BackgroundPosition(Side.LEFT, 10, false, Side.TOP, 15, false),
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundImagePosition_Example8_2() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-position: left top;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.DEFAULT,
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundImagePosition_Example8_3() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-position: 10px 15px;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                new BackgroundPosition(Side.LEFT, 10, false, Side.TOP, 15, false),
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundImagePosition_Example8_4() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-position: left 15px;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                new BackgroundPosition(Side.LEFT, 0, true, Side.TOP, 15, false),
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundImagePosition_Example8_5() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-position: 10px top;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                new BackgroundPosition(Side.LEFT, 10, false, Side.TOP, 0, true),
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundImagePosition_Example8_6() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-position: left top 15px;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                new BackgroundPosition(Side.LEFT, 0, true, Side.TOP, 15, false),
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundImagePosition_Example8_7() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-position: left 10px top;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                new BackgroundPosition(Side.LEFT, 10, false, Side.TOP, 0, true),
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundImagePosition_Example10_1() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-position: right top;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                new BackgroundPosition(Side.LEFT, 1, true, Side.TOP, 0, true),
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundImagePosition_Example10_2() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-position: top center;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                new BackgroundPosition(Side.LEFT, .5, true, Side.TOP, 0, true),
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundImagePosition_Example11() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-position: 100% 100%;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                new BackgroundPosition(Side.LEFT, 1, true, Side.TOP, 1, true),
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundImagePosition_75Percent() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-position: 75% 75%;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                new BackgroundPosition(Side.LEFT, .75, true, Side.TOP, .75, true),
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundImagePosition_Example12() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-position: right 10% bottom 10%;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                new BackgroundPosition(Side.RIGHT, .1, true, Side.BOTTOM, .1, true),
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    @Ignore("We do not presently implement -fx-background-clip")
    @Test public void backgroundClip_defaultValue() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

//        BackgroundImage image = region.getBackground().getImages().get(0);
        // TODO backgroundClip needs to be set to border-box by default
//        BackgroundImage expected = new BackgroundImage(image.getImage(), BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
//                0, .1, .1, 0, // top, right, bottom, left
//                1, 1,
//                true, true, // asHorizontalPercentage, asVerticalPercentage
//                true, true,
//                false, false);
//        assertEquals(expected, image);
    }

    @Ignore("We do not presently implement -fx-background-clip")
    @Test public void backgroundClip_BorderBox() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-clip: border-box");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

//        BackgroundImage image = region.getBackground().getImages().get(0);
//        BackgroundImage expected = new BackgroundImage(image.getImage(), BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
//                0, .1, .1, 0, // top, right, bottom, left
//                1, 1,
//                true, true, // asHorizontalPercentage, asVerticalPercentage
//                true, true,
//                false, false);
//        assertEquals(expected, image);
    }

    @Ignore("We do not presently implement -fx-background-clip")
    @Test public void backgroundClip_PaddingBox() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-clip: padding-box");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

//        BackgroundImage image = region.getBackground().getImages().get(0);
//        BackgroundImage expected = new BackgroundImage(image.getImage(), BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
//                0, .1, .1, 0, // top, right, bottom, left
//                1, 1,
//                true, true, // asHorizontalPercentage, asVerticalPercentage
//                true, true,
//                false, false);
//        assertEquals(expected, image);
    }

    @Ignore("We do not presently implement -fx-background-clip")
    @Test public void backgroundClip_ContentBox() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-clip: content-box");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

//        BackgroundImage image = region.getBackground().getImages().get(0);
//        BackgroundImage expected = new BackgroundImage(image.getImage(), BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
//                0, .1, .1, 0, // top, right, bottom, left
//                1, 1,
//                true, true, // asHorizontalPercentage, asVerticalPercentage
//                true, true,
//                false, false);
//        assertEquals(expected, image);
    }

    @Ignore("We do not presently implement -fx-background-origin")
    @Test public void backgroundOrigin_defaultValue() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

//        BackgroundImage image = region.getBackground().getImages().get(0);
        // TODO backgroundOrigin needs to be set to padding-box by default
//        BackgroundImage expected = new BackgroundImage(image.getImage(), BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
//                0, .1, .1, 0, // top, right, bottom, left
//                1, 1,
//                true, true, // asHorizontalPercentage, asVerticalPercentage
//                true, true,
//                false, false);
//        assertEquals(expected, image);
    }

    @Ignore("We do not presently implement -fx-background-origin")
    @Test public void backgroundOrigin_BorderBox() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-origin: border-box");
        processCSS();

//        assertEquals(0, region.getBackground().getFills().size(), 0);
//        assertEquals(1, region.getBackground().getImages().size(), 0);
//        assertEquals(0, region.getImageBorders().size(), 0);
//        assertEquals(0, region.getStrokeBorders().size(), 0);

//        BackgroundImage image = region.getBackground().getImages().get(0);
//        BackgroundImage expected = new BackgroundImage(image.getImage(), BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
//                0, .1, .1, 0, // top, right, bottom, left
//                1, 1,
//                true, true, // asHorizontalPercentage, asVerticalPercentage
//                true, true,
//                false, false);
//        assertEquals(expected, image);
    }

    @Ignore("We do not presently implement -fx-background-origin")
    @Test public void backgroundOrigin_PaddingBox() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-origin: padding-box");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

//        BackgroundImage image = region.getBackground().getImages().get(0);
//        BackgroundImage expected = new BackgroundImage(image.getImage(), BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
//                0, .1, .1, 0, // top, right, bottom, left
//                1, 1,
//                true, true, // asHorizontalPercentage, asVerticalPercentage
//                true, true,
//                false, false);
//        assertEquals(expected, image);
    }

    @Ignore("We do not presently implement -fx-background-origin")
    @Test public void backgroundOrigin_ContentBox() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-origin: content-box");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

//        BackgroundImage image = region.getBackground().getImages().get(0);
//        BackgroundImage expected = new BackgroundImage(image.getImage(), BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
//                0, .1, .1, 0, // top, right, bottom, left
//                1, 1,
//                true, true, // asHorizontalPercentage, asVerticalPercentage
//                true, true,
//                false, false);
//        assertEquals(expected, image);
    }

    @Test public void backgroundSize_defaultValue() {
        region.setStyle("-fx-background-image: url('javafx/scene/layout/red.png');");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.DEFAULT,
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundSize_cover() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-size: cover;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.DEFAULT,
                new BackgroundSize(AUTO, AUTO, true, true, false, true));
        assertEquals(expected, image);
    }

    @Test public void backgroundSize_contain() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-size: contain;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.DEFAULT,
                new BackgroundSize(AUTO, AUTO, true, true, true, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundSize_length() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-size: 170px;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.DEFAULT,
                new BackgroundSize(170, AUTO, false, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundSize_percent() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-size: 65%;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.DEFAULT,
                new BackgroundSize(.65, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundSize_auto() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-size: auto;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.DEFAULT,
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundSize_length_length() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-size: 10px 20px;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.DEFAULT,
                new BackgroundSize(10, 20, false, false, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundSize_length_percent() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-size: 50px 25%;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.DEFAULT,
                new BackgroundSize(50, .25, false, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundSize_length_auto() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-size: 40px auto;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.DEFAULT,
                new BackgroundSize(40, AUTO, false, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundSize_percent_length() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-size: 25% 30px;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.DEFAULT,
                new BackgroundSize(.25, 30, true, false, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundSize_percent_percent() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-size: 25% 75%;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.DEFAULT,
                new BackgroundSize(.25, .75, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundSize_percent_auto() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-size: 25% auto;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.DEFAULT,
                new BackgroundSize(.25, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundSize_auto_length() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-size: auto 25px;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.DEFAULT,
                new BackgroundSize(AUTO, 25, true, false, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundSize_auto_percent() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-size: auto 50%;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.DEFAULT,
                new BackgroundSize(AUTO, .5, true, true, false, false));
        assertEquals(expected, image);
    }

    @Test public void backgroundSize_auto_auto() {
        region.setStyle(
                "-fx-background-image: url('javafx/scene/layout/red.png');" +
                "-fx-background-size: auto auto;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(1, region.getBackground().getImages().size(), 0);
        assertNull(region.getBorder());

        BackgroundImage image = region.getBackground().getImages().get(0);
        BackgroundImage expected = new BackgroundImage(image.getImage(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.DEFAULT,
                new BackgroundSize(AUTO, AUTO, true, true, false, false));
        assertEquals(expected, image);
    }


    /**************************************************************************
     *                                                                        *
     * Stroke Border tests                                                    *
     *                                                                        *
     *  These include -fx-border-color, -fx-border-style, and                 *
     *  -fx-border-width properties.                                          *
     *                                                                        *
     *************************************************************************/

    @Test public void borderStrokeStyleIsNull() {
        region.setStyle("-fx-border-style: null;");
        processCSS();

        assertEquals(new Insets(0), region.getPadding());
        assertNull(region.getBackground());
        assertNull(region.getBorder());
    }

    @Ignore("-fx-border-style-top is not supported")
    @Test public void borderStyle_top() {
        region.setStyle("-fx-border-style-top: solid;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(
                Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK,
                BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID,
                null, null, Insets.EMPTY);
        assertEquals(expected, stroke);
    }

    @Ignore("-fx-border-style-right is not supported")
    @Test public void borderStyle_right() {
        region.setStyle("-fx-border-style-right: solid;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(
                Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK,
                BorderStrokeStyle.NONE, BorderStrokeStyle.SOLID, BorderStrokeStyle.NONE, BorderStrokeStyle.SOLID,
                null, null, Insets.EMPTY);
        assertEquals(expected, stroke);
    }

    @Ignore("-fx-border-style-bottom is not supported")
    @Test public void borderStyle_bottom() {
        region.setStyle("-fx-border-style-bottom: solid;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(
                Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK,
                BorderStrokeStyle.NONE, BorderStrokeStyle.NONE, BorderStrokeStyle.SOLID, BorderStrokeStyle.NONE,
                null, null, Insets.EMPTY);
        assertEquals(expected, stroke);
    }

    @Ignore("-fx-border-style-left is not supported")
    @Test public void borderStyle_left() {
        region.setStyle("-fx-border-style-left: solid;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(
                Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK,
                BorderStrokeStyle.NONE, BorderStrokeStyle.NONE, BorderStrokeStyle.NONE, BorderStrokeStyle.SOLID,
                null, null, Insets.EMPTY);
        assertEquals(expected, stroke);
    }

    @Ignore("-fx-border-style-top and -fx-border-style-right are not supported")
    @Test public void borderStyle_top_right() {
        region.setStyle(
                "-fx-border-style-top: solid;" +
                "-fx-border-style-right: dashed;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(
                Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK,
                BorderStrokeStyle.SOLID, BorderStrokeStyle.DASHED, BorderStrokeStyle.SOLID, BorderStrokeStyle.DASHED,
                null, null, Insets.EMPTY);
        assertEquals(expected, stroke);
    }

    @Ignore("-fx-border-style-top and -fx-border-style-bottom are not supported")
    @Test public void borderStyle_bottom_top() {
        region.setStyle(
                "-fx-border-style-top: solid;" +
                "-fx-border-style-bottom: dashed;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(
                Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK,
                BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.DASHED, BorderStrokeStyle.SOLID,
                null, null, Insets.EMPTY);
        assertEquals(expected, stroke);
    }

    @Ignore("-fx-border-style-bottom and -fx-border-style-left are not supported")
    @Test public void borderStyle_left_bottom() {
        region.setStyle(
                "-fx-border-style-bottom: solid;" +
                "-fx-border-style-left: dashed;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(
                Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK,
                BorderStrokeStyle.NONE, BorderStrokeStyle.NONE, BorderStrokeStyle.SOLID, BorderStrokeStyle.DASHED,
                null, null, Insets.EMPTY);
        assertEquals(expected, stroke);
    }

    @Test public void borderStyle_none() {
        region.setStyle("-fx-border-style: none;");
        processCSS();

        assertEquals(new Insets(0), region.getPadding());
        assertNull(region.getBackground());
        assertNull(region.getBorder());
    }

    @Test public void borderStyle_hidden() {
        region.setStyle("-fx-border-style: hidden;");
        processCSS();

        assertEquals(new Insets(0), region.getPadding());
        assertNull(region.getBackground());
        assertNull(region.getBorder());
    }

    @Test public void borderStyle_dotted() {
        region.setStyle("-fx-border-color: black; -fx-border-style: dotted;");
        processCSS();

        assertNull(region.getBackground());
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(
                Color.BLACK, BorderStrokeStyle.DOTTED,
                null, null);
        assertEquals(expected, stroke);
    }

    // border-style: dashed
    @Test public void borderStyle_dashed() {
        region.setStyle("-fx-border-color: black; -fx-border-style: dashed;");
        processCSS();

        assertNull(region.getBackground());
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(
                Color.BLACK, BorderStrokeStyle.DASHED,
                null, null);
        assertEquals(expected, stroke);
    }

    @Test public void borderStyle_solid() {
        region.setStyle("-fx-border-color: black; -fx-border-style: solid;");
        processCSS();

        assertNull(region.getBackground());
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(
                Color.BLACK, BorderStrokeStyle.SOLID,
                null, null);
        assertEquals(expected, stroke);
    }

    @Ignore ("double not supported yet")
    @Test public void borderStyle_double() {
        region.setStyle("-fx-border-color: black; -fx-border-style: double;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
//        BorderStroke expected = new BorderStroke(
//                Color.BLACK, BorderStrokeStyle.DOUBLE,
//                null, null);
//        assertEquals(expected, stroke);
    }

    @Ignore ("groove not supported yet")
    @Test public void borderStyle_groove() {
        region.setStyle("-fx-border-color: black; -fx-border-style: groove;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
//        BorderStroke expected = new BorderStroke(
//                Color.BLACK, BorderStrokeStyle.GROOVE,
//                null, null);
//        assertEquals(expected, stroke);
    }

    @Ignore ("ridge not supported yet")
    @Test public void borderStyle_ridge() {
        region.setStyle("-fx-border-color: black; -fx-border-style: ridge;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
//        BorderStroke expected = new BorderStroke(
//                Color.BLACK, BorderStrokeStyle.RIDGE,
//                null, null);
//        assertEquals(expected, stroke);
    }

    @Ignore ("inset not supported yet")
    @Test public void borderStyle_inset() {
        region.setStyle("-fx-border-color: black; -fx-border-style: inset;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
//        BorderStroke expected = new BorderStroke(
//                Color.BLACK, BorderStrokeStyle.INSET,
//                null, null);
//        assertEquals(expected, stroke);
    }

    @Ignore ("outset not supported yet")
    @Test public void borderStyle_outset() {
        region.setStyle("-fx-border-color: black; -fx-border-style: outset;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
//        BorderStroke expected = new BorderStroke(
//                Color.BLACK, BorderStrokeStyle.OUTSET,
//                null, null);
//        assertEquals(expected, stroke);
    }

    @Test public void borderStyle_solid_dotted() {
        region.setStyle("-fx-border-color: black; -fx-border-style: solid dotted;");
        processCSS();

        assertNull(region.getBackground());
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(
                Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK,
                BorderStrokeStyle.SOLID, BorderStrokeStyle.DOTTED, BorderStrokeStyle.SOLID, BorderStrokeStyle.DOTTED,
                null, null, Insets.EMPTY);
        assertEquals(expected, stroke);
    }

    @Test public void borderStyle_solid_dotted_dashed() {
        region.setStyle("-fx-border-color: black; -fx-border-style: solid dotted dashed;");
        processCSS();

        assertNull(region.getBackground());
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(
                Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK,
                BorderStrokeStyle.SOLID, BorderStrokeStyle.DOTTED, BorderStrokeStyle.DASHED, BorderStrokeStyle.DOTTED,
                null, null, Insets.EMPTY);
        assertEquals(expected, stroke);
    }

    @Ignore ("ridge not supported yet")
    @Test public void borderStyle_solid_dotted_dashed_ridge() {
        region.setStyle("-fx-border-color: black; -fx-border-style: solid dotted dashed ridge;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
//        BorderStroke expected = new BorderStroke(
//                Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK,
//                BorderStrokeStyle.SOLID, BorderStrokeStyle.DOTTED, BorderStrokeStyle.DASHED, BorderStrokeStyle.RIDGE,
//                null, null, Insets.EMPTY);
//        assertEquals(expected, stroke);
    }

    @Ignore ("-fx-border-width-top is not supported")
    @Test public void borderStrokeWidth_top() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-width-top: 10px;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, new BorderWidths(10, 0, 0, 0));
        assertEquals(expected, stroke);
    }

    @Ignore ("-fx-border-width-right is not supported")
    @Test public void borderStrokeWidth_right() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-width-right: 10px;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, new BorderWidths(0, 10, 0, 0));
        assertEquals(expected, stroke);
    }

    @Ignore ("-fx-border-width-bottom is not supported")
    @Test public void borderStrokeWidth_bottom() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-width-bottom: 10px;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, new BorderWidths(0, 0, 10, 0));
        assertEquals(expected, stroke);
    }

    @Ignore ("-fx-border-width-left is not supported")
    @Test public void borderStrokeWidth_left() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-width-left: 10px;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, new BorderWidths(0, 0, 0, 10));
        assertEquals(expected, stroke);
    }

    @Ignore ("-fx-border-width-top and -fx-border-width-right are not supported")
    @Test public void borderStrokeWidth_top_right() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-width-top: 10px;" +
                "-fx-border-width-right: 20px;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, new BorderWidths(10, 20, 0, 0));
        assertEquals(expected, stroke);
    }

    @Ignore ("-fx-border-width-top and -fx-border-width-bottom are not supported")
    @Test public void borderStrokeWidth_top_bottom() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-width-top: 10px;" +
                "-fx-border-width-bottom: 20px;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, new BorderWidths(10, 0, 20, 0));
        assertEquals(expected, stroke);
    }

    @Ignore ("-fx-border-width-left and -fx-border-width-bottom are not supported")
    @Test public void borderStrokeWidth_left_bottom() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-width-left: 10px;" +
                "-fx-border-width-bottom: 20px;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, new BorderWidths(0, 0, 20, 10));
        assertEquals(expected, stroke);
    }

    @Test public void borderStrokeWidth2() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-width: 1px 2px;");
        processCSS();

        assertNull(region.getBackground());
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, new BorderWidths(1, 2, 1, 2));
        assertEquals(expected, stroke);
    }

    @Test public void borderStrokeWidth3() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-width: 1px 2px 3px;");
        processCSS();

        assertNull(region.getBackground());
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, new BorderWidths(1, 2, 3, 2));
        assertEquals(expected, stroke);
    }

    @Test public void borderStrokeWidth4() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-width: 1px 2px 3px 4px;");
        processCSS();

        assertNull(region.getBackground());
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, new BorderWidths(1, 2, 3, 4));
        assertEquals(expected, stroke);
    }

    @Ignore("thin keyword is not supported")
    @Test public void borderStrokeWidth_thin() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-width: thin;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, new BorderWidths(1, 1, 1, 1));
        assertEquals(expected, stroke);
    }

    @Ignore("thick keyword is not supported")
    @Test public void borderStrokeWidth_thick() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-width: thick;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, new BorderWidths(5, 5, 5, 5));
        assertEquals(expected, stroke);
    }

    @Ignore("medium keyword is not supported")
    @Test public void borderStrokeWidth_medium() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-width: medium;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, new BorderWidths(3, 3, 3, 3));
        assertEquals(expected, stroke);
    }

    @Ignore("thin, medium, and thick keywords are not supported")
    @Test public void borderStrokeWidth_thin_medium_thick() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-width: thin medium thick;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, new BorderWidths(1, 3, 5, 3));
        assertEquals(expected, stroke);
    }

    // TODO example 20
    // TODO example 21

    // TODO!! The initial width of a border is MEDIUM, NOT 0!

    @Ignore("-fx-border-top-left-radius not supported")
    @Test public void borderStrokeRadius_topLeft1() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-top-left-radius: 5px;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(5, 0, 0, 0, false), BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Ignore("-fx-border-top-left-radius not supported")
    @Test public void borderStrokeRadius_topLeft2() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-top-left-radius: 5px, 10px;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(
                        5, 10, // top left
                        0, 0, // top right
                        0, 0, // bottom right
                        0, 0, // bottom left
                        false, false, // top left as percent
                        false, false, // top right as percent
                        false, false, // bottom right as percent
                        false, false), // bottom left as percent
                BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Ignore("-fx-border-top-left-radius not supported")
    @Test public void borderStrokeRadius_topLeft3() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-top-left-radius: 5%;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(
                        5, 5, // top left
                        0, 0, // top right
                        0, 0, // bottom right
                        0, 0, // bottom left
                        true, true, // top left as percent
                        false, false, // top right as percent
                        false, false, // bottom right as percent
                        false, false), // bottom left as percent
                BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Ignore("-fx-border-top-left-radius not supported")
    @Test public void borderStrokeRadius_topLeft4() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-top-left-radius: 5%, 10%;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(
                        5, 10, // top left
                        0, 0, // top right
                        0, 0, // bottom right
                        0, 0, // bottom left
                        true, true, // top left as percent
                        false, false, // top right as percent
                        false, false, // bottom right as percent
                        false, false), // bottom left as percent
                BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Ignore("-fx-border-top-left-radius not supported")
    @Test public void borderStrokeRadius_topLeft5() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-top-left-radius: 5% 10px;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(
                        5, 10, // top left
                        0, 0, // top right
                        0, 0, // bottom right
                        0, 0, // bottom left
                        true, false, // top left as percent
                        false, false, // top right as percent
                        false, false, // bottom right as percent
                        false, false), // bottom left as percent
                BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Ignore("-fx-border-top-left-radius not supported")
    @Test public void borderStrokeRadius_topLeft6() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-top-left-radius: 5px, 10%;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(
                        5, 10, // top left
                        0, 0, // top right
                        0, 0, // bottom right
                        0, 0, // bottom left
                        false, true, // top left as percent
                        false, false, // top right as percent
                        false, false, // bottom right as percent
                        false, false), // bottom left as percent
                BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Ignore("-fx-border-top-right-radius not supported")
    @Test public void borderStrokeRadius_topRight1() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-top-right-radius: 5px;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(0, 5, 0, 0, false), BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Ignore("-fx-border-top-right-radius not supported")
    @Test public void borderStrokeRadius_topRight2() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-top-right-radius: 5px, 10px;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(
                        0, 0, // top left
                        5, 10, // top right
                        0, 0, // bottom right
                        0, 0, // bottom left
                        false, false, // top left as percent
                        false, false, // top right as percent
                        false, false, // bottom right as percent
                        false, false), // bottom left as percent
                BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Ignore("-fx-border-top-right-radius not supported")
    @Test public void borderStrokeRadius_topRight3() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-top-right-radius: 5%;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(
                        0, 0, // top left
                        5, 5, // top right
                        0, 0, // bottom right
                        0, 0, // bottom left
                        false, false, // top left as percent
                        true, true, // top right as percent
                        false, false, // bottom right as percent
                        false, false), // bottom left as percent
                BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Ignore("-fx-border-top-right-radius not supported")
    @Test public void borderStrokeRadius_topRight4() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-top-right-radius: 5%, 10%;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(
                        0, 0, // top left
                        5, 10, // top right
                        0, 0, // bottom right
                        0, 0, // bottom left
                        false, false, // top left as percent
                        true, true, // top right as percent
                        false, false, // bottom right as percent
                        false, false), // bottom left as percent
                BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Ignore("-fx-border-top-right-radius not supported")
    @Test public void borderStrokeRadius_topRight5() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-top-right-radius: 5% 10px;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(
                        0, 0, // top left
                        5, 10, // top right
                        0, 0, // bottom right
                        0, 0, // bottom left
                        false, false, // top left as percent
                        true, false, // top right as percent
                        false, false, // bottom right as percent
                        false, false), // bottom left as percent
                BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Ignore("-fx-border-top-right-radius not supported")
    @Test public void borderStrokeRadius_topRight6() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-top-right-radius: 5px, 10%;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(
                        0, 0, // top left
                        5, 10, // top right
                        0, 0, // bottom right
                        0, 0, // bottom left
                        false, false, // top left as percent
                        false, true, // top right as percent
                        false, false, // bottom right as percent
                        false, false), // bottom left as percent
                BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Ignore("-fx-border-bottom-right-radius not supported")
    @Test public void borderStrokeRadius_bottomRight1() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-bottom-right-radius: 5px;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(0, 0, 5, 0, false), BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Ignore("-fx-border-bottom-right-radius not supported")
    @Test public void borderStrokeRadius_bottomRight2() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-bottom-right-radius: 5px, 10px;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(
                        0, 0, // top left
                        0, 0, // top right
                        5, 10, // bottom right
                        0, 0, // bottom left
                        false, false, // top left as percent
                        false, false, // top right as percent
                        false, false, // bottom right as percent
                        false, false), // bottom left as percent
                BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Ignore("-fx-border-bottom-right-radius not supported")
    @Test public void borderStrokeRadius_bottomRight3() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-bottom-right-radius: 5%;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(
                        0, 0, // top left
                        0, 0, // top right
                        5, 5, // bottom right
                        0, 0, // bottom left
                        false, false, // top left as percent
                        false, false, // top right as percent
                        true, true, // bottom right as percent
                        false, false), // bottom left as percent
                BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Ignore("-fx-border-bottom-right-radius not supported")
    @Test public void borderStrokeRadius_bottomRight4() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-bottom-right-radius: 5%, 10%;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(
                        0, 0, // top left
                        0, 0, // top right
                        5, 10, // bottom right
                        0, 0, // bottom left
                        false, false, // top left as percent
                        false, false, // top right as percent
                        true, true, // bottom right as percent
                        false, false), // bottom left as percent
                BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Ignore("-fx-border-bottom-right-radius not supported")
    @Test public void borderStrokeRadius_bottomRight5() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-bottom-right-radius: 5% 10px;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(
                        0, 0, // top left
                        0, 0, // top right
                        5, 10, // bottom right
                        0, 0, // bottom left
                        false, false, // top left as percent
                        false, false, // top right as percent
                        true, false, // bottom right as percent
                        false, false), // bottom left as percent
                BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Ignore("-fx-border-bottom-right-radius not supported")
    @Test public void borderStrokeRadius_bottomRight6() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-bottom-right-radius: 5px, 10%;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(
                        0, 0, // top left
                        0, 0, // top right
                        5, 10, // bottom right
                        0, 0, // bottom left
                        false, false, // top left as percent
                        false, false, // top right as percent
                        false, true, // bottom right as percent
                        false, false), // bottom left as percent
                BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Ignore("-fx-border-bottom-left-radius not supported")
    @Test public void borderStrokeRadius_bottomLeft1() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-bottom-left-radius: 5px;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(0, 0, 0, 5, false), BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Ignore("-fx-border-bottom-left-radius not supported")
    @Test public void borderStrokeRadius_bottomLeft2() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-bottom-left-radius: 5px, 10px;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(
                        0, 0, // top left
                        0, 0, // top right
                        0, 0, // bottom right
                        5, 10, // bottom left
                        false, false, // top left as percent
                        false, false, // top right as percent
                        false, false, // bottom right as percent
                        false, false), // bottom left as percent
                BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Ignore("-fx-border-bottom-left-radius not supported")
    @Test public void borderStrokeRadius_bottomLeft3() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-bottom-left-radius: 5%;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(
                        0, 0, // top left
                        0, 0, // top right
                        0, 0, // bottom right
                        5, 5, // bottom left
                        false, false, // top left as percent
                        false, false, // top right as percent
                        false, false, // bottom right as percent
                        true, true), // bottom left as percent
                BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Ignore("-fx-border-bottom-left-radius not supported")
    @Test public void borderStrokeRadius_bottomLeft4() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-bottom-left-radius: 5%, 10%;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(
                        0, 0, // top left
                        0, 0, // top right
                        0, 0, // bottom right
                        5, 10, // bottom left
                        false, false, // top left as percent
                        false, false, // top right as percent
                        false, false, // bottom right as percent
                        true, true), // bottom left as percent
                BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Ignore("-fx-border-bottom-left-radius not supported")
    @Test public void borderStrokeRadius_bottomLeft5() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-bottom-left-radius: 5% 10px;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(
                        0, 0, // top left
                        0, 0, // top right
                        0, 0, // bottom right
                        5, 10, // bottom left
                        false, false, // top left as percent
                        false, false, // top right as percent
                        false, false, // bottom right as percent
                        true, false), // bottom left as percent
                BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Ignore("-fx-border-bottom-left-radius not supported")
    @Test public void borderStrokeRadius_bottomLeft6() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-bottom-left-radius: 5px, 10%;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(
                        0, 0, // top left
                        0, 0, // top right
                        0, 0, // bottom right
                        5, 10, // bottom left
                        false, false, // top left as percent
                        false, false, // top right as percent
                        false, false, // bottom right as percent
                        false, true), // bottom left as percent
                BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Ignore("-fx-border-top-left-radius and -fx-border-top-right-radius are not supported")
    @Test public void borderStrokeRadius_topLeft_topRight() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-top-left-radius: 5px 10%;" +
                "-fx-border-top-right-radius: 20px 30%;");
                processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(
                        5, 10, // top left
                        20, 30, // top right
                        0, 0, // bottom right
                        0, 0, // bottom left
                        false, true, // top left as percent
                        false, true, // top right as percent
                        false, false, // bottom right as percent
                        false, false), // bottom left as percent
                BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Test public void borderStrokeRadius1() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-radius: 5px;");
        processCSS();

        assertNull(region.getBackground());
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(5, 5, 5, 5, false),
                BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Test public void borderStrokeRadius1_Percent() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-radius: 5%;");
        processCSS();

        assertNull(region.getBackground());
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(.05, .05, .05, .05, true),
                BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Test public void borderStrokeRadius2() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-radius: 5px 10px;");
        processCSS();

        assertNull(region.getBackground());
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(5, 10, 5, 10, false),
                BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Test public void borderStrokeRadius2_Percent() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-radius: 5% 10%;");
        processCSS();

        assertNull(region.getBackground());
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(.05, .10, .05, .10, true),
                BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Test public void borderStrokeRadius3() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-radius: 5px 10px 15px;");
        processCSS();

        assertNull(region.getBackground());
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(5, 10, 15, 10, false),
                BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Test public void borderStrokeRadius3_Percent() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-radius: 5% 10% 15%;");
        processCSS();

        assertNull(region.getBackground());
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(.05, .10, .15, .10, true),
                BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Test public void borderStrokeRadius4() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-radius: 5px 10px 15px 20px;");
        processCSS();

        assertNull(region.getBackground());
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(5, 10, 15, 20, false),
                BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    @Test public void borderStrokeRadius4_Percent() {
        region.setStyle(
                "-fx-border-color: black;" +
                "-fx-border-radius: 5% 10% 15% 20%;");
        processCSS();

        assertNull(region.getBackground());
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(.05, .10, .15, .20, true),
                BorderStroke.DEFAULT_WIDTHS);
        assertEquals(expected, stroke);
    }

    // TODO Example 22

    // http://www.w3.org/TR/css3-background/#the-border-radius
    // Example 23 (except using px here instead of em)
    @Test public void testBorderRadiusWithHorizontalAndVerticalRadii() {

        region.setStyle("-fx-border-color: black; -fx-border-radius: 2px 1px 4px / 0.5px 3px;");
        processCSS();

        assertNull(region.getBackground());
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                new CornerRadii(2, .5, 3, 1, 4, .5, 3, 1, false, false, false, false, false, false, false, false),
                BorderStroke.DEFAULT_WIDTHS);

        assertEquals(expected, stroke);

    }



    @Test public void borderStrokeIsTransparent() {
        region.setStyle(
                "-fx-border-style: solid;" +
                "-fx-border-color: transparent;");
        processCSS();

        assertNull(region.getBackground());
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.TRANSPARENT, BorderStrokeStyle.SOLID, null, null);
        assertEquals(expected, stroke);
    }

    @Test public void borderStrokeIsSpecifiedOnly() {
        region.setStyle(
                "-fx-border-style: solid;" +
                "-fx-border-color: red;");
        processCSS();

        assertNull(region.getBackground());
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, null, null);
        assertEquals(expected, stroke);
    }

    @Test public void borderStroke2IsSpecifiedOnly() {
        region.setStyle(
                "-fx-border-style: solid;" +
                "-fx-border-color: red green;");
        processCSS();

        assertNull(region.getBackground());
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(
                Color.RED, Color.GREEN, Color.RED, Color.GREEN,
                BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID,
                null, null, Insets.EMPTY);
        assertEquals(expected, stroke);
    }

    @Test public void borderStroke3IsSpecifiedOnly() {
        region.setStyle(
                "-fx-border-style: solid;" +
                "-fx-border-color: red green blue;");
        processCSS();

        assertNull(region.getBackground());
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(
                Color.RED, Color.GREEN, Color.BLUE, Color.GREEN,
                BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID,
                null, null, Insets.EMPTY);
        assertEquals(expected, stroke);
    }

    @Test public void borderStroke4IsSpecifiedOnly() {
        region.setStyle(
                "-fx-border-style: solid;" +
                "-fx-border-color: red green blue yellow;");
        processCSS();

        assertNull(region.getBackground());
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(
                Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
                BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID,
                null, null, Insets.EMPTY);
        assertEquals(expected, stroke);
    }

    @Ignore ("-fx-border-top-color is not supported by the CSS parser")
    @Test public void borderStroke_top_IsSpecifiedOnly() {
        region.setStyle(
                "-fx-border-style: solid;" +
                "-fx-border-top-color: purple;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(
                Color.PURPLE, Color.BLACK, Color.BLACK, Color.BLACK,
                BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID,
                null, null, Insets.EMPTY);
        assertEquals(expected, stroke);
    }

    @Ignore ("-fx-border-right-color is not supported by the CSS parser")
    @Test public void borderStroke_right_IsSpecifiedOnly() {
        region.setStyle(
                "-fx-border-style: solid;" +
                "-fx-border-right-color: purple;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(
                Color.BLACK, Color.PURPLE, Color.BLACK, Color.BLACK,
                BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID,
                null, null, Insets.EMPTY);
        assertEquals(expected, stroke);
    }

    @Ignore ("-fx-border-bottom-color is not supported by the CSS parser")
    @Test public void borderStroke_bottom_IsSpecifiedOnly() {
        region.setStyle(
                "-fx-border-style: solid;" +
                "-fx-border-bottom-color: purple;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(
                Color.BLACK, Color.BLACK, Color.PURPLE, Color.BLACK,
                BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID,
                null, null, Insets.EMPTY);
        assertEquals(expected, stroke);
    }

    @Ignore ("-fx-border-left-color is not supported by the CSS parser")
    @Test public void borderStroke_left_IsSpecifiedOnly() {
        region.setStyle(
                "-fx-border-style: solid;" +
                "-fx-border-left-color: purple;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(
                Color.BLACK, Color.BLACK, Color.BLACK, Color.PURPLE,
                BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID,
                null, null, Insets.EMPTY);
        assertEquals(expected, stroke);
    }

    @Ignore ("-fx-border-top-color and -fx-border-right-color is not supported by the CSS parser")
    @Test public void borderStroke_top_right_IsSpecifiedOnly() {
        region.setStyle(
                "-fx-border-style: solid;" +
                "-fx-border-top-color: red;" +
                "-fx-border-right-color: green;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(
                Color.RED, Color.GREEN, Color.BLACK, Color.BLACK,
                BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID,
                null, null, Insets.EMPTY);
        assertEquals(expected, stroke);
    }

    @Ignore ("-fx-border-left-color and -fx-border-right-color is not supported by the CSS parser")
    @Test public void borderStroke_right_left_IsSpecifiedOnly() {
        region.setStyle(
                "-fx-border-style: solid;" +
                "-fx-border-right-color: red;" +
                "-fx-border-left-color: green;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(
                Color.BLACK, Color.RED, Color.BLACK, Color.GREEN,
                BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID,
                null, null, Insets.EMPTY);
        assertEquals(expected, stroke);
    }

    @Ignore ("-fx-border-top-color and -fx-border-bottom-color is not supported by the CSS parser")
    @Test public void borderStroke_bottom_top_IsSpecifiedOnly() {
        region.setStyle(
                "-fx-border-style: solid;" +
                "-fx-border-bottom-color: red;" +
                "-fx-border-top-color: green;");
        processCSS();

        assertEquals(0, region.getBackground().getFills().size(), 0);
        assertEquals(0, region.getBackground().getImages().size(), 0);
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(
                Color.GREEN, Color.BLACK, Color.RED, Color.BLACK,
                BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID,
                null, null, Insets.EMPTY);
        assertEquals(expected, stroke);
    }

    @Test public void borderRadiusIsSpecifiedOnly() {
        region.setStyle("-fx-border-radius: 1px;");
        processCSS();

        assertNull(region.getBackground());
        assertNull(region.getBorder());
    }

    @Test public void borderWidthIsSpecifiedOnly() {
        region.setStyle("-fx-border-width: 1;");
        processCSS();

        assertNull(region.getBackground());
        assertNull(region.getBorder());
    }

    @Test public void testWithExcessBorderWidths() {
        region.setStyle(
                "-fx-border-style: solid;" +
                "-fx-border-color: red;" +
                "-fx-border-width: 0 0 0 0, 0, 1, 2;");
        processCSS();

        assertNull(region.getBackground());
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(
                Color.RED, Color.RED, Color.RED, Color.RED,
                BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID,
                null, new BorderWidths(0, 0, 0, 0), Insets.EMPTY);
        assertEquals(expected, stroke);
    }

    @Test public void testWithExcessBorderRadii() {
        region.setStyle(
                "-fx-border-style: solid;" +
                "-fx-border-color: red;" +
                "-fx-border-radius: 5%, 0, 10%, 2;");
        processCSS();

        assertNull(region.getBackground());
        assertEquals(1, region.getBorder().getStrokes().size(), 0);
        assertEquals(0, region.getBorder().getImages().size(), 0);

        BorderStroke stroke = region.getBorder().getStrokes().get(0);
        BorderStroke expected = new BorderStroke(
                Color.RED, Color.RED, Color.RED, Color.RED,
                BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID,
                new CornerRadii(.05, .05, .05, .05, true),
                null, Insets.EMPTY);
        assertEquals(expected, stroke);
    }
    
    // Finally, the image borders!!
    // I probably have to rework things. The image border do depend on some settings from
    // the stroke borders (or at least, they can depend on them). As such I might not be
    // able to short-circuit the creation of stroke borders, even if they have no style.

    @Test public void borderImageSourceIsNull() {
        region.setStyle("-fx-border-image-source: null;");
        processCSS();

        assertEquals(new Insets(0), region.getPadding());
        assertNull(region.getBackground());
        assertNull(region.getBorder());
    }

    @Test public void borderImageSource() {
        region.setStyle("-fx-border-image-source: url('javafx/scene/layout/center-btn.png')");
        processCSS();

        assertNull(region.getBackground());
        assertEquals(0, region.getBorder().getStrokes().size(), 0);
        assertEquals(1, region.getBorder().getImages().size(), 0);
    }

    @Test public void defaultBorderImageValues() {
        region.setStyle("-fx-border-image-source: url('javafx/scene/layout/center-btn.png')");
        processCSS();

        BorderImage image = region.getBorder().getImages().get(0);
        BorderImage expected = new BorderImage(
                image.getImage(),
                BorderWidths.DEFAULT,
                Insets.EMPTY,
                BorderWidths.FULL,
                false,
                BorderRepeat.STRETCH,
                BorderRepeat.STRETCH
        );
        assertEquals(expected, image);
    }

    @Test public void borderImageSlice_1() {
        region.setStyle(
                "-fx-border-image-source: url('javafx/scene/layout/center-btn.png');" +
                "-fx-border-image-slice: 1;");
        processCSS();

        BorderImage image = region.getBorder().getImages().get(0);
        BorderImage expected = new BorderImage(
                image.getImage(),
                BorderWidths.DEFAULT,
                Insets.EMPTY,
                new BorderWidths(1, 1, 1, 1, false, false, false, false),
                false,
                BorderRepeat.STRETCH,
                BorderRepeat.STRETCH
        );
        assertEquals(expected, image);
    }

    @Test public void borderImageSlice_1_2() {
        region.setStyle(
                "-fx-border-image-source: url('javafx/scene/layout/center-btn.png');" +
                "-fx-border-image-slice: 1 2;");
        processCSS();

        BorderImage image = region.getBorder().getImages().get(0);
        BorderImage expected = new BorderImage(
                image.getImage(),
                BorderWidths.DEFAULT,
                Insets.EMPTY,
                new BorderWidths(1, 2, 1, 2, false, false, false, false),
                false,
                BorderRepeat.STRETCH,
                BorderRepeat.STRETCH
        );
        assertEquals(expected, image);
    }

    @Test public void borderImageSlice_1_2_3() {
        region.setStyle(
                "-fx-border-image-source: url('javafx/scene/layout/center-btn.png');" +
                "-fx-border-image-slice: 1 2 3;");
        processCSS();

        BorderImage image = region.getBorder().getImages().get(0);
        BorderImage expected = new BorderImage(
                image.getImage(),
                BorderWidths.DEFAULT,
                Insets.EMPTY,
                new BorderWidths(1, 2, 3, 2, false, false, false, false),
                false,
                BorderRepeat.STRETCH,
                BorderRepeat.STRETCH
        );
        assertEquals(expected, image);
    }

    @Test public void borderImageSlice_1_2_3_4() {
        region.setStyle(
                "-fx-border-image-source: url('javafx/scene/layout/center-btn.png');" +
                "-fx-border-image-slice: 1 2 3 4;");
        processCSS();

        BorderImage image = region.getBorder().getImages().get(0);
        BorderImage expected = new BorderImage(
                image.getImage(),
                BorderWidths.DEFAULT,
                Insets.EMPTY,
                new BorderWidths(1, 2, 3, 4, false, false, false, false),
                false,
                BorderRepeat.STRETCH,
                BorderRepeat.STRETCH
        );
        assertEquals(expected, image);
    }

    @Test public void borderImageSlice_1_fill() {
        region.setStyle(
                "-fx-border-image-source: url('javafx/scene/layout/center-btn.png');" +
                "-fx-border-image-slice: 1 fill;");
        processCSS();

        BorderImage image = region.getBorder().getImages().get(0);
        BorderImage expected = new BorderImage(
                image.getImage(),
                BorderWidths.DEFAULT,
                Insets.EMPTY,
                new BorderWidths(1, 1, 1, 1),
                true,
                BorderRepeat.STRETCH,
                BorderRepeat.STRETCH
        );
        assertEquals(expected, image);
    }

    @Test public void borderImageSlice_1_2_fill() {
        region.setStyle(
                "-fx-border-image-source: url('javafx/scene/layout/center-btn.png');" +
                "-fx-border-image-slice: 1 2 fill;");
        processCSS();

        BorderImage image = region.getBorder().getImages().get(0);
        BorderImage expected = new BorderImage(
                image.getImage(),
                BorderWidths.DEFAULT,
                Insets.EMPTY,
                new BorderWidths(1, 2, 1, 2),
                true,
                BorderRepeat.STRETCH,
                BorderRepeat.STRETCH
        );
        assertEquals(expected, image);
    }

    @Test public void borderImageSlice_1_2_3_fill() {
        region.setStyle(
                "-fx-border-image-source: url('javafx/scene/layout/center-btn.png');" +
                "-fx-border-image-slice: 1 2 3 fill;");
        processCSS();

        BorderImage image = region.getBorder().getImages().get(0);
        BorderImage expected = new BorderImage(
                image.getImage(),
                BorderWidths.DEFAULT,
                Insets.EMPTY,
                new BorderWidths(1, 2, 3, 2),
                true,
                BorderRepeat.STRETCH,
                BorderRepeat.STRETCH
        );
        assertEquals(expected, image);
    }

    @Test public void borderImageSlice_1_2_3_4_fill() {
        region.setStyle(
                "-fx-border-image-source: url('javafx/scene/layout/center-btn.png');" +
                "-fx-border-image-slice: 1 2 3 4 fill;");
        processCSS();

        BorderImage image = region.getBorder().getImages().get(0);
        BorderImage expected = new BorderImage(
                image.getImage(),
                BorderWidths.DEFAULT,
                Insets.EMPTY,
                new BorderWidths(1, 2, 3, 4),
                true,
                BorderRepeat.STRETCH,
                BorderRepeat.STRETCH
        );
        assertEquals(expected, image);
    }

    @Test public void borderImageWidth_1() {
        region.setStyle(
                "-fx-border-image-source: url('javafx/scene/layout/center-btn.png');" +
                "-fx-border-image-width: 1;");
        processCSS();

        BorderImage image = region.getBorder().getImages().get(0);
        BorderImage expected = new BorderImage(
                image.getImage(),
                new BorderWidths(1, 1, 1, 1, false, false, false, false),
                Insets.EMPTY,
                BorderWidths.FULL,
                false,
                BorderRepeat.STRETCH,
                BorderRepeat.STRETCH
        );
        assertEquals(expected, image);
    }

    @Test public void borderImageWidth_1_2() {
        region.setStyle(
                "-fx-border-image-source: url('javafx/scene/layout/center-btn.png');" +
                "-fx-border-image-width: 1 2;");
        processCSS();

        BorderImage image = region.getBorder().getImages().get(0);
        BorderImage expected = new BorderImage(
                image.getImage(),
                new BorderWidths(1, 2, 1, 2, false, false, false, false),
                Insets.EMPTY,
                BorderWidths.FULL,
                false,
                BorderRepeat.STRETCH,
                BorderRepeat.STRETCH
        );
        assertEquals(expected, image);
    }

    @Test public void borderImageWidth_1_2_3() {
        region.setStyle(
                "-fx-border-image-source: url('javafx/scene/layout/center-btn.png');" +
                "-fx-border-image-width: 1 2 3;");
        processCSS();

        BorderImage image = region.getBorder().getImages().get(0);
        BorderImage expected = new BorderImage(
                image.getImage(),
                new BorderWidths(1, 2, 3, 2, false, false, false, false),
                Insets.EMPTY,
                BorderWidths.FULL,
                false,
                BorderRepeat.STRETCH,
                BorderRepeat.STRETCH
        );
        assertEquals(expected, image);
    }

    @Test public void borderImageWidth_1_2_3_4() {
        region.setStyle(
                "-fx-border-image-source: url('javafx/scene/layout/center-btn.png');" +
                "-fx-border-image-width: 1 2 3 4;");
        processCSS();

        BorderImage image = region.getBorder().getImages().get(0);
        BorderImage expected = new BorderImage(
                image.getImage(),
                new BorderWidths(1, 2, 3, 4, false, false, false, false),
                Insets.EMPTY,
                BorderWidths.FULL,
                false,
                BorderRepeat.STRETCH,
                BorderRepeat.STRETCH
        );
        assertEquals(expected, image);
    }

    @Test public void borderImageWidth_1_2Percent() {
        region.setStyle(
                "-fx-border-image-source: url('javafx/scene/layout/center-btn.png');" +
                "-fx-border-image-width: 1 2%;");
        processCSS();

        BorderImage image = region.getBorder().getImages().get(0);
        BorderImage expected = new BorderImage(
                image.getImage(),
                new BorderWidths(1, .02, 1, .02, false, true, false, true),
                Insets.EMPTY,
                BorderWidths.FULL,
                false,
                BorderRepeat.STRETCH,
                BorderRepeat.STRETCH
        );
        assertEquals(expected, image);
    }

    @Test public void borderImageWidth_1Percent_2Percent_3Percent_4Percent() {
        region.setStyle(
                "-fx-border-image-source: url('javafx/scene/layout/center-btn.png');" +
                "-fx-border-image-width: 1% 2% 3% 4%;");
        processCSS();

        BorderImage image = region.getBorder().getImages().get(0);
        BorderImage expected = new BorderImage(
                image.getImage(),
                new BorderWidths(.01, .02, .03, .04, true, true, true, true),
                Insets.EMPTY,
                BorderWidths.FULL,
                false,
                BorderRepeat.STRETCH,
                BorderRepeat.STRETCH
        );
        assertEquals(expected, image);
    }

    @Ignore("I am not certain that supporting auto makes sense for us, and if it does, is it anything other than 1?")
    @Test public void borderImageWidth_auto() {
        region.setStyle(
                "-fx-border-image-source: url('javafx/scene/layout/center-btn.png');" +
                "-fx-border-image-width: auto;");
        processCSS();

        BorderImage image = region.getBorder().getImages().get(0);
        BorderImage expected = new BorderImage(
                image.getImage(),
                new BorderWidths(
                        BorderWidths.AUTO, BorderWidths.AUTO,
                        BorderWidths.AUTO, BorderWidths.AUTO, false, false, false, false),
                Insets.EMPTY,
                BorderWidths.FULL,
                false,
                BorderRepeat.STRETCH,
                BorderRepeat.STRETCH
        );
        assertEquals(expected, image);
    }

    @Ignore("I am not certain that supporting auto makes sense for us, and if it does, is it anything other than 1?")
    @Test public void borderImageWidth_1_auto() {
        region.setStyle(
                "-fx-border-image-source: url('javafx/scene/layout/center-btn.png');" +
                "-fx-border-image-width: 1 auto;");
        processCSS();

        BorderImage image = region.getBorder().getImages().get(0);
        BorderImage expected = new BorderImage(
                image.getImage(),
                new BorderWidths(1, BorderWidths.AUTO,
                        1, BorderWidths.AUTO, false, false, false, false),
                Insets.EMPTY,
                BorderWidths.FULL,
                false,
                BorderRepeat.STRETCH,
                BorderRepeat.STRETCH
        );
        assertEquals(expected, image);
    }

    @Ignore("I am not certain that supporting auto makes sense for us, and if it does, is it anything other than 1?")
    @Test public void borderImageWidth_1_2Percent_auto() {
        region.setStyle(
                "-fx-border-image-source: url('javafx/scene/layout/center-btn.png');" +
                "-fx-border-image-width: 1 2% auto;");
        processCSS();

        BorderImage image = region.getBorder().getImages().get(0);
        BorderImage expected = new BorderImage(
                image.getImage(),
                new BorderWidths(1, .02,
                        BorderWidths.AUTO, .02, false, true, false, true),
                Insets.EMPTY,
                BorderWidths.FULL,
                false,
                BorderRepeat.STRETCH,
                BorderRepeat.STRETCH
        );
        assertEquals(expected, image);
    }

    @Test public void borderImageOutset_1() {
        region.setStyle(
                "-fx-border-image-source: url('javafx/scene/layout/center-btn.png');" +
                "-fx-border-image-insets: 1;");
        processCSS();

        BorderImage image = region.getBorder().getImages().get(0);
        BorderImage expected = new BorderImage(
                image.getImage(),
                BorderWidths.DEFAULT,
                new Insets(1),
                BorderWidths.FULL,
                false,
                BorderRepeat.STRETCH,
                BorderRepeat.STRETCH
        );
        assertEquals(expected, image);
    }

    @Test public void borderImageOutset_1_2() {
        region.setStyle(
                "-fx-border-image-source: url('javafx/scene/layout/center-btn.png');" +
                "-fx-border-image-insets: 1 2;");
        processCSS();

        BorderImage image = region.getBorder().getImages().get(0);
        BorderImage expected = new BorderImage(
                image.getImage(),
                BorderWidths.DEFAULT,
                new Insets(1, 2, 1, 2),
                BorderWidths.FULL,
                false,
                BorderRepeat.STRETCH,
                BorderRepeat.STRETCH
        );
        assertEquals(expected, image);
    }

    @Test public void borderImageOutset_1_2_3() {
        region.setStyle(
                "-fx-border-image-source: url('javafx/scene/layout/center-btn.png');" +
                "-fx-border-image-insets: 1 2 3;");
        processCSS();

        BorderImage image = region.getBorder().getImages().get(0);
        BorderImage expected = new BorderImage(
                image.getImage(),
                BorderWidths.DEFAULT,
                new Insets(1, 2, 3, 2),
                BorderWidths.FULL,
                false,
                BorderRepeat.STRETCH,
                BorderRepeat.STRETCH
        );
        assertEquals(expected, image);
    }

    @Test public void borderImageOutset_1_2_3_4() {
        region.setStyle(
                "-fx-border-image-source: url('javafx/scene/layout/center-btn.png');" +
                "-fx-border-image-insets: 1 2 3 4;");
        processCSS();

        BorderImage image = region.getBorder().getImages().get(0);
        BorderImage expected = new BorderImage(
                image.getImage(),
                BorderWidths.DEFAULT,
                new Insets(1, 2, 3, 4),
                BorderWidths.FULL,
                false,
                BorderRepeat.STRETCH,
                BorderRepeat.STRETCH
        );
        assertEquals(expected, image);
    }

    @Test public void borderImageRepeat_stretch() {
        region.setStyle(
                "-fx-border-image-source: url('javafx/scene/layout/center-btn.png');" +
                "-fx-border-image-repeat: stretch;");
        processCSS();

        BorderImage image = region.getBorder().getImages().get(0);
        BorderImage expected = new BorderImage(
                image.getImage(),
                BorderWidths.DEFAULT,
                Insets.EMPTY,
                BorderWidths.FULL,
                false,
                BorderRepeat.STRETCH,
                BorderRepeat.STRETCH
        );
        assertEquals(expected, image);
    }

    @Test public void borderImageRepeat_repeat() {
        region.setStyle(
                "-fx-border-image-source: url('javafx/scene/layout/center-btn.png');" +
                "-fx-border-image-repeat: repeat;");
        processCSS();

        BorderImage image = region.getBorder().getImages().get(0);
        BorderImage expected = new BorderImage(
                image.getImage(),
                BorderWidths.DEFAULT,
                Insets.EMPTY,
                BorderWidths.FULL,
                false,
                BorderRepeat.REPEAT,
                BorderRepeat.REPEAT
        );
        assertEquals(expected, image);
    }

    @Test public void borderImageRepeat_round() {
        region.setStyle(
                "-fx-border-image-source: url('javafx/scene/layout/center-btn.png');" +
                "-fx-border-image-repeat: round;");
        processCSS();

        BorderImage image = region.getBorder().getImages().get(0);
        BorderImage expected = new BorderImage(
                image.getImage(),
                BorderWidths.DEFAULT,
                Insets.EMPTY,
                BorderWidths.FULL,
                false,
                BorderRepeat.ROUND,
                BorderRepeat.ROUND
        );
        assertEquals(expected, image);
    }

    @Test public void borderImageRepeat_space() {
        region.setStyle(
                "-fx-border-image-source: url('javafx/scene/layout/center-btn.png');" +
                "-fx-border-image-repeat: space;");
        processCSS();

        BorderImage image = region.getBorder().getImages().get(0);
        BorderImage expected = new BorderImage(
                image.getImage(),
                BorderWidths.DEFAULT,
                Insets.EMPTY,
                BorderWidths.FULL,
                false,
                BorderRepeat.SPACE,
                BorderRepeat.SPACE
        );
        assertEquals(expected, image);
    }

    @Test public void borderImageRepeat_round_stretch() {
        region.setStyle(
                "-fx-border-image-source: url('javafx/scene/layout/center-btn.png');" +
                "-fx-border-image-repeat: round stretch;");
        processCSS();

        BorderImage image = region.getBorder().getImages().get(0);
        BorderImage expected = new BorderImage(
                image.getImage(),
                BorderWidths.DEFAULT,
                Insets.EMPTY,
                BorderWidths.FULL,
                false,
                BorderRepeat.ROUND,
                BorderRepeat.STRETCH
        );
        assertEquals(expected, image);
    }

    @Test public void borderImageRepeat_round_repeat() {
        region.setStyle(
                "-fx-border-image-source: url('javafx/scene/layout/center-btn.png');" +
                "-fx-border-image-repeat: round repeat;");
        processCSS();

        BorderImage image = region.getBorder().getImages().get(0);
        BorderImage expected = new BorderImage(
                image.getImage(),
                BorderWidths.DEFAULT,
                Insets.EMPTY,
                BorderWidths.FULL,
                false,
                BorderRepeat.ROUND,
                BorderRepeat.REPEAT
        );
        assertEquals(expected, image);
    }

    // TODO multiple image borders
    // TODO complex background fill, background image, stroke border, image border scenario
}
