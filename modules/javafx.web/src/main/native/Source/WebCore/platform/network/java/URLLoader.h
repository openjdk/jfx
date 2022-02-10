/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#pragma once

#include <wtf/java/JavaRef.h>
#include <wtf/Vector.h>
#include <wtf/text/WTFString.h>

namespace WebCore {

class FormData;
class NetworkingContext;
class ResourceError;
class ResourceHandle;
class ResourceRequest;
class ResourceResponse;

class URLLoader {
public:
    static std::unique_ptr<URLLoader> loadAsynchronously(NetworkingContext* context,
                                                    ResourceHandle* handle,
                                                    const ResourceRequest& request);
    void cancel();
    static void loadSynchronously(NetworkingContext* context,
                                  const ResourceRequest& request,
                                  ResourceError& error,
                                  ResourceResponse& response,
                                  Vector<uint8_t>& data);
    ~URLLoader();

    class Target {
    public:
        virtual void didSendData(long totalBytesSent,
                                 long totalBytesToBeSent) = 0;
        virtual bool willSendRequest(const ResourceResponse& response) = 0;
        virtual void didReceiveResponse(const ResourceResponse& response) = 0;
        virtual void didReceiveData(const uint8_t* data, int length) = 0;
        virtual void didFinishLoading() = 0;
        virtual void didFail(const ResourceError& error) = 0;
        virtual ~Target();
    };

private:
    URLLoader();

    static JLObject load(bool asynchronous,
                         NetworkingContext* context,
                         const ResourceRequest& request,
                         Target* target);
    static JLObjectArray toJava(const FormData* formData);

    class AsynchronousTarget : public Target {
    public:
        AsynchronousTarget(ResourceHandle* handle);

        void didSendData(long totalBytesSent, long totalBytesToBeSent) final;
        bool willSendRequest(const ResourceResponse& response) final;
        void didReceiveResponse(const ResourceResponse& response) final;
        void didReceiveData(const uint8_t* data, int length) final;
        void didFinishLoading() final;
        void didFail(const ResourceError& error) final;
    private:
        ResourceHandle* m_handle;
    };

    class SynchronousTarget : public Target {
    public:
        SynchronousTarget(const ResourceRequest& request,
                          ResourceError& error,
                          ResourceResponse& response,
                          Vector<uint8_t>& data);

        void didSendData(long totalBytesSent, long totalBytesToBeSent) final;
        bool willSendRequest(const ResourceResponse& response) final;
        void didReceiveResponse(const ResourceResponse& response) final;
        void didReceiveData(const uint8_t* data, int length) final;
        void didFinishLoading() final;
        void didFail(const ResourceError& error) final;
    private:
        const ResourceRequest& m_request;
        ResourceError& m_error;
        ResourceResponse& m_response;
        Vector<uint8_t>& m_data;
    };

    JGObject m_ref;
    std::unique_ptr<AsynchronousTarget> m_target;
};

} // namespace WebCore
