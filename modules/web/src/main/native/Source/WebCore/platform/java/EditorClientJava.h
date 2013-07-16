/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

    virtual void pageDestroyed() OVERRIDE;
    virtual void frameWillDetachPage(Frame*) OVERRIDE;

    virtual bool shouldDeleteRange(Range*) OVERRIDE;
    virtual bool smartInsertDeleteEnabled() OVERRIDE; 
    virtual bool isSelectTrailingWhitespaceEnabled() OVERRIDE;
    virtual bool isContinuousSpellCheckingEnabled() OVERRIDE;
    virtual void toggleContinuousSpellChecking() OVERRIDE;
    virtual bool isGrammarCheckingEnabled() OVERRIDE;
    virtual void toggleGrammarChecking() OVERRIDE;
    virtual int spellCheckerDocumentTag() OVERRIDE;

    virtual bool shouldBeginEditing(Range*) OVERRIDE;
    virtual bool shouldEndEditing(Range*) OVERRIDE;
    virtual bool shouldInsertNode(Node*, Range*, EditorInsertAction) OVERRIDE;
    virtual bool shouldInsertText(const String&, Range*, EditorInsertAction) OVERRIDE;
    virtual bool shouldChangeSelectedRange(Range* fromRange, Range* toRange, EAffinity, bool stillSelecting) OVERRIDE;

    virtual bool shouldApplyStyle(StylePropertySet*, Range*) OVERRIDE;
    virtual bool shouldMoveRangeAfterDelete(Range*, Range*) OVERRIDE;

    virtual void didBeginEditing() OVERRIDE;
    virtual void respondToChangedContents() OVERRIDE;
    virtual void respondToChangedSelection(Frame*) OVERRIDE;
    virtual void didEndEditing() OVERRIDE;
    virtual void willWriteSelectionToPasteboard(Range*) OVERRIDE;
    virtual void didWriteSelectionToPasteboard() OVERRIDE;
    virtual void getClientPasteboardDataForRange(Range*, Vector<String>& pasteboardTypes, Vector<RefPtr<SharedBuffer> >& pasteboardData) OVERRIDE;
    virtual void didSetSelectionTypesForPasteboard() OVERRIDE;

    virtual void registerUndoStep(PassRefPtr<UndoStep>) OVERRIDE;
    virtual void registerRedoStep(PassRefPtr<UndoStep>) OVERRIDE;
    virtual void clearUndoRedoOperations() OVERRIDE;

    virtual bool canCopyCut(Frame*, bool defaultValue) const OVERRIDE;
    virtual bool canPaste(Frame*, bool defaultValue) const OVERRIDE;
    virtual bool canUndo() const OVERRIDE;
    virtual bool canRedo() const OVERRIDE;

    virtual void undo() OVERRIDE;
    virtual void redo() OVERRIDE;

    virtual void handleKeyboardEvent(KeyboardEvent*) OVERRIDE;
    virtual void handleInputMethodKeydown(KeyboardEvent*) OVERRIDE;

    virtual void textFieldDidBeginEditing(Element*) OVERRIDE;
    virtual void textFieldDidEndEditing(Element*) OVERRIDE;
    virtual void textDidChangeInTextField(Element*) OVERRIDE;
    virtual bool doTextFieldCommandFromEvent(Element*, KeyboardEvent*) OVERRIDE;
    virtual void textWillBeDeletedInTextField(Element*) OVERRIDE;
    virtual void textDidChangeInTextArea(Element*) OVERRIDE;

#if USE(APPKIT)
    virtual void uppercaseWord() OVERRIDE;
    virtual void lowercaseWord() OVERRIDE;
    virtual void capitalizeWord() OVERRIDE;
#endif

#if USE(AUTOMATIC_TEXT_REPLACEMENT)
    virtual void showSubstitutionsPanel(bool show) OVERRIDE;
    virtual bool substitutionsPanelIsShowing() OVERRIDE;
    virtual void toggleSmartInsertDelete() OVERRIDE;
    virtual bool isAutomaticQuoteSubstitutionEnabled() OVERRIDE;
    virtual void toggleAutomaticQuoteSubstitution() OVERRIDE;
    virtual bool isAutomaticLinkDetectionEnabled() OVERRIDE;
    virtual void toggleAutomaticLinkDetection() OVERRIDE;
    virtual bool isAutomaticDashSubstitutionEnabled() OVERRIDE;
    virtual void toggleAutomaticDashSubstitution() OVERRIDE;
    virtual bool isAutomaticTextReplacementEnabled() OVERRIDE;
    virtual void toggleAutomaticTextReplacement() OVERRIDE;
    virtual bool isAutomaticSpellingCorrectionEnabled() OVERRIDE;
    virtual void toggleAutomaticSpellingCorrection() OVERRIDE;
#endif

#if ENABLE(DELETION_UI)
    virtual bool shouldShowDeleteInterface(HTMLElement*) OVERRIDE;
#endif

    virtual TextCheckerClient* textChecker() OVERRIDE { return static_cast<TextCheckerClient*>(this); }

    virtual void updateSpellingUIWithGrammarString(const String&, const GrammarDetail& detail) OVERRIDE;
    virtual void updateSpellingUIWithMisspelledWord(const String&) OVERRIDE;
    virtual void showSpellingUI(bool show) OVERRIDE;
    virtual bool spellingUIIsShowing() OVERRIDE;
    virtual void willSetInputMethodState() OVERRIDE;
    virtual void setInputMethodState(bool enabled) OVERRIDE;

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
