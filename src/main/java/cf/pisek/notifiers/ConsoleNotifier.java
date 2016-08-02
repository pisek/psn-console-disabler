package cf.pisek.notifiers;

import cf.pisek.Notifier;
import cf.pisek.User;

public class ConsoleNotifier implements Notifier {

	@Override
	public void yes(User user, String text) {
		System.out.println(text);
	}

	@Override
	public void error(User user, String text) {
		System.err.println(text);
	}

	@Override
	public void no(User user, String text) {
		System.out.println(text);
	}

}
