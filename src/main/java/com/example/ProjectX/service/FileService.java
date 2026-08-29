package com.example.ProjectX.service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.ProjectX.dto.FileResponseDto;
import com.example.ProjectX.model.File;
import com.example.ProjectX.model.User;
import com.example.ProjectX.repository.FileRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileService {
    private final FileRepository fileRepository;
    private final String directory = System.getProperty("user.dir") + java.io.File.separator + "storage";
    
    public FileResponseDto save(MultipartFile file, User activeUser) throws IOException {
        java.io.File dir = new java.io.File(directory);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String originalName = file.getOriginalFilename();
        Long fileSize = file.getSize();
        LocalDateTime now = LocalDateTime.now();

        String extension = originalName.substring(originalName.lastIndexOf("."));
        String generatedName = UUID.randomUUID().toString() + extension;

        File entity = new File();
        entity.setOriginalName(originalName);
        entity.setGeneratedName(generatedName);
        entity.setUser(activeUser);
        entity.setSize(fileSize);
        entity.setCreateTime(now);

        java.io.File dest = new java.io.File(directory + java.io.File.separator + generatedName);

        file.transferTo(dest);
        fileRepository.save(entity);

        return new FileResponseDto(entity.getId(),originalName.substring(0, originalName.lastIndexOf(".")), fileSize, now, now, activeUser.getEmail());
    }

    public List<FileResponseDto> getAll(User activeUser) {
        return fileRepository.findAllByUser(activeUser).stream().map(
            f -> new FileResponseDto(
                f.getId(),
                f.getOriginalName(), 
                f.getSize(), 
                f.getCreateTime(), 
                f.getChangTime(), 
                f.getUser().getEmail()
            )).toList();
    }

    public FileResponseDto findById(UUID id, User activeUser) {
        File file = getRightAccess(activeUser, id);
        return new FileResponseDto(
            file.getId(),
            file.getOriginalName(), 
            file.getSize(), 
            file.getCreateTime(), 
            file.getChangTime(), 
            file.getUser().getEmail()
        );
    }

    @Transactional
    public void deleteFile(UUID id, User activeUser) {
        File deleteFile = getRightAccess(activeUser, id);
        java.io.File fileOnDisk = new java.io.File(directory + java.io.File.separator + deleteFile.getGeneratedName());
        fileOnDisk.delete();
        fileRepository.deleteById(id);
        System.out.println("File deleted successfully!");
    }

    public ResponseEntity<Resource> downloadFile(UUID id, User activeUser) {
        File file = getRightAccess(activeUser, id);
        try {
            Path filePatch = Paths.get(directory).resolve(file.getGeneratedName()).normalize();
            Resource resource = new UrlResource(filePatch.toUri());
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getOriginalName() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    public File getRightAccess(User activeUser, UUID id) {
        File file = fileRepository.findById(id).orElseThrow(() -> new RuntimeException("Access denied!"));
        if (!file.getUser().getId().equals(activeUser.getId())) {
            throw new RuntimeException("File not accessible!");
        }
        return file;
    }
}
