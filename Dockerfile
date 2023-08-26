FROM eclipse-temurin:17
VOLUME /tmp
COPY vars.sh .
COPY build/libs/*.jar calefaction.jar
ENTRYPOINT ["java","-jar","/calefaction.jar"]