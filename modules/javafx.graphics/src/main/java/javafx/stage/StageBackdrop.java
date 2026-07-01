/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package javafx.stage;

import java.util.Map;
import java.util.HashMap;
import java.lang.ref.WeakReference;

import com.sun.javafx.stage.WindowHelper;

/**
 * The backdrop of a {@code Stage}. Each {@code Stage} has at most one
 * backdrop and each backdrop is associated with a single stage.
 *
 * @since 27
 */
@Deprecated(since = "27")
public final class StageBackdrop {

    private final StageBackdropStyle style;
    private WeakReference<Stage> stage;
    private Map<String, Object> options;

    StageBackdrop(StageBackdropStyle style, Stage stage) {
        this.style = style;
        this.stage = new WeakReference(stage);
    }

    void clearStage() {
        stage = null;
    }

    /**
     * Gets the backdrop's style
     *
     * @return the style of the backdrop
     */
    public final StageBackdropStyle getStyle() {
        return this.style;
    }

    /**
     * Set a new value for an option.
     *
     * @param name the name of the option
     * @param option the new value of the option
     */
    public final void setOption(String name, Object option) {
        if (stage == null) return;
        if (stage.get() == null) return;
        if (stage.get().getBackdrop() != this) return;

        var avail = style.getAvailableOptions();
        var optionClass = avail.get(name);
        if (optionClass != null && optionClass.isInstance(option)) {
            if (options == null) {
                options = new HashMap<>();
            }
            options.put(name, option);
            var peerWindow = WindowHelper.getPeer(stage.get());
            if (peerWindow != null) {
                peerWindow.setBackdropOption(name, option);
            }
        }
    }

    /**
     * Get the current value for an option.
     *
     * @param name the name of the option
     * @return the value of the option
     */
    public final Object getOption(String name) {
        if (options == null) return null;
        return options.get(name);
    }
}

