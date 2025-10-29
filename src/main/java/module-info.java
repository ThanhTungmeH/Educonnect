module org.example.educonnect1 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jbcrypt;
    requires javax.mail.api;
    requires cloudinary.core;

    opens org.example.educonnect1.client.controllers to javafx.fxml;
    opens org.example.educonnect1 to javafx.fxml;
    exports org.example.educonnect1;
}

