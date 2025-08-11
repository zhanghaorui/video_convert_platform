# Video Convert Platform

Skeleton for a medical video archiving and conversion service.

Requires Java 8.

Provides a unified `ApiResponse` wrapper and global exception handling for consistent REST outputs.

## Build

```bash
mvn -q -Djava.net.preferIPv4Stack=true package
```

The packaged JAR can be found under `target/`.

## Hardware Acceleration

macOS servers can leverage Apple's VideoToolbox for faster transcoding. Enable it via configuration:

```yaml
video:
  processing:
    ffmpeg:
      use-videotoolbox: true
```

With this flag FFmpeg will use `h264_videotoolbox` when available.
