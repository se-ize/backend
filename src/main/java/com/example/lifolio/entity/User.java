package com.example.lifolio.entity;

import com.example.lifolio.base.BaseEntity;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.util.Set;

@Entity
@Table(name = "User")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@DynamicUpdate
@DynamicInsert
public class User extends BaseEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //username == userId
    //여러가지 시도해보다가 오류나서 일단은 username상태로 뒀습니다ㅠㅠ
    //테스트용 주석  (민기)
    @Column(name = "username", length = 50, unique = true)
    private String username;


    @Column(name = "password", length = 100)
    private String password;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "activated")
    private boolean activated;

    @Column(name="phone")
    private String phone;

    @Column(name="social")
    private String social;

    @Column(name="all_alarm")
    private String allAlarm;

    @Column(name="my_alarm")
    private String myAlarm;

    @Column(name="planning_alarm")
    private String planningAlarm;

    @Column(name="social_alarm")
    private String socialAlarm;







    @ManyToMany
    @JoinTable(
            name = "user_authority",
            joinColumns = {@JoinColumn(name = "id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "authority_name", referencedColumnName = "authority_name")})
    private Set<Authority> authorities;
}