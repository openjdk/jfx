/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene;

import com.sun.javafx.beans.design.author.DisplayAction;
import com.sun.javafx.beans.design.author.Result;
import com.sun.javafx.beans.design.tool.DesignBean;
import com.sun.javafx.beans.design.tool.DesignEvent;
import com.sun.javafx.beans.design.tool.DesignProperty;
import com.sun.javafx.beans.metadata.PropertyMetaData;
import java.util.Arrays;
import java.util.List;
import com.sun.javafx.beans.design.author.AbstractDesignInfo;

/**
 *
 * @author Richard
 */
public class ParentDesignInfo extends AbstractDesignInfo {

    public ParentDesignInfo() {
        super(Parent.class);
    }

    protected ParentDesignInfo(Class<? extends Parent> type) {
        super(type);
    }

    @Override
    public boolean acceptParent(DesignBean parentBean, DesignBean childBean, Class childClass) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean acceptChild(DesignBean parentBean, DesignBean childBean, Class childClass) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result beanCreatedSetup(DesignBean designBean) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result beanPastedSetup(DesignBean designBean) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result beanDeletedCleanup(DesignBean designBean) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DisplayAction[] getContextItems(DesignBean designBean) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean acceptLink(DesignBean targetBean, DesignBean sourceBean, Class sourceClass) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result linkBeans(DesignBean targetBean, DesignBean sourceBean) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override public List<PropertyMetaData> getChildrenProperties() {
        return Arrays.asList(getMetaData().findProperty("childrenUnmodifiable"));
    }

    @Override
    public void beanContextActivated(DesignBean designBean) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void beanContextDeactivated(DesignBean designBean) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void instanceNameChanged(DesignBean designBean, String oldInstanceName) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void beanChanged(DesignBean designBean) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void propertyChanged(DesignProperty prop, Object oldValue) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void eventChanged(DesignEvent event) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
