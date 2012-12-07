/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.css.StyleManager;
import com.sun.javafx.css.StyleablePropertyMetaData;
import com.sun.javafx.scene.control.skin.ProgressIndicatorSkin;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.geometry.NodeOrientation;


/**
 * A circular control which is used for indicating progress, either
 * infinite (aka indeterminate) or finite. Often used with the Task API for
 * representing progress of background Tasks.
 * <p>
 * ProgressIndicator sets focusTraversable to false.
 * </p>
 *
 * <p>
 * This first example creates a ProgressIndicator with an indeterminate value :
 * <pre><code>
 * import javafx.scene.control.ProgressIndicator;
 * ProgressIndicator p1 = new ProgressIndicator();
 * </code></pre>
 * 
 * <p>
 * This next example creates a ProgressIndicator which is 25% complete :
 * <pre><code>
 * import javafx.scene.control.ProgressIndicator;
 * ProgressIndicator p2 = new ProgressIndicator();
 * p2.setProgress(0.25F);
 * </code></pre>
 *
 * Implementation of ProgressIndicator According to JavaFX UI Control API Specification
 */

public class ProgressIndicator extends Control {

    /**
     * Value for progress indicating that the progress is indeterminate.
     * 
     * @see #setProgress
     */
    public static final double INDETERMINATE_PROGRESS = -1;

    /***************************************************************************
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
     */
    public ProgressIndicator(double progress) {
        setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        // focusTraversable is styleable through css. Calling setFocusTraversable
        // makes it look to css like the user set the value and css will not 
        // override. Initializing focusTraversable by calling set on the 
        // StyleablePropertyMetaData ensures that css will be able to override the value.
        final StyleablePropertyMetaData prop = StyleablePropertyMetaData.getStyleablePropertyMetaData(focusTraversableProperty());
        prop.set(this, Boolean.FALSE);            
        setProgress(progress);
        getStyleClass().setAll(DEFAULT_STYLE_CLASS);
    }
    /***************************************************************************
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
                    impl_pseudoClassStateChanged(PSEUDO_CLASS_INDETERMINATE);
                    impl_pseudoClassStateChanged(PSEUDO_CLASS_DETERMINATE);
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

    /***************************************************************************
     *                                                                         *
     * Methods                                                                 *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new ProgressIndicatorSkin(this);
    }

    /***************************************************************************
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
    private static final String PSEUDO_CLASS_DETERMINATE = "determinate";

    /**
     * Pseudoclass indicating this is an indeterminate (i.e., progress cannot
     * be determined) progress indicator.
     */
    private static final String PSEUDO_CLASS_INDETERMINATE = "indeterminate";

    private static final long INDETERMINATE_PSEUDOCLASS_STATE = StyleManager.getPseudoclassMask("indeterminate");
    private static final long DETERMINATE_PSEUDOCLASS_STATE = StyleManager.getPseudoclassMask("determinate");

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated @Override public long impl_getPseudoClassState() {
        long mask = super.impl_getPseudoClassState();
        mask |= isIndeterminate() ? INDETERMINATE_PSEUDOCLASS_STATE : DETERMINATE_PSEUDOCLASS_STATE;
        return mask;
    }
    
    
    /**
      * Most Controls return true for focusTraversable, so Control overrides
      * this method to return true, but ProgressIndicator returns false for
      * focusTraversable's initial value; hence the override of the override. 
      * This method is called from CSS code to get the correct initial value.
      * @treatAsPrivate implementation detail
      */
    @Deprecated @Override
    protected /*do not make final*/ Boolean impl_cssGetFocusTraversableInitialValue() {
        return Boolean.FALSE;
    }
    

}
