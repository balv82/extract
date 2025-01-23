package ch.asit_asso.extract.web.controllers;

import ch.asit_asso.extract.services.AdminUserBuilder;
import ch.asit_asso.extract.services.AppInitializationService;
import ch.asit_asso.extract.services.UserService;
import ch.asit_asso.extract.web.model.SetupModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import java.util.HashMap;
import java.util.Map;
import javax.validation.Valid;

@Controller
public class StatusController {
    @GetMapping("/status")
    public Map<String, String> getStatus() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        return response;
    }
}
