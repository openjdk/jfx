/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "PasteboardUtilitiesJava.h"
#include "CachedImage.h"
#include "DocumentFragment.h"
#include "Image.h"
#include "Editor.h"
#include "Frame.h"
#include "FrameView.h"
#include "markup.h"
#include "Pasteboard.h"
#include "RenderImage.h"
#include "Range.h"
#include "NotImplemented.h"
#include "DataObjectJava.h"
#include "DragData.h"
#include "JavaEnv.h"
#include "JavaRef.h"
#include "WTFString.h"
#include "StringBuilder.h"
#include "NamedNodeMap.h"
#include "Attr.h"
#include "HTMLNames.h"
#include "HTMLParserIdioms.h"
#include "com_sun_webkit_WCPasteboard.h"

namespace WebCore {

///////////////////
// WCPasteboard JNI
///////////////////

namespace {

#define PB_CLASS jPBClass()

#define DEFINE_PB_CLASS(_name) \
    JNIEnv* env = WebCore_GetJavaEnv(); \
    static JGClass cls(env->FindClass(_name)); \
    ASSERT(cls);

#define DEFINE_PB_STATIC_METHOD(_name, _params) \
    JNIEnv* env = WebCore_GetJavaEnv(); \
    static jmethodID mid = env->GetStaticMethodID(PB_CLASS, _name, _params); \
    ASSERT(mid);

#define CALL_PB_STATIC_VOID_METHOD(...) \
    env->CallStaticVoidMethod(PB_CLASS, mid, __VA_ARGS__); \
    CheckAndClearException(env);

#define CALL_PB_STATIC_JSTROBJ_METHOD(_jstrobj) \
    JLString _jstrobj(static_cast<jstring>(env->CallStaticObjectMethod(PB_CLASS, mid))); \
    CheckAndClearException(env);

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
    CALL_PB_STATIC_VOID_METHOD(jobject(*const_cast<Image&>(image).javaImage()));
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

void writeImageToDataObject(PassRefPtr<DataObjectJava> dataObject, const Element& element, const URL& url)
{
    if (!dataObject) {
        return;
    }
    // Shove image data into a DataObject for use as a file
    CachedImage* cachedImage = getCachedImage(element);
    if (!cachedImage || !cachedImage->image() || !cachedImage->isLoaded()) {
        return;
    }
    SharedBuffer* imageBuffer = cachedImage->image()->data();
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
    markup.append("<img src=\"");
    markup.append(url);
    markup.append("\"");
    // Copy over attributes.  If we are dragging an image, we expect things like
    // the id to be copied as well.
    NamedNodeMap* attrs = element.attributes();
    unsigned length = attrs->length();
    for (unsigned i = 0; i < length; ++i) {
        RefPtr<Attr> attr(static_cast<Attr*>(attrs->item(i).get()));
        if (attr->name() == "src")
            continue;
        markup.append(" ");
        markup.append(attr->name());
        markup.append("=\"");
        String escapedAttr = attr->value();
        escapedAttr.replace("\"", "&quot;");
        markup.append(escapedAttr);
        markup.append("\"");
    }

    markup.append("/>");
    return markup.toString();
}

} // anonymouse namespace

///////////////////////////
// WebCore::Pasteboard impl
///////////////////////////

Pasteboard::Pasteboard(PassRefPtr<DataObjectJava> dataObject, bool copyPasteMode = false) :
    m_dataObject(dataObject),
    m_copyPasteMode(copyPasteMode)
{
    ASSERT(m_dataObject);
}

PassOwnPtr<Pasteboard> Pasteboard::create(PassRefPtr<DataObjectJava> dataObject)
{
    return adoptPtr(new Pasteboard(dataObject));
}

PassOwnPtr<Pasteboard> Pasteboard::createPrivate()
{
    return adoptPtr(new Pasteboard(DataObjectJava::create()));
}
    
PassOwnPtr<Pasteboard> Pasteboard::createForCopyAndPaste()
{
    // Use single shared data instance for all copy'n'paste pasteboards.
    static RefPtr<DataObjectJava> data = DataObjectJava::create();

    return adoptPtr(new Pasteboard(data, true));
}

#if ENABLE(DRAG_SUPPORT)
PassOwnPtr<Pasteboard> Pasteboard::createForDragAndDrop()
{
    return create(DataObjectJava::create());
}

PassOwnPtr<Pasteboard> Pasteboard::createForDragAndDrop(const DragData& dragData)
{
    return create(dragData.platformData());
}

void Pasteboard::setDragImage(DragImageRef image, const IntPoint& hotSpot)
{
}
#endif

void Pasteboard::writeSelection(
    Range& selectedRange,
    bool canSmartCopyOrDelete,
    Frame& frame,
    ShouldSerializeSelectedTextForClipboard shouldSerializeSelectedTextForClipboard)
{
    String markup = createMarkup(selectedRange, 0, AnnotateForInterchange, false, ResolveNonLocalURLs);
    String plainText = shouldSerializeSelectedTextForClipboard == IncludeImageAltTextForClipboard
        ? frame.editor().selectedTextForClipboard()
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
        title = pasteboardURL.url.lastPathComponent();
        if (title.isEmpty()) {
            title = pasteboardURL.url.host();
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

void Pasteboard::writeImage(Element& node, const URL& url, const String& title)
{
    m_dataObject->setURL(url, title);

    // Write the bytes of the image to the file format
    writeImageToDataObject(m_dataObject,    node, url);

    AtomicString imageURL = node.getAttribute(HTMLNames::srcAttr);
    if (!imageURL.isEmpty()) {
        String fullURL = node.document().completeURL(stripLeadingAndTrailingHTMLSpaces(imageURL));
        if (!fullURL.isEmpty()) {
            m_dataObject->setHTML(
                imageToMarkup(fullURL, node),
                node.document().url());
        }
    }
    if (m_copyPasteMode) {
        Image* image = getCachedImage(node)->image();
        jWriteImage(*image);
    }
}

bool Pasteboard::writeString(const String& type, const String& data)
{
    // DnD only mode
    if (m_dataObject) {
        return m_dataObject->setData(type, data);
    }
    return false;
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
    
Vector<String> Pasteboard::types()
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

Vector<String> Pasteboard::readFilenames()
{
    if (m_dataObject) {
        Vector<String> fn;
        m_dataObject->asFilenames(fn);
        return fn;
    }
    return Vector<String>();
}

void Pasteboard::read(PasteboardPlainText& text)
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

PassRefPtr<DocumentFragment> Pasteboard::documentFragment(
    Frame& frame,
    Range& range,
    bool allowPlainText,
    bool &chosePlainText)
{
    chosePlainText = false;

    String htmlString = m_copyPasteMode ?
        jGetHtml() :
        m_dataObject ? m_dataObject->asHTML() : String();

    if (!htmlString.isNull()) {
        if (PassRefPtr<DocumentFragment> fragment = createFragmentFromMarkup(
                *frame.document(),
                htmlString,
                String(),
                DisallowScriptingContent))
        {
            return fragment;
        }
    }

    if (!allowPlainText) {
        return 0;
    }

    String plainTextString = m_copyPasteMode ?
        jGetPlainText() :
        m_dataObject ? m_dataObject->asPlainText() : String();

    if (!plainTextString.isNull()) {
        chosePlainText = true;
        if (PassRefPtr<DocumentFragment> fragment = createFragmentFromText(
                range,
                plainTextString))
        {
            return fragment;
        }
    }
    return 0;
}

void Pasteboard::writePasteboard(const Pasteboard& sourcePasteboard)
{
    if (m_dataObject) {
        m_dataObject = sourcePasteboard.dataObject()->copy();
    }
    if (m_copyPasteMode) {
        RefPtr<DataObjectJava> data = sourcePasteboard.dataObject();
        if (data->containsURL()) jWriteURL(data->asURL(), data->asHTML());
        if (data->containsHTML()) jWriteSelection(false, data->asPlainText(), data->asHTML());
        if (data->containsPlainText()) jWritePlainText(data->asPlainText());
    }
}

} // namespace WebCore
