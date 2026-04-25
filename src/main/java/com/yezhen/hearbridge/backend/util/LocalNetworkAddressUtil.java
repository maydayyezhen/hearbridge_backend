package com.yezhen.hearbridge.backend.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * 本机局域网地址工具类。
 *
 * 用于开发环境下自动推导浏览器/手机可访问的本机 IPv4 地址。
 */
public final class LocalNetworkAddressUtil {

    private LocalNetworkAddressUtil() {
    }

    /**
     * 解析当前机器优先可用的局域网 IPv4 地址。
     *
     * Windows 下优先读取 ipconfig 中“无线局域网适配器 WLAN”的 IPv4 地址；
     * 失败后再回退到 Java NetworkInterface 扫描。
     *
     * @return 局域网 IPv4 地址
     */
    public static String resolveLocalLanIp() {
        String windowsWlanIp = resolveWindowsWlanIpFromIpconfig();
        if (isUsableLanIp(windowsWlanIp)) {
            return windowsWlanIp;
        }

        String interfaceIp = resolveFromNetworkInterfaces();
        if (isUsableLanIp(interfaceIp)) {
            return interfaceIp;
        }

        return "localhost";
    }

    /**
     * Windows 环境下从 ipconfig 输出中优先解析 WLAN 适配器的 IPv4 地址。
     *
     * @return WLAN IPv4 地址；解析失败返回 null
     */
    private static String resolveWindowsWlanIpFromIpconfig() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (!osName.contains("win")) {
            return null;
        }

        try {
            Process process = new ProcessBuilder("ipconfig").redirectErrorStream(true).start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), Charset.forName("GBK")))) {

                String line;
                boolean inWlanAdapter = false;
                boolean adapterHasGateway = false;
                String candidateIp = null;

                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();

                    if (isAdapterTitle(trimmed)) {
                        if (inWlanAdapter && isUsableLanIp(candidateIp) && adapterHasGateway) {
                            return candidateIp;
                        }

                        inWlanAdapter = isWlanAdapterTitle(trimmed);
                        adapterHasGateway = false;
                        candidateIp = null;
                        continue;
                    }

                    if (!inWlanAdapter) {
                        continue;
                    }

                    if (trimmed.startsWith("IPv4 地址") || trimmed.startsWith("IPv4 Address")) {
                        candidateIp = extractValueAfterColon(trimmed);
                    }

                    if (trimmed.startsWith("默认网关") || trimmed.startsWith("Default Gateway")) {
                        String gateway = extractValueAfterColon(trimmed);
                        if (gateway != null && !gateway.isBlank()) {
                            adapterHasGateway = true;
                        }
                    }
                }

                if (inWlanAdapter && isUsableLanIp(candidateIp) && adapterHasGateway) {
                    return candidateIp;
                }
            }
        } catch (Exception ignored) {
            // ipconfig 解析失败时回退到 NetworkInterface。
        }

        return null;
    }

    /**
     * 通过 Java NetworkInterface 扫描可用 IPv4。
     *
     * @return IPv4 地址；找不到返回 null
     */
    private static String resolveFromNetworkInterfaces() {
        try {
            List<String> preferredIps = new ArrayList<>();
            List<String> normalIps = new ArrayList<>();

            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                if (!isUsableInterface(networkInterface)) {
                    continue;
                }

                String name = safeLower(networkInterface.getName());
                String displayName = safeLower(networkInterface.getDisplayName());

                if (isExcludedInterface(name, displayName)) {
                    continue;
                }

                boolean preferred = isPreferredWirelessInterface(name, displayName);

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();

                    if (!isUsableIpv4(address)) {
                        continue;
                    }

                    String ip = address.getHostAddress();
                    if (!isUsableLanIp(ip)) {
                        continue;
                    }

                    if (preferred) {
                        preferredIps.add(ip);
                    } else {
                        normalIps.add(ip);
                    }
                }
            }

            if (!preferredIps.isEmpty()) {
                return preferredIps.get(0);
            }

            if (!normalIps.isEmpty()) {
                return normalIps.get(0);
            }
        } catch (Exception ignored) {
            // 自动识别失败时返回 null。
        }

        return null;
    }

    /**
     * 判断是否是适配器标题行。
     *
     * @param line 文本行
     * @return 是否适配器标题
     */
    private static boolean isAdapterTitle(String line) {
        return line.endsWith(":")
                && (line.contains("适配器") || line.toLowerCase().contains("adapter"));
    }

    /**
     * 判断是否是目标 WLAN 适配器标题。
     *
     * @param line 适配器标题行
     * @return 是否 WLAN 适配器
     */
    private static boolean isWlanAdapterTitle(String line) {
        String lower = safeLower(line);

        if (isExcludedInterface(lower, lower)) {
            return false;
        }

        return lower.contains("wlan")
                || lower.contains("wi-fi")
                || lower.contains("wifi")
                || lower.contains("wireless")
                || lower.contains("无线局域网适配器 wlan");
    }

    /**
     * 提取冒号后的值。
     *
     * @param line 文本行
     * @return 冒号后的值
     */
    private static String extractValueAfterColon(String line) {
        int index = line.indexOf(":");
        if (index < 0 || index + 1 >= line.length()) {
            return null;
        }
        return line.substring(index + 1).trim();
    }

    /**
     * 判断网卡是否可用。
     *
     * @param networkInterface 网卡
     * @return 是否可用
     */
    private static boolean isUsableInterface(NetworkInterface networkInterface) {
        try {
            return networkInterface.isUp()
                    && !networkInterface.isLoopback()
                    && !networkInterface.isVirtual();
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 判断是否是可用 IPv4 地址。
     *
     * @param address IP 地址
     * @return 是否可用
     */
    private static boolean isUsableIpv4(InetAddress address) {
        return address instanceof Inet4Address
                && !address.isLoopbackAddress()
                && !address.isLinkLocalAddress()
                && address.isSiteLocalAddress();
    }

    /**
     * 判断是否优先使用无线网卡。
     *
     * @param interfaceName 网卡名称
     * @param displayName 网卡展示名称
     * @return 是否无线网卡
     */
    private static boolean isPreferredWirelessInterface(String interfaceName, String displayName) {
        return interfaceName.contains("wlan")
                || interfaceName.contains("wi-fi")
                || interfaceName.contains("wifi")
                || interfaceName.contains("wireless")
                || displayName.contains("wlan")
                || displayName.contains("wi-fi")
                || displayName.contains("wifi")
                || displayName.contains("wireless")
                || displayName.contains("无线");
    }

    /**
     * 排除虚拟网卡、代理网卡、VPN 网卡。
     *
     * @param interfaceName 网卡名称
     * @param displayName 网卡展示名称
     * @return 是否应排除
     */
    private static boolean isExcludedInterface(String interfaceName, String displayName) {
        String text = interfaceName + " " + displayName;

        return text.contains("meta")
                || text.contains("clash")
                || text.contains("tap")
                || text.contains("letstap")
                || text.contains("vmware")
                || text.contains("vmnet")
                || text.contains("wsl")
                || text.contains("hyper-v")
                || text.contains("vethernet")
                || text.contains("docker")
                || text.contains("virtualbox")
                || text.contains("loopback")
                || text.contains("本地连接*");
    }

    /**
     * 判断是否是适合对外访问的局域网 IPv4。
     *
     * @param ip IPv4 地址
     * @return 是否可用
     */
    private static boolean isUsableLanIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }

        if (ip.startsWith("127.")) {
            return false;
        }

        if (ip.startsWith("169.254.")) {
            return false;
        }

        if (ip.startsWith("198.18.") || ip.startsWith("198.19.")) {
            return false;
        }

        if ("192.168.137.1".equals(ip)) {
            return false;
        }

        return ip.startsWith("10.")
                || ip.startsWith("172.")
                || ip.startsWith("192.168.");
    }

    /**
     * 安全转小写。
     *
     * @param value 原始字符串
     * @return 小写字符串
     */
    private static String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
