package cn.com.test.my12306.my12306;

import cn.com.test.my12306.my12306.core.ClientTicket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class My12306Application implements CommandLineRunner {

	@Autowired
	ClientTicket ct;
	public static void main(String[] args) {
//		SpringApplication.run(My12306Application.class, args);
		SpringApplicationBuilder builder = new SpringApplicationBuilder(My12306Application.class);
		builder.headless(false).web(false).run(args);
	}


	@Override
	public void run(String... args) throws Exception {
//		Thread.sleep(5000);
		System.out.println("开始执行");
//		ClientTicket ct = new ClientTicket();
		ct.run();
	}
}
