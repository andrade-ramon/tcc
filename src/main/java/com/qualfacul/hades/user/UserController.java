package com.qualfacul.hades.user;

import static com.qualfacul.hades.login.LoginOrigin.USER;
import static org.springframework.http.HttpStatus.CREATED;

import java.util.Optional;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.qualfacul.hades.annotation.Get;
import com.qualfacul.hades.annotation.Patch;
import com.qualfacul.hades.annotation.Post;
import com.qualfacul.hades.annotation.PublicEndpoint;
import com.qualfacul.hades.configuration.security.TokenAuthenticationService;
import com.qualfacul.hades.exceptions.AccessDeniedException;
import com.qualfacul.hades.exceptions.EmailAlreadyInUseException;
import com.qualfacul.hades.exceptions.UsernameNotFoundException;
import com.qualfacul.hades.login.LoggedUserManager;
import com.qualfacul.hades.login.LoginInfo;
import com.qualfacul.hades.login.LoginInfoDTO;

@RestController
public class UserController {

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private LoggedUserManager loggedUserManager;

	@Autowired
	private TokenAuthenticationService tokenService;

	@Transactional
	@PublicEndpoint
	@Post(value = "/register", responseStatus = CREATED)
	public LoginInfoDTO register(@Valid @RequestBody UserDTO userDTO) throws EmailAlreadyInUseException {
		User user;
		Optional<User> optionalUser = userRepository.findByEmail(userDTO.getEmail());

		if (optionalUser.isPresent()) {
			user = optionalUser.get();
			if (user.getLoginInfo().isFromFacebook()) {
				user.getLoginInfo().setLoginOrigin(USER);
				user.getLoginInfo().setPassword(userDTO.getPassword());
			} else {
				throw new EmailAlreadyInUseException();
			}
		} else {
			LoginInfo loginInfo = new LoginInfo(userDTO.getEmail(), userDTO.getPassword(), USER);
			user = new User(userDTO.getName(), userDTO.getEmail(), loginInfo);
		}
		userRepository.save(user);
		LoginInfo loginInfo = user.getLoginInfo();
		tokenService.createTokenFor(loginInfo);
		return new LoginInfoDTO().from(loginInfo);
	}
	
	@Patch("/user/preferences")
	public void updateUser(@Valid @RequestBody UserSettingsDTO userSettingsDTO){
		User user = userRepository.findByLoginInfo(loggedUserManager.getLoginInfo())
									.orElseThrow(UsernameNotFoundException::new);
		boolean hasChanges = false;
		if (user.getLoginInfo().getId() != loggedUserManager.getLoginInfo().getId()){
			throw new AccessDeniedException();
		}
		if (!StringUtils.isEmpty(userSettingsDTO.getName())){
			user.setName(userSettingsDTO.getName());
			hasChanges = true;
		}
		if (!StringUtils.isEmpty(userSettingsDTO.getPassword())){
			user.getLoginInfo().setPassword(userSettingsDTO.getPassword());
			hasChanges = true;
		}
		if (hasChanges){
			userRepository.save(user);
		}
	}
	
	@Get("/users/current")
	public User getCurrentUser() {
		return userRepository.findByEmail(loggedUserManager.getLoginInfo().getLogin()).orElseThrow(UsernameNotFoundException::new);
	}

}