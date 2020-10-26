package seedu.address.ui;

import java.util.logging.Logger;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.Logic;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.patient.Patient;
import seedu.address.model.visit.Visit;

/**
 * The Main Window. Provides the basic application layout containing
 * a menu bar and space where other JavaFX elements can be placed.
 */
public class MainWindow extends UiPart<Stage> {

    private static final String FXML = "MainWindow.fxml";

    private final Logger logger = LogsCenter.getLogger(getClass());

    private Stage primaryStage;
    private Logic logic;

    // Independent Ui parts residing in this Ui container
    private PatientListPanel patientListPanel;
    private ResultDisplay resultDisplay;
    private HelpWindow helpWindow;
    private CalendarDisplay calendarDisplay;
    private ProfileWindow profilePanel;
    private VisitFormWindow visitWindow;
    private ProfileVisitPanel profileVisitPanel;
    private EmptyVisitList emptyVisitList;

    @FXML
    private StackPane commandBoxPlaceholder;

    @FXML
    private MenuItem helpMenuItem;

    @FXML
    private StackPane patientListPanelPlaceholder;

    @FXML
    private StackPane resultDisplayPlaceholder;

    @FXML
    private StackPane calendarDisplayPlaceholder;

    @FXML
    private StackPane statusbarPlaceholder;

    /**
     * Creates a {@code MainWindow} with the given {@code Stage} and {@code Logic}.
     */
    public MainWindow(Stage primaryStage, Logic logic) {
        super(FXML, primaryStage);

        // Set dependencies
        this.primaryStage = primaryStage;
        this.logic = logic;

        // Configure the UI
        setWindowDefaultSize(logic.getGuiSettings());

        setAccelerators();

        helpWindow = new HelpWindow();
        visitWindow = new VisitFormWindow(windowEvent -> {
            resultDisplay.setFeedbackToUser(visitWindow.getFeedbackMessage());
            visitWindow.flushParameters();
        });
        profilePanel = new ProfileWindow();
        profileVisitPanel = new ProfileVisitPanel();
        emptyVisitList = new EmptyVisitList();
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    private void setAccelerators() {
        setAccelerator(helpMenuItem, KeyCombination.valueOf("F1"));
    }

    /**
     * Sets the accelerator of a MenuItem.
     * @param keyCombination the KeyCombination value of the accelerator
     */
    private void setAccelerator(MenuItem menuItem, KeyCombination keyCombination) {
        menuItem.setAccelerator(keyCombination);

        /*
         * TODO: the code below can be removed once the bug reported here
         * https://bugs.openjdk.java.net/browse/JDK-8131666
         * is fixed in later version of SDK.
         *
         * According to the bug report, TextInputControl (TextField, TextArea) will
         * consume function-key events. Because CommandBox contains a TextField, and
         * ResultDisplay contains a TextArea, thus some accelerators (e.g F1) will
         * not work when the focus is in them because the key event is consumed by
         * the TextInputControl(s).
         *
         * For now, we add following event filter to capture such key events and open
         * help window purposely so to support accelerators even when focus is
         * in CommandBox or ResultDisplay.
         */
        getRoot().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getTarget() instanceof TextInputControl && keyCombination.match(event)) {
                menuItem.getOnAction().handle(new ActionEvent());
                event.consume();
            }
        });
    }

    /**
     * Fills up all the placeholders of this window.
     */
    void fillInnerParts() {
        patientListPanel = new PatientListPanel(logic.getFilteredPatientList(), logic);
        patientListPanelPlaceholder.getChildren().add(patientListPanel.getRoot());

        resultDisplay = new ResultDisplay();
        resultDisplayPlaceholder.getChildren().add(resultDisplay.getRoot());

        StatusBarFooter statusBarFooter = new StatusBarFooter(logic.getCliniCalFilePath());
        statusbarPlaceholder.getChildren().add(statusBarFooter.getRoot());

        CommandBox commandBox = new CommandBox(this::executeCommand);
        commandBoxPlaceholder.getChildren().add(commandBox.getRoot());

        calendarDisplay = new CalendarDisplay();
        calendarDisplayPlaceholder.getChildren().add(calendarDisplay.getRoot());
    }

    /**
     * Sets the default size based on {@code guiSettings}.
     */
    private void setWindowDefaultSize(GuiSettings guiSettings) {
        primaryStage.setHeight(guiSettings.getWindowHeight());
        primaryStage.setWidth(guiSettings.getWindowWidth());
        if (guiSettings.getWindowCoordinates() != null) {
            primaryStage.setX(guiSettings.getWindowCoordinates().getX());
            primaryStage.setY(guiSettings.getWindowCoordinates().getY());
        }
    }

    /**
     * Opens the help window or focuses on it if it's already opened.
     */
    @FXML
    public void handleHelp() {
        if (!helpWindow.isShowing()) {
            helpWindow.show();
        } else {
            helpWindow.focus();
        }
    }

    void show() {
        primaryStage.show();
    }

    /**
     * Exits the application.
     */
    @FXML
    private void handleExit() {
        GuiSettings guiSettings = new GuiSettings(primaryStage.getWidth(), primaryStage.getHeight(),
                (int) primaryStage.getX(), (int) primaryStage.getY());
        logic.setGuiSettings(guiSettings);
        helpWindow.hide();
        primaryStage.hide();
        profilePanel.hide();
        visitWindow.hide();
        profileVisitPanel.hide();
    }

    /**
     * Displays the visit window.
     * If it is already open, focus on the visit window.
     */
    @FXML
    public void handleDisplayVisit() {
        if (profileVisitPanel.isShowing()) {
            profileVisitPanel.hide();
        }

        if (!visitWindow.isShowing()) {
            visitWindow.show();
        } else {
            visitWindow.focus();
        }
    }

    /**
     * Displays the empty visit window.
     * If it is already open, focus on the empty visit window.
     */
    @FXML
    public void handleEmptyVisitHistory() {
        if (!emptyVisitList.isShowing()) {
            emptyVisitList.show();
        } else {
            emptyVisitList.focus();
        }
    }

    /**
     * Displays the patient profile panel.
     * If it is already open, focus on the profile panel.
     */
    @FXML
    public void handleProfilePanel() {
        if (!profilePanel.isShowing()) {
            profilePanel.show();
        } else {
            profilePanel.focus();
        }
    }

    /**
     * Executes the command and returns the result.
     *
     * @see seedu.address.logic.Logic#execute(String)
     */
    private CommandResult executeCommand(String commandText) throws CommandException, ParseException {
        try {
            CommandResult commandResult = logic.execute(commandText);
            logger.info("Result: " + commandResult.getFeedbackToUser());
            resultDisplay.setFeedbackToUser(commandResult.getFeedbackToUser());
            int patientIndex = commandResult.getPatientIndex();
            String visitDate = commandResult.getVisitDate();
            int visitIndex = commandResult.getVisitIndex();
            Visit previousVisit = commandResult.getPreviousVisit();
            ObservableList<Visit> observableHistory = commandResult.getObservableVisitHistory();
            Patient patient = commandResult.getPatientProfile();

            if (commandResult.isShowHelp()) {
                handleHelp();
            }

            if (commandResult.isExit()) {
                handleExit();
            }

            if (commandResult.isAddVisit()) {
                visitWindow.setVisitDetails(logic, visitDate, patientIndex);
                handleDisplayVisit();
            }

            if (commandResult.isEditVisit()) {
                visitWindow.setPreviousVisitDetails(logic, previousVisit, visitIndex, patientIndex);
                if (profileVisitPanel.isShowing()) {
                    profileVisitPanel.hide();
                }
                handleDisplayVisit();
            }

            if (commandResult.isDisplayVisitHistory()) {
                if (observableHistory.isEmpty()) {
                    if (profileVisitPanel.isShowing()) {
                        profileVisitPanel.hide();
                    }
                    handleEmptyVisitHistory();
                }
            }

            if (commandResult.isDisplayProfile()) {
                profilePanel.setup(patient, logic);
                handleProfilePanel();
            }

            return commandResult;
        } catch (CommandException | ParseException e) {
            logger.info("Invalid command: " + commandText);
            resultDisplay.setFeedbackToUser(e.getMessage());
            throw e;
        }
    }
}
