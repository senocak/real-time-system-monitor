package com.github.senocak.systemmonitor

import oshi.SystemInfo
import oshi.hardware.CentralProcessor
import oshi.hardware.GlobalMemory
import oshi.hardware.HardwareAbstractionLayer
import oshi.hardware.NetworkIF
import oshi.software.os.OSProcess
import oshi.software.os.OperatingSystem
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Scanner
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class Main {
    companion object {
        // ANSI escape codes for colors and terminal control
        private const val ANSI_RESET = "\u001B[0m"
        private const val ANSI_BLACK = "\u001B[30m"
        private const val ANSI_RED = "\u001B[31m"
        private const val ANSI_GREEN = "\u001B[32m"
        private const val ANSI_YELLOW = "\u001B[33m"
        private const val ANSI_BLUE = "\u001B[34m"
        private const val ANSI_PURPLE = "\u001B[35m"
        private const val ANSI_CYAN = "\u001B[36m"
        private const val ANSI_WHITE = "\u001B[37m"
        private const val ANSI_CLEAR_SCREEN = "\u001B[H\u001B[2J"
        private const val ANSI_BG_GREEN = "\u001B[42m"
        private const val ANSI_BG_BLACK = "\u001B[40m"

        private data class ProcessInfo(
            val pid: Int,
            val name: String,
            val cpu: Double,
            val memory: Double,
            val memSize: Long
        )

        private val systemInfo = SystemInfo()
        private val hardware = systemInfo.hardware
        private val os = systemInfo.operatingSystem
        private val processor = hardware.processor
        private var prevTicks = processor.systemCpuLoadTicks
        private var terminalWidth = 80 // Default width
        private var terminalHeight = 24 // Default height

        @JvmStatic
        fun main(args: Array<String>) {
            try {
                // Set up terminal size detection
                try {
                    val process = Runtime.getRuntime().exec(arrayOf("bash", "-c", "stty size </dev/tty"))
                    val br = BufferedReader(InputStreamReader(process.inputStream))
                    val line = br.readLine()
                    if (line != null) {
                        val parts = line.split(" ")
                        if (parts.size == 2) {
                            terminalHeight = parts[0].toInt()
                            terminalWidth = parts[1].toInt()
                        }
                    }
                    br.close()
                } catch (e: Exception) {
                    // Fallback to default size if detection fails
                    System.err.println("Could not detect terminal size, using defaults: ${e.message}")
                }

                val running = AtomicBoolean(true)
                val executor = Executors.newSingleThreadScheduledExecutor()

                // Set up a thread to check for 'q' key press to exit
                val inputThread = Thread {
                    val scanner = Scanner(System.`in`)
                    while (running.get()) {
                        try {
                            if (System.`in`.available() > 0) {
                                val input = System.`in`.read()
                                if (input == 'q'.code || input == 'Q'.code || input == 27) { // 'q', 'Q' or ESC
                                    running.set(false)
                                    break
                                }
                            }
                            Thread.sleep(100)
                        } catch (e: IOException) {
                            System.err.println("Error reading input: ${e.message}")
                        } catch (e: InterruptedException) {
                            Thread.currentThread().interrupt()
                        }
                    }
                    scanner.close()
                }
                inputThread.isDaemon = true
                inputThread.start()

                // Schedule regular updates
                executor.scheduleAtFixedRate({
                    try {
                        if (running.get()) {
                            updateScreen()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, 0, 1, TimeUnit.SECONDS)

                // Wait for the input thread to finish
                inputThread.join()

                // Clean up
                executor.shutdown()
                print(ANSI_RESET)
                println("System monitor exited.")
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        private fun updateScreen() {
            clearScreen()

            // Memory information
            val memory = hardware.memory
            val totalMemory = memory.total
            val usedMemory = totalMemory - memory.available
            val memUsed = usedMemory.toDouble() / totalMemory * 100

            val memStats = String.format(
                "Total: %.2fGB Used: %.2fGB Free: %.2fGB (%.1f%%)",
                totalMemory.toDouble() / (1024 * 1024 * 1024),
                usedMemory.toDouble() / (1024 * 1024 * 1024),
                memory.available.toDouble() / (1024 * 1024 * 1024),
                memUsed
            )

            drawBar(1, "Memory", memUsed, memStats)

            // CPU information
            val ticks = processor.systemCpuLoadTicks
            val cpuUsed = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100
            prevTicks = ticks

            drawBar(4, "CPU", cpuUsed, String.format("Usage: %.1f%%", cpuUsed))

            // Network information
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

                val netStats = String.format(
                    "Network - Received: %.2fMB (%d pkts) Sent: %.2fMB (%d pkts)",
                    totalBytesRecv.toDouble() / (1024 * 1024),
                    totalPacketsRecv,
                    totalBytesSent.toDouble() / (1024 * 1024),
                    totalPacketsSent
                )
                println()
                println(ANSI_GREEN + netStats + ANSI_RESET)
            }

            // Process information
            val processes = getProcessInfo()
            println()

            // Calculate table widths
            val tableWidth = terminalWidth / 2 - 2
            val tableHeight = terminalHeight - 12 // Adjust based on other content

            // Draw process tables side by side
            drawProcessTable(0, "Top Memory Usage", sortByMemory(processes), tableWidth, tableHeight)
            moveCursor(terminalWidth / 2 + 1, 7)
            drawProcessTable(terminalWidth / 2, "Top CPU Usage", sortByCPU(processes), tableWidth, tableHeight)

            // Move cursor to bottom of screen
            moveCursor(0, terminalHeight - 1)
            println("Press 'q' or ESC to exit")

            // Flush output
            System.out.flush()
        }

        private fun clearScreen() {
            print(ANSI_CLEAR_SCREEN)
            System.out.flush()
        }

        private fun moveCursor(x: Int, y: Int) {
            print(String.format("\u001B[%d;%dH", y, x))
        }

        private fun drawBar(y: Int, label: String, value: Double, stats: String) {
            val barWidth = terminalWidth - 10
            val filled = (barWidth * value / 100).toInt()

            moveCursor(1, y)
            println(ANSI_YELLOW + label + ": " + stats + ANSI_RESET)

            moveCursor(1, y + 1)
            print("[")

            for (i in 0 until barWidth) {
                if (i < filled) {
                    print(ANSI_BG_GREEN + " " + ANSI_RESET)
                } else {
                    print(ANSI_BG_BLACK + " " + ANSI_RESET)
                }
            }

            println("]")
        }

        private fun drawProcessTable(x: Int, title: String, processes: List<ProcessInfo>, width: Int, height: Int) {
            moveCursor(x + 1, 8)
            println(ANSI_YELLOW + title + ANSI_RESET)

            moveCursor(x + 1, 9)
            println(ANSI_GREEN + String.format("%-6s %-20s %-10s %-10s", "PID", "Name", "CPU%", "Memory%") + ANSI_RESET)

            for (i in processes.indices) {
                if (i >= height - 3) break
                val p = processes[i]
                moveCursor(x + 1, 10 + i)
                println(
                    String.format(
                        "%-6d %-20s %-10.1f %-10.1f",
                        p.pid, truncateString(p.name, 20), p.cpu, p.memory
                    )
                )
            }
        }

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

        private fun sortByMemory(processes: List<ProcessInfo>): List<ProcessInfo> {
            return processes
                .sortedByDescending { it.memSize }
                .take(10)
        }

        private fun sortByCPU(processes: List<ProcessInfo>): List<ProcessInfo> {
            return processes
                .sortedByDescending { it.cpu }
                .take(10)
        }

        private fun truncateString(s: String, length: Int): String {
            if (s.length <= length) {
                return s
            }
            return s.substring(0, length - 3) + "..."
        }
    }
}
