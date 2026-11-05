-- ============================================================
--  SkillBridge seed data (all passwords: skillbridge1)
--  Run with:  mysql -uroot skillbridge < seed.sql
-- ============================================================
USE skillbridge;

-- ---- users (admin + 6 students) ----
INSERT INTO users (full_name, email, password_hash, role, status) VALUES
('SkillBridge Admin','admin@skillbridge.edu','$2y$10$8Bk3BMnY2DcGeRwl5nEpZ.pHTIN7/rvtktXSXIoWd3yTkmt5Tw0uC','ADMIN','ACTIVE'),
('Aarav Mehta','aarav@skillbridge.edu','$2y$10$8Bk3BMnY2DcGeRwl5nEpZ.pHTIN7/rvtktXSXIoWd3yTkmt5Tw0uC','STUDENT','ACTIVE'),
('Riya Sharma','riya@skillbridge.edu','$2y$10$8Bk3BMnY2DcGeRwl5nEpZ.pHTIN7/rvtktXSXIoWd3yTkmt5Tw0uC','STUDENT','ACTIVE'),
('Karthik Iyer','karthik@skillbridge.edu','$2y$10$8Bk3BMnY2DcGeRwl5nEpZ.pHTIN7/rvtktXSXIoWd3yTkmt5Tw0uC','STUDENT','ACTIVE'),
('Priya Nair','priya@skillbridge.edu','$2y$10$8Bk3BMnY2DcGeRwl5nEpZ.pHTIN7/rvtktXSXIoWd3yTkmt5Tw0uC','STUDENT','ACTIVE'),
('Sahil Verma','sahil@skillbridge.edu','$2y$10$8Bk3BMnY2DcGeRwl5nEpZ.pHTIN7/rvtktXSXIoWd3yTkmt5Tw0uC','STUDENT','SUSPENDED'),
('Meera Joshi','meera@skillbridge.edu','$2y$10$8Bk3BMnY2DcGeRwl5nEpZ.pHTIN7/rvtktXSXIoWd3yTkmt5Tw0uC','STUDENT','ACTIVE');

SET @admin=(SELECT id FROM users WHERE email='admin@skillbridge.edu');
SET @aarav=(SELECT id FROM users WHERE email='aarav@skillbridge.edu');
SET @riya=(SELECT id FROM users WHERE email='riya@skillbridge.edu');
SET @karthik=(SELECT id FROM users WHERE email='karthik@skillbridge.edu');
SET @priya=(SELECT id FROM users WHERE email='priya@skillbridge.edu');
SET @sahil=(SELECT id FROM users WHERE email='sahil@skillbridge.edu');
SET @meera=(SELECT id FROM users WHERE email='meera@skillbridge.edu');

-- ---- profiles (U1/F2) ----
INSERT INTO student_profiles (user_id, college, course, year_of_study, bio, interests, hours_per_week, visibility, github_url, portfolio_url) VALUES
(@aarav,'SRM University','B.Tech CSE',2,'Full-stack learner who loves shipping hackathon projects.','Web development,ML,Hackathons','SIX_10','PUBLIC','https://github.com/aarav77','https://aarav.dev'),
(@riya,'SRM University','B.Tech CSE',2,'UI/UX designer who sketches before she codes.','UI design,Figma,Branding','ELEVEN_20','PUBLIC',NULL,'https://riya.design'),
(@karthik,'SRM University','B.Tech CSE',3,'Backend guy handling our database questions.','Spring Boot,Databases,APIs','SIX_10','PUBLIC','https://github.com/karthik-backend',NULL),
(@priya,'SRM University','B.Tech IT',2,'Data enthusiast exploring analytics and ML.','Data science,Python,Visualization','ONE_5','PUBLIC',NULL,NULL),
(@sahil,'SRM University','B.Tech AI&DS',3,'Wants to learn mobile app development.','Android,Flutter,ML','SIX_10','PRIVATE',NULL,NULL),
(@meera,'SRM University','B.Tech CSE',1,'First-year student looking for hands-on projects.','Learning,Basics,Floorball','ELEVEN_20','PUBLIC','https://github.com/meera23',NULL);

-- ---- skill categories + skills (admin managed) ----
INSERT INTO skill_categories (id, name, description) VALUES
(1,'Programming Languages','Languages for coding projects'),
(2,'Web & Mobile','Frontend, backend and app frameworks'),
(3,'Data & AI','Data science, ML and analytics'),
(4,'Design & Media','UI/UX, graphics and branding');

INSERT INTO skills (id, category_id, name, description) VALUES
(1,1,'Java','Core Java and OOP'),
(2,1,'Python','Python scripting and libraries'),
(3,2,'React','React web frontend'),
(4,2,'Spring Boot','Java REST API framework'),
(5,2,'Flutter','Cross-platform mobile apps'),
(6,2,'HTML/CSS','Page structure and styling'),
(7,3,'Machine Learning','Models and training basics'),
(8,3,'SQL','Relational databases'),
(9,4,'UI/UX Design','Wireframes and interfaces'),
(10,4,'Figma','Design tooling');

-- ---- user skills ----
INSERT INTO user_skills (user_id, skill_id, level) VALUES
(@aarav,1,'INTERMEDIATE'),(@aarav,3,'INTERMEDIATE'),(@aarav,6,'ADVANCED'),(@aarav,8,'BEGINNER'),
(@riya,9,'ADVANCED'),(@riya,10,'EXPERT'),(@riya,6,'INTERMEDIATE'),
(@karthik,1,'EXPERT'),(@karthik,4,'ADVANCED'),(@karthik,8,'ADVANCED'),
(@priya,2,'INTERMEDIATE'),(@priya,7,'BEGINNER'),(@priya,8,'INTERMEDIATE'),
(@sahil,5,'BEGINNER'),(@sahil,2,'BEGINNER'),
(@meera,1,'BEGINNER'),(@meera,6,'BEGINNER');

-- ---- projects (U3/F4) ----
INSERT INTO projects (id, owner_id, title, description, goals, category, status, team_size_max, start_date, target_deadline) VALUES
(1,@aarav,'Campus Event Hub','A web portal to discover and register for campus events.','Ship a working portal with event listings, registration and an admin dashboard before tech fest.','Web Development','PUBLISHED',5,'2026-11-02','2026-12-20'),
(2,@karthik,'SkillSwap API','Backend REST API powering skill exchange between students.','Design clean REST endpoints with MySQL persistence and role-based access.','Backend','PUBLISHED',4,'2026-11-05','2027-01-15'),
(3,@meera,'Portfolio Site Sprint','Learning project where juniors build their first portfolio site.','Each member publishes a personal portfolio page after guided sessions.','Learning','PUBLISHED',6,'2026-11-10','2026-12-15'),
(4,@priya,'Fest Analytics Dash','Dashboard analysing fest attendance and feedback data.','Publish trend charts that help clubs plan better.','Data & AI','DRAFT',5,NULL,'2027-02-01');

-- required skills
INSERT INTO project_skills (project_id, skill_id, required_level) VALUES
(1,3,'INTERMEDIATE'),(1,6,'INTERMEDIATE'),(1,1,'BEGINNER'),
(2,4,'ADVANCED'),(2,8,'INTERMEDIATE'),(2,1,'INTERMEDIATE'),
(3,6,'BEGINNER'),(3,1,'BEGINNER'),(3,9,'BEGINNER'),
(4,2,'INTERMEDIATE'),(4,7,'BEGINNER');

-- open roles
INSERT INTO project_roles (project_id, title, slots) VALUES
(1,'Frontend Developer',2),(1,'Designer',1),
(2,'Backend Developer',2),(2,'Database Helper',1),
(3,'Participant',5);

INSERT INTO project_members (project_id, user_id, role, project_role_id, joined_at) VALUES
(1,@aarav,'OWNER',(SELECT id FROM project_roles WHERE project_id=1 AND title='Frontend Developer' LIMIT 1),NOW()),
(1,@riya,'MEMBER',(SELECT id FROM project_roles WHERE project_id=1 AND title='Designer' LIMIT 1),NOW()),
(2,@karthik,'OWNER',(SELECT id FROM project_roles WHERE project_id=2 AND title='Backend Developer' LIMIT 1),NOW()),
(2,@aarav,'MEMBER',(SELECT id FROM project_roles WHERE project_id=2 AND title='Backend Developer' LIMIT 1),NOW()),
(3,@meera,'OWNER',(SELECT id FROM project_roles WHERE project_id=3 AND title='Participant' LIMIT 1),NOW());

-- join requests & invitations (U3/F6)
INSERT INTO join_requests (project_id, requester_id, type, message, status, decided_by, decided_at) VALUES
(1,@karthik,'JOIN_REQUEST','I can build the registration API and admin panel.','PENDING',NULL,NULL),
(1,@priya,'JOIN_REQUEST','I would love to add analytics charts here.','PENDING',NULL,NULL),
(3,@priya,'INVITATION','Meera invited you to boost the design of the group portfolio.','ACCEPTED',@meera,NOW()),
(2,@sahil,'JOIN_REQUEST','I want to learn backend while helping you.','PENDING',NULL,NULL),
(3,@riya,'INVITATION','Partner to mentor juniors during the sprints.','PENDING',NULL,NULL);

-- ---- milestones (U5/F9) ----
INSERT INTO milestones (project_id, title, description, target_date, status) VALUES
(1,'M1: Wireframes & setup','Design pages and repo setup','2026-11-08','COMPLETED'),
(1,'M2: Event module','Event CRUD, listing and registration flow','2026-11-25','ACTIVE'),
(1,'M3: Admin dashboard','Club admin panel and analytics','2026-12-10','PLANNED'),
(2,'M1: Schema & auth','Database schema plus token auth','2026-11-18','ACTIVE'),
(3,'M1: Templates','Shared page templates ready','2026-11-20','PLANNED');

-- ---- tasks (U4/F7-F8) with mixed statuses ----
INSERT INTO tasks (id, project_id, milestone_id, created_by, assignee_id, title, description, acceptance_criteria, priority, due_date, status, depends_on_id, blocker_note, completed_at) VALUES
(1,1,(SELECT id FROM milestones WHERE project_id=1 AND status='COMPLETED'),@aarav,@riya,'Design wireframes for event pages','Home, listing, detail and registration screens.','Figma link shared + all four frames approved','HIGH','2026-11-05','APPROVED',NULL,NULL,NOW()),
(2,1,(SELECT id FROM milestones WHERE project_id=1 AND status='ACTIVE'),@aarav,@aarav,'Build event listing API','Endpoints for event CRUD and filters.','All endpoints tested with Postman','HIGH','2026-11-14','IN_PROGRESS',1,NULL,NULL),
(3,1,(SELECT id FROM milestones WHERE project_id=1 AND status='ACTIVE'),@aarav,@riya,'Registration form UI','Patient-friendly form with validation and success state.','Form validates empty input','MEDIUM','2026-11-10','READY_FOR_REVIEW',2,NULL,NULL),
(4,1,(SELECT id FROM milestones WHERE project_id=1 AND status='ACTIVE'),@aarav,@riya,'Event detail card component','Responsive card with banner and countdown.','Looks good on mobile','MEDIUM','2026-11-22','REOPENED',3,NULL,NULL),
(5,2,(SELECT id FROM milestones WHERE project_id=2 AND status='ACTIVE'),@karthik,@karthik,'Define database schema','Tables for users, skills, projects and tasks.','schema.sql runs clean on MySQL','HIGH','2026-11-06','APPROVED',NULL,NULL,NOW()),
(6,2,(SELECT id FROM milestones WHERE project_id=2 AND status='ACTIVE'),@karthik,@aarav,'Token auth endpoints','Login and register with BCrypt hashing.','Invalid login returns 401','HIGH','2026-11-12','IN_PROGRESS',5,'Waiting on Karthik to review token expiry rules',NULL),
(7,2,(SELECT id FROM milestones WHERE project_id=2 AND status='ACTIVE'),@karthik,@karthik,'Project search filtering','Filter projects by category and required skill.','Filters page results under 2 sec','LOW','2026-11-25','TODO',6,NULL,NULL),
(8,3,(SELECT id FROM milestones WHERE project_id=3 AND status='PLANNED'),@meera,NULL,'Collect template ideas','Shortlist 3 portfolio templates from the community.','List posted in the project chat','LOW','2026-11-15','TODO',NULL,NULL,NULL),
(9,3,(SELECT id FROM milestones WHERE project_id=3 AND status='PLANNED'),@meera,@priya,'Splash illustration','Warm illustration for the template header.','Image approved by owner','MEDIUM','2026-11-12','BLOCKED',NULL,'Waiting for free hours this week',NULL);

-- review trail
INSERT INTO task_reviews (task_id, reviewer_id, decision, comment) VALUES
(1,@aarav,'APPROVED','Great wireframes, ship them.'),
(3,@aarav,'CHANGES_REQUESTED','Add inline validation states please.'),
(5,@karthik,'APPROVED','Reviewed my own schema with a checklist.');

-- task comments
INSERT INTO task_comments (task_id, author_id, body) VALUES
(2,@karthik,'Use DTO validation to keep controllers lean.'),
(3,@riya,'Pushed the first draft, would love an API contract.'),
(4,@aarav,'Banner image needs a smaller version for mobile.');

-- project chat
INSERT INTO project_messages (project_id, sender_id, body) VALUES
(1,@aarav,'Welcome! Kick-off meeting tonight at 9 pm in this thread.'),
(1,@riya,'Wireframes are up - first four frames are in the draft link.'),
(1,@karthik,'I would take the registration API if that is fair.'),
(2,@karthik,'Schema approved. Next: auth endpoints. Assign yourself freely.'),
(2,@aarav,'I am on the login filters after my fest task.'),
(3,@meera,'Sprint begins Monday. Bring your template ideas!');

INSERT INTO direct_messages (sender_id, recipient_id, body) VALUES
(@karthik,@aarav,'Ping me when you finish the fest API, I will review the auth code.'),
(@priya,@meera,'Happy to design the splash art this weekend if you need it.');

-- ---- mentorship offers (U8/F13) ----
INSERT INTO mentorship_offers (id, owner_id, skill_id, type, title, description, max_participants, status) VALUES
(1,@karthik,4,'OFFER','Spring Boot basics 1:1','Three sessions covering REST controllers, JPA and validation. Bring a laptop.',2,'IN_PROGRESS'),
(2,@riya,10,'OFFER','Figma crash course','From frames to clickable prototypes in two evenings.',3,'OPEN'),
(3,@aarav,8,'REQUEST','Need help with SQL joins','Stuck on multi-table joins for my fest portal. Can swap React hours.',1,'OPEN'),
(4,@priya,7,'OFFER','ML basics study circle','Weekly circle walking through regression and classification.',4,'OPEN');

INSERT INTO mentorship_requests (offer_id, requester_id, message, status, decided_at) VALUES
(1,@meera,'I am a beginner but eager - Saturdays work for me.','ACCEPTED',NOW()),
(1,@sahil,'Can I join the second slot?','PENDING' ,NULL),
(2,@meera,'Sign me up please!','PENDING',NULL),
(3,@karthik,'Sure, Sunday evening 7 pm? Joins are easy after one whiteboard pass.','ACCEPTED',NOW());

INSERT INTO feedbacks (offer_id, from_user_id, to_user_id, rating, comment) VALUES
(1,@karthik,@meera,5,'Fast learner - by session two she had controllers running.'),
(1,@meera,@karthik,5,'Patient mentor with great analogies.');

-- ---- portfolio (U8/F14) ----
INSERT INTO portfolio_items (user_id, project_id, title, description, tag, visibility) VALUES
(@aarav,1,'Campus Event Hub','Building the event portal: React UI and event APIs.','PROJECT','PUBLIC'),
(@riya,NULL,'Top 10 in design jam','Designathon finalist with the college branding brief.','ACHIEVEMENT','PUBLIC'),
(@karthik,NULL,'MySQL performance certificate','Finished the database tuning short course.','CERTIFICATION','PUBLIC'),
(@meera,3,'My first portfolio','Template built during the sprint.','PROJECT','PRIVATE');

-- ---- notifications demo (U7/F12) ----
INSERT INTO notifications (user_id, type, title, body, link, is_read) VALUES
(@aarav,'TASK_ASSIGNED','You were assigned "Token auth endpoints"','Karthik Iyer assigned you a task in SkillSwap API.','/tasks',0),
(@karthik,'JOIN_REQUEST','New join request on Campus Event Hub','Karthik Iyer wants to join Campus Event Hub.','/requests',0),
(@riya,'TASK_SUBMITTED','Work ready for review','Registration form UI is ready for your review.','/tasks',0),
(@priya,'INVITE_RECEIVED','You were invited to a project','Meera Joshi invited you to Portfolio Site Sprint.','/requests',1),
(@meera,'MENTORSHIP_ACCEPTED','Your mentorship request was accepted','Spring Boot basics 1:1 - Sundays work.','/mentorship',0);

-- ---- admin report + audit sample (U9/F15) ----
INSERT INTO reports (reporter_id, reported_user_id, content_type, content_id, reason, description, status) VALUES
(@riya,@sahil,'USER',NULL,'Suspicious messages','Reported for spam DMs outside project scope.','PENDING');

INSERT INTO admin_audit_log (admin_id, action, entity_type, entity_id, reason) VALUES
(@admin,'SUSPEND_USER','USER',NULL,'Repeated spam DM reports pending review'),
(@admin,'ADD_SKILL','SKILL',10,'Requested by students in feedback form');
