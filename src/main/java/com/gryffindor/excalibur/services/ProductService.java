package com.gryffindor.excalibur.services;

import com.gryffindor.excalibur.model.db.Product;
import com.gryffindor.excalibur.model.request.ProductRequest;
import com.gryffindor.excalibur.model.response.ProductResponse;
import com.gryffindor.excalibur.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
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

  public ResponseEntity<List<ProductResponse>> findAllProduct() {
    List<Product> products = productRepository.findAll();
    List<ProductResponse> response = products.stream().map(ProductResponse::from).toList();
    return ResponseEntity.ok(response);
  }

  @Transactional
  public ResponseEntity<String> addProduct(ProductRequest productRequest) {
    if (productRepository.findByName(productRequest.getName()).isPresent()) {
      throw new DataIntegrityViolationException(
          "Product with name '" + productRequest.getName() + "' already exists");
    }

    Product product = productRequest.toProduct();
    productRepository.save(product);
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

    existingProduct.setCategory(productRequest.getCategory());
    existingProduct.setName(productRequest.getName());
    existingProduct.setDescription(productRequest.getDescription());
    existingProduct.setImageUrl(productRequest.getImageUrl());
    existingProduct.setHealthBenefits(productRequest.getHealthBenefits());
    existingProduct.setPrice(productRequest.getPrice());
    existingProduct.setQty(productRequest.getQty());
    productRepository.save(existingProduct);

    return new ResponseEntity<>("Product updated successfully", HttpStatus.OK);
  }

  @Transactional
  public ResponseEntity<String> deleteProduct(String id) {
    productRepository
        .findById(id)
        .orElseThrow(
            () ->
                new EntityNotFoundException(
                    "Product with id " + id + " not found. Deletion cannot be performed"));

    productRepository.deleteById(id);
    return new ResponseEntity<>("Product deleted successfully", HttpStatus.OK);
  }
}
