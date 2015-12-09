/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.prism.delta.PropertyDelta.createModificationReplaceProperty;
import static com.evolveum.midpoint.schema.RetrieveOption.INCLUDE;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REMEDIATION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REVIEW_STAGE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_CASE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_STATE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_CAMPAIGN_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_CURRENT_RESPONSE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_CURRENT_STAGE_NUMBER;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_DECISION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_REVIEWER_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType.F_COMMENT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType.F_RESPONSE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.ACCEPT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.DELEGATE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NOT_DECIDED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NO_RESPONSE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.F_NAME;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CertificationTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(CertificationTest.class);
    private static final File TEST_DIR = new File("src/test/resources/cert");
    public static final File CAMPAIGN_1_FILE = new File(TEST_DIR, "cert-campaign-1.xml");

    private String campaignOid;
    private PrismObjectDefinition<AccessCertificationCampaignType> campaignDef;

    @Test
    public void test100AddCampaignNonOverwrite() throws Exception {
        PrismObject<AccessCertificationCampaignType> campaign = prismContext.parseObject(CAMPAIGN_1_FILE);
        campaignDef = campaign.getDefinition();

        OperationResult result = new OperationResult("test100AddCampaignNonOverwrite");

        campaignOid = repositoryService.addObject(campaign, null, result);

        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());

        checkCampaign(campaignOid, result, (PrismObject) prismContext.parseObject(CAMPAIGN_1_FILE), null);
    }

    @Test(expectedExceptions = ObjectAlreadyExistsException.class)
    public void test105AddCampaignNonOverwriteExisting() throws Exception {
        PrismObject<AccessCertificationCampaignType> campaign = prismContext.parseObject(CAMPAIGN_1_FILE);
        OperationResult result = new OperationResult("test105AddCampaignNonOverwriteExisting");
        repositoryService.addObject(campaign, null, result);
    }

    @Test
    public void test108AddCampaignOverwriteExisting() throws Exception {
        PrismObject<AccessCertificationCampaignType> campaign = prismContext.parseObject(CAMPAIGN_1_FILE);
        OperationResult result = new OperationResult("test108AddCampaignOverwriteExisting");
        campaign.setOid(campaignOid);       // doesn't work without specifying OID
        campaignOid = repositoryService.addObject(campaign, RepoAddOptions.createOverwrite(), result);

        checkCampaign(campaignOid, result, (PrismObject) prismContext.parseObject(CAMPAIGN_1_FILE), null);
    }

    @Test
    public void test200ModifyCampaignProperties() throws Exception {
        OperationResult result = new OperationResult("test200ModifyCampaignProperties");

        List<ItemDelta> modifications = new ArrayList<>();
        modifications.add(createModificationReplaceProperty(F_NAME, campaignDef, new PolyString("Campaign 1+", "campaign 1")));
        modifications.add(createModificationReplaceProperty(F_STATE, campaignDef, IN_REVIEW_STAGE));

        executeAndCheckModification(modifications, result);
    }

    @Test
    public void test210ModifyCaseProperties() throws Exception {
        OperationResult result = new OperationResult("test210ModifyCaseProperties");

        List<ItemDelta> modifications = new ArrayList<>();
        ItemPath case1 = new ItemPath(F_CASE).subPath(new IdItemPathSegment(1L));
        modifications.add(createModificationReplaceProperty(case1.subPath(F_CURRENT_RESPONSE), campaignDef, DELEGATE));
        modifications.add(createModificationReplaceProperty(case1.subPath(F_CURRENT_STAGE_NUMBER), campaignDef, 300));

        executeAndCheckModification(modifications, result);
    }

    @Test
    public void test220ModifyDecisionProperties() throws Exception {
        OperationResult result = new OperationResult("test220ModifyDecisionProperties");

        List<ItemDelta> modifications = new ArrayList<>();
        ItemPath d1 = new ItemPath(F_CASE).subPath(1L).subPath(F_DECISION).subPath(1L);
        modifications.add(createModificationReplaceProperty(d1.subPath(F_RESPONSE), campaignDef, DELEGATE));
        modifications.add(createModificationReplaceProperty(d1.subPath(F_COMMENT), campaignDef, "hi"));

        executeAndCheckModification(modifications, result);
    }

    @Test
    public void test230ModifyAllLevels() throws Exception {
        OperationResult result = new OperationResult("test230ModifyAllLevels");

        List<ItemDelta> modifications = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                .item(F_NAME).replace(new PolyString("Campaign 2", "campaign 2"))
                .item(F_STATE).replace(IN_REMEDIATION)
                .item(F_CASE, 2, F_CURRENT_RESPONSE).replace(NO_RESPONSE)
                .item(F_CASE, 2, F_CURRENT_STAGE_NUMBER).replace(400)
                .item(F_CASE, 1, F_DECISION, 1, F_RESPONSE).replace(NOT_DECIDED)
                .item(F_CASE, 1, F_DECISION, 1, F_COMMENT).replace("low")
                .asItemDeltas();

        executeAndCheckModification(modifications, result);
    }

    @Test
    public void test240AddCases() throws Exception {
        OperationResult result = new OperationResult("test240AddDeleteCases");

        AccessCertificationCaseType caseNoId = new AccessCertificationCaseType(prismContext);
        caseNoId.setObjectRef(createObjectRef("123", ObjectTypes.USER));
        caseNoId.setTargetRef(createObjectRef("456", ObjectTypes.ROLE));
        caseNoId.setCurrentStageNumber(1);

        AccessCertificationCaseType case100 = new AccessCertificationCaseType(prismContext);
        case100.setId(100L);
        case100.setObjectRef(createObjectRef("100123", ObjectTypes.USER));
        case100.setTargetRef(createObjectRef("100456", ObjectTypes.ROLE));
        case100.getReviewerRef().add(createObjectRef("100789", ObjectTypes.USER));
        case100.setCurrentStageNumber(1);

        List<ItemDelta> modifications = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                .item(F_CASE).add(caseNoId, case100)
                .asItemDeltas();

        executeAndCheckModification(modifications, result);
    }

    @Test
    public void test242DeleteCase() throws Exception {
        OperationResult result = new OperationResult("test242DeleteCase");

        AccessCertificationCaseType case7 = new AccessCertificationCaseType();
        case7.setId(7L);

        List<ItemDelta> modifications = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                .item(F_CASE).delete(case7)
                .asItemDeltas();

        executeAndCheckModification(modifications, result);
    }

    @Test
    public void test244ModifyCase() throws Exception {
        OperationResult result = new OperationResult("test244ModifyCase");

        AccessCertificationCaseType case7 = new AccessCertificationCaseType();
        case7.setId(7L);

        PrismReferenceValue reviewerToDelete = createObjectRef("100789", ObjectTypes.USER).asReferenceValue();

        List<ItemDelta> modifications = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                .item(F_CASE, 100, F_REVIEWER_REF).delete(reviewerToDelete)
                .asItemDeltas();

        executeAndCheckModification(modifications, result);
    }


    @Test
    public void test248AddDeleteModifyCase() throws Exception {
        OperationResult result = new OperationResult("test248AddDeleteModifyCase");

        AccessCertificationCaseType caseNoId = new AccessCertificationCaseType(prismContext);
        caseNoId.setObjectRef(createObjectRef("x123", ObjectTypes.USER));
        caseNoId.setTargetRef(createObjectRef("x456", ObjectTypes.ROLE));
        caseNoId.setCurrentStageNumber(1);

        AccessCertificationCaseType case110 = new AccessCertificationCaseType(prismContext);
        case110.setId(110L);
        case110.setObjectRef(createObjectRef("x100123", ObjectTypes.USER));
        case110.setTargetRef(createObjectRef("x100456", ObjectTypes.ROLE));
        case110.getReviewerRef().add(createObjectRef("x100789", ObjectTypes.USER));
        case110.setCurrentStageNumber(1);

        AccessCertificationCaseType case100 = new AccessCertificationCaseType();
        case100.setId(100L);

        List<ItemDelta> modifications = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                .item(F_CASE).add(caseNoId, case110).delete(case100)
                .item(F_CASE, 3, F_CURRENT_STAGE_NUMBER).replace(400)
                .asItemDeltas();

        executeAndCheckModification(modifications, result);
    }

    @Test
    public void test250AddDeleteModifyResponse() throws Exception {
        OperationResult result = new OperationResult("test250AddDeleteModifyResponse");

        AccessCertificationDecisionType decNoId = new AccessCertificationDecisionType(prismContext);
        decNoId.setReviewerRef(createObjectRef("888", ObjectTypes.USER));
        decNoId.setStageNumber(1);

        AccessCertificationDecisionType dec200 = new AccessCertificationDecisionType(prismContext);
        dec200.setId(200L);
        dec200.setStageNumber(1);
        dec200.setReviewerRef(createObjectRef("200888", ObjectTypes.USER));

        AccessCertificationDecisionType dec1 = new AccessCertificationDecisionType();
        dec1.setId(1L);

        List<ItemDelta> modifications = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                .item(F_CASE, 6, F_DECISION).add(decNoId, dec200)
                .item(F_CASE, 6, F_DECISION).delete(dec1)
                .item(F_CASE, 6, F_DECISION, 2, F_RESPONSE).replace(ACCEPT)
                .asItemDeltas();

        executeAndCheckModification(modifications, result);
    }


    @Test
    public void test900DeleteCampaign() throws Exception {
        OperationResult result = new OperationResult("test900DeleteCampaign");
        repositoryService.deleteObject(AccessCertificationCampaignType.class, campaignOid, result);
        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());
    }

    protected void executeAndCheckModification(List<ItemDelta> modifications, OperationResult result) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, IOException {
        PrismObject<AccessCertificationCampaignType> before = getFullCampaign(campaignOid, result);
        List<ItemDelta> savedModifications = (List) CloneUtil.cloneCollectionMembers(modifications);

        repositoryService.modifyObject(AccessCertificationCampaignType.class, campaignOid, modifications, result);

        checkCampaign(campaignOid, result, before, savedModifications);
    }

    private void checkCampaign(String campaignOid, OperationResult result, PrismObject<AccessCertificationCampaignType> expectedObject, List<ItemDelta> modifications) throws SchemaException, ObjectNotFoundException, IOException {
        expectedObject.setOid(campaignOid);
        if (modifications != null) {
            ItemDelta.applyTo(modifications, expectedObject);
        }

        LOGGER.trace("Expected object = \n{}", expectedObject.debugDump());

        PrismObject<AccessCertificationCampaignType> campaign = getFullCampaign(campaignOid, result);

        LOGGER.trace("Actual object from repo = \n{}", campaign.debugDump());

        removeCampaignRef(expectedObject.asObjectable());
        removeCampaignRef(campaign.asObjectable());
        PrismAsserts.assertEquivalent("Campaign is not as expected", expectedObject, campaign);
    }

    private PrismObject<AccessCertificationCampaignType> getFullCampaign(String campaignOid, OperationResult result) throws ObjectNotFoundException, SchemaException {
        SelectorOptions<GetOperationOptions> retrieve = SelectorOptions.create(F_CASE, GetOperationOptions.createRetrieve(INCLUDE));
        return repositoryService.getObject(AccessCertificationCampaignType.class, campaignOid, Arrays.asList(retrieve), result);
    }

    private void removeCampaignRef(AccessCertificationCampaignType campaign) {
        for (AccessCertificationCaseType aCase : campaign.getCase()) {
            aCase.asPrismContainerValue().removeReference(F_CAMPAIGN_REF);
        }
    }

}
