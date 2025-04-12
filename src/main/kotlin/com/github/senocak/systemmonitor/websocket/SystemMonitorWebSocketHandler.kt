package com.github.senocak.systemmonitor.websocket

import com.github.senocak.systemmonitor.dto.SystemMonitorData
import com.github.senocak.systemmonitor.service.SystemMonitorService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * WebSocket handler for system monitoring data.
 * This class periodically sends system monitoring data to connected clients.
 */
@Component
@EnableScheduling
class SystemMonitorWebSocketHandler @Autowired constructor(
    private val systemMonitorService: SystemMonitorService,
    private val webSocketHandler: PlainWebSocketHandler
) {

    /**
     * Send system monitoring data to clients every second.
     */
    @Scheduled(fixedRate = 1000)
    fun sendSystemMonitorData() {
        // Get system monitoring data
        val memoryInfo = convertMemoryInfo(systemMonitorService.getMemoryInfo())
        val cpuUsage = systemMonitorService.getCpuUsage()
        val networkInfo = convertNetworkInfo(systemMonitorService.getNetworkInfo())

        // Get top processes by memory and CPU
        val topProcessesByMemory = systemMonitorService.getTopProcessesByMemory(10)
            .map { convertProcessInfo(it) }

        val topProcessesByCpu = systemMonitorService.getTopProcessesByCpu(10)
            .map { convertProcessInfo(it) }

        // Create data object
        val data = SystemMonitorData(
            memoryInfo,
            cpuUsage,
            networkInfo,
            topProcessesByMemory,
            topProcessesByCpu
        )

        // Send data to clients
        webSocketHandler.broadcast(data)
    }

    /**
     * Convert SystemMonitorService.MemoryInfo to SystemMonitorData.MemoryInfo
     */
    private fun convertMemoryInfo(info: SystemMonitorService.MemoryInfo?): SystemMonitorData.MemoryInfo? {
        if (info == null) {
            return null
        }
        return SystemMonitorData.MemoryInfo(
            info.total,
            info.used,
            info.available,
            info.usedPercent
        )
    }

    /**
     * Convert SystemMonitorService.NetworkInfo to SystemMonitorData.NetworkInfo
     */
    private fun convertNetworkInfo(info: SystemMonitorService.NetworkInfo?): SystemMonitorData.NetworkInfo? {
        if (info == null) {
            return null
        }
        return SystemMonitorData.NetworkInfo(
            info.bytesReceived,
            info.packetsReceived,
            info.bytesSent,
            info.packetsSent
        )
    }

    /**
     * Convert SystemMonitorService.ProcessInfo to SystemMonitorData.ProcessInfo
     */
    private fun convertProcessInfo(info: SystemMonitorService.ProcessInfo): SystemMonitorData.ProcessInfo {
        return SystemMonitorData.ProcessInfo(
            info.pid,
            info.name,
            info.cpu,
            info.memory,
            info.memSize
        )
    }
}