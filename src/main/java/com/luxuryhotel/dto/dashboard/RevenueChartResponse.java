package com.luxuryhotel.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record RevenueChartResponse(
        List<String> labels,
        List<BigDecimal> currentYear,
        List<BigDecimal> previousYear
) {
}
