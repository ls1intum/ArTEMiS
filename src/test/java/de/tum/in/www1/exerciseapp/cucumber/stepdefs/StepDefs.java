package de.tum.in.www1.exerciseapp.cucumber.stepdefs;

import de.tum.in.www1.exerciseapp.ArTEMiSApp;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.ResultActions;

@WebAppConfiguration
@SpringBootTest
@ContextConfiguration(classes = ArTEMiSApp.class)
public abstract class StepDefs {

    protected ResultActions actions;

}
