package spring.mine.dictionary.controller;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import spring.mine.common.controller.BaseController;
import spring.mine.common.form.BaseForm;
import spring.mine.common.validator.BaseErrors;
import spring.mine.dictionary.form.DictionaryForm;
import spring.mine.dictionary.validator.DictionaryFormValidator;
import spring.mine.internationalization.MessageUtil;
import spring.service.dictionary.DictionaryService;
import spring.service.dictionarycategory.DictionaryCategoryService;
import us.mn.state.health.lims.common.exception.LIMSDuplicateRecordException;
import us.mn.state.health.lims.common.exception.LIMSFrozenRecordException;
import us.mn.state.health.lims.common.exception.LIMSRuntimeException;
import us.mn.state.health.lims.common.log.LogEvent;
import us.mn.state.health.lims.common.util.StringUtil;
import us.mn.state.health.lims.common.util.SystemConfiguration;
import us.mn.state.health.lims.common.util.validator.GenericValidator;
import us.mn.state.health.lims.dictionary.valueholder.Dictionary;
import us.mn.state.health.lims.dictionarycategory.valueholder.DictionaryCategory;
import us.mn.state.health.lims.login.valueholder.UserSessionData;

@Controller
@SessionAttributes("form")
public class DictionaryController extends BaseController {

	@Autowired
	DictionaryFormValidator formValidator;
	@Autowired
	DictionaryService dictionaryService;
	@Autowired
	DictionaryCategoryService dictionaryCategoryService;

	@ModelAttribute("form")
	public DictionaryForm form() {
		return new DictionaryForm();
	}

	@RequestMapping(value = "/Dictionary", method = RequestMethod.GET)
	public ModelAndView showDictionary(HttpServletRequest request, @ModelAttribute("form") BaseForm form)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		form = resetFormToType(form, DictionaryForm.class);
		form.setCancelAction("CancelDictionary.do");

		String id = request.getParameter(ID);

		setDefaultButtonAttributes(request);

		Dictionary dictionary = new Dictionary();
		dictionary.setId(id);

		if ((id != null) && (!"0".equals(id))) {
			// this is an existing dictionary
			dictionary = dictionaryService.get(id);

			if (dictionaryService.hasNext(id)) {
				request.setAttribute(NEXT_DISABLED, "false");
			}

			if (dictionaryService.hasPrevious(id)) {
				request.setAttribute(PREVIOUS_DISABLED, "false");
			}

		} else { // this is a new dictionary
			dictionary.setIsActive(YES);
		}

		if (dictionary.getId() != null && !dictionary.getId().equals("0")) {
			request.setAttribute(ID, dictionary.getId());
			// bugzilla 2062 initialize selectedDictionaryCategoryId
			if (dictionary.getDictionaryCategory() != null) {
				dictionary.setSelectedDictionaryCategoryId(dictionary.getDictionaryCategory().getId());
			}
		}

		// populate form from valueholder
		PropertyUtils.copyProperties(form, dictionary);

		List<DictionaryCategory> dictCats = dictionaryCategoryService.getAll();

		PropertyUtils.setProperty(form, "categories", dictCats);

		return findForward(FWD_SUCCESS, form);
	}

	private void setDefaultButtonAttributes(HttpServletRequest request) {
		request.setAttribute(ALLOW_EDITS_KEY, "true");
		// bugzilla 2062
		request.setAttribute(RECORD_FROZEN_EDIT_DISABLED_KEY, "false");
		request.setAttribute(PREVIOUS_DISABLED, "true");
		request.setAttribute(NEXT_DISABLED, "true");
	}

	@RequestMapping(value = "/NextPreviousDictionary", method = RequestMethod.GET)
	public ModelAndView showNextPreviousDictionary(HttpServletRequest request) {
		String id = request.getParameter(ID);
		String direction = request.getParameter("direction");

		String nextPrevId = null;
		if (FWD_NEXT.equals(direction)) {
			nextPrevId = dictionaryService.getNext(id).getId();
		} else if (FWD_PREVIOUS.equals(direction)) {
			nextPrevId = dictionaryService.getPrevious(id).getId();
		}
		if (GenericValidator.isBlankOrNull(nextPrevId)) {
			Errors errors = new BaseErrors();
			errors.reject("dictionary.nextprev.error");
			saveErrors(errors);
			return new ModelAndView(findForward(FWD_FAIL));
		}

		String url = "redirect:/Dictionary.do?ID=" + nextPrevId;
		return new ModelAndView(url);
	}

	@RequestMapping(value = "/Dictionary", method = RequestMethod.POST)
	public ModelAndView showUpdateDictionary(HttpServletRequest request,
			@ModelAttribute("form") @Valid DictionaryForm form, BindingResult result, SessionStatus status,
			RedirectAttributes redirectAttributes)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

		formValidator.validate(form, result);
		if (result.hasErrors()) {
			saveErrors(result);
			return findForward(FWD_FAIL_INSERT, form);
		}

		setDefaultButtonAttributes(request);

		Dictionary dictionary = setupDictionary(form);

		try {
			String id = form.getId();
			if (!(id == null || "0".equals(id))) {
				// UPDATE
				// bugzilla 2062
				boolean isDictionaryFrozenCheckRequired = checkForDictionaryFrozenCheck(form);
				dictionaryService.update(dictionary, isDictionaryFrozenCheckRequired);
			} else {
				// INSERT
				dictionaryService.insert(dictionary);
			}
		} catch (LIMSRuntimeException lre) {
			// bugzilla 2154
			LogEvent.logError("DictionaryUpdateAction", "performAction()", lre.toString());
			// 1482
			if (lre.getException() instanceof org.hibernate.StaleObjectStateException) {
				result.reject("errors.OptimisticLockException");
			} else if (lre.getException() instanceof LIMSDuplicateRecordException) {
				String messageKey = "dictionary.dictEntryByCategory";
				String msg = MessageUtil.getMessage(messageKey);
				result.reject("errors.DuplicateRecord.activate", new String[] { msg },
						"errors.DuplicateRecord.activate");
			} else if (lre.getException() instanceof LIMSFrozenRecordException) {
				String messageKey = "dictionary.dictEntry";
				String msg = MessageUtil.getMessage(messageKey);
				result.reject("errors.FrozenRecord", new String[] { msg }, "errors.FrozenRecord");
				// Now disallow further edits RECORD_FROZEN_EDIT_DISABLED_KEY
				// in this case User needs to Exit and come back to refresh form
				// for further updates (this is to restore isDirty() functionality
				// that relies on defaultValues of form
				// --this is needed to determine whether frozen check is required
				request.setAttribute(RECORD_FROZEN_EDIT_DISABLED_KEY, "true");
			} else {
				result.reject("errors.UpdateException");
			}

			saveErrors(result);
			// bugzilla 1485: allow change and try updating again (enable save button)
			// disable previous and next
			request.setAttribute(PREVIOUS_DISABLED, "true");
			request.setAttribute(NEXT_DISABLED, "true");
			return findForward(FWD_FAIL_INSERT, form);

		}

		status.setComplete();
		redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
		return findForward(FWD_SUCCESS_INSERT, form);
	}

	private Dictionary setupDictionary(DictionaryForm form)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Dictionary dictionary;
		if (form.getId() != null && !form.getId().equals("0")) {
			dictionary = dictionaryService.get(form.getId());
		} else {
			dictionary = new Dictionary();
		}
		// get sysUserId from login module
		UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
		String sysUserId = String.valueOf(usd.getSystemUserId());
		dictionary.setSysUserId(sysUserId);

		// populate valueholder from form
		PropertyUtils.copyProperties(dictionary, form);

		String selectedCategoryId = (String) form.get("selectedDictionaryCategoryId");
		// bugzilla 2108
		DictionaryCategory dictionaryCategory = dictionaryCategoryService.get(selectedCategoryId);
		dictionary.setDictionaryCategory(dictionaryCategory);
		return dictionary;

	}

	private boolean checkForDictionaryFrozenCheck(DictionaryForm form) {
		boolean isDictionaryFrozenCheckRequired = false;
		// there is an exception to rule of checking whether dictionary record
		// is frozen (can no longer be updated):
		// currenly if only isActive has changed and
		// the current value is 'Y'
		// OR
		// bugzilla 1847: also the local abbreviation can be deleted/updated/inserted at
		// anytime
		String dirtyFormFields = form.getString("dirtyFormFields");
		String isActiveValue = form.getString("isActive");

		String[] dirtyFields = dirtyFormFields.split(SystemConfiguration.getInstance().getDefaultIdSeparator(), -1);
		List<String> listOfDirtyFields = new ArrayList<>();

		for (int i = 0; i < dirtyFields.length; i++) {
			String dirtyField = dirtyFields[i];
			if (!StringUtil.isNullorNill(dirtyField)) {
				listOfDirtyFields.add(dirtyField);
			}
		}

		List<String> listOfDirtyFieldsNoFrozenCheckRequired = new ArrayList<>();
		listOfDirtyFieldsNoFrozenCheckRequired.add("isActive");
		listOfDirtyFieldsNoFrozenCheckRequired.add("localAbbreviation");

		// bugzilla 1847 : added to exception for frozen check required
		// isActive changed to Y (no frozen check required)
		// localAbbreviation has changed (no frozen check required)
		if (!listOfDirtyFields.isEmpty()) {
			for (int i = 0; i < listOfDirtyFields.size(); i++) {
				String dirtyField = listOfDirtyFields.get(i);
				if (!listOfDirtyFieldsNoFrozenCheckRequired.contains(dirtyField)) {
					isDictionaryFrozenCheckRequired = true;
				} else {
					// in case of isActive: need to make sure it changed to YES to be able
					// to skip isFrozenCheck
					if (dirtyField.equals("isActive") && !isActiveValue.equals(YES)) {
						isDictionaryFrozenCheckRequired = true;
					}
				}
			}
		}
		return isDictionaryFrozenCheckRequired;
	}

	@RequestMapping(value = "/CancelDictionary", method = RequestMethod.GET)
	public ModelAndView cancelDictionary(HttpServletRequest request, @ModelAttribute("form") DictionaryForm form,
			SessionStatus status) {
		status.setComplete();
		return findForward(FWD_CANCEL, form);
	}

	@Override
	protected String findLocalForward(String forward) {
		if (FWD_SUCCESS.equals(forward)) {
			return "dictionaryDefinition";
		} else if (FWD_FAIL.equals(forward)) {
			return "redirect:/DictionaryMenu.do";
		} else if (FWD_SUCCESS_INSERT.equals(forward)) {
			return "redirect:/DictionaryMenu.do";
		} else if (FWD_FAIL_INSERT.equals(forward)) {
			return "dictionaryDefinition";
		} else if (FWD_CANCEL.equals(forward)) {
			return "redirect:/DictionaryMenu.do";
		} else {

			return "PageNotFound";
		}
	}

	@Override
	protected String getPageTitleKey() {
		return "dictionary.edit.title";
	}

	@Override
	protected String getPageSubtitleKey() {
		return "dictionary.edit.title";
	}
}