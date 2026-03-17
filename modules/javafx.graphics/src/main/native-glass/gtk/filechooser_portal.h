/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

#ifndef FILECHOOSER_PORTAL_H
#define FILECHOOSER_PORTAL_H

#include <gdk/gdk.h>
#include <gtk/gtk.h>
#include <gio/gio.h>

#include <vector>
#include <string>

struct PortalFileFilter {
    std::string name;
    std::vector<std::string> patterns;
};

struct PortalFileChooserResult {
    bool accepted;
    bool failed;
    std::vector<std::string> uris;
    int filterIndex;
};

class PortalFileChooser {
public:
    PortalFileChooser();
    ~PortalFileChooser();

    void setParentWindow(GdkWindow *gdkWindow);
    void setTitle(const char *title);
    void setCurrentFolder(const char *folder);
    void setCurrentName(const char *name);
    void setMultiple(bool multiple);
    void setFilters(const std::vector<PortalFileFilter> &filters);
    void setDefaultFilterIndex(int index);

    PortalFileChooserResult openFile();
    PortalFileChooserResult saveFile();
    PortalFileChooserResult openFolder();

private:
    std::string parentWindowHandle;
    std::string dialogTitle;
    std::string currentFolder;
    std::string currentName;
    bool multipleSelection;
    std::vector<PortalFileFilter> filters;
    int defaultFilterIndex;

    GDBusConnection *connection;
    gchar *senderName;

    bool initConnection();
    void buildRequestPath(gchar **path, gchar **token);

    GVariant *buildFilterVariant();
    GVariant *buildCurrentFilterVariant(int index);
    GVariant *buildOptionsDict(const char *handleToken, bool isOpen, bool isDirectory);

    PortalFileChooserResult runDialog(const char *method, bool isOpen, bool isDirectory);

    static void onResponse(GDBusConnection *connection,
                           const char *senderName,
                           const char *objectPath,
                           const char *interfaceName,
                           const char *signalName,
                           GVariant *parameters,
                           void *userData);

    static void callReturned(GObject *source,
                             GAsyncResult *result,
                             gpointer userData);

    struct ResponseData {
        guint subscriptionId;
        PortalFileChooserResult result;
        PortalFileChooser *self;
        GMainLoop *loop;
        bool done;
    };

    PortalFileChooserResult parseResponse(GVariant *parameters);
};

#endif
