module com.example {

    /* JavaFX */
    requires javafx.controls;
    requires javafx.fxml;

    /* JDBC / MySQL */
    requires java.sql;
    requires javafx.graphics;

    /* Open packages for FXML */
    opens com.cc103 to javafx.fxml;

    /* Export main package */
    exports com.cc103;
}
