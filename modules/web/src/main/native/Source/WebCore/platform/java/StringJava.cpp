/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "JavaEnv.h"
#include "StringJava.h"

namespace WebCore {

using WTF::String;

jobjectArray strVect2JArray(JNIEnv* env, const Vector<String>& strVect)
{
    if (!strVect.size()) {
        jobjectArray arr = (jobjectArray) env->NewObjectArray(0,
            JLClass(env->FindClass("java/lang/String")), 0);
        CheckAndClearException(env); // OOME
        return arr;
    }

    ASSERT(strVect[0]);
    JLString str(strVect[0].toJavaString(env));

    JLClass sclass(env->GetObjectClass(str));
    jobjectArray strArray =
        (jobjectArray) env->NewObjectArray(strVect.size(), sclass, 0);
    CheckAndClearException(env); // OOME

    env->SetObjectArrayElement(strArray, 0, (jstring)str);

    for (int i = 1; i < strVect.size(); i++) {
        ASSERT(strVect[i]);
        str = strVect[i].toJavaString(env);
        env->SetObjectArrayElement(strArray, i, (jstring)str);
    }

    return strArray;
}

} // namespace WebCore
