package de.tum.in.www1.artemis.service.compass.umlmodel.deployment;

import static com.google.gson.JsonParser.parseString;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import de.tum.in.www1.artemis.domain.modeling.ModelingSubmission;
import de.tum.in.www1.artemis.service.compass.controller.UMLModelParser;
import de.tum.in.www1.artemis.service.compass.umlmodel.AbstractUMLDiagramTest;
import de.tum.in.www1.artemis.service.compass.umlmodel.UMLDiagram;
import de.tum.in.www1.artemis.service.compass.umlmodel.component.UMLComponent;

public class UMLDeploymentDiagramTest extends AbstractUMLDiagramTest {

    private static final String deploymentModel1 = "{\"version\":\"2.0.0\",\"type\":\"DeploymentDiagram\",\"size\":{\"width\":1280,\"height\":580},\"interactive\":{\"elements\":[],\"relationships\":[]},\"elements\":[{\"id\":\"57d932fb-e2bd-4c60-9195-7beab7e8f72a\",\"name\":\"UserPC\",\"type\":\"DeploymentNode\",\"owner\":null,\"bounds\":{\"x\":0,\"y\":0,\"width\":1280,\"height\":430},\"stereotype\":\"node\"},{\"id\":\"a66e1617-7875-428d-a878-924bf55dd1b6\",\"name\":\"Controller\",\"type\":\"Component\",\"owner\":\"57d932fb-e2bd-4c60-9195-7beab7e8f72a\",\"bounds\":{\"x\":20,\"y\":32.800025939941406,\"width\":300,\"height\":290}},{\"id\":\"ef0e78cb-f09a-4ae9-983f-b7f6c0a1fc97\",\"name\":\"KeyboardSteering\",\"type\":\"Component\",\"owner\":\"a66e1617-7875-428d-a878-924bf55dd1b6\",\"bounds\":{\"x\":60,\"y\":102.8000259399414,\"width\":210,\"height\":40}},{\"id\":\"7109a608-cc4b-454c-be1e-846023d46f4c\",\"name\":\"Collision\",\"type\":\"Component\",\"owner\":\"a66e1617-7875-428d-a878-924bf55dd1b6\",\"bounds\":{\"x\":60,\"y\":242.8000259399414,\"width\":190,\"height\":40}},{\"id\":\"ba4c05f0-203e-4a0a-88ac-f0db3d4bedf8\",\"name\":\"Model\",\"type\":\"Component\",\"owner\":\"57d932fb-e2bd-4c60-9195-7beab7e8f72a\",\"bounds\":{\"x\":490,\"y\":52.800025939941406,\"width\":540,\"height\":330}},{\"id\":\"f2b3de7c-cadb-407e-829a-c4d9883a4139\",\"name\":\"Spacecraft\",\"type\":\"Component\",\"owner\":\"ba4c05f0-203e-4a0a-88ac-f0db3d4bedf8\",\"bounds\":{\"x\":550,\"y\":102.8000259399414,\"width\":200,\"height\":40}},{\"id\":\"b9ebb783-8cde-4a34-bbaf-7a146cf7347d\",\"name\":\"Debris\",\"type\":\"Component\",\"owner\":\"ba4c05f0-203e-4a0a-88ac-f0db3d4bedf8\",\"bounds\":{\"x\":540,\"y\":242.8000259399414,\"width\":200,\"height\":40}},{\"id\":\"c4a70c29-18d1-4baf-8d49-7ac2c30aeec2\",\"name\":\"Obstacle\",\"type\":\"Component\",\"owner\":\"ba4c05f0-203e-4a0a-88ac-f0db3d4bedf8\",\"bounds\":{\"x\":540,\"y\":322.8000259399414,\"width\":200,\"height\":40}},{\"id\":\"d868c517-0287-40bb-916d-b1bc1328e4f0\",\"name\":\"Space\",\"type\":\"Component\",\"owner\":\"ba4c05f0-203e-4a0a-88ac-f0db3d4bedf8\",\"bounds\":{\"x\":900,\"y\":262.8000259399414,\"width\":110,\"height\":40}},{\"id\":\"6637f723-d4d8-48ac-8022-cb322939898a\",\"name\":\"UI Position Service\",\"type\":\"DeploymentInterface\",\"owner\":\"ba4c05f0-203e-4a0a-88ac-f0db3d4bedf8\",\"bounds\":{\"x\":940,\"y\":154.79998779296875,\"width\":20,\"height\":20}},{\"id\":\"bbbe5445-50c5-4923-8026-294bfc81c3c2\",\"name\":\"Spacecraft Position Service\",\"type\":\"DeploymentInterface\",\"owner\":\"ba4c05f0-203e-4a0a-88ac-f0db3d4bedf8\",\"bounds\":{\"x\":800,\"y\":107.60000610351562,\"width\":20,\"height\":20}},{\"id\":\"1e891366-a0ad-4438-8fec-b36ff3e777c8\",\"name\":\"Debris Position Service\",\"type\":\"DeploymentInterface\",\"owner\":\"ba4c05f0-203e-4a0a-88ac-f0db3d4bedf8\",\"bounds\":{\"x\":800,\"y\":257.6000061035156,\"width\":20,\"height\":20}},{\"id\":\"c1065419-b9ff-4cdf-bc1b-623f443eae51\",\"name\":\"Obstacle Position Service\",\"type\":\"DeploymentInterface\",\"owner\":\"ba4c05f0-203e-4a0a-88ac-f0db3d4bedf8\",\"bounds\":{\"x\":800,\"y\":337.6000061035156,\"width\":20,\"height\":20}},{\"id\":\"214cf566-8aac-4df3-9a6d-62084152993b\",\"name\":\"Steering Service\",\"type\":\"DeploymentInterface\",\"owner\":\"57d932fb-e2bd-4c60-9195-7beab7e8f72a\",\"bounds\":{\"x\":490,\"y\":112.8000259399414,\"width\":20,\"height\":20}},{\"id\":\"bb632c63-3ef2-43a7-ac43-a829083ddcb7\",\"name\":\"Collision Evaluation Service\",\"type\":\"DeploymentInterface\",\"owner\":\"57d932fb-e2bd-4c60-9195-7beab7e8f72a\",\"bounds\":{\"x\":290,\"y\":252.8000259399414,\"width\":20,\"height\":20}},{\"id\":\"88ca0b77-a47a-416a-98c7-6a5be0b15a0c\",\"name\":\"View\",\"type\":\"Component\",\"owner\":null,\"bounds\":{\"x\":1050,\"y\":162.8000259399414,\"width\":200,\"height\":150}},{\"id\":\"63aa80f9-d736-45f1-aca7-a65b5f66d69e\",\"name\":\"SpaceUI\",\"type\":\"Component\",\"owner\":\"88ca0b77-a47a-416a-98c7-6a5be0b15a0c\",\"bounds\":{\"x\":1080,\"y\":224.79998779296875,\"width\":140,\"height\":40}}],\"relationships\":[{\"id\":\"7b815aa0-6877-4d6e-be25-8935bb99f88a\",\"name\":\"\",\"type\":\"DeploymentInterfaceProvided\",\"owner\":null,\"bounds\":{\"x\":250,\"y\":262.8000259399414,\"width\":40,\"height\":1},\"path\":[{\"x\":0,\"y\":0},{\"x\":40,\"y\":0},{\"x\":0,\"y\":0},{\"x\":40,\"y\":0}],\"source\":{\"direction\":\"Right\",\"element\":\"7109a608-cc4b-454c-be1e-846023d46f4c\"},\"target\":{\"direction\":\"Left\",\"element\":\"bb632c63-3ef2-43a7-ac43-a829083ddcb7\"}},{\"id\":\"366f2a93-27f6-47d6-aa10-75912411d72f\",\"name\":\"\",\"type\":\"DeploymentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":270,\"y\":122.8000259399414,\"width\":220,\"height\":1},\"path\":[{\"x\":0,\"y\":0},{\"x\":220,\"y\":0}],\"source\":{\"direction\":\"Right\",\"element\":\"ef0e78cb-f09a-4ae9-983f-b7f6c0a1fc97\"},\"target\":{\"direction\":\"Left\",\"element\":\"214cf566-8aac-4df3-9a6d-62084152993b\"}},{\"id\":\"f03bb53c-7fac-47eb-9e5e-04a6a3d63ad5\",\"name\":\"\",\"type\":\"DeploymentInterfaceProvided\",\"owner\":null,\"bounds\":{\"x\":510,\"y\":122.8000259399414,\"width\":40,\"height\":1},\"path\":[{\"x\":40,\"y\":0},{\"x\":0,\"y\":0},{\"x\":40,\"y\":0},{\"x\":0,\"y\":0}],\"source\":{\"direction\":\"Left\",\"element\":\"f2b3de7c-cadb-407e-829a-c4d9883a4139\"},\"target\":{\"direction\":\"Right\",\"element\":\"214cf566-8aac-4df3-9a6d-62084152993b\"}},{\"id\":\"fabd227c-99a7-44ce-9120-4f15789a2d97\",\"name\":\"\",\"type\":\"DeploymentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":300,\"y\":142.8000259399414,\"width\":350,\"height\":110},\"path\":[{\"x\":350,\"y\":0},{\"x\":350,\"y\":55},{\"x\":0,\"y\":55},{\"x\":0,\"y\":110}],\"source\":{\"direction\":\"Down\",\"element\":\"f2b3de7c-cadb-407e-829a-c4d9883a4139\"},\"target\":{\"direction\":\"Up\",\"element\":\"bb632c63-3ef2-43a7-ac43-a829083ddcb7\"}},{\"id\":\"47211c02-3bc1-4ac8-9cbd-0889486833e7\",\"name\":\"\",\"type\":\"DeploymentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":310,\"y\":262.8000259399414,\"width\":230,\"height\":1},\"path\":[{\"x\":230,\"y\":0},{\"x\":0,\"y\":0}],\"source\":{\"direction\":\"Left\",\"element\":\"b9ebb783-8cde-4a34-bbaf-7a146cf7347d\"},\"target\":{\"direction\":\"Right\",\"element\":\"bb632c63-3ef2-43a7-ac43-a829083ddcb7\"}},{\"id\":\"444c0e9e-3554-4307-a23e-379883717605\",\"name\":\"\",\"type\":\"DeploymentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":300,\"y\":272.8000259399414,\"width\":240,\"height\":70},\"path\":[{\"x\":240,\"y\":70},{\"x\":0,\"y\":70},{\"x\":0,\"y\":0}],\"source\":{\"direction\":\"Left\",\"element\":\"c4a70c29-18d1-4baf-8d49-7ac2c30aeec2\"},\"target\":{\"direction\":\"Down\",\"element\":\"bb632c63-3ef2-43a7-ac43-a829083ddcb7\"}},{\"id\":\"aff00ecb-d2b4-410b-9643-8b1b5e7376b2\",\"name\":\"\",\"type\":\"DeploymentDependency\",\"owner\":null,\"bounds\":{\"x\":165,\"y\":62.800025939941406,\"width\":985,\"height\":161.99996185302734},\"path\":[{\"x\":0,\"y\":40},{\"x\":0,\"y\":0},{\"x\":985,\"y\":0},{\"x\":985,\"y\":161.99996185302734}],\"source\":{\"direction\":\"Up\",\"element\":\"ef0e78cb-f09a-4ae9-983f-b7f6c0a1fc97\"},\"target\":{\"direction\":\"Up\",\"element\":\"63aa80f9-d736-45f1-aca7-a65b5f66d69e\"}},{\"id\":\"41291a73-9c26-495b-a99e-c562703075f9\",\"name\":\"\",\"type\":\"DeploymentInterfaceProvided\",\"owner\":null,\"bounds\":{\"x\":950,\"y\":174.79998779296875,\"width\":5,\"height\":88.00003814697266},\"path\":[{\"x\":5,\"y\":88.00003814697266},{\"x\":5,\"y\":44.00001907348633},{\"x\":0,\"y\":44.00001907348633},{\"x\":0,\"y\":0}],\"source\":{\"direction\":\"Up\",\"element\":\"d868c517-0287-40bb-916d-b1bc1328e4f0\"},\"target\":{\"direction\":\"Down\",\"element\":\"6637f723-d4d8-48ac-8022-cb322939898a\"}},{\"id\":\"418efae4-bee9-4921-92b0-e2b13ca42a84\",\"name\":\"\",\"type\":\"DeploymentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":960,\"y\":164.79998779296875,\"width\":120,\"height\":80},\"path\":[{\"x\":120,\"y\":80},{\"x\":60,\"y\":80},{\"x\":60,\"y\":0},{\"x\":0,\"y\":0}],\"source\":{\"direction\":\"Left\",\"element\":\"63aa80f9-d736-45f1-aca7-a65b5f66d69e\"},\"target\":{\"direction\":\"Right\",\"element\":\"6637f723-d4d8-48ac-8022-cb322939898a\"}},{\"id\":\"ee4bffde-f611-43a1-8032-ac33d1a22925\",\"name\":\"\",\"type\":\"DeploymentInterfaceProvided\",\"owner\":null,\"bounds\":{\"x\":750,\"y\":117.60000610351562,\"width\":50,\"height\":5.200019836425781},\"path\":[{\"x\":0,\"y\":5.200019836425781},{\"x\":40,\"y\":5.200019836425781},{\"x\":10,\"y\":0},{\"x\":50,\"y\":0}],\"source\":{\"direction\":\"Right\",\"element\":\"f2b3de7c-cadb-407e-829a-c4d9883a4139\"},\"target\":{\"direction\":\"Left\",\"element\":\"bbbe5445-50c5-4923-8026-294bfc81c3c2\"}},{\"id\":\"508753ff-1ec2-45b3-a06d-8b9096abb9f2\",\"name\":\"\",\"type\":\"DeploymentInterfaceProvided\",\"owner\":null,\"bounds\":{\"x\":740,\"y\":262.8000259399414,\"width\":60,\"height\":4.799980163574219},\"path\":[{\"x\":0,\"y\":0},{\"x\":40,\"y\":0},{\"x\":20,\"y\":4.799980163574219},{\"x\":60,\"y\":4.799980163574219}],\"source\":{\"direction\":\"Right\",\"element\":\"b9ebb783-8cde-4a34-bbaf-7a146cf7347d\"},\"target\":{\"direction\":\"Left\",\"element\":\"1e891366-a0ad-4438-8fec-b36ff3e777c8\"}},{\"id\":\"65c8646d-2775-4ee0-9785-089f36ec3b24\",\"name\":\"\",\"type\":\"DeploymentInterfaceProvided\",\"owner\":null,\"bounds\":{\"x\":740,\"y\":342.8000259399414,\"width\":60,\"height\":4.799980163574219},\"path\":[{\"x\":0,\"y\":0},{\"x\":40,\"y\":0},{\"x\":20,\"y\":4.799980163574219},{\"x\":60,\"y\":4.799980163574219}],\"source\":{\"direction\":\"Right\",\"element\":\"c4a70c29-18d1-4baf-8d49-7ac2c30aeec2\"},\"target\":{\"direction\":\"Left\",\"element\":\"c1065419-b9ff-4cdf-bc1b-623f443eae51\"}},{\"id\":\"f278aef1-a0cc-4f3f-b113-727f94d2264a\",\"name\":\"\",\"type\":\"DeploymentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":820,\"y\":267.6000061035156,\"width\":80,\"height\":15.200019836425781},\"path\":[{\"x\":80,\"y\":15.200019836425781},{\"x\":40,\"y\":15.200019836425781},{\"x\":40,\"y\":0},{\"x\":0,\"y\":0}],\"source\":{\"direction\":\"Left\",\"element\":\"d868c517-0287-40bb-916d-b1bc1328e4f0\"},\"target\":{\"direction\":\"Right\",\"element\":\"1e891366-a0ad-4438-8fec-b36ff3e777c8\"}},{\"id\":\"97057339-d3a1-4dd2-b35d-37c167aedefc\",\"name\":\"\",\"type\":\"DeploymentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":820,\"y\":282.8000259399414,\"width\":80,\"height\":64.79998016357422},\"path\":[{\"x\":80,\"y\":0},{\"x\":40,\"y\":0},{\"x\":40,\"y\":64.79998016357422},{\"x\":0,\"y\":64.79998016357422}],\"source\":{\"direction\":\"Left\",\"element\":\"d868c517-0287-40bb-916d-b1bc1328e4f0\"},\"target\":{\"direction\":\"Right\",\"element\":\"c1065419-b9ff-4cdf-bc1b-623f443eae51\"}},{\"id\":\"f0df4516-c7b9-4192-8252-882f2abbba77\",\"name\":\"\",\"type\":\"DeploymentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":820,\"y\":117.60000610351562,\"width\":80,\"height\":165.20001983642578},\"path\":[{\"x\":80,\"y\":165.20001983642578},{\"x\":40,\"y\":165.20001983642578},{\"x\":40,\"y\":0},{\"x\":0,\"y\":0}],\"source\":{\"direction\":\"Left\",\"element\":\"d868c517-0287-40bb-916d-b1bc1328e4f0\"},\"target\":{\"direction\":\"Right\",\"element\":\"bbbe5445-50c5-4923-8026-294bfc81c3c2\"}}],\"assessments\":[]}";

    private static final String deploymentModel2 = "{\"version\":\"2.0.0\",\"type\":\"DeploymentDiagram\",\"size\":{\"width\":1420,\"height\":840},\"interactive\":{\"elements\":[],\"relationships\":[]},\"elements\":[{\"id\":\"b55ec5ea-f138-4b3a-bc68-3653834d0011\",\"name\":\":PC\",\"type\":\"DeploymentNode\",\"owner\":null,\"bounds\":{\"x\":0,\"y\":0,\"width\":1410,\"height\":830},\"stereotype\":\"node\"},{\"id\":\"31f4386e-6026-4f5a-bd0a-7a0349ff84d7\",\"name\":\"View\",\"type\":\"Component\",\"owner\":\"b55ec5ea-f138-4b3a-bc68-3653834d0011\",\"bounds\":{\"x\":90,\"y\":580,\"width\":600,\"height\":220}},{\"id\":\"bd078690-adad-4aa1-90fb-7e411ecc1c0e\",\"name\":\"PlayingFieldView\",\"type\":\"Component\",\"owner\":\"31f4386e-6026-4f5a-bd0a-7a0349ff84d7\",\"bounds\":{\"x\":270,\"y\":681.111083984375,\"width\":210,\"height\":40}},{\"id\":\"cc82c8a4-6782-4d8b-b72f-ef62d9555455\",\"name\":\"Controller\",\"type\":\"Component\",\"owner\":\"b55ec5ea-f138-4b3a-bc68-3653834d0011\",\"bounds\":{\"x\":90,\"y\":66.66667175292969,\"width\":600,\"height\":460}},{\"id\":\"4b274b24-37c2-41c5-a421-c4232f5e979f\",\"name\":\"ArrowKeySteering\",\"type\":\"Component\",\"owner\":\"cc82c8a4-6782-4d8b-b72f-ef62d9555455\",\"bounds\":{\"x\":430,\"y\":461.111083984375,\"width\":210,\"height\":40}},{\"id\":\"fa096a7f-1a86-4f30-8a38-269fad1bca79\",\"name\":\"PlayingField\",\"type\":\"Component\",\"owner\":\"cc82c8a4-6782-4d8b-b72f-ef62d9555455\",\"bounds\":{\"x\":120,\"y\":364.4444122314453,\"width\":220,\"height\":40}},{\"id\":\"a920d1c1-787c-461e-8a1f-9e30c9c7487a\",\"name\":\"AudioPlayer\",\"type\":\"Component\",\"owner\":\"cc82c8a4-6782-4d8b-b72f-ef62d9555455\",\"bounds\":{\"x\":120,\"y\":117.77774047851562,\"width\":220,\"height\":40}},{\"id\":\"21cf9f19-d62e-49df-a2e3-39dd1a17d36a\",\"name\":\"Collision\",\"type\":\"Component\",\"owner\":\"cc82c8a4-6782-4d8b-b72f-ef62d9555455\",\"bounds\":{\"x\":420,\"y\":201.11106872558594,\"width\":210,\"height\":40}},{\"id\":\"a1d98a42-f687-4f0c-8688-b06560e4558b\",\"name\":\"Music Service\",\"type\":\"DeploymentInterface\",\"owner\":\"cc82c8a4-6782-4d8b-b72f-ef62d9555455\",\"bounds\":{\"x\":220,\"y\":241,\"width\":20,\"height\":20}},{\"id\":\"eb0dc2f9-e4f2-4d9b-8cdb-a7fe2cbda69e\",\"name\":\"Game Setup\",\"type\":\"DeploymentInterface\",\"owner\":\"cc82c8a4-6782-4d8b-b72f-ef62d9555455\",\"bounds\":{\"x\":220,\"y\":484.3333282470703,\"width\":20,\"height\":20}},{\"id\":\"87d0b3ca-c47b-496f-99ec-f82d4ed723e9\",\"name\":\"Collision Detection Service\",\"type\":\"DeploymentInterface\",\"owner\":\"cc82c8a4-6782-4d8b-b72f-ef62d9555455\",\"bounds\":{\"x\":370,\"y\":327.6666564941406,\"width\":20,\"height\":20}},{\"id\":\"e3db54f5-cfc0-4887-a4c6-8ee539e8e9b6\",\"name\":\"Model\",\"type\":\"Component\",\"owner\":\"b55ec5ea-f138-4b3a-bc68-3653834d0011\",\"bounds\":{\"x\":740,\"y\":66.66667175292969,\"width\":590,\"height\":730}},{\"id\":\"50c85aef-910b-4119-b6d8-8ff79ee8f829\",\"name\":\"Person\",\"type\":\"Component\",\"owner\":\"e3db54f5-cfc0-4887-a4c6-8ee539e8e9b6\",\"bounds\":{\"x\":990,\"y\":244.44442749023438,\"width\":220,\"height\":40}},{\"id\":\"b830c184-9e6f-4063-88bc-ac55452cf1dc\",\"name\":\"Building\",\"type\":\"Component\",\"owner\":\"e3db54f5-cfc0-4887-a4c6-8ee539e8e9b6\",\"bounds\":{\"x\":990,\"y\":457.7777557373047,\"width\":220,\"height\":40}},{\"id\":\"c6e6936d-5595-4643-abe5-fd359aa01ef8\",\"name\":\"Position Service\",\"type\":\"DeploymentInterface\",\"owner\":\"e3db54f5-cfc0-4887-a4c6-8ee539e8e9b6\",\"bounds\":{\"x\":790,\"y\":624,\"width\":20,\"height\":20}},{\"id\":\"b94863af-b95f-41f2-a110-e4343c49b5b5\",\"name\":\"Host&DeHost Service\",\"type\":\"DeploymentInterface\",\"owner\":\"e3db54f5-cfc0-4887-a4c6-8ee539e8e9b6\",\"bounds\":{\"x\":1090,\"y\":397.3333282470703,\"width\":20,\"height\":20}},{\"id\":\"d678d02e-62f3-48f6-baeb-7c3c4d4c9189\",\"name\":\"Health Status CheckUpdate Service\",\"type\":\"DeploymentInterface\",\"owner\":\"e3db54f5-cfc0-4887-a4c6-8ee539e8e9b6\",\"bounds\":{\"x\":870,\"y\":124,\"width\":20,\"height\":20}},{\"id\":\"9d259fda-6e6e-416c-b97f-75a90559ba3a\",\"name\":\"Moving Service\",\"type\":\"DeploymentInterface\",\"owner\":\"e3db54f5-cfc0-4887-a4c6-8ee539e8e9b6\",\"bounds\":{\"x\":840,\"y\":354,\"width\":20,\"height\":20}}],\"relationships\":[{\"id\":\"e77e03c5-9e73-41e5-ba59-fb21226a35e0\",\"name\":\"\",\"type\":\"DeploymentAssociation\",\"owner\":null,\"bounds\":{\"x\":230,\"y\":157.77774047851562,\"width\":1,\"height\":83.22225952148438},\"path\":[{\"x\":0,\"y\":83.22225952148438},{\"x\":0,\"y\":0}],\"source\":{\"direction\":\"Up\",\"element\":\"a1d98a42-f687-4f0c-8688-b06560e4558b\"},\"target\":{\"direction\":\"Down\",\"element\":\"a920d1c1-787c-461e-8a1f-9e30c9c7487a\"}},{\"id\":\"95c12d9a-7f74-4ee1-98f2-d266cf1e4276\",\"name\":\"\",\"type\":\"DeploymentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":230,\"y\":261,\"width\":1,\"height\":103.44441223144531},\"path\":[{\"x\":0,\"y\":103.44441223144531},{\"x\":0,\"y\":0}],\"source\":{\"direction\":\"Up\",\"element\":\"fa096a7f-1a86-4f30-8a38-269fad1bca79\"},\"target\":{\"direction\":\"Down\",\"element\":\"a1d98a42-f687-4f0c-8688-b06560e4558b\"}},{\"id\":\"e7cde67b-a496-4cd2-aab5-cc9d906821b9\",\"name\":\"\",\"type\":\"DeploymentAssociation\",\"owner\":null,\"bounds\":{\"x\":230,\"y\":404.4444122314453,\"width\":1,\"height\":79.888916015625},\"path\":[{\"x\":0,\"y\":79.888916015625},{\"x\":0,\"y\":0}],\"source\":{\"direction\":\"Up\",\"element\":\"eb0dc2f9-e4f2-4d9b-8cdb-a7fe2cbda69e\"},\"target\":{\"direction\":\"Down\",\"element\":\"fa096a7f-1a86-4f30-8a38-269fad1bca79\"}},{\"id\":\"55e6d1b9-4181-4412-9ac4-7386f5292a00\",\"name\":\"\",\"type\":\"DeploymentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":230,\"y\":504.3333282470703,\"width\":40,\"height\":196.7777557373047},\"path\":[{\"x\":40,\"y\":196.7777557373047},{\"x\":0,\"y\":196.7777557373047},{\"x\":0,\"y\":0}],\"source\":{\"direction\":\"Left\",\"element\":\"bd078690-adad-4aa1-90fb-7e411ecc1c0e\"},\"target\":{\"direction\":\"Down\",\"element\":\"eb0dc2f9-e4f2-4d9b-8cdb-a7fe2cbda69e\"}},{\"id\":\"9b2fb510-1925-4991-bfe8-80856485409e\",\"name\":\"\",\"type\":\"DeploymentDependency\",\"owner\":null,\"bounds\":{\"x\":455,\"y\":501.111083984375,\"width\":1,\"height\":180},\"path\":[{\"x\":0,\"y\":0},{\"x\":0,\"y\":180}],\"source\":{\"direction\":\"Down\",\"element\":\"4b274b24-37c2-41c5-a421-c4232f5e979f\"},\"target\":{\"direction\":\"Up\",\"element\":\"bd078690-adad-4aa1-90fb-7e411ecc1c0e\"}},{\"id\":\"3fdbc32c-7053-4502-ac71-f0263e26795f\",\"name\":\"\",\"type\":\"DeploymentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":340,\"y\":347.6666564941406,\"width\":40,\"height\":36.77775573730469},\"path\":[{\"x\":0,\"y\":36.77775573730469},{\"x\":40,\"y\":36.77775573730469},{\"x\":40,\"y\":0}],\"source\":{\"direction\":\"Right\",\"element\":\"fa096a7f-1a86-4f30-8a38-269fad1bca79\"},\"target\":{\"direction\":\"Down\",\"element\":\"87d0b3ca-c47b-496f-99ec-f82d4ed723e9\"}},{\"id\":\"a03d2321-4666-4d03-9f03-5c3951d6424f\",\"name\":\"\",\"type\":\"DeploymentAssociation\",\"owner\":null,\"bounds\":{\"x\":380,\"y\":241.11106872558594,\"width\":145,\"height\":86.55558776855469},\"path\":[{\"x\":0,\"y\":86.55558776855469},{\"x\":0,\"y\":43.277793884277344},{\"x\":145,\"y\":43.277793884277344},{\"x\":145,\"y\":0}],\"source\":{\"direction\":\"Up\",\"element\":\"87d0b3ca-c47b-496f-99ec-f82d4ed723e9\"},\"target\":{\"direction\":\"Down\",\"element\":\"21cf9f19-d62e-49df-a2e3-39dd1a17d36a\"}},{\"id\":\"0b5327ab-2d06-4189-929b-97bb9eea8b0e\",\"name\":\"\",\"type\":\"DeploymentAssociation\",\"owner\":null,\"bounds\":{\"x\":480,\"y\":644,\"width\":320,\"height\":57.111083984375},\"path\":[{\"x\":0,\"y\":57.111083984375},{\"x\":320,\"y\":57.111083984375},{\"x\":320,\"y\":0}],\"source\":{\"direction\":\"Right\",\"element\":\"bd078690-adad-4aa1-90fb-7e411ecc1c0e\"},\"target\":{\"direction\":\"Down\",\"element\":\"c6e6936d-5595-4643-abe5-fd359aa01ef8\"}},{\"id\":\"0a2d33db-afdc-4e55-a6f1-caf3db71d5d2\",\"name\":\"\",\"type\":\"DeploymentAssociation\",\"owner\":null,\"bounds\":{\"x\":810,\"y\":497.7777557373047,\"width\":290,\"height\":136.2222442626953},\"path\":[{\"x\":0,\"y\":136.2222442626953},{\"x\":290,\"y\":136.2222442626953},{\"x\":290,\"y\":0}],\"source\":{\"direction\":\"Right\",\"element\":\"c6e6936d-5595-4643-abe5-fd359aa01ef8\"},\"target\":{\"direction\":\"Down\",\"element\":\"b830c184-9e6f-4063-88bc-ac55452cf1dc\"}},{\"id\":\"4549fecd-f2dd-4533-b684-701125e04c18\",\"name\":\"\",\"type\":\"DeploymentAssociation\",\"owner\":null,\"bounds\":{\"x\":800,\"y\":264.4444274902344,\"width\":450,\"height\":419.5555725097656},\"path\":[{\"x\":410,\"y\":0},{\"x\":450,\"y\":0},{\"x\":450,\"y\":419.5555725097656},{\"x\":0,\"y\":419.5555725097656},{\"x\":0,\"y\":379.5555725097656}],\"source\":{\"direction\":\"Right\",\"element\":\"50c85aef-910b-4119-b6d8-8ff79ee8f829\"},\"target\":{\"direction\":\"Down\",\"element\":\"c6e6936d-5595-4643-abe5-fd359aa01ef8\"}},{\"id\":\"3cf51844-3255-4ac7-9815-6480a7b5615f\",\"name\":\"\",\"type\":\"DeploymentAssociation\",\"owner\":null,\"bounds\":{\"x\":1100,\"y\":417.3333282470703,\"width\":1,\"height\":40.444427490234375},\"path\":[{\"x\":0,\"y\":40.444427490234375},{\"x\":0,\"y\":0.444427490234375},{\"x\":0,\"y\":40},{\"x\":0,\"y\":0}],\"source\":{\"direction\":\"Up\",\"element\":\"b830c184-9e6f-4063-88bc-ac55452cf1dc\"},\"target\":{\"direction\":\"Down\",\"element\":\"b94863af-b95f-41f2-a110-e4343c49b5b5\"}},{\"id\":\"f8e23941-caf3-4aef-adc3-91e6894edc02\",\"name\":\"\",\"type\":\"DeploymentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":1100,\"y\":284.4444274902344,\"width\":1,\"height\":112.88890075683594},\"path\":[{\"x\":0,\"y\":0},{\"x\":0,\"y\":112.88890075683594}],\"source\":{\"direction\":\"Down\",\"element\":\"50c85aef-910b-4119-b6d8-8ff79ee8f829\"},\"target\":{\"direction\":\"Up\",\"element\":\"b94863af-b95f-41f2-a110-e4343c49b5b5\"}},{\"id\":\"054bcb63-d60d-462e-9903-482403bf09b1\",\"name\":\"\",\"type\":\"DeploymentAssociation\",\"owner\":null,\"bounds\":{\"x\":880,\"y\":144,\"width\":220,\"height\":100.44442749023438},\"path\":[{\"x\":220,\"y\":100.44442749023438},{\"x\":220,\"y\":50.22221374511719},{\"x\":0,\"y\":50.22221374511719},{\"x\":0,\"y\":0}],\"source\":{\"direction\":\"Up\",\"element\":\"50c85aef-910b-4119-b6d8-8ff79ee8f829\"},\"target\":{\"direction\":\"Down\",\"element\":\"d678d02e-62f3-48f6-baeb-7c3c4d4c9189\"}},{\"id\":\"ca21befe-9d91-4916-9fdb-a1e559c034f2\",\"name\":\"\",\"type\":\"DeploymentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":380,\"y\":134,\"width\":490,\"height\":87.11106872558594},\"path\":[{\"x\":40,\"y\":87.11106872558594},{\"x\":0,\"y\":87.11106872558594},{\"x\":0,\"y\":0},{\"x\":490,\"y\":0}],\"source\":{\"direction\":\"Left\",\"element\":\"21cf9f19-d62e-49df-a2e3-39dd1a17d36a\"},\"target\":{\"direction\":\"Left\",\"element\":\"d678d02e-62f3-48f6-baeb-7c3c4d4c9189\"}},{\"id\":\"400ace71-965a-466c-9ad6-6bcd95212870\",\"name\":\"\",\"type\":\"DeploymentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":525,\"y\":161.11106872558594,\"width\":275,\"height\":462.88893127441406},\"path\":[{\"x\":0,\"y\":40},{\"x\":0,\"y\":0},{\"x\":275,\"y\":0},{\"x\":275,\"y\":462.88893127441406}],\"source\":{\"direction\":\"Up\",\"element\":\"21cf9f19-d62e-49df-a2e3-39dd1a17d36a\"},\"target\":{\"direction\":\"Up\",\"element\":\"c6e6936d-5595-4643-abe5-fd359aa01ef8\"}},{\"id\":\"9a2b7044-445c-4fd3-9c57-eda3dc8e0906\",\"name\":\"\",\"type\":\"DeploymentAssociation\",\"owner\":null,\"bounds\":{\"x\":850,\"y\":264.4444274902344,\"width\":140,\"height\":89.55557250976562},\"path\":[{\"x\":140,\"y\":0},{\"x\":0,\"y\":0},{\"x\":0,\"y\":89.55557250976562}],\"source\":{\"direction\":\"Left\",\"element\":\"50c85aef-910b-4119-b6d8-8ff79ee8f829\"},\"target\":{\"direction\":\"Up\",\"element\":\"9d259fda-6e6e-416c-b97f-75a90559ba3a\"}},{\"id\":\"f79db097-1558-4c81-a815-6d4740fbd542\",\"name\":\"\",\"type\":\"DeploymentAssociation\",\"owner\":null,\"bounds\":{\"x\":535,\"y\":364,\"width\":305,\"height\":97.111083984375},\"path\":[{\"x\":305,\"y\":0},{\"x\":0,\"y\":0},{\"x\":0,\"y\":97.111083984375}],\"source\":{\"direction\":\"Left\",\"element\":\"9d259fda-6e6e-416c-b97f-75a90559ba3a\"},\"target\":{\"direction\":\"Up\",\"element\":\"4b274b24-37c2-41c5-a421-c4232f5e979f\"}},{\"id\":\"e1bfe2d1-55e0-4221-886e-619f695db2b8\",\"name\":\"\",\"type\":\"DeploymentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":340,\"y\":374,\"width\":510,\"height\":55.222206115722656},\"path\":[{\"x\":0,\"y\":10.444412231445312},{\"x\":40,\"y\":10.444412231445312},{\"x\":40,\"y\":55.222206115722656},{\"x\":510,\"y\":55.222206115722656},{\"x\":510,\"y\":0}],\"source\":{\"direction\":\"Right\",\"element\":\"fa096a7f-1a86-4f30-8a38-269fad1bca79\"},\"target\":{\"direction\":\"Down\",\"element\":\"9d259fda-6e6e-416c-b97f-75a90559ba3a\"}}],\"assessments\":[]}";

    private static final String deploymentModel3 = "{\"version\":\"2.0.0\",\"type\":\"DeploymentDiagram\",\"size\":{\"width\":1100,\"height\":520},\"interactive\":{\"elements\":[],\"relationships\":[]},\"elements\":[{\"id\":\"d821e11d-6cf1-497b-8462-6533047cb0e8\",\"name\":\"A\",\"type\":\"DeploymentNode\",\"owner\":null,\"bounds\":{\"x\":0,\"y\":0,\"width\":650,\"height\":520},\"stereotype\":\"stereotype\"},{\"id\":\"035638ac-9bb1-4bfb-a2c1-028310ae4c3e\",\"name\":\"Component\",\"type\":\"Component\",\"owner\":\"d821e11d-6cf1-497b-8462-6533047cb0e8\",\"bounds\":{\"x\":60,\"y\":110,\"width\":300,\"height\":190}},{\"id\":\"29d85bcc-a5c4-4f40-96df-61d3ff99d296\",\"name\":\"Component\",\"type\":\"Component\",\"owner\":\"035638ac-9bb1-4bfb-a2c1-028310ae4c3e\",\"bounds\":{\"x\":80,\"y\":160,\"width\":230,\"height\":100}},{\"id\":\"800db443-242f-4c40-8106-6b2a5fa99d2f\",\"name\":\"Artifact\",\"type\":\"DeploymentArtifact\",\"owner\":\"29d85bcc-a5c4-4f40-96df-61d3ff99d296\",\"bounds\":{\"x\":100,\"y\":200,\"width\":200,\"height\":40}},{\"id\":\"e7e19fbf-cbaa-43f2-b39f-929e3e78f4f3\",\"name\":\"Interface\",\"type\":\"DeploymentInterface\",\"owner\":\"d821e11d-6cf1-497b-8462-6533047cb0e8\",\"bounds\":{\"x\":240,\"y\":350,\"width\":20,\"height\":20}},{\"id\":\"f729b118-7f2a-4006-b928-10e820d5fd99\",\"name\":\"Artifact\",\"type\":\"DeploymentArtifact\",\"owner\":\"d821e11d-6cf1-497b-8462-6533047cb0e8\",\"bounds\":{\"x\":390,\"y\":130,\"width\":200,\"height\":40}},{\"id\":\"2b163764-7c83-46e4-9682-fff3e1f5b457\",\"name\":\"Component\",\"type\":\"Component\",\"owner\":\"d821e11d-6cf1-497b-8462-6533047cb0e8\",\"bounds\":{\"x\":360,\"y\":380,\"width\":200,\"height\":100}},{\"id\":\"4e7df539-9ceb-4a56-8b5e-890b9124c368\",\"name\":\"Artifact\",\"type\":\"DeploymentArtifact\",\"owner\":\"d821e11d-6cf1-497b-8462-6533047cb0e8\",\"bounds\":{\"x\":400,\"y\":250,\"width\":200,\"height\":40}},{\"id\":\"6caef276-eb9d-45cc-9572-0a4181790748\",\"name\":\"component\",\"type\":\"DeploymentNode\",\"owner\":null,\"bounds\":{\"x\":820,\"y\":100,\"width\":200,\"height\":100},\"stereotype\":\"B\"}],\"relationships\":[{\"id\":\"908c65c5-7c25-48ce-bb9d-f436bb5ee401\",\"name\":\"\",\"type\":\"DeploymentAssociation\",\"owner\":null,\"bounds\":{\"x\":650,\"y\":150,\"width\":170,\"height\":1},\"path\":[{\"x\":170,\"y\":0},{\"x\":0,\"y\":0}],\"source\":{\"direction\":\"Left\",\"element\":\"6caef276-eb9d-45cc-9572-0a4181790748\"},\"target\":{\"direction\":\"Right\",\"element\":\"d821e11d-6cf1-497b-8462-6533047cb0e8\"}},{\"id\":\"bbdbd842-1ddc-4687-8449-e3afff980209\",\"name\":\"\",\"type\":\"DeploymentAssociation\",\"owner\":null,\"bounds\":{\"x\":460,\"y\":340,\"width\":140,\"height\":90},\"path\":[{\"x\":100,\"y\":90},{\"x\":140,\"y\":90},{\"x\":140,\"y\":0},{\"x\":0,\"y\":0},{\"x\":0,\"y\":40}],\"source\":{\"direction\":\"Right\",\"element\":\"2b163764-7c83-46e4-9682-fff3e1f5b457\"},\"target\":{\"direction\":\"Up\",\"element\":\"2b163764-7c83-46e4-9682-fff3e1f5b457\"}},{\"id\":\"4a93d54e-6cd3-4abc-9bf0-01c23b7d843c\",\"name\":\"\",\"type\":\"DeploymentDependency\",\"owner\":null,\"bounds\":{\"x\":490,\"y\":60,\"width\":430,\"height\":70},\"path\":[{\"x\":0,\"y\":70},{\"x\":0,\"y\":0},{\"x\":430,\"y\":0},{\"x\":430,\"y\":40}],\"source\":{\"direction\":\"Up\",\"element\":\"f729b118-7f2a-4006-b928-10e820d5fd99\"},\"target\":{\"direction\":\"Up\",\"element\":\"6caef276-eb9d-45cc-9572-0a4181790748\"}},{\"id\":\"94238b1a-1875-44ed-8b75-e54bc6759ba2\",\"name\":\"\",\"type\":\"DeploymentInterfaceRequired\",\"owner\":null,\"bounds\":{\"x\":250,\"y\":370,\"width\":110,\"height\":60},\"path\":[{\"x\":110,\"y\":60},{\"x\":0,\"y\":60},{\"x\":0,\"y\":0}],\"source\":{\"direction\":\"Left\",\"element\":\"2b163764-7c83-46e4-9682-fff3e1f5b457\"},\"target\":{\"direction\":\"Down\",\"element\":\"e7e19fbf-cbaa-43f2-b39f-929e3e78f4f3\"}},{\"id\":\"64fc87f7-6a37-4bd5-bf4d-2182f80e4fee\",\"name\":\"\",\"type\":\"DeploymentInterfaceProvided\",\"owner\":null,\"bounds\":{\"x\":200,\"y\":240,\"width\":40,\"height\":120},\"path\":[{\"x\":40,\"y\":120},{\"x\":0,\"y\":120},{\"x\":0,\"y\":0}],\"source\":{\"direction\":\"Left\",\"element\":\"e7e19fbf-cbaa-43f2-b39f-929e3e78f4f3\"},\"target\":{\"direction\":\"Down\",\"element\":\"800db443-242f-4c40-8106-6b2a5fa99d2f\"}},{\"id\":\"c30a8c04-5db9-4424-96f1-cd25f9705db2\",\"name\":\"\",\"type\":\"DeploymentAssociation\",\"owner\":null,\"bounds\":{\"x\":360,\"y\":270,\"width\":40,\"height\":1},\"path\":[{\"x\":40,\"y\":0},{\"x\":0,\"y\":0}],\"source\":{\"direction\":\"Left\",\"element\":\"4e7df539-9ceb-4a56-8b5e-890b9124c368\"},\"target\":{\"direction\":\"Right\",\"element\":\"035638ac-9bb1-4bfb-a2c1-028310ae4c3e\"}},{\"id\":\"8345194f-2363-4b35-9609-0cfe84709005\",\"name\":\"\",\"type\":\"DeploymentDependency\",\"owner\":null,\"bounds\":{\"x\":495,\"y\":170,\"width\":1,\"height\":80},\"path\":[{\"x\":0,\"y\":80},{\"x\":0,\"y\":0}],\"source\":{\"direction\":\"Up\",\"element\":\"4e7df539-9ceb-4a56-8b5e-890b9124c368\"},\"target\":{\"direction\":\"Down\",\"element\":\"f729b118-7f2a-4006-b928-10e820d5fd99\"}}],\"assessments\":[]}";

    @Test
    void similarityDeploymentDiagram_EqualModels() {
        compareSubmissions(new ModelingSubmission().model(deploymentModel1), new ModelingSubmission().model(deploymentModel1), 0.8, 1.0);
        compareSubmissions(new ModelingSubmission().model(deploymentModel2), new ModelingSubmission().model(deploymentModel2), 0.8, 1.0);
        compareSubmissions(new ModelingSubmission().model(deploymentModel3), new ModelingSubmission().model(deploymentModel3), 0.8, 1.0);
    }

    @Test
    void similarityDeploymentDiagram_DifferentModels() {
        compareSubmissions(new ModelingSubmission().model(deploymentModel1), new ModelingSubmission().model(deploymentModel2), 0.0, 0.3688);
        compareSubmissions(new ModelingSubmission().model(deploymentModel1), new ModelingSubmission().model(deploymentModel3), 0.0, 0.1022);
        compareSubmissions(new ModelingSubmission().model(deploymentModel2), new ModelingSubmission().model(deploymentModel3), 0.0, 0.1099);
    }

    @Test
    void parseDeploymentDiagramModelCorrectly() throws IOException {
        UMLDiagram diagram = UMLModelParser.buildModelFromJSON(parseString(deploymentModel3).getAsJsonObject(), 1L);
        assertThat(diagram).isInstanceOf(UMLDeploymentDiagram.class);
        UMLDeploymentDiagram deploymentDiagram = (UMLDeploymentDiagram) diagram;
        assertThat(deploymentDiagram.getComponentList()).hasSize(3);
        assertThat(deploymentDiagram.getComponentInterfaceList()).hasSize(1);
        assertThat(deploymentDiagram.getArtifactList()).hasSize(3);
        assertThat(deploymentDiagram.getNodeList()).hasSize(2);
        assertThat(deploymentDiagram.getComponentRelationshipList()).hasSize(7);

        assertThat(deploymentDiagram.getElementByJSONID("d821e11d-6cf1-497b-8462-6533047cb0e8")).isInstanceOf(UMLNode.class);
        assertThat(deploymentDiagram.getElementByJSONID("035638ac-9bb1-4bfb-a2c1-028310ae4c3e")).isInstanceOf(UMLComponent.class);
        assertThat(deploymentDiagram.getElementByJSONID("800db443-242f-4c40-8106-6b2a5fa99d2f")).isInstanceOf(UMLArtifact.class);
    }
}
