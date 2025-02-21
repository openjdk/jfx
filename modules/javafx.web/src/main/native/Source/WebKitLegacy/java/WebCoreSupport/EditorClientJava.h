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

#pragma once

#include <WebCore/DOMPasteAccess.h>
#include <WebCore/EditorClient.h>
#include <WebCore/TextCheckerClient.h>
#include <WebCore/PlatformJavaClasses.h>

#include <wtf/Deque.h>
#include <wtf/Forward.h>

namespace WebCore {

class Page;

class EditorClientJava final : public EditorClient, public TextCheckerClient {
    WTF_MAKE_NONCOPYABLE(EditorClientJava); WTF_MAKE_FAST_ALLOCATED;
public:
    EditorClientJava(const JLObject &webPage);
    ~EditorClientJava() override;

    bool shouldDeleteRange(const std::optional<SimpleRange>&) override;
    bool smartInsertDeleteEnabled() override;
    bool isSelectTrailingWhitespaceEnabled() const override;
    bool isContinuousSpellCheckingEnabled() override;
    void toggleContinuousSpellChecking() override;
    bool isGrammarCheckingEnabled() override;
    void toggleGrammarChecking() override;
    int spellCheckerDocumentTag() override;

    bool shouldBeginEditing(const SimpleRange&) override;
    bool shouldEndEditing(const SimpleRange&) override;
    bool shouldInsertNode(Node&, const std::optional<SimpleRange>&, EditorInsertAction) override;
    bool shouldInsertText(const String&, const std::optional<SimpleRange>&, EditorInsertAction) override;
    bool shouldChangeSelectedRange(const std::optional<SimpleRange>& fromRange, const std::optional<SimpleRange>& toRange, Affinity, bool stillSelecting) override;

    bool shouldApplyStyle(const StyleProperties&, const std::optional<SimpleRange>&) override;
    void didApplyStyle() override;
    bool shouldMoveRangeAfterDelete(const SimpleRange&, const SimpleRange&) override;

    void didBeginEditing() override;
    void respondToChangedContents() override;
    void respondToChangedSelection(LocalFrame*) override;
    void didEndUserTriggeredSelectionChanges() override { }
    void updateEditorStateAfterLayoutIfEditabilityChanged() override;
    void didEndEditing() override;
    void willWriteSelectionToPasteboard(const std::optional<SimpleRange>&) override;
    void didWriteSelectionToPasteboard() override;
    virtual void getClientPasteboardData(const std::optional<SimpleRange>&, Vector<std::pair<String, RefPtr<WebCore::SharedBuffer>>>& pasteboardTypesAndData) override;
    void didUpdateComposition() override { }

    DOMPasteAccessResponse requestDOMPasteAccess(DOMPasteAccessCategory, FrameIdentifier, const String& originIdentifier) override;
    void discardedComposition(const Document&) override;
    void canceledComposition() override;

    void registerUndoStep(UndoStep&) override;
    void registerRedoStep(UndoStep&) override;
    void clearUndoRedoOperations() override;

    bool canCopyCut(LocalFrame*, bool defaultValue) const override;
    bool canPaste(LocalFrame*, bool defaultValue) const override;
    bool canUndo() const override;
    bool canRedo() const override;

    void undo() override;
    void redo() override;

    void handleKeyboardEvent(KeyboardEvent&) override;
    void handleInputMethodKeydown(KeyboardEvent&) override;

    void textFieldDidBeginEditing(Element&) override;
    void textFieldDidEndEditing(Element&) override;
    void textDidChangeInTextField(Element&) override;
    bool doTextFieldCommandFromEvent(Element&, KeyboardEvent*) override;
    void textWillBeDeletedInTextField(Element&) override;
    void textDidChangeInTextArea(Element&) override;
    void overflowScrollPositionChanged() override;
    void subFrameScrollPositionChanged() override;

#if USE(APPKIT)
    void uppercaseWord() override;
    void lowercaseWord() override;
    void capitalizeWord() override;
#endif

#if USE(AUTOMATIC_TEXT_REPLACEMENT)
    void showSubstitutionsPanel(bool show) override;
    bool substitutionsPanelIsShowing() override;
    void toggleSmartInsertDelete() override;
    bool isAutomaticQuoteSubstitutionEnabled() override;
    void toggleAutomaticQuoteSubstitution() override;
    bool isAutomaticLinkDetectionEnabled() override;
    void toggleAutomaticLinkDetection() override;
    bool isAutomaticDashSubstitutionEnabled() override;
    void toggleAutomaticDashSubstitution() override;
    bool isAutomaticTextReplacementEnabled() override;
    void toggleAutomaticTextReplacement() override;
    bool isAutomaticSpellingCorrectionEnabled() override;
    void toggleAutomaticSpellingCorrection() override;
#endif

#if ENABLE(DELETION_UI)
    bool shouldShowDeleteInterface(HTMLElement*) override;
#endif

    TextCheckerClient* textChecker() override { return static_cast<TextCheckerClient*>(this); }

    void updateSpellingUIWithGrammarString(const String&, const GrammarDetail& detail) override;
    void updateSpellingUIWithMisspelledWord(const String&) override;
    void showSpellingUI(bool show) override;
    bool spellingUIIsShowing() override;
    void willSetInputMethodState() override;
    void setInputMethodState(Element*) override;

    // TextCheckerClient member functions
    bool shouldEraseMarkersAfterChangeSelection(TextCheckingType) const override;
    void ignoreWordInSpellDocument(const String&) override;
    void learnWord(const String&) override;
    void checkSpellingOfString(StringView, int* misspellingLocation, int* misspellingLength) override;
    void checkGrammarOfString(StringView, Vector<GrammarDetail>&, int* badGrammarLocation, int* badGrammarLength) override;

#if USE(UNIFIED_TEXT_CHECKING)
    Vector<TextCheckingResult> checkTextOfParagraph(StringView, TextCheckingTypeMask checkingTypes, const VisibleSelection& currentSelection) override;
#endif

    // For spellcheckers that support multiple languages, it's often important to be able to identify the language in order to
    // provide more accurate correction suggestions. Caller can pass in more text in "context" to aid such spellcheckers on language
    // identification. Noramlly it's the text surrounding the "word" for which we are getting correction suggestions.
    void getGuessesForWord(const String& word, const String& context, const VisibleSelection& currentSelection, Vector<String>& guesses) override;
    void requestCheckingOfString(TextCheckingRequest&, const VisibleSelection& currentSelection) override;
    bool performTwoStepDrop(DocumentFragment&, const SimpleRange&, bool) final { return false; }
    bool canShowFontPanel() const  { return false; }


protected:
    JGObject m_webPage;

    bool m_isInRedo;
    Deque<Ref<UndoStep>> m_redoStack;
    Deque<Ref<UndoStep>> m_undoStack;
    static const char* interpretKeyEvent(const KeyboardEvent*);
    static bool handleEditingKeyboardEvent(KeyboardEvent*);
};

} // namespace WebCore
