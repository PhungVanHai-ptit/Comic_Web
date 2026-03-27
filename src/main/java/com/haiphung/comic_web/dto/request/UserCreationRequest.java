package com.haiphung.comic_web.dto.request;

import com.haiphung.comic_web.entity.Role;
import jakarta.persistence.Column;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {

    String email; // dùng email làm định danh chính
    String password;
    String fullName;

}
