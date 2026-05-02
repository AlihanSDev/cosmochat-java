module cosmochat {
    requires javafx.controls;
    requires javafx.graphics;
    requires java.net.http;
    requires com.google.gson;
    requires java.sql;
    requires org.xerial.sqlitejdbc;
    
    opens cosmochat to javafx.graphics, javafx.controls;
    opens cosmochat.database to java.sql;
    opens cosmochat.model to java.sql;
    opens cosmochat.security to java.sql;
    opens cosmochat.auth to java.sql;
}