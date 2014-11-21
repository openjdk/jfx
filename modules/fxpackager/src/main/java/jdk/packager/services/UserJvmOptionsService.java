/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package jdk.packager.services;

import jdk.packager.services.userjvmoptions.LauncherUserJvmOptions;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Runtime access to the UserJVMOptions.
 * 
 * This class is not typically available in the Java Runtime, you must 
 * explicitly include the 'packager.jar' file from the lib directory
 * of the JDK as part of your application bundle. 
 *
 * @since 8u40
 */
public interface UserJvmOptionsService {

    /**
     * Get the instance of UserJvmOptionService to use.  Which one to use is 
     * configured by the packager and the launcher.  Do not directly 
     * instantiate any instance of this interface, use this method to get
     * an appropriate instance.
     * 
     * @return the instance of UserJvmOptionsService for your application.
     */
    static UserJvmOptionsService getUserJVMDefaults() {
        ServiceLoader<UserJvmOptionsService> loader = ServiceLoader.load(UserJvmOptionsService.class);
        Iterator<UserJvmOptionsService> iter = loader.iterator();
        if (iter.hasNext()) {
            return iter.next();
        } else {
            return new LauncherUserJvmOptions();
            //return new PreferencesUserJvmOptions();
        }
    }

    /**
     * The "current" set of UserJVMOptions.
     *
     * This will take effect on the next application start, and this may not
     * reflect the current set of UserJVMOptions used to start this application.
     *
     * @return A map of the keys and values.  Alterations to this map will not
     *         change the stored UserJVMOptions
     */
    Map<String, String> getUserJVMOptions();

    /**
     * Sets the passed in options as the UserJVMOptions.
     *
     * If the application has specified default values and those keys are not
     * in this map, they will be replaced by the default values.
     *
     * No validation or error checking is performed on these values.  It is
     * entirely possible that you may provide a set of UserJVMOptions that
     * may prevent the normal startup of your application and may require
     * manual intervention to resolve.
     *
     * @param options The UserJVMOptions to set.
     */
    void setUserJVMOptions(Map<String, String> options);

    /**
     * The "default" set of UserJVMOptions.
     *
     * This returns the default set of keys and values that the application has
     * been configured to use.
     *
     * @return the keys and values of the default UserJVMOptions.
     * @throws UnsupportedOperationException if the defaults cannot be calculated.
     */
    Map<String, String> getUserJVMOptionDefaults();

}
