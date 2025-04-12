package com.github.senocak.systemmonitor.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList

/**
 * WebSocket handler for system monitoring data.
 * This class manages WebSocket sessions and sends messages directly to connected clients.
 */
@Component
class PlainWebSocketHandler : TextWebSocketHandler() {

    private val sessions = CopyOnWriteArrayList<WebSocketSession>()
    private val objectMapper = ObjectMapper()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        sessions.add(session)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        sessions.remove(session)
    }

    /**
     * Send a message to all connected WebSocket sessions.
     * @param message The message to send
     */
    fun broadcast(message: Any) {
        try {
            val json = objectMapper.writeValueAsString(message)
            val textMessage = TextMessage(json)
            for (session in sessions) {
                if (session.isOpen) {
                    try {
                        session.sendMessage(textMessage)
                    } catch (e: IOException) {
                        // Log the error and continue with other sessions
                        System.err.println("Error sending message to session: ${e.message}")
                    }
                }
            }
        } catch (e: IOException) {
            // Log the error
            System.err.println("Error serializing message: ${e.message}")
        }
    }
}