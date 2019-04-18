/*
 * Copyright (c) 2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.component.assignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.AssignmentPopup;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.ItemWrapperOld;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismContainerWrapperColumn;
import com.evolveum.midpoint.gui.impl.prism.ContainerWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerPanel;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValuePanel;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.session.ObjectTabStorage;
import com.evolveum.midpoint.model.api.AssignmentCandidatesSpecification;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.MultifunctionalButton;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.prism.PropertyOrReferenceWrapper;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.prism.ValueWrapperOld;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.users.component.AllAssignmentsPreviewDialog;
import com.evolveum.midpoint.web.page.admin.users.component.AssignmentInfoDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AreaCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExclusionPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PersonaConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;

public class AssignmentPanel extends BasePanel<PrismContainerWrapper<AssignmentType>> {

	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(AssignmentPanel.class);

	private static final String ID_ASSIGNMENTS = "assignments";
	protected static final String ID_SEARCH_FRAGMENT = "searchFragment";
	protected static final String ID_SPECIFIC_CONTAINERS_FRAGMENT = "specificContainersFragment";
	private final static String ID_ACTIVATION_PANEL = "activationPanel";
	protected static final String ID_SPECIFIC_CONTAINER = "specificContainers";
	private static final String ID_NEW_ITEM_BUTTON = "newItemButton";
	private static final String ID_SHOW_ALL_ASSIGNMENTS_BUTTON = "showAllAssignmentsButton";
	private static final String ID_BUTTON_TOOLBAR_FRAGMENT = "buttonToolbarFragment";


	private static final String DOT_CLASS = AssignmentPanel.class.getName() + ".";
	protected static final String OPERATION_LOAD_ASSIGNMENTS_LIMIT = DOT_CLASS + "loadAssignmentsLimit";
	protected static final String OPERATION_LOAD_ASSIGNMENTS_TARGET_OBJ = DOT_CLASS + "loadAssignmentsTargetRefObject";
	protected static final String OPERATION_LOAD_ASSIGNMENT_TARGET_RELATIONS = DOT_CLASS + "loadAssignmentTargetRelations";

	protected int assignmentsRequestsLimit = -1;
	private List<ContainerValueWrapper<AssignmentType>> detailsPanelAssignmentsList = new ArrayList<>();

	public AssignmentPanel(String id, IModel<PrismContainerWrapper<AssignmentType>> assignmentContainerWrapperModel) {
		super(id, assignmentContainerWrapperModel);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		assignmentsRequestsLimit = AssignmentsUtil.loadAssignmentsLimit(new OperationResult(OPERATION_LOAD_ASSIGNMENTS_LIMIT), getPageBase());
		initLayout();
	}

	private void initLayout() {

		MultivalueContainerListPanelWithDetailsPanel<AssignmentType, AssignmentObjectRelation> multivalueContainerListPanel =
				new MultivalueContainerListPanelWithDetailsPanel<AssignmentType, AssignmentObjectRelation>(ID_ASSIGNMENTS, getModel(), getTableId(),
				getAssignmentsTabStorage()) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void initPaging() {
				initCustomPaging();
			}

			@Override
			protected boolean enableActionNewObject() {
				return isNewObjectButtonVisible(getFocusObject());
			}

			@Override
			protected void cancelItemDetailsPerformed(AjaxRequestTarget target){
				AssignmentPanel.this.cancelAssignmentDetailsPerformed(target);
			}

			@Override
			protected ObjectQuery createQuery() {
				return createObjectQuery();
			}

			@Override
			protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> createColumns() {
				return initBasicColumns();
			}

			@Override
			protected void newItemPerformed(AjaxRequestTarget target){
				//todo clean up
				newAssignmentClickPerformed(target, null);
			}

			@Override
			protected void newItemPerformed(AjaxRequestTarget target, AssignmentObjectRelation assignmentTargetRelation) {
				newAssignmentClickPerformed(target, assignmentTargetRelation);
			}

			@Override
			protected List<AssignmentObjectRelation> getNewObjectInfluencesList() {
				return WebComponentUtil.getRelationsDividedList(loadAssignmentTargetRelationsList());
			}

			@Override
			protected DisplayType getNewObjectButtonDisplayType() {
				return WebComponentUtil.createDisplayType(GuiStyleConstants.EVO_ASSIGNMENT_ICON, "green",
						AssignmentPanel.this.createStringResource(isInducement() ?
								"AssignmentPanel.newInducementTitle" : "AssignmentPanel.newAssignmentTitle").getString());
			}

			@Override
			protected DisplayType getNewObjectAdditionalButtonDisplayType(AssignmentObjectRelation assignmentTargetRelation) {
				return WebComponentUtil.getAssignmentObjectRelationDisplayType(assignmentTargetRelation,
						AssignmentPanel.this.createStringResource(isInducement() ?
								"AssignmentPanel.newInducementTitle" : "AssignmentPanel.newAssignmentTitle").getString());
			}

			@Override
			protected boolean isNewObjectButtonEnabled(){
				return !isAssignmentsLimitReached();
			}

			@Override
			protected void deleteItemPerformed(AjaxRequestTarget target, List<PrismContainerValueWrapper<AssignmentType>> toDeleteList) {
				int countAddedAssignments = 0;
				for (PrismContainerValueWrapper<AssignmentType> assignment : toDeleteList) {
					if (ValueStatus.ADDED.equals(assignment.getStatus())){
						countAddedAssignments++;
					}
				}
				boolean isLimitReached = isAssignmentsLimitReached(toDeleteList.size() - countAddedAssignments, true);
				if (isLimitReached) {
					warn(getParentPage().getString("AssignmentPanel.assignmentsLimitReachedWarning", assignmentsRequestsLimit));
					target.add(getPageBase().getFeedbackPanel());
					return;
				}
				super.deleteItemPerformed(target, toDeleteList);
			}

			@Override
			protected List<PrismContainerValueWrapper<AssignmentType>> postSearch(
					List<PrismContainerValueWrapper<AssignmentType>> assignments) {
				return customPostSearch(assignments);
			}

			@Override
			protected MultivalueContainerDetailsPanel<AssignmentType> getMultivalueContainerDetailsPanel(
					ListItem<PrismContainerValueWrapper<AssignmentType>> item) {
				return createMultivalueContainerDetailsPanel(item);
			}

			@Override
			protected WebMarkupContainer getSearchPanel(String contentAreaId) {
				return getCustomSearchPanel(contentAreaId);
			}

			@Override
			protected List<SearchItemDefinition> initSearchableItems(PrismContainerDefinition<AssignmentType> containerDef) {
				return createSearchableItems(containerDef);
			}

			@Override
			protected WebMarkupContainer initButtonToolbar(String id) {
				WebMarkupContainer buttonToolbar = initCustomButtonToolbar(id);
				if(buttonToolbar == null) {
					return super.initButtonToolbar(id);
				}
				return buttonToolbar;
			}

		};
		add(multivalueContainerListPanel);

		setOutputMarkupId(true);
	}

	protected Fragment initCustomButtonToolbar(String contentAreaId){
		Fragment searchContainer = new Fragment(contentAreaId, ID_BUTTON_TOOLBAR_FRAGMENT, this);

		MultifunctionalButton newObjectIcon = getMultivalueContainerListPanel().getNewItemButton(ID_NEW_ITEM_BUTTON);
		searchContainer.add(newObjectIcon);

		AjaxIconButton showAllAssignmentsButton = new AjaxIconButton(ID_SHOW_ALL_ASSIGNMENTS_BUTTON, new Model<>("fa fa-address-card"),
				createStringResource("AssignmentTablePanel.menu.showAllAssignments")) {

			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget ajaxRequestTarget) {
				showAllAssignments(ajaxRequestTarget);
			}
		};
		searchContainer.addOrReplace(showAllAssignmentsButton);
		showAllAssignmentsButton.setOutputMarkupId(true);
		showAllAssignmentsButton.add(new VisibleBehaviour(() -> !isInducement()));
		return searchContainer;
	}

	protected void showAllAssignments(AjaxRequestTarget target) {
		PageBase pageBase = getPageBase();
		List<AssignmentInfoDto> previewAssignmentsList;
		if (pageBase instanceof PageAdminFocus) {
			previewAssignmentsList = ((PageAdminFocus<?>) pageBase).showAllAssignmentsPerformed(target);
		} else {
			previewAssignmentsList = Collections.emptyList();
		}
		AllAssignmentsPreviewDialog assignmentsDialog = new AllAssignmentsPreviewDialog(pageBase.getMainPopupBodyId(), previewAssignmentsList,
				pageBase);
		pageBase.showMainPopup(assignmentsDialog, target);
	}

	protected List<SearchItemDefinition> createSearchableItems(PrismContainerDefinition<AssignmentType> containerDef){
		List<SearchItemDefinition> defs = new ArrayList<>();

		if (getAssignmentType() == null) {
			SearchFactory.addSearchRefDef(containerDef, ItemPath.create(AssignmentType.F_TARGET_REF), defs, AreaCategoryType.ADMINISTRATION, getPageBase());
			SearchFactory.addSearchRefDef(containerDef, ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF), defs, AreaCategoryType.ADMINISTRATION, getPageBase());
			SearchFactory.addSearchPropertyDef(containerDef, ItemPath.create(AssignmentType.F_POLICY_RULE, PolicyRuleType.F_NAME), defs);
			SearchFactory.addSearchRefDef(containerDef,
					ItemPath.create(AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS,
							PolicyConstraintsType.F_EXCLUSION, ExclusionPolicyConstraintType.F_TARGET_REF), defs, AreaCategoryType.POLICY, getPageBase());
		}
		SearchFactory.addSearchPropertyDef(containerDef, ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), defs);
		SearchFactory.addSearchPropertyDef(containerDef, ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS), defs);

		defs.addAll(SearchFactory.createExtensionDefinitionList(containerDef));

		return defs;

	}

	protected QName getAssignmentType(){
		return null;
	}

	protected void initCustomPaging(){
		getAssignmentsTabStorage().setPaging(getPrismContext().queryFactory().createPaging(0, (int) getParentPage().getItemsPerPage(UserProfileStorage.TableId.ASSIGNMENTS_TAB_TABLE)));
	}

	protected ObjectTabStorage getAssignmentsTabStorage(){
		if (isInducement()){
			return getParentPage().getSessionStorage().getInducementsTabStorage();
		} else {
			return getParentPage().getSessionStorage().getAssignmentsTabStorage();
		}
	}

	protected List<PrismContainerValueWrapper<AssignmentType>> customPostSearch(List<PrismContainerValueWrapper<AssignmentType>> assignments) {
		return assignments;
	}

	protected boolean isNewObjectButtonVisible(PrismObject focusObject){
		try {
			return getParentPage().isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_ASSIGN_ACTION_URI,
					AuthorizationPhaseType.REQUEST, focusObject,
					null, null, null);
		} catch (Exception ex){
			return WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_ASSIGN_ACTION_URI);
		}
	}

	protected ObjectQuery createObjectQuery(){
		Collection<QName> delegationRelations = getParentPage().getRelationRegistry()
				.getAllRelationsFor(RelationKindType.DELEGATION);


		//do not show archetype assignments
		ObjectReferenceType archetypeRef = new ObjectReferenceType();
		archetypeRef.setType(ArchetypeType.COMPLEX_TYPE);
		archetypeRef.setRelation(new QName(PrismConstants.NS_QUERY, "any"));
		RefFilter archetypeFilter = (RefFilter) getParentPage().getPrismContext().queryFor(AssignmentType.class)
				.item(AssignmentType.F_TARGET_REF)
				.ref(archetypeRef.asReferenceValue())
				.buildFilter();
		archetypeFilter.setOidNullAsAny(true);
		archetypeFilter.setRelationNullAsAny(true);

		ObjectQuery query = getParentPage().getPrismContext().queryFor(AssignmentType.class)
				.not()
				.item(AssignmentType.F_TARGET_REF)
				.ref(delegationRelations.toArray(new QName[0]))
				.build();
		query.addFilter(getPrismContext().queryFactory().createNot(archetypeFilter));
		return query;
	}

	protected void cancelAssignmentDetailsPerformed(AjaxRequestTarget target){
	}

	private List<AssignmentObjectRelation> loadAssignmentTargetRelationsList(){
		OperationResult result = new OperationResult(OPERATION_LOAD_ASSIGNMENT_TARGET_RELATIONS);
		List<AssignmentObjectRelation> assignmentTargetRelations = new ArrayList<>();
		PrismObject obj = getMultivalueContainerListPanel().getFocusObject();
		try {
			AssignmentCandidatesSpecification spec = getPageBase().getModelInteractionService()
					.determineAssignmentTargetSpecification(obj, result);
			assignmentTargetRelations = spec != null ? spec.getAssignmentObjectRelations() : new ArrayList<>();
		} catch (SchemaException | ConfigurationException ex){
			result.recordPartialError(ex.getLocalizedMessage());
			LOGGER.error("Couldn't load assignment target specification for the object {} , {}", obj.getName(), ex.getLocalizedMessage());
		}
		return assignmentTargetRelations;
	}

	protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> initBasicColumns() {
		List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> columns = new ArrayList<>();

		columns.add(new CheckBoxHeaderColumn<>());

		columns.add(new IconColumn<PrismContainerValueWrapper<AssignmentType>>(Model.of("")) {

			private static final long serialVersionUID = 1L;

			@Override
			protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<AssignmentType>> rowModel){
				AssignmentType assignment = rowModel.getObject().getRealValue();
				if (assignment != null && assignment.getTargetRef() != null && StringUtils.isNotEmpty(assignment.getTargetRef().getOid())){
					List<ObjectType> targetObjectList = WebComponentUtil.loadReferencedObjectList(Arrays.asList(assignment.getTargetRef()), OPERATION_LOAD_ASSIGNMENTS_TARGET_OBJ,
							AssignmentPanel.this.getPageBase());
					if (targetObjectList != null && targetObjectList.size() > 0){
						ObjectType targetObject = targetObjectList.get(0);
						DisplayType displayType = WebComponentUtil.getArchetypePolicyDisplayType(targetObject, AssignmentPanel.this.getPageBase());
						if (displayType != null){
							String disabledStyle = "";
							if (targetObject instanceof FocusType) {
								disabledStyle = WebComponentUtil.getIconEnabledDisabled(((FocusType)targetObject).asPrismObject());
								if (displayType.getIcon() != null && StringUtils.isNotEmpty(displayType.getIcon().getCssClass()) &&
										disabledStyle != null){
									displayType.getIcon().setCssClass(displayType.getIcon().getCssClass() + " " + disabledStyle);
									displayType.getIcon().setColor("");
								}
							}
							return displayType;
						}
					}
				}
				return WebComponentUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(
						AssignmentsUtil.getTargetType(rowModel.getObject().getRealValue())));
			}

		});

		columns.add(new LinkColumn<PrismContainerValueWrapper<AssignmentType>>(createStringResource("PolicyRulesPanel.nameColumn")){
			private static final long serialVersionUID = 1L;

			@Override
			protected IModel<String> createLinkModel(IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {
				String name = AssignmentsUtil.getName(rowModel.getObject(), getParentPage());
				if (StringUtils.isBlank(name)) {
					return createStringResource("AssignmentPanel.noName");
				}
				return Model.of(name);
			}

			@Override
			public boolean isEnabled(IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {
				if (rowModel.getObject().getRealValue().getFocusMappings() != null){
					return false;
				}
				return true;
			}

			@Override
			public void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {
				AssignmentPanel.this.assignmentDetailsPerformed(target);
				getMultivalueContainerListPanel().itemDetailsPerformed(target, rowModel);
			}
		});

		columns.add(new PrismContainerWrapperColumn<>(getModel(), AssignmentType.F_ACTIVATION));
		
		
//		columns.add(new AbstractItemWrapperColumn<AssignmentType>(getModel(), AssignmentType.F_ACTIVATION, getPageBase()){
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			public void populateItem(Item<ICellPopulator<ContainerValueWrapper<AssignmentType>>> item, String componentId,
//									 final IModel<ContainerValueWrapper<AssignmentType>> rowModel) {
//				Label label = new Label(componentId, getActivationLabelModel(rowModel.getObject()));
//				label.add(AttributeModifier.append("class", " col-xs-12 "));
//				item.add(label);
//			}
//		});
		columns.addAll(initColumns());
		List<InlineMenuItem> menuActionsList = getAssignmentMenuActions();
		columns.add(new InlineMenuButtonColumn<PrismContainerValueWrapper<AssignmentType>>(menuActionsList, getPageBase()){
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean isButtonMenuItemEnabled(IModel<PrismContainerValueWrapper<AssignmentType>> rowModel){
				if (rowModel != null
						&& ValueStatus.ADDED.equals(rowModel.getObject().getStatus())) {
					return true;
				}
				return !isAssignmentsLimitReached();
			}
		});
		return columns;
	}

	protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> initColumns(){
		return new ArrayList<>();
	}

	protected void assignmentDetailsPerformed(AjaxRequestTarget target){
	}

	protected void newAssignmentClickPerformed(AjaxRequestTarget target, AssignmentObjectRelation assignmentTargetRelation){
		AssignmentPopup popupPanel = new AssignmentPopup(getPageBase().getMainPopupBodyId()) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void addPerformed(AjaxRequestTarget target, List newAssignmentsList) {
				super.addPerformed(target, newAssignmentsList);
				addSelectedAssignmentsPerformed(target, newAssignmentsList);
			}

			@Override
			protected List<ObjectTypes> getAvailableObjectTypesList(){
				if (assignmentTargetRelation == null || CollectionUtils.isEmpty(assignmentTargetRelation.getObjectTypes())) {
					return getObjectTypesList();
				} else {
					return mergeNewAssignmentTargetTypeLists(assignmentTargetRelation.getObjectTypes(), getObjectTypesList());
				}
			}

			@Override
			protected QName getPredefinedRelation(){
				if (assignmentTargetRelation == null){
					return null;
				}
				return !CollectionUtils.isEmpty(assignmentTargetRelation.getRelations()) ? assignmentTargetRelation.getRelations().get(0) : null;
			}

			@Override
			protected List<ObjectReferenceType> getArchetypeRefList(){
				return assignmentTargetRelation != null ? assignmentTargetRelation.getArchetypeRefs() : null;
			}

			@Override
			protected ObjectFilter getSubtypeFilter(){
				return AssignmentPanel.this.getSubtypeFilter();
			}

			@Override
			protected boolean isEntitlementAssignment(){
				return AssignmentPanel.this.isEntitlementAssignment();
			}

			@Override
			protected PrismContainerWrapper<AssignmentType> getAssignmentWrapperModel() {
				return AssignmentPanel.this.getModelObject();
			}
		};
		popupPanel.setOutputMarkupId(true);
		getPageBase().showMainPopup(popupPanel, target);
	}

	protected List<ObjectTypes> getObjectTypesList(){
		if (getAssignmentType() == null) {
			return WebComponentUtil.createAssignableTypesList();
		} else {
			return Arrays.asList(ObjectTypes.getObjectTypeFromTypeQName(getAssignmentType()));
		}
	}

	protected boolean isEntitlementAssignment(){
		return false;
	}

	protected void addSelectedAssignmentsPerformed(AjaxRequestTarget target, List<AssignmentType> newAssignmentsList){
		if (newAssignmentsList == null || newAssignmentsList.isEmpty()) {
			warn(getParentPage().getString("AssignmentTablePanel.message.noAssignmentSelected"));
			target.add(getPageBase().getFeedbackPanel());
			return;
		}
		boolean isAssignmentsLimitReached = isAssignmentsLimitReached(newAssignmentsList != null ? newAssignmentsList.size() : 0, true);
		if (isAssignmentsLimitReached) {
			warn(getParentPage().getString("AssignmentPanel.assignmentsLimitReachedWarning", assignmentsRequestsLimit));
			target.add(getPageBase().getFeedbackPanel());
			return;
		}

		newAssignmentsList.forEach(assignment -> {
			PrismContainerDefinition<AssignmentType> definition = getModelObject().getItem().getDefinition();
			PrismContainerValue<AssignmentType> newAssignment;
			try {
				newAssignment = definition.instantiate().createNewValue();
				AssignmentType assignmentType = newAssignment.asContainerable();

				if (assignment.getConstruction() != null && assignment.getConstruction().getResourceRef() != null) {
					assignmentType.setConstruction(assignment.getConstruction());
				} else {
					assignmentType.setTargetRef(assignment.getTargetRef());
				}
				getMultivalueContainerListPanel().createNewItemContainerValueWrapper(newAssignment, getModelObject());
				getMultivalueContainerListPanel().refreshTable(target);
				getMultivalueContainerListPanel().reloadSavePreviewButtons(target);
			} catch (SchemaException e) {
				getSession().error("Cannot create new assignment " + e.getMessage());
				target.add(getPageBase().getFeedbackPanel());
				target.add(this);
			}

		});


	}

	protected WebMarkupContainer getCustomSearchPanel(String contentAreaId) {
		return new WebMarkupContainer(contentAreaId);
	}

	private MultivalueContainerDetailsPanel<AssignmentType> createMultivalueContainerDetailsPanel(
			ListItem<PrismContainerValueWrapper<AssignmentType>> item) {
		if (isAssignmentsLimitReached()){
			item.getModelObject().setReadOnly(true, true);
		} else if (item.getModelObject().isReadOnly()){
			item.getModelObject().setReadOnly(false, true);
		}
		
		MultivalueContainerDetailsPanel<AssignmentType> detailsPanel = new  MultivalueContainerDetailsPanel<AssignmentType>(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel()) {

			private static final long serialVersionUID = 1L;

			@Override
			protected ItemVisibility getBasicTabVisibity(ItemWrapper<?,?,?,?> itemWrapper, ItemPath parentAssignmentPath) {
				return AssignmentPanel.this.getBasicTabVisibity(itemWrapper, parentAssignmentPath, item.getModel());
			}

			@Override
			protected void addBasicContainerValuePanel(String idPanel) {
				add(getBasicContainerPanel(idPanel, item.getModel()));
			}

			@Override
			protected  Fragment getSpecificContainers(String contentAreaId) {
				Fragment specificContainers = getCustomSpecificContainers(contentAreaId, item.getModel());
				Form form = this.findParent(Form.class);

				ItemPath assignmentPath = item.getModelObject().getRealValue().asPrismContainerValue().getPath();
				PrismContainerWrapperModel<AssignmentType, ActivationType> activationModel = PrismContainerWrapperModel.fromContainerValueWrapper(item.getModel(), AssignmentType.F_ACTIVATION);
				PrismContainerPanel<ActivationType> acitvationContainer = new PrismContainerPanel<ActivationType>(ID_ACTIVATION_PANEL, activationModel);
//				PrismContainerPanelOld<ActivationType> acitvationContainer = new PrismContainerPanelOld<ActivationType>(ID_ACTIVATION_PANEL, IModel.of(activationModel), form, itemWrapper -> getActivationVisibileItems(itemWrapper.getPath(), assignmentPath));
				specificContainers.add(acitvationContainer);

				return specificContainers;
			}

			@Override
			protected DisplayNamePanel<AssignmentType> createDisplayNamePanel(String displayNamePanelId) {
				IModel<AssignmentType> displayNameModel = getDisplayModel(item.getModelObject().getRealValue());
				return new DisplayNamePanel<AssignmentType>(displayNamePanelId, displayNameModel) {

					private static final long serialVersionUID = 1L;

					@Override
					protected QName getRelation() {
						return getRelationForDisplayNamePanel(item.getModelObject());
					}

					@Override
					protected IModel<String> getKindIntentLabelModel() {
						return getKindIntentLabelModelForDisplayNamePanel(item.getModelObject());
					}

				};
			}

		};
		return detailsPanel;
	}

	private ItemVisibility getBasicTabVisibity(ItemWrapper<?, ?, ?, ?> itemWrapper, ItemPath parentAssignmentPath, IModel<PrismContainerValueWrapper<AssignmentType>> model) {
//		PrismContainerValue<AssignmentType> prismContainerValue = model.getObject().getContainerValue();
		ItemPath assignmentPath = model.getObject().getPath();
//		ItemPath assignmentPath = model.getObject().getRealValue().asPrismContainerValue().getPath();
		return getAssignmentBasicTabVisibity(itemWrapper, parentAssignmentPath, assignmentPath, model.getObject().getRealValue());
	}

	protected PrismContainerValuePanel<AssignmentType, PrismContainerValueWrapper<AssignmentType>> getBasicContainerPanel(String idPanel, IModel<PrismContainerValueWrapper<AssignmentType>>  model) {
		Form form = new Form<>("form");
		ItemPath itemPath = getModelObject().getPath();
//		model.getObject().getParent().setShowOnTopLevel(true);
		return new PrismContainerValuePanel<>(idPanel, model);
		
//		(idPanel, model, true, form,
//				itemWrapper -> getBasicTabVisibity(itemWrapper, itemPath, model), getPageBase());
	}

	private QName getRelationForDisplayNamePanel(PrismContainerValueWrapper<AssignmentType> modelObject) {
		AssignmentType assignment = modelObject.getRealValue();
		if (assignment.getTargetRef() != null) {
			return assignment.getTargetRef().getRelation();
		} else {
			return null;
		}
	}

	private IModel<String> getKindIntentLabelModelForDisplayNamePanel(PrismContainerValueWrapper<AssignmentType> modelObject) {
		AssignmentType assignment = modelObject.getRealValue();
		if (assignment.getConstruction() != null){
			return createStringResource("DisplayNamePanel.kindIntentLabel", assignment.getConstruction().getKind(),
					assignment.getConstruction().getIntent());
		}
		return Model.of();
	}

	private ItemVisibility getActivationVisibileItems(ItemPath pathToCheck, ItemPath assignmentPath) {
		if (assignmentPath.append(ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP)).equivalent(pathToCheck)) {
			return ItemVisibility.HIDDEN;
		}

		if (assignmentPath.append(ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS)).equivalent(pathToCheck)) {
			return ItemVisibility.HIDDEN;
		}

		return ItemVisibility.AUTO;
	}

	private List<ObjectTypes> mergeNewAssignmentTargetTypeLists(List<QName> allowedByAssignmentTargetSpecification, List<ObjectTypes> availableTypesList){
		if (CollectionUtils.isEmpty(allowedByAssignmentTargetSpecification)){
			return availableTypesList;
		}
		if (CollectionUtils.isEmpty(availableTypesList)){
			return availableTypesList;
		}
		List<ObjectTypes> mergedList = new ArrayList<>();
		allowedByAssignmentTargetSpecification.forEach(qnameValue -> {
			ObjectTypes objectTypes = ObjectTypes.getObjectTypeFromTypeQName(qnameValue);
			for (ObjectTypes availableObjectTypes : availableTypesList) {
				if (availableObjectTypes.getClassDefinition().equals(objectTypes.getClassDefinition())) {
					mergedList.add(objectTypes);
					break;
				}
			}
		});
		return mergedList;
	}

	protected Fragment getCustomSpecificContainers(String contentAreaId, IModel<PrismContainerValueWrapper<AssignmentType>> modelObject) {
		Fragment specificContainers = new Fragment(contentAreaId, AssignmentPanel.ID_SPECIFIC_CONTAINERS_FRAGMENT, this);
		specificContainers.add(getSpecificContainerPanel(modelObject));
		return specificContainers;
	}

	protected PrismContainerPanel getSpecificContainerPanel(IModel<PrismContainerValueWrapper<AssignmentType>> modelObject) {
		Form form = new Form<>("form");
//		ItemPath assignmentPath = modelObject.getPath();
		PrismContainerPanel constraintsContainerPanel = new PrismContainerPanel(ID_SPECIFIC_CONTAINER, getSpecificContainerModel(modelObject));
//		PrismContainerPanelOld constraintsContainerPanel = new PrismContainerPanelOld(ID_SPECIFIC_CONTAINER,
//				getSpecificContainerModel(modelObject), form,
//				itemWrapper -> getSpecificContainersItemsVisibility(itemWrapper, assignmentPath));
		constraintsContainerPanel.setOutputMarkupId(true);
		return constraintsContainerPanel;
	}

	protected ItemVisibility getSpecificContainersItemsVisibility(ItemWrapperOld itemWrapper, ItemPath parentAssignmentPath) {
		if(parentAssignmentPath.append(AssignmentType.F_CONSTRUCTION).append(ConstructionType.F_ATTRIBUTE).append(ResourceAttributeDefinitionType.F_INBOUND).namedSegmentsOnly().isSubPathOrEquivalent(itemWrapper.getPath().namedSegmentsOnly())
				|| parentAssignmentPath.append(AssignmentType.F_CONSTRUCTION).append(ConstructionType.F_ASSOCIATION).append(ResourceAttributeDefinitionType.F_INBOUND).namedSegmentsOnly().isSubPathOrEquivalent(itemWrapper.getPath().namedSegmentsOnly())) {
			return ItemVisibility.HIDDEN;
		}
		if (ContainerWrapperImpl.class.isAssignableFrom(itemWrapper.getClass())){
			return ItemVisibility.AUTO;
		}
		List<ItemPath> pathsToHide = new ArrayList<>();
		pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_CONSTRUCTION).append(ConstructionType.F_RESOURCE_REF).namedSegmentsOnly());
		pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_CONSTRUCTION).append(ConstructionType.F_AUXILIARY_OBJECT_CLASS).namedSegmentsOnly());
		if (PropertyOrReferenceWrapper.class.isAssignableFrom(itemWrapper.getClass()) && !WebComponentUtil.isItemVisible(pathsToHide, itemWrapper.getPath().namedSegmentsOnly())) {
			return ItemVisibility.AUTO;
		} else {
			return ItemVisibility.HIDDEN;
		}
	}

	@Deprecated
	protected IModel<PrismContainerWrapper> getSpecificContainerModel(IModel<PrismContainerValueWrapper<AssignmentType>> modelObject){
		//TODO cannot this be done by inheritance for concrete panel???
		AssignmentType assignment = modelObject.getObject().getRealValue();
		if (ConstructionType.COMPLEX_TYPE.equals(AssignmentsUtil.getTargetType(assignment))) {
			IModel<PrismContainerWrapper<ConstructionType>> constructionModel = PrismContainerWrapperModel.fromContainerValueWrapper(modelObject, AssignmentType.F_CONSTRUCTION);
			return (IModel) constructionModel;
//			PrismContainerWrapper<ConstructionType> constructionWrapper = modelObject.findContainer(ItemPath.create(modelObject.getPath(),
//					AssignmentType.F_CONSTRUCTION));
//
//			return Model.of(constructionWrapper);
		}

		if (PersonaConstructionType.COMPLEX_TYPE.equals(AssignmentsUtil.getTargetType(assignment))) {
			IModel<PrismContainerWrapper<PersonaConstructionType>> constructionModel = PrismContainerWrapperModel.fromContainerValueWrapper(modelObject, AssignmentType.F_PERSONA_CONSTRUCTION);
			return (IModel) constructionModel;
			//TODO is it correct? findContainerWrapper by path F_PERSONA_CONSTRUCTION will return PersonaConstructionType
			//but not PolicyRuleType
//			PrismContainerWrapper<PolicyRuleType> personasWrapper = modelObject.findContainer(ItemPath.create(modelObject.getPath(),
//					AssignmentType.F_PERSONA_CONSTRUCTION));
//
//			return Model.of(personasWrapper);
		}
		if (PolicyRuleType.COMPLEX_TYPE.equals(AssignmentsUtil.getTargetType(assignment))) {
			IModel<PrismContainerWrapper<PolicyRuleType>> constructionModel = PrismContainerWrapperModel.fromContainerValueWrapper(modelObject, AssignmentType.F_POLICY_RULE);
			return (IModel) constructionModel;
//			PrismContainerWrapper<PolicyRuleType> policyRuleWrapper = modelObject.findContainer(ItemPath.create(modelObject.getPath(), AssignmentType.F_POLICY_RULE));
//			return Model.of(policyRuleWrapper);
		}
		return Model.of();
	}

	protected ItemVisibility getAssignmentBasicTabVisibity(ItemWrapper<?, ?, ?, ?> itemWrapper, ItemPath parentAssignmentPath, ItemPath assignmentPath, AssignmentType assignment) {

		if (itemWrapper.getPath().equals(assignmentPath.append(AssignmentType.F_METADATA))){
			return ItemVisibility.AUTO;
		}
		ObjectReferenceType targetRef = assignment.getTargetRef();
		List<ItemPath> pathsToHide = new ArrayList<>();
		QName targetType = null;
		if (targetRef != null) {
			targetType = targetRef.getType();
		}
		pathsToHide.add(assignmentPath.append(AssignmentType.F_TARGET_REF));
		pathsToHide.add(assignmentPath.append(AssignmentType.F_TARGET));

		if (OrgType.COMPLEX_TYPE.equals(targetType) || AssignmentsUtil.isPolicyRuleAssignment(assignment)) {
			pathsToHide.add(assignmentPath.append(AssignmentType.F_TENANT_REF));
			pathsToHide.add(assignmentPath.append(AssignmentType.F_ORG_REF));
		}
		if (AssignmentsUtil.isPolicyRuleAssignment(assignment)){
			pathsToHide.add(assignmentPath.append(AssignmentType.F_FOCUS_TYPE));
		}

		if (assignment.getConstruction() == null) {
			pathsToHide.add(assignmentPath.append(AssignmentType.F_CONSTRUCTION));
		}
		if (assignment.getPersonaConstruction() == null) {
			pathsToHide.add(assignmentPath.append(AssignmentType.F_PERSONA_CONSTRUCTION));
		}
		if (assignment.getPolicyRule() == null) {
			pathsToHide.add(assignmentPath.append(AssignmentType.F_POLICY_RULE));
		}

		if (PropertyOrReferenceWrapper.class.isAssignableFrom(itemWrapper.getClass()) && !WebComponentUtil.isItemVisible(pathsToHide, itemWrapper.getPath())) {
			return ItemVisibility.AUTO;
		} else {
			return ItemVisibility.HIDDEN;
		}
	}

	private <C extends Containerable> IModel<C> getDisplayModel(AssignmentType assignment){
		final IModel<C> displayNameModel = new IModel<C>() {

			private static final long serialVersionUID = 1L;

			@Override
			public C getObject() {
				if (assignment.getTargetRef() != null) {
					Task task = getPageBase().createSimpleTask("Load target");
					com.evolveum.midpoint.schema.result.OperationResult result = task.getResult();
					PrismObject<ObjectType> targetObject = WebModelServiceUtils.loadObject(assignment.getTargetRef(), getPageBase(), task, result);
					return targetObject != null ? (C) targetObject.asObjectable() : null;
				}
				if (assignment.getConstruction() != null && assignment.getConstruction().getResourceRef() != null) {
					Task task = getPageBase().createSimpleTask("Load resource");
					com.evolveum.midpoint.schema.result.OperationResult result = task.getResult();
					return (C) WebModelServiceUtils.loadObject(assignment.getConstruction().getResourceRef(), getPageBase(), task, result).asObjectable();
				} else if (assignment.getPersonaConstruction() != null) {
					return (C) assignment.getPersonaConstruction();
				} else if (assignment.getPolicyRule() !=null) {
					return (C) assignment.getPolicyRule();
				}

				return null;
			}

		};
		return displayNameModel;
	}

	private List<InlineMenuItem> getAssignmentMenuActions() {
		List<InlineMenuItem> menuItems = new ArrayList<>();
		PrismObject obj = getMultivalueContainerListPanel().getFocusObject();
		try {
			boolean isUnassignAuthorized = getParentPage().isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_UNASSIGN_ACTION_URI,
					AuthorizationPhaseType.REQUEST, obj,
					null, null, null);
			if (isUnassignAuthorized) {
				menuItems.add(new ButtonInlineMenuItem(getAssignmentsLimitReachedTitleModel("pageAdminFocus.menu.unassign")) {
					private static final long serialVersionUID = 1L;

					@Override
					public String getButtonIconCssClass() {
						return GuiStyleConstants.CLASS_DELETE_MENU_ITEM;
					}

					@Override
					public InlineMenuItemAction initAction() {
						return getMultivalueContainerListPanel().createDeleteColumnAction();
					}
				});
			}

		} catch (Exception ex){
			LOGGER.error("Couldn't check unassign authorization for the object: {}, {}", obj.getName(), ex.getLocalizedMessage());
			if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_ASSIGN_ACTION_URI)){
				menuItems.add(new ButtonInlineMenuItem(createStringResource("PageBase.button.unassign")) {
					private static final long serialVersionUID = 1L;

					@Override
					public String getButtonIconCssClass() {
						return GuiStyleConstants.CLASS_DELETE_MENU_ITEM;
					}

					@Override
					public InlineMenuItemAction initAction() {
						return getMultivalueContainerListPanel().createDeleteColumnAction();
					}
				});
			}
		}
		menuItems.add(new ButtonInlineMenuItem(createStringResource("PageBase.button.edit")) {
			private static final long serialVersionUID = 1L;

			@Override
			public String getButtonIconCssClass() {
				return GuiStyleConstants.CLASS_EDIT_MENU_ITEM;
			}

			@Override
			public InlineMenuItemAction initAction() {
				return getMultivalueContainerListPanel().createEditColumnAction();
			}
		});
		menuItems.add(new ButtonInlineMenuItem(createStringResource("AssignmentPanel.viewTargetObject")) {
			private static final long serialVersionUID = 1L;

			@Override
			public String getButtonIconCssClass() {
				return GuiStyleConstants.CLASS_NAVIGATE_ARROW;
			}

			@Override
			public InlineMenuItemAction initAction() {
				return new ColumnMenuAction<ContainerValueWrapper<AssignmentType>>() {
					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target) {
						ContainerValueWrapper<AssignmentType> assignmentContainer = getRowModel().getObject();
						PropertyOrReferenceWrapper targetRef = assignmentContainer.findPropertyWrapper(assignmentContainer.getPath()
								.append(AssignmentType.F_TARGET_REF));

						if (targetRef != null && targetRef.getValues() != null && targetRef.getValues().size() > 0) {
							ValueWrapperOld refWrapper = (ValueWrapperOld)targetRef.getValues().get(0);
							PrismReferenceValue refValue = (PrismReferenceValue)refWrapper.getValue();
							ObjectReferenceType ort = new ObjectReferenceType();
							ort.setupReferenceValue(refValue);
							if (!StringUtils.isEmpty(ort.getOid())) {
								WebComponentUtil.dispatchToObjectDetailsPage(ort, AssignmentPanel.this, false);
							}
						}
					}
				};
			}

			@Override
			public boolean isHeaderMenuItem(){
				return false;
			}
		});
		return menuItems;
	}

	protected MultivalueContainerListPanelWithDetailsPanel<AssignmentType, AssignmentCandidatesSpecification> getMultivalueContainerListPanel() {
		return ((MultivalueContainerListPanelWithDetailsPanel<AssignmentType, AssignmentCandidatesSpecification>)get(ID_ASSIGNMENTS));
	}

	public MultivalueContainerDetailsPanel<AssignmentType> getMultivalueContainerDetailsPanel() {
		return ((MultivalueContainerDetailsPanel<AssignmentType>)get(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS));
	}

	//TODO override for each type ?
	protected TableId getTableId() {
		return UserProfileStorage.TableId.ASSIGNMENTS_TAB_TABLE;
	}

	protected WebMarkupContainer getAssignmentContainer() {
		return getMultivalueContainerListPanel().getItemContainer();
	}

	protected PageBase getParentPage() {
		return getPageBase();
	}

//	private IModel<String> getActivationLabelModel(ContainerValueWrapper<AssignmentType> assignmentContainer){
//		ContainerWrapperImpl<ActivationType> activationContainer = assignmentContainer.findContainerWrapper(assignmentContainer.getPath().append(AssignmentType.F_ACTIVATION));
//		ActivationStatusType administrativeStatus = null;
//		XMLGregorianCalendar validFrom = null;
//		XMLGregorianCalendar validTo = null;
//		ActivationType activation = null;
//		String lifecycleStatus = "";
//		PropertyOrReferenceWrapper lifecycleStatusProperty = assignmentContainer.findPropertyWrapperByName(AssignmentType.F_LIFECYCLE_STATE);
//		if (lifecycleStatusProperty != null && lifecycleStatusProperty.getValues() != null){
//			Iterator<ValueWrapperOld> iter = lifecycleStatusProperty.getValues().iterator();
//			if (iter.hasNext()){
//				lifecycleStatus = (String) iter.next().getValue().getRealValue();
//			}
//		}
//		if (activationContainer != null){
//			activation = new ActivationType();
//			PropertyOrReferenceWrapper administrativeStatusProperty = activationContainer.findPropertyWrapper(ActivationType.F_ADMINISTRATIVE_STATUS);
//			if (administrativeStatusProperty != null && administrativeStatusProperty.getValues() != null){
//				Iterator<ValueWrapperOld> iter = administrativeStatusProperty.getValues().iterator();
//				if (iter.hasNext()){
//					administrativeStatus = (ActivationStatusType) iter.next().getValue().getRealValue();
//					activation.setAdministrativeStatus(administrativeStatus);
//				}
//			}
//			PropertyOrReferenceWrapper validFromProperty = activationContainer.findPropertyWrapper(ActivationType.F_VALID_FROM);
//			if (validFromProperty != null && validFromProperty.getValues() != null){
//				Iterator<ValueWrapperOld> iter = validFromProperty.getValues().iterator();
//				if (iter.hasNext()){
//					validFrom = (XMLGregorianCalendar) iter.next().getValue().getRealValue();
//					activation.setValidFrom(validFrom);
//				}
//			}
//			PropertyOrReferenceWrapper validToProperty = activationContainer.findPropertyWrapper(ActivationType.F_VALID_TO);
//			if (validToProperty != null && validToProperty.getValues() != null){
//				Iterator<ValueWrapperOld> iter = validToProperty.getValues().iterator();
//				if (iter.hasNext()){
//					validTo = (XMLGregorianCalendar) iter.next().getValue().getRealValue();
//					activation.setValidTo(validTo);
//				}
//			}
//		}
//		if (administrativeStatus != null){
//			return Model.of(WebModelServiceUtils
//					.getAssignmentEffectiveStatus(lifecycleStatus, activation, getPageBase()).value().toLowerCase());
//		} else {
//			return AssignmentsUtil.createActivationTitleModel(WebModelServiceUtils
//							.getAssignmentEffectiveStatus(lifecycleStatus, activation, getPageBase()),
//					validFrom, validTo, getMultivalueContainerListPanel());
//		}
//
//	}

	private IModel<String> getAssignmentsLimitReachedTitleModel(String defaultTitleKey) {
		return new LoadableModel<String>(true) {
			@Override
			protected String load() {
				return isAssignmentsLimitReached() ?
						AssignmentPanel.this.getPageBase().createStringResource("RoleCatalogItemButton.assignmentsLimitReachedTitle",
								assignmentsRequestsLimit).getString() : createStringResource(defaultTitleKey).getString();
			}
		};
	}

	protected boolean isAssignmentsLimitReached() {
		return isAssignmentsLimitReached(0, false);
	}

	protected boolean isAssignmentsLimitReached(int selectedAssignmentsCount, boolean actionPerformed) {
		if (assignmentsRequestsLimit < 0){
			return false;
		}
		int changedItems = 0;
		List<PrismContainerValueWrapper<AssignmentType>> assignmentsList = getModelObject().getValues();
		for (PrismContainerValueWrapper<AssignmentType> assignment : assignmentsList){
			if (assignment.hasChanged()){
				changedItems++;
			}
		}
		return actionPerformed ? (changedItems + selectedAssignmentsCount) > assignmentsRequestsLimit :
				(changedItems + selectedAssignmentsCount)  >= assignmentsRequestsLimit;
	}

	protected boolean isInducement(){
		return getModelObject().getPath().containsNameExactly(AbstractRoleType.F_INDUCEMENT);
	}

	protected ObjectFilter getSubtypeFilter(){
		return null;
	}
}
