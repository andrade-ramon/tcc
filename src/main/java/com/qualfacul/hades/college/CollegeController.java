package com.qualfacul.hades.college;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.qualfacul.hades.annotation.Delete;
import com.qualfacul.hades.annotation.Get;
import com.qualfacul.hades.annotation.OnlyAdmins;
import com.qualfacul.hades.annotation.OnlyColleges;
import com.qualfacul.hades.annotation.OnlyStudents;
import com.qualfacul.hades.annotation.Post;
import com.qualfacul.hades.annotation.PublicEndpoint;
import com.qualfacul.hades.converter.ListConverter;
import com.qualfacul.hades.course.CourseDTO;
import com.qualfacul.hades.course.CourseToDTOConverter;
import com.qualfacul.hades.exceptions.CollegeAlreadyHaveLoginException;
import com.qualfacul.hades.exceptions.CollegeNotFoundException;
import com.qualfacul.hades.exceptions.CollegeUpdateAccessDeniedException;
import com.qualfacul.hades.exceptions.CollegeWithoutLoginAccessException;
import com.qualfacul.hades.exceptions.UsernameNotFoundException;
import com.qualfacul.hades.login.LoggedUserManager;
import com.qualfacul.hades.login.LoginInfoRepository;
import com.qualfacul.hades.search.PaginatedResult;
import com.qualfacul.hades.search.SearchQuery;
import com.qualfacul.hades.user.User;
import com.qualfacul.hades.user.UserRepository;

@RestController
public class CollegeController {
	
	private static final float COLLEGE_THRESHOLD = 0.4f;
	private static final String REGEXP_ONLY_NUMBERS = "[^0-9]";
	
	@Autowired
	private SearchQuery<College, CollegeDTO> collegeSearch;
	@Autowired
	private CollegeRepository collegeRepository;	
	@Autowired
	private CollegeAddressRepository collegeAddressRepository;
	@Autowired
	private CollegeAddressToDTOConverter addressConverter;
	@Autowired
	private CollegeToCollegeDTOConverter collegeConverter;
	@Autowired
	private CourseToDTOConverter courseConverter;
	@Autowired
	private LoggedUserManager loggedUserManager;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private LoginInfoRepository loginInfoRepository;
	@Autowired
	private CollegeGradeToDTOConverter gradeConverter;
	
	@PublicEndpoint
	@Get("/colleges/{id}")
	public CollegeDTO show(@PathVariable Long id){
		return collegeRepository.findById(id)
								.map(college -> collegeConverter.convert(college))
								.orElseThrow(CollegeNotFoundException::new);
	}
	
	@PublicEndpoint
	@Get("/colleges/{collegeId}/addresses/{addressId}")
	public CollegeAddressDTO showAddress(@PathVariable Long collegeId, @PathVariable Long addressId){
		return collegeAddressRepository.findByIdAndCollegeId(addressId, collegeId)
								.map(collegeAddress -> addressConverter.convert(collegeAddress))
								.orElseThrow(CollegeAddressNotFoundException::new);
	}
	
	@PublicEndpoint
	@Get("/colleges/{collegeId}/addresses")
	public List<CollegeAddressDTO> collegeAddressesSearch(@PathVariable Long collegeId){
		return collegeAddressRepository.findAllByCollegeId(collegeId).stream()
				.map(collegeAddress -> addressConverter.convert(collegeAddress))
				.collect(Collectors.toList());
	}
	
	@PublicEndpoint
	@Get("/colleges/{collegeId}/courses")
	public List<CourseDTO> listAllCourses(@PathVariable Long collegeId){
		return collegeAddressRepository.findAllByCollegeId(collegeId).stream()
				.flatMap(address -> address.getCourses().stream())
				.map(course -> courseConverter.convert(course))
				.collect(Collectors.toList());
	}
	
	@OnlyStudents
	@Post("/colleges/{collegeId}/assign")
	public void assignStudent(@RequestBody UserCollegeAddressDTO dto) {
		CollegeAddress collegeAddress = collegeAddressRepository.findByIdAndCollegeId(dto.getCollegeAddressId(), dto.getCollegeId())
						.orElseThrow(CollegeAddressNotFoundException::new);
		User student = loggedUserManager.getStudent().orElseThrow(UsernameNotFoundException::new);
		
		student.assignCollege(collegeAddress, dto.getStudentRa());
		userRepository.save(student);
	}
	
	@OnlyStudents
	@Get("/colleges/{collegeId}/ratings")
	public List<CollegeGradeDTO> listRatings(@PathVariable Long collegeId) {
		return collegeRepository.findById(collegeId).orElseThrow(CollegeNotFoundException::new)
						.getGrades().stream()
						.filter(college -> college.getGradeOrigin().isFromStudent())
						.map(gradeConverter::convert)
						.collect(Collectors.toList());
	}
	
	@OnlyStudents
	@Post("/colleges/{collegeId}/ratings")
	public void rate(@PathVariable Long collegeId, @RequestBody SimpleCollegeGradeDTO dto) {
		User student = loggedUserManager.getStudent().orElseThrow(UsernameNotFoundException::new);
		College college = collegeRepository.findById(collegeId).orElseThrow(CollegeNotFoundException::new);
		college.rate(student, dto.getOrigin(), dto.getValue());
		collegeRepository.save(college);
	}
	
	@OnlyAdmins
	@Post("/colleges/{collegeId}/login")
	public void createLoginInfo(@PathVariable Long collegeId, @Valid @RequestBody CollegeLoginDTO dto){
		College college = collegeRepository.findById(collegeId).orElseThrow(CollegeNotFoundException::new);
		if (college.getLoginInfo().isPresent()){
			throw new CollegeAlreadyHaveLoginException();
		}
		college.createLogin(collegeRepository, dto.getPassword());
	}
	
	@OnlyAdmins
	@Delete("/colleges/{collegeId}/login")
	public void deleteLoginInfo(@PathVariable Long collegeId){
		College college = collegeRepository.findById(collegeId).orElseThrow(CollegeNotFoundException::new);
		if (!college.getLoginInfo().isPresent()){
			throw new CollegeWithoutLoginAccessException();
		}
		college.removeLogin(loginInfoRepository, collegeRepository);
	}
	
	@OnlyColleges
	@Post("/colleges/{collegeId}")
	public void updateCollege(@PathVariable Long collegeId, @RequestBody CollegeDTO collegeDTO){
		College college = collegeRepository.findById(collegeId).orElseThrow(CollegeNotFoundException::new);
		if (!college.getLoginInfo().isPresent()){
			throw new CollegeUpdateAccessDeniedException();
		}
		if (college.getLoginInfo().get().getId() != loggedUserManager.getLoginInfo().getId()){
			throw new CollegeUpdateAccessDeniedException();
		}
		if (!StringUtils.isEmpty(collegeDTO.getName()))
			college.setName(collegeDTO.getName());
		
		if (!StringUtils.isEmpty(collegeDTO.getPhone()))
			college.setPhone(collegeDTO.getPhone().replaceAll(REGEXP_ONLY_NUMBERS, ""));
		
		if (!StringUtils.isEmpty(collegeDTO.getCnpj()))
			college.setCnpj(collegeDTO.getCnpj().replaceAll(REGEXP_ONLY_NUMBERS, ""));
		
		if (!StringUtils.isEmpty(collegeDTO.getSite()))
			college.setSite(collegeDTO.getSite());
		
		collegeRepository.save(college);
	}
	
	@PublicEndpoint
	@Get("/colleges/search/{query}")
	public PaginatedResult<CollegeDTO> list(@PathVariable String query, @RequestParam(required = false) Integer page) {
		ListConverter<College, CollegeDTO> listConverter = new ListConverter<>(collegeConverter);
		
		PaginatedResult<CollegeDTO> dtos = collegeSearch
			.builder()
			.forEntity(College.class)
			.withThreshold(COLLEGE_THRESHOLD)
			.matching(query)
			.forPage(page)
			.withListConverter(listConverter)
			.build();
		
		return dtos;
	}

}
