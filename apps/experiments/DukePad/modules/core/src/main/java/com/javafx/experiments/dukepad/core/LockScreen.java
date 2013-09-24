/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.javafx.experiments.dukepad.core;

import javafx.beans.property.SimpleBooleanProperty;

/**
 * Interface for lock screens. Lock screens are special kinds of applications that provide extra functionality for
 * locking and unlocking the screen. The first registered application service that implements this LockScreen interface
 * is used by the core framework.
 */
public abstract class LockScreen extends BaseDukeApplication {

    /**
     * Lock or unlock screen
     *
     * @param locked true if screen should be locked
     */
    public abstract void setLocked(boolean locked);

    /**
     * Is the screen locked
     *
     * @return true if screen is locked
     */
    public abstract boolean isLocked();

    /**
     * Locked property for listening/changing lock state
     *
     * @return lock property
     */
    public abstract SimpleBooleanProperty lockedProperty();
}
