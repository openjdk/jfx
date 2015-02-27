/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 */

#include "config.h"
#include "PlatformStrategiesJava.h"

#include "NotImplemented.h"
#include "Page.h"
#include "PageGroup.h"
#include "PlatformCookieJar.h"
#include "PluginDatabase.h"
#include "PluginPackage.h"

using namespace WebCore;

void PlatformStrategiesJava::initialize()
{
    DEFINE_STATIC_LOCAL(PlatformStrategiesJava, platformStrategies, ());
    setPlatformStrategies(&platformStrategies);
}

PlatformStrategiesJava::PlatformStrategiesJava()
{
}

CookiesStrategy* PlatformStrategiesJava::createCookiesStrategy()
{
    return this;
}

DatabaseStrategy* PlatformStrategiesJava::createDatabaseStrategy()
{
    return this;
}

LoaderStrategy* PlatformStrategiesJava::createLoaderStrategy()
{
    return this;
}

PasteboardStrategy* PlatformStrategiesJava::createPasteboardStrategy()
{
    // This is currently used only by Mac code.
    notImplemented();
    return 0;
}

PluginStrategy* PlatformStrategiesJava::createPluginStrategy()
{
    return this;
}

SharedWorkerStrategy* PlatformStrategiesJava::createSharedWorkerStrategy()
{
    return this;
}

StorageStrategy* PlatformStrategiesJava::createStorageStrategy()
{
    return this;
}

VisitedLinkStrategy* PlatformStrategiesJava::createVisitedLinkStrategy()
{
    return this;
}

// CookiesStrategy
String PlatformStrategiesJava::cookiesForDOM(const NetworkStorageSession& session, const URL& firstParty, const URL& url)
{
    return WebCore::cookiesForDOM(session, firstParty, url);
}

void PlatformStrategiesJava::setCookiesFromDOM(const NetworkStorageSession& session, const URL& firstParty, const URL& url, const String& cookieString)
{
    WebCore::setCookiesFromDOM(session, firstParty, url, cookieString);
}

bool PlatformStrategiesJava::cookiesEnabled(const NetworkStorageSession& session, const URL& firstParty, const URL& url)
{
    return WebCore::cookiesEnabled(session, firstParty, url);
}

String PlatformStrategiesJava::cookieRequestHeaderFieldValue(const NetworkStorageSession& session, const URL& firstParty, const URL& url)
{
    return WebCore::cookieRequestHeaderFieldValue(session, firstParty, url);
}

bool PlatformStrategiesJava::getRawCookies(const NetworkStorageSession& session, const URL& firstParty, const URL& url, Vector<Cookie>& rawCookies)
{
    return WebCore::getRawCookies(session, firstParty, url, rawCookies);
}

void PlatformStrategiesJava::deleteCookie(const NetworkStorageSession& session, const URL& url, const String& cookieName)
{
    WebCore::deleteCookie(session, url, cookieName);
}

// PluginStrategy
void PlatformStrategiesJava::refreshPlugins()
{
    PluginDatabase::installedPlugins()->refresh();
}

void PlatformStrategiesJava::getPluginInfo(const Page* page, Vector<PluginInfo>& outPlugins)
{
    PluginDatabase* database = PluginDatabase::installedPlugins();
    const Vector<PluginPackage*> &plugins = database->plugins();

    for (size_t i = 0; i < plugins.size(); ++i) {
        PluginPackage* package = plugins[i];

        PluginInfo pluginInfo;
        pluginInfo.name = package->name();
        pluginInfo.file = package->fileName();
        pluginInfo.desc = package->description();

        const MIMEToDescriptionsMap& mimeToDescriptions = package->mimeToDescriptions();
        MIMEToDescriptionsMap::const_iterator end = mimeToDescriptions.end();
        for (MIMEToDescriptionsMap::const_iterator it = mimeToDescriptions.begin(); it != end; ++it) {
            MimeClassInfo mime;
            mime.type = it->key;
            mime.desc = it->value;
            mime.extensions = package->mimeToExtensions().get(mime.type);
            pluginInfo.mimes.append(mime);
        }

        outPlugins.append(pluginInfo);
    }
}

// VisitedLinkStrategy
bool PlatformStrategiesJava::isLinkVisited(Page* page, LinkHash hash, const URL&, const AtomicString&)
{
    return page->group().isLinkVisited(hash);
}

void PlatformStrategiesJava::addVisitedLink(Page* page, LinkHash hash)
{
    page->group().addVisitedLinkHash(hash);
}
