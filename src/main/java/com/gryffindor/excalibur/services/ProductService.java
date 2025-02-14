package com.gryffindor.excalibur.services;

import com.gryffindor.excalibur.models.SimpleResponse;
import com.gryffindor.excalibur.models.db.Product;
import com.gryffindor.excalibur.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

  public SimpleResponse<Product> findById(String id) {
    Product product = productRepository.findById(id)
            .orElseThrow(()-> new EntityNotFoundException("Product with id "+id+" not found"));
    return SimpleResponse.<Product>builder()
            .status(HttpStatus.OK)
            .message("Success")
            .content(product)
            .build();
  }

  public SimpleResponse<List<Product>> findAllProduct() {
    List<Product> products = productRepository.findAll();
    if(products.isEmpty()) {
     throw new EntityNotFoundException("Products not found");
    }
      return SimpleResponse.<List<Product>>builder()
              .status(HttpStatus.OK)
              .message("Success")
              .content(products)
              .build();
  }

  @Transactional
  public SimpleResponse<String> addProduct(Product product) {
      Set<ConstraintViolation<Product>> violations = validator.validate(product);
      if (!violations.isEmpty()) {
        throw new ConstraintViolationException(violations);
      }

      productRepository.save(product);
      return SimpleResponse.<String>builder()
              .status(HttpStatus.CREATED)
              .message("Product Added Successfully")
              .build();
  }

  @Transactional
  public SimpleResponse<Product> updateProductById(String id, Product product) {
    Product existingProduct = productRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Product with id "+id+" not found. Record cannot be updated"));

    existingProduct.setName(product.getName());
    existingProduct.setPrice(product.getPrice());
      Product updatedProduct = productRepository.save(existingProduct);

      return SimpleResponse.<Product>builder()
              .status(HttpStatus.OK)
              .message("Product Updated Successfully")
              .content(updatedProduct)
              .build();
  }

  @Transactional
  public SimpleResponse<String> deleteProduct(String id) {
      productRepository.findById(id)
              .orElseThrow(() -> new EntityNotFoundException("Product with id "+id+" not found. Record cannot be deleted"));

      productRepository.deleteById(id);
      return SimpleResponse.<String>builder()
              .status(HttpStatus.OK)
              .message("Product Deleted Successfully")
              .build();
  }
}
