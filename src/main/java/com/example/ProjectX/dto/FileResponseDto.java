package com.example.ProjectX.dto;

import java.time.LocalDateTime;

public record FileResponseDto (String name, Long size, 
                LocalDateTime createTime, LocalDateTime changeTime, String creatorBy) {
}
