/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.util;

import java.security.BasicPermission;

/**
 * This class is for JavaFX permissions.
 * An {@code FXPermission} contains a target name but
 * no actions list; you either have the named permission
 * or you don't.
 *
 * <p>
 * The target name is the name of the JavaFX permission (see below). The naming
 * convention follows the hierarchical property naming convention.
 * Also, an asterisk can be used to represent all JavaFX permissions.
 * </p>
 *
 * <p>
 * The following table lists all the possible {@code FXPermission}
 * target names, and for each provides a description of what the permission
 * allows and a discussion of the risks of granting code the permission.
 * </p>
 *
 * <table border=1>
 * <caption>FXPermission Table</caption>
 * <tr>
 * <th style="padding: 5px;" scope="col">Permission Target Name</th>
 * <th style="padding: 5px;" scope="col">What the Permission Allows</th>
 * <th style="padding: 5px;" scope="col">Risks of Allowing this Permission</th>
 * </tr>
 *
 * <tr>
 *   <th style="padding: 5px;" scope="row">accessClipboard</th>
 *   <td style="padding: 5px;">Posting and retrieval of information to and from the system clipboard</td>
 *   <td style="padding: 5px;">This would allow a malicious application to share or read
 *       potentially sensitive or confidential information.</td>
 * </tr>
 *
 * <tr>
 *   <th style="padding: 5px;" scope="row">accessWindowList</th>
 *   <td style="padding: 5px;">Accessing the list of all JavaFX Windows</td>
 *   <td style="padding: 5px;">Providing access to the complete list of all JavaFX windows could give
 *       a malicious application the ability to modify a security dialog.</td>
 * </tr>
 *
 * <tr>
 *   <th style="padding: 5px;" scope="row">createRobot</th>
 *   <td style="padding: 5px;">Creating JavaFX Robot objects</td>
 *   <td style="padding: 5px;">The JavaFX Robot object allows code to generate native-level
 *       mouse and keyboard events as well as read the screen. It could allow
 *       malicious code to control the system, run other programs, read the
 *       display, and deny mouse and keyboard access to the user.</td>
 * </tr>
 *
 * <tr>
 *   <th style="padding: 5px;" scope="row">createTransparentWindow</th>
 *   <td style="padding: 5px;">Creating transparent windows</td>
 *   <td style="padding: 5px;">Transparent windows are not limited to a rectangular region that
 *       obscures what is underneath the window.
 *       This can make it difficult to distinguish parts of the window
 *       from other application windows or the platform desktop, and can be used
 *       to allow a malicious application to trick a user into entering
 *       sensitive data, especially in conjunction with either
 *       {@code setWindowAlwaysOnTop} or {@code unrestrictedFullScreen}.</td>
 * </tr>
 *
 * <tr>
 *   <th style="padding: 5px;" scope="row">loadFont</th>
 *   <td style="padding: 5px;">Loading a custom font, either via the
 *       {@link javafx.scene.text.Font#loadFont} method or a jar file containing
 *       embedded fonts listed in the jar manifest</td>
 *   <td style="padding: 5px;">Loading a custom font might allow a malicious application to provide a
 *       malformed font. Such a font could crash the application, allowing the
 *       malicious application to take control of the system, if there are any
 *       bugs in the underlying platform font implementation.</td>
 * </tr>
 *
 * <tr>
 *   <th style="padding: 5px;" scope="row">modifyFXMLClassLoader</th>
 *   <td style="padding: 5px;">Setting the ClassLoader used to load FXML objects, and removing the
 *       restriction against loading system classes</td>
 *   <td style="padding: 5px;">Allowing an application to set an arbitrary ClassLoader might enable
 *       a malicious application to load classes with elevated permissions. Also
 *       removing the restriction against loading system classes with a null
 *       ClassLoader, might allow the application access to classes they would
 *       otherwise be denied from accessing.</td>
 * </tr>
 *
 * <tr>
 *   <th style="padding: 5px;" scope="row">setWindowAlwaysOnTop</th>
 *   <td style="padding: 5px;">Setting the always-on-top property of a window:
 *       {@link javafx.stage.Stage#setAlwaysOnTop}</td>
 *   <td style="padding: 5px;">The malicious window might make itself look and behave like the
 *       platform desktop, so that information entered by the unsuspecting user
 *       is captured and subsequently misused,
 *       especially in conjunction with the {@code createTransparentWindow}
 *       permission.</td>
 * </tr>
 *
 * <tr>
 *   <th style="padding: 5px;" scope="row">unrestrictedFullScreen</th>
 *   <td style="padding: 5px;">Allow unrestricted full-screen access, including keyboard events
 *       and warning banner</td>
 *   <td style="padding: 5px;">This permission allows an application to enter full-screen mode at any
 *       time, override the warning banner, and disable the function of
 *       the ESC key to exit from full-screen mode.
 *       All keyboard input will be delivered to the application while in
 *       full-screen mode, rather than being limited to KEY_PRESSED and
 *       KEY_RELEASED events for a subset of keys. See
 *       {@link javafx.stage.Stage#setFullScreen}.
 *       This could allow a malicious window to
 *       look and behave like the platform desktop, so that information entered
 *       by the unsuspecting user is captured and subsequently misused,
 *       especially in conjunction with the {@code createTransparentWindow}
 *       permission.</td>
 * </tr>
 *
 * </table>
 *
 * @see java.security.BasicPermission
 * @see java.security.Permission
 * @see java.security.Permissions
 * @see java.security.PermissionCollection
 * @see java.lang.SecurityManager
 *
 * @since 9
 */
public final class FXPermission extends BasicPermission {

    private static final long serialVersionUID = 2890556410764946054L;

    /**
     * Creates a new {@code FXPermission} with the specified name.
     * The name is the symbolic name of the {@code FXPermission},
     * such as "accessClipboard", "createTransparentWindow ", etc. An asterisk
     * may be used to indicate all JavaFX permissions.
     *
     * @param name the name of the FXPermission
     *
     * @throws NullPointerException if {@code name} is {@code null}.
     * @throws IllegalArgumentException if {@code name} is empty.
     */
    public FXPermission(String name) {
        super(name);
    }

}
