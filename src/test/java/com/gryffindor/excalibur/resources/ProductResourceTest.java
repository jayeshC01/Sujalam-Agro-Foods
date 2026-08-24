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
    mockMvc =
        MockMvcBuilders.standaloneSetup(new ProductResource(productService))
            .setControllerAdvice(new ErrorHandler())
            .build();
  }

  private ProductRequest validProduct() {
    ProductRequest product = new ProductRequest();
    product.setName("Rice");
    product.setImageUrl("https://example.com/rice.png");
    product.setPrice(new BigDecimal("100.00"));
    product.setQty(5);
    product.setCategory(Product.Category.EDIBLE);
    product.setGstRate(new BigDecimal("0.05"));
    return product;
  }

  @Test
  @DisplayName("Get product by id returns ok")
  void getProductById_returnsOk() throws Exception {
    when(productService.findById("p1")).thenReturn(ResponseEntity.ok(new ProductResponse()));

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

    when(productService.findAllProduct(0, 10)).thenReturn(ResponseEntity.ok(pageResponse));

    mockMvc
        .perform(get("/products").param("page", "0").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0]").exists())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(10));
  }

  @Test
  @DisplayName("valid product is accepted")
  void addProduct_returnsCreated() throws Exception {
    ProductRequest product = validProduct();

    when(productService.addProduct(any()))
        .thenReturn(new ResponseEntity<>("Product Added successfully", HttpStatus.CREATED));

    mockMvc
        .perform(
            post("/admin/product")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
        .andExpect(status().isCreated());

    verify(productService).addProduct(any());
  }

  @Test
  @DisplayName("valid update is accepted")
  void updateProductById_returnsOk() throws Exception {
    ProductRequest product = validProduct();

    when(productService.updateProductById(any(), any()))
        .thenReturn(new ResponseEntity<>("Product updated successfully", HttpStatus.OK));

    mockMvc
        .perform(
            put("/admin/product/{id}", "p1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
        .andExpect(status().isOk());

    verify(productService).updateProductById("p1", product);
  }

  @Test
  @DisplayName("Update request with blank name is rejected")
  void updateProductById_blankName_isRejected() throws Exception {
    ProductRequest product = validProduct();
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
  @DisplayName("Restock request with valid quantity returns ok")
  void restockProduct_returnsOk() throws Exception {
    when(productService.restockProduct("p1", 10))
        .thenReturn(new ResponseEntity<>("Product restocked successfully", HttpStatus.OK));

    mockMvc
        .perform(post("/admin/product/{id}/restock", "p1").param("quantity", "10"))
        .andExpect(status().isOk());

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
  @DisplayName("delete product by id returns ok")
  void deleteProductById_returnsOk() throws Exception {
    when(productService.deleteProduct("p1"))
        .thenReturn(new ResponseEntity<>("Product deleted successfully", HttpStatus.OK));

    mockMvc.perform(delete("/admin/product/{id}", "p1")).andExpect(status().isOk());
  }
}
