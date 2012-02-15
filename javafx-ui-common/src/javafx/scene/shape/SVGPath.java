/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 */

package javafx.scene.shape;



import com.sun.javafx.Logging;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.StringProperty;
import javafx.beans.property.StringPropertyBase;

import com.sun.javafx.geom.Path2D;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.sg.PGNode;
import com.sun.javafx.sg.PGSVGPath;
import com.sun.javafx.tk.Toolkit;

/**
 * The {@code SVGPath} class represents a simple shape that is constructed by
 * parsing SVG path data from a String.
 *
<PRE>
import javafx.scene.shape.*;

SVGPath svg = new SVGPath();
svg.setContent("M40,60 C42,48 44,30 25,32");
</PRE>
 */
public  class SVGPath extends Shape {
    /**
     * Defines the filling rule constant for determining the interior of the path.
     * The value must be one of the following constants:
     * {@code FillRile.EVEN_ODD} or {@code FillRule.NON_ZERO}.
     * The default value is {@code FillRule.NON_ZERO}.
     *
     * @defaultValue FillRule.NON_ZERO
     */
    private ObjectProperty<FillRule> fillRule;

    private Path2D path2d;

    public final void setFillRule(FillRule value) {
        fillRuleProperty().set(value);
    }

    public final FillRule getFillRule() {
        return fillRule == null ? FillRule.NON_ZERO : fillRule.get();
    }

    public final ObjectProperty<FillRule> fillRuleProperty() {
        if (fillRule == null) {
            fillRule = new ObjectPropertyBase<FillRule>(FillRule.NON_ZERO) {

                @Override
                public void invalidated() {
                    impl_markDirty(DirtyBits.SHAPE_FILLRULE);
                    impl_geomChanged();
                }

                @Override
                public Object getBean() {
                    return SVGPath.this;
                }

                @Override
                public String getName() {
                    return "fillRule";
                }
            };
        }
        return fillRule;
    }

    /**
     * Defines the SVG Path encoded string as specified at:
     * <a href="http://www.w3.org/TR/SVG/paths.html">http://www.w3.org/TR/SVG/paths.html</a>.
     *
     * @defaultValue empty string
     */
    private StringProperty content;


    public final void setContent(String value) {
        contentProperty().set(value);
    }

    public final String getContent() {
        return content == null ? "" : content.get();
    }

    public final StringProperty contentProperty() {
        if (content == null) {
            content = new StringPropertyBase("") {

                @Override
                public void invalidated() {
                    impl_markDirty(DirtyBits.NODE_CONTENTS);
                    impl_geomChanged();
                    path2d = null;
                }

                @Override
                public Object getBean() {
                    return SVGPath.this;
                }

                @Override
                public String getName() {
                    return "content";
                }
            };
        }
        return content;
    }

    private Object svgPathObject;

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    protected PGNode impl_createPGNode() {
        return Toolkit.getToolkit().createPGSVGPath();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public PGSVGPath impl_getPGSVGPath() {
        return (PGSVGPath)impl_getPGNode();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public Path2D impl_configShape() {
        if (path2d == null) {
            path2d = createSVGPath2D();
        } else {
            path2d.setWindingRule(getFillRule() == FillRule.NON_ZERO ?
                                  Path2D.WIND_NON_ZERO : Path2D.WIND_EVEN_ODD);
        }

        return path2d;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public void impl_updatePG() {
        super.impl_updatePG();

        if (impl_isDirty(DirtyBits.SHAPE_FILLRULE) ||
            impl_isDirty(DirtyBits.NODE_CONTENTS))
        {
            if (impl_getPGSVGPath().acceptsPath2dOnUpdate()) {
                if (svgPathObject == null) {
                    svgPathObject = new Path2D();
                }
                Path2D tempPathObject = (Path2D) svgPathObject;
                tempPathObject.setTo(impl_configShape());
            } else {
                svgPathObject = createSVGPathObject();
            }
            impl_getPGSVGPath().setContent(svgPathObject);
        }
    }

    /**
     * Returns a string representation of this {@code SVGPath} object.
     * @return a string representation of this {@code SVGPath} object.
     */
    @Override
    public String toString() {
        return "SVGPath[" + getContent() + "]";
    }

    private Path2D createSVGPath2D() {
        try {
            return Toolkit.getToolkit().createSVGPath2D(this);
        } catch (final RuntimeException e) {
            Logging.getJavaFXLogger().warning(
                    "Failed to configure svg path \"{0}\": {1}",
                    getContent(), e.getMessage());

            return Toolkit.getToolkit().createSVGPath2D(new SVGPath());
        }
    }

    private Object createSVGPathObject() {
        try {
            return Toolkit.getToolkit().createSVGPathObject(this);
        } catch (final RuntimeException e) {
            Logging.getJavaFXLogger().warning(
                    "Failed to configure svg path \"{0}\": {1}",
                    getContent(), e.getMessage());

            return Toolkit.getToolkit().createSVGPathObject(new SVGPath());
        }
    }
}
