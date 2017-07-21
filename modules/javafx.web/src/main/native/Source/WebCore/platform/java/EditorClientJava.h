/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
 */

#pragma once

#include "EditorClient.h"
#include "TextCheckerClient.h"
#include <wtf/java/JavaEnv.h>

#include <wtf/Deque.h>
#include <wtf/Forward.h>

namespace WebCore {

class Page;

class EditorClientJava final : public EditorClient, public TextCheckerClient {
    WTF_MAKE_NONCOPYABLE(EditorClientJava); WTF_MAKE_FAST_ALLOCATED;
public:
    EditorClientJava(const JLObject &webPage);
    ~EditorClientJava() override;

    bool shouldDeleteRange(Range*) override;
    bool smartInsertDeleteEnabled() override;
    bool isSelectTrailingWhitespaceEnabled() override;
    bool isContinuousSpellCheckingEnabled() override;
    void toggleContinuousSpellChecking() override;
    bool isGrammarCheckingEnabled() override;
    void toggleGrammarChecking() override;
    int spellCheckerDocumentTag() override;

    bool shouldBeginEditing(Range*) override;
    bool shouldEndEditing(Range*) override;
    bool shouldInsertNode(Node*, Range*, EditorInsertAction) override;
    bool shouldInsertText(const String&, Range*, EditorInsertAction) override;
    bool shouldChangeSelectedRange(Range* fromRange, Range* toRange, EAffinity, bool stillSelecting) override;

    bool shouldApplyStyle(StyleProperties*, Range*) override;
    void didApplyStyle() override;
    bool shouldMoveRangeAfterDelete(Range*, Range*) override;

    void didBeginEditing() override;
    void respondToChangedContents() override;
    void respondToChangedSelection(Frame*) override;
    void didChangeSelectionAndUpdateLayout() override;
    void updateEditorStateAfterLayoutIfEditabilityChanged() override;
    void didEndEditing() override;
    void willWriteSelectionToPasteboard(Range*) override;
    void didWriteSelectionToPasteboard() override;
    void getClientPasteboardDataForRange(Range*, Vector<String>& pasteboardTypes, Vector<RefPtr<SharedBuffer> >& pasteboardData) override;

    void discardedComposition(Frame*) override;
    void canceledComposition() override;

    void registerUndoStep(UndoStep&) override;
    void registerRedoStep(UndoStep&) override;
    void clearUndoRedoOperations() override;

    bool canCopyCut(Frame*, bool defaultValue) const override;
    bool canPaste(Frame*, bool defaultValue) const override;
    bool canUndo() const override;
    bool canRedo() const override;

    void undo() override;
    void redo() override;

    void handleKeyboardEvent(KeyboardEvent*) override;
    void handleInputMethodKeydown(KeyboardEvent*) override;

    void textFieldDidBeginEditing(Element*) override;
    void textFieldDidEndEditing(Element*) override;
    void textDidChangeInTextField(Element*) override;
    bool doTextFieldCommandFromEvent(Element*, KeyboardEvent*) override;
    void textWillBeDeletedInTextField(Element*) override;
    void textDidChangeInTextArea(Element*) override;
    void overflowScrollPositionChanged() override;

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
    void setInputMethodState(bool enabled) override;

    // TextCheckerClient member functions
    bool shouldEraseMarkersAfterChangeSelection(TextCheckingType) const override;
    void ignoreWordInSpellDocument(const String&) override;
    void learnWord(const String&) override;
    void checkSpellingOfString(StringView, int* misspellingLocation, int* misspellingLength) override;
    String getAutoCorrectSuggestionForMisspelledWord(const String& misspelledWord) override;
    void checkGrammarOfString(StringView, Vector<GrammarDetail>&, int* badGrammarLocation, int* badGrammarLength) override;

#if USE(UNIFIED_TEXT_CHECKING)
    Vector<TextCheckingResult> checkTextOfParagraph(StringView, TextCheckingTypeMask checkingTypes, const VisibleSelection& currentSelection) override;
#endif

    // For spellcheckers that support multiple languages, it's often important to be able to identify the language in order to
    // provide more accurate correction suggestions. Caller can pass in more text in "context" to aid such spellcheckers on language
    // identification. Noramlly it's the text surrounding the "word" for which we are getting correction suggestions.
    void getGuessesForWord(const String& word, const String& context, const VisibleSelection& currentSelection, Vector<String>& guesses) override;
    void requestCheckingOfString(TextCheckingRequest&, const VisibleSelection& currentSelection) override;
protected:
    JGObject m_webPage;

    bool m_isInRedo;
    Deque<Ref<UndoStep>> m_redoStack;
    Deque<Ref<UndoStep>> m_undoStack;
    static const char* interpretKeyEvent(const KeyboardEvent*);
    static bool handleEditingKeyboardEvent(KeyboardEvent*);
};

} // namespace WebCore
