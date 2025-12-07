FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY kanbin-projest-app/target/*.war app.war

ENTRYPOINT ["java", "-jar", "app.war"]