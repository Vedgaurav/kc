package com.one.kc.user.service;

import com.one.kc.chanting.dto.ChantingDashboardResponseDto;
import com.one.kc.chanting.dto.ChantingDto;
import com.one.kc.chanting.dto.PageResponse;
import com.one.kc.chanting.service.ChantingService;
import com.one.kc.common.enums.UserRole;
import com.one.kc.common.utils.PhoneNumberUtils;
import com.one.kc.common.utils.ResponseEntityUtils;
import com.one.kc.user.dto.FacilitatorListDto;
import com.one.kc.user.dto.FacilitatorUserListDto;
import com.one.kc.user.entity.User;
import com.one.kc.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class FacilitatorService {

    private final UserRepository userRepository;
    private final ChantingService chantingService;

    public FacilitatorService(UserRepository userRepository,
                              ChantingService chantingService
    ) {
        this.userRepository = userRepository;
        this.chantingService = chantingService;
    }

    public List<FacilitatorListDto> getFacilitators(Long userId) {
        List<User> users = userRepository.findActiveUsersByRole(UserRole.FACILITATOR);

        return users.stream()
                .filter(user -> !user.getUserId().equals(userId))
                .map(user ->
                     FacilitatorListDto.builder()
                            .id(String.valueOf(user.getUserId()))
                            .email(user.getEmail())
                            .name(user.getFirstName() + " " + user.getLastName())
                            .build()
                ).toList();

    }

    public ResponseEntity<PageResponse<FacilitatorUserListDto>> getFacilitatorUsers(Long userId, Pageable pageable) {
        Page<User> userListPage =  userRepository.findUsersByFacilitator(userId, pageable);

          List<FacilitatorUserListDto> facilitatorUserListDtoList =   userListPage.getContent().stream()
                    .map(userItem -> {
                                PhoneNumberUtils.PhoneParts phoneParts = PhoneNumberUtils.fromE164(userItem.getPhoneNumber());
                                      return  FacilitatorUserListDto.builder().userId(String.valueOf(userItem.getUserId()))
                                                .email(userItem.getEmail())
                                                .name(userItem.getFirstName() + " " + userItem.getLastName())
                                                .phone(phoneParts.countryCode()+" "+phoneParts.phoneNumber())
                                                .build();
                            }
                    ).toList();

          return ResponseEntityUtils.getPaginatedResponse(userListPage, facilitatorUserListDtoList);
    }

    public ResponseEntity<PageResponse<ChantingDto>> getFacilitatorUserChantingListByUserId(
            Long facilitatorId,
            Long userId,
            Pageable pageable

    ){
       Optional<User> userOptional =  userRepository.findByUserId(userId);
       if(userOptional.isPresent()) {
           User user = userOptional.get();
           if(user.getFacilitator().getUserId().equals(facilitatorId)) {
               return chantingService.getChantingListByUserId(userId, pageable);
           }
       }
        return ResponseEntity.badRequest().build();
    }

    public ChantingDashboardResponseDto getFacilitatorsUserDashboardDetails(
            Long facilitatorId,
            Long userId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        Optional<User> userOptional =  userRepository.findByUserId(userId);
        if(userOptional.isPresent()) {
            User user = userOptional.get();
            if(user.getFacilitator().getUserId().equals(facilitatorId)) {
                return chantingService.getDashboard(userId, fromDate, toDate);
            }
        }
       return null;
    }
}

