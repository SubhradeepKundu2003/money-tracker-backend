package com.codewithsubhra.money_tracker_backend.category;

import com.codewithsubhra.money_tracker_backend.category.domain.Category;
import com.codewithsubhra.money_tracker_backend.category.domain.CategoryType;
import com.codewithsubhra.money_tracker_backend.category.web.dto.CategoryRequest;
import com.codewithsubhra.money_tracker_backend.common.exception.ResourceNotFoundException;
import com.codewithsubhra.money_tracker_backend.user.domain.User;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    /** Categories every new user starts with. */
    private static final List<DefaultCategory> DEFAULTS = List.of(
            new DefaultCategory("Salary", CategoryType.INCOME, "💼", "#2ecc71"),
            new DefaultCategory("Other Income", CategoryType.INCOME, "💰", "#27ae60"),
            new DefaultCategory("Food & Dining", CategoryType.EXPENSE, "🍔", "#e67e22"),
            new DefaultCategory("Groceries", CategoryType.EXPENSE, "🛒", "#f39c12"),
            new DefaultCategory("Transport", CategoryType.EXPENSE, "🚌", "#3498db"),
            new DefaultCategory("Shopping", CategoryType.EXPENSE, "🛍️", "#9b59b6"),
            new DefaultCategory("Bills & Utilities", CategoryType.EXPENSE, "📄", "#e74c3c"),
            new DefaultCategory("Entertainment", CategoryType.EXPENSE, "🎬", "#1abc9c"),
            new DefaultCategory("Health", CategoryType.EXPENSE, "🏥", "#e84393"));

    private final CategoryRepository categories;

    public CategoryService(CategoryRepository categories) {
        this.categories = categories;
    }

    @Transactional
    public void seedDefaultsFor(User user) {
        List<Category> seeded = DEFAULTS.stream().map(d -> {
            Category c = new Category();
            c.setUser(user);
            c.setName(d.name());
            c.setType(d.type());
            c.setIcon(d.icon());
            c.setColor(d.color());
            return c;
        }).toList();
        categories.saveAll(seeded);
    }

    @Transactional(readOnly = true)
    public List<Category> list(UUID userId, CategoryType type) {
        return type == null
                ? categories.findByUserIdOrderByName(userId)
                : categories.findByUserIdAndType(userId, type);
    }

    @Transactional(readOnly = true)
    public Category get(UUID userId, UUID id) {
        return categories.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category"));
    }

    @Transactional
    public Category create(User user, CategoryRequest request) {
        Category category = new Category();
        category.setUser(user);
        apply(category, request);
        return categories.save(category);
    }

    @Transactional
    public Category update(UUID userId, UUID id, CategoryRequest request) {
        Category category = get(userId, id);
        apply(category, request);
        return categories.save(category);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        categories.delete(get(userId, id));
    }

    private void apply(Category category, CategoryRequest request) {
        category.setName(request.name().trim());
        category.setType(request.type());
        category.setIcon(request.icon());
        category.setColor(request.color());
    }

    private record DefaultCategory(String name, CategoryType type, String icon, String color) {
    }
}
