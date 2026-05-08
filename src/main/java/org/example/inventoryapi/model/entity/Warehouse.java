package org.example.inventoryapi.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Warehouses")
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warehouse_id")
    private int id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "location_address")
    private String locationAddress;

    public Warehouse() {}

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }
    public String getName()                         { return name; }
    public void setName(String name)                { this.name = name; }
    public String getLocationAddress()              { return locationAddress; }
    public void setLocationAddress(String addr)     { this.locationAddress = addr; }
}
