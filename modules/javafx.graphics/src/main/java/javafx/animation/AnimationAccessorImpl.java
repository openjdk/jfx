/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

package javafx.animation;

import com.sun.scenario.animation.shared.AnimationAccessor;

final class AnimationAccessorImpl extends AnimationAccessor{

    @Override
    public void setCurrentRate(Animation animation, double currentRate) {
        animation.setCurrentRate(currentRate);
    }

    @Override
    public void playTo(Animation animation, long pos, long cycleTicks) {
        animation.doPlayTo(pos, cycleTicks);
    }

    @Override
    public void jumpTo(Animation animation, long pos, long cycleTicks, boolean forceJump) {
        animation.doJumpTo(pos, cycleTicks, forceJump);
    }

    @Override
    public void finished(Animation animation) {
        animation.finished();
    }

    @Override
    public void setCurrentTicks(Animation animation, long ticks) {
        animation.setCurrentTicks(ticks);
    }


}
