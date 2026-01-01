package com.example.flashsale;

import com.example.flashsale.entity.Product;
import com.example.flashsale.repo.ProductRepository;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;

@SpringBootApplication
public class FlashsaleApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlashsaleApplication.class, args);
    }

    @Bean
    CommandLineRunner seed(ProductRepository repo) {
        return args -> {
            if (repo.count() == 0) {
                repo.save(Product.builder().name("iPhone Case").price(new BigDecimal("199.00")).stock(5).build());
                repo.save(Product.builder().name("USB Cable").price(new BigDecimal("99.00")).stock(10).build());
                repo.save(Product.builder().name("Power Bank").price(new BigDecimal("999.00")).stock(2).build());
            }
        };
    }
}
