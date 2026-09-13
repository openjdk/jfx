# RichTextArea Behavior

## Function Tags

|Function Tag              |Description                                                                  |
|:-------------------------|:----------------------------------------------------------------------------|
|BACKSPACE                 |Deletes the symbol before the caret
|COPY                      |Copies selected text to the clipboard
|CUT                       |Cuts selected text and places it to the clipboard
|DELETE                    |Deletes the symbol at the caret
|DELETE_PARAGRAPH          |Deletes paragraph at the caret, or selected paragraphs
|DELETE_PARAGRAPH_START    |Deletes text from the caret to paragraph start, ignoring selection
|DELETE_WORD_NEXT_END      |Deletes empty paragraph or text to the end of the next word
|DELETE_WORD_NEXT_START    |Deletes empty paragraph or text to the start of the next word
|DELETE_WORD_PREVIOUS      |Deletes (multiple) empty paragraphs or text to the beginning of the previous word
|DESELECT                  |Clears any existing selection by moving anchor to the caret position
|ERROR_FEEDBACK            |Provides audio and/or visual error feedback
|FOCUS_NEXT                |Transfer focus to the next focusable node
|FOCUS_PREVIOUS            |Transfer focus to the previous focusable node
|INSERT_LINE_BREAK         |Inserts a line break at the caret
|INSERT_TAB                |Inserts a tab symbol at the caret (if editable), or transfer focus to the next focusable node
|MOVE_DOWN                 |Moves the caret one visual line down
|MOVE_LEFT                 |Moves the caret one symbol to the left
|MOVE_PARAGRAPH_DOWN       |Moves the caret to the end of the current paragraph, or, if already there, to the end of the next paragraph
|MOVE_PARAGRAPH_UP         |Moves the caret to the start of the current paragraph, or, if already there, to the start of the previous paragraph
|MOVE_RIGHT                |Moves the caret one symbol to the right
|MOVE_TO_DOCUMENT_END      |Moves the caret to after the last character of the text
|MOVE_TO_DOCUMENT_START    |Moves the caret to before the first character of the text
|MOVE_TO_LINE_END          |Moves the caret to the end of the visual text line at caret
|MOVE_TO_LINE_START        |Moves the caret to the beginning of the visual text line at caret
|MOVE_TO_PARAGRAPH_END     |Moves the caret to the end of the paragraph at caret
|MOVE_TO_PARAGRAPH_START   |Moves the caret to the beginning of the paragraph at caret
|MOVE_UP                   |Moves the caret one visual text line up
|MOVE_WORD_LEFT            |Moves the caret one word left (previous word if LTR, next word if RTL)
|MOVE_WORD_NEXT_END        |Moves the caret to the end of the next word
|MOVE_WORD_NEXT_START      |Moves the caret to the start of the next word, or next paragraph if at the start of an empty paragraph
|MOVE_WORD_PREVIOUS        |Moves the caret to the beginning of previous word
|MOVE_WORD_RIGHT           |Moves the caret one word right (next word if LTR, previous word if RTL)
|PAGE_DOWN                 |Moves the caret one visual page down
|PAGE_UP                   |Moves the caret one visual page up
|PASTE                     |Pastes the clipboard content
|PASTE_PLAIN_TEXT          |Pastes the plain text clipboard content
|REDO                      |If possible, redoes the last undone modification
|SELECT_ALL                |Selects all text in the document
|SELECT_DOWN               |Extends selection one visual text line down
|SELECT_LEFT               |Extends selection one symbol to the left
|SELECT_PAGE_DOWN          |Extends selection one visible page down
|SELECT_PAGE_UP            |Extends selection one visible page up
|SELECT_PARAGRAPH          |Selects the current paragraph
|SELECT_PARAGRAPH_DOWN     |Extends selection to the end of the current paragraph, or, if already there, to the end of the next paragraph
|SELECT_PARAGRAPH_END      |Extends selection to the paragraph end
|SELECT_PARAGRAPH_START    |Extends selection to the paragraph start
|SELECT_PARAGRAPH_UP       |Extends selection to the start of the current paragraph, or, if already there, to the start of the previous paragraph
|SELECT_RIGHT              |Extends selection one symbol to the right
|SELECT_TO_DOCUMENT_END    |Extends selection to the end of the document
|SELECT_TO_DOCUMENT_START  |Extends selection to the start of the document
|SELECT_TO_LINE_END        |Extends selection to the end of the visual text line at caret
|SELECT_TO_LINE_START      |Extends selection to the start of the visual text line at caret
|SELECT_UP                 |Extends selection one visual text line up
|SELECT_WORD               |Selects a word at the caret position
|SELECT_WORD_LEFT          |Extends selection to the previous word (LTR) or next word (RTL)
|SELECT_WORD_NEXT          |Extends selection to the beginning of next word
|SELECT_WORD_NEXT_END      |Extends selection to the end of next word
|SELECT_WORD_PREVIOUS      |Extends selection to the previous word
|SELECT_WORD_RIGHT         |Extends selection to the next word (LTR) or previous word (RTL)
|UNDO                      |If possible, undoes the last modification



## Key Bindings

|Key Combination       |Platform   |Tag                       |Notes             |
|:---------------------|:----------|:-------------------------|:-----------------|
|shortcut-A            |           |SELECT_ALL                |
|ctrl-BACK_SLASH       |linux, win |DESELECT                  |
|BACKSPACE             |           |BACKSPACE                 |7
|ctrl-BACKSPACE        |linux, win |DELETE_WORD_PREVIOUS      |
|option-BACKSPACE      |mac        |DELETE_WORD_PREVIOUS      |7
|shift-BACKSPACE       |           |BACKSPACE                 |7
|shortcut-BACKSPACE    |mac        |DELETE_PARAGRAPH_START    |7, mac only
|shortcut-C            |           |COPY                      |
|COPY                  |           |COPY                      |
|CUT                   |           |CUT                       |
|shortcut-D            |           |DELETE_PARAGRAPH          |
|DELETE                |           |DELETE                    |8
|option-DELETE         |mac        |DELETE_WORD_NEXT_END      |8, option-fn-delete
|ctrl-DELETE           |linux, win |DELETE_WORD_NEXT_START    |
|DOWN                  |           |MOVE_DOWN                 |
|ctrl-DOWN             |linux, win |MOVE_PARAGRAPH_DOWN       |
|ctrl-shift-DOWN       |linux, win |SELECT_PARAGRAPH_DOWN     |
|option-DOWN           |mac        |MOVE_PARAGRAPH_DOWN       |
|option-shift-DOWN     |mac        |SELECT_PARAGRAPH_DOWN     |
|shift-DOWN            |           |SELECT_DOWN               |
|shift-shortcut-DOWN   |mac        |SELECT_TO_DOCUMENT_END    |
|shortcut-DOWN         |mac        |MOVE_TO_DOCUMENT_END      |
|END                   |           |MOVE_TO_LINE_END          |4
|ctrl-END              |           |MOVE_TO_DOCUMENT_END      |
|ctrl-shift-END        |           |SELECT_TO_DOCUMENT_END    |
|shift-END             |           |SELECT_LINE_END           |
|ENTER                 |           |INSERT_LINE_BREAK         |
|ctrl-H                |linux, win |BACKSPACE                 |
|HOME                  |           |MOVE_TO_LINE_START        |3
|ctrl-HOME             |           |MOVE_TO_DOCUMENT_START    |
|ctrl-shift-HOME       |           |SELECT_TO_DOCUMENT_START  |
|shift-HOME            |           |SELECT_TO_LINE_START      |3
|LEFT                  |           |MOVE_LEFT                 |
|ctrl-LEFT             |linux, win |MOVE_WORD_LEFT            |
|ctrl-shift-LEFT       |linux, win |SELECT_WORD_LEFT          |
|option-LEFT           |mac        |MOVE_WORD_LEFT            |
|option-shift-LEFT     |mac        |SELECT_WORD_LEFT          |
|shift-LEFT            |           |SELECT_LEFT               |
|shift-shortcut-LEFT   |mac        |SELECT_TO_LINE_START      |3
|shortcut-LEFT         |mac        |MOVE_TO_LINE_START        |3
|PAGE_DOWN             |           |PAGE_DOWN                 |6
|shift-PAGE_DOWN       |           |SELECT_PAGE_DOWN          |6
|PAGE_UP               |           |PAGE_UP                   |5
|shift-PAGE_UP         |           |SELECT_PAGE_UP            |5
|PASTE                 |           |PASTE                     |
|RIGHT                 |           |MOVE_RIGHT                |
|ctrl-RIGHT            |linux, win |MOVE_WORD_RIGHT           |
|ctrl-shift-RIGHT      |linux, win |SELECT_WORD_RIGHT         |
|option-RIGHT          |mac        |MOVE_WORD_RIGHT           |
|option-shift-RIGHT    |mac        |SELECT_WORD_RIGHT         |
|shift-RIGHT           |           |SELECT_RIGHT              |
|shift-shortcut-RIGHT  |mac        |SELECT_LINE_END           |
|shortcut-RIGHT        |mac        |MOVE_TO_LINE_END          |
|TAB                   |           |TAB                       |
|alt-ctrl-shift-TAB    |linux, win |FOCUS_NEXT                |
|ctrl-TAB              |           |FOCUS_NEXT                |
|ctrl-shift-TAB        |           |FOCUS_PREVIOUS            |
|ctrl-option-shift-TAB |mac        |FOCUS_NEXT                |
|shift-TAB             |           |FOCUS_PREVIOUS            |
|UP                    |           |MOVE_UP                   |
|ctrl-UP               |linux, win |MOVE_PARAGRAPH_UP         |
|ctrl-shift-UP         |linux, win |SELECT_PARAGRAPH_UP       |
|option-UP             |mac        |MOVE_PARAGRAPH_UP         |
|option-shift-UP       |mac        |SELECT_PARAGRAPH_UP       |
|shift-UP              |           |SELECT_UP                 |
|shift-shortcut-UP     |           |SELECT_TO_DOCUMENT_START  |
|shortcut-UP           |mac        |MOVE_TO_DOCUMENT_START    |
|shortcut-V            |           |PASTE                     |
|shift-shortcut-V      |           |PASTE_PLAIN_TEXT          |
|shortcut-X            |           |CUT                       |
|ctrl-Y                |win        |REDO                      |
|command-shift-Z       |mac        |REDO                      |
|ctrl-shift-Z          |linux      |REDO                      |
|shortcut-Z            |           |UNDO                      |


### Other Mappings

The following functions currently have no mapping:
ERROR_FEEDBACK, MOVE_WORD_NEXT_END, MOVE_WORD_NEXT_START, MOVE_WORD_PREVIOUS, SELECT_WORD_NEXT, SELECT_WORD_NEXT_END, SELECT_WORD_PREVIOUS

The following functions are mapped to the mouse events:
SELECT_PARAGRAPH, SELECT_WORD



### Notes:

1. On macOS, `alt` is represented by the `option` key
2. On macOS, `shortcut` is represented by the `command` key
3. On macOS, Home is represented by the `command` + left arrow keys
4. On macOS, End is represented by the `command` + right arrow keys
5. On macOS, PgUp is represented by the `fn` + `up arrow` keys
6. On macOS, PgDn is represented by the `fn` + `down arrow` keys
7. On macOS, BACKSPACE is represented by the `delete` key
8. On macOS, DELETE is represented by the `fn` + `delete` keys
