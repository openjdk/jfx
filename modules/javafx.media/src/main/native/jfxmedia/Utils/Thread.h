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

#ifndef _JFX_THREAD_H_
#define _JFX_THREAD_H_

#if TARGET_OS_WIN32
    #include <windows.h>

    typedef unsigned (__stdcall *Functor)(void*);
#elif TARGET_OS_MAC
#endif

//*******************************************************************
/// @class CThread
///
/// @brief Thread abstraction class.
//*******************************************************************
class CThread
{
public:
    CThread();
    CThread(Functor pFunction, void *pvData, const char* pStrDebugThreadName);
    ~CThread();

    bool    IsRunning();

    //***** These functions assume this object's life time is the same as the thread
    bool    Run();
    bool    Run(Functor pFunction, void *pvData, const char* pStrDebugThreadName);
    bool    Stop();

    int     WaitForExit(DWORD dwWaitTime=INFINITE);

    HANDLE  GetHandle();

#ifdef _DEBUG
    static void                 SetThreadName(DWORD threadID, const char * name);
#endif

protected:
    static unsigned __stdcall   Executor(void* lpThreadParameter);

protected:
    Functor             m_pFunction;
    void                *m_pvData;

    HANDLE              m_hThread;
    unsigned            m_uThreadID;

    bool                m_bDeleteSelf;

#ifdef _DEBUG
    char                        m_strDebugThreadName[256];
#endif
};

#endif // _JFX_THREAD_H_
