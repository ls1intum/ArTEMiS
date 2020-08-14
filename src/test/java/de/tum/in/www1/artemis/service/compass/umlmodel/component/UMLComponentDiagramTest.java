package de.tum.in.www1.artemis.service.compass.umlmodel.component;

import static com.google.gson.JsonParser.parseString;
import static de.tum.in.www1.artemis.service.compass.umlmodel.component.UMLComponentRelationship.UMLComponentRelationshipType.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import de.tum.in.www1.artemis.domain.modeling.ModelingSubmission;
import de.tum.in.www1.artemis.service.compass.controller.UMLModelParser;
import de.tum.in.www1.artemis.service.compass.umlmodel.AbstractUMLDiagramTest;
import de.tum.in.www1.artemis.service.compass.umlmodel.UMLDiagram;
import de.tum.in.www1.artemis.service.compass.umlmodel.UMLElement;

public class UMLComponentDiagramTest extends AbstractUMLDiagramTest {

    private static final String componentModel1 = "{\"version\":\"2.0.0\",\"type\":\"ComponentDiagram\",\"size\":{\"width\":1020,\"height\":500},\"interactive\":{\"elements\":[],\"relationships\":[]},\"elements\":[{\"id\":\"aacfed39-684b-4248-a60b-93c74c750a56\",\"name\":\"Component\",\"type\":\"Component\",\"owner\":null,\"bounds\":{\"x\":0,\"y\":0,\"width\":360,\"height\":460}},{\"id\":\"425bbfb2-9942-4d96-83d0-0b40147611d5\",\"name\":\"AudioPlayer\",\"type\":\"Component\",\"owner\":\"aacfed39-684b-4248-a60b-93c74c750a56\",\"bounds\":{\"x\":10,\"y\":40,\"width\":160,\"height\":40}},{\"id\":\"fa272700-d8ed-4f0f-a05a-5978d6a801cc\",\"name\":\"Collision\",\"type\":\"Component\",\"owner\":\"aacfed39-684b-4248-a60b-93c74c750a56\",\"bounds\":{\"x\":180,\"y\":40,\"width\":160,\"height\":40}},{\"id\":\"beb9a966-3264-4ccb-9d9d-e2c207e7900b\",\"name\":\"GameController\",\"type\":\"Component\",\"owner\":\"aacfed39-684b-4248-a60b-93c74c750a56\",\"bounds\":{\"x\":20,\"y\":220,\"width\":190,\"height\":50}},{\"id\":\"6e5a4900-d1a5-429d-9fb1-d5587409317c\",\"name\":\"MouseSteering\",\"type\":\"Component\",\"owner\":\"aacfed39-684b-4248-a60b-93c74c750a56\",\"bounds\":{\"x\":150,\"y\":310,\"width\":190,\"height\":40}},{\"id\":\"13d5190d-6c6c-465c-b5be-af3d295df178\",\"name\":\"Music Service\",\"type\":\"ComponentInterface\",\"owner\":\"aacfed39-684b-4248-a60b-93c74c750a56\",\"bounds\":{\"x\":40,\"y\":140,\"width\":20,\"height\":20}},{\"id\":\"14c5be74-74ca-490b-a5f4-359b6e125a5e\",\"name\":\"Collision Detection Service\",\"type\":\"ComponentInterface\",\"owner\":\"aacfed39-684b-4248-a60b-93c74c750a56\",\"bounds\":{\"x\":190,\"y\":153,\"width\":20,\"height\":20}},{\"id\":\"a4a78131-2fa2-4631-8478-47f6132866a6\",\"name\":\"Game Setup\",\"type\":\"ComponentInterface\",\"owner\":\"aacfed39-684b-4248-a60b-93c74c750a56\",\"bounds\":{\"x\":80,\"y\":370,\"width\":20,\"height\":20}},{\"id\":\"2715dfbb-cd33-4a32-8b9d-486e873c5efe\",\"name\":\"Model\",\"type\":\"Component\",\"owner\":null,\"bounds\":{\"x\":420,\"y\":30,\"width\":300,\"height\":290}},{\"id\":\"46c1441e-fc7e-4346-befd-d0bacb22bba6\",\"name\":\"Car\",\"type\":\"Component\",\"owner\":\"2715dfbb-cd33-4a32-8b9d-486e873c5efe\",\"bounds\":{\"x\":540,\"y\":260,\"width\":160,\"height\":40}},{\"id\":\"38f7f83d-57e5-4531-adbc-78da6c139944\",\"name\":\"Driving Service\",\"type\":\"ComponentInterface\",\"owner\":\"2715dfbb-cd33-4a32-8b9d-486e873c5efe\",\"bounds\":{\"x\":440,\"y\":243,\"width\":20,\"height\":20}},{\"id\":\"430181d9-0a50-47a1-a7b3-f8c5ca801bbc\",\"name\":\"Position Service\",\"type\":\"ComponentInterface\",\"owner\":\"2715dfbb-cd33-4a32-8b9d-486e873c5efe\",\"bounds\":{\"x\":560,\"y\":163,\"width\":20,\"height\":20}},{\"id\":\"0de303c3-cc80-466b-9f48-374fcbf44247\",\"name\":\"View\",\"type\":\"Component\",\"owner\":null,\"bounds\":{\"x\":740,\"y\":100,\"width\":200,\"height\":180}},{\"id\":\"0088ad7a-c05a-412f-89dd-56a5a1a099a0\",\"name\":\"GameView\",\"type\":\"Component\",\"owner\":\"0de303c3-cc80-466b-9f48-374fcbf44247\",\"bounds\":{\"x\":760,\"y\":170,\"width\":160,\"height\":40}}],\"relationships\":[{\"id\":\"2456e608-3070-494d-a91c-3edc872140fa\",\"name\":\"\",\"type\":\"ComponentInterfaceProvided\",\"owner\":null,\"bounds\":{\"x\":50,\"y\":80,\"width\":40,\"height\":60},\"path\":[{\"x\":0,\"y\":60},{\"x\":0,\"y\":20},{\"x\":40,\"y\":40},{\"x\":40,\"y\":0}],\"source\":{\"direction\":\"Up\",\"element\":\"13d5190d-6c6c-465c-b5be-af3d295df178\"},\"target\":{\"direction\":\"Down\",\"element\":\"425bbfb2-9942-4d96-83d0-0b40147611d5\"}},{\"id\":\"73c0c661-d809-4422-9c8c-f04d8f614a9c\",\"name\":\"\",\"type\":\"ComponentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":50,\"y\":160,\"width\":65,\"height\":60},\"path\":[{\"x\":65,\"y\":60},{\"x\":65,\"y\":20},{\"x\":0,\"y\":40},{\"x\":0,\"y\":0}],\"source\":{\"direction\":\"Up\",\"element\":\"beb9a966-3264-4ccb-9d9d-e2c207e7900b\"},\"target\":{\"direction\":\"Down\",\"element\":\"13d5190d-6c6c-465c-b5be-af3d295df178\"}},{\"id\":\"83ab21c7-56eb-44f1-ba51-29f853605cc2\",\"name\":\"\",\"type\":\"ComponentInterfaceProvided\",\"owner\":null,\"bounds\":{\"x\":200,\"y\":80,\"width\":60,\"height\":73},\"path\":[{\"x\":0,\"y\":73},{\"x\":0,\"y\":33},{\"x\":60,\"y\":40},{\"x\":60,\"y\":0}],\"source\":{\"direction\":\"Up\",\"element\":\"14c5be74-74ca-490b-a5f4-359b6e125a5e\"},\"target\":{\"direction\":\"Down\",\"element\":\"fa272700-d8ed-4f0f-a05a-5978d6a801cc\"}},{\"id\":\"edee7194-0604-4a44-8197-54ce3c5939a9\",\"name\":\"\",\"type\":\"ComponentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":200,\"y\":173,\"width\":50,\"height\":72},\"path\":[{\"x\":10,\"y\":72},{\"x\":50,\"y\":72},{\"x\":50,\"y\":40},{\"x\":0,\"y\":40},{\"x\":0,\"y\":0}],\"source\":{\"direction\":\"Right\",\"element\":\"beb9a966-3264-4ccb-9d9d-e2c207e7900b\"},\"target\":{\"direction\":\"Down\",\"element\":\"14c5be74-74ca-490b-a5f4-359b6e125a5e\"}},{\"id\":\"454a9773-9dbd-46d3-ad61-35a0db0cf2e5\",\"name\":\"\",\"type\":\"ComponentInterfaceProvided\",\"owner\":null,\"bounds\":{\"x\":460,\"y\":253,\"width\":80,\"height\":27},\"path\":[{\"x\":80,\"y\":27},{\"x\":40,\"y\":27},{\"x\":40,\"y\":0},{\"x\":0,\"y\":0}],\"source\":{\"direction\":\"Left\",\"element\":\"46c1441e-fc7e-4346-befd-d0bacb22bba6\"},\"target\":{\"direction\":\"Right\",\"element\":\"38f7f83d-57e5-4531-adbc-78da6c139944\"}},{\"id\":\"d1c9987b-5d2e-49c5-9d9f-fd315337d44a\",\"name\":\"\",\"type\":\"ComponentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":210,\"y\":245,\"width\":230,\"height\":8},\"path\":[{\"x\":0,\"y\":0},{\"x\":115,\"y\":0},{\"x\":115,\"y\":8},{\"x\":230,\"y\":8}],\"source\":{\"direction\":\"Right\",\"element\":\"beb9a966-3264-4ccb-9d9d-e2c207e7900b\"},\"target\":{\"direction\":\"Left\",\"element\":\"38f7f83d-57e5-4531-adbc-78da6c139944\"}},{\"id\":\"0136788f-289d-4203-90ec-23f5c044f6b1\",\"name\":\"\",\"type\":\"ComponentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":245,\"y\":263,\"width\":205,\"height\":47},\"path\":[{\"x\":0,\"y\":47},{\"x\":0,\"y\":7},{\"x\":145,\"y\":7},{\"x\":145,\"y\":40},{\"x\":205,\"y\":40},{\"x\":205,\"y\":0}],\"source\":{\"direction\":\"Up\",\"element\":\"6e5a4900-d1a5-429d-9fb1-d5587409317c\"},\"target\":{\"direction\":\"Down\",\"element\":\"38f7f83d-57e5-4531-adbc-78da6c139944\"}},{\"id\":\"d513524b-8957-4761-8202-8b15bffcb2f6\",\"name\":\"\",\"type\":\"ComponentDependency\",\"owner\":null,\"bounds\":{\"x\":340,\"y\":210,\"width\":500,\"height\":120},\"path\":[{\"x\":0,\"y\":120},{\"x\":500,\"y\":120},{\"x\":500,\"y\":0}],\"source\":{\"direction\":\"Right\",\"element\":\"6e5a4900-d1a5-429d-9fb1-d5587409317c\"},\"target\":{\"direction\":\"Down\",\"element\":\"0088ad7a-c05a-412f-89dd-56a5a1a099a0\"}},{\"id\":\"da7207b5-ddf7-47ca-b273-cc47ef7eb953\",\"name\":\"\",\"type\":\"ComponentInterfaceProvided\",\"owner\":null,\"bounds\":{\"x\":90,\"y\":270,\"width\":25,\"height\":100},\"path\":[{\"x\":0,\"y\":100},{\"x\":0,\"y\":50},{\"x\":25,\"y\":50},{\"x\":25,\"y\":0}],\"source\":{\"direction\":\"Up\",\"element\":\"a4a78131-2fa2-4631-8478-47f6132866a6\"},\"target\":{\"direction\":\"Down\",\"element\":\"beb9a966-3264-4ccb-9d9d-e2c207e7900b\"}},{\"id\":\"17f48a08-8b82-4ded-b9f6-2e4516fe3f60\",\"name\":\"\",\"type\":\"ComponentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":100,\"y\":190,\"width\":860,\"height\":190},\"path\":[{\"x\":820,\"y\":0},{\"x\":860,\"y\":0},{\"x\":860,\"y\":190},{\"x\":0,\"y\":190}],\"source\":{\"direction\":\"Right\",\"element\":\"0088ad7a-c05a-412f-89dd-56a5a1a099a0\"},\"target\":{\"direction\":\"Right\",\"element\":\"a4a78131-2fa2-4631-8478-47f6132866a6\"}},{\"id\":\"9d6c3cc0-e8d8-4dc1-97cf-07c0ab7cba2e\",\"name\":\"\",\"type\":\"ComponentInterfaceProvided\",\"owner\":null,\"bounds\":{\"x\":570,\"y\":183,\"width\":50,\"height\":77},\"path\":[{\"x\":0,\"y\":0},{\"x\":0,\"y\":40},{\"x\":50,\"y\":37},{\"x\":50,\"y\":77}],\"source\":{\"direction\":\"Down\",\"element\":\"430181d9-0a50-47a1-a7b3-f8c5ca801bbc\"},\"target\":{\"direction\":\"Up\",\"element\":\"46c1441e-fc7e-4346-befd-d0bacb22bba6\"}},{\"id\":\"528f9420-3d65-4564-b593-c787a87a6c9a\",\"name\":\"\",\"type\":\"ComponentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":580,\"y\":173,\"width\":180,\"height\":17},\"path\":[{\"x\":180,\"y\":17},{\"x\":90,\"y\":17},{\"x\":90,\"y\":0},{\"x\":0,\"y\":0}],\"source\":{\"direction\":\"Left\",\"element\":\"0088ad7a-c05a-412f-89dd-56a5a1a099a0\"},\"target\":{\"direction\":\"Right\",\"element\":\"430181d9-0a50-47a1-a7b3-f8c5ca801bbc\"}},{\"id\":\"c1a73e1c-f04e-4f95-a8c5-e7e110667986\",\"name\":\"\",\"type\":\"ComponentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":340,\"y\":60,\"width\":230,\"height\":103},\"path\":[{\"x\":0,\"y\":0},{\"x\":230,\"y\":0},{\"x\":230,\"y\":103}],\"source\":{\"direction\":\"Right\",\"element\":\"fa272700-d8ed-4f0f-a05a-5978d6a801cc\"},\"target\":{\"direction\":\"Up\",\"element\":\"430181d9-0a50-47a1-a7b3-f8c5ca801bbc\"}}],\"assessments\":[]}";

    private static final String componentModel2 = "{\"version\":\"2.0.0\",\"type\":\"ComponentDiagram\",\"size\":{\"width\":1860,\"height\":660},\"interactive\":{\"elements\":[],\"relationships\":[]},\"elements\":[{\"id\":\"51ba313e-a5d0-46b6-8d4f-377259960275\",\"name\":\"View\",\"type\":\"Component\",\"owner\":null,\"bounds\":{\"x\":1360,\"y\":0,\"width\":380,\"height\":280}},{\"id\":\"1016dcfd-67b7-4c8b-9fad-94452a261fae\",\"name\":\"GameView\",\"type\":\"Component\",\"owner\":\"51ba313e-a5d0-46b6-8d4f-377259960275\",\"bounds\":{\"x\":1450,\"y\":119,\"width\":200,\"height\":100}},{\"id\":\"04f0a555-9af1-4b83-b4f7-a8e82417dee0\",\"name\":\"Model\",\"type\":\"Component\",\"owner\":null,\"bounds\":{\"x\":700,\"y\":10,\"width\":600,\"height\":560}},{\"id\":\"a2574389-9ca2-457e-846d-0149706b4af6\",\"name\":\"Driving Service\",\"type\":\"ComponentInterface\",\"owner\":\"04f0a555-9af1-4b83-b4f7-a8e82417dee0\",\"bounds\":{\"x\":820,\"y\":310,\"width\":20,\"height\":20}},{\"id\":\"32c3620c-4340-4f22-8dc4-fd6678c4576c\",\"name\":\"Car\",\"type\":\"Component\",\"owner\":\"04f0a555-9af1-4b83-b4f7-a8e82417dee0\",\"bounds\":{\"x\":1010,\"y\":302,\"width\":160,\"height\":40}},{\"id\":\"acc92bda-1378-4c27-9000-101929eb9d59\",\"name\":\"Position Service\",\"type\":\"ComponentInterface\",\"owner\":\"04f0a555-9af1-4b83-b4f7-a8e82417dee0\",\"bounds\":{\"x\":1080,\"y\":160,\"width\":20,\"height\":20}},{\"id\":\"da4b78da-8871-42a7-9bd3-ee9b4a4f4db5\",\"name\":\"Controller\",\"type\":\"Component\",\"owner\":null,\"bounds\":{\"x\":0,\"y\":0,\"width\":600,\"height\":570}},{\"id\":\"d713af95-f5ba-400c-90b0-32cd2899eef9\",\"name\":\"Mousesteering\",\"type\":\"Component\",\"owner\":\"da4b78da-8871-42a7-9bd3-ee9b4a4f4db5\",\"bounds\":{\"x\":290,\"y\":350,\"width\":200,\"height\":100}},{\"id\":\"53b61479-14ab-49dd-af0b-c73dd1b3c0b4\",\"name\":\"GameBoard\",\"type\":\"Component\",\"owner\":\"da4b78da-8871-42a7-9bd3-ee9b4a4f4db5\",\"bounds\":{\"x\":30,\"y\":270,\"width\":200,\"height\":100}},{\"id\":\"314f5494-0eee-450d-a242-764c84f96cda\",\"name\":\"AudioPlayer\",\"type\":\"Component\",\"owner\":\"da4b78da-8871-42a7-9bd3-ee9b4a4f4db5\",\"bounds\":{\"x\":30,\"y\":20,\"width\":200,\"height\":100}},{\"id\":\"1154e431-36dc-40fa-9162-072fa5e7ff87\",\"name\":\"Music Service\",\"type\":\"ComponentInterface\",\"owner\":\"da4b78da-8871-42a7-9bd3-ee9b4a4f4db5\",\"bounds\":{\"x\":120,\"y\":160,\"width\":20,\"height\":20}},{\"id\":\"e6ab93ad-94f3-4ea7-ad98-efb4799a3e5c\",\"name\":\"Collision\",\"type\":\"Component\",\"owner\":\"da4b78da-8871-42a7-9bd3-ee9b4a4f4db5\",\"bounds\":{\"x\":340,\"y\":40,\"width\":200,\"height\":100}},{\"id\":\"ab02bf60-0ca9-46d3-bc5b-6f46a19306df\",\"name\":\"Collision Detection Service\",\"type\":\"ComponentInterface\",\"owner\":\"da4b78da-8871-42a7-9bd3-ee9b4a4f4db5\",\"bounds\":{\"x\":430,\"y\":210,\"width\":20,\"height\":20}},{\"id\":\"b6c62b90-3bf0-4219-801e-a3191d6d3434\",\"name\":\"Game Setup\",\"type\":\"ComponentInterface\",\"owner\":\"da4b78da-8871-42a7-9bd3-ee9b4a4f4db5\",\"bounds\":{\"x\":120,\"y\":479,\"width\":20,\"height\":20}}],\"relationships\":[{\"id\":\"f2ca2746-1d6d-4caf-bca8-41c001736936\",\"name\":\"\",\"type\":\"ComponentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":490,\"y\":330,\"width\":340,\"height\":70},\"path\":[{\"x\":0,\"y\":70},{\"x\":340,\"y\":70},{\"x\":340,\"y\":0}],\"source\":{\"direction\":\"Right\",\"element\":\"d713af95-f5ba-400c-90b0-32cd2899eef9\"},\"target\":{\"direction\":\"Down\",\"element\":\"a2574389-9ca2-457e-846d-0149706b4af6\"}},{\"id\":\"46751f1b-7e9b-40b7-8ed5-e206322868c7\",\"name\":\"\",\"type\":\"ComponentInterfaceProvided\",\"owner\":null,\"bounds\":{\"x\":130,\"y\":120,\"width\":1,\"height\":40},\"path\":[{\"x\":0,\"y\":0},{\"x\":0,\"y\":40},{\"x\":0,\"y\":0},{\"x\":0,\"y\":40}],\"source\":{\"direction\":\"Down\",\"element\":\"314f5494-0eee-450d-a242-764c84f96cda\"},\"target\":{\"direction\":\"Up\",\"element\":\"1154e431-36dc-40fa-9162-072fa5e7ff87\"}},{\"id\":\"5b0e39db-dd7f-4268-b95d-68507bf5eccb\",\"name\":\"\",\"type\":\"ComponentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":130,\"y\":180,\"width\":1,\"height\":90},\"path\":[{\"x\":0,\"y\":90},{\"x\":0,\"y\":0}],\"source\":{\"direction\":\"Up\",\"element\":\"53b61479-14ab-49dd-af0b-c73dd1b3c0b4\"},\"target\":{\"direction\":\"Down\",\"element\":\"1154e431-36dc-40fa-9162-072fa5e7ff87\"}},{\"id\":\"a12eadea-8b91-4c2b-b78f-6949ec85fd89\",\"name\":\"\",\"type\":\"ComponentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":230,\"y\":230,\"width\":210,\"height\":90},\"path\":[{\"x\":0,\"y\":90},{\"x\":210,\"y\":90},{\"x\":210,\"y\":0}],\"source\":{\"direction\":\"Right\",\"element\":\"53b61479-14ab-49dd-af0b-c73dd1b3c0b4\"},\"target\":{\"direction\":\"Down\",\"element\":\"ab02bf60-0ca9-46d3-bc5b-6f46a19306df\"}},{\"id\":\"f84e1970-3414-4cb5-99a7-3c4cc18bc3b8\",\"name\":\"\",\"type\":\"ComponentInterfaceProvided\",\"owner\":null,\"bounds\":{\"x\":440,\"y\":140,\"width\":1,\"height\":70},\"path\":[{\"x\":0,\"y\":0},{\"x\":0,\"y\":40},{\"x\":0,\"y\":30},{\"x\":0,\"y\":70}],\"source\":{\"direction\":\"Down\",\"element\":\"e6ab93ad-94f3-4ea7-ad98-efb4799a3e5c\"},\"target\":{\"direction\":\"Up\",\"element\":\"ab02bf60-0ca9-46d3-bc5b-6f46a19306df\"}},{\"id\":\"d7679b5a-141b-4c23-a5be-6cc2a147bb98\",\"name\":\"\",\"type\":\"ComponentInterfaceProvided\",\"owner\":null,\"bounds\":{\"x\":130,\"y\":370,\"width\":1,\"height\":109},\"path\":[{\"x\":0,\"y\":109},{\"x\":0,\"y\":0}],\"source\":{\"direction\":\"Up\",\"element\":\"b6c62b90-3bf0-4219-801e-a3191d6d3434\"},\"target\":{\"direction\":\"Down\",\"element\":\"53b61479-14ab-49dd-af0b-c73dd1b3c0b4\"}},{\"id\":\"aa660bd3-7f48-4f14-8895-999e472e2984\",\"name\":\"\",\"type\":\"ComponentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":130,\"y\":169,\"width\":1560,\"height\":370},\"path\":[{\"x\":1520,\"y\":0},{\"x\":1560,\"y\":0},{\"x\":1560,\"y\":370},{\"x\":0,\"y\":370},{\"x\":0,\"y\":330}],\"source\":{\"direction\":\"Right\",\"element\":\"1016dcfd-67b7-4c8b-9fad-94452a261fae\"},\"target\":{\"direction\":\"Down\",\"element\":\"b6c62b90-3bf0-4219-801e-a3191d6d3434\"}},{\"id\":\"63004c15-7edf-4fb2-be04-d2fcfa00d95c\",\"name\":\"\",\"type\":\"ComponentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":230,\"y\":320,\"width\":590,\"height\":1},\"path\":[{\"x\":0,\"y\":0},{\"x\":590,\"y\":0}],\"source\":{\"direction\":\"Right\",\"element\":\"53b61479-14ab-49dd-af0b-c73dd1b3c0b4\"},\"target\":{\"direction\":\"Left\",\"element\":\"a2574389-9ca2-457e-846d-0149706b4af6\"}},{\"id\":\"94d3aace-82de-440b-b936-9d939d77ec73\",\"name\":\"\",\"type\":\"ComponentInterfaceProvided\",\"owner\":null,\"bounds\":{\"x\":840,\"y\":320,\"width\":170,\"height\":2},\"path\":[{\"x\":0,\"y\":0},{\"x\":85,\"y\":0},{\"x\":85,\"y\":2},{\"x\":170,\"y\":2}],\"source\":{\"direction\":\"Right\",\"element\":\"a2574389-9ca2-457e-846d-0149706b4af6\"},\"target\":{\"direction\":\"Left\",\"element\":\"32c3620c-4340-4f22-8dc4-fd6678c4576c\"}},{\"id\":\"1bf5cb12-5a18-4c27-8343-8d4823136ae7\",\"name\":\"\",\"type\":\"ComponentInterfaceProvided\",\"owner\":null,\"bounds\":{\"x\":1090,\"y\":180,\"width\":1,\"height\":122},\"path\":[{\"x\":0,\"y\":0},{\"x\":0,\"y\":122}],\"source\":{\"direction\":\"Down\",\"element\":\"acc92bda-1378-4c27-9000-101929eb9d59\"},\"target\":{\"direction\":\"Up\",\"element\":\"32c3620c-4340-4f22-8dc4-fd6678c4576c\"}},{\"id\":\"7f47fc15-7c02-46e8-b534-5885c42222c3\",\"name\":\"\",\"type\":\"ComponentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":1100,\"y\":169,\"width\":350,\"height\":1},\"path\":[{\"x\":350,\"y\":0},{\"x\":175,\"y\":0},{\"x\":175,\"y\":1},{\"x\":0,\"y\":1}],\"source\":{\"direction\":\"Left\",\"element\":\"1016dcfd-67b7-4c8b-9fad-94452a261fae\"},\"target\":{\"direction\":\"Right\",\"element\":\"acc92bda-1378-4c27-9000-101929eb9d59\"}},{\"id\":\"b1ad2e90-8bdb-432b-8492-857af6aacacb\",\"name\":\"\",\"type\":\"ComponentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":540,\"y\":90,\"width\":550,\"height\":70},\"path\":[{\"x\":0,\"y\":0},{\"x\":550,\"y\":0},{\"x\":550,\"y\":70}],\"source\":{\"direction\":\"Right\",\"element\":\"e6ab93ad-94f3-4ea7-ad98-efb4799a3e5c\"},\"target\":{\"direction\":\"Up\",\"element\":\"acc92bda-1378-4c27-9000-101929eb9d59\"}},{\"id\":\"8e682b93-aa01-4f5c-9c16-a98f9affd009\",\"name\":\"\",\"type\":\"ComponentDependency\",\"owner\":null,\"bounds\":{\"x\":390,\"y\":219,\"width\":1160,\"height\":271},\"path\":[{\"x\":0,\"y\":231},{\"x\":0,\"y\":271},{\"x\":1160,\"y\":271},{\"x\":1160,\"y\":0}],\"source\":{\"direction\":\"Down\",\"element\":\"d713af95-f5ba-400c-90b0-32cd2899eef9\"},\"target\":{\"direction\":\"Down\",\"element\":\"1016dcfd-67b7-4c8b-9fad-94452a261fae\"}}],\"assessments\":[]}";

    private static final String componentModel3 = "{\"version\":\"2.0.0\",\"type\":\"ComponentDiagram\",\"size\":{\"width\":1160,\"height\":720},\"interactive\":{\"elements\":[],\"relationships\":[]},\"elements\":[{\"id\":\"77282dd6-9b02-4cf8-81f3-90f4ed76d3f8\",\"name\":\"A\",\"type\":\"Component\",\"owner\":null,\"bounds\":{\"x\":170,\"y\":40,\"width\":800,\"height\":460}},{\"id\":\"0db29365-aeb9-4ff3-8b46-2bbabdf0ef93\",\"name\":\"B\",\"type\":\"Component\",\"owner\":\"77282dd6-9b02-4cf8-81f3-90f4ed76d3f8\",\"bounds\":{\"x\":280,\"y\":140,\"width\":580,\"height\":270}},{\"id\":\"acc139d3-78b3-455c-85f2-735a19ff3cf0\",\"name\":\"C\",\"type\":\"Component\",\"owner\":\"0db29365-aeb9-4ff3-8b46-2bbabdf0ef93\",\"bounds\":{\"x\":510,\"y\":230,\"width\":310,\"height\":120}},{\"id\":\"fccb5123-7a35-4367-95c8-75beb9cdaa8a\",\"name\":\"I4\",\"type\":\"ComponentInterface\",\"owner\":\"acc139d3-78b3-455c-85f2-735a19ff3cf0\",\"bounds\":{\"x\":560,\"y\":290,\"width\":20,\"height\":20}},{\"id\":\"38fbf718-6029-4590-a9f0-88d5e78df22d\",\"name\":\"I3\",\"type\":\"ComponentInterface\",\"owner\":\"0db29365-aeb9-4ff3-8b46-2bbabdf0ef93\",\"bounds\":{\"x\":360,\"y\":230,\"width\":20,\"height\":20}},{\"id\":\"dbc7f018-2618-49f5-a2d9-3e664ef85f59\",\"name\":\"I2\",\"type\":\"ComponentInterface\",\"owner\":\"77282dd6-9b02-4cf8-81f3-90f4ed76d3f8\",\"bounds\":{\"x\":220,\"y\":110,\"width\":20,\"height\":20}},{\"id\":\"5fddf922-19db-46cd-8dfe-668d4fe08b0e\",\"name\":\"I1\",\"type\":\"ComponentInterface\",\"owner\":null,\"bounds\":{\"x\":10,\"y\":110,\"width\":20,\"height\":20}},{\"id\":\"cd8618b2-1ab2-4b5e-960d-714ed6e4f935\",\"name\":\"D\",\"type\":\"Component\",\"owner\":null,\"bounds\":{\"x\":240,\"y\":550,\"width\":200,\"height\":100}},{\"id\":\"5edf0b9a-3a7d-4975-b379-e2bfeda8bcfa\",\"name\":\"I5\",\"type\":\"ComponentInterface\",\"owner\":null,\"bounds\":{\"x\":0,\"y\":560,\"width\":20,\"height\":20}}],\"relationships\":[{\"id\":\"5c7972de-aa60-4f25-896b-b85190e7f377\",\"name\":\"\",\"type\":\"ComponentInterfaceProvided\",\"owner\":null,\"bounds\":{\"x\":30,\"y\":120,\"width\":190,\"height\":1},\"path\":[{\"x\":0,\"y\":0},{\"x\":190,\"y\":0}],\"source\":{\"direction\":\"Right\",\"element\":\"5fddf922-19db-46cd-8dfe-668d4fe08b0e\"},\"target\":{\"direction\":\"Left\",\"element\":\"dbc7f018-2618-49f5-a2d9-3e664ef85f59\"}},{\"id\":\"e1345c34-909c-49a1-8f0d-02159a431587\",\"name\":\"\",\"type\":\"ComponentInterfaceProvided\",\"owner\":null,\"bounds\":{\"x\":380,\"y\":240,\"width\":130,\"height\":50},\"path\":[{\"x\":130,\"y\":50},{\"x\":65,\"y\":50},{\"x\":65,\"y\":0},{\"x\":0,\"y\":0}],\"source\":{\"direction\":\"Left\",\"element\":\"acc139d3-78b3-455c-85f2-735a19ff3cf0\"},\"target\":{\"direction\":\"Right\",\"element\":\"38fbf718-6029-4590-a9f0-88d5e78df22d\"}},{\"id\":\"ae76c1e0-201f-47cc-968a-d37ef314ed8b\",\"name\":\"\",\"type\":\"ComponentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":240,\"y\":240,\"width\":120,\"height\":35},\"path\":[{\"x\":40,\"y\":35},{\"x\":0,\"y\":35},{\"x\":80,\"y\":0},{\"x\":120,\"y\":0}],\"source\":{\"direction\":\"Left\",\"element\":\"0db29365-aeb9-4ff3-8b46-2bbabdf0ef93\"},\"target\":{\"direction\":\"Left\",\"element\":\"38fbf718-6029-4590-a9f0-88d5e78df22d\"}},{\"id\":\"dddfa7e8-e8a8-435c-b7f7-1e199c4816fd\",\"name\":\"\",\"type\":\"ComponentDependency\",\"owner\":null,\"bounds\":{\"x\":570,\"y\":0,\"width\":440,\"height\":270},\"path\":[{\"x\":400,\"y\":270},{\"x\":440,\"y\":270},{\"x\":440,\"y\":0},{\"x\":0,\"y\":0},{\"x\":0,\"y\":40}],\"source\":{\"direction\":\"Right\",\"element\":\"77282dd6-9b02-4cf8-81f3-90f4ed76d3f8\"},\"target\":{\"direction\":\"Up\",\"element\":\"77282dd6-9b02-4cf8-81f3-90f4ed76d3f8\"}},{\"id\":\"06b6adb0-4d84-4b92-90d8-98b889f62277\",\"name\":\"\",\"type\":\"ComponentDependency\",\"owner\":null,\"bounds\":{\"x\":340,\"y\":350,\"width\":325,\"height\":200},\"path\":[{\"x\":0,\"y\":200},{\"x\":0,\"y\":100},{\"x\":325,\"y\":100},{\"x\":325,\"y\":0}],\"source\":{\"direction\":\"Up\",\"element\":\"cd8618b2-1ab2-4b5e-960d-714ed6e4f935\"},\"target\":{\"direction\":\"Down\",\"element\":\"acc139d3-78b3-455c-85f2-735a19ff3cf0\"}},{\"id\":\"8476df26-8f57-49e1-b3b3-0ea122afad39\",\"name\":\"\",\"type\":\"ComponentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":440,\"y\":500,\"width\":130,\"height\":100},\"path\":[{\"x\":130,\"y\":0},{\"x\":130,\"y\":100},{\"x\":0,\"y\":100}],\"source\":{\"direction\":\"Down\",\"element\":\"77282dd6-9b02-4cf8-81f3-90f4ed76d3f8\"},\"target\":{\"direction\":\"Right\",\"element\":\"cd8618b2-1ab2-4b5e-960d-714ed6e4f935\"}},{\"id\":\"0e4e1075-5e58-44ef-8bbe-76229de3e79d\",\"name\":\"\",\"type\":\"ComponentInterfaceProvided\",\"owner\":null,\"bounds\":{\"x\":10,\"y\":270,\"width\":160,\"height\":290},\"path\":[{\"x\":160,\"y\":0},{\"x\":0,\"y\":0},{\"x\":0,\"y\":290}],\"source\":{\"direction\":\"Left\",\"element\":\"77282dd6-9b02-4cf8-81f3-90f4ed76d3f8\"},\"target\":{\"direction\":\"Up\",\"element\":\"5edf0b9a-3a7d-4975-b379-e2bfeda8bcfa\"}},{\"id\":\"a8dd80b9-b204-4d21-8dfa-b3b0de0b694a\",\"name\":\"\",\"type\":\"ComponentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":20,\"y\":570,\"width\":220,\"height\":30},\"path\":[{\"x\":220,\"y\":30},{\"x\":110,\"y\":30},{\"x\":110,\"y\":0},{\"x\":0,\"y\":0}],\"source\":{\"direction\":\"Left\",\"element\":\"cd8618b2-1ab2-4b5e-960d-714ed6e4f935\"},\"target\":{\"direction\":\"Right\",\"element\":\"5edf0b9a-3a7d-4975-b379-e2bfeda8bcfa\"}}],\"assessments\":[]}";

    @Test
    void similarityComponentDiagram_EqualModels() {
        compareSubmissions(new ModelingSubmission().model(componentModel1), new ModelingSubmission().model(componentModel1), 0.8, 1.0);
        compareSubmissions(new ModelingSubmission().model(componentModel2), new ModelingSubmission().model(componentModel2), 0.8, 1.0);
        compareSubmissions(new ModelingSubmission().model(componentModel3), new ModelingSubmission().model(componentModel3), 0.8, 1.0);
    }

    @Test
    void similarityComponentDiagram_SimilarModels() {
        compareSubmissions(new ModelingSubmission().model(componentModel1), new ModelingSubmission().model(componentModel2), 0.0, 0.6425);
    }

    @Test
    void similarityComponentDiagram_DifferentModels() {
        compareSubmissions(new ModelingSubmission().model(componentModel1), new ModelingSubmission().model(componentModel3), 0.0, 0.1335);
    }

    @Test
    void parseComponentDiagramModelCorrectly() throws IOException {
        UMLDiagram diagram = UMLModelParser.buildModelFromJSON(parseString(componentModel3).getAsJsonObject(), 1L);
        assertThat(diagram).isInstanceOf(UMLComponentDiagram.class);
        UMLComponentDiagram componentDiagram = (UMLComponentDiagram) diagram;
        // 4 Components A, B, C and D
        assertThat(componentDiagram.getComponentList()).hasSize(4);
        UMLComponent componentA = getComponent(componentDiagram, "A");
        UMLComponent componentB = getComponent(componentDiagram, "B");
        UMLComponent componentC = getComponent(componentDiagram, "C");
        UMLComponent componentD = getComponent(componentDiagram, "D");
        // 5 Interfaces: I1, I2, I3, I4, I5
        assertThat(componentDiagram.getComponentInterfaceList()).hasSize(5);
        UMLComponentInterface interfaceI1 = getInterface(componentDiagram, "I1");
        UMLComponentInterface interfaceI2 = getInterface(componentDiagram, "I2");
        UMLComponentInterface interfaceI3 = getInterface(componentDiagram, "I3");
        UMLComponentInterface interfaceI4 = getInterface(componentDiagram, "I4");
        UMLComponentInterface interfaceI5 = getInterface(componentDiagram, "I5");

        // 8 relationships: 3 ComponentInterfaceProvided, 3 ComponentInterfaceRequired, 2 Dependencies
        assertThat(componentDiagram.getComponentRelationshipList()).hasSize(8);
        UMLComponentRelationship relationship1 = getRelationship(componentDiagram, componentA, componentA);
        UMLComponentRelationship relationship2 = getRelationship(componentDiagram, interfaceI1, interfaceI2);
        UMLComponentRelationship relationship3 = getRelationship(componentDiagram, componentA, interfaceI5);
        UMLComponentRelationship relationship4 = getRelationship(componentDiagram, componentD, interfaceI5);
        UMLComponentRelationship relationship5 = getRelationship(componentDiagram, componentD, componentC);
        UMLComponentRelationship relationship6 = getRelationship(componentDiagram, componentD, componentA);
        UMLComponentRelationship relationship7 = getRelationship(componentDiagram, componentB, interfaceI3);
        UMLComponentRelationship relationship8 = getRelationship(componentDiagram, componentC, interfaceI3);

        assertThat(relationship1.getRelationshipType()).isEqualByComparingTo(COMPONENT_DEPENDENCY);
        assertThat(relationship2.getRelationshipType()).isEqualByComparingTo(COMPONENT_INTERFACE_PROVIDED);
        assertThat(relationship3.getRelationshipType()).isEqualByComparingTo(COMPONENT_INTERFACE_PROVIDED);
        assertThat(relationship4.getRelationshipType()).isEqualByComparingTo(COMPONENT_INTERFACE_REQUIRED);
        assertThat(relationship5.getRelationshipType()).isEqualByComparingTo(COMPONENT_DEPENDENCY);
        assertThat(relationship6.getRelationshipType()).isEqualByComparingTo(COMPONENT_INTERFACE_REQUIRED);
        assertThat(relationship7.getRelationshipType()).isEqualByComparingTo(COMPONENT_INTERFACE_REQUIRED);
        assertThat(relationship8.getRelationshipType()).isEqualByComparingTo(COMPONENT_INTERFACE_PROVIDED);

        // check owner relationships
        assertThat(componentA.getParentElement()).isNull();
        assertThat(componentB.getParentElement()).isEqualTo(componentA);
        assertThat(componentC.getParentElement()).isEqualTo(componentB);
        assertThat(componentD.getParentElement()).isNull();

        assertThat(interfaceI1.getParentElement()).isNull();
        assertThat(interfaceI2.getParentElement()).isEqualTo(componentA);
        assertThat(interfaceI3.getParentElement()).isEqualTo(componentB);
        assertThat(interfaceI4.getParentElement()).isEqualTo(componentC);
        assertThat(interfaceI5.getParentElement()).isNull();
    }

    private UMLComponent getComponent(UMLComponentDiagram componentDiagram, String name) {
        return componentDiagram.getComponentList().stream().filter(component -> component.getName().equals(name)).findFirst().get();
    }

    private UMLComponentInterface getInterface(UMLComponentDiagram componentDiagram, String name) {
        return componentDiagram.getComponentInterfaceList().stream().filter(componentInterface -> componentInterface.getName().equals(name)).findFirst().get();
    }

    private UMLComponentRelationship getRelationship(UMLComponentDiagram componentDiagram, UMLElement source, UMLElement target) {
        // Source and target do not really matter in this test so we can also check the other way round
        return componentDiagram.getComponentRelationshipList().stream().filter(relationship -> (relationship.getSource().equals(source) && relationship.getTarget().equals(target))
                || (relationship.getSource().equals(target) && relationship.getTarget().equals(source))).findFirst().get();
    }
}
