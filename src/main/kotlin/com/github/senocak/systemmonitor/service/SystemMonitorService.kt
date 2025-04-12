package com.github.senocak.systemmonitor.service

import org.springframework.stereotype.Service
import oshi.SystemInfo
import oshi.hardware.CentralProcessor
import oshi.hardware.GlobalMemory
import oshi.hardware.HardwareAbstractionLayer
import oshi.hardware.NetworkIF
import oshi.software.os.OSProcess
import oshi.software.os.OperatingSystem

/**
 * Service for monitoring system resources using OSHI.
 */
@Service
class SystemMonitorService {

    private val systemInfo: SystemInfo
    private val hardware: HardwareAbstractionLayer
    private val os: OperatingSystem
    private val processor: CentralProcessor
    private var prevTicks: LongArray

    init {
        this.systemInfo = SystemInfo()
        this.hardware = systemInfo.hardware
        this.os = systemInfo.operatingSystem
        this.processor = hardware.processor
        this.prevTicks = processor.systemCpuLoadTicks
    }

    /**
     * Get memory usage information.
     * @return MemoryInfo object containing memory statistics
     */
    fun getMemoryInfo(): MemoryInfo {
        val memory = hardware.memory
        val totalMemory = memory.total
        val usedMemory = totalMemory - memory.available
        val memUsedPercent = usedMemory.toDouble() / totalMemory * 100

        return MemoryInfo(
            totalMemory,
            usedMemory,
            memory.available,
            memUsedPercent
        )
    }

    /**
     * Get CPU usage information.
     * @return CPU usage percentage
     */
    fun getCpuUsage(): Double {
        val ticks = processor.systemCpuLoadTicks
        val cpuUsed = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100
        prevTicks = ticks
        return cpuUsed
    }

    /**
     * Get network information by summing traffic from all network interfaces.
     * @return NetworkInfo object containing network statistics, or null if no network interfaces are available
     */
    fun getNetworkInfo(): NetworkInfo? {
        val networks = hardware.networkIFs
        if (networks.isNotEmpty()) {
            var totalBytesRecv: Long = 0
            var totalPacketsRecv: Long = 0
            var totalBytesSent: Long = 0
            var totalPacketsSent: Long = 0

            for (net in networks) {
                net.updateAttributes()
                totalBytesRecv += net.bytesRecv
                totalPacketsRecv += net.packetsRecv
                totalBytesSent += net.bytesSent
                totalPacketsSent += net.packetsSent
            }

            return NetworkInfo(
                totalBytesRecv,
                totalPacketsRecv,
                totalBytesSent,
                totalPacketsSent
            )
        }
        return null
    }

    /**
     * Get information about top processes by memory usage.
     * @param limit Maximum number of processes to return
     * @return List of ProcessInfo objects sorted by memory usage
     */
    fun getTopProcessesByMemory(limit: Int): List<ProcessInfo> {
        val processes = getProcessInfo()
        return processes
            .sortedByDescending { it.memSize }
            .take(limit)
    }

    /**
     * Get information about top processes by CPU usage.
     * @param limit Maximum number of processes to return
     * @return List of ProcessInfo objects sorted by CPU usage
     */
    fun getTopProcessesByCpu(limit: Int): List<ProcessInfo> {
        val processes = getProcessInfo()
        return processes
            .sortedByDescending { it.cpu }
            .take(limit)
    }

    /**
     * Get information about all processes.
     * @return List of ProcessInfo objects
     */
    private fun getProcessInfo(): List<ProcessInfo> {
        val processInfo = mutableListOf<ProcessInfo>()
        val processes = os.processes

        for (p in processes) {
            processInfo.add(
                ProcessInfo(
                    p.processID,
                    p.name,
                    p.processCpuLoadCumulative * 100,
                    100.0 * p.residentSetSize / hardware.memory.total,
                    p.residentSetSize
                )
            )
        }

        return processInfo
    }

    /**
     * Class representing memory information.
     */
    data class MemoryInfo(
        val total: Long,
        val used: Long,
        val available: Long,
        val usedPercent: Double
    ) {
        override fun toString(): String {
            return String.format(
                "Total: %.2fGB Used: %.2fGB Free: %.2fGB (%.1f%%)",
                total.toDouble() / (1024 * 1024 * 1024),
                used.toDouble() / (1024 * 1024 * 1024),
                available.toDouble() / (1024 * 1024 * 1024),
                usedPercent
            )
        }
    }

    /**
     * Class representing network information.
     */
    data class NetworkInfo(
        val bytesReceived: Long,
        val packetsReceived: Long,
        val bytesSent: Long,
        val packetsSent: Long
    ) {
        override fun toString(): String {
            return String.format(
                "Network - Received: %.2fMB (%d pkts) Sent: %.2fMB (%d pkts)",
                bytesReceived.toDouble() / (1024 * 1024),
                packetsReceived,
                bytesSent.toDouble() / (1024 * 1024),
                packetsSent
            )
        }
    }

    /**
     * Class representing process information.
     */
    data class ProcessInfo(
        val pid: Int,
        val name: String,
        val cpu: Double,
        val memory: Double,
        val memSize: Long
    ) {
        override fun toString(): String {
            return String.format(
                "%-6d %-20s %-10.1f %-10.1f",
                pid, truncateString(name, 20), cpu, memory
            )
        }

        private fun truncateString(s: String, length: Int): String {
            if (s.length <= length) {
                return s
            }
            return s.substring(0, length - 3) + "..."
        }
    }
}
