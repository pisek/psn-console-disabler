package cf.pisek;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class Disabler {

	public static void main(String[] args) throws Exception {
		
		Options options = new Options();
		options.addOption("u", true, "email");
		options.addOption("p", true, "password");
		options.addOption("t", true, "try periodically (seconds)");

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);
		
		User user = null;
		if (cmd.hasOption("u") && cmd.hasOption("p")) {
			user = new User(cmd.getOptionValue("u"), cmd.getOptionValue("p"));
		}
		
		if (cmd.hasOption("t")) {
			// continously
			int seconds = Integer.parseInt(cmd.getOptionValue("t"));
			while (true) {
				
				tryDisablingTheConsole(user);
				
				Thread.sleep(seconds * 1000);
			}
		} else {
			// only once
			
			tryDisablingTheConsole(user);
			
		}
		
	}

	private static void tryDisablingTheConsole(User user) throws IOException {

		Connection con = Jsoup.connect("https://account.sonyentertainmentnetwork.com/login.action?category=psn&displayNavigation=false");
		con.header("User-Agent", "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36");
		con.get();
		
		con.url("https://account.sonyentertainmentnetwork.com/liquid/j_spring_security_check");
		con.data("j_username", user.getUser());
		con.data("j_password", user.getPassword());
		con.data("service-entity", "np");
		con.post();
		
		//already logged in
		
		// go to https://account.sonyentertainmentnetwork.com/liquid/cam/account/devices/media-devices-confirm-deactivate.action
		
		// sprawdzamy czy istnieje button pozwalajacy na wylaczenie konsol
		
		// odpalamy wylaczenie
		
	}

}
