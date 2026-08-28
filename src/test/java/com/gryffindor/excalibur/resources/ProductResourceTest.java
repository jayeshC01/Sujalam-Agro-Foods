package com.gryffindor.excalibur.resources;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gryffindor.excalibur.model.db.Product;
import com.gryffindor.excalibur.model.request.ProductRequest;
import com.gryffindor.excalibur.model.request.ProductUpdateRequest;
import com.gryffindor.excalibur.model.response.PageResponse;
import com.gryffindor.excalibur.model.response.ProductResponse;
import com.gryffindor.excalibur.services.ProductService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ProductResourceTest {

  @Mock private ProductService productService;
  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    ProductResource productResource = new ProductResource(productService);
    this.mockMvc =
        MockMvcBuilders.standaloneSetup(productResource)
            .setControllerAdvice(new ErrorHandler())
            .build();
  }

  private ProductRequest validProduct() {
    ProductRequest product = new ProductRequest();
    product.setCategory(Product.Category.EDIBLE);
    product.setName("Rice");
    product.setDescription("Premium basmati rice");
    product.setImageUrl("https://example.com/rice.png");
    product.setHealthBenefits("Good source of energy");
    product.setPrice(new BigDecimal("100.00"));
    product.setQty(10);
    product.setGstRate(new BigDecimal("0.05"));
    return product;
  }

  private ProductUpdateRequest validProductUpdateRequest() {
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
  @DisplayName("Get product by id returns ok when found")
  void getProductById_returnsOk() throws Exception {
    Product product = validProduct().toProduct();
    product.setId("p1");

    when(productService.findById("p1"))
        .thenReturn(ResponseEntity.ok(ProductResponse.from(product)));

    mockMvc.perform(get("/product/{id}", "p1")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("Get all products returns ok with pagination metadata")
  void getAllProducts_returnsOkWithPaginationMetadata() throws Exception {
    PageResponse<ProductResponse> pageResponse =
        PageResponse.<ProductResponse>builder()
            .content(List.of(new ProductResponse()))
            .page(0)
            .size(10)
            .totalElements(1)
            .totalPages(1)
            .first(true)
            .last(true)
            .build();

    when(productService.findAllProduct(null, null, 0, 10, "name", "asc"))
        .thenReturn(ResponseEntity.ok(pageResponse));

    mockMvc
        .perform(get("/products").param("page", "0").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0]").exists())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(10));
  }

  @Test
  @DisplayName("Get all products filters by category and search query with sorting")
  void getAllProducts_filtersByCategoryAndSearch_whenParamsProvided() throws Exception {
    PageResponse<ProductResponse> pageResponse =
        PageResponse.<ProductResponse>builder()
            .content(List.of(new ProductResponse()))
            .page(0)
            .size(10)
            .totalElements(1)
            .totalPages(1)
            .first(true)
            .last(true)
            .build();

    when(productService.findAllProduct(Product.Category.EDIBLE, "basmati", 0, 10, "price", "desc"))
        .thenReturn(ResponseEntity.ok(pageResponse));

    mockMvc
        .perform(
            get("/products")
                .param("category", "EDIBLE")
                .param("q", "basmati")
                .param("sortBy", "price")
                .param("sortDirection", "desc")
                .param("page", "0")
                .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0]").exists());

    verify(productService)
        .findAllProduct(Product.Category.EDIBLE, "basmati", 0, 10, "price", "desc");
  }

  @Test
  @DisplayName("Get admin products returns ok with pagination metadata")
  void getAdminProducts_returnsOkWithPaginationMetadata() throws Exception {
    PageResponse<ProductResponse> pageResponse =
        PageResponse.<ProductResponse>builder()
            .content(List.of(new ProductResponse()))
            .page(0)
            .size(10)
            .totalElements(1)
            .totalPages(1)
            .first(true)
            .last(true)
            .build();

    when(productService.findAdminProducts(null, null, null, 0, 10, "name", "asc"))
        .thenReturn(ResponseEntity.ok(pageResponse));

    mockMvc
        .perform(get("/admin/products").param("page", "0").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0]").exists())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(10));
  }

  @Test
  @DisplayName("Get admin products filters by status, category, and search query")
  void getAdminProducts_filtersByStatusCategoryAndSearch() throws Exception {
    PageResponse<ProductResponse> pageResponse =
        PageResponse.<ProductResponse>builder()
            .content(List.of(new ProductResponse()))
            .page(0)
            .size(10)
            .totalElements(1)
            .totalPages(1)
            .first(true)
            .last(true)
            .build();

    when(productService.findAdminProducts(
            Product.Status.INACTIVE, Product.Category.EDIBLE, "rice", 0, 10, "price", "asc"))
        .thenReturn(ResponseEntity.ok(pageResponse));

    mockMvc
        .perform(
            get("/admin/products")
                .param("status", "INACTIVE")
                .param("category", "EDIBLE")
                .param("q", "rice")
                .param("sortBy", "price")
                .param("sortDirection", "asc")
                .param("page", "0")
                .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0]").exists());

    verify(productService)
        .findAdminProducts(
            Product.Status.INACTIVE, Product.Category.EDIBLE, "rice", 0, 10, "price", "asc");
  }

  @Test
  @DisplayName("Get admin product by id returns ok for active or inactive product")
  void getAdminProductById_returnsOk() throws Exception {
    ProductResponse response = validProductResponse();
    when(productService.findAdminProductById("p1")).thenReturn(ResponseEntity.ok(response));

    mockMvc
        .perform(get("/admin/product/{id}", "p1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("p1"))
        .andExpect(jsonPath("$.name").value("Rice"));

    verify(productService).findAdminProductById("p1");
  }

  private ProductResponse validProductResponse() {
    Product p = validProduct().toProduct();
    p.setId("p1");
    return ProductResponse.from(p);
  }

  @Test
  @DisplayName("Add product request with valid body returns created")
  void addProduct_returnsCreated() throws Exception {
    ProductRequest product = validProduct();

    when(productService.addProduct(any()))
        .thenReturn(new ResponseEntity<>(validProductResponse(), HttpStatus.CREATED));

    mockMvc
        .perform(
            post("/admin/product")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value("p1"))
        .andExpect(jsonPath("$.name").value("Rice"));

    verify(productService).addProduct(product);
  }

  @Test
  @DisplayName("Add product request with duplicate name returns conflict")
  void addProduct_duplicateName_returnsConflict() throws Exception {
    ProductRequest product = validProduct();

    when(productService.addProduct(any()))
        .thenThrow(
            new com.gryffindor.excalibur.model.exception.DuplicateProductException(
                "Product with name 'Rice' already exists"));

    mockMvc
        .perform(
            post("/admin/product")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Product with name 'Rice' already exists"));

    verify(productService).addProduct(product);
  }

  @Test
  @DisplayName("valid update is accepted")
  void updateProductById_returnsOk() throws Exception {
    ProductUpdateRequest product = validProductUpdateRequest();

    when(productService.updateProductById(any(), any()))
        .thenReturn(ResponseEntity.ok(validProductResponse()));

    mockMvc
        .perform(
            put("/admin/product/{id}", "p1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("p1"));

    verify(productService).updateProductById("p1", product);
  }

  @Test
  @DisplayName("Update request with blank name is rejected")
  void updateProductById_blankName_isRejected() throws Exception {
    ProductUpdateRequest product = validProductUpdateRequest();
    product.setName(" ");

    mockMvc
        .perform(
            put("/admin/product/{id}", "p1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details").value(containsString("name")));

    verify(productService, never()).updateProductById(any(), any());
  }

  @Test
  @DisplayName("Request with blank name is rejected")
  void blankName_isRejected() throws Exception {
    ProductRequest product = validProduct();
    product.setName(" ");

    mockMvc
        .perform(
            post("/admin/product")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details").value(containsString("name")));

    verify(productService, never()).addProduct(any());
  }

  @Test
  @DisplayName("Request with blank image is rejected")
  void blankImageUrl_isRejected() throws Exception {
    ProductRequest product = validProduct();
    product.setImageUrl(" ");

    mockMvc
        .perform(
            post("/admin/product")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details").value(containsString("imageUrl")));

    verify(productService, never()).addProduct(any());
  }

  @Test
  @DisplayName("Request with invalid image URL format is rejected")
  void invalidImageUrl_isRejected() throws Exception {
    ProductRequest product = validProduct();
    product.setImageUrl("not-a-valid-url");

    mockMvc
        .perform(
            post("/admin/product")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details").value(containsString("imageUrl")));

    verify(productService, never()).addProduct(any());
  }

  @Test
  @DisplayName("Request with more than 2 decimal places in price is rejected")
  void excessivePriceDecimalPlaces_isRejected() throws Exception {
    ProductRequest product = validProduct();
    product.setPrice(new BigDecimal("100.999"));

    mockMvc
        .perform(
            post("/admin/product")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details").value(containsString("price")));

    verify(productService, never()).addProduct(any());
  }

  @Test
  @DisplayName("Request with missing price is rejected")
  void missingPrice_isRejected() throws Exception {
    ProductRequest product = validProduct();
    product.setPrice(null);

    mockMvc
        .perform(
            post("/admin/product")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details").value(containsString("price")));

    verify(productService, never()).addProduct(any());
  }

  @Test
  @DisplayName("Request with negative price is rejected")
  void negativePrice_isRejected() throws Exception {
    ProductRequest product = validProduct();
    product.setPrice(new BigDecimal("-1.00"));

    mockMvc
        .perform(
            post("/admin/product")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details").value(containsString("price")));

    verify(productService, never()).addProduct(any());
  }

  @Test
  @DisplayName("Request with negative quantity is rejected")
  void negativeQuantity_isRejected() throws Exception {
    ProductRequest product = validProduct();
    product.setQty(-1);

    mockMvc
        .perform(
            post("/admin/product")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details").value(containsString("qty")));

    verify(productService, never()).addProduct(any());
  }

  @Test
  @DisplayName("Request with missing GST rate is rejected")
  void missingGstRate_isRejected() throws Exception {
    ProductRequest product = validProduct();
    product.setGstRate(null);

    mockMvc
        .perform(
            post("/admin/product")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details").value(containsString("gstRate")));

    verify(productService, never()).addProduct(any());
  }

  @Test
  @DisplayName("Request with negative GST rate is rejected")
  void negativeGstRate_isRejected() throws Exception {
    ProductRequest product = validProduct();
    product.setGstRate(new BigDecimal("-0.05"));

    mockMvc
        .perform(
            post("/admin/product")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details").value(containsString("gstRate")));

    verify(productService, never()).addProduct(any());
  }

  @Test
  @DisplayName("Request with GST rate exceeding 1.0 (100%) is rejected")
  void excessiveGstRate_isRejected() throws Exception {
    ProductRequest product = validProduct();
    product.setGstRate(new BigDecimal("1.05"));

    mockMvc
        .perform(
            post("/admin/product")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details").value(containsString("gstRate")));

    verify(productService, never()).addProduct(any());
  }

  @Test
  @DisplayName("Restock request with valid quantity returns ok")
  void restockProduct_returnsOk() throws Exception {
    when(productService.restockProduct("p1", 10))
        .thenReturn(ResponseEntity.ok(validProductResponse()));

    mockMvc
        .perform(post("/admin/product/{id}/restock", "p1").param("quantity", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("p1"));

    verify(productService).restockProduct("p1", 10);
  }

  @Test
  @DisplayName("Restock request with missing quantity is rejected")
  void restockProduct_missingQuantity_isRejected() throws Exception {
    mockMvc.perform(post("/admin/product/{id}/restock", "p1")).andExpect(status().isBadRequest());

    verify(productService, never()).restockProduct(any(), any(Integer.class));
  }

  @Test
  @DisplayName("Restock request with zero or negative quantity is rejected")
  void restockProduct_nonPositiveQuantity_isRejected() throws Exception {
    when(productService.restockProduct("p1", 0))
        .thenThrow(new IllegalArgumentException("Restock quantity must be greater than 0"));

    mockMvc
        .perform(post("/admin/product/{id}/restock", "p1").param("quantity", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message").value(containsString("Restock quantity must be greater than 0")));

    verify(productService).restockProduct("p1", 0);
  }

  @Test
  @DisplayName("Write-off request with valid quantity returns ok")
  void writeOffStock_returnsOk() throws Exception {
    when(productService.writeOffStock("p1", 5))
        .thenReturn(ResponseEntity.ok(validProductResponse()));

    mockMvc
        .perform(post("/admin/product/{id}/write-off", "p1").param("quantity", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("p1"));

    verify(productService).writeOffStock("p1", 5);
  }

  @Test
  @DisplayName("Write-off request with missing quantity is rejected")
  void writeOffStock_missingQuantity_isRejected() throws Exception {
    mockMvc.perform(post("/admin/product/{id}/write-off", "p1")).andExpect(status().isBadRequest());

    verify(productService, never()).writeOffStock(any(), any(Integer.class));
  }

  @Test
  @DisplayName("Write-off request with zero or negative quantity is rejected")
  void writeOffStock_nonPositiveQuantity_isRejected() throws Exception {
    when(productService.writeOffStock("p1", 0))
        .thenThrow(new IllegalArgumentException("Write-off quantity must be greater than 0"));

    mockMvc
        .perform(post("/admin/product/{id}/write-off", "p1").param("quantity", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value(containsString("Write-off quantity must be greater than 0")));

    verify(productService).writeOffStock("p1", 0);
  }

  @Test
  @DisplayName("delete product by id returns ok")
  void deleteProductById_returnsOk() throws Exception {
    when(productService.deleteProduct("p1")).thenReturn(ResponseEntity.ok(validProductResponse()));

    mockMvc
        .perform(delete("/admin/product/{id}", "p1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("p1"));
  }

  @Test
  @DisplayName("restore product by id returns ok")
  void restoreProductById_returnsOk() throws Exception {
    when(productService.restoreProduct("p1")).thenReturn(ResponseEntity.ok(validProductResponse()));

    mockMvc
        .perform(post("/admin/product/{id}/restore", "p1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("p1"));

    verify(productService).restoreProduct("p1");
  }
}
