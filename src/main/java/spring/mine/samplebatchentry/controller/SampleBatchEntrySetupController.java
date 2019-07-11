package spring.mine.samplebatchentry.controller;

import java.lang.reflect.InvocationTargetException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import spring.mine.sample.controller.BaseSampleEntryController;
import spring.mine.samplebatchentry.form.SampleBatchEntryForm;
import spring.service.siteinformation.SiteInformationService;
import us.mn.state.health.lims.common.formfields.FormFields;
import us.mn.state.health.lims.common.services.DisplayListService;
import us.mn.state.health.lims.common.services.DisplayListService.ListType;
import us.mn.state.health.lims.common.services.SampleOrderService;
import us.mn.state.health.lims.common.util.DateUtil;
import us.mn.state.health.lims.sample.form.ProjectData;

@Controller
public class SampleBatchEntrySetupController extends BaseSampleEntryController {

	@Autowired
	SiteInformationService siteInformationService;

	@RequestMapping(value = "/SampleBatchEntrySetup", method = RequestMethod.GET)
	public ModelAndView showSampleBatchEntrySetup(HttpServletRequest request)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		SampleBatchEntryForm form = new SampleBatchEntryForm();

		request.getSession().setAttribute(NEXT_DISABLED, TRUE);

		SampleOrderService sampleOrderService = new SampleOrderService();
		form.setSampleOrderItems(sampleOrderService.getSampleOrderItem());
		form.setSampleTypes(DisplayListService.getInstance().getList(ListType.SAMPLE_TYPE_ACTIVE));
		form.setTestSectionList(DisplayListService.getInstance().getList(ListType.TEST_SECTION));
		form.setCurrentDate(DateUtil.getCurrentDateAsText());
		form.setCurrentTime(DateUtil.getCurrentTimeAsText());
		form.getSampleOrderItems().setReceivedTime(DateUtil.getCurrentTimeAsText());
		form.getSampleOrderItems().setReceivedDateForDisplay(DateUtil.getCurrentDateAsText());
		form.setProjectDataVL(new ProjectData());
		form.setProjectDataEID(new ProjectData());

		String siteInfo = siteInformationService.getSiteInformationByName("Study Management tab").getValue();
		request.getSession().setAttribute("siteInfo", siteInfo);

		addProjectList(form);

		if (FormFields.getInstance().useField(FormFields.Field.InitialSampleCondition)) {
			form.setInitialSampleConditionList(DisplayListService.getInstance().getList(ListType.INITIAL_SAMPLE_CONDITION));
		}

		return findForward(FWD_SUCCESS, form);
	}

	@Override
	protected String findLocalForward(String forward) {
		if (FWD_SUCCESS.equals(forward)) {
			return "sampleBatchEntrySetupDefinition";
		} else if (FWD_FAIL.equals(forward)) {
			return "homePageDefinition";
		} else {
			return "PageNotFound";
		}
	}

	@Override
	protected String getPageTitleKey() {
		return "sample.batchentry.setup.title";
	}

	@Override
	protected String getPageSubtitleKey() {
		return "sample.batchentry.setup.title";
	}
}