/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef EditorClientJava_h
#define EditorClientJava_h

#include "EditorClient.h"
#include "TextCheckerClient.h"
#include "JavaEnv.h"
#include "Page.h"

namespace WebCore {

class Page;

//utatodo: replace by real spell checker
class EmptyTextCheckerClient : public TextCheckerClient {
public:
    virtual bool shouldEraseMarkersAfterChangeSelection(TextCheckingType) const { return true; }
    virtual void ignoreWordInSpellDocument(const String&) { }
    virtual void learnWord(const String&) { }
    virtual void checkSpellingOfString(const UChar*, int, int*, int*) { }
    virtual String getAutoCorrectSuggestionForMisspelledWord(const String&) { return String(); }
    virtual void checkGrammarOfString(const UChar*, int, Vector<GrammarDetail>&, int*, int*) { }

#if USE(UNIFIED_TEXT_CHECKING)
    virtual void checkTextOfParagraph(const UChar*, int, TextCheckingTypeMask, Vector<TextCheckingResult>&) { };
#endif

    virtual void getGuessesForWord(const String&, const String&, Vector<String>&) { }
    virtual void requestCheckingOfString(PassRefPtr<TextCheckingRequest>) OVERRIDE;
};

class EditorClientJava : public EditorClient, public EmptyTextCheckerClient {
    WTF_MAKE_NONCOPYABLE(EditorClientJava); WTF_MAKE_FAST_ALLOCATED;
public:
    EditorClientJava(const JLObject &webPage);
    virtual ~EditorClientJava();

    // from EditorClient
    virtual void pageDestroyed();
    virtual void frameWillDetachPage(Frame*) { }

    virtual bool shouldDeleteRange(Range*);
    virtual bool shouldShowDeleteInterface(HTMLElement*);
    virtual bool smartInsertDeleteEnabled();
    virtual bool isContinuousSpellCheckingEnabled();
    virtual void toggleContinuousSpellChecking();
    virtual bool isGrammarCheckingEnabled();
    virtual bool isSelectTrailingWhitespaceEnabled();
    virtual void toggleGrammarChecking();
    virtual int spellCheckerDocumentTag();

    virtual bool selectWordBeforeMenuEvent();

    virtual bool shouldBeginEditing(Range*);
    virtual bool shouldEndEditing(Range*);
    virtual bool shouldInsertNode(Node*, Range*, EditorInsertAction);
    virtual bool shouldInsertText(const String&, Range*, EditorInsertAction);
    virtual bool shouldChangeSelectedRange(Range* fromRange, Range* toRange, EAffinity, bool stillSelecting);

    virtual bool shouldApplyStyle(StylePropertySet*, Range*);
    virtual bool shouldMoveRangeAfterDelete(Range*, Range*);

    virtual void didBeginEditing();
    virtual void respondToChangedContents();
    virtual void respondToChangedSelection(Frame *frame);
    virtual void didEndEditing();
    virtual void didWriteSelectionToPasteboard();
    virtual void didSetSelectionTypesForPasteboard();

    virtual void registerUndoStep(PassRefPtr<UndoStep>) OVERRIDE {}
    virtual void registerRedoStep(PassRefPtr<UndoStep>) OVERRIDE {}
    virtual void clearUndoRedoOperations() OVERRIDE {}

    virtual bool canUndo() const;
    virtual bool canRedo() const;
    virtual bool canCopyCut(Frame*, bool defaultValue) const;
    virtual bool canPaste(Frame*, bool defaultValue) const;

    virtual void undo();
    virtual void redo();

    virtual void handleKeyboardEvent(KeyboardEvent*);
    virtual void handleInputMethodKeydown(KeyboardEvent*);
    virtual void willSetInputMethodState();

    virtual void textFieldDidBeginEditing(Element*);
    virtual void textFieldDidEndEditing(Element*);
    virtual void textDidChangeInTextField(Element*);
    virtual bool doTextFieldCommandFromEvent(Element*, KeyboardEvent*);
    virtual void textWillBeDeletedInTextField(Element*);
    virtual void textDidChangeInTextArea(Element*);

    virtual void ignoreWordInSpellDocument(const String&);
    virtual void learnWord(const String&);
    virtual void checkSpellingOfString(const UChar*, int length, int* misspellingLocation, int* misspellingLength);
    virtual String getAutoCorrectSuggestionForMisspelledWord(const String& misspelledWord);
    virtual void checkGrammarOfString(const UChar*, int length, Vector<GrammarDetail>&, int* badGrammarLocation, int* badGrammarLength);
    virtual void updateSpellingUIWithGrammarString(const String&, const GrammarDetail&);
    virtual void updateSpellingUIWithMisspelledWord(const String&);
    virtual void showSpellingUI(bool show);
    virtual bool spellingUIIsShowing();
    virtual void getGuessesForWord(const String&, Vector<String>& guesses);

    virtual void setInputMethodState(bool enabled);
    virtual TextCheckerClient* textChecker() { return static_cast<TextCheckerClient*>(this); }
private:
    static const char* interpretKeyEvent(const KeyboardEvent*);
    static bool handleEditingKeyboardEvent(KeyboardEvent*);

    JGObject m_webPage;
};

}

#endif
