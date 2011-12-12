/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.beans.metadata;

import com.sun.javafx.beans.metadata.widgets.Widget3;
import com.sun.javafx.beans.metadata.widgets.Widget2;
import com.sun.javafx.beans.metadata.widgets.MultiWordWidget;
import com.sun.javafx.beans.metadata.widgets.MultiWordWidget2;
import com.sun.javafx.beans.metadata.widgets.Widget;
import com.sun.javafx.beans.metadata.widgets.displayname.Apple;
import com.sun.javafx.beans.metadata.widgets.displayname.Carrot;
import com.sun.javafx.beans.metadata.widgets.displayname.Orange;
import com.sun.javafx.beans.metadata.widgets.displayname.Pear;
import com.sun.javafx.beans.metadata.widgets.displayname.Radish;
import java.util.List;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Richard
 */
public class BeanMetaDataTest {

    public BeanMetaDataTest() {
    }

    private PropertyMetaData findProperty(BeanMetaData md, String name) {
        List<PropertyMetaData> list = md.getProperties();
        for (PropertyMetaData pmd : list) {
            if (name.equals(pmd.getName())) {
                return pmd;
            }
        }
        return null;
    }

    private PropertyMetaData findEvent(BeanMetaData md, String name) {
        List<EventMetaData> list = md.getEvents();
        for (EventMetaData pmd : list) {
            if (name.equals(pmd.getName())) {
                return pmd;
            }
        }
        return null;
    }

    /**************************************************************************
     *
     *      Tests for the "name" of a BeanMetaData
     *
     *************************************************************************/

    /**
     * This is a plain Widget (no annotations, just a short single word name)
     */
    @Test public void nameOnBeanWithNoAnnotationIsSameAsClassName() {
        BeanMetaData<Widget> metaData = new BeanMetaData<Widget>(Widget.class);
        assertEquals("Widget", metaData.getName());
    }

    /**
     * This is a plain Widget but with a multi-word name
     */
    @Test public void nameOnBeanWithNoAnnotationIsSameAsClassName2() {
        BeanMetaData<MultiWordWidget> metaData = new BeanMetaData<MultiWordWidget>(MultiWordWidget.class);
        assertEquals("MultiWordWidget", metaData.getName());
    }

    /**
     * This is a Widget with a single-word name, but with an annotation
     */
    @Test public void nameOnBeanWithAnnotationIsSameAsClassName() {
        BeanMetaData<Widget2> metaData = new BeanMetaData<Widget2>(Widget2.class);
        assertEquals("Widget2", metaData.getName());
    }

    /**
     * This is a Widget with a multi-word name, but with an annotation
     */
    @Test public void nameOnBeanWithAnnotationIsSameAsClassName2() {
        BeanMetaData<MultiWordWidget2> metaData = new BeanMetaData<MultiWordWidget2>(MultiWordWidget2.class);
        assertEquals("MultiWordWidget2", metaData.getName());
    }

    /**************************************************************************
     *
     *      Tests for the "displayName" of a BeanMetaData
     *          - Not Annotated
     *              -- No Resource Bundle Entries
     *              -- "resources" resource bundle entry
     *              -- "WidgetResources" resource bundle entry
     *          - Annotated
     *              -- No %
     *              -- % and "resources" entry
     *              -- % and "WidgetResources" entry
     *              -- % and no resource bundle entry
     *
     *************************************************************************/

    @Test public void displayNameOnBeanWithNoAnnotationIsSpaceSeparatedClassName() {
        BeanMetaData<Widget> metaData = new BeanMetaData<Widget>(Widget.class);
        assertEquals("Widget", metaData.getDisplayName());
    }

    @Test public void displayNameOnBeanWithNoAnnotationIsSpaceSeparatedClassName2() {
        BeanMetaData<MultiWordWidget> metaData = new BeanMetaData<MultiWordWidget>(MultiWordWidget.class);
        assertEquals("Multi Word Widget", metaData.getDisplayName());
    }

    @Test public void displayNameOnBeanWithNoAnnotationBut_resources_entryMatchesTheEntry() {
        BeanMetaData<Apple> metaData = new BeanMetaData<Apple>(Apple.class);
        assertEquals("Golden Delicious", metaData.getDisplayName());
    }

    @Test public void displayNameOnBeanWithNoAnnotationBut_OrangeResources_entryMatchesTheEntry() {
        BeanMetaData<Orange> metaData = new BeanMetaData<Orange>(Orange.class);
        assertEquals("Florida Orange", metaData.getDisplayName());
    }

    @Test public void displayNameOnBeanWithNoAnnotationBut_bothResources_entryMatchesThePearResourcesEntry() {
        BeanMetaData<Pear> metaData = new BeanMetaData<Pear>(Pear.class);
        assertEquals("Juicy Pear", metaData.getDisplayName());
    }

    @Test public void displayNameOnBeanWithAnnotationIsSameAsAnnotation() {
        BeanMetaData<Widget2> metaData = new BeanMetaData<Widget2>(Widget2.class);
        assertEquals("Fun Widget", metaData.getDisplayName());
    }

    @Test public void displayNameOnBeanWithAnnotationWithLeadingPercentResultsInResourceBundleLookup() {
        BeanMetaData<Radish> metaData = new BeanMetaData<Radish>(Radish.class);
        assertEquals("Raspberry", metaData.getDisplayName());
    }

    @Test public void displayNameOnBeanWithAnnotationWithLeadingPercentResultsInResourceBundleLookup2() {
        BeanMetaData<Carrot> metaData = new BeanMetaData<Carrot>(Carrot.class);
        assertEquals("Fruit", metaData.getDisplayName());
    }

    @Test public void displayNameOnBeanWithAnnotationSetToEmptyStringIsEmptyString() {
        BeanMetaData<Widget3> metaData = new BeanMetaData<Widget3>(Widget3.class);
        assertEquals("", metaData.getDisplayName());
    }

    /**************************************************************************
     *
     *      Tests for the "shortDescription" of a BeanMetaData
     *
     *************************************************************************/

    @Test public void shortDescriptionOnBeanWithNoAnnotationIsEmpty() {
        BeanMetaData<Widget> metaData = new BeanMetaData<Widget>(Widget.class);
        assertEquals("", metaData.getShortDescription());
    }

    @Test public void shortDescriptionOnBeanWithAnnotationIsSameAsAnnotation() {
        BeanMetaData<Widget2> metaData = new BeanMetaData<Widget2>(Widget2.class);
        assertEquals("This will be fun!", metaData.getShortDescription());
    }

    @Test public void shortDescriptionOnBeanWithAnnotationButNoShortDescriptionIsComputed() {
        BeanMetaData<Widget3> metaData = new BeanMetaData<Widget3>(Widget3.class);
        assertEquals("", metaData.getShortDescription());
    }

    /**************************************************************************
     *
     *      Tests for the "category" of a BeanMetaData
     *
     *************************************************************************/

    @Test public void categoryOnBeanWithNoAnnotationIsEmpty() {
        BeanMetaData<Widget> metaData = new BeanMetaData<Widget>(Widget.class);
        assertEquals("", metaData.getCategory());
    }

    @Test public void categoryOnBeanWithAnnotationIsSameAsAnnotation() {
        BeanMetaData<Widget2> metaData = new BeanMetaData<Widget2>(Widget2.class);
        assertEquals("Fun", metaData.getCategory());
    }

    @Test public void categoryOnBeanWithAnnotationIsSameAsAnnotation2() {
        BeanMetaData<Widget3> metaData = new BeanMetaData<Widget3>(Widget3.class);
        assertEquals("Fun", metaData.getCategory());
    }

    /**************************************************************************
     *
     *      Tests for the "image" of a BeanMetaData
     *
     *************************************************************************/

    /**************************************************************************
     *
     *      Tests that all properties, events, and callbacks were found
     *      and categorized correctly
     *
     *************************************************************************/

    @Test public void allPropertiesFound() {
        BeanMetaData<Widget> metaData = new BeanMetaData<Widget>(Widget.class);
        assertNotNull(findProperty(metaData, "name"));
        assertNotNull(findProperty(metaData, "colorized"));
        assertNotNull(findProperty(metaData, "widgetValue"));
        assertNull(findProperty(metaData, "onAction"));
    }

    @Test public void allEventsFound() {
        BeanMetaData<Widget> metaData = new BeanMetaData<Widget>(Widget.class);
        assertNull(findEvent(metaData, "name"));
        assertNull(findEvent(metaData, "colorized"));
        assertNull(findEvent(metaData, "widgetValue"));
        assertNotNull(findEvent(metaData, "onAction"));
    }
}
