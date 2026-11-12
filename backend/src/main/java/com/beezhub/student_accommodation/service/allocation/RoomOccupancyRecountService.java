package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.config.TransactionTemplateConfig;
import com.beezhub.student_accommodation.repository.RoomRepository;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Nightly recount of every room's occupancy (GH-541).
 *
 * <p>{@link RoomOccupancyManager} counts who is in a room today, and an OFFERED or ACCEPTED
 * allocation only starts counting on its stay start date and stops on its end date. Nothing
 * changes on those dates, so no write path recounts the room then. Recounting every room shortly
 * after midnight covers both edges, and also repairs rows left wrong by earlier bugs, such as
 * check-in never syncing. Rooms are few and each recount is one count query, with a write only
 * when the value changed, so a full pass is cheaper than working out which rooms to visit.
 *
 * <p>Runs outside a request, so the tenant context is cleared for the run: the job is platform
 * maintenance and must see every company's rooms. Each room is recounted in its own transaction,
 * so one failure costs one room.
 */
@Service
@Slf4j
public class RoomOccupancyRecountService {

    private final RoomRepository roomRepository;
    private final RoomOccupancyManager roomOccupancyManager;
    private final TransactionTemplate transactionTemplate;

    public RoomOccupancyRecountService(
            RoomRepository roomRepository,
            RoomOccupancyManager roomOccupancyManager,
            @Qualifier(TransactionTemplateConfig.REQUIRES_NEW) TransactionTemplate transactionTemplate) {
        this.roomRepository = roomRepository;
        this.roomOccupancyManager = roomOccupancyManager;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Daily job body, called by {@code ScheduledJobRunner}.
     *
     * @return how many rooms had a wrong occupancy or status and were corrected
     */
    public int recountAll() {
        Long previousTenant = TenantContext.getTenantId();
        TenantContext.clear();
        try {
            List<Long> roomIds = roomRepository.findAllIds();
            int corrected = 0;
            for (Long roomId : roomIds) {
                if (recount(roomId)) {
                    corrected++;
                }
            }
            log.info("action=roomOccupancyRecount rooms={} corrected={}", roomIds.size(), corrected);
            return corrected;
        } finally {
            if (previousTenant != null) {
                TenantContext.setTenantId(previousTenant);
            }
        }
    }

    private boolean recount(Long roomId) {
        try {
            Boolean changed = transactionTemplate.execute(status -> roomRepository.findById(roomId)
                    .map(roomOccupancyManager::syncOccupancy)
                    .orElse(false));
            return Boolean.TRUE.equals(changed);
        } catch (Exception e) {
            log.error("action=roomOccupancyRecountFailed roomId={}", roomId, e);
            return false;
        }
    }
}
