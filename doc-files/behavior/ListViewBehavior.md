# ListView Behavior

## Key Bindings

|Key Combination          |Platform |Condition         |Function                                |
|-------------------------|---------|------------------|----------------------------------------|
|shortcut-A               |         |not in combo box  |select all
|shortcut-BACKSLASH       |         |                  |clear selection
|DOWN                     |         |vertical          |select next row
|alt-shortcut-DOWN        |         |                  |vertical unit scroll down
|shift-DOWN               |         |vertical          |extend selection to the next row
|shift-shortcut-DOWN      |         |vertical          |discontinuous select next row **function unclear**
|shortcut-DOWN            |         |vertical          |focus next row
|END                      |         |not editing       |select last row 
|shift-END                |         |not in edit field |select to last row **function unclear**
|shift-shortcut-END       |         |vertical, not in combo box |discontinuous select all to last row **win,linux? on mac, END is shortcut-RIGHT** **function unclear**
|shortcut-END             |         |not in combo box  |focus last row **win,linux? on mac, END is shortcut-RIGHT** **function unclear**
|ENTER                    |         |                  |activate editing
|ESCAPE                   |         |                  |cancel editing
|F2                       |         |                  |activate editing
|HOME                     |         |not editing       |select first row
|shift-HOME               |         |not in edit field |select to first row **function unclear**
|shift-shortcut-HOME      |         |vertical, not in combo box |discontinuous select all to first row **win,linux? on mac, HOME is shortcut-LEFT** **function unclear**
|shortcut-HOME            |         |not in combo box  |focus first row **win,linux? on mac, HOME is shortcut-LEFT**
|KP_DOWN                  |         |vertical          |select next row
|shift-KP_DOWN            |         |vertical          |extend selection to the next row
|KP_LEFT                  |         |horizontal        |select previous row
|shift-KP_LEFT            |         |horizontal        |extend selection to the previous row
|KP_RIGHT                 |         |horizontal        |select next row
|shift-KP_RIGHT           |         |horizontal        |extend selection to the next row
|KP_UP                    |         |vertical          |select previous row
|shift-KP_UP              |         |vertical          |extend selection to the previous row
|LEFT                     |         |horizontal        |select previous row
|alt-shortcut-LEFT        |         |                  |horizontal unit scroll left
|shift-LEFT               |         |horizontal        |extend selection to the previous row
|shift-shortcut-LEFT      |         |horizontal        |discontinuous select previous row **function unclear**
|shortcut-LEFT            |         |horizontal        |move focus to the previous row
|PAGE_DOWN                |         |                  |scroll page down
|shift-PAGE_DOWN          |         |                  |select to last row
|shift-shortcut-PAGE_DOWN |         |vertical          |discontinuous select page down **function unclear**
|shortcut-PAGE_DOWN       |         |not in edit field |focus page down
|PAGE_UP                  |         |                  |scroll page up
|shift-PAGE_UP            |         |                  |select to first row
|shift-shortcut-PAGE_UP   |         |vertical          |discontinuous select page up **function unclear**
|shortcut-PAGE_UP         |         |not in edit field |focus page up
|RIGHT                    |         |horizontal        |select next row
|alt-shortcut-RIGHT       |         |                  |horizontal unit scroll right
|shift-RIGHT              |         |horizontal        |extend selection to the next row
|shift-shortcut-RIGHT     |         |horizontal        |discontinuous select next row **function unclear**
|shortcut-RIGHT           |         |horizontal        |move focus to the next row
|SPACE                    |         |                  |activate editing
|ctrl-SPACE               |non-mac  |                  |toggle focus of selected row, set anchor to same
|ctrl-shortcut-SPACE      |mac      |                  |toggle focus of selected row, set anchor to same
|shift-SPACE              |         |                  |select all to focus ??
|shift-shortcut-SPACE     |         |                  |select all to focus, set anchor **function unclear**
|UP                       |         |vertical          |select previous row
|alt-shortcut-UP          |         |                  |vertical unit scroll up
|shift-UP                 |         |vertical          |extend selection to the previous row
|shift-shortcut-UP        |         |vertical          |discontinuous select previous row **function unclear**
|shortcut-UP              |         |vertical          |focus previous row



### Notes

1. On macOS, `alt` is represented by the `option` key
2. On macOS, `shortcut` is represented by the `command` key
3. On macOS, Home = `command` left arrow key
4. On macOS, End = `command` right arrow key
5. On macOS, PgUp = `fn` + `up arrow` key
6. On macOS, PgDn = `fn` + `down arrow` key
7. On macOS, BACKSPACE = `delete` key
8. On macOS, DELETE = `fn` + `delete` key
