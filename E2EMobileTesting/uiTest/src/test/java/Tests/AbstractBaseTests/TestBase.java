/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package Tests.AbstractBaseTests;

import io.appium.java_client.MobileElement;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.testng.annotations.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * An abstract base for all of the Android tests within this package
 * responsible for setting up the Appium test Driver
 */
public abstract class TestBase {

    private final String URL_STRING = "http://127.0.0.1:4723/wd/hub";
    private final int TIMEOUT = 30;

    /**
     * Make the driver static. This allows it to be created only once
     * and used across all of the test classes.
     */
    public static AndroidDriver<MobileElement> driver;

    /**
     * This method runs before any other method.
     *
     * Appium follows a client - server model:
     * We are setting up our appium client in order to connect to Device Farm's appium server.
     *
     * We do not need to and SHOULD NOT set our own DesiredCapabilities
     * Device Farm creates custom settings at the server level. Setting your own DesiredCapabilities
     * will result in unexpected results and failures.
     *
     * @throws MalformedURLException An exception that occurs when the URL is wrong
     */
    @BeforeSuite
    public void setUpAppium() throws MalformedURLException{

        URL url = new URL(URL_STRING);

	/*
	 * As mentioned in the documentation above we should not set the capabilities when 
	 * running on Device Farm's server. The settings made here will be ignored or may have
	 * unexpected results.
	 */
        driver = new AndroidDriver<MobileElement>(url, new DesiredCapabilities());

        //Use a higher value if your mobile elements take time to show up.
        driver.manage().timeouts().implicitlyWait(TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * Always remember to quit.
     */
    @AfterSuite
    public void tearDownAppium(){
        if (driver != null) {
            driver.quit();
        }
    }

    /**
     * Restart the app after every test class to go back to the main
     * screen and to reset the behavior.
     */
    @AfterClass
    public void restartApp() {
    }
}
