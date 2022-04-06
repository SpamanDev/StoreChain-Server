package com.cobnet.spring.boot.entity;

import com.cobnet.interfaces.security.Account;
import com.cobnet.interfaces.security.Permissible;
import com.cobnet.interfaces.security.Permission;
import com.cobnet.interfaces.spring.entity.StoreMemberRelated;
import com.cobnet.security.RoleRule;
import com.cobnet.spring.boot.core.ProjectBeanHolder;
import com.cobnet.spring.boot.entity.support.JsonPermissionSetConverter;
import com.cobnet.spring.boot.entity.support.OwnedExternalUserCollection;
import com.cobnet.spring.boot.entity.support.OwnedPermissionCollection;
import com.cobnet.spring.boot.entity.support.OwnedRoleCollection;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
public class User extends EntityBase implements Permissible, Account, UserDetails, StoreMemberRelated {

    private static final Logger LOG = LoggerFactory.getLogger(User.class);

    @Id
    private String username;

    @Column(nullable = false)
    private String password;

    private boolean passwordEncoded;

    @ManyToMany
    @LazyCollection(LazyCollectionOption.FALSE)
    @JoinTable(name = "user_roles", joinColumns = { @JoinColumn(name = "user", referencedColumnName = "username") },
            inverseJoinColumns = { @JoinColumn(name = "role", referencedColumnName = "role") })
    private Set<UserRole> roles = new HashSet<>();

    @Convert(converter = JsonPermissionSetConverter.class)
    @Column(columnDefinition = "json")
    private Set<Permission> permissions = new HashSet<>();

    private boolean expired;

    private boolean locked;

    private boolean vaildPassword;

    private boolean enabled;

    @LazyCollection(LazyCollectionOption.FALSE)
    @OneToMany(mappedBy="user")
    private Set<ExternalUser> externalUsers = new HashSet<ExternalUser>();

    @LazyCollection(LazyCollectionOption.FALSE)
    @OneToMany(mappedBy = "user")
    private Set<StoreStaff> associated = new HashSet<>();

    private transient OwnedRoleCollection roleCollection;

    private transient OwnedPermissionCollection permissionsCollection;

    private transient OwnedExternalUserCollection externalUserCollection;

    public User() {}

    public User(@NonNull String username, @NonNull String password, @NonNull List<UserRole> roles, boolean expired, boolean locked, boolean vaildPassword, boolean enabled) {

        this.username = username;
        this.password = password;
        this.getOwnedRoleCollection().addAll(roles.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(EntityBase::getCreatedTime))), ArrayList::new)));
        this.expired = expired;
        this.locked = locked;
        this.vaildPassword = vaildPassword;
        this.enabled = enabled;

    }

    public User(@NonNull String username, @NonNull String password, @NonNull List<UserRole> roles) {

        this(username, password, roles, false, false, true, true);
    }

    public User(@NonNull String username, @NonNull String password, UserRole... roles) {

        this(username, password, Arrays.stream(roles).toList());
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return !this.expired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !this.locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.vaildPassword;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.getPermissions();
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {

        this.password = password;
        this.passwordEncoded = false;
    }

    @Override
    public String getIdentity() {
        return this.getUsername();
    }

    public void setPasswordEncoded(boolean passwordEncoded) {
        this.passwordEncoded = passwordEncoded;
    }

    public boolean isPasswordEncoded() {
        return passwordEncoded;
    }

    @Override
    public RoleRule getRule() {

        Optional<UserRole> role = this.roles.stream().max(Comparator.comparingInt(UserRole::getPower));

        return role.isEmpty() ? RoleRule.VISITOR : role.get().getRule();
    }

    public OwnedRoleCollection getOwnedRoleCollection() {

        if(this.roleCollection == null) {

            this.roleCollection = new OwnedRoleCollection(this, this.roles);
        }

        return this.roleCollection;
    }

    public OwnedPermissionCollection getOwnedPermissionCollection() {

        if(this.permissionsCollection == null) {

            this.permissionsCollection = new OwnedPermissionCollection(this, this.permissions);
        }

        return this.permissionsCollection;
    }

    public OwnedExternalUserCollection getOwnedExternalUserCollection() {

        if(this.externalUserCollection == null) {

            this.externalUserCollection = new OwnedExternalUserCollection(this, this.externalUsers);
        }

        return this.externalUserCollection;
    }

    public Set<? extends ExternalUser> getExternalUsers() {

        return Collections.unmodifiableSet(this.externalUsers);
    }


    @Override
    public Collection<? extends Permission> getPermissions() {

        return Stream.of(roles.stream().map(UserRole::getPermissions).flatMap(Collection::stream).collect(Collectors.toList()), permissions).flatMap(Collection::stream).collect(Collectors.toUnmodifiableList());
    }

    public Collection<StoreStaff> getAssociated() {

        return associated.stream().collect(Collectors.toUnmodifiableList());
    }

    public Collection<Store> getStores() {

        return associated.stream().map(staff -> staff.getStore()).collect(Collectors.toUnmodifiableList());
    }

    public boolean hasRole(String... roles) {

        return Arrays.stream(roles).allMatch(this::isRole);
    }

    public boolean isRole(String name) {

        return this.roles.stream().anyMatch(role -> role.getName().equalsIgnoreCase(name));
    }

    @Override
    public boolean isPermitted(String authority) {

        return this.getOwnedPermissionCollection().hasPermission(authority) || this.getOwnedRoleCollection().stream().anyMatch(role -> role.isPermitted(authority));
    }

    @Override
    public boolean addPermission(Permission permission) {

        return this.getOwnedPermissionCollection().add(permission);
    }

    @Override
    public boolean removePermission(Permission permission) {

        if(this.getOwnedPermissionCollection().hasPermission(permission)) {

            return this.getOwnedPermissionCollection().remove(permission);
        }

        boolean result = false;

        for(ExternalUser user : this.getOwnedExternalUserCollection()) {

            result = user.removePermission(permission);
        }

        return result;
    }

    public boolean addExternalUser(ExternalUser user) {

        return this.getOwnedExternalUserCollection().add(user);
    }

    public boolean removeExternalUser(ExternalUser user) {

        return this.getOwnedExternalUserCollection().remove(user);
    }

    public PersistentLogins getRemeberMeInfo() {

        return ProjectBeanHolder.getPersistentLoginsRepository().findByUsernameEqualsIgnoreCase(this.getUsername());
    }

    public static class Builder {

        private String username;

        private String password;

        private List<UserRole> roles;

        private boolean expired;

        private boolean locked;

        private boolean vaildPassword;

        private boolean enabled;

        public Builder setUsername(String username) {

            this.username = username;

            return this;
        }

        public Builder setPassword(String password) {

            this.password = password;

            return this;
        }

        public Builder setRoles(UserRole... roles) {

            this.roles = Arrays.stream(roles).toList();

            return this;
        }

        public Builder setExpired(boolean expired) {

            this.expired = expired;

            return this;
        }

        public Builder setLocked(boolean locked) {

            this.locked = locked;

            return this;
        }

        public Builder setVaildPassword(boolean vaildPassword) {

            this.vaildPassword = vaildPassword;

            return this;
        }

        public Builder setEnabled(boolean enabled) {

            this.enabled = enabled;

            return this;
        }

        public User build() {

            return new User(this.username, this.password, this.roles, this.expired, this.locked, this.vaildPassword, this.enabled);
        }
    }
}
