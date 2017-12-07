/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

#pragma once

enum DOMExceptionCode {
    INDEX_SIZE_ERR                = 1,
    DOMSTRING_SIZE_ERR            = 2,
    HIERARCHY_REQUEST_ERR         = 3,
    WRONG_DOCUMENT_ERR            = 4,
    INVALID_CHARACTER_ERR         = 5,
    NO_DATA_ALLOWED_ERR           = 6,
    NO_MODIFICATION_ALLOWED_ERR   = 7,
    NOT_FOUND_ERR                 = 8,
    NOT_SUPPORTED_ERR             = 9,
    INUSE_ATTRIBUTE_ERR           = 10,
    INVALID_STATE_ERR             = 11,
    SYNTAX_ERR                    = 12,
    INVALID_MODIFICATION_ERR      = 13,
    NAMESPACE_ERR                 = 14,
    INVALID_ACCESS_ERR            = 15,
    VALIDATION_ERR                = 16,
    TYPE_MISMATCH_ERR             = 17
};
