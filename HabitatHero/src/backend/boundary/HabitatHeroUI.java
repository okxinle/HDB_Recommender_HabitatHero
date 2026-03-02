package boundary;

import control.IPasswordMgr;
import entity.UserAccount;
import entity.UserProfile;
import repository.UserAccountDbMgr;
import java.util.Scanner;

public class HabitatHeroUI {

	private Scanner sc;
	private UserAccountDbMgr accountDbMgr;
	private UserProfileDbMgr profileDbMgr;
	private IPasswordMgr passwordMgr;
	private UserAccount currentUser;

	/**
	 * 
	 * @param sc
	 * @param accountDbMgr
	 * @param profileDbMgr
	 * @param passwordMgr
	 */
	public HabitatHeroUI(Scanner sc, UserAccountDbMgr accountDbMgr, UserProfileDbMgr profileDbMgr, IPasswordMgr passwordMgr) {
		// TODO - implement HabitatHeroUI.HabitatHeroUI
		throw new UnsupportedOperationException();
	}

	/**
	 * 
	 * @param email
	 * @param password
	 */
	public UserAccount registerAccount(String email, String password) {
		// TODO - implement HabitatHeroUI.registerAccount
		throw new UnsupportedOperationException();
	}

	public boolean accountLogin() {
		// TODO - implement HabitatHeroUI.accountLogin
		throw new UnsupportedOperationException();
	}

	/**
	 * 
	 * @param profile
	 */
	public void saveProfile(UserProfile profile) {
		// TODO - implement HabitatHeroUI.saveProfile
		throw new UnsupportedOperationException();
	}

	public UserProfile retrieveProfile() {
		// TODO - implement HabitatHeroUI.retrieveProfile
		throw new UnsupportedOperationException();
	}

}