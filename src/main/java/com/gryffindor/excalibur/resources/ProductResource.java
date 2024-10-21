package com.gryffindor.excalibur.resources;

import com.gryffindor.excalibur.db.Product;
import com.gryffindor.excalibur.services.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ProductResource {
    private final ProductService productService;

    @Autowired
    ProductResource(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/product/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        return productService.findById(id);
    }

    @GetMapping("/product/name/{name}")
    public ResponseEntity<Product> getProductByName(@PathVariable String name) {
        return productService.findByName(name);
    }

    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAllProducts() {
        return productService.findAllProduct();
    }

    @PostMapping("/admin/product")
    public ResponseEntity<String> addProduct(@RequestBody Product product) {
        return productService.addProduct(product);
    }

    @DeleteMapping("/admin/product/{id}")
    public ResponseEntity<String> deleteProductById(@PathVariable String id) {
        return productService.deleteProduct(id);
    }
}
