package com.amberfilm.auth;

import com.amberfilm.user.UserDto;

public record LoginResponse(String token, UserDto user) {
}

