package com.luxuryhotel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*Phần đặt phòng trên nền web:
 Người dùng chọn địa điểm muốn đến và khoảng thời gian họ muốn đặt phòng, hệ thống sẽ liệt kê ra
tất cả các khách sạn ở đó cùng với danh sách các loại phòng còn trống (bao gồm thông tin về giá, diện
tích phòng và cho biết phòng có các tiện ích khác hay không như tivi, mini-bar, bàn làm việc, máy điều
hòa) và các hình ảnh, comment của những người đã được ở nơi này. Lưu ý tại 1 địa điểm nào đó, Luxury
Hotel có nhiều khách sạn với các tên khác nhau.
 Người dùng có thể đăng kí đặt phòng từ kết quả tìm kiếm ở trên. Sau khi đặt phòng xong, hệ thống sẽ
xuất ra giấy xác nhận đặt phòng cùng với mã số đặt phòng.*/
@SpringBootApplication
public class LuxuryHotelApplication {

    public static void main(String[] args) {
        SpringApplication.run(LuxuryHotelApplication.class, args);
    }
}
