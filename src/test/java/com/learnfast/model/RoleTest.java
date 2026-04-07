package com.learnfast.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RoleTest {

    @Test
    void defaultConstructor_createsInstance() {
        Role role = new Role();
        assertThat(role).isNotNull();
        assertThat(role.getId()).isNull();
        assertThat(role.getName()).isNull();
    }

    @Test
    void nameConstructor_setsName() {
        Role role = new Role("mentor");
        assertThat(role.getName()).isEqualTo("mentor");
        assertThat(role.getId()).isNull(); // id not set by constructor
    }

    @Test
    void setAndGetId() {
        Role role = new Role();
        role.setId(1L);
        assertThat(role.getId()).isEqualTo(1L);
    }

    @Test
    void setAndGetName() {
        Role role = new Role();
        role.setName("student");
        assertThat(role.getName()).isEqualTo("student");
    }

    @Test
    void nameConstructor_withStudent() {
        Role role = new Role("student");
        assertThat(role.getName()).isEqualTo("student");
    }

    @Test
    void nameConstructor_withMentor() {
        Role role = new Role("mentor");
        assertThat(role.getName()).isEqualTo("mentor");
    }

    @Test
    void setName_overwritesPreviousValue() {
        Role role = new Role("student");
        role.setName("mentor");
        assertThat(role.getName()).isEqualTo("mentor");
    }

    @Test
    void twoRolesWithSameName_areNotSameObject() {
        Role r1 = new Role("mentor");
        Role r2 = new Role("mentor");
        assertThat(r1).isNotSameAs(r2);
        assertThat(r1.getName()).isEqualTo(r2.getName());
    }
}
