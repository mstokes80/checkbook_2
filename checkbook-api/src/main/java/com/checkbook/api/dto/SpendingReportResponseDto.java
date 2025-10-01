package com.checkbook.api.dto;

import java.util.List;

public class SpendingReportResponseDto {
    private ReportMetadataDto reportMetadata;
    private ReportSummaryDto summary;
    private List<CategoryBreakdownDto> categoryBreakdown;
    private List<CategoryBreakdownDto> topCategories;
    private List<TrendDataDto> trendData;

    public SpendingReportResponseDto() {
    }

    public SpendingReportResponseDto(ReportMetadataDto reportMetadata, ReportSummaryDto summary,
                                    List<CategoryBreakdownDto> categoryBreakdown,
                                    List<CategoryBreakdownDto> topCategories,
                                    List<TrendDataDto> trendData) {
        this.reportMetadata = reportMetadata;
        this.summary = summary;
        this.categoryBreakdown = categoryBreakdown;
        this.topCategories = topCategories;
        this.trendData = trendData;
    }

    public ReportMetadataDto getReportMetadata() {
        return reportMetadata;
    }

    public void setReportMetadata(ReportMetadataDto reportMetadata) {
        this.reportMetadata = reportMetadata;
    }

    public ReportSummaryDto getSummary() {
        return summary;
    }

    public void setSummary(ReportSummaryDto summary) {
        this.summary = summary;
    }

    public List<CategoryBreakdownDto> getCategoryBreakdown() {
        return categoryBreakdown;
    }

    public void setCategoryBreakdown(List<CategoryBreakdownDto> categoryBreakdown) {
        this.categoryBreakdown = categoryBreakdown;
    }

    public List<CategoryBreakdownDto> getTopCategories() {
        return topCategories;
    }

    public void setTopCategories(List<CategoryBreakdownDto> topCategories) {
        this.topCategories = topCategories;
    }

    public List<TrendDataDto> getTrendData() {
        return trendData;
    }

    public void setTrendData(List<TrendDataDto> trendData) {
        this.trendData = trendData;
    }
}