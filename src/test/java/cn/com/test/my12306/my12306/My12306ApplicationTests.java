package cn.com.test.my12306.my12306;

import cn.com.test.my12306.my12306.core.util.DateUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.text.ParseException;
import java.util.Date;

//@RunWith(SpringRunner.class)
//@SpringBootTest
public class My12306ApplicationTests {



    public static void main(String[] args) {
        try {
            System.out.println( DateUtil.getCurrentDay(new Date()));
            System.out.println( DateUtil.getDaysAfter(DateUtil.getCurrentDay(new Date()),-29));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

}
