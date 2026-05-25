package org.example.inventoryapi.dto;

public class AuthResponse {
    private String token;
    private String username;
    private String role;
    private boolean forcePasswordChange;
    private Integer warehouseId;
    private String warehouseName;
    private Integer supplierId;
    private String supplierName;

    public AuthResponse(String token, String username, String role, boolean forcePasswordChange,
                        Integer warehouseId, String warehouseName,
                        Integer supplierId, String supplierName) {
        this.token               = token;
        this.username            = username;
        this.role                = role;
        this.forcePasswordChange = forcePasswordChange;
        this.warehouseId         = warehouseId;
        this.warehouseName       = warehouseName;
        this.supplierId          = supplierId;
        this.supplierName        = supplierName;
    }

    public String getToken()                { return token; }
    public String getUsername()             { return username; }
    public String getRole()                 { return role; }
    public boolean isForcePasswordChange()  { return forcePasswordChange; }
    public Integer getWarehouseId()         { return warehouseId; }
    public String getWarehouseName()        { return warehouseName; }
    public Integer getSupplierId()          { return supplierId; }
    public String getSupplierName()         { return supplierName; }
}
