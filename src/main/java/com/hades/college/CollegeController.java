package com.hades.college;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.springframework.http.HttpStatus.CREATED;

import java.util.Optional;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hades.annotation.InternalEndpoint;
import com.hades.annotation.Post;
import com.hades.college.converter.CollegeMecDTOToCollegeConverter;
import com.hermes.college.CollegeMecDTO;

@RestController
public class CollegeController {

	@Autowired
	private CollegeMecDTOToCollegeConverter converter;

	@Autowired
	private CollegeRepository repository;

	@Post("/admin/colleges")
	@InternalEndpoint
	@ResponseStatus(CREATED)
	public Long updateOrSave(@Valid @RequestBody CollegeMecDTO dto) {
		College collegeToMerge = converter.convert(dto);

		Optional<College> optionalCollege = repository.findByCnpj(collegeToMerge.getCnpj());

		optionalCollege.ifPresent(college -> {
			if (equalsIgnoreCase(collegeToMerge.getCnpj(), college.getCnpj())) {
				collegeToMerge.setId(college.getId());
			}
		});

		repository.save(collegeToMerge);

		return collegeToMerge.getId();
	}
}