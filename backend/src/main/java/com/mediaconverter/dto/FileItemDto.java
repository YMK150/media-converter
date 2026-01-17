package com.mediaconverter.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class FileItemDto {
    private String name;
    private String path;
    @JsonProperty("isDirectory")
    private boolean isDirectory;
    private long size;
    private String extension;
    private String mimeType;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastModified;
    
    @JsonProperty("isMediaFile")
    private boolean isMediaFile;
    private String mediaType; // "audio" or "video"

    public FileItemDto() {}

    public FileItemDto(String name, String path, boolean isDirectory) {
        this.name = name;
        this.path = path;
        this.isDirectory = isDirectory;
    }

}