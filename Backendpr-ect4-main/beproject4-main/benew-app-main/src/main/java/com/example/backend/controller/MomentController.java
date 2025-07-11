package com.example.backend.controller;

import com.example.backend.dto.MomentDTO;
import com.example.backend.dto.CreateMomentRequest;
import com.example.backend.service.MomentService;
import com.example.backend.service.UserService;
import com.example.backend.service.FileStorageService;
import com.example.backend.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/moments")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Moment", description = "Moment management APIs")
public class MomentController {

    @Autowired
    private MomentService momentService;

    @Autowired
    private UserService userService;

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostMapping(value = "/{gamePlayerId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a new moment with JSON data")
    public ResponseEntity<MomentDTO> createMoment(
            @PathVariable Long gamePlayerId,
            @RequestBody CreateMomentRequest request,
            Authentication authentication) {
        
        User user = userService.findByUsername(authentication.getName());
        
        // Kiểm tra xem user có phải là owner của game player không
        if (!momentService.isGamePlayerOwner(gamePlayerId, user.getId())) {
            return ResponseEntity.status(403).body(null);
        }
        
        MomentDTO moment = momentService.createMoment(gamePlayerId, request);
        return ResponseEntity.ok(moment);
    }

    @PostMapping("/{gamePlayerId}/upload")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a new moment with file upload")
    public ResponseEntity<MomentDTO> createMomentWithFile(
            @PathVariable Long gamePlayerId,
            @RequestParam("content") String content,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            Authentication authentication) throws IOException {
        
        System.out.println("=== BẮT ĐẦU: createMomentWithFile ===");
        System.out.println("gamePlayerId: " + gamePlayerId);
        System.out.println("content: " + content);
        System.out.println("imageFile: " + (imageFile != null ? imageFile.getOriginalFilename() : "null"));
        System.out.println("authentication: " + authentication.getName());
        
        try {
            User user = userService.findByUsername(authentication.getName());
            System.out.println("User found: " + user.getUsername() + " (ID: " + user.getId() + ")");
            
            // Kiểm tra xem user có phải là owner của game player không
            boolean isOwner = momentService.isGamePlayerOwner(gamePlayerId, user.getId());
            System.out.println("Is owner: " + isOwner);
            
            if (!isOwner) {
                System.out.println("User is not owner of game player");
                return ResponseEntity.status(403).body(null);
            }
            
            if (content.trim().isEmpty()) {
                System.out.println("Content is empty");
                return ResponseEntity.badRequest().body(null);
            }
            
            CreateMomentRequest request = new CreateMomentRequest();
            request.setContent(content.trim());
            
            List<String> imageUrls = new ArrayList<>();
            
            // Upload image if provided
            if (imageFile != null && !imageFile.isEmpty()) {
                System.out.println("Processing image file...");
                
                // Validate file type
                String contentType = imageFile.getContentType();
                String originalFilename = imageFile.getOriginalFilename();
                System.out.println("Content type: " + contentType);
                System.out.println("Original filename: " + originalFilename);
                
                // Check content type or file extension
                boolean isValidImage = false;
                if (contentType != null && contentType.startsWith("image/")) {
                    isValidImage = true;
                } else if (originalFilename != null) {
                    String extension = originalFilename.toLowerCase();
                    if (extension.endsWith(".jpg") || extension.endsWith(".jpeg") || 
                        extension.endsWith(".png") || extension.endsWith(".gif") || 
                        extension.endsWith(".bmp") || extension.endsWith(".webp")) {
                        isValidImage = true;
                    }
                }
                
                if (!isValidImage) {
                    System.out.println("Invalid file type");
                    return ResponseEntity.badRequest().body(null);
                }
                
                // Validate file size (max 5MB)
                long fileSize = imageFile.getSize();
                System.out.println("File size: " + fileSize + " bytes");
                
                if (fileSize > 5 * 1024 * 1024) {
                    System.out.println("File too large");
                    return ResponseEntity.badRequest().body(null);
                }
                
                String fileName = fileStorageService.storeFile(imageFile, "moment-images");
                System.out.println("File stored: " + fileName);
                imageUrls.add(fileName);
            } else {
                System.out.println("No image file provided");
            }
            
            request.setImageUrls(imageUrls);
            System.out.println("Request prepared: " + request);
            
            MomentDTO moment = momentService.createMoment(gamePlayerId, request);
            System.out.println("Moment created successfully: " + moment.getId());
            System.out.println("=== KẾT THÚC: createMomentWithFile ===");
            
            return ResponseEntity.ok(moment);
            
        } catch (Exception e) {
            System.err.println("=== LỖI: createMomentWithFile ===");
            System.err.println("Exception: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @GetMapping("/{momentId}")
    @Operation(summary = "Get moment by ID")
    public ResponseEntity<MomentDTO> getMomentById(@PathVariable Long momentId) {
        MomentDTO moment = momentService.getMomentById(momentId);
        return ResponseEntity.ok(moment);
    }

    @GetMapping("/player/{gamePlayerId}")
    @Operation(summary = "Get moments by game player ID")
    public ResponseEntity<Page<MomentDTO>> getMomentsByGamePlayerId(
            @PathVariable Long gamePlayerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<MomentDTO> moments = momentService.getMomentsByGamePlayerId(gamePlayerId, pageable);
        return ResponseEntity.ok(moments);
    }

    @GetMapping("/my-moments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user's moments")
    public ResponseEntity<Page<MomentDTO>> getMyMoments(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        User user = userService.findByUsername(authentication.getName());
        Pageable pageable = PageRequest.of(page, size);
        Page<MomentDTO> moments = momentService.getMyMoments(user.getId(), pageable);
        return ResponseEntity.ok(moments);
    }

    @GetMapping("/feed")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get moment feed (moments from followed players)")
    public ResponseEntity<Page<MomentDTO>> getMomentFeed(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        User user = userService.findByUsername(authentication.getName());
        Pageable pageable = PageRequest.of(page, size);
        Page<MomentDTO> moments = momentService.getMomentFeed(user.getId(), pageable);
        return ResponseEntity.ok(moments);
    }

    @GetMapping("/all")
    @Operation(summary = "Get all moments (public, paginated)")
    public ResponseEntity<Page<MomentDTO>> getAllMoments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MomentDTO> moments = momentService.getAllMoments(pageable);
        return ResponseEntity.ok(moments);
    }

    @PutMapping("/{momentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update moment")
    public ResponseEntity<MomentDTO> updateMoment(
            @PathVariable Long momentId,
            @RequestBody CreateMomentRequest request,
            Authentication authentication) {
        
        User user = userService.findByUsername(authentication.getName());
        MomentDTO moment = momentService.updateMoment(momentId, user.getId(), request);
        return ResponseEntity.ok(moment);
    }

    @DeleteMapping("/{momentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete moment")
    public ResponseEntity<?> deleteMoment(
            @PathVariable Long momentId,
            Authentication authentication) {
        
        User user = userService.findByUsername(authentication.getName());
        momentService.deleteMoment(momentId, user.getId());
        return ResponseEntity.ok("Moment deleted successfully");
    }

    @PatchMapping("/{momentId}/toggle-visibility")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Toggle moment visibility")
    public ResponseEntity<?> toggleMomentVisibility(
            @PathVariable Long momentId,
            Authentication authentication) {
        
        User user = userService.findByUsername(authentication.getName());
        momentService.toggleMomentVisibility(momentId, user.getId());
        return ResponseEntity.ok("Moment visibility toggled successfully");
    }

    @GetMapping("/moment-images/{filename:.+}")
    @Operation(summary = "Get moment image by filename")
    public ResponseEntity<Resource> getMomentImage(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve("moment-images").resolve(filename);
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                // Determine content type based on file extension
                String contentType = "image/jpeg"; // default
                if (filename.toLowerCase().endsWith(".png")) {
                    contentType = "image/png";
                } else if (filename.toLowerCase().endsWith(".gif")) {
                    contentType = "image/gif";
                } else if (filename.toLowerCase().endsWith(".webp")) {
                    contentType = "image/webp";
                } else if (filename.toLowerCase().endsWith(".bmp")) {
                    contentType = "image/bmp";
                }
                
                return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
} 