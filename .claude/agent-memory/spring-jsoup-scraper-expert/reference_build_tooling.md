---
name: Build tooling notes — mvnw wrapper is broken, use system mvn
description: The ./mvnw wrapper fails because .mvn/wrapper/maven-wrapper.properties is missing; fall back to system mvn.
type: reference
---

`./mvnw` in this repo errors with `cannot read distributionUrl property in ./.mvn/wrapper/maven-wrapper.properties` because the wrapper config file is missing. System `mvn` (Homebrew, Maven 3.9.x + JDK 21) compiles the project fine against Java 17 release target.

Use:
- `mvn -DskipTests compile`
- `mvn clean package`
- `mvn test -Dtest=ClassName#method`

When/if the wrapper is repaired, switch back to `./mvnw` as documented in CLAUDE.md.
