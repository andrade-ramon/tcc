package com.qualfacul.hades.login;


interface LoginServiceCallback {
	
	void onSuccess(LoginInfo loginInfo);
	
	void onError(LoginInfo loginInfo);

}
