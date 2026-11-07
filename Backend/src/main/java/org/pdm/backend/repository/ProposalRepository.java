package org.pdm.backend.repository;

import org.pdm.backend.model.Proposal;

import java.util.List;
import java.util.Optional;

public interface ProposalRepository {
    /**
     * Saves a new proposal.
     *
     * @param proposal The proposal to save.
     * @return The saved Proposal object.
     */
    Proposal save(Proposal proposal);

    /**
     * Retrieves a proposal by its ID.
     *
     * @param id The ID of the proposal.
     * @return An Optional containing the proposal if found.
     */
    Optional<Proposal> findById(Long id);

    /**
     * Retrieves a paginated list of proposals.
     *
     * @param page The page number.
     * @param size The number of records per page.
     * @param sortBy The field to sort by.
     * @param sortDir The direction of sorting (ASC or DESC).
     * @return A list of paginated proposals.
     */
    List<Proposal> findAllPaged(int page, int size, String sortBy, String sortDir);

    /**
     * Counts the total number of proposals.
     *
     * @return The total count.
     */
    long count();

    /**
     * Counts the total number of proposals in PENDING status.
     *
     * @return The count of pending proposals.
     */
    long countPending();

    /**
     * Updates the status and review details of a proposal.
     *
     * @param id The ID of the proposal.
     * @param status The new status.
     * @param reviewedBy The ID of the user who reviewed it.
     * @param reviewNote The review note.
     */
    void updateStatus(Long id, String status, Long reviewedBy, String reviewNote);

    /**
     * Links a proposal to its executed transaction.
     *
     * @param proposalId The ID of the proposal.
     * @param transactionId The ID of the transaction.
     */
    void updateTransactionId(Long proposalId, Long transactionId);
}