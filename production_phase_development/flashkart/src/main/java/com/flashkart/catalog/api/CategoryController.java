package com.flashkart.catalog.api;

import com.flashkart.catalog.api.dto.CategoryResponse;
import com.flashkart.catalog.service.CategoryService;
import com.flashkart.shared.api.ApiResponse;
import com.flashkart.shared.observability.CorrelationId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Category controller.
 */
@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "Category APIs")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    public ApiResponse<CategoryResponse> getCategory(
            @Parameter(description = "Category ID", required = true)
            @PathVariable UUID id) {
        CategoryResponse category = categoryService.getById(id);
        String requestId = MDC.get(CorrelationId.MDC_KEY);
        return ApiResponse.ok("v1", requestId, category);
    }

    @GetMapping
    @Operation(summary = "Get all active categories")
    public ApiResponse<List<CategoryResponse>> getAllCategories() {
        List<CategoryResponse> categories = categoryService.getAllActiveCategories();
        String requestId = MDC.get(CorrelationId.MDC_KEY);
        return ApiResponse.ok("v1", requestId, categories);
    }
}
