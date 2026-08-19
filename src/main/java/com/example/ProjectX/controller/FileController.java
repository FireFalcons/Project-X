package com.example.ProjectX.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/files")
public class FileController {
    
    @GetMapping()
    public String validation() {
        return "";
    }
}
