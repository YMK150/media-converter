package com.mediaconverter.config;

import com.mediaconverter.util.BannerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.Banner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.PrintStream;

@Slf4j
public class CustomBanner implements Banner {
    
    @Override
    public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
        StringBuilder sb = new StringBuilder();
        try {
            String banner = BannerUtil.generateBanner();
            sb.append(banner);
            // 额外输出一些调试信息（可选）
            if (environment.acceptsProfiles("dev", "test")) {
                sb.append("\033[90m"); // 灰色
                sb.append("=== 开发模式信息 ===");
                sb.append("Active Profiles: ").append(String.join(", ", environment.getActiveProfiles()));
                sb.append("Config Files: ").append(getConfigFiles());
                sb.append("\033[0m");
            }
            
        } catch (Exception e) {
            // 如果自定义banner失败，使用默认banner
            sb.append("影音文件格式转换服务 v1.0.0");
            sb.append("启动时间: ").append(java.time.LocalDateTime.now());
        }
        log.info("banner:\n {}", sb);
    }
    
    private String getConfigFiles() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:application*.yml");
            StringBuilder files = new StringBuilder();
            for (Resource resource : resources) {
                files.append(resource.getFilename()).append(" ");
            }
            Resource[] properties = resolver.getResources("classpath:application*.properties");
            for (Resource resource : properties) {
                files.append(resource.getFilename()).append(" ");
            }
            return files.toString();
        } catch (Exception e) {
            return "无法获取配置文件列表";
        }
    }
}