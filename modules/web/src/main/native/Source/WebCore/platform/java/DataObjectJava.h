/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef DataObjectJava_h
#define DataObjectJava_h

#include "config.h"

#include "SharedBuffer.h"
#include "StringHash.h"
#include <wtf/ListHashSet.h>
#include <wtf/Vector.h>
#include <URL.h>

//#define DS(x) String(x).charactersWithNullTermination()

namespace WebCore {
    // A data object for holding data that would be in a clipboard or moved
    // during a drag-n-drop operation.  This is the data that WebCore is aware
    // of and is not specific to a platform.
    class DataObjectJava : public RefCounted<DataObjectJava> {
        static const URL   &emptyURL(){ static URL r; return r; }
        static const String &emptyString(){ static String r; return r; }
    public:
        static const String &mimePlainText()    { static String r("text/plain"); return r; }
        static const String &mimeHTML()         { static String r("text/html"); return r; }
        static const String &mimeURIList()      { static String r("text/uri-list"); return r; }
        static const String &mimeShortcutName() { static String r("text/ie-shortcut-filename"); return r; }

        // We provide the IE clipboard types (URL and Text),
        // and the clipboard types specified in the WHATWG Web Applications 1.0 draft
        // see http://www.whatwg.org/specs/web-apps/current-work/ Section 6.3.5.3
        static String normalizeMIMEType(const String& type)
        {
            String qType = type.stripWhiteSpace().lower();
            // two special cases for IE compatibility
            if (qType == "text" || qType.startsWith("text/plain;"))
                return mimePlainText();
            if (qType == "url")
                return mimeURIList();
            return qType;
        }

        static PassRefPtr<DataObjectJava> create()
        {
            return adoptRef(new DataObjectJava);
        }

        PassRefPtr<DataObjectJava> copy() const
        {
            return adoptRef(new DataObjectJava(*this));
        }

        void clear() {
            availMimeTypes.clear();
        }
        void clearData(const String& mimeType) {
            availMimeTypes.remove(mimeType);
        }

        bool hasData() const {
            return !availMimeTypes.isEmpty();
        }

        //setters
        void setUrl(const URL &_url, const String &_urlTitle) {
            availMimeTypes.add(mimeURIList());
            availMimeTypes.add(mimeShortcutName());
            url = _url;
            urlTitle = _urlTitle;
            m_filenames.clear();
        }
        void setFiles(const Vector<String> &filenames){
            availMimeTypes.add(mimeURIList());
            availMimeTypes.remove(mimeShortcutName());
            url = emptyURL();
            urlTitle = emptyString();
            m_filenames = filenames;
        }
        void setPlainText(const String &_plainText){
            availMimeTypes.add( mimePlainText() );
            plainText = _plainText;
        }
        void setHTML(const String &_textHtml, const URL &_htmlBaseUrl){
            availMimeTypes.add( mimeHTML() );
            textHtml = _textHtml;
            htmlBaseUrl = _htmlBaseUrl;
        }

        bool setData(const String& mimeType, const String& data){
            bool succeeded = true;
            String canonicalMimeType = normalizeMIMEType(mimeType);
            if (canonicalMimeType == mimeURIList())
                setUrl(URL(ParsedURLString, data), emptyString());
            else if (canonicalMimeType == mimeHTML())
                setHTML(data, emptyURL());
            else if (canonicalMimeType == mimePlainText()) // two special cases for IE compatibility
                setPlainText(data);
            else if (canonicalMimeType == mimeShortcutName())
                urlTitle = data; //activates by previous setUrl call
            else
                succeeded = false;
            return succeeded;
        }

        //getters
        //URL
        ListHashSet<String> types(){
            //returns MIME Types available in clipboard.
            return availMimeTypes;
        }
        String getData(const String& mimeType){
            String canonicalMimeType = normalizeMIMEType(mimeType);
            String ret;
            if (canonicalMimeType == mimeURIList())
                ret = asURL();
            else if (canonicalMimeType == mimeHTML())
                ret = asHTML();
            else if (canonicalMimeType == mimePlainText())
                ret = asPlainText();
            else if (canonicalMimeType == mimeShortcutName())
                ret = urlTitle;
            return ret;
        }
        bool containsURL() const {
            return availMimeTypes.contains(mimeURIList());
        }
        String asURL(String* title = NULL) const
        {
            if (!containsURL())
                return String();

            if( url.isEmpty() && !m_filenames.isEmpty())
                return m_filenames.at(0);

            // |title| can be NULL
            if (title)
                *title = urlTitle;
            return url.string();
        }

        //File List
        bool containsFiles() const {
            return containsURL();
        }
        void asFilenames(Vector<String>& result) const {
            if( url.isEmpty() && !m_filenames.isEmpty())
                result = m_filenames;
            else
                result.append( url.string() );
        }

        //Plain Text
        bool containsPlainText() const {
            return availMimeTypes.contains(mimePlainText());
        }
        String asPlainText() const {
            return plainText;
        }


        bool containsHTML() const {
            return availMimeTypes.contains(mimeHTML());
        }
        String asHTML(String* baseURL = NULL) const
        {
            if (!containsHTML())
                return String();

            // |baseURL| can be NULL
            if (baseURL)
                *baseURL = htmlBaseUrl;
            return textHtml;
        }

        String fileContentFilename;
        RefPtr<SharedBuffer> fileContent;

        ~DataObjectJava() {
        }

        const Vector<String>& filenames() const {
            return m_filenames;
        }

    private:
        ListHashSet<String> availMimeTypes;

        //URL
        URL url;
        String urlTitle;
        Vector<String> m_filenames;

        //plain text
        String plainText;

        //html text
        String textHtml;
        URL   htmlBaseUrl;

        DataObjectJava() {
        }
        DataObjectJava(const DataObjectJava&){
            ASSERT(false);
        }
    };
} // namespace WebCore

#endif //DataObjectJava_h
