package org.pdm.backend.repository.Impl;

import lombok.RequiredArgsConstructor;
import org.pdm.backend.enums.TransactionStatus;
import org.pdm.backend.enums.TransactionType;
import org.pdm.backend.enums.UserRole;
import org.pdm.backend.model.Product;
import org.pdm.backend.model.Supplier;
import org.pdm.backend.model.Transaction;
import org.pdm.backend.model.User;
import org.pdm.backend.repository.ProductRepository;
import org.pdm.backend.repository.SupplierRepository;
import org.pdm.backend.repository.TransactionRepository;
import org.pdm.backend.repository.UserRepository;
import org.pdm.backend.security.DatabaseConfig;
import org.pdm.backend.util.JdbcUtils;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class TransactionRepositoryImpl implements TransactionRepository {
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    @Override
    /**
     * Persists a new transaction in the database.
     *
     * @param transaction The transaction details to save.
     * @return The saved transaction with generated ID, or null if an error occurred.
     */
    public Transaction save(Transaction transaction) {
        String sql = "INSERT INTO transactions (total_products, total_price, transaction_type, status, description, note, product_id, user_id, supplier_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try(Connection conn= DatabaseConfig.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
            ps.setInt(1, transaction.getTotalProducts());
            ps.setBigDecimal(2, transaction.getTotalPrice());
            ps.setString(3, transaction.getTransactionType().name());
            ps.setString(4, transaction.getStatus().name());
            ps.setString(5, transaction.getDescription());
            ps.setString(6, transaction.getNote());
            JdbcUtils.setLongOrNull(ps, 7, transaction.getProductId());

            JdbcUtils.setLongOrNull(ps, 8, transaction.getUserId());

            JdbcUtils.setLongOrNull(ps, 9, transaction.getSupplierId());
            LocalDateTime now = LocalDateTime.now();
            ps.setTimestamp(10, Timestamp.valueOf(now));
            ps.setTimestamp(11, Timestamp.valueOf(now));
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                ResultSet generatedKeys = ps.getGeneratedKeys();
                if (generatedKeys.next()) {
                    transaction.setId(generatedKeys.getLong(1));
                    transaction.setUpdatedAt(now);
                    return transaction;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    /**
     * Updates an existing transaction's details.
     *
     * @param transaction The transaction entity containing updated values.
     * @return The updated transaction, or null if the update failed.
     */
    public Transaction update(Transaction transaction) {
        String sql = "UPDATE transactions SET total_products = ?, total_price = ?, transaction_type = ?, status = ?, description = ?, note = ?, product_id = ?, user_id = ?, supplier_id = ?, updated_at = ? WHERE id = ?";
        try(Connection conn= DatabaseConfig.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setInt(1, transaction.getTotalProducts());
            ps.setBigDecimal(2, transaction.getTotalPrice());
            ps.setString(3, transaction.getTransactionType().name());
            ps.setString(4, transaction.getStatus().name());
            ps.setString(5, transaction.getDescription());
            ps.setString(6, transaction.getNote());
            JdbcUtils.setLongOrNull(ps, 7, transaction.getProductId());

            JdbcUtils.setLongOrNull(ps, 8, transaction.getUserId());

            JdbcUtils.setLongOrNull(ps, 9, transaction.getSupplierId());
            ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(11, transaction.getId());
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                return transaction;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    /**
     * Finds a transaction by ID and hydrates its associated Product, Supplier, and User entities.
     *
     * @param id The unique identifier of the transaction.
     * @return An Optional containing the hydrated transaction if found.
     */
    public Optional<Transaction> findById(Long id) {

        String sql = "select * from transactions where id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                return Optional.empty();
            }

            // --- Build Transaction ---
            Transaction transaction = mapToTransaction(rs);

            // --- Build Product ---
           transaction.setProduct(productRepository.findById(rs.getLong("product_id")).orElse(null));

            // --- Build Supplier ---
            transaction.setSupplier(supplierRepository.findById(rs.getLong("supplier_id")).orElse(null));


            // --- Build User ---
            transaction.setUser(userRepository.findById(rs.getLong("user_id")).orElse(null));

            return Optional.of(transaction);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }


    @Override
    /**
     * Searches for transactions by description or note with pagination.
     *
     * @param page      The page number (1-indexed).
     * @param size     The number of elements per page.
     * @param searchText The keyword to search for in description or note.
     * @return A list of matching transactions.
     */
    public List<Transaction> searchTransactions( int page, int size, String searchText) {
        String sql = "SELECT * FROM transactions WHERE description LIKE ? OR note LIKE ? LIMIT ? OFFSET ?";
        List<Transaction> transactions = new java.util.ArrayList<>();
        try(Connection conn= DatabaseConfig.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)){
            String likeSearchText = "%" + searchText + "%";
            ps.setString(1, likeSearchText);
            ps.setString(2, likeSearchText);
            ps.setInt(3, size);
            ps.setInt(4, (page - 1) * size);
            ResultSet rs = ps.executeQuery();
            while (rs.next()){
                transactions.add(mapToTransaction(rs));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return transactions;
    }

    @Override
    /**
     * Retrieves all COMPLETED transactions for a given month and year.
     *
     * @param month The month (1-12).
     * @param year  The year (e.g., 2026).
     * @return A list of completed transactions for the specified period.
     */
    public List<Transaction> findAllByMonthAndYear(int month, int year) {
        String sql = "SELECT * FROM transactions WHERE MONTH(created_at) = ? AND YEAR(created_at) = ? AND status = 'COMPLETED'";
        List<Transaction> transactions = new java.util.ArrayList<>();
        try(Connection conn= DatabaseConfig.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setInt(1, month);
            ps.setInt(2, year);
            ResultSet rs = ps.executeQuery();
            while (rs.next()){
                Transaction transaction = mapToTransaction(rs);
                if (transaction.getProductId() != null) {
                    transaction.setProduct(productRepository.findById(transaction.getProductId()).orElse(null));
                }
                transactions.add(transaction);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return transactions;
    }



    private Transaction mapToTransaction(ResultSet rs) throws SQLException {
        Transaction transaction = new Transaction();
        transaction.setId(rs.getLong("id"));
        transaction.setTotalProducts(rs.getInt("total_products"));
        transaction.setTotalPrice(rs.getBigDecimal("total_price"));
        String type = rs.getString("transaction_type");
        if (type != null) {
            try {
                transaction.setTransactionType(TransactionType.valueOf(type));
            } catch (Exception ignore) {}
        }

        String status = rs.getString("status");
        if (status != null) {
            try {
                transaction.setStatus(TransactionStatus.valueOf(status));
            } catch (Exception ignore) {}
        }
        transaction.setDescription(rs.getString("description"));
        transaction.setNote(rs.getString("note"));
        transaction.setProductId(rs.getLong("product_id"));
        transaction.setUserId(rs.getLong("user_id"));
        transaction.setSupplierId(rs.getLong("supplier_id"));
        transaction.setCreatedAt(JdbcUtils.getLocalDateTime(rs, "created_at"));
        transaction.setUpdatedAt(JdbcUtils.getLocalDateTime(rs, "updated_at"));
        return transaction;
    }

    private static final java.util.Set<String> ALLOWED_SORT_COLUMNS = java.util.Set.of(
            "created_at", "total_price", "total_products", "status", "transaction_type"
    );

    @Override
    /**
     * Retrieves a paginated list of transactions with optional filters for search text and status.
     * Joins with the products table to allow searching by product name.
     *
     * @param filter   Keyword to search in description, note, type, status, or product name.
     * @param status   Filter by transaction status.
     * @param page     Page number (1-indexed).
     * @param size     Page size.
     * @param sortBy   Field to sort by.
     * @param sortDir  Sorting direction (ASC/DESC).
     * @return A list of matching transactions with hydrated products.
     */
    public List<Transaction> findAllFilteredPaged(String filter, String status, int page, int size, String sortBy, String sortDir) {

        boolean hasFilter = filter != null && !filter.isBlank();
        boolean hasStatus = status != null && !status.isBlank();
        List<Transaction> list = new ArrayList<>();

        String orderColumn = ALLOWED_SORT_COLUMNS.contains(sortBy) ? sortBy : "created_at";
        String orderDir = "ASC".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";
        String orderClause = " ORDER BY t." + orderColumn + " " + orderDir;

        StringBuilder sql = new StringBuilder("SELECT t.* FROM transactions t LEFT JOIN products ON t.product_id = products.id");
        List<Object> params = new ArrayList<>();
        if (hasFilter || hasStatus) {
            sql.append(" WHERE");
            if (hasFilter) {
                sql.append(" (t.description LIKE ? OR t.note LIKE ? OR t.transaction_type LIKE ? OR t.status LIKE ? OR products.name LIKE ?)");
                String pattern = "%" + filter + "%";
                params.add(pattern);
                params.add(pattern);
                params.add(pattern);
                params.add(pattern);
                params.add(pattern);
            }
            if (hasStatus) {
                if (hasFilter) {
                    sql.append(" AND");
                }
                sql.append(" t.status = ?");
                params.add(status);
            }
        }
        sql.append(orderClause).append(" LIMIT ? OFFSET ?");

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i).toString());
            }
            ps.setInt(params.size() + 1, size);
            ps.setInt(params.size() + 2, (page - 1) * size);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Transaction transaction = mapToTransaction(rs);
                if (transaction.getProductId() != null) {
                    transaction.setProduct(productRepository.findById(transaction.getProductId()).orElse(null));
                }
                list.add(transaction);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }


    @Override
    /**
     * Counts the total number of transactions matching the filters.
     *
     * @param filter Keyword search.
     * @param status Filter by transaction status.
     * @return The total count of matching transactions.
     */
    public long countFiltered(String filter, String status) {

        boolean hasFilter = filter != null && !filter.isBlank();
        boolean hasStatus = status != null && !status.isBlank();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM transactions t LEFT JOIN products ON t.product_id = products.id");
        List<Object> params = new ArrayList<>();

        if (hasFilter || hasStatus) {
            sql.append(" WHERE");
            if (hasFilter) {
                sql.append(" (t.description LIKE ? OR t.note LIKE ? OR t.transaction_type LIKE ? OR t.status LIKE ? OR products.name LIKE ?)");
                String pattern = "%" + filter + "%";
                params.add(pattern);
                params.add(pattern);
                params.add(pattern);
                params.add(pattern);
                params.add(pattern);
            }
            if (hasStatus) {
                if (hasFilter) {
                    sql.append(" AND");
                }
                sql.append(" t.status = ?");
                params.add(status);
            }
        }

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i).toString());
            }

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    /**
     * Updates the status of a transaction.
     *
     * @param id     The ID of the transaction.
     * @param status The new status to set.
     * @return The updated transaction entity, or null if not found.
     */
    public Transaction updateStatus(Long id, TransactionStatus status) {
        String sql = "UPDATE transactions SET status = ?, updated_at = ? WHERE id = ?";
        try(Connection conn= DatabaseConfig.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1, status.name());
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(3, id);
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                return findById(id).orElse(null);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    /**
     * Sets the product_id to NULL for all transactions associated with a specific product.
     * Used during product deletion to maintain data integrity.
     *
     * @param productId The ID of the product.
     */
    public void nullifyProductId(Long productId) {
        String sql = "UPDATE transactions SET product_id = NULL WHERE product_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productId);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
