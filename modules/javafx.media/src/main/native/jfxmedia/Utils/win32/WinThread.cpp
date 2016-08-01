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

#include <Utils/Thread.h>

#include <Common/ProductFlags.h>
#include <Common/VSMemory.h>
#include <windows.h>
#include <process.h>

const DWORD     ACTIVE_CHECK_TIMEOUT    = 100;

//*******************************************************************
/// @fn CThread::CThread()
///
/// @brief      Constructor
//*******************************************************************
CThread::CThread()
{
    m_pFunction             = NULL;
    m_pvData                = NULL;
    m_hThread               = NULL;
    m_uThreadID             = 0;
#ifdef _DEBUG
    memset(&m_strDebugThreadName, sizeof(m_strDebugThreadName), 0);
#endif
}

//*******************************************************************
/// @fn CThread::CThread()
///
/// @brief      Constructor
/// @param[in]  pFunction   pointer to the thread function
/// @param[in]  pvData      void pointer to data to pass to function
/// @param[in]  bStopOnDestruction  Determines whether the thread
///                                 should stop when the class instance
///                                 is destroyed
//*******************************************************************
CThread::CThread(Functor pFunction, void *pvData, const char* pStrDebugThreadName)
{
    m_pFunction             = pFunction;
    m_pvData                = pvData;
    m_hThread               = NULL;
    m_uThreadID             = 0;

#ifdef _DEBUG
    #pragma warning( push )
    #pragma warning( disable : 4996 )
    //***** Keep track of thread name so Visual Studio debugger can display it.
    strncpy(m_strDebugThreadName, pStrDebugThreadName, sizeof(m_strDebugThreadName)-1);
    #pragma warning( pop )
    m_strDebugThreadName[sizeof(m_strDebugThreadName) - 1] = 0;
#endif
}

//*******************************************************************
/// @fn CThread::~CThread()
///
/// @brief      Destructor
//*******************************************************************
CThread::~CThread()
{
    if (NULL != m_hThread)
        CloseHandle(m_hThread);
}

//*******************************************************************
/// @fn CThread::IsRunning()
///
/// @brief      Checks if thread is still active.
/// @return     true if successful, false otherwise
//*******************************************************************
bool CThread::IsRunning()
{
    DWORD   dwResult;

    if (NULL == m_hThread)
        return false;

    dwResult = WaitForSingleObject(m_hThread, ACTIVE_CHECK_TIMEOUT);

    return (WAIT_TIMEOUT == dwResult);
}

//*******************************************************************
/// @fn CThread::Run()
///
/// @brief      Starts up the thread to execute the functor.
/// @return     true if successful, false otherwise
//*******************************************************************
bool CThread::Run()
{
    //***** Pre-condition - thread not already used and functor non-null
    if ((NULL != m_hThread) || (NULL == m_pFunction))
        return false;

    m_hThread = (HANDLE) _beginthreadex(NULL, 0, Executor, this, 0, &m_uThreadID);

    return (NULL != m_hThread);
}

//*******************************************************************
/// @fn CThread::Run()
///
/// @brief      Starts up the thread to execute the functor.
/// @param[in]  pFunction   pointer to the thread function
/// @param[in]  pvData      void pointer to data to pass to function
/// @param[in]  bStopOnDestruction  Determines whether the thread
///                                 should stop when the class instance
///                                 is destroyed
/// @return     true if successful, false otherwise
//*******************************************************************
bool CThread::Run(Functor pFunction, void *pvData, const char* pStrDebugThreadName)
{
    //***** Pre-condition - thread not already used and functor non-null
    if ((NULL != m_hThread) || (NULL == pFunction))
        return false;

    m_pFunction = pFunction;
    m_pvData    = pvData;

#ifdef _DEBUG
    size_t  sizeSrc;
    size_t  sizeDest;
    size_t  sizeFinal;

    sizeDest    = sizeof(m_strDebugThreadName) - 1;
    sizeSrc     = strlen(pStrDebugThreadName);
    sizeFinal   = min(sizeDest, sizeSrc);

    //***** Set thread name.
    #pragma warning( push )
    #pragma warning( disable : 4996 )
    strncpy(m_strDebugThreadName, pStrDebugThreadName, sizeFinal);
    #pragma warning( pop )
    m_strDebugThreadName[sizeFinal] = NULL;
#endif

    m_hThread = (HANDLE) _beginthreadex(NULL, 0, Executor, this, 0, &m_uThreadID);

    return (NULL != m_hThread);
}

//*******************************************************************
/// @fn CThread::Stop()
///
/// @brief      Force a running thread to stop.  Do not use!
/// @return     true if successful, false otherwise
//*******************************************************************
bool CThread::Stop()
{
    bool    bSuccess;

    if (NULL == m_hThread)
        return true;

    bSuccess = (TRUE == TerminateThread(m_hThread, 1));

    if (bSuccess)
    {
        CloseHandle(m_hThread);
        m_hThread   = NULL;
        m_uThreadID = 0;
    }

    return bSuccess;
}

//*******************************************************************
/// @fn CThread::WaitForExit()
///
/// @brief      Waits for a thread to stop.  Waits indefinitely.
/// @param      Time to wait for exit in milliseconds.
/// @return     Thread exit code.  If thread already stop, 0 is returned.
//*******************************************************************
int CThread::WaitForExit(DWORD dwWaitTime)
{
    DWORD   dwRetCode;
    DWORD   dwResult;

    if (NULL == m_hThread)
        return 0;

    dwResult = WaitForSingleObject(m_hThread, dwWaitTime);

    if (WAIT_ABANDONED == dwResult)
        dwRetCode = -1;
    else
    {
        if (FALSE == GetExitCodeThread(m_hThread, &dwResult))
            dwResult = -1;
    }

    return static_cast<int>(dwResult);
}

//*******************************************************************
/// @fn CThread::GetHandle()
///
/// @brief      Returns the thread handle
/// @return     Returns the thread handle
//*******************************************************************
HANDLE CThread::GetHandle()
{
    return m_hThread;
}

//*******************************************************************
/// @fn CThread::Executor(void* lpThreadParameter)
///
/// @brief      Wrapper around the real entry point for CThread.
/// @return     Thread result.
//*******************************************************************
unsigned __stdcall CThread::Executor(void* lpThreadParameter)
{
    unsigned    result;
    CThread*    pThread;

    pThread = (CThread*)lpThreadParameter;
    if (NULL == pThread)
        return 0;

#ifdef _DEBUG
//  Assert(NULL != pThread->m_strDebugThreadName);

    // Set thread name for Visual Studio debugger.
    SetThreadName(-1, pThread->m_strDebugThreadName);
#endif

//  Assert(NULL != pThread->m_pFunction);
    result = pThread->m_pFunction(pThread->m_pvData);

    CloseHandle(pThread->m_hThread);
    pThread->m_hThread = NULL;

    return result;
}

//*******************************************************************
/// @fn CThread::SetThreadName(UInt32 threadID, const char * name)
///
/// @brief      Sets the thread name for the debugger
//*******************************************************************

#ifdef _DEBUG
struct THREADNAME_INFO
{
    DWORD dwType;       // must be 0x1000
    LPCSTR szName;      // pointer to name (in user addr space)
    DWORD dwThreadID;   // thread ID (-1=caller thread)
    DWORD dwFlags;      // reserved for future use, must be zero
};

void CThread::SetThreadName(DWORD threadID, const char * name)
{
    THREADNAME_INFO info;

    info.dwType = 0x1000;
    info.szName = name;
    info.dwThreadID = threadID;
    info.dwFlags = 0;

    __try
    {
        RaiseException( 0x406D1388, 0, sizeof(info)/sizeof(DWORD), (ULONG_PTR*)&info );
    }
    __except (EXCEPTION_CONTINUE_EXECUTION)
    {
    }

}
#endif  // _DEBUG
