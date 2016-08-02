package cf.pisek;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class User {
	
	private static final Logger log = LogManager.getLogger();

	private String user;
	private String password;

	public User(String user, String password) {
		this.user = user;
		this.password = password;
		log.debug("Username: ["+user+"]");
		log.debug("Password: ["+password+"]");
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
