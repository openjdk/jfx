# TableView Behavior

TableView behavior shares most of the key bindings with TreeTableView behavior.


## Key Bindings

|Key Combination          |Platform |Condition         |Function                                |
|-------------------------|---------|------------------|----------------------------------------|
|shortcut-A               |         |                  |select all
|DOWN                     |         |                  |select next row
|alt-shortcut-DOWN        |         |                  |vertical unit scroll down
|shift-DOWN               |         |                  |extend selection to the next row
|shift-shortcut-DOWN      |         |                  |discontinuous select next row **function unclear**
|shortcut-DOWN            |         |                  |focus next row
|END                      |         |                  |select last row 
|shift-END                |         |                  |select to last row **function unclear**
|shift-shortcut-END       |         |                  |discontinuous select all to last row **win,linux? on mac, END is shortcut-RIGHT** **function unclear**
|shortcut-END             |         |                  |focus last row **win,linux? on mac, END is shortcut-RIGHT**
|ENTER                    |         |                  |activate editing
|ESCAPE                   |         |                  |cancel editing
|F2                       |         |                  |activate editing
|HOME                     |         |                  |select first row
|shift-HOME               |         |                  |select to first row **function unclear**
|shift-shortcut-HOME      |         |                  |discontinuous select all to first row **win,linux? on mac, HOME is shortcut-LEFT** **function unclear**
|shortcut-HOME            |         |                  |focus first row **win,linux? on mac, HOME is shortcut-LEFT**
|KP_DOWN                  |         |                  |select next row
|shift-KP_DOWN            |         |                  |extend selection to the next row
|KP_LEFT                  |         |                  |LTR: select left cell; RTL: select right cell;
|shift-KP_LEFT            |         |                  |LTR: extend selection left cell; RTL: extend selection right cell;
|KP_RIGHT                 |         |                  |LTR: select right cell; RTL: select left cell;
|shift-KP_RIGHT           |         |                  |LTR: extend selection right cell; RTL: extend selection left cell;
|shortcut-KP_LEFT         |         |                  |LTR: focus left cell; RTL: focus right cell;
|KP_UP                    |         |                  |select previous row
|shift-KP_UP              |         |                  |extend selection to the previous row
|shortcut-KP_RIGHT        |         |                  |LTR: focus right cell; RTL: focus left cell;
|LEFT                     |         |                  |LTR: select left cell; RTL: select right cell;
|alt-shortcut-LEFT        |         |                  |horizontal unti scroll left
|shift-LEFT               |         |                  |LTR: extend selection left cell; RTL: extend selection right cell;
|shift-shortcut-LEFT      |         |                  |LTR: discontinuous select previuos column; RTL: discontinuous select next column; **win,linux? on mac, HOME is shortcut-LEFT**
|shortcut-LEFT            |         |                  |LTR: focus left cell; RTL: focus right cell;
|PAGE_DOWN                |         |                  |scroll page down
|shift-PAGE_DOWN          |         |                  |extend selection page down
|shift-shortcut-PAGE_DOWN |         |                  |discontinuous select page down **function unclear**
|shortcut-PAGE_DOWN       |         |                  |focus page down
|PAGE_UP                  |         |                  |scroll page up
|shift-PAGE_UP            |         |                  |extend selection page up
|shift-shortcut-PAGE_UP   |         |                  |discontinuous select page up **function unclear**
|shortcut-PAGE_UP         |         |                  |focus page up
|RIGHT                    |         |                  |LTR: select right cell; RTL: select left cell;
|alt-shortcut-RIGHT       |         |                  |horizontal unit scroll right
|shift-shortcut-RIGHT     |         |                  |LTR: discontinuous select next column; RTL: discontinuous select previous column; **win,linux? on mac, END is shortcut-RIGHT**
|shortcut-RIGHT           |         |                  |LTR: focus right cell; RTL: focus left cell;
|shift-RIGHT              |         |                  |LTR: extend selection right cell; RTL: extend selection left cell;
|SPACE                    |         |                  |activate editing
|ctrl-SPACE               |non-mac  |                  |toggle focus owner selection
|ctrl-shortcut-SPACE      |mac      |                  |toggle focus owner selection
|shift-SPACE              |         |                  |extend selection to focus
|shift-shortcut-SPACE     |         |                  |extend selection to focus
|TAB                      |         |                  |traverse focus next
|shift-TAB                |         |                  |traverse focus previous
|UP                       |         |                  |select previous row
|alt-shortcut-UP          |         |                  |vertical unit scroll up
|shift-UP                 |         |                  |extend selection to the previous row
|shift-shortcut-UP        |         |                  |discontinuous select previous row **function unclear**
|shortcut-UP              |         |                  |focus previous row



### Notes

1. On macOS, `alt` is represented by the `option` key
2. On macOS, `shortcut` is represented by the `command` key
3. On macOS, Home = `command` left arrow key
4. On macOS, End = `command` right arrow key
5. On macOS, PgUp = `fn` + `up arrow` key
6. On macOS, PgDn = `fn` + `down arrow` key
7. On macOS, BACKSPACE = `delete` key
8. On macOS, DELETE = `fn` + `delete` key
