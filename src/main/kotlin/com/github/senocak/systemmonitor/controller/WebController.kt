package com.github.senocak.systemmonitor.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

/**
 * Controller for web endpoints.
 * This controller handles HTTP requests and serves the web UI.
 */
@Controller
class WebController {

    /**
     * Redirect the root URL to the index.html page.
     * @return redirect to index.html
     */
    @GetMapping("/")
    fun index(): String {
        return "redirect:/index.html"
    }
}