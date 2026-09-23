FROM docker.io/library/maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -q -B package

FROM docker.io/library/eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/solution.jar /app/solution.jar
COPY data /app/data
COPY input /app/input
# output files (queue logs, client results) are written to the working directory
WORKDIR /app/out
ENTRYPOINT ["java", "-cp", "/app/solution.jar"]
CMD ["com.ass1.server.Server", "2001", "/app/data/exercise_1_dataset.csv"]
