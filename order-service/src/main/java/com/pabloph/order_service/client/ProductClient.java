package com.pabloph.order_service.client;

import com.pabloph.order_service.dto.client.ProductResponse;
import com.pabloph.order_service.dto.client.StockAdjustmentRequest;
import com.pabloph.order_service.dto.client.UpdateStockRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "product-service", url = "${product-service.url}")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ProductResponse getProductById(@PathVariable Long id);

    @PatchMapping("/api/products/{id}/stock")
    ProductResponse updateStock(@PathVariable Long id, @RequestBody UpdateStockRequest request);

    @PatchMapping("/api/products/{id}/stock/decrement")
    ProductResponse decrementStock(@PathVariable Long id, @RequestBody StockAdjustmentRequest request);

    @PatchMapping("/api/products/{id}/stock/increment")
    ProductResponse incrementStock(@PathVariable Long id, @RequestBody StockAdjustmentRequest request);
}
