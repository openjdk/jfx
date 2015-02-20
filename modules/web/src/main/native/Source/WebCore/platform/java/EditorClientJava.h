/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef EditorClientJava_h
#define EditorClientJava_h

#include "EditorClient.h"
#include "EmptyClients.h"
#include "TextCheckerClient.h"
#include "JavaEnv.h"
#include "Page.h"

#include <wtf/Deque.h>
#include <wtf/Forward.h>

namespace WebCore {

class Page;

class EditorClientJava : public EditorClient, public EmptyTextCheckerClient {
    WTF_MAKE_NONCOPYABLE(EditorClientJava); WTF_MAKE_FAST_ALLOCATED;
public:
    EditorClientJava(const JLObject &webPage);
    virtual ~EditorClientJava();

    virtual void pageDestroyed() override;

    virtual bool shouldDeleteRange(Range*) override;
    virtual bool smartInsertDeleteEnabled() override; 
    virtual bool isSelectTrailingWhitespaceEnabled() override;
    virtual bool isContinuousSpellCheckingEnabled() override;
    virtual void toggleContinuousSpellChecking() override;
    virtual bool isGrammarCheckingEnabled() override;
    virtual void toggleGrammarChecking() override;
    virtual int spellCheckerDocumentTag() override;

    virtual bool shouldBeginEditing(Range*) override;
    virtual bool shouldEndEditing(Range*) override;
    virtual bool shouldInsertNode(Node*, Range*, EditorInsertAction) override;
    virtual bool shouldInsertText(const String&, Range*, EditorInsertAction) override;
    virtual bool shouldChangeSelectedRange(Range* fromRange, Range* toRange, EAffinity, bool stillSelecting) override;

    virtual bool shouldApplyStyle(StyleProperties*, Range*) override;
    virtual bool shouldMoveRangeAfterDelete(Range*, Range*) override;

    virtual void didBeginEditing() override;
    virtual void respondToChangedContents() override;
    virtual void respondToChangedSelection(Frame*) override;
    virtual void didEndEditing() override;
    virtual void willWriteSelectionToPasteboard(Range*) override;
    virtual void didWriteSelectionToPasteboard() override;
    virtual void getClientPasteboardDataForRange(Range*, Vector<String>& pasteboardTypes, Vector<RefPtr<SharedBuffer> >& pasteboardData) override;

    virtual void registerUndoStep(PassRefPtr<UndoStep>) override;
    virtual void registerRedoStep(PassRefPtr<UndoStep>) override;
    virtual void clearUndoRedoOperations() override;

    virtual bool canCopyCut(Frame*, bool defaultValue) const override;
    virtual bool canPaste(Frame*, bool defaultValue) const override;
    virtual bool canUndo() const override;
    virtual bool canRedo() const override;

    virtual void undo() override;
    virtual void redo() override;

    virtual void handleKeyboardEvent(KeyboardEvent*) override;
    virtual void handleInputMethodKeydown(KeyboardEvent*) override;

    virtual void textFieldDidBeginEditing(Element*) override;
    virtual void textFieldDidEndEditing(Element*) override;
    virtual void textDidChangeInTextField(Element*) override;
    virtual bool doTextFieldCommandFromEvent(Element*, KeyboardEvent*) override;
    virtual void textWillBeDeletedInTextField(Element*) override;
    virtual void textDidChangeInTextArea(Element*) override;

#if USE(APPKIT)
    virtual void uppercaseWord() override;
    virtual void lowercaseWord() override;
    virtual void capitalizeWord() override;
#endif

#if USE(AUTOMATIC_TEXT_REPLACEMENT)
    virtual void showSubstitutionsPanel(bool show) override;
    virtual bool substitutionsPanelIsShowing() override;
    virtual void toggleSmartInsertDelete() override;
    virtual bool isAutomaticQuoteSubstitutionEnabled() override;
    virtual void toggleAutomaticQuoteSubstitution() override;
    virtual bool isAutomaticLinkDetectionEnabled() override;
    virtual void toggleAutomaticLinkDetection() override;
    virtual bool isAutomaticDashSubstitutionEnabled() override;
    virtual void toggleAutomaticDashSubstitution() override;
    virtual bool isAutomaticTextReplacementEnabled() override;
    virtual void toggleAutomaticTextReplacement() override;
    virtual bool isAutomaticSpellingCorrectionEnabled() override;
    virtual void toggleAutomaticSpellingCorrection() override;
#endif

#if ENABLE(DELETION_UI)
    virtual bool shouldShowDeleteInterface(HTMLElement*) override;
#endif

    virtual TextCheckerClient* textChecker() override { return static_cast<TextCheckerClient*>(this); }

    virtual void updateSpellingUIWithGrammarString(const String&, const GrammarDetail& detail) override;
    virtual void updateSpellingUIWithMisspelledWord(const String&) override;
    virtual void showSpellingUI(bool show) override;
    virtual bool spellingUIIsShowing() override;
    virtual void willSetInputMethodState() override;
    virtual void setInputMethodState(bool enabled) override;

protected:
    bool m_isInRedo;
    Deque<RefPtr<UndoStep> > m_redoStack;
    Deque<RefPtr<UndoStep> > m_undoStack;
    static const char* interpretKeyEvent(const KeyboardEvent*);
    static bool handleEditingKeyboardEvent(KeyboardEvent*);

    JGObject m_webPage;
};

}

#endif
