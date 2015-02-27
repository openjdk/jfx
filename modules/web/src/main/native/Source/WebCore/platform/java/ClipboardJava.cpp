/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "NotImplemented.h"

#include "Attr.h"
#include "ClipboardJava.h"
#include "ClipboardUtilitiesJava.h"
#include "DragData.h"
#include "Editor.h"
#include "FileList.h"
#include "StringHash.h"
#include "CachedImage.h"
#include "Document.h"
#include "Element.h"
#include "FileList.h"
#include "Frame.h"
#include "HTMLNames.h"
#include "HTMLParserIdioms.h"
#include "MIMETypeRegistry.h"
#include "markup.h"
#include "NamedNodeMap.h"
#include "Range.h"
#include "RenderImage.h"
#include "StringBuilder.h"
#include "Pasteboard.h"

#include <wtf/HashSet.h>
#include <wtf/text/WTFString.h>

namespace WebCore {
using namespace HTMLNames;

static CachedImage* getCachedImage(Element* element)
{
    // Attempt to pull CachedImage from element
    ASSERT(element);
    RenderObject* renderer = element->renderer();
    if (!renderer || !renderer->isImage())
        return 0;

    RenderImage* image = static_cast<RenderImage*>(renderer);
    if (image->cachedImage() && !image->cachedImage()->errorOccurred())
        return image->cachedImage();

    return 0;
}

static void writeImageToDataObject(
    DataObjectJava* dataObject,
    Element* element,
    const URL& url)
{
    // Shove image data into a DataObject for use as a file
    CachedImage* cachedImage = getCachedImage(element);
    if (!cachedImage || !cachedImage->image() || !cachedImage->isLoaded())
        return;

    SharedBuffer* imageBuffer = cachedImage->image()->data();
    if (!imageBuffer || !imageBuffer->size())
        return;

    dataObject->fileContent = imageBuffer;

    // Determine the filename for the file contents of the image.  We try to
    // use the alt tag if one exists, otherwise we fall back on the suggested
    // filename in the http header, and finally we resort to using the filename
    // in the URL.
    //String title = element->getAttribute(altAttr);
    //if (title.isEmpty())
    //  title = cachedImage->response().suggestedFilename();

    //TODO: do we need it?
    dataObject->fileContentFilename = cachedImage->response().suggestedFilename();
}

static String imageToMarkup(const String& url, Element* element)
{
    StringBuilder markup;
    markup.append("<img src=\"");
    markup.append(url);
    markup.append("\"");
    // Copy over attributes.  If we are dragging an image, we expect things like
    // the id to be copied as well.
    NamedNodeMap* attrs = element->attributes();
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

ClipboardJava::ClipboardJava(
    ClipboardAccessPolicy policy,
    ClipboardType type,
    PassRefPtr<DataObjectJava> dataObject,
    Frame *frame)
  : Clipboard(policy, nullptr, type) // todo tav new param
, m_dataObject(dataObject)
, m_frame(frame)
{}

ClipboardJava::~ClipboardJava()
{
}

void ClipboardJava::clearData(const String& type)
{
    if (m_dataObject && policy() == ClipboardWritable){
        m_dataObject->clearData(type);
    }
}

void ClipboardJava::clearData()
{
    if ( m_dataObject && policy() == ClipboardWritable){
        m_dataObject->clear();
    }
}

String ClipboardJava::getData(const String& type) const
{
    if(policy() != ClipboardReadable || !m_dataObject)
        return String();

    return m_dataObject->getData(type);
}

bool ClipboardJava::setData(const String& type, const String& data)
{
    if (m_dataObject && policy() == ClipboardWritable){
        return m_dataObject->setData(type, data);
    }
    return false;
}

ListHashSet<String> ClipboardJava::typesPrivate() const
{
    if ( m_dataObject ){
        //TODO: returns the types that have the data entries
        //as mime-strings  ("text/uri-list", "text/plain")
        return m_dataObject->types();
    }
    return ListHashSet<String>();
}

ListHashSet<String> ClipboardJava::types() const
{
    ClipboardAccessPolicy ap = policy();
    if ( ap == ClipboardReadable || ap == ClipboardTypesReadable ){
        return typesPrivate();
    }
    return ListHashSet<String>();
}

PassRefPtr<FileList> ClipboardJava::files() const
{
    PassRefPtr<FileList> fl = FileList::create();
    if (
        m_dataObject
        &&  policy() == ClipboardReadable
        &&  m_dataObject->containsFiles()
    ){
        Vector<String> fn;
        m_dataObject->asFilenames(fn);
        for(
            Vector<String>::const_iterator i = fn.begin();
            fn.end()!=i;
            ++i
        )
            fl->append(File::create(*i));
    }
    return fl;
}

void ClipboardJava::setDragImage(CachedImage* image, Node* node, const IntPoint& loc)
{
    if (policy() != ClipboardImageWritable && policy() != ClipboardWritable)
        return;

    if (m_dragImage)
        m_dragImage->removeClient(this);
    m_dragImage = image;
    if (m_dragImage)
        m_dragImage->addClient(this);

    m_dragLocation = loc;
    m_dragImageElement = static_cast<Element*>(node);
}

void ClipboardJava::setDragImage(CachedImage* img, const IntPoint& loc)
{
    setDragImage(img, 0, loc);
}

void ClipboardJava::setDragImageElement(Node* node, const IntPoint& loc)
{
    setDragImage(0, node, loc);
}

void ClipboardJava::declareAndWriteDragImage(Element* element, const URL& url, const String& title, Frame* frame)
{
    if (!m_dataObject)
        return;

    m_dataObject->setUrl(url, title);

    // Write the bytes in the image to the file format.
    writeImageToDataObject(m_dataObject.get(), element, url);

    AtomicString imageURL = element->getAttribute(srcAttr);
    if (imageURL.isEmpty())
        return;

    String fullURL = frame->document()->completeURL(stripLeadingAndTrailingHTMLSpaces(imageURL));
    if (fullURL.isEmpty())
        return;

    // Put img tag on the clipboard referencing the image
    m_dataObject->setHTML(
        imageToMarkup(fullURL, element),
        frame->document()->url());
}

void ClipboardJava::writeURL(const URL& url, const String& title, Frame* frame)
{
    if (!m_dataObject)
        return;
    m_dataObject->setUrl(url, title);

    // The URL can also be used as plain text.
    m_dataObject->setPlainText(url.string());

    // The URL can also be used as an HTML fragment.
    m_dataObject->setHTML(urlToMarkup(url, title), url);
}

void ClipboardJava::writeRange(Range* selectedRange, Frame* frame)
{
    ASSERT(selectedRange);
    if (!m_dataObject)
         return;

    m_dataObject->setHTML(
        createMarkup(*selectedRange, 0, AnnotateForInterchange, false, ResolveAllURLs),
        frame->document()->url());

    String str = frame->editor().selectedText();
#if OS(WINDOWS)
    replaceNewlinesWithWindowsStyleNewlines(str);
#endif
    replaceNBSPWithSpace(str);
    m_dataObject->setPlainText(str);
}

void ClipboardJava::writePlainText(const String& text)
{
    if ( m_dataObject ) {
#if OS(WINDOWS)
        String str(text);
        replaceNewlinesWithWindowsStyleNewlines(str);
        m_dataObject->setPlainText(str);
#else
        m_dataObject->setPlainText(text);
#endif
    }
}

bool ClipboardJava::hasData()
{
    //no security limitation now for fact that there is something in.
    return m_dataObject && m_dataObject->hasData();
}

} // namespace WebCore
