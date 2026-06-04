# Security Policy

## Scope

This public repository contains the generic source code for a video archiving
and transcoding platform. Deployment-specific configuration, production
database schemas, private integrations, real credentials, production storage
paths, and patient data are out of scope and must not be committed.

## Reporting Vulnerabilities

Please do not open public GitHub issues for vulnerabilities.

Use GitHub private vulnerability reporting for this repository if it is enabled.
If private reporting is unavailable, contact the repository maintainers through a
private channel before sharing technical details or proof-of-concept material.

Include the affected version or commit, a clear impact description, reproduction
steps, and any relevant logs with secrets removed.

## Supported Versions

Security fixes are handled on the default branch unless maintainers explicitly
document a supported release branch. If you maintain a private fork or deployment,
backport fixes according to your own release process.

## Secrets and Configuration Policy

Do not commit real secrets or environment-specific company configuration.

The repository must not contain real database credentials, RabbitMQ credentials,
JWT or API secrets, access keys, internal IP addresses, private domains,
production webhook or callback URLs, production storage paths, or patient data.

Use environment variables for deployment-specific values. Public examples should
use safe placeholders such as `example.com`, `video.example.com`, `PROJ001`,
`SUBJ001`, `/tmp/video-nfs/dev`, `${DB_URL}`, and `${RABBITMQ_HOST}`.

If a secret or sensitive endpoint is committed, rotate the credential or endpoint
immediately and treat Git history cleanup as a separate incident-response task.

## Patient and Clinical Data

This public repository is for source code only. Do not submit real patient
identifiers, visit identifiers, video files, metadata exports, webhook payloads,
or any other protected health information.

## Optional Webhooks

Outbound webhook notification is optional and disabled by default. Do not commit
production webhook URLs, signing secrets, private payload contracts, or examples
derived from an internal integration. Keep public examples generic and safe.

## Before Publishing Changes

Before opening a pull request, scan changed files for secrets, private endpoints,
absolute production paths, real database names, and realistic patient or clinical
sample data. If sensitive data was ever committed, rotate affected credentials
and handle Git history cleanup separately from ordinary code review.
