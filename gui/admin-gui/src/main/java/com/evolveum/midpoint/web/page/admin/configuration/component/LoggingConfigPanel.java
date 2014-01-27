/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.page.admin.configuration.component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.EditableLinkColumn;
import com.evolveum.midpoint.web.component.data.column.EditablePropertyColumn;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.ListMultipleChoicePanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.Editable;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.configuration.dto.*;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author lazyman
 */
public class LoggingConfigPanel extends SimplePanel<LoggingDto> {

    private static final String DOT_CLASS = LoggingConfigPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_LOGGING_CONFIGURATION = DOT_CLASS + "loadLoggingConfiguration";
    private static final String OPERATION_UPDATE_LOGGING_CONFIGURATION = DOT_CLASS + "updateLoggingConfiguration";

    private static final String ID_LOGGERS_TABLE = "loggersTable";
    private static final String ID_ROOT_LEVEL = "rootLevel";
    private static final String ID_ROOT_APPENDER = "rootAppender";

    public LoggingConfigPanel(String id) {
        super(id, null);
    }

    @Override
    public IModel<LoggingDto> createModel() {
        return new LoadableModel<LoggingDto>(false) {

            @Override
            protected LoggingDto load() {
                return initLoggingModel();
            }
        };
    }

    private LoggingDto initLoggingModel() {
        LoggingDto dto = null;
        OperationResult result = new OperationResult(OPERATION_LOAD_LOGGING_CONFIGURATION);
        try {
            Task task = getPageBase().createSimpleTask(OPERATION_LOAD_LOGGING_CONFIGURATION);

            PrismObject<SystemConfigurationType> config = getPageBase().getModelService().getObject(
                    SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), null,
                    task, result);
            SystemConfigurationType systemConfiguration = config.asObjectable();
            LoggingConfigurationType logging = systemConfiguration.getLogging();
            dto = new LoggingDto(config, logging);

            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't load logging configuration.", ex);
        }

        if (!result.isSuccess()) {
            getPageBase().showResult(result);
        }

        if (dto == null) {
            dto = new LoggingDto();
        }

        return dto;
    }

    @Override
    protected void initLayout() {
        initLoggers();
        initAudit();
        initProfiling();
    }

    private void initLoggers() {
        initRoot();

        ISortableDataProvider<LoggerConfiguration, String> provider = new ListDataProvider<LoggerConfiguration>(this,
                new PropertyModel<List<LoggerConfiguration>>(getModel(), "loggers"));
        TablePanel table = new TablePanel<LoggerConfiguration>(ID_LOGGERS_TABLE, provider, initLoggerColumns());
        table.setOutputMarkupId(true);
        table.setShowPaging(false);
        add(table);

        AjaxButton addComponentLogger = new AjaxButton("addComponentLogger",
                createStringResource("LoggingConfigPanel.button.addComponentLogger")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addComponentLoggerPerformed(target);
            }
        };
        add(addComponentLogger);

        AjaxButton addClassLogger = new AjaxButton("addClassLogger",
                createStringResource("LoggingConfigPanel.button.addClassLogger")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addClassLoggerPerformed(target);
            }
        };
        add(addClassLogger);

        AjaxButton deleteLogger = new AjaxButton("deleteLogger",
                createStringResource("LoggingConfigPanel.button.deleteLogger")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteLoggerPerformed(target);
            }
        };
        add(deleteLogger);

        //initProfiling();
    }

    private void initRoot() {
        DropDownChoice<LoggingLevelType> rootLevel = new DropDownChoice<LoggingLevelType>(ID_ROOT_LEVEL,
                new PropertyModel<LoggingLevelType>(getModel(), LoggingDto.F_ROOT_LEVEL),
                WebMiscUtil.createReadonlyModelFromEnum(LoggingLevelType.class));

        add(rootLevel);

        DropDownChoice<String> rootAppender = new DropDownChoice<String>(ID_ROOT_APPENDER,
                new PropertyModel<String>(getModel(), LoggingDto.F_ROOT_APPENDER), createAppendersListModel());
        rootAppender.setNullValid(true);
        add(rootAppender);
    }

    private void initAudit(){
        CheckBox auditLog = new CheckBox("auditLog", new PropertyModel<Boolean>(getModel(), "auditLog"));
        add(auditLog);

        CheckBox auditDetails = new CheckBox("auditDetails", new PropertyModel<Boolean>(getModel(), "auditDetails"));
        add(auditDetails);

        DropDownChoice<String> auditAppender = new DropDownChoice<String>("auditAppender", new PropertyModel<String>(
                getModel(), "auditAppender"), createAppendersListModel());
        auditAppender.setNullValid(true);
        add(auditAppender);
    }

    private void initProfiling(){
        //Entry-Exit profiling init
        DropDownChoice<ProfilingLevel> profilingLevel = new DropDownChoice<ProfilingLevel>("profilingLevel",
                new PropertyModel<ProfilingLevel>(getModel(), "profilingLevel"),
                WebMiscUtil.createReadonlyModelFromEnum(ProfilingLevel.class),
                new EnumChoiceRenderer(this));
        add(profilingLevel);

        DropDownChoice<String> profilingAppender = new DropDownChoice<String>("profilingAppender",
                new PropertyModel<String>(getModel(), "profilingAppender"), createAppendersListModel());
        profilingAppender.setNullValid(true);
        add(profilingAppender);

        //Subsystem and general profiling init
        CheckBox requestFilter = new CheckBox("requestFilter", new PropertyModel<Boolean>(getModel(), "requestFilter"));
        CheckBox performanceStatistics = new CheckBox("performanceStatistics", new PropertyModel<Boolean>(getModel(), "performanceStatistics"));
        CheckBox subsystemModel = new CheckBox("subsystemModel", new PropertyModel<Boolean>(getModel(), "subsystemModel"));
        CheckBox subsystemRepository = new CheckBox("subsystemRepository", new PropertyModel<Boolean>(getModel(), "subsystemRepository"));
        CheckBox subsystemProvisioning = new CheckBox("subsystemProvisioning", new PropertyModel<Boolean>(getModel(), "subsystemProvisioning"));
        CheckBox subsystemUcf = new CheckBox("subsystemUcf", new PropertyModel<Boolean>(getModel(), "subsystemUcf"));
        CheckBox subsystemResourceObjectChangeListener = new CheckBox("subsystemResourceObjectChangeListener", new PropertyModel<Boolean>(getModel(), "subsystemResourceObjectChangeListener"));
        CheckBox subsystemTaskManager = new CheckBox("subsystemTaskManager", new PropertyModel<Boolean>(getModel(), "subsystemTaskManager"));
        CheckBox subsystemWorkflow = new CheckBox("subsystemWorkflow", new PropertyModel<Boolean>(getModel(), "subsystemWorkflow"));
        add(requestFilter);
        add(performanceStatistics);
        add(subsystemModel);
        add(subsystemRepository);
        add(subsystemProvisioning);
        add(subsystemUcf);
        add(subsystemResourceObjectChangeListener);
        add(subsystemTaskManager);
        add(subsystemWorkflow);

        TextField<Integer> dumpInterval = new TextField<Integer>("dumpInterval", new PropertyModel<Integer>(getModel(),
                "dumpInterval"));
        add(dumpInterval);
    }

    private void addComponentLoggerPerformed(AjaxRequestTarget target) {
        LoggingDto dto = getModel().getObject();
        ComponentLogger logger = new ComponentLogger(new ClassLoggerConfigurationType());
        logger.setEditing(true);
        dto.getLoggers().add(logger);

        target.add(getLoggersTable());
    }

    private void addClassLoggerPerformed(AjaxRequestTarget target) {
        LoggingDto dto = getModel().getObject();
        ClassLogger logger = new ClassLogger(new ClassLoggerConfigurationType());
        logger.setEditing(true);
        dto.getLoggers().add(logger);

        target.add(getLoggersTable());
    }

    private TablePanel getLoggersTable() {
        return (TablePanel) get(ID_LOGGERS_TABLE);
    }

    private List<IColumn<LoggerConfiguration, String>> initLoggerColumns() {
        List<IColumn<LoggerConfiguration, String>> columns = new ArrayList<IColumn<LoggerConfiguration, String>>();
        IColumn column = new CheckBoxHeaderColumn<LoggerConfiguration>();
        columns.add(column);

        //name editing column
        columns.add(new EditableLinkColumn<LoggerConfiguration>(
                createStringResource("LoggingConfigPanel.logger"), "name") {

            @Override
            protected Component createInputPanel(String componentId, final IModel<LoggerConfiguration> model) {
                if (model.getObject() instanceof ComponentLogger) {
                    DropDownChoicePanel dropDownChoicePanel = new DropDownChoicePanel(componentId,
                            new PropertyModel(model, "component"),
                            WebMiscUtil.createReadonlyModelFromEnum(LoggingComponentType.class),
                            new IChoiceRenderer<LoggingComponentType>() {

                                @Override
                                public Object getDisplayValue(LoggingComponentType item) {
                                    return LoggingConfigPanel.this.getString("LoggingConfigPanel.logger." + item);
                                }

                                @Override
                                public String getIdValue(LoggingComponentType item, int index) {
                                    return Integer.toString(index);
                                }
                            });

                    FormComponent<LoggingComponentType> input = dropDownChoicePanel.getBaseFormComponent();
                    input.add(new LoggerValidator());
                    input.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                    return dropDownChoicePanel;
                } else {
                    TextPanel<String> textPanel = new TextPanel(componentId, new PropertyModel(model, getPropertyExpression()));
                    FormComponent input = textPanel.getBaseFormComponent();
                    input.add(new AttributeAppender("style", "width: 100%"));
                    input.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                    input.add(new InputStringValidator());
                    return textPanel;
                }

            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<LoggerConfiguration> rowModel) {
                loggerEditPerformed(target, rowModel);
            }
        });

        //level editing column
        columns.add(new EditableLinkColumn<LoggerConfiguration>(createStringResource("LoggingConfigPanel.loggersLevel"), "level") {

            @Override
            protected Component createInputPanel(String componentId, IModel<LoggerConfiguration> model) {
                DropDownChoicePanel dropDownChoicePanel = new DropDownChoicePanel(componentId,
                        new PropertyModel(model, getPropertyExpression()),
                        WebMiscUtil.createReadonlyModelFromEnum(LoggingLevelType.class));
                FormComponent<LoggingLevelType> input = dropDownChoicePanel.getBaseFormComponent();
                input.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                input.add(new LevelValidator());
                return dropDownChoicePanel;
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<LoggerConfiguration> rowModel) {
                loggerEditPerformed(target, rowModel);
            }

            @Override
            protected IModel<String> createLinkModel(IModel<LoggerConfiguration> rowModel) {
                LoggerConfiguration configuration = rowModel.getObject();
                return WebMiscUtil.createLocalizedModelForEnum(configuration.getLevel(), getPageBase());
            }
        });

        //appender editing column
        columns.add(new EditablePropertyColumn<LoggerConfiguration>(createStringResource("LoggingConfigPanel.loggersAppender"),
                "appenders") {

            @Override
            public IModel<Object> getDataModel(final IModel rowModel) {
                return new LoadableModel<Object>() {

                    @Override
                    protected Object load() {
                        LoggerConfiguration config = (LoggerConfiguration) rowModel.getObject();
                        StringBuilder builder = new StringBuilder();
                        for (String appender : config.getAppenders()) {
                            if (config.getAppenders().indexOf(appender) != 0) {
                                builder.append(", ");
                            }
                            builder.append(appender);
                        }

                        return builder.toString();
                    }
                };
            }

            @Override
            protected InputPanel createInputPanel(String componentId, IModel<LoggerConfiguration> model) {
                ListMultipleChoicePanel panel = new ListMultipleChoicePanel<String>(componentId,
                        new PropertyModel<List<String>>(model, getPropertyExpression()), createNewLoggerAppendersListModel());

                ListMultipleChoice choice = (ListMultipleChoice) panel.getBaseFormComponent();
                choice.setMaxRows(3);

                panel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());

                return panel;
            }
        });

        return columns;
    }

    private IModel<List<String>> createNewLoggerAppendersListModel(){
        return new AbstractReadOnlyModel<List<String>>() {

            @Override
            public List<String> getObject() {
                List<String> list = new ArrayList<String>();

                LoggingDto dto = getModel().getObject();

                //list.add("(default - root appender)");
                list.add("");
                for(AppenderConfiguration appender: dto.getAppenders()){
                    list.add(appender.getName());
                }

                return list;
            }
        };
    }

    private IModel<List<String>> createAppendersListModel() {
        return new AbstractReadOnlyModel<List<String>>() {

            @Override
            public List<String> getObject() {
                List<String> list = new ArrayList<String>();

                LoggingDto dto = getModel().getObject();
                for (AppenderConfiguration appender : dto.getAppenders()) {
                    list.add(appender.getName());
                }

                return list;
            }
        };
    }

    private void deleteLoggerPerformed(AjaxRequestTarget target) {
        Iterator<LoggerConfiguration> iterator = getModel().getObject().getLoggers().iterator();
        while (iterator.hasNext()) {
            LoggerConfiguration item = iterator.next();
            if (item.isSelected()) {
                iterator.remove();
            }
        }
        target.add(getLoggersTable());
    }

    private IModel<LoggingComponentType> createFilterModel(final IModel<FilterConfiguration> model) {
        return new Model<LoggingComponentType>() {

            @Override
            public LoggingComponentType getObject() {
                String name = model.getObject().getName();
                if (StringUtils.isEmpty(name)) {
                    return null;
                }
                return LoggingComponentType.fromValue(name);
            }

            @Override
            public void setObject(LoggingComponentType object) {
                model.getObject().setName(object.name());
            }
        };
    }

    private void loggerEditPerformed(AjaxRequestTarget target, IModel<LoggerConfiguration> rowModel) {
        LoggerConfiguration config = rowModel.getObject();
        config.setEditing(true);
        target.add(getLoggersTable());
    }

    private static class EmptyOnBlurAjaxFormUpdatingBehaviour extends AjaxFormComponentUpdatingBehavior {

        public EmptyOnBlurAjaxFormUpdatingBehaviour() {
            super("onBlur");
        }

        @Override
        protected void onUpdate(AjaxRequestTarget target) {
        }
    }

    private static class FileAppenderColumn<T extends Editable> extends EditablePropertyColumn<T> {

        private FileAppenderColumn(IModel<String> displayModel, String propertyExpression) {
            super(displayModel, propertyExpression);
        }

        @Override
        protected boolean isEditing(IModel<T> rowModel) {
            return super.isEditing(rowModel) && (rowModel.getObject() instanceof FileAppenderConfig);
        }

        @Override
        protected InputPanel createInputPanel(String componentId, IModel iModel) {
            InputPanel panel = super.createInputPanel(componentId, iModel);
            panel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
            panel.add(new InputStringValidator());
            return panel;
        }
    }
}
