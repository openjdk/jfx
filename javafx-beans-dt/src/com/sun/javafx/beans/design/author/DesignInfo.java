/*
 * Copyright (c) 2005, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.beans.design.author;

import com.sun.javafx.beans.design.tool.DesignBean;
import com.sun.javafx.beans.design.tool.DesignBeanListener;
import com.sun.javafx.beans.metadata.PropertyMetaData;
import java.util.List;

/**
 * <P>The DesignInfo interface is another type of BeanInfo interface to provide more live design-
 * time functionality for a JavaBean.  BeanInfo represents static meta-data about a JavaBean, while
 * DesignInfo provides dynamic design-time behavior.</P>
 *
 * <P>To provide a DesignInfo for a JavaBean, a component author must provide an implementation
 * of the DesignInfo interface available at design-time that matches the name of the JavaBean
 * class with "DesignInfo" appended to it.</P>
 *
 * <P>For example, a component author may supply a JavaBean class named 'com.company.Donkey', and
 * may also supply a corresponding 'com.company.DonkeyBeanInfo' (implements BeanInfo) as well as
 * 'com.company.DonkeyDesignInfo' (implements DesignInfo).  Note that these cannot be the same
 * class, as there is no guarantee that the supplied BeanInfo class will be the same instance that
 * is used in the designer - typically, a BeanInfo class is 'deep-copied' into another instance
 * inside of an IDE.</P>
 *
 * <p><b>GLOBAL CONTEXT LISTENERS:</b>  If you wish to provide a global IDE-wide
 * DesignContextListener (to listen to multiple beans across multiple contexts), you can declare
 * the following static method in your DesignInfo class.  When the DesignInfo for your bean is
 * loaded, this static method will be looked for via reflection and called if it exists:</p>
 *
 * <code>
 *    public static DesignContextListener getGlobalDesignContextListener() { ... }
 * </code>
 *
 * <p>If this method is declared in a DesignInfo implementation class, it will be called when the
 * DesignInfo is first loaded by the IDE, and added to a static list of global listeners.  These
 * listeners will be notified of *every* event that happens in the IDE, so please use sparingly!</p>
 *
 * <P><B>IMPLEMENTED BY THE COMPONENT AUTHOR</B> - This interface is designed to be implemented by
 * the component (bean) author.  BasicDesignInfo is supplied for convenience for subclassing.</P>
 *
 * @author Joe Nuxoll
 * @author Tor Norbye
 * @version 1.0
 * @todo Add additional feature descriptors. How about quick tips and fixes?
 *   How about error annotations and smart tags
 */
public interface DesignInfo<T> extends DesignBeanListener<T> {

    /**
     * Returns the class type of the JavaBean that this DesignInfo was designed to work with
     *
     * @return The JavaBean's class type object
     */
    public Class<T> getBeanClass();

    /**
     * <p>Returns <code>true</code> if this child component (passed as 'childBean' and/or
     * 'childClass') can be added as a child to the specified parent component (passed as
     * 'parentBean').  This allows a component author to dynamically inspect the component hierarchy
     * to determine if a particular component may be inserted.</p>
     *
     * <p>This method is called on the DesignInfo representing the childBean component any time a
     * new component is being created, or dragged around in the visual designer.</p>
     *
     * <p>Note that the 'childBean' argument may be null if this operation is happening as a result
     * of a fresh component drop from the palette.  In that case, the child component instance will
     * not be created until the actual drop happens, thus these checks must be done with only the
     * child component's Class.</p>
     *
     * @param parentBean The DesignBean representing the potential parent component to receive the
     *        child
     * @param childBean The DesignBean representing the potential child component that is being
     *        created or reparented.  This argument may be null if this represents an initial drag
     *        from the palette, where the child bean has not been instantiated yet.
     * @param childClass The Class object representing the potential child component that is being
     *        created or reparented.
     * @return <code>true</code> if this parent bean is suitable for this child bean, or
     *         <code>false</code> if not
     */
    public boolean acceptParent(DesignBean parentBean, DesignBean<T> childBean, Class<T> childClass);

    /**
     * <p>Returns <code>true</code> if this child component (passed as 'childBean' and/or
     * 'childClass') can be added as a child to the specified parent component (passed as
     * 'parentBean').  This allows a component author to dynamically inspect the component hierarchy
     * to determine if a particular component may be inserted.</p>
     *
     * <p>This method is called on the DesignInfo representing the parentBean component any time a
     * new component is being created or dragged around in the visual designer.</p>
     *
     * <p>Note that the 'childBean' argument may be null if this operation is happening as a result
     * of a fresh component drop from the palette.  In that case, the child component instance will
     * not be created until the actual drop happens, thus these checks must be done with only the
     * child component's Class.</p>
     *
     * @param parentBean The DesignBean representing the potential parent component to receive the
     *        child
     * @param childBean The DesignBean representing the potential child component that is being
     *        created or reparented.  This argument may be null if this represents an initial drag
     *        from the palette, where the child bean has not been instantiated yet.
     * @param childClass The Class object representing the potential child component that is being
     *        created or reparented.
     * @return <code>true</code> if this child bean is suitable for this parent bean, or
     *         <code>false</code> if not
     */
    public boolean acceptChild(DesignBean<T> parentBean, DesignBean childBean, Class childClass);

    /**
     * Provides an opportunity for a DesignInfo to setup the initial state of a newly created
     * bean.  Anything can be done here, including property settings, event hooks, and even the
     * creation of other ancillary beans within the context.  Note that this method is only called
     * once after the component has been first created from the palette.
     *
     * @param designBean The bean that was just created
     * @return A Result object, indicating success or failure and including messages for the user
     */
    public Result beanCreatedSetup(DesignBean<T> designBean);

    /**
     * Provides an opportunity for a DesignInfo to fix-up the state of a pasted bean. Anything can
     * be done here, including property settings, event hooks, and even the creation of other
     * ancillary beans within the context.
     *
     * @param designBean The bean that was just pasted from the clipboard
     * @return A Result object, indicating success or failure and including messages for the user
     */
    public Result beanPastedSetup(DesignBean<T> designBean);

    /**
     * Provides an opportunity for a DesignInfo to cleanup just before a bean gets deleted.
     * Anything can be done here, including property settings, event hooks, and even the
     * creation/deletion of other ancillary beans within the context.  Note, however, that this
     * DesignBean will be deleted immediately upon the return of this method.  This is intended for
     * cleanup of ancillary items created in 'beanCreatedSetup'.
     *
     * @param designBean The bean that is about to be deleted
     * @return A Result object, indicating success or failure and including messages for the user
     */
    public Result beanDeletedCleanup(DesignBean<T> designBean);

    /**
     * Returns the list (or hierarchy) of items to be included in a right-click context menu for
     * this bean at design-time.
     *
     * @param designBean The DesignBean that a user has right-clicked on
     * @return An array of DisplayAction objects representing a context menu to display to the user,
     *  or {@link DisplayAction#EMPTY_ARRAY} if there are no items
     */
    public DisplayAction[] getContextItems(DesignBean<T> designBean);

    /**
     * This method is called when an object from a design surface or palette is being dragged 'over'
     * a JavaBean type handled by this DesignInfo.  If the 'sourceBean' or 'sourceClass' is of
     * interest to the 'targetBean' instance or vice-versa (they can be "linked"), this method
     * should return <code>true</code>.  The user will then be presented with visual cues that this
     * is an appropriate place to 'drop' the item and establish a link.  If the user decides to drop
     * the item on this targetBean, the 'linkBeans' method will be called.  Note that the
     * 'sourceBean' argument may be null if this drag operation is originating from the palette,
     * because an instance of the bean will not have been created yet.
     *
     * @param targetBean The DesignBean instance that the user is 'hovering' the mouse over
     * @param sourceBean The DesignBean instance that the user may potentially 'drop' to link - may
     *        be null if this drag operation originated from the palette, because the instance will
     *        not have been created yet
     * @param sourceClass The class type of the object that the user may potentially 'drop' to link
     * @return <code>true</code> if the 'targetBean' cares to have the 'sourceBean' or an instance
     *         of type 'sourceClass' linked to it, <code>false</code> if not
     * @see #linkBeans(DesignBean, DesignBean)
     */
    public boolean acceptLink(DesignBean<T> targetBean, DesignBean sourceBean, Class sourceClass);

    /**
     * <P>This method is called when an object from a design surface or palette is being dropped or
     * has been dropped 'on' a JavaBean type handled by this DesignInfo (to establish a link). This
     * method will not be called unless the corresponding 'acceptLink' method call returned
     * <code>true</code> for at least one of the beans involved.  Typically, this results in new
     * property settings on potentially both of the DesignBean objects.</P>
     *
     * @param targetBean The target DesignBean instance that the user has 'dropped' an object onto
     *        to establish a link
     * @param sourceBean The DesignBean instance that has been 'dropped'
     * @return A Result object, indicating success or failure and including messages for the user
     * @see #acceptLink(DesignBean, DesignBean, Class)
     */
    public Result linkBeans(DesignBean<T> targetBean, DesignBean sourceBean);

    /**
     * <P> This method is called when something is being dragged over a JavaBean handled by this DesignInfo.
     * This method should indicate whether the component is interested in accepting the drop. If it returns
     * true, and the user commits the drag &amp; drop operation, then the {@link #handleDnd} method
     * will be called.</P>
     * <P>
     * The objects handled by this method will not be DesignBeans from this or any other instance of
     * the IDE. Those operations will be handled by the normal {@link #acceptLink} method. This method
     * is intended for all other drag operations, typically from outside of the IDE. Common examples will
     * be dragging images or text from the system desktop and dropping them on components. As an
     * example, a Label component should look for String Transferables and to handle the drop, set the
     * text property to the dropped text. An Image component should look for an Image filename to be dropped
     * and set its image source to the given filename. Components could also check for java.util.List
     * when appropriate to handle sets of items being dropped.
     * </P>
     *
     * @param targetBean The DesignBean instance that the user is 'hovering' the mouse over
     * @param transferInfo Vital information about the drop, such as the type of drop, drop location, the
     *     Transferable, etc.
     * @return <code>true</code> if the 'targetBean' is willing to accept the drop
     * @see #handleDnd(DesignBean, TransferHandler.TransferSupport)
     */
//    public boolean acceptDnd(DesignBean<T> targetBean, TransferHandler.TransferSupport transferInfo);


    /**
     * <P>This method is called when a system drag &amp; drop operation is performed on top of
     * a JavaBean type handled by this DesignInfo.  This method will not be called unless the corresponding
     * {@link #acceptDnd} method call returned <code>true</code>.  Typically, this results in new
     * property settings on the DesignBean.</P>
     * <P>
     * @param targetBean The DesignBean instance that the user is 'hovering' the mouse over
     * @param transferInfo Vital information about the drop, such as the type of drop, drop location, the
     *     Transferable, etc.
     * @return A Result object, indicating success or failure and including messages for the user
     * @see #acceptDnd(DesignBean, TransferHandler.TransferSupport)
     */
//    public Result handleDnd(DesignBean<T> targetBean, TransferHandler.TransferSupport transferInfo);

    // TODO Shouldn't this have been part of the interface? How does an
    // implementation of DesignBean know whether it should return isContainer
    // or not? Doesn't that require specific knowledge of the component? For
    // example, although strict containership is well known in the JavaFX
    // API (Stage has a Scene, Scene has a Root, Parent has children), this
    // only tells us the actual hierarchy. But it doesn't tell us anything
    // about the virtual or "logical" hierarchy, which in the case of
    // Control omits the Skin and adds Tooltip and ContextMenu, and in the case
    // of some arbitrary component might be any old arbitrary thing. How does
    // a component author encode these rules such that the tool can ask in
    // some generic manner "please help me construct design beans based on your
    // logical structure"?
    public List<PropertyMetaData> getChildrenProperties();
}
