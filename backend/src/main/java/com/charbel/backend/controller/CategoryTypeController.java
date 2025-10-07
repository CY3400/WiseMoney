package com.charbel.backend.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.charbel.backend.model.CategoryType;

@RestController
@RequestMapping("/category-types")
public class CategoryTypeController {
    @GetMapping
    public ResponseEntity<List<String>> list() {
        return ResponseEntity.ok(
            Arrays.stream(CategoryType.values())
                .map(Enum::name)
                .toList()
        );
    }
}
