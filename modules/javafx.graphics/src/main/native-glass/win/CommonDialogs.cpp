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

#include "common.h"

#include "CommonDialogs_COM.h"
#include "CommonDialogs_Standard.h"
#include "BaseWnd.h"
#include "GlassStringBlock.h"
#include "glass_win_api.h"

class CommonDialogOwner {
    private:
        BaseWnd * wnd;
    public:
        CommonDialogOwner(HWND hwnd)
        {
            wnd = hwnd ? BaseWnd::FromHandle(hwnd) : NULL;
            if (wnd) {
                wnd->SetCommonDialogOwner(true);
            }
        }

        ~CommonDialogOwner()
        {
            if (wnd) {
                wnd->SetCommonDialogOwner(false);
            }
        }
};

/*
 * ---- The exports of glass_win_api.h's common-dialog section ----
 *
 * Not marshalled (the dialogs run a modal loop on the calling thread, as the former JNI entry points
 * did); the CommonDialogOwner marking and the Vista / legacy switch are what those entry points did
 * around the show functions. C linkage comes from glass_win_api.h.
 */

int32_t gwin_dialog_file(void* owner, const uint16_t* folder, const uint16_t* filename, const uint16_t* title,
                         int32_t type, int32_t multiple, const GwinFileFilter* filters, int32_t filter_count,
                         int32_t default_filter_index,
                         uint16_t** out_files, int32_t* out_count, int32_t* out_filter_index)
{
    if (out_files == NULL || out_count == NULL || out_filter_index == NULL) {
        return GWIN_DIALOG_FAILED;
    }
    *out_files = NULL;
    *out_count = 0;
    *out_filter_index = 0;
    try {
        CommonDialogOwner cdo((HWND) owner);

        if (IS_WINVISTA) {
            return COMFileChooser_Show((HWND) owner, reinterpret_cast<LPCWSTR>(folder),
                                       reinterpret_cast<LPCWSTR>(filename), reinterpret_cast<LPCWSTR>(title),
                                       type, multiple, filters, filter_count, default_filter_index,
                                       out_files, out_count, out_filter_index);
        } else {
            return StandardFileChooser_Show((HWND) owner, reinterpret_cast<LPCWSTR>(folder),
                                            reinterpret_cast<LPCWSTR>(filename), reinterpret_cast<LPCWSTR>(title),
                                            type, multiple, filters, filter_count, default_filter_index,
                                            out_files, out_count, out_filter_index);
        }
    } catch (...) {
        return GWIN_DIALOG_FAILED;
    }
}

int32_t gwin_dialog_folder(void* owner, const uint16_t* folder, const uint16_t* title, uint16_t** out_path)
{
    if (out_path == NULL) {
        return GWIN_DIALOG_FAILED;
    }
    *out_path = NULL;
    try {
        CommonDialogOwner cdo((HWND) owner);

        if (IS_WINVISTA) {
            return COMFolderChooser_Show((HWND) owner, reinterpret_cast<LPCWSTR>(folder),
                                         reinterpret_cast<LPCWSTR>(title), out_path);
        } else {
            return StandardFolderChooser_Show((HWND) owner, reinterpret_cast<LPCWSTR>(folder),
                                              reinterpret_cast<LPCWSTR>(title), out_path);
        }
    } catch (...) {
        return GWIN_DIALOG_FAILED;
    }
}

