package com.retailpulse;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class BusinessEntityMicroserviceTest {

    @Test
    void mainDelegatesToSpringApplicationRun() {
        String[] args = {"--spring.profiles.active=test"};

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            BusinessEntityMicroservice.main(args);

            springApplication.verify(() -> SpringApplication.run(BusinessEntityMicroservice.class, args));
        }
    }
}
