package spring.mine.qaevent.controller;

import java.lang.String;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.validator.GenericValidator;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import spring.mine.common.controller.BaseController;
import spring.mine.common.form.BaseForm;
import spring.mine.qaevent.form.NonConformityForm;
import spring.mine.common.validator.BaseErrors;
import us.mn.state.health.lims.common.exception.LIMSInvalidConfigurationException;
import us.mn.state.health.lims.common.formfields.FormFields;
import us.mn.state.health.lims.common.formfields.FormFields.Field;
import us.mn.state.health.lims.common.services.DisplayListService;
import us.mn.state.health.lims.common.services.NoteService;
import us.mn.state.health.lims.common.services.PatientService;
import us.mn.state.health.lims.common.services.PersonService;
import us.mn.state.health.lims.common.services.QAService;
import us.mn.state.health.lims.common.services.TableIdService;
import us.mn.state.health.lims.common.services.DisplayListService.ListType;
import us.mn.state.health.lims.common.services.QAService.QAObservationType;
import us.mn.state.health.lims.common.util.ConfigurationProperties;
import us.mn.state.health.lims.common.util.DateUtil;
import us.mn.state.health.lims.common.util.ConfigurationProperties.Property;
import us.mn.state.health.lims.note.valueholder.Note;
import us.mn.state.health.lims.observationhistory.dao.ObservationHistoryDAO;
import us.mn.state.health.lims.observationhistory.daoimpl.ObservationHistoryDAOImpl;
import us.mn.state.health.lims.observationhistory.valueholder.ObservationHistory;
import us.mn.state.health.lims.organization.dao.OrganizationDAO;
import us.mn.state.health.lims.organization.daoimpl.OrganizationDAOImpl;
import us.mn.state.health.lims.organization.valueholder.Organization;
import us.mn.state.health.lims.patient.util.PatientUtil;
import us.mn.state.health.lims.patient.valueholder.Patient;
import us.mn.state.health.lims.person.dao.PersonDAO;
import us.mn.state.health.lims.person.daoimpl.PersonDAOImpl;
import us.mn.state.health.lims.person.valueholder.Person;
import us.mn.state.health.lims.project.dao.ProjectDAO;
import us.mn.state.health.lims.project.daoimpl.ProjectDAOImpl;
import us.mn.state.health.lims.project.valueholder.Project;
import us.mn.state.health.lims.provider.dao.ProviderDAO;
import us.mn.state.health.lims.provider.daoimpl.ProviderDAOImpl;
import us.mn.state.health.lims.provider.valueholder.Provider;
import us.mn.state.health.lims.qaevent.valueholder.retroCI.QaEventItem;
import us.mn.state.health.lims.requester.dao.SampleRequesterDAO;
import us.mn.state.health.lims.requester.daoimpl.SampleRequesterDAOImpl;
import us.mn.state.health.lims.requester.valueholder.SampleRequester;
import us.mn.state.health.lims.sample.dao.SampleDAO;
import us.mn.state.health.lims.sample.daoimpl.SampleDAOImpl;
import us.mn.state.health.lims.sample.valueholder.Sample;
import us.mn.state.health.lims.samplehuman.dao.SampleHumanDAO;
import us.mn.state.health.lims.samplehuman.daoimpl.SampleHumanDAOImpl;
import us.mn.state.health.lims.samplehuman.valueholder.SampleHuman;
import us.mn.state.health.lims.sampleitem.dao.SampleItemDAO;
import us.mn.state.health.lims.sampleitem.daoimpl.SampleItemDAOImpl;
import us.mn.state.health.lims.sampleitem.valueholder.SampleItem;
import us.mn.state.health.lims.sampleproject.dao.SampleProjectDAO;
import us.mn.state.health.lims.sampleproject.daoimpl.SampleProjectDAOImpl;
import us.mn.state.health.lims.sampleproject.valueholder.SampleProject;
import us.mn.state.health.lims.sampleqaevent.dao.SampleQaEventDAO;
import us.mn.state.health.lims.sampleqaevent.daoimpl.SampleQaEventDAOImpl;
import us.mn.state.health.lims.sampleqaevent.valueholder.SampleQaEvent;
import us.mn.state.health.lims.test.daoimpl.TestSectionDAOImpl;
import us.mn.state.health.lims.test.valueholder.TestSection;
import us.mn.state.health.lims.typeofsample.dao.TypeOfSampleDAO;
import us.mn.state.health.lims.typeofsample.daoimpl.TypeOfSampleDAOImpl;
import us.mn.state.health.lims.typeofsample.valueholder.TypeOfSample;

@Controller
public class NonConformityController extends BaseController {

	private PatientService patientService;
	private List<ObservationHistory> observationHistoryList;
	private List<SampleQaEvent> sampleQAEventList;

	private static final String QA_NOTE_SUBJECT = "QaEvent Note";

	private static SampleDAO sampleDAO = new SampleDAOImpl();
	private static SampleItemDAO sampleItemDAO = new SampleItemDAOImpl();
	private static TypeOfSampleDAO typeOfSampleDAO = new TypeOfSampleDAOImpl();
	private static SampleHumanDAO sampleHumanDAO = new SampleHumanDAOImpl();
	private static PersonDAO personDAO = new PersonDAOImpl();
	private static ProviderDAO providerDAO = new ProviderDAOImpl();
	private static OrganizationDAO orgDAO = new OrganizationDAOImpl();

	private Boolean readOnly = Boolean.FALSE;
	private boolean sampleFound;
	private Sample sample;
	private boolean useSiteList;

	@RequestMapping(value = "/NonConformity", method = RequestMethod.GET)
	public ModelAndView showNonConformity(HttpServletRequest request, @ModelAttribute("form") NonConformityForm form)
			throws LIMSInvalidConfigurationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		String forward = FWD_SUCCESS;
		if (form == null) {
			form = new NonConformityForm();
		}
		form.setFormAction("");
		BaseErrors errors = new BaseErrors();
		if (getErrors() != null) {
			errors = (BaseErrors) getErrors();
		}
		ModelAndView mv = checkUserAndSetup(form, errors, request);

		useSiteList = FormFields.getInstance().useField(Field.NON_CONFORMITY_SITE_LIST);
		readOnly = Boolean.FALSE;
		request.getSession().setAttribute(SAVE_DISABLED, TRUE);

		String labNumber = request.getParameter("labNo");
		if (!GenericValidator.isBlankOrNull(labNumber)) {

			sample = getSampleForLabNumber(labNumber);

			sampleFound = !(sample == null || GenericValidator.isBlankOrNull(sample.getId()));

			PropertyUtils.setProperty(form, "labNo", labNumber);
			Date today = Calendar.getInstance().getTime();
			PropertyUtils.setProperty(form, "date", DateUtil.formatDateAsText(today));
			if (FormFields.getInstance().useField(Field.QATimeWithDate)) {
				PropertyUtils.setProperty(form, "time", DateUtil.nowTimeAsText());
			}

			if (sampleFound) {
				createForExistingSample(form);
			}

			setProjectList(form);

			PropertyUtils.setProperty(form, "sampleItemsTypeOfSampleIds", getSampleTypeOfSamplesString());
			PropertyUtils.setProperty(form, "sections", createSectionList());
			PropertyUtils.setProperty(form, "qaEventTypes", DisplayListService.getList(ListType.QA_EVENTS));
			PropertyUtils.setProperty(form, "qaEvents", getSampleQaEventItems(sample));

			PropertyUtils.setProperty(form, "typeOfSamples",
					DisplayListService.getList(ListType.SAMPLE_TYPE_ACTIVE));

			PropertyUtils.setProperty(form, "readOnly", readOnly);
			PropertyUtils.setProperty(form, "siteList",
					DisplayListService.getFreshList(ListType.SAMPLE_PATIENT_REFERRING_CLINIC));
			Provider provider = getProvider();
			if (provider != null) {
				PropertyUtils.setProperty(form, "providerNew", Boolean.FALSE.toString());
				Person providerPerson = getProviderPerson(provider);
				if (providerPerson != null && !providerPerson.getId().equals(PatientUtil.getUnknownPerson().getId())) {
					PersonService personService = new PersonService(providerPerson);
					PropertyUtils.setProperty(form, "providerFirstName", personService.getFirstName());
					PropertyUtils.setProperty(form, "providerLastName", personService.getLastName());
					PropertyUtils.setProperty(form, "providerWorkPhone", personService.getPhone());
					Map<String, String> addressComponents = personService.getAddressComponents();

					PropertyUtils.setProperty(form, "providerStreetAddress", addressComponents.get("Street"));
					PropertyUtils.setProperty(form, "providerCity", addressComponents.get("village"));
					PropertyUtils.setProperty(form, "providerCommune", addressComponents.get("commune"));
					PropertyUtils.setProperty(form, "providerDepartment", addressComponents.get("department"));
				}
			} else {
				PropertyUtils.setProperty(form, "providerNew", Boolean.TRUE.toString());
				PropertyUtils.setProperty(form, "requesterSampleID", "");
				PropertyUtils.setProperty(form, "providerFirstName", "");
				PropertyUtils.setProperty(form, "providerLastName", "");
				PropertyUtils.setProperty(form, "providerWorkPhone", "");
			}

			PropertyUtils.setProperty(form, "departments", DisplayListService.getList(ListType.HAITI_DEPARTMENTS));
		}

		if (errors.hasErrors()) {
			return mv;
		}

		return findForward(forward, form);
	}

	private void createForExistingSample(NonConformityForm form)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		getPatient(sample);
		getObservationHistory(sample);
		getSampleQaEvents(sample);
		PropertyUtils.setProperty(form, "sampleId", sample.getId());
		PropertyUtils.setProperty(form, "patientId", patientService.getPatientId());

		Project project = getProjectForSample(sample);
		if (project != null) {
			PropertyUtils.setProperty(form, "projectId", project.getId());
			PropertyUtils.setProperty(form, "project", project.getLocalizedName());
		}

		String subjectNo = patientService.getSubjectNumber();
		if (!GenericValidator.isBlankOrNull(subjectNo)) {
			PropertyUtils.setProperty(form, "subjectNew", Boolean.FALSE);
			PropertyUtils.setProperty(form, "subjectNo", subjectNo);
		}

		String STNo = patientService.getSTNumber();
		if (!GenericValidator.isBlankOrNull(STNo)) {
			PropertyUtils.setProperty(form, "newSTNumber", Boolean.FALSE);
			PropertyUtils.setProperty(form, "STNumber", STNo);
		}

		String nationalId = patientService.getNationalId();
		if (!GenericValidator.isBlankOrNull(nationalId)) {
			PropertyUtils.setProperty(form, "nationalIdNew", Boolean.FALSE);
			PropertyUtils.setProperty(form, "nationalId", nationalId);
		}

		ObservationHistory doctorObservation = getRefererObservation(sample);
		if (doctorObservation != null) {
			PropertyUtils.setProperty(form, "doctorNew", Boolean.FALSE);
			PropertyUtils.setProperty(form, "doctor", doctorObservation.getValue());
		}

		if (useSiteList) {
			PropertyUtils.setProperty(form, "serviceNew", Boolean.FALSE);
			PropertyUtils.setProperty(form, "service", getSampleRequesterOrganizationName());
		} else {
			ObservationHistory serviceObservation = getServiceObservation(sample);
			if (serviceObservation != null) {
				PropertyUtils.setProperty(form, "serviceNew", Boolean.FALSE);
				PropertyUtils.setProperty(form, "service", serviceObservation.getValue());
			}
		}

		PropertyUtils.setProperty(form, "comment", getNoteForSample(sample));

		PropertyUtils.setProperty(form, "requesterSampleID", sample.getReferringId());
	}

	/**
	 * @return
	 */
	private String getSampleRequesterOrganizationName() {
		SampleRequesterDAO sampleRequesterDAO = new SampleRequesterDAOImpl();
		List<SampleRequester> sampleRequestors = sampleRequesterDAO.getRequestersForSampleId(sample.getId());
		if (sampleRequestors.size() == 0) {
			return null;
		}
		long typeID = TableIdService.ORGANIZATION_REQUESTER_TYPE_ID;
		for (SampleRequester sampleRequester : sampleRequestors) {
			if (sampleRequester.getRequesterTypeId() == typeID) {
				String orgId = String.valueOf(sampleRequester.getRequesterId());
				Organization org = orgDAO.getOrganizationById(orgId);

				if (org != null) {
					String orgName = org.getOrganizationName();
					orgName += GenericValidator.isBlankOrNull(org.getShortName()) ? "" : ("-" + org.getShortName());
					return orgName;
				}
			}
		}
		return null;
	}

	/**
	 * @param provider
	 * @return
	 */
	private Person getProviderPerson(Provider provider) {
		if (provider == null) {
			return null;
		}
		Person providerPerson = provider.getPerson();
		personDAO.getData(providerPerson);
		return providerPerson;
	}

	private Provider getProvider() {
		if (sample == null) {
			return null;
		}
		SampleHuman sampleHuman = getSampleHuman();
		Provider provider = new Provider();
		String id = sampleHuman.getProviderId();
		if (id == null) {
			return null;
		}
		provider.setId(id);
		providerDAO.getData(provider);
		return provider;

	}

	/**
	 * @return
	 */
	private SampleHuman getSampleHuman() {
		SampleHuman sampleHuman = new SampleHuman();
		sampleHuman.setSampleId(sample.getId());
		sampleHumanDAO.getDataBySample(sampleHuman);
		return sampleHuman;
	}

	/**
	 * @param sample
	 * @return
	 */
	private List<QaEventItem> getSampleQaEventItems(Sample sample) {
		List<QaEventItem> qaEventItems = new ArrayList<QaEventItem>();
		if (sample != null) {
			getSampleQaEvents(sample);
			for (SampleQaEvent event : sampleQAEventList) {
				QAService qa = new QAService(event);
				QaEventItem item = new QaEventItem();
				item.setId(qa.getEventId());
				item.setQaEvent(qa.getQAEvent().getId());
				SampleItem sampleItem = qa.getSampleItem();
				// -1 is the index for "all samples"
				item.setSampleType((sampleItem == null) ? "-1" : sampleItem.getTypeOfSampleId());
				item.setSection(qa.getObservationValue(QAObservationType.SECTION));
				item.setAuthorizer(qa.getObservationValue(QAObservationType.AUTHORIZER));
				item.setRecordNumber(qa.getObservationValue(QAObservationType.DOC_NUMBER));
				item.setRemove(false);
				item.setNote(getNoteForSampleQaEvent(event));

				qaEventItems.add(item);
			}
		}

		int oldQaEvents = qaEventItems.size();
		for (int i = oldQaEvents; i < 10; i++) {
			qaEventItems.add(new QaEventItem());
		}
		return qaEventItems;
	}

	private Set<TypeOfSample> getSampleTypeOfSamples() {
		Set<TypeOfSample> typeOfSamples = new HashSet<TypeOfSample>();
		List<SampleItem> sampleItems = sampleItemDAO.getSampleItemsBySampleId(sample.getId());
		for (SampleItem sampleItem : sampleItems) {
			TypeOfSample typeOfSample = typeOfSampleDAO.getTypeOfSampleById(sampleItem.getTypeOfSampleId());
			if (!typeOfSamples.contains(typeOfSample)) {
				typeOfSamples.add(typeOfSample);
			}
		}
		return typeOfSamples;
	}

	private String getSampleTypeOfSamplesString() {
		if (sample == null) {
			return "";
		}
		Set<TypeOfSample> sampleTypeOfSamples = getSampleTypeOfSamples();
		StringBuilder str = new StringBuilder(",");
		for (TypeOfSample typeOfSample : sampleTypeOfSamples) {
			str.append(typeOfSample.getId()).append(",");
		}
		return str.toString();
	}

	public static String getNoteForSample(Sample sample) {
		Note note = new NoteService(sample).getMostRecentNoteFilteredBySubject(QA_NOTE_SUBJECT);
		return note != null ? note.getText() : null;
	}

	public static String getNoteForSampleQaEvent(SampleQaEvent sampleQaEvent) {
		if (sampleQaEvent == null || GenericValidator.isBlankOrNull(sampleQaEvent.getId())) {
			return null;
		} else {
			Note note = new NoteService(sampleQaEvent).getMostRecentNoteFilteredBySubject(null);
			return note != null ? note.getText() : null;
		}
	}

	private void getSampleQaEvents(Sample sample) {
		SampleQaEventDAO sampleQaEventDAO = new SampleQaEventDAOImpl();
		sampleQAEventList = sampleQaEventDAO.getSampleQaEventsBySample(sample);
	}

	private void setProjectList(NonConformityForm form)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		ProjectDAO projectDAO = new ProjectDAOImpl();
		List<Project> projects = projectDAO.getAllProjects();
		PropertyUtils.setProperty(form, "projects", projects);
	}

	private Sample getSampleForLabNumber(String labNumber) throws LIMSInvalidConfigurationException {
		return sampleDAO.getSampleByAccessionNumber(labNumber);
	}

	private void getPatient(Sample sample) {
		SampleHumanDAO sampleHumanDAO = new SampleHumanDAOImpl();
		Patient patient = sampleHumanDAO.getPatientForSample(sample);
		patientService = new PatientService(patient);
	}

	private void getObservationHistory(Sample sample) {
		ObservationHistoryDAO observationDAO = new ObservationHistoryDAOImpl();
		observationHistoryList = observationDAO.getAll(patientService.getPatient(), sample);
	}

	private Project getProjectForSample(Sample sample) {
		SampleProjectDAO samplePorjectDAO = new SampleProjectDAOImpl();
		SampleProject sampleProject = samplePorjectDAO.getSampleProjectBySampleId(sample.getId());

		return sampleProject == null ? null : sampleProject.getProject();
	}

	private ObservationHistory getRefererObservation(Sample sample) {
		for (ObservationHistory observation : observationHistoryList) {
			if (observation.getObservationHistoryTypeId().equals(TableIdService.DOCTOR_OBSERVATION_TYPE_ID)) {
				return observation;
			}
		}

		return null;
	}

	private ObservationHistory getServiceObservation(Sample sample) {
		for (ObservationHistory observation : observationHistoryList) {
			if (observation.getObservationHistoryTypeId().equals(TableIdService.SERVICE_OBSERVATION_TYPE_ID)) {
				return observation;
			}
		}

		return null;
	}

	private void sortSections(List<TestSection> list) {
		Collections.sort(list, new Comparator<TestSection>() {
			@Override
			public int compare(TestSection o1, TestSection o2) {
				return o1.getSortOrderInt() - o2.getSortOrderInt();
			}
		});
	}

	private List<TestSection> createSectionList() {

		List<TestSection> sections = new TestSectionDAOImpl().getAllActiveTestSections();
		if (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.NONCONFORMITY_RECEPTION_AS_UNIT,
				"true")) {
			TestSection extra = new TestSection();
			extra.setTestSectionName("Reception");
			extra.setSortOrder("0");
			extra.setNameKey("testSection.Reception");
			sections.add(extra);
		}

		if (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.NONCONFORMITY_SAMPLE_COLLECTION_AS_UNIT,
				"true")) {
			TestSection extra = new TestSection();
			extra.setTestSectionName("Sample Collection");
			extra.setSortOrder("1");
			extra.setNameKey("testSection.SampleCollection");
			sections.add(extra);
		}

		sortSections(sections);
		return sections;
	}

	@Override
	protected String getPageSubtitleKey() {
		return "qaevent.add.title";
	}

	@Override
	protected String getPageTitleKey() {
		return "qaevent.add.title";
	}

	protected ModelAndView findLocalForward(String forward, BaseForm form) {
		if ("success".equals(forward)) {
			return new ModelAndView("nonConformityDefiniton", "form", form);
		} else {
			return new ModelAndView("PageNotFound");
		}
	}
}
