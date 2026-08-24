package com.example.ProjectX.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ProjectX.model.File;

public interface FileRepository extends JpaRepository<File, Long>{
}
