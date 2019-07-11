package spring.generated.inventory.form;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import spring.mine.common.form.BaseForm;
import spring.mine.validation.annotations.ValidDate;
import us.mn.state.health.lims.common.util.IdValuePair;
import us.mn.state.health.lims.common.util.validator.CustomDateValidator.DateRelation;
import us.mn.state.health.lims.inventory.form.InventoryKitItem;

public class InventoryForm extends BaseForm {

	public interface ManageInventory {
	}

	@ValidDate(relative = DateRelation.TODAY)
	private String currentDate = "";

	@NotNull(groups = { ManageInventory.class })
	@Valid
	private List<InventoryKitItem> inventoryItems;

	// for display
	private List<IdValuePair> sources;

	// for display
	private List<IdValuePair> kitTypes;

	// in display
	private String newKitsXML = "";

	public InventoryForm() {
		setFormName("InventoryForm");
	}

	public String getCurrentDate() {
		return currentDate;
	}

	public void setCurrentDate(String currentDate) {
		this.currentDate = currentDate;
	}

	public List<InventoryKitItem> getInventoryItems() {
		return inventoryItems;
	}

	public void setInventoryItems(List<InventoryKitItem> inventoryItems) {
		this.inventoryItems = inventoryItems;
	}

	public List<IdValuePair> getSources() {
		return sources;
	}

	public void setSources(List<IdValuePair> sources) {
		this.sources = sources;
	}

	public List<IdValuePair> getKitTypes() {
		return kitTypes;
	}

	public void setKitTypes(List<IdValuePair> kitTypes) {
		this.kitTypes = kitTypes;
	}

	public String getNewKitsXML() {
		return newKitsXML;
	}

	public void setNewKitsXML(String newKitsXML) {
		this.newKitsXML = newKitsXML;
	}
}