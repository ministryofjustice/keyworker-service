plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "1.0.6"
  kotlin("plugin.spring") version "1.4.10"
  kotlin("plugin.jpa") version "1.4.10"
}

configurations {
  implementation { exclude(mapOf("module" to "tomcat-jdbc")) }
}

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
  annotationProcessor("org.projectlombok:lombok:1.18.12")
  testAnnotationProcessor("org.projectlombok:lombok:1.18.12")

  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-cache")
  implementation("org.springframework.boot:spring-boot-starter-quartz")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

  implementation("org.springframework.security:spring-security-oauth2-jose")
  implementation("org.springframework:spring-webflux")
  implementation("org.springframework.boot:spring-boot-starter-reactor-netty")

  implementation("org.apache.camel:camel-spring-boot:2.25.1")
  implementation("org.apache.camel:camel-quartz2:2.25.1")

  implementation("org.springframework:spring-jms")
  implementation("com.amazonaws:amazon-sqs-java-messaging-lib:1.0.8")

  implementation("javax.annotation:javax.annotation-api:1.3.2")
  implementation("javax.xml.bind:jaxb-api:2.3.1")
  implementation("com.sun.xml.bind:jaxb-impl:2.3.3")
  implementation("com.sun.xml.bind:jaxb-core:2.3.0.1")
  implementation("javax.activation:activation:1.1.1")
  implementation("javax.transaction:javax.transaction-api:1.3")

  implementation("io.springfox:springfox-boot-starter:3.0.0")
/*  implementation("io.springfox:springfox-swagger2:3.0.0")
  implementation("io.springfox:springfox-swagger-ui:3.0.0")*/

  implementation("net.sf.ehcache:ehcache:2.10.6")
  implementation("org.apache.commons:commons-text:1.9")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.11.2")

  implementation("com.google.code.gson:gson:2.8.6")

  compileOnly("org.projectlombok:lombok:1.18.12")

  runtimeOnly("org.hsqldb:hsqldb:2.5.1")
  runtimeOnly("org.flywaydb:flyway-core:6.5.6")
  runtimeOnly("org.postgresql:postgresql:42.2.16")

  testImplementation("org.codehaus.groovy:groovy-all:3.0.6")
  testImplementation("org.spockframework:spock-spring:2.0-M3-groovy-3.0")
  testCompile("org.spockframework:spock-core:2.0-M3-groovy-3.0") {
    exclude("org.codehaus.groovy")
  }

  testCompileOnly("org.projectlombok:lombok:1.18.12")
  testImplementation("org.gebish:geb-core:3.4")
  testImplementation("org.gebish:geb-spock:3.4")
  testImplementation("org.seleniumhq.selenium:selenium-support:3.141.59")
  testImplementation("org.seleniumhq.selenium:selenium-chrome-driver:3.141.59")
  testImplementation("org.seleniumhq.selenium:selenium-firefox-driver:3.141.59")
  testImplementation("io.github.http-builder-ng:http-builder-ng-apache:1.0.4")

  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
  testImplementation("com.github.tomjankes:wiremock-groovy:0.2.0")
  testImplementation("org.apache.camel:camel-test-spring:2.25.0")
  testImplementation("com.nhaarman:mockito-kotlin-kt1.1:1.6.0")
  testImplementation("org.testcontainers:localstack:1.14.3")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.19.0")
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
  testImplementation("org.springframework.security:spring-security-test")
}

dependencyCheck {
  suppressionFiles.add("suppressions.xml")
}
