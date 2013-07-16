/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef StringJava_h
#define StringJava_h

#include <wtf/text/WTFString.h>
#include "Font.h"

namespace WebCore {
using WTF::String;
jobjectArray strVect2JArray(
    JNIEnv* env, const Vector<String>& strVect);

} // namespace WebCore
#endif
