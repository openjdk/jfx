/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef URLLoader_h
#define URLLoader_h

#include <JavaRef.h>
#include <wtf/OwnPtr.h>
#include <wtf/PassOwnPtr.h>
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
    static PassOwnPtr<URLLoader> loadAsynchronously(NetworkingContext* context,
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

        virtual void didSendData(long totalBytesSent, long totalBytesToBeSent);
        virtual bool willSendRequest(const String& newUrl,
                                     const String& newMethod,
                                     const ResourceResponse& response);
        virtual void didReceiveResponse(const ResourceResponse& response);
        virtual void didReceiveData(const char* data, int length);
        virtual void didFinishLoading();
        virtual void didFail(const ResourceError& error);
    private:
        ResourceHandle* m_handle;
    };

    class SynchronousTarget : public Target {
    public:
        SynchronousTarget(const ResourceRequest& request,
                          ResourceError& error,
                          ResourceResponse& response,
                          Vector<char>& data);

        virtual void didSendData(long totalBytesSent, long totalBytesToBeSent);
        virtual bool willSendRequest(const String& newUrl,
                                     const String& newMethod,
                                     const ResourceResponse& response);
        virtual void didReceiveResponse(const ResourceResponse& response);
        virtual void didReceiveData(const char* data, int length);
        virtual void didFinishLoading();
        virtual void didFail(const ResourceError& error);
    private:
        const ResourceRequest& m_request;
        ResourceError& m_error;
        ResourceResponse& m_response;
        Vector<char>& m_data;
    };

    JGObject m_ref;
    OwnPtr<AsynchronousTarget> m_target;
};

} // namespace WebCore

#endif // URLLoader_h
