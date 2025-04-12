package com.github.senocak.systemmonitor.config

import com.github.senocak.systemmonitor.websocket.PlainWebSocketHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

/**
 * Configuration class for WebSocket.
 * This sets up the WebSocket endpoints without using a message broker.
 */
@Configuration
@EnableWebSocket
open class WebSocketConfig(
    private val webSocketHandler: PlainWebSocketHandler
): WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        // Register the "/system-monitor" endpoint with plain WebSocket
        registry.addHandler(webSocketHandler, "/system-monitor")
                .setAllowedOrigins("*") // Allow connections from any origin
    }
}
