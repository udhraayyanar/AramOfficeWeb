-- ============================================================
--  Aram Office Document Portal - MySQL / XAMPP database
--  Import in phpMyAdmin:  http://localhost/phpmyadmin  ->  Import  ->  choose this file  ->  Go
--  (or:  C:\xampp\mysql\bin\mysql -u root < aramoffice.sql)
--  Safe to import more than once: existing rows are kept.
-- ============================================================

CREATE DATABASE IF NOT EXISTS `aramoffice`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `aramoffice`;

-- Staff accounts (hash = SHA-256 of salt + password, same as the Java code)
CREATE TABLE IF NOT EXISTS `users` (
  `username`  VARCHAR(50)  COLLATE utf8mb4_bin NOT NULL,
  `salt`      VARCHAR(64)  NOT NULL,
  `hash`      VARCHAR(64)  NOT NULL,
  `full_name` VARCHAR(100) NOT NULL,
  PRIMARY KEY (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- One row per government document
CREATE TABLE IF NOT EXISTS `documents` (
  `id`         VARCHAR(50)  COLLATE utf8mb4_bin NOT NULL,
  `department` VARCHAR(100) NOT NULL,
  `doc_type`   VARCHAR(50)  NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Full version history of every document
CREATE TABLE IF NOT EXISTS `versions` (
  `doc_id`     VARCHAR(50)  COLLATE utf8mb4_bin NOT NULL,
  `version_no` INT          NOT NULL,
  `title`      VARCHAR(500) NOT NULL,
  `category`   VARCHAR(50)  NOT NULL,
  `importance` INT          NOT NULL,
  `keywords`   TEXT         NOT NULL,
  PRIMARY KEY (`doc_id`, `version_no`),
  CONSTRAINT `fk_versions_doc` FOREIGN KEY (`doc_id`) REFERENCES `documents` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Demo staff logins:  admin / admin123   and   staff / staff123
INSERT IGNORE INTO `users` (`username`, `salt`, `hash`, `full_name`) VALUES
('admin', 'a1b2c3d4-0001-4aram-office-admin00001', '5aea1f3edc610056a2142302b4f08676ca205c941a280bb0099693d8fe1b116c', 'Office Admin'),
('staff', 'a1b2c3d4-0002-4aram-office-staff00002', 'f2812ab99fc3eca81eb592372003e5a3962af5e4b97a07fadb7b912ac1644ba5', 'Office Staff'),
('udhra', 'a1b2c3d4-0003-4aram-office-udhra00003', 'c9f8e7d6c5b4a3f2e1d0c9b8a7f6e5d4c3b2a1f0e9d8c7b6a5f4e3d2c1b0a9f8', 'Udhra Office');

-- 48 synthetic documents (4 departments x 6 categories x 2 documents)
INSERT IGNORE INTO `documents` (`id`, `department`, `doc_type`) VALUES
('ADM-APP-039', 'Administration', 'ApplicationForm'),
('ADM-APP-040', 'Administration', 'ApplicationForm'),
('ADM-CIR-037', 'Administration', 'Circular'),
('ADM-CIR-038', 'Administration', 'Circular'),
('ADM-GUI-045', 'Administration', 'Document'),
('ADM-GUI-046', 'Administration', 'Document'),
('ADM-NOT-043', 'Administration', 'Document'),
('ADM-NOT-044', 'Administration', 'Document'),
('ADM-REP-047', 'Administration', 'Document'),
('ADM-REP-048', 'Administration', 'Document'),
('ADM-SER-041', 'Administration', 'Document'),
('ADM-SER-042', 'Administration', 'Document'),
('HEA-APP-027', 'Health', 'ApplicationForm'),
('HEA-APP-028', 'Health', 'ApplicationForm'),
('HEA-CIR-025', 'Health', 'Circular'),
('HEA-CIR-026', 'Health', 'Circular'),
('HEA-GUI-033', 'Health', 'Document'),
('HEA-GUI-034', 'Health', 'Document'),
('HEA-NOT-031', 'Health', 'Document'),
('HEA-NOT-032', 'Health', 'Document'),
('HEA-REP-035', 'Health', 'Document'),
('HEA-REP-036', 'Health', 'Document'),
('HEA-SER-029', 'Health', 'Document'),
('HEA-SER-030', 'Health', 'Document'),
('PUB-APP-015', 'PublicWorks', 'ApplicationForm'),
('PUB-APP-016', 'PublicWorks', 'ApplicationForm'),
('PUB-CIR-013', 'PublicWorks', 'Circular'),
('PUB-CIR-014', 'PublicWorks', 'Circular'),
('PUB-GUI-021', 'PublicWorks', 'Document'),
('PUB-GUI-022', 'PublicWorks', 'Document'),
('PUB-NOT-019', 'PublicWorks', 'Document'),
('PUB-NOT-020', 'PublicWorks', 'Document'),
('PUB-REP-023', 'PublicWorks', 'Document'),
('PUB-REP-024', 'PublicWorks', 'Document'),
('PUB-SER-017', 'PublicWorks', 'Document'),
('PUB-SER-018', 'PublicWorks', 'Document'),
('REV-APP-003', 'Revenue', 'ApplicationForm'),
('REV-APP-004', 'Revenue', 'ApplicationForm'),
('REV-CIR-001', 'Revenue', 'Circular'),
('REV-CIR-002', 'Revenue', 'Circular'),
('REV-GUI-009', 'Revenue', 'Document'),
('REV-GUI-010', 'Revenue', 'Document'),
('REV-NOT-007', 'Revenue', 'Document'),
('REV-NOT-008', 'Revenue', 'Document'),
('REV-REP-011', 'Revenue', 'Document'),
('REV-REP-012', 'Revenue', 'Document'),
('REV-SER-005', 'Revenue', 'Document'),
('REV-SER-006', 'Revenue', 'Document');

INSERT IGNORE INTO `versions` (`doc_id`, `version_no`, `title`, `category`, `importance`, `keywords`) VALUES
('ADM-APP-039', 1, 'ApplicationForm document for Administration office #1', 'ApplicationForm', 2, 'form,application,administration'),
('ADM-APP-040', 1, 'ApplicationForm document for Administration office #2', 'ApplicationForm', 1, 'application,submit,administration,form'),
('ADM-CIR-037', 1, 'Circular document for Administration office #1', 'Circular', 1, 'policy,notice,administration,circular'),
('ADM-CIR-038', 1, 'Circular document for Administration office #2', 'Circular', 5, 'policy,notice,administration,circular'),
('ADM-GUI-045', 1, 'Guideline document for Administration office #1', 'Guideline', 4, 'procedure,administration,guideline'),
('ADM-GUI-046', 1, 'Guideline document for Administration office #2', 'Guideline', 4, 'guideline,procedure,administration'),
('ADM-NOT-043', 1, 'Notification document for Administration office #1', 'Notification', 3, 'schedule,administration,notification'),
('ADM-NOT-044', 1, 'Notification document for Administration office #2', 'Notification', 3, 'deadline,administration,notification'),
('ADM-REP-047', 1, 'Report document for Administration office #1', 'Report', 3, 'review,administration,report'),
('ADM-REP-048', 1, 'Report document for Administration office #2', 'Report', 3, 'summary,review,administration,report'),
('ADM-SER-041', 1, 'ServiceInstruction document for Administration office #1', 'ServiceInstruction', 2, 'staff,administration,service'),
('ADM-SER-042', 1, 'ServiceInstruction document for Administration office #2', 'ServiceInstruction', 4, 'process,administration,service'),
('HEA-APP-027', 1, 'ApplicationForm document for Health office #1', 'ApplicationForm', 1, 'citizen,form,health'),
('HEA-APP-028', 1, 'ApplicationForm document for Health office #2', 'ApplicationForm', 5, 'citizen,request,health,form'),
('HEA-CIR-025', 1, 'Circular document for Health office #1', 'Circular', 4, 'office,update,health,circular'),
('HEA-CIR-026', 1, 'Circular document for Health office #2', 'Circular', 4, 'notice,circular,health'),
('HEA-GUI-033', 1, 'Guideline document for Health office #1', 'Guideline', 5, 'standard,health,guideline'),
('HEA-GUI-034', 1, 'Guideline document for Health office #2', 'Guideline', 4, 'guideline,health'),
('HEA-NOT-031', 1, 'Notification document for Health office #1', 'Notification', 3, 'alert,schedule,health,notification'),
('HEA-NOT-032', 1, 'Notification document for Health office #2', 'Notification', 1, 'public,schedule,health,notification'),
('HEA-REP-035', 1, 'Report document for Health office #1', 'Report', 3, 'annual,review,health,report'),
('HEA-REP-036', 1, 'Report document for Health office #2', 'Report', 2, 'annual,summary,health,report'),
('HEA-SER-029', 1, 'ServiceInstruction document for Health office #1', 'ServiceInstruction', 3, 'staff,manual,health,service'),
('HEA-SER-030', 1, 'ServiceInstruction document for Health office #2', 'ServiceInstruction', 5, 'staff,manual,health,service'),
('PUB-APP-015', 1, 'ApplicationForm document for PublicWorks office #1', 'ApplicationForm', 4, 'request,form,publicworks'),
('PUB-APP-016', 1, 'ApplicationForm document for PublicWorks office #2', 'ApplicationForm', 5, 'request,form,publicworks'),
('PUB-CIR-013', 1, 'Circular document for PublicWorks office #1', 'Circular', 3, 'update,publicworks,circular'),
('PUB-CIR-014', 1, 'Circular document for PublicWorks office #2', 'Circular', 5, 'office,circular,publicworks'),
('PUB-GUI-021', 1, 'Guideline document for PublicWorks office #1', 'Guideline', 3, 'rule,procedure,publicworks,guideline'),
('PUB-GUI-022', 1, 'Guideline document for PublicWorks office #2', 'Guideline', 2, 'rule,publicworks,guideline'),
('PUB-NOT-019', 1, 'Notification document for PublicWorks office #1', 'Notification', 1, 'notification,schedule,publicworks'),
('PUB-NOT-020', 1, 'Notification document for PublicWorks office #2', 'Notification', 5, 'alert,schedule,publicworks,notification'),
('PUB-REP-023', 1, 'Report document for PublicWorks office #1', 'Report', 4, 'annual,report,publicworks'),
('PUB-REP-024', 1, 'Report document for PublicWorks office #2', 'Report', 5, 'report,summary,publicworks'),
('PUB-SER-017', 1, 'ServiceInstruction document for PublicWorks office #1', 'ServiceInstruction', 3, 'staff,publicworks,service'),
('PUB-SER-018', 1, 'ServiceInstruction document for PublicWorks office #2', 'ServiceInstruction', 1, 'staff,instruction,publicworks,service'),
('REV-APP-003', 1, 'ApplicationForm document for Revenue office #1', 'ApplicationForm', 1, 'citizen,submit,revenue,form'),
('REV-APP-004', 1, 'ApplicationForm document for Revenue office #2', 'ApplicationForm', 4, 'request,revenue,form'),
('REV-CIR-001', 1, 'Circular document for Revenue office #1', 'Circular', 1, 'update,revenue,circular'),
('REV-CIR-002', 1, 'Circular document for Revenue office #2', 'Circular', 5, 'circular,revenue'),
('REV-GUI-009', 1, 'Guideline document for Revenue office #1', 'Guideline', 4, 'standard,guideline,revenue'),
('REV-GUI-010', 1, 'Guideline document for Revenue office #2', 'Guideline', 4, 'procedure,standard,revenue,guideline'),
('REV-NOT-007', 1, 'Notification document for Revenue office #1', 'Notification', 4, 'public,notification,revenue'),
('REV-NOT-008', 1, 'Notification document for Revenue office #2', 'Notification', 4, 'alert,schedule,revenue,notification'),
('REV-REP-011', 1, 'Report document for Revenue office #1', 'Report', 1, 'summary,report,revenue'),
('REV-REP-012', 1, 'Report document for Revenue office #2', 'Report', 1, 'audit,revenue,report'),
('REV-SER-005', 1, 'ServiceInstruction document for Revenue office #1', 'ServiceInstruction', 2, 'process,instruction,revenue,service'),
('REV-SER-006', 1, 'ServiceInstruction document for Revenue office #2', 'ServiceInstruction', 3, 'instruction,service,revenue');
