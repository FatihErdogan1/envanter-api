package org.example.inventoryapi.model.entity;

import jakarta.persistence.*;
import org.example.inventoryapi.dto.ProductWarehouseStock;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "product_suppliers",
        joinColumns = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "supplier_id")
    )
    private Set<Supplier> suppliers = new HashSet<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(name = "sku", nullable = false, unique = true)
    private String sku;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "quantity_in_stock")
    private int quantityInStock;

    @Transient
    private List<ProductWarehouseStock> warehouseStocks;

    public Product() {}

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }
    public Category getCategory()               { return category; }
    public void setCategory(Category c)         { this.category = c; }
    public Set<Supplier> getSuppliers()              { return suppliers; }
    public void setSuppliers(Set<Supplier> suppliers) { this.suppliers = suppliers; }
    public Warehouse getWarehouse()             { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }
    public String getSku()                      { return sku; }
    public void setSku(String sku)              { this.sku = sku; }
    public String getName()                     { return name; }
    public void setName(String name)            { this.name = name; }
    public BigDecimal getPrice()                    { return price; }
    public void setPrice(BigDecimal price)          { this.price = price; }
    public int getQuantityInStock()             { return quantityInStock; }
    public void setQuantityInStock(int qty)     { this.quantityInStock = qty; }
    public List<ProductWarehouseStock> getWarehouseStocks() { return warehouseStocks; }
    public void setWarehouseStocks(List<ProductWarehouseStock> warehouseStocks) { this.warehouseStocks = warehouseStocks; }
}
