/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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
                                                    ResourceHandle* handle);
    void cancel();
    static void loadSynchronously(NetworkingContext* context,
                                  const ResourceRequest& request,
                                  ResourceError& error,
                                  ResourceResponse& response,
                                  Vector<char>& data);
    ~URLLoader();

    class Target {
    public:
        virtual void didSendData(long totalBytesSent,
                                 long totalBytesToBeSent) = 0;
        virtual bool willSendRequest(const String& newUrl,
                                     const String& newMethod,
                                     const ResourceResponse& response) = 0;
        virtual void didReceiveResponse(const ResourceResponse& response) = 0;
        virtual void didReceiveData(const char* data, int length) = 0;
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
        bool willSendRequest(const String& newUrl,
                             const String& newMethod,
                             const ResourceResponse& response) final;
        void didReceiveResponse(const ResourceResponse& response) final;
        void didReceiveData(const char* data, int length) final;
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
                          Vector<char>& data);

        void didSendData(long totalBytesSent, long totalBytesToBeSent) final;
        bool willSendRequest(const String& newUrl,
                             const String& newMethod,
                             const ResourceResponse& response) final;
        void didReceiveResponse(const ResourceResponse& response) final;
        void didReceiveData(const char* data, int length) final;
        void didFinishLoading() final;
        void didFail(const ResourceError& error) final;
    private:
        const ResourceRequest& m_request;
        ResourceError& m_error;
        ResourceResponse& m_response;
        Vector<char>& m_data;
    };

    JGObject m_ref;
    std::unique_ptr<AsynchronousTarget> m_target;
};

} // namespace WebCore
