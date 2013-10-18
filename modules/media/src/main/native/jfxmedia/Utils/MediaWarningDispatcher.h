/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _MEDIA_WARNING_DISPATCHER_H
#define _MEDIA_WARNING_DISPATCHER_H

/**
 * class CMediaWarningDispatcher
 *
 * A convenience class which forwards errors and warnings to the
 * CMediaErrorListener set on the unique instance of CMediaManager.
 * This class obviates the need to have direct references to
 * CMediaManager in code which needs to emits errors or warnings.
 */
class CMediaWarningDispatcher
{
public:
    static void Warning(int warningCode, const char* warningMessage);

private:
    CMediaWarningDispatcher();
    ~CMediaWarningDispatcher();
};

#endif // _MEDIA_WARNING_DISPATCHER_H
