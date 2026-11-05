package com.skillbridge.backend.repository.spec;

import com.skillbridge.backend.entity.Project;
import com.skillbridge.backend.entity.ProjectMember;
import com.skillbridge.backend.entity.ProjectSkill;
import com.skillbridge.backend.entity.User;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/** Dynamic filters for U2 / F3 project discovery. */
public final class ProjectSpecs {

    public static Specification<Project> filter(String q, String category, Long skillId,
                                                Project.Status status, User requester,
                                                boolean mine, boolean ownedByMe) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("status").in(Project.Status.DRAFT, Project.Status.CANCELLED)));

            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase() + "%";
                Predicate title = cb.like(cb.lower(root.get("title")), like);
                Predicate description = cb.like(cb.lower(cb.coalesce(root.get("description"), "")), like);
                predicates.add(cb.or(title, description));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.toLowerCase()));
            }
            if (skillId != null) {
                Subquery<Integer> sub = query.subquery(Integer.class);
                Root<ProjectSkill> psRoot = sub.from(ProjectSkill.class);
                sub.select(cb.literal(1));
                sub.where(cb.equal(psRoot.get("project").get("id"), root.get("id")),
                        cb.equal(psRoot.get("skill").get("id"), skillId));
                predicates.add(cb.exists(sub));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (mine && requester != null) {
                Subquery<Integer> sub = query.subquery(Integer.class);
                Root<ProjectMember> pmRoot = sub.from(ProjectMember.class);
                sub.select(cb.literal(1));
                sub.where(cb.equal(pmRoot.get("project").get("id"), root.get("id")),
                        cb.equal(pmRoot.get("user").get("id"), requester.getId()));
                predicates.add(cb.exists(sub));
            }
            if (ownedByMe && requester != null) {
                predicates.add(cb.equal(root.get("owner").get("id"), requester.getId()));
            }
            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
