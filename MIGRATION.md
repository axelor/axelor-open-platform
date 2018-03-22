# Migration Guide

This migration guide will help you to migrate your applications from
older version to more recent version.

## Migrating from 4.x to 5.x

Migrating your v4.x apps to 5.0 should be easy as there very few breaking changes.

### Prerequisite

* Requires JDK 8
* Requires Tomcat 8.5
* Requires PostgreSQL 9.4 or greater

### Update build scripts

ADK 5.0 uses Gradle 4.4.1 and there are some major changes with gradle build scripts.

The application build script now looks like this:

```gradle
buildscript {
	ext.repos = {
		jcenter()
		mavenCentral()
		mavenLocal()
		maven { url 'https://plugins.gradle.org/m2/' }
		maven { url 'https://repository.axelor.com/nexus/public/' }
	}
	repositories repos
	dependencies {
		classpath "com.axelor:axelor-gradle:5.0.0-SNAPSHOT"
	}
}

allprojects {
	repositories repos
}

apply plugin: 'idea'
apply plugin: 'eclipse'
apply plugin: "com.axelor.app"

axelor {
	title = "My App"
}

allprojects {
	apply plugin: 'idea'
	apply plugin: 'eclipse'

	group = "com.axelor"
	version = "1.0.0"

	sourceCompatibility = 1.8
	targetCompatibility = 1.8
}

dependencies {
	// add module dependencies
	compile project(":modules:my-module-1")
	compile project(":modules:my-module-2")
}
```

The `application { }` extension is replaced with `axelor { }` and
supports following properties:

* `title` - display name of the app
* `description` - short description for the app
* `install` - list of modules to install by default

The old `module` property is removed as module dependencies are now handled
as normal gradle dependencies.

The old `axelor-app` plugin is renamed to `com.axelor.app`.

The app now doesn't require ADK installation, it will fetch core dependencies
from our nexus repository.

The module build scripts now looks like this:

```gradle
apply plugin: 'com.axelor.app-module'

axelor {
	title = "My Module 1"
}

dependencies {
	compile project(":modules:my-module-2")
}
```

The plugin `axelor-module` is renamed to `com.axelor.app-module`.

The `application { }` extension is replaced with `axelor { }` and
supports following properties:

* `title` - display name of the module
* `description` - short description of the module
* `removable` - whether this module is removable

The old `name` property is removed, the directory name is module name.

The old `module` property is removed as module dependencies are now handled
as normal gradle dependencies.

Once you update your build scripts accordingly, you have to update gradle
wrapper.

First, change the `gradle/wrapper/gradle-wrapper.properties` file and
change `distributionUrl` to `https\://services.gradle.org/distributions/gradle-4.4.1-bin.zip`.

And run following command:

```sh
$ ./gradlew wrapper --gradle-version=4.4.1 --distribution-type=bin
```

Some gradle tasks are either removed or renamed:

* The `i18n-extract` and `i18n-update` tasks are merged in a single `i18n` task
* The `migrate` and `init` tasks are replaced with `database` task
* The `tomcatRun` task is replaced with `run` task

### Update XML definitions

Run following gradle task to update XML definitions with new XSD versions.

```sh
$ ./gradlew updateVersion
```

### Update persistence.xml

Since 5.0, we can configure Hibernate with `application.properties` by adding
hibernate properties with `hibernate.` prefix. So the use of `persistence.xml`
is only to comply with JPA requirements.

The `persistence.xml` can be minimal. However, you can still provide extra
properties from it.

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<persistence version="2.1"
  xmlns="http://xmlns.jcp.org/xml/ns/persistence"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/persistence http://xmlns.jcp.org/xml/ns/persistence/persistence_2_1.xsd">
  <persistence-unit name="persistenceUnit"
    transaction-type="RESOURCE_LOCAL">
    <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>
    <shared-cache-mode>ENABLE_SELECTIVE</shared-cache-mode>
  </persistence-unit>
</persistence>
```

HikariCP is now the default connection pool library.

The `hibernate.dialect` property should not be set as we now use custom dialects
and they will be detected automatically.

### Update Logging

We have dropped log4j and uses logback as logging framework.

* remove `log4j.properties`

The logger settings can be provided from `application.properties` using
`logging.` as key prefix.

For example:

```properties
# Global logging
logging.level.root = ERROR

# Debug axelor api.
logging.level.com.axelor = DEBUG

# Log everything. Good for troubleshooting
logging.level.org.hibernate = INFO

# Log all SQL DML statements as they are executed
logging.level.org.hibernate.SQL = DEBUG
logging.level.org.hibernate.engine.jdbc = DEBUG
```

See documentation for more details.

If you prefer separate `logback.xml` (for example, for testing code), you can
use following minimal configuration:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xml>
<configuration>

  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>

  <logger name="com.axelor" level="debug" />
  <logger name="org.hibernate.SQL" level="info" />
  <logger name="org.hibernate.tool.hbm2ddl" level="warn" />
  <logger name="com.zaxxer.hikari" level="info" />

  <root level="error">
    <appender-ref ref="STDOUT" />
  </root>
</configuration>
```

Put this file inside `resources` folder.

### Database Changes

* `auth_permission.condition_value` column size changed from `255` to `1024`
* `mail_group` table dropped
* `mail_group_users` table dropped
* `mail_group_groups` table dropped
* `meta_module.depends` column dropped
* `meta_translation.message_key` column type changed from `text` to `varchar(1024)`
* `meta_translation.message_value` column type changed from `text` to `varchar(1024)`

### Joda-Time to Java Time

* `org.joda.time.LocalDate` -> `java.time.LocalDate`
* `org.joda.time.LocalDateTime` -> `java.time.LocalDateTime`
* `org.joda.time.LocalTime` -> `java.time.LocalTime`
* `org.joda.time.DateTime` -> `java.time.ZonedDateTime`

Please check java time api documentation for joda-time equivalent api.

### API Changes

* The joda-time is dropped in favour or java.time api
* The `Context.asType(Class)` now returns lazy loading proxy instance
* The scripting helper `__repo__.of()` is changed to `__repo__()`
* The mail groups are replaced with team (see basic teams feature)
