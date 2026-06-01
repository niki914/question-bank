package com.example.questionbank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.questionbank.dto.ExamRoomDTO;
import com.example.questionbank.dto.ExamRoomVO;
import com.example.questionbank.entity.ExamRoom;
import com.example.questionbank.entity.Teacher;
import com.example.questionbank.mapper.ExamRoomMapper;
import com.example.questionbank.mapper.TeacherMapper;
import com.example.questionbank.service.ExamRoomService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExamRoomServiceImpl implements ExamRoomService {

    private final ExamRoomMapper roomMapper;
    private final TeacherMapper teacherMapper;

    public ExamRoomServiceImpl(ExamRoomMapper roomMapper, TeacherMapper teacherMapper) {
        this.roomMapper = roomMapper;
        this.teacherMapper = teacherMapper;
    }

    @Override
    public ExamRoomVO create(ExamRoomDTO dto) {
        ExamRoom room = new ExamRoom();
        copyDto(dto, room);
        roomMapper.insert(room);
        return buildVO(room);
    }

    @Override
    public ExamRoomVO update(Long id, ExamRoomDTO dto) {
        ExamRoom room = roomMapper.selectById(id);
        if (room == null) throw new IllegalArgumentException("考场不存在");
        copyDto(dto, room);
        roomMapper.updateById(room);
        return buildVO(room);
    }

    @Override
    public void delete(Long id) {
        ExamRoom room = roomMapper.selectById(id);
        if (room == null) throw new IllegalArgumentException("考场不存在");
        roomMapper.deleteById(id);
    }

    @Override
    public ExamRoomVO getById(Long id) {
        ExamRoom room = roomMapper.selectById(id);
        if (room == null) throw new IllegalArgumentException("考场不存在");
        return buildVO(room);
    }

    @Override
    public List<ExamRoomVO> listAll() {
        return roomMapper.selectList(new LambdaQueryWrapper<ExamRoom>()
                        .orderByDesc(ExamRoom::getCreateTime))
                .stream().map(this::buildVO).collect(Collectors.toList());
    }

    private void copyDto(ExamRoomDTO dto, ExamRoom room) {
        room.setRoomName(dto.getRoomName());
        room.setLocation(dto.getLocation());
        room.setCapacity(dto.getCapacity());
        room.setTeacherId(dto.getTeacherId());
    }

    private ExamRoomVO buildVO(ExamRoom room) {
        ExamRoomVO vo = new ExamRoomVO();
        vo.setId(room.getId());
        vo.setRoomName(room.getRoomName());
        vo.setLocation(room.getLocation());
        vo.setCapacity(room.getCapacity());
        vo.setTeacherId(room.getTeacherId());
        vo.setCreateTime(room.getCreateTime());
        Teacher teacher = teacherMapper.selectById(room.getTeacherId());
        if (teacher != null) vo.setTeacherName(teacher.getName());
        return vo;
    }
}
