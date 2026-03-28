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

#include "filechooser_portal.h"
#include "glass_general.h"

#include <gdk/gdkx.h>
#include <cstring>
#include <atomic>

#define PORTAL_DESKTOP_BUS_NAME    "org.freedesktop.portal.Desktop"
#define PORTAL_DESKTOP_OBJECT_PATH "/org/freedesktop/portal/desktop"
#define PORTAL_IFACE_FILECHOOSER   "org.freedesktop.portal.FileChooser"
#define PORTAL_IFACE_REQUEST       "org.freedesktop.portal.Request"

#define PORTAL_FC_TOKEN_TEMPLATE   "fxFileChooser%lu"
#define PORTAL_FC_REQUEST_TEMPLATE "/org/freedesktop/portal/desktop/request/%s/fxFileChooser%lu"

PortalFileChooser::PortalFileChooser()
    : multipleSelection(false),
      defaultFilterIndex(-1),
      connection(nullptr),
      senderName(nullptr) {
}

PortalFileChooser::~PortalFileChooser() {
    LOG0("PortalFileChooser: destructor called\n");
    if (senderName) {
        g_free(senderName);
        senderName = nullptr;
    }
    if (connection) {
        g_object_unref(connection);
        connection = nullptr;
    }
}

void PortalFileChooser::setParentWindow(GdkWindow *gdkWindow) {
    if (gdkWindow) {
        XID xid = GDK_WINDOW_XID(gdkWindow);
        char buf[64];
        snprintf(buf, sizeof(buf), "x11:%lx", (unsigned long) xid);
        parentWindowHandle = buf;
        LOG1("PortalFileChooser: Parent window handle: %s\n", parentWindowHandle.c_str());
    } else {
        parentWindowHandle = "";
    }
}

void PortalFileChooser::setTitle(const char *title) {
    dialogTitle = title ? title : "";
    LOG1("PortalFileChooser: Dialog title: %s\n", dialogTitle.c_str());
}

void PortalFileChooser::setCurrentFolder(const char *folder) {
    currentFolder = folder ? folder : "";
    LOG1("PortalFileChooser: setCurrentFolder: %s\n", currentFolder.c_str());
}

void PortalFileChooser::setCurrentName(const char *name) {
    LOG1("PortalFileChooser: setCurrentName: %s\n", name ? name : "(empty)");
    currentName = name ? name : "";
}

void PortalFileChooser::setMultiple(bool multiple) {
    LOG1("PortalFileChooser: setMultiple: %s\n", multiple ? "true" : "false");
    multipleSelection = multiple;
}

void PortalFileChooser::setFilters(const std::vector<PortalFileFilter> &f) {
    LOG1("PortalFileChooser: setFilters: %zu filters\n", f.size());
    filters = f;
}

void PortalFileChooser::setDefaultFilterIndex(int index) {
    LOG1("PortalFileChooser: setDefaultFilterIndex: %d\n", index);
    defaultFilterIndex = index;
}

bool PortalFileChooser::initConnection() {
    if (connection) {
        return true;
    }

    GError *err = nullptr;
    connection = g_bus_get_sync(G_BUS_TYPE_SESSION, nullptr, &err);
    if (err) {
        LOG1("PortalFileChooser: failed to get session bus: %s\n", err->message)
        g_error_free(err);
        return false;
    }

    const gchar *name = g_dbus_connection_get_unique_name(connection);
    if (!name) {
        LOG0("PortalFileChooser: failed to get unique connection name\n")
        g_object_unref(connection);
        connection = nullptr;
        return false;
    }

    GString *nameStr = g_string_new(name);
    g_string_erase(nameStr, 0, 1);
    for (gsize i = 0; i < nameStr->len; ++i) {
        if (nameStr->str[i] == '.') {
            nameStr->str[i] = '_';
        }
    }
    senderName = g_strdup(nameStr->str);
    g_string_free(nameStr, TRUE);

    return true;
}

void PortalFileChooser::buildRequestPath(gchar **path, gchar **token) {
    static std::atomic<uint64_t> counter{0};
    uint64_t current = ++counter;

    *token = g_strdup_printf(PORTAL_FC_TOKEN_TEMPLATE, current);
    *path = g_strdup_printf(PORTAL_FC_REQUEST_TEMPLATE, senderName, current);
}

GVariant *PortalFileChooser::buildFilterVariant() {
    GVariantBuilder filtersBuilder;
    g_variant_builder_init(&filtersBuilder, G_VARIANT_TYPE("a(sa(us))"));

    for (const auto &f : filters) {
        GVariantBuilder patternsBuilder;
        g_variant_builder_init(&patternsBuilder, G_VARIANT_TYPE("a(us)"));

        for (const auto &p : f.patterns) {
            g_variant_builder_add(&patternsBuilder, "(us)", (guint32) 0, p.c_str());
        }

        g_variant_builder_add(&filtersBuilder, "(s@a(us))",
                              f.name.c_str(),
                              g_variant_builder_end(&patternsBuilder));
    }

    return g_variant_builder_end(&filtersBuilder);
}

GVariant *PortalFileChooser::buildCurrentFilterVariant(int index) {
    if (index < 0 || index >= (int) filters.size()) {
        return nullptr;
    }

    const auto &f = filters[index];

    GVariantBuilder patternsBuilder;
    g_variant_builder_init(&patternsBuilder, G_VARIANT_TYPE("a(us)"));
    for (const auto &p : f.patterns) {
        g_variant_builder_add(&patternsBuilder, "(us)", (guint32) 0, p.c_str());
    }

    return g_variant_new("(s@a(us))",
                         f.name.c_str(),
                         g_variant_builder_end(&patternsBuilder));
}

GVariant *PortalFileChooser::buildOptionsDict(const char *handleToken, bool isOpen, bool isDirectory) {
    GVariantBuilder builder;
    g_variant_builder_init(&builder, G_VARIANT_TYPE_VARDICT);

    g_variant_builder_add(&builder, "{sv}", "handle_token",
                          g_variant_new_string(handleToken));

    if (isOpen) {
        g_variant_builder_add(&builder, "{sv}", "multiple",
                              g_variant_new_boolean(multipleSelection));
    }

    if (isDirectory) {
        g_variant_builder_add(&builder, "{sv}", "directory",
                              g_variant_new_boolean(TRUE));
    }

    if (!filters.empty()) {
        g_variant_builder_add(&builder, "{sv}", "filters", buildFilterVariant());

        GVariant *currentFilter = buildCurrentFilterVariant(defaultFilterIndex);
        if (currentFilter) {
            g_variant_builder_add(&builder, "{sv}", "current_filter", currentFilter);
        }
    }

    if (!currentFolder.empty()) {
        gsize len = currentFolder.size();
        const char *folderStr = currentFolder.c_str();
        GVariant *folderBytes = g_variant_new_fixed_array(
                G_VARIANT_TYPE_BYTE, folderStr, len + 1, sizeof(char));
        g_variant_builder_add(&builder, "{sv}", "current_folder", folderBytes);
    }

    if (!currentName.empty() && !isOpen) {
        g_variant_builder_add(&builder, "{sv}", "current_name",
                              g_variant_new_string(currentName.c_str()));
    }

    return g_variant_builder_end(&builder);
}

void PortalFileChooser::onResponse(
        GDBusConnection *conn,
        const char *sender,
        const char *objectPath,
        const char *interfaceName,
        const char *signalName,
        GVariant *parameters,
        void *userData) {

    (void) conn;
    (void) sender;
    (void) objectPath;
    (void) interfaceName;
    (void) signalName;

    ResponseData *data = static_cast<ResponseData *>(userData);

    data->result = data->self->parseResponse(parameters);
    data->done = true;

    if (data->loop && g_main_loop_is_running(data->loop)) {
        g_main_loop_quit(data->loop);
    }
}

void PortalFileChooser::callReturned(
        GObject *source,
        GAsyncResult *result,
        gpointer userData) {

    ResponseData *data = static_cast<ResponseData *>(userData);

    GError *err = nullptr;
    GVariant *ret = g_dbus_connection_call_finish(G_DBUS_CONNECTION(source), result, &err);

    if (err) {
        LOG1("PortalFileChooser: DBus call failed: %s\n", err->message)
        g_error_free(err);

        data->result.failed = true;
        data->done = true;

        if (data->loop && g_main_loop_is_running(data->loop)) {
            g_main_loop_quit(data->loop);
        }
        return;
    }

    if (ret) {
        g_variant_unref(ret);
    }
}

PortalFileChooserResult PortalFileChooser::parseResponse(GVariant *parameters) {
    PortalFileChooserResult result;
    result.accepted = false;
    result.failed = false;
    result.filterIndex = -1;

    guint32 status;
    GVariant *resultDict = nullptr;

    g_variant_get(parameters, "(u@a{sv})", &status, &resultDict);

    if (status != 0) {
        if (resultDict) g_variant_unref(resultDict);
        return result;
    }

    result.accepted = true;

    GVariant *urisVariant = g_variant_lookup_value(resultDict, "uris", G_VARIANT_TYPE_STRING_ARRAY);
    if (urisVariant) {
        gsize nUris = 0;
        const gchar **uris = g_variant_get_strv(urisVariant, &nUris);
        for (gsize i = 0; i < nUris; i++) {
            result.uris.push_back(std::string(uris[i]));
        }
        g_free(uris);
        g_variant_unref(urisVariant);
    }

    if (!filters.empty()) {
        GVariant *currentFilterVariant = g_variant_lookup_value(
                resultDict, "current_filter", G_VARIANT_TYPE("(sa(us))"));
        if (currentFilterVariant) {
            const gchar *filterName = nullptr;
            GVariant *patternsVariant = nullptr;
            g_variant_get(currentFilterVariant, "(&s@a(us))", &filterName, &patternsVariant);

            if (filterName) {
                for (int i = 0; i < (int) filters.size(); i++) {
                    if (filters[i].name == filterName) {
                        result.filterIndex = i;
                        break;
                    }
                }
            }

            if (patternsVariant) g_variant_unref(patternsVariant);
            g_variant_unref(currentFilterVariant);
        }
    }

    if (resultDict) g_variant_unref(resultDict);
    return result;
}

PortalFileChooserResult PortalFileChooser::runDialog(const char *method, bool isOpen, bool isDirectory) {

    PortalFileChooserResult emptyResult;
    emptyResult.accepted = false;
    emptyResult.failed = true;
    emptyResult.filterIndex = -1;

    if (!initConnection()) {
        return emptyResult;
    }

    gchar *requestPath = nullptr;
    gchar *requestToken = nullptr;
    buildRequestPath(&requestPath, &requestToken);

    ResponseData responseData;
    responseData.subscriptionId = 0;
    responseData.result = emptyResult;
    responseData.self = this;
    responseData.loop = nullptr;
    responseData.done = false;

    responseData.subscriptionId = g_dbus_connection_signal_subscribe(
            connection,
            PORTAL_DESKTOP_BUS_NAME,
            PORTAL_IFACE_REQUEST,
            "Response",
            requestPath,
            nullptr,
            G_DBUS_SIGNAL_FLAGS_NO_MATCH_RULE,
            onResponse,
            &responseData,
            nullptr);

    GVariant *options = buildOptionsDict(requestToken, isOpen, isDirectory);

    responseData.loop = g_main_loop_new(nullptr, FALSE);

    g_dbus_connection_call(
            connection,
            PORTAL_DESKTOP_BUS_NAME,
            PORTAL_DESKTOP_OBJECT_PATH,
            PORTAL_IFACE_FILECHOOSER,
            method,
            g_variant_new("(ss@a{sv})",
                          parentWindowHandle.c_str(),
                          dialogTitle.c_str(),
                          options),
            nullptr,
            G_DBUS_CALL_FLAGS_NONE,
            -1,
            nullptr,
            callReturned,
            &responseData);

    if (!responseData.done) {
        gdk_threads_leave();
        g_main_loop_run(responseData.loop);
        gdk_threads_enter();
    }

    g_main_loop_unref(responseData.loop);

    if (responseData.subscriptionId) {
        g_dbus_connection_signal_unsubscribe(connection, responseData.subscriptionId);
    }

    g_free(requestPath);
    g_free(requestToken);

    return responseData.result;
}

PortalFileChooserResult PortalFileChooser::openFile() {
    return runDialog("OpenFile", true, false);
}

PortalFileChooserResult PortalFileChooser::saveFile() {
    return runDialog("SaveFile", false, false);
}

PortalFileChooserResult PortalFileChooser::openFolder() {
    return runDialog("OpenFile", true, true);
}
