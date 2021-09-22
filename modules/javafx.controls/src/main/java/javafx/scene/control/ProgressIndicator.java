/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control;

import javafx.css.PseudoClass;

import javafx.scene.control.skin.ProgressIndicatorSkin;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.value.WritableValue;
import javafx.css.StyleableProperty;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;

/**
 * A circular control which is used for indicating progress, either
 * infinite (aka indeterminate) or finite. Often used with the Task API for
 * representing progress of background Tasks.
 * <p>
 * ProgressIndicator sets focusTraversable to false.
 * </p>
 *
 * <p>
 * This first example creates a ProgressIndicator with an indeterminate value:
 * <pre><code> ProgressIndicator p1 = new ProgressIndicator();</code></pre>
 *
 * <img src="doc-files/ProgressIndicator_indeterminate.png" alt="Image of the indeterminate progress ProgressIndicator control">
 *
 * <p>
 * This next example creates a ProgressIndicator which is 25% complete:
 * <pre><code> ProgressIndicator p2 = new ProgressIndicator();
 * p2.setProgress(0.25F);</code></pre>
 *
 * <img src="doc-files/ProgressIndicator.png" alt="Image of the ProgressIndicator control">
 *
 * @since JavaFX 2.0
 */
public class ProgressIndicator extends Control {

    /**
     * Value for progress indicating that the progress is indeterminate.
     *
     * @see #setProgress
     */
    public static final double INDETERMINATE_PROGRESS = -1;

    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new indeterminate ProgressIndicator.
     */
    public ProgressIndicator() {
        this(INDETERMINATE_PROGRESS);
    }

    /**
     * Creates a new ProgressIndicator with the given progress value.
     * @param progress the progress, represented as a value between 0 and 1
     */
    public ProgressIndicator(double progress) {
        // focusTraversable is styleable through css. Calling setFocusTraversable
        // makes it look to css like the user set the value and css will not
        // override. Initializing focusTraversable by calling applyStyle with null
        // StyleOrigin ensures that css will be able to override the value.
        ((StyleableProperty<Boolean>)(WritableValue<Boolean>)focusTraversableProperty()).applyStyle(null, Boolean.FALSE);
        setProgress(progress);
        getStyleClass().setAll(DEFAULT_STYLE_CLASS);
        setAccessibleRole(AccessibleRole.PROGRESS_INDICATOR);

        // need to initialize pseudo-class state
        final int c = Double.compare(INDETERMINATE_PROGRESS, progress);
        pseudoClassStateChanged(PSEUDO_CLASS_INDETERMINATE, c == 0);
        pseudoClassStateChanged(PSEUDO_CLASS_DETERMINATE,   c != 0);
    }
    /* *************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/
    /**
     * A flag indicating whether it is possible to determine the progress
     * of the ProgressIndicator. Typically indeterminate progress bars are
     * rendered with some form of animation indicating potentially "infinite"
     * progress.
     */
    private ReadOnlyBooleanWrapper indeterminate;
    private void setIndeterminate(boolean value) {
        indeterminatePropertyImpl().set(value);
    }

    public final boolean isIndeterminate() {
        return indeterminate == null ? true : indeterminate.get();
    }

    public final ReadOnlyBooleanProperty indeterminateProperty() {
        return indeterminatePropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyBooleanWrapper indeterminatePropertyImpl() {
        if (indeterminate == null) {
            indeterminate = new ReadOnlyBooleanWrapper(true) {
                @Override protected void invalidated() {
                    final boolean active = get();
                    pseudoClassStateChanged(PSEUDO_CLASS_INDETERMINATE, active);
                    pseudoClassStateChanged(PSEUDO_CLASS_DETERMINATE,  !active);
                }

                @Override
                public Object getBean() {
                    return ProgressIndicator.this;
                }

                @Override
                public String getName() {
                    return "indeterminate";
                }
            };
        }
        return indeterminate;
    }
    /**
     * The actual progress of the ProgressIndicator. A negative value for
     * progress indicates that the progress is indeterminate. A positive value
     * between 0 and 1 indicates the percentage of progress where 0 is 0% and 1
     * is 100%. Any value greater than 1 is interpreted as 100%.
     */
    private DoubleProperty progress;
    public final void setProgress(double value) {
        progressProperty().set(value);
    }

    public final double getProgress() {
        return progress == null ? INDETERMINATE_PROGRESS : progress.get();
    }

    public final DoubleProperty progressProperty() {
        if (progress == null) {
            progress = new DoublePropertyBase(-1.0) {
                @Override protected void invalidated() {
                    setIndeterminate(getProgress() < 0.0);
                }

                @Override
                public Object getBean() {
                    return ProgressIndicator.this;
                }

                @Override
                public String getName() {
                    return "progress";
                }
            };
        }
        return progress;
    }

    /* *************************************************************************
     *                                                                         *
     * Methods                                                                 *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new ProgressIndicatorSkin(this);
    }

    /* *************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    /**
     * Initialize the style class to 'progress-indicator'.
     *
     * This is the selector class from which CSS can be used to style
     * this control.
     */
    private static final String DEFAULT_STYLE_CLASS = "progress-indicator";

    /**
     * Pseudoclass indicating this is a determinate (i.e., progress can be
     * determined) progress indicator.
     */
    private static final PseudoClass PSEUDO_CLASS_DETERMINATE =
            PseudoClass.getPseudoClass("determinate");

    /**
     * Pseudoclass indicating this is an indeterminate (i.e., progress cannot
     * be determined) progress indicator.
     */
    private static final PseudoClass PSEUDO_CLASS_INDETERMINATE =
            PseudoClass.getPseudoClass("indeterminate");

    /**
     * Returns the initial focus traversable state of this control, for use
     * by the JavaFX CSS engine to correctly set its initial value. This method
     * is overridden as by default UI controls have focus traversable set to true,
     * but that is not appropriate for this control.
     *
     * @since 9
     */
    @Override protected Boolean getInitialFocusTraversable() {
        return Boolean.FALSE;
    }


    /* *************************************************************************
     *                                                                         *
     * Accessibility handling                                                  *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override
    public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case VALUE: return getProgress();
            case MAX_VALUE: return 1.0;
            case MIN_VALUE: return 0.0;
            case INDETERMINATE: return isIndeterminate();
            default: return super.queryAccessibleAttribute(attribute, parameters);
        }
    }

}
