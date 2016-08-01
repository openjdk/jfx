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

#ifndef _JFX_CRITICAL_SECTION_H_
#define _JFX_CRITICAL_SECTION_H_

#include <Common/ProductFlags.h>

#if TARGET_OS_WIN32

    #include <windows.h>
    typedef CRITICAL_SECTION  NativeCriticalSection;

#elif (TARGET_OS_LINUX || TARGET_OS_MAC)

    #include <pthread.h>
    typedef pthread_mutex_t  NativeCriticalSection;

#endif

class CJfxCriticalSection
{
public:
    virtual ~CJfxCriticalSection();

    static CJfxCriticalSection*   Create();

    bool    Enter();
    bool    TryEnter();
    void    Exit();

private:
    CJfxCriticalSection();

private:
    NativeCriticalSection      m_CriticalSection;
};

#endif  //_JFX_CRITICAL_SECTION_H_
