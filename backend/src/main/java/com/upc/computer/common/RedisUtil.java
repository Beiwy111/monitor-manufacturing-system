package com.upc.computer.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类（登录会话等）
 */
@Component
public class RedisUtil {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public void setJson(String key, Object value, long timeoutSeconds) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("Redis 键不能为空");
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            stringRedisTemplate.opsForValue().set(key, json, timeoutSeconds, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            throw new BusinessException("缓存用户信息失败");
        }
    }

    public void setJson(String key, Object value) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("Redis 键不能为空");
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            stringRedisTemplate.opsForValue().set(key, json);
        } catch (JsonProcessingException e) {
            throw new BusinessException("缓存数据失败");
        }
    }

    public <T> T getJson(String key, Class<T> clazz) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        String json = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public <T> T getJson(String key, TypeReference<T> typeReference) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        String json = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public void delete(String key) {
        if (StringUtils.hasText(key)) {
            stringRedisTemplate.delete(key);
        }
    }

    public static String loginTokenKey(String token) {
        return JwtConstants.LOGIN_TOKEN_PREFIX + token;
    }
}
