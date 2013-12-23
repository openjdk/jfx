/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"
#include "EventSender.h"

#include "JavaEnv.h"
#include <JavaScriptCore/API/JSStringRef.h>

static jmethodID keyDownMID;
static jmethodID mouseUpDownMID;
static jmethodID mouseMoveToMID;
static jmethodID mouseScrollMID;
static jmethodID leapForwardMID;
static jmethodID contextClickMID;
static jmethodID scheduleAsynchronousClickMID;
static jmethodID touchStartMID;
static jmethodID touchCancelMID;
static jmethodID touchMoveMID;
static jmethodID touchEndMID;
static jmethodID addTouchPointMID;
static jmethodID updateTouchPointMID;
static jmethodID cancelTouchPointMID;
static jmethodID releaseTouchPointMID;
static jmethodID clearTouchPointsMID;
static jmethodID setTouchModifierMID;
static jmethodID scalePageByMID;
static jmethodID zoomMID;
static jmethodID beginDragWithFilesMID;
static jmethodID getDragModeMID;
static jmethodID setDragModeMID;

static JGObject* getEventSender(JSObjectRef object)
{
    JGObject* result = static_cast<JGObject*>(JSObjectGetPrivate(object));
    ASSERT(result);
    return result;
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

static jstring getJString(JSContextRef context, JSValueRef value, JSValueRef* exception)
{
    JSStringRef string = JSValueToStringCopy(context, value, exception);
    ASSERT(!exception || !*exception);

    const JSChar* chars = JSStringGetCharactersPtr(string);
    JNIEnv* env = DumpRenderTree_GetJavaEnv();
    jstring result = env->NewString((const jchar*) chars, JSStringGetLength(string));
    JSStringRelease(string);
    return result;
}

static jint getModifier(JSContextRef context, const JSValueRef value, JSValueRef* exception)
{
    jint modifier = 0;
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
    JSStringRelease(string);
    return modifier;
}

static jint getModifers(JSContextRef context, const JSValueRef value, JSValueRef* exception)
{
    // The value may either be a string with a single modifier or an array of modifiers.
    if (JSValueIsString(context, value))
        return getModifier(context, value, exception);

    JSObjectRef array = JSValueToObject(context, value, 0);
    if (!array)
        return 0;

    jint modifiers = 0;
    int length = (int) getNumber(context, getProperty(context, array, "length", exception), exception);
    for (int i = 0; i < length; i++) {
        modifiers |= getModifier(context, getValueAt(context, array, i, exception), exception);
    }
    return modifiers;
}

static void call(JSObjectRef object, jmethodID method, ...)
{
    JGObject* eventSender = getEventSender(object);
    JNIEnv* env = DumpRenderTree_GetJavaEnv();

    va_list args;
    va_start(args, method);
    env->CallVoidMethodV(*eventSender, method, args);
    va_end(args);

    CheckAndClearException(env);
}

static JSValueRef handleMouseScroll(JSContextRef context, bool continuous,
        JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    if (argumentCount > 1) {
        call(object, mouseScrollMID,
                (jfloat) getNumber(context, arguments[0], exception),
                (jfloat) getNumber(context, arguments[1], exception),
                bool_to_jbool(continuous));
    }
    return JSValueMakeUndefined(context);
}

static JSValueRef handleMouseUpDown(JSContextRef context, bool pressed,
        JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    jint button = 1; // com.sun.webkit.event.WCMouseEvent.BUTTON1 (left)
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
    jint modifiers = pressed ? 16 : 0; // com.sun.javafx.webkit.drt.EventSender.PRESSED
    if (argumentCount > 1) {
        modifiers |= getModifers(context, arguments[1], exception);
    }
    call(object, mouseUpDownMID, button, modifiers);
    return JSValueMakeUndefined(context);
}

static JSValueRef keyDownCallback(JSContextRef context, JSObjectRef function,
        JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    if (argumentCount > 0) {
        jint modifiers = 0;
        if (argumentCount > 1) {
            modifiers |= getModifers(context, arguments[1], exception);
        }
        call(object, keyDownMID,
                getJString(context, arguments[0], exception),
                modifiers);
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
        call(object, mouseMoveToMID,
                (jint) getNumber(context, arguments[0], exception),
                (jint) getNumber(context, arguments[1], exception));
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
        call(object, leapForwardMID,
                (jint) getNumber(context, arguments[0], exception));
    }
    return JSValueMakeUndefined(context);
}

static JSValueRef contextClickCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    call(object, contextClickMID);
    return JSValueMakeUndefined(context);
}

static JSValueRef scheduleAsynchronousClickCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    call(object, scheduleAsynchronousClickMID);
    return JSValueMakeUndefined(context);
}

static JSValueRef touchStartCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    call(object, touchStartMID);
    return JSValueMakeUndefined(context);
}

static JSValueRef touchCancelCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    call(object, touchCancelMID);
    return JSValueMakeUndefined(context);
}

static JSValueRef touchMoveCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    call(object, touchMoveMID);
    return JSValueMakeUndefined(context);
}

static JSValueRef touchEndCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    call(object, touchEndMID);
    return JSValueMakeUndefined(context);
}

static JSValueRef addTouchPointCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    if (argumentCount > 1) {
        call(object, addTouchPointMID,
                (jint) getNumber(context, arguments[0], exception),
                (jint) getNumber(context, arguments[1], exception));
    }
    return JSValueMakeUndefined(context);
}

static JSValueRef updateTouchPointCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    if (argumentCount > 2) {
        call(object, updateTouchPointMID,
                (jint) getNumber(context, arguments[0], exception),
                (jint) getNumber(context, arguments[1], exception),
                (jint) getNumber(context, arguments[2], exception));
    }
    return JSValueMakeUndefined(context);
}

static JSValueRef cancelTouchPointCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    if (argumentCount > 0) {
        call(object, cancelTouchPointMID,
                (jint) getNumber(context, arguments[0], exception));
    }
    return JSValueMakeUndefined(context);
}

static JSValueRef releaseTouchPointCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    if (argumentCount > 0) {
        call(object, releaseTouchPointMID,
                (jint) getNumber(context, arguments[0], exception));
    }
    return JSValueMakeUndefined(context);
}

static JSValueRef clearTouchPointsCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    call(object, clearTouchPointsMID);
    return JSValueMakeUndefined(context);
}

static JSValueRef setTouchModifierCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    if (argumentCount > 1) {
        JSStringRef string = JSValueToStringCopy(context, arguments[0], exception);
        ASSERT(!exception || !*exception);
        
        jint modifier = 0;
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
        
        call(object, setTouchModifierMID,
                modifier,
                bool_to_jbool(JSValueToBoolean(context, arguments[1])));
    }
    return JSValueMakeUndefined(context);
}

static JSValueRef scalePageByCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    if (argumentCount > 2) {
        call(object, scalePageByMID,
                (jfloat) getNumber(context, arguments[0], exception),
                (jint) getNumber(context, arguments[1], exception),
                (jint) getNumber(context, arguments[2], exception));
    }
    return JSValueMakeUndefined(context);
}

static JSValueRef zoomPageInCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    call(object, zoomMID, JNI_TRUE, JNI_FALSE);
    return JSValueMakeUndefined(context);
}

static JSValueRef zoomPageOutCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    call(object, zoomMID, JNI_FALSE, JNI_FALSE);
    return JSValueMakeUndefined(context);
}

static JSValueRef textZoomInCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    call(object, zoomMID, JNI_TRUE, JNI_TRUE);
    return JSValueMakeUndefined(context);
}

static JSValueRef textZoomOutCallback(JSContextRef context,
        JSObjectRef function, JSObjectRef object, size_t argumentCount,
        const JSValueRef arguments[], JSValueRef* exception)
{
    call(object, zoomMID, JNI_FALSE, JNI_TRUE);
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

        JNIEnv* env = DumpRenderTree_GetJavaEnv();
        JGClass stringClass = JLClass(env->FindClass("java/lang/String"));
        jobjectArray stringArray = (jobjectArray) env->NewObjectArray(length, stringClass, 0);
        CheckAndClearException(env);

        for (int i = 0; i < length; i++) {
            env->SetObjectArrayElement(stringArray, i, getJString(context,
                    getValueAt(context, array, i, exception), exception));
        }
        call(object, beginDragWithFilesMID, stringArray);
        env->DeleteLocalRef(stringArray);
    }
    return JSValueMakeUndefined(context);
}

static JSValueRef getDragModeCallback(JSContextRef context,
        JSObjectRef object, JSStringRef propertyName,
        JSValueRef* exception)
{
    JNIEnv* env = DumpRenderTree_GetJavaEnv();
    JGObject* eventSender = getEventSender(object);
    return JSValueMakeBoolean(context,
            jbool_to_bool(env->CallBooleanMethod(*eventSender, getDragModeMID)));
}

static bool setDragModeCallback(JSContextRef context,
        JSObjectRef object, JSStringRef propertyName,
        JSValueRef value, JSValueRef* exception)
{
    call(object, setDragModeMID,
            bool_to_jbool(JSValueToBoolean(context, value)));
    return true;
}

static void finalizeCallback(JSObjectRef object)
{
    JGObject* eventSender = getEventSender(object);
    delete eventSender;
}

void makeEventSender(JSContextRef context, JSObjectRef windowObject,
        const JLObject& eventSender, JSValueRef* exception)
{
    static JGClass javaClass = 0;
    if (!javaClass) {
        JNIEnv* env = DumpRenderTree_GetJavaEnv();

        javaClass = JLClass(env->FindClass("com/sun/javafx/webkit/drt/EventSender"));
        ASSERT(javaClass);

        keyDownMID = env->GetMethodID(javaClass, "keyDown", "(Ljava/lang/String;I)V");
        ASSERT(keyDownMID);

        mouseUpDownMID = env->GetMethodID(javaClass, "mouseUpDown", "(II)V");
        ASSERT(mouseUpDownMID);

        mouseMoveToMID = env->GetMethodID(javaClass, "mouseMoveTo", "(II)V");
        ASSERT(mouseMoveToMID);

        mouseScrollMID = env->GetMethodID(javaClass, "mouseScroll", "(FFZ)V");
        ASSERT(mouseScrollMID);

        leapForwardMID = env->GetMethodID(javaClass, "leapForward", "(I)V");
        ASSERT(leapForwardMID);

        contextClickMID = env->GetMethodID(javaClass, "contextClick", "()V");
        ASSERT(contextClickMID);

        scheduleAsynchronousClickMID = env->GetMethodID(javaClass, "scheduleAsynchronousClick", "()V");
        ASSERT(scheduleAsynchronousClickMID);

        touchStartMID = env->GetMethodID(javaClass, "touchStart", "()V");
        ASSERT(touchStartMID);

        touchCancelMID = env->GetMethodID(javaClass, "touchCancel", "()V");
        ASSERT(touchCancelMID);

        touchMoveMID = env->GetMethodID(javaClass, "touchMove", "()V");
        ASSERT(touchMoveMID);

        touchEndMID = env->GetMethodID(javaClass, "touchEnd", "()V");
        ASSERT(touchEndMID);

        addTouchPointMID = env->GetMethodID(javaClass, "addTouchPoint", "(II)V");
        ASSERT(addTouchPointMID);

        updateTouchPointMID = env->GetMethodID(javaClass, "updateTouchPoint", "(III)V");
        ASSERT(updateTouchPointMID);

        cancelTouchPointMID = env->GetMethodID(javaClass, "cancelTouchPoint", "(I)V");
        ASSERT(cancelTouchPointMID);

        releaseTouchPointMID = env->GetMethodID(javaClass, "releaseTouchPoint", "(I)V");
        ASSERT(releaseTouchPointMID);

        clearTouchPointsMID = env->GetMethodID(javaClass, "clearTouchPoints", "()V");
        ASSERT(clearTouchPointsMID);

        setTouchModifierMID = env->GetMethodID(javaClass, "setTouchModifier", "(IZ)V");
        ASSERT(setTouchModifierMID);

        scalePageByMID = env->GetMethodID(javaClass, "scalePageBy", "(FII)V");
        ASSERT(scalePageByMID);

        zoomMID = env->GetMethodID(javaClass, "zoom", "(ZZ)V");
        ASSERT(zoomMID);

        beginDragWithFilesMID = env->GetMethodID(javaClass, "beginDragWithFiles", "([Ljava/lang/String;)V");
        ASSERT(beginDragWithFilesMID);

        getDragModeMID = env->GetMethodID(javaClass, "getDragMode", "()Z");
        ASSERT(getDragModeMID);

        setDragModeMID = env->GetMethodID(javaClass, "setDragMode", "(Z)V");
        ASSERT(setDragModeMID);
    }
    static JSStaticValue staticValues[] = {
        { "dragMode", getDragModeCallback, setDragModeCallback, kJSPropertyAttributeNone },
        { 0, 0, 0, 0 }
    };
    static int attribute = kJSPropertyAttributeReadOnly | kJSPropertyAttributeDontDelete;
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
    JSClassRef eventSenderClass = JSClassCreate(&classDefinition);
    JSValueRef jsEventSender = JSObjectMake(context, eventSenderClass, new JGObject(eventSender));
    JSClassRelease(eventSenderClass);

    JSStringRef propName = JSStringCreateWithUTF8CString("eventSender");
    JSObjectSetProperty(context, windowObject, propName, jsEventSender, attribute, exception);
    JSStringRelease(propName);
    ASSERT(!exception || !*exception);
}
