package com.example.annotationPlatform.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@Builder
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails, Principal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Le nom utilisateur est requis")
    @Size(min = 3, max = 50, message = "Le nom utilisateur doit faire au moins 3 caractères")
    private String username;

    @Column(nullable = false)
    private String password;

    @NotBlank(message = "Le prénom est requis")
    @Column(name = "first_name")
    private String firstName;

    @NotBlank(message = "Le nom est requis")
    @Column(name = "last_name")
    private String lastName;

    @Email(message = "L'email est invalide")
    @NotBlank(message = "L'email est requis")
    private String email;

    private Boolean enable = true;
    private Boolean accountLocked = false;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createDate;

    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime lastModifiedDate;

    @ManyToMany(fetch = FetchType.EAGER)
    private List<Role> roles;

    @OneToMany(mappedBy = "annotator", cascade = CascadeType.ALL)
    private Set<AnnotationTask> annotationTasks = new HashSet<>();

    @OneToMany(mappedBy = "annotator", cascade = CascadeType.ALL)
    private Set<Annotation> annotations = new HashSet<>();

    @Column(name = "annotations_count")
    private Integer annotationsCount = 0;

    @Column(name = "avg_annotation_time_ms")
    private Long avgAnnotationTimeMs = 0L;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !accountLocked;
    }

    @Override
    public boolean isEnabled() {
        return enable;
    }

    @Override
    public String getName() {
        return email;
    }

    public String fullName() {
        return firstName + ' ' + lastName;
    }
}
