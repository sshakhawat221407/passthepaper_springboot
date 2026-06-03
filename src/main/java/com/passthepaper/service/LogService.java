package com.passthepaper.service;

import java.util.List;
import com.passthepaper.dto.AppealDto;
import com.passthepaper.entity.*;
import com.passthepaper.exception.AppException;
import com.passthepaper.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LogService {

    private final LogRepository logRepo;

    public void log(Log.LogType type, String action, String description,
                    UUID userId, String userName, UUID targetUserId, String targetUserName,
                    Map<String, Object> metadata) {
        logRepo.save(Log.builder()
                .type(type).action(action).description(description)
                .userId(userId).userName(userName)
                .targetUserId(targetUserId).targetUserName(targetUserName)
                .metadata(metadata).build());
    }

public List<Log> getLogs(int page, int size) {
    return logRepo.findAllByOrderByCreatedAtDesc(
            org.springframework.data.domain.PageRequest.of(page, size)).getContent();
}
}
