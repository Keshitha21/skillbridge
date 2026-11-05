-- ============================================================
--  SkillBridge — Student Skill Exchange & Project Collaboration
--  Local MySQL Community schema (all use cases: U1–U9 / F1–F15)
--  Run with:  mysql -uroot < schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS skillbridge
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE skillbridge;

-- ---------------- U1/F1: accounts ----------------
CREATE TABLE users (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  full_name     VARCHAR(120)  NOT NULL,
  email         VARCHAR(180)  NOT NULL UNIQUE,
  password_hash VARCHAR(120)  NOT NULL,
  role          ENUM('STUDENT','ADMIN') NOT NULL DEFAULT 'STUDENT',
  status        ENUM('ACTIVE','SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- simple auth token login sessions
CREATE TABLE auth_tokens (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT NOT NULL,
  token_hash  VARCHAR(64) NOT NULL UNIQUE,        -- sha-256 of the bearer token
  expires_at  TIMESTAMP NOT NULL,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- F1 password recovery
CREATE TABLE password_resets (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT NOT NULL,
  token_hash  VARCHAR(64) NOT NULL UNIQUE,
  used        TINYINT NOT NULL DEFAULT 0,
  expires_at  TIMESTAMP NOT NULL,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ---------------- U1/F2: student profiles ----------------
CREATE TABLE student_profiles (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id         BIGINT NOT NULL UNIQUE,
  college         VARCHAR(120),
  course          VARCHAR(120),
  year_of_study   INT,
  bio             TEXT,
  interests       VARCHAR(500),                      -- comma separated tags
  hours_per_week  ENUM('ONE_5','SIX_10','ELEVEN_20','TWENTY_PLUS') DEFAULT 'SIX_10',
  visibility      ENUM('PUBLIC','PRIVATE') NOT NULL DEFAULT 'PUBLIC',
  github_url      VARCHAR(300),
  linkedin_url    VARCHAR(300),
  portfolio_url   VARCHAR(300),
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ---------------- skills & categories (admin managed, F15) ----------------
CREATE TABLE skill_categories (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(80) NOT NULL UNIQUE,
  description VARCHAR(300)
);

CREATE TABLE skills (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  category_id BIGINT,
  name        VARCHAR(80) NOT NULL UNIQUE,
  description VARCHAR(300),
  CONSTRAINT fk_skill_category FOREIGN KEY (category_id) REFERENCES skill_categories(id)
);

CREATE TABLE user_skills (
  id       BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id  BIGINT NOT NULL,
  skill_id BIGINT NOT NULL,
  level    ENUM('BEGINNER','INTERMEDIATE','ADVANCED','EXPERT') NOT NULL DEFAULT 'BEGINNER',
  CONSTRAINT uk_user_skill UNIQUE (user_id, skill_id),
  CONSTRAINT fk_us_user  FOREIGN KEY (user_id)  REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_us_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
);

-- ---------------- U3/F4: projects ----------------
CREATE TABLE projects (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  owner_id        BIGINT NOT NULL,
  title           VARCHAR(150) NOT NULL,
  description     TEXT,
  goals           TEXT,
  category        VARCHAR(60),
  status          ENUM('DRAFT','PUBLISHED','PAUSED','COMPLETED','CANCELLED') NOT NULL DEFAULT 'DRAFT',
  team_size_max   INT NOT NULL DEFAULT 5,
  start_date      DATE,
  target_deadline DATE,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_project_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE TABLE project_skills (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  project_id     BIGINT NOT NULL,
  skill_id       BIGINT NOT NULL,
  required_level ENUM('BEGINNER','INTERMEDIATE','ADVANCED','EXPERT') NOT NULL DEFAULT 'BEGINNER',
  CONSTRAINT uk_proj_skill UNIQUE (project_id, skill_id),
  CONSTRAINT fk_ps_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
  CONSTRAINT fk_ps_skill   FOREIGN KEY (skill_id)   REFERENCES skills(id)
);

CREATE TABLE project_roles (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  project_id BIGINT NOT NULL,
  title      VARCHAR(60) NOT NULL,
  slots      INT NOT NULL DEFAULT 1,
  CONSTRAINT fk_pr_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE TABLE project_members (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  project_id BIGINT NOT NULL,
  user_id    BIGINT NOT NULL,
  role       ENUM('OWNER','MANAGER','MEMBER') NOT NULL DEFAULT 'MEMBER',
  project_role_id BIGINT,
  joined_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_member UNIQUE (project_id, user_id),
  CONSTRAINT fk_pm_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
  CONSTRAINT fk_pm_user    FOREIGN KEY (user_id)    REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_pm_prole   FOREIGN KEY (project_role_id) REFERENCES project_roles(id) ON DELETE SET NULL
);

-- U3/F6: join requests & invitations
CREATE TABLE join_requests (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  project_id    BIGINT NOT NULL,
  requester_id  BIGINT NOT NULL,
  type          ENUM('JOIN_REQUEST','INVITATION') NOT NULL,
  message       VARCHAR(500),
  status        ENUM('PENDING','ACCEPTED','REJECTED','CANCELLED') NOT NULL DEFAULT 'PENDING',
  decided_by    BIGINT,
  decided_at    TIMESTAMP NULL,
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_jr_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
  CONSTRAINT fk_jr_user FOREIGN KEY (requester_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ---------------- U4/U5: milestones, tasks, reviews ----------------
CREATE TABLE milestones (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  project_id  BIGINT NOT NULL,
  title       VARCHAR(120) NOT NULL,
  description VARCHAR(500),
  target_date DATE,
  status      ENUM('PLANNED','ACTIVE','COMPLETED') NOT NULL DEFAULT 'PLANNED',
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ms_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE TABLE tasks (
  id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
  project_id          BIGINT NOT NULL,
  milestone_id        BIGINT,
  created_by          BIGINT NOT NULL,
  assignee_id         BIGINT,
  title               VARCHAR(150) NOT NULL,
  description         TEXT,
  acceptance_criteria VARCHAR(600),
  priority            ENUM('LOW','MEDIUM','HIGH') NOT NULL DEFAULT 'MEDIUM',
  due_date            DATE,
  status              ENUM('TODO','IN_PROGRESS','BLOCKED','READY_FOR_REVIEW','APPROVED','REOPENED','CANCELLED')
                      NOT NULL DEFAULT 'TODO',
  depends_on_id       BIGINT,                          -- no circular chains (checked in service)
  blocker_note        VARCHAR(300),
  completed_at        TIMESTAMP NULL,
  created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_t_project  FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
  CONSTRAINT fk_t_milestone FOREIGN KEY (milestone_id) REFERENCES milestones(id) ON DELETE SET NULL,
  CONSTRAINT fk_t_assignee FOREIGN KEY (assignee_id) REFERENCES users(id),
  CONSTRAINT fk_t_depends  FOREIGN KEY (depends_on_id) REFERENCES tasks(id) ON DELETE SET NULL
);

CREATE TABLE task_reviews (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  task_id     BIGINT NOT NULL,
  reviewer_id BIGINT NOT NULL,
  decision    ENUM('APPROVED','CHANGES_REQUESTED') NOT NULL,
  comment     VARCHAR(600),
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_tr_task     FOREIGN KEY (task_id)     REFERENCES tasks(id) ON DELETE CASCADE,
  CONSTRAINT fk_tr_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id)
);

CREATE TABLE task_comments (
  id        BIGINT AUTO_INCREMENT PRIMARY KEY,
  task_id   BIGINT NOT NULL,
  author_id BIGINT NOT NULL,
  body      VARCHAR(1000) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_tc_task   FOREIGN KEY (task_id)   REFERENCES tasks(id) ON DELETE CASCADE,
  CONSTRAINT fk_tc_author FOREIGN KEY (author_id) REFERENCES users(id)
);

-- ---------------- U6/F10-F11: chat, DMs, files ----------------
CREATE TABLE project_messages (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  project_id BIGINT NOT NULL,
  sender_id  BIGINT NOT NULL,
  body       VARCHAR(1000) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_pmsg_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
  CONSTRAINT fk_pmsg_sender  FOREIGN KEY (sender_id)  REFERENCES users(id)
);

CREATE TABLE direct_messages (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  sender_id    BIGINT NOT NULL,
  recipient_id BIGINT NOT NULL,
  body         VARCHAR(1000) NOT NULL,
  read_at      TIMESTAMP NULL,
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_dm_sender    FOREIGN KEY (sender_id)    REFERENCES users(id),
  CONSTRAINT fk_dm_recipient FOREIGN KEY (recipient_id) REFERENCES users(id)
);

CREATE TABLE uploaded_files (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  project_id    BIGINT NOT NULL,
  uploader_id   BIGINT NOT NULL,
  task_id       BIGINT,
  original_name VARCHAR(255) NOT NULL,
  stored_name   VARCHAR(255) NOT NULL UNIQUE,
  mime_type     VARCHAR(120),
  size_bytes    BIGINT,
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_file_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
  CONSTRAINT fk_file_user    FOREIGN KEY (uploader_id) REFERENCES users(id),
  CONSTRAINT fk_file_task    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL
);

-- ---------------- U7/F12: notifications ----------------
CREATE TABLE notifications (
  id        BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id   BIGINT NOT NULL,
  type      VARCHAR(40) NOT NULL,
  title     VARCHAR(150) NOT NULL,
  body      VARCHAR(600),
  link      VARCHAR(300),
  is_read   TINYINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_notif_user_unread (user_id, is_read)
);

-- ---------------- U8/F13-F14: peer learning & portfolio ----------------
CREATE TABLE mentorship_offers (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  owner_id    BIGINT NOT NULL,
  skill_id    BIGINT,
  type        ENUM('OFFER','REQUEST') NOT NULL,
  title       VARCHAR(120) NOT NULL,
  description TEXT,
  max_participants INT NOT NULL DEFAULT 1,
  status      ENUM('OPEN','IN_PROGRESS','COMPLETED','CLOSED') NOT NULL DEFAULT 'OPEN',
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_mo_owner FOREIGN KEY (owner_id) REFERENCES users(id),
  CONSTRAINT fk_mo_skill FOREIGN KEY (skill_id) REFERENCES skills(id)
);

CREATE TABLE mentorship_requests (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  offer_id     BIGINT NOT NULL,
  requester_id BIGINT NOT NULL,
  message      VARCHAR(500),
  status       ENUM('PENDING','ACCEPTED','DECLINED','COMPLETED','CANCELLED') NOT NULL DEFAULT 'PENDING',
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  decided_at   TIMESTAMP NULL,
  CONSTRAINT fk_mr_offer FOREIGN KEY (offer_id) REFERENCES mentorship_offers(id) ON DELETE CASCADE,
  CONSTRAINT fk_mr_user  FOREIGN KEY (requester_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE feedbacks (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  offer_id    BIGINT NOT NULL,
  from_user_id BIGINT NOT NULL,
  to_user_id  BIGINT NOT NULL,
  rating      INT NOT NULL,
  comment     VARCHAR(600),
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_feedback UNIQUE (offer_id, from_user_id),
  CONSTRAINT fk_fb_offer FOREIGN KEY (offer_id) REFERENCES mentorship_offers(id) ON DELETE CASCADE,
  CONSTRAINT fk_fb_from  FOREIGN KEY (from_user_id) REFERENCES users(id),
  CONSTRAINT fk_fb_to    FOREIGN KEY (to_user_id) REFERENCES users(id)
);

CREATE TABLE portfolio_items (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT NOT NULL,
  project_id  BIGINT,
  title       VARCHAR(150) NOT NULL,
  description TEXT,
  tag         ENUM('PROJECT','ACHIEVEMENT','CERTIFICATION') NOT NULL DEFAULT 'PROJECT',
  link        VARCHAR(300),
  visibility  ENUM('PUBLIC','PRIVATE') NOT NULL DEFAULT 'PUBLIC',
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_pi_user    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_pi_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL
);

-- ---------------- U9/F15: reports & admin audit ----------------
CREATE TABLE reports (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  reporter_id     BIGINT NOT NULL,
  reported_user_id BIGINT,
  content_type    ENUM('USER','PROJECT','MESSAGE','FILE','OFFER','OTHER') NOT NULL,
  content_id      BIGINT,
  reason          VARCHAR(300) NOT NULL,
  description     VARCHAR(1000),
  status          ENUM('PENDING','RESOLVED','DISMISSED') NOT NULL DEFAULT 'PENDING',
  admin_note      VARCHAR(500),
  reviewed_by     BIGINT,
  reviewed_at     TIMESTAMP NULL,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_rp_reporter   FOREIGN KEY (reporter_id) REFERENCES users(id),
  CONSTRAINT fk_rp_reported   FOREIGN KEY (reported_user_id) REFERENCES users(id)
);

CREATE TABLE admin_audit_log (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  admin_id    BIGINT NOT NULL,
  action      VARCHAR(120) NOT NULL,
  entity_type VARCHAR(60) NOT NULL,
  entity_id   BIGINT,
  reason      VARCHAR(500),
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_aal_admin FOREIGN KEY (admin_id) REFERENCES users(id)
);
