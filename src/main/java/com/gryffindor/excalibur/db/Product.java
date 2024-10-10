package com.gryffindor.excalibur.db;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name= "PRODUCT")
public class Product {
    @Id
    @Column(name="ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name="NAME", nullable = false)
    private  String name;

    @Column(name="DESCRIPTION")
    private  String description;

    @Column(name="PRICE", nullable = false)
    private long price;

    @Column(name="QTY", nullable = false)
    private int qty;

    public int id() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String description() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long price() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public int qty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }
}
