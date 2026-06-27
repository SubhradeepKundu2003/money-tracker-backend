package com.codewithsubhra.money_tracker_backend.category.web.dto;

import com.codewithsubhra.money_tracker_backend.category.domain.Category;

public record CategoryResponse(
        String id,
        String name,
        String type,
        String icon,
        String color) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId().toString(),
                category.getName(),
                category.getType().name(),
                category.getIcon(),
                category.getColor());
    }
}
