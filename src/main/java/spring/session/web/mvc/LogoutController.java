package spring.session.web.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;

/**
 * Created by hanwen on 15-7-31.
 */
@Controller
@RequestMapping("/logout")
public class LogoutController {

	@RequestMapping
	public String index(HttpSession session) {
		if (session != null) {
			session.invalidate();
		}
		return "redirect:/";
	}
}
