package com.mediaconverter.util;

import org.springframework.boot.SpringBootVersion;
import org.springframework.core.env.Environment;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class BannerUtil {
    
    public static String generateBanner() {
        StringBuilder banner = new StringBuilder();
        
        // 获取所有环境变量
        Map<String, String> envVars = System.getenv();
        
        // 基本信息
        banner.append("██╗    ██╗███████╗████████╗███████╗██████╗ \n");
        banner.append("██║    ██║██╔════╝╚══██╔══╝██╔════╝██╔══██╗\n");
        banner.append("██║ █╗ ██║█████╗     ██║   █████╗  ██████╔╝\n");
        banner.append("██║███╗██║██╔══╝     ██║   ██╔══╝  ██╔══██╗\n");
        banner.append("╚███╔███╔╝███████╗   ██║   ███████╗██║  ██║\n");
        banner.append(" ╚══╝╚══╝ ╚══════╝   ╚═╝   ╚══════╝╚═╝  ╚═╝\n");
        banner.append("                                              \n");
        banner.append("     影音文件格式转换服务 v1.0.0\n");
        banner.append("     支持多种音视频格式转换\n");
        banner.append("     本地运行，安全可靠 \n\n");
        
        // 系统信息部分
        banner.append("================== 系统信息 ==================\n");

        // 操作系统信息
        banner.append("操作系统: ").append(System.getProperty("os.name"))
              .append(" ").append(System.getProperty("os.arch"))
              .append(" ").append(System.getProperty("os.version")).append("\n");
        
        banner.append("Java版本: ").append(System.getProperty("java.version"))
              .append(" (").append(System.getProperty("java.vendor")).append(")\n");
        
        banner.append("Spring Boot: ").append(SpringBootVersion.getVersion()).append("\n");
        
        banner.append("当前时间: ").append(LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        
        banner.append("工作目录: ").append(System.getProperty("user.dir")).append("\n");
        
        // 运行时信息
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        banner.append("JVM运行时间: ").append(runtimeMXBean.getUptime() / 1000).append("秒\n");
        
        // 内存信息
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        banner.append("JVM内存: ").append(totalMemory - freeMemory).append("MB/")
              .append(maxMemory).append("MB\n");
        
        banner.append("================= 应用环境变量 =================\n");

        // 应用相关变量
        appendEnvVar(banner, envVars, "TRIM_APPNAME", "应用名称");
        appendEnvVar(banner, envVars, "TRIM_APPVER", "应用版本");
        appendEnvVar(banner, envVars, "TRIM_OLD_APPVER", "升级前版本");
        appendEnvVar(banner, envVars, "TRIM_SERVICE_PORT", "服务端口");
        
        banner.append("================= 路径信息 =================\n");

        appendEnvVar(banner, envVars, "TRIM_APPDEST", "可执行文件目录");
        appendEnvVar(banner, envVars, "TRIM_PKGETC", "配置文件目录");
        appendEnvVar(banner, envVars, "TRIM_PKGVAR", "动态数据目录");
        appendEnvVar(banner, envVars, "TRIM_PKGTMP", "临时文件目录");
        appendEnvVar(banner, envVars, "TRIM_PKGHOME", "用户数据目录");
        appendEnvVar(banner, envVars, "TRIM_PKGMETA", "元数据目录");
        appendEnvVar(banner, envVars, "TRIM_APPDEST_VOL", "安装存储路径");
        
        banner.append("================= 用户和权限 =================\n");

        appendEnvVar(banner, envVars, "TRIM_USERNAME", "专用用户名");
        appendEnvVar(banner, envVars, "TRIM_GROUPNAME", "专用用户组");
        appendEnvVar(banner, envVars, "TRIM_UID", "用户ID");
        appendEnvVar(banner, envVars, "TRIM_GID", "用户组ID");
        appendEnvVar(banner, envVars, "TRIM_RUN_USERNAME", "当前执行用户");
        appendEnvVar(banner, envVars, "TRIM_RUN_GROUPNAME", "当前执行用户组");
        
        banner.append("================= 系统信息 =================\n");

        appendEnvVar(banner, envVars, "TRIM_SYS_VERSION", "系统版本");
        appendEnvVar(banner, envVars, "TRIM_SYS_ARCH", "系统架构");
        appendEnvVar(banner, envVars, "TRIM_KERNEL_VERSION", "内核版本");
        appendEnvVar(banner, envVars, "TRIM_SYS_MACHINE_ID", "设备标识");
        appendEnvVar(banner, envVars, "TRIM_SYS_LANGUAGE", "系统语言");
        appendEnvVar(banner, envVars, "TRIM_APP_STATUS", "当前状态");
        
        banner.append("================= 其他信息 =================\n");

        appendEnvVar(banner, envVars, "TRIM_DATA_SHARE_PATHS", "数据共享路径");
        appendEnvVar(banner, envVars, "TRIM_DATA_ACCESSIBLE_PATHS", "可访问路径");
        appendEnvVar(banner, envVars, "TRIM_TEMP_LOGFILE", "日志文件路径");
        appendEnvVar(banner, envVars, "TRIM_TEMP_UPGRADE_FOLDER", "升级临时目录");
        banner.append("================================================\n");
        banner.append("应用启动成功 - ").append(LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");


        return banner.toString();
    }
    
    private static void appendEnvVar(StringBuilder banner, Map<String, String> envVars,
                                   String varName, String description) {
        String value = envVars.get(varName);
        if (value != null && !value.trim().isEmpty()) {
            banner.append(description).append("(").append(varName).append(") ")
                  .append(value).append('\n');
        }
    }
    


}