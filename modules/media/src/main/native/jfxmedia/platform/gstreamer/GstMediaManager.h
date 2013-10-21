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

#ifndef _GST_MEDIA_MANAGER_H_
#define _GST_MEDIA_MANAGER_H_

#include <MediaManagement/MediaManager.h>
#include <gst/gst.h>

class CGstMediaManager : public CMediaManager
{
protected:
    friend class CMediaManager;
    friend class CGstAudioPlaybackPipeline;


    CGstMediaManager();
    virtual ~CGstMediaManager();

    uint32_t    Init();

private:
    static void     GlibLogFunc(const gchar* log_domain, GLogLevelFlags log_level,
                                const gchar* message, gpointer user_data);

    static gpointer run_loop(CGstMediaManager* manager);

    bool          m_bMainLoopCreateFailed;

    GMainContext* m_pMainContext;
    GMainLoop*    m_pMainLoop;
    GThread*      m_pMainLoopThread;

    GMutex*       m_pRunloopMutex;
    GCond*        m_pRunloopCond;

    static volatile bool m_bStopGlibLogFunc;
};

#endif // _GST_MEDIA_MANAGER_H_
