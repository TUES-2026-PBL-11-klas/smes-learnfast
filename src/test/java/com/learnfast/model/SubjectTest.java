package com.learnfast.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class SubjectTest {

    @Test
    void defaultConstructor_createsInstance() {
        Subject subject = new Subject();
        assertThat(subject).isNotNull();
        assertThat(subject.getId()).isNull();
        assertThat(subject.getName()).isNull();
    }

    @Test
    void nameConstructor_setsName() {
        Subject subject = new Subject("Mathematics");
        assertThat(subject.getName()).isEqualTo("Mathematics");
        assertThat(subject.getId()).isNull();
    }

    @Test
    void setAndGetId() {
        Subject subject = new Subject();
        subject.setId(7L);
        assertThat(subject.getId()).isEqualTo(7L);
    }

    @Test
    void setAndGetName() {
        Subject subject = new Subject();
        subject.setName("Physics");
        assertThat(subject.getName()).isEqualTo("Physics");
    }

    @Test
    void setName_overwritesPreviousValue() {
        Subject subject = new Subject("Math");
        subject.setName("Science");
        assertThat(subject.getName()).isEqualTo("Science");
    }

    @Test
    void twoSubjectsWithSameName_areNotSameObject() {
        Subject s1 = new Subject("Biology");
        Subject s2 = new Subject("Biology");
        assertThat(s1).isNotSameAs(s2);
        assertThat(s1.getName()).isEqualTo(s2.getName());
    }

    @Test
    void nameConstructor_withVariousSubjects() {
        assertThat(new Subject("Chemistry").getName()).isEqualTo("Chemistry");
        assertThat(new Subject("History").getName()).isEqualTo("History");
        assertThat(new Subject("Computer Science").getName()).isEqualTo("Computer Science");
    }
}
