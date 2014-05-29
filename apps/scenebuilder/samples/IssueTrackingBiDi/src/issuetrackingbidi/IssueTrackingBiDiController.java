/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
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
package issuetrackingbidi;

import issuetrackingbidi.model.Issue;
import issuetrackingbidi.model.Issue.IssueStatus;
import issuetrackingbidi.model.ObservableIssue;
import issuetrackingbidi.model.TrackingService;
import issuetrackingbidi.model.TrackingServiceStub;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;

public class IssueTrackingBiDiController {

    @FXML // ResourceBundle that was given to the FXMLLoader
    private ResourceBundle resources;

    @FXML // URL location of the FXML file that was given to the FXMLLoader
    private URL location;

    @FXML //  fx:id="colName"
    private TableColumn<ObservableIssue, String> colName; // Value injected by FXMLLoader

    @FXML //  fx:id="colStatus"
    private TableColumn<ObservableIssue, IssueStatus> colStatus; // Value injected by FXMLLoader

    @FXML //  fx:id="colSynopsis"
    private TableColumn<ObservableIssue, String> colSynopsis; // Value injected by FXMLLoader
    
    @FXML //  fx:id="colNeeded"
    private TableColumn<ObservableIssue, LocalDate> colNeeded; // Value injected by FXMLLoader
    
    @FXML //  fx:id="deleteIssue"
    private Button deleteIssue; // Value injected by FXMLLoader
    
    @FXML // fx:id="neededBy"
    private DatePicker neededBy;  // Value injected by FXMLLoader

    @FXML //  fx:id="descriptionValue"
    private TextArea descriptionValue; // Value injected by FXMLLoader

    @FXML //  fx:id="details"
    private AnchorPane details; // Value injected by FXMLLoader

    @FXML //  fx:id="displayedIssueLabel"
    // It will contain a concatenation of the project name and the bug id.
    private Label displayedIssueLabel; // Value injected by FXMLLoader

    @FXML //  fx:id="list"
    private ListView<String> list; // Value injected by FXMLLoader

    @FXML //  fx:id="newIssue"
    private Button newIssue; // Value injected by FXMLLoader

    @FXML //  fx:id="saveIssue"
    private Button saveIssue; // Value injected by FXMLLoader

    @FXML //  fx:id="synopsis"
    private TextField synopsis; // Value injected by FXMLLoader

    @FXML //  fx:id="table"
    private TableView<ObservableIssue> table; // Value injected by FXMLLoader

    private String displayedBugId; // the id of the bug displayed in the details section.
    private String displayedBugProject; // the name of the project of the bug displayed in the detailed section.
    ObservableList<String> projectsView = FXCollections.observableArrayList();
    TrackingService model = null;
    private TextField statusValue = new TextField();
    final ObservableList<ObservableIssue> tableContent = FXCollections.observableArrayList();
    
    /**
     * Initializes the controller class.
     */
    @FXML // This method is called by the FXMLLoader when initialization is complete
    void initialize() {
        assert colName != null : "fx:id=\"colName\" was not injected: check your FXML file 'IssueTrackingBiDi.fxml'.";
        assert colStatus != null : "fx:id=\"colStatus\" was not injected: check your FXML file 'IssueTrackingBiDi.fxml'.";
        assert colSynopsis != null : "fx:id=\"colSynopsis\" was not injected: check your FXML file 'IssueTrackingBiDi.fxml'.";
        assert colNeeded != null : "fx:id=\"colUpdated\" was not injected: check your FXML file 'IssueTrackingBiDi.fxml'.";
        assert deleteIssue != null : "fx:id=\"deleteIssue\" was not injected: check your FXML file 'IssueTrackingBiDi.fxml'.";
        assert descriptionValue != null : "fx:id=\"descriptionValue\" was not injected: check your FXML file 'IssueTrackingBiDi.fxml'.";
        assert details != null : "fx:id=\"details\" was not injected: check your FXML file 'IssueTrackingBiDi.fxml'.";
        assert displayedIssueLabel != null : "fx:id=\"displayedIssueLabel\" was not injected: check your FXML file 'IssueTrackingBiDi.fxml'.";
        assert newIssue != null : "fx:id=\"newIssue\" was not injected: check your FXML file 'IssueTrackingBiDi.fxml'.";
        assert saveIssue != null : "fx:id=\"saveIssue\" was not injected: check your FXML file 'IssueTrackingBiDi.fxml'.";
        assert synopsis != null : "fx:id=\"synopsis\" was not injected: check your FXML file 'IssueTrackingBiDi.fxml'.";
        assert table != null : "fx:id=\"table\" was not injected: check your FXML file 'IssueTrackingBiDi.fxml'.";
        assert list != null : "fx:id=\"list\" was not injected: check your FXML file 'IssueTrackingBiDi.fxml'.";
        assert neededBy != null : "fx:id=\"neededBy\" was not injected: check your FXML file 'IssueTrackingBiDi.fxml'.";
        
        System.out.println(this.getClass().getSimpleName() + ".initialize");
        configureButtons();
        configureDetails();
        configureTable();
        connectToService();
        if (list != null) {
            list.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            list.getSelectionModel().selectedItemProperty().addListener(projectItemSelected);
            displayedProjectNames.addListener(projectNamesListener);
        }
    }

    /**
     * Called when the NewIssue button is fired.
     *
     * @param event the action event.
     */
    @FXML
    void newIssueFired(ActionEvent event) {
        final String selectedProject = getSelectedProject();
        if (model != null && selectedProject != null) {
            ObservableIssue issue = model.createIssueFor(selectedProject);
            if (table != null) {
                // Select the newly created issue.
                table.getSelectionModel().clearSelection();
                table.getSelectionModel().select(issue);
            }
        }
    }

    /**
     * Called when the DeleteIssue button is fired.
     *
     * @param event the action event.
     */
    @FXML
    void deleteIssueFired(ActionEvent event) {
        final String selectedProject = getSelectedProject();
        if (model != null && selectedProject != null && table != null) {
            // We create a copy of the current selection: we can't delete
            //    issue while looping over the live selection, since
            //    deleting selected issues will modify the selection.
            final List<?> selectedIssue = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            for (Object o : selectedIssue) {
                if (o instanceof ObservableIssue) {
                    model.deleteIssue(((ObservableIssue) o).getId());
                }
            }
            table.getSelectionModel().clearSelection();
        }
    }

    /**
     * Called when the SaveIssue button is fired.
     *
     * @param event the action event.
     */
    @FXML
    void saveIssueFired(ActionEvent event) {
        final ObservableIssue ref = getSelectedIssue();
        final Issue edited = new DetailsData();
        SaveState saveState = computeSaveState(edited, ref);
        if (saveState == SaveState.UNSAVED) {
            model.saveIssue(ref.getId(), 
                    edited.getStatus(),
                    edited.getSynopsis(), 
                    edited.getDescription(),
                    edited.getNeededBy());
        }
        // We refresh the content of the table because synopsis and/or description
        // are likely to have been modified by the user.
        int selectedRowIndex = table.getSelectionModel().getSelectedIndex();
        table.getItems().clear();
        displayedIssues = model.getIssueIds(getSelectedProject());
        for (String id : displayedIssues) {
            final ObservableIssue issue = model.getIssue(id);
            table.getItems().add(issue);
        }
        table.getSelectionModel().select(selectedRowIndex);

        updateSaveIssueButtonState();
    }
    
    private void configureButtons() {
        if (newIssue != null) {
            newIssue.setDisable(true);
        }
        if (saveIssue != null) {
            saveIssue.setDisable(true);
        }
        if (deleteIssue != null) {
            deleteIssue.setDisable(true);
        }
    }
    
    // An observable list of project names obtained from the model.
    // This is a live list, and we will react to its changes by removing
    // and adding project names to/from our list widget.
    private ObservableList<String> displayedProjectNames;
    
    // The list of Issue IDs relevant to the selected project. Can be null
    // if no project is selected. This list is obtained from the model.
    // This is a live list, and we will react to its changes by removing
    // and adding Issue objects to/from our table widget.
    private ObservableList<String> displayedIssues;
    
    // This listener will listen to changes in the displayedProjectNames list,
    // and update our list widget in consequence.
    private final ListChangeListener<String> projectNamesListener = new ListChangeListener<String>() {

        @Override
        public void onChanged(Change<? extends String> c) {
            if (projectsView == null) {
                return;
            }
            while (c.next()) {
                if (c.wasAdded() || c.wasReplaced()) {
                    for (String p : c.getAddedSubList()) {
                        projectsView.add(p);
                    }
                }
                if (c.wasRemoved() || c.wasReplaced()) {
                    for (String p : c.getRemoved()) {
                        projectsView.remove(p);
                    }
                }
            }
            FXCollections.sort(projectsView);
        }
    };
    
    // This listener will listen to changes in the displayedIssues list,
    // and update our table widget in consequence.
    private final ListChangeListener<String> projectIssuesListener = new ListChangeListener<String>() {

        @Override
        public void onChanged(Change<? extends String> c) {
            if (table == null) {
                return;
            }
            while (c.next()) {
                if (c.wasAdded() || c.wasReplaced()) {
                    for (String p : c.getAddedSubList()) {
                        table.getItems().add(model.getIssue(p));
                    }
                }
                if (c.wasRemoved() || c.wasReplaced()) {
                    for (String p : c.getRemoved()) {
                        ObservableIssue removed = null;
                        // Issue already removed:
                        // we can't use model.getIssue(issueId) to get it.
                        // we need to loop over the table content instead.
                        // Then we need to remove it - but outside of the for loop
                        // to avoid ConcurrentModificationExceptions.
                        for (ObservableIssue t : table.getItems()) {
                            if (t.getId().equals(p)) {
                                removed = t;
                                break;
                            }
                        }
                        if (removed != null) {
                            table.getItems().remove(removed);
                        }
                    }
                }
            }
        }
    };

    // Connect to the model, get the project's names list, and listen to
    // its changes. Initializes the list widget with retrieved project names.
    private void connectToService() {
        if (model == null) {
            model = new TrackingServiceStub();
            displayedProjectNames = model.getProjectNames();
        }
        projectsView.clear();
        List<String> sortedProjects = new ArrayList<>(displayedProjectNames);
        Collections.sort(sortedProjects);
        projectsView.addAll(sortedProjects);
        list.setItems(projectsView);
    }
    
    // This listener listen to changes in the table widget selection and
    // update the DeleteIssue button state accordingly.
    private final ListChangeListener<ObservableIssue> tableSelectionChanged =
            new ListChangeListener<ObservableIssue>() {

                @Override
                public void onChanged(Change<? extends ObservableIssue> c) {
                    updateDeleteIssueButtonState();
                    updateBugDetails();
                    updateSaveIssueButtonState();
                }
            };

    private static String nonNull(String s) {
        return s == null ? "" : s;
    }

    private void updateBugDetails() {
        final ObservableIssue selectedIssue = getSelectedIssue();
        if (details != null && selectedIssue != null) {
            if (displayedIssueLabel != null) {
                displayedBugId = selectedIssue.getId();
                displayedBugProject = selectedIssue.getProjectName();
                displayedIssueLabel.setText( displayedBugId + " / " + displayedBugProject );
            }
            if (synopsis != null) {
                synopsis.setText(nonNull(selectedIssue.getSynopsis()));
            }
            if (statusValue != null) {
                statusValue.setText(selectedIssue.getStatus().toString());
            }
            if (descriptionValue != null) {
                descriptionValue.selectAll();
                descriptionValue.cut();
                descriptionValue.setText(selectedIssue.getDescription());
            }
            if (neededBy != null) {
                neededBy.setValue(selectedIssue.getNeededBy());
            }
        } else {
            displayedIssueLabel.setText("");
            displayedBugId = null;
            displayedBugProject = null;
        }
        if (details != null) {
            details.setVisible(selectedIssue != null);
        }
    }

    private boolean isVoid(Object o) {
        if (o instanceof String) {
            return isEmpty((String) o);
        } else {
            return o == null;
        }
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private boolean equal(Object o1, Object o2) {
        if (isVoid(o1)) {
            return isVoid(o2);
        }
        return o1.equals(o2);
    }

    private static enum SaveState {

        INVALID, UNSAVED, UNCHANGED
    }

    private final class DetailsData implements Issue {

        @Override
        public String getId() {
            if (displayedBugId == null || isEmpty(displayedIssueLabel.getText())) {
                return null;
            }
            return displayedBugId;
        }

        @Override
        public IssueStatus getStatus() {
            if (statusValue == null || isEmpty(statusValue.getText())) {
                return null;
            }
            return IssueStatus.valueOf(statusValue.getText().trim());
        }
        
        @Override
        public String getProjectName() {
            if (displayedBugProject == null || isEmpty(displayedIssueLabel.getText())) {
                return null;
            }
            return displayedBugProject;
        }

        @Override
        public String getSynopsis() {
            if (synopsis == null || isEmpty(synopsis.getText())) {
                return "";
            }
            return synopsis.getText();
        }

        @Override
        public String getDescription() {
            if (descriptionValue == null || isEmpty(descriptionValue.getText())) {
                return "";
            }
            return descriptionValue.getText();
        }
        
        @Override
        public LocalDate getNeededBy() {
            if (neededBy == null) {
                    return LocalDate.now();
            }
            return neededBy.getValue();
        }
    }

    private SaveState computeSaveState(Issue edited, Issue issue) {
        try {
            // These fields are not editable - so if they differ they are invalid
            // and we cannot save.
            if (!equal(edited.getId(), issue.getId())) {
                return SaveState.INVALID;
            }
            if (!equal(edited.getProjectName(), issue.getProjectName())) {
                return SaveState.INVALID;
            }

            // If these fields differ, the issue needs saving.
            if (!equal(edited.getStatus(), issue.getStatus())) {
                return SaveState.UNSAVED;
            }
            if (!equal(edited.getSynopsis(), issue.getSynopsis())) {
                return SaveState.UNSAVED;
            }
            if (!equal(edited.getDescription(), issue.getDescription())) {
                return SaveState.UNSAVED;
            }
            if (!equal(edited.getNeededBy(), issue.getNeededBy())) {
                return SaveState.UNSAVED;
            }
        } catch (Exception x) {
            // If there's an exception, some fields are invalid.
            return SaveState.INVALID;
        }
        // No field is invalid, no field needs saving.
        return SaveState.UNCHANGED;
    }

    private void updateDeleteIssueButtonState() {
        boolean disable = true;
        if (deleteIssue != null && table != null) {
            final boolean nothingSelected = table.getSelectionModel().getSelectedItems().isEmpty();
            disable = nothingSelected;
        }
        if (deleteIssue != null) {
            deleteIssue.setDisable(disable);
        }
    }

    private void updateSaveIssueButtonState() {
        boolean disable = true;
        if (saveIssue != null && table != null) {
            final boolean nothingSelected = table.getSelectionModel().getSelectedItems().isEmpty();
            disable = nothingSelected;
        }
        if (disable == false) {
            disable = computeSaveState(new DetailsData(), getSelectedIssue()) != SaveState.UNSAVED;
        }
        if (saveIssue != null) {
            saveIssue.setDisable(disable);
        }
    }

    // Configure the table widget: set up its column, and register the
    // selection changed listener.
    private void configureTable() {
        colName.setCellValueFactory(new PropertyValueFactory<>("id"));
        colSynopsis.setCellValueFactory(new PropertyValueFactory<>("synopsis"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colNeeded.setCellValueFactory(new PropertyValueFactory<>("neededBy"));

        // In order to limit the amount of setup in Getting Started we set the width
        // of the 3 columns programmatically but one can do it from SceneBuilder.
        colName.setPrefWidth(75);
        colStatus.setPrefWidth(75);
        colSynopsis.setPrefWidth(300);
        colNeeded.setPrefWidth(90);

        colName.setMinWidth(75);
        colStatus.setMinWidth(75);
        colSynopsis.setMinWidth(300);
        colNeeded.setMinWidth(90);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.setItems(tableContent);
        assert table.getItems() == tableContent;

        final ObservableList<ObservableIssue> tableSelection = table.getSelectionModel().getSelectedItems();

        tableSelection.addListener(tableSelectionChanged);
    }

    /**
     * Return the name of the project currently selected, or null if no project
     * is currently selected.
     *
     */
    public String getSelectedProject() {
        if (model != null && list != null) {
            final ObservableList<String> selectedProjectItem = list.getSelectionModel().getSelectedItems();
            final String selectedProject = selectedProjectItem.get(0);
            return selectedProject;
        }
        return null;
    }

    public ObservableIssue getSelectedIssue() {
        if (model != null && table != null) {
            List<ObservableIssue> selectedIssues = table.getSelectionModel().getSelectedItems();
            if (selectedIssues.size() == 1) {
                final ObservableIssue selectedIssue = selectedIssues.get(0);
                return selectedIssue;
            }
        }
        return null;
    }
    
    /**
     * Listen to changes in the list selection, and updates the table widget and
     * DeleteIssue and NewIssue buttons accordingly.
     */
    private final ChangeListener<String> projectItemSelected = new ChangeListener<String>() {

        @Override
        public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
            projectUnselected(oldValue);
            projectSelected(newValue);
        }
    };

    // Called when a project is unselected.
    private void projectUnselected(String oldProjectName) {
        if (oldProjectName != null) {
            displayedIssues.removeListener(projectIssuesListener);
            displayedIssues = null;
            table.getSelectionModel().clearSelection();
            table.getItems().clear();
            if (newIssue != null) {
                newIssue.setDisable(true);
            }
            if (deleteIssue != null) {
                deleteIssue.setDisable(true);
            }
        }
    }

    // Called when a project is selected.
    private void projectSelected(String newProjectName) {
        if (newProjectName != null) {
            table.getItems().clear();
            displayedIssues = model.getIssueIds(newProjectName);
            for (String id : displayedIssues) {
                final ObservableIssue issue = model.getIssue(id);
                table.getItems().add(issue);
            }
            displayedIssues.addListener(projectIssuesListener);
            if (newIssue != null) {
                newIssue.setDisable(false);
            }
            updateDeleteIssueButtonState();
            updateSaveIssueButtonState();
        }
    }

    private void configureDetails() {
        if (details != null) {
            details.setVisible(false);
        }

        if (details != null) {
            details.addEventFilter(EventType.ROOT, new EventHandler<Event>() {

                @Override
                public void handle(Event event) {
                    if (event.getEventType() == MouseEvent.MOUSE_RELEASED
                            || event.getEventType() == KeyEvent.KEY_RELEASED) {
                        updateSaveIssueButtonState();
                    }
                }
            });
        }
    }
}
