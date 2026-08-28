package com.gryffindor.excalibur.services;

import com.gryffindor.excalibur.model.db.Product;
import com.gryffindor.excalibur.model.exception.DuplicateProductException;
import com.gryffindor.excalibur.model.request.ProductRequest;
import com.gryffindor.excalibur.model.request.ProductUpdateRequest;
import com.gryffindor.excalibur.model.response.PageResponse;
import com.gryffindor.excalibur.model.response.ProductResponse;
import com.gryffindor.excalibur.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
  private static final Logger log = LoggerFactory.getLogger(ProductService.class);

  private final ProductRepository productRepository;

  @Autowired
  ProductService(ProductRepository productRepository) {
    this.productRepository = productRepository;
  }

  private Sort createSort(String sortBy, String sortDirection) {
    String property =
        switch (sortBy != null ? sortBy.toLowerCase().trim() : "") {
          case "price" -> "price";
          case "category" -> "category";
          case "createdat", "created_at" -> "createdAt";
          case "qty", "quantity" -> "qty";
          default -> "name";
        };
    Sort.Direction direction =
        "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
    return Sort.by(direction, property);
  }

  @Transactional(readOnly = true)
  public ResponseEntity<ProductResponse> findById(String id) {
    Product product =
        productRepository
            .findById(id)
            .filter(p -> p.getStatus() == Product.Status.ACTIVE)
            .orElseThrow(() -> new EntityNotFoundException("Product with id " + id + " not found"));
    return ResponseEntity.ok(ProductResponse.from(product));
  }

  @Transactional(readOnly = true)
  public ResponseEntity<ProductResponse> findAdminProductById(String id) {
    Product product =
        productRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Product with id " + id + " not found"));
    return ResponseEntity.ok(ProductResponse.from(product));
  }

  @Transactional(readOnly = true)
  public ResponseEntity<PageResponse<ProductResponse>> findAllProduct(
      Product.Category category,
      String query,
      int page,
      int size,
      String sortBy,
      String sortDirection) {
    PageRequest pageRequest = PageRequest.of(page, size, createSort(sortBy, sortDirection));
    Page<Product> products = productRepository.searchPublicProducts(category, query, pageRequest);

    List<ProductResponse> response =
        products.getContent().stream().map(ProductResponse::from).toList();
    PageResponse<ProductResponse> pageResponse =
        PageResponse.<ProductResponse>builder()
            .content(response)
            .page(products.getNumber())
            .size(products.getSize())
            .totalElements(products.getTotalElements())
            .totalPages(products.getTotalPages())
            .first(products.isFirst())
            .last(products.isLast())
            .build();

    return ResponseEntity.ok(pageResponse);
  }

  @Transactional(readOnly = true)
  public ResponseEntity<PageResponse<ProductResponse>> findAdminProducts(
      Product.Status status,
      Product.Category category,
      String query,
      int page,
      int size,
      String sortBy,
      String sortDirection) {
    PageRequest pageRequest = PageRequest.of(page, size, createSort(sortBy, sortDirection));
    Page<Product> products =
        productRepository.searchAdminProducts(status, category, query, pageRequest);

    List<ProductResponse> response =
        products.getContent().stream().map(ProductResponse::from).toList();
    PageResponse<ProductResponse> pageResponse =
        PageResponse.<ProductResponse>builder()
            .content(response)
            .page(products.getNumber())
            .size(products.getSize())
            .totalElements(products.getTotalElements())
            .totalPages(products.getTotalPages())
            .first(products.isFirst())
            .last(products.isLast())
            .build();

    return ResponseEntity.ok(pageResponse);
  }

  @Transactional
  public ResponseEntity<ProductResponse> addProduct(ProductRequest productRequest) {
    Product product = productRequest.toProduct();
    if (product.getName() != null) {
      product.setName(product.getName().trim());
    }
    if (product.getImageUrl() != null) {
      product.setImageUrl(product.getImageUrl().trim());
    }
    if (product.getDescription() != null) {
      product.setDescription(product.getDescription().trim());
    }
    if (product.getHealthBenefits() != null) {
      product.setHealthBenefits(product.getHealthBenefits().trim());
    }

    Product savedProduct;
    try {
      savedProduct = productRepository.saveAndFlush(product);
    } catch (DataIntegrityViolationException ex) {
      log.warn("Duplicate product name constraint violated for '{}'", productRequest.getName());
      throw new DuplicateProductException(
          "Product with name '" + productRequest.getName() + "' already exists");
    }
    log.info(
        "Product {} '{}' created with qty {}",
        savedProduct.getId(),
        savedProduct.getName(),
        savedProduct.getQty());
    return new ResponseEntity<>(ProductResponse.from(savedProduct), HttpStatus.CREATED);
  }

  @Transactional
  public ResponseEntity<ProductResponse> updateProductById(
      String id, ProductUpdateRequest productRequest) {
    Product existingProduct =
        productRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Product with id " + id + " not found. Updation cannot be performed"));

    existingProduct.setCategory(productRequest.getCategory());
    existingProduct.setName(
        productRequest.getName() != null ? productRequest.getName().trim() : null);
    existingProduct.setDescription(
        productRequest.getDescription() != null ? productRequest.getDescription().trim() : null);
    existingProduct.setImageUrl(
        productRequest.getImageUrl() != null ? productRequest.getImageUrl().trim() : null);
    existingProduct.setHealthBenefits(
        productRequest.getHealthBenefits() != null
            ? productRequest.getHealthBenefits().trim()
            : null);
    existingProduct.setPrice(productRequest.getPrice());
    existingProduct.setGstRate(productRequest.getGstRate());

    Product savedProduct;
    try {
      savedProduct = productRepository.saveAndFlush(existingProduct);
    } catch (DataIntegrityViolationException ex) {
      log.warn(
          "Duplicate product name constraint violated when updating '{}'",
          productRequest.getName());
      throw new DuplicateProductException(
          "Product with name '" + productRequest.getName() + "' already exists");
    }
    log.info("Product {} '{}' updated", id, savedProduct.getName());

    return ResponseEntity.ok(ProductResponse.from(savedProduct));
  }

  @Transactional
  public ResponseEntity<ProductResponse> restockProduct(String id, int quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("Restock quantity must be greater than 0");
    }
    int updatedRows = productRepository.incrementStock(id, quantity);
    if (updatedRows == 0) {
      throw new EntityNotFoundException(
          "Product with id " + id + " not found. Restock cannot be performed");
    }
    Product product =
        productRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Product with id " + id + " not found. Restock cannot be performed"));
    log.info(
        "Product {} restocked with {} unit(s), total qty now {}", id, quantity, product.getQty());
    return ResponseEntity.ok(ProductResponse.from(product));
  }

  @Transactional
  public ResponseEntity<ProductResponse> deleteProduct(String id) {
    Product product =
        productRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Product with id " + id + " not found. Deletion cannot be performed"));

    product.setStatus(Product.Status.INACTIVE);
    Product savedProduct = productRepository.save(product);
    log.info("Product {} '{}' marked as inactive", id, savedProduct.getName());
    return ResponseEntity.ok(ProductResponse.from(savedProduct));
  }

  @Transactional
  public ResponseEntity<ProductResponse> restoreProduct(String id) {
    Product product =
        productRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Product with id " + id + " not found. Restoration cannot be performed"));

    product.setStatus(Product.Status.ACTIVE);
    Product savedProduct = productRepository.save(product);
    log.info("Product {} '{}' restored to active", id, savedProduct.getName());
    return ResponseEntity.ok(ProductResponse.from(savedProduct));
  }

  @Transactional
  public ResponseEntity<ProductResponse> writeOffStock(String id, int quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("Write-off quantity must be greater than 0");
    }
    int updatedRows = productRepository.decrementStock(id, quantity);
    if (updatedRows == 0) {
      Product product =
          productRepository
              .findById(id)
              .orElseThrow(
                  () ->
                      new EntityNotFoundException(
                          "Product with id " + id + " not found. Write-off cannot be performed"));
      throw new IllegalArgumentException(
          "Cannot write-off "
              + quantity
              + " unit(s) of product '"
              + product.getName()
              + "'. Current stock is only "
              + product.getQty());
    }
    Product product =
        productRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Product with id " + id + " not found. Write-off cannot be performed"));
    log.info(
        "Product {} '{}' wrote off {} unit(s), total qty now {}",
        id,
        product.getName(),
        quantity,
        product.getQty());
    return ResponseEntity.ok(ProductResponse.from(product));
  }
}
