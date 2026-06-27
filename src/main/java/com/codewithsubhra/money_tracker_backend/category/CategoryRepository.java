package com.codewithsubhra.money_tracker_backend.category;

import com.codewithsubhra.money_tracker_backend.category.domain.Category;
import com.codewithsubhra.money_tracker_backend.category.domain.CategoryType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByUserIdOrderByName(UUID userId);

    List<Category> findByUserIdAndType(UUID userId, CategoryType type);

    Optional<Category> findByIdAndUserId(UUID id, UUID userId);
}
