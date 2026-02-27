package com.hutech.quizbackend.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String index(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return "Chào bạn, vui lòng đăng nhập tại <a href='/login'>đây</a>";
        }
        // Lấy tên của Mạnh từ tài khoản Google
        String name = principal.getAttribute("name");
        return "<h1>ĐĂNG NHẬP THÀNH CÔNG!</h1>" +
                "<h2>Chào mừng " + name + " đã đến với hệ thống Ôn luyện trắc nghiệm AI.</h2>" +
                "<p>Hệ thống bảo mật của chúng ta đã nhận diện được bạn.</p>";
    }
}