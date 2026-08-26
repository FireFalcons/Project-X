package com.example.ProjectX.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
        entity.setName(generatedName);
        entity.setUser(activeUser);
        entity.setSize(fileSize);
        entity.setCreateTime(now);

        java.io.File dest = new java.io.File(directory + java.io.File.separator + generatedName);

        file.transferTo(dest);
        fileRepository.save(entity);

        return new FileResponseDto(generatedName, fileSize, now, now, activeUser.getEmail());
    }

    public List<FileResponseDto> getAll(User activeUser) {
        return fileRepository.findAllByUser(activeUser).stream().map(
            f -> new FileResponseDto(
                f.getName(), 
                f.getSize(), 
                f.getCreateTime(), 
                f.getChangTime(), 
                f.getUser().getEmail()
            )).toList();
    }

    public FileResponseDto findById(Long id, User activeUser) {
        File file = fileRepository.findById(id).orElseThrow(() -> new RuntimeException("File not found"));
        if (!file.getUser().getId().equals(activeUser.getId())) {
            throw new RuntimeException("File not accessible!");
        }
        return new FileResponseDto(
            file.getName(), 
            file.getSize(), 
            file.getCreateTime(), 
            file.getChangTime(), 
            file.getUser().getEmail()
        );
    }

    @Transactional
    public void deleteFile(Long id, User activeUser) {
        File deleteFile = fileRepository.findById(id).orElseThrow(() -> new RuntimeException("File not found"));
        if (!deleteFile.getUser().getId().equals(activeUser.getId())) {
            throw new RuntimeException("File not accessible!");
        }
        java.io.File fileOnDisk = new java.io.File(directory + java.io.File.separator + deleteFile.getName());
        fileOnDisk.delete();
        fileRepository.deleteById(id);
        System.out.println("File deleted successfully!");
    }
}
