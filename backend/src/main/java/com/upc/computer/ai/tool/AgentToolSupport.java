package com.upc.computer.ai.tool;

import com.upc.computer.common.BusinessException;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

final class AgentToolSupport {

    private AgentToolSupport() {
    }

    static <T> List<T> limit(Collection<T> values, int max) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream().limit(max).toList();
    }

    static String requiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(fieldName + "不能为空");
        }
        return value.trim();
    }

    static Long requiredId(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new BusinessException(fieldName + "必须是正整数");
        }
        return value;
    }

    static LocalDate requiredDate(String value, String fieldName) {
        try {
            return LocalDate.parse(requiredText(value, fieldName));
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(fieldName + "格式必须为 yyyy-MM-dd");
        }
    }
}
