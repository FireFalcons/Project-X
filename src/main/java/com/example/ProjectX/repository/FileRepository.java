package com.example.ProjectX.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.ProjectX.model.File;
import com.example.ProjectX.model.User;

public interface FileRepository extends JpaRepository<File, UUID>, JpaSpecificationExecutor<File> {
    List<File> findAllByUser(User user);
}
