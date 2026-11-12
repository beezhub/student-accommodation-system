package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.dto.ContactInfoResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContactInfoServiceTest {

    @Test
    void getContactInfo_bothSet_returnsBothTrimmed() {
        ContactInfoService service = new ContactInfoService(" help@example.co.za ", " +27 11 555 0100 ");

        assertThat(service.getContactInfo())
                .isEqualTo(new ContactInfoResponse("help@example.co.za", "+27 11 555 0100"));
    }

    @Test
    void getContactInfo_phoneBlank_returnsNullPhone() {
        ContactInfoService service = new ContactInfoService("help@example.co.za", "   ");

        assertThat(service.getContactInfo().phone()).isNull();
    }

    @Test
    void getContactInfo_phoneEmpty_returnsNullPhone() {
        ContactInfoService service = new ContactInfoService("help@example.co.za", "");

        assertThat(service.getContactInfo().phone()).isNull();
    }
}
