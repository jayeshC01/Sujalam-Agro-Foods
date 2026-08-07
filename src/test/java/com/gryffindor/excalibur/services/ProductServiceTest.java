package com.gryffindor.excalibur.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gryffindor.excalibur.models.db.Product;
import com.gryffindor.excalibur.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

  @Mock private ProductRepository productRepository;

  @Mock private Validator validator;

  private ProductService productService;

  private Product product;

  @BeforeEach
  void setUp() {
    productService = new ProductService(productRepository, validator);
    product = new Product();
    product.setId("p1");
    product.setName("Rice");
    product.setPrice(new BigDecimal("100.00"));
    product.setQty(10);
    product.setCategory(Product.Category.EDIBLE);
  }

  @Test
  void findById_returnsProduct_whenFound() {
    when(productRepository.findById("p1")).thenReturn(Optional.of(product));

    ResponseEntity<Product> response = productService.findById("p1");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(product);
  }

  @Test
  void findById_throws_whenNotFound() {
    when(productRepository.findById("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> productService.findById("missing"))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void findAllProduct_returnsList_whenNotEmpty() {
    when(productRepository.findAll()).thenReturn(List.of(product));

    ResponseEntity<List<Product>> response = productService.findAllProduct();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsExactly(product);
  }

  @Test
  void findAllProduct_throws_whenEmpty() {
    when(productRepository.findAll()).thenReturn(List.of());

    assertThatThrownBy(() -> productService.findAllProduct())
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void addProduct_savesAndReturnsCreated_whenValid() {
    when(validator.validate(product)).thenReturn(Set.of());

    ResponseEntity<String> response = productService.addProduct(product);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    verify(productRepository).save(product);
  }

  @Test
  @SuppressWarnings("unchecked")
  void addProduct_throws_whenViolationsExist() {
    ConstraintViolation<Product> violation = mock(ConstraintViolation.class);
    when(validator.validate(product)).thenReturn(Set.of(violation));

    assertThatThrownBy(() -> productService.addProduct(product))
        .isInstanceOf(ConstraintViolationException.class);
    verify(productRepository, never()).save(any());
  }

  @Test
  void updateProductById_updatesFields_whenFound() {
    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    Product update = new Product();
    update.setName("Wheat");
    update.setPrice(new BigDecimal("200.00"));

    ResponseEntity<String> response = productService.updateProductById("p1", update);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(product.getName()).isEqualTo("Wheat");
    assertThat(product.getPrice()).isEqualByComparingTo(new BigDecimal("200.00"));
    verify(productRepository).save(product);
  }

  @Test
  void updateProductById_throws_whenNotFound() {
    when(productRepository.findById("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> productService.updateProductById("missing", product))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void deleteProduct_deletes_whenFound() {
    when(productRepository.findById("p1")).thenReturn(Optional.of(product));

    ResponseEntity<String> response = productService.deleteProduct("p1");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(productRepository).deleteById("p1");
  }

  @Test
  void deleteProduct_throws_whenNotFound() {
    when(productRepository.findById("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> productService.deleteProduct("missing"))
        .isInstanceOf(EntityNotFoundException.class);
    verify(productRepository, never()).deleteById(any());
  }
}
