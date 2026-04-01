FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml ./
COPY kanbin-projest-model ./kanbin-projest-model
COPY kanbin-projest-repo-mem ./kanbin-projest-repo-mem
COPY kanbin-projest-app ./kanbin-projest-app
COPY kanbin-projest-reporting ./kanbin-projest-reporting

RUN mvn -B -pl kanbin-projest-app -am -DskipTests package

FROM jetty:11.0-jre21

COPY --from=build /app/kanbin-projest-app/target/kanbin-projest-app-*.war /var/lib/jetty/webapps/ROOT.war

EXPOSE 8080