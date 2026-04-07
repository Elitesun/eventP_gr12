FROM maven:3.9.9-eclipse-temurin-11 AS build
WORKDIR /build

COPY pom.xml ./
COPY src ./src
RUN mvn -B -DskipTests clean package

FROM payara/server-full:latest

USER root

# Deploy application and JDBC driver.
COPY --from=build /build/target/eventProjectGlsi-1.0-SNAPSHOT.war /opt/payara/deployments/eventProjectGlsi.war
COPY --from=build /root/.m2/repository/org/postgresql/postgresql/42.7.3/postgresql-42.7.3.jar /opt/payara/appserver/glassfish/domains/domain1/lib/postgresql-42.7.3.jar
COPY docker/payara/postboot.asadmin /opt/payara/config/postboot.asadmin

ENV DB_HOST=db
ENV DB_PORT=5432
ENV DB_NAME=event_platform
ENV DB_USER=eventuser
ENV DB_PASSWORD=eventpass123
ENV POSTBOOT_COMMANDS=/opt/payara/config/postboot.asadmin

USER payara
