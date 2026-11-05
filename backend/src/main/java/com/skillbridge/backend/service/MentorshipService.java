package com.skillbridge.backend.service;

import com.skillbridge.backend.dto.CommonDtos;
import com.skillbridge.backend.entity.*;
import com.skillbridge.backend.exception.BadRequestException;
import com.skillbridge.backend.exception.ForbiddenException;
import com.skillbridge.backend.exception.ResourceNotFoundException;
import com.skillbridge.backend.repository.FeedbackRepository;
import com.skillbridge.backend.repository.MentorshipOfferRepository;
import com.skillbridge.backend.repository.MentorshipRequestRepository;
import com.skillbridge.backend.repository.SkillRepository;
import com.skillbridge.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** U8 / F13: peer-learning offers & requests, and feedback after completion. */
@Service
public class MentorshipService {

    private final MentorshipOfferRepository offers;
    private final MentorshipRequestRepository mentorshipRequests;
    private final FeedbackRepository feedbackRepo;
    private final SkillRepository skills;
    private final UserRepository users;
    private final NotificationService notifier;

    public MentorshipService(MentorshipOfferRepository offers, MentorshipRequestRepository mentorshipRequests,
                             FeedbackRepository feedbackRepo, SkillRepository skills,
                             UserRepository users, NotificationService notifier) {
        this.offers = offers;
        this.mentorshipRequests = mentorshipRequests;
        this.feedbackRepo = feedbackRepo;
        this.skills = skills;
        this.users = users;
        this.notifier = notifier;
    }

    // ---------- catalog ----------

    @Transactional(readOnly = true)
    public List<CommonDtos.OfferResponse> search(User requester, MentorshipOffer.Type type,
                                                 MentorshipOffer.Status status, Long skillId, String q) {
        List<MentorshipOffer> all = status != null
                ? offers.findAll().stream().filter(o -> o.getStatus() == status).toList()
                : offers.findAll();
        return all.stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .filter(o -> type == null || o.getType() == type)
                .filter(o -> skillId == null || (o.getSkill() != null && o.getSkill().getId().equals(skillId)))
                .filter(o -> q == null || q.isBlank()
                        || (o.getTitle() + " " + (o.getDescription() == null ? "" : o.getDescription())).toLowerCase()
                                .contains(q.toLowerCase()))
                .map(o -> toResponse(o))
                .toList();
    }

    @Transactional
    public CommonDtos.OfferResponse getOne(Long offerId) {
        return toResponse(offers.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning offer not found.")));
    }

    @Transactional
    public CommonDtos.OfferResponse create(User owner, CommonDtos.CreateOffer request) {
        if (request.type() == null) {
            throw new BadRequestException("Choose whether you are offering or requesting help.");
        }
        Skill skill = request.skillId() == null ? null : skills.findById(request.skillId())
                .orElseThrow(() -> new BadRequestException("Selected skill was not found."));
        MentorshipOffer saved = offers.save(MentorshipOffer.builder()
                .owner(owner)
                .skill(skill)
                .type(request.type())
                .title(request.title().trim())
                .description(request.description())
                .maxParticipants(request.maxParticipants() == null ? 1 : request.maxParticipants())
                .status(MentorshipOffer.Status.OPEN)
                .build());
        return toResponse(saved);
    }

    @Transactional
    public void close(User owner, Long offerId, MentorshipOffer.Status status) {
        MentorshipOffer offer = offers.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning offer not found."));
        if (!offer.getOwner().getId().equals(owner.getId()) && owner.getRole() != User.Role.ADMIN) {
            throw new ForbiddenException("Only the offer owner can close it.");
        }
        offer.setStatus(status == null ? MentorshipOffer.Status.CLOSED : status);
        offers.save(offer);
    }

    // ---------- learning requests ----------

    @Transactional
    public CommonDtos.MentorshipRequestResponse request(User requester, Long offerId, String message) {
        MentorshipOffer offer = offers.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning offer not found."));
        if (offer.getOwner().getId().equals(requester.getId())) {
            throw new BadRequestException("You own this learning activity.");
        }
        if (offer.getStatus() == MentorshipOffer.Status.COMPLETED || offer.getStatus() == MentorshipOffer.Status.CLOSED) {
            throw new BadRequestException("This learning activity is no longer taking requests.");
        }
        if (mentorshipRequests.findByOfferIdIn(List.of(offerId)).stream()
                .anyMatch(mr -> mr.getRequester().getId().equals(requester.getId())
                        && (mr.getStatus() == MentorshipRequest.Status.PENDING || mr.getStatus() == MentorshipRequest.Status.ACCEPTED))) {
            throw new BadRequestException("You already have an active request for this learning activity.");
        }
        MentorshipRequest saved = mentorshipRequests.save(MentorshipRequest.builder()
                .offer(offer)
                .requester(requester)
                .message(message)
                .status(MentorshipRequest.Status.PENDING)
                .build());
        notifier.notify(offer.getOwner(), NotificationService.Type.MENTORSHIP_REQUEST,
                "New learning " + (offer.getType() == MentorshipOffer.Type.OFFER ? "participant request" : "help request"),
                requester.getFullName() + " responded to \"" + offer.getTitle() + "\".",
                "/mentorship?requests=incoming");
        return toResponse(saved);
    }

    @Transactional
    public List<CommonDtos.MentorshipRequestResponse> myRequests(User me) {
        // outgoing + incoming (offers I own)
        List<CommonDtos.MentorshipRequestResponse> out = new ArrayList<>();
        mentorshipRequests.findByRequesterIdOrderByCreatedAtDesc(me.getId())
                .forEach(mr -> out.add(toResponse(mr)));
        mentorshipRequests.findByOffer_OwnerId(me.getId())
                .forEach(mr -> {
                    if (!out.stream().anyMatch(r2 -> r2.id().equals(mr.getId()))) out.add(toResponse(mr));
                    if (!out.contains(toResponse(mr))) {
                        out.add(toResponse(mr));
                    }
                });
        return out;
    }

    @Transactional
    public CommonDtos.MentorshipRequestResponse decide(User owner, Long requestId, boolean accept) {
        MentorshipRequest mr = mentorshipRequests.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning request not found."));
        if (!mr.getOffer().getOwner().getId().equals(owner.getId())) {
            throw new ForbiddenException("Only the offer owner can accept or decline.");
        }
        if (mr.getStatus() != MentorshipRequest.Status.PENDING) {
            throw new BadRequestException("This learning request was already handled.");
        }
        if (accept) {
            long active = mentorshipRequests.findByOfferId(mr.getOffer().getId()).stream()
                    .filter(other -> other.getStatus() == MentorshipRequest.Status.ACCEPTED)
                    .count();
            if (active >= mr.getOffer().getMaxParticipants()) {
                throw new BadRequestException("All learning spots are taken for this offer.");
            }
            mr.setStatus(MentorshipRequest.Status.ACCEPTED);
            mr.getOffer().setStatus(MentorshipOffer.Status.IN_PROGRESS);
            offers.save(mr.getOffer());
        } else {
            mr.setStatus(MentorshipRequest.Status.DECLINED);
        }
        mr.setDecidedAt(Instant.now());
        mentorshipRequests.save(mr);
        notifier.notify(mr.getRequester(), NotificationService.Type.MENTORSHIP_DECIDED,
                accept ? "Learning request accepted" : "Learning request declined",
                "\"" + mr.getOffer().getTitle() + "\" — " + (accept
                        ? owner.getFullName() + " accepted your request."
                        : owner.getFullName() + " was not able to accept this time."),
                "/mentorship");
        return toResponse(mr);
    }

    /** Participants mark the shared activity complete; the owner confirms. */
    @Transactional
    public CommonDtos.MentorshipRequestResponse complete(User participant, Long requestId) {
        MentorshipRequest mr = mentorshipRequests.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning request not found."));
        boolean isParticipant = mr.getRequester().getId().equals(participant.getId())
                || mr.getOffer().getOwner().getId().equals(participant.getId());
        if (!isParticipant) {
            throw new ForbiddenException("Only eligible participants can complete this activity.");
        }
        if (mr.getStatus() != MentorshipRequest.Status.ACCEPTED) {
            throw new BadRequestException("Only accepted learning activities can be completed.");
        }
        mr.setStatus(MentorshipRequest.Status.COMPLETED);
        mentorshipRequests.save(mr);
        User partner = mr.getRequester().getId().equals(participant.getId())
                ? mr.getOffer().getOwner() : mr.getRequester();
        notifier.notify(partner, NotificationService.Type.MENTORSHIP_COMPLETED, "Learning activity completed",
                participant.getFullName() + " marked \"" + mr.getOffer().getTitle() + "\" as completed. Leave feedback!",
                "/feedback?offer=" + mr.getOffer().getId());
        return toResponse(mr);
    }

    // ---------- feedback (only eligible participants, no duplicates) ----------

    @Transactional
    public CommonDtos.FeedbackResponse leaveFeedback(User author, Long offerId, int rating, String comment) {
        MentorshipOffer offer = offers.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning offer not found."));
        if (offer.getStatus() != MentorshipOffer.Status.COMPLETED
                && mentorshipRequests.findByOfferId(offerId).stream().noneMatch(
                        mr -> mr.getStatus() == MentorshipRequest.Status.COMPLETED)) {
            throw new BadRequestException("Feedback opens once the learning activity is completed.");
        }
        boolean eligible = mentorshipRequests.findByOfferId(offerId).stream().anyMatch(mr ->
                mr.getStatus() == MentorshipRequest.Status.COMPLETED
                        && (mr.getRequester().getId().equals(author.getId())
                            || mr.getOffer().getOwner().getId().equals(author.getId())))
                || offer.getOwner().getId().equals(author.getId());
        if (!eligible) {
            throw new ForbiddenException("Only eligible participants can provide feedback.");
        }
        if (feedbackRepo.existsByOfferIdAndFromUserId(offerId, author.getId())) {
            throw new BadRequestException("You already left feedback for this learning activity.");
        }
        boolean authorIsOwner = offer.getOwner().getId().equals(author.getId());
        User partner = authorIsOwner
                ? mentorshipRequests.findByOfferId(offerId).stream()
                        .filter(mr -> mr.getStatus() == MentorshipRequest.Status.COMPLETED)
                        .findFirst()
                        .map(MentorshipRequest::getRequester)
                        .orElseThrow(() -> new BadRequestException("No completed participant to review yet."))
                : offer.getOwner();

        Feedback saved = feedbackRepo.save(Feedback.builder()
                .offer(offer)
                .fromUser(author)
                .toUser(partner)
                .rating(rating)
                .comment(comment)
                .build());
        notifier.notify(partner, NotificationService.Type.FEEDBACK_RECEIVED, "New peer feedback received",
                author.getFullName() + " rated your session " + rating + "/5.",
                "/mentorship");
        return toFeedbackResponse(saved);
    }

    @Transactional
    public List<CommonDtos.FeedbackResponse> feedbackFor(Long userId) {
        return feedbackRepo.findByToUserId(userId).stream().map(this::toFeedbackResponse).toList();
    }

    @Transactional
    public List<CommonDtos.FeedbackResponse> feedbackForOffer(Long offerId) {
        return feedbackRepo.findByOfferId(offerId).stream().map(this::toFeedbackResponse).toList();
    }

    // ---------- mapping ----------

    private CommonDtos.OfferResponse toResponse(MentorshipOffer o) {
        int active = (int) mentorshipRequests.findByOfferId(o.getId()).stream()
                .filter(mr -> mr.getStatus() == MentorshipRequest.Status.ACCEPTED)
                .count();
        return new CommonDtos.OfferResponse(o.getId(), o.getOwner().getId(), o.getOwner().getFullName(),
                o.getSkill() == null ? null : o.getSkill().getId(),
                o.getSkill() == null ? null : o.getSkill().getName(),
                o.getType(), o.getTitle(), o.getDescription(), o.getMaxParticipants(), active,
                o.getStatus(), o.getCreatedAt());
    }

    private CommonDtos.MentorshipRequestResponse toResponse(MentorshipRequest mr) {
        return new CommonDtos.MentorshipRequestResponse(mr.getId(), mr.getOffer().getId(), mr.getOffer().getTitle(),
                mr.getOffer().getOwner().getFullName(), mr.getOffer().getOwner().getId(),
                mr.getRequester().getId(), mr.getRequester().getFullName(), mr.getMessage(),
                mr.getStatus(), mr.getCreatedAt());
    }

    private CommonDtos.FeedbackResponse toFeedbackResponse(Feedback f) {
        return new CommonDtos.FeedbackResponse(f.getId(), f.getOffer().getId(), f.getFromUser().getId(),
                f.getFromUser().getFullName(), f.getToUser().getId(), f.getToUser().getFullName(),
                f.getRating(), f.getComment(), f.getCreatedAt());
    }
}
