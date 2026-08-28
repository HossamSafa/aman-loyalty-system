package com.aman.acceptance.loyalty.service.validators;

import com.aman.acceptance.loyalty.enums.ErrorCode;
import com.aman.acceptance.loyalty.exception.BusinessException;
import com.aman.acceptance.loyalty.repository.LoyaltyProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ReportValidator {

    private final LoyaltyProgramRepository loyaltyProgramRepository;

    public void validate(Long programId, LocalDateTime from, LocalDateTime to) {
        validateDates(from, to);
        validateProgramExists(programId);
    }

    private void validateDates(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            throw BusinessException.badRequest(
                    ErrorCode.INVALID_REPORT_DATE_RANGE,
                    "From and to dates are required"
            );
        }

        if (from.isAfter(to)) {
            throw BusinessException.badRequest(
                    ErrorCode.INVALID_REPORT_DATE_RANGE,
                    "From date must not be after to date"
            );
        }
    }

    private void validateProgramExists(Long programId) {
        if (!loyaltyProgramRepository.existsById(programId)) {
            throw BusinessException.notFound(
                    ErrorCode.LOYALTY_PROGRAM_NOT_FOUND,
                    "Loyalty program not found: " + programId
            );
        }
    }
}