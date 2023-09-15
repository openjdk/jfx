# PasswordField Behavior

## Key Binginds

|Key Combination|Platform|Function|
|---------------|--------|--------|
|ctrl-BACK_SLASH|linux, win|deselect|
|BACKSPACE| |delete previous char|
|alt-BACKSPACE|mac|**disabled for security reasons**|
|ctrl-BACKSPACE|linux, win|**disabled for security reasons**|
|shift-BACKSPACE| |delete previous char|
|shortcut-BACKSPACE|mac|delete from line start|
|COPY| |copy|
|CUT| |cut|
|DELETE| |delete next char|
|alt-DELETE|mac|**disabled for security reasons**|
|ctrl-DELETE|linux, win|**disabled for security reasons**|
|ctrl-shift-DIGIT9| |toggle the virtual keyboard (if supported)|
|DOWN| |move to document end|
|shift-DOWN| |select to document end|
|END| |move to document end|
|shift-END|mac|extend selection to document end|
|shift-END|linux, win|select document end|
|shift-shortcut-END| |select to document end|
|shortcut-END| |move to document end|
|ENTER| |fire event|
|ESCAPE| |cancel edit (forwarded to the parent container)|
|HOME| |move to document start|
|shift-HOME|mac|extend selection to document start|
|shift-HOME|linux, win|select to document start|
|shift-shortcut-HOME| |select to document start|
|shortcut-HOME| |move to document start|
|shift-INSERT| |paste|
|shortcut-INSERT| |copy|
|LEFT| |previous character|
|alt-LEFT| |**disabled for security reasons**|
|alt-shift-LEFT|mac|**disabled for security reasons**|
|ctrl-LEFT|linux, win|**disabled for security reasons**|
|ctrl-shift-LEFT|linux, win|**disabled for security reasons**|
|shift-LEFT| |select left|
|shift-shortcut-LEFT|mac|select to document start|
|shortcut-LEFT|mac|move to document start|
|PASTE| |paste|
|RIGHT| |next character|
|alt-RIGHT|mac|**disabled for security reasons**|
|alt-shift-RIGHT|mac|**disabled for security reasons**|
|ctrl-RIGHT|linux, win|**disabled for security reasons**|
|ctrl-shift-RIGHT|linux, win|**disabled for security reasons**|
|shift-RIGHT| |select right|
|shift-shortcut-RIGHT|mac|select to document end|
|shortcut-RIGHT|mac|move to document end|
|TAB| |focus next|
|ctrl-TAB| |focus next|
|ctrl-shift-TAB| |focus previous|
|shift-TAB| |focus previous|
|UP| |move to document start|
|shift-UP| |select to document start|
| | |  |
|shortcut-A| |select all|
|shortcut-C| |copy|
|ctrl-H|linux, win|delete previous char|
|shortcut-V| |paste|
|shortcut-X| |cut|
|ctrl-Y|win|redo|
|ctrl-shift-Z|linux|redo|
|shift-shortcut-Z|mac|redo|
|shortcut-Z| |undo|


Notes:

1. Base class mappings modified by the PasswordField class are highlighted in bold.
2. On Mac, alt is represented by the Option key.
3. On Mac, shortcut is represented by the Command key.