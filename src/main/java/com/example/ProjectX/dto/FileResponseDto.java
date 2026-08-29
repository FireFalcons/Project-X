package com.example.ProjectX.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record FileResponseDto (UUID id, String name, Long size, 
                LocalDateTime createTime, LocalDateTime changeTime, String creatorBy) {
}
