package com.smartcommunity.server.controller;

import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.common.Result;
import com.smartcommunity.server.entity.Person;
import com.smartcommunity.server.security.RequirePermission;
import com.smartcommunity.server.security.OperateLog;
import com.smartcommunity.server.service.PersonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "居民管理", description = "居民信息增删改查、导入导出")
@RestController
@RequestMapping("/api/property/person")
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @Operation(summary = "分页查询居民")
    @RequirePermission("property:person:list")
    @GetMapping("/list")
    public Result<PageResult<Person>> list(PageQuery pageQuery,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Long communityId,
                                            @RequestParam(required = false) Integer personType) {
        return Result.success(personService.pageList(pageQuery, keyword, communityId, personType));
    }

    @Operation(summary = "获取居民详情")
    @RequirePermission("property:person:list")
    @GetMapping("/{id}")
    public Result<Person> getById(@PathVariable Long id) {
        return Result.success(personService.getById(id));
    }

    @Operation(summary = "新增居民")
    @RequirePermission("property:person:list")
    @OperateLog(module = "居民管理", operation = "新增")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody Person person) {
        personService.add(person);
        return Result.success();
    }

    @Operation(summary = "修改居民")
    @RequirePermission("property:person:list")
    @OperateLog(module = "居民管理", operation = "修改")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody Person person) {
        personService.update(person);
        return Result.success();
    }

    @Operation(summary = "删除居民")
    @RequirePermission("property:person:list")
    @OperateLog(module = "居民管理", operation = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        personService.delete(id);
        return Result.success();
    }

    @Operation(summary = "上传人脸照片")
    @RequirePermission("property:person:list")
    @PostMapping("/{id}/face")
    public Result<String> uploadFace(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return Result.success(personService.uploadFace(id, file));
    }

    @Operation(summary = "导入Excel")
    @RequirePermission("property:person:list")
    @PostMapping("/import")
    public Result<Void> importExcel(@RequestParam("file") MultipartFile file) {
        personService.importExcel(file);
        return Result.success();
    }

    @Operation(summary = "导出Excel")
    @RequirePermission("property:person:list")
    @GetMapping("/export")
    public void exportExcel(HttpServletResponse response,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(required = false) Long communityId,
                            @RequestParam(required = false) Integer personType,
                            @RequestParam(required = false) String ids) {
        personService.exportExcel(response, keyword, communityId, personType, ids);
    }
}
