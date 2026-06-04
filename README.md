# video_convert_platform

A self-hosted medical video archiving and transcoding platform built with
Spring Boot, FFmpeg, MySQL, RabbitMQ, and Prometheus.

## Public Repository Scope

This repository contains only the public, generic implementation of a video
archiving and transcoding platform.

Deployment-specific database schemas, production migrations, internal
integrations, credentials, hostnames, private storage paths, production data,
and company-specific operational details are intentionally excluded.

Do not submit real patient data, production credentials, internal network
addresses, private domains, confidential webhook payloads, or business-specific
configuration to this repository.

## Features

- Large video upload through multipart HTTP APIs
- Chunked upload and merge for large source files
- MD5 calculation and archive metadata persistence
- FFmpeg-based validation, transcoding, and HLS slicing
- Original video archiving to NFS or local archive storage
- RabbitMQ-driven asynchronous video task processing
- Project-level archive roots and optional outbound webhook notification
- Play URL query APIs for task and business identifiers
- Prometheus metrics and Spring Boot Actuator endpoints
- Scheduled dry-run cleanup of orphan chunk and original files

## Architecture

```text
Client / MQ Producer
  -> Spring Boot API
  -> MySQL
  -> NFS / local archive storage
  -> FFmpeg processing
  -> HLS output
  -> Optional webhook notification
  -> Prometheus / Actuator
```

The application keeps the original video under an archive root, creates
low-quality and standard-quality HLS outputs, records archive files in MySQL,
and exposes playback URLs through the API.

## Requirements

- Java 8+
- Maven
- MySQL 8 or compatible MySQL server
- RabbitMQ, required when `mq.enabled=true`
- FFmpeg available on `PATH`, or configured through
  `video.processing.ffmpeg.executable-path`
- Writable archive root, for example `/tmp/video-nfs/dev` in local development

## Profiles

| Profile | Default port | MQ default | Archive root default |
| --- | ---: | --- | --- |
| `dev` | `8080` | disabled | `/tmp/video-nfs/dev` |
| `test` | `9092` | enabled | `/tmp/video-nfs/test` |
| `prod` | `9091` | disabled | `/tmp/video-nfs/prod` |

`test` is the default active profile in `application.yml`. For local manual
testing, prefer the `dev` profile unless you have MySQL and RabbitMQ prepared.

## Configuration

Set configuration through environment variables or standard Spring Boot
configuration overrides:

| Variable | Purpose | Example |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Runtime profile | `dev` |
| `DB_URL` | Full JDBC URL | `jdbc:mysql://localhost:3306/video_dev?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true` |
| `DB_USERNAME` | MySQL username | `video_app` |
| `DB_PASSWORD` | MySQL password | `replace-with-local-password` |
| `RABBITMQ_HOST` | RabbitMQ host | `localhost` |
| `RABBITMQ_PORT` | RabbitMQ port | `5672` |
| `RABBITMQ_USERNAME` | RabbitMQ username | `rabbitmq_app` |
| `RABBITMQ_PASSWORD` | RabbitMQ password | `replace-with-local-password` |
| `RABBITMQ_VHOST` | RabbitMQ virtual host | `/` |
| `MQ_ENABLED` | Enable MQ consumer | `false` |
| `MQ_VIDEO_TASK_QUEUE` | Video task queue name | `video.task.queue` |
| `NFS_ROOT_PATH` | Archive root path | `/tmp/video-nfs/dev` |
| `NFS_BASE_URL` | Base URL for playback links | `http://localhost:8080/files` |
| `VIDEO_TEMP_DIR` | FFmpeg temporary directory | `/tmp/video-processing` |
| `WEBHOOK_CALLBACK_ENABLED` | Enable optional outbound webhook notification | `false` |

Real database schema and migration files are intentionally not packaged in this
public repository. Bring your own development schema for local experiments, and
do not publish production DDL, migrated data, or company-specific data model
details here.

## Quick Start

```bash
export SPRING_PROFILES_ACTIVE=dev
export DB_URL='jdbc:mysql://localhost:3306/video_dev?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true'
export DB_USERNAME='video_app'
export DB_PASSWORD='replace-with-local-password'
export MQ_ENABLED=false
export NFS_ROOT_PATH=/tmp/video-nfs/dev
export NFS_BASE_URL=http://localhost:8080/files

mkdir -p "$NFS_ROOT_PATH"
mvn clean test
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Health and metrics endpoints:

```text
GET http://localhost:8080/actuator/health
GET http://localhost:8080/actuator/prometheus
```

## API Overview

Create a project archive configuration:

```http
POST /project
Content-Type: application/json

{
  "projectNo": "PROJ001",
  "projectName": "Demo Project",
  "archiveRoot": "/tmp/video-nfs/dev"
}
```

Upload a complete video:

```http
POST /api/v1/videos/upload
Content-Type: multipart/form-data

file=<video file>
projectNo=PROJ001
patientCode=SUBJ001
tpStage=V1
```

Upload a chunk:

```http
POST /api/v1/videos/upload/chunk
Content-Type: multipart/form-data

file=<chunk file>
projectNo=PROJ001
patientCode=SUBJ001
tpStage=V1
filename=source.mp4
uuid=<upload session uuid>
chunk=0
chunks=10
visit=optional-visit-label
```

Query task status and playback URLs:

```text
GET /api/v1/videos/tasks/{taskId}
GET /api/v1/videos/tasks/{taskId}/play-urls
GET /api/v1/videos/play-urls?projectNo=PROJ001&patientCode=SUBJ001&tpStage=V1
GET /api/v1/videos/play-urls?projectNo=PROJ001&patientCode=SUBJ001&visit=Screening
```

The playback URL query accepts either `tpStage` or `visit`, but not both.

Monitoring APIs:

```text
GET /api/v1/monitor/overview
GET /api/v1/monitor/project-stats?projectNo=PROJ001
GET /api/v1/monitor/performance
```

## MQ Task Payload

When `mq.enabled=true`, the consumer listens on `mq.queues.video-task`.

```json
{
  "projectNo": "PROJ001",
  "patientCode": "SUBJ001",
  "tpStage": "V1",
  "visit": "Screening",
  "checkDate": "2026-01-01",
  "filePath": "/tmp/video-inbox/source.mp4",
  "fileMd5": "optional-md5"
}
```

`projectNo` and `filePath` are required. The service copies the source file into
the configured archive root before processing it.

## Optional Webhook Notification

Outbound webhook notification is disabled by default. Set
`WEBHOOK_CALLBACK_ENABLED=true` and configure a project-level callback URL only
when your deployment needs to notify another system after processing completes.

Keep webhook payloads generic. Do not commit production callback URLs,
company-specific payload contracts, credentials, or patient data.

## Processing Flow

```text
1. Receive a full upload, final chunk, or MQ task.
2. Archive the original file under the project archive root.
3. Validate and normalize the source video with FFmpeg when needed.
4. Generate low and standard quality outputs.
5. Slice each output into HLS files under slice_low and slice_standard.
6. Persist archive metadata and playback URLs.
7. Publish completion events; send optional webhook only when explicitly enabled
   and configured.
8. Record logs, failures, and metrics for operations.
```

## Operations

- Actuator exposes `health`, `info`, `metrics`, and `prometheus`.
- Cleanup runs on `maintenance.cleanup.cron`.
- Cleanup defaults to `dry-run=true`; turn it off only after reviewing logs.
- The `prod` profile uses safe local defaults in this public repository; provide
  real deployment values through environment variables outside Git.
- Set `MQ_ENABLED=true` explicitly when a deployment should consume RabbitMQ
  tasks.
- Set `WEBHOOK_CALLBACK_ENABLED=true` explicitly when a deployment should send
  outbound webhook notifications.
- Development profile stores relative playback URLs and builds full URLs at read
  time using `nfs.base-url`.

## Tests

```bash
mvn clean test
```

The Maven build includes Surefire unit tests, JaCoCo coverage reporting, SpotBugs,
PMD, and Failsafe integration-test wiring.
