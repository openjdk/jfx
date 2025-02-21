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


#include "EditorClientJava.h"

#include <WebCore/NotImplemented.h>
#include <WebCore/CharacterData.h>
#include <WebCore/Document.h>
#include <WebCore/Editor.h>
#include <WebCore/NodeList.h>
#include <WebCore/FocusController.h>
#include <WebCore/Frame.h>
#include <WebCore/FrameView.h>
#include <WebCore/PlatformJavaClasses.h>
#include <WebCore/KeyboardEvent.h>
#include <WebCore/Page.h>
#include <WebCore/PlatformKeyboardEvent.h>
#include <WebCore/TextIterator.h>
#include <WebCore/Widget.h>

#include <wtf/Assertions.h>
//#include <wtf/text/ASCIILiteral.h>


#include "com_sun_webkit_event_WCKeyEvent.h"

using namespace std;

namespace WebCore {


EditorClientJava::EditorClientJava(const JLObject &webPage)
    : m_webPage(webPage)
    , m_isInRedo(false)
{
}

EditorClientJava::~EditorClientJava()
{
}

void dump(int, Node*)
{
    // for (int i=0; i<indent; i++) cout << " "; //XXX: uncomment
    // cout << node->nodeType() << StringView(node->nodeName()) << endl;
    // for (int i=0; i<node->childNodes()->length(); i++) {
    //     dump(indent+2, node->childNodes()->item(i));
    // }
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
    const PlatformKeyboardEvent* keyEvent = evt->underlyingPlatformEvent();
    if (!keyEvent)
        return "";

    static HashMap<int, const char*>* keyDownCommandsMap = 0;
    static HashMap<int, const char*>* keyPressCommandsMap = 0;

    if (!keyDownCommandsMap) {
        keyDownCommandsMap = new HashMap<int, const char*>;
        keyPressCommandsMap = new HashMap<int, const char*>;

        for (unsigned i = 0; i < std::size(keyDownEntries); i++) {
            keyDownCommandsMap->set(keyDownEntries[i].modifiers << 16 | keyDownEntries[i].virtualKey,
                                    keyDownEntries[i].name);
        }

        for (unsigned i = 0; i < std::size(keyPressEntries); i++) {
            keyPressCommandsMap->set(keyPressEntries[i].modifiers << 16 | keyPressEntries[i].charCode,
                                     keyPressEntries[i].name);
        }
    }

    unsigned modifiers = 0;
    if (keyEvent->shiftKey())
        modifiers |= ShiftKey;
    if (keyEvent->altKey())
        modifiers |= AltKey;
    if (keyEvent->controlKey())
        modifiers |= CtrlKey;
    if (keyEvent->metaKey())
        modifiers |= MetaKey;

    if (keyEvent->type() == PlatformEvent::Type::RawKeyDown) {
        int mapKey = modifiers << 16 | evt->keyCode();
        return mapKey ? keyDownCommandsMap->get(mapKey) : 0;
    }

    int mapKey = modifiers << 16 | evt->charCode();
    return mapKey ? keyPressCommandsMap->get(mapKey) : 0;
}

bool EditorClientJava::handleEditingKeyboardEvent(KeyboardEvent* evt)
{
    const PlatformKeyboardEvent* keyEvent = evt->underlyingPlatformEvent();
    if (!keyEvent)
        return false;

    Frame* frame = downcast<Node>(evt->target())->document().frame();
    auto* localFrame = dynamicDowncast<LocalFrame>(frame);
    if (!frame || !localFrame)
        return false;

    String commandName = String::fromLatin1(interpretKeyEvent(evt));
    Editor::Command command = localFrame->editor().command(commandName);

    if (keyEvent->type() == PlatformEvent::Type::RawKeyDown) {
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
    if (evt->underlyingPlatformEvent()->text().length() == 1) {
        UChar ch = evt->underlyingPlatformEvent()->text()[0U];

        // Don't insert null or control characters as they can result in
        // unexpected behaviour
        if (ch < ' ')
            return false;
#if !OS(WINDOWS)
        // Don't insert ASCII character if ctrl w/o alt or meta is on.
        // On Mac, we should ignore events when meta is on (Command-<x>).
        if (ch < 0x80) {
            if (evt->underlyingPlatformEvent()->controlKey() && !evt->underlyingPlatformEvent()->altKey())
                return false;
#if OS(DARWIN)
            if (evt->underlyingPlatformEvent()->metaKey())
                return false;
#endif
        }
#endif
    }

    if (!localFrame->editor().canEdit())
        return false;

    return localFrame->editor().insertText(evt->underlyingPlatformEvent()->text(), evt);
}

void EditorClientJava::handleKeyboardEvent(KeyboardEvent& evt)
{
    if (handleEditingKeyboardEvent(&evt)) {
        evt.setDefaultHandled();
    }
}

bool EditorClientJava::shouldDeleteRange(const std::optional<SimpleRange>&)
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

bool EditorClientJava::isSelectTrailingWhitespaceEnabled() const
{
    notImplemented();
    return false;
}

int EditorClientJava::spellCheckerDocumentTag()
{
    notImplemented();
    return 0;
}

bool EditorClientJava::shouldBeginEditing(const SimpleRange&)
{
    notImplemented();
    return true;
}

bool EditorClientJava::shouldEndEditing(const SimpleRange&)
{
    notImplemented();
    return true;
}

bool EditorClientJava::shouldInsertText(const String&, const std::optional<SimpleRange>&, EditorInsertAction)
{
    notImplemented();
    return true;
}

bool EditorClientJava::shouldChangeSelectedRange(const std::optional<SimpleRange>&, const std::optional<SimpleRange>&, Affinity, bool)
{
    return true;
}

bool EditorClientJava::shouldApplyStyle(const StyleProperties&, const std::optional<SimpleRange>&)
{
    return true;
}

void EditorClientJava::didApplyStyle()
{
}

void EditorClientJava::didBeginEditing()
{
    notImplemented();
}

void EditorClientJava::respondToChangedContents()
{
    notImplemented();
}

void EditorClientJava::respondToChangedSelection(LocalFrame *frame)
{
    if (!frame || !frame->editor().hasComposition()
        || frame->editor().ignoreSelectionChanges()) {
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
        setInputMethodState(nullptr);
    }
}

void EditorClientJava::updateEditorStateAfterLayoutIfEditabilityChanged() {
    notImplemented();
}

void EditorClientJava::didEndEditing()
{
    notImplemented();
}

void EditorClientJava::didWriteSelectionToPasteboard()
{
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
        Ref<WebCore::UndoStep> step = WTFMove(*(--m_undoStack.end()));
        m_undoStack.remove(--m_undoStack.end());
        // unapply will call us back to push this command onto the redo stack.
        step->unapply();
    }
}

void EditorClientJava::redo()
{
    if (canRedo()) {
        Ref<WebCore::UndoStep> step = WTFMove(*(--m_redoStack.end()));
        m_redoStack.remove(--m_redoStack.end());

        ASSERT(!m_isInRedo);
        m_isInRedo = true;
        // reapply will call us back to push this command onto the undo stack.
        step->reapply();
        m_isInRedo = false;
    }
}

bool EditorClientJava::shouldInsertNode(Node&, const std::optional<SimpleRange>&, EditorInsertAction)
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

void EditorClientJava::textFieldDidBeginEditing(Element&)
{
    notImplemented();
}

void EditorClientJava::textFieldDidEndEditing(Element&)
{
    notImplemented();
}

void EditorClientJava::textDidChangeInTextField(Element&)
{
    notImplemented();
}

bool EditorClientJava::doTextFieldCommandFromEvent(Element&, KeyboardEvent*)
{
    notImplemented();
    return false;
}

void EditorClientJava::textWillBeDeletedInTextField(Element&)
{
    notImplemented();
}

void EditorClientJava::textDidChangeInTextArea(Element&)
{
    notImplemented();
}

void EditorClientJava::overflowScrollPositionChanged() {
    notImplemented();
}

void EditorClientJava::subFrameScrollPositionChanged() {
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


bool EditorClientJava::shouldMoveRangeAfterDelete(const SimpleRange&, const SimpleRange&)
{
    notImplemented();
    return true;
}

void EditorClientJava::setInputMethodState(Element* element)
{
    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID midSetInputMethodState = env->GetMethodID(
        PG_GetWebPageClass(env),
        "setInputMethodState",
        "(Z)V");
    ASSERT(midSetInputMethodState);

    env->CallVoidMethod(
        m_webPage,
        midSetInputMethodState,
        bool_to_jbool(element && element->shouldUseInputMethod()));
    WTF::CheckAndClearException(env);
}

void EditorClientJava::handleInputMethodKeydown(KeyboardEvent&)
{
    notImplemented();
}

void EditorClientJava::willSetInputMethodState()
{
    notImplemented();
}

bool EditorClientJava::canCopyCut(LocalFrame*, bool defaultValue) const
{
    return defaultValue;
}

bool EditorClientJava::canPaste(LocalFrame*, bool defaultValue) const
{
    return defaultValue;
}

void EditorClientJava::discardedComposition(const Document&)
{
}

DOMPasteAccessResponse EditorClientJava::requestDOMPasteAccess(DOMPasteAccessCategory, FrameIdentifier, const String& originIdentifier)
{
    return DOMPasteAccessResponse::DeniedForGesture;
}

void EditorClientJava::canceledComposition()
{
}

const int gc_maximumm_undoStackDepth = 1000;
void EditorClientJava::registerUndoStep(UndoStep& step)
{
    if (m_undoStack.size() == gc_maximumm_undoStackDepth)
        m_undoStack.removeFirst();
    if (!m_isInRedo)
        m_redoStack.clear();
    m_undoStack.append(step);
}

void EditorClientJava::registerRedoStep(UndoStep& step)
{
    m_redoStack.append(step);
}

void EditorClientJava::clearUndoRedoOperations()
{
    m_undoStack.clear();
    m_redoStack.clear();
}

void EditorClientJava::getClientPasteboardData(const std::optional<SimpleRange>&, Vector<std::pair<String, RefPtr<WebCore::SharedBuffer>>>& pasteboardTypesAndData)
{
    notImplemented();
}

void EditorClientJava::willWriteSelectionToPasteboard(const std::optional<SimpleRange>&)
{
}

// All of the member functions from TextCheckerClient is umimplemented
bool EditorClientJava::shouldEraseMarkersAfterChangeSelection(TextCheckingType) const
{
    notImplemented();
    return true;
}

void EditorClientJava::ignoreWordInSpellDocument(const String&)
{
    notImplemented();
}

void EditorClientJava::learnWord(const String&)
{
    notImplemented();
}

void EditorClientJava::checkSpellingOfString(StringView, int*, int*)
{
    notImplemented();
}

/*String EditorClientJava::getAutoCorrectSuggestionForMisspelledWord(const String&)
{
    notImplemented();
    return String();
}*/

void EditorClientJava::checkGrammarOfString(StringView, Vector<GrammarDetail>&, int*, int*)
{
    notImplemented();
}

#if USE(UNIFIED_TEXT_CHECKING)
Vector<TextCheckingResult> EditorClientJava::checkTextOfParagraph(StringView, TextCheckingTypeMask, const VisibleSelection&)
{
    notImplemented();
    return Vector<TextCheckingResult>();
}
#endif

// For spellcheckers that support multiple languages, it's often important to be able to identify the language in order to
// provide more accurate correction suggestions. Caller can pass in more text in "context" to aid such spellcheckers on language
// identification. Noramlly it's the text surrounding the "word" for which we are getting correction suggestions.
void EditorClientJava::getGuessesForWord(const String&, const String&, const VisibleSelection&, Vector<String>&)
{
    notImplemented();
}

void EditorClientJava::requestCheckingOfString(TextCheckingRequest&, const VisibleSelection&)
{
    notImplemented();
}

} // namespace WebCore
