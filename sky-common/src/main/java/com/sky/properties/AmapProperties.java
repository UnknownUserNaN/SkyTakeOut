package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 高德地图API配置
 */
@Component
@ConfigurationProperties(prefix = "sky.amap")
@Data
public class AmapProperties {
    private String key;
    private String geoUrl; // 地理编码API调用的URL
    private String output = "json"; // 输出格式，默认为JSON
    private String distanceUrl; // 距离计算API调用的URL
}