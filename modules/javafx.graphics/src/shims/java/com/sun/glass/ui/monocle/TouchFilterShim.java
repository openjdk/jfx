/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle;

public class TouchFilterShim  implements TouchFilter {

    @Override
    public boolean filter(TouchState ts) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public boolean flush(TouchState ts) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public int getPriority() {
        throw new RuntimeException("not implemented");
    }

    public static class TranslateFilter extends TouchFilterShim {
        @Override
        public boolean filter(TouchState state) {
            for (int i = 0; i < state.getPointCount(); i++) {
                state.getPoint(i).x += 8;
                state.getPoint(i).y -= 5;
            }
            return false;
        }

        @Override
        public int getPriority() {
            return 50;
        }

        @Override
        public boolean flush(TouchState state) {
            return false;
        }
    }

    public static class OverrideIDFilter extends TouchFilterShim {
        @Override
        public boolean filter(TouchState state) {
            for (int i = 0; i < state.getPointCount(); i++) {
                state.getPoint(i).id = 5;
            }
            return false;
        }

        @Override
        public int getPriority() {
            return -50;
        }

        @Override
        public boolean flush(TouchState state) {
            return false;
        }
    }

    public static class NoMultiplesOfTenOnXFilter extends TouchFilterShim {
        @Override
        public boolean filter(TouchState state) {
            for (int i = 0; i < state.getPointCount(); i++) {
                if (state.getPoint(i).x % 10 == 0) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int getPriority() {
            return 60;
        }

        @Override
        public boolean flush(TouchState state) {
            return false;
        }
    }

    public static class LoggingFilter extends TouchFilterShim {
        @Override
        public boolean filter(TouchState state) {
            for (int i = 0; i < state.getPointCount(); i++) {
                TestLogShim.format("Touch point id=%d at %d,%d",
                               state.getPoint(i).id,
                               state.getPoint(i).x,
                               state.getPoint(i).y);
            }
            return false;
        }

        @Override
        public int getPriority() {
            return -100;
        }

        @Override
        public boolean flush(TouchState state) {
            return false;
        }
    }

    public static class FlushingFilter extends TouchFilterShim {
        int i = 3;
        @Override
        public boolean filter(TouchState state) {
            return false;
        }

        @Override
        public int getPriority() {
            return 90;
        }

        @Override
        public boolean flush(TouchState state) {
            if (i > 0) {
                i --;
                state.clear();
                TouchState.Point p = state.addPoint(null);
                p.x = 205 + i * 100;
                p.y = 100;
                p.id = -1;
                return true;
            } else {
                return false;
            }
        }
    }

}
