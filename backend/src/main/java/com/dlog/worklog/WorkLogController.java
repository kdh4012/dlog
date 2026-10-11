package com.dlog.worklog;

import com.dlog.worklog.dto.WorkLogRequest;
import com.dlog.worklog.dto.WorkLogResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/work-logs")
@RequiredArgsConstructor
public class WorkLogController {

    private final WorkLogService workLogService;

    @GetMapping
    public List<WorkLogResponse> getAll() {
        return workLogService.getAll();
    }

    @GetMapping("/{id}")
    public WorkLogResponse get(@PathVariable Long id) {
        return workLogService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkLogResponse create(@Valid @RequestBody WorkLogRequest request) {
        return workLogService.create(request);
    }

    @PutMapping("/{id}")
    public WorkLogResponse update(@PathVariable Long id, @Valid @RequestBody WorkLogRequest request) {
        return workLogService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        workLogService.delete(id);
    }
}
