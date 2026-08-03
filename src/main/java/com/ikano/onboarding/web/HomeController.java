package com.ikano.onboarding.web;

import com.ikano.onboarding.domain.Country;
import com.ikano.onboarding.domain.CustomerType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("countries", Country.values());
        model.addAttribute("customerTypes", CustomerType.values());
        return "index";
    }
}
