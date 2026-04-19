package com.learnfast.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class FileUploadController {

    private static final long MAX_BYTES = 20 * 1024 * 1024; // 20 MB
    private static final java.util.Set<String> IMAGE_TYPES = java.util.Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml"
    );

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    HttpSession session) throws IOException {
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Empty file"));
        }
        if (file.getSize() > MAX_BYTES) {
            return ResponseEntity.badRequest().body(Map.of("error", "File too large (max 20 MB)"));
        }

        String original = Paths.get(file.getOriginalFilename()).getFileName().toString();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        String filename = UUID.randomUUID() + ext;

        Path uploadsDir = Paths.get("uploads").toAbsolutePath();
        Files.createDirectories(uploadsDir);
        Files.copy(file.getInputStream(), uploadsDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);

        String contentType = file.getContentType() != null ? file.getContentType() : "";
        String messageType = IMAGE_TYPES.contains(contentType) ? "IMAGE" : "FILE";

        return ResponseEntity.ok(Map.of(
            "url", "/uploads/" + filename,
            "filename", original,
            "messageType", messageType
        ));
    }
}
