package com.axelor.auth;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.crypto.hash.DefaultHashService;
import org.apache.shiro.crypto.hash.format.ParsableHashFormat;
import org.apache.shiro.crypto.hash.format.Shiro1CryptFormat;

import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.google.common.base.Objects;
import com.google.inject.persist.Transactional;

@Singleton
public class AuthService {

	private final DefaultPasswordService passwordService = new DefaultPasswordService();

	private final DefaultHashService hashService = new DefaultHashService();

	private final ParsableHashFormat hashFormat = new Shiro1CryptFormat();

	public static AuthService instance;

	@Inject
	public AuthService(
			@Named("auth.hash.algorithm") String hashAlgorihm,
			@Named("auth.hash.iterations") int hashIterations) {
		super();

		this.hashService.setHashAlgorithmName(hashAlgorihm);
		this.hashService.setHashIterations(hashIterations);
		this.hashService.setGeneratePublicSalt(true);

		this.passwordService.setHashService(hashService);
		this.passwordService.setHashFormat(hashFormat);

		if (instance != null) {
			throw new RuntimeException("AuthService initialized twice.");
		}
		instance = this;
	}

	public static AuthService getInstance() {
		if (instance == null) {
			throw new IllegalStateException("AuthService is not initialized, did you forget to bind the AuthService?");
		}
		return instance;
	}

	public String encrypt(String password) {
		try {
			hashFormat.parse(password);
			return password;
		} catch (IllegalArgumentException e) {
		}
		return passwordService.encryptPassword(password);
	}

	public User encrypt(User user) {
		user.setPassword(encrypt(user.getPassword()));
		return user;
	}

	public Object encrypt(Object user, @SuppressWarnings("rawtypes") Map context) {
		if (user instanceof User) {
			return encrypt((User) user);
		}
		return user;
	}

	public boolean match(String plain, String saved) {
		//TODO: remove plain text match in final version
		if (Objects.equal(plain, saved)) { // plain text match
			return true;
		}
		return passwordService.passwordsMatch(plain, saved);
	}

	@Transactional
	public void validate(ActionRequest request, ActionResponse response) {
		Context context = request.getContext();
		User user = context.asType(User.class);
		if (context.get("confirm") == null) {
			return;
		}

		String password = (String) context.get("newPassword");
		String confirm = (String) context.get("confirm");

		if (Objects.equal(password, confirm)) {
			password = encrypt(password);
			user.setPassword(password);
			user.save();
			response.setValue("id", user.getId());
			response.setReload(true);
		} else {
			response.addError("confirm", JPA.translate("Password doesn't match"));
		}
	}
}
