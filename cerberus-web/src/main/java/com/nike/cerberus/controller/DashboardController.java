package com.nike.cerberus.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
  @GetMapping(value = {"/", "/dashboard", "/dashboard/"})
  public String root() {
    return "redirect:/dashboard/index.html";
  }
}
