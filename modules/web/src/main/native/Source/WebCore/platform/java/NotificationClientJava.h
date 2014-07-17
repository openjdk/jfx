/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 */
#if ENABLE(NOTIFICATIONS) || ENABLE(LEGACY_NOTIFICATIONS)
#include "NotificationClient.h"

namespace WebCore {
// Empty stub for NotificationClient; to be implemented later
class NotificationClientJava: public NotificationClient {
public:
    // since WebKit Notification API doesn't provide a method to remove a NotificationClient,
    // notificationClient is to be instantiated on WebPage creation and remain till the app termination
    static NotificationClientJava* instance() {
        static NotificationClientJava inst;
        return &inst;
    }
    NotificationClientJava() {}
    bool show(Notification* n) {return false;}
    void cancel(Notification*) {}
    void notificationObjectDestroyed(Notification*) {}
    void notificationControllerDestroyed() {}
#if ENABLE(LEGACY_NOTIFICATIONS)
    void requestPermission(ScriptExecutionContext*, PassRefPtr<VoidCallback>) {}
#endif
#if ENABLE(NOTIFICATIONS)
    void requestPermission(ScriptExecutionContext*, PassRefPtr<NotificationPermissionCallback>) {}
#endif
    void cancelRequestsForPermission(ScriptExecutionContext*) {}
    Permission checkPermission(ScriptExecutionContext*) {return PermissionDenied;}
    ~NotificationClientJava() {}
};

} // namespace WebCore
#endif // ENABLE(NOTIFICATIONS) || ENABLE(LEGACY_NOTIFICATIONS)
