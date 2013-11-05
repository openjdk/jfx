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

import com.oracle.ipack.blobs.SuperBlob;

public final class RequirementsBlob extends SuperBlob<RequirementBlob> {
    /* what hosts may run us */
    public static final int KSEC_HOST_REQUIREMENT_TYPE = 1;
    /* what guests we may run */
    public static final int KSEC_GUEST_REQUIREMENT_TYPE = 2;
    /* designated requirement */
    public static final int KSEC_DESIGNATED_REQUIREMENT_TYPE = 3;
    /* what libraries we may link against */
    public static final int KSEC_LIBRARY_REQUIREMENT_TYPE = 4;
    /* what plug-ins we may load */
    public static final int KSEC_PLUGIN_REQUIREMENT_TYPE = 5;

    public RequirementsBlob(final int numberOfSubBlobs) {
        super(numberOfSubBlobs);
    }

    @Override
    protected int getMagic() {
        return 0xfade0c01;
    }
}
