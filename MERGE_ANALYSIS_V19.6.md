# V19.6 Feature Merge Analysis
## Detailed Code Snippets for V13.3_merged Branch

**Analysis Date:** April 14, 2026  
**Target:** Merging V19.6 features into V13.3_merged branch

---

## 1. TaskController.java - NEW IMPORTS & CLASS DECLARATION

### New Import to Add:
```java
import com.cc103sys.cc103.Utils.TimerService;
```

### Class Declaration Update:
```java
public class TaskController implements TimerService.TimerListener {
```

---

## 2. TaskController.java - NEW CLASS FIELDS

### New Instance Variables to Add:
```java
// Timer Service
private TimerService timerService;

// Tab Navigation Fields
@FXML private Button tabMyTasksBtn;
@FXML private Button tabApprovalsBtn;
@FXML private VBox myTasksView;
@FXML private VBox approvalsView;

// Task Details Display
@FXML private TextArea taskDescriptionArea;
@FXML private TextArea taskInstructions;

// Attachment Management
@FXML private Button uploadAttachmentBtn;
@FXML private ListView<String> attachmentsList;

// Task Actions
@FXML private Button startTimerBtn;
@FXML private Button saveTaskInfoBtn;

// Approval Management (for hosts/teachers)
@FXML private ListView<Task> pendingApprovalsList;
@FXML private Button approveSelectedBtn;
@FXML private Button rejectSelectedBtn;
```

---

## 3. TaskController.java - INITIALIZATION IN initialize()

### Add to initialize() method:
```java
// Initialize TimerService
timerService = TimerService.getInstance();
timerService.removeTimerListener(this);
timerService.addTimerListener(this);

// Start with My Tasks view
switchToMyTasksView();
```

---

## 4. TaskController.java - NEW METHODS FOR TAB SWITCHING

### Add these new methods:
```java
private void switchToMyTasksView() {
    if (myTasksView != null) {
        myTasksView.setVisible(true);
        myTasksView.setManaged(true);
    }
    if (approvalsView != null) {
        approvalsView.setVisible(false);
        approvalsView.setManaged(false);
    }
    updateTabStyles(true);
    loadAllClassTasks();
    loadFilteredTasks();
}

private void switchToApprovalsView() {
    if (myTasksView != null) {
        myTasksView.setVisible(false);
        myTasksView.setManaged(false);
    }
    if (approvalsView != null) {
        approvalsView.setVisible(true);
        approvalsView.setManaged(true);
    }
    updateTabStyles(false);
    loadPendingApprovals();
}

private void updateTabStyles(boolean isMyTasksActive) {
    String activeStyle = "-fx-background-color: #3182ce; -fx-text-fill: white; -fx-border-color: #3182ce;";
    String inactiveStyle = "-fx-background-color: white; -fx-text-fill: #cbd5e0; -fx-border-color: #e2e8f0;";
    
    if (tabMyTasksBtn != null) {
        tabMyTasksBtn.setStyle(isMyTasksActive ? activeStyle : inactiveStyle);
    }
    if (tabApprovalsBtn != null) {
        tabApprovalsBtn.setStyle(isMyTasksActive ? inactiveStyle : activeStyle);
    }
}
```

---

## 5. TaskController.java - ROLE-BASED UI SETUP UPDATE

### Modify setupRoleBasedUI() to include:
```java
private void setupRoleBasedUI() {
    boolean isHost = "HOST".equalsIgnoreCase(Session.getRole());
    
    // Show/hide approvals view for hosts only
    if (tabApprovalsBtn != null) {
        tabApprovalsBtn.setVisible(isHost);
    }
    if (approvalsView != null) {
        approvalsView.setVisible(false);
        approvalsView.setManaged(false);
    }
    
    // All users can add personal tasks
}
```

---

## 6. TaskController.java - TIMER HANDLING METHODS

### Add Timer-related methods:
```java
private void handleStartTimer() {
    Task selected = allTaskList.getSelectionModel().getSelectedItem();
    if (selected == null) {
        selected = filteredTaskList.getSelectionModel().getSelectedItem();
    }
    if (selected == null) return;
    
    // Assuming task has duration field (default 25 minutes = 1500 seconds)
    int totalSeconds = 1500; // 25 minutes
    timerService.start(totalSeconds, selected.getId(), true);
}

@Override
public Button getStartTimerBtn() {
    return startTimerBtn;
}

@Override
public void setStartTimerBtn(Button startTimerBtn) {
    this.startTimerBtn = startTimerBtn;
}
```

---

## 7. TaskController.java - ATTACHMENT HANDLING METHODS

### Add Attachment-related methods:
```java
private void handleUploadAttachment() {
    Task selected = allTaskList.getSelectionModel().getSelectedItem();
    if (selected == null) {
        selected = filteredTaskList.getSelectionModel().getSelectedItem();
    }
    if (selected == null) return;
    
    showAttachmentDialog(selected);
}

private void handleSaveTaskInfo() {
    Task selected = allTaskList.getSelectionModel().getSelectedItem();
    if (selected == null) {
        selected = filteredTaskList.getSelectionModel().getSelectedItem();
    }
    if (selected == null) return;
    
    String notes = taskInstructions.getText();
    try (Connection conn = DBUtil.getConnection()) {
        String updateSql = "UPDATE tasks SET description = ? WHERE id = ? AND username = ?";
        try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setString(1, notes);
            stmt.setInt(2, selected.getId());
            stmt.setString(3, Session.getUsername());
            stmt.executeUpdate();
        }
        LOGGER.info("Task notes saved for task: " + selected.getTaskName());
    } catch (Exception e) {
        LOGGER.severe("Failed to save task notes: " + e.getMessage());
    }
}

private void showAttachmentDialog(Task task) {
    // File chooser for attachment uploads
    java.io.File selectedFile = new java.io.File(System.getProperty("user.home"));
    
    // In production, integrate with FileChooser API
    // For now, store attachment path in database
    if (task != null && task.getAttachmentPath() != null && !task.getAttachmentPath().isEmpty()) {
        if (attachmentsList != null) {
            attachmentsList.getItems().add("Attached: " + task.getAttachmentPath());
        }
    }
}

private void completeTask(Task task) {
    updateTaskStatus(task, "For Approval");
    
    // For students, prompt for attachment
    if ("STUDENT".equalsIgnoreCase(Session.getRole())) {
        showAttachmentDialog(task);
    }
}
```

---

## 8. TaskController.java - APPROVAL HANDLING (FOR HOSTS)

### Add Approval methods:
```java
private void handleApproveSelected() {
    Task selected = pendingApprovalsList.getSelectionModel().getSelectedItem();
    if (selected == null) return;
    
    approveTask(selected);
}

private void handleRejectSelected() {
    Task selected = pendingApprovalsList.getSelectionModel().getSelectedItem();
    if (selected == null) return;
    
    updateTaskStatus(selected, "Pending");
}

private void approveTask(Task task) {
    updateTaskStatus(task, "Done");
}

private void loadPendingApprovals() {
    // Load tasks with "For Approval" status for host review
    if (pendingApprovalsList != null) {
        pendingApprovalsList.setItems(filteredTasks);
        pendingApprovalsList.setCellFactory(param -> createTaskListCell());
    }
}
```

---

## 9. TaskScene.fxml - COMPLETE NEW STRUCTURE

### Key UI Elements to Add/Update:

#### A. Tab Buttons (Navigation):
```xml
<HBox spacing="10" alignment="CENTER_LEFT">
    <children>
        <Button fx:id="tabMyTasksBtn" onAction="#switchToMyTasksView" 
            style="-fx-background-color: white; -fx-text-fill: #718096; -fx-border-color: #e2e8f0; 
                   -fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 8 20; 
                   -fx-font-weight: 600; -fx-cursor: hand;" text="My Tasks" />
        <Button fx:id="tabApprovalsBtn" onAction="#switchToApprovalsView" 
            style="-fx-background-color: white; -fx-text-fill: #cbd5e0; -fx-border-color: #e2e8f0; 
                   -fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 8 20; 
                   -fx-font-weight: 600; -fx-cursor: default;" text="Pendings" />
    </children>
</HBox>
```

#### B. My Tasks View Container:
```xml
<VBox fx:id="myTasksView" spacing="20">
    <!-- Task input section with TextArea for description -->
    <TextArea fx:id="taskDescriptionArea" prefRowCount="3" 
        promptText="Task description (optional)..." 
        style="-fx-padding: 10; -fx-border-color: #e2e8f0; -fx-border-radius: 8; 
               -fx-background-radius: 8; -fx-background-color: #ffffff;" wrapText="true" />
    
    <!-- Lists for all and filtered tasks -->
    <ListView fx:id="allTaskList" VBox.vgrow="ALWAYS" />
    <ListView fx:id="filteredTaskList" VBox.vgrow="ALWAYS" />
    
    <!-- Task Details Section -->
    <TextArea fx:id="taskInstructions" prefRowCount="8" 
        promptText="Select a task to view details..." 
        style="-fx-padding: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 8; 
               -fx-background-radius: 8; -fx-background-color: #ffffff; -fx-wrap-text: true;" 
        wrapText="true" />
    
    <!-- Control Buttons -->
    <Button fx:id="startTimerBtn" onAction="#handleStartTimer" 
        style="-fx-background-color: #3182ce; -fx-text-fill: white; -fx-background-radius: 8; 
               -fx-padding: 12; -fx-font-weight: 600; -fx-cursor: hand;" 
        text="Start Focus Timer" HBox.hgrow="ALWAYS" />
    
    <Button fx:id="saveTaskInfoBtn" onAction="#handleSaveTaskInfo" 
        style="-fx-background-color: #edf2f7; -fx-text-fill: #4a5568; -fx-background-radius: 8; 
               -fx-padding: 10; -fx-font-weight: 600; -fx-cursor: hand;" 
        text="Save Notes" HBox.hgrow="ALWAYS" />
    
    <Button fx:id="markDoneBtn" onAction="#handleMarkDone" 
        style="-fx-background-color: #38a169; -fx-text-fill: white; -fx-background-radius: 8; 
               -fx-padding: 15; -fx-font-weight: bold; -fx-font-size: 14; -fx-cursor: hand;" 
        text="Mark as Done" HBox.hgrow="ALWAYS" />
    
    <Button fx:id="uploadAttachmentBtn" onAction="#handleUploadAttachment" 
        style="-fx-background-color: white; -fx-text-fill: #3182ce; -fx-border-color: #3182ce; 
               -fx-background-radius: 8; -fx-padding: 12; -fx-font-weight: 600; -fx-cursor: hand;" 
        text="Upload Attachment" HBox.hgrow="ALWAYS" />
    
    <Button fx:id="deleteTaskBtn" onAction="#handleDeleteTask" 
        style="-fx-background-color: #fff5f5; -fx-text-fill: #e53e3e; -fx-border-color: #e53e3e; 
               -fx-background-radius: 8; -fx-padding: 12; -fx-font-weight: 600; -fx-cursor: hand;" 
        text="Delete Task" HBox.hgrow="ALWAYS" />
    
    <!-- Attachments List -->
    <ListView fx:id="attachmentsList" prefHeight="100" 
        style="-fx-background-color: #f7fafc; -fx-border-color: #edf2f7; -fx-background-radius: 8;" />
</VBox>
```

#### C. Pending Approvals View (for hosts/teachers):
```xml
<VBox fx:id="approvalsView" managed="false" spacing="20" visible="false">
    <Label text="Pending Task Approvals" />
    
    <ListView fx:id="pendingApprovalsList" prefHeight="400" 
        style="-fx-background-color: transparent; -fx-border-color: transparent;" VBox.vgrow="ALWAYS" />
    
    <HBox spacing="10">
        <Button fx:id="approveSelectedBtn" onAction="#handleApproveSelected" 
            style="-fx-background-color: #38a169; -fx-text-fill: white; -fx-background-radius: 8; 
                   -fx-padding: 12; -fx-font-weight: 600; -fx-cursor: hand;" 
            text="Approve Selected" HBox.hgrow="ALWAYS" />
        <Button fx:id="rejectSelectedBtn" onAction="#handleRejectSelected" 
            style="-fx-background-color: #e53e3e; -fx-text-fill: white; -fx-background-radius: 8; 
                   -fx-padding: 12; -fx-font-weight: 600; -fx-cursor: hand;" 
            text="Reject Selected" HBox.hgrow="ALWAYS" />
    </HBox>
</VBox>
```

---

## 10. Settings.fxml - NEW PROFILE MANAGEMENT SECTION

### Add to Settings.fxml under Account Settings:

```xml
<!-- Profile Picture Section -->
<HBox spacing="20" alignment="TOP_LEFT">
    <children>
        <VBox alignment="CENTER" spacing="10">
            <ImageView fx:id="profilePictureImageView" fitHeight="140" fitWidth="140" 
                pickOnBounds="true" preserveRatio="true" smooth="true"
                style="-fx-background-radius: 100; -fx-background-color: #ecf0f1;" />
        </VBox>
        <VBox spacing="10" HBox.hgrow="ALWAYS">
            <children>
                <Button fx:id="uploadPictureButton" text="Upload New Avatar" onAction="#handleUploadPicture" 
                    style="-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 8 15; 
                           -fx-background-radius: 5;" />
                <Label text="Accepted formats: JPG, PNG. Max size: 5MB" 
                    style="-fx-font-size: 11; -fx-text-fill: #7f8c8d;" wrapText="true" />
            </children>
        </VBox>
    </children>
</HBox>

<Separator />

<!-- Full Name Row -->
<HBox spacing="15" alignment="CENTER_LEFT">
    <children>
        <Label text="Full Name" style="-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #7f8c8d;" prefWidth="150" />
        <TextField fx:id="fullNameField" HBox.hgrow="ALWAYS" 
            style="-fx-padding: 8 10; -fx-border-radius: 5; -fx-background-radius: 5;" />
        <Button fx:id="editFullNameButton" text="Edit" onAction="#handleEditFullName" 
            style="-fx-background-color: #95a5a6; -fx-text-fill: white;" />
    </children>
</HBox>

<!-- Username Row -->
<HBox spacing="15" alignment="CENTER_LEFT">
    <children>
        <Label text="Username" style="-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #7f8c8d;" prefWidth="150" />
        <TextField fx:id="usernameField" HBox.hgrow="ALWAYS" 
            style="-fx-padding: 8 10; -fx-border-radius: 5; -fx-background-radius: 5;" />
        <Button fx:id="editUsernameButton" text="Edit" onAction="#handleEditUsername" 
            style="-fx-background-color: #95a5a6; -fx-text-fill: white;" />
    </children>
</HBox>

<!-- Email Row -->
<HBox spacing="15" alignment="CENTER_LEFT">
    <children>
        <Label text="Email Address" style="-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #7f8c8d;" prefWidth="150" />
        <TextField fx:id="emailField" HBox.hgrow="ALWAYS" 
            style="-fx-padding: 8 10; -fx-border-radius: 5; -fx-background-radius: 5;" />
        <Button fx:id="editEmailButton" text="Edit" onAction="#handleEditEmail" 
            style="-fx-background-color: #95a5a6; -fx-text-fill: white;" />
    </children>
</HBox>

<!-- Password Row -->
<HBox spacing="15" alignment="CENTER_LEFT">
    <children>
        <Label text="Password" style="-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #7f8c8d;" prefWidth="150" />
        <PasswordField fx:id="passwordField" HBox.hgrow="ALWAYS" 
            promptText="●●●●●●●●" editable="false" 
            style="-fx-padding: 8 10; -fx-border-radius: 5; -fx-background-radius: 5;" />
        <Button fx:id="changePasswordButton" text="Change" onAction="#handleChangePassword" 
            style="-fx-background-color: #95a5a6; -fx-text-fill: white;" />
    </children>
</HBox>
```

### Add Sounds Section in Settings.fxml:

```xml
<!-- Sounds / Audio Section -->
<VBox spacing="12" styleClass="card">
    <children>
        <Label text="Audio Settings" styleClass="heading-3" />
        
        <!-- Background Music Toggle -->
        <HBox spacing="15" alignment="CENTER_LEFT">
            <children>
                <Label text="Background Music" styleClass="body" HBox.hgrow="ALWAYS" />
                <ToggleButton fx:id="enableMusicToggle" onAction="#toggleMusic" styleClass="switch">
                    <graphic>
                        <StackPane>
                            <Rectangle fx:id="musicTrack" width="48" height="24" arcWidth="999" arcHeight="999" 
                                style="-fx-fill: rgba(148,163,184,0.4);" />
                            <Circle fx:id="musicThumb" styleClass="thumb" radius="10" translateX="-12"/>
                        </StackPane>
                    </graphic>
                </ToggleButton>
            </children>
        </HBox>
        
        <!-- Sound Effects Toggle -->
        <HBox spacing="15" alignment="CENTER_LEFT">
            <children>
                <Label text="Sound Effects (SFX)" styleClass="body" HBox.hgrow="ALWAYS" />
                <ToggleButton fx:id="enableSFXToggle" onAction="#toggleSfx" styleClass="switch">
                    <graphic>
                        <StackPane>
                            <Rectangle fx:id="sfxTrack" width="48" height="24" arcWidth="999" arcHeight="999" 
                                style="-fx-fill: rgba(148,163,184,0.4);" />
                            <Circle fx:id="sfxThumb" styleClass="thumb" radius="10" translateX="-12"/>
                        </StackPane>
                    </graphic>
                </ToggleButton>
            </children>
        </HBox>
    </children>
</VBox>
```

---

## 11. CSS ANIMATIONS - task.css

### Add hover animation for List Cells:
```css
/* LIST CELLS - WITH HOVER ANIMATION */
.list-cell {
    -fx-background-color: rgba(255,255,255,0.05);
    background-color: rgba(255,255,255,0.05);
    -fx-text-fill: white;
    -fx-background-radius: 10;
    -fx-padding: 10;
    padding: 10;
    -fx-margin: 5;
    margin: 5;
    
    /* Smooth transition for hover effects */
    -fx-transition: all 0.2s ease;
    transition: all 0.2s ease;
}

.list-cell:hover {
    -fx-background-color: rgba(255,255,255,0.1);
    background-color: rgba(255,255,255,0.1);
    -fx-translate-x: 3;
}

/* SELECTED CELL - HIGHLIGHT */
.list-cell:selected {
    -fx-background-color: linear-gradient(to right, #6366f1, #8b5cf6);
    background-color: linear-gradient(to right, #6366f1, #8b5cf6);
}
```

### Add Card hover animation:
```css
.card:hover {
    -fx-background-color: rgba(255,255,255,0.08);
    background-color: rgba(255,255,255,0.08);
    -fx-translate-y: -2;
    transition: all 0.3s ease;
}
```

### Add Button hover scale animation:
```css
/* BUTTON HOVER ANIMATION - SCALE */
.button:hover {
    -fx-background-color: linear-gradient(to right, #7c83ff, #a78bfa);
    background-color: linear-gradient(to right, #7c83ff, #a78bfa);
    -fx-scale-x: 1.05;
    -fx-scale-y: 1.05;
    transition: all 0.2s ease;
}

/* SUCCESS BUTTON HOVER */
.success-btn:hover {
    -fx-background-color: linear-gradient(to right, #4ade80, #22c55e);
    background-color: linear-gradient(to right, #4ade80, #22c55e);
    -fx-scale-x: 1.03;
    -fx-scale-y: 1.03;
}

/* DANGER BUTTON HOVER */
.danger-btn:hover {
    -fx-background-color: linear-gradient(to right, #f87171, #ef4444);
    background-color: linear-gradient(to right, #f87171, #ef4444);
    -fx-scale-x: 1.03;
    -fx-scale-y: 1.03;
}
```

### Add Tab Button styling:
```css
/* TAB BUTTONS */
.tab-button {
    -fx-background-color: white;
    background-color: white;
    -fx-text-fill: #718096;
    -fx-border-color: #e2e8f0;
    -fx-background-radius: 20;
    -fx-border-radius: 20;
    -fx-padding: 8 20;
    -fx-font-weight: 600;
    -fx-cursor: hand;
    cursor: pointer;
    
    -fx-transition: all 0.3s ease;
    transition: all 0.3s ease;
}

.tab-button:hover {
    -fx-background-color: #f0f4f8;
    background-color: #f0f4f8;
    -fx-translate-y: -1;
}

.tab-button-active {
    -fx-background-color: #3182ce;
    background-color: #3182ce;
    -fx-text-fill: white;
    -fx-border-color: #3182ce;
}
```

---

## 12. IMPLEMENTATION CHECKLIST

### For TaskController.java:
- [ ] Add TimerService import
- [ ] Add `implements TimerService.TimerListener` to class declaration
- [ ] Add all new FXML fields (@FXML annotations)
- [ ] Add `timerService` instance variable
- [ ] Update `initialize()` method with timer initialization
- [ ] Add `switchToMyTasksView()` method
- [ ] Add `switchToApprovalsView()` method
- [ ] Add `updateTabStyles()` method
- [ ] Update `setupRoleBasedUI()` method
- [ ] Add `handleStartTimer()` method
- [ ] Add `handleUploadAttachment()` method
- [ ] Add `handleSaveTaskInfo()` method
- [ ] Add `showAttachmentDialog()` method
- [ ] Update `completeTask()` method to show attachment dialog
- [ ] Add `handleApproveSelected()` method
- [ ] Add `handleRejectSelected()` method
- [ ] Add `approveTask()` method
- [ ] Add `loadPendingApprovals()` method
- [ ] Add `getStartTimerBtn()` and `setStartTimerBtn()` methods

### For TaskScene.fxml:
- [ ] Add TextArea import: `<?import javafx.scene.control.TextArea?>`
- [ ] Add tab navigation buttons (tabMyTasksBtn, tabApprovalsBtn)
- [ ] Add myTasksView container with all controls
- [ ] Add approvalsView container with approval buttons
- [ ] Rename/update existing button IDs to match new structure

### For Settings.fxml:
- [ ] Add ImageView field for profile picture
- [ ] Add profile picture upload button
- [ ] Add full name, username, email edit fields
- [ ] Add password change button
- [ ] Add ToggleButton imports for audio settings
- [ ] Add music and SFX toggle buttons with custom graphics
- [ ] Add Rectangle and Circle imports for custom toggle design

### For task.css:
- [ ] Add list-cell hover animation styles
- [ ] Add card hover animation
- [ ] Add button scale animation on hover
- [ ] Add tab-button styling
- [ ] Update transitions for smooth animations
- [ ] Add success and danger button hover effects

---

## 13. DATABASE SCHEMA UPDATES NEEDED

The following columns should already exist or need to be added:

```sql
-- If not already present, add these columns to tasks table:
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS attachment_path VARCHAR(255);
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS pending_points INT DEFAULT 0;

-- Update status enum to include 'pending_approval' if using enum
-- Current values: 'Pending', 'For Approval', 'Done'
```

---

## SUMMARY OF CHANGES

| Component | Type | Addition | Impact |
|-----------|------|----------|--------|
| TaskController.java | Java | ~300 lines | Logic for tabs, attachments, approvals, timer |
| TaskScene.fxml | FXML | ~100 lines | New tab UI, TextArea fields, approval section |
| Settings.fxml | FXML | ~80 lines | Profile picture, audio toggles |
| task.css | CSS | ~50 lines | Hover animations, transitions |
| Database | SQL | Optional | Attachment path & description columns |

---

## TESTING CHECKLIST

- [ ] Tab switching works (My Tasks ↔ Pendings)
- [ ] Task attachment upload/display works
- [ ] Save Notes functionality persists data
- [ ] Start Timer button connects to TimerService
- [ ] Approval buttons visible only for hosts/teachers
- [ ] Approve/Reject functionality updates status
- [ ] CSS animations smooth on hover
- [ ] Profile picture upload displays correctly
- [ ] Audio toggles save preferences
- [ ] All database queries execute without errors

---

**End of Analysis Document**
