package com.gryffindor.excalibur.db;

import jakarta.persistence.*;


import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;


@Entity
@Data
@Table(name= "products")
public class Product implements Serializable {
    public enum Category {
        EDIBLE,
        NOT_EDIBLE
    }

    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name="category", nullable = false)
    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(name="name", nullable = false)
    @NotBlank(message = "Name cannot be null. Provide a name field")
    private String name;

    @Column(name="description")
    private String description;


    @Column(name="price", nullable = false)
    @Min(value = 0)
    @NotNull(message = "Price of an item cannot be null or <= 0")
    private Long price;

    @Column(name="quantity", nullable = false)
    @Min(value = 0)
    private Integer qty;
}
