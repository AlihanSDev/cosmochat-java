  module cosmochat {
      requires javafx.controls;
      requires javafx.graphics;
      requires javafx.web;
      requires java.net.http;
      requires com.google.gson;
      requires java.sql;
      requires org.postgresql.jdbc;

     // Domain layer — pure entities, value objects, ports (interfaces)
     opens cosmochat.domain to javafx.graphics;
     opens cosmochat.domain.port to javafx.graphics;

     // Application layer — DTOs, ports (interfaces), services, mappers
     opens cosmochat.application.service to javafx.graphics;
     opens cosmochat.application.port to javafx.graphics;
     opens cosmochat.application.dto to javafx.graphics;
     opens cosmochat.application.mapper to javafx.graphics;

     // Infrastructure — adapters
     opens cosmochat.infrastructure to javafx.graphics;
     opens cosmochat.infrastructure.adapter to javafx.graphics;

     // Presentation — JavaFX UI
     opens cosmochat to javafx.graphics, javafx.controls;
     opens cosmochat.auth to javafx.graphics;
     opens cosmochat.database to java.sql;
     opens cosmochat.security to java.sql;
 }
