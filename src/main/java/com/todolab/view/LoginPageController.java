package com.todolab.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginPageController {

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("title", "ToDoLab 로그인");
        return "pages/auth/login";
    }
}
