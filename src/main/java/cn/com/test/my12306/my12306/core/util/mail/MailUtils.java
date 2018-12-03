package cn.com.test.my12306.my12306.core.util.mail;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class MailUtils {
	private static Logger logger = LogManager.getLogger(MailUtils.class);

	public static String emailHost = "mail.139.com";// 邮件地址

	public static String emailName = "订票";// 邮件名

	public static String emailUsername = "用户名";// 邮件用户

	public static String emailPassword = "邮件密码";// 邮件密码

	public static String emailSender = "864654233@qq.com";// 发送者

	public static String emailReceiver = "864654233@qq.com";// 接收者

	@Value("${emailHost}")
	public void setEmailHost(String emailHost) {
		this.emailHost = emailHost;
	}
	@Value("${emailName}")
	public void setEmailName(String emailName) {
		this.emailName = emailName;
	}
	@Value("${emailUsername}")
	public void setEmailUsername(String emailUsername) {
		this.emailUsername = emailUsername;
	}
	@Value("${emailPassword}")
	public void setEmailPassword(String emailPassword) {
		this.emailPassword = emailPassword;
	}
	@Value("${emailSender}")
	public void setEmailSender(String emailSender) {
		this.emailSender = emailSender;
	}

	@Value("${emailReceiver}")
	public void setEmailReceiver(String emailReceiver) {
		this.emailReceiver = emailReceiver;
	}

	public boolean send(String msg) {
		// 发送email
		HtmlEmail email = new HtmlEmail();
		try {
			// 这里是SMTP发送服务器的名字：163的如下："smtp.163.com"
			email.setHostName(emailHost);
			email.setSubject("订票通知");
			// 字符编码集的设置
			email.setCharset(Mail.ENCODEING);
			// 收件人的邮箱
			email.addTo(emailReceiver);
			// 发送人的邮箱
			email.setFrom(emailSender);


//			email.setFrom(mail.getSender(), mail.getName());
			// 如果需要认证信息的话，设置认证：用户名-密码。分别为发件人在邮件服务器上的注册名称和密码
			email.setAuthentication(emailUsername, emailPassword);
			// 要发送的邮件主题
			// 要发送的信息，由于使用了HtmlEmail，可以在邮件内容中使用HTML标签
			email.setMsg(msg);
			// 发送
			email.send();
			if (logger.isDebugEnabled()) {
				logger.debug(emailSender + " 发送邮件到 " + emailReceiver);
			}
			return true;
		} catch (EmailException e) {
			e.printStackTrace();
			logger.info(emailSender + " 发送邮件到 " + emailReceiver + " 失败");
			return false;
		}
	}


}
