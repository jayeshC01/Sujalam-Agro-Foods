package com.gryffindor.excalibur.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gryffindor.excalibur.model.db.Product;
import com.gryffindor.excalibur.model.exception.DuplicateProductException;
import com.gryffindor.excalibur.model.request.ProductRequest;
import com.gryffindor.excalibur.model.request.ProductUpdateRequest;
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
import org.springframework.data.domain.Sort;
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
  @DisplayName("findById throws EntityNotFoundException when product is inactive")
  void findById_throws_whenInactive() {
    product.setStatus(Product.Status.INACTIVE);
    when(productRepository.findById("p1")).thenReturn(Optional.of(product));

    assertThatThrownBy(() -> productService.findById("p1"))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("p1");
  }

  @Test
  @DisplayName("findById throws EntityNotFoundException with the requested id when missing")
  void findById_throws_whenNotFound() {
    when(productRepository.findById("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> productService.findById("missing"))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("missing");
  }

  // ---------- findAdminProductById ----------

  @Test
  @DisplayName("findAdminProductById returns product even when it is inactive")
  void findAdminProductById_returnsProduct_whenInactive() {
    product.setStatus(Product.Status.INACTIVE);
    when(productRepository.findById("p1")).thenReturn(Optional.of(product));

    ResponseEntity<ProductResponse> response = productService.findAdminProductById("p1");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getId()).isEqualTo("p1");
    assertThat(response.getBody().getName()).isEqualTo("Rice");
  }

  @Test
  @DisplayName("findAdminProductById throws EntityNotFoundException when missing")
  void findAdminProductById_throws_whenMissing() {
    when(productRepository.findById("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> productService.findAdminProductById("missing"))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("missing");
  }

  // ---------- findAllProduct ----------

  @Test
  @DisplayName("findAllProduct returns a paged response with metadata for active products")
  void findAllProduct_returnsPagedResponse_whenNotEmpty() {
    Product second = new Product();
    second.setId("p2");
    second.setCategory(Product.Category.NOT_EDIBLE);
    second.setName("Soap");
    second.setPrice(new BigDecimal("25.00"));
    second.setQty(50);
    second.setGstRate(new BigDecimal("0.18"));
    second.setStatus(Product.Status.ACTIVE);

    Page<Product> page =
        new PageImpl<>(
            List.of(product, second),
            PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "name")),
            2);
    when(productRepository.searchPublicProducts(
            null, null, PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "name"))))
        .thenReturn(page);

    ResponseEntity<PageResponse<ProductResponse>> response =
        productService.findAllProduct(null, null, 0, 10, "name", "asc");

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
  @DisplayName("findAllProduct filters by category and search query when provided")
  void findAllProduct_filtersByCategoryAndQuery_whenSpecified() {
    Page<Product> page =
        new PageImpl<>(
            List.of(product), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "price")), 1);
    when(productRepository.searchPublicProducts(
            Product.Category.EDIBLE,
            "rice",
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "price"))))
        .thenReturn(page);

    ResponseEntity<PageResponse<ProductResponse>> response =
        productService.findAllProduct(Product.Category.EDIBLE, "rice", 0, 10, "price", "desc");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    PageResponse<ProductResponse> body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getContent()).hasSize(1);
    assertThat(body.getContent().get(0).getCategory()).isEqualTo(Product.Category.EDIBLE);
    assertThat(body.getTotalElements()).isEqualTo(1);
  }

  @Test
  @DisplayName("findAllProduct returns an empty page when the catalog is empty")
  void findAllProduct_returnsEmptyPage_whenEmpty() {
    Page<Product> page =
        new PageImpl<>(List.of(), PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "name")), 0);
    when(productRepository.searchPublicProducts(
            null, null, PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "name"))))
        .thenReturn(page);

    ResponseEntity<PageResponse<ProductResponse>> response =
        productService.findAllProduct(null, null, 0, 10, "name", "asc");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getContent()).isEmpty();
  }

  // ---------- findAdminProducts ----------

  @Test
  @DisplayName("findAdminProducts returns all products when no filters are specified")
  void findAdminProducts_returnsAllProducts_whenNoFiltersSpecified() {
    Product inactiveProduct = new Product();
    inactiveProduct.setId("p2");
    inactiveProduct.setStatus(Product.Status.INACTIVE);

    Page<Product> page =
        new PageImpl<>(
            List.of(product, inactiveProduct),
            PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "name")),
            2);
    when(productRepository.searchAdminProducts(
            null, null, null, PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "name"))))
        .thenReturn(page);

    ResponseEntity<PageResponse<ProductResponse>> response =
        productService.findAdminProducts(null, null, null, 0, 10, "name", "asc");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getContent()).hasSize(2);
  }

  @Test
  @DisplayName("findAdminProducts filters by status, category, and search query")
  void findAdminProducts_filtersByStatusCategoryAndQuery() {
    Page<Product> page =
        new PageImpl<>(
            List.of(product), PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "name")), 1);
    when(productRepository.searchAdminProducts(
            Product.Status.INACTIVE,
            Product.Category.EDIBLE,
            "rice",
            PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "name"))))
        .thenReturn(page);

    ResponseEntity<PageResponse<ProductResponse>> response =
        productService.findAdminProducts(
            Product.Status.INACTIVE, Product.Category.EDIBLE, "rice", 0, 10, "name", "asc");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getContent()).hasSize(1);
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

  private ProductUpdateRequest fullProductUpdateRequest() {
    return ProductUpdateRequest.builder()
        .category(Product.Category.EDIBLE)
        .name("Rice")
        .description("Premium basmati rice")
        .imageUrl("https://example.com/rice.png")
        .healthBenefits("Good source of energy")
        .price(new BigDecimal("100.00"))
        .gstRate(new BigDecimal("0.05"))
        .build();
  }

  @Test
  @DisplayName(
      "addProduct converts every DTO field onto a new entity with trimmed strings, and returns 201")
  void addProduct_savesFullyMappedEntityWithoutId_andReturnsCreated() {
    when(productRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

    ProductRequest request = fullProductRequest();
    request.setName("  Rice  ");
    request.setImageUrl("  https://example.com/rice.png  ");

    ResponseEntity<ProductResponse> response = productService.addProduct(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getName()).isEqualTo("Rice");
    assertThat(response.getBody().getImageUrl()).isEqualTo("https://example.com/rice.png");
    assertThat(response.getBody().getCategory()).isEqualTo(Product.Category.EDIBLE);
    assertThat(response.getBody().getPrice()).isEqualByComparingTo(new BigDecimal("100.00"));
    assertThat(response.getBody().getQty()).isEqualTo(10);
    assertThat(response.getBody().getGstRate()).isEqualByComparingTo(new BigDecimal("0.05"));

    ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
    verify(productRepository).saveAndFlush(captor.capture());
    Product saved = captor.getValue();
    assertThat(saved.getId()).isNull();
    assertThat(saved.getName()).isEqualTo("Rice");
    assertThat(saved.getImageUrl()).isEqualTo("https://example.com/rice.png");
  }

  @Test
  @DisplayName("addProduct throws a conflict when the name is already taken")
  void addProduct_throwsConflict_whenNameAlreadyExists() {
    when(productRepository.saveAndFlush(any()))
        .thenThrow(
            new DataIntegrityViolationException(
                "Duplicate entry 'Rice' for key 'uk_products_name'"));

    assertThatThrownBy(() -> productService.addProduct(fullProductRequest()))
        .isInstanceOf(DuplicateProductException.class)
        .hasMessageContaining("Product with name 'Rice' already exists");
  }

  // ---------- updateProductById ----------

  @Test
  @DisplayName("updateProductById overwrites every field, trims strings, and leaves qty unchanged")
  void updateProductById_updatesAllFields_whenFound() {
    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(productRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

    ProductUpdateRequest update =
        ProductUpdateRequest.builder()
            .category(Product.Category.NOT_EDIBLE)
            .name("  Wheat  ")
            .description(null)
            .imageUrl("  https://example.com/wheat.png  ")
            .healthBenefits(null)
            .price(new BigDecimal("200.00"))
            .gstRate(new BigDecimal("0.12"))
            .build();

    ResponseEntity<ProductResponse> response = productService.updateProductById("p1", update);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCategory()).isEqualTo(Product.Category.NOT_EDIBLE);
    assertThat(response.getBody().getName()).isEqualTo("Wheat");
    assertThat(response.getBody().getDescription()).isNull();
    assertThat(response.getBody().getImageUrl()).isEqualTo("https://example.com/wheat.png");
    assertThat(response.getBody().getHealthBenefits()).isNull();
    assertThat(response.getBody().getPrice()).isEqualByComparingTo(new BigDecimal("200.00"));
    assertThat(response.getBody().getGstRate()).isEqualByComparingTo(new BigDecimal("0.12"));
    assertThat(response.getBody().getQty())
        .isEqualTo(10); // Qty is preserved, not overwritten by catalog updates

    assertThat(product.getCategory()).isEqualTo(Product.Category.NOT_EDIBLE);
    assertThat(product.getName()).isEqualTo("Wheat");
    assertThat(product.getDescription()).isNull();
    assertThat(product.getImageUrl()).isEqualTo("https://example.com/wheat.png");
    assertThat(product.getHealthBenefits()).isNull();
    assertThat(product.getPrice()).isEqualByComparingTo(new BigDecimal("200.00"));
    assertThat(product.getGstRate()).isEqualByComparingTo(new BigDecimal("0.12"));
    assertThat(product.getQty()).isEqualTo(10);
    verify(productRepository).saveAndFlush(product);
  }

  @Test
  @DisplayName("updateProductById allows resubmitting the product's own unchanged name")
  void updateProductById_allowsUnchangedName_whenUpdatingSameProduct() {
    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(productRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

    ProductUpdateRequest update = fullProductUpdateRequest();

    ResponseEntity<ProductResponse> response = productService.updateProductById("p1", update);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getName()).isEqualTo("Rice");
    verify(productRepository).saveAndFlush(product);
  }

  @Test
  @DisplayName("updateProductById throws a conflict when renaming to another product's name")
  void updateProductById_throwsConflict_whenNameBelongsToDifferentProduct() {
    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(productRepository.saveAndFlush(any()))
        .thenThrow(
            new DataIntegrityViolationException(
                "Duplicate entry 'Soap' for key 'uk_products_name'"));

    ProductUpdateRequest update = fullProductUpdateRequest();
    update.setName("Soap");

    assertThatThrownBy(() -> productService.updateProductById("p1", update))
        .isInstanceOf(DuplicateProductException.class)
        .hasMessageContaining("Product with name 'Soap' already exists");
  }

  @Test
  @DisplayName("updateProductById throws and never saves when the product does not exist")
  void updateProductById_throwsAndDoesNotSave_whenNotFound() {
    when(productRepository.findById("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> productService.updateProductById("missing", fullProductUpdateRequest()))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("missing");
    verify(productRepository, never()).saveAndFlush(any());
  }

  // ---------- restockProduct ----------

  @Test
  @DisplayName(
      "restockProduct atomically increments stock and returns 200 with updated product when found")
  void restockProduct_incrementsStock_whenFound() {
    when(productRepository.incrementStock("p1", 15)).thenReturn(1);
    when(productRepository.findById("p1")).thenReturn(Optional.of(product));

    ResponseEntity<ProductResponse> response = productService.restockProduct("p1", 15);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getId()).isEqualTo("p1");
    verify(productRepository).incrementStock("p1", 15);
    verify(productRepository).findById("p1");
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
  @DisplayName("restockProduct throws EntityNotFoundException when product is inactive")
  void restockProduct_throwsNotFound_whenInactive() {
    when(productRepository.incrementStock("p1", 10)).thenReturn(0);

    assertThatThrownBy(() -> productService.restockProduct("p1", 10))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("p1");
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
  @DisplayName(
      "deleteProduct marks the product as inactive and returns 200 with updated product when found")
  void deleteProduct_marksInactive_whenFound() {
    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(productRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    ResponseEntity<ProductResponse> response = productService.deleteProduct("p1");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getId()).isEqualTo("p1");
    assertThat(product.getStatus()).isEqualTo(Product.Status.INACTIVE);
    verify(productRepository).save(product);
    verify(productRepository, never()).deleteById(any());
  }

  @Test
  @DisplayName("deleteProduct throws and never modifies when the product does not exist")
  void deleteProduct_throwsAndDoesNotDelete_whenNotFound() {
    when(productRepository.findById("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> productService.deleteProduct("missing"))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("missing");
    verify(productRepository, never()).save(any());
    verify(productRepository, never()).deleteById(any());
  }

  @Test
  @DisplayName(
      "restoreProduct marks the product as active and returns 200 with updated product when found")
  void restoreProduct_restores_whenFound() {
    product.setStatus(Product.Status.INACTIVE);
    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(productRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    ResponseEntity<ProductResponse> response = productService.restoreProduct("p1");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getId()).isEqualTo("p1");
    assertThat(product.getStatus()).isEqualTo(Product.Status.ACTIVE);
    verify(productRepository).save(product);
  }

  @Test
  @DisplayName("restoreProduct throws and never modifies when the product does not exist")
  void restoreProduct_throwsAndDoesNotRestore_whenNotFound() {
    when(productRepository.findById("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> productService.restoreProduct("missing"))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("missing");
    verify(productRepository, never()).save(any());
  }

  // ---------- writeOffStock ----------

  @Test
  @DisplayName(
      "writeOffStock atomically decrements stock and returns 200 with updated product when found")
  void writeOffStock_decrementsStock_whenFoundAndSufficientStock() {
    when(productRepository.decrementStock("p1", 5)).thenReturn(1);
    when(productRepository.findById("p1")).thenReturn(Optional.of(product));

    ResponseEntity<ProductResponse> response = productService.writeOffStock("p1", 5);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getId()).isEqualTo("p1");
    verify(productRepository).decrementStock("p1", 5);
    verify(productRepository).findById("p1");
  }

  @Test
  @DisplayName("writeOffStock throws EntityNotFoundException when product does not exist")
  void writeOffStock_throwsNotFound_whenMissing() {
    when(productRepository.decrementStock("missing", 5)).thenReturn(0);
    when(productRepository.findById("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> productService.writeOffStock("missing", 5))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("missing");
  }

  @Test
  @DisplayName("writeOffStock throws IllegalArgumentException when current stock is insufficient")
  void writeOffStock_throwsIllegalArgument_whenInsufficientStock() {
    product.setQty(3);
    when(productRepository.decrementStock("p1", 10)).thenReturn(0);
    when(productRepository.findById("p1")).thenReturn(Optional.of(product));

    assertThatThrownBy(() -> productService.writeOffStock("p1", 10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Cannot write-off 10 unit(s)");
  }

  @Test
  @DisplayName("writeOffStock throws IllegalArgumentException when quantity is zero or negative")
  void writeOffStock_throwsIllegalArgument_whenQuantityNonPositive() {
    assertThatThrownBy(() -> productService.writeOffStock("p1", 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Write-off quantity must be greater than 0");

    assertThatThrownBy(() -> productService.writeOffStock("p1", -2))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Write-off quantity must be greater than 0");

    verify(productRepository, never()).decrementStock(any(), any(Integer.class));
  }
}
