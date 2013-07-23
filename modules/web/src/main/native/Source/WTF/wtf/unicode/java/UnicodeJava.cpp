/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include <wtf/java/JavaRef.h>
#include "UnicodeJava.h"

#include <wtf/Assertions.h>
#include <wtf/HashMap.h>

#include <jni.h>
#include <string.h>

static jclass characterClass;
static jclass stringCaseClass;

// Build generic function that calls static method of java.lang.Character class
#define CreateCharFunction(fType, name, jType, signature, eType) \
    fType name(uint32_t c) { \
        static jmethodID mid; \
        JNIEnv* env = setUpClass(&characterClass, "java/lang/Character"); \
        if (! mid) { \
            mid = env->GetStaticMethodID(characterClass, #name, signature); \
            ASSERT(mid); \
        } \
        jType r = env->CallStatic##eType##Method(characterClass, mid, c); \
        CheckAndClearException(env); \
        return static_cast<fType>(r); \
    }

#define CreateStringCaseFunction(name) \
    int name(uint16_t* dst, int dstLength, \
             const uint16_t* src, int srcLength, bool* err) { \
        static jmethodID mid; \
        return stringCaseConvert(#name, &mid, \
                                 dst, dstLength, src, srcLength, err); \
    }


static JNIEnv* setUpClass(jclass* aClass, const char* className)
{
    JNIEnv* env = JavaScriptCore_GetJavaEnv();
    if (!*aClass) {
        jclass cls = env->FindClass(className);
        ASSERT(cls);
        *aClass = static_cast<jclass>(env->NewGlobalRef(cls));
        ASSERT(*aClass);
        env->DeleteLocalRef(cls);
    }
    return env;
}

static int stringCaseConvert(const char* methodName, jmethodID* mid,
                             uint16_t* dst, int dstLength,
                             const uint16_t* src, int srcLength, bool* err)
{
    *err = false;

    if ((dstLength < 0) || (srcLength < 0)) {
        *err = true;
        return dstLength;
    }
    
    if (srcLength == 0) {
        return 0;
    }
    
    JNIEnv* env = setUpClass(&stringCaseClass,
                      "com/sun/webkit/text/StringCase");

    if (!*mid) {
        *mid = env->GetStaticMethodID(stringCaseClass,
                        methodName, "(Ljava/lang/String;)Ljava/lang/String;");
        ASSERT(*mid);
    }

    jstring jSrc = env->NewString(src, srcLength);
    ASSERT(jSrc);
    
    jstring jDst = static_cast<jstring>(env->CallStaticObjectMethod(
                                                stringCaseClass, *mid, jSrc));
    CheckAndClearException(env);
    ASSERT(jDst);
    env->DeleteLocalRef(jSrc);
    jint length = env->GetStringLength(jDst);
    if (length > dstLength) {
        *err = true;
    } else {
        const jchar* chars = env->GetStringChars(jDst, NULL);
        ASSERT(chars);
        memcpy(dst, chars, length * sizeof(jchar));
        env->ReleaseStringChars(jDst, chars);
    }
    
    env->DeleteLocalRef(jDst);
    return length;
}

// Exported interface
namespace WTF {
  namespace Unicode {
    namespace Java {

      // For use in DeprecatedString.h
      CreateCharFunction(bool, isSpaceChar, jboolean, "(I)Z", Boolean);
      CreateCharFunction(bool, isLetterOrDigit, jboolean, "(I)Z", Boolean);

      CreateCharFunction(uint32_t, toLowerCase, jint, "(I)I", Int);
      CreateCharFunction(uint32_t, toUpperCase, jint, "(I)I", Int);
      CreateCharFunction(uint32_t, toTitleCase, jint, "(I)I", Int);

      CreateCharFunction(int, getType, jint, "(I)I", Int);
      CreateCharFunction(int, getNumericValue, jint, "(I)I", Int);
      CreateCharFunction(int, getDirectionality, jbyte, "(I)B", Byte);

      CreateStringCaseFunction(toLowerCase);
      CreateStringCaseFunction(toUpperCase);
      CreateStringCaseFunction(foldCase);
    }

    //not a Java
    static const UChar32 g_mirrorPairs[][2] = {
#include "mirrorPairs.h"
    };
    UChar32 mirroredChar(UChar32 c){
        static HashMap<UChar32, UChar32> mapPairs;
        if ( mapPairs.isEmpty() ) {
            for (int i=0; i<sizeof(g_mirrorPairs)/sizeof(*g_mirrorPairs); ++i) {
                mapPairs.add(g_mirrorPairs[i][0], g_mirrorPairs[i][1]);
            }
        }
        //http://www.unicode.org/Public/5.2.0/ucd/BidiMirroring.txt
        //There is only boolean support in Java:  boolean Caracter.isMirrored(int ch),
        //no idea about mirror code. So, use auto-generated table.
        UChar32 cM = mapPairs.get(c);
        if( 0!=cM ){
            return cM;
        }
        return c;
    }
  }
}
