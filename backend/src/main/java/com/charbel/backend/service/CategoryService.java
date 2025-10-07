package com.charbel.backend.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.charbel.backend.model.Category;
import com.charbel.backend.model.CategoryType;
import com.charbel.backend.model.Users;
import com.charbel.backend.repo.CategoryRepo;


@Service
@Transactional
public class CategoryService {
    private final CategoryRepo repo;

    public CategoryService(CategoryRepo repo) {
        this.repo = repo;
    }

    public Category createCategory(Users user, CategoryType type, Category parent, String name) {
        if(user == null) {
            throw new IllegalArgumentException("Utilisateur requis");
        }

        if(type == null && parent == null) {
            throw new IllegalArgumentException("Type ou parent requis");
        }

        if(name == null) {
            throw new IllegalArgumentException("Nom requis");
        }

        name = name.trim();
        if (name.length() < 2 || name.length() > 100) {
            throw new IllegalArgumentException("Le nom doit contenir entre 2 et 100 caractères");
        }

        if(repo.existsByUserAndNameIgnoreCase(user, name)) {
            throw new IllegalArgumentException("Nom déjà existant pour cet utilisateur");
        }

        Category c = new Category();
        c.setUser(user);
        c.setType(type);
        c.setParent(parent);
        c.setName(name);

        return repo.save(c);
    }

    public Category updateCategory(Long id, Users user, Category parent, String name) {
        if(id == null) {
            throw new IllegalArgumentException("Identifiant de la catégorie requis");
        }

        if(user == null) {
            throw new IllegalArgumentException("Utilisateur requis");
        }

        if(name == null) {
            throw new IllegalArgumentException("Nom requis");
        }

        name = name.trim();
        if (name.length() < 2 || name.length() > 100) {
            throw new IllegalArgumentException("Le nom doit contenir entre 2 et 100 caractères");
        }

        Category existing = repo.findByIdAndUser(id, user).orElseThrow(() -> new IllegalArgumentException("Catégorie introuvable"));

        if (parent != null && parent.getId().equals(id)) {
            throw new IllegalArgumentException("Une catégorie ne peut pas être son propre parent");
        }

        if (!existing.getName().equals(name) && repo.existsByUserAndNameIgnoreCase(user, name)) {
            throw new IllegalArgumentException("Nom déjà existant pour cet utilisateur");
        }

        existing.setParent(parent);
        existing.setName(name);

        return repo.save(existing);
    }

    @Transactional(readOnly = true)
    public List<Category> getCategoriesForUser(Users user) {
        Objects.requireNonNull(user, "Utilisateur requis");
        return repo.findByUserOrderByNameAsc(user);
    }

    @Transactional(readOnly = true)
    public List<Category> getRootCategories(Users user, CategoryType type, boolean onlyActive) {
        Objects.requireNonNull(user, "Utilisateur requis");

        if (type == null) {
            return onlyActive
                    ? repo.findByUserAndParentIsNullAndStatusOrderByNameAsc(user, 1)
                    : repo.findByUserAndParentIsNullOrderByNameAsc(user);
        }

        // Construire la liste des types à inclure
        List<CategoryType> types = switch (type) {
            case DEPENSE -> List.of(CategoryType.DEPENSE, CategoryType.LES_2);
            case REVENU  -> List.of(CategoryType.REVENU,  CategoryType.LES_2);
            case LES_2   -> List.of(CategoryType.LES_2);
        };

        return onlyActive
                ? repo.findByUserAndParentIsNullAndStatusAndTypeInOrderByNameAsc(user, 1, types)
                : repo.findByUserAndParentIsNullAndTypeInOrderByNameAsc(user, types);
    }

    @Transactional(readOnly = true)
    public List<Category> getChildrenCategories(Users user, boolean onlyActive) {
        Objects.requireNonNull(user, "Utilisateur requis");
        return onlyActive
                ? repo.findByUserAndParentIsNotNullAndStatusOrderByNameAsc(user, 1)
                : repo.findByUserAndParentIsNotNullOrderByNameAsc(user);
    }

    public Category switchStatus(Long id, Users user) {
        if (id == null) throw new IllegalArgumentException("Identifiant de la catégorie requis");
        if (user == null) throw new IllegalArgumentException("Utilisateur requis");

        Category existing = repo.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Catégorie introuvable"));

        existing.setStatus(existing.getStatus() == 0 ? 1 : 0);
        return repo.save(existing);
    }
}
