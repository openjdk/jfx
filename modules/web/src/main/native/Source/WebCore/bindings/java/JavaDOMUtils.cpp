/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
/* utility functions */

#include "config.h"

#include "Document.h"
#include "Frame.h"
#include "Element.h"
#include "HTMLDocument.h"
#include "HTMLElement.h"
#include "JavaEnv.h"

#include "BridgeUtils.h"
#include "JavaDOMUtils.h"
#include "runtime_root.h"

using namespace WebCore;

#ifdef __cplusplus
extern "C" {
#endif

jobject makeObjectFromNode(
    JNIEnv* env,
    Frame* frame,
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

JNIEXPORT jobject JNICALL Java_com_sun_webkit_WebPage_twkGetDocument
    (JNIEnv* env, jclass clazz, jlong jframe)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(jframe));
    if (!frame)
        return NULL;

    Document* document = frame->document();
    if (!document)
        return NULL;

    return makeObjectFromNode(env, frame, document);
}

JNIEXPORT jobject JNICALL Java_com_sun_webkit_WebPage_twkGetOwnerElement
    (JNIEnv* env, jclass clazz, jlong jframe)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(jframe));
    if (!frame)
        return NULL;

    Element* ownerElement = (Element*) frame->ownerElement();
    if (!ownerElement)
        return NULL;

    return makeObjectFromNode(env, frame, ownerElement);
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

#ifdef __cplusplus
}
#endif
