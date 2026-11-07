package org.pdm.backend.service;

import org.pdm.backend.wrappers.Response;

public interface DashboardService {
    /**
     * Retrieves statistics and metrics for the system dashboard.
     *
     * @return A Response containing dashboard statistics.
     */
    Response getDashboardStats();

    /**
     * Exports dashboard statistics to a CSV format.
     *
     * @return A CSV string containing the dashboard data.
     */
    String exportDashboardCsv();
}
