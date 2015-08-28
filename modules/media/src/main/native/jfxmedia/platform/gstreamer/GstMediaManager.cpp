/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
    : m_bMainLoopCreateFailed(false), m_pMainContext(NULL), m_pMainLoop(NULL),
      m_pMainLoopThread(NULL), m_bClearRunloopMutex(false), m_bClearRunloopCond(false)
{
    m_bClearStartLoopMutex = false;
    m_bClearStartLoopCond = false;
    m_bStartMainLoop = false;
}

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

    if (m_bClearRunloopCond)
    {
        g_cond_clear(&m_RunloopCond);
        m_bClearRunloopCond = false;
    }

    if (m_bClearRunloopMutex)
    {
        g_mutex_clear(&m_RunloopMutex);
        m_bClearRunloopMutex = false;
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

    if (m_bClearStartLoopMutex)
    {
        g_mutex_clear(&m_StartLoopMutex);
        m_bClearStartLoopMutex = false;
    }

    if (m_bClearStartLoopCond)
    {
        g_cond_clear(&m_StartLoopCond);
        m_bClearStartLoopCond = false;
    }

    // We do not call gst_deinit() anymore, due to deadlock when GLib tries to
    // free memory and pipeline is not shutdown. Also, it is not required to
    // call this function.
    // gst_deinit();

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
    uint32_t    uRetCode = ERROR_NONE;

#if ENABLE_LOWLEVELPERF && TARGET_OS_MAC
    g_mem_set_vtable (glib_mem_profiler_table);
#endif

#if ENABLE_VISUAL_STUDIO_MEMORY_LEAKS_DETECTION && TARGET_OS_WIN32
    _CrtSetDbgFlag ( 0 );
#endif // ENABLE_VISUAL_STUDIO_MEMORY_LEAKS_DETECTION

    //***** Try to initialize the GStreamer system
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
    g_cond_init(&m_RunloopCond);
    m_bClearRunloopCond = true;
    g_mutex_init(&m_RunloopMutex);
    m_bClearRunloopMutex = true;
    g_mutex_init(&m_StartLoopMutex);
    m_bClearStartLoopMutex = true;
    g_cond_init(&m_StartLoopCond);
    m_bClearStartLoopCond = true;

#if ENABLE_VISUAL_STUDIO_MEMORY_LEAKS_DETECTION && TARGET_OS_WIN32
    _CrtSetDbgFlag(0);
#endif // ENABLE_VISUAL_STUDIO_MEMORY_LEAKS_DETECTION

    //***** Create the primary run loop
    m_pMainLoopThread = g_thread_new("MainLoop", (GThreadFunc)run_loop, this);
    if (m_pMainLoopThread == NULL)
    {
        LOGGER_LOGMSG(LOGGER_DEBUG, "Could not create main GThread!!\n");
        return ERROR_MANAGER_RUNLOOP_FAIL;
    }

#if ENABLE_VISUAL_STUDIO_MEMORY_LEAKS_DETECTION && TARGET_OS_WIN32
    _CrtSetDbgFlag(_CRTDBG_ALLOC_MEM_DF | _CRTDBG_LEAK_CHECK_DF);
#endif // ENABLE_VISUAL_STUDIO_MEMORY_LEAKS_DETECTION

    //***** Wait till the run loop has fully initialized.  Bad things happen if we do not do this, including crashers.
    g_mutex_lock(&m_RunloopMutex);
    while (NULL == m_pMainLoop)
        g_cond_wait(&m_RunloopCond, &m_RunloopMutex);
    g_mutex_unlock(&m_RunloopMutex);

    if (m_bMainLoopCreateFailed)
    {
        uRetCode = ERROR_GSTREAMER_MAIN_LOOP_CREATE;
    }

    // Free no longer needed GCond.
    if (m_bClearRunloopCond)
    {
        g_cond_clear(&m_RunloopCond);
        m_bClearRunloopCond = false;
    }

    // Free no longer needed GMutex.
    if (m_bClearRunloopMutex)
    {
        g_mutex_clear(&m_RunloopMutex);
        m_bClearRunloopMutex = false;
    }

    // Set the default Glib log handler.
    g_log_set_default_handler (GlibLogFunc, this);

    return uRetCode;
}

void CGstMediaManager::StartMainLoop()
{
    if (m_bStartMainLoop)
        return;

    g_mutex_lock(&m_StartLoopMutex);
    m_bStartMainLoop = true;
    g_cond_signal(&m_StartLoopCond);
    g_mutex_unlock(&m_StartLoopMutex);
}

/**
 * CGstMediaManager::run_loop().  The glib runloop.  One per process.
 *
 * @param   data user-defined data. Pointer to this.
 * @return  Java long reference to the media.
 */

gpointer CGstMediaManager::run_loop(CGstMediaManager *manager)
{
    g_mutex_lock(&manager->m_RunloopMutex);

#if ENABLE_VISUAL_STUDIO_MEMORY_LEAKS_DETECTION && TARGET_OS_WIN32
    _CrtSetDbgFlag(0);
#endif // ENABLE_VISUAL_STUDIO_MEMORY_LEAKS_DETECTION

    manager->m_pMainContext = g_main_context_new();
    manager->m_pMainLoop = g_main_loop_new(manager->m_pMainContext, FALSE);
    manager->m_bMainLoopCreateFailed = NULL == manager->m_pMainLoop;

#if ENABLE_VISUAL_STUDIO_MEMORY_LEAKS_DETECTION && TARGET_OS_WIN32
    _CrtSetDbgFlag(_CRTDBG_ALLOC_MEM_DF | _CRTDBG_LEAK_CHECK_DF);
#endif // ENABLE_VISUAL_STUDIO_MEMORY_LEAKS_DETECTION

    g_cond_signal(&manager->m_RunloopCond);
    g_mutex_unlock(&manager->m_RunloopMutex);

    if (NULL != manager->m_pMainLoop)
    {
        g_mutex_lock(&manager->m_StartLoopMutex);
        while (!manager->m_bStartMainLoop)
            g_cond_wait(&manager->m_StartLoopCond, &manager->m_StartLoopMutex);
        g_mutex_unlock(&manager->m_StartLoopMutex);

        g_main_loop_run(manager->m_pMainLoop);
    }

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
