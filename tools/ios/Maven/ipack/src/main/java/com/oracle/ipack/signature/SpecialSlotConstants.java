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

package com.oracle.ipack.signature;

public final class SpecialSlotConstants {
    /* Info.plist */
    public static final int CD_INFO_SLOT = 1;
    /* internal requirements */
	public static final int CD_REQUIREMENTS_SLOT = 2;
    /* resource directory */
	public static final int CD_RESOURCE_DIR_SLOT = 3;
    /* Application specific slot */
	public static final int CD_APPLICATION_SLOT = 4;
    /* embedded entitlement configuration */
    public static final int CD_ENTITLEMENT_SLOT = 5;
    /* CodeDirectory */
	public static final int CD_CODE_DIRECTORY_SLOT = 0;
    /* CMS signature */
	public static final int CD_SIGNATURE_SLOT = 0x10000;

    private SpecialSlotConstants() {
    }
}
