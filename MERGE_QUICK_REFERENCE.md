# V19.6 Merge - Quick Reference Guide

## 🎯 FOUR KEY FILES TO UPDATE

---

## 1️⃣ TaskController.java - 20+ NEW METHODS & FIELDS

### New Imports:
```java
import com.cc103sys.cc103.Utils.TimerService;
```

### Key Fields to Add:
```java
private TimerService timerService;
@FXML private Button tabMyTasksBtn, tabApprovalsBtn;
@FXML private VBox myTasksView, approvalsView;
@FXML private TextArea taskDescriptionArea, taskInstructions;
@FXML private Button uploadAttachmentBtn, startTimerBtn, saveTaskInfoBtn;
@FXML private ListView<String> attachmentsList;
@FXML private ListView<Task> pendingApprovalsList;
@FXML private Button approveSelectedBtn, rejectSelectedBtn;
```

### Critical New Methods:
- `switchToMyTasksView()` - Show My Tasks tab
- `switchToApprovalsView()` - Show Pending Approvals (hosts only)
- `updateTabStyles(boolean)` - Update tab styling
- `handleStartTimer()` - Integrate with TimerService
- `handleUploadAttachment()` - File upload handler
- `handleSaveTaskInfo()` - Save notes to DB
- `showAttachmentDialog(Task)` - Display attachments
- `handleApproveSelected()` - Host approval action
- `handleRejectSelected()` - Host rejection action

---

## 2️⃣ TaskScene.fxml - NEW TAB STRUCTURE

### What's Added:
```xml
<!-- Tab navigation buttons -->
<Button fx:id="tabMyTasksBtn" onAction="#switchToMyTasksView" text="My Tasks" />
<Button fx:id="tabApprovalsBtn" onAction="#switchToApprovalsView" text="Pendings" />

<!-- Two separate views: myTasksView & approvalsView -->
<VBox fx:id="myTasksView">
  <TextArea fx:id="taskDescriptionArea" />
  <TextArea fx:id="taskInstructions" />
  <Button fx:id="uploadAttachmentBtn" text="Upload Attachment" />
  <Button fx:id="startTimerBtn" text="Start Focus Timer" />
  <Button fx:id="saveTaskInfoBtn" text="Save Notes" />
  <ListView fx:id="attachmentsList" />
</VBox>

<VBox fx:id="approvalsView" managed="false" visible="false">
  <ListView fx:id="pendingApprovalsList" />
  <Button fx:id="approveSelectedBtn" text="Approve Selected" />
  <Button fx:id="rejectSelectedBtn" text="Reject Selected" />
</VBox>
```

---

## 3️⃣ Settings.fxml - PROFILE MANAGEMENT & AUDIO

### New Fields/Controls:
```xml
<!-- Profile Picture -->
<ImageView fx:id="profilePictureImageView" fitHeight="140" fitWidth="140" />
<Button fx:id="uploadPictureButton" text="Upload New Avatar" />

<!-- Profile Editing -->
<TextField fx:id="fullNameField" />
<TextField fx:id="usernameField" />
<TextField fx:id="emailField" />
<PasswordField fx:id="passwordField" />

<!-- Audio Toggles -->
<ToggleButton fx:id="enableMusicToggle" onAction="#toggleMusic" />
<ToggleButton fx:id="enableSFXToggle" onAction="#toggleSfx" />
```

---

## 4️⃣ task.css - HOVER ANIMATIONS

### Key Animation Additions:
```css
/* List Cell Hover - Slide Right */
.list-cell:hover {
    -fx-background-color: rgba(255,255,255,0.1);
    -fx-translate-x: 3;
    transition: all 0.2s ease;
}

/* Button Hover - Scale Up */
.button:hover {
    -fx-scale-x: 1.05;
    -fx-scale-y: 1.05;
    transition: all 0.2s ease;
}

/* Card Hover - Lift & Brighten */
.card:hover {
    -fx-translate-y: -2;
    -fx-background-color: rgba(255,255,255,0.08);
}
```

---

## 🔄 MERGE WORKFLOW

### Step 1: Backup Current Branch
```bash
git checkout V13.3_merged
git branch backup_before_v19.6_merge
```

### Step 2: Make Changes
1. Update `TaskController.java` with all new methods
2. Update `TaskScene.fxml` with new tab structure
3. Update `Settings.fxml` with profile controls
4. Update `task.css` with animations

### Step 3: Test
```bash
mvn clean compile
mvn javafx:run
```

### Step 4: Commit
```bash
git add -A
git commit -m "Merge V19.6 features: attachments, task tabs, settings profile, animations"
```

---

## 📋 FEATURE BREAKDOWN

| Feature | File | Method | Status |
|---------|------|--------|--------|
| Tab Navigation | TaskScene.fxml | switchToMyTasksView() | ✅ New |
| Attachments Upload | TaskController.java | handleUploadAttachment() | ✅ New |
| Save Task Notes | TaskController.java | handleSaveTaskInfo() | ✅ New |
| Focus Timer | TaskController.java | handleStartTimer() | ✅ New |
| Approve Tasks | TaskController.java | handleApproveSelected() | ✅ New |
| Profile Picture | Settings.fxml | handleUploadPicture() | ✅ New |
| Audio Toggles | Settings.fxml | toggleMusic() / toggleSfx() | ✅ New |
| Hover Animations | task.css | .list-cell:hover | ✅ New |
| Button Scale | task.css | .button:hover | ✅ New |

---

## 🚫 IMPORTANT NOTES

1. **Role-Based UI**: Approval buttons only show for HOST/TEACHER roles
2. **Database Columns**: Ensure `attachment_path` and `description` columns exist in `tasks` table
3. **TimerService**: Already implemented in V19.6 Utils - just needs integration
4. **Backward Compatibility**: All new features are additive - no breaking changes

---

## 📚 FULL DOCUMENTATION

See: [MERGE_ANALYSIS_V19.6.md](MERGE_ANALYSIS_V19.6.md)

Contains:
- Complete code snippets for each method
- Full FXML structure for new UI elements
- CSS animation rules
- Implementation checklist
- Testing checklist
- Database schema updates

---

**Last Updated:** April 14, 2026
