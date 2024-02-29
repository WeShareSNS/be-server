package com.weshare.api.v1.domain.schedule;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long id;

    private String title;

    @Enumerated(EnumType.STRING)
    private Destination destination;

    private LocalDate startDate;

    private LocalDate endDate;

    @Embedded
    private Days days;

    @Builder
    private Schedule(String title, Destination destination, LocalDate startDate, LocalDate endDate, Days days) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작, 종료 날짜가 올바르지 않습니다.");
        }
        if (!days.areAllDistinctDaysWithinRange(startDate, endDate) || !days.isDayCountMatching(startDate, endDate)) {
            throw new IllegalArgumentException("날짜 정보가 올바르지 않습니다.");
        }
        this.title = title;
        this.destination = destination;
        this.startDate = startDate;
        this.endDate = endDate;
        this.days = days;
    }

    public BigDecimal getTotalScheduleExpense() {
        return days.getTotalDaysExpense().getValue();
    }

}