package com.ar.crm2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class BootApplication {

    public static void main(String[] args) {
        // Todo el backend (timestamps via LocalDateTime.now()) asume la hora
        // de pared del JVM. Fijamos México explícitamente para no depender de
        // la zona del contenedor/host donde se despliegue.
        TimeZone.setDefault(TimeZone.getTimeZone("America/Mexico_City"));
        SpringApplication.run(BootApplication.class, args);
    }
}
