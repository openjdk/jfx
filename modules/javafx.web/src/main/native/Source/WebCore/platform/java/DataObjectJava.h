/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

#include "SharedBuffer.h"
#include <wtf/ListHashSet.h>
#include <wtf/URL.h>
#include <wtf/Vector.h>
#include <wtf/text/StringHash.h>

namespace WebCore {
// A data object for holding data that would be in a clipboard or moved
// during a drag-n-drop operation.  This is the data that WebCore is aware
// of and is not specific to a platform.
class DataObjectJava : public RefCounted<DataObjectJava> {
public:
    static const URL& emptyURL()            { static URL r; return r; }
    static const String& emptyString()      { static String r; return r; }

    static const String &mimePlainText()    { static String r("text/plain"_s); return r; }
    static const String &mimeHTML()         { static String r("text/html"_s); return r; }
    static const String &mimeURIList()      { static String r("text/uri-list"_s); return r; }
    static const String &mimeShortcutName() { static String r("text/ie-shortcut-filename"_s); return r; }

    // We provide the IE clipboard types (URL and Text),
    // and the clipboard types specified in the WHATWG Web Applications 1.0 draft
    // see http://www.whatwg.org/specs/web-apps/current-work/ Section 6.3.5.3
    static String normalizeMIMEType(const String& type)
    {
        String qType = type.convertToLowercaseWithoutLocale();
        // two special cases for IE compatibility
        if (qType == "text"_s || qType.startsWith("text/plain;"_s))
            return mimePlainText();
        if (qType == "url"_s)
            return mimeURIList();
        return qType;
    }

    static RefPtr<DataObjectJava> create()
    {
        return adoptRef(new DataObjectJava);
    }

    RefPtr<DataObjectJava> copy() const
    {
        return adoptRef(new DataObjectJava(*this));
    }

    void clear() {
        m_availMimeTypes.clear();
    }

    void clearData(const String& mimeType) {
        m_availMimeTypes.remove(mimeType);
    }

    bool hasData() const {
        return !m_availMimeTypes.isEmpty();
    }

    //setters
    void setURL(const URL &url, const String &urlTitle) {
        m_availMimeTypes.add(mimeURIList());
        m_availMimeTypes.add(mimeShortcutName());
        m_url = url;
        m_urlTitle = urlTitle;
        m_filenames.clear();
    }

    void setFiles(const Vector<String> &filenames) {
        m_availMimeTypes.add(mimeURIList());
        clearData(mimeShortcutName());
        m_url = emptyURL();
        m_urlTitle = emptyString();
        m_filenames = filenames;
    }

    void setPlainText(const String &plainText){
        m_availMimeTypes.add(mimePlainText());
        m_plainText = plainText;
    }

    void setHTML(const String &textHtml, const URL &htmlBaseUrl) {
        m_availMimeTypes.add(mimeHTML());
        m_textHtml = textHtml;
        m_htmlBaseUrl = htmlBaseUrl;
    }

    bool setData(const String& mimeType, const String& data) {
        bool succeeded = true;
        String canonicalMimeType = normalizeMIMEType(mimeType);
        if (canonicalMimeType == mimeURIList())
            setURL(URL({ }, data), emptyString());
        else if (canonicalMimeType == mimeHTML())
            setHTML(data, emptyURL());
        else if (canonicalMimeType == mimePlainText()) // two special cases for IE compatibility
            setPlainText(data);
        else if (canonicalMimeType == mimeShortcutName())
            m_urlTitle = data; //activates by previous setUrl call
        else
            succeeded = false;
        return succeeded;
    }

    //getters
    //URL
    Vector<String> types() {
        Vector<String> types;
        types.appendRange(m_availMimeTypes.begin(), m_availMimeTypes.end());
        return types; //returns MIME Types available in clipboard
    }

    String getData(const String& mimeType) {
        String canonicalMimeType = normalizeMIMEType(mimeType);
        String ret;
        if (canonicalMimeType == mimeURIList())
            ret = asURL();
        else if (canonicalMimeType == mimeHTML())
            ret = asHTML();
        else if (canonicalMimeType == mimePlainText())
            ret = asPlainText();
        else if (canonicalMimeType == mimeShortcutName())
            ret = m_urlTitle;
        return ret;
    }

    bool containsURL() const {
        return m_availMimeTypes.contains(mimeURIList());
    }

    String asURL(String* title = NULL) const
    {
        if (!containsURL())
            return String();

        if(m_url.isEmpty() && !m_filenames.isEmpty())
            return m_filenames.at(0);

        // |title| can be NULL
        if (title)
            *title = m_urlTitle;
        return m_url.string();
    }

    //File List
    bool containsFiles() const {
        return containsURL();
    }

    Vector<String> asFilenames() const {
        Vector<String> result {};
        if(m_url.isEmpty() && !m_filenames.isEmpty())
            result = m_filenames;
        else if(!m_url.isEmpty())
            result.append(m_url.string());
        return result;
    }

    //Plain Text
    bool containsPlainText() const {
        return m_availMimeTypes.contains(mimePlainText());
    }
    String asPlainText() const {
        return m_plainText;
    }


    bool containsHTML() const {
        return m_availMimeTypes.contains(mimeHTML());
    }
    String asHTML(String* baseURL = NULL) const
    {
        if (!containsHTML())
            return String();

        // |baseURL| can be NULL
        if (baseURL)
            *baseURL = m_htmlBaseUrl.string();
        return m_textHtml;
    }

    // tav todo: where and how it's supposed to be used?
    String m_fileContentFilename;
    RefPtr<FragmentedSharedBuffer> m_fileContent;

    ~DataObjectJava() {
    }

    const Vector<String>& filenames() const {
        return m_filenames;
    }

private:
    ListHashSet<String> m_availMimeTypes;

    //URL
    URL m_url;
    String m_urlTitle;
    Vector<String> m_filenames;

    //plain text
    String m_plainText;

    //html text
    String m_textHtml;
    URL m_htmlBaseUrl;

    DataObjectJava() {
    }

    DataObjectJava(const DataObjectJava& data) :
        RefCounted<WebCore::DataObjectJava>(),
        m_availMimeTypes(data.m_availMimeTypes),
        m_url(data.m_url),
        m_urlTitle(data.m_urlTitle),
        m_filenames(data.m_filenames),
        m_plainText(data.m_plainText),
        m_textHtml(data.m_textHtml),
        m_htmlBaseUrl(data.m_htmlBaseUrl)
    {
    }
};
} // namespace WebCore
