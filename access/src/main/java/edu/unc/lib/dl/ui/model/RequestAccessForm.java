package edu.unc.lib.dl.ui.model;

public class RequestAccessForm {
	private String personalName;
	private String emailAddress;
	private String phoneNumber;
	private String comments;
	private String username;
	private String affiliation;
	private String requestedId;
	private String requestedTitle;
	private String recaptcha_challenge_field;
	private String recaptcha_response_field;
	private String remoteAddr;

	public String getPersonalName() {
		return personalName;
	}

	public void setPersonalName(String personalName) {
		this.personalName = personalName;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getAffiliation() {
		return affiliation;
	}

	public void setAffiliation(String affiliation) {
		this.affiliation = affiliation;
	}

	public String getRequestedId() {
		return requestedId;
	}

	public void setRequestedId(String requestedId) {
		this.requestedId = requestedId;
	}

	public String getRequestedTitle() {
		return requestedTitle;
	}

	public void setRequestedTitle(String requestedTitle) {
		this.requestedTitle = requestedTitle;
	}

	public String getRecaptcha_challenge_field() {
		return recaptcha_challenge_field;
	}

	public void setRecaptcha_challenge_field(String recaptcha_challenge_field) {
		this.recaptcha_challenge_field = recaptcha_challenge_field;
	}

	public String getRecaptcha_response_field() {
		return recaptcha_response_field;
	}

	public void setRecaptcha_response_field(String recaptcha_response_field) {
		this.recaptcha_response_field = recaptcha_response_field;
	}

	public String getRemoteAddr() {
		return remoteAddr;
	}

	public void setRemoteAddr(String remoteAddr) {
		this.remoteAddr = remoteAddr;
	}
}
