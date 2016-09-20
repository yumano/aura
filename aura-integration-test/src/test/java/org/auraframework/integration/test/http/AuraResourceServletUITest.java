/*
 * Copyright (C) 2013 salesforce.com, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.auraframework.integration.test.http;

import org.auraframework.integration.test.util.WebDriverTestCase;
import org.auraframework.integration.test.util.WebDriverTestCase.CheckAccessibility;
import org.auraframework.util.test.annotation.ThreadHostileTest;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;

@CheckAccessibility(false)
public class AuraResourceServletUITest extends WebDriverTestCase {
    private final String CACHES_URL = "/performance/caches.app";

    private final String BOGUS_KEY = "youwouldntputthiskeyinnowwouldyou";

    private void openCachesAppWithRefresh(String cache, String key) throws Exception {
        open(CACHES_URL+"?key="+BOGUS_KEY);
        waitForKey(BOGUS_KEY);
        WebElement el = getDriver().findElement(By.className("searchButton"));
        el.sendKeys(Keys.CONTROL,Keys.F5);
        open(CACHES_URL+"?cache="+cache+"&key="+key);
        waitForCache(cache);
        waitForKey(key);
    }

    private void waitForCache(String cacheName) throws Exception {
        getAuraUITestingUtil().waitUntil(new ExpectedCondition<Boolean>() {
                                             @Override
                                             public Boolean apply(WebDriver d) {
                                                 WebElement el = d.findElement(By.className("cache"));
                                                 if (el != null) {
                                                     return cacheName.equals(el.getText());
                                                 }
                                                 return false;
                                             }
                                         });
    }

    private void waitForKey(String keyValue) throws Exception {
        getAuraUITestingUtil().waitUntil(new ExpectedCondition<Boolean>() {
                                             @Override
                                             public Boolean apply(WebDriver d) {
                                                 WebElement el = d.findElement(By.className("searchValue"));
                                                 if (el != null) {
                                                     return keyValue.equals(el.getText());
                                                 }
                                                 return false;
                                             }
                                         });
    }

    private String getCount() throws Exception {
        WebElement el = getDriver().findElement(By.className("count"));
        if (el != null) {
            return el.getText();
        }
        return "";
    }

    @Test
    @ThreadHostileTest
    public void testDepsCacheContainsApp() throws Exception {
        openCachesAppWithRefresh("depsCache", "APPLICATION:markup://performance:caches");
        assertEquals("There should be 2 entries in the deps cache", "2", getCount());
    }

    @Test
    @ThreadHostileTest
    public void testAltStringsCacheContainsAppJs() throws Exception {
        openCachesAppWithRefresh("altStringsCache", "markup://performance:caches@JS");
        assertEquals("There should be 1 entry in the alt strings cache for JS", "1", getCount());
    }

    @Test
    @ThreadHostileTest
    public void testAltStringsCacheContainsAppCss() throws Exception {
        openCachesAppWithRefresh("altStringsCache", "markup://performance:caches@CSS");
        assertEquals("There should be 1 entry in the alt strings cache for CSS", "1", getCount());
    }
}
