# ListView Behavior

## Key Bindings

|Key Combination          |Platform |Condition         |Function                                |
|-------------------------|---------|------------------|----------------------------------------|
|shortcut-A               |         |not in combo box  |select all
|shortcut-BACKSLASH       |         |                  |clear selection
|DOWN                     |         |vertical          |select next row
|shift-DOWN               |         |vertical          |extend selection to the next row
|shift-shortcut-DOWN      |         |vertical          |discuntinuous select next row ??
|shortcut-DOWN            |         |vertical          |move focus to the next row
|END                      |         |not editing       |select last row 
|shift-END                |         |not in edit field |select to last row **function unclear**
|shift-shortcut-END       |         |vertical, not in combo box |discontinuous select all to first row **win,linux? on mac, END is shortcut-RIGHT**
|shortcut-END             |         |not in combo box  |focus last row **win,linux? on mac, END is shortcut-RIGHT**
|ENTER                    |         |                  |activate editing
|ESCAPE                   |         |                  |cancel editing
|F2                       |         |                  |activate editing
|HOME                     |         |not editing       |select first row
|shift-HOME               |         |not in edit field |select to first row **function unclear**
|shift-shortcut-HOME      |         |vertical, not in combo box |discontinuous select all to first row **win,linux? on mac, HOME is shortcut-LEFT**
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
|alt-shortcut-LEFT        |         |                  |horizontal scroll left (proposed in JDK-8313138)
|shift-LEFT               |         |horizontal        |extend selection to the previous row
|shift-shortcut-LEFT      |         |horizontal        |discuntinuous select previous row ??
|shortcut-LEFT            |         |horizontal        |move focus to the previous row
|PAGE_DOWN                |         |                  |scroll page down
|shift-PAGE_DOWN          |         |                  |select to last row
|shift-shortcut-PAGE_DOWN |         |vertical          |discuntinuous select page down ??
|shortcut-PAGE_DOWN       |         |not in edit field |focus page down
|PAGE_UP                  |         |                  |scroll page up
|shift-PAGE_UP            |         |                  |select to first row
|shift-shortcut-PAGE_UP   |         |vertical          |discuntinuous select page up ??
|shortcut-PAGE_UP         |         |not in edit field |focus page up
|RIGHT                    |         |horizontal        |select next row
|alt-shortcut-RIGHT       |         |                  |horizontal scroll right (proposed in JDK-8313138)
|shift-RIGHT              |         |horizontal        |extend selection to the next row
|shift-shortcut-RIGHT     |         |horizontal        |discuntinuous select next row ??
|shortcut-RIGHT           |         |horizontal        |move focus to the next row
|SPACE                    |         |                  |activate editing
|ctrl-SPACE               |non-mac  |                  |toggle focus of selected row, set anchor to same
|ctrl-shortcut-SPACE      |mac      |                  |toggle focus of selected row, set anchor to same
|shift-SPACE              |         |                  |select all to focus ??
|shift-shortcut-SPACE     |         |                  |select all to focus, set anchor ??
|UP                       |         |vertical          |select previous row
|shift-UP                 |         |vertical          |extend selection to the previous row
|shift-shortcut-UP        |         |vertical          |discuntinuous select previous row ??
|shortcut-UP              |         |vertical          |move focus to the previous row



### Notes

1. On Mac, alt is represented by the Option key.
2. On Mac, shortcut is represented by the Command key.

### macOS Aliases

- Home: command-LEFT
- End: command-RIGHT
- PgUp: fn-UP
- PgDn: fn-DOWN
