package com.webox.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String email;
    @JsonIgnore
    private String password;
    private String name;
    private LocalDateTime createdAt;
}
