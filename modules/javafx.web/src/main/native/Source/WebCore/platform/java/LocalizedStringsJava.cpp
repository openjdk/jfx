/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
    return getLocalizedProperty("inputElementAltText");
}

String resetButtonDefaultLabel()
{
    return getLocalizedProperty("resetButtonDefaultLabel");
}

String searchableIndexIntroduction()
{
    return getLocalizedProperty("searchableIndexIntroduction");
}

String submitButtonDefaultLabel()
{
    return getLocalizedProperty("submitButtonDefaultLabel");
}

String fileButtonChooseFileLabel()
{
    return getLocalizedProperty("fileButtonChooseFileLabel");
}

String fileButtonNoFilesSelectedLabel()
{
    return getLocalizedProperty("fileButtonNoFilesSelectedLabel");
}

String fileButtonNoFileSelectedLabel()
{
    return getLocalizedProperty("fileButtonNoFileSelectedLabel");
}

String fileButtonChooseMultipleFilesLabel()
{
    return getLocalizedProperty("fileButtonChooseMultipleFilesLabel");
}

String multipleFileUploadText(unsigned numberOfFiles)
{
    return String::number(numberOfFiles) + " " + getLocalizedProperty("multipleFileUploadText");
}

String contextMenuItemTagOpenLinkInNewWindow()
{
    return getLocalizedProperty("contextMenuItemTagOpenLinkInNewWindow");
}

String contextMenuItemTagDownloadLinkToDisk()
{
    return String(); // UNSUPPORTED: getLocalizedProperty("contextMenuItemTagDownloadLinkToDisk");
}

String contextMenuItemTagCopyLinkToClipboard()
{
    return getLocalizedProperty("contextMenuItemTagCopyLinkToClipboard");
}

String contextMenuItemTagOpenImageInNewWindow()
{
    return getLocalizedProperty("contextMenuItemTagOpenImageInNewWindow");
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
    return getLocalizedProperty("contextMenuItemTagCopyImageToClipboard");
}

String contextMenuItemTagOpenFrameInNewWindow()
{
    return getLocalizedProperty("contextMenuItemTagOpenFrameInNewWindow");
}

String contextMenuItemTagCopy()
{
    return getLocalizedProperty("contextMenuItemTagCopy");
}

String contextMenuItemTagGoBack()
{
    return getLocalizedProperty("contextMenuItemTagGoBack");
}

String contextMenuItemTagGoForward()
{
    return getLocalizedProperty("contextMenuItemTagGoForward");
}

String contextMenuItemTagStop()
{
    return getLocalizedProperty("contextMenuItemTagStop");
}

String contextMenuItemTagReload()
{
    return getLocalizedProperty("contextMenuItemTagReload");
}

String contextMenuItemTagCut()
{
    return getLocalizedProperty("contextMenuItemTagCut");
}

String contextMenuItemTagPaste()
{
    return getLocalizedProperty("contextMenuItemTagPaste");
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
    return getLocalizedProperty("contextMenuItemTagOpenLink");
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
    return getLocalizedProperty("searchMenuNoRecentSearchesText");
}

String searchMenuRecentSearchesText()
{
    return getLocalizedProperty("searchMenuRecentSearchesText");
}

String searchMenuClearRecentSearchesText()
{
    return getLocalizedProperty("searchMenuClearRecentSearchesText");
}

String unknownFileSizeText()
{
    return getLocalizedProperty("unknownFileSizeText");
}

String crashedPluginText()
{
    return getLocalizedProperty("crashedPluginText");
}

String blockedPluginByContentSecurityPolicyText()
{
    return getLocalizedProperty("blockedPluginByContentSecurityPolicyText");
}

String inactivePluginText()
{
    return getLocalizedProperty("inactivePluginText");
}

String snapshottedPlugInLabelSubtitle()
{
    return getLocalizedProperty("snapshottedPlugInLabelSubtitle");
}

String snapshottedPlugInLabelTitle()
{
    return getLocalizedProperty("snapshottedPlugInLabelTitle");
}

String missingPluginText()
{
    return getLocalizedProperty("missingPluginText");
}

String insecurePluginVersionText()
{
    return getLocalizedProperty("insecurePluginVersionText");
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

#if ENABLE(VIDEO_TRACK)
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

String textTrackNoLabelText()
{
    return String::fromUTF8("No label");
}

String audioTrackNoLabelText()
{
    return String::fromUTF8("No label");
}
#endif

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
    return getLocalizedProperty("mediaElementLoadingStateText");
}

String mediaElementLiveBroadcastStateText()
{
    return getLocalizedProperty("mediaElementLiveBroadcastStateText");
}

String localizedMediaControlElementString(const String& s)
{
    return getLocalizedProperty(String("localizedMediaControlElementString") + s);
}

String localizedMediaControlElementHelpText(const String& s)
{
    return getLocalizedProperty(String("localizedMediaControlElementHelpText") + s);
}

String localizedMediaTimeDescription(float time)
{
    if (!std::isfinite(time))
        return getLocalizedProperty("localizedMediaTimeDescriptionIndefinite");

    int seconds = (int)fabsf(time);
    int days = seconds / (60 * 60 * 24);
    int hours = seconds / (60 * 60);
    int minutes = (seconds / 60) % 60;
    seconds %= 60;

    String result;
    if (days) {
        String s = getLocalizedProperty("localizedMediaTimeDescriptionDays");
        result.append(String::number(days) + " " + s + "  ");
    }
    if (days || hours) {
        String s = getLocalizedProperty("localizedMediaTimeDescriptionHours");
        result.append(String::number(hours) + " " + s + "  ");
    }
    if (days || hours || minutes) {
        String s = getLocalizedProperty("localizedMediaTimeDescriptionMinutes");
        result.append(String::number(minutes) + " " + s + "  ");
    }
    String s = getLocalizedProperty("localizedMediaTimeDescriptionSeconds");
    return result + String::number(days) + " " + s;
}

String AXWebAreaText()
{
    return getLocalizedProperty("AXWebAreaText");
}

String AXAutoFillCreditCardLabel()
{
    return getLocalizedProperty("AXAutoFillCreditCardLabel");
}

String AXLinkText()
{
    return getLocalizedProperty("AXLinkText");
}

String AXListMarkerText()
{
    return getLocalizedProperty("AXListMarkerText");
}

String AXImageMapText()
{
    return getLocalizedProperty("AXImageMapText");
}

String AXHeadingText()
{
    return getLocalizedProperty("AXHeadingText");
}

String AXDefinitionListTermText()
{
    return getLocalizedProperty("AXDefinitionListTermText");
}

String AXDefinitionListDefinitionText()
{
    return getLocalizedProperty("AXDefinitionListDefinitionText");
}

String AXFigureText()
{
    return getLocalizedProperty("AXFigureText");
}

String AXARIAContentGroupText(const String& ariaType)
{
    return getLocalizedProperty(String("AXARIAContentGroupText") + ariaType);
}

String AXButtonActionVerb()
{
    return getLocalizedProperty("AXButtonActionVerb");
}

String AXRadioButtonActionVerb()
{
    return getLocalizedProperty("AXRadioButtonActionVerb");
}

String AXTextFieldActionVerb()
{
    return getLocalizedProperty("AXTextFieldActionVerb");
}

String AXCheckedCheckBoxActionVerb()
{
    return getLocalizedProperty("AXCheckedCheckBoxActionVerb");
}

String AXUncheckedCheckBoxActionVerb()
{
    return getLocalizedProperty("AXUncheckedCheckBoxActionVerb");
}

String AXLinkActionVerb()
{
    return getLocalizedProperty("AXLinkActionVerb");
}

String AXMenuListPopupActionVerb()
{
    return getLocalizedProperty("AXMenuListPopupActionVerb");
}

String AXMenuListActionVerb()
{
    return getLocalizedProperty("AXMenuListActionVerb");
}

String AXSearchFieldCancelButtonText() {
    notImplemented();
    return String(); // UNSUPPORTED: getLocalizedProperty("AXSearchFieldCancelButtonText");
}

String AXAutoFillStrongPasswordLabel()
{
    // return WEB_UI_STRING("strong password auto fill", "Label for the strong password auto fill button inside a text field.");
    return getLocalizedProperty("AXAutoFillStrongPasswordLabel");
}

String AXAutoFillStrongConfirmationPasswordLabel()
{
    // return WEB_UI_STRING("strong confirmation password auto fill", "Label for the strong confirmation password auto fill button inside a text field.");
    return getLocalizedProperty("AXAutoFillStrongConfirmationPasswordLabel");
}

String autoFillStrongPasswordLabel()
{
    // return WEB_UI_STRING("strong password", "Label for strong password.");
    return getLocalizedProperty("autoFillStrongPasswordLabel");
}

String validationMessageValueMissingText()
{
    return getLocalizedProperty("validationMessageValueMissingText");
}

String validationMessageTypeMismatchText()
{
    return getLocalizedProperty("validationMessageTypeMismatchText");
}

String validationMessagePatternMismatchText()
{
    return getLocalizedProperty("validationMessagePatternMismatchText");
}

String validationMessageTooShortText(int, int)
{
    notImplemented();
    return String::fromUTF8("too short");
}

String validationMessageTooLongText(int, int)
{
    return getLocalizedProperty("validationMessageTooLongText");
}

String validationMessageRangeUnderflowText(const String&)
{
    return getLocalizedProperty("validationMessageRangeUnderflowText");
}

String validationMessageRangeOverflowText(const String&)
{
    return getLocalizedProperty("validationMessageRangeOverflowText");
}

String validationMessageStepMismatchText(const String&, const String&)
{
    return getLocalizedProperty("validationMessageStepMismatchText");
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
    return getLocalizedProperty("validationMessageBadInputForNumberText");
}

#if ENABLE(INPUT_TYPE_WEEK)
// weekFormatInLDML() returns week and year format in LDML, Unicode
// technical standard 35, Locale Data Markup Language, e.g. "'Week' ww, yyyy"
String weekFormatInLDML()
{
    return getLocalizedProperty("weekFormatInLDML");
}
#endif


String defaultDetailsSummaryText()
{
    return getLocalizedProperty("defaultDetailsSummaryText");
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

String localizedString(const char* key)
{
    return String::fromUTF8(key, strlen(key));
}
} // namespace WebCore
