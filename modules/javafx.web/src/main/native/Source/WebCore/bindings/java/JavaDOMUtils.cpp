/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
 */
/* utility functions */

#include "config.h"

#include "Document.h"
#include "Frame.h"
#include "Element.h"
#include "HTMLDocument.h"
#include "HTMLElement.h"
#include "Exception.h"
#include "ExceptionCodeDescription.h"

#include <wtf/java/JavaEnv.h>

#include "JavaDOMUtils.h"
#include "runtime_root.h"

namespace WebCore {

static void raiseDOMErrorException(JNIEnv* env, WebCore::ExceptionCode ec)
{
    ASSERT(ec);

    WebCore::ExceptionCodeDescription description(ec);

    // FIXME: This should use type and code exclusively and not try to use typeName.
    // lazy init
    jclass clz = 0L;
    jmethodID  mid = 0;
    if (strcmp(description.typeName, "DOM Range") == 0) {
        static JGClass exceptionClass(env->FindClass("org/w3c/dom/ranges/RangeException"));
        static jmethodID midCtor = env->GetMethodID(exceptionClass, "<init>", "(SLjava/lang/String;)V");
        clz = exceptionClass;
        mid = midCtor;
    } else if (strcmp(description.typeName, "DOM Events") == 0) {
        static JGClass exceptionClass(env->FindClass("org/w3c/dom/events/EventException"));
        static jmethodID midCtor = env->GetMethodID(exceptionClass, "<init>", "(SLjava/lang/String;)V");
        clz = exceptionClass;
        mid = midCtor;
    } else {
        static JGClass exceptionClass(env->FindClass("org/w3c/dom/DOMException"));
        static jmethodID midCtor = env->GetMethodID(exceptionClass, "<init>", "(SLjava/lang/String;)V");
        clz = exceptionClass;
        mid = midCtor;
    }

    ASSERT(mid);
    env->Throw(JLocalRef<jthrowable>((jthrowable)env->NewObject(clz, mid, (jshort)ec, (jstring)String(description.description).toJavaString(env))));
}

void raiseTypeErrorException(JNIEnv* env)
{
    raiseDOMErrorException(env, WebCore::TypeError);
}

void raiseNotSupportedErrorException(JNIEnv* env)
{
    raiseDOMErrorException(env, WebCore::NOT_SUPPORTED_ERR);
}

void raiseDOMErrorException(JNIEnv* env, Exception&& ec)
{
    raiseDOMErrorException(env, ec.code());
}
} // namespace WebCore


namespace WebCore {
extern "C" {
namespace {
jobject makeObjectFromNode(
    JNIEnv* env,
    Frame*,
    Node* peer)
{
    static JGClass clNode(env->FindClass("com/sun/webkit/dom/NodeImpl"));
    static jmethodID midGetImpl = env->GetStaticMethodID(clNode, "getImpl", "(J)Lorg/w3c/dom/Node;");
    ASSERT(midGetImpl);

    peer->ref(); //deref is in NodeImpl disposer
    return env->CallStaticObjectMethod(
        clNode,
        midGetImpl,
        ptr_to_jlong(peer));
}
} // namespace

JNIEXPORT jobject JNICALL Java_com_sun_webkit_WebPage_twkGetDocument
    (JNIEnv* env, jclass, jlong jframe)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(jframe));
    if (!frame)
        return nullptr;

    Document* document = frame->document();
    if (!document)
        return nullptr;

    return makeObjectFromNode(env, frame, document);
}

JNIEXPORT jobject JNICALL Java_com_sun_webkit_WebPage_twkGetOwnerElement
    (JNIEnv* env, jclass, jlong jframe)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(jframe));
    if (!frame)
        return nullptr;

    Element* ownerElement = (Element*) frame->ownerElement();
    if (!ownerElement)
        return nullptr;

    return makeObjectFromNode(env, frame, ownerElement);
}
}


uint32_t getJavaHashCode(jobject o)
{
    JNIEnv* env = WebCore_GetJavaEnv();

    static JGClass clObject(env->FindClass("java/lang/Object"));
    static jmethodID midHash = env->GetMethodID(clObject, "hashCode", "()I");
    ASSERT(midHash);

    return env->CallIntMethod(clObject, midHash, o);
}

bool isJavaEquals(jobject o1, jobject o2)
{
    if (!o1)
        return !o2;

    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID midEquals = env->GetMethodID(
        JLClass(env->FindClass("java/lang/Object")),
        "equals",
        "(Ljava/lang/Object;)Z");
    ASSERT(midEquals);

    return jbool_to_bool(env->CallBooleanMethod(o1, midEquals, o2));
}

} // namespace WebCore
