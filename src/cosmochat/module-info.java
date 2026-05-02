module cosmochat {
    requires javafx.controls;
    requires javafx.graphics;
    
    exports cosmochat;
    opens cosmochat to javafx.graphics, javafx.controls;
}