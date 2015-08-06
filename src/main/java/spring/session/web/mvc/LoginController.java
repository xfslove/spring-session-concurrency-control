package spring.session.web.mvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import spring.session.concurrent.ConfigDataProvider;
import spring.session.web.Account;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

/**
 * Created by hanwen on 15-7-31.
 */
@Controller
@RequestMapping("/")
public class LoginController {

	@Autowired
	private ConfigDataProvider configDataProvider;

	@RequestMapping
	public String index() {
		return "login/index";
	}

	@RequestMapping(value = "/login", method = RequestMethod.POST)
	public String login(HttpSession session, @Valid Account account) {
		configDataProvider.setPrincipalAttr("principal");
		session.setAttribute(configDataProvider.getPrincipalAttr(), account.getUsername());
		return "redirect:/home";
	}
}
