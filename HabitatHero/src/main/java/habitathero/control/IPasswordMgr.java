package habitathero.control;
import habitathero.entity.UserAccount;

public interface IPasswordMgr {

	/**
	 * 
	 * @param password
	 */
	String hashPassword(String password);

	/**
	 * 
	 * @param user
	 * @param password
	 */
	boolean validate(UserAccount user, String password);

}