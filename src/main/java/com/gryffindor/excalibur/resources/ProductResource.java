package com.gryffindor.excalibur.resources;

import com.gryffindor.excalibur.models.SimpleResponse;
import com.gryffindor.excalibur.models.db.Product;
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
    public SimpleResponse<Product> getProductById(@PathVariable String id) {
        return productService.findById(id);
    }

    @GetMapping("/products")
    public SimpleResponse<List<Product>> getAllProducts() {
        return productService.findAllProduct();
    }

    @PostMapping("/admin/product")
    public SimpleResponse<String> addProduct(@RequestBody Product product) {
        return productService.addProduct(product);
    }

    @DeleteMapping("/admin/product/{id}")
    public SimpleResponse<String> deleteProductById(@PathVariable String id) {
        return productService.deleteProduct(id);
    }
}
