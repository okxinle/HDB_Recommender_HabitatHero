public class UserAccount {

	private int userID;
	private String email;
	private String passwordHash;
	private DateTime createdAt;

	/**
	 * 
	 * @param userID
	 * @param email
	 * @param passwordHash
	 * @param createdAt
	 */
	public UserAccount(int userID, String email, String passwordHash, DateTime createdAt) {
		// TODO - implement UserAccount.UserAccount
		throw new UnsupportedOperationException();
	}

	public int getUserID() {
		return this.userID;
	}

	public String getEmail() {
		return this.email;
	}

}