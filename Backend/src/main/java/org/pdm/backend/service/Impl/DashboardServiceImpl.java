package org.pdm.backend.service.Impl;

import org.pdm.backend.model.DashboardStats;
import org.pdm.backend.security.DatabaseConfig;
import org.pdm.backend.service.DashboardService;
import org.pdm.backend.wrappers.Response;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class DashboardServiceImpl implements DashboardService {

    private final JdbcTemplate jdbcTemplate = new JdbcTemplate(DatabaseConfig.getDataSource());

    /**
     * Retrieves overall dashboard statistics including product counts, stock values, and warehouse usage.
     *
     * @return A Response containing the aggregated dashboard statistics.
     */
    @Override
    public Response getDashboardStats() {
        DashboardStats stats = jdbcTemplate.queryForObject(
                """
                        SELECT
                          COUNT(CASE WHEN is_deleted = FALSE THEN 1 END) AS total_active,
                          COUNT(CASE WHEN is_deleted = FALSE AND stock_quantity = 0 THEN 1 END) AS out_of_stock,
                          COUNT(CASE WHEN is_deleted = FALSE AND stock_quantity > 0
                                     AND stock_quantity <= reorder_threshold THEN 1 END) AS low_stock,
                          COALESCE(SUM(CASE WHEN is_deleted = FALSE THEN price * stock_quantity ELSE 0 END), 0) AS total_value
                        FROM products
                        """,
                (rs, rowNum) -> DashboardStats.builder()
                        .totalActiveProducts(rs.getLong("total_active"))
                        .lowStockCount(rs.getLong("low_stock"))
                        .outOfStockCount(rs.getLong("out_of_stock"))
                        .totalStockValue(rs.getDouble("total_value"))
                        .build()
        );

        // Calculate inventory cost using preferred supplier's unit_cost
        Double inventoryCost = jdbcTemplate.queryForObject(
                """
                        SELECT COALESCE(SUM(p.stock_quantity * ps.unit_cost), 0)
                        FROM products p
                        JOIN product_suppliers ps ON ps.product_id = p.id
                        WHERE p.is_deleted = FALSE
                          AND p.stock_quantity > 0
                          AND ps.is_preferred = TRUE
                        """,
                Double.class
        );

        // Calculate estimated margin = stock value - inventory cost
        Double estimatedMargin = null;
        if (stats != null && inventoryCost != null) {
            estimatedMargin = stats.getTotalStockValue() - inventoryCost;
        }

        if (stats != null) {
            stats.setInventoryCost(inventoryCost);
            stats.setEstimatedMargin(estimatedMargin);
        }

        List<DashboardStats.WarehouseStat> warehouseStats = jdbcTemplate.query(
                """
                        SELECT w.id, w.name, w.capacity,
                               COUNT(p.id) AS product_count,
                               COALESCE(SUM(p.stock_quantity), 0) AS total_quantity
                        FROM warehouses w
                        LEFT JOIN products p ON p.warehouse_id = w.id AND p.is_deleted = FALSE
                        WHERE w.is_active = TRUE
                        GROUP BY w.id, w.name, w.capacity
                        ORDER BY w.name ASC
                        """,
                (rs, rowNum) -> DashboardStats.WarehouseStat.builder()
                        .id(rs.getLong("id"))
                        .name(rs.getString("name"))
                        .capacity((Integer) rs.getObject("capacity"))
                        .productCount(rs.getLong("product_count"))
                        .totalQuantity(rs.getLong("total_quantity"))
                        .build()
        );

        if (stats == null) {
            stats = DashboardStats.builder()
                    .totalActiveProducts(0L)
                    .lowStockCount(0L)
                    .outOfStockCount(0L)
                    .totalStockValue(0.0)
                    .build();
        }

        stats.setWarehouseStats(warehouseStats);

        return Response.builder()
                .status(200)
                .message("success")
                .dashboardStats(stats)
                .build();
    }

    /**
     * Exports the current dashboard statistics to a CSV format.
     *
     * @return A CSV string containing the key dashboard metrics.
     */
    @Override
    public String exportDashboardCsv() {
        Response response = getDashboardStats();
        DashboardStats stats = response.getDashboardStats();
        StringBuilder csv = new StringBuilder();
        csv.append("Metric,Value\n");
        csv.append("Total Products in Stock,").append(stats.getTotalActiveProducts()).append("\n");
        csv.append("Low Stock Products,").append(stats.getLowStockCount()).append("\n");
        csv.append("Out of Stock Products,").append(stats.getOutOfStockCount()).append("\n");
        csv.append("Stock Value (Retail),").append(stats.getTotalStockValue() != null ? String.format("$%.2f", stats.getTotalStockValue()) : "").append("\n");
        csv.append("Inventory Cost,").append(stats.getInventoryCost() != null ? String.format("$%.2f", stats.getInventoryCost()) : "").append("\n");
        csv.append("Estimated Margin,").append(stats.getEstimatedMargin() != null ? String.format("$%.2f", stats.getEstimatedMargin()) : "").append("\n");
        return csv.toString();
    }
}
