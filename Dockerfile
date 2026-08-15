# OrderFlow 后端运行镜像
# 前提：已通过 `mvn clean package -DskipTests` 在 target/ 生成 orderflow.jar
FROM eclipse-temurin:17-jre

WORKDIR /app
COPY target/*.jar app.jar

ENV JAVA_OPTS="-Xms256m -Xmx512m"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
