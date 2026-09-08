package com.example.ProjectX.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "Files")
public class File {
    @Id
    private UUID id;
    
    private String name;
    private String extension;

    @Column(name = "sysfName"  ,unique = true)
    private UUID sysfName;

    @JoinColumn(name = "createdBy", nullable = false)
    private UUID userId;
    private String filePath;
    private Long size;
    private LocalDateTime createTime;
    private LocalDateTime changTime;
}
