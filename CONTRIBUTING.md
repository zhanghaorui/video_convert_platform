# Contributing

Thanks for helping improve this generic open-source implementation. Keep
contributions focused on reusable video archiving, transcoding, HLS generation,
metadata persistence, monitoring, and local development workflows.

## Local Development

Use safe local values only. Do not commit `.env` files, real credentials,
internal network addresses, private domains, production paths, patient data, or
company-specific configuration.

Typical local commands:

```bash
export SPRING_PROFILES_ACTIVE=dev
export MQ_ENABLED=false
export WEBHOOK_CALLBACK_ENABLED=false
export NFS_ROOT_PATH=/tmp/video-nfs/dev
mkdir -p "$NFS_ROOT_PATH"

mvn clean test
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Run quality checks locally when changing code:

```bash
mvn clean test
mvn pmd:check
mvn spotbugs:check
mvn jacoco:report
```

## Pull Request Expectations

- Keep changes focused and reversible.
- Preserve the generic open-source implementation; do not add company-specific
  deployment details or production integration payloads.
- Do not add real database schema or migration files.
- Do not infer, reconstruct, or publish production table structure.
- Do not commit real DB/RabbitMQ credentials, internal IP addresses, private
  domains, production paths, webhook URLs, patient identifiers, visit
  identifiers, or clinical sample data.
- Keep outbound webhook behavior optional and disabled by default unless the
  change is explicitly about webhook configuration.
- Update documentation when changing configuration, APIs, or operational
  behavior.
- Run `mvn clean test` before opening a PR and include the result in the PR
  description.

This project uses GitHub Actions for CI. The build includes Surefire tests,
JaCoCo coverage reporting, PMD, and SpotBugs checks.

## PR Checklist

- `mvn clean test` passes locally, or the reason it could not run is documented.
- New or changed configuration uses environment variables or safe placeholders.
- No schema, migration, seed data, production export, or patient data was added.
- Documentation was updated for user-visible behavior changes.
- Rollback impact is clear for any behavior change.
