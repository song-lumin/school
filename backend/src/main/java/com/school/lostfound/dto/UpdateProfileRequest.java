package com.school.lostfound.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String realName;
    private String phone;
    private String email;
}
