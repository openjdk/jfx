/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "JavaEnv.h"
#include "TextNormalizerJava.h"

namespace WebCore {

namespace TextNormalizer {

    static JGClass textNormalizerClass;
    static jmethodID normalizeMID;

    static JNIEnv* setUpNormalizer(void)
    {
        JNIEnv* env = WebCore_GetJavaEnv();

        if (! textNormalizerClass) {
            textNormalizerClass = JLClass(env->FindClass(
                         "com/sun/webkit/text/TextNormalizer"));
            ASSERT(textNormalizerClass);
            normalizeMID = env->GetStaticMethodID(textNormalizerClass,
                    "normalize", "(Ljava/lang/String;I)Ljava/lang/String;");
            ASSERT(normalizeMID);
        }

        return env;
    }

    String normalize(const UChar* data, int length, Form form)
    {
        JNIEnv *env = setUpNormalizer();

        JLString jData(env->NewString(reinterpret_cast<const jchar*>(data), length));
        ASSERT(jData);
        CheckAndClearException(env); // OOME

        JLString s(static_cast<jstring>(env->CallStaticObjectMethod(
                textNormalizerClass, normalizeMID, (jstring)jData, form)));
        ASSERT(s);
        CheckAndClearException(env);

        return String(env, s);
    }

} // namespace TextNormalizer

} // namespace WebCore
