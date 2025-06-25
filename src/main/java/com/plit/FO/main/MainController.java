package com.plit.FO.main;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/main") //기본 경로를 설정
public class MainController {

    // GET 요청을 처리하여 main.html을 반환하는 메서드
    @GetMapping
    public String mainPage() {
        return "fo/main/main";
    }

    @GetMapping("/home") // /main/home 다른 경로는 이렇게
    public String homePage() {
        return "main/main";
    }
}

