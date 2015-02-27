/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"
#include "NotImplemented.h"
#include "TextBreakIterator.h"

#include "JavaEnv.h"

#include <wtf/Assertions.h>
#include <wtf/Atomics.h>
#include <wtf/text/WTFString.h>

#include "com_sun_webkit_text_TextBreakIterator.h"

#define JNI_EXPAND(n) com_sun_webkit_text_TextBreakIterator_##n

jclass getTextBreakIteratorClass() {
    static JGClass textBreakIteratorClass(WebCore_GetJavaEnv()->FindClass(
        "com/sun/webkit/text/TextBreakIterator"));
    return textBreakIteratorClass;
}

namespace WebCore {

static String textBreakLocale;
static String usLocale("en-US");
static bool isValidLocale = false;
static int lastType = -1;

void setTextBreakLocale(String locale)
{
    textBreakLocale = locale;
    isValidLocale = true;
}

static TextBreakIterator* setUpIterator(
    int type,
    const UChar* string,
    int length,
    bool create = false)
{
    if (!string)
        return NULL;

    JNIEnv* env = WebCore_GetJavaEnv();
    LOG_PERF_RECORD(env, "XXXX", "setUpIterator")
    static jmethodID midGetIterator = env->GetStaticMethodID(
        getTextBreakIteratorClass(),
        "getIterator",
        "(ILjava/lang/String;Ljava/lang/String;Z)Ljava/text/BreakIterator;");
    ASSERT(midGetIterator);

    isValidLocale = (lastType == -1 || type == lastType);
    const String &locale = isValidLocale && !textBreakLocale.isNull()
        ? textBreakLocale
        : usLocale;

    JLString jLocale(locale.toJavaString(env));
    ASSERT(jLocale);
    JLString jText(env->NewString(reinterpret_cast<const jchar*>(string), length));
    ASSERT(jText);

    if (CheckAndClearException(env))
        return NULL; //OOME

    JLObject iterator(env->CallStaticObjectMethod(
        getTextBreakIteratorClass(),
        midGetIterator,
        (jint)type,
        (jstring)jLocale,
        (jstring)jText,
        (jboolean)create));
    CheckAndClearException(env);
    ASSERT(iterator);

    lastType = type;
    return reinterpret_cast<TextBreakIterator*>(JGObject(iterator).releaseGlobal());

/*
    return reinterpret_cast<TextBreakIterator*>(create
        ? JGObject(iterator).releaseGlobal()
        : (jobject)iterator);
*/
}

static int invokeTextBreakMethod(TextBreakIterator* bi, int method, int pos)
{
    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID midInvokeMethod = env->GetStaticMethodID(
        getTextBreakIteratorClass(),
        "invokeMethod",
        "(Ljava/text/BreakIterator;II)I");
    ASSERT(midInvokeMethod);

    jint n = env->CallStaticIntMethod(
        getTextBreakIteratorClass(),
        midInvokeMethod,
        reinterpret_cast<jobject>(bi),
        method,
        pos);
    CheckAndClearException(env);

    return n;
}


TextBreakIterator* characterBreakIterator(const UChar* string, int length)
{
    return setUpIterator(JNI_EXPAND(CHARACTER_ITERATOR), string, length);
}

TextBreakIterator* wordBreakIterator(const UChar* string, int length)
{
    return setUpIterator(JNI_EXPAND(WORD_ITERATOR), string, length);
}

TextBreakIterator* cursorMovementIterator(const UChar* string, int length)
{
    return characterBreakIterator(string, length);
}

//TextBreakIterator* lineBreakIterator(const UChar* string, int length)
//UTATODO: need to recycle staticLineBreakIterator
TextBreakIterator* acquireLineBreakIterator(const UChar* string, int length, const AtomicString& locale, const UChar* priorContext, unsigned priorContextLength)
{
    return setUpIterator(JNI_EXPAND(LINE_ITERATOR), string, length);
}

// tav todo see TextBreakIterator impl
//void releaseLineBreakIterator(TextBreakIterator* iterator)
//{
//    ASSERT(createdLineBreakIterator);
//    ASSERT(iterator);

//    if (!staticLineBreakIterator)
//        staticLineBreakIterator = iterator;
//    else
//        delete iterator;
//}

TextBreakIterator* sentenceBreakIterator(const UChar* string, int length)
{
    return setUpIterator(JNI_EXPAND(SENTENCE_ITERATOR), string, length);
}

// tav todo see TextBreakIterator impl
//int textBreakFirst(TextBreakIterator* bi) {
//    return invokeTextBreakMethod(bi, JNI_EXPAND(TEXT_BREAK_FIRST), 0);
//}

// tav todo see TextBreakIterator impl
// int textBreakLast(TextBreakIterator* bi) {
//     return invokeTextBreakMethod(bi, JNI_EXPAND(TEXT_BREAK_LAST), 0);
// }

// tav todo see TextBreakIterator impl
// int textBreakNext(TextBreakIterator* bi) {
//     return invokeTextBreakMethod(bi, JNI_EXPAND(TEXT_BREAK_NEXT), 0);
// }

// tav todo see TextBreakIterator impl
// int textBreakPrevious(TextBreakIterator* bi) {
//     return invokeTextBreakMethod(bi, JNI_EXPAND(TEXT_BREAK_PREVIOUS), 0);
// }

// tav todo see TextBreakIterator impl
// int textBreakCurrent(TextBreakIterator* bi) {
//     return invokeTextBreakMethod(bi, JNI_EXPAND(TEXT_BREAK_CURRENT), 0);
// }

// tav todo see TextBreakIterator impl
// int textBreakPreceding(TextBreakIterator* bi, int pos) {
//     return invokeTextBreakMethod(bi, JNI_EXPAND(TEXT_BREAK_PRECEDING), pos);
// }

// tav todo see TextBreakIterator impl
// int textBreakFollowing(TextBreakIterator* bi, int pos) {
//     return invokeTextBreakMethod(bi, JNI_EXPAND(TEXT_BREAK_FOLLOWING), pos);
// }

// tav todo see TextBreakIterator impl
// bool isTextBreak(TextBreakIterator* bi, int pos) {
//     return invokeTextBreakMethod(bi, JNI_EXPAND(IS_TEXT_BREAK), pos);
// }

// tav todo see TextBreakIterator impl
// bool isWordTextBreak(TextBreakIterator* bi)
// {
//     return invokeTextBreakMethod(bi, JNI_EXPAND(IS_WORD_TEXT_BREAK), 0);
// }

// tav todo see TextBreakIterator impl
//static TextBreakIterator* nonSharedCharacterBreakIterator;

inline bool _weakCompareAndSwap(void*volatile* location, void *expected, void *newValue)
{
#if ENABLE(COMPARE_AND_SWAP)
    return WTF::weakCompareAndSwap(location, expected, newValue);
#else
    if (*location == expected) {
        *location = newValue;
        return true;
    }
    return false;
#endif
}

// tav todo see TextBreakIterator impl
// NonSharedCharacterBreakIterator::NonSharedCharacterBreakIterator(StringView string)
// {
//     m_iterator = nonSharedCharacterBreakIterator;
//     bool createdIterator = m_iterator && _weakCompareAndSwap(reinterpret_cast<void**>(&nonSharedCharacterBreakIterator), m_iterator, 0);
//     if (!createdIterator) {
//          m_iterator = setUpIterator(JNI_EXPAND(CHARACTER_ITERATOR), string.characters16(), string.length(), true);
//     }
// }

// tav todo see TextBreakIterator impl
// NonSharedCharacterBreakIterator::~NonSharedCharacterBreakIterator()
// {
//     if (!_weakCompareAndSwap(reinterpret_cast<void**>(&nonSharedCharacterBreakIterator), 0, m_iterator)) {
//         //delete m_iterator;
//         JNIEnv* env = JavaScriptCore_GetJavaEnv();
//         if (env && m_iterator) {
//             //uta: that is ok - m_iterator is a global java object
//             env->DeleteGlobalRef(reinterpret_cast<jobject>(m_iterator));
//         }
//     }
// }

} // namespace WebCore

#undef JNI_EXPAND
