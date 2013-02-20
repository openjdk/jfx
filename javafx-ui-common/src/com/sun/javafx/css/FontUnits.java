/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.css;

import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

/** Mapping of CSS Font style and weight values to JavaFX FontPosture and FontWeight values */
final public class FontUnits {

        public static enum Style   {
                
                NORMAL(FontPosture.REGULAR),
                ITALIC(FontPosture.ITALIC),
                OBLIQUE(FontPosture.ITALIC),
                INHERIT;

                FontPosture posture;
                
                private Style() {
                        this.posture = FontPosture.REGULAR;
                }
                
                private Style(FontPosture posture) {
                        this.posture = posture;
                }

                public FontPosture toFontPosture() {
                        return posture;
                }
                        
                public String toString() {
                        return posture.toString();
                }

        };

        public static enum Weight  {
                
            // Mappings follow http://www.w3.org/TR/css3-fonts/#font-weight-the-font-weight-property
            NORMAL(FontWeight.NORMAL),
            BOLD(FontWeight.BOLD),
            BOLDER, 
            LIGHTER, 
            INHERIT, 
            SCALE_100(FontWeight.THIN),
            SCALE_200(FontWeight.EXTRA_LIGHT),
            SCALE_300(FontWeight.LIGHT),
            SCALE_400(FontWeight.NORMAL),
            SCALE_500(FontWeight.MEDIUM),
            SCALE_600(FontWeight.SEMI_BOLD),
            SCALE_700(FontWeight.BOLD),
            SCALE_800(FontWeight.EXTRA_BOLD),
            SCALE_900(FontWeight.BLACK);
                
            FontWeight weight;
                
            private Weight() {
                this.weight = FontWeight.NORMAL;
            }

            private Weight(FontWeight weight) {
                this.weight = weight;
            }

            public static Weight toWeight(FontWeight weight) {
                Weight returnVal = null;
                if (FontWeight.THIN == weight) {
                    returnVal = SCALE_100;
                } else if (FontWeight.EXTRA_LIGHT == weight) {
                    returnVal = SCALE_200;
                } else if (FontWeight.LIGHT == weight) {
                    returnVal = SCALE_300;
                } else if (FontWeight.NORMAL == weight) {
                    returnVal = SCALE_400;
                } else if (FontWeight.MEDIUM == weight) {
                    returnVal = SCALE_500;
                } else if (FontWeight.SEMI_BOLD == weight) {
                    returnVal = SCALE_600;
                } else if (FontWeight.BOLD == weight) {
                    returnVal = SCALE_700;
                } else if (FontWeight.EXTRA_BOLD == weight) {
                    returnVal = SCALE_800;
                } else if (FontWeight.BLACK == weight) {
                    returnVal = SCALE_900;
                } else {
                    returnVal = SCALE_400;
                }
                return returnVal;
            }

                public FontWeight toFontWeight() {
                    return weight;
                }

                public String toString() {
                    return weight.toString();
                }

                public Weight bolder() {
                        Weight returnVal = NORMAL;
                        switch(this) {
                            case SCALE_100:
                                        returnVal = SCALE_200;
                                        break;
                            case SCALE_200:
                                        returnVal = SCALE_300;
                                        break;
                            case SCALE_300:
                                        returnVal = SCALE_400;
                                        break;
                            default:    
                            case NORMAL:
                            case SCALE_400:
                                        returnVal = SCALE_500;
                                        break;
                            case SCALE_500:
                                        returnVal = SCALE_600;
                                        break;
                            case SCALE_600:
                                        returnVal = SCALE_700;
                                        break;
                            case BOLD:
                            case SCALE_700:
                                        returnVal = SCALE_800;
                                        break;
                            case SCALE_800:
                            case SCALE_900:
                                        returnVal = SCALE_900;
                                        break;
                        }
                        return returnVal;
                }

                public Weight lighter() {
                        Weight returnVal = NORMAL;
                        switch(this) {
                            case SCALE_100: 
                            case SCALE_200:
                                        returnVal = SCALE_100;
                                        break;
                            case SCALE_300:
                                        returnVal = SCALE_200;
                                        break;
                            default:    
                            case NORMAL:                
                            case SCALE_400:
                                        returnVal = SCALE_300;
                                        break;
                            case SCALE_500:
                                        returnVal = SCALE_400;
                                        break;
                            case SCALE_600:
                                        returnVal = SCALE_500;
                                        break;
                            case BOLD:
                            case SCALE_700:
                                        returnVal = SCALE_600;
                                        break;
                            case SCALE_800:
                                        returnVal = SCALE_700;
                                        break;
                            case SCALE_900:
                                        returnVal = SCALE_800;
                                        break;
                        }
                        return returnVal;
                }

        };
}
