package cf.pisek;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import cf.pisek.exceptions.LoginException;

public class Disabler {

	public static void main(String[] args) throws Exception {
		
		Options options = new Options();
		options.addOption("u", true, "email");
		options.addOption("p", true, "password");
		options.addOption("t", true, "try periodically (seconds)");
		options.addOption("d", false, "try to disable console (default: only checks if it is possible and notifies)");

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);
		
		Notifier not = new ConsoleNotifier();
		
		User user = null;
		if (cmd.hasOption("u") && cmd.hasOption("p")) {
			user = new User(cmd.getOptionValue("u"), cmd.getOptionValue("p"));
			boolean tryDisable = cmd.hasOption("d");
		
			if (cmd.hasOption("t")) {
				// continously
				int seconds = Integer.parseInt(cmd.getOptionValue("t"));
				while (true) {
					
					checkDisablingTheConsole(not, user, tryDisable);
					
					Thread.sleep(seconds * 1000);
				}
			} else {
				// only once
				
				checkDisablingTheConsole(not, user, tryDisable);
				
			}
			
		} else {
			
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("disabler", options );
			
		}
		
	}
	
	private static Connection generateConnection(String url, Map<String, String> cookies) {
		Connection con = Jsoup.connect(url);
		con.header("User-Agent", "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36");
		con.validateTLSCertificates(false);
		con.timeout(10000);
		con.cookies(cookies);
		System.out.println("Cookies: " + Arrays.toString(cookies.entrySet().toArray()));
		return con;
	}

	private static void checkDisablingTheConsole(Notifier not, User user, boolean tryDisable) throws Exception {
		Connection con;
		Document doc;
		Map<String, String> cookies = new HashMap<> ();
		
		
		boolean isDisablingPossible = true;
		
		try(PrintWriter log = new PrintWriter("log_"+ DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now()) +".html")){

			// init
			con = generateConnection("https://account.sonyentertainmentnetwork.com/login.action", cookies);
			log.println("<h1>LOG IN</h1>");
			log.println(con.get());
			
			cookies.putAll(con.response().cookies());
			sleep();
			
			
			// log in
			con = generateConnection("https://account.sonyentertainmentnetwork.com/liquid/j_spring_security_check", cookies);
			con.data("j_username", user.getUser());
			con.data("j_password", user.getPassword());
			con.data("service-entity", "np");
			log.println("<h1>LOGGED IN - ACCOUNT INFO</h1>");
			doc = con.post();
			log.println(doc);

			cookies.putAll(con.response().cookies());
			sleep();
			
			
			System.out.println("Cookies: " + Arrays.toString(cookies.entrySet().toArray()));
			
			// check login status
			if (!cookies.containsKey("rememberSignIn")) {
				throw new LoginException();
			}
			
			
			// check disable button/error box
			con = generateConnection("https://account.sonyentertainmentnetwork.com/liquid/cam/devices/device-media-list.action", cookies);
			log.println("<h1>DEVICE MEDIA LIST</h1>");
			doc = con.get();
			log.println(doc);
			
			Element gameMediaDevicesDeactivateSection = doc.getElementById("gameMediaDevicesDeactivateSection");
			Element errorLabel = gameMediaDevicesDeactivateSection.getElementById("toutLabel");
			if (errorLabel != null) {
				isDisablingPossible = false;
			}
			
			cookies.putAll(con.response().cookies());
			sleep();
			
			
			
			if (isDisablingPossible) {
				
				if (tryDisable) {
					
					// GET https://account.sonyentertainmentnetwork.com/liquid/cam/account/devices/media-devices-confirm-deactivate.action
					con = generateConnection("https://account.sonyentertainmentnetwork.com/liquid/cam/account/devices/media-devices-confirm-deactivate.action", cookies);
					log.println("<h1>DEACTIVATION CONFIRMATION</h1>");
					log.println(con.get());
					
					cookies.putAll(con.response().cookies());
					sleep();
					
					
					// odpalamy wylaczenie
					
					// POST https://account.sonyentertainmentnetwork.com/liquid/cam/devices/clear-domain.action
					con = generateConnection("https://account.sonyentertainmentnetwork.com/liquid/cam/devices/clear-domain.action", cookies);
					log.println("<h1>DEACTIVATION DONE!</h1>");
					log.println(con.post());
					
					cookies.putAll(con.response().cookies());
					sleep();
					
					
					// check the status of disabling the console (check again disable button/error box)
					con = generateConnection("https://account.sonyentertainmentnetwork.com/liquid/cam/devices/device-media-list.action", cookies);
					log.println("<h1>DEVICE MEDIA LIST</h1>");
					doc = con.get();
					log.println(doc);
					
					gameMediaDevicesDeactivateSection = doc.getElementById("gameMediaDevicesDeactivateSection");
					errorLabel = gameMediaDevicesDeactivateSection.getElementById("toutLabel");
					if (errorLabel != null) {
						not.yes("Success - Disabling was performed!");
					} else {
						not.yes("Failed to perform console disabling... Try manually, because it is still possible...");
					}
					
					cookies.putAll(con.response().cookies());
					sleep();
					
				} else {
					
					not.yes("Success - It is possible to disable the console!");
					
				}
					
			} else {
				
				not.no("Failed: " + errorLabel.text());
				
			}
			
				
				
			// log out (wymagany aby ponownie sie zalogowac!
			con = generateConnection("https://account.sonyentertainmentnetwork.com/liquid/j_spring_security_logout", cookies);
			log.println("<h1>LOG OUT</h1>");
			log.println(con.get());
			
			cookies.putAll(con.response().cookies());
			sleep();
			
		} catch (LoginException e) {
			
			not.error("Could not login - possible wrong credentials.");
		
		} catch (HttpStatusException e) {
			
			not.error("Could not access website - possible BAN for too many connections...\n" + e);
			
		}
		
	}

	private static void sleep() throws InterruptedException {
		Thread.sleep(1000 + new Random().nextInt(2000));		
	}

}
