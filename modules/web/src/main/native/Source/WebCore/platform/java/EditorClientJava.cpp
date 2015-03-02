/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "NotImplemented.h"

#include "CharacterData.h"
#include "Document.h"
#include "EditCommand.h"
#include "Editor.h"
#include "NodeList.h"
#include "EditorClientJava.h"
#include "FocusController.h"
#include "Frame.h"
#include "FrameView.h"
#include "htmlediting.h"
#include "JavaEnv.h"
#include "KeyboardEvent.h"
#include "PlatformKeyboardEvent.h"
#include "TextIterator.h"
#include "Widget.h"
//#include "visible_units.h"

#include <wtf/Assertions.h>

#include <iostream>

#include "com_sun_webkit_event_WCKeyEvent.h"

using namespace std;

namespace WebCore {


EditorClientJava::EditorClientJava(const JLObject &webPage)
    : m_webPage(webPage), m_isInRedo(false)
{
}

EditorClientJava::~EditorClientJava()
{
}

void EditorClientJava::pageDestroyed()
{
    notImplemented();

    delete this;
}

void dump(int indent, Node* node)
{
    for (int i=0; i<indent; i++) cout << " ";
    cout << node->nodeType() << node->nodeName().deprecatedCharacters() << endl;
    for (int i=0; i<node->childNodes()->length(); i++) {
        dump(indent+2, node->childNodes()->item(i));
    }
}

//
// The below keyboard event handling code was adapted from
// WebKit/chromium/src/EditorClientImpl.cpp and WebKit/win/WebView.cpp
//

static const int VKEY_BACK = com_sun_webkit_event_WCKeyEvent_VK_BACK;
static const int VKEY_TAB = com_sun_webkit_event_WCKeyEvent_VK_TAB;
static const int VKEY_RETURN = com_sun_webkit_event_WCKeyEvent_VK_RETURN;
static const int VKEY_ESCAPE = com_sun_webkit_event_WCKeyEvent_VK_ESCAPE;
static const int VKEY_PRIOR = com_sun_webkit_event_WCKeyEvent_VK_PRIOR;
static const int VKEY_NEXT = com_sun_webkit_event_WCKeyEvent_VK_NEXT;
static const int VKEY_END = com_sun_webkit_event_WCKeyEvent_VK_END;
static const int VKEY_HOME = com_sun_webkit_event_WCKeyEvent_VK_HOME;
static const int VKEY_LEFT = com_sun_webkit_event_WCKeyEvent_VK_LEFT;
static const int VKEY_UP = com_sun_webkit_event_WCKeyEvent_VK_UP;
static const int VKEY_RIGHT = com_sun_webkit_event_WCKeyEvent_VK_RIGHT;
static const int VKEY_DOWN = com_sun_webkit_event_WCKeyEvent_VK_DOWN;
static const int VKEY_INSERT = com_sun_webkit_event_WCKeyEvent_VK_INSERT;
static const int VKEY_DELETE = com_sun_webkit_event_WCKeyEvent_VK_DELETE;
static const int VKEY_OEM_PERIOD = com_sun_webkit_event_WCKeyEvent_VK_OEM_PERIOD;

static const unsigned CtrlKey = 1 << 0;
static const unsigned AltKey = 1 << 1;
static const unsigned ShiftKey = 1 << 2;
static const unsigned MetaKey = 1 << 3;
#if OS(DARWIN)
// Aliases for the generic key defintions to make kbd shortcuts definitions more
// readable on OS X.
static const unsigned OptionKey  = AltKey;

// Do not use this constant for anything but cursor movement commands.
static const unsigned CommandKey = MetaKey;
#endif

struct KeyDownEntry {
    unsigned virtualKey;
    unsigned modifiers;
    const char* name;
};

struct KeyPressEntry {
    unsigned charCode;
    unsigned modifiers;
    const char* name;
};

static const KeyDownEntry keyDownEntries[] = {
    { VKEY_LEFT,   0,                  "MoveLeft"                             },
    { VKEY_LEFT,   ShiftKey,           "MoveLeftAndModifySelection"           },
#if OS(DARWIN)
    { VKEY_LEFT,   OptionKey,          "MoveWordLeft"                         },
    { VKEY_LEFT,   OptionKey | ShiftKey,
        "MoveWordLeftAndModifySelection"                                      },
#else
    { VKEY_LEFT,   CtrlKey,            "MoveWordLeft"                         },
    { VKEY_LEFT,   CtrlKey | ShiftKey,
        "MoveWordLeftAndModifySelection"                                      },
#endif
    { VKEY_RIGHT,  0,                  "MoveRight"                            },
    { VKEY_RIGHT,  ShiftKey,           "MoveRightAndModifySelection"          },
#if OS(DARWIN)
    { VKEY_RIGHT,  OptionKey,          "MoveWordRight"                        },
    { VKEY_RIGHT,  OptionKey | ShiftKey,
      "MoveWordRightAndModifySelection"                                       },
#else
    { VKEY_RIGHT,  CtrlKey,            "MoveWordRight"                        },
    { VKEY_RIGHT,  CtrlKey | ShiftKey,
      "MoveWordRightAndModifySelection"                                       },
#endif
    { VKEY_UP,     0,                  "MoveUp"                               },
    { VKEY_UP,     ShiftKey,           "MoveUpAndModifySelection"             },
    { VKEY_PRIOR,  ShiftKey,           "MovePageUpAndModifySelection"         },
    { VKEY_DOWN,   0,                  "MoveDown"                             },
    { VKEY_DOWN,   ShiftKey,           "MoveDownAndModifySelection"           },
    { VKEY_NEXT,   ShiftKey,           "MovePageDownAndModifySelection"       },
#if !OS(DARWIN)
    { VKEY_PRIOR,  0,                  "MovePageUp"                           },
    { VKEY_NEXT,   0,                  "MovePageDown"                         },
#endif
    { VKEY_HOME,   0,                  "MoveToBeginningOfLine"                },
    { VKEY_HOME,   ShiftKey,
        "MoveToBeginningOfLineAndModifySelection"                             },
#if OS(DARWIN)
    { VKEY_LEFT,   CommandKey,         "MoveToBeginningOfLine"                },
    { VKEY_LEFT,   CommandKey | ShiftKey,
      "MoveToBeginningOfLineAndModifySelection"                               },
    { VKEY_PRIOR,  OptionKey,          "MovePageUp"                           },
    { VKEY_NEXT,   OptionKey,          "MovePageDown"                         },
#endif
#if OS(DARWIN)
    { VKEY_UP,     CommandKey,         "MoveToBeginningOfDocument"            },
    { VKEY_UP,     CommandKey | ShiftKey,
        "MoveToBeginningOfDocumentAndModifySelection"                         },
#else
    { VKEY_HOME,   CtrlKey,            "MoveToBeginningOfDocument"            },
    { VKEY_HOME,   CtrlKey | ShiftKey,
        "MoveToBeginningOfDocumentAndModifySelection"                         },
#endif
    { VKEY_END,    0,                  "MoveToEndOfLine"                      },
    { VKEY_END,    ShiftKey,           "MoveToEndOfLineAndModifySelection"    },
#if OS(DARWIN)
    { VKEY_DOWN,   CommandKey,         "MoveToEndOfDocument"                  },
    { VKEY_DOWN,   CommandKey | ShiftKey,
        "MoveToEndOfDocumentAndModifySelection"                               },
#else
    { VKEY_END,    CtrlKey,            "MoveToEndOfDocument"                  },
    { VKEY_END,    CtrlKey | ShiftKey,
        "MoveToEndOfDocumentAndModifySelection"                               },
#endif
#if OS(DARWIN)
    { VKEY_RIGHT,  CommandKey,         "MoveToEndOfLine"                      },
    { VKEY_RIGHT,  CommandKey | ShiftKey,
        "MoveToEndOfLineAndModifySelection"                                   },
#endif
    { VKEY_BACK,   0,                  "DeleteBackward"                       },
    { VKEY_BACK,   ShiftKey,           "DeleteBackward"                       },
    { VKEY_DELETE, 0,                  "DeleteForward"                        },
#if OS(DARWIN)
    { VKEY_BACK,   OptionKey,          "DeleteWordBackward"                   },
    { VKEY_DELETE, OptionKey,          "DeleteWordForward"                    },
#else
    { VKEY_BACK,   CtrlKey,            "DeleteWordBackward"                   },
    { VKEY_DELETE, CtrlKey,            "DeleteWordForward"                    },
#endif
    { 'B',         CtrlKey,            "ToggleBold"                           },
    { 'I',         CtrlKey,            "ToggleItalic"                         },
    { 'U',         CtrlKey,            "ToggleUnderline"                      },
    { VKEY_ESCAPE, 0,                  "Cancel"                               },
    { VKEY_OEM_PERIOD, CtrlKey,        "Cancel"                               },
    { VKEY_TAB,    0,                  "InsertTab"                            },
    { VKEY_TAB,    ShiftKey,           "InsertBacktab"                        },
    { VKEY_RETURN, 0,                  "InsertNewline"                        },
    { VKEY_RETURN, CtrlKey,            "InsertNewline"                        },
    { VKEY_RETURN, AltKey,             "InsertNewline"                        },
    { VKEY_RETURN, AltKey | ShiftKey,  "InsertNewline"                        },
    { VKEY_RETURN, ShiftKey,           "InsertLineBreak"                      },
    { VKEY_INSERT, CtrlKey,            "Copy"                                 },
    { VKEY_INSERT, ShiftKey,           "Paste"                                },
    { VKEY_DELETE, ShiftKey,           "Cut"                                  },
#if OS(DARWIN)
    // We differ from Chromium here in that we implement
    // the {Meta|Ctrl}-{C|V|X|A|Z|Y} shortcuts for both OS X
    // and non-OS X platforms here, whereas Chromium has the
    // OS X handling of these shortcuts implemented elsewhere
    { 'C',         MetaKey,            "Copy"                                 },
    { 'V',         MetaKey,            "Paste"                                },
    { 'V',         MetaKey | ShiftKey, "PasteAndMatchStyle"                   },
    { 'X',         MetaKey,            "Cut"                                  },
    { 'A',         MetaKey,            "SelectAll"                            },
    { 'Z',         MetaKey,            "Undo"                                 },
    { 'Z',         MetaKey | ShiftKey, "Redo"                                 },
    { 'Y',         MetaKey,            "Redo"                                 },
#else
    { 'C',         CtrlKey,            "Copy"                                 },
    { 'V',         CtrlKey,            "Paste"                                },
    { 'V',         CtrlKey | ShiftKey, "PasteAndMatchStyle"                   },
    { 'X',         CtrlKey,            "Cut"                                  },
    { 'A',         CtrlKey,            "SelectAll"                            },
    { 'Z',         CtrlKey,            "Undo"                                 },
    { 'Z',         CtrlKey | ShiftKey, "Redo"                                 },
    { 'Y',         CtrlKey,            "Redo"                                 },
#endif
};

static const KeyPressEntry keyPressEntries[] = {
    { '\t',   0,                  "InsertTab"                                 },
    { '\t',   ShiftKey,           "InsertBacktab"                             },
    { '\r',   0,                  "InsertNewline"                             },
    { '\r',   CtrlKey,            "InsertNewline"                             },
    { '\r',   ShiftKey,           "InsertLineBreak"                           },
    { '\r',   AltKey,             "InsertNewline"                             },
    { '\r',   AltKey | ShiftKey,  "InsertNewline"                             },
};

const char* EditorClientJava::interpretKeyEvent(const KeyboardEvent* evt)
{
    const PlatformKeyboardEvent* keyEvent = evt->keyEvent();
    if (!keyEvent)
        return "";

    static HashMap<int, const char*>* keyDownCommandsMap = 0;
    static HashMap<int, const char*>* keyPressCommandsMap = 0;

    if (!keyDownCommandsMap) {
        keyDownCommandsMap = new HashMap<int, const char*>;
        keyPressCommandsMap = new HashMap<int, const char*>;

        for (unsigned i = 0; i < WTF_ARRAY_LENGTH(keyDownEntries); i++) {
            keyDownCommandsMap->set(keyDownEntries[i].modifiers << 16 | keyDownEntries[i].virtualKey,
                                    keyDownEntries[i].name);
        }

        for (unsigned i = 0; i < WTF_ARRAY_LENGTH(keyPressEntries); i++) {
            keyPressCommandsMap->set(keyPressEntries[i].modifiers << 16 | keyPressEntries[i].charCode,
                                     keyPressEntries[i].name);
        }
    }

    unsigned modifiers = 0;
    if (keyEvent->shiftKey())
        modifiers |= ShiftKey;
    if (keyEvent->altKey())
        modifiers |= AltKey;
    if (keyEvent->ctrlKey())
        modifiers |= CtrlKey;
    if (keyEvent->metaKey())
        modifiers |= MetaKey;

    if (keyEvent->type() == PlatformKeyboardEvent::RawKeyDown) {
        int mapKey = modifiers << 16 | evt->keyCode();
        return mapKey ? keyDownCommandsMap->get(mapKey) : 0;
    }

    int mapKey = modifiers << 16 | evt->charCode();
    return mapKey ? keyPressCommandsMap->get(mapKey) : 0;
}

bool EditorClientJava::handleEditingKeyboardEvent(KeyboardEvent* evt)
{
    const PlatformKeyboardEvent* keyEvent = evt->keyEvent();
    if (!keyEvent)
        return false;

    Frame* frame = evt->target()->toNode()->document().frame();
    if (!frame)
        return false;

    String commandName = interpretKeyEvent(evt);
    Editor::Command command = frame->editor().command(commandName);

    if (keyEvent->type() == PlatformKeyboardEvent::RawKeyDown) {
        // WebKit doesn't have enough information about mode to decide how
        // commands that just insert text if executed via Editor should be treated,
        // so we leave it upon WebCore to either handle them immediately
        // (e.g. Tab that changes focus) or let a keypress event be generated
        // (e.g. Tab that inserts a Tab character, or Enter).
        if (command.isTextInsertion() || commandName.isEmpty())
            return false;
        return command.execute(evt);
    }

    if (command.execute(evt)) {
        return true;
    }

    // Here we need to filter key events.
    // On Gtk/Linux, it emits key events with ASCII text and ctrl on for ctrl-<x>.
    // In Webkit, EditorClient::handleKeyboardEvent in
    // WebKit/gtk/WebCoreSupport/EditorClientGtk.cpp drop such events.
    // On Mac, it emits key events with ASCII text and meta on for Command-<x>.
    // These key events should not emit text insert event.
    // Alt key would be used to insert alternative character, so we should let
    // through. Also note that Ctrl-Alt combination equals to AltGr key which is
    // also used to insert alternative character.
    // http://code.google.com/p/chromium/issues/detail?id=10846
    // Windows sets both alt and meta are on when "Alt" key pressed.
    // http://code.google.com/p/chromium/issues/detail?id=2215
    // Also, we should not rely on an assumption that keyboards don't
    // send ASCII characters when pressing a control key on Windows,
    // which may be configured to do it so by user.
    // See also http://en.wikipedia.org/wiki/Keyboard_Layout
    // FIXME(ukai): investigate more detail for various keyboard layout.
    if (evt->keyEvent()->text().length() == 1) {
        UChar ch = evt->keyEvent()->text()[0U];

        // Don't insert null or control characters as they can result in
        // unexpected behaviour
        if (ch < ' ')
            return false;
#if !OS(WINDOWS)
        // Don't insert ASCII character if ctrl w/o alt or meta is on.
        // On Mac, we should ignore events when meta is on (Command-<x>).
        if (ch < 0x80) {
            if (evt->keyEvent()->ctrlKey() && !evt->keyEvent()->altKey())
                return false;
#if OS(DARWIN)
            if (evt->keyEvent()->metaKey())
                return false;
#endif
        }
#endif
    }

    if (!frame->editor().canEdit())
        return false;

    return frame->editor().insertText(evt->keyEvent()->text(), evt);
}

void EditorClientJava::handleKeyboardEvent(KeyboardEvent* evt)
{
    if (handleEditingKeyboardEvent(evt)) {
        evt->setDefaultHandled();
    }
}

bool EditorClientJava::shouldDeleteRange(Range*)
{
    notImplemented();
    return true;
}

#if ENABLE(DELETION_UI)
bool EditorClientJava::shouldShowDeleteInterface(HTMLElement*)
{
    return false;
}
#endif

bool EditorClientJava::isContinuousSpellCheckingEnabled()
{
    notImplemented();
    return false;
}

bool EditorClientJava::isGrammarCheckingEnabled()
{
    notImplemented();
    return false;
}

bool EditorClientJava::isSelectTrailingWhitespaceEnabled()
{
    notImplemented();
    return false;
}

int EditorClientJava::spellCheckerDocumentTag()
{
    notImplemented();
    return 0;
}

bool EditorClientJava::shouldBeginEditing(WebCore::Range*)
{
    notImplemented();
    return true;
}

bool EditorClientJava::shouldEndEditing(WebCore::Range*)
{
    notImplemented();
    return true;
}

bool EditorClientJava::shouldInsertText(const String&, Range*, EditorInsertAction)
{
    notImplemented();
    return true;
}

bool EditorClientJava::shouldChangeSelectedRange(Range*, Range*, EAffinity, bool)
{
    return true;
}

bool EditorClientJava::shouldApplyStyle(StyleProperties*, Range*)
{
    return true;
}

void EditorClientJava::didBeginEditing()
{
    notImplemented();
}

void EditorClientJava::respondToChangedContents()
{
    notImplemented();
}

void EditorClientJava::respondToChangedSelection(Frame *frame)
{
    //Frame* frame = page()->focusController()->focusedOrMainFrame();
    if (!frame || !frame->editor().hasComposition()
        || frame->editor().ignoreCompositionSelectionChange()) {
        return;
    }
    unsigned start, end;
    if (!frame->editor().getCompositionSelection(start, end)) {
        // Commit composed text here outside the Java Input Method
        // Framework. InputContext.endComposition() will be called
        // later through a setInputMethodState() call. The
        // endComposition call will generate an InputMethodEvent with
        // committed text which will be ignored in
        // JWebPane.processInputMethodEvent().
        frame->editor().cancelComposition();
        setInputMethodState(false);
    }
}

void EditorClientJava::didEndEditing()
{
    notImplemented();
}

void EditorClientJava::didWriteSelectionToPasteboard()
{
    notImplemented();
}

bool EditorClientJava::canUndo() const
{
    return !m_undoStack.isEmpty();
}

bool EditorClientJava::canRedo() const
{
    return !m_redoStack.isEmpty();
}

void EditorClientJava::undo()
{
    if (canUndo()) {
        RefPtr<WebCore::UndoStep> step(*(--m_undoStack.end()));
        m_undoStack.remove(--m_undoStack.end());
        // unapply will call us back to push this command onto the redo stack.
        step->unapply();
    }
}

void EditorClientJava::redo()
{
    if (canRedo()) {
        RefPtr<WebCore::UndoStep> step(*(--m_redoStack.end()));
        m_redoStack.remove(--m_redoStack.end());

        ASSERT(!m_isInRedo);
        m_isInRedo = true;
        // reapply will call us back to push this command onto the undo stack.
        step->reapply();
        m_isInRedo = false;
    }
}

bool EditorClientJava::shouldInsertNode(Node*, Range*, EditorInsertAction)
{
    notImplemented();
    return true;
}

bool EditorClientJava::smartInsertDeleteEnabled()
{
    notImplemented();
    return false;
}

void EditorClientJava::toggleContinuousSpellChecking()
{
    notImplemented();
}

void EditorClientJava::toggleGrammarChecking()
{
    notImplemented();
}

void EditorClientJava::textFieldDidBeginEditing(Element*)
{
    notImplemented();
}

void EditorClientJava::textFieldDidEndEditing(Element*)
{
    notImplemented();
}

void EditorClientJava::textDidChangeInTextField(Element*)
{
    notImplemented();
}

bool EditorClientJava::doTextFieldCommandFromEvent(Element*, KeyboardEvent*)
{
    notImplemented();
    return false;
}

void EditorClientJava::textWillBeDeletedInTextField(Element*)
{
    notImplemented();
}

void EditorClientJava::textDidChangeInTextArea(Element*)
{
    notImplemented();
}

void EditorClientJava::updateSpellingUIWithGrammarString(const String&, const GrammarDetail&)
{
    notImplemented();
}

void EditorClientJava::updateSpellingUIWithMisspelledWord(const String&)
{
    notImplemented();
}

void EditorClientJava::showSpellingUI(bool)
{
    notImplemented();
}

bool EditorClientJava::spellingUIIsShowing()
{
    notImplemented();
    return false;
}


bool EditorClientJava::shouldMoveRangeAfterDelete(Range*, Range*)
{
    notImplemented();
    return true;
}

void EditorClientJava::setInputMethodState(bool enabled)
{
    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID midSetInputMethodState = env->GetMethodID(
        PG_GetWebPageClass(env),
        "setInputMethodState",
        "(Z)V");
    ASSERT(midSetInputMethodState);

    env->CallVoidMethod(
        m_webPage,
        midSetInputMethodState,
        bool_to_jbool(enabled));
    CheckAndClearException(env);
}

void EditorClientJava::handleInputMethodKeydown(KeyboardEvent*)
{
    notImplemented();
}

void EditorClientJava::willSetInputMethodState()
{
    notImplemented();
}

bool EditorClientJava::canCopyCut(Frame*, bool defaultValue) const
{
    return defaultValue;
}

bool EditorClientJava::canPaste(Frame*, bool defaultValue) const
{
    return defaultValue;
}

const int gc_maximumm_undoStackDepth = 1000;
void EditorClientJava::registerUndoStep(PassRefPtr<UndoStep> step)
{
    if (m_undoStack.size() == gc_maximumm_undoStackDepth)
        m_undoStack.removeFirst();
    if (!m_isInRedo)
        m_redoStack.clear();
    m_undoStack.append(step);
}

void EditorClientJava::registerRedoStep(PassRefPtr<UndoStep> step)
{
    m_redoStack.append(step);
}

void EditorClientJava::clearUndoRedoOperations()
{
    m_undoStack.clear();
    m_redoStack.clear();
}

void EditorClientJava::getClientPasteboardDataForRange(Range*, Vector<String>&, Vector<RefPtr<SharedBuffer> >&)
{
}

void EditorClientJava::willWriteSelectionToPasteboard(Range*)
{
}


} // namespace WebCore
