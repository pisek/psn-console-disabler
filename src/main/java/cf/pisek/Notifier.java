package cf.pisek;

public interface Notifier {
	
	void yes(User user, String text);
	void no(User user, String text);
	void error(User user, String text);

}
