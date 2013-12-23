/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef IDNJava_h
#define IDNJava_h

#include <wtf/text/WTFString.h>

namespace WebCore {

namespace IDNJava {

String toASCII(const String& hostname);

} // namespace IDNJava

} // namespace WebCore

#endif // IDNJava_h
