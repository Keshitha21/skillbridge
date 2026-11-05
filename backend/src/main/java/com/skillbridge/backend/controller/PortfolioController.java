package com.skillbridge.backend.controller;

import com.skillbridge.backend.dto.CommonDtos;
import com.skillbridge.backend.entity.User;
import com.skillbridge.backend.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** U8 / F14: portfolio entries with visibility settings. */
@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolio;

    public PortfolioController(PortfolioService portfolio) {
        this.portfolio = portfolio;
    }

    @GetMapping("/mine")
    public ResponseEntity<List<CommonDtos.PortfolioItemResponse>> mine(@AuthenticationPrincipal User me) {
        return ResponseEntity.ok(portfolio.mine(me));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CommonDtos.PortfolioItemResponse>> ofUser(@AuthenticationPrincipal User me,
                                                                         @PathVariable Long userId) {
        return ResponseEntity.ok(portfolio.forUser(userId, me));
    }

    @PostMapping
    public ResponseEntity<CommonDtos.PortfolioItemResponse> create(@AuthenticationPrincipal User me,
                                                                   @Valid @RequestBody CommonDtos.CreatePortfolioItem request) {
        return ResponseEntity.ok(portfolio.create(me, request));
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<CommonDtos.PortfolioItemResponse> update(@AuthenticationPrincipal User me,
                                                                   @PathVariable Long itemId,
                                                                   @Valid @RequestBody CommonDtos.CreatePortfolioItem request) {
        return ResponseEntity.ok(portfolio.update(me, itemId, request));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<CommonDtos.OkResponse> delete(@AuthenticationPrincipal User me, @PathVariable Long itemId) {
        portfolio.delete(me, itemId);
        return ResponseEntity.ok(new CommonDtos.OkResponse(true, "Portfolio entry deleted."));
    }
}
