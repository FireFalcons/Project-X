package com.example.ProjectX.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.ProjectX.dto.FileResponseDto;
import com.example.ProjectX.model.File;
import com.example.ProjectX.repository.FileRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileService {
    private final FileRepository fileRepository;
    private final String uploadDir = "D:\\ProjectX-Files";

    public FileResponseDto save(MultipartFile file) throws IOException {
       String originalName = file.getOriginalFilename();
       Long fileSize = file.getSize();
       LocalDateTime now = LocalDateTime.now();

       String extension = originalName.substring(originalName.lastIndexOf("."));
       String generatedName = UUID.randomUUID().toString() + extension;

       File entity = new File();
       entity.setName(generatedName);
       entity.setSize(fileSize);
       entity.setCreateTime(now);

       java.io.File dest = new java.io.File(uploadDir + java.io.File.separator + generatedName);

       file.transferTo(dest);
       fileRepository.save(entity);

       return new FileResponseDto(generatedName, fileSize, now, now);
    }

    public List<FileResponseDto> getAll() {
        return fileRepository.findAll().stream().map(
            f -> new FileResponseDto(f.getName(), f.getSize(), f.getCreateTime(), f.getChangTime())
        ).toList();
    }

    public FileResponseDto findById(Long id) {
        return fileRepository.findById(id).map(
            f -> new FileResponseDto(f.getName(), f.getSize(), f.getCreateTime(), f.getChangTime())
        ).orElseThrow(() -> new RuntimeException("File not found"));
    }

    @Transactional
    public void deleteFile(Long id) {
        File deleteFile = fileRepository.findById(id).orElseThrow(() -> new RuntimeException("File not found"));
        java.io.File fileOnDisk = new java.io.File(uploadDir + java.io.File.separator + deleteFile.getName());
        fileOnDisk.delete();
        fileRepository.deleteById(id);
        System.out.println("File deleted successfully!");
    }
}
