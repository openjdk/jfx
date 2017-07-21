/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 */

#include "config.h"

#include "CryptoDigest.h"
#undef WEBCORE_EXPORT
#define WEBCORE_EXPORT
#include "NotImplemented.h"

namespace PAL {

struct CryptoDigestContext { };

CryptoDigest::CryptoDigest()
{
    notImplemented();
}

CryptoDigest::~CryptoDigest()
{
    notImplemented();
}

std::unique_ptr<CryptoDigest> CryptoDigest::create(CryptoDigest::Algorithm)
{
    notImplemented();
    return std::unique_ptr<CryptoDigest>(new CryptoDigest);
}

void CryptoDigest::addBytes(const void*, size_t)
{
    notImplemented();
}

Vector<uint8_t> CryptoDigest::computeHash()
{
    notImplemented();
    return {};
}

} // namespace PAL
