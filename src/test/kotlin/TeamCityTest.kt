import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.dotenv
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.testng.Assert
import org.testng.annotations.AfterSuite
import org.testng.annotations.BeforeSuite
import org.testng.annotations.Test
import java.time.Duration.ofSeconds


class TeamCityTest {
    private lateinit var driver: WebDriver
    private lateinit var dotenv: Dotenv
    private lateinit var projectName: String
    private lateinit var wait: WebDriverWait
    //Before preparations
    @BeforeSuite
    fun setUp() {
        dotenv = dotenv()
        projectName = dotenv["TC_PROJECT_SAMPLE_URL"].substringAfterLast("/")
        driver = ChromeDriver()
        driver.manage().window().maximize()
        wait = WebDriverWait(driver, ofSeconds(10))
    }

    @Test(groups = ["login"])
    fun loginUser() {
        driver.get(dotenv["TC_BASE_URL"])
        //Verify that the login page is shown
        Assert.assertEquals(driver.findElement(By.xpath("//h1[@id='header']")).text, "Log in to TeamCity")
        //Put the correct login credentials and click login button
        driver.findElement(By.xpath("//input[@id='username']")).sendKeys(dotenv["TC_USERNAME"])
        driver.findElement(By.xpath("//input[@id='password']")).sendKeys(dotenv["TC_PASSWORD"])
        //Make sure checkbox for "Remember me" is ticked
        val checkBox = driver.findElement(By.xpath("//input[@id='remember']"))
        if (checkBox.getAttribute("value") != "true") {
            checkBox.click()
        }
        //Click login button
        driver.findElement(By.xpath("//input[@type='submit']")).click()
        //Verify that the projects page is shown
        Assert.assertTrue(wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//span[contains(text(), 'Projects')]"))).isDisplayed)
    }
    @Test(groups = ["agent"], dependsOnGroups = ["login"])
    fun checkAgent(){
        //Go to the Agents tab
        driver.findElement(By.xpath("//span[contains(text(), 'Agents')]")).click()
        //Verify that the amount of agents is not 0
        Assert.assertNotEquals(driver.findElement(By.xpath("//span[@data-hint-container-id='header-agents-active']")).text, "0")
        //Verify that the agent is shown as enabled
        Assert.assertTrue(wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//label[@title='Agent is enabled, click to disable.']"))).isDisplayed)
        //Verify that the agent has a name
        Assert.assertTrue(driver.findElement(By.xpath("//span[@class='MiddleEllipsis__middle-ellipsis--G7']")).getAttribute("title").contains("Agent name"))
        //Open the Agent details
        driver.findElement(By.xpath("//span[contains(text(), 'Default Agent')]")).click()
        //Verify that the agent is connected, authorized and enabled
        val agentStatuses = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath("//span[@class='AgentStatus__status-up--_o']")))
        Assert.assertEquals(agentStatuses[0].text, "Connected")
        Assert.assertEquals(agentStatuses[1].text, "Authorized")
        Assert.assertEquals(agentStatuses[2].text, "Enabled")
    }

    @Test(groups = ["project"], dependsOnGroups = ["agent"])
    fun createProject() {
        //Create a new project
        driver.findElement(By.xpath("//span[@data-test-icon='add']")).click()
        //Verify that the page "Create Project" is opened
        Assert.assertTrue(driver.findElement(By.xpath("//a[contains(text(), 'Create Project')]")).isDisplayed)
        //Fill in the repository URL and click "Proceed"
        driver.findElement(By.xpath("//input[@id='url']")).sendKeys(dotenv["TC_PROJECT_SAMPLE_URL"])
        driver.findElement(By.xpath("//input[@value='Proceed']")).click()
        //Verify that the page "Create Project From URL" is opened
        Assert.assertTrue(wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//a[contains(text(), 'Create Project From URL')]"))).isDisplayed)
        //Verify that the Project name field has the same name as the project in the URL
        Assert.assertTrue(driver.findElement(By.xpath("//input[@id='projectName']")).getAttribute("value").contains(projectName))
        //Verify that the Build configuration name is not empty
        Assert.assertTrue(driver.findElement(By.xpath("//input[@id='buildTypeName']")).getAttribute("value").isNotEmpty())
        //Verify that the Default branch is the same as the default branch in the project in the URL
        Assert.assertEquals(driver.findElement(By.xpath("//input[@id='branch']")).getAttribute("value"), dotenv["TC_PROJECT_SAMPLE_DEFAULT_BRANCH"])
        //Verify that the Branch specification field has content "refs/heads/*"
        Assert.assertTrue(driver.findElement(By.xpath("//textarea[@id='teamcity:branchSpec']")).text.contains("refs/heads/*"))
        //Click the "Proceed" button
        driver.findElement(By.xpath("//input[@value='Proceed']")).click()
        //Verify that the page with project name is opened
        Assert.assertTrue(driver.findElement(By.xpath("//div[@class='successMessage ']")).text.contains("have been successfully created"))
        Assert.assertTrue(driver.findElement(By.xpath("//a[contains(text(), '$projectName')]")).isDisplayed)
    }

    @Test(groups = ["build"], dependsOnGroups = ["project"])
    fun autoDetectBuild(){
        //If not opened, open the Auto Detected Build Steps page
        if (!driver.findElement(By.xpath("//h2[contains(text(), 'Auto-detected Build Steps')]")).isDisplayed){
            //Open the project edit page
            driver.findElement(By.xpath("//span[contains(text(), 'Projects')]")).click()
            driver.findElements(By.xpath("//span[contains(text(), '$projectName')]"))[0].click()
            driver.findElement(By.xpath("//span[contains(text(), 'Edit project...')]")).click()
            //Go to Build steps and click "Auto-detect build steps" button
            driver.findElement(By.xpath("//a[@class='Edit build configuration settings']")).click()
            ////Go to Build steps and click "Auto-detect build steps" button
            driver.findElement(By.xpath("//a[contains(text(), 'Build steps')]")).click()
            driver.findElement(By.xpath("//a[@class='btn '")).click()
            //Verify that the Build General Settings page is opened
            Assert.assertTrue(driver.findElement(By.xpath("//h2[contains(text(), 'Auto-detected Build Steps']")).isDisplayed)
        }
        //Verify that the auto detection is done
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath("//div[@id='discoveryProgressContainer']")))
        Assert.assertTrue(wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//th[contains(text(), 'Build Step')]"))).isDisplayed)
        //Verify that the detected build contains "Maven"
        Assert.assertTrue(driver.findElement(By.xpath("//td[contains(text(), 'Maven')]")).isDisplayed)
        //Check the checkbox with "Maven" build step and click "Use selected"
        driver.findElements(By.xpath("//span[@class='custom-checkbox ring-checkbox-checkbox']"))[0].click()
        driver.findElement(By.xpath("//a[contains(text(), 'Use selected')]")).click()
        //Verify that the new step is added and contains Maven
        Assert.assertTrue(wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@class='successMessage ']"))).text.contains("New build step added."))
        Assert.assertTrue(driver.findElements(By.xpath("//div[@class='stepName']"))[0].text.contains("Maven"))
    }
    @Test(groups = ["run"], dependsOnGroups = ["build"])
    fun runBuild() {
        //Open the Maven Build Step page
        driver.findElement(By.xpath("//a[contains(text(), 'Build Step: Maven')]")).click()
        //Click the "Run" button
        driver.findElement(By.xpath("//button[contains(text(), 'Run')]")).click()
        //Verify that the run is started
        Assert.assertTrue(wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//span[@data-test-icon='running_green']"))).isDisplayed)
        //Wait until the run is done and verify that the build it successful (check mark is present)
        var waitLong = WebDriverWait(driver, ofSeconds(50))
        Assert.assertTrue(waitLong.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//span[@data-test-icon='finished_green']"))).isDisplayed)
    }


    //Cleaning and closing the browser
    @AfterSuite
    fun tearDown() {
        //Delete the created project
        driver.findElement(By.xpath("//span[contains(text(), 'Projects')]")).click()
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//span[contains(text(), '$projectName')]")))
        driver.findElements(By.xpath("//span[contains(text(), '$projectName')]"))[0].click()
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//span[contains(text(), 'Edit project...')]"))).click()
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(), 'Actions')]"))).click()
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(text(), 'Delete project...')]"))).click()
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@id='deleteProject__hostnameConfirmation']"))).sendKeys(driver.findElement(By.xpath("//strong[@class='hostnamePlaceholder']")).text)
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@value='Delete']"))).click()
        //Verify that the project was deleted
        Assert.assertTrue(driver.findElement(By.xpath("//div[@id='message_projectRemoved']")).text.contains("has been moved to the \"config/_trash\" directory."))
        //Close browser
        driver.close()
    }

}