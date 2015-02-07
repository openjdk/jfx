/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package industrial;

import static industrial.Industrial.chartWidth;
import javafx.scene.Group;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

/**
 *
 * @author ddhill
 */
public class Help {

    static final String helpText =
            "Welcome to the Industrial Demo" +
            "\n\n" +
            "This is a simplistic system containing a holding tank that is fed " +
            "by a pump, and is drained by two flow controlled outlets." +
            "The change in the level of the tank is computed about 4 times a " +
            "second and is the sum of the incoming rate minus the outgoing rates." +
            "\n\n" +
            "There is a high and low water mark indicated on the tank. " +
            "When the fill level drops below the low water mark, the pump will "+ 
            "be changed to full (10) and above the high water mark, the pump " +
            "will be turned off (0).\n\n" +
            "The high and low water marks can be controlled with the sliders " +
            "next to the tanks." +
            "\n\n" +
            "There are two history charts that can be shown/hidden using the " +
            "buttons. These charts will show a sliding window of historical data." +
            "\n\n" +
            "Click the Help button to hide this text.";

    static Group createHelp(int width, int height) {

        TextArea fd = new TextArea();
        fd.setEditable(false);
        fd.setWrapText(true);
        fd.setText(helpText);
        fd.setFont(new Font(18));
        fd.setPrefWidth(width);
        fd.setPrefHeight(height);

        return  new Group(
                fd
        );
        
    }
    
}
