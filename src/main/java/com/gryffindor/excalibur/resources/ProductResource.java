package com.gryffindor.excalibur.resources;

import com.gryffindor.excalibur.model.db.Product;
import com.gryffindor.excalibur.model.request.ProductRequest;
import com.gryffindor.excalibur.model.request.ProductUpdateRequest;
import com.gryffindor.excalibur.model.response.PageResponse;
import com.gryffindor.excalibur.model.response.ProductResponse;
import com.gryffindor.excalibur.services.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
public class ProductResource {
  private final ProductService productService;

  @Autowired
  ProductResource(ProductService productService) {
    this.productService = productService;
  }

  @GetMapping("/product/{id}")
  public ResponseEntity<ProductResponse> getProductById(@PathVariable String id) {
    return productService.findById(id);
  }

  @GetMapping("/products")
  public ResponseEntity<PageResponse<ProductResponse>> getAllProducts(
      @RequestParam(required = false) Product.Category category,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
      @RequestParam(defaultValue = "name") String sortBy,
      @RequestParam(defaultValue = "asc") String sortDirection) {
    return productService.findAllProduct(category, q, page, size, sortBy, sortDirection);
  }

  @GetMapping("/admin/products")
  public ResponseEntity<PageResponse<ProductResponse>> getAdminProducts(
      @RequestParam(required = false) Product.Status status,
      @RequestParam(required = false) Product.Category category,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
      @RequestParam(defaultValue = "name") String sortBy,
      @RequestParam(defaultValue = "asc") String sortDirection) {
    return productService.findAdminProducts(status, category, q, page, size, sortBy, sortDirection);
  }

  @GetMapping("/admin/product/{id}")
  public ResponseEntity<ProductResponse> getAdminProductById(@PathVariable String id) {
    return productService.findAdminProductById(id);
  }

  @PostMapping("/admin/product")
  public ResponseEntity<ProductResponse> addProduct(
      @Valid @RequestBody ProductRequest productRequest) {
    return productService.addProduct(productRequest);
  }

  @PutMapping("/admin/product/{id}")
  public ResponseEntity<ProductResponse> updateProductById(
      @PathVariable String id, @Valid @RequestBody ProductUpdateRequest productRequest) {
    return productService.updateProductById(id, productRequest);
  }

  @PostMapping("/admin/product/{id}/restock")
  public ResponseEntity<ProductResponse> restockProduct(
      @PathVariable String id,
      @RequestParam @Min(value = 1, message = "Restock quantity must be at least 1") int quantity) {
    return productService.restockProduct(id, quantity);
  }

  @PostMapping("/admin/product/{id}/write-off")
  public ResponseEntity<ProductResponse> writeOffStock(
      @PathVariable String id,
      @RequestParam @Min(value = 1, message = "Write-off quantity must be at least 1")
          int quantity) {
    return productService.writeOffStock(id, quantity);
  }

  @DeleteMapping("/admin/product/{id}")
  public ResponseEntity<ProductResponse> deleteProductById(@PathVariable String id) {
    return productService.deleteProduct(id);
  }

  @PostMapping("/admin/product/{id}/restore")
  public ResponseEntity<ProductResponse> restoreProductById(@PathVariable String id) {
    return productService.restoreProduct(id);
  }
}
