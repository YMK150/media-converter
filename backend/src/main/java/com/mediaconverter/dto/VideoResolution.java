package com.mediaconverter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 视频分辨率内部类
 */
@Data
@AllArgsConstructor
public class VideoResolution {
    private Integer width;
    private Integer height;
}