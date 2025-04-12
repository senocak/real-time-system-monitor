package com.github.senocak.systemmonitor

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Main Spring Boot application class for the Real-Time System Monitor.
 * This class serves as the entry point for the Spring Shell application.
 * It also enables scheduling for the WebSocket handler.
 */
@SpringBootApplication
@EnableScheduling
open class SystemMonitorApplication

/**
 * Main function that starts the Spring Boot application.
 */
fun main(args: Array<String>) {
    runApplication<SystemMonitorApplication>(*args)
}