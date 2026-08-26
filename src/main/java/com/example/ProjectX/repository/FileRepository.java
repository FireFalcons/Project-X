package com.example.ProjectX.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ProjectX.model.File;
import com.example.ProjectX.model.User;

public interface FileRepository extends JpaRepository<File, Long>{
    List<File> findAllByUser(User user);
}
