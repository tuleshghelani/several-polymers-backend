package com.inventory.config;

import com.inventory.entity.Product;
import com.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Order(1)
@RequiredArgsConstructor
public class ProductStartupInitializer implements ApplicationRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(ApplicationArguments args) {
        List<String> requiredCodes = Arrays.asList("RESIGN", "CPW");

        for (String code : requiredCodes) {
            boolean exists = productRepository.existsByProductCode(code);
            if (!exists) {
                Product product = new Product();
                product.setName(code);
                product.setStatus("A");
                product.setProductCode(code);
                productRepository.save(product);
            }
        }
    }
}


