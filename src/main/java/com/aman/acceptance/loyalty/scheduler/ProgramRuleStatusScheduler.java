package com.aman.acceptance.loyalty.scheduler;

import com.aman.acceptance.loyalty.enums.RuleStatus;
import com.aman.acceptance.loyalty.model.RuleVersion;
import com.aman.acceptance.loyalty.repository.RuleVersionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProgramRuleStatusScheduler {

    private final RuleVersionRepository ruleVersionRepository;
    private final CacheManager cacheManager;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void updateRuleStatuses() {

        LocalDateTime now = LocalDateTime.now();

        System.out.println("SCHEDULER NOW = " + now);

        List<RuleVersion> scheduledRules =
                ruleVersionRepository
                        .findByStatusAndEffectiveFromLessThanEqual(
                                RuleStatus.SCHEDULED,
                                now
                        );

        System.out.println(
                "SCHEDULED RULES FOUND = " + scheduledRules.size()
        );

        for (RuleVersion rule : scheduledRules) {

            System.out.println(
                    "ACTIVATING VERSION = " + rule.getVersion()
            );

            rule.setStatus(RuleStatus.ACTIVE);

            Cache cache = cacheManager.getCache("activeProgramRules");

            if (cache != null) {
                cache.evict(rule.getProgram().getId());
            }
        }

        List<RuleVersion> activeRules =
                ruleVersionRepository
                        .findByStatusAndEffectiveToLessThanEqual(
                                RuleStatus.ACTIVE,
                                now
                        );

        System.out.println(
                "ACTIVE RULES TO CLOSE = " + activeRules.size()
        );

        for (RuleVersion rule : activeRules) {

            System.out.println(
                    "CLOSING VERSION = " + rule.getVersion()
            );

            rule.setStatus(RuleStatus.CLOSED);

            Cache cache = cacheManager.getCache("activeProgramRules");

            if (cache != null) {
                cache.evict(rule.getProgram().getId());
            }
        }
    }
}