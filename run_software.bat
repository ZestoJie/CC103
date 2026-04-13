@echo off
cd /d "c:\Users\rhejie carl\OneDrive\Documents\NetBeansProjects\CC103_Client"
java -Dprism.order=sw -cp "target\classes;C:\Users\rhejie carl\.m2\repository\mysql\mysql-connector-java\8.0.33\mysql-connector-java-8.0.33.jar;C:\Program Files\Java\javafx-sdk-21.0.10\lib\javafx-controls.jar;C:\Program Files\Java\javafx-sdk-21.0.10\lib\javafx-fxml.jar;C:\Program Files\Java\javafx-sdk-21.0.10\lib\javafx-graphics.jar;C:\Program Files\Java\javafx-sdk-21.0.10\lib\javafx-base.jar" com.cc103sys.cc103.App
pause