package com.charbel.backend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.charbel.backend.DTO.CreateCategoryRequest;
import com.charbel.backend.DTO.UpdateCategoryRequest;
import com.charbel.backend.model.Category;
import com.charbel.backend.model.CategoryType;
import com.charbel.backend.model.Users;
import com.charbel.backend.repo.CategoryRepo;
import com.charbel.backend.service.CategoryService;
import com.charbel.backend.service.UserService;

@RestController
@RequestMapping("/categories")
public class CategoryController {
    private final CategoryRepo categoryRepo;
    private final CategoryService categoryService;
    private final UserService userService;

    public CategoryController(CategoryRepo categoryRepo, CategoryService categoryService, UserService userService) {
        this.categoryRepo = categoryRepo;
        this.categoryService = categoryService;
        this.userService = userService;
    }

    private Map<String, Object> toMap(Category c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("type", c.getType());
        m.put("parent", c.getParent() != null ? c.getParent().getId() : null);
        m.put("name", c.getName());
        m.put("status", c.getStatus());

        return m;
    }

    private Category resolveParentOrNull (Long parentId, Users user) {
        if(parentId == null) return null;

        return categoryRepo.findByIdAndUser(parentId, user).orElseThrow(() -> new IllegalArgumentException("Catégorie parente introuvable"));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody CreateCategoryRequest req, Authentication auth) {
        Users user = userService.currentUser(auth);
        Category parent = resolveParentOrNull(req.getParentId(), user);

        Category created = categoryService.createCategory(user, req.getType(), parent, req.getName());

        return ResponseEntity.status(201).body(toMap(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @RequestBody UpdateCategoryRequest req, Authentication auth) {
        Users user = userService.currentUser(auth);
        Category parent = resolveParentOrNull(req.getParentId(), user);

        Category updated = categoryService.updateCategory(id, user, parent, req.getName());
        return ResponseEntity.ok(toMap(updated));
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<Map<String, Object>> switchStatus(@PathVariable Long id, Authentication auth) {
        Users user = userService.currentUser(auth);

        Category updated = categoryService.switchStatus(id, user);
        return ResponseEntity.ok(toMap(updated));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(Authentication auth) {
        Users user = userService.currentUser(auth);

        List<Map<String, Object>> out = categoryService.getCategoriesForUser(user).stream().map(this::toMap).toList();

        return ResponseEntity.ok(out);
    }

    @GetMapping("/parents")
    public ResponseEntity<List<Map<String, Object>>> rootParents(@RequestParam(required = false) CategoryType type, @RequestParam(required = false, defaultValue = "true") boolean onlyActive, Authentication auth) {
        Users user = userService.currentUser(auth);

        List<Map<String, Object>> root = categoryService.getRootCategories(user, type, onlyActive).stream().map(
            c -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", c.getId());
                m.put("type", c.getType());
                m.put("name", c.getName());
                return m;
            }
        ).toList();

        return ResponseEntity.ok(root);
    }

    @GetMapping("/children")
    public ResponseEntity<List<Map<String, Object>>> rootChildren(Authentication auth, @RequestParam(defaultValue = "true") boolean onlyActive) {
        Users user = userService.currentUser(auth);

        List<Map<String, Object>> root = categoryService.getChildrenCategories(user, onlyActive).stream().map(
            c -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", c.getId());
                m.put("name", c.getName());
                return m;
            }
        ).toList();

        return ResponseEntity.ok(root);
    }
}
