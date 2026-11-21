package com.dlog.study;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/** 유형 목록을 'WORK,TECH' 같은 쉼표 구분 문자열 한 칸에 저장한다 (없으면 빈 문자열) */
@Converter
public class StudyTypesConverter implements AttributeConverter<Set<StudyType>, String> {

    @Override
    public String convertToDatabaseColumn(Set<StudyType> types) {
        if (types == null || types.isEmpty()) {
            return "";
        }
        // EnumSet 으로 바꿔 항상 선언 순서로 저장
        return EnumSet.copyOf(types).stream().map(Enum::name).collect(Collectors.joining(","));
    }

    @Override
    public Set<StudyType> convertToEntityAttribute(String value) {
        EnumSet<StudyType> types = EnumSet.noneOf(StudyType.class);
        if (value == null || value.isBlank()) {
            return types;
        }
        Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .map(StudyType::valueOf)
                .forEach(types::add);
        return types;
    }
}
