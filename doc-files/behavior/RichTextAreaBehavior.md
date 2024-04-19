# RichTextArea Behavior

## Function Tags

|Function Tag              |Description                                                                  |
|:-------------------------|:----------------------------------------------------------------------------|
|BACKSPACE                 |Deletes the symbol before the caret
|COPY                      |Copies selected text to the clipboard
|CUT                       |Cuts selected text and places it to the clipboard
|DELETE                    |Deletes the symbol at the caret
|DELETE_PARAGRAPH          |Deletes paragraph at the caret, or selected paragraphs
|DESELECT                  |Clears any existing selection by moving anchor to the caret position
|FOCUS_NEXT                |Transfer focus to the next focusable node
|FOCUS_PREVIOUS            |Transfer focus to the previous focusable node
|INSERT_LINE_BREAK         |Inserts a line break at the caret
|MOVE_DOWN                 |Moves the caret one visual line down
|MOVE_LEFT                 |Moves the caret one symbol to the left
|MOVE_RIGHT                |Moves the caret one symbol to the right
|MOVE_TO_DOCUMENT_END      |Moves the caret to after the last character of the text
|MOVE_TO_DOCUMENT_START    |Moves the caret to before the first character of the text
|MOVE_TO_PARAGRAPH_END     |Moves the caret to the end of the paragraph at caret
|MOVE_TO_PARAGRAPH_START   |Moves the caret to the beginning of the paragraph at caret
|MOVE_UP                   |Moves the caret one visual text line up
|MOVE_WORD_LEFT            |Moves the caret one word left (previous word if LTR, next word if RTL)
|MOVE_WORD_NEXT            |Moves the caret to the beginning of next word
|MOVE_WORD_NEXT_END        |Moves the caret to the end of the next word
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
|SELECT_PARAGRAPH_END      |Extends selection to the paragraph end
|SELECT_PARAGRAPH_START    |Extends selection to the paragraph start
|SELECT_RIGHT              |Extends selection one symbol to the right
|SELECT_TO_DOCUMENT_END    |Extends selection to the end of the document
|SELECT_TO_DOCUMENT_START  |Extends selection to the start of the document
|SELECT_UP                 |Extends selection one visual text line up
|SELECT_WORD               |Selects a word at the caret position
|SELECT_WORD_LEFT          |Extends selection to the previous word (LTR) or next word (RTL)
|SELECT_WORD_NEXT          |Extends selection to the beginning of next word
|SELECT_WORD_NEXT_END      |Extends selection to the end of next word
|SELECT_WORD_PREVIOUS      |Extends selection to the previous word
|SELECT_WORD_RIGHT         |Extends selection to the next word (LTR) or previous word (RTL)
|TAB                       |Inserts a tab symbol at the caret (editable), or transfer focus to the next focusable node
|UNDO                      |If possible, undoes the last modification



## Key Bindings

|Key Combination       |Platform  |Tag                      |
|:---------------------|:---------|:------------------------|
|shortcut-A            |          |SELECT_ALL
|BACKSPACE             |          |BACKSPACE
|shortcut-C            |          |COPY
|COPY                  |          |COPY
|CUT                   |          |CUT
|shortcut-D            |          |DELETE_PARAGRAPH
|DELETE                |          |DELETE
|DOWN                  |          |MOVE_DOWN
|shift-DOWN            |          |SELECT_DOWN
|shift-shortcut-DOWN   |mac       |SELECT_TO_DOCUMENT_END
|shortcut-DOWN         |mac       |MOVE_TO_DOCUMENT_END
|END                   |          |MOVE_TO_PARAGRAPH_END
|ctrl-END              |linux,win |MOVE_TO_DOCUMENT_END
|ctrl-shift-END        |linux,win |SELECT_TO_DOCUMENT_END
|shift-END             |linux,win |SELECT_PARAGRAPH_END
|ENTER                 |          |INSERT_LINE_BREAK
|ctrl-H                |linux,win |BACKSPACE
|HOME                  |          |MOVE_TO_PARAGRAPH_START
|ctrl-HOME             |linux,win |MOVE_TO_DOCUMENT_START
|ctrl-shift-HOME       |linux,win |SELECT_TO_DOCUMENT_START
|shift-HOME            |linux,win |SELECT_PARAGRAPH_START
|LEFT                  |          |MOVE_LEFT
|ctrl-LEFT             |linux,win |MOVE_WORD_LEFT
|ctrl-shift-LEFT       |linux,win |SELECT_WORD_LEFT
|option-LEFT           |mac       |MOVE_WORD_LEFT
|option-shift-LEFT     |mac       |SELECT_WORD_LEFT
|shift-LEFT            |          |SELECT_LEFT
|shift-shortcut-LEFT   |mac       |SELECT_PARAGRAPH_START
|shortcut-LEFT         |mac       |MOVE_TO_PARAGRAPH_START
|PAGE_DOWN             |          |PAGE_DOWN
|shift-PAGE_DOWN       |          |SELECT_PAGE_DOWN
|PAGE_UP               |          |PAGE_UP
|shift-PAGE_UP         |          |SELECT_PAGE_UP
|PASTE                 |          |PASTE
|RIGHT                 |          |MOVE_RIGHT
|ctrl-RIGHT            |linux,win |MOVE_WORD_RIGHT
|ctrl-shift-RIGHT      |linux,win |SELECT_WORD_RIGHT
|option-RIGHT          |mac       |MOVE_WORD_RIGHT
|option-shift-RIGHT    |mac       |SELECT_WORD_RIGHT
|shift-RIGHT           |          |SELECT_RIGHT
|shift-shortcut-RIGHT  |mac       |SELECT_PARAGRAPH_END
|shortcut-RIGHT        |mac       |MOVE_TO_PARAGRAPH_END
|TAB                   |          |TAB
|alt-ctrl-shift-TAB    |linux,win |FOCUS_NEXT
|ctrl-TAB              |          |FOCUS_NEXT
|ctrl-shift-TAB        |          |FOCUS_PREVIOUS
|ctrl-option-shift-TAB |mac       |FOCUS_NEXT
|shift-TAB             |          |FOCUS_PREVIOUS
|UP                    |          |MOVE_UP
|shift-UP              |          |SELECT_UP
|shift-shortcut-UP     |          |SELECT_TO_DOCUMENT_START
|shortcut-UP           |mac       |MOVE_TO_DOCUMENT_START
|shortcut-V            |          |PASTE
|shift-shortcut-V      |          |PASTE_PLAIN_TEXT
|shortcut-X            |          |CUT
|ctrl-Y                |win       |REDO
|command-shift-Z       |mac       |REDO
|ctrl-shift-Z          |linux     |REDO
|shortcut-Z            |          |UNDO


Notes:

1. Currently unmapped functions: MOVE_WORD_NEXT, MOVE_WORD_NEXT_END, MOVE_WORD_PREVIOUS, SELECT_WORD_NEXT, SELECT_WORD_NEXT_END, SELECT_WORD_PREVIOUS
2. Mapped to mouse events: SELECT_PARAGRAPH, SELECT_WORD


### TODO

The following key combinations, present in the TextArea, should be considered for adding to the RichTextArea.

|Key Combination|Platform|Function|
|---------------|--------|--------|
|alt-BACKSPACE|mac|delete previous word|
|ctrl-BACKSPACE|linux, win|delete previous word|
|shift-BACKSPACE|          |delete previous char **no BACKSPACE on mac keyboard, is this shift-DELETE**?|
|shortcut-BACKSPACE|mac|delete from line start|
|alt-DELETE|mac|delete next word|
|ctrl-DELETE|linux, win|delete next word|
|ctrl-shift-DIGIT9|          |toggle the virtual keyboard (if supported)|
|alt-DOWN|mac|paragraph down **option-DOWN goes to end of the current or next paragraph**|
|alt-shift-DOWN|mac|select paragraph down|
|ctrl-DOWN|linux, win|paragraph down|
|ctrl-shift-DOWN|linux, win|select paragraph down|
|alt-UP|mac|paragraph up|
|alt-shift-UP|mac|select paragraph up|
|ctrl-UP|linux, win|paragraph up|
|ctrl-shift-UP|linux, win|select paragraph up|




Notes:

- On macOS, alt is represented by the Option key
- On macOS, shortcut is represented by the Command key
- On macOS, Home = command left arrow key
- On macOS, End = command right arrow key
- On macOS, PgUp = fn up arrow key
- On macOS, PgDn = fn down arrow key
