/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include "config.h"

#include <wtf/CompletionHandler.h>
#include "NotImplemented.h"
#include "ResourceHandle.h"
#include "ResourceHandleInternal.h"
#include "com_sun_webkit_LoadListenerClient.h"

namespace WebCore {

ResourceHandleInternal::~ResourceHandleInternal()
{
}

ResourceHandle::~ResourceHandle()
{
}

bool ResourceHandle::start()
{
    ASSERT(!d->m_loader);
    d->m_loader = URLLoader::loadAsynchronously(context(), this, this->firstRequest());
    return d->m_loader != nullptr;
}

void ResourceHandle::cancel()
{
    if (d->m_loader) {
        d->m_loader->cancel();
        d->m_loader.reset();
    }
}

static bool shouldRedirectAsGET(const ResourceRequest& request, const ResourceResponse& response, bool crossOrigin)
{
    if (request.httpMethod() == "GET" || request.httpMethod() == "HEAD")
        return false;

    if (!request.url().protocolIsInHTTPFamily())
        return true;

    if (response.isSeeOther())
        return true;

    if ((response.isMovedPermanently() || response.isFound()) && (request.httpMethod() == "POST"))
        return true;

    if (crossOrigin && (request.httpMethod() == "DELETE"))
        return true;

    return false;
}

void ResourceHandle::willSendRequest(const ResourceResponse& response)
{
    ASSERT(isMainThread());

    ResourceRequest request = firstRequest();

    static const int maxRedirects = 20;
    if (d->m_redirectCount++ > maxRedirects) {
        client()->didFail(this, ResourceError(
            String(),
            com_sun_webkit_LoadListenerClient_TOO_MANY_REDIRECTS,
            request.url(),
            "Illegal redirect"));
        return;
    }

    if (response.httpStatusCode() == 307) {
        String lastHTTPMethod = d->m_lastHTTPMethod;
        if (!equalIgnoringASCIICase(lastHTTPMethod, request.httpMethod())) {
            request.setHTTPMethod(lastHTTPMethod);

            FormData* body = d->m_firstRequest.httpBody();
            if (!equalLettersIgnoringASCIICase(lastHTTPMethod, "get") && body && !body->isEmpty())
                request.setHTTPBody(body);

            String originalContentType = d->m_firstRequest.httpContentType();
            if (!originalContentType.isEmpty())
                request.setHTTPHeaderField(HTTPHeaderName::ContentType, originalContentType);
        }
    }

    String location = response.httpHeaderField(HTTPHeaderName::Location);
    URL newURL = URL(response.url(), location);
    bool crossOrigin = !protocolHostAndPortAreEqual(request.url(), newURL);

    ResourceRequest newRequest = request;
    newRequest.setURL(newURL);

    if (shouldRedirectAsGET(newRequest, response, crossOrigin)) {
        newRequest.setHTTPMethod("GET");
        newRequest.setHTTPBody(nullptr);
        newRequest.clearHTTPContentType();
    }

    if (crossOrigin) {
        // If the network layer carries over authentication headers from the original request
        // in a cross-origin redirect, we want to clear those headers here.
        newRequest.clearHTTPAuthorization();
        newRequest.clearHTTPOrigin();
    }

    // Should not set Referer after a redirect from a secure resource to non-secure one.
    if (!newURL.protocolIs("https") && protocolIs(newRequest.httpReferrer(), "https") && context()->shouldClearReferrerOnHTTPSToHTTPRedirect())
        newRequest.clearHTTPReferrer();

    client()->willSendRequestAsync(this, WTFMove(newRequest), ResourceResponse(response), [this, protectedThis = makeRef(*this)] (ResourceRequest&& request) {
        continueAfterWillSendRequest(WTFMove(request));
    });
}

void ResourceHandle::continueAfterWillSendRequest(ResourceRequest&& request)
{
    ASSERT(isMainThread());

    // willSendRequest might cancel the load.
    if (!d->m_loader || !client())
        return;

    cancel();
    if (request.isNull()) {
        return;
    }
    d->m_loader = URLLoader::loadAsynchronously(context(), this, request);
}

//utatodo: merge artifact
void ResourceHandle::platformLoadResourceSynchronously(NetworkingContext* context,
                                               const ResourceRequest& request,
                                               StoredCredentialsPolicy,
                                               ResourceError& error,
                                               ResourceResponse& response,
                                               Vector<char>& data)
{
    URLLoader::loadSynchronously(context, request, error, response, data);
}

void ResourceHandle::platformSetDefersLoading(bool)
{
    notImplemented();
}

void ResourceHandle::receivedCredential(const AuthenticationChallenge&, const Credential&)
{
    // Implement like ResourceHandleCurl
    notImplemented();
}

void ResourceHandle::receivedRequestToContinueWithoutCredential(const AuthenticationChallenge&)
{
    // Implement like ResourceHandleCurl
    notImplemented();
}

void ResourceHandle::receivedCancellation(const AuthenticationChallenge& challenge)
{
    if (challenge != d->m_currentWebChallenge)
        return;

    if (client())
        client()->receivedCancellation(this, challenge);
}

void ResourceHandle::receivedRequestToPerformDefaultHandling(const AuthenticationChallenge&)
{
    ASSERT_NOT_REACHED();
}

void ResourceHandle::receivedChallengeRejection(const AuthenticationChallenge&)
{
    ASSERT_NOT_REACHED();
}

} // namespace WebCore
