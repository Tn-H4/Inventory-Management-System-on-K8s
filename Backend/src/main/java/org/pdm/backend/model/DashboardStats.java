package org.pdm.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {
    private Long totalActiveProducts;
    private Long lowStockCount;
    private Long outOfStockCount;
    private Double totalStockValue;
    private Double inventoryCost;
    private Double estimatedMargin;
    private List<WarehouseStat> warehouseStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WarehouseStat {
        private Long id;
        private String name;
        private Integer capacity;
        private Long productCount;
        private Long totalQuantity;
    }
}
