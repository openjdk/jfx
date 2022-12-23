/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.paint;

import java.util.LinkedList;
import java.util.List;
import javafx.scene.paint.Color;
import javafx.scene.paint.Stop;

public class GradientUtils {

    public static String lengthToString(double value, boolean proportional) {
        if (proportional) {
            return (value * 100) + "%";
        } else {
            return value + "px";
        }
    }

    public static class Point {

        public static final Point MIN = new Point(0, true);
        public static final Point MAX = new Point(1, true);
        public double value;
        public boolean proportional;

        @Override
        public String toString() {
            return "value = " + value + ", proportional = " + proportional;
        }

        public Point(double value, boolean proportional) {
            this.value = value;
            this.proportional = proportional;
        }

        public Point() {
        }
    }

    public static class Parser {

        private int index;
        private String[] tokens;
        private boolean proportional;
        private boolean proportionalSet = false;

        private interface Delimiter {
            public boolean isDelimiter(char value);
        }

        private String[] splitString(String string, Delimiter delimiter, boolean canRepeat) {
            List<String> tokenList = new LinkedList<>();
            StringBuilder token = new StringBuilder();
            int i = 0;
            char[] input = string.toCharArray();
            while (i < input.length) {
                char currentChar = input[i];

                if (delimiter.isDelimiter(currentChar)) {
                    if (!canRepeat || token.length() > 0) {
                        tokenList.add(token.toString());
                    }
                    token.setLength(0);
                } else if (currentChar == '(') {
                    while (i < input.length) {
                        token.append(input[i]);
                        if (input[i] == ')') {
                            break;
                        }
                        i++;
                    }
                } else {
                    token.append(input[i]);
                }
                i++;
            }
            if (!canRepeat || token.length() > 0) {
                tokenList.add(token.toString());
            }

            return tokenList.toArray(new String[tokenList.size()]);
        }

        public Parser(String content) {
            tokens = splitString(content, value -> (value == ','), false);

            index = 0;
        }

        public int getSize() {
            return tokens.length;
        }

        public void shift() {
            index++;
        }

        public String getCurrentToken() {
            String currentToken = tokens[index].trim();
            if (currentToken.isEmpty()) {
                throw new IllegalArgumentException("Invalid gradient specification: "
                        + "found empty token.");
            }
            return currentToken;
        }

        public String[] splitCurrentToken() {
            return getCurrentToken().split("\\s");
        }

        public static void checkNumberOfArguments(String[] tokens, int count) {
            if (tokens.length < count + 1) {
                throw new IllegalArgumentException("Invalid gradient specification: "
                        + "parameter '"+ tokens[0] + "' needs " + count + " argument(s).");
            }
        }

        public static double parseAngle(String value) {
            double angle = 0;
            if (value.endsWith("deg")) {
                value = value.substring(0, value.length() - 3);
                angle = Double.parseDouble(value);
            } else if (value.endsWith("grad")) {
                value = value.substring(0, value.length() - 4);
                angle = Double.parseDouble(value);
                angle = angle * 9 / 10;
            } else if (value.endsWith("rad")) {
                value = value.substring(0, value.length() - 3);
                angle = Double.parseDouble(value);
                angle = angle * 180 / Math.PI;
            } else if (value.endsWith("turn")) {
                value = value.substring(0, value.length() - 4);
                angle = Double.parseDouble(value);
                angle = angle * 360;
            } else {
                throw new IllegalArgumentException("Invalid gradient specification:"
                        + "angle must end in deg, rad, grad, or turn");
            }

            return angle;
        }

        public static double parsePercentage(String value) {
            double percentage;
            if (value.endsWith("%")) {
                value = value.substring(0, value.length() - 1);
                percentage = Double.parseDouble(value) / 100;
            } else {
                throw new IllegalArgumentException("Invalid gradient specification: "
                        + "focus-distance must be specified as percentage");
            }
            return percentage;
        }

        public Point parsePoint(String value) {
            Point p = new Point();
            if (value.endsWith("%")) {
                p.proportional = true;
                value = value.substring(0, value.length() - 1);
            } else if (value.endsWith("px")) {
                value = value.substring(0, value.length() - 2);
            }
            p.value = Double.parseDouble(value);
            if (p.proportional) {
                p.value /= 100;
            }

            if (proportionalSet && proportional != p.proportional) {
                throw new IllegalArgumentException("Invalid gradient specification:"
                        + "cannot mix proportional and non-proportional values");
            }

            proportionalSet = true;
            proportional = p.proportional;

            return p;
        }

        // length specifies the length of gradient line used when recalculating
        // non-proportional color-stops
        public Stop[] parseStops(boolean proportional, double length) {
            int stopsCount = tokens.length - index;
            Color[] colors = new Color[stopsCount];
            double[] offsets = new double[stopsCount];
            Stop[] stops = new Stop[stopsCount];

            for (int i = 0; i < stopsCount; i++) {
                String stopString = tokens[i + index].trim();
                String[] stopTokens = splitString(stopString, value -> Character.isWhitespace(value), true);

                if (stopTokens.length == 0) {
                    throw new IllegalArgumentException("Invalid gradient specification, "
                                + "empty stop found");
                }

                String currentToken = stopTokens[0];
                double offset = -1;

                Color c = Color.web(currentToken);
                if (stopTokens.length == 2) {
                    // parsing offset
                    String o = stopTokens[1];
                    if (o.endsWith("%")) {
                        o = o.substring(0, o.length() - 1);
                        offset = Double.parseDouble(o) / 100;
                    } else if (!proportional) {
                        if (o.endsWith("px")) {
                            o = o.substring(0, o.length() - 2);
                        }
                        offset = Double.parseDouble(o) / length;
                    } else {
                        throw new IllegalArgumentException("Invalid gradient specification, "
                                + "non-proportional stops not permited in proportional gradient: " + o);
                    }
                } else if (stopTokens.length > 2) {
                    throw new IllegalArgumentException("Invalid gradient specification, "
                            + "unexpected content in stop specification: " + stopTokens[2]);
                }

                colors[i] = c;
                offsets[i] = offset;
            }

            // normalize based on CSS specification
            // If the first color-stop does not have a position, set its position to 0%.
            // If the last color-stop does not have a position, set its position to 100%.
            if (offsets[0] < 0) {
                offsets[0] = 0;
            }
            if (offsets[offsets.length - 1] < 0) {
                offsets[offsets.length - 1] = 1;
            }

            // If a color-stop has a position that is less than the specified position
            // of any color-stop before it in the list, set its position to be equal
            // to the largest specified position of any color-stop before it.
            double max = offsets[0];
            for (int i = 1; i < offsets.length; i++) {
                if (offsets[i] < max && offsets[i] > 0) {
                    offsets[i] = max;
                } else {
                    max = offsets[i];
                }
            }

            // If any color-stop still does not have a position, then,
            // for each run of adjacent color-stops without positions,
            // set their positions so that they are evenly spaced
            // between the preceding and following color-stops with positions.
            int firstIndex = -1;
            for (int i = 1; i < offsets.length; i++) {
                double offset = offsets[i];
                if (offset < 0 && firstIndex < 0) {
                    firstIndex = i;
                } else if (offset >= 0 && firstIndex > 0) {
                    int n = i - firstIndex + 1;
                    double part = (offsets[i] - offsets[firstIndex - 1]) / n;
                    for (int j = 0; j < n - 1; j++) {
                        offsets[firstIndex + j] = offsets[firstIndex - 1] + part * (j + 1);
                    }
                }
            }

            for (int i = 0; i < stops.length; i++) {
                Stop stop = new Stop(offsets[i], colors[i]);
                stops[i] = stop;
            }

            return stops;
        }
    }
}
