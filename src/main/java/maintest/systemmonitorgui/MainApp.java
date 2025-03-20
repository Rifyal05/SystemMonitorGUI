/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package maintest.systemmonitorgui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.BorderFactory;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.hardware.PhysicalMemory;
import oshi.util.FormatUtil;

/**
 *
 * @author rifial
 */
@SuppressWarnings("serial")
public class MainApp extends javax.swing.JFrame {

    private int time = 0;

    private final List<Double> cpuHistory = new ArrayList<>();
    private final List<Double> memoryHistory = new ArrayList<>();
    private final List<Double> diskUsageHistory = new ArrayList<>();
    private final List<Double> networkHistory = new ArrayList<>();

    private static final int HISTORY_SIZE = 31;
    private static final int DISK_HISTORY_SIZE = 3;

    private final SystemInfo systemInfo = new SystemInfo();
    private final HardwareAbstractionLayer hal = systemInfo.getHardware();
    private final CentralProcessor processor = hal.getProcessor();
    private final GlobalMemory memory = hal.getMemory();

    private final XYSeries cpuSeries = new XYSeries("CPU Usage");
    private final XYSeries memorySeries = new XYSeries("Memory Usage");
    private final XYSeries diskSeries = new XYSeries("Disk Usage");
    private final XYSeries networkSeries = new XYSeries("Network Traffic");

    private ChartPanel cpuChartPanel;
    private ChartPanel memoryChartPanel;
    private ChartPanel diskChartPanel;
    private ChartPanel networkChartPanel;

    private long[] prevTicks;
    private long prevDiskTime = 0;
    private long prevReadBytes = 0;
    private long prevWriteBytes = 0;

    // Network
    private long prevRxBytes = 0;
    private long prevTxBytes = 0;

    /**
     * Creates new form MainApp
     */
    public MainApp() {
        initComponents();
        prevTicks = processor.getSystemCpuLoadTicks();
        initCharts();
        startMonitoring();
        initNetworkData();
        initDiskData();
        setResizable(false);
        setLocationRelativeTo(null);

        final String cpuGhzString = String.format("%.2f GHz", processor.getMaxFreq() / 1e9);
        cpughz.setText(cpuGhzString);
    }

    private void initDiskData() {
        for (HWDiskStore diskStore : hal.getDiskStores()) {
            prevDiskTime = diskStore.getTransferTime();
            prevReadBytes = diskStore.getReadBytes();
            prevWriteBytes = diskStore.getWriteBytes();
        }
    }

    private void initCharts() {
        panelgrafikcpu.setLayout(new BorderLayout());
        panelgrafikmemory.setLayout(new BorderLayout());
        panelgrafikdisk.setLayout(new BorderLayout());
        panelgrafiknetwork.setLayout(new BorderLayout());

        JFreeChart cpuChart = ChartFactory.createXYLineChart("CPU MONITOR", "SECOND(s)", "PERCENTAGE(%)", new XYSeriesCollection(cpuSeries), PlotOrientation.VERTICAL, false, true, false);
        JFreeChart memoryChart = ChartFactory.createXYLineChart("MEMORY MONITOR", "SECOND(s)", "USAGE(&)", new XYSeriesCollection(memorySeries), PlotOrientation.VERTICAL, false, true, false);
        JFreeChart diskChart = ChartFactory.createXYLineChart("DISK MONITOR", "SECOND(s)", "ACTIVE TIME(%)", new XYSeriesCollection(diskSeries), PlotOrientation.VERTICAL, false, true, false);
        JFreeChart networkChart = ChartFactory.createXYLineChart("NETWORK TRAFFIC", "SECOND(s)", "Bytes(/s)", new XYSeriesCollection(networkSeries), PlotOrientation.VERTICAL, false, true, false);

        XYPlot cpuPlot = cpuChart.getXYPlot();
        XYAreaRenderer cpuRenderer = createAreaRendererWithOpacity();
        cpuPlot.setRenderer(cpuRenderer);

        XYPlot memoryPlot = memoryChart.getXYPlot();
        XYAreaRenderer memoryRenderer = createAreaRendererWithOpacity();
        memoryPlot.setRenderer(memoryRenderer);

        XYPlot diskPlot = diskChart.getXYPlot();
        XYAreaRenderer diskRenderer = createAreaRendererWithOpacity();
        diskPlot.setRenderer(diskRenderer);

        XYPlot networkPlot = networkChart.getXYPlot();
        XYAreaRenderer networkRenderer = createAreaRendererWithOpacity();
        networkPlot.setRenderer(networkRenderer);

        cpuChartPanel = new ChartPanel(cpuChart);
        cpuChartPanel.setPreferredSize(new Dimension(538, 351));
        cpuChartPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        memoryChartPanel = new ChartPanel(memoryChart);
        memoryChartPanel.setPreferredSize(new Dimension(538, 351));
        memoryChartPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        diskChartPanel = new ChartPanel(diskChart);
        diskChartPanel.setPreferredSize(new Dimension(538, 351));
        diskChartPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        networkChartPanel = new ChartPanel(networkChart);
        networkChartPanel.setPreferredSize(new Dimension(538, 351));
        networkChartPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        panelgrafikcpu.add(cpuChartPanel, BorderLayout.CENTER);
        panelgrafikmemory.add(memoryChartPanel, BorderLayout.CENTER);
        panelgrafikdisk.add(diskChartPanel, BorderLayout.CENTER);
        panelgrafiknetwork.add(networkChartPanel, BorderLayout.CENTER);
    }

    private XYAreaRenderer createAreaRendererWithOpacity() {
        return new XYAreaRenderer() {
            @Override
            public Paint getSeriesPaint(int series) {
                Paint paint = super.getSeriesPaint(series);
                if (paint instanceof Color) {
                    Color originalColor = (Color) paint;
                    return new Color(originalColor.getRed(), originalColor.getGreen(), originalColor.getBlue(), (int) (255 * 0.9));
                } else {
                    return paint;
                }
            }
        };
    }

    private void startMonitoring() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateSystemInfo();
            }
        }, 0, 1000);
    }

    private void updateSystemInfo() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    // CPU
                    double cpuUsage = calculateCpuUsage();

                    // Memory
                    long usedMemory = memory.getTotal() - memory.getAvailable();
                    double memoryUsage = (double) usedMemory / memory.getTotal() * 100;

                    // Disk
                    long diskTime = 0;
                    long readRate = 0;
                    long writeRate = 0;
                    for (HWDiskStore diskStore : systemInfo.getHardware().getDiskStores()) {
                        diskTime += diskStore.getTransferTime();
                        readRate += diskStore.getReadBytes() - prevReadBytes;
                        writeRate += diskStore.getWriteBytes() - prevWriteBytes;
                        diskStore.getReads();
                        diskStore.getWrites();
                    }

                    long diskTimeDiff = diskTime - prevDiskTime;
                    prevDiskTime = diskTime;
                    double diskUsage = diskTimeDiff / 1000.0 * 100.0;

                    diskUsageHistory.add(diskUsage);
                    if (diskUsageHistory.size() > DISK_HISTORY_SIZE) {
                        diskUsageHistory.remove(0);
                    }

                    diskUsageHistory.add(diskUsage);
                    if (diskUsageHistory.size() > DISK_HISTORY_SIZE) {
                        diskUsageHistory.remove(0);
                    }

                    final double finalDiskUsage = diskUsage;
                    final String diskUsageString = String.format("%.2f%%", finalDiskUsage);
                    diskactivetime.setText(diskUsageString + " Active Time");

                    String diskInfo = String.format("%.2f%%, Read: %.2f MB/s, Write: %.2f MB/s", finalDiskUsage, readRate / 1024.0 / 1024.0, writeRate / 1024.0 / 1024.0);
                    diskreadwrite.setText(diskInfo);
                    long currentReadBytes = systemInfo.getHardware().getDiskStores().get(0).getReadBytes();
                    long currentWriteBytes = systemInfo.getHardware().getDiskStores().get(0).getWriteBytes();

                    prevReadBytes = currentReadBytes;
                    prevWriteBytes = currentWriteBytes;

                    // Network
                    updateNetworkData();
                    long currentRxBytes = getTotalRxBytes();
                    long currentTxBytes = getTotalTxBytes();

                    long rxDiff = currentRxBytes - prevRxBytes;
                    long txDiff = currentTxBytes - prevTxBytes;
                    prevRxBytes = currentRxBytes;
                    prevTxBytes = currentTxBytes;

                    List<PhysicalMemory> memoryModules = hal.getMemory().getPhysicalMemory();
//                    String memorySpeed = "N/A";
                    if (!memoryModules.isEmpty()) {
                        final String memorySpeed;
                        final String memoryType;
                        PhysicalMemory firstModule = memoryModules.get(0);
//                        System.out.println("PhysicalMemory class: " + firstModule.getClass().getName());
                        long clockSpeed = firstModule.getClockSpeed();
                        memorySpeed = String.format("%.0f MHz", clockSpeed / 1e6);
//                        System.out.println("Memory Speed: " + memorySpeed);
                        memoryspeed.setText(memorySpeed);
                        memoryType = firstModule.getMemoryType();
                        memorytype.setText(memoryType);
                    }

                    SwingUtilities.invokeLater(() -> {
                        cpupersenusage.setText(String.format("%.1f%%", cpuUsage));
                        long currentFreq = processor.getCurrentFreq()[0];
//                        System.out.println("currentFreq: " + currentFreq);
                        final String cpuGhzString = String.format("%.2f GHz", currentFreq / 1e9);
                        cpughz.setText(String.valueOf(cpuGhzString));

                        cpuHistory.add(cpuUsage);
                        memoryHistory.add(memoryUsage);
                        diskUsageHistory.add(finalDiskUsage);
                        networkHistory.add((double) (rxDiff + txDiff));

                        if (cpuHistory.size() > HISTORY_SIZE) {
                            cpuHistory.remove(0);
                            memoryHistory.remove(0);
                            diskUsageHistory.remove(0);
                            networkHistory.remove(0);
                        }

                        updateCharts();

                        time++;
                        networksend.setText(FormatUtil.formatBytes(txDiff) + "/s");
                        networkreceive.setText(FormatUtil.formatBytes(rxDiff) + "/s");
                        memoryusage.setText(FormatUtil.formatBytes(usedMemory) + " / " + FormatUtil.formatBytes(memory.getTotal()));
                        cpughz.setText(String.valueOf(cpuGhzString));
//                        memoryspeed.setText(memorySpeed);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return null;
            }

            private void updateCharts() {
                updateSeries(cpuSeries, cpuHistory);
                updateSeries(memorySeries, memoryHistory);
                updateSeries(diskSeries, diskUsageHistory);
                updateSeries(networkSeries, networkHistory);
            }

            private void updateSeries(XYSeries series, List<Double> data) {
                series.clear();
                for (int i = 0; i < data.size(); i++) {
                    series.add(i, data.get(i));
                }
            }

            @Override
            protected void done() {
                SwingUtilities.invokeLater(() -> {
                    repaint();
                });
            }
        }.execute();
    }

    private void initNetworkData() {
        long rxBytes = 0;
        long txBytes = 0;
        for (NetworkIF netIF : hal.getNetworkIFs()) {
            rxBytes += netIF.getBytesRecv();
            txBytes += netIF.getBytesSent();
        }
        prevRxBytes = rxBytes;
        prevTxBytes = txBytes;

    }

    private void updateNetworkData() {
        for (NetworkIF netIF : hal.getNetworkIFs()) {
            netIF.updateAttributes();
        }
    }

    private long getTotalRxBytes() {
        long rxBytes = 0;
        for (NetworkIF netIF : hal.getNetworkIFs()) {
            rxBytes += netIF.getBytesRecv();
        }
        return rxBytes;
    }

    private long getTotalTxBytes() {
        long txBytes = 0;
        for (NetworkIF netIF : hal.getNetworkIFs()) {
            txBytes += netIF.getBytesSent();
        }
        return txBytes;
    }

    private double calculateCpuUsage() {
        try {
            long[] ticks = processor.getSystemCpuLoadTicks();
            long nice = ticks[CentralProcessor.TickType.NICE.getIndex()] - prevTicks[CentralProcessor.TickType.NICE.getIndex()];
            long irq = ticks[CentralProcessor.TickType.IRQ.getIndex()] - prevTicks[CentralProcessor.TickType.IRQ.getIndex()];
            long softirq = ticks[CentralProcessor.TickType.SOFTIRQ.getIndex()] - prevTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()];
            long steal = ticks[CentralProcessor.TickType.STEAL.getIndex()] - prevTicks[CentralProcessor.TickType.STEAL.getIndex()];
            long cSys = ticks[CentralProcessor.TickType.SYSTEM.getIndex()] - prevTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
            long user = ticks[CentralProcessor.TickType.USER.getIndex()] - prevTicks[CentralProcessor.TickType.USER.getIndex()];
            long iowait = ticks[CentralProcessor.TickType.IOWAIT.getIndex()] - prevTicks[CentralProcessor.TickType.IOWAIT.getIndex()];
            long idle = ticks[CentralProcessor.TickType.IDLE.getIndex()] - prevTicks[CentralProcessor.TickType.IDLE.getIndex()];
            long totalCpu = user + nice + cSys + idle + iowait + irq + softirq + steal;

            prevTicks = ticks;
            return (1.0 - (double) idle / totalCpu) * 100.0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0;
        }

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        panelgrafiknetwork = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        panelgrafikcpu = new javax.swing.JPanel();
        panelgrafikmemory = new javax.swing.JPanel();
        panelgrafikdisk = new javax.swing.JPanel();
        diskactivetime = new javax.swing.JLabel();
        memoryusage = new javax.swing.JLabel();
        cpughz = new javax.swing.JLabel();
        cpupersenusage = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        diskreadwrite = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        networksend = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        networkreceive = new javax.swing.JLabel();
        memoryspeed = new javax.swing.JLabel();
        memorytype = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel2.setBackground(new java.awt.Color(255, 102, 102));

        panelgrafiknetwork.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        javax.swing.GroupLayout panelgrafiknetworkLayout = new javax.swing.GroupLayout(panelgrafiknetwork);
        panelgrafiknetwork.setLayout(panelgrafiknetworkLayout);
        panelgrafiknetworkLayout.setHorizontalGroup(
            panelgrafiknetworkLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 536, Short.MAX_VALUE)
        );
        panelgrafiknetworkLayout.setVerticalGroup(
            panelgrafiknetworkLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 349, Short.MAX_VALUE)
        );

        jLabel2.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel2.setText("SYSTEM MONITOR GUI SIMPLE");

        panelgrafikcpu.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelgrafikcpu.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                panelgrafikcpuMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout panelgrafikcpuLayout = new javax.swing.GroupLayout(panelgrafikcpu);
        panelgrafikcpu.setLayout(panelgrafikcpuLayout);
        panelgrafikcpuLayout.setHorizontalGroup(
            panelgrafikcpuLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 511, Short.MAX_VALUE)
        );
        panelgrafikcpuLayout.setVerticalGroup(
            panelgrafikcpuLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 352, Short.MAX_VALUE)
        );

        panelgrafikmemory.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelgrafikmemory.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                panelgrafikmemoryMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout panelgrafikmemoryLayout = new javax.swing.GroupLayout(panelgrafikmemory);
        panelgrafikmemory.setLayout(panelgrafikmemoryLayout);
        panelgrafikmemoryLayout.setHorizontalGroup(
            panelgrafikmemoryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 528, Short.MAX_VALUE)
        );
        panelgrafikmemoryLayout.setVerticalGroup(
            panelgrafikmemoryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 345, Short.MAX_VALUE)
        );

        panelgrafikdisk.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelgrafikdisk.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                panelgrafikdiskMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout panelgrafikdiskLayout = new javax.swing.GroupLayout(panelgrafikdisk);
        panelgrafikdisk.setLayout(panelgrafikdiskLayout);
        panelgrafikdiskLayout.setHorizontalGroup(
            panelgrafikdiskLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        panelgrafikdiskLayout.setVerticalGroup(
            panelgrafikdiskLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 336, Short.MAX_VALUE)
        );

        diskactivetime.setText("%");

        memoryusage.setText("memory");

        cpughz.setText("Ghz");

        cpupersenusage.setText("%");

        jLabel1.setText("INFORMASI LAINNYA");

        jLabel3.setText("SPEED            :");

        jLabel4.setText("CAPACITY       :");

        jLabel5.setText("CPU USAGE      :");

        jLabel6.setText("CLOCK SPEED  :");

        jLabel7.setText("MEMORY");

        jLabel8.setText("CPU");

        jLabel9.setText("DISK");

        jLabel10.setText("ACTIVE TIME    : ");

        jLabel11.setText("READ/WRITE    :");

        diskreadwrite.setText("R/W");

        jLabel12.setText("NETWORK");

        jLabel13.setText("SEND      : ");

        networksend.setText("networksend");

        jLabel16.setText("RECEIVE :");

        networkreceive.setText("receive");

        memoryspeed.setText("speed");

        memorytype.setText("type");

        jLabel14.setText("TYPE               :");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(jLabel3)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(memoryspeed, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(jLabel14, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(memoryusage, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(memorytype, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                .addGap(41, 41, 41)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, 105, Short.MAX_VALUE)
                                            .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(cpupersenusage, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(cpughz, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(jLabel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addGap(22, 22, 22)
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(diskreadwrite, javax.swing.GroupLayout.PREFERRED_SIZE, 299, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(diskactivetime, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                    .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(12, 12, 12)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jLabel13)
                                            .addComponent(jLabel16))
                                        .addGap(2, 2, 2)
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(networkreceive, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(networksend, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                    .addComponent(jLabel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(panelgrafiknetwork, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(panelgrafikmemory, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 27, Short.MAX_VALUE)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(panelgrafikcpu, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(panelgrafikdisk, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                        .addGap(47, 47, 47))))
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(393, 393, 393)
                .addComponent(jLabel2)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addGap(50, 50, 50)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(panelgrafiknetwork, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(panelgrafikcpu, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(27, 27, 27)
                        .addComponent(panelgrafikdisk, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(panelgrafikmemory, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(12, 12, 12)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel9)
                                    .addComponent(jLabel12))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(diskactivetime)
                                        .addComponent(jLabel10)
                                        .addComponent(jLabel13))
                                    .addComponent(networksend, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel11)
                                    .addComponent(diskreadwrite)
                                    .addComponent(jLabel16)
                                    .addComponent(networkreceive)))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel7)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel3)
                                    .addComponent(memoryspeed))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel4)
                                    .addComponent(memoryusage)))))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5)
                            .addComponent(cpupersenusage))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel6)
                            .addComponent(cpughz))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(memorytype)
                    .addComponent(jLabel14))
                .addContainerGap(105, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void panelgrafikdiskMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_panelgrafikdiskMouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_panelgrafikdiskMouseClicked

    private void panelgrafikmemoryMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_panelgrafikmemoryMouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_panelgrafikmemoryMouseClicked

    private void panelgrafikcpuMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_panelgrafikcpuMouseClicked

    }//GEN-LAST:event_panelgrafikcpuMouseClicked

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(MainApp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MainApp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MainApp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainApp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new MainApp().setVisible(true);
            }
        });
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel cpughz;
    private javax.swing.JLabel cpupersenusage;
    private javax.swing.JLabel diskactivetime;
    private javax.swing.JLabel diskreadwrite;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JLabel memoryspeed;
    private javax.swing.JLabel memorytype;
    private javax.swing.JLabel memoryusage;
    private javax.swing.JLabel networkreceive;
    private javax.swing.JLabel networksend;
    private javax.swing.JPanel panelgrafikcpu;
    private javax.swing.JPanel panelgrafikdisk;
    private javax.swing.JPanel panelgrafikmemory;
    private javax.swing.JPanel panelgrafiknetwork;
    // End of variables declaration//GEN-END:variables
}
