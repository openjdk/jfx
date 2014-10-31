/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package hello;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import hello.dialog.dialogs.CommandLinksDialog;
import hello.dialog.dialogs.ExceptionDialog;
import hello.dialog.dialogs.FontSelectorDialog;
import hello.dialog.fxml.FXMLSampleDialog;
import hello.dialog.wizard.LinearWizardFlow;
import hello.dialog.wizard.Wizard;
import hello.dialog.wizard.Wizard.WizardPane;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class HelloDialogs extends Application {

    private final ComboBox<StageStyle> styleCombobox = new ComboBox<>();
    private final ComboBox<Modality> modalityCombobox = new ComboBox<Modality>();
    private final CheckBox cbUseBlocking = new CheckBox();
    private final CheckBox cbCloseDialogAutomatically = new CheckBox();
    private final CheckBox cbShowMasthead = new CheckBox();
    private final CheckBox cbSetOwner = new CheckBox();
    private final CheckBox cbCustomGraphic = new CheckBox();
    
    private Stage stage;

    @Override public void start(Stage stage) throws Exception {
        this.stage = stage;
        // VBox vbox = new VBox(10);
        // vbox.setAlignment(Pos.CENTER);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setHgap(10);
        grid.setVgap(10);

        int row = 0;

        // *******************************************************************
        // Information Dialog
        // *******************************************************************

        grid.add(createLabel("Information Dialog: "), 0, row);

        final Button Hyperlink2 = new Button("Show");
        Hyperlink2.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                Alert dlg = createAlert(AlertType.INFORMATION);
                dlg.setTitle("Custom title");
                String optionalMasthead = "Wouldn't this be nice?";
                dlg.getDialogPane().setContentText("A collection of pre-built JavaFX dialogs?\nSeems like a great idea to me...");
                configureSampleDialog(dlg, optionalMasthead);
                
                // lets get some output when events happen
                dlg.setOnShowing(evt -> System.out.println(evt));
                dlg.setOnShown(evt -> System.out.println(evt));
                dlg.setOnHiding(evt -> System.out.println(evt));
                dlg.setOnHidden(evt -> System.out.println(evt));
                
//              dlg.setOnCloseRequest(evt -> evt.consume());
                
                showDialog(dlg);
            }
        });

        final Button Hyperlink2a = new Button("2 x Buttons (no cancel)");
        Hyperlink2a.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                Alert dlg = createAlert(AlertType.INFORMATION);
                dlg.setTitle("Custom title");
                String optionalMasthead = "Wouldn't this be nice?";
                dlg.getDialogPane().setContentText("A collection of pre-built JavaFX dialogs?\nSeems like a great idea to me...");
                configureSampleDialog(dlg, optionalMasthead);
                dlg.getButtonTypes().add(ButtonType.NEXT);

//              dlg.setOnCloseRequest(evt -> evt.consume());

                showDialog(dlg);
            }
        });

        grid.add(new HBox(10, Hyperlink2, Hyperlink2a), 1, row);

        row++;

        // *******************************************************************
        // Confirmation Dialog
        // *******************************************************************

        grid.add(createLabel("Confirmation Dialog: "), 0, row);

        final CheckBox cbShowCancel = new CheckBox("Show Cancel Button");
        cbShowCancel.setSelected(true);

        final Button Hyperlink3 = new Button("Show");
        Hyperlink3.setOnAction(e -> {
            Alert dlg = createAlert(AlertType.CONFIRMATION);
            dlg.setTitle("You do want dialogs right?");
            String optionalMasthead = "Just Checkin'";
            dlg.getDialogPane().setContentText("I was a bit worried that you might not want them, so I wanted to double check.");

            if (!cbShowCancel.isSelected()) {
                dlg.getDialogPane().getButtonTypes().remove(ButtonType.CANCEL);
            }

            configureSampleDialog(dlg, optionalMasthead);
            showDialog(dlg);
        });
        grid.add(new HBox(10, Hyperlink3, cbShowCancel), 1, row);

        row++;

        // *******************************************************************
        // Warning Dialog
        // *******************************************************************

        grid.add(createLabel("Warning Dialog: "), 0, row);

        final Button Hyperlink6a = new Button("Show");
        Hyperlink6a.setOnAction(e -> {
            Alert dlg = createAlert(AlertType.WARNING);
            dlg.setTitle("I'm warning you!");
            String optionalMasthead = "This is a warning";
            dlg.getDialogPane().setContentText("I'm glad I didn't need to use this...");
            configureSampleDialog(dlg, optionalMasthead);
            showDialog(dlg);
        });
        grid.add(new HBox(10, Hyperlink6a), 1, row);

        row++;

        // *******************************************************************
        // Error Dialog
        // *******************************************************************

        grid.add(createLabel("Error Dialog: "), 0, row);

        final Button Hyperlink7a = new Button("Show");
        Hyperlink7a.setOnAction(e -> {
            Alert dlg = createAlert(AlertType.ERROR);
            dlg.setTitle("It looks like you're making a bad decision");
            String optionalMasthead = "Exception Encountered";
            dlg.getDialogPane().setContentText("Better change your mind - this is really your last chance! (Even longer text that should probably wrap)");
            configureSampleDialog(dlg, optionalMasthead);
            showDialog(dlg);
        });
        grid.add(new HBox(10, Hyperlink7a), 1, row);

        row++;

        // *******************************************************************
        // More Details Dialog
        // *******************************************************************

        grid.add(createLabel("'Exception' Dialog: "), 0, row);

        final Button Hyperlink5a = new Button("Show");
        Hyperlink5a.setOnAction(e -> {
            ExceptionDialog dlg = new ExceptionDialog(new RuntimeException("Exception text"));
            dlg.setTitle("It looks like you're making a bad decision");
            String optionalMasthead = "Exception Encountered";
            dlg.getDialogPane().setContentText("This is the content to show to the user - but it'll be a good idea to see what the exception text says...");
            configureSampleDialog(dlg, optionalMasthead);
            showDialog(dlg);
        });

        grid.add(new HBox(10, Hyperlink5a), 1, row);
        row++;

        // *******************************************************************
        // Input Dialog (with masthead)
        // *******************************************************************

        grid.add(createLabel("Input Dialog: "), 0, row);

        final Button Hyperlink8 = new Button("TextField");
        Hyperlink8.setOnAction(e -> {
            TextInputDialog dlg = new TextInputDialog("");
            dlg.setTitle("Name Check");
            String optionalMasthead = "Please type in your name";
            dlg.getDialogPane().setContentText("What is your name?");
            configureSampleDialog(dlg, optionalMasthead);
            showDialog(dlg);
        });

        final Button Hyperlink9 = new Button("Initial Value Set");
        Hyperlink9.setOnAction(e -> {
            TextInputDialog dlg = new TextInputDialog("Jonathan");
            dlg.setTitle("Name Guess");
            String optionalMasthead = "Name Guess";
            dlg.getDialogPane().setContentText("Pick a name?");
            configureSampleDialog(dlg, optionalMasthead);
            showDialog(dlg);
        });

        final Button Hyperlink10 = new Button("Set Choices (< 10)");
        Hyperlink10.setOnAction(e -> {
            ChoiceDialog<String> dlg = new ChoiceDialog<String>("Jonathan",
                                                                "Matthew", "Jonathan", "Ian", "Sue", "Hannah");
            dlg.setTitle("Name Guess");
            String optionalMasthead = "Name Guess";
            dlg.getDialogPane().setContentText("Pick a name?");
            configureSampleDialog(dlg, optionalMasthead);
            showDialog(dlg);
        });

        final Button Hyperlink11 = new Button("Set Choices (>= 10)");
        Hyperlink11.setOnAction(e -> {
            ChoiceDialog<String> dlg = new ChoiceDialog<String>("Jonathan",
                                                                "Matthew", "Jonathan", "Ian", "Sue",
                                                                "Hannah", "Julia", "Denise", "Stephan",
                                                                "Sarah", "Ron", "Ingrid");
            dlg.setTitle("Name Guess");
            String optionalMasthead = "Name Guess";
            dlg.getDialogPane().setContentText("Pick a name?");
            configureSampleDialog(dlg, optionalMasthead);
            showDialog(dlg);
        });

        grid.add(
                new HBox(10, Hyperlink8, Hyperlink9, Hyperlink10, Hyperlink11),
                1, row);
        row++;

        // *******************************************************************
        // Command links
        // *******************************************************************

        grid.add(createLabel("Other pre-built dialogs: "), 0, row);
        final Button Hyperlink12 = new Button("Command Links");
        Hyperlink12.setOnAction(e -> {
            List<ButtonType> links = Arrays
                    .asList(buildCommandLink("Add a network that is in the range of this computer", false),
                            buildCommandLink("Manually create a network profile", true),
                            buildCommandLink("Create an ad hoc network", false));

            CommandLinksDialog dlg = new CommandLinksDialog(links);
            dlg.setTitle("Manually connect to wireless network");
            String optionalMasthead = "Manually connect to wireless network";
            dlg.getDialogPane().setContentText("How do you want to add a network?");
            configureSampleDialog(dlg, optionalMasthead);
            showDialog(dlg);
        });

        final Button Hyperlink12a = new Button("Font Chooser");
        Hyperlink12a.setOnAction(e -> {
            FontSelectorDialog dlg = new FontSelectorDialog(null);
            configureSampleDialog(dlg, "");
            showDialog(dlg);
        });

        grid.add(new HBox(10, Hyperlink12, Hyperlink12a), 1, row);
        row++;

        // *******************************************************************
        // Custom dialogs
        // *******************************************************************

        grid.add(createLabel("Custom Dialog: "), 0, row);
        final Button Hyperlink14 = new Button("Login");
        Hyperlink14.setOnAction(new EventHandler<ActionEvent>() {
            final TextField txUserName = new TextField();
            final PasswordField txPassword = new PasswordField();
            
            final ButtonType loginButtonType = new ButtonType("Login", ButtonData.OK_DONE);
            
            Dialog<String> dlg;

            private void validate() {
                boolean disabled = txUserName.getText().trim().isEmpty() || txPassword.getText().trim().isEmpty();
                dlg.getDialogPane().lookupButton(loginButtonType).setDisable(disabled);
            }

            @Override
            public void handle(ActionEvent arg0) {
                dlg = new Dialog<String>();
                dlg.setResultConverter(buttonType -> buttonType == loginButtonType ? 
                        "[" + txUserName.getText() + "/" + txPassword.getText() + "]" : 
                         null);
                dlg.initOwner(cbSetOwner.isSelected() ? stage : null);
                dlg.setTitle("Login Dialog");
                dlg.initModality(modalityCombobox.getValue());
                
                if (cbShowMasthead.isSelected()) {
                    dlg.getDialogPane().setHeaderText("Login to ControlsFX");
                }

                ChangeListener<String> changeListener = (o, oldValue, newValue) -> validate();
                txUserName.textProperty().addListener(changeListener);
                txPassword.textProperty().addListener(changeListener);

                final GridPane content = new GridPane();
                content.setHgap(10);
                content.setVgap(10);

                content.add(new Label("User name"), 0, 0);
                content.add(txUserName, 1, 0);
                GridPane.setHgrow(txUserName, Priority.ALWAYS);
                content.add(new Label("Password"), 0, 1);
                content.add(txPassword, 1, 1);
                GridPane.setHgrow(txPassword, Priority.ALWAYS);

                dlg.setResizable(false);
                dlg.getDialogPane().setGraphic(new ImageView(new Image(HelloAccordion.class.getResource("heart_16.png").toExternalForm())));
                dlg.getDialogPane().setContent(content);
                
                // instead of creating an action, we have to do the following:
                dlg.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);
                Node loginButtonNode = dlg.getDialogPane().lookupButton(loginButtonType);
                ((Button) loginButtonNode).setOnAction(evt -> {
                    dlg.setResult("[" + txUserName.getText() + " " + txPassword.getText() + "]");
                });
                
                validate();

                Platform.runLater( () -> txUserName.requestFocus() );

                dlg.showAndWait().ifPresent(result -> System.out.println("Result is " + result));
            }
        });
        
        final Button Hyperlink14a = new Button("FXML");
        Hyperlink14a.setOnAction(event -> new FXMLSampleDialog().showAndWait());

        grid.add(new HBox(10, Hyperlink14, Hyperlink14a), 1, row++);
        
        
        // *******************************************************************
        // wizards
        // *******************************************************************

        grid.add(createLabel("Wizard: "), 0, row);
        final Button Hyperlink15a = new Button("Linear Wizard");
        Hyperlink15a.setOnAction(e -> showLinearWizard());
        
        final Button Hyperlink15b = new Button("Branching Wizard");
        Hyperlink15b.setOnAction(e -> showBranchingWizard());
        
        grid.add(new HBox(10, Hyperlink15a, Hyperlink15b), 1, row++);
        
        

        SplitPane splitPane = new SplitPane();
        splitPane.setPrefSize(1200, 500);
        splitPane.getItems().addAll(grid, getControlPanel());
        splitPane.setDividerPositions(0.6);
        
        Scene scene = new Scene(splitPane);
        stage.setScene(scene);
        stage.setTitle("JavaFX Dialogs");
        stage.show();
    }

    private Alert createAlert(AlertType type) {
        Window owner = cbSetOwner.isSelected() ? stage : null;
        Alert dlg = new Alert(type, "");
        dlg.initModality(modalityCombobox.getValue());
        dlg.initOwner(owner);
        return dlg;
    }
    
    private void configureSampleDialog(Dialog<?> dlg, String masthead) {
        dlg.getDialogPane().setHeaderText(cbShowMasthead.isSelected() ? masthead : null);
        
        if (cbCustomGraphic.isSelected()) {
            URL url = getClass().getResource("dialog/tick.png");
            Image image = new Image(url.toExternalForm());
            ImageView iv = new ImageView(image);
            dlg.getDialogPane().setGraphic(iv);
        }
        
        dlg.initStyle(styleCombobox.getValue());
    }
    
    private void showDialog(Dialog<?> dlg) {
        if (cbCloseDialogAutomatically.isSelected()) {
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("Attempting to close dialog now...");
                Platform.runLater(() -> dlg.close());
            }).start();
        }

        if (cbUseBlocking.isSelected()) {
            dlg.showAndWait().ifPresent(result -> System.out.println("Result is " + result));
        } else {
            dlg.show();
            dlg.resultProperty().addListener(o -> System.out.println("Result is: " + dlg.getResult()));
            System.out.println("This println is _after_ the show method - we're non-blocking!");
        }
    }

    private Node getControlPanel() {
        GridPane grid = new GridPane();
        grid.setVgap(10);
        grid.setHgap(10);
        grid.setPadding(new Insets(30, 30, 0, 30));

        int row = 0;

        // stage style
        grid.add(createLabel("Style: ", "property"), 0, row);
        styleCombobox.getItems().setAll(StageStyle.values());
        styleCombobox.setValue(styleCombobox.getItems().get(0));
        grid.add(styleCombobox, 1, row);
        row++;

        // modality
        grid.add(createLabel("Modality: ", "property"), 0, row);
        modalityCombobox.getItems().setAll(Modality.values());
        modalityCombobox.setValue(modalityCombobox.getItems().get(Modality.values().length-1));
        grid.add(modalityCombobox, 1, row);
        row++;
        
        // use blocking
        cbUseBlocking.setSelected(true);
        grid.add(createLabel("Use blocking: ", "property"), 0, row);
        grid.add(cbUseBlocking, 1, row);
        row++;

        // close dialog automatically
        grid.add(createLabel("Close dialog after 2000ms: ", "property"), 0, row);
        grid.add(cbCloseDialogAutomatically, 1, row);
        row++;

        // show masthead
        grid.add(createLabel("Show masthead: ", "property"), 0, row);
        grid.add(cbShowMasthead, 1, row);
        row++;

        // set owner
        grid.add(createLabel("Set dialog owner: ", "property"), 0, row);
        grid.add(cbSetOwner, 1, row);
        row++;
        
        // custom graphic
        grid.add(createLabel("Use custom graphic: ", "property"), 0, row);
        grid.add(cbCustomGraphic, 1, row);
        row++;

        return grid;
    }
    
    private ButtonType buildCommandLink(String text, boolean isDefault) {
        return new ButtonType(text, isDefault ? ButtonData.OK_DONE : ButtonData.OTHER);
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

    private Node createLabel(String text, String... styleclass) {
        Label label = new Label(text);

        if (styleclass == null || styleclass.length == 0) {
            label.setFont(Font.font(13));
        } else {
            label.getStyleClass().addAll(styleclass);
        }
        return label;
    }

    private void showLinearWizard() {
        // define pages to show

        Wizard wizard = new Wizard();
        
        // --- page 1
        int row = 0;

        GridPane page1Grid = new GridPane();
        page1Grid.setVgap(10);
        page1Grid.setHgap(10);

        page1Grid.add(new Label("First Name:"), 0, row);
        TextField txFirstName = createTextField("firstName");
//        wizard.getValidationSupport().registerValidator(txFirstName, Validator.createEmptyValidator("First Name is mandatory"));  
        page1Grid.add(txFirstName, 1, row++);

        page1Grid.add(new Label("Last Name:"), 0, row);
        TextField txLastName = createTextField("lastName");
//        wizard.getValidationSupport().registerValidator(txLastName, Validator.createEmptyValidator("Last Name is mandatory"));
        page1Grid.add(txLastName, 1, row);

        WizardPane page1 = new WizardPane();
        page1.setHeaderText("Please Enter Your Details");
        page1.setContent(page1Grid);


        // --- page 2
        final WizardPane page2 = new WizardPane() {
            @Override public void onEnteringPage(Wizard wizard) {
                String firstName = (String) wizard.getSettings().get("firstName");
                String lastName = (String) wizard.getSettings().get("lastName");

                setContentText("Welcome, " + firstName + " " + lastName + "! Let's add some newlines!\n\n\n\n\n\n\nHello World!");
            }
        };
        page2.setHeaderText("Thanks For Your Details!");


        // --- page 3
        WizardPane page3 = new WizardPane();
        page3.setHeaderText("Goodbye!");
        page3.setContentText("Page 3, with extra 'help' button!");
        
        ButtonType helpDialogButton = new ButtonType("Help", ButtonData.HELP_2);
        page3.getButtonTypes().add(helpDialogButton);
        Button helpButton = (Button) page3.lookupButton(helpDialogButton);
        helpButton.addEventFilter(ActionEvent.ACTION, actionEvent -> {
            actionEvent.consume(); // stop hello.dialog from closing
            System.out.println("Help clicked!");
        });
                
                

        // create wizard
        wizard.setFlow(new LinearWizardFlow(page1, page2, page3));
        
        System.out.println("page1: " + page1);
        System.out.println("page2: " + page2);
        System.out.println("page3: " + page3);

        // show wizard and wait for response
        wizard.showAndWait().ifPresent(result -> {
            if (result == ButtonType.FINISH) {
                System.out.println("Wizard finished, settings: " + wizard.getSettings());
            }
        });
    }
    
    private void showBranchingWizard() {
        // define pages to show.
        // Because page1 references page2, we need to declare page2 first.
        final WizardPane page2 = new WizardPane();
        page2.setContentText("Page 2");

        final CheckBox checkBox = new CheckBox("Skip the second page");
        checkBox.setId("skip-page-2");
        VBox vbox = new VBox(10, new Label("Page 1"), checkBox);
        final WizardPane page1 = new WizardPane() {
            // when we exit page 1, we will check the state of the 'skip page 2'
            // checkbox, and if it is true, we will remove page 2 from the pages list
            @Override public void onExitingPage(Wizard wizard) {
//                List<WizardPage> pages = wizard.getPages();
//                if (checkBox.isSelected()) {
//                    pages.remove(page2);
//                } else {
//                    if (! pages.contains(page2)) {
//                        pages.add(1, page2);
//                    }
//                }
            }
        };
        page1.setContent(vbox);

        final WizardPane page3 = new WizardPane();
        page3.setContentText("Page 3");

        // create wizard
        Wizard wizard = new Wizard();
        Wizard.Flow branchingFlow = new Wizard.Flow() {

            @Override
            public Optional<WizardPane> advance(WizardPane currentPage) {
                return Optional.of(getNext(currentPage));
            }

            @Override
            public boolean canAdvance(WizardPane currentPage) {
                return currentPage != page3;
            }
            
            private WizardPane getNext(WizardPane currentPage) {
                if ( currentPage == null ) {
                    return page1;
                } else if ( currentPage == page1) {
                    return checkBox.isSelected()? page3: page2;
                } else {
                    return page3;
                }
            }
            
        };
        
        //wizard.setFlow( new LinearWizardFlow( page1, page2, page3));
        wizard.setFlow( branchingFlow);

        // show wizard
        wizard.showAndWait().ifPresent(result -> {
            if (result == ButtonType.FINISH) {
                System.out.println("Wizard finished, settings: " + wizard.getSettings());
            }
        });
    }
    
    private TextField createTextField(String id) {
        TextField textField = new TextField();
        textField.setId(id);
        GridPane.setHgrow(textField, Priority.ALWAYS);
        return textField;
    }

}
