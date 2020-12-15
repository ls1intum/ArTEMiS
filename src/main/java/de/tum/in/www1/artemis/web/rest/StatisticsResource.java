package de.tum.in.www1.artemis.web.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import de.tum.in.www1.artemis.domain.enumeration.GraphType;
import de.tum.in.www1.artemis.domain.enumeration.SpanType;
import de.tum.in.www1.artemis.service.*;

/**
 * REST controller for managing user statistics.
 */
@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('ADMIN')")
public class StatisticsResource {

    private final Logger log = LoggerFactory.getLogger(StatisticsResource.class);

    private final StatisticsService service;

    public StatisticsResource(StatisticsService service) {
        this.service = service;
    }

    /**
     * GET management/statistics/data : get the graph data in the last "span" days in the given period.
     *
     * @param span the spantime of which the amount should be calculated
     * @param periodIndex an index indicating which time period, 0 is current week, -1 is one week in the past, -2 is two weeks in the past ...
     * @param graphType the type of graph the data should be fetched
     * @return the ResponseEntity with status 200 (OK) and the data in body, or status 404 (Not Found)
     */
    @GetMapping("management/statistics/data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Integer[]> getChartData(@RequestParam SpanType span, @RequestParam Integer periodIndex, @RequestParam GraphType graphType) {
        log.debug("REST request to get graph data");
        return ResponseEntity.ok(this.service.getChartData(span, periodIndex, graphType));
    }

}
