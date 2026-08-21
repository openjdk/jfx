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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import com.sun.javafx.stage.StageBackdropHelper;

/**
 * The backdrop of a {@code Stage}. Each {@code Stage} has at most one
 * backdrop.
 *
 * @since 28
 */
@Deprecated(since = "28")
public final class StageBackdrop {

    private final StageBackdropStyle style;
    private ObservableMap<String, Object> options;

    static {
        StageBackdropHelper.setStageBackdropAccessor(
                new StageBackdropHelper.StageBackdropAccessor() {
                    @Override
                    public ObservableMap<String, Object> getOptions(StageBackdrop backdrop) {
                        return backdrop.getOptions();
                    }
                });
    }

    /**
     * Construct a new StageBackdrop with the given style
     *
     * @param style the style of the backdrop
     */
    public StageBackdrop(StageBackdropStyle style) {
        Objects.requireNonNull(style, "Stage backdrop style cannot be null");
        this.style = style;
    }

    /**
     * Gets the backdrop's style
     *
     * @return the style of the backdrop
     */
    public final StageBackdropStyle getStyle() {
        return style;
    }

    /**
     * Set a new value for an option. Pass {@code null} to set the option to
     * its default value.
     *
     * @param name the name of the option
     * @param option the new value of the option
     */
    public final void setOption(String name, Object option) {
        var avail = style.getAvailableOptions();
        var optionClass = avail.get(name);
        if (optionClass != null) {
            options = getOptions();
            if (option == null) {
                options.remove(name);
            } else if (optionClass.isInstance(option)) {
                options.put(name, option);
            }
        }
    }

    /**
     * Get the current value for an option
     *
     * @param name the name of the option
     * @return the value of the option
     */
    public final Object getOption(String name) {
        if (options == null) return null;
        return options.get(name);
    }

    // Called when setting or observing options. Returns null if there are no
    // available options.
    private ObservableMap<String, Object> getOptions() {
        if (options == null) {
            if (style.getAvailableOptions().size() > 0) {
                options = FXCollections.observableMap(new HashMap<>());
            }
        }
        return options;
    }
}

