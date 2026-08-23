package com.gryffindor.excalibur.resources;

import com.gryffindor.excalibur.model.request.ProductRequest;
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
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
    return productService.findAllProduct(page, size);
  }

  @PostMapping("/admin/product")
  public ResponseEntity<String> addProduct(@Valid @RequestBody ProductRequest productRequest) {
    return productService.addProduct(productRequest);
  }

  @PutMapping("/admin/product/{id}")
  public ResponseEntity<String> updateProductById(
      @PathVariable String id, @Valid @RequestBody ProductRequest productRequest) {
    return productService.updateProductById(id, productRequest);
  }

  @PostMapping("/admin/product/{id}/restock")
  public ResponseEntity<String> restockProduct(
      @PathVariable String id,
      @RequestParam @Min(value = 1, message = "Restock quantity must be at least 1") int quantity) {
    return productService.restockProduct(id, quantity);
  }

  @DeleteMapping("/admin/product/{id}")
  public ResponseEntity<String> deleteProductById(@PathVariable String id) {
    return productService.deleteProduct(id);
  }
}
