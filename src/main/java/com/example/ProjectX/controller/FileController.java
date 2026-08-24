package com.example.ProjectX.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.ProjectX.dto.FileResponseDto;
import com.example.ProjectX.service.FileService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/files")
public class FileController {
    private final FileService fileService;
    
    @GetMapping()
    public String validation() {
        return "";
    }

    @PostMapping("/upload")
    public FileResponseDto uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        return fileService.save(file);
    }

    @GetMapping("/get")
    public List<FileResponseDto> getAll() {
        return fileService.getAll();
    }

    @GetMapping("/get/{id}")
    public FileResponseDto getById(@PathVariable Long id) {
        return fileService.findById(id);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteFile(@PathVariable Long id) {
        fileService.deleteFile(id);
    }

}
