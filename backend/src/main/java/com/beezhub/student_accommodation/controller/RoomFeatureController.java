package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.RoomFeatureRequest;
import com.beezhub.student_accommodation.model.dto.RoomFeatureResponse;
import com.beezhub.student_accommodation.service.RoomFeatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/room-features")
@RequiredArgsConstructor
@Tag(name = "Room Feature", description = "Endpoints for managing room features")
public class RoomFeatureController {

    private final RoomFeatureService roomFeatureService;

    @PostMapping
    @Operation(summary = "Create room feature")
    public ResponseEntity<RoomFeatureResponse> create(@Valid @RequestBody RoomFeatureRequest request) {
        return ResponseEntity.ok(roomFeatureService.create(request));
    }

    @GetMapping
    @Operation(summary = "Get all room features")
    public ResponseEntity<List<RoomFeatureResponse>> getAll() {
        return ResponseEntity.ok(roomFeatureService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get room feature by id")
    public ResponseEntity<RoomFeatureResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(roomFeatureService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update room feature by id")
    public ResponseEntity<RoomFeatureResponse> update(@PathVariable Long id,
            @Valid @RequestBody RoomFeatureRequest request) {
        return ResponseEntity.ok(roomFeatureService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete room feature by id")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roomFeatureService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
