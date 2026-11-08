ẢNH CODE — USE CASE THEO DÕI LỊCH SỬ UỐNG THUỐC

Các ảnh được dựng từ code gốc trong dự án, không dùng AI sinh nội dung code.
Mỗi ảnh có tên file và số dòng nguồn. Dòng dài được xuống hàng để đọc; ký hiệu > là phần tiếp của cùng dòng.
Logic xem theo ngày hiện nằm trong HomeActivity. Các Activity lịch sử/chi tiết ở biểu đồ thiết kế trước chưa được triển khai thành lớp riêng.
isTaken=false biểu thị chưa được đánh dấu đã uống; không phải trường dữ liệu riêng xác nhận bỏ lỡ.

01-model-dose-record.png
  Mô hình lưu lịch sử uống thuốc
  app/src/main/java/com/example/medicationreminderapp/model/DoseRecord.java — dòng 1–26

02-dao-dose-record.png
  Lưu và truy vấn lịch sử theo ngày
  app/src/main/java/com/example/medicationreminderapp/database/DoseRecordDao.java — dòng 1–22

03-room-database.png
  Khai báo bảng lịch sử trong Room
  app/src/main/java/com/example/medicationreminderapp/database/AppDatabase.java — dòng 14–37

04-chon-ngay.png
  Chọn ngày để xem các liều đã ghi nhận
  app/src/main/java/com/example/medicationreminderapp/activity/HomeActivity.java — dòng 187–200

05-luu-xac-nhan-uong.png
  Xác nhận uống thuốc và hoàn tác
  app/src/main/java/com/example/medicationreminderapp/activity/HomeActivity.java — dòng 202–238

06-doc-lich-su-theo-ngay.png
  Đọc lịch sử và ghép trạng thái từng liều
  app/src/main/java/com/example/medicationreminderapp/activity/HomeActivity.java — dòng 264–305

07-hien-thi-trang-thai.png
  Hiển thị trạng thái và giờ đã uống
  app/src/main/java/com/example/medicationreminderapp/adapter/MedicineAdapter.java — dòng 42–72

08-tong-hop-va-trang-thai-rong.png
  Tổng số liều đã uống và trạng thái rỗng
  app/src/main/java/com/example/medicationreminderapp/activity/HomeActivity.java — dòng 427–470

09-luu-tu-chuong-bao.png
  Ghi nhận lần uống từ luồng chuông báo
  app/src/main/java/com/example/medicationreminderapp/service/AlarmService.java — dòng 174–198
