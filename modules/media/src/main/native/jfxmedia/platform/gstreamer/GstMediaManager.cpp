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

#include "GstMediaManager.h"
#include <jfxmedia_errors.h>
#include <jni/Logger.h>
#include <Common/VSMemory.h>
#include <Utils/LowLevelPerf.h>

//*************************************************************************************************
//********** class CGstMediaManager
//*************************************************************************************************

volatile bool CGstMediaManager::m_bStopGlibLogFunc = false;

/**
 * CGstMediaManager::CGstMediaManager()
 *
 * Constructor
 */
CGstMediaManager::CGstMediaManager()
    : m_bMainLoopCreateFailed(false), m_pMainContext(NULL), m_pMainLoop(NULL), m_pMainLoopThread(NULL)
{}

/**
 * CGstMediaManager::~CGstMediaManager()
 *
 * Destructor
 */
CGstMediaManager::~CGstMediaManager()
{
#if JFXMEDIA_DEBUG
    g_print ("CGstMediaManager::~CGstMediaManager()\n");
#endif
    m_bStopGlibLogFunc = true;

    if (NULL != m_pRunloopCond)
    {
        g_cond_free(m_pRunloopCond);
        m_pRunloopCond = NULL;
    }

    if (NULL != m_pRunloopMutex)
    {
        g_mutex_free(m_pRunloopMutex);
        m_pRunloopMutex = NULL;
    }

    if (NULL != m_pMainLoop)
    {
        g_main_loop_quit (m_pMainLoop);
        g_main_loop_unref(m_pMainLoop);
        m_pMainLoop = NULL;
    }

    if (NULL != m_pMainContext)
    {
        g_main_context_unref(m_pMainContext);
        m_pMainContext = NULL;
    }

    gst_deinit();

#if ENABLE_LOWLEVELPERF && TARGET_OS_MAC
    g_mem_profile ();
#endif
}

/**
 * CGstMediaManager::Init().
 *
 * @param   data    user-defined data. Pointer to this.
 * @return  Java long reference to the media.
 */
uint32_t CGstMediaManager::Init()
{
    GError*     pError = NULL;
    uint32_t    uRetCode = ERROR_NONE;

#if ENABLE_LOWLEVELPERF && TARGET_OS_MAC
    g_mem_set_vtable (glib_mem_profiler_table);
#endif

#if ENABLE_VISUAL_STUDIO_MEMORY_LEAKS_DETECTION && TARGET_OS_WIN32
    _CrtSetDbgFlag ( 0 );
#endif // ENABLE_VISUAL_STUDIO_MEMORY_LEAKS_DETECTION

    //***** Try to initialize the GStreamer system
    if (!g_thread_supported())
        g_thread_init (NULL);

    LOWLEVELPERF_EXECTIMESTART("gst_init_check()");
    // disable installing SIGSEGV signal handling as it interferes with Java's signal handling
    gst_segtrap_set_enabled(false);
    if (!gst_init_check(NULL, NULL, NULL))
    {
        LOGGER_LOGMSG(LOGGER_DEBUG, "Could not init GStreamer!\n");
        return ERROR_MANAGER_ENGINEINIT_FAIL;
    }
    LOWLEVELPERF_EXECTIMESTOP("gst_init_check()");

#if ENABLE_VISUAL_STUDIO_MEMORY_LEAKS_DETECTION && TARGET_OS_WIN32
    _CrtSetDbgFlag(_CRTDBG_ALLOC_MEM_DF | _CRTDBG_LEAK_CHECK_DF);
#endif // ENABLE_VISUAL_STUDIO_MEMORY_LEAKS_DETECTION

    //***** Create mutex and condition variable
    m_pRunloopCond = g_cond_new();
    m_pRunloopMutex = g_mutex_new();

#if ENABLE_VISUAL_STUDIO_MEMORY_LEAKS_DETECTION && TARGET_OS_WIN32
    _CrtSetDbgFlag(0);
#endif // ENABLE_VISUAL_STUDIO_MEMORY_LEAKS_DETECTION

    //***** Create the primary run loop
    m_pMainLoopThread = g_thread_create((GThreadFunc)run_loop, this, FALSE, &pError);
    if (m_pMainLoopThread == NULL)
    {
        LOGGER_LOGMSG(LOGGER_DEBUG, "Could not create main GThread!!\n");
        LOGGER_LOGMSG(LOGGER_DEBUG, pError->message);
        return ERROR_MANAGER_RUNLOOP_FAIL;
    }

#if ENABLE_VISUAL_STUDIO_MEMORY_LEAKS_DETECTION && TARGET_OS_WIN32
    _CrtSetDbgFlag(_CRTDBG_ALLOC_MEM_DF | _CRTDBG_LEAK_CHECK_DF);
#endif // ENABLE_VISUAL_STUDIO_MEMORY_LEAKS_DETECTION

    //***** Wait till the run loop has fully initialized.  Bad things happen if we do not do this, including crashers.
    g_mutex_lock(m_pRunloopMutex);
    while (NULL == m_pMainLoop)
        g_cond_wait(m_pRunloopCond, m_pRunloopMutex);
    g_mutex_unlock(m_pRunloopMutex);

    if (m_bMainLoopCreateFailed)
        uRetCode = ERROR_GSTREAMER_MAIN_LOOP_CREATE;

    // Free no longer needed GCond.
    if (NULL != m_pRunloopCond)
    {
        g_cond_free(m_pRunloopCond);
        m_pRunloopCond = NULL;
    }

    // Free no longer needed GMutex.
    if (NULL != m_pRunloopMutex)
    {
        g_mutex_free(m_pRunloopMutex);
        m_pRunloopMutex = NULL;
    }

    // Set the default Glib log handler.
    g_log_set_default_handler (GlibLogFunc, this);

    return uRetCode;
}

/**
 * CGstMediaManager::run_loop().  The glib runloop.  One per process.
 *
 * @param   data user-defined data. Pointer to this.
 * @return  Java long reference to the media.
 */

gpointer CGstMediaManager::run_loop(CGstMediaManager *manager)
{
    g_mutex_lock(manager->m_pRunloopMutex);

#if ENABLE_VISUAL_STUDIO_MEMORY_LEAKS_DETECTION && TARGET_OS_WIN32
    _CrtSetDbgFlag(0);
#endif // ENABLE_VISUAL_STUDIO_MEMORY_LEAKS_DETECTION

    manager->m_pMainContext = g_main_context_new();
    manager->m_pMainLoop = g_main_loop_new(manager->m_pMainContext, FALSE);
    manager->m_bMainLoopCreateFailed = NULL == manager->m_pMainLoop;

#if ENABLE_VISUAL_STUDIO_MEMORY_LEAKS_DETECTION && TARGET_OS_WIN32
    _CrtSetDbgFlag(_CRTDBG_ALLOC_MEM_DF | _CRTDBG_LEAK_CHECK_DF);
#endif // ENABLE_VISUAL_STUDIO_MEMORY_LEAKS_DETECTION

    g_cond_signal(manager->m_pRunloopCond);
    g_mutex_unlock(manager->m_pRunloopMutex);

    if (NULL != manager->m_pMainLoop)
        g_main_loop_run(manager->m_pMainLoop);

    return NULL;
}

/**
 * CGstMediaManager::GlibLogFunc  Default handler for Glib log messages.
 *
 * @param   log_domain  the log domain of the message.
 * @param   log_level   the log level of the message.
 * @param   message     the message to process
 * @param   user_data   user data, set in g_log_set_handler().
 */
void CGstMediaManager::GlibLogFunc(const gchar* log_domain, GLogLevelFlags log_level,
                                   const gchar* message, gpointer user_data)
{
    if (m_bStopGlibLogFunc)
        return;

#if ENABLE_LOGGING
    if (log_level & G_LOG_LEVEL_CRITICAL || log_level & G_LOG_LEVEL_ERROR)
    {
        LOGGER_LOGMSG (LOGGER_ERROR, message);
    }
    else if (log_level & G_LOG_LEVEL_WARNING)
    {
        LOGGER_LOGMSG (LOGGER_WARNING, message);
    }
    else if (log_level & G_LOG_LEVEL_MESSAGE ||
             log_level & G_LOG_LEVEL_INFO)
    {
        LOGGER_LOGMSG (LOGGER_INFO, message);
    }
    else if (log_level & G_LOG_LEVEL_DEBUG)
    {
        LOGGER_LOGMSG (LOGGER_DEBUG, message);
    }
#endif
}
