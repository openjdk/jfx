/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

/*
 * WinLauncherSvc.cpp : Defines the entry points for the service application.
 *
 * http://msdn.microsoft.com/en-us/library/windows/desktop/bb540475(v=vs.85).aspx
 */

#include <windows.h>
#include <tchar.h>
#include <strsafe.h>
#include <shellapi.h>
#include <stdlib.h>

#pragma comment(lib, "advapi32.lib")

SERVICE_STATUS          gSvcStatus; 
SERVICE_STATUS_HANDLE   gSvcStatusHandle; 
HANDLE                  ghSvcStopEvent = NULL;

VOID SvcInstall(TCHAR *svcName, TCHAR *svcDesc, TCHAR *mainExe,
                BOOL startOnInstall, BOOL runAtStartup);
VOID SvcStartOnInstall(SC_HANDLE schService);

VOID SvcUninstall(TCHAR *svcName, BOOL stopOnUninstall);
VOID SvcStopOnUninstall(SC_HANDLE schSCManager, SC_HANDLE schService);
BOOL StopDependentServices(SC_HANDLE schSCManager, SC_HANDLE schService);

VOID SvcStartup(TCHAR *mainExe);

VOID WINAPI SvcMain(DWORD argc, LPTSTR *argv);
VOID WINAPI SvcCtrlHandler(DWORD); 

VOID ReportSvcStatus(DWORD, DWORD, DWORD);
VOID SvcInit(DWORD, LPTSTR *); 
VOID SvcReportEvent(LPTSTR);

HANDLE CreateMainProcess();

#define SVC_ERROR                        ((DWORD)0xC0020001L)
#define SVCNAME  _T("")

static bool isDebug = FALSE;

/*
 * Set env variable JAVAFX_LAUNCHER_DEBUG to enable debugging output
 */
static void enableDebugIfNeeded() {
    TCHAR* buffer = NULL;
    size_t bufferLen = 0;

    //Check if env variable is set
    _tdupenv_s(&buffer, &bufferLen, _T("JAVAFX_LAUNCHER_DEBUG"));
    if (buffer != NULL) { //env variable set
        isDebug = true;
        free(buffer);
    }
}

static void debug(LPCTSTR pszFormat, ...) {
    TCHAR szOutput[MAX_PATH];
    size_t cchDest = MAX_PATH;
    va_list ap;

    if (isDebug) {
        va_start(ap, cchDest);
        StringCchPrintf(szOutput, cchDest, pszFormat);
        va_end(ap);

        OutputDebugString(szOutput);
    }
}

/*
 * internal parameters coming from the bundler
 */
static TCHAR *gMainExe = NULL; // relative path to the main executable

/*
 * Entry point for the executable
 *
 * Usages:
 *
 *   - install new service in the SCM database
 *
 *     WinLauncherSrv.exe -install -svcName Name -svcDesc Description -mainExe WinLauncher.exe
 *                        [-startOnInstall] [-runAtStartup]
 *
 *   - uninstall service from the SCM database
 *
 *     WinLauncherSrv.exe -uninstall -svcName Name
 *                        [-stopOnUninstall]
 *
 *   - the service will be started by the SCM
 *
 *     WinLauncherSrv.exe -mainExe WinLauncher.exe
 */
int APIENTRY _tWinMain(HINSTANCE hInstance,
                       HINSTANCE hPrevInstance,
                       LPTSTR    lpCmdLine,
                       int       nCmdShow)
{
    LPWSTR *szArgList;
    int argCount;

    BOOL isInstall = FALSE;
    BOOL isUninstall = FALSE;
    BOOL startOnInstall = FALSE;
    BOOL stopOnUninstall = FALSE;
    BOOL runAtStartup = FALSE;

    TCHAR *mainExe = NULL;
    TCHAR *svcName = NULL;
    TCHAR *svcDesc = NULL;

    enableDebugIfNeeded();

    // Parse command line arguments
    szArgList = CommandLineToArgvW(GetCommandLine(), &argCount);

    int i;
    for (i = 1; i < argCount; i++) {
        if (lstrcmpi(szArgList[i], TEXT("-install")) == 0) {
            isInstall = TRUE;
        } else if (lstrcmpi(szArgList[i], TEXT("-uninstall")) == 0) {
            isUninstall = TRUE;
        } else if (lstrcmpi(szArgList[i], TEXT("-mainExe")) == 0) {
            mainExe = szArgList[++i];
        } else if (lstrcmpi(szArgList[i], TEXT("-svcName")) == 0) {
            svcName = szArgList[++i];
        } else if (lstrcmpi(szArgList[i], TEXT("-svcDesc")) == 0) {
            svcDesc = szArgList[++i];
        } else if (lstrcmpi(szArgList[i], TEXT("-startOnInstall")) == 0) {
            startOnInstall = TRUE;
        } else if (lstrcmpi(szArgList[i], TEXT("-stopOnUninstall")) == 0) {
            stopOnUninstall = TRUE;
        } else if (lstrcmpi(szArgList[i], TEXT("-runAtStartup")) == 0) {
            runAtStartup = TRUE;
        } else {
            // unrecognized option
        }
    }

    if (isInstall) {
        SvcInstall(svcName, svcDesc, mainExe, startOnInstall, runAtStartup);
    } else if (isUninstall) {
        SvcUninstall(svcName, stopOnUninstall);
    } else {
        SvcStartup(mainExe);
    }
    LocalFree(szArgList);

    return 1;
}

/*
 * Installs a service in the SCM database
 */
VOID SvcInstall(TCHAR *svcName, TCHAR *svcDesc, TCHAR *mainExe,
                BOOL startOnInstall, BOOL runAtStartup)
{
    SC_HANDLE schSCManager;
    SC_HANDLE schService;
    TCHAR szModuleName[MAX_PATH];
    TCHAR szPath[MAX_PATH]; // also includes arguments for the service
    SERVICE_DESCRIPTION sd = {0};
    DWORD dwStartType;

    if (svcName == NULL) {
        debug(TEXT("SvcInstall failed - svcName cannot be NULL"));
        return;
    }

    if (mainExe == NULL) {
        debug(TEXT("SvcInstall failed - mainExe cannot be NULL"));
        return;
    }

    if(!GetModuleFileName(NULL, szModuleName, MAX_PATH )) {
        debug(TEXT("Cannot install service (%d)"), GetLastError());
        return;
    }

    // append the service arguments to the service executable
    StringCchPrintf(szPath, MAX_PATH, _T("%s -mainExe \"%s\""), szModuleName, mainExe);

    // Get a handle to the SCM database. 
    schSCManager = OpenSCManager( 
        NULL,                    // local computer
        NULL,                    // ServicesActive database 
        SC_MANAGER_ALL_ACCESS);  // full access rights 
 
    if (NULL == schSCManager) {
        debug(TEXT("OpenSCManager failed (%d)"), GetLastError());
        return;
    }

    if (runAtStartup) {
        dwStartType = SERVICE_AUTO_START;
    } else {
        dwStartType = SERVICE_DEMAND_START;
    }

    // Create the service
    schService = CreateService( 
        schSCManager,              // SCM database 
        svcName,                   // name of service 
        svcName,                   // service name to display 
        SERVICE_ALL_ACCESS,        // desired access 
        SERVICE_WIN32_OWN_PROCESS, // service type 
        dwStartType,               // start type 
        SERVICE_ERROR_NORMAL,      // error control type 
        szPath,                    // path to service's binary 
        NULL,                      // no load ordering group 
        NULL,                      // no tag identifier 
        NULL,                      // no dependencies 
        NULL,                      // LocalSystem account 
        NULL);                     // no password 
 
    if (schService == NULL) {
        debug(TEXT("CreateService failed (%d)"), GetLastError());
        CloseServiceHandle(schSCManager);
        return;
    }

    debug(TEXT("Service installed successfully"));

    // Change the service description
    if (svcDesc != NULL) {
        sd.lpDescription = svcDesc;

        if(!ChangeServiceConfig2(
            schService,                 // handle to service
            SERVICE_CONFIG_DESCRIPTION, // change: description
            &sd))                       // new description
        {
            debug(TEXT("ChangeServiceConfig2 failed"));
        }
        else
        {
            debug(TEXT("Service description updated successfully"));
        }
    }

    // Start the service
    if (startOnInstall) {
        SvcStartOnInstall(schService);
    }

    CloseServiceHandle(schService); 
    CloseServiceHandle(schSCManager);
}

/*
 * Attempts to start the service.
 */
VOID SvcStartOnInstall(SC_HANDLE schService)
{
    SERVICE_STATUS_PROCESS ssStatus;
    DWORD dwOldCheckPoint; 
    DWORD dwStartTickCount;
    DWORD dwWaitTime;
    DWORD dwBytesNeeded;

    if (!StartService(
                      schService,  // handle to service 
                      0,           // number of arguments
                      NULL) )      // no arguments 
    {
        debug(TEXT("StartService failed (%d)"), GetLastError());
        return; 
    }
    else
    {
        debug(TEXT("Service start pending..."));
    }

    // Check the status until the service is no longer start pending.
    if (!QueryServiceStatusEx(
                              schService,                     // handle to service 
                              SC_STATUS_PROCESS_INFO,         // info level
                              (LPBYTE) &ssStatus,             // address of structure
                              sizeof(SERVICE_STATUS_PROCESS), // size of structure
                              &dwBytesNeeded ) )              // if buffer too small
    {
        debug(TEXT("QueryServiceStatusEx failed (%d)"), GetLastError());
        return; 
    }
 
    // Save the tick count and initial checkpoint.
    dwStartTickCount = GetTickCount();
    dwOldCheckPoint = ssStatus.dwCheckPoint;

    while (ssStatus.dwCurrentState == SERVICE_START_PENDING) 
    { 
        // Do not wait longer than the wait hint. A good interval is 
        // one-tenth the wait hint, but no less than 1 second and no 
        // more than 10 seconds. 
        dwWaitTime = ssStatus.dwWaitHint / 10;

        if(dwWaitTime < 1000) {
            dwWaitTime = 1000;
        } else if (dwWaitTime > 10000) {
            dwWaitTime = 10000;
        }

        Sleep(dwWaitTime);

        // Check the status again. 
        if (!QueryServiceStatusEx( 
                                  schService,                     // handle to service 
                                  SC_STATUS_PROCESS_INFO,         // info level
                                  (LPBYTE) &ssStatus,             // address of structure
                                  sizeof(SERVICE_STATUS_PROCESS), // size of structure
                                  &dwBytesNeeded ) )              // if buffer too small
        {
            debug(TEXT("QueryServiceStatusEx failed (%d)"), GetLastError());
            break;
        }
 
        if (ssStatus.dwCheckPoint > dwOldCheckPoint)
        {
            // Continue to wait and check.
            dwStartTickCount = GetTickCount();
            dwOldCheckPoint = ssStatus.dwCheckPoint;
        } else {
            if(GetTickCount() - dwStartTickCount > ssStatus.dwWaitHint) {
                // No progress made within the wait hint.
                break;
            }
        }
    }

    // Determine whether the service is running.
    if (ssStatus.dwCurrentState == SERVICE_RUNNING) {
        debug(TEXT("Service started successfully."));
    } else {
        debug(TEXT("Service not started."));
        debug(TEXT("Current State: %d"), ssStatus.dwCurrentState);
        debug(TEXT("Exit Code: %d"), ssStatus.dwWin32ExitCode);
        debug(TEXT("Check Point: %d"), ssStatus.dwCheckPoint);
        debug(TEXT("Wait Hint: %d"), ssStatus.dwWaitHint);
    }
}

/*
 * Deletes a service from the SCM database
 */
VOID SvcUninstall(TCHAR *svcName, BOOL stopOnUninstall)
{
    SC_HANDLE schSCManager;
    SC_HANDLE schService;

    if (svcName == NULL) {
        debug(TEXT("SvcUninstall failed - svcName cannot be NULL"));
        return;
    }

    // Get a handle to the SCM database. 
    schSCManager = OpenSCManager( 
        NULL,                    // local computer
        NULL,                    // ServicesActive database 
        SC_MANAGER_ALL_ACCESS);  // full access rights 
 
    if (schSCManager == NULL) {
        debug(TEXT("OpenSCManager failed (%d)"), GetLastError());
        return;
    }

    // Get a handle to the service.
    schService = OpenService( 
        schSCManager,                                      // SCM database 
        svcName,                                           // name of service 
        DELETE | SERVICE_STOP | SERVICE_QUERY_STATUS);     // need stop/delete access
 
    if (schService == NULL) {
        debug(TEXT("OpenService failed (%d)"), GetLastError());
        CloseServiceHandle(schSCManager);
        return;
    }

    // Stop the service
    if (stopOnUninstall) {
        SvcStopOnUninstall(schSCManager, schService);
    }

    // Delete the service.
    if (!DeleteService(schService)) {
        debug(TEXT("DeleteService failed (%d)"), GetLastError());
    } else {
        debug(TEXT("Service deleted successfully"));
    }
 
    CloseServiceHandle(schService); 
    CloseServiceHandle(schSCManager);
}

/*
 * Stops the service
 */
VOID SvcStopOnUninstall(SC_HANDLE schSCManager, SC_HANDLE schService)
{
    SERVICE_STATUS_PROCESS ssp;
    DWORD dwStartTime = GetTickCount();
    DWORD dwBytesNeeded;
    DWORD dwTimeout = 30000; // 30-second time-out
    DWORD dwWaitTime;

    // Make sure the service is not already stopped.
    if ( !QueryServiceStatusEx(
                               schService,
                               SC_STATUS_PROCESS_INFO,
                               (LPBYTE)&ssp,
                               sizeof(SERVICE_STATUS_PROCESS),
                               &dwBytesNeeded))
    {
        debug(TEXT("QueryServiceStatusEx failed (%d)"), GetLastError());
        return;
    }

    if (ssp.dwCurrentState == SERVICE_STOPPED) {
        debug(TEXT("Service is already stopped."));
        return;
    }

    // If a stop is pending, wait for it.
    while (ssp.dwCurrentState == SERVICE_STOP_PENDING)
    {
        debug(TEXT("Service stop pending..."));

        // Do not wait longer than the wait hint. A good interval is 
        // one-tenth of the wait hint but not less than 1 second  
        // and not more than 10 seconds.
        dwWaitTime = ssp.dwWaitHint / 10;

        if(dwWaitTime < 1000) {
            dwWaitTime = 1000;
        } else if (dwWaitTime > 10000) {
            dwWaitTime = 10000;
        }

        Sleep(dwWaitTime);

        if (!QueryServiceStatusEx( 
                                  schService, 
                                  SC_STATUS_PROCESS_INFO,
                                  (LPBYTE)&ssp, 
                                  sizeof(SERVICE_STATUS_PROCESS),
                                  &dwBytesNeeded ) )
        {
            debug(TEXT("QueryServiceStatusEx failed (%d)"), GetLastError());
            return;
        }

        if (ssp.dwCurrentState == SERVICE_STOPPED) {
            debug(TEXT("Service stopped successfully."));
            return;
        }

        if (GetTickCount() - dwStartTime > dwTimeout) {
            debug(TEXT("Service stop timed out."));
            return;
        }
    }

    // If the service is running, dependencies must be stopped first.
    StopDependentServices(schSCManager, schService);

    // Send a stop code to the service.
    if (!ControlService(
                        schService,
                        SERVICE_CONTROL_STOP,
                        (LPSERVICE_STATUS)&ssp))
    {
        debug(TEXT("ControlService failed (%d)"), GetLastError());
        return;
    }

    // Wait for the service to stop.
    while (ssp.dwCurrentState != SERVICE_STOPPED)
    {
        Sleep( ssp.dwWaitHint );
        if (!QueryServiceStatusEx(
                                  schService,
                                  SC_STATUS_PROCESS_INFO,
                                  (LPBYTE)&ssp,
                                  sizeof(SERVICE_STATUS_PROCESS),
                                  &dwBytesNeeded))
        {
            debug(TEXT("QueryServiceStatusEx failed (%d)"), GetLastError());
            return;
        }

        if (ssp.dwCurrentState == SERVICE_STOPPED) {
            break;
        }

        if (GetTickCount() - dwStartTime > dwTimeout) {
            debug(TEXT("Wait timed out."));
            return;
        }
    }

    debug(TEXT("Service stopped successfully"));
}

BOOL StopDependentServices(SC_HANDLE schSCManager, SC_HANDLE schService) {
    DWORD i;
    DWORD dwBytesNeeded;
    DWORD dwCount;

    LPENUM_SERVICE_STATUS   lpDependencies = NULL;
    ENUM_SERVICE_STATUS     ess;
    SC_HANDLE               hDepService;
    SERVICE_STATUS_PROCESS  ssp;

    DWORD dwStartTime = GetTickCount();
    DWORD dwTimeout = 30000; // 30-second time-out

    // Pass a zero-length buffer to get the required buffer size.
    if (EnumDependentServices(schService, SERVICE_ACTIVE,
                              lpDependencies, 0, &dwBytesNeeded, &dwCount))
    {
        // If the Enum call succeeds, then there are no dependent
        // services, so do nothing.
        return TRUE;
    } 
    else 
    {
        if (GetLastError() != ERROR_MORE_DATA) {
            return FALSE; // Unexpected error
        }

        // Allocate a buffer for the dependencies.
        lpDependencies = (LPENUM_SERVICE_STATUS) HeapAlloc(
                      GetProcessHeap(), HEAP_ZERO_MEMORY, dwBytesNeeded);
  
        if (!lpDependencies) {
            return FALSE;
        }

        __try {
            // Enumerate the dependencies.
            if (!EnumDependentServices(schService, SERVICE_ACTIVE,
                                       lpDependencies, dwBytesNeeded, &dwBytesNeeded,
                                       &dwCount))
            {
                return FALSE;
            }

            for (i = 0; i < dwCount; i++) {
                ess = *(lpDependencies + i);
                // Open the service.
                hDepService = OpenService(schSCManager, 
                                          ess.lpServiceName, 
                                          SERVICE_STOP | SERVICE_QUERY_STATUS);

                if (!hDepService) {
                    return FALSE;
                }

                __try {
                    // Send a stop code.
                    if (!ControlService(hDepService,
                                        SERVICE_CONTROL_STOP,
                                        (LPSERVICE_STATUS) &ssp)) {
                        return FALSE;
                    }

                    // Wait for the service to stop.
                    while (ssp.dwCurrentState != SERVICE_STOPPED)
                    {
                        Sleep( ssp.dwWaitHint );
                        if (!QueryServiceStatusEx(
                                                  hDepService, 
                                                  SC_STATUS_PROCESS_INFO,
                                                  (LPBYTE)&ssp,
                                                  sizeof(SERVICE_STATUS_PROCESS),
                                                  &dwBytesNeeded))
                        {
                            return FALSE;
                        }

                        if (ssp.dwCurrentState == SERVICE_STOPPED) {
                            break;
                        }

                        if (GetTickCount() - dwStartTime > dwTimeout) {
                            return FALSE;
                        }
                    }
                }
                __finally
                {
                    // Always release the service handle.
                    CloseServiceHandle(hDepService);
                }
            }
        }
        __finally 
        {
            // Always free the enumeration buffer.
            HeapFree( GetProcessHeap(), 0, lpDependencies );
        }
    } 
    return TRUE;
}

/*
 * The service is being started by the SCM.
 */
VOID SvcStartup(TCHAR *mainExe)
{
    if (mainExe == NULL) {
        debug(TEXT("SvcStartup failed - mainExe cannot be NULL"));
        return;
    }

    // store the path to the main executable as global variable
    TCHAR szMainExe[MAX_PATH];
    StringCchPrintf(szMainExe, MAX_PATH, _T("%s"), mainExe);
    gMainExe = szMainExe;

    // If the service is installed with the SERVICE_WIN32_OWN_PROCESS
    // service type, the lpServiceName member of lpServiceTable is ignored.
    // This member cannot be NULL but it can be an empty string (SVCNAME).

    SERVICE_TABLE_ENTRY DispatchTable[] = 
    {
        { SVCNAME, (LPSERVICE_MAIN_FUNCTION) SvcMain },
        { NULL, NULL }
    };

    // This call returns when the service has stopped. 
    // The process should simply terminate when the call returns.
    if (!StartServiceCtrlDispatcher (DispatchTable)) {
        SvcReportEvent(TEXT("StartServiceCtrlDispatcher"));
    }
}

/*
 * Entry point for the service
 *
 * lpszArgv are parameters that come from the "Start Parameters" text field in
 * the properties dialog for the service (the Services snap-in from the Control Panel)
 * Ignore the params.
 */
VOID WINAPI SvcMain(DWORD dwArgc, LPTSTR *lpszArgv)
{
    DWORD Status = E_FAIL;

    enableDebugIfNeeded();

    if (gMainExe == NULL) {
        debug(TEXT("SvcMain failed - gMainExe cannot be NULL"));
        return;
    }

    // Register the handler function for the service
    gSvcStatusHandle = RegisterServiceCtrlHandler (SVCNAME, SvcCtrlHandler);

    if(!gSvcStatusHandle) { 
        SvcReportEvent(TEXT("RegisterServiceCtrlHandler")); 
        return; 
    }

    // These SERVICE_STATUS members remain as set here
    gSvcStatus.dwServiceType = SERVICE_WIN32_OWN_PROCESS; 
    gSvcStatus.dwServiceSpecificExitCode = 0;    

    // Report initial status to the SCM
    ReportSvcStatus(SERVICE_START_PENDING, NO_ERROR, 3000);

    // Perform service-specific initialization and work.
    SvcInit(dwArgc, lpszArgv);
}

/*
 * Performs the service code
 *
 * @param dwArgc - Number of arguments in the lpszArgv array
 * @param lpszArgv - Array of strings. The first string is the name of
 *                the service and subsequent strings are passed by the process
 *                that called the StartService function to start the service.
 */
VOID SvcInit(DWORD dwArgc, LPTSTR *lpszArgv)
{
    // Create an event. The control handler function, SvcCtrlHandler,
    // signals this event when it receives the stop control code.
    ghSvcStopEvent = CreateEvent(
                         NULL,    // default security attributes
                         TRUE,    // manual reset event
                         FALSE,   // not signaled
                         NULL);   // no name

    if (ghSvcStopEvent == NULL) {
        ReportSvcStatus( SERVICE_STOPPED, NO_ERROR, 0 );
        return;
    }

    // Report running status when initialization is complete.
    ReportSvcStatus( SERVICE_RUNNING, NO_ERROR, 0 );

    // Perform work until service stops.
    HANDLE hProcess = CreateMainProcess();

    while(1)
    {
        // Check whether to stop the service.
        WaitForSingleObject(ghSvcStopEvent, INFINITE);

        // REMIND: better way to stop execution of the main process?
        if (hProcess) {
            TerminateProcess(hProcess, NULL);
        }

        ReportSvcStatus( SERVICE_STOPPED, NO_ERROR, 0 );
        return;
    }
}

/*
 * Starts the main process
 *
 * ASSUMPTION - the main and the service executables are in the same directory
 */
HANDLE CreateMainProcess() {
    TCHAR szBaseDir[MAX_PATH] = {0};
    TCHAR szMainExe[MAX_PATH] = {0};

    GetModuleFileName(NULL, szBaseDir, MAX_PATH);
    TCHAR* end = _tcsrchr(szBaseDir, '\\');
    if (end != NULL) {
        *end = 0;
    }

    StringCchPrintf(szMainExe, MAX_PATH, _T("%s\\%s"), szBaseDir, gMainExe);

    STARTUPINFO si = {0};
    si.cb          = sizeof(STARTUPINFO);
    si.dwFlags     = STARTF_USESHOWWINDOW;
    si.wShowWindow = FALSE;

    PROCESS_INFORMATION pi = {0};
    BOOL ret = CreateProcess(
                        szMainExe,
                        szMainExe,
                        NULL,
                        NULL,
                        FALSE,
                        CREATE_NO_WINDOW,
                        NULL,
                        szBaseDir,
                        &si,
                        &pi
                );

    if (!ret) {
        debug(TEXT("CreateProcess failed (%d)"), GetLastError());
        debug(TEXT("    szBaseDir=%s"), szBaseDir);
        debug(TEXT("    szMainExe=%s"), szMainExe);
    }

    return pi.hProcess;
}

/*
 *
 * Called by SCM whenever a control code is sent to the service
 * using the ControlService function.
 *
 * @param dwCtrl - control code
 */
VOID WINAPI SvcCtrlHandler( DWORD dwCtrl )
{
    switch(dwCtrl) 
    {  
        case SERVICE_CONTROL_STOP: 
            ReportSvcStatus(SERVICE_STOP_PENDING, NO_ERROR, 0);
            // Signal the service to stop.
            SetEvent(ghSvcStopEvent);
            ReportSvcStatus(gSvcStatus.dwCurrentState, NO_ERROR, 0);
            return; 
        case SERVICE_CONTROL_INTERROGATE: 
            break; 
        default: 
            break;
   }   
}

/*
 * Sets the current service status and reports it to the SCM.
 *
 * @param dwCurrentState - The current state (see SERVICE_STATUS)
 * @param dwWin32ExitCode - The system error code
 * @param dwWaitHint - Estimated time for pending operation, 
 *                     in milliseconds
 */
VOID ReportSvcStatus( DWORD dwCurrentState,
                      DWORD dwWin32ExitCode,
                      DWORD dwWaitHint)
{
    static DWORD dwCheckPoint = 1;

    // Fill in the SERVICE_STATUS structure.
    gSvcStatus.dwCurrentState = dwCurrentState;
    gSvcStatus.dwWin32ExitCode = dwWin32ExitCode;
    gSvcStatus.dwWaitHint = dwWaitHint;

    if (dwCurrentState == SERVICE_START_PENDING) {
        gSvcStatus.dwControlsAccepted = 0;
    } else {
        gSvcStatus.dwControlsAccepted = SERVICE_ACCEPT_STOP;
    }

    if ((dwCurrentState == SERVICE_RUNNING) ||
        (dwCurrentState == SERVICE_STOPPED))
    {
        gSvcStatus.dwCheckPoint = 0;
    } else {
        gSvcStatus.dwCheckPoint = dwCheckPoint++;
    }

    // Report the status of the service to the SCM.
    SetServiceStatus( gSvcStatusHandle, &gSvcStatus );
}

/*
 * Logs messages to the event log
 *
 * @param szFunction - name of function that failed
 *
 * The service must have an entry in the Application event log.
 */
VOID SvcReportEvent(LPTSTR szFunction) 
{ 
    HANDLE hEventSource;
    LPCTSTR lpszStrings[2];
    TCHAR Buffer[80];

    hEventSource = RegisterEventSource(NULL, SVCNAME);

    if(hEventSource != NULL) {
        StringCchPrintf(Buffer, 80, TEXT("%s failed with %d"), szFunction, GetLastError());

        lpszStrings[0] = SVCNAME;
        lpszStrings[1] = Buffer;

        ReportEvent(hEventSource,        // event log handle
                    EVENTLOG_ERROR_TYPE, // event type
                    0,                   // event category
                    SVC_ERROR,           // event identifier
                    NULL,                // no security identifier
                    2,                   // size of lpszStrings array
                    0,                   // no binary data
                    lpszStrings,         // array of strings
                    NULL);               // no binary data

        DeregisterEventSource(hEventSource);
    }
}
