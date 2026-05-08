package org.example.inventoryapi.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

@Entity
@Table(name = "Suppliers")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_id")
    private int id;

    @Column(name = "name", nullable = false)
    private String name;

    @JsonProperty("email")
    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "phone", unique = true)
    private String phone;

    @Column(name = "address")
    private String address;

    public Supplier() {}

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }
    public String getName()                         { return name; }
    public void setName(String name)                { this.name = name; }
    @JsonProperty("email")
    public String getContactEmail()                 { return contactEmail; }
    public void setContactEmail(String email)       { this.contactEmail = email; }
    public String getPhone()                        { return phone; }
    public void setPhone(String phone)              { this.phone = phone; }
    public String getAddress()                      { return address; }
    public void setAddress(String address)          { this.address = address; }
}
