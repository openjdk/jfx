/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

#pragma once

#if ENABLE(NOTIFICATIONS) || ENABLE(LEGACY_NOTIFICATIONS)
#include <WebCore/NotificationClient.h>

namespace WebCore {
// Empty stub for NotificationClient; to be implemented later
class NotificationClientJava final : public NotificationClient {
public:
    // since WebKit Notification API doesn't provide a method to remove a NotificationClient,
    // notificationClient is to be instantiated on WebPage creation and remain till the app termination
    static NotificationClientJava* instance() {
        static NotificationClientJava inst;
        return &inst;
    }
    NotificationClientJava() {}
    bool show(Notification*) override { return false; }
    void cancel(Notification*) override {}
    void notificationObjectDestroyed(Notification*) override {}
    void notificationControllerDestroyed() override {}
#if ENABLE(LEGACY_NOTIFICATIONS)
    void requestPermission(ScriptExecutionContext*, RefPtr<VoidCallback>&&) override {}
#endif
#if ENABLE(NOTIFICATIONS)
    void requestPermission(ScriptExecutionContext*, RefPtr<NotificationPermissionCallback>&&) override {}
#endif
    bool hasPendingPermissionRequests(ScriptExecutionContext*) const override { return false;};
    void cancelRequestsForPermission(ScriptExecutionContext*) override {}
    Permission checkPermission(ScriptExecutionContext*) override { return NotificationPermission::Denied; }
    ~NotificationClientJava() override {}
};

} // namespace WebCore
#endif // ENABLE(NOTIFICATIONS) || ENABLE(LEGACY_NOTIFICATIONS)
