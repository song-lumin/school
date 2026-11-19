# 构建阶段：Maven + JDK 17
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY settings.xml .
RUN mvn dependency:go-offline -q -s settings.xml
COPY src ./src
RUN mvn clean package -DskipTests -q -s settings.xml

# 运行阶段：JRE 17
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENV TZ=Asia/Shanghai
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
