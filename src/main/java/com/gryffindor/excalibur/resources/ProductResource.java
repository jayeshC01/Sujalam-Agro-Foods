package com.gryffindor.excalibur.resources;

import com.gryffindor.excalibur.model.db.Product;
import com.gryffindor.excalibur.model.request.ProductRequest;
import com.gryffindor.excalibur.model.request.ProductUpdateRequest;
import com.gryffindor.excalibur.model.response.PageResponse;
import com.gryffindor.excalibur.model.response.ProductResponse;
import com.gryffindor.excalibur.services.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@Tag(
    name = "Products",
    description = "Public catalog browsing and admin inventory management endpoints")
public class ProductResource {
  private final ProductService productService;

  @Autowired
  ProductResource(ProductService productService) {
    this.productService = productService;
  }

  @GetMapping("/product/{id}")
  @Operation(
      summary = "Get product by ID",
      description = "Fetches a single active product by its unique ID (public access)")
  public ResponseEntity<ProductResponse> getProductById(
      @Parameter(description = "Product unique identifier", example = "prod_123") @PathVariable
          String id) {
    return productService.findById(id);
  }

  @GetMapping("/products")
  @Operation(
      summary = "Browse active products",
      description =
          "Paginated public product catalog with optional search, category filtering, and sorting")
  public ResponseEntity<PageResponse<ProductResponse>> getAllProducts(
      @Parameter(description = "Filter by product category (EDIBLE, NOT_EDIBLE)")
          @RequestParam(required = false)
          Product.Category category,
      @Parameter(
              description = "Search query matching product name or description",
              example = "groundnut")
          @RequestParam(required = false)
          String q,
      @Parameter(description = "Page number (0-based index)", example = "0")
          @RequestParam(defaultValue = "0")
          @Min(0)
          int page,
      @Parameter(description = "Number of items per page (1-100)", example = "10")
          @RequestParam(defaultValue = "10")
          @Min(1)
          @Max(100)
          int size,
      @Parameter(
              description = "Field to sort by (name, price, qty, createdAt, updatedAt)",
              example = "name")
          @RequestParam(defaultValue = "name")
          String sortBy,
      @Parameter(description = "Sort direction (asc, desc)", example = "asc")
          @RequestParam(defaultValue = "asc")
          String sortDirection) {
    return productService.findAllProduct(category, q, page, size, sortBy, sortDirection);
  }

  @GetMapping("/admin/products")
  @Operation(
      summary = "Admin: List all products",
      description =
          "Paginated product list for administrators, including inactive/archived products")
  public ResponseEntity<PageResponse<ProductResponse>> getAdminProducts(
      @Parameter(description = "Filter by product status (ACTIVE, INACTIVE)")
          @RequestParam(required = false)
          Product.Status status,
      @Parameter(description = "Filter by product category (EDIBLE, NOT_EDIBLE)")
          @RequestParam(required = false)
          Product.Category category,
      @Parameter(description = "Search query matching product name or description")
          @RequestParam(required = false)
          String q,
      @Parameter(description = "Page number (0-based index)", example = "0")
          @RequestParam(defaultValue = "0")
          @Min(0)
          int page,
      @Parameter(description = "Number of items per page (1-100)", example = "10")
          @RequestParam(defaultValue = "10")
          @Min(1)
          @Max(100)
          int size,
      @Parameter(
              description = "Field to sort by (name, price, qty, createdAt, updatedAt)",
              example = "name")
          @RequestParam(defaultValue = "name")
          String sortBy,
      @Parameter(description = "Sort direction (asc, desc)", example = "asc")
          @RequestParam(defaultValue = "asc")
          String sortDirection) {
    return productService.findAdminProducts(status, category, q, page, size, sortBy, sortDirection);
  }

  @GetMapping("/admin/product/{id}")
  @Operation(
      summary = "Admin: Get product by ID",
      description = "Fetches any product by ID, including inactive/archived products")
  public ResponseEntity<ProductResponse> getAdminProductById(
      @Parameter(description = "Product unique identifier", example = "prod_123") @PathVariable
          String id) {
    return productService.findAdminProductById(id);
  }

  @PostMapping("/admin/product")
  @Operation(
      summary = "Admin: Create a new product",
      description = "Adds a new product to the catalog")
  public ResponseEntity<ProductResponse> addProduct(
      @Valid @RequestBody ProductRequest productRequest) {
    return productService.addProduct(productRequest);
  }

  @PutMapping("/admin/product/{id}")
  @Operation(
      summary = "Admin: Update product details",
      description = "Updates an existing product's details and price")
  public ResponseEntity<ProductResponse> updateProductById(
      @Parameter(description = "Product unique identifier", example = "prod_123") @PathVariable
          String id,
      @Valid @RequestBody ProductUpdateRequest productRequest) {
    return productService.updateProductById(id, productRequest);
  }

  @PostMapping("/admin/product/{id}/restock")
  @Operation(
      summary = "Admin: Restock product inventory",
      description = "Increments available stock quantity for a product")
  public ResponseEntity<ProductResponse> restockProduct(
      @Parameter(description = "Product unique identifier", example = "prod_123") @PathVariable
          String id,
      @Parameter(description = "Quantity of units to add to stock (min: 1)", example = "50")
          @RequestParam
          @Min(value = 1, message = "Restock quantity must be at least 1")
          int quantity) {
    return productService.restockProduct(id, quantity);
  }

  @PostMapping("/admin/product/{id}/write-off")
  @Operation(
      summary = "Admin: Write off damaged stock",
      description = "Decrements inventory units due to damage, expiry, or loss")
  public ResponseEntity<ProductResponse> writeOffStock(
      @Parameter(description = "Product unique identifier", example = "prod_123") @PathVariable
          String id,
      @Parameter(description = "Quantity of units to write off (min: 1)", example = "5")
          @RequestParam
          @Min(value = 1, message = "Write-off quantity must be at least 1")
          int quantity) {
    return productService.writeOffStock(id, quantity);
  }

  @DeleteMapping("/admin/product/{id}")
  @Operation(
      summary = "Admin: Deactivate product",
      description = "Soft-deletes a product by setting its status to INACTIVE")
  public ResponseEntity<ProductResponse> deleteProductById(
      @Parameter(description = "Product unique identifier", example = "prod_123") @PathVariable
          String id) {
    return productService.deleteProduct(id);
  }

  @PostMapping("/admin/product/{id}/restore")
  @Operation(
      summary = "Admin: Restore inactive product",
      description = "Restores a soft-deleted product back to ACTIVE status")
  public ResponseEntity<ProductResponse> restoreProductById(
      @Parameter(description = "Product unique identifier", example = "prod_123") @PathVariable
          String id) {
    return productService.restoreProduct(id);
  }
}
