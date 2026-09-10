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
        
        String extension = originalName.substring(originalName.lastIndexOf(".") + 1);
        originalName = originalName.substring(0, originalName.lastIndexOf("."));
        UUID fileId = UUID.randomUUID();

        File entity = new File();
        entity.setId(fileId);
        entity.setName(originalName);
        entity.setSysfName(fileId);
        entity.setExtension(extension);
        entity.setUserId(activeUser.getId());
        entity.setSize(fileSize);
        entity.setCreateTime(now);

        java.io.File dest = new java.io.File(directory + java.io.File.separator + fileId.toString() + extension);

        file.transferTo(dest);
        fileRepository.save(entity);

        String size = getCalculatedSize(fileSize);

        return new FileResponseDto(entity.getId(), originalName, extension, size, now, now, activeUser.getId());
    }

    public List<FileResponseDto> getAll(
                User activeUser, 
                String minSize, 
                String maxSize,
                String typeSize, 
                String name,
                String extension, 
                LocalDate dateStart, 
                LocalDate dateEnd,
                LocalDateTime dateTimeStart,
                LocalDateTime dateTimeEnd) {

        Specification<File> spec;

        Long minSizeFile = (minSize != null) ? getConverterSize(minSize) : null;
        Long maxSizeFile = (maxSize != null) ? getConverterSize(maxSize) : null;

        if (activeUser.getRole().equals(Role.USER)) {
            spec = (root, query, cb) -> cb.equal(root.get("userId"), activeUser.getId()); 
        } else {
            spec = (root, query, cb) -> cb.conjunction();
        }
        
        spec = filterFiles(spec, minSizeFile, maxSizeFile, name, extension, dateStart, dateEnd, dateTimeStart, dateTimeEnd);

        return fileRepository.findAll(spec).stream().map(
            f -> new FileResponseDto(
                f.getId(),
                f.getName(),
                f.getExtension(), 
                typeSize != null ? getFormattedSizeInUnit(f.getSize(), typeSize) : getCalculatedSize(f.getSize()), 
                f.getCreateTime(), 
                f.getChangTime(), 
                f.getUserId()
            )).toList();
    }

    public Specification<File> filterFiles(
                Specification<File> spec, 
                Long minSize, 
                Long maxSize,
                String name,
                String extension, 
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
            SearchCriteria criteria = new SearchCriteria("name", ":", name);
            FileSpecification fileSpec = new FileSpecification(criteria);
            spec = spec.and(fileSpec);
        }

        if (extension != null) {
            SearchCriteria criteria = new SearchCriteria("extension", ":", extension);
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
            file.getName(),
            file.getExtension(), 
            size, 
            file.getCreateTime(), 
            file.getChangTime(), 
            file.getUserId()
        );
    }

    @Transactional
    public void deleteFile(UUID id, User activeUser) {
        File deleteFile = getValidatedFile(activeUser, id);
        java.io.File fileOnDisk = new java.io.File(directory + java.io.File.separator + deleteFile.getSysfName());
        fileOnDisk.delete();
        fileRepository.deleteById(id);
        System.out.println("File deleted successfully!");
    }

    public ResponseEntity<Resource> downloadFile(UUID id, User activeUser) {
        File file = getValidatedFile(activeUser, id);
        try {
            Path filePatch = Paths.get(directory).resolve(file.getSysfName().toString()).normalize();
            Resource resource = new UrlResource(filePatch.toUri());
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getSysfName() + "\"")
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
        if (file.getUserId().equals(activeUser.getId()) || activeUser.getRole().equals(Role.ADMIN)) {
            return file;
        }
        throw new AccessibleRefusedException("File access denied!");
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
        try{
            Double result = 0.0;
            if (size.trim().length() < 3) {
                return Long.valueOf(size);
            }
    
            String type = size.substring(size.length() - 2).toUpperCase();
            if (type.matches("\\p{L}+")) {
                if (size.contains(".")) {
                    if (size.substring(size.indexOf(".") + 1).length() > 4) {
                        throw new InvalidSizeFormatException("Maximum number of fractional digits is 2!");
                    }
                } 
                result = Double.valueOf(size.substring(0, size.length() - 2));
                switch (type) {
                    case "BB" -> result.longValue();
                    case "KB" -> result *= 1024.0;
                    case "MB" -> result *= 1048576.0;
                    case "GB" -> result *= 1073741824.0;
                    default -> throw new InvalidSizeFormatException("Invalid type format! Expected format: BB, KB, MB, GB");
                }
            } else {
                result = Double.valueOf(size);
            }
            return result.longValue();
        } catch (NumberFormatException ex) {
            throw new InvalidSizeFormatException("Incorrectly set filter parameter!");
        }
    }

    public String getFormattedSizeInUnit(Long sizeInBytes, String targetUtil) {
        Double result = 0.0;
        switch (targetUtil.toUpperCase()) {
            case "BB" -> result = Double.valueOf(sizeInBytes);
            case "KB" -> result = sizeInBytes / 1024.0;
            case "MB" -> result = sizeInBytes / 1048576.0;
            case "GB" -> result = sizeInBytes / 1073741824.0;
            default ->  throw new InvalidSizeFormatException("Invalid type format! Expected format: BB, KB, MB, GB");
        }

        return result + targetUtil;
    }
}
