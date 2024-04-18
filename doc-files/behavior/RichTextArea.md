# RichTextArea Behavior

## Key Bindings

|:Key Combination      |:Platform |:Tag                     |:Function                                       |
|----------------------|----------|-------------------------|------------------------------------------------|
|shortcut-A            |          |SELECT_ALL               |select all
|BACKSPACE             |          |BACKSPACE                |delete previous char
|shortcut-C            |          |COPY                     |copy
|COPY                  |          |COPY                     |copy
|CUT                   |          |CUT                      |cut
|shortcut-D            |          |DELETE_PARAGRAPH         |delete current paragraph
|DELETE                |          |DELETE                   |delete next char
|DOWN                  |          |MOVE_DOWN                |line down
|shift-DOWN            |          |SELECT_DOWN              |select line down
|shift-shortcut-DOWN   |mac       |SELECT_TO_DOCUMENT_END   |extend selection to document end
|shortcut-DOWN         |mac       |MOVE_TO_DOCUMENT_END     |move to document end
|END                   |          |MOVE_TO_PARAGRAPH_END    |paragraph end
|ctrl-END              |linux,win |MOVE_TO_DOCUMENT_END     |move to document end
|ctrl-shift-END        |linux,win |SELECT_TO_DOCUMENT_END   |select to document end
|ENTER                 |          |INSERT_LINE_BREAK        |insert line break
|HOME                  |          |MOVE_TO_PARAGRAPH_START  |paragraph start
|ctrl-HOME             |linux,win |MOVE_TO_DOCUMENT_START   |move to document start
|ctrl-shift-HOME       |linux,win |SELECT_TO_DOCUMENT_START |select to document start
|LEFT                  |          |MOVE_LEFT                |previous character
|ctrl-LEFT             |linux,win |MOVE_WORD_LEFT           |left word
|ctrl-shift-LEFT       |linux,win |SELECT_WORD_LEFT         |select word left
|option-LEFT           |mac       |MOVE_WORD_LEFT           |left word
|option-shift-LEFT     |mac       |SELECT_WORD_LEFT         |select word left
|shift-LEFT            |          |SELECT_LEFT              |select left
|PAGE_DOWN             |          |PAGE_DOWN                |page down
|shift-PAGE_DOWN       |          |SELECT_PAGE_DOWN         |select page down
|PAGE_UP               |          |PAGE_UP                  |page up
|shift-PAGE_UP         |          |SELECT_PAGE_UP           |select page up
|PASTE                 |          |PASTE                    |paste
|RIGHT                 |          |MOVE_RIGHT               |next character
|ctrl-shift-RIGHT      |linux,win |SELECT_WORD_RIGHT        |select word right
|option-RIGHT          |mac       |MOVE_WORD_RIGHT          |right word
|option-shift-RIGHT    |mac       |SELECT_WORD_RIGHT        |select word right
|shift-RIGHT           |          |SELECT_RIGHT             |select right
|TAB                   |          |TAB                      |insert tab
|alt-ctrl-shift-TAB    |linux,win |FOCUS_NEXT               |transfer focus to the previous focusable node
|ctrl-TAB              |          |FOCUS_NEXT               |transfer focus to the next focusable node
|ctrl-shift-TAB        |          |FOCUS_PREVIOUS           |transfer focus to the previous focusable node
|ctrl-option-shift-TAB |mac       |FOCUS_NEXT               |transfer focus to the previous focusable node
|shift-TAB             |          |FOCUS_PREVIOUS           |transfer focus to the previous focusable node
|UP                    |          |MOVE_UP                  |line up
|shift-UP              |          |SELECT_UP                |select line up
|shift-shortcut-UP     |          |SELECT_TO_DOCUMENT_START |extend selection to document start
|shortcut-UP           |mac       |MOVE_TO_DOCUMENT_START   |move to document start
|shortcut-V            |          |PASTE                    |paste
|shift-shortcut-V      |          |PASTE_PLAIN_TEXT         |paste plain text
|shortcut-X            |          |CUT                      |cut
|ctrl-Y                |win       |REDO                     |redo
|command-shift-Z       |mac       |REDO                     |redo
|ctrl-shift-Z          |linux     |REDO                     |redo
|shortcut-Z            |          |UNDO                     |undo


### TODO

The following key combinations, present in the TextArea, should be considered for adding to the RichTextArea.

|Key Combination|Platform|Function|
|---------------|--------|--------|
|alt-shift-RIGHT       |mac|select right word|
|ctrl-BACK_SLASH|linux, win|deselect|
|alt-BACKSPACE|mac|delete previous word|
|ctrl-BACKSPACE|linux, win|delete previous word|
|shift-BACKSPACE|          |delete previous char|
|shortcut-BACKSPACE|mac|delete from line start|
|alt-DELETE|mac|delete next word|
|ctrl-DELETE|linux, win|delete next word|
|ctrl-shift-DIGIT9|          |toggle the virtual keyboard (if supported)|
|alt-DOWN|mac|paragraph down|
|alt-shift-DOWN|mac|select paragraph down|
|ctrl-DOWN|linux, win|paragraph down|
|ctrl-shift-DOWN|linux, win|select paragraph down|
|shift-END|          |select to line end|
|shift-HOME|          |select to line start|
|shift-INSERT|          |paste|
|shortcut-INSERT|          |copy|
|alt-LEFT|          |left word|
|alt-shift-LEFT|mac|select left word|
|shift-shortcut-LEFT|mac|select to line start|
|shortcut-LEFT|mac|move to line start|
|alt-RIGHT|mac|right word|
|alt-shift-RIGHT       |mac|select right word|
|ctrl-RIGHT            |linux, win|right word|
|shift-shortcut-RIGHT  |mac|select to line end|
|shortcut-RIGHT|mac|move to line end|
|alt-UP|mac|paragraph up|
|alt-shift-UP|mac|select paragraph up|
|ctrl-UP|linux, win|paragraph up|
|ctrl-shift-UP|linux, win|select paragraph up|
|shift-shortcut-UP|mac|extend selection to document start|
|ctrl-H|linux, win|delete previous char|


Notes:

1. On Mac, alt is represented by the Option key.
2. On Mac, shortcut is represented by the Command key.
