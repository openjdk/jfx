/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package myapp2;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import javax.script.Bindings;
import javax.script.ScriptContext;

import static myapp2.Constants.*;
import pseudoScriptEngineCompilable.InvocationInfos;
import pseudoScriptEngineCompilable.RgfPseudoScriptEngineCompilable;

/**
 * Modular test application for testing FXML.
 * This is launched by ModuleLauncherTest.
 */
public class FXMLScriptDeployment2Compile_On extends Application {

    static boolean bDebug = false; // true; // display invocation list

    /** Runs the application and invokes the tests.
     *  @param args the command line arguments, if any given the RgfPseudoScriptEngine invocation logs get displayed
     *              which are used in the asserCorrectInvocations() method
     */
    public static void main(String[] args) {
        try {
            // any argument will cause the bDebug flag to be set to true
            if (args.length > 0) {
               bDebug = true;
            }
            new FXMLScriptDeployment2Compile_On().launch();
            // for debugging, allows to study invocation logs in detail
            if (bDebug) { dumpEvalInformation(); }
            assertCorrectInvocations();
        } catch (AssertionError ex) {
            System.err.println("ASSERTION ERROR: caught unexpected exception: " + ex);
            ex.printStackTrace(System.err);
            System.exit(ERROR_ASSERTION_FAILURE);
        } catch (Error | Exception ex) {
            System.err.println("ERROR: caught unexpected exception: " + ex);
            ex.printStackTrace(System.err);
            System.exit(ERROR_UNEXPECTED_EXCEPTION);
        }
        System.exit(ERROR_NONE);    // not in stop() method as we need to run the assertions first
    }

    @Override
    public void start(Stage mainStage) {
        URL fxmlUrl = null;
        Parent rootNode = null;
        Scene scene = null;
        Button btn = null;
        try {
             fxmlUrl = Util.getURL(FXMLScriptDeployment2Compile_On.class, "demo_02_on");
             rootNode = FXMLLoader.load(fxmlUrl);
             scene = new Scene(rootNode);
             btn = (Button) scene.lookup("#idButton");
        }
        catch (Exception ioe) {
            ioe.printStackTrace();
            System.exit(ERROR_UNEXPECTED_EXCEPTION);
        }
            // fire three events on the button
        btn.fire();
        btn.fireEvent(new ActionEvent());
        btn.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED,
                                     0,       // double x,
                                     0,       // double y,
                                     0,       // double screenX,
                                     0,       // double screenY,
                                     MouseButton.PRIMARY,         // MouseButton button,
                                     0,       // int clickCount,
                                     false,   // boolean shiftDown,
                                     false,   // boolean controlDown,
                                     false,   // boolean altDown,
                                     false,   // boolean metaDown,
                                     true,    // boolean primaryButtonDown,
                                     false,   // boolean middleButtonDown,
                                     false,   // boolean secondaryButtonDown,
                                     false,   // boolean synthesized,
                                     false,   // boolean popupTrigger,
                                     false,   // boolean stillSincePress,
                                     null     // PickResult pickResult
                                     )
                      );

        // mainStage.setScene(scene);
        // mainStage.show();
        Platform.exit();
    }

    // show engine invocations with script text and their Bindings
    static void dumpEvalInformation() {
        System.err.println("\nListing eval() invocation information (invocationList):");

        Iterator<RgfPseudoScriptEngineCompilable> it = RgfPseudoScriptEngineCompilable.getEnginesUsed().iterator();
        while (it.hasNext()) {
            RgfPseudoScriptEngineCompilable rpse = it.next();
            ArrayList invocationList = rpse.getInvocationList();
            System.err.println("ScriptEngine: [" + rpse + "]");

            Iterator<InvocationInfos> itEval = invocationList.iterator();
            int count = 1;
            while (itEval.hasNext()) {
                System.err.println("\teval() invocation # " + count + ": ");
                InvocationInfos entry = itEval.next();
                System.err.println(entry.toDebugFormat("\t\t"));    // indentation
                count++;
                System.err.println();
            }
        }
    }

    static void assertCorrectInvocations() {
            // test only creates one engine for a script controller
        Util.assertTrue("exactly one pseudo script engine instance",
                        RgfPseudoScriptEngineCompilable.getEnginesUsed().size() == 1);
        RgfPseudoScriptEngineCompilable rpse = RgfPseudoScriptEngineCompilable.getEnginesUsed().get(0);

        ArrayList invocationList = rpse.getInvocationList();
        Util.assertTrue("exactly nine script engine invocations", invocationList.size() == 9);

        final String FILENAME = "javax.script.filename";
        final String ARGV = "javax.script.argv";
        final String EVENT = "event";
        final String IDBUTTON = "idButton";
        final String IDROOT = "idRoot";
        final String LOCATION = "location";    // always FXML File hosting script controller code
        final String RESOURCES = "resources";   // always null in this test

        for (int invocation = 1; invocation <= invocationList.size(); invocation++) {
            InvocationInfos entry = (InvocationInfos) invocationList.get(invocation - 1);
            String script = entry.script;
            TreeMap<Integer,TreeMap> scopes = (TreeMap) entry.bindings;

            TreeMap<String,Object> engineBindings = scopes.get(100);
            TreeMap<String,Object> globalBindings = scopes.get(200);

            Object obj = null;
            Button btn = null;

            // global Bindings
            Util.assertExists(IDROOT + " in global scope Bindings", globalBindings.containsKey(IDROOT));
            obj = globalBindings.get(IDROOT);
            Util.assertType(IDROOT, AnchorPane.class, obj);

            Util.assertExists(LOCATION + " in global scope Bindings", globalBindings.containsKey(LOCATION));
            obj = globalBindings.get(LOCATION);
            Util.assertType(LOCATION, URL.class, obj);

            Util.assertExists(RESOURCES + " in global scope Bindings", globalBindings.containsKey(RESOURCES));
            obj = globalBindings.get(RESOURCES);
            Util.assertNull(RESOURCES,obj);

            if (invocation == 1) {
                Util.assertNotExists(IDBUTTON + " in global scope Bindings", globalBindings.containsKey(IDBUTTON));
            }
            else {
                Util.assertExists(IDBUTTON + " in global scope Bindings", globalBindings.containsKey(IDBUTTON));
                obj = globalBindings.get(IDBUTTON);
                Util.assertType(IDBUTTON, Button.class, obj);
                btn = (Button) obj;
            }

            // engine Bindings
            Util.assertExists(FILENAME + " in engine scope Bindings", engineBindings.containsKey(FILENAME));
            if (invocation < 7) {  // no event objects, no arguments
                Util.assertNotExists(ARGV + " in engine scope Bindings", engineBindings.containsKey(ARGV));
                Util.assertNotExists(EVENT + " in engine scope Bindings", engineBindings.containsKey(EVENT));
            }
            else {    // this has events on the Button
                Util.assertExists(ARGV + " in engine scope Bindings", engineBindings.containsKey(ARGV));
                Object[] argv = (Object[]) engineBindings.get(ARGV);

                Util.assertExists(EVENT + " in engine scope Bindings", engineBindings.containsKey(EVENT));
                obj = engineBindings.get(EVENT);

                Util.assertSame("argv[0] == event", argv[0], obj);

                if (invocation == 9) {
                    Util.assertType(EVENT, MouseEvent.class, obj);
                    MouseEvent ev = (MouseEvent) obj;
                    Util.assertSame("MouseEvent.getSource() == btn", ev.getSource(), btn);
                    Util.assertSame("MouseEvent.MOUSE_CLICKED", MouseEvent.MOUSE_CLICKED, ev.getEventType());
                } else {
                    Util.assertType(EVENT, ActionEvent.class, obj);
                    ActionEvent ev = (ActionEvent) obj;
                    Util.assertSame("ActionEvent.getSource() == btn", ev.getSource(), btn);
                }
            }

            // check filename and script
            String filename = (String) engineBindings.get(FILENAME);
            boolean ok = false;
            switch (invocation) {
                case 1:
                    Util.assertEndsWith  ("demo_02_topscript.sqtmc", filename);
                    Util.assertStartsWith("RgfPseudoCompiledScript.eval(): RgfPseudoCompiledScript=[demo_02_topscript.sqtmc " +
                                          "file - pseudo script", script);
                    break;

                case 2:
                    Util.assertEndsWith  ("demo_02_middlescript.sqtmc", filename);
                    Util.assertStartsWith("RgfPseudoCompiledScript.eval(): RgfPseudoCompiledScript=[demo_02_middlescript.sqtmc " +
                                          "file - pseudo script", script);
                    break;

                case 3:
                    Util.assertEndsWith("demo_02_on.fxml-script_starting_at_line_52", filename);
                    Util.assertStartsWith("RgfPseudoCompiledScript.eval(): RgfPseudoCompiledScript=[demo_02_on.fxml embedded " +
                                          "script sqtmc - line # 52", script);
                    break;

                case 4:
                    Util.assertEndsWith  ("demo_02_bottomscript.sqtmc", filename);
                    Util.assertStartsWith("RgfPseudoCompiledScript.eval(): RgfPseudoCompiledScript=[demo_02_bottomscript.sqtmc " +
                                          "file - pseudo script", script);
                    break;

                case 5:
                    Util.assertEndsWith("demo_02_on.fxml-script_starting_at_line_56", filename);
                    Util.assertStartsWith("RgfPseudoCompiledScript.eval(): RgfPseudoCompiledScript=[something (line # 56)", script);
                    break;

                case 6:
                    Util.assertEndsWith("demo_02_on.fxml-script_starting_at_line_59", filename);
                    Util.assertStartsWith("RgfPseudoCompiledScript.eval(): RgfPseudoCompiledScript=[demo_02_on.fxml (line # 59):", script);
                    break;

                case 7:     // same as case 8 (same button clicked)
                    Util.assertEndsWith("demo_02_on.fxml-onAction_attribute_in_element_ending_at_line_46", filename);
                    Util.assertStartsWith("RgfPseudoCompiledScript.eval(Bindings bindings): RgfPseudoCompiledScript=[demo_02_on.fxml " +
                                          "embedded event - ActionEvent - line # 45 -", script);
                    break;

                case 8:     // same as case 7 (same button clicked)
                    Util.assertEndsWith("demo_02_on.fxml-onAction_attribute_in_element_ending_at_line_46", filename);
                    Util.assertStartsWith("RgfPseudoCompiledScript.eval(Bindings bindings): RgfPseudoCompiledScript=[demo_02_on.fxml " +
                                          "embedded event - ActionEvent - line # 45 -", script);
                    break;

                case 9:
                    Util.assertEndsWith("demo_02_on.fxml-onMouseClicked_attribute_in_element_ending_at_line_46", filename);
                    Util.assertStartsWith("RgfPseudoCompiledScript.eval(Bindings bindings): RgfPseudoCompiledScript=[demo_02_on.fxml " +
                                          "embedded event - MouseClicked - line # 44", script);
                    break;
            }
        }
    }
}
