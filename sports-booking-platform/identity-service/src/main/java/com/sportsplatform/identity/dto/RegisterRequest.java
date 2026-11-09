package com.sportsplatform.identity.dto;

import com.sportsplatform.common.enums.RoleEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
        @NotBlank(message = "Số điện thoại không được để trống")
        String phone,

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Định dạng email không hợp lệ")
        String email,

        @NotBlank(message = "Mật khẩu không được để trống")
        String password,

        @NotBlank(message = "Họ tên không được để trống")
        String fullName,

        @NotNull(message = "Vai trò (PLAYER / FACILITY_MANAGER) không được để trống")
        RoleEnum role
) {}