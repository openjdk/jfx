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
package com.sun.javafx.beans.design.tool;

import com.sun.javafx.beans.design.DisplayItem;
import java.io.IOException;
import java.net.URL;
import javafx.util.Pair;

/**
 * <p>A DesignContext is a 'host' for DesignBean instances at design-time.  The DesignContext
 * represents the 'source file' or 'persistence model' for a design-time session.  A DesignContext
 * is the container (instance host) for a set of DesignBeans.  For example, in a JSF application,
 * the DesignContext represents the logical backing file which is the combination of the 'Page1.jsp'
 * and the 'Page1.java' files.  In a Swing application, the DesignContext represents the
 * 'JFrame1.java' file.</p>
 *
 * <P><B>IMPLEMENTED BY THE IDE</B> - This interface is implemented by the IDE for use by the
 * component (bean) author.</P>
 *
 * @author Joe Nuxoll
 * @version 1.0
 */
public interface DesignContext extends DisplayItem {

    //-------------------------------------------------------------------- DesignBean Access Methods

    /**
     * Returns the root container DesignBean for this DesignContext.  This is typically the "this"
     * component being designed.  For example, this would be the view root in a JSF application.
     * The children of the root container are the items you see on the page.  To get all of the
     * DesignBeans within the scope of this context (ignoring the containership hierarchy), use
     * the getBeans() method.
     *
     * @return The root container DesignBean for this DesignContext
     * @see #getBeans()
     */
    public DesignBean getRootContainer();

    /**
     * Returns a DesignBean (design-time proxy) to represent the specified JavaBean instance.  This
     * must be an instance that lives within the scope of this DesignContext, or the method will
     * return null.
     *
     * @param beanInstance A live instance of a JavaBean
     * @return A DesignBean (design-time proxy) representing the specified bean instance, or null if
     *         the specified Object does not represent a JavaBean within the scope of this
     *         DesignContext
     */
    public DesignBean getBeanForInstance(Object beanInstance);

    /**
     * Returns a DesignBean (design-time proxy) to represent the JavaBean with the specified
     * instance name.  This must be an instance that lives within the scope of this DesignContext,
     * or the method will return null.
     *
     * @param instanceName The String instance name of the desired JavaBean
     * @return A DesignBean (design-time proxy) representing the specified bean, or null if the
     *         specified instance name does not represent a JavaBean within the scope of this
     *         DesignContext
     */
    public DesignBean getBeanByName(String instanceName);

    /**
     * Returns a DesignBean array (design-time proxies) representing the JavaBeans within the scope
     * of this DesignContext that are assignable from the specified class type.  This uses
     * Class.isAssignableFrom(...) to determine if a JavaBean satisfies the specified criteria, so
     * subtypes of the specified type will be included.
     *
     * @param beanClass The desired class type
     * @return An array of DesignBean representing the JavaBeans within the scope of this
     *         DesignContext that are assignable from the specified class type
     * @see Class#isAssignableFrom(Class)
     */
    public <T> DesignBean<T>[] getBeansOfType(Class<T> beanClass);

    /**
     * Returns an array of all the DesignBeans within the scope of this DesignContext.  This is a 
     * flat list of instances, ignoring the containership hierarchy.  To navigate the containership
     * hierarchy, use the getRootContainer() method.
     *
     * @return An array of DesignBean representing the JavaBeans within the scope of this 
     *         DesignContext
     * @see #getRootContainer()
     */
    public DesignBean[] getBeans();

    /**
     * Returns the Class corresponding to the Java bean of the given fully qualified class
     * name.
     *
     * @param      className the fully qualified name of the desired class.
     * @return     the <code>Class</code> object for the class with the
     *             specified name.
     * @exception LinkageError if the linkage fails
     * @exception ExceptionInInitializerError if the initialization provoked
     *            by this method fails
     * @exception ClassNotFoundException if the class cannot be located
     */
    public Class findBeanClass(String className) throws ClassNotFoundException;

    //-------------------------------------------------------------- DesignBean Manipulation Methods

    /**
     * Returns <code>true</code> if the specified type (beanClass) of JavaBean can be created as a 
     * child of the specified parent DesignBean at the specified position.  This is a test call that 
     * should be performed before calling the createBean method.
     *
     * @param beanClass The class of the JavaBean to be created
     * @param parent The DesignBean parent for the JavaBean to be created
     * @param position The desired position for the JavaBean to be created
     * @return <code>true</code> if a matching call to 'createBean' would succeed, or 
     *         <code>false</code> if not
     * @see DesignContext#createBean(Class, DesignBean, Position)
     */
    public boolean canCreateBean(Class beanClass, DesignBean parent, Position position);

    /**
     * Creates an instance of a JavaBean of the specified type, as a child of the
     * specified parent DesignBean at the specified position.  If successful, a DesignBean
     * representing the newly created bean is returned.  Before this method is called, a test call
     * should be performed to the canCreateBean method. The IDE will call
     * {@link DesignBeanListener#beanChanged} on the parent bean if creating the bean succeeds.
     *
     * @param beanClass The class of the JavaBean to be created
     * @param parent The DesignBean parent for the JavaBean to be created
     * @param position The desired position for the JavaBean to be created
     * @return A DesignBean representing the JavaBean that was created, or null if the operation
     *         failed
     * @see DesignContext#canCreateBean(Class, DesignBean, Position)
     */
    public <T> DesignBean<T> createBean(Class<T> beanClass, DesignBean parent, Position position);

    /**
     * Returns <code>true</code> if the specified DesignBean can be can be moved to be a child of 
     * the specified parent DesignBean at the specified position.  This is a test call that should 
     * be performed before calling the moveBean method.
     *
     * @param designBean The DesignBean to be moved
     * @param newParent The new DesignBean parent for the DesignBean
     * @param position The desired position for the DesignBean to be moved
     * @return <code>true</code> if a matching call to 'moveBean' would succeed, or 
     *         <code>false</code> if not
     * @see #moveBean(DesignBean, DesignBean, Position)
     */
    public boolean canMoveBean(DesignBean designBean, DesignBean newParent, Position position);

    /**
     * Moves a DesignBean, to become a child of the specified parent DesignBean at the specified
     * position.  Returns <code>true</code> if successful, <code>false</code> if not.  Before this 
     * method is called, a test call should be performed to the canMoveBean method.
     * The IDE will call {@link DesignBeanListener#beanChanged} on both the old and new parent
     * beans if the move succeeds.
     *
     * @param designBean The DesignBean to move
     * @param newParent The new DesignBean parent for the DesignBean
     * @param position The desired position for the DesignBean to be moved
     * @return <code>true</code> if move was successful, or <code>false</code> if not
     * @see #canMoveBean(DesignBean, DesignBean, Position)
     */
    public boolean moveBean(DesignBean designBean, DesignBean newParent, Position position);

    /**
     * Returns <code>true</code> if the specified DesignBean can be can be replaced with
     * another. This is a test call that should be performed before calling the replaceBean
     * method.
     *
     * @param designBean The DesignBean to be replaced
     * @param newDesignBean The DesignBean that will replace the original DesignBean
     * @return <code>true</code> if a matching call to 'replaceBean' would succeed, or 
     *         <code>false</code> if not
     * @see #replaceBean(DesignBean, DesignBean)
     */
    public boolean canReplaceBean(DesignBean designBean, DesignBean newDesignBean);

    /**
     * Replaces a given DesignBean with another. The replaced DesignBean will be deleted.
     * The new DesignBean will be moved from its current position to the position of the
     * original design bean.
     * Returns <code>true</code> if successful, <code>false</code> if not.  Before this 
     * method is called, a test call should be performed to the canReplaceBean method.
     * (And canReplaceBean will call canMoveBean on the new bean to ensure that the
     * move is valid.)
     * The IDE will call {@link DesignBeanListener#beanChanged} on the parent if the 
     * replacement succeeds.
     *
     * @param designBean The DesignBean to be replaced
     * @param newDesignBean The DesignBean that will replace the original DesignBean
     * @return <code>true</code> if replacement was successful, or <code>false</code> if not
     * @see #canReplaceBean(DesignBean, DesignBean)
     */
    public boolean replaceBean(DesignBean designBean, DesignBean newDesignBean);

    /**
     * Copies a set of DesignBean instances into a clipboard-like format.  This returns a
     * Transferable object that stores all the necessary data for the pasteBeans method.
     *
     * @param designBeans An array of desired DesignBean instances
     * @return the resulting Transferable object representing the copied beans
     * @see #pasteBeans(java.awt.datatransfer.Transferable, DesignBean, Position)
     */
//    public Transferable copyBeans(DesignBean[] designBeans);

    /**
     * Pastes a set of DesignBean instances (acquired via copyBeans) into the specified parent
     * DesignBean at the specified position.  This returns an array of DesignBean(s), representing
     * the newly pasted children.
     * The IDE will call {@link DesignBeanListener#beanChanged} on the parent bean after the beans
     * have been pasted.
     *
     * @param persistData The Transferable object acquired via 'copyBeans' that contains the data
     *        representing the DesignBean(s) to be pasted
     * @param newParent The desired new parent DesignBean to paste the DesignBean(s) into
     * @param position The desired new position for the pasted DesignBean(s)
     * @return The newly created DesignBean instances
     * @see #copyBeans(DesignBean[])
     */
//    public DesignBean[] pasteBeans(Transferable persistData, DesignBean newParent, Position position);

    /**
     * Deletes a DesignBean object (and removes all persistence).  Returns <code>true</code> if the 
     * delete was successful, <code>false</code> if not.
     * The IDE will call {@link DesignBeanListener#beanChanged} on the parent if the deletion succeeded.
     *
     * @param designBean The desired DesignBean to delete
     * @return <code>true</code> if the delete operation was successful, <code>false</code> if not
     */
    public boolean deleteBean(DesignBean designBean);

    //------------------------------------------------------------------------- Context Data Methods

    /**
     * <p>Sets a name-value pair of data on this DesignContext.  This name-value pair will be stored
     * in the associated project file (as text) that contains this DesignContext, so this data is
     * retrievable in a future IDE session.</p>
     *
     * <p>NOTE: The 'data' Object must be a Java Bean, such as a String, or a List (containing
     *  only JavaBeans, etc.). The IDE may rely on Java Beans persistence
     * to store the data in the project.</p>
     *
     * @param key The key to store the data object under
     * @param data The data object to store - this must be a JavaBean, such as a String, or a Collection
     *    of Colors
     * @see #getContextData(Key)
     */
    public <K,V> void setContextData(Pair<K,V> pair);

    /**
     * <p>Retrieves the value for a name-value pair of data on this DesignContext.  This name-value
     * pair is stored in the associated project file (as text) that contains this DesignContext, so
     * this data is retrievable in any IDE session once it has been set.</p>
     *
     * <p>NOTE: The 'data' Object must be a Java Bean, such as a String, or a List (containing
     *  only JavaBeans, etc.). The IDE may rely on Java Beans persistence
     * to store the data in the project.</p>
     *
     * @param key The desired key to retrieve the data object for
     * @return The data object that is currently stored under this key - this must be a JavaBean, 
     *   such as a String, or a Collection of Colors
     * @see #setContextData(Key, Object)
     * @see Constants.ContextData
     */
    public <K,V> V getContextData(K key);

    //----------------------------------------------------------------------------- Resource Methods

    /**
     * Adds a resource reference to this DesignContext, and converts the external URL into a local
     * resource identifier String.  This may also copy (if specified) an external resource into the
     * project.
     *
     * @param resource A URL pointing to the desired external resource
     * @param copy <code>true</code> if the resource should be copied into the project, 
     *        <code>false</code> if not
     * @throws IOException if the resource cannot be copied
     * @return The resulting relative resource identifier String.  This will be a local relative
     *         resource if the external resource was copied into the project.
     */
    public String addResource(URL resource, boolean copy) throws IOException;

    /**
     * Resolves a local resource identifier String into a fully-qualified URL.
     *
     * @param localResource A local resource identifier string
     * @return A fully-qualified URL
     */
    public URL resolveResource(String localResource);

    //----------------------------------------------------------------------- Context Method Methods

    /**
     * Returns a set of {@link ContextMember} objects describing the fields and methods declared on this
     * DesignContext (source file).
     *
     * @return An array of {@link ContextMember} objects, describing the fields and methods declared on this
     * DesignContext (source file)
     */
    public ContextMember[] getContextMembers();

    /**
     * Returns a {@link ContextMethod} object describing the method with the specified name and
     * parameter types.  Returns <code>null</code> if no method exists on this DesignContext with
     * the specified name and parameter types.
     *
     * @param methodName The method name of the desired context method
     * @param parameterTypes The parameter types of the desired context method
     * @return A ContextMethod object describing the requested method, or <code>null</code> if no
     *         method exists with the specified name and parameter types
     */
    public ContextMethod getContextMethod(String methodName, Class[] parameterTypes);

    /**
     * Returns a {@link ContextField} object describing the field with the specified name.
     * Returns <code>null</code> if no method exists on this DesignContext with the specified name.
     *
     * @param fieldName The field name of the desired context method
     * @return A ContextField object describing the requested field, or <code>null</code> if no
     *         field exists with the specified name
     */
    public ContextField getContextField(String fieldName);
    
    /**
     * <p>Creates a new method or field in the source code for this DesignContext.  The passed 
     * ContextMember <strong>must</strong> specify at least the designContext and name, and <strong>must
     * not</strong> describe a method or field that already exists in the DesignContext source.  To update
     * an existing member, use the <code>updateContextMember()</code> method.  These methods are
     * separated to help prevent accidental method overwriting.  The following table
     * details how the specified ContextMember is used for this method:</p>
     *
     * <p><table border="1">
     * <tr><th>designContext <td><strong>REQUIRED.</strong> Must match the DesignContext that is
     *         being called.  This is essentially a safety precaution to help prevent accidental
     *         method overwriting.
     * <tr><th>name <td><strong>REQUIRED.</strong> Defines the method name.
     * <tr><th>modifiers <td>Defines the method modifiers.  Use {@link java.lang.reflect.Modifier}
     *         to define the modifier bits.  If <code>0</code> is specified (no modifier bits), then
     *         a public method is created.
     * <tr><th>returnType <td>(ContextMethods only) Defines the return type.  If <code>null</code> is specified, the
     *         created method will have a <code>void</code> return type.
     * <tr><th>type <td>(ContextFields only) Defines the field type.  Should never be <code>null</code>.
     * <tr><th>parameterTypes <td>(ContextMethods only) Defines the parameter types.  If <code>null</code> or an empty
     *         array is specified, the created method will have no arguments.
     * <tr><th>parameterNames <td>(ContextMethods only) Defines the parameter names.  If <code>null</code> or an empty
     *         array is specified (or an array shorter than the parameterTypes array), default
     *         argument names will be used.
     * <tr><th>exceptionTypes <td>(ContextMethods only) Defines the throws clause exception types.  If <code>null</code>
     *         is specified, the created method will have no throws clause.
     * <tr><th>methodBodyText <td>(ContextMethods only) Defines the method body Java source code.  If <code>null</code> is
     *         specified, the method will have an empty body.  If the value is non-null, this must
     *         represent valid (compilable) Java source code.
     * <tr><th>commentText <td>Defines the comment text above the newly created method.  If
     *         <code>null</code> is specified, no comment text will be included.
     * </table></p>
     *
     * @param method A ContextMember object representing the desired method.
     * @return <code>true</code> if the member was created successfully, or <code>false</code> if
     *         it was not.
     * @throws IllegalArgumentException If there was a syntax error in any of the ContextMember
     *         settings, or if the ContextMember represents a member that already exists on this
     *         DesignContext (<code>updateContextMember()</code> must be used in this case to avoid
     *         accidental member overwriting)
     * 
     * @see ContextMethod
     * @see ContextField
     */
    public boolean createContextMember(ContextMember method) throws IllegalArgumentException;

    /**
     * <p>Updates an existing member in the source code for this DesignContext.  The passed
     * ContextMember will be used to locate the desired member to update using the designContext,
     * name, and (if applicable) parameterTypes.  This method may only be used to update the modifiers, 
     * type or returnType, parameterNames, exceptionTypes, methodBodyText, or commentText.  Any other changes
     * actually constitute the creation of a new method, as they alter the method signature.  To
     * create a new method, the <code>createContextMember()</code> method should be used.  These
     * operations are separated to help prevent accidental member overwriting.  The following table
     * details how the specified ContextMember is used for this method:</p>
     *
     * <p><table border="1">
     * <tr><th>designContext <td><strong>REQUIRED.</strong> Must match the DesignContext that is
     *         being called.  This is essentially a safety precaution to help prevent accidental
     *         method overwriting.
     * <tr><th>name <td><strong>REQUIRED.</strong> Specifies the desired method name.
     * <tr><th>modifiers <td>Defines the method modifiers.  Use {@link java.lang.reflect.Modifier}
     *         to define the modifier bits.
     * <tr><th>returnType <td>(ContextMethods only) Defines the method's return type.  If <code>null</code> is specified,
     *         the method is assumed to have a <code>void</code> return type.
     * <tr><th>type <td>(ContextFields only) Defines the field type.  Should never be <code>null</code>.
     * <tr><th>parameterTypes <td>(ContextMethods only) <strong>REQUIRED.</strong> Specifies the desired method's
     *         parameter types (if it has any).  If <code>null</code> or an empty array is
     *         specified, the desired method is assumed to have zero arguments.
     * <tr><th>parameterNames <td>(ContextMethods only) Defines the parameter names.  If <code>null</code> or an empty
     *         array is specified (or an array shorter than the parameterTypes array), default
     *         argument names will be used.
     * <tr><th>exceptionTypes <td>(ContextMethods only) Defines the throws clause exception types.  If <code>null</code>
     *         is specified, the resulting method will have no throws clause.
     * <tr><th>methodBodyText <td>(ContextMethods only) Defines the method body Java source code.  If <code>null</code> is
     *         specified, the resulting method body will be empty.  If the value is non-null, this
     *         must represent valid (compilable) Java source code.  Note that a method with a
     *         non-void return type <strong>must</strong> return a value.
     * <tr><th>commentText <td>Defines the comment text above the newly created method.  If
     *         <code>null</code> is specified, no comment text will be included.
     * </table></p>
     *
     * @todo Clean this documentation up. The table should not be necessary. The ContextMembers
     *   should do this.
     * 
     * @param method The desired ContextMember representing the method to be updated
     * @return The resulting ContextMember object (including any updates from the process)
     * @throws IllegalArgumentException If there was a syntax error in any of the ContextMember
     *         settings, or if the ContextMember does not exist in this DesignContext.
     */
    public ContextMember updateContextMember(ContextMember method) throws IllegalArgumentException;

    /**
     * <p>Removes an existing member from the source code for this DesignContext.  The passed
     * ContextMember will be used to locate the desired member to remove using the designContext,
     * name, and (if applicable) parameterTypes.  No other portions of the ContextMember are used.  The
     * following table details how the specified ContextMember is used:</p>
     *
     * <p><table border="1">
     * <tr><th>designContext <td><strong>REQUIRED.</strong> Must match the DesignContext that is
     *         being called.  This is essentially a safety precaution to help prevent accidental
     *         method removal.
     * <tr><th>name <td><strong>REQUIRED.</strong> Specifies the desired method name.
     * <tr><tr>modifiers <id>Ignored.
     * <tr><th>type/returnType <td>Ignored.
     * <tr><th>parameterTypes <td>(ContextMethods only) <strong>REQUIRED.</strong> Specifies the desired method's
     *         parameter types (if it has any).  If <code>null</code> or an empty array is
     *         specified, the desired method is assumed to have zero arguments.
     * <tr><th>parameterNames <td>Ignored.
     * <tr><th>exceptionTypes <td>Ignored.
     * <tr><th>methodBodyText <td>Ignored.
     * <tr><th>commentText <td>Ignored.
     * </table></p>
     *
     * @param method A ContextMember object defining the field or method to be removed
     * @return <code>true</code> if the method was successfully removed
     * @exception IllegalArgumentException if the specified ContextMember does not exist on this
     *            DesignContext
     */
    public boolean removeContextMember(ContextMember method);

    //------------------------------------------------------------------------ Project Access Method

    /**
     * Returns the project, which is the top-level container for all contexts.
     *
     * @return The DesignProject associated with this DesignContext
     */
    public DesignProject getProject();

    //------------------------------------------------------------------- Event Registration Methods

    /**
     * Adds a listener to this DesignContext
     *
     * @param listener The desired listener to add
     */
    public void addDesignContextListener(DesignContextListener listener);

    /**
     * Removes a listener from this DesignContext
     *
     * @param listener The desired listener to remove
     */
    public void removeDesignContextListener(DesignContextListener listener);

    /**
     * Returns the array of current listeners to this DesignContext
     *
     * @return An array of listeners currently listening to this DesignContext
     */
    public DesignContextListener[] getDesignContextListeners();
}
