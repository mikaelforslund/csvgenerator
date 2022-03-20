package com.example.demo;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class Controller {
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> upload(@RequestPart String methods, @RequestPart Map<String, String> metadata, 
    @RequestPart MultipartFile file) { 

        System.out.println(metadata);

        try (Reader reader = new InputStreamReader(file.getInputStream())) {
            String data = FileCopyUtils.copyToString(reader);
            System.out.println(data);

            return ResponseEntity.ok(data);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
