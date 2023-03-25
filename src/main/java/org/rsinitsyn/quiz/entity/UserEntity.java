//package org.rsinitsyn.quiz.entity;
//
//import jakarta.persistence.Column;
//import jakarta.persistence.Entity;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//import jakarta.persistence.Table;
//import java.util.UUID;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//import org.hibernate.annotations.NaturalId;
//
//@Entity
//@Table(name = "userse")
//@Getter
//@Setter
//@NoArgsConstructor
//public class UserEntity {
//    @Id
//    @GeneratedValue(strategy = GenerationType.UUID)
//    private UUID id;
//    @NaturalId
//    @Column(unique = true, nullable = false)
//    private String username;
//}
