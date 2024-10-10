package com.gryffindor.excalibur.resources;

import com.gryffindor.excalibur.db.Product;
import com.gryffindor.excalibur.services.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/product")
public class ProductResource {
    @Autowired
    private ProductService productService;

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return productService.findById(id);
    }

    @GetMapping("/{name}")
    public ResponseEntity<Product> getProductByName(@PathVariable String name) {
        return productService.findByName(name);
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return productService.findAllProduct();
    }

    @PostMapping
    public ResponseEntity<String> addProduct(@RequestBody Product product) {
        return productService.addProduct(product);
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateProductById(@PathVariable Long id, @RequestBody Product product) {
        return productService.updateProductById(id, product);
    }

    @DeleteMapping
    public ResponseEntity<String> deleteProductById(@PathVariable Long id) {
        return productService.deleteProduct(id);
    }
}
