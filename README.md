# Chat Application

Multi-module Java chat application with:

- `chat-common`: shared message DTOs and JSON serialization
- `chat-server`: Spring Boot + WebSocket + SQLite server
- `chat-client`: JavaFX desktop client

## Build

```bash
mvn clean package
```

## Run Server

```bash
mvn -q -DskipTests install
mvn -pl chat-server spring-boot:run
```

## Run Client

```bash
mvn -q -DskipTests install
mvn -pl chat-client javafx:run
```
