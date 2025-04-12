package com.github.senocak.systemmonitor.dto

/**
 * Data Transfer Object for system monitoring data.
 * This class contains all the data that will be sent to clients via WebSocket.
 */
data class SystemMonitorData(
    var memoryInfo: MemoryInfo? = null,
    var cpuUsage: Double = 0.0,
    var networkInfo: NetworkInfo? = null,
    var topProcessesByMemory: List<ProcessInfo>? = null,
    var topProcessesByCpu: List<ProcessInfo>? = null
) {
    /**
     * Memory information data class.
     */
    data class MemoryInfo(
        var total: Long = 0,
        var used: Long = 0,
        var available: Long = 0,
        var usedPercent: Double = 0.0
    )

    /**
     * Network information data class.
     */
    data class NetworkInfo(
        var bytesReceived: Long = 0,
        var packetsReceived: Long = 0,
        var bytesSent: Long = 0,
        var packetsSent: Long = 0
    )

    /**
     * Process information data class.
     */
    data class ProcessInfo(
        var pid: Int = 0,
        var name: String = "",
        var cpu: Double = 0.0,
        var memory: Double = 0.0,
        var memSize: Long = 0
    )
}