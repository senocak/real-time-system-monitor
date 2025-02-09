package main

import (
	"context"
	"fmt"
	"os"
	"os/signal"
	"sort"
	"sync"
	"syscall"
	"time"

	"github.com/gdamore/tcell/v2"
	"github.com/shirou/gopsutil/v3/cpu"
	"github.com/shirou/gopsutil/v3/mem"
	"github.com/shirou/gopsutil/v3/net"
	"github.com/shirou/gopsutil/v3/process"
)

type ProcessInfo struct {
	PID     int32
	Name    string
	CPU     float64
	Memory  float64
	MemSize uint64
}

func main() {
	screen, err := tcell.NewScreen()
	if err != nil {
		fmt.Fprintf(os.Stderr, "%v\n", err)
		os.Exit(1)
	}

	if err := screen.Init(); err != nil {
		fmt.Fprintf(os.Stderr, "%v\n", err)
		os.Exit(1)
	}

	screen.SetStyle(tcell.StyleDefault.
		Background(tcell.ColorBlack).
		Foreground(tcell.ColorWhite))
	screen.Clear()

	defer screen.Fini()

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, os.Interrupt, syscall.SIGTERM)

	var wg sync.WaitGroup
	wg.Add(1)

	go func() {
		defer wg.Done()
		for {
			select {
			case <-ctx.Done():
				return
			case <-sigChan:
				cancel()
				return
			default:
				updateScreen(screen)
				time.Sleep(1 * time.Second)
			}
		}
	}()

	for {
		switch ev := screen.PollEvent().(type) {
		case *tcell.EventKey:
			if ev.Key() == tcell.KeyEscape {
				cancel()
				return
			}
		case *tcell.EventResize:
			screen.Sync()
		}
	}
}

func updateScreen(screen tcell.Screen) {
	screen.Clear()
	width, height := screen.Size()

	v, _ := mem.VirtualMemory()
	c, _ := cpu.Percent(0, false)
	n, _ := net.IOCounters(false)

	memUsed := float64(v.Used) / float64(v.Total) * 100
	drawBar(screen, 1, 1, width-2, "Memory", memUsed,
		fmt.Sprintf("Total: %.2fGB Used: %.2fGB Free: %.2fGB (%.1f%%)",
			float64(v.Total)/1024/1024/1024,
			float64(v.Used)/1024/1024/1024,
			float64(v.Free)/1024/1024/1024,
			memUsed))

	cpuUsed := c[0]
	drawBar(screen, 1, 3, width-2, "CPU", cpuUsed,
		fmt.Sprintf("Usage: %.1f%%", cpuUsed))

	netStats := fmt.Sprintf("Network - Received: %.2fMB (%d pkts) Sent: %.2fMB (%d pkts)",
		float64(n[0].BytesRecv)/1024/1024,
		n[0].PacketsRecv,
		float64(n[0].BytesSent)/1024/1024,
		n[0].PacketsSent)
	drawText(screen, 1, 5, netStats, tcell.StyleDefault.Foreground(tcell.ColorGreen))

	processes := getProcessInfo()

	drawProcessTable(screen, 1, 7, width/2-1, height-7, "Top Memory Usage", sortByMemory(processes))
	drawProcessTable(screen, width/2+1, 7, width/2-2, height-7, "Top CPU Usage", sortByCPU(processes))

	screen.Show()
}

func drawBar(screen tcell.Screen, x, y, width int, label string, value float64, stats string) {
	barWidth := width - 2
	filled := int(float64(barWidth) * value / 100)

	drawText(screen, x, y, label+": "+stats, tcell.StyleDefault.Foreground(tcell.ColorYellow))

	screen.SetContent(x, y+1, '[', nil, tcell.StyleDefault)
	for i := 0; i < barWidth; i++ {
		char := ' '
		style := tcell.StyleDefault.Background(tcell.ColorDarkGray)
		if i < filled {
			style = tcell.StyleDefault.Background(tcell.ColorGreen)
		}
		screen.SetContent(x+1+i, y+1, char, nil, style)
	}
	screen.SetContent(x+barWidth+1, y+1, ']', nil, tcell.StyleDefault)
}

func drawText(screen tcell.Screen, x, y int, text string, style tcell.Style) {
	for i, r := range text {
		screen.SetContent(x+i, y, r, nil, style)
	}
}

func drawProcessTable(screen tcell.Screen, x, y, width, height int, title string, processes []ProcessInfo) {
	drawText(screen, x, y, title, tcell.StyleDefault.Foreground(tcell.ColorYellow))
	drawText(screen, x, y+1, fmt.Sprintf("%-6s %-20s %-10s %-10s", "PID", "Name", "CPU%", "Memory"),
		tcell.StyleDefault.Foreground(tcell.ColorGreen))

	for i, p := range processes {
		if i >= height-3 {
			break
		}
		drawText(screen, x, y+2+i, fmt.Sprintf("%-6d %-20s %-10.1f %-10.1f",
			p.PID, truncateString(p.Name, 20), p.CPU, p.Memory),
			tcell.StyleDefault)
	}
}

func getProcessInfo() []ProcessInfo {
	processes, _ := process.Processes()
	var processInfo []ProcessInfo

	for _, p := range processes {
		name, _ := p.Name()
		cpu, _ := p.CPUPercent()
		mem, _ := p.MemoryPercent()
		memInfo, _ := p.MemoryInfo()

		if memInfo != nil {
			processInfo = append(processInfo, ProcessInfo{
				PID:     p.Pid,
				Name:    name,
				CPU:     cpu,
				Memory:  float64(mem),
				MemSize: memInfo.RSS,
			})
		}
	}

	return processInfo
}

func sortByMemory(processes []ProcessInfo) []ProcessInfo {
	sort.Slice(processes, func(i, j int) bool {
		return processes[i].MemSize > processes[j].MemSize
	})
	if len(processes) > 10 {
		processes = processes[:10]
	}
	return processes
}

func sortByCPU(processes []ProcessInfo) []ProcessInfo {
	sort.Slice(processes, func(i, j int) bool {
		return processes[i].CPU > processes[j].CPU
	})
	if len(processes) > 10 {
		processes = processes[:10]
	}
	return processes
}

func truncateString(s string, length int) string {
	if len(s) <= length {
		return s
	}
	return s[:length-3] + "..."
}