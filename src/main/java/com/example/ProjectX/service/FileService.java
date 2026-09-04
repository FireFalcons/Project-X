package com.example.ProjectX.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
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
import com.example.ProjectX.exception.file.InvalidSizeFormatException;
import com.example.ProjectX.model.File;
import com.example.ProjectX.model.Role;
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

    public List<FileResponseDto> getAll(
                User activeUser, 
                String minSize, 
                String maxSize, 
                String name, 
                LocalDate dateStart, 
                LocalDate dateEnd,
                LocalDateTime dateTimeStart,
                LocalDateTime dateTimeEnd) {

        Specification<File> spec;

        Long minSizeFile = (minSize != null) ? getConverterSize(minSize) : null;
        Long maxSizeFile = (maxSize != null) ? getConverterSize(maxSize) : null;

        if (activeUser.getRole().equals(Role.USER)) {
            spec = (root, query, cb) -> cb.equal(root.get("user"), activeUser); 
        } else {
            spec = (root, query, cb) -> cb.conjunction();
        }
        
        spec = filterFiles(spec, minSizeFile, maxSizeFile, name, dateStart, dateEnd, dateTimeStart, dateTimeEnd);

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

    public Specification<File> filterFiles(
                Specification<File> spec, 
                Long minSize, 
                Long maxSize, 
                String name, 
                LocalDate dateStart, 
                LocalDate dateEnd, 
                LocalDateTime dateTimeStart, 
                LocalDateTime dateTimeEnd) {

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

        if (dateStart != null) {
            SearchCriteria criteria = new SearchCriteria("createTime", ">", dateStart);
            FileSpecification fileSpec = new FileSpecification(criteria);
            spec = spec.and(fileSpec);
        }

        if (dateEnd != null) {
            SearchCriteria criteria = new SearchCriteria("createTime", "<", dateEnd);
            FileSpecification fileSpec = new FileSpecification(criteria);
            spec = spec.and(fileSpec);
        }

        if (dateTimeStart != null) {
            SearchCriteria criteria = new SearchCriteria("createTime", ">", dateTimeStart);
            FileSpecification fileSpec = new FileSpecification(criteria);
            spec = spec.and(fileSpec);
        }

        if (dateTimeEnd != null) {
            SearchCriteria criteria = new SearchCriteria("createTime", "<", dateTimeEnd);
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
        } catch (MalformedURLException e) {
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
            return String.format("%.2f", result) + "KB";
        }

        if ((size / 1048576) < 1024) {
            result = size / 1048576.0;
            return String.format("%.2f", result) + "MB";
        }
        result = size / 1073741824.0;
        return String.format("%.2f", result) + "GB";
    }

    public Long getConverterSize (String size) {
        String type = size.substring(size.length() - 2).toUpperCase();
        if (!type.matches("\\p{L}+")) {
            throw new InvalidSizeFormatException("Invalid size format!");
        }
        
        Double result = Double.valueOf(size.substring(0, size.length() - 2));

        switch (type) {
            case "KB" -> result *= 1024.0;
            case "MB" -> result *= 1048576.0;
            case "GB" -> result *= 1073741824.0;
        }
        return result.longValue();
    }
}
