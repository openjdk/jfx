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
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import javafx.util.Pair;

/**
 * <p>A DesignProject is a top-level container for DesignContexts at design-time.  The DesignProject
 * represents the project in the IDE.  Not much can be done with Projects in the Design-Time API for
 * JavaBeans, except for accessing DesignContexts and resources, listening to project-level events,
 * and storing project-level and global data.  Check <code>instancoef</code> to see if this
 * DesignProject class is from a specific IDE to access more extensive "project" functionality.</p>
 *
 * <P><B>IMPLEMENTED BY THE IDE</B> - This interface is implemented by the IDE for use by the
 * component (bean) author.</P>
 *
 * @author Joe Nuxoll
 * @version 1.0
 * @see DesignContext#getProject()
 */
public interface DesignProject extends DisplayItem {

    //------------------------------------------------------------------------ DesignContext Methods

    /**
     * Returns all the DesignContexts in this project.  There will be one DesignContext for each
     * designable file in the project.  Note that for JSF, this means one DesignContext for each
     * combination of "PageX.java" and "PageX.jsp" file, as well as one for each of the non-page
     * beans, like "SessionBeanX.java", "ApplicationBeanX.java", etc.
     *
     * @return An array of DesignContext objects - one for each designable file in the project
     */
    public DesignContext[] getDesignContexts();

    /**
     * Creates a new DesignContext (backing file) in this project.
     *
     * @param className The desired fully-qualified class name for the file
     * @param baseClass The desired base class for the file
     * @param contextData A Map of context data to apply to the newly created context file
     * @return The newly created DesignContext, or null if the operation was unsuccessful
     */
    public DesignContext createDesignContext(String className, Class baseClass, Map contextData);

    /**
     * Removes an existing DesignContext (backing file) from this project.
     *
     * @param context The desired DesignContext to remove from the project
     * @return <code>true</code> if the operation was successful, <code>false</code> if not
     */
    public boolean removeDesignContext(DesignContext context);

    //----------------------------------------------------------------------------- Resource Methods

    /**
     * <p>Returns the set of project root relative resources in this project as an array of local
     * resource identifiers.  The returned URIs will always be paths from the project root,
     * including folder hierarchy within the project.  The specified <code>rootPath</code> is used
     * as a filter, to allow drilling-in to directories as desired.  Use
     * <code>URI.relativize()</code> to make relative URIs when needed.  Use
     * <code>getResourceFile(URI)</code> to retrieve a File object for a particular resource in
     * the project.</p>
     *
     * @param rootPath The root path to fetch resources underneath.  Passing <code>null</code> will
     *        start at the root of the project.
     * @param recurseFolders <code>true</code> to include the sub-resources inside of any folders
     * @return A project root relative array of URIs representing all the resource files under the
     *         specified root path
     */
    public URI[] getResources(URI rootPath, boolean recurseFolders);

    /**
     * Returns a File object containing the specified resource.
     *
     * @param resourceUri The desired project relative resource URI to fetch a file object
     * @return A File object containing the project resource
     */
    public File getResourceFile(URI resourceUri);

    /**
     * Copies a resource into this project, and converts the external URL into a local URI
     * (resource identifier string).
     *
     * @param sourceUrl A URL pointing to the desired external resource
     * @param targetUri The desired resource URI (path) within the project directory
     * @return The resulting project relative resource URI (resourceUri)
     * @throws IOException if the resource cannot be copied
     */
    public URI addResource(URL sourceUrl, URI targetUri) throws IOException;

    /**
     * Removes a resource from the project directory.
     *
     * @param resourceUri The desired resource to remove from the project
     * @return boolean <code>true</code> if the resource was successfully removed,
     *         <code>false</code> if not
     */
    public boolean removeResource(URI resourceUri);

    //------------------------------------------------------------------------- Project Data Methods

    /**
     * <p>Sets a name-value pair of data on this DesignContext.  This name-value pair will be stored
     * in the associated project file, so this data is retrievable in a future IDE session.</p>
     *
     * <p>NOTE: The 'data' Object must be a Java Bean, such as a String, or a List (containing
     *  only JavaBeans, etc.). The IDE may rely on Java Beans persistence
     * to store the data in the project.</p>
     *
     * @param key The key to store the data object under
     * @param data The data object to store - this must be a JavaBean, such as a String, or a Collection
     *    of Colors
     * @see #getProjectData(Key)
     */
    public <K,V> void setProjectData(Pair<K,V> pair);

    /**
     * <p>Retrieves the value for a name-value pair of data on this DesignProject.  This name-value
     * pair is stored in the project file, so this data is retrievable in any IDE session once it
     * has been set.</p>
     *
     * <p>NOTE: The 'data' Object must be a Java Bean, such as a String, or a List (containing
     *  only JavaBeans, etc.). The IDE may rely on Java Beans persistence
     * to store the data in the project.</p>
     *
     * @param key The desired key to retrieve the data object for
     * @return The data object that is currently stored under this key - this must be a JavaBean,
     *   such as a String, or a Collection of Colors
     * @see #setProjectData(Key, Object)
     */
    public <K,V> V getProjectData(K key);

    //-------------------------------------------------------------------------- Global Data Methods


    /**
     * <p>Sets a global name-value pair of data.  This name-value pair will be stored in the
     * associated user settings file (as text), so this data is retrievable in a future IDE
     * session.</p>
     *
     * <p>NOTE: The 'data' Object must be a Java Bean, such as a String, or a List (containing
     *  only JavaBeans, etc.). The IDE may rely on Java Beans persistence
     * to store the data in the project.</p>
     *
     * @param key The key to store the data object under
     * @param data The data object to store - this must be a JavaBean, such as a String, or a Collection
     *    of Colors
     * @see #getGlobalData(Key)
     */
    public <K,V> void setGlobalData(Pair<K,V> pair);

    /**
     * <p>Retrieves the value for a global name-value pair of data.  This name-value pair will be
     * stored in the associated user settings file (as text), so this data is retrievable in any
     * IDE session once it has been set.</p>
     *
     * <p>NOTE: The 'data' Object must be a Java Bean, such as a String, or a List (containing
     *  only JavaBeans, etc.). The IDE may rely on Java Beans persistence
     * to store the data in the project.</p>
     *
     * @param key The desired key to retrieve the data object for
     * @return The data object that is currently stored under this key - this must be a JavaBean,
     *   such as a String, or a Collection of Colors
     * @see #setGlobalData(Key, Object)
     */
    public <K,V> V getGlobalData(K key);
}
