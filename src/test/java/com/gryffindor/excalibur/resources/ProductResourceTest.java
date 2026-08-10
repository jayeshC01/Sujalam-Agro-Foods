package com.gryffindor.excalibur.resources;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gryffindor.excalibur.model.db.Product;
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

  private Product validProduct() {
    Product product = new Product();
    product.setName("Rice");
    product.setImageUrl("https://example.com/rice.png");
    product.setPrice(new BigDecimal("100.00"));
    product.setQty(5);
    product.setCategory(Product.Category.EDIBLE);
    return product;
  }

  @Test
  @DisplayName("Get product by id returns ok")
  void getProductById_returnsOk() throws Exception {
    when(productService.findById("p1")).thenReturn(ResponseEntity.ok(new Product()));

    mockMvc.perform(get("/product/{id}", "p1")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("Get all products returns ok")
  void getAllProducts_returnsOk() throws Exception {
    when(productService.findAllProduct()).thenReturn(ResponseEntity.ok(List.of(new Product())));

    mockMvc.perform(get("/products")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("valid product is accepted")
  void addProduct_returnsCreated() throws Exception {
    Product product = validProduct();

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
  @DisplayName("Request with blank name is rejected")
  void blankName_isRejected() throws Exception {
    Product product = validProduct();
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
    Product product = validProduct();
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
    Product product = validProduct();
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
    Product product = validProduct();
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
    Product product = validProduct();
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
  @DisplayName("delete product by id returns ok")
  void deleteProductById_returnsOk() throws Exception {
    when(productService.deleteProduct("p1"))
        .thenReturn(new ResponseEntity<>("Product deleted successfully", HttpStatus.OK));

    mockMvc.perform(delete("/admin/product/{id}", "p1")).andExpect(status().isOk());
  }
}
