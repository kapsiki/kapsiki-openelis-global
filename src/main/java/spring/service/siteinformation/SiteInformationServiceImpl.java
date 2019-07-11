package spring.service.siteinformation;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import spring.service.common.BaseObjectServiceImpl;
import us.mn.state.health.lims.common.util.ConfigurationSideEffects;
import us.mn.state.health.lims.siteinformation.dao.SiteInformationDAO;
import us.mn.state.health.lims.siteinformation.valueholder.SiteInformation;

@Service
public class SiteInformationServiceImpl extends BaseObjectServiceImpl<SiteInformation, String>
		implements SiteInformationService {

	@Autowired
	private SiteInformationDAO siteInformationDAO;
	@Autowired
	private ConfigurationSideEffects configurationSideEffects;

	public SiteInformationServiceImpl() {
		super(SiteInformation.class);
	}

	@Override
	protected SiteInformationDAO getBaseObjectDAO() {
		return siteInformationDAO;
	}

	@Override
	@Transactional(readOnly = true)
	public List<SiteInformation> getPageOfSiteInformationByDomainName(int startingRecNo, String dbDomainName) {
		return siteInformationDAO.getMatchingOrderedPage("domain.name", dbDomainName, "name", false, startingRecNo);
	}

	@Override
	@Transactional(readOnly = true)
	public int getCountForDomainName(String dbDomainName) {
		return this.getCountMatching("domain.name", dbDomainName);
	}

	@Override
	@Transactional(readOnly = true)
	public SiteInformation getSiteInformationByName(String name) {
		return getMatch("name", name).orElse(null);
	}

	@Override
	@Transactional(readOnly = true)
	public void getData(SiteInformation siteInformation) {
		getBaseObjectDAO().getData(siteInformation);

	}

	@Override
	@Transactional(readOnly = true)
	public List<SiteInformation> getAllSiteInformation() {
		return getBaseObjectDAO().getAllSiteInformation();
	}

	@Override
	@Transactional(readOnly = true)
	public SiteInformation getSiteInformationById(String urlId) {
		return getBaseObjectDAO().getSiteInformationById(urlId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<SiteInformation> getNextSiteInformationRecord(String id) {
		return getBaseObjectDAO().getNextSiteInformationRecord(id);
	}

	@Override
	@Transactional(readOnly = true)
	public List<SiteInformation> getSiteInformationByDomainName(String domainName) {
		return getBaseObjectDAO().getSiteInformationByDomainName(domainName);
	}

	@Override
	@Transactional(readOnly = true)
	public List<SiteInformation> getPreviousSiteInformationRecord(String id) {
		return getBaseObjectDAO().getPreviousSiteInformationRecord(id);
	}

	@Override
	@Transactional
	public void persistData(SiteInformation siteInformation, boolean newSiteInformation) {
		if (newSiteInformation) {
			insert(siteInformation);
//			siteInformationDAO.insertData(siteInformation);
		} else {
			update(siteInformation);
//			siteInformationDAO.updateData(siteInformation);
		}

		configurationSideEffects.siteInformationChanged(siteInformation);
	}

}