package com.freightfox.transporter.repository;

import com.freightfox.transporter.entity.LaneQuote;
import com.freightfox.transporter.optimization.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LaneQuoteRepository extends JpaRepository<LaneQuote, Long> {

    /** All quotes of a dataset in one query, already projected to client-facing ids (no N+1). */
    @Query("""
            select new com.freightfox.transporter.optimization.Quote(l.laneRef, t.transporterRef, q.quote)
            from LaneQuote q join q.lane l join q.transporter t
            where l.dataset.id = :datasetId
            """)
    List<Quote> findQuotesByDatasetId(Long datasetId);
}
