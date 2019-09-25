package cn.com.test.my12306.my12306.core.util;

import cn.com.test.my12306.my12306.core.ClientTicket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Set;

@Component
public class WebDriverUtil {

    private static Logger logger = LogManager.getLogger(WebDriverUtil.class);
	private  WebDriver webDriver;

    @Value("${geckodriverPath}")
	private String geckodriverPath;
    @Value("${firefoxBinaryPath}")
    private String firefoxBinaryPath;

    @Autowired
    ClientTicket ct;

    @PostConstruct
    public void init(){
        resetWebDriver();
    }

    public void resetWebDriver(){
        try {
            FirefoxOptions fo = new FirefoxOptions();
            fo.addArguments("--headless");
            fo.setBinary(firefoxBinaryPath);
            System.setProperty("webdriver.gecko.driver", geckodriverPath);
            webDriver = new FirefoxDriver(fo);
        }catch (Exception e){
            logger.error("创建浏览器实例失败",e);
        }
    }

    public  Set<Cookie> getCookieA() {
        Set<Cookie> cookieSet = null;
        try {
            webDriver.manage().deleteAllCookies();
            logger.info("开始获取RAIL_DEVICEID等cookie");
            webDriver.get("https://kyfw.12306.cn/otn/resources/login.html");
            cookieSet = webDriver.manage().getCookies();
            boolean completed = false;
            while (!completed) {
                for (Cookie cookie : cookieSet) {
                    if (cookie.getName().equalsIgnoreCase("RAIL_DEVICEID")) {
                        completed = true;
                    }
                }
                Thread.sleep(300L);
                cookieSet = webDriver.manage().getCookies();
            }
//            webDriver.close();
        } catch (Exception e) {
            logger.error("获取cookie失败", e);
            resetWebDriver();
        }
        logger.info("RAIL_DEVICEID cookie信息获取结束");
        return cookieSet;
    }

    @PreDestroy
    public void preDestroy(){
        logger.info("开始退出浏览器");
        webDriver.quit();
        logger.info("浏览器已退出");
    }


    public static void main(String[] args) {
//        ChromeDriverUtil.init1();
//        ChromeDriverUtil.init();

//        WebDriver driver =getChromeDriver();
    }

} 
