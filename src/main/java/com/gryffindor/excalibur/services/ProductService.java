package com.gryffindor.excalibur.services;

import com.gryffindor.excalibur.models.db.Product;
import com.gryffindor.excalibur.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;

@Service
public class ProductService {
  private final ProductRepository productRepository;
  private final Validator validator;

  @Autowired
  ProductService(ProductRepository productRepository, Validator validator) {
    this.productRepository = productRepository;
    this.validator = validator;
  }

  public ResponseEntity<Product> findById(String id) {
    Product product = productRepository.findById(id)
            .orElseThrow(()-> new EntityNotFoundException("Product with id "+id+" not found"));
    return ResponseEntity.ok(product);
  }

  public ResponseEntity<List<Product>> findAllProduct() {
    List<Product> products = productRepository.findAll();
    if(products.isEmpty()) {
     throw new EntityNotFoundException("Products not found");
    }
    return ResponseEntity.ok(products);
  }

  @Transactional
  public ResponseEntity<String> addProduct(Product product) {
      Set<ConstraintViolation<Product>> violations = validator.validate(product);
      if (!violations.isEmpty()) {
        throw new ConstraintViolationException(violations);
      }

      productRepository.save(product);
      return new ResponseEntity<>("Product Added successfully", HttpStatus.CREATED);
  }

  @Transactional
  public ResponseEntity<String> updateProductById(String id, Product product) {
    Product existingProduct = productRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Product with id "+id+" not found. Updation cannot be performed"));

    existingProduct.setName(product.getName());
    existingProduct.setPrice(product.getPrice());
    productRepository.save(existingProduct);

    return new ResponseEntity<>("Product updated successfully", HttpStatus.OK);
  }

  @Transactional
  public ResponseEntity<String> deleteProduct(String id) {
      productRepository.findById(id)
              .orElseThrow(() -> new EntityNotFoundException("Product with id "+id+" not found. Deletion cannot be performed"));

      productRepository.deleteById(id);
      return new ResponseEntity<>("Product deleted successfully", HttpStatus.OK);
  }
}
