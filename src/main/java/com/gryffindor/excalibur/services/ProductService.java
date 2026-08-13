package com.gryffindor.excalibur.services;

import com.gryffindor.excalibur.model.db.Product;
import com.gryffindor.excalibur.model.request.ProductRequest;
import com.gryffindor.excalibur.model.response.PageResponse;
import com.gryffindor.excalibur.model.response.ProductResponse;
import com.gryffindor.excalibur.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

  public ResponseEntity<ProductResponse> findById(String id) {
    Product product =
        productRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Product with id " + id + " not found"));
    return ResponseEntity.ok(ProductResponse.from(product));
  }

  public ResponseEntity<PageResponse<ProductResponse>> findAllProduct(int page, int size) {
    PageRequest pageRequest = PageRequest.of(page, size);
    Page<Product> products = productRepository.findAll(pageRequest);

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
  public ResponseEntity<String> addProduct(ProductRequest productRequest) {
    if (productRepository.findByName(productRequest.getName()).isPresent()) {
      throw new DataIntegrityViolationException(
          "Product with name '" + productRequest.getName() + "' already exists");
    }

    Product product = productRequest.toProduct();
    productRepository.save(product);
    log.info(
        "Product {} '{}' created with qty {}",
        product.getId(),
        product.getName(),
        product.getQty());
    return new ResponseEntity<>("Product Added successfully", HttpStatus.CREATED);
  }

  @Transactional
  public ResponseEntity<String> updateProductById(String id, ProductRequest productRequest) {
    Product existingProduct =
        productRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Product with id " + id + " not found. Updation cannot be performed"));

    Optional<Product> productWithSameName = productRepository.findByName(productRequest.getName());
    if (productWithSameName.isPresent() && !productWithSameName.get().getId().equals(id)) {
      throw new DataIntegrityViolationException(
          "Product with name '" + productRequest.getName() + "' already exists");
    }

    Integer previousQty = existingProduct.getQty();

    existingProduct.setCategory(productRequest.getCategory());
    existingProduct.setName(productRequest.getName());
    existingProduct.setDescription(productRequest.getDescription());
    existingProduct.setImageUrl(productRequest.getImageUrl());
    existingProduct.setHealthBenefits(productRequest.getHealthBenefits());
    existingProduct.setPrice(productRequest.getPrice());
    existingProduct.setQty(productRequest.getQty());
    productRepository.save(existingProduct);
    log.info(
        "Product {} '{}' updated (qty {} -> {})",
        id,
        existingProduct.getName(),
        previousQty,
        existingProduct.getQty());

    return new ResponseEntity<>("Product updated successfully", HttpStatus.OK);
  }

  @Transactional
  public ResponseEntity<String> deleteProduct(String id) {
    Product product =
        productRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Product with id " + id + " not found. Deletion cannot be performed"));

    productRepository.deleteById(id);
    log.info("Product {} '{}' deleted", id, product.getName());
    return new ResponseEntity<>("Product deleted successfully", HttpStatus.OK);
  }
}
