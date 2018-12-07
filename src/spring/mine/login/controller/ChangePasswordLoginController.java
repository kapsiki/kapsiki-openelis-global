package spring.mine.login.controller;

import java.lang.String;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import spring.mine.common.controller.BaseController;
import spring.mine.common.form.BaseForm;
import spring.mine.common.validator.BaseErrors;
import spring.mine.login.form.LoginChangePasswordForm;

@Controller
public class ChangePasswordLoginController extends BaseController {
	
	@RequestMapping(value = "/ChangePasswordLogin", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView showChangePasswordLogin(HttpServletRequest request) {
		String forward = FWD_SUCCESS;
		LoginChangePasswordForm form = new LoginChangePasswordForm();
		form.setFormName("loginChangePasswordForm");
		form.setFormAction("UpdateLoginChangePassword.do");
		BaseErrors errors = new BaseErrors();
		
		form.setPassword("");
		form.setNewPassword("");
		form.setConfirmPassword("");
		
		return findForward(forward, form);
	}

	protected ModelAndView findLocalForward(String forward, BaseForm form) {
		if ("success".equals(forward)) {
			return new ModelAndView("loginChangePasswordDefinition", "form", form);
		} else if ("fail".equals(forward)) {
			return new ModelAndView("loginChangePasswordDefinition", "form", form);
		} else {
			return new ModelAndView("PageNotFound");
		}
	}

	protected String getPageTitleKey() {
		return "login.changePass";
	}

	protected String getPageSubtitleKey() {
		return "login.changePass";
	}
}
