/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping.metadata;

import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.common.util.ObjectTemplateIncludeProcessor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.model.common.mapping.metadata.ProcessingUtil.getProcessingOfItem;

/**
 * Specification of value metadata processing for a given data item: mappings that should be applied
 * and item definitions driving e.g. storage and applicability of built-in processing of individual
 * metadata items.
 *
 * Information present here is derived from e.g. object templates and metadata mappings attached to data mappings.
 * It is already processed regarding applicability of metadata processing to individual data items.
 */
public class ItemValueMetadataProcessingSpec implements ShortDumpable, DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(ItemValueMetadataProcessingSpec.class);

    @NotNull private final Collection<MetadataMappingType> mappings = new ArrayList<>();
    @NotNull private final Collection<MetadataItemDefinitionType> itemDefinitions = new ArrayList<>();
    @NotNull private final Collection<ItemPath> metadataPathsToIgnore = new ArrayList<>();

    /**
     * This processing spec will contain mappings targeted at this scope.
     */
    @NotNull private final MetadataMappingScopeType scope;

    /**
     * Item processing for given metadata items. Lazily evaluated.
     */
    private Map<ItemPath, ItemProcessingType> itemProcessingMap;

    private ItemValueMetadataProcessingSpec(@NotNull MetadataMappingScopeType scope) {
        this.scope = scope;
    }

    public static ItemValueMetadataProcessingSpec forScope(@NotNull MetadataMappingScopeType scope) {
        return new ItemValueMetadataProcessingSpec(scope);
    }

    public void populateFromCurrentFocusTemplate(ItemPath dataPath, ObjectResolver objectResolver, String contextDesc,
            Task task, OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        populateFromCurrentFocusTemplate(
                ModelExpressionThreadLocalHolder.getLensContext(), dataPath, objectResolver, contextDesc, task, result);
    }

    public void populateFromCurrentFocusTemplate(ModelContext<?> lensContext, ItemPath dataPath, ObjectResolver objectResolver, String contextDesc,
            Task task, OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        if (lensContext != null) {
            ObjectTemplateType focusTemplate = lensContext.getFocusTemplate();
            if (focusTemplate != null) {
                addFromObjectTemplate(focusTemplate, dataPath, objectResolver, contextDesc, task, result);
            } else {
                LOGGER.trace("No focus template for {}, no metadata handling from this source", lensContext);
            }
        } else {
            LOGGER.trace("No current lens context, no metadata handling");
        }
    }

    public boolean isEmpty() {
        return mappings.isEmpty() && itemDefinitions.isEmpty();
    }

    private void addFromObjectTemplate(ObjectTemplateType rootTemplate, ItemPath dataPath, ObjectResolver objectResolver, String contextDesc,
            Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {

        LOGGER.trace("Obtaining metadata handling instructions from {}", rootTemplate);

        // TODO resolve conflicts in included templates
        try {
            new ObjectTemplateIncludeProcessor(objectResolver)
                    .processThisAndIncludedTemplates(rootTemplate, contextDesc, task, result,
                            template -> addFromObjectTemplate(template, dataPath));
        } catch (TunnelException e) {
            if (e.getCause() instanceof SchemaException) {
                throw (SchemaException) e.getCause();
            } else {
                MiscUtil.unwrapTunnelledExceptionToRuntime(e);
            }
        }
    }

    private void addFromObjectTemplate(ObjectTemplateType template, ItemPath dataPath) {
        try {
            addHandling(template.getMeta(), dataPath);

            for (ObjectTemplateItemDefinitionType itemDef : template.getItem()) {
                if (itemDef.getRef() != null && itemDef.getRef().getItemPath().equivalent(dataPath)) {
                    addHandling(itemDef.getMeta(), dataPath);
                }
            }
        } catch (SchemaException e) {
            throw new TunnelException(e);
        }
    }

    private void addHandling(MetadataHandlingType handling, ItemPath dataPath) throws SchemaException {
        if (isHandlingApplicable(handling, dataPath)) {
            addMetadataMappings(handling.getMapping());
            addMetadataItems(handling.getItem(), dataPath);
        }
    }

    private boolean isHandlingApplicable(MetadataHandlingType handling, ItemPath dataPath) throws SchemaException {
        return handling != null && ProcessingUtil.doesApplicabilityMatch(handling.getApplicability(), dataPath);
    }

    private void addMetadataItems(List<MetadataItemDefinitionType> items, ItemPath dataPath) throws SchemaException {
        for (MetadataItemDefinitionType item : items) {
            if (isMetadataItemDefinitionApplicable(item, dataPath)) {
                itemDefinitions.add(item);
                for (MetadataMappingType itemMapping : item.getMapping()) {
                    if (isMetadataMappingApplicable(itemMapping)) {
                        mappings.add(provideDefaultTarget(itemMapping, item));
                    }
                }
            }
        }
    }

    private boolean isMetadataItemDefinitionApplicable(MetadataItemDefinitionType item, ItemPath dataPath) throws SchemaException {
        return !isMetadataItemIgnored(item.getRef()) && ProcessingUtil.doesApplicabilityMatch(item.getApplicability(), dataPath);
    }

    private MetadataMappingType provideDefaultTarget(MetadataMappingType itemMapping, MetadataItemDefinitionType item) {
        if (itemMapping.getTarget() != null && itemMapping.getTarget().getPath() != null) {
            return itemMapping;
        } else {
            checkRefNotNull(item);
            MetadataMappingType clone = itemMapping.clone();
            if (clone.getTarget() == null) {
                clone.beginTarget().path(item.getRef());
            } else {
                clone.getTarget().path(item.getRef());
            }
            return clone;
        }
    }

    private void checkRefNotNull(MetadataItemDefinitionType item) {
        if (item.getRef() == null) {
            // todo better error handling
            throw new IllegalArgumentException("No `ref` value in item definition");
        }
    }

    public void addMetadataMappings(List<MetadataMappingType> mappingsToAdd) {
        for (MetadataMappingType mapping : mappingsToAdd) {
            if (isMetadataMappingApplicable(mapping)) {
                mappings.add(mapping);
            }
        }
    }

    public @NotNull Collection<MetadataMappingType> getMappings() {
        return Collections.unmodifiableCollection(mappings);
    }

    private void computeItemProcessingMapIfNeeded() throws SchemaException {
        if (itemProcessingMap == null) {
            computeItemProcessingMap();
        }
    }

    private void computeItemProcessingMap() throws SchemaException {
        itemProcessingMap = new HashMap<>();
        for (MetadataItemDefinitionType item : itemDefinitions) {
            if (item.getRef() == null) {
                throw new SchemaException("No 'ref' in item definition: " + item);
            }
            ItemProcessingType processing = getProcessingOfItem(item);
            if (processing != null) {
                itemProcessingMap.put(item.getRef().getItemPath(), processing);
            }
        }
    }

    /**
     * Looks up processing information for given path. Proceeds from the most specific to most abstract (empty) path.
     */
    private ItemProcessingType getProcessing(ItemPath itemPath) throws SchemaException {
        computeItemProcessingMapIfNeeded();
        while (!itemPath.isEmpty()) {
            ItemProcessingType processing = ItemPathCollectionsUtil.getFromMap(itemProcessingMap, itemPath);
            if (processing != null) {
                return processing;
            }
            itemPath = itemPath.allExceptLast();
        }
        return null;
    }

    public boolean isFullProcessing(ItemPath itemPath) throws SchemaException {
        return getProcessing(itemPath) == ItemProcessingType.FULL;
    }

    public Collection<ItemPath> getTransientPaths() {
        PathSet transientPaths = new PathSet();
        PathSet persistentPaths = new PathSet();
        for (MetadataItemDefinitionType item : itemDefinitions) {
            ItemPersistenceType persistence = item.getPersistence();
            if (persistence != null) {
                ItemPath path = item.getRef().getItemPath();
                if (persistence == ItemPersistenceType.PERSISTENT) {
                    persistentPaths.add(path);
                } else {
                    transientPaths.add(path);
                }
            }
        }
        for (ItemPath persistentPath : persistentPaths) {
            transientPaths.remove(persistentPath);
        }
        return transientPaths;
    }

    // For item-contained mappings the target exclusion can be checked twice
    private boolean isMetadataMappingApplicable(MetadataMappingType mapping) {
        return hasCompatibleScope(mapping) &&
                (mapping.getTarget() == null || mapping.getTarget().getPath() == null
                    || isMetadataItemIgnored(mapping.getTarget().getPath()));
    }

    private boolean hasCompatibleScope(MetadataMappingType metadataMapping) {
        MetadataMappingScopeType mappingScope = metadataMapping.getScope();
        return mappingScope == null || mappingScope == scope;
    }

    public @NotNull MetadataMappingScopeType getScope() {
        return scope;
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append(String.format("%d mapping(s), %d item definition(s)", mappings.size(), itemDefinitions.size()));
        if (!itemDefinitions.isEmpty()) {
            sb.append(" for ");
            sb.append(itemDefinitions.stream()
                    .map(ItemRefinedDefinitionType::getRef)
                    .map(String::valueOf)
                    .collect(Collectors.joining(", ")));
        }
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();

        DebugUtil.debugDumpWithLabelLn(sb, "Mappings defined",
                mappings.stream()
                        .map(this::getTargetPath)
                        .collect(Collectors.toList()), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Items defined",
                itemDefinitions.stream()
                        .map(ItemRefinedDefinitionType::getRef)
                        .collect(Collectors.toList()), indent);
        DebugUtil.debugDumpWithLabel(sb, "Paths that were ignored",
                metadataPathsToIgnore, indent);
        return sb.toString();
    }

    private ItemPathType getTargetPath(MetadataMappingType mapping) {
        return mapping != null && mapping.getTarget() != null ? mapping.getTarget().getPath() : null;
    }

    public void addPathsToIgnore(@NotNull List<ItemPathType> pathsToIgnore) {
        pathsToIgnore.forEach(path -> this.metadataPathsToIgnore.add(path.getItemPath()));
    }

    private boolean isMetadataItemIgnored(ItemPathType pathBean) {
        if (metadataPathsToIgnore.isEmpty()) {
            return false;
        } else {
            ItemPath path = Objects.requireNonNull(pathBean, "'ref' is null").getItemPath();
            return ItemPathCollectionsUtil.containsSubpathOrEquivalent(metadataPathsToIgnore, path);
        }
    }
}
