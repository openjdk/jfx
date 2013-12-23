/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef TextCodecJava_h
#define TextCodecJava_h

#include "TextCodec.h"
#include "TextEncoding.h"
#include "JavaEnv.h"

namespace WebCore {

class TextCodecJava : public TextCodec {
public:
    static void registerEncodingNames(EncodingNameRegistrar);
    static void registerCodecs(TextCodecRegistrar);

    TextCodecJava(const TextEncoding&);
    virtual ~TextCodecJava();

    virtual String decode(const char*, size_t length, bool flush,
                                            bool stopOnError, bool& sawError);
    virtual CString encode(
                const UChar*, size_t length, UnencodableHandling);

private:
    TextEncoding m_encoding;
    jobject m_codec;
};

}

#endif
