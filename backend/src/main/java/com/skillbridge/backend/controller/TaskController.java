package com.skillbridge.backend.controller;

import com.skillbridge.backend.dto.CommonDtos;
import com.skillbridge.backend.entity.User;
import com.skillbridge.backend.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** U4 / F7–F8: create, assign, update status, submit for review, review decisions, comments. */
@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskService tasks;

    public TaskController(TaskService tasks) {
        this.tasks = tasks;
    }

    @GetMapping("/tasks/mine")
    public ResponseEntity<List<TaskService.TaskView>> mine(@AuthenticationPrincipal User me) {
        return ResponseEntity.ok(tasks.myTasks(me));
    }

    @GetMapping("/tasks/managed")
    public ResponseEntity<List<TaskService.TaskView>> managed(@AuthenticationPrincipal User me) {
        return ResponseEntity.ok(tasks.managedTasks(me));
    }

    @GetMapping("/projects/{projectId}/tasks")
    public ResponseEntity<List<TaskService.TaskView>> projectTasks(@AuthenticationPrincipal User me,
                                                                   @PathVariable Long projectId) {
        return ResponseEntity.ok(tasks.projectTasks(me, projectId));
    }

    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<TaskService.TaskView> create(@AuthenticationPrincipal User me,
                                                       @PathVariable Long projectId,
                                                       @Valid @RequestBody CommonDtos.CreateTaskRequest request) {
        // keep path and body consistent
        if (request.projectId() != null && !request.projectId().equals(projectId)) {
            throw new com.skillbridge.backend.exception.BadRequestException("Project in path and body do not match.");
        }
        CommonDtos.CreateTaskRequest body = new CommonDtos.CreateTaskRequest(
                projectId, request.title(), request.description(), request.acceptanceCriteria(),
                request.priority(), request.status(), request.milestoneId(), request.assigneeId(),
                request.dependsOnId(), request.dueDate(), request.blockerNote());
        return ResponseEntity.ok(tasks.create(me, body));
    }

    @PutMapping("/tasks/{taskId}")
    public ResponseEntity<TaskService.TaskView> update(@AuthenticationPrincipal User me, @PathVariable Long taskId,
                                                       @Valid @RequestBody CommonDtos.UpdateTaskRequest request) {
        return ResponseEntity.ok(tasks.update(me, taskId, request));
    }

    @PostMapping("/tasks/{taskId}/status")
    public ResponseEntity<TaskService.TaskView> status(@AuthenticationPrincipal User me, @PathVariable Long taskId,
                                                       @Valid @RequestBody CommonDtos.TaskStatusRequest request) {
        return ResponseEntity.ok(tasks.updateStatus(me, taskId, request));
    }

    @PostMapping("/tasks/{taskId}/cancel")
    public ResponseEntity<TaskService.TaskView> cancel(@AuthenticationPrincipal User me, @PathVariable Long taskId) {
        return ResponseEntity.ok(tasks.cancel(me, taskId));
    }    @PostMapping("/tasks/{taskId}/review")
    public ResponseEntity<TaskService.TaskView> review(@AuthenticationPrincipal User me, @PathVariable Long taskId,
                                                       @Valid @RequestBody CommonDtos.ReviewRequest request) {
        return ResponseEntity.ok(tasks.review(me, taskId, request));
    }

    @GetMapping("/tasks/{taskId}/reviews")
    public ResponseEntity<List<CommonDtos.ReviewResponse>> reviews(@AuthenticationPrincipal User me,
                                                                   @PathVariable Long taskId) {
        return ResponseEntity.ok(tasks.taskReviews(taskId));
    }

    @GetMapping("/tasks/{taskId}/comments")
    public ResponseEntity<List<CommonDtos.CommentResponse>> comments(@AuthenticationPrincipal User me,
                                                                     @PathVariable Long taskId) {
        return ResponseEntity.ok(tasks.comments(me, taskId));
    }

    @PostMapping("/tasks/{taskId}/comments")
    public ResponseEntity<CommonDtos.CommentResponse> comment(@AuthenticationPrincipal User me,
                                                              @PathVariable Long taskId,
                                                              @Valid @RequestBody CommonDtos.CommentRequest request) {
        return ResponseEntity.ok(tasks.addComment(me, taskId, request.body()));
    }

    @GetMapping("/projects/{projectId}/assignee-suggestions")
    public ResponseEntity<List<CommonDtos.AssigneeSuggestion>> suggestions(@AuthenticationPrincipal User me,
                                                                           @PathVariable Long projectId) {
        return ResponseEntity.ok(tasks.assigneeSuggestions(me, projectId));
    }
}
