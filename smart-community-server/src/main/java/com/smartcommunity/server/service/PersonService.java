package com.smartcommunity.server.service;

import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.entity.Person;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;

public interface PersonService {

    PageResult<Person> pageList(PageQuery pageQuery, String keyword, Long communityId, Integer personType);

    Person getById(Long id);

    void add(Person person);

    void update(Person person);

    void delete(Long id);

    String uploadFace(Long personId, MultipartFile file);

    void importExcel(MultipartFile file);

    void exportExcel(HttpServletResponse response, String keyword, Long communityId, Integer personType, String ids);
}
