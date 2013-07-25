/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

package region;

import javafx.scene.layout.Region;

/**
 * Visual tests for background images.
 */
public class RegionBackgroundImageUITest extends RegionUITestBase {
    public static void main(String[] args) {
        launch(args);
    }

    /**************************************************************************
     *                                                                        *
     * Tests for aligned background images. The test image in use is chose    *
     * to align naturally with the edge of the test region.                   *
     *                                                                        *
     *************************************************************************/

    public void alignedImage(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');");
    }

    public void alignedImage_RepeatX(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-repeat: repeat-x");
    }

    public void alignedImage_RepeatY(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-repeat: repeat-y");
    }

    public void alignedImage_Space(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-repeat: space space");
    }

    public void alignedImage_Round(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-repeat: round");
    }

    public void alignedImage_RoundSpace(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-repeat: round space");
    }

    public void alignedImage_PositionCenter(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-repeat: no-repeat;" +
                        "-fx-background-position: center");
    }

    public void alignedImage_PositionCenterFiftyPercent(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-repeat: no-repeat;" +
                        "-fx-background-position: 50%");
    }

    public void alignedImage_PositionCenterLeft(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-repeat: no-repeat;" +
                        "-fx-background-position: left center");
    }

    public void alignedImage_PositionCenterRight(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-repeat: no-repeat;" +
                        "-fx-background-position: right center");
    }

    public void alignedImage_PositionCenterTop(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-repeat: no-repeat;" +
                        "-fx-background-position: center top");
    }

    public void alignedImage_PositionCenterBottom(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-repeat: no-repeat;" +
                        "-fx-background-position: center bottom");
    }

    public void alignedImage_PositionBottomRight(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-repeat: no-repeat;" +
                        "-fx-background-position: right bottom");
    }

    public void alignedImage_PositionCenterRepeatX(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-repeat: repeat-x;" +
                        "-fx-background-position: center");
    }

    public void alignedImage_PositionCenterFiftyPercentRepeatX(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-repeat: repeat-x;" +
                        "-fx-background-position: 50%");
    }

    public void alignedImage_PositionCenterLeftRepeatX(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-repeat: repeat-x;" +
                        "-fx-background-position: left center");
    }

    public void alignedImage_PositionCenterRightRepeatX(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-repeat: repeat-x;" +
                        "-fx-background-position: right center");
    }

    public void alignedImage_PositionCenterTopRepeatX(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-repeat: repeat-x;" +
                        "-fx-background-position: center top");
    }

    public void alignedImage_PositionCenterBottomRepeatX(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-repeat: repeat-x;" +
                        "-fx-background-position: center bottom");
    }

    public void alignedImage_PositionBottomRightRepeatX(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-repeat: repeat-x;" +
                        "-fx-background-position: right bottom");
    }

    public void alignedImage_PositionCenterRepeatY(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-repeat: repeat-y;" +
                        "-fx-background-position: center");
    }

    public void alignedImage_PositionCenterFiftyPercentRepeatY(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-repeat: repeat-y;" +
                        "-fx-background-position: 50%");
    }

    public void alignedImage_PositionCenterLeftRepeatY(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-repeat: repeat-y;" +
                        "-fx-background-position: left center");
    }

    public void alignedImage_PositionCenterRightRepeatY(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-repeat: repeat-y;" +
                        "-fx-background-position: right center");
    }

    public void alignedImage_PositionCenterTopRepeatY(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-repeat: repeat-y;" +
                        "-fx-background-position: center top");
    }

    public void alignedImage_PositionCenterBottomRepeatY(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-repeat: repeat-y;" +
                        "-fx-background-position: center bottom");
    }

    public void alignedImage_PositionBottomRightRepeatY(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-repeat: repeat-y;" +
                        "-fx-background-position: right bottom");
    }

    public void alignedImage_Position25PercentLeft(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-repeat: no-repeat;" +
                        "-fx-background-position: left 25% center");
    }

    public void alignedImage_Position25PercentRight(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-repeat: no-repeat;" +
                        "-fx-background-position: right 25% center");
    }

    // TODO should do from the top & bottom

    // TODO should test that cover causes other properties to be ignored
    public void alignedImage_Cover(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-size: cover;");
    }

    // TODO should test that contain causes other properties to be ignored
    public void alignedImage_Contain(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-size: contain;");
    }

    public void alignedImage_ContainNoRepeat(Region region) {
        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
                        "-fx-background-size: contain;" +
                        "-fx-background-repeat: no-repeat;");
    }

    public void unalignedImage(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');");
    }

    public void unalignedImage_RepeatX(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-repeat: repeat-x");
    }

    public void unalignedImage_RepeatY(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-repeat: repeat-y");
    }

    public void unalignedImage_Space(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-repeat: space space");
    }

    public void unalignedImage_Round(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-repeat: round");
    }

    public void unalignedImage_RoundSpace(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-repeat: round space");
    }

    public void unalignedImage_PositionCenter(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-repeat: no-repeat;" +
                        "-fx-background-position: center");
    }

    public void unalignedImage_PositionCenterFiftyPercent(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-repeat: no-repeat;" +
                        "-fx-background-position: 50%");
    }

    public void unalignedImage_PositionCenterLeft(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-repeat: no-repeat;" +
                        "-fx-background-position: left center");
    }

    public void unalignedImage_PositionCenterRight(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-repeat: no-repeat;" +
                        "-fx-background-position: right center");
    }

    public void unalignedImage_PositionCenterTop(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-repeat: no-repeat;" +
                        "-fx-background-position: center top");
    }

    public void unalignedImage_PositionCenterBottom(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-repeat: no-repeat;" +
                        "-fx-background-position: center bottom");
    }

    public void unalignedImage_PositionBottomRight(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-repeat: no-repeat;" +
                        "-fx-background-position: right bottom");
    }

    public void unalignedImage_PositionCenterRepeatX(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-repeat: repeat-x;" +
                        "-fx-background-position: center");
    }

    public void unalignedImage_PositionCenterFiftyPercentRepeatX(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-repeat: repeat-x;" +
                        "-fx-background-position: 50%");
    }

    public void unalignedImage_PositionCenterLeftRepeatX(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-repeat: repeat-x;" +
                        "-fx-background-position: left center");
    }

    public void unalignedImage_PositionCenterRightRepeatX(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-repeat: repeat-x;" +
                        "-fx-background-position: right center");
    }

    public void unalignedImage_PositionCenterTopRepeatX(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-repeat: repeat-x;" +
                        "-fx-background-position: center top");
    }

    public void unalignedImage_PositionCenterBottomRepeatX(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-repeat: repeat-x;" +
                        "-fx-background-position: center bottom");
    }

    public void unalignedImage_PositionBottomRightRepeatX(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-repeat: repeat-x;" +
                        "-fx-background-position: right bottom");
    }

    public void unalignedImage_PositionCenterRepeatY(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-repeat: repeat-y;" +
                        "-fx-background-position: center");
    }

    public void unalignedImage_PositionCenterFiftyPercentRepeatY(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-repeat: repeat-y;" +
                        "-fx-background-position: 50%");
    }

    public void unalignedImage_PositionCenterLeftRepeatY(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-repeat: repeat-y;" +
                        "-fx-background-position: left center");
    }

    public void unalignedImage_PositionCenterRightRepeatY(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-repeat: repeat-y;" +
                        "-fx-background-position: right center");
    }

    public void unalignedImage_PositionCenterTopRepeatY(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-repeat: repeat-y;" +
                        "-fx-background-position: center top");
    }

    public void unalignedImage_PositionCenterBottomRepeatY(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-repeat: repeat-y;" +
                        "-fx-background-position: center bottom");
    }

    public void unalignedImage_PositionBottomRightRepeatY(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-repeat: repeat-y;" +
                        "-fx-background-position: right bottom");
    }

    public void unalignedImage_Position25PercentLeft(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-repeat: no-repeat;" +
                        "-fx-background-position: left 25% center");
    }

    public void unalignedImage_Position25PercentRight(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-repeat: no-repeat;" +
                        "-fx-background-position: right 25% center");
    }

    // TODO should do from the top & bottom

    // TODO should test that cover causes other properties to be ignored
    public void unalignedImage_Cover(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-size: cover;");
    }

    // TODO should test that contain causes other properties to be ignored
    public void unalignedImage_Contain(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-size: contain;");
    }

    public void unalignedImage_ContainNoRepeat(Region region) {
        region.setStyle("-fx-background-image: url('region/test48x48.png');" +
                        "-fx-background-size: contain;" +
                        "-fx-background-repeat: no-repeat;");
    }

    // TODO need to write tests for when there are multiple images

    // TODO there are probably a hundred other combinations to try, easy.
    // And then there are another thousand combinations where some of the
    // settings are ignored, and we need to ensure that indeed they are ignored.
    // TODO I need to turn most of these tests into proper unit tests as well
    // so that we know that the CSS->Region process is working correctly.

    // TODO need to write tests where SPACE is the repeat but only 2 tiles fit, and where only 1 tile fits.
}
