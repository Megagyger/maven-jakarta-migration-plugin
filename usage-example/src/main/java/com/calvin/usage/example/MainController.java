package com.calvin.usage.example;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/main")
public class MainController {

  @GetMapping
  @ResponseBody
  ResponseEntity<String> hello() {
    return ResponseEntity.ok("howdy do");
  }
}
