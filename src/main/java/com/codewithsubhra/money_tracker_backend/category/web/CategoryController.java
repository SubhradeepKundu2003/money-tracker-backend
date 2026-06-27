package com.codewithsubhra.money_tracker_backend.category.web;

import com.codewithsubhra.money_tracker_backend.category.CategoryService;
import com.codewithsubhra.money_tracker_backend.category.domain.CategoryType;
import com.codewithsubhra.money_tracker_backend.category.web.dto.CategoryRequest;
import com.codewithsubhra.money_tracker_backend.category.web.dto.CategoryResponse;
import com.codewithsubhra.money_tracker_backend.common.web.ApiResponse;
import com.codewithsubhra.money_tracker_backend.security.AuthPrincipal;
import com.codewithsubhra.money_tracker_backend.user.UserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final UserService userService;

    public CategoryController(CategoryService categoryService, UserService userService) {
        this.categoryService = categoryService;
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> list(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(required = false) CategoryType type) {
        List<CategoryResponse> body = categoryService.list(principal.userId(), type).stream()
                .map(CategoryResponse::from).toList();
        return ApiResponse.ok(body);
    }

    @PostMapping
    public ApiResponse<CategoryResponse> create(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody CategoryRequest request) {
        var user = userService.getById(principal.userId());
        return ApiResponse.ok(CategoryResponse.from(categoryService.create(user, request)));
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> update(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok(CategoryResponse.from(categoryService.update(principal.userId(), id, request)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID id) {
        categoryService.delete(principal.userId(), id);
        return ApiResponse.ok(null);
    }
}
