package cf.pisek;

public class ConsoleNotifier implements Notifier {

	@Override
	public void yes(String text) {
		System.out.println(text);
	}

	@Override
	public void error(String text) {
		System.err.println(text);
	}

	@Override
	public void no(String text) {
		System.out.println(text);
	}

}
