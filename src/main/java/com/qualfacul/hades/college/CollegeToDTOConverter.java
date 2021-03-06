package com.qualfacul.hades.college;

import static com.qualfacul.hades.college.CollegeGradeOrigin.MEC_CI;
import static org.apache.commons.lang3.StringUtils.trim;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;

import com.qualfacul.hades.annotation.WebComponent;
import com.qualfacul.hades.login.LoggedUserManager;
import com.qualfacul.hades.user.User;

@WebComponent
public class CollegeToDTOConverter implements Converter<College, CollegeDTO>{
	
	private LoggedUserManager loggedUserManager;

	@Autowired
	public CollegeToDTOConverter(LoggedUserManager loggedUserManager) {
		this.loggedUserManager = loggedUserManager;
	}

	@Override
	public CollegeDTO convert(College from) {
		
		CollegeDTO dto = new CollegeDTO();
		dto.setId(from.getId());
		dto.setName(from.getName());
		dto.setInitials(from.getInitials());
		dto.setPhone(from.getPhone());
		dto.setCnpj(from.getCnpj());
		dto.setSite(from.getSite());
		
		for (CollegeGrade grade : from.getGrades()) {
			if(MEC_CI.equals(grade.getGradeOrigin())){
				dto.setMecGrade(grade.getValue());
			}
		}
		
		Integer collegesCount = from.getAddresses().stream()
				.map(collegeAddress -> collegeAddress.getCourses().size())
				.mapToInt(i -> i.intValue()).sum();
		
		Integer studentsCount = from.getAddresses().stream()
						.map(collegeAddress -> collegeAddress.getUserCollegeAddress().size())
						.mapToInt(i -> i.intValue()).sum();
		
		Map<User, List<CollegeGrade>> gradesGroupByUser = from.getGrades().stream()
			.filter(grade -> grade.getGradeOrigin().isFromStudent())
			.collect(Collectors.groupingBy(CollegeGrade::getUser));
		
		dto.setCoursesCount(collegesCount);
		dto.setStudentsCount(studentsCount);
		dto.setRatingsCount(gradesGroupByUser.keySet().size());
		
		loggedUserManager.getStudent().ifPresent(user -> {
			if (from.isAssigned(user)) {
				dto.setAlreadyRated(gradesGroupByUser.containsKey(user));
			}
		});
		
		List<String> states = new ArrayList<>();
		from.getAddresses().forEach(c -> {
			String state = trim(c.getState()).replaceAll("  ","");
			if(!states.contains(state)){
				states.add(state);
			}
		});
		dto.setStates(states);
		
		return dto;
	}
}
