package spring.session.example.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;

/**
 * Created by hanwen on 15-7-31.
 */
@Controller
@RequestMapping("/home")
public class HomeController {

	@RequestMapping
	public ModelAndView index(HttpSession session) {
		return new ModelAndView("home/index", "se", session);
	}
}
