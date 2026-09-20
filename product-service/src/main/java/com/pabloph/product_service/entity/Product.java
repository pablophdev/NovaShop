package com.pabloph.product_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable= false) 
    private String name;

    private String description;

    @Column(nullable= false)
    private BigDecimal price;

    @Column(nullable= false)
    private Integer stock;

    @Column(nullable= false)
    private Boolean active;

    @Column(nullable= false)
    private String category;

    @Column(nullable= false)
    private LocalDateTime createdAt;

    @Column(nullable= false)
    private LocalDateTime updatedAt;
}
