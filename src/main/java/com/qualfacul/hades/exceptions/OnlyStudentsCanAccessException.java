package com.qualfacul.hades.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.FORBIDDEN, reason = "Only students can do this operation")
public class OnlyStudentsCanAccessException extends RuntimeException {
	private static final long serialVersionUID = 1L;
}
