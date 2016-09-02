/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.media.jfxmedia.control;

/**
 * MediaPlayerOverlay interface provides control of media player overlay.
 */
public interface MediaPlayerOverlay {

    public void setOverlayX(final double x);

    public void setOverlayY(final double y);

    public void setOverlayVisible(final boolean visible);

    public void setOverlayWidth(final double width);

    public void setOverlayHeight(final double height);

    public void setOverlayPreserveRatio(final boolean preserveRatio);

    public void setOverlayOpacity(final double opacity);

    public void setOverlayTransform(
            final double mxx, final double mxy, final double mxz, final double mxt,
            final double myx, final double myy, final double myz, final double myt,
            final double mzx, final double mzy, final double mzz, final double mzt);
}
