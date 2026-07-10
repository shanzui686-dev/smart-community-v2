package com.smartcommunity.server;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartcommunity.server.entity.Camera;
import com.smartcommunity.server.entity.Community;
import com.smartcommunity.server.entity.InOutRecord;
import com.smartcommunity.server.entity.Person;
import com.smartcommunity.server.entity.Visitor;
import com.smartcommunity.server.mapper.CameraMapper;
import com.smartcommunity.server.mapper.CommunityMapper;
import com.smartcommunity.server.mapper.InOutRecordMapper;
import com.smartcommunity.server.mapper.PersonMapper;
import com.smartcommunity.server.mapper.VisitorMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SpringBootTest
public class DataInitTest {

    @Autowired
    private CommunityMapper communityMapper;

    @Autowired
    private PersonMapper personMapper;

    @Autowired
    private CameraMapper cameraMapper;

    @Autowired
    private InOutRecordMapper inOutRecordMapper;

    @Autowired
    private VisitorMapper visitorMapper;

    private final Random random = new Random();

    private static final String[] FAMILY_NAMES = {
            "张", "李", "王", "刘", "陈", "杨", "赵", "黄", "周", "吴",
            "徐", "孙", "马", "朱", "胡", "郭", "何", "罗", "高", "林",
            "梁", "谢", "宋", "唐", "许", "邓", "韩", "冯", "曹", "彭"
    };

    private static final String[] GIVEN_NAMES_MALE = {
            "伟", "强", "勇", "军", "磊", "涛", "明", "杰", "超", "鹏",
            "亮", "辉", "军", "洋", "平", "刚", "勇", "健", "峰", "波"
    };

    private static final String[] GIVEN_NAMES_FEMALE = {
            "丽", "敏", "静", "芳", "燕", "霞", "玲", "兰", "英", "雪",
            "梅", "华", "红", "秀", "娟", "萍", "婷", "芬", "娜", "莉"
    };

    @Test
    public void initAllData() {
        System.out.println("========== 开始初始化测试数据 ==========");
        
        List<Community> communities = initCommunityData();
        System.out.println("小区数据初始化完成，共创建 " + communities.size() + " 个小区");
        
        int personCount = initPersonData(communities);
        System.out.println("人员数据初始化完成，共创建 " + personCount + " 条记录");
        
        int cameraCount = initCameraData(communities);
        System.out.println("摄像头数据初始化完成，共创建 " + cameraCount + " 条记录");
        
        int recordCount = initInOutRecordData();
        System.out.println("出入记录数据初始化完成，共创建 " + recordCount + " 条记录");
        
        int visitorCount = initVisitorData();
        System.out.println("访客登记数据初始化完成，共创建 " + visitorCount + " 条记录");
        
        System.out.println("========== 测试数据初始化完成 ==========");
    }

    @Test
    public void initCommunityDataOnly() {
        List<Community> communities = initCommunityData();
        System.out.println("小区数据初始化完成，共创建 " + communities.size() + " 个小区");
    }

    @Test
    public void initPersonDataOnly() {
        List<Community> communities = communityMapper.selectList(null);
        if (communities.isEmpty()) {
            System.out.println("请先初始化小区数据");
            return;
        }
        int count = initPersonData(communities);
        System.out.println("人员数据初始化完成，共创建 " + count + " 条记录");
    }

    @Test
    public void initCameraDataOnly() {
        List<Community> communities = communityMapper.selectList(null);
        if (communities.isEmpty()) {
            System.out.println("请先初始化小区数据");
            return;
        }
        int count = initCameraData(communities);
        System.out.println("摄像头数据初始化完成，共创建 " + count + " 条记录");
    }

    @Test
    public void initInOutRecordDataOnly() {
        int count = initInOutRecordData();
        System.out.println("出入记录数据初始化完成，共创建 " + count + " 条记录");
    }

    @Test
    public void initVisitorDataOnly() {
        int count = initVisitorData();
        System.out.println("访客登记数据初始化完成，共创建 " + count + " 条记录");
    }

    public List<Community> initCommunityData() {
        List<Community> communities = new ArrayList<>();

        Community c1 = new Community();
        c1.setName("阳光花园小区");
        c1.setAddress("北京市朝阳区建国路88号");
        c1.setMapLng(new BigDecimal("116.479286"));
        c1.setMapLat(new BigDecimal("39.904989"));
        c1.setTotalBuilding(12);
        c1.setTotalHouse(864);
        c1.setDescription("高档住宅小区，配套设施完善");
        c1.setStatus(1);
        communities.add(c1);

        Community c2 = new Community();
        c2.setName("锦绣家园");
        c2.setAddress("北京市海淀区中关村大街1号");
        c2.setMapLng(new BigDecimal("116.318229"));
        c2.setMapLat(new BigDecimal("39.984158"));
        c2.setTotalBuilding(8);
        c2.setTotalHouse(576);
        c2.setDescription("学区房，周边教育资源丰富");
        c2.setStatus(1);
        communities.add(c2);

        Community c3 = new Community();
        c3.setName("碧水湾小区");
        c3.setAddress("北京市丰台区南四环西路188号");
        c3.setMapLng(new BigDecimal("116.283332"));
        c3.setMapLat(new BigDecimal("39.853331"));
        c3.setTotalBuilding(15);
        c3.setTotalHouse(1080);
        c3.setDescription("水景园林社区，环境优美");
        c3.setStatus(1);
        communities.add(c3);

        Community c4 = new Community();
        c4.setName("幸福港湾");
        c4.setAddress("北京市西城区西直门外大街1号");
        c4.setMapLng(new BigDecimal("116.343333"));
        c4.setMapLat(new BigDecimal("39.946667"));
        c4.setTotalBuilding(6);
        c4.setTotalHouse(432);
        c4.setDescription("老城区改造项目，生活便利");
        c4.setStatus(1);
        communities.add(c4);

        Community c5 = new Community();
        c5.setName("星河湾小区");
        c5.setAddress("北京市通州区新华大街200号");
        c5.setMapLng(new BigDecimal("116.655000"));
        c5.setMapLat(new BigDecimal("39.911667"));
        c5.setTotalBuilding(20);
        c5.setTotalHouse(1440);
        c5.setDescription("大型社区，配套商业齐全");
        c5.setStatus(1);
        communities.add(c5);

        Community c6 = new Community();
        c6.setName("翠湖雅居");
        c6.setAddress("北京市房山区长阳路88号");
        c6.setMapLng(new BigDecimal("116.120000"));
        c6.setMapLat(new BigDecimal("39.735000"));
        c6.setTotalBuilding(10);
        c6.setTotalHouse(720);
        c6.setDescription("低密度花园洋房，宜居社区");
        c6.setStatus(1);
        communities.add(c6);

        for (Community community : communities) {
            communityMapper.insert(community);
        }

        return communities;
    }

    public int initPersonData(List<Community> communities) {
        int totalCount = 0;
        
        int[] personCounts = generateNormalDistribution(communities.size(), 100, 30);
        
        for (int i = 0; i < communities.size(); i++) {
            Community community = communities.get(i);
            int count = personCounts[i];
            List<Person> persons = generatePersonData(community, count);
            for (Person person : persons) {
                personMapper.insert(person);
            }
            totalCount += count;
            System.out.println("小区[" + community.getName() + "]生成 " + count + " 条人员记录");
        }
        
        return totalCount;
    }

    private int[] generateNormalDistribution(int size, int mean, int stdDev) {
        int[] counts = new int[size];
        double[] rawValues = new double[size];
        
        double sum = 0;
        for (int i = 0; i < size; i++) {
            rawValues[i] = random.nextGaussian() * stdDev + mean;
            if (rawValues[i] < 30) rawValues[i] = 30;
            sum += rawValues[i];
        }
        
        int totalSum = mean * size;
        for (int i = 0; i < size; i++) {
            counts[i] = (int) Math.round(rawValues[i] / sum * totalSum);
            if (counts[i] < 20) counts[i] = 20;
        }
        
        int actualSum = 0;
        for (int count : counts) {
            actualSum += count;
        }
        int diff = totalSum - actualSum;
        if (diff != 0) {
            counts[size / 2] += diff;
        }
        
        return counts;
    }

    private List<Person> generatePersonData(Community community, int count) {
        List<Person> persons = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            Person person = new Person();
            person.setCommunityId(community.getCommunityId());
            
            int sex = random.nextInt(2) + 1;
            person.setSex(sex);
            person.setUserName(generateName(sex));
            
            person.setMobile(generateMobile());
            person.setHouseNo(generateHouseNo(community.getTotalBuilding()));
            
            int type = random.nextInt(3) + 1;
            person.setPersonType(type);
            
            person.setState(random.nextInt(10) < 9 ? 1 : 0);
            
            persons.add(person);
        }
        
        return persons;
    }

    private String generateName(int sex) {
        String familyName = FAMILY_NAMES[random.nextInt(FAMILY_NAMES.length)];
        String givenName;
        if (sex == 1) {
            givenName = GIVEN_NAMES_MALE[random.nextInt(GIVEN_NAMES_MALE.length)];
        } else {
            givenName = GIVEN_NAMES_FEMALE[random.nextInt(GIVEN_NAMES_FEMALE.length)];
        }
        if (random.nextBoolean()) {
            givenName += GIVEN_NAMES_MALE[random.nextInt(GIVEN_NAMES_MALE.length)];
        }
        return familyName + givenName;
    }

    private String generateMobile() {
        StringBuilder sb = new StringBuilder("1");
        sb.append(random.nextInt(7) + 3);
        for (int i = 0; i < 9; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private String generateHouseNo(int totalBuilding) {
        int building = random.nextInt(totalBuilding) + 1;
        int unit = random.nextInt(4) + 1;
        int floor = random.nextInt(18) + 1;
        int room = random.nextInt(4) + 1;
        return String.format("%d-%d-%02d%02d", building, unit, floor, room);
    }

    public int initCameraData(List<Community> communities) {
        int totalCount = 0;
        
        for (Community community : communities) {
            int cameraCount = random.nextInt(4) + 1;
            List<Camera> cameras = generateCameraData(community, cameraCount);
            for (Camera camera : cameras) {
                cameraMapper.insert(camera);
            }
            totalCount += cameraCount;
            System.out.println("小区[" + community.getName() + "]生成 " + cameraCount + " 个摄像头");
        }
        
        return totalCount;
    }

    private List<Camera> generateCameraData(Community community, int count) {
        List<Camera> cameras = new ArrayList<>();
        
        String[] locations = {"东门", "西门", "南门", "北门", "1号楼", "2号楼", "3号楼", "地下车库"};
        
        for (int i = 0; i < count; i++) {
            Camera camera = new Camera();
            camera.setName(community.getName() + "-" + locations[i % locations.length]);
            camera.setDeviceCode("CAM-" + community.getCommunityId() + "-" + String.format("%03d", i + 1));
            camera.setIpAddress(generateIpAddress());
            camera.setCommunityId(community.getCommunityId());
            camera.setLocation(locations[i % locations.length]);
            camera.setDeviceType(i < 2 ? 1 : 2);
            camera.setOnlineStatus(random.nextInt(10) < 8 ? 1 : 0);
            camera.setStatus(random.nextInt(10) < 9 ? 1 : 0);
            
            cameras.add(camera);
        }
        
        return cameras;
    }

    private String generateIpAddress() {
        return String.format("192.168.%d.%d", 
                random.nextInt(255) + 1, 
                random.nextInt(254) + 1);
    }

    public int initInOutRecordData() {
        List<Person> persons = personMapper.selectList(null);
        List<Camera> cameras = cameraMapper.selectList(null);
        List<Community> communities = communityMapper.selectList(null);
        
        if (persons.isEmpty() || cameras.isEmpty() || communities.isEmpty()) {
            System.out.println("请先初始化小区、人员和摄像头数据");
            return 0;
        }

        int totalCount = 0;
        int[] dailyCounts = generateNormalDistribution(7, 30, 8);
        
        LocalDateTime now = LocalDateTime.now();
        int dayIndex = 0;
        
        for (int dayOffset = 6; dayOffset >= 0; dayOffset--) {
            int count = dailyCounts[dayIndex++];
            List<InOutRecord> records = generateInOutRecordData(persons, cameras, communities, count, dayOffset, now);
            for (InOutRecord record : records) {
                inOutRecordMapper.insert(record);
            }
            totalCount += count;
            System.out.println("第" + (7 - dayOffset) + "天生成 " + count + " 条出入记录");
        }
        
        return totalCount;
    }

    private List<InOutRecord> generateInOutRecordData(List<Person> persons, List<Camera> cameras, 
                                                       List<Community> communities, int count, 
                                                       int dayOffset, LocalDateTime now) {
        List<InOutRecord> records = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            InOutRecord record = new InOutRecord();
            
            Person person = persons.get(random.nextInt(persons.size()));
            Camera camera = cameras.get(random.nextInt(cameras.size()));
            
            record.setPersonId(person.getPersonId());
            record.setPersonName(person.getUserName());
            record.setCommunityId(person.getCommunityId());
            record.setCameraId(camera.getCameraId());
            record.setLocation(person.getHouseNo());
            record.setType(random.nextInt(2) + 1);
            record.setVerifyType(random.nextInt(4) + 1);
            record.setVerified(1);
            
            LocalDateTime baseTime = now.minusDays(dayOffset);
            int hour = random.nextInt(12) + 6;
            int minute = random.nextInt(60);
            int second = random.nextInt(60);
            record.setTime(baseTime.withHour(hour).withMinute(minute).withSecond(second));
            
            records.add(record);
        }
        
        return records;
    }

    public int initVisitorData() {
        List<Person> persons = personMapper.selectList(null);
        List<Community> communities = communityMapper.selectList(null);
        
        if (persons.isEmpty() || communities.isEmpty()) {
            System.out.println("请先初始化小区和人员数据");
            return 0;
        }

        int totalCount = 0;
        int[] dailyCounts = generateNormalDistribution(7, 30, 8);
        
        LocalDateTime now = LocalDateTime.now();
        int dayIndex = 0;
        
        for (int dayOffset = 6; dayOffset >= 0; dayOffset--) {
            int count = dailyCounts[dayIndex++];
            List<Visitor> visitors = generateVisitorData(persons, communities, count, dayOffset, now);
            for (Visitor visitor : visitors) {
                visitorMapper.insert(visitor);
            }
            totalCount += count;
            System.out.println("第" + (7 - dayOffset) + "天生成 " + count + " 条访客登记");
        }
        
        return totalCount;
    }

    private List<Visitor> generateVisitorData(List<Person> persons, List<Community> communities, 
                                              int count, int dayOffset, LocalDateTime now) {
        List<Visitor> visitors = new ArrayList<>();
        
        String[] reasons = {"探亲访友", "快递送货", "维修服务", "装修施工", "访客参观", "外卖配送"};
        String[] platePrefixes = {"京", "沪", "粤", "浙", "苏", "鲁", "川", "渝", "湘", "鄂"};
        
        for (int i = 0; i < count; i++) {
            Visitor visitor = new Visitor();
            
            Person person = persons.get(random.nextInt(persons.size()));
            Community community = communities.get(random.nextInt(communities.size()));
            
            int sex = random.nextInt(2) + 1;
            visitor.setName(generateName(sex));
            visitor.setMobile(generateMobile());
            visitor.setIdCard(generateIdCard());
            visitor.setCommunityId(community.getCommunityId());
            visitor.setHouseNo(person.getHouseNo());
            visitor.setPersonId(person.getPersonId());
            visitor.setReason(reasons[random.nextInt(reasons.length)]);
            visitor.setVisitorCount(random.nextInt(3) + 1);
            
            LocalDateTime baseTime = now.minusDays(dayOffset);
            int hour = random.nextInt(8) + 8;
            int minute = random.nextInt(60);
            int second = random.nextInt(60);
            visitor.setVisitTime(baseTime.withHour(hour).withMinute(minute).withSecond(second));
            
            int stayHours = random.nextInt(8) + 1;
            visitor.setLeaveTime(visitor.getVisitTime().plusHours(stayHours));
            
            visitor.setPlateNumber(random.nextBoolean() ? 
                    platePrefixes[random.nextInt(platePrefixes.length)] + 
                    generatePlateNumber() : null);
            
            visitor.setStatus(random.nextInt(10) < 7 ? 2 : 1);
            
            visitors.add(visitor);
        }
        
        return visitors;
    }

    private String generateIdCard() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 17; i++) {
            sb.append(random.nextInt(10));
        }
        sb.append(random.nextInt(10));
        return sb.toString();
    }

    private String generatePlateNumber() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder();
        sb.append(chars.charAt(random.nextInt(chars.length())));
        for (int i = 0; i < 5; i++) {
            if (random.nextBoolean()) {
                sb.append(random.nextInt(10));
            } else {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
        }
        return sb.toString();
    }

    @Test
    public void clearTestData() {
        LambdaQueryWrapper<InOutRecord> recordWrapper = new LambdaQueryWrapper<>();
        inOutRecordMapper.delete(recordWrapper);
        
        LambdaQueryWrapper<Visitor> visitorWrapper = new LambdaQueryWrapper<>();
        visitorMapper.delete(visitorWrapper);
        
        LambdaQueryWrapper<Camera> cameraWrapper = new LambdaQueryWrapper<>();
        cameraMapper.delete(cameraWrapper);
        
        LambdaQueryWrapper<Person> personWrapper = new LambdaQueryWrapper<>();
        personMapper.delete(personWrapper);
        
        LambdaQueryWrapper<Community> communityWrapper = new LambdaQueryWrapper<>();
        communityMapper.delete(communityWrapper);
        
        System.out.println("测试数据已清空");
    }
}