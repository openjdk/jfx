/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "ClipboardUtilitiesJava.h"
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

#include "JavaEnv.h"

#include "com_sun_webkit_WCPasteboard.h"

static jmethodID wcGetPlainTextMID;
static jmethodID wcGetHtmlMID;
static jmethodID wcWritePlainTextMID;
static jmethodID wcWriteSelectionMID;
static jmethodID wcWriteImageMID;
static jmethodID wcWriteUrlMID;

namespace WebCore {

Pasteboard::Pasteboard()
: m_pasteboardClass(WebCore_GetJavaEnv()->FindClass("com/sun/webkit/WCPasteboard"))
{
}

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

    JNIEnv* env = WebCore_GetJavaEnv();
    env->CallStaticVoidMethod(m_pasteboardClass, wcWriteSelectionMID,
            bool_to_jbool(canSmartCopyOrDelete),
            (jstring)plainText.toJavaString(env),
            (jstring)markup.toJavaString(env));
    CheckAndClearException(env);
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

    JNIEnv* env = WebCore_GetJavaEnv();
    env->CallStaticVoidMethod(m_pasteboardClass, wcWriteUrlMID,
        (jstring)pasteboardURL.url.string().toJavaString(env),
        (jstring)markup.toJavaString(env));
    CheckAndClearException(env);
}

void Pasteboard::writeImage(
    Element& node,
    const URL &,
    const String & /*title*/)
{
    ASSERT(node.renderer() && node.renderer()->isImage());

    RenderImage* renderer = static_cast<RenderImage*>(node.renderer());
    ASSERT(renderer);
    CachedImage* cachedImage = static_cast<CachedImage*>(renderer->cachedImage());
    ASSERT(cachedImage);
    Image* image = cachedImage->image();
    ASSERT(image);

    JNIEnv* env = WebCore_GetJavaEnv();
    env->CallStaticVoidMethod(m_pasteboardClass, wcWriteImageMID, jobject(*image->javaImage()));
    CheckAndClearException(env);
}

PassOwnPtr<Pasteboard> Pasteboard::createForCopyAndPaste()
{
    notImplemented(); // todo tav
    return nullptr;
}

String Pasteboard::readString(const String& type)
{
    notImplemented(); // todo tav
    return String("");
}

bool Pasteboard::writeString(const String& type, const String& data)
{
    notImplemented(); // todo tav
    return false;
}

PassOwnPtr<Pasteboard> Pasteboard::createPrivate()
{
    notImplemented(); // todo tav
    return nullptr;
}

void Pasteboard::clear()
{
    notImplemented();
}

Vector<String> Pasteboard::types()
{
    notImplemented(); // todo tav
    return Vector<String>();
}

bool Pasteboard::hasData()
{
    notImplemented(); // todo tav
    return false;
}

void Pasteboard::clear(const String& type)
{
    notImplemented(); // todo tav
}

Vector<String> Pasteboard::readFilenames()
{
    notImplemented(); // todo tav
    return Vector<String>();
}

void Pasteboard::read(PasteboardPlainText&)
{
    notImplemented(); // todo tav
}

void Pasteboard::read(PasteboardWebContentReader&)
{
    notImplemented(); // todo tav
}

#if ENABLE(DRAG_SUPPORT)
void Pasteboard::setDragImage(DragImageRef, const IntPoint& hotSpot)
{
    notImplemented(); // todo tav
}

PassOwnPtr<Pasteboard> Pasteboard::createForDragAndDrop()
{
    notImplemented(); // todo tav
    return nullptr;
}

PassOwnPtr<Pasteboard> Pasteboard::createForDragAndDrop(const DragData&)
{
    notImplemented(); // todo tav
    return nullptr;
}
#endif

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

    String htmlString = html();
    if (!htmlString.isNull()) {
        PassRefPtr<DocumentFragment> fragment = createFragmentFromMarkup(
            *frame.document(),
            htmlString,
            String(),
            DisallowScriptingContent);
        if (fragment) {
            return fragment;
        }
    }

    if (allowPlainText) {
        String plainTextString = plainText(frame);
        if (!plainTextString.isNull()) {
            chosePlainText = true;
            PassRefPtr<DocumentFragment> fragment = createFragmentFromText(
                range,
                plainTextString);
            if (fragment) {
                return fragment;
            }
        }
    }

    return 0;
}

String Pasteboard::plainText(Frame& frame)
{
    JNIEnv* env = WebCore_GetJavaEnv();

    JLString text(static_cast<jstring>(env->CallStaticObjectMethod(
                                       m_pasteboardClass, wcGetPlainTextMID)));
    CheckAndClearException(env);

    return text ? String(env, text) : String();
}

String Pasteboard::html() const
{
    JNIEnv* env = WebCore_GetJavaEnv();

    JLString html(static_cast<jstring>(env->CallStaticObjectMethod(
                                       m_pasteboardClass, wcGetHtmlMID)));
    CheckAndClearException(env);

    return html ? String(env, html) : String();
}

void Pasteboard::writePasteboard(const Pasteboard& sourcePasteboard)
{
    notImplemented(); // todo tav
}

void Pasteboard::writePlainText(const String& text, SmartReplaceOption)
{
    String plainText(text);
#if OS(WINDOWS)
    replaceNewlinesWithWindowsStyleNewlines(plainText);
#endif

    JNIEnv* env = WebCore_GetJavaEnv();
    env->CallStaticVoidMethod(m_pasteboardClass, wcWritePlainTextMID,
            (jstring)plainText.toJavaString(env));
    CheckAndClearException(env);
}

} // namespace WebCore

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_com_sun_webkit_WCPasteboard_initIDs
    (JNIEnv* env, jclass clazz)
{
    wcGetPlainTextMID = env->GetStaticMethodID(clazz, "getPlainText", "()Ljava/lang/String;");
    ASSERT(wcGetPlainTextMID);
    wcGetHtmlMID = env->GetStaticMethodID(clazz, "getHtml", "()Ljava/lang/String;");
    ASSERT(wcGetHtmlMID);
    wcWritePlainTextMID = env->GetStaticMethodID(clazz, "writePlainText", "(Ljava/lang/String;)V");
    ASSERT(wcWritePlainTextMID);
    wcWriteSelectionMID = env->GetStaticMethodID(clazz, "writeSelection", "(ZLjava/lang/String;Ljava/lang/String;)V");
    ASSERT(wcWriteSelectionMID);
    wcWriteImageMID = env->GetStaticMethodID(clazz, "writeImage", "(Lcom/sun/webkit/graphics/WCImageFrame;)V");
    ASSERT(wcWriteImageMID);
    wcWriteUrlMID = env->GetStaticMethodID(clazz, "writeUrl", "(Ljava/lang/String;Ljava/lang/String;)V");
    ASSERT(wcWriteUrlMID);
}

#ifdef __cplusplus
}
#endif
