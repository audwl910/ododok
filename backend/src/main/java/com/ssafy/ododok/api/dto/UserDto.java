package com.ssafy.ododok.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ssafy.ododok.db.model.Gender;
import com.ssafy.ododok.db.model.Onoff;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class UserDto {

    @Getter
    public static class Id {
        private int userId;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Basic {
        Long id;
        String userName;
        String userNickname;
        String userEmail;
        String userPassword;
        String userPhone;
        String userImage;
        int userReviewcnt;
        int completeDodokCnt;
        long boardcnt;
        Gender userGender;
        int userAge;
        String userGenre1;
        String userGenre2;
        String userGenre3;
        String userRegion;
        Onoff userOnoff;
        int userFrequency;
    }
}
