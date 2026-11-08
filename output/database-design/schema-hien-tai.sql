-- Cấu trúc ba bảng nghiệp vụ do Room sinh từ dự án phiên bản 7.

CREATE TABLE IF NOT EXISTS `medicines` (`medicineId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `frequency` TEXT, `startDate` INTEGER NOT NULL, `endDate` INTEGER NOT NULL, `notes` TEXT, `times` TEXT);

CREATE TABLE IF NOT EXISTS `alarms` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `medicine_id` INTEGER NOT NULL, `time` INTEGER NOT NULL, `enabled` INTEGER NOT NULL, `scheduled_time` TEXT, FOREIGN KEY(`medicine_id`) REFERENCES `medicines`(`medicineId`) ON UPDATE NO ACTION ON DELETE CASCADE );

CREATE INDEX IF NOT EXISTS `index_alarms_medicine_id` ON `alarms` (`medicine_id`);

CREATE TABLE IF NOT EXISTS `dose_records` (`medicineId` INTEGER NOT NULL, `date` TEXT NOT NULL, `scheduledTime` TEXT NOT NULL, `isTaken` INTEGER NOT NULL, `timeTaken` TEXT, PRIMARY KEY(`medicineId`, `date`, `scheduledTime`));
