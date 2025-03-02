FROM eclipse-temurin:21
VOLUME /tmp
COPY vars.sh .
COPY build/libs/*.jar calefaction.jar
ENTRYPOINT ["java","-jar","/calefaction.jar"]

# Create directory
RUN mkdir -p /opt/myapp/charts

# Set permissions
RUN chown root:root /opt/myapp/charts