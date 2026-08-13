package com.example.ProjectX.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/file")
public class FileController {
    
    @GetMapping("/valid")
    public String validation() {
        return "Token is valid";
    }
}
