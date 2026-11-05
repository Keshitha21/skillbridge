package com.skillbridge.backend.service;

import com.skillbridge.backend.dto.CommonDtos;
import com.skillbridge.backend.entity.*;
import com.skillbridge.backend.exception.BadRequestException;
import com.skillbridge.backend.exception.ForbiddenException;
import com.skillbridge.backend.exception.ResourceNotFoundException;
import com.skillbridge.backend.repository.FeedbackRepository;
import com.skillbridge.backend.repository.MentorshipFileRepository;
import com.skillbridge.backend.repository.MentorshipOfferRepository;
import com.skillbridge.backend.repository.MentorshipRequestRepository;
import com.skillbridge.backend.repository.MentorshipTaskRepository;
import com.skillbridge.backend.repository.SkillRepository;
import com.skillbridge.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** U8 / F13: peer-learning offers & requests, feedback after completion, and the
 *  mentorship workspace — teaching files & learning-plan tasks for participants. */
@Service
public class MentorshipService {

    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "csv",
            "png", "jpg", "jpeg", "gif", "webp", "zip");

    private final MentorshipOfferRepository offers;
    private final MentorshipRequestRepository mentorshipRequests;
    private final MentorshipTaskRepository mentorTasks;
    private final MentorshipFileRepository mentorFiles;
    private final FeedbackRepository feedbackRepo;
    private final SkillRepository skills;
    private final UserRepository users;
    private final NotificationService notifier;
    private final Path uploadDir;
    private final long maxBytes;

    public MentorshipService(MentorshipOfferRepository offers, MentorshipRequestRepository mentorshipRequests,
                             FeedbackRepository feedbackRepo, SkillRepository skills,
                             UserRepository users, NotificationService notifier,
                             MentorshipTaskRepository mentorTasks, MentorshipFileRepository mentorFiles,
                             @Value("${skillbridge.upload-dir:./uploads}") String uploadDirValue,
                             @Value("${skillbridge.max-file-size:10485760}") long maxBytes) {
        this.offers = offers;
        this.mentorshipRequests = mentorshipRequests;
        this.mentorTasks = mentorTasks;
        this.mentorFiles = mentorFiles;
        this.feedbackRepo = feedbackRepo;
        this.skills = skills;
        this.users = users;
        this.notifier = notifier;
        this.uploadDir = Paths.get(uploadDirValue).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
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

    // ---------- mentorship workspace: participants only (owner + accepted requesters) ----------

    private MentorshipOffer loadOffer(Long offerId) {
        return offers.findById(offerId).orElseThrow(() -> new ResourceNotFoundException("Learning offer not found."));
    }

    /** Owner + every accepted learner/mentor of the offer — the workspace audience. */
    private List<User> participants(MentorshipOffer offer) {
        List<User> out = new ArrayList<>();
        out.add(offer.getOwner());
        mentorshipRequests.findByOfferId(offer.getId()).stream()
                .filter(mr -> mr.getStatus() == MentorshipRequest.Status.ACCEPTED)
                .forEach(mr -> out.add(mr.getRequester()));
        return out;
    }

    private void requireParticipant(User me, MentorshipOffer offer) {
        boolean ok = offer.getOwner().getId().equals(me.getId()) || me.getRole() == User.Role.ADMIN
                || mentorshipRequests.findByOfferId(offer.getId()).stream().anyMatch(mr ->
                        mr.getStatus() == MentorshipRequest.Status.ACCEPTED
                        && mr.getRequester().getId().equals(me.getId()));
        if (!ok) throw new ForbiddenException("Only participants of this learning activity can use its workspace.");
    }

    private User requireAssignee(User candidate, MentorshipOffer offer) {
        if (candidate == null || candidate.getId().equals(offer.getOwner().getId())) return candidate;
        return mentorshipRequests.findByOfferId(offer.getId()).stream()
                .anyMatch(mr -> mr.getStatus() == MentorshipRequest.Status.ACCEPTED
                        && mr.getRequester().getId().equals(candidate.getId()))
                ? candidate : null;
    }

    // ----- teaching files -----

    @Transactional(readOnly = true)
    public List<CommonDtos.MentorshipFileView> listFiles(User me, Long offerId) {
        MentorshipOffer offer = loadOffer(offerId);
        requireParticipant(me, offer);
        return mentorFiles.findByOfferIdOrderByCreatedAtDesc(offer.getId()).stream()
                .map(this::toFileResponse).toList();
    }

    @Transactional
    public CommonDtos.MentorshipFileView uploadFile(User me, Long offerId, MultipartFile multipart) {
        MentorshipOffer offer = loadOffer(offerId);
        requireParticipant(me, offer);
        if (multipart == null || multipart.isEmpty()) {
            throw new BadRequestException("Choose a file to upload.");
        }
        if (multipart.getSize() > maxBytes) {
            throw new BadRequestException("This file exceeds the supported size limit of "
                    + (maxBytes / (1024 * 1024)) + " MB.");
        }
        String original = multipart.getOriginalFilename() == null ? "file" : multipart.getOriginalFilename();
        String extension = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
        if (original.lastIndexOf('.') < 0 || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Unsupported file type: ." + extension
                    + ". Allowed: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException ex) {
            throw new BadRequestException("The upload folder is not accessible.");
        }
        String storedName = UUID.randomUUID() + "." + extension;
        try {
            multipart.transferTo(uploadDir.resolve(storedName).toFile());
        } catch (IOException ex) {
            throw new BadRequestException("The upload could not be saved. Please try again.");
        }
        MentorshipFile saved = mentorFiles.save(MentorshipFile.builder()
                .offer(offer)
                .uploader(me)
                .originalName(original)
                .storedName(storedName)
                .mimeType(multipart.getContentType())
                .sizeBytes(multipart.getSize())
                .build());

        String byline = offer.getOwner().getId().equals(me.getId()) ? "The mentor" : me.getFullName();
        participants(offer).stream()
                .filter(p -> !p.getId().equals(me.getId()))
                .forEach(p -> notifier.notify(p, NotificationService.Type.MENTORSHIP_FILE_SHARED,
                        "Teaching file shared",
                        byline + " uploaded \"" + original + "\" in \"" + offer.getTitle() + "\".",
                        "/mentorship"));
        return toFileResponse(saved);
    }

    @Transactional(readOnly = true)
    public Path downloadPath(User me, Long fileId) {
        MentorshipFile file = mentorFiles.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found."));
        requireParticipant(me, file.getOffer());
        return uploadDir.resolve(file.getStoredName()).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public MentorshipFile fileMeta(Long fileId) {
        return mentorFiles.findById(fileId).orElseThrow(() -> new ResourceNotFoundException("File not found."));
    }

    @Transactional
    public void deleteFile(User me, Long fileId) {
        MentorshipFile file = mentorFiles.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found."));
        if (!file.getUploader().getId().equals(me.getId())
                && !file.getOffer().getOwner().getId().equals(me.getId())
                && me.getRole() != User.Role.ADMIN) {
            throw new ForbiddenException("Only the uploader or the offer owner can remove this file.");
        }
        try {
            Files.deleteIfExists(uploadDir.resolve(file.getStoredName()));
        } catch (IOException ignored) { /* row is authoritative; disk cleanup is best effort */ }
        mentorFiles.delete(file);
    }

    // ----- learning-plan tasks -----

    @Transactional(readOnly = true)
    public List<CommonDtos.MentorshipTaskView> listTasks(Long offerId) {
        MentorshipOffer offer = loadOffer(offerId);
        return mentorTasks.findByOfferIdOrderByCreatedAtAsc(offer.getId()).stream()
                .map(this::toTaskResponse).toList();
    }

    @Transactional
    public CommonDtos.MentorshipTaskView createTask(User me, Long offerId, CommonDtos.CreateMentorshipTask request) {
        MentorshipOffer offer = loadOffer(offerId);
        requireParticipant(me, offer);
        User assignee = null;
        if (request.assigneeId() != null) {
            User candidate = users.findById(request.assigneeId())
                    .orElseThrow(() -> new BadRequestException("Assignee not found."));
            assignee = requireAssignee(candidate, offer);
            if (assignee == null) throw new BadRequestException("Assignee must be a participant of this activity.");
        }
        MentorshipTask saved = mentorTasks.save(MentorshipTask.builder()
                .offer(offer)
                .title(request.title().trim())
                .assignee(assignee)
                .createdBy(me)
                .build());
        if (assignee != null && !assignee.getId().equals(me.getId())) {
            notifier.notify(assignee, NotificationService.Type.MENTORSHIP_TASK_ASSIGNED,
                    "New learning task assigned to you",
                    me.getFullName() + " added \"" + saved.getTitle() + "\" in \"" + offer.getTitle() + "\".",
                    "/mentorship");
        }
        return toTaskResponse(saved);
    }

    @Transactional
    public CommonDtos.MentorshipTaskView updateTaskStatus(User me, Long taskId, MentorshipTask.Status next) {
        MentorshipTask task = mentorTasks.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning task not found."));
        if (next == null) throw new BadRequestException("Provide the new task status.");
        requireParticipant(me, task.getOffer());
        boolean isAssignedParticipant = task.getAssignee() != null && task.getAssignee().getId().equals(me.getId());
        boolean isOwner = task.getOffer().getOwner().getId().equals(me.getId())
                || me.getRole() == User.Role.ADMIN;
        boolean sharedUnassignedTask = task.getAssignee() == null;
        if (!isAssignedParticipant && !isOwner && !sharedUnassignedTask) {
            throw new ForbiddenException("Only the assigned participant or the offer owner can update this task.");
        }
        task.setStatus(next);
        mentorTasks.save(task);
        participants(task.getOffer()).stream()
                .filter(p -> !p.getId().equals(me.getId()))
                .forEach(p -> notifier.notify(p, NotificationService.Type.MENTORSHIP_TASK_UPDATED,
                        "Learning task updated",
                        me.getFullName() + " moved \"" + task.getTitle() + "\" to "
                                + next.name().replace('_', ' ').toLowerCase()
                                + " in \"" + task.getOffer().getTitle() + "\".",
                        "/mentorship"));
        return toTaskResponse(task);
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
                mr.getOffer().getType(),
                mr.getRequester().getId(), mr.getRequester().getFullName(), mr.getMessage(),
                mr.getStatus(), mr.getCreatedAt());
    }

    private CommonDtos.FeedbackResponse toFeedbackResponse(Feedback f) {
        return new CommonDtos.FeedbackResponse(f.getId(), f.getOffer().getId(), f.getFromUser().getId(),
                f.getFromUser().getFullName(), f.getToUser().getId(), f.getToUser().getFullName(),
                f.getRating(), f.getComment(), f.getCreatedAt());
    }

    private CommonDtos.MentorshipTaskView toTaskResponse(MentorshipTask t) {
        return new CommonDtos.MentorshipTaskView(t.getId(), t.getOffer().getId(), t.getTitle(), t.getStatus(),
                t.getAssignee() == null ? null : t.getAssignee().getId(),
                t.getAssignee() == null ? null : t.getAssignee().getFullName(),
                t.getCreatedBy().getId(), t.getCreatedBy().getFullName(),
                t.getCreatedAt(), t.getUpdatedAt());
    }

    private CommonDtos.MentorshipFileView toFileResponse(MentorshipFile f) {
        return new CommonDtos.MentorshipFileView(f.getId(), f.getOffer().getId(), f.getUploader().getId(),
                f.getUploader().getFullName(), f.getOriginalName(), f.getMimeType(),
                f.getSizeBytes(), f.getCreatedAt());
    }
}
