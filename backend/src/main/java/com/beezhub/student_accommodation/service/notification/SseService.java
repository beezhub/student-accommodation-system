package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.dto.UserNotificationResponse;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SseService {

    private final AppUserRepository appUserRepository;
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribeByEmail(String email) {
        Long userId = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
        return subscribe(userId);
    }

    public SseEmitter subscribe(Long userId) {
        // Set timeout to 30 minutes
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        emitters.put(userId, emitter);

        emitter.onCompletion(() -> {
            log.debug("SSE completion for user: {}", userId);
            emitters.remove(userId);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE timeout for user: {}", userId);
            emitter.complete();
            emitters.remove(userId);
        });

        emitter.onError((e) -> {
            log.debug("SSE error for user: {}", userId, e);
            emitter.complete();
            emitters.remove(userId);
        });

        // Send checking event to confirm connection
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("connected"));
        } catch (IOException e) {
            log.debug("Error sending connection confirmation to user: {}", userId);
            emitters.remove(userId);
        }

        log.info("User {} subscribed to SSE", userId);
        return emitter;
    }

    public void sendNotification(Long userId, UserNotificationResponse notification) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notification));
                log.debug("Sent SSE notification to user: {}", userId);
            } catch (IOException e) {
                log.debug("Error sending SSE notification to user: {}", userId);
                emitters.remove(userId);
            }
        }
    }
}
