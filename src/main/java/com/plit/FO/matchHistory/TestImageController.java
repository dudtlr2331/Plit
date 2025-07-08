package com.plit.FO.matchHistory;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestImageController {

    @GetMapping("/test")
    public String testImage() {
        return "/fo/matchHistory/test";
    }
}
