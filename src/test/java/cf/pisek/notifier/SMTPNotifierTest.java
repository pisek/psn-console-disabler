package cf.pisek.notifier;

import org.junit.Test;

import cf.pisek.User;
import cf.pisek.notifiers.SMTPNotifier;

public class SMTPNotifierTest {
	
	@Test
	public void testYes() {
		new SMTPNotifier().yes(new User("dupa@dupa.com", "pass"), "Testowa dupa");
	}
	
	@Test
	public void testError() {
		new SMTPNotifier().error(new User("dupa@dupa.com", "pass"), "Testowa dupa");
	}

}
