package org.example.inventoryapi.service;

import org.example.inventoryapi.dto.ProductWarehouseStock;
import org.example.inventoryapi.model.entity.InventoryTransaction;
import org.example.inventoryapi.model.entity.Product;
import org.example.inventoryapi.model.entity.Supplier;
import org.example.inventoryapi.model.entity.User;
import org.example.inventoryapi.model.entity.Warehouse;
import org.example.inventoryapi.model.enums.Role;
import org.example.inventoryapi.model.enums.TransactionType;
import org.example.inventoryapi.repository.InventoryTransactionRepository;
import org.example.inventoryapi.repository.ProductRepository;
import org.example.inventoryapi.repository.SupplierRepository;
import org.example.inventoryapi.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final WarehouseRepository warehouseRepository;

    public ProductService(ProductRepository productRepository,
                          SupplierRepository supplierRepository,
                          InventoryTransactionRepository transactionRepository,
                          WarehouseRepository warehouseRepository) {
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.transactionRepository = transactionRepository;
        this.warehouseRepository = warehouseRepository;
    }

    public List<Product> getAllProducts() {
        List<Product> products = productRepository.findAll();
        products.forEach(this::attachWarehouseStocks);
        return products;
    }

    public Product getProduct(int id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ürün bulunamadı."));
        attachWarehouseStocks(product);
        return product;
    }

    @Transactional
    public Product addProduct(Product product, User requestingUser) {
        if (product.getSku() == null || product.getSku().isBlank())
            throw new IllegalArgumentException("SKU zorunludur.");
        if (product.getName() == null || product.getName().isBlank())
            throw new IllegalArgumentException("Ürün adı zorunludur.");
        if (productRepository.existsBySkuIgnoreCase(product.getSku()))
            throw new IllegalArgumentException("Bu SKU zaten kayıtlı.");
        if (product.getPrice() != null && product.getPrice().signum() < 0)
            throw new IllegalArgumentException("Fiyat negatif olamaz.");
        if (product.getQuantityInStock() < 0)
            throw new IllegalArgumentException("Stok miktarı negatif olamaz.");
        product.setSuppliers(resolveSuppliers(product));
        Warehouse warehouse = resolveRequiredWarehouse(product, requestingUser);
        product.setWarehouse(warehouse);
        Product saved = productRepository.save(product);
        if (saved.getQuantityInStock() > 0) {
            InventoryTransaction initialTx = new InventoryTransaction();
            initialTx.setProduct(saved);
            initialTx.setWarehouse(warehouse);
            initialTx.setType(TransactionType.IN);
            initialTx.setQuantity(saved.getQuantityInStock());
            initialTx.setTransactionDate(LocalDateTime.now());
            initialTx.setUser(requestingUser);
            transactionRepository.save(initialTx);
        }
        return saved;
    }

    public Product updateProduct(int id, Product updated, User requestingUser) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ürün bulunamadı."));
        checkProductWarehouseAccess(existing, requestingUser);
        if (productRepository.existsBySkuIgnoreCaseAndIdNot(updated.getSku(), id))
            throw new IllegalArgumentException("Bu SKU başka bir ürüne ait.");
        if (updated.getPrice() != null && updated.getPrice().signum() < 0)
            throw new IllegalArgumentException("Fiyat negatif olamaz.");
        if (updated.getQuantityInStock() < 0)
            throw new IllegalArgumentException("Stok miktarı negatif olamaz.");
        Set<Supplier> suppliers = resolveSuppliers(updated);
        Warehouse warehouse = resolveRequiredWarehouse(updated, requestingUser);
        existing.setSku(updated.getSku());
        existing.setName(updated.getName());
        existing.setCategory(updated.getCategory());
        existing.setSuppliers(suppliers);
        existing.setWarehouse(warehouse);
        existing.setPrice(updated.getPrice());
        existing.setQuantityInStock(updated.getQuantityInStock());
        return productRepository.save(existing);
    }

    public void deleteProduct(int id, User requestingUser) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ürün bulunamadı."));
        checkProductWarehouseAccess(product, requestingUser);
        productRepository.deleteById(id);
    }

    public List<ProductWarehouseStock> getProductStockSummary(int id) {
        productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Ürün bulunamadı."));
        return transactionRepository.findStockByProduct(id);
    }

    public List<Supplier> getProductSuppliers(int id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Ürün bulunamadı."));
        return new ArrayList<>(product.getSuppliers());
    }

    public List<InventoryTransaction> getProductTransactionHistory(int id) {
        productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Ürün bulunamadı."));
        return transactionRepository.findByProduct_IdOrderByTransactionDateDesc(id);
    }

    private Set<Supplier> resolveSuppliers(Product product) {
        if (product.getSuppliers() == null || product.getSuppliers().isEmpty())
            throw new IllegalArgumentException("En az bir tedarikçi seçilmelidir.");
        Set<Supplier> resolved = new HashSet<>();
        for (Supplier s : product.getSuppliers()) {
            if (s.getId() <= 0) throw new IllegalArgumentException("Geçersiz tedarikçi ID.");
            resolved.add(supplierRepository.findById(s.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Tedarikçi bulunamadı: id=" + s.getId())));
        }
        return resolved;
    }

    private Warehouse resolveRequiredWarehouse(Product product, User requestingUser) {
        if (product.getWarehouse() == null || product.getWarehouse().getId() <= 0)
            throw new IllegalArgumentException("Depo seçilmelidir.");
        Warehouse warehouse = warehouseRepository.findById(product.getWarehouse().getId())
                .orElseThrow(() -> new IllegalArgumentException("Depo bulunamadı."));
        checkWarehouseAccess(warehouse, requestingUser);
        return warehouse;
    }

    private void checkProductWarehouseAccess(Product product, User requestingUser) {
        if (requestingUser == null || requestingUser.getRole() == Role.ADMIN) return;
        if (requestingUser.getWarehouse() == null)
            throw new IllegalStateException("Kullanıcının kayıtlı deposu yok.");
        if (product.getWarehouse() != null
                && product.getWarehouse().getId() != requestingUser.getWarehouse().getId())
            throw new IllegalStateException("Bu ürün yalnızca kayıtlı deponuzdan düzenlenebilir.");
    }

    private void checkWarehouseAccess(Warehouse warehouse, User requestingUser) {
        if (requestingUser == null || requestingUser.getRole() == Role.ADMIN) return;
        if (requestingUser.getWarehouse() == null)
            throw new IllegalStateException("Kullanıcının kayıtlı deposu yok.");
        if (warehouse.getId() != requestingUser.getWarehouse().getId())
            throw new IllegalStateException("Yalnızca kayıtlı deponuza ürün ekleyip düzenleyebilirsiniz.");
    }

    private void attachWarehouseStocks(Product product) {
        product.setWarehouseStocks(transactionRepository.findStockByProduct(product.getId()));
    }
}
