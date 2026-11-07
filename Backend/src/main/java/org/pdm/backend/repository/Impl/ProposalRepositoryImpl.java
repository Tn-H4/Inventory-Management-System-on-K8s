package org.pdm.backend.repository.Impl;

import lombok.RequiredArgsConstructor;
import org.pdm.backend.model.Proposal;
import org.pdm.backend.model.User;
import org.pdm.backend.repository.ProductRepository;
import org.pdm.backend.repository.ProposalRepository;
import org.pdm.backend.repository.SupplierRepository;
import org.pdm.backend.repository.UserRepository;
import org.pdm.backend.security.DatabaseConfig;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class ProposalRepositoryImpl implements ProposalRepository {

    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;

    private final JdbcTemplate jdbcTemplate = new JdbcTemplate(DatabaseConfig.getDataSource());

    private final RowMapper<Proposal> mapRowToProposal = (rs, rowNum) -> {
        Proposal proposal = new Proposal();
        proposal.setId(rs.getLong("id"));
        proposal.setTransactionType(org.pdm.backend.enums.TransactionType.valueOf(rs.getString("transaction_type")));
        proposal.setStatus(org.pdm.backend.enums.ProposalStatus.valueOf(rs.getString("status")));
        if (rs.getObject("product_id") != null) {
            proposal.setProductId(rs.getLong("product_id"));
        }
        if (rs.getObject("supplier_id") != null) {
            proposal.setSupplierId(rs.getLong("supplier_id"));
        }
        proposal.setQuantity(rs.getInt("quantity"));
        proposal.setDescription(rs.getString("description"));
        proposal.setTotalPrice(rs.getBigDecimal("total_price"));
        proposal.setProposedBy(rs.getLong("proposed_by"));
        if (rs.getObject("reviewed_by") != null) {
            proposal.setReviewedBy(rs.getLong("reviewed_by"));
        }
        proposal.setReviewNote(rs.getString("review_note"));
        if (rs.getTimestamp("created_at") != null) {
            proposal.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        if (rs.getTimestamp("reviewed_at") != null) {
            proposal.setReviewedAt(rs.getTimestamp("reviewed_at").toLocalDateTime());
        }
        try {
            if (rs.getObject("transaction_id") != null) {
                proposal.setTransactionId(rs.getLong("transaction_id"));
            }
        } catch (java.sql.SQLException ignored) {
            // Column absent in pre-migration schema — transactionId stays null
        }
        return proposal;
    };

    private Proposal hydrateProposal(Proposal proposal) {
        if (proposal.getProductId() != null) {
            proposal.setProduct(productRepository.findById(proposal.getProductId()).orElse(null));
        }
        if (proposal.getSupplierId() != null) {
            proposal.setSupplier(supplierRepository.findById(proposal.getSupplierId()).orElse(null));
        }
        if (proposal.getProposedBy() != null) {
            proposal.setProposedByUser(
                    userRepository.findById(proposal.getProposedBy())
                            .map(this::sanitizeUser)
                            .orElse(null)
            );
        }
        if (proposal.getReviewedBy() != null) {
            proposal.setReviewedByUser(
                    userRepository.findById(proposal.getReviewedBy())
                            .map(this::sanitizeUser)
                            .orElse(null)
            );
        }
        return proposal;
    }

    private User sanitizeUser(User user) {
        user.setPassword(null);
        return user;
    }

    @Override
    /**
     * Persists a proposal and hydrates its associated entities.
     *
     * @param proposal The proposal to save.
     * @return The saved proposal with hydrated entities.
     */
    public Proposal save(Proposal proposal) {
        String sql = "INSERT INTO proposals (transaction_type, status, product_id, supplier_id, quantity, description, total_price, proposed_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, proposal.getTransactionType().name());
            ps.setString(2, proposal.getStatus().name());
            if (proposal.getProductId() != null) {
                ps.setLong(3, proposal.getProductId());
            } else {
                ps.setNull(3, java.sql.Types.BIGINT);
            }
            if (proposal.getSupplierId() != null) {
                ps.setLong(4, proposal.getSupplierId());
            } else {
                ps.setNull(4, java.sql.Types.BIGINT);
            }
            ps.setInt(5, proposal.getQuantity());
            ps.setString(6, proposal.getDescription());
            if (proposal.getTotalPrice() != null) {
                ps.setBigDecimal(7, proposal.getTotalPrice());
            } else {
                ps.setNull(7, java.sql.Types.DECIMAL);
            }
            ps.setLong(8, proposal.getProposedBy());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            proposal.setId(key.longValue());
        }
        return hydrateProposal(proposal);
    }

    @Override
    /**
     * Finds a proposal by ID and hydrates its associated entities.
     *
     * @param id The ID of the proposal.
     * @return An Optional containing the hydrated proposal if found.
     */
    public Optional<Proposal> findById(Long id) {
        String sql = "SELECT * FROM proposals WHERE id = ?";
        List<Proposal> results = jdbcTemplate.query(sql, mapRowToProposal, id);
        if (results.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(hydrateProposal(results.get(0)));
    }

    @Override
    /**
     * Retrieves a paginated list of proposals with hydration.
     *
     * @param page    The page number (1-indexed).
     * @param size    The number of elements per page.
     * @param sortBy  The field to sort by (e.g., "status", "transaction_type").
     * @param sortDir The sorting direction ("ASC" or "DESC").
     * @return A list of hydrated proposals for the page.
     */
    public List<Proposal> findAllPaged(int page, int size, String sortBy, String sortDir) {
        String column = "created_at";
        if ("status".equals(sortBy)) {
            column = "status";
        } else if ("transaction_type".equals(sortBy)) {
            column = "transaction_type";
        }
        String direction = "DESC".equalsIgnoreCase(sortDir) ? "DESC" : "ASC";
        String sql = "SELECT * FROM proposals ORDER BY " + column + " " + direction + " LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, mapRowToProposal, size, (page - 1) * size)
                .stream()
                .map(this::hydrateProposal)
                .toList();
    }

    @Override
    /**
     * Counts total number of proposals in the database.
     *
     * @return The total count of proposals.
     */
    public long count() {
        String sql = "SELECT COUNT(*) FROM proposals";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    @Override
    /**
     * Counts proposals that are currently in PENDING status.
     *
     * @return The count of pending proposals.
     */
    public long countPending() {
        String sql = "SELECT COUNT(*) FROM proposals WHERE status = 'PENDING'";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    @Override
    /**
     * Links a proposal to an executed transaction.
     *
     * @param proposalId   The ID of the proposal.
     * @param transactionId The ID of the resulting transaction.
     */
    public void updateTransactionId(Long proposalId, Long transactionId) {
        jdbcTemplate.update(
            "UPDATE proposals SET transaction_id = ? WHERE id = ?",
            transactionId, proposalId
        );
    }

    @Override
    /**
     * Updates the status of a proposal and records the reviewer's information.
     *
     * @param id         The ID of the proposal.
     * @param status     The new status.
     * @param reviewedBy The ID of the user who reviewed the proposal.
     * @param reviewNote The note from the reviewer.
     */
    public void updateStatus(Long id, String status, Long reviewedBy, String reviewNote) {
        String sql = "UPDATE proposals SET status = ?, reviewed_by = ?, review_note = ?, reviewed_at = NOW() WHERE id = ?";
        if (reviewedBy != null) {
            jdbcTemplate.update(sql, status, reviewedBy, reviewNote, id);
        } else {
            jdbcTemplate.update(sql, status, null, reviewNote, id);
        }
    }
}
