package com.travelmate.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    @GetMapping({
            "/",
            "/login",
            "/destinations",
            "/destination/{slug}",
            "/about",
            "/terms",
            "/privacy",
            "/help",
            "/flight-search",
            "/train-search",
            "/hotel-search",
            "/hotel/{id}",
            "/attractions",
            "/ai-plan",
            "/community",
            "/post/create",
            "/post/{id}",
            "/my-orders",
            "/coupons",
            "/notifications",
            "/collections",
            "/profile/{username}",
            "/admin"
    })
    public String forwardFrontendRoutes() {
        return "forward:/index.html";
    }
}
