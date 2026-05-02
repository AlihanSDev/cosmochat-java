module cosmochat {
    requires javafx.controls;
    requires javafx.graphics;
    requires java.net.http;
    requires com.google.gson;
    
    exports cosmochat;
    opens cosmochat to javafx.graphics, javafx.controls;
}