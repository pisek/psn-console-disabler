package cf.pisek.notifiers;

import cf.pisek.Notifier;
import cf.pisek.User;

import static cf.pisek.AnsiColors.*;

public class ConsoleNotifier implements Notifier {

	@Override
	public void yes(User user, String text) {
		System.out.println(BG_BLACK + FG_GREEN + text + RESET);
	}

	@Override
	public void error(User user, String text) {
		System.out.println(BG_BLACK + FG_RED + text + RESET);
	}

	@Override
	public void no(User user, String text) {
		System.out.println(BG_BLACK + FG_YELLOW + text + RESET);
	}

}
