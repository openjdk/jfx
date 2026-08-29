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

/*
 * webkit_java_api_bridge.h - the LiveConnect (netscape.javascript.JSObject) third of the
 * jfxwebkit C ABI.
 *
 * Scope, exactly: Source/WebCore/bridge/jni/**. That directory is the JavaScript <-> Java
 * bridge: it turns a JS value into a Java object and back, it exposes an arbitrary Java
 * object to script as a JS object, and it carries the nine JNI entry points behind
 * com.sun.webkit.dom.JSObject. Its directory name is now a misnomer - there is no JNI in
 * it any more - but the name is kept so that upstream WebKit merges stay tractable.
 *
 * ------------------------------------------------------------------------------------------
 * WHAT IS HERE AND WHAT IS ONLY PASSING THROUGH  (FFM-AUDIT-wtf-webcore.md section 9.1)
 * ------------------------------------------------------------------------------------------
 * The audit splits this directory in two, and the split is why this header has the shape it
 * has:
 *
 *   OS-CALL, stays native for good
 *       BridgeUtils.cpp's JavaScript half (JSEvaluateScript, JSObjectGetProperty,
 *       JSObjectSetProperty, JSObjectDeleteProperty, JSObjectCallAsFunction,
 *       JSStringCreateWithCharacters, RootObject::gcProtect / gcUnprotect), plus
 *       JavaInstanceJSC and JavaRuntimeObject, which are JavaScriptCore RuntimeObject
 *       subclasses that JSC itself instantiates and dispatches on under a JSLockHolder.
 *       Nothing in Java can do any of that. The nine wkj_js_* entry points below are its
 *       front door.
 *
 *   WRAPPER over java.lang.reflect, PARITY: unknown, so it stays for now
 *       JavaClassJSC, JavaFieldJSC, JavaMethodJSC, JavaArrayJSC and the reflective half of
 *       JNIUtility - about 1,400 lines whose entire job is to enumerate a Java class and
 *       invoke its methods, and which already round-trips through Java to do it
 *       (com.sun.webkit.Utilities.fwkInvokeWithContext performs the actual Method.invoke).
 *       The audit's recommendation is to delete it and do the reflection in Java, but it
 *       rates the parity `unknown` until JavaScript-visible overload resolution is proven
 *       equivalent - JavaClass::methodNamed parses `name(paramTypes)` syntax and applies
 *       LiveConnect's own matching rules - so it is migrated here rather than deleted.
 *
 *       The WKJLiveConnectHost slots marked "reflection" below are that code's only
 *       remaining contact with Java. When the parity experiment in the audit (an overload
 *       matrix added to test/javafx/scene/web/JavaScriptBridgeTest.java) is written and
 *       passing, those slots are the deletion's blueprint: the Java side already implements
 *       every operation the C++ needs, so the change is to call them from a Java
 *       reimplementation and drop the five files. That is a behaviour commit and must not
 *       be folded into a migration commit.
 *
 * ------------------------------------------------------------------------------------------
 * INCLUDE DIRECTION - deliberately the same as the DOM and page headers
 * ------------------------------------------------------------------------------------------
 * This header includes webkit_java_api.h, exactly as webkit_java_api_dom.h and
 * webkit_java_api_page.h do; it is not included by the master, the way
 * webkit_java_api_platform.h is.
 *
 * The reason for the difference is mechanical. The platform header defines
 * WKJHostGraphics, WKJHostNetwork and WKJHostMedia, which are *members* of WKJHost, so
 * their definitions have to be complete half way through the master header, and only the
 * master-includes-area direction can deliver that. Nothing here is a member of WKJHost:
 * WKJLiveConnectHost is a second, independent process-wide table with its own installer
 * (wkj_live_connect_init), which is what FFM-AUDIT-wtf-webcore.md section 3.4 asks for -
 * "they belong to a separate WebKitLiveConnectHost installed by JSObject". So there is no
 * placeholder to supersede here, no duplicate-tag hazard, and no edit owed by the owner of
 * webkit_java_api.h. Keeping the table separate is also what lets this slice be migrated,
 * reviewed and (later) deleted without touching the master header at all.
 */

/*
 * ------------------------------------------------------------------------------------------
 * CONVENTIONS - all inherited from webkit_java_api.h; only the additions are restated
 * ------------------------------------------------------------------------------------------
 * Strings         UTF-16. Inbound as `const uint16_t* s, int32_t s_len`, where s == NULL
 *                 means the Java value was null and IS distinguished from the empty string
 *                 on this ABI: every one of the nine entry points below rejects a null
 *                 String argument before WebCore ever sees it, so the null-and-empty
 *                 collapse WKJString performs for the DOM does not apply to them.
 *                 Outbound into a caller-provided buffer, returning WKJ_STR_OK /
 *                 WKJ_STR_NULL / WKJ_STR_OVERFLOW (contract 13).
 * Booleans        int32_t, 0 or 1. FFM has no boolean layout. The one exception on this
 *                 ABI is WKJ_JT_BOOLEAN inside a WKJJavaValue, which carries the raw 8-bit
 *                 truncation described on that member and in faithfulness note 1.
 * Java objects    wkj_ref (contract 3), 0 for null. A slot or function that RETURNS a
 *                 wkj_ref returns a NEW id which the receiver owns and must release exactly
 *                 once; a wkj_ref PARAMETER is borrowed for the duration of the call.
 * Native objects  int64_t peers, converted with wkj_to_ptr / wkj_from_ptr.
 * NULL slots      every callback pointer may be NULL. The library tests it before every
 *                 call and falls back to the default documented on the slot, which in every
 *                 case reproduces what the JNI code did when a lookup failed: log it and
 *                 carry on with a zero, null or undefined value.
 * Errors          nothing here writes the WKJExceptionSlot. The four sites that raised a
 *                 Java exception from C (NullPointerException twice, JSException twice)
 *                 became the WKJ_JS_* status codes below, which the Java facade turns back
 *                 into exactly the same four exceptions. Every exported function still
 *                 clears the slot on entry, as webkit_java_api.h rule 4 requires.
 * critical        Linker.Option.critical(true) is forbidden on every function here. All of
 *                 them run JavaScript, and several call back into Java while doing it.
 *
 * ------------------------------------------------------------------------------------------
 * THREAD
 * ------------------------------------------------------------------------------------------
 * Everything in this header - entry points and callback slots alike - runs on the JavaFX
 * application thread, which is also the WebKit main thread and the thread that owns the
 * JavaScript context. The Java side asserts it: com.sun.webkit.dom.JSObject calls
 * Invoker.getInvoker().checkEventThread() before every native method. The library holds the
 * JSC API lock (JSC::JSLockHolder) across most of the work, and therefore across the
 * callbacks - a callback must not block, and must not re-enter WebKit other than through
 * this ABI.
 *
 * ------------------------------------------------------------------------------------------
 * WEAK REFERENCES - read this before implementing retain on the Java side
 * ------------------------------------------------------------------------------------------
 * Source/WebCore/bridge/jni/JobjectWrapper.cpp takes a WEAK reference by default: its
 * useGlobalRef parameter defaults to false, so every Java object exposed to script -
 * JavaInstance's target, JavaArray's array, JavaField's Field - was held by
 * NewWeakGlobalRef, and only the access-control context was held strongly. Its destructor
 * asked GetObjectRefType to decide which delete to use.
 *
 * That is why WKJHostCore has retain_weak and is_live and why WKJHandle has retainedWeak().
 * JobjectWrapper now calls WKJRetainWeak for the same objects and WKJRetain for the same one
 * strong case, and every dereference still goes through a liveness check first
 * (WKJHandle::retained on the weak id, which yields 0 once the object is gone) exactly where
 * the JNI code created a local reference from the weak global for the same purpose.
 * Modelling these ids as strong would pin every object an application ever passed to
 * JSObject.setMember for the life of the registry, which is a leak, not a simplification.
 * WKJHostCore.release must therefore accept a weak id.
 *
 * ------------------------------------------------------------------------------------------
 * THREE FAITHFULNESS NOTES, each of which is a place where "obvious" is wrong
 * ------------------------------------------------------------------------------------------
 * 1. A JS number converted to a Java boolean truncates to 8 bits. JNIUtilityPrivate.cpp
 *    wrote `result.z = (jboolean) value.toNumber(...)` and jboolean is unsigned char, so
 *    256 becomes false and 257 becomes true. The C++ now masks with 0xFF explicitly and
 *    says why. WKJ_JT_BOOLEAN values on this ABI are therefore 0 or 1 as produced by that
 *    truncation, not by a general non-zero test. Do not "fix" it on the Java side; it is a
 *    real bug, it is recorded in FFM-ABI-CONTRACT.md section 13.1 finding 7, and it gets its
 *    own commit with its own JavaScriptBridgeTest case.
 *
 * 2. Java class names travel as UTF-16, not as modified UTF-8. JNIUtility.cpp read
 *    Class.getName() with GetStringUTFChars, which encodes a supplementary character as
 *    six CESU-8 bytes where standard UTF-8 uses four. The consumers - strcmp against ASCII
 *    literals in JavaClass, and array-descriptor parsing in BridgeUtils - agree for every
 *    name in the Basic Multilingual Plane, which is every name anyone has, but the
 *    divergence is not provably impossible. class_get_name below hands over UTF-16 and the
 *    C++ converts once, so the question does not arise again.
 *
 * 3. resolve_method must reproduce what GetMethodID + ToReflectedMethod produced, because
 *    com.sun.webkit.Utilities.fwkInvokeWithContext makes a security decision on
 *    method.getDeclaringClass(). See the slot's own comment.
 */

#ifndef WEBKIT_JAVA_API_BRIDGE_H
#define WEBKIT_JAVA_API_BRIDGE_H

#include "webkit_java_api.h"

/*
 * The peer_type values that accompany a JSObject peer - JS_CONTEXT_OBJECT,
 * JS_DOM_NODE_OBJECT and JS_DOM_WINDOW_OBJECT - are not redeclared here. They live in
 * wkj_constants.h under their original com_sun_webkit_dom_JSObject_* spelling, generated
 * from com/sun/webkit/dom/JSObject.java, which is the single definition for both sides.
 */
#include "wkj_constants.h"

#ifdef __cplusplus
extern "C" {
#endif

/* ======================================================================================== */
/* Java type codes                                                                          */
/* ======================================================================================== */

/*
 * The type of a Java value crossing this ABI. The values are those of the JavaType enum in
 * Source/WebCore/bridge/jni/JavaType.h, unchanged and in the same order, so that the C++
 * passes its existing JavaType through as an int32_t. JNIUtility.cpp carries a static_assert
 * per constant binding the two definitions together; if JavaType is ever reordered - its own
 * comment warns against it - the build stops there rather than here.
 */
#define WKJ_JT_INVALID  0
#define WKJ_JT_VOID     1
#define WKJ_JT_OBJECT   2
#define WKJ_JT_BOOLEAN  3
#define WKJ_JT_BYTE     4
#define WKJ_JT_CHAR     5
#define WKJ_JT_SHORT    6
#define WKJ_JT_INT      7
#define WKJ_JT_LONG     8
#define WKJ_JT_FLOAT    9
#define WKJ_JT_DOUBLE   10
#define WKJ_JT_ARRAY    11

/*
 * One Java value, tagged with its type: the replacement for the JNI jvalue union at the
 * places where the library has to hand a typed value to Java or take one back - reflective
 * field access, array element access, boxing and unboxing.
 *
 * It is a struct rather than a union because FFM has no union layout, and it costs no more
 * to fill. Which member carries the value depends on `type`:
 *
 *   WKJ_JT_BOOLEAN   i   0..255, and non-zero means true. This is the one place on this ABI
 *                        where a boolean is not simply 0 or 1: a JS number reaches a Java
 *                        boolean through an 8-bit truncation (faithfulness note 1), so 256
 *                        arrives as 0 and 2 arrives as 2. The JVM read the same byte, and
 *                        the raw value is carried rather than normalised so that Java can
 *                        keep making exactly the decision it made before.
 *   WKJ_JT_BYTE      i   sign extended, -128..127
 *   WKJ_JT_CHAR      i   0..65535, one UTF-16 code unit
 *   WKJ_JT_SHORT     i   sign extended, -32768..32767
 *   WKJ_JT_INT       i
 *   WKJ_JT_LONG      j
 *   WKJ_JT_FLOAT     d   the float widened to double, which is exact in both directions;
 *                        the Java side narrows with a (float) cast and gets the same bits
 *   WKJ_JT_DOUBLE    d
 *   WKJ_JT_OBJECT    l   a wkj_ref, 0 for null
 *   WKJ_JT_ARRAY     l   a wkj_ref naming a Java array, 0 for null
 *   WKJ_JT_VOID      -   no member is meaningful
 *   WKJ_JT_INVALID   -   no value at all: what a failed call leaves behind, and what
 *                        memset(&result, 0, sizeof(jvalue)) meant in the JNI code
 *
 * Unused members are zero. Ownership of `l` follows the general rule: a value the library
 * receives borrows it, a value the library is handed back owns it.
 */
typedef struct WKJJavaValue {
    int32_t type;
    int32_t i;
    int64_t j;
    double  d;
    wkj_ref l;
} WKJJavaValue;

/* ======================================================================================== */
/* JavaScript values                                                                        */
/* ======================================================================================== */

/*
 * The kind of a JavaScript value as it crosses to or from Java. This is the tagged union
 * that replaces "the library constructs a java.lang.Object"
 * (FFM-AUDIT-wtf-webcore.md section 6.6): the library no longer builds Integer, Double,
 * Boolean, String, JSObject or NodeImpl instances, it describes the value and Java builds
 * whatever it wants.
 *
 * OUTBOUND (library -> Java) these are the exact outcomes of convertValueToJValue for the
 * target type java.lang.Object, which is what all nine entry points used:
 *
 *   WKJ_JS_KIND_NULL         JS null, and every value the JNI code left as a zero reference
 *   WKJ_JS_KIND_UNDEFINED    JS undefined -> the JSObject.UNDEFINED singleton
 *   WKJ_JS_KIND_BOOLEAN      `number` is 0.0 or 1.0            -> Boolean.valueOf
 *   WKJ_JS_KIND_INT          `number` holds an exact int32     -> Integer.valueOf
 *   WKJ_JS_KIND_DOUBLE       `number`                          -> Double.valueOf
 *   WKJ_JS_KIND_STRING       `string` / `string_length`        -> java.lang.String
 *   WKJ_JS_KIND_JS_OBJECT    `peer` is a gc-protected JSObjectRef and `peer_type` is
 *                            com_sun_webkit_dom_JSObject_JS_CONTEXT_OBJECT
 *                                                              -> new JSObject(peer, type)
 *   WKJ_JS_KIND_DOM_NODE     `peer` is a WebCore Node* on which the library has already
 *                            called ref(); the deref is the NodeImpl disposer, as before
 *                                                              -> NodeImpl.getCachedImpl
 *   WKJ_JS_KIND_JAVA_OBJECT  `object` is a new strong id for the Java object that the JS
 *                            value wrapped - a JavaInstance target or a JavaArray - so the
 *                            original object comes back, not a copy
 *
 * The INT / DOUBLE split is load-bearing rather than an optimisation: the JNI code produced
 * an Integer for a JS value that isInt32() and a Double otherwise, and an application can
 * tell the difference.
 *
 * INBOUND (Java -> library) the kinds Java produces are NULL, BOOLEAN, DOUBLE, STRING,
 * JS_OBJECT and JAVA_OBJECT, chosen in that order by the instanceof chain that used to live
 * in Java_Object_to_JSValue:
 *
 *   null                  -> WKJ_JS_KIND_NULL
 *   com.sun.webkit.dom.JSObject, which NodeImpl extends
 *                         -> WKJ_JS_KIND_JS_OBJECT with the object own peer and peer_type.
 *                            This is where the GetFieldID reads of JSObject.peer and
 *                            JSObject.peer_type went: Java reads its own fields, so the
 *                            library no longer needs the field ids or the names "peer" and
 *                            "peer_type" at all.
 *   java.lang.String      -> WKJ_JS_KIND_STRING
 *   java.lang.Boolean     -> WKJ_JS_KIND_BOOLEAN
 *   java.lang.Number      -> WKJ_JS_KIND_DOUBLE, from Number.doubleValue(), for every
 *                            Number including Integer; the JNI code did the same, which is
 *                            why there is deliberately no inbound INT kind
 *   anything else         -> WKJ_JS_KIND_JAVA_OBJECT; the library then asks the reflection
 *                            slots whether the class is an array, and wraps the object as a
 *                            RuntimeArray or a JavaInstance
 *
 * Inbound UNDEFINED, INT and DOM_NODE do not occur. In particular a Java value that happens
 * to be the JSObject.UNDEFINED singleton is a plain String on the way in and becomes the JS
 * string "undefined", which is what it has always done.
 */
#define WKJ_JS_KIND_NULL        0
#define WKJ_JS_KIND_UNDEFINED   1
#define WKJ_JS_KIND_BOOLEAN     2
#define WKJ_JS_KIND_INT         3
#define WKJ_JS_KIND_DOUBLE      4
#define WKJ_JS_KIND_STRING      5
#define WKJ_JS_KIND_JS_OBJECT   6
#define WKJ_JS_KIND_DOM_NODE    7
#define WKJ_JS_KIND_JAVA_OBJECT 8

/*
 * One JavaScript value.
 *
 * STRINGS, AND WHY THERE IS A HANDLE
 *
 * A string result is copied into the caller buffer, like every other string on this ABI:
 * the caller sets `string` and `string_cap` before the call and reads `string_length`
 * afterwards. What is different here is what happens when it does not fit.
 *
 * The DOM half answers WKJ_STR_OVERFLOW and the facade calls again with a bigger buffer.
 * That is safe for a DOM getter and unsafe here: calling wkj_js_eval or wkj_js_call again
 * would run the script a second time, and even wkj_js_get_member can run a user-defined
 * getter. So when the string does not fit, the library keeps it alive as a retained
 * JavaScriptCore string, returns its address in `string_handle` with `string_length` set to
 * the capacity required, and the caller finishes the transfer with wkj_js_string_copy
 * followed by wkj_js_string_release. Nothing is re-executed, and nothing is owned
 * implicitly: a non-zero `string_handle` is an obligation to release, exactly like a
 * wkj_ref.
 *
 * `string_handle` is 0 in every other case, including a string that fitted.
 */
typedef struct WKJJSValue {
    int32_t   kind;            /* WKJ_JS_KIND_*                                            */
    int32_t   peer_type;       /* JS_OBJECT: com_sun_webkit_dom_JSObject_JS_*_OBJECT        */
    double    number;          /* BOOLEAN (0.0 / 1.0), INT (an exact int32), DOUBLE         */
    int64_t   peer;            /* JS_OBJECT: JSObjectRef; DOM_NODE: a ref()ed Node*         */
    int64_t   string_handle;   /* out only: a retained string to copy out, then release     */
    wkj_ref   object;          /* JAVA_OBJECT: in, borrowed; out, a new strong id           */
    uint16_t* string;          /* STRING: in, the characters; out, the caller buffer        */
    int32_t   string_cap;      /* out only: capacity of `string`, in UTF-16 code units      */
    int32_t   string_length;   /* code units present, or required when string_handle != 0   */
} WKJJSValue;

/* ======================================================================================== */
/* Status codes                                                                             */
/* ======================================================================================== */

/*
 * What an entry point below returns. These replace the four places where the JNI code threw
 * a Java exception from C; the Java facade throws the same four exceptions, and nothing else
 * about the control flow changes.
 *
 *   WKJ_JS_OK               the call completed; an out value, if any, has been written
 *   WKJ_JS_EXCEPTION        JavaScript threw. The thrown value is in the out parameter,
 *                           converted the way a result would be, and Java raises
 *                           JSObject.fwkMakeException(value) - which is what the
 *                           throwJavaException of BridgeUtils.cpp did with the same value.
 *   WKJ_JS_NULL_ARGUMENT    a required argument was null: the script, the member name or
 *                           the argument array. Java raises NullPointerException, which is
 *                           what throwNullPointerException raised, and the call did nothing.
 *   WKJ_JS_NO_CONTEXT       the peer named no live JavaScript context - checkJSPeer found no
 *                           RootObject, or the frame is gone. Java raises
 *                           NullPointerException: the same exception, from the same place.
 *   WKJ_JS_INVALID_FUNCTION only from wkj_js_call, and only where the JNI code called
 *                           ThrowNew(JSException, "Invalid function reference"): Java raises
 *                           netscape.javascript.JSException with that exact message.
 *
 * wkj_js_to_string does not use these. Being a plain string return it uses WKJ_STR_OK,
 * WKJ_STR_NULL and WKJ_STR_OVERFLOW like the rest of the ABI; it has never thrown.
 */
#define WKJ_JS_OK                0
#define WKJ_JS_EXCEPTION         1
#define WKJ_JS_NULL_ARGUMENT     2
#define WKJ_JS_NO_CONTEXT        3
#define WKJ_JS_INVALID_FUNCTION  4

/* ======================================================================================== */
/* WKJLiveConnectHost - the callbacks that replace the 38 cached-method-id upcalls     */
/* ======================================================================================== */

/*
 * A second process-wide table, installed by wkj_live_connect_init, holding everything this
 * directory needs from Java. It is separate from WKJHost because it belongs to a subsystem
 * that is expected to be deleted rather than kept (see the top of this file), and because
 * nothing else in the library may call these: they run arbitrary application code.
 *
 * Every slot may be NULL, and every default is the behaviour the JNI code produced when its
 * own lookup failed, which was always "log it and carry on with nothing".
 *
 * Every slot runs on the thread that called into the library, with the JSC lock held. None
 * may block, and none may re-enter WebKit except through this ABI.
 */
typedef struct WKJLiveConnectHost {

    /* sizeof(WKJLiveConnectHost) as the caller sees it; must equal the host_size argument. */
    int32_t size;

    /* --- reflection: java.lang.Object and java.lang.Class ------------------------------ */

    /*
     * obj.getClass(). Returns a new id for the Class, or 0.
     * Was callJNIMethod<Object>(obj, "getClass", "()Ljava/lang/Class;").
     * Default when NULL: 0.
     */
    wkj_ref (*object_get_class)(wkj_ref obj);

    /*
     * cls.getName(), as UTF-16 (see faithfulness note 2). WKJ_STR_OK / WKJ_STR_NULL /
     * WKJ_STR_OVERFLOW. This is the reflection name - "int", "[I", "java.lang.String",
     * "[Ljava.lang.String;" - and not a JNI descriptor.
     * Default when NULL: WKJ_STR_NULL, which the C++ turns into "<Unknown>" exactly as it
     * did when getName returned nothing.
     */
    int32_t (*class_get_name)(wkj_ref cls, uint16_t* result_buf, int32_t result_cap,
                              int32_t* result_length);

    /* cls.isArray(). Default when NULL: 0. */
    int32_t (*class_is_array)(wkj_ref cls);

    /*
     * new java.lang.Object(). The stand-in JavaClass builds when the object it was asked
     * about has already been collected - the weak reference is dead - so that the rest of
     * its constructor has something to reflect on. Returns a new id, or 0.
     * Was FindClass("java/lang/Object") + NewObject in JavaClass::createDummyObject.
     * Default when NULL: 0, which lands on the same "<Unknown>" path as before.
     */
    wkj_ref (*create_dummy_object)(void);

    /* --- reflection: methods ---------------------------------------------------------- */

    /*
     * The java.lang.reflect.Method that JNI produced from
     *     ToReflectedMethod(GetObjectClass(obj), GetMethodID(GetObjectClass(obj), name, sig))
     * or 0 if there is no such method. `sig` is a JNI method descriptor, for example
     * "()Ljava/lang/String;": the C++ builds it in JavaMethod::signature() and passes it
     * back, and it also asks for well-known ones such as toString by hand.
     *
     * WHY THE DESCRIPTOR IS STILL HERE, rather than the Method the C++ already had: the
     * search has to reach the same Method object JNI would have, because
     * com.sun.webkit.Utilities.fwkInvokeWithContext decides whether the call is permitted
     * from method.getDeclaringClass(). Search obj.getClass().getMethods() - the same set
     * JavaClass enumerates, and the only set reachable here - and match the name plus the
     * descriptor rebuilt from the parameter types and the return type. Matching the return
     * type as well as the parameters is not optional: a covariant override gives a class two
     * methods with one name and one parameter list.
     *
     * Default when NULL: 0, which every caller treats as the failed lookup it always was.
     */
    wkj_ref (*resolve_method)(wkj_ref obj, const uint16_t* name, int32_t name_length,
                              const uint16_t* signature, int32_t signature_length);

    /*
     * com.sun.webkit.Utilities.fwkInvokeWithContext(method, instance, args, acc): the actual
     * invocation, which has always happened in Java. Returns a new id for the result - null
     * and a void return are both 0 - and, if the invocation threw, writes a new id for the
     * Throwable through out_exception. Both ids belong to the library.
     *
     * `args` is argc borrowed ids; Java builds the Object[] itself, which is where
     * NewObjectArray and SetObjectArrayElement went. `acc` is the opaque access-control
     * object the Java side handed in (JSObject.DUMMY_ACC), passed straight back; the library
     * has never looked inside it, and fwkInvokeWithContext ignores it.
     *
     * Java must not let the Throwable escape the upcall: catch it, and report it here. That
     * is the same swallowing the JNI code did with ExceptionOccurred + ExceptionClear, and
     * the behaviour of the caller is unchanged - it wraps the Throwable in a JavaInstance and
     * throws it into JavaScript.
     *
     * Default when NULL: 0 result, 0 exception.
     */
    wkj_ref (*invoke)(wkj_ref method, wkj_ref instance, const wkj_ref* args, int32_t argc,
                      wkj_ref acc, wkj_ref* out_exception);

    /* method.getName(). Default when NULL: WKJ_STR_NULL, i.e. "<Unknown>". */
    int32_t (*method_get_name)(wkj_ref method, uint16_t* result_buf, int32_t result_cap,
                               int32_t* result_length);

    /*
     * method.getReturnType().getName(). One call instead of the two the JNI code made; the
     * intermediate Class was never used for anything else. Default: WKJ_STR_NULL.
     */
    int32_t (*method_get_return_type_name)(wkj_ref method, uint16_t* result_buf,
                                           int32_t result_cap, int32_t* result_length);

    /* method.getParameterTypes().length. Default when NULL: 0. */
    int32_t (*method_get_parameter_count)(wkj_ref method);

    /*
     * method.getParameterTypes()[index].getName(). Default: WKJ_STR_NULL, i.e. "<Unknown>",
     * which is what the JNI code substituted for a parameter whose name it could not read.
     */
    int32_t (*method_get_parameter_type_name)(wkj_ref method, int32_t index,
                                              uint16_t* result_buf, int32_t result_cap,
                                              int32_t* result_length);

    /* method.getModifiers(). Only bit 0x8, ACC_STATIC, is read. Default when NULL: 0. */
    int32_t (*method_get_modifiers)(wkj_ref method);

    /* --- reflection: fields ------------------------------------------------------------ */

    /* field.getName(). Default when NULL: WKJ_STR_NULL, i.e. "<Unknown>". */
    int32_t (*field_get_name)(wkj_ref field, uint16_t* result_buf, int32_t result_cap,
                              int32_t* result_length);

    /* field.getType().getName(), one call for the two the JNI code made. Default: NULL. */
    int32_t (*field_get_type_name)(wkj_ref field, uint16_t* result_buf, int32_t result_cap,
                                   int32_t* result_length);

    /*
     * The typed read of one field of one instance: field.get(instance) for WKJ_JT_OBJECT and
     * WKJ_JT_ARRAY, and field.getBoolean / getByte / getChar / getShort / getInt / getLong /
     * getFloat / getDouble(instance) for the primitive types. The `type` argument says which;
     * the C++ knows it from the declared type of the field and has always switched on it
     * here.
     *
     * Returns 1 on success and 0 on failure; on failure `out` is left WKJ_JT_INVALID, which
     * is what the zeroed jvalue meant. Default when NULL: 0.
     *
     * Note that these bypass fwkInvokeWithContext, exactly as the direct Field.get* calls of
     * the JNI code did. Adding the Utilities allow-list check here would be a behaviour
     * change, not a hardening; it belongs to whoever revisits the security model.
     */
    int32_t (*field_get)(wkj_ref field, wkj_ref instance, int32_t type, WKJJavaValue* out);

    /*
     * The matching typed write: field.set / setBoolean / ... (instance, value).
     * Returns 1 on success, 0 on failure. Default when NULL: 0.
     */
    int32_t (*field_set)(wkj_ref field, wkj_ref instance, int32_t type,
                         const WKJJavaValue* value);

    /* --- Java arrays ------------------------------------------------------------------- */

    /*
     * The length of a Java array, i.e. GetArrayLength. Used both for the Field[] and Method[]
     * that the class enumeration walks and for a Java array exposed to script as a JS array.
     * Default when NULL: 0.
     */
    int32_t (*array_length)(wkj_ref array);

    /*
     * One element, typed as for field_get: WKJ_JT_OBJECT and WKJ_JT_ARRAY read the element as
     * an object, the primitive codes read it as that primitive. Replaces
     * GetObjectArrayElement and the Get<Type>ArrayRegion family.
     * Returns 1 on success, 0 on failure - including an out-of-range index, which the JNI
     * code left to the JVM to report. Default when NULL: 0.
     */
    int32_t (*array_get)(wkj_ref array, int32_t index, int32_t type, WKJJavaValue* out);

    /*
     * One element written, replacing SetObjectArrayElement and Set<Type>ArrayRegion.
     * Returns 1 on success, 0 on failure. Default when NULL: 0.
     */
    int32_t (*array_set)(wkj_ref array, int32_t index, int32_t type,
                         const WKJJavaValue* value);

    /* --- boxing, unboxing and strings -------------------------------------------------- */

    /*
     * Boolean.valueOf / Byte.valueOf / Character.valueOf / Short.valueOf / Integer.valueOf /
     * Long.valueOf / Float.valueOf / Double.valueOf, chosen by value->type. Returns a new id,
     * or 0. WKJ_JT_OBJECT and WKJ_JT_ARRAY are not valid here - an object needs no box - and
     * neither are VOID and INVALID. Default when NULL: 0.
     */
    wkj_ref (*box)(const WKJJavaValue* value);

    /*
     * booleanValue() / byteValue() / charValue() / shortValue() / intValue() / longValue() /
     * floatValue() / doubleValue() on a boxed value, chosen by `type`. Returns 1 on success
     * and 0 on failure; on failure `out` is WKJ_JT_INVALID.
     *
     * The JNI code reached these through callJNIMethod<T>(obj, "intValue", "()I") and
     * friends, so they too bypass the Utilities allow list, and so does this.
     * Default when NULL: 0.
     */
    int32_t (*unbox)(wkj_ref boxed, int32_t type, WKJJavaValue* out);

    /*
     * A java.lang.String holding the given UTF-16 characters; s == NULL means null and
     * returns 0. Returns a new id. This is the replacement for String::toJavaString, and it
     * is how a JS string becomes a Java String without the library naming a Java type.
     * Default when NULL: 0.
     */
    wkj_ref (*box_string)(const uint16_t* s, int32_t s_length);

    /*
     * The characters of a java.lang.String, as UTF-16: WKJ_STR_OK / WKJ_STR_NULL /
     * WKJ_STR_OVERFLOW. Replaces GetStringChars and GetStringCritical on a value that reached
     * the library as an object rather than as a parameter - the result of toString(), for
     * instance. A `str` that is not a String is WKJ_STR_NULL.
     * Default when NULL: WKJ_STR_NULL.
     */
    int32_t (*string_value)(wkj_ref str, uint16_t* result_buf, int32_t result_cap,
                            int32_t* result_length);

    /*
     * Describes one Java object as a WKJJSValue, using the INBOUND rules documented above:
     * null, JSObject (with its own peer and peer_type), String, Boolean, Number, or
     * anything else as WKJ_JS_KIND_JAVA_OBJECT. This is the instanceof chain that used to
     * live in Java_Object_to_JSValue, and it is the same classification Java performs on the
     * arguments of wkj_js_set_member, wkj_js_set_slot and wkj_js_call - one implementation,
     * two callers, so a value cannot be classified one way as an argument and another way as
     * a field value.
     *
     * `out->string` and `out->string_cap` are the buffer of the caller. Returns WKJ_STR_OK,
     * or WKJ_STR_OVERFLOW with out->string_length set to the capacity required, in which case
     * the caller grows and asks again; describing an object twice has no side effects.
     *
     * For WKJ_JS_KIND_JAVA_OBJECT, out->object is a NEW strong id which the library releases
     * when it is done with it. Note that what it does with it first is wrap it weakly in a
     * JavaInstance, so the object stays exactly as collectable as it was under the weak
     * global reference this replaces.
     * Default when NULL: WKJ_JS_KIND_NULL, WKJ_STR_OK.
     */
    int32_t (*describe_object)(wkj_ref obj, WKJJSValue* out);

    /* --- the three LiveConnect objects the library cannot describe generically ---------- */

    /*
     * The com.sun.webkit.dom.JSObject.UNDEFINED singleton: the value LiveConnect hands back
     * for JavaScript undefined.
     *
     * This is where a GetStaticFieldID read of a private static field went. It cannot become
     * a plain value on this ABI, because the identity of the field is the point -
     * JSObject.java creates it as `new String("undefined")` precisely so that it is not
     * equal by reference to any other "undefined" string, and callers compare against it
     * with ==. So the library asks for the object and keeps one id for the life of the
     * process, exactly as it kept one global reference. Returns a new id, or 0.
     *
     * On the way out through the nine entry points there is no call at all: the value is
     * described as WKJ_JS_KIND_UNDEFINED and Java uses its own field. This slot exists for
     * the other direction, where the library needs an actual object to store in a Java field
     * or pass as a Java method argument.
     * Default when NULL: 0, which reaches Java as null - the same thing the JNI code
     * produced when the field lookup failed.
     */
    wkj_ref (*undefined_object)(void);

    /*
     * new com.sun.webkit.dom.JSObject(peer, peer_type), for a JavaScript object the library
     * has already gc-protected. Returns a new id, or 0. The protection is dropped by
     * wkj_js_unprotect from the disposer of the Java object, which is unchanged.
     * Default when NULL: 0.
     */
    wkj_ref (*jsobject_create)(int64_t peer, int32_t peer_type);

    /*
     * com.sun.webkit.dom.NodeImpl.getCachedImpl(peer), for a DOM node the library has already
     * called ref() on; the matching deref() is in the NodeImpl disposer, which is unchanged.
     * Returns a new id, or 0.
     *
     * getCachedImpl is a private static method, which is why the JNI code needed
     * GetStaticMethodID to reach it. A Java-side facade calls it from inside the package of
     * the class instead, so the privacy is no longer worked around, only respected.
     * Default when NULL: 0.
     */
    wkj_ref (*node_get_cached_impl)(int64_t node_peer);

} WKJLiveConnectHost;

/*
 * Installs the LiveConnect table. Called once per process, before any wkj_js_* call, from
 * the Java facade that owns com.sun.webkit.dom.JSObject. `host` must outlive the library,
 * which keeps the pointer; `host_size` must equal host->size and sizeof(WKJLiveConnectHost)
 * as the library sees it; `abi_version` must equal WKJ_ABI_VERSION.
 *
 * Returns WKJ_INIT_OK, or one of the WKJ_INIT_ERR_* codes from webkit_java_api.h. It is
 * separate from wkj_init because this table belongs to one subsystem, and because a use of
 * the library that never exposes a Java object to script never needs it.
 */
WKJ_EXPORT int32_t wkj_live_connect_init(const WKJLiveConnectHost* host, int32_t host_size,
                                         uint32_t abi_version);

/*
 * Library-internal: the installed table, NULL until wkj_live_connect_init succeeds. Declared
 * here for the same reason wkj_host is declared in the master header - it is read directly
 * by the inline helpers - and it is not part of the Java-facing ABI.
 */
extern const WKJLiveConnectHost* wkj_live_connect_host;

/* ======================================================================================== */
/* The nine com.sun.webkit.dom.JSObject entry points                                        */
/* ======================================================================================== */

/*
 * Each of these replaces one Java_com_sun_webkit_dom_JSObject_*Impl function, argument for
 * argument, with the Java object result replaced by a described value.
 *
 * `peer` and `peer_type` are the two fields of the calling JSObject: for JS_CONTEXT_OBJECT
 * the peer is a JSObjectRef the library gc-protected when it created that JSObject; for
 * JS_DOM_NODE_OBJECT and JS_DOM_WINDOW_OBJECT it is a WebCore Node* or LocalDOMWindow* and
 * the library re-derives the script context from its frame on every call.
 *
 * `out` must not be NULL where one is taken. Its `string` and `string_cap` are the buffer of
 * the caller for a string result and may be NULL and 0, in which case a string result comes
 * back through `string_handle` (see WKJJSValue). Every other member is written by the
 * library.
 */

/* eval(script). Was Java_com_sun_webkit_dom_JSObject_evalImpl. */
WKJ_EXPORT int32_t wkj_js_eval(int64_t peer, int32_t peer_type,
                               const uint16_t* script, int32_t script_length,
                               WKJJSValue* out);

/* getMember(name). Was Java_com_sun_webkit_dom_JSObject_getMemberImpl. */
WKJ_EXPORT int32_t wkj_js_get_member(int64_t peer, int32_t peer_type,
                                     const uint16_t* name, int32_t name_length,
                                     WKJJSValue* out);

/*
 * setMember(name, value). Was Java_com_sun_webkit_dom_JSObject_setMemberImpl. `acc` is the
 * opaque JSObject.DUMMY_ACC.
 *
 * It takes an `out` because a setter can throw: JSObjectSetProperty is the one call of the
 * three writing entry points that the JNI code passed an exception out-parameter to, and it
 * turned a JavaScript exception there into a Java one. `out` is written only for
 * WKJ_JS_EXCEPTION.
 */
WKJ_EXPORT int32_t wkj_js_set_member(int64_t peer, int32_t peer_type,
                                     const uint16_t* name, int32_t name_length,
                                     const WKJJSValue* value, wkj_ref acc,
                                     WKJJSValue* out);

/* removeMember(name). Was Java_com_sun_webkit_dom_JSObject_removeMemberImpl. */
WKJ_EXPORT int32_t wkj_js_remove_member(int64_t peer, int32_t peer_type,
                                        const uint16_t* name, int32_t name_length);

/* getSlot(index). Was Java_com_sun_webkit_dom_JSObject_getSlotImpl. */
WKJ_EXPORT int32_t wkj_js_get_slot(int64_t peer, int32_t peer_type, int32_t index,
                                   WKJJSValue* out);

/* setSlot(index, value). Was Java_com_sun_webkit_dom_JSObject_setSlotImpl. */
WKJ_EXPORT int32_t wkj_js_set_slot(int64_t peer, int32_t peer_type, int32_t index,
                                   const WKJJSValue* value, wkj_ref acc);

/*
 * toString(). Was Java_com_sun_webkit_dom_JSObject_toStringImpl, whose Java String return is a
 * plain contract-13 string: WKJ_STR_OK, WKJ_STR_NULL when the peer names no live context
 * (the JNI version returned null there and threw nothing), or WKJ_STR_OVERFLOW.
 *
 * On WKJ_STR_OVERFLOW *result_handle is a retained string to finish the transfer with,
 * because toString can run a user-defined toString and must not be called twice, and
 * *result_length is the capacity required. *result_handle is 0 in every other case, and
 * result_handle itself must not be NULL.
 */
WKJ_EXPORT int32_t wkj_js_to_string(int64_t peer, int32_t peer_type,
                                    uint16_t* result_buf, int32_t result_cap,
                                    int32_t* result_length, int64_t* result_handle);

/*
 * call(methodName, args). Was Java_com_sun_webkit_dom_JSObject_callImpl. `args` may not be
 * NULL - a null Object[] is WKJ_JS_NULL_ARGUMENT, as it was a NullPointerException before -
 * but argc may be 0. A member that is not a function is not an error: the result is
 * WKJ_JS_KIND_UNDEFINED, which is what convertUndefinedToJObject returned there.
 */
WKJ_EXPORT int32_t wkj_js_call(int64_t peer, int32_t peer_type,
                               const uint16_t* name, int32_t name_length,
                               const WKJJSValue* args, int32_t argc, wkj_ref acc,
                               WKJJSValue* out);

/*
 * Drops the gc protection taken when the JSObject was created. Was
 * Java_com_sun_webkit_dom_JSObject_unprotectImpl, called from the SelfDisposer of JSObject,
 * and like it this does nothing for a peer that names no live context.
 */
WKJ_EXPORT void wkj_js_unprotect(int64_t peer, int32_t peer_type);

/* ======================================================================================== */
/* Finishing an oversized string                                                            */
/* ======================================================================================== */

/*
 * Copies a string kept alive by a non-zero string_handle or result_handle into the buffer of
 * the caller. WKJ_STR_OK when it fits; WKJ_STR_OVERFLOW with the required capacity in
 * *result_length when it does not, in which case the handle stays valid so that the caller
 * can size a buffer and try again; WKJ_STR_NULL for a handle of 0.
 */
WKJ_EXPORT int32_t wkj_js_string_copy(int64_t string_handle, uint16_t* result_buf,
                                      int32_t result_cap, int32_t* result_length);

/*
 * Releases a string handle. Every non-zero handle the library hands out must be released
 * exactly once, including after a successful wkj_js_string_copy. Releasing 0 is a no-op.
 */
WKJ_EXPORT void wkj_js_string_release(int64_t string_handle);

/* ======================================================================================== */
/* WebPage.twkExecuteScript                                                                 */
/* ======================================================================================== */

/*
 * Runs `script` in the global scope of the given frame and describes the result, exactly as
 * Java_com_sun_webkit_WebPage_twkExecuteScript did through WebCore::executeScript.
 *
 * It is declared and implemented here, rather than with the rest of the WebPage ABI, because
 * everything it does after finding the frame is LiveConnect: it is the same call to
 * executeScript that wkj_js_eval makes, and its result needs a WKJJSValue. A null script is
 * WKJ_JS_NULL_ARGUMENT, and a frame that is not a local frame writes WKJ_JS_KIND_NULL and
 * returns WKJ_JS_OK; both match the JNI version.
 *
 * OWED EDIT, for the owner of Source/WebKitLegacy/java: WebPage.cpp still carries
 * Java_com_sun_webkit_WebPage_twkExecuteScript, which called the environment-taking
 * WebCore::executeScript that no longer exists. That function is superseded by this one and
 * should be deleted, together with the now unused include of <WebCore/BridgeUtils.h>.
 */
WKJ_EXPORT int32_t wkj_frame_execute_script(int64_t pFrame,
                                            const uint16_t* script, int32_t script_length,
                                            WKJJSValue* out);

/* ======================================================================================== */
/* Layout self-checks                                                                       */
/* ======================================================================================== */

/*
 * The sizes of the three structs above, as the library sees them, for the binding test to
 * compare against its MemoryLayout.byteSize(). This is the cheapest guard there is against
 * the Java and C++ halves disagreeing about padding on one of the three platforms.
 */
WKJ_EXPORT int32_t wkj_bridge_sizeof_java_value(void);
WKJ_EXPORT int32_t wkj_bridge_sizeof_js_value(void);
WKJ_EXPORT int32_t wkj_bridge_sizeof_live_connect_host(void);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* WEBKIT_JAVA_API_BRIDGE_H */
