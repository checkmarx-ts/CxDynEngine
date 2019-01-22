FROM openjdk:8
VOLUME /tmp
ADD cx-dyn-engines-app/build/libs/cx-dyn-engines-app-1.0-SNAPSHOT.jar /cx-dyn-1.0.jar
ENTRYPOINT ["/usr/bin/java"]
CMD ["-Xms512m", "-Xmx1024m","-Djava.security.egd=file:/dev/./urandom", "-jar", "/cx-dyn-1.0.jar"]
EXPOSE 8080:8080
