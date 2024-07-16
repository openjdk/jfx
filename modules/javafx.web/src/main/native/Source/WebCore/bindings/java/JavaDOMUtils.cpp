/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

/* utility functions */

#include "config.h"

#include "Document.h"
#include "Frame.h"
#include "Element.h"
#include "HTMLDocument.h"
#include "HTMLElement.h"
#include "HTMLFrameOwnerElement.h"
#include "DOMException.h"
#include "LocalFrame.h"

#include <wtf/java/JavaEnv.h>

#include "JavaDOMUtils.h"
#include "runtime_root.h"

class LocalFrame;

namespace WebCore {

static void raiseDOMErrorException(JNIEnv* env, WebCore::ExceptionCode ec)
{
    ASSERT(ec);

    auto description = DOMException::description(ec);

    static JGClass exceptionClass(env->FindClass("org/w3c/dom/DOMException"));
    static jmethodID midCtor = env->GetMethodID(exceptionClass, "<init>", "(SLjava/lang/String;)V");

    ASSERT(midCtor);
    const char* message;
    if (description.name) {
        message = description.message;
    } else {
        message = "Unknown Exception";
    }
    env->Throw(JLocalRef<jthrowable>((jthrowable)env->NewObject(exceptionClass, midCtor, jshort(description.legacyCode), (jstring)String::fromLatin1(message).toJavaString(env))));
}

void raiseTypeErrorException(JNIEnv* env)
{
    raiseDOMErrorException(env, ExceptionCode::TypeError);
}

void raiseNotSupportedErrorException(JNIEnv* env)
{
    raiseDOMErrorException(env, ExceptionCode::NotSupportedError);
}

void raiseDOMErrorException(JNIEnv* env, Exception&& ec)
{
    raiseDOMErrorException(env, ec.code());
}
} // namespace WebCore


namespace WebCore {
extern "C" {
static jobject makeObjectFromNode(
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

JNIEXPORT jobject JNICALL Java_com_sun_webkit_WebPage_twkGetDocument
    (JNIEnv* env, jclass, jlong jframe)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(jframe));
    if (!frame)
        return nullptr;

    auto* localFrame = dynamicDowncast<LocalFrame>(frame);
    Document* document = localFrame->document();
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
    JNIEnv* env = WTF::GetJavaEnv();

    static JGClass clObject(env->FindClass("java/lang/Object"));
    static jmethodID midHash = env->GetMethodID(clObject, "hashCode", "()I");
    ASSERT(midHash);

    return env->CallIntMethod(clObject, midHash, o);
}

bool isJavaEquals(jobject o1, jobject o2)
{
    if (!o1)
        return !o2;

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID midEquals = env->GetMethodID(
        JLClass(env->FindClass("java/lang/Object")),
        "equals",
        "(Ljava/lang/Object;)Z");
    ASSERT(midEquals);

    return jbool_to_bool(env->CallBooleanMethod(o1, midEquals, o2));
}

} // namespace WebCore
