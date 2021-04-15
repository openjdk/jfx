/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package test.robot.javafx.scene.layout;

import test.robot.javafx.scene.layout.RegionUITestBase;
import javafx.geometry.Bounds;
import javafx.scene.paint.Color;
import org.junit.Ignore;
import org.junit.Test;
import com.sun.javafx.PlatformUtil;
import static org.junit.Assume.assumeTrue;

public class RegionBackgroundImageUITest extends RegionUITestBase {
    /**************************************************************************
     *                                                                        *
     * Tests for aligned background images. The test image in use is chosen   *
     * to align naturally with the edge of the test region.                   *
     *                                                                        *
     *************************************************************************/

    @Test(timeout = 20000)
    public void alignedImage() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');");

        checkCompletelyFilled(20);
    }

    @Test(timeout = 20000)
    public void alignedImage_RepeatX() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-repeat: repeat-x");

        checkHorizontalStripAlongTop(20, LEFT);
    }

    @Test(timeout = 20000)
    public void alignedImage_RepeatY() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-repeat: repeat-y");

        checkVerticalStripAlongLeft(20, TOP);
    }

    @Test(timeout = 20000)
    public void alignedImage_Space() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-repeat: space space");

        checkCompletelyFilled(20);
    }

    @Test(timeout = 20000)
    public void alignedImage_Round() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-repeat: round");

        checkCompletelyFilled(20);
    }

    @Test(timeout = 20000)
    public void alignedImage_RoundSpace() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-repeat: round space");

        checkCompletelyFilled(20);
    }

    @Test(timeout = 20000)
    public void alignedImage_PositionCenter() {
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-repeat: no-repeat;" +
                "-fx-background-position: center");

        checkNonRepeating(20, WIDTH / 2, HEIGHT / 2, true);
    }

    @Test(timeout = 20000)
    public void alignedImage_PositionCenterFiftyPercent() {
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-repeat: no-repeat;" +
                "-fx-background-position: 50%");

        checkNonRepeating(20, WIDTH / 2, HEIGHT / 2, true);
    }

    @Test(timeout = 20000)
    public void alignedImage_PositionCenterLeft() {
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-repeat: no-repeat;" +
                "-fx-background-position: left center");

        final int centerX = REGION_LEFT + 10;
        checkNonRepeating(20, centerX, HEIGHT / 2, true);
        assertColorEquals(Color.RED, centerX - 10, HEIGHT / 2, TOLERANCE);
        assertColorEquals(SCENE_FILL, centerX - 11, HEIGHT / 2, TOLERANCE);
    }

    @Test(timeout = 20000)
    public void alignedImage_PositionCenterRight() {
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-repeat: no-repeat;" +
                "-fx-background-position: right center");

        final int centerX = REGION_RIGHT - 10;
        checkNonRepeating(20, centerX, HEIGHT / 2, true);
        assertColorEquals(Color.RED, centerX + 9, HEIGHT / 2, TOLERANCE);
        assertColorEquals(SCENE_FILL, centerX + 10, HEIGHT / 2, TOLERANCE);
    }

    @Test(timeout = 20000)
    public void alignedImage_PositionCenterTop() {
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-repeat: no-repeat;" +
                "-fx-background-position: center top");

        final int centerY = (REGION_TOP + 10);
        checkNonRepeating(20, WIDTH / 2, centerY, true);
        assertColorEquals(Color.RED, WIDTH / 2, centerY - 10, TOLERANCE);
        assertColorEquals(SCENE_FILL, WIDTH / 2, centerY - 11, TOLERANCE);
    }

    @Test(timeout = 20000)
    public void alignedImage_PositionCenterBottom() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-repeat: no-repeat;" +
                "-fx-background-position: center bottom");

        final int centerY = (REGION_BOTTOM - 10);
        checkNonRepeating(20, WIDTH / 2, centerY, true);
        assertColorEquals(Color.RED, WIDTH / 2, centerY + 9, TOLERANCE);
        assertColorEquals(SCENE_FILL, WIDTH / 2, centerY + 10, TOLERANCE);
    }

    @Test(timeout = 20000)
    public void alignedImage_PositionBottomRight() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-repeat: no-repeat;" +
                "-fx-background-position: right bottom");

        final int centerX = (REGION_RIGHT - 10);
        final int centerY = (REGION_BOTTOM - 10);
        checkNonRepeating(20, centerX, centerY, true);
        assertColorEquals(Color.RED, centerX + 9, centerY, TOLERANCE);
        assertColorEquals(SCENE_FILL, centerX + 10, centerY, TOLERANCE);
        assertColorEquals(Color.RED, centerX, centerY + 9, TOLERANCE);
        assertColorEquals(SCENE_FILL, centerX, centerY + 10, TOLERANCE);
    }

    @Test(timeout = 20000)
    public void alignedImage_PositionCenterRepeatX() {
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-repeat: repeat-x;" +
                "-fx-background-position: center");

        checkHorizontalStripAlongCenter(20, CENTER);
    }

    @Test(timeout = 20000)
    public void alignedImage_PositionCenterFiftyPercentRepeatX() {
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-repeat: repeat-x;" +
                "-fx-background-position: 50%");

        checkHorizontalStripAlongCenter(20, CENTER);
    }

    @Test(timeout = 20000)
    public void alignedImage_PositionCenterLeftRepeatX() {
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-repeat: repeat-x;" +
                "-fx-background-position: left center");

        checkHorizontalStripAlongCenter(20, LEFT);
    }

    @Test(timeout = 20000)
    public void alignedImage_PositionCenterRightRepeatX() {
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-repeat: repeat-x;" +
                "-fx-background-position: right center");
        checkHorizontalStripAlongCenter(20, LEFT);
    }

    @Test(timeout = 20000)
    public void alignedImage_PositionCenterTopRepeatX() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-repeat: repeat-x;" +
                "-fx-background-position: center top");

        checkHorizontalStripAlongTop(20, CENTER);
    }

    @Test(timeout = 20000)
    public void alignedImage_PositionCenterBottomRepeatX() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-repeat: repeat-x;" +
                "-fx-background-position: center bottom");

        checkHorizontalStripAlongBottom(20, CENTER);
    }

    @Test(timeout = 20000)
    public void alignedImage_PositionBottomRightRepeatX() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-repeat: repeat-x;" +
                "-fx-background-position: right bottom");

        checkHorizontalStripAlongBottom(20, LEFT);
    }

    @Test(timeout = 20000)
    public void alignedImage_PositionCenterRepeatY() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-repeat: repeat-y;" +
                "-fx-background-position: center");

        checkVerticalStripAlongCenter(20, CENTER);
    }

    @Test(timeout = 20000)
    public void alignedImage_PositionCenterFiftyPercentRepeatY() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-repeat: repeat-y;" +
                "-fx-background-position: 50%");

        checkVerticalStripAlongCenter(20, CENTER);
    }

    @Test(timeout = 20000)
    public void alignedImage_PositionCenterLeftRepeatY() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-repeat: repeat-y;" +
                "-fx-background-position: left center");

        checkVerticalStripAlongLeft(20, CENTER);
    }

    @Test(timeout = 20000)
    public void alignedImage_PositionCenterRightRepeatY() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-repeat: repeat-y;" +
                "-fx-background-position: right center");

        checkVerticalStripAlongRight(20, CENTER);
    }

    @Test(timeout = 20000)
    public void alignedImage_PositionCenterTopRepeatY() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-repeat: repeat-y;" +
                "-fx-background-position: center top");

        checkVerticalStripAlongCenter(20, TOP);
    }

    @Test(timeout = 20000)
    public void alignedImage_PositionCenterBottomRepeatY() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-repeat: repeat-y;" +
                "-fx-background-position: center bottom");

        checkVerticalStripAlongCenter(20, TOP);
    }

    @Test(timeout = 20000)
    public void alignedImage_PositionBottomRightRepeatY() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-repeat: repeat-y;" +
                "-fx-background-position: right bottom");

        checkVerticalStripAlongRight(20, TOP);
    }

    @Test(timeout = 20000)
    public void alignedImage_Position25PercentLeft() {
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-repeat: no-repeat;" +
                "-fx-background-position: left 25% center");

        int offsetX = 5;
        checkNonRepeating(20,
                (REGION_LEFT +  (int) (.25 * REGION_WIDTH) + offsetX),
                (REGION_TOP +  (int) (.5 * REGION_HEIGHT)), true);
    }

    @Test(timeout = 20000)
    public void alignedImage_Position25PercentRight() {
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-repeat: no-repeat;" +
                "-fx-background-position: right 25% center");

        int offsetX = 5;
        checkNonRepeating(20,
                (REGION_LEFT +  (int) (.75 * REGION_WIDTH) - offsetX),
                (REGION_TOP +  (int) (.5 * REGION_HEIGHT)), true);
    }

    // TODO should do from the top & bottom

    // TODO should test that cover causes other properties to be ignored
    @Test(timeout = 20000)
    public void alignedImage_Cover() {
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-size: cover;");

        // The image is 20x20. To fit, it would need to be stretched to be 300x300.
        checkNonRepeating(300, REGION_LEFT + 150, REGION_TOP + 150, false);
    }

    // TODO should test that contain causes other properties to be ignored
    @Test(timeout = 20000)
    public void alignedImage_Contain() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-size: contain;");

        // The image is 20x20. So there will be two copies, 200x200 in size
        checkNonRepeating(200, REGION_LEFT + 100, REGION_TOP + 100, false);
        checkNonRepeating(200, REGION_LEFT + 300, REGION_TOP + 100, false);
    }

    @Test(timeout = 20000)
    public void alignedImage_ContainNoRepeat() {
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-size: contain;" +
                "-fx-background-repeat: no-repeat;");

        checkNonRepeating(200, REGION_LEFT + 100, REGION_TOP + 100, true);
    }

    @Test(timeout = 20000)
    public void unalignedImage() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');");
        checkCompletelyFilled(48);
    }

    @Test(timeout = 20000)
    public void unalignedImage_RepeatX() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-repeat: repeat-x");

        checkHorizontalStripAlongTop(48, LEFT);
    }

    @Test(timeout = 20000)
    public void unalignedImage_RepeatY() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-repeat: repeat-y");

        checkVerticalStripAlongLeft(48, TOP);
    }

    @Ignore("RT-33411: Doesn't work at present because of Prism bug where the gaps between rows are inconsistent")
    @Test(timeout = 20000)
    public void unalignedImage_Space() {
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-repeat: space space");

        checkEvenlyFilled(48);
    }

    @Test(timeout = 20000)
    public void unalignedImage_Round() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-repeat: round");

        checkCompletelyFilled(50); // rounded up from 48 to 50
    }

    @Ignore("RT-33411: Doesn't work at present because of Prism bug where the gaps between rows are inconsistent")
    @Test(timeout = 20000)
    public void unalignedImage_RoundSpace() {
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-repeat: round space");

        // Need to update checkEvenlyFilled / checkCompletely filled to handle case like this where it is
        // even in one dimension and spaced in the other
    }

    @Test(timeout = 20000)
    public void unalignedImage_PositionCenter() {
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-repeat: no-repeat;" +
                "-fx-background-position: center");
        checkNonRepeating(48, WIDTH / 2, HEIGHT / 2, true);
    }

    @Test(timeout = 20000)
    public void unalignedImage_PositionCenterFiftyPercent() {
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-repeat: no-repeat;" +
                "-fx-background-position: 50%");
        checkNonRepeating(48, WIDTH / 2, HEIGHT / 2, true);
    }

    @Test(timeout = 20000)
    public void unalignedImage_PositionCenterLeft() {
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-repeat: no-repeat;" +
                "-fx-background-position: left center");
        checkNonRepeating(48, REGION_LEFT + 24, HEIGHT / 2, true);
    }

    @Test(timeout = 20000)
    public void unalignedImage_PositionCenterRight() {
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-repeat: no-repeat;" +
                "-fx-background-position: right center");
        checkNonRepeating(48, REGION_RIGHT - 24, HEIGHT / 2, true);
    }

    @Test(timeout = 20000)
    public void unalignedImage_PositionCenterTop() {
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-repeat: no-repeat;" +
                "-fx-background-position: center top");
        checkNonRepeating(48, WIDTH / 2, REGION_TOP + 24, true);
    }

    @Test(timeout = 20000)
    public void unalignedImage_PositionCenterBottom() {
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-repeat: no-repeat;" +
                "-fx-background-position: center bottom");
        checkNonRepeating(48, WIDTH / 2, REGION_BOTTOM - 24, true);
    }

    @Test(timeout = 20000)
    public void unalignedImage_PositionBottomRight() {
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-repeat: no-repeat;" +
                "-fx-background-position: right bottom");
        checkNonRepeating(48, REGION_RIGHT - 24, REGION_BOTTOM - 24, true);
    }

    @Test(timeout = 20000)
    public void unalignedImage_PositionCenterRepeatX() {
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-repeat: repeat-x;" +
                "-fx-background-position: center");

        checkHorizontalStripAlongCenter(48, CENTER);
    }

    @Test(timeout = 20000)
    public void unalignedImage_PositionCenterFiftyPercentRepeatX() {
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-repeat: repeat-x;" +
                "-fx-background-position: 50%");

        checkHorizontalStripAlongCenter(48, CENTER);
    }

    @Test(timeout = 20000)
    public void unalignedImage_PositionCenterLeftRepeatX() {
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-repeat: repeat-x;" +
                "-fx-background-position: left center");

        checkHorizontalStripAlongCenter(48, LEFT);
    }

    @Test(timeout = 20000)
    public void unalignedImage_PositionCenterRightRepeatX() {
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-repeat: repeat-x;" +
                "-fx-background-position: right center");

        checkHorizontalStripAlongCenter(48, RIGHT);
    }

    @Test(timeout = 20000)
    public void unalignedImage_PositionCenterTopRepeatX() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-repeat: repeat-x;" +
                "-fx-background-position: center top");

        checkHorizontalStripAlongTop(48, CENTER);
    }

    @Test(timeout = 20000)
    public void unalignedImage_PositionCenterBottomRepeatX() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-repeat: repeat-x;" +
                "-fx-background-position: center bottom");

        checkHorizontalStripAlongBottom(48, CENTER);
    }

    @Test(timeout = 20000)
    public void unalignedImage_PositionBottomRightRepeatX() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-repeat: repeat-x;" +
                "-fx-background-position: right bottom");

        checkHorizontalStripAlongBottom(48, RIGHT);
    }

    @Test(timeout = 20000)
    public void unalignedImage_PositionCenterRepeatY() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-repeat: repeat-y;" +
                "-fx-background-position: center");

        checkVerticalStripAlongCenter(48, CENTER);
    }

    @Test(timeout = 20000)
    public void unalignedImage_PositionCenterFiftyPercentRepeatY() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-repeat: repeat-y;" +
                "-fx-background-position: 50%");

        checkVerticalStripAlongCenter(48, CENTER);
    }

    @Test(timeout = 20000)
    public void unalignedImage_PositionCenterLeftRepeatY() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-repeat: repeat-y;" +
                "-fx-background-position: left center");

        checkVerticalStripAlongLeft(48, CENTER);
    }

    @Test(timeout = 20000)
    public void unalignedImage_PositionCenterRightRepeatY() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-repeat: repeat-y;" +
                "-fx-background-position: right center");

        checkVerticalStripAlongRight(48, CENTER);
    }

    @Test(timeout = 20000)
    public void unalignedImage_PositionCenterTopRepeatY() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-repeat: repeat-y;" +
                "-fx-background-position: center top");

        checkVerticalStripAlongCenter(48, TOP);
    }

    @Test(timeout = 20000)
    public void unalignedImage_PositionCenterBottomRepeatY() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-repeat: repeat-y;" +
                "-fx-background-position: center bottom");

        checkVerticalStripAlongCenter(48, BOTTOM);
    }

    @Test(timeout = 20000)
    public void unalignedImage_PositionBottomRightRepeatY() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-repeat: repeat-y;" +
                "-fx-background-position: right bottom");

        checkVerticalStripAlongRight(48, BOTTOM);
    }

    @Test(timeout = 20000)
    public void unalignedImage_Position25PercentLeft() {
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-repeat: no-repeat;" +
                "-fx-background-position: left 25% center");

        int offsetX = 12;
        checkNonRepeating(48,
                (REGION_LEFT +  (int) (.25 * REGION_WIDTH) + offsetX),
                (REGION_TOP +  (int) (.5 * REGION_HEIGHT)), true);
    }

    @Test(timeout = 20000)
    public void unalignedImage_Position25PercentRight() {
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-repeat: no-repeat;" +
                "-fx-background-position: right 25% center");

        int offsetX = 12;
        checkNonRepeating(48,
                (REGION_LEFT +  (int) (.75 * REGION_WIDTH) - offsetX),
                (REGION_TOP +  (int) (.5 * REGION_HEIGHT)), true);
    }

    // TODO should do from the top & bottom

    // TODO should test that cover causes other properties to be ignored

    @Test(timeout = 20000)
    public void unalignedImage_Cover() {
        assumeTrue(checkIntegralUIScale());
        assumeTrue(!PlatformUtil.isMac()); // JDK-8255679
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-size: cover;");

        // The image is 48x48. To fit it would need to be stretched to 336x336
        checkNonRepeating(336, REGION_LEFT + 168, REGION_TOP + 168, false);
    }

    // TODO should test that contain causes other properties to be ignored
    @Test(timeout = 20000)
    public void unalignedImage_Contain() {
        assumeTrue(checkIntegralUIScale());
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-size: contain;");

        // The image is 48x48. So there will be two copies, 200x200 in size
        checkNonRepeating(200, REGION_LEFT + 100, REGION_TOP + 100, false);
        checkNonRepeating(200, REGION_LEFT + 300, REGION_TOP + 100, false);
    }

    @Test(timeout = 20000)
    public void unalignedImage_ContainNoRepeat() {
        setStyle("-fx-background-color: black;" +
                "-fx-background-image: url('test/robot/javafx/scene/layout/test48x48.png');" +
                "-fx-background-size: contain;" +
                "-fx-background-repeat: no-repeat;");

        checkNonRepeating(200, REGION_LEFT + 100, REGION_TOP + 100, true);
    }

    // TODO need to write tests for when there are multiple images

    // TODO there are probably a hundred other combinations to try, easy.
    // And then there are another thousand combinations where some of the
    // settings are ignored, and we need to ensure that indeed they are ignored.
    // TODO I need to turn most of these tests into proper unit tests as well
    // so that we know that the CSS->Region process is working correctly.

    // TODO need to write tests where SPACE is the repeat but only 2 tiles fit, and where only 1 tile fits.

    static final int LEFT = 0;
    static final int RIGHT = 1;
    static final int CENTER = 2;
    static final int TOP = 3;
    static final int BOTTOM = 4;

    private void checkRegionFillOutsideTheseBounds(Color expectedFill, int x, int y, int w, int h) {
        // There should be a single image in the very center, and that is all. We'll check a few other places to make
        // sure it is all black, except for the center.
        final double hw = w / 2.0;
        final double hh = h / 2.0;
        final int halfWidth = (int) hw;
        final int halfHeight = (int) hh;
        final int centerX = (int) (x + hw);
        final int centerY = (int) (y + hh);

        // Every pixel left of the image
        for (int xx=centerX-halfWidth-1; xx>REGION_LEFT; xx--) {
            assertColorEquals(expectedFill, xx, centerY, TOLERANCE);
        }
        // Every pixel to the right of the image
        for (int xx=centerX+halfWidth; xx<REGION_RIGHT; xx++) {
            assertColorEquals(expectedFill, xx, centerY, TOLERANCE);
        }
        // Every pixel above the image
        for (int yy=centerY-halfHeight-1; yy>REGION_TOP; yy--) {
            assertColorEquals(expectedFill, centerX, yy, TOLERANCE);
        }
        // Every pixel below the image
        for (int yy=centerY+halfHeight; yy<REGION_TOP+REGION_HEIGHT; yy++) {
            assertColorEquals(expectedFill, centerX, yy, TOLERANCE);
        }
    }

    // Without gaps!
    private void checkCompletelyFilled(int size) {
        final int halfSize = size / 2;
        final int numRows = (200 / size) + 1;
        final int numCols = (300 / size) + 1;
        for (int row=0; row<numRows; row++) {
            for (int col=0; col<numCols; col++) {
                int centerX = (REGION_LEFT + (size * col) + halfSize);
                int centerY = (REGION_TOP + (size * row) + halfSize);
                checkNonRepeating(size, centerX, centerY, false);
            }
        }

        // Because the tiles fit perfectly, we shouldn't be able to see the background fill at all
        checkRegionCornersAndBoundariesOfBackgroundFill(region.getBackground().getFills().get(0), Color.RED, SCENE_FILL);
    }

    // With gaps!
    private void checkEvenlyFilled(int size) {
        final int halfSize = size / 2;
        final int numRows = 200 / size;
        final int numCols = 300 / size;
        final int rowGap = (200 - (numRows * size)) / (numRows - 1);
        final int colGap = (300 - (numCols * size)) / (numCols - 1);
        for (int row=0; row<numRows; row++) {
            for (int col=0; col<numCols; col++) {
                int centerX = (REGION_LEFT + (size * col) + (colGap * col) + halfSize);
                int centerY = (REGION_TOP + (size * row) + (rowGap * row) + halfSize);
                checkNonRepeating(size, centerX, centerY, false);
                if (col > 0) {
                    int x = centerX - halfSize - (colGap / 2);
                    assertColorEquals(Color.BLACK, x, centerY, TOLERANCE);
                }
                if (row > 0) {
                    int y = centerY - halfSize - (rowGap / 2);
                    assertColorEquals(Color.BLACK, centerX, y, TOLERANCE);
                }
            }
        }

        // Because the tiles fit perfectly, we shouldn't be able to see the background fill at all
        checkRegionCornersAndBoundariesOfBackgroundFill(region.getBackground().getFills().get(0), Color.RED, SCENE_FILL);
    }

    private void checkHorizontalStripAlongTop(int size, int where) {
        final int halfSize = size / 2;
        final int quarterSize = size / 4;
        final int centerX = (REGION_LEFT + halfSize);
        final int centerY = (REGION_TOP + halfSize);
        final int leftX = REGION_LEFT;
        final int topY = REGION_TOP;
        final int rightX = REGION_RIGHT;

        // The centers
        if (where == CENTER) {
            for (int i=WIDTH/2; i<rightX; i+=size) {
                checkNonRepeating(size, i, centerY, false);
            }
            for (int i=WIDTH/2; i>leftX; i-=size) {
                checkNonRepeating(size, i, centerY, false);
            }
        } else if (where == LEFT) {
            for (int i=centerX; i<rightX; i+=size) {
                checkNonRepeating(size, i, centerY, false);
            }
        } else {
            for (int i=rightX - halfSize; i>leftX; i-=size) {
                checkNonRepeating(size, i, centerY, false);
            }
        }
        // Every pixel outside the image
        checkRegionFillOutsideTheseBounds(Color.BLACK, leftX, topY, REGION_WIDTH, size);
        // Should be butted up to the left and right and top edges
        checkRegionTopBoundary(0, Color.RED, SCENE_FILL, 0, TOLERANCE);
        // left
        assertColorDoesNotEqual(SCENE_FILL, leftX, topY, TOLERANCE);
        assertColorEquals(SCENE_FILL, leftX - 1, topY, TOLERANCE);
        assertColorEquals(SCENE_FILL, leftX, topY - 1, TOLERANCE);
        // right
        assertColorDoesNotEqual(SCENE_FILL, (REGION_RIGHT) - 1, REGION_TOP, TOLERANCE);
        assertColorEquals(SCENE_FILL, (REGION_RIGHT), REGION_TOP, TOLERANCE);
        assertColorEquals(SCENE_FILL, (REGION_RIGHT), REGION_TOP-1, TOLERANCE);
    }

    private void checkHorizontalStripAlongCenter(int size, int where) {
        final int halfSize = size / 2;
        final int centerX = (REGION_LEFT + halfSize);
        final int centerY = (REGION_TOP + (REGION_HEIGHT / 2));
        final int leftX = REGION_LEFT;
        final int rightX = REGION_RIGHT;

        // The centers
        if (where == CENTER) {
            for (int i=WIDTH/2; i<rightX; i+=size) {
                checkNonRepeating(size, i, centerY, false);
            }
            for (int i=WIDTH/2; i>leftX; i-=size) {
                checkNonRepeating(size, i, centerY, false);
            }
        } else if (where == LEFT) {
            for (int i=centerX; i<rightX; i+=size) {
                checkNonRepeating(size, i, centerY, false);
            }
        } else {
            for (int i=rightX - halfSize; i>leftX; i-=size) {
                checkNonRepeating(size, i, centerY, false);
            }
        }
        // Every pixel outside the image
        checkRegionFillOutsideTheseBounds(Color.BLACK, leftX, centerY-halfSize, REGION_WIDTH, size);
        // Should be butted up to the left and right edges
        checkRegionLeftBoundary(0, null, SCENE_FILL, 0, TOLERANCE);
        checkRegionRightBoundary(0, null, SCENE_FILL, 0, TOLERANCE);
    }

    private void checkHorizontalStripAlongBottom(int size, int where) {
        final int halfSize = size / 2;
        final int centerX = (REGION_LEFT + halfSize);
        final int centerY = (REGION_BOTTOM - halfSize);
        final int leftX = REGION_LEFT;
        final int rightX = REGION_RIGHT;
        final int bottomY = REGION_BOTTOM;

        // The centers
        if (where == CENTER) {
            for (int i=WIDTH/2; i<rightX; i+=size) {
                checkNonRepeating(size, i, centerY, false);
            }
            for (int i=WIDTH/2; i>leftX; i-=size) {
                checkNonRepeating(size, i, centerY, false);
            }
        } else if (where == LEFT) {
            for (int i=centerX; i<rightX; i+=size) {
                checkNonRepeating(size, i, centerY, false);
            }
        } else {
            for (int i=rightX - halfSize; i>leftX; i-=size) {
                checkNonRepeating(size, i, centerY, false);
            }
        }
        // Every pixel outside the image
        checkRegionFillOutsideTheseBounds(Color.BLACK, leftX, bottomY - size, REGION_WIDTH, size);
        // Should be butted up to the left and right and bottom edges
        checkRegionBottomBoundary(0, Color.RED, SCENE_FILL, 0, TOLERANCE);
        // left
        assertColorDoesNotEqual(SCENE_FILL, leftX, bottomY - 1, TOLERANCE);
        assertColorEquals(SCENE_FILL, leftX - 1, bottomY, TOLERANCE);
        assertColorEquals(SCENE_FILL, leftX, bottomY, TOLERANCE);
        // right
        assertColorDoesNotEqual(SCENE_FILL, rightX - 1, bottomY - 1, TOLERANCE);
        assertColorEquals(SCENE_FILL, rightX, bottomY - 1, TOLERANCE);
        assertColorEquals(SCENE_FILL, rightX - 1, bottomY, TOLERANCE);
    }

    private void checkVerticalStripAlongLeft(int size, int where) {
        final int halfSize = size / 2;
        final int centerX = (REGION_LEFT + halfSize);
        final int centerY = (REGION_TOP + halfSize);
        final int leftX = REGION_LEFT;
        final int topY = REGION_TOP;
        final int rightX = (REGION_RIGHT);
        final int bottomY = (REGION_BOTTOM);

        // The centers
        if (where == CENTER) {
            for (int i=HEIGHT/2; i<bottomY; i+=size) {
                checkNonRepeating(size, centerX, i, false);
            }
            for (int i=HEIGHT/2; i>topY; i-=size) {
                checkNonRepeating(size, centerX, i, false);
            }
        } else if (where == TOP) {
            for (int i=centerY; i<bottomY; i+=size) {
                checkNonRepeating(size, centerX, i, false);
            }
        } else {
            for (int i=bottomY - halfSize; i>topY; i-=size) {
                checkNonRepeating(size, centerX, i, false);
            }
        }
        // Every pixel outside the image
        checkRegionFillOutsideTheseBounds(Color.BLACK, leftX, topY, size, REGION_HEIGHT);
        // Should be butted up to the left and top and bottom edges
        checkRegionLeftBoundary(0, Color.RED, SCENE_FILL, 0, TOLERANCE);
        // Top
        assertColorDoesNotEqual(SCENE_FILL, leftX, topY, TOLERANCE); // inside
        assertColorEquals(SCENE_FILL, leftX - 1, topY, TOLERANCE); // outside left
        assertColorEquals(SCENE_FILL, leftX, topY - 1, TOLERANCE); // outside top
        // Bottom
        assertColorDoesNotEqual(SCENE_FILL, leftX, bottomY - 1, TOLERANCE); // inside
        assertColorEquals(SCENE_FILL, leftX - 1, bottomY - 1, TOLERANCE); // outside left
        assertColorEquals(SCENE_FILL, leftX, bottomY, TOLERANCE); // outside bottom
    }

    private void checkVerticalStripAlongCenter(int size, int where) {
        final int halfSize = size / 2;
        final int centerX = (REGION_LEFT + (REGION_WIDTH / 2));
        final int centerY = (REGION_TOP + halfSize);
        final int leftX = REGION_LEFT;
        final int topY = REGION_TOP;
        final int rightX = (REGION_RIGHT);
        final int bottomY = (REGION_BOTTOM);

        // The centers
        if (where == CENTER) {
            for (int i=HEIGHT/2; i<bottomY; i+=size) {
                checkNonRepeating(size, centerX, i, false);
            }
            for (int i=HEIGHT/2; i>topY; i-=size) {
                checkNonRepeating(size, centerX, i, false);
            }
        } else if (where == TOP) {
            for (int i=centerY; i<bottomY; i+=size) {
                checkNonRepeating(size, centerX, i, false);
            }
        } else {
            for (int i=bottomY - halfSize; i>topY; i-=size) {
                checkNonRepeating(size, centerX, i, false);
            }
        }
        // Every pixel outside the image
        checkRegionFillOutsideTheseBounds(Color.BLACK, centerX-halfSize, topY, size, REGION_HEIGHT);
        // Should be butted up to the top and bottom edges
        // Top
        assertColorDoesNotEqual(SCENE_FILL, centerX, topY, TOLERANCE); // inside
        assertColorEquals(SCENE_FILL, centerX, topY - 1, TOLERANCE); // outside top
        // Bottom
        assertColorDoesNotEqual(SCENE_FILL, centerX, bottomY - 1, TOLERANCE); // inside
        assertColorEquals(SCENE_FILL, centerX, bottomY, TOLERANCE); // outside bottom
    }

    private void checkVerticalStripAlongRight(int size, int where) {
        final int halfSize = size / 2;
        final int centerX = (REGION_RIGHT - halfSize);
        final int centerY = (REGION_TOP + halfSize);
        final int leftX = REGION_LEFT;
        final int topY = REGION_TOP;
        final int rightX = (REGION_RIGHT);
        final int bottomY = (REGION_BOTTOM);

        // The centers
        if (where == CENTER) {
            for (int i=HEIGHT/2; i<bottomY; i+=size) {
                checkNonRepeating(size, centerX, i, false);
            }
            for (int i=HEIGHT/2; i>topY; i-=size) {
                checkNonRepeating(size, centerX, i, false);
            }
        } else if (where == TOP) {
            for (int i=centerY; i<bottomY; i+=size) {
                checkNonRepeating(size, centerX, i, false);
            }
        } else {
            for (int i=bottomY - halfSize; i>topY; i-=size) {
                checkNonRepeating(size, centerX, i, false);
            }
        }
        // Every pixel outside the image
        checkRegionFillOutsideTheseBounds(Color.BLACK, rightX-size, topY, size, REGION_HEIGHT);
        // Should be butted up to the right and top and bottom edges
        checkRegionRightBoundary(0, Color.RED, SCENE_FILL, 0, TOLERANCE);
        // Top
        assertColorDoesNotEqual(SCENE_FILL, rightX - 1, topY, TOLERANCE); // inside
        assertColorEquals(SCENE_FILL, rightX, topY, TOLERANCE); // outside right
        assertColorEquals(SCENE_FILL, rightX - 1, topY - 1, TOLERANCE); // outside top
        // Bottom
        assertColorDoesNotEqual(SCENE_FILL, rightX - 1, bottomY - 1, TOLERANCE); // inside
        assertColorEquals(SCENE_FILL, rightX, bottomY - 1, TOLERANCE); // outside right
        assertColorEquals(SCENE_FILL, rightX - 1, bottomY, TOLERANCE); // outside bottom
    }

    private void checkNonRepeating(int size, int centerX, int centerY, boolean checkForBlack) {
        final int halfSize = size / 2;
        final int quarterSize = size / 4;
        // The image
        int x = centerX - halfSize;
        int y = centerY - halfSize;
        assertColorEquals(contains(x, y) ? Color.RED : SCENE_FILL, x, y, TOLERANCE);
        x = centerX - quarterSize;
        y = centerY - quarterSize;
        assertColorEquals(contains(x, y) ? Color.rgb(0, 255, 0) : SCENE_FILL, x, y, TOLERANCE);
        x = centerX;
        y = centerY;
        assertColorEquals(contains(x, y) ? Color.BLUE : SCENE_FILL, x, y, TOLERANCE);
        // Every pixel outside the image
        if (checkForBlack) checkRegionFillOutsideTheseBounds(Color.BLACK, centerX - halfSize, centerY - halfSize, size, size);
    }

    // This contains has an inclusive of minX, minY, but exclusive of maxX, maxY because when filling the pixels
    // of a rectangle, this is what happens.
    private boolean contains(int x, int y) {
        final Bounds bounds = region.getBoundsInParent();
        return x >= bounds.getMinX() && x < bounds.getMaxX() && y >= bounds.getMinY() && y < bounds.getMaxY();
    }
}
