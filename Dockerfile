# Stage runtime only
FROM eclipse-temurin:21-jre

# Set working directory
WORKDIR /app

# Copy hasil build (jar) ke dalam container
COPY target/*.jar app.jar

# Expose port aplikasi
EXPOSE 8080

# Jalankan Spring Boot dengan profile prod
ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.profiles.active=prod"]
