package com.example.ProjectX.service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.ProjectX.dto.FileResponseDto;
import com.example.ProjectX.exception.file.AccessibleRefusedException;
import com.example.ProjectX.exception.file.FileNotFoundException;
import com.example.ProjectX.model.File;
import com.example.ProjectX.model.User;
import com.example.ProjectX.repository.FileRepository;
import com.example.ProjectX.specification.FileSpecification;
import com.example.ProjectX.specification.SearchCriteria;

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

        String size = getCalculatedSize(fileSize);

        return new FileResponseDto(entity.getId(),originalName.substring(0, originalName.lastIndexOf(".")), size, now, now, activeUser.getEmail());
    }

    public List<FileResponseDto> getAll(User activeUser, Long minSize, Long maxSize, String name) {
        Specification<File> spec = (root, query, cb) -> cb.equal(root.get("user"), activeUser);
        spec = filterFiles(spec, minSize, maxSize, name);

        return fileRepository.findAll(spec).stream().map(
            f -> new FileResponseDto(
                f.getId(),
                f.getOriginalName(), 
                getCalculatedSize(f.getSize()), 
                f.getCreateTime(), 
                f.getChangTime(), 
                f.getUser().getEmail()
            )).toList();
    }

    public Specification<File> filterFiles(Specification<File> spec, Long minSize, Long maxSize, String name) {
        if (minSize != null) {
            SearchCriteria criteria = new SearchCriteria("size", ">", minSize);
            FileSpecification fileSpec = new FileSpecification(criteria);
            spec = spec.and(fileSpec);
        }

        if (maxSize != null) {
            SearchCriteria criteria = new SearchCriteria("size", "<", maxSize);
            FileSpecification fileSpec = new FileSpecification(criteria);
            spec = spec.and(fileSpec);
        }

        if (name != null) {
            SearchCriteria criteria = new SearchCriteria("originalName", ":", name);
            FileSpecification fileSpec = new FileSpecification(criteria);
            spec = spec.and(fileSpec);
        }
        return spec;
    }

    public FileResponseDto findById(UUID id, User activeUser) {
        File file = getValidatedFile(activeUser, id);
        String size = getCalculatedSize(file.getSize());
        return new FileResponseDto(
            file.getId(),
            file.getOriginalName(), 
            size, 
            file.getCreateTime(), 
            file.getChangTime(), 
            file.getUser().getEmail()
        );
    }

    @Transactional
    public void deleteFile(UUID id, User activeUser) {
        File deleteFile = getValidatedFile(activeUser, id);
        java.io.File fileOnDisk = new java.io.File(directory + java.io.File.separator + deleteFile.getGeneratedName());
        fileOnDisk.delete();
        fileRepository.deleteById(id);
        System.out.println("File deleted successfully!");
    }

    public ResponseEntity<Resource> downloadFile(UUID id, User activeUser) {
        File file = getValidatedFile(activeUser, id);
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

    public File getValidatedFile(User activeUser, UUID id) {
        File file = fileRepository.findById(id).orElseThrow(() -> new FileNotFoundException("File not found!"));
        if (!file.getUser().getId().equals(activeUser.getId())) {
            throw new AccessibleRefusedException("File access denied!");
        }
        return file;
    }
    
    public String getCalculatedSize(Long size) {
        double result;

        if (size < 1024) {
            return size + "B";
        }

        if ((size / 1024) < 1024) {
            result = size / 1024.0;
            return String.format("%.2f", result) + " KB";
        }

        if ((size / 1048576) < 1024) {
            result = size / 1048576.0;
            return String.format("%.2f", result) + " MB";
        }
        result = size / 1073741824.0;
        return String.format("%.2f", result) + " GB";
    }
}
