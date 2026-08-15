package com.orderflow.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class OrderStatsDTO {
    private long todayCount;
    private long pendingCount;
    private long shippedCount;
    private long completedCount;
    private long totalCount;
    private long totalSalesCent;
    private List<StatusCount> statusDistribution;
    private List<DailyStat> last7Days;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StatusCount {
        private String status;
        private long count;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyStat {
        private String date;
        private long count;
        private long amountCent;
    }
}
