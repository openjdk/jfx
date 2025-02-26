/*
 * Copyright (c) 2011, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include "PasteboardUtilitiesJava.h"
#include "CachedImage.h"
#include "DocumentFragment.h"
#include "Image.h"
#include "Editor.h"
#include "Frame.h"
#include "FrameView.h"
#include "LocalFrame.h"
#include "markup.h"
#include "Pasteboard.h"
#include "RenderImage.h"
#include "Range.h"
#include "NotImplemented.h"
#include "DataObjectJava.h"
#include "DragData.h"
#include "PlatformJavaClasses.h"
#include <wtf/java/JavaRef.h>
#include <wtf/text/WTFString.h>
#include <wtf/text/StringBuilder.h>
#include "NamedNodeMap.h"
#include "Attr.h"
#include "HTMLNames.h"
#include "HTMLParserIdioms.h"

#include "wtf/Ref.h"

namespace WebCore {

///////////////////
// WCPasteboard JNI
///////////////////

namespace {

#define PB_CLASS jPBClass()

#define DEFINE_PB_CLASS(_name) \
    JNIEnv* env = WTF::GetJavaEnv(); \
    static JGClass cls(env->FindClass(_name)); \
    ASSERT(cls);

#define DEFINE_PB_STATIC_METHOD(_name, _params) \
    JNIEnv* env = WTF::GetJavaEnv(); \
    static jmethodID mid = env->GetStaticMethodID(PB_CLASS, _name, _params); \
    ASSERT(mid);

#define CALL_PB_STATIC_VOID_METHOD(...) \
    env->CallStaticVoidMethod(PB_CLASS, mid, __VA_ARGS__); \
    WTF::CheckAndClearException(env);

#define CALL_PB_STATIC_JSTROBJ_METHOD(_jstrobj) \
    JLString _jstrobj(static_cast<jstring>(env->CallStaticObjectMethod(PB_CLASS, mid))); \
    WTF::CheckAndClearException(env);

jclass jPBClass()
{
    DEFINE_PB_CLASS("com/sun/webkit/WCPasteboard");
    return cls;
}

String jGetPlainText()
{
    DEFINE_PB_STATIC_METHOD("getPlainText", "()Ljava/lang/String;");
    CALL_PB_STATIC_JSTROBJ_METHOD(jstr);

    return jstr ? String(env, jstr) : String();
}

void jWritePlainText(const String& plainText)
{
    DEFINE_PB_STATIC_METHOD("writePlainText", "(Ljava/lang/String;)V");
    CALL_PB_STATIC_VOID_METHOD((jstring)plainText.toJavaString(env));
}

void jWriteSelection(bool canSmartCopyOrDelete, const String& plainText, const String& markup)
{
    DEFINE_PB_STATIC_METHOD("writeSelection", "(ZLjava/lang/String;Ljava/lang/String;)V");
    CALL_PB_STATIC_VOID_METHOD(
        bool_to_jbool(canSmartCopyOrDelete),
        (jstring)plainText.toJavaString(env),
        (jstring)markup.toJavaString(env));
}

void jWriteImage(const Image& image)
{
    DEFINE_PB_STATIC_METHOD("writeImage", "(Lcom/sun/webkit/graphics/WCImageFrame;)V");
    CALL_PB_STATIC_VOID_METHOD(jobject(*const_cast<Image&>(image).javaImage()->platformImage()->getImage()));
}

void jWriteURL(const String& url, const String& markup)
{
    DEFINE_PB_STATIC_METHOD("writeUrl", "(Ljava/lang/String;Ljava/lang/String;)V");
    CALL_PB_STATIC_VOID_METHOD(
        (jstring)url.toJavaString(env),
        (jstring)markup.toJavaString(env));
}

String jGetHtml()
{
    DEFINE_PB_STATIC_METHOD("getHtml", "()Ljava/lang/String;");
    CALL_PB_STATIC_JSTROBJ_METHOD(jstr);

    return jstr ? String(env, jstr) : String();
}

///////////////////
// Helper functions
///////////////////

CachedImage* getCachedImage(const Element& element)
{
    // Attempt to pull CachedImage from element
    RenderObject* renderer = element.renderer();
    if (!renderer || !renderer->isImage()) {
        return 0;
    }
    RenderImage* image = static_cast<RenderImage*>(renderer);
    if (image->cachedImage() && !image->cachedImage()->errorOccurred()) {
        return image->cachedImage();
    }
    return 0;
}

void writeImageToDataObject(RefPtr<DataObjectJava> dataObject, const Element& element, const URL&)
{
    if (!dataObject) {
        return;
    }
    // Shove image data into a DataObject for use as a file
    CachedImage* cachedImage = getCachedImage(element);
    if (!cachedImage || !cachedImage->image() || !cachedImage->isLoaded()) {
        return;
    }
    FragmentedSharedBuffer* imageBuffer = cachedImage->image()->data();
    if (!imageBuffer || !imageBuffer->size()) {
        return;
    }
    dataObject->m_fileContent = imageBuffer;

    // Determine the filename for the file contents of the image.  We try to
    // use the alt tag if one exists, otherwise we fall back on the suggested
    // filename in the http header, and finally we resort to using the filename
    // in the URL.
    //String title = element->getAttribute(altAttr);
    //if (title.isEmpty())
    //  title = cachedImage->response().suggestedFilename();

    //TODO: do we need it?
    dataObject->m_fileContentFilename = cachedImage->response().suggestedFilename();
}

String imageToMarkup(const String& url, const Element& element)
{
    StringBuilder markup;
    markup.append(WTF::String::fromUTF8("<img src=\""));
    markup.append(url);
    markup.append(WTF::String::fromUTF8("\""));
    // Copy over attributes.  If we are dragging an image, we expect things like
    // the id to be copied as well.
    NamedNodeMap* attrs = &element.attributes();
    unsigned length = attrs->length();
    for (unsigned i = 0; i < length; ++i) {
        RefPtr<Attr> attr(static_cast<Attr*>(attrs->item(i).get()));
        if (attr->name() == "src"_s)
            continue;
        markup.append(WTF::String::fromUTF8(" "));
        markup.append(attr->name());
        markup.append(WTF::String::fromUTF8("=\""));
        String escapedAttr = attr->value();
        escapedAttr = makeStringByReplacingAll(escapedAttr,"\""_s, "&quot;"_s);
        markup.append(escapedAttr);
        markup.append(WTF::String::fromUTF8("\""));
    }

    markup.append(WTF::String::fromUTF8("/>"));
    return markup.toString();
}

} // anonymouse namespace

///////////////////////////
// WebCore::Pasteboard impl
///////////////////////////

struct PasteboardFileCounter final : PasteboardFileReader {
    void readFilename(const String&) final { ++count; }
    void readBuffer(const String&, const String&, Ref<SharedBuffer>&&) final { ++count; }

    unsigned count { 0 };
};

Pasteboard::Pasteboard(RefPtr<DataObjectJava> dataObject, bool copyPasteMode = false)
  : m_dataObject(dataObject),
    m_copyPasteMode(copyPasteMode)
{
    ASSERT(m_dataObject);
}

Pasteboard::Pasteboard(std::unique_ptr<PasteboardContext>&&) : Pasteboard(DataObjectJava::create())
{
}

std::unique_ptr<Pasteboard> Pasteboard::create(RefPtr<DataObjectJava> dataObject)
{
    return std::unique_ptr<Pasteboard>(new Pasteboard(dataObject));
}

std::unique_ptr<Pasteboard> Pasteboard::createForCopyAndPaste(std::unique_ptr<PasteboardContext>&&)
{
    // Use single shared data instance for all copy'n'paste pasteboards.
    static RefPtr<DataObjectJava> data = DataObjectJava::create();
    // TODO: setURL, setFiles, setData, setHtml (needs URL)
    data->setPlainText(jGetPlainText());
    data->setData(DataObjectJava::mimeHTML(), jGetPlainText());
    return std::unique_ptr<Pasteboard>(new Pasteboard(data, true));
}

#if ENABLE(DRAG_SUPPORT)
std::unique_ptr<Pasteboard> Pasteboard::createForDragAndDrop(std::unique_ptr<PasteboardContext>&&)
{
    return create(DataObjectJava::create());
}

std::unique_ptr<Pasteboard> Pasteboard::create(const DragData& dragData)
{
    return create(dragData.platformData());
}

void Pasteboard::setDragImage(DragImage, const IntPoint&)
{
}
#endif

void Pasteboard::writeSelection(
    const SimpleRange& selectedRange,
    bool canSmartCopyOrDelete,
    LocalFrame& frame,
    ShouldSerializeSelectedTextForDataTransfer shouldSerializeSelectedTextForDataTransfer)
{
    String markup = serializePreservingVisualAppearance(selectedRange, nullptr, AnnotateForInterchange::Yes, ConvertBlocksToInlines::No, ResolveURLs::YesExcludingURLsForPrivacy);
    String plainText = shouldSerializeSelectedTextForDataTransfer == IncludeImageAltTextForDataTransfer
        ? frame.editor().selectedTextForDataTransfer()
        : frame.editor().selectedText();

#if OS(WINDOWS)
    replaceNewlinesWithWindowsStyleNewlines(plainText);
#endif
    replaceNBSPWithSpace(plainText);

    m_dataObject->clear();
    m_dataObject->setPlainText(plainText);
    m_dataObject->setHTML(markup, frame.document()->url());

    if (m_copyPasteMode) {
        jWriteSelection(canSmartCopyOrDelete, plainText, markup);
    }
}

void Pasteboard::writePlainText(const String& text, SmartReplaceOption)
{
    String plainText(text);
#if OS(WINDOWS)
    replaceNewlinesWithWindowsStyleNewlines(plainText);
#endif

    if (m_dataObject) {
        m_dataObject->clear();
        m_dataObject->setPlainText(plainText);
    }
    if (m_copyPasteMode) {
        jWritePlainText(plainText);
    }
}

void Pasteboard::write(const PasteboardURL& pasteboardURL)
{
    ASSERT(!pasteboardURL.url.isEmpty());

    String title(pasteboardURL.title);
    if (title.isEmpty()) {
        title = pasteboardURL.url.lastPathComponent().toString();
        if (title.isEmpty()) {
            title = pasteboardURL.url.host().toString();
        }
    }
    String markup(urlToMarkup(pasteboardURL.url, title));

    m_dataObject->clear();
    m_dataObject->setURL(pasteboardURL.url, title);
    m_dataObject->setPlainText(pasteboardURL.url.string());
    m_dataObject->setHTML(markup, pasteboardURL.url);

    if (m_copyPasteMode) {
        jWriteURL(pasteboardURL.url.string(), markup);
    }
}

void Pasteboard::writeImage(Element& element, const URL& url, const String& title)
{
    m_dataObject->setURL(url, title);

    // Write the bytes of the image to the file format
    writeImageToDataObject(m_dataObject, element, url);

    AtomString imageURL = element.getAttribute(HTMLNames::srcAttr);
    if (!imageURL.isEmpty()) {
        String fullURL = element.document().completeURL(imageURL).string();  //REVISIT
        if (!fullURL.isEmpty()) {
            m_dataObject->setHTML(
                imageToMarkup(fullURL, element),
                element.document().url());
        }
    }
    if (m_copyPasteMode) {
        CachedImage* cachedImage = getCachedImage(element);
        // CachedImage not exist
        if (!cachedImage) {
            return;
        }

        Image* image = cachedImage->image();
        // Image data not exist
        if (!image) {
            return;
        }

        // SVGImage are not Bitmap backed, Let the receiving end decode the svg image
        // based on url and its markup
        if (image->isSVGImage()) {
            jWriteURL(url.string(), serializeFragment(element, SerializedNodes::SubtreeIncludingNode));
        }
        else {
            jWriteImage(*image);
        }
    }
}

void Pasteboard::writeString(const String& type, const String& data)
{
    // DnD only mode
    if (m_dataObject) {
        m_dataObject->setData(type, data);
    }
}

String Pasteboard::readString(const String& type)
{
    // DnD only mode
    if (m_dataObject) {
        return m_dataObject->getData(type);
    }
    return String();
}

void Pasteboard::clear(const String& type)
{
    if (m_dataObject) {
        m_dataObject->clearData(type);
    }
    if (m_copyPasteMode) {
        String canonicalMimeType = DataObjectJava::normalizeMIMEType(type);
        if (canonicalMimeType == DataObjectJava::mimeURIList())
            jWriteURL(DataObjectJava::emptyString(), DataObjectJava::emptyString());
        else if (canonicalMimeType == DataObjectJava::mimeHTML())
            jWriteSelection(false, DataObjectJava::emptyString(), DataObjectJava::emptyString());
        else if (canonicalMimeType == DataObjectJava::mimePlainText())
            jWritePlainText(DataObjectJava::emptyString());
    }
}

void Pasteboard::clear()
{
    if (m_dataObject) {
        m_dataObject->clear();
    }
    if (m_copyPasteMode) {
        jWriteURL(DataObjectJava::emptyString(), DataObjectJava::emptyString());
        jWriteSelection(false, DataObjectJava::emptyString(), DataObjectJava::emptyString());
        jWritePlainText(DataObjectJava::emptyString());
    }
}

Vector<String> Pasteboard::typesSafeForBindings(const String&)
{
    notImplemented();
    return { };
}

String Pasteboard::readOrigin()
{
    notImplemented();
    return { };
}

Vector<String> Pasteboard::typesForLegacyUnsafeBindings()
{
    if (m_dataObject) {
        return m_dataObject->types();
    }
    return Vector<String>();
}

bool Pasteboard::hasData()
{
    return m_dataObject && m_dataObject->hasData();
}

void Pasteboard::read(PasteboardFileReader& reader, std::optional<size_t>)
{
    if (m_dataObject) {
        for (const auto& filename : m_dataObject->asFilenames())
            reader.readFilename(filename);
    }
}

String Pasteboard::readStringInCustomData(const String&)
{
    notImplemented();
    return { };
}

Pasteboard::FileContentState Pasteboard::fileContentState()
{
    // FIXME: This implementation can be slightly more efficient by avoiding calls to DragQueryFileW.
    PasteboardFileCounter reader;
    read(reader);
    return reader.count ? FileContentState::MayContainFilePaths : FileContentState::NoFileOrImageData;
}

void Pasteboard::read(PasteboardPlainText& text, PlainTextURLReadingPolicy, std::optional<size_t>)
{
    if (m_copyPasteMode) {
        text.text = jGetPlainText();
        if (m_dataObject) {
            m_dataObject->setPlainText(text.text);
        }
        return;
    }
    if (m_dataObject) {
        text.text = m_dataObject->asPlainText();
    }
}

bool Pasteboard::canSmartReplace()
{
    // we do not provide smart replace for now
    return false;
}

RefPtr<DocumentFragment> Pasteboard::documentFragment(
    LocalFrame& frame, const SimpleRange& range, bool allowPlainText, bool &chosePlainText)
{
    chosePlainText = false;

    String htmlString = m_copyPasteMode ?
        jGetHtml() :
        m_dataObject ? m_dataObject->asHTML() : String();

    if (!htmlString.isNull()) {
        if (RefPtr<DocumentFragment> fragment = createFragmentFromMarkup(
                *frame.document(), htmlString, emptyString(), ParserContentPolicy::AllowScriptingContent))
        {
            return fragment;
        }
    }

    if (!allowPlainText) {
        return nullptr;
    }

    String plainTextString = m_copyPasteMode ?
        jGetPlainText() :
        m_dataObject ? m_dataObject->asPlainText() : String();

    if (!plainTextString.isNull()) {
        chosePlainText = true;
        if (RefPtr<DocumentFragment> fragment =
            createFragmentFromText(range, plainTextString))
        {
            return fragment;
        }
    }
    return nullptr;
}

void Pasteboard::read(PasteboardWebContentReader&, WebContentReadingPolicy, std::optional<size_t>)
{
}

void Pasteboard::write(const PasteboardImage&)
{
}

void Pasteboard::write(const PasteboardBuffer&)
{
}

void Pasteboard::write(const PasteboardWebContent&)
{
}

void Pasteboard::writeMarkup(const String&)
{
}

void Pasteboard::writeCustomData(const Vector<PasteboardCustomData>&)
{
}

void Pasteboard::write(const WebCore::Color&)
{
}

void Pasteboard::writeTrustworthyWebURLsPboardType(const PasteboardURL&)
{
    notImplemented();
}

} // namespace WebCore
