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

#include "config.h"

#include "PlatformJavaClasses.h"
#include "LocalizedStrings.h"
#include "NotImplemented.h"

#include <wtf/MathExtras.h>
#include <wtf/text/WTFString.h>

namespace WebCore {

String getLocalizedProperty(String name)
{
    JNIEnv* env = WTF::GetJavaEnv();

    static JGClass cls(env->FindClass("com/sun/webkit/LocalizedStrings"));
    ASSERT(cls);

    static jmethodID mid = env->GetStaticMethodID(cls,
        "getLocalizedProperty",
        "(Ljava/lang/String;)Ljava/lang/String;");
    ASSERT(mid);


    JLString ls(static_cast<jstring>(env->CallStaticObjectMethod(cls, mid,
        (jstring)name.toJavaString(env))));
    WTF::CheckAndClearException(env);

    return !ls ? name : String(env, ls);
}

String contextMenuItemTagInspectElement()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagInspectElement");
}

String inputElementAltText()
{
    return getLocalizedProperty("inputElementAltText"_s);
}

String resetButtonDefaultLabel()
{
    return getLocalizedProperty("resetButtonDefaultLabel"_s);
}

String searchableIndexIntroduction()
{
    return getLocalizedProperty("searchableIndexIntroduction"_s);
}

String submitButtonDefaultLabel()
{
    return getLocalizedProperty("submitButtonDefaultLabel"_s);
}

String fileButtonChooseFileLabel()
{
    return getLocalizedProperty("fileButtonChooseFileLabel"_s);
}

String fileButtonNoFilesSelectedLabel()
{
    return getLocalizedProperty("fileButtonNoFilesSelectedLabel"_s);
}

String fileButtonNoFileSelectedLabel()
{
    return getLocalizedProperty("fileButtonNoFileSelectedLabel"_s);
}

String AXAutoFillLoadingLabel()
{
    return String(); // UNSUPPORTED
}

String fileButtonChooseMultipleFilesLabel()
{
    return getLocalizedProperty("fileButtonChooseMultipleFilesLabel"_s);
}

String multipleFileUploadText(unsigned numberOfFiles)
{
    return String::number(numberOfFiles) + " " + getLocalizedProperty("multipleFileUploadText"_s);
}

String contextMenuItemTagOpenLinkInNewWindow()
{
    return getLocalizedProperty("contextMenuItemTagOpenLinkInNewWindow"_s);
}

String contextMenuItemTagDownloadLinkToDisk()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagDownloadLinkToDisk");
}

String contextMenuItemTagCopyLinkToClipboard()
{
    return getLocalizedProperty("contextMenuItemTagCopyLinkToClipboard"_s);
}

String contextMenuItemTagOpenImageInNewWindow()
{
    return getLocalizedProperty("contextMenuItemTagOpenImageInNewWindow"_s);
}

String contextMenuItemTagDownloadImageToDisk()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagDownloadImageToDisk");
}

String contextMenuItemTagDownloadAudioToDisk()
{
    return String(); // UNSUPPORTED
}

String contextMenuItemTagDownloadVideoToDisk()
{
    return String(); // UNSUPPORTED
}

String contextMenuItemTagCopyImageToClipboard()
{
    return getLocalizedProperty("contextMenuItemTagCopyImageToClipboard"_s);
}

String contextMenuItemTagOpenFrameInNewWindow()
{
    return getLocalizedProperty("contextMenuItemTagOpenFrameInNewWindow"_s);
}

String contextMenuItemTagCopy()
{
    return getLocalizedProperty("contextMenuItemTagCopy"_s);
}

String contextMenuItemTagGoBack()
{
    return getLocalizedProperty("contextMenuItemTagGoBack"_s);
}

String contextMenuItemTagGoForward()
{
    return getLocalizedProperty("contextMenuItemTagGoForward"_s);
}

String contextMenuItemTagStop()
{
    return getLocalizedProperty("contextMenuItemTagStop"_s);
}

String contextMenuItemTagReload()
{
    return getLocalizedProperty("contextMenuItemTagReload"_s);
}

String contextMenuItemTagCut()
{
    return getLocalizedProperty("contextMenuItemTagCut"_s);
}

String contextMenuItemTagPaste()
{
    return getLocalizedProperty("contextMenuItemTagPaste"_s);
}

String contextMenuItemTagNoGuessesFound()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagNoGuessesFound");
}

String contextMenuItemTagIgnoreSpelling()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagIgnoreSpelling");
}

String contextMenuItemTagLearnSpelling()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagLearnSpelling");
}

String contextMenuItemTagSearchWeb()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagSearchWeb");
}

String contextMenuItemTagLookUpInDictionary()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagLookUpInDictionary");
}

String contextMenuItemTagOpenLink()
{
    return getLocalizedProperty("contextMenuItemTagOpenLink"_s);
}

String contextMenuItemTagIgnoreGrammar()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagIgnoreGrammar");
}

String contextMenuItemTagSpellingMenu()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagSpellingMenu");
}

String contextMenuItemTagShowSpellingPanel(bool)
{
    return String(); /* UNSUPPORTED: show
            ? getLocalizedProperty("contextMenuItemTagShowSpellingPanelShow")
            : getLocalizedProperty("contextMenuItemTagShowSpellingPanelHide");*/
}

String contextMenuItemTagCheckSpelling()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagCheckSpelling");
}

String contextMenuItemTagCheckSpellingWhileTyping()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagCheckSpellingWhileTyping");
}

String contextMenuItemTagCheckGrammarWithSpelling()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagCheckGrammarWithSpelling");
}

String contextMenuItemTagFontMenu()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagFontMenu");
}

String contextMenuItemTagBold()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagBold");
}

String contextMenuItemTagItalic()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagItalic");
}

String contextMenuItemTagUnderline()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagUnderline");
}

String contextMenuItemTagOutline()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagOutline");
}

String contextMenuItemTagWritingDirectionMenu()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagWritingDirectionMenu");
}

String contextMenuItemTagDefaultDirection()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagDefaultDirection");
}

String contextMenuItemTagLeftToRight()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagLeftToRight");
}

String contextMenuItemTagRightToLeft()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagRightToLeft");
}

String contextMenuItemTagTextDirectionMenu()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagTextDirectionMenu");
}

String searchMenuNoRecentSearchesText()
{
    return getLocalizedProperty("searchMenuNoRecentSearchesText"_s);
}

String searchMenuRecentSearchesText()
{
    return getLocalizedProperty("searchMenuRecentSearchesText"_s);
}

String searchMenuClearRecentSearchesText()
{
    return getLocalizedProperty("searchMenuClearRecentSearchesText"_s);
}

String unknownFileSizeText()
{
    return getLocalizedProperty("unknownFileSizeText"_s);
}

String crashedPluginText()
{
    return getLocalizedProperty("crashedPluginText"_s);
}

String blockedPluginByContentSecurityPolicyText()
{
    return getLocalizedProperty("blockedPluginByContentSecurityPolicyText"_s);
}

String inactivePluginText()
{
    return getLocalizedProperty("inactivePluginText"_s);
}

String snapshottedPlugInLabelSubtitle()
{
    return getLocalizedProperty("snapshottedPlugInLabelSubtitle"_s);
}

String snapshottedPlugInLabelTitle()
{
    return getLocalizedProperty("snapshottedPlugInLabelTitle"_s);
}

String missingPluginText()
{
    return getLocalizedProperty("missingPluginText"_s);
}

String insecurePluginVersionText()
{
    return getLocalizedProperty("insecurePluginVersionText"_s);
}


String imageTitle(const String&, const IntSize&)
{
    return String();
}

String contextMenuItemTagCopyAudioLinkToClipboard()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagCopyAudioLinkToClipboard");
}

String contextMenuItemTagCopyVideoLinkToClipboard()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagCopyVideoLinkToClipboard");
}

String contextMenuItemTagEnterVideoFullscreen()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagEnterVideoFullscreen");
}

String textTrackSubtitlesText()
{
    return String::fromUTF8("Subtitles");
}

String textTrackOffMenuItemText()
{
    return String::fromUTF8("Off");
}

String textTrackAutomaticMenuItemText()
{
    return String::fromUTF8("Auto");
}

String trackNoLabelText()
{
    return String::fromUTF8("No label");
}

String textTrackNoLabelText()
{
    return String::fromUTF8("No label");
}

String audioTrackNoLabelText()
{
    return String::fromUTF8("No label");
}

String contextMenuItemTagMediaPlay()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagMediaPlay");
}

String contextMenuItemTagMediaPause()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagMediaPause");
}

String contextMenuItemTagMediaMute()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagMediaMute");
}

String contextMenuItemTagOpenAudioInNewWindow()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagOpenAudioInNewWindow");
}

String contextMenuItemTagOpenVideoInNewWindow()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagOpenVideoInNewWindow");
}

String contextMenuItemTagToggleMediaControls()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagToggleMediaControls");
}

String contextMenuItemTagToggleMediaLoop()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagToggleMediaLoop");
}

String mediaElementLoadingStateText()
{
    return getLocalizedProperty("mediaElementLoadingStateText"_s);
}

String mediaElementLiveBroadcastStateText()
{
    return getLocalizedProperty("mediaElementLiveBroadcastStateText"_s);
}

String localizedMediaControlElementString(const String& s)
{
    return getLocalizedProperty(String("localizedMediaControlElementString"_s) + s);
}

String localizedMediaControlElementHelpText(const String& s)
{
    return getLocalizedProperty(String("localizedMediaControlElementHelpText"_s) + s);
}

String localizedMediaTimeDescription(float time)
{
    if (!std::isfinite(time))
        return getLocalizedProperty("localizedMediaTimeDescriptionIndefinite"_s);

    int seconds = (int)fabsf(time);
    int days = seconds / (60 * 60 * 24);
    int hours = seconds / (60 * 60);
    int minutes = (seconds / 60) % 60;
    seconds %= 60;

    String result;
    if (days) {
        String s = getLocalizedProperty("localizedMediaTimeDescriptionDays"_s);
        //result.append(String::number(days) + " " + s + "  ");
        result = makeString(result, String::number(days), " ", s, "  ");
    }
    if (days || hours) {
        String s = getLocalizedProperty("localizedMediaTimeDescriptionHours"_s);
        //result.append(String::number(hours) + " " + s + "  ");
        result = makeString(result, String::number(hours), " ", s, "  ");
    }
    if (days || hours || minutes) {
        String s = getLocalizedProperty("localizedMediaTimeDescriptionMinutes"_s);
        //result.append(String::number(minutes) + " " + s + "  ");
        result = makeString(result, String::number(minutes), " ", s, "  ");
    }
    String s = getLocalizedProperty("localizedMediaTimeDescriptionSeconds"_s);
    return result + String::number(days) + " " + s;
}

String AXWebAreaText()
{
    return getLocalizedProperty("AXWebAreaText"_s);
}

String AXAutoFillCreditCardLabel()
{
    return getLocalizedProperty("AXAutoFillCreditCardLabel"_s);
}

String AXLinkText()
{
    return getLocalizedProperty("AXLinkText"_s);
}

String AXListMarkerText()
{
    return getLocalizedProperty("AXListMarkerText"_s);
}

String AXAttachmentRoleText()
{
    notImplemented();
    return String();
}

String AXImageMapText()
{
    return getLocalizedProperty("AXImageMapText"_s);
}

String AXHeadingText()
{
    return getLocalizedProperty("AXHeadingText"_s);
}

String AXDefinitionListTermText()
{
    return getLocalizedProperty("AXDefinitionListTermText"_s);
}

String AXDefinitionListDefinitionText()
{
    return getLocalizedProperty("AXDefinitionListDefinitionText"_s);
}

String AXFigureText()
{
    return getLocalizedProperty("AXFigureText"_s);
}

String AXARIAContentGroupText(const String& ariaType)
{
    return getLocalizedProperty(String("AXARIAContentGroupText"_s) + ariaType);
}

String AXButtonActionVerb()
{
    return getLocalizedProperty("AXButtonActionVerb"_s);
}

String AXRadioButtonActionVerb()
{
    return getLocalizedProperty("AXRadioButtonActionVerb"_s);
}

String AXTextFieldActionVerb()
{
    return getLocalizedProperty("AXTextFieldActionVerb"_s);
}

String AXCheckedCheckBoxActionVerb()
{
    return getLocalizedProperty("AXCheckedCheckBoxActionVerb"_s);
}

String AXUncheckedCheckBoxActionVerb()
{
    return getLocalizedProperty("AXUncheckedCheckBoxActionVerb"_s);
}

String AXLinkActionVerb()
{
    return getLocalizedProperty("AXLinkActionVerb"_s);
}

String AXMenuListPopupActionVerb()
{
    return getLocalizedProperty("AXMenuListPopupActionVerb"_s);
}

String AXMenuListActionVerb()
{
    return getLocalizedProperty("AXMenuListActionVerb"_s);
}

String AXSearchFieldCancelButtonText() {
    notImplemented();
    return String(); // UNSUPPORTED: getLocalizedProperty("AXSearchFieldCancelButtonText");
}

String AXAutoFillStrongPasswordLabel()
{
    // return WEB_UI_STRING("strong password auto fill", "Label for the strong password auto fill button inside a text field."_s);
    return getLocalizedProperty("AXAutoFillStrongPasswordLabel"_s);
}

String AXAutoFillStrongConfirmationPasswordLabel()
{
    // return WEB_UI_STRING("strong confirmation password auto fill", "Label for the strong confirmation password auto fill button inside a text field."_s);
    return getLocalizedProperty("AXAutoFillStrongConfirmationPasswordLabel"_s);
}

String autoFillStrongPasswordLabel()
{
    // return WEB_UI_STRING("strong password", "Label for strong password."_s);
    return getLocalizedProperty("autoFillStrongPasswordLabel"_s);
}

String validationMessageValueMissingText()
{
    return getLocalizedProperty("validationMessageValueMissingText"_s);
}

String validationMessageTypeMismatchText()
{
    return getLocalizedProperty("validationMessageTypeMismatchText"_s);
}

String validationMessagePatternMismatchText()
{
    return getLocalizedProperty("validationMessagePatternMismatchText"_s);
}

String validationMessagePatternMismatchText(const String& title)
{
    UNUSED_PARAM(title);
    return validationMessagePatternMismatchText();
}

String validationMessageTooShortText(int, int)
{
    notImplemented();
    return String::fromUTF8("too short");
}

String validationMessageTooLongText(int, int)
{
    return getLocalizedProperty("validationMessageTooLongText"_s);
}

String validationMessageRangeUnderflowText(const String&)
{
    return getLocalizedProperty("validationMessageRangeUnderflowText"_s);
}

String validationMessageRangeOverflowText(const String&)
{
    return getLocalizedProperty("validationMessageRangeOverflowText"_s);
}

String validationMessageStepMismatchText(const String&, const String&)
{
    return getLocalizedProperty("validationMessageStepMismatchText"_s);
}

String validationMessageTypeMismatchForEmailText()
{
    notImplemented();
    return validationMessageTypeMismatchText();
}

String validationMessageTypeMismatchForMultipleEmailText()
{
    notImplemented();
    return validationMessageTypeMismatchText();
}

String validationMessageTypeMismatchForURLText()
{
    notImplemented();
    return validationMessageTypeMismatchText();
}

String validationMessageValueMissingForCheckboxText()
{
    notImplemented();
    return validationMessageValueMissingText();
}

String validationMessageValueMissingForFileText()
{
    notImplemented();
    return validationMessageValueMissingText();
}

String validationMessageValueMissingForMultipleFileText()
{
    notImplemented();
    return validationMessageValueMissingText();
}

String validationMessageValueMissingForRadioText()
{
    notImplemented();
    return validationMessageValueMissingText();
}

String validationMessageValueMissingForSelectText()
{
    notImplemented();
    return validationMessageValueMissingText();
}

String validationMessageBadInputForNumberText()
{
    return getLocalizedProperty("validationMessageBadInputForNumberText"_s);
}

#if ENABLE(INPUT_TYPE_WEEK)
// weekFormatInLDML() returns week and year format in LDML, Unicode
// technical standard 35, Locale Data Markup Language, e.g. "'Week' ww, yyyy"
String weekFormatInLDML()
{
    return getLocalizedProperty("weekFormatInLDML"_s);
}
#endif


String defaultDetailsSummaryText()
{
    return getLocalizedProperty("defaultDetailsSummaryText"_s);
}

String AXAutoFillCredentialsLabel()
{
    notImplemented();
    return String::fromUTF8("password auto fill");
}

String AXAutoFillContactsLabel()
{
    notImplemented();
    return String::fromUTF8("contact info auto fill");
}

String unsupportedPluginText()
{
    notImplemented();
    return String::fromUTF8("Unsupported Plug-in");
}

String pluginTooSmallText()
{
    return String::fromUTF8("Plug-In too small");
}


String pdfDocumentTypeDescription()
{
    return WEB_UI_STRING("Portable Document Format", "Description of the primary type supported by the PDF pseudo plug-in.");
}

String contextMenuItemTagShowMediaStats()
{
    return WEB_UI_STRING("Show Media Stats", "Media stats context menu item");
}

#if USE(CF) && !PLATFORM(JAVA)
String localizedString(CFStringRef key)
{
     UNUSED_PARAM(key);
     notImplemented();
     return String::fromUTF8("localizedString(CFStringRef key)"); //Need to add implementation
}
#else
String localizedString(const char* key)
{
    return String::fromUTF8(key, strlen(key));
}
#endif

String formatLocalizedString(const char* format, ...)
{
    notImplemented();
    return String::fromUTF8(format);
}

String validationMessageValueMissingForSwitchText()
{
    return WEB_UI_STRING("Tap this switch", "Validation message for required switches that are not on");
}

} // namespace WebCore
