package cf.pisek.notifiers;

import cf.pisek.Notifier;
import cf.pisek.User;

import static cf.pisek.AnsiColors.*;

public class ConsoleNotifier implements Notifier {

	@Override
	public void yes(User user, String text) {
		System.out.println(ANSI_GREEN+text+ANSI_RESET);
	}

	@Override
	public void error(User user, String text) {
		System.out.println(ANSI_RED+text+ANSI_RESET);
	}

	@Override
	public void no(User user, String text) {
		System.out.println(ANSI_YELLOW+text+ANSI_RESET);
	}

}
