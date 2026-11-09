package com.sportsplatform.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateVietQrRequest(
        @NotBlank(message = "Tên ngân hàng không được để trống")
        String bankName,

        @NotBlank(message = "Số tài khoản không được để trống")
        String bankAccountNo,

        @NotBlank(message = "URL ảnh mã VietQR không được để trống")
        String vietqrImageUrl
) {}