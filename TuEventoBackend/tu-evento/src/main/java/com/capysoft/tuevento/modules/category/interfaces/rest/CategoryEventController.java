package com.capysoft.tuevento.modules.category.interfaces.rest;

import com.capysoft.tuevento.modules.category.application.dto.request.AssignCategoryRequest;
import com.capysoft.tuevento.modules.category.application.dto.response.CategoryEventResponse;
import com.capysoft.tuevento.modules.category.application.dto.response.CategoryResponse;
import com.capysoft.tuevento.modules.category.application.port.in.CategoryEventUseCase;
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
@RequestMapping("/api/v1/category-events")
@RequiredArgsConstructor
@Tag(name = "Category Events", description = "Category-to-event assignment endpoints")
public class CategoryEventController {

    private final CategoryEventUseCase categoryEventUseCase;

    @Operation(summary = "Assign a category to an event — requires ORGANIZER or ADMIN")
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryEventResponse>> assign(
            @Valid @RequestBody AssignCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Category assigned to event successfully",
                        categoryEventUseCase.assignCategoryToEvent(request)));
    }

    @Operation(summary = "Remove a category from an event — requires ORGANIZER or ADMIN")
    @DeleteMapping("/{categoryEventId}")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable Integer categoryEventId) {
        categoryEventUseCase.removeCategoryFromEvent(categoryEventId);
        return ResponseEntity.ok(ApiResponse.ok("Category removed from event successfully"));
    }

    @Operation(summary = "Remove all categories from an event — requires ORGANIZER or ADMIN")
    @DeleteMapping("/event/{eventId}")
    public ResponseEntity<ApiResponse<Void>> removeAll(@PathVariable Integer eventId) {
        categoryEventUseCase.removeAllCategoriesFromEvent(eventId);
        return ResponseEntity.ok(ApiResponse.ok("All categories removed from event successfully"));
    }

    @Operation(summary = "Get categories assigned to an event — public")
    @GetMapping("/event/{eventId}")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getByEvent(
            @PathVariable Integer eventId) {
        return ResponseEntity.ok(ApiResponse.ok("Categories retrieved",
                categoryEventUseCase.getCategoriesByEvent(eventId)));
    }

    @Operation(summary = "Get event IDs that have a given category — public")
    @GetMapping("/category/{categoryId}/events")
    public ResponseEntity<ApiResponse<List<Integer>>> getEventsByCategory(
            @PathVariable Integer categoryId) {
        return ResponseEntity.ok(ApiResponse.ok("Event IDs retrieved",
                categoryEventUseCase.getEventIdsByCategory(categoryId)));
    }
}
