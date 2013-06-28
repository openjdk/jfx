/*
 * Copyright (c) 2008, 2012 Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.javafx.experiments.scheduleapp.control;

import com.sun.javafx.scene.control.skin.ScrollPaneSkin;
import java.lang.reflect.Field;
import javafx.event.EventHandler;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

public class ScrollPaneSkin3 extends ScrollPaneSkin {
    private static final String os = System.getProperty("os.name");
    public static final boolean IS_BEAGLE = "Linux".equals(os);
    public ScrollPaneSkin3(final ScrollPane scrollpane) {
        super(scrollpane);
        if (IS_BEAGLE) {
            scrollpane.addEventFilter(MouseEvent.ANY, new EventHandler<MouseEvent>() {
                public void handle(MouseEvent event) {
                    if (event.getEventType() == MouseEvent.MOUSE_DRAGGED) {
                        event.consume();
                    }
                    scrollpane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                }
            });
            scrollpane.addEventFilter(ScrollEvent.SCROLL, new EventHandler<ScrollEvent>() {
                public void handle(ScrollEvent event) {
                    if (vsb.getVisibleAmount() < vsb.getMax()) {
                        double nodeHeight = getNodeHeight();
                        double vRange = getSkinnable().getVmax()-getSkinnable().getVmin();
                        double vPixelValue;
                        if (nodeHeight > 0.0) {
                            vPixelValue = vRange / nodeHeight;
                        }
                        else {
                            vPixelValue = 0.0;
                        }
                        double newValue = vsb.getValue()+(-event.getDeltaY())*vPixelValue;
                        if (true /*!PlatformUtil.isEmbedded()*/) {
                            if ((event.getDeltaY() > 0.0 && vsb.getValue() > vsb.getMin()) ||
                                (event.getDeltaY() < 0.0 && vsb.getValue() < vsb.getMax())) {
                                vsb.setValue(newValue);
                                event.consume();
                            }
                        }
                        // eat all scroll events to avoid white space scrolling past start or end
                        event.consume();
                    }
                }
            });
        }
    }
    double getNodeHeight() {
        try {
            Field field = ScrollPaneSkin.class.getDeclaredField("nodeHeight");
            field.setAccessible(true);
            return field.getDouble(this);
        } catch (NoSuchFieldException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return 0;
    }

    protected void startContentsToViewport() {
        if (IS_BEAGLE) return;
        super.startContentsToViewport();
    }

    protected void startSBReleasedAnimation() {
        if (IS_BEAGLE) return;
        super.startSBReleasedAnimation();
    }
}
