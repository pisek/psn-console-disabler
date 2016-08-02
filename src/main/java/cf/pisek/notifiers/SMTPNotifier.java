package cf.pisek.notifiers;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

import cf.pisek.Notifier;
import cf.pisek.User;

public class SMTPNotifier implements Notifier {
	
	private Properties prop = new Properties();

	public SMTPNotifier() {
		try {
			prop.load(new FileInputStream("disabler.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void yes(User user, String text) {
		System.out.println("YES: " + text);
		
		try {
			
			SimpleEmail email = new SimpleEmail();
			
			email.setHostName(prop.getProperty("host"));
			email.setSmtpPort(Integer.parseInt(prop.getProperty("port")));
			email.setAuthenticator(new DefaultAuthenticator(prop.getProperty("login"), prop.getProperty("password")));
			email.setSSLOnConnect(true);
			
			email.setFrom(prop.getProperty("login"), prop.getProperty("fromName"));
			email.addTo(prop.getProperty("toEmail"), prop.getProperty("toName"));
			email.setSubject("Success: Account " + user.getUser() + " was processed");
			email.setMsg(text);
			email.send();
			
		} catch (EmailException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void no(User user, String text) {
		System.out.println("NO: " + text);
	}

	@Override
	public void error(User user, String text) {
		System.err.println("ERROR: " + text);
		
		try {
			
			SimpleEmail email = new SimpleEmail();
			
			email.setHostName(prop.getProperty("host"));
			email.setSmtpPort(Integer.parseInt(prop.getProperty("port")));
			email.setAuthenticator(new DefaultAuthenticator(prop.getProperty("login"), prop.getProperty("password")));
			email.setSSLOnConnect(true);
			
			email.setFrom(prop.getProperty("login"), prop.getProperty("fromName"));
			email.addTo(prop.getProperty("toEmail"), prop.getProperty("toName"));
			email.setSubject("Error: Account " + user.getUser() + " was processed with error");
			email.setMsg(text);
			email.send();
			
		} catch (EmailException e) {
			e.printStackTrace();
		}
		
	}

}
