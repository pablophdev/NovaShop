package com.pabloph.product_service.repository;

import com.pabloph.product_service.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Product p
            set p.stock = p.stock - :quantity, p.updatedAt = :updatedAt
            where p.id = :id and p.stock >= :quantity
            """)
    int decrementStockIfAvailable(
            @Param("id") Long id,
            @Param("quantity") Integer quantity,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Product p
            set p.stock = p.stock + :quantity, p.updatedAt = :updatedAt
            where p.id = :id
            """)
    int incrementStock(
            @Param("id") Long id,
            @Param("quantity") Integer quantity,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}
