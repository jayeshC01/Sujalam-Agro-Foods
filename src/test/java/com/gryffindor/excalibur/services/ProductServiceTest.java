package com.gryffindor.excalibur.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gryffindor.excalibur.model.db.Product;
import com.gryffindor.excalibur.model.request.ProductRequest;
import com.gryffindor.excalibur.model.response.PageResponse;
import com.gryffindor.excalibur.model.response.ProductResponse;
import com.gryffindor.excalibur.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

  @Mock private ProductRepository productRepository;

  private ProductService productService;

  private Product product;

  @BeforeEach
  void setUp() {
    productService = new ProductService(productRepository);
    product = new Product();
    product.setId("p1");
    product.setCategory(Product.Category.EDIBLE);
    product.setName("Rice");
    product.setDescription("Premium basmati rice");
    product.setImageUrl("https://example.com/rice.png");
    product.setHealthBenefits("Good source of energy");
    product.setPrice(new BigDecimal("100.00"));
    product.setQty(10);
    product.setGstRate(new BigDecimal("0.05"));
  }

  // ---------- findById ----------

  @Test
  @DisplayName("findById maps every field onto the response when the product exists")
  void findById_returnsFullyMappedResponse_whenFound() {
    when(productRepository.findById("p1")).thenReturn(Optional.of(product));

    ResponseEntity<ProductResponse> response = productService.findById("p1");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    ProductResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getId()).isEqualTo("p1");
    assertThat(body.getCategory()).isEqualTo(Product.Category.EDIBLE);
    assertThat(body.getName()).isEqualTo("Rice");
    assertThat(body.getDescription()).isEqualTo("Premium basmati rice");
    assertThat(body.getImageUrl()).isEqualTo("https://example.com/rice.png");
    assertThat(body.getHealthBenefits()).isEqualTo("Good source of energy");
    assertThat(body.getPrice()).isEqualByComparingTo(new BigDecimal("100.00"));
    assertThat(body.getQty()).isEqualTo(10);
    assertThat(body.getGstRate()).isEqualByComparingTo(new BigDecimal("0.05"));
  }

  @Test
  @DisplayName("findById throws EntityNotFoundException with the requested id when missing")
  void findById_throws_whenNotFound() {
    when(productRepository.findById("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> productService.findById("missing"))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("missing");
  }

  // ---------- findAllProduct ----------

  @Test
  @DisplayName("findAllProduct returns a paged response with metadata")
  void findAllProduct_returnsPagedResponse_whenNotEmpty() {
    Product second = new Product();
    second.setId("p2");
    second.setCategory(Product.Category.NOT_EDIBLE);
    second.setName("Soap");
    second.setPrice(new BigDecimal("25.00"));
    second.setQty(50);
    second.setGstRate(new BigDecimal("0.18"));

    Page<Product> page = new PageImpl<>(List.of(product, second), PageRequest.of(0, 10), 2);
    when(productRepository.findAll(PageRequest.of(0, 10))).thenReturn(page);

    ResponseEntity<PageResponse<ProductResponse>> response = productService.findAllProduct(0, 10);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    PageResponse<ProductResponse> body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getContent()).hasSize(2);
    assertThat(body.getPage()).isEqualTo(0);
    assertThat(body.getSize()).isEqualTo(10);
    assertThat(body.getTotalElements()).isEqualTo(2);
    assertThat(body.getTotalPages()).isEqualTo(1);
    assertThat(body.getFirst()).isTrue();
    assertThat(body.getLast()).isTrue();
  }

  @Test
  @DisplayName("findAllProduct returns an empty page when the catalog is empty")
  void findAllProduct_returnsEmptyPage_whenEmpty() {
    Page<Product> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
    when(productRepository.findAll(PageRequest.of(0, 10))).thenReturn(page);

    ResponseEntity<PageResponse<ProductResponse>> response = productService.findAllProduct(0, 10);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getContent()).isEmpty();
  }

  // ---------- addProduct ----------

  private ProductRequest fullProductRequest() {
    ProductRequest request = new ProductRequest();
    request.setCategory(Product.Category.EDIBLE);
    request.setName("Rice");
    request.setDescription("Premium basmati rice");
    request.setImageUrl("https://example.com/rice.png");
    request.setHealthBenefits("Good source of energy");
    request.setPrice(new BigDecimal("100.00"));
    request.setQty(10);
    request.setGstRate(new BigDecimal("0.05"));
    return request;
  }

  @Test
  @DisplayName("addProduct converts every DTO field onto a new entity with no id, and returns 201")
  void addProduct_savesFullyMappedEntityWithoutId_andReturnsCreated() {
    ResponseEntity<String> response = productService.addProduct(fullProductRequest());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isEqualTo("Product Added successfully");

    ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
    verify(productRepository).save(captor.capture());
    Product saved = captor.getValue();
    assertThat(saved.getId()).isNull();
    assertThat(saved.getCategory()).isEqualTo(Product.Category.EDIBLE);
    assertThat(saved.getName()).isEqualTo("Rice");
    assertThat(saved.getDescription()).isEqualTo("Premium basmati rice");
    assertThat(saved.getImageUrl()).isEqualTo("https://example.com/rice.png");
    assertThat(saved.getHealthBenefits()).isEqualTo("Good source of energy");
    assertThat(saved.getPrice()).isEqualByComparingTo(new BigDecimal("100.00"));
    assertThat(saved.getQty()).isEqualTo(10);
    assertThat(saved.getGstRate()).isEqualByComparingTo(new BigDecimal("0.05"));
  }

  @Test
  @DisplayName("addProduct throws a conflict and never saves when the name is already taken")
  void addProduct_throwsConflict_whenNameAlreadyExists() {
    when(productRepository.findByName("Rice")).thenReturn(Optional.of(product));

    assertThatThrownBy(() -> productService.addProduct(fullProductRequest()))
        .isInstanceOf(DataIntegrityViolationException.class);
    verify(productRepository, never()).save(any());
  }

  // ---------- updateProductById ----------

  @Test
  @DisplayName("updateProductById overwrites every field, including clearing optional ones")
  void updateProductById_updatesAllFields_whenFound() {
    when(productRepository.findById("p1")).thenReturn(Optional.of(product));

    ProductRequest update = new ProductRequest();
    update.setCategory(Product.Category.NOT_EDIBLE);
    update.setName("Wheat");
    update.setDescription(null);
    update.setImageUrl("https://example.com/wheat.png");
    update.setHealthBenefits(null);
    update.setPrice(new BigDecimal("200.00"));
    update.setQty(20);
    update.setGstRate(new BigDecimal("0.12"));

    ResponseEntity<String> response = productService.updateProductById("p1", update);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo("Product updated successfully");

    assertThat(product.getCategory()).isEqualTo(Product.Category.NOT_EDIBLE);
    assertThat(product.getName()).isEqualTo("Wheat");
    assertThat(product.getDescription()).isNull();
    assertThat(product.getImageUrl()).isEqualTo("https://example.com/wheat.png");
    assertThat(product.getHealthBenefits()).isNull();
    assertThat(product.getPrice()).isEqualByComparingTo(new BigDecimal("200.00"));
    assertThat(product.getGstRate()).isEqualByComparingTo(new BigDecimal("0.12"));
    assertThat(product.getQty())
        .isEqualTo(10); // Qty is preserved, not overwritten by catalog updates
    verify(productRepository).save(product);
  }

  @Test
  @DisplayName("updateProductById allows resubmitting the product's own unchanged name")
  void updateProductById_allowsUnchangedName_whenUpdatingSameProduct() {
    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(productRepository.findByName("Rice")).thenReturn(Optional.of(product));

    ProductRequest update = fullProductRequest();

    ResponseEntity<String> response = productService.updateProductById("p1", update);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(productRepository).save(product);
  }

  @Test
  @DisplayName("updateProductById throws a conflict when renaming to another product's name")
  void updateProductById_throwsConflict_whenNameBelongsToDifferentProduct() {
    Product other = new Product();
    other.setId("p2");
    other.setName("Soap");

    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(productRepository.findByName("Soap")).thenReturn(Optional.of(other));

    ProductRequest update = fullProductRequest();
    update.setName("Soap");

    assertThatThrownBy(() -> productService.updateProductById("p1", update))
        .isInstanceOf(DataIntegrityViolationException.class);
    verify(productRepository, never()).save(any());
  }

  @Test
  @DisplayName("updateProductById throws and never saves when the product does not exist")
  void updateProductById_throwsAndDoesNotSave_whenNotFound() {
    when(productRepository.findById("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> productService.updateProductById("missing", new ProductRequest()))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("missing");
    verify(productRepository, never()).save(any());
  }

  // ---------- restockProduct ----------

  @Test
  @DisplayName("restockProduct atomically increments stock and returns 200 when found")
  void restockProduct_incrementsStock_whenFound() {
    when(productRepository.incrementStock("p1", 15)).thenReturn(1);

    ResponseEntity<String> response = productService.restockProduct("p1", 15);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo("Product restocked successfully");
    verify(productRepository).incrementStock("p1", 15);
  }

  @Test
  @DisplayName("restockProduct throws EntityNotFoundException when product does not exist")
  void restockProduct_throwsNotFound_whenMissing() {
    when(productRepository.incrementStock("missing", 10)).thenReturn(0);

    assertThatThrownBy(() -> productService.restockProduct("missing", 10))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("missing");
  }

  @Test
  @DisplayName("restockProduct throws IllegalArgumentException when quantity is zero or negative")
  void restockProduct_throwsIllegalArgument_whenQuantityNonPositive() {
    assertThatThrownBy(() -> productService.restockProduct("p1", 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Restock quantity must be greater than 0");

    assertThatThrownBy(() -> productService.restockProduct("p1", -5))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Restock quantity must be greater than 0");

    verify(productRepository, never()).incrementStock(any(), any(Integer.class));
  }

  // ---------- deleteProduct ----------

  @Test
  @DisplayName("deleteProduct removes the product and returns 200 when found")
  void deleteProduct_deletes_whenFound() {
    when(productRepository.findById("p1")).thenReturn(Optional.of(product));

    ResponseEntity<String> response = productService.deleteProduct("p1");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo("Product deleted successfully");
    verify(productRepository).deleteById("p1");
  }

  @Test
  @DisplayName("deleteProduct throws and never deletes when the product does not exist")
  void deleteProduct_throwsAndDoesNotDelete_whenNotFound() {
    when(productRepository.findById("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> productService.deleteProduct("missing"))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("missing");
    verify(productRepository, never()).deleteById(any());
  }
}
