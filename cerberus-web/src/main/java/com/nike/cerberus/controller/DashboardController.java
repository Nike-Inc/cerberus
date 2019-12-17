package com.nike.cerberus.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class DashboardController {

  @RequestMapping(value = {"/", "/dashboard", "/dashboard/"})
  public String root() {
    return "redirect:/dashboard/index.html";
  }
}
