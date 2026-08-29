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
#include <wkj_constants.h>

#include "BridgeUtils.h"

#include <cstring>

#include <JavaScriptCore/CallFrame.h>
#include <JavaScriptCore/Identifier.h>

#include "Document.h"
#include "Frame.h"
#include "JavaInstanceJSC.h"
#include "JavaArrayJSC.h"
#include "JavaRuntimeObject.h"
#include "JNIUtilityPrivate.h"
#include "JSDOMBinding.h"
#include "JSDOMGlobalObject.h"
#include "JSExecState.h"
#include "JSNode.h"
#include "NodeDocument.h"
#include "ScriptController.h"
#include "WKJDOMUtils.h"
#include "runtime_array.h"
#include "runtime_object.h"
#include "runtime_root.h"
#include <wtf/Vector.h>
#include <wtf/text/WTFString.h>
#include <JavaScriptCore/JSArray.h>
#include <JavaScriptCore/JSLock.h>
#include <JavaScriptCore/APICast.h>
#include <JavaScriptCore/OpaqueJSString.h>
#include <JavaScriptCore/JSBase.h>
#include <JavaScriptCore/JSStringRef.h>

/*
 * Everything in this anonymous namespace carries a wkjBridge prefix on purpose: WebCore
 * builds these sources unified, so several .cpp files share one translation unit and
 * therefore one anonymous namespace.
 */
namespace {

/*
 * Copies the first `count` code units of `value` into `destination` as UTF-16, with no NUL
 * terminator.
 *
 * The Latin-1 branch is required, not an optimisation: StringImpl::span16() asserts
 * !is8Bit(), and with the assert compiled out it reads length() bytes past the end of the
 * allocation.
 */
void wkjBridgeCopyToUTF16(const WTF::String& value, uint16_t* destination, unsigned count)
{
    if (!count)
        return;

    if (value.is8Bit()) {
        auto characters = value.span8();
        for (unsigned i = 0; i < count; ++i)
            destination[i] = characters[i];
    } else {
        auto characters = value.span16();
        std::memcpy(destination, characters.data(), count * sizeof(char16_t));
    }
}

/*
 * Clears the described value, leaving the string buffer of the caller alone: `string` and
 * `string_cap` belong to whoever allocated them and are inputs, not outputs.
 */
void wkjBridgeResetValue(WKJJSValue* out)
{
    if (!out)
        return;
    out->kind = WKJ_JS_KIND_NULL;
    out->peer_type = 0;
    out->number = 0;
    out->peer = 0;
    out->string_handle = 0;
    out->object = 0;
    out->string_length = 0;
}

/*
 * Puts a string into the described value: into the buffer of the caller when it fits, and
 * otherwise into a retained JavaScript string whose address goes back as `string_handle`.
 *
 * The handle is what keeps the entry points free of a retry that would re-run script. The
 * DOM half of the ABI answers WKJ_STR_OVERFLOW and lets the facade call again, which is
 * safe for a DOM getter; calling wkj_js_eval or wkj_js_call twice is not, and neither is
 * calling wkj_js_get_member twice when the member is a user-defined getter.
 */
void wkjBridgeSetString(WKJJSValue* out, const WTF::String& value)
{
    out->kind = WKJ_JS_KIND_STRING;
    out->string_handle = 0;

    const unsigned length = value.length();
    out->string_length = static_cast<int32_t>(length);

    if (!length)
        return;

    if (out->string && out->string_cap >= static_cast<int32_t>(length)) {
        wkjBridgeCopyToUTF16(value, out->string, length);
        return;
    }

    Vector<char16_t> characters(length);
    wkjBridgeCopyToUTF16(value, reinterpret_cast<uint16_t*>(characters.mutableSpan().data()),
        length);
    out->string_handle = wkj_from_ptr(JSStringCreateWithCharacters(
        reinterpret_cast<const JSChar*>(characters.span().data()), length));
}

/*
 * Asks Java to describe one object, growing the buffer once if its string did not fit.
 * Describing is side-effect free, so a second call is harmless - which is exactly why the
 * argument direction can use the plain overflow-and-retry rule that a result cannot.
 */
void wkjBridgeDescribeObject(wkj_ref object, WKJJSValue* out, Vector<char16_t>& storage)
{
    wkjBridgeResetValue(out);

    const WKJLiveConnectHost* host = wkj_live_connect_host;
    if (!object || !host || !host->describe_object)
        return;

    if (host->describe_object(object, out) != WKJ_STR_OVERFLOW)
        return;

    storage.grow(static_cast<size_t>(out->string_length));
    out->string = reinterpret_cast<uint16_t*>(storage.mutableSpan().data());
    out->string_cap = out->string_length;
    host->describe_object(object, out);
}

} // namespace

namespace WebCore {

JSGlobalContextRef getGlobalContext(WebCore::ScriptController* scriptController)
{
    return toGlobalRef(scriptController->globalObject(WebCore::mainThreadNormalWorldSingleton()));
}

JSStringRef asJSStringRef(const uint16_t* s, int32_t length)
{
    return JSStringCreateWithCharacters(reinterpret_cast<const JSChar*>(s),
        static_cast<size_t>(length < 0 ? 0 : length));
}

JSValueRef WKJValueToJSValue(
    JSContextRef ctx,
    JSC::Bindings::RootObject* rootObject,
    const WKJJSValue& value,
    wkj_ref accessControlContext)
{
    if (value.kind == WKJ_JS_KIND_NULL)
        return JSValueMakeNull(ctx);

    JSC::JSGlobalObject* lexicalGlobalObject = toJS(ctx);
    JSC::JSLockHolder lock(lexicalGlobalObject);

    switch (value.kind) {
    case WKJ_JS_KIND_JS_OBJECT:
        /*
         * The value is a com.sun.webkit.dom.JSObject, and Java has read its own peer and
         * peer_type fields - the two reads that used to be a pair of cached field ids here.
         */
        switch (value.peer_type) {
        case com_sun_webkit_dom_JSObject_JS_CONTEXT_OBJECT:
            return static_cast<JSObjectRef>(wkj_to_ptr(value.peer));
        case com_sun_webkit_dom_JSObject_JS_DOM_NODE_OBJECT:
        case com_sun_webkit_dom_JSObject_JS_DOM_WINDOW_OBJECT:
            {
                JSDOMGlobalObject* globalObject = toJSDOMGlobalObject(
                    ((value.peer_type == com_sun_webkit_dom_JSObject_JS_DOM_WINDOW_OBJECT)
                        ? *static_cast<LocalDOMWindow*>(wkj_to_ptr(value.peer))->document()
                        : static_cast<Node*>(wkj_to_ptr(value.peer))->document()),
                    normalWorld(lexicalGlobalObject->vm()));
                return toRef(lexicalGlobalObject,
                    (value.peer_type == com_sun_webkit_dom_JSObject_JS_DOM_WINDOW_OBJECT)
                        ? WebCore::toJS(lexicalGlobalObject, globalObject, *static_cast<DOMWindow*>(wkj_to_ptr(value.peer)))
                        : WebCore::toJS(lexicalGlobalObject, globalObject, *static_cast<Node*>(wkj_to_ptr(value.peer))));
            }
        default:
            /* Not reachable: the three peer types are the only values the field can hold. */
            return JSValueMakeNull(ctx);
        }

    case WKJ_JS_KIND_STRING:
        {
            JSStringRef string = asJSStringRef(value.string, value.string_length);
            JSValueRef jsvalue = JSValueMakeString(ctx, string);
            JSStringRelease(string);
            return jsvalue;
        }

    case WKJ_JS_KIND_BOOLEAN:
        return JSValueMakeBoolean(ctx, value.number != 0);

    case WKJ_JS_KIND_INT:
    case WKJ_JS_KIND_DOUBLE:
        return JSValueMakeNumber(ctx, value.number);

    case WKJ_JS_KIND_UNDEFINED:
        /* Not produced on the way in; the Java undefined is a plain String there. */
        return JSValueMakeUndefined(ctx);

    case WKJ_JS_KIND_JAVA_OBJECT:
        break;

    default:
        return JSValueMakeNull(ctx);
    }

    /*
     * An ordinary Java object. An array becomes a RuntimeArray and everything else, including
     * java.lang.Character, is wrapped in a JavaInstance - the same two outcomes as before,
     * reached through the same three questions (getClass, isArray, getName) that are now
     * three host slots.
     */
    WKJHandle valClass = JSC::Bindings::javaObjectClass(value.object);
    if (JSC::Bindings::javaClassIsArray(valClass.get())) {
        String className = JSC::Bindings::javaClassName(valClass.get());
        /*
         * A null name reached GetStringUTFChars unchecked before, which dereferenced it.
         * An empty name is not an array descriptor, so convertJObjectToArray returns
         * undefined for it, which is the closest defined behaviour to what was intended.
         */
        CString classNameC = className.isNull() ? CString("") : className.utf8();
        JSC::JSValue arr = JSC::Bindings::JavaArray::convertJObjectToArray(lexicalGlobalObject,
            value.object, classNameC.data(), rootObject, accessControlContext);
        return toRef(lexicalGlobalObject, arr);
    }

    // All other Java Object types including java.lang.Character will be wrapped inside JavaInstance.
    RefPtr<JSC::Bindings::JavaInstance> jinstance = JSC::Bindings::JavaInstance::create(
        value.object, rootObject, accessControlContext);
    return toRef(jinstance->createRuntimeObject(lexicalGlobalObject));
}

JSValueRef Java_Object_to_JSValue(
    JSContextRef ctx,
    JSC::Bindings::RootObject* rootObject,
    wkj_ref val,
    wkj_ref accessControlContext)
{
    if (!val)
        return JSValueMakeNull(ctx);

    /*
     * The JavaScript lock is taken here, before Java is asked anything, because that is where
     * this function took it: everything the instanceof chain called - booleanValue,
     * doubleValue, getClass - ran with the lock held, and one of them can be an application
     * override. JSLockHolder is recursive, so the one WKJValueToJSValue takes below nests.
     */
    JSC::JSGlobalObject* lexicalGlobalObject = toJS(ctx);
    JSC::JSLockHolder lock(lexicalGlobalObject);

    /*
     * The chain of instanceof tests this function used to open with - JSObject, String,
     * Boolean, Number, then getClass().isArray() - is one callback now, and it is the same
     * one the Java side uses to describe the arguments of setMember, setSlot and call.
     */
    char16_t inlineBuffer[256];
    Vector<char16_t> grownBuffer;
    WKJJSValue described;
    std::memset(&described, 0, sizeof(described));
    described.string = reinterpret_cast<uint16_t*>(inlineBuffer);
    described.string_cap = static_cast<int32_t>(sizeof(inlineBuffer) / sizeof(inlineBuffer[0]));
    wkjBridgeDescribeObject(val, &described, grownBuffer);

    JSValueRef result = WKJValueToJSValue(ctx, rootObject, described, accessControlContext);

    /*
     * describe_object minted a strong id for a plain Java object and this frame owns it. It
     * is released here, after the JavaInstance has taken its own weak reference - which is
     * the lifetime the JNI code gave the same object through a local reference.
     */
    if (described.kind == WKJ_JS_KIND_JAVA_OBJECT)
        WKJRelease(described.object);

    return result;
}

void JSValueToWKJValue(
    JSValueRef value,
    JSContextRef ctx,
    JSC::Bindings::RootObject* rootObject,
    WKJJSValue* out)
{
    wkjBridgeResetValue(out);

    JSC::JSGlobalObject* globalObject = toJS(ctx);
    JSC::JSLockHolder lock(globalObject);
    JSC::JSValue jsValue = toJS(globalObject, value);

    /*
     * This is convertValueToJValue for the target type java.lang.Object, which is what all
     * nine entry points asked for, with one difference: it describes the value instead of
     * building the Java object. The order of the tests, and every outcome, is unchanged.
     */
    if (jsValue.isObject()) {
        JSC::JSObject* object = JSC::asObject(jsValue);

        if (object->inherits(JSC::Bindings::JavaRuntimeObject::info())) {
            // Unwrap a Java instance.
            JSC::Bindings::JavaRuntimeObject* runtimeObject
                = static_cast<JSC::Bindings::JavaRuntimeObject*>(object);
            JSC::Bindings::JavaInstance* instance = runtimeObject->getInternalJavaInstance();
            if (instance) {
                // Since instance->javaInstance() is a weak reference, taking a strong one to safeguard it from GC
                WKJHandle live = WKJHandle::retained(instance->javaInstance());
                if (!live) {
                    LOG_ERROR("Could not get javaInstance for %llu in JSValueToWKJValue",
                        static_cast<unsigned long long>(instance->javaInstance()));
                    return;
                }
                out->kind = WKJ_JS_KIND_JAVA_OBJECT;
                out->object = live.leakRef();
            }
            return;
        }

        if (object->classInfo() == JSC::RuntimeArray::info()) {
            // Input is a JavaScript Array that was originally created from a Java Array
            JSC::RuntimeArray* imp = static_cast<JSC::RuntimeArray*>(object);
            JSC::Bindings::JavaArray* array
                = static_cast<JSC::Bindings::JavaArray*>(imp->getConcreteArray());

            // Since array->javaArray() is a weak reference, taking a strong one to safeguard it from GC
            WKJHandle live = WKJHandle::retained(array->javaArray());
            if (!live) {
                LOG_ERROR("Could not get javaArrayInstance for %llu in JSValueToWKJValue",
                    static_cast<unsigned long long>(array->javaArray()));
                return;
            }
            out->kind = WKJ_JS_KIND_JAVA_OBJECT;
            out->object = live.leakRef();
            return;
        }

        // Wrap objects in JSObject instances.
        if (object->inherits(WebCore::JSNode::info())) {
            WebCore::JSNode* jsnode = static_cast<WebCore::JSNode*>(object);
            WebCore::Node* peer = &jsnode->wrapped();
            peer->ref(); //deref is in NodeImpl disposer
            out->kind = WKJ_JS_KIND_DOM_NODE;
            out->peer = wkj_from_ptr(peer);
            return;
        }

        rootObject->gcProtect(object);
        out->kind = WKJ_JS_KIND_JS_OBJECT;
        out->peer = wkj_from_ptr(object);
        out->peer_type = com_sun_webkit_dom_JSObject_JS_CONTEXT_OBJECT;
        return;
    }

    if (jsValue.isString()) {
        String stringValue = JSC::asString(jsValue)->value(globalObject);
        wkjBridgeSetString(out, stringValue);
        return;
    }

    if (jsValue.isNumber()) {
        /*
         * int32 becomes an Integer and everything else a Double. Load bearing: an
         * application can tell the two apart, and the JNI code made the same split.
         */
        if (jsValue.isInt32()) {
            out->kind = WKJ_JS_KIND_INT;
            out->number = jsValue.asInt32();
        } else {
            out->kind = WKJ_JS_KIND_DOUBLE;
            out->number = jsValue.asNumber();
        }
        return;
    }

    if (jsValue.isBoolean()) {
        out->kind = WKJ_JS_KIND_BOOLEAN;
        out->number = jsValue.asBoolean() ? 1 : 0;
        return;
    }

    if (jsValue.isUndefined()) {
        out->kind = WKJ_JS_KIND_UNDEFINED;
        return;
    }

    /* JS null, and everything the conversion above left alone, is Java null. */
}

int32_t executeScript(
    JSObjectRef object,
    JSContextRef ctx,
    JSC::Bindings::RootObject* rootObject,
    const uint16_t* script,
    int32_t scriptLength,
    WKJJSValue* out)
{
    if (script == nullptr)
        return WKJ_JS_NULL_ARGUMENT;

    JSStringRef scriptString = asJSStringRef(script, scriptLength);
    JSValueRef exception = 0;
    JSValueRef value = JSEvaluateScript(ctx, scriptString, object, nullptr, 1, &exception);
    JSStringRelease(scriptString);
    if (exception) {
        /*
         * The thrown value goes back described, and Java raises
         * JSObject.fwkMakeException(value) - which is what throwJavaException did here with
         * the same value, one JNI Throw later.
         */
        JSValueToWKJValue(exception, ctx, rootObject, out);
        return WKJ_JS_EXCEPTION;
    }
    JSValueToWKJValue(value, ctx, rootObject, out);
    return WKJ_JS_OK;
}

}


RefPtr<JSC::Bindings::RootObject> checkJSPeer(
    int64_t peer,
    int32_t peer_type,
    JSObjectRef &object,
    JSContextRef &context)
{
    JSC::Bindings::RootObject *rootObject = nullptr;
    switch (peer_type) {
    case com_sun_webkit_dom_JSObject_JS_CONTEXT_OBJECT:
        {
            object = static_cast<JSObjectRef>(wkj_to_ptr(peer));
            rootObject = JSC::Bindings::findProtectingRootObject(reinterpret_cast<JSC::JSObject*>(object));
            if (rootObject) {
                context = toRef(rootObject->globalObject());
            }
        }
        break;
    case com_sun_webkit_dom_JSObject_JS_DOM_NODE_OBJECT:
    case com_sun_webkit_dom_JSObject_JS_DOM_WINDOW_OBJECT:
        {
            WebCore::Frame* frame = (peer_type == com_sun_webkit_dom_JSObject_JS_DOM_WINDOW_OBJECT)
                ? static_cast<WebCore::LocalDOMWindow*>(wkj_to_ptr(peer))->document()->frame()
                : static_cast<WebCore::Node*>(wkj_to_ptr(peer))->document().frame();

            if (!frame) {
                return rootObject;
            }

            auto* localFrame = dynamicDowncast<LocalFrame>(frame);
            rootObject = &(localFrame->script().createRootObject(frame).leakRef());
            if (rootObject) {
                context = WebCore::getGlobalContext(&localFrame->script());
                JSC::JSGlobalObject* JSGlobalObject = toJS(context);
                JSC::JSLockHolder lock(JSGlobalObject);

                object = const_cast<JSObjectRef>(toRef(JSGlobalObject,
                    (peer_type == com_sun_webkit_dom_JSObject_JS_DOM_WINDOW_OBJECT)
                    ? WebCore::toJS(JSGlobalObject, static_cast<WebCore::JSDOMGlobalObject *>(rootObject->globalObject()), *static_cast<WebCore::DOMWindow*>(wkj_to_ptr(peer)))
                    : WebCore::toJS(JSGlobalObject, static_cast<WebCore::JSDOMGlobalObject *>(rootObject->globalObject()), *static_cast<WebCore::Node*>(wkj_to_ptr(peer)))));

            }
        }
        break;
    };

    return rootObject;
}



extern "C" {

/*
 * The installed LiveConnect table, and its installer. This is the LiveConnect equivalent of
 * wkj_init: a table of its own, for a subsystem of its own, so that a use of the library
 * that never exposes a Java object to script never has to provide one.
 */
const WKJLiveConnectHost* wkj_live_connect_host = nullptr;

WKJ_EXPORT int32_t wkj_live_connect_init(const WKJLiveConnectHost* host, int32_t host_size,
    uint32_t abi_version)
{
    if (!host)
        return WKJ_INIT_ERR_NULL_HOST;
    if (abi_version != WKJ_ABI_VERSION)
        return WKJ_INIT_ERR_ABI_VERSION;
    if (host_size != static_cast<int32_t>(sizeof(WKJLiveConnectHost)) || host->size != host_size)
        return WKJ_INIT_ERR_HOST_SIZE;
    if (wkj_live_connect_host)
        return WKJ_INIT_ERR_ALREADY_INITED;

    wkj_live_connect_host = host;
    return WKJ_INIT_OK;
}

WKJ_EXPORT int32_t wkj_bridge_sizeof_java_value(void)
{
    return static_cast<int32_t>(sizeof(WKJJavaValue));
}

WKJ_EXPORT int32_t wkj_bridge_sizeof_js_value(void)
{
    return static_cast<int32_t>(sizeof(WKJJSValue));
}

WKJ_EXPORT int32_t wkj_bridge_sizeof_live_connect_host(void)
{
    return static_cast<int32_t>(sizeof(WKJLiveConnectHost));
}

WKJ_EXPORT int32_t wkj_js_eval(int64_t peer, int32_t peer_type,
    const uint16_t* script, int32_t script_length, WKJJSValue* out)
{
    WebCore::WKJCallScope wkjScope;
    wkjBridgeResetValue(out);

    if (script == nullptr)
        return WKJ_JS_NULL_ARGUMENT;

    JSObjectRef object = nullptr;
    JSContextRef ctx = nullptr;
    RefPtr<JSC::Bindings::RootObject> rootObject(checkJSPeer(peer, peer_type, object, ctx));
    if (rootObject.get() == nullptr)
        return WKJ_JS_NO_CONTEXT;

    return WebCore::executeScript(object, ctx, rootObject.get(), script, script_length, out);
}

WKJ_EXPORT int32_t wkj_js_get_member(int64_t peer, int32_t peer_type,
    const uint16_t* name, int32_t name_length, WKJJSValue* out)
{
    WebCore::WKJCallScope wkjScope;
    wkjBridgeResetValue(out);

    if (name == nullptr)
        return WKJ_JS_NULL_ARGUMENT;

    JSObjectRef object = nullptr;
    JSContextRef ctx = nullptr;
    RefPtr<JSC::Bindings::RootObject> rootObject(checkJSPeer(peer, peer_type, object, ctx));

    if (rootObject.get() == nullptr)
        return WKJ_JS_NO_CONTEXT;

    JSStringRef nameString = WebCore::asJSStringRef(name, name_length);
    JSValueRef value = JSObjectGetProperty(ctx, object, nameString, nullptr);
    JSStringRelease(nameString);
    WebCore::JSValueToWKJValue(value, ctx, rootObject.get(), out);
    return WKJ_JS_OK;
}

WKJ_EXPORT int32_t wkj_js_set_member(int64_t peer, int32_t peer_type,
    const uint16_t* name, int32_t name_length, const WKJJSValue* value, wkj_ref acc,
    WKJJSValue* out)
{
    WebCore::WKJCallScope wkjScope;
    wkjBridgeResetValue(out);

    if (name == nullptr)
        return WKJ_JS_NULL_ARGUMENT;

    JSObjectRef object = nullptr;
    JSContextRef ctx = nullptr;
    RefPtr<JSC::Bindings::RootObject> rootObject(checkJSPeer(peer, peer_type, object, ctx));
    if (rootObject.get() == nullptr)
        return WKJ_JS_NO_CONTEXT;

    /* A missing value struct is a Java null, which is what a null object reference was. */
    WKJJSValue nullValue;
    std::memset(&nullValue, 0, sizeof(nullValue));

    JSStringRef nameString = WebCore::asJSStringRef(name, name_length);
    JSValueRef jsvalue = WebCore::WKJValueToJSValue(ctx, rootObject.get(),
        value ? *value : nullValue, acc);
    JSPropertyAttributes attributes = 0;
    JSValueRef exception = 0;
    JSObjectSetProperty(ctx, object, nameString, jsvalue, attributes, &exception);
    JSStringRelease(nameString);
    if (exception) {
        WebCore::JSValueToWKJValue(exception, ctx, rootObject.get(), out);
        return WKJ_JS_EXCEPTION;
    }
    return WKJ_JS_OK;
}

WKJ_EXPORT int32_t wkj_js_remove_member(int64_t peer, int32_t peer_type,
    const uint16_t* name, int32_t name_length)
{
    WebCore::WKJCallScope wkjScope;

    if (name == nullptr)
        return WKJ_JS_NULL_ARGUMENT;

    JSObjectRef object = nullptr;
    JSContextRef ctx = nullptr;
    if (!checkJSPeer(peer, peer_type, object, ctx))
        return WKJ_JS_NO_CONTEXT;

    JSStringRef nameString = WebCore::asJSStringRef(name, name_length);
    JSObjectDeleteProperty(ctx, object, nameString, nullptr);
    JSStringRelease(nameString);
    return WKJ_JS_OK;
}

WKJ_EXPORT int32_t wkj_js_get_slot(int64_t peer, int32_t peer_type, int32_t index,
    WKJJSValue* out)
{
    WebCore::WKJCallScope wkjScope;
    wkjBridgeResetValue(out);

    JSObjectRef object = nullptr;
    JSContextRef ctx = nullptr;
    RefPtr<JSC::Bindings::RootObject> rootObject(checkJSPeer(peer, peer_type, object, ctx));
    if (rootObject.get() == nullptr)
        return WKJ_JS_NO_CONTEXT;

    JSValueRef value = JSObjectGetPropertyAtIndex(ctx, object, static_cast<unsigned>(index),
        nullptr);
    WebCore::JSValueToWKJValue(value, ctx, rootObject.get(), out);
    return WKJ_JS_OK;
}

WKJ_EXPORT int32_t wkj_js_set_slot(int64_t peer, int32_t peer_type, int32_t index,
    const WKJJSValue* value, wkj_ref acc)
{
    WebCore::WKJCallScope wkjScope;

    JSObjectRef object = nullptr;
    JSContextRef ctx = nullptr;
    RefPtr<JSC::Bindings::RootObject> rootObject(checkJSPeer(peer, peer_type, object, ctx));
    if (rootObject.get() == nullptr)
        return WKJ_JS_NO_CONTEXT;

    WKJJSValue nullValue;
    std::memset(&nullValue, 0, sizeof(nullValue));

    JSValueRef jsvalue = WebCore::WKJValueToJSValue(ctx, rootObject.get(),
        value ? *value : nullValue, acc);
    JSObjectSetPropertyAtIndex(ctx, object, (unsigned) index, jsvalue, nullptr);
    return WKJ_JS_OK;
}

WKJ_EXPORT int32_t wkj_js_to_string(int64_t peer, int32_t peer_type,
    uint16_t* result_buf, int32_t result_cap, int32_t* result_length, int64_t* result_handle)
{
    WebCore::WKJCallScope wkjScope;

    if (result_length)
        *result_length = 0;
    if (result_handle)
        *result_handle = 0;

    JSObjectRef object = nullptr;
    JSContextRef ctx = nullptr;
    if (!checkJSPeer(peer, peer_type, object, ctx))
        return WKJ_STR_NULL;

    JSC::JSGlobalObject* JSGlobalObject = toJS(ctx);
    JSC::JSLockHolder lock(JSGlobalObject);

    String text = toJS(object)->toString(JSGlobalObject)->value(JSGlobalObject);

    /*
     * The same buffer-or-handle rule the described values use, for the same reason: a
     * user-defined toString must not run twice because the first buffer was too small.
     */
    WKJJSValue holder;
    std::memset(&holder, 0, sizeof(holder));
    holder.string = result_buf;
    holder.string_cap = result_cap;
    wkjBridgeSetString(&holder, text);

    if (result_length)
        *result_length = holder.string_length;
    if (holder.string_handle) {
        if (result_handle)
            *result_handle = holder.string_handle;
        else
            wkj_js_string_release(holder.string_handle);
        return WKJ_STR_OVERFLOW;
    }
    return WKJ_STR_OK;
}

WKJ_EXPORT int32_t wkj_js_call(int64_t peer, int32_t peer_type,
    const uint16_t* name, int32_t name_length, const WKJJSValue* args, int32_t argc,
    wkj_ref acc, WKJJSValue* out)
{
    WebCore::WKJCallScope wkjScope;
    wkjBridgeResetValue(out);

    if (name == nullptr || args == nullptr)
        return WKJ_JS_NULL_ARGUMENT;

    JSObjectRef object = nullptr;
    JSContextRef ctx = nullptr;
    RefPtr<JSC::Bindings::RootObject> rootObject(checkJSPeer(peer, peer_type, object, ctx));
    if (!rootObject || !rootObject.get() || !ctx)
        return WKJ_JS_INVALID_FUNCTION;

    JSStringRef nameString = WebCore::asJSStringRef(name, name_length);
    JSValueRef member = JSObjectGetProperty(ctx, object, nameString, nullptr);
    JSStringRelease(nameString);
    if (!JSValueIsObject(ctx, member)) {
        out->kind = WKJ_JS_KIND_UNDEFINED;
        return WKJ_JS_OK;
    }
    JSObjectRef function = JSValueToObject(ctx, member, nullptr);
    if (! JSObjectIsFunction(ctx, function)) {
        out->kind = WKJ_JS_KIND_UNDEFINED;
        return WKJ_JS_OK;
    }

    size_t argumentCount = static_cast<size_t>(argc < 0 ? 0 : argc);
    Vector<JSValueRef> arguments(argumentCount);
    for (size_t i = 0; i < argumentCount; i++)
        arguments[i] = WebCore::WKJValueToJSValue(ctx, rootObject.get(), args[i], acc);

    JSValueRef exception = 0;
    JSValueRef result = JSObjectCallAsFunction(ctx, function, object,
                                               argumentCount, arguments.span().data(),
                                               &exception);
    if (exception) {
        WebCore::JSValueToWKJValue(exception, ctx, rootObject.get(), out);
        return WKJ_JS_EXCEPTION;
    }
    WebCore::JSValueToWKJValue(result, ctx, rootObject.get(), out);
    return WKJ_JS_OK;
}

WKJ_EXPORT void wkj_js_unprotect(int64_t peer, int32_t peer_type)
{
    WebCore::WKJCallScope wkjScope;

    JSObjectRef object = nullptr;
    JSContextRef ctx = nullptr;
    RefPtr<JSC::Bindings::RootObject> rootObject(checkJSPeer(peer, peer_type, object, ctx));
    if (!rootObject || !rootObject.get() || !peer || !ctx) {
        return;
    }

    rootObject->gcUnprotect(toJS(object));
}

WKJ_EXPORT int32_t wkj_js_string_copy(int64_t string_handle, uint16_t* result_buf,
    int32_t result_cap, int32_t* result_length)
{
    WebCore::WKJCallScope wkjScope;

    if (result_length)
        *result_length = 0;

    JSStringRef string = static_cast<JSStringRef>(wkj_to_ptr(string_handle));
    if (!string)
        return WKJ_STR_NULL;

    size_t length = JSStringGetLength(string);
    if (result_length)
        *result_length = static_cast<int32_t>(length);
    if (!result_buf || result_cap < static_cast<int32_t>(length))
        return WKJ_STR_OVERFLOW;

    if (length)
        std::memcpy(result_buf, JSStringGetCharactersPtr(string), length * sizeof(char16_t));
    return WKJ_STR_OK;
}

WKJ_EXPORT void wkj_js_string_release(int64_t string_handle)
{
    WebCore::WKJCallScope wkjScope;

    JSStringRef string = static_cast<JSStringRef>(wkj_to_ptr(string_handle));
    if (string)
        JSStringRelease(string);
}

/*
 * WebPage.twkExecuteScript. It lives here because everything it does after finding the frame
 * is LiveConnect: the same executeScript wkj_js_eval calls, and a described result.
 */
WKJ_EXPORT int32_t wkj_frame_execute_script(int64_t pFrame,
    const uint16_t* script, int32_t script_length, WKJJSValue* out)
{
    WebCore::WKJCallScope wkjScope;
    wkjBridgeResetValue(out);

    WebCore::Frame* mainFrame = static_cast<WebCore::Frame*>(wkj_to_ptr(pFrame));
    auto* frame = dynamicDowncast<WebCore::LocalFrame>(mainFrame);
    if (!frame) {
        /* The JNI version returned a null object reference here and threw nothing. */
        return WKJ_JS_OK;
    }

    JSGlobalContextRef globalContext = WebCore::getGlobalContext(&frame->script());
    RefPtr<JSC::Bindings::RootObject> rootObject(frame->script().createRootObject(frame));
    return WebCore::executeScript(nullptr, globalContext, rootObject.get(), script,
        script_length, out);
}

}
