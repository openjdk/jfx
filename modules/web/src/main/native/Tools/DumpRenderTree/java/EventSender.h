/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef EventSender_h
#define EventSender_h

#include <JavaScriptCore/JSObjectRef.h>
#include <wtf/java/JavaRef.h>

void makeEventSender(JSContextRef context, JSObjectRef windowObject,
        const JLObject& eventSender, JSValueRef* exception);

#endif // EventSender_h
