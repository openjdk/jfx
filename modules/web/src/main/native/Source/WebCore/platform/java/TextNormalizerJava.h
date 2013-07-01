/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef TextNormalizerJava_h
#define TextNormalizerJava_h

#include "PlatformString.h"

#include "com_sun_webkit_text_TextNormalizer.h"
#define JNI_EXPAND(n) com_sun_webkit_text_TextNormalizer_##n

namespace WebCore {

namespace TextNormalizer {
    enum Form {
      NFC = JNI_EXPAND(FORM_NFC),
      NFD = JNI_EXPAND(FORM_NFD),
      NFKC = JNI_EXPAND(FORM_NFKC),
      NFKD = JNI_EXPAND(FORM_NFKD),
    };

    String normalize(const UChar* data, int length, Form form);
};
}

#undef JNI_EXPAND
#endif
