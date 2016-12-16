/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 */

#include "config.h"

#include "KeyedCoding.h"
#include "NotImplemented.h"

namespace WebCore {

std::unique_ptr<KeyedEncoder> KeyedEncoder::encoder()
{
    notImplemented();
    return nullptr;
}

std::unique_ptr<KeyedDecoder> KeyedDecoder::decoder(const uint8_t*, size_t)
{
    notImplemented();
    return nullptr;
}

}
