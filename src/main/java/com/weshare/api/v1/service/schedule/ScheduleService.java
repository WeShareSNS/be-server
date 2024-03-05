package com.weshare.api.v1.service.schedule;

import com.weshare.api.v1.controller.schedule.CreateScheduleDto;
import com.weshare.api.v1.domain.schedule.*;
import com.weshare.api.v1.domain.user.User;
import com.weshare.api.v1.repository.schedule.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;

    @Transactional
    public void saveSchedule(final CreateScheduleDto createScheduleDto, User user) {
        Schedule schedule = createSchedule(createScheduleDto);
        schedule.setUser(user);
        scheduleRepository.save(schedule);
    }

    private Schedule createSchedule(CreateScheduleDto createScheduleDto) {
        return Schedule.builder()
                .title(createScheduleDto.getTitle())
                .destination(Destination.findDestinationByName(createScheduleDto.getDestination()))
                .startDate(createScheduleDto.getStartDate())
                .endDate(createScheduleDto.getEndDate())
                .days(
                        new Days(
                                createScheduleDto.getVisitDates().stream()
                                .map(this::createDay)
                                .toList()
                        )
                )
                .build();
    }

    private Day createDay(CreateScheduleDto.TravelDayDto travelDayDto) {
        return Day.builder()
                .travelDate(travelDayDto.getTravelDate())
                .places(travelDayDto.getVisitPlaceDtos()
                        .stream()
                        .map(this::createPlace)
                        .toList()
                )
                .build();
    }

    private Place createPlace(CreateScheduleDto.TravelDayDto.VisitPlaceDto visitPlaceDto) {
        return Place.builder()
                .title(visitPlaceDto.getTitle())
                .time(visitPlaceDto.getTime())
                .memo(visitPlaceDto.getMemo())
                .expense(new Money(visitPlaceDto.getExpense()))
                .location(new Location(
                        visitPlaceDto.getLatitude(),
                        visitPlaceDto.getLongitude())
                )
                .build();
    }
}
