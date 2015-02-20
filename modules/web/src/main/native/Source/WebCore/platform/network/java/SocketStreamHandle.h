/*
 * Copyright (C) 2009 Apple Inc. All rights reserved.
 * Copyright (C) 2009, 2011 Google Inc.  All rights reserved.
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#ifndef SocketStreamHandle_h
#define SocketStreamHandle_h

#include "SocketStreamHandleBase.h"

#include <JavaRef.h>
#include <wtf/PassRefPtr.h>
#include <wtf/RefCounted.h>

namespace WebCore {

class Page;
class SocketStreamHandleClient;

class SocketStreamHandle : public RefCounted<SocketStreamHandle>, public SocketStreamHandleBase {
public:
    static PassRefPtr<SocketStreamHandle> create(const URL& url, Page* page, SocketStreamHandleClient* client) { return adoptRef(new SocketStreamHandle(url, page, client)); }

    virtual ~SocketStreamHandle();

    void didOpen();
    void didReceiveData(const char* data, int length);
    void didFail(int errorCode, const String& errorDescription);
    void didClose();

protected:
    // SocketStreamHandleBase functions.
    virtual int platformSend(const char* data, int length) override;
    virtual void platformClose() override;

private:
    SocketStreamHandle(const URL&, Page*, SocketStreamHandleClient*);

    JGObject m_ref;
};

}  // namespace WebCore

#endif  // SocketStreamHandle_h
