package com.careflow.serviceops.repository;

import com.careflow.serviceops.domain.SlaHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SlaHolidayRepository extends JpaRepository<SlaHoliday, UUID> {

    @Query("""
            select h.holidayDate from SlaHoliday h
            where h.organizationId = :organizationId
              and (h.siteId is null or h.siteId = :siteId)
              and h.holidayDate between :from and :to
            """)
    List<LocalDate> findHolidayDates(@Param("organizationId") UUID organizationId,
                                     @Param("siteId") UUID siteId,
                                     @Param("from") LocalDate from,
                                     @Param("to") LocalDate to);
}