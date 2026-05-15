package com.capysoft.tuevento.modules.category.interfaces.rest;

import com.capysoft.tuevento.modules.category.application.dto.request.CreateCategoryRequest;
import com.capysoft.tuevento.modules.category.application.dto.request.UpdateCategoryRequest;
import com.capysoft.tuevento.modules.category.application.dto.response.CategoryResponse;
import com.capysoft.tuevento.modules.category.application.port.in.CategoryUseCase;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Event category management endpoints")
public class CategoryController {

    private final CategoryUseCase categoryUseCase;

    @Operation(summary = "Create a category — requires ADMIN")
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Category created successfully",
                        categoryUseCase.createCategory(request)));
    }

    @Operation(summary = "Update a category — requires ADMIN")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Category updated successfully",
                categoryUseCase.updateCategory(id, request)));
    }

    @Operation(summary = "Deactivate a category — requires ADMIN")
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Integer id) {
        categoryUseCase.deactivateCategory(id);
        return ResponseEntity.ok(ApiResponse.ok("Category deactivated successfully"));
    }

    @Operation(summary = "Activate a category — requires ADMIN")
    @PutMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable Integer id) {
        categoryUseCase.activateCategory(id);
        return ResponseEntity.ok(ApiResponse.ok("Category activated successfully"));
    }

    @Operation(summary = "Get category by ID — public")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok("Category retrieved",
                categoryUseCase.getCategoryById(id)));
    }

    @Operation(summary = "Get all categories — public")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok("Categories retrieved",
                categoryUseCase.getAllCategories()));
    }

    @Operation(summary = "Get active categories — public")
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getActive() {
        return ResponseEntity.ok(ApiResponse.ok("Active categories retrieved",
                categoryUseCase.getActiveCategories()));
    }

    @Operation(summary = "Get root categories — public")
    @GetMapping("/root")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getRoot() {
        return ResponseEntity.ok(ApiResponse.ok("Root categories retrieved",
                categoryUseCase.getRootCategories()));
    }

    @Operation(summary = "Get subcategories of a parent — public")
    @GetMapping("/{id}/subcategories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getSubcategories(
            @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok("Subcategories retrieved",
                categoryUseCase.getSubcategories(id)));
    }
}
