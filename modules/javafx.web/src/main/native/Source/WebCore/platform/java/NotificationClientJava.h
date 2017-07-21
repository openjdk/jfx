/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
 */

#pragma once

#if ENABLE(NOTIFICATIONS) || ENABLE(LEGACY_NOTIFICATIONS)
#include "NotificationClient.h"

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
    Permission checkPermission(ScriptExecutionContext*) override { return PermissionDenied; }
    ~NotificationClientJava() override {}
};

} // namespace WebCore
#endif // ENABLE(NOTIFICATIONS) || ENABLE(LEGACY_NOTIFICATIONS)
