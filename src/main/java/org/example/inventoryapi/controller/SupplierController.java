package org.example.inventoryapi.controller;

import org.example.inventoryapi.dto.DeletionErrorResponse;
import org.example.inventoryapi.exception.DeletionBlockedException;
import org.example.inventoryapi.model.entity.Supplier;
import org.example.inventoryapi.service.SupplierService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public List<Supplier> getAll() { return supplierService.getAllSuppliers(); }

    @PostMapping
    public ResponseEntity<?> add(@RequestBody Supplier supplier) {
        try { return ResponseEntity.ok(supplierService.addSupplier(supplier)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody Supplier supplier) {
        try { return ResponseEntity.ok(supplierService.updateSupplier(id, supplier)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable int id) {
        try { supplierService.deleteSupplier(id); return ResponseEntity.ok("Tedarikçi silindi."); }
        catch (DeletionBlockedException e) { return ResponseEntity.status(409).body(DeletionErrorResponse.of(e.getMessage(), e.getCount(), e.getRelatedEntity())); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }
}
