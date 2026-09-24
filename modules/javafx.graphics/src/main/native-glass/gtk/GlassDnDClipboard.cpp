/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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
#include "glass_general.h"
#include "glass_dnd.h"

extern gboolean is_dnd_owner;

/*
 * The natives of GtkDnDClipboard at commit 033187ad90 as ggtk_dnd_* functions (glass_gtk_api.h), which
 * GtkDnDClipboard calls through GtkGlassNative. isOwner is here; pushToSystemImpl, popFromSystem,
 * supportedSourceActionsFromSystem and mimesFromSystem live in glass_dnd.cpp (ggtk_dnd_push_to_system,
 * ggtk_dnd_target_get_data, ggtk_dnd_target_get_supported_actions, ggtk_dnd_target_get_mimes), because the drag
 * state they read is file-static there. pushTargetActionToSystem did nothing ("Never called") and has no
 * function: GtkDnDClipboard does nothing in Java.
 */

extern "C" {

int32_t ggtk_dnd_is_owner(void)
{
    return (is_dnd_owner) ? 1 : 0;
}

} // extern "C"
