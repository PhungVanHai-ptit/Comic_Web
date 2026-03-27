package com.haiphung.comic_web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/register")
    public String register() {
        return "redirect:/auth/register";
    }

    @GetMapping("/login")
    public String login() {
        return "redirect:/auth/login";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "redirect:/auth/forgot-password";
    }
}
