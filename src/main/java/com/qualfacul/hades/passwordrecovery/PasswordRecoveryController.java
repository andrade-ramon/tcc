package com.qualfacul.hades.passwordrecovery;

import java.util.Date;
import java.util.Optional;
import com.qualfacul.hades.configuration.security.TokenAuthenticationService;
import com.qualfacul.hades.exceptions.FacebookAccountException;
import com.qualfacul.hades.login.LoginInfo;
import com.qualfacul.hades.login.LoginInfoRepository;
import com.qualfacul.hades.login.LoginOrigin;
import com.qualfacul.hades.mail.EmailAddress;
import com.qualfacul.hades.mail.SmtpEmailSender;

import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.qualfacul.hades.annotation.PermitEndpoint;
import com.qualfacul.hades.annotation.Post;

@RestController
public class PasswordRecoveryController {
	//EXPIRATION_TIME must be in minutes
	public static int EXPIRATION_TIME = 60*24;
	
	@Autowired
	private TokenAuthenticationService tokenAuthenticationService;
	@Autowired
	private LoginInfoRepository loginInfoRepository;
	@Autowired
	private SmtpEmailSender emailDelivery;
	
	@PermitEndpoint
	@Post("/account/password/recovery")
	public boolean requestToken(@RequestBody @NotBlank String login) throws FacebookAccountException {
		System.out.println(login+"\n");
		Optional<LoginInfo> optionalLoginInfo = loginInfoRepository.findByLogin(login);
		if (optionalLoginInfo.isPresent()){
			LoginInfo loginInfo = optionalLoginInfo.get();
			if (loginInfo.getLoginOrigin() == LoginOrigin.FACEBOOK){
				throw new FacebookAccountException();
			}
			Date expirationDate = new Date();
			expirationDate = DateUtils.addMinutes(expirationDate, EXPIRATION_TIME);
			String userToken = tokenAuthenticationService.createTokenFor(loginInfo, expirationDate);
			StringBuilder message = new StringBuilder();
			message.append("Ol�!<br/>Para redefinir sua senha, clique ");
			message.append("<a href=\"http://dev.qualfacul.com:9000/redefinirsenha/"+userToken+"\">");
			message.append("aqui</a><br/>");
			message.append("Este link � v�lido por 24h, ap�s esse per�odo, ser� necess�rio ");
			message.append("solicitar um novo link atrav�s do site.");
			EmailAddress email = new EmailAddress(loginInfo.getLogin());
			return emailDelivery.send("Recupera��o de Senha", message, email);
		}
		return true;
	}
	
}