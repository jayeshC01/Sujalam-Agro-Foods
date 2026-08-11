package com.gryffindor.excalibur;

import static org.assertj.core.api.Assertions.assertThat;

import com.gryffindor.excalibur.model.db.Product;
import com.gryffindor.excalibur.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Exercises {@link ProductRepository#decrementStock} against a real MySQL instance with genuinely
 * concurrent transactions (separate threads/connections), to prove the atomic conditional UPDATE
 * cannot oversell stock the way a read-then-write (check-then-act) decrement can.
 */
@Import({TestcontainersConfiguration.class, TestFirebaseConfig.class})
@ActiveProfiles("test")
@SpringBootTest
@Disabled("Requires Docker/Testcontainers")
class ProductStockConcurrencyTest {

  @Autowired private ProductRepository productRepository;

  @Test
  void decrementStock_neverOversells_whenManyCustomersRaceForLimitedStock()
      throws InterruptedException {
    Product product =
        Product.builder()
            .category(Product.Category.EDIBLE)
            .name("Concurrency Test Cardamom " + System.nanoTime())
            .imageUrl("https://example.com/cardamom.png")
            .price(new BigDecimal("100.00"))
            .qty(2)
            .build();
    product = productRepository.saveAndFlush(product);
    String productId = product.getId();

    int customers = 10; // 10 customers race for only 2 units of stock
    ExecutorService pool = Executors.newFixedThreadPool(customers);
    CountDownLatch ready = new CountDownLatch(customers);
    CountDownLatch start = new CountDownLatch(1);

    List<Callable<Integer>> tasks =
        IntStream.range(0, customers)
            .<Callable<Integer>>mapToObj(
                i ->
                    () -> {
                      ready.countDown();
                      start.await();
                      return productRepository.decrementStock(productId, 1);
                    })
            .toList();

    List<Future<Integer>> futures = tasks.stream().map(pool::submit).toList();

    ready.await();
    start.countDown();

    long successfulPurchases =
        futures.stream()
            .map(
                f -> {
                  try {
                    return f.get();
                  } catch (Exception e) {
                    throw new RuntimeException(e);
                  }
                })
            .filter(rowsUpdated -> rowsUpdated == 1)
            .count();
    pool.shutdown();

    // Exactly 2 of the 10 concurrent customers should win the race; the other 8 must be rejected
    // (0 rows updated) rather than all succeeding and driving stock negative.
    assertThat(successfulPurchases).isEqualTo(2);

    Product finalState = productRepository.findById(productId).orElseThrow();
    assertThat(finalState.getQty()).isEqualTo(0);
  }
}
