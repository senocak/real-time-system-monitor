/**
 * Real-Time System Monitor JavaScript
 * This script handles WebSocket communication and updates the UI with system monitoring data.
 */

// WebSocket connection
let socket = null;

// Connect to WebSocket server
function connect() {
    // Use plain WebSocket
    socket = new WebSocket('ws://' + window.location.host + '/system-monitor');

    // Connection opened
    socket.onopen = function(event) {
        // Update connection status
        document.getElementById('connection-status').className = 'alert alert-success';
        document.getElementById('connection-status').textContent = 'Connected to server';
    };

    // Listen for messages
    socket.onmessage = function(event) {
        // Parse the JSON data
        const data = JSON.parse(event.data);

        // Update the UI with the received data
        updateUI(data);
    };

    // Connection closed
    socket.onclose = function(event) {
        // Update connection status
        document.getElementById('connection-status').className = 'alert alert-warning';
        document.getElementById('connection-status').textContent = 'Connection closed. Reconnecting...';

        // Try to reconnect after 5 seconds
        setTimeout(connect, 5000);
    };

    // Connection error
    socket.onerror = function(error) {
        // Update connection status
        document.getElementById('connection-status').className = 'alert alert-danger';
        document.getElementById('connection-status').textContent = 'Connection error';

        // Try to reconnect after 5 seconds
        setTimeout(connect, 5000);
    };
}

// Update the UI with system monitoring data
function updateUI(data) {
    // Update memory information
    if (data.memoryInfo) {
        const memoryPercent = data.memoryInfo.usedPercent.toFixed(1);
        const totalGB = (data.memoryInfo.total / (1024 * 1024 * 1024)).toFixed(2);
        const usedGB = (data.memoryInfo.used / (1024 * 1024 * 1024)).toFixed(2);
        const availableGB = (data.memoryInfo.available / (1024 * 1024 * 1024)).toFixed(2);

        // Update progress bar
        const memoryProgress = document.getElementById('memory-progress');
        memoryProgress.style.width = memoryPercent + '%';
        memoryProgress.textContent = memoryPercent + '%';

        // Update color based on usage
        if (memoryPercent > 90) {
            memoryProgress.className = 'progress-bar bg-danger';
        } else if (memoryPercent > 70) {
            memoryProgress.className = 'progress-bar bg-warning';
        } else {
            memoryProgress.className = 'progress-bar bg-success';
        }

        // Update stats text
        document.getElementById('memory-stats').textContent = 
            `Total: ${totalGB} GB | Used: ${usedGB} GB | Free: ${availableGB} GB (${memoryPercent}%)`;
    }

    // Update CPU information
    if (data.cpuUsage !== undefined) {
        const cpuPercent = data.cpuUsage.toFixed(1);

        // Update progress bar
        const cpuProgress = document.getElementById('cpu-progress');
        cpuProgress.style.width = cpuPercent + '%';
        cpuProgress.textContent = cpuPercent + '%';

        // Update color based on usage
        if (cpuPercent > 90) {
            cpuProgress.className = 'progress-bar bg-danger';
        } else if (cpuPercent > 70) {
            cpuProgress.className = 'progress-bar bg-warning';
        } else {
            cpuProgress.className = 'progress-bar bg-success';
        }

        // Update stats text
        document.getElementById('cpu-stats').textContent = `Usage: ${cpuPercent}%`;
    }

    // Update network information
    if (data.networkInfo) {
        const bytesReceivedMB = (data.networkInfo.bytesReceived / (1024 * 1024)).toFixed(2);
        const bytesSentMB = (data.networkInfo.bytesSent / (1024 * 1024)).toFixed(2);

        document.getElementById('bytes-received').textContent = bytesReceivedMB + ' MB';
        document.getElementById('packets-received').textContent = data.networkInfo.packetsReceived + ' packets';
        document.getElementById('bytes-sent').textContent = bytesSentMB + ' MB';
        document.getElementById('packets-sent').textContent = data.networkInfo.packetsSent + ' packets';
    }

    // Update top processes by memory
    if (data.topProcessesByMemory) {
        const memoryProcessesTable = document.getElementById('memory-processes');
        memoryProcessesTable.innerHTML = ''; // Clear existing rows

        data.topProcessesByMemory.forEach(process => {
            const row = document.createElement('tr');

            // PID
            const pidCell = document.createElement('td');
            pidCell.textContent = process.pid;
            row.appendChild(pidCell);

            // Name
            const nameCell = document.createElement('td');
            nameCell.textContent = process.name;
            row.appendChild(nameCell);

            // Memory %
            const memoryPercentCell = document.createElement('td');
            memoryPercentCell.textContent = process.memory.toFixed(1) + '%';
            row.appendChild(memoryPercentCell);

            // Memory Size
            const memorySizeCell = document.createElement('td');
            const memorySizeMB = (process.memSize / (1024 * 1024)).toFixed(2);
            memorySizeCell.textContent = memorySizeMB + ' MB';
            row.appendChild(memorySizeCell);

            memoryProcessesTable.appendChild(row);
        });
    }

    // Update top processes by CPU
    if (data.topProcessesByCpu) {
        const cpuProcessesTable = document.getElementById('cpu-processes');
        cpuProcessesTable.innerHTML = ''; // Clear existing rows

        data.topProcessesByCpu.forEach(process => {
            const row = document.createElement('tr');

            // PID
            const pidCell = document.createElement('td');
            pidCell.textContent = process.pid;
            row.appendChild(pidCell);

            // Name
            const nameCell = document.createElement('td');
            nameCell.textContent = process.name;
            row.appendChild(nameCell);

            // CPU %
            const cpuPercentCell = document.createElement('td');
            cpuPercentCell.textContent = process.cpu.toFixed(1) + '%';
            row.appendChild(cpuPercentCell);

            // Memory %
            const memoryPercentCell = document.createElement('td');
            memoryPercentCell.textContent = process.memory.toFixed(1) + '%';
            row.appendChild(memoryPercentCell);

            cpuProcessesTable.appendChild(row);
        });
    }
}

// Connect when the page loads
document.addEventListener('DOMContentLoaded', function() {
    connect();
});
