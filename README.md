# Video Convert Platform

Skeleton for a medical video archiving and conversion service.

Requires Java 8.

Provides a unified `ApiResponse` wrapper and global exception handling for consistent REST outputs.

## Build

```bash
mvn -q -Djava.net.preferIPv4Stack=true package
```

The packaged JAR can be found under `target/`.
