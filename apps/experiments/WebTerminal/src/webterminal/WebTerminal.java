/*
 * Copyright (c) 2011, 2014 Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package webterminal;

import javafx.scene.web.*;
import org.w3c.dom.*;
import org.w3c.dom.Node;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import org.w3c.dom.events.EventTarget;
import netscape.javascript.JSObject;
import javafx.application.Platform;
import javafx.scene.control.Control;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import java.util.List;
import java.util.ArrayList;

/** Implements a "rich-term" console/terminal based on a WebView.
 *
 * A "line" is a sequence of text or inline elements - usually span elements.
 * A div or paragraph of N lines has (N-1) br elements between lines.
 * (br elements are only allowed in div or p elements.)
 */
public abstract class WebTerminal extends VBox // FIXME should extend Control
      implements javafx.event.EventHandler, org.w3c.dom.events.EventListener
                      /*implements KeyListener, ChangeListener*/ 
{
    WebView webView;
    protected WebView getWebView() { return webView; } 

    protected WebEngine webEngine;

    String defaultBackgroundColor = "white";
    String defaultForegroundColor = "black";

    public boolean isLineEditing() { return lineEditing; }
    public void setLineEditing(boolean lineEditing) {
        this.lineEditing = lineEditing;
    }
    private boolean lineEditing = true;

    public boolean outputLFasCRLF() { return isLineEditing(); }

    /** The current input line.
     * Note there is always a current (active) input line, even if the
     * inferior isn't ready for it, and hasn't emitted a prompt.
     * This is to support type-ahead, as well as application code
     * reading from standard input.  (FUTURE - untested.)
     */
    public Element getInputLine() { return inputLine; }
    public void setInputLine(Element inputLine) { this.inputLine = inputLine; }
    Element inputLine;

    Document documentNode;
    Element bodyNode;

    public Document getDocumentNode() { return documentNode; }

    /** The element (normally a div or pre) which cursor navigation is relative to.
     * Cursor motion is relative to the start of this element.
     * "Erase screen" only erases in this element.
     * @return the cursor home location is the start of this element
     */
    public Element getCursorHome() { return cursorHome; }
    public void setCursorHome(Element cursorHome) { this.cursorHome = cursorHome; }
    /** The element (normally div) which cursor navigation is relative to. */
    private Element cursorHome;

    /** Current line number, 0-origin, relative to start of cursorHome.
     * -1 if unknown. */
    int currentCursorLine = -1;
    /** Current column number, 0-origin, relative to start of cursorHome.
     * -1 if unknown. */
    int currentCursorColumn = -1;

    /** This is the column width at which the next line implicitly starts.
     * Compare with wrapWidth - if both are less than Integer.MAX_VALUE
     * then they should normally be equal. */
    int columnWidth = Integer.MAX_VALUE;

    /** If inserting a character at this column width, insert a wrap-break. */
    int wrapWidth = 80;
    boolean wrapOnLongLines = true;

    /** Determine if we should break when a long line overflows.
     * If inserting a character would takes us beyond the wrap-width
     * (normally the line-width), insert a newline first.
     * This is needed for VT100-style behavior.
     * (If would be preferable if the layout-algorithm could be modified
     * to break in the middle of a word, but that is difficult.  At least
     * we want selection/copy to not include the inserted newline.  Better
     * would be for windows re-size to re-calculate the breaks.)
     * Initial value is true.
     */
    public void setWrapOnLongLines(boolean value) {
        wrapOnLongLines = value;
    }

    /** Default string to use to mark an automatic break for over-long lines.
     * It is 'arrow pointing downwards then curving leftwards' followed by
     * 'zero width space'.  This visually looks OK, plus is easy to
     * recognize as it wouldn't appear in real text.
     */
    public static final String DEFAULT_WRAP_STRING ="\u2936\u200B";
    /** String to use to mark an automatic break for over-long lines.
     * TODO: On windows resize remove wrapString, and re-wrap.
     * TODO: On copy/selection, remove wrapString.
     */
    String wrapString = DEFAULT_WRAP_STRING;
    String wrapStringNewline = DEFAULT_WRAP_STRING + '\n';

    public void resetCursorCache() {
        currentCursorColumn = -1;
        currentCursorLine = -1;
    }

    void updateCursorCache() {
        long lcol = delta2D(cursorHome, outputBefore);
        currentCursorLine = (int) (lcol >> 32);
        currentCursorColumn = (int) (lcol >> 1) & 0x7fffffff;
    }

    /** Get line of current cursor position, starting with 0 at the top. */
    public int getCursorLine() {
        if (currentCursorLine < 0)
            updateCursorCache();
        return currentCursorLine;
    }

    /** Get column of current cursor position, starting with 0 at the left. */
    public int getCursorColumn() {
        if (currentCursorColumn < 0)
            updateCursorCache();
        return currentCursorColumn;
    }

    /** First (top) line of scroll region, 0-origin. */
    int scrollRegionTop = 0;
    /** Last (bottom) line of scroll region, 1-origin.
     * Equivalently, first line following scroll region, 0-origin.
     * The value -1 is equivalent to numRows. */
    int scrollRegionBottom = -1;

    public int getScrollTop() {
        return scrollRegionTop;
    }
    public int getScrollBottom() {
        return scrollRegionBottom < 0 ? getNumRows() : scrollRegionBottom;
    }

    // 0-origin
    int savedCursorLine, savedCursorColumn;

    public void saveCursor() {
        savedCursorLine = getCursorLine();
        savedCursorColumn = getCursorColumn();
    }
 
    public void restoreCursor() {
        moveTo(savedCursorLine, savedCursorColumn);
    }
 
    /** The output position (cursor).
     * Insert output before this node.
     * Usually equal to inputLine except for temporary updates.
     * @return the current output position. If null, this means append
     * output to the end of the output container's children.
     * (FIXME: The null case is not fully debugged.)
     */
    public Node getOutputPosition() { return outputBefore; }
    org.w3c.dom.Node outputBefore;
    public void setOutputPosition(Node node) {
        outputBefore = node; }

    /** The parent node of the output position.
     * @return the output container - new output is by default inserted into
     * this Node, at the position indicated by {@code getOutputPostion()}.
     */
    public Node getOutputContainer() { return outputContainer; }
    Node outputContainer;

    /* * Index of outputContainer in which to insert output.
     * Normally this is the same as outputNode's text length,
     * but in the future we may support cursor movement commands.
     */
    //int outputPosition = -1;

    int inputLineNumber = 0;

    protected void loadSucceeded() {
        addInputLine();
        outputBefore = inputLine;
    }

    public WebTerminal() {
        webView = new WebView();
        webEngine = webView.getEngine();
        webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
                public void changed(ObservableValue<? extends State> ov, State t, State newValue) {
                    if (newValue == State.SUCCEEDED) {
                        initialize();
                        loadSucceeded();
                    }
                }});

        loadPage(webEngine);
        this.getChildren().add(webView);

        // We run the key-event handlers during the filter (capture) phase,
        // rather than the normal (bubbling) phase.  This allows us to
        // consume the event, so it never gets to the bubbling phase - and
        // thus never gets passed to the native component.  (In JavaScript
        // one can call preventDefault or have the handler return false,
        // but we don't have the functionality with JavaFX events.)
        webView.addEventFilter(KeyEvent.KEY_PRESSED, this);
        webView.addEventFilter(KeyEvent.KEY_TYPED, this);

        VBox.setVgrow(webView, Priority.ALWAYS);
    }

    public static final boolean USE_XHTML = true;
    public static final String XHTML_NAMESPACE = "http://www.w3.org/1999/xhtml";
    public static final String htmlNamespace = USE_XHTML ? XHTML_NAMESPACE : "";

    /** URL for the initial page to be loaded. */
    protected String pageUrl() {
        String rname = USE_XHTML ? "repl.xml" : "repl.html";
        java.net.URL rurl = WebTerminal.class.getResource(rname);
        if (rurl == null)
            throw new RuntimeException("no initial web page "+rname);
        return rurl.toString();
    }

    /** Load the start page.  Do not call directly.
     * Can be overridden to load a start page from a String:
     * webEngibe.loadContent("initialContent", "MIME/type");
     */
    protected void loadPage(WebEngine webEngine) {
        webEngine.load(pageUrl());
    }

    protected void initialize() {
        documentNode = webEngine.getDocument();
        bodyNode = documentNode.getElementById("body");

        //((EventTarget) bodyNode).addEventListener("click", this, false);

        //if (isLineEditing())             ((JSObject) bodyNode).call("focus");
        // Element initial = documentNode.getElementById("initial"); FIXME LEAKS
        Element initial = (Element) bodyNode.getFirstChild();
        cursorHome = initial;
        outputContainer = initial;
    }

    /** For debugging.*/
    public String getHTMLText() {
        if (true) {
            return NodeWriter.writeNodeToString(bodyNode);
        } /*else if (true) {
        java.io.StringWriter sw = new java.io.StringWriter();
        gnu.xml.XMLPrinter xp = new gnu.xml.XMLPrinter(sw, false);
        ConsumeDOM.consumeNode(bodyNode, xp, true);
        xp.flush();
        return sw.toString();
        } */ else if (false) {
            return ((JSObject) (Object) bodyNode).getMember("innerHTML").toString();
        } else {
        com.sun.webkit.WebPage webPage = com.sun.javafx.webkit.Accessor.getPageFor(webEngine);
        return "HTML="+webPage+"["+webPage.getHtml(webPage.getMainFrame())+"]";
        //return  webEngine.getPage().toString(); //executeScript("document.innerHTML").toString();
        }
    }

    /*
    public void setHtmlText(String htmlText) {
        ((HTMLEditorSkin)getSkin()).setHTMLText(htmlText);
    }
    */

    protected void setEditable(Element element, boolean editable) {
        ((JSObject) element).setMember("contentEditable", editable);
    }
    
    protected void setEditable(boolean editable) {
        setEditable(getInputLine(), editable);
    }
    
    /** Add an input span as the last child of outputContainer.
     */
    public void addInputLine () {
        Element inputNode = createSpanNode();
        String id = "input"+(++inputLineNumber);

        inputNode.setAttribute("id", id);
        inputNode.setAttribute("std", "input");
        setEditable(inputNode, true);
        outputContainer.appendChild(inputNode);

        // KLUDGE: The insertion caret isn't visible until something has inserted
        // into the input line.  So we insert U-200B "zero width space".
        // This gets removed in enter.
        // (Note if a space is inserted and removed from the UI then the
        // caret remains visible.  Thus a cleaner work-around would be if
        // we could simulate this.  I haven't gotten that to work so far.)
        Text dummyText = documentNode.createTextNode("\u200B");
        //Text dummyText = documentNode.createTextNode("\n");
        inputNode.appendChild(dummyText);
        inputLine = inputNode;
        setFocus();
    }

    public String inputAsString() { return toString(inputLine.getChildNodes());}

    protected abstract void enter(KeyEvent ke);

    public String grabInput(Element input) {
        //WTDebug.println("grabInput input:"+WTDebug.pnode(input)+" firstCh:"+WTDebug.pnode(input.getFirstChild())+" inputL:"+WTDebug.pnode(inputLine));
        String text = ((Text)input.getChildNodes().item(0)).getData();
        //if (text == null) text = "";

        // Replace Non-breaking space (inserted by webkit) with regular space.
        text = text.replace((char) 160, ' ');
        int tlen = text.length();
        // See comment in addInputLine.
        if (tlen > 0 && text.charAt(tlen-1) == (char) 0x200B) // OLD? 
            text = text.substring(0, --tlen);
        if (tlen > 0 && text.charAt(0) == (char) 0x200B) // NEW?
        { tlen--; text = text.substring(1);}
        return text;
    }

    protected String handleEnter (KeyEvent ke) {
        String text = grabInput(inputLine);
        nextInputLine();
        //super.processKeyEvent(ke);
        return text;
    }

    protected void nextInputLine() {
        Node outputSave = outputBefore;
        Node containerSave = outputContainer;
        boolean normal = outputSave == inputLine;
        outputBefore = inputLine.getNextSibling();
        outputContainer = inputLine.getParentNode();
        setEditable(false);
        insertBreak();
        addInputLine();
        outputBefore = normal ? inputLine : outputSave;
        outputContainer = containerSave;
    }

    public void insertOutput (final String str, final char kind) {
        Platform.runLater(new Runnable() {
                public void run() {
                    insertString(str, kind);
                }
            });
    }

    // States of escape sequences handler state machine.
    static final int INITIAL_STATE = 0;
    static final int SEEN_ESC_STATE = 1;
    /** We have seen ESC '['. */
    static final int SEEN_ESC_LBRACKET_STATE = 2;
    /** We have seen ESC '[' '?'. */
    static final int SEEN_ESC_LBRACKET_QUESTION_STATE = 3;
    /** We have seen ESC ']'. */
    static final int SEEN_ESC_RBRACKET_STATE = 4;
    /** We have seen ESC ']' numeric-parameter ';'. */
    static final int SEEN_ESC_BRACKET_TEXT_STATE = 5;
    int controlSequenceState = INITIAL_STATE;

    int prevParametersCount = 0; // number of semicolon argument separators seen.
    // The actual number of arguments seen is:
    // prevParametersCount+(curNumParameter>=0?1:0)
    // Of these prevParametersCount are in the first prevParametersCount
    // elements of prevNumParameters.
    int curNumParameter = -1;
    int[] prevNumParameters = null;
    StringBuilder curTextParameter;

    boolean insertMode;
    public boolean inInsertMode() { return insertMode; }
    public void setInsertMode(boolean enable) { insertMode = enable; }

    int numRows = 24, numCols = 80;
    public int getNumColumns() { return numCols; }
    public int getNumRows() { return numRows; }
    public void setRowsColumns(int rows, int columns) {
        numCols = columns;
        numRows = rows;
        wrapWidth = columns;
    }

    public void handleBell() {
        // Nothing.
    }

    public void handleOperatingSystemControl(int code, String text) {
        if (code == 72) {
            ((com.sun.webkit.dom.HTMLElementImpl) inputLine).insertAdjacentHTML("beforeBegin", text);
        }
        else {
            // WTDebug.println("Saw Operating System Control #"+code+" \""+WTDebug.toQuoted(text)+"\"");
        }
    }

    boolean usingAlternateScreenBuffer;
    private Element savedCursorHome;
    public void setAlternateScreenBuffer(boolean val) {
        if (usingAlternateScreenBuffer != val) {
            if (val) {
                // FIXME should scroll top of new buffer to top of window.
                Element buffer = createElement("pre");
                savedCursorHome = cursorHome;
                bodyNode.appendChild(buffer);
                cursorHome = buffer;
                outputContainer.removeChild(inputLine);
                buffer.appendChild(inputLine);
                outputContainer = buffer;
                outputBefore = inputLine;
                currentCursorColumn = 0;
                currentCursorLine = 0;
                setFocus();
            } else { 
                outputContainer.removeChild(inputLine);
                cursorHome.getParentNode().removeChild(cursorHome);
                cursorHome = savedCursorHome;
                cursorHome.appendChild(inputLine);
                outputContainer = cursorHome;
                outputBefore = inputLine;
                savedCursorHome = null;
                outputContainer = cursorHome;
                resetCursorCache();
                setFocus();
            }
            usingAlternateScreenBuffer = val;
            scrollRegionTop = 0;
            scrollRegionBottom = -1;
        }
    }

    public void handleControlSequence(char last) {
        switch (last) {
        case '@':
            boolean saveInsertMode = inInsertMode();
            setInsertMode(true);
            if (curNumParameter<0) curNumParameter = 1;
            insertSimpleOutput(makeSpaces(curNumParameter), 0, curNumParameter,
                               'O', getCursorColumn()+curNumParameter);
            cursorLeft(curNumParameter);
            setInsertMode(saveInsertMode);
            break;
        case 'd': // Line Position Absolute
            if (curNumParameter<0) curNumParameter = 1;
            moveTo(curNumParameter-1, getCursorColumn());
            break;
        case 'h':
            if (controlSequenceState == SEEN_ESC_LBRACKET_QUESTION_STATE) {
                // DEC Private Mode Set (DECSET)
                switch (curNumParameter) {
                case 47:
                case 1047:
                    setAlternateScreenBuffer(true); break;
                case 1048: saveCursor(); break;
                case 1049: saveCursor(); setAlternateScreenBuffer(true); break;
                }
            }
            else {
                switch (curNumParameter) {
                case 4: setInsertMode(true); break;
                }
            }
            break;
        case 'l':
            if (controlSequenceState == SEEN_ESC_LBRACKET_QUESTION_STATE) {
                // DEC Private Mode Reset (DECRST)
                switch (curNumParameter) {
                case 47:
                case 1047:
                    // should clear first?
                    setAlternateScreenBuffer(false); break;
                case 1048: restoreCursor(); break;
                case 1049: setAlternateScreenBuffer(false); restoreCursor(); break;
              }
            }
            else {
                switch (curNumParameter) {
                case 4: setInsertMode(false); break;
                }
            }
            break;
        case 'm':
            for (int i = 0;  i <= prevParametersCount;  i++) {
                int val = i < prevParametersCount ? prevNumParameters[i] : curNumParameter;
                if (val <= 0) {
                    currentStyles.clear();
                }
                else {
                    int nstyles = currentStyles.size();
                    switch (val) {
                    case 1:
                        pushSimpleStyle("font-weight:", "bold");
                        break;
                    case 22:
                        pushSimpleStyle("font-weight:", null/*"normal"*/);
                        break;
                    case 4:
                        pushSimpleStyle("text-decoration:", "underline");
                        break;
                    case 24:
                        pushSimpleStyle("text-decoration:", null/*"none"*/);
                        break;
                    case 7:
                        pushSimpleStyle("color:", defaultBackgroundColor);
                        pushSimpleStyle("background-color:", defaultForegroundColor);
                        break;
                    case 27:
                        pushSimpleStyle("color:", null/*defaultForegroundColor*/);
                        pushSimpleStyle("background-color:", null/*defaultBackgroundColor*/);
                        break;
                    case 30: pushSimpleStyle("color:", "black"); break;
                    case 31: pushSimpleStyle("color:", "red"); break;
                    case 32: pushSimpleStyle("color:", "green"); break;
                    case 33: pushSimpleStyle("color:", "yellow"); break;
                    case 34: pushSimpleStyle("color:", "blue"); break;
                    case 35: pushSimpleStyle("color:", "magenta"); break;
                    case 36: pushSimpleStyle("color:", "cyan"); break;
                    case 37: pushSimpleStyle("color:", "white"); break;
                    case 39: pushSimpleStyle("color:", null/*defaultForegroundColor*/); break;
                    case 40: pushSimpleStyle("background-color:", "black"); break;
                    case 41: pushSimpleStyle("background-color:", "red"); break;
                    case 42: pushSimpleStyle("background-color:", "green"); break;
                    case 43: pushSimpleStyle("background-color:", "yellow"); break;
                    case 44: pushSimpleStyle("background-color:", "blue"); break;
                    case 45: pushSimpleStyle("background-color:", "magenta"); break;
                    case 46: pushSimpleStyle("background-color:", "cyan"); break;
                    case 47: pushSimpleStyle("background-color:", "white"); break;
                    case 49: pushSimpleStyle("background-color:", null/*defaultBackgroundColor*/); break;
                    }
                }
            }
            adjustStyleNeeded = true;
            break;
        case 'r':
            int top = prevParametersCount >= 1 ? prevNumParameters[0] : curNumParameter>=0 ? curNumParameter : 1;
            int bottom = prevParametersCount >= 2 ? prevNumParameters[1]
                : prevParametersCount==1 && curNumParameter>=0 ? curNumParameter : -1;
            scrollRegionTop = top - 1;
            scrollRegionBottom = bottom;
            break;
        case 'A': // cursor up
            if (curNumParameter<0) curNumParameter = 1;
            cursorDown(- curNumParameter);
            break;
        case 'B': // cursor down
            if (curNumParameter<0) curNumParameter = 1;
            cursorDown(curNumParameter);
            break;
        case 'C':
            if (curNumParameter<0) curNumParameter = 1;
            cursorRight(curNumParameter);
            break;
        case 'D':
            if (curNumParameter<0) curNumParameter = 1;
            cursorLeft(curNumParameter);
            break;
        case 'H':
            int row = prevParametersCount >= 1 ? prevNumParameters[0] : curNumParameter>=0 ? curNumParameter : 1;
            int col = prevParametersCount >= 2 ? prevNumParameters[1]
                : prevParametersCount==1 && curNumParameter>=0 ? curNumParameter : 1;
            moveTo(row-1, col-1);
            break;
        case 'J':
            if (curNumParameter<0) curNumParameter = 0;
            if (curNumParameter == 0) // Erase below.
                eraseUntil(cursorHome);
            else {
                int saveLine = getCursorLine();
                int saveCol = getCursorColumn();
                if (curNumParameter == 1) { // Erase above
                    for (int line = 0;  line < saveLine;  line++) {
                        moveTo(line, 0);
                        eraseLineRight();
                    }
                    if (saveCol != 0) {
                        moveTo(saveLine, 0);
                        eraseCharactersRight(saveCol, false);
                    }
                } else { // Erase all
                    moveTo(0, 0);
                    eraseUntil(cursorHome);
                }
                moveTo(saveLine, saveCol);
            }
            break;
        case 'K':
            if (curNumParameter!=1)
                eraseLineRight();
            if (curNumParameter>=1)
                eraseLineLeft();
            break;
        case 'L': // Insert lines
            if (curNumParameter<0) curNumParameter = 1;
            insertLines(curNumParameter);
            break;
        case 'M': // Delete lines
            if (curNumParameter<0) curNumParameter = 1;
            deleteLines(curNumParameter);
            break;
        case 'P': // Delete characters
            if (curNumParameter<0) curNumParameter = 1;
            eraseCharactersRight(curNumParameter, true);
            break;
        case 'S':
            if (curNumParameter<0) curNumParameter = 1;
            scrollForward(curNumParameter);
            break;
        case 'T':
            if (curNumParameter<0) curNumParameter = 1;
            else if (curNumParameter >= 5)
                ; // FIXME Initiate mouse tracking.
            scrollReverse(curNumParameter);
            break;
        }
    }

    /** A stack of currently active "style" strings. */
    public List<String> currentStyles = new ArrayList<String>();

    /** True if currentStyles may not match the current style context.
     * Thus the context needs to be adjusted before text is inserted. */
    boolean adjustStyleNeeded;

    /** Add a style property specifier to the currentStyles list.
     * However, if the new specifier "cancels" an existing specifier,
     * just remove the old one.
     * @param styleNameWithColon style property name including colon,
     *     (for example "text-decoration:").
     * @param styleValue style property value string (for example "underline"),
     *     or null to indicate the default value.
     */
    protected void pushSimpleStyle(String styleNameWithColon, String styleValue) {
        int nstyles = currentStyles.size();
        for (int i = 0;  i < nstyles;  ) {
            String style = currentStyles.get(i);
            if (style.startsWith(styleNameWithColon)) {
                currentStyles.remove(i);
                nstyles--;
            } else
                i++;
        }
        if (styleValue != null)
            currentStyles.add(styleNameWithColon+' '+styleValue);
    }

    /** Adjust style at current position to match desired style.
     * The desired style is a specified by the currentStyles list.
     * This usually means adding {@code <span style=...>} nodes around the
     * current position.  If the current position is already inside
     * a {@code <span style=...>} node that doesn't match the desired style,
     * then we have to split the {@code span} node so the current
     * position is not inside the span node, but text before and after is.
     */
    protected void adjustStyle() {
        adjustStyleNeeded = false;

        List<String> parentStyles = new ArrayList<String>();
        for (Node n = outputContainer;  n != bodyNode && n != null;
             n = n.getParentNode()) {
            String style;
            if (n instanceof Element) {
                style = ((Element) n).getAttribute("style");
                if (style != null && style.length() > 0)
                    parentStyles.add(style);
            }
        }

        // Compare the parentStyles and currentStyles lists,
        // so we can "keep" the styles where the match, and pop or add
        // the styles where they don't match.
        int keptStyles = 0;
        int currentStylesLength = currentStyles.size();
        int j;
        for (j = parentStyles.size(); --j >= 0; ) {
            String parentStyle = parentStyles.get(j);
            if (parentStyle != null) {
                if (keptStyles == currentStylesLength) {
                    break;
                }

                // Matching is made more complicate because parentStyles
                // may specify multiple properties in a single style attribute.
                // For example "color: red; background-color: blue".
                int k = 0;
                while (k >= 0 && (parentStyle = parentStyle.trim()).length() > 0) {
                    // Assume property values cannot contain semi-colons.
                    // This may fail if there are string-valued properties,
                    // since we don't check for quoted semi-colons.
                    int semi = parentStyle.indexOf(';');
                    String s;
                    if (semi >= 0) {
                        s = parentStyle.substring(0, semi).trim();
                        parentStyle = parentStyle.substring(semi+1);
                        if (s.length() == 0)
                            continue;
                    }
                    else {
                        s = parentStyle;
                        parentStyle = "";
                    }
                    if (keptStyles+k < currentStylesLength
                        && s.equals(currentStyles.get(keptStyles+k)))
                        k++;
                    else
                        k = -1;
                }
                if (k >= 0)
                    keptStyles += k;
                else
                    break;                   
            }
        }
        int popCount = j+1;
        while (--popCount >= 0) {
            popStyle();
        }
        if (keptStyles < currentStylesLength) {
            outputBefore = inputLine.getNextSibling();
            outputContainer.removeChild(inputLine);
            String styleValue = null;
            do {
                String s = currentStyles.get(keptStyles);
                styleValue = styleValue == null ? s : styleValue + ';' + s;
            } while (++keptStyles < currentStylesLength);
            Element spanNode = createSpanNode();
            spanNode.setAttribute("style", styleValue);
            outputContainer.insertBefore(spanNode, outputBefore);
            outputContainer = spanNode;
            outputBefore = null;
            spanNode.appendChild(inputLine);
            outputBefore = inputLine;
            setFocus();
        }
    }

    public void setFocus() {
        ((JSObject) inputLine).call("focus");
    }

    /** Move inputLine outside current (style) span. */
    protected void popStyle() {
        Node following = inputLine.getNextSibling();
        Element span1 = (Element) inputLine.getParentNode();
        Node parent = span1.getParentNode();
        span1.removeChild(inputLine);
        outputContainer = parent;
        parent.insertBefore(inputLine, span1.getNextSibling());
        outputBefore = inputLine;
        if (following != null) {
            Element span2 = createSpanNode();
            String classAttr = span1.getAttribute("class");
            String styleAttr = span1.getAttribute("style");
            if (classAttr != null && classAttr.length() > 0)
                span2.setAttribute("class", classAttr);
            if (styleAttr != null && styleAttr.length() > 0)
                span2.setAttribute("style", styleAttr);
            parent.insertBefore(span2, inputLine.getNextSibling());
            do {
                Node ch = following;
                following = ch.getNextSibling();
                span1.removeChild(ch);
                span2.appendChild(ch);
            } while (following != null);
        }
        setFocus();
    }

    public void insertLinesIgnoreScroll(int count) {
        StringBuilder builder = new StringBuilder(count);
        while (--count >= 0)
            builder.append('\n');
        Text text = documentNode.createTextNode(builder.toString());
        if (outputBefore == inputLine && inputLine != null)
            outputContainer.insertBefore(text, outputBefore.getNextSibling());
        else {
            insertNode(text);
            outputBefore = text;
        }
    }

    public void deleteLinesIgnoreScroll(int count) {
        for (int i = count; --i >= 0; ) {
            eraseCharactersRight(-1, true);
            Node current = outputBefore;
            if (current==inputLine)
                current=current.getNextSibling();
            if (current instanceof Text) {
                Text tnode = (Text) current;
                String text = tnode.getTextContent();
                int length = text.length();
                if (length == 0 || text.charAt(0) != '\n') // Invalid usage
                    break;
                tnode.deleteData(0, 1);
            }
            else if (isBreakNode(current)) {
                current.getParentNode().removeChild(current);
                current=current.getNextSibling();
            }
            else {
                // Invalid usage.
                break;
            }
        }
    }

    public void insertLines(int count) {
        int line = getCursorLine();
        moveTo(getScrollBottom()-count, 0);
        deleteLinesIgnoreScroll(count);
        moveTo(line, 0);
        insertLinesIgnoreScroll(count);
    }

    public void deleteLines(int count) {
        deleteLinesIgnoreScroll(count);
        int line = getCursorLine();
        cursorLineStart(getScrollBottom() - line - count);
        insertLinesIgnoreScroll(count);
        moveTo(line, 0);
    }

    public void scrollForward(int count) {
        int line = getCursorLine();
        moveTo(getScrollTop(), 0);
        deleteLinesIgnoreScroll(count);
        int scrollRegionSize = getScrollBottom() - getScrollTop();
        cursorLineStart(scrollRegionSize-count);
        insertLinesIgnoreScroll(count);
        moveTo(line, 0);
    }

    public void scrollReverse(int count) {
        int line = getCursorLine();
        moveTo(getScrollBottom()-count, 0);
        deleteLinesIgnoreScroll(count);
        moveTo(getScrollTop(), 0);
        insertLinesIgnoreScroll(count);
        moveTo(line, 0);
    }

    public void eraseLineLeft() {
        int column = getCursorColumn();
        cursorLineStart(0);
        eraseCharactersRight(column, false);
        cursorRight(column);
    }

    /** Erase from the current position until stopNode.
     * If currently inside stopNode, erase to end of stopNode;
     * otherwise erase until start of stopNode.
     */
    void eraseUntil(Node stopNode) {
        Node current = outputBefore;
        Node parent = outputContainer;
        if (current==inputLine && current != null)
            current=current.getNextSibling();
        for (;;) {
            if (current == stopNode)
                return;
            if (current == null) {
                current = parent;
                parent = current.getParentNode();
            } else {
                Node next = current.getNextSibling();
                parent.removeChild(current);
                current = next;
            }
        }
    }

    /** Erase or delete characters in current line.
     * @param count number of characters to erase or -1 to end of line
     * @param doDelete true if delete, false if erase
     */
    public void eraseCharactersRight(int count, boolean doDelete) {
        if (count < 0)
            count = Integer.MAX_VALUE;
        // Note that the traversal logic is similar to move.
        Node current = outputBefore;
        Node parent = outputContainer;
        if (current==inputLine && current != null)
            current=current.getNextSibling();
        int curColumn = -1;
        int seen = 0; // Number of columns scanned so far.
        for (;;) {
            if (isBreakNode(current) || seen >= count) {
                break;
            }
            else if (current instanceof Text) {
                Text tnode = (Text) current;
                String text = tnode.getTextContent();
                int length = text.length();

                int i = 0;
                for (; i < length; i++) {
                    if (seen >= count)
                        break;
                    char ch = text.charAt(i);
                    // Optimization - don't need to calculate getCurrentColumn.
                    if (ch >= ' ' && ch < 127) {
                        seen++;
                    }
                    else if (ch == '\r' || ch == '\n' || ch == '\f') {
                        count = seen;
                        break;
                    }
                    else {
                        if (curColumn < 0)
                            curColumn = getCursorColumn();
                        int col = updateColumn(ch, curColumn+seen);
                        seen = col - curColumn;
                        // general case using updateColumn FIXME
                    }
                }

                if (i >= length && doDelete) {
                    Node next = current.getNextSibling();
                    parent.removeChild(current);
                    current = next;
                    //break;
                }
                else {
                    if (doDelete)
                        tnode.deleteData(0, i);
                    else {
                        tnode.replaceData(0, i, makeSpaces(i));
                    }
                }
                continue;
            } else if (current instanceof Element) {
                if (isObjectElement((Element) current)) {
                    Node next = current.getNextSibling();
                    parent.removeChild(current);
                    current = next;
                    count--;
                    continue;
                }
            }

            Node ch;
            if (current != null) {
                // If there is a child, go to the first child next.
                ch = current.getFirstChild();
                if (ch != null) {
                    parent = current;
                    current = ch;
                    continue;
                }
                // Otherwise, go to the next sibling.
                ch = current.getNextSibling();
                if (ch != null) {
                    current = ch;
                    continue;
                }

                // Otherwise go to the parent's sibling - but this gets complicated.
                if (isBlockNode(current))
                    break;
            }

            //ch = current;
            for (;;) {
                if (parent == bodyNode) {
                    return;
                }
                Node sib = parent.getNextSibling();
                Node pparent = parent.getParentNode();
                if (isSpanNode(parent) && parent.getFirstChild() == null)
                    pparent.removeChild(parent);
                parent = pparent;
                if (sib != null) {
                    current = sib;
                    break;
                }
            }
        }
    }

    public void eraseLineRight() {
        eraseCharactersRight(-1, true);
    }

    /** Move cursor to beginning of line, relative.
     * @param deltaLines line number to move to, relative to current line.
     */
    public void cursorLineStart(int deltaLines) {
        if (deltaLines > 0) // Optimization
            moveTo(outputBefore, deltaLines, 0, true);
        else
            moveTo(getCursorLine()+deltaLines, 0);
    }

    public void cursorDown(int deltaLines) {
        moveTo(getCursorLine()+deltaLines, getCursorColumn());
    }

    public void cursorRight(int count) {
        if (false) {
            moveTo(inputLine, 0, count, true);
        } else {
            // FIXME optimize same way cursorLeft is.
            //long lcol = delta2D(cursorHome, outputBefore);
            //inline = (int) (lcol >> 32);
            //int col = (int) (lcol >> 1) & 0x7fffffff;
            moveTo(getCursorLine(), getCursorColumn()+count);
        }
    }

    public void cursorLeft(int count) {
        if (count == 0)
            return;
        org.w3c.dom.Node prev = outputBefore.getPreviousSibling();
        // Optimize common case
        if (prev instanceof org.w3c.dom.Text) {
            org.w3c.dom.Text ptext = (org.w3c.dom.Text) prev;
            int len = ptext.getLength();
            String tstr = ptext.getTextContent();
            int tcols = 0;
            int tcount = 0;
            for (;;) {
                if (tcols == count)
                    break;
                if (tcount == len) {
                    tcount = -1;
                    break;
                }
                tcount++;
                char ch = tstr.charAt(len-tcount);
                int chcols = charColumns(ch);
                if (ch == '\n' || ch == '\r' || ch == '\f' || ch == 't'
                    || chcols < 0 || tcols+chcols > count) {
                    tcount = -1;
                    break;
                }
                tcols += chcols;
            }
            if (tcount > 0) {
                String after = tstr.substring(len-tcount);
                ptext.deleteData(len-tcount, tcount);
                count -= tcols;
                org.w3c.dom.Node following = outputBefore.getNextSibling();
                if (following instanceof org.w3c.dom.Text) {
                    org.w3c.dom.Text rtext = (org.w3c.dom.Text) following;
                    rtext.replaceData(0, 0, after);
                } else {
                    org.w3c.dom.Text nafter = documentNode.createTextNode(after);
                    outputContainer.insertBefore(nafter, following);
                }
                if (currentCursorColumn > 0)
                    currentCursorColumn -= tcols;
            }
        }
        if (count > 0) {
            moveTo(getCursorLine(), getCursorColumn()-count);
        }
    }

    /** Return column number following a tab at initial {@code col}.
     * @param col initial column, 0-origin
     * @return column number (0-origin) after a tab
     * Default implementation assumes tabs every 8 columns.
     */
    protected int nextTabCol(int col) {
        return (col & ~7) + 8;
    }

    public Text createText(String data) {
        return documentNode.createTextNode(data);
    }
    
    public Element createElement(String tag) {
        return USE_XHTML ? documentNode.createElementNS(htmlNamespace, tag)
            : documentNode.createElement(tag);
    }

    protected Element createSpanNode() {
        return createElement("span");
    }

    protected void insertString(String str, char kind) {
        //WTDebug.println("insertString \""+WTDebug.toQuoted(str)+"\" len:"+str.length()+" in-is-out:"+(inputLine==outputBefore)+" before:"+WTDebug.pnode(outputBefore)+" DOM["+getHTMLText()+"]");
        int prevEnd = 0;
        int curColumn = getCursorColumn();
        int slen = str.length();
        int i = 0;
        for (; i < slen;  i++) {
            char ch = str.charAt(i);
            switch (controlSequenceState) {
            case SEEN_ESC_STATE:
                switch (ch) {
                case '[':
                    controlSequenceState = SEEN_ESC_LBRACKET_STATE;
                    curNumParameter = -1;
                    prevParametersCount = 0;
                    continue;
                case ']':
                    controlSequenceState = SEEN_ESC_RBRACKET_STATE;
                    curNumParameter = -1;
                    prevParametersCount = 0;
                    continue;
                case '7': // DECSC
                    saveCursor();
                    break;
                case '8': // DECRC
                    restoreCursor();
                    break;
                case 'M': // Reverse index
                    insertLines(1);
                    break;
                }
                controlSequenceState = INITIAL_STATE;
                prevEnd = i + 1;
                curColumn = getCursorColumn();
                break;
            case SEEN_ESC_LBRACKET_STATE:
            case SEEN_ESC_LBRACKET_QUESTION_STATE:
                if (ch >= '0' && ch <= '9') {
                    curNumParameter = curNumParameter >= 0 ? 10 * curNumParameter : 0;
                    curNumParameter += (ch - '0');
                }
                else if (ch == ';') {
                    if (prevNumParameters == null)
                        prevNumParameters = new int[4];
                    else if (prevNumParameters.length <= prevParametersCount) {
                        prevNumParameters = java.util.Arrays.copyOf(prevNumParameters, 2*prevParametersCount);
                    }
                    prevNumParameters[prevParametersCount] = curNumParameter;
                    curNumParameter = -1;
                    prevParametersCount++;
                }
                else if (ch == '?')
                    controlSequenceState = SEEN_ESC_LBRACKET_QUESTION_STATE;
                else {
                    handleControlSequence(ch);
                    prevNumParameters = null;
                    prevEnd = i + 1;
                    curColumn = getCursorColumn();
                    controlSequenceState = INITIAL_STATE;
                }
                continue;

            case SEEN_ESC_RBRACKET_STATE:
                if (ch >= '0' && ch <= '9') {
                    curNumParameter = curNumParameter >= 0 ? 10 * curNumParameter : 0;
                    curNumParameter += (ch - '0');
                }
                else if (ch == ';') {
                    controlSequenceState = SEEN_ESC_BRACKET_TEXT_STATE;
                    curTextParameter = new StringBuilder();
                }
                else {
                    prevNumParameters = null;
                    prevEnd = i + 1;
                    curColumn = getCursorColumn();
                    controlSequenceState = INITIAL_STATE;
                }
                continue;

            case SEEN_ESC_BRACKET_TEXT_STATE:
                if (ch == '\007' || ch == '\000') {
                    handleOperatingSystemControl(curNumParameter, curTextParameter.toString());
                    curTextParameter = null;
                    curColumn = getCursorColumn();
                    controlSequenceState = INITIAL_STATE;
                    prevNumParameters = null;
                    prevEnd = i + 1;
                } else {
                    curTextParameter.append(ch);
                }
                continue;
            case INITIAL_STATE:
                switch (ch) {
                case '\r':
                    insertSimpleOutput(str, prevEnd, i, kind, curColumn);
                    //currentCursorColumn = column;
                    if (i+1 < slen && str.charAt(i+1) == '\n'
                        && getCursorLine() != scrollRegionBottom-1) {
                        cursorLineStart(1);
                        i++;
                    } else {
                        cursorLineStart(0);
                    }
                    prevEnd = i + 1;
                    curColumn = 0;
                    break;
                case '\n':
                    insertSimpleOutput(str, prevEnd, i, kind, curColumn);
                    if (outputLFasCRLF()) {
                        if (inInsertMode()) {
                            insertRawOutput("\n");
                            if (currentCursorLine >= 0)
                                currentCursorLine++;
                            currentCursorColumn = 0;
                        } else {
                            cursorLineStart(1);
                        }
                    }
                    // Only scroll if scrollRegionBottom explicitly set to a value >= 0.
                    else if (getCursorLine() == scrollRegionBottom-1)
                        scrollForward(1);
                    else
                        cursorDown(1);
                    prevEnd = i + 1;
                    curColumn = currentCursorColumn;
                    break;
                case '\b':
                    insertSimpleOutput(str, prevEnd, i, kind, curColumn); 
                    cursorLeft(1);
                    //WTDebug.println("BACKSPACE after DOM["+getHTMLText()+"]");
                    prevEnd = i + 1; 
                    curColumn = currentCursorColumn;
                    break;
                case '\007': // Bell
                    insertSimpleOutput(str, prevEnd, i, kind, curColumn); 
                    //currentCursorColumn = column;
                    handleBell();
                    prevEnd = i + 1;
                    break;
                case '\033':
                    insertSimpleOutput(str, prevEnd, i, kind, curColumn);
                    //currentCursorColumn = column;
                    prevEnd = i + 1;
                    controlSequenceState = SEEN_ESC_STATE;
                    continue;
                case '\t':
                    insertSimpleOutput(str, prevEnd, i, kind, curColumn);
                    int nextStop = nextTabCol(getCursorColumn());
                    //WTDebug.println("TAB cur:"+currentCursorColumn+" tab-to:"+nextStop+" move:"+(nextStop-currentCursorColumn));
                    cursorRight(nextStop-currentCursorColumn);
                    curColumn = currentCursorColumn;
                    prevEnd = i + 1;
                    break;
                default:
                    int nextColumn = updateColumn(ch, curColumn);
                    if (nextColumn > wrapWidth) {
                        if (wrapOnLongLines) {
                            insertSimpleOutput(str, prevEnd, i, kind, curColumn);
                            insertWrapBreak();
                            prevEnd = i;
                        }
                        //line++;
                        nextColumn = updateColumn(ch, 0);
                    }
                    curColumn = nextColumn;
                }
            }
        }
        if (controlSequenceState == INITIAL_STATE) {
            insertSimpleOutput(str, prevEnd, i, kind, curColumn);
            //currentCursorColumn = column;
        }
        //long lcol = delta2D(cursorHome, outputBefore);
        //WTDebug.println("after insertString \""+WTDebug.toQuoted(str)+"\" len:"+str.length()+" in-is-out:"+(inputLine==outputBefore)+" DOM["+getHTMLText()+"]");
    }

    protected void insertSimpleOutput (String str, int beginIndex, int endIndex, char kind, int endColumn) {
        int sslen = endIndex - beginIndex;
        if (sslen == 0)
            return;

        if (adjustStyleNeeded)
            adjustStyle();
        int slen = str.length();
        if (beginIndex > 0 || endIndex != slen)
            str = str.substring(beginIndex, endIndex);
        //WTDebug.println("[insertSimple \""+WTDebug.toQuoted(str)+"\" kind:"+kind+" DOM["+getHTMLText()+"] outBef:"+WTDebug.pnode(outputBefore)+" container:"+WTDebug.pnode(outputContainer)+" endCol:"+endColumn+" cur:"+getCursorColumn()+" ins:"+inInsertMode());
        int column = getCursorColumn();
        int widthInColums = endColumn-column;
        if (! inInsertMode()) {
            eraseCharactersRight(widthInColums, true);
        } else if (wrapOnLongLines) {
            moveTo(cursorHome, getCursorLine(), wrapWidth-widthInColums, false);
            eraseCharactersRight(-1, true);
            moveTo(getCursorLine(), column);
        }
        if (kind == 'E') {
            Element errElement = createSpanNode();
            errElement.setAttribute("std", "error");
            //errElement.setAttribute("style", "font-weight: bold; color: green; background: blue");
            //resetCursorCache(); // FIXME - should avoid
            insertNode(errElement);
            errElement.appendChild(documentNode.createTextNode(str));
            outputBefore = errElement.getNextSibling();
        }
        else {
            insertRawOutput(str);
        }
        currentCursorColumn = endColumn;
    }

    void insertRawOutput(String str) {
        org.w3c.dom.Node previous
            = outputBefore != null ? outputBefore.getPreviousSibling()
            : outputContainer.getLastChild();
        if (previous instanceof Text)
            ((Text) previous).appendData(str);
        else {
            Text text = documentNode.createTextNode(str);
            insertNode(text);
        }
    }

    /** Insert a node at (before) current position.
     * Caller needs to update cursor cache or call resetCursorCache. */
    public void insertNode (org.w3c.dom.Node node) {
        outputContainer.insertBefore(node, outputBefore);
    }

    /** Insert element at current position, and move to start of element. */
    public void pushIntoElement(Element element) {
        resetCursorCache(); // FIXME - not needed if element is span, say.
        insertNode(element);
        outputContainer = element;
        outputBefore = null;
    }

    /** Move position to follow current container. */
    public void popFromElement() {
        Node element = outputContainer;
        outputContainer = element.getParentNode();
        outputBefore = element.getNextSibling();
    }

    public void insertPrompt (final String str) {
        Platform.runLater(new Runnable() {
                public void run() {
                    //WTDebug.println("insertPrompt "+WTDebug.toQuoted(str)+" DOM["+getHTMLText()+"] curL:"+currentCursorLine);
                    // <span std="prompt">
                    Element promptElement = createSpanNode();
                    promptElement.setAttribute("std", "prompt");
                    insertNode(promptElement);
                    org.w3c.dom.Node savedParent = outputContainer;
                    Node savedBefore = outputBefore;
                    try {
                        // Note this sets outputBefore to null, which is not
                        // fully supported yet (see comment for outputBefore).
                        // It should be safe for prompt contents.
                        // The alternative of temporarily moving the inputLine
                        // inside the proptElement works, but is really ugly.
                        outputContainer = promptElement;
                        outputBefore = null;
                        insertString(str, '\0');
                    } finally {
                        // </span>
                        outputBefore = savedBefore;
                        outputContainer = savedParent;
                    }
                }
            });
    }

    private void appendText(Node parent, String data) {
        if (data.length() == 0)
            return;
        Node last = parent.getLastChild();
        if (last instanceof Text)
            ((Text) last).appendData(data);
        else
            parent.appendChild(documentNode.createTextNode(data));
    }

    /** Insert a {@code <br>} node. */
    protected void insertBreak () {
        org.w3c.dom.Node breakNode = createElement("br");
        insertNode(breakNode);
        currentCursorColumn = 0;
        if (currentCursorLine >= 0)
            currentCursorLine++;
    }

    /** Insert a line break because of wrapping an over-long line. */
    protected void insertWrapBreak() {
        if (false) {
            cursorLineStart(1);
        } else {
            int oldLine = currentCursorLine;
            insertRawOutput(wrapStringNewline);
            if (oldLine >= 0)
                currentCursorLine = oldLine + 1;
            currentCursorColumn = 0;
        }
    }

    public void handleEvent(org.w3c.dom.events.Event event) {
        //WTDebug.println("handle1Event "+event+" type:"+event.getType()+" INPUT:"+inputAsString());
    }

    public void handle(javafx.event.Event ke) {
        if (ke instanceof javafx.event.ActionEvent)
            handle((javafx.event.ActionEvent) ke);
        if (ke instanceof javafx.scene.input.KeyEvent)
            handle((javafx.scene.input.KeyEvent) ke);
    }

    public void handle(javafx.event.ActionEvent ke) {
        //WTDebug.println("handle2 action "+ke+" INPUT:"+inputAsString());
    }
    
    public boolean isApplicationMode() { return true; }

    private void processArrowKey(char ch) {
        processInputCharacters((isApplicationMode() ? "\033O" : "\033[")+ch);
    }

    public void handle(javafx.scene.input.KeyEvent ke) {
        KeyCode code = ke.getCode();
        if (!isLineEditing()) {
            switch (code) {
                //redundant case ENTER: processInputCharacters("\r");  break;
            case UP: processArrowKey('A');  break;
            case DOWN: processArrowKey('B');  break;
            case RIGHT: processArrowKey('C');  break;
            case LEFT: processArrowKey('D');  break;
            case HOME: processInputCharacters("\033[1~"); break;
            case END: processInputCharacters("\033[4~"); break;
                //case DELETE: processInputCharacters("\033[3~"); break;
            case INSERT: processInputCharacters("\033[2~"); break;
            case PAGE_UP: processInputCharacters("\033[5~"); break;
            case PAGE_DOWN: processInputCharacters("\033[6~"); break;
            default:
                String chars = ke.getCharacter();
                if (chars != KeyEvent.CHAR_UNDEFINED && chars.length() > 0)
                    processInputCharacters(chars);
            }
            ke.consume();
        } else {
            if (ke.getEventType() == KeyEvent.KEY_TYPED
                && "\r".equals(ke.getCharacter())) {
                enter(ke);
                ke.consume();
            } else if (ke.getEventType() == KeyEvent.KEY_PRESSED && code == KeyCode.V && ke.isControlDown()) {
                javafx.scene.input.Clipboard cc = javafx.scene.input.Clipboard.getSystemClipboard();
                System.err.println("SCLIP has-h:"+cc.hasHtml()+" has-s:"+cc.hasString()+" str:"+cc.getString());
                //ke.consume();
            } else {
                //WTDebug.println("handle "+ke+" char:"+WTDebug.toQuoted(ke.getCharacter())+" code:"+ke.getCode());
            }
        }
    }

    public void processInputCharacters(String text) {
    }

    /*
    public boolean isDivNode(Node node) {
        if (! (node instanceof Element)) return false;
        String tag = ((Element) node).getTagName();
        return "div".equals(tag);
    }
    */

    /** True if an img/object/a element.
     * These are treated as black boxes similar to a single
     * 1-column character. */
    public boolean isObjectElement(Element node) {
        String tag = node.getTagName();
        return "a".equals(tag) || "object".equals(tag) || "img".equals(tag);
    }

    public boolean isBlockNode(Node node) {
        if (! (node instanceof Element)) return false;
        String tag = ((Element) node).getTagName();
        return "p".equals(tag) || "div".equals(tag) || "pre".equals(tag);
    }

    public boolean isBreakNode(Node node) {
        if (! (node instanceof Element)) return false;
        String tag = ((Element) node).getTagName();
        return "br".equals(tag);
    }

    public boolean isSpanNode(Node node) {
        if (! (node instanceof Element)) return false;
        String tag = ((Element) node).getTagName();
        return "span".equals(tag);
    }

    /** Move forwards relative to cursorHome.
     * Add spaces as needed.
     * @param goalLine number of lines (non-negative) to move down from startNode
     * @param goalColumn number of columns to move right from the start of teh goalLine
     */
    public void moveTo(int goalLine, int goalColumn) {
        moveTo(cursorHome, goalLine, goalColumn, true);
    }

    /** Move forwards relative to startNode.
     * @param startNode the origin (zero) location - usually this is {@code cursorHome}
     * @param goalLine number of lines (non-negative) to move down from startNode
     * @param goalColumn number of columns to move right from the start of teh goalLine
     * @param addSpaceAsNeeded if we should add blank linesor spaces if needed to move as requested; otherwise stop at the last existing line, or (just past the) last existing contents of the goalLine
     */
    public void moveTo(Node startNode, int goalLine, int goalColumn, boolean addSpaceAsNeeded) {
        //WTDebug.println("move start:"+WTDebug.pnode(startNode)+" gl:"+goalLine+" gc:"+goalColumn+" inputL:"+WTDebug.pnode(inputLine));
        //WTDebug.println("move DOM["+getHTMLText()+"]");
        adjustStyleNeeded = true;
        int line = 0, column = 0;
        Node current;
        Node parent;

        if (startNode == cursorHome) {
            if (currentCursorLine >= 0 && currentCursorColumn >= 0
                && goalLine >= currentCursorLine
                && (goalLine > currentCursorLine
                    || goalColumn >= currentCursorColumn)) {
                current = outputBefore;
                parent = outputContainer;
                line = currentCursorLine;
                column = currentCursorColumn;
            }
            else {
                parent = cursorHome;
                current = cursorHome.getFirstChild();
            }
        } else {
            current = startNode;
            parent = current == null ? outputContainer : current.getParentNode();
        }
        // Temporarily remove inputLine from tree.
        if (inputLine != null) {
            Node inputParent = inputLine.getParentNode();
            if (inputParent != null) {
                if (outputBefore==inputLine)
                    outputBefore = outputBefore.getNextSibling();
                if (current==inputLine)
                    current = current.getNextSibling();
               inputParent.removeChild(inputLine);
               // Removing input line may leave 2 Text nodes adjacent.
               // These are merged below.
            }
        }

        //if (parent==null||(current!=null&&parent!=current.getParentNode()))
        //throw new Error("BAD PARENT "+WTDebug.pnode(parent)+" OF "+WTDebug.pnode(current));
        mainLoop:
        while (line < goalLine || column < goalColumn) {
            //WTDebug.println("-move cur:"+WTDebug.pnode(current)+(current==null?"":(" .par:"+WTDebug.pnode(current.getParentNode())))+" parent:"+WTDebug.pnode(parent)+" l:"+line+" col:"+column+" DOM["+getHTMLText()+"]");
            if (parent==null||(current!=null&&parent!=current.getParentNode()))
                throw new Error("BAD PARENT "+WTDebug.pnode(parent)+" OF "+WTDebug.pnode(current));
            if (isBreakNode(current)) {
                if (line == goalLine) {
                    if (addSpaceAsNeeded) {
                        Node previous = current.getPreviousSibling();
                        String str = makeSpaces(goalColumn-column);
                        if (previous instanceof Text)
                            ((Text) previous).appendData(str);
                        else
                            parent.insertBefore(createText(str), current);
                        column = goalColumn;
                    }
                    else
                        goalColumn = column;
                    break;
                } else {
                    line++;
                    column = 0;
                }
            }
            else if (current instanceof Text) {
                Text tnode = (Text) current;
                int tstart = 0;
                Node before;
                while ((before = tnode.getPreviousSibling()) instanceof Text) {
                    // merge nodes
                    // (adjacent text nodes may happen after removing inputLine)
                    String beforeData = ((Text) before).getData();
                    tstart += beforeData.length();
                    tnode.insertData(0, beforeData);
                    parent.removeChild(before);
                }
                String text = tnode.getTextContent();
                int tlen = text.length();
                for (int i = tstart; i < tlen;  i++) {
                    if (line >= goalLine && column >= goalColumn) {
                        tnode.splitText(i);
                        break;
                    }
                    char ch = text.charAt(i);
                    int nextColumn = updateColumn(ch, column);
                    if (nextColumn > columnWidth) {
                        line++;
                        column = updateColumn(ch, 0);
                    }
                    else if (nextColumn == -1) {
                        if (line == goalLine) {
                            int nspaces = goalColumn-column;
                            if (addSpaceAsNeeded) {
                                String spaces = makeSpaces(nspaces);
                                tnode.insertData(i, spaces);
                                tlen += nspaces;
                                i += nspaces;
                            }
                            column = goalColumn;
                            i--;
                        } else {
                            line++;
                            column = 0;
                            if (ch == '\r' && i+1<tlen && text.charAt(i+1) == '\n')
                                i++;
                        }
                    }
                    else
                        column = nextColumn;
                }
            }

            if (parent==null||(current!=null&&parent!=current.getParentNode()))
                throw new Error("BAD PARENT "+WTDebug.pnode(parent)+" OF "+WTDebug.pnode(current));
            // If there is a child, go the the first child next.
            Node ch;
            if (current != null) {
                if (current instanceof Element
                    && isObjectElement((Element) current))
                    column += 1;
                else {
                    ch = current.getFirstChild();
                    if (ch != null) {
                        parent = current;
                        current = ch;
                        continue;
                    }
                }
                // Otherwise, go to the next sibling.
                ch = current.getNextSibling();
                if (ch != null) {
                    current = ch;
                    if (parent==null||(current!=null&&parent!=current.getParentNode()))
                        throw new Error("BAD PARENT "+WTDebug.pnode(parent)+" OF "+WTDebug.pnode(current));
                    continue;
                }

                // Otherwise go to the parent's sibling - but this gets complicated.
                if (isBlockNode(current))
                    line++;
            }

            ch = current;
            for (;;) {
                if (parent == cursorHome || parent == bodyNode) {
                    current = null;
                    if (true) { 
                        if (line < goalLine) {
                            StringBuilder sb = new StringBuilder();
                            while (line++ < goalLine)
                                sb.append('\n');
                            appendText(parent, sb.toString());
                        }
                    }
                    else {
                        while (line++ < goalLine) {
                            parent.appendChild(createElement("br"));
                        }
                    }
                    int fill = goalColumn - column;
                    if (fill > 0) {
                        appendText(parent, makeSpaces(fill));
                    }
                    line = goalLine;
                    column = goalColumn;
                    break mainLoop;
                }
                Node sib = parent.getNextSibling();
                ch = parent; // ??
                parent = parent.getParentNode();
                //WTDebug.println("-at end sib:"+WTDebug.pnode(sib)+" pp:"+WTDebug.pnode(parent));
                if (sib != null) {
                    current = sib;
                    //parent = ch;
                    break;
                }
            }
            continue;
        }
        if (parent==null||(current!=null&&parent!=current.getParentNode()))
            throw new Error("BAD PARENT "+WTDebug.pnode(parent)+" OF "+WTDebug.pnode(current));
        if (parent == bodyNode && isBlockNode(current)) {
            parent = current;
            current = parent.getFirstChild();
        }
        if (inputLine != null) {
            parent.insertBefore(inputLine, current);
            setFocus();
        }
        outputContainer = parent;
        outputBefore = inputLine;
        if (startNode == cursorHome) {
            currentCursorLine = line;
            currentCursorColumn = column;
        }
        else
            resetCursorCache(); // ??? can we do better?
    }

    /** Returns number of columns needed for argument.
     * Currently always returns 1 (except for 'zero width space').
     * However, in the future we should handle zero-width characters
     * as well as double-width characters, and composing charcters.
     */
    int charColumns(int ch) {
        if (ch == 0x200B)
            return 0;
        return 1;
    }

    /** Calculate a "column state" after appending a given char.
     * A non-negative column state is a number of columns.
     * The value -1 as a return value indicates a newline character.
     *
     * In the future, a value less than -1 can be used to encode an
     * initial part of a compound character, including a start surrogate.
     * Compound character support is not implemented yet,
     * nor is support for zero-width or double-width characters.
     */
    protected int updateColumn(char ch, int startState) {
        if (ch == '\n' || ch == '\r' || ch == '\f')
            return -1;
        if (startState < 0) {
            // TODO handle surrogates, compound characters, etc.
        }
        if (ch == '\t')
            return nextTabCol(startState);
        return startState+charColumns(ch);
    }

    /** Calculate (lines, columns) from startNode inclusive to stopNode (exclusive).
     * @param startNode origin - the zero/start location
     * @param stopNode the goal/end location
     * @return {@code lines<<32|columns<<1|(stopSeen?1:0)}
     */
    public long delta2D(Node startNode, Node stopNode) {
        //WTDebug.println("delta2D start:"+WTDebug.pnode(startNode)+" stop:"+WTDebug.pnode(stopNode));
        return delta2D(startNode, 0, stopNode);
    }
    protected long delta2D(Node startNode, long startDelta, Node stopNode) {
        //WTDebug.println("delta2Dr start:"+WTDebug.pnode(startNode)+" stD:"+(startDelta>>32)+"/"+((startDelta>>1)&0x7fffffff)+"?"+(startDelta&1)+" stop:"+WTDebug.pnode(stopNode));
        long delta = startDelta;
        if (startNode == stopNode)
            return delta|1;
        if (startNode instanceof Text) {
            Text tnode = (Text) startNode;
            String text = tnode.getTextContent();
            int tlen = text.length();
            int line = (int) (delta >> 32);
            int col = ((int) delta) >> 1;
            for (int i = 0; i < tlen;  i++) {
                char ch = text.charAt(i);

                col = updateColumn(ch, col);
                if (col > columnWidth) {
                    line++;
                    col = updateColumn(ch, 0);
                }
                else if (col == -1) {
                    line++;
                    col = 0;
                    if (ch == '\r' && i+1<tlen && text.charAt(i+1) == '\n')
                        i++;
                }
            }
            return ((long)line << 32)|((long) col << 1);
        }
        if (isBreakNode(startNode)) {
            return ((delta >> 32) + 1) << 32;
        }
        if (startNode instanceof Element) {
            if (isObjectElement((Element) startNode)) {
                // FIXME
            }
            for (Node n = startNode.getFirstChild(); n != null;
                 n = n.getNextSibling()) {
                delta = delta2D(n, delta, stopNode);
                if ((delta & 1) != 0)
                    return delta;
            }
            if (isBlockNode(startNode))
                delta = ((delta >> 32) + 1) << 32;
        }
        return delta;
    }

    public static String makeSpaces(int count) {
        StringBuilder builder = new StringBuilder(count);
        for (; count >= 8; count -= 8)
            builder.append("        ");
        while (--count >= 0)
            builder.append(' ');
        return builder.toString();
    }

  public static String toString(org.w3c.dom.NodeList ich)
  {
    if (ich.getLength() == 1)
      {
        org.w3c.dom.Node n = (org.w3c.dom.Node) ich.item(0);
        if (n instanceof Text)
          return '\"' + WTDebug.toQuoted(((Text) n).getData()) + '\"';
        else
          return n.toString();
      }
    else
      return ""+ich.getLength()+"items";
  }

}
