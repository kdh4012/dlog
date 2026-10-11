package com.dlog.worklog;

import com.dlog.global.error.BusinessException;
import com.dlog.global.error.ErrorCode;
import com.dlog.worklog.dto.WorkLogRequest;
import com.dlog.worklog.dto.WorkLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkLogService {

    private final WorkLogRepository workLogRepository;

    public List<WorkLogResponse> getAll() {
        return workLogRepository.findAllByOrderByStartDateDescIdDesc().stream()
                .map(WorkLogResponse::from)
                .toList();
    }

    public WorkLogResponse get(Long id) {
        return WorkLogResponse.from(findById(id));
    }

    @Transactional
    public WorkLogResponse create(WorkLogRequest r) {
        WorkLog workLog = WorkLog.builder()
                .title(r.title())
                .organization(r.organization())
                .role(r.role())
                .startDate(r.startDate())
                .endDate(r.endDate())
                .techStack(r.techStack())
                .content(r.content())
                .build();
        return WorkLogResponse.from(workLogRepository.save(workLog));
    }

    @Transactional
    public WorkLogResponse update(Long id, WorkLogRequest r) {
        WorkLog workLog = findById(id);
        workLog.update(r.title(), r.organization(), r.role(), r.startDate(), r.endDate(), r.techStack(), r.content());
        return WorkLogResponse.from(workLog);
    }

    @Transactional
    public void delete(Long id) {
        workLogRepository.delete(findById(id));
    }

    private WorkLog findById(Long id) {
        return workLogRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORK_LOG_NOT_FOUND));
    }
}
