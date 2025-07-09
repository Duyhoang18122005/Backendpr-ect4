package com.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "address")
    private String address;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(name = "bio")
    private String bio;

    @Column(name = "gender")
    private String gender;

    @Column(nullable = false)
    private java.math.BigDecimal walletBalance = java.math.BigDecimal.ZERO;

    @Column(nullable = false)
    private Long coin = 0L;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> roles = new HashSet<>();

    @Column(name = "device_token")
    private String deviceToken;

    @Lob
    @Column(name = "avatar_data")
    @JsonIgnore
    private byte[] avatarData;

    @Lob
    @Column(name = "profile_image_data")
    private byte[] profileImageData;

    private boolean enabled = true;
    private boolean accountNonExpired = true;
    private boolean credentialsNonExpired = true;
    private boolean accountNonLocked = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @Column(name = "is_online")
    private Boolean isOnline = false;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    public void setAccountNonExpired(boolean accountNonExpired) {
        this.accountNonExpired = accountNonExpired;
    }

    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    public void setCredentialsNonExpired(boolean credentialsNonExpired) {
        this.credentialsNonExpired = credentialsNonExpired;
    }

    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    public void setAccountNonLocked(boolean accountNonLocked) {
        this.accountNonLocked = accountNonLocked;
    }
} 