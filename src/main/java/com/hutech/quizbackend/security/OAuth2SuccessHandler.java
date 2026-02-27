package com.hutech.quizbackend.security;

import com.hutech.quizbackend.entity.User;
import com.hutech.quizbackend.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        // Chống SQL Injection bằng cách dùng Repository (PreparedStatements)
        userRepository.findByEmail(email).ifPresentOrElse(
                user -> { /* Đã tồn tại thì thôi */ },
                () -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setName(name);
                    newUser.setProvider("GOOGLE");
                    // Tự động set Admin cho Mạnh, người khác là USER
                    newUser.setRole(email.equals("manhksnb05@gmail.com") ? "ADMIN" : "USER");
                    userRepository.save(newUser);
                }
        );
        getRedirectStrategy().sendRedirect(request, response, "/");
    }
}