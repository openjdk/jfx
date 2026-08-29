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

#include "config.h"
#include "EventSender.h"

#include <vector>

#include <drt_java_api.h>
#include <JavaScriptCore/API/JSStringRef.h>

// Defined in TestRunnerJava.cpp, which is where its JNI predecessor lived. The returned
// pointer is owned by `ref` and is valid only while `ref` is alive.
extern const uint16_t* JSStringRef_to_utf16(JSStringRef ref, int32_t* length);

// The 21 EventSender slots of the process-wide host table, or nullptr before drt_init.
// Every slot is optional, so each call site tests its own pointer as well.
static const WKJEventSenderCallbacks* eventSenderCallbacks()
{
    return drt_host ? &drt_host->event_sender : nullptr;
}

// The registry id of the Java EventSender this JavaScript object forwards to. It is stored
// in the object's private data, where the JNI implementation stored a JGObject.
static wkj_ref getEventSender(JSObjectRef object)
{
    const wkj_ref* result = static_cast<const wkj_ref*>(JSObjectGetPrivate(object));
    ASSERT(result);
    return result ? *result : 0;
}

static double getNumber(JSContextRef context, JSValueRef value, JSValueRef* exception)
{
    double result = JSValueToNumber(context, value, exception);
    ASSERT(!exception || !*exception);
    return result;
}

static JSValueRef getProperty(JSContextRef context, JSObjectRef array, const char* property, JSValueRef* exception)
{
    JSStringRef propName = JSStringCreateWithUTF8CString(property);
    JSValueRef result = JSObjectGetProperty(context, array, propName, exception);
    JSStringRelease(propName);
    ASSERT(!exception || !*exception);
    return result;
}

static JSValueRef getValueAt(JSContextRef context, JSObjectRef array, int index, JSValueRef* exception)
{
    JSValueRef result = JSObjectGetPropertyAtIndex(context, array, index, exception);
    ASSERT(!exception || !*exception);
    return result;
}

// Replaces getJString(): the UTF-16 code units go straight to the callback, so the copy
// that env->NewString made is gone. The caller owns the returned JSStringRef and must keep
// it alive for as long as it uses the characters.
static JSStringRef copyString(JSContextRef context, JSValueRef value, JSValueRef* exception)
{
    JSStringRef string = JSValueToStringCopy(context, value, exception);
    ASSERT(!exception || !*exception);
    return string;
}

static int32_t getModifier(JSContextRef context, const JSValueRef value, JSValueRef* exception)
{
    int32_t modifier = 0;
    JSStringRef string = JSValueToStringCopy(context, value, exception);
    ASSERT(!exception || !*exception);

    if (JSStringIsEqualToUTF8CString(string, "altKey")) {
        modifier = 1; // com.sun.javafx.webkit.drt.EventSender.ALT
    }
    else if (JSStringIsEqualToUTF8CString(string, "ctrlKey")) {
        modifier = 2; // com.sun.javafx.webkit.drt.EventSender.CTRL
    }
    else if (JSStringIsEqualToUTF8CString(string, "metaKey")) {
        modifier = 4; // com.sun.javafx.webkit.drt.EventSender.META
    }
    else if (JSStringIsEqualToUTF8CString(string, "shiftKey")) {
        modifier = 8; // com.sun.javafx.webkit.drt.EventSender.SHIFT
    }
    else if (JSStringIsEqualToUTF8CString(string, "addSelectionKey")) {
#if OS(MAC_OS_X)
        modifier = 4; // com.sun.javafx.webkit.drt.EventSender.META
#else
        modifier = 2; // com.sun.javafx.webkit.drt.EventSender.CTRL
#endif
    }
    else if (JSStringIsEqualToUTF8CString(string, "rangeSelectionKey")) {
        modifier = 8; // com.sun.javafx.webkit.drt.EventSender.SHIFT
    }
    else if (JSStringIsEqualToUTF8CString(string, "capsLockKey")) {
        modifier = 32; // com.sun.javafx.webkit.drt.EventSender.CAPS_LOCK
    }
    JSStringRelease(string);
    return modifier;
}

static int32_t getModifers(JSContextRef context, const JSValueRef value, JSValueRef* exception)
{
    // The value may either be a string with a single modifier or an array of modifiers.
    if (JSValueIsString(context, value))
        return getModifier(context, value, exception);

    JSObjectRef array = JSValueToObject(context, value, 0);
    if (!array)
        return 0;

    int32_t modifiers = 0;
    int length = (int) getNumber(context, getProperty(context, array, "length", exception), exception);
    for (int i = 0; i < length; i++) {
        modifiers |= getModifier(context, getValueAt(context, array, i, exception), exception);
    }
    return modifiers;
}

static JSValueRef handleMouseScroll(JSContextRef context, bool continuous,
        JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    if (argumentCount > 1) {
        const WKJEventSenderCallbacks* cb = eventSenderCallbacks();
        if (cb && cb->mouse_scroll) {
            cb->mouse_scroll(getEventSender(object),
                    (float) getNumber(context, arguments[0], exception),
                    (float) getNumber(context, arguments[1], exception),
                    continuous ? 1 : 0);
        }
    }
    return JSValueMakeUndefined(context);
}

static JSValueRef handleMouseUpDown(JSContextRef context, bool pressed,
        JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    int32_t button = 1; // com.sun.webkit.event.WCMouseEvent.BUTTON1 (left)
    if (argumentCount > 0) {
        int number = (int) getNumber(context, arguments[0], exception);
        if ((number == 1) || (number == 3)) {
            // fast/events/mouse-click-events expects the 4th button has event.button = 1
            button = 2; // com.sun.webkit.event.WCMouseEvent.BUTTON2 (middle)
        }
        else if (number == 2) {
            button = 4; // com.sun.webkit.event.WCMouseEvent.BUTTON3 (right)
        }
    }
    int32_t modifiers = pressed ? 16 : 0; // com.sun.javafx.webkit.drt.EventSender.PRESSED
    if (argumentCount > 1) {
        modifiers |= getModifers(context, arguments[1], exception);
    }
    const WKJEventSenderCallbacks* cb = eventSenderCallbacks();
    if (cb && cb->mouse_up_down)
        cb->mouse_up_down(getEventSender(object), button, modifiers);
    return JSValueMakeUndefined(context);
}

static JSValueRef keyDownCallback(JSContextRef context, JSObjectRef function,
        JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    if (argumentCount > 0) {
        int32_t modifiers = 0;
        if (argumentCount > 1) {
            modifiers |= getModifers(context, arguments[1], exception);
        }
        JSStringRef key = copyString(context, arguments[0], exception);
        int32_t keyLength = 0;
        const uint16_t* keyCharacters = JSStringRef_to_utf16(key, &keyLength);

        const WKJEventSenderCallbacks* cb = eventSenderCallbacks();
        if (cb && cb->key_down)
            cb->key_down(getEventSender(object), keyCharacters, keyLength, modifiers);
        JSStringRelease(key);
    }
    return JSValueMakeUndefined(context);
}

static JSValueRef mouseDownCallback(JSContextRef context, JSObjectRef function,
        JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    return handleMouseUpDown(context, true, object,
            argumentCount, arguments, exception);
}

static JSValueRef mouseUpCallback(JSContextRef context, JSObjectRef function,
        JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    return handleMouseUpDown(context, false, object,
            argumentCount, arguments, exception);
}

static JSValueRef mouseMoveToCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    if (argumentCount > 1) {
        const WKJEventSenderCallbacks* cb = eventSenderCallbacks();
        if (cb && cb->mouse_move_to) {
            cb->mouse_move_to(getEventSender(object),
                    (int32_t) getNumber(context, arguments[0], exception),
                    (int32_t) getNumber(context, arguments[1], exception));
        }
    }
    return JSValueMakeUndefined(context);
}

static JSValueRef mouseScrollByCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    return handleMouseScroll(context, false, object,
            argumentCount, arguments, exception);
}

static JSValueRef continuousMouseScrollByCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    return handleMouseScroll(context, true, object,
            argumentCount, arguments, exception);
}

static JSValueRef leapForwardCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    if (argumentCount > 0) {
        const WKJEventSenderCallbacks* cb = eventSenderCallbacks();
        if (cb && cb->leap_forward) {
            cb->leap_forward(getEventSender(object),
                    (int32_t) getNumber(context, arguments[0], exception));
        }
    }
    return JSValueMakeUndefined(context);
}

static JSValueRef contextClickCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    const WKJEventSenderCallbacks* cb = eventSenderCallbacks();
    if (cb && cb->context_click)
        cb->context_click(getEventSender(object));
    return JSValueMakeUndefined(context);
}

static JSValueRef scheduleAsynchronousClickCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    const WKJEventSenderCallbacks* cb = eventSenderCallbacks();
    if (cb && cb->schedule_asynchronous_click)
        cb->schedule_asynchronous_click(getEventSender(object));
    return JSValueMakeUndefined(context);
}

static JSValueRef touchStartCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    const WKJEventSenderCallbacks* cb = eventSenderCallbacks();
    if (cb && cb->touch_start)
        cb->touch_start(getEventSender(object));
    return JSValueMakeUndefined(context);
}

static JSValueRef touchCancelCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    const WKJEventSenderCallbacks* cb = eventSenderCallbacks();
    if (cb && cb->touch_cancel)
        cb->touch_cancel(getEventSender(object));
    return JSValueMakeUndefined(context);
}

static JSValueRef touchMoveCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    const WKJEventSenderCallbacks* cb = eventSenderCallbacks();
    if (cb && cb->touch_move)
        cb->touch_move(getEventSender(object));
    return JSValueMakeUndefined(context);
}

static JSValueRef touchEndCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    const WKJEventSenderCallbacks* cb = eventSenderCallbacks();
    if (cb && cb->touch_end)
        cb->touch_end(getEventSender(object));
    return JSValueMakeUndefined(context);
}

static JSValueRef addTouchPointCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    if (argumentCount > 1) {
        const WKJEventSenderCallbacks* cb = eventSenderCallbacks();
        if (cb && cb->add_touch_point) {
            cb->add_touch_point(getEventSender(object),
                    (int32_t) getNumber(context, arguments[0], exception),
                    (int32_t) getNumber(context, arguments[1], exception));
        }
    }
    return JSValueMakeUndefined(context);
}

static JSValueRef updateTouchPointCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    if (argumentCount > 2) {
        const WKJEventSenderCallbacks* cb = eventSenderCallbacks();
        if (cb && cb->update_touch_point) {
            cb->update_touch_point(getEventSender(object),
                    (int32_t) getNumber(context, arguments[0], exception),
                    (int32_t) getNumber(context, arguments[1], exception),
                    (int32_t) getNumber(context, arguments[2], exception));
        }
    }
    return JSValueMakeUndefined(context);
}

static JSValueRef cancelTouchPointCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    if (argumentCount > 0) {
        const WKJEventSenderCallbacks* cb = eventSenderCallbacks();
        if (cb && cb->cancel_touch_point) {
            cb->cancel_touch_point(getEventSender(object),
                    (int32_t) getNumber(context, arguments[0], exception));
        }
    }
    return JSValueMakeUndefined(context);
}

static JSValueRef releaseTouchPointCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    if (argumentCount > 0) {
        const WKJEventSenderCallbacks* cb = eventSenderCallbacks();
        if (cb && cb->release_touch_point) {
            cb->release_touch_point(getEventSender(object),
                    (int32_t) getNumber(context, arguments[0], exception));
        }
    }
    return JSValueMakeUndefined(context);
}

static JSValueRef clearTouchPointsCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    const WKJEventSenderCallbacks* cb = eventSenderCallbacks();
    if (cb && cb->clear_touch_points)
        cb->clear_touch_points(getEventSender(object));
    return JSValueMakeUndefined(context);
}

static JSValueRef setTouchModifierCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    if (argumentCount > 1) {
        JSStringRef string = JSValueToStringCopy(context, arguments[0], exception);
        ASSERT(!exception || !*exception);

        int32_t modifier = 0;
        if (JSStringIsEqualToUTF8CString(string, "alt")) {
            modifier = 1; // com.sun.javafx.webkit.drt.EventSender.ALT
        }
        else if (JSStringIsEqualToUTF8CString(string, "ctrl")) {
            modifier = 2; // com.sun.javafx.webkit.drt.EventSender.CTRL
        }
        else if (JSStringIsEqualToUTF8CString(string, "meta")) {
            modifier = 4; // com.sun.javafx.webkit.drt.EventSender.META
        }
        else if (JSStringIsEqualToUTF8CString(string, "shift")) {
            modifier = 8; // com.sun.javafx.webkit.drt.EventSender.SHIFT
        }
        JSStringRelease(string);

        const WKJEventSenderCallbacks* cb = eventSenderCallbacks();
        if (cb && cb->set_touch_modifier) {
            cb->set_touch_modifier(getEventSender(object), modifier,
                    JSValueToBoolean(context, arguments[1]) ? 1 : 0);
        }
    }
    return JSValueMakeUndefined(context);
}

static JSValueRef scalePageByCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    if (argumentCount > 2) {
        const WKJEventSenderCallbacks* cb = eventSenderCallbacks();
        if (cb && cb->scale_page_by) {
            cb->scale_page_by(getEventSender(object),
                    (float) getNumber(context, arguments[0], exception),
                    (int32_t) getNumber(context, arguments[1], exception),
                    (int32_t) getNumber(context, arguments[2], exception));
        }
    }
    return JSValueMakeUndefined(context);
}

static void callZoom(JSObjectRef object, int32_t in, int32_t textOnly)
{
    const WKJEventSenderCallbacks* cb = eventSenderCallbacks();
    if (cb && cb->zoom)
        cb->zoom(getEventSender(object), in, textOnly);
}

static JSValueRef zoomPageInCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    callZoom(object, 1, 0);
    return JSValueMakeUndefined(context);
}

static JSValueRef zoomPageOutCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    callZoom(object, 0, 0);
    return JSValueMakeUndefined(context);
}

static JSValueRef textZoomInCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    callZoom(object, 1, 1);
    return JSValueMakeUndefined(context);
}

static JSValueRef textZoomOutCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    callZoom(object, 0, 1);
    return JSValueMakeUndefined(context);
}

static JSValueRef clearKillRingCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    return JSValueMakeUndefined(context);
}

static JSValueRef beginDragWithFilesCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    if (argumentCount > 0) {
        JSObjectRef array = JSValueToObject(context, arguments[0], exception);
        ASSERT(!exception || !*exception);

        int length = (int) getNumber(context, getProperty(context, array, "length", exception), exception);
        // The JNI implementation passed this straight to NewObjectArray, which threw (and
        // then dereferenced null) for a negative length. Clamping keeps a malformed test
        // from taking the harness down; a well-formed one never reaches it.
        if (length < 0)
            length = 0;

        std::vector<JSStringRef> strings;
        std::vector<const uint16_t*> files;
        std::vector<int32_t> fileLengths;
        strings.reserve(length);
        files.reserve(length);
        fileLengths.reserve(length);

        for (int i = 0; i < length; i++) {
            JSStringRef file = copyString(context,
                    getValueAt(context, array, i, exception), exception);
            int32_t fileLength = 0;
            const uint16_t* characters = JSStringRef_to_utf16(file, &fileLength);
            strings.push_back(file);
            files.push_back(characters);
            fileLengths.push_back(fileLength);
        }

        const WKJEventSenderCallbacks* cb = eventSenderCallbacks();
        if (cb && cb->begin_drag_with_files) {
            cb->begin_drag_with_files(getEventSender(object), files.data(),
                    fileLengths.data(), (int32_t) length);
        }

        for (JSStringRef file : strings)
            JSStringRelease(file);
    }
    return JSValueMakeUndefined(context);
}

static JSValueRef getDragModeCallback(JSContextRef context,
        JSObjectRef object, JSStringRef propertyName,
        JSValueRef* exception)
{
    const WKJEventSenderCallbacks* cb = eventSenderCallbacks();
    const int32_t dragMode = (cb && cb->get_drag_mode)
            ? cb->get_drag_mode(getEventSender(object)) : 0;
    return JSValueMakeBoolean(context, dragMode != 0);
}

static bool setDragModeCallback(JSContextRef context,
        JSObjectRef object, JSStringRef propertyName,
        JSValueRef value, JSValueRef* exception)
{
    const WKJEventSenderCallbacks* cb = eventSenderCallbacks();
    if (cb && cb->set_drag_mode) {
        cb->set_drag_mode(getEventSender(object),
                JSValueToBoolean(context, value) ? 1 : 0);
    }
    return true;
}

static void finalizeCallback(JSObjectRef object)
{
    wkj_ref* eventSender = static_cast<wkj_ref*>(JSObjectGetPrivate(object));
    if (!eventSender)
        return;
    // The counterpart of the DeleteGlobalRef that ~JGObject performed.
    if (drt_host && drt_host->core.release)
        drt_host->core.release(*eventSender);
    delete eventSender;
}

void makeEventSender(JSContextRef context, JSObjectRef windowObject,
        wkj_ref eventSender, JSValueRef* exception)
{
    static JSStaticValue staticValues[] = {
        { "dragMode", getDragModeCallback, setDragModeCallback, kJSPropertyAttributeNone },
        { 0, 0, 0, 0 }
    };
    static const auto attribute = kJSPropertyAttributeReadOnly | kJSPropertyAttributeDontDelete;
    static JSStaticFunction staticFunctions[] = {
        { "keyDown",                   keyDownCallback,                   attribute },
        { "mouseDown",                 mouseDownCallback,                 attribute },
        { "mouseUp",                   mouseUpCallback,                   attribute },
        { "mouseMoveTo",               mouseMoveToCallback,               attribute },
        { "mouseScrollBy",             mouseScrollByCallback,             attribute },
        { "continuousMouseScrollBy",   continuousMouseScrollByCallback,   attribute },
        { "leapForward",               leapForwardCallback,               attribute },
        { "contextClick",              contextClickCallback,              attribute },
        { "scheduleAsynchronousClick", scheduleAsynchronousClickCallback, attribute },
        { "touchStart",                touchStartCallback,                attribute },
        { "touchCancel",               touchCancelCallback,               attribute },
        { "touchMove",                 touchMoveCallback,                 attribute },
        { "touchEnd",                  touchEndCallback,                  attribute },
        { "addTouchPoint",             addTouchPointCallback,             attribute },
        { "updateTouchPoint",          updateTouchPointCallback,          attribute },
        { "cancelTouchPoint",          cancelTouchPointCallback,          attribute },
        { "releaseTouchPoint",         releaseTouchPointCallback,         attribute },
        { "clearTouchPoints",          clearTouchPointsCallback,          attribute },
        { "setTouchModifier",          setTouchModifierCallback,          attribute },
        { "scalePageBy",               scalePageByCallback,               attribute },
        { "zoomPageIn",                zoomPageInCallback,                attribute },
        { "zoomPageOut",               zoomPageOutCallback,               attribute },
        { "textZoomIn",                textZoomInCallback,                attribute },
        { "textZoomOut",               textZoomOutCallback,               attribute },
        { "clearKillRing",             clearKillRingCallback,             attribute },
        { "beginDragWithFiles",        beginDragWithFilesCallback,        attribute },
        { 0, 0, 0 }
    };
    static JSClassDefinition classDefinition = {
        0, kJSClassAttributeNone, "EventSender",
        0, staticValues, staticFunctions,
        0, finalizeCallback,
        0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    // The NewGlobalRef that the JGObject constructor used to take. With no retain slot the
    // id is stored as it arrived, so a host that installs none must keep the EventSender
    // reachable itself.
    wkj_ref retained = eventSender;
    if (drt_host && drt_host->core.retain)
        retained = drt_host->core.retain(eventSender);

    JSClassRef eventSenderClass = JSClassCreate(&classDefinition);
    JSValueRef jsEventSender = JSObjectMake(context, eventSenderClass, new wkj_ref(retained));
    JSClassRelease(eventSenderClass);

    JSStringRef propName = JSStringCreateWithUTF8CString("eventSender");
    JSObjectSetProperty(context, windowObject, propName, jsEventSender, attribute, exception);
    JSStringRelease(propName);
    ASSERT(!exception || !*exception);
}
