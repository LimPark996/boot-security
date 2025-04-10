package org.example.bootsecurity.controller;

import org.example.bootsecurity.model.domain.Memo;
import org.example.bootsecurity.model.mapper.MemoMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;

@Controller
public class MainController {
    private final MemoMapper memoMapper;

    public MainController(MemoMapper memoMapper) {
        this.memoMapper = memoMapper;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("message", "Hello World!");
        memoMapper.insert(new Memo(0L, "메모!", ""));
        model.addAttribute("memoList", memoMapper.findAll());
        return "index";
    }
}
