/*
 * Copyright (c) 2016, 2024, Oracle and/or its affiliates. All rights reserved.
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
 * @apiNote
 * This permission cannot be used for controlling access to resources anymore
 * as the Security Manager is no longer supported.
 *
 * @see java.security.BasicPermission
 * @see java.security.Permission
 * @see java.security.Permissions
 * @see java.security.PermissionCollection
 *
 * @since 9
 *
 * @deprecated This class was only useful in connection with the
 *       Security Manager, which is no longer supported.
 *       There is no replacement for this class.
 */
@Deprecated(since = "24", forRemoval = true)
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
