package com.gryffindor.excalibur.services;

import com.gryffindor.excalibur.db.Product;
import com.gryffindor.excalibur.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

  @Autowired
  private ProductRepository productRepository;

  ProductService() {}

  public ResponseEntity<Product> findById(Long id) {
    Product product = productRepository.findById(id).orElse(null);
    if (product == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(product);
  }

  public ResponseEntity<Product> findByName(String name) {
    Product product = productRepository.findByName(name).orElse(null);
    if(product == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(product);
  }

  public ResponseEntity<List<Product>> findAllProduct() {
    List<Product> products = productRepository.findAll();
    if(products.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(products);
  }

  public ResponseEntity<String> addProduct(Product product) {
    Product newProduct = productRepository.save(product);
    return new ResponseEntity<>("Product Added successfully", HttpStatus.CREATED);
  }

  public ResponseEntity<String> updateProductById(Long id, Product product) {
    Product existingProduct = productRepository.findById(id).orElse(null);
    if (existingProduct == null)
    {
      return new ResponseEntity<>("Product not found", HttpStatus.NOT_FOUND);
    }

    existingProduct.setName(product.getName());
    existingProduct.setPrice(product.getPrice());
    productRepository.save(existingProduct);

    return new ResponseEntity<>("Product updated successfully", HttpStatus.OK);
  }

  @Transactional
  public ResponseEntity<String> deleteProduct(Long id) {
    try {
      Product deletedProduct = productRepository.findById(id).orElse(null);
      if (deletedProduct == null) {
        return ResponseEntity.notFound().build();
      }
      productRepository.deleteById(id);
      return new ResponseEntity<>("Product deleted successfully", HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
