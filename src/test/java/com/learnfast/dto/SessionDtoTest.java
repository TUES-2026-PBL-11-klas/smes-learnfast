package com.learnfast.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class SessionDtoTest {

    private UserDto studentDto;
    private UserDto mentorDto;

    @BeforeEach
    void setUp() {
        // UserDto has no public setters — build via UserDto.from() would require
        // full User objects; use two separate instances sourced from distinct Users
        // instead we just verify the reference identity after set/get

        studentDto = new UserDto();
        mentorDto = new UserDto();
    }

    @Test
    void defaultConstructor_createsInstance() {
        SessionDto dto = new SessionDto();
        assertThat(dto).isNotNull();
    }

    @Test
    void allFieldsNullByDefault() {
        SessionDto dto = new SessionDto();
        assertThat(dto.getId()).isNull();
        assertThat(dto.getStudent()).isNull();
        assertThat(dto.getMentor()).isNull();
        assertThat(dto.getStatus()).isNull();
        assertThat(dto.getCreatedAt()).isNull();
        assertThat(dto.getRoomId()).isNull();
    }

    @Test
    void setAndGetId() {
        SessionDto dto = new SessionDto();
        dto.setId(7L);
        assertThat(dto.getId()).isEqualTo(7L);
    }

    @Test
    void setAndGetStudent() {
        SessionDto dto = new SessionDto();
        dto.setStudent(studentDto);
        assertThat(dto.getStudent()).isSameAs(studentDto);
    }

    @Test
    void setAndGetMentor() {
        SessionDto dto = new SessionDto();
        dto.setMentor(mentorDto);
        assertThat(dto.getMentor()).isSameAs(mentorDto);
    }

    @Test
    void setAndGetStatus() {
        SessionDto dto = new SessionDto();
        dto.setStatus("PENDING");
        assertThat(dto.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void status_acceptsAllSessionStatuses() {
        SessionDto dto = new SessionDto();
        for (String status : new String[]{"PENDING", "ACCEPTED", "REJECTED", "COMPLETED"}) {
            dto.setStatus(status);
            assertThat(dto.getStatus()).isEqualTo(status);
        }
    }

    @Test
    void setAndGetCreatedAt() {
        SessionDto dto = new SessionDto();
        LocalDateTime time = LocalDateTime.of(2024, 4, 1, 9, 0);
        dto.setCreatedAt(time);
        assertThat(dto.getCreatedAt()).isEqualTo(time);
    }

    @Test
    void setAndGetRoomId() {
        SessionDto dto = new SessionDto();
        dto.setRoomId("room-abc-xyz");
        assertThat(dto.getRoomId()).isEqualTo("room-abc-xyz");
    }

    @Test
    void studentAndMentorDtosAreIndependent() {
        SessionDto dto = new SessionDto();
        dto.setStudent(studentDto);
        dto.setMentor(mentorDto);
        assertThat(dto.getStudent()).isNotSameAs(dto.getMentor());
    }

    @Test
    void fullyPopulatedDto_hasAllFieldsSet() {
        SessionDto dto = new SessionDto();
        LocalDateTime time = LocalDateTime.of(2024, 4, 1, 9, 0);

        dto.setId(1L);
        dto.setStudent(studentDto);
        dto.setMentor(mentorDto);
        dto.setStatus("ACCEPTED");
        dto.setCreatedAt(time);
        dto.setRoomId("room-123");

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getStudent()).isSameAs(studentDto);
        assertThat(dto.getMentor()).isSameAs(mentorDto);
        assertThat(dto.getStatus()).isEqualTo("ACCEPTED");
        assertThat(dto.getCreatedAt()).isEqualTo(time);
        assertThat(dto.getRoomId()).isEqualTo("room-123");
    }
}
