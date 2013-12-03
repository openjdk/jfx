/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.metadata.util;

import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * Encoder for the gradient color
 */
public class GradientEncoder {

    public static String encodeLinearGradient(LinearGradient gradient) {
        // For now, simple case is handled. Is there any other cases ?
        // Need to check all possible syntaxes.
        final StringBuilder sb = new StringBuilder();
        sb.append("linear-gradient("); //NOI18N
        sb.append("from "); //NOI18N
        sb.append(gradient.getStartX() * 100);
        sb.append("% "); //NOI18N
        sb.append(gradient.getStartY() * 100);
        sb.append("% "); //NOI18N
        sb.append("to "); //NOI18N
        sb.append(gradient.getEndX()* 100);
        sb.append("% "); //NOI18N
        sb.append(gradient.getEndY() * 100);
        sb.append("%, "); //NOI18N

        int index = 1;
        for (Stop stop : gradient.getStops()) {
            final String color = ColorEncoder.encodeColor(stop.getColor());
            sb.append(color);
            sb.append(" ");
            sb.append(stop.getOffset() * 100);
            sb.append("%");
            if (index++ < gradient.getStops().size()) {
                sb.append(", ");
            }
        }
        sb.append(")"); //NOI18N
        return sb.toString();
    }

    public static String encodeRadialGradient(RadialGradient gradient) {
        // For now, simple case is handled. Is there any other cases ?
        // Need to check all possible syntaxes.
        final StringBuilder sb = new StringBuilder();
        sb.append("radial-gradient("); //NOI18N
        sb.append("focus-angle "); //NOI18N
        sb.append(gradient.getFocusAngle() * 100);
        sb.append("deg, "); //NOI18N
        sb.append("focus-distance "); //NOI18N
        sb.append(gradient.getFocusDistance() * 100);
        sb.append("%, "); //NOI18N
        sb.append("center "); //NOI18N
        sb.append(gradient.getCenterX() * 100);
        sb.append("% "); //NOI18N
        sb.append(gradient.getCenterY() * 100);
        sb.append("%, "); //NOI18N
        sb.append("radius "); //NOI18N
        sb.append(gradient.getRadius() * 100);
        sb.append("%, "); //NOI18N

        int index = 1;
        for (Stop stop : gradient.getStops()) {
            final String color = ColorEncoder.encodeColor(stop.getColor());
            sb.append(color);
            sb.append(" ");
            sb.append(stop.getOffset() * 100);
            sb.append("%");
            if (index++ < gradient.getStops().size()) {
                sb.append(", ");
            }
        }
        sb.append(")"); //NOI18N
        return sb.toString();
    }
}
