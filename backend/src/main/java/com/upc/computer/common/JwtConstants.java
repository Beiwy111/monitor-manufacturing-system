package com.upc.computer.common;

/**
 * JWT 与登录会话常量
 */
public final class JwtConstants {

    private JwtConstants() {
    }

    public static final String JWT_HEADER = "Authorization";

    public static final String JWT_PREFIX = "Bearer ";

    /** Redis 登录会话 key 前缀：computer:login:token:{token} */
    public static final String LOGIN_TOKEN_PREFIX = "computer:login:token:";
}
