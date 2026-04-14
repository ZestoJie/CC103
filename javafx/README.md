# JavaFX SDK Integration Instructions

JavaFX SDK cannot be automatically downloaded due to licensing and distribution restrictions. You must manually download the JavaFX SDK from the official website:

1. Go to https://gluonhq.com/products/javafx/
2. Download the JavaFX SDK for your operating system (Windows x64).
3. Extract the downloaded archive.
4. Move the extracted folder (e.g., javafx-sdk-21) into the `javafx` directory in your project workspace.

After you have placed the JavaFX SDK in the `javafx` folder, you can run your application using the following command:

```
"C:\Program Files\Java\jdk-21\bin\java.exe" --module-path "${workspaceFolder}/javafx/javafx-sdk-21/lib" --add-modules javafx.controls,javafx.fxml -cp "target/classes" com.cc103sys.cc103.App
```

Replace `javafx-sdk-21` with the actual folder name if it differs.

If you want, I can set up a VS Code launch configuration for you once the SDK is present.
