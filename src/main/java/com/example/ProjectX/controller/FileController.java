package com.example.ProjectX.controller;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.ProjectX.dto.FileResponseDto;
import com.example.ProjectX.model.User;
import com.example.ProjectX.service.FileService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/files")
public class FileController {
    private final FileService fileService;

    @PostMapping("/upload")
    public FileResponseDto uploadFile(@RequestParam("file") MultipartFile file, 
                                      @AuthenticationPrincipal User user) throws IOException {
        
        return fileService.save(file, user);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam UUID id, @AuthenticationPrincipal User user) {
        return fileService.downloadFile(id, user);
    }

    @GetMapping()
    public List<FileResponseDto> getAll(@AuthenticationPrincipal User user) {
        return fileService.getAll(user);
    }

    @GetMapping("/{id}")
    public FileResponseDto getById(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return fileService.findById(id, user);
    }

    @DeleteMapping("/{id}")
    public void deleteFile(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        fileService.deleteFile(id, user);
    }

}
