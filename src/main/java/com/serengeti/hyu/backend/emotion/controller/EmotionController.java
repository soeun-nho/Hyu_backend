package com.serengeti.hyu.backend.emotion.controller;

import com.serengeti.hyu.backend.emotion.dto.EmotionDetailResponseDto;
import com.serengeti.hyu.backend.emotion.dto.EmotionDto;
import com.serengeti.hyu.backend.emotion.dto.EmotionResponseDto;
import com.serengeti.hyu.backend.emotion.entity.Emotion;
import com.serengeti.hyu.backend.emotion.exception.EmotionAlreadyExistsException;
import com.serengeti.hyu.backend.emotion.service.EmotionService;
import com.serengeti.hyu.backend.user.entity.User;
import com.serengeti.hyu.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.http.HttpStatus;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/hue-records")
public class EmotionController {

    @Autowired
    private EmotionService emotionService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<String> createEmotion(@RequestBody EmotionDto request, @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authorized"); // 사용자 인증 실패 메시지
        }
        Long userId = user.getUserId();
        emotionService.createEmotion(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Emotion record created successfully"); // 성공 메시지 반환
    }


//    @PatchMapping("/{recordId}")
//    public ResponseEntity<Emotion> updateEmotion(@PathVariable int recordId, @RequestBody EmotionDto request, @AuthenticationPrincipal User user) {
//        if (user == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null); // 사용자 인증 실패
//        }
//        Long userId = user.getUserId();
//        Emotion emotion = emotionService.updateEmotion(userId, recordId, request);
//        return ResponseEntity.ok(emotion);
//    }


    @PatchMapping("/{recordId}")
    public ResponseEntity<EmotionDetailResponseDto> updateEmotion(@PathVariable int recordId, @RequestBody EmotionDto request, @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null); // 사용자 인증 실패
        }
        Long userId = user.getUserId();
        Emotion emotion = emotionService.updateEmotion(userId, recordId, request);

        // EmotionDetailResponseDto로 변환하여 반환
        EmotionDetailResponseDto responseDto = EmotionDetailResponseDto.builder()
                .username(emotion.getUser().getUsername())
                .content(emotion.getContent())
                .emotionImg(emotion.getEmotionImg())
                .recordDate(emotion.getRecordDate().toString()) // Date를 String으로 변환
                .build();

        return ResponseEntity.ok(responseDto);
    }


    @GetMapping
    public ResponseEntity<List<EmotionResponseDto>> getEmotions(@RequestParam(required = false) String date, @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null); // 사용자 인증 실패
        }
        Long userId = user.getUserId();
        Date queryDate;
        if (date == null) {
            queryDate = new Date(); // 날짜가 지정되지 않은 경우 오늘 날짜로 설정
        } else {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                queryDate = dateFormat.parse(date);
            } catch (ParseException e) {
                return ResponseEntity.badRequest().body(null); // 잘못된 날짜 포맷 처리
            }
        }

        List<EmotionResponseDto> emotions = emotionService.getEmotionsByWeek(userId, queryDate);
        return ResponseEntity.ok(emotions);
    }



    @GetMapping("/detail")
    public ResponseEntity<Emotion> getEmotionByDate(@RequestParam String date, @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null); // 사용자 인증 실패
        }
        Long userId = user.getUserId();
        Date queryDate;
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            queryDate = dateFormat.parse(date);
        } catch (ParseException e) {
            return ResponseEntity.badRequest().body(null); // 잘못된 날짜 포맷 처리
        }

        Emotion emotion = emotionService.getEmotionByDate(userId, queryDate);
        if (emotion == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // 감정 기록이 없을 경우 404 반환
        }

        return ResponseEntity.ok(emotion);
    }

    // 예외 처리
    @ExceptionHandler(EmotionAlreadyExistsException.class)
    public ResponseEntity<String> handleEmotionAlreadyExistsException(EmotionAlreadyExistsException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }
}
