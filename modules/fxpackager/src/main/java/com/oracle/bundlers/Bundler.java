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

package com.oracle.bundlers;

import com.sun.javafx.tools.packager.bundlers.ConfigException;
import com.sun.javafx.tools.packager.bundlers.UnsupportedPlatformException;

import java.io.File;
import java.util.Collection;
import java.util.Map;

public interface Bundler {


    /**
     * @return User Friendly name of this bundler.
     */
    String getName();

    /**
     * @return A more verbose description of the bundler.
     */
    String getDescription();

    /**
     * @return Command line identifier of the bundler.  Should be unique.
     */
    String getID();

    /**
     * @return The bundle type of the bundle that is created by this bundler.
     */
    String getBundleType();

    /**
     * The parameters that this bundler uses to generate it's bundle.
     * @return immutable collection
     */
    Collection<BundlerParamInfo<?>> getBundleParameters();

    /**
     * Determines if this bundler will execute with the given parameters.
     *
     * @param params The parameters to be validate.  Validation may modify
     *               the map, so if you are going to be using the same map
     *               across multiple bundlers you should pass in a deep copy.
     * @return true if valid
     * @throws UnsupportedPlatformException If the bundler cannot run on this
     *         platform (i.e. creating mac apps on windows)
     * @throws ConfigException If the configuration params are incorrect.  The
     *         exception will contain advice to bring the parameters into compliance.
     */
    boolean validate(Map<String, ? super Object> params) throws UnsupportedPlatformException, ConfigException;

    /**
     * Creates a bundle from existing content.
     * @param params The parameters as specified by getBundleParameters.
     *               Keyed by the id from the ParamInfo.  Validation may
     *               modify the map, so if you are going to be using the
     *               same map across multiple bundlers you should pass
     *               in a deep copy.
     * @param outputParentDir
     *   The parent dir that the returned bundle will be placed in.
     * @return The resulting bundled file
     *
     * For a bundler that produces a single artifact file this will be the
     * location of that artifact (.exe file, .deb file, etc)
     *
     * For a bundler that produces a specific directory format output this will
     * be the location of that specific directory (.app file, etc).
     *
     * For a bundler that produce multiple files, this will be a parent
     * directory of those files (linux, and windows, images), whose name is not
     * relavent to the result.
     *
     * @throws java.lang.IllegalArgumentException for any of the following
     * reasons:
     *  <ul>
     *      <li>A required parameter is not found in the params list, for
     *      example missing the main class.</li>
     *      <li>A parameter has the wrong type of an object, for example a
     *      String where a File is required</li>
     *      <li>Bundler specific incompatibilities with the parameters, for
     *      example a bad version number format or an application id with
     *      forward slashes.</li>
     *      <li>(?) a parameter not listed in getBundleParameters - or do we allow it? (?)</li>
     *  </ul>
     */
    public File execute(Map<String, ? super Object> params, File outputParentDir);

}
