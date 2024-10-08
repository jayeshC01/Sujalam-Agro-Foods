package com.gryffindor.excalibur.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name= "PRODUCT")
public class Product {
    @Id
    @Column(name="ID")
    private int id;

    @Column(name="NAME")
    private  String name;

    @Column(name="DESCRIPTION")
    private  String description;

    @Column(name="PRICE")
    private long price;

    @Column(name="QTY")
    private int qty;
}
