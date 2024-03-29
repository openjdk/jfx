# TreeView Behavior

## Key Bindings

|Key Combination          |Platform |Condition         |Function                                |
|-------------------------|---------|------------------|----------------------------------------|
|shortcut-A               |         |                  |select all
|ADD                      |         |                  |expand row
|DOWN                     |         |                  |select next row
|shift-DOWN               |         |                  |extend selection to the next row
|shift-shortcut-DOWN      |         |                  |discontinuous select next row **function unclear**
|shortcut-DOWN            |         |                  |focus next row
|END                      |         |                  |select last row 
|shift-END                |         |                  |select to last row **function unclear**
|shift-shortcut-END       |         |                  |discontinuous select all to last row **win,linux? on mac, END is shortcut-RIGHT** **function unclear**
|shortcut-END             |         |                  |focus last row **win,linux? on mac, END is shortcut-RIGHT** **function unclear**
|ENTER                    |         |                  |activate editing
|ESCAPE                   |         |                  |cancel editing
|F2                       |         |                  |activate editing
|HOME                     |         |                  |select first row
|shift-HOME               |         |                  |select to first row **function unclear**
|shift-shortcut-HOME      |         |                  |discontinuous select all to first row **win,linux? on mac, HOME is shortcut-LEFT** **function unclear**
|shortcut-HOME            |         |                  |focus first row **win,linux? on mac, HOME is shortcut-LEFT**
|KP_DOWN                  |         |                  |select next row
|shift-KP_DOWN            |         |                  |extend selection to the next row
|KP_LEFT                  |         |                  |LTR: collapse row; RTL: expand row;
|KP_RIGHT                 |         |                  |LTR: expand row; RTL: collapse row;
|KP_UP                    |         |                  |select previous row
|shift-KP_UP              |         |                  |extend selection to the previous row
|LEFT                     |         |                  |LTR: collapse row; RTL: expand row;
|alt-shortcut-LEFT        |         |                  |horizontal scroll left (proposed in JDK-8313138)
|MULTIPLY                 |         |                  |expand all
|PAGE_DOWN                |         |                  |scroll page down
|shift-PAGE_DOWN          |         |                  |select all page down **function unclear**
|shift-shortcut-PAGE_DOWN |         |                  |discontinuous select page down **function unclear**
|shortcut-PAGE_DOWN       |         |                  |focus page down
|PAGE_UP                  |         |                  |scroll page up
|shift-PAGE_UP            |         |                  |select all page up
|shift-shortcut-PAGE_UP   |         |                  |discontinuous select page up **function unclear**
|shortcut-PAGE_UP         |         |                  |focus page up
|RIGHT                    |         |                  |LTR: expand row; RTL: collapse row;
|alt-shortcut-RIGHT       |         |                  |horizontal scroll right (proposed in JDK-8313138)
|SPACE                    |         |                  |toggle focus owner selection
|ctrl-SPACE               |non-mac  |                  |toggle focus owner selection
|ctrl-shortcut-SPACE      |mac      |                  |toggle focus owner selection
|shift-SPACE              |         |                  |select all to focus **function unclear**
|shift-shortcut-SPACE     |         |                  |select all to focus, set anchor **function unclear**
|SUBTRACT                 |         |                  |collapse row
|UP                       |         |                  |select previous row
|shift-UP                 |         |                  |extend selection to the previous row
|shift-shortcut-UP        |         |                  |discontinuous select previous row **function unclear**
|shortcut-UP              |         |                  |focus previous row



### Notes

1. On Mac, alt is represented by the Option key.
2. On Mac, shortcut is represented by the Command key.

### macOS Aliases

- Home: command-LEFT
- End: command-RIGHT
- PgUp: fn-UP
- PgDn: fn-DOWN
