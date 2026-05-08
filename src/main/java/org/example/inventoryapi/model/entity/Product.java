package org.example.inventoryapi.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "Products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private int id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "sku", nullable = false, unique = true)
    private String sku;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "quantity_in_stock")
    private int quantityInStock;

    public Product() {}

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }
    public Category getCategory()               { return category; }
    public void setCategory(Category c)         { this.category = c; }
    public String getSku()                      { return sku; }
    public void setSku(String sku)              { this.sku = sku; }
    public String getName()                     { return name; }
    public void setName(String name)            { this.name = name; }
    public BigDecimal getPrice()                    { return price; }
    public void setPrice(BigDecimal price)          { this.price = price; }
    public int getQuantityInStock()             { return quantityInStock; }
    public void setQuantityInStock(int qty)     { this.quantityInStock = qty; }
}
