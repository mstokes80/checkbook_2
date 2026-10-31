import api from './apiClient';

export const reportService = {
  /**
   * Generate a spending report
   * @param {string} startDate - Start date in YYYY-MM-DD format
   * @param {string} endDate - End date in YYYY-MM-DD format
   * @param {Array<number>} accountIds - Optional array of account IDs
   * @returns {Promise} Report data
   */
  async generateReport(startDate, endDate, accountIds = null) {
    const params = {
      startDate,
      endDate
    };

    if (accountIds && accountIds.length > 0) {
      params.accountIds = accountIds.join(',');
    }

    const response = await api.get('/reports/spending', { params });
    return response.data;
  },

  /**
   * Export report as PDF
   * @param {string} startDate - Start date in YYYY-MM-DD format
   * @param {string} endDate - End date in YYYY-MM-DD format
   * @param {Array<number>} accountIds - Optional array of account IDs
   */
  async exportPdf(startDate, endDate, accountIds = null) {
    const params = {
      startDate,
      endDate
    };

    if (accountIds && accountIds.length > 0) {
      params.accountIds = accountIds.join(',');
    }

    const response = await api.get('/reports/spending/export/pdf', {
      params,
      responseType: 'blob'
    });

    // Create blob and download
    const blob = new Blob([response.data], { type: 'application/pdf' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `spending-report-${startDate}-${endDate}.pdf`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  },

  /**
   * Export report as CSV
   * @param {string} startDate - Start date in YYYY-MM-DD format
   * @param {string} endDate - End date in YYYY-MM-DD format
   * @param {Array<number>} accountIds - Optional array of account IDs
   */
  async exportCsv(startDate, endDate, accountIds = null) {
    const params = {
      startDate,
      endDate
    };

    if (accountIds && accountIds.length > 0) {
      params.accountIds = accountIds.join(',');
    }

    const response = await api.get('/reports/spending/export/csv', {
      params,
      responseType: 'blob'
    });

    // Create blob and download
    const blob = new Blob([response.data], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `spending-report-${startDate}-${endDate}.csv`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  }
};

export default reportService;