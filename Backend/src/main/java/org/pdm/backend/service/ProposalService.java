package org.pdm.backend.service;

import org.pdm.backend.wrappers.Response;

public interface ProposalService {
    /**
     * Creates a new transaction proposal based on the provided request.
     *
     * @param request The transaction request containing proposal details.
     * @return A Response indicating the result of the proposal creation.
     */
    Response createProposal(org.pdm.backend.wrappers.TransactionRequest request);

    /**
     * Retrieves a paginated list of all transaction proposals.
     *
     * @param page The page number to retrieve.
     * @param size The number of records per page.
     * @param sortBy The field to sort by.
     * @param sortDir The direction of sorting (ASC or DESC).
     * @return A Response containing the paginated list of proposals.
     */
    Response getAllProposals(int page, int size, String sortBy, String sortDir);

    /**
     * Retrieves a specific proposal by its ID.
     *
     * @param id The ID of the proposal.
     * @return A Response containing the proposal details.
     * @throws org.pdm.backend.exception.NotFoundException if the proposal is not found.
     */
    Response getProposalById(Long id);

    /**
     * Approves a transaction proposal and executes the transaction.
     *
     * @param id The ID of the proposal to approve.
     * @param reviewNote The note associated with the approval.
     * @return A Response indicating the result of the approval process.
     */
    Response approveProposal(Long id, String reviewNote);

    /**
     * Rejects a transaction proposal.
     *
     * @param id The ID of the proposal to reject.
     * @param reviewNote The note associated with the rejection.
     * @return A Response indicating the result of the rejection process.
     */
    Response rejectProposal(Long id, String reviewNote);

    /**
     * Retrieves the count of pending transaction proposals.
     *
     * @return A Response containing the count of pending proposals.
     */
    Response getPendingCount();

    /**
     * Exports all proposals to a CSV format.
     *
     * @return A CSV string containing the proposal data.
     */
    String exportProposalsCsv();
}