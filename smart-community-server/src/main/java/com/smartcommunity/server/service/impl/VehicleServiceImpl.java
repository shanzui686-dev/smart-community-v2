package com.smartcommunity.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.common.StatusEnum;
import com.smartcommunity.server.entity.Community;
import com.smartcommunity.server.entity.Person;
import com.smartcommunity.server.entity.Vehicle;
import com.smartcommunity.server.mapper.CommunityMapper;
import com.smartcommunity.server.mapper.PersonMapper;
import com.smartcommunity.server.mapper.VehicleMapper;
import com.smartcommunity.server.service.VehicleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleServiceImpl implements VehicleService {

    private final VehicleMapper vehicleMapper;
    private final PersonMapper personMapper;
    private final CommunityMapper communityMapper;

    public VehicleServiceImpl(VehicleMapper vehicleMapper, PersonMapper personMapper, CommunityMapper communityMapper) {
        this.vehicleMapper = vehicleMapper;
        this.personMapper = personMapper;
        this.communityMapper = communityMapper;
    }

    @Override
    public PageResult<Vehicle> pageList(PageQuery pageQuery, String keyword, Long communityId) {
        Page<Vehicle> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        LambdaQueryWrapper<Vehicle> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Vehicle::getPlateNumber, keyword);
            LambdaQueryWrapper<Person> personWrapper = new LambdaQueryWrapper<>();
            personWrapper.like(Person::getUserName, keyword);
            java.util.List<Person> persons = personMapper.selectList(personWrapper);
            if (!persons.isEmpty()) {
                java.util.List<Long> personIds = persons.stream().map(Person::getPersonId).toList();
                wrapper.or().in(Vehicle::getPersonId, personIds);
            }
        }
        if (communityId != null) {
            wrapper.eq(Vehicle::getCommunityId, communityId);
        }
        wrapper.orderByDesc(Vehicle::getCreateTime);
        Page<Vehicle> result = vehicleMapper.selectPage(page, wrapper);
        for (Vehicle vehicle : result.getRecords()) {
            fillExtraInfo(vehicle);
        }
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public Vehicle getById(Long id) {
        Vehicle vehicle = vehicleMapper.selectById(id);
        if (vehicle != null) {
            fillExtraInfo(vehicle);
        }
        return vehicle;
    }

    private void fillExtraInfo(Vehicle vehicle) {
        if (vehicle.getPersonId() != null) {
            Person person = personMapper.selectById(vehicle.getPersonId());
            if (person != null) {
                vehicle.setPersonName(person.getUserName());
                vehicle.setHouseNo(person.getHouseNo());
                vehicle.setMobile(person.getMobile());
            }
        }
        if (vehicle.getCommunityId() != null) {
            Community community = communityMapper.selectById(vehicle.getCommunityId());
            if (community != null) {
                vehicle.setCommunityName(community.getName());
            }
        }
    }

    @Override
    @Transactional
    public void add(Vehicle vehicle) {
        if (vehicle.getPersonId() == null) {
            throw new RuntimeException("车主不能为空");
        }
        Person person = personMapper.selectById(vehicle.getPersonId());
        if (person == null) {
            throw new RuntimeException("车主不存在");
        }
        if (person.getState() != 1) {
            throw new RuntimeException("车主必须是在住状态");
        }
        vehicle.setCommunityId(person.getCommunityId());
        vehicleMapper.insert(vehicle);
    }

    @Override
    @Transactional
    public void update(Vehicle vehicle) {
        if (vehicle.getPersonId() != null) {
            Person person = personMapper.selectById(vehicle.getPersonId());
            if (person == null) {
                throw new RuntimeException("车主不存在");
            }
            if (person.getState() != 1) {
                throw new RuntimeException("车主必须是在住状态");
            }
            vehicle.setCommunityId(person.getCommunityId());
        }
        vehicleMapper.updateById(vehicle);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        vehicleMapper.deleteById(id);
    }
}